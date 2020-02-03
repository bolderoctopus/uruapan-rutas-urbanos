package com.rico.omarw.rutasuruapan

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager

class InformativeDialog{
    companion object{
        fun show(context: Context,verticalOffset: Int, message: Int){

            val preferenceEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            preferenceEditor.putBoolean("show_informative_dialog", false)
            preferenceEditor.apply()

            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_help, null)
            val dialog = AlertDialog.Builder(context)
                    .setView(dialogView)
                    .create()

            dialog.window?.decorView?.background = ColorDrawable(Color.TRANSPARENT)
            dialog.window?.decorView?.setPadding(0,0,0,0)

            dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
            dialog.window!!.attributes.apply {
                gravity = Gravity.BOTTOM
                y = verticalOffset
                y -= 50
            }

            dialogView.findViewById<Button>(R.id.button_ok)?.setOnClickListener {dialog.dismiss() }
            dialogView.findViewById<TextView>(R.id.textview_message)?.setText(message)

            dialog.show()
        }
    }
}