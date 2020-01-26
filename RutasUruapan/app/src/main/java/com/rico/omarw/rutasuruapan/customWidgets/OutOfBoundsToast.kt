package com.rico.omarw.rutasuruapan.customWidgets

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.rico.omarw.rutasuruapan.R

class OutOfBoundsToast(context: Context): Toast(context){

    init {
        view = LayoutInflater.from(context).inflate(R.layout.out_of_bounds_toast,null)
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(300).withEndAction {
            view.animate().alpha(0f).setDuration(300).setStartDelay(2_800).start()
        }.start()
        duration = LENGTH_LONG
    }

}