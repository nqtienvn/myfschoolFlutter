import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/home_screen_giaovien.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_attendance_screen.dart';

class _TeacherBackend extends BackendApiClient {
  _TeacherBackend() : super(baseUrl: 'http://localhost');

  final List<Map<String, String?>> attendanceQueries = [];

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    switch (path) {
      case '/api/academic-years/available':
        return [
          {
            'id': 26,
            'name': '2026-2027',
            'status': 'ACTIVE',
            'semesters': [
              {
                'id': 1,
                'name': 'Học kỳ I',
                'startDate': '2026-07-01',
                'endDate': '2026-12-31',
                'isCurrent': true,
                'status': 'ACTIVE',
              },
            ],
          },
        ];
      case '/api/user/profile':
        return {
          'name': 'Nguyễn Thu Hà',
          'teacherProfile': {'employeeCode': 'GV-DEMO-01'},
        };
      case '/api/dashboard/teacher':
        expect(query, {'academicYearId': '26', 'semesterId': '1'});
        return {
          'classId': 12,
          'className': '12A1',
          'academicYearId': 26,
          'academicYearName': '2026-2027',
          'semesterId': 1,
          'semesterName': 'Học kỳ I',
          'attendanceRate': 92.5,
          'averageGpa': 8.1,
          'parentReadRate': 75,
        };
      case '/api/leave-requests/pending-count':
        expect(query, {'academicYearId': '26', 'semesterId': '1'});
        return 2;
      case '/api/tuition/bills/class-summary':
        expect(query, {'classId': '12', 'semesterId': '1'});
        return {
          'classId': 12,
          'className': '12A1',
          'semesterId': 1,
          'semesterName': 'Học kỳ I',
          'totalStudents': 2,
          'paidStudents': 1,
          'outstandingStudents': 1,
          'studentsWithoutBills': 0,
          'students': [
            {
              'studentId': 1,
              'studentName': 'Nguyễn An',
              'studentCode': 'HS001',
              'paymentState': 'PAID',
              'outstandingAmount': 0,
              'bills': <Object>[],
            },
            {
              'studentId': 2,
              'studentName': 'Trần Bình',
              'studentCode': 'HS002',
              'paymentState': 'UNPAID',
              'outstandingAmount': 1500000,
              'bills': <Object>[],
            },
          ],
        };
      default:
        throw StateError('Unexpected path: $path');
    }
  }
}

class _FailingTeacherBackend extends _TeacherBackend {
  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) {
    if (path == '/api/dashboard/teacher') {
      throw const BackendApiException('Service unavailable', statusCode: 503);
    }
    return super.getData(path, token: token, query: query);
  }
}

class _DelayedTeacherHomeBackend extends BackendApiClient {
  _DelayedTeacherHomeBackend() : super(baseUrl: 'http://localhost');

  final dashboardResponses = <Completer<Object?>>[];
  final dashboardSemesterIds = <String?>[];

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) {
    if (path == '/api/user/profile') {
      return Future.value({
        'name': 'Nguyễn Thu Hà',
        'teacherProfile': {'employeeCode': 'GV-DEMO-01'},
      });
    }
    if (path == '/api/dashboard/teacher') {
      dashboardSemesterIds.add(query['semesterId']);
      final response = Completer<Object?>();
      dashboardResponses.add(response);
      return response.future;
    }
    if (path == '/api/leave-requests/pending-count') return Future.value(1);
    if (path == '/api/tuition/bills/class-summary') {
      final classId = int.parse(query['classId']!);
      final semesterId = int.parse(query['semesterId']!);
      return Future.value({
        'classId': classId,
        'className': 'Lớp $classId',
        'semesterId': semesterId,
        'semesterName': 'Học kỳ $semesterId',
        'totalStudents': 0,
        'paidStudents': 0,
        'outstandingStudents': 0,
        'studentsWithoutBills': 0,
        'students': <Object>[],
      });
    }
    throw StateError('Unexpected path: $path');
  }
}

Map<String, dynamic> _teacherDashboard({
  required int semesterId,
  required int classId,
  required String className,
}) => {
  'classId': classId,
  'className': className,
  'academicYearId': 26,
  'academicYearName': '2026-2027',
  'semesterId': semesterId,
  'semesterName': 'Học kỳ $semesterId',
  'attendanceRate': 90,
  'averageGpa': 8,
  'parentReadRate': 75,
};

class _TeacherAuthApi extends AuthApiClient {
  _TeacherAuthApi(BackendApiClient backend) : super(backend: backend);

  @override
  Future<AuthSessionDto> login({
    required String phone,
    required String password,
  }) async {
    return const AuthSessionDto(
      token: 'teacher-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 10,
      userName: 'Nguyễn Thu Hà',
      role: 'TEACHER',
      phone: '0901000001',
      status: 'ACTIVE',
      accountCode: 'GV-DEMO-01',
    );
  }
}

class _AttendanceApi extends AttendanceApiClient {
  _AttendanceApi()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final List<String?> contextDates = [];
  final List<String> dailyDates = [];

  @override
  Future<List<AttendanceCorrectionDto>> getCorrectionHistory({
    required String token,
    required int academicYearId,
  }) async => const [];

  @override
  Future<Map<String, dynamic>> getHomeroomContext({
    required String token,
    String? date,
  }) async {
    contextDates.add(date);
    return {
      'classId': 12,
      'className': '12A1',
      'academicYearId': 25,
      'academicYearName': '2025-2026',
      'shifts': ['MORNING'],
    };
  }

  @override
  Future<Map<String, dynamic>> getDailyAttendance({
    required String token,
    required int classId,
    required String date,
    required String shift,
  }) async {
    dailyDates.add(date);
    return {
      'students': <Object>[],
      'submitted': false,
      'canEdit': false,
      'scheduledPeriods': 1,
      'correctionPending': false,
    };
  }
}

class _DelayedAttendanceApi extends AttendanceApiClient {
  _DelayedAttendanceApi()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final firstRefresh = Completer<Map<String, dynamic>>();
  final secondRefresh = Completer<Map<String, dynamic>>();
  int dailyCalls = 0;

  @override
  Future<Map<String, dynamic>> getHomeroomContext({
    required String token,
    String? date,
  }) async => {
    'classId': 12,
    'className': '12A1',
    'academicYearId': 26,
    'shifts': ['MORNING'],
  };

  @override
  Future<List<AttendanceCorrectionDto>> getCorrectionHistory({
    required String token,
    required int academicYearId,
  }) async => const [];

  @override
  Future<Map<String, dynamic>> getDailyAttendance({
    required String token,
    required int classId,
    required String date,
    required String shift,
  }) {
    dailyCalls++;
    if (dailyCalls == 1) {
      return Future.value(_dailyResult('Ban đầu'));
    }
    if (dailyCalls == 2) return firstRefresh.future;
    return secondRefresh.future;
  }

  Map<String, dynamic> _dailyResult(String studentName) => {
    'students': [
      {
        'studentId': 1,
        'studentName': studentName,
        'studentCode': 'HS001',
        'status': 'PRESENT',
        'hasApprovedLeave': false,
      },
    ],
    'submitted': false,
    'canEdit': true,
    'scheduledPeriods': 1,
    'correctionPending': false,
  };
}

class _SubmittedAttendanceApi extends _AttendanceApi {
  String? correctionReason;

  @override
  Future<Map<String, dynamic>> getDailyAttendance({
    required String token,
    required int classId,
    required String date,
    required String shift,
  }) async {
    dailyDates.add(date);
    return {
      'students': [
        {
          'studentId': 1,
          'studentName': 'Nguyễn An',
          'studentCode': 'HS001',
          'status': 'PRESENT',
          'hasApprovedLeave': false,
        },
      ],
      'submitted': true,
      'canEdit': true,
      'scheduledPeriods': 1,
      'correctionPending': correctionReason != null,
    };
  }

  @override
  Future<void> requestAttendanceCorrection({
    required String token,
    required int classId,
    required String date,
    required String shift,
    required List<Map<String, dynamic>> entries,
    required String reason,
  }) async {
    correctionReason = reason;
  }
}

void main() {
  testWidgets('teacher home uses backend class without tuition management', (
    tester,
  ) async {
    final backend = _TeacherBackend();
    final auth = AuthService(apiClient: _TeacherAuthApi(backend));
    await auth.login('0901000001', 'Demo@123');
    final periods = AcademicPeriodController(
      token: 'teacher-token',
      backend: backend,
    );
    await periods.load();

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periods,
          child: HomeTeacher(authService: auth, backend: backend),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('GVCN 12A1'), findsOneWidget);
    expect(find.textContaining('GV-DEMO-01'), findsOneWidget);
    expect(find.text('QL Học phí'), findsNothing);
  });

  testWidgets('teacher attendance uses the selected period reference date', (
    tester,
  ) async {
    final api = _AttendanceApi();
    await tester.pumpWidget(
      MaterialApp(
        home: TeacherAttendanceScreen(
          token: 'teacher-token',
          date: DateTime(2025, 9, 8),
          apiClient: api,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(api.contextDates, ['2025-09-08']);
    expect(api.dailyDates, ['2025-09-08']);
    expect(find.textContaining('08/09/2025'), findsOneWidget);
  });

  testWidgets('teacher must explain an attendance correction', (tester) async {
    final api = _SubmittedAttendanceApi();
    await tester.pumpWidget(
      MaterialApp(
        home: TeacherAttendanceScreen(
          token: 'teacher-token',
          date: DateTime(2025, 9, 8),
          apiClient: api,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Sửa điểm danh'));
    await tester.pump();
    await tester.ensureVisible(find.text('Lưu thay đổi điểm danh'));
    await tester.tap(find.text('Lưu thay đổi điểm danh'));
    await tester.pumpAndSettle();

    expect(find.text('Lý do sửa điểm danh'), findsOneWidget);
    await tester.tap(find.text('Gửi yêu cầu'));
    await tester.pump();
    expect(
      find.text('Vui lòng nhập lý do để Admin có căn cứ duyệt.'),
      findsOneWidget,
    );
    await tester.enterText(find.byType(TextFormField), 'Điểm danh nhầm');
    await tester.tap(find.text('Gửi yêu cầu'));
    await tester.pumpAndSettle();

    expect(api.correctionReason, 'Điểm danh nhầm');
  });

  testWidgets('teacher home distinguishes a backend error from no homeroom', (
    tester,
  ) async {
    final backend = _FailingTeacherBackend();
    final auth = AuthService(apiClient: _TeacherAuthApi(backend));
    await auth.login('0901000001', 'Demo@123');
    final periods = AcademicPeriodController(
      token: 'teacher-token',
      backend: backend,
    );
    await periods.load();

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periods,
          child: HomeTeacher(authService: auth, backend: backend),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.text('Không thể đồng bộ dữ liệu lớp. Vui lòng thử lại.'),
      findsOneWidget,
    );
    expect(find.textContaining('Chưa xếp lớp'), findsNothing);
  });

  testWidgets('teacher attendance ignores an older refresh response', (
    tester,
  ) async {
    final api = _DelayedAttendanceApi();
    await tester.pumpWidget(
      MaterialApp(
        home: TeacherAttendanceScreen(
          token: 'teacher-token',
          date: DateTime(2026, 7, 15),
          apiClient: api,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.ensureVisible(find.text('Tải lại'));
    await tester.tap(find.text('Tải lại'));
    await tester.pump();
    await tester.tap(find.text('Tải lại'));
    await tester.pump();

    api.secondRefresh.complete(api._dailyResult('Dữ liệu mới'));
    await tester.pump();
    expect(find.text('Dữ liệu mới'), findsOneWidget);

    api.firstRefresh.complete(api._dailyResult('Dữ liệu cũ'));
    await tester.pump();
    expect(find.text('Dữ liệu mới'), findsOneWidget);
    expect(find.text('Dữ liệu cũ'), findsNothing);
  });

  testWidgets('teacher period switch A-B-A keeps latest dashboard and badges', (
    tester,
  ) async {
    final backend = _DelayedTeacherHomeBackend();
    final auth = AuthService(apiClient: _TeacherAuthApi(backend));
    await auth.login('0901000001', 'Demo@123');
    final first = AcademicPeriod(
      academicYearId: 26,
      academicYearName: '2026-2027',
      semesterId: 1,
      semesterName: 'Học kỳ I',
      startDate: DateTime(2026, 7, 1),
      endDate: DateTime(2026, 12, 31),
    );
    final second = AcademicPeriod(
      academicYearId: 26,
      academicYearName: '2026-2027',
      semesterId: 2,
      semesterName: 'Học kỳ II',
      startDate: DateTime(2027, 1, 1),
      endDate: DateTime(2027, 5, 31),
    );
    final periods = AcademicPeriodController(token: 'teacher-token')
      ..periods = [first, second]
      ..selected = first
      ..isLoading = false;
    addTearDown(periods.dispose);

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periods,
          child: HomeTeacher(authService: auth, backend: backend),
        ),
      ),
    );
    await tester.pump();
    periods.select(second);
    await tester.pump();
    periods.select(first);
    await tester.pump();
    expect(backend.dashboardSemesterIds, ['1', '2', '1']);

    backend.dashboardResponses[2].complete(
      _teacherDashboard(semesterId: 1, classId: 31, className: '12A mới'),
    );
    await tester.pumpAndSettle();
    expect(find.text('GVCN 12A mới'), findsOneWidget);

    backend.dashboardResponses[1].complete(
      _teacherDashboard(semesterId: 2, classId: 22, className: '11B cũ'),
    );
    backend.dashboardResponses[0].complete(
      _teacherDashboard(semesterId: 1, classId: 11, className: '12A cũ'),
    );
    await tester.pump();

    expect(find.text('GVCN 12A mới'), findsOneWidget);
    expect(find.text('GVCN 11B cũ'), findsNothing);
    expect(find.text('GVCN 12A cũ'), findsNothing);
  });
}
