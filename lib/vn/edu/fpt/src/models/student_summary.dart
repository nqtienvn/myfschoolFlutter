import 'grade.dart';
import 'grade_analytics.dart';
import 'attendance_stats.dart';
import 'student.dart';

class StudentSummary {
  final Student student; //quan he hinh thoi den - rang buoc chat che
  final List<Grade> _grades;
  final AttendanceStats attendance;
  final int unreadAnnouncementCount;
  final int missingHomeworkCount;

  const StudentSummary({
    required this.student,
    required List<Grade> grades,
    required this.attendance,
    required this.unreadAnnouncementCount,
    required this.missingHomeworkCount,
  }) : _grades = grades;

  List<Grade> get grades => List.unmodifiable(_grades);

  double get averageGrade => gradeAverage(_grades);

  double get weightedAverageGrade => weightedGradeAverage(_grades);

  bool get hasAttendanceWarning => attendance.absentCount >= 2;

  bool get hasAcademicWarning => _grades.isNotEmpty && averageGrade < 5;

  bool get needsAttention =>
      hasAttendanceWarning || hasAcademicWarning || missingHomeworkCount > 0;

  String get shortStatus {
    if (needsAttention) {
      return 'Có nội dung cần phụ huynh chú ý';
    }
    return 'Tình hình học tập ổn định';
  }

  factory StudentSummary.empty(Student student) {
    return StudentSummary(
      student: student,
      grades: const [],
      attendance: const AttendanceStats.empty(),
      unreadAnnouncementCount: 0,
      missingHomeworkCount: 0,
    );
  }
}
