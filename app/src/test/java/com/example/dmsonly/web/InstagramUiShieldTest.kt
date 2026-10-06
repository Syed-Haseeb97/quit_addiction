package com.example.dmsonly.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramUiShieldTest {

    @Test
    fun homeShieldUsesPersistentCssWithoutMutatingFeedNodes() {
        val script = InstagramUiShield.script

        assertTrue(script.contains("html[data-dms-only-home=\\\"true\\\"] main article"))
        assertTrue(script.contains("visibility: hidden !important"))
        assertTrue(script.contains("pointer-events: none !important"))
        assertTrue(script.contains("a[href^=\\\"/p/\\\"]"))
        assertTrue(script.contains("a[href^=\\\"/tv/\\\"]"))
        assertTrue(script.contains("function isHomePath()"))
        assertFalse(script.contains("function hideFeedPosts()"))
        assertFalse(script.contains("new MutationObserver"))
        assertFalse(script.contains("setProperty(\\\"display\\\", \\"none\\\""))
    }

    @Test
    fun shieldGuardsSpaHistoryAndDmReelContext() {
        val script = InstagramUiShield.script

        assertTrue(script.contains("[\"pushState\", \"replaceState\"]"))
        assertTrue(script.contains("window.addEventListener(\"popstate\", applyShield)"))
        assertTrue(script.contains("window.addEventListener(\"hashchange\", applyShield)"))
        assertTrue(script.contains("function isGenericReelsPath(path)"))
        assertTrue(script.contains("function routeTransitionBlocked(raw)"))
        assertTrue(script.contains("function isBlockedUrl(raw)"))
    }

    @Test
    fun dopamineFreeShieldIncludesEngagementNeutralizers() {
        val script = InstagramUiShield.scriptFor(true)
        assertTrue(script.contains("/accounts/activity"))
        assertTrue(script.contains("Notifications"))
        assertTrue(script.contains("likes"))
    }

    @Test
    fun normalShieldDoesNotAddDopamineNeutralizers() {
        val script = InstagramUiShield.scriptFor(false)
        assertFalse(script.contains("/accounts/activity"))
    }

    @Test
    fun shieldDoesNotPersistReelContextInStorage() {
        val script = InstagramUiShield.script

        assertFalse(script.contains("sessionStorage"))
        assertFalse(script.contains("localStorage"))
    }
}
