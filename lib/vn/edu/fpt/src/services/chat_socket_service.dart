import 'dart:async';
import 'dart:convert';

import 'package:web_socket_channel/web_socket_channel.dart';

import '../api/client/backend_api_client.dart';
import '../api/dto/chat_socket_event_dto.dart';
import '../models/auth_session.dart';

class ChatSocketService {
  ChatSocketService({required BackendApiClient backend}) : _backend = backend;

  final BackendApiClient _backend;
  final StreamController<ChatSocketEventDto> _events =
      StreamController<ChatSocketEventDto>.broadcast();
  final StreamController<void> _reconnected =
      StreamController<void>.broadcast();
  final List<Map<String, Object?>> _pendingReliableEvents = [];
  WebSocketChannel? _channel;
  StreamSubscription? _channelSub;
  AuthSession? _session;
  Timer? _heartbeatTimer;
  Timer? _reconnectTimer;
  bool _explicitDisconnect = false;
  bool _ready = false;
  int _reconnectAttempt = 0;

  Stream<ChatSocketEventDto> get events => _events.stream;
  Stream<void> get reconnected => _reconnected.stream;
  bool get isConnected => _ready && _channel != null;

  Future<void> connect(AuthSession session) async {
    _session = session;
    _explicitDisconnect = false;
    try {
      await _openSocket();
    } catch (_) {
      _scheduleReconnect();
      rethrow;
    }
  }

  Future<void> disconnect() async {
    _explicitDisconnect = true;
    _heartbeatTimer?.cancel();
    _reconnectTimer?.cancel();
    await _channelSub?.cancel();
    await _channel?.sink.close();
    _channel = null;
    _ready = false;
    _pendingReliableEvents.clear();
  }

  void sendMessage({
    required int conversationId,
    required String clientMessageId,
    required String content,
  }) {
    _send({
      'type': 'message.send',
      'conversationId': conversationId,
      'clientMessageId': clientMessageId,
      'messageType': 'TEXT',
      'content': content,
    }, reliable: true);
  }

  void markDelivered({required int conversationId, required int messageId}) {
    _send({
      'type': 'message.delivered',
      'conversationId': conversationId,
      'messageId': messageId,
    }, reliable: true);
  }

  void markRead({required int conversationId, required int lastReadMessageId}) {
    _send({
      'type': 'message.read',
      'conversationId': conversationId,
      'lastReadMessageId': lastReadMessageId,
    }, reliable: true);
  }

  void sendTypingStart({required int conversationId}) {
    _send({'type': 'typing.start', 'conversationId': conversationId});
  }

  void sendTypingStop({required int conversationId}) {
    _send({'type': 'typing.stop', 'conversationId': conversationId});
  }

  Future<void> _openSocket() async {
    final session = _session;
    if (session == null) return;
    final wasReconnect = _reconnectAttempt > 0;
    final uri = _backend.wsUri('/chat', token: session.token);
    _channel = WebSocketChannel.connect(uri);
    await _channel!.ready;
    _ready = true;
    for (final event in List<Map<String, Object?>>.of(_pendingReliableEvents)) {
      _channel!.sink.add(jsonEncode(event));
    }
    _pendingReliableEvents.clear();
    _reconnectAttempt = 0;
    if (wasReconnect) _reconnected.add(null);
    _heartbeatTimer?.cancel();
    _heartbeatTimer = Timer.periodic(
      const Duration(seconds: 15),
      (_) => _send({'type': 'presence.heartbeat'}),
    );
    _channelSub = _channel!.stream.listen(
      _handleData,
      onDone: _scheduleReconnect,
      onError: (_) => _scheduleReconnect(),
      cancelOnError: true,
    );
  }

  void _handleData(dynamic data) {
    if (data is! String) return;
    final decoded = jsonDecode(data);
    if (decoded is Map<String, dynamic>) {
      _events.add(ChatSocketEventDto.fromJson(decoded));
    }
  }

  void _send(Map<String, Object?> payload, {bool reliable = false}) {
    if (!isConnected) {
      if (reliable) _pendingReliableEvents.add(payload);
      return;
    }
    _channel!.sink.add(jsonEncode(payload));
  }

  void _scheduleReconnect() {
    _heartbeatTimer?.cancel();
    _channelSub = null;
    _channel = null;
    _ready = false;
    if (_explicitDisconnect || _session == null) return;
    final delays = [1, 2, 5, 10, 15];
    final seconds = delays[_reconnectAttempt.clamp(0, delays.length - 1)];
    _reconnectAttempt++;
    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(Duration(seconds: seconds), () {
      _openSocket().catchError((_) => _scheduleReconnect());
    });
  }
}
