import '../models/models.dart';
import '../repositories/school_repository.dart';

class SchoolService {
  final SchoolRepository repository;

  const SchoolService({required this.repository});

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
    final missingHomework = await missingHomeworkFuture;

    return StudentSummary(
      student: student,
      grades: grades,
      attendance: attendance,
      unreadAnnouncementCount: unreadCount,
      missingHomeworkCount: missingHomework,
    );
  }

  Stream<AppNotification> watchNotifications() {
    return repository.watchNotifications();
  }
}
