package com.safetravel.app.data.model

import com.google.gson.annotations.SerializedName

data class NewsWeatherResponse(
    @SerializedName("provinces") val provinces: List<ProvinceData>? = null
)

data class ProvinceData(
    @SerializedName("province_name") val provinceName: String? = null,
    @SerializedName("report_date") val reportDate: String? = null,
    @SerializedName("weather_forecast") val weatherForecast: List<WeatherForecast>? = null,
    @SerializedName("travel_advice") val travelAdvice: List<TravelAdvice>? = null,
    @SerializedName("executive_summary") val executiveSummary: String? = null,
    @SerializedName("sources") val sources: List<String>? = null,
    @SerializedName("score") val score: Int? = null,
    @SerializedName("recent_news") val recentNews: List<NewsItem>? = null // Keeping it if used elsewhere, but not in new JSON
)

data class WeatherForecast(
    @SerializedName("date") val date: String? = null,
    @SerializedName("temperature") val temperature: String? = null,
    @SerializedName("condition") val condition: String? = null
)

data class TravelAdvice(
    @SerializedName("category") val category: String? = null,
    @SerializedName("advice") val advice: String? = null
)

data class NewsItem(
    @SerializedName("date") val date: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("snippet") val snippet: String? = null
)
