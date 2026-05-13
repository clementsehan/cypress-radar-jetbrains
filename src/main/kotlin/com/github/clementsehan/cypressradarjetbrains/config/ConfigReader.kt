package com.github.clementsehan.cypressradarjetbrains.config

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import java.io.File

object ConfigReader {

    private val LOG = logger<ConfigReader>()
    private val json = Json { ignoreUnknownKeys = true }

    fun read(project: Project): FlakeGuardConfig? {
        val basePath = project.basePath ?: return null
        val configFile = File(basePath, "flake-guard.json")
        if (!configFile.exists()) {
            LOG.info("flake-guard.json not found at $basePath")
            return null
        }
        return try {
            json.decodeFromString<FlakeGuardConfig>(configFile.readText())
        } catch (e: Exception) {
            LOG.warn("Failed to parse flake-guard.json", e)
            null
        }
    }
}
