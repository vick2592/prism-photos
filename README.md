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
| **Slideshow** | Auto-advancing fullscreen slideshow with play/pause and configurable interval |
| **Settings** | Grid columns (2/3/4), slideshow interval (2/3/5/10s), gallery source album picker (dialog with checkboxes) — all persisted with DataStore |
| **Dynamic color** | Material You Monet theming on Android 12+, purple fallback on older devices |

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
| `bucketId` type mismatch in navigation | `Album.id` and `MediaItem.bucketId` correctly typed as `String` |

---

## Roadmap

- [x] All core gallery features
- [x] Debug APK — beta testing in progress
- [ ] **Investigate:** Photos saved to `DCIM/Prism/` (cropped edits) appear in the Prism album but not in the main gallery grid — requires deeper investigation into why `DCIM/Prism` is excluded from the all-media query
- [ ] Signed release APK
- [ ] `ACTION_VIEW` intent filters (appear in "Open with" chooser)
- [ ] Wallpaper setter
- [ ] Sort options (date ascending, size, name)
- [ ] Bulk select and delete / share
- [ ] Create album folder from within the app

---

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE) for details.
