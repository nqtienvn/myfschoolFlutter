import '../../models/school_announcement.dart';
import '../dto/announcement_dto.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

abstract class AnnouncementApi {
  Future<List<AnnouncementDto>> getAnnouncements({
    required String token,
    required bool teacher,
    int? academicYearId,
  });

  Future<AnnouncementDto> getDetail({required String token, required int id});
  Future<void> markRead({required String token, required int id});
  Future<int> getUnreadCount({required String token});
  Future<int> getUnreadCountForYear({
    required String token,
    int? academicYearId,
  }) => getUnreadCount(token: token);
  Future<AnnouncementRecipientPage> getRecipients({
    required String token,
    required int announcementId,
    required int academicYearId,
    int? classId,
    String? role,
    String? status,
    String? keyword,
    int page = 0,
    int size = 20,
  });
}

class AnnouncementApiClient implements AnnouncementApi {
  const AnnouncementApiClient({required BackendApiClient backend})
    : _backend = backend;

  final BackendApiClient _backend;

  @override
  Future<List<AnnouncementDto>> getAnnouncements({
    required String token,
    required bool teacher,
    int? academicYearId,
  }) async {
    final data = await _backend.getData(
      '/api/announcements',
      token: token,
      query: {'academicYearId': academicYearId?.toString()},
    );
    return _objectList(
      data,
      'Announcements',
    ).map(AnnouncementDto.fromJson).toList(growable: false);
  }

  @override
  Future<AnnouncementDto> getDetail({
    required String token,
    required int id,
  }) async {
    final data = await _backend.getData('/api/announcements/$id', token: token);
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Announcement detail must be an object.');
    }
    return AnnouncementDto.fromJson(data);
  }

  @override
  Future<void> markRead({required String token, required int id}) =>
      _backend.putData('/api/announcements/$id/read', token: token);

  @override
  Future<int> getUnreadCount({required String token}) =>
      _count('/api/announcements/unread-count', token);

  @override
  Future<int> getUnreadCountForYear({
    required String token,
    int? academicYearId,
  }) => _count(
    '/api/announcements/unread-count',
    token,
    query: {'academicYearId': academicYearId?.toString()},
  );

  Future<int> _count(
    String path,
    String token, {
    Map<String, String?>? query,
  }) async {
    final data = await _backend.getData(
      path,
      token: token,
      query: query ?? const {},
    );
    if (data is! int) {
      throw const ParseException('Announcement count must be an int.');
    }
    return data;
  }

  @override
  Future<AnnouncementRecipientPage> getRecipients({
    required String token,
    required int announcementId,
    required int academicYearId,
    int? classId,
    String? role,
    String? status,
    String? keyword,
    int page = 0,
    int size = 20,
  }) async {
    final data = await _backend.getData(
      '/api/announcements/$announcementId/recipients',
      token: token,
      query: {
        'academicYearId': '$academicYearId',
        'classId': classId?.toString(),
        'role': role,
        'status': status,
        'keyword': keyword?.trim(),
        'page': '$page',
        'size': '$size',
      },
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Recipient page must be an object.');
    }
    final content = _objectList(data['content'], 'Recipient content')
        .map(AnnouncementRecipientDto.fromJson)
        .map((item) => item.toDomain())
        .toList(growable: false);
    return AnnouncementRecipientPage(
      content: content,
      totalElements: data['totalElements'] is int
          ? data['totalElements'] as int
          : content.length,
      totalPages: data['totalPages'] is int ? data['totalPages'] as int : 1,
      page: data['number'] is int ? data['number'] as int : page,
    );
  }
}

List<Map<String, dynamic>> _objectList(Object? data, String label) {
  if (data is! List) throw ParseException('$label must be a list.');
  return data
      .map((item) {
        if (item is! Map<String, dynamic>) {
          throw ParseException('$label item must be an object.');
        }
        return item;
      })
      .toList(growable: false);
}
