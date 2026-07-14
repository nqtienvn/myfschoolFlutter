import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';

class _FakeBackendApiClient extends BackendApiClient {
  _FakeBackendApiClient(this.response) : super(baseUrl: 'http://localhost');

  final Object? response;

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    expect(path, '/api/academic-years/available');
    return response;
  }
}

class _DelayedPeriodBackend extends BackendApiClient {
  _DelayedPeriodBackend() : super(baseUrl: 'http://localhost');

  final calls = <int?>[];
  final responses = <Completer<Object?>>[];

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) {
    calls.add(int.tryParse(query['studentId'] ?? ''));
    final response = Completer<Object?>();
    responses.add(response);
    return response.future;
  }
}

Object _periodResponse(int semesterId) => [
  {
    'id': 2,
    'name': '2026-2027',
    'status': 'ACTIVE',
    'semesters': [
      {
        'id': semesterId,
        'name': 'Học kỳ $semesterId',
        'startDate': '2026-08-01',
        'endDate': '2026-12-31',
        'isCurrent': true,
        'status': 'ACTIVE',
      },
    ],
  },
];

void main() {
  test('mặc định chọn đúng năm ACTIVE và học kỳ current từ admin', () async {
    final controller = AcademicPeriodController(
      token: 'token',
      backend: _FakeBackendApiClient([
        {
          'id': 1,
          'name': '2025-2026',
          'status': 'COMPLETED',
          'semesters': [
            {
              'id': 11,
              'name': 'Học kỳ 2',
              'startDate': '2026-01-01',
              'endDate': '2026-05-31',
              'isCurrent': false,
              'status': 'COMPLETED',
            },
          ],
        },
        {
          'id': 2,
          'name': '2026-2027',
          'status': 'ACTIVE',
          'semesters': [
            {
              'id': 21,
              'name': 'Học kỳ 1',
              'startDate': '2026-08-01',
              'endDate': '2026-12-31',
              'isCurrent': true,
              'status': 'ACTIVE',
            },
          ],
        },
      ]),
    );

    await controller.load();

    expect(controller.periods, hasLength(2));
    expect(controller.selected?.academicYearName, '2026-2027');
    expect(controller.selected?.semesterId, 21);
    expect(controller.selected?.isActive, isTrue);
  });

  test('đổi học kỳ cập nhật lựa chọn dùng chung', () async {
    final controller = AcademicPeriodController(
      token: 'token',
      backend: _FakeBackendApiClient([
        {
          'id': 2,
          'name': '2026-2027',
          'status': 'ACTIVE',
          'semesters': [
            {
              'id': 21,
              'name': 'Học kỳ 1',
              'startDate': '2026-08-01',
              'endDate': '2026-12-31',
              'isCurrent': true,
              'status': 'ACTIVE',
            },
            {
              'id': 22,
              'name': 'Học kỳ 2',
              'startDate': '2027-01-01',
              'endDate': '2027-05-31',
              'isCurrent': false,
              'status': 'COMPLETED',
            },
          ],
        },
      ]),
    );
    await controller.load();
    var notifications = 0;
    controller.addListener(() => notifications++);

    controller.select(controller.periods.last);

    expect(controller.selected?.semesterId, 22);
    expect(notifications, 1);
  });

  test('đổi học sinh A-B-A bỏ qua cả hai phản hồi cũ', () async {
    final backend = _DelayedPeriodBackend();
    final controller = AcademicPeriodController(
      token: 'token',
      studentId: 1,
      backend: backend,
    );

    final firstA = controller.load();
    final loadB = controller.setStudentId(2);
    final latestA = controller.setStudentId(1);
    expect(backend.calls, [1, 2, 1]);

    backend.responses[2].complete(_periodResponse(13));
    await latestA;
    expect(controller.selected?.semesterId, 13);

    backend.responses[1].complete(_periodResponse(22));
    backend.responses[0].complete(_periodResponse(11));
    await Future.wait([firstA, loadB]);

    expect(controller.studentId, 1);
    expect(controller.selected?.semesterId, 13);
    expect(controller.periods.single.semesterId, 13);
  });
}
