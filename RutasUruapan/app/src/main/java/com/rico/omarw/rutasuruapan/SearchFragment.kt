package com.rico.omarw.rutasuruapan

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rico.omarw.rutasuruapan.adapters.AutoCompleteAdapter
import com.rico.omarw.rutasuruapan.models.AutocompleteItemModel
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

class SearchFragment : Fragment(){

    var destinationLatLng: LatLng? = null
    var originLatLng: LatLng? = null
    private lateinit var placesClient: PlacesClient
    lateinit var origin: CustomAutocompleteTextView
    lateinit var destination: CustomAutocompleteTextView
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var originAdapter: AutoCompleteAdapter
    private lateinit var destinationAdapter: AutoCompleteAdapter
    private val uruapanBounds = RectangularBounds.newInstance(
            LatLng(19.367936, -102.098275),
            LatLng(19.478144, -101.993454)
    )
    private lateinit var geocoder: Geocoder
    private var uiScope = CoroutineScope(Dispatchers.Main)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(context == null) return
        placesClient = Places.createClient(context!!)
        if(!uiScope.isActive)
            uiScope = CoroutineScope(Dispatchers.Main)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        view.findViewById<FloatingActionButton>(R.id.fab_search).setOnClickListener{search()}

        origin = view.findViewById(R.id.custom_actv_origin)
        destination = view.findViewById(R.id.custom_actv_destination)

        origin.autoCompleteTextView.tag = MarkerType.Origin
        destination.autoCompleteTextView.tag = MarkerType.Destination

        origin.autoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            try {
                if (hasFocus) origin.autoCompleteTextView.showDropDown()
            }catch (error: Exception){
                Log.e(DEBUG_TAG, error.message)
            }
        }
        origin.autoCompleteTextView.setOnClickListener {origin.autoCompleteTextView.showDropDown()}
        origin.autoCompleteTextView.setOnItemClickListener{ parent, _, position, id ->
            val item = originAdapter.getItem(position)
            autoCompleteItemClick(origin.autoCompleteTextView, item)
            destination.autoCompleteTextView.requestFocus()
        }

        destination.autoCompleteTextView.setOnClickListener {destination.autoCompleteTextView.showDropDown()}
        destination.autoCompleteTextView.setOnItemClickListener{ parent, _, position, id ->
            val item = destinationAdapter.getItem(position)
            autoCompleteItemClick(destination.autoCompleteTextView, item)
            hideKeyboard(destination.autoCompleteTextView.windowToken)
        }

        if(context != null){
            originAdapter = AutoCompleteAdapter(context!!, placesClient, uruapanBounds, AutocompleteItemModel.ItemKind.CurrentLocation)
            destinationAdapter = AutoCompleteAdapter(context!!, placesClient, uruapanBounds, AutocompleteItemModel.ItemKind.PickLocation)
            destination.autoCompleteTextView.setAdapter(destinationAdapter)
            origin.autoCompleteTextView.setAdapter(originAdapter)
        }

        origin.onClear = this::onTextViewClear
        destination.onClear = this::onTextViewClear

        return view
    }

    //nextTask: 1 fix lag caused by this block
    fun updatePosition(markerType: MarkerType, latLng: LatLng, enableTextview: Boolean){
        when(markerType){
            MarkerType.Origin -> {
                originLatLng = latLng
                origin.autoCompleteTextView.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))
                origin.autoCompleteTextView.setAdapter(if(enableTextview) originAdapter else null)
                origin.autoCompleteTextView.isEnabled = enableTextview
            }
            MarkerType.Destination -> {
                destinationLatLng = latLng
                destination.autoCompleteTextView.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))
                destination.autoCompleteTextView.setAdapter(if(enableTextview) destinationAdapter else null)
                destination.autoCompleteTextView.isEnabled = enableTextview
            }
        }
        if(enableTextview) findPlaceByLatLng(markerType, latLng)
    }

    private fun onTextViewClear(view: View){
        when(view.id){
            R.id.custom_actv_origin -> {
                listener?.clearMarker(MarkerType.Origin)
                originLatLng = null
            }
            R.id.custom_actv_destination -> {
                listener?.clearMarker(MarkerType.Destination)
                destinationLatLng = null
            }
        }
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
        uiScope.cancel()
        listener = null
        super.onDetach()
    }

    private fun search(){
        if(originLatLng == null){
            origin.autoCompleteTextView.error = "You must pick a place"
        }
        else if (destinationLatLng == null) {
            destination.autoCompleteTextView.error = "You must pick a place"
        }
        else {
            listener?.onSearch(originLatLng!!, destinationLatLng!!)
        }
    }

    //todo: format the address: looks different when is currentLocation, see note (it has to do with the to string method on the model)
    // suggested format for address: street name + colonia? no number?
    private fun autoCompleteItemClick(sender: AutoCompleteTextView, item: AutocompleteItemModel){
        val markerType = sender.tag as MarkerType
        val title = if(markerType == MarkerType.Origin) "Origin" else "Destination"

        when(item.kind){
            AutocompleteItemModel.ItemKind.CurrentLocation -> {
                sender.setText(item.currentPlace?.address)
                darMarker(markerType, item.currentPlace?.latLng, title)
            }
            AutocompleteItemModel.ItemKind.AutocompletePrediction -> {// request coordinates from place id
//                sender.setText(item.autocompletePrediction!!.getPrimaryText(null))    // this shows object toString()
                val fetchPlaceRequest = FetchPlaceRequest.builder(item.autocompletePrediction!!.placeId, PlaceFields)
                        .setSessionToken(AutocompleteSessionToken.newInstance()).build()
                placesClient.fetchPlace(fetchPlaceRequest).addOnCompleteListener {
                    if(it.isSuccessful && it.result != null){
                        darMarker(markerType, it.result?.place?.latLng, title)
                    }
                    else Log.d(DEBUG_TAG, "couldnt find current place")
                }
            }

            AutocompleteItemModel.ItemKind.PickLocation -> {
                sender.setText(" ")
                darMarker(markerType, null, title)
            }

        }
    }

    private fun darMarker(markerType: MarkerType, latLng: LatLng?, title: String){
        if(markerType == MarkerType.Origin)
            originLatLng = latLng
        else
            destinationLatLng = latLng

        listener?.drawMarker(latLng, title, markerType)
    }

    private fun hideKeyboard(windowToken: IBinder){
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(windowToken,0)
    }
//todo: prevent to start autoComplete query after setting text on the TextView
    fun findPlaceByLatLng(markerType: MarkerType, latLng: LatLng){
        if(!::geocoder.isInitialized) geocoder = Geocoder(context, Locale.getDefault())

        uiScope.launch {
            val address: List<Address>? = async(Dispatchers.IO) { geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)}.await()
            if(!address.isNullOrEmpty() && address[0].maxAddressLineIndex > -1){
                Log.d(DEBUG_TAG, "address[0].maxAddressLineIndex : ${address[0].maxAddressLineIndex}")
                when(markerType){
                    MarkerType.Origin ->{
                        origin.autoCompleteTextView.setText(address[0].getAddressLine(0))
                    }
                    MarkerType.Destination ->{
                        destination.autoCompleteTextView.setText(address[0].getAddressLine(0))
                    }
                }
            }
        }
    }

    enum class MarkerType {
        Origin,
        Destination
    }

    interface OnFragmentInteractionListener {
        fun onSearch(origin: LatLng, destination: LatLng)
//        fun onGetCurLocation(onSuccess: (Location) -> Unit)
        fun drawMarker(position: LatLng?, title: String, markerType: MarkerType)
        fun clearMarker(markerType: MarkerType)
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
