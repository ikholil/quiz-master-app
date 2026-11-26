# Backend API contract

The Android client talks to the NestJS backend over HTTPS JSON. This file is generated from
`core/network/src/main/kotlin/app/prostuti/core/network/api/ProstutiApi.kt`; DTO shapes live in
`core/network/.../dto`. Keep both in sync with the backend's OpenAPI document.

## Conventions

- Base URL from `prostuti.apiBaseUrl`; all paths are versioned under `/v1`.
- Every response is wrapped: `{ "data": <payload>, "meta": { "nextCursor": string?, "serverTime": string? } }`.
- Errors: HTTP status plus `{ "statusCode", "code", "message", "fieldErrors"?, "entitlement"? }`.
  `401` → refresh token flow; `402`/`403` with `code = PREMIUM_REQUIRED` → paywall; `409` → conflict (e.g. exam time over);
  `400`/`422` → validation messages shown to the student; `429` honours `Retry-After`.
- Auth: `Authorization: Bearer <access>`; refresh with `POST /v1/auth/refresh` (refresh tokens rotate).
- Headers sent by the client: `Accept-Language` (`bn`/`en`), `X-Client: android`, `X-App-Version`.
- Dates are ISO-8601 (`Instant` for timestamps, `yyyy-MM-dd` for dates). Text content is `{ "en": string, "bn": string? }`.
- Pagination is cursor based (`cursor`, `limit` query params; `meta.nextCursor`).

## Integrity rules the backend must enforce

- **Mock exams**: the server owns the clock. `POST /mocks/{id}/attempts` returns `serverNow` and `endsAt`;
  questions are sent **without** `solution` unless the mock's rules allow revealing answers. Autosave
  (`PUT /attempts/{id}/answers`) must be rejected with `409` after the deadline. `POST /attempts/{id}/submit`
  must be idempotent per attempt and score on the server. Late submissions keep only answers saved before the deadline.
- **Offline practice** results arrive later through `POST /sync` as `practice.offline_result` and are
  stored as unofficial practice, never as mock scores.
- **AI tutor** requests carry an intent and context (question/topic/material ids); the backend retrieves
  approved content (RAG), returns source references and enforces daily limits. Provider keys never reach the app.
- **Payments**: `POST /billing/checkout` returns a hosted checkout URL (bKash/Nagad/card). Entitlements are
  read back from `GET /me/subscription`; the client never trusts local purchase state.

## Offline sync operations (`POST /v1/sync`)

| `type` | Payload |
|---|---|
| `topic.progress` | `{ topicId, completed, updatedAt }` |
| `flashcard.review` | `{ cardId, grade, reviewedAt }` (server replays SM-2) |
| `flashcard.create` | `{ clientId, deckId, front, back }` |
| `note.upsert` / `note.delete` | `NoteDto` / note id |
| `practice.offline_result` | `SubmitPracticeDto` with `clientSessionId` |
| `wrong_answer.resolve` | `[questionId]` |

The response lists `appliedIds` and `rejectedIds`; operations are replayed in `createdAt` order.

## Endpoints

### AuthApi

| Method | Path | Function |
|---|---|---|
| POST | `/v1/auth/otp/request` | `requestOtp` |
| POST | `/v1/auth/otp/verify` | `verifyOtp` |
| POST | `/v1/auth/email/login` | `emailLogin` |
| POST | `/v1/auth/email/register` | `emailRegister` |
| POST | `/v1/auth/password/forgot` | `forgotPassword` |
| POST | `/v1/auth/logout` | `logout` |

### RefreshApi

| Method | Path | Function |
|---|---|---|
| POST | `/v1/auth/refresh` | `refresh` |

### ProfileApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/me/profile` | `profile` |
| PUT | `/v1/me/preferences` | `savePreferences` |
| POST | `/v1/planner/preview` | `previewPlan` |
| POST | `/v1/me/onboarding/complete` | `completeOnboarding` |
| GET | `/v1/me/dashboard` | `dashboard` |
| GET | `/v1/me/analytics` | `analytics` |

### ExamApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/exams` | `exams` |
| GET | `/v1/exams/{id}` | `exam` |
| PUT | `/v1/exams/{id}/bookmark` | `bookmark` |
| DELETE | `/v1/exams/{id}/bookmark` | `unbookmark` |
| GET | `/v1/exams/compare` | `compare` |
| PUT | `/v1/me/targets` | `setTargets` |
| GET | `/v1/announcements` | `announcements` |

### LearningApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/exams/{id}/subjects` | `subjects` |
| GET | `/v1/subjects/{id}/chapters` | `chapters` |
| GET | `/v1/topics/{id}` | `topic` |
| GET | `/v1/topics/{id}/offline-bundle` | `topicBundle` |
| GET | `/v1/materials/{id}` | `material` |
| PUT | `/v1/topics/{id}/progress` | `setProgress` |
| PUT | `/v1/topics/{id}/bookmark` | `bookmark` |
| DELETE | `/v1/topics/{id}/bookmark` | `unbookmark` |
| PUT | `/v1/topics/{id}/note` | `saveNote` |

### QuestionApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/questions` | `questions` |
| GET | `/v1/questions/{id}` | `question` |
| GET | `/v1/questions/{id}/similar` | `similar` |
| PUT | `/v1/questions/{id}/bookmark` | `bookmark` |
| DELETE | `/v1/questions/{id}/bookmark` | `unbookmark` |
| PUT | `/v1/questions/{id}/note` | `saveNote` |
| POST | `/v1/questions/{id}/reports` | `report` |
| GET | `/v1/me/wrong-answers` | `wrongAnswers` |
| POST | `/v1/me/wrong-answers/{id}/resolve` | `resolveWrongAnswer` |
| GET | `/v1/me/collections` | `collections` |
| POST | `/v1/me/collections` | `createCollection` |
| POST | `/v1/me/collections/{id}/questions` | `addToCollection` |

### PracticeApi

| Method | Path | Function |
|---|---|---|
| POST | `/v1/practice/sessions` | `start` |
| POST | `/v1/practice/sessions/{id}/submit` | `submit` |
| GET | `/v1/practice/recommended-sets` | `recommendedSets` |
| GET | `/v1/practice/sets/{id}/bundle` | `setBundle` |
| GET | `/v1/practice/daily-challenge` | `dailyChallenge` |

### MockExamApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/mocks` | `mocks` |
| GET | `/v1/mocks/{id}` | `mock` |
| POST | `/v1/mocks/{id}/attempts` | `startAttempt` |
| GET | `/v1/attempts/{id}` | `attempt` |
| PUT | `/v1/attempts/{id}/answers` | `saveAnswers` |
| POST | `/v1/attempts/{id}/submit` | `submit` |
| GET | `/v1/attempts/{id}/result` | `result` |
| GET | `/v1/me/attempts` | `history` |

### PlannerApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/planner/plan` | `plan` |
| PATCH | `/v1/planner/tasks/{id}` | `setStatus` |
| POST | `/v1/planner/recover` | `recover` |
| POST | `/v1/planner/regenerate` | `regenerate` |

### RevisionApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/flashcards/decks` | `decks` |
| GET | `/v1/flashcards` | `flashcards` |
| POST | `/v1/flashcards` | `create` |
| POST | `/v1/flashcards/batch` | `saveBatch` |
| GET | `/v1/revision/quick` | `quickRevision` |
| GET | `/v1/notes` | `notes` |
| POST | `/v1/sync` | `sync` |

### TutorApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/ai/usage` | `usage` |
| POST | `/v1/ai/tutor/messages` | `ask` |

### CommunityApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/groups` | `groups` |
| POST | `/v1/groups/{id}/members` | `join` |
| DELETE | `/v1/groups/{id}/members/me` | `leave` |
| GET | `/v1/groups/{id}/leaderboard` | `leaderboard` |
| GET | `/v1/discussions` | `discussions` |
| GET | `/v1/discussions/{id}/replies` | `replies` |
| POST | `/v1/discussions` | `post` |
| POST | `/v1/discussions/{id}/replies` | `reply` |
| POST | `/v1/content/{id}/reports` | `report` |

### AccountApi

| Method | Path | Function |
|---|---|---|
| GET | `/v1/me/subscription` | `subscription` |
| GET | `/v1/billing/plans` | `plans` |
| POST | `/v1/billing/checkout` | `checkout` |
| GET | `/v1/me/privacy` | `privacy` |
| PUT | `/v1/me/privacy` | `updatePrivacy` |
| POST | `/v1/me/data-export` | `requestExport` |
| POST | `/v1/me/deletion-request` | `requestDeletion` |
| GET | `/v1/me/achievements` | `achievements` |
| GET | `/v1/support/tickets` | `tickets` |
| POST | `/v1/support/tickets` | `createTicket` |
| GET | `/v1/me/notifications` | `notifications` |
| POST | `/v1/me/devices` | `registerDevice` |
