import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class HomeroomStudentDetailScreen extends StatefulWidget {
  const HomeroomStudentDetailScreen({
    super.key,
    required this.token,
    required this.student,
    required this.semesterId,
    required this.semesterName,
    required this.apiClient,
  });

  final String token;
  final HomeroomStudentDto student;
  final int semesterId;
  final String semesterName;
  final HomeroomAcademicApiClient apiClient;

  @override
  State<HomeroomStudentDetailScreen> createState() =>
      _HomeroomStudentDetailScreenState();
}

class _HomeroomStudentDetailScreenState
    extends State<HomeroomStudentDetailScreen> {
  HomeroomStudentResultDto? _result;
  String? _error;
  bool _loading = true;
  int _requestVersion = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final version = ++_requestVersion;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await widget.apiClient.getStudentSemesterResult(
        token: widget.token,
        studentId: widget.student.id,
        semesterId: widget.semesterId,
      );
      if (!mounted || version != _requestVersion) return;
      if (result != null && result.studentId != widget.student.id) {
        throw const ParseException(
          'Kết quả học kỳ không thuộc học sinh đang xem.',
        );
      }
      if (result != null && result.semesterId != widget.semesterId) {
        throw const ParseException(
          'Kết quả học sinh không thuộc học kỳ đang xem.',
        );
      }
      setState(() {
        _result = result;
        _loading = false;
      });
    } catch (error) {
      if (!mounted || version != _requestVersion) return;
      setState(() {
        _result = null;
        _loading = false;
        _error = _errorMessage(error);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Tổng kết học sinh'),
      body: SafeArea(child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.error_outline_rounded,
                color: AppColors.danger,
                size: 36,
              ),
              const SizedBox(height: AppSpacing.sm),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppColors.muted),
              ),
              const SizedBox(height: AppSpacing.md),
              OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
            ],
          ),
        ),
      );
    }

    final result = _result;
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          _StudentIdentityCard(
            student: widget.student,
            semesterName: result?.semesterName ?? widget.semesterName,
          ),
          const SizedBox(height: AppSpacing.md),
          if (result == null)
            AppCard(
              backgroundColor: AppColors.warningSoft,
              child: const Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.hourglass_empty_rounded, color: AppColors.warning),
                  SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Text(
                      'Học sinh chưa có kết quả tổng kết trong học kỳ này. '
                      'Dữ liệu sẽ xuất hiện sau khi Admin tính và công bố tổng kết.',
                      style: TextStyle(color: AppColors.ink, height: 1.4),
                    ),
                  ),
                ],
              ),
            )
          else ...[
            const SectionHeader(title: 'Kết quả học kỳ'),
            GridView.count(
              crossAxisCount: 2,
              childAspectRatio: 1.55,
              mainAxisSpacing: AppSpacing.sm,
              crossAxisSpacing: AppSpacing.sm,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              children: [
                _ResultMetric(
                  icon: Icons.auto_graph_rounded,
                  label: 'GPA',
                  value: result.gpa?.toStringAsFixed(2) ?? '--',
                  color: AppColors.blue,
                ),
                _ResultMetric(
                  icon: Icons.emoji_events_outlined,
                  label: 'Xếp hạng',
                  value: result.rank == null ? '--' : '#${result.rank}',
                  color: AppColors.fptOrange,
                ),
                _ResultMetric(
                  icon: Icons.school_outlined,
                  label: 'Học lực',
                  value: _display(result.academicAbility),
                  color: AppColors.teal,
                ),
                _ResultMetric(
                  icon: Icons.volunteer_activism_outlined,
                  label: 'Hạnh kiểm',
                  value: _display(result.conduct),
                  color: AppColors.green,
                ),
              ],
            ),
            if (result.honor?.trim().isNotEmpty == true) ...[
              const SectionHeader(title: 'Ghi nhận'),
              AppCard(
                backgroundColor: AppColors.primarySoft,
                child: Row(
                  children: [
                    const Icon(
                      Icons.workspace_premium_outlined,
                      color: AppColors.fptOrange,
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    Expanded(
                      child: Text(
                        result.honor!,
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          color: AppColors.ink,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ],
      ),
    );
  }

  static String _display(String? value) =>
      value?.trim().isNotEmpty == true ? value!.trim() : '--';

  static String _errorMessage(Object error) =>
      error.toString().replaceFirst(RegExp(r'^[A-Za-z]+Exception:\s*'), '');
}

class _StudentIdentityCard extends StatelessWidget {
  const _StudentIdentityCard({
    required this.student,
    required this.semesterName,
  });

  final HomeroomStudentDto student;
  final String semesterName;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      gradient: const LinearGradient(
        colors: [Color(0xFFFFF3E9), Color(0xFFFFFBF7)],
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 28,
            backgroundColor: AppColors.fptOrange,
            child: Text(
              _initials(student.name),
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w900,
              ),
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
                    fontSize: 17,
                    fontWeight: FontWeight.w900,
                    color: AppColors.ink,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${student.studentCode} · ${student.className}',
                  style: const TextStyle(color: AppColors.muted, fontSize: 12),
                ),
                const SizedBox(height: AppSpacing.sm),
                StatusPill(label: semesterName, compact: true),
              ],
            ),
          ),
        ],
      ),
    );
  }

  static String _initials(String name) {
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty || parts.first.isEmpty) return '--';
    if (parts.length == 1) return parts.first[0].toUpperCase();
    return '${parts[parts.length - 2][0]}${parts.last[0]}'.toUpperCase();
  }
}

class _ResultMetric extends StatelessWidget {
  const _ResultMetric({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });

  final IconData icon;
  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: AppSpacing.md,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: color, size: 20),
          const Spacer(),
          Text(
            value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              color: color,
              fontSize: 18,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: const TextStyle(color: AppColors.muted, fontSize: 11),
          ),
        ],
      ),
    );
  }
}
