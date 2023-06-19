package com.rico.omarw.rutasuruapan.customWidgets

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.rico.omarw.rutasuruapan.R

fun showOutOfBoundsSnack(coordinatorLayout: View) {
    Snackbar.make(coordinatorLayout, R.string.out_of_bounds_error, Snackbar.LENGTH_SHORT)
        .show()
}