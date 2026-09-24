Build a production-ready Android app for discovering Fortnite Creative / UEFN island codes.

## 1. Main goal

The app is a gallery/catalog of Fortnite island codes.

The monetization goal is to maximize AdMob revenue **while strictly complying with Google Play Developer Policies, Google AdMob policies, and Google UMP consent requirements**.

The app must NOT use deceptive ads, accidental clicks, forced ad interactions, or misleading UI.

Core UX:

* Browse island thumbnails in a gallery.
* See island name/title.
* Like/favorite islands.
* Browse by categories.
* Search islands.
* Tap an island to see its details.
* The island code is initially locked.
* User can voluntarily watch a rewarded ad to unlock the code.
* After the rewarded ad is successfully completed, reveal the island code.
* The unlocked state must persist permanently on the device.
* User can copy the code.
* User can share the code using Android's native share sheet.

The reward must be clearly explained before the rewarded ad:

"Watch a rewarded ad to unlock this island code."

Do NOT automatically show rewarded ads.

Do NOT require users to watch an ad merely to browse the app.

Skipping/dismissing the rewarded ad must not prevent normal browsing.

---

# 2. Data source

Load island data remotely from:

https://raw.githubusercontent.com/susantohenri/creative-island-hub/refs/heads/main/content/data.json

The JSON currently contains fields such as:

* code
* title
* tags
* category
* creatorCode
* createdIn

Do not hard-code island data into the application.

Create a clean data model that tolerates missing optional fields.

Use:

* `code` as the unique island identifier.
* `title` as the display name.
* `tags` for filtering/search/category discovery.
* `category` when available.

If `category` is missing, do not crash.

---

# 3. Thumbnail URL

Construct thumbnail URLs dynamically:

https://raw.githubusercontent.com/susantohenri/creative-island-hub/refs/heads/main/content/thumbnails/[island-code].webp

Example:

https://raw.githubusercontent.com/susantohenri/creative-island-hub/refs/heads/main/content/thumbnails/9266-8386-7240.webp

Use the island `code` dynamically.

Use proper image loading/caching.

If a thumbnail fails:

* show a neutral placeholder
* do not crash
* keep the island item usable

---

# 4. Screens

Create these main sections:

## Home

Gallery of islands.

Each card should contain:

* thumbnail
* island title
* Like button
* category/tag information when available
* locked/unlocked state
* "Watch Ad to Unlock Code" button when locked
* island code when unlocked
* Copy button when unlocked
* Share button when unlocked

Add search.

Add useful sorting/filtering where appropriate.

The gallery should be visually attractive and optimized for fast scrolling.

---

## Liked

Show islands that the user has liked.

Like state must persist locally.

If there are no liked islands, show a friendly empty state.

---

## Categories

Show available categories/tags derived from the downloaded data.

Selecting a category displays matching islands.

Do not assume every island has a `category` field.

Use tags as a fallback where appropriate.

---

## Settings

### Preferences

* Language:

  * English
  * Bahasa Indonesia

* Theme:

  * Light
  * Dark

Default language:

Automatically detect the Android operating system language.

If the system language is Indonesian, default to Bahasa Indonesia.

Otherwise default to English.

Allow the user to manually change the language.

Persist the user's selection.

Default theme should follow the Android system theme if appropriate.

Allow manual Light/Dark selection.

Persist the user's selection.

---

### Legal & Info

Add:

* About
* App version
* Privacy Policy

Privacy Policy button must open this external URL:

https://tokiocv.blogspot.com/2026/07/privacy-policy.html

Use Android's standard browser intent.

Also include a short disclaimer:

"This app is an independent fan-made utility and is not affiliated with, endorsed by, or sponsored by Epic Games."

Do not imply official affiliation with Fortnite or Epic Games.

---

# 5. AdMob

Use Google Mobile Ads SDK.

Ensure the dependency exists in `build.gradle.kts`:

`com.google.android.gms:play-services-ads`

Use the Google Sample App ID during development:

`ca-app-pub-3940256099942544~3347511713`

Add it correctly to `AndroidManifest.xml`.

Do not hard-code production AdMob IDs into source code if they can be configured remotely.

---

# 6. Remote ads_config.json

Load the ad configuration remotely from:

https://raw.githubusercontent.com/susantohenri/admob-remote-configs/refs/heads/main/creativeIslandHub/ads_config.json

Create a robust configuration system.

The configuration should be able to control at minimum:

ads enabled/disabled
rewarded ad unit ID
banner ad unit ID
optional frequency/cooldown settings if needed

If remote configuration fails:

app must continue working
do not crash
use safe fallback defaults

Never let remote configuration break the main app.

IMPORTANT:

Use Google's official test ad IDs during development/testing.

Never use production ad IDs for testing.

---

# 7. Rewarded ad implementation

Implement rewarded ads correctly according to Google AdMob policy.

Flow:

1. User sees locked island.
2. User taps "Watch Ad to Unlock Code".
3. Show a clear confirmation/disclosure that watching the ad will unlock that island's code.
4. User explicitly chooses to continue.
5. Load/show rewarded ad.
6. Only grant the unlock after the rewarded ad's reward callback is successfully received.
7. Save the island code as permanently unlocked locally.
8. Reveal the island code.
9. Allow Copy and Share.

Do NOT unlock the code merely because the ad was loaded.

Do NOT unlock the code merely because the ad was opened.

Unlock only after the reward event is received.

If the user closes/skips the ad before earning the reward:

* do not unlock the code
* return to the island card normally
* do not punish the user
* do not repeatedly force another ad

The user must always understand what they receive for watching the ad.

Do not use misleading text such as:

* "Support us by watching"
* "You must watch this"
* "Continue" when the real action is watching an ad

Use clear language such as:

"Watch Ad to Unlock Code"

---

# 8. Persistent unlocks

Use Android DataStore (preferred) or another reliable local persistence mechanism.

Store unlocked island codes using the island code as the key.

Example concept:

unlockedIslandCodes = [
"9266-8386-7240",
"5739-6288-4476"
]

Once unlocked, the island remains unlocked across:

* app restart
* activity recreation
* configuration changes

No account/login/backend is required.

---

# 9. Likes

Persist liked island codes locally.

Users can:

* like/unlike an island
* view liked islands from the Liked screen

Do not require login.

---

# 10. Copy

When an island is unlocked:

Show the island code prominently.

Provide a Copy button.

Copy only the actual island code to the clipboard.

After copying, show a short confirmation such as:

"Island code copied"

Do not automatically copy anything without user action.

---

# 11. Share

When unlocked, provide Share.

Use Android's native share intent.

Suggested shared text:

`[Island Title]`

`Island Code: [CODE]`

Do not launch external apps automatically.

Let Android's standard share sheet handle the destination.

---

# 12. Google UMP / consent

Integrate the official Google User Messaging Platform SDK.

Use UMP to handle consent requirements for personalized/non-personalized advertising.

Consent flow must be initialized appropriately before requesting ads where required.

Respect the user's consent choice.

Do not attempt to bypass or manipulate the consent dialog.

Provide the appropriate privacy/consent options according to Google's current UMP implementation.

Make sure the implementation works correctly for users in regions where consent is required and for users outside those regions.

---

# 13. Ads UX / monetization

The monetization strategy must prioritize policy compliance and good user experience.

Recommended ad placements:

### Rewarded

Primary monetization:

User voluntarily watches a rewarded ad to unlock an island code.

One successful reward permanently unlocks that island.

### Banner

A non-intrusive banner may be displayed on appropriate browsing screens such as:

* Home
* Categories
* Liked

Do NOT place banners:

* over buttons
* over island thumbnails
* immediately adjacent to controls in a way that encourages accidental clicks
* where they can be mistaken for app navigation

Avoid excessive ad density.

Do not implement deceptive ad placements.

Do not create artificial clicks.

Do not repeatedly refresh ads unnecessarily.

Do not use ads that interfere with normal navigation.

---

# 14. Search

Implement local search over downloaded data.

Search should match:

* title
* island code
* creator code
* tags
* category

Search should be case-insensitive.

Use a sensible debounce so typing does not cause excessive processing.

---

# 15. Network/data handling

The app loads the latest island data from the remote GitHub JSON:

https://raw.githubusercontent.com/susantohenri/creative-island-hub/refs/heads/main/content/data.json

No offline mode is required.

If the data cannot be loaded:

show a clear error state
provide a Retry button
do not crash

Do NOT implement offline data caching or offline browsing.

---

# 16. Image caching

Use a proper Android image-loading library such as Coil.

Requirements:

* memory/disk caching
* efficient lazy loading
* placeholder
* error image
* avoid downloading the same thumbnail repeatedly

The gallery must remain smooth while scrolling.

---

# 17. UI/UX

Use modern Material 3 design.

The app should feel like a polished gaming discovery/gallery application.

Prioritize:

* large thumbnails
* clear titles
* obvious locked/unlocked state
* readable island codes
* fast scrolling
* responsive touch targets
* dark mode
* light mode

Use responsive layouts for different Android screen sizes.

Avoid clutter.

---

# 18. Navigation

Use a bottom navigation bar:

* Home
* Liked
* Categories
* Settings

Maintain navigation state appropriately.

The main content should not unexpectedly reset when switching tabs.

---

# 19. Loading/error states

Every remote operation must have proper states:

* loading
* success
* empty
* error
* retry

Do not show blank screens.

Do not crash because of:

* malformed optional JSON fields
* missing thumbnail
* GitHub unavailable
* invalid remote ads configuration
* unavailable ad
* user closing rewarded ad
* failed image download

---

# 20. App icon

Generate a suitable original app icon.

Requirements:

* square Android launcher icon
* recognizable at small sizes
* gaming/island-code discovery theme
* do NOT copy Fortnite/Epic Games logos or copyrighted branding
* do not use Fortnite's official logo
* create an original visual identity

Generate all required Android launcher icon assets/adaptive icon configuration.

---

# 21. Internationalization

All user-facing strings must use Android string resources.

Languages:

* English
* Bahasa Indonesia

Do not hard-code UI strings directly into Kotlin/Compose code.

Translate:

* navigation
* buttons
* dialogs
* settings
* errors
* empty states
* ad disclosures
* copy/share messages
* about page
* privacy-related text

Island titles and remote content should NOT be automatically translated.

---

# 22. Accessibility

Implement:

* meaningful content descriptions
* sufficient touch target sizes
* readable text
* good contrast
* screen-reader-friendly controls
* buttons with clear labels

---

# 23. Privacy / Play Store compliance

Before considering the app finished, perform a compliance review.

The app must:

* use HTTPS for network requests
* use only permissions actually required
* avoid unnecessary sensitive permissions
* include Privacy Policy access
* correctly integrate AdMob
* correctly integrate UMP
* correctly handle advertising consent
* correctly declare ads in Google Play Console
* ensure Data Safety declarations can accurately reflect actual SDK/data behavior
* not collect unnecessary personal information
* not misrepresent itself as an official Fortnite/Epic Games application

Do not add unnecessary analytics, tracking SDKs, login systems, or permissions unless explicitly required.

Google Play requires accurate disclosure of data practices, including those introduced by third-party SDKs.

---

# 24. Technical requirements

Use modern Android development practices.

Prefer:

* Kotlin
* Jetpack Compose
* Material 3
* Navigation Compose
* ViewModel
* Coroutines
* Kotlin Serialization or another robust JSON parser
* DataStore
* Coil
* Google Mobile Ads SDK
* Google UMP SDK

Use the latest stable versions that are compatible with the current Android/Gradle environment.

Ensure the project targets the current Google Play required API level.

Google Play's current requirement is that apps meet the latest target API level requirements by August 31, 2026, so verify the project against the current requirement rather than assuming an old target SDK.

---

# 25. Architecture

Keep the code maintainable.

Suggested layers:

* data
* network
* repository
* domain/model
* UI
* ads
* preferences
* navigation

Create dedicated components/classes for:

* IslandRepository
* RemoteConfigRepository
* RewardedAdManager
* ConsentManager
* Preferences/DataStore
* image loading
* navigation

Do not put all logic inside Activities or Composables.

---

# 26. Security / robustness

Never execute code received from the remote JSON.

Treat GitHub JSON and remote ads configuration as untrusted external data.

Validate:

* island code format
* URLs
* nullable fields
* remote configuration values

Do not allow malformed remote data to crash the app.

---

# 27. Important legal/content consideration

This app is an independent third-party utility.

Do not claim:

* official Fortnite app
* official Epic Games app
* endorsed by Epic Games
* affiliated with Epic Games

Use an appropriate disclaimer in About.

Do not copy Fortnite/Epic trademarks into the app icon or branding.

---

# 28. Final implementation checklist

Before finishing, verify all of these:

[ ] App builds successfully.

[ ] `com.google.android.gms:play-services-ads` is present in `build.gradle.kts`.

[ ] Sample AdMob App ID is present in AndroidManifest.xml during development:
`ca-app-pub-3940256099942544~3347511713`

[ ] UMP SDK is integrated.

[ ] Consent flow works.

[ ] Remote ads_config.json works.

[ ] App survives ads_config.json failure.

[ ] Rewarded ad is explicitly opt-in.

[ ] Reward disclosure is shown before rewarded ad.

[ ] Island code is unlocked only after reward callback.

[ ] Unlocked islands remain unlocked permanently on the device.

[ ] Likes persist.

[ ] Copy works.

[ ] Share works.

[ ] Home works.

[ ] Liked works.

[ ] Categories work.

[ ] Settings works.

[ ] English works.

[ ] Bahasa Indonesia works.

[ ] Automatic OS language detection works.

[ ] Light mode works.

[ ] Dark mode works.

[ ] Search works.

[ ] Offline/cache fallback works.

[ ] Thumbnail errors are handled.

[ ] GitHub data errors are handled.

[ ] App icon is generated.

[ ] No unnecessary Android permissions are requested.

[ ] No deceptive ad placement exists.

[ ] No automatic rewarded ads exist.

[ ] No accidental-click ad placement exists.

[ ] Privacy Policy opens:
https://tokiocv.blogspot.com/2026/07/privacy-policy.html

[ ] About contains app version.

[ ] About contains third-party/non-affiliation disclaimer.

[ ] All user-facing strings use Android localization resources.

[ ] Release build works.

[ ] ProGuard/R8 configuration is checked if minification is enabled.

[ ] No debug/test logging or test ad configuration accidentally ships in production.

Finally, perform a code review specifically for Google Play and AdMob compliance and fix any issue you find before declaring the project complete.
