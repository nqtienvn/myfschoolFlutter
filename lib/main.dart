import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/repositories/repositories.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_theme.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/login_screen.dart';

void main() {
  final backend = BackendApiClient();
  final authService = AuthService(apiClient: AuthApiClient(backend: backend));
  final chatService = ChatService(
    repository: ChatRepository(apiClient: ChatApiClient(backend: backend)),
    socketService: ChatSocketService(backend: backend),
  );

  runApp(MyFschoolApp(authService: authService, chatService: chatService));
}

class MyFschoolApp extends StatelessWidget {
  const MyFschoolApp({super.key, required this.authService, required this.chatService});

  final AuthService authService;
  final ChatService chatService;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MyFschool',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      home: LoginScreen(authService: authService, chatService: chatService),
    );
  }
}
