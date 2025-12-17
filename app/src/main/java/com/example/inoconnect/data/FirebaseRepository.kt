package com.example.inoconnect.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage // <--- IMPORT ADDED
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance() // <--- FIX IS HERE

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // --- AUTH ---
    suspend fun registerUser(email: String, pass: String, role: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: return false
            val newUser = User(userId = uid, email = email, role = role)
            db.collection("users").document(uid).set(newUser).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loginUser(email: String, pass: String): String? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: return null
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.getString("role")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun logout() {
        auth.signOut()
    }

    // --- ORGANIZER ---
// Updated signature to accept 'tag'
    suspend fun createEvent(
        title: String,
        description: String,
        date: String,
        location: String,
        imageUrl: String,
        tag: String // <--- New Parameter
    ) {
        val uid = currentUserId ?: return
        val newDoc = db.collection("events").document()
        val event = Event(
            eventId = newDoc.id,
            organizerId = uid,
            title = title,
            description = description,
            date = date,
            location = location,
            imageUrl = imageUrl,
            tag = tag // <--- Save it
        )
        newDoc.set(event).await()
    }

    suspend fun getOrganizerEvents(): List<Event> {
        val uid = currentUserId ?: return emptyList()
        val snapshot = db.collection("events").whereEqualTo("organizerId", uid).get().await()
        return snapshot.toObjects(Event::class.java)
    }

    // --- PARTICIPANT ---
    suspend fun getAllEvents(): List<Event> {
        val snapshot = db.collection("events").get().await()
        return snapshot.toObjects(Event::class.java)
    }

    suspend fun getEventById(eventId: String): Event? {
        val snapshot = db.collection("events").document(eventId).get().await()
        return snapshot.toObject(Event::class.java)
    }

    suspend fun joinEvent(eventId: String) {
        val uid = currentUserId ?: return
        db.collection("events").document(eventId)
            .update("participantIds", FieldValue.arrayUnion(uid))
            .await()
    }

    // --- STORAGE (The part that was breaking) ---
    suspend fun uploadEventImage(imageUri: Uri): String? {
        return try {
            val filename = UUID.randomUUID().toString()
            // storage is now defined, so this will work:
            val ref = storage.reference.child("event_images/$filename")

            // Upload the file
            ref.putFile(imageUri).await()

            // Get the download URL
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteEvent(eventId: String) {
        try {
            // 1. Get the event document first to find the image URL
            val snapshot = db.collection("events").document(eventId).get().await()
            val imageUrl = snapshot.getString("imageUrl")

            // 2. If an image exists, delete it from Storage
            if (!imageUrl.isNullOrEmpty()) {
                try {
                    val imageRef = storage.getReferenceFromUrl(imageUrl)
                    imageRef.delete().await()
                } catch (e: Exception) {
                    // If image fails to delete (e.g. not found), we still want to delete the event
                    e.printStackTrace()
                }
            }

            // 3. Delete the Firestore document
            db.collection("events").document(eventId).delete().await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}