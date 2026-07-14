import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

enum _TuitionFilter { all, paid, outstanding, noBills }

class TeacherTuitionScreen extends StatefulWidget {
  const TeacherTuitionScreen({
    super.key,
    required this.token,
    required this.classId,
    required this.semesterId,
    this.apiClient,
  });

  final String token;
  final int classId;
  final int semesterId;
  final TuitionBillApiClient? apiClient;

  @override
  State<TeacherTuitionScreen> createState() => _TeacherTuitionScreenState();
}

class _TeacherTuitionScreenState extends State<TeacherTuitionScreen> {
  late final TuitionBillApiClient _api =
      widget.apiClient ?? TuitionBillApiClient(backend: BackendApiClient());
  TeacherTuitionSummaryDto? _summary;
  String? _error;
  bool _loading = true;
  _TuitionFilter _filter = _TuitionFilter.all;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await _api.getTeacherClassSummary(
        token: widget.token,
        classId: widget.classId,
        semesterId: widget.semesterId,
      );
      if (!mounted) return;
      setState(() {
        _summary = result;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _summary = null;
        _loading = false;
        _error = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  List<TeacherTuitionStudentDto> _filtered(TeacherTuitionSummaryDto summary) {
    return summary.students
        .where(
          (student) => switch (_filter) {
            _TuitionFilter.all => true,
            _TuitionFilter.paid => student.isPaid,
            _TuitionFilter.outstanding => student.hasOutstanding,
            _TuitionFilter.noBills => student.paymentState == 'NO_BILLS',
          },
        )
        .toList(growable: false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Học phí lớp chủ nhiệm'),
      body: SafeArea(child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    final summary = _summary;
    if (summary == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                _error ?? 'Không có dữ liệu học phí.',
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

    final students = _filtered(summary);
    return Column(
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
          color: Colors.white,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Lớp ${summary.className} · ${summary.semesterName}',
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: AppColors.ink,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'Đã hoàn tất ${summary.paidStudents}/${summary.totalStudents} · '
                'Còn phải thu ${summary.outstandingStudents} · '
                'Chưa phát sinh ${summary.studentsWithoutBills}',
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: AppColors.muted,
                ),
              ),
            ],
          ),
        ),
        const Divider(height: 1, color: AppColors.line),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 8),
          child: Row(
            children: [
              _FilterChip(
                label: 'Tất cả',
                selected: _filter == _TuitionFilter.all,
                onTap: () => setState(() => _filter = _TuitionFilter.all),
              ),
              _FilterChip(
                label: 'Đã hoàn tất',
                selected: _filter == _TuitionFilter.paid,
                onTap: () => setState(() => _filter = _TuitionFilter.paid),
              ),
              _FilterChip(
                label: 'Còn phải thu',
                selected: _filter == _TuitionFilter.outstanding,
                onTap: () =>
                    setState(() => _filter = _TuitionFilter.outstanding),
              ),
              _FilterChip(
                label: 'Chưa phát sinh',
                selected: _filter == _TuitionFilter.noBills,
                onTap: () => setState(() => _filter = _TuitionFilter.noBills),
              ),
            ],
          ),
        ),
        Expanded(
          child: students.isEmpty
              ? const Center(
                  child: Text(
                    'Không có học sinh ở trạng thái này.',
                    style: TextStyle(color: AppColors.muted),
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
                  itemCount: students.length,
                  itemBuilder: (context, index) =>
                      _StudentTuitionTile(student: students[index]),
                ),
        ),
      ],
    );
  }
}

class _FilterChip extends StatelessWidget {
  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ChoiceChip(
        label: Text(label),
        selected: selected,
        onSelected: (_) => onTap(),
        selectedColor: AppColors.primarySoft,
        side: BorderSide(
          color: selected ? AppColors.fptOrange : AppColors.line,
        ),
        labelStyle: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.bold,
          color: selected ? AppColors.fptOrange : AppColors.ink,
        ),
      ),
    );
  }
}

class _StudentTuitionTile extends StatelessWidget {
  const _StudentTuitionTile({required this.student});

  final TeacherTuitionStudentDto student;

  @override
  Widget build(BuildContext context) {
    final (label, color, background) = switch (student.paymentState) {
      'PAID' => ('Đã hoàn tất', AppColors.success, AppColors.successSoft),
      'PROCESSING' => (
        'Đang xử lý',
        AppColors.fptOrange,
        AppColors.primarySoft,
      ),
      'NO_BILLS' => ('Chưa phát sinh', AppColors.muted, AppColors.background),
      _ => ('Còn phải thu', AppColors.danger, AppColors.dangerSoft),
    };
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.line.withValues(alpha: 0.4)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  student.studentName,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppColors.ink,
                  ),
                ),
                if (student.studentCode.isNotEmpty)
                  Text(
                    student.studentCode,
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppColors.muted,
                    ),
                  ),
                if (student.outstandingAmount > 0)
                  Text(
                    'Còn ${_formatMoney(student.outstandingAmount)} đ',
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppColors.danger,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: background,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              label,
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
          ),
        ],
      ),
    );
  }

  static String _formatMoney(double value) {
    final digits = value.round().toString();
    return digits.replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => '.');
  }
}
