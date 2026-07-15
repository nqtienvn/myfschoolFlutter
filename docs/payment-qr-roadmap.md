# Thanh toán chuyển khoản và lộ trình mở rộng QR

## Luồng đang triển khai

1. Admin chọn năm học trên Admin Web.
2. Admin cấu hình ngân hàng, số tài khoản, chủ tài khoản, chi nhánh và mẫu nội dung chuyển khoản.
3. Backend lưu đúng một cấu hình cho mỗi năm học.
4. App lấy cấu hình theo học kỳ; backend tự suy ra năm học của học kỳ đó.
5. Phụ huynh/học sinh tự chuyển khoản, sau đó bấm **Xác nhận đã chuyển**.
6. Backend chuyển khoản học phí sang trạng thái `PROCESSING` để nhà trường đối soát.
7. Admin kiểm tra sao kê rồi chọn **Đã nhận tiền** để chuyển bill sang `PAID`, hoặc **Không tìm thấy giao dịch** để trả bill về `UNPAID`.

App không tạo QR ở luồng hiện tại. Hai trường trong response đã dành sẵn cho việc mở rộng:

- `displayMode`: hiện là `MANUAL`, sau này có thể là `MANUAL_AND_QR`.
- `qrAvailable`: hiện là `false`, chỉ chuyển thành `true` khi backend tạo được QR hợp lệ.

`bankCode` cũng được lưu ngay từ đầu để sau này ánh xạ sang mã BIN/NAPAS của ngân hàng.

## Không nên tạo QR trực tiếp từ tổng tiền trên màn hình

Trước khi bật QR, cần tạo khái niệm **payment intent** (yêu cầu thanh toán). Payment intent phải được backend tạo từ các khoản học phí thực tế, không nhận số tiền hoặc nội dung tùy ý từ app.

Các bảng đề xuất:

### `payment_intents`

- `id`
- `student_id`
- `academic_year_id`
- `semester_id`
- `amount`
- `transfer_content` — duy nhất, ngắn và dùng để đối soát
- `status` — `CREATED | PENDING | PAID | EXPIRED | CANCELLED`
- `provider`
- `provider_reference`
- `qr_payload` hoặc `qr_image_url`
- `expires_at`, `created_at`, `updated_at`

### `payment_intent_bills`

- `payment_intent_id`
- `tuition_bill_id`
- `allocated_amount`

Bảng nối này cho phép một QR thanh toán nhiều khoản học phí và vẫn biết chính xác khoản nào đã được trả.

## API nên bổ sung khi bật QR

### Tạo yêu cầu thanh toán

`POST /api/payment-intents`

Request chỉ gửi `billIds`. Backend phải:

1. xác thực người dùng có quyền với học sinh;
2. kiểm tra các bill cùng học sinh, học kỳ và năm học;
3. chỉ nhận bill chưa thanh toán;
4. tự tính tổng tiền;
5. tạo nội dung chuyển khoản duy nhất;
6. tạo QR qua provider adapter.

Response nên giữ cả dữ liệu chuyển khoản thủ công và QR:

```json
{
  "id": 101,
  "amount": 15000000,
  "bankName": "TPBank",
  "accountNumber": "1234567890",
  "accountHolder": "FPT SCHOOLS",
  "transferContent": "MFS-101-HS009",
  "displayMode": "MANUAL_AND_QR",
  "qrAvailable": true,
  "qrImageUrl": "https://...",
  "expiresAt": "2026-12-31T23:59:59"
}
```

### Nhận kết quả thanh toán

`POST /api/payment-webhooks/{provider}`

Webhook phải kiểm tra chữ ký, chống xử lý trùng bằng `provider_reference`, đối chiếu đúng số tiền và nội dung rồi mới cập nhật bill sang `PAID`. Không đưa secret của provider vào Admin Web hoặc app mobile.

### Xem trạng thái

`GET /api/payment-intents/{id}`

App có thể polling API này; khi cần realtime mới bổ sung WebSocket/push notification.

## Provider adapter

Backend nên định nghĩa một interface chung, ví dụ:

```java
public interface QrPaymentProvider {
    QrPaymentResult createQr(PaymentIntent intent, PaymentConfiguration config);
    VerifiedPayment verifyWebhook(String payload, Map<String, String> headers);
}
```

Mỗi nhà cung cấp QR là một implementation riêng. Phần nghiệp vụ học phí không phụ thuộc trực tiếp VietQR hay một ngân hàng cụ thể, nên có thể thay provider mà không sửa mobile.

## Thay đổi Admin Web và mobile khi bật QR

- Admin Web thêm `qrEnabled`, provider và thông tin xác thực; secret phải lưu mã hóa ở backend.
- Backend chỉ trả `qrAvailable = true` khi cấu hình hợp lệ và provider hoạt động.
- Mobile hiển thị QR theo `displayMode`; nếu provider lỗi thì vẫn hiển thị số tài khoản để chuyển thủ công.
- Không cho app tự ghép URL QR từ số tiền, số tài khoản hoặc nội dung chuyển khoản.
- Thêm test cách ly theo hai năm học, test webhook lặp, sai chữ ký, sai số tiền, intent hết hạn và thanh toán đồng thời.

## Thứ tự triển khai an toàn

1. Hoàn thiện đối soát chuyển khoản thủ công.
2. Thêm `payment_intents` và dùng intent cho cả luồng thủ công.
3. Thêm provider adapter và QR trong môi trường thử nghiệm.
4. Thêm webhook idempotent và audit log.
5. Bật QR bằng feature flag cho một năm học thử nghiệm.
6. Theo dõi đối soát, sau đó mới mở rộng toàn trường.
