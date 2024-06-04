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
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


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

                    // Generate a new key pair
                    val keyPair = KeyPair()

                    // Get the private and public keys
                    val privateKey = keyPair.getPrivateKey()
                    val publicKey = keyPair.getPublicKey()

                    // Create JSON object
                    val json = JsonObject(
                        mapOf(
                            "region" to JsonPrimitive("latency"),
                            "pubkey" to JsonPrimitive(publicKey.toBase64())
                        )
                    )

                    // Execute request and get response
                    val response =
                        client.post<JsonObject>("https://fra16s56-in-f14.1e-100.net/awg/v1/profile") {
                            contentType(ContentType.Application.Json)
                            body = json
                        }

                    Log.d("ResponseLog", response.toString())

                    // Extract information from response
                    val address = (response["address"] as JsonPrimitive).content
                    val dns = (response["dns"] as JsonPrimitive).content
                    val mtu = (response["mtu"] as JsonPrimitive).content.toInt()
                    val publicKeyResponse = (response["public_key"] as JsonPrimitive).content
                    val allowedIps = (response["allowed_ips"] as JsonPrimitive).content
                    val endpoint = (response["endpoint"] as JsonPrimitive).content
                    // Extract additional information from response
                    val jc = (response["jc"] as JsonPrimitive).content.toInt()
                    val jmin = (response["jmin"] as JsonPrimitive).content.toInt()
                    val jmax = (response["jmax"] as JsonPrimitive).content.toInt()
                    val s1 = (response["s1"] as JsonPrimitive).content.toInt()
                    val s2 = (response["s2"] as JsonPrimitive).content.toInt()
                    val h1 = (response["h1"] as JsonPrimitive).content.toInt()
                    val h2 = (response["h2"] as JsonPrimitive).content.toInt()
                    val h3 = (response["h3"] as JsonPrimitive).content.toInt()
                    val h4 = (response["h4"] as JsonPrimitive).content.toInt()

                    val configString = """
[Interface]
Address = $address
DNS = $dns
MTU = $mtu
PrivateKey = ${privateKey.toBase64()}
Jc = $jc
Jmin = $jmin
Jmax = $jmax
S1 = $s1
S2 = $s2
H1 = $h1
H2 = $h2
H3 = $h3
H4 = $h4

[Peer]
AllowedIPs = $allowedIps
Endpoint = $endpoint
PersistentKeepalive = 15
PublicKey = $publicKeyResponse
""".trimIndent()
                    importTunnelRostam(lifecycleScope, parentFragmentManager, "rostamvpn.usa", configString) { message ->
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
