package com.rico.omarw.rutasuruapan

import android.content.Context
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.IBinder
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.inputmethod.InputMethodManager


object Utils{
    fun hideKeyboard(context: Context, windowToken: IBinder?){
        if(windowToken!= null)
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(windowToken,0)
    }

    fun formatRouteTitle(name: String, shortName: String): SpannableString {
        val spannableString = SpannableString("$name #$shortName")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), spannableString.length - shortName.length, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
    }


    fun checkInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}