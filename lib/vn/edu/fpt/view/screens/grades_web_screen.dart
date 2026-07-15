import 'package:flutter/material.dart';
import '../../src/api/api.dart';
import '../design_system/app_colors.dart';
import 'academic_period_scope.dart';

class GradesWebScreen extends StatefulWidget {
  const GradesWebScreen({super.key, required this.token, this.apiClient});

  final String token;
  final GradebookApiClient? apiClient;

  @override
  State<GradesWebScreen> createState() => _GradesWebScreenState();
}

class _GradesWebScreenState extends State<GradesWebScreen> {
  late final GradebookApiClient api = widget.apiClient ?? GradebookApiClient();
  List<Map<String, dynamic>> assignments = [], scoreRows = [];
  Map<String, dynamic>? assignment, book, item;
  final scoreValues = <int, double?>{};
  final textValues = <int, String>{};
  bool loading = false, saving = false;
  String? error;
  int? loadedYear;
  String? loadedPeriod;

  String get assessmentType => item?['assessmentType'] as String? ?? 'SCORE';

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
    setState(() {
      loading = true;
      error = null;
      assignments = [];
      assignment = null;
      book = null;
      item = null;
      scoreRows = [];
    });
    try {
      final rows = await api.getMyAssignments(
        token: widget.token,
        academicYearId: yearId,
      );
      if (!mounted || loadedYear != yearId) return;
      setState(() => assignments = rows);
      if (rows.isNotEmpty) await _selectClass(rows.first['classId'] as int);
    } catch (exception) {
      if (mounted && loadedYear == yearId) {
        setState(() => error = _errorText(exception));
      }
    } finally {
      if (mounted && loadedYear == yearId) {
        setState(() => loading = false);
      }
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
    final requestKey = '${period.academicYearId}-${period.semesterId}';
    final requestedClass = assignment!['classId'] as int;
    final requestedSubject = assignment!['subjectId'] as int;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final loaded = await api.getGradeBook(
        token: widget.token,
        classId: requestedClass,
        subjectId: requestedSubject,
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
      if (!mounted ||
          loadedPeriod != requestKey ||
          assignment?['classId'] != requestedClass ||
          assignment?['subjectId'] != requestedSubject) {
        return;
      }
      setState(() {
        book = loaded;
        scoreRows = rows;
        item = allowed.isEmpty ? null : allowed.first;
        _fillValues();
      });
    } catch (exception) {
      if (mounted &&
          loadedPeriod == requestKey &&
          assignment?['classId'] == requestedClass &&
          assignment?['subjectId'] == requestedSubject) {
        setState(() => error = _errorText(exception));
      }
    } finally {
      if (mounted &&
          loadedPeriod == requestKey &&
          assignment?['classId'] == requestedClass &&
          assignment?['subjectId'] == requestedSubject) {
        setState(() => loading = false);
      }
    }
  }

  void _fillValues() {
    scoreValues.clear();
    textValues.clear();
    if (item == null) return;
    for (final row in scoreRows.where((r) => r['gradeItemId'] == item!['id'])) {
      final studentId = row['studentId'] as int;
      scoreValues[studentId] = (row['score'] as num?)?.toDouble();
      textValues[studentId] = row['comment'] as String? ?? '';
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
    if (assessmentType == 'SCORE' &&
        scoreValues.values.any((v) => v != null && (v < 0 || v > 10))) {
      setState(() => error = 'Điểm phải từ 0 đến 10.');
      return;
    }
    setState(() {
      saving = true;
      error = null;
    });
    try {
      await api.updateScores(
        token: widget.token,
        gradeItemId: item!['id'] as int,
        entries: students.map((student) {
          final studentId = student['studentId'] as int;
          if (assessmentType == 'SCORE') {
            return <String, dynamic>{
              'studentId': studentId,
              'score': scoreValues[studentId],
              'isGraded': scoreValues[studentId] != null,
            };
          }
          final value = textValues[studentId]?.trim() ?? '';
          return <String, dynamic>{
            'studentId': studentId,
            'comment': value.isEmpty ? null : value,
            'isGraded': value.isNotEmpty,
          };
        }).toList(),
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã lưu đầu điểm cho nhà trường.')),
        );
        await _loadBook();
      }
    } catch (exception) {
      if (mounted) setState(() => error = _errorText(exception));
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Nhập đầu điểm')),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () => loadedYear == null
                  ? Future.value()
                  : _loadAssignments(loadedYear!),
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
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
                    onChanged: (value) {
                      if (value != null) _selectClass(value);
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
                    onChanged: (value) {
                      if (value != null) _selectSubject(value);
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
                            (gradeItem) => DropdownMenuItem(
                              value: gradeItem['id'] as int,
                              child: Text(
                                '${gradeItem['name']} · ${_typeLabel(gradeItem['assessmentType'] as String?)}',
                              ),
                            ),
                          )
                          .toList(),
                      onChanged: (value) {
                        if (value != null) _selectItem(value);
                      },
                    ),
                  const SizedBox(height: 16),
                  if (book != null)
                    _FormulaCard(
                      book: book!,
                      subjectName: '${assignment?['subjectName']}',
                    ),
                  if (assignments.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(32),
                      child: Center(
                        child: Text(
                          'Bạn chưa được Admin phân công lớp/môn trong năm học này.',
                        ),
                      ),
                    ),
                  if (item != null) ...[
                    const SizedBox(height: 12),
                    Card(
                      color: AppColors.blueSoft,
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Text(
                          '${assignment?['className']} · ${assignment?['subjectName']} · ${item?['name']} · ${_typeLabel(assessmentType)}',
                          style: const TextStyle(fontWeight: FontWeight.w800),
                        ),
                      ),
                    ),
                    ...students.map(_studentEditor),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      onPressed: saving ? null : _upload,
                      icon: const Icon(Icons.cloud_upload),
                      label: Text(saving ? 'Đang lưu…' : 'Lưu đầu điểm'),
                    ),
                  ],
                ],
              ),
            ),
    );
  }

  Widget _studentEditor(Map<String, dynamic> student) {
    final studentId = student['studentId'] as int;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              student['studentName'] as String,
              style: const TextStyle(fontWeight: FontWeight.w800),
            ),
            Text(
              student['studentCode'] as String,
              style: const TextStyle(color: AppColors.muted, fontSize: 12),
            ),
            const SizedBox(height: 10),
            if (assessmentType == 'SCORE')
              TextFormField(
                key: ValueKey('${item!['id']}-$studentId-score'),
                initialValue: scoreValues[studentId]?.toString(),
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: const InputDecoration(
                  labelText: 'Điểm số',
                  hintText: '0–10',
                ),
                onChanged: (value) =>
                    scoreValues[studentId] = double.tryParse(value),
              )
            else if (assessmentType == 'PASS_FAIL')
              DropdownButtonFormField<String>(
                key: ValueKey('${item!['id']}-$studentId-pass-fail'),
                initialValue: textValues[studentId]?.isEmpty == true
                    ? null
                    : textValues[studentId],
                decoration: const InputDecoration(labelText: 'Kết quả'),
                items: const [
                  DropdownMenuItem(value: 'PASS', child: Text('Đạt')),
                  DropdownMenuItem(value: 'FAIL', child: Text('Chưa đạt')),
                ],
                onChanged: (value) => textValues[studentId] = value ?? '',
              )
            else
              TextFormField(
                key: ValueKey('${item!['id']}-$studentId-comment'),
                initialValue: textValues[studentId],
                maxLength: 255,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Nhận xét',
                  hintText: 'Nhập nhận xét cho học sinh',
                  alignLabelWithHint: true,
                ),
                onChanged: (value) => textValues[studentId] = value,
              ),
          ],
        ),
      ),
    );
  }

  static String _typeLabel(String? type) => switch (type) {
    'PASS_FAIL' => 'Đạt / Chưa đạt',
    'COMMENT' => 'Nhận xét',
    _ => 'Điểm số',
  };

  static String _errorText(Object error) =>
      error.toString().replaceFirst(RegExp(r'^[A-Za-z]+Exception:\s*'), '');
}

class _FormulaCard extends StatelessWidget {
  const _FormulaCard({required this.book, required this.subjectName});

  final Map<String, dynamic> book;
  final String subjectName;

  @override
  Widget build(BuildContext context) {
    final numericItems = (book['items'] as List)
        .whereType<Map<String, dynamic>>()
        .where((item) => item['assessmentType'] == 'SCORE')
        .toList();
    return Card(
      child: ExpansionTile(
        leading: const Icon(Icons.calculate_outlined, color: AppColors.blue),
        title: const Text(
          'Cách hệ thống tính điểm',
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
        subtitle: const Text('Nhận xét và Đạt/Chưa đạt không tính vào ĐTB'),
        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        children: [
          Align(
            alignment: Alignment.centerLeft,
            child: Text(
              'ĐTB môn $subjectName = Tổng (điểm số × hệ số) / Tổng hệ số, làm tròn 1 chữ số thập phân.',
            ),
          ),
          if (numericItems.isNotEmpty) ...[
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                numericItems
                    .map((item) => '${item['name']} × ${item['weight']}')
                    .join(' + '),
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
