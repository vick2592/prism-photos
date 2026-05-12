# Prism — Project Context

## Overview

**Prism** is a modern, local-only Android gallery app built as a full replacement for Google Photos on a Sony Xperia 1V (Android 15). It is inspired by the Sony Ericsson Gallery app (`com.sonyericsson.gallery` v3.2.A.0.21) but rebuilt entirely from scratch using modern Android architecture.

The core philosophy: **your photos live on your device. Prism never touches the cloud.**

> **Status (11 May 2026):** Beta testing complete. All major planned features implemented and verified on-device. Preparing for signed release APK and Google Play submission.

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

**Pattern:** MVVM + Clean Architecture  
**Package:** `dev.prism.gallery`

```
app/src/main/java/dev/prism/gallery/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── FavoriteDao.kt
│   │   │   └── TrashDao.kt
│   │   ├── entity/
│   │   │   ├── FavoriteEntity.kt
│   │   │   └── TrashEntity.kt
│   │   ├── MediaStoreRepository.kt       # MediaStore queries + ContentObserver (instant photo detection)
│   │   └── PrismDatabase.kt              # Room DB v1 (favorites + trash tables)
│   ├── model/
│   │   └── MediaItem.kt                  # Unified image/video model
│   └── preferences/
│       └── PreferencesRepository.kt      # DataStore: grid_columns, slideshow_interval_secs
├── domain/
│   ├── model/
│   │   └── Album.kt
│   └── usecase/
│       ├── GetAlbumsUseCase.kt
│       ├── GetGalleryUseCase.kt
│       └── TrashMediaUseCase.kt
├── ui/
│   ├── albums/
│   │   ├── AlbumDetailScreen.kt
│   │   ├── AlbumDetailViewModel.kt       # bucketId: String from SavedStateHandle
│   │   ├── AlbumsScreen.kt               # "Recently Deleted" tile + folder albums
│   │   └── AlbumsViewModel.kt
│   ├── components/
│   │   ├── MediaGrid.kt                  # LazyVerticalGrid, accepts columns: Int param
│   │   └── MediaThumbnail.kt             # Coil AsyncImage with video duration badge
│   ├── gallery/
│   │   ├── GalleryScreen.kt              # Date-grouped grid + slideshow FAB
│   │   └── GalleryViewModel.kt           # Reads gridColumns from PreferencesRepository
│   ├── search/
│   │   ├── SearchScreen.kt
│   │   └── SearchViewModel.kt            # 250ms debounce, flatMapLatest
│   ├── settings/
│   │   ├── SettingsScreen.kt             # FilterChip pickers for columns + interval
│   │   └── SettingsViewModel.kt
│   ├── slideshow/
│   │   ├── SlideshowScreen.kt            # Fullscreen HorizontalPager, tap to pause
│   │   └── SlideshowViewModel.kt
│   ├── trash/
│   │   ├── TrashScreen.kt                # 3-col grid, days-remaining badge, restore/delete
│   │   └── TrashViewModel.kt
│   └── viewer/
│       ├── EditHelper.kt                 # UCrop intent builder + MediaStore save (with DATE_TAKEN)
│       ├── ExifReader.kt                 # ExifInterface wrapper, reverse geocoding
│       ├── VideoPlayerScreen.kt          # ExoPlayer, only active on current page
│       ├── ViewerScreen.kt               # HorizontalPager, zoom/pan, swipe-to-dismiss
│       └── ViewerViewModel.kt
├── worker/
│   └── TrashPurgeWorker.kt               # PeriodicWorkRequest, 1-day interval, purges >30 days
├── NavGraph.kt                           # All routes + bottom bar AnimatedVisibility
└── PrismApplication.kt                   # @HiltAndroidApp, ImageLoaderFactory, TrashPurgeWorker.schedule
```

---

## Key Dependencies (actual versions in use)

| Library | Version |
|---------|---------|
| Kotlin | 2.0.21 |
| Jetpack Compose BOM | 2024.12.01 |
| AGP | 8.9.0 |
| KSP | 2.0.21-1.0.28 |
| Hilt | 2.52 |
| Navigation Compose | 2.8.5 |
| Coil | 2.7.0 (+ coil-video) |
| Media3 ExoPlayer | 1.4.1 |
| Room | 2.6.1 |
| DataStore Preferences | 1.1.1 |
| WorkManager | 2.9.1 |
| ExifInterface | 1.3.7 |
| UCrop | 2.2.8 |
| Gradle | 8.11.1 |

---

## App Identity

| Field | Value |
|-------|-------|
| App name | Prism |
| Package name | `dev.prism.gallery` |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |
| Language | Kotlin |
| UI toolkit | Jetpack Compose (no XML layouts) |
| Architecture | MVVM + Clean Architecture (data / domain / ui) |
| DI | Hilt 2.52 + KSP |
| GitHub | https://github.com/vick2592/prism-photos |

---

## Target Device

**Sony Xperia 1V** running **Android 15 (API 35)**. All permissions, media APIs, and features validated against Android 13+ media permission model (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`) and Android 15 behaviour changes.

- ADB path: `~/Library/Android/sdk/platform-tools/adb`
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

---

## Why We're Building This

Google Photos on Android 15 has two critical pain points this user experiences daily:
1. **Constant cloud backup pressure** — repeated prompts and UI nudges to back up to Google cloud
2. **Delayed photo display** — newly captured photos from the Camera app are not immediately visible in Google Photos when both apps are open

Prism solves both:
- Zero cloud, zero accounts, zero backup prompts — ever
- Instant photo detection via `ContentObserver` registered on both `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` and `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` — grid updates within seconds of the Camera app saving a file

---

## Permissions

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.MANAGE_MEDIA" />
<uses-permission android:name="android.permission.SET_WALLPAPER" />
```

UCropActivity is declared in the manifest.

---

## Feature Status

| Feature | Status | Notes |
|---------|--------|-------|
| Gallery grid (date-grouped) | ✅ Done | Day-level headers: "Today", "Yesterday", "Tue 6 May", "March 2024" |
| Gallery source filter | ✅ Done | `RELATIVE_PATH.startsWith("DCIM/")` (OEM-safe); extra albums opt-in via Settings dialog |
| Configurable columns (2/3/4) | ✅ Done | DataStore persisted |
| Viewer (swipe, zoom, dismiss) | ✅ Done | Custom awaitEachGesture, zoom up to 5× |
| Double-tap to zoom | ✅ Done | Animated with spring (zoom-in) + tween (zoom-out); double-tap again resets to 1× |
| Video playback | ✅ Done | ExoPlayer, controls, only active on current page |
| Video thumbnails in grid | ✅ Done | Coil VideoFrameDecoder |
| EXIF info sheet | ✅ Done | Date, size, resolution, GPS + reverse geocode |
| Favorites | ✅ Done | Room FavoriteEntity, heart icon |
| Share | ✅ Done | System share intent |
| Albums (folder-based) | ✅ Done | BUCKET_ID grouping, includes "Recently Deleted" tile |
| Album detail | ✅ Done | |
| Live search | ✅ Done | 250ms debounce |
| Trash (30-day soft-delete) | ✅ Done | Days-remaining badge, restore, permanent delete |
| Trash auto-purge | ✅ Done | WorkManager PeriodicWorkRequest, 1-day interval |
| Image crop/rotate (UCrop) | ✅ Done | Saves copy in same folder AND same storage volume (SD card or internal) as original, named `<original>_2.jpg` (auto-increment) |
| Slideshow | ✅ Done | Fullscreen HorizontalPager, configurable interval |
| Scrollbar with date label | ✅ Done | Draggable/tappable; pixel-accurate thumb; floating day/month pill while scrolling |
| Nav bar auto-hide on scroll | ✅ Done | Hides when scrolling down gallery, restores on scroll up via `NestedScrollConnection` |
| Settings screen | ✅ Done | Grid columns + slideshow interval + gallery source album picker (dialog with checkboxes) |
| Material You dynamic color | ✅ Done | Monet on API 32+, purple fallback |
| App icon | ✅ Done | White bg, two-tone purple prism, rainbow rays |
| Bottom nav flash fix | ✅ Done | AnimatedVisibility with 220ms delay |
| Multi-select in gallery | ✅ Done | Long-press to enter selection mode; tap to toggle; action bar: count, Share, Trash, Delete forever (confirmation dialog); Back exits |
| Viewer 3-dot menu | ✅ Done | MoreVert top-right, auto-hides on zoom/tap; Slideshow / Set as wallpaper (photos only) / Delete from device |
| Permanent delete from viewer | ✅ Done | `contentResolver.delete()` + `MANAGE_MEDIA`; confirmation dialog; Toast + navigate back |
| Overlay auto-hide on zoom | ✅ Done | `overlaysVisible = uiVisible && !isZoomedIn`; `ZoomableImage.onZoomStateChange` callback at 1.05× |
| Scrollbar thumb | ✅ Done | 8dp pill thumb (vs 3dp track), alpha 0.70; easier to see and grab |
| Touch UX polish | ⬜ Roadmap | See roadmap below |
| `ACTION_VIEW` intent filters | ⬜ Roadmap | Allows Prism to appear in "Open with" |
| Wallpaper setter | ⬜ Roadmap | |
| Sort options | ⬜ Roadmap | Date ascending, size, name |
| Bulk select | ⬜ Roadmap | Multi-select delete/share |
| Signed release APK | ⬜ Next | After beta testing complete |

---

## Known Bug Fixes Applied

| Bug | Fix | Commit |
|-----|-----|--------|
| Bottom nav bar flashing when closing viewer | `AnimatedVisibility` + 220ms delay on show | `cb1a1a9` |
| Horizontal pager swipe broken when zoomed | Custom `awaitEachGesture` — only consumes touch when scale > 1 | `cb1a1a9` |
| Video on adjacent pages blocked swipe | Adjacent pages show static `AsyncImage` thumbnail, not live player | `cb1a1a9` |
| UCrop `currentItem` unresolved in launcher callback | `editingDisplayName` state var captured at tap time | `920bf41` |
| Cropped photo not appearing in gallery | `DATE_TAKEN`, `DATE_ADDED`, `DATE_MODIFIED` now written to MediaStore on save | `bbcba1a` |
| `bucketId` type mismatch (Long vs String) | `Album.id` and `MediaItem.bucketId` are `String` | `9251de5` |
| Gallery not refreshing after crop save | `MutableSharedFlow` manual refresh trigger in `MediaStoreRepository`; `refreshMedia()` called after save | `adb5b8c` |
| Search bar + UCrop toolbar behind status bar on Android 15 | `statusBarsPadding()` on SearchScreen column; `windowOptOutEdgeToEdgeEnforcement` on UCropActivity theme | `15b4451` |
| Swipe-to-dismiss 1-second delay | `onNavigateBack()` called immediately at threshold (not after animation); fly-off reduced 220ms → 120ms; bottom bar delay 220ms → 80ms | `482c103` |
| Crop save not indexed by MediaStore | `MediaScannerConnection.scanFile()` called after `IS_PENDING=0` update | `482c103` |
| isCameraRoll() filter broken on Sony OEM folder names | Replaced with `isInDcim(relativePath)` — checks `RELATIVE_PATH.startsWith("DCIM/")`; `MediaItem` + `Album` now carry `relativePath` field queried from MediaStore | `b42a8d2` |
| Viewer swipe showed photos outside gallery context | `ViewerViewModel` now reads `filter` arg from `SavedStateHandle`; gallery passes `"gallery"`, album detail passes `"bucket:{id}"`, search passes `"all"` | `b42a8d2` |
| Settings album picker was overflowing horizontal scroll | Replaced `LazyRow` + `FilterChip` with `OutlinedButton` → `AlertDialog` + `LazyColumn` + `Checkbox` | `b42a8d2` |
| Crop saved to internal storage even when original is on SD card | `EditHelper.volumeCollectionUri()` reads volume name from URI path segments and inserts into the same volume | `3569910` |
| Double-tap zoom was instant (no animation) | `scale`/`panX`/`panY` converted to `Animatable`; zoom-in uses `spring(NoBouncy)`, zoom-out uses parallel `tween(280ms)` | `3569910` |
| Build error: stale duplicate lines in `MediaStoreRepository.kt` | Removed leftover `bucketName`/`duration` lines from previous edit | `69d9936` |
| Build error: Unicode arrow in KDoc caused `DCIM/*` to be parsed as block comment open | Replaced `→` with ASCII `->` in `ViewerViewModel` comment | `69d9936` |
| Viewer swiping to wrong photo after crop | `EditHelper.saveToMediaStore` now accepts `originalDateTaken: Long`; crop copy gets the same `DATE_TAKEN` as the original so it sorts adjacent in the date-sorted list | `41adcc8` |
| Scrollbar thumb not visible / hard to grab | Thumb widened from 3dp to 8dp pill; alpha raised from 0.45 to 0.70; track remains 3dp | `41adcc8` |
| No way to permanently delete from viewer | Added 3-dot MoreVert menu (Slideshow / Set as wallpaper / Delete from device); permanent delete via `contentResolver.delete()` + confirmation dialog; `MediaStoreRepository.deleteItemPermanently()` + `ViewerViewModel.deleteItemPermanentlyAndWait()` | `41adcc8` |
| Overlays not hiding on zoom-in | `ZoomableImage.onZoomStateChange` callback fires at 1.05× threshold; `overlaysVisible = uiVisible && !isZoomedIn` | `41adcc8` |
| Build error: Expecting `}` at `ViewerScreen.kt:543` | Missing closing `}` for `ViewerScreen` composable function after the delete dialog block | `391eae5` |
| Multi-select: `combinedClickable` long-press + selection state in `GalleryViewModel` | `_selectedIds: MutableStateFlow<Set<Long>>`; `toggleSelection`, `clearSelection`, `trashSelected`, `deleteSelectedPermanently`; `MediaThumbnail` shows dim overlay + checkmark; `MediaGrid` wires `selectedIds` + `onMediaLongClick`; `GalleryScreen` action bar with Share/Trash/Delete | `34a2435` |
| Crop saved to wrong storage volume | `EditHelper.volumeCollectionUri()` now queries `MediaStore.MediaColumns.VOLUME_NAME` column instead of parsing URI path segment (always was `"external"` → always internal); returns real volume UUID for SD card | `1a20cb1` |
| Viewer pager drifts after crop save | `pendingScrollToId: Long?` state set to `ContentUris.parseId(saved)` after crop; `LaunchedEffect(state.items, pendingScrollToId)` scrolls pager to crop index when list updates | `1a20cb1` |
| Slideshow shows all photos (ignores gallery filter) | `SlideshowViewModel` now uses 3-flow `combine(observeMedia, observeTrash, extraGalleryBucketIds)` with `isInDcim + extraBuckets` filter | `1558dc4` |
| Viewer sort inconsistent with gallery (crop went to bottom) | All sort paths unified: `compareByDescending{dateTaken OR dateModified×1000}.thenByDescending{id}`; MediaStore SQL query now `DATE_TAKEN DESC, _ID DESC`; crops (higher `_ID`, same `DATE_TAKEN`) always sort immediately before their originals | `c6a1458` |
| `editingDateTaken` was 0 for photos with no EXIF date | `editingDateTaken` now uses `dateModified×1000L` as fallback when `dateTaken==0`; prevents crop from getting `DATE_TAKEN=0` in MediaStore | `c6a1458` |

---

## Known Open Issues (Side Notes)

| Issue | Notes |
|-------|-------|
| **Touch UX polish (planned)** | Current gesture handling works correctly but has rough edges under load: (1) tapping a photo immediately after a fast fling can feel sluggish — Coil is still decoding thumbnails and the tap-to-open transition stalls; (2) selection mode has no drag-to-select (finger held down + slide) — only discrete taps; (3) no haptic feedback on long-press; (4) viewer swipe to next photo has a tiny input latency because `beyondViewportPageCount = 1` pre-loads one page but Coil still needs to decode the full image. **Plan:** tune `beyondViewportPageCount`, add Coil `MemoryCache` warming for adjacent items, add `HapticFeedbackType.LongPress` on selection entry, investigate `Modifier.pointerInput` drag-select for `LazyVerticalGrid`. |

---

## Step-by-Step Development Rule

> **IMPORTANT — always follow this workflow:**
> 1. State clearly what change will be made and why, before touching any file.
> 2. Make the change.
> 3. Run `get_errors` on every modified file.
> 4. If there are errors, fix them before proceeding to the next step.
> 5. Ask the user a question if the correct approach is unclear — never guess at the intention.
> 6. Update `CONTEXT.md` and `README.md` at the end of every session.

---

## Reference APK: Sony Ericsson Gallery v3.2.A.0.21

Decompiled from `com.sonyericsson.gallery_3.2.A.0.21` (minAPI 10, Android 2.3.3 era).

### Key Observations

- The Sony gallery used a **3D OpenGL grid renderer** (`RenderView`, `GridLayer`, `GridCamera`) — visually impressive for 2011 but replaced by Compose `LazyVerticalGrid` with Coil.
- `GalleryApplication` generated a **unique device ID from IMEI hash** for telemetry — Prism has zero telemetry.
- `BootReceiver` listened to `MEDIA_MOUNTED` / `MEDIA_UNMOUNTED` — Prism uses `ContentObserver` which is more granular.
- `BitmapManager` and `DiskCache` handled manual thumbnail caching — Coil handles this automatically.
- Facebook/Renren/Picasa integrations dropped — no cloud, no social.
- DLNA casting, NFC Handover, Sony Illumination API, IDD probes — all dropped (Sony private APIs or out of scope).

---

## Development Notes

- All commits follow conventional commits: `feat:`, `fix:`, `chore:`, `refactor:`, `docs:`
- JVM heap set to 2048MB in `gradle.properties`
- `gradlew` not committed — use Android Studio Build menu or `./gradlew` from the project root after first sync
- ProGuard/R8 enabled (`isMinifyEnabled = true`) for release builds

