package com.rico.omarw.rutasuruapan

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.rico.omarw.rutasuruapan.Constants.DEBUG_TAG
import com.rico.omarw.rutasuruapan.Utils.checkInternetConnection
import com.rico.omarw.rutasuruapan.Utils.hideKeyboard
import com.rico.omarw.rutasuruapan.adapters.AutoCompleteAdapter
import com.rico.omarw.rutasuruapan.models.AutocompleteItemModel
import kotlinx.coroutines.*
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class SearchFragment : Fragment(){

    var destinationLatLng: LatLng? = null
    var originLatLng: LatLng? = null
    private lateinit var placesClient: PlacesClient
    lateinit var origin: TextInputLayout
    lateinit var originAutoCompleteTextView: AutoCompleteTextView
    lateinit var destination: TextInputLayout
    lateinit var destinationAutoCompleteTextView: AutoCompleteTextView
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var autoCompleteAdapter: AutoCompleteAdapter

    private lateinit var geocoder: Geocoder
    private var uiScope = CoroutineScope(Dispatchers.Main)
    private var currentLocationOwner: MarkerType? = null


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
        originAutoCompleteTextView = view.findViewById(R.id.autocompletetextview_origin)
        destination = view.findViewById(R.id.custom_actv_destination)
        destinationAutoCompleteTextView = view.findViewById(R.id.autocompletetextview_destination)

        originAutoCompleteTextView.tag = MarkerType.Origin
        destinationAutoCompleteTextView.tag = MarkerType.Destination

        originAutoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            try {
                if (hasFocus) originAutoCompleteTextView.showDropDown()
            }catch (error: Exception){
                Log.e(DEBUG_TAG, error.message)
            }
        }

        originAutoCompleteTextView.setOnClickListener {originAutoCompleteTextView.showDropDown()}
        originAutoCompleteTextView.setOnItemClickListener(this::onAutoCompleteItemClick)

        destinationAutoCompleteTextView.setOnClickListener {destinationAutoCompleteTextView.showDropDown()}
        destinationAutoCompleteTextView.setOnItemClickListener (this::onAutoCompleteItemClick)

        if(context != null){
            val locationClient = LocationServices.getFusedLocationProviderClient(context!!)
            autoCompleteAdapter = AutoCompleteAdapter(context!!, uiScope, locationClient, placesClient, uruapanBounds, includeCurrentLocation = true, includePickLocation = true)
            destinationAutoCompleteTextView.setAdapter(autoCompleteAdapter)
            originAutoCompleteTextView.setAdapter(autoCompleteAdapter)
        }

        origin.setEndIconOnClickListener{ clearAutoCompleteTextView(MarkerType.Origin)}
        destination.setEndIconOnClickListener{ clearAutoCompleteTextView(MarkerType.Destination)}

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
        Log.d(DEBUG_TAG, "cancelling coroutines")
        uiScope.cancel()
        listener = null
        super.onDetach()
    }

    fun startUpdatePosition(markerType: MarkerType, latLng: LatLng){
        ignoreFiltering(true)
        val autocompleteTextview: AutoCompleteTextView
        when(markerType){
            MarkerType.Origin -> {
                originLatLng = latLng
                origin.error = null
                autocompleteTextview = originAutoCompleteTextView
            }
            MarkerType.Destination -> {
                destinationLatLng = latLng
                destination.error = null
                autocompleteTextview = destinationAutoCompleteTextView
            }
        }
        autocompleteTextview.isEnabled = false
        autocompleteTextview.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))// todo, if resultsFragment is active and you move a marker, this crashes the app
    }

    fun updatePosition(markerType: MarkerType, latLng: LatLng){
        when(markerType){
            MarkerType.Origin -> {
                originLatLng = latLng
                originAutoCompleteTextView.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))
            }
            MarkerType.Destination -> {
                destinationLatLng = latLng
                destinationAutoCompleteTextView.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))
            }
        }
    }

    fun endUpdatePosition(markerType: MarkerType, latLng: LatLng){
        when(markerType){
            MarkerType.Origin -> {
                originLatLng = latLng
                originAutoCompleteTextView.isEnabled = true
                originAutoCompleteTextView.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))
                restoreCurrentLocation(MarkerType.Origin)
            }
            MarkerType.Destination -> {
                destinationLatLng = latLng
                destinationAutoCompleteTextView.isEnabled = true
                destinationAutoCompleteTextView.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))
                restoreCurrentLocation(MarkerType.Destination)
            }
        }
        findPlaceByLatLng(markerType, latLng)
    }

    fun oneTimeUpdatePosition(markerType: MarkerType, latLng: LatLng){
        ignoreFiltering(true)
        when(markerType){
            MarkerType.Origin -> {
                originLatLng = latLng
                origin.error = null
                originAutoCompleteTextView.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))
            }
            MarkerType.Destination -> {
                destinationLatLng = latLng
                destination.error = null
                destinationAutoCompleteTextView.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))
            }
        }
        findPlaceByLatLng(markerType, latLng)
    }

    private fun onAutoCompleteItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long){
        val markerType: MarkerType
        val item = autoCompleteAdapter.getItem(position)
        val title: String

        if(originAutoCompleteTextView.hasFocus()){
            title = getString(R.string.origin)
            origin.error = null
            markerType = MarkerType.Origin
            destinationAutoCompleteTextView.requestFocus()
        }else{
            title = getString(R.string.destination)
            destination.error = null
            markerType = MarkerType.Destination
            hideKeyboard(context!!, destinationAutoCompleteTextView.windowToken)
        }

        when(item.kind){
            AutocompleteItemModel.ItemKind.AutocompletePrediction -> {// request coordinates from place id
                val fetchPlaceRequest = FetchPlaceRequest.builder(item.autocompletePrediction!!.placeId, PlaceFields)
                        .setSessionToken(AutocompleteSessionToken.newInstance()).build()
                placesClient.fetchPlace(fetchPlaceRequest).addOnCompleteListener {
                    if(it.isSuccessful && it.result != null) drawMarker(markerType, it.result?.place?.latLng, title)
                    else Log.d(DEBUG_TAG, "We're having trouble fetching the coordinates from this address, try again later or instead drag the marker")//todo: should I display this to the user?
                }
            }
            AutocompleteItemModel.ItemKind.PickLocation -> drawMarker(markerType, null, title)
            AutocompleteItemModel.ItemKind.CurrentLocation -> {
                drawMarker(markerType, item.currentLatLng, title)
                currentLocationOwner = markerType
                // Si la opcion de "Use Current Location" es seleccionada en un textView esta ya no se mostrara hasta que se presione el boton de limpiar o cambie de ubicacion el marcador
                //correspondiente al textview
                autoCompleteAdapter.removeCurrentLocation()
            }
        }
    }

    private fun findPlaceByLatLng(markerType: MarkerType, latLng: LatLng){
        if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("resolve_locations_to_addresses", false)
                || (context != null && !checkInternetConnection(context!!)))
            return

        if(!::geocoder.isInitialized) geocoder = Geocoder(context, Locale.getDefault())

        uiScope.launch {
            try {
                val addresses: List<Address>? = withContext(Dispatchers.IO) { geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) }
                if (!addresses.isNullOrEmpty())
                    when (markerType) {
                        MarkerType.Origin -> originAutoCompleteTextView.setText(getShortAddress(addresses[0]))
                        MarkerType.Destination -> destinationAutoCompleteTextView.setText(getShortAddress(addresses[0]))
                    }
            }catch (exception: java.lang.Exception){
                Log.e(TAG, "findPlaceByLatLng", exception)
            }
            ignoreFiltering(false)
        }
    }

    private fun clearAutoCompleteTextView(markerType: MarkerType){
        when(markerType){
            MarkerType.Origin -> {
                originAutoCompleteTextView.setText("")
                listener?.clearMarker(MarkerType.Origin)
                originLatLng = null
                restoreCurrentLocation(MarkerType.Origin)
            }
            MarkerType.Destination -> {
                destinationAutoCompleteTextView.setText("")
                listener?.clearMarker(MarkerType.Destination)
                destinationLatLng = null
                restoreCurrentLocation(MarkerType.Destination)
            }
        }
    }

    // If the user had picked 'Show current location' in this textview but then pressed the clear button we'll make the option available to both textviews again.
    /* Si en uno de los textFields se habia seleccionado la opcion "Use Current Location", pero luego
        1. Presiona el boton de limpiar en ese textView
        o
        2. Mueve el marcador correspondiente a otra ubicaion
        entonces la opcion de "User Current Location" vuelve a estar disponible en el adaptador para ambos textViews

     */
    private fun restoreCurrentLocation(v: MarkerType){
        if(currentLocationOwner == v) {
            Log.d(DEBUG_TAG, "restoring current location")
            autoCompleteAdapter.addCurrentLocation()
            currentLocationOwner = null
        }
    }

    private fun search(){
        if(originLatLng == null){// todo: use textinput error instead
            origin.error = getString(R.string.empty_textview_error)
        }
        else if (destinationLatLng == null) {
            destination.error = getString(R.string.empty_textview_error)
        }
        else {
            listener?.onSearch(originLatLng!!, destinationLatLng!!)
        }
    }

    private fun drawMarker(markerType: MarkerType, latLng: LatLng?, title: String){
        if(markerType == MarkerType.Origin)
            originLatLng = latLng
        else
            destinationLatLng = latLng

        listener?.drawMarker(latLng, title, markerType)
    }

    private fun ignoreFiltering(ignore: Boolean){
        autoCompleteAdapter.ignoreFiltering = ignore
    }

    fun clearInputs(){
        if(currentLocationOwner != null) {
            autoCompleteAdapter.addCurrentLocation()
            currentLocationOwner = null
        }

        originLatLng = null
        origin.error = null
        originAutoCompleteTextView.setText("")

        destinationLatLng = null
        destination.error = null
        destinationAutoCompleteTextView.setText("")
    }


    enum class MarkerType {//todo: rename, for something more relevant since this no longer works to differentiate between markers but also inputs
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
        val uruapanBounds = RectangularBounds.newInstance(
                LatLng(19.367936, -102.098275),
                LatLng(19.478144, -101.993454)
        )
        val uruapanLatLngBounds = LatLngBounds(
                uruapanBounds.southwest,
                uruapanBounds.northeast)
        @JvmStatic
        fun newInstance() = SearchFragment().apply {
        }

        val PlaceFields = ArrayList<Place.Field>().apply{
            add(Place.Field.ADDRESS)
            add(Place.Field.LAT_LNG)
        }

        private val decimalFormat = DecimalFormat("#.#####")

        fun getShortAddress(address: Address): String{
            // use featureName if it's not the street number
            return if(address.featureName != address.subThoroughfare)
                address.featureName
            // use coords if street + subLocality are null or street + postalCode are null
            else if(address.thoroughfare == null || (address.subLocality == null || address.postalCode == null ))
                decimalFormat.format(address.latitude) + ", " + decimalFormat.format(address.longitude)
            // use street + subLocality if it's not "Colonia"
            else if(address.subLocality != "Colonia")
                address.thoroughfare + ", " + address.subLocality
            // use street + postalCode
            else
                address.thoroughfare + ", " + address.postalCode
        }
    }
}
