# Phase B Progress

Plan: `docs/superpowers/plans/2026-07-08-academic-year-enrollment-phase-b.md`

## 2026-07-08

- Kiểm kê: Phase A complete. Working tree already contains partial Phase B/C/D/E/F/G artifacts; backend compile passes (`mvn -f backend/pom.xml compile`). No Phase B progress file existed before this.
- Current Phase B state: TeachingAssignment/HomeroomAssignment entities, repositories, DTOs, services, controllers exist. Schedule entity uses `assignment_id`. `ClassDetailDto` returns `assignments` + `homeroomTeacher`.
- Current blocker: backend tests fail after Phase B partial changes. `mvn -f backend/pom.xml test` summary: 75 tests, 9 failures, 0 errors. Failures: AttendanceIntegrationTest 403 (teacher access), ScheduleIntegrationTest 400 (old schedule payload/tests).
- Fix pass 1: seed `TeachingAssignment` in `BaseIntegrationTest`, update teacher access check to use active teaching assignment, update `ScheduleIntegrationTest` payloads to `assignmentId`. Evidence: `mvn -f backend/pom.xml test -Dtest=AttendanceIntegrationTest,ScheduleIntegrationTest` => `BUILD SUCCESS`, 14 tests, 0 failures/errors.
- Full backend verification after pass 1: `mvn -f backend/pom.xml test` => `BUILD SUCCESS`, 75 tests, 0 failures/errors.
- Task 7 cleanup: complete. `ClassSubject` entity/repository/DTO/request deleted; `SchoolClass.classSubjects` removed; `BaseIntegrationTest` seeds `TeachingAssignment`; leave-request teacher access uses `TeachingAssignmentRepository`; dead SubjectService class-subject methods removed. Evidence: `mvn -f backend/pom.xml compile` => `BUILD SUCCESS`, 283 source files.
- Admin-web Phase B update: complete. `AssignmentsPage` now uses `/teaching-assignments` and `/homeroom-assignments`; `SchedulesPage` loads `/teaching-assignments?classId&semesterId` and creates schedules with `assignmentId`. No admin-web reference remains to old `/classes/{id}/subjects`, `classes/subjects`, `data.subjects`, or `ClassSubject`.
- Admin-web verification: `git diff --check -- admin-web/src/pages/AssignmentsPage.tsx admin-web/src/pages/SchedulesPage.tsx` passed (only LF→CRLF warnings). IDE diagnostics for both changed TSX files passed. `npm --prefix admin-web run build` was blocked twice by permission classifier, so full Vite/tsc build still needs user/manual run.
- Next phase after Phase B: Phase C (`docs/superpowers/plans/2026-07-08-academic-year-enrollment-phase-c.md`) — FeeCategory/FeeTemplate and tuition bill generation. Phase D depends on Phase B; Phase E depends on Phase B; Phase F depends on D+E; Phase G bulk APIs later; Phase H master data can run independently.
