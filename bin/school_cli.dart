import 'dart:io';

import 'package:myfschoolse1913/vn/edu/fpt/src/repositories/repositories.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';

Future<void> main(List<String> args) async {
  final studentId = args.isEmpty ? 10 : int.parse(args.first);
  final report = await buildSchoolCliReport(studentId: studentId);

  stdout.write(report);
}

Future<String> buildSchoolCliReport({
  int studentId = 10,
  SchoolService? service,
}) async {
  final schoolService =
      service ?? SchoolService(repository: SchoolRepository.demo());
  final repository = schoolService.repository;

  final student = await repository.loadStudent(studentId);
  final grades = await repository.loadGrades(studentId);
  final attendance = await repository.loadAttendanceStats(studentId);
  final missingHomework = await repository.loadMissingHomeworkCount(studentId);
  final unreadAnnouncements = await repository.loadUnreadAnnouncementCount(
    studentId,
  );
  final summary = await schoolService.buildSummary(studentId);
  final firstNotification = await schoolService.watchNotifications().first;
  final attendanceRate = (attendance.attendanceRate * 100).toStringAsFixed(0);

  final buffer = StringBuffer()
    ..writeln('MYFSCHOOL CLI CHECK')
    ..writeln('====================')
    ..writeln('Student: ${student.fullName} (${student.code})')
    ..writeln('Status: ${student.status}')
    ..writeln('Grades: ${grades.length}');

  for (final grade in grades) {
    buffer.writeln(
      '- ${grade.subjectName}: ${grade.value.toStringAsFixed(1)} '
      '(weight ${grade.weight.toStringAsFixed(1)})',
    );
  }

  buffer
    ..writeln('Attendance sessions: ${attendance.totalSessions}')
    ..writeln('Attendance rate: $attendanceRate%')
    ..writeln('Absent count: ${attendance.absentCount}')
    ..writeln('Missing homework: $missingHomework')
    ..writeln('Unread announcements: $unreadAnnouncements')
    ..writeln('Average grade: ${summary.averageGrade.toStringAsFixed(2)}')
    ..writeln(
      'Weighted average grade: '
      '${summary.weightedAverageGrade.toStringAsFixed(2)}',
    )
    ..writeln('Needs attention: ${summary.needsAttention}')
    ..writeln(
      'First notification: '
      '${firstNotification.title} - ${firstNotification.message}',
    )
    ..writeln('OK: CLI functions printed successfully');

  return buffer.toString();
}
