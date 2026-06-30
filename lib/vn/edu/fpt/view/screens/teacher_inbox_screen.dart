import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/chat_detail_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherInboxScreen extends StatelessWidget {
  const TeacherInboxScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final threads = [
      _InboxThread(
        parent: 'PH Trần Hoàng Nam',
        student: 'Trần Hoàng Nam - 12A',
        message: 'Gia đình đã gửi giấy hẹn khám, nhờ cô kiểm tra đơn nghỉ.',
        time: '09:20',
        tag: 'Đơn nghỉ',
        color: AppColors.warning,
      ),
      _InboxThread(
        parent: 'PH Nguyễn Minh An',
        student: 'Nguyễn Minh An - SE1913',
        message: 'Em An hỏi lại tiêu chí chấm Lab 2 môn PRM393.',
        time: 'Hôm qua',
        tag: 'Điểm',
        color: AppColors.blue,
      ),
      _InboxThread(
        parent: 'PH Lê Bảo Châu',
        student: 'Lê Bảo Châu - 12A',
        message: 'Phụ huynh đã đọc thông báo họp lớp và xác nhận tham dự.',
        time: '12/06',
        tag: 'Thông báo',
        color: AppColors.green,
      ),
    ];

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Tin nhắn phụ huynh'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            const SectionHeader(title: 'Luồng tin nhắn cần phản hồi'),
            const SizedBox(height: AppSpacing.xs),
            for (final thread in threads) ...[
              _ThreadCard(thread: thread),
              const SizedBox(height: AppSpacing.sm),
            ],
          ],
        ),
      ),
    );
  }
}

class _InboxThread {
  const _InboxThread({
    required this.parent,
    required this.student,
    required this.message,
    required this.time,
    required this.tag,
    required this.color,
  });

  final String parent;
  final String student;
  final String message;
  final String time;
  final String tag;
  final Color color;
}

class _ThreadCard extends StatelessWidget {
  const _ThreadCard({required this.thread});

  final _InboxThread thread;

  ChatThread _toChatThread() {
    return ChatThread(
      title: thread.parent,
      subtitle: 'Học sinh: ${thread.student}',
      lastMessage: thread.message,
      time: thread.time,
      accentColor: thread.color,
      tag: thread.tag,
      initialMessages: [
        ChatMessage(text: thread.message, time: thread.time, isMine: false),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
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
                  builder: (context) => ChatDetailScreen(thread: _toChatThread()),
                ),
              );
            },
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          thread.parent,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: AppColors.ink),
                        ),
                      ),
                      const SizedBox(width: AppSpacing.sm),
                      StatusPill(
                        label: thread.tag,
                        foreground: thread.color,
                        background: thread.color.withValues(alpha: 0.12),
                        compact: true,
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    'Học sinh: ${thread.student} • ${thread.time}',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 11, color: AppColors.muted, fontWeight: FontWeight.bold),
                  ),
                  const Divider(height: AppSpacing.lg),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Text(
                          thread.message,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 13, color: AppColors.ink, height: 1.3),
                        ),
                      ),
                      const SizedBox(width: AppSpacing.sm),
                      const Icon(Icons.chevron_right, color: AppColors.quiet, size: 20),
                    ],
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
