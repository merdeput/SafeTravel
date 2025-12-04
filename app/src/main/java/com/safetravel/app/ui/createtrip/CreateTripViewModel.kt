package com.safetravel.app.ui.createtrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.CircleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateTripUiState(
    val where: String = "",
    val time: String = "",
    val duration: String = "",
    val tripType: String = "",
    val hasElderly: Boolean = false,
    val hasChildren: Boolean = false,
    val isGenerating: Boolean = false,
    val generatedReport: String? = null,
    val isCreatingCircle: Boolean = false,
    val createdCircleId: Int? = null,
    val error: String? = null
)

@HiltViewModel
class CreateTripViewModel @Inject constructor(
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTripUiState())
    val uiState = _uiState.asStateFlow()

    fun onWhereChange(newWhere: String) {
        _uiState.update { it.copy(where = newWhere) }
    }

    fun onTimeChange(newTime: String) {
        _uiState.update { it.copy(time = newTime) }
    }

    fun onDurationChange(newDuration: String) {
        _uiState.update { it.copy(duration = newDuration) }
    }

    fun onTripTypeChange(newType: String) {
        _uiState.update { it.copy(tripType = newType) }
    }

    fun onHasElderlyChange(newValue: Boolean) {
        _uiState.update { it.copy(hasElderly = newValue) }
    }

    fun onHasChildrenChange(newValue: Boolean) {
        _uiState.update { it.copy(hasChildren = newValue) }
    }

    fun generateSafetyReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            delay(2000) // Simulate API call
            val report = """
                ## Safety Report for ${_uiState.value.where}
                
                **General Advisory**: This location is generally safe, but exercise caution in crowded areas.
                
                **Weather**: Expect sunny weather with a chance of rain in the evening.
                
                **Emergency Contacts**: 
                * Local Police: 113
                * Ambulance: 115
                
                **Tips**:
                * Keep your valuables secure.
                * Stay hydrated.
                ${if (_uiState.value.hasElderly) "* **Accessibility**: Check for wheelchair ramps." else ""}
                ${if (_uiState.value.hasChildren) "* **Family**: Keep children close in busy markets." else ""}
            """.trimIndent()
            
            _uiState.update { it.copy(generatedReport = report, isGenerating = false) }
        }
    }

    fun onStartTripClick() {
        if (_uiState.value.createdCircleId != null) return 

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingCircle = true, error = null) }
            
            val circleName = "Trip to ${_uiState.value.where}"
            val result = circleRepository.createCircle(circleName, "Circle for trip to ${_uiState.value.where}")
            
            if (result.isSuccess) {
                val circle = result.getOrThrow()
                _uiState.update { it.copy(createdCircleId = circle.id, isCreatingCircle = false) }
            } else {
                _uiState.update { it.copy(error = "Failed to create trip circle.", isCreatingCircle = false) }
            }
        }
    }
    
    fun onTripCreationNavigated() {
        _uiState.update { it.copy(createdCircleId = null, error = null) }
    }
}
