package com.example.inoconnect.ui.participant

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
fun CreateProjectScreen(
    onProjectCreated: () -> Unit,
    onBackClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    Scaffold(
        topBar = {
            // You can add a TopAppBar here with a back button if you like
            Text("Create New Project", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Image Picker
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Button(
                onClick = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedImageUri == null) "Upload Project Cover" else "Change Photo")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))

            if (isUploading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isUploading = true
                            var finalImageUrl = ""
                            if (selectedImageUri != null) {
                                finalImageUrl = repository.uploadEventImage(selectedImageUri!!) ?: ""
                            }

                            // *** CRITICAL: Saving with tag = "Project" ***
                            repository.createEvent(title, description, date, location, finalImageUrl, "Project")

                            isUploading = false
                            onProjectCreated()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publish Project")
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}