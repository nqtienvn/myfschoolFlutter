import 'attendance_stats.dart';
import 'gradebook.dart';
import 'student.dart';

class StudentSummary {
  final Student student;
  final List<SubjectTranscript> transcript;
  final AttendanceStats attendance;
  final int unreadAnnouncementCount;
  final int missingHomeworkCount;
  const StudentSummary({
    required this.student,
    required this.transcript,
    required this.attendance,
    required this.unreadAnnouncementCount,
    required this.missingHomeworkCount,
  });
  double get averageGrade {
    final values = transcript
        .map((row) => row.average)
        .whereType<double>()
        .toList();
    return values.isEmpty ? 0 : values.reduce((a, b) => a + b) / values.length;
  }

  bool get hasAttendanceWarning => attendance.absentCount >= 2;
  bool get hasAcademicWarning => transcript.isNotEmpty && averageGrade < 5;
  bool get needsAttention =>
      hasAttendanceWarning || hasAcademicWarning || missingHomeworkCount > 0;
  String get shortStatus => needsAttention
      ? 'Có nội dung cần phụ huynh chú ý'
      : 'Tình hình học tập ổn định';
  factory StudentSummary.empty(Student student) => StudentSummary(
    student: student,
    transcript: const [],
    attendance: const AttendanceStats.empty(),
    unreadAnnouncementCount: 0,
    missingHomeworkCount: 0,
  );
}
