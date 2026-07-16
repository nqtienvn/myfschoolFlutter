import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_web_screen.dart';

class _TeacherGradebookApi extends GradebookApiClient {
  _TeacherGradebookApi({required this.type})
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final String type;
  List<Map<String, dynamic>>? savedEntries;

  @override
  Future<List<Map<String, dynamic>>> getMyAssignments({
    required String token,
    required int academicYearId,
  }) async => [
    {
      'classId': 12,
      'className': '12A1',
      'subjectId': 3,
      'subjectName': 'Toán',
      'subjectCode': 'MATH',
    },
  ];

  @override
  Future<Map<String, dynamic>> getGradeBook({
    required String token,
    required int classId,
    required int subjectId,
    required int semesterId,
  }) async => {
    'id': 20,
    'items': [
      {
        'id': 101,
        'name': type == 'COMMENT' ? 'Nhận xét học tập' : 'Kỹ năng thực hành',
        'weight': 1,
        'assessmentType': type,
        'entryRole': 'SUBJECT_TEACHER',
      },
    ],
  };

  @override
  Future<List<Map<String, dynamic>>> getStudents({
    required String token,
    required int gradeBookId,
  }) async => [
    {
      'studentId': 1,
      'studentName': 'Nguyễn An',
      'studentCode': 'HS001',
      'gradeItemId': 101,
      'score': null,
      'comment': null,
      'isGraded': false,
    },
  ];

  @override
  Future<void> updateScores({
    required String token,
    required int gradeItemId,
    required List<Map<String, dynamic>> entries,
  }) async {
    savedEntries = entries;
  }
}

class _TranscriptApi extends GradebookApiClient {
  _TranscriptApi()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  @override
  Future<Map<String, dynamic>> getTranscript({
    required String token,
    required int academicYearId,
    required int semesterId,
    int? studentId,
  }) async => {
    'studentName': 'Nguyễn An',
    'subjects': [
      {
        'subjectName': 'Kỹ năng sống',
        'average': 8.5,
        'complete': true,
        'scores': [
          {
            'name': 'Kiểm tra miệng',
            'weight': 1,
            'assessmentType': 'SCORE',
            'score': 8.5,
            'comment': null,
          },
          {
            'name': 'Kiểm tra 15 phút',
            'weight': 1,
            'assessmentType': 'PASS_FAIL',
            'score': null,
            'comment': 'PASS',
          },
          {
            'name': 'Nhận xét',
            'weight': 1,
            'assessmentType': 'COMMENT',
            'score': null,
            'comment': 'Chủ động và hợp tác tốt',
          },
        ],
      },
    ],
  };
}

AcademicPeriodController _periodController() {
  final period = AcademicPeriod(
    academicYearId: 26,
    academicYearName: '2026-2027',
    semesterId: 2,
    semesterName: 'Học kỳ II',
    startDate: DateTime(2027, 1, 1),
    endDate: DateTime(2027, 5, 31),
  );
  return AcademicPeriodController(token: 'token')
    ..periods = [period]
    ..selected = period
    ..isLoading = false;
}

void main() {
  Future<void> pumpTeacher(
    WidgetTester tester,
    _TeacherGradebookApi api,
  ) async {
    final periods = _periodController();
    addTearDown(periods.dispose);
    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periods,
          child: GradesWebScreen(token: 'teacher-token', apiClient: api),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('teacher saves PASS_FAIL as a canonical comment value', (
    tester,
  ) async {
    final api = _TeacherGradebookApi(type: 'PASS_FAIL');
    await pumpTeacher(tester, api);

    await tester.tap(find.byKey(const ValueKey('101-1-pass-fail')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đạt').last);
    await tester.pumpAndSettle();
    await tester.drag(find.byType(ListView), const Offset(0, -800));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lưu đầu điểm'));
    await tester.pumpAndSettle();

    expect(api.savedEntries, [
      {'studentId': 1, 'comment': 'PASS', 'isGraded': true},
    ]);
  });

  testWidgets('teacher saves COMMENT without a numeric score', (tester) async {
    final api = _TeacherGradebookApi(type: 'COMMENT');
    await pumpTeacher(tester, api);

    await tester.enterText(
      find.byKey(const ValueKey('101-1-comment')),
      'Chủ động và hợp tác tốt',
    );
    await tester.drag(find.byType(ListView), const Offset(0, -800));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lưu đầu điểm'));
    await tester.pumpAndSettle();

    expect(api.savedEntries, [
      {'studentId': 1, 'comment': 'Chủ động và hợp tác tốt', 'isGraded': true},
    ]);
  });

  testWidgets('student sees all assessment types and only numeric average', (
    tester,
  ) async {
    final periods = _periodController();
    addTearDown(periods.dispose);
    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periods,
          child: GradesScreen(
            token: 'student-token',
            apiClient: _TranscriptApi(),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Đ'), findsOneWidget);
    expect(find.text('Chủ động và hợp tác tốt'), findsNothing);
    expect(find.text('8.5'), findsWidgets);
    expect(find.text('BẢNG TỔNG KẾT HỌC LỰC'), findsOneWidget);
    expect(find.text('BẢNG ĐIỂM CHI TIẾT'), findsOneWidget);
  });
}
