class AppNotification {
  const AppNotification({
    required this.id,
    required this.title,
    required this.message,
    required this.createdAt,
    this.tag = 'Hệ thống',
    this.isRead = false,
    this.relatedId,
    this.relatedType,
  });

  final int id;
  final String title;
  final String message;
  final String tag;
  final bool isRead;
  final DateTime createdAt;
  final int? relatedId;
  final String? relatedType;

  AppNotification copyWith({bool? isRead}) => AppNotification(
    id: id,
    title: title,
    message: message,
    tag: tag,
    isRead: isRead ?? this.isRead,
    createdAt: createdAt,
    relatedId: relatedId,
    relatedType: relatedType,
  );
}
