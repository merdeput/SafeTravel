package com.safetravel.app.ui.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// These data classes are now shared between the ViewModels
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

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val circles: List<Circle> = emptyList()
)

@HiltViewModel
class ContactsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val dummyContacts = listOf(
            Contact("1", "Mom", "555-0101"),
            Contact("2", "Dad", "555-0102"),
            Contact("3", "Best Friend", "555-0103")
        )
        _uiState.update {
            it.copy(
                contacts = dummyContacts,
                circles = listOf(
                    Circle("c1", "Family", listOf("1", "2")),
                    Circle("c2", "Close Friends", listOf("3"))
                )
            )
        }
    }

    fun addContact(name: String, phone: String) {
        val newContact = Contact(id = (uiState.value.contacts.size + 1).toString(), name = name, phone = phone)
        _uiState.update { it.copy(contacts = it.contacts + newContact) }
    }

    fun createCircle(name: String) {
        val newCircle = Circle(id = (uiState.value.circles.size + 1).toString(), name = name, memberIds = emptyList())
        _uiState.update { it.copy(circles = it.circles + newCircle) }
    }
}
