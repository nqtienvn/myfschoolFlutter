import '../api/client/schedule_api_client.dart';
import '../models/school_schedule.dart';

class ScheduleService {
  const ScheduleService({
    required ScheduleApiClient apiClient,
    required String token,
  }) : _apiClient = apiClient,
       _token = token;

  final ScheduleApiClient _apiClient;
  final String _token;

  Future<SchoolSchedule> getForStudent(int studentId, [DateTime? date]) async {
    final dto = await _apiClient.getForStudent(
      token: _token,
      studentId: studentId,
      date: date,
    );
    return dto.toDomain();
  }

  Future<SchoolSchedule> getMine([DateTime? date]) async {
    final dto = await _apiClient.getMine(token: _token, date: date);
    return dto.toDomain();
  }
}
