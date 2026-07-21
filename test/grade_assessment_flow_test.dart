import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_screen.dart';

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
            'code': 'DA_1',
            'name': 'Dự án cá nhân',
            'weight': 1,
            'assessmentType': 'SCORE',
            'score': 8.5,
            'comment': null,
            'isGraded': true,
          },
          {
            'code': 'NL_1',
            'name': 'Năng lực',
            'weight': 1,
            'assessmentType': 'PASS_FAIL',
            'score': null,
            'comment': 'PASS',
            'isGraded': true,
          },
          {
            'code': 'PX_1',
            'name': 'Phản hồi',
            'weight': 1,
            'assessmentType': 'COMMENT',
            'score': null,
            'comment': 'Chủ động và hợp tác tốt',
            'isGraded': true,
          },
        ],
      },
      {
        'subjectName': 'Vật lý',
        'average': null,
        'complete': false,
        'scores': [
          {
            'code': 'DA_1',
            'name': 'Dự án cá nhân',
            'assessmentType': 'SCORE',
            'score': null,
            'comment': null,
            'isGraded': false,
          },
          {
            'code': 'NL_1',
            'name': 'Năng lực',
            'assessmentType': 'PASS_FAIL',
            'score': null,
            'comment': null,
            'isGraded': false,
          },
          {
            'code': 'PX_1',
            'name': 'Phản hồi',
            'assessmentType': 'COMMENT',
            'score': null,
            'comment': null,
            'isGraded': false,
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
  testWidgets('student sees configured columns and subjects without scores', (
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

    expect(find.text('Dự án cá nhân'), findsOneWidget);
    expect(find.text('Năng lực'), findsOneWidget);
    expect(find.text('Phản hồi'), findsOneWidget);
    expect(find.text('Đạt'), findsOneWidget);
    expect(find.text('Chủ động và hợp tác tốt'), findsOneWidget);
    expect(find.text('Vật lý'), findsOneWidget);
    expect(find.text('8.5'), findsWidgets);
    expect(find.text('BẢNG TỔNG KẾT HỌC LỰC'), findsOneWidget);
    expect(find.text('BẢNG ĐIỂM CHI TIẾT'), findsOneWidget);
  });
}
