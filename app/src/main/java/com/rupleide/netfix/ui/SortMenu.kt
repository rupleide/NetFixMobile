package com.rupleide.netfix.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.ui.components.NetFixHaptic
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.nfTvFocus
import com.rupleide.netfix.ui.components.rememberNetFixHaptics

internal data class SortOption(val key: String, val label: String, val short: String)

internal fun parseSortKeys(raw: String?, allowed: Collection<String>): List<String> =
    raw.orEmpty().split(',').map { it.trim() }.filter { it in allowed }.distinct().take(2)

private val CenteredChipText = TextStyle(
    fontSize = 13.sp,
    lineHeight = 13.sp,
    fontWeight = FontWeight.SemiBold,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
)

@Composable
internal fun SortMenu(
    options: List<SortOption>,
    selectedKeys: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberNetFixHaptics()
    var expanded by remember { mutableStateOf(false) }
    val primary = options.firstOrNull { it.key == selectedKeys.getOrNull(0) }
    val secondary = options.firstOrNull { it.key == selectedKeys.getOrNull(1) && it.key != primary?.key }
    val summary = when {
        primary == null -> "По умолчанию"
        secondary == null -> primary.short
        else -> "${primary.short}, потом ${secondary.short}"
    }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "sortChevron")
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF1F1F1F))
            .border(1.dp, Nf.Line, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .nfTvFocus(corner = 12.dp, inside = true)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    haptics(NetFixHaptic.Tap)
                    expanded = !expanded
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = Nf.TextSub, modifier = Modifier.size(18.dp))
            Text("Сортировка", color = Nf.TextSub, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                summary,
                color = Nf.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Nf.TextSub,
                modifier = Modifier.size(18.dp).rotate(chevronRotation)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(240)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SortGroupLabel("Сначала по")
                ChipWrap {
                    SortChoiceChip("Умолчанию", selected = primary == null) {
                        haptics(NetFixHaptic.Tap)
                        onChange(emptyList())
                    }
                    options.forEach { option ->
                        SortChoiceChip(option.label, selected = primary?.key == option.key) {
                            haptics(NetFixHaptic.Tap)
                            onChange(listOfNotNull(option.key, secondary?.key?.takeIf { it != option.key }))
                        }
                    }
                }
                AnimatedVisibility(
                    visible = primary != null,
                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SortGroupLabel("Если поровну, потом по")
                        ChipWrap {
                            SortChoiceChip("Не важно", selected = secondary == null) {
                                haptics(NetFixHaptic.Tap)
                                onChange(listOfNotNull(primary?.key))
                            }
                            options.filter { it.key != primary?.key }.forEach { option ->
                                SortChoiceChip(option.label, selected = secondary?.key == option.key) {
                                    haptics(NetFixHaptic.Tap)
                                    onChange(listOfNotNull(primary?.key, option.key))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipWrap(content: @Composable () -> Unit) {
    val gap = 6.dp
    Layout(content = content) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val positions = ArrayList<Pair<Int, Int>>(placeables.size)
        var x = 0
        var y = 0
        var rowHeight = 0
        placeables.forEach { placeable ->
            if (x > 0 && x + placeable.width > constraints.maxWidth) {
                x = 0
                y += rowHeight + gapPx
                rowHeight = 0
            }
            positions.add(x to y)
            x += placeable.width + gapPx
            rowHeight = maxOf(rowHeight, placeable.height)
        }
        layout(constraints.maxWidth, y + rowHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(positions[index].first, positions[index].second)
            }
        }
    }
}

@Composable
private fun SortGroupLabel(text: String) {
    Text(
        text,
        color = Nf.TextSub,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
    )
}

@Composable
private fun SortChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .height(32.dp)
            .nfTvFocus(corner = 16.dp, inside = true)
            .clip(shape)
            .background(if (selected) Nf.Green.copy(alpha = 0.14f) else Color(0xFF262626))
            .border(1.dp, if (selected) Nf.Green.copy(alpha = 0.55f) else Nf.Line, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Nf.GreenText else Nf.TextSub, style = CenteredChipText, maxLines = 1)
    }
}
