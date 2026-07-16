import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart';

void main() {
  test(
    'class report client always carries academic year, semester and class',
    () async {
      final backend = _RecordingBackend();
      final result = await HomeroomMonitoringApiClient(backend: backend)
          .getClassSummaries(
            token: 'teacher-token',
            academicYearId: 26,
            semesterId: 3,
            classId: 12,
          );
      expect(result.single.className, '12A1');
      expect(backend.path, '/api/homeroom/reports/class-summary');
      expect(backend.query, {
        'academicYearId': '26',
        'semesterId': '3',
        'classId': '12',
      });
    },
  );

  test('internal violation parser keeps submitted metadata', () {
    final event = StudentEvent.fromJson(_eventJson);
    expect(event.eventType, 'VIOLATION');
    expect(event.status, 'SUBMITTED');
    expect(event.studentId, 9);
  });
}

const _summaryJson = <String, dynamic>{
  'classId': 12,
  'className': '12A1',
  'studentCount': 35,
  'attendanceRate': 96.5,
  'openRiskCount': 2,
  'averageGpa': 7.8,
  'reviewProgressRate': 82.0,
  'parentContactCount': 4,
  'meetingCount': 1,
  'meetingParticipationRate': 75.0,
  'rewardCount': 0,
  'violationCount': 1,
};

const _eventJson = <String, dynamic>{
  'id': 41,
  'studentId': 9,
  'eventType': 'VIOLATION',
  'category': 'Nề nếp',
  'title': 'Đi học muộn',
  'description': 'Ghi nhận nội bộ để Admin tổng hợp.',
  'eventDate': '2026-09-15',
  'status': 'SUBMITTED',
};

class _RecordingBackend extends BackendApiClient {
  _RecordingBackend() : super(baseUrl: 'http://localhost');
  String? path;
  Map<String, String?> query = const {};

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    this.path = path;
    this.query = query;
    return [_summaryJson];
  }
}
