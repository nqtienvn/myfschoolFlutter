import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

class StudentProfileScreen extends StatelessWidget {
  const StudentProfileScreen({super.key, required this.student});

  final StudentSnapshot student;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Hồ sơ học sinh'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            AppCard(
              child: Column(
                children: [
                  CircleAvatar(
                    radius: 36,
                    backgroundColor: student.avatarColor.withValues(alpha: 0.12),
                    child: Text(
                      student.shortName,
                      style: TextStyle(
                        color: student.avatarColor,
                        fontSize: 24,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  Text(
                    student.name,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w900,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    'Mã HS: ${student.studentCode}',
                    style: const TextStyle(
                      color: AppColors.muted,
                      fontWeight: FontWeight.w600,
                      fontSize: 13,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.sm),
                  StatusPill(
                    label: student.linkStatus,
                    foreground: AppColors.success,
                    background: AppColors.successSoft,
                    compact: true,
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            const SectionHeader(title: 'Thông tin trường lớp'),
            AppCard(
              child: Column(
                children: [
                  _ProfileRow(label: 'Trường học', value: student.school),
                  const Divider(height: AppSpacing.lg),
                  _ProfileRow(label: 'Lớp học', value: student.className),
                  const Divider(height: AppSpacing.lg),
                  _ProfileRow(label: 'GV Chủ nhiệm', value: student.homeroomTeacher),
                  const Divider(height: AppSpacing.lg),
                  _ProfileRow(label: 'SĐT Giáo viên', value: student.homeroomPhone, isPhone: true),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            const SectionHeader(title: 'Dữ liệu liên kết'),
            const AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Phạm vi dữ liệu',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w700,
                      color: AppColors.muted,
                    ),
                  ),
                  SizedBox(height: AppSpacing.xs),
                  Text(
                    'Hồ sơ được liên kết trực tiếp từ Cơ sở dữ liệu trường FPT Schools. Các thông tin thay đổi xin vui lòng liên hệ văn phòng hành chính học vụ của trường.',
                    style: TextStyle(
                      fontSize: 12,
                      color: AppColors.ink,
                      height: 1.35,
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

class _ProfileRow extends StatelessWidget {
  const _ProfileRow({
    required this.label,
    required this.value,
    this.isPhone = false,
  });

  final String label;
  final String value;
  final bool isPhone;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 110,
          child: Text(
            label,
            style: const TextStyle(
              color: AppColors.muted,
              fontSize: 13,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: TextStyle(
              color: isPhone ? AppColors.blue : AppColors.ink,
              fontSize: 13,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
      ],
    );
  }
}
