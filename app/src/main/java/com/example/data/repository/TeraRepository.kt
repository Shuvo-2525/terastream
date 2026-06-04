package com.example.data.repository

import com.example.data.api.BASE_URL
import com.example.data.api.TeraApiService
import com.example.data.database.RecentLinkDao
import com.example.data.model.ApiResponse
import com.example.data.model.FolderItem
import com.example.data.model.RecentLink
import com.example.data.model.ResolveRequest
import com.example.data.model.StreamRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class TeraRepository(private val recentLinkDao: RecentLinkDao) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(if (BASE_URL.endsWith("/")) BASE_URL else "$BASE_URL/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val apiService = retrofit.create(TeraApiService::class.java)

    // Database Actions
    val recentLinks: Flow<List<RecentLink>> = recentLinkDao.getAllRecentLinks()

    suspend fun saveRecentLink(url: String, title: String, thumbnail: String?) = withContext(Dispatchers.IO) {
        recentLinkDao.insertRecentLink(RecentLink(url = url, title = title, thumbnail = thumbnail))
    }

    suspend fun deleteRecentLink(url: String) = withContext(Dispatchers.IO) {
        recentLinkDao.deleteRecentLink(url)
    }

    suspend fun clearAllRecentLinks() = withContext(Dispatchers.IO) {
        recentLinkDao.clearAllRecentLinks()
    }

    // API Resolve
    suspend fun resolveUrl(url: String, password: String? = null, token: String? = null): ApiResponse = withContext(Dispatchers.IO) {
        // If it's a placeholder base URL or we want testing in-emulator, provide high-polish mock fallbacks
        if (BASE_URL.contains("placeholder") || url.contains("mock", ignoreCase = true) || url.contains("test", ignoreCase = true)) {
            return@withContext getMockResolveResponse(url, password)
        }

        try {
            val response = apiService.resolveLink(ResolveRequest(url = url, password = password, token = token))
            if (response.type != "error") {
                saveRecentLink(
                    url = url,
                    title = response.title ?: response.name ?: "Resolved Stream",
                    thumbnail = if (response.type == "stream") null else response.items?.firstOrNull()?.thumbnail
                )
            }
            response
        } catch (e: Exception) {
            // Log or fallback on failure
            ApiResponse(
                type = "error",
                code = "RESOLVE_FAILED",
                message = e.localizedMessage ?: "Failed to reach resolver server. (Timeout or bad gateway)"
            )
        }
    }

    // API Stream URL for a selected folder file
    suspend fun getStreamForFile(id: String, url: String): ApiResponse = withContext(Dispatchers.IO) {
        if (BASE_URL.contains("placeholder") || url.contains("mock", ignoreCase = true) || url.contains("test", ignoreCase = true)) {
            return@withContext getMockStreamResponse(id, url)
        }

        try {
            apiService.getStreamUrl(StreamRequest(id = id, url = url))
        } catch (e: Exception) {
            ApiResponse(
                type = "error",
                code = "RESOLVE_FAILED",
                message = e.localizedMessage ?: "Failed to get streaming media link."
            )
        }
    }

    // Mock response helper for out-of-the-box functional demo
    private fun getMockResolveResponse(url: String, password: String?): ApiResponse {
        val cleanUrl = url.lowercase().trim()

        // 1. Password case
        if (cleanUrl.contains("password") || cleanUrl.contains("locked") || cleanUrl.contains("pass")) {
            if (password == null || password != "1234") {
                return ApiResponse(
                    type = "error",
                    code = "PASSWORD_REQUIRED",
                    message = "This link is password protected. Enter '1234' to unlock."
                )
            }
        }

        // 2. Error cases
        if (cleanUrl.contains("error") || cleanUrl.contains("fail")) {
            return ApiResponse(
                type = "error",
                code = "RESOLVE_FAILED",
                message = "The server encountered a problem trying to scrape the provided link. Try again later."
            )
        }
        if (cleanUrl.contains("expired")) {
            return ApiResponse(
                type = "error",
                code = "EXPIRED",
                message = "The server link has expired or the file source has been deleted."
            )
        }
        if (cleanUrl.contains("geo")) {
            return ApiResponse(
                type = "error",
                code = "GEO_BLOCKED",
                message = "This file stream is restricted in your geographic area."
            )
        }

        // 3. Direct Streaming Case: link contains "stream", "direct", or standard video extensions
        if (cleanUrl.contains("stream") || cleanUrl.contains("direct") || cleanUrl.endsWith(".mp4") || cleanUrl.endsWith(".mkv") || cleanUrl.endsWith(".m3u8")) {
            return ApiResponse(
                type = "stream",
                name = "Sintel HD (Cinematic Demo).mp4",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                headers = mapOf(
                    "User-Agent" to "TeraStream/1.0.0 (Android Native; ExoPlayer)",
                    "Referer" to "https://www.terabox.com/"
                )
            )
        }

        // 4. Default: Folder List Case (Multiple files)
        val items = listOf(
            FolderItem(
                id = "f1",
                name = "Big Buck Bunny Cinema Movie (1080p).mp4",
                sizeBytes = 276192134L,
                durationSec = 596L,
                thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                isVideo = true
            ),
            FolderItem(
                id = "f2",
                name = "Tears of Steel Sci-Fi Short Trailer.mp4",
                sizeBytes = 194512903L,
                durationSec = 734L,
                thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                isVideo = true
            ),
            FolderItem(
                id = "f3",
                name = "Elephant's Dream Storybook.mp4",
                sizeBytes = 168923145L,
                durationSec = 653L,
                thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
                isVideo = true
            ),
            FolderItem(
                id = "f4",
                name = "Video Guide Subtitles.srt",
                sizeBytes = 41243L,
                durationSec = null,
                thumbnail = null,
                isVideo = false
            ),
            FolderItem(
                id = "f5",
                name = "For Bigger Blazes (Action Demo).mp4",
                sizeBytes = 15321094L,
                durationSec = 15L,
                thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg",
                isVideo = true
            )
        )

        return ApiResponse(
            type = "list",
            title = "Cinematic Shorts Collection 2026",
            items = items
        )
    }

    private fun getMockStreamResponse(id: String, url: String): ApiResponse {
        val streamUrl = when (id) {
            "f1" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            "f2" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            "f3" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            "f5" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            else -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
        }

        val name = when (id) {
            "f1" -> "Big Buck Bunny (1080p).mp4"
            "f2" -> "Tears of Steel Sci-Fi Movie.mp4"
            "f3" -> "Elephant's Dream HD.mp4"
            "f5" -> "For Bigger Blazes.mp4"
            else -> "Sintel HD Trailer.mp4"
        }

        return ApiResponse(
            type = "stream",
            name = name,
            streamUrl = streamUrl,
            headers = mapOf(
                "User-Agent" to "TeraStream/1.0.0 (Android Native; ExoPlayer)",
                "Referer" to "https://www.terabox.com/"
            )
        )
    }
}
