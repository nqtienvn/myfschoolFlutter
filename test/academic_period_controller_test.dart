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
}
