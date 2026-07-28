# Graph Report - .  (2026-07-28)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 311 nodes · 435 edges · 24 communities
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 8 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `236b6c93`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 9
- Community 10
- Community 11
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16
- Community 17
- Community 18
- Community 19

## God Nodes (most connected - your core abstractions)
1. `EssentialsRepository` - 17 edges
2. `HomeViewModel` - 15 edges
3. `AppBlockingService` - 14 edges
4. `EssentialsApi` - 13 edges
5. `PhotoCaptureViewModel` - 12 edges
6. `LocalChoreEntity` - 11 edges
7. `LocalCompletionEntity` - 11 edges
8. `LoginViewModel` - 11 edges
9. `ChoreDetailViewModel` - 10 edges
10. `Chore` - 10 edges

## Surprising Connections (you probably didn't know these)
- `EssentialsNavGraph()` --calls--> `ChoreDetailScreen()`  [INFERRED]
  app/src/main/java/com/isaacshub/essentials/ui/navigation/NavGraph.kt → app/src/main/java/com/isaacshub/essentials/ui/choredetail/ChoreDetailScreen.kt
- `EssentialsNavGraph()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/isaacshub/essentials/ui/navigation/NavGraph.kt → app/src/main/java/com/isaacshub/essentials/ui/home/HomeScreen.kt
- `EssentialsNavGraph()` --calls--> `LoginScreen()`  [INFERRED]
  app/src/main/java/com/isaacshub/essentials/ui/navigation/NavGraph.kt → app/src/main/java/com/isaacshub/essentials/ui/login/LoginScreen.kt
- `EssentialsNavGraph()` --calls--> `SetupScreen()`  [INFERRED]
  app/src/main/java/com/isaacshub/essentials/ui/navigation/NavGraph.kt → app/src/main/java/com/isaacshub/essentials/ui/setup/SetupScreen.kt
- `EssentialsApp` --references--> `EssentialsRepository`  [EXTRACTED]
  app/src/main/java/com/isaacshub/essentials/EssentialsApp.kt → app/src/main/java/com/isaacshub/essentials/data/repository/EssentialsRepository.kt

## Import Cycles
- None detected.

## Communities (24 total, 0 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.07
Nodes (20): EssentialsApi, EssentialsEndpointPaths, Result, CreateChoreRequest, UpdateChoreRequest, AdminOverrideRequest, ChoreCompletion, ChoreWithCompletion (+12 more)

### Community 1 - "Community 1"
Cohesion: 0.10
Nodes (7): ChoreDao, Flow, DayOfWeekListConverter, LocalChoreEntity, EssentialsRepository, Flow, Result

### Community 2 - "Community 2"
Cohesion: 0.11
Nodes (12): ChoreCard(), CompletionProgressCard(), HomeScreen(), Modifier, ChoreUiState, Factory, HomeUiState, HomeViewModel (+4 more)

### Community 3 - "Community 3"
Cohesion: 0.16
Nodes (10): AppBlockingService, Context, Intent, start(), stop(), IBinder, Notification, Service (+2 more)

### Community 4 - "Community 4"
Cohesion: 0.16
Nodes (7): CompletionDao, Flow, LocalCompletionEntity, EssentialsDatabase, getInstance(), Context, RoomDatabase

### Community 5 - "Community 5"
Cohesion: 0.15
Nodes (11): MainActivity, EssentialsNavGraph(), CameraPreviewScreen(), Modifier, Uri, PhotoCaptureScreen(), PhotoPreviewScreen(), EssentialsTheme() (+3 more)

### Community 6 - "Community 6"
Cohesion: 0.14
Nodes (9): Factory, Context, StateFlow, T, Uri, ViewModel, ViewModelProvider, PhotoCaptureUiState (+1 more)

### Community 7 - "Community 7"
Cohesion: 0.20
Nodes (9): Activity, androidx, DeviceAdminManager, CameraPermissionStep(), DeviceAdminStep(), OverlayPermissionStep(), PermissionStep(), SetupScreen() (+1 more)

### Community 8 - "Community 8"
Cohesion: 0.15
Nodes (8): LoginScreen(), Factory, StateFlow, T, ViewModel, ViewModelProvider, LoginUiState, LoginViewModel

### Community 9 - "Community 9"
Cohesion: 0.21
Nodes (5): AuthRepository, AuthToken, Result, EssentialsApp, Application

### Community 10 - "Community 10"
Cohesion: 0.18
Nodes (8): ChoreDetailScreen(), ChoreDetailUiState, ChoreDetailViewModel, Factory, StateFlow, T, ViewModel, ViewModelProvider

### Community 11 - "Community 11"
Cohesion: 0.27
Nodes (8): ChoreDto, ChoresResponse, CompletionSyncRequest, CompletionSyncResponse, EssentialsApiClient, Result, PhotoVerificationRequest, PhotoVerificationResponse

### Community 12 - "Community 12"
Cohesion: 0.33
Nodes (3): Chore, ChoreScheduleCalculator, TimeZone

### Community 13 - "Community 13"
Cohesion: 0.40
Nodes (4): ChargingMonitorReceiver, Context, Intent, BroadcastReceiver

### Community 14 - "Community 14"
Cohesion: 0.28
Nodes (6): CompletionStatus, COMPLETED, PENDING_VERIFICATION, REJECTED, VERIFIED, CompletionStatusConverter

### Community 15 - "Community 15"
Cohesion: 0.22
Nodes (6): ChoreDetail, Home, Login, PhotoCapture, Routes, Setup

### Community 16 - "Community 16"
Cohesion: 0.28
Nodes (4): AppVersion, Result, UpdateManager, DownloadManager

### Community 17 - "Community 17"
Cohesion: 0.39
Nodes (4): EssentialsDeviceAdminReceiver, Context, Intent, DeviceAdminReceiver

### Community 18 - "Community 18"
Cohesion: 0.25
Nodes (8): CompletionStatus, COMPLETED, FAILED, IN_PROGRESS, NOT_STARTED, PENDING_VERIFICATION, REJECTED, VERIFIED

### Community 19 - "Community 19"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **19 isolated node(s):** `ChoresResponse`, `PENDING_VERIFICATION`, `VERIFIED`, `REJECTED`, `COMPLETED` (+14 more)
  These have ≤1 connection - possible missing edges or undocumented components.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `EssentialsNavGraph()` connect `Community 5` to `Community 8`, `Community 10`, `Community 2`, `Community 7`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Why does `EssentialsRepository` connect `Community 1` to `Community 9`?**
  _High betweenness centrality (0.039) - this node is a cross-community bridge._
- **Why does `HomeScreen()` connect `Community 2` to `Community 5`?**
  _High betweenness centrality (0.036) - this node is a cross-community bridge._
- **What connects `ChoresResponse`, `PENDING_VERIFICATION`, `VERIFIED` to the rest of the system?**
  _19 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.07317073170731707 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.1028225806451613 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.11462450592885376 - nodes in this community are weakly interconnected._