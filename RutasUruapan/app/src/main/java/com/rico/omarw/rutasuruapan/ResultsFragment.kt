package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class ResultsFragment : Fragment(){
    private var listener: OnFragmentInteractionListener? = null
    private var height: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            height = it.getInt(AllRoutesFragment.HEIGHT_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_results, container, false).also {
            it.findViewById<ImageButton>(R.id.imagebutton_back).setOnClickListener{run{listener?.onBackFromResults()}}
            it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, if(this.height == null) ViewGroup.LayoutParams.MATCH_PARENT else this.height!!)

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
        fun onBackFromResults()
    }

    companion object{
        const val HEIGHT_KEY = "height"
        val TAG = "ResultsFragment"
        @JvmStatic
        fun newInstance(height: Int) = ResultsFragment().apply {
                arguments = Bundle().apply{
                    putInt(HEIGHT_KEY, height)
                }
//            enterTransition = Slide(Gravity.RIGHT)
//            exitTransition = Slide(Gravity.LEFT)
        }
    }

}