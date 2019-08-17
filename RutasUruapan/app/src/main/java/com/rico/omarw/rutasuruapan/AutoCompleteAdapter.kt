package com.rico.omarw.rutasuruapan

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

class AutoCompleteAdapter (context: Context,
                           resource: Int,
                           private val objects: MutableList<String>,
                           private val onAutoCompleteListener: OnAutoCompleteListener)
    : ArrayAdapter<String>(context, resource, objects) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = super.getDropDownView(position, convertView, parent).apply{
        setOnClickListener { onAutoCompleteListener.onAutoComplete(objects[position]) }
    }



    interface OnAutoCompleteListener{
        fun onAutoComplete(string: String)
    }
}