import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/backend_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/chat_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/chat_message_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/chat_socket_event_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/auth_session.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/chat_message.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/conversation.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/repositories/chat_repository.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/chat_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/chat_socket_service.dart';

void main() {
  test(
    'new conversation appears after first message reaches the server',
    () async {
      const pending = Conversation(
        id: 99,
        unreadCount: 0,
        otherParticipant: ChatParticipant(
          userId: 2,
          name: 'Giáo viên Test',
          role: 'TEACHER',
        ),
      );
      final repository = _FakeChatRepository(createdConversation: pending);
      final socket = _FakeSocketService();
      final service = ChatService(
        repository: repository,
        socketService: socket,
      );
      await service.start(_session);

      await service.createConversation(otherUserId: 2);
      expect(service.conversations, isEmpty);

      service.sendText(99, 'Xin chào cô');
      final clientMessageId = socket.sentClientMessageId!;
      socket.emit(
        ChatSocketEventDto(
          type: 'message.ack',
          clientMessageId: clientMessageId,
          message: ChatMessageDto(
            id: 501,
            clientMessageId: clientMessageId,
            conversationId: 99,
            senderId: 1,
            senderName: 'Phụ huynh Test',
            messageType: 'TEXT',
            content: 'Xin chào cô',
            serverSeq: 1,
            status: 'sent',
            createdAt: DateTime(2026, 7, 14, 18),
          ),
        ),
      );
      await Future<void>.delayed(Duration.zero);

      expect(service.conversations, hasLength(1));
      expect(service.conversations.single.lastMessage, 'Xin chào cô');
      expect(service.messagesFor(99).single.status, ChatMessageStatus.sent);

      socket.emit(
        const ChatSocketEventDto(
          type: 'message.receipt',
          conversationId: 99,
          messageId: 501,
          status: 'delivered',
        ),
      );
      await Future<void>.delayed(Duration.zero);
      expect(
        service.messagesFor(99).single.status,
        ChatMessageStatus.delivered,
      );

      socket.emit(
        const ChatSocketEventDto(
          type: 'conversation.read',
          conversationId: 99,
          lastReadMessageId: 501,
          userId: 2,
        ),
      );
      await Future<void>.delayed(Duration.zero);
      expect(service.messagesFor(99).single.status, ChatMessageStatus.read);

      await service.stop();
    },
  );

  test(
    'incoming realtime message updates badges and opening clears unread',
    () async {
      final repository = _FakeChatRepository(
        conversations: [
          Conversation(
            id: 7,
            unreadCount: 0,
            lastMessage: 'Cũ',
            lastMessageAt: DateTime(2026, 7, 14, 17),
            otherParticipant: const ChatParticipant(
              userId: 2,
              name: 'Giáo viên Test',
              role: 'TEACHER',
            ),
          ),
        ],
      );
      final socket = _FakeSocketService();
      final service = ChatService(
        repository: repository,
        socketService: socket,
      );
      await service.start(_session);

      socket.emit(
        ChatSocketEventDto(
          type: 'message.new',
          message: ChatMessageDto(
            id: 701,
            clientMessageId: 'teacher-701',
            conversationId: 7,
            senderId: 2,
            senderName: 'Giáo viên Test',
            messageType: 'TEXT',
            content: 'Tin mới',
            serverSeq: 2,
            status: 'sent',
            createdAt: DateTime(2026, 7, 14, 18, 5),
          ),
        ),
      );
      await Future<void>.delayed(Duration.zero);

      expect(service.totalUnreadCount, 1);
      expect(service.conversations.single.lastMessage, 'Tin mới');
      expect(socket.deliveredMessageIds, [701]);

      await service.openConversation(7);
      await Future<void>.delayed(Duration.zero);

      expect(service.totalUnreadCount, 0);
      expect(socket.lastReadMessageId, 701);
      expect(repository.lastReadMessageId, 701);

      await service.stop();
    },
  );

  test(
    'messages fetched after being offline are acknowledged as delivered',
    () async {
      final repository = _FakeChatRepository(
        conversations: [
          Conversation(
            id: 8,
            unreadCount: 1,
            lastMessage: 'Tin lúc offline',
            lastMessageAt: DateTime(2026, 7, 14, 18, 10),
            otherParticipant: const ChatParticipant(
              userId: 2,
              name: 'Giáo viên Test',
              role: 'TEACHER',
            ),
          ),
        ],
        messages: [
          ChatMessage(
            id: 801,
            clientMessageId: 'teacher-offline-801',
            conversationId: 8,
            senderId: 2,
            senderName: 'Giáo viên Test',
            content: 'Tin lúc offline',
            type: ChatMessageType.text,
            status: ChatMessageStatus.delivered,
            createdAt: DateTime(2026, 7, 14, 18, 10),
            isMine: false,
            serverSeq: 1,
          ),
        ],
      );
      final socket = _FakeSocketService();
      final service = ChatService(
        repository: repository,
        socketService: socket,
      );
      await service.start(_session);

      await service.openConversation(8);
      await Future<void>.delayed(Duration.zero);

      expect(socket.deliveredMessageIds, [801]);
      expect(socket.lastReadMessageId, 801);
      expect(repository.lastReadMessageId, 801);

      await service.stop();
    },
  );
}

const _session = AuthSession(
  token: 'token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  userId: 1,
  userName: 'Phụ huynh Test',
  role: 'PARENT',
  phone: '0909000002',
  email: 'parent@test.local',
  status: 'ACTIVE',
  accountCode: 'PH-000001',
);

class _FakeChatRepository extends ChatRepository {
  _FakeChatRepository({
    this.conversations = const [],
    this.createdConversation,
    this.messages = const [],
  }) : super(
         apiClient: ChatApiClient(
           backend: BackendApiClient(baseUrl: 'http://localhost'),
         ),
       );

  final List<Conversation> conversations;
  final Conversation? createdConversation;
  final List<ChatMessage> messages;
  int? lastReadMessageId;

  @override
  Future<List<Conversation>> getConversations({required String token}) async =>
      List.of(conversations);

  @override
  Future<Conversation> createConversation({
    required String token,
    required int otherUserId,
  }) async => createdConversation!;

  @override
  Future<List<ChatMessage>> getMessages({
    required String token,
    required int currentUserId,
    required int conversationId,
    int? beforeMessageId,
    int? afterSeq,
    int limit = 20,
  }) async => List.of(messages);

  @override
  Future<void> markRead({
    required String token,
    required int conversationId,
    int? lastReadMessageId,
  }) async {
    this.lastReadMessageId = lastReadMessageId;
  }
}

class _FakeSocketService extends ChatSocketService {
  _FakeSocketService()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final StreamController<ChatSocketEventDto> _eventController =
      StreamController<ChatSocketEventDto>.broadcast();
  final List<int> deliveredMessageIds = [];
  String? sentClientMessageId;
  int? lastReadMessageId;

  @override
  Stream<ChatSocketEventDto> get events => _eventController.stream;

  void emit(ChatSocketEventDto event) => _eventController.add(event);

  @override
  Future<void> connect(AuthSession session) async {}

  @override
  Future<void> disconnect() => _eventController.close();

  @override
  void sendMessage({
    required int conversationId,
    required String clientMessageId,
    required String content,
  }) {
    sentClientMessageId = clientMessageId;
  }

  @override
  void markDelivered({required int conversationId, required int messageId}) {
    deliveredMessageIds.add(messageId);
  }

  @override
  void markRead({required int conversationId, required int lastReadMessageId}) {
    this.lastReadMessageId = lastReadMessageId;
  }
}
