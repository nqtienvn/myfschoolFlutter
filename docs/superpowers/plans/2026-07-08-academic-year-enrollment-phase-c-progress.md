# Phase C Progress

Plan: `docs/superpowers/plans/2026-07-08-academic-year-enrollment-phase-c.md`

## 2026-07-08

- Kiểm kê: Phase C backend artifacts already partially present (FeeCategory, FeeTemplate, controllers, services, DTOs all exist). Backend compiles.
- Critical bug found: `FeeTemplateServiceImpl.generateBills()` checks duplicate via `existsByStudentIdAndSemesterIdAndFeeTemplateId` but never sets `bill.setFeeTemplate(ft)` on the new bill, making duplicate guard useless.
- Fix pass 1: `bill.setFeeTemplate(ft)` added in `FeeTemplateServiceImpl.java:88`. Unique constraint added to `TuitionBill` entity: `(student_id, semester_id, fee_template_id)`. `TuitionBillRepository` gained `existsByFeeTemplateId` for safe delete guard.
- Fix pass 2: `FeeTemplateServiceImpl.delete()` now blocks deletion of templates that already generated bills (`existsByFeeTemplateId`). Year-class/semester consistency validated in `create()`.
- DTO fix: `TuitionBillDto` now includes `feeTemplateId` and `feeTemplateName`. `TuitionBillServiceImpl.toDto()` maps both fields.
- Tests added: `TuitionIntegrationTest` extended with 4 new tests:
  - `create_fee_category_admin_only` — POST `/api/fee-categories` returns name
  - `create_fee_template_counts_seeded_students` — POST `/api/fee-templates` returns correct studentCount=3
  - `generate_bills_creates_once_then_skips_duplicates` — generate → 3 created/0 skipped; re-generate → 0 created/3 skipped; GET bills still 3
  - `duplicate_fee_template_fails` — conflict on duplicate category+class+semester
- Evidence: `mvn -f backend/pom.xml test` → `BUILD SUCCESS`, 79 tests, 0 failures/errors.
- Admin-web Phase C: `FeeCategoriesPage.tsx` created, `FeeTemplatesPage.tsx` created, `TuitionPage.tsx` updated (removed manual bill creation, added template column). `App.tsx` updated with nav items for fee categories/templates.
- Admin-web verify: `npm --prefix admin-web run build` → `BUILD SUCCESS`. IDE diagnostics all clear. `git diff --check` passed.
- Skipped: `FeeCategoriesPage` and `FeeTemplatesPage` nav labels use English keys `fee-categories`/`fee-templates` matching API paths — consistent with existing pattern but labels are Vietnamese.
- Next: Phase D (Attendance Session + Detail refactor) — depends on Phase B only.
