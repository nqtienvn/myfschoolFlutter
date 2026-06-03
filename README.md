# MyFschool

MyFschool là app sổ liên lạc điện tử gồm Flutter Mobile, Flutter Admin Web,
Backend API và MySQL.

## Architecture

Flutter Mobile / Flutter Admin Web
-> HTTPS + JWT
-> Backend REST API
-> Service / Repository
-> MySQL

## Rule

Flutter không kết nối trực tiếp MySQL. Flutter chỉ gọi Backend API.
Backend chịu trách nhiệm xác thực, phân quyền, nghiệp vụ và truy vấn SQL.

## Modules

- Auth
- Users
- Students
- Classes
- Schedule
- Attendance
- Grades
- Announcements
- Messages
- Tuition
- Admin Reports

## Setup Checklist

- [x] Git installed
- [x] Dart installed
- [x] Flutter installed
- [x] VS Code installed
- [x] Android toolchain ready
- [ ] Create first Flutter app
- [ ] Create backend health endpoint

## Request Flow Example

Parent opens Grades screen
-> Flutter calls GET /api/students/{id}/grades
-> Backend checks JWT and permission
-> Backend queries MySQL
-> Backend returns JSON
-> Flutter renders loading/error/empty/success UI
