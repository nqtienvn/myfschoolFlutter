import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherStatsScreen extends StatelessWidget {
  const TeacherStatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Thống kê lớp học'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            const SectionHeader(title: 'Tổng quan lớp 12A'),
            AppCard(
              child: Column(
                children: [
                  _StatItem(label: 'Tỷ lệ chuyên cần trung bình', value: '94%', progress: 0.94, color: AppColors.green),
                  const Divider(height: AppSpacing.lg),
                  _StatItem(label: 'Điểm trung bình học tập', value: '7.8 / 10', progress: 0.78, color: AppColors.blue),
                  const Divider(height: AppSpacing.lg),
                  _StatItem(label: 'Tương tác phụ huynh (đã đọc)', value: '86%', progress: 0.86, color: AppColors.fptOrange),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatItem extends StatelessWidget {
  const _StatItem({
    required this.label,
    required this.value,
    required this.progress,
    required this.color,
  });

  final String label;
  final String value;
  final double progress;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              label,
              style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.bold, color: AppColors.ink),
            ),
            Text(
              value,
              style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: color),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.sm),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: progress,
            backgroundColor: color.withValues(alpha: 0.12),
            valueColor: AlwaysStoppedAnimation<Color>(color),
            minHeight: 6,
          ),
        ),
      ],
    );
  }
}

class _WarningItem extends StatelessWidget {
  const _WarningItem({
    required this.title,
    required this.detail,
    required this.color,
  });

  final String title;
  final String detail;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(Icons.warning_amber_rounded, color: color, size: 20),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: color),
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                detail,
                style: const TextStyle(fontSize: 12, color: AppColors.muted, height: 1.35),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
