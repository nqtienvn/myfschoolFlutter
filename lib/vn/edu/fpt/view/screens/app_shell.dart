import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/auth_session.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/home_screen_giaovien.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/home_screen_phuhuynh.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/home_screen_hocsinh.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/messages_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/announcement_inbox_screen.dart';

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
  AnnouncementInboxService? _announcementInboxService;
  AcademicPeriodController? _academicPeriodController;

  @override
  void initState() {
    super.initState();
    _navigatorKeys = List.generate(4, (_) => GlobalKey<NavigatorState>());
    final session =
        widget.session ??
        widget.authService?.currentSession ??
        widget.chatService?.session;
    final chatService = widget.chatService;
    if (session != null) {
      _academicPeriodController = AcademicPeriodController(
        token: session.token,
        studentId: session.actor == AppActor.parent
            ? widget.authService?.selectedChild?.id
            : null,
      );
      unawaited(_academicPeriodController!.load());
      widget.authService?.addListener(_syncAcademicPeriodStudent);
      _announcementInboxService = AnnouncementInboxService(
        api: AnnouncementApiClient(backend: BackendApiClient()),
        token: session.token,
        teacher: session.actor == AppActor.teacher,
      );
      unawaited(_announcementInboxService!.start());
      _notificationService = NotificationService(
        apiClient: NotificationApiClient(backend: BackendApiClient()),
        socketEvents: chatService?.socketEvents ?? const Stream.empty(),
        token: session.token,
      );
      unawaited(_notificationService!.start());
    }
  }

  @override
  void dispose() {
    widget.authService?.removeListener(_syncAcademicPeriodStudent);
    _notificationService?.dispose();
    _announcementInboxService?.dispose();
    _academicPeriodController?.dispose();
    super.dispose();
  }

  void _syncAcademicPeriodStudent() {
    final session = widget.authService?.currentSession;
    if (session == null || session.actor != AppActor.parent) return;
    unawaited(
      _academicPeriodController?.setStudentId(
        widget.authService?.selectedChild?.id,
      ),
    );
  }

  Widget _homeForActor() {
    switch (widget.actor) {
      case AppActor.parent:
        return HomeParent(
          authService: widget.authService!,
          notificationService: _notificationService,
        );
      case AppActor.teacher:
        return HomeTeacher(
          authService: widget.authService!,
          notificationService: _notificationService,
        );
      case AppActor.student:
        return HomeStudent(
          authService: widget.authService!,
          notificationService: _notificationService,
        );
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
                  return AnnouncementInboxScreen(
                    service: _announcementInboxService!,
                    notificationService: _notificationService,
                    token:
                        (widget.session ??
                                widget.authService?.currentSession ??
                                widget.chatService?.session)
                            ?.token,
                    authService: widget.authService,
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
    if (index == 2) {
      unawaited(_announcementInboxService?.load());
      unawaited(_notificationService?.load());
    }
    if (_selectedIndex == index) {
      _navigatorKeys[index].currentState?.popUntil((route) => route.isFirst);
    } else {
      setState(() => _selectedIndex = index);
    }
  }

  @override
  Widget build(BuildContext context) {
    final shell = PopScope(
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
        bottomNavigationBar: DecoratedBox(
          decoration: const BoxDecoration(
            color: AppColors.surface,
            border: Border(top: BorderSide(color: AppColors.line)),
          ),
          child: SafeArea(
            top: false,
            maintainBottomViewPadding: true,
            child: AnimatedBuilder(
              animation: Listenable.merge([
                ?_notificationService,
                ?_announcementInboxService,
                ?widget.chatService,
              ]),
              builder: (context, _) => BottomNavigationBar(
                currentIndex: _selectedIndex,
                onTap: _selectTab,
                items: [
                  BottomNavigationBarItem(
                    icon: Icon(Icons.home_outlined),
                    activeIcon: Icon(Icons.home),
                    label: 'Trang chủ',
                  ),
                  BottomNavigationBarItem(
                    icon: _chatIcon(Icons.chat_bubble_outline),
                    activeIcon: _chatIcon(Icons.chat_bubble),
                    label: 'Tin nhắn',
                  ),
                  BottomNavigationBarItem(
                    icon: _notificationIcon(Icons.mail_outline),
                    activeIcon: _notificationIcon(Icons.mail),
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
        ),
      ),
    );
    final periodController = _academicPeriodController;
    return periodController == null
        ? shell
        : AcademicPeriodScope(controller: periodController, child: shell);
  }

  Widget _notificationIcon(IconData icon) {
    final gradeUnread =
        _notificationService?.notifications
            .where(
              (item) => item.relatedType == 'GRADE_PUBLISHED' && !item.isRead,
            )
            .length ??
        0;
    final unread = (_announcementInboxService?.unreadCount ?? 0) + gradeUnread;
    return SizedBox(
      width: 32,
      height: 28,
      child: Stack(
        clipBehavior: Clip.none,
        alignment: Alignment.center,
        children: [
          Icon(icon),
          if (unread > 0)
            Positioned(
              right: -5,
              top: -6,
              child: _countBadge(unread, Colors.red),
            ),
        ],
      ),
    );
  }

  Widget _countBadge(int count, Color color) => Container(
    key: ValueKey('announcement-badge-$color'),
    constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
    padding: const EdgeInsets.symmetric(horizontal: 4),
    decoration: BoxDecoration(
      color: color,
      borderRadius: BorderRadius.circular(9),
    ),
    alignment: Alignment.center,
    child: Text(
      count > 99 ? '99+' : '$count',
      style: const TextStyle(
        color: Colors.white,
        fontSize: 9,
        fontWeight: FontWeight.w800,
      ),
    ),
  );

  Widget _chatIcon(IconData icon) {
    final count = widget.chatService?.totalUnreadCount ?? 0;
    return Badge(
      isLabelVisible: count > 0,
      label: Text(count > 99 ? '99+' : '$count'),
      child: Icon(icon),
    );
  }
}
