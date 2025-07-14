package com.rico.omarw.rutasuruapan

import android.content.Context
import android.content.DialogInterface.OnDismissListener
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.preference.PreferenceManager
import com.rico.omarw.rutasuruapan.Constants.PreferenceKeys

class InformativeDialogs {
    enum class Style {
        Left,
        Center
    }

    companion object {

        fun shouldDisplayHowToShowRouteDialog(context: Context): Boolean {
            return !PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PreferenceKeys.HOW_TO_SHOW_ROUTE_DIALOG_SHOWN, false)
        }

        fun howToShowRouteDialogDisplayed(context: Context) {
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putBoolean(PreferenceKeys.HOW_TO_SHOW_ROUTE_DIALOG_SHOWN, true)
            }
        }

        fun displayHowToShowRouteDialog(
            context: Context, verticalOffset: Int, onDismissListener: OnDismissListener?
        ) {
            show(context, verticalOffset, Style.Left,
                R.string.how_to_show_routes_message, onDismissListener
            )
        }


        fun show(
            context: Context,
            verticalOffset: Int,
            style: Style,
            @StringRes message: Int,
            onDismissListener: OnDismissListener?
        ) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_help, null)

            when (style) {
                Style.Left -> dialogView.setBackgroundResource(R.drawable.dialog_background_left)
                Style.Center -> dialogView.setBackgroundResource(R.drawable.dialog_background_center)
            }

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setOnDismissListener(onDismissListener)
                .create()

            dialog.window?.let {
                it.decorView.background = Color.TRANSPARENT.toDrawable()
                it.decorView.setPadding(0, 0, 0, 0)

                it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                it.requestFeature(Window.FEATURE_NO_TITLE)
                it.attributes.apply {
                    gravity = Gravity.BOTTOM
                    y = verticalOffset
                }
            }

            dialogView.findViewById<TextView>(R.id.textview_message)?.setText(message)
            dialogView.setOnClickListener { dialog.dismiss() }

            dialog.show()
        }
    }
}