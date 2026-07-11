import '../dto/notification_dto.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class NotificationApiClient {
  const NotificationApiClient({required BackendApiClient backend})
    : _backend = backend;

  final BackendApiClient _backend;

  Future<List<NotificationDto>> getNotifications({
    required String token,
  }) async {
    final data = await _backend.getData('/api/notifications', token: token);
    if (data is! List) {
      throw const ParseException('Notifications response must be a list.');
    }
    return data
        .map((item) {
          if (item is! Map<String, dynamic>) {
            throw const ParseException('Notification item must be an object.');
          }
          return NotificationDto.fromJson(item);
        })
        .toList(growable: false);
  }

  Future<void> markAsRead({required String token, required int id}) =>
      _backend.putData('/api/notifications/$id/read', token: token);

  Future<void> markAllAsRead({required String token}) =>
      _backend.putData('/api/notifications/read-all', token: token);
}
