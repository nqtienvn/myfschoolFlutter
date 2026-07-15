import '../exception/parse_exception.dart';

class AttendanceCorrectionEntryDto {
  const AttendanceCorrectionEntryDto({
    required this.studentId,
    required this.studentName,
    required this.studentCode,
    required this.oldStatus,
    required this.newStatus,
  });

  factory AttendanceCorrectionEntryDto.fromJson(Map<String, dynamic> json) {
    return AttendanceCorrectionEntryDto(
      studentId: _requiredInt(json, 'studentId'),
      studentName: _requiredString(json, 'studentName'),
      studentCode: _requiredString(json, 'studentCode'),
      oldStatus: json['oldStatus'] as String?,
      newStatus: _requiredString(json, 'newStatus'),
    );
  }

  final int studentId;
  final String studentName;
  final String studentCode;
  final String? oldStatus;
  final String newStatus;
}

class AttendanceCorrectionDto {
  const AttendanceCorrectionDto({
    required this.id,
    required this.classId,
    required this.className,
    required this.teacherName,
    required this.date,
    required this.shift,
    required this.status,
    required this.originalPresentCount,
    required this.originalAbsentWithLeaveCount,
    required this.originalAbsentWithoutLeaveCount,
    required this.presentCount,
    required this.absentWithLeaveCount,
    required this.absentWithoutLeaveCount,
    required this.reason,
    required this.changes,
    required this.createdAt,
    required this.reviewedByName,
    required this.reviewedAt,
  });

  factory AttendanceCorrectionDto.fromJson(Map<String, dynamic> json) {
    final changes = json['changes'];
    if (changes is! List) {
      throw const ParseException(
        'Chi tiết yêu cầu sửa điểm danh không hợp lệ.',
      );
    }
    return AttendanceCorrectionDto(
      id: _requiredInt(json, 'id'),
      classId: _requiredInt(json, 'classId'),
      className: _requiredString(json, 'className'),
      teacherName: _requiredString(json, 'teacherName'),
      date: _requiredString(json, 'date'),
      shift: _requiredString(json, 'shift'),
      status: _requiredString(json, 'status'),
      originalPresentCount: _requiredInt(json, 'originalPresentCount'),
      originalAbsentWithLeaveCount: _requiredInt(
        json,
        'originalAbsentWithLeaveCount',
      ),
      originalAbsentWithoutLeaveCount: _requiredInt(
        json,
        'originalAbsentWithoutLeaveCount',
      ),
      presentCount: _requiredInt(json, 'presentCount'),
      absentWithLeaveCount: _requiredInt(json, 'absentWithLeaveCount'),
      absentWithoutLeaveCount: _requiredInt(json, 'absentWithoutLeaveCount'),
      reason: _requiredString(json, 'reason'),
      changes: changes
          .map((item) {
            if (item is! Map<String, dynamic>) {
              throw const ParseException(
                'Một thay đổi điểm danh không hợp lệ.',
              );
            }
            return AttendanceCorrectionEntryDto.fromJson(item);
          })
          .toList(growable: false),
      createdAt: _requiredString(json, 'createdAt'),
      reviewedByName: json['reviewedByName'] as String?,
      reviewedAt: json['reviewedAt'] as String?,
    );
  }

  final int id;
  final int classId;
  final String className;
  final String teacherName;
  final String date;
  final String shift;
  final String status;
  final int originalPresentCount;
  final int originalAbsentWithLeaveCount;
  final int originalAbsentWithoutLeaveCount;
  final int presentCount;
  final int absentWithLeaveCount;
  final int absentWithoutLeaveCount;
  final String reason;
  final List<AttendanceCorrectionEntryDto> changes;
  final String createdAt;
  final String? reviewedByName;
  final String? reviewedAt;
}

int _requiredInt(Map<String, dynamic> json, String field) {
  final value = json[field];
  if (value is! num) {
    throw ParseException(
      'Trường $field của yêu cầu sửa điểm danh không hợp lệ.',
    );
  }
  return value.toInt();
}

String _requiredString(Map<String, dynamic> json, String field) {
  final value = json[field];
  if (value is! String || value.trim().isEmpty) {
    throw ParseException(
      'Trường $field của yêu cầu sửa điểm danh không hợp lệ.',
    );
  }
  return value.trim();
}
