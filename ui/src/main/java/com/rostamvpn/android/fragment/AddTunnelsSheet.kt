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
import kotlinx.coroutines.launch

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

                // Check Connection
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
                        intent.putExtra(
                            "url",
                            "https://www.rostam.app/fa/articles/how-does-a-vpn-work/"
                        )
                        startActivity(intent)
                    }
                }
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