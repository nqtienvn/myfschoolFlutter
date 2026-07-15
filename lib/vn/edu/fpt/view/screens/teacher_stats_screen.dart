import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/homeroom_student_detail_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherStatsScreen extends StatefulWidget {
  const TeacherStatsScreen({
    super.key,
    required this.token,
    this.apiClient,
    this.academicApiClient,
  });

  final String token;
  final DashboardApiClient? apiClient;
  final HomeroomAcademicApiClient? academicApiClient;

  @override
  State<TeacherStatsScreen> createState() => _TeacherStatsScreenState();
}

class _TeacherStatsScreenState extends State<TeacherStatsScreen> {
  late final BackendApiClient _backend = BackendApiClient();
  late final DashboardApiClient _api =
      widget.apiClient ?? DashboardApiClient(backend: _backend);
  late final HomeroomAcademicApiClient _academicApi =
      widget.academicApiClient ?? HomeroomAcademicApiClient(backend: _backend);
  TeacherDashboardStatsDto? _stats;
  HomeroomClassDetailDto? _classDetail;
  HomeroomClassRankingDto? _ranking;
  final TextEditingController _searchController = TextEditingController();
  String _query = '';
  String? _periodKey;
  String? _error;
  bool _loading = true;
  bool _didResolvePeriod = false;
  int _requestVersion = 0;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    final key = period == null
        ? null
        : '${period.academicYearId}:${period.semesterId}';
    if (_didResolvePeriod && _periodKey == key) return;
    _didResolvePeriod = true;
    _periodKey = key;
    if (period == null) {
      _requestVersion++;
      setState(() {
        _loading = false;
        _stats = null;
        _classDetail = null;
        _ranking = null;
        _error = 'Chưa có kỳ học để tải hồ sơ lớp.';
      });
      return;
    }
    _load(period);
  }

  Future<void> _load(AcademicPeriod period) async {
    final version = ++_requestVersion;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final stats = await _api.getTeacherStats(
        token: widget.token,
        academicYearId: period.academicYearId,
        semesterId: period.semesterId,
      );
      final academicData = await Future.wait<Object>([
        _academicApi.getClassDetail(
          token: widget.token,
          classId: stats.classId,
        ),
        _academicApi.getClassRanking(
          token: widget.token,
          classId: stats.classId,
          semesterId: period.semesterId,
        ),
      ]);
      final classDetail = academicData[0] as HomeroomClassDetailDto;
      final ranking = academicData[1] as HomeroomClassRankingDto;
      _validateResponse(
        period: period,
        stats: stats,
        classDetail: classDetail,
        ranking: ranking,
      );
      if (!mounted || version != _requestVersion) return;
      setState(() {
        _stats = stats;
        _classDetail = classDetail;
        _ranking = ranking;
        _loading = false;
      });
    } catch (error) {
      if (!mounted || version != _requestVersion) return;
      setState(() {
        _stats = null;
        _classDetail = null;
        _ranking = null;
        _loading = false;
        _error = _errorMessage(error);
      });
    }
  }

  static void _validateResponse({
    required AcademicPeriod period,
    required TeacherDashboardStatsDto stats,
    required HomeroomClassDetailDto classDetail,
    required HomeroomClassRankingDto ranking,
  }) {
    if (stats.classId != classDetail.id || stats.classId != ranking.classId) {
      throw const ParseException(
        'Backend trả về dữ liệu không cùng một lớp chủ nhiệm.',
      );
    }
    if (classDetail.academicYearId != period.academicYearId ||
        ranking.semesterId != period.semesterId) {
      throw const ParseException(
        'Backend trả về hồ sơ lớp không đúng năm học hoặc học kỳ đã chọn.',
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Hồ sơ lớp chủ nhiệm'),
      body: SafeArea(child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    final stats = _stats;
    final classDetail = _classDetail;
    final ranking = _ranking;
    if (stats == null || classDetail == null || ranking == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                _error ?? 'Không có dữ liệu hồ sơ lớp.',
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppColors.muted),
              ),
              const SizedBox(height: AppSpacing.md),
              OutlinedButton(
                onPressed: () {
                  final period = AcademicPeriodScope.maybeOf(context)?.selected;
                  if (period != null) _load(period);
                },
                child: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }

    final rows = _mergeStudents(classDetail, ranking);
    final normalizedQuery = _query.trim().toLowerCase();
    final visibleRows = normalizedQuery.isEmpty
        ? rows
        : rows
              .where(
                (row) =>
                    row.student.name.toLowerCase().contains(normalizedQuery) ||
                    row.student.studentCode.toLowerCase().contains(
                      normalizedQuery,
                    ),
              )
              .toList(growable: false);

    return RefreshIndicator(
      onRefresh: () async {
        final period = AcademicPeriodScope.maybeOf(context)?.selected;
        if (period != null) await _load(period);
      },
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          SectionHeader(title: 'Tổng quan lớp ${stats.className}'),
          Text(
            '${stats.academicYearName} · ${stats.semesterName}',
            style: const TextStyle(color: AppColors.muted, fontSize: 12),
          ),
          const SizedBox(height: AppSpacing.md),
          AppCard(
            child: Column(
              children: [
                _StatItem(
                  label: 'Tỷ lệ chuyên cần trung bình',
                  value: _percent(stats.attendanceRate),
                  progress: _progress(stats.attendanceRate, 100),
                  color: AppColors.green,
                ),
                const Divider(height: AppSpacing.lg),
                _StatItem(
                  label: 'Điểm trung bình học tập',
                  value: stats.averageGpa == null
                      ? 'Chưa có dữ liệu'
                      : '${stats.averageGpa!.toStringAsFixed(1)} / 10',
                  progress: _progress(stats.averageGpa, 10),
                  color: AppColors.blue,
                ),
                const Divider(height: AppSpacing.lg),
                _StatItem(
                  label: 'Tương tác phụ huynh (đã đọc)',
                  value: _percent(stats.parentReadRate),
                  progress: _progress(stats.parentReadRate, 100),
                  color: AppColors.fptOrange,
                ),
              ],
            ),
          ),
          SectionHeader(title: 'Học sinh (${classDetail.students.length})'),
          TextField(
            controller: _searchController,
            onChanged: (value) => setState(() => _query = value),
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(
              hintText: 'Tìm theo họ tên hoặc mã học sinh',
              prefixIcon: const Icon(Icons.search_rounded),
              suffixIcon: _query.isEmpty
                  ? null
                  : IconButton(
                      tooltip: 'Xóa tìm kiếm',
                      onPressed: () {
                        FocusScope.of(context).unfocus();
                        _searchController.clear();
                        setState(() => _query = '');
                      },
                      icon: const Icon(Icons.close_rounded),
                    ),
              filled: true,
              fillColor: AppColors.surface,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: AppColors.line),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: AppColors.line),
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.md),
          if (visibleRows.isEmpty)
            const AppCard(
              child: Text(
                'Không tìm thấy học sinh phù hợp.',
                textAlign: TextAlign.center,
                style: TextStyle(color: AppColors.muted),
              ),
            )
          else
            for (final row in visibleRows) ...[
              _StudentResultCard(
                row: row,
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => HomeroomStudentDetailScreen(
                      token: widget.token,
                      student: row.student,
                      semesterId: ranking.semesterId,
                      semesterName: ranking.semesterName,
                      apiClient: _academicApi,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.sm),
            ],
        ],
      ),
    );
  }

  static List<_StudentRow> _mergeStudents(
    HomeroomClassDetailDto classDetail,
    HomeroomClassRankingDto ranking,
  ) {
    final rankingByStudent = {
      for (final entry in ranking.rankings) entry.studentId: entry,
    };
    final rows = classDetail.students
        .map(
          (student) => _StudentRow(
            student: student,
            ranking: rankingByStudent[student.id],
          ),
        )
        .toList();
    rows.sort((left, right) {
      final leftRank = left.ranking?.rank;
      final rightRank = right.ranking?.rank;
      if (leftRank != null && rightRank != null) {
        return leftRank.compareTo(rightRank);
      }
      if (leftRank != null) return -1;
      if (rightRank != null) return 1;
      return left.student.name.compareTo(right.student.name);
    });
    return rows;
  }

  static String _percent(double? value) =>
      value == null ? 'Chưa có dữ liệu' : '${value.toStringAsFixed(1)}%';

  static String _errorMessage(Object error) =>
      error.toString().replaceFirst(RegExp(r'^[A-Za-z]+Exception:\s*'), '');

  static double? _progress(double? value, double maximum) =>
      value == null ? null : (value / maximum).clamp(0, 1).toDouble();
}

class _StudentRow {
  const _StudentRow({required this.student, required this.ranking});

  final HomeroomStudentDto student;
  final HomeroomRankEntryDto? ranking;
}

class _StudentResultCard extends StatelessWidget {
  const _StudentResultCard({required this.row, required this.onTap});

  final _StudentRow row;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ranking = row.ranking;
    return Semantics(
      button: true,
      label: 'Xem tổng kết ${row.student.name}',
      child: AppCard(
        padding: 0,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 22,
                  backgroundColor: ranking == null
                      ? AppColors.warningSoft
                      : AppColors.blueSoft,
                  child: Text(
                    ranking == null ? '–' : '#${ranking.rank}',
                    style: TextStyle(
                      color: ranking == null
                          ? AppColors.warning
                          : AppColors.blue,
                      fontWeight: FontWeight.w900,
                      fontSize: 12,
                    ),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        row.student.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: AppColors.ink,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        row.student.studentCode,
                        style: const TextStyle(
                          color: AppColors.muted,
                          fontSize: 12,
                        ),
                      ),
                      if (ranking != null) ...[
                        const SizedBox(height: AppSpacing.sm),
                        Wrap(
                          spacing: 6,
                          runSpacing: 6,
                          children: [
                            StatusPill(
                              label: ranking.gpa == null
                                  ? 'GPA --'
                                  : 'GPA ${ranking.gpa!.toStringAsFixed(2)}',
                              foreground: AppColors.blue,
                              background: AppColors.blueSoft,
                              compact: true,
                            ),
                            if (ranking.academicAbility?.trim().isNotEmpty ==
                                true)
                              StatusPill(
                                label: ranking.academicAbility!.trim(),
                                foreground: AppColors.teal,
                                background: AppColors.tealSoft,
                                compact: true,
                              ),
                            if (ranking.conduct?.trim().isNotEmpty == true)
                              StatusPill(
                                label: 'Hạnh kiểm ${ranking.conduct!.trim()}',
                                foreground: AppColors.green,
                                background: AppColors.greenSoft,
                                compact: true,
                              ),
                          ],
                        ),
                      ] else ...[
                        const SizedBox(height: AppSpacing.sm),
                        const StatusPill(
                          label: 'Chưa tổng kết',
                          foreground: AppColors.warning,
                          background: AppColors.warningSoft,
                          compact: true,
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                const Icon(Icons.chevron_right_rounded, color: AppColors.quiet),
              ],
            ),
          ),
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
  final double? progress;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                label,
                style: const TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.bold,
                  color: AppColors.ink,
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.md),
            Text(
              value,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.bold,
                color: progress == null ? AppColors.muted : color,
              ),
            ),
          ],
        ),
        if (progress != null) ...[
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
      ],
    );
  }
}
