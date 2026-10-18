package br.com.dojolb.graduacao.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.dojolb.graduacao.ui.theme.DojoSurface
import br.com.dojolb.graduacao.ui.theme.MaterialTheme
import br.com.dojolb.graduacao.ui.theme.RedPrimary
import br.com.dojolb.graduacao.ui.theme.White

@Composable
fun DojoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    DojoSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, onClick = onClick)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun DojoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = RedPrimary,
        contentColor = White,
        disabledContainerColor = RedPrimary.copy(alpha = 0.5f),
        disabledContentColor = White.copy(alpha = 0.5f)
    ),
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.material3.Shapes.medium
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        colors = colors,
        shape = shape
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = colors.contentColor(enabled).value
        )
    }
}

@Composable
fun DojoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = RedPrimary,
    contentColor: Color = RedPrimary
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.5f),
            borderColor = { _, _ -> borderColor }
        ),
        shape = androidx.compose.material3.Shapes.medium
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun DojoTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = RedPrimary
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        "APPROVED" -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "FAILED" -> Pair(ErrorRed, White)
        else -> Pair(WarningOrange, Black)
    }

    androidx.compose.material3.Chip(
        modifier = modifier,
        colors = androidx.compose.material3.ChipDefaults.chipColors(
            containerColor = bgColor,
            contentColor = textColor
        ),
        shape = androidx.compose.material3.Shapes.small
    ) {
        Text(
            text = when (status) {
                "APPROVED" -> "APROVADO"
                "FAILED" -> "REPROVADO"
                else -> "EM ANDAMENTO"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProgressBar(
    progress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    color: Color = RedPrimary,
    trackColor: Color = MediumGray
) {
    Column(modifier = modifier.fillMaxWidth()) {
        androidx.compose.material3.LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = trackColor
        )
    }
}

@Composable
fun GradeButton(
    grade: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isEvaluatedGrade = grade != -1
    val colors = when {
        !enabled -> Pair(MediumGray, TextMuted)
        isSelected -> Pair(RedPrimary, White)
        isEvaluatedGrade -> Pair(MediumGray, TextSecondary)
        else -> Pair(DarkGray, TextPrimary)
    }

    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier
            .weight(1f)
            .height(56.dp)
            .padding(horizontal = 4.dp),
        enabled = enabled,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = colors.first,
            contentColor = colors.second
        ),
        shape = androidx.compose.material3.Shapes.medium
    ) {
        Text(
            text = if (grade == -1) "—" else "$grade%",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LoadingOverlay(message: String = "Carregando…") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        DojoSurface(
            modifier = Modifier
                .padding(32.dp)
                .wrapContentSize(),
            color = DarkGray
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = RedPrimary,
                    modifier = Modifier.size(48.dp)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.res.Painter? = null,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon?.let {
            androidx.compose.foundation.Image(
                painter = it,
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
        }
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = androidx.compose.ui.text.TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        actionText?.let { text ->
            onAction?.let { action ->
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))
                DojoButton(text = text, onClick = action, modifier = Modifier.wrapContentWidth())
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Confirmar",
    dismissText: String = "Cancelar",
    isDestructive: Boolean = false
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = { Text(text = message, fontSize = 16.sp) },
        confirmButton = {
            if (isDestructive) {
                androidx.compose.material3.Button(
                    onClick = { onConfirm(); onDismiss() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = White
                    )
                ) {
                    Text(text = confirmText, fontWeight = FontWeight.Bold)
                }
            } else {
                androidx.compose.material3.Button(
                    onClick = { onConfirm(); onDismiss() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = RedPrimary,
                        contentColor = White
                    )
                ) {
                    Text(text = confirmText, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(text = dismissText, fontWeight = FontWeight.Medium)
            }
        }
    )
}