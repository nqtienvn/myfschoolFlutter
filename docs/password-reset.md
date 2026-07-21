# Khôi phục mật khẩu qua email

## Phạm vi hiện tại

- `PARENT`, `STUDENT` và `TEACHER` đều được yêu cầu link quên mật khẩu.
- Chỉ tài khoản có email đã xác minh mới được gửi link; hồ sơ học sinh cũ thiếu email
  phải được bổ sung và xác minh trước khi dùng recovery.
- Tài khoản `LOCKED` được đặt mật khẩu mới nhưng vẫn giữ `LOCKED`.
- Cả ba role vẫn đổi mật khẩu khi đã đăng nhập. Sau khi đổi/reset, JWT phát hành trước
  `credentials_updated_at` không còn hợp lệ và client phải đăng nhập lại.

## Chuẩn hóa và xác minh email

Email luôn được trim và chuyển về chữ thường trước khi persist; kiểm tra trùng dùng so
sánh không phân biệt hoa/thường. Email do Admin cấp khi tạo/import tài khoản được xem là
đã được nhà trường xác minh. Email người dùng tự thay đổi sẽ đặt lại `email_verified_at`
về `NULL` và chưa được dùng cho recovery.

Tạo giáo viên, học sinh và phụ huynh mới đều bắt buộc có email. Thông tin đăng nhập ban
đầu của mỗi tài khoản được gửi trực tiếp đến email đã xác minh của chính tài khoản đó;
không gửi mật khẩu học sinh sang email phụ huynh.
Mật khẩu tạm được tạo ngẫu nhiên và không còn xuất hiện trong response/API quản trị.
Self-registration mặc định tắt (`SELF_REGISTRATION_ENABLED=false`); tài khoản production
được nhà trường tạo qua luồng giáo viên hoặc ghi danh học sinh/phụ huynh.

## API

```http
POST /api/auth/password-reset/request
{"phone":"0901234567"}

POST /api/auth/password-reset/validate
{"token":"..."}

POST /api/auth/password-reset/confirm
{"token":"...","newPassword":"...","confirmPassword":"..."}
```

Endpoint request luôn trả cùng một thông báo cho tài khoản thiếu email, chưa xác minh,
không tồn tại, sai role hoặc đã chạm rate limit. Mail chạy ngoài request thread và retry
cùng một nội dung, vì vậy lỗi Gmail không sinh thêm token.

## Cấu hình

```dotenv
PASSWORD_RESET_EMAIL_ENABLED=true
PASSWORD_RESET_ALLOWED_ROLES=PARENT,STUDENT,TEACHER
PASSWORD_RESET_FRONTEND_URL=https://admin.myfschool.vn/reset-password
PASSWORD_RESET_TOKEN_SECRET=<random-secret-at-least-32-bytes>

MAIL_PROVIDER=GMAIL
MAIL_FROM=no-reply@myfschool.edu.vn
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_REFRESH_TOKEN=...
MAIL_ASYNC_ENABLED=true
MAIL_MAX_ATTEMPTS=3
MAIL_RETRY_DELAY_MILLIS=1000

FLYWAY_ENABLED=true
```

`MAIL_PROVIDER=FAKE` là mặc định cho local/test và tuyệt đối không gửi Gmail thật. Provider
`GMAIL` gọi Gmail API bằng OAuth 2.0 refresh token; không dùng mật khẩu Gmail thường.

Link email có dạng:

```text
https://admin.myfschool.vn/reset-password#token=...
```

Fragment `#token` không được gửi trong HTTP request/access log. Web server production cần
rewrite `/forgot-password` và `/reset-password` về SPA `index.html`.

Nếu backend chạy sau reverse proxy, chỉ đặt `FORWARD_HEADERS_STRATEGY=FRAMEWORK` khi proxy
tin cậy đã xóa header `Forwarded`/`X-Forwarded-*` do client tự gửi; nếu không, giữ `NONE`
để tránh giả mạo IP rate-limit.

## Checklist production

1. Chạy migration `V26__password_reset_email_recovery.sql` trên staging và kiểm tra email trùng.
2. Cấu hình Google Workspace/Gmail OAuth, sender và quyền gửi.
3. Cấu hình SPF, DKIM và DMARC cho domain trường.
4. Kiểm tra retry mail, dashboard/log lỗi và dung lượng executor.
5. Bật feature flag ở staging, chạy test bảo mật, sau đó mới bật production.
6. Bổ sung/xác minh email cho các hồ sơ học sinh cũ trước khi bật cờ trên production.
