package com.ghhccghk.multiplatform.kugouapi

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KogouAPi Kotlin Multiplatform SDK",
    ) {
        App()
    }
}