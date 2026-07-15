import 'package:flutter/material.dart';
import '../../src/api/api.dart';
import '../design_system/app_colors.dart';
import 'academic_period_scope.dart';

class GradesScreen extends StatefulWidget {
  const GradesScreen({
    super.key,
    required this.token,
    this.studentId,
    this.studentName,
    this.apiClient,
  });
  final String token;
  final int? studentId;
  final String? studentName;
  final GradebookApiClient? apiClient;
  @override
  State<GradesScreen> createState() => _GradesScreenState();
}

class _GradesScreenState extends State<GradesScreen> {
  late final GradebookApiClient api = widget.apiClient ?? GradebookApiClient();
  Map<String, dynamic>? transcript;
  bool loading = true;
  String? error;
  String? loadedPeriod;
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    if (period == null) return;
    final key = '${period.academicYearId}-${period.semesterId}';
    if (key != loadedPeriod) {
      loadedPeriod = key;
      _load(period);
    }
  }

  Future<void> _load(AcademicPeriod period) async {
    final requestKey = '${period.academicYearId}-${period.semesterId}';
    setState(() {
      loading = true;
      error = null;
      transcript = null;
    });
    try {
      final data = await api.getTranscript(
        token: widget.token,
        academicYearId: period.academicYearId,
        semesterId: period.semesterId,
        studentId: widget.studentId,
      );
      if (mounted && loadedPeriod == requestKey) {
        setState(() => transcript = data);
      }
    } catch (e) {
      if (mounted && loadedPeriod == requestKey) {
        setState(() => error = '$e');
      }
    } finally {
      if (mounted && loadedPeriod == requestKey) {
        setState(() => loading = false);
      }
    }
  }

  List<Map<String, dynamic>> get subjects =>
      (transcript?['subjects'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .toList();
  double? get overall {
    final values = subjects
        .map((s) => (s['average'] as num?)?.toDouble())
        .whereType<double>()
        .toList();
    return values.isEmpty
        ? null
        : values.reduce((a, b) => a + b) / values.length;
  }

  @override
  Widget build(BuildContext context) {
    final controller = AcademicPeriodScope.maybeOf(context);
    final period = controller?.selected;
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(
          widget.studentName == null
              ? 'Bảng điểm'
              : 'Bảng điểm · ${widget.studentName}',
        ),
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () => period == null ? Future.value() : _load(period),
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (controller != null && controller.periods.isNotEmpty)
                    DropdownButtonFormField<AcademicPeriod>(
                      key: ValueKey(period),
                      initialValue: period,
                      decoration: const InputDecoration(
                        labelText: 'Năm học · học kỳ',
                      ),
                      items: controller.periods
                          .map(
                            (p) => DropdownMenuItem(
                              value: p,
                              child: Text(p.label),
                            ),
                          )
                          .toList(),
                      onChanged: (p) {
                        if (p != null) controller.select(p);
                      },
                    ),
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [AppColors.fptOrange, Color(0xffff8a3d)],
                      ),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.school, color: Colors.white, size: 42),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                transcript?['studentName'] as String? ??
                                    widget.studentName ??
                                    'Học sinh',
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 18,
                                  fontWeight: FontWeight.w900,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                period?.label ?? '',
                                style: const TextStyle(color: Colors.white70),
                              ),
                            ],
                          ),
                        ),
                        Column(
                          children: [
                            Text(
                              overall?.toStringAsFixed(1) ?? '—',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 30,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            const Text(
                              'TB môn đã công bố',
                              style: TextStyle(
                                color: Colors.white70,
                                fontSize: 11,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  if (error != null)
                    Card(
                      color: AppColors.dangerSoft,
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Text(error!),
                      ),
                    ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: _StatCard(
                          label: 'Môn đã công bố',
                          value: '${subjects.length}',
                          icon: Icons.menu_book,
                          color: AppColors.blue,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _StatCard(
                          label: 'Đã đủ điểm',
                          value:
                              '${subjects.where((s) => s['complete'] == true).length}',
                          icon: Icons.verified,
                          color: AppColors.green,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 18),
                  const Text(
                    'Bảng điểm chi tiết',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w900,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: 10),
                  _GradeFormulaCard(subjects: subjects),
                  const SizedBox(height: 12),
                  if (subjects.isEmpty)
                    const Card(
                      child: Padding(
                        padding: EdgeInsets.all(28),
                        child: Center(
                          child: Text(
                            'Chưa có bảng điểm nào được nhà trường công bố.',
                          ),
                        ),
                      ),
                    ),
                  ...subjects.map((subject) {
                    final scores =
                        (subject['scores'] as List<dynamic>? ?? const [])
                            .whereType<Map<String, dynamic>>()
                            .toList();
                    return Card(
                      margin: const EdgeInsets.only(bottom: 12),
                      child: ExpansionTile(
                        initiallyExpanded: true,
                        title: Text(
                          subject['subjectName'] as String,
                          style: const TextStyle(fontWeight: FontWeight.w800),
                        ),
                        subtitle: Text(
                          subject['complete'] == true
                              ? 'Đã đủ đầu điểm'
                              : 'Đang cập nhật',
                        ),
                        trailing: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 7,
                          ),
                          decoration: BoxDecoration(
                            color: AppColors.primarySoft,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            (subject['average'] as num?)?.toStringAsFixed(1) ??
                                '—',
                            style: const TextStyle(
                              color: AppColors.fptOrange,
                              fontWeight: FontWeight.w900,
                              fontSize: 16,
                            ),
                          ),
                        ),
                        children: [
                          SingleChildScrollView(
                            scrollDirection: Axis.horizontal,
                            child: DataTable(
                              headingRowColor: WidgetStatePropertyAll(
                                AppColors.background,
                              ),
                              columns: [
                                ...scores.map(
                                  (s) => DataColumn(
                                    label: Column(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        Text(s['name'] as String),
                                        Text(
                                          _scoreSubtitle(s),
                                          style: const TextStyle(
                                            fontSize: 10,
                                            color: AppColors.muted,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                                const DataColumn(label: Text('ĐTB')),
                              ],
                              rows: [
                                DataRow(
                                  cells: [
                                    ...scores.map(
                                      (s) => DataCell(
                                        Text(
                                          _scoreDisplay(s),
                                          style: const TextStyle(
                                            fontWeight: FontWeight.w700,
                                          ),
                                        ),
                                      ),
                                    ),
                                    DataCell(
                                      Text(
                                        (subject['average'] as num?)
                                                ?.toStringAsFixed(1) ??
                                            '—',
                                        style: const TextStyle(
                                          fontWeight: FontWeight.w900,
                                          color: AppColors.fptOrange,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    );
                  }),
                ],
              ),
            ),
    );
  }

  static String _scoreSubtitle(Map<String, dynamic> score) {
    return switch (score['assessmentType']) {
      'PASS_FAIL' => 'Đạt / Chưa đạt',
      'COMMENT' => 'Nhận xét',
      _ => 'HS ${score['weight']}',
    };
  }

  static String _scoreDisplay(Map<String, dynamic> score) {
    return switch (score['assessmentType']) {
      'PASS_FAIL' => switch (score['comment']) {
        'PASS' => 'Đạt',
        'FAIL' => 'Chưa đạt',
        _ => '—',
      },
      'COMMENT' =>
        (score['comment'] as String?)?.trim().isNotEmpty == true
            ? score['comment'] as String
            : '—',
      _ => (score['score'] as num?)?.toStringAsFixed(1) ?? '—',
    };
  }
}

class _GradeFormulaCard extends StatelessWidget {
  const _GradeFormulaCard({required this.subjects});
  final List<Map<String, dynamic>> subjects;

  @override
  Widget build(BuildContext context) {
    final configured = <String, Map<String, dynamic>>{};
    for (final subject in subjects) {
      for (final score in (subject['scores'] as List<dynamic>? ?? const [])) {
        if (score is Map<String, dynamic>) {
          if (score['assessmentType'] != 'SCORE') continue;
          configured['${score['name']}-${score['weight']}'] = score;
        }
      }
    }
    final terms = configured.values
        .map((score) => '${score['name']} × ${score['weight']}')
        .join(' + ');
    final weights = configured.values.fold<int>(
      0,
      (sum, score) => sum + (score['weight'] as num).toInt(),
    );
    return Card(
      child: ExpansionTile(
        leading: const Icon(Icons.calculate_outlined, color: AppColors.blue),
        title: const Text(
          'Cách hệ thống tính điểm',
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
        subtitle: const Text(
          'Chỉ đầu điểm số được tính; nhận xét và Đạt/Chưa đạt không tính ĐTB',
        ),
        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        children: [
          const Align(
            alignment: Alignment.centerLeft,
            child: Text(
              'Điểm trung bình môn = Tổng (điểm × hệ số) / Tổng hệ số. Kết quả được làm tròn 1 chữ số thập phân.',
            ),
          ),
          if (configured.isNotEmpty) ...[
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                'Theo cấu hình hiện tại: ($terms) / $weights',
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
          ],
          const SizedBox(height: 8),
          const Align(
            alignment: Alignment.centerLeft,
            child: Text(
              'Điểm trung bình học kỳ = Tổng điểm trung bình các môn / số môn có điểm. Admin thực hiện tính và công bố trước khi bảng điểm hiển thị tại đây.',
            ),
          ),
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });
  final String label, value;
  final IconData icon;
  final Color color;
  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Icon(icon, color: color),
          const SizedBox(width: 10),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                value,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                ),
              ),
              Text(
                label,
                style: const TextStyle(fontSize: 11, color: AppColors.muted),
              ),
            ],
          ),
        ],
      ),
    ),
  );
}
