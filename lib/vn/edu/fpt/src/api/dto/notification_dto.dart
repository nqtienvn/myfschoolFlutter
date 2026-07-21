import '../../models/app_notification.dart';
import '../exception/parse_exception.dart';

class NotificationDto {
  const NotificationDto({
    required this.id,
    required this.title,
    required this.body,
    required this.tag,
    required this.isRead,
    required this.createdAt,
    this.relatedId,
    this.relatedType,
    this.academicYearId,
    this.semesterId,
  });

  final int id;
  final String title;
  final String body;
  final String tag;
  final bool isRead;
  final DateTime createdAt;
  final int? relatedId;
  final String? relatedType;
  final int? academicYearId;
  final int? semesterId;

  factory NotificationDto.fromJson(Map<String, dynamic> json) =>
      NotificationDto(
        id: requireField<int>(json, 'id'),
        title: requireField<String>(json, 'title'),
        body: json['body'] is String ? json['body'] as String : '',
        tag: json['tag'] is String ? json['tag'] as String : 'Hệ thống',
        isRead: json['isRead'] == true,
        relatedId: json['relatedId'] as int?,
        relatedType: json['relatedType'] as String?,
        academicYearId: json['academicYearId'] as int?,
        semesterId: json['semesterId'] as int?,
        createdAt: DateTime.parse(requireField<String>(json, 'createdAt')),
      );

  AppNotification toDomain() => AppNotification(
    id: id,
    title: title,
    message: body,
    tag: tag,
    isRead: isRead,
    createdAt: createdAt,
    relatedId: relatedId,
    relatedType: relatedType,
    academicYearId: academicYearId,
    semesterId: semesterId,
  );
}
