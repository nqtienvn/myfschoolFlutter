import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/repositories/repositories.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';

Future<List<Map<String, dynamic>>> fixtures(String name) async =>
    switch (name) {
      'students' => [
        {
          'id': 10,
          'code': 'STU-001',
          'fullName': 'Nguyễn Minh An',
          'status': 'ACTIVE',
          'dateOfBirth': '2010-09-01',
        },
      ],
      'transcripts' => [
        {
          'studentId': 10,
          'subjectName': 'Toán',
          'columns': [
            {
              'id': 1,
              'code': 'TX_1',
              'name': 'Thường xuyên 1',
              'weight': 1,
              'entryRole': 'subjectTeacher',
            },
            {
              'id': 2,
              'code': 'GK_1',
              'name': 'Giữa kỳ',
              'weight': 2,
              'entryRole': 'admin',
            },
          ],
          'scores': {'1': 8.0, '2': 9.0},
          'average': 8.7,
        },
      ],
      'attendance' => [
        {'studentId': 10, 'status': 'PRESENT'},
      ],
      'assignments' => [],
      'announcements' => [],
      _ => [],
    };

void main() {
  final repository = SchoolRepository.fromJsonLoader(fixtures);
  test('loads configured transcript', () async {
    final rows = await repository.loadTranscript(10);
    expect(rows.single.columns.map((c) => c.code), ['TX_1', 'GK_1']);
    expect(rows.single.average, 8.7);
  });
  test('builds summary from transcript', () async {
    final summary = await SchoolService(
      repository: repository,
    ).buildSummary(10);
    expect(summary.transcript.single.subjectName, 'Toán');
    expect(summary.averageGrade, 8.7);
  });
}
