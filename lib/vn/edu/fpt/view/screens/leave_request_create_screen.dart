import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

class LeaveRequestCreateScreen extends StatefulWidget {
  const LeaveRequestCreateScreen({super.key, required this.student});

  final StudentSnapshot student;

  @override
  State<LeaveRequestCreateScreen> createState() => _LeaveRequestCreateScreenState();
}

class _LeaveRequestCreateScreenState extends State<LeaveRequestCreateScreen> {
  final _reasonController = TextEditingController();
  String _selectedSession = 'Cả ngày';
  final _dateController = TextEditingController(text: '17/06/2026');
  bool _attachedEvidence = false;

  @override
  void dispose() {
    _reasonController.dispose();
    _dateController.dispose();
    super.dispose();
  }

  void _submitForm() {
    if (_reasonController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng nhập lý do nghỉ học.'),
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    final newRequest = LeaveRequest(
      title: 'Đơn xin nghỉ ${_dateController.text}',
      date: 'Gửi hôm nay',
      reason: _reasonController.text,
      status: 'Pending',
      statusColor: AppColors.warning,
      statusBackground: AppColors.warningSoft,
      note: 'Đang chờ cô chủ nhiệm phản hồi.',
    );

    Navigator.of(context).pop(newRequest);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Tạo đơn xin nghỉ'),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.lg),
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
                        CircleAvatar(
                          radius: 20,
                          backgroundColor: widget.student.avatarColor.withValues(alpha: 0.12),
                          child: Text(
                            widget.student.shortName,
                            style: TextStyle(
                              color: widget.student.avatarColor,
                              fontSize: 14,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                        ),
                        const SizedBox(width: AppSpacing.md),
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
                      decoration: InputDecoration(
                        hintText: 'Nhập ngày xin nghỉ (dd/mm/yyyy)',
                        prefixIcon: const Icon(Icons.calendar_today_outlined),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(AppRadius.md),
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
                      children: ['Cả ngày', 'Buổi sáng', 'Buổi chiều'].map((session) {
                        final isSelected = _selectedSession == session;
                        return Expanded(
                          child: Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 4),
                            child: ChoiceChip(
                              label: Text(session),
                              selected: isSelected,
                              onSelected: (_) {
                                setState(() => _selectedSession = session);
                              },
                              selectedColor: AppColors.primarySoft,
                              labelStyle: TextStyle(
                                color: isSelected ? AppColors.fptOrange : AppColors.ink,
                                fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600,
                                fontSize: 12,
                              ),
                            ),
                          ),
                        );
                      }).toList(),
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
                        hintText: 'Ví dụ: Con bị sốt cao, gia đình xin phép nghỉ...',
                        prefixIcon: const Icon(Icons.edit_note),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(AppRadius.md),
                        ),
                      ),
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    OutlinedButton.icon(
                      onPressed: () {
                        setState(() => _attachedEvidence = !_attachedEvidence);
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              _attachedEvidence
                                  ? 'Đã gắn minh chứng đơn nghỉ học!'
                                  : 'Đã hủy gắn minh chứng.',
                            ),
                            behavior: SnackBarBehavior.floating,
                          ),
                        );
                      },
                      icon: Icon(
                        _attachedEvidence ? Icons.check_circle : Icons.attach_file,
                        color: _attachedEvidence ? AppColors.green : AppColors.fptOrange,
                      ),
                      label: Text(
                        _attachedEvidence ? 'Đã đính kèm minh chứng' : 'Đính kèm minh chứng',
                        style: TextStyle(
                          color: _attachedEvidence ? AppColors.green : AppColors.fptOrange,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      style: OutlinedButton.styleFrom(
                        side: BorderSide(
                          color: _attachedEvidence ? AppColors.green : AppColors.fptOrange,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(AppRadius.md),
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
