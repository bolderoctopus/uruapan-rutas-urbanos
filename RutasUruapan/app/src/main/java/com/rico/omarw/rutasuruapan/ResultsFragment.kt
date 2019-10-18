package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.rico.omarw.rutasuruapan.Constants.DEBUG_TAG
import com.rico.omarw.rutasuruapan.adapters.RouteListAdapter
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.database.Point
import com.rico.omarw.rutasuruapan.models.RouteModel
import kotlinx.coroutines.*
import kotlin.math.sqrt

class ResultsFragment : Fragment(), RouteListAdapter.DrawRouteListener{

    lateinit var recyclerView: RecyclerView
    private lateinit var groupWalkMessage: Group
    private var listener: OnFragmentInteractionListener? = null
    private var height: Int? = null
    public var onViewCreated: Runnable? = null
    private lateinit var originLatLng: LatLng
    private lateinit var destinationLatLng: LatLng
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
        groupWalkMessage = view.findViewById(R.id.group_walk_message)
        recyclerView = view.findViewById(R.id.recyclerView_results)
        view.findViewById<ImageButton>(R.id.imagebutton_back).setOnClickListener{
            backButtonPressed()
        }
        if(height != null)
            view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height!!)

        onViewCreated?.run()
        onViewCreated = null

        findRoutesAsync(originLatLng, destinationLatLng, getWalkDistLimit())

        return view
    }

    fun backButtonPressed(){
        clearDrawnRoutes()
        listener?.onBackFromResults()
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

    //todo: take in consideration streets
    private fun distanceBetweenPoints(p1: LatLng, p2: LatLng): Double {
        val d1 = (p1.latitude - p2.latitude)
        val d2 = (p1.longitude - p2.longitude)
        return sqrt(d1 * d1 + d2 * d2)
    }
    /**
     * what unit am I using? degrees?
     * new variables:
     * walkDistanceLimit: distance the user is willing to walk between buses and origin/destination
     * walkDistToDest: distance that you'd have to walk to reach the destination
     * routeWalkDisk: given a certain route, it's distance you'd have to walk from origin/busstop + bustop/destination
     *                  must be leser than walkDistLimit
     * totalDistRoute: give a certain route, it's its routeWalkDisk + the distance travelled on the bus, for ordering purpouses
     *
     * delete preference for amount of results to show?
     */

    /**
     * note on getting the mutual routes linearly:
     * supposing a walkDistLimit greater than walkDistToDest
     * it'd wouldnt matter if the totalRouteDist is greater than walkDistLimit, as long as routeWalkDisk still be lesser than it
     * we'd show the routes anyway, even tho you'd be travelling a greater ditance on bus rather than walking
     * found routes would still be sorted totalDistRoute desc
     */

    /**
     *
     * @param originLatLng Start point
     * @param destinationLatLng End point
     * @param walkDistLimit How much the user is willing to walk between the route start/end point and the given origin/destination respectively
     */
    private fun findRoutesAsync(originLatLng: LatLng, destinationLatLng: LatLng, walkDistLimit: Double){
//        if(walkDistLimit < 1) throw Exception("walkDistLimit must be a greater than 1")
        listener?.drawSquares(walkDistLimit)
        val walkDistToDest = distanceBetweenPoints(originLatLng, destinationLatLng)

        uiScope.launch {
            val routesDao = AppDatabase.getInstance(context!!)?.routesDAO()
            var commonRoutesIds: Set<Long>? = null

            val routesNearOrigin = async(Dispatchers.IO) { routesDao?.getRoutesIntercepting(walkDistLimit, originLatLng.latitude, originLatLng.longitude)}
            val routesNearDest = async(Dispatchers.IO) { routesDao?.getRoutesIntercepting(walkDistLimit, destinationLatLng.latitude, destinationLatLng.longitude)}
            awaitAll(routesNearDest, routesNearOrigin)

            //if(routesNearDest.await().isNullOrEmpty() || routesNearOrigin.await().isNullOrEmpty())// todo: suggest to increase WalkDistLimit?
            commonRoutesIds = routesNearOrigin.await()!!.intersect(routesNearDest.await()!!)

            if(commonRoutesIds.isNullOrEmpty()){
                 showNoResultsMessage()// todo: suggest to increase WalkDistLimit?
            }

            else{
                for(routeId in commonRoutesIds){
                    Log.d(DEBUG_TAG, "routeId: $routeId")
//                    var startPoint: Point? = withContext(Dispatchers.IO){routesDao?.getNearestPointTo(routeId)}
                    var startPoint: Point? = withContext(Dispatchers.IO){routesDao?.getNearestPointTo(originLatLng.latitude, originLatLng.longitude ,routeId)}
//                    var endPoint: Point? = withContext(Dispatchers.IO){routesDao?.getNearestPointTo(destinationLatLng.latitude, destinationLatLng.longitude ,routeId)}
                    listener?.drawMarker(LatLng(startPoint!!.lat, startPoint.lng))
//                    listener?.drawMarker(LatLng(endPoint!!.lat, endPoint.lng))
//                  get start/end points (points from route with the least distance to origin and dest)
//                  calculate routeWalkDist,
//                  calculate totalWalkDist
                    break
                }

//             sort them by totalWalkDist
//             display results
                /*
                val results = arrayListOf<RouteModel>()
                val routesInfo = withContext(Dispatchers.IO) { routesDao?.getRoutes(commonRoutesIds.toList()) }
                routesInfo?.forEach{results.add(RouteModel(it))}
                displayRoutes(results)

                 */
            }

        }
    }
    // nota sobre condicion en la busqueda iterativa
    // condicion 1
    // si la distancia que caminas desde el origen hasta la parada del camion
    // mas la distancia que caminas cuando te bajas hasta tu destino
    // es mayor que lo que original mente recorrerias y caminaras desde el origen hacia el destino
    // entonces deberias caminar

    private fun clearDrawnRoutes() = drawnRoutes?.forEach{it.remove()}

    private fun displayRoutes(results: ArrayList<RouteModel>){
        groupWalkMessage.visibility = View.GONE

        recyclerView.visibility = View.VISIBLE
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RouteListAdapter(results, this)
    }

    private fun showNoResultsMessage(){
        recyclerView.visibility = View.GONE
        groupWalkMessage.visibility = View.VISIBLE
    }

    override fun drawRoute(route: RouteModel) {
        if(drawnRoutes == null) drawnRoutes = ArrayList()
        drawnRoutes!!.add(route)
        listener?.drawRoute(route)
    }

    fun startUpdate(){
        recyclerView.visibility = View.INVISIBLE
        clearDrawnRoutes()
    }

    fun endUpdate(origin: LatLng, destination: LatLng){
        findRoutesAsync(origin, destination, getWalkDistLimit())
    }


    fun getWalkDistLimit() : Double {
        val s = PreferenceManager.getDefaultSharedPreferences(context).getString("walk_dist_limit", Constants.WALK_DIST_LIMIT_DEFFAULT.toString())
        return s?.toDouble() ?: Constants.WALK_DIST_LIMIT_DEFFAULT

    }


    interface OnFragmentInteractionListener {
        fun onBackFromResults()
        fun drawRoute(route: RouteModel)
        fun drawSquares(walkingDistance: Double)
        fun drawMarker(latLng: LatLng)
        fun clearMap()
    }

    companion object{
        private const val HEIGHT_KEY = "height"
        private const val ORIGIN_LATLNG_KEY = "originlatlng"
        private const val DESTINATION_LATLNG_KEY = "destinationlatlng"
        val TAG = "ResultsFragment"
        @JvmStatic
        fun newInstance(height: Int, originLatLng: LatLng, destinationLatLng: LatLng) = ResultsFragment().apply {
                arguments = Bundle().apply{
                    putInt(HEIGHT_KEY, height)
                    putParcelable(ORIGIN_LATLNG_KEY, originLatLng)
                    putParcelable(DESTINATION_LATLNG_KEY, destinationLatLng)
                }
//            enterTransition = Slide(Gravity.RIGHT)
//            exitTransition = Slide(Gravity.LEFT)
        }
    }

}