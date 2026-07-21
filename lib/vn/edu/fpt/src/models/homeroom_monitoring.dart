class StudentRiskFlag {
  const StudentRiskFlag({
    required this.id,
    required this.studentId,
    required this.studentName,
    required this.studentCode,
    required this.riskType,
    required this.severity,
    required this.metricValue,
    required this.thresholdValue,
    required this.message,
    required this.status,
    required this.detectedAt,
  });

  final int id;
  final int studentId;
  final String studentName;
  final String studentCode;
  final String riskType;
  final String severity;
  final String metricValue;
  final String thresholdValue;
  final String message;
  final String status;
  final DateTime? detectedAt;

  factory StudentRiskFlag.fromJson(Map<String, dynamic> json) =>
      StudentRiskFlag(
        id: _int(json, 'id'),
        studentId: _int(json, 'studentId'),
        studentName: _string(json, 'studentName'),
        studentCode: _string(json, 'studentCode'),
        riskType: _string(json, 'riskType'),
        severity: _string(json, 'severity'),
        metricValue: json['metricValue']?.toString() ?? '',
        thresholdValue: json['thresholdValue']?.toString() ?? '',
        message: _string(json, 'message'),
        status: _string(json, 'status'),
        detectedAt: DateTime.tryParse(json['detectedAt'] as String? ?? ''),
      );
}

class ParentContactLog {
  const ParentContactLog({
    required this.id,
    required this.studentId,
    required this.contactType,
    required this.subject,
    required this.summary,
    required this.result,
    required this.contactedAt,
    this.nextActionAt,
    this.createdByName = '',
  });

  final int id;
  final int studentId;
  final String contactType;
  final String subject;
  final String summary;
  final String result;
  final DateTime contactedAt;
  final DateTime? nextActionAt;
  final String createdByName;

  factory ParentContactLog.fromJson(Map<String, dynamic> json) =>
      ParentContactLog(
        id: _int(json, 'id'),
        studentId: _int(json, 'studentId'),
        contactType: _string(json, 'contactType'),
        subject: _string(json, 'subject'),
        summary: _string(json, 'summary'),
        result: json['result'] as String? ?? '',
        contactedAt: _dateTime(json, 'contactedAt'),
        nextActionAt: DateTime.tryParse(json['nextActionAt'] as String? ?? ''),
        createdByName: json['createdByName'] as String? ?? '',
      );
}

class ParentMeetingParticipant {
  const ParentMeetingParticipant({
    required this.guardianId,
    required this.guardianName,
    required this.response,
    required this.attendance,
  });

  final int guardianId;
  final String guardianName;
  final String response;
  final String attendance;

  factory ParentMeetingParticipant.fromJson(Map<String, dynamic> json) =>
      ParentMeetingParticipant(
        guardianId: _int(json, 'guardianId'),
        guardianName: _string(json, 'guardianName'),
        response: _string(json, 'response'),
        attendance: _string(json, 'attendance'),
      );
}

class ParentMeeting {
  const ParentMeeting({
    required this.id,
    required this.title,
    required this.classId,
    required this.className,
    required this.startsAt,
    required this.location,
    required this.agenda,
    required this.status,
    required this.participants,
    this.studentId,
    this.studentName,
  });

  final int id;
  final String title;
  final int classId;
  final String className;
  final int? studentId;
  final String? studentName;
  final DateTime startsAt;
  final String location;
  final String agenda;
  final String status;
  final List<ParentMeetingParticipant> participants;

  factory ParentMeeting.fromJson(Map<String, dynamic> json) => ParentMeeting(
    id: _int(json, 'id'),
    title: _string(json, 'title'),
    classId: _int(json, 'classId'),
    className: json['className'] as String? ?? '',
    studentId: _nullableInt(json['studentId']),
    studentName: json['studentName'] as String?,
    startsAt: _dateTime(json, 'startsAt'),
    location: json['location'] as String? ?? '',
    agenda: json['agenda'] as String? ?? '',
    status: _string(json, 'status'),
    participants: (json['participants'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(ParentMeetingParticipant.fromJson)
        .toList(growable: false),
  );
}

class HomeroomClassSummary {
  const HomeroomClassSummary({
    required this.classId,
    required this.className,
    required this.studentCount,
    required this.attendanceRate,
    required this.openRiskCount,
    required this.averageGpa,
    required this.reviewProgressRate,
    required this.parentContactCount,
    required this.meetingCount,
    required this.meetingParticipationRate,
  });

  final int classId;
  final String className;
  final int studentCount;
  final double attendanceRate;
  final int openRiskCount;
  final double averageGpa;
  final double reviewProgressRate;
  final int parentContactCount;
  final int meetingCount;
  final double meetingParticipationRate;

  factory HomeroomClassSummary.fromJson(Map<String, dynamic> json) =>
      HomeroomClassSummary(
        classId: _int(json, 'classId'),
        className: _string(json, 'className'),
        studentCount: _int(json, 'studentCount'),
        attendanceRate: _double(json['attendanceRate']),
        openRiskCount: _int(json, 'openRiskCount'),
        averageGpa: _double(json['averageGpa']),
        reviewProgressRate: _double(json['reviewProgressRate']),
        parentContactCount: _int(json, 'parentContactCount'),
        meetingCount: _int(json, 'meetingCount'),
        meetingParticipationRate: _double(json['meetingParticipationRate']),
      );
}

int _int(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is num) return value.toInt();
  throw FormatException('Trường $key không hợp lệ.');
}

int? _nullableInt(Object? value) => value is num ? value.toInt() : null;
double _double(Object? value) => value is num ? value.toDouble() : 0;
String _string(Map<String, dynamic> json, String key) =>
    json[key] as String? ?? '';
DateTime _dateTime(Map<String, dynamic> json, String key) =>
    DateTime.tryParse(json[key] as String? ?? '') ??
    DateTime.fromMillisecondsSinceEpoch(0);
