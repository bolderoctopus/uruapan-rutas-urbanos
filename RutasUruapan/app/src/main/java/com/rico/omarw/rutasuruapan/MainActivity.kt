package com.rico.omarw.rutasuruapan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.os.*
import android.util.SparseArray
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.rico.omarw.rutasuruapan.Constants.BOUNCE_DURATION
import com.rico.omarw.rutasuruapan.Constants.CAMERA_PADDING_MARKER
import com.rico.omarw.rutasuruapan.Constants.INITIAL_ZOOM
import com.rico.omarw.rutasuruapan.Constants.LINE_WIDTH
import com.rico.omarw.rutasuruapan.Constants.LOCATION_PERMISSION_REQUEST
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys
import com.rico.omarw.rutasuruapan.Constants.REFRESH_INTERVAL
import com.rico.omarw.rutasuruapan.Constants.URUAPAN_LATLNG
import com.rico.omarw.rutasuruapan.Constants.VIBRATION_DURATION
import com.rico.omarw.rutasuruapan.Utils.hideKeyboard
import com.rico.omarw.rutasuruapan.customWidgets.CustomImageButton
import com.rico.omarw.rutasuruapan.customWidgets.showOutOfBoundsSnack
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.database.Point
import com.rico.omarw.rutasuruapan.databinding.ActivityMainBinding
import com.rico.omarw.rutasuruapan.models.RouteModel
import com.rico.omarw.rutasuruapan.models.ZoomLevel
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapLongClickListener,
        AllRoutesFragment.InteractionsInterface,
        SearchFragment.OnFragmentInteractionListener,
        ResultsFragment.OnFragmentInteractionListener,
        GoogleMap.OnCameraMoveListener,
        TabLayout.OnTabSelectedListener {

    private lateinit var binding: ActivityMainBinding

    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var originSquare: Polygon? = null
    private var destinationSquare: Polygon? = null

    private lateinit var searchFragment: SearchFragment
    private lateinit var allRoutesFragment: AllRoutesFragment
    private var resultsFragment: ResultsFragment? = null
    private lateinit var activeFragment: Fragment

    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheet: LinearLayout
    private lateinit var map: GoogleMap
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var slideIndicator: ImageView
    private var refreshStartTime: Long = 0

    private lateinit var startMarkerPosition: LatLng
    private var uiScope = CoroutineScope(Dispatchers.Main)
    private val drawnRoutes = ArrayList<RouteModel>()
    private var mapZoomLevel: Int = INITIAL_ZOOM.toInt()
    private var mapHeight: Int? = null
    var showInformativeDialog: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!Places.isInitialized()){
            val metaData = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
            Places.initialize(this, metaData.getString("com.google.android.geo.API_KEY")!!)
        }


        locationClient = LocationServices.getFusedLocationProviderClient(this)

        showInformativeDialog = !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.DIALOG_1_SHOWN, false)
        val showDisclaimer = !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.DISCLAIMER_SHOWN, false)

        bottomSheet = findViewById(R.id.bottom_sheet)
        sheetBehavior = BottomSheetBehavior.from(bottomSheet)
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        slideIndicator = findViewById(R.id.imageview_slide_indicator)
        (slideIndicator.parent as View).setOnClickListener {
            when (sheetBehavior.state){
                BottomSheetBehavior.STATE_EXPANDED -> sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                BottomSheetBehavior.STATE_COLLAPSED -> sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment

        binding.tablayout.addOnTabSelectedListener(this)

        sheetBehavior.bottomSheetCallback = sheetBehaviorCallback

        searchFragment = SearchFragment.newInstance()
        allRoutesFragment = AllRoutesFragment.newInstance()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, searchFragment, SearchFragment.TAG).commit()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, allRoutesFragment, AllRoutesFragment.TAG).hide(allRoutesFragment).commit()
        activeFragment = searchFragment

        mapFragment.getMapAsync(this)
        showDisclaimer(showDisclaimer)
    }

    private fun showDisclaimer(show: Boolean) {
        if(show) {
            MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.disclaimer_title)
                    .setMessage(R.string.disclaimer_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.accept) { _, _ ->
                        PreferenceManager.getDefaultSharedPreferences(this@MainActivity).edit()
                                .putBoolean(PreferenceKeys.DISCLAIMER_SHOWN, true)
                                .apply()
                    }
                    .show()
        }
    }

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }

    private fun getSearchFragmentHeight(): Int{
        return if(searchFragment.view == null || searchFragment.view!!.height == 0) resources.getDimensionPixelSize(R.dimen.default_fragment_height) else searchFragment.view!!.height
    }

    private fun showFragment(newFragment: Fragment){
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(newFragment)
            .commit()
        activeFragment = newFragment
    }

    private fun setMarkerBounce(marker: Marker) {
        val handler = Handler()
        val startTime = SystemClock.uptimeMillis()
        val interpolator = BounceInterpolator()
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - startTime
                val t = Math.max(1 - interpolator.getInterpolation((elapsed/ BOUNCE_DURATION)), 0f)

                marker.setAnchor(0.5f, 1.0f +  t)

                if (t > 0f) {
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapLongClickListener(this)
        map.setOnMarkerDragListener(this)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(URUAPAN_LATLNG, INITIAL_ZOOM))
        map.setOnCameraMoveListener(this)
        if (locationPermissionEnabled()){
            map.isMyLocationEnabled = true
        }else {
            askPermission()
        }

        // Moves the Google logo to the top left corner of the screen
        val googleLogo = findViewById<View>(R.id.main_content).findViewWithTag<View>("GoogleWatermark")
        val logoLP = googleLogo.layoutParams as RelativeLayout.LayoutParams
        logoLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
        logoLP.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE)
        logoLP.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
        logoLP.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        googleLogo.layoutParams = logoLP

        setupSettingsButton()

        // Observes the drawing events of the slideIndicator to know its height and take it into consideration for the map's bottom padding, then it removes itself.
        // also takes the point on screen of where to show the informative dialog 2
        val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                map.setPadding(0, 0, 0, getSearchFragmentHeight() + slideIndicator.height)
                if(!searchFragment.getHasInformativeDialogBeenShown())
                    mapHeight = (supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment).view?.height

                // Move the GoogleMapCompass below the logo
                val compass = findViewById<View>(R.id.main_content).findViewWithTag<View>("GoogleMapCompass")
                val compassLP = compass.layoutParams as RelativeLayout.LayoutParams
                compassLP.topMargin += googleLogo.height
                compass.layoutParams = compassLP

                slideIndicator.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
        slideIndicator.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun setupSettingsButton(){
        val mainContent = findViewById<View>(R.id.main_content)
        val locationButton = mainContent.findViewWithTag<ImageView>("GoogleMapMyLocationButton")
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

        var settingsButton = mainContent.findViewWithTag<CustomImageButton>(CustomImageButton.TAG)
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

    override fun onMapLongClick(pos: LatLng){
        vibrate()
        if(!SearchFragment.uruapanLatLngBounds.contains(pos)){
            showOutOfBoundsSnack(binding.coordinatorLayout)
            return
        }
        when {
            originMarker == null -> {
                drawMarker(pos, getString(R.string.marker_title_origin), SearchFragment.MarkerType.Origin, animate = false, bounce = false)
                searchFragment.oneTimeUpdatePosition(SearchFragment.MarkerType.Origin, pos)
            }
            destinationMarker == null -> {
                drawMarker(pos, getString(R.string.marker_title_destination), SearchFragment.MarkerType.Destination, animate = false, bounce = false)
                searchFragment.oneTimeUpdatePosition(SearchFragment.MarkerType.Destination, pos)
            }
            else -> {
                clearMarker(SearchFragment.MarkerType.Origin)
                clearMarker(SearchFragment.MarkerType.Destination)
                searchFragment.clearInputs()
                resultsFragment?.backButtonPressed()
            }
        }

    }

    override fun drawRouteResult(route: RouteModel) {
        uiScope.launch {
            if (route.mainSegment != null){
                route.setVisibility(route.isDrawn.not())

            }else{
                val color = Color.parseColor(route.color)
                val points = withContext(Dispatchers.IO) {AppDatabase.getInstance(this@MainActivity)?.routesDAO()?.getPointsFrom( route.id)}
                val mainSegmentPolOpt = PolylineOptions().apply{
                    color(color)
                    width(LINE_WIDTH)
                    zIndex(0.5f)
                    endCap(CustomCap(getBitmapDescriptor(R.drawable.ic_route_endpoint, color)!!))
                    startCap(CustomCap(getBitmapDescriptor(R.drawable.ic_route_startpoint, color)!!))
                    addAll(route.getMainSegment(points!!))
                }
                val secondarySegmentPolOpt = PolylineOptions().apply {
                    color(color)
                    width(LINE_WIDTH/3)
                    jointType(JointType.ROUND)
                    pattern(RouteModel.dashedPatter)
                    addAll(route.getSecondarySegment(points!!))
                }

                route.mainSegment = map.addPolyline(mainSegmentPolOpt)
                route.secondarySegment = map.addPolyline(secondarySegmentPolOpt)
                route.directionalMarkers = drawDirectionalMarkers(route.getMainSegmentPoints(points!!), color)
                drawnRoutes.add(route)

                // for debug purposes
                route.startMarker = drawMarker(route.startPoint!!.getLatLng(), "startPoint")
                route.endMarker = drawMarker(route.endPoint!!.getLatLng(), "endPoint")
                route.mainSegmentMarkers = drawMarkers(route.getMainSegmentPoints(points))

                route.isDrawn = true
            }
            //todo: there could be a better way to update the visible directionalArrows at the current zoom lvl
            updateShownDirectionalArrows(0, mapZoomLevel, route, routeVisible = route.isDrawn)
        }
    }

    override fun drawRoute(route: RouteModel) {
        uiScope.launch {
            if (route.polyline != null){
                route.setVisibility(route.isDrawn.not())

            }else{
                    val color = Color.parseColor(route.color)
                    val points = withContext(Dispatchers.IO) {AppDatabase.getInstance(this@MainActivity)?.routesDAO()?.getPointsFrom( route.id)}
                    val polylineOptions = PolylineOptions()
                    polylineOptions
                            .color(color)
                            .width(LINE_WIDTH)
                            .jointType(JointType.ROUND)
                    points?.forEach { polylineOptions.add(it.getLatLng()) }
                    route.polyline = map.addPolyline(polylineOptions)

                    route.directionalMarkers = drawDirectionalMarkers(points!!, color)
                    drawnRoutes.add(route)
                    // for debug
                    route.mainSegmentMarkers = drawMarkers(points)

                    route.isDrawn = true
            }
            //todo: there could be a better way to update the visible directionalArrows at the current zoom lvl
            updateShownDirectionalArrows(0, mapZoomLevel, route, routeVisible = route.isDrawn)
        }
    }

    private fun drawDirectionalMarkers(sourcePoints: List<Point>, color: Int): SparseArray<Iterable<Marker>> {
        val arrowCap = getBitmapDescriptor(R.drawable.ic_arrow, color)

        val zoomLvls: List<ZoomLevel> = ArrayList<ZoomLevel>().apply{
            add(ZoomLevel(12, 3200, 0, ArrayList()))
            add(ZoomLevel(13, 1600, 0, ArrayList()))
            add(ZoomLevel(14, 800, 0, ArrayList()))
            add(ZoomLevel(16, 400, 0, ArrayList()))
            add(ZoomLevel(17, 200, 0, ArrayList()))
            add(ZoomLevel(18, 100, 0, ArrayList()))
        }
        var distCounter = 0
        for(x in sourcePoints.indices){
            val point = sourcePoints[x]
            distCounter += point.distanceToNextPoint
            for(z in zoomLvls){
                z.counter += point.distanceToNextPoint
                if(z.counter >= z.distanceInterval && (x + 1 < sourcePoints.size)){
                    map.addMarker(MarkerOptions()
                        .icon(arrowCap)
                        .position(point.getLatLng())
                        .rotation(point.bearingTo(sourcePoints[x+1].lat, sourcePoints[x+1].lng ))
                        .draggable(false)
                        .flat(true)
                        .visible(false))?.let { z.markers.add(it) }
                    val d = z.counter - z.distanceInterval
                    zoomLvls.forEach { if(it.zoomLevel >= z.zoomLevel) it.counter = d }
                    break
                }
            }
        }

        val sparseArray = SparseArray<Iterable<Marker>>()
        for(z in zoomLvls){
            sparseArray.append(z.zoomLevel, z.markers)
        }

        return sparseArray
    }

    private fun locationPermissionEnabled(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun askPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.permission_location_dialog_title)
                    .setMessage(R.string.permission_location_dialog_message)
                    .setPositiveButton(getString(R.string.ok)){ _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setCancelable(true)
                    .show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            LOCATION_PERMISSION_REQUEST ->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    searchFragment.restoreCurrentLocation(null)
                    map.isMyLocationEnabled = true
                    setupSettingsButton()
                }
            }
        }
    }

    override fun onMarkerDragStart(m: Marker) {
        if(m == null) return
        vibrate()
        startMarkerPosition = m.position
        searchFragment.startUpdatePosition(m.tag as SearchFragment.MarkerType, m.position)
        resultsFragment?.startUpdate()
    }

    override fun onMarkerDragEnd(m: Marker) {
        if(m == null) return
        if(!SearchFragment.uruapanLatLngBounds.contains(m.position)){
            showOutOfBoundsSnack(binding.coordinatorLayout)
            m.position = startMarkerPosition
        }
        searchFragment.endUpdatePosition(m.tag as SearchFragment.MarkerType, m.position)
        if(originMarker != null && destinationMarker != null)
            resultsFragment?.endUpdate(originMarker!!.position, destinationMarker!!.position)
    }

    override fun onMarkerDrag(m: Marker) {
        // Only update display the corresponding location on the textField every REFRESH_INTERVAL in order to prevent greater lag while dragging the marker
        if(System.currentTimeMillis() - refreshStartTime > REFRESH_INTERVAL){
            searchFragment.updatePosition(m?.tag as SearchFragment.MarkerType, m.position)
            refreshStartTime = System.currentTimeMillis()
        }
    }

    private fun getBitmapDescriptor(@DrawableRes idRes: Int, color: Int): BitmapDescriptor?{
        val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getDrawable(idRes) ?: return null
        } else {
            resources.getDrawable(idRes)
        }
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
        allRoutesFragment.recyclerView.isNestedScrollingEnabled = false
    }

    fun informativeDialog1Shown(){
        val preferenceEditor = PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()
        preferenceEditor.putBoolean(PreferenceKeys.DIALOG_1_SHOWN, true)
        preferenceEditor.apply()
        showInformativeDialog = false
    }

    private fun getDummyLatLng(): LatLng{
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        val mapCenter = Point(mapFragment.view!!.width/2, mapFragment.view!!.height/2)
        return map.projection.fromScreenLocation(mapCenter)
    }


    override fun drawMarker(position: LatLng?, title: String, markerType: SearchFragment.MarkerType, animate: Boolean, bounce: Boolean) {
        var pos = position ?: getDummyLatLng()
        var shouldAnimate = animate

        if(!SearchFragment.uruapanLatLngBounds.contains(pos)){
            pos = URUAPAN_LATLNG
            shouldAnimate = true
        }

        if(shouldAnimate)
            map.animateCamera(CameraUpdateFactory.newLatLng(pos))

        if(markerType == SearchFragment.MarkerType.Origin) {
            originMarker?.remove()
            originMarker = map.addMarker(MarkerOptions().title(title).position(pos).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).draggable(true))
            originMarker?.tag = markerType
            if(bounce) setMarkerBounce(originMarker!!)
        }
        else if(markerType == SearchFragment.MarkerType.Destination) {
            destinationMarker?.remove()
            destinationMarker = map.addMarker(MarkerOptions().title(title).position(pos).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).draggable(true))
            destinationMarker?.tag = markerType
            if(bounce) setMarkerBounce(destinationMarker!!)
        }


        if((searchFragment.getShowInformativeDialog() && mapHeight != null)){
            // use the map initial height as vertical offset from the bottom
            // because the keyboard doesn't hide immediately and there's no easy way to find out the keyboard's height
            var verticalOffset = mapHeight!!/2
            verticalOffset += resources.getDimension(R.dimen.default_marker_height).toInt()

            InformativeDialog.show(this, verticalOffset, InformativeDialog.Style.Center, R.string.how_to_move_markers_message,
                DialogInterface.OnDismissListener {
                    searchFragment.setHasInformativeDialogBeenShown(true)
                }
            )
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

    override fun onBackFromResults(removedRoutes: List<RouteModel>?){
        if(removedRoutes != null){
            for(removedRoute in removedRoutes)
                drawnRoutes.remove(removedRoute)
        }


        clearSquares()
        supportFragmentManager.beginTransaction()
                .remove(resultsFragment!!)
                .show(searchFragment)
                .commit()
        activeFragment = searchFragment
        resultsFragment = null
        allRoutesFragment.recyclerView.isNestedScrollingEnabled = true
    }

    private val sheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            slideIndicator.rotation = 180 * (1-slideOffset)
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when(newState){
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    hideKeyboard(bottomSheet.context, window.decorView.windowToken)
                    map.setPadding(0, 0, 0, slideIndicator.height)
                }
                BottomSheetBehavior.STATE_EXPANDED -> map.setPadding(0, 0, 0, getSearchFragmentHeight() + slideIndicator.height)
            }
        }

    }

    override fun onBackPressed() {
        if(resultsFragment?.isVisible == true)
            resultsFragment!!.backButtonPressed()
        else
            super.onBackPressed()
    }

    override fun onCameraMove() {
        if (mapZoomLevel != map.cameraPosition.zoom.toInt()) {
            for (route in drawnRoutes){
                if(route.isDrawn)
                    updateShownDirectionalArrows(mapZoomLevel, map.cameraPosition.zoom.toInt(), route)
            }

            mapZoomLevel = map.cameraPosition.zoom.toInt()
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?){
        if(sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED){
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

    override fun onTabSelected(tab: TabLayout.Tab?) {
        when(tab?.position){
            1 ->{
                hideKeyboard(this, window.decorView.windowToken)
                allRoutesFragment.setHeight(searchFragment.view?.height!!)
                allRoutesFragment.recyclerView.isNestedScrollingEnabled = true
                showFragment(allRoutesFragment)
            }
            0 -> {
                if (resultsFragment != null) {
                    showFragment(resultsFragment!!)
                    allRoutesFragment.recyclerView.isNestedScrollingEnabled = false
                }
                else
                    showFragment(searchFragment)
            }
        }
        if(sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED){
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    //currentZoomLvl hasta que nivel de zoom estan visibles los marcadores
    private fun updateShownDirectionalArrows(currentZoomLvl: Int, newZoomLvl: Int, route: RouteModel, routeVisible: Boolean = true){
        if(route.directionalMarkers == null) return

        if(currentZoomLvl < newZoomLvl){
            //routeVisible markers between current and new
            for(x in (currentZoomLvl+1)..newZoomLvl){
                if(route.directionalMarkers!![x] != null) route.directionalMarkers!![x].forEach { it.isVisible = routeVisible }
            }
        }
        else{
            //hide markers between new and current
            for(x in (newZoomLvl+1)..currentZoomLvl){
                if(route.directionalMarkers!![x] != null) route.directionalMarkers!![x].forEach { it.isVisible = false }
            }
        }
    }

    
    /**
     * For debug purpouses
     */
    private fun drawMarker(latLng: LatLng, title: String): Marker? {
        return if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.DRAW_STARTEND_POINTS, false)) null
        else map.addMarker(MarkerOptions().title(title).position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)).draggable(false))
    }

    private fun drawMarkers(points: List<Point>): List<Marker>? {
        return if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.DRAW_ROUTE_POINTS, false))
            null
        else {
            val markers = ArrayList<Marker>()
            points.forEach{
                map.addMarker(MarkerOptions().title(it.number.toString()).position(it.getLatLng()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)).draggable(false))
                    ?.let { it1 -> markers.add(it1) }
            }

            markers
        }
    }

    override fun drawSquares(walkingDistance: Double){
        if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.DRAW_SQUARES, false)) return

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

    private fun clearSquares(){
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
}
