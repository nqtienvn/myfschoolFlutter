import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/periodic_reviews_screen.dart';

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

  testWidgets('parent sees the published homeroom and subject reviews', (
    tester,
  ) async {
    final authService = AuthService(
      apiClient: _FakeAuthApiClient(
        const AuthSessionDto(
          token: 'parent-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userId: 5,
          userName: 'Phụ huynh An',
          role: 'PARENT',
          phone: '0900000000',
          status: 'ACTIVE',
          children: [
            LinkedStudent(
              id: 9,
              name: 'Nguyễn An',
              studentCode: 'HS009',
              status: 'ACTIVE',
              className: '12A1',
              classId: 12,
            ),
          ],
        ),
      ),
    );
    await authService.login('0900000000', 'password');
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
    addTearDown(authService.dispose);

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: controller,
          child: PeriodicReviewsScreen(authService: authService, api: api),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(api.requestedStudentId, 9);
    expect(find.text('Nhận xét Học kỳ I'), findsOneWidget);
    expect(find.text('Nguyễn An'), findsOneWidget);
    expect(find.textContaining('Hạnh kiểm: Tốt'), findsOneWidget);
    expect(find.text('Nhận xét của GVCN'), findsOneWidget);
    expect(find.text('Toán'), findsOneWidget);
    expect(find.text('Có tiến bộ rõ rệt.'), findsOneWidget);
  });
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

class _FakeAuthApiClient extends AuthApiClient {
  _FakeAuthApiClient(this.session)
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final AuthSessionDto session;

  @override
  Future<AuthSessionDto> login({
    required String phone,
    required String password,
  }) async => session;
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
