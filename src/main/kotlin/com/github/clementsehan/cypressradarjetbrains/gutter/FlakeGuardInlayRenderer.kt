package com.github.clementsehan.cypressradarjetbrains.gutter

import com.github.clementsehan.cypressradarjetbrains.model.TestHealth
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints

class FlakeGuardInlayRenderer(
    val testTitle: String,
    private val health: TestHealth?,
    private val isDynamic: Boolean = false
) : EditorCustomElementRenderer {

    private data class RunLink(val relX1: Int, val relX2: Int, val url: String)
    private val runLinks = mutableListOf<RunLink>()

    private companion object {
        const val H_PAD = 8
        const val V_MARGIN = 2
        const val CORNER = 5
        const val RUN_GAP = 2
        const val FONT_SCALE = 0.82f

        val BG_GREEN  = JBColor(0xD4EDDA, 0x1E4D2B)
        val BG_YELLOW = JBColor(0xFFF3CD, 0x4D3B00)
        val BG_RED    = JBColor(0xF8D7DA, 0x4D1A1D)
        val BG_GRAY   = JBColor(0xE2E3E5, 0x3C3F41)
        val BG_BLUE   = JBColor(0xCCE5FF, 0x1A3A5C)

        val FG_GREEN  = JBColor(0x155724, 0x82C996)
        val FG_YELLOW = JBColor(0x856404, 0xFFD966)
        val FG_RED    = JBColor(0x721C24, 0xFF8B8B)
        val FG_GRAY   = JBColor(0x383D41, 0xBBBBBB)
        val FG_BLUE   = JBColor(0x004085, 0x7EB8E8)
    }

    private val bgColor: JBColor get() = when {
        isDynamic -> BG_BLUE
        health == null -> BG_GRAY
        health.isCurrentlyFailing || health.passPercentage < 75.0 -> BG_RED
        health.passPercentage < 100.0 -> BG_YELLOW
        else -> BG_GREEN
    }

    private val fgColor: JBColor get() = when {
        isDynamic -> FG_BLUE
        health == null -> FG_GRAY
        health.isCurrentlyFailing || health.passPercentage < 75.0 -> FG_RED
        health.passPercentage < 100.0 -> FG_YELLOW
        else -> FG_GREEN
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int =
        inlay.editor.scrollingModel.visibleArea.width

    override fun calcHeightInPixels(inlay: Inlay<*>): Int =
        (inlay.editor.lineHeight * 0.82).toInt().coerceAtLeast(18)

    override fun paint(
        inlay: Inlay<*>,
        g: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes
    ) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            // Background pill
            g2.color = bgColor
            g2.fillRoundRect(
                targetRegion.x, targetRegion.y + V_MARGIN,
                targetRegion.width, targetRegion.height - V_MARGIN * 2,
                CORNER, CORNER
            )

            val baseFont = inlay.editor.colorsScheme.getFont(EditorFontType.PLAIN)
            val font = baseFont.deriveFont(inlay.editor.colorsScheme.editorFontSize * FONT_SCALE)
            g2.font = font
            val fm = g2.fontMetrics
            val baseline = targetRegion.y + V_MARGIN +
                (targetRegion.height - V_MARGIN * 2 + fm.ascent - fm.descent) / 2

            runLinks.clear()
            var x = targetRegion.x + H_PAD
            g2.color = fgColor

            when {
                isDynamic -> g2.drawString("dynamic title — data exists in Cypress Cloud but cannot be matched statically", x, baseline)
                health == null -> g2.drawString("new test — no historical data", x, baseline)
                else -> {
                    val pct = "${"%.1f".format(health.passPercentage)}%"
                    val mainText = "${health.passes}/${health.total} passed ($pct)" +
                        if (health.failingRunNumbers.isEmpty()) "" else ", failed in runs:"
                    g2.drawString(mainText, x, baseline)
                    x += fm.stringWidth(mainText)

                    // Clickable run number links
                    health.failingRunNumbers.forEachIndexed { i, runNum ->
                        if (x > targetRegion.x + targetRegion.width - H_PAD) return@forEachIndexed
                        val url = health.failingRunUrls.getOrElse(i) { "" }
                        val text = " #$runNum"
                        val w = fm.stringWidth(text)
                        val spaceW = fm.stringWidth(" ")
                        if (url.isNotEmpty()) {
                            runLinks += RunLink(x - targetRegion.x, x - targetRegion.x + w, url)
                        }
                        g2.drawString(text, x, baseline)
                        // underline (skip the leading space)
                        g2.drawLine(x + spaceW, baseline + 1, x + w, baseline + 1)
                        x += w + RUN_GAP
                    }
                }
            }
        } finally {
            g2.dispose()
        }
    }

    fun handleClick(relX: Int) {
        for (link in runLinks) {
            if (relX in link.relX1..link.relX2) {
                BrowserUtil.browse(link.url)
                return
            }
        }
    }

    override fun equals(other: Any?) = other is FlakeGuardInlayRenderer && testTitle == other.testTitle
    override fun hashCode() = testTitle.hashCode()
}
