package com.example.dmsonly.web

import android.webkit.WebView

/**
 * DOM/CSS shielding is deliberately isolated here.
 *
 * URL-level blocking in InstagramRoutes/WebViewClient remains authoritative;
 * this shield removes Feed/Explore/Reels controls from Instagram's UI.
 */
object InstagramUiShield {

    val script: String by lazy { buildScript() }

    fun install(webView: WebView) {
        webView.evaluateJavascript(script, null)
    }

    private fun buildScript(): String {
        val css = listOf(
            """a[href^="/explore"]""",
            """a[href^="/reels"]""",
            """a[href^="/feed"]""",
            """a[href^="/p/"]""",
            """a[href^="/tv/"]""",
            """a[aria-label="Explore"]""",
            """a[aria-label="Search and explore"]""",
            """a[aria-label="Reels"]"""
        ).joinToString(",\n") + " { display: none !important; }"

        val segments = InstagramRoutes.BLOCKED_FIRST_SEGMENTS
            .joinToString(",") { "\"" + it + "\"" }

        val hosts = InstagramRoutes.FEED_HOSTS
            .joinToString(",") { "\"" + it + "\"" }

        return TEMPLATE
            .replace("__INBOX_URL__", InstagramRoutes.DM_INBOX)
            .replace("__BLOCKED_SEGMENTS__", segments)
            .replace("__FEED_HOSTS__", hosts)
            .replace("__CSS__", jsString(css))
    }

    private fun jsString(value: String): String =
        "\"" + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n") + "\""

    private const val TEMPLATE = """
(function () {
  "use strict";
  if (window.top !== window) return;
  if (window.__DM_ONLY_SHIELD__) {
    if (window.__DM_ONLY_APPLY__) window.__DM_ONLY_APPLY__();
    return;
  }

  var INBOX_URL = "__INBOX_URL__";
  var BLOCKED_FIRST_SEGMENTS = [__BLOCKED_SEGMENTS__];
  var FEED_HOSTS = [__FEED_HOSTS__];
  var STYLE_ID = "dms-only-shield-style";
  var HIDDEN_ATTRIBUTE = "data-dms-only-feed-hidden";
  var CSS = __CSS__;

  function isBlockedPath(path) {
    var parts = String(path).split("/").filter(function (s) { return s.length > 0; });
    if (parts.length === 0) return false;
    var first = parts[0];
    try { first = decodeURIComponent(first); } catch (e) {}
    return BLOCKED_FIRST_SEGMENTS.indexOf(first.toLowerCase()) !== -1;
  }

  function isBlockedUrl(raw) {
    try {
      var u = new URL(String(raw), location.href);
      return FEED_HOSTS.indexOf(u.hostname.toLowerCase()) !== -1 && isBlockedPath(u.pathname);
    } catch (e) {
      return false;
    }
  }

  function isHomePath() {
    return FEED_HOSTS.indexOf(location.hostname.toLowerCase()) !== -1 &&
      (location.pathname === "/" || location.pathname === "");
  }

  function hideFeedPosts() {
    if (isHomePath()) {
      document.querySelectorAll("main article, main [role='article']").forEach(function (post) {
        post.setAttribute(HIDDEN_ATTRIBUTE, "true");
        post.style.setProperty("display", "none", "important");
      });
    }

    document.querySelectorAll("a[href^='/p/'], a[href^='/tv/']").forEach(function (link) {
      link.setAttribute(HIDDEN_ATTRIBUTE, "true");
      link.style.setProperty("display", "none", "important");
      var post = link.closest("article, [role='article']");
      if (post) {
        post.setAttribute(HIDDEN_ATTRIBUTE, "true");
        post.style.setProperty("display", "none", "important");
      }
    });
  }

  function applyShield() {
    ensureObserver();
    installStyle();
    hideFeedPosts();
    guardLocation();
  }

  function goInbox() {
    if (location.href !== INBOX_URL) location.replace(INBOX_URL);
  }

  document.addEventListener("click", function (event) {
    var target = event.target;
    var anchor = target && target.closest ? target.closest("a[href]") : null;
    if (anchor && isBlockedUrl(anchor.getAttribute("href"))) {
      event.preventDefault();
      event.stopPropagation();
      event.stopImmediatePropagation();
      goInbox();
    }
  }, true);

  ["pushState", "replaceState"].forEach(function (name) {
    var original = history[name];
    history[name] = function (state, title, url) {
      if (url !== undefined && url !== null && isBlockedUrl(url)) {
        goInbox();
        return;
      }
      var result = original.apply(this, arguments);
      queueMicrotask(applyShield);
      return result;
    };
  });

  function guardLocation() {
    if (isBlockedUrl(location.href)) goInbox();
  }

  window.addEventListener("popstate", applyShield);

  var pending = false;

  function installStyle() {
    pending = false;
    if (document.getElementById(STYLE_ID)) return;
    var root = document.head || document.documentElement;
    if (!root) return;
    var style = document.createElement("style");
    style.id = STYLE_ID;
    style.textContent = CSS;
    root.appendChild(style);
  }

  function scheduleStyle() {
    if (pending) return;
    pending = true;
    setTimeout(applyShield, 100);
  }

  function ensureObserver() {
    if (!document.documentElement || window.__DM_ONLY_OBSERVER__) return;
    window.__DM_ONLY_OBSERVER__ = new MutationObserver(scheduleStyle);
    window.__DM_ONLY_OBSERVER__.observe(
      document.documentElement,
      { childList: true, subtree: true }
    );
  }

  window.__DM_ONLY_APPLY__ = applyShield;
  window.__DM_ONLY_SHIELD__ = true;

  applyShield();
})();
"""
}
