# Picsum Gallery

A small Android app that lists photos from the [Lorem Picsum](https://picsum.photos) API and shows a detail page for each one.
It's a showcase of modern Android practices: **Jetpack Compose, MVVM + MVI, Hilt, Coroutines/Flow, Retrofit, type-safe Navigation, and tests on every layer**.

## Features
- Adaptive photo grid with **infinite scroll** (page-based "load more") and **pull-to-refresh**
- Detail screen with a large image, author, resolution and a link to the original on Unsplash
- Loading / error / retry states; errors while content is visible show a snackbar instead of wiping the screen
- Server-side resized thumbnails (`/id/{id}/400/{h}`), so the grid never downloads 5000px originals

## Tech stack
| Concern | Choice |
|---|---|
| UI | Jetpack Compose, Material 3, Coil 3 |
| Architecture | MVVM + MVI (State / Intent / Effect), clean layers |
| DI | Hilt (+ KSP) |
| Async | Kotlin Coroutines, `StateFlow`, `Channel`, `snapshotFlow` |
| Networking | Retrofit 3, OkHttp 5, kotlinx.serialization |
| Navigation | Navigation Compose with `@Serializable` type-safe routes |
| Testing | JUnit4, kotlinx-coroutines-test, Turbine, MockWebServer, Compose UI test |

## Architecture

```
ui/            Compose screens + ViewModels (MVI)
 ├─ list/      PhotoListContract · PhotoListViewModel · PhotoListScreen · PhotoItem
 ├─ detail/    PhotoDetailContract · PhotoDetailViewModel · PhotoDetailScreen
 ├─ navigation/AppNavHost + type-safe destinations
 └─ common/    shared state composables, error → string mapping, ObserveAsEvents
domain/        Pure Kotlin: Photo, DataError, PhotoRepository (interface), use cases
data/          PicsumApi (Retrofit), PhotoDto, mappers, PhotoRepositoryImpl
di/            Hilt modules: Network, Repository, Dispatchers
```

Dependencies point **inwards**: `ui → domain ← data`. The domain layer owns the `PhotoRepository` interface and `data` implements it (dependency inversion), bound with `@Binds` in Hilt.

### Data flow (unidirectional)

```
User action ──Intent──▶ ViewModel ──calls──▶ UseCase ──▶ Repository ──▶ Retrofit
     ▲                     │
     │                     ├── StateFlow<State>  (what to render, survives rotation)
     └──── Compose UI ◀────┴── Flow<Effect>      (one-off: navigate, snackbar, open URL)
```

## Key decisions and why

**MVI on top of MVVM**
- A single immutable `State` per screen is the single source of truth, so impossible state combinations can't happen and every screen state is easy to test.
- A sealed `Intent` gives the screen one entry point, `onIntent()`. All user actions are explicit and easy to find.
- `Effect`s go through a `Channel` + `receiveAsFlow()` and are delivered **once**. A `StateFlow` would replay navigation and snackbars on rotation.

**State hoisting: Route vs Screen**
- `PhotoListRoute` is the only composable that knows the ViewModel. It collects state with `collectAsStateWithLifecycle()` and handles effects.
- `PhotoListScreen(state, onIntent)` is **stateless**, which makes it easy to preview and to UI-test without Hilt or the network.
- Navigation is passed down as lambdas, so screens never see the `NavController`.

**Coroutines and Flow**
- `MutableStateFlow` stays private and is exposed as a read-only `StateFlow`. State is changed atomically with `update {}`.
- The IO dispatcher is **injected** with a `@IoDispatcher` qualifier, so tests can substitute a `TestDispatcher`.
- `suspendRunCatching` rethrows `CancellationException`. The stdlib `runCatching` would swallow it and break structured concurrency.
- A load-more `Job` is tracked so duplicate scroll triggers are ignored. Refresh cancels the in-flight request.
- `snapshotFlow` turns the grid's scroll position into a `Flow` and uses `distinctUntilChanged` + `filter` to trigger paging.
- `ObserveAsEvents` collects effects with `repeatOnLifecycle(STARTED)` on `Dispatchers.Main.immediate`, so no events are lost or handled in the background.
- The repository's in-memory cache is protected by a `Mutex`. The detail screen opens instantly for photos already loaded in the list.

**Error handling**
- The data layer maps `IOException` / `HttpException` / parsing errors to a domain `DataError`. Above the data layer, nothing depends on Retrofit or OkHttp.
- The UI maps `DataError` to string resources.

**Navigation**
- Routes are `@Serializable` classes, so arguments are checked at compile time.
- `PhotoDetailViewModel` reads `photoId` from `SavedStateHandle`, so it survives process death and is trivial to provide in tests.

## Testing

```bash
./gradlew testDebugUnitTest            # JVM unit tests
./gradlew connectedDebugAndroidTest    # Compose UI tests (emulator/device)
```

| Test | What it shows |
|---|---|
| `PhotoListViewModelTest` | Initial load, error, retry, pagination, duplicate-request guard, end of list, snackbar effect, refresh, navigation effect |
| `PhotoDetailViewModelTest` | Nav args via `SavedStateHandle`, error and retry, effects |
| `PhotoRepositoryImplTest` | Real Retrofit + serialization against **MockWebServer**: request URL, mapping, HTTP and parsing errors, cache |
| `PhotoListScreenTest` | Stateless screen in loading, error and content states; intents emitted on click |

Test tools:
- A **hand-written fake** repository instead of mocks: simpler, no mocking library, and it tests behaviour rather than implementation
- `MainDispatcherRule` with `StandardTestDispatcher`, so intermediate states like `isLoading == true` can be asserted before `advanceUntilIdle()`
- **Turbine** for asserting one-off effects emitted from a `Flow`

## What I'd do next
- **Offline-first:** Room as the single source of truth, with the repository exposing `Flow<List<Photo>>` and the network only refreshing the DB
- **Paging 3** with a `RemoteMediator` once offline caching is in
- **Modularization:** `:core:network`, `:core:domain`, `:feature:list`, `:feature:detail`
- Shared-element transition from grid to detail; screenshot tests (Roborazzi/Paparazzi)
- CI (GitHub Actions) running lint, unit tests and detekt/ktlint; baseline profiles
