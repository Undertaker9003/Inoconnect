package com.example.inoconnect.data

data class Event(
    val eventId: String = "",
    val organizerId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val tag: String = "Event", // <--- NEW: Can be "Event" or "Project"
    val participantIds: List<String> = emptyList()
)