import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class FormsScreen extends StatelessWidget {
  const FormsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Đơn từ hành chính'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            const SectionHeader(title: 'Tạo đơn mới'),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: AppSpacing.md,
              crossAxisSpacing: AppSpacing.md,
              childAspectRatio: 1.5,
              children: [
                _FormAction(
                  label: 'Đăng ký xe bus',
                  icon: Icons.directions_bus_filled_outlined,
                  color: AppColors.blue,
                  onTap: () => _showMsg(context, 'Đăng ký xe bus trường học'),
                ),
                _FormAction(
                  label: 'Đăng ký bán trú',
                  icon: Icons.restaurant_outlined,
                  color: AppColors.green,
                  onTap: () => _showMsg(context, 'Đăng ký ăn bán trú tại trường'),
                ),
                _FormAction(
                  label: 'Rút hồ sơ học sinh',
                  icon: Icons.badge_outlined,
                  color: AppColors.danger,
                  onTap: () => _showMsg(context, 'Rút hồ sơ học sinh chuyển trường'),
                ),
                _FormAction(
                  label: 'Đơn từ biểu mẫu khác',
                  icon: Icons.edit_document,
                  color: AppColors.violet,
                  onTap: () => _showMsg(context, 'Mở danh sách biểu mẫu hành chính khác'),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.lg),
            const SectionHeader(title: 'Lịch sử đơn từ gửi đi'),
            _RequestCard(
              icon: Icons.directions_bus_filled_outlined,
              title: 'Đơn đăng ký xe đưa đón',
              time: 'Gửi lúc 08:15 - Hôm nay',
              detail: 'Chi tiết: Tuyến số 12 - Cầu Giấy (Chiều về)',
              status: 'Chờ duyệt',
              statusColor: AppColors.warning,
              statusBg: AppColors.warningSoft,
            ),
            const SizedBox(height: AppSpacing.sm),
            _RequestCard(
              icon: Icons.restaurant_outlined,
              title: 'Đơn đăng ký ăn bán trú',
              time: 'Gửi lúc 14:00 - 01/06/2026',
              detail: 'Chi tiết: Đăng ký ăn trưa kỳ học hè',
              status: 'Đã duyệt',
              statusColor: AppColors.success,
              statusBg: AppColors.successSoft,
            ),
          ],
        ),
      ),
    );
  }

  void _showMsg(BuildContext context, String formName) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Đã chọn: $formName'),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }
}

class _FormAction extends StatelessWidget {
  const _FormAction({
    required this.label,
    required this.icon,
    required this.color,
    required this.onTap,
  });

  final String label;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: AppSpacing.sm,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: color, size: 28),
            const SizedBox(height: AppSpacing.sm),
            Text(
              label,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.bold,
                color: AppColors.ink,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _RequestCard extends StatelessWidget {
  const _RequestCard({
    required this.icon,
    required this.title,
    required this.time,
    required this.detail,
    required this.status,
    required this.statusColor,
    required this.statusBg,
  });

  final IconData icon;
  final String title;
  final String time;
  final String detail;
  final String status;
  final Color statusColor;
  final Color statusBg;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CircleAvatar(
            backgroundColor: AppColors.primarySoft,
            child: Icon(icon, color: AppColors.fptOrange),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Text(
                        title,
                        style: const TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w900,
                          color: AppColors.ink,
                        ),
                      ),
                    ),
                    StatusPill(
                      label: status,
                      foreground: statusColor,
                      background: statusBg,
                      compact: true,
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  time,
                  style: const TextStyle(
                    fontSize: 11,
                    color: AppColors.muted,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Divider(height: AppSpacing.lg),
                Text(
                  detail,
                  style: const TextStyle(
                    fontSize: 12.5,
                    color: AppColors.ink,
                    fontWeight: FontWeight.w600,
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
