# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Multi-module Android app using MVI + Clean Architecture.
Package root: `com.template.android` · Min SDK: 24 · Compile SDK: 37
Build system: Gradle 9.4.1 · AGP 9.2.1 · Kotlin 2.2.10

---

## Common commands

```bash
./gradlew assembleDevDebug                        # build debug APK (dev flavor)
./gradlew assembleProdRelease                     # build release APK (prod flavor, minified)
./gradlew :app:testDevDebugUnitTest               # run app unit tests
./gradlew :app:connectedDevDebugAndroidTest       # run instrumented tests (device/emulator required)
./gradlew :<module>:testDebugUnitTest             # run unit tests for a non-app module (no flavor needed)
./gradlew clean                                   # wipe build artifacts
```

Note: `:app` has `dev` and `prod` flavors — all `:app` tasks require a flavor prefix (`Dev`/`Prod`).
Library and feature modules have no flavors; plain `testDebugUnitTest` works for them.

---

## Module graph

```
app
 ├── :feature:feature-*        (one module per screen group)
 ├── :layer:layer-data
 ├── :core:core-ui
 └── :core:core-common

feature:feature-*
 ├── :layer:layer-domain       (use cases + repository interfaces)
 ├── :core:core-ui
 └── :core:core-common

layer:layer-data               (repository implementations)
 ├── :layer:layer-domain
 ├── :core:core-common
 ├── :core:core-network
 └── :core:core-database

layer:layer-domain             (pure Kotlin — no Android framework)
 └── :core:core-common         (via api() — transitively available to features)

core:core-datastore
 └── :core:core-common

core:core-testing              (testImplementation only — never ship)
 └── :core:core-common
```

**Hard rules — never break these:**
- Features depend on `:layer:layer-domain` for use cases and repository interfaces. Never on `:layer:layer-data`, `:core:core-network`, or `:core:core-database` directly.
- Features never depend on other features.
- `:layer:layer-domain` is pure Kotlin — no Android framework imports, no Hilt.
- `:app` only wires navigation, the Hilt application class, and top-level DI; no business logic lives here.
- Concrete auth implementations (e.g. `DataStoreTokenProvider`) belong in `:layer:layer-data`, not in `:core:core-datastore`. Core modules are infrastructure only.

---

## Module purposes

| Module | Purpose |
|---|---|
| `:app` | `MyApplication`, `MainActivity`, `TemplateApp` composable, top-level `NavDisplay`, `AppModule` |
| `:core:core-common` | `Result<T>`, `AppDispatchers`, `UiText`, `TokenProvider` interface |
| `:layer:layer-domain` | `UseCase<P,R>`, `FlowUseCase<P,T>` base classes, repository interfaces, domain models |
| `:layer:layer-data` | Repository implementations, `networkBoundResource`, `safeApiCall`, `@StaticToken` |
| `:core:core-network` | Retrofit + OkHttp setup, `NetworkModule` (Hilt), `ApiResponse`, `@BaseUrl` |
| `:core:core-database` | Room `AppDatabase`, `DatabaseModule` (Hilt), all `@Entity` + `@Dao` classes, `Converters` |
| `:core:core-datastore` | `PreferencesRepository`, `DataStoreModule` (Hilt) — infrastructure only |
| `:core:core-ui` | `AndroidTemplateTheme`, `AppTheme.spacing`, `ScreenScaffold`, `LoadingIndicator`, `ErrorContent`, `EmptyState`, `AvatarImage`, `RemoteImage`, `shimmerEffect()`, preview utilities |
| `:core:core-testing` | `MainDispatcherRule`, `TestAppDispatchers`, `Fixtures.kt` — `testImplementation` only |
| `:feature:feature-home` | Home screen — template for all future feature modules |

---

## Convention plugins

Defined in `build-logic/convention/src/main/kotlin/`. Applied by ID — no catalog entry needed.

| Plugin ID | Applies | Use on |
|---|---|---|
| `template.android.application` | `com.android.application` | `:app` only |
| `template.android.library` | `com.android.library` | all library modules |
| `template.android.compose` | `kotlin.plugin.compose` + `buildFeatures.compose` | any module with Compose UI |
| `template.android.hilt` | `hilt.android` + `ksp` + hilt deps | any module with `@Inject` / `@HiltViewModel` |

**Important — AGP 9.x notes:**
- Do NOT apply `org.jetbrains.kotlin.android` manually — AGP 9.x applies it automatically.
- `buildFeatures.compose` must be set via the `template.android.compose` plugin, not inline.
- `compileSdk`, `minSdk`, `targetSdk` are set centrally in the convention plugins from `libs.versions.toml` — don't override them per-module.

---

## MVI pattern

Every feature follows this exact split:

```
<Name>Contract.kt     — Action, Event, UiState types (no logic)
<Name>Screen.kt       — @Composable fun, calls hiltViewModel(), no logic
<Name>ViewModel.kt    — @HiltViewModel, extends BaseViewModel<Action, Event, UiState>
navigation/<Name>Navigation.kt — NavKey + EntryProviderScope extension
```

**Three types per feature (in `<Name>Contract.kt`):**
- **Action** — sealed interface of user intents sent from Compose to the VM
- **Event** — sealed interface of one-time side effects sent from VM to Compose (navigation, toasts)
- **UiState** — immutable data class representing the full screen state

**ViewModel template:**
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSomething: GetSomethingUseCase,
) : BaseViewModel<HomeAction, HomeEvent, HomeUiState>(
    initialState = HomeUiState(),
) {
    override fun handleAction(action: HomeAction) {
        when (action) {
            HomeAction.LoadContent -> loadContent()
        }
    }

    private fun loadContent() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            // call use case, then:
            updateState { copy(isLoading = false, items = result) }
            // or for one-time side effects:
            sendEvent(HomeEvent.ShowSnackbar(UiText.StringResource(R.string.home_loaded)))
        }
    }
}
```

**Screen template:**
```kotlin
@Composable
fun HomeScreen(
    onItemClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message.resolve(context))
            }
        }
    }

    HomeContent(uiState = uiState, onAction = viewModel::onAction, snackbarHostState = snackbarHostState)
}
```

**Navigation template:**
```kotlin
@Serializable
data object HomeKey : NavKey   // data class for destinations with args

fun EntryProviderScope<NavKey>.homeDestination(
    onItemClick: (String) -> Unit = {},
) {
    entry<HomeKey> { HomeScreen(onItemClick = onItemClick) }
}
```

Register in `TemplateApp` inside `entryProvider { }`: `homeDestination(onItemClick = { ... })`

Navigate to a destination: `backStack.add(HomeKey)`
Navigate back: `backStack.removeLastOrNull()`

**`BaseViewModel` lives in `:core:core-ui`** (`core/core-ui/src/main/java/.../core/ui/BaseViewModel.kt`). It exposes:
- `uiState: StateFlow<UiState>` — collect with `collectAsStateWithLifecycle()`
- `events: Flow<Event>` — collect inside `LaunchedEffect(Unit)` for one-time side effects
- `onAction(action)` — called by Compose; routes to `handleAction` via a buffered `SharedFlow`

---

## `UiText`

ViewModels must never hold raw strings — they don't have a `Context` and raw strings break testability.

```kotlin
// ViewModel — always use UiText
sendEvent(HomeEvent.ShowSnackbar(UiText.StringResource(R.string.home_error)))
updateState { copy(error = UiText.Plain(e.message ?: "Unknown error")) }

// Composable — resolve to String
val message = uiTextValue.asString()
```

`UiText` lives in `:core:core-ui`: `UiText.StringResource(id, vararg args)` for localized strings, `UiText.Plain(value)` for raw strings (logs, debug, network messages).

---

## Strings

- All user-visible strings go in `res/values/strings.xml` of the module that owns them.
- Naming: `<module>_<component>_<qualifier>` snake_case — e.g. `home_empty_title`, `profile_cd_back`.
- Content descriptions use `cd_` qualifier: `home_cd_poster`.
- Never hardcode a string in a `@Composable` — always use `stringResource()`.

---

## Spacing

Never use hardcoded `dp` values in composables. Always use `AppTheme.spacing.*`:

```kotlin
Modifier.padding(AppTheme.spacing.lg)   // 16.dp
Modifier.height(AppTheme.spacing.xxl)   // 32.dp
```

Tokens: `xs=4`, `sm=8`, `md=12`, `lg=16`, `content=20`, `xl=24`, `xxl=32` dp.

---

## DI pattern

Use `abstract class` with `companion object` — never mix `@Binds` and `@Provides` in an `object`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {
    @Binds @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository

    companion object {
        @Provides @Singleton
        fun provideHomeApi(retrofit: Retrofit): HomeApiService = retrofit.create(HomeApiService::class.java)
    }
}
```

---

## Secrets (API keys)

API keys go in `local.properties` (gitignored), injected via `BuildConfig` at build time. Never hardcode in source files. Injected with `@StaticToken` qualifier from `layer-data/di/StaticToken.kt`.

```
# local.properties
api.key=your_key_here
```

```kotlin
// AppModule.kt
@Provides @StaticToken fun provideStaticApiKey(): String = BuildConfig.API_KEY
```

---

## Mapper pattern

Mappers are always extension functions in a dedicated `*Mapper.kt` file. Never inside DTOs or entity classes.

```kotlin
// layer-data/foo/FooMapper.kt
fun FooDto.toDomain(): Foo = Foo(id = id, name = name)
fun FooEntity.toDomain(): Foo = Foo(id = id, name = name)
fun Foo.toEntity(): FooEntity = FooEntity(id = id, name = name)
```

Domain models store full image URLs, not raw paths — URL construction happens in the mapper.

---

## Room / database

- `@Entity` and `@Dao` classes live exclusively in `:core:core-database`.
- Complex types use `Converters` (JSON-based via `org.json` — no extra dependency). Annotate at the `@Database` level.
- Migrations are declared as `companion object` constants inside `AppDatabase`.
- `DatabaseModule` provides individual DAOs, not just `AppDatabase`, so modules only take what they need.
- For DAO tests: use `buildTestDatabase()` from `core-database/src/test/` (Robolectric, in-memory).

---

## Testing

**ViewModel tests (MockK + Turbine):**
```kotlin
class FooViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val getFoo: GetFooUseCase = mockk()
    private lateinit var viewModel: FooViewModel

    @Before fun setUp() { viewModel = FooViewModel(getFoo) }

    @Test fun `loads data on init`() = runTest {
        coEvery { getFoo() } returns listOf(fakeFoo())
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

**DAO tests (Robolectric, in-memory Room):**
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FooDaoTest {
    private lateinit var db: AppDatabase
    @Before fun setUp() { db = buildTestDatabase() }
    @After fun tearDown() { db.close() }
}
```

Add shared fixture builders to `core/core-testing/src/main/java/.../Fixtures.kt`.

---

## Navigation — adding a new bottom tab

1. Add a `NavKey` + `destination` function in the new feature's `navigation/` package.
2. Add a new entry to the `Tab` enum in `app/navigation/TemplateNavigationBar.kt`.
3. Register the destination in `TemplateApp`'s `entryProvider { }` block.
4. Add `it is NewKey` to the `showBottomBar` `derivedStateOf` check in `TemplateApp`.

---

## Skills

Detailed step-by-step recipes live in `.github/skills/`. Load the relevant file for the task:

| Task | Skill |
|---|---|
| Add a feature screen/module | `.github/skills/add-feature-module/SKILL.md` |
| Add a Room entity + DAO + repository | `.github/skills/add-room-entity/SKILL.md` |
| Add a Retrofit API call end-to-end | `.github/skills/add-api-call/SKILL.md` |
| Add a library dependency | `.github/skills/add-dependency/SKILL.md` |
| Write or set up tests | `.github/skills/android-testing/SKILL.md` |
| Load remote images with Coil | `.github/skills/add-image-loading/SKILL.md` |

---

## Key files

| File | Purpose |
|---|---|
| `gradle/libs.versions.toml` | Single source of truth for all versions and dependencies |
| `settings.gradle.kts` | Module includes + `includeBuild("build-logic")` |
| `build-logic/convention/src/main/kotlin/` | All four convention plugins |
| `core/core-common/src/main/java/.../Result.kt` | `Result<T>` — use for all async return types |
| `core/core-common/src/main/java/.../AppDispatchers.kt` | Inject for coroutine dispatcher control in tests |
| `core/core-ui/src/main/java/.../UiText.kt` | ViewModel-safe string wrapper |
| `core/core-ui/src/main/java/.../theme/Spacing.kt` | `AppTheme.spacing.*` tokens |
| `layer/layer-data/src/main/java/.../util/NetworkBoundResource.kt` | Offline-first Flow helper |
| `layer/layer-domain/src/main/java/.../UseCase.kt` | Base class for all use cases |
| `core/core-ui/src/main/java/.../theme/Theme.kt` | App theme — `AndroidTemplateTheme`, `AppTheme` |
| `app/src/main/java/.../MainActivity.kt` | Single activity, hosts `TemplateApp` composable |
| `app/src/main/java/.../navigation/TemplateNavigationBar.kt` | Bottom nav bar — `Tab` enum + `TemplateNavigationBar` |

---

## What NOT to do

- Don't apply `org.jetbrains.kotlin.android` in any build file or convention plugin.
- Don't add `android.useAndroidX=true` to module-level `gradle.properties` — it's set globally.
- Don't put `@Entity` or `@Dao` classes outside `:core:core-database`.
- Don't put Retrofit service interfaces outside `:core:core-network`.
- Don't put business logic in `@Composable` functions or in `MainActivity`.
- Don't add direct dependencies between feature modules.
- Don't use `LiveData` — use `StateFlow` / `collectAsStateWithLifecycle()`.
- Don't extend `ViewModel` directly in feature modules — extend `BaseViewModel<Action, Event, UiState>`.
- Don't use a sealed interface for `UiState` — use a data class with default values.
- Don't use the deprecated `kotlinOptions { jvmTarget }` DSL — use `compilerOptions { jvmTarget.set(...) }`.
- Don't hardcode `dp` values — use `AppTheme.spacing.*`.
- Don't hardcode colors — use `MaterialTheme.colorScheme.*`.
- Don't hardcode strings in composables — use `stringResource()`.
- Don't hardcode API keys in source files — use `local.properties` + `BuildConfig`.
- Don't hold raw strings in ViewModels — use `UiText`.
- Don't put mapper logic inside DTO or entity classes — use a dedicated `*Mapper.kt` file.
- Don't define `Action`, `Event`, and `UiState` inside the ViewModel file — put them in `<Name>Contract.kt`.
- Don't let feature modules depend on `:layer:layer-data`, `:core:core-network`, or `:core:core-database` — only `:layer:layer-domain`.
- Don't put concrete auth implementations in `:core:core-datastore` — that module is infrastructure only.
