import '../dto/school_schedule_dto.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class ScheduleApiClient {
  const ScheduleApiClient({required BackendApiClient backend})
    : _backend = backend;

  final BackendApiClient _backend;

  Future<SchoolScheduleDto> getForStudent({
    required String token,
    required int studentId,
    int? semesterId,
    DateTime? date,
  }) async {
    final data = await _backend.getData(
      '/api/schedules/student/$studentId',
      token: token,
      query: {
        'semesterId': semesterId?.toString(),
        'date': date == null ? null : _date(date),
      },
    );
    return _parse(data);
  }

  Future<SchoolScheduleDto> getMine({
    required String token,
    int? semesterId,
    DateTime? date,
  }) async {
    final data = await _backend.getData(
      '/api/schedules/me',
      token: token,
      query: {
        'semesterId': semesterId?.toString(),
        'date': date == null ? null : _date(date),
      },
    );
    return _parse(data);
  }

  SchoolScheduleDto _parse(Object? data) {
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu thời khóa biểu không hợp lệ');
    }
    return SchoolScheduleDto.fromJson(data);
  }

  static String _date(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}
