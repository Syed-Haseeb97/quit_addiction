package com.example.dmsonly.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramUiShieldTest {

    @Test
    fun homeShieldUsesPersistentCssInsteadOfMutatingFeedNodes() {
        val script = InstagramUiShield.script

        assertTrue(script.contains("html[data-dms-only-home=\\\"true\\\"] main article"))
        assertTrue(script.contains("a[href^=\\\"/p/\\\"]"))
        assertTrue(script.contains("a[href^=\\\"/tv/\\\"]"))
        assertTrue(script.contains("function isHomePath()"))
        assertFalse(script.contains("function hideFeedPosts()"))
        assertFalse(script.contains("new MutationObserver"))
        assertFalse(script.contains("setProperty(\"display\", \"none\""))
    }

    @Test
    fun shieldGuardsSpaHistoryAndDmReelContext() {
        val script = InstagramUiShield.script

        assertTrue(script.contains("\"pushState\", \"replaceState\""))
        assertTrue(script.contains("window.addEventListener(\"popstate\", applyShield)"))
        assertTrue(script.contains("dms-only-dm-reel-context"))
        assertTrue(script.contains("dms-only-last-dm-reel-url"))
        assertTrue(script.contains("function isGenericReelsPath(path)"))
        assertTrue(script.contains("function routeTransitionBlocked(raw)"))
        assertTrue(script.contains("function isBlockedUrl(raw)"))
    }
}
