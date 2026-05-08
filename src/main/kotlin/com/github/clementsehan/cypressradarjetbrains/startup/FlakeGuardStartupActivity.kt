package com.github.clementsehan.cypressradarjetbrains.startup

import com.github.clementsehan.cypressradarjetbrains.listeners.FlakeGuardFileListener
import com.github.clementsehan.cypressradarjetbrains.services.FlakeGuardService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

private val LOG = logger<FlakeGuardStartupActivity>()
private val SPEC_SUFFIXES = listOf(".spec.js", ".cy.js", ".cy.ts", ".spec.ts")

class FlakeGuardStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        LOG.info("Flake Guard: startup activity running")
        val service = project.service<FlakeGuardService>()

        // Subscribe listener via message bus (reliable, not subject to timing issues)
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            FlakeGuardFileListener(project)
        )

        // Handle files that were already open before our listener was registered
        val basePath = project.basePath ?: return
        FileEditorManager.getInstance(project).openFiles.forEach { file ->
            if (SPEC_SUFFIXES.any { file.name.endsWith(it) } && file.path.startsWith(basePath)) {
                val relativePath = file.path.removePrefix("$basePath/")
                LOG.info("Flake Guard: fetching for already-open spec: $relativePath")
                service.fetchIfNeeded(relativePath)
            }
        }
    }
}
