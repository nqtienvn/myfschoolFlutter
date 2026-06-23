import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/assigned_classes_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_attendance_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_leave_requests_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_web_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/announcements_create_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_stats_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_tuition_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/schedule_screen.dart';


class HomeTeacher extends StatefulWidget {
  const HomeTeacher({super.key});

  @override
  State<HomeTeacher> createState() => _HomeTeacherState();
}

class _HomeTeacherState extends State<HomeTeacher> {
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
                avatarWidget: const CircleAvatar(
                  backgroundColor: AppColors.primarySoft,
                  child: Icon(Icons.co_present, color: AppColors.fptOrange, size: 20),
                ),
              ),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(24, 0, 24, 16),
                children: [

            // Teacher Profile / Role Summary Card
            AppCard(
              padding: 20,
              gradient: const LinearGradient(
                colors: [
                  AppColors.fptOrange,
                  Color(0xFFFF8533),
                ],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              child: Row(
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.co_present, color: Colors.white, size: 26),
                  ),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: const [
                        Text(
                          'GVCN Lớp 12A',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: Colors.white),
                        ),
                        SizedBox(height: 2),
                        Text(
                          'Môn dạy: PRM393 - SE1913 • FPT Schools',
                          style: TextStyle(fontSize: 12, color: Colors.white70, fontWeight: FontWeight.w500),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.xs),

            const SizedBox(height: 24),

            // Grid of Features
            const SectionHeader(title: 'Chức năng giảng dạy & điều hành'),
            Builder(
              builder: (context) {
                final total = mockStudents.length;
                final paid = mockStudents.where((student) {
                  final unpaid = student.tuitionBills
                      .where((bill) => bill.status == 'Chưa đóng')
                      .fold(0, (sum, bill) => sum + bill.amount);
                  return unpaid == 0;
                }).length;
                final unpaidCount = total - paid;

                return GridView.count(
                  crossAxisCount: 2,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  mainAxisSpacing: AppSpacing.md,
                  crossAxisSpacing: AppSpacing.md,
                  childAspectRatio: 1.4,
                  children: [
                    _FeatureButton(
                      title: 'Lớp được phân công',
                      icon: Icons.groups_2_outlined,
                      color: AppColors.blue,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => const AssignedClassesScreen(),
                          ),
                        );
                      },
                    ),
                    _FeatureButton(
                      title: 'Điểm danh lớp',
                      icon: Icons.fact_check_outlined,
                      color: AppColors.green,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => const TeacherAttendanceScreen(),
                          ),
                        );
                      },
                    ),
                    _FeatureButton(
                      title: 'Duyệt đơn xin nghỉ',
                      icon: Icons.assignment_turned_in_outlined,
                      color: AppColors.warning,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => const TeacherLeaveRequestsScreen(),
                          ),
                        );
                      },
                    ),
                    _FeatureButton(
                      title: 'Nhập & Upload điểm',
                      icon: Icons.grid_on_outlined,
                      color: AppColors.blue,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => const GradesWebScreen(),
                          ),
                        );
                      },
                    ),
                    _FeatureButton(
                      title: 'Gửi thông báo lớp',
                      icon: Icons.campaign_outlined,
                      color: AppColors.fptOrange,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => const AnnouncementsCreateScreen(),
                          ),
                        );
                      },
                    ),
                    _FeatureButton(
                      title: 'Thống kê lớp học',
                      icon: Icons.auto_graph_outlined,
                      color: AppColors.teal,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => const TeacherStatsScreen(),
                          ),
                        );
                      },
                    ),
                    _FeatureButton(
                      title: 'QL Học phí',
                      icon: Icons.request_quote_outlined,
                      color: AppColors.fptOrange,
                      badgeCount: unpaidCount > 0 ? unpaidCount : null,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => const TeacherTuitionScreen(),
                          ),
                        ).then((_) => setState(() {}));
                      },
                    ),
                    _FeatureButton(
                      title: 'Lịch dạy',
                      icon: Icons.calendar_month,
                      color: AppColors.teal,
                      onTap: () {
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => const ScheduleScreen(),
                          ),
                        );
                      },
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
    required this.color,
    required this.onTap,
    this.badgeCount,
  });

  final String title;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;
  final int? badgeCount;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: 0,
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
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: color.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Icon(icon, color: color, size: 20),
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
                      fontWeight: FontWeight.w800,
                      color: AppColors.ink,
                      height: 1.2,
                    ),
                  ),
                ],
              ),
            ),
            if (badgeCount != null && badgeCount! > 0)
              Positioned(
                top: 8,
                right: 8,
                child: Container(
                  padding: const EdgeInsets.all(4),
                  decoration: const BoxDecoration(
                    color: AppColors.danger,
                    shape: BoxShape.circle,
                  ),
                  constraints: const BoxConstraints(
                    minWidth: 16,
                    minHeight: 16,
                  ),
                  child: Text(
                    '$badgeCount',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 9,
                      fontWeight: FontWeight.bold,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
