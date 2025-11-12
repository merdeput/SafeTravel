
package com.safetravel.app.ui.createtrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val hasElderly: Boolean = false,
    val hasChildren: Boolean = false,
    val tripType: String = "Sightseeing",
    val isGenerating: Boolean = false,
    val generatedReport: String? = null
)

@HiltViewModel
class CreateTripViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTripUiState())
    val uiState = _uiState.asStateFlow()

    fun onWhereChange(where: String) = _uiState.update { it.copy(where = where) }
    fun onTimeChange(time: String) = _uiState.update { it.copy(time = time) }
    fun onDurationChange(duration: String) = _uiState.update { it.copy(duration = duration) }
    fun onHasElderlyChange(has: Boolean) = _uiState.update { it.copy(hasElderly = has) }
    fun onHasChildrenChange(has: Boolean) = _uiState.update { it.copy(hasChildren = has) }
    fun onTripTypeChange(type: String) = _uiState.update { it.copy(tripType = type) }

    fun generateSafetyReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, generatedReport = null) }
            delay(2500) // Simulate AI generation
            val currentState = _uiState.value
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    generatedReport = """
### Safety Report for your ${currentState.tripType} trip to ${currentState.where}

**Trip Details:**
- **Time:** ${currentState.time}
- **Duration:** ${currentState.duration}
- **Group:** ${if(currentState.hasElderly || currentState.hasChildren) "Includes vulnerable members (elderly/children)" else "Adults only"}

---

#### **Weather & Disaster Outlook**
- **Forecast:** Expect partly cloudy skies with a slight chance of intermittent rain. Temperatures will be moderate.
- **Alerts:** There are currently no active weather advisories or disaster warnings for this region. However, always stay aware of local news channels.

---

#### **Crime & Safety Analysis**
- **Crime Rate:** The selected area has a **low to moderate** crime rate. Petty crimes like pickpocketing in crowded tourist areas are the most common concern.
- **High-Risk Zones:** Avoid walking alone late at night near the old city harbor. Keep valuables out of sight in markets.

---

#### **Customized Safety Tips**
- **General:** Share your itinerary with a trusted contact. Keep digital and physical copies of your important documents.
- **For Children:** Establish a clear meeting point in case you get separated in crowded places. Consider using a child locator or wristband.
- **For Elderly:** Ensure all accommodations are easily accessible. Keep a list of local emergency services and the address of your lodging handy.
- **${currentState.tripType}-Specific:** For adventure activities, always use certified guides and check that safety equipment is up to standard.

*Disclaimer: This is an AI-generated report. Information is for planning purposes only. Always consult official sources and use your best judgment.*
""".trimIndent()
                )
            }
        }
    }
}
