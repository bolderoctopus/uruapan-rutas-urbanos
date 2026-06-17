package com.rico.omarw.rutasuruapan

import android.location.Address
import java.text.DecimalFormat

object AddressUtils {

    private val decimalFormat = DecimalFormat("#.#####")

    fun getShortAddress(address: Address): String {
        // use featureName if it's not the street number
        return if (address.featureName != address.subThoroughfare)
            address.featureName
        // use cords if street + subLocality are null or street + postalCode are null
        else if (address.thoroughfare == null || (address.subLocality == null || address.postalCode == null))
            decimalFormat.format(address.latitude) + ", " + decimalFormat.format(address.longitude)
        // use street + subLocality if it's not "Colonia"
        else if (address.subLocality != "Colonia")
            address.thoroughfare + ", " + address.subLocality
        // use street + postalCode
        else
            address.thoroughfare + ", " + address.postalCode
    }
}