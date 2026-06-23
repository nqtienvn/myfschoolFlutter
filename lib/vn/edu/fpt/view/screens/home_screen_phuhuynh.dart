import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/attendance_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/leave_request_list_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/schedule_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/tuition_payment_screen.dart';


class HomeParent extends StatefulWidget {
  const HomeParent({super.key});

  @override
  State<HomeParent> createState() => _HomeParentState();
}

class _HomeParentState extends State<HomeParent> {
  int _selectedStudentIndex = 0;

  StudentSnapshot get _student => mockStudents[_selectedStudentIndex];

  void _showTuitionNotificationsSheet(BuildContext context) {
    final unpaidSum = _student.tuitionBills
        .where((bill) => bill.status == 'Chưa đóng')
        .fold(0, (sum, bill) => sum + bill.amount);

    final tuitionNotifs = unpaidSum > 0
        ? _student.notifications
            .where((n) => n.tag == 'Học phí' || n.title.toLowerCase().contains('học phí'))
            .toList()
        : <ParentNotification>[];

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return Container(
          decoration: const BoxDecoration(
            color: AppColors.background,
            borderRadius: BorderRadius.only(
              topLeft: Radius.circular(24),
              topRight: Radius.circular(24),
            ),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppColors.line,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              const Text(
                'Thông báo học phí',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.ink),
              ),
              const SizedBox(height: 12),
              if (tuitionNotifs.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Center(
                    child: Text(
                      'Không có thông báo học phí nào mới.',
                      style: TextStyle(color: AppColors.muted, fontSize: 13),
                    ),
                  ),
                )
              else
                ConstrainedBox(
                  constraints: BoxConstraints(
                    maxHeight: MediaQuery.of(context).size.height * 0.4,
                  ),
                  child: ListView(
                    shrinkWrap: true,
                    children: tuitionNotifs.map((n) {
                      return Card(
                        margin: const EdgeInsets.only(bottom: 12),
                        color: AppColors.surface,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                          side: BorderSide(color: AppColors.line.withValues(alpha: 0.5)),
                        ),
                        elevation: 0,
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Icon(Icons.warning_amber_rounded, color: n.color, size: 20),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      n.title,
                                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: AppColors.ink),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 8),
                              Text(
                                n.body,
                                style: const TextStyle(fontSize: 12.5, color: AppColors.muted, height: 1.4),
                              ),
                            ],
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ),
              if (tuitionNotifs.isNotEmpty) ...[
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton.icon(
                    onPressed: () {
                      Navigator.pop(context);
                      Navigator.of(context).push(
                        MaterialPageRoute<void>(
                          builder: (_) => TuitionPaymentScreen(student: _student),
                        ),
                      ).then((_) => setState(() {}));
                    },
                    icon: const Icon(Icons.account_balance_wallet_outlined),
                    label: const Text('Đóng học phí', style: TextStyle(fontWeight: FontWeight.bold)),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.fptOrange,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
              child: SharedHeader(
                avatarWidget: CircleAvatar(
                  backgroundColor: _student.avatarColor.withValues(alpha: 0.12),
                  child: Text(
                    _student.shortName,
                    style: TextStyle(
                      color: _student.avatarColor,
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(24, 0, 24, 16),
                children: [
                  // Student Switcher
                  SizedBox(
                    height: 48,
                    child: ListView.builder(
                      scrollDirection: Axis.horizontal,
                      itemCount: mockStudents.length,
                      itemBuilder: (context, index) {
                        final s = mockStudents[index];
                        final isSelected = index == _selectedStudentIndex;
                        return Padding(
                          padding: const EdgeInsets.only(right: AppSpacing.sm),
                          child: GestureDetector(
                            onTap: () => setState(() => _selectedStudentIndex = index),
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: 8),
                              decoration: BoxDecoration(
                                color: isSelected ? AppColors.primarySoft : AppColors.surface,
                                borderRadius: BorderRadius.circular(20),
                                border: Border.all(
                                  color: isSelected ? AppColors.fptOrange : AppColors.line.withValues(alpha: 0.6),
                                  width: 1.5,
                                ),
                                boxShadow: isSelected
                                    ? [
                                        BoxShadow(
                                          color: AppColors.fptOrange.withValues(alpha: 0.1),
                                          blurRadius: 8,
                                          offset: const Offset(0, 4),
                                        )
                                      ]
                                    : null,
                              ),
                              child: Row(
                                children: [
                                  CircleAvatar(
                                    radius: 12,
                                    backgroundColor: s.avatarColor.withValues(alpha: 0.12),
                                    child: Text(
                                      s.shortName,
                                      style: TextStyle(
                                        color: s.avatarColor,
                                        fontSize: 10,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: AppSpacing.sm),
                                  Text(
                                    s.name,
                                    style: TextStyle(
                                      color: isSelected ? AppColors.fptOrange : AppColors.ink,
                                      fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600,
                                      fontSize: 13,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                  const SizedBox(height: 24),
                  const SectionHeader(title: 'Tiện ích học tập'),
                  Builder(
                    builder: (context) {
                      final unpaidSum = _student.tuitionBills
                          .where((bill) => bill.status == 'Chưa đóng')
                          .fold(0, (sum, bill) => sum + bill.amount);

                      final tuitionNotifsCount = unpaidSum > 0
                          ? _student.notifications
                              .where((n) => n.tag == 'Học phí' || n.title.toLowerCase().contains('học phí'))
                              .length
                          : 0;

                      return GridView.count(
                        crossAxisCount: 2,
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        mainAxisSpacing: AppSpacing.md,
                        crossAxisSpacing: AppSpacing.md,
                        childAspectRatio: 1.4,
                        children: [
                          _FeatureButton(
                            title: 'Thời khóa biểu',
                            icon: Icons.calendar_month,
                            iconColor: AppColors.fptOrange,
                            iconBgColor: AppColors.primarySoft,
                            onTap: () {
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => const ScheduleScreen(),
                                ),
                              );
                            },
                          ),
                          _FeatureButton(
                            title: 'Bảng điểm',
                            icon: Icons.school,
                            iconColor: AppColors.blue,
                            iconBgColor: AppColors.blueSoft,
                            onTap: () {
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => const GradesScreen(),
                                ),
                              );
                            },
                          ),
                          _FeatureButton(
                            title: 'Chuyên cần',
                            icon: Icons.check_circle,
                            iconColor: AppColors.teal,
                            iconBgColor: AppColors.tealSoft,
                            onTap: () {
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => StudentAttendanceScreen(student: _student),
                                ),
                              );
                            },
                          ),
                          _FeatureButton(
                            title: 'Đơn từ',
                            icon: Icons.description,
                            iconColor: AppColors.danger,
                            iconBgColor: AppColors.dangerSoft,
                            onTap: () {
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => LeaveRequestListScreen(student: _student),
                                ),
                              );
                            },
                          ),
                          _FeatureButton(
                            title: 'TB Học phí',
                            icon: Icons.notification_important_outlined,
                            iconColor: AppColors.warning,
                            iconBgColor: AppColors.warningSoft,
                            showDot: tuitionNotifsCount > 0,
                            onTap: () => _showTuitionNotificationsSheet(context),
                          ),
                        ],
                      );
                    },
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

class _FeatureButton extends StatelessWidget {
  const _FeatureButton({
    required this.title,
    required this.icon,
    required this.iconColor,
    required this.iconBgColor,
    required this.onTap,
    this.showDot = false,
  });

  final String title;
  final IconData icon;
  final Color iconColor;
  final Color iconBgColor;
  final VoidCallback onTap;
  final bool showDot;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.line.withValues(alpha: 0.5)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: Stack(
            clipBehavior: Clip.none,
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 10),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Center(
                      child: Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: iconBgColor,
                          shape: BoxShape.circle,
                        ),
                        child: Icon(icon, color: iconColor, size: 20),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      title,
                      textAlign: TextAlign.center,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: AppColors.ink,
                      ),
                    ),
                  ],
                ),
              ),
              if (showDot)
                Positioned(
                  top: 10,
                  right: 10,
                  child: Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: AppColors.danger,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
