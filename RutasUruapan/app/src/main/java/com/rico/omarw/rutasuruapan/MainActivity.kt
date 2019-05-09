package com.rico.omarw.rutasuruapan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.rico.omarw.rutasuruapan.Database.AppDatabase
import com.sothree.slidinguppanel.SlidingUpPanelLayout

class MainActivity : AppCompatActivity(), OnMapReadyCallback, RouteListAdapter.ListCallback, ControlPanel.OnFragmentInteractionListener{
    private val LOCATION_PERMISSION_REQUEST = 32
    private val LINE_WIDTH = 15f
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

    private lateinit var mMap: GoogleMap
    private lateinit var controlPanel: ControlPanel
    private lateinit var slidingLayout: SlidingUpPanelLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        controlPanel = supportFragmentManager.findFragmentById(R.id.fragment_control_panel) as ControlPanel
        slidingLayout= findViewById(R.id.sliding_layout)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
//
//        recyclerView.post { fillList() }
    }

//    private fun fillList() {
//        AsyncTask.execute{
//            val routesDao = AppDatabase.getInstance(this)?.routesDAO()
//            val dbRoutes = routesDao?.getRoutes()
//            val adapterItems = arrayListOf<RouteModel>()
//            Log.d(DEBUG_TAG, "adapterIitems size: ${adapterItems.size}")
//            dbRoutes?.forEach{
//                adapterItems.add(RouteModel(it))
//            }
//
//            runOnUiThread {
//                recyclerView.layoutManager = LinearLayoutManager(this)
//                recyclerView.adapter = RouteListAdapter(adapterItems, this, this)
//            }
//        }
//    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this::onMapLongClick)

        val uruapan = LatLng(19.411843, -102.051518)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uruapan, 13f))
        if (locationPermissionEnabled()){
            mMap.isMyLocationEnabled = true
        }else {
            askPermission()
        }
    }

    private fun onMapLongClick(pos: LatLng){
        when {
            originMarker == null ->
                originMarker = mMap.addMarker(MarkerOptions().title("Origin").position(pos))

            destinationMarker == null ->
                destinationMarker = mMap.addMarker(MarkerOptions().title("Destination").position(pos))

            else -> {
                originMarker?.isVisible = false
                destinationMarker?.isVisible = false
                originMarker = null
                destinationMarker = null
            }
        }
        controlPanel.setOriginDestinationText(originMarker?.title, destinationMarker?.title)
    }

    override fun findRoute() {
        if(originMarker == null && destinationMarker == null){
            Toast.makeText(this, "You must select destination and origin", Toast.LENGTH_SHORT).show()
            return
        }



    }

    override fun drawRoute(model: RouteModel) {
        if (model.polyline != null){
            model.polyline?.isVisible = !model.isDrawed
            model.isDrawed = model.isDrawed.not()

        }else{
            AsyncTask.execute {
                val points = AppDatabase.getInstance(this)?.routesDAO()?.getPointsFrom( model.id)
                val polylineOptions = PolylineOptions()
                Log.d(DEBUG_TAG, model.color);
                polylineOptions.color(Color.parseColor(model.color))
                        .width(LINE_WIDTH)
                        .jointType(JointType.ROUND)

                points?.forEach{
                    polylineOptions.add(LatLng(it.lat, it.lng))
                }


                runOnUiThread{
                    model.polyline = mMap.addPolyline(polylineOptions)
                    model.isDrawed = true
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
                    mMap.isMyLocationEnabled = true
                }
            }
        }
    }
}
