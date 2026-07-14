import '../exception/parse_exception.dart';

class StudentDashboardStatsDto {
  const StudentDashboardStatsDto({
    required this.studentId,
    required this.studentName,
    required this.studentCode,
    required this.classId,
    required this.className,
    required this.schoolName,
    required this.academicYearId,
    required this.academicYearName,
    required this.semesterId,
    required this.semesterName,
    required this.attendanceRate,
    required this.presentSessions,
    required this.absentSessions,
    this.currentGpa,
    this.academicAbility,
    this.conduct,
    this.classRank,
    this.homeroomTeacherName,
    this.homeroomTeacherPhone,
  });

  final int studentId;
  final String studentName;
  final String studentCode;
  final int classId;
  final String className;
  final String schoolName;
  final int academicYearId;
  final String academicYearName;
  final int semesterId;
  final String semesterName;
  final double attendanceRate;
  final int presentSessions;
  final int absentSessions;
  final double? currentGpa;
  final String? academicAbility;
  final String? conduct;
  final int? classRank;
  final String? homeroomTeacherName;
  final String? homeroomTeacherPhone;

  factory StudentDashboardStatsDto.fromJson(Map<String, dynamic> json) {
    return StudentDashboardStatsDto(
      studentId: _requiredInt(json, 'studentId'),
      studentName: _requiredString(json, 'studentName'),
      studentCode: _requiredString(json, 'studentCode'),
      classId: _requiredInt(json, 'classId'),
      className: _requiredString(json, 'className'),
      schoolName: _requiredString(json, 'schoolName'),
      academicYearId: _requiredInt(json, 'academicYearId'),
      academicYearName: _requiredString(json, 'academicYearName'),
      semesterId: _requiredInt(json, 'semesterId'),
      semesterName: _requiredString(json, 'semesterName'),
      attendanceRate: _requiredDouble(json, 'attendanceRate'),
      presentSessions: _requiredInt(json, 'presentSessions'),
      absentSessions: _requiredInt(json, 'absentSessions'),
      currentGpa: _nullableDouble(json['currentGpa']),
      academicAbility: json['academicAbility'] as String?,
      conduct: json['conduct'] as String?,
      classRank: _nullableInt(json['classRank']),
      homeroomTeacherName: json['homeroomTeacherName'] as String?,
      homeroomTeacherPhone: json['homeroomTeacherPhone'] as String?,
    );
  }

  static int _requiredInt(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value is! num) {
      throw ParseException('Field "$key" must be numeric.');
    }
    return value.toInt();
  }

  static double _requiredDouble(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value is! num) {
      throw ParseException('Field "$key" must be numeric.');
    }
    return value.toDouble();
  }

  static String _requiredString(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value is! String) {
      throw ParseException('Field "$key" must be text.');
    }
    return value;
  }

  static double? _nullableDouble(Object? value) =>
      value is num ? value.toDouble() : null;

  static int? _nullableInt(Object? value) =>
      value is num ? value.toInt() : null;
}
