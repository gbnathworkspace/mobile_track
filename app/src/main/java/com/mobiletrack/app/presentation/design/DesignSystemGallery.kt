package com.mobiletrack.app.presentation.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mobiletrack.app.presentation.design.components.MTCard
import com.mobiletrack.app.presentation.design.components.MTEmptyState
import com.mobiletrack.app.presentation.design.components.MTIconRow
import com.mobiletrack.app.presentation.design.components.MTListValueRow
import com.mobiletrack.app.presentation.design.components.MTMetricCard
import com.mobiletrack.app.presentation.design.components.MTPrimaryButton
import com.mobiletrack.app.presentation.design.components.MTSearchField
import com.mobiletrack.app.presentation.design.components.MTSectionTitle
import com.mobiletrack.app.presentation.design.components.MTSubsectionLabel
import com.mobiletrack.app.presentation.theme.MobileTrackTheme

@Preview(
    name = "Design System Gallery",
    group = "Design System",
    showBackground = true,
    backgroundColor = 0xFFF7F8FA,
    widthDp = 411,
    heightDp = 891
)
@Composable
fun DesignSystemGalleryPreview() {
    MobileTrackTheme {
        Column(
            modifier = Modifier
                .background(MTColors.Surface)
                .padding(MTSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MTSpacing.md)
        ) {
            MTSectionTitle("Buttons")
            MTPrimaryButton(text = "Primary Action", onClick = {}, modifier = Modifier.fillMaxWidth())

            MTSectionTitle("Cards")
            MTCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MTSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(MTSpacing.xs)
                ) {
                    Text("Focused card", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Use MTCard for standard raised surfaces across screens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            MTSectionTitle("Metrics")
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    icon = Icons.Default.Visibility,
                    label = "Unlocks",
                    value = "37",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            MTSectionTitle("Search")
            MTSearchField(value = "", onValueChange = {}, placeholder = "Search apps")

            MTSectionTitle("Rows")
            MTListValueRow(title = "Instagram", value = "42m")
            MTCard(modifier = Modifier.fillMaxWidth()) {
                MTIconRow(
                    icon = Icons.Default.Info,
                    title = "Usage Access",
                    subtitle = "Required to track screen time",
                    trailing = {
                        androidx.compose.material3.Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            MTSubsectionLabel("Empty State")
            MTEmptyState(
                icon = Icons.Default.Timer,
                title = "No limits set",
                subtitle = "Tap + to add a time limit for an app"
            )
        }
    }
}

@Preview(
    name = "Dark Theme Gallery",
    group = "Design System",
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 411
)
@Composable
fun DesignSystemGalleryDarkPreview() {
    MobileTrackTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(MTSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MTSpacing.md)
        ) {
            MTSectionTitle("Dark Components")
            MTPrimaryButton(text = "Continue", onClick = {}, modifier = Modifier.fillMaxWidth())
            MTSearchField(value = "", onValueChange = {}, placeholder = "Search")
            MTMetricCard(
                icon = Icons.Default.Search,
                label = "Searches",
                value = "12",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
