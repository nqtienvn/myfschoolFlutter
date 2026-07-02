import 'dart:async';
import 'dart:convert';
import 'dart:io';

import '../api/client/backend_api_client.dart';
import '../api/dto/chat_socket_event_dto.dart';
import '../models/auth_session.dart';

class ChatSocketService {
  ChatSocketService({required BackendApiClient backend}) : _backend = backend;

  final BackendApiClient _backend;
  final StreamController<ChatSocketEventDto> _events = StreamController<ChatSocketEventDto>.broadcast();
  WebSocket? _socket;
  AuthSession? _session;
  Timer? _heartbeatTimer;
  Timer? _reconnectTimer;
  bool _explicitDisconnect = false;
  int _reconnectAttempt = 0;

  Stream<ChatSocketEventDto> get events => _events.stream;
  bool get isConnected => _socket?.readyState == WebSocket.open;

  Future<void> connect(AuthSession session) async {
    _session = session;
    _explicitDisconnect = false;
    await _openSocket();
  }

  Future<void> disconnect() async {
    _explicitDisconnect = true;
    _heartbeatTimer?.cancel();
    _reconnectTimer?.cancel();
    await _socket?.close();
    _socket = null;
  }

  void sendMessage({required int conversationId, required String clientMessageId, required String content}) {
    _send({
      'type': 'message.send',
      'conversationId': conversationId,
      'clientMessageId': clientMessageId,
      'messageType': 'TEXT',
      'content': content,
    });
  }

  void markDelivered({required int conversationId, required int messageId}) {
    _send({'type': 'message.delivered', 'conversationId': conversationId, 'messageId': messageId});
  }

  void markRead({required int conversationId, required int lastReadMessageId}) {
    _send({'type': 'message.read', 'conversationId': conversationId, 'lastReadMessageId': lastReadMessageId});
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
    _socket = await WebSocket.connect(_backend.wsUri('/chat', token: session.token).toString());
    _reconnectAttempt = 0;
    _heartbeatTimer?.cancel();
    _heartbeatTimer = Timer.periodic(const Duration(seconds: 15), (_) => _send({'type': 'presence.heartbeat'}));
    _socket!.listen(
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

  void _send(Map<String, Object?> payload) {
    if (!isConnected) return;
    _socket!.add(jsonEncode(payload));
  }

  void _scheduleReconnect() {
    _heartbeatTimer?.cancel();
    _socket = null;
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
