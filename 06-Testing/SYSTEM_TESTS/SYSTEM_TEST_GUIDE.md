# 🖥️ SYSTEM TESTS – MyFschool Chat Realtime

> **Thư mục:** `06-Testing/SYSTEM_TESTS/`  
> **Mục đích:** Kiểm thử toàn hệ thống End-to-End (E2E) – mô phỏng kịch bản trao đổi tin nhắn thực tế giữa các vai trò (Phụ huynh, Học sinh, Giáo viên) qua mạng WebSocket.  
> **Công cụ:** Playwright (E2E Multi-client Browser Tests) | k6 (WebSocket Load Testing)

---

## 1. Chiến lược System Test (E2E)

Kiểm thử hệ thống chat realtime yêu cầu chạy **ít nhất hai client đồng thời** (hoặc mô phỏng một client thật tương tác với một socket bot của server) để kiểm tra tính chính xác của các sự kiện truyền tin tức thời:

```
  Phụ huynh Client                      Giáo viên Client
┌─────────────────┐                  ┌──────────────────┐
│   Mở màn hình   │                  │   Mở màn hình    │
│   hội thoại B   │                  │   Inbox (Tab 1)  │
│        │        │                  │        │         │
│        ▼        │                  │        ▼         │
│   Gõ tin nhắn   │                  │ Thấy dấu chấm    │
│  (typing.start) ├─────────────────►│ online của PH B  │
│        │        │                  │ Thấy "...typing" │
│        ▼        │                  │        │         │
│  Gửi "Chào cô"  │                  │        ▼         │
│  (message.send) ├─►[ WebSocket ]──►│ Nhận tin nhắn mới│
│                 │                  │   (message.new)  │
│ Thấy 2 check    │                  │        │         │
│ (message.receipt)◄─[ WebSocket ]◄──┤ Gửi phản hồi     │
└─────────────────┘                  └──────────────────┘
```

---

## 2. Kịch bản E2E tự động hóa (Playwright TypeScript)

Kịch bản dưới đây mô phỏng hai trình duyệt mở song song để thực hiện chat thời gian thực.

#### `chat_realtime_e2e.spec.ts`

```typescript
import { test, expect, BrowserContext, Page } from '@playwright/test';

test.describe('E2E Real-time Chat Journey', () => {
  let parentContext: BrowserContext;
  let teacherContext: BrowserContext;
  let parentPage: Page;
  let teacherPage: Page;

  test.beforeAll(async ({ browser }) => {
    // Tạo 2 session trình duyệt riêng biệt để tránh chia sẻ cookie/localStorage
    parentContext = await browser.newContext();
    teacherContext = await browser.newContext();
    
    parentPage = await parentContext.newPage();
    teacherPage = await teacherContext.newPage();
  });

  test('SYS-CHAT-001: Nhắn tin realtime hai chiều giữa Phụ huynh và Giáo viên', async () => {
    // 1. Giáo viên đăng nhập và mở Tab Tin nhắn
    await teacherPage.goto('http://localhost:3000/login');
    await teacherPage.fill('#input-phone', '0909000001');
    await teacherPage.fill('#input-password', 'password123');
    await teacherPage.click('#btn-login');
    await teacherPage.click('#tab-messages'); // Mở màn hình TeacherInboxScreen
    
    // 2. Phụ huynh đăng nhập và mở hội thoại với Giáo viên
    await parentPage.goto('http://localhost:3000/login');
    await parentPage.fill('#input-phone', '0909000002');
    await parentPage.fill('#input-password', 'password123');
    await parentPage.click('#btn-login');
    await parentPage.click('.conversation-tile:has-text("Giáo viên A")');
    
    // 3. Phụ huynh bắt đầu gõ chữ -> Giáo viên thấy typing indicator
    await parentPage.focus('#chat-input');
    await parentPage.keyboard.type('C'); // Bắt đầu gõ chữ
    await expect(teacherPage.locator('.typing-indicator')).toBeVisible();
    await expect(teacherPage.locator('.typing-indicator')).toContainText('Phụ huynh B đang nhập...');

    // 4. Phụ huynh gửi tin nhắn -> Giáo viên nhận được lập tức
    await parentPage.fill('#chat-input', 'Chào cô, cháu hôm nay nghỉ ốm ạ.');
    await parentPage.click('#btn-send');
    
    // Phụ huynh thấy trạng thái tin nhắn đổi sang "Đã gửi" (1 check)
    await expect(parentPage.locator('.message-status-icon').last()).toHaveAttribute('data-status', 'sent');

    // Giáo viên nhận tin nhắn realtime
    const lastReceivedMsg = teacherPage.locator('.message-bubble-left').last();
    await expect(lastReceivedMsg).toBeVisible();
    await expect(lastReceivedMsg).toContainText('Chào cô, cháu hôm nay nghỉ ốm ạ.');

    // Do Giáo viên đang mở màn hình chat, Phụ huynh sẽ thấy trạng thái tin nhắn đổi sang "Đã xem" (read)
    await expect(parentPage.locator('.message-status-icon').last()).toHaveAttribute('data-status', 'read');

    // 5. Giáo viên trả lời
    await teacherPage.fill('#chat-input-teacher', 'Chào anh, tôi đã nhận được thông tin.');
    await teacherPage.click('#btn-send-teacher');

    // Phụ huynh nhận tin nhắn trả lời realtime
    const parentReceivedMsg = parentPage.locator('.message-bubble-left').last();
    await expect(parentReceivedMsg).toBeVisible();
    await expect(parentReceivedMsg).toContainText('Chào anh, tôi đã nhận được thông tin.');
  });

  test.afterAll(async () => {
    await parentContext.close();
    await teacherContext.close();
  });
});
```

---

## 3. Load Testing cho kết nối WebSocket (k6)

Kiểm thử hiệu năng chịu tải khi hàng trăm kết nối WebSocket được thiết lập cùng lúc và trao đổi tin nhắn.

#### `websocket_load_test.js`

```javascript
import ws from 'k6/ws';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp-up: Thiết lập nhanh 50 connections
    { duration: '1m', target: 50 },   // Giữ tải ổn định trong 1 phút
    { duration: '30s', target: 0 },   // Ramp-down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<200'], // Thời gian handshake < 200ms
  },
};

export default function () {
  const token = 'test-token-jwt-placeholder';
  const url = `ws://localhost:8080/chat?token=${token}`;

  const res = ws.connect(url, {}, function (socket) {
    socket.on('open', function () {
      // Gửi heartbeat presence định kỳ
      socket.setInterval(function () {
        socket.send(JSON.stringify({ type: 'presence.heartbeat' }));
      }, 15000);

      // Gửi thử tin nhắn sau khi connect
      socket.send(JSON.stringify({
        type: 'message.send',
        conversationId: 123,
        clientMessageId: `k6-msg-${__VU}-${__ITER}`,
        messageType: 'TEXT',
        content: 'Load test message'
      }));
    });

    socket.on('message', function (data) {
      const msg = JSON.parse(data);
      if (msg.type === 'message.ack') {
        check(msg, {
          'ack status is sent': (m) => m.data.status === 'sent',
        });
      }
    });

    socket.setTimeout(function () {
      socket.close();
    }, 20000); // Đóng sau 20s
  });

  check(res, {
    'websocket connected successfully': (r) => r && r.status === 101,
  });
}
```

---

## 4. Hướng dẫn chạy kiểm thử hệ thống

### Chạy kiểm thử tự động Playwright
```bash
# Cài đặt môi trường kiểm thử
cd frontend
npm install @playwright/test
npx playwright install

# Chạy test suite
npx playwright test system_tests/chat_realtime_e2e.spec.ts --headed
```

### Chạy Load Test bằng k6
```bash
# Cài đặt k6 trước (qua choco trên Windows hoặc brew trên macOS)
# Chạy k6 test
k6 run websocket_load_test.js
```
