import 'dart:collection';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_stats_screen.dart';

class _Call {
  const _Call(this.path, this.query);

  final String path;
  final Map<String, String?> query;
}

class _RecordingBackend extends BackendApiClient {
  _RecordingBackend(List<Object?> responses)
    : responses = Queue<Object?>.of(responses),
      super(baseUrl: 'http://localhost');

  final Queue<Object?> responses;
  final List<_Call> calls = [];

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    expect(token, 'teacher-token');
    calls.add(_Call(path, query));
    return responses.removeFirst();
  }
}

class _FakeDashboardClient extends DashboardApiClient {
  _FakeDashboardClient()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  @override
  Future<TeacherDashboardStatsDto> getTeacherStats({
    required String token,
    required int academicYearId,
    required int semesterId,
  }) async {
    expect(token, 'teacher-token');
    expect(academicYearId, 26);
    expect(semesterId, 1);
    return _stats;
  }
}

class _FakeAcademicClient extends HomeroomAcademicApiClient {
  _FakeAcademicClient()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  int? detailClassId;
  int? rankingClassId;
  int? rankingSemesterId;
  int? resultStudentId;
  int? resultSemesterId;

  @override
  Future<HomeroomClassDetailDto> getClassDetail({
    required String token,
    required int classId,
  }) async {
    detailClassId = classId;
    return _classDetail;
  }

  @override
  Future<HomeroomClassRankingDto> getClassRanking({
    required String token,
    required int classId,
    required int semesterId,
  }) async {
    rankingClassId = classId;
    rankingSemesterId = semesterId;
    return _ranking;
  }

  @override
  Future<HomeroomStudentResultDto?> getStudentSemesterResult({
    required String token,
    required int studentId,
    required int semesterId,
  }) async {
    resultStudentId = studentId;
    resultSemesterId = semesterId;
    return studentId == 9 ? _studentResult : null;
  }
}

const _stats = TeacherDashboardStatsDto(
  classId: 12,
  className: '12A1',
  academicYearId: 26,
  academicYearName: '2026-2027',
  semesterId: 1,
  semesterName: 'Học kỳ I',
  attendanceRate: 94.25,
  averageGpa: 8.4,
  parentReadRate: 82,
);

const _classDetail = HomeroomClassDetailDto(
  id: 12,
  name: '12A1',
  gradeLevel: 12,
  academicYearId: 26,
  academicYearName: '2026-2027',
  schoolName: 'FPT Schools',
  students: [
    HomeroomStudentDto(
      id: 9,
      name: 'Nguyễn An',
      studentCode: 'HS009',
      className: '12A1',
    ),
    HomeroomStudentDto(
      id: 10,
      name: 'Trần Bình',
      studentCode: 'HS010',
      className: '12A1',
    ),
  ],
);

const _ranking = HomeroomClassRankingDto(
  classId: 12,
  className: '12A1',
  semesterId: 1,
  semesterName: 'Học kỳ I',
  rankings: [
    HomeroomRankEntryDto(
      rank: 1,
      studentId: 9,
      studentName: 'Nguyễn An',
      studentCode: 'HS009',
      gpa: 9.15,
      academicAbility: 'Giỏi',
      conduct: 'Tốt',
    ),
  ],
);

const _studentResult = HomeroomStudentResultDto(
  id: 101,
  studentId: 9,
  studentName: 'Nguyễn An',
  semesterId: 1,
  semesterName: 'Học kỳ I',
  classId: 12,
  className: '12A1',
  gpa: 9.15,
  rank: 1,
  honor: 'Học sinh xuất sắc',
  conduct: 'Tốt',
  academicAbility: 'Giỏi',
);

void main() {
  test('homeroom academic client maps all secured backend endpoints', () async {
    final backend = _RecordingBackend([
      {
        'id': 12,
        'name': '12A1',
        'gradeLevel': 12,
        'academicYearId': 26,
        'academicYearName': '2026-2027',
        'schoolName': 'FPT Schools',
        'students': [
          {
            'id': 9,
            'name': 'Nguyễn An',
            'studentCode': 'HS009',
            'className': '12A1',
          },
        ],
      },
      {
        'classId': 12,
        'className': '12A1',
        'semesterId': 1,
        'semesterName': 'Học kỳ I',
        'rankings': [
          {
            'rank': 1,
            'studentId': 9,
            'studentName': 'Nguyễn An',
            'studentCode': 'HS009',
            'gpa': 9.15,
            'academicAbility': 'Giỏi',
            'conduct': 'Tốt',
          },
        ],
      },
      {
        'id': 101,
        'studentId': 9,
        'studentName': 'Nguyễn An',
        'semesterId': 1,
        'semesterName': 'Học kỳ I',
        'classId': 12,
        'className': '12A1',
        'gpa': 9.15,
        'rank': 1,
        'honor': 'Học sinh xuất sắc',
        'conduct': 'Tốt',
        'academicAbility': 'Giỏi',
      },
      null,
    ]);
    final client = HomeroomAcademicApiClient(backend: backend);

    final classDetail = await client.getClassDetail(
      token: 'teacher-token',
      classId: 12,
    );
    final ranking = await client.getClassRanking(
      token: 'teacher-token',
      classId: 12,
      semesterId: 1,
    );
    final result = await client.getStudentSemesterResult(
      token: 'teacher-token',
      studentId: 9,
      semesterId: 1,
    );
    final missingResult = await client.getStudentSemesterResult(
      token: 'teacher-token',
      studentId: 10,
      semesterId: 1,
    );

    expect(classDetail.students.single.studentCode, 'HS009');
    expect(ranking.rankings.single.gpa, 9.15);
    expect(result?.honor, 'Học sinh xuất sắc');
    expect(missingResult, isNull);
    expect(backend.calls.map((call) => call.path), [
      '/api/classes/12',
      '/api/semester-results/ranking',
      '/api/semester-results',
      '/api/semester-results',
    ]);
    expect(backend.calls[1].query, {'classId': '12', 'semesterId': '1'});
    expect(backend.calls[2].query, {'studentId': '9', 'semesterId': '1'});
    expect(backend.calls[3].query, {'studentId': '10', 'semesterId': '1'});
  });

  testWidgets(
    'homeroom profile keeps students without results and opens real detail',
    (tester) async {
      await tester.binding.setSurfaceSize(const Size(430, 900));
      addTearDown(() => tester.binding.setSurfaceSize(null));
      final api = _FakeAcademicClient();
      final period = AcademicPeriod(
        academicYearId: 26,
        academicYearName: '2026-2027',
        semesterId: 1,
        semesterName: 'Học kỳ I',
        startDate: DateTime(2026, 7, 1),
        endDate: DateTime(2026, 12, 31),
        isCurrent: true,
        academicYearStatus: 'ACTIVE',
        semesterStatus: 'ACTIVE',
      );
      final controller = AcademicPeriodController(token: 'teacher-token')
        ..periods = [period]
        ..selected = period
        ..isLoading = false;
      addTearDown(controller.dispose);

      await tester.pumpWidget(
        MaterialApp(
          home: AcademicPeriodScope(
            controller: controller,
            child: TeacherStatsScreen(
              token: 'teacher-token',
              apiClient: _FakeDashboardClient(),
              academicApiClient: api,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Tổng quan lớp 12A1'), findsOneWidget);
      expect(api.detailClassId, 12);
      expect(api.rankingClassId, 12);
      expect(api.rankingSemesterId, 1);

      await tester.enterText(find.byType(TextField), 'HS010');
      await tester.pump();
      expect(find.text('Trần Bình'), findsOneWidget);
      expect(find.text('Nguyễn An'), findsNothing);
      await tester.enterText(find.byType(TextField), '');
      await tester.pump();

      await tester.scrollUntilVisible(
        find.text('Trần Bình'),
        250,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.text('Trần Bình'), findsOneWidget);
      expect(find.text('Chưa tổng kết'), findsOneWidget);

      await tester.scrollUntilVisible(
        find.text('Nguyễn An'),
        -200,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(find.text('Nguyễn An'));
      await tester.pumpAndSettle();

      expect(find.text('Tổng kết học sinh'), findsOneWidget);
      expect(find.text('9.15'), findsOneWidget);
      expect(find.text('#1'), findsOneWidget);
      expect(find.text('Học sinh xuất sắc'), findsOneWidget);
      expect(api.resultStudentId, 9);
      expect(api.resultSemesterId, 1);
      expect(tester.takeException(), isNull);
    },
  );
}
