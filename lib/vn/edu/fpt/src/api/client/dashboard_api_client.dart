import '../dto/student_dashboard_stats_dto.dart';
import '../dto/teacher_dashboard_stats_dto.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class DashboardApiClient {
  const DashboardApiClient({required BackendApiClient backend})
    : _backend = backend;

  final BackendApiClient _backend;

  Future<StudentDashboardStatsDto> getStudentStats({
    required String token,
    required int academicYearId,
    required int semesterId,
    int? studentId,
  }) async {
    final data = await _backend.getData(
      '/api/dashboard/student',
      token: token,
      query: {
        'studentId': studentId?.toString(),
        'academicYearId': academicYearId.toString(),
        'semesterId': semesterId.toString(),
      },
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu dashboard học sinh không hợp lệ.');
    }
    return StudentDashboardStatsDto.fromJson(data);
  }

  Future<TeacherDashboardStatsDto> getTeacherStats({
    required String token,
    required int academicYearId,
    required int semesterId,
  }) async {
    final data = await _backend.getData(
      '/api/dashboard/teacher',
      token: token,
      query: {
        'academicYearId': academicYearId.toString(),
        'semesterId': semesterId.toString(),
      },
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu thống kê giáo viên không hợp lệ.');
    }
    return TeacherDashboardStatsDto.fromJson(data);
  }
}
