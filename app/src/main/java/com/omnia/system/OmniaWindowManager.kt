
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
    private val drawTaskbarButton: Button,
    private val browserTaskbarButton: Button
) {

    private var drawWindow: LinearLayout? = null
    private var browserWindow: LinearLayout? = null

    private var browserApp: OmniaBrowser? = null

    private var lastActiveWindow: String? = null

    fun openDraw() {
        if (drawWindow != null) {
            restoreDraw()
            return
        }

        val window = createWindow("OMNIA Draw") {
            minimizeDraw()
        } {
            closeDraw()
        }

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
        drawTaskbarButton.visibility = View.VISIBLE
        lastActiveWindow = "draw"

        desktop.post {
            showWindow(window)
        }
    }

    fun openBrowser() {
        if (browserWindow != null) {
            restoreBrowser()
            return
        }

        val window = createWindow("OMNIA Browser") {
            minimizeBrowser()
        } {
            closeBrowser()
        }

        val app = OmniaBrowser(activity)
        browserApp = app

        window.addView(
            app.create(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        browserWindow = window
        browserTaskbarButton.visibility = View.VISIBLE
        lastActiveWindow = "browser"

        desktop.post {
            showWindow(window)
        }
    }

    private fun createWindow(
        titleText: String,
        minimizeAction: () -> Unit,
        closeAction: () -> Unit
    ): LinearLayout {

        val window = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(30, 33, 38))
            elevation = dp(12).toFloat()
        }

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(6), 0)
            setBackgroundColor(Color.rgb(43, 47, 54))
        }

        val title = TextView(activity).apply {
            text = titleText
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
            setOnClickListener { minimizeAction() }
        }

        val closeButton = Button(activity).apply {
            text = "X"
            textSize = 13f
            setOnClickListener { closeAction() }
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

        return window
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

        desktop.addView(
            window,
            FrameLayout.LayoutParams(
                windowWidth,
                windowHeight,
                Gravity.CENTER
            )
        )
    }

    fun minimizeDraw() {
        val window = drawWindow ?: return
        desktop.removeView(window)
    }

    fun restoreDraw() {
        val window = drawWindow ?: return

        if (window.parent == null) {
            showWindow(window)
        }

        lastActiveWindow = "draw"
    }

    fun closeDraw() {
        val window = drawWindow ?: return

        desktop.removeView(window)
        drawWindow = null
        drawTaskbarButton.visibility = View.GONE

        if (lastActiveWindow == "draw") {
            lastActiveWindow =
                if (browserWindow != null) "browser" else null
        }
    }

    fun minimizeBrowser() {
        val window = browserWindow ?: return
        desktop.removeView(window)
    }

    fun restoreBrowser() {
        val window = browserWindow ?: return

        if (window.parent == null) {
            showWindow(window)
        }

        lastActiveWindow = "browser"
    }

    fun closeBrowser() {
        val window = browserWindow ?: return

        desktop.removeView(window)

        browserApp?.destroy()
        browserApp = null

        browserWindow = null
        browserTaskbarButton.visibility = View.GONE

        if (lastActiveWindow == "browser") {
            lastActiveWindow =
                if (drawWindow != null) "draw" else null
        }
    }

    fun closeActiveWindow(): Boolean {
        return when (lastActiveWindow) {
            "draw" -> {
                closeDraw()
                true
            }

            "browser" -> {
                closeBrowser()
                true
            }

            else -> false
        }
    }

    fun destroy() {
        closeDraw()
        closeBrowser()
    }

    private fun dp(value: Int): Int {
        return (
            value * activity.resources.displayMetrics.density
        ).toInt()
    }
}