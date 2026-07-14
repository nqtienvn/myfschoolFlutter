import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/attendance_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/schedule_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/tuition_payment_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_bottom_sheet.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';

class HomeStudent extends StatefulWidget {
  const HomeStudent({super.key, required this.authService, this.backend});

  final AuthService authService;
  final BackendApiClient? backend;

  @override
  State<HomeStudent> createState() => _HomeStudentState();
}

class _HomeStudentState extends State<HomeStudent> {
  late final BackendApiClient _backend = widget.backend ?? BackendApiClient();
  late final TuitionBillApiClient _tuitionApi = TuitionBillApiClient(
    backend: _backend,
  );
  late final DashboardApiClient _dashboardApi = DashboardApiClient(
    backend: _backend,
  );
  StudentDashboardStatsDto? _dashboard;
  List<TuitionBill> _tuitionBills = const [];
  String? _loadedPeriodKey;
  int _loadGeneration = 0;
  bool _dashboardLoading = true;
  String? _dashboardError;

  StudentSnapshot? get _student {
    final data = _dashboard;
    if (data == null) return null;
    return StudentSnapshot.linked(
      id: data.studentId,
      name: data.studentName,
      studentCode: data.studentCode,
      className: data.className,
      school: data.schoolName,
      linkStatus: 'Đang học',
      academicYearName: data.academicYearName,
      homeroomTeacherName: data.homeroomTeacherName,
      homeroomTeacherPhone: data.homeroomTeacherPhone,
      averageScore: data.currentGpa ?? 0,
      attendanceRate: data.attendanceRate,
    );
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    if (period == null) {
      if (_loadedPeriodKey != null) {
        _loadedPeriodKey = null;
        _loadGeneration++;
      }
      return;
    }
    final key = '${period.academicYearId}-${period.semesterId}';
    if (_loadedPeriodKey != key) {
      _loadedPeriodKey = key;
      _loadPeriod(period);
    }
  }

  Future<void> _loadPeriod(AcademicPeriod period) async {
    final requestKey = '${period.academicYearId}-${period.semesterId}';
    if (_loadedPeriodKey != requestKey) return;
    final session = widget.authService.currentSession;
    if (session == null) return;
    final requestedToken = session.token;
    final generation = ++_loadGeneration;
    setState(() {
      _dashboardLoading = true;
      _dashboardError = null;
      _dashboard = null;
      _tuitionBills = const [];
    });
    try {
      final results = await Future.wait<Object?>([
        _dashboardApi.getStudentStats(
          token: session.token,
          academicYearId: period.academicYearId,
          semesterId: period.semesterId,
        ),
        _loadTuitionSafely(session.token, period.semesterId),
      ]);
      if (!_isCurrentLoad(generation, requestKey, requestedToken)) return;
      setState(() {
        _dashboard = results[0] as StudentDashboardStatsDto;
        _tuitionBills = results[1] as List<TuitionBill>;
      });
    } catch (error) {
      if (_isCurrentLoad(generation, requestKey, requestedToken)) {
        setState(() {
          _dashboardError = error.toString().replaceAll('Exception: ', '');
          _dashboard = null;
          _tuitionBills = const [];
        });
      }
    } finally {
      if (_isCurrentLoad(generation, requestKey, requestedToken)) {
        setState(() => _dashboardLoading = false);
      }
    }
  }

  bool _isCurrentLoad(
    int generation,
    String requestKey,
    String requestedToken,
  ) =>
      mounted &&
      generation == _loadGeneration &&
      _loadedPeriodKey == requestKey &&
      widget.authService.currentSession?.token == requestedToken;

  Future<List<TuitionBill>> _loadTuitionSafely(
    String token,
    int semesterId,
  ) async {
    try {
      return await _tuitionApi.getStudentBills(
        token: token,
        semesterId: semesterId,
      );
    } catch (_) {
      return const [];
    }
  }

  void _showStudentTuitionAlertSheet(BuildContext context) {
    final student = _student;
    if (student == null) return;
    final unpaidSum = _tuitionBills
        .where((bill) => bill.status == 'Chưa đóng')
        .fold(0, (sum, bill) => sum + bill.amount);
    final isPaid = unpaidSum == 0;
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    final hasBills = _tuitionBills.isNotEmpty;

    showAppBottomSheet(
      context: context,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Thông tin & Cảnh báo học phí',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: AppColors.ink,
            ),
          ),
          const SizedBox(height: 16),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: isPaid ? AppColors.successSoft : AppColors.dangerSoft,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  isPaid
                      ? 'ĐÃ HOÀN THÀNH HỌC PHÍ'
                      : 'CÒN KHOẢN HỌC PHÍ CHƯA ĐÓNG',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: isPaid ? AppColors.success : AppColors.danger,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  isPaid
                      ? hasBills
                            ? 'Học sinh ${student.name} đã hoàn thành học phí ${period?.label ?? ''}.'
                            : 'Không có khoản học phí nào trong ${period?.label ?? 'học kỳ đã chọn'}.'
                      : 'Học sinh ${student.name} còn khoản học phí chưa thanh toán trong ${period?.label ?? 'học kỳ đã chọn'}.\nTổng số tiền: ${unpaidSum.toString().replaceAllMapped(RegExp(r"(\d{1,3})(?=(\d{3})+(?!\d))"), (Match m) => "${m[1]}.")} đ.',
                  style: const TextStyle(
                    fontSize: 13,
                    color: AppColors.ink,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
          if (hasBills) ...[
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.pop(context);
                  Navigator.of(context)
                      .push(
                        MaterialPageRoute<void>(
                          builder: (_) => TuitionPaymentScreen(
                            student: student,
                            token: widget.authService.currentSession!.token,
                            viewAsStudent: true,
                          ),
                        ),
                      )
                      .then((_) {
                        if (period != null) _loadPeriod(period);
                      });
                },
                icon: const Icon(Icons.account_balance_wallet_outlined),
                label: const Text(
                  'Đóng học phí',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.fptOrange,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
            ),
          ],
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final student = _student;
    final dashboard = _dashboard;
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
              child: const SharedHeader(),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(24, 0, 24, 16),
                children: [
                  if (_dashboardLoading && student == null)
                    const AppCard(
                      padding: 28,
                      child: Center(child: CircularProgressIndicator()),
                    )
                  else if (_dashboardError != null && student == null)
                    AppCard(
                      child: Column(
                        children: [
                          Text(
                            _dashboardError!,
                            textAlign: TextAlign.center,
                            style: const TextStyle(color: AppColors.danger),
                          ),
                          const SizedBox(height: 8),
                          TextButton.icon(
                            onPressed: period == null
                                ? null
                                : () => _loadPeriod(period),
                            icon: const Icon(Icons.refresh),
                            label: const Text('Tải lại hồ sơ'),
                          ),
                        ],
                      ),
                    )
                  else if (student != null)
                    AppCard(
                      padding: 20,
                      gradient: const LinearGradient(
                        colors: [AppColors.green, Color(0xFF66BB6A)],
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
                              Icons.school,
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
                                  student.name,
                                  style: const TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.w900,
                                    color: Colors.white,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  'Lớp ${student.className} • Mã HS: ${student.studentCode}\n${student.school} • ${period?.label ?? ''}',
                                  style: const TextStyle(
                                    fontSize: 12,
                                    color: Colors.white70,
                                    fontWeight: FontWeight.w500,
                                    height: 1.35,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  if (dashboard != null) ...[
                    const SizedBox(height: 12),
                    AppCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: _DashboardMetric(
                                  label: 'GPA học kỳ',
                                  value:
                                      dashboard.currentGpa?.toStringAsFixed(
                                        2,
                                      ) ??
                                      '--',
                                ),
                              ),
                              Expanded(
                                child: _DashboardMetric(
                                  label: 'Chuyên cần',
                                  value:
                                      '${dashboard.attendanceRate.toStringAsFixed(1)}%',
                                ),
                              ),
                              Expanded(
                                child: _DashboardMetric(
                                  label: 'Xếp hạng',
                                  value:
                                      dashboard.classRank?.toString() ?? '--',
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          Text(
                            'GVCN: ${student?.homeroomTeacher ?? 'Chưa cập nhật'} • ${student?.homeroomPhone ?? 'Chưa cập nhật'}',
                            style: const TextStyle(
                              color: AppColors.muted,
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 24),

                  // Learning Utilities Grid
                  const SectionHeader(title: 'Tiện ích học tập cá nhân'),
                  Builder(
                    builder: (context) {
                      final unpaidSum = _tuitionBills
                          .where((bill) => bill.status == 'Chưa đóng')
                          .fold(0, (sum, bill) => sum + bill.amount);
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
                                    mode: ScheduleViewMode.student,
                                  ),
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
                                  builder: (_) => GradesScreen(
                                    token: widget
                                        .authService
                                        .currentSession!
                                        .token,
                                    studentName: widget
                                        .authService
                                        .currentSession!
                                        .userName,
                                  ),
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
                              if (student == null) return;
                              final session =
                                  widget.authService.currentSession!;
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => StudentAttendanceScreen(
                                    student: student,
                                    token: session.token,
                                    viewAsStudent: true,
                                  ),
                                ),
                              );
                            },
                          ),
                          _FeatureButton(
                            title: 'Học phí',
                            icon: Icons.account_balance_wallet_outlined,
                            iconColor: unpaidSum > 0
                                ? AppColors.danger
                                : AppColors.success,
                            iconBgColor: unpaidSum > 0
                                ? AppColors.dangerSoft
                                : AppColors.successSoft,
                            onTap: () => _showStudentTuitionAlertSheet(context),
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

class _DashboardMetric extends StatelessWidget {
  const _DashboardMetric({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          style: const TextStyle(
            color: AppColors.ink,
            fontSize: 16,
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: const TextStyle(color: AppColors.muted, fontSize: 11),
        ),
      ],
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
  });

  final String title;
  final IconData icon;
  final Color iconColor;
  final Color iconBgColor;
  final VoidCallback onTap;

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
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 10),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: iconBgColor,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(icon, color: iconColor, size: 20),
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
        ),
      ),
    );
  }
}
