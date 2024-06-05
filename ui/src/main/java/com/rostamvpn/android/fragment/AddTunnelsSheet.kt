/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.rostamvpn.android.fragment

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rostamvpn.android.FullScreenWebViewActivity
import com.rostamvpn.android.R
import com.rostamvpn.android.crypto.KeyPair
import com.rostamvpn.android.util.TunnelImporter.importTunnelRostam
import com.rostamvpn.android.util.resolveAttribute
import io.ktor.client.HttpClient
import io.ktor.client.features.get
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Base64
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AddTunnelsSheet : BottomSheetDialogFragment() {
    private var behavior: BottomSheetBehavior<FrameLayout>? = null
    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                dismiss()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (savedInstanceState != null) dismiss()
        val view = inflater.inflate(R.layout.add_tunnels_bottom_sheet, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dialog = dialog as BottomSheetDialog? ?: return
                behavior = dialog.behavior
                behavior?.apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    peekHeight = 0
                    addBottomSheetCallback(bottomSheetCallback)
                }

// Check Connnction
                dialog.findViewById<View>(R.id.check_connection)?.setOnClickListener {
                    lifecycleScope.launch {
                        val intent = Intent(context, FullScreenWebViewActivity::class.java)
                        intent.putExtra("url", "https://api.rostam.app/")
                        startActivity(intent)
                    }
                }

                dialog.findViewById<View>(R.id.digital_security)?.setOnClickListener {
                    lifecycleScope.launch {
                        val intent = Intent(context, FullScreenWebViewActivity::class.java)
                        intent.putExtra("url", "https://www.rostam.app/fa/articles/how-does-a-vpn-work/")
                        startActivity(intent)
                    }
                }
                dialog.findViewById<View>(R.id.create_from_hardcoded)?.setOnClickListener {
                    lifecycleScope.launch {
                        val client = HttpClient {
                            install(JsonFeature) {
                                serializer = KotlinxSerializer(Json {
                                    isLenient = true
                                    ignoreUnknownKeys = true
                                })
                            }
                        }
                        //const val rostamApiUrl = "fra16s56-in-f14.1e-100.net"
                        val googleDocUrl = "https://docs.google.com/feeds/download/documents/export/Export?id=18g4AqcoHsbaKsxCeAU98zsvxB5u4HUwOvtCfO_vA8-4&amp;exportFormat=html"
                        val response: HttpResponse = client.get(googleDocUrl)
                        val contentDisposition = response.headers["content-disposition"]
                        val filename = contentDisposition?.substringAfter("filename=\"")?.substringBefore(".html\"")
                        val decodedFilename = Base64.getDecoder().decode(filename).toString(Charsets.UTF_8)
                        val jsonObject = Json.parseToJsonElement(decodedFilename) as JsonObject
                        val rostamApiUrl = (jsonObject["host"] as JsonPrimitive).content


                        // Generate a new key pair
                        val awg_keyPair = KeyPair()
                        val wg_keyPair = KeyPair()

                        val awg_privateKey= awg_keyPair.getPrivateKey()
                        val awg_publicKey = awg_keyPair.getPublicKey()

                        val wg_privateKey= wg_keyPair.getPrivateKey()
                        val wg_publicKey = wg_keyPair.getPublicKey()

                        // Create JSON object
                        val awg_json = JsonObject(
                            mapOf(
                                "region" to JsonPrimitive("latency"),
                                "pubkey" to JsonPrimitive(awg_publicKey.toBase64())
                            )
                        )
                        val wg_json = JsonObject(
                            mapOf(
                                "region" to JsonPrimitive("latency"),
                                "pubkey" to JsonPrimitive(wg_publicKey.toBase64())
                            )
                        )
                            // Execute request and get response
                        val wg_response = client.post<JsonObject>("https://" + rostamApiUrl + "/api/profile") {
                            contentType(ContentType.Application.Json)
                            body = wg_json
                        }
                        Log.d("ResponseLog", wg_response.toString())

                        val awg_response = client.post<JsonObject>("https://" + rostamApiUrl + "/awg/v1/profile") {
                                contentType(ContentType.Application.Json)
                                body = awg_json
                            }
                            Log.d("ResponseLog", awg_response.toString())

                        // Extract information from response
                        val awg_address = (awg_response["address"] as JsonPrimitive).content
                        val awg_dns = (awg_response["dns"] as JsonPrimitive).content
                        val awg_mtu = (awg_response["mtu"] as JsonPrimitive).content.toInt()
                        val awg_publicKeyResponse = (awg_response["public_key"] as JsonPrimitive).content
                        val awg_allowedIps = (awg_response["allowed_ips"] as JsonPrimitive).content
                        val awg_endpoint = (awg_response["endpoint"] as JsonPrimitive).content
                        // Extract additional information from response
                        val awg_jc = awg_response["jc"]?.let { it as JsonPrimitive }?.content?.toInt()
                        val awg_jmin = awg_response["jmin"]?.let { it as JsonPrimitive }?.content?.toInt()
                        val awg_jmax = awg_response["jmax"]?.let { it as JsonPrimitive }?.content?.toInt()
                        val awg_s1 = awg_response["s1"]?.let { it as JsonPrimitive }?.content?.toInt()
                        val awg_s2 = awg_response["s2"]?.let { it as JsonPrimitive }?.content?.toInt()
                        val awg_h1 = awg_response["h1"]?.let { it as JsonPrimitive }?.content?.toInt()
                        val awg_h2 = awg_response["h2"]?.let { it as JsonPrimitive }?.content?.toInt()
                        val awg_h3 = awg_response["h3"]?.let { it as JsonPrimitive }?.content?.toInt()
                        val awg_h4 = awg_response["h4"]?.let { it as JsonPrimitive }?.content?.toInt()

                        val awg_configString = """
[Interface]
Address = $awg_address
DNS = $awg_dns
MTU = $awg_mtu
PrivateKey = ${awg_privateKey.toBase64()}
Jc = $awg_jc
Jmin = $awg_jmin
Jmax = $awg_jmax
S1 = $awg_s1
S2 = $awg_s2
H1 = $awg_h1
H2 = $awg_h2
H3 = $awg_h3
H4 = $awg_h4

[Peer]
AllowedIPs = $awg_allowedIps
Endpoint = $awg_endpoint
PersistentKeepalive = 15
PublicKey = $awg_publicKeyResponse
""".trimIndent()

                        // Extract information from response
                        val wg_address = (wg_response["address"] as JsonPrimitive).content

                        val wg_dns = if (wg_response.contains("dns")) {
                            (wg_response["dns"] as JsonPrimitive).content
                        } else {
                            "8.8.8.8"
                        }

                        val wg_mtu = if (wg_response.contains("mtu")) {
                            (wg_response["dns"] as JsonPrimitive).content
                        } else {
                            "1240"
                        }

                        val wg_allowedIps = if (wg_response.contains("wg_allowedIps")) {
                            (wg_response["wg_allowedIps"] as JsonPrimitive).content
                        } else {
                            "0.0.0.0/0"
                        }

                        val wg_publicKeyResponse = (wg_response["pubkey"] as JsonPrimitive).content
                        val wg_endpoint = (wg_response["endpoint"] as JsonPrimitive).content

                        val wg_configString = """
[Interface]
Address = $wg_address
DNS = $wg_dns
MTU = $wg_mtu
PrivateKey = ${wg_privateKey.toBase64()}

[Peer]
AllowedIPs = $wg_allowedIps
Endpoint = $wg_endpoint
PersistentKeepalive = 15
PublicKey = $wg_publicKeyResponse
""".trimIndent()


                        importTunnelRostam(lifecycleScope, parentFragmentManager, "wireguard.usa", wg_configString) { message ->
                            Log.d("Tunnel Import Response", message.toString())
                        }

                        importTunnelRostam(lifecycleScope, parentFragmentManager, "amnezia.usa", awg_configString) { message ->
                            Log.d("Tunnel Import Response", message.toString())
                        }
                        dismiss()
                    }
                }
                val gradientDrawable = GradientDrawable().apply {
                    setColor(requireContext().resolveAttribute(com.google.android.material.R.attr.colorSurface))
                }
                view.background = gradientDrawable
            }
        })
    }

    override fun dismiss() {
        super.dismiss()
        behavior?.removeBottomSheetCallback(bottomSheetCallback)
    }

    private fun onRequestCreateConfig() {
        setFragmentResult(REQUEST_KEY_NEW_TUNNEL, bundleOf(REQUEST_METHOD to REQUEST_CREATE))
    }

    private fun onRequestImportConfig() {
        setFragmentResult(REQUEST_KEY_NEW_TUNNEL, bundleOf(REQUEST_METHOD to REQUEST_IMPORT))
    }

    private fun onRequestScanQRCode() {
        setFragmentResult(REQUEST_KEY_NEW_TUNNEL, bundleOf(REQUEST_METHOD to REQUEST_SCAN))
    }

    companion object {
        const val REQUEST_KEY_NEW_TUNNEL = "request_new_tunnel"
        const val REQUEST_METHOD = "request_method"
        const val REQUEST_CREATE = "request_create"
        const val REQUEST_IMPORT = "request_import"
        const val REQUEST_SCAN = "request_scan"
    }
}