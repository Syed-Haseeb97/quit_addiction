package com.example.dmsonly.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramBackNavigationTest {

    @Test
    fun androidBackSkipsBlockedPostAndReelsHistoryEntries() {
        val history = listOf(
            InstagramRoutes.DM_INBOX,
            "https://www.instagram.com/stories/user/123/",
            "https://www.instagram.com/p/post/",
            "https://www.instagram.com/reels/"
        )

        assertEquals(
            1,
            InstagramBackNavigation.previousAllowedHistoryIndex(history, currentIndex = 3)
        )
        assertEquals(
            0,
            InstagramBackNavigation.previousAllowedHistoryIndex(
                listOf(
                    InstagramRoutes.DM_INBOX,
                    "https://www.instagram.com/p/post/",
                    "https://www.instagram.com/reels/"
                ),
                currentIndex = 2
            )
        )
    }

    @Test
    fun androidBackCanReturnToHomeStorySurface() {
        val history = listOf(
            InstagramRoutes.DM_INBOX,
            "https://www.instagram.com/"
        )

        assertEquals(
            0,
            InstagramBackNavigation.previousAllowedHistoryIndex(history, currentIndex = 1)
        )
    }

    @Test
    fun androidBackHasNoAllowedTargetWhenOnlyBlockedHistoryRemains() {
        val history = listOf(
            "https://www.instagram.com/p/post/",
            "https://www.instagram.com/reels/"
        )

        assertNull(InstagramBackNavigation.previousAllowedHistoryIndex(history, currentIndex = 1))
    }
}
