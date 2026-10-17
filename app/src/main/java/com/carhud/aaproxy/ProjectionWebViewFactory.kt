package com.carhud.aaproxy

import android.content.Context
import android.content.MutableContextWrapper
import android.os.Build
import android.webkit.WebView

internal object ProjectionWebViewFactory {
    // View and Chromium cache resources at construction. Changing a wrapper alone
    // does not update those cached metrics when moving to another display. Even
    // equal-DPI displays can leave Chromium bound to the phone after reattachment.
    fun needsRecreation(web: WebView, targetContext: Context): Boolean =
        web.resources.displayMetrics.densityDpi != targetContext.resources.displayMetrics.densityDpi ||
            displayId(web.context) != displayId(targetContext)

    private fun displayId(context: Context): Int? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display?.displayId
        else @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.displayId
    } catch (_: Exception) { null }

    fun create(context: Context): BackgroundAudioWebView =
        BackgroundAudioWebView(MutableContextWrapper(context))
}
