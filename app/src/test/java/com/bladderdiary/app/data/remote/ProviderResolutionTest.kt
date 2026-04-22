package com.bladderdiary.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderResolutionTest {
    @Test
    fun `social fallback wins when preferred provider is email`() {
        assertEquals(
            "google",
            resolveProvider(preferredProvider = "email", fallbackProvider = "google")
        )
    }

    @Test
    fun `preferred provider is kept when already social`() {
        assertEquals(
            "kakao",
            resolveProvider(preferredProvider = "kakao", fallbackProvider = "google")
        )
    }

    @Test
    fun `fallback provider is used when preferred provider is absent`() {
        assertEquals(
            "google",
            resolveProvider(preferredProvider = null, fallbackProvider = "google")
        )
    }
}
