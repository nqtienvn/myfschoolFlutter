import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/leave_request_create_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';

class LeaveRequestListScreen extends StatefulWidget {
  const LeaveRequestListScreen({
    super.key,
    required this.student,
    required this.token,
  });

  final StudentSnapshot student;
  final String token;

  @override
  State<LeaveRequestListScreen> createState() => _LeaveRequestListScreenState();
}

class _LeaveRequestListScreenState extends State<LeaveRequestListScreen> {
  late final LeaveRequestApiClient _apiClient;
  List<LeaveRequest> _requests = [];
  bool _isLoading = true;
  String? _errorMessage;
  int? _loadedSemesterId;
  bool _periodInitialized = false;

  @override
  void initState() {
    super.initState();
    _apiClient = LeaveRequestApiClient(backend: BackendApiClient());
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final semesterId = AcademicPeriodScope.maybeOf(
      context,
    )?.selected?.semesterId;
    if (!_periodInitialized || _loadedSemesterId != semesterId) {
      _periodInitialized = true;
      _loadedSemesterId = semesterId;
      _loadData();
    }
  }

  Future<void> _loadData() async {
    final requestSemesterId = _loadedSemesterId;
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final studentId = widget.student.id;
      if (studentId == null) {
        throw StateError('Không xác định được học sinh cần xem đơn.');
      }
      final studentRequests = await _apiClient.getMyLeaveRequests(
        token: widget.token,
        studentId: studentId,
        semesterId: requestSemesterId,
      );

      if (!mounted || _loadedSemesterId != requestSemesterId) return;
      setState(() {
        _requests = studentRequests;
        _isLoading = false;
      });
    } catch (e) {
      if (mounted && _loadedSemesterId == requestSemesterId) {
        setState(() {
          _errorMessage = e.toString().replaceAll('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _cancel(LeaveRequest request) async {
    if (request.id == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Hủy đơn xin nghỉ?'),
        content: const Text('Bạn chỉ có thể hủy đơn khi giáo viên chưa xử lý.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Không'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Hủy đơn'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await _apiClient.cancelRequest(token: widget.token, id: request.id!);
      await _loadData();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString().replaceAll('Exception: ', ''))),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final selectedPeriod = AcademicPeriodScope.maybeOf(context)?.selected;
    final canCreate = selectedPeriod?.isCurrent == true;
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Đơn xin nghỉ học'),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _loadData,
          child: ListView(
            padding: const EdgeInsetsDirectional.all(AppSpacing.lg),
            physics: const AlwaysScrollableScrollPhysics(),
            children: [
              PrimaryButton(
                label: canCreate
                    ? 'Tạo đơn xin nghỉ'
                    : 'Chỉ tạo đơn trong học kỳ đang hoạt động',
                icon: Icons.add_circle_outline,
                onPressed: !canCreate
                    ? null
                    : () async {
                        final messenger = ScaffoldMessenger.of(context);
                        final navigator = Navigator.of(context);
                        final newRequest = await navigator.push<LeaveRequest?>(
                          MaterialPageRoute<LeaveRequest?>(
                            builder: (_) => LeaveRequestCreateScreen(
                              student: widget.student,
                              token: widget.token,
                            ),
                          ),
                        );
                        if (newRequest != null) {
                          await _loadData();
                          if (!mounted) return;
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
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const SectionHeader(title: 'Lịch sử đơn xin nghỉ'),
                  TextButton.icon(
                    onPressed: _loadData,
                    icon: const Icon(Icons.refresh, size: 16),
                    label: const Text('Tải lại'),
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.xs),
              if (_isLoading)
                const Center(
                  child: Padding(
                    padding: EdgeInsetsDirectional.all(AppSpacing.lg),
                    child: CircularProgressIndicator(),
                  ),
                )
              else if (_errorMessage != null)
                Center(
                  child: Padding(
                    padding: const EdgeInsetsDirectional.all(AppSpacing.lg),
                    child: Text(
                      _errorMessage!,
                      style: const TextStyle(color: AppColors.danger),
                    ),
                  ),
                )
              else if (_requests.isEmpty)
                const AppCard(
                  child: Center(
                    child: Padding(
                      padding: EdgeInsetsDirectional.all(AppSpacing.lg),
                      child: Text(
                        'Chưa có đơn xin nghỉ học nào.',
                        style: TextStyle(color: AppColors.muted),
                      ),
                    ),
                  ),
                )
              else
                for (final request in _requests) ...[
                  _LeaveRequestTile(
                    request: request,
                    onCancel: request.status == 'Pending'
                        ? () => _cancel(request)
                        : null,
                  ),
                  const SizedBox(height: AppSpacing.sm),
                ],
            ],
          ),
        ),
      ),
    );
  }
}

class _LeaveRequestTile extends StatelessWidget {
  const _LeaveRequestTile({required this.request, this.onCancel});

  final LeaveRequest request;
  final VoidCallback? onCancel;

  @override
  Widget build(BuildContext context) {
    String displayStatus = request.status;
    if (request.status == 'Approved') displayStatus = 'Đã duyệt';
    if (request.status == 'Rejected') displayStatus = 'Từ chối';
    if (request.status == 'Pending') displayStatus = 'Chờ duyệt';

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
                label: displayStatus,
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
          if (onCancel != null) ...[
            const SizedBox(height: AppSpacing.sm),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: onCancel,
                icon: const Icon(Icons.cancel_outlined, size: 16),
                label: const Text('Hủy đơn'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
