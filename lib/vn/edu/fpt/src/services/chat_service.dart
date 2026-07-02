import 'dart:async';
import 'dart:math';

import 'package:flutter/foundation.dart';

import '../api/dto/chat_socket_event_dto.dart';
import '../models/auth_session.dart';
import '../models/chat_message.dart';
import '../models/conversation.dart';
import '../repositories/chat_repository.dart';
import 'chat_socket_service.dart';

class ChatService extends ChangeNotifier {
  ChatService({required ChatRepository repository, required ChatSocketService socketService})
      : _repository = repository,
        _socketService = socketService;

  final ChatRepository _repository;
  final ChatSocketService _socketService;
  final Map<int, List<ChatMessage>> _messagesByConversationId = {};
  final Map<String, Timer> _ackTimers = {};
  final Map<int, Timer> _typingTimers = {};
  late final StreamSubscription<ChatSocketEventDto> _socketSub;

  AuthSession? _session;
  List<Conversation> _conversations = [];
  int? _activeConversationId;
  bool isLoadingConversations = false;
  String? errorMessage;
  final Set<int> typingConversationIds = {};

  List<Conversation> get conversations => List.unmodifiable(_conversations);
  int? get activeConversationId => _activeConversationId;

  List<ChatMessage> messagesFor(int conversationId) {
    return List.unmodifiable(_messagesByConversationId[conversationId] ?? const []);
  }

  Future<void> start(AuthSession session) async {
    _session = session;
    _socketSub = _socketService.events.listen(_handleEvent);
    await _socketService.connect(session);
    await loadConversations();
  }

  Future<void> stop() async {
    for (final timer in _ackTimers.values) {
      timer.cancel();
    }
    for (final timer in _typingTimers.values) {
      timer.cancel();
    }
    _ackTimers.clear();
    _typingTimers.clear();
    await _socketSub.cancel();
    await _socketService.disconnect();
  }

  Future<void> loadConversations() async {
    final session = _session;
    if (session == null) return;
    isLoadingConversations = true;
    errorMessage = null;
    notifyListeners();
    try {
      _conversations = await _repository.getConversations(token: session.token);
    } catch (ex) {
      errorMessage = 'Không tải được danh sách tin nhắn';
    } finally {
      isLoadingConversations = false;
      notifyListeners();
    }
  }

  Future<void> loadMessages(int conversationId, {int limit = 20}) async {
    final session = _session;
    if (session == null) return;
    final messages = await _repository.getMessages(
      token: session.token,
      currentUserId: session.userId,
      conversationId: conversationId,
      limit: limit,
    );
    _messagesByConversationId[conversationId] = _mergeAll(_messagesByConversationId[conversationId] ?? [], messages);
    _markReadIfActive(conversationId);
    notifyListeners();
  }

  Future<void> loadOlderMessages(int conversationId) async {
    final session = _session;
    if (session == null) return;
    final current = _messagesByConversationId[conversationId] ?? [];
    final ids = current.map((m) => m.id).whereType<int>();
    if (ids.isEmpty) return;
    final older = await _repository.getMessages(
      token: session.token,
      currentUserId: session.userId,
      conversationId: conversationId,
      beforeMessageId: ids.reduce(min),
    );
    _messagesByConversationId[conversationId] = _mergeAll(current, older);
    notifyListeners();
  }

  Future<void> syncAfterReconnect() async {
    final session = _session;
    if (session == null) return;
    for (final conversation in _conversations) {
      final current = _messagesByConversationId[conversation.id] ?? [];
      final seqs = current.map((m) => m.serverSeq).whereType<int>();
      if (seqs.isEmpty) continue;
      final fresh = await _repository.getMessages(
        token: session.token,
        currentUserId: session.userId,
        conversationId: conversation.id,
        afterSeq: seqs.reduce(max),
        limit: 100,
      );
      _messagesByConversationId[conversation.id] = _mergeAll(current, fresh);
    }
    notifyListeners();
  }

  Future<void> openConversation(int conversationId) async {
    _activeConversationId = conversationId;
    await loadMessages(conversationId);
  }

  void closeConversation(int conversationId) {
    if (_activeConversationId == conversationId) _activeConversationId = null;
  }

  void sendText(int conversationId, String text) {
    final session = _session;
    final content = text.trim();
    if (session == null || content.isEmpty) return;
    final clientMessageId = 'fe-${DateTime.now().microsecondsSinceEpoch}-${Random().nextInt(999999)}';
    final local = ChatMessage(
      clientMessageId: clientMessageId,
      conversationId: conversationId,
      senderId: session.userId,
      senderName: session.userName,
      content: content,
      type: ChatMessageType.text,
      status: ChatMessageStatus.sending,
      createdAt: DateTime.now(),
      isMine: true,
    );
    _upsert(local);
    _socketService.sendMessage(conversationId: conversationId, clientMessageId: clientMessageId, content: content);
    _ackTimers[clientMessageId]?.cancel();
    _ackTimers[clientMessageId] = Timer(const Duration(seconds: 8), () {
      _updateStatus(clientMessageId, ChatMessageStatus.failed);
    });
    notifyListeners();
  }

  void retryMessage(String clientMessageId) {
    final message = _findByClientId(clientMessageId);
    if (message == null) return;
    _updateStatus(clientMessageId, ChatMessageStatus.sending);
    _socketService.sendMessage(
      conversationId: message.conversationId,
      clientMessageId: message.clientMessageId,
      content: message.content,
    );
  }

  void sendTypingStart(int conversationId) => _socketService.sendTypingStart(conversationId: conversationId);
  void sendTypingStop(int conversationId) => _socketService.sendTypingStop(conversationId: conversationId);

  void _handleEvent(ChatSocketEventDto event) {
    switch (event.type) {
      case 'message.ack':
        _handleAck(event);
        break;
      case 'message.new':
        _handleNew(event);
        break;
      case 'message.receipt':
        if (event.messageId != null) _updateStatusById(event.messageId!, ChatMessageStatus.delivered);
        break;
      case 'conversation.read':
        _handleRead(event);
        break;
      case 'typing.update':
        _handleTyping(event);
        break;
      case 'presence.update':
        _handlePresence(event);
        break;
      case 'error':
        if (event.clientMessageId != null) _updateStatus(event.clientMessageId!, ChatMessageStatus.failed);
        errorMessage = event.errorMessage ?? 'Không gửi được tin nhắn';
        break;
    }
    notifyListeners();
  }

  void _handleAck(ChatSocketEventDto event) {
    final session = _session;
    final message = event.message;
    if (session == null || message == null) return;
    _ackTimers.remove(event.clientMessageId)?.cancel();
    _upsert(message.toDomain(session.userId).copyWith(status: ChatMessageStatus.sent));
  }

  void _handleNew(ChatSocketEventDto event) {
    final session = _session;
    final messageDto = event.message;
    if (session == null || messageDto == null) return;
    final message = messageDto.toDomain(session.userId);
    _upsert(message);
    _updateConversationPreview(message);
    if (!message.isMine && message.id != null) {
      _socketService.markDelivered(conversationId: message.conversationId, messageId: message.id!);
    }
    _markReadIfActive(message.conversationId);
  }

  void _handleRead(ChatSocketEventDto event) {
    final id = event.lastReadMessageId;
    final convId = event.conversationId;
    if (id == null || convId == null) return;
    final messages = _messagesByConversationId[convId] ?? [];
    _messagesByConversationId[convId] = messages
        .map((m) => m.isMine && m.id != null && m.id! <= id ? m.copyWith(status: ChatMessageStatus.read) : m)
        .toList();
  }

  void _handleTyping(ChatSocketEventDto event) {
    final convId = event.conversationId;
    final session = _session;
    if (convId == null || session == null || event.userId == session.userId) return;
    if (event.typing == true) {
      typingConversationIds.add(convId);
      _typingTimers[convId]?.cancel();
      _typingTimers[convId] = Timer(const Duration(seconds: 5), () {
        typingConversationIds.remove(convId);
        notifyListeners();
      });
    } else {
      typingConversationIds.remove(convId);
    }
  }

  void _handlePresence(ChatSocketEventDto event) {
    final userId = event.userId;
    if (userId == null) return;
    _conversations = _conversations.map((c) {
      if (c.otherParticipant?.userId != userId) return c;
      return c.copyWith(isOnline: event.online ?? false, lastSeenAt: event.lastSeenAt);
    }).toList();
  }

  void _markReadIfActive(int conversationId) {
    if (_activeConversationId != conversationId) return;
    final lastId = (_messagesByConversationId[conversationId] ?? []).map((m) => m.id).whereType<int>().fold<int?>(null, (a, b) => a == null || b > a ? b : a);
    if (lastId != null) _socketService.markRead(conversationId: conversationId, lastReadMessageId: lastId);
  }

  void _upsert(ChatMessage message) {
    final current = _messagesByConversationId[message.conversationId] ?? [];
    _messagesByConversationId[message.conversationId] = _mergeAll(current, [message]);
  }

  List<ChatMessage> _mergeAll(List<ChatMessage> current, List<ChatMessage> incoming) {
    final result = [...current];
    for (final message in incoming) {
      final index = result.indexWhere((m) =>
          (message.id != null && m.id == message.id) ||
          (message.clientMessageId.isNotEmpty && m.clientMessageId == message.clientMessageId));
      if (index >= 0) {
        result[index] = _preferNewer(result[index], message);
      } else {
        result.add(message);
      }
    }
    result.sort((a, b) {
      if (a.serverSeq != null && b.serverSeq != null) return a.serverSeq!.compareTo(b.serverSeq!);
      return a.createdAt.compareTo(b.createdAt);
    });
    return result;
  }

  ChatMessage _preferNewer(ChatMessage old, ChatMessage next) {
    final status = _rank(next.status) >= _rank(old.status) ? next.status : old.status;
    return next.copyWith(status: status);
  }

  int _rank(ChatMessageStatus status) => switch (status) {
        ChatMessageStatus.sending => 0,
        ChatMessageStatus.failed => 0,
        ChatMessageStatus.sent => 1,
        ChatMessageStatus.delivered => 2,
        ChatMessageStatus.read => 3,
      };

  void _updateStatus(String clientMessageId, ChatMessageStatus status) {
    for (final entry in _messagesByConversationId.entries) {
      entry.value.replaceRange(
        0,
        entry.value.length,
        entry.value.map((m) => m.clientMessageId == clientMessageId ? m.copyWith(status: status) : m),
      );
    }
    notifyListeners();
  }

  void _updateStatusById(int id, ChatMessageStatus status) {
    for (final entry in _messagesByConversationId.entries) {
      entry.value.replaceRange(
        0,
        entry.value.length,
        entry.value.map((m) => m.id == id && _rank(status) > _rank(m.status) ? m.copyWith(status: status) : m),
      );
    }
  }

  ChatMessage? _findByClientId(String clientMessageId) {
    for (final messages in _messagesByConversationId.values) {
      for (final message in messages) {
        if (message.clientMessageId == clientMessageId) return message;
      }
    }
    return null;
  }

  void _updateConversationPreview(ChatMessage message) {
    _conversations = _conversations.map((conversation) {
      if (conversation.id != message.conversationId) return conversation;
      return conversation.copyWith(
        lastMessage: message.content,
        lastMessageAt: message.createdAt,
        unreadCount: _activeConversationId == message.conversationId || message.isMine ? 0 : conversation.unreadCount + 1,
      );
    }).toList();
  }
}
