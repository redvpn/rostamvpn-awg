package com.rostamvpn.android.model

import com.rostamvpn.android.backend.Tunnel.State

interface ObservableTunnelListener {
    fun stateChanged(state: State)
}