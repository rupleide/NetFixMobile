package com.rupleide.netfix.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

val WindowInsets.Companion.safeBarsWithoutKeyboard: WindowInsets
    @Composable get() = WindowInsets.systemBars.union(WindowInsets.displayCutout)
