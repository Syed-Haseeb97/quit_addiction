package com.example.dmsonly.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramRoutesTest {

    @Test
    fun dmRoutesAreAllowed() {
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/direct/inbox/"))
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/direct/t/123/"))
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/direct/requests/"))
    }

    @Test
    fun feedExploreAndReelsAreBlocked() {
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/"))
        assertTrue(InstagramRoutes.isBlocked("https://www.instagram.com/explore/"))
        assertTrue(InstagramRoutes.isBlocked("https://www.instagram.com/explore/tags/android/"))
        assertTrue(InstagramRoutes.isBlocked("https://www.instagram.com/reels/"))
        assertTrue(InstagramRoutes.isBlocked("https://www.instagram.com/reels/audio/123/"))
        assertTrue(InstagramRoutes.isBlocked("https://www.instagram.com/feed/"))
        assertTrue(InstagramRoutes.isBlocked("https://www.instagram.com/p/shortcode/"))
        assertTrue(InstagramRoutes.isBlocked("https://www.instagram.com/tv/shortcode/"))
    }

    @Test
    fun storiesAndIndividualReelRoutesRemainAllowed() {
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/stories/user/123/"))
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/reel/shortcode/"))
    }

    @Test
    fun rootBackDestinationIsAllowedForStoriesWhileFeedPostsAreBlocked() {
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/"))
        assertTrue(InstagramRoutes.isBlocked("https://www.instagram.com/p/shortcode/"))
    }

    @Test
    fun authenticationRoutesRemainAvailable() {
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/accounts/login/"))
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/challenge/abc/"))
        assertFalse(InstagramRoutes.isBlocked("https://www.instagram.com/checkpoint/foo/"))
    }

    @Test
    fun navigationDecisionRedirectsBlockedInstagramRoutes() {
        val decision = InstagramRoutes.decide("https://www.instagram.com/reels/audio/123/")
        assertTrue(decision is InstagramRoutes.Decision.Redirect)
        assertEquals(InstagramRoutes.DM_INBOX, (decision as InstagramRoutes.Decision.Redirect).url)
    }

    @Test
    fun navigationDecisionOpensExternalHttpLinks() {
        val decision = InstagramRoutes.decide("https://example.com/help")
        assertTrue(decision is InstagramRoutes.Decision.OpenExternally)
    }

    @Test
    fun navigationDecisionBlocksNonHttpSchemes() {
        assertTrue(InstagramRoutes.decide("intent://instagram/") is InstagramRoutes.Decision.Block)
    }

    @Test
    fun onlyInstagramHostsAreSubjectToInstagramRules() {
        assertFalse(InstagramRoutes.isInstagramHost("https://example.com/explore/"))
        assertFalse(InstagramRoutes.isBlocked("https://example.com/explore/"))
        assertTrue(InstagramRoutes.isInstagramHost("https://www.instagram.com/direct/"))
    }
}
