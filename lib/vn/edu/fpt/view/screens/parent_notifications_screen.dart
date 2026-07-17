import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

class ParentNotificationsScreen extends StatelessWidget {
  const ParentNotificationsScreen({super.key, required this.student});

  final StudentSnapshot student;

  @override
  Widget build(BuildContext context) {
    final unpaidSum = student.tuitionBills
        .where((bill) => bill.status == 'Chưa đóng')
        .fold(0, (sum, bill) => sum + bill.amount);

    final filteredNotifications = unpaidSum > 0
        ? student.notifications
        : student.notifications
              .where(
                (n) =>
                    n.tag != 'Học phí' &&
                    !n.title.toLowerCase().contains('học phí'),
              )
              .toList();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Thông báo'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            const SectionHeader(title: 'Thông báo mới nhận'),
            if (filteredNotifications.isEmpty)
              const AppCard(
                child: Center(
                  child: Text(
                    'Không có thông báo mới.',
                    style: TextStyle(color: AppColors.muted),
                  ),
                ),
              )
            else
              for (final notification in filteredNotifications) ...[
                _NotificationTile(notification: notification),
                const SizedBox(height: AppSpacing.sm),
              ],
          ],
        ),
      ),
    );
  }
}

class _NotificationTile extends StatelessWidget {
  const _NotificationTile({required this.notification});

  final ParentNotification notification;

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
                  notification.title,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w900,
                    color: AppColors.ink,
                  ),
                ),
              ),
              StatusPill(
                label: notification.tag,
                foreground: notification.color,
                background: notification.color.withValues(alpha: 0.12),
                compact: true,
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(
            notification.body,
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.ink,
              height: 1.35,
            ),
          ),
        ],
      ),
    );
  }
}
