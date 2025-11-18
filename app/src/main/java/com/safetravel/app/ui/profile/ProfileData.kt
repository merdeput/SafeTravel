package com.safetravel.app.ui.profile

// --- Data Models for Profile, Contacts and Circles ---

data class Contact(
    val id: String,
    val name: String,
    val phone: String
)

data class Circle(
    val id: String,
    val name: String,
    val memberIds: List<String>
)

data class Trip(
    val id: String,
    val destination: String,
    val date: String
)

// --- UI State Models ---

data class ProfileUiState(
    val userName: String = "Jane Doe",
    val trips: List<Trip> = emptyList()
)

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val circles: List<Circle> = emptyList()
)
