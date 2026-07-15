import '../dto/homeroom_academic_dto.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class HomeroomAcademicApiClient {
  const HomeroomAcademicApiClient({required BackendApiClient backend})
    : _backend = backend;

  final BackendApiClient _backend;

  Future<HomeroomClassDetailDto> getClassDetail({
    required String token,
    required int classId,
  }) async {
    final data = await _backend.getData('/api/classes/$classId', token: token);
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu hồ sơ lớp không hợp lệ.');
    }
    return HomeroomClassDetailDto.fromJson(data);
  }

  Future<HomeroomClassRankingDto> getClassRanking({
    required String token,
    required int classId,
    required int semesterId,
  }) async {
    final data = await _backend.getData(
      '/api/semester-results/ranking',
      token: token,
      query: {
        'classId': classId.toString(),
        'semesterId': semesterId.toString(),
      },
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu xếp hạng lớp không hợp lệ.');
    }
    return HomeroomClassRankingDto.fromJson(data);
  }

  Future<HomeroomStudentResultDto?> getStudentSemesterResult({
    required String token,
    required int studentId,
    required int semesterId,
  }) async {
    final data = await _backend.getData(
      '/api/semester-results',
      token: token,
      query: {
        'studentId': studentId.toString(),
        'semesterId': semesterId.toString(),
      },
    );
    if (data == null) return null;
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu tổng kết học sinh không hợp lệ.');
    }
    return HomeroomStudentResultDto.fromJson(data);
  }
}
