package com.cycode.plugin.components.toolWindow.components.scanContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.ScanScope
import com.cycode.plugin.components.Component
import com.cycode.plugin.components.common.createClickableLabel
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.git.HEAD_REF
import com.cycode.plugin.git.LOCAL_DIRECTORY_REF
import com.cycode.plugin.git.RefOption
import com.cycode.plugin.services.CycodeService
import com.cycode.plugin.services.pluginLocalState
import com.cycode.plugin.services.scanScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Box
import javax.swing.ButtonGroup
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class ScanContentTab : Component<CycodeService>() {
    private val panel = JPanel(GridBagLayout())
    private val gbc = GridBagConstraints().apply {
        gridy = 0
        insets = JBUI.insetsBottom(10)
        anchor = GridBagConstraints.NORTHWEST
        fill = GridBagConstraints.HORIZONTAL
    }
    private var updatingCombos = false

    private fun addComponentToPanel(component: JComponent) {
        gbc.gridy++
        panel.add(component, gbc)
    }

    override fun getContent(service: CycodeService): JPanel {
        // do not get local state on class level because will be outdated
        val pluginLocalState = pluginLocalState(service.project)

        addComponentToPanel(createClickableLabel(CycodeBundle.message("scanTabTitleLabel")))
        addScopeControls(service)
        addComponentToPanel(createScanButton(service, CliScanType.Secret, "scanTabSecretsBtn") {
            pluginLocalState.isSecretScanningEnabled
        })
        addComponentToPanel(createScanButton(service, CliScanType.Sca, "scanTabScaBtn") {
            pluginLocalState.isScaScanningEnabled
        })
        addComponentToPanel(createScanButton(service, CliScanType.Iac, "scanTabIacBtn") {
            pluginLocalState.isIacScanningEnabled
        })
        addComponentToPanel(createScanButton(service, CliScanType.Sast, "scanTabSastBtn") {
            pluginLocalState.isSastScanningEnabled
        })

        addComponentToPanel(createClickableLabel(CycodeBundle.message("scanTabOnSaveTip")))
        addComponentToPanel(createClickableLabel(CycodeBundle.message("howToUseLabel")))

        return panel
    }

    private fun createScanButton(
        service: CycodeService,
        scanType: CliScanType,
        key: String,
        isModuleEnabled: () -> Boolean
    ): JButton {
        return JButton(CycodeBundle.message(key)).apply {
            isEnabled = isModuleEnabled()
            addActionListener { scanScope(service.project).startScanForCurrentScope(scanType) }
        }
    }

    private fun createNote(text: String, icon: javax.swing.Icon? = null): JBLabel {
        return JBLabel("<html>$text</html>").apply {
            setAllowAutoWrapping(true)
            icon?.let {
                this.icon = it
                iconTextGap = JBUI.scale(6)
                verticalTextPosition = SwingConstants.TOP
            }
        }
    }

    private fun addScopeControls(service: CycodeService) {
        val scopeService = scanScope(service.project)
        val pluginLocalState = pluginLocalState(service.project)
        val effectiveScope = scopeService.getEffectiveScanScope()
        val isGitRepository = scopeService.isGitRepository()
        val scopes = ScanScope.entries.filter { it != ScanScope.Diff || scopeService.isDiffScanSupported() }

        // Diff is disabled when this turns out not to be a git repository; the tab is rebuilt if that changes
        scopeService.refreshGitRepositoryState()

        addComponentToPanel(createClickableLabel(CycodeBundle.message("scanScopeLabel")))

        val group = ButtonGroup()
        val radioPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        scopes.forEach { scope ->
            val radio = JBRadioButton(CycodeBundle.message("scanScope${scope.name}"), scope == effectiveScope).apply {
                // Diff cannot work without a git repository
                isEnabled = scope != ScanScope.Diff || isGitRepository != false
                addActionListener {
                    if (scope != scopeService.getScanScope()) {
                        scopeService.setScanScope(scope)
                        updateToolWindowState(service.project)
                    }
                }
            }
            group.add(radio)
            radioPanel.add(radio)
            radioPanel.add(Box.createHorizontalStrut(JBUI.scale(12)))
        }
        addComponentToPanel(radioPanel)
        addComponentToPanel(createNote(CycodeBundle.message("scanScope${effectiveScope.name}Hint")))
        if (scopes.contains(ScanScope.Diff) && isGitRepository == false) {
            addComponentToPanel(createNote(CycodeBundle.message("scanScopeDiffNoGitNote"), AllIcons.General.Warning))
        }

        if (effectiveScope != ScanScope.Diff) {
            return
        }

        val fromCombo = createRefCombo(service, true)
        val toCombo = createRefCombo(service, false)
        addComponentToPanel(JPanel(GridBagLayout()).apply {
            val c = GridBagConstraints().apply { fill = GridBagConstraints.HORIZONTAL; gridy = 0 }
            add(JBLabel(CycodeBundle.message("scanScopeDiffFromLabel")), c.apply { gridx = 0; weightx = 0.0 })
            add(fromCombo, c.apply { gridx = 1; weightx = 1.0; insets = JBUI.insets(0, 4, 0, 8) })
            add(JBLabel(CycodeBundle.message("scanScopeDiffToLabel")), c.apply { gridx = 2; weightx = 0.0; insets = JBUI.emptyInsets() })
            add(toCombo, c.apply { gridx = 3; weightx = 1.0; insets = JBUI.insetsLeft(4) })
        })
        addComponentToPanel(createNote(CycodeBundle.message("scanScopeDiffIacNote"), AllIcons.General.Warning))

        // lists are reloaded in the background and the combos updated in place (never while a popup is open)
        scopeService.refreshDiffRefs {
            populateRefCombo(fromCombo, true, pluginLocalState.diffFromRef, pluginLocalState.diffRefs)
            populateRefCombo(toCombo, false, pluginLocalState.diffToRef, pluginLocalState.diffRefs)
        }
    }

    private fun createRefCombo(service: CycodeService, isFrom: Boolean): ComboBox<RefOption> {
        val pluginLocalState = pluginLocalState(service.project)
        return ComboBox<RefOption>().apply {
            // long labels are truncated instead of widening the tool window
            setMinimumAndPreferredWidth(JBUI.scale(REF_COMBO_WIDTH))
            maximumRowCount = REF_COMBO_VISIBLE_ROWS
            renderer = SimpleListCellRenderer.create("") { it.label }
            populateRefCombo(
                this,
                isFrom,
                if (isFrom) pluginLocalState.diffFromRef else pluginLocalState.diffToRef,
                pluginLocalState.diffRefs
            )
            addActionListener {
                if (updatingCombos) return@addActionListener
                val selected = selectedItem as? RefOption ?: return@addActionListener
                if (isFrom) {
                    scanScope(service.project).setDiffRefs(selected.value, pluginLocalState.diffToRef)
                } else {
                    scanScope(service.project).setDiffRefs(pluginLocalState.diffFromRef, selected.value)
                }
            }
        }
    }

    private fun populateRefCombo(combo: ComboBox<RefOption>, isFrom: Boolean, selectedValue: String, refs: List<RefOption>) {
        // replacing the model closes an open popup
        if (combo.isPopupVisible) return

        val items = mutableListOf(RefOption(LOCAL_DIRECTORY_REF, CycodeBundle.message("scanScopeDiffLocalDirectory")))
        items.addAll(refs.ifEmpty { listOf(RefOption(HEAD_REF, HEAD_REF)) })

        updatingCombos = true
        try {
            combo.model = DefaultComboBoxModel(items.toTypedArray())
            combo.selectedItem = items.firstOrNull { it.value == selectedValue }
                ?: items.first { it.value == if (isFrom) LOCAL_DIRECTORY_REF else HEAD_REF }
        } finally {
            updatingCombos = false
        }
    }

    companion object {
        private const val REF_COMBO_WIDTH = 110
        private const val REF_COMBO_VISIBLE_ROWS = 12
    }
}
