# Architecture

## Layers

```
feature/*  ──►  core:ui, core:designsystem  ──►  core:domain (repository contracts, use cases, rules)
                                                        ▲
app (DI per flavor) ──► core:data (live) ───────────────┤
                    └─► core:mockbackend (mock flavor) ─┘
core:data ──► core:network, core:database, core:datastore
```

- **Domain is pure Kotlin.** `core:model`, `core:common` and `core:domain` have no Android
  dependencies, so exam-critical rules (scoring, timing, navigation, spaced repetition) are tested
  on the JVM in milliseconds.
- **Repositories return `AppResult<T>`**, never throw. `safeApiCall` maps every exception to an
  `AppError` that `core:ui` turns into a localized, actionable message.
- **ViewModels** expose a single `StateFlow` of immutable UI state. Screens that load one thing extend
  `LoadViewModel<T>`: reloading cancels the previous request, stale data stays on screen during a
  refresh, and one-off errors go to snackbars.
- **Navigation**: type-safe `@Serializable` routes. Features don't import each other; they request a
  `Destination`, which `DestinationNavigator` in `:app` maps to a route. Leaving a submitted exam
  for its result pops the exam off the back stack so "back" can't return into it.

## Exam integrity (mock exams)

| Concern | Where |
|---|---|
| Timer from server time + monotonic clock, immune to device clock changes | `ServerAnchoredClock` |
| No solutions during an active exam (stripped server-side; UI renders `revealed = false`) | `MockExamViewModel`, `QuestionView` |
| Autosave every 5 s while dirty and on every background transition | `MockExamViewModel.saveNow`, `LifecycleEventEffect(ON_STOP)` |
| Resume the same attempt after process death | attempt id in `SavedStateHandle` |
| Auto-submit at timeout; server `409` on late saves triggers submission | `MockExamViewModel` |
| Idempotent submission with retries on transient failures | `SubmitMockExamUseCase` |
| Stable shuffling per attempt | `OptionShuffler` |
| Mock exams are online-only; offline practice is never presented as an official score | `LiveMockExamRepository`, `PracticeResult.isServerVerified` |

## Offline

- **Room is the source of truth for personal study data** (flashcards, notes, mistake notebook,
  topic progress). Changes are written locally first, then appended to an **outbox**
  (`pending_operations`) that `SyncWorker` replays in order when connectivity returns.
  Rejected operations are retried up to 5 times and then dropped, so one bad record can't block the queue.
- **Downloads** (`DownloadWorker`) fetch a topic bundle (topic, materials, questions) or a question
  set, honour the Wi-Fi-only setting and require storage-not-low. The Downloads screen shows
  storage use, the sync queue and what works offline.
- **Shared topics** keep a single progress record keyed by topic id (`topic_progress`), so a topic
  that appears in several target exams is studied once.
- Practice falls back to downloaded questions when the network is unavailable, scores on-device
  and queues the result for sync.

## Security

- Access/refresh tokens are AES-256-GCM encrypted with a non-exportable Android Keystore key and
  excluded from backup (`backup_rules.xml`, `data_extraction_rules.xml`).
- A single `Mutex`-guarded `TokenRefresher` serialises refreshes so parallel 401s trigger only one
  refresh; a rejected refresh token signs the student out.
- Cleartext traffic is disabled; logging redacts `Authorization` and is debug-only.
- No AI or payment credentials ship in the app; both go through backend endpoints.

## Performance on affordable phones

- System fonts only (Roboto and Noto Sans Bengali): no font downloads, no APK cost.
- Flat, outlined surfaces instead of shadows; skeletons pulse alpha instead of animating gradients.
- Coil memory cache is capped at 15 % of app memory, with a 50 MB disk cache. The HTTP cache is 10 MB.
- R8 with resource shrinking and `en`/`bn`-only resources; the release APK is about 3 MB.
- Generous timeouts and idempotent-only retries suit unstable mobile networks.

## Localization

Every screen has English and Bengali strings. Per-app language uses
`AppCompatDelegate.setApplicationLocales`, which is persisted on API < 33 too. Content is
bilingual (`LocalizedText`). Numbers, dates and timers switch to Bengali numerals in Bengali, and
option letters become ক খ গ ঘ. Until the student picks a language, content follows the UI locale
so the interface and the questions never disagree.

## Adding a feature

1. Create `feature/<name>` with `plugins { alias(libs.plugins.prostuti.android.feature) }`.
2. Add `@Serializable` routes and a `NavGraphBuilder.<name>Graph(navController)` function.
3. If other features need to reach it, add a `Destination` and map it in `DestinationNavigator`.
4. Add English and Bengali strings.
5. Include the module in `settings.gradle.kts`, register its graph in `ProstutiAppRoot`, and add it to `app/build.gradle.kts`.
