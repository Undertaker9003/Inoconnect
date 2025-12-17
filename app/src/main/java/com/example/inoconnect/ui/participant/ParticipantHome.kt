package com.example.inoconnect.ui.participant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.inoconnect.data.Event
import com.example.inoconnect.data.FirebaseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantHome(
    onEventClick: (String) -> Unit,
    onCreateProjectClick: () -> Unit // <--- NEW CALLBACK
) {
    val repository = remember { FirebaseRepository() }

    // Data State
    var allEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Filter & Search State
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Event", "Project"

    LaunchedEffect(Unit) {
        allEvents = repository.getAllEvents()
        isLoading = false
    }

    // Logic to Filter List
    val filteredEvents = allEvents.filter { event ->
        val matchesSearch = event.title.contains(searchQuery, ignoreCase = true) ||
                event.description.contains(searchQuery, ignoreCase = true)
        val matchesTag = if (selectedFilter == "All") true else event.tag == selectedFilter

        matchesSearch && matchesTag
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateProjectClick,
                icon = { Icon(Icons.Default.Add, "Create") },
                text = { Text("New Project") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search events or projects...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Filter Chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedFilter == "All",
                    onClick = { selectedFilter = "All" },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedFilter == "Event",
                    onClick = { selectedFilter = "Event" },
                    label = { Text("Events") }
                )
                FilterChip(
                    selected = selectedFilter == "Project",
                    onClick = { selectedFilter = "Project" },
                    label = { Text("Projects") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. The List
            if (isLoading) {
                CircularProgressIndicator()
            } else if (filteredEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found", style = MaterialTheme.typography.titleLarge)
                }
            } else {
                LazyColumn {
                    items(filteredEvents) { event ->
                        EventItemCard(event, onClick = { onEventClick(event.eventId) })
                    }
                }
            }
        }
    }
}

@Composable
fun EventItemCard(event: Event, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            if (event.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                // Show Tag Badge
                SuggestionChip(
                    onClick = {},
                    label = { Text(event.tag) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if(event.tag == "Event") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                Text(event.title, style = MaterialTheme.typography.titleLarge)
                Text(event.date, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}