package com.example.inoconnect.ui.organizer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import kotlinx.coroutines.launch

@Composable
fun CreateEventScreen(
    onEventCreated: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // Image State
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // The Image Picker
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Make form scrollable
    ) {
        Text("Create New Event", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        // Image Selection Button
        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentScale = ContentScale.Crop
            )
        }
        Button(
            onClick = {
                imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedImageUri == null) "Upload Event Photo" else "Change Photo")
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))

        if (isUploading) {
            CircularProgressIndicator()
            Text("Uploading image...")
        } else {
            Button(
                onClick = {
                    scope.launch {
                        isUploading = true

                        // 1. Upload Image first (if exists)
                        var finalImageUrl = ""
                        if (selectedImageUri != null) {
                            finalImageUrl = repository.uploadEventImage(selectedImageUri!!) ?: ""
                        }

                        // 2. Create Event with the link
                        repository.createEvent(title, description, date, location, finalImageUrl ,"Event")

                        isUploading = false
                        onEventCreated()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Publish Event")
            }
        }
    }
}