import '../../models/school_announcement.dart';
import '../exception/parse_exception.dart';

class AnnouncementDto {
  const AnnouncementDto({
    required this.id,
    required this.title,
    required this.body,
    required this.senderName,
    required this.classNames,
    required this.classIds,
    required this.targetRole,
    required this.isRead,
    required this.createdAt,
    required this.academicYearId,
    required this.deliveryStatus,
  });

  factory AnnouncementDto.fromJson(Map<String, dynamic> json) =>
      AnnouncementDto(
        id: _int(json, 'id'),
        title: _string(json, 'title'),
        body: _string(json, 'body'),
        senderName: _string(json, 'teacherName'),
        classNames: _strings(json['classNames']),
        classIds: _ints(json['classIds']),
        targetRole: _string(json, 'targetRole'),
        isRead: json['isRead'] == true,
        createdAt:
            _date(json['createdAt']) ??
            (throw const ParseException('createdAt must be a date.')),
        academicYearId: _int(json, 'academicYearId'),
        deliveryStatus: _string(json, 'deliveryStatus'),
      );

  final int id;
  final String title;
  final String body;
  final String senderName;
  final List<String> classNames;
  final List<int> classIds;
  final String targetRole;
  final bool isRead;
  final DateTime createdAt;
  final int academicYearId;
  final String deliveryStatus;

  SchoolAnnouncement toDomain() => SchoolAnnouncement(
    id: id,
    title: title,
    body: body,
    senderName: senderName,
    classNames: classNames,
    classIds: classIds,
    targetRole: targetRole,
    isRead: isRead,
    createdAt: createdAt,
    academicYearId: academicYearId,
    deliveryStatus: deliveryStatus,
  );
}

class AnnouncementRecipientDto {
  const AnnouncementRecipientDto({
    required this.userId,
    required this.userName,
    required this.role,
    required this.studentNames,
    required this.classNames,
    required this.status,
    this.readAt,
  });

  factory AnnouncementRecipientDto.fromJson(Map<String, dynamic> json) =>
      AnnouncementRecipientDto(
        userId: _int(json, 'userId'),
        userName: _string(json, 'userName'),
        role: _string(json, 'role'),
        studentNames: _strings(json['studentNames']),
        classNames: _strings(json['classNames']),
        readAt: _date(json['readAt']),
        status: _string(json, 'status'),
      );

  final int userId;
  final String userName;
  final String role;
  final List<String> studentNames;
  final List<String> classNames;
  final DateTime? readAt;
  final String status;

  AnnouncementRecipient toDomain() => AnnouncementRecipient(
    userId: userId,
    userName: userName,
    role: role,
    studentNames: studentNames,
    classNames: classNames,
    readAt: readAt,
    status: status,
  );
}

int _int(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is int) return value;
  throw ParseException('$key must be an int.');
}

String _string(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is String) return value;
  throw ParseException('$key must be a string.');
}

List<String> _strings(Object? value) => value is List
    ? value.whereType<String>().toList(growable: false)
    : const [];

List<int> _ints(Object? value) =>
    value is List ? value.whereType<int>().toList(growable: false) : const [];

DateTime? _date(Object? value) =>
    value is String ? DateTime.tryParse(value) : null;
