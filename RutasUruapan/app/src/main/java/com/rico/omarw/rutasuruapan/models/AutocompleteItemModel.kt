package com.rico.omarw.rutasuruapan.models

import android.text.SpannableString
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place

data class AutocompleteItemModel(
        val kind: ItemKind,
        val primaryText: String,
        val secondaryText: String,
        val autocompletePrediction: AutocompletePrediction? = null,
        val currentPlace: Place? = null){

    enum class ItemKind{
        CurrentLocation,
        AutocompletePrediction,
        PickLocation
    }

    constructor(autocompletePrediction: AutocompletePrediction):
            this(ItemKind.AutocompletePrediction, autocompletePrediction.getPrimaryText(null).toString(), autocompletePrediction.getSecondaryText(null).toString(), autocompletePrediction)

    override fun toString(): String = primaryText

}