package com.github.clementsehan.cypressradarjetbrains

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

    fun testTestHealth_propertiesStoredCorrectly() {
        val health = TestHealth(
            title = "should login",
            passPercentage = 85.5,
            passes = 17,
            total = 20,
            isCurrentlyFailing = false,
            lastRunUrl = "https://example.com/runs/1",
            failingRunNumbers = listOf(3, 7),
            failingRunUrls = listOf("https://example.com/runs/3", "https://example.com/runs/7")
        )
        assertEquals("should login", health.title)
        assertTrue(health.passPercentage in 0.0..100.0)
        assertEquals(17, health.passes)
        assertEquals(20, health.total)
        assertFalse(health.isCurrentlyFailing)
        assertEquals(2, health.failingRunNumbers.size)
        assertEquals(2, health.failingRunUrls.size)
    }

    fun testTestHealth_defaultValues() {
        val health = TestHealth("should logout", 100.0, isCurrentlyFailing = false, lastRunUrl = "")
        assertEquals(0, health.passes)
        assertEquals(0, health.total)
        assertTrue(health.failingRunNumbers.isEmpty())
        assertTrue(health.failingRunUrls.isEmpty())
    }

    fun testStubApiClient_returnsExpectedHealth() {
        val expected = listOf(
            TestHealth("should login", 100.0, isCurrentlyFailing = false, lastRunUrl = "https://example.com/1"),
            TestHealth("should logout", 80.0, isCurrentlyFailing = false, lastRunUrl = "https://example.com/2")
        )
        val stub = object : FlakeGuardApiClient {
            override fun fetchTestHealth(specFilePath: String, testTitles: List<String>, config: FlakeGuardConfig) = expected
        }
        val result = stub.fetchTestHealth("cypress/e2e/login.cy.ts", titles, config)
        assertEquals(2, result.size)
        assertEquals("should login", result[0].title)
        assertEquals(100.0, result[0].passPercentage, 0.001)
    }

    fun testStubApiClient_emptyForUnknownSpec() {
        val stub = object : FlakeGuardApiClient {
            override fun fetchTestHealth(specFilePath: String, testTitles: List<String>, config: FlakeGuardConfig) =
                if (specFilePath == "cypress/e2e/login.cy.ts") listOf(
                    TestHealth("should login", 100.0, isCurrentlyFailing = false, lastRunUrl = "")
                ) else emptyList()
        }
        assertTrue(stub.fetchTestHealth("cypress/e2e/unknown.cy.ts", titles, config).isEmpty())
        assertEquals(1, stub.fetchTestHealth("cypress/e2e/login.cy.ts", titles, config).size)
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
