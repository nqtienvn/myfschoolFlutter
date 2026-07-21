import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_screen.dart';

void main() {
  test('periodic report maps progress, conduct and subject reviews', () {
    final report = StudentPeriodicReport.fromJson(_reportJson);

    expect(report.isPublished, isTrue);
    expect(report.subjectsComplete, isTrue);
    expect(report.conduct, 'Tốt');
    expect(report.suggestedConduct, 'Khá');
    expect(report.subjectReviews.single.status, 'SUBMITTED');
    expect(report.subjectReviews.single.canEdit, isFalse);
  });

  test(
    'published report client always sends academic year and semester',
    () async {
      final backend = _RecordingBackend();

      final report = await PeriodicReviewApiClient(backend: backend)
          .getPublishedReport(
            token: 'parent-token',
            studentId: 9,
            academicYearId: 26,
            semesterId: 3,
          );

      expect(report.studentId, 9);
      expect(backend.path, '/api/periodic-reports/students/9');
      expect(backend.query, {'academicYearId': '26', 'semesterId': '3'});
    },
  );

  testWidgets(
    'parent still sees published homeroom and subject reviews in grades',
    (tester) async {
      final api = _FakePeriodicReviewApi();
      final period = AcademicPeriod(
        academicYearId: 26,
        academicYearName: '2026-2027',
        semesterId: 3,
        semesterName: 'Học kỳ I',
        startDate: DateTime(2026, 7, 1),
        endDate: DateTime(2026, 12, 31),
        isCurrent: true,
        academicYearStatus: 'ACTIVE',
        semesterStatus: 'ACTIVE',
      );
      final controller = AcademicPeriodController(token: 'parent-token')
        ..periods = [period]
        ..selected = period
        ..isLoading = false;
      addTearDown(controller.dispose);

      await tester.pumpWidget(
        MaterialApp(
          home: AcademicPeriodScope(
            controller: controller,
            child: GradesScreen(
              token: 'parent-token',
              studentId: 9,
              studentName: 'Nguyễn An',
              apiClient: _PublishedTranscriptApi(),
              academicApiClient: _AcademicApi(),
              periodicReviewApi: api,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(api.requestedStudentId, 9);
      expect(find.text('Bảng điểm · Nguyễn An'), findsOneWidget);
      expect(find.text('Nhận xét GVCN'), findsOneWidget);
      expect(
        find.text('Chăm ngoan và tích cực tham gia hoạt động lớp.'),
        findsOneWidget,
      );
      await tester.drag(find.byType(ListView), const Offset(0, -700));
      await tester.pumpAndSettle();
      expect(find.text('Có tiến bộ rõ rệt.'), findsOneWidget);
    },
  );
}

const _reportJson = <String, dynamic>{
  'id': 71,
  'academicYearId': 26,
  'semesterId': 3,
  'classId': 12,
  'className': '12A1',
  'studentId': 9,
  'studentName': 'Nguyễn An',
  'studentCode': 'HS009',
  'homeroomTeacherName': 'Cô Mai',
  'generalComment': 'Chăm ngoan và tích cực tham gia hoạt động lớp.',
  'conduct': 'Tốt',
  'suggestedConduct': 'Khá',
  'status': 'PUBLISHED',
  'submittedSubjects': 1,
  'totalSubjects': 1,
  'missingSubjects': <String>[],
  'subjectReviews': <Map<String, dynamic>>[
    <String, dynamic>{
      'id': 81,
      'studentId': 9,
      'studentName': 'Nguyễn An',
      'studentCode': 'HS009',
      'subjectName': 'Toán',
      'subjectTeacherName': 'Thầy Nam',
      'status': 'SUBMITTED',
      'comment': 'Có tiến bộ rõ rệt.',
      'strengths': 'Tư duy tốt.',
      'improvements': 'Cần trình bày cẩn thận hơn.',
    },
  ],
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
    return _reportJson;
  }
}

class _PublishedTranscriptApi extends GradebookApiClient {
  _PublishedTranscriptApi()
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
        'subjectName': 'Toán',
        'average': 8.5,
        'complete': true,
        'scores': [
          {
            'name': 'Kiểm tra giữa kỳ',
            'weight': 2,
            'assessmentType': 'SCORE',
            'score': 8.5,
            'comment': null,
          },
        ],
      },
    ],
  };
}

class _AcademicApi extends HomeroomAcademicApiClient {
  _AcademicApi()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  @override
  Future<HomeroomStudentResultDto?> getStudentSemesterResult({
    required String token,
    required int studentId,
    required int semesterId,
  }) async => const HomeroomStudentResultDto(
    id: 1,
    studentId: 9,
    studentName: 'Nguyễn An',
    semesterId: 3,
    semesterName: 'Học kỳ I',
    classId: 12,
    className: '12A1',
    gpa: 8.5,
    rank: 2,
    honor: 'Học sinh giỏi',
    conduct: 'Tốt',
    academicAbility: 'Giỏi',
  );
}

class _FakePeriodicReviewApi implements PeriodicReviewApi {
  int? requestedStudentId;

  @override
  Future<StudentPeriodicReport> getPublishedReport({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
  }) async {
    requestedStudentId = studentId;
    return StudentPeriodicReport.fromJson(_reportJson);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
