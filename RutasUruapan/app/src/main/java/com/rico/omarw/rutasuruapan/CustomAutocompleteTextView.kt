package com.rico.omarw.rutasuruapan

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout

class CustomAutocompleteTextView(context: Context?, attrs: AttributeSet) : RelativeLayout(context, attrs),
        TextWatcher{

    var onClear: (sender: View) -> Unit = {}
    val autoCompleteTextView: AutoCompleteTextView
    private val clearButton: ImageView


    init {
        View.inflate(context, R.layout.custom_textview, this)
        autoCompleteTextView = findViewById(R.id.autocompletetextview)
        clearButton = findViewById(R.id.imagebutton)

        clearButton.visibility = INVISIBLE
        clearButton.setOnClickListener {
            onClear(this)
            autoCompleteTextView.setText("")
        }
        autoCompleteTextView.addTextChangedListener(this)
    }

    override fun afterTextChanged(editable: Editable) {
        clearButton.visibility = if(editable.isEmpty()) INVISIBLE
                                else VISIBLE
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit



}