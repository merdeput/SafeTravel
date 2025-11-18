package com.safetravel.app.ui.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// The data classes (Contact, Circle, ContactsUiState) are now correctly defined in ProfileData.kt

@HiltViewModel
class ContactsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val dummyContacts = listOf(
            Contact("1", "Mom", "555-0101"),
            Contact("2", "Dad", "555-0102"),
            Contact("3", "Best Friend", "555-0103"),
            Contact("4", "Sibling", "555-0104")
        )
        _uiState.value = _uiState.value.copy(
            contacts = dummyContacts,
            circles = listOf(
                Circle("c1", "Family", listOf("1", "2")),
                Circle("c2", "Close Friends", listOf("3"))
            )
        )
    }

    fun addContact(name: String, phone: String) {
        val newContact = Contact(id = (System.currentTimeMillis().toString()), name = name, phone = phone)
        _uiState.update { it.copy(contacts = it.contacts + newContact) }
    }

    fun deleteContact(contactId: String) {
        _uiState.update { state ->
            val updatedContacts = state.contacts.filterNot { it.id == contactId }
            val updatedCircles = state.circles.map {
                it.copy(memberIds = it.memberIds.filterNot { id -> id == contactId })
            }
            state.copy(contacts = updatedContacts, circles = updatedCircles)
        }
    }

    fun createCircle(name: String) {
        val newCircle = Circle(id = (System.currentTimeMillis().toString()), name = name, memberIds = emptyList())
        _uiState.update { it.copy(circles = it.circles + newCircle) }
    }

    fun addMembersToCircle(circleId: String, memberIds: List<String>) {
        _uiState.update { state ->
            val updatedCircles = state.circles.map {
                if (it.id == circleId) {
                    val newMembers = (it.memberIds + memberIds).distinct()
                    it.copy(memberIds = newMembers)
                } else {
                    it
                }
            }
            state.copy(circles = updatedCircles)
        }
    }
}
