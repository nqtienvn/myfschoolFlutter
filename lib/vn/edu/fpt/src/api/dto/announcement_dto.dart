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
    required this.requiresReply,
    required this.isRead,
    required this.acknowledged,
    required this.totalRecipients,
    required this.readCount,
    required this.acknowledgedCount,
    required this.repliedCount,
    required this.createdAt,
    required this.academicYearId,
    required this.approvalStatus,
    this.replyText,
    this.repliedAt,
    this.recipientStatus,
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
        requiresReply: json['requiresReply'] == true,
        isRead: json['isRead'] == true,
        acknowledged: json['acknowledged'] == true,
        replyText: json['replyText'] as String?,
        repliedAt: _date(json['repliedAt']),
        recipientStatus: json['recipientStatus'] as String?,
        totalRecipients: _intOrZero(json['totalRecipients']),
        readCount: _intOrZero(json['readCount']),
        acknowledgedCount: _intOrZero(json['acknowledgedCount']),
        repliedCount: _intOrZero(json['repliedCount']),
        createdAt:
            _date(json['createdAt']) ??
            (throw const ParseException('createdAt must be a date.')),
        academicYearId: _int(json, 'academicYearId'),
        approvalStatus: _string(json, 'approvalStatus'),
      );

  final int id;
  final String title;
  final String body;
  final String senderName;
  final List<String> classNames;
  final List<int> classIds;
  final String targetRole;
  final bool requiresReply;
  final bool isRead;
  final bool acknowledged;
  final String? replyText;
  final DateTime? repliedAt;
  final String? recipientStatus;
  final int totalRecipients;
  final int readCount;
  final int acknowledgedCount;
  final int repliedCount;
  final DateTime createdAt;
  final int academicYearId;
  final String approvalStatus;

  SchoolAnnouncement toDomain() => SchoolAnnouncement(
    id: id,
    title: title,
    body: body,
    senderName: senderName,
    classNames: classNames,
    classIds: classIds,
    targetRole: targetRole,
    requiresReply: requiresReply,
    isRead: isRead,
    acknowledged: acknowledged,
    replyText: replyText,
    repliedAt: repliedAt,
    recipientStatus: recipientStatus,
    totalRecipients: totalRecipients,
    readCount: readCount,
    acknowledgedCount: acknowledgedCount,
    repliedCount: repliedCount,
    createdAt: createdAt,
    academicYearId: academicYearId,
    approvalStatus: approvalStatus,
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
    this.acknowledgedAt,
    this.replyText,
    this.repliedAt,
  });

  factory AnnouncementRecipientDto.fromJson(Map<String, dynamic> json) =>
      AnnouncementRecipientDto(
        userId: _int(json, 'userId'),
        userName: _string(json, 'userName'),
        role: _string(json, 'role'),
        studentNames: _strings(json['studentNames']),
        classNames: _strings(json['classNames']),
        readAt: _date(json['readAt']),
        acknowledgedAt: _date(json['acknowledgedAt']),
        replyText: json['replyText'] as String?,
        repliedAt: _date(json['repliedAt']),
        status: _string(json, 'status'),
      );

  final int userId;
  final String userName;
  final String role;
  final List<String> studentNames;
  final List<String> classNames;
  final DateTime? readAt;
  final DateTime? acknowledgedAt;
  final String? replyText;
  final DateTime? repliedAt;
  final String status;

  AnnouncementRecipient toDomain() => AnnouncementRecipient(
    userId: userId,
    userName: userName,
    role: role,
    studentNames: studentNames,
    classNames: classNames,
    readAt: readAt,
    acknowledgedAt: acknowledgedAt,
    replyText: replyText,
    repliedAt: repliedAt,
    status: status,
  );
}

int _int(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is int) return value;
  throw ParseException('$key must be an int.');
}

int _intOrZero(Object? value) => value is int ? value : 0;

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
