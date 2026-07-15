import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/exception/backend_api_exception.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/school_schedule.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/schedule_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';

enum ScheduleViewMode { student, teacher }

class ScheduleScreen extends StatefulWidget {
  const ScheduleScreen({
    super.key,
    required this.service,
    required this.mode,
    this.studentId,
    this.studentName,
  });

  final ScheduleService service;
  final ScheduleViewMode mode;
  final int? studentId;
  final String? studentName;

  @override
  State<ScheduleScreen> createState() => _ScheduleScreenState();
}

class _ScheduleScreenState extends State<ScheduleScreen> {
  int _selectedDay = 2;
  SchoolSchedule? _schedule;
  bool _loading = true;
  String? _error;
  String? _loadedPeriod;

  @override
  void initState() {
    super.initState();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    final key = period == null
        ? 'current'
        : '${period.academicYearId}-${period.semesterId}';
    if (_loadedPeriod != key) {
      _loadedPeriod = key;
      _load(period);
    }
  }

  Future<void> _load([AcademicPeriod? period]) async {
    final requestKey = period == null
        ? 'current'
        : '${period.academicYearId}-${period.semesterId}';
    setState(() {
      _loading = true;
      _error = null;
      _schedule = null;
    });
    try {
      final schedule = widget.studentId == null
          ? await widget.service.getMine(
              semesterId: period?.semesterId,
              date: period?.referenceDate,
            )
          : await widget.service.getForStudent(
              widget.studentId!,
              semesterId: period?.semesterId,
              date: period?.referenceDate,
            );
      if (!mounted || _loadedPeriod != requestKey) return;
      setState(() => _schedule = schedule);
    } on BackendApiException catch (error) {
      if (mounted && _loadedPeriod == requestKey) {
        setState(() => _error = error.message);
      }
    } catch (_) {
      if (mounted && _loadedPeriod == requestKey) {
        setState(
          () => _error = 'Không thể tải thời khóa biểu. Vui lòng thử lại.',
        );
      }
    } finally {
      if (mounted && _loadedPeriod == requestKey) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    final schedule = _schedule;
    final title = widget.mode == ScheduleViewMode.teacher
        ? 'Lịch dạy của tôi'
        : widget.studentName == null
        ? 'Thời khóa biểu của tôi'
        : 'Thời khóa biểu · ${widget.studentName}';

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: OrangeTopBar(title: title),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () => _load(period),
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            children: [
              if (schedule != null) _buildSummary(schedule),
              if (schedule != null) const SizedBox(height: 12),
              _buildDayPicker(),
              const SizedBox(height: 20),
              if (_loading)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 64),
                  child: Center(
                    child: CircularProgressIndicator(
                      color: AppColors.fptOrange,
                    ),
                  ),
                )
              else if (_error != null)
                _buildError()
              else if (schedule != null)
                _buildDay(schedule.day(_selectedDay)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSummary(SchoolSchedule schedule) {
    final label = widget.mode == ScheduleViewMode.teacher
        ? '${schedule.semesterName} · Lịch dạy theo phân công bộ môn'
        : '${schedule.ownerName} · ${schedule.semesterName}';
    return AppCard(
      padding: 14,
      child: Row(
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: AppColors.primarySoft,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              widget.mode == ScheduleViewMode.teacher
                  ? Icons.co_present
                  : Icons.school,
              color: AppColors.fptOrange,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              label,
              style: const TextStyle(
                color: AppColors.ink,
                fontWeight: FontWeight.w800,
                fontSize: 14,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDayPicker() {
    const days = [
      (2, 'T2'),
      (3, 'T3'),
      (4, 'T4'),
      (5, 'T5'),
      (6, 'T6'),
      (7, 'T7'),
      (1, 'CN'),
    ];
    return AppCard(
      padding: 12,
      child: Column(
        children: [
          Text(
            widget.mode == ScheduleViewMode.teacher
                ? 'LỊCH DẠY THEO THỨ'
                : 'LỊCH HỌC THEO THỨ',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w900,
              color: AppColors.muted,
              letterSpacing: 0.6,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: days
                .map((day) {
                  final selected = day.$1 == _selectedDay;
                  return Expanded(
                    child: Semantics(
                      button: true,
                      selected: selected,
                      label: day.$2,
                      child: InkWell(
                        borderRadius: BorderRadius.circular(10),
                        onTap: () => setState(() => _selectedDay = day.$1),
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 160),
                          margin: const EdgeInsets.symmetric(horizontal: 2),
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          decoration: BoxDecoration(
                            color: selected
                                ? AppColors.fptOrange
                                : Colors.transparent,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            day.$2,
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w900,
                              color: selected ? Colors.white : AppColors.ink,
                            ),
                          ),
                        ),
                      ),
                    ),
                  );
                })
                .toList(growable: false),
          ),
        ],
      ),
    );
  }

  Widget _buildDay(ScheduleDay day) {
    final lessons = [...day.morningLessons, ...day.afternoonLessons];
    if (lessons.isEmpty) {
      return AppCard(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 36),
          child: Column(
            children: [
              const Icon(
                Icons.event_available,
                size: 44,
                color: AppColors.muted,
              ),
              const SizedBox(height: 12),
              Text(
                widget.mode == ScheduleViewMode.teacher
                    ? 'Không có tiết dạy trong ngày này.'
                    : 'Không có tiết học trong ngày này.',
                style: const TextStyle(
                  color: AppColors.muted,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (day.morningLessons.isNotEmpty) ...[
          _sectionTitle('Buổi sáng'),
          ...day.morningLessons.map(_lessonCard),
        ],
        if (day.afternoonLessons.isNotEmpty) ...[
          const SizedBox(height: 12),
          _sectionTitle('Buổi chiều'),
          ...day.afternoonLessons.map(_lessonCard),
        ],
      ],
    );
  }

  Widget _sectionTitle(String title) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: Text(
      title.toUpperCase(),
      style: const TextStyle(
        color: AppColors.muted,
        fontSize: 11,
        fontWeight: FontWeight.w900,
        letterSpacing: 0.6,
      ),
    ),
  );

  Widget _lessonCard(ScheduleLesson lesson) {
    final color = _lessonColor(lesson.shift);
    final detail = widget.mode == ScheduleViewMode.teacher
        ? 'Lớp ${lesson.className}'
        : lesson.teacherName;
    final room = lesson.room.isEmpty ? lesson.className : lesson.room;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: AppCard(
        padding: 0,
        child: IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                width: 4,
                decoration: BoxDecoration(
                  color: color,
                  borderRadius: const BorderRadius.horizontal(
                    left: Radius.circular(16),
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 14, 10, 14),
                child: Semantics(
                  label: lesson.periodName,
                  child: Container(
                    key: ValueKey('schedule-period-${lesson.id}'),
                    width: 54,
                    height: 58,
                    decoration: BoxDecoration(
                      color: color.withValues(alpha: 0.08),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: color.withValues(alpha: 0.18)),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'TIẾT',
                          style: TextStyle(
                            fontSize: 9,
                            height: 1,
                            letterSpacing: 0.5,
                            color: color,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          '${lesson.period}',
                          style: TextStyle(
                            fontSize: 24,
                            height: 1,
                            color: color,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(2, 14, 14, 14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        lesson.subjectName,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 15,
                          height: 1.2,
                          fontWeight: FontWeight.w900,
                          color: AppColors.ink,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Flexible(
                            flex: 3,
                            child: _lessonMeta(
                              widget.mode == ScheduleViewMode.teacher
                                  ? Icons.groups_2_outlined
                                  : Icons.person_outline,
                              detail,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Flexible(
                            flex: 2,
                            child: _lessonMeta(
                              Icons.meeting_room_outlined,
                              room,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _lessonMeta(IconData icon, String label) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
    decoration: BoxDecoration(
      color: AppColors.background,
      borderRadius: BorderRadius.circular(8),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: AppColors.muted),
        const SizedBox(width: 4),
        Flexible(
          child: Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontSize: 11.5,
              color: AppColors.muted,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    ),
  );

  Color _lessonColor(String shift) =>
      shift.toUpperCase() == 'AFTERNOON' ? AppColors.sunset : AppColors.sunrise;

  Widget _buildError() => AppCard(
    child: Padding(
      padding: const EdgeInsets.symmetric(vertical: 28),
      child: Column(
        children: [
          const Icon(
            Icons.cloud_off_outlined,
            size: 42,
            color: AppColors.danger,
          ),
          const SizedBox(height: 12),
          Text(
            _error!,
            textAlign: TextAlign.center,
            style: const TextStyle(
              color: AppColors.muted,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: _load,
            icon: const Icon(Icons.refresh),
            label: const Text('Thử lại'),
          ),
        ],
      ),
    ),
  );
}
