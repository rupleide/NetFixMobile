package com.rupleide.netfix.ui

import com.rupleide.netfix.ui.components.nfTvFocus
import com.rupleide.netfix.ui.components.LocalIsTv
import com.rupleide.netfix.ui.components.requestFocusSafe
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import android.content.pm.PackageInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.rupleide.netfix.ui.components.Nf
import com.rupleide.netfix.ui.components.NfButton
import com.rupleide.netfix.ui.components.NfButtonStyle
import com.rupleide.netfix.ui.components.NfCheckbox
import com.rupleide.netfix.ui.components.NfCornerAura
import com.rupleide.netfix.ui.components.NfInputField
import com.rupleide.netfix.ui.components.nfEnter
import com.rupleide.netfix.ui.components.NfSwitch
import com.rupleide.netfix.ui.components.nfFloat
import com.rupleide.netfix.ui.components.safeBarsWithoutKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppEntry(val packageName: String, val label: String)

private val TilePalette = listOf(
    Color(0x2EEF4444) to Color(0xFFFCA5A5),
    Color(0x387C6AF7) to Color(0xFFC4B5FD),
    Color(0x383B82F6) to Color(0xFF93C5FD),
    Color(0x33EC4899) to Color(0xFFF9A8D4),
    Color(0x2922C55E) to Color(0xFF86EFAC),
    Color(0x1AFFFFFF) to Color(0xFFF4F4F5)
)

@Composable
fun AppBypassScreen(
    selectedOnly: Boolean,
    onSelectedOnlyChange: (Boolean) -> Unit,
    installedApps: List<PackageInfo>,
    loaded: Boolean,
    failed: Boolean,
    selectedApps: Set<String>,
    onToggle: (String) -> Unit,
    onPickPopular: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeBarsWithoutKeyboard.getBottom(density).toDp() }
    val bottomReserve = safeBottomInset + com.rupleide.netfix.ui.components.navBarReserve()

    BackHandler(onBack = onBack)

    val entries by produceState<List<AppEntry>?>(initialValue = null, installedApps) {
        val initiallySelected = selectedApps
        value = withContext(Dispatchers.Default) {
            val pm = context.packageManager
            installedApps
                .filter { it.packageName != context.packageName }
                .map { pkg ->
                    val label = pkg.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg.packageName
                    AppEntry(pkg.packageName, label)
                }
                .sortedWith(
                    compareByDescending<AppEntry> { it.packageName in initiallySelected }
                        .thenBy { it.label.lowercase() }
                )
        }
    }

    val installedSet = remember(installedApps) { installedApps.map { it.packageName }.toSet() }
    val savedSelectedCount = remember {
        context.getSharedPreferences(context.packageName + "_preferences", android.content.Context.MODE_PRIVATE)
            .getInt("selected_apps_installed_count", -1)
    }
    val selectedCount = when {
        loaded -> selectedApps.count { it in installedSet }
        savedSelectedCount >= 0 -> savedSelectedCount
        else -> selectedApps.size
    }
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }
    val tv = LocalIsTv.current
    val entryFocus = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(tv) {
        if (tv) {
            kotlinx.coroutines.delay(250)
            entryFocus.requestFocusSafe()
        }
    }

    ProvideTextStyle(TextStyle.Default) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Nf.Bg),
            contentAlignment = Alignment.TopCenter
        ) {
            NfCornerAura(Nf.Bright.copy(alpha = 0.056f))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .imePadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = bottomReserve + 16.dp
                )
            ) {
                item(key = "back") {
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .focusRequester(entryFocus)
                            .nfTvFocus(corner = 12.dp, grow = 1.03f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onBack)
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = Nf.TextSub, modifier = Modifier.size(24.dp))
                        Text("Настройки", color = Nf.TextSub, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                item(key = "title") {
                    Text(
                        text = "Обход приложений",
                        color = Nf.Text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        lineHeight = 29.sp,
                        modifier = Modifier
                            .nfEnter(0)
                            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)
                    )
                }
                item(key = "mode") {
                    Box(modifier = Modifier.nfEnter(1)) {
                        ModeCard(selectedOnly, onSelectedOnlyChange)
                    }
                }
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Список приложений", color = Nf.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        when {
                            failed -> Unit
                            !loaded || entries == null -> Text("Загружаем…", color = Nf.Bright, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            selectedOnly -> Text("Выбрано $selectedCount", color = Nf.TextSub, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                if (selectedOnly && loaded && !failed && entries != null) {
                    item(key = "search") {
                        NfInputField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "Поиск приложения",
                            modifier = Modifier.padding(bottom = 12.dp),
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .nfTvFocus(corner = 10.dp, grow = 1.03f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = query.isNotEmpty()) { query = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (query.isEmpty()) Icons.Default.Search else Icons.Default.Close,
                                        contentDescription = if (query.isEmpty()) null else "Очистить",
                                        tint = Nf.TextSub,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                val needle = query.trim()
                val list = entries?.let { all ->
                    if (needle.isEmpty()) all else all.filter {
                        it.label.contains(needle, ignoreCase = true) || it.packageName.contains(needle, ignoreCase = true)
                    }
                }
                when {
                    failed -> item(key = "failed") {
                        StateCard(
                            icon = Icons.Default.Warning,
                            accent = Nf.Red,
                            title = "Не удалось загрузить список",
                            subtitle = "Обход работает как раньше, список можно открыть позже.",
                            actionText = "Повторить",
                            actionStyle = NfButtonStyle.Secondary,
                            onAction = onRetry
                        )
                    }
                    !selectedOnly -> item(key = "notNeeded") {
                        StateCard(
                            icon = Icons.Default.Search,
                            accent = null,
                            title = "Список не нужен",
                            subtitle = "Обход включён для всех приложений. Включите переключатель выше, чтобы выбрать только нужные.",
                            actionText = null,
                            actionStyle = NfButtonStyle.Secondary,
                            onAction = {}
                        )
                    }
                    !loaded || list == null -> itemsIndexed(List(6) { it }, key = { _, i -> "sk$i" }) { index, _ ->
                        SkeletonRow(index == 0, index == 5, index)
                    }
                    else -> {
                        if (list.isEmpty() && needle.isNotEmpty()) {
                            item(key = "noResults") {
                                StateCard(
                                    icon = Icons.Default.Search,
                                    accent = null,
                                    title = "Ничего не найдено",
                                    subtitle = "Попробуйте другое название или пакет приложения.",
                                    actionText = null,
                                    actionStyle = NfButtonStyle.Secondary,
                                    onAction = {}
                                )
                            }
                        }
                        if (selectedCount == 0 && needle.isEmpty()) {
                            item(key = "empty") {
                                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                    StateCard(
                                        icon = Icons.Default.Search,
                                        accent = null,
                                        title = "Ничего не выбрано",
                                        subtitle = "Отметьте приложения, которые нужно ускорить.",
                                        actionText = "Выбрать популярные",
                                        actionStyle = NfButtonStyle.Secondary,
                                        onAction = onPickPopular
                                    )
                                }
                            }
                        }
                        itemsIndexed(list, key = { _, entry -> entry.packageName }) { index, entry ->
                            AppRow(
                                entry = entry,
                                checked = entry.packageName in selectedApps,
                                first = index == 0,
                                last = index == list.lastIndex,
                                onToggle = { onToggle(entry.packageName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(selectedOnly: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .then(if (LocalIsTv.current) Modifier.nfTvFocus(corner = 24.dp, inside = true) else Modifier)
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, Nf.Line, shape)
            .then(if (LocalIsTv.current) Modifier.clickable { onChange(!selectedOnly) } else Modifier)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Ускорять только нужные приложения",
                color = Nf.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp
            )
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selectedOnly) Nf.Green.copy(alpha = 0.14f) else Color(0x12FFFFFF))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedOnly) "Рекомендуется" else "Все приложения",
                    color = if (selectedOnly) Nf.GreenText else Nf.TextSub,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Text(
                text = if (selectedOnly) "Банки и игры не затрагиваются." else "Обход применяется ко всем приложениям.",
                color = Nf.TextSub,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        NfSwitch(checked = selectedOnly, onCheckedChange = onChange)
    }
}

@Composable
private fun StateCard(
    icon: ImageVector,
    accent: Color?,
    title: String,
    subtitle: String,
    actionText: String?,
    actionStyle: NfButtonStyle,
    onAction: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nf.Glass)
            .border(1.dp, if (accent != null) accent.copy(alpha = 0.30f) else Nf.Line, shape)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(accent?.copy(alpha = 0.12f) ?: Nf.Surface)
                .border(1.dp, accent?.copy(alpha = 0.45f) ?: Color(0x1FFFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent ?: Nf.TextSub, modifier = Modifier.size(28.dp).nfFloat())
        }
        Text(title, color = Nf.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
        Text(
            subtitle,
            color = Nf.TextSub,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        if (actionText != null) {
            NfButton(
                text = actionText,
                style = actionStyle,
                compact = true,
                modifier = Modifier.padding(top = 6.dp).width(240.dp),
                onClick = onAction
            )
        }
    }
}

private fun rowShape(first: Boolean, last: Boolean): androidx.compose.ui.graphics.Shape = when {
    first && last -> RoundedCornerShape(24.dp)
    first -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    last -> RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> androidx.compose.ui.graphics.RectangleShape
}

@Composable
private fun Modifier.listRowFrame(first: Boolean, last: Boolean): Modifier {
    val shape = rowShape(first, last)
    return this
        .fillMaxWidth()
        .clip(shape)
        .background(Nf.Glass)
        .then(
            if (first || last) {
                Modifier.border(1.dp, Nf.Line, shape)
            } else {
                Modifier.drawBehind {
                    val stroke = 1.dp.toPx()
                    drawLine(Nf.Line, Offset(stroke / 2f, 0f), Offset(stroke / 2f, size.height), stroke)
                    drawLine(Nf.Line, Offset(size.width - stroke / 2f, 0f), Offset(size.width - stroke / 2f, size.height), stroke)
                    drawLine(Nf.LineSoft, Offset(0f, 0f), Offset(size.width, 0f), stroke)
                }
            }
        )
}

@Composable
private fun AppRow(entry: AppEntry, checked: Boolean, first: Boolean, last: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .listRowFrame(first, last)
            .nfTvFocus(corner = 0.dp, grow = 1f)
            .clickable(onClick = onToggle)
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppTile(entry)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = entry.label,
                color = if (checked) Nf.Text else Nf.TextSub,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = entry.packageName,
                color = Nf.TextOff,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        NfCheckbox(checked = checked)
    }
}

@Composable
private fun AppTile(entry: AppEntry) {
    val context = LocalContext.current
    val bitmap = remember(entry.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(entry.packageName).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
        )
    } else {
        val (background, foreground) = TilePalette[(entry.packageName.hashCode() and Int.MAX_VALUE) % TilePalette.size]
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.label.firstOrNull()?.uppercase() ?: "?",
                color = foreground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun SkeletonRow(first: Boolean, last: Boolean, index: Int) {
    val widths = listOf(140, 110, 160, 90, 130, 100)
    val block = Color(0x12FFFFFF)
    Row(
        modifier = Modifier
            .listRowFrame(first, last)
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(block))
        Box(modifier = Modifier.width(widths[index % widths.size].dp).height(14.dp).clip(RoundedCornerShape(8.dp)).background(block))
        Box(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(block))
    }
}
