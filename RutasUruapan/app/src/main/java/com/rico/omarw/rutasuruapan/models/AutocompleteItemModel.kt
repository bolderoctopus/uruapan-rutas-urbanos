package com.rico.omarw.rutasuruapan.models

import android.text.SpannableString
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place

data class AutocompleteItemModel(
        val primaryText: String,
        val secondaryText: String,
        val isCurrentLocation: Boolean,
        val autocompletePrediction: AutocompletePrediction? = null,
        val currentPlace: Place? = null){

    constructor(autocompletePrediction: AutocompletePrediction):
            this(autocompletePrediction.getPrimaryText(null).toString(), autocompletePrediction.getSecondaryText(null).toString(), false, autocompletePrediction)
    constructor(primaryText: String, place: Place):
            this(primaryText, place.address!!, true, null, place)

}