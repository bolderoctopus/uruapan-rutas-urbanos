package com.rico.omarw.rutasuruapan

import android.content.Context
import android.content.DialogInterface.OnDismissListener
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable

class InformativeDialog {
    enum class Style {
        Left,
        Center
    }

    companion object {
        fun show(
            context: Context,
            verticalOffset: Int,
            style: Style,
            message: Int,
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