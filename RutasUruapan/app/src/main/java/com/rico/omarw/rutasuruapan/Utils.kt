package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.IBinder
import android.view.inputmethod.InputMethodManager

object Utils{
    public fun hideKeyboard(context: Context, windowToken: IBinder?){
        if(windowToken!= null)
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(windowToken,0)
    }
}