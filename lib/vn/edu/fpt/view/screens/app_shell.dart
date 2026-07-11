import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/auth_session.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/home_screen_giaovien.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/home_screen_phuhuynh.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/home_screen_hocsinh.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/messages_screen.dart';

class AppShell extends StatefulWidget {
  const AppShell({
    super.key,
    this.actor = AppActor.parent,
    this.authService,
    this.chatService,
    this.session,
  });

  final AppActor actor;
  final AuthService? authService;
  final ChatService? chatService;
  final AuthSession? session;

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int _selectedIndex = 0;
  late final List<GlobalKey<NavigatorState>> _navigatorKeys;
  NotificationService? _notificationService;

  @override
  void initState() {
    super.initState();
    _navigatorKeys = List.generate(4, (_) => GlobalKey<NavigatorState>());
    final session =
        widget.session ??
        widget.authService?.currentSession ??
        widget.chatService?.session;
    final chatService = widget.chatService;
    if (session != null && chatService != null) {
      _notificationService = NotificationService(
        apiClient: NotificationApiClient(backend: BackendApiClient()),
        socketEvents: chatService.socketEvents,
        token: session.token,
      );
      unawaited(_notificationService!.start());
    }
  }

  @override
  void dispose() {
    _notificationService?.dispose();
    super.dispose();
  }

  Widget _homeForActor() {
    switch (widget.actor) {
      case AppActor.parent:
        return HomeParent(authService: widget.authService!);
      case AppActor.teacher:
        return HomeTeacher(authService: widget.authService!);
      case AppActor.student:
        return HomeStudent(authService: widget.authService!);
    }
  }

  Widget _buildTabNavigator(int index) {
    return Navigator(
      key: _navigatorKeys[index],
      onGenerateInitialRoutes: (navigator, initialRoute) {
        return [
          MaterialPageRoute<void>(
            builder: (context) {
              switch (index) {
                case 0:
                  return _homeForActor();
                case 1:
                  return ConversationsScreen(
                    actor: widget.actor,
                    chatService: widget.chatService!,
                  );
                case 2:
                  return AnnouncementsScreen(
                    actor: widget.actor,
                    service: _notificationService!,
                  );
                case 3:
                  return AccountProfileScreen(
                    actor: widget.actor,
                    authService: widget.authService,
                    chatService: widget.chatService,
                  );
                default:
                  return const Scaffold();
              }
            },
          ),
        ];
      },
    );
  }

  void _selectTab(int index) {
    if (_selectedIndex == index) {
      _navigatorKeys[index].currentState?.popUntil((route) => route.isFirst);
    } else {
      setState(() => _selectedIndex = index);
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) return;
        final navigator = _navigatorKeys[_selectedIndex].currentState;
        if (navigator != null && navigator.canPop()) {
          navigator.pop();
        } else {
          final rootNavigator = Navigator.of(context);
          if (rootNavigator.canPop()) {
            rootNavigator.pop(result);
          } else {
            SystemNavigator.pop();
          }
        }
      },
      child: Scaffold(
        body: IndexedStack(
          index: _selectedIndex,
          children: List.generate(4, (index) => _buildTabNavigator(index)),
        ),
        bottomNavigationBar: SafeArea(
          top: false,
          child: BottomNavigationBar(
            currentIndex: _selectedIndex,
            onTap: _selectTab,
            items: const [
              BottomNavigationBarItem(
                icon: Icon(Icons.home_outlined),
                activeIcon: Icon(Icons.home),
                label: 'Trang chủ',
              ),
              BottomNavigationBarItem(
                icon: Icon(Icons.chat_bubble_outline),
                activeIcon: Icon(Icons.chat_bubble),
                label: 'Tin nhắn',
              ),
              BottomNavigationBarItem(
                icon: Icon(Icons.mail_outline),
                activeIcon: Icon(Icons.mail),
                label: 'Thông báo',
              ),
              BottomNavigationBarItem(
                icon: Icon(Icons.account_circle_outlined),
                activeIcon: Icon(Icons.account_circle),
                label: 'Tài khoản',
              ),
            ],
          ),
        ),
      ),
    );
  }
}
