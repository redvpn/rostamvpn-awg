package com.rostamvpn.android

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class FullScreenWebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        setContentView(webView)
        // Enable JavaScript
        webView.settings.javaScriptEnabled = true

        val url = intent.getStringExtra(/* name = */ "url")
        Log.d("URL:", url.toString())

        if (url != null) {
            webView.loadUrl(url)
        }

    }
}
