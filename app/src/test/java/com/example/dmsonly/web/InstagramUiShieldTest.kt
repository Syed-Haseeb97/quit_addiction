package com.example.dmsonly.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramUiShieldTest {

    @Test
    fun homeShieldHidesFeedArticlesAndPostTilesWithoutHidingStories() {
        val script = InstagramUiShield.script

        assertTrue(script.contains("main article"))
        assertTrue(script.contains("a[href^='/p/']"))
        assertTrue(script.contains("a[href^='/tv/']"))
        assertTrue(script.contains("function isHomePath()"))
        assertFalse(script.contains("\"stories\""))
    }

    @Test
    fun shieldGuardsSpaHistoryAndReattachesItsObserver() {
        val script = InstagramUiShield.script

        assertTrue(script.contains("\"pushState\", \"replaceState\""))
        assertTrue(script.contains("window.addEventListener(\"popstate\", applyShield)"))
        assertTrue(script.contains("function ensureObserver()"))
        assertTrue(script.contains("function isBlockedUrl(raw)"))
    }
}
