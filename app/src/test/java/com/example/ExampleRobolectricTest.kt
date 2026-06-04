package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.LinkHandler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TeraStream", appName)
    }

    @Test
    fun `link handler extracts first URL from surrounding text`() {
        val pastedText = "Awesome video stream here: https://terabox.com/s/1xyzABCDEF?utm_source=twitter and shared with friends!"
        val result = LinkHandler.normalizeAndExtractLink(pastedText)
        
        assertNotNull(result)
        assertEquals("https://terabox.com/s/1xyzABCDEF?utm_source=twitter", result?.originalUrl)
        assertEquals("https://terabox.com/s/1xyzABCDEF", result?.normalizedUrl)
        assertEquals("1xyzABCDEF", result?.token)
    }

    @Test
    fun `link handler extracts token from surl query parameter`() {
        val pastedText = "https://nephobox.com/sharing/link?surl=t0K3n_abc"
        val result = LinkHandler.normalizeAndExtractLink(pastedText)
        
        assertNotNull(result)
        assertEquals("t0K3n_abc", result?.token)
    }

    @Test
    fun `link handler extracts password query parameter`() {
        val pastedText = "https://1024tera.com/s/foo?pwd=my_secret_pass&utm_medium=referral"
        val result = LinkHandler.normalizeAndExtractLink(pastedText)
        
        assertNotNull(result)
        assertEquals("my_secret_pass", result?.password)
        assertEquals("https://1024tera.com/s/foo?pwd=my_secret_pass", result?.normalizedUrl)
    }

    @Test
    fun `layer 1 allowlist matching works case insensitively with or without www`() {
        // Known allowed domains in lower or mixed case, with or without www.
        assertTrue(LinkHandler.matchesLayer1("https://terabox.com/s/1abc"))
        assertTrue(LinkHandler.matchesLayer1("https://WWW.TERABOX.APP/s/1abc"))
        assertTrue(LinkHandler.matchesLayer1("http://nephobox.com/s/1abc"))
        assertTrue(LinkHandler.matchesLayer1("https://www.fancybox.in/s/1abc"))

        // Host not in allowlist
        assertFalse(LinkHandler.matchesLayer1("https://google.com/s/1abc"))
    }

    @Test
    fun `layer 2 pattern fallbacks correctly identify related links`() {
        // Matches because of host substring 'tera'
        assertTrue(LinkHandler.matchesLayer2("https://custom-teralink-host.xyz/path"))
        
        // Matches because path contains /s/
        assertTrue(LinkHandler.matchesLayer2("https://unrecognized-host.com/s/token"))
        
        // Matches because query contains surl=
        assertTrue(LinkHandler.matchesLayer2("https://unrecognized-host.com/page?surl=123"))

        // Matches because it is a direct video URL ending in .mp4
        assertTrue(LinkHandler.matchesLayer2("https://any-domain-host.org/clips/cinematic.mp4"))

        // Does not match anything in Layer 1 or Layer 2
        assertFalse(LinkHandler.matchesLayer1("https://mysterious-host.net/view/123"))
        assertFalse(LinkHandler.matchesLayer2("https://mysterious-host.net/view/123"))
    }
}
