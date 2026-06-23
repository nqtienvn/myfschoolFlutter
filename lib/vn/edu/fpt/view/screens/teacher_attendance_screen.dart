import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherAttendanceScreen extends StatefulWidget {
  const TeacherAttendanceScreen({super.key});

  @override
  State<TeacherAttendanceScreen> createState() => _TeacherAttendanceScreenState();
}

class _TeacherAttendanceScreenState extends State<TeacherAttendanceScreen> {
  final List<_AttendanceRoster> _roster = [
    _AttendanceRoster(name: 'Nguyễn Minh An', code: '12A-01', status: 'Có mặt'),
    _AttendanceRoster(name: 'Trần Hoàng Nam', code: '12A-07', status: 'Muộn'),
    _AttendanceRoster(name: 'Lê Bảo Châu', code: '12A-18', status: 'Vắng có phép'),
    _AttendanceRoster(name: 'Phạm Gia Huy', code: '12A-31', status: 'Vắng không phép'),
  ];

  @override
  Widget build(BuildContext context) {
    int present = _roster.where((r) => r.status == 'Có mặt').length;
    int late = _roster.where((r) => r.status == 'Muộn').length;
    int absent = _roster.where((r) => r.status.contains('Vắng')).length;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Điểm danh lớp'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            // Stats Panel
            AppCard(
              child: Row(
                children: [
                  Expanded(
                    child: _StatColumn(label: 'Có mặt', value: '$present', color: AppColors.green),
                  ),
                  Container(width: 1, height: 35, color: AppColors.line),
                  Expanded(
                    child: _StatColumn(label: 'Muộn', value: '$late', color: AppColors.warning),
                  ),
                  Container(width: 1, height: 35, color: AppColors.line),
                  Expanded(
                    child: _StatColumn(label: 'Vắng', value: '$absent', color: AppColors.danger),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            const SectionHeader(title: 'Danh sách lớp 12A'),
            const SizedBox(height: AppSpacing.xs),
            for (int i = 0; i < _roster.length; i++) ...[
              _RosterTile(
                roster: _roster[i],
                onStatusChanged: (newStatus) {
                  setState(() {
                    _roster[i].status = newStatus;
                  });
                },
              ),
              const SizedBox(height: AppSpacing.sm),
            ],
            const SizedBox(height: AppSpacing.lg),
            PrimaryButton(
              label: 'Lưu & Gửi phụ huynh',
              icon: Icons.send_outlined,
              onPressed: () {
                Navigator.of(context).pop();
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Lưu điểm danh và gửi thông báo thành công!'),
                    behavior: SnackBarBehavior.floating,
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _AttendanceRoster {
  _AttendanceRoster({
    required this.name,
    required this.code,
    required this.status,
  });

  final String name;
  final String code;
  String status;
}

class _StatColumn extends StatelessWidget {
  const _StatColumn({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          label,
          style: const TextStyle(fontSize: 11, color: AppColors.muted, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppSpacing.xs),
        Text(
          value,
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: color),
        ),
      ],
    );
  }
}

class _RosterTile extends StatelessWidget {
  const _RosterTile({
    required this.roster,
    required this.onStatusChanged,
  });

  final _AttendanceRoster roster;
  final ValueChanged<String> onStatusChanged;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                roster.name,
                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: AppColors.ink),
              ),
              Text(
                roster.code,
                style: const TextStyle(fontSize: 12, color: AppColors.muted, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          const Divider(height: AppSpacing.lg),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: ['Có mặt', 'Muộn', 'Vắng phép', 'Vắng không phép'].map((statusOption) {
              String currentMatch = roster.status;
              if (currentMatch == 'Vắng có phép' && statusOption == 'Vắng phép') currentMatch = 'Vắng phép';
              if (currentMatch == 'Vắng không phép' && statusOption == 'Vắng không phép') currentMatch = 'Vắng không phép';
              
              String statusKey = statusOption;
              if (statusOption == 'Vắng phép') statusKey = 'Vắng có phép';
              if (statusOption == 'Vắng không phép') statusKey = 'Vắng không phép';

              final isSelected = roster.status == statusKey;
              Color activeColor = AppColors.fptOrange;
              if (statusOption == 'Có mặt') activeColor = AppColors.green;
              if (statusOption == 'Muộn') activeColor = AppColors.warning;
              if (statusOption.contains('Vắng')) activeColor = AppColors.danger;

              return ChoiceChip(
                label: Text(statusOption),
                selected: isSelected,
                onSelected: (_) => onStatusChanged(statusKey),
                selectedColor: activeColor.withValues(alpha: 0.12),
                labelStyle: TextStyle(
                  color: isSelected ? activeColor : AppColors.ink,
                  fontSize: 10,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}
