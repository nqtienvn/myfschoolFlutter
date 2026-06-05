class AppNotification {
  final int id;
  final String title;
  final String message;
  final DateTime createdAt;

  const AppNotification({
    required this.id,
    required this.title,
    required this.message,
    required this.createdAt,
  });

  String get unreadLabel {
    return '2 thông báo chưa đọc';
  }
}
