package com.github.clementsehan.cypressradarjetbrains.services

import com.github.clementsehan.cypressradarjetbrains.api.CypressCloudApiClient
import com.github.clementsehan.cypressradarjetbrains.api.FlakeGuardApiClient
import com.github.clementsehan.cypressradarjetbrains.config.ConfigReader
import com.github.clementsehan.cypressradarjetbrains.gutter.FlakeGuardInlayRenderer
import com.github.clementsehan.cypressradarjetbrains.model.TestHealth
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.WindowManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// Matches it('title') / it("title") on the same line — group 2 is the title
private val STATIC_QUOTE_REGEX = Regex("""^\s*(?:it|test)\s*\(\s*(['"])(.*?)\1""")

// Matches it(`static title`) — template literal with no ${ interpolation — group 1 is the title
// Dollar-sign check is done in code to avoid having $ inside the character class with the backtick
private val STATIC_TEMPLATE_REGEX = Regex("""^\s*(?:it|test)\s*\(\s*`([^`]*)`""")

// Matches it( / test( alone on a line — title follows on the next line
private val MULTILINE_IT_REGEX = Regex("""^\s*(?:it|test)\s*\(\s*$""")

// Matches a quoted or uninterpolated template literal at start of a line (next-line title argument)
private val QUOTED_LITERAL_REGEX = Regex("""^\s*(['"])(.*?)\1""")
private val TEMPLATE_LITERAL_REGEX = Regex("""^\s*`([^`]*)`""")

// Matches it(`${dynamic}`) / it(variable) — cannot be resolved statically
private val DYNAMIC_TITLE_REGEX = Regex("""^\s*(?:it|test)\s*\(\s*[^'"\s]""")

private fun extractStaticTitle(line: String): String? =
    STATIC_QUOTE_REGEX.find(line)?.groupValues?.get(2)
        ?: STATIC_TEMPLATE_REGEX.find(line)?.groupValues?.get(1)?.takeIf { '$' !in it }

private fun extractNextLineTitle(line: String): String? =
    QUOTED_LITERAL_REGEX.find(line)?.groupValues?.get(2)
        ?: TEMPLATE_LITERAL_REGEX.find(line)?.groupValues?.get(1)?.takeIf { '$' !in it }

@Service(Service.Level.PROJECT)
class FlakeGuardService(private val project: Project) : Disposable {

    override fun dispose() {}


    companion object {
        private val LOG = logger<FlakeGuardService>()
        private val MOUSE_LISTENER_KEY = Key.create<Boolean>("FlakeGuardMouseListener")
    }

    var apiClient: FlakeGuardApiClient = CypressCloudApiClient()

    private val cache = ConcurrentHashMap<String, List<TestHealth>>()

    fun getHealthForSpec(specFilePath: String): List<TestHealth>? = cache[specFilePath]

    fun fetchIfNeeded(specFilePath: String) {
        if (cache.containsKey(specFilePath)) {
            LOG.info("Flake Guard: cache hit for $specFilePath — refreshing gutter icons")
            refreshGutterIconsOnEdt(specFilePath)
            return
        }

        val config = ConfigReader.read(project)
        if (config == null) {
            LOG.warn("Flake Guard: flake-guard.json not found or invalid — skipping fetch for $specFilePath")
            return
        }

        val basePath = project.basePath ?: return
        LOG.info("Flake Guard: starting fetch for $specFilePath (provider=${config.provider})")
        showStatusBarMessage("Flake Guard: Loading...")

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Parse test titles from disk so the mock (and real API) can use them
                val specFile = File("$basePath/$specFilePath")
                val testTitles = if (specFile.exists()) {
                    specFile.readLines().mapNotNull { extractStaticTitle(it) }
                } else emptyList()

                LOG.info("Flake Guard: found ${testTitles.size} static titles in $specFilePath")
                val results = apiClient.fetchTestHealth(specFilePath, testTitles, config)
                cache[specFilePath] = results
                LOG.info("Flake Guard: fetched ${results.size} entries for $specFilePath")
            } catch (e: Exception) {
                LOG.warn("Flake Guard: fetch failed for $specFilePath — ${e.message}")
                showStatusBarMessage("Flake Guard: Error — ${e.message?.take(80)}")
                showErrorNotification(e.message ?: "Unknown error")
                // Do NOT cache on failure so the next file-open retries
            } finally {
                showStatusBarMessage("Flake Guard: Ready")
                refreshGutterIconsOnEdt(specFilePath)
            }
        }
    }

    private fun refreshGutterIconsOnEdt(specFilePath: String) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            val basePath = project.basePath ?: return@invokeLater
            val absolutePath = "$basePath/$specFilePath"

            for (fileEditor in FileEditorManager.getInstance(project).allEditors) {
                val file = fileEditor.file ?: continue
                if (file.path != absolutePath) continue
                val textEditor = fileEditor as? TextEditor ?: continue
                LOG.info("Flake Guard: updating gutter for $specFilePath")
                updateEditorGutter(textEditor.editor, specFilePath)
            }
        }
    }

    private fun ensureMouseListener(editor: Editor) {
        if (editor.getUserData(MOUSE_LISTENER_KEY) == true) return
        editor.putUserData(MOUSE_LISTENER_KEY, true)
        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                if (event.mouseEvent.clickCount != 1) return
                val mouseX = event.mouseEvent.point.x
                val mouseY = event.mouseEvent.point.y
                editor.inlayModel.getBlockElementsInRange(0, editor.document.textLength)
                    .forEach { inlay ->
                        val renderer = inlay.renderer as? FlakeGuardInlayRenderer ?: return@forEach
                        val bounds = inlay.bounds ?: return@forEach
                        if (mouseY in bounds.y..bounds.y + bounds.height) {
                            renderer.handleClick(mouseX - bounds.x)
                        }
                    }
            }
        }, this)
    }

    private fun updateEditorGutter(editor: Editor, specFilePath: String) {
        ensureMouseListener(editor)
        val healthList = cache[specFilePath] ?: return
        val document = editor.document
        val inlayModel = editor.inlayModel

        // Remove stale Flake Guard inlays
        inlayModel.getBlockElementsInRange(0, document.textLength)
            .filter { it.renderer is FlakeGuardInlayRenderer }
            .forEach { it.dispose() }

        val lines = document.text.lines()
        lines.forEachIndexed { lineIndex, line ->
            if (lineIndex >= document.lineCount) return@forEachIndexed
            val offset = document.getLineStartOffset(lineIndex)

            val staticTitle = extractStaticTitle(line)
            if (staticTitle != null) {
                addHealthInlay(inlayModel, offset, staticTitle, healthList)
                return@forEachIndexed
            }

            // Multi-line form: it(\n  'title', ...)  — inlay on the it( line, title on next line
            if (MULTILINE_IT_REGEX.containsMatchIn(line)) {
                val nextLine = lines.getOrNull(lineIndex + 1)
                val nextLineTitle = nextLine?.let { extractNextLineTitle(it) }
                if (nextLineTitle != null) {
                    addHealthInlay(inlayModel, offset, nextLineTitle, healthList)
                } else {
                    LOG.info("Flake Guard: dynamic multi-line it() on line $lineIndex")
                    inlayModel.addBlockElement(offset, false, true, 0,
                        FlakeGuardInlayRenderer("dynamic:$lineIndex", null, isDynamic = true))
                }
                return@forEachIndexed
            }

            // Dynamic it() — template literal or variable title on the same line
            if (DYNAMIC_TITLE_REGEX.containsMatchIn(line)) {
                LOG.info("Flake Guard: dynamic it() on line $lineIndex")
                inlayModel.addBlockElement(offset, false, true, 0,
                    FlakeGuardInlayRenderer("dynamic:$lineIndex", null, isDynamic = true))
            }
        }
    }

    private fun addHealthInlay(
        inlayModel: com.intellij.openapi.editor.InlayModel,
        offset: Int,
        testTitle: String,
        healthList: List<TestHealth>
    ) {
        val health = healthList.find { it.title == testTitle || it.title.endsWith(testTitle) }
        if (health == null && healthList.isNotEmpty()) {
            LOG.info("Flake Guard: no match for '$testTitle'. API titles: ${healthList.map { it.title }.take(3)}")
        } else {
            LOG.info("Flake Guard: inlay for '$testTitle' — ${if (health != null) "${"%.0f".format(health.passPercentage)}%" else "new test"}")
        }
        inlayModel.addBlockElement(offset, false, true, 0, FlakeGuardInlayRenderer(testTitle, health))
    }

    fun invalidateAll() = cache.clear()

    private fun showStatusBarMessage(message: String) {
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                WindowManager.getInstance().getStatusBar(project)?.info = message
            }
        }
    }

    private fun showErrorNotification(message: String) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Cypress Radar")
                ?.createNotification("Flake guard", message, NotificationType.ERROR)
                ?.notify(project)
        }
    }
}
