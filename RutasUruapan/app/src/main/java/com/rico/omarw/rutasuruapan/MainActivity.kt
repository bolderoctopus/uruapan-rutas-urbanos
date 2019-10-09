package com.rico.omarw.rutasuruapan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.os.*
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rico.omarw.rutasuruapan.Constants.CAMERA_PADDING_MARKER
import com.rico.omarw.rutasuruapan.Constants.DEBUG_TAG
import com.rico.omarw.rutasuruapan.Constants.INITIAL_WALKING_DISTANCE_TOL
import com.rico.omarw.rutasuruapan.Utils.hideKeyboard
import com.rico.omarw.rutasuruapan.customWidgets.CustomImageButton
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.models.RouteModel
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.coroutines.*
import java.lang.Runnable
import kotlin.collections.ArrayList

//todo: see below
/*
* [] modify database, add direction data
* [] update algorithm, take into consideration direction
* [] improve function walkingDistanceToDest, take into consideration buildings
* [] i think the amount of results to show is currently not working
* [] draw only the relevant part of the route?
* [] sort resulting routes
* [] show tips for using the app
* [] add missing routes 176 y 45
* [] settings: how many results to show?
* [] nextTask: 6 improve color palette: bottomNavBar,
* [] set fragment transitions between search and results
* [] nextTask: 5 improve looks of outside of bounds error, possible create custom Toast
* [] settings: add how many results to show?
* [] nextTask: 2 find a way to differentiate between origin/dest markers
* [] nextTask: 7 what to do if search has been done but a marker is dragged
* [] nextTask: 4 show message when no routes found or that the user should walk
* [] try to draw arrows bigger, maybe change it according to zoom
* [] add lap time
* [] add setting for walking distance tolerance, what type? list? spinner? seekbar?
*/



class MainActivity : AppCompatActivity(), OnMapReadyCallback,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapLongClickListener,
        AllRoutesFragment.InteractionsInterface,
        SearchFragment.OnFragmentInteractionListener,
        ResultsFragment.OnFragmentInteractionListener,
        BottomNavigationView.OnNavigationItemSelectedListener, SlidingUpPanelLayout.PanelSlideListener {
    private val DIRECTIONAL_ARROWS_STEP = 7
    private val URUAPAN_LATLNG = LatLng(19.411843, -102.051518)

    private val DEBUG_SQUARES = false
    private val VIBRATION_DURATION: Long = 75

    private val LOCATION_PERMISSION_REQUEST = 32
    private val LINE_WIDTH = 15f

    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var originSquare: Polygon? = null
    private var destinationSquare: Polygon? = null

    private lateinit var searchFragment: SearchFragment
    private lateinit var allRoutesFragment: AllRoutesFragment
    private var resultsFragment: ResultsFragment? = null
    private lateinit var activeFragment: Fragment

    private lateinit var slidingLayout: SlidingUpPanelLayout
    private lateinit var map: GoogleMap
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var slideIndicator: ImageView
    private var refreshStartTime: Long = 0
    private val REFRESH_INTERVAL: Int = 300// in milliseconds
    private lateinit var startMarkerPosition: LatLng
    private var uiScope = CoroutineScope(Dispatchers.Main)
    private lateinit var bottomNavView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(!Places.isInitialized())
            Places.initialize(this, resources.getString(R.string.google_maps_key))

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        slidingLayout= findViewById(R.id.sliding_layout)
        slideIndicator = findViewById(R.id.imageview_slide_indicator)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation_slide_panel)



        bottomNavView.setOnNavigationItemSelectedListener (this)
        slidingLayout.addPanelSlideListener(this)

        searchFragment = SearchFragment.newInstance()
        allRoutesFragment = AllRoutesFragment.newInstance()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, searchFragment, SearchFragment.TAG).commit()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, allRoutesFragment, AllRoutesFragment.TAG).hide(allRoutesFragment).commit()
        activeFragment = searchFragment

        mapFragment.getMapAsync(this)
    }

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }

    private fun getSearchFragmentHeight(): Int{
        return if(searchFragment.view == null || searchFragment.view!!.height == 0) resources.getDimensionPixelSize(R.dimen.default_fragment_height) else searchFragment.view!!.height
    }

    override fun onNavigationItemSelected(menuitem: MenuItem): Boolean {
        when(menuitem.itemId){
            R.id.menu_item_all_routes ->{
                allRoutesFragment.setHeight(searchFragment.view?.height!!)
                showFragment(allRoutesFragment, AllRoutesFragment.TAG)
            }

            R.id.menu_item_find_route -> if(resultsFragment != null)
                                            showFragment(resultsFragment!!, ResultsFragment.TAG)
                                        else
                                            showFragment(searchFragment, SearchFragment.TAG)
        }
        return true
    }

    private fun showFragment(newFragment: Fragment, tag: String){
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(newFragment)
            .commit()
        activeFragment = newFragment
        // slidingPanel needs to know if it has a scrollable view in order to work the nested scrolling thing
        // when the view is being created, its scrollView is set as the slidingPanel scrollableView
        // other wise, causes not initialized view error
        when(tag){
            AllRoutesFragment.TAG -> slidingLayout.setScrollableView(allRoutesFragment.recyclerView)
            ResultsFragment.TAG -> if(resultsFragment != null) slidingLayout.setScrollableView(resultsFragment?.recyclerView)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapLongClickListener(this)
        map.setOnMarkerDragListener(this)
        //map.setLatLngBoundsForCameraTarget(SearchFragment.uruapanLatLngBounds)// restrict camera to just uruapan?
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(URUAPAN_LATLNG, 13f))
        if (locationPermissionEnabled()){
            map.isMyLocationEnabled = true
        }else {
            askPermission()
        }

        // Moves the Google logo to the top left corner of the screen
        val googleLogo = slidingLayout.findViewWithTag<View>("GoogleWatermark")
        val layoutParams = googleLogo.layoutParams as RelativeLayout.LayoutParams
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        googleLogo.layoutParams = layoutParams

        // Add settings button next to location button
        val locationButton = slidingLayout.findViewWithTag<ImageView>("GoogleMapMyLocationButton")
        val settingsButtonLayoutParams = RelativeLayout.LayoutParams(resources.getDimensionPixelSize(R.dimen.settings_button_size), resources.getDimensionPixelSize(R.dimen.settings_button_size)).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.settings_button_marginEnd)
            addRule(RelativeLayout.ALIGN_TOP, locationButton.id)
            addRule(RelativeLayout.START_OF, locationButton.id)
        }
        val settingsButton = CustomImageButton(this@MainActivity, settingsButtonLayoutParams).apply {
            setOnClickListener{showSettings()}
        }
        (locationButton.parent as RelativeLayout).addView(settingsButton)

        // Observes the drawing events of the bottomNavigationBar to know its height and take it into consideration for the map's bottom padding, then it removes itself.
        val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                map.setPadding(0, 0, 0, getSearchFragmentHeight() + bottomNavView.height)
                bottomNavView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                Log.d(DEBUG_TAG, "bottomNavView.height: ${bottomNavView.height}")
            }
        }
        bottomNavView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun showSettings(){
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun getStatusBarHeight(): Int{
        val rectangle = Rect()
        val window = window
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        return rectangle.top
    }

    private fun showOutOfBoundsError(){
        Toast.makeText(this, "position outside uruapan bounds", Toast.LENGTH_SHORT).show()
    }

    override fun onMapLongClick(pos: LatLng){
        vibrate()
        if(!SearchFragment.uruapanLatLngBounds.contains(pos)){
            showOutOfBoundsError()
            return
        }
        when {
            originMarker == null -> {
                drawMarker(pos, "Origin", SearchFragment.MarkerType.Origin, false)
                searchFragment.oneTimeUpdatePosition(SearchFragment.MarkerType.Origin, pos)
            }
            destinationMarker == null -> {
                drawMarker(pos, "Destination", SearchFragment.MarkerType.Destination, false)
                searchFragment.oneTimeUpdatePosition(SearchFragment.MarkerType.Destination, pos)
            }
            else -> {// remove both
                clearMarker(SearchFragment.MarkerType.Origin)
                clearMarker(SearchFragment.MarkerType.Destination)
                searchFragment.clearInputs()
            }
        }

    }

    private fun latLngToString(pos: LatLng?): String{
        if(pos == null) return ""
        return "%.5f,  %.5f".format(pos.latitude, pos.longitude) //.toString() + ", " + pos.longitude.toString()
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

    //.endCap(CustomCap(getEndCapIcon(Color.parseColor("#82b1ff"))))
    override fun drawRoute(route: RouteModel) {
        if (route.polyline != null){
            route.showLines(route.isDrawed.not())

        }else{
            uiScope.launch {
                val color = Color.parseColor(route.color)
                val arrowCap = getEndCapArrow(color)
                val points = withContext(Dispatchers.IO) {AppDatabase.getInstance(this@MainActivity)?.routesDAO()?.getPointsFrom( route.id)}
                val polylineOptions = PolylineOptions()
                val arrowPolylineOptions = ArrayList<PolylineOptions>()
                polylineOptions
                        .color(color)
                        .width(LINE_WIDTH)
                        .jointType(JointType.ROUND)
                var counter = 0
                // Add small polylines with arrow caps in order to show the direction
                for(x in 0 until (points?.size ?: 0)){
                    val point = points!![x]
                    polylineOptions.add(LatLng(point.lat, point.lng))
                    if(counter == 0 && arrowCap != null){
                        counter = DIRECTIONAL_ARROWS_STEP
                        if(x + 1 < points.size)
                            arrowPolylineOptions.add(PolylineOptions()
                                    .color(Color.TRANSPARENT)
                                    .endCap(CustomCap(arrowCap))
                                    .add(LatLng(point.lat, point.lng), LatLng(points[x+1].lat, points[x+1].lng)))

                    }
                    counter--
                }
                route.polyline = map.addPolyline(polylineOptions)
                route.arrowPolylines = ArrayList()
                arrowPolylineOptions.forEach {
                    route.arrowPolylines?.add(map.addPolyline(it))
                }
                route.isDrawed = true
                Log.d(DEBUG_TAG, "\npoints in route: ${points?.size} \narrows drawn: ${arrowPolylineOptions.size}")
            }
        }
    }

    private fun locationPermissionEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun askPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this, "Location permission is necessary in order to show your location on the map", Toast.LENGTH_SHORT).show()
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

    override fun onMarkerDragStart(m: Marker?) {
        if(m == null) return
        vibrate()
        startMarkerPosition = m.position
        searchFragment.startUpdatePosition(m.tag as SearchFragment.MarkerType, m.position)
    }

    override fun onMarkerDragEnd(m: Marker?) {
        if(m == null) return
        if(SearchFragment.uruapanLatLngBounds.contains(m.position))
            searchFragment.endUpdatePosition(m.tag as SearchFragment.MarkerType, m.position)
        else{
            showOutOfBoundsError()
            m.position = startMarkerPosition
            searchFragment.endUpdatePosition(m.tag as SearchFragment.MarkerType, startMarkerPosition)
        }
    }

    override fun onMarkerDrag(m: Marker?) {
        // Only update display the corresponding location on the textField every REFRESH_INTERVAL in order to prevent greater lag while dragging the marker
        if(System.currentTimeMillis() - refreshStartTime > REFRESH_INTERVAL){
            searchFragment.updatePosition(m?.tag as SearchFragment.MarkerType, m.position)
            refreshStartTime = System.currentTimeMillis()
        }
    }

    @SuppressLint("NewApi")
    private fun getEndCapArrow(color: Int): BitmapDescriptor?{
        val drawable = getDrawable(R.drawable.ic_arrow) ?: return null
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    @SuppressLint("NewApi")
    private fun getBitmapDescriptor(): BitmapDescriptor?{
        val drawable = getDrawable(R.drawable.ic_place) ?: return null
//        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun vibrate(){
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if(!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
        }else{
            vibrator.vibrate(VIBRATION_DURATION)
        }
    }

    private fun getLatLngBoundsFrom(point1: LatLng, point2: LatLng) =
        LatLngBounds.builder().include(point1).include(point2).build()

    override fun onSearch(origin: LatLng, destination: LatLng){
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBoundsFrom(destination, origin), CAMERA_PADDING_MARKER))
        resultsFragment = ResultsFragment.newInstance(getSearchFragmentHeight(), origin, destination, INITIAL_WALKING_DISTANCE_TOL)

        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, resultsFragment!!, ResultsFragment.TAG)
                .hide(searchFragment)
                .commit()
        activeFragment = resultsFragment!!
        resultsFragment?.onViewCreated = Runnable {slidingLayout.setScrollableView(resultsFragment?.recyclerView)}
    }

    private fun getDummyLatLng(): LatLng{
        val latlng: LatLng
        val increment = 0.002
        if(originMarker != null) latlng = LatLng(originMarker!!.position.latitude - increment, originMarker!!.position.longitude)
        else if(destinationMarker != null) latlng = LatLng(destinationMarker!!.position.latitude - increment, destinationMarker!!.position.longitude)
        else latlng = URUAPAN_LATLNG

        return latlng
    }

    override fun drawMarker(position: LatLng?, title: String, markerType: SearchFragment.MarkerType) = drawMarker(position, title, markerType, true)

    private fun drawMarker(position: LatLng?, title: String, markerType: SearchFragment.MarkerType, animate: Boolean) {
        val pos = position ?: getDummyLatLng()

        if(animate)
            map.animateCamera(CameraUpdateFactory.newLatLng(pos))

        if(markerType == SearchFragment.MarkerType.Origin) {
            originMarker?.remove()
            originMarker = map.addMarker(MarkerOptions().title(title).position(pos).icon(getBitmapDescriptor()).draggable(true))
            originMarker?.tag = markerType
        }
        else if(markerType == SearchFragment.MarkerType.Destination) {
            destinationMarker?.remove()
            destinationMarker = map.addMarker(MarkerOptions().title(title).position(pos).draggable(true))
            destinationMarker?.tag = markerType
        }
    }

    override fun clearMarker(markerType: SearchFragment.MarkerType) {
        when(markerType){
            SearchFragment.MarkerType.Origin -> {
                originMarker?.remove()
                originMarker = null
            }
            SearchFragment.MarkerType.Destination -> {
                destinationMarker?.remove()
                destinationMarker = null
            }
        }
    }

    override fun onBackFromResults(){
        supportFragmentManager.beginTransaction()
                .remove(resultsFragment!!)
                .show(searchFragment)
                .commit()
        activeFragment = searchFragment
        slidingLayout.setScrollableView(allRoutesFragment.recyclerView)
        resultsFragment = null
    }

    override fun onPanelSlide(panel: View?, slideOffset: Float) {
        slideIndicator.rotation = 180 * (1-slideOffset)
    }

    override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
        if(newState == SlidingUpPanelLayout.PanelState.COLLAPSED){
            hideKeyboard(this, window.decorView.windowToken)
        }
    }

}
