import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

class LeaveRequestCreateScreen extends StatefulWidget {
  const LeaveRequestCreateScreen({
    super.key,
    required this.student,
    required this.token,
  });

  final StudentSnapshot student;
  final String token;

  @override
  State<LeaveRequestCreateScreen> createState() =>
      _LeaveRequestCreateScreenState();
}

class _LeaveRequestCreateScreenState extends State<LeaveRequestCreateScreen> {
  final _reasonController = TextEditingController();
  String _selectedSession = 'Cả ngày';
  late final TextEditingController _dateController;
  late DateTimeRange _selectedRange;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    final today = DateUtils.dateOnly(DateTime.now());
    _selectedRange = DateTimeRange(start: today, end: today);
    _dateController = TextEditingController(text: _formatRange(_selectedRange));
  }

  @override
  void dispose() {
    _reasonController.dispose();
    _dateController.dispose();
    super.dispose();
  }

  Future<void> _submitForm() async {
    final reason = _reasonController.text.trim();
    if (reason.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng nhập lý do nghỉ học.'),
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      String shift = 'FULL_DAY';
      if (_selectedSession == 'Buổi sáng') shift = 'MORNING';
      if (_selectedSession == 'Buổi chiều') shift = 'AFTERNOON';

      final apiClient = LeaveRequestApiClient(backend: BackendApiClient());
      final studentId = widget.student.id;
      if (studentId == null) {
        throw StateError('Không xác định được học sinh để gửi đơn.');
      }
      final createdRequest = await apiClient.createLeaveRequest(
        token: widget.token,
        studentId: studentId,
        dateFrom: _formatApiDate(_selectedRange.start),
        dateTo: _formatApiDate(_selectedRange.end),
        shift: shift,
        reason: reason,
      );

      if (mounted) {
        setState(() => _isLoading = false);
        Navigator.of(context).pop(createdRequest);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceAll('Exception: ', '')),
            behavior: SnackBarBehavior.floating,
            backgroundColor: AppColors.danger,
          ),
        );
      }
    }
  }

  Future<void> _pickDateRange() async {
    final picked = await showDateRangePicker(
      context: context,
      firstDate: DateUtils.dateOnly(DateTime.now()),
      lastDate: DateTime(DateTime.now().year + 2, 12, 31),
      initialDateRange: _selectedRange,
      helpText: 'Chọn thời gian xin nghỉ',
    );
    if (picked == null) return;
    setState(() {
      _selectedRange = picked;
      _dateController.text = _formatRange(picked);
    });
  }

  String _formatApiDate(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}';

  String _formatRange(DateTimeRange range) {
    String display(DateTime date) =>
        '${date.day.toString().padLeft(2, '0')}/'
        '${date.month.toString().padLeft(2, '0')}/${date.year}';
    return range.start == range.end
        ? display(range.start)
        : '${display(range.start)} - ${display(range.end)}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Tạo đơn xin nghỉ'),
      body: SafeArea(
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : SingleChildScrollView(
                padding: const EdgeInsetsDirectional.all(AppSpacing.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    AppCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Thông tin học sinh',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: AppColors.muted,
                            ),
                          ),
                          const SizedBox(height: AppSpacing.sm),
                          Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      widget.student.name,
                                      style: const TextStyle(
                                        fontSize: 14,
                                        fontWeight: FontWeight.w900,
                                        color: AppColors.ink,
                                      ),
                                    ),
                                    Text(
                                      'Lớp: ${widget.student.className} • GVCN: ${widget.student.homeroomTeacher}',
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
                        ],
                      ),
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    AppCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Ngày xin nghỉ',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: AppColors.muted,
                            ),
                          ),
                          const SizedBox(height: AppSpacing.sm),
                          TextField(
                            controller: _dateController,
                            readOnly: true,
                            onTap: _pickDateRange,
                            decoration: InputDecoration(
                              hintText: 'Chọn ngày hoặc khoảng ngày xin nghỉ',
                              prefixIcon: const Icon(
                                Icons.calendar_today_outlined,
                              ),
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(
                                  AppRadius.md,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: AppSpacing.lg),
                          const Text(
                            'Buổi nghỉ học',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: AppColors.muted,
                            ),
                          ),
                          const SizedBox(height: AppSpacing.sm),
                          Row(
                            children: ['Cả ngày', 'Buổi sáng', 'Buổi chiều']
                                .map((session) {
                                  final isSelected =
                                      _selectedSession == session;
                                  return Expanded(
                                    child: Padding(
                                      padding:
                                          const EdgeInsetsDirectional.symmetric(
                                            horizontal: 4,
                                          ),
                                      child: ChoiceChip(
                                        label: Text(session),
                                        selected: isSelected,
                                        onSelected: (_) {
                                          setState(
                                            () => _selectedSession = session,
                                          );
                                        },
                                        selectedColor: AppColors.primarySoft,
                                        labelStyle: TextStyle(
                                          color: isSelected
                                              ? AppColors.fptOrange
                                              : AppColors.ink,
                                          fontWeight: isSelected
                                              ? FontWeight.w800
                                              : FontWeight.w600,
                                          fontSize: 12,
                                        ),
                                      ),
                                    ),
                                  );
                                })
                                .toList(),
                          ),
                          const SizedBox(height: AppSpacing.lg),
                          const Text(
                            'Lý do xin nghỉ học',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: AppColors.muted,
                            ),
                          ),
                          const SizedBox(height: AppSpacing.sm),
                          TextField(
                            controller: _reasonController,
                            minLines: 3,
                            maxLines: 5,
                            decoration: InputDecoration(
                              hintText:
                                  'Ví dụ: Con bị sốt cao, gia đình xin phép nghỉ...',
                              prefixIcon: const Icon(Icons.edit_note),
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(
                                  AppRadius.md,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: AppSpacing.xl),
                          PrimaryButton(
                            label: 'Gửi đơn nghỉ học',
                            icon: Icons.send_outlined,
                            onPressed: _submitForm,
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
}
