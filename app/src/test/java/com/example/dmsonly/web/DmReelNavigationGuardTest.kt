package com.example.dmsonly.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DmReelNavigationGuardTest {

    @Test
    fun dmOriginatedIndividualReelsAreAllowedButGenericReelsReturnToLastItem() {
        val guard = DmReelNavigationGuard()

        assertNull(guard.redirectTarget(InstagramRoutes.DM_INBOX))
        assertNull(guard.redirectTarget("https://www.instagram.com/reel/first/"))
        assertNull(guard.redirectTarget("https://www.instagram.com/reel/next/"))
        assertEquals(
            "https://www.instagram.com/reel/next/",
            guard.redirectTarget("https://www.instagram.com/reels/")
        )
        assertEquals(
            "https://www.instagram.com/reel/next/",
            guard.redirectTarget("https://www.instagram.com/reels/audio/123/")
        )
    }

    @Test
    fun leavingTheDmReelContextClearsItsAllowance() {
        val guard = DmReelNavigationGuard()

        guard.redirectTarget(InstagramRoutes.DM_INBOX)
        guard.redirectTarget("https://www.instagram.com/reel/shared/")
        assertNull(guard.redirectTarget("https://www.instagram.com/direct/inbox/"))
        assertNull(guard.redirectTarget("https://www.instagram.com/reels/"))
    }

    @Test
    fun individualReelNavigationWithoutDmOriginDoesNotCreateContext() {
        val guard = DmReelNavigationGuard()

        guard.redirectTarget("https://www.instagram.com/accounts/login/")
        assertEquals(
            InstagramRoutes.DM_INBOX,
            guard.redirectTarget("https://www.instagram.com/reel/unrelated/")
        )

        assertNull(guard.redirectTarget("https://www.instagram.com/reels/"))
    }

    @Test
    fun restoredHistoryRetainsOnlyTheDmOriginatedReelContext() {
        val guard = DmReelNavigationGuard()
        val history = listOf(
            InstagramRoutes.DM_INBOX,
            "https://www.instagram.com/reel/shared/"
        )

        guard.restoreFromHistory(history, currentIndex = 1)
        assertEquals(
            "https://www.instagram.com/reel/shared/",
            guard.redirectTarget("https://www.instagram.com/reels/")
        )
    }

    @Test
    fun restoredUnrelatedReelHistoryDoesNotCreateContext() {
        val guard = DmReelNavigationGuard()
        val history = listOf(
            "https://www.instagram.com/explore/",
            "https://www.instagram.com/reel/unrelated/"
        )

        guard.restoreFromHistory(history, currentIndex = 1)
        assertEquals(
            InstagramRoutes.DM_INBOX,
            guard.redirectTarget("https://www.instagram.com/reel/unrelated/")
        )
    }
}
