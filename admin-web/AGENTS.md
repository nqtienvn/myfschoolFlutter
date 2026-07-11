# Admin Web Academic-Year Scope Rule

## Mandatory invariant

Every operational feature in `admin-web` must use the academic year selected in the shared application context as its root data scope.

- A page must receive `selectedYearId` (and `selectedSemesterId` when applicable) from the shared app context. It must not silently select the current/latest year or keep a private year selection.
- Every list, search, create, update, delete, import, export, validation, and publish request for year-owned data must include `academicYearId` directly or use an identifier whose ownership the backend resolves and verifies against that year.
- The backend is the authorization and data-isolation boundary. Never trust only the frontend filter. It must reject class/semester/subject/shift/period/assignment identifiers that belong to another academic year.
- When `selectedYearId` changes, clear page state, selections, pagination, drafts, and cached results before loading the new year's data. Include the year in query/cache keys.
- A selected semester must belong to the selected academic year. Classes, enrollments, teaching assignments, timetables, attendance, grades, fees, announcements, and reports must never cross that boundary.
- Global catalogs and identities (for example users, teachers, the subject catalog, the shift catalog, and the period catalog) may be loaded globally only for catalog/identity administration. Inside a year-owned workflow, usable options must be intersected with that year's applied configuration.
- Do not hard-code year configuration such as subjects, shifts, period counts, period numbers, or semester dates. Read them from the selected academic year's configuration.
- New backend integration tests must create at least two academic years and prove that reads and writes cannot leak or attach data across years.
- Any intentional exception must be documented in the feature PR and must be limited to global security, identity, or catalog administration.

## Review checklist

Reject an admin feature if any answer is "no":

1. Does the page derive its scope from the shared selected academic year?
2. Does every operational API call carry or securely derive that scope?
3. Does Spring validate ownership and cross-year relationships?
4. Is local state reset when the year changes?
5. Is there a cross-year isolation test?
