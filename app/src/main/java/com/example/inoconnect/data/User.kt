package com.example.inoconnect.data

data class User(
    val userId: String = "",
    val email: String = "",
    val role: String = UserRole.PARTICIPANT // Default
)

object UserRole {
    const val ORGANIZER = "ORGANIZER"
    const val PARTICIPANT = "PARTICIPANT"
}