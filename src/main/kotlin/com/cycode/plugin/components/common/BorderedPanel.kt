package com.cycode.plugin.components.common

import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

class BorderedPanel : JPanel() {
    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(10, 20, 10, 20)
    }
}
