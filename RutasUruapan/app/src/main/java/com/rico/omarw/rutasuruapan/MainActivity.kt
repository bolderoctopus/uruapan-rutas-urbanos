package com.rico.omarw.rutasuruapan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.os.*
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
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
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.models.RouteModel
import com.sothree.slidinguppanel.SlidingUpPanelLayout

//todo: see below
/*
* [] modify database, add direction data
* [] update algorithm, take into consideration direction
* [] improve function walkingDistanceToDest, take into consideration buildings
*
* [] id think the app crashes if its starting but the screen is blocked
* [] draw only the relevant part of the route?
* [x] if available, use current location as origin
* [] sort resulting routes
* [x] improve origin/destination looks
* [...] overall design
* [] add settings
* [] add missing routes 176 y 45
* [] settings: how many results to show?
* [] replace Asynctasks with coroutines
*
* [...] nextTask: update AutoComplete adapter to offer "Pick location from map option"
*           add callback or something
* */

/*
sub taks
[] improve color palette
[x] size of searchFragment, fab touches the destination
[x] check the shadow of the fab
[x] add drag up indicator, (small view on top of the sliding panel)
[x] fix menu selection thing
[] set fragment transitions between seach and results
[x] code origyn & destination textBoxes
    [x] design and choose functionality
    [x] clear button
    [x] google maps suggestions
    [x] use actual location in suggestions
    [x] create markers
[] fragments lose state
[x] switch scroll view when the thing changes
[x] fix issues when keyboard is shown
    [x] it hides the recyclerview from allRoutesFragment
    [x] more space between the dropdwn in the autocompleteTextView and the editTExt
[x] implement ResultsFragment
    [x] validate before search that markers exist
    [x] pass info to onSearch
    [x] decide where to find route
    [x] display results in resultsFragment
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

    private lateinit var map: GoogleMap
    private lateinit var searchFragment: SearchFragment
    private lateinit var allRoutesFragment: AllRoutesFragment
    private var resultsFragment: ResultsFragment? = null
    private lateinit var slidingLayout: SlidingUpPanelLayout
    private lateinit var locationClient: FusedLocationProviderClient
    private var resultsFragmentActive = false
    private lateinit var slideIndicator: ImageView

    //todo: to delete
    private lateinit var controlPanel: ControlPanelFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(!Places.isInitialized())
            Places.initialize(this, resources.getString(R.string.google_maps_key))

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        slidingLayout= findViewById(R.id.sliding_layout)
        slideIndicator = findViewById(R.id.imageview_slide_indicator)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation_slide_panel)


        searchFragment = SearchFragment.newInstance()

        bottomNavView.setOnNavigationItemSelectedListener (this)
        slidingLayout.addPanelSlideListener(this)

        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, searchFragment)
                .commit()

        mapFragment.getMapAsync(this)
        // when the bottomNavView first becomes visible, set the height of the other fragments
        // according to searchFragment's height
        bottomNavView.post{
            val height = getFragmentHeight()
            allRoutesFragment = AllRoutesFragment.newInstance(height)
//            resultsFragment = ResultsFragment.newInstance(height)
        }
    }

    private fun getFragmentHeight(): Int = if(searchFragment.view == null) 500 else searchFragment.view!!.height

    override fun onNavigationItemSelected(menuitem: MenuItem): Boolean {
        when(menuitem.itemId){
            R.id.menu_item_all_routes -> replaceFragment(allRoutesFragment, AllRoutesFragment.TAG)

            R.id.menu_item_find_route -> if(resultsFragmentActive)
                                            replaceFragment(resultsFragment!!, ResultsFragment.TAG)
                                        else
                                            replaceFragment(searchFragment, SearchFragment.TAG)
        }

        return true
    }

    private fun replaceFragment(newFragment: Fragment, tag: String){
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, newFragment, tag)
            .commit()
        // slidingPanel needs to know if it has a scrollable view in order to work the nested scrolling thing
        // when the view is being created, its scrollView is set as the slidingPanel scrollableView
        // other wise, causes not initialized view error
        when(tag){
            AllRoutesFragment.TAG -> allRoutesFragment.onViewCreated = Runnable{slidingLayout.setScrollableView(allRoutesFragment.recyclerView)}
            ResultsFragment.TAG -> if(resultsFragmentActive) resultsFragment?.onViewCreated = Runnable{slidingLayout.setScrollableView(resultsFragment?.recyclerView)}
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapLongClickListener(this)
        map.setOnMarkerDragListener(this)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(URUAPAN_LATLNG, 13f))
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
        return rectangle.top
    }

    //todo: if there are no origin or destination put marker, and update textView
    override fun onMapLongClick(pos: LatLng){
        vibrate()
//        when {
//            originMarker == null ->
//                originMarker = map.addMarker(MarkerOptions().title("Origin").position(pos).draggable(true))
//
//            destinationMarker == null ->
//                destinationMarker = map.addMarker(MarkerOptions().title("Destination").position(pos).draggable(true))
//
//            else -> {
//                originMarker?.isVisible = false
//                destinationMarker?.isVisible = false
//                originMarker = null
//                destinationMarker = null
//                originSquare?.remove()
//                destinationSquare?.remove()
//                controlPanel.clearRoutes()
//            }
//        }
//        controlPanel.setOriginDestinationText(latLngToString(originMarker?.position), latLngToString(destinationMarker?.position))

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
            AsyncTask.execute {
                val color = Color.parseColor(route.color)
                val arrowCap = getEndCapArrow(color)
                val points = AppDatabase.getInstance(this)?.routesDAO()?.getPointsFrom( route.id)
                val polylineOptions = PolylineOptions()
                val arrowPolylineOptions = ArrayList<PolylineOptions>()

                polylineOptions
                        .color(color)
                        .width(LINE_WIDTH)
                        .jointType(JointType.ROUND)

                var counter = 0
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

                runOnUiThread{
                    route.polyline = map.addPolyline(polylineOptions)
                    route.arrowPolylines = ArrayList<Polyline>()
                    arrowPolylineOptions.forEach {
                        route.arrowPolylines?.add(map.addPolyline(it))
                    }
                    route.isDrawed = true

                    Log.d(DEBUG_TAG, "\npoints in route: ${points?.size} \narrows drawn: ${arrowPolylineOptions.size}")
                }
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

    //marker drag events interface
    override fun onMarkerDragStart(m: Marker?) {
        Log.d(DEBUG_TAG, "onMarkerDragStart, tag: ${m?.tag}")
        if(m?.tag == SearchFragment.MarkerType.Origin){
            searchFragment.origin.autoCompleteTextView.isEnabled = false
            searchFragment.origin.autoCompleteTextView.threshold = 99
        }
        else if(m?.tag == SearchFragment.MarkerType.Destination){
            searchFragment.destination.autoCompleteTextView.isEnabled = false
            searchFragment.destination.autoCompleteTextView.threshold = 99
        }
    }

    //nextTask: request place according to position and update textView
    override fun onMarkerDragEnd(m: Marker?) {
        if(m?.tag == SearchFragment.MarkerType.Origin){
            searchFragment.origin.autoCompleteTextView.isEnabled = true
            searchFragment.origin.autoCompleteTextView.threshold = COMPLETION_THRESHOLD
        }
        else if(m?.tag == SearchFragment.MarkerType.Destination){
            searchFragment.destination.autoCompleteTextView.isEnabled = true
            searchFragment.destination.autoCompleteTextView.threshold = COMPLETION_THRESHOLD
        }

        if(m?.tag != null)
        searchFragment.findPlaceByLatLng(m.tag as SearchFragment.MarkerType, m.position)
    }
    override fun onMarkerDrag(m: Marker?) {
        //todo: fix lag caused by this block
        if(m?.tag == SearchFragment.MarkerType.Origin){
            searchFragment.origin.autoCompleteTextView.setText(getString(R.string.lat_lng, m.position.latitude, m.position.longitude))
            searchFragment.originLatLng = m.position
        }
        else if(m?.tag == SearchFragment.MarkerType.Destination){
            searchFragment.destination.autoCompleteTextView.setText(getString(R.string.lat_lng, m.position.latitude, m.position.longitude))
            searchFragment.destinationLatLng = m.position
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

    private fun vibrate(){
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if(!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
        }else{
            vibrator.vibrate(VIBRATION_DURATION)
        }
    }

    override fun onSearch(origin: LatLng, destination: LatLng){
        resultsFragmentActive = true
        resultsFragment = ResultsFragment.newInstance(getFragmentHeight(), origin, destination, INITIAL_WALKING_DISTANCE_TOL)
        replaceFragment(resultsFragment!!, ResultsFragment.TAG)
    }

//    @SuppressLint("MissingPermission")
//    override fun onGetCurLocation(onSuccess: (Location) -> Unit){
//        try{
//            if(locationPermissionEnabled()){
//                locationClient.lastLocation.addOnCompleteListener {
//                    if(it.isSuccessful && it.result != null){
//                        onSuccess.invoke(it.result!!)
//                    }else{
//                        Log.d(DEBUG_TAG,"unable to find location")
//                    }
//                }
//
//            }else{
//                Toast.makeText(this, "No location permission", Toast.LENGTH_SHORT).show()
//            }
//        }catch (excep: Exception){
//            Log.d(DEBUG_TAG, "error while requesting current location",excep)
//        }
//    }

    private fun getDummyLatLng(): LatLng{
        val latlng: LatLng
        val increment = 0.002
        if(originMarker != null) latlng = LatLng(originMarker!!.position.latitude - increment, originMarker!!.position.longitude)
        else if(destinationMarker != null) latlng = LatLng(destinationMarker!!.position.latitude - increment, destinationMarker!!.position.longitude)
        else latlng = URUAPAN_LATLNG

        return latlng
    }

    override fun drawMarker(position: LatLng?, title: String, markerType: SearchFragment.MarkerType) {
        val pos = position ?: getDummyLatLng()

        map.animateCamera(CameraUpdateFactory.newLatLng(pos))

        if(markerType == SearchFragment.MarkerType.Origin) {
            originMarker?.remove()
            originMarker = map.addMarker(MarkerOptions().title(title).position(pos).draggable(true))
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
            SearchFragment.MarkerType.Origin -> originMarker?.remove()
            SearchFragment.MarkerType.Destination -> destinationMarker?.remove()
        }
    }

    override fun onBackFromResults(){
        resultsFragmentActive = false
        replaceFragment(searchFragment, SearchFragment.TAG)
    }

    override fun onPanelSlide(panel: View?, slideOffset: Float) {
        slideIndicator.rotation = 180 * (1-slideOffset)
    }

    override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) = Unit

}
