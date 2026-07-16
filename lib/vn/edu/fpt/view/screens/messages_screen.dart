import 'dart:async';

import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/search_result_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart' as domain;
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/chat_detail_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/login_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_profile_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_leave_requests_screen.dart';

class ConversationsScreen extends StatelessWidget {
  const ConversationsScreen({
    super.key,
    this.actor = AppActor.parent,
    required this.chatService,
  });

  final AppActor actor;
  final ChatService chatService;

  @override
  Widget build(BuildContext context) {
    return _ServiceConversationsScreen(chatService: chatService, actor: actor);
  }
}

class _ServiceConversationsScreen extends StatelessWidget {
  const _ServiceConversationsScreen({
    required this.chatService,
    this.actor = AppActor.parent,
  });

  final ChatService chatService;
  final AppActor actor;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: chatService,
      builder: (context, _) {
        final conversations = chatService.conversations;
        return Scaffold(
          backgroundColor: AppColors.background,
          appBar: const OrangeTopBar(title: 'Tin nhắn liên lạc'),
          body: SafeArea(
            child: RefreshIndicator(
              onRefresh: chatService.loadConversations,
              child: ListView(
                padding: const EdgeInsets.all(AppSpacing.lg),
                children: [
                  _InlineUserSearch(chatService: chatService),
                  const SizedBox(height: AppSpacing.lg),
                  const SectionHeader(title: 'Hộp thoại gần đây'),
                  if (chatService.isLoadingConversations)
                    const Padding(
                      padding: EdgeInsets.all(AppSpacing.lg),
                      child: Center(child: CircularProgressIndicator()),
                    )
                  else if (conversations.isEmpty)
                    const AppCard(
                      child: Center(child: Text('Chưa có cuộc hội thoại nào.')),
                    )
                  else
                    for (final conversation in conversations) ...[
                      _ConversationCard(
                        conversation: conversation,
                        chatService: chatService,
                      ),
                      const SizedBox(height: AppSpacing.sm),
                    ],
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _InlineUserSearch extends StatefulWidget {
  const _InlineUserSearch({required this.chatService});

  final ChatService chatService;

  @override
  State<_InlineUserSearch> createState() => _InlineUserSearchState();
}

class _InlineUserSearchState extends State<_InlineUserSearch> {
  final TextEditingController _controller = TextEditingController();
  Timer? _debounce;
  List<SearchResultDto> _results = const [];
  bool _isSearching = false;
  int? _openingUserId;
  String? _error;

  @override
  void dispose() {
    _debounce?.cancel();
    _controller.dispose();
    super.dispose();
  }

  void _onChanged(String value) {
    _debounce?.cancel();
    final keyword = value.trim();
    if (keyword.isEmpty) {
      setState(() {
        _results = const [];
        _isSearching = false;
        _error = null;
      });
      return;
    }
    setState(() {
      _results = const [];
      _isSearching = true;
      _error = null;
    });
    _debounce = Timer(
      const Duration(milliseconds: 350),
      () => _search(keyword),
    );
  }

  Future<void> _search(String keyword) async {
    setState(() {
      _isSearching = true;
      _error = null;
    });
    try {
      final results = await widget.chatService.searchUsers(keyword);
      if (!mounted || _controller.text.trim() != keyword) return;
      setState(() => _results = results);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _results = const [];
        _error = 'Không thể tìm kiếm tài khoản lúc này.';
      });
    } finally {
      if (mounted && _controller.text.trim() == keyword) {
        setState(() => _isSearching = false);
      }
    }
  }

  Future<void> _openChat(SearchResultDto user) async {
    setState(() => _openingUserId = user.id);
    try {
      final conversation = await widget.chatService.createConversation(
        otherUserId: user.id,
      );
      if (!mounted) return;
      widget.chatService.clearConversationUnread(conversation.id);
      _controller.clear();
      setState(() => _results = const []);
      await Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => ChatDetailScreen(
            conversation: conversation,
            chatService: widget.chatService,
          ),
        ),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể mở cuộc hội thoại.')),
      );
    } finally {
      if (mounted) setState(() => _openingUserId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final hasQuery = _controller.text.trim().isNotEmpty;
    return AppCard(
      child: Column(
        children: [
          TextField(
            key: const ValueKey('conversation-user-search'),
            controller: _controller,
            onChanged: _onChanged,
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(
              hintText: 'Tìm theo tên, số điện thoại, tên tài khoản',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: _isSearching
                  ? const Padding(
                      padding: EdgeInsets.all(12),
                      child: SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    )
                  : null,
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: AppSpacing.sm),
            Text(
              _error!,
              style: const TextStyle(color: AppColors.danger, fontSize: 12),
            ),
          ] else if (hasQuery && !_isSearching && _results.isEmpty) ...[
            const SizedBox(height: AppSpacing.sm),
            const Align(
              alignment: Alignment.centerLeft,
              child: Text(
                'Không tìm thấy tài khoản phù hợp.',
                style: TextStyle(color: AppColors.muted, fontSize: 12),
              ),
            ),
          ] else if (_results.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.sm),
            const Divider(height: 1),
            for (final user in _results)
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const CircleAvatar(child: Icon(Icons.person_outline)),
                title: Text(
                  user.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                subtitle: Text('${user.phone} • ${_roleLabel(user.role)}'),
                trailing: _openingUserId == user.id
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.chat_bubble_outline),
                enabled: _openingUserId == null,
                onTap: () => _openChat(user),
              ),
          ],
        ],
      ),
    );
  }

  String _roleLabel(String role) => switch (role) {
    'PARENT' => 'Phụ huynh',
    'STUDENT' => 'Học sinh',
    'TEACHER' => 'Giáo viên',
    _ => role,
  };
}

class _ConversationCard extends StatelessWidget {
  const _ConversationCard({
    required this.conversation,
    required this.chatService,
  });

  final domain.Conversation conversation;
  final ChatService chatService;

  @override
  Widget build(BuildContext context) {
    final participant = conversation.otherParticipant;
    final name = participant?.name ?? 'Hội thoại #${conversation.id}';
    return AppCard(
      padding: 0,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: () {
              chatService.clearConversationUnread(conversation.id);
              Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (_) => ChatDetailScreen(
                    conversation: conversation,
                    chatService: chatService,
                  ),
                ),
              );
            },
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                            color: AppColors.ink,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.xs),
                        Text(
                          conversation.lastMessage ?? 'Nhắn tin ngay',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 12.5,
                            color: conversation.unreadCount > 0
                                ? AppColors.ink
                                : AppColors.muted,
                            fontWeight: conversation.unreadCount > 0
                                ? FontWeight.w700
                                : FontWeight.normal,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (conversation.unreadCount > 0)
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: AppColors.fptOrange,
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Text(
                        '${conversation.unreadCount}',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  const SizedBox(width: AppSpacing.sm),
                  const Icon(
                    Icons.chevron_right,
                    color: AppColors.quiet,
                    size: 20,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class AnnouncementsScreen extends StatelessWidget {
  const AnnouncementsScreen({
    super.key,
    this.actor = AppActor.parent,
    required this.service,
    this.token,
  });

  final AppActor actor;
  final NotificationService service;
  final String? token;

  Future<void> _openNotification(
    BuildContext context,
    domain.AppNotification item,
  ) async {
    await service.markAsRead(item.id);
    if (!context.mounted) return;
    if (item.relatedType == 'ANNOUNCEMENT') {
      await showDialog<void>(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(item.title),
          content: SingleChildScrollView(child: Text(item.message)),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Đóng'),
            ),
          ],
        ),
      );
      return;
    }
    if (actor == AppActor.teacher &&
        item.relatedType == 'LEAVE_REQUEST' &&
        token != null) {
      await Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => TeacherLeaveRequestsScreen(
            token: token!,
            notificationService: service,
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: service,
      builder: (context, _) => Scaffold(
        backgroundColor: AppColors.background,
        appBar: OrangeTopBar(
          title: service.unreadCount == 0
              ? 'Trung tâm thông báo'
              : 'Thông báo (${service.unreadCount})',
          actions: [
            if (service.unreadCount > 0)
              TextButton(
                onPressed: service.markAllAsRead,
                child: const Text(
                  'Đọc tất cả',
                  style: TextStyle(color: Colors.white),
                ),
              ),
          ],
        ),
        body: SafeArea(
          child: RefreshIndicator(
            onRefresh: service.load,
            child: _notificationBody(),
          ),
        ),
      ),
    );
  }

  Widget _notificationBody() {
    if (service.isLoading && service.notifications.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: const [
          SizedBox(height: 180),
          Center(child: CircularProgressIndicator(color: AppColors.fptOrange)),
        ],
      );
    }
    if (service.errorMessage != null && service.notifications.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          const SizedBox(height: 100),
          const Icon(
            Icons.cloud_off_outlined,
            color: AppColors.danger,
            size: 44,
          ),
          const SizedBox(height: 12),
          Text(service.errorMessage!, textAlign: TextAlign.center),
        ],
      );
    }
    if (service.notifications.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: const [
          SizedBox(height: 100),
          Icon(Icons.notifications_none, color: AppColors.muted, size: 48),
          SizedBox(height: 12),
          Text('Chưa có thông báo nào.', textAlign: TextAlign.center),
        ],
      );
    }
    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(AppSpacing.lg),
      itemCount: service.notifications.length + 1,
      separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.sm),
      itemBuilder: (context, index) {
        if (index == 0) {
          return const Padding(
            padding: EdgeInsets.only(bottom: AppSpacing.xs),
            child: SectionHeader(title: 'Thông báo lớp học & nhà trường'),
          );
        }
        final item = service.notifications[index - 1];
        return _NotificationCard(
          item: item,
          onTap: () => _openNotification(context, item),
        );
      },
    );
  }
}

class _NotificationCard extends StatelessWidget {
  const _NotificationCard({required this.item, required this.onTap});

  final domain.AppNotification item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = item.tag == 'Thời khóa biểu'
        ? AppColors.fptOrange
        : AppColors.blue;
    return AppCard(
      backgroundColor: item.isRead
          ? AppColors.surface
          : color.withValues(alpha: 0.05),
      padding: 0,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(Icons.calendar_month_outlined, color: color),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            item.title,
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w800,
                              color: AppColors.ink,
                            ),
                          ),
                        ),
                        if (!item.isRead)
                          Container(
                            width: 8,
                            height: 8,
                            decoration: BoxDecoration(
                              color: color,
                              shape: BoxShape.circle,
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      item.message,
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: AppColors.muted,
                        height: 1.35,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '${item.tag} · ${_formatTime(item.createdAt)}',
                      style: TextStyle(
                        fontSize: 11,
                        color: color,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _formatTime(DateTime value) {
    final local = value.toLocal();
    return '${local.day.toString().padLeft(2, '0')}/${local.month.toString().padLeft(2, '0')}/${local.year} '
        '${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')}';
  }
}

class ContactsScreen extends StatelessWidget {
  const ContactsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Danh bạ liên hệ'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: const [
            SectionHeader(title: 'Văn phòng & Giáo viên'),
            _ContactCard(
              name: 'Cô Nguyễn Thu Hà',
              role: 'Giáo viên chủ nhiệm lớp 12A',
              phone: '0901 234 567',
            ),
            SizedBox(height: AppSpacing.sm),
            _ContactCard(
              name: 'Văn phòng FPT Schools',
              role: 'Hành chính học vụ',
              phone: '024 7300 5588',
            ),
            SizedBox(height: AppSpacing.sm),
            _ContactCard(
              name: 'Y tế học đường',
              role: 'Hỗ trợ sức khỏe học sinh',
              phone: '024 7300 5599',
            ),
          ],
        ),
      ),
    );
  }
}

class _ContactCard extends StatelessWidget {
  const _ContactCard({
    required this.name,
    required this.role,
    required this.phone,
  });

  final String name;
  final String role;
  final String phone;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: AppColors.ink,
                  ),
                ),
                Text(
                  role,
                  style: const TextStyle(
                    fontSize: 11.5,
                    color: AppColors.muted,
                  ),
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  phone,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.blue,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class AccountProfileScreen extends StatelessWidget {
  const AccountProfileScreen({
    super.key,
    this.actor = AppActor.parent,
    this.authService,
    this.chatService,
    this.authApiClient,
  });

  final AppActor actor;
  final AuthService? authService;
  final ChatService? chatService;
  final AuthApiClient? authApiClient;

  Future<void> _showChangePassword(BuildContext context, String token) async {
    final changed = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (_) => _ChangePasswordDialog(
        token: token,
        apiClient: authApiClient ?? AuthApiClient(backend: BackendApiClient()),
      ),
    );
    if (changed == true && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đổi mật khẩu thành công.'),
          behavior: SnackBarBehavior.floating,
          backgroundColor: AppColors.success,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final service = authService;
    if (service == null) return _buildContent(context);
    return ListenableBuilder(
      listenable: service,
      builder: (context, _) => _buildContent(context),
    );
  }

  Widget _buildContent(BuildContext context) {
    final session = authService?.currentSession;
    if (session == null) {
      return const Scaffold(
        backgroundColor: AppColors.background,
        appBar: OrangeTopBar(title: 'Tài khoản'),
        body: Center(child: Text('Không tìm thấy thông tin tài khoản')),
      );
    }

    final name = session.userName;
    final email = session.email?.trim().isNotEmpty == true
        ? session.email!
        : 'Chưa cập nhật';
    final phone = session.phone.trim().isNotEmpty
        ? session.phone
        : 'Chưa cập nhật';
    final initials = name
        .trim()
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .take(2)
        .map((part) => part[0].toUpperCase())
        .join();
    final subtitle = switch (actor) {
      AppActor.parent => 'Phụ huynh',
      AppActor.teacher => 'Giáo viên',
      AppActor.student => 'Học sinh',
    };

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Tài khoản'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            // Bento ID Card
            AppCard(
              padding: 0,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: Stack(
                  children: [
                    // Left primary-colored vertical accent bar
                    Positioned(
                      left: 0,
                      top: 0,
                      bottom: 0,
                      child: Container(width: 5, color: actor.color),
                    ),
                    // Decorative icon in background
                    Positioned(
                      right: -20,
                      bottom: -20,
                      child: Opacity(
                        opacity: 0.05,
                        child: Icon(
                          actor.icon,
                          size: 150,
                          color: AppColors.ink,
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.all(20),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            width: 88,
                            height: 88,
                            decoration: BoxDecoration(
                              gradient: LinearGradient(
                                colors: [
                                  actor.color,
                                  actor.color.withValues(alpha: 0.8),
                                ],
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                              ),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(color: Colors.white, width: 3),
                              boxShadow: [
                                BoxShadow(
                                  color: Colors.black.withValues(alpha: 0.08),
                                  blurRadius: 6,
                                  offset: const Offset(0, 2),
                                ),
                              ],
                            ),
                            child: Center(
                              child: Text(
                                initials.isEmpty ? '--' : initials,
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 22,
                                  fontWeight: FontWeight.w900,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: AppSpacing.md),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  name,
                                  style: const TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.bold,
                                    color: AppColors.ink,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  subtitle,
                                  style: TextStyle(
                                    fontSize: 12.5,
                                    color: actor.color,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.md),

            // Contact Info Card
            AppCard(
              padding: 16,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        Icons.contact_page_outlined,
                        color: actor.color,
                        size: 20,
                      ),
                      const SizedBox(width: 8),
                      const Text(
                        'Thông tin liên hệ',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: AppColors.muted,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Icon(Icons.email_outlined, color: actor.color, size: 18),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          email,
                          style: const TextStyle(
                            fontSize: 13.5,
                            color: AppColors.ink,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Icon(Icons.phone_outlined, color: actor.color, size: 18),
                      const SizedBox(width: 12),
                      Text(
                        phone,
                        style: const TextStyle(
                          fontSize: 13.5,
                          color: AppColors.ink,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                  const Divider(height: 24, thickness: 0.5),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Trạng thái tài khoản',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppColors.muted,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      StatusPill(
                        label: session.isActive
                            ? 'Đang hoạt động'
                            : session.status,
                        foreground: AppColors.success,
                        background: AppColors.successSoft,
                        compact: true,
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.md),

            if (actor == AppActor.parent) ...[
              const SectionHeader(title: 'Bạn là phụ huynh của'),
              _ParentChildrenCard(authService: authService!),
              const SizedBox(height: AppSpacing.md),
            ],

            // Settings & Preferences Card
            const SectionHeader(title: 'Cài đặt & Thiết lập'),
            AppCard(
              padding: 8,
              child: Column(
                children: [
                  _OptionRow(
                    icon: Icons.lock_outline,
                    title: 'Đổi mật khẩu',
                    subtitle: 'Cập nhật mật khẩu bảo mật tài khoản',
                    iconColor: AppColors.blue,
                    iconBgColor: AppColors.blueSoft,
                    onTap: () => _showChangePassword(context, session.token),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.lg),

            // Logout Action
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4),
              child: InkWell(
                onTap: () async {
                  await chatService?.stop();
                  authService?.logout();
                  if (!context.mounted) return;
                  Navigator.of(context, rootNavigator: true).pushAndRemoveUntil(
                    MaterialPageRoute<void>(
                      builder: (_) => LoginScreen(
                        authService: authService,
                        chatService: chatService,
                      ),
                    ),
                    (route) => false,
                  );
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Đã đăng xuất khỏi tài khoản!'),
                      behavior: SnackBarBehavior.floating,
                    ),
                  );
                },
                borderRadius: BorderRadius.circular(12),
                child: Container(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  decoration: BoxDecoration(
                    color: AppColors.dangerSoft,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.logout, color: AppColors.danger, size: 20),
                      SizedBox(width: 8),
                      Text(
                        'Đăng xuất khỏi ứng dụng',
                        style: TextStyle(
                          color: AppColors.danger,
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ChangePasswordDialog extends StatefulWidget {
  const _ChangePasswordDialog({required this.token, required this.apiClient});

  final String token;
  final AuthApiClient apiClient;

  @override
  State<_ChangePasswordDialog> createState() => _ChangePasswordDialogState();
}

class _ChangePasswordDialogState extends State<_ChangePasswordDialog> {
  final _formKey = GlobalKey<FormState>();
  final _oldPasswordController = TextEditingController();
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _submitting = false;
  String? _serverError;

  @override
  void dispose() {
    _oldPasswordController.dispose();
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() {
      _submitting = true;
      _serverError = null;
    });
    try {
      await widget.apiClient.changePassword(
        token: widget.token,
        oldPassword: _oldPasswordController.text,
        newPassword: _newPasswordController.text,
      );
      if (mounted) Navigator.pop(context, true);
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _serverError = error.toString().replaceFirst('Exception: ', '');
        _submitting = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      title: const Text(
        'Đổi mật khẩu',
        style: TextStyle(fontWeight: FontWeight.w900),
      ),
      content: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _oldPasswordController,
                obscureText: true,
                enabled: !_submitting,
                decoration: const InputDecoration(
                  labelText: 'Mật khẩu hiện tại',
                  prefixIcon: Icon(Icons.lock_outline),
                ),
                validator: (value) => value == null || value.isEmpty
                    ? 'Vui lòng nhập mật khẩu hiện tại'
                    : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _newPasswordController,
                obscureText: true,
                enabled: !_submitting,
                decoration: const InputDecoration(
                  labelText: 'Mật khẩu mới',
                  prefixIcon: Icon(Icons.password_outlined),
                ),
                validator: (value) {
                  if (value == null || value.length < 6) {
                    return 'Mật khẩu mới phải có ít nhất 6 ký tự';
                  }
                  if (value == _oldPasswordController.text) {
                    return 'Mật khẩu mới phải khác mật khẩu hiện tại';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _confirmPasswordController,
                obscureText: true,
                enabled: !_submitting,
                decoration: const InputDecoration(
                  labelText: 'Nhập lại mật khẩu mới',
                  prefixIcon: Icon(Icons.verified_user_outlined),
                ),
                validator: (value) => value != _newPasswordController.text
                    ? 'Mật khẩu nhập lại không khớp'
                    : null,
              ),
              if (_serverError != null) ...[
                const SizedBox(height: 12),
                Text(
                  _serverError!,
                  style: const TextStyle(color: AppColors.danger),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _submitting ? null : () => Navigator.pop(context, false),
          child: const Text('Hủy'),
        ),
        ElevatedButton(
          onPressed: _submitting ? null : _submit,
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.fptOrange,
            foregroundColor: Colors.white,
          ),
          child: _submitting
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.white,
                  ),
                )
              : const Text('Cập nhật'),
        ),
      ],
    );
  }
}

class _ParentChildrenCard extends StatelessWidget {
  const _ParentChildrenCard({required this.authService});

  final AuthService authService;

  StudentSnapshot _snapshot(domain.LinkedStudent child) {
    return StudentSnapshot.linked(
      id: child.id,
      name: child.name,
      studentCode: child.studentCode,
      className: child.className ?? 'Chưa xếp lớp',
      school: child.schoolName ?? 'FPT Schools',
      linkStatus: child.status == 'ACTIVE' ? 'Đang học' : child.status,
      homeroomTeacherName: child.homeroomTeacherName,
      homeroomTeacherPhone: child.homeroomTeacherPhone,
      dateOfBirth: child.dateOfBirth,
      gender: child.gender,
      address: child.address,
      email: child.email,
      academicYearName: child.academicYearName,
    );
  }

  @override
  Widget build(BuildContext context) {
    final children = authService.currentSession?.children ?? const [];
    if (children.isEmpty) {
      return const AppCard(
        child: Text(
          'Tài khoản chưa được liên kết với học sinh nào.',
          style: TextStyle(color: AppColors.muted),
        ),
      );
    }

    return AppCard(
      padding: 8,
      child: Column(
        children: List.generate(children.length, (index) {
          final child = children[index];
          final selected = index == authService.selectedChildIndex;
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 8,
                  vertical: 10,
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            child.name,
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              color: AppColors.ink,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            '${child.className ?? 'Chưa xếp lớp'}${selected ? ' · Đang quản lý' : ''}',
                            style: TextStyle(
                              color: selected
                                  ? AppColors.fptOrange
                                  : AppColors.muted,
                              fontSize: 11.5,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                    OutlinedButton(
                      onPressed: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) =>
                                StudentProfileScreen(student: _snapshot(child)),
                          ),
                        );
                      },
                      child: const Text('Xem thông tin'),
                    ),
                  ],
                ),
              ),
              if (index != children.length - 1) const Divider(height: 1),
            ],
          );
        }),
      ),
    );
  }
}

class _OptionRow extends StatelessWidget {
  const _OptionRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
    required this.iconColor,
    required this.iconBgColor,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final Color iconColor;
  final Color iconBgColor;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: iconBgColor,
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: iconColor, size: 20),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 13.5,
                      fontWeight: FontWeight.bold,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontSize: 11.5,
                      color: AppColors.muted,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: AppColors.quiet, size: 18),
          ],
        ),
      ),
    );
  }
}

class FileViewerScreen extends StatelessWidget {
  const FileViewerScreen({super.key, this.actor = AppActor.parent});

  final AppActor actor;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const OrangeTopBar(title: 'Tệp đính kèm'),
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            AppCard(
              child: Column(
                children: const [
                  Icon(
                    Icons.picture_as_pdf_outlined,
                    color: AppColors.fptOrange,
                    size: 48,
                  ),
                  SizedBox(height: AppSpacing.sm),
                  Text(
                    'lich-thi-cuoi-ky-ii.pdf',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                      color: AppColors.ink,
                    ),
                  ),
                  SizedBox(height: AppSpacing.xs),
                  Text(
                    'Kích thước: 1.2 MB • Tài liệu tham khảo',
                    style: TextStyle(fontSize: 12, color: AppColors.muted),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
