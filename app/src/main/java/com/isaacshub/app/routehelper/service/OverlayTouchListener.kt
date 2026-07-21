package com.isaacshub.app.routehelper.service

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * Touch listener that allows the overlay bubble to be dragged around the screen.
 */
class OverlayTouchListener(
    private val layoutParams: WindowManager.LayoutParams,
    private val windowManager: WindowManager?
) : View.OnTouchListener {

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                windowManager?.updateViewLayout(view, layoutParams)
                return true
            }
        }
        return false
    }
}
