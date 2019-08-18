package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.FloatingActionButton


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
            autocompleteViewOrigin.setOnItemClickListener{ parent, view, position, id ->
                val item = adapter.getItem(position)
                autocompleteViewOrigin.setText(item.getPrimaryText(null))
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

    interface OnFragmentInteractionListener {
        fun onSearch()
    }
    companion object {
        const val TAG = "SearchFragment"
        @JvmStatic
        fun newInstance() = SearchFragment().apply {
//            enterTransition = androidx.transition.Explode()
//            exitTransition = androidx.transition.Explode()
        }
    }
}
