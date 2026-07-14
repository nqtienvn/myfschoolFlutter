// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'dart:ui' show Size;

import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/repositories/repositories.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/login_screen.dart';
import 'package:myfschoolse1913/main.dart';

void main() {
  testWidgets('MyFschool app opens the login screen', (tester) async {
    await tester.binding.setSurfaceSize(const Size(1280, 720));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    final backend = BackendApiClient();
    final authService = AuthService(apiClient: AuthApiClient(backend: backend));
    final chatService = ChatService(
      repository: ChatRepository(apiClient: ChatApiClient(backend: backend)),
      socketService: ChatSocketService(backend: backend),
    );

    await tester.pumpWidget(
      MyFschoolApp(authService: authService, chatService: chatService),
    );

    expect(find.byType(LoginScreen), findsOneWidget);
    expect(tester.getSize(find.byType(LoginScreen)).width, 520);
  });
}
