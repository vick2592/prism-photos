# Prism — Local Photo Gallery

A modern, privacy-first Android photo and video gallery built entirely with Kotlin and Jetpack Compose. No cloud sync. No account required. Your photos stay on your device.

> Built as a full replacement for Google Photos on Sony Xperia 1V running Android 15.

---

## Features

| Feature | Details |
|---|---|
| **Gallery grid** | Date-grouped grid with day-level headers — "Today", "Yesterday", "Tue, 6 May", "March 2024". Configurable 2/3/4 column layout |
| **Gallery source filter** | Camera roll (DCIM/) shown by default — detected via `RELATIVE_PATH` for OEM compatibility. Additional albums can be pinned via Settings dialog |
| **Scrollbar** | Draggable and tappable right-edge scrollbar with floating day/month label that tracks the thumb position |
| **Nav bar auto-hide** | Navigation bar slides away when scrolling down the gallery, returns when scrolling up |
| **Viewer** | Full-screen HorizontalPager — swipe left/right between media |
| **Pinch-to-zoom** | Smooth multi-touch zoom up to 5× with panning when zoomed in |
| **Double-tap to zoom** | Double-tap zooms in smoothly (spring animation); double-tap again zooms out with tween animation |
| **Swipe to dismiss** | Pull down with spring-back and fly-off animation to return to grid |
| **Video playback** | ExoPlayer with native transport controls |
| **Video thumbnails** | First-frame thumbnails in the grid via Coil VideoFrameDecoder |
| **EXIF info** | Date, resolution, file size, camera make/model, GPS location with reverse geocoding |
| **Favorites** | Heart icon to mark and filter favorites, persisted locally |
| **Share** | System share sheet for any photo or video |
| **Albums** | Auto-grouped by folder (camera, screenshots, downloads, etc.) |
| **Album detail** | Per-album grid with full viewer access |
| **Search** | Live search with 250ms debounce across all display names |
| **Trash (30-day)** | Soft-delete with restore, permanent delete, and days-remaining badge |
| **Auto-purge** | WorkManager background job purges trash older than 30 days |
| **Image editing** | Crop and rotate with UCrop — saves a new copy in the same folder AND same storage volume (SD card or internal) as the original, named `photo_2.jpg` (auto-incrementing) |
| Multi-select in gallery | Long-press any photo to enter selection mode. Tap photos to add/remove. Action bar shows count with Share, Trash, and permanent Delete buttons. Back button exits selection |
| **3-dot overflow menu in viewer** | MoreVert button (top-right, auto-hides on zoom) with Slideshow, Set as wallpaper, and Delete from device options |
| **Permanent delete from viewer** | Confirmation dialog → `contentResolver.delete()` with `MANAGE_MEDIA` permission — no system consent dialog on API 31+ |

---

## Screenshots

_Coming soon._

---

## Tech Stack

| Layer | Library / Tool |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose BOM 2024.12.01 + Material 3 |
| Architecture | MVVM + Clean Architecture (data / domain / ui) |
| Dependency injection | Hilt 2.52 + KSP |
| Navigation | Navigation Compose 2.8.5 |
| Image loading | Coil 2.7.0 + coil-video |
| Video playback | Media3 ExoPlayer 1.4.1 |
| Database | Room 2.6.1 (favorites + trash) |
| Preferences | DataStore Preferences 1.1.1 |
| Background work | WorkManager 2.9.1 |
| EXIF metadata | AndroidX ExifInterface 1.3.7 |
| Image editing | UCrop 2.2.8 |
| Build | AGP 8.9.0, Gradle 8.11.1, JVM 17 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## Project Structure

```
app/src/main/java/dev/prism/gallery/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs (FavoriteDao, TrashDao)
│   │   ├── entity/       # Room entities (FavoriteEntity, TrashEntity)
│   │   ├── MediaStoreRepository.kt
│   │   └── PrismDatabase.kt
│   ├── model/
│   │   └── MediaItem.kt
│   └── preferences/
│       └── PreferencesRepository.kt
├── domain/
│   ├── model/
│   │   └── Album.kt
│   └── usecase/
│       ├── GetAlbumsUseCase.kt
│       ├── GetGalleryUseCase.kt
│       └── TrashMediaUseCase.kt
├── ui/
│   ├── albums/           # Albums grid + AlbumDetail
│   ├── components/       # MediaGrid, MediaThumbnail
│   ├── gallery/          # GalleryScreen + GalleryViewModel
│   ├── search/           # SearchScreen + SearchViewModel
│   ├── settings/         # SettingsScreen + SettingsViewModel
│   ├── slideshow/        # SlideshowScreen + SlideshowViewModel
│   ├── trash/            # TrashScreen + TrashViewModel
│   └── viewer/           # ViewerScreen, VideoPlayerScreen, EXIF, EditHelper
├── worker/
│   └── TrashPurgeWorker.kt
├── NavGraph.kt
└── PrismApplication.kt
```

---

## Building

### Prerequisites

- Android Studio Panda 2025.3.4 or later
- Android SDK 35
- JDK 17

### Debug build

Open the project in Android Studio and press **Run ▶**, or build an APK via:

**Build → Build Bundle(s) / APK(s) → Build APK(s)**

The output APK is at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Permissions

| Permission | Purpose |
|---|---|
| `READ_MEDIA_IMAGES` | Read photos (Android 13+) |
| `READ_MEDIA_VIDEO` | Read videos (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Read media (Android 12 and below) |
| `WRITE_EXTERNAL_STORAGE` | Write media (Android 8 only) |
| `MANAGE_MEDIA` | Permanent delete via system dialog (Android 11+) |
| `SET_WALLPAPER` | Reserved for future wallpaper feature |

---

## Privacy

Prism is 100% local. It:
- Makes zero network requests
- Has no analytics, crash reporting, or telemetry
- Does not require any account or sign-in
- Never uploads, backs up, or shares your photos

---

## Bug Fixes

| Bug | Fix |
|-----|-----|
| Bottom nav bar flashing when closing viewer | `AnimatedVisibility` + 220ms show delay |
| Horizontal swipe broken when zoomed in | Custom `awaitEachGesture` — only intercepts touch when `scale > 1` |
| Video on adjacent pages blocking swipe | Adjacent pages render a static thumbnail instead of live `ExoPlayer` |
| UCrop result callback referencing stale item | `editingDisplayName` state var captured at tap time |
| Cropped photo not appearing in gallery | `DATE_TAKEN`, `DATE_ADDED`, `DATE_MODIFIED` now written to MediaStore on save |
| Crop saved to wrong storage volume | `EditHelper.volumeCollectionUri()` queries `VOLUME_NAME` column instead of parsing URI path segment — now correctly targets SD card or internal storage |
| Viewer pager drifts after crop save | `pendingScrollToId` + `LaunchedEffect` scrolls pager to crop's actual index when the reactive list updates |
| Viewer sort inconsistent with gallery | All sort paths now use `compareByDescending{dateTaken OR dateModified×1000}.thenByDescending{id}` — gallery, viewer, and MediaStore SQL query all agree |
| Crop sorted to bottom when original has no EXIF date | `editingDateTaken` falls back to `dateModified×1000L` when `dateTaken==0` |
| Slideshow showing all photos regardless of gallery filter | `SlideshowViewModel` now applies `isInDcim + extraGalleryBucketIds` filter matching the gallery grid |
| `bucketId` type mismatch in navigation | `Album.id` and `MediaItem.bucketId` correctly typed as `String` |

---

## Roadmap

- [x] All core gallery features
- [x] Multi-select (long press to select, share/trash/delete selected)
- [x] Viewer 3-dot menu (slideshow, set wallpaper, permanent delete)
- [x] Debug APK — beta testing complete
- [ ] **Touch UX polish** — improve scroll smoothness and gesture responsiveness:
  - Faster thumbnail decode on first open (pre-warm Coil disk cache)
  - Eliminate jank when tapping a photo immediately after a fast grid scroll (input event debounce tuning)
  - Reduce swipe-to-next-photo latency in the viewer (beyondViewportPageCount tuning + Coil prefetch)
  - Selection mode: drag gesture to select a range of photos without lifting finger
  - Haptic feedback on long-press to enter selection mode
- [ ] Signed release APK
- [ ] Publish to Google Play
- [ ] `ACTION_VIEW` intent filters (appear in "Open with" chooser)
- [ ] Sort options (date ascending, size, name)
- [ ] Create album folder from within the app
- [ ] Print support (`androidx.print`)
- [ ] Home screen widget (latest photo, or random from an album)

---

## Releasing to Google Play

### 1. Create a signing keystore (once)
```bash
keytool -genkey -v -keystore prism-release.jks -keyAlias prism -keyalg RSA -keysize 2048 -validity 10000
```
Store this file safely — losing it means you can never update the app on Play.

### 2. Configure signing in `app/build.gradle.kts`
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../prism-release.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "prism"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
release {
    signingConfig = signingConfigs.getByName("release")
    isMinifyEnabled = true
    isShrinkResources = true
}
```

### 3. Build a signed AAB
**Build → Generate Signed Bundle / APK → Android App Bundle**
Output: `app/build/outputs/bundle/release/app-release.aab`

### 4. Google Play Console checklist
- [ ] Create developer account at [play.google.com/console](https://play.google.com/console) ($25 one-time fee)
- [ ] Create new app — **Package name: `dev.prism.gallery`** (permanent, cannot change)
- [ ] Complete store listing:
  - [ ] App title, short description (80 chars), full description
  - [ ] Screenshots: phone (min 2), 7-inch tablet optional, 10-inch tablet optional
  - [ ] Feature graphic (1024×500)
  - [ ] App icon (512×512 PNG, already done)
  - [ ] Privacy policy URL (required — even for local-only apps)
- [ ] Complete content rating questionnaire (no violence, no user-generated content → "Everyone")
- [ ] Set up target audience (general public, not children → no COPPA concerns)
- [ ] Upload AAB to **Internal testing** track first → test on device → promote to **Production**
- [ ] No `INTERNET` permission declared → no data safety section required beyond confirming no data is collected

### 5. Privacy policy
Because Prism accesses photos, Google requires a privacy policy URL even though nothing is transmitted. A minimal hosted page stating "Prism does not collect, transmit, or share any data. All photos remain on your device." is sufficient.

---

## Releasing to Google Play

### 1. Create a signing keystore (once)
```bash
keytool -genkey -v -keystore prism-release.jks -keyAlias prism -keyalg RSA -keysize 2048 -validity 10000
```
Store this file safely — losing it means you can never update the app on Play.

### 2. Configure signing in `app/build.gradle.kts`
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../prism-release.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "prism"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
release {
    signingConfig = signingConfigs.getByName("release")
    isMinifyEnabled = true
    isShrinkResources = true
}
```

### 3. Build a signed AAB
**Build → Generate Signed Bundle / APK → Android App Bundle**  
Output: `app/build/outputs/bundle/release/app-release.aab`

### 4. Google Play Console checklist
- [ ] Create developer account at [play.google.com/console](https://play.google.com/console) ($25 one-time fee)
- [ ] Create new app — **Package name: `dev.prism.gallery`** (permanent, cannot change after first publish)
- [ ] Complete store listing:
  - [ ] App title, short description (80 chars max), full description
  - [ ] Screenshots: at least 2 phone screenshots
  - [ ] Feature graphic (1024×500 px)
  - [ ] App icon (512×512 PNG)
  - [ ] Privacy policy URL (required even for local-only apps)
- [ ] Content rating questionnaire — no violence, no user-generated content → "Everyone"
- [ ] Target audience — general public, not directed at children (no COPPA/GDPR-K requirements)
- [ ] Upload AAB to **Internal testing** track first → test on device → promote to **Production**
- [ ] Data safety section — Prism collects no data, select "No" for all data collection questions

### 5. Privacy policy
Google requires a privacy policy URL even for apps that collect nothing. A minimal hosted page is sufficient:
> "Prism does not collect, transmit, store, or share any personal data or photos. All media remains on your device and is never uploaded anywhere."

A free option: publish as a GitHub Gist or a simple GitHub Pages page.

---

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE) for details.
