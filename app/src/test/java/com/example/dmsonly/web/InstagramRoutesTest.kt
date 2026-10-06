package com.example.dmsonly.web

import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramRoutesTest {

    @Test
    fun dmRoutesAreAllowed() {
        assertFalse(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/direct/inbox/")))
        assertFalse(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/direct/t/123/")))
        assertFalse(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/direct/requests/")))
    }

    @Test
    fun feedExploreAndReelsAreBlocked() {
        assertTrue(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/")))
        assertTrue(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/explore/")))
        assertTrue(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/explore/tags/android/")))
        assertTrue(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/reels/")))
        assertTrue(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/reels/audio/123/")))
    }

    @Test
    fun authenticationRoutesRemainAvailable() {
        assertFalse(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/accounts/login/")))
        assertFalse(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/challenge/abc/")))
        assertFalse(InstagramRoutes.isBlocked(Uri.parse("https://www.instagram.com/checkpoint/foo/")))
    }

    @Test
    fun onlyInstagramHostsAreSubjectToInstagramRules() {
        assertFalse(InstagramRoutes.isInstagramHost(Uri.parse("https://example.com/explore/")))
        assertFalse(InstagramRoutes.isBlocked(Uri.parse("https://example.com/explore/")))
        assertTrue(InstagramRoutes.isInstagramHost(Uri.parse("https://www.instagram.com/direct/")))
    }
}
