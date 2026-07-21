import 'dart:async';

import 'package:flutter/material.dart';

import '../../src/api/api.dart';
import '../../src/models/app_notification.dart';
import '../../src/models/periodic_review.dart';
import '../../src/services/auth_service.dart';
import '../../src/services/notification_service.dart';
import '../design_system/app_colors.dart';
import 'academic_period_scope.dart';

class GradesScreen extends StatefulWidget {
  const GradesScreen({
    super.key,
    required this.token,
    this.studentId,
    this.studentName,
    this.authService,
    this.backendApiClient,
    this.apiClient,
    this.academicApiClient,
    this.periodicReviewApi,
    this.notificationService,
  });

  final String token;
  final int? studentId;
  final String? studentName;
  final AuthService? authService;
  final BackendApiClient? backendApiClient;
  final GradebookApiClient? apiClient;
  final HomeroomAcademicApiClient? academicApiClient;
  final PeriodicReviewApi? periodicReviewApi;
  final NotificationService? notificationService;

  @override
  State<GradesScreen> createState() => _GradesScreenState();
}

class _GradesScreenState extends State<GradesScreen> {
  late final BackendApiClient _backend =
      widget.backendApiClient ?? BackendApiClient();
  late final GradebookApiClient _gradebookApi =
      widget.apiClient ?? GradebookApiClient(backend: _backend);
  late final HomeroomAcademicApiClient _academicApi =
      widget.academicApiClient ?? HomeroomAcademicApiClient(backend: _backend);
  late final PeriodicReviewApi _reviewApi =
      widget.periodicReviewApi ?? PeriodicReviewApiClient(backend: _backend);

  Map<String, dynamic>? _transcript;
  HomeroomStudentResultDto? _semesterResult;
  StudentPeriodicReport? _publishedReport;
  bool _loading = true;
  String? _error;
  String? _loadedPeriod;
  int _latestGradeNotificationId = 0;

  @override
  void initState() {
    super.initState();
    _latestGradeNotificationId = _latestRelevantNotification()?.id ?? 0;
    widget.notificationService?.addListener(_onGradeNotification);
  }

  @override
  void dispose() {
    widget.notificationService?.removeListener(_onGradeNotification);
    super.dispose();
  }

  AppNotification? _latestRelevantNotification() {
    AppNotification? latest;
    for (final item in widget.notificationService?.notifications ?? const []) {
      if (item.relatedType != 'GRADE_PUBLISHED') continue;
      if (widget.studentId != null && item.relatedId != widget.studentId) {
        continue;
      }
      if (latest == null || item.id > latest.id) latest = item;
    }
    return latest;
  }

  void _onGradeNotification() {
    final latest = _latestRelevantNotification();
    if (!mounted || latest == null || latest.id <= _latestGradeNotificationId) {
      return;
    }
    _latestGradeNotificationId = latest.id;
    final controller = AcademicPeriodScope.maybeOf(context);
    if (latest.academicYearId != null && latest.semesterId != null) {
      final selected = controller?.selected;
      final isSelected =
          selected?.academicYearId == latest.academicYearId &&
          selected?.semesterId == latest.semesterId;
      if (!isSelected &&
          (controller?.selectByIds(
                academicYearId: latest.academicYearId!,
                semesterId: latest.semesterId!,
              ) ??
              false)) {
        return;
      }
      if (!isSelected) return;
    }
    final period = controller?.selected;
    if (period != null) unawaited(_load(period));
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    if (period == null) return;
    final key = '${period.academicYearId}-${period.semesterId}';
    if (key != _loadedPeriod) {
      _loadedPeriod = key;
      _load(period);
    }
  }

  Future<void> _load(AcademicPeriod period) async {
    final requestKey = '${period.academicYearId}-${period.semesterId}';
    setState(() {
      _loading = true;
      _error = null;
      _transcript = null;
      _semesterResult = null;
      _publishedReport = null;
    });
    try {
      final studentId = await _resolveStudentId();
      final results = await Future.wait<Object?>([
        _gradebookApi.getTranscript(
          token: widget.token,
          academicYearId: period.academicYearId,
          semesterId: period.semesterId,
          studentId: widget.studentId,
        ),
        if (studentId == null)
          Future<Object?>.value()
        else
          _academicApi
              .getStudentSemesterResult(
                token: widget.token,
                studentId: studentId,
                semesterId: period.semesterId,
              )
              .then<Object?>((value) => value, onError: (_, _) => null),
        if (studentId == null)
          Future<Object?>.value()
        else
          _reviewApi
              .getPublishedReport(
                token: widget.token,
                studentId: studentId,
                academicYearId: period.academicYearId,
                semesterId: period.semesterId,
              )
              .then<Object?>((value) => value, onError: (_, _) => null),
      ]);
      if (!_isCurrent(requestKey)) return;
      setState(() {
        _transcript = results[0] as Map<String, dynamic>;
        _semesterResult = results[1] as HomeroomStudentResultDto?;
        _publishedReport = results[2] as StudentPeriodicReport?;
      });
    } catch (error) {
      if (_isCurrent(requestKey)) {
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
      }
    } finally {
      if (_isCurrent(requestKey)) setState(() => _loading = false);
    }
  }

  Future<int?> _resolveStudentId() async {
    if (widget.studentId != null) return widget.studentId;
    final auth = widget.authService;
    if (auth == null) return null;
    if (auth.currentSession?.role == 'PARENT') return auth.selectedChild?.id;
    return _reviewApi.resolveStudentId(token: widget.token);
  }

  bool _isCurrent(String requestKey) => mounted && _loadedPeriod == requestKey;

  List<Map<String, dynamic>> get _subjects =>
      (_transcript?['subjects'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .toList(growable: false);

  Map<String, SubjectPeriodicReview> get _reviewsBySubject => {
    for (final review in _publishedReport?.subjectReviews ?? const [])
      _normalize(review.subjectName): review,
  };

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
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () => period == null ? Future.value() : _load(period),
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
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
                            (item) => DropdownMenuItem(
                              value: item,
                              child: Text(item.label),
                            ),
                          )
                          .toList(growable: false),
                      onChanged: (value) {
                        if (value != null) controller.select(value);
                      },
                    ),
                  if (_error != null) ...[
                    const SizedBox(height: 12),
                    Card(
                      color: AppColors.dangerSoft,
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Text(_error!),
                      ),
                    ),
                  ],
                  const SizedBox(height: 18),
                  const _SectionTitle(
                    index: '01',
                    title: 'Bảng tổng kết học lực',
                  ),
                  const SizedBox(height: 10),
                  _SemesterSummaryTable(
                    result: _semesterResult,
                    report: _publishedReport,
                  ),
                  const SizedBox(height: 22),
                  const _SectionTitle(index: '02', title: 'Bảng điểm chi tiết'),
                  const SizedBox(height: 4),
                  const Text(
                    'Các cột theo đúng cấu hình năm học; ô chưa nhập được để trống. Vuốt ngang để xem toàn bộ.',
                    style: TextStyle(color: AppColors.muted, fontSize: 12),
                  ),
                  const SizedBox(height: 10),
                  if (_subjects.isEmpty)
                    const Card(
                      child: Padding(
                        padding: EdgeInsets.all(28),
                        child: Center(
                          child: Text(
                            'Năm học chưa được cấu hình môn học.',
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ),
                    )
                  else
                    _AllSubjectsGradeTable(
                      subjects: _subjects,
                      reviewsBySubject: _reviewsBySubject,
                    ),
                ],
              ),
            ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.index, required this.title});

  final String index;
  final String title;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      Container(
        width: 34,
        height: 34,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: AppColors.primarySoft,
          borderRadius: BorderRadius.circular(10),
        ),
        child: Text(
          index,
          style: const TextStyle(
            color: AppColors.fptOrange,
            fontWeight: FontWeight.w900,
          ),
        ),
      ),
      const SizedBox(width: 10),
      Expanded(
        child: Text(
          title.toUpperCase(),
          style: const TextStyle(
            color: AppColors.ink,
            fontSize: 16,
            fontWeight: FontWeight.w900,
          ),
        ),
      ),
    ],
  );
}

class _SemesterSummaryTable extends StatelessWidget {
  const _SemesterSummaryTable({required this.result, required this.report});

  final HomeroomStudentResultDto? result;
  final StudentPeriodicReport? report;

  @override
  Widget build(BuildContext context) {
    if (result == null) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(20),
          child: Text('Chưa có kết quả tổng kết.'),
        ),
      );
    }
    final comment = report?.generalComment?.trim();
    final teacher = report?.homeroomTeacherName?.trim();
    final rows = <(String, Widget)>[
      ('Danh hiệu', _value(result!.honor)),
      ('Điểm TB', _value(result!.gpa?.toStringAsFixed(1))),
      ('Hạnh kiểm', _value(result!.conduct)),
      ('Học lực', _value(result!.academicAbility)),
      ('Xếp hạng', _value(result!.rank?.toString())),
      (
        'Nhận xét GVCN',
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              comment?.isNotEmpty == true ? comment! : 'Chưa có nhận xét.',
              style: const TextStyle(fontStyle: FontStyle.italic),
            ),
            if (teacher?.isNotEmpty == true) ...[
              const SizedBox(height: 5),
              Text(
                '— $teacher',
                style: const TextStyle(
                  color: AppColors.muted,
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ],
        ),
      ),
    ];
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Table(
        columnWidths: const {0: FixedColumnWidth(128), 1: FlexColumnWidth()},
        border: const TableBorder(
          horizontalInside: BorderSide(color: AppColors.line),
          verticalInside: BorderSide(color: AppColors.line),
        ),
        children: [
          for (final row in rows)
            TableRow(
              decoration: const BoxDecoration(color: Colors.white),
              children: [
                Padding(
                  padding: const EdgeInsets.all(13),
                  child: Text(
                    row.$1,
                    style: const TextStyle(
                      color: AppColors.muted,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                Padding(padding: const EdgeInsets.all(13), child: row.$2),
              ],
            ),
        ],
      ),
    );
  }

  static Widget _value(String? value) => Text(
    value?.trim().isNotEmpty == true ? value! : '—',
    style: const TextStyle(fontWeight: FontWeight.w800),
  );
}

class _AllSubjectsGradeTable extends StatelessWidget {
  const _AllSubjectsGradeTable({
    required this.subjects,
    required this.reviewsBySubject,
  });

  static const double _headerHeight = 54;
  static const double _rowHeight = 66;
  final List<Map<String, dynamic>> subjects;
  final Map<String, SubjectPeriodicReview> reviewsBySubject;

  @override
  Widget build(BuildContext context) {
    final columns = _columnsFor(subjects);
    final rows = subjects
        .map(
          (subject) => _SubjectGradeRow(
            subject: subject,
            review:
                reviewsBySubject[_normalize(
                  subject['subjectName'] as String? ?? '',
                )],
          ),
        )
        .toList(growable: false);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 138,
            child: Column(
              children: [
                _headerCell('Môn học', height: _headerHeight, left: true),
                for (final row in rows)
                  _bodyCell(
                    row.subjectName,
                    height: _rowHeight,
                    left: true,
                    bold: true,
                  ),
              ],
            ),
          ),
          Expanded(
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: SizedBox(
                width:
                    columns.fold<double>(
                      0,
                      (total, column) => total + column.width,
                    ) +
                    340,
                child: Column(
                  children: [
                    Row(
                      children: [
                        for (final column in columns)
                          _headerCell(column.name, width: column.width),
                        _headerCell('TBM', width: 70),
                        _headerCell('Nhận xét GV', width: 270),
                      ],
                    ),
                    for (final row in rows)
                      Row(
                        children: [
                          for (final column in columns)
                            _bodyCell(
                              row.scoreFor(column),
                              width: column.width,
                              left: column.assessmentType == 'COMMENT',
                            ),
                          _bodyCell(
                            row.average,
                            width: 70,
                            bold: true,
                            color: AppColors.fptOrange,
                          ),
                          _ReviewCell(
                            text: row.comment,
                            teacherName: row.teacherName,
                            width: 270,
                            height: _rowHeight,
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

  static List<_GradeTableColumn> _columnsFor(
    List<Map<String, dynamic>> subjects,
  ) {
    final columns = <String, _GradeTableColumn>{};
    for (final subject in subjects) {
      final scores = (subject['scores'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>();
      for (final score in scores) {
        final name = (score['name'] as String? ?? '').trim();
        if (name.isEmpty) continue;
        final key = _gradeColumnKey(score);
        columns.putIfAbsent(
          key,
          () => _GradeTableColumn(
            key: key,
            name: name,
            assessmentType: score['assessmentType'] as String? ?? 'SCORE',
          ),
        );
      }
    }
    return columns.values.toList(growable: false);
  }

  static Widget _headerCell(
    String text, {
    double width = 138,
    double height = _headerHeight,
    bool left = false,
  }) => Container(
    width: width,
    height: height,
    alignment: left ? Alignment.centerLeft : Alignment.center,
    padding: const EdgeInsets.symmetric(horizontal: 10),
    decoration: const BoxDecoration(
      color: AppColors.primarySoft,
      border: Border(
        right: BorderSide(color: AppColors.line),
        bottom: BorderSide(color: AppColors.line),
      ),
    ),
    child: Text(
      text,
      textAlign: left ? TextAlign.left : TextAlign.center,
      style: const TextStyle(
        color: AppColors.ink,
        fontSize: 12,
        fontWeight: FontWeight.w900,
      ),
    ),
  );

  static Widget _bodyCell(
    String text, {
    double width = 138,
    double height = _rowHeight,
    bool left = false,
    bool bold = false,
    Color color = AppColors.ink,
  }) => Container(
    width: width,
    height: height,
    alignment: left ? Alignment.centerLeft : Alignment.center,
    padding: const EdgeInsets.symmetric(horizontal: 10),
    decoration: const BoxDecoration(
      color: Colors.white,
      border: Border(
        right: BorderSide(color: AppColors.line),
        bottom: BorderSide(color: AppColors.line),
      ),
    ),
    child: Text(
      text,
      maxLines: 2,
      overflow: TextOverflow.ellipsis,
      textAlign: left ? TextAlign.left : TextAlign.center,
      style: TextStyle(
        color: color,
        fontSize: 12,
        fontWeight: bold ? FontWeight.w800 : FontWeight.w600,
      ),
    ),
  );
}

class _GradeTableColumn {
  const _GradeTableColumn({
    required this.key,
    required this.name,
    required this.assessmentType,
  });

  final String key;
  final String name;
  final String assessmentType;

  double get width => assessmentType == 'COMMENT' ? 190 : 104;
}

class _ReviewCell extends StatelessWidget {
  const _ReviewCell({
    required this.text,
    required this.teacherName,
    required this.width,
    required this.height,
  });

  final String text;
  final String teacherName;
  final double width;
  final double height;

  @override
  Widget build(BuildContext context) => InkWell(
    onTap: text == '—'
        ? null
        : () => showDialog<void>(
            context: context,
            builder: (context) => AlertDialog(
              title: const Text('Nhận xét giáo viên'),
              content: SingleChildScrollView(child: Text(text)),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Đóng'),
                ),
              ],
            ),
          ),
    child: Container(
      width: width,
      height: height,
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(
          right: BorderSide(color: AppColors.line),
          bottom: BorderSide(color: AppColors.line),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            text,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 12),
          ),
          if (teacherName.isNotEmpty)
            Text(
              teacherName,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(color: AppColors.muted, fontSize: 10),
            ),
        ],
      ),
    ),
  );
}

class _SubjectGradeRow {
  _SubjectGradeRow({
    required Map<String, dynamic> subject,
    required SubjectPeriodicReview? review,
  }) : subjectName = subject['subjectName'] as String? ?? '—',
       _scoresByColumn = _scores(subject),
       average = _average(subject),
       comment = review?.comment?.trim().isNotEmpty == true
           ? review!.comment!.trim()
           : '—',
       teacherName = review?.subjectTeacherName.trim() ?? '';

  final String subjectName;
  final Map<String, Map<String, dynamic>> _scoresByColumn;
  final String average;
  final String comment;
  final String teacherName;

  static Map<String, Map<String, dynamic>> _scores(
    Map<String, dynamic> subject,
  ) => {
    for (final score
        in (subject['scores'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .where(
              (score) => (score['name'] as String? ?? '').trim().isNotEmpty,
            ))
      _gradeColumnKey(score): score,
  };

  String scoreFor(_GradeTableColumn column) {
    final score = _scoresByColumn[column.key];
    if (score == null || score['isGraded'] != true) return '';
    return switch (column.assessmentType) {
      'PASS_FAIL' => switch (score['comment']) {
        'PASS' => 'Đạt',
        'FAIL' => 'Chưa đạt',
        _ => '',
      },
      'COMMENT' => (score['comment'] as String? ?? '').trim(),
      _ => (score['score'] as num?)?.toStringAsFixed(1) ?? '',
    };
  }

  static String _average(Map<String, dynamic> subject) {
    return (subject['average'] as num?)?.toStringAsFixed(1) ?? '';
  }
}

String _gradeColumnKey(Map<String, dynamic> score) {
  final code = (score['code'] as String? ?? '').trim();
  return code.isNotEmpty ? code : (score['name'] as String? ?? '').trim();
}

String _normalize(String value) {
  var normalized = value.trim().toLowerCase();
  const source =
      'àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ';
  const target =
      'aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyd';
  for (var index = 0; index < source.length; index++) {
    normalized = normalized.replaceAll(source[index], target[index]);
  }
  return normalized.replaceAll(RegExp(r'\s+'), ' ');
}
