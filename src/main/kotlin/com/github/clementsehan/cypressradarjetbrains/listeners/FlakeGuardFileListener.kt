package com.github.clementsehan.cypressradarjetbrains.listeners

import com.github.clementsehan.cypressradarjetbrains.services.FlakeGuardService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

private val SPEC_SUFFIXES = listOf(".spec.js", ".cy.js", ".cy.ts", ".spec.ts")
private val LOG = logger<FlakeGuardFileListener>()

class FlakeGuardFileListener(private val project: Project) : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        LOG.info("Flake Guard: fileOpened — ${file.name}")
        triggerFetchIfSpec(file)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val file = event.newFile ?: return
        LOG.info("Flake Guard: selectionChanged — ${file.name}")
        triggerFetchIfSpec(file)
    }

    private fun triggerFetchIfSpec(file: VirtualFile) {
        if (!isSpecFile(file)) {
            LOG.info("Flake Guard: skipping non-spec file — ${file.name}")
            return
        }
        val relativePath = toRelativePath(file) ?: return
        LOG.info("Flake Guard: triggering fetch for $relativePath")
        project.service<FlakeGuardService>().fetchIfNeeded(relativePath)
    }

    private fun isSpecFile(file: VirtualFile) = SPEC_SUFFIXES.any { file.name.endsWith(it) }

    private fun toRelativePath(file: VirtualFile): String? {
        val basePath = project.basePath ?: return null
        val filePath = file.path
        return if (filePath.startsWith(basePath)) filePath.removePrefix("$basePath/") else null
    }
}
