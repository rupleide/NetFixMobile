package com.rupleide.netfix.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ripple
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.rupleide.netfix.core.tgproxy.TgProxyController
import com.rupleide.netfix.ui.components.NetFixSwitch
import com.rupleide.netfix.ui.components.NetFixTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun TgProxyTab(
    focusRequester: FocusRequester,
    navBarFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val safeBottomInset = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
    val navOverlayReserve = safeBottomInset + 86.dp

    LaunchedEffect(Unit) {
        repeat(8) { index ->
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            delay(100L + index * 50L)
        }
    }

    var port by remember { mutableStateOf(TgProxyController.getPort(context).toString()) }
    var dcIps by remember { mutableStateOf(TgProxyController.getDcIps(context)) }
    var isDcAuto by remember { mutableStateOf(dcIps.isEmpty()) }
    var poolSize by remember { mutableIntStateOf(TgProxyController.getPoolSize(context)) }
    var secretKey by remember { mutableStateOf(TgProxyController.getOrGenerateSecret(context)) }
    var showSecret by remember { mutableStateOf(false) }
    var cfEnabled by remember { mutableStateOf(TgProxyController.isCfEnabled(context)) }
    var cfPriority by remember { mutableStateOf(TgProxyController.isCfPriority(context)) }

    var latency by remember { mutableIntStateOf(-1) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(port, lifecycleOwner) {
        val currentPort = port.toIntOrNull() ?: TgProxyController.DEFAULT_PORT
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                latency = withContext(Dispatchers.IO) {
                    TgProxyController.measureLatency(TgProxyController.DEFAULT_BIND_IP, currentPort)
                }
                delay(5000)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF161616)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = if (isLandscape) 820.dp else 500.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = navOverlayReserve + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFFF4F4F5),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Подключение",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            NetFixTextField(
                value = port,
                onValueChange = { value ->
                    val filtered = value.filter { it.isDigit() }
                    if (filtered.length <= 5) {
                        port = filtered
                        val newPort = filtered.toIntOrNull() ?: TgProxyController.DEFAULT_PORT
                        TgProxyController.setPort(context, newPort)
                        com.rupleide.netfix.core.debug.AppDebugManager.log("Изменен порт TG прокси: $newPort")
                    }
                },
                label = "Порт подключения",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Автовыбор IP-адресов DC", color = Color(0xFFF4F4F5))
                    Text(
                        text = "Использовать встроенные адреса Telegram",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                NetFixSwitch(
                    checked = isDcAuto,
                    onCheckedChange = { checked ->
                        isDcAuto = checked
                        if (checked) {
                            dcIps = ""
                            TgProxyController.setDcIps(context, "")
                        }
                    }
                )
            }

            if (!isDcAuto) {
                Spacer(modifier = Modifier.height(12.dp))
                NetFixTextField(
                    value = dcIps,
                    onValueChange = { value ->
                        dcIps = value
                        TgProxyController.setDcIps(context, value)
                    },
                    label = "IP-адреса DC (например 2:ip,4:ip)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = Color(0xFFF4F4F5),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Пул WebSocket-соединений",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF161616))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                listOf(2, 4, 6).forEach { size ->
                    val isSelected = poolSize == size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF2C2C35) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                poolSize = size
                                TgProxyController.setPoolSize(context, size)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = size.toString(),
                            color = if (isSelected) Color(0xFFF4F4F5) else Color(0xFFA1A1AA),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF4F4F5),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Безопасность",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            NetFixTextField(
                value = secretKey,
                onValueChange = {},
                readOnly = true,
                label = "Секретный ключ",
                singleLine = true,
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showSecret) "Скрыть" else "Показать",
                            tint = Color(0xFFA1A1AA)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF242426))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) {
                            val currentPort = port.toIntOrNull() ?: TgProxyController.DEFAULT_PORT
                            val url = TgProxyController.getTgProxyUrl(
                                TgProxyController.DEFAULT_BIND_IP,
                                currentPort,
                                secretKey
                            )
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Proxy", url))
                            Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color(0xFFF4F4F5),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Копировать",
                            color = Color(0xFFF4F4F5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF242426))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) {
                            secretKey = TgProxyController.regenerateSecret(context)
                            Toast.makeText(context, "Ключ обновлен", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFFF4F4F5),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Пересоздать",
                            color = Color(0xFFF4F4F5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color(0xFFF4F4F5),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Cloudflare CDN Proxy",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Использовать Cloudflare CDN", color = Color(0xFFF4F4F5))
                    Text(
                        text = "Туннелирование трафика через Cloudflare",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                NetFixSwitch(
                    checked = cfEnabled,
                    onCheckedChange = { checked ->
                        cfEnabled = checked
                        TgProxyController.setCfEnabled(context, checked)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Приоритет Cloudflare", color = Color(0xFFF4F4F5))
                    Text(
                        text = "Приоритетное использование CDN адресов",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                NetFixSwitch(
                    checked = cfPriority,
                    onCheckedChange = { checked ->
                        cfPriority = checked
                        TgProxyController.setCfPriority(context, checked)
                    },
                    modifier = Modifier.focusProperties { down = navBarFocusRequester }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = Color(0xFFF4F4F5),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Статус соединения",
                    color = Color(0xFFF4F4F5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Задержка", color = Color(0xFFA1A1AA))
                Text(
                    text = if (latency >= 0) "$latency мс" else "недоступен",
                    color = if (latency >= 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
}
