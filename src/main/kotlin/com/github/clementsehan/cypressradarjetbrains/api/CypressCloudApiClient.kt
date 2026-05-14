package com.github.clementsehan.cypressradarjetbrains.api

import com.github.clementsehan.cypressradarjetbrains.config.FlakeGuardConfig
import com.github.clementsehan.cypressradarjetbrains.model.TestHealth
import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LOG = logger<CypressCloudApiClient>()
private val JSON = Json { ignoreUnknownKeys = true; isLenient = true }

class CypressCloudApiClient : FlakeGuardApiClient {

    private val lock = Any()
    private var cachedRecords: List<TestRecord>? = null
    private var cacheTime: Long = 0

    private fun getOrFetchRecords(config: FlakeGuardConfig, startDate: String): List<TestRecord> {
        val cacheTtlMs = config.cache * 60 * 1_000L
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val cached = cachedRecords
            if (cached != null && (now - cacheTime) < cacheTtlMs) {
                LOG.info("Flake Guard: cache hit — ${cached.size} records (age ${(now - cacheTime) / 1000}s)")
                return cached
            }
        }
        val fresh = fetchTestDetails(config, startDate)
        synchronized(lock) {
            cachedRecords = fresh
            cacheTime = System.currentTimeMillis()
        }
        return fresh
    }

    override fun fetchTestHealth(
        specFilePath: String,
        testTitles: List<String>,
        config: FlakeGuardConfig
    ): List<TestHealth> {
        val startDate = LocalDate.now().minusDays(config.timeframe.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val records = getOrFetchRecords(config, startDate)
        LOG.info("Flake Guard: received ${records.size} records from Data Extract API")

        if (records.isEmpty()) return emptyList()

        val specName = specFilePath.substringAfterLast('/')

        val matchingRecords = records.filter { r ->
            r.spec == specFilePath
                || r.spec.endsWith("/$specFilePath")
                || r.spec.endsWith("/$specName")
                || r.spec == specName
                || specFilePath.endsWith("/${r.spec}")
        }

        if (matchingRecords.isEmpty()) {
            LOG.warn("Flake Guard: no records found for '$specFilePath'. Sample specs in response: ${records.map { it.spec }.distinct().take(5)}")
            return emptyList()
        }

        LOG.info("Flake Guard: ${matchingRecords.size} records matched for '$specFilePath'")

        val latestRunNumber = matchingRecords.maxOfOrNull { it.runNumber } ?: 0

        // (runNumber, replayUrl) — url comes directly from the API response
        data class Stats(
            var passes: Int = 0,
            var total: Int = 0,
            var lastUrl: String = "",
            var failingInLatest: Boolean = false,
            val failingRuns: MutableList<Pair<Int, String>> = mutableListOf()
        )
        val stats = mutableMapOf<String, Stats>()

        matchingRecords.forEach { r ->
            val title = r.testName.trim()
            if (title.isEmpty()) return@forEach
            val s = stats.getOrPut(title) { Stats() }
            s.total++
            if (r.status == "passed") {
                s.passes++
            } else if (r.status != "pending") {
                s.failingRuns += Pair(r.runNumber, r.testReplayUrl)
            }
            if (s.lastUrl.isEmpty() && r.testReplayUrl.isNotEmpty()) s.lastUrl = r.testReplayUrl
            if (r.runNumber == latestRunNumber && r.status != "passed" && r.status != "pending") {
                s.failingInLatest = true
            }
        }

        return stats.map { (title, s) ->
            val pct = if (s.total > 0) (s.passes.toDouble() / s.total) * 100.0 else 0.0
            val sortedFails = s.failingRuns.sortedBy { it.first }
            LOG.info(
                "Flake Guard: '$title' — ${s.passes}/${s.total} passed (${"%.1f".format(pct)}%)" +
                if (sortedFails.isEmpty()) "" else ", failed in runs: ${sortedFails.map { it.first }}"
            )
            TestHealth(
                title = title,
                passPercentage = pct,
                passes = s.passes,
                total = s.total,
                isCurrentlyFailing = s.failingInLatest,
                lastRunUrl = s.lastUrl,
                failingRunNumbers = sortedFails.map { it.first },
                failingRunUrls = sortedFails.map { it.second }
            )
        }
    }

    private fun fetchTestDetails(config: FlakeGuardConfig, startDate: String): List<TestRecord> {
        val projectsParams = config.projects.joinToString("") { "&projects=$it" }
        val endpoint = "https://cloud.cypress.io/enterprise-reporting/report" +
            "?token=${config.apiToken}" +
            "&report_id=test-details" +
            "&export_format=json" +
            "&start_date=$startDate" +
            projectsParams

        val logProjects = if (config.projects.isEmpty()) "" else "&projects=${config.projects.joinToString("&projects=")}"
        LOG.info("Flake Guard: GET enterprise-reporting/report?report_id=test-details&start_date=$startDate$logProjects")

        val conn = URI.create(endpoint).toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("Accept", "application/json")

        val code = conn.responseCode
        val body = try {
            if (code == 200) conn.inputStream.bufferedReader().readText()
            else conn.errorStream?.bufferedReader()?.readText() ?: ""
        } finally {
            conn.disconnect()
        }

        LOG.info("Flake Guard: API response $code — ${body.take(300)}")

        if (code == 401 || code == 403) {
            throw IOException(
                "$code Unauthorized — verify 'apiToken' in flake-guard.json is a valid " +
                "Cypress Cloud Data Extract API key (Enterprise feature). " +
                "Generate one at: cloud.cypress.io → Integrations → Data Extract API"
            )
        }
        if (code != 200) {
            throw IOException("Cypress Cloud Data Extract API $code: ${body.take(300)}")
        }

        return try {
            JSON.decodeFromString<List<TestRecord>>(body)
        } catch (e: Exception) {
            // Some API versions wrap the array in a data envelope
            try {
                JSON.decodeFromString<TestRecordsEnvelope>(body).data
            } catch (_: Exception) {
                LOG.warn("Flake Guard: JSON parse failed. Raw: ${body.take(500)}", e)
                emptyList()
            }
        }
    }
}

@Serializable
private data class TestRecordsEnvelope(
    val data: List<TestRecord> = emptyList()
)

@Serializable
private data class TestRecord(
    val spec: String = "",
    @SerialName("test_name") val testName: String = "",
    val status: String = "",    // "passed" | "failed" | "skipped" | "pending"
    @SerialName("run_number") val runNumber: Int = 0,
    @SerialName("test_replay_url") val testReplayUrl: String = ""
)
