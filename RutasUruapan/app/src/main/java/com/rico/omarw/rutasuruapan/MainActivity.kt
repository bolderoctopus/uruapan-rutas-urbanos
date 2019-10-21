package com.rico.omarw.rutasuruapan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rico.omarw.rutasuruapan.Constants.CAMERA_PADDING_MARKER
import com.rico.omarw.rutasuruapan.Utils.hideKeyboard
import com.rico.omarw.rutasuruapan.customWidgets.CustomImageButton
import com.rico.omarw.rutasuruapan.customWidgets.OutOfBoundsToast
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.database.Point
import com.rico.omarw.rutasuruapan.models.RouteModel
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.coroutines.*
import java.lang.Runnable
import kotlin.collections.ArrayList

//todo: see below
/*
* [x] modify database, add direction data
* [] update algorithm, take into consideration direction
* [] sort resulting routes
* [] draw only the relevant part of the route?
*
* [] add indexes to speed up the db
* [] improve function walkingDistanceToDest, take into consideration buildings
* [] settings: add how many results to show?
* [] i think the amount of results to show is currently not working
* [] show tips for using the app
* [] add missing routes 176 y 45
* [] set fragment transitions between search and results
* [] try to draw arrows bigger, maybe change it according to zoom
* [] add setting for walking distance tolerance, what type? list? spinner? seekbar?
* [] display lap time per route?
* [...] improve allRoutes Fragment look
*       [x] fix spacing with route items on recycler views
*       [] format in a better way the labels
*
* [] decide if new design stays
*
* [x] improve looks of outside of bounds error, possible create custom Toast
* [x] what to do if search has been done but a marker is dragged: recalculate
* [x] find a way to differentiate between origin/dest markers
* [x] SearchFragment: test with a single button "Search/Find Route", how to show all routes then?
* [x] show message when no routes found or that the user should walk
*/



class MainActivity : AppCompatActivity(), OnMapReadyCallback,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapLongClickListener,
        AllRoutesFragment.InteractionsInterface,
        SearchFragment.OnFragmentInteractionListener,
        ResultsFragment.OnFragmentInteractionListener,
        BottomNavigationView.OnNavigationItemSelectedListener, SlidingUpPanelLayout.PanelSlideListener {
    private val DIRECTIONAL_ARROWS_STEP = 3//7
    private val URUAPAN_LATLNG = LatLng(19.411843, -102.051518)

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
        bottomNavView = findViewById(R.id.bottom_navigation_slide_panel)



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

        setupSettingsButton()

        // Observes the drawing events of the bottomNavigationBar to know its height and take it into consideration for the map's bottom padding, then it removes itself.
        val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                map.setPadding(0, 0, 0, getSearchFragmentHeight() + bottomNavView.height)
                // todo: remove this
                onMapLongClick(LatLng(19.422123631224547, -102.07343347370625))
                onMapLongClick(LatLng(19.41543181604419, -102.03406568616629))
                bottomNavView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
        bottomNavView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun setupSettingsButton(){
        val locationButton = slidingLayout.findViewWithTag<ImageView>("GoogleMapMyLocationButton")
        val settingsButtonLayoutParams = RelativeLayout.LayoutParams(resources.getDimensionPixelSize(R.dimen.settings_button_size), resources.getDimensionPixelSize(R.dimen.settings_button_size)).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.settings_button_marginEnd)
            if(locationButton.visibility == View.VISIBLE){
                addRule(RelativeLayout.ALIGN_TOP, locationButton.id)
                addRule(RelativeLayout.START_OF, locationButton.id)
            }else{
                topMargin = resources.getDimensionPixelSize(R.dimen.settings_button_marginEnd)
                addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
            }
        }

        var settingsButton = slidingLayout.findViewWithTag<CustomImageButton>(CustomImageButton.TAG)
        if(settingsButton == null) {
            settingsButton = CustomImageButton(this@MainActivity, settingsButtonLayoutParams).apply {
                setOnClickListener { showSettings() }}
            (locationButton.parent as RelativeLayout).addView(settingsButton)
        }
        else
            settingsButton.layoutParams = settingsButtonLayoutParams
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
        OutOfBoundsToast(this).show()
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
                resultsFragment?.backButtonPressed()
            }
        }

    }

    private fun latLngToString(pos: LatLng?): String{
        if(pos == null) return ""
        return "%.5f,  %.5f".format(pos.latitude, pos.longitude) //.toString() + ", " + pos.longitude.toString()
    }

    override fun drawSquares(walkingDistance: Double){
        if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("draw_squares", Constants.DRAW_SQUARES_DEFAULT)) return

        clearSquares()

        originSquare = map.addPolygon(PolygonOptions()
                .addAll(getSquareFrom(walkingDistance, originMarker!!.position))
                .strokeColor(Color.BLACK)
                .fillColor(Color.argb(100,100,100,100)))
        destinationSquare = map.addPolygon(PolygonOptions()
                .addAll(getSquareFrom(walkingDistance, destinationMarker!!.position))
                .strokeColor(Color.BLACK)
                .fillColor(Color.argb(100,100,100,100)))

    }

    fun clearSquares(){
        originSquare?.remove()
        destinationSquare?.remove()
    }

    private fun getSquareFrom(distance: Double, center: LatLng): List<LatLng>{
        val points = ArrayList<LatLng>(4)

        points.add(LatLng(center.latitude - distance, center.longitude + distance))
        points.add(LatLng(center.latitude + distance, center.longitude + distance))
        points.add(LatLng(center.latitude + distance, center.longitude - distance))
        points.add(LatLng(center.latitude - distance, center.longitude - distance))

        return points
    }

    override fun drawRouteResult(route: RouteModel) {
        if (route.mainSegment != null){
            route.setVisibility(route.isDrawed.not())

        }else{
            uiScope.launch {
                val color = Color.parseColor(route.color)
                val points = withContext(Dispatchers.IO) {AppDatabase.getInstance(this@MainActivity)?.routesDAO()?.getPointsFrom( route.id)}
                val mainSegmentPolOpt = PolylineOptions().apply{
                    color(color)
                    width(LINE_WIDTH)
                    addAll(route.getMainSegment(points!!))
                }
                val possibleSecondaryPolOpt = PolylineOptions().apply {
                    color(color)
                    width(LINE_WIDTH/4)
                    jointType(JointType.ROUND)
                    pattern(RouteModel.dashedPatter)
                    addAll(route.getSecondarySegment(points!!))
                }


                route.mainSegment = map.addPolyline(mainSegmentPolOpt)
                route.secondarySegment = map.addPolyline(possibleSecondaryPolOpt)
                route.arrowPolylines = getArrowPolylines(route.getMainSegmentPoints(points!!), color)
                route.startMarker = drawMarker(route.startPoint!!.getLatLng(), "startPoint")
                route.endMarker = drawMarker(route.endPoint!!.getLatLng(), "endPoint")
                route.mainSegmentMarkers = drawMarkers(route.getMainSegmentPoints(points))

                route.isDrawed = true
            }
        }
    }

    override fun drawRoute(route: RouteModel) {
        if (route.polyline != null){
            route.setVisibility(route.isDrawed.not())

        }else{
            uiScope.launch {
                val color = Color.parseColor(route.color)
                val points = withContext(Dispatchers.IO) {AppDatabase.getInstance(this@MainActivity)?.routesDAO()?.getPointsFrom( route.id)}
                val polylineOptions = PolylineOptions()
                polylineOptions
                        .color(color)
                        .width(LINE_WIDTH)
                        .jointType(JointType.ROUND)
                points?.forEach { polylineOptions.add(it.getLatLng()) }
                route.polyline = map.addPolyline(polylineOptions)
                route.arrowPolylines = getArrowPolylines(points!!, color)
                route.isDrawed = true
            }
        }
    }

    /**
     * Add small polylines with arrow caps in order to show the direction
     */
    private fun getArrowPolylines(points: List<Point>, color: Int ): List<Polyline>{
        var counter = 0
        val arrowCap = getEndCapArrow(color)
        val arrowPolylineOptions = ArrayList<PolylineOptions>()
        val arrowPolylines = ArrayList<Polyline>()

        for(x in 0 until points.size){
            val point = points[x]
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


        arrowPolylineOptions.forEach {
            arrowPolylines.add(map.addPolyline(it))
        }
        return arrowPolylines
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
                    setupSettingsButton()
                }
            }
        }
    }

    override fun onMarkerDragStart(m: Marker?) {
        if(m == null) return
        vibrate()
        startMarkerPosition = m.position
        searchFragment.startUpdatePosition(m.tag as SearchFragment.MarkerType, m.position)
        resultsFragment?.startUpdate()
    }

    override fun onMarkerDragEnd(m: Marker?) {
        if(m == null) return
        if(!SearchFragment.uruapanLatLngBounds.contains(m.position)){
            showOutOfBoundsError()
            m.position = startMarkerPosition
        }
        searchFragment.endUpdatePosition(m.tag as SearchFragment.MarkerType, m.position)
        if(originMarker != null && destinationMarker != null)
            resultsFragment?.endUpdate(originMarker!!.position, destinationMarker!!.position)
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
        resultsFragment = ResultsFragment.newInstance(getSearchFragmentHeight(), origin, destination)

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
            originMarker = map.addMarker(MarkerOptions().title(title).position(pos).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).draggable(true))
            originMarker?.tag = markerType
        }
        else if(markerType == SearchFragment.MarkerType.Destination) {
            destinationMarker?.remove()
            destinationMarker = map.addMarker(MarkerOptions().title(title).position(pos).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).draggable(true))
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
        clearSquares()
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

    override fun onBackPressed() {
        if(resultsFragment?.isVisible == true)
            resultsFragment!!.backButtonPressed()
        else
            super.onBackPressed()
    }

    /**
     * For debug purpouses
     */
    fun drawMarker(latLng: LatLng, title: String): Marker? {
        return if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("draw_startend_points", Constants.DRAW_STARTEND_POINTS)) null
        else map.addMarker(MarkerOptions().title(title).position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)).draggable(false))
    }

    fun drawMarkers(points: List<Point>): List<Marker>? {
        if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("draw_route_points", Constants.DRAW_ROUTE_POINTS))
            return null
        else {
            val markers = ArrayList<Marker>()
            points.forEach{
                markers.add(map.addMarker(MarkerOptions().title(it.number.toString()).position(it.getLatLng()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)).draggable(false)))
            }

            return markers
        }
    }
}
