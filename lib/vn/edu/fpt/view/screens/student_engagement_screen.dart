import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';

class StudentEngagementScreen extends StatefulWidget {
  const StudentEngagementScreen({
    super.key,
    required this.authService,
    this.api,
    this.studentId,
  });

  final AuthService authService;
  final HomeroomMonitoringApi? api;
  final int? studentId;

  @override
  State<StudentEngagementScreen> createState() =>
      _StudentEngagementScreenState();
}

class _StudentEngagementScreenState extends State<StudentEngagementScreen> {
  late final HomeroomMonitoringApi _api;
  AcademicPeriod? _period;
  int? _studentId;
  List<ParentMeeting>? _meetings;
  List<StudentEvent>? _events;
  String? _error;

  bool get _isParent => widget.authService.currentSession?.role == 'PARENT';

  @override
  void initState() {
    super.initState();
    _api = widget.api ?? HomeroomMonitoringApiClient();
    _studentId = widget.studentId ?? widget.authService.selectedChild?.id;
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
    final session = widget.authService.currentSession;
    final period = _period;
    if (session == null || period == null) return;
    setState(() {
      _meetings = null;
      _events = null;
      _error = null;
    });
    try {
      var studentId = _studentId;
      if (studentId == null) {
        studentId = await PeriodicReviewApiClient().resolveStudentId(
          token: session.token,
        );
        _studentId = studentId;
      }
      final futures = <Future<Object>>[
        _api.getStudentEvents(
          token: session.token,
          studentId: studentId,
          academicYearId: period.academicYearId,
          semesterId: period.semesterId,
        ),
        if (_isParent)
          _api.getMeetings(
            token: session.token,
            academicYearId: period.academicYearId,
            semesterId: period.semesterId,
          ),
      ];
      final values = await Future.wait(futures);
      if (!mounted) return;
      setState(() {
        _events = values.first as List<StudentEvent>;
        _meetings = _isParent
            ? (values[1] as List<ParentMeeting>)
                  .where(
                    (meeting) =>
                        meeting.studentId == null ||
                        meeting.studentId == studentId,
                  )
                  .toList()
            : const [];
      });
    } catch (error) {
      if (mounted) {
        setState(
          () => _error = error.toString().replaceFirst('Exception: ', ''),
        );
      }
    }
  }

  Future<void> _respond(ParentMeeting meeting, String response) async {
    final session = widget.authService.currentSession!;
    try {
      await _api.respondMeeting(
        token: session.token,
        meetingId: meeting.id,
        response: response,
      );
      await _load();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final period = _period;
    final tabs = _isParent ? 2 : 1;
    return DefaultTabController(
      length: tabs,
      child: Scaffold(
        appBar: AppBar(
          title: Text(
            period == null ? 'Hồ sơ học kỳ' : 'Hồ sơ · ${period.semesterName}',
          ),
          bottom: TabBar(
            tabs: [
              if (_isParent) const Tab(text: 'Lịch họp'),
              const Tab(text: 'Khen thưởng/vi phạm'),
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
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 12),
              OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
            ],
          ),
        ),
      );
    }
    return TabBarView(
      children: [
        if (_isParent)
          _MeetingInvitations(meetings: _meetings, onRespond: _respond),
        _PublishedEvents(events: _events),
      ],
    );
  }
}

class _MeetingInvitations extends StatelessWidget {
  const _MeetingInvitations({required this.meetings, required this.onRespond});
  final List<ParentMeeting>? meetings;
  final Future<void> Function(ParentMeeting, String) onRespond;

  @override
  Widget build(BuildContext context) {
    if (meetings == null) {
      return const Center(child: CircularProgressIndicator());
    }
    if (meetings!.isEmpty) {
      return const _PublicEmpty(
        icon: Icons.event_available_outlined,
        text: 'Không có lời mời họp trong học kỳ này.',
      );
    }
    return ListView.separated(
      key: const Key('parent-meeting-list'),
      padding: const EdgeInsets.all(12),
      itemCount: meetings!.length,
      separatorBuilder: (_, _) => const SizedBox(height: 8),
      itemBuilder: (_, index) {
        final meeting = meetings![index];
        final response = meeting.participants.isEmpty
            ? 'PENDING'
            : meeting.participants.first.response;
        return Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  meeting.title,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 5),
                Text(
                  '${_dateTime(meeting.startsAt)} · ${meeting.location.isEmpty ? 'Chưa cập nhật địa điểm' : meeting.location}',
                ),
                if (meeting.agenda.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(meeting.agenda),
                  ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Text('Phản hồi: ${_response(response)}'),
                    OutlinedButton(
                      onPressed: () => onRespond(meeting, 'DECLINED'),
                      child: const Text('Từ chối'),
                    ),
                    FilledButton(
                      onPressed: () => onRespond(meeting, 'ACCEPTED'),
                      child: const Text('Sẽ tham gia'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _PublishedEvents extends StatelessWidget {
  const _PublishedEvents({required this.events});
  final List<StudentEvent>? events;
  @override
  Widget build(BuildContext context) {
    if (events == null) return const Center(child: CircularProgressIndicator());
    final visible = events!
        .where(
          (event) => event.status == 'PUBLISHED' && event.eventType != 'NOTE',
        )
        .toList();
    if (visible.isEmpty) {
      return const _PublicEmpty(
        icon: Icons.emoji_events_outlined,
        text: 'Chưa có khen thưởng hoặc vi phạm được công bố.',
      );
    }
    return ListView.separated(
      key: const Key('published-event-list'),
      padding: const EdgeInsets.all(12),
      itemCount: visible.length,
      separatorBuilder: (_, _) => const SizedBox(height: 8),
      itemBuilder: (_, index) {
        final event = visible[index];
        final reward = event.eventType == 'REWARD';
        return Card(
          child: ListTile(
            leading: CircleAvatar(
              child: Icon(
                reward ? Icons.emoji_events_outlined : Icons.report_outlined,
              ),
            ),
            title: Text(event.title),
            subtitle: Text(
              '${reward ? 'Khen thưởng' : 'Vi phạm'} · ${_date(event.eventDate)}\n${event.description}',
            ),
            isThreeLine: true,
          ),
        );
      },
    );
  }
}

class _PublicEmpty extends StatelessWidget {
  const _PublicEmpty({required this.icon, required this.text});
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

String _date(DateTime value) =>
    '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
String _dateTime(DateTime value) =>
    '${_date(value)} ${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')}';
String _response(String value) =>
    const {
      'PENDING': 'Chờ phản hồi',
      'ACCEPTED': 'Sẽ tham gia',
      'DECLINED': 'Từ chối',
    }[value] ??
    value;
