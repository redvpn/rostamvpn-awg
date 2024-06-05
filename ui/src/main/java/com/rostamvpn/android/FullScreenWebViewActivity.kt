package com.rostamvpn.android

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class FullScreenWebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)

        // Configure WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            domStorageEnabled = false
            databaseEnabled = false
            javaScriptCanOpenWindowsAutomatically = false
            userAgentString = "RostamVPN/2.0 (Andrroid; Mobile)"
        }

        // Set WebViewClient to control the loading of URLs
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // Only load URLs that are from trusted sources
                if (url.startsWith("https://www.rostam.app")) {
                    return false // Allow WebView to load the URL
                }
                if (url.startsWith("https://api.rostam.app")) {
                    return false // Allow WebView to load the URL
                }
                return true // Prevent WebView from loading the URL
            }
        }

        val url = intent.getStringExtra(/* name = */ "url")
        Log.d("URL:", url.toString())

        if (url != null) {
            webView.loadUrl(url)
        }
    }
}
