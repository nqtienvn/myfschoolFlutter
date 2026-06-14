import 'package:myfschoolse1913/vn/edu/fpt/src/core/core.dart';

abstract interface class ReportRepository {
  Future<Student> loadStudent(int studentId);

  Future<List<Grade>> loadGrades(int studentId);

  Future<AttendanceStats> loadAttendanceStats(int studentId);

  Future<int> loadUnreadAnnouncementCount(int studentId);

  Future<int> loadMissingHomeworkCount(int studentId);
}
