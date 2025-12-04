package com.safetravel.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddMembersDialog(
    circle: Circle,
    allContacts: List<Contact>,
    onAddMembers: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val eligibleContacts = remember(allContacts, circle.memberIds) {
        allContacts.filter { it.id !in circle.memberIds }
    }
    
    val selectedMemberIds = remember { mutableStateOf<Set<String>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Members to ${circle.name}") },
        text = {
            if (eligibleContacts.isEmpty()) {
                Text("All your friends are already in this circle.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(eligibleContacts) { contact ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentSelection = selectedMemberIds.value.toMutableSet()
                                    if (contact.id in currentSelection) {
                                        currentSelection.remove(contact.id)
                                    } else {
                                        currentSelection.add(contact.id)
                                    }
                                    selectedMemberIds.value = currentSelection
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(checked = contact.id in selectedMemberIds.value, onCheckedChange = null)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(contact.name)
                                if (contact.username.isNotEmpty()) {
                                    Text("@${contact.username}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { onAddMembers(selectedMemberIds.value.toList()); onDismiss() },
                enabled = selectedMemberIds.value.isNotEmpty()
            ) { 
                Text("Add Selected") 
            } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
