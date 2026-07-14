import '../exception/parse_exception.dart';

class TeacherDashboardStatsDto {
  const TeacherDashboardStatsDto({
    required this.classId,
    required this.className,
    required this.academicYearId,
    required this.academicYearName,
    required this.semesterId,
    required this.semesterName,
    required this.attendanceRate,
    required this.averageGpa,
    required this.parentReadRate,
  });

  final int classId;
  final String className;
  final int academicYearId;
  final String academicYearName;
  final int semesterId;
  final String semesterName;
  final double? attendanceRate;
  final double? averageGpa;
  final double? parentReadRate;

  factory TeacherDashboardStatsDto.fromJson(Map<String, dynamic> json) {
    final classId = json['classId'];
    final academicYearId = json['academicYearId'];
    final semesterId = json['semesterId'];
    if (classId is! num || academicYearId is! num || semesterId is! num) {
      throw const ParseException(
        'Dữ liệu thống kê giáo viên thiếu lớp hoặc kỳ học.',
      );
    }
    return TeacherDashboardStatsDto(
      classId: classId.toInt(),
      className: json['className'] as String? ?? '',
      academicYearId: academicYearId.toInt(),
      academicYearName: json['academicYearName'] as String? ?? '',
      semesterId: semesterId.toInt(),
      semesterName: json['semesterName'] as String? ?? '',
      attendanceRate: _nullableDouble(json['attendanceRate']),
      averageGpa: _nullableDouble(json['averageGpa']),
      parentReadRate: _nullableDouble(json['parentReadRate']),
    );
  }

  static double? _nullableDouble(Object? value) =>
      value is num ? value.toDouble() : null;
}
