
package com.omnia.system

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var desktop: FrameLayout
    private lateinit var drawTaskbarButton: Button
    private lateinit var browserTaskbarButton: Button
    private lateinit var windowManager: OmniaWindowManager

    private val backgroundColor = Color.rgb(17, 19, 22)
    private val panelColor = Color.rgb(28, 31, 36)
    private val textColor = Color.rgb(235, 238, 242)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(18), 0)
            setBackgroundColor(panelColor)
        }

        val systemName = TextView(this).apply {
            text = "OMNIA"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }

        val version = TextView(this).apply {
            text = "  SYSTEM 1.1"
            textSize = 10f
            setTextColor(Color.LTGRAY)
        }

        topBar.addView(systemName)
        topBar.addView(version)

        root.addView(
            topBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        )

        desktop = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(21, 23, 27))
        }

        root.addView(
            desktop,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val drawIcon = createDesktopIcon("✎\nOMNIA Draw") {
            windowManager.openDraw()
        }

        desktop.addView(
            drawIcon,
            FrameLayout.LayoutParams(
                dp(112),
                dp(100),
                Gravity.TOP or Gravity.START
            ).apply {
                setMargins(dp(18), dp(22), 0, 0)
            }
        )

        val browserIcon = createDesktopIcon("🌐\nOMNIA Browser") {
            windowManager.openBrowser()
        }

        desktop.addView(
            browserIcon,
            FrameLayout.LayoutParams(
                dp(112),
                dp(100),
                Gravity.TOP or Gravity.START
            ).apply {
                setMargins(dp(145), dp(22), 0, 0)
            }
        )

        val taskbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(panelColor)
        }

        val startButton = Button(this).apply {
            text = "OMNIA"
            textSize = 12f
            setOnClickListener {
                windowManager.openDraw()
            }
        }

        drawTaskbarButton = Button(this).apply {
            text = "Draw"
            textSize = 11f
            visibility = View.GONE
            setOnClickListener {
                windowManager.restoreDraw()
            }
        }

        browserTaskbarButton = Button(this).apply {
            text = "Browser"
            textSize = 11f
            visibility = View.GONE
            setOnClickListener {
                windowManager.restoreBrowser()
            }
        }

        taskbar.addView(
            startButton,
            LinearLayout.LayoutParams(dp(90), dp(46))
        )

        taskbar.addView(
            drawTaskbarButton,
            LinearLayout.LayoutParams(dp(90), dp(46)).apply {
                leftMargin = dp(6)
            }
        )

        taskbar.addView(
            browserTaskbarButton,
            LinearLayout.LayoutParams(dp(100), dp(46)).apply {
                leftMargin = dp(6)
            }
        )

        root.addView(
            taskbar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        setContentView(root)

        windowManager = OmniaWindowManager(
            this,
            desktop,
            drawTaskbarButton,
            browserTaskbarButton
        )
    }

    private fun createDesktopIcon(
        label: String,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(textColor)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setBackgroundColor(panelColor)
            setOnClickListener { action() }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (::windowManager.isInitialized) {
            if (!windowManager.closeActiveWindow()) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::windowManager.isInitialized) {
            windowManager.destroy()
        }
        super.onDestroy()
    }
}