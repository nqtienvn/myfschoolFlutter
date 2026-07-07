# TESTING GUIDELINES - Tiêu chuẩn kiểm thử cho MyFschool Electronic Contact Book

> Tài liệu này tập trung vào quy chuẩn kiểm thử kỹ thuật và best practices.

---

## 1. Test Pyramid & Coverage Target

Dự án áp dụng test pyramid chuẩn:

```text
              /\
             /E2E\       <- Ít, chậm, bao trùm luồng business quan trọng
            /------\
           /  API   \    <- Vừa, test endpoint/controller/validation
          /----------\
         / Unit/Int  \   <- Nhiều, nhanh, test logic/service/repo
        /--------------\
```

### 1.1 Coverage target

| Layer | Target | Rationale |
| :--- | :--- | :--- |
| Unit (Service/Util) | ≥ 80% | Core business logic phải có test tốt |
| Integration (Repo/DB) | ≥ 70% | Truy vấn SQL và mapping entity quan trọng |
| API (Controller) | ≥ 80% | Endpoint là contract với frontend |
| E2E | Key flows only | Đắt, chậm; chỉ test happy path quan trọng nhất |

---

## 2. Test Naming Convention

### 2.1 Java (JUnit 5)

Chuẩn method name:

```java
@Test
void methodName_condition_expectedBehavior()
```

Ví dụ:

```java
@Test
void createInvoice_validBooking_returnsInvoiceDTOWithCorrectAmounts()

@Test
void createInvoice_missingBooking_throwsBusinessException()

@Test
void processVNPayCallback_invalidHash_throwsBusinessException()
```

### 2.2 Test class structure

```java
@SpringBootTest // hoặc @WebMvcTest, @DataJpaTest tùy layer
class InvoiceServiceImplTest {
    
    @Mock
    private BookingRepository bookingRepository;
    
    @InjectMocks
    private InvoiceServiceImpl invoiceService;
    
    @BeforeEach
    void setUp() {
        // Arrange common setup
    }
    
    @Test
    void testName_condition_expected() {
        // Arrange
        // Act
        // Assert
    }
}
```

---

## 3. Test Data Management

### 3.1 Synthetic data cho test

Tất cả test data phải là synthetic/giả lập:

```java
// GOOD
User testUser = User.builder()
    .email("test.customer@example.com")
    .phone("0901234567")
    .firstName("Test")
    .lastName("Customer")
    .build();

// BAD - không dùng dữ liệu thật
User testUser = User.builder()
    .email("nguyenvana@gmail.com") // email thật
    .phone("0912345678") // số thật
    .build();
```

### 3.2 Test database

Ưu tiên:

1. H2 in-memory nếu schema đơn giản và không dùng SQL Server-specific syntax.
2. Test container SQL Server nếu cần SQL Server thật.
3. Nếu không chạy được container, seed data vào DB dev riêng và cleanup sau test.

Bắt buộc cleanup:

```java
@AfterEach
void tearDown() {
    invoiceRepository.deleteAll(); // hoặc deleteById(testIds)
}
```

Không để test data tồn tại lâu dài gây nhiễu test khác.

### 3.3 Mock external services

VNPay, email, SMS, payment gateway phải mock:

```java
@MockBean
private VNPayService vnpayService;

@Test
void createPaymentUrl_validInvoice_returnsUrl() {
    when(vnpayService.buildPaymentUrl(any())).thenReturn("https://sandbox.vnpayment.vn/...");
    // test logic
}
```

Không gọi thật API production trong test tự động.

---

## 4. Test Categories & Annotations

Sử dụng JUnit 5 Tag để phân loại test:

```java
@Tag("unit")
class ServiceUnitTest { }

@Tag("integration")
class RepositoryIntegrationTest { }

@Tag("api")
class ControllerApiTest { }

@Tag("slow")
@Tag("e2e")
class EndToEndTest { }
```

Maven profile gợi ý:

```xml
<profile>
  <id>unit</id>
  <properties>
    <groups>unit</groups>
  </properties>
</profile>

<profile>
  <id>integration</id>
  <properties>
    <groups>integration</groups>
  </properties>
</profile>
```

Chạy nhanh unit test:

```bash
mvn test -Dgroups=unit
```

---

## 5. Assertion Best Practices

### 5.1 Use AssertJ hoặc Hamcrest

```java
// GOOD - expressive
assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
assertThat(invoice.getAmountDue()).isGreaterThan(BigDecimal.ZERO);
assertThat(invoice.getPaymentTime()).isNotNull();

// ACCEPTABLE - JUnit 5
assertEquals(InvoiceStatus.PAID, invoice.getStatus());
assertTrue(invoice.getAmountDue().compareTo(BigDecimal.ZERO) > 0);
```

### 5.2 Assert multiple properties

```java
// GOOD - kiểm tra nhiều field
assertThat(invoice)
    .extracting("status", "finalAmount", "depositAmount")
    .containsExactly(InvoiceStatus.UNPAID, new BigDecimal("1000.00"), new BigDecimal("300.00"));
```

### 5.3 Assert exceptions

```java
// GOOD
assertThrows(BusinessException.class, () -> {
    invoiceService.createInvoice(999999L);
});

// BETTER - kiểm tra message/code
BusinessException ex = assertThrows(BusinessException.class, () -> {
    invoiceService.createInvoice(999999L);
});
assertThat(ex.getCode()).isEqualTo("INV-404");
```

---

## 6. Security & Sensitive Data Testing

### 6.1 Test authentication

```java
@Test
void getInvoice_unauthenticated_returns401() {
    mockMvc.perform(get("/api/invoices/1"))
        .andExpect(status().isUnauthorized());
}
```

### 6.2 Test authorization

```java
@Test
@WithMockUser(roles = "CUSTOMER")
void cashPayment_customerRole_returns403() {
    mockMvc.perform(post("/api/invoices/1/cash-payment"))
        .andExpect(status().isForbidden());
}

@Test
@WithMockUser(roles = "RECEPTIONIST")
void cashPayment_receptionistRole_returns200() {
    // seed invoice UNPAID
    mockMvc.perform(post("/api/invoices/1/cash-payment"))
        .andExpect(status().isOk());
    // assert invoice PAID
}
```

### 6.3 No sensitive data leak

```java
@Test
void getInvoice_customerRole_doesNotLeakOtherCustomerData() {
    // Customer A login
    // Get invoice của customer B
    mockMvc.perform(get("/api/invoices/999")) // invoice của customer khác
        .andExpect(status().isForbidden());
}

@Test
void getMedicalProfile_chefRole_onlyReturnsAllergyTags() {
    // Chef không được xem full medical record
    MedicalProfileDTO dto = medicalProfileService.getForRole(userId, Role.CHEF);
    assertThat(dto.getMedicalNote()).isNull(); // full note bị mask
    assertThat(dto.getAllergyTags()).isNotEmpty(); // allergy tags được trả
}
```

---

## 7. Database Test Guidelines

### 7.1 Repository test

```java
@DataJpaTest
class InvoiceRepositoryTest {
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    @Test
    void findByBookingId_existingBooking_returnsInvoice() {
        // Arrange: seed invoice
        Invoice invoice = invoiceRepository.save(testInvoice);
        
        // Act
        Optional<Invoice> found = invoiceRepository.findByBookingId(invoice.getBooking().getId());
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(invoice.getId());
    }
}
```

### 7.2 Test constraint/validation

```java
@Test
void saveInvoice_invalidStatus_throwsException() {
    Invoice invoice = new Invoice();
    invoice.setStatus("INVALID_STATUS"); // không hợp lệ theo DB constraint
    
    assertThrows(DataIntegrityViolationException.class, () -> {
        invoiceRepository.saveAndFlush(invoice);
    });
}
```

---

## 8. API Test Guidelines

### 8.1 MockMvc test

```java
@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private InvoiceService invoiceService;
    
    @Test
    void createInvoice_validBooking_returns201() throws Exception {
        InvoiceDTO dto = new InvoiceDTO(/* ... */);
        when(invoiceService.createInvoice(1L)).thenReturn(dto);
        
        mockMvc.perform(post("/api/invoices/create/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invoiceId").exists())
            .andExpect(jsonPath("$.status").value("UNPAID"));
    }
}
```

### 8.2 Validation test

```java
@Test
void createBooking_missingRequiredField_returns400() throws Exception {
    String invalidPayload = "{ \"checkInDate\": null }"; // thiếu field bắt buộc
    
    mockMvc.perform(post("/api/bookings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidPayload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").isNotEmpty());
}
```

---

## 9. E2E Test Guidelines

E2E chỉ test key business flows:

1. Booking -> Invoice -> Payment -> Paid.
2. Customer register -> login -> book room -> check-in -> check-out.
3. Chef view allergy warning -> mark dish prepared.

Tool gợi ý:

- Selenium/Playwright cho full UI test nếu cần.
- RestAssured/TestNG cho API E2E nếu chưa có frontend.
- Manual test documented markdown nếu chưa tự động hóa.

---

## 10. Continuous Testing & CI/CD

### 10.1 Pre-commit hook gợi ý

```bash
#!/bin/bash
# .git/hooks/pre-commit
mvn test -Dgroups=unit
if [ $? -ne 0 ]; then
  echo "Unit test failed. Commit rejected."
  exit 1
fi
```

### 10.2 CI pipeline gợi ý

```yaml
# GitHub Actions example
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '21'
      - run: mvn clean test
      - run: mvn verify
```

---

## 11. Test Documentation

Mỗi test class nên có header comment:

```java
/**
 * Unit tests for InvoiceServiceImpl.
 * 
 * Coverage:
 * - createInvoice: happy path, missing booking, cancelled booking
 * - createPaymentUrl: happy path, paid invoice guard
 * - processVNPayCallback: valid hash, invalid hash, duplicate callback
 * 
 * Related UC: UC03 Payment & Invoice
 * Test Case Doc: Test_Cases/UC03_PaymentInvoice_TESTCASE.md
 */
class InvoiceServiceImplTest {
    // ...
}
```

---

## 12. Known Limitations & TODO

Ghi rõ test chưa làm được:

```java
@Disabled("VNPay sandbox không khả dụng trong môi trường CI")
@Test
void realVNPayIntegration_success() {
    // test thật VNPay sandbox
}
```

Hoặc trong markdown test case:

```markdown
| TC-PAY-E2E-002 | E2E | High | Real VNPay sandbox flow | Blocked | VNPay sandbox credentials unavailable |
```

---

## 13. Review Checklist cho Test Code

Trước khi merge test code, reviewer kiểm tra:

- [ ] Test name rõ nghĩa.
- [ ] Không dùng dữ liệu thật.
- [ ] Không hardcode password/token/secret.
- [ ] Test có arrange/act/assert rõ ràng.
- [ ] Test pass khi chạy local.
- [ ] Test độc lập, không phụ thuộc thứ tự chạy.
- [ ] Cleanup data sau test nếu cần.
- [ ] Mock external service đúng chỗ.
- [ ] Assert đủ field quan trọng, không chỉ assert 1 field rồi cho qua.
- [ ] Security/permission test nếu UC có auth.

---

## 14. Common Pitfalls

| Lỗi hay gặp | Giải thích | Fix |
| :--- | :--- | :--- |
| Test pass cục bộ, fail trên CI | Dữ liệu seed khác nhau, timezone khác | Dùng test container hoặc seed script nhất quán |
| Test chậm | Khởi động toàn bộ Spring context không cần thiết | Dùng `@WebMvcTest`, `@DataJpaTest` thay vì `@SpringBootTest` |
| Test không độc lập | Dùng chung static/global state | Reset state trong `@BeforeEach` |
| Assertion quá chung | `assertTrue(result != null)` | Assert cụ thể field/value quan trọng |
| Không test negative case | Chỉ test happy path | Thêm test validation/error/unauthorized |

---

## 15. Resources & References

| Resource | Link/Path |
| :--- | :--- |
| JUnit 5 User Guide | https://junit.org/junit5/docs/current/user-guide/ |
| AssertJ | https://assertj.github.io/doc/ |
| Spring Boot Testing | https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing |
| Testcontainers | https://www.testcontainers.org/ |
| TDD Template | `Template/TDD_TEMPLATE_V1.md` |
| Test Case Docs | `Test_Cases/` |
| AI Team Rules | `Quy_tac_AI_Test/AI_TEAM_RULES.md` |
