# Academic Year + Enrollment Phase A Design

## Context

Current backend stores `academicYear` as a `VARCHAR` on `classes`, `semesters`, `class_subjects`, and `student_classes`. This blocks FK constraints, renaming, consistent filtering, and safe long-term workflows like transfer/promotion. User chose Phase A only and API switch to `academicYearId` now.

## Scope

Phase A changes only the school-year foundation:

- Add `AcademicYear` as source of truth.
- Change `SchoolClass` and `Semester` from `academicYear: String` to `academicYear: AcademicYear` FK.
- Add `Semester.order` for sorting.
- Replace `StudentClass` with `Enrollment` containing dates/status.
- Change backend DTO/API/admin-web from `academicYear` string input to `academicYearId`.

Out of scope:

- `TeachingAssignment`, `HomeroomAssignment`, `Schedule.assignment_id`.
- `FeeTemplate` and generate bill API.
- `AttendanceSession/AttendanceDetail`.
- `GradeBook/GradeItem/StudentScore`.
- Bulk APIs: initialize year, promote, imports.
- Extra master data: room/period/shift/school.

## Data model

### AcademicYear

Fields:

- `id: Long`
- `name: String` (`2026-2027`)
- `startDate: LocalDate`
- `endDate: LocalDate`
- `status: AcademicYearStatus`

Enum:

- `DRAFT`
- `ACTIVE`
- `CLOSED`

Rules:

- `name` unique.
- Only one `ACTIVE` year should exist. Enforce in service in Phase A; DB partial unique index is DB-specific and skipped.

### SchoolClass

Replace:

- `String academicYear`

With:

- `AcademicYear academicYear`

Unique:

- `(name, academic_year_id)`

### Semester

Replace:

- `String academicYear`

With:

- `AcademicYear academicYear`
- `Integer order`

Unique:

- `(name, academic_year_id)`
- `(academic_year_id, order)`

### Enrollment

Replace `StudentClass` entity/table concept.

Fields:

- `id`
- `student`
- `cls`
- `academicYear`
- `joinDate`
- `leaveDate`
- `status`

Enum:

- `ACTIVE`
- `LEFT`
- `TRANSFERRED`

Unique in Phase A:

- `(student_id, academic_year_id, class_id)`

Service-level rule:

- One active enrollment per student per academic year.

## API contract

### AcademicYear API

Add minimal admin endpoints:

- `GET /api/academic-years`
- `POST /api/academic-years`
- `PUT /api/academic-years/{id}`
- `PUT /api/academic-years/{id}/status`

DTOs:

- `AcademicYearDto(id, name, startDate, endDate, status)`
- `CreateAcademicYearRequest(name, startDate, endDate, status)`
- `UpdateAcademicYearStatusRequest(status)`

### Classes API

Change:

- `GET /api/classes?academicYear=2026-2027`

To:

- `GET /api/classes?academicYearId=1`

Change request:

- `CreateClassRequest.academicYearId`

Change DTO response:

- `ClassDto.academicYearId`
- `ClassDto.academicYearName`

### Semesters API

Change:

- `GET /api/semesters?academicYear=2026-2027`

To:

- `GET /api/semesters?academicYearId=1`

Change request:

- `CreateSemesterRequest.academicYearId`
- `CreateSemesterRequest.order`

Change DTO response:

- `SemesterDto.academicYearId`
- `SemesterDto.academicYearName`
- `SemesterDto.order`

## Admin web changes

Update pages that currently use `academicYear` string:

- `admin-web/src/pages/ClassesPage.tsx`
- `admin-web/src/pages/SemestersPage.tsx`
- `admin-web/src/pages/AssignmentsPage.tsx`
- `admin-web/src/pages/SchedulesPage.tsx`
- `admin-web/src/pages/TuitionPage.tsx`

Minimal UI:

- Load `/academic-years`.
- Select active year by default if present, otherwise first year.
- Send `academicYearId` in class/semester creation and filters.
- Display `academicYearName` where table previously displayed `academicYear`.

## Migration strategy

No production migration is required in this repo state because schema is managed from JPA/H2 tests and existing MySQL SQL docs are reference. If Flyway baselines become authoritative later, add a SQL migration then.

Test seed updates:

- `BaseIntegrationTest` creates one `AcademicYear` first.
- Classes, semesters, enrollments reference it.

## Verification

Backend:

- Add/update integration tests for academic year CRUD/status.
- Update existing class/semester tests to use `academicYearId`.
- Run `mvn -f backend/pom.xml test` or targeted tests first if full suite is slow.

Admin web:

- Run `npm --prefix admin-web run build`.
- Manual check: create academic year, create semesters/classes under selected year, list filters use IDs.

## Deliberate simplifications

- Service-level single `ACTIVE` academic year, not DB-level partial unique index.
- No backward-compatible `academicYear` string API.
- No bulk APIs in Phase A.
