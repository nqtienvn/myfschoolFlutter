# USE CASE TEST CASE TEMPLATE

> Dùng file này làm mẫu để tạo test case cho từng Use Case. Copy file này thành `UCxx_<ten-use-case>_TESTCASE.md`, sau đó điền đầy đủ thông tin.

---

## Document Control

| Field | Value |
| :--- | :--- |
| **Document ID** | `MFS-UCxx-TC-001` |
| **Use Case ID** | `UCxx` |
| **Use Case Name** | `[Tên use case]` |
| **Module** | `[AUTH / CHAT / GRADE / NOTIFY / SCHEDULE / ADMIN]` |
| **Version** | `1.0` |
| **Status** | `Draft / In Review / Approved / Deprecated` |
| **Author** | `[Tên người viết]` |
| **Reviewer** | `[Tên người review]` |
| **Created Date** | `YYYY-MM-DD` |
| **Last Updated** | `YYYY-MM-DD` |
| **Data Classification** | `Public / Internal / PII / Sensitive-PII` |

---

## Changelog

> Không xóa lịch sử cũ. Mỗi lần sửa phải thêm một dòng mới.

| Date | Author | Change |
| :--- | :--- | :--- |
| YYYY-MM-DD | [Name] | Created test case document for UCxx |

---

## 1. Use Case Summary

### 1.1 Business Goal

Mô tả mục tiêu nghiệp vụ của UC này.

Ví dụ:

> Phụ huynh và Giáo viên có thể nhắn tin trực tiếp qua WebSocket thời gian thực. Tin nhắn được lưu an toàn, hỗ trợ trạng thái đã gửi/đã nhận/đã xem và đồng bộ khi kết nối lại.

### 1.2 Actors

| Actor | Role in UC |
| :--- | :--- |
| Parent / Student | [Mô tả vai trò] |
| Teacher | [Nếu có] |
| Admin/Manager | [Nếu có] |
| System (WebSocket/REST) | [Nếu có] |

### 1.3 Scope

| In Scope | Out of Scope |
| :--- | :--- |
| [Luồng được test] | [Luồng không test trong file này] |

---

## 2. Requirement Traceability

| Requirement ID | Source Document | Requirement Summary | Test Case IDs |
| :--- | :--- | :--- | :--- |
| `UCxx-BR-001` | `docs/superpowers/specs/` | [Business rule] | `TC-XXX-API-001` |
| `UCxx-API-001` | `[API/Controller/WebSocket path]` | [API behavior] | `TC-XXX-API-002` |
| `UCxx-DB-001` | `docs/database.md` | [DB constraint/table/column] | `TC-XXX-INT-001` |
| `UCxx-SEC-001` | `00-policytest/TESTING_GUIDELINES.md` | [Auth/privacy rule] | `TC-XXX-SEC-001` |

---

## 3. Preconditions & Assumptions

### 3.1 Preconditions

- [ ] Backend đang chạy local tại `[host:port]` nếu test API.
- [ ] Frontend đang chạy local tại `[host:port]` nếu test UI.
- [ ] Database đã seed dữ liệu test.
- [ ] User test đã có role phù hợp.
- [ ] Không sử dụng dữ liệu thật của khách hàng.

### 3.2 Assumptions

| ID | Assumption | Impact if Wrong |
| :--- | :--- | :--- |
| `ASM-001` | [Giả định] | [Ảnh hưởng] |

---

## 4. Test Data

> Tất cả dữ liệu test phải là synthetic hoặc anonymized.

| Data ID | Type | Value / Setup | Used By | Note |
| :--- | :--- | :--- | :--- | :--- |
| `TD-001` | User | `[email/password/role giả]` | `TC-XXX-*` | Không dùng password thật |
| `TD-002` | DB Seed | `[conversation/message/participant]` | `TC-XXX-*` | Reset được |
| `TD-003` | API Payload | `[JSON sample]` | `TC-XXX-API-001` | Valid payload |
| `TD-004` | Invalid Payload | `[JSON sample]` | `TC-XXX-API-002` | Validation test |

---

## 5. Test Environment

| Item | Value |
| :--- | :--- |
| Frontend | Flutter (SDK ^3.11.5) |
| Backend | Spring Boot 3.4.5 (Java 21) |
| Database | MySQL / H2 |
| API Base URL | `http://localhost:8080/api` |
| WebSocket URL | `ws://localhost:8080/chat?token=<JWT>` |
| Test Tools | JUnit 5, Mockito, Flutter Widget Test, Postman/wscat |
| Build Commands | `flutter test`, `mvn test` |

---

## 6. Test Case Summary

| TC ID | Type | Priority | Scenario | Status |
| :--- | :--- | :---: | :--- | :--- |
| `TC-XXX-UNIT-001` | Unit | High | [Service happy path] | Not Run |
| `TC-XXX-API-001` | API | High | [API happy path] | Not Run |
| `TC-XXX-API-002` | API | Medium | [Validation error] | Not Run |
| `TC-XXX-SEC-001` | Security | Critical | [Unauthorized/forbidden] | Not Run |
| `TC-XXX-UI-001` | UI | Medium | [UI flow] | Not Run |
| `TC-XXX-E2E-001` | E2E | High | [Full business flow] | Not Run |

Status hợp lệ:

```text
Not Run / Pass / Fail / Blocked / Skipped
```

`Skipped` chỉ được dùng khi reviewer đồng ý và có lý do.

---

## 7. Detailed Test Cases

### TC-XXX-UNIT-001 - [Tên test unit]

| Field | Value |
| :--- | :--- |
| **Type** | Unit |
| **Priority** | High |
| **Layer** | Backend Service / Util / Mapper |
| **Feature Under Test** | `[ClassName.methodName()]` |
| **Requirement Ref** | `UCxx-BR-001` |
| **Test Data** | `TD-001`, `TD-002` |
| **Automation File** | `[path/to/Test.java]` |
| **Status** | Not Run |

#### Preconditions

- [Điều kiện trước khi chạy test]

#### Steps

1. Arrange: [Chuẩn bị mock/input].
2. Act: [Gọi method].
3. Assert: [Kiểm tra output/state/repository call].

#### Expected Result

- [Kết quả đúng].

#### Actual Result

- [Điền sau khi chạy].

#### Evidence

- Build/test command: `[command]`
- Screenshot/log path: `[path nếu có]`

---

### TC-XXX-API-001 - [Tên test API happy path]

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | High |
| **Endpoint** | `[METHOD] /api/...` |
| **Auth Required** | Yes/No |
| **Required Role** | `[PARENT / STUDENT / TEACHER / ADMIN]` |
| **Requirement Ref** | `UCxx-API-001` |
| **Test Data** | `TD-003` |
| **Status** | Not Run |

#### Request

```http
METHOD /api/... HTTP/1.1
Authorization: Bearer <test-token>
Content-Type: application/json
```

```json
{
  "field": "value"
}
```

#### Steps

1. Login hoặc chuẩn bị token test.
2. Gửi request tới endpoint.
3. Kiểm tra HTTP status.
4. Kiểm tra response body.
5. Kiểm tra database nếu UC có thay đổi dữ liệu.

#### Expected Result

- HTTP Status: `200 / 201 / 204`.
- Response body đúng schema.
- Database state đúng.
- Không có dữ liệu nhạy cảm bị trả về nếu không cần thiết.

#### Actual Result

- [Điền sau khi chạy].

#### Evidence

```bash
# curl/postman command hoặc screenshot
```

---

### TC-XXX-API-002 - [Tên test validation/error]

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | Medium |
| **Endpoint** | `[METHOD] /api/...` |
| **Requirement Ref** | `UCxx-API-001` |
| **Test Data** | `TD-004` |
| **Status** | Not Run |

#### Invalid Input

```json
{
  "invalidField": "invalidValue"
}
```

#### Steps

1. Gửi request với payload sai/thiếu field.
2. Kiểm tra response lỗi.
3. Kiểm tra database không bị thay đổi ngoài ý muốn.

#### Expected Result

- HTTP Status: `400 / 404 / 409` tùy case.
- Có error code rõ ràng.
- Message đủ hiểu, không leak stack trace.
- Không tạo/cập nhật dữ liệu sai.

#### Actual Result

- [Điền sau khi chạy].

---

### TC-XXX-SEC-001 - [Tên test phân quyền/bảo mật]

| Field | Value |
| :--- | :--- |
| **Type** | Security |
| **Priority** | Critical |
| **Endpoint/UI** | `[endpoint hoặc màn hình]` |
| **Security Rule** | `[Role/PII/Encryption/No secret leak]` |
| **Requirement Ref** | `UCxx-SEC-001` |
| **Status** | Not Run |

#### Attack / Negative Scenario

Mô tả hành vi không được phép, ví dụ:

- User chưa login kết nối WebSocket hoặc gọi API cần auth.
- User cố gửi tin nhắn tới cuộc hội thoại không phải thành viên.
- Gửi token sai/hết hạn khi handshake WebSocket.
- Payload có script/injection trong nội dung tin nhắn.
- User cố đọc tin nhắn của hội thoại người khác (IDOR).

#### Steps

1. Chuẩn bị user/token không đủ quyền hoặc payload nguy hiểm.
2. Gọi API/mở UI.
3. Kiểm tra hệ thống chặn đúng.

#### Expected Result

- HTTP Status: `401` nếu chưa xác thực.
- HTTP Status: `403` nếu không đủ quyền.
- Không trả về dữ liệu nhạy cảm.
- Không ghi secret/password/token vào log.

#### Actual Result

- [Điền sau khi chạy].

---

### TC-XXX-UI-001 - [Tên test UI]

| Field | Value |
| :--- | :--- |
| **Type** | UI |
| **Priority** | Medium |
| **Screen/Page** | `[Flutter screen/widget]` |
| **Route** | `[Tab / Named route]` |
| **Requirement Ref** | `UCxx-BR-001` |
| **Status** | Not Run |

#### Steps

1. Mở route `[route]`.
2. Thực hiện thao tác người dùng.
3. Kiểm tra loading/error/success state.
4. Kiểm tra dữ liệu hiển thị.
5. Kiểm tra responsive nếu cần.

#### Expected Result

- UI không crash.
- Form validate đúng.
- Loading state rõ ràng.
- Error message thân thiện.
- Không hiển thị field mà role hiện tại không được xem.

#### Actual Result

- [Điền sau khi chạy].

---

### TC-XXX-E2E-001 - [Tên test luồng end-to-end]

| Field | Value |
| :--- | :--- |
| **Type** | E2E |
| **Priority** | High |
| **Business Flow** | `[Từ bước A đến bước Z]` |
| **Actors** | `[Customer, Receptionist, Admin...]` |
| **Requirement Ref** | `UCxx-BR-001` |
| **Status** | Not Run |

#### Steps

1. [Actor 1] thực hiện bước 1.
2. [Actor 1] thực hiện bước 2.
3. [Actor 2] xử lý tiếp.
4. Kiểm tra kết quả cuối cùng trên UI/API/DB.

#### Expected Result

- Flow hoàn tất đúng.
- Trạng thái nghiệp vụ chuyển đúng.
- Dữ liệu liên quan đồng bộ.
- Không phát sinh side effect sai.

#### Actual Result

- [Điền sau khi chạy].

---

## 8. State Transition Matrix

> Bắt buộc nếu UC có trạng thái như tin nhắn, kết nối WebSocket, cuộc hội thoại, hoặc bất kỳ entity có lifecycle.

| Entity | From State | Action | To State | Allowed? | Test Case |
| :--- | :--- | :--- | :--- | :---: | :--- |
| `[Entity]` | `[OLD]` | `[Action]` | `[NEW]` | Yes | `TC-XXX-*` |
| `[Entity]` | `[OLD]` | `[Invalid Action]` | `[NEW]` | No | `TC-XXX-*` |

---

## 9. Database Verification

| Check ID | SQL / Verification | Expected Result | Test Case |
| :--- | :--- | :--- | :--- |
| `DB-001` | `SELECT ...` | [Expected row/value] | `TC-XXX-INT-001` |
| `DB-002` | `SELECT ...` | [No sensitive data/plaintext] | `TC-XXX-SEC-001` |

```sql
-- Sample only. Replace with actual table/column names.
SELECT *
FROM dbo.[table_name]
WHERE [id_column] = [test_id];
```

---

## 10. Build & Test Execution Log

| Date | Runner | Command | Result | Note |
| :--- | :--- | :--- | :--- | :--- |
| YYYY-MM-DD | [Name/AI] | `npm run build` | Not Run |  |
| YYYY-MM-DD | [Name/AI] | `mvn test` | Not Run |  |
| YYYY-MM-DD | [Name/AI] | `[manual/API test]` | Not Run |  |

---

## 11. Defects Found

| Defect ID | Test Case | Severity | Description | Status | Owner |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `BUG-UCxx-001` | `TC-XXX-*` | High | [Mô tả lỗi] | Open | [Name] |

---

## 12. Exit Criteria

UC này chỉ được nghiệm thu khi:

- [ ] Tất cả test case Priority Critical/High đã Pass.
- [ ] Không còn bug Critical/High đang Open.
- [ ] Build frontend/backend pass.
- [ ] Không có dữ liệu nhạy cảm bị leak.
- [ ] Database state đúng sau test.
- [ ] Reviewer đã kiểm tra và approve.

---

## 13. Final Review

| Role | Name | Decision | Date | Note |
| :--- | :--- | :--- | :--- | :--- |
| Developer | [Name] | Pending | YYYY-MM-DD |  |
| Tester/QA | [Name] | Pending | YYYY-MM-DD |  |
| Tech Lead | [Name] | Pending | YYYY-MM-DD |  |
