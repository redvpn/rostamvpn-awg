package com.rostamvpn.android.model

import kotlinx.serialization.Serializable

@Serializable
data class ConfigData(
    val firstRun: Boolean = true,
    var tunnelConfigTime: Long?,
    var tunnelConfig: String?
)
