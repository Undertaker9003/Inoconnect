package com.example.inoconnect.ui.participant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.inoconnect.data.Event
import com.example.inoconnect.data.FirebaseRepository
import kotlinx.coroutines.launch

@Composable
fun EventDetailScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<Event?>(null) }
    var isJoined by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        event = repository.getEventById(eventId)
        // Check if current user is already in participant list
        val uid = repository.currentUserId
        if (event != null && uid != null) {
            isJoined = event!!.participantIds.contains(uid)
        }
    }

    if (event == null) {
        Box(modifier = Modifier.fillMaxSize()) { Text("Loading...") }
    } else {
        val currentEvent = event!! // force non-null for UI
        Column(modifier = Modifier.padding(16.dp)) {
            Text(currentEvent.title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("Date: ${currentEvent.date}")
            Text("Location: ${currentEvent.location}")
            Spacer(Modifier.height(16.dp))
            Text(currentEvent.description, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        repository.joinEvent(currentEvent.eventId)
                        isJoined = true
                    }
                },
                enabled = !isJoined,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isJoined) "You have joined this event" else "Join Event")
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}