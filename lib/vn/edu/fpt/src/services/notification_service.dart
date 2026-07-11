import 'dart:async';

import 'package:flutter/foundation.dart';

import '../api/client/notification_api_client.dart';
import '../api/dto/chat_socket_event_dto.dart';
import '../models/app_notification.dart';

class NotificationService extends ChangeNotifier {
  NotificationService({
    required NotificationApiClient apiClient,
    required Stream<ChatSocketEventDto> socketEvents,
    required String token,
  }) : _apiClient = apiClient,
       _socketEvents = socketEvents,
       _token = token;

  final NotificationApiClient _apiClient;
  final Stream<ChatSocketEventDto> _socketEvents;
  final String _token;
  StreamSubscription<ChatSocketEventDto>? _subscription;

  List<AppNotification> notifications = const [];
  bool isLoading = false;
  String? errorMessage;

  int get unreadCount => notifications.where((item) => !item.isRead).length;

  Future<void> start() async {
    _subscription ??= _socketEvents.listen(_handleSocketEvent);
    await load();
  }

  Future<void> load() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();
    try {
      final dtos = await _apiClient.getNotifications(token: _token);
      notifications = dtos
          .map((item) => item.toDomain())
          .toList(growable: false);
    } catch (_) {
      errorMessage = 'Không thể tải thông báo.';
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  Future<void> markAsRead(int id) async {
    final index = notifications.indexWhere((item) => item.id == id);
    if (index < 0 || notifications[index].isRead) return;
    notifications = [
      for (final item in notifications)
        item.id == id ? item.copyWith(isRead: true) : item,
    ];
    notifyListeners();
    try {
      await _apiClient.markAsRead(token: _token, id: id);
    } catch (_) {
      await load();
    }
  }

  Future<void> markAllAsRead() async {
    notifications = notifications
        .map((item) => item.copyWith(isRead: true))
        .toList(growable: false);
    notifyListeners();
    try {
      await _apiClient.markAllAsRead(token: _token);
    } catch (_) {
      await load();
    }
  }

  void _handleSocketEvent(ChatSocketEventDto event) {
    if (event.type != 'notification.new' || event.notification == null) return;
    final incoming = event.notification!.toDomain();
    notifications = [
      incoming,
      ...notifications.where((item) => item.id != incoming.id),
    ];
    notifyListeners();
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }
}
