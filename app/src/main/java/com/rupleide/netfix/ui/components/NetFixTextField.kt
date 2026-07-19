package com.rupleide.netfix.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetFixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusTransition = updateTransition(targetState = isFocused, label = "NetFixTextFieldFocus")

    val borderAlpha by focusTransition.animateFloat(
        transitionSpec = {
            spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)
        },
        label = "BorderAlpha"
    ) { focused ->
        if (focused) 0.3f else 0.1f
    }

    val borderColor = Color.White.copy(alpha = borderAlpha)

    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = borderColor,
        unfocusedBorderColor = borderColor,
        focusedLabelColor = Color(0xFFA1A1AA),
        unfocusedLabelColor = Color(0xFFA1A1AA),
        focusedTextColor = Color(0xFFF4F4F5),
        unfocusedTextColor = Color(0xFFF4F4F5),
        cursorColor = borderColor,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    val customTextSelectionColors = remember {
        TextSelectionColors(
            handleColor = Color(0x4DFFFFFF),
            backgroundColor = Color(0x1AFFFFFF)
        )
    }

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            readOnly = readOnly,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFF4F4F5), fontSize = 14.sp),
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(borderColor),
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    label = label?.let { labelText ->
                        {
                            val isLabelFloating = isFocused || value.isNotEmpty()
                            Text(
                                text = labelText,
                                modifier = Modifier
                                    .background(if (isLabelFloating) Color(0xFF1E1E1E) else Color.Transparent)
                                    .padding(horizontal = if (isLabelFloating) 4.dp else 0.dp)
                            )
                        }
                    },
                    placeholder = placeholder?.let { { Text(it, color = Color(0xFFA1A1AA), fontSize = 13.sp) } },
                    trailingIcon = trailingIcon,
                    colors = colors,
                    container = {
                        OutlinedTextFieldDefaults.ContainerBox(
                            enabled = true,
                            isError = false,
                            interactionSource = interactionSource,
                            colors = colors,
                            shape = RoundedCornerShape(12.dp),
                            focusedBorderThickness = 1.dp,
                            unfocusedBorderThickness = 1.dp
                        )
                    }
                )
            }
        )
    }
}
