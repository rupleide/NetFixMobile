package com.rupleide.netfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupleide.netfix.core.whatsnew.WhatsNew

private const val NEW_BADGE_INLINE_ID = "nf_new_badge"

private val NewBadgeText = TextStyle(
    fontSize = 10.sp,
    lineHeight = 10.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.8.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
)

@Composable
fun currentVersionName(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}

@Composable
fun isNewFeature(id: String?): Boolean = id != null && WhatsNew.isNew(id, currentVersionName())

@Composable
fun NewBadge(id: String, modifier: Modifier = Modifier, openWhatsNew: Boolean = true) {
    if (!isNewFeature(id)) return
    val shape = RoundedCornerShape(6.dp)
    val tappable = openWhatsNew && !LocalIsTv.current
    Box(
        modifier = modifier
            .height(18.dp)
            .clip(shape)
            .background(Nf.Surface)
            .border(1.dp, Nf.Line, shape)
            .then(
                if (tappable) {
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        com.rupleide.netfix.data.whatsNewRequestGlobal = id
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("NEW", color = Nf.TextSub, style = NewBadgeText)
    }
}

fun titleWithNewBadge(title: String, isNew: Boolean): AnnotatedString = buildAnnotatedString {
    append(title)
    if (isNew) {
        append(" ")
        appendInlineContent(NEW_BADGE_INLINE_ID, "NEW")
    }
}

fun newBadgeInlineContent(id: String, openWhatsNew: Boolean = true): Map<String, InlineTextContent> = mapOf(
    NEW_BADGE_INLINE_ID to InlineTextContent(
        Placeholder(width = 40.sp, height = 18.sp, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            NewBadge(id, openWhatsNew = openWhatsNew)
        }
    }
)
