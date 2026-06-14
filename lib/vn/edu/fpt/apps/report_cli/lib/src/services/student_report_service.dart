import 'package:myfschoolse1913/vn/edu/fpt/src/core/core.dart';
import '../repositories/report_repository.dart';

class StudentReportService {
  final ReportRepository repository;

  const StudentReportService({required this.repository});

  Future<StudentSummary> buildSummary(int studentId) async {
    final studentFuture = repository.loadStudent(studentId);
    final gradesFuture = repository.loadGrades(studentId);
    final attendanceFuture = repository.loadAttendanceStats(studentId);
    final unreadFuture = repository.loadUnreadAnnouncementCount(studentId);
    final missingHomeworkFuture = repository.loadMissingHomeworkCount(
      studentId,
    );
    final student = await studentFuture;
    final grades = await gradesFuture;
    final attendance = await attendanceFuture;
    final unreadCount = await unreadFuture;
    final missingHomeWork = await missingHomeworkFuture;

    return StudentSummary(
      student: student,
      grades: grades,
      attendance: attendance,
      unreadAnnouncementCount: unreadCount,
      missingHomeworkCount: missingHomeWork,
    );
  }
}
