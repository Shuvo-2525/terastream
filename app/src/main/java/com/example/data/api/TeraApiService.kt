package com.example.data.api

import com.example.data.model.ApiResponse
import com.example.data.model.ResolveRequest
import com.example.data.model.StreamRequest
import retrofit2.http.Body
import retrofit2.http.POST

// === EDITABLE BASE URL CONSTANT ===
// Change this to point to the actual hosted resolver backend API.
const val BASE_URL = "https://ais-dev-amfldoq6ppfodox6szjxox-464227396947.asia-southeast1.run.app/api/"

interface TeraApiService {
    @POST("resolve")
    suspend fun resolveLink(@Body request: ResolveRequest): ApiResponse

    @POST("stream")
    suspend fun getStreamUrl(@Body request: StreamRequest): ApiResponse
}
