package com.cybersaad.hackstreak.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Scrapes TryHackMe profile page using an invisible WebView.
 *
 * WHY WebView? TryHackMe is a React SPA behind Cloudflare. OkHttp only gets
 * the empty HTML shell — zero user data. The WebView acts as a real browser:
 * it passes Cloudflare, executes JavaScript, waits for React to render the
 * profile stats, then we inject JS to extract them from the live DOM.
 */
class ThmProfileScraper(private val context: Context) {

    data class ScrapedProfile(
        val username: String,
        val streak: Int,
        val rank: String,
        val points: Int,
        val roomsCompleted: Int,
        val badges: Int,
        val avatarUrl: String,
        val level: String,
        /**
         * Estimated weekly activity derived from streak count.
         * NOT actual per-day data — TryHackMe doesn't expose that reliably.
         * If streak >= 7, all days marked active. Otherwise, most recent N days.
         */
        val weeklyActivity: List<Boolean>
    )

    /**
     * Fetches and parses a TryHackMe user's public profile using WebView.
     * Must be called from a coroutine — it suspends until data is extracted.
     */
    suspend fun scrapeProfile(username: String): Result<ScrapedProfile> =
        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            var webView: WebView? = null
            var resumed = false

            fun finish(result: Result<ScrapedProfile>) {
                if (resumed) return
                resumed = true
                handler.post {
                    webView?.apply {
                        stopLoading()
                        removeJavascriptInterface("HackStreak")
                        destroy()
                    }
                    webView = null
                }
                continuation.resume(result)
            }

            // Timeout after 30 seconds
            val timeoutRunnable = Runnable {
                finish(Result.failure(Exception("Timed out loading profile. Check your connection and try again.")))
            }
            handler.postDelayed(timeoutRunnable, 30000)

            continuation.invokeOnCancellation {
                handler.removeCallbacks(timeoutRunnable)
                handler.post {
                    webView?.apply {
                        stopLoading()
                        removeJavascriptInterface("HackStreak")
                        destroy()
                    }
                }
            }

            handler.post {
                @SuppressLint("SetJavaScriptEnabled")
                val wv = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                }
                webView = wv

                // JavaScript bridge to receive extracted data
                wv.addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onDataReceived(jsonStr: String) {
                        handler.removeCallbacks(timeoutRunnable)
                        try {
                            val json = JSONObject(jsonStr)
                            val error = json.optString("error", "")
                            if (error.isNotEmpty()) {
                                finish(Result.failure(Exception(error)))
                                return
                            }

                            val streak = json.optInt("streak", 0)

                            // Build estimated weekly activity from streak count.
                            // This is an approximation — not actual per-day data.
                            val weeklyActivity = MutableList(7) { false }
                            for (i in 0 until minOf(streak, 7)) {
                                weeklyActivity[6 - i] = true // Fill from Sunday backwards
                            }

                            finish(Result.success(ScrapedProfile(
                                username = username,
                                streak = streak,
                                rank = json.optString("rank", "N/A"),
                                points = json.optInt("points", 0),
                                roomsCompleted = json.optInt("rooms", 0),
                                badges = json.optInt("badges", 0),
                                avatarUrl = json.optString("avatar", ""),
                                level = json.optString("level", ""),
                                weeklyActivity = weeklyActivity
                            )))
                        } catch (e: Exception) {
                            finish(Result.failure(Exception("Failed to parse profile data.")))
                        }
                    }
                }, "HackStreak")

                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject the polling extraction script after page loads.
                        // The script retries every 1s for up to 15 attempts,
                        // waiting for React to render the profile stats.
                        handler.postDelayed({
                            view?.evaluateJavascript(buildExtractionScript(), null)
                        }, 2000)
                    }

                    override fun onReceivedError(
                        view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            handler.removeCallbacks(timeoutRunnable)
                            finish(Result.failure(
                                Exception("Network error: ${error?.description ?: "Unknown"}. Check your internet connection.")
                            ))
                        }
                    }
                }

                wv.loadUrl("https://tryhackme.com/r/p/$username")
            }
        }

    companion object {
        /**
         * JavaScript that polls the rendered DOM for TryHackMe profile stats.
         *
         * The THM profile page renders stat cards as:
         *   "Rank"            -> "Top 50%"   (label on one line, value on next)
         *   "Badges"          -> "2"
         *   "Streak"          -> "14"
         *   "Completed rooms" -> "4"
         *   "Points"          -> "1234"
         *   "[0x2][APPRENTICE]" appears as a span next to the username
         *
         * document.body.innerText produces lines like:
         *   ...Rank\nTop 50%\nBadges\n2\nStreak\n14\nCompleted rooms\n4...
         *
         * The script splits by newlines and matches labels to their next-line values.
         */
        private fun buildExtractionScript(): String = """
            (function() {
                var attempts = 0;
                var maxAttempts = 15;

                function tryExtract() {
                    attempts++;
                    var body = document.body ? document.body.innerText : '';
                    var lines = body.split('\n').map(function(l) { return l.trim(); }).filter(function(l) { return l.length > 0; });

                    var result = {
                        streak: 0, rank: '', level: '', rooms: 0,
                        points: 0, badges: 0, avatar: '', error: ''
                    };

                    // Check if page is a 404 / not found
                    var lowerBody = body.toLowerCase();
                    if ((lowerBody.indexOf('not found') !== -1 || lowerBody.indexOf('page not found') !== -1)
                        && lowerBody.indexOf('streak') === -1) {
                        result.error = 'User not found on TryHackMe. Check the username and try again.';
                        HackStreak.onDataReceived(JSON.stringify(result));
                        return;
                    }

                    // Check if React has rendered the profile (look for stat labels)
                    var hasStreak = lowerBody.indexOf('streak') !== -1;
                    var hasRank = lowerBody.indexOf('rank') !== -1;

                    if (!hasStreak && !hasRank && attempts < maxAttempts) {
                        setTimeout(tryExtract, 1000);
                        return;
                    }

                    // === STRATEGY 1: Line-by-line label matching ===
                    for (var i = 0; i < lines.length; i++) {
                        var line = lines[i];
                        var nextLine = (i + 1 < lines.length) ? lines[i + 1] : '';

                        if (line.toLowerCase() === 'streak' && nextLine.match(/^\d+$/)) {
                            result.streak = parseInt(nextLine);
                        }
                        if (line.toLowerCase() === 'rank' && nextLine.match(/top\s+\d+%/i)) {
                            result.rank = nextLine;
                        }
                        if (line.toLowerCase() === 'completed rooms' && nextLine.match(/^\d+$/)) {
                            result.rooms = parseInt(nextLine);
                        }
                        if (line.toLowerCase() === 'badges' && nextLine.match(/^\d+$/)) {
                            result.badges = parseInt(nextLine);
                        }
                        if (line.toLowerCase() === 'points' && nextLine.match(/^\d[\d,]*$/)) {
                            result.points = parseInt(nextLine.replace(/,/g, ''));
                        }
                    }

                    // === STRATEGY 2: Regex fallbacks ===
                    if (result.streak === 0) {
                        var sm = body.match(/Streak\s+(\d+)/i);
                        if (sm) result.streak = parseInt(sm[1]);
                    }
                    if (result.rank === '') {
                        var rm = body.match(/Top\s+(\d+)\s*%/i);
                        if (rm) result.rank = 'Top ' + rm[1] + '%';
                    }
                    if (result.rooms === 0) {
                        var cm = body.match(/Completed\s+rooms?\s+(\d+)/i);
                        if (cm) result.rooms = parseInt(cm[1]);
                    }
                    if (result.badges === 0) {
                        var bm = body.match(/Badges\s+(\d+)/i);
                        if (bm) result.badges = parseInt(bm[1]);
                    }
                    if (result.points === 0) {
                        var pm = body.match(/Points\s+([\d,]+)/i);
                        if (pm) result.points = parseInt(pm[1].replace(/,/g, ''));
                    }

                    // === Level: [0xN][TITLE] ===
                    var levelMatch = body.match(/\[0x\w+\]\s*\[(\w+)\]/);
                    if (levelMatch) {
                        result.level = levelMatch[1];
                    } else {
                        var lvl2 = body.match(/0x\w+.*?(NEWBIE|HACKER|APPRENTICE|WIZARD|MASTER|GURU)/i);
                        if (lvl2) result.level = lvl2[1];
                    }

                    // === Avatar ===
                    var avatarImg = document.querySelector('img[src*="tryhackme-images"]');
                    if (avatarImg) result.avatar = avatarImg.src;

                    // === STRATEGY 3: DOM walking for streak ===
                    if (result.streak === 0) {
                        var allEls = document.querySelectorAll('div, span, p, h1, h2, h3, h4, h5, h6');
                        for (var el of allEls) {
                            var txt = el.textContent.trim();
                            if (txt === 'Streak' && el.children.length === 0) {
                                var parent = el.parentElement;
                                if (parent) {
                                    var gText = parent.parentElement ? parent.parentElement.innerText : parent.innerText;
                                    var gm = gText.match(/Streak\s+(\d+)/i);
                                    if (gm) result.streak = parseInt(gm[1]);
                                }
                                break;
                            }
                        }
                    }

                    // === STRATEGY 3b: DOM walking for points ===
                    if (result.points === 0) {
                        var allEls2 = document.querySelectorAll('div, span, p, h1, h2, h3, h4, h5, h6');
                        for (var el2 of allEls2) {
                            var txt2 = el2.textContent.trim();
                            if (txt2 === 'Points' && el2.children.length === 0) {
                                var parent2 = el2.parentElement;
                                if (parent2) {
                                    var gText2 = parent2.parentElement ? parent2.parentElement.innerText : parent2.innerText;
                                    var pm2 = gText2.match(/Points\s+([\d,]+)/i);
                                    if (pm2) result.points = parseInt(pm2[1].replace(/,/g, ''));
                                }
                                break;
                            }
                        }
                    }

                    // If still nothing, retry
                    if (result.streak === 0 && result.rank === '' && result.rooms === 0 && attempts < maxAttempts) {
                        setTimeout(tryExtract, 1000);
                        return;
                    }

                    HackStreak.onDataReceived(JSON.stringify(result));
                }

                tryExtract();
            })();
        """.trimIndent()
    }
}
