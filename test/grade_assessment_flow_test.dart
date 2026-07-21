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
