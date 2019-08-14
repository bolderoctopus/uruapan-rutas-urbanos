package com.rico.omarw.rutasuruapan

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.Fade
import androidx.transition.Slide
import com.google.android.material.floatingactionbutton.FloatingActionButton


class SearchFragment : Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false).apply {
            findViewById<FloatingActionButton>(R.id.fab_search).setOnClickListener{run{listener?.onSearch()}}
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
        super.onDetach()
        listener = null
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
