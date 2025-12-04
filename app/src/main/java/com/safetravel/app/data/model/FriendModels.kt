package com.safetravel.app.data.model

import com.google.gson.annotations.SerializedName

data class FriendRequestRequest(
    @SerializedName("receiver_username") val receiverUsername: String
)

data class FriendRequest(
    @SerializedName("id") val id: Int,
    
    // Try to catch the User object if it exists under "sender" or "from_user"
    @SerializedName("sender", alternate = ["from_user", "user"]) val sender: User?,
    
    // Also capture the ID if the server only sends "sender_id"
    @SerializedName("sender_id") val senderId: Int?,

    @SerializedName("receiver", alternate = ["to_user"]) val receiver: User?,
    @SerializedName("receiver_id") val receiverId: Int?,
    
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class Friendship(
    @SerializedName("id") val id: Int,
    @SerializedName("user1") val user1: User?,
    @SerializedName("user2") val user2: User?,
    @SerializedName("created_at") val createdAt: String?
)
