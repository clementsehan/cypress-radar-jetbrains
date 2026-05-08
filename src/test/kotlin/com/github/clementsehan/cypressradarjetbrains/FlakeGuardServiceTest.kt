package com.github.clementsehan.cypressradarjetbrains

import com.github.clementsehan.cypressradarjetbrains.api.CypressCloudApiClient
import com.github.clementsehan.cypressradarjetbrains.api.FlakeGuardApiClient
import com.github.clementsehan.cypressradarjetbrains.config.ConfigReader
import com.github.clementsehan.cypressradarjetbrains.config.FlakeGuardConfig
import com.github.clementsehan.cypressradarjetbrains.model.TestHealth
import com.github.clementsehan.cypressradarjetbrains.services.FlakeGuardService
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FlakeGuardServiceTest : BasePlatformTestCase() {

    private val config = FlakeGuardConfig("cypress-cloud", "token")
    private val titles = listOf("should login", "should logout", "should show error")

    fun testGetHealthForSpec_returnsNullBeforeFetch() {
        val service = FlakeGuardService(project)
        assertNull(service.getHealthForSpec("cypress/e2e/login.cy.ts"))
    }

    fun testInvalidateAll_clearsCache() {
        val service = FlakeGuardService(project)
        service.invalidateAll()
        assertNull(service.getHealthForSpec("cypress/e2e/login.cy.ts"))
    }

    fun testCypressCloudApiClient_returnsDeterministicResults() {
        val client = CypressCloudApiClient()
        val result1 = client.fetchTestHealth("cypress/e2e/login.cy.ts", titles, config)
        val result2 = client.fetchTestHealth("cypress/e2e/login.cy.ts", titles, config)
        assertEquals(result1, result2)
        assertEquals(titles.size, result1.size)
        result1.forEach { health ->
            assertTrue(health.passPercentage in 0.0..100.0)
            assertTrue(health.lastRunUrl.contains("proj123"))
            assertTrue(health.title.isNotBlank())
        }
    }

    fun testCypressCloudApiClient_titlesMatchInput() {
        val client = CypressCloudApiClient()
        val result = client.fetchTestHealth("cypress/e2e/login.cy.ts", titles, config)
        assertEquals(titles, result.map { it.title })
    }

    fun testCypressCloudApiClient_emptyTitlesReturnsEmpty() {
        val client = CypressCloudApiClient()
        val result = client.fetchTestHealth("cypress/e2e/login.cy.ts", emptyList(), config)
        assertTrue(result.isEmpty())
    }

    fun testCypressCloudApiClient_differentPathsProduceDifferentResults() {
        val client = CypressCloudApiClient()
        val result1 = client.fetchTestHealth("cypress/e2e/login.cy.ts", titles, config)
        val result2 = client.fetchTestHealth("cypress/e2e/dashboard.cy.ts", titles, config)
        assertFalse(result1 == result2)
    }

    fun testConfigReader_returnsNullWhenFileAbsent() {
        assertNull(ConfigReader.read(project))
    }

    fun testFlakeGuardService_apiClientCanBeSwappedForTesting() {
        val service = FlakeGuardService(project)
        service.apiClient = object : FlakeGuardApiClient {
            override fun fetchTestHealth(
                specFilePath: String,
                testTitles: List<String>,
                config: FlakeGuardConfig
            ) = listOf(TestHealth("should login", 100.0, isCurrentlyFailing = false, lastRunUrl = "https://example.com/runs/1"))
        }
        assertNotNull(service.apiClient)
    }
}
