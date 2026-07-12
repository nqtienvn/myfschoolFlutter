import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

class StudentAttendanceScreen extends StatefulWidget {
  const StudentAttendanceScreen({
    super.key,
    required this.student,
    required this.token,
    this.viewAsStudent = false,
  });

  final StudentSnapshot student;
  final String token;
  final bool viewAsStudent;

  @override
  State<StudentAttendanceScreen> createState() => _StudentAttendanceScreenState();
}
class _StudentAttendanceScreenState extends State<StudentAttendanceScreen> {
  late final AttendanceApiClient _apiClient;
  bool _isLoading = true;
  String? _errorMessage;
  List<AttendanceEvent> _events = [];
  double _attendanceRate = 0;
  int _presentCount = 0;
  int _absentCount = 0;
  int _absentWithLeave = 0;
  int _absentWithoutLeave = 0;
  int _totalSessions = 0;
  String _semesterName = '';

  @override
  void initState() {
    super.initState();
    _apiClient = AttendanceApiClient(backend: BackendApiClient());
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final result = await _apiClient.getStudentAttendanceLog(
        token: widget.token,
        studentId: widget.viewAsStudent ? null : widget.student.id,
      );

      setState(() {
        _events = result['events'] as List<AttendanceEvent>;
        _attendanceRate = result['attendanceRate'] as double;
        _presentCount = result['presentCount'] as int;
        _absentCount = result['absentCount'] as int;
        _absentWithLeave = result['absentWithLeave'] as int;
        _absentWithoutLeave = result['absentWithoutLeave'] as int;
        _totalSessions = result['totalSessions'] as int;
        _semesterName = result['semesterName'] as String;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = e.toString().replaceAll('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Chuyên cần'),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _loadData,
          child: _isLoading
              ? const Center(child: CircularProgressIndicator())
              : _errorMessage != null
                  ? Center(
                      child: Padding(
                        padding: const EdgeInsets.all(AppSpacing.lg),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(_errorMessage!, style: const TextStyle(color: AppColors.danger), textAlign: TextAlign.center),
                            const SizedBox(height: AppSpacing.md),
                            ElevatedButton(onPressed: _loadData, child: const Text('Thử lại')),
                          ],
                        ),
                      ),
                    )
                  : ListView(
                      padding: const EdgeInsets.all(AppSpacing.lg),
                      children: [
                        AppCard(
                          child: Row(
                            children: [
                              Expanded(
                                child: Column(
                                  children: [
                                    const Text(
                                      'Tỷ lệ đi học',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: AppColors.muted,
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                    const SizedBox(height: AppSpacing.xs),
                                    Text(
                                      '${_attendanceRate.toStringAsFixed(1)}%',
                                      style: const TextStyle(
                                        fontSize: 24,
                                        fontWeight: FontWeight.w900,
                                        color: AppColors.fptOrange,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Container(width: 1, height: 40, color: AppColors.line),
                              Expanded(
                                child: Column(
                                  children: [
                                    const Text(
                                      'Số buổi vắng',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: AppColors.muted,
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                    const SizedBox(height: AppSpacing.xs),
                                    Text(
                                      '$_absentCount',
                                      style: const TextStyle(
                                        fontSize: 24,
                                        fontWeight: FontWeight.w900,
                                        color: AppColors.danger,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: AppSpacing.sm),
                        AppCard(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _semesterName.isEmpty ? 'Học kỳ hiện tại' : _semesterName,
                                style: const TextStyle(fontWeight: FontWeight.w900, color: AppColors.ink),
                              ),
                              const SizedBox(height: AppSpacing.md),
                              Row(
                                children: [
                                  Expanded(child: _SummaryValue(label: 'Có mặt', value: _presentCount, color: AppColors.green)),
                                  Expanded(child: _SummaryValue(label: 'Vắng phép', value: _absentWithLeave, color: AppColors.blue)),
                                  Expanded(child: _SummaryValue(label: 'Vắng KP', value: _absentWithoutLeave, color: AppColors.danger)),
                                ],
                              ),
                              const SizedBox(height: AppSpacing.sm),
                              Text('Tổng số buổi đã điểm danh: $_totalSessions', style: const TextStyle(fontSize: 12, color: AppColors.muted)),
                            ],
                          ),
                        ),
                        const SizedBox(height: AppSpacing.lg),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const SectionHeader(title: 'Nhật ký điểm danh'),
                            TextButton.icon(
                              onPressed: _loadData,
                              icon: const Icon(Icons.refresh, size: 16),
                              label: const Text('Tải lại'),
                            ),
                          ],
                        ),
                        const SizedBox(height: AppSpacing.xs),
                        if (_events.isEmpty)
                          const AppCard(
                            child: Center(
                              child: Padding(
                                padding: EdgeInsets.all(AppSpacing.lg),
                                child: Text(
                                  'Chưa có dữ liệu điểm danh',
                                  style: TextStyle(color: AppColors.muted),
                                ),
                              ),
                            ),
                          )
                        else
                          for (final event in _events) ...[
                            _AttendanceEventTile(event: event),
                            const SizedBox(height: AppSpacing.sm),
                          ],
                      ],
                    ),
        ),
      ),
    );
  }
}

class _SummaryValue extends StatelessWidget {
  const _SummaryValue({required this.label, required this.value, required this.color});

  final String label;
  final int value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text('$value', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: color)),
        const SizedBox(height: 2),
        Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 10, color: AppColors.muted)),
      ],
    );
  }
}

class _AttendanceEventTile extends StatelessWidget {
  const _AttendanceEventTile({required this.event});

  final AttendanceEvent event;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: AppSpacing.sm,
            ),
            decoration: BoxDecoration(
              color: AppColors.background,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              children: [
                const Text(
                  'Ngày',
                  style: TextStyle(fontSize: 10, color: AppColors.muted),
                ),
                Text(
                  event.date,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w900,
                    color: AppColors.ink,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      event.session == 'Cả ngày'
                          ? 'Cả ngày'
                          : 'Buổi ${event.session.toLowerCase()}',
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w900,
                        color: AppColors.ink,
                      ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    StatusPill(
                      label: event.status,
                      foreground: event.color,
                      background: event.background,
                      compact: true,
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  event.reason,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.muted,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
