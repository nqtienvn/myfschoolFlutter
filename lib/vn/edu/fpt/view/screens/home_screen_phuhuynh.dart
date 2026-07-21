import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/attendance_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/leave_request_list_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/schedule_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/tuition_payment_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_bottom_sheet.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/auth_session.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';

class HomeParent extends StatefulWidget {
  const HomeParent({
    super.key,
    required this.authService,
    this.backend,
    this.notificationService,
  });

  final AuthService authService;
  final BackendApiClient? backend;
  final NotificationService? notificationService;

  @override
  State<HomeParent> createState() => _HomeParentState();
}

class _HomeParentState extends State<HomeParent> {
  late final BackendApiClient _backend = widget.backend ?? BackendApiClient();
  late final TuitionBillApiClient _tuitionApi = TuitionBillApiClient(
    backend: _backend,
  );
  late final DashboardApiClient _dashboardApi = DashboardApiClient(
    backend: _backend,
  );
  StudentDashboardStatsDto? _dashboard;
  List<TuitionBill> _tuitionBills = const [];
  AcademicPeriod? _selectedPeriod;
  String? _loadedContextKey;
  int _loadGeneration = 0;
  bool _dashboardLoading = false;
  String? _dashboardError;
  bool _tuitionLoading = false;
  String? _tuitionError;

  @override
  void initState() {
    super.initState();
    widget.authService.addListener(_onSelectionChanged);
  }

  @override
  void didUpdateWidget(covariant HomeParent oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.authService != widget.authService) {
      oldWidget.authService.removeListener(_onSelectionChanged);
      widget.authService.addListener(_onSelectionChanged);
    }
  }

  @override
  void dispose() {
    widget.authService.removeListener(_onSelectionChanged);
    super.dispose();
  }

  void _onSelectionChanged() {
    if (!mounted) return;
    _loadGeneration++;
    _loadedContextKey = null;
    setState(() {
      _dashboard = null;
      _dashboardError = null;
      _tuitionBills = const [];
      _tuitionError = null;
    });
    final period = _selectedPeriod;
    if (period != null) _loadContext(period);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    _selectedPeriod = period;
    if (period == null) {
      _loadGeneration++;
      _loadedContextKey = null;
      return;
    }
    _loadContext(period);
  }

  Future<void> _loadContext(AcademicPeriod period) async {
    if (!mounted || !_samePeriod(period, _selectedPeriod)) return;
    final child = widget.authService.selectedChild;
    if (child == null) return;
    final session = widget.authService.currentSession;
    if (session == null) return;
    final requestedToken = session.token;
    final key = '${child.id}-${period.academicYearId}-${period.semesterId}';
    if (_loadedContextKey == key) return;
    _loadedContextKey = key;
    final generation = ++_loadGeneration;
    setState(() {
      _dashboardLoading = true;
      _dashboardError = null;
      _dashboard = null;
      _tuitionLoading = true;
      _tuitionError = null;
      _tuitionBills = const [];
    });
    Object? tuitionError;
    try {
      final results = await Future.wait<Object?>([
        _dashboardApi.getStudentStats(
          token: requestedToken,
          studentId: child.id,
          academicYearId: period.academicYearId,
          semesterId: period.semesterId,
        ),
        _tuitionApi
            .getStudentBills(
              token: requestedToken,
              studentId: child.id,
              semesterId: period.semesterId,
            )
            .then<List<TuitionBill>>(
              (value) => value,
              onError: (Object error, StackTrace _) {
                tuitionError = error;
                return const <TuitionBill>[];
              },
            ),
      ]);
      if (_isCurrentLoad(generation, key, child.id, period, requestedToken)) {
        setState(() {
          _dashboard = results[0] as StudentDashboardStatsDto;
          _tuitionBills = results[1] as List<TuitionBill>;
          _tuitionError = tuitionError?.toString().replaceAll(
            'Exception: ',
            '',
          );
        });
      }
    } catch (error) {
      if (_isCurrentLoad(generation, key, child.id, period, requestedToken)) {
        setState(() {
          _dashboard = null;
          _dashboardError = error.toString().replaceAll('Exception: ', '');
          _tuitionBills = const [];
          _tuitionError = tuitionError?.toString().replaceAll(
            'Exception: ',
            '',
          );
        });
      }
    } finally {
      if (_isCurrentLoad(generation, key, child.id, period, requestedToken)) {
        setState(() {
          _dashboardLoading = false;
          _tuitionLoading = false;
        });
      }
    }
  }

  bool _isCurrentLoad(
    int generation,
    String requestKey,
    int childId,
    AcademicPeriod period,
    String requestedToken,
  ) =>
      mounted &&
      generation == _loadGeneration &&
      _loadedContextKey == requestKey &&
      widget.authService.selectedChild?.id == childId &&
      widget.authService.currentSession?.token == requestedToken &&
      _samePeriod(period, _selectedPeriod);

  bool _samePeriod(AcademicPeriod? first, AcademicPeriod? second) =>
      first?.academicYearId == second?.academicYearId &&
      first?.semesterId == second?.semesterId;

  List<LinkedStudent> get _children =>
      widget.authService.currentSession?.children ?? const [];

  StudentSnapshot? get _student {
    final data = _dashboard;
    final child = widget.authService.selectedChild;
    if (data == null || child == null) return null;
    return StudentSnapshot.linked(
      id: data.studentId,
      name: data.studentName,
      studentCode: data.studentCode,
      className: data.className,
      school: data.schoolName,
      linkStatus: child.status == 'ACTIVE' ? 'Đang học' : child.status,
      homeroomTeacherName: data.homeroomTeacherName,
      homeroomTeacherPhone: data.homeroomTeacherPhone,
      dateOfBirth: child.dateOfBirth,
      gender: child.gender,
      address: child.address,
      email: child.email,
      academicYearName: data.academicYearName,
      averageScore: data.currentGpa ?? 0,
      attendanceRate: data.attendanceRate,
    );
  }

  void _showTuitionNotificationsSheet(BuildContext context) {
    final period = _selectedPeriod;
    final student = _student;
    if (student == null) return;

    showAppBottomSheet(
      context: context,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Thông báo học phí',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: AppColors.ink,
            ),
          ),
          const SizedBox(height: 12),
          if (_tuitionLoading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_tuitionError != null)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 16),
              child: Text(
                _tuitionError!,
                style: const TextStyle(color: AppColors.danger),
              ),
            )
          else if (_tuitionBills.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 24),
              child: Center(
                child: Text(
                  'Không có khoản học phí trong ${period?.label ?? 'học kỳ đã chọn'}.',
                  style: const TextStyle(color: AppColors.muted, fontSize: 13),
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
                children: _tuitionBills.map((bill) {
                  return Card(
                    margin: const EdgeInsets.only(bottom: 12),
                    color: AppColors.surface,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                      side: BorderSide(
                        color: AppColors.line.withValues(alpha: 0.5),
                      ),
                    ),
                    elevation: 0,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Icon(
                                bill.status == 'Đã đóng'
                                    ? Icons.check_circle_outline
                                    : Icons.warning_amber_rounded,
                                color: bill.status == 'Đã đóng'
                                    ? AppColors.success
                                    : AppColors.warning,
                                size: 20,
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  bill.title,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 14,
                                    color: AppColors.ink,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Text(
                            '${bill.amount.toString().replaceAllMapped(RegExp(r"(\d{1,3})(?=(\d{3})+(?!\d))"), (Match m) => "${m[1]}.")} đ • Hạn ${bill.dueDate} • ${bill.status}',
                            style: const TextStyle(
                              fontSize: 12.5,
                              color: AppColors.muted,
                              height: 1.4,
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
          if (_tuitionBills.isNotEmpty) ...[
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
                            studentId: student.id!,
                            token: widget.authService.currentSession!.token,
                          ),
                        ),
                      )
                      .then((_) {
                        if (!mounted) return;
                        final selected = _selectedPeriod;
                        _loadedContextKey = null;
                        if (selected != null) _loadContext(selected);
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
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final students = _children;
    final student = _student;
    final dashboard = _dashboard;
    if (students.isEmpty) {
      return const Scaffold(
        backgroundColor: AppColors.background,
        body: SafeArea(
          child: Center(
            child: Padding(
              padding: EdgeInsets.all(AppSpacing.lg),
              child: Text(
                'Tài khoản phụ huynh chưa được liên kết với học sinh nào.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: AppColors.muted,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
        ),
      );
    }
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
                  // Student Switcher
                  SizedBox(
                    height: 48,
                    child: ListView.builder(
                      scrollDirection: Axis.horizontal,
                      itemCount: students.length,
                      itemBuilder: (context, index) {
                        final s = students[index];
                        final isSelected =
                            index == widget.authService.selectedChildIndex;
                        return Padding(
                          padding: const EdgeInsets.only(right: AppSpacing.sm),
                          child: GestureDetector(
                            onTap: () => widget.authService.selectChild(index),
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: AppSpacing.md,
                                vertical: 8,
                              ),
                              decoration: BoxDecoration(
                                color: isSelected
                                    ? AppColors.primarySoft
                                    : AppColors.surface,
                                borderRadius: BorderRadius.circular(20),
                                border: Border.all(
                                  color: isSelected
                                      ? AppColors.fptOrange
                                      : AppColors.line.withValues(alpha: 0.6),
                                  width: 1.5,
                                ),
                                boxShadow: isSelected
                                    ? [
                                        BoxShadow(
                                          color: AppColors.fptOrange.withValues(
                                            alpha: 0.1,
                                          ),
                                          blurRadius: 8,
                                          offset: const Offset(0, 4),
                                        ),
                                      ]
                                    : null,
                              ),
                              child: Row(
                                children: [
                                  Text(
                                    s.name,
                                    style: TextStyle(
                                      color: isSelected
                                          ? AppColors.fptOrange
                                          : AppColors.ink,
                                      fontWeight: isSelected
                                          ? FontWeight.w800
                                          : FontWeight.w600,
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
                  const SizedBox(height: 12),
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
                            onPressed: _selectedPeriod == null
                                ? null
                                : () {
                                    _loadedContextKey = null;
                                    _loadGeneration++;
                                    _loadContext(_selectedPeriod!);
                                  },
                            icon: const Icon(Icons.refresh),
                            label: const Text('Tải lại hồ sơ'),
                          ),
                        ],
                      ),
                    )
                  else if (student != null && dashboard != null)
                    AppCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            student.name,
                            style: const TextStyle(
                              color: AppColors.ink,
                              fontSize: 16,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Lớp ${student.className} • Mã HS: ${student.studentCode}\n${student.school} • ${_selectedPeriod?.label ?? ''}',
                            style: const TextStyle(
                              color: AppColors.muted,
                              fontSize: 12,
                              height: 1.35,
                            ),
                          ),
                          const SizedBox(height: 14),
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
                            'GVCN: ${student.homeroomTeacher} • ${student.homeroomPhone}',
                            style: const TextStyle(
                              color: AppColors.muted,
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                  const SizedBox(height: 24),
                  const SectionHeader(title: 'Tiện ích học tập'),
                  Builder(
                    builder: (context) {
                      final tuitionNotifsCount = _tuitionBills
                          .where((bill) => bill.status == 'Chưa đóng')
                          .length;

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
                              if (student == null) return;
                              final session =
                                  widget.authService.currentSession!;
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => ScheduleScreen(
                                    service: ScheduleService(
                                      apiClient: ScheduleApiClient(
                                        backend: _backend,
                                      ),
                                      token: session.token,
                                    ),
                                    mode: ScheduleViewMode.student,
                                    studentId: student.id,
                                    studentName: student.name,
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
                              if (student == null) return;
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => GradesScreen(
                                    token: widget
                                        .authService
                                        .currentSession!
                                        .token,
                                    studentId: student.id,
                                    studentName: student.name,
                                    authService: widget.authService,
                                    backendApiClient: _backend,
                                    notificationService:
                                        widget.notificationService,
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
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => StudentAttendanceScreen(
                                    student: student,
                                    token: widget
                                        .authService
                                        .currentSession!
                                        .token,
                                  ),
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
                              if (student == null) return;
                              final session =
                                  widget.authService.currentSession!;
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => LeaveRequestListScreen(
                                    student: student,
                                    token: session.token,
                                  ),
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
                            onTap: () =>
                                _showTuitionNotificationsSheet(context),
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
                padding: const EdgeInsets.symmetric(
                  horizontal: 8,
                  vertical: 10,
                ),
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
