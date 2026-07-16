import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';

class PeriodicReviewsScreen extends StatefulWidget {
  const PeriodicReviewsScreen({
    super.key,
    required this.authService,
    this.backend,
    this.api,
    this.monitoringApi,
    this.homeroomClassId,
  });

  final AuthService authService;
  final BackendApiClient? backend;
  final PeriodicReviewApi? api;
  final HomeroomMonitoringApi? monitoringApi;
  final int? homeroomClassId;

  @override
  State<PeriodicReviewsScreen> createState() => _PeriodicReviewsScreenState();
}

class _PeriodicReviewsScreenState extends State<PeriodicReviewsScreen> {
  AcademicPeriod? _period;
  late final PeriodicReviewApi _api;
  late final HomeroomMonitoringApi _monitoringApi;

  @override
  void initState() {
    super.initState();
    _api = widget.api ?? PeriodicReviewApiClient(backend: widget.backend);
    _monitoringApi =
        widget.monitoringApi ??
        HomeroomMonitoringApiClient(backend: widget.backend);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _period ??= AcademicPeriodScope.maybeOf(context)?.selected;
  }

  @override
  Widget build(BuildContext context) {
    final session = widget.authService.currentSession;
    final period = _period;
    if (session == null || period == null) {
      return const Scaffold(
        body: Center(child: Text('Chưa chọn năm học và học kỳ.')),
      );
    }
    if (session.role == 'TEACHER') {
      final hasHomeroom = widget.homeroomClassId != null;
      return DefaultTabController(
        length: hasHomeroom ? 2 : 1,
        child: Scaffold(
          appBar: AppBar(
            title: Text('Nhận xét · ${period.semesterName}'),
            bottom: TabBar(
              tabs: [
                const Tab(text: 'Bộ môn'),
                if (hasHomeroom) const Tab(text: 'Chủ nhiệm'),
              ],
            ),
          ),
          body: TabBarView(
            children: [
              _SubjectReviewView(
                api: _api,
                token: session.token,
                period: period,
              ),
              if (hasHomeroom)
                _HomeroomReviewView(
                  api: _api,
                  monitoringApi: _monitoringApi,
                  token: session.token,
                  period: period,
                  classId: widget.homeroomClassId!,
                ),
            ],
          ),
        ),
      );
    }
    return _PublishedReportView(
      api: _api,
      token: session.token,
      period: period,
      studentId: session.role == 'PARENT'
          ? widget.authService.selectedChild?.id
          : null,
    );
  }
}

class _SubjectReviewView extends StatefulWidget {
  const _SubjectReviewView({
    required this.api,
    required this.token,
    required this.period,
  });
  final PeriodicReviewApi api;
  final String token;
  final AcademicPeriod period;

  @override
  State<_SubjectReviewView> createState() => _SubjectReviewViewState();
}

class _SubjectReviewViewState extends State<_SubjectReviewView> {
  List<ReviewAssignment> _assignments = const [];
  List<SubjectPeriodicReview> _reviews = const [];
  ReviewAssignment? _selected;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadAssignments();
  }

  Future<void> _loadAssignments() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final items = await widget.api.getAssignments(
        token: widget.token,
        academicYearId: widget.period.academicYearId,
      );
      if (!mounted) return;
      _assignments = items;
      _selected = items.isEmpty ? null : items.first;
      if (_selected != null) await _loadReviews();
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadReviews() async {
    final assignment = _selected;
    if (assignment == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final items = await widget.api.getSubjectReviews(
        token: widget.token,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: assignment.classId,
        subjectId: assignment.subjectId,
      );
      if (mounted) setState(() => _reviews = items);
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _edit(SubjectPeriodicReview review) async {
    final assignment = _selected!;
    final comment = TextEditingController(text: review.comment);
    final strengths = TextEditingController(text: review.strengths);
    final improvements = TextEditingController(text: review.improvements);
    final save = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Nhận xét ${review.studentName}'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: comment,
                maxLength: 2000,
                maxLines: 4,
                decoration: const InputDecoration(labelText: 'Nhận xét *'),
              ),
              TextField(
                controller: strengths,
                maxLength: 1000,
                maxLines: 2,
                decoration: const InputDecoration(labelText: 'Điểm mạnh'),
              ),
              TextField(
                controller: improvements,
                maxLength: 1000,
                maxLines: 2,
                decoration: const InputDecoration(labelText: 'Cần cải thiện'),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Lưu nháp'),
          ),
        ],
      ),
    );
    if (save != true) return;
    try {
      await widget.api.saveSubjectReview(
        token: widget.token,
        studentId: review.studentId,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: assignment.classId,
        subjectId: assignment.subjectId,
        comment: comment.text.trim(),
        strengths: strengths.text.trim(),
        improvements: improvements.text.trim(),
      );
      await _loadReviews();
    } catch (error) {
      if (mounted) _snack(_message(error));
    }
  }

  Future<void> _submit(SubjectPeriodicReview review) async {
    final assignment = _selected!;
    try {
      await widget.api.submitSubjectReview(
        token: widget.token,
        studentId: review.studentId,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: assignment.classId,
        subjectId: assignment.subjectId,
      );
      await _loadReviews();
    } catch (error) {
      if (mounted) _snack(_message(error));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading && _assignments.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null && _assignments.isEmpty) {
      return _ErrorState(message: _error!, retry: _loadAssignments);
    }
    if (_assignments.isEmpty) {
      return const Center(
        child: Text('Không có phân công bộ môn trong năm học này.'),
      );
    }
    return RefreshIndicator(
      onRefresh: _loadReviews,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          DropdownButtonFormField<ReviewAssignment>(
            initialValue: _selected,
            decoration: const InputDecoration(labelText: 'Lớp · Môn học'),
            items: _assignments
                .map(
                  (item) => DropdownMenuItem(
                    value: item,
                    child: Text('${item.className} · ${item.subjectName}'),
                  ),
                )
                .toList(),
            onChanged: (value) {
              setState(() {
                _selected = value;
                _reviews = const [];
              });
              _loadReviews();
            },
          ),
          const SizedBox(height: 14),
          if (_loading) const LinearProgressIndicator(),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.all(8),
              child: Text(
                _error!,
                style: const TextStyle(color: AppColors.danger),
              ),
            ),
          ..._reviews.map(
            (review) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: AppCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            '${review.studentName} · ${review.studentCode}',
                            style: const TextStyle(fontWeight: FontWeight.w800),
                          ),
                        ),
                        _StatusChip(status: review.status),
                      ],
                    ),
                    if (review.returnReason != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text(
                          'Lý do trả lại: ${review.returnReason}',
                          style: const TextStyle(color: AppColors.danger),
                        ),
                      ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: Text(review.comment ?? 'Chưa nhập nhận xét'),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        TextButton(
                          onPressed: review.canEdit
                              ? () => _edit(review)
                              : null,
                          child: const Text('Chỉnh sửa'),
                        ),
                        FilledButton.tonal(
                          onPressed:
                              review.canEdit &&
                                  (review.comment?.trim().isNotEmpty ?? false)
                              ? () => _submit(review)
                              : null,
                          child: const Text('Gửi hệ thống'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _snack(String message) => ScaffoldMessenger.of(
    context,
  ).showSnackBar(SnackBar(content: Text(message)));
}

class _HomeroomReviewView extends StatefulWidget {
  const _HomeroomReviewView({
    required this.api,
    required this.monitoringApi,
    required this.token,
    required this.period,
    required this.classId,
  });
  final PeriodicReviewApi api;
  final HomeroomMonitoringApi monitoringApi;
  final String token;
  final AcademicPeriod period;
  final int classId;

  @override
  State<_HomeroomReviewView> createState() => _HomeroomReviewViewState();
}

class _HomeroomReviewViewState extends State<_HomeroomReviewView> {
  List<StudentPeriodicReport> _reports = const [];
  bool _loading = true;
  String? _error;

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
      final rows = await widget.api.getHomeroomReports(
        token: widget.token,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: widget.classId,
      );
      if (mounted) setState(() => _reports = rows);
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _submitClass() async {
    try {
      await widget.api.submitHomeroomClass(
        token: widget.token,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: widget.classId,
      );
      await _load();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(_message(error))));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading && _reports.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null && _reports.isEmpty) {
      return _ErrorState(message: _error!, retry: _load);
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          FilledButton.icon(
            onPressed: _reports.isEmpty ? null : _submitClass,
            icon: const Icon(Icons.send_outlined),
            label: const Text('Submit cả lớp'),
          ),
          const SizedBox(height: 12),
          ..._reports.map(
            (report) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: AppCard(
                child: ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(
                    report.studentName,
                    style: const TextStyle(fontWeight: FontWeight.w800),
                  ),
                  subtitle: Text(
                    '${report.studentCode} · ${report.submittedSubjects}/${report.totalSubjects} môn đã gửi${report.missingSubjects.isEmpty ? '' : '\nThiếu: ${report.missingSubjects.join(', ')}'}',
                  ),
                  trailing: _StatusChip(status: report.status),
                  onTap: () async {
                    await Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => _HomeroomStudentReportScreen(
                          api: widget.api,
                          monitoringApi: widget.monitoringApi,
                          token: widget.token,
                          period: widget.period,
                          report: report,
                        ),
                      ),
                    );
                    _load();
                  },
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _HomeroomStudentReportScreen extends StatefulWidget {
  const _HomeroomStudentReportScreen({
    required this.api,
    required this.monitoringApi,
    required this.token,
    required this.period,
    required this.report,
  });
  final PeriodicReviewApi api;
  final HomeroomMonitoringApi monitoringApi;
  final String token;
  final AcademicPeriod period;
  final StudentPeriodicReport report;

  @override
  State<_HomeroomStudentReportScreen> createState() =>
      _HomeroomStudentReportScreenState();
}

class _HomeroomStudentReportScreenState
    extends State<_HomeroomStudentReportScreen> {
  late StudentPeriodicReport _report = widget.report;
  late final TextEditingController _comment = TextEditingController(
    text: _report.generalComment,
  );
  List<StudentEvent> _violations = const [];
  bool _loadingViolations = true;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadViolations();
  }

  @override
  void dispose() {
    _comment.dispose();
    super.dispose();
  }

  Future<bool> _save() async {
    setState(() => _saving = true);
    try {
      _report = await widget.api.saveHomeroomReport(
        token: widget.token,
        studentId: _report.studentId,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: _report.classId,
        generalComment: _comment.text.trim(),
        conduct: '',
      );
      if (mounted) setState(() {});
      return true;
    } catch (error) {
      if (mounted) _snack(_message(error));
      return false;
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _submit() async {
    if (_comment.text.trim().isEmpty) {
      _snack('Vui lòng nhập nhận xét chung trước khi Submit.');
      return;
    }
    if (!await _save()) return;
    try {
      _report = await widget.api.submitHomeroomReport(
        token: widget.token,
        studentId: _report.studentId,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: _report.classId,
      );
      await _loadViolations();
      if (mounted) {
        setState(() {});
        _snack('Đã Submit nhận xét và vi phạm cho Admin.');
      }
    } catch (error) {
      if (mounted) _snack(_message(error));
    }
  }

  Future<void> _loadViolations() async {
    if (mounted) setState(() => _loadingViolations = true);
    try {
      final items = await widget.monitoringApi.getStudentEvents(
        token: widget.token,
        studentId: _report.studentId,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: _report.classId,
      );
      if (mounted) {
        setState(() {
          _violations = items
              .where((item) => item.eventType == 'VIOLATION')
              .toList(growable: false);
        });
      }
    } catch (error) {
      if (mounted) _snack(_message(error));
    } finally {
      if (mounted) setState(() => _loadingViolations = false);
    }
  }

  Future<void> _editViolation([StudentEvent? current]) async {
    final title = TextEditingController(text: current?.title);
    final category = TextEditingController(text: current?.category);
    final description = TextEditingController(text: current?.description);
    var eventDate = current?.eventDate ?? DateTime.now();
    final save = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(current == null ? 'Thêm vi phạm' : 'Sửa vi phạm'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: title,
                  maxLength: 200,
                  decoration: const InputDecoration(labelText: 'Tiêu đề *'),
                ),
                TextField(
                  controller: category,
                  maxLength: 100,
                  decoration: const InputDecoration(labelText: 'Phân loại'),
                ),
                TextField(
                  controller: description,
                  maxLength: 2000,
                  maxLines: 4,
                  decoration: const InputDecoration(
                    labelText: 'Mô tả chi tiết',
                  ),
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Ngày vi phạm'),
                  subtitle: Text(_date(eventDate)),
                  trailing: const Icon(Icons.calendar_month_outlined),
                  onTap: () async {
                    final selected = await showDatePicker(
                      context: context,
                      initialDate: eventDate,
                      firstDate: widget.period.startDate,
                      lastDate: widget.period.endDate,
                    );
                    if (selected != null) {
                      setDialogState(() => eventDate = selected);
                    }
                  },
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Hủy'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Lưu'),
            ),
          ],
        ),
      ),
    );
    if (save != true || title.text.trim().isEmpty) return;
    try {
      await widget.monitoringApi.saveStudentEvent(
        token: widget.token,
        studentId: _report.studentId,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: _report.classId,
        eventType: 'VIOLATION',
        category: category.text.trim(),
        title: title.text.trim(),
        description: description.text.trim(),
        eventDate: eventDate,
        eventId: current?.id,
      );
      await _loadViolations();
    } catch (error) {
      if (mounted) _snack(_message(error));
    }
  }

  Future<void> _deleteViolation(StudentEvent violation) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Xóa vi phạm?'),
        content: Text('Vi phạm “${violation.title}” sẽ bị xóa khỏi hồ sơ.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await widget.monitoringApi.deleteStudentEvent(
        token: widget.token,
        eventId: violation.id,
        academicYearId: widget.period.academicYearId,
      );
      await _loadViolations();
    } catch (error) {
      if (mounted) _snack(_message(error));
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(_report.studentName)),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        AppCard(
          child: Row(
            children: [
              const Icon(Icons.person_outline, color: AppColors.fptOrange),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _report.studentName,
                      style: const TextStyle(fontWeight: FontWeight.w900),
                    ),
                    Text(
                      '${_report.studentCode} · ${_report.className}',
                      style: const TextStyle(color: AppColors.muted),
                    ),
                  ],
                ),
              ),
              _StatusChip(status: _report.status),
            ],
          ),
        ),
        const SizedBox(height: 12),
        const Text(
          'Nhận xét chung của GVCN',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _comment,
          maxLength: 2000,
          maxLines: 5,
          enabled: !_report.isSubmitted,
          decoration: const InputDecoration(
            hintText: 'Nhập nhận xét tổng quát về học tập và rèn luyện...',
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            const Expanded(
              child: Text(
                'Vi phạm trong học kỳ',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900),
              ),
            ),
            FilledButton.tonalIcon(
              onPressed: _report.isSubmitted ? null : _editViolation,
              icon: const Icon(Icons.add),
              label: const Text('Thêm vi phạm'),
            ),
          ],
        ),
        const SizedBox(height: 8),
        if (_loadingViolations)
          const LinearProgressIndicator()
        else if (_violations.isEmpty)
          const AppCard(child: Text('Chưa ghi nhận vi phạm nào.'))
        else
          ..._violations.map(
            (violation) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: AppCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            violation.title,
                            style: const TextStyle(fontWeight: FontWeight.w800),
                          ),
                        ),
                        _StatusChip(status: violation.status),
                      ],
                    ),
                    Text(
                      '${violation.category.isEmpty ? 'Vi phạm' : violation.category} · ${_date(violation.eventDate)}',
                      style: const TextStyle(
                        color: AppColors.muted,
                        fontSize: 12,
                      ),
                    ),
                    if (violation.description.isNotEmpty) ...[
                      const SizedBox(height: 5),
                      Text(violation.description),
                    ],
                    if (!_report.isSubmitted && violation.status == 'DRAFT')
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          TextButton.icon(
                            onPressed: () => _editViolation(violation),
                            icon: const Icon(Icons.edit_outlined),
                            label: const Text('Sửa'),
                          ),
                          TextButton.icon(
                            onPressed: () => _deleteViolation(violation),
                            icon: const Icon(Icons.delete_outline),
                            label: const Text('Xóa'),
                          ),
                        ],
                      ),
                  ],
                ),
              ),
            ),
          ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _saving || _report.isSubmitted ? null : _save,
                child: const Text('Lưu nháp'),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: FilledButton(
                onPressed: _saving || _report.isSubmitted ? null : _submit,
                child: Text(_report.isSubmitted ? 'Đã Submit' : 'Submit'),
              ),
            ),
          ],
        ),
      ],
    ),
  );

  void _snack(String message) => ScaffoldMessenger.of(
    context,
  ).showSnackBar(SnackBar(content: Text(message)));

  String _date(DateTime value) =>
      '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
}

class _PublishedReportView extends StatefulWidget {
  const _PublishedReportView({
    required this.api,
    required this.token,
    required this.period,
    this.studentId,
  });
  final PeriodicReviewApi api;
  final String token;
  final AcademicPeriod period;
  final int? studentId;
  @override
  State<_PublishedReportView> createState() => _PublishedReportViewState();
}

class _PublishedReportViewState extends State<_PublishedReportView> {
  StudentPeriodicReport? _report;
  bool _loading = true;
  String? _error;
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
      final studentId =
          widget.studentId ??
          await widget.api.resolveStudentId(token: widget.token);
      final report = await widget.api.getPublishedReport(
        token: widget.token,
        studentId: studentId,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
      );
      if (mounted) setState(() => _report = report);
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text('Nhận xét ${widget.period.semesterName}')),
    body: _loading
        ? const Center(child: CircularProgressIndicator())
        : _report == null
        ? _ErrorState(
            message: _error ?? 'Chưa có báo cáo được công bố.',
            retry: _load,
          )
        : RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                AppCard(
                  gradient: const LinearGradient(
                    colors: [AppColors.fptOrange, Color(0xFFFF8533)],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _report!.studentName,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 19,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      Text(
                        '${_report!.className} · Hạnh kiểm: ${_report!.conduct}',
                        style: const TextStyle(color: Colors.white),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                AppCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Nhận xét của GVCN',
                        style: TextStyle(fontWeight: FontWeight.w800),
                      ),
                      const SizedBox(height: 6),
                      Text(_report!.generalComment ?? ''),
                      if (_report!.homeroomTeacherName != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 8),
                          child: Text(
                            _report!.homeroomTeacherName!,
                            style: const TextStyle(color: AppColors.muted),
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                const Text(
                  'Nhận xét từng môn',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900),
                ),
                const SizedBox(height: 8),
                ..._report!.subjectReviews.map(
                  (review) => Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: AppCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            review.subjectName,
                            style: const TextStyle(fontWeight: FontWeight.w800),
                          ),
                          const SizedBox(height: 5),
                          Text(review.comment ?? ''),
                          if (review.strengths?.isNotEmpty ?? false)
                            Text('Điểm mạnh: ${review.strengths}'),
                          if (review.improvements?.isNotEmpty ?? false)
                            Text('Cần cải thiện: ${review.improvements}'),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
  );
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});
  final String status;
  @override
  Widget build(BuildContext context) {
    final color = status == 'PUBLISHED' || status == 'SUBMITTED'
        ? AppColors.success
        : AppColors.warning;
    final label =
        {
          'PUBLISHED': 'Đã công bố',
          'SUBMITTED': 'Đã gửi',
          'DRAFT': 'Bản nháp',
        }[status] ??
        status;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: .12),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.retry});
  final String message;
  final Future<void> Function() retry;
  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 8),
          TextButton.icon(
            onPressed: retry,
            icon: const Icon(Icons.refresh),
            label: const Text('Tải lại'),
          ),
        ],
      ),
    ),
  );
}

String _message(Object error) => error.toString().replaceAll('Exception: ', '');
