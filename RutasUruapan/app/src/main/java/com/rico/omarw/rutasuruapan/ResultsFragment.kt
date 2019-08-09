package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.transition.Slide

class ResultsFragment : Fragment(){
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_results, container, false).apply {
            findViewById<ImageButton>(R.id.imagebutton_back).setOnClickListener{run{listener?.onBack()}}
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

    interface OnFragmentInteractionListener{
        fun onBack()
    }

    companion object{
        @JvmStatic
        fun newInstance() = ResultsFragment().apply {
//            enterTransition = Slide(Gravity.RIGHT)
//            exitTransition = Slide(Gravity.LEFT)
        }
    }

}