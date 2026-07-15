import 'dart:collection';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/payment_configuration.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_stats_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_tuition_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/tuition_payment_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

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
  final List<String> postCalls = [];

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    calls.add(_Call(path, query));
    return responses.removeFirst();
  }

  @override
  Future<Object?> postData(String path, {String? token, Object? body}) async {
    postCalls.add(path);
    return null;
  }
}

class _FakeDashboardClient extends DashboardApiClient {
  _FakeDashboardClient(this.value)
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final TeacherDashboardStatsDto value;

  @override
  Future<TeacherDashboardStatsDto> getTeacherStats({
    required String token,
    required int academicYearId,
    required int semesterId,
  }) async => value;
}

class _FakeHomeroomAcademicClient extends HomeroomAcademicApiClient {
  _FakeHomeroomAcademicClient()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  @override
  Future<HomeroomClassDetailDto> getClassDetail({
    required String token,
    required int classId,
  }) async => _homeroomClass;

  @override
  Future<HomeroomClassRankingDto> getClassRanking({
    required String token,
    required int classId,
    required int semesterId,
  }) async => _homeroomRanking;

  @override
  Future<HomeroomStudentResultDto?> getStudentSemesterResult({
    required String token,
    required int studentId,
    required int semesterId,
  }) async => null;
}

class _FakeTuitionClient extends TuitionBillApiClient {
  _FakeTuitionClient(this.value)
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final TeacherTuitionSummaryDto value;

  @override
  Future<TeacherTuitionSummaryDto> getTeacherClassSummary({
    required String token,
    required int classId,
    required int semesterId,
  }) async => value;
}

class _FakeStudentTuitionClient extends TuitionBillApiClient {
  _FakeStudentTuitionClient()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  bool requested = false;

  @override
  Future<List<TuitionBill>> getStudentBills({
    required String token,
    required int semesterId,
    int? studentId,
  }) async => [
    TuitionBill(
      id: 41,
      title: 'Học phí học kỳ I',
      amount: 15000000,
      dueDate: '31/12/2026',
      status: requested ? 'Đang xử lý' : 'Chưa đóng',
    ),
  ];

  @override
  Future<PaymentConfiguration?> getPaymentConfiguration({
    required String token,
    required int semesterId,
  }) async => const PaymentConfiguration(
    id: 8,
    academicYearId: 26,
    bankCode: 'TPB',
    bankName: 'TPBank',
    accountNumber: '1234567890',
    accountHolder: 'FPT SCHOOLS',
    branch: 'Hà Nội',
    transferContentTemplate: 'MFS {studentCode} {semester}',
    enabled: true,
    method: 'BANK_TRANSFER',
    displayMode: 'MANUAL',
    qrAvailable: false,
  );

  @override
  Future<void> requestBankTransfer({
    required String token,
    required int billId,
  }) async {
    expect(billId, 41);
    requested = true;
  }
}

const _stats = TeacherDashboardStatsDto(
  classId: 12,
  className: '12A1',
  academicYearId: 26,
  academicYearName: '2026-2027',
  semesterId: 1,
  semesterName: 'Học kỳ I',
  attendanceRate: 92.5,
  averageGpa: 8.1,
  parentReadRate: 75,
);

const _homeroomClass = HomeroomClassDetailDto(
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
  ],
);

const _homeroomRanking = HomeroomClassRankingDto(
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
      gpa: 8.1,
      academicAbility: 'Giỏi',
      conduct: 'Tốt',
    ),
  ],
);

const _tuition = TeacherTuitionSummaryDto(
  classId: 12,
  className: '12A1',
  semesterId: 1,
  semesterName: 'Học kỳ I',
  totalStudents: 3,
  paidStudents: 1,
  outstandingStudents: 1,
  studentsWithoutBills: 1,
  students: [
    TeacherTuitionStudentDto(
      studentId: 1,
      studentName: 'Nguyễn An',
      studentCode: 'HS001',
      paymentState: 'PAID',
      outstandingAmount: 0,
    ),
    TeacherTuitionStudentDto(
      studentId: 2,
      studentName: 'Trần Bình',
      studentCode: 'HS002',
      paymentState: 'UNPAID',
      outstandingAmount: 1500000,
    ),
    TeacherTuitionStudentDto(
      studentId: 3,
      studentName: 'Lê Chi',
      studentCode: 'HS003',
      paymentState: 'NO_BILLS',
      outstandingAmount: 0,
    ),
  ],
);

const _processingTuition = TeacherTuitionSummaryDto(
  classId: 12,
  className: '12A1',
  semesterId: 1,
  semesterName: 'Học kỳ I',
  totalStudents: 1,
  paidStudents: 0,
  outstandingStudents: 1,
  studentsWithoutBills: 0,
  students: [
    TeacherTuitionStudentDto(
      studentId: 4,
      studentName: 'Phạm Dũng',
      studentCode: 'HS004',
      paymentState: 'PROCESSING',
      outstandingAmount: 2500000,
    ),
  ],
);

void main() {
  test('teacher dashboard client maps selected-period backend data', () async {
    final backend = _RecordingBackend([
      <String, dynamic>{
        'classId': 12,
        'className': '12A1',
        'academicYearId': 26,
        'academicYearName': '2026-2027',
        'semesterId': 1,
        'semesterName': 'Học kỳ I',
        'attendanceRate': 92.5,
        'averageGpa': 8.1,
        'parentReadRate': 75,
      },
    ]);

    final result = await DashboardApiClient(backend: backend).getTeacherStats(
      token: 'teacher-token',
      academicYearId: 26,
      semesterId: 1,
    );

    expect(result.className, '12A1');
    expect(result.averageGpa, 8.1);
    expect(backend.calls.single.path, '/api/dashboard/teacher');
    expect(backend.calls.single.query, {
      'academicYearId': '26',
      'semesterId': '1',
    });
  });

  test('teacher tuition client includes students without bills', () async {
    final backend = _RecordingBackend([
      <String, dynamic>{
        'classId': 12,
        'className': '12A1',
        'semesterId': 1,
        'semesterName': 'Học kỳ I',
        'totalStudents': 3,
        'paidStudents': 1,
        'outstandingStudents': 1,
        'studentsWithoutBills': 1,
        'students': [
          <String, dynamic>{
            'studentId': 1,
            'studentName': 'Nguyễn An',
            'studentCode': 'HS001',
            'paymentState': 'PAID',
            'outstandingAmount': 0,
            'bills': <dynamic>[],
          },
          <String, dynamic>{
            'studentId': 2,
            'studentName': 'Trần Bình',
            'studentCode': 'HS002',
            'paymentState': 'UNPAID',
            'outstandingAmount': 1500000,
            'bills': <dynamic>[],
          },
          <String, dynamic>{
            'studentId': 3,
            'studentName': 'Lê Chi',
            'studentCode': 'HS003',
            'paymentState': 'NO_BILLS',
            'outstandingAmount': 0,
            'bills': <dynamic>[],
          },
        ],
      },
    ]);

    final result = await TuitionBillApiClient(backend: backend)
        .getTeacherClassSummary(
          token: 'teacher-token',
          classId: 12,
          semesterId: 1,
        );

    expect(result.students, hasLength(3));
    expect(result.students.last.paymentState, 'NO_BILLS');
    expect(backend.calls.single.path, '/api/tuition/bills/class-summary');
    expect(backend.calls.single.query, {'classId': '12', 'semesterId': '1'});
  });

  test('teacher tuition mapping rejects missing payment state', () {
    expect(
      () => TeacherTuitionStudentDto.fromJson({
        'studentId': 1,
        'studentName': 'Nguyễn An',
        'studentCode': 'HS001',
        'outstandingAmount': 0,
      }),
      throwsA(isA<ParseException>()),
    );
  });

  test('teacher tuition maps processing as an outstanding payment', () {
    final student = TeacherTuitionStudentDto.fromJson({
      'studentId': 4,
      'studentName': 'Phạm Dũng',
      'studentCode': 'HS004',
      'paymentState': 'PROCESSING',
      'outstandingAmount': 2500000,
    });

    expect(student.paymentState, 'PROCESSING');
    expect(student.isPaid, isFalse);
    expect(student.hasOutstanding, isTrue);
  });

  test('student tuition maps canonical id and posts payment request', () async {
    final backend = _RecordingBackend([
      [
        {
          'id': 41,
          'name': 'Học phí học kỳ I',
          'amount': 15000000,
          'dueDate': '2026-12-31',
          'status': 'UNPAID',
        },
      ],
    ]);
    final client = TuitionBillApiClient(backend: backend);

    final bills = await client.getStudentBills(
      token: 'student-token',
      semesterId: 1,
    );
    await client.requestBankTransfer(
      token: 'student-token',
      billId: bills.single.id!,
    );

    expect(bills.single.id, 41);
    expect(bills.single.status, 'Chưa đóng');
    expect(backend.postCalls, ['/api/tuition/bills/41/payment-request']);
  });

  test('student tuition maps manual bank transfer configuration', () async {
    final backend = _RecordingBackend([
      {
        'id': 8,
        'academicYearId': 26,
        'bankCode': 'TPB',
        'bankName': 'TPBank',
        'accountNumber': '1234567890',
        'accountHolder': 'FPT SCHOOLS',
        'branch': 'Hà Nội',
        'transferContentTemplate': 'MFS {studentCode} {semester}',
        'enabled': true,
        'method': 'BANK_TRANSFER',
        'displayMode': 'MANUAL',
        'qrAvailable': false,
      },
    ]);

    final configuration = await TuitionBillApiClient(
      backend: backend,
    ).getPaymentConfiguration(token: 'student-token', semesterId: 1);

    expect(
      backend.calls.single.path,
      '/api/payment-configurations/semesters/1',
    );
    expect(configuration?.accountNumber, '1234567890');
    expect(configuration?.qrAvailable, isFalse);
    expect(
      configuration?.renderTransferContent(
        studentCode: 'HS009',
        academicYear: '2026-2027',
        semester: 'Học kỳ I',
      ),
      'MFS HS009 Học kỳ I',
    );
  });

  test('student tuition rejects unknown backend status', () async {
    final backend = _RecordingBackend([
      [
        {
          'id': 41,
          'name': 'Học phí học kỳ I',
          'amount': 15000000,
          'dueDate': '2026-12-31',
          'status': 'UNKNOWN',
        },
      ],
    ]);

    await expectLater(
      TuitionBillApiClient(
        backend: backend,
      ).getStudentBills(token: 'student-token', semesterId: 1),
      throwsA(isA<ParseException>()),
    );
  });

  testWidgets('teacher stats resolves an empty academic period', (
    tester,
  ) async {
    final controller = AcademicPeriodController(token: 'teacher-token')
      ..isLoading = false;

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: controller,
          child: TeacherStatsScreen(
            token: 'teacher-token',
            apiClient: _FakeDashboardClient(_stats),
            academicApiClient: _FakeHomeroomAcademicClient(),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(find.text('Chưa có kỳ học để tải hồ sơ lớp.'), findsOneWidget);
  });

  testWidgets(
    'teacher stats screen renders backend values for selected period',
    (tester) async {
      final controller = AcademicPeriodController(token: 'teacher-token');
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
      controller.periods = [period];
      controller.selected = period;
      controller.isLoading = false;

      await tester.pumpWidget(
        MaterialApp(
          home: AcademicPeriodScope(
            controller: controller,
            child: TeacherStatsScreen(
              token: 'teacher-token',
              apiClient: _FakeDashboardClient(_stats),
              academicApiClient: _FakeHomeroomAcademicClient(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Tổng quan lớp 12A1'), findsOneWidget);
      expect(find.text('92.5%'), findsOneWidget);
      expect(find.text('8.1 / 10'), findsOneWidget);
      expect(find.text('75.0%'), findsOneWidget);
    },
  );

  testWidgets(
    'teacher tuition screen renders canonical summary without fake reminder',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: TeacherTuitionScreen(
            token: 'teacher-token',
            classId: 12,
            semesterId: 1,
            apiClient: _FakeTuitionClient(_tuition),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Lớp 12A1 · Học kỳ I'), findsOneWidget);
      expect(find.textContaining('Đã hoàn tất 1/3'), findsOneWidget);
      expect(find.text('Nguyễn An'), findsOneWidget);
      expect(find.text('Trần Bình'), findsOneWidget);
      expect(find.byIcon(Icons.notifications_active), findsNothing);
    },
  );

  testWidgets(
    'teacher tuition keeps processing students in outstanding filter',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: TeacherTuitionScreen(
            token: 'teacher-token',
            classId: 12,
            semesterId: 1,
            apiClient: _FakeTuitionClient(_processingTuition),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Phạm Dũng'), findsOneWidget);
      expect(find.text('Đang xử lý'), findsOneWidget);

      await tester.tap(find.widgetWithText(ChoiceChip, 'Còn phải thu'));
      await tester.pump();

      expect(find.text('Phạm Dũng'), findsOneWidget);
      expect(find.text('Đang xử lý'), findsOneWidget);
    },
  );

  testWidgets('student tuition uses backend confirmation on narrow screen', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(320, 568));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final api = _FakeStudentTuitionClient();
    final period = AcademicPeriod(
      academicYearId: 26,
      academicYearName: '2026-2027',
      semesterId: 1,
      semesterName: 'Học kỳ I',
      startDate: DateTime(2026, 7, 1),
      endDate: DateTime(2026, 12, 31),
      isCurrent: true,
    );
    final controller = AcademicPeriodController(token: 'student-token')
      ..periods = [period]
      ..selected = period
      ..isLoading = false;
    addTearDown(controller.dispose);
    final student = StudentSnapshot.linked(
      id: 9,
      name: 'Học sinh Test',
      studentCode: 'HS009',
      className: '12A1',
      school: 'FPT Schools',
      linkStatus: 'ACTIVE',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: controller,
          child: TuitionPaymentScreen(
            student: student,
            token: 'student-token',
            viewAsStudent: true,
            apiClient: api,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await tester.scrollUntilVisible(
      find.text('TPBank'),
      180,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('TPBank'), findsOneWidget);
    expect(find.text('1234567890'), findsOneWidget);
    expect(find.text('MFS HS009 Học kỳ I'), findsOneWidget);
    expect(find.textContaining('Chưa sử dụng QR'), findsOneWidget);

    await tester.tap(find.text('Xác nhận đã chuyển'));
    await tester.pumpAndSettle();
    expect(find.textContaining('không tự chuyển tiền'), findsOneWidget);
    expect(find.textContaining('VietQR'), findsNothing);

    await tester.tap(find.text('Gửi xác nhận'));
    await tester.pumpAndSettle();
    expect(api.requested, isTrue);
    expect(find.text('Đang xử lý'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
