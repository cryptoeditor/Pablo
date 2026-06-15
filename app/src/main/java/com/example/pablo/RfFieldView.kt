package com.example.pablo

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hosts the Three.js RF-field scene (assets/rf_scene.html) inside a WebView.
 *
 * The 3D rendering happens in JavaScript/WebGL; this Kotlin side just creates
 * the WebView and points it at the bundled HTML. Everything is offline — the
 * Three.js library is bundled in assets, so no internet permission is needed.
 *
 * Later: call evaluateJavascript("setFields(...)") to push real signal data in.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RfFieldView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(Color.TRANSPARENT)
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/rf_scene.html")
            }
        }
    )
}
