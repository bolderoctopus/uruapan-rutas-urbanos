package com.rico.omarw.rutasuruapan.customWidgets

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.rico.omarw.rutasuruapan.Constants.DEBUG_TAG

public class CustomScrollingViewBehavior : com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior {
    constructor():super()
    constructor(context: Context?, attrs: AttributeSet?) : super (context, attrs)
    fun log(s: String){
        Log.d(DEBUG_TAG, s)
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        log("scroll")
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
    }


    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        log("start")
        return true
//        return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type).apply {
//            log("onStartNestedScroll: $this")
//        }
    }

    override fun onNestedScrollAccepted(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View, target: View, axes: Int, type: Int) {
        log("accepted")
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, axes, type)
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, type: Int) {
        log("stop")
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        log("pre")

        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }

}