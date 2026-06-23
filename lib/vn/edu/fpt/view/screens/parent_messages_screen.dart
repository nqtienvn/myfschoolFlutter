import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

class ParentMessagesScreen extends StatelessWidget {
  const ParentMessagesScreen({super.key, required this.student});

  final StudentSnapshot student;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Liên lạc giáo viên'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            const SectionHeader(title: 'Hộp thoại trao đổi'),
            if (student.teacherMessages.isEmpty)
              const AppCard(
                child: Center(
                  child: Text(
                    'Chưa có cuộc hội thoại nào.',
                    style: TextStyle(color: AppColors.muted),
                  ),
                ),
              )
            else
              for (final msg in student.teacherMessages) ...[
                _TeacherMessageTile(msg: msg),
                const SizedBox(height: AppSpacing.sm),
              ],
          ],
        ),
      ),
    );
  }
}

class _TeacherMessageTile extends StatelessWidget {
  const _TeacherMessageTile({required this.msg});

  final TeacherMessage msg;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        // Simple mock chat detail popup
        showDialog<void>(
          context: context,
          builder: (context) {
            return AlertDialog(
              title: Row(
                children: [
                  CircleAvatar(
                    backgroundColor: msg.color.withValues(alpha: 0.12),
                    child: Icon(Icons.person, color: msg.color),
                  ),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          msg.teacher,
                          style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w900),
                        ),
                        Text(
                          msg.role,
                          style: const TextStyle(fontSize: 11, color: AppColors.muted),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Tin nhắn cuối:',
                    style: TextStyle(fontSize: 11, color: AppColors.muted, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(AppSpacing.md),
                    decoration: BoxDecoration(
                      color: AppColors.background,
                      borderRadius: BorderRadius.circular(AppRadius.md),
                    ),
                    child: Text(
                      msg.preview,
                      style: const TextStyle(fontSize: 13, height: 1.3),
                    ),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  TextField(
                    decoration: InputDecoration(
                      labelText: 'Trả lời nhanh giáo viên',
                      hintText: 'Nhập câu trả lời...',
                      suffixIcon: IconButton(
                        icon: const Icon(Icons.send, color: AppColors.fptOrange),
                        onPressed: () {
                          Navigator.pop(context);
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Đã gửi phản hồi cho giáo viên!'),
                              behavior: SnackBarBehavior.floating,
                            ),
                          );
                        },
                      ),
                    ),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Đóng'),
                ),
              ],
            );
          },
        );
      },
      borderRadius: BorderRadius.circular(AppRadius.md),
      child: AppCard(
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: msg.color.withValues(alpha: 0.12),
              child: Icon(Icons.person, color: msg.color),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        msg.teacher,
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w900,
                          color: AppColors.ink,
                        ),
                      ),
                      Text(
                        msg.time,
                        style: const TextStyle(
                          fontSize: 10,
                          color: AppColors.quiet,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    msg.role,
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppColors.muted,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    msg.preview,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 12,
                      color: AppColors.ink,
                      fontWeight: msg.unread ? FontWeight.w800 : FontWeight.w500,
                    ),
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
