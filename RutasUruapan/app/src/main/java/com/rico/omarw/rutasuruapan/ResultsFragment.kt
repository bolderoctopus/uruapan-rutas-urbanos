package com.rico.omarw.rutasuruapan

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.MaterialToolbar
import com.rico.omarw.rutasuruapan.Constants.METER_IN_ANGULAR_LAT_LNG
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys
import com.rico.omarw.rutasuruapan.Constants.WALK_DIST_LIMIT_DEFAULT
import com.rico.omarw.rutasuruapan.adapters.RouteListAdapter
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.database.Point
import com.rico.omarw.rutasuruapan.database.Route
import com.rico.omarw.rutasuruapan.models.RouteModel
import kotlinx.coroutines.*
import kotlin.math.sqrt

class ResultsFragment : Fragment(), RouteListAdapter.DrawRouteListener{

    private lateinit var recyclerView: RecyclerView
    private lateinit var groupWalkMessage: LinearLayout
    private lateinit var originLatLng: LatLng
    private lateinit var destinationLatLng: LatLng
    private lateinit var progressBar: ProgressBar
    private lateinit var materialToolbar: MaterialToolbar
    private var height: Int? = null
    private var listener: OnFragmentInteractionListener? = null
    private var drawnRoutes: ArrayList<RouteModel>? = null

    private var uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            height = it.getInt(HEIGHT_KEY)
            originLatLng = it.getParcelable(ORIGIN_LATLNG_KEY)
            destinationLatLng = it.getParcelable(DESTINATION_LATLNG_KEY)
        }

        if(!uiScope.isActive)
            uiScope = CoroutineScope(Dispatchers.Main)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_results, container, false)

        materialToolbar = view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { backButtonPressed() }
        }

        view.findViewById<TextView>(R.id.textview_increase_distance).text = getString(R.string.increase_distance_limit, getString(R.string.title_activity_settings))

        progressBar = view.findViewById(R.id.progressBar)
        groupWalkMessage = view.findViewById(R.id.group_walk_message)
        recyclerView = view.findViewById(R.id.recyclerView_results)

        if(height != null)
            view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height!!)

        if ((activity as MainActivity).showInformativeDialog)
            addRecyclerViewLayoutListener()

        findRoutesAsync(originLatLng, destinationLatLng, getWalkDistLimit())

        return view
    }

    fun backButtonPressed(){
        clearDrawnRoutes()
        listener?.onBackFromResults(drawnRoutes)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        clearDrawnRoutes()
        uiScope.cancel()
        listener = null
        super.onDetach()
    }

    private fun addRecyclerViewLayoutListener(){
        val listener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                if((activity as MainActivity).showInformativeDialog && isVisible && top != 0 && v!= null && (recyclerView.adapter?.itemCount ?: 0) > 0 ){
                    var verticalOffset = recyclerView.height
                    verticalOffset -= resources.getDimension(R.dimen.collapsed_panel_height).toInt()
                    verticalOffset -= resources.getDimension(R.dimen.toolbar_height).toInt()

                    InformativeDialog.show(v.context,
                            verticalOffset,
                            InformativeDialog.Style.Left,
                            R.string.how_to_show_routes_message,
                            DialogInterface.OnDismissListener { (activity as MainActivity).informativeDialog1Shown() })
                    recyclerView.removeOnLayoutChangeListener(this)
                }
            }
        }
        recyclerView.addOnLayoutChangeListener(listener)
    }

    private fun distanceBetweenPoints(p1: LatLng, p2: LatLng): Double {
        val d1 = (p1.latitude - p2.latitude)
        val d2 = (p1.longitude - p2.longitude)
        return sqrt(d1 * d1 + d2 * d2)
    }

    /**
     *
     * @param originLatLng Start point
     * @param destinationLatLng End point
     * @param walkDistLimit How much the user is willing to walk between the route start/end point and the given origin/destination respectively
     */
    private fun findRoutesAsync(originLatLng: LatLng, destinationLatLng: LatLng, walkDistLimit: Double){
        showProgressBar()
        if(walkDistLimit <= 0) throw Exception("walkDistLimit must be a greater than 0")
        listener?.drawSquares(walkDistLimit)

        uiScope.launch {
            val routesDao = AppDatabase.getInstance(context!!)?.routesDAO()
            val commonRoutesIds: Set<Long>?

            val routesNearOrigin = async(Dispatchers.IO) { routesDao?.getRoutesIntercepting(walkDistLimit, originLatLng.latitude, originLatLng.longitude)}
            val routesNearDest = async(Dispatchers.IO) { routesDao?.getRoutesIntercepting(walkDistLimit, destinationLatLng.latitude, destinationLatLng.longitude)}
            awaitAll(routesNearDest, routesNearOrigin)
            commonRoutesIds = routesNearOrigin.await()!!.intersect(routesNearDest.await()!!)

            if(commonRoutesIds.isNullOrEmpty()){
                 showNoResultsMessage()
            }
            else{
                val results = arrayListOf<RouteModel>()
                for(routeId in commonRoutesIds){
                    val route: Route? = withContext(Dispatchers.IO){ routesDao?.getRoute(routeId)}

                    val startPoint: Point? = withContext(Dispatchers.IO){routesDao?.getNearestPointTo(routeId, originLatLng.latitude, originLatLng.longitude, walkDistLimit)}
                    val endPoint: Point? = withContext(Dispatchers.IO){routesDao?.getNearestPointTo(routeId, destinationLatLng.latitude, destinationLatLng.longitude, walkDistLimit)}

                    val betterEndPoint: Point? = withContext(Dispatchers.IO){routesDao?.findBestEndPoint(routeId, destinationLatLng.latitude, destinationLatLng.longitude, startPoint!!.number, walkDistLimit)}
                    val betterStartPoint: Point? = withContext(Dispatchers.IO){routesDao?.findBestStartPoint(routeId, originLatLng.latitude, originLatLng.longitude, endPoint!!.number, walkDistLimit)}

                    val routeDist = withContext(Dispatchers.IO){ routesDao?.getRouteDist(routeId, betterStartPoint!!.number, betterEndPoint!!.number)} ?: 0

                    val routeModel = RouteModel(route!!)
                    routeModel.startPoint = betterStartPoint
                    routeModel.endPoint = betterEndPoint
                    routeModel.walkDist = distanceBetweenPoints(originLatLng, betterStartPoint!!.getLatLng()) + distanceBetweenPoints(destinationLatLng, betterEndPoint!!.getLatLng())
                    routeModel.totalDist = routeModel.walkDist!! + routeDist
                    results.add(routeModel)

                }
                results.sortBy {
                    it.walkDist
                }

                displayRoutes(results)
            }
        }
    }

    private fun clearDrawnRoutes() = drawnRoutes?.forEach{it.remove()}

    private fun displayRoutes(results: ArrayList<RouteModel>){
        groupWalkMessage.visibility = View.GONE
        hideProgressBar()

        recyclerView.visibility = View.VISIBLE
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RouteListAdapter(results, this)
        recyclerView.scheduleLayoutAnimation()

        updateTitle(results.size)
    }

    private fun showNoResultsMessage(){
        hideProgressBar()
        updateTitle(0)
        recyclerView.visibility = View.GONE
        groupWalkMessage.visibility = View.VISIBLE
    }

    private fun updateTitle(amountOfResults: Int?){
        if (amountOfResults == null)
            materialToolbar.title = ""
        else
            materialToolbar.title = resources.getQuantityString(R.plurals.title_results, amountOfResults, amountOfResults)
    }

    private fun hideProgressBar(){
        if(progressBar.visibility == View.VISIBLE)
            progressBar.animate().scaleY(0f).withEndAction { progressBar.visibility = View.GONE }.start()
    }

    private fun showProgressBar(){
        if(progressBar.visibility != View.VISIBLE) {
            progressBar.visibility = View.VISIBLE
            progressBar.animate().scaleY(1f).start()
        }
    }

    override fun drawRouteResult(route: RouteModel) {
        if(drawnRoutes == null) drawnRoutes = ArrayList()
        drawnRoutes!!.add(route)
        listener?.drawRouteResult(route)
    }

    fun startUpdate(){
        recyclerView.visibility = View.INVISIBLE
        clearDrawnRoutes()
        updateTitle(null)
    }

    fun endUpdate(origin: LatLng, destination: LatLng){
        findRoutesAsync(origin, destination, getWalkDistLimit())
    }


    private fun getWalkDistLimit() : Double {
        val string = PreferenceManager.getDefaultSharedPreferences(context).getString(PreferenceKeys.WALK_DIST_LIMIT, WALK_DIST_LIMIT_DEFAULT.toString())
        return if(string == null) WALK_DIST_LIMIT_DEFAULT * METER_IN_ANGULAR_LAT_LNG
                else (string.toDouble()  * METER_IN_ANGULAR_LAT_LNG)
    }


    interface OnFragmentInteractionListener {
        fun onBackFromResults(removedRoutes: List<RouteModel>?)
        fun drawRouteResult(route: RouteModel)
        fun drawSquares(walkingDistance: Double)
    }

    companion object{
        private const val HEIGHT_KEY = "height"
        private const val ORIGIN_LATLNG_KEY = "originlatlng"
        private const val DESTINATION_LATLNG_KEY = "destinationlatlng"
        const val TAG = "ResultsFragment"
        @JvmStatic
        fun newInstance(height: Int, originLatLng: LatLng, destinationLatLng: LatLng) = ResultsFragment().apply {
                arguments = Bundle().apply{
                    putInt(HEIGHT_KEY, height)
                    putParcelable(ORIGIN_LATLNG_KEY, originLatLng)
                    putParcelable(DESTINATION_LATLNG_KEY, destinationLatLng)
                }
        }
    }

}