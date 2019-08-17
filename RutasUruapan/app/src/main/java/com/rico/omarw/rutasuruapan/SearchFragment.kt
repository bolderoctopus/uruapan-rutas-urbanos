package com.rico.omarw.rutasuruapan

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.transition.Fade
import androidx.transition.Slide
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.FloatingActionButton


class SearchFragment : Fragment(), AutoCompleteAdapter.OnAutoCompleteListener, TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(texto: Editable?) {
        if(texto == null ||  texto.length < 5) return

        Log.d(DEBUG_TAG, "starting autoCompleteTest with: ${texto.toString()}")
        autoCompleteTest(texto.toString())
    }

    private lateinit var placesClient: PlacesClient
    private lateinit var actvOrigin: AutoCompleteTextView
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(context == null) return
        Places.initialize(context!!, resources.getString(R.string.google_maps_key))
        placesClient = Places.createClient(context!!)
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_search, container, false).apply {
            findViewById<FloatingActionButton>(R.id.fab_search).setOnClickListener{run{listener?.onSearch()}}
            actvOrigin = findViewById(R.id.autocompletetextview_origin)
            actvOrigin.addTextChangedListener(this@SearchFragment)

//            val emails = mutableListOf("uno", "dos", "tres", "cuatro", "unanimidad", "underscore", "donomo", "domotica", "trepador", "canceroso", "matador")
//            val adapter = AutoCompleteAdapter(context, android.R.layout.simple_spinner_dropdown_item, emails, this@SearchFragment)
//            actvOrigin.setAdapter(adapter)
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

    override fun onAutoComplete(string: String){
        actvOrigin.setText(string)
        actvOrigin.dismissDropDown()
    }

    fun autoCompleteTest(query: String){
        val token = AutocompleteSessionToken.newInstance()
        val bounds = RectangularBounds.newInstance(
                LatLng(19.367936, -102.098275),
                LatLng(19.478144, -101.993454)
        )

        val request = FindAutocompletePredictionsRequest.builder()
                .setLocationRestriction(bounds)
                .setCountry("mx")
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .setQuery(query)
                .build()
        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener {
                    for (autoCompletePrediction in it.autocompletePredictions){
//                        Log.d(DEBUG_TAG, "autoCompletePrediction.placeId: ${autoCompletePrediction.placeId}")
                        Log.d(DEBUG_TAG, "autoCompletePrediction.getFullText(null): ${autoCompletePrediction.getFullText(null).toString()}")
//                        Log.d(DEBUG_TAG, "autoCompletePrediction.getPrimaryText(null): ${autoCompletePrediction.getPrimaryText(null).toString()}")
//                        Log.d(DEBUG_TAG, "autoCompletePrediction.getSecondaryText(null): ${autoCompletePrediction.getSecondaryText(null).toString()}")
                    }
                }
                .addOnFailureListener{
                    if(it is ApiException) {
                        val apiException = it as ApiException
                        Log.d(DEBUG_TAG, "place not found, statusCode: ${apiException.statusCode}")
                    }
                }
    }



    interface OnFragmentInteractionListener {
        fun onSearch()
    }
    companion object {
        val TAG = "SearchFragment"
        @JvmStatic
        fun newInstance() = SearchFragment().apply {
//            enterTransition = androidx.transition.Explode()
//            exitTransition = androidx.transition.Explode()
        }
    }
}
