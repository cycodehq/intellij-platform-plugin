package com.cycode.plugin

import com.cycode.plugin.editor.FINDING_TAB_NAME
import com.cycode.plugin.editor.FindingFileEditor
import com.cycode.plugin.editor.FindingFileType
import com.cycode.plugin.editor.FindingVirtualFile
import com.cycode.plugin.services.CycodePersistentSettingsService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.swing.JLabel

class FindingEditorTest {
    @Test
    fun openingFindingsInAnEditorTabIsOffByDefault() {
        val settings = CycodePersistentSettingsService()
        assertFalse(settings.openFindingsInEditorTab)
        assertFalse(settings.getSettings().openFindingsInEditorTab)
    }

    @Test
    fun theSettingIsPartOfTheComparedSettings() {
        val settings = CycodePersistentSettingsService()
        val before = settings.getSettings()
        settings.openFindingsInEditorTab = true
        assertTrue(before != settings.getSettings())
    }

    @Test
    fun theFindingFileIsAnInMemoryFileWithItsOwnType() {
        val file = FindingVirtualFile()
        assertEquals(FINDING_TAB_NAME, file.name)
        assertSame(FindingFileType, file.fileType)
        assertTrue(FindingFileType.isReadOnly)
    }

    @Test
    fun showingAnotherCardReplacesTheFirstOne() {
        val editor = FindingFileEditor(FindingVirtualFile())
        val first = JLabel("first")
        val second = JLabel("second")

        editor.showCard(first)
        assertEquals(1, editor.component.componentCount)
        assertSame(first, editor.component.getComponent(0))

        editor.showCard(second)
        assertEquals(1, editor.component.componentCount)
        assertSame(second, editor.component.getComponent(0))
    }

    @Test
    fun theEditorIsNeverModifiedAndAlwaysValid() {
        val editor = FindingFileEditor(FindingVirtualFile())
        assertFalse(editor.isModified)
        assertTrue(editor.isValid)
    }
}
