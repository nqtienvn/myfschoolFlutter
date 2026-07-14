import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/auth_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/backend_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/chat_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/chat_socket_event_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/search_result_dto.dart';
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
    expect(
      find.byKey(const ValueKey('conversation-user-search')),
      findsOneWidget,
    );
  });

  testWidgets('ConversationsScreen searches and opens a user inline', (
    tester,
  ) async {
    final service = _FakeChatService(const [], const [
      SearchResultDto(
        id: 8,
        name: 'Nguyễn Văn Giáo viên',
        phone: '0909000001',
        role: 'TEACHER',
      ),
    ]);

    await tester.pumpWidget(
      MaterialApp(
        home: ConversationsScreen(actor: AppActor.parent, chatService: service),
      ),
    );
    await tester.enterText(
      find.byKey(const ValueKey('conversation-user-search')),
      '0909000001',
    );
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pump();

    expect(find.text('Nguyễn Văn Giáo viên'), findsOneWidget);
    await tester.tap(find.text('Nguyễn Văn Giáo viên'));
    await tester.pumpAndSettle();

    expect(service.createdForUserId, 8);
    expect(find.text('Nguyễn Văn Giáo viên'), findsOneWidget);
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
      final fields = tester
          .widgetList<TextField>(find.byType(TextField))
          .toList(growable: false);
      expect(fields[0].controller?.text, '0902000001');
      expect(fields[1].controller?.text, 'Demo@123');
      await tester.tap(find.text('Đăng Nhập'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(auth.loginCalls, 1);
      expect(auth.lastPhone, '0902000001');
      expect(auth.lastPassword, 'Demo@123');
      expect(chat.startCalls, 1);
      // Screen navigates to AppShell after success — no success toast
      await tester.pumpAndSettle();
      expect(find.byType(LoginScreen), findsNothing);
    },
  );

  testWidgets('account change password calls backend before showing success', (
    tester,
  ) async {
    final api = _RecordingAuthApi();
    final auth = _ProfileAuthService();
    await tester.pumpWidget(
      MaterialApp(
        home: AccountProfileScreen(
          actor: AppActor.student,
          authService: auth,
          authApiClient: api,
        ),
      ),
    );
    await tester.ensureVisible(find.text('Đổi mật khẩu'));
    await tester.tap(find.text('Đổi mật khẩu'));
    await tester.pumpAndSettle();

    await tester.enterText(
      find.widgetWithText(TextFormField, 'Mật khẩu hiện tại'),
      'old-password',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Mật khẩu mới'),
      'new-password',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Nhập lại mật khẩu mới'),
      'new-password',
    );
    await tester.tap(find.text('Cập nhật'));
    await tester.pumpAndSettle();

    expect(api.oldPassword, 'old-password');
    expect(api.newPassword, 'new-password');
    expect(api.token, 'profile-token');
    expect(find.text('Đổi mật khẩu thành công.'), findsOneWidget);
  });
}

class _RecordingAuthApi extends AuthApiClient {
  _RecordingAuthApi()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  String? token;
  String? oldPassword;
  String? newPassword;

  @override
  Future<void> changePassword({
    required String token,
    required String oldPassword,
    required String newPassword,
  }) async {
    this.token = token;
    this.oldPassword = oldPassword;
    this.newPassword = newPassword;
  }
}

class _ProfileAuthService extends AuthService {
  _ProfileAuthService()
    : super(
        apiClient: AuthApiClient(
          backend: BackendApiClient(baseUrl: 'http://localhost'),
        ),
      );

  @override
  AuthSession get currentSession => const AuthSession(
    token: 'profile-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    userId: 3,
    userName: 'Học sinh Test',
    role: 'STUDENT',
    phone: '0903000001',
    email: 'student@test.local',
    status: 'ACTIVE',
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
  String? lastPassword;

  @override
  Future<AuthSession> login(String phone, String password) async {
    loginCalls++;
    lastPhone = phone;
    lastPassword = password;
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
  _FakeChatService([this.items = const [], this.searchItems = const []])
    : super(
        repository: _FakeChatRepository(const []),
        socketService: _FakeSocketService(),
      );

  final List<Conversation> items;
  final List<SearchResultDto> searchItems;
  int startCalls = 0;
  int? createdForUserId;

  @override
  List<Conversation> get conversations => items;

  @override
  Future<void> start(AuthSession session) async {
    startCalls++;
  }

  @override
  Future<void> stop() async {}

  @override
  Future<List<SearchResultDto>> searchUsers(String keyword) async =>
      searchItems;

  @override
  Future<Conversation> createConversation({required int otherUserId}) async {
    createdForUserId = otherUserId;
    final user = searchItems.firstWhere((item) => item.id == otherUserId);
    return Conversation(
      id: 99,
      unreadCount: 0,
      otherParticipant: ChatParticipant(
        userId: user.id,
        name: user.name,
        role: user.role,
      ),
    );
  }
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
