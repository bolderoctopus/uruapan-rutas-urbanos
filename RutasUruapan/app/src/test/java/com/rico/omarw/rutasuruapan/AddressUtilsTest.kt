package com.rico.omarw.rutasuruapan

import android.location.Address
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class AddressUtilsTest {

    @Test
    fun `getShortAddress returns featureName when it is not street number`() {
        val address = Address(Locale.getDefault()).apply {
            featureName = "My Business"
            subThoroughfare = "123"
        }
        val result = AddressUtils.getShortAddress(address)
        assertEquals("My Business", result)
    }

    @Test
    fun `getShortAddress returns coordinates when street is null`() {
        val address = Address(Locale.getDefault()).apply {
            featureName = "19.4"
            subThoroughfare = "19.4"
            thoroughfare = null
            latitude = 19.4326
            longitude = -102.0621
        }
        val result = AddressUtils.getShortAddress(address)
        assertEquals("19.4326, -102.0621", result)
    }

    @Test
    fun `getShortAddress returns street and subLocality when subLocality is not Colonia`() {
        val address = Address(Locale.getDefault()).apply {
            featureName = "123"
            subThoroughfare = "123"
            thoroughfare = "Main St"
            subLocality = "Centro"
            postalCode = "60000"
        }
        val result = AddressUtils.getShortAddress(address)
        assertEquals("Main St, Centro", result)
    }

    @Test
    fun `getShortAddress returns street and postalCode when subLocality is Colonia`() {
        val address = Address(Locale.getDefault()).apply {
            featureName = "123"
            subThoroughfare = "123"
            thoroughfare = "Main St"
            subLocality = "Colonia"
            postalCode = "60000"
        }
        val result = AddressUtils.getShortAddress(address)
        assertEquals("Main St, 60000", result)
    }
}