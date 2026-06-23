import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/leave_request_create_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

class LeaveRequestListScreen extends StatefulWidget {
  const LeaveRequestListScreen({super.key, required this.student});

  final StudentSnapshot student;

  @override
  State<LeaveRequestListScreen> createState() => _LeaveRequestListScreenState();
}

class _LeaveRequestListScreenState extends State<LeaveRequestListScreen> {
  late List<LeaveRequest> _requests;

  @override
  void initState() {
    super.initState();
    _requests = List.from(widget.student.leaveRequests);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Đơn xin nghỉ học'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            PrimaryButton(
              label: 'Tạo đơn xin nghỉ',
              icon: Icons.add_circle_outline,
              onPressed: () async {
                final messenger = ScaffoldMessenger.of(context);
                final navigator = Navigator.of(context);
                final newRequest = await navigator.push<LeaveRequest?>(
                  MaterialPageRoute<LeaveRequest?>(
                    builder: (_) => LeaveRequestCreateScreen(student: widget.student),
                  ),
                );
                if (newRequest != null) {
                  setState(() {
                    _requests.insert(0, newRequest);
                  });
                  messenger.showSnackBar(
                    const SnackBar(
                      content: Text('Tạo đơn nghỉ học thành công!'),
                      behavior: SnackBarBehavior.floating,
                    ),
                  );
                }
              },
            ),
            const SizedBox(height: AppSpacing.lg),
            const SectionHeader(title: 'Lịch sử đơn xin nghỉ'),
            if (_requests.isEmpty)
              const AppCard(
                child: Center(
                  child: Text(
                    'Chưa có đơn xin nghỉ học nào.',
                    style: TextStyle(color: AppColors.muted),
                  ),
                ),
              )
            else
              for (final request in _requests) ...[
                _LeaveRequestTile(request: request),
                const SizedBox(height: AppSpacing.sm),
              ],
          ],
        ),
      ),
    );
  }
}

class _LeaveRequestTile extends StatelessWidget {
  const _LeaveRequestTile({required this.request});

  final LeaveRequest request;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  request.title,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w900,
                    color: AppColors.ink,
                  ),
                ),
              ),
              StatusPill(
                label: request.status,
                foreground: request.statusColor,
                background: request.statusBackground,
                compact: true,
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            request.date,
            style: const TextStyle(
              fontSize: 11,
              color: AppColors.muted,
              fontWeight: FontWeight.w600,
            ),
          ),
          const Divider(height: AppSpacing.lg),
          Text(
            'Lý do: ${request.reason}',
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.ink,
              fontWeight: FontWeight.w700,
            ),
          ),
          if (request.note.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.xs),
            Text(
              'Phản hồi: ${request.note}',
              style: const TextStyle(
                fontSize: 12,
                color: AppColors.muted,
                fontStyle: FontStyle.italic,
              ),
            ),
          ],
        ],
      ),
    );
  }
}
