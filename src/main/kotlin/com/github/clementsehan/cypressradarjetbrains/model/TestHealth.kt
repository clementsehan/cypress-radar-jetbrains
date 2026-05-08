package com.github.clementsehan.cypressradarjetbrains.model

data class TestHealth(
    val title: String,
    val passPercentage: Double,
    val passes: Int = 0,
    val total: Int = 0,
    val isCurrentlyFailing: Boolean,
    val lastRunUrl: String,
    val failingRunNumbers: List<Int> = emptyList(),
    val failingRunUrls: List<String> = emptyList()
)
