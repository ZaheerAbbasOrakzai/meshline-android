package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeshGreenPrimary
import com.example.ui.theme.MeshMintAccent

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreateGroup: (groupName: String, memberCount: Int) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var memberCount by remember { mutableStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.GroupAdd,
                contentDescription = null,
                tint = MeshMintAccent
            )
        },
        title = {
            Text(
                text = "New Off-Grid Mesh Group",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Create an encrypted broadcast channel relayed across nearby mesh nodes.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group / Channel Name") },
                    placeholder = { Text("e.g. Rescue Ops Alpha") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MeshMintAccent,
                        focusedLabelColor = MeshMintAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Initial Relay Nodes: $memberCount Nodes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = memberCount.toFloat(),
                    onValueChange = { memberCount = it.toInt() },
                    valueRange = 2f..25f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = MeshMintAccent,
                        activeTrackColor = MeshGreenPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreateGroup(groupName, memberCount)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MeshMintAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_create_group_button")
            ) {
                Text("Create Channel", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
