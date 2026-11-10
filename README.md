# Product Requirements Document (PRD) & System Specification
## Creative Island Hub (Android)

> **LLM Context Note**: This document serves as the single source of truth (SSOT) and comprehensive Product Requirements Document (PRD) for **Creative Island Hub**. You can copy-paste this entire document into any LLM prompt whenever you need it to understand the architecture, data contracts, monetization rules, compliance guidelines, and code structure of this application.

---

## 1. Executive Summary & App Metadata

* **Application Name**: Creative Island Hub
* **Package Name**: `com.henrisusanto.creativeislandhub`
* **Google Play Store URL**: https://play.google.com/store/apps/details?id=com.henrisusanto.creativeislandhub
* **Target Platform**: Android (Minimum SDK: 26 / Android 8.0 Oreo, Target SDK: 36, Compile SDK: 36)
* **Current Version**: 1.4.0 (Version Code: 5)
* **Application Category**: Gaming Utility / Catalog & Discovery
* **Privacy Policy URL**: https://susantohenri.github.io/privacy-policy
* **Firebase Project ID**: `tempat-sambung` (`215024871022`)

### Core Mission
Creative Island Hub is a lightweight, fan-made Android discovery utility and catalog for **Fortnite Creative & UEFN (Unreal Editor for Fortnite) island codes**. It enables players to browse, search, filter, and discover community-created islands. Island codes are initially locked behind an ethical, voluntary, policy-compliant rewarded ad mechanic. Once unlocked, the island code remains permanently accessible on the user's device without requiring user accounts or logins.

### Community & Inspiration
* **r/TheFortniteCreatives** — [https://www.reddit.com/r/TheFortniteCreatives/](https://www.reddit.com/r/TheFortniteCreatives/)
  * Primary community hub for Fortnite Creative & UEFN creators
  * Source of island discovery, feedback, and promotional inspiration
  * Community-driven showcase of creative maps and game modes

---

## 2. Legal Positioning & Store Policy Compliance

### 2.1 Fan-Made Disclaimer & Trademark Non-Affiliation
* **Policy Compliance**: Strictly complies with Google Play Intellectual Property and Impersonation policies as well as Epic Games Fan Content guidelines.
* **Branding Rules**:
  * Never use official Epic Games or Fortnite logos, trademarks, or copyrighted keyart in the app icon, splash screen, or promotional material.
  * The app utilizes custom, original vector launcher icons (`ic_launcher`).
* **Mandatory Legal Disclaimer** (Displayed prominently in the Settings / About screen):
  > *"This app is an independent fan-made utility and is not affiliated with, endorsed by, or sponsored by Epic Games."*

### 2.2 Google Play & AdMob Policy Adherence
* **Zero Deceptive Placements**: No disguised buttons, no misleading clickable elements, no ads placed on top of interactive UI controls.
* **No Accidental Clicks**: Generous padding around banners; banner ads are placed at the bottom safe area separated by distinct surface boundaries.
* **Voluntary Rewarded Ads**: Rewarded ads are **never** triggered automatically. Users are never forced to watch ads to browse the catalog.
* **Prior Disclosure**: Before triggering a rewarded ad, an explicit dialog is presented:
  > *"Watch a rewarded ad to unlock this island code."*
* **Earned Reward Guarantee**: Island codes are **only** unlocked when the AdMob SDK fires the `OnUserEarnedRewardListener` callback. If the user dismisses or skips the ad early, the code remains locked without penalizing the user or preventing normal browsing.
* **Permanent Local Unlocks**: Once an island code is unlocked, it is permanently persisted locally on the device (survives app restarts and navigation).

---

## 3. Architecture & Tech Stack

The application adheres to modern Android architecture principles: **MVVM (Model-View-ViewModel)** with **Clean Architecture separation**, unidirectional data flow (UDF), and **Koin** dependency injection.

```
                          ┌────────────────────────┐
                          │    Remote Endpoints    │
                          │ (GitHub JSON & WebP)   │
                          └───────────┬────────────┘
                                      │
                                      ▼
                          ┌────────────────────────┐
                          │   Firebase Services    │
                          │(RemoteConfig/Analytics)│
                          └───────────┬────────────┘
                                      │
                                      ▼
┌────────────────────────┐ ┌──────────────────────┐ ┌────────────────────────┐
│  PreferencesDataStore  │ │   RemoteDataSource   │ │       AdManager        │
│ (Local Jetpack DS)     │ │   (OkHttp + JSON)    │ │   (UMP & AdMob SDK)    │
└───────────┬────────────┘ └──────────┬───────────┘ └───────────┬────────────┘
            │                         │                         │
            └────────────────┐        │        ┌────────────────┘
                             ▼        ▼        ▼
                          ┌────────────────────────┐
                          │    IslandRepository    │
                          └───────────┬────────────┘
                                      │
                                      ▼
                          ┌────────────────────────┐
                          │     MainViewModel      │
                          └───────────┬────────────┘
                                      │
                                      ▼
                          ┌────────────────────────┐
                          │ Jetpack Compose UI/M3  │
                          │ (Screens & Components) │
                          └────────────────────────┘
```

### Technology Matrix
* **Language**: Kotlin 2.x (JVM target: 11)
* **UI Framework**: Jetpack Compose with Material Design 3 (M3)
* **Navigation**: Jetpack Navigation Compose (`NavHost`, bottom bar destination routing)
* **Dependency Injection**: Koin (`koin-androidx-compose`)
* **Asynchronous & Reactive**: Kotlin Coroutines & StateFlow / SharedFlow
* **Networking**: OkHttp 4.x + Kotlinx Serialization (JSON parser with `ignoreUnknownKeys = true`)
* **Local Persistence**: Jetpack Preferences DataStore (`androidx.datastore:datastore-preferences`)
* **Image Loading & Caching**: Coil Compose (`coil-kt:coil-compose`) with disk/memory caching and fallback placeholders
* **Monetization**: Google Mobile Ads SDK (`play-services-ads:23.x+`) + Google User Messaging Platform (`ump:2.x+`)
* **Cloud & Telemetry**: Firebase BoM (Firebase Analytics, Firebase Crashlytics, Firebase Remote Config)

---

## 4. Data Sources & Remote Contracts

### 4.1 Primary Remote Catalog JSON
* **URL**: `https://raw.githubusercontent.com/susantohenri/creative-island-hub/refs/heads/main/content/data.json`
* **Network Behavior**: Fetched on application startup. Shuffled on load (`response.data.shuffled()`) to provide fresh discovery on every launch.
* **Failure Handling**: If the network request fails, displays a user-friendly error screen with a "Retry" button. No blank screens or unhandled crashes.

#### JSON Data Contract
```json
{
  "_pagination": {
    "next": "/ecosystem/v1/islands?after=...&size=250"
  },
  "data": [
    {
      "code": "0961-9832-4970",
      "title": "📦CLASSIC BOXFIGHTS PVP📦",
      "tags": ["battle", "pvp", "boxfight", "fighting"],
      "category": "PVP",
      "creatorCode": "kafson",
      "createdIn": "FNC",
      "cursor": "MDk2MS05ODMyLTQ5NzA="
    }
  ]
}
```

#### Field Specifications:
| Field | Type | Required | Description |
|---|---|---|---|
| `code` | `String` | Yes | Unique Fortnite island identifier (format: `XXXX-XXXX-XXXX`). |
| `title` | `String` | Yes | Display name of the island. |
| `tags` | `List<String>` | No (default: `[]`) | Taxonomy tags used for search, filtering, and category chips. |
| `category` | `String?` | No (nullable) | Optional high-level category name (e.g. `TMNT`, `Star Wars`). |
| `creatorCode` | `String?` | No (nullable) | Creator's Epic Support-A-Creator code. |
| `createdIn` | `String?` | No (nullable) | Engine tag (`FNC` for Creative 1.0, `UEFN` for Creative 2.0). |

### 4.2 Dynamic Thumbnail URLs
* **URL Pattern**: `https://raw.githubusercontent.com/susantohenri/creative-island-hub/refs/heads/main/content/thumbnails/[code].webp`
* **Example**: `https://raw.githubusercontent.com/susantohenri/creative-island-hub/refs/heads/main/content/thumbnails/0961-9832-4970.webp`
* **Handling**: Rendered via Coil `AsyncImage` with crossfade, placeholder drawable (`ic_thumbnail_placeholder`), and error drawable (`ic_thumbnail_error`).

### 4.3 Firebase Remote Config (Ad Configuration)
Remote parameters control ad units dynamically without requiring Play Store releases. Fetched via `FirebaseRemoteConfig` with `fetchAndActivate()`.

| Parameter Key | Type | Default Value | Description |
|---|---|---|---|
| `is_ads_enabled` | `Boolean` | `true` | Master kill-switch for all ad formats. |
| `rewarded_ad_unit_id` | `String` | `ca-app-pub-3360913653749022/3278549739` | Rewarded Ad Unit ID for island code unlocks. |
| `banner_ad_unit_id` | `String` | `ca-app-pub-3360913653749022/1199307619` | Adaptive Banner Ad Unit ID. |
| `interstitial_ad_unit_id`| `String` | `ca-app-pub-3360913653749022/8071089297` | Reserved for future interstitial placements. |
| `app_open_ad_unit_id` | `String` | `ca-app-pub-3360913653749022/7252831641` | Reserved for App Open placements. |
| `native_ad_unit_id` | `String` | `ca-app-pub-3360913653749022/2747444572` | Reserved for Native Ad placements. |
| `ads_config` | `String` (JSON) | `{}` | Optional JSON blob override for all ad configs. |

* **Fallback Guarantee**: If Firebase Remote Config is unreachable or fails to parse, the app falls back to `AdsConfig.DEFAULT` without crashing.

---

## 5. Local Persistence & State Management

All local preferences are managed through **Jetpack Preferences DataStore** (`settings.preferences_pb`):

1. **`unlocked_islands`** (`Set<String>`):
   - Stores the list of `code` strings for unlocked islands.
   - Permanent: Unlocked islands remain unlocked indefinitely across sessions, app updates, and configuration changes.
2. **`liked_islands`** (`Set<String>`):
   - Stores the list of `code` strings favorited/liked by the user.
   - Synced with the Liked screen.
3. **`theme_mode`** (`String`):
   - Value options: `"SYSTEM"` (default), `"LIGHT"`, `"DARK"`.
4. **`language`** (`String`):
   - Value options: `"AUTO"` (system default), `"EN"` (English), `"ID"` (Bahasa Indonesia).

---

## 6. Screens & User Journey

### 6.1 Home Screen (`HomeScreen.kt`)
* **Feed Display**: Dynamic, scrollable `LazyColumn` of `IslandCard`s. Content is randomized on launch for fresh discovery.
* **Debounced Search**:
  * Real-time search bar with 300ms debounce.
  * Case-insensitive matching across `title`, `code`, `creatorCode`, `category`, and `tags`.
  * Logs a Firebase search event when query length is >= 3 characters.
* **Active Filter Indicator**: Shows active category/tag filter pill with a clear button.
* **Banner Ad Placement**: Adaptive anchored banner at the bottom of the screen with proper spacing.
* **Loading & Error States**: Displays `CircularProgressIndicator` during loading; provides error text and a "Retry" button on failure.

### 6.2 Liked Screen (`LikedScreen.kt`)
* **Favorites Feed**: Displays islands whose `code` exists in `likedIslands`.
* **Empty State**: Friendly illustration and message encouraging the user to favorite islands from the Home feed.
* **Banner Ad Placement**: Adaptive anchored banner at the bottom.

### 6.3 Categories Screen (`CategoriesScreen.kt`)
* **Taxonomy Engine**: Dynamically harvests unique categories and tags from the downloaded JSON list, eliminates blank/duplicate entries, and sorts them alphabetically.
* **Navigation Action**: Tapping a category navigates to the filtered Home feed showing only matching islands.

### 6.4 Settings Screen (`SettingsScreen.kt`)
* **Preferences**:
  * **Theme**: Dialog selector for System Default, Light, or Dark mode.
  * **Language**: Dialog selector for Auto (OS detection), English, or Bahasa Indonesia. Uses `AppCompatDelegate.setApplicationLocales` for immediate runtime locale switching.
* **Legal & Information**:
  * **Privacy Policy**: Opens `https://susantohenri.github.io/privacy-policy` via Android native Chrome Custom Tabs / Browser Intent.
  * **Version & Build**: Displays `BuildConfig.VERSION_NAME` (`1.4.0`).
  * **Third-Party Disclaimer**: Displays non-affiliation disclaimer.
  * **Debug Tool**: In debug builds, tapping the version string triggers a test crash to verify Firebase Crashlytics logging.

---

## 7. Component Deep-Dive: Island Card (`IslandCard.kt`)

The `IslandCard` is the primary interactive element of the app.

```
┌────────────────────────────────────────────────────────┐
│  [🔒 LOCKED] / [✅ UNLOCKED]              [🤍/❤️ LIKE] │
│                                                        │
│               16:9 Island Thumbnail                    │
│            (Coil AsyncImage with Cache)                │
│                                                        │
├────────────────────────────────────────────────────────┤
│ Island Title (Bold, 18sp)                              │
│ Creator: creatorCode | Engine: createdIn               │
│ [Tag Chip 1] [Tag Chip 2] [Tag Chip 3] ...            │
├────────────────────────────────────────────────────────┤
│ IF LOCKED:                                             │
│   [ 🎬 Watch Ad to Unlock Code ]                       │
│                                                        │
│ IF UNLOCKED:                                           │
│   ┌────────────────────────────────────────────────┐   │
│   │ CODE: 0961-9832-4970                           │   │
│   │ [📋 Copy Code]             [🔗 Share Code]     │   │
│   └────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

### Card State Lifecycle:
1. **Initial State (Locked)**:
   * Card displays title, creator, tags, and a prominent badge indicating "LOCKED".
   * Action button displays: **"Watch Ad to Unlock Code"** with play icon.
2. **Unlock Flow**:
   * User taps unlock button $\rightarrow$ Disclosure dialog appears: *"Watch a rewarded ad to unlock this island code."*
   * User confirms $\rightarrow$ `AdManager.showRewardedAd(activity, onUserEarnedReward = { ... })`.
   * On reward earned $\rightarrow$ `PreferencesDataStore.unlockIsland(code)` is called $\rightarrow$ Card transitions to Unlocked state with a celebration feedback.
   * If ad is closed before completion $\rightarrow$ Dialog closes, no unlock is granted, no error penalty.
3. **Unlocked State**:
   * Badge switches to "UNLOCKED" in green.
   * Island code is prominently revealed in a high-contrast container.
   * Action buttons:
     * **Copy Code**: Copies the raw code (`XXXX-XXXX-XXXX`) to the Android clipboard, shows a toast notification, and logs `copy_code` analytics.
     * **Share Code**: Opens Android's native share sheet with formatted text:
       ```
       [Island Title]
       Island Code: [CODE]
       ```

---

## 8. Monetization & Ad Integration Architecture

### 8.1 Google UMP (User Messaging Platform) Consent
1. Initialized in `MainActivity.onCreate()` via `AdManager.requestConsentAndInit()`.
2. Requests consent update via `UserMessagingPlatform.getConsentInformation(activity)`.
3. Loads and displays the consent form if required (GDPR / UK / EEA compliance).
4. Calls `MobileAds.initialize(context)` only when `consentInformation.canRequestAds()` is true.

### 8.2 Ad Unit Configuration
* **AdMob Application ID**: `ca-app-pub-3360913653749022~3924794879` (Declared in `AndroidManifest.xml`)
* **Ad Units (Production)**:
  * Banner: `ca-app-pub-3360913653749022/1199307619`
  * Rewarded: `ca-app-pub-3360913653749022/3278549739`
* **Sample / Test Ad Units** (Used in development & automated test suites):
  * Sample App ID: `ca-app-pub-3940256099942544~3347511713`
  * Sample Rewarded ID: `ca-app-pub-3940256099942544/5224354917`
  * Sample Banner ID: `ca-app-pub-3940256099942544/6300978111`

### 8.3 Revenue & Paid Event Telemetry
`AdManager` attaches `OnPaidEventListener` to both Banner and Rewarded ads, capturing:
* Revenue value in micro-units (`valueMicros`).
* Currency code (e.g. `USD`).
* Precision type.
* AdMob Ad Unit ID and format.
* Forwards paid impressions directly to Firebase Analytics `ad_impression` event.

---

## 9. Firebase Telemetry & Instrumentation

Firebase is connected via Google Play Console linking (Project: `tempat-sambung`, Number: `215024871022`).

### 9.1 Analytics Events Registry
| Event Name | Parameters | Trigger Condition |
|---|---|---|
| `screen_view` | `screen_name`, `screen_class` | Screen navigation (protected against duplicate firing). |
| `unlock_code` | `island_code`, `island_title` | User successfully earns reward and unlocks an island code. |
| `copy_code` | `island_code` | User taps the "Copy Code" button. |
| `share` | `content_type`, `item_id` | User taps the "Share Code" button. |
| `toggle_like` | `island_code`, `status` (`liked`/`unliked`) | User favorites or un-favorites an island. |
| `search` | `search_term` | User searches with a query $\ge 3$ characters (debounced). |
| `select_category` | `category_name` | User selects a category or tag chip. |
| `ad_request` | `ad_unit_id`, `ad_format`, `screen` | AdMob ad request initiated. |
| `ad_impression` | `ad_unit_id`, `ad_format`, `currency`, `value` | Ad rendered & impression recorded via `OnPaidEventListener`. |
| `ad_click` | `ad_unit_id`, `ad_format`, `screen` | User clicks an ad banner or rewarded ad. |
| `ad_failed` | `ad_unit_id`, `ad_format`, `error_code`, `message` | Ad fails to load or fail to display. |

### 9.2 User Properties
* `language`: Current active language (`en`, `in`).
* `country`: Country ISO code derived from telephony / system locale.
* `engagement_level`: Calculated user engagement (`new`, `active`, `dormant`) cached locally to prevent redundant network transmissions.

### 9.3 Crashlytics
* Automatically enabled in release builds.
* Custom keys: `app_version` set on initialization.
* Native symbol and ProGuard mapping file upload enabled via `com.google.firebase.crashlytics` Gradle plugin.

---

## 10. Codebase Structure & File Mapping

```
creative-island-hub/
├── app/
│   ├── build.gradle.kts                # App dependencies, SDK targets, plugins, ProGuard
│   ├── proguard-rules.pro              # ProGuard/R8 obfuscation & keep rules
│   └── src/main/
│       ├── AndroidManifest.xml         # Permissions, AdMob App ID, Application & Activity
│       ├── java/com/henrisusanto/creativeislandhub/
│       │   ├── CreativeIslandHubApplication.kt # Koin & Firebase initialization
│       │   ├── MainActivity.kt         # Entry Activity, UMP consent init, Compose NavHost
│       │   ├── ads/
│       │   │   └── AdManager.kt        # UMP consent, AdMob Rewarded ad lifecycle, Paid listener
│       │   ├── analytics/
│       │   │   └── AnalyticsLogger.kt  # Firebase Analytics & Crashlytics event dispatchers
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   └── PreferencesDataStore.kt # DataStore for unlocks, likes, theme, lang
│       │   │   ├── model/
│       │   │   │   ├── Island.kt       # Island & IslandDataResponse data models
│       │   │   │   └── AdsConfig.kt    # Remote config data model & defaults
│       │   │   ├── network/
│       │   │   │   └── RemoteDataSource.kt # OkHttp JSON fetcher & Firebase Remote Config
│       │   │   └── repository/
│       │   │       └── IslandRepository.kt # Central repository unifying remote & local data
│       │   ├── di/
│       │   │   └── AppModule.kt        # Koin singletons and ViewModel factory
│       │   └── ui/
│       │       ├── components/
│       │       │   ├── IslandCard.kt   # Collapsible island card, badges, copy/share actions
│       │       │   └── BannerAdView.kt # Anchored adaptive AdMob banner composable
│       │       ├── screens/
│       │       │   ├── HomeScreen.kt   # Catalog feed, debounced search, unlock dialog
│       │       │   ├── LikedScreen.kt  # User favorites list & empty state
│       │       │   ├── CategoriesScreen.kt # Dynamic tag/category taxonomy grid
│       │       │   └── SettingsScreen.kt # Theme, language, privacy policy, legal disclaimer
│       │       ├── theme/
│       │       │   └── Theme.kt        # Material 3 light/dark color schemes & typography
│       │       └── viewmodel/
│       │           └── MainViewModel.kt # StateFlows, debounced filtering, data operations
│       └── res/
│           ├── values/                 # English string resources, colors, themes
│           │   └── strings.xml
│           ├── values-in/              # Indonesian string resources
│           │   └── strings.xml
│           └── xml/
│               ├── gma_ad_services_config.xml # AdServices configuration
│               └── backup_rules.xml
├── content/
│   ├── data.json                       # Master catalog JSON
│   └── thumbnails/                     # Thumbnail WebP image assets ([code].webp)
├── build.gradle.kts                    # Root build script
├── gradle/libs.versions.toml           # Version catalog for dependencies
└── README.md                           # This PRD & system specification
```

---

## 11. Internationalization (i18n) & Localization

* **Supported Languages**:
  1. English (`en`) - Default international fallback.
  2. Bahasa Indonesia (`in` / `id`) - Localized fallback.
* **Auto-Detection Logic**: On first run with `language == "AUTO"`, the app inspects the Android OS system language. If the locale starts with `in` or `id`, the app defaults to Bahasa Indonesia; otherwise, English is selected.
* **Resource Separation**: Zero hard-coded UI strings in Kotlin/Compose code. All strings reside in `res/values/strings.xml` and `res/values-in/strings.xml`. Remote catalog data (island titles) remains untouched in its original language.

---

## 12. Instructions & Rules for LLMs Modifying this Codebase

When assisting with this project, any LLM **must strictly follow these rules**:

1. **AdMob & Google Play Compliance is Sacred**:
   * Never implement automatic interstitial ads or auto-playing rewarded ads.
   * Never bypass or alter the UMP consent flow.
   * Never grant an island unlock without verifying that the AdMob `OnUserEarnedRewardListener` was called.
   * Never hide, obscure, or place ads directly over buttons or interactive thumbnails.
2. **Persistence Guarantee**:
   * Unlocked island codes and liked islands must always be saved to `PreferencesDataStore`.
   * Unlocks must be permanent on the device; do not introduce expiration timers unless explicitly instructed.
3. **Robust Data Parsing**:
   * The remote JSON schema may evolve. Always keep `ignoreUnknownKeys = true` and ensure all new fields in data classes are nullable or have sensible default values.
   * Missing thumbnails, invalid URLs, or empty categories must fail gracefully without crashing.
4. **Localization Requirement**:
   * Any new user-facing string added to Compose UI must be declared in both `res/values/strings.xml` and `res/values-in/strings.xml`.
5. **Architectural Purity**:
   * Keep business logic in `MainViewModel` or `IslandRepository`.
   * Do not make direct network or DataStore calls from Composable functions.
   * Register any new dependencies in `di/AppModule.kt`.
6. **Telemetry Preservation**:
   * Always log corresponding events in `AnalyticsLogger` when adding or modifying user interactions (e.g. unlocks, copies, shares, filters).

---

## 13. Firebase - Google Play Linking Reference

To maintain data synchronization between Firebase Analytics, Crashlytics, and Google Play Console:
1. Open [Firebase Console](https://console.firebase.google.com/) $\rightarrow$ Project **`tempat-sambung`** (`215024871022`).
2. Navigate to **Project Settings** $\rightarrow$ **Integrations** tab.
3. Select **Google Play** $\rightarrow$ **Link**.
4. Choose package `com.henrisusanto.creativeislandhub`.
5. Enable synchronization for:
   * **In-app purchase & revenue metrics**.
   * **Google Play app installation data**.
6. Save and allow up to 24 hours for complete historical synchronization.
