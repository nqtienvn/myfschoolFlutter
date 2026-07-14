import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherAttendanceScreen extends StatefulWidget {
  const TeacherAttendanceScreen({
    super.key,
    required this.token,
    required this.date,
    this.apiClient,
  });

  final String token;
  final DateTime date;
  final AttendanceApiClient? apiClient;

  @override
  State<TeacherAttendanceScreen> createState() =>
      _TeacherAttendanceScreenState();
}

class _TeacherAttendanceScreenState extends State<TeacherAttendanceScreen> {
  late final AttendanceApiClient _apiClient;
  bool _isLoading = true;
  String? _errorMessage;
  List<_StudentAttendanceState> _students = [];
  int? _classId;
  String _className = 'Lớp chủ nhiệm';
  List<String> _shifts = const [];
  String? _selectedShift;
  bool _isSubmitted = false;
  bool _canEdit = true;
  bool _isEditing = false;
  int _scheduledPeriods = 0;
  bool _correctionPending = false;
  int _loadVersion = 0;

  @override
  void initState() {
    super.initState();
    _apiClient =
        widget.apiClient ?? AttendanceApiClient(backend: BackendApiClient());
    _loadContextAndData();
  }

  Future<void> _loadContextAndData() async {
    final version = ++_loadVersion;
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final selectedDate = _dateString();
      final contextData = await _apiClient.getHomeroomContext(
        token: widget.token,
        date: selectedDate,
      );
      final shifts = (contextData['shifts'] as List? ?? const [])
          .whereType<String>()
          .toList(growable: false);
      if (!mounted || version != _loadVersion) return;
      if (shifts.isEmpty) {
        setState(() {
          _classId = contextData['classId'] as int;
          _className = contextData['className'] as String? ?? 'Lớp chủ nhiệm';
          _shifts = const [];
          _selectedShift = null;
          _students = [];
          _isLoading = false;
        });
        return;
      }
      final selectedShift =
          _selectedShift != null && shifts.contains(_selectedShift)
          ? _selectedShift!
          : (shifts.contains('MORNING') ? 'MORNING' : shifts.first);
      _classId = contextData['classId'] as int;
      _className = contextData['className'] as String? ?? 'Lớp chủ nhiệm';
      _shifts = shifts;
      _selectedShift = selectedShift;
      await _loadDailyData(version: version);
    } catch (e) {
      if (!mounted || version != _loadVersion) return;
      setState(() {
        _errorMessage = e.toString().replaceAll('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  Future<void> _loadDailyData({int? version}) async {
    final requestVersion = version ?? ++_loadVersion;
    final classId = _classId;
    final selectedShift = _selectedShift;
    if (classId == null || selectedShift == null) return;
    try {
      final selectedDate = _dateString();
      final data = await _apiClient.getDailyAttendance(
        token: widget.token,
        classId: classId,
        date: selectedDate,
        shift: selectedShift,
      );

      final entries = data['students'] as List? ?? [];
      final list = entries.map((e) {
        final map = e as Map<String, dynamic>;
        final studentId = map['studentId'] as int;
        final name = map['studentName'] as String;
        final code = map['studentCode'] as String;
        // Nếu chưa điểm danh (status = null), mặc định là 'PRESENT'
        final status = map['status'] as String? ?? 'PRESENT';
        return _StudentAttendanceState(
          studentId: studentId,
          name: name,
          code: code,
          status: status,
          hasApprovedLeave: map['hasApprovedLeave'] == true,
        );
      }).toList();

      if (!mounted ||
          requestVersion != _loadVersion ||
          classId != _classId ||
          selectedShift != _selectedShift) {
        return;
      }
      setState(() {
        _students = list;
        _isSubmitted = data['submitted'] == true;
        _canEdit = data['canEdit'] == true;
        _scheduledPeriods = data['scheduledPeriods'] as int? ?? 0;
        _correctionPending = data['correctionPending'] == true;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted || requestVersion != _loadVersion) return;
      setState(() {
        _errorMessage = e.toString().replaceAll('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  Future<void> _submit() async {
    final classId = _classId;
    final selectedShift = _selectedShift;
    if (classId == null || selectedShift == null) return;
    setState(() => _isLoading = true);
    try {
      final selectedDate = _dateString();
      final entriesJson = _students
          .map((s) => {'studentId': s.studentId, 'status': s.status})
          .toList();

      if (_isSubmitted) {
        await _apiClient.requestAttendanceCorrection(
          token: widget.token,
          classId: classId,
          date: selectedDate,
          shift: selectedShift,
          entries: entriesJson,
        );
      } else {
        await _apiClient.submitAttendance(
          token: widget.token,
          classId: classId,
          date: selectedDate,
          shift: selectedShift,
          entries: entriesJson,
        );
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              _isSubmitted
                  ? 'Đã gửi yêu cầu sửa điểm danh để Admin duyệt.'
                  : 'Lưu điểm danh thành công!',
            ),
            behavior: SnackBarBehavior.floating,
          ),
        );
        await _loadDailyData();
        if (mounted) setState(() => _isEditing = false);
      }
    } catch (e) {
      setState(() {
        _errorMessage = e.toString().replaceAll('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  String _dateString() {
    final date = widget.date;
    return '${date.year.toString().padLeft(4, '0')}-'
        '${date.month.toString().padLeft(2, '0')}-'
        '${date.day.toString().padLeft(2, '0')}';
  }

  String _dateDisplay() {
    final date = widget.date;
    const weekdays = [
      'Thứ hai',
      'Thứ ba',
      'Thứ tư',
      'Thứ năm',
      'Thứ sáu',
      'Thứ bảy',
      'Chủ nhật',
    ];
    return '${weekdays[date.weekday - 1]}, '
        '${date.day.toString().padLeft(2, '0')}/'
        '${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  @override
  Widget build(BuildContext context) {
    int present = _students.where((r) => r.status == 'PRESENT').length;
    int absent = _students.where((r) => r.status.contains('ABSENT')).length;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: OrangeTopBar(title: 'Điểm danh: $_className'),
      body: SafeArea(
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _errorMessage != null
            ? Center(
                child: Padding(
                  padding: const EdgeInsets.all(AppSpacing.lg),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        _errorMessage!,
                        style: const TextStyle(color: AppColors.danger),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: AppSpacing.md),
                      ElevatedButton(
                        onPressed: _loadContextAndData,
                        child: const Text('Thử lại'),
                      ),
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
                        const Icon(
                          Icons.calendar_today_outlined,
                          color: AppColors.fptOrange,
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _dateDisplay(),
                                style: const TextStyle(
                                  fontWeight: FontWeight.w900,
                                  color: AppColors.ink,
                                ),
                              ),
                              const SizedBox(height: 3),
                              Text(
                                _shifts.isEmpty
                                    ? 'Ngày này lớp không có lịch học'
                                    : '$_scheduledPeriods tiết trong buổi đã chọn',
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
                  ),
                  const SizedBox(height: AppSpacing.md),
                  if (_shifts.isEmpty)
                    const AppCard(
                      child: Padding(
                        padding: EdgeInsets.all(AppSpacing.md),
                        child: Text(
                          'Không cần điểm danh vì lớp không có tiết học trong thời khóa biểu ngày đã chọn.',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: AppColors.muted,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    )
                  else ...[
                    if (_isSubmitted)
                      AppCard(
                        backgroundColor: AppColors.greenSoft,
                        child: Row(
                          children: [
                            const Icon(
                              Icons.check_circle,
                              color: AppColors.green,
                            ),
                            const SizedBox(width: AppSpacing.md),
                            const Expanded(
                              child: Text(
                                'Đã điểm danh buổi này',
                                style: TextStyle(
                                  fontWeight: FontWeight.w900,
                                  color: AppColors.green,
                                ),
                              ),
                            ),
                            if (_canEdit && !_isEditing && !_correctionPending)
                              TextButton.icon(
                                onPressed: () =>
                                    setState(() => _isEditing = true),
                                icon: const Icon(Icons.edit_outlined, size: 18),
                                label: const Text('Sửa điểm danh'),
                              ),
                          ],
                        ),
                      ),
                    if (_correctionPending) ...[
                      const SizedBox(height: AppSpacing.sm),
                      const AppCard(
                        backgroundColor: AppColors.warningSoft,
                        child: Row(
                          children: [
                            Icon(
                              Icons.hourglass_top_rounded,
                              color: AppColors.warning,
                            ),
                            SizedBox(width: AppSpacing.md),
                            Expanded(
                              child: Text(
                                'Yêu cầu sửa điểm danh đang chờ Admin duyệt. Dữ liệu hiện tại chưa thay đổi.',
                                style: TextStyle(
                                  fontWeight: FontWeight.w800,
                                  color: AppColors.warning,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                    if (_isSubmitted) const SizedBox(height: AppSpacing.md),
                    // Stats Panel
                    AppCard(
                      child: Row(
                        children: [
                          Expanded(
                            child: _StatColumn(
                              label: 'Có mặt',
                              value: '$present',
                              color: AppColors.green,
                            ),
                          ),
                          Container(
                            width: 1,
                            height: 35,
                            color: AppColors.line,
                          ),
                          Expanded(
                            child: _StatColumn(
                              label: 'Vắng mặt',
                              value: '$absent',
                              color: AppColors.danger,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    if (_shifts.length > 1) ...[
                      Wrap(
                        spacing: AppSpacing.sm,
                        children: _shifts.map((shift) {
                          final selected = shift == _selectedShift;
                          return ChoiceChip(
                            label: Text(
                              shift == 'MORNING' ? 'Buổi sáng' : 'Buổi chiều',
                            ),
                            selected: selected,
                            onSelected: selected
                                ? null
                                : (_) async {
                                    setState(() {
                                      _selectedShift = shift;
                                      _isLoading = true;
                                    });
                                    await _loadDailyData();
                                  },
                          );
                        }).toList(),
                      ),
                      const SizedBox(height: AppSpacing.md),
                    ],
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        SectionHeader(
                          title: 'Danh sách học sinh (${_students.length})',
                        ),
                        TextButton.icon(
                          onPressed: _loadDailyData,
                          icon: const Icon(Icons.refresh, size: 16),
                          label: const Text('Tải lại'),
                        ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    if (_students.isEmpty)
                      const AppCard(
                        child: Center(
                          child: Padding(
                            padding: EdgeInsets.all(AppSpacing.lg),
                            child: Text(
                              'Lớp không có học sinh nào.',
                              style: TextStyle(color: AppColors.muted),
                            ),
                          ),
                        ),
                      )
                    else
                      for (int i = 0; i < _students.length; i++) ...[
                        _RosterTile(
                          student: _students[i],
                          onToggleAbsent: _isSubmitted && !_isEditing
                              ? null
                              : () {
                                  setState(() {
                                    final currentStatus = _students[i].status;
                                    if (currentStatus.contains('ABSENT')) {
                                      _students[i].status = 'PRESENT';
                                    } else {
                                      // Mặc định vắng không phép, nếu có đơn thì chuyển vắng có phép
                                      _students[i].status =
                                          'ABSENT_WITHOUT_LEAVE';
                                    }
                                  });
                                },
                        ),
                        const SizedBox(height: AppSpacing.sm),
                      ],
                    const SizedBox(height: AppSpacing.lg),
                    PrimaryButton(
                      label: _isSubmitted
                          ? 'Lưu thay đổi điểm danh'
                          : 'Lưu điểm danh lớp',
                      icon: Icons.save_outlined,
                      onPressed:
                          (!_canEdit ||
                              _correctionPending ||
                              (_isSubmitted && !_isEditing))
                          ? null
                          : _submit,
                    ),
                  ],
                ],
              ),
      ),
    );
  }
}

class _StudentAttendanceState {
  _StudentAttendanceState({
    required this.studentId,
    required this.name,
    required this.code,
    required this.status,
    required this.hasApprovedLeave,
  });

  final int studentId;
  final String name;
  final String code;
  String status;
  final bool hasApprovedLeave;
}

class _StatColumn extends StatelessWidget {
  const _StatColumn({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            color: AppColors.muted,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: AppSpacing.xs),
        Text(
          value,
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w900,
            color: color,
          ),
        ),
      ],
    );
  }
}

class _RosterTile extends StatelessWidget {
  const _RosterTile({required this.student, required this.onToggleAbsent});

  final _StudentAttendanceState student;
  final VoidCallback? onToggleAbsent;

  @override
  Widget build(BuildContext context) {
    final isAbsent = student.status.contains('ABSENT');
    final isAbsentWithLeave =
        student.status == 'ABSENT_WITH_LEAVE' ||
        (isAbsent && student.hasApprovedLeave);

    return AppCard(
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  student.name,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w900,
                    color: AppColors.ink,
                  ),
                ),
                const SizedBox(height: 2),
                Row(
                  children: [
                    Text(
                      student.code,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.muted,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    if (isAbsent || student.hasApprovedLeave) ...[
                      const SizedBox(width: AppSpacing.sm),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 6,
                          vertical: 2,
                        ),
                        decoration: BoxDecoration(
                          color: isAbsentWithLeave
                              ? AppColors.blueSoft
                              : AppColors.dangerSoft,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          student.hasApprovedLeave && !isAbsent
                              ? 'Có đơn đã duyệt'
                              : (isAbsentWithLeave
                                    ? 'Nghỉ có phép'
                                    : 'Nghỉ không phép'),
                          style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                            color: isAbsentWithLeave
                                ? AppColors.blue
                                : AppColors.danger,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),
          // Chỉ hiện duy nhất nút vắng
          // Nếu bấm vắng, nút vắng đổi sang màu đỏ nổi bật
          // Nếu không bấm, nghĩa là học sinh đó có mặt
          Column(
            children: [
              OutlinedButton(
                onPressed: onToggleAbsent,
                style: OutlinedButton.styleFrom(
                  backgroundColor: isAbsent
                      ? (isAbsentWithLeave
                            ? AppColors.blue.withValues(alpha: 0.12)
                            : AppColors.danger.withValues(alpha: 0.12))
                      : Colors.transparent,
                  side: BorderSide(
                    color: isAbsent
                        ? (isAbsentWithLeave
                              ? AppColors.blue
                              : AppColors.danger)
                        : AppColors.muted.withValues(alpha: 0.4),
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                ),
                child: Text(
                  'Đánh dấu vắng',
                  style: TextStyle(
                    color: isAbsent
                        ? (isAbsentWithLeave
                              ? AppColors.blue
                              : AppColors.danger)
                        : AppColors.muted,
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
