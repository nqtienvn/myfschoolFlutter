import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class ClubsScreen extends StatelessWidget {
  const ClubsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Đăng ký Câu lạc bộ'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: const [
            SectionHeader(title: 'Câu lạc bộ đang tham gia'),
            _ClubCard(
              icon: Icons.sports_soccer_outlined,
              iconColor: AppColors.green,
              title: 'CLB Bóng đá (FPT Junior Football)',
              schedule: 'Thứ 3 & Thứ 5 (17:00 - 18:30)',
              status: 'Đã tham gia',
              statusColor: AppColors.green,
            ),
            SizedBox(height: AppSpacing.lg),
            SectionHeader(title: 'Khám phá các CLB mới'),
            _ClubCard(
              icon: Icons.translate_outlined,
              iconColor: AppColors.blue,
              title: 'CLB Tiếng Anh giao tiếp',
              schedule: 'Thứ 4 & Thứ 6 (16:30 - 18:00)',
              status: 'Còn 6 chỗ trống',
              statusColor: AppColors.blue,
            ),
            SizedBox(height: AppSpacing.sm),
            _ClubCard(
              icon: Icons.code,
              iconColor: AppColors.violet,
              title: 'CLB Lập trình Robot STEM',
              schedule: 'Thứ 7 (08:30 - 11:30)',
              status: 'Còn 3 chỗ trống',
              statusColor: AppColors.violet,
            ),
          ],
        ),
      ),
    );
  }
}

class _ClubCard extends StatelessWidget {
  const _ClubCard({
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.schedule,
    required this.status,
    required this.statusColor,
  });

  final IconData icon;
  final Color iconColor;
  final String title;
  final String schedule;
  final String status;
  final Color statusColor;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(AppSpacing.md),
            decoration: BoxDecoration(
              color: iconColor.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, color: iconColor, size: 28),
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
                    fontWeight: FontWeight.w900,
                    color: AppColors.ink,
                  ),
                ),
                const SizedBox(height: AppSpacing.xs),
                Row(
                  children: [
                    const Icon(Icons.access_time_filled, size: 14, color: AppColors.muted),
                    const SizedBox(width: 4),
                    Expanded(
                      child: Text(
                        schedule,
                        style: const TextStyle(fontSize: 12, color: AppColors.muted),
                      ),
                    ),
                  ],
                ),
                const Divider(height: AppSpacing.lg),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    StatusPill(
                      label: status,
                      foreground: statusColor,
                      background: statusColor.withValues(alpha: 0.12),
                      compact: true,
                    ),
                    TextButton(
                      onPressed: () {},
                      style: TextButton.styleFrom(
                        visualDensity: VisualDensity.compact,
                        padding: EdgeInsets.zero,
                      ),
                      child: const Text('Xem chi tiết', style: TextStyle(color: AppColors.fptOrange, fontWeight: FontWeight.bold, fontSize: 12)),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
