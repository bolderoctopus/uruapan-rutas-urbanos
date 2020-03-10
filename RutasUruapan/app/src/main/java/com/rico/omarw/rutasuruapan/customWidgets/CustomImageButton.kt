package com.rico.omarw.rutasuruapan.customWidgets

import android.content.Context
import android.graphics.PorterDuff
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.rico.omarw.rutasuruapan.R

class CustomImageButton(c: Context, layoutParams: RelativeLayout.LayoutParams): androidx.appcompat.widget.AppCompatImageView(c) {
    companion object {
        const val TAG = "SettingsButton"
    }

    init {
        tag = TAG
        isClickable = true
        scaleType = ScaleType.FIT_CENTER
        contentDescription = c.getString(R.string.title_activity_settings)
        setImageResource(R.drawable.settingsbutton4)
        setLayoutParams(layoutParams)
        setPadding(0,0,0,0)
        setOnTouchListener(this::onTouchEvent)
    }

    private fun onTouchEvent(view: View?, event: MotionEvent?): Boolean {
        val imageButton = view as ImageView
        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                imageButton.drawable.setColorFilter(0x44000000, PorterDuff.Mode.SRC_ATOP)
            }
            MotionEvent.ACTION_UP ->{
                imageButton.drawable.clearColorFilter()
            }
            MotionEvent.ACTION_CANCEL -> {
                imageButton.clearColorFilter()
            }

        }
        invalidate()
        return false
    }
}