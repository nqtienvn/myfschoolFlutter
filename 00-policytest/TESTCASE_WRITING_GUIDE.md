# TEST CASE WRITING GUIDE - Hướng dẫn viết test case markdown nhanh

> Tài liệu này hướng dẫn dev/tester/AI cách viết file test case markdown nhanh, đúng format, và đủ chi tiết.

---

## 1. Khi nào phải viết test case markdown?

Viết test case markdown khi:

- Bắt đầu code 1 UC mới.
- Sửa nghiệp vụ/API/database của UC đã có.
- Thêm/sửa tính năng có ảnh hưởng tới flow nghiệp vụ.
- Phát hiện bug nghiệp vụ và cần test case để verify fix.

Không cần viết test case markdown riêng cho:

- Chỉnh CSS/UI nhỏ không đổi logic.
- Refactor code không đổi behavior.
- Fix typo/comment.

---

## 2. Template nào dùng?

Luôn copy từ:

```text
Test_Cases/UC_TESTCASE_TEMPLATE.md
```

Đổi tên thành:

```text
Test_Cases/UCxx_<module-name>_TESTCASE.md
```

Ví dụ:

```text
Test_Cases/UC03_PaymentInvoice_TESTCASE.md
Test_Cases/UC01_Authentication_TESTCASE.md
Test_Cases/UC05_DietaryFoodOrdering_TESTCASE.md
```

---

## 3. Các phần bắt buộc phải điền

### 3.1 Document Control

Điền đầy đủ:

| Field | Ví dụ |
| :--- | :--- |
| **Document ID** | `MFS-UC03-TC-001` |
| **Use Case ID** | `UC03` |
| **Use Case Name** | `Payment & Invoice` |
| **Module** | `PAY` |
| **Version** | `1.0` |
| **Status** | `Draft` -> `In Review` -> `Approved` |
| **Author** | Tên người viết |
| **Reviewer** | Tên người review |
| **Created Date** | `2026-06-10` |
| **Last Updated** | `2026-06-10` |
| **Data Classification** | `Internal / PII / Sensitive-PII` tùy UC |

### 3.2 Use Case Summary

Viết ngắn gọn:

- Business Goal: 1-2 câu mô tả mục đích nghiệp vụ.
- Actors: liệt kê actor và vai trò trong UC.
- Scope: In Scope là gì, Out of Scope là gì.

### 3.3 Requirement Traceability

Trace tới requirement/source:

| Requirement ID | Source Document | Requirement Summary | Test Case IDs |
| :--- | :--- | :--- | :--- |
| `UCxx-BR-001` | `SRS/SRS_Summary.md` | Business rule | `TC-XXX-API-001` |
| `UCxx-API-001` | `SDS/BACKEND_ARCHITECTURE.md` | API endpoint behavior | `TC-XXX-API-002` |
| `UCxx-DB-001` | `docs/database.md` | DB constraint/table | `TC-XXX-INT-001` |

### 3.4 Test Case Summary

Liệt kê tất cả test case trong UC:

| TC ID | Type | Priority | Scenario | Status |
| :--- | :--- | :---: | :--- | :--- |
| `TC-PAY-UNIT-001` | Unit | Critical | Create invoice aggregates charges | Not Run |
| `TC-PAY-API-001` | API | High | Missing booking returns 404 | Not Run |
| `TC-PAY-SEC-001` | Security | Critical | Invalid VNPay hash rejected | Not Run |

Priority gợi ý:

- `Critical`: Lỗi làm UC không dùng được hoặc mất tiền/data/security.
- `High`: Lỗi làm nghiệp vụ chính sai.
- `Medium`: Lỗi validation/UX nhưng không chặn flow chính.
- `Low`: Edge case hiếm gặp.

### 3.5 Detailed Test Cases

Mỗi test case viết theo format:

```markdown
### TC-XXX-TYPE-NNN - [Tên test]

| Field | Value |
| :--- | :--- |
| **Type** | Unit / API / UI / Security / Integration / E2E |
| **Priority** | Critical / High / Medium / Low |
| **Feature Under Test** | `ClassName.methodName()` hoặc endpoint hoặc UI screen |
| **Requirement Ref** | `UCxx-BR-001` |
| **Test Data** | `TD-001`, `TD-002` |
| **Status** | Not Run / Pass / Fail / Blocked / Skipped |

#### Preconditions
- [Điều kiện trước khi chạy test]

#### Steps
1. Arrange: [Chuẩn bị]
2. Act: [Thực hiện action]
3. Assert: [Kiểm tra kết quả]

#### Expected Result
- [Kết quả đúng]

#### Actual Result
- [Điền sau khi chạy test]

#### Evidence
- Command: `[command đã chạy]`
- Screenshot/log: `[path nếu có]`
```

---

## 4. Quy tắc viết test case ID

Format:

```text
TC-<MODULE>-<TYPE>-<NNN>
```

Ví dụ:

```text
TC-PAY-UNIT-001
TC-PAY-API-002
TC-PAY-SEC-001
TC-AUTH-API-001
TC-BOOK-E2E-001
```

Module code:

| Module | Code |
| :--- | :--- |
| Authentication | `AUTH` |
| Booking | `BOOK` |
| Payment | `PAY` |
| Chat/Realtime | `CHAT` |
| Food/Dietary | `FOOD` |
| Admin | `ADMIN` |
| Staff | `STAFF` |

Type code:

| Type | Code |
| :--- | :--- |
| Unit test | `UNIT` |
| Integration test | `INT` |
| API test | `API` |
| UI test | `UI` |
| Security test | `SEC` |
| End-to-end test | `E2E` |

---

## 5. Quy tắc viết test data

Ghi rõ test data trong section `Test Data`:

| Data ID | Type | Value / Setup | Used By | Note |
| :--- | :--- | :--- | :--- | :--- |
| `TD-001` | User | `[email/password/role giả]` | `TC-XXX-*` | Không dùng password thật |
| `TD-002` | DB Seed | `[booking/invoice/room/service]` | `TC-XXX-*` | Reset được |
| `TD-003` | API Payload | `[JSON sample]` | `TC-XXX-API-001` | Valid payload |
| `TD-004` | Invalid Payload | `[JSON sample]` | `TC-XXX-API-002` | Validation test |

**Bắt buộc**: dữ liệu test phải là synthetic/giả lập, không dùng data thật.

---

## 6. Quy tắc ghi test status

| Status | Khi nào dùng |
| :--- | :--- |
| `Not Run` | Test case đã viết nhưng chưa chạy |
| `Pass` | Đã chạy và đạt expected result |
| `Fail` | Đã chạy nhưng không đạt expected result |
| `Blocked` | Không chạy được vì thiếu môi trường/dependency/data/permission |
| `Skipped` | Reviewer đồng ý bỏ qua test này (phải ghi lý do) |

**Không được** đánh `Pass` nếu chưa thật sự chạy hoặc không có evidence.

---

## 7. Quy tắc ghi actual result & evidence

Sau khi chạy test, phải cập nhật:

```markdown
#### Actual Result
- Test executed on: 2026-06-10 17:00
- Status: Pass
- Invoice created with correct amounts.
- DB verified: status = UNPAID, amountDue = 700.00

#### Evidence
- Command: `mvn test -Dtest=InvoiceServiceImplTest#createInvoice_validBooking_returnsInvoiceDTOWithCorrectAmounts`
- Output: [paste relevant output hoặc link screenshot]
```

Nếu fail:

```markdown
#### Actual Result
- Test executed on: 2026-06-10 17:00
- Status: Fail
- Expected status PAID but got UNPAID.

#### Evidence
- Command: `mvn test -Dtest=...`
- Error log: [paste stack trace relevant part]
- Screenshot: `docs/test-evidence/TC-PAY-API-002-fail-20260610.png`
```

Nếu blocked:

```markdown
#### Actual Result
- Status: Blocked
- Reason: VNPay sandbox credentials not available in CI environment.

#### Evidence
- Attempt: `mvn test -Dtest=...`
- Error: Connection refused to sandbox.vnpayment.vn
```

---

## 8. State Transition Matrix (nếu UC có trạng thái)

Bắt buộc nếu UC quản lý entity có trạng thái như booking, invoice, appointment, order:

| Entity | From State | Action | To State | Allowed? | Test Case |
| :--- | :--- | :--- | :--- | :---: | :--- |
| Invoice | `UNPAID` | VNPay success callback | `PAID` | Yes | `TC-PAY-API-002` |
| Invoice | `UNPAID` | Invalid hash callback | `UNPAID` | No state change | `TC-PAY-SEC-001` |
| Invoice | `PAID` | Create payment URL | `PAID` | No | `TC-PAY-API-003` |

---

## 9. Database Verification (nếu UC ghi database)

Bắt buộc ghi SQL query để verify:

| Check ID | SQL / Verification | Expected Result | Test Case |
| :--- | :--- | :--- | :--- |
| `DB-001` | `SELECT status FROM invoice WHERE id = ?` | `PAID` | `TC-PAY-API-002` |
| `DB-002` | `SELECT payment_time FROM invoice WHERE id = ?` | Not null | `TC-PAY-API-002` |

---

## 10. Exit Criteria

Bắt buộc ghi exit criteria:

```markdown
UC này chỉ được nghiệm thu khi:

- [ ] Tất cả test case Priority Critical/High đã Pass.
- [ ] Không còn bug Critical/High đang Open.
- [ ] Build frontend/backend pass.
- [ ] Không có dữ liệu nhạy cảm bị leak.
- [ ] Database state đúng sau test.
- [ ] Reviewer đã kiểm tra và approve.
```

---

## 11. Checklist trước khi submit test case

- [ ] Đã copy từ `UC_TESTCASE_TEMPLATE.md`.
- [ ] Đã đổi tên file đúng format `UCxx_*_TESTCASE.md`.
- [ ] Document Control đã điền đầy đủ.
- [ ] Use Case Summary rõ nghĩa.
- [ ] Requirement Traceability có trace tới source.
- [ ] Test Case Summary liệt kê đầy đủ test case.
- [ ] Detailed Test Cases có ít nhất: happy path, validation/error, permission (nếu có auth).
- [ ] Test Data không dùng dữ liệu thật.
- [ ] Test Status được ghi rõ: Not Run / Pass / Fail / Blocked.
- [ ] Actual Result và Evidence được điền sau khi chạy test.
- [ ] State Transition Matrix nếu UC có trạng thái.
- [ ] Database Verification nếu UC ghi database.
- [ ] Exit Criteria được định nghĩa.
- [ ] File đã được update vào `TESTCASE_INDEX.md`.

---

## 12. Common mistakes

| Lỗi | Giải thích | Fix |
| :--- | :--- | :--- |
| Test case quá chung | "Test login works" | Viết rõ: "TC-AUTH-API-001 - Valid credentials return 200 and JWT token" |
| Không có expected result cụ thể | "API trả về đúng" | Ghi rõ HTTP status, JSON field, DB state |
| Không trace requirement | Không rõ test case này cover requirement nào | Điền `Requirement Ref` |
| Đánh Pass mà không có evidence | Status = Pass nhưng không có command/screenshot | Phải ghi command và output |
| Test case duplicate | Có 2 test case giống nhau | Merge hoặc phân biệt rõ điều kiện khác nhau |
| Test data có dữ liệu thật | Email/phone/passport thật | Dùng synthetic data |

---

## 13. Example flow viết test case cho UC mới

1. Nhận task: "Implement UC03 Payment & Invoice".
2. Copy `Test_Cases/UC_TESTCASE_TEMPLATE.md` thành `Test_Cases/UC03_PaymentInvoice_TESTCASE.md`.
3. Điền Document Control: UC03, module PAY, author, date.
4. Đọc SRS/SDS/PAYMENT_TDD để hiểu requirement.
5. Viết Use Case Summary: business goal, actors, scope.
6. Trace requirement vào bảng Requirement Traceability.
7. Liệt kê test case trong Test Case Summary:
   - Happy path: create invoice, payment success.
   - Validation: missing booking, paid invoice guard.
   - Security: invalid hash, unauthorized.
8. Viết chi tiết từng test case: preconditions, steps, expected result.
9. Thêm State Transition Matrix cho invoice status.
10. Thêm Database Verification.
11. Thêm Exit Criteria.
12. Code UC03.
13. Chạy test.
14. Cập nhật Actual Result & Evidence.
15. Update `TESTCASE_INDEX.md`.
16. Submit PR.

---

## 14. Tips nâng cao

- Nếu UC phức tạp, chia nhỏ thành nhiều file test case theo sub-module.
- Nếu UC có nhiều actor, viết test case theo từng actor role.
- Nếu UC có nhiều state, vẽ state diagram trước khi viết test case.
- Nếu test case quá dài, tách phần automated test vào file riêng và link vào markdown.

---

## 15. Khi cần help

Nếu không biết viết test case cho một tình huống cụ thể:

- Xem `Test_Cases/UC03_PaymentInvoice_TESTCASE.md` làm ví dụ.
- Đọc `Quy_tac_AI_Test/TESTING_GUIDELINES.md` để hiểu test pyramid.
- Đọc `Backend/docs/PAYMENT_TDD.md` để thấy format TDD chính thức.
- Hỏi tech lead/reviewer.
