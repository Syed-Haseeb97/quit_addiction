package com.example.dmsonly.web

import android.webkit.WebView

/**
 * DOM/CSS shielding is deliberately isolated here.
 *
 * URL-level blocking in InstagramRoutes/WebViewClient remains authoritative;
 * this shield removes Feed/Explore/Reels controls without mutating Feed nodes.
 */
object InstagramUiShield {

    val script: String by lazy { buildScript(false) }

    fun install(webView: WebView, dopamineFreeUi: Boolean = false) {
        webView.evaluateJavascript(buildScript(dopamineFreeUi), null)
    }

    fun scriptFor(dopamineFreeUi: Boolean): String = buildScript(dopamineFreeUi)

    private fun buildScript(dopamineFreeUi: Boolean): String {
        val dopamineCss = if (dopamineFreeUi) listOf("""a[href*="/accounts/activity"]""", """a[aria-label*="Notifications" i]""", """[aria-label*="notification" i][role="button"]""", """span[aria-label*="notification" i]""", """span[aria-label*="likes" i]""").joinToString(",\n") + " { visibility: hidden !important; pointer-events: none !important; }" else ""

        val css = listOf(
            """html[data-dms-only-home="true"] main article""",
            """html[data-dms-only-home="true"] main [role="article"]""",
            """a[href^="/explore"]""",
            """a[href^="/reels"]""",
            """a[href^="/feed"]""",
            """a[href^="/p/"]""",
            """a[href^="/tv/"]""",
            """a[aria-label="Explore"]""",
            """a[aria-label="Search and explore"]""",
            """a[aria-label="Reels"]"""
        ).joinToString(",\n") + " { visibility: hidden !important; pointer-events: none !important; }" + dopamineCss

        val segments = InstagramRoutes.BLOCKED_FIRST_SEGMENTS
            .joinToString(",") { "\"" + it + "\"" }

        val hosts = InstagramRoutes.FEED_HOSTS
            .joinToString(",") { "\"" + it + "\"" }

        return TEMPLATE
            .replace("__INBOX_URL__", InstagramRoutes.DM_INBOX)
            .replace("__BLOCKED_SEGMENTS__", segments)
            .replace("__FEED_HOSTS__", hosts)
            .replace("__DOPAMINE_FREE__", dopamineFreeUi.toString())
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
  var INBOX_URL = "__INBOX_URL__";
  var BLOCKED_FIRST_SEGMENTS = [__BLOCKED_SEGMENTS__];
  var FEED_HOSTS = [__FEED_HOSTS__];
  var STYLE_ID = "dms-only-shield-style";
  var CSS = __CSS__;
  var DOPAMINE_FREE_UI = __DOPAMINE_FREE__;
  var lastRoutePath = location.pathname;
  var dmReelContextActive = false;
  var lastDmReelUrl = null;

  if (window.__DM_ONLY_SHIELD__) {
    window.__DM_ONLY_DOPAMINE_FREE__ = DOPAMINE_FREE_UI;
    var existingStyle = document.getElementById(STYLE_ID);
    if (existingStyle) existingStyle.textContent = CSS;
    if (window.__DM_ONLY_APPLY__) window.__DM_ONLY_APPLY__();
    return;
  }

  try {
    var referrer = new URL(document.referrer);
    if (isDirectPath(referrer.pathname) && isIndividualReelPath(lastRoutePath)) {
      dmReelContextActive = true;
      lastDmReelUrl = location.href;
    }
  } catch (e) {}

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

  function isDirectPath(path) {
    return path === "/direct" || path.indexOf("/direct/") === 0;
  }

  function isIndividualReelPath(path) {
    return path === "/reel" || path.indexOf("/reel/") === 0;
  }

  function isGenericReelsPath(path) {
    return path === "/reels" || path.indexOf("/reels/") === 0;
  }

  function clearDmReelContext() {
    dmReelContextActive = false;
    lastDmReelUrl = null;
  }

  function rememberDmReelUrl(url) {
    lastDmReelUrl = url;
  }

  function routeTransitionBlocked(raw) {
    try {
      var target = new URL(String(raw), location.href);
      if (FEED_HOSTS.indexOf(target.hostname.toLowerCase()) === -1) return false;
      if (isDirectPath(lastRoutePath) && isIndividualReelPath(target.pathname)) {
        dmReelContextActive = true;
        rememberDmReelUrl(target.href);
        return false;
      }
      if (isIndividualReelPath(target.pathname) && !dmReelContextActive) {
        goInbox();
        return true;
      }
      if (dmReelContextActive && isIndividualReelPath(target.pathname)) {
        rememberDmReelUrl(target.href);
        return false;
      }
      if (dmReelContextActive && isGenericReelsPath(target.pathname)) {
        returnToDmReel();
        return true;
      }
      if (dmReelContextActive && !isIndividualReelPath(target.pathname)) {
        clearDmReelContext();
      }
      if (isBlockedUrl(target.href)) {
        goInbox();
        return true;
      }
      return false;
    } catch (e) {
      return false;
    }
  }

  function updateRouteState() {
    var currentPath = location.pathname;
    if (isDirectPath(lastRoutePath) && isIndividualReelPath(currentPath)) {
      dmReelContextActive = true;
      rememberDmReelUrl(location.href);
    } else if (dmReelContextActive && isIndividualReelPath(currentPath)) {
      rememberDmReelUrl(location.href);
    } else if (dmReelContextActive && isGenericReelsPath(currentPath)) {
      returnToDmReel();
      return;
    } else if (dmReelContextActive) {
      clearDmReelContext();
    }
    lastRoutePath = currentPath;
  }

  function isHomePath() {
    return FEED_HOSTS.indexOf(location.hostname.toLowerCase()) !== -1 &&
      (location.pathname === "/" || location.pathname === "");
  }

  function applyShield() {
    updateRouteState();
    installStyle();
    var homeValue = isHomePath() ? "true" : null;
    if (document.documentElement.getAttribute("data-dms-only-home") !== homeValue) {
      if (homeValue) document.documentElement.setAttribute("data-dms-only-home", homeValue);
      else document.documentElement.removeAttribute("data-dms-only-home");
    }
    guardLocation();
  }

  function goInbox() {
    if (location.href !== INBOX_URL) location.replace(INBOX_URL);
  }

  function returnToDmReel() {
    if (lastDmReelUrl) location.replace(lastDmReelUrl);
    else goInbox();
  }

  ["pushState", "replaceState"].forEach(function (name) {
    var original = history[name];
    history[name] = function (state, title, url) {
      if (url !== undefined && url !== null && routeTransitionBlocked(url)) return;
      var result = original.apply(this, arguments);
      queueMicrotask(applyShield);
      return result;
    };
  });

  document.addEventListener("click", function (event) {
    var target = event.target;
    var anchor = target && target.closest ? target.closest("a[href]") : null;
    if (anchor && routeTransitionBlocked(anchor.getAttribute("href"))) {
      event.preventDefault();
      event.stopPropagation();
      event.stopImmediatePropagation();
    }
  }, true);

  window.addEventListener("popstate", applyShield);
  window.addEventListener("hashchange", applyShield);

  function guardLocation() {
    if (!isBlockedUrl(location.href)) return;
    if (dmReelContextActive && isGenericReelsPath(location.pathname)) returnToDmReel();
    else goInbox();
  }

  function installStyle() {
    if (document.getElementById(STYLE_ID)) return;
    var root = document.head || document.documentElement;
    if (!root) return;
    var style = document.createElement("style");
    style.id = STYLE_ID;
    style.textContent = CSS;
    root.appendChild(style);
  }

  window.__DM_ONLY_APPLY__ = applyShield;
  window.__DM_ONLY_SHIELD__ = true;
  window.__DM_ONLY_DOPAMINE_FREE__ = DOPAMINE_FREE_UI;
  applyShield();
})();
"""
}