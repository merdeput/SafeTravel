package com.safetravel.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.NewsWeatherResponse
import com.safetravel.app.data.model.SPECIAL_END_DATE
import com.safetravel.app.data.model.TripDTO
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.NewsWeatherRepository
import com.safetravel.app.data.repository.TripRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val currentTrip: TripDTO? = null,
    val userName: String = "",
    val error: String? = null,
    val newsWeather: NewsWeatherResponse? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tripRepository: TripRepository,
    private val newsWeatherRepository: NewsWeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser
        _uiState.update { it.copy(userName = user?.fullName ?: user?.username ?: "Traveler") }
        checkActiveTrip()
    }

    fun checkActiveTrip() {
        val userId = authRepository.currentUser?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = tripRepository.getTripsByUser(userId)
            if (result.isSuccess) {
                val trips = result.getOrThrow()
                val activeTrip = trips.find { isTripActive(it) }
                _uiState.update { it.copy(isLoading = false, currentTrip = activeTrip) }
                
                // Fetch News & Weather if active trip exists
                if (activeTrip != null) {
                    fetchNewsAndWeather(activeTrip.destination)
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    private fun fetchNewsAndWeather(location: String) {
        // Use Mock Data as requested
        val mockJson = """
        {
          "provinces": [
            {
              "province_name": "Lâm Đồng",
              "weather_forecast": [
                {
                  "date": "14/12/2025",
                  "temperature": "17-21°C",
                  "condition": "Ngày nhiều mây, đêm nhiều mây. Khả năng mưa 10%."
                },
                {
                  "date": "15/12/2025",
                  "temperature": "16-23°C",
                  "condition": "Ngày trời nhiều mây một phần, đêm quang đãng với mây định kỳ. Khả năng mưa 10%."
                },
                {
                  "date": "16/12/2025",
                  "temperature": "17-24°C",
                  "condition": "Ngày nhiều mây, đêm nhiều mây. Khả năng mưa 10%."
                }
              ],
              "recent_news": [
                {
                  "date": "11/12/2025",
                  "title": "Thông xe đèo D'ran, các cửa ngõ vào Đà Lạt cơ bản thông suốt.",
                  "snippet": "Ngày 11/12/2025, đèo D'ran trên Quốc lộ 20 (đoạn qua phường Xuân Trường - Đà Lạt) đã chính thức thông xe trở lại sau hơn 40 ngày tạm đóng do sạt lở nghiêm trọng. Hiện tại, chỉ xe tải dưới 5 tấn và xe khách dưới 16 chỗ được phép lưu thông, các loại xe lớn hơn cần chọn tuyến thay thế. Các đèo khác như Prenn và Mimosa cũng đã được khắc phục sau sạt lở vào đầu tháng 12."
                },
                {
                  "date": "13/12/2025",
                  "title": "Khởi tố hai cán bộ Sở Y tế Lâm Đồng nhận hối lộ.",
                  "snippet": "Ngày 13/12/2025, Công an tỉnh Lâm Đồng đã khởi tố, bắt tạm giam hai cán bộ Sở Y tế tỉnh để điều tra hành vi nhận hối lộ liên quan đến chuỗi 6 phòng khám nha khoa 'chui' hoạt động trái phép."
                },
                {
                  "date": "13/12/2025",
                  "title": "Lâm Đồng đẩy mạnh hợp tác văn hóa, thể thao, du lịch và giáo dục kỹ năng sống.",
                  "snippet": "Trong ngày 13/12/2025, Trường Phổ thông Hermann Gmeiner Đà Lạt đã trao tặng máy tính cho Trường Tiểu học và THCS Đắk Plao. Đồng thời, Sở Văn hóa, Thể thao và Du lịch Thanh Hóa và Lâm Đồng đã ký kết hợp tác giai đoạn 2026-2030. Trung tâm Hoạt động Thanh thiếu nhi tỉnh Lâm Đồng cũng tổ chức chương trình giáo dục kỹ năng sống cho 1.200 học sinh, và Hải đoàn 32 tổ chức cuộc thi 'Em yêu biển, đảo quê hương'."
                }
              ],
              "executive_summary": "Tỉnh Lâm Đồng trong 3 ngày tới (14-16/12/2025) sẽ có thời tiết mát mẻ và khá dễ chịu, với nhiệt độ dao động từ 16°C đến 24°C và khả năng mưa thấp (khoảng 10%). Về tin tức, đèo D'ran, một cửa ngõ quan trọng vào Đà Lạt, đã được thông xe trở lại vào ngày 11/12 sau hơn 40 ngày bị sạt lở, giúp giao thông ổn định hơn. Tuy nhiên, cần lưu ý hạn chế lưu thông vào ban đêm do sửa chữa và sương mù. Trong ngày 13/12, hai cán bộ Sở Y tế Lâm Đồng bị khởi tố vì nhận hối lộ liên quan đến các phòng khám nha khoa trái phép. Ngoài ra, tỉnh cũng đang tích cực thúc đẩy các hoạt động giáo dục và hợp tác văn hóa, thể thao, du lịch với các tỉnh khác.",
              "sources": [
                "https://www.google.com/search?q=weather+in+%C4%90%C3%A0+L%E1%BA%A1t,+VN",
                "https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQFBJhKW3kRSuOHpmtH3PsmouX3t7CQutf0Zl-9CGvSW4eitnwp475yxJktmqSL05t9bKOUmBAm-GXgZlvIUxtymzhSlPhfWicS28-gtnATO9fFoRkCqtl8gyo-3o0n_pWNPA_PLzg==",
                "https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQHlhhuMKrqtDdZKrrypYRUXwu2rJPqx7vd7mb5fbLfsijceEScUsuu4yRp1sKGe1n5pr74aMIdZkdE37_8LoKJUeodtkwa1_Zs9aKASPd_IpWz9QGDxzlRFS653l03BMnN5XRw=",
                "https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQGmslTo1LDkKYeizpswLh8lf6TvatDQUYCVJzobQCiL5MD-76HBjMLhvNm-aFhf_eJCl7O7hQi_p0b8-Kh57jxsds45XzaogYyshMMWTYzGGpSUoqz-skH-2hU7FFgxYyxihcq_XJdyWgZmil-1UmTC",
                "https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQFrFw7VP0Ox0iaNmyumA3vb-X8hL8gTt_kPfyn07rcCfKDZr2AuxDGagFnZxhwjkrNBiQv3P-4gH3JhywSToBObDNqdvmrRA-kn-ZJjJR3tgkmy-45ndqICJ8GJ-l6bQSQ=",
                "https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQGJQelEU1XBTIahgq7JqJR7WgYibPm-DxfJRBUWTd5u15RJi_1l4HZr3y3M3wWwJ_luTrpmGsZNTjYgkbo_8p2g6gKwrzjCF-3pN19MvSKxtVwlYnhxj2CuyR9aR04swPi3vOgT4YzAV0u45ozdlDUXjqGzM1KR771LXYuYEHl6EE1knnMUznws",
                "https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQGAhhcY9yZxjxQCQ12OSN4fhxnAIgq9-UmLVdzASunD9_ijFHkNO51ot6ZBKdRdzvtA6jthH1aiMZY9PTvOX-73SZUp_co9ni2vZypjDtRdNmXvhXkMTKm-CIY1i4NDUWnfi1BJjEBjJkhwxfsBRSwt3JrAXUyooPB-DA9JAsfa7bAekvZmVMIvGUyYKzMYhkyPfB_FDHFSMZA="
              ],
              "score": 85
            }
          ]
        }
        """.trimIndent()

        try {
            val response = Gson().fromJson(mockJson, NewsWeatherResponse::class.java)
            _uiState.update { it.copy(newsWeather = response) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Error parsing mock data: ${e.message}") }
        }
    }

    private fun isTripActive(trip: TripDTO): Boolean {
        val endStr = trip.endDate
        if (endStr == null || endStr == SPECIAL_END_DATE) return true
        return try {
             val end = LocalDateTime.parse(endStr, DateTimeFormatter.ISO_DATE_TIME)
             LocalDateTime.now().isBefore(end)
        } catch (e: Exception) {
            try {
                 val end = LocalDateTime.parse(endStr)
                 LocalDateTime.now().isBefore(end)
            } catch (e2: Exception) {
                false
            }
        }
    }
}
