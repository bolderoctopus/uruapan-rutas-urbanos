package com.rico.omarw.rutasuruapan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.rico.omarw.rutasuruapan.Database.AppDatabase
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import android.R.attr.top
import android.graphics.Rect
import android.util.Log
import com.rico.omarw.rutasuruapan.Database.Points


class MainActivity : AppCompatActivity(), OnMapReadyCallback, ControlPanel.OnFragmentInteractionListener, GoogleMap.OnMarkerDragListener, GoogleMap.OnMapLongClickListener {

    private val LOCATION_PERMISSION_REQUEST = 32
    private val LINE_WIDTH = 15f

    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var originSquare: Polygon? = null

    //views
    private lateinit var map: GoogleMap
    private lateinit var controlPanel: ControlPanel
    private lateinit var slidingLayout: SlidingUpPanelLayout



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controlPanel = supportFragmentManager.findFragmentById(R.id.fragment_control_panel) as ControlPanel
        slidingLayout= findViewById(R.id.sliding_layout)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapLongClickListener(this)
        map.setOnMarkerDragListener(this)
        slidingLayout.post{
            map.setPadding(0,getStatusBarHeight(),0,0)
            //todo: fix status bar color (currently not working:)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                window.statusBarColor = Color.parseColor("#20FF0000")
        }


        val uruapan = LatLng(19.411843, -102.051518)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(uruapan, 13f))
        if (locationPermissionEnabled()){
            map.isMyLocationEnabled = true
        }else {
            askPermission()
        }
    }

    private fun getStatusBarHeight(): Int{
        val rectangle = Rect()
        val window = window
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        Log.d(DEBUG_TAG, "statusBarHeight: " + rectangle.top)
        return rectangle.top
    }

    override fun onMapLongClick(pos: LatLng){
        when {
            originMarker == null ->
                originMarker = map.addMarker(MarkerOptions().title("Origin").position(pos).draggable(true))

            destinationMarker == null ->
                destinationMarker = map.addMarker(MarkerOptions().title("Destination").position(pos).draggable(true))

            else -> {
                originMarker?.isVisible = false
                destinationMarker?.isVisible = false
                originMarker = null
                destinationMarker = null
            }
        }
        controlPanel.setOriginDestinationText(latLngToString(originMarker?.position), latLngToString(destinationMarker?.position))
    }

    private fun latLngToString(pos: LatLng?): String{
        if(pos == null) return ""
        return "%.5f,  %.5f".format(pos.latitude, pos.longitude) //.toString() + ", " + pos.longitude.toString()
    }

    override fun findRoute() {
        originSquare?.remove()
        //map.clear()

        if(originMarker == null){
            Toast.makeText(this, "You must select an origin point", Toast.LENGTH_SHORT).show()
            return
        }
        if(controlPanel.getDistance() == null){
            Toast.makeText(this, "You must enter distance tolerance", Toast.LENGTH_SHORT).show()
            return
        }

        val walkingDistance = controlPanel.getDistance()

        if(walkingDistance!! < 0){
            Toast.makeText(this, "distance can't be negative", Toast.LENGTH_SHORT).show()
            return
        }
        //test 1, only with one point
        // draw rect around origin point
        originSquare = map.addPolygon(
            PolygonOptions()
                    .addAll(getSquareFrom(walkingDistance, originMarker!!.position))
                    .strokeColor(Color.BLACK)
                    .fillColor(Color.argb(100,100,100,100))

        )

        val originLat = originMarker!!.position.latitude
        val originLng = originMarker!!.position.longitude
        //find routes within rect
        AsyncTask.execute{

            val routesList = AppDatabase.getInstance(this)?.routesDAO()?.getRoutesIntercepting(walkingDistance, originLat, originLng)
            val adapterItems = arrayListOf<RouteModel>()
            routesList?.forEach{
                adapterItems.add(RouteModel(it))
            }
            runOnUiThread{controlPanel.setAdapterRoutes(adapterItems)}
        }

        //display them on the recycler



    }

    private fun getSquareFrom(distance: Double, center: LatLng): List<LatLng>{
        val points = ArrayList<LatLng>(4)

        points.add(LatLng(center.latitude - distance, center.longitude + distance))
        points.add(LatLng(center.latitude + distance, center.longitude + distance))
        points.add(LatLng(center.latitude + distance, center.longitude - distance))
        points.add(LatLng(center.latitude - distance, center.longitude - distance))

        return points
    }

    override fun drawRoute(route: RouteModel) {
        if (route.polyline != null){
            route.polyline?.isVisible = !route.isDrawed
            route.isDrawed = route.isDrawed.not()

        }else{
            AsyncTask.execute {
                val points = AppDatabase.getInstance(this)?.routesDAO()?.getPointsFrom( route.id)
                val polylineOptions = PolylineOptions()
                polylineOptions.color(Color.parseColor(route.color))
                        .width(LINE_WIDTH)
                        .jointType(JointType.ROUND)

                points?.forEach{
                    polylineOptions.add(LatLng(it.lat, it.lng))
                }

                runOnUiThread{
                    route.polyline = map.addPolyline(polylineOptions)
                    route.isDrawed = true
                }
            }
        }
    }

    private fun locationPermissionEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun askPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this, "Location permission is necesary in order to show your location on the map", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            LOCATION_PERMISSION_REQUEST ->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    map.isMyLocationEnabled = true
                }
            }
        }
    }

    //marker drag events interface
    override fun onMarkerDragStart(m: Marker?) {}

    override fun onMarkerDragEnd(m: Marker?) {
        controlPanel.setOriginDestinationText(latLngToString(originMarker?.position), latLngToString(destinationMarker?.position))
    }

    override fun onMarkerDrag(p0: Marker?) {}
}
