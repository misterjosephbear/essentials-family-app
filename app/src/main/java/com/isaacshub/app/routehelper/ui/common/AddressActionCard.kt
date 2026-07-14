package com.isaacshub.app.routehelper.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isaacshub.app.routehelper.domain.StopSide

/**
 * One-tap address row used by both the live route builder and testing mode: plain tap routes with
 * no note, L/R/D route with the matching canned note. All four targets sit in a single compact row
 * so five of these fit on screen without scrolling while driving.
 */
@Composable
fun AddressActionCard(label: String, onTap: (StopSide) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onTap(StopSide.NONE) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SideButton(text = "L", onClick = { onTap(StopSide.LEFT) })
            SideButton(text = "R", onClick = { onTap(StopSide.RIGHT) })
            SideButton(text = "D", onClick = { onTap(StopSide.IN_DRIVE) })
        }
    }
}

/** L(eft) / R(ight) / (In) D(rive) - single letters so the fixed three targets stay tappable at a glance without wrapping. */
@Composable
private fun SideButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.width(44.dp).padding(start = 4.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
