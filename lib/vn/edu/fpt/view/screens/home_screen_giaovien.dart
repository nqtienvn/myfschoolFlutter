import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_attendance_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_leave_requests_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_web_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/announcements_create_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_stats_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/schedule_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/periodic_reviews_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';

class HomeTeacher extends StatefulWidget {
  const HomeTeacher({
    super.key,
    required this.authService,
    this.notificationService,
    this.backend,
  });

  final AuthService authService;
  final NotificationService? notificationService;
  final BackendApiClient? backend;

  @override
  State<HomeTeacher> createState() => _HomeTeacherState();
}

class _HomeTeacherState extends State<HomeTeacher> {
  late final BackendApiClient _backend = widget.backend ?? BackendApiClient();
  late final DashboardApiClient _dashboardApi = DashboardApiClient(
    backend: _backend,
  );
  late final HomeroomAcademicApiClient _homeroomAcademicApi =
      HomeroomAcademicApiClient(backend: _backend);
  late final LeaveRequestApiClient _leaveApi = LeaveRequestApiClient(
    backend: _backend,
  );
  bool _loadingClass = true;
  int? _classId;
  String? _className;
  String? _teacherName;
  String? _employeeCode;
  int _pendingLeaveCount = 0;
  AcademicPeriod? _selectedPeriod;
  String? _loadedPeriodKey;
  int _profileLoadGeneration = 0;
  int _periodLoadGeneration = 0;
  int _pendingLeaveLoadGeneration = 0;
  String? _classLoadError;
  bool _pendingLeaveLoadFailed = false;

  @override
  void initState() {
    super.initState();
    widget.notificationService?.addListener(_onNotificationChanged);
    _loadTeacherProfile();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final controller = AcademicPeriodScope.maybeOf(context);
    final selected = controller?.selected;
    if (selected == null) {
      _selectedPeriod = null;
      _periodLoadGeneration++;
      _pendingLeaveLoadGeneration++;
      if ((controller?.isLoading ?? false) || _loadedPeriodKey == 'none') {
        return;
      }
      _loadedPeriodKey = 'none';
      setState(() {
        _loadingClass = false;
        _classId = null;
        _className = null;
        _pendingLeaveCount = 0;
        _classLoadError = null;
        _pendingLeaveLoadFailed = false;
      });
      return;
    }
    if (_samePeriod(_selectedPeriod, selected)) return;
    _selectedPeriod = selected;
    _loadTeacherPeriod(selected);
  }

  @override
  void dispose() {
    widget.notificationService?.removeListener(_onNotificationChanged);
    super.dispose();
  }

  void _onNotificationChanged() {
    final notifications = widget.notificationService?.notifications ?? const [];
    if (notifications.any((item) => item.relatedType == 'LEAVE_REQUEST')) {
      final period = _selectedPeriod;
      if (period != null) _loadPendingLeaveCount(period);
    }
  }

  Future<void> _loadPendingLeaveCount(AcademicPeriod period) async {
    final session = widget.authService.currentSession;
    if (session == null) return;
    final requestedToken = session.token;
    final generation = ++_pendingLeaveLoadGeneration;
    try {
      final count = await _leaveApi.getPendingCount(
        token: session.token,
        academicYearId: period.academicYearId,
        semesterId: period.semesterId,
      );
      if (_isCurrentPendingLeaveLoad(generation, period, requestedToken)) {
        setState(() {
          _pendingLeaveCount = count;
          _pendingLeaveLoadFailed = false;
        });
      }
    } catch (_) {
      if (_isCurrentPendingLeaveLoad(generation, period, requestedToken)) {
        setState(() => _pendingLeaveLoadFailed = true);
      }
    }
  }

  bool _isCurrentPendingLeaveLoad(
    int generation,
    AcademicPeriod period,
    String requestedToken,
  ) =>
      mounted &&
      generation == _pendingLeaveLoadGeneration &&
      widget.authService.currentSession?.token == requestedToken &&
      _samePeriod(period, _selectedPeriod);

  bool _samePeriod(AcademicPeriod? first, AcademicPeriod? second) =>
      first?.academicYearId == second?.academicYearId &&
      first?.semesterId == second?.semesterId;

  Future<void> _loadTeacherProfile() async {
    final generation = ++_profileLoadGeneration;
    final requestedSession = widget.authService.currentSession;
    try {
      final session = requestedSession;
      if (session == null) {
        return;
      }
      final profile =
          await _backend.getData('/api/user/profile', token: session.token)
              as Map<String, dynamic>?;
      final teacherProfile =
          profile?['teacherProfile'] as Map<String, dynamic>?;
      if (!_isCurrentProfileLoad(generation, requestedSession?.token)) return;
      setState(() {
        _teacherName = profile?['name'] as String? ?? session.userName;
        _employeeCode =
            teacherProfile?['employeeCode'] as String? ?? session.accountCode;
      });
    } catch (_) {
      if (!_isCurrentProfileLoad(generation, requestedSession?.token)) return;
      setState(() {
        _teacherName = widget.authService.currentSession?.userName;
        _employeeCode = widget.authService.currentSession?.accountCode;
      });
    }
  }

  bool _isCurrentProfileLoad(int generation, String? requestedToken) =>
      mounted &&
      generation == _profileLoadGeneration &&
      widget.authService.currentSession?.token == requestedToken;

  Future<void> _loadTeacherPeriod(AcademicPeriod period) async {
    final session = widget.authService.currentSession;
    if (session == null) return;
    final requestedToken = session.token;
    final key = '${period.academicYearId}:${period.semesterId}';
    final generation = ++_periodLoadGeneration;
    _pendingLeaveLoadGeneration++;
    _loadedPeriodKey = key;
    setState(() {
      _loadingClass = true;
      _classId = null;
      _className = null;
      _pendingLeaveCount = 0;
      _classLoadError = null;
      _pendingLeaveLoadFailed = false;
    });
    try {
      final stats = await _dashboardApi.getTeacherStats(
        token: session.token,
        academicYearId: period.academicYearId,
        semesterId: period.semesterId,
      );
      if (!_isCurrentPeriodLoad(generation, key, period, requestedToken)) {
        return;
      }
      setState(() {
        _classId = stats.classId;
        _className = stats.className;
        _loadingClass = false;
        _classLoadError = null;
      });
      await _loadPendingLeaveCount(period);
    } on BackendApiException catch (error) {
      if (!_isCurrentPeriodLoad(generation, key, period, requestedToken)) {
        return;
      }
      setState(() {
        _classId = null;
        _className = null;
        _loadingClass = false;
        _classLoadError = error.statusCode == 403
            ? 'Chưa được phân công chủ nhiệm trong học kỳ này.'
            : 'Không thể đồng bộ dữ liệu lớp. Vui lòng thử lại.';
      });
    } catch (_) {
      if (!_isCurrentPeriodLoad(generation, key, period, requestedToken)) {
        return;
      }
      setState(() {
        _classId = null;
        _className = null;
        _loadingClass = false;
        _classLoadError = 'Không thể đồng bộ dữ liệu lớp. Vui lòng thử lại.';
      });
    }
  }

  bool _isCurrentPeriodLoad(
    int generation,
    String requestKey,
    AcademicPeriod period,
    String requestedToken,
  ) =>
      mounted &&
      generation == _periodLoadGeneration &&
      _loadedPeriodKey == requestKey &&
      widget.authService.currentSession?.token == requestedToken &&
      _samePeriod(period, _selectedPeriod);

  @override
  Widget build(BuildContext context) {
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
                                _loadingClass
                                    ? 'Đang tải thông tin...'
                                    : _className == null
                                    ? 'Thông tin chủ nhiệm chưa sẵn sàng'
                                    : 'GVCN $_className',
                                style: const TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w900,
                                  color: Colors.white,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                _loadingClass
                                    ? '...'
                                    : 'Mã GV: ${_employeeCode ?? ''} • ${_teacherName ?? ''}',
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

                  if (_classLoadError != null) ...[
                    AppCard(
                      backgroundColor: AppColors.warningSoft,
                      child: Row(
                        children: [
                          const Icon(
                            Icons.sync_problem_outlined,
                            color: AppColors.warning,
                          ),
                          const SizedBox(width: AppSpacing.md),
                          Expanded(
                            child: Text(
                              _classLoadError!,
                              style: const TextStyle(
                                color: AppColors.ink,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                          TextButton(
                            onPressed: () {
                              final period = _selectedPeriod;
                              if (period != null) _loadTeacherPeriod(period);
                            },
                            child: const Text('Thử lại'),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: AppSpacing.md),
                  ],

                  const SizedBox(height: 24),

                  // Grid of Features
                  const SectionHeader(title: 'Chức năng giảng dạy & điều hành'),
                  Builder(
                    builder: (context) {
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
                                        backend: _backend,
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
                                      content: Text(
                                        'Đang tải dữ liệu lớp học, vui lòng đợi...',
                                      ),
                                      behavior: SnackBarBehavior.floating,
                                    ),
                                  );
                                  return;
                                }
                                final session =
                                    widget.authService.currentSession!;
                                final period = _selectedPeriod;
                                if (period == null) return;
                                Navigator.of(context).push(
                                  MaterialPageRoute<void>(
                                    builder: (_) => TeacherAttendanceScreen(
                                      token: session.token,
                                      date: period.referenceDate,
                                      apiClient: AttendanceApiClient(
                                        backend: _backend,
                                      ),
                                    ),
                                  ),
                                );
                              },
                            ),
                          if (_classId != null)
                            _FeatureButton(
                              title: 'Duyệt đơn xin nghỉ',
                              icon: Icons.assignment_turned_in_outlined,
                              color: AppColors.warning,
                              badgeCount: _pendingLeaveCount > 0
                                  ? _pendingLeaveCount
                                  : null,
                              hasLoadError: _pendingLeaveLoadFailed,
                              onTap: () {
                                final session =
                                    widget.authService.currentSession!;
                                Navigator.of(context)
                                    .push(
                                      MaterialPageRoute<void>(
                                        builder: (_) =>
                                            TeacherLeaveRequestsScreen(
                                              token: session.token,
                                              academicYearId: _selectedPeriod
                                                  ?.academicYearId,
                                              semesterId:
                                                  _selectedPeriod?.semesterId,
                                              notificationService:
                                                  widget.notificationService,
                                              apiClient: _leaveApi,
                                            ),
                                      ),
                                    )
                                    .then((_) {
                                      final period = _selectedPeriod;
                                      if (period != null) {
                                        _loadPendingLeaveCount(period);
                                      }
                                    });
                              },
                            ),
                          _FeatureButton(
                            title: 'Nhập & Upload điểm',
                            icon: Icons.grid_on_outlined,
                            color: AppColors.blue,
                            onTap: () {
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => GradesWebScreen(
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
                            title: 'Nhận xét định kỳ',
                            icon: Icons.rate_review_outlined,
                            color: AppColors.teal,
                            onTap: () {
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => PeriodicReviewsScreen(
                                    authService: widget.authService,
                                    backend: _backend,
                                    homeroomClassId: _classId,
                                  ),
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
                                  builder: (_) => AnnouncementsCreateScreen(
                                    token: widget
                                        .authService
                                        .currentSession!
                                        .token,
                                  ),
                                ),
                              );
                            },
                          ),
                          if (_classId != null)
                            _FeatureButton(
                              title: 'Hồ sơ lớp chủ nhiệm',
                              icon: Icons.groups_2_outlined,
                              color: AppColors.teal,
                              onTap: () {
                                Navigator.of(context).push(
                                  MaterialPageRoute<void>(
                                    builder: (_) => TeacherStatsScreen(
                                      token: widget
                                          .authService
                                          .currentSession!
                                          .token,
                                      apiClient: _dashboardApi,
                                      academicApiClient: _homeroomAcademicApi,
                                    ),
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
    this.hasLoadError = false,
  });

  final String title;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;
  final int? badgeCount;
  final bool hasLoadError;

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
            if (hasLoadError)
              const Positioned(
                top: 8,
                right: 8,
                child: Tooltip(
                  message: 'Chưa đồng bộ được dữ liệu',
                  child: Icon(
                    Icons.error_outline,
                    color: AppColors.warning,
                    size: 20,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
