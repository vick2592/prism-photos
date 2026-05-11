---
applyTo: "**"
---

# Prism Photos — Copilot Coding Instructions

## Project Identity

- **App**: Prism — a local-only Android gallery app, replacing Google Photos
- **Package**: `dev.prism.gallery`
- **Language**: Kotlin (no Java)
- **UI**: Jetpack Compose exclusively — no XML layouts
- **Architecture**: MVVM + Clean Architecture (data / domain / ui layers)
- **DI**: Hilt
- **Min SDK**: 26, **Target SDK**: 35 (Android 15)
- **Test device**: Sony Xperia 1V, Android 15

## Core Principle: Zero Cloud

Prism NEVER syncs, uploads, or transmits photos anywhere. Do not suggest, scaffold, or implement any cloud, account, or network-based photo features. There are no user accounts. There is no internet permission.

## Coding Conventions

- Use Kotlin idioms: `data class`, `sealed class`, `object`, `companion object`, extension functions
- Prefer `StateFlow` and `collectAsStateWithLifecycle()` over `LiveData`
- All `ViewModel`s use `viewModelScope` for coroutines
- Use `suspend` functions in repositories; never block the main thread
- All Compose functions are annotated `@Composable`, use `remember {}` and `derivedStateOf {}` appropriately
- Use `LaunchedEffect` for one-shot side effects, `SideEffect` only when needed for non-compose world
- Follow unidirectional data flow: UI events → ViewModel → Repository → UI state
- Never expose `MutableStateFlow` publicly from ViewModels; use backing private `_state` + public `state`

## Package Structure

```
dev.prism.gallery/
├── data/
│   ├── local/         # MediaStore queries, Room, ContentObserver
│   ├── model/         # Data models (MediaItem, Album)
│   └── preferences/   # DataStore
├── domain/
│   ├── model/         # Domain models
│   └── usecase/       # One class per use case
├── ui/
│   ├── gallery/       # Main grid screen
│   ├── viewer/        # Full-screen photo/video viewer
│   ├── albums/        # Albums list + detail
│   ├── search/        # Search screen
│   ├── edit/          # UCrop-based editing
│   ├── settings/      # Settings screen
│   ├── components/    # Shared reusable composables
│   └── theme/         # Material 3 theme, colors, typography
└── di/                # Hilt modules
```

## Key Libraries — Always Use These, Never Alternatives

| Purpose | Library |
|---------|---------|
| Image loading | `io.coil-kt:coil-compose` |
| Video playback | `androidx.media3:media3-exoplayer` + `media3-ui` |
| Database | `androidx.room` with KSP |
| DI | `com.google.dagger:hilt-android` with KSP |
| Settings | `androidx.datastore:datastore-preferences` |
| EXIF parsing | `androidx.exifinterface:exifinterface` |
| Background work | `androidx.work:work-runtime-ktx` |
| Image editing | `com.github.yalantis:ucrop` |
| Navigation | `androidx.navigation:navigation-compose` |

## MediaStore Rules

- Always query `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` and `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` on a background coroutine dispatcher (`Dispatchers.IO`)
- Use `ContentObserver` registered on both URIs to detect new photos instantly — this is the key differentiator from Google Photos
- Sort by `MediaStore.MediaColumns.DATE_TAKEN DESC` as default
- For Android 13+, use `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` permissions (not `READ_EXTERNAL_STORAGE`)
- Thumbnail loading via Coil with `rememberAsyncImagePainter(uri)` — never load full bitmaps in the grid

## Permissions Handling

- Request permissions with `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())`
- Show rationale in a Compose dialog before re-requesting
- On Android 13+: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`
- On Android 11+: `MANAGE_MEDIA` for trash operations
- Never hardcode SDK checks inline — use a utility function `isAtLeastApi(level: Int)`

## Compose UI Patterns

- Grid: `LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 120.dp))`
- Full-screen viewer: `HorizontalPager` from `androidx.compose.foundation.pager`
- Pinch-to-zoom: `Modifier.transformable(state = rememberTransformableState {...})`
- Date section headers: use `stickyHeader {}` in `LazyVerticalGrid`
- Bottom navigation: `NavigationBar` with 4 items (Gallery, Albums, Search, Settings)
- Use `Material3` components only — no Material 2 imports
- Dynamic color theming: `dynamicDarkColorScheme` / `dynamicLightColorScheme` for Android 12+

## Room Database Schema

- `FavoritesEntity(mediaId: Long, addedAt: Long)` — stores favorited photo MediaStore IDs
- `TrashEntity(mediaId: Long, originalUri: String, deletedAt: Long)` — soft-delete with 30-day TTL
- WorkManager `TrashPurgeWorker` runs daily to delete items older than 30 days

## Video Playback

- Use `Media3 ExoPlayer` inside a `DisposableEffect` to manage lifecycle
- Persist resume position per video URI in DataStore
- Support local file URIs from MediaStore only — no streaming URLs

## EXIF & Location

- Parse with `ExifInterface(inputStream)`
- Extract: `TAG_GPS_LATITUDE`, `TAG_GPS_LONGITUDE`, `TAG_DATETIME_ORIGINAL`, `TAG_IMAGE_WIDTH`, `TAG_IMAGE_LENGTH`
- Reverse geocode with `android.location.Geocoder` — run on `Dispatchers.IO`, handle `IOException`
- Display raw coordinates as fallback if geocoding fails
- GPS data is displayed only, never transmitted

## Image Editing

- Crop/rotate: Launch UCrop via `ActivityResultContracts.StartActivityForResult()`
- Brightness/contrast: Apply `ColorMatrix` via `Canvas` + save to `Pictures/Prism/` using `MediaStore.insertImage()`
- Always save as a new copy — never overwrite the original

## Error Handling

- Repository functions return `Result<T>` — use `runCatching { }` at the boundary
- ViewModels expose a `UiState` sealed class with `Loading`, `Success(data)`, `Error(message)` states
- Never show raw exception messages to the user

## Testing Checklist (run on Xperia 1V / Android 15 via ADB)

1. Take photo with Camera → Prism grid updates within 2 seconds (ContentObserver test)
2. Swipe through photos in viewer with pinch-to-zoom
3. Play video — confirm resume dialog on second open
4. Favorite a photo → appears in Favorites album
5. Trash a photo → disappears from grid, visible in Trash album, purged after 30 days
6. Search by date range
7. Crop + rotate a photo → saved copy in Pictures/Prism/
8. Slideshow runs with correct interval

## ADB Commands

```bash
# Install debug build
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logcat for Prism only
~/Library/Android/sdk/platform-tools/adb logcat -s "Prism"

# Check connected devices
~/Library/Android/sdk/platform-tools/adb devices
```

## Commit Convention

Use conventional commits:
- `feat: add pinch-to-zoom in viewer`
- `fix: ContentObserver not firing on cold launch`
- `chore: add UCrop dependency`
- `refactor: extract MediaGrid into shared composable`
