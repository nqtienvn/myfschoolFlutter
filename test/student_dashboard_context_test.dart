import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/auth_session.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/home_screen_phuhuynh.dart';

Map<String, dynamic> _dashboard({
  int studentId = 99,
  String studentName = 'Nguyễn Backend',
  required int semesterId,
  required String semesterName,
  required String className,
  required String schoolName,
  required double gpa,
}) {
  return {
    'studentId': studentId,
    'studentName': studentName,
    'studentCode': 'HS0099',
    'classId': semesterId,
    'className': className,
    'schoolName': schoolName,
    'academicYearId': 7,
    'academicYearName': '2026-2027',
    'semesterId': semesterId,
    'semesterName': semesterName,
    'attendanceRate': 95.5,
    'presentSessions': 41,
    'absentSessions': 2,
    'currentGpa': gpa,
    'academicAbility': 'Tốt',
    'conduct': 'Tốt',
    'classRank': 4,
    'homeroomTeacherName': 'Cô Hà Backend',
    'homeroomTeacherPhone': '0901234567',
  };
}

class _PeriodBackend extends BackendApiClient {
  _PeriodBackend() : super(baseUrl: 'http://localhost');

  final firstDashboard = Completer<Object?>();
  final calls = <Map<String, String?>>[];

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    if (path == '/api/academic-years/available') {
      return [
        {
          'id': 7,
          'name': '2026-2027',
          'status': 'ACTIVE',
          'semesters': [
            {
              'id': 71,
              'name': 'Học kỳ 1',
              'startDate': '2026-07-01',
              'endDate': '2026-12-31',
              'isCurrent': true,
              'status': 'ACTIVE',
            },
            {
              'id': 72,
              'name': 'Học kỳ 2',
              'startDate': '2027-01-01',
              'endDate': '2027-05-31',
              'isCurrent': false,
              'status': 'COMPLETED',
            },
          ],
        },
      ];
    }
    if (path == '/api/dashboard/student') {
      calls.add(query);
      if (query['semesterId'] == '71') return firstDashboard.future;
      return _dashboard(
        semesterId: 72,
        semesterName: 'Học kỳ 2',
        className: '10B2',
        schoolName: 'FPT Schools Backend',
        gpa: 8.1,
      );
    }
    if (path == '/api/tuition/bills/student') return <Object>[];
    throw StateError('Unexpected path: $path');
  }
}

class _ParentAuthApi extends AuthApiClient {
  _ParentAuthApi(BackendApiClient backend) : super(backend: backend);

  @override
  Future<AuthSessionDto> login({
    required String phone,
    required String password,
  }) async {
    return const AuthSessionDto(
      token: 'parent-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 1,
      userName: 'Phụ huynh',
      role: 'PARENT',
      phone: '0900000000',
      status: 'ACTIVE',
      children: [
        LinkedStudent(
          id: 99,
          name: 'Tên đăng nhập cũ',
          studentCode: 'AUTH-OLD',
          status: 'ACTIVE',
          className: 'AUTH-CLASS',
          schoolName: 'AUTH-SCHOOL',
        ),
        LinkedStudent(
          id: 100,
          name: 'Học sinh B',
          studentCode: 'AUTH-B',
          status: 'ACTIVE',
          className: 'AUTH-B-CLASS',
          schoolName: 'AUTH-B-SCHOOL',
        ),
      ],
    );
  }
}

class _ChildSwitchBackend extends BackendApiClient {
  _ChildSwitchBackend() : super(baseUrl: 'http://localhost');

  final dashboardCalls = <int>[];
  final dashboardResponses = <Completer<Object?>>[];

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) {
    if (path == '/api/tuition/bills/student') return Future.value(<Object>[]);
    if (path == '/api/dashboard/student') {
      dashboardCalls.add(int.parse(query['studentId']!));
      final response = Completer<Object?>();
      dashboardResponses.add(response);
      return response.future;
    }
    throw StateError('Unexpected path: $path');
  }
}

void main() {
  test(
    'student dashboard client sends parent child and selected period',
    () async {
      final backend = _ImmediateDashboardBackend();
      final stats = await DashboardApiClient(backend: backend).getStudentStats(
        token: 'parent-token',
        studentId: 99,
        academicYearId: 7,
        semesterId: 72,
      );

      expect(backend.query, {
        'studentId': '99',
        'academicYearId': '7',
        'semesterId': '72',
      });
      expect(stats.className, '10B2');
      expect(stats.currentGpa, 8.1);
      expect(stats.attendanceRate, 95.5);
      expect(stats.homeroomTeacherName, 'Cô Hà Backend');
    },
  );

  testWidgets(
    'parent home uses selected-period dashboard and ignores stale response',
    (tester) async {
      final backend = _PeriodBackend();
      final authService = AuthService(apiClient: _ParentAuthApi(backend));
      await authService.login('0900000000', 'password');
      final controller = AcademicPeriodController(
        token: 'parent-token',
        studentId: 99,
        backend: backend,
      );
      await controller.load();

      await tester.pumpWidget(
        MaterialApp(
          home: AcademicPeriodScope(
            controller: controller,
            child: HomeParent(authService: authService, backend: backend),
          ),
        ),
      );
      await tester.pump();

      controller.select(controller.periods.last);
      await tester.pump();
      await tester.pump();

      expect(find.textContaining('Lớp 10B2'), findsOneWidget);
      expect(find.textContaining('FPT Schools Backend'), findsOneWidget);
      expect(find.text('8.10'), findsOneWidget);
      expect(find.text('95.5%'), findsOneWidget);
      expect(find.textContaining('Cô Hà Backend'), findsOneWidget);
      expect(find.textContaining('AUTH-CLASS'), findsNothing);

      backend.firstDashboard.complete(
        _dashboard(
          semesterId: 71,
          semesterName: 'Học kỳ 1',
          className: '10A1',
          schoolName: 'Dữ liệu cũ',
          gpa: 6.0,
        ),
      );
      await tester.pump();

      expect(find.textContaining('Lớp 10B2'), findsOneWidget);
      expect(find.textContaining('Dữ liệu cũ'), findsNothing);
      expect(backend.calls.first['studentId'], '99');
      expect(backend.calls.last['semesterId'], '72');
    },
  );

  testWidgets('parent child switch A-B-A reloads and keeps latest A', (
    tester,
  ) async {
    final backend = _ChildSwitchBackend();
    final authService = AuthService(apiClient: _ParentAuthApi(backend));
    await authService.login('0900000000', 'password');
    final period = AcademicPeriod(
      academicYearId: 7,
      academicYearName: '2026-2027',
      semesterId: 71,
      semesterName: 'Học kỳ 1',
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
          child: HomeParent(authService: authService, backend: backend),
        ),
      ),
    );
    await tester.pump();
    authService.selectChild(1);
    await tester.pump();
    authService.selectChild(0);
    await tester.pump();
    expect(backend.dashboardCalls, [99, 100, 99]);

    backend.dashboardResponses[2].complete(
      _dashboard(
        semesterId: 71,
        semesterName: 'Học kỳ 1',
        className: '12A mới',
        schoolName: 'Dữ liệu A mới nhất',
        gpa: 9.2,
      ),
    );
    await tester.pump();
    expect(find.textContaining('12A mới'), findsOneWidget);
    expect(find.text('9.20'), findsOneWidget);

    backend.dashboardResponses[1].complete(
      _dashboard(
        studentId: 100,
        studentName: 'Học sinh B',
        semesterId: 71,
        semesterName: 'Học kỳ 1',
        className: '11B cũ',
        schoolName: 'Dữ liệu B cũ',
        gpa: 7.0,
      ),
    );
    backend.dashboardResponses[0].complete(
      _dashboard(
        semesterId: 71,
        semesterName: 'Học kỳ 1',
        className: '12A rất cũ',
        schoolName: 'Dữ liệu A cũ',
        gpa: 6.0,
      ),
    );
    await tester.pump();

    expect(find.textContaining('12A mới'), findsOneWidget);
    expect(find.textContaining('Dữ liệu B cũ'), findsNothing);
    expect(find.textContaining('Dữ liệu A cũ'), findsNothing);
  });
}

class _ImmediateDashboardBackend extends BackendApiClient {
  _ImmediateDashboardBackend() : super(baseUrl: 'http://localhost');

  Map<String, String?>? query;

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    this.query = query;
    return _dashboard(
      semesterId: 72,
      semesterName: 'Học kỳ 2',
      className: '10B2',
      schoolName: 'FPT Schools Backend',
      gpa: 8.1,
    );
  }
}
