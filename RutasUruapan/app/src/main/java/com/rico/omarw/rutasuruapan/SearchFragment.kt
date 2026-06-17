package com.rico.omarw.rutasuruapan

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.rico.omarw.rutasuruapan.Constants.COMPLETION_THRESHOLD
import com.rico.omarw.rutasuruapan.Constants.DEBUG_TAG
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys
import com.rico.omarw.rutasuruapan.Utils.checkInternetConnection
import com.rico.omarw.rutasuruapan.Utils.hideKeyboard
import com.rico.omarw.rutasuruapan.adapters.AutoCompleteAdapter
import com.rico.omarw.rutasuruapan.models.AutocompleteItemModel
import kotlin.collections.ArrayList
import androidx.preference.PreferenceManager
import com.rico.omarw.rutasuruapan.databinding.FragmentSearchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.getValue

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private val routeViewModel: RouteViewModel by activityViewModels()

    enum class MarkerType {
        Origin,
        Destination
    }

    //todo: move to viewmodel or controller
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var autoCompleteAdapter: AutoCompleteAdapter
    private lateinit var geocoder: Geocoder
    private lateinit var placesClient: PlacesClient

    private lateinit var listener: OnFragmentInteractionListener
    private var currentLocationOwner: MarkerType? = null
    private var shouldDisplayRemoveMarkerDialog: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (context == null) return
        placesClient = Places.createClient(requireContext())
        shouldDisplayRemoveMarkerDialog =
            InformativeDialogs.shouldDisplayRemoveMarkerDialog(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.apply {
            searchButton.setOnClickListener { search() }

            originAutocompleteTextview.tag = MarkerType.Origin
            destinationAutocompleteTextview.tag = MarkerType.Destination

            originAutocompleteTextview.setOnFocusChangeListener { _, hasFocus ->
                try {
                    if (hasFocus) originAutocompleteTextview.showDropDown()
                } catch (error: Exception) {
                    Log.e(DEBUG_TAG, error.message ?: "")
                }
            }

            originAutocompleteTextview.setOnClickListener { originAutocompleteTextview.showDropDown() }
            originAutocompleteTextview.setOnItemClickListener(this@SearchFragment::onAutoCompleteItemClick)
            originAutocompleteTextview.threshold = COMPLETION_THRESHOLD

            destinationAutocompleteTextview.setOnClickListener { destinationAutocompleteTextview.showDropDown() }
            destinationAutocompleteTextview.setOnItemClickListener(this@SearchFragment::onAutoCompleteItemClick)
            destinationAutocompleteTextview.threshold = COMPLETION_THRESHOLD

            if (context != null) {
                val locationClient = LocationServices.getFusedLocationProviderClient(requireContext())
                autoCompleteAdapter = AutoCompleteAdapter(
                    requireContext(),
                    lifecycleScope,
                    locationClient,
                    placesClient,
                    uruapanBounds,
                    includeCurrentLocation = true,
                    includePickLocation = true
                )
                destinationAutocompleteTextview.setAdapter(autoCompleteAdapter)
                originAutocompleteTextview.setAdapter(autoCompleteAdapter)
            }

            originTextInput.setEndIconOnClickListener { clearAutoCompleteTextView(MarkerType.Origin) }
            destinationTextInput.setEndIconOnClickListener { clearAutoCompleteTextView(MarkerType.Destination) }
        }

        return binding.root
    }

    fun startUpdatePosition(markerType: MarkerType, latLng: LatLng) {
        ignoreFiltering(true)
        val autocompleteTextview: AutoCompleteTextView
        when (markerType) {
            MarkerType.Origin -> {
                originLatLng = latLng
                binding.originTextInput.error = null
                autocompleteTextview = binding.originAutocompleteTextview
            }

            MarkerType.Destination -> {
                destinationLatLng = latLng
                binding.destinationTextInput.error = null
                autocompleteTextview = binding.destinationAutocompleteTextview
            }
        }
        autocompleteTextview.isEnabled = false
        autocompleteTextview.setText(getString(R.string.lat_lng, latLng.latitude, latLng.longitude))
    }

    fun updatePosition(markerType: MarkerType, latLng: LatLng) {
        when (markerType) {
            MarkerType.Origin -> {
                originLatLng = latLng
                binding.originAutocompleteTextview.setText(
                    getString(
                        R.string.lat_lng,
                        latLng.latitude,
                        latLng.longitude
                    )
                )
            }

            MarkerType.Destination -> {
                destinationLatLng = latLng
                binding.destinationAutocompleteTextview.setText(
                    getString(
                        R.string.lat_lng,
                        latLng.latitude,
                        latLng.longitude
                    )
                )
            }
        }
    }

    fun endUpdatePosition(markerType: MarkerType, latLng: LatLng) {
        binding.apply {
            when (markerType) {
                MarkerType.Origin -> {
                    originLatLng = latLng
                    originAutocompleteTextview.isEnabled = true
                    originAutocompleteTextview.setText(
                        getString(
                            R.string.lat_lng,
                            latLng.latitude,
                            latLng.longitude
                        )
                    )
                    restoreCurrentLocation(MarkerType.Origin)
                }

                MarkerType.Destination -> {
                    destinationLatLng = latLng
                    destinationAutocompleteTextview.isEnabled = true
                    destinationAutocompleteTextview.setText(
                        getString(
                            R.string.lat_lng,
                            latLng.latitude,
                            latLng.longitude
                        )
                    )
                    restoreCurrentLocation(MarkerType.Destination)
                }
            }
        }
        findPlaceByLatLng(markerType, latLng)
    }

    fun oneTimeUpdatePosition(markerType: MarkerType, latLng: LatLng) {
        binding.apply {
            when (markerType) {
                MarkerType.Origin -> {
                    if (originAutocompleteTextview.hasFocus()) originAutocompleteTextview.clearFocus()
                    originLatLng = latLng
                    originTextInput.error = null
                    originAutocompleteTextview.setText(
                        getString(
                            R.string.lat_lng,
                            latLng.latitude,
                            latLng.longitude
                        )
                    )
                }

                MarkerType.Destination -> {
                    if (destinationAutocompleteTextview.hasFocus()) destinationAutocompleteTextview.clearFocus()
                    destinationLatLng = latLng
                    destinationTextInput.error = null
                    destinationAutocompleteTextview.setText(
                        getString(
                            R.string.lat_lng,
                            latLng.latitude,
                            latLng.longitude
                        )
                    )
                }
            }

        }
        findPlaceByLatLng(markerType, latLng)
    }

    private fun onAutoCompleteItemClick(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        val markerType: MarkerType
        val item = autoCompleteAdapter.getItem(position)
        val title: String

        binding.apply {
            if (originAutocompleteTextview.hasFocus()) {
                title = getString(R.string.marker_title_origin)
                originTextInput.error = null
                markerType = MarkerType.Origin
                if (item.kind != AutocompleteItemModel.ItemKind.PickLocation)
                    destinationAutocompleteTextview.requestFocus()
                else {
                    originAutocompleteTextview.clearFocus()
                    hideKeyboard(requireContext(), originAutocompleteTextview.windowToken)
                }
            } else {
                title = getString(R.string.marker_title_destination)
                destinationTextInput.error = null
                markerType = MarkerType.Destination
                hideKeyboard(requireContext(), destinationAutocompleteTextview.windowToken)
            }
        }

        when (item.kind) {
            AutocompleteItemModel.ItemKind.AutocompletePrediction -> {// request coordinates from place id
                val fetchPlaceRequest =
                    FetchPlaceRequest.builder(item.autocompletePrediction!!.placeId, PlaceFields)
                        .setSessionToken(AutocompleteSessionToken.newInstance()).build()
                placesClient.fetchPlace(fetchPlaceRequest).addOnCompleteListener {
                    if (it.isSuccessful && it.result != null) drawMarker(
                        markerType,
                        it.result?.place?.latLng,
                        title,
                        true,
                        false
                    )
                }
            }

            AutocompleteItemModel.ItemKind.PickLocation -> {
                displayRemoveMarkerDialog()
                drawMarker(markerType, null, title, false, true)
            }

            AutocompleteItemModel.ItemKind.CurrentLocation -> {
                drawMarker(markerType, item.currentLatLng, title, true, false)
                currentLocationOwner = markerType
                // Si la opcion de "Use Current Location" es seleccionada en un textView esta ya no se mostrara hasta que se presione el boton de limpiar o cambie de ubicacion el marcador
                //correspondiente al textview
                autoCompleteAdapter.removeCurrentLocation()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun findPlaceByLatLng(markerType: MarkerType, latLng: LatLng) {
        if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(PreferenceKeys.RESOLVE_LOCATIONS_TO_ADDRESSES, true)
            || (context != null && !checkInternetConnection(requireContext()))
        )
            return

        if (!::geocoder.isInitialized) geocoder = Geocoder(requireContext(), Locale.getDefault())

        lifecycleScope.launch {
            try {
                val addresses: List<Address>? = withContext(Dispatchers.IO) {
                    geocoder.getFromLocation(
                        latLng.latitude,
                        latLng.longitude,
                        1
                    )
                }
                if (!addresses.isNullOrEmpty())
                    when (markerType) {
                        MarkerType.Origin -> binding.originAutocompleteTextview.setText(
                            AddressUtils.getShortAddress(
                                addresses[0]
                            )
                        )

                        MarkerType.Destination -> binding.destinationAutocompleteTextview.setText(
                            AddressUtils.getShortAddress(addresses[0])
                        )
                    }
            } catch (exception: java.lang.Exception) {
                Log.e(TAG, "findPlaceByLatLng", exception)
            }
            ignoreFiltering(false)
        }
    }

    private fun clearAutoCompleteTextView(markerType: MarkerType) {
        when (markerType) {
            MarkerType.Origin -> {
                binding.originAutocompleteTextview.setText("")
                listener.clearMarker(MarkerType.Origin)
                originLatLng = null
                restoreCurrentLocation(MarkerType.Origin)
            }

            MarkerType.Destination -> {
                binding.destinationAutocompleteTextview.setText("")
                listener.clearMarker(MarkerType.Destination)
                destinationLatLng = null
                restoreCurrentLocation(MarkerType.Destination)
            }
        }
    }

    /* Si en uno de los textFields se habia seleccionado la opcion "Use Current Location", pero luego
        1. Presiona el boton de limpiar en ese textView
        o
        2. Mueve el marcador correspondiente a otra ubicaion
        entonces la opcion de "User Current Location" vuelve a estar disponible en el adaptador para ambos textViews

     */
    fun restoreCurrentLocation(v: MarkerType?) {
        if (v == null || currentLocationOwner == v) {
            autoCompleteAdapter.addCurrentLocation()
            currentLocationOwner = null
        }
    }

    private fun search() {
        val currentOrigin = originLatLng
        val currentDestination = destinationLatLng

        if (currentOrigin == null) {
            binding.originTextInput.error = getString(R.string.empty_textview_error)
        } else if (currentDestination == null) {
            binding.destinationTextInput.error = getString(R.string.empty_textview_error)
        } else {
            listener.onSearch(currentOrigin, currentDestination)
        }
    }

    private fun drawMarker(
        markerType: MarkerType,
        latLng: LatLng?,
        title: String,
        animate: Boolean,
        bounce: Boolean
    ) {
        if (markerType == MarkerType.Origin)
            originLatLng = latLng
        else
            destinationLatLng = latLng
        listener.drawMarker(latLng, title, markerType, animate, bounce)
    }

    private fun ignoreFiltering(ignore: Boolean) {
        autoCompleteAdapter.ignoreFiltering = ignore
    }

    fun clearInputs() {
        if (currentLocationOwner != null) {
            autoCompleteAdapter.addCurrentLocation()
            currentLocationOwner = null
        }

        originLatLng = null
        destinationLatLng = null

        binding.apply {
            originTextInput.error = null
            originAutocompleteTextview.setText("")

            destinationTextInput.error = null
            destinationAutocompleteTextview.setText("")
        }
    }

    private fun displayRemoveMarkerDialog() {
        if (shouldDisplayRemoveMarkerDialog) {
            shouldDisplayRemoveMarkerDialog = false
            InformativeDialogs.displayHowToRemoveMarkersDialog(
                requireContext(),
                listener.getMapVerticalOffset()
            ) {
                if (context != null) {
                    InformativeDialogs.removeMarkerDialogDisplayed(requireContext())
                }
            }
        }
    }

    interface OnFragmentInteractionListener {
        fun onSearch(origin: LatLng, destination: LatLng)
        fun drawMarker(
            position: LatLng?,
            title: String,
            markerType: MarkerType,
            animate: Boolean,
            bounce: Boolean
        )

        fun clearMarker(markerType: MarkerType)
        fun getMapVerticalOffset(): Int
    }

    companion object {
        const val TAG = "SearchFragment"
        val uruapanBounds = RectangularBounds.newInstance(
            LatLng(19.367936, -102.098275),
            LatLng(19.478144, -101.993454)
        )
        val uruapanLatLngBounds = LatLngBounds(
            uruapanBounds.southwest,
            uruapanBounds.northeast
        )

        @JvmStatic
        fun newInstance(listener: OnFragmentInteractionListener) = SearchFragment().apply {
            this.listener = listener
        }

        val PlaceFields = ArrayList<Place.Field>().apply {
            add(Place.Field.ADDRESS)
            add(Place.Field.LAT_LNG)
        }
    }
}
