import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class AssignedClassesScreen extends StatelessWidget {
  const AssignedClassesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Lớp được phân công'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            const SectionHeader(title: 'Lớp chủ nhiệm'),
            const _ClassCard(
              name: '12A',
              role: 'GVCN',
              subject: 'Tổng hợp học sinh',
              size: '42 học sinh',
              contact: '40/42 phụ huynh có liên hệ xác thực',
              warning: '2 học sinh vắng chưa có đơn',
              color: AppColors.fptOrange,
            ),
            const SizedBox(height: AppSpacing.lg),
            const SectionHeader(title: 'Lớp giảng dạy'),
            const _ClassCard(
              name: 'SE1913',
              role: 'Giáo viên bộ môn',
              subject: 'PRM393 - Mobile Programming',
              size: '36 học sinh',
              contact: 'Nhập điểm Lab/PE trong kỳ mở điểm',
              warning: '8 bài Lab 2 chưa có điểm',
              color: AppColors.blue,
            ),
            const SizedBox(height: AppSpacing.sm),
            const _ClassCard(
              name: '11B',
              role: 'Cố vấn học tập',
              subject: 'Kỹ năng dự án',
              size: '39 học sinh',
              contact: '3 nhóm cần phản hồi nhận xét',
              warning: 'Tương tác phụ huynh 74%',
              color: AppColors.green,
            ),
            const SizedBox(height: AppSpacing.lg),
            const SectionHeader(title: 'Danh bạ nhanh'),
            AppCard(
              child: Column(
                children: [
                  _ContactRow(name: 'Phụ huynh Nguyễn Minh An', detail: 'SĐT: 0901 234 567 • Đã đọc 92% thông báo'),
                  const Divider(height: AppSpacing.lg),
                  _ContactRow(name: 'Phụ huynh Trần Hoàng Nam', detail: 'SĐT: 0988 112 233 • Cần xác nhận đơn nghỉ'),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ClassCard extends StatelessWidget {
  const _ClassCard({
    required this.name,
    required this.role,
    required this.subject,
    required this.size,
    required this.contact,
    required this.warning,
    required this.color,
  });

  final String name;
  final String role;
  final String subject;
  final String size;
  final String contact;
  final String warning;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Center(
                  child: Text(
                    name,
                    style: TextStyle(color: color, fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                ),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      role,
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: AppColors.ink),
                    ),
                    Text(
                      subject,
                      style: const TextStyle(fontSize: 12, color: AppColors.muted),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const Divider(height: AppSpacing.lg),
          Text('Sĩ số: $size', style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700)),
          const SizedBox(height: AppSpacing.xs),
          Text(contact, style: const TextStyle(fontSize: 12, color: AppColors.muted)),
          const SizedBox(height: AppSpacing.xs),
          Text(
            warning,
            style: const TextStyle(fontSize: 12, color: AppColors.danger, fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

class _ContactRow extends StatelessWidget {
  const _ContactRow({required this.name, required this.detail});

  final String name;
  final String detail;

  @override
  Widget build(BuildContext context) {
    return Row(
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
                style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.ink),
              ),
              Text(
                detail,
                style: const TextStyle(fontSize: 11, color: AppColors.muted),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
