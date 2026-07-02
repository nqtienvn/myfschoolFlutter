import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart' as domain;
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/chat_detail_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/login_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_inbox_screen.dart';

class ConversationsScreen extends StatelessWidget {
  const ConversationsScreen({super.key, this.actor = AppActor.parent, this.chatService});

  final AppActor actor;
  final ChatService? chatService;

  @override
  Widget build(BuildContext context) {
    if (actor == AppActor.teacher) {
      return const TeacherInboxScreen();
    }

    final service = chatService;
    if (service != null) {
      return _ServiceConversationsScreen(chatService: service);
    }

    final threads = <ChatThread>[
      const ChatThread(
        title: 'Cô Nguyễn Thu Hà',
        subtitle: 'Giáo viên chủ nhiệm • Lớp SE1913',
        lastMessage: 'Cô đã nhận đơn nghỉ, gia đình theo dõi sức khỏe của An nhé.',
        time: '09:20',
        accentColor: AppColors.fptOrange,
        tag: 'GV chủ nhiệm',
        initialMessages: [
          ChatMessage(text: 'Chào gia đình, cô đã nhận đơn nghỉ của An.', time: '09:12', isMine: false),
          ChatMessage(text: 'Dạ gia đình cảm ơn cô đã hỗ trợ.', time: '09:16', isMine: true),
          ChatMessage(text: 'Cô đã nhận đơn nghỉ, gia đình theo dõi sức khỏe của An nhé.', time: '09:20', isMine: false),
        ],
      ),
      const ChatThread(
        title: 'Thầy Trần Quốc Huy',
        subtitle: 'Giáo viên Tin học • PRM393',
        lastMessage: 'An cần nộp lại ảnh chụp màn hình bài thực hành.',
        time: 'Hôm qua',
        accentColor: AppColors.blue,
        tag: 'Bài tập',
        initialMessages: [
          ChatMessage(text: 'Thầy ơi, em đã nộp bài Lab 2 trên hệ thống.', time: 'Hôm qua', isMine: true),
          ChatMessage(text: 'An cần nộp lại ảnh chụp màn hình bài thực hành.', time: 'Hôm qua', isMine: false),
        ],
      ),
    ];

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Tin nhắn liên lạc'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            const SectionHeader(title: 'Hộp thoại gần đây'),
            for (final thread in threads) ...[
              AppCard(
                padding: 0,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(16),
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (context) => ChatDetailScreen(thread: thread),
                          ),
                        );
                      },
                      child: Padding(
                        padding: const EdgeInsets.all(AppSpacing.lg),
                        child: Row(
                          children: [
                            CircleAvatar(
                              backgroundColor: thread.accentColor.withValues(alpha: 0.12),
                              child: Icon(Icons.person, color: thread.accentColor),
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
                                          thread.title,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: const TextStyle(
                                            fontSize: 14,
                                            fontWeight: FontWeight.bold,
                                            color: AppColors.ink,
                                          ),
                                        ),
                                      ),
                                      const SizedBox(width: AppSpacing.sm),
                                      Text(
                                        thread.time,
                                        style: const TextStyle(fontSize: 10, color: AppColors.quiet),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: AppSpacing.xs),
                                  Text(
                                    thread.lastMessage,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(fontSize: 12.5, color: AppColors.muted),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: AppSpacing.sm),
                            const Icon(Icons.chevron_right, color: AppColors.quiet, size: 20),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.sm),
            ],
          ],
        ),
      ),
    );
  }
}

class _ServiceConversationsScreen extends StatelessWidget {
  const _ServiceConversationsScreen({required this.chatService});

  final ChatService chatService;

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
                      _ConversationCard(conversation: conversation, chatService: chatService),
                      const SizedBox(height: AppSpacing.sm),
                    ],
                  if (chatService.errorMessage != null) ...[
                    const SizedBox(height: AppSpacing.sm),
                    Text(
                      chatService.errorMessage!,
                      style: const TextStyle(color: AppColors.danger, fontWeight: FontWeight.w600),
                    ),
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

class _ConversationCard extends StatelessWidget {
  const _ConversationCard({required this.conversation, required this.chatService});

  final domain.Conversation conversation;
  final ChatService chatService;

  @override
  Widget build(BuildContext context) {
    final participant = conversation.otherParticipant;
    final name = participant?.name ?? 'Hội thoại #${conversation.id}';
    final subtitle = participant?.role ?? 'Tin nhắn';
    return AppCard(
      padding: 0,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (_) => ChatDetailScreen(conversation: conversation, chatService: chatService),
                ),
              );
            },
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: Row(
                children: [
                  Stack(
                    children: [
                      CircleAvatar(
                        backgroundColor: AppColors.fptOrange.withValues(alpha: 0.12),
                        child: const Icon(Icons.person, color: AppColors.fptOrange),
                      ),
                      if (conversation.isOnline)
                        Positioned(
                          right: 0,
                          bottom: 0,
                          child: Container(
                            width: 10,
                            height: 10,
                            decoration: BoxDecoration(
                              color: AppColors.success,
                              shape: BoxShape.circle,
                              border: Border.all(color: AppColors.surface, width: 2),
                            ),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.ink)),
                        const SizedBox(height: AppSpacing.xs),
                        Text(conversation.lastMessage ?? subtitle, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12.5, color: AppColors.muted)),
                      ],
                    ),
                  ),
                  if (conversation.unreadCount > 0)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: const BoxDecoration(color: AppColors.fptOrange, shape: BoxShape.circle),
                      child: Text('${conversation.unreadCount}', style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold)),
                    ),
                  const SizedBox(width: AppSpacing.sm),
                  const Icon(Icons.chevron_right, color: AppColors.quiet, size: 20),
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
  const AnnouncementsScreen({super.key, this.actor = AppActor.parent});

  final AppActor actor;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Trung tâm thông báo'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            const SectionHeader(title: 'Thông báo lớp học & nhà trường'),
            _AnnouncementCard(
              title: 'Lịch thi cuối kỳ II sắp tới',
              body: 'Nhà trường công bố lịch thi, phòng thi và danh sách giám thị theo lớp học.',
              tag: 'Quan trọng',
              color: AppColors.fptOrange,
            ),
            const SizedBox(height: AppSpacing.sm),
            _AnnouncementCard(
              title: 'Yêu cầu xác nhận thông tin bán trú',
              body: 'Vui lòng kiểm tra dữ liệu đăng ký bán trú trước ngày 20/06/2026.',
              tag: 'Cần phản hồi',
              color: AppColors.warning,
            ),
            const SizedBox(height: AppSpacing.sm),
            _AnnouncementCard(
              title: 'Cập nhật hệ thống sổ liên lạc',
              body: 'Hệ thống bổ sung chatbot AI thống kê và theo dõi trạng thái đọc thông báo.',
              tag: 'Tin hệ thống',
              color: AppColors.blue,
            ),
          ],
        ),
      ),
    );
  }
}

class _AnnouncementCard extends StatelessWidget {
  const _AnnouncementCard({
    required this.title,
    required this.body,
    required this.tag,
    required this.color,
  });

  final String title;
  final String body;
  final String tag;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.ink),
                ),
              ),
              StatusPill(
                label: tag,
                foreground: color,
                background: color.withValues(alpha: 0.12),
                compact: true,
              ),
            ],
          ),
          const Divider(height: AppSpacing.lg),
          Text(
            body,
            style: const TextStyle(fontSize: 12.5, color: AppColors.muted, height: 1.35),
          ),
        ],
      ),
    );
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
            _ContactCard(name: 'Cô Nguyễn Thu Hà', role: 'Giáo viên chủ nhiệm lớp 12A', phone: '0901 234 567'),
            SizedBox(height: AppSpacing.sm),
            _ContactCard(name: 'Văn phòng FPT Schools', role: 'Hành chính học vụ', phone: '024 7300 5588'),
            SizedBox(height: AppSpacing.sm),
            _ContactCard(name: 'Y tế học đường', role: 'Hỗ trợ sức khỏe học sinh', phone: '024 7300 5599'),
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
          CircleAvatar(
            backgroundColor: AppColors.primarySoft,
            child: const Icon(Icons.person, color: AppColors.fptOrange),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.ink),
                ),
                Text(
                  role,
                  style: const TextStyle(fontSize: 11.5, color: AppColors.muted),
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  phone,
                  style: const TextStyle(fontSize: 12, color: AppColors.blue, fontWeight: FontWeight.bold),
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
  const AccountProfileScreen({super.key, this.actor = AppActor.parent, this.authService, this.chatService});

  final AppActor actor;
  final AuthService? authService;
  final ChatService? chatService;

  @override
  Widget build(BuildContext context) {
    // Dynamically retrieve info based on actor
    final String name;
    final String subtitle;
    final String idLabel;
    final String idValue;
    final String deptLabel;
    final String deptValue;
    final String email;
    final String phone;

    switch (actor) {
      case AppActor.parent:
        name = 'Nguyễn Minh Anh';
        subtitle = 'Phụ huynh của Nguyễn Minh An (12A1)';
        idLabel = 'MÃ PHỤ HUYNH';
        idValue = 'PH-1201';
        deptLabel = 'HỌC KỲ';
        deptValue = 'Fall 2026';
        email = 'parent.an@fpt.edu.vn';
        phone = '0987 654 321';
        break;
      case AppActor.teacher:
        name = 'Cô Nguyễn Thu Hà';
        subtitle = 'Giáo viên chủ nhiệm lớp 12A1';
        idLabel = 'MÃ GIÁO VIÊN';
        idValue = 'GV-1201';
        deptLabel = 'BỘ MÔN';
        deptValue = 'Tin học (CNTT)';
        email = 'ha.nt@fpt.edu.vn';
        phone = '0901 234 567';
        break;
      case AppActor.student:
        name = 'Nguyễn Minh An';
        subtitle = 'Học sinh lớp 12A1';
        idLabel = 'MÃ HỌC SINH';
        idValue = 'MFS-1201';
        deptLabel = 'LỚP';
        deptValue = '12A1';
        email = 'an.nm@fpt.edu.vn';
        phone = '0901 234 567';
        break;
    }

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
                      child: Container(
                        width: 5,
                        color: actor.color,
                      ),
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
                                )
                              ],
                            ),
                            child: Center(
                              child: Text(
                                actor == AppActor.parent ? 'AN' : (actor == AppActor.teacher ? 'TH' : 'MA'),
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
                                const SizedBox(height: 16),
                                Row(
                                  children: [
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            idLabel,
                                            style: const TextStyle(
                                              fontSize: 9,
                                              fontWeight: FontWeight.bold,
                                              color: AppColors.muted,
                                              letterSpacing: 0.5,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            idValue,
                                            style: const TextStyle(
                                              fontSize: 13,
                                              fontWeight: FontWeight.bold,
                                              color: AppColors.ink,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            deptLabel,
                                            style: const TextStyle(
                                              fontSize: 9,
                                              fontWeight: FontWeight.bold,
                                              color: AppColors.muted,
                                              letterSpacing: 0.5,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            deptValue,
                                            style: const TextStyle(
                                              fontSize: 13,
                                              fontWeight: FontWeight.bold,
                                              color: AppColors.ink,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
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
                      Icon(Icons.contact_page_outlined, color: actor.color, size: 20),
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
                      CircleAvatar(
                        radius: 14,
                        backgroundColor: AppColors.line.withValues(alpha: 0.5),
                        child: Icon(Icons.email_outlined, color: actor.color, size: 14),
                      ),
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
                      CircleAvatar(
                        radius: 14,
                        backgroundColor: AppColors.line.withValues(alpha: 0.5),
                        child: Icon(Icons.phone_outlined, color: actor.color, size: 14),
                      ),
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
                        label: 'Đang hoạt động',
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
                    onTap: () {},
                  ),
                  const Divider(height: 8, thickness: 0.5, indent: 56),
                  _OptionRow(
                    icon: Icons.language,
                    title: 'Ngôn ngữ hiển thị',
                    subtitle: 'Tiếng Việt / English',
                    iconColor: AppColors.fptOrange,
                    iconBgColor: AppColors.primarySoft,
                    onTap: () {},
                  ),
                  const Divider(height: 8, thickness: 0.5, indent: 56),
                  _OptionRow(
                    icon: Icons.palette_outlined,
                    title: 'Giao diện hiển thị',
                    subtitle: 'Chế độ sáng đang hoạt động',
                    iconColor: AppColors.teal,
                    iconBgColor: AppColors.tealSoft,
                    onTap: () {},
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.lg),

            // Logout Action
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4),
              child: InkWell(
                onTap: () {
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
                  Icon(Icons.picture_as_pdf_outlined, color: AppColors.fptOrange, size: 48),
                  SizedBox(height: AppSpacing.sm),
                  Text(
                    'lich-thi-cuoi-ky-ii.pdf',
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.ink),
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
