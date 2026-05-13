package com.github.clementsehan.cypressradarjetbrains.api

import com.github.clementsehan.cypressradarjetbrains.config.FlakeGuardConfig
import com.github.clementsehan.cypressradarjetbrains.model.TestHealth

interface FlakeGuardApiClient {
    /**
     * Fetch health data for tests in the given spec file.
     * [testTitles] contains statically-resolved titles from the file; real implementations
     * query the API by spec path and may ignore this list entirely.
     */
    fun fetchTestHealth(specFilePath: String, testTitles: List<String>, config: FlakeGuardConfig): List<TestHealth>
}
