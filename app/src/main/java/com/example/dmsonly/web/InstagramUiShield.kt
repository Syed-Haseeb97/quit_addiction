package com.example.dmsonly.web

import android.webkit.WebView

/**
 * Single maintenance point for the DOM/CSS shield.
 *
 * URL-based selectors are preferred because Instagram's markup changes often.
 * Update SELECTORS or the semantic fallback below if Instagram changes its UI.
 */
object InstagramUiShield {

    fun install(webView: WebView) {
        webView.evaluateJavascript(SCRIPT, null)
    }

    private val SCRIPT = """
        (function () {
          'use strict';

          if (window.__DM_ONLY_SHIELD_INSTALLED__) {
            if (window.__DM_ONLY_APPLY_SHIELD__) window.__DM_ONLY_APPLY_SHIELD__();
            return;
          }

          const DM_INBOX = 'https://www.instagram.com/direct/inbox/';

          // Maintenance point: these selectors target Feed, Explore and Reels links.
          const SELECTORS = [
            'a[href="/explore/"]',
            'a[href^="/explore/"]',
            'a[href="/reels/"]',
            'a[href^="/reels/"]',
            'a[href="/"]',
            'a[href="https://www.instagram.com/"]',
            'a[aria-label="Explore"]',
            'a[aria-label="Reels"]',
            '[role="link"][aria-label="Explore"]',
            '[role="link"][aria-label="Reels"]'
          ];

          function instagramPath() {
            if (location.hostname !== 'instagram.com' &&
                location.hostname !== 'www.instagram.com') return null;
            return location.pathname.replace(//+$/, '') || '/';
          }

          function isBlockedPath(path) {
            if (!path) return false;
            return path === '/' ||
                   path === '/explore' ||
                   path.indexOf('/explore/') === 0 ||
                   path === '/reels' ||
                   path.indexOf('/reels/') === 0;
          }

          function routeBackIfBlocked() {
            if (isBlockedPath(instagramPath()) && location.href !== DM_INBOX) {
              location.replace(DM_INBOX);
            }
          }

          function applyShield() {
            SELECTORS.forEach(function (selector) {
              document.querySelectorAll(selector).forEach(function (node) {
                node.style.setProperty('display', 'none', 'important');
                node.setAttribute('data-dm-only-hidden', 'true');
              });
            });

            // Semantic fallback for markup variants/localized navigation.
            document.querySelectorAll('[role="link"], a').forEach(function (node) {
              const label = (node.getAttribute('aria-label') || node.textContent || '')
                .trim().toLowerCase();

              if (label === 'explore' || label === 'reels') {
                node.style.setProperty('display', 'none', 'important');
                node.setAttribute('data-dm-only-hidden', 'true');
              }
            });

            routeBackIfBlocked();
          }

          const originalPushState = history.pushState;
          history.pushState = function () {
            const result = originalPushState.apply(history, arguments);
            queueMicrotask(routeBackIfBlocked);
            queueMicrotask(applyShield);
            return result;
          };

          const originalReplaceState = history.replaceState;
          history.replaceState = function () {
            const result = originalReplaceState.apply(history, arguments);
            queueMicrotask(routeBackIfBlocked);
            queueMicrotask(applyShield);
            return result;
          };

          window.addEventListener('popstate', routeBackIfBlocked, { passive: true });

          let pending = false;
          const observer = new MutationObserver(function () {
            if (pending) return;
            pending = true;
            requestAnimationFrame(function () {
              pending = false;
              applyShield();
            });
          });

          observer.observe(document.documentElement, {
            subtree: true,
            childList: true
          });

          window.__DM_ONLY_APPLY_SHIELD__ = applyShield;
          window.__DM_ONLY_SHIELD_INSTALLED__ = true;

          applyShield();
        })();
    """.trimIndent()
}
