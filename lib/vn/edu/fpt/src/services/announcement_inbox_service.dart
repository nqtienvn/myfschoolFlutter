import 'package:flutter/foundation.dart';

import '../api/client/announcement_api_client.dart';
import '../api/dto/announcement_dto.dart';
import '../models/school_announcement.dart';

class AnnouncementInboxService extends ChangeNotifier {
  AnnouncementInboxService({
    required AnnouncementApi api,
    required String token,
    required bool teacher,
  }) : _api = api,
       _token = token,
       isTeacher = teacher;

  final AnnouncementApi _api;
  final String _token;
  final bool isTeacher;

  List<SchoolAnnouncement> announcements = const [];
  int unreadCount = 0;
  int pendingActionCount = 0;
  int? academicYearId;
  bool isLoading = false;
  String? errorMessage;

  AnnouncementApi get api => _api;
  String get token => _token;

  Future<void> start() => load();

  Future<void> setAcademicYearId(int? value) async {
    if (academicYearId == value) return;
    academicYearId = value;
    announcements = const [];
    notifyListeners();
    await load();
  }

  Future<void> load() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();
    try {
      final listFuture = _api.getAnnouncements(
        token: _token,
        teacher: isTeacher,
        academicYearId: academicYearId,
      );
      final results = await Future.wait<Object>([
        listFuture,
        _api.getUnreadCountForYear(
          token: _token,
          academicYearId: isTeacher ? academicYearId : null,
        ),
        if (isTeacher)
          Future<Object>.value(0)
        else
          _api.getPendingActionCount(token: _token),
      ]);
      announcements = (results[0] as List<AnnouncementDto>)
          .map((item) => item.toDomain())
          .toList(growable: false);
      unreadCount = results[1] as int;
      pendingActionCount = results[2] as int;
    } catch (_) {
      errorMessage = 'Không thể tải thông báo.';
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<SchoolAnnouncement> open(int id) async {
    final wasUnread = announcements.any(
      (item) => item.id == id && !item.isRead,
    );
    await _api.markRead(token: _token, id: id);
    final detail = (await _api.getDetail(token: _token, id: id)).toDomain();
    _replace(detail);
    if (wasUnread && unreadCount > 0) unreadCount--;
    notifyListeners();
    return detail;
  }

  Future<SchoolAnnouncement> acknowledge(int id) async {
    await _api.acknowledge(token: _token, id: id);
    final detail = (await _api.getDetail(token: _token, id: id)).toDomain();
    _replace(detail);
    pendingActionCount = await _api.getPendingActionCount(token: _token);
    notifyListeners();
    return detail;
  }

  Future<SchoolAnnouncement> reply(int id, String text) async {
    await _api.reply(token: _token, id: id, text: text.trim());
    final detail = (await _api.getDetail(token: _token, id: id)).toDomain();
    _replace(detail);
    pendingActionCount = await _api.getPendingActionCount(token: _token);
    notifyListeners();
    return detail;
  }

  void _replace(SchoolAnnouncement value) {
    announcements = [
      for (final item in announcements) item.id == value.id ? value : item,
    ];
  }
}
