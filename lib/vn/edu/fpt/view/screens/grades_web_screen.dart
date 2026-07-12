import 'package:flutter/material.dart';
import '../../src/api/api.dart';
import '../design_system/app_colors.dart';
import 'academic_period_scope.dart';

class GradesWebScreen extends StatefulWidget {
  const GradesWebScreen({super.key, required this.token});
  final String token;
  @override
  State<GradesWebScreen> createState() => _GradesWebScreenState();
}

class _GradesWebScreenState extends State<GradesWebScreen> {
  final api = GradebookApiClient();
  List<Map<String, dynamic>> assignments = [], scoreRows = [];
  Map<String, dynamic>? assignment, book, item;
  final values = <int, double?>{};
  bool loading = false, saving = false;
  String? error;
  int? loadedYear;
  String? loadedPeriod;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    if (period == null) return;
    final periodKey = '${period.academicYearId}-${period.semesterId}';
    if (loadedYear != period.academicYearId) {
      loadedYear = period.academicYearId;
      loadedPeriod = periodKey;
      _loadAssignments(period.academicYearId);
    } else if (loadedPeriod != periodKey) {
      loadedPeriod = periodKey;
      if (assignment != null) _loadBook();
    }
  }

  Future<void> _loadAssignments(int yearId) async {
    setState(() => loading = true);
    try {
      final rows = await api.getMyAssignments(
        token: widget.token,
        academicYearId: yearId,
      );
      if (!mounted) return;
      setState(() => assignments = rows);
      if (rows.isNotEmpty) await _selectClass(rows.first['classId'] as int);
    } catch (e) {
      if (mounted) setState(() => error = '$e');
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  List<Map<String, dynamic>> get classes {
    final seen = <int>{};
    return assignments.where((a) => seen.add(a['classId'] as int)).toList();
  }

  List<Map<String, dynamic>> get subjects =>
      assignments.where((a) => a['classId'] == assignment?['classId']).toList();
  List<Map<String, dynamic>> get editableItems => book == null
      ? []
      : (book!['items'] as List)
            .whereType<Map<String, dynamic>>()
            .where((i) => i['entryRole'] != 'ADMIN')
            .toList();
  List<Map<String, dynamic>> get students => item == null
      ? []
      : scoreRows.where((r) => r['gradeItemId'] == item!['id']).toList();

  Future<void> _selectClass(int classId) async {
    setState(
      () => assignment = assignments.firstWhere((a) => a['classId'] == classId),
    );
    await _loadBook();
  }

  Future<void> _selectSubject(int subjectId) async {
    setState(
      () => assignment = assignments.firstWhere(
        (a) =>
            a['classId'] == assignment!['classId'] &&
            a['subjectId'] == subjectId,
      ),
    );
    await _loadBook();
  }

  Future<void> _loadBook() async {
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    if (assignment == null || period == null) return;
    setState(() => loading = true);
    try {
      final loaded = await api.getGradeBook(
        token: widget.token,
        classId: assignment!['classId'] as int,
        subjectId: assignment!['subjectId'] as int,
        semesterId: period.semesterId,
      );
      final rows = await api.getStudents(
        token: widget.token,
        gradeBookId: loaded['id'] as int,
      );
      final allowed = (loaded['items'] as List)
          .whereType<Map<String, dynamic>>()
          .where((i) => i['entryRole'] != 'ADMIN')
          .toList();
      if (!mounted) return;
      setState(() {
        book = loaded;
        scoreRows = rows;
        item = allowed.isEmpty ? null : allowed.first;
        _fillValues();
      });
    } catch (e) {
      if (mounted) setState(() => error = '$e');
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  void _fillValues() {
    values.clear();
    if (item == null) return;
    for (final row in scoreRows.where((r) => r['gradeItemId'] == item!['id'])) {
      values[row['studentId'] as int] = (row['score'] as num?)?.toDouble();
    }
  }

  void _selectItem(int id) {
    setState(() {
      item = editableItems.firstWhere((i) => i['id'] == id);
      _fillValues();
    });
  }

  Future<void> _upload() async {
    if (item == null) return;
    if (values.values.any((v) => v != null && (v < 0 || v > 10))) {
      setState(() => error = 'Điểm phải từ 0 đến 10.');
      return;
    }
    setState(() => saving = true);
    try {
      await api.updateScores(
        token: widget.token,
        gradeItemId: item!['id'] as int,
        entries: students
            .map(
              (s) => {
                'studentId': s['studentId'],
                'score': values[s['studentId']],
                'isGraded': values[s['studentId']] != null,
              },
            )
            .toList(),
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã upload điểm cho admin.')),
        );
        await _loadBook();
      }
    } catch (e) {
      if (mounted) setState(() => error = '$e');
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Nhập & upload điểm')),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () => loadedYear == null
                  ? Future.value()
                  : _loadAssignments(loadedYear!),
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (period != null)
                    Text(
                      period.label,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        color: AppColors.muted,
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
                  const SizedBox(height: 12),
                  DropdownButtonFormField<int>(
                    initialValue: assignment?['classId'] as int?,
                    decoration: const InputDecoration(
                      labelText: '1. Chọn lớp được phân công',
                    ),
                    items: classes
                        .map(
                          (a) => DropdownMenuItem(
                            value: a['classId'] as int,
                            child: Text(a['className'] as String),
                          ),
                        )
                        .toList(),
                    onChanged: (v) {
                      if (v != null) _selectClass(v);
                    },
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<int>(
                    initialValue: assignment?['subjectId'] as int?,
                    decoration: const InputDecoration(labelText: '2. Chọn môn'),
                    items: subjects
                        .map(
                          (a) => DropdownMenuItem(
                            value: a['subjectId'] as int,
                            child: Text(
                              '${a['subjectName']} (${a['subjectCode']})',
                            ),
                          ),
                        )
                        .toList(),
                    onChanged: (v) {
                      if (v != null) _selectSubject(v);
                    },
                  ),
                  const SizedBox(height: 12),
                  if (book != null)
                    DropdownButtonFormField<int>(
                      initialValue: item?['id'] as int?,
                      decoration: const InputDecoration(
                        labelText: '3. Chọn đầu điểm giáo viên được nhập',
                      ),
                      items: editableItems
                          .map(
                            (i) => DropdownMenuItem(
                              value: i['id'] as int,
                              child: Text(
                                '${i['name']} · hệ số ${i['weight']} · môn ${assignment?['subjectName']}',
                              ),
                            ),
                          )
                          .toList(),
                      onChanged: (v) {
                        if (v != null) _selectItem(v);
                      },
                    ),
                  const SizedBox(height: 16),
                  if (assignments.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(32),
                      child: Center(
                        child: Text(
                          'Bạn chưa được admin phân công lớp/môn trong năm học này.',
                        ),
                      ),
                    ),
                  if (item != null) ...[
                    Card(
                      color: AppColors.blueSoft,
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Text(
                          '${assignment?['className']} · ${assignment?['subjectName']} · ${item?['name']} (hệ số ${item?['weight']})',
                          style: const TextStyle(fontWeight: FontWeight.w800),
                        ),
                      ),
                    ),
                    ...students.map(
                      (student) => Card(
                        child: ListTile(
                          leading: CircleAvatar(
                            child: Text(
                              (student['studentName'] as String).substring(
                                0,
                                1,
                              ),
                            ),
                          ),
                          title: Text(student['studentName'] as String),
                          subtitle: Text(student['studentCode'] as String),
                          trailing: SizedBox(
                            width: 82,
                            child: TextFormField(
                              key: ValueKey(
                                '${item!['id']}-${student['studentId']}',
                              ),
                              initialValue: values[student['studentId']]
                                  ?.toString(),
                              keyboardType:
                                  const TextInputType.numberWithOptions(
                                    decimal: true,
                                  ),
                              decoration: const InputDecoration(
                                hintText: '0–10',
                              ),
                              onChanged: (v) =>
                                  values[student['studentId'] as int] =
                                      double.tryParse(v),
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      onPressed: saving ? null : _upload,
                      icon: const Icon(Icons.cloud_upload),
                      label: Text(
                        saving ? 'Đang upload…' : 'Upload điểm cho admin',
                      ),
                    ),
                  ],
                ],
              ),
            ),
    );
  }
}
