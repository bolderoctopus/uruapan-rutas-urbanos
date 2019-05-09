package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView


class ControlPanel : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private lateinit var originTextView: TextView
    private lateinit var destinationTextView: TextView
    private lateinit var outputTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_control_panel, container, false)

        originTextView = view.findViewById(R.id.textview_origin)
        destinationTextView = view.findViewById(R.id.textView_destination)
        outputTextView = view.findViewById(R.id.textView_output)

        view.findViewById<Button>(R.id.button_find_routes).setOnClickListener { listener?.findRoute() }

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

    public fun setOriginDestinationText(origin: String?, destination: String?){
        originTextView.setText(origin)
        destinationTextView.setText(destination)
    }

    interface OnFragmentInteractionListener {
        fun findRoute()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                ControlPanel().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
