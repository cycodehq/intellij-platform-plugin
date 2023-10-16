package com.cycode.plugin.components

import javax.swing.JPanel

abstract class Component<T> {
    abstract fun getContent(service: T): JPanel
}