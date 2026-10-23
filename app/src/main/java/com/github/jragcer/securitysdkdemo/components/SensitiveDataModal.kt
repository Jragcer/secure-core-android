package com.github.jragcer.securitysdkdemo.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.jragcer.securitycore.api.screen.SecureScreenEffect

@Composable
fun SensitiveDataModal (onDismiss: () -> Unit,
                        onProtectionChanged: (Boolean) -> Unit){
    SecureScreenEffect(
        enabled = true,
        onProtectionChanged = onProtectionChanged)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sensitive Data") },
        text ={
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This screen uses SecureScreenEffect() directly in Compose. While this dialog is open, screenshots are blocked and app switcher preview is hidden.",
                    style = MaterialTheme.typography.bodySmall
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Confidential Token: SEC-9942-X901",
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close & Restore")
            }
        }
    )
}