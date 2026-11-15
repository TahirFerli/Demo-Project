# Met Gallery

A small Android app that browses highlighted artworks from [The Metropolitan Museum of Art Collection API](https://metmuseum.github.io/) and shows a detail page for each one.
It's a showcase of modern Android practices: **Jetpack Compose, MVVM + MVI, Hilt, Coroutines/Flow, Retrofit, type-safe Navigation, and tests on every layer**.

## Features
- Adaptive grid of Met highlights with **infinite scroll** and **pull-to-refresh**
- Detail screen with image, artist and bio, date, medium, dimensions, culture, department, credit line, and a link to metmuseum.org
- Loading, error and retry states:
  - Refresh failures show a snackbar.
  - Failed next pages show an **inline retry footer**, and paging pauses instead of retrying in a loop.
- The detail screen opens **instantly from an in-memory cache** for items already in the list

## Tech stack
| Concern | Choice |
|---|---|
| UI | Jetpack Compose, Material 3, Coil 3 |
| Architecture | MVVM + MVI (State / Intent / Effect), clean layers |
| DI | Hilt (+ KSP) |
| Async | Kotlin Coroutines, `StateFlow`, `Channel`, `snapshotFlow`, `async/awaitAll`, `Semaphore`, `Mutex` |
| Networking | Retrofit 3, OkHttp 5, kotlinx.serialization |
| Navigation | Navigation Compose with `@Serializable` type-safe routes |
| Testing | JUnit4, kotlinx-coroutines-test, Turbine, MockWebServer, Compose UI test |

## Architecture

```
ui/            Compose screens + ViewModels (MVI)
 ├─ list/      ArtworkListContract · ArtworkListViewModel · ArtworkListScreen · ArtworkItem
 ├─ detail/    ArtworkDetailContract · ArtworkDetailViewModel · ArtworkDetailScreen
 ├─ navigation/AppNavHost + type-safe destinations
 └─ common/    shared state composables, error → string mapping, ObserveAsEvents
domain/        Pure Kotlin: Artwork, ArtworkPage, DataError, ArtworkRepository (interface), use cases
data/          MetMuseumApi (Retrofit), DTOs, mappers, ArtworkRepositoryImpl
di/            Hilt modules: Network, Repository, Dispatchers
```

Dependencies point **inwards**: `ui → domain ← data`. The domain layer owns the `ArtworkRepository` interface and `data` implements it (dependency inversion), bound with `@Binds` in Hilt.

### Data flow (unidirectional)

```
User action ──Intent──▶ ViewModel ──calls──▶ UseCase ──▶ Repository ──▶ Retrofit
     ▲                     │
     │                     ├── StateFlow<State>  (what to render, survives rotation)
     └──── Compose UI ◀────┴── Flow<Effect>      (one-off: navigate, snackbar, open URL)
```

### How a page is loaded (Met API specifics)
The Met search endpoint only returns **IDs**, so the repository builds each page in two steps:

```
GET v1.1/search?hasImages=true&isHighlight=true&offset=20&limit=20  →  { total, objectIDs[20] }
          │
          └─▶ 20 × GET v1/objects/{id}   in parallel: async/awaitAll, at most 4 at a time (Semaphore)
                    │
                    └─▶ map to Artwork, drop objects without a public-domain image
```

- **Partial failure is tolerated.** Each object request is wrapped on its own, so one 404 doesn't fail the page. If *every* request fails (for example offline), the page fails.
- **`hasMore` comes from `total`** and the API's 10,000-result window, not from the page size, because filtering can make a page shorter.
- The old `v1/search` was retired on 2026-10-01. The app uses the paginated `v1.1/search`.

## Key decisions and why

**MVI on top of MVVM**
- A single immutable `State` per screen is the single source of truth, so impossible state combinations can't happen and states are easy to test.
- A sealed `Intent` gives the screen one entry point, `onIntent()`.
- `Effect`s go through a `Channel` + `receiveAsFlow()` and are delivered **once**. A `StateFlow` would replay navigation and snackbars on rotation.

**State hoisting: Route vs Screen**
- `ArtworkListRoute` is the only composable that knows the ViewModel. It collects state with `collectAsStateWithLifecycle()` and handles effects.
- `ArtworkListScreen(state, onIntent)` is **stateless**, which makes it easy to preview and to UI-test without Hilt or the network.
- Navigation is passed down as lambdas, so screens never see the `NavController`.

**Coroutines and Flow**
- `MutableStateFlow` stays private and is exposed as a read-only `StateFlow`. State is changed atomically with `update {}`.
- **Structured concurrency** in the repository: `coroutineScope { ids.map { async { … } }.awaitAll() }`. If the screen is closed, every child request is cancelled.
- A `Semaphore` limits concurrent requests. The Met asks for at most 80 requests/second, and its firewall blocks bursts.
- A `Mutex` protects the in-memory cache, because requests run concurrently.
- The IO dispatcher is **injected** with a `@IoDispatcher` qualifier, so tests can substitute a `TestDispatcher`.
- `suspendRunCatching` rethrows `CancellationException`. The stdlib `runCatching` would swallow it and break cancellation.
- `snapshotFlow` turns the grid's scroll position into a `Flow` to trigger paging. It keys on the item count too, so a short page still triggers the next one.
- `ObserveAsEvents` collects effects with `repeatOnLifecycle(STARTED)` on `Dispatchers.Main.immediate`, so no events are lost or handled in the background.

**Error handling**
- The data layer maps exceptions to a domain `DataError`:

  | Failure | `DataError` |
  |---|---|
  | `IOException` | `Network` |
  | HTTP 404 | `NotFound` |
  | HTTP 403 / 429 (firewall) | `RateLimited` |
  | Other HTTP errors | `Server(code)` |
  | Anything else | `Unknown` |

- Above the data layer, nothing depends on Retrofit or OkHttp. The UI maps `DataError` to string resources.
- **Lesson learned while building:** once the Met firewall started returning 403, a failed load-more kept re-triggering itself. Now a next-page failure sets `loadMoreError`, paging pauses, and only an explicit `RetryLoadMore` intent or a refresh tries again. A unit test covers it.

**Navigation**
- Routes are `@Serializable` classes, so arguments are checked at compile time.
- `ArtworkDetailViewModel` reads `artworkId` from `SavedStateHandle`, so it survives process death and is trivial to provide in tests.

## Testing

```bash
./gradlew testDebugUnitTest            # JVM unit tests
./gradlew connectedDebugAndroidTest    # Compose UI tests (emulator/device)
```

| Test | What it shows |
|---|---|
| `ArtworkListViewModelTest` | Initial load, error, retry, pagination, duplicate-request guard, end of data, load-more error pauses paging, retry load-more, refresh snackbar, navigation effect |
| `ArtworkDetailViewModelTest` | Nav args via `SavedStateHandle`, error and retry, effects |
| `ArtworkRepositoryImplTest` | Real Retrofit + serialization against **MockWebServer**: request URL, order kept in the parallel fetch, image filtering, partial vs total failure, `hasMore` arithmetic, 403/404/5xx mapping, cache |
| `ArtworkListScreenTest` | Stateless screen in loading, error, content and load-more-error states; fallback texts; intents emitted on click |

Test tools:
- A **hand-written fake** repository instead of mocks: simpler, and it tests behaviour rather than implementation
- `MainDispatcherRule` with `StandardTestDispatcher`, so intermediate states like `isLoading == true` can be asserted before `advanceUntilIdle()`
- **Turbine** for asserting one-off effects emitted from a `Flow`
- A MockWebServer `Dispatcher` that routes by path, because object requests arrive concurrently and in any order

## What I'd do next
- **Offline-first:** Room as the single source of truth, with the repository exposing `Flow<List<Artwork>>` and the network only refreshing the DB
- **Search and filters:** query field and department chips. The API supports `q`, `departmentId` and `dateBegin/dateEnd`.
- **Paging 3** with a `RemoteMediator` once offline caching is in
- **Modularization:** `:core:network`, `:core:domain`, `:feature:list`, `:feature:detail`
- Retry with exponential backoff for `RateLimited`; shared-element transition from grid to detail
- Screenshot tests (Roborazzi/Paparazzi), CI with lint, detekt and tests, baseline profiles
