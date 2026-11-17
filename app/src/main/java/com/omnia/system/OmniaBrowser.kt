
package com.omnia.system

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*

class OmniaBrowser(private val activity: Activity) {

    private val dark = Color.rgb(20, 22, 27)
    private val panel = Color.rgb(31, 34, 41)
    private val accent = Color.rgb(80, 150, 255)
    private val textColor = Color.WHITE

    private val tabs = mutableListOf<WebView>()
    private var currentTab = 0

    private lateinit var root: LinearLayout
    private lateinit var addressBar: EditText
    private lateinit var tabLabel: TextView
    private lateinit var progress: ProgressBar
    private lateinit var webContainer: FrameLayout

    @SuppressLint("SetJavaScriptEnabled")
    fun create(): View {

        root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }

        // Header
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 10, 10, 10)
            setBackgroundColor(panel)
        }

        addressBar = EditText(activity).apply {
            hint = "Search or enter website"
            setSingleLine(true)
            textSize = 14f
            setTextColor(textColor)
            setHintTextColor(Color.LTGRAY)
            setPadding(12, 8, 12, 8)
            background = rounded(panel, 12)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
        }

        addressBar.setOnEditorActionListener { _, action, _ ->
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadAddress(addressBar.text.toString())
                true
            } else false
        }

        header.addView(addressBar, LinearLayout.LayoutParams(0, 45, 1f))

        header.addView(button("GO") {
            loadAddress(addressBar.text.toString())
        })

        root.addView(header)

        // Navigation bar
        val navigation = LinearLayout(activity).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(panel)
        }

        navigation.addView(button("‹") { goBack() })
        navigation.addView(button("›") { goForward() })
        navigation.addView(button("↻") { reload() })
        navigation.addView(button("⌂") { openHome() })
        navigation.addView(button("+") { newTab() })

        tabLabel = TextView(activity).apply {
            text = "Tab 1"
            textSize = 12f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }

        navigation.addView(tabLabel)
        navigation.addView(button("×") { closeTab() })

        root.addView(navigation)

        // Loading progress
        progress = ProgressBar(
            activity,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progressTintList =
                android.content.res.ColorStateList.valueOf(accent)
            visibility = View.GONE
        }

        root.addView(
            progress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                3
            )
        )

        // Web content
        webContainer = FrameLayout(activity).apply {
            setBackgroundColor(Color.WHITE)
        }

        root.addView(
            webContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        newTab()

        return root
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {

        return WebView(activity).apply {

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {

                    val url = request.url.toString()

                    return !(url.startsWith("https://") ||
                            url.startsWith("http://"))
                }

                override fun onPageFinished(
                    view: WebView,
                    url: String
                ) {
                    if (tabs.getOrNull(currentTab) === view) {
                        addressBar.setText(url)
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {

                override fun onProgressChanged(
                    view: WebView,
                    newProgress: Int
                ) {
                    if (tabs.getOrNull(currentTab) === view) {
                        progress.progress = newProgress
                        progress.visibility =
                            if (newProgress < 100) View.VISIBLE
                            else View.GONE
                    }
                }

                override fun onReceivedTitle(
                    view: WebView,
                    title: String?
                ) {
                    if (tabs.getOrNull(currentTab) === view) {
                        tabLabel.text = title ?: "OMNIA Browser"
                    }
                }
            }
        }
    }

    private fun newTab() {

        val webView = createWebView()
        tabs.add(webView)

        currentTab = tabs.lastIndex

        showCurrentTab()
        openHome()
    }

    private fun showCurrentTab() {

        if (tabs.isEmpty()) return

        webContainer.removeAllViews()

        val webView = tabs[currentTab]

        webContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        tabLabel.text = "Tab ${currentTab + 1}"
        addressBar.setText(webView.url ?: "")
    }

    private fun closeTab() {

        if (tabs.isEmpty()) return

        val webView = tabs.removeAt(currentTab)

        webContainer.removeView(webView)
        webView.stopLoading()
        webView.destroy()

        if (tabs.isEmpty()) {
            newTab()
            return
        }

        currentTab = currentTab.coerceAtMost(tabs.lastIndex)
        showCurrentTab()
    }

    private fun loadAddress(input: String) {

        val value = input.trim()

        if (value.isEmpty()) return

        val url = when {
            value.startsWith("https://") ||
                    value.startsWith("http://") -> value

            value.contains(" ") ->
                "https://www.google.com/search?q=" +
                        java.net.URLEncoder.encode(value, "UTF-8")

            value.contains(".") -> "https://$value"

            else ->
                "https://www.google.com/search?q=" +
                        java.net.URLEncoder.encode(value, "UTF-8")
        }

        tabs.getOrNull(currentTab)?.loadUrl(url)
    }

    private fun openHome() {
        tabs.getOrNull(currentTab)?.loadUrl(
            "https://www.google.com"
        )
    }

    private fun goBack() {
        tabs.getOrNull(currentTab)?.let {
            if (it.canGoBack()) it.goBack()
        }
    }

    private fun goForward() {
        tabs.getOrNull(currentTab)?.let {
            if (it.canGoForward()) it.goForward()
        }
    }

    private fun reload() {
        tabs.getOrNull(currentTab)?.reload()
    }

    fun destroy() {
        tabs.forEach {
            it.stopLoading()
            it.destroy()
        }
        tabs.clear()
    }

    private fun button(
        label: String,
        action: () -> Unit
    ): TextView {

        return TextView(activity).apply {
            text = label
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(textColor)
            setPadding(12, 10, 12, 10)
            setOnClickListener { action() }
        }
    }

    private fun rounded(
        color: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }
}