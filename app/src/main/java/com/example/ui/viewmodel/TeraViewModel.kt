package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.FolderItem
import com.example.data.model.RecentLink
import com.example.data.repository.TeraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TeraUiState {
    object Idle : TeraUiState
    object Loading : TeraUiState
    data class ListReady(val title: String, val items: List<FolderItem>) : TeraUiState
    data class StreamReady(
        val name: String,
        val url: String,
        val headers: Map<String, String>,
        val originalShareUrl: String,
        val fileId: String? = null
    ) : TeraUiState
    data class Error(val code: String, val message: String, val urlAttempted: String) : TeraUiState
}

class TeraViewModel(private val repository: TeraRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TeraUiState>(TeraUiState.Idle)
    val uiState: StateFlow<TeraUiState> = _uiState.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _inlineLoadingId = MutableStateFlow<String?>(null)
    val inlineLoadingId: StateFlow<String?> = _inlineLoadingId.asStateFlow()

    // Flag for tracking folder contents caching to return to selection screen on back
    var lastFolderTitle: String? = null
        private set
    var lastFolderItems: List<FolderItem>? = null
        private set

    // Flag for tracking if we already attempted one automatic refresh for expired links
    private var isRefreshingExpired = false

    // Load recent links from room DB
    val recentLinks: StateFlow<List<RecentLink>> = repository.recentLinks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onUrlInputChange(newValue: String) {
        _urlInput.value = newValue
    }

    fun onPasswordInputChange(newValue: String) {
        _passwordInput.value = newValue
    }

    // Check if the pasted string is a plausible URL
    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return true
        return LinkHandler.normalizeAndExtractLink(url) != null
    }

    // Primary action: stream/resolve the link
    fun resolveLink(url: String, password: String? = null) {
        if (url.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = TeraUiState.Loading
            
            val normalized = LinkHandler.normalizeAndExtractLink(url)
            val urlToSend = normalized?.normalizedUrl ?: url
            val extractedPwd = password ?: normalized?.password ?: _passwordInput.value.takeIf { it.isNotBlank() }
            val extractedToken = normalized?.token
            
            val response = repository.resolveUrl(urlToSend, extractedPwd, extractedToken)
            
            when (response.type) {
                "stream" -> {
                    _uiState.value = TeraUiState.StreamReady(
                        name = response.name ?: "Streamed Media",
                        url = response.streamUrl ?: "",
                        headers = response.headers ?: emptyMap(),
                        originalShareUrl = normalized?.originalUrl ?: url
                    )
                }
                "list" -> {
                    val title = response.title ?: "Folder Contents"
                    val items = response.items ?: emptyList()
                    lastFolderTitle = title
                    lastFolderItems = items
                    _uiState.value = TeraUiState.ListReady(title = title, items = items)
                }
                "error" -> {
                    _uiState.value = TeraUiState.Error(
                        code = response.code ?: "RESOLVE_FAILED",
                        message = response.message ?: "An unknown resolution error occurred.",
                        urlAttempted = normalized?.originalUrl ?: url
                    )
                }
                else -> {
                    _uiState.value = TeraUiState.Error(
                        code = "MALFORMED_RESPONSE",
                        message = "The server returned an invalid or unsupported response structure.",
                        urlAttempted = normalized?.originalUrl ?: url
                    )
                }
            }
        }
    }

    // Called when a user taps an item in the video selection screen
    fun selectFile(item: FolderItem, originalShareUrl: String) {
        viewModelScope.launch {
            _inlineLoadingId.value = item.id
            val response = repository.getStreamForFile(item.id, originalShareUrl)
            _inlineLoadingId.value = null

            if (response.type == "stream") {
                _uiState.value = TeraUiState.StreamReady(
                    name = response.name ?: item.name,
                    url = response.streamUrl ?: "",
                    headers = response.headers ?: emptyMap(),
                    originalShareUrl = originalShareUrl,
                    fileId = item.id
                )
            } else if (response.type == "error") {
                _uiState.value = TeraUiState.Error(
                    code = response.code ?: "RESOLVE_FAILED",
                    message = response.message ?: "Failed to generate stream for '${item.name}'",
                    urlAttempted = originalShareUrl
                )
            }
        }
    }

    // Automatically handle playback failure/expiry refresh once
    fun handlePlaybackError(originalShareUrl: String, fileId: String?) {
        if (isRefreshingExpired) {
            // Already tried refreshing once and it failed again, show error state now to prevent loop
            _uiState.value = TeraUiState.Error(
                code = "EXPIRED",
                message = "The source video link has expired. Please re-generate your stream links.",
                urlAttempted = originalShareUrl
            )
            isRefreshingExpired = false
            return
        }

        isRefreshingExpired = true
        _uiState.value = TeraUiState.Loading

        viewModelScope.launch {
            if (fileId != null) {
                // Refresh inline file request
                val response = repository.getStreamForFile(fileId, originalShareUrl)
                if (response.type == "stream") {
                    _uiState.value = TeraUiState.StreamReady(
                        name = response.name ?: "Streamed Media",
                        url = response.streamUrl ?: "",
                        headers = response.headers ?: emptyMap(),
                        originalShareUrl = originalShareUrl,
                        fileId = fileId
                    )
                } else {
                    _uiState.value = TeraUiState.Error(
                        code = "EXPIRED_REFRESH_FAILED",
                        message = "Visual streaming stream expired and auto-renewal failed.",
                        urlAttempted = originalShareUrl
                    )
                }
            } else {
                // Refresh root url
                val response = repository.resolveUrl(originalShareUrl)
                if (response.type == "stream") {
                    _uiState.value = TeraUiState.StreamReady(
                        name = response.name ?: "Streamed Media",
                        url = response.streamUrl ?: "",
                        headers = response.headers ?: emptyMap(),
                        originalShareUrl = originalShareUrl
                    )
                } else {
                    _uiState.value = TeraUiState.Error(
                        code = "EXPIRED_REFRESH_FAILED",
                        message = "Direct video stream expired and auto-renewal failed.",
                        urlAttempted = originalShareUrl
                    )
                }
            }
        }
    }

    fun deleteRecent(url: String) {
        viewModelScope.launch {
            repository.deleteRecentLink(url)
        }
    }

    fun clearAllRecents() {
        viewModelScope.launch {
            repository.clearAllRecentLinks()
        }
    }

    fun resetToIdle() {
        _uiState.value = TeraUiState.Idle
        _passwordInput.value = ""
        lastFolderTitle = null
        lastFolderItems = null
        isRefreshingExpired = false
    }

    fun goBackFromPlayer() {
        val title = lastFolderTitle
        val items = lastFolderItems
        if (title != null && items != null) {
            _uiState.value = TeraUiState.ListReady(title, items)
        } else {
            resetToIdle()
        }
    }

    fun resetToFolder(title: String, items: List<FolderItem>) {
        _uiState.value = TeraUiState.ListReady(title, items)
    }
}

class TeraViewModelFactory(private val repository: TeraRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeraViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

object LinkHandler {
    val ALLOWED_DOMAINS = listOf(
        "terabox.com", "terabox.app", "teraboxapp.com", "1024tera.com", "1024tera.co", 
        "1024terabox.com", "1024-terabox.com", "tera1024box.com", "1024teraboxlink.com", 
        "nephobox.com", "mirrobox.com", "4funbox.com", "4funbox.co", "4funbox.in", 
        "freeterabox.com", "tibibox.com", "momerybox.com", "memorybox.com", "gibibox.com", 
        "pebibox.com", "fancybox.in", "bestclouddrive.com", "terasharelink.com", 
        "terasharefile.com", "terashareus.com", "terafileshare.com", "teraboxshare.com", 
        "teraboxsharefile.com", "teraboxlink.com", "teraboxlinks.com", "teraboxurl.com", 
        "teraboxfree.com", "teraboxshort.com", "teraboxshortlink.com", "teraboxfan.com", 
        "urlshortterabox.com", "shortlinkshare.com", "terabox.link", "terabox.fun", 
        "terabox.club", "terabox.click"
    )

    fun normalizeAndExtractLink(pastedText: String): NormalizationResult? {
        val trimmed = pastedText.trim()
        val urlRegex = "https?://[^\\s\\\"'<>]+".toRegex(RegexOption.IGNORE_CASE)
        val match = urlRegex.find(trimmed) ?: return null
        var url = match.value
        
        // Strip trailing punctuation
        while (url.isNotEmpty() && (url.endsWith(".") || url.endsWith(",") || url.endsWith(")") || url.endsWith("]") || url.endsWith(";"))) {
            url = url.substring(0, url.length - 1)
        }
        
        if (url.isBlank()) return null
        
        // Extract original token
        var token: String? = null
        val surlRegex = "[?&]surl=([^&]+)".toRegex(RegexOption.IGNORE_CASE)
        val surlMatch = surlRegex.find(url)
        if (surlMatch != null) {
            token = surlMatch.groupValues[1]
        } else {
            val sPathRegex = "/s/([^/?#]+)".toRegex(RegexOption.IGNORE_CASE)
            val sPathMatch = sPathRegex.find(url)
            if (sPathMatch != null) {
                token = sPathMatch.groupValues[1]
            }
        }
        
        // Extract password (pwd) if present
        val pwdRegex = "[?&]pwd=([^&]+)".toRegex(RegexOption.IGNORE_CASE)
        val pwdMatch = pwdRegex.find(url)
        val password = pwdMatch?.groupValues?.get(1)

        val strippedUrl = stripTrackingParameters(url)

        return NormalizationResult(
            originalUrl = url,
            normalizedUrl = strippedUrl,
            token = token,
            password = password
        )
    }

    private fun stripTrackingParameters(url: String): String {
        try {
            val uri = android.net.Uri.parse(url)
            val queryNames = uri.queryParameterNames ?: return url
            if (queryNames.isEmpty()) return url
            
            val builder = uri.buildUpon()
            builder.clearQuery()
            
            for (name in queryNames) {
                val isTracking = name.lowercase().startsWith("utm_") || 
                                 name.lowercase() == "spm" || 
                                 name.lowercase() == "shfl" || 
                                 name.lowercase() == "tracking" || 
                                 name.lowercase() == "_at_" ||
                                 name.lowercase() == "fbclid" ||
                                 name.lowercase() == "gclid"
                if (!isTracking) {
                    val value = uri.getQueryParameter(name)
                    builder.appendQueryParameter(name, value)
                }
            }
            return builder.build().toString()
        } catch (e: Exception) {
            return url
        }
    }

    fun extractHost(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host ?: ""
            host.lowercase()
        } catch (e: Exception) {
            ""
        }
    }

    fun matchesLayer1(url: String): Boolean {
        val host = extractHost(url)
        if (host.isEmpty()) return false
        
        val cleanHost = if (host.startsWith("www.")) host.substring(4) else host
        return ALLOWED_DOMAINS.any { domain ->
            cleanHost == domain.lowercase()
        }
    }

    fun matchesLayer2(url: String): Boolean {
        val cleanUrl = url.trim().lowercase()
        val videoExtensions = listOf(".mp4", ".mkv", ".m3u8", ".mov", ".flv", ".webm", ".avi")
        val hasVideoExtension = videoExtensions.any { cleanUrl.substringBefore("?").endsWith(it) }
        if (hasVideoExtension) {
            return true
        }

        val host = extractHost(url)
        val hostSubstrings = listOf("tera", "funbox", "nephobox", "mirrobox", "tibibox", "gibibox", "pebibox", "momerybox", "memorybox")
        val matchesHostSubstring = hostSubstrings.any { host.contains(it) }
        if (matchesHostSubstring) {
            return true
        }
        
        val pathContainsS = url.contains("/s/", ignoreCase = true)
        val hasSurlParam = url.contains("surl=", ignoreCase = true)
        if (pathContainsS || hasSurlParam) {
            return true
        }
        
        return false
    }
}

data class NormalizationResult(
    val originalUrl: String,
    val normalizedUrl: String,
    val token: String?,
    val password: String?
)
