import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/auth_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/backend_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/chat_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/chat_socket_event_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/auth_session.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/conversation.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/repositories/chat_repository.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/auth_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/chat_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/chat_socket_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/login_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/messages_screen.dart';

void main() {
  testWidgets('ConversationsScreen renders conversations from ChatService', (
    tester,
  ) async {
    final service = _FakeChatService([
      const Conversation(
        id: 42,
        unreadCount: 2,
        lastMessage: 'Tin nhắn từ backend',
        otherParticipant: ChatParticipant(
          userId: 7,
          name: 'GV Backend',
          role: 'TEACHER',
        ),
      ),
    ]);

    await tester.pumpWidget(
      MaterialApp(
        home: ConversationsScreen(actor: AppActor.parent, chatService: service),
      ),
    );
    await tester.pump();

    expect(find.text('GV Backend'), findsOneWidget);
    expect(find.text('Tin nhắn từ backend'), findsOneWidget);
    expect(find.text('2'), findsOneWidget);
  });

  testWidgets(
    'ConversationsScreen renders conversations with correct title for TEACHER role',
    (tester) async {
      final service = _FakeChatService([
        const Conversation(
          id: 42,
          unreadCount: 3,
          lastMessage: 'Chào cô giáo',
          otherParticipant: ChatParticipant(
            userId: 8,
            name: 'PH Học Sinh',
            role: 'PARENT',
          ),
        ),
      ]);

      await tester.pumpWidget(
        MaterialApp(
          home: ConversationsScreen(
            actor: AppActor.teacher,
            chatService: service,
          ),
        ),
      );
      await tester.pump();

      expect(find.text('PH Học Sinh'), findsOneWidget);
      expect(find.text('Chào cô giáo'), findsOneWidget);
      expect(find.text('3'), findsOneWidget);
      expect(find.text('Tin nhắn phụ huynh'), findsOneWidget);
    },
  );

  testWidgets(
    'LoginScreen authenticates and starts chat service before role selection',
    (tester) async {
      final auth = _FakeAuthService();
      final chat = _FakeChatService();

      await tester.pumpWidget(
        MaterialApp(
          home: LoginScreen(authService: auth, chatService: chat),
        ),
      );
      await tester.enterText(find.byType(TextField).at(0), '0909000002');
      await tester.enterText(find.byType(TextField).at(1), 'test1234');
      await tester.tap(find.text('Đăng Nhập'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(auth.loginCalls, 1);
      expect(auth.lastPhone, '0909000002');
      expect(chat.startCalls, 1);
      // Screen navigates to AppShell after success — no success toast
      await tester.pumpAndSettle();
      expect(find.byType(LoginScreen), findsNothing);
    },
  );
}

class _FakeAuthService extends AuthService {
  _FakeAuthService()
    : super(
        apiClient: AuthApiClient(
          backend: BackendApiClient(baseUrl: 'http://localhost'),
        ),
      );

  int loginCalls = 0;
  String? lastPhone;

  @override
  Future<AuthSession> login(String phone, String password) async {
    loginCalls++;
    lastPhone = phone;
    return const AuthSession(
      token: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 1,
      userName: 'PH Test',
      role: 'PARENT',
      phone: '0901000000',
      email: 'parent@test.local',
      status: 'ACTIVE',
      accountCode: 'PH-000001',
    );
  }
}

class _FakeChatService extends ChatService {
  _FakeChatService([this.items = const []])
    : super(
        repository: _FakeChatRepository(const []),
        socketService: _FakeSocketService(),
      );

  final List<Conversation> items;
  int startCalls = 0;

  @override
  List<Conversation> get conversations => items;

  @override
  Future<void> start(AuthSession session) async {
    startCalls++;
  }

  @override
  Future<void> stop() async {}
}

class _FakeChatRepository extends ChatRepository {
  _FakeChatRepository(this.items)
    : super(
        apiClient: ChatApiClient(
          backend: BackendApiClient(baseUrl: 'http://localhost'),
        ),
      );

  final List<Conversation> items;

  @override
  Future<List<Conversation>> getConversations({required String token}) async =>
      items;
}

class _FakeSocketService extends ChatSocketService {
  _FakeSocketService()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final _events = StreamController<ChatSocketEventDto>.broadcast();

  @override
  Stream<ChatSocketEventDto> get events => _events.stream;

  @override
  Future<void> connect(AuthSession session) async {}

  @override
  Future<void> disconnect() async => _events.close();
}
