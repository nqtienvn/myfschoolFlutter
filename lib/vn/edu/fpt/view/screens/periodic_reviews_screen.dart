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
    this.homeroomClassId,
  });

  final AuthService authService;
  final BackendApiClient? backend;
  final PeriodicReviewApi? api;
  final int? homeroomClassId;

  @override
  State<PeriodicReviewsScreen> createState() => _PeriodicReviewsScreenState();
}

class _PeriodicReviewsScreenState extends State<PeriodicReviewsScreen> {
  AcademicPeriod? _period;
  late final PeriodicReviewApi _api;

  @override
  void initState() {
    super.initState();
    _api = widget.api ?? PeriodicReviewApiClient(backend: widget.backend);
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
                          child: const Text('Gửi GVCN'),
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
    required this.token,
    required this.period,
    required this.classId,
  });
  final PeriodicReviewApi api;
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

  Future<void> _publishClass() async {
    try {
      await widget.api.publishClass(
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
            onPressed: _reports.isEmpty ? null : _publishClass,
            icon: const Icon(Icons.publish),
            label: const Text('Công bố cả lớp'),
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
    required this.token,
    required this.period,
    required this.report,
  });
  final PeriodicReviewApi api;
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
  String? _conduct;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _conduct = _report.conduct ?? _report.suggestedConduct;
  }

  @override
  void dispose() {
    _comment.dispose();
    super.dispose();
  }

  Future<bool> _save() async {
    if (_conduct == null) return false;
    setState(() => _saving = true);
    try {
      _report = await widget.api.saveHomeroomReport(
        token: widget.token,
        studentId: _report.studentId,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: _report.classId,
        generalComment: _comment.text.trim(),
        conduct: _conduct!,
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

  Future<void> _publish() async {
    if (!await _save() || _comment.text.trim().isEmpty || _conduct == null) {
      return;
    }
    try {
      _report = await widget.api.publishStudent(
        token: widget.token,
        studentId: _report.studentId,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: _report.classId,
      );
      if (mounted) setState(() {});
    } catch (error) {
      if (mounted) _snack(_message(error));
    }
  }

  Future<void> _returnReview(SubjectPeriodicReview review) async {
    if (review.id == null) return;
    final controller = TextEditingController();
    final reason = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Trả lại nhận xét ${review.subjectName}'),
        content: TextField(
          controller: controller,
          maxLength: 500,
          maxLines: 3,
          decoration: const InputDecoration(labelText: 'Lý do *'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: const Text('Trả lại'),
          ),
        ],
      ),
    );
    if (reason == null || reason.isEmpty) return;
    try {
      await widget.api.returnSubjectReview(
        token: widget.token,
        reviewId: review.id!,
        academicYearId: widget.period.academicYearId,
        reason: reason,
      );
      if (mounted) {
        _snack('Đã trả lại nhận xét.');
        Navigator.pop(context);
      }
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
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Tiến độ ${_report.submittedSubjects}/${_report.totalSubjects} môn',
                style: const TextStyle(fontWeight: FontWeight.w800),
              ),
              if (_report.missingSubjects.isNotEmpty)
                Text(
                  'Còn thiếu: ${_report.missingSubjects.join(', ')}',
                  style: const TextStyle(color: AppColors.danger),
                ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        ..._report.subjectReviews.map(
          (review) => Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          review.subjectName,
                          style: const TextStyle(fontWeight: FontWeight.w800),
                        ),
                      ),
                      _StatusChip(status: review.status),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(review.comment ?? 'Chưa gửi nhận xét'),
                  if (review.status == 'SUBMITTED')
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton(
                        onPressed: () => _returnReview(review),
                        child: const Text('Trả lại'),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
        TextField(
          controller: _comment,
          maxLength: 2000,
          maxLines: 5,
          decoration: const InputDecoration(
            labelText: 'Nhận xét chung của GVCN',
          ),
        ),
        DropdownButtonFormField<String>(
          initialValue: _conduct,
          decoration: InputDecoration(
            labelText: 'Hạnh kiểm',
            helperText:
                'Gợi ý từ chuyên cần: ${_report.suggestedConduct ?? 'Chưa có'}',
          ),
          items: const ['Tốt', 'Khá', 'Trung bình', 'Yếu']
              .map((item) => DropdownMenuItem(value: item, child: Text(item)))
              .toList(),
          onChanged: (value) => setState(() => _conduct = value),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _saving || _report.isPublished ? null : _save,
                child: const Text('Lưu nháp'),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: FilledButton(
                onPressed: _saving || _report.isPublished ? null : _publish,
                child: Text(_report.isPublished ? 'Đã công bố' : 'Công bố'),
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
        : status == 'RETURNED'
        ? AppColors.danger
        : AppColors.warning;
    final label =
        {
          'PUBLISHED': 'Đã công bố',
          'SUBMITTED': 'Đã gửi',
          'RETURNED': 'Trả lại',
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
