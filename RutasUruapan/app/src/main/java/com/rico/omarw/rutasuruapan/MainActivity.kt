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
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import android.graphics.Rect
import android.util.Log
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.rico.omarw.rutasuruapan.database.Routes
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_control_panel.*

//todo: see below
/*
* [x] clear routes before search
* [x] add option to show all the routes
* [] modify database, add direction data
* [] draw routes with direction
* [] update algorithm, take into consideration direction
* [] if available, use current location as origin
* [] improve function walkingDistanceToDest, take into consideration buildings
*
* [] sort resulting routes
* [] improve origin/destination looks
* [] overall design
* [] add settings
* */



class MainActivity : AppCompatActivity(), OnMapReadyCallback,
        ControlPanelFragment.Listener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapLongClickListener,
        RouteListAdapter.Listener, ViewPager.OnPageChangeListener {

    private val INITIAL_WALKING_DISTANCE_TOL = 0.001
    private val WALKING_DISTANCE_INCREMENT: Double = 0.001
    private val MAX_WALKING_DISTANCE = 0.05// should be configurable
    private val DEBUG_SQUARES = false
    private val MAX_AMOUNT_ROUTES = 3

    private val LOCATION_PERMISSION_REQUEST = 32
    private val LINE_WIDTH = 15f

    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var originSquare: Polygon? = null
    private var destinationSquare: Polygon? = null

    //views
    private lateinit var map: GoogleMap
    private lateinit var controlPanel: ControlPanelFragment
    private lateinit var allRoutesFragment: AllRoutesFragment
    private lateinit var slidingLayout: SlidingUpPanelLayout
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        slidingLayout= findViewById(R.id.sliding_layout)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        viewPager = findViewById(R.id.viewpager)
        tabLayout = findViewById(R.id.tablayout)
        controlPanel = ControlPanelFragment.newInstance()
        allRoutesFragment = AllRoutesFragment.newInstance()

        val fragments = ArrayList<Fragment>()
        fragments.add(controlPanel)
        fragments.add(allRoutesFragment)
        viewPager.adapter = ViewPagerAdapter(supportFragmentManager, fragments)
        viewPager.addOnPageChangeListener(this)
        tabLayout.setupWithViewPager(viewPager)

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
                originSquare?.remove()
                destinationSquare?.remove()
                controlPanel.clearRoutes()
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
        destinationSquare?.remove()
        controlPanel.setDistanceToBusStop(INITIAL_WALKING_DISTANCE_TOL)
        controlPanel.clearRoutes()

        if(originMarker == null || destinationMarker == null){
            Toast.makeText(this, "You must select an origin and destination point", Toast.LENGTH_SHORT).show()
            return
        }

        if(controlPanel.getWalkingDistTolerance() == null){
            Toast.makeText(this, "You must enter distance tolerance", Toast.LENGTH_SHORT).show()
            return
        }

        var walkingDistanceTolerance = controlPanel.getWalkingDistTolerance()

        if(walkingDistanceTolerance!! < 0){
            Toast.makeText(this, "distance can't be negative", Toast.LENGTH_SHORT).show()
            return
        }

        drawSquares(walkingDistanceTolerance)

        val originLatLng = originMarker!!.position
        val destinationLatLng = destinationMarker!!.position
        val walkingDistToDest = walkingDistance(originLatLng, destinationLatLng)
        Log.d("findRoute", "walkingDistToDest: $walkingDistToDest")
        Log.d("findRoute", "walkingDistanceTolerance*2: $walkingDistanceTolerance*2")
        controlPanel.activateLoadingMode(true)
        //todo: if the distance that you need to walk to the bus stop is greater or equal than the distance
        //to the other point then maybe you should walk
        AsyncTask.execute{
            val routesDao = AppDatabase.getInstance(this)?.routesDAO()
            var mutualRoutes: Set<Long>? = null
            while(walkingDistToDest > walkingDistanceTolerance * 2){
                Log.d("findRoute", "walkingDistToDest: $walkingDistToDest walkingDistanceTolerance: $walkingDistanceTolerance")
                val routesNearOrigin = routesDao?.getRoutesIntercepting(walkingDistanceTolerance, originLatLng.latitude, originLatLng.longitude)
                val routesNearDest = routesDao?.getRoutesIntercepting(walkingDistanceTolerance, destinationLatLng.latitude, destinationLatLng.longitude)
                walkingDistanceTolerance += WALKING_DISTANCE_INCREMENT

                if(routesNearDest.isNullOrEmpty() || routesNearOrigin.isNullOrEmpty()) continue
                mutualRoutes = routesNearOrigin.intersect(routesNearDest)
                if(mutualRoutes.size > MAX_AMOUNT_ROUTES) break
            }

            if(mutualRoutes.isNullOrEmpty())
                runOnUiThread{
                    Toast.makeText(this, "No routes found, maybe you should walk", Toast.LENGTH_SHORT).show()
                    drawSquares(walkingDistanceTolerance)
                    controlPanel.activateLoadingMode(false)
                }
            else{
                val routesInfo = routesDao?.getRoutes(mutualRoutes.toList())
                val adapterItems = arrayListOf<RouteModel>()
                routesInfo?.forEach{
                    adapterItems.add(RouteModel(it))
                }
                runOnUiThread{
                    controlPanel.setAdapterRoutes(adapterItems)
                    controlPanel.setDistanceToBusStop(walkingDistanceTolerance)
                    controlPanel.activateLoadingMode(false)
                    Toast.makeText(this, "route found", Toast.LENGTH_SHORT).show()
                    drawSquares(walkingDistanceTolerance)
                }
            }
        }
    }

    //todo: take in consideration streets
    private fun walkingDistance(from: LatLng, to: LatLng): Double {
        val d1 = (from.latitude - to.latitude)
        val d2 = (from.longitude - to.longitude)
        return Math.sqrt(d1 * d1 + d2 * d2)
    }

    private fun drawSquares(walkingDistance: Double){
        if(!DEBUG_SQUARES) return

        originSquare?.remove()
        destinationSquare?.remove()

        originSquare = map.addPolygon(PolygonOptions()
                .addAll(getSquareFrom(walkingDistance, originMarker!!.position))
                .strokeColor(Color.BLACK)
                .fillColor(Color.argb(100,100,100,100)))
        destinationSquare = map.addPolygon(PolygonOptions()
                .addAll(getSquareFrom(walkingDistance, destinationMarker!!.position))
                .strokeColor(Color.BLACK)
                .fillColor(Color.argb(100,100,100,100)))

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

    override fun clear() {
        map.clear()
        controlPanel.setOriginDestinationText(latLngToString(originMarker?.position), latLngToString(destinationMarker?.position))
        controlPanel.setDistanceToBusStop(0.001)
        controlPanel.setAdapterRoutes(List(0) {RouteModel(Routes("","", ""))})
    }

    override fun onPageSelected(position: Int) {
        when(position){
            0 -> slidingLayout.setScrollableView(controlPanel.recyclerView)
            1 -> slidingLayout.setScrollableView(allRoutesFragment.recyclerView)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {}
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
}
