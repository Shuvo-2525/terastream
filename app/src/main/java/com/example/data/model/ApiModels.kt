package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResolveRequest(
    @Json(name = "url") val url: String,
    @Json(name = "password") val password: String? = null,
    @Json(name = "token") val token: String? = null
)

@JsonClass(generateAdapter = true)
data class StreamRequest(
    @Json(name = "id") val id: String,
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class FolderItem(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "sizeBytes") val sizeBytes: Long,
    @Json(name = "durationSec") val durationSec: Long? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "isVideo") val isVideo: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ApiResponse(
    @Json(name = "type") val type: String, // "list", "stream", "error"
    @Json(name = "title") val title: String? = null, // Folder title
    @Json(name = "items") val items: List<FolderItem>? = null, // Folder items
    @Json(name = "name") val name: String? = null, // Direct stream file name
    @Json(name = "streamUrl") val streamUrl: String? = null, // Media URL
    @Json(name = "headers") val headers: Map<String, String>? = null, // HTTP headers
    @Json(name = "code") val code: String? = null, // Error code: INVALID_LINK/EXPIRED/PASSWORD_REQUIRED/etc.
    @Json(name = "message") val message: String? = null // Human readable message
)
