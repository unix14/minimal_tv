# MinimalTV 📺⚡

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Android%20TV-green.svg)](https://developer.android.com/tv)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-21-orange.svg)](https://developer.android.com/about/dashboards)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34%2B-brightgreen.svg)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-purple.svg)](https://developer.android.com/topic/architecture)

A lightweight, blazingly fast, open-source Leanback-style launcher for Android TV, specifically engineered for low-spec hardware (such as Xiaomi TV / PatchWall boxes with 2GB–4GB RAM).

Built with **100% Kotlin**, **Coroutines**, **Flow**, and native **AndroidX Leanback** without Compose to ensure rock-solid 60fps rendering and minimal RAM footprint on budget SOCs.

---

## 🌟 Key Features

- **🚀 Blazingly Fast & Lightweight:** Minimal memory footprint, zero lag, smooth 60fps scrolling on budget TV boxes.
- **✨ Apple TV / Netflix Style Focus:** Subtle `1.08x` scale-up animation with gentle elevation and border glow.
- **🕒 Top Bar Header:** Dynamic live time & date clock, one-click access to Android TV System Settings and MinimalTV Settings.
- **🌐 Independent In-App Language Toggle:** Switch between English and Hebrew (עברית) with immediate per-app locale and full RTL support without altering the global system language.
- **📦 Installed TV & Leanback Apps Swimlane:** Fast asynchronous discovery of all Leanback and standard TV applications with zero-lag in-memory icon caching.
- **⭐ Pin to Favorites:** Pin frequently-used apps into a dedicated top swimlane.
- **🎬 Android TV Recommendations / Preview Channels:** Full support for `TvContractCompat` / Preview Channels & Programs API with per-channel visibility toggles.
- **🎮 Live D-Pad Reordering & Management:** Long-press D-Pad Center on any card to move left/right, pin/unpin favorites, inspect app details, or uninstall.
- **🛡️ Xiaomi TV / PatchWall Interception:** Includes a lightweight `AccessibilityService` that intercepts `KEYCODE_HOME` to guarantee MinimalTV launches even if PatchWall attempts to override your default launcher.

---

## 📸 Screenshots

*(Screenshots placeholder — Add UI captures of MinimalTV running on Xiaomi TV / Android TV)*

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.0+
- **Framework:** AndroidX Leanback (`BrowseSupportFragment` / `RowsSupportFragment`)
- **Architecture:** Clean Architecture + MVVM with Repository Pattern
- **Persistence:** Jetpack DataStore Preferences
- **Asynchrony:** Coroutines & StateFlow
- **Minimum SDK:** Android 5.0 (API 21)
- **Target SDK:** Android 14+ (API 34+)

---

## 📥 Installation

### 1. Direct ADB Install
Download the latest APK from GitHub Releases and install over your local Wi-Fi:

```bash
adb connect <tv-ip-address>:5555
adb install -r minimal_tv.apk
```

### 2. Set as Default Launcher
Press the **Home** button on your TV remote and choose **MinimalTV** -> **Always**.

---

## 🔧 Disabling Xiaomi PatchWall (Recommended for Xiaomi TVs)

Xiaomi's PatchWall OS sometimes attempts to hijack the Home button. To permanently disable PatchWall bloatware and free up RAM, run these commands via ADB:

```bash
# 1. Connect to TV
adb connect <tv-ip-address>:5555

# 2. Disable Xiaomi PatchWall launcher
adb shell pm disable-user --user 0 com.mitv.tvhome

# 3. (Optional) Disable Xiaomi analytics and push services
adb shell pm disable-user --user 0 com.xiaomi.mitv.tvpush.tvpushservice
adb shell pm disable-user --user 0 com.mitv.videoplayer

# 4. If you ever want to re-enable PatchWall:
adb shell pm enable com.mitv.tvhome
```

---

## 🤝 Contributing

Contributions, feature requests, and bug reports are welcome!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.
