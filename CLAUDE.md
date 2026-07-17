# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MyFschool** — Ứng dụng Sổ liên lạc điện tử cho trường FPT Schools.
Gồm 3 thành phần (chưa tách repo):
- **Flutter Mobile** — App cho Phụ huynh, Học sinh, Giáo viên
- **Flutter Admin Web** — (planned) Dashboard quản trị
- **Backend REST API** — (planned) Spring Boot + MySQL

> ⚠️ Flutter **không kết nối trực tiếp MySQL**. Flutter chỉ gọi Backend API. Backend chịu trách nhiệm xác thực, phân quyền, nghiệp vụ và truy vấn SQL.

## Commands

```bash
# Run app
flutter run

# Analyze code
flutter analyze

# Run all tests
flutter test

# Run single test file
flutter test test/grade_analytics_test.dart

# Build APK
flutter build apk

# Get dependencies
flutter pub get

# Check outdated packages
flutter pub outdated
```

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend Mobile | Dart / Flutter | SDK ^3.11.5 |
| Frontend Admin | React | (planned) |
| Backend API | Spring Boot | (planned) |
| Database | MySQL | (see docs/database.md) |
| Auth | JWT | (planned) |

## Architecture — MVC (Flutter)

Package root: `lib/vn/edu/fpt/`

```
lib/vn/edu/fpt/
├── src/
│   ├── models/          # Domain models (Student, Grade, AttendanceStats, ...)
│   ├── api/
│   │   ├── client/      # API clients (FakeApiClient — mock data)
│   │   ├── dto/         # Data Transfer Objects (json → model mapping)
│   │   └── exception/   # Custom exceptions
│   ├── repositories/    # Data access layer (SchoolRepository)
│   └── services/        # Business logic (SchoolService)
└── view/
    ├── design_system/   # App theme, colors, text styles, reusable widgets
    │   └── widgets/     # (AppCard, PrimaryButton, InfoCard, MenuItem)
    └── screens/         # All screens (one file per screen)
```

### Data Flow

```
Screen → Service → Repository → FakeApiClient → (future: REST API)
                                              → DTO → Domain Model
```

- **Models** are pure Dart objects (no JSON dependency)
- **DTOs** handle JSON serialization (`fromJson` → `toDomain()`)
- **Repository** abstracts data source (currently mock collections)
- **Service** orchestrates business logic across repositories

### Key Pattern: Actor-Based Navigation

3 actors: `parent`, `student`, `teacher` — determined after login via `AppActor` enum.

`AppShell` (IndexedStack, 4 tabs):
| Tab | Index | Parent | Student | Teacher |
|-----|-------|--------|---------|---------|
| 🏠 Trang chủ | 0 | HomeParent | HomeStudent | HomeTeacher |
| 💬 Tin nhắn | 1 | ConversationsScreen | same | TeacherInboxScreen |
| 📢 Thông báo | 2 | AnnouncementsScreen | same | same |
| 👤 Tài khoản | 3 | AccountProfileScreen | same | same |

Each tab has its own `Navigator` (nested navigator pattern) — use `_navigatorKeys[index]` for in-tab navigation.

## Naming Conventions

- **Files**: `snake_case.dart` (e.g. `home_screen_phuhuynh.dart`)
- **Classes**: `PascalCase` (e.g. `SchoolRepository`, `HomeParent`)
- **Screen files**: named after function: `*_screen.dart`, `*_list_screen.dart`, `*_create_screen.dart`
- **Models**: singular nouns (`Student`, `Grade`, `AppNotification`)
- **Barrel exports**: use `models.dart`, `repositories.dart`, `services.dart` for grouped exports
- **Language**: Vietnamese for UI text and comments, English for code identifiers

## Database Design

Full schema: [docs/database.md](docs/database.md)
ERD with relationships: [docs/erd.md](docs/erd.md)

**27 tables** covering: users, parents, students, teachers, classes, subjects, semesters, schedules, grades, semester_results, attendance, leave_requests, tuition_bills, payment_transactions, conversations, messages, announcements, notifications, club_registrations, attachments.

Key relationships:
- `users` has `role` field (PARENT | STUDENT | TEACHER) — 1 account = 1 role
- `students` ↔ `parents` via `student_guardians` (M:N with relationship: father/mother/guardian)
- `grades` = per-subject per-semester (oral, quiz_15p, mid_term, final, average)
- `semester_results` = summary per-student per-semester (gpa, rank, honor, conduct, academic_ability)

## Business Rules

See [docs/screen-flow.md](docs/screen-flow.md) for full screen flow and [docs/business_model.md](docs/business_model.md) for business model.

### Grades Simulation Mode
Students can toggle "Simulation" to preview grades without saving to DB — client-side only.

### Leave Request Flow
PH creates → GV approves/rejects → attendance auto-updates to "ABSENT_WITH_LEAVE"

### Announcements
GV selects multiple assigned classes (M:N via `announcement_classes`) and `target_role` filters PARENT/STUDENT/ALL. The announcement stays `PENDING` until Admin approves it, then it is published. Admin can also publish directly to every non-Admin account. Announcements support read/unread only; acknowledgement and reply workflows do not exist.

## Docs Index

| File | Content |
|------|---------|
| `docs/business_model.md` | Business model & feature overview |
| `docs/screen-flow.md` | Detailed screen flows for all 3 actors |
| `docs/erd.md` | Entity-Relationship Diagram (27 tables) |
| `docs/database.md` | Full MySQL schema, indexes, stored procedures, sample data |

## Current State (as of 2026-06-24)

- ✅ Flutter Mobile: Login, role selection, AppShell (4 tabs), home screens for all 3 actors
- ✅ UI screens: Grades, Attendance, Schedule, Leave Requests, Tuition, Announcements, Messages, Stats, Forms/Clubs
- ✅ Design system: Theme, colors, spacing, radius, text styles, reusable widgets
- ✅ Mock data layer: FakeApiClient with demo collections
- ✅ Unit tests: Grade parsing, analytics, repository/service tests
- 🔲 Backend API: Not yet built (Spring Boot)
- 🔲 Admin Web: Not yet built (React)
- 🔲 Real authentication: Not yet implemented (JWT planned)
- Only Edit, commit and push code on branch master
