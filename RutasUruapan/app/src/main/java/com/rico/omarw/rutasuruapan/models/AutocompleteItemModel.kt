package com.rico.omarw.rutasuruapan.models

import android.text.SpannableString
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import com.google.android.libraries.places.api.model.AutocompletePrediction

data class AutocompleteItemModel(
        val primaryText: String,
        val secondaryText: String,
        val isCurrentLocation: Boolean,
        val autocompletePrediction: AutocompletePrediction? = null){

    constructor(autocompletePrediction: AutocompletePrediction):
            this(autocompletePrediction.getPrimaryText(null).toString(), autocompletePrediction.getSecondaryText(null).toString(), false, autocompletePrediction)

}