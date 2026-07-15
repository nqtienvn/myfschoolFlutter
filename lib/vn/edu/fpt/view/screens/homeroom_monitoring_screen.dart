import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';

class HomeroomMonitoringScreen extends StatefulWidget {
  const HomeroomMonitoringScreen({
    super.key,
    required this.token,
    required this.classId,
    this.api,
    this.academicApi,
    this.reviewApi,
  });

  final String token;
  final int classId;
  final HomeroomMonitoringApi? api;
  final HomeroomAcademicApiClient? academicApi;
  final PeriodicReviewApi? reviewApi;

  @override
  State<HomeroomMonitoringScreen> createState() =>
      _HomeroomMonitoringScreenState();
}

class _HomeroomMonitoringScreenState extends State<HomeroomMonitoringScreen> {
  late final HomeroomMonitoringApi _api;
  late final HomeroomAcademicApiClient _academicApi;
  late final PeriodicReviewApi _reviewApi;
  AcademicPeriod? _period;
  HomeroomClassDetailDto? _classDetail;
  HomeroomClassSummary? _summary;
  List<StudentRiskFlag> _risks = const [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = widget.api ?? HomeroomMonitoringApiClient();
    _academicApi =
        widget.academicApi ??
        HomeroomAcademicApiClient(backend: BackendApiClient());
    _reviewApi = widget.reviewApi ?? PeriodicReviewApiClient();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final next = AcademicPeriodScope.maybeOf(context)?.selected;
    if (next != null && next != _period) {
      _period = next;
      _load();
    }
  }

  Future<void> _load() async {
    final period = _period;
    if (period == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final values = await Future.wait<Object>([
        _academicApi.getClassDetail(
          token: widget.token,
          classId: widget.classId,
        ),
        _api.getRisks(
          token: widget.token,
          academicYearId: period.academicYearId,
          semesterId: period.semesterId,
          classId: widget.classId,
        ),
        _api.getClassSummaries(
          token: widget.token,
          academicYearId: period.academicYearId,
          semesterId: period.semesterId,
          classId: widget.classId,
        ),
      ]);
      if (!mounted) return;
      final summaries = values[2] as List<HomeroomClassSummary>;
      setState(() {
        _classDetail = values[0] as HomeroomClassDetailDto;
        _risks = values[1] as List<StudentRiskFlag>;
        _summary = summaries.isEmpty ? null : summaries.first;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = _message(error);
      });
    }
  }

  Future<void> _recalculate() async {
    final period = _period!;
    setState(() => _loading = true);
    try {
      final risks = await _api.recalculateRisks(
        token: widget.token,
        academicYearId: period.academicYearId,
        semesterId: period.semesterId,
        classId: widget.classId,
      );
      if (mounted) setState(() => _risks = risks);
      await _load();
    } catch (error) {
      if (mounted) {
        setState(() => _loading = false);
        _snack(context, _message(error));
      }
    }
  }

  Future<void> _setRiskStatus(StudentRiskFlag risk, String action) async {
    try {
      final updated = await _api.updateRiskStatus(
        token: widget.token,
        riskId: risk.id,
        action: action,
      );
      if (!mounted) return;
      setState(() {
        _risks = _risks
            .map((item) => item.id == updated.id ? updated : item)
            .toList();
      });
    } catch (error) {
      if (mounted) _snack(context, _message(error));
    }
  }

  @override
  Widget build(BuildContext context) {
    final period = _period;
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          title: Text(
            period == null
                ? 'Theo dõi học sinh'
                : 'Theo dõi · ${period.semesterName}',
          ),
          actions: [
            IconButton(
              tooltip: 'Tính lại cảnh báo',
              onPressed: _loading || period == null ? null : _recalculate,
              icon: const Icon(Icons.refresh_rounded),
            ),
          ],
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Cảnh báo'),
              Tab(text: 'Học sinh'),
              Tab(text: 'Báo cáo lớp'),
            ],
          ),
        ),
        body: _body(),
      ),
    );
  }

  Widget _body() {
    if (_period == null) {
      return const Center(child: Text('Chưa chọn năm học và học kỳ.'));
    }
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return _ErrorView(message: _error!, onRetry: _load);
    }
    return TabBarView(
      children: [
        _RiskDashboard(risks: _risks, onAction: _setRiskStatus),
        _StudentRoster(
          students: _classDetail?.students ?? const [],
          risks: _risks,
          onOpen: _openStudent,
        ),
        _ClassReport(summary: _summary),
      ],
    );
  }

  void _openStudent(HomeroomStudentDto student) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => AdvancedStudentProfileScreen(
          token: widget.token,
          classId: widget.classId,
          student: student,
          period: _period!,
          risks: _risks.where((risk) => risk.studentId == student.id).toList(),
          api: _api,
          academicApi: _academicApi,
          reviewApi: _reviewApi,
        ),
      ),
    );
  }
}

class _RiskDashboard extends StatelessWidget {
  const _RiskDashboard({required this.risks, required this.onAction});
  final List<StudentRiskFlag> risks;
  final Future<void> Function(StudentRiskFlag, String) onAction;

  @override
  Widget build(BuildContext context) {
    if (risks.isEmpty) {
      return const _EmptyView(
        icon: Icons.verified_outlined,
        text: 'Không có cảnh báo trong học kỳ này.',
      );
    }
    return RefreshIndicator(
      onRefresh: () async {},
      child: ListView.separated(
        key: const Key('risk-list'),
        padding: const EdgeInsets.all(12),
        itemCount: risks.length,
        separatorBuilder: (_, _) => const SizedBox(height: 10),
        itemBuilder: (_, index) {
          final risk = risks[index];
          return Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Wrap(
                    spacing: 8,
                    runSpacing: 6,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      Text(
                        risk.studentName,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      _StatusChip(
                        text: _riskLabel(risk.riskType),
                        color: _severityColor(risk.severity),
                      ),
                      _StatusChip(
                        text: _statusLabel(risk.status),
                        color: _statusColor(risk.status),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(risk.message),
                  if (risk.metricValue.isNotEmpty)
                    Text(
                      'Chỉ số: ${risk.metricValue} · Ngưỡng: ${risk.thresholdValue}',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8,
                    children: [
                      if (risk.status == 'OPEN')
                        OutlinedButton(
                          onPressed: () => onAction(risk, 'acknowledge'),
                          child: const Text('Đã tiếp nhận'),
                        ),
                      if (risk.status != 'RESOLVED')
                        FilledButton.tonal(
                          onPressed: () => onAction(risk, 'resolve'),
                          child: const Text('Đánh dấu xử lý'),
                        ),
                    ],
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _StudentRoster extends StatelessWidget {
  const _StudentRoster({
    required this.students,
    required this.risks,
    required this.onOpen,
  });
  final List<HomeroomStudentDto> students;
  final List<StudentRiskFlag> risks;
  final ValueChanged<HomeroomStudentDto> onOpen;

  @override
  Widget build(BuildContext context) => ListView.separated(
    padding: const EdgeInsets.all(12),
    itemCount: students.length,
    separatorBuilder: (_, _) => const Divider(height: 1),
    itemBuilder: (_, index) {
      final student = students[index];
      final count = risks
          .where(
            (risk) => risk.studentId == student.id && risk.status != 'RESOLVED',
          )
          .length;
      return ListTile(
        leading: CircleAvatar(
          child: Text(student.name.isEmpty ? '?' : student.name[0]),
        ),
        title: Text(student.name),
        subtitle: Text(student.studentCode),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (count > 0) Badge(label: Text('$count')),
            const Icon(Icons.chevron_right_rounded),
          ],
        ),
        onTap: () => onOpen(student),
      );
    },
  );
}

class _ClassReport extends StatelessWidget {
  const _ClassReport({required this.summary});
  final HomeroomClassSummary? summary;

  @override
  Widget build(BuildContext context) {
    final value = summary;
    if (value == null) {
      return const _EmptyView(
        icon: Icons.analytics_outlined,
        text: 'Chưa có dữ liệu báo cáo lớp.',
      );
    }
    final metrics = <(String, String, IconData)>[
      ('Sĩ số', '${value.studentCount}', Icons.groups_outlined),
      (
        'Chuyên cần',
        '${value.attendanceRate.toStringAsFixed(1)}%',
        Icons.fact_check_outlined,
      ),
      ('Cảnh báo mở', '${value.openRiskCount}', Icons.warning_amber_rounded),
      (
        'GPA trung bình',
        value.averageGpa.toStringAsFixed(2),
        Icons.auto_graph_rounded,
      ),
      (
        'Tiến độ nhận xét',
        '${value.reviewProgressRate.toStringAsFixed(1)}%',
        Icons.rate_review_outlined,
      ),
      ('Liên hệ PH', '${value.parentContactCount}', Icons.phone_outlined),
      ('Lịch họp', '${value.meetingCount}', Icons.event_outlined),
      (
        'Tỷ lệ tham gia',
        '${value.meetingParticipationRate.toStringAsFixed(1)}%',
        Icons.how_to_reg_outlined,
      ),
      ('Khen thưởng', '${value.rewardCount}', Icons.emoji_events_outlined),
      ('Vi phạm', '${value.violationCount}', Icons.report_outlined),
    ];
    return LayoutBuilder(
      builder: (context, constraints) {
        final columns = constraints.maxWidth >= 700
            ? 3
            : constraints.maxWidth >= 390
            ? 2
            : 1;
        return GridView.builder(
          key: const Key('class-summary-grid'),
          padding: const EdgeInsets.all(12),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: columns,
            mainAxisSpacing: 10,
            crossAxisSpacing: 10,
            childAspectRatio: columns == 1 ? 3.2 : 1.45,
          ),
          itemCount: metrics.length,
          itemBuilder: (_, index) {
            final metric = metrics[index];
            return Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Row(
                  children: [
                    Icon(
                      metric.$3,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(metric.$1),
                          Text(
                            metric.$2,
                            style: Theme.of(context).textTheme.headlineSmall,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }
}

class AdvancedStudentProfileScreen extends StatelessWidget {
  const AdvancedStudentProfileScreen({
    super.key,
    required this.token,
    required this.classId,
    required this.student,
    required this.period,
    required this.risks,
    required this.api,
    required this.academicApi,
    required this.reviewApi,
  });

  final String token;
  final int classId;
  final HomeroomStudentDto student;
  final AcademicPeriod period;
  final List<StudentRiskFlag> risks;
  final HomeroomMonitoringApi api;
  final HomeroomAcademicApiClient academicApi;
  final PeriodicReviewApi reviewApi;

  @override
  Widget build(BuildContext context) => DefaultTabController(
    length: 5,
    child: Scaffold(
      appBar: AppBar(
        title: Text(student.name),
        bottom: const TabBar(
          isScrollable: true,
          tabs: [
            Tab(text: 'Tổng quan'),
            Tab(text: 'Kết quả/nhận xét'),
            Tab(text: 'Liên hệ PH'),
            Tab(text: 'Lịch hẹn'),
            Tab(text: 'Khen thưởng/vi phạm'),
          ],
        ),
      ),
      body: TabBarView(
        children: [
          _StudentOverview(student: student, period: period, risks: risks),
          _StudentResult(
            token: token,
            classId: classId,
            student: student,
            period: period,
            api: academicApi,
            reviewApi: reviewApi,
          ),
          _ContactLogsTab(
            token: token,
            student: student,
            classId: classId,
            period: period,
            api: api,
          ),
          _MeetingsTab(
            token: token,
            student: student,
            classId: classId,
            period: period,
            api: api,
          ),
          _StudentEventsTab(
            token: token,
            student: student,
            classId: classId,
            period: period,
            api: api,
          ),
        ],
      ),
    ),
  );
}

class _StudentOverview extends StatelessWidget {
  const _StudentOverview({
    required this.student,
    required this.period,
    required this.risks,
  });
  final HomeroomStudentDto student;
  final AcademicPeriod period;
  final List<StudentRiskFlag> risks;

  @override
  Widget build(BuildContext context) => ListView(
    padding: const EdgeInsets.all(16),
    children: [
      Card(
        child: ListTile(
          leading: const Icon(Icons.person_outline),
          title: Text(student.name),
          subtitle: Text(
            '${student.studentCode} · ${student.className}\n${period.label}',
          ),
        ),
      ),
      const SizedBox(height: 12),
      Text(
        'Cảnh báo đang theo dõi',
        style: Theme.of(context).textTheme.titleMedium,
      ),
      const SizedBox(height: 8),
      if (risks.where((risk) => risk.status != 'RESOLVED').isEmpty)
        const Text('Không có cảnh báo đang mở.')
      else
        ...risks
            .where((risk) => risk.status != 'RESOLVED')
            .map(
              (risk) => Card(
                child: ListTile(
                  leading: const Icon(Icons.warning_amber_rounded),
                  title: Text(_riskLabel(risk.riskType)),
                  subtitle: Text(risk.message),
                  trailing: Text(_statusLabel(risk.status)),
                ),
              ),
            ),
    ],
  );
}

class _StudentResult extends StatefulWidget {
  const _StudentResult({
    required this.token,
    required this.classId,
    required this.student,
    required this.period,
    required this.api,
    required this.reviewApi,
  });
  final String token;
  final int classId;
  final HomeroomStudentDto student;
  final AcademicPeriod period;
  final HomeroomAcademicApiClient api;
  final PeriodicReviewApi reviewApi;

  @override
  State<_StudentResult> createState() => _StudentResultState();
}

class _StudentResultState extends State<_StudentResult> {
  HomeroomStudentResultDto? _result;
  StudentPeriodicReport? _report;
  String? _error;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    HomeroomStudentResultDto? result;
    StudentPeriodicReport? report;
    Object? failure;
    try {
      result = await widget.api.getStudentSemesterResult(
        token: widget.token,
        studentId: widget.student.id,
        semesterId: widget.period.semesterId,
      );
    } catch (error) {
      failure = error;
    }
    try {
      final reports = await widget.reviewApi.getHomeroomReports(
        token: widget.token,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: widget.classId,
      );
      report = reports.cast<StudentPeriodicReport?>().firstWhere(
        (item) => item?.studentId == widget.student.id,
        orElse: () => null,
      );
    } catch (error) {
      failure ??= error;
    }
    if (mounted) {
      setState(() {
        _result = result;
        _report = report;
        _error = result == null && report == null && failure != null
            ? _message(failure)
            : null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_error != null) return _ErrorView(message: _error!, onRetry: _load);
    final result = _result;
    final report = _report;
    if (result == null && report == null) {
      return const _EmptyView(
        icon: Icons.hourglass_empty,
        text: 'Chưa có kết quả hoặc nhận xét học kỳ.',
      );
    }
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (result != null) ...[
          Text(
            'Kết quả tổng kết',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          _InfoTile(
            label: 'GPA',
            value: result.gpa?.toStringAsFixed(2) ?? '--',
          ),
          _InfoTile(
            label: 'Xếp hạng',
            value: result.rank == null ? '--' : '#${result.rank}',
          ),
          _InfoTile(label: 'Học lực', value: result.academicAbility ?? '--'),
          _InfoTile(label: 'Hạnh kiểm', value: result.conduct ?? '--'),
          _InfoTile(label: 'Danh hiệu', value: result.honor ?? '--'),
        ],
        if (report != null) ...[
          const SizedBox(height: 12),
          Text(
            'Nhận xét định kỳ',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          Card(
            child: ListTile(
              title: const Text('Nhận xét của GVCN'),
              subtitle: Text(_displayText(report.generalComment)),
              trailing: Text(_displayText(report.conduct)),
            ),
          ),
          ...report.subjectReviews.map(
            (review) => Card(
              child: ListTile(
                title: Text(review.subjectName),
                subtitle: Text(_displayText(review.comment)),
              ),
            ),
          ),
        ],
      ],
    );
  }
}

class _ContactLogsTab extends StatefulWidget {
  const _ContactLogsTab({
    required this.token,
    required this.student,
    required this.classId,
    required this.period,
    required this.api,
  });
  final String token;
  final HomeroomStudentDto student;
  final int classId;
  final AcademicPeriod period;
  final HomeroomMonitoringApi api;
  @override
  State<_ContactLogsTab> createState() => _ContactLogsTabState();
}

class _ContactLogsTabState extends State<_ContactLogsTab> {
  List<ParentContactLog>? _items;
  String? _error;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = await widget.api.getContactLogs(
        token: widget.token,
        studentId: widget.student.id,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: widget.classId,
      );
      if (mounted) {
        setState(() {
          _items = items;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    }
  }

  Future<void> _edit([ParentContactLog? current]) async {
    final subject = TextEditingController(text: current?.subject);
    final summary = TextEditingController(text: current?.summary);
    final result = TextEditingController(text: current?.result);
    var type = current?.contactType ?? 'CALL';
    var nextActionAt = current?.nextActionAt;
    final accepted = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(current == null ? 'Ghi nhận liên hệ' : 'Sửa liên hệ'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: type,
                  decoration: const InputDecoration(labelText: 'Hình thức'),
                  items: const ['CALL', 'MESSAGE', 'MEETING', 'OTHER']
                      .map(
                        (value) => DropdownMenuItem(
                          value: value,
                          child: Text(_contactLabel(value)),
                        ),
                      )
                      .toList(),
                  onChanged: (value) => setDialogState(() => type = value!),
                ),
                TextField(
                  controller: subject,
                  decoration: const InputDecoration(labelText: 'Chủ đề'),
                ),
                TextField(
                  controller: summary,
                  maxLines: 3,
                  decoration: const InputDecoration(labelText: 'Nội dung'),
                ),
                TextField(
                  controller: result,
                  decoration: const InputDecoration(labelText: 'Kết quả'),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        icon: const Icon(Icons.event_outlined),
                        label: Text(
                          nextActionAt == null
                              ? 'Chọn hành động tiếp theo'
                              : 'Tiếp theo: ${_shortDateTime(nextActionAt!)}',
                        ),
                        onPressed: () async {
                          final selected = await _pickDateTime(
                            context,
                            current: nextActionAt ?? _periodDate(widget.period),
                            firstDate: widget.period.startDate,
                            lastDate: widget.period.endDate,
                          );
                          if (selected != null) {
                            setDialogState(() => nextActionAt = selected);
                          }
                        },
                      ),
                    ),
                    if (nextActionAt != null)
                      IconButton(
                        tooltip: 'Bỏ hành động tiếp theo',
                        onPressed: () =>
                            setDialogState(() => nextActionAt = null),
                        icon: const Icon(Icons.clear),
                      ),
                  ],
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
              child: const Text('Lưu'),
            ),
          ],
        ),
      ),
    );
    if (accepted != true ||
        subject.text.trim().isEmpty ||
        summary.text.trim().isEmpty) {
      return;
    }
    try {
      await widget.api.saveContactLog(
        token: widget.token,
        studentId: widget.student.id,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: widget.classId,
        contactType: type,
        subject: subject.text.trim(),
        summary: summary.text.trim(),
        result: result.text.trim(),
        contactedAt: current?.contactedAt ?? _periodDate(widget.period),
        nextActionAt: nextActionAt,
        logId: current?.id,
      );
      await _load();
    } catch (error) {
      if (mounted) _snack(context, _message(error));
    }
  }

  Future<void> _delete(ParentContactLog item) async {
    await widget.api.deleteContactLog(token: widget.token, logId: item.id);
    await _load();
  }

  @override
  Widget build(BuildContext context) => _AsyncList<ParentContactLog>(
    items: _items,
    error: _error,
    onRetry: _load,
    emptyText: 'Chưa có nhật ký liên hệ.',
    action: FloatingActionButton.small(
      onPressed: _edit,
      child: const Icon(Icons.add),
    ),
    itemBuilder: (item) => Card(
      child: ListTile(
        title: Text(item.subject),
        subtitle: Text(
          '${_contactLabel(item.contactType)} · ${_shortDate(item.contactedAt)}\n${item.summary}'
          '${item.nextActionAt == null ? '' : '\nTiếp theo: ${_shortDateTime(item.nextActionAt!)}'}',
        ),
        isThreeLine: true,
        onTap: () => _edit(item),
        trailing: IconButton(
          icon: const Icon(Icons.delete_outline),
          onPressed: () => _delete(item),
        ),
      ),
    ),
  );
}

class _MeetingsTab extends StatefulWidget {
  const _MeetingsTab({
    required this.token,
    required this.student,
    required this.classId,
    required this.period,
    required this.api,
  });
  final String token;
  final HomeroomStudentDto student;
  final int classId;
  final AcademicPeriod period;
  final HomeroomMonitoringApi api;
  @override
  State<_MeetingsTab> createState() => _MeetingsTabState();
}

class _MeetingsTabState extends State<_MeetingsTab> {
  List<ParentMeeting>? _items;
  String? _error;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final all = await widget.api.getMeetings(
        token: widget.token,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: widget.classId,
      );
      if (mounted) {
        setState(() {
          _items = all
              .where(
                (item) =>
                    item.studentId == null ||
                    item.studentId == widget.student.id,
              )
              .toList();
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    }
  }

  Future<void> _edit([ParentMeeting? current]) async {
    final title = TextEditingController(text: current?.title);
    final location = TextEditingController(text: current?.location);
    final agenda = TextEditingController(text: current?.agenda);
    var startsAt = current?.startsAt ?? _periodDate(widget.period, hour: 17);
    var status = current?.status ?? 'SCHEDULED';
    final accepted = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(
            current == null ? 'Tạo lịch hẹn phụ huynh' : 'Cập nhật lịch hẹn',
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: title,
                  decoration: const InputDecoration(labelText: 'Tiêu đề'),
                ),
                TextField(
                  controller: location,
                  decoration: const InputDecoration(labelText: 'Địa điểm'),
                ),
                TextField(
                  controller: agenda,
                  maxLines: 3,
                  decoration: const InputDecoration(labelText: 'Nội dung'),
                ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.schedule_outlined),
                    label: Text(_shortDateTime(startsAt)),
                    onPressed: () async {
                      final selected = await _pickDateTime(
                        context,
                        current: startsAt,
                        firstDate: widget.period.startDate,
                        lastDate: widget.period.endDate,
                      );
                      if (selected != null) {
                        setDialogState(() => startsAt = selected);
                      }
                    },
                  ),
                ),
                DropdownButtonFormField<String>(
                  initialValue: status,
                  decoration: const InputDecoration(labelText: 'Trạng thái'),
                  items: const ['SCHEDULED', 'COMPLETED', 'CANCELLED']
                      .map(
                        (value) => DropdownMenuItem(
                          value: value,
                          child: Text(_meetingStatusLabel(value)),
                        ),
                      )
                      .toList(),
                  onChanged: (value) => setDialogState(() => status = value!),
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
              child: Text(current == null ? 'Tạo lịch' : 'Lưu thay đổi'),
            ),
          ],
        ),
      ),
    );
    if (accepted != true || title.text.trim().isEmpty) return;
    try {
      await widget.api.saveMeeting(
        token: widget.token,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: widget.classId,
        studentId: widget.student.id,
        title: title.text.trim(),
        startsAt: startsAt,
        location: location.text.trim(),
        agenda: agenda.text.trim(),
        meetingId: current?.id,
        status: status,
      );
      await _load();
    } catch (error) {
      if (mounted) _snack(context, _message(error));
    }
  }

  Future<void> _attendance(
    ParentMeeting meeting,
    ParentMeetingParticipant participant,
    String value,
  ) async {
    await widget.api.markMeetingAttendance(
      token: widget.token,
      meetingId: meeting.id,
      guardianId: participant.guardianId,
      attendance: value,
    );
    await _load();
  }

  @override
  Widget build(BuildContext context) => _AsyncList<ParentMeeting>(
    items: _items,
    error: _error,
    onRetry: _load,
    emptyText: 'Chưa có lịch hẹn.',
    action: FloatingActionButton.small(
      onPressed: _edit,
      child: const Icon(Icons.add),
    ),
    itemBuilder: (item) => Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    item.title,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                IconButton(
                  tooltip: 'Sửa lịch hẹn',
                  onPressed: () => _edit(item),
                  icon: const Icon(Icons.edit_outlined),
                ),
              ],
            ),
            Text(
              '${_shortDateTime(item.startsAt)} · ${item.location} · ${_meetingStatusLabel(item.status)}',
            ),
            if (item.agenda.isNotEmpty) Text(item.agenda),
            const Divider(),
            ...item.participants.map(
              (participant) => Wrap(
                spacing: 8,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Text(
                    '${participant.guardianName}: ${_responseLabel(participant.response)}',
                  ),
                  DropdownButton<String>(
                    value: participant.attendance,
                    items: const ['UNKNOWN', 'ATTENDED', 'ABSENT']
                        .map(
                          (value) => DropdownMenuItem(
                            value: value,
                            child: Text(_attendanceLabel(value)),
                          ),
                        )
                        .toList(),
                    onChanged: (value) {
                      if (value != null) _attendance(item, participant, value);
                    },
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class _StudentEventsTab extends StatefulWidget {
  const _StudentEventsTab({
    required this.token,
    required this.student,
    required this.classId,
    required this.period,
    required this.api,
  });
  final String token;
  final HomeroomStudentDto student;
  final int classId;
  final AcademicPeriod period;
  final HomeroomMonitoringApi api;
  @override
  State<_StudentEventsTab> createState() => _StudentEventsTabState();
}

class _StudentEventsTabState extends State<_StudentEventsTab> {
  List<StudentEvent>? _items;
  String? _error;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = await widget.api.getStudentEvents(
        token: widget.token,
        studentId: widget.student.id,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: widget.classId,
      );
      if (mounted) {
        setState(() {
          _items = items;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    }
  }

  Future<void> _edit([StudentEvent? current]) async {
    final title = TextEditingController(text: current?.title);
    final category = TextEditingController(text: current?.category);
    final description = TextEditingController(text: current?.description);
    var type = current?.eventType ?? 'REWARD';
    var eventDate = current?.eventDate ?? widget.period.referenceDate;
    final accepted = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(
            current == null ? 'Thêm sự kiện học sinh' : 'Sửa sự kiện học sinh',
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: type,
                  items: const ['REWARD', 'VIOLATION', 'NOTE']
                      .map(
                        (value) => DropdownMenuItem(
                          value: value,
                          child: Text(_eventLabel(value)),
                        ),
                      )
                      .toList(),
                  onChanged: (value) => setDialogState(() => type = value!),
                ),
                TextField(
                  controller: title,
                  decoration: const InputDecoration(labelText: 'Tiêu đề'),
                ),
                TextField(
                  controller: category,
                  decoration: const InputDecoration(labelText: 'Nhóm sự kiện'),
                ),
                TextField(
                  controller: description,
                  maxLines: 3,
                  decoration: const InputDecoration(labelText: 'Mô tả'),
                ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.event_outlined),
                    label: Text(_shortDate(eventDate)),
                    onPressed: () async {
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
              child: Text(current == null ? 'Lưu nháp' : 'Lưu thay đổi'),
            ),
          ],
        ),
      ),
    );
    if (accepted != true || title.text.trim().isEmpty) return;
    try {
      await widget.api.saveStudentEvent(
        token: widget.token,
        studentId: widget.student.id,
        academicYearId: widget.period.academicYearId,
        semesterId: widget.period.semesterId,
        classId: widget.classId,
        eventType: type,
        category: category.text.trim(),
        title: title.text.trim(),
        description: description.text.trim(),
        eventDate: eventDate,
        eventId: current?.id,
      );
      await _load();
    } catch (error) {
      if (mounted) _snack(context, _message(error));
    }
  }

  Future<void> _publish(StudentEvent item) async {
    try {
      await widget.api.publishStudentEvent(
        token: widget.token,
        eventId: item.id,
      );
      await _load();
    } catch (error) {
      if (mounted) _snack(context, _message(error));
    }
  }

  @override
  Widget build(BuildContext context) => _AsyncList<StudentEvent>(
    items: _items,
    error: _error,
    onRetry: _load,
    emptyText: 'Chưa có khen thưởng hoặc vi phạm.',
    action: FloatingActionButton.small(
      onPressed: _edit,
      child: const Icon(Icons.add),
    ),
    itemBuilder: (item) => Card(
      child: ListTile(
        leading: Icon(
          item.eventType == 'REWARD'
              ? Icons.emoji_events_outlined
              : item.eventType == 'VIOLATION'
              ? Icons.report_outlined
              : Icons.notes_outlined,
        ),
        title: Text(item.title),
        subtitle: Text(
          '${_eventLabel(item.eventType)} · ${_shortDate(item.eventDate)}\n${item.description}',
        ),
        isThreeLine: true,
        trailing: item.status == 'DRAFT'
            ? PopupMenuButton<String>(
                tooltip: 'Thao tác sự kiện',
                onSelected: (value) {
                  if (value == 'edit') {
                    _edit(item);
                  } else {
                    _publish(item);
                  }
                },
                itemBuilder: (_) => [
                  const PopupMenuItem(
                    value: 'edit',
                    child: Text('Sửa bản nháp'),
                  ),
                  if (item.eventType != 'NOTE')
                    const PopupMenuItem(
                      value: 'publish',
                      child: Text('Công bố'),
                    ),
                ],
              )
            : Text(_statusLabel(item.status)),
      ),
    ),
  );
}

class _AsyncList<T> extends StatelessWidget {
  const _AsyncList({
    required this.items,
    required this.error,
    required this.onRetry,
    required this.emptyText,
    required this.action,
    required this.itemBuilder,
  });
  final List<T>? items;
  final String? error;
  final Future<void> Function() onRetry;
  final String emptyText;
  final Widget action;
  final Widget Function(T) itemBuilder;
  @override
  Widget build(BuildContext context) {
    if (error != null) return _ErrorView(message: error!, onRetry: onRetry);
    if (items == null) return const Center(child: CircularProgressIndicator());
    return Stack(
      children: [
        if (items!.isEmpty)
          _EmptyView(icon: Icons.inbox_outlined, text: emptyText)
        else
          ListView.separated(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 80),
            itemCount: items!.length,
            separatorBuilder: (_, _) => const SizedBox(height: 8),
            itemBuilder: (_, index) => itemBuilder(items![index]),
          ),
        Positioned(right: 16, bottom: 16, child: action),
      ],
    );
  }
}

class _InfoTile extends StatelessWidget {
  const _InfoTile({required this.label, required this.value});
  final String label;
  final String value;
  @override
  Widget build(BuildContext context) => Card(
    child: ListTile(
      title: Text(label),
      trailing: Text(value, style: Theme.of(context).textTheme.titleMedium),
    ),
  );
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.text, required this.color});
  final String text;
  final Color color;
  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      color: color.withValues(alpha: .12),
      borderRadius: BorderRadius.circular(99),
    ),
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Text(
        text,
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.w700,
        ),
      ),
    ),
  );
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});
  final String message;
  final Future<void> Function() onRetry;
  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 36),
          const SizedBox(height: 8),
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    ),
  );
}

class _EmptyView extends StatelessWidget {
  const _EmptyView({required this.icon, required this.text});
  final IconData icon;
  final String text;
  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 42, color: Theme.of(context).colorScheme.primary),
          const SizedBox(height: 10),
          Text(text, textAlign: TextAlign.center),
        ],
      ),
    ),
  );
}

String _message(Object error) =>
    error.toString().replaceFirst('Exception: ', '');
String _displayText(String? value) =>
    value == null || value.trim().isEmpty ? 'Chưa cập nhật' : value.trim();
void _snack(BuildContext context, String text) =>
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
String _shortDate(DateTime value) =>
    '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
String _shortDateTime(DateTime value) =>
    '${_shortDate(value)} ${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')}';
DateTime _periodDate(AcademicPeriod period, {int? hour}) {
  final date = period.referenceDate;
  final now = DateTime.now();
  return DateTime(
    date.year,
    date.month,
    date.day,
    hour ?? now.hour,
    hour == null ? now.minute : 0,
  );
}

Future<DateTime?> _pickDateTime(
  BuildContext context, {
  required DateTime current,
  required DateTime firstDate,
  required DateTime lastDate,
}) async {
  final date = await showDatePicker(
    context: context,
    initialDate: current,
    firstDate: firstDate,
    lastDate: lastDate,
  );
  if (date == null || !context.mounted) return null;
  final time = await showTimePicker(
    context: context,
    initialTime: TimeOfDay.fromDateTime(current),
  );
  if (time == null) return null;
  return DateTime(date.year, date.month, date.day, time.hour, time.minute);
}

String _riskLabel(String value) =>
    const {
      'LOW_GPA': 'GPA thấp',
      'LOW_ATTENDANCE': 'Chuyên cần thấp',
      'UNEXCUSED_ABSENCE': 'Vắng không phép',
      'CONDUCT': 'Hạnh kiểm',
      'OVERDUE_TUITION': 'Học phí quá hạn',
    }[value] ??
    value;
String _statusLabel(String value) =>
    const {
      'OPEN': 'Đang mở',
      'ACKNOWLEDGED': 'Đã tiếp nhận',
      'RESOLVED': 'Đã xử lý',
      'DRAFT': 'Bản nháp',
      'PUBLISHED': 'Đã công bố',
    }[value] ??
    value;
String _contactLabel(String value) =>
    const {
      'CALL': 'Cuộc gọi',
      'MESSAGE': 'Tin nhắn',
      'MEETING': 'Cuộc họp',
      'OTHER': 'Khác',
    }[value] ??
    value;
String _eventLabel(String value) =>
    const {
      'REWARD': 'Khen thưởng',
      'VIOLATION': 'Vi phạm',
      'NOTE': 'Ghi chú nội bộ',
    }[value] ??
    value;
String _responseLabel(String value) =>
    const {
      'PENDING': 'Chờ phản hồi',
      'ACCEPTED': 'Tham gia',
      'DECLINED': 'Từ chối',
    }[value] ??
    value;
String _meetingStatusLabel(String value) =>
    const {
      'SCHEDULED': 'Đã lên lịch',
      'COMPLETED': 'Đã hoàn thành',
      'CANCELLED': 'Đã hủy',
    }[value] ??
    value;
String _attendanceLabel(String value) =>
    const {
      'UNKNOWN': 'Chưa ghi nhận',
      'ATTENDED': 'Có mặt',
      'ABSENT': 'Vắng mặt',
    }[value] ??
    value;
Color _severityColor(String value) => switch (value) {
  'CRITICAL' => Colors.red.shade800,
  'HIGH' => Colors.red,
  'MEDIUM' => Colors.orange,
  _ => Colors.blue,
};
Color _statusColor(String value) => switch (value) {
  'RESOLVED' => Colors.green,
  'ACKNOWLEDGED' => Colors.orange,
  _ => Colors.red,
};
