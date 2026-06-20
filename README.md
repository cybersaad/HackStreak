<div align="center">
  
# HackStreak
**The Ultimate TryHackMe Streak Tracker for Android**

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-UI-blue.svg?style=for-the-badge&logo=android)
![Glance](https://img.shields.io/badge/Glance-App_Widgets-green.svg?style=for-the-badge&logo=android)
![Room](https://img.shields.io/badge/Room-Database-orange.svg?style=for-the-badge)

*Built to solve the frustration of manually checking your TryHackMe profile every day. Created for time saving like this and **Develop with Vibe**.*

</div>

---

## Overview

**HackStreak** is a completely native Android application designed to track your [TryHackMe](https://tryhackme.com) streak directly from your home screen. While it requires an internet connection to fetch and sync your live stats, it seamlessly caches this data so your widget and dashboard remain visible even when you're offline. TryHackMe doesn't offer an official public API, and their web application is heavily shielded by Cloudflare and client-side React rendering. 

This application bridges the gap by employing a robust, headless **WebView Scraping Engine** that mimics a real browser to bypass Cloudflare challenges, wait for React to hydrate, and securely extract your live streak, rank, and weekly activity right to your Android device.

## Key Features

- **Real-Time Streak Tracking**: Never lose your streak again. HackStreak pulls your exact day count, badges, and platform rank.
- **Fully Responsive Widgets**: Built with Jetpack Glance, the widgets dynamically resize to small, medium, and large layouts. It actively adapts to narrow or squat dimensions, hiding/showing elements (like the stats bar) to ensure your streak is never cut off!
- **Manual Widget Refresh**: Features a convenient refresh button directly on the widget header to instantly sync your latest profile data in the background without opening the app.
- **Cloudflare Bypass Engine**: Completely abandons fragile HTTP/OkHttp requests in favor of a silent, background WebView. The app actively polls the DOM via injected JavaScript to bypass Cloudflare's JS-challenge and extract the rendered React data.
- **Offline Persistence**: Uses **Room Database** and **DataStore** to cache your profile. If you're offline or TryHackMe is down, your widget and app still display your last known stats.
- **"GitStreak" Style Setup & THM Dashboard**: Features a hyper-clean MVP-style setup flow (Header, Connection Card, Widget Guide) seamlessly paired with a gorgeous TryHackMe dashboard (Circular fire progress rings, M T W T F S S dot indicators, and a 2x2 stats grid).

## Under the Hood: The Scraper Engine

The core of HackStreak is its data extraction engine (`ThmProfileScraper`). Since TryHackMe is a React SPA:
1. **Headless Execution**: A hidden `WebView` loads the public profile page.
2. **Polling**: An injected JavaScript payload begins polling the DOM, waiting up to 15 seconds for the `__NEXT_DATA__` JSON or the React component tree to fully render.
3. **Smart Extraction**: Uses a multi-layered fallback strategy (Line-by-line label matching -> Regex -> DOM traversal) to extract stats accurately regardless of minor CSS changes on the site.
4. **Memory Safe**: The WebView is dynamically created and destroyed to prevent memory leaks in the background.

## Tech Stack

- **UI**: Jetpack Compose (100% Kotlin UI)
- **Widgets**: Jetpack Glance
- **Local Storage**: Room (SQLite) for profile data, Preferences DataStore for user settings
- **Asynchronous Operations**: Kotlin Coroutines & Flow
- **Scraping**: `android.webkit.WebView` with custom `evaluateJavascript` payloads

## Download

Don't want to build it yourself? **[Download the latest .apk from GitHub Releases](https://github.com/cybersaad/HackStreak/releases)**! Just download, install, and you're good to go.

## Installation (For Developers)

1. **Clone the repository:**
   ```bash
   git clone https://github.com/YourUsername/HackStreak.git
   ```
2. **Open in Android Studio:**
   Open the project in Android Studio (Iguana or newer recommended).
3. **Sync & Build:**
   Allow Gradle to sync all dependencies.
4. **Run on Device:**
   Run the app on an emulator or a physical device running Android 8.0+.
5. **Add the Widget:**
   Long-press your Android home screen, navigate to Widgets, and drag the **HackStreak** widget to your screen.

## Contributing

**I welcome anyone to contribute!** Whether you want to fix a bug, improve the widget layout, or adapt the scraper if TryHackMe updates their DOM structure—your pull requests and ideas are highly appreciated. Feel free to open issues or submit PRs!

---
<div align="center">
  <i>Develop with Vibe. Keep your streak alive.</i><br>
  <b>© 2026 Saad Khan. All Rights Reserved.</b>
</div>
