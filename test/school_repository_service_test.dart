import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/repositories/repositories.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';

Future<List<Map<String, dynamic>>> loadReportFixture(
  String collectionName,
) async {
  final file = File('test/fixtures/reports/$collectionName.json');
  final rawJson = await file.readAsString();
  final decoded = jsonDecode(rawJson);

  if (decoded is! List<dynamic>) {
    throw FormatException('$collectionName fixture must be a JSON array.');
  }

  return decoded.map((item) {
    if (item is! Map<String, dynamic>) {
      throw FormatException('$collectionName fixture items must be objects.');
    }
    return item;
  }).toList();
}

void main() {
  late SchoolRepository repository;

  setUp(() {
    repository = const SchoolRepository.fromJsonLoader(loadReportFixture);
  });

  group('SchoolRepository', () {
    test('loads student data', () async {
      final student = await repository.loadStudent(10);

      expect(student.id, 10);
      expect(student.code, 'STU-001');
      expect(student.status, 'ACTIVE');
      expect(student.dateOfBirth, DateTime(2010, 9, 1));
    });

    test('throws when student is not found', () async {
      expect(
        () => repository.loadStudent(999),
        throwsA(isA<StudentNotFoundException>()),
      );
    });

    test('loads grades for one student', () async {
      final grades = await repository.loadGrades(10);

      expect(grades, hasLength(3));
      expect(grades.map((grade) => grade.id), [101, 102, 103]);
      expect(grades.map((grade) => grade.value), [8.5, 7.5, 8.5]);
    });

    test('loads attendance stats', () async {
      final attendance = await repository.loadAttendanceStats(10);

      expect(attendance.presentCount, 4);
      expect(attendance.absentCount, 1);
      expect(attendance.excusedCount, 0);
      expect(attendance.totalSessions, 5);
      expect(attendance.attendanceRate, closeTo(0.8, 0.001));
    });

    test('loads missing assignment count', () async {
      expect(await repository.loadMissingHomeworkCount(10), 1);
      expect(await repository.loadMissingHomeworkCount(11), 0);
    });

    test('loads unread announcement count', () async {
      expect(await repository.loadUnreadAnnouncementCount(10), 2);
      expect(await repository.loadUnreadAnnouncementCount(11), 1);
    });

    test(
      'falls back to json grades when api client is not configured',
      () async {
        final grades = await repository.getGrades(studentId: 10);

        expect(grades, hasLength(3));
        expect(grades.first.id, 101);
      },
    );
  });

  group('SchoolService', () {
    test('builds a full student summary from api data', () async {
      final service = SchoolService(repository: repository);

      final summary = await service.buildSummary(10);

      expect(summary.student.code, 'STU-001');
      expect(summary.grades, hasLength(3));
      expect(summary.averageGrade, closeTo(8.166, 0.001));
      expect(summary.weightedAverageGrade, closeTo(8.0, 0.001));
      expect(summary.attendance.totalSessions, 5);
      expect(summary.missingHomeworkCount, 1);
      expect(summary.unreadAnnouncementCount, 2);
      expect(summary.needsAttention, isTrue);
    });

    test('streams notifications through repository', () async {
      final service = SchoolService(repository: SchoolRepository.demo());

      final notification = await service.watchNotifications().first;

      expect(notification.title, 'Diem moi');
    });
  });
}
