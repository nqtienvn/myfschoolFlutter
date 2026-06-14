import 'dart:convert';
import 'dart:io';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/core/core.dart';
import 'report_repository.dart';
import '../dto/student_dto.dart';

class StudentNoFoundException implements Exception {
  final int studentId;

  StudentNoFoundException(this.studentId);

  @override
  String toString() => 'Không tìm thấy học sinh id=$studentId';
}

class LocalReportRepository implements ReportRepository {
  final String assetsPath;

  const LocalReportRepository({
    this.assetsPath = 'lib/vn/edu/fpt/apps/report_cli/assets',
  });

  Future<List<Map<String, dynamic>>> _loadJsonList(String fileName) async {
    final File file = File("$assetsPath/$fileName");
    final String rawJson = await file.readAsString();
    final Object? decoded = jsonDecode(rawJson);
    if (decoded is! List<dynamic>) {
      throw FormatException('$fileName phải chứa Json Array');
    }
    return decoded.map((item) {
      if (item is! Map<String, dynamic>) {
        throw FormatException('$fileName chứa phần tử không phải JSON object.');
      }
      return item;
    }).toList();
  }

  @override
  Future<AttendanceStats> loadAttendanceStats(int studentId) async {
    final items = await _loadJsonList('attendance.json');
    final statuses = items
        .where((item) => item['studentId'] == studentId)
        .map((item) => item['status'] as String)
        .toList();

    return AttendanceStats.fromStatuses(statuses);
  } //lọc hết thông tin của 1 student luon

  @override
  Future<List<Grade>> loadGrades(int studentId) async {
    final items = await _loadJsonList('grades.json');
    return items
        .where((item) => item['studentId'] == studentId)
        .map((item) => GradeDto.fromJson(item).toDomain())
        .toList();
  }

  @override
  Future<int> loadMissingHomeworkCount(int studentId) async {
    final items = await _loadJsonList('assignments.json');
    return items
        .where((item) => item['studentId'] == studentId && item['status'] == 'MISSING')
        .length;
  }

  @override
  Future<Student> loadStudent(int studentId) async {
    final items = await _loadJsonList('students.json');
    final matches = items.where((item) => item['id'] == studentId);
    if (matches.isEmpty) {
      throw StudentNoFoundException(studentId);
    }
    return StudentDto.fromJson(matches.first).toDomain();
  }

  //hiển thị hộp thư
  @override
  Future<int> loadUnreadAnnouncementCount(int studentId) async {
    final items = await _loadJsonList('announcements.json');
    return items
        .where(
          (item) => item['studentId'] == studentId && item['isRead'] == false,
        )
        .length;
  }
}
