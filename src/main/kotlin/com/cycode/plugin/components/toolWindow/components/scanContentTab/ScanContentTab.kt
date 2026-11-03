package com.cycode.plugin.components.toolWindow.components.scanContentTab

import com.cycode.plugin.CycodeBundle
import com.cycode.plugin.cli.CliScanType
import com.cycode.plugin.cli.ScanScope
import com.cycode.plugin.components.Component
import com.cycode.plugin.components.common.createClickableLabel
import com.cycode.plugin.components.common.createWrappedHtmlPane
import com.cycode.plugin.components.toolWindow.updateToolWindowState
import com.cycode.plugin.git.HEAD_REF
import com.cycode.plugin.git.LOCAL_DIRECTORY_REF
import com.cycode.plugin.git.RefOption
import com.cycode.plugin.services.CycodeService
import com.cycode.plugin.services.pluginLocalState
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ScanContentTab : Component<CycodeService>() {
    private var updatingCombos = false

    /** The panel returned by [getContent]; lets the owner check that this tab is the one on screen. */
    var panel: JPanel? = null
        private set

    private class ScanButton(val button: JButton, val scanType: CliScanType, val isModuleEnabled: () -> Boolean)

    private val scanButtons = mutableListOf<ScanButton>()
    private var scanAllButton: JButton? = null
    private var builtScope: ScanScope? = null
    private var builtDiffSupported: Boolean? = null
    private var builtCollapsed: Boolean? = null

    /**
     * Disabling the focused button makes Swing move focus to the next button (it shows up highlighted).
     * Park the focus on the panel first so nothing else gets selected.
     */
    private fun setEnabledKeepingFocus(button: JButton, enabled: Boolean) {
        if (button.isEnabled == enabled) return
        if (!enabled && button.isFocusOwner) {
            panel?.requestFocusInWindow()
        }
        button.isEnabled = enabled
    }

    /**
     * Updates the scan buttons in place (enabled state) instead of rebuilding the whole panel, which flickered
     * every time a scan started or finished. Returns false when something structural changed and the owner
     * must rebuild the tab.
     */
    fun refresh(service: CycodeService): Boolean {
        val local = pluginLocalState(service.project)
        if (builtScope != service.getEffectiveScanScope() ||
            builtDiffSupported != service.isDiffScanSupported() ||
            builtCollapsed != local.scanControlsCollapsed
        ) {
            return false
        }

        scanAllButton?.let {
            val enabled = !local.hasRunningScans()
            setEnabledKeepingFocus(it, enabled)
        }
        scanButtons.forEach {
            val enabled = it.isModuleEnabled() && !local.isScanRunning(it.scanType)
            setEnabledKeepingFocus(it.button, enabled)
        }
        return true
    }

    private fun column(vararg components: JComponent): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            gridy = 0
            weightx = 1.0
            insets = JBUI.insetsBottom(8)
            anchor = GridBagConstraints.NORTHWEST
            fill = GridBagConstraints.HORIZONTAL
        }
        components.forEach {
            panel.add(it, gbc)
            gbc.gridy++
        }
        // filler row absorbs spare height so every row stays pinned to the top
        panel.add(JPanel(), GridBagConstraints().apply {
            gridy = gbc.gridy
            weighty = 1.0
            fill = GridBagConstraints.VERTICAL
        })
        return panel
    }

    override fun getContent(service: CycodeService): JPanel {
        // do not get local state on class level because will be outdated
        val pluginLocalState = pluginLocalState(service.project)

        // docked on the bottom the tab is wide and short: put scope and scan buttons side by side;
        // docked on the left/right it is narrow and tall: stack everything
        val dockAnchor = ToolWindowManager.getInstance(service.project)
            .getToolWindow(CycodeBundle.message("toolWindowId"))?.anchor
        val horizontal = dockAnchor?.isHorizontal ?: false

        scanButtons.clear()
        scanAllButton = null
        builtScope = service.getEffectiveScanScope()
        builtDiffSupported = service.isDiffScanSupported()
        builtCollapsed = pluginLocalState.scanControlsCollapsed

        val scopeComponents = mutableListOf<JComponent>()
        addScopeControls(service, horizontal, scopeComponents)

        val buttons = listOf(
            createScanButton(service, CliScanType.Secret, "scanTabSecretsBtn") { pluginLocalState.isSecretScanningEnabled },
            createScanButton(service, CliScanType.Sca, "scanTabScaBtn") { pluginLocalState.isScaScanningEnabled },
            createScanButton(service, CliScanType.Sast, "scanTabSastBtn") { pluginLocalState.isSastScanningEnabled },
            createScanButton(service, CliScanType.Iac, "scanTabIacBtn") { pluginLocalState.isIacScanningEnabled },
        )
        val footer = arrayOf<JComponent>(
            createWrappedHtmlPane(CycodeBundle.message("scanTabOnSaveTip")),
            createWrappedHtmlPane(CycodeBundle.message("howToUseLabel")),
        )

        // "Ready to scan." with a scan-all button at the end of the line
        val scanAll = JButton(CycodeBundle.message("scanTabScanAllBtn")).apply {
            isEnabled = !pluginLocalState.hasRunningScans()
            toolTipText = CycodeBundle.message("toolbarRunAllAction")
            addActionListener { service.startAllScansForCurrentScope() }
        }
        scanAllButton = scanAll
        val titleLabel = createClickableLabel(CycodeBundle.message("scanTabTitleLabel"))
        val title = JPanel(BorderLayout()).apply {
            add(titleLabel, BorderLayout.CENTER)
            add(scanAll, BorderLayout.EAST)
        }
        val root = ScrollableContentPanel()
        root.border = JBUI.Borders.empty(4, 12, 0, 12)

        if (horizontal) {
            val columns = JPanel(GridBagLayout())
            val gbc = GridBagConstraints().apply {
                weightx = 1.0
                weighty = 1.0
                anchor = GridBagConstraints.NORTHWEST
                fill = GridBagConstraints.HORIZONTAL
                insets = JBUI.insetsRight(24)
            }
            // the title row lives in the same grid so "Scan all" lines up with the right edge of the scan buttons
            columns.add(titleLabel, GridBagConstraints().apply {
                gridx = 0; gridy = 0
                anchor = GridBagConstraints.WEST
                insets = JBUI.insetsBottom(8)
            })
            columns.add(scanAll, GridBagConstraints().apply {
                gridx = 1; gridy = 0
                anchor = GridBagConstraints.EAST
                insets = JBUI.insets(0, 0, 8, 24)
            })
            gbc.gridy = 1
            columns.add(column(*scopeComponents.toTypedArray()), gbc)
            gbc.gridx = 1
            columns.add(column(*buttons.toTypedArray()), gbc)
            root.add(columns, BorderLayout.CENTER)
            root.add(column(*footer), BorderLayout.SOUTH)
        } else {
            root.add(
                column(title, *scopeComponents.toTypedArray(), *buttons.toTypedArray(), *footer),
                BorderLayout.CENTER
            )
        }

        // the scan options are collapsible so the results can use the whole window
        val collapsed = pluginLocalState.scanControlsCollapsed
        val outer = JPanel(BorderLayout())
        outer.add(createHeader(service, collapsed), BorderLayout.NORTH)
        if (!collapsed) {
            outer.add(ScrollPaneFactory.createScrollPane(root, true).apply {
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            }, BorderLayout.CENTER)
        }
        outer.isFocusable = true
        panel = outer
        return outer
    }

    private fun createHeader(service: CycodeService, collapsed: Boolean): JComponent {
        val label = JBLabel(
            // the chosen scope stays visible while the section is collapsed
            if (collapsed) CycodeBundle.message("scanTabHeaderCollapsed",
                CycodeBundle.message("scanScope${service.getEffectiveScanScope().name}"))
            else CycodeBundle.message("scanTabHeader"),
            if (collapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown,
            SwingConstants.LEFT
        ).apply {
            iconTextGap = JBUI.scale(4)
            font = font.deriveFont(Font.BOLD)
            toolTipText = CycodeBundle.message(if (collapsed) "scanTabExpand" else "scanTabCollapse")
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    pluginLocalState(service.project).scanControlsCollapsed = !collapsed
                    updateToolWindowState(service.project, forceRebuild = true, reloadResults = false)
                }
            })
        }
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 12)
            add(label, BorderLayout.CENTER)
        }
    }

    /** Tracks the viewport width (so wrapped text re-flows) and scrolls vertically only when needed. */
    private class ScrollableContentPanel : JPanel(BorderLayout()), Scrollable {
        override fun getPreferredScrollableViewportSize() = preferredSize
        override fun getScrollableUnitIncrement(r: Rectangle, o: Int, d: Int) = JBUI.scale(16)
        override fun getScrollableBlockIncrement(r: Rectangle, o: Int, d: Int) = JBUI.scale(64)
        override fun getScrollableTracksViewportWidth() = true
        // never stretch to the viewport height: the wrapped text makes the preferred height depend on the
        // width, and tracking it made the layout flip-flop (the "shake") when a combo was clicked
        override fun getScrollableTracksViewportHeight() = false
    }

    // always visible and wrapped to the available width (no tooltips)
    private fun createWrappedNote(text: String, icon: Icon?): JBLabel {
        return JBLabel("<html>$text</html>").apply {
            setAllowAutoWrapping(true)
            icon?.let {
                this.icon = it
                iconTextGap = JBUI.scale(6)
                verticalTextPosition = SwingConstants.TOP
            }
        }
    }

    private fun createScanButton(
        service: CycodeService,
        scanType: CliScanType,
        key: String,
        isModuleEnabled: () -> Boolean
    ): JButton {
        val running = pluginLocalState(service.project).isScanRunning(scanType)
        return JButton(CycodeBundle.message(key)).apply {
            isEnabled = isModuleEnabled() && !running
            addActionListener { service.startScanForCurrentScope(scanType) }
            scanButtons.add(ScanButton(this, scanType, isModuleEnabled))
        }
    }

    private fun addScopeControls(service: CycodeService, horizontal: Boolean, out: MutableList<JComponent>) {
        val pluginLocalState = pluginLocalState(service.project)
        val effectiveScope = service.getEffectiveScanScope()
        val scopes = ScanScope.entries.filter { it != ScanScope.Diff || service.isDiffScanSupported() }

        out.add(createClickableLabel(CycodeBundle.message("scanScopeLabel")))

        // radio buttons instead of a dropdown: every option is visible and one click selects it
        val group = ButtonGroup()
        val radios = scopes.map { scope ->
            JBRadioButton(CycodeBundle.message("scanScope${scope.name}"), scope == effectiveScope).apply {
                group.add(this)
                addActionListener {
                    if (scope != service.getScanScope()) {
                        service.setScanScope(scope)
                        updateToolWindowState(service.project, forceRebuild = true, reloadResults = false)
                    }
                }
            }
        }
        val radioPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        radios.forEach { radioPanel.add(it); radioPanel.add(Box.createHorizontalStrut(JBUI.scale(12))) }
        out.add(radioPanel)
        out.add(createWrappedNote(CycodeBundle.message("scanScope${effectiveScope.name}Hint"), null))

        if (effectiveScope != ScanScope.Diff) {
            return
        }

        val fromCombo = createRefCombo(service, true)
        val toCombo = createRefCombo(service, false)
        // From and To share one row; the combos share the width and shrink (long labels are truncated)
        out.add(JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply { fill = GridBagConstraints.HORIZONTAL; gridy = 0 }
            add(JBLabel(CycodeBundle.message("scanScopeDiffFromLabel")), gbc.apply { gridx = 0; weightx = 0.0 })
            add(fromCombo, gbc.apply { gridx = 1; weightx = 1.0; insets = JBUI.insets(0, 4, 0, 8) })
            add(JBLabel(CycodeBundle.message("scanScopeDiffToLabel")), gbc.apply { gridx = 2; weightx = 0.0; insets = JBUI.emptyInsets() })
            add(toCombo, gbc.apply { gridx = 3; weightx = 1.0; insets = JBUI.insetsLeft(4) })
            // pin the row height (with room for the focus ring) so focusing or opening a combo
            // cannot change the size of the row and shift the layout around it
            val height = maxOf(fromCombo.preferredSize.height, toCombo.preferredSize.height) + JBUI.scale(8)
            preferredSize = Dimension(preferredSize.width, height)
            minimumSize = Dimension(0, height)
            maximumSize = Dimension(Int.MAX_VALUE, height)
        })
        out.add(createWrappedNote(CycodeBundle.message("scanScopeDiffIacNote"), AllIcons.General.Warning))

        // lists are reloaded in the background and the combos updated in place (never while a popup is open)
        service.refreshDiffRefs {
            populateRefCombo(fromCombo, true, pluginLocalState.diffFromRef, pluginLocalState.diffRefs)
            populateRefCombo(toCombo, false, pluginLocalState.diffToRef, pluginLocalState.diffRefs)
        }
    }

    private fun createRefCombo(service: CycodeService, isFrom: Boolean): ComboBox<RefOption> {
        val pluginLocalState = pluginLocalState(service.project)
        return ComboBox<RefOption>().apply {
            // the closed combo has a fixed small width (long labels are truncated); the popup list is sized
            // by its widest item in a single pass, which avoids the popup resizing itself after it opens
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
                    service.setDiffRefs(selected.value, pluginLocalState.diffToRef)
                } else {
                    service.setDiffRefs(pluginLocalState.diffFromRef, selected.value)
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
