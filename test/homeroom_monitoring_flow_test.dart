import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_engagement_screen.dart';

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

  test('public event parser keeps published reward metadata', () {
    final event = StudentEvent.fromJson(_eventJson);
    expect(event.eventType, 'REWARD');
    expect(event.status, 'PUBLISHED');
    expect(event.studentId, 9);
  });

  for (final width in [320.0, 360.0, 390.0, 430.0]) {
    testWidgets('parent engagement has no overflow at ${width.toInt()} px', (
      tester,
    ) async {
      tester.view.devicePixelRatio = 1;
      tester.view.physicalSize = Size(width, 760);
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      final authService = AuthService(apiClient: _FakeAuthApiClient());
      await authService.login('0900000000', 'password');
      addTearDown(authService.dispose);
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
            child: StudentEngagementScreen(
              authService: authService,
              api: _FakeMonitoringApi(),
              studentId: 9,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('parent-meeting-list')), findsOneWidget);
      expect(find.text('Họp trao đổi học kỳ'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });
  }
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
  'rewardCount': 3,
  'violationCount': 1,
};

const _eventJson = <String, dynamic>{
  'id': 41,
  'studentId': 9,
  'eventType': 'REWARD',
  'category': 'Học tập',
  'title': 'Tiến bộ nổi bật',
  'description': 'Có nhiều cố gắng trong tháng.',
  'eventDate': '2026-09-15',
  'status': 'PUBLISHED',
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

class _FakeAuthApiClient extends AuthApiClient {
  _FakeAuthApiClient()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  @override
  Future<AuthSessionDto> login({
    required String phone,
    required String password,
  }) async => const AuthSessionDto(
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
  );
}

class _FakeMonitoringApi implements HomeroomMonitoringApi {
  @override
  Future<List<ParentMeeting>> getMeetings({
    required String token,
    required int academicYearId,
    required int semesterId,
    int? classId,
  }) async => [
    ParentMeeting(
      id: 31,
      title: 'Họp trao đổi học kỳ',
      classId: 12,
      className: '12A1',
      studentId: 9,
      studentName: 'Nguyễn An',
      startsAt: DateTime(2026, 10, 10, 18, 30),
      location: 'Phòng 203',
      agenda: 'Trao đổi tình hình học tập.',
      status: 'SCHEDULED',
      participants: const [
        ParentMeetingParticipant(
          guardianId: 7,
          guardianName: 'Phụ huynh An',
          response: 'PENDING',
          attendance: 'UNKNOWN',
        ),
      ],
    ),
  ];

  @override
  Future<List<StudentEvent>> getStudentEvents({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    int? classId,
  }) async => [StudentEvent.fromJson(_eventJson)];

  @override
  Future<ParentMeeting> respondMeeting({
    required String token,
    required int meetingId,
    required String response,
  }) async => (await getMeetings(
    token: token,
    academicYearId: 26,
    semesterId: 3,
  )).single;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
