import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/chat_socket_event_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/notification_dto.dart';

void main() {
  const json = {
    'id': 42,
    'title': 'Thời khóa biểu mới của lớp 12A',
    'body': 'Thời khóa biểu đã được phát hành.',
    'tag': 'Thời khóa biểu',
    'isRead': false,
    'relatedId': null,
    'relatedType': 'SYSTEM',
    'createdAt': '2026-07-12T00:00:00',
  };

  test('parses notification REST payload', () {
    final notification = NotificationDto.fromJson(json).toDomain();

    expect(notification.id, 42);
    expect(notification.tag, 'Thời khóa biểu');
    expect(notification.isRead, isFalse);
  });

  test('parses realtime notification.new event', () {
    final event = ChatSocketEventDto.fromJson({
      'type': 'notification.new',
      'notification': json,
      'unreadCount': 3,
    });

    expect(event.type, 'notification.new');
    expect(event.notification?.id, 42);
    expect(event.unreadCount, 3);
  });
}
