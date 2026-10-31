# creative-island-hub
https://play.google.com/store/apps/details?id=com.henrisusanto.creativeislandhub

---

## Firebase - Google Play Linking

Untuk menghubungkan Firebase Analytics & Crashlytics dengan Google Play Console agar data konversi, revenue in-app purchases, serta metrik stabilitas Play Console tersinkronisasi:

### Langkah-langkah Integrasi di Firebase Console:
1. Buka [Firebase Console](https://console.firebase.google.com/) dan pilih project **`tempat-sambung`** (`215024871022`).
2. Masuk ke **Project settings** (klik ikon gerigi di navigasi kiri atas).
3. Pilih tab **Integrations**.
4. Temukan kartu **Google Play**, kemudian klik tombol **Link** (atau **Manage** jika sudah ada link sebelumnya).
5. Pastikan akun Google yang Anda gunakan memiliki akses Owner atau Admin pada **Google Play Console** pengelola aplikasi `com.henrisusanto.creativeislandhub`.
6. Pilih package name aplikasi: `com.henrisusanto.creativeislandhub`.
7. Aktifkan sinkronisasi data:
   - **In-app purchase data & revenue metrics**.
   - **Google Play app installation data**.
8. Klik **Link** / **Simpan**.
9. Status integrasi akan aktif (biasanya memakan waktu hingga 24 jam untuk sinkronisasi penuh data riwayat).

---

## Firebase Instrumentation Overview

- **Analytics**: Basic events (`first_open`, `session_start`, `screen_view`), Core feature events (`unlock_code`, `copy_code`, `share`, `toggle_like`, `search`, `select_category`).
- **Crashlytics**: Crash reporting with automatic mapping file upload for release builds.
- **AdMob**: Monetization events (`ad_request`, `ad_impression` via `OnPaidEventListener`, `ad_click`, `ad_failed`) for Banner & Rewarded formats.
- **User Properties**: `language`, `country`, `engagement_level` (new, active, dormant) with anti-redundancy local caching.
- **Remote Config**: Dependency configured via Firebase BoM for remote parameter management.
