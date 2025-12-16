package com.safetravel.app.data.api

import com.safetravel.app.data.model.AddCircleMemberRequest
import com.safetravel.app.data.model.CircleMemberResponse
import com.safetravel.app.data.model.CircleResponse
import com.safetravel.app.data.model.CreateCircleRequest
import com.safetravel.app.data.model.CreateNotificationRequest
import com.safetravel.app.data.model.FriendRequest
import com.safetravel.app.data.model.FriendRequestRequest
import com.safetravel.app.data.model.Friendship
import com.safetravel.app.data.model.GetIncidentsResponseDTO
import com.safetravel.app.data.model.IncidentCreateDTO
import com.safetravel.app.data.model.IncidentDTO
import com.safetravel.app.data.model.LocationData
import com.safetravel.app.data.model.LoginResponse
import com.safetravel.app.data.model.NewsWeatherResponse
import com.safetravel.app.data.model.NotificationResponse
import com.safetravel.app.data.model.RegisterRequest
import com.safetravel.app.data.model.SendSosRequest
import com.safetravel.app.data.model.SosAlertResponse
import com.safetravel.app.data.model.TripBase
import com.safetravel.app.data.model.TripDTO
import com.safetravel.app.data.model.UpdateCircleRequest
import com.safetravel.app.data.model.UpdateNotificationRequest
import com.safetravel.app.data.model.UpdateSosStatusRequest
import com.safetravel.app.data.model.User
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("location")
    suspend fun sendLocation(@Body locationData: LocationData): Response<Map<String, Any>>

    // --- AUTH ENDPOINTS ---

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<User>

    @FormUrlEncoded
    @POST("api/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    // Add this for Logout!
    @POST("api/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>> // Server returns a JSON message
    
    @GET("api/users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<User>

    // --- FRIEND ENDPOINTS ---

    @POST("api/friend-requests")
    suspend fun sendFriendRequest(
        @Header("Authorization") token: String,
        @Body request: FriendRequestRequest
    ): Response<FriendRequest>

    @GET("api/friend-requests/pending")
    suspend fun getPendingFriendRequests(
        @Header("Authorization") token: String
    ): Response<List<FriendRequest>>

    @POST("api/friend-requests/{request_id}/accept")
    suspend fun acceptFriendRequest(
        @Header("Authorization") token: String,
        @Path("request_id") requestId: Int
    ): Response<Friendship>

    @POST("api/friend-requests/{request_id}/reject")
    suspend fun rejectFriendRequest(
        @Header("Authorization") token: String,
        @Path("request_id") requestId: Int
    ): Response<FriendRequest> // Returns the rejected request details

    @GET("api/friends")
    suspend fun getFriends(
        @Header("Authorization") token: String
    ): Response<List<User>>

    @DELETE("api/friends/{friend_id}")
    suspend fun deleteFriend(
        @Header("Authorization") token: String,
        @Path("friend_id") friendId: Int
    ): Response<Unit>

    // New endpoint to get user details by ID
    @GET("api/users/{user_id}")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): Response<User>

    // --- CIRCLE ENDPOINTS ---

    @POST("api/circles")
    suspend fun createCircle(
        @Header("Authorization") token: String,
        @Body request: CreateCircleRequest
    ): Response<CircleResponse>

    @GET("api/circles")
    suspend fun getCircles(
        @Header("Authorization") token: String
    ): Response<List<CircleResponse>>

    @GET("api/circles/{circle_id}")
    suspend fun getCircle(
        @Header("Authorization") token: String,
        @Path("circle_id") circleId: Int
    ): Response<CircleResponse>

    @PUT("api/circles/{circle_id}")
    suspend fun updateCircle(
        @Header("Authorization") token: String,
        @Path("circle_id") circleId: Int,
        @Body request: UpdateCircleRequest
    ): Response<CircleResponse>

    @DELETE("api/circles/{circle_id}")
    suspend fun deleteCircle(
        @Header("Authorization") token: String,
        @Path("circle_id") circleId: Int
    ): Response<Unit>

    // --- CIRCLE MEMBERS ENDPOINTS ---

    // Changed to match backend: POST /circles/{circle_id}/members
    @POST("api/circles/{circle_id}/members")
    suspend fun addCircleMember(
        @Header("Authorization") token: String,
        @Path("circle_id") circleId: Int,
        @Body request: AddCircleMemberRequest
    ): Response<CircleMemberResponse>

    // Changed to match backend: GET /circles/{circle_id}/members -> returns List<User>
    @GET("api/circles/{circle_id}/members")
    suspend fun getCircleMembers(
        @Header("Authorization") token: String,
        @Path("circle_id") circleId: Int
    ): Response<List<User>>

    // Changed to match backend: DELETE /circles/{circle_id}/members/{user_id}
    @DELETE("api/circles/{circle_id}/members/{user_id}")
    suspend fun removeCircleMember(
        @Header("Authorization") token: String,
        @Path("circle_id") circleId: Int,
        @Path("user_id") userId: Int
    ): Response<Unit>

    // --- SOS ENDPOINTS ---

    @POST("api/sos")
    suspend fun sendSos(
        @Header("Authorization") token: String,
        @Body request: SendSosRequest
    ): Response<SosAlertResponse>

    @POST("api/sos/{alert_id}/status")
    suspend fun updateSosStatus(
        @Header("Authorization") token: String,
        @Path("alert_id") alertId: Int,
        @Body request: UpdateSosStatusRequest
    ): Response<SosAlertResponse>

    @GET("api/sos/my_alerts")
    suspend fun getMySosAlerts(
        @Header("Authorization") token: String
    ): Response<List<SosAlertResponse>>

    // --- NOTIFICATION ENDPOINTS ---

    @POST("api/notifications")
    suspend fun createNotification(
        @Header("Authorization") token: String,
        @Body request: CreateNotificationRequest
    ): Response<NotificationResponse>

    @GET("api/notifications/{notification_id}")
    suspend fun getNotification(
        @Header("Authorization") token: String,
        @Path("notification_id") notificationId: Int
    ): Response<NotificationResponse>

    @GET("api/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<List<NotificationResponse>>

    @PUT("api/notifications/{notification_id}")
    suspend fun updateNotification(
        @Header("Authorization") token: String,
        @Path("notification_id") notificationId: Int,
        @Body request: UpdateNotificationRequest
    ): Response<NotificationResponse>

    @DELETE("api/notifications/{notification_id}")
    suspend fun deleteNotification(
        @Header("Authorization") token: String,
        @Path("notification_id") notificationId: Int
    ): Response<Unit>

    // --- TRIP ENDPOINTS ---
    
    // Note: Python router provided earlier had "/trips/", but based on other endpoints they likely need "/api/" prefix.
    // If the python router was mounted with "/api" prefix, then it's "/api/trips/...".
    // I'll assume "/api/" prefix for consistency with other endpoints like "/api/circles".
    // Wait, the user provided python code: `@router.get("/trips/{trip_id}", ...)`
    // And `app.include_router(trip_router, prefix="/api", tags=["Trips"])` is usually how it's done.
    // So "api/trips" is likely correct.

    @GET("api/trips/{trip_id}")
    suspend fun getTripById(
        @Header("Authorization") token: String,
        @Path("trip_id") tripId: Int
    ): Response<TripDTO>

    @GET("api/users/{user_id}/trips")
    suspend fun getTripsByUser(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): Response<List<TripDTO>>

    @POST("api/trips/")
    suspend fun createTrip(
        @Header("Authorization") token: String,
        @Body tripData: TripBase
    ): Response<TripDTO>

    @DELETE("api/trips/{trip_id}")
    suspend fun deleteTrip(
        @Header("Authorization") token: String,
        @Path("trip_id") tripId: Int
    ): Response<Unit>

    @PUT("api/trips/{trip_id}")
    suspend fun updateTrip(
        @Header("Authorization") token: String,
        @Path("trip_id") tripId: Int,
        @Body tripData: TripBase
    ): Response<TripDTO>

    @GET("api/{location}")
    suspend fun getNewsAndWeather(
        @Header("Authorization") token: String,
        @Path("location") location: String
    ): Response<NewsWeatherResponse>

    // --- INCIDENT ENDPOINTS ---

    @GET("api/incidents")
    suspend fun getIncidents(
        @Header("Authorization") token: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double
    ): Response<GetIncidentsResponseDTO>

    @POST("api/incidents")
    suspend fun createIncident(
        @Header("Authorization") token: String,
        @Body request: IncidentCreateDTO
    ): Response<IncidentDTO>

    @DELETE("api/incidents/{incident_id}")
    suspend fun deleteIncident(
        @Header("Authorization") token: String,
        @Path("incident_id") incidentId: Long
    ): Response<Unit>
}
