package com.github.clementsehan.cypressradarjetbrains.gutter

import com.github.clementsehan.cypressradarjetbrains.model.TestHealth
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import javax.swing.Icon

class FlakeGuardGutterRenderer(
    val testTitle: String,
    private val health: TestHealth?,
    private val isDynamic: Boolean = false
) : GutterIconRenderer() {

    override fun getIcon(): Icon = when {
        isDynamic -> AllIcons.General.Information
        health == null -> AllIcons.RunConfigurations.TestIgnored
        health.isCurrentlyFailing || health.passPercentage < 75.0 -> AllIcons.RunConfigurations.TestFailed
        health.passPercentage < 95.0 -> AllIcons.General.Warning
        else -> AllIcons.RunConfigurations.TestPassed
    }

    override fun getTooltipText(): String = when {
        isDynamic -> "Flake Guard: dynamic title — data exists in Cypress Cloud but cannot be matched statically"
        health == null -> "Flake Guard: new test (no historical data)"
        else -> buildString {
            append("Pass rate: ${"%.1f".format(health.passPercentage)}%")
            if (health.isCurrentlyFailing) append(" (currently failing on main)")
            append("\nLast run: ${health.lastRunUrl}")
        }
    }

    override fun getClickAction(): AnAction? = health?.let {
        object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                BrowserUtil.browse(it.lastRunUrl)
            }
        }
    }

    override fun isNavigateAction() = health != null
    override fun getAlignment() = Alignment.LEFT
    override fun equals(other: Any?) = other is FlakeGuardGutterRenderer && testTitle == other.testTitle
    override fun hashCode() = testTitle.hashCode()
}
