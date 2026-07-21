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
    this.academicYearId,
    this.semesterId,
  });

  final int id;
  final String title;
  final String message;
  final String tag;
  final bool isRead;
  final DateTime createdAt;
  final int? relatedId;
  final String? relatedType;
  final int? academicYearId;
  final int? semesterId;

  AppNotification copyWith({bool? isRead}) => AppNotification(
    id: id,
    title: title,
    message: message,
    tag: tag,
    isRead: isRead ?? this.isRead,
    createdAt: createdAt,
    relatedId: relatedId,
    relatedType: relatedType,
    academicYearId: academicYearId,
    semesterId: semesterId,
  );
}
