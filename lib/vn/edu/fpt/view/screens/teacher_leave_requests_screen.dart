import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherLeaveRequestsScreen extends StatefulWidget {
  const TeacherLeaveRequestsScreen({super.key});

  @override
  State<TeacherLeaveRequestsScreen> createState() => _TeacherLeaveRequestsScreenState();
}

class _TeacherLeaveRequestsScreenState extends State<TeacherLeaveRequestsScreen> {
  final List<_PendingLeaveRequest> _pendingRequests = [
    _PendingLeaveRequest(
      studentName: 'Trần Hoàng Nam',
      period: '16/06/2026 | Tiết 2-4',
      reason: 'Khám sức khỏe định kỳ, có giấy hẹn đính kèm',
    ),
    _PendingLeaveRequest(
      studentName: 'Lê Bảo Châu',
      period: '17/06/2026 | Cả ngày',
      reason: 'Gia đình có việc riêng đột xuất',
    ),
    _PendingLeaveRequest(
      studentName: 'Phạm Gia Huy',
      period: '18/06/2026 | Buổi sáng',
      reason: 'Sốt nhẹ, xin nghỉ theo dõi thêm',
    ),
  ];

  void _handleAction(int index, String action) {
    setState(() {
      _pendingRequests.removeAt(index);
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Đã $action đơn xin nghỉ học thành công!'),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Duyệt đơn xin nghỉ'),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          children: [
            const SectionHeader(title: 'Danh sách đơn chờ duyệt'),
            if (_pendingRequests.isEmpty)
              const AppCard(
                child: Center(
                  child: Text(
                    'Không còn đơn xin nghỉ nào chờ duyệt.',
                    style: TextStyle(color: AppColors.muted),
                  ),
                ),
              )
            else
              for (int i = 0; i < _pendingRequests.length; i++) ...[
                _TeacherLeaveTile(
                  request: _pendingRequests[i],
                  onApprove: () => _handleAction(i, 'duyệt'),
                  onReject: () => _handleAction(i, 'từ chối'),
                ),
                const SizedBox(height: AppSpacing.sm),
              ],
          ],
        ),
      ),
    );
  }
}

class _PendingLeaveRequest {
  const _PendingLeaveRequest({
    required this.studentName,
    required this.period,
    required this.reason,
  });

  final String studentName;
  final String period;
  final String reason;
}

class _TeacherLeaveTile extends StatelessWidget {
  const _TeacherLeaveTile({
    required this.request,
    required this.onApprove,
    required this.onReject,
  });

  final _PendingLeaveRequest request;
  final VoidCallback onApprove;
  final VoidCallback onReject;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                request.studentName,
                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w900, color: AppColors.ink),
              ),
              StatusPill(
                label: 'Pending',
                foreground: AppColors.warning,
                background: AppColors.warningSoft,
                compact: true,
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            request.period,
            style: const TextStyle(fontSize: 11, color: AppColors.muted, fontWeight: FontWeight.bold),
          ),
          const Divider(height: AppSpacing.lg),
          Text(
            'Lý do: ${request.reason}',
            style: const TextStyle(fontSize: 13, color: AppColors.ink, height: 1.3),
          ),
          const SizedBox(height: AppSpacing.lg),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: onReject,
                  icon: const Icon(Icons.close, size: 16),
                  label: const Text('Từ chối'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.danger,
                    side: const BorderSide(color: AppColors.danger),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                ),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: onApprove,
                  icon: const Icon(Icons.check, size: 16),
                  label: const Text('Phê duyệt'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.green,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
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
