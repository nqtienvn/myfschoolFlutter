import '../models/app_notification.dart';

class NotificationService {
  const NotificationService();

  Stream<AppNotification> watchNotifications() async* {
    await Future<void>.delayed(const Duration(milliseconds: 500));
    yield AppNotification(
      id: 1,
      title: 'Điểm mới',
      message: 'An vừa có điểm Toán 8.5.',
      createdAt: DateTime.now(),
    );
    await Future<void>.delayed(const Duration(seconds: 5));
    yield AppNotification(
      id: 2,
      title: 'Bài tập sắp đến hạn',
      message: 'Bài tập Văn cần nộp trước 20:00.',
      createdAt: DateTime.now(),
    );

    await Future<void>.delayed(const Duration(seconds: 5));
    yield AppNotification(
      id: 3,
      title: 'Thông báo lớp',
      message: 'Lớp 10A1 đổi phòng học tiết 3.',
      createdAt: DateTime.now(),
    );
  }
}
