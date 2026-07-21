import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/home_screen_hocsinh.dart';

class _HomeBackend extends BackendApiClient {
  _HomeBackend() : super(baseUrl: 'http://localhost');

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
              'startDate': '2026-08-01',
              'endDate': '2026-12-31',
              'isCurrent': true,
              'status': 'ACTIVE',
            },
          ],
        },
      ];
    }
    if (path == '/api/dashboard/student') {
      expect(query['academicYearId'], '7');
      expect(query['semesterId'], '71');
      return {
        'studentId': 99,
        'studentName': 'Nguyễn Backend',
        'studentCode': 'HS0099',
        'classId': 5,
        'className': '10A1',
        'schoolName': 'FPT Schools Cầu Giấy',
        'academicYearId': 7,
        'academicYearName': '2026-2027',
        'semesterId': 71,
        'semesterName': 'Học kỳ 1',
        'attendanceRate': 96.5,
        'presentSessions': 38,
        'absentSessions': 2,
        'currentGpa': 8.25,
        'academicAbility': 'Tốt',
        'conduct': 'Tốt',
        'classRank': 3,
        'homeroomTeacherName': 'Cô Nguyễn Thu Hà',
        'homeroomTeacherPhone': '0901234567',
      };
    }
    throw StateError('Unexpected path: $path');
  }
}

class _AuthApi extends AuthApiClient {
  _AuthApi() : super(backend: _HomeBackend());

  @override
  Future<AuthSessionDto> login({
    required String phone,
    required String password,
  }) async {
    return const AuthSessionDto(
      token: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 1,
      userName: 'Tên cũ trong phiên',
      role: 'STUDENT',
      phone: '0900000000',
      status: 'ACTIVE',
      accountCode: 'OLD-CODE',
    );
  }
}

void main() {
  testWidgets('thẻ học sinh hiển thị dữ liệu backend của kỳ đang chọn', (
    tester,
  ) async {
    final backend = _HomeBackend();
    final authService = AuthService(apiClient: _AuthApi());
    await authService.login('0900000000', 'password');
    final periodController = AcademicPeriodController(
      token: 'token',
      backend: backend,
    );
    await periodController.load();

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periodController,
          child: HomeStudent(authService: authService, backend: backend),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Nguyễn Backend'), findsOneWidget);
    expect(find.textContaining('Lớp 10A1'), findsOneWidget);
    expect(find.textContaining('Mã HS: HS0099'), findsOneWidget);
    expect(find.textContaining('FPT Schools Cầu Giấy'), findsOneWidget);
    expect(find.textContaining('2026-2027 · Học kỳ 1'), findsOneWidget);
    expect(find.text('8.25'), findsOneWidget);
    expect(find.text('96.5%'), findsOneWidget);
    expect(find.text('3'), findsOneWidget);
    expect(find.textContaining('Cô Nguyễn Thu Hà'), findsOneWidget);
    expect(find.text('Học phí'), findsNothing);
  });
}
