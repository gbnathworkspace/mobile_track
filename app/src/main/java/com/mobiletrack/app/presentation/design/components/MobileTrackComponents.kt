package com.mobiletrack.app.presentation.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobiletrack.app.presentation.design.MTColors
import com.mobiletrack.app.presentation.design.MTElevation
import com.mobiletrack.app.presentation.design.MTIconSize
import com.mobiletrack.app.presentation.design.MTRadius
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.theme.MobileTrackTheme

@Composable
fun MTSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun MTSubsectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
fun MTCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(MTRadius.lg),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = MTElevation.card)
    ) {
        content()
    }
}

@Composable
fun MTPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(MTRadius.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = MTColors.FocusBlue,
            contentColor = Color.White
        )
    ) {
        Text(text)
    }
}

@Composable
fun MTSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(MTRadius.pill)
    )
}

@Composable
fun MTEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MTSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(MTIconSize.hero),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(MTSpacing.md))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(MTSpacing.xs))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MTIconRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(MTSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(MTIconSize.md)
        )
        Spacer(Modifier.width(MTSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(MTSpacing.sm))
            trailing()
        }
    }
}

@Composable
fun MTMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    MTCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(MTSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(MTIconSize.lg))
            Spacer(Modifier.height(MTSpacing.sm))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun MTListValueRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    MTCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MTSpacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(
    name = "Primary Button",
    group = "Buttons",
    showBackground = true,
    backgroundColor = 0xFFF7F8FA,
    widthDp = 360
)
@Composable
private fun MTPrimaryButtonPreview() {
    MobileTrackTheme {
        MTPrimaryButton(
            text = "Continue",
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(MTSpacing.md)
        )
    }
}

@Preview(
    name = "Card + Icon Row",
    group = "Cards",
    showBackground = true,
    backgroundColor = 0xFFF7F8FA,
    widthDp = 360
)
@Composable
private fun MTCardIconRowPreview() {
    MobileTrackTheme {
        MTCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MTSpacing.md)
        ) {
            MTIconRow(
                icon = Icons.Default.Info,
                title = "Usage Access",
                subtitle = "Required to track screen time"
            )
        }
    }
}

@Preview(
    name = "Metric Cards",
    group = "Cards",
    showBackground = true,
    backgroundColor = 0xFFF7F8FA,
    widthDp = 360
)
@Composable
private fun MTMetricCardsPreview() {
    MobileTrackTheme {
        Row(
            modifier = Modifier.padding(MTSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(MTSpacing.sm)
        ) {
            MTMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.PhoneAndroid,
                label = "Screen Time",
                value = "2h 14m"
            )
            MTMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                label = "Limit Left",
                value = "46m"
            )
        }
    }
}

@Preview(
    name = "Search Field",
    group = "Inputs",
    showBackground = true,
    backgroundColor = 0xFFF7F8FA,
    widthDp = 360
)
@Composable
private fun MTSearchFieldPreview() {
    MobileTrackTheme {
        MTSearchField(
            value = "",
            onValueChange = {},
            placeholder = "Search apps",
            modifier = Modifier.padding(MTSpacing.md)
        )
    }
}

@Preview(
    name = "Empty State",
    group = "States",
    showBackground = true,
    backgroundColor = 0xFFF7F8FA,
    widthDp = 360
)
@Composable
private fun MTEmptyStatePreview() {
    MobileTrackTheme {
        MTEmptyState(
            icon = Icons.Default.Timer,
            title = "No limits set",
            subtitle = "Tap + to add a time limit for an app"
        )
    }
}
