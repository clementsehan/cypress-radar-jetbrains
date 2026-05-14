package com.github.clementsehan.cypressradarjetbrains.config

import kotlinx.serialization.Serializable

@Serializable
data class FlakeGuardConfig(
    val provider: String,
    val apiToken: String,
    val cache: Int = 30,
    val timeframe: Int = 7,
    val projects: List<String> = emptyList()
)
