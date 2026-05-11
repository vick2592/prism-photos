# Prism — Project Context

## Overview

**Prism** is a modern, local-only Android gallery app built as a full replacement for Google Photos on a Sony Xperia 1V (Android 15). It is inspired by the Sony Ericsson Gallery app (`com.sonyericsson.gallery` v3.2.A.0.21) but rebuilt entirely from scratch using modern Android architecture.

The core philosophy: **your photos live on your device. Prism never touches the cloud.**

---

## App Identity

| Field | Value |
|-------|-------|
| App name | Prism |
| Package name | `dev.prism.gallery` |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |
| Language | Kotlin |
| UI toolkit | Jetpack Compose |
| Architecture | MVVM + Clean Architecture (data / domain / ui layers) |
| DI | Hilt |
| GitHub | https://github.com/vick2592/prism-photos |

---

## Target Device

**Sony Xperia 1V** running **Android 15 (API 35)**. All permissions, media APIs, and features must be validated against Android 13+ media permission model (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`) and Android 15 behaviour changes.

---

## Why We're Building This

Google Photos on Android 15 has two critical pain points this user experiences daily:
1. **Constant cloud backup pressure** — repeated prompts and UI nudges to back up to Google cloud
2. **Delayed photo display** — newly captured photos from the Camera app are not immediately visible in Google Photos when both apps are open

Prism solves both:
- Zero cloud, zero accounts, zero backup prompts — ever
- Instant photo detection via `ContentObserver` registered on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` and `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` — grid updates within seconds of the Camera app saving a file

---

## Reference APK: Sony Ericsson Gallery v3.2.A.0.21

Decompiled from `com.sonyericsson.gallery_3.2.A.0.21` (minAPI 10, Android 2.3.3 era).

### Features We Are Keeping / Reimplementing

| Feature | Sony Implementation | Prism Implementation |
|---------|---------------------|----------------------|
| Photo grid | 3D OpenGL `RenderView` + `GridLayer` | `LazyVerticalGrid` (Compose) with Coil image loading |
| Video playback | `MovieView` + `VideoView` with RTSP/HTTP streaming | Media3 ExoPlayer (local files only) |
| Resume video playback | Dialog prompt to resume from last position | DataStore persisted position per video URI |
| Image crop | `CropImageView` with face detection overlay | UCrop library |
| Rotate left/right | In-place EXIF rotation | EXIF rotation + rewrite |
| Set as wallpaper | `Photographs` activity | `WallpaperManager` intent |
| Set as contact photo | Intent to contacts app | Intent to contacts app |
| Share | Standard share intent | Standard share intent |
| Show on map | Location → maps intent | EXIF GPS → maps intent |
| Slideshow | Auto-advancing viewer | `HorizontalPager` with configurable interval |
| Reverse geocoder | `ReverseGeocoder.java` | `Geocoder` API (offline fallback: raw GPS coordinates) |
| Dominant color extraction | `ExtractColorTask.java` | Palette API (Compose `MaterialTheme` dynamic theming) |
| Camera folder | First-class album | First-class album (sorted by `DATE_TAKEN` desc) |
| Storage location labels | eMMC / SD card labels | Internal / SD card badges |
| Face detection (crop) | In `CropImage` activity | UCrop handles aspect ratio; face detection stretch goal (ML Kit) |

### Features We Are Dropping

| Sony Feature | Reason Dropped |
|-------------|----------------|
| Facebook integration (sync, likes, comments) | No cloud / no social |
| Renren integration (Chinese social network) | No cloud / no social |
| Picasa/Google sync | No cloud |
| DLNA "Play on Device" casting | Out of scope for v1 |
| NFC Handover Service | Out of scope for v1 |
| IDD (In-Device Debug) probes | Sony private API, irrelevant |
| Illumination API | Sony private API, irrelevant |
| Account management | No accounts in Prism |
| Edit Video (Sony video editor) | Out of scope for v1 |
| Display Settings (in video) | Out of scope for v1 |
| RTSP/HTTP streaming | Local files only |
| DRM content restrictions | No DRM handling needed |
| Home screen widget | Stretch goal |

### Key Observations from APK Analysis

- The Sony gallery used a **3D OpenGL grid renderer** (`RenderView`, `GridLayer`, `GridCamera`, `GridInputProcessor`) — visually impressive for 2011 but overkill. Prism uses Compose's `LazyVerticalGrid` with smooth animations instead.
- `GalleryApplication` generated a **unique device ID from IMEI hash** for telemetry — Prism has zero telemetry.
- `BootReceiver` listened to `MEDIA_MOUNTED` / `MEDIA_UNMOUNTED` events to refresh caches — Prism uses `ContentObserver` which is more granular and doesn't require a device reboot.
- `BitmapManager` and `DiskCache` handled manual thumbnail caching — Coil handles this automatically with memory + disk caching.
- `MonitoredActivity` was a base class for lifecycle monitoring — replaced by standard Compose lifecycle-aware ViewModels.
- Slideshow and selection modes were part of the main `Gallery` activity — Prism separates these into dedicated composables.
- `ExtractColorTask` extracted dominant colors — we use Android's `Palette` API for the same purpose with dynamic Material 3 theming.

---

## Architecture

```
dev.prism.gallery/
├── data/
│   ├── local/
│   │   ├── MediaStoreRepository.kt       # Queries MediaStore for images + videos
│   │   ├── MediaContentObserver.kt       # ContentObserver for instant photo detection
│   │   └── PrismDatabase.kt              # Room DB (favorites, trash)
│   ├── model/
│   │   └── MediaItem.kt                  # Unified image/video model
│   └── preferences/
│       └── UserPreferences.kt            # DataStore preferences
├── domain/
│   ├── usecase/
│   │   ├── GetGalleryUseCase.kt
│   │   ├── GetAlbumsUseCase.kt
│   │   ├── SearchMediaUseCase.kt
│   │   ├── ToggleFavoriteUseCase.kt
│   │   └── TrashMediaUseCase.kt
│   └── model/
│       └── Album.kt
├── ui/
│   ├── gallery/
│   │   ├── GalleryScreen.kt              # Main LazyVerticalGrid
│   │   └── GalleryViewModel.kt
│   ├── viewer/
│   │   ├── ViewerScreen.kt               # HorizontalPager full-screen
│   │   ├── ViewerViewModel.kt
│   │   └── VideoPlayerScreen.kt          # Media3 ExoPlayer
│   ├── albums/
│   │   ├── AlbumsScreen.kt
│   │   └── AlbumsViewModel.kt
│   ├── search/
│   │   ├── SearchScreen.kt
│   │   └── SearchViewModel.kt
│   ├── edit/
│   │   └── EditScreen.kt                 # UCrop + brightness/contrast
│   ├── settings/
│   │   └── SettingsScreen.kt
│   ├── components/
│   │   ├── MediaGrid.kt                  # Reusable grid composable
│   │   ├── MediaThumbnail.kt             # Coil-backed image tile
│   │   └── DateHeader.kt                 # Sticky date section header
│   └── theme/
│       ├── Theme.kt                      # Material 3 dynamic color
│       ├── Color.kt
│       └── Type.kt
└── di/
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    └── AppModule.kt
```

---

## Key Dependencies

```kotlin
// Image loading
implementation("io.coil-kt:coil-compose:2.7.0")

// Video playback
implementation("androidx.media3:media3-exoplayer:1.4.0")
implementation("androidx.media3:media3-ui:1.4.0")

// Database (favorites, trash)
implementation("androidx.room:room-runtime:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// Dependency injection
implementation("com.google.dagger:hilt-android:2.51")
ksp("com.google.dagger:hilt-compiler:2.51")

// Settings persistence
implementation("androidx.datastore:datastore-preferences:1.1.1")

// EXIF data (location, date, orientation)
implementation("androidx.exifinterface:exifinterface:1.3.7")

// Background work (trash auto-purge)
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Image editing (crop, rotate)
implementation("com.github.yalantis:ucrop:2.2.8")

// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.06.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.foundation:foundation")  // HorizontalPager, LazyVerticalGrid
```

---

## ContentObserver Strategy (Instant Photo Detection)

This is the most critical feature differentiating Prism from Google Photos:

```kotlin
// MediaContentObserver.kt — registered in the app's lifecycle
class MediaContentObserver(handler: Handler) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        // Notify MediaStoreRepository to re-query
        // This fires within ~1 second of Camera saving a photo
    }
}

// Register on both URIs:
contentResolver.registerContentObserver(
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer
)
contentResolver.registerContentObserver(
    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer
)
```

---

## Permissions (Android 13+ / API 33+)

```xml
<!-- Images -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<!-- Videos -->
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<!-- Required for trash / manage media on Android 11+ -->
<uses-permission android:name="android.permission.MANAGE_MEDIA" />
<!-- Writing edited photos back to MediaStore -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<!-- Wallpaper -->
<uses-permission android:name="android.permission.SET_WALLPAPER" />
<!-- Location (EXIF display only, never transmitted) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

## Feature Roadmap

### Phase 1 — Project Scaffolding
Android Studio project setup, Gradle dependencies, package structure, theme.

### Phase 2 — Core Gallery
MediaStore query, ContentObserver, `LazyVerticalGrid`, date headers, permission flow.

### Phase 3 — Photo & Video Viewer
`HorizontalPager`, pinch-to-zoom (`Modifier.transformable`), Media3 ExoPlayer, action bar (share, favorite, trash, info).

### Phase 4 — Albums & Organization
Albums by `BUCKET_DISPLAY_NAME`, Favorites (Room), Trash with 30-day WorkManager purge.

### Phase 5 — Search
Date range picker, EXIF location search, reverse geocoding.

### Phase 6 — Basic Editing
UCrop (crop/rotate), brightness/contrast via `ColorMatrix`, save copy to `Pictures/Prism/`.

### Phase 7 — Advanced Polish
Slideshow, Material 3 dynamic theming, settings screen, onboarding, face grouping (ML Kit stretch goal).

### Phase 8 — Deploy & Test
`adb install`, full device testing on Xperia 1V / Android 15.

---

## Development Notes

- **ADB path**: `~/Library/Android/sdk/platform-tools/adb`
- **Test device**: Sony Xperia 1V, Android 15, model QV7706YMJA
- **APK reference files**: `../apk-resources/` (apktool) and `../apk-source/` (jadx) — relative to this repo
- All commits follow conventional commits format: `feat:`, `fix:`, `chore:`, `refactor:`
