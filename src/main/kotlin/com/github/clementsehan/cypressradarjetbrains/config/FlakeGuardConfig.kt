package com.github.clementsehan.cypressradarjetbrains.config

import kotlinx.serialization.Serializable

@Serializable
data class FlakeGuardConfig(
    val provider: String,
    val apiToken: String
)
