package com.rostamvpn.android.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rostamvpn.android.R
import com.rostamvpn.android.backend.Tunnel.State
import com.rostamvpn.android.model.ObservableTunnelListener

/**
 * A simple [ViewModel] subclass.
 */
class TunnelListFragmentViewModel: ViewModel(), ObservableTunnelListener {
    val isLoading = MutableLiveData<Boolean>()
    val tunnelUp = MutableLiveData<Boolean>()
    val tunnelStatusImageId = MutableLiveData<Int>()

    init {
        isLoading.value = false
        tunnelUp.value = false
        tunnelStatusImageId.value = R.drawable.rostam_logo_inactive
    }

    fun setLoading(isLoading: Boolean) {
        this.isLoading.value = isLoading
    }

    override fun stateChanged(state: State) {
        Log.e(TAG, state.toString())
        if (state == State.UP) {
            tunnelUp.postValue(true)
            tunnelStatusImageId.postValue(R.drawable.rostam_logo_active)
        }
        else {
            tunnelUp.postValue(false)
            tunnelStatusImageId.postValue(R.drawable.rostam_logo_inactive)
        }
    }

    companion object {
        private const val TAG = "RostamVPN/TunnelListFragmentViewModel"
    }
}
