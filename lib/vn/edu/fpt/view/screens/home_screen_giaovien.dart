import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_attendance_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_leave_requests_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_web_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/announcements_create_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_stats_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_tuition_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/schedule_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';

class HomeTeacher extends StatefulWidget {
  const HomeTeacher({
    super.key,
    required this.authService,
    this.notificationService,
  });

  final AuthService authService;
  final NotificationService? notificationService;

  @override
  State<HomeTeacher> createState() => _HomeTeacherState();
}

class _HomeTeacherState extends State<HomeTeacher> {
  bool _loadingClass = true;
  int? _classId;
  String? _className;
  String? _teacherName;
  String? _employeeCode;
  int _pendingLeaveCount = 0;

  @override
  void initState() {
    super.initState();
    widget.notificationService?.addListener(_onNotificationChanged);
    _loadTeacherClass();
    _loadPendingLeaveCount();
  }

  @override
  void dispose() {
    widget.notificationService?.removeListener(_onNotificationChanged);
    super.dispose();
  }

  void _onNotificationChanged() {
    final notifications = widget.notificationService?.notifications ?? const [];
    if (notifications.any((item) => item.relatedType == 'LEAVE_REQUEST')) {
      _loadPendingLeaveCount();
    }
  }

  Future<void> _loadPendingLeaveCount() async {
    final session = widget.authService.currentSession;
    if (session == null) return;
    try {
      final count = await LeaveRequestApiClient(backend: BackendApiClient())
          .getPendingCount(token: session.token);
      if (mounted) setState(() => _pendingLeaveCount = count);
    } catch (_) {
      // Không chặn trang chủ nếu badge chưa tải được.
    }
  }

  Future<void> _loadTeacherClass() async {
    setState(() => _loadingClass = true);
    try {
      final session = widget.authService.currentSession;
      if (session == null) {
        setState(() => _loadingClass = false);
        return;
      }

      final backend = BackendApiClient();
      final profile = await backend.getData(
        '/api/user/profile',
        token: session.token,
      ) as Map<String, dynamic>?;
      final context = await AttendanceApiClient(backend: backend)
          .getHomeroomContext(token: session.token);
      final teacherProfile = profile?['teacherProfile'] as Map<String, dynamic>?;
      if (!mounted) return;
      setState(() {
        _classId = context['classId'] as int?;
        _className = context['className'] as String?;
        _teacherName = profile?['name'] as String? ?? session.userName;
        _employeeCode = teacherProfile?['employeeCode'] as String? ?? session.accountCode;
        _loadingClass = false;
      });
    } catch (e) {
      _setNullClass(widget.authService.currentSession?.userName, widget.authService.currentSession?.accountCode);
    }
  }

  void _setNullClass(String? teacherName, String? employeeCode) {
    if (mounted) {
      setState(() {
        _classId = null;
        _className = null;
        _teacherName = teacherName;
        _employeeCode = employeeCode;
        _loadingClass = false;
      });
    }
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
                avatarWidget: const CircleAvatar(
                  backgroundColor: AppColors.primarySoft,
                  child: Icon(
                    Icons.co_present,
                    color: AppColors.fptOrange,
                    size: 20,
                  ),
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
                      colors: [AppColors.fptOrange, Color(0xFFFF8533)],
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
                          child: const Icon(
                            Icons.co_present,
                            color: Colors.white,
                            size: 26,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _loadingClass ? 'Đang tải thông tin...' : 'GVCN ${_className ?? 'Chưa xếp lớp'}',
                                style: const TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w900,
                                  color: Colors.white,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                _loadingClass ? '...' : 'Mã GV: ${_employeeCode ?? ''} • ${_teacherName ?? ''}',
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: Colors.white70,
                                  fontWeight: FontWeight.w500,
                                ),
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
                            title: 'Lịch dạy',
                            icon: Icons.calendar_month_outlined,
                            color: AppColors.teal,
                            onTap: () {
                              final session =
                                  widget.authService.currentSession!;
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => ScheduleScreen(
                                    service: ScheduleService(
                                      apiClient: ScheduleApiClient(
                                        backend: BackendApiClient(),
                                      ),
                                      token: session.token,
                                    ),
                                    mode: ScheduleViewMode.teacher,
                                  ),
                                ),
                              );
                            },
                          ),
                          if (_classId != null)
                            _FeatureButton(
                              title: 'Điểm danh lớp',
                              icon: Icons.fact_check_outlined,
                              color: AppColors.green,
                              onTap: () {
                                if (_loadingClass) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(
                                      content: Text('Đang tải dữ liệu lớp học, vui lòng đợi...'),
                                      behavior: SnackBarBehavior.floating,
                                    ),
                                  );
                                  return;
                                }
                                final session =
                                    widget.authService.currentSession!;
                                Navigator.of(context).push(
                                  MaterialPageRoute<void>(
                                    builder: (_) => TeacherAttendanceScreen(
                                      token: session.token,
                                    ),
                                  ),
                                );
                              },
                            ),
                          _FeatureButton(
                            title: 'Duyệt đơn xin nghỉ',
                            icon: Icons.assignment_turned_in_outlined,
                            color: AppColors.warning,
                            badgeCount: _pendingLeaveCount > 0
                                ? _pendingLeaveCount
                                : null,
                            onTap: () {
                              final session =
                                  widget.authService.currentSession!;
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => TeacherLeaveRequestsScreen(
                                    token: session.token,
                                    notificationService: widget.notificationService,
                                  ),
                                ),
                              ).then((_) => _loadPendingLeaveCount());
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
                                  builder: (_) =>
                                      const AnnouncementsCreateScreen(),
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
                              Navigator.of(context)
                                  .push(
                                    MaterialPageRoute<void>(
                                      builder: (_) =>
                                          const TeacherTuitionScreen(),
                                    ),
                                  )
                                  .then((_) => setState(() {}));
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
