package com.isaacshub.app.routehelper.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isaacshub.app.routehelper.domain.StopSide

/** One-tap address card used by both the live route builder and testing mode: plain tap routes with no note, Left/Right/In Drive route with the matching canned note. */
@Composable
fun AddressActionCard(label: String, onTap: (StopSide) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onTap(StopSide.NONE) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onTap(StopSide.LEFT) }, modifier = Modifier.weight(1f)) {
                    Text("Left")
                }
                Button(onClick = { onTap(StopSide.RIGHT) }, modifier = Modifier.weight(1f)) {
                    Text("Right")
                }
                Button(onClick = { onTap(StopSide.IN_DRIVE) }, modifier = Modifier.weight(1f)) {
                    Text("In Drive")
                }
            }
        }
    }
}
