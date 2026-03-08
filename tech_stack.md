# Tech Stack & Architecture — MobileTrack

## Platform
**Android-only (v1)**
iOS restricts the system-level APIs needed (app blocking, usage stats, accessibility).
Minimum SDK: API 26 (Android 8.0) | Target SDK: API 34 (Android 14)

---

## Language & Framework
| Layer | Choice | Reason |
|---|---|---|
| Language | Kotlin | Modern, concise, coroutine support |
| UI | Jetpack Compose | Declarative, less boilerplate |
| Architecture | MVVM + Clean Architecture | Separation of concerns, testable |
| DI | Hilt | Standard Android DI |
| DB | Room | Local-only, privacy-first |
| Async | Coroutines + Flow | Reactive data streams |
| Background | WorkManager + Foreground Service | Reliable background execution |
| Navigation | Navigation Compose | Single-activity architecture |

---

## Key Android System APIs Used

| Feature | Android API |
|---|---|
| Per-app screen time tracking | `UsageStatsManager` |
| Unlock / peek detection | `BroadcastReceiver` (ACTION_USER_PRESENT, ACTION_SCREEN_ON) |
| Current foreground app detection | `AccessibilityService` or `UsageStatsManager` |
| Scroll / interaction detection | `AccessibilityService` (AccessibilityEvent.TYPE_VIEW_SCROLLED) |
| App blocking (hard block) | `AccessibilityService` → navigate away when blocked app opens |
| Daily time budget enforcement | Foreground `Service` + timer |
| Notifications / nudges | `NotificationManager` |

---

## Architecture Layers

```
app/
├── data/
│   ├── local/          # Room DB, DAOs, Entities
│   ├── repository/     # Repository implementations
│   └── preferences/    # DataStore for user settings/rules
├── domain/
│   ├── model/          # Domain models
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic use cases
├── presentation/
│   ├── dashboard/      # Home screen — daily summary
│   ├── applimits/      # Set per-app time budgets
│   ├── rules/          # Focus hours, unlock limits, blocked apps
│   ├── reports/        # Weekly trends & insights
│   └── settings/       # App settings
├── service/
│   ├── TrackingService.kt        # Foreground service, coordinates tracking
│   ├── UsageTracker.kt           # Polls UsageStatsManager
│   ├── UnlockReceiver.kt         # BroadcastReceiver for screen on/unlock
│   └── BlockerAccessibility.kt  # AccessibilityService for scroll & blocking
└── di/                 # Hilt modules
```

---

## Database Schema (Room)

### AppUsageSession
| Column | Type | Description |
|---|---|---|
| id | Long (PK) | |
| packageName | String | e.g. com.instagram.android |
| appName | String | Display name |
| date | String | yyyy-MM-dd |
| totalMinutes | Int | Usage in minutes |
| openCount | Int | How many times opened |
| updatedAt | Long | Timestamp |

### UnlockEvent
| Column | Type | Description |
|---|---|---|
| id | Long (PK) | |
| timestamp | Long | Unix ms |
| hadPurpose | Boolean | Did user select a purpose? |
| purpose | String? | User-selected reason |

### AppRule
| Column | Type | Description |
|---|---|---|
| packageName | String (PK) | |
| dailyLimitMinutes | Int | 0 = no limit |
| isBlocked | Boolean | Hard block |
| blockedDuringFocus | Boolean | |

### FocusSession
| Column | Type | Description |
|---|---|---|
| id | Long (PK) | |
| startHour | Int | e.g. 9 |
| endHour | Int | e.g. 17 |
| daysOfWeek | String | e.g. "MON,TUE,WED" |
| isActive | Boolean | |

---

## Screens

1. **Dashboard** — Today's screen time, unlock count, behavior score, top apps
2. **App Limits** — List of installed apps with daily time budgets
3. **Rules** — Focus hours, max unlocks/day, blocked apps
4. **Reports** — 7-day trends, streaks, weekly score
5. **Unlock Prompt** — Full-screen dialog shown on unlock asking "Why are you here?"
6. **Onboarding** — Grant permissions walkthrough (UsageStats, Accessibility, Overlay)

---

## Permissions Required

```xml
PACKAGE_USAGE_STATS        <!-- UsageStatsManager — user must grant manually -->
BIND_ACCESSIBILITY_SERVICE <!-- AccessibilityService — user must grant manually -->
SYSTEM_ALERT_WINDOW        <!-- Overlay for unlock prompt -->
FOREGROUND_SERVICE         <!-- Persistent tracking service -->
RECEIVE_BOOT_COMPLETED     <!-- Restart service on reboot -->
POST_NOTIFICATIONS         <!-- Nudge notifications (API 33+) -->
```

---

## Privacy Principle
- Zero network calls — no internet permission
- All data stored locally in Room DB
- No analytics, no crash reporting, no third-party SDKs (except Hilt/Compose from Google)
