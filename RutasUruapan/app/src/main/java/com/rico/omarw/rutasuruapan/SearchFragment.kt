package com.rico.omarw.rutasuruapan

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rico.omarw.rutasuruapan.models.AutocompleteItemModel
import java.util.*
import kotlin.collections.ArrayList

class SearchFragment : Fragment(){

    private lateinit var placesClient: PlacesClient
    private lateinit var autocompleteViewOrigin: AutoCompleteTextView
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var adapter: AutoCompleteAdapter
    private val uruapanBounds = RectangularBounds.newInstance(
            LatLng(19.367936, -102.098275),
            LatLng(19.478144, -101.993454)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(context == null) return

        //todo: nextTask: get current place
        // use it to initialize the auto complete adapter
        placesClient = Places.createClient(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        view.findViewById<FloatingActionButton>(R.id.fab_search).setOnClickListener{
                //                run{listener?.onSearch()}
            }
        autocompleteViewOrigin = view.findViewById(R.id.autocompletetextview_origin)
        if(context != null){
            adapter = AutoCompleteAdapter(context!!, placesClient, uruapanBounds)
            autocompleteViewOrigin.setAdapter(adapter)
            autocompleteViewOrigin.setOnFocusChangeListener { view, hasFocus ->
                if(hasFocus) autocompleteViewOrigin.showDropDown()
            }

            autocompleteViewOrigin.setOnClickListener {
                autocompleteViewOrigin.showDropDown()
            }

            autocompleteViewOrigin.setOnItemClickListener{ parent, view, position, id ->
                val item = adapter.getItem(position)
                autoCompleteItemClick(item)
            }
        }
        return view
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
        super.onDetach()
        listener = null
    }
//todo:  Task: create marquers
    // how to get the current place?
    private fun autoCompleteItemClick(item: AutocompleteItemModel){
        if(item.isCurrentLocation){
            autocompleteViewOrigin.setText(item.currentPlace?.address)
            listener?.drawMarker(item.currentPlace!!.latLng!!, "Origin", MarkerType.Origin)
        }
        else if(item.autocompletePrediction != null){
            autocompleteViewOrigin.setText(item.autocompletePrediction.getPrimaryText(null) ?: item.primaryText)
            // request coordinates from place id
            val placeId = item.autocompletePrediction.placeId
            val fetchPlaceRequest = FetchPlaceRequest.builder(placeId, PlaceFields).setSessionToken(AutocompleteSessionToken.newInstance()).build()
            placesClient.fetchPlace(fetchPlaceRequest).addOnCompleteListener {
                if(it.isSuccessful && it.result != null){
                    Log.d(DEBUG_TAG, "SearchFragment, success")
                    listener?.drawMarker(it.result!!.place.latLng!!, "Origin", MarkerType.Origin)

                }else{
                    Log.d(DEBUG_TAG, "couldnt find current place")
                    if(it.exception != null)
                        Log.d(DEBUG_TAG, "exception", it.exception)
                }
            }
        }
    (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(autocompleteViewOrigin.windowToken,0)

    }

    enum class MarkerType {
        Origin,
        Destination
    }

    interface OnFragmentInteractionListener {
        fun onSearch()
//        fun onGetCurLocation(onSuccess: (Location) -> Unit)
        fun drawMarker(position: LatLng, title: String, markerType: MarkerType)
    }
    companion object {
        const val TAG = "SearchFragment"
        @JvmStatic
        fun newInstance() = SearchFragment().apply {
//            enterTransition = androidx.transition.Explode()
//            exitTransition = androidx.transition.Explode()
        }

        val PlaceFields = ArrayList<Place.Field>().apply{
            add(Place.Field.ADDRESS)
            add(Place.Field.LAT_LNG)
        }
    }
}
