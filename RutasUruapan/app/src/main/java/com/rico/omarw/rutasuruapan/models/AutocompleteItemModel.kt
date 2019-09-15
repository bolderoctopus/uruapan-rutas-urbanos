package com.rico.omarw.rutasuruapan.models

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction

data class AutocompleteItemModel(
        val kind: ItemKind,
        val primaryText: String,
        val secondaryText: String,
        val autocompletePrediction: AutocompletePrediction? = null,
        val currentLatLng: LatLng? = null){

    enum class ItemKind{
        CurrentLocation,
        AutocompletePrediction,
        PickLocation
    }

    constructor(autocompletePrediction: AutocompletePrediction):
            this(ItemKind.AutocompletePrediction, autocompletePrediction.getPrimaryText(null).toString(), autocompletePrediction.getSecondaryText(null).toString(), null)

    override fun toString(): String = primaryText

}