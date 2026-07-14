import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherStatsScreen extends StatefulWidget {
  const TeacherStatsScreen({super.key, required this.token, this.apiClient});

  final String token;
  final DashboardApiClient? apiClient;

  @override
  State<TeacherStatsScreen> createState() => _TeacherStatsScreenState();
}

class _TeacherStatsScreenState extends State<TeacherStatsScreen> {
  late final DashboardApiClient _api =
      widget.apiClient ?? DashboardApiClient(backend: BackendApiClient());
  TeacherDashboardStatsDto? _stats;
  String? _periodKey;
  String? _error;
  bool _loading = true;
  bool _didResolvePeriod = false;
  int _requestVersion = 0;

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
        _error = 'Chưa có kỳ học để tải thống kê.';
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
      final result = await _api.getTeacherStats(
        token: widget.token,
        academicYearId: period.academicYearId,
        semesterId: period.semesterId,
      );
      if (!mounted || version != _requestVersion) return;
      setState(() {
        _stats = result;
        _loading = false;
      });
    } catch (error) {
      if (!mounted || version != _requestVersion) return;
      setState(() {
        _stats = null;
        _loading = false;
        _error = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Thống kê lớp học'),
      body: SafeArea(child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    final stats = _stats;
    if (stats == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                _error ?? 'Không có dữ liệu thống kê.',
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
    return ListView(
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
      ],
    );
  }

  static String _percent(double? value) =>
      value == null ? 'Chưa có dữ liệu' : '${value.toStringAsFixed(1)}%';

  static double? _progress(double? value, double maximum) =>
      value == null ? null : (value / maximum).clamp(0, 1).toDouble();
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
