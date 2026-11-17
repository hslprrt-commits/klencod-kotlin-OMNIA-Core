package com.omnia.system

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class OmniaWindowManager(
    private val activity: Activity,
    private val desktop: FrameLayout,
    private val taskbarButton: Button
) {

    private var drawWindow: LinearLayout? = null
    private var isMinimized = false

    fun openDraw() {
        if (drawWindow != null) {
            restoreDraw()
            return
        }

        val window = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(30, 33, 38))
            elevation = dp(12).toFloat()
        }

        // رأس النافذة
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(6), 0)
            setBackgroundColor(Color.rgb(43, 47, 54))
        }

        val title = TextView(activity).apply {
            text = "OMNIA Draw"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        val minimizeButton = Button(activity).apply {
            text = "_"
            textSize = 16f
            setOnClickListener {
                minimizeDraw()
            }
        }

        val closeButton = Button(activity).apply {
            text = "X"
            textSize = 13f
            setOnClickListener {
                closeDraw()
            }
        }

        header.addView(minimizeButton)
        header.addView(closeButton)

        window.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        // محتوى التطبيق
        val drawApp = OmniaDrawApp(activity)

        window.addView(
            drawApp,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        drawWindow = window
        isMinimized = false

        taskbarButton.visibility = View.VISIBLE

        desktop.post {
            showWindow(window)
        }
    }

    private fun showWindow(window: LinearLayout) {
        if (window.parent != null) return

        val availableWidth = desktop.width
        val availableHeight = desktop.height

        if (availableWidth <= 0 || availableHeight <= 0) return

        val landscape = availableWidth > availableHeight

        val windowWidth = if (landscape) {
            (availableWidth * 0.78f).toInt()
        } else {
            (availableWidth * 0.94f).toInt()
        }

        val windowHeight = if (landscape) {
            (availableHeight * 0.88f).toInt()
        } else {
            (availableHeight * 0.82f).toInt()
        }

        val params = FrameLayout.LayoutParams(
            windowWidth,
            windowHeight,
            Gravity.CENTER
        )

        desktop.addView(window, params)
        isMinimized = false
    }

    fun minimizeDraw() {
        val window = drawWindow ?: return

        desktop.removeView(window)
        isMinimized = true
    }

    fun restoreDraw() {
        val window = drawWindow ?: return

        if (window.parent == null) {
            showWindow(window)
        }

        isMinimized = false
    }

    fun closeDraw() {
        val window = drawWindow ?: return

        desktop.removeView(window)
        drawWindow = null
        isMinimized = false
        taskbarButton.visibility = View.GONE
    }

    private fun dp(value: Int): Int {
        return (value * activity.resources.displayMetrics.density).toInt()
    }
}