/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.rostamvpn.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rostamvpn.android.backend.AwgQuickBackend
import com.rostamvpn.android.util.applicationScope
import kotlinx.coroutines.launch

class BootShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        applicationScope.launch {
            if (Application.getBackend() !is AwgQuickBackend) return@launch
            val tunnelManager = Application.getTunnelManager()
            if (Intent.ACTION_BOOT_COMPLETED == action) {
                Log.i(TAG, "Broadcast receiver restoring state (boot)")
                tunnelManager.restoreState(false)
            } else if (Intent.ACTION_SHUTDOWN == action) {
                Log.i(TAG, "Broadcast receiver saving state (shutdown)")
                tunnelManager.saveState()
            }
        }
    }

    companion object {
        private const val TAG = "RostamVPN/BootShutdownReceiver"
    }
}
