package com.cycode.plugin

import com.cycode.plugin.components.toolWindow.components.treeView.toProjectRelativePath
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ProjectRelativePathTest {
    private val root = File("/Users/me/git/NodeGoat")

    @Test
    fun absolutePathInsideTheProjectIsMadeRelative() {
        assertEquals("app/routes/index.js", toProjectRelativePath("/Users/me/git/NodeGoat/app/routes/index.js", root))
    }

    @Test
    fun pathWithoutTheLeadingSlashIsMadeRelative() {
        // seen in IaC detections: "Users/me/git/NodeGoat/Dockerfile"
        assertEquals("Dockerfile", toProjectRelativePath("Users/me/git/NodeGoat/Dockerfile", root))
    }

    @Test
    fun alreadyRelativePathIsKept() {
        assertEquals("src/app.js", toProjectRelativePath("src/app.js", root))
    }

    @Test
    fun relativePathOutsideTheProjectIsKept() {
        assertEquals("Users/other/Dockerfile", toProjectRelativePath("Users/other/Dockerfile", root))
    }

    @Test
    fun absolutePathOutsideTheProjectKeepsTheUpwardsWalk() {
        assertEquals("../Other/a.js", toProjectRelativePath("/Users/me/git/Other/a.js", root))
    }

    @Test
    fun projectWithoutARootShowsThePathAsItIs() {
        assertEquals("/a/b.js", toProjectRelativePath("/a/b.js", File("")))
        assertEquals("a/b.js", toProjectRelativePath("a/b.js", File("")))
    }
}
