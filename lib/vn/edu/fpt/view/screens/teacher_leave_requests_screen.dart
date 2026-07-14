import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class TeacherLeaveRequestsScreen extends StatefulWidget {
  const TeacherLeaveRequestsScreen({
    super.key,
    required this.token,
    this.academicYearId,
    this.semesterId,
    this.notificationService,
    this.apiClient,
  });

  final String token;
  final int? academicYearId;
  final int? semesterId;
  final NotificationService? notificationService;
  final LeaveRequestApiClient? apiClient;

  @override
  State<TeacherLeaveRequestsScreen> createState() =>
      _TeacherLeaveRequestsScreenState();
}

class _TeacherLeaveRequestsScreenState
    extends State<TeacherLeaveRequestsScreen> {
  late final LeaveRequestApiClient _apiClient;
  bool _isLoading = true;
  String? _errorMessage;
  List<Map<String, dynamic>> _pendingRequests = [];
  List<Map<String, dynamic>> _reviewedRequests = [];
  int? _lastLeaveNotificationId;
  int? _academicYearId;
  int? _semesterId;
  bool _didResolvePeriod = false;
  int _loadGeneration = 0;
  int _mutationGeneration = 0;

  @override
  void initState() {
    super.initState();
    _apiClient =
        widget.apiClient ?? LeaveRequestApiClient(backend: BackendApiClient());
    widget.notificationService?.addListener(_onNotificationChanged);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final periodController = AcademicPeriodScope.maybeOf(context);
    if (widget.academicYearId == null &&
        widget.semesterId == null &&
        periodController?.isLoading == true) {
      return;
    }

    final academicYearId =
        widget.academicYearId ?? periodController?.selected?.academicYearId;
    final semesterId =
        widget.semesterId ?? periodController?.selected?.semesterId;
    if (_didResolvePeriod &&
        _academicYearId == academicYearId &&
        _semesterId == semesterId) {
      return;
    }

    _didResolvePeriod = true;
    _loadGeneration++;
    _mutationGeneration++;
    _academicYearId = academicYearId;
    _semesterId = semesterId;
    _loadData();
  }

  @override
  void dispose() {
    widget.notificationService?.removeListener(_onNotificationChanged);
    super.dispose();
  }

  void _onNotificationChanged() {
    final leaveNotifications = widget.notificationService?.notifications
        .where((item) => item.relatedType == 'LEAVE_REQUEST')
        .toList(growable: false);
    if (leaveNotifications == null || leaveNotifications.isEmpty) return;
    final newestId = leaveNotifications.first.id;
    if (_lastLeaveNotificationId == newestId) return;
    _lastLeaveNotificationId = newestId;
    _loadData();
  }

  Future<void> _loadData() async {
    final academicYearId = _academicYearId;
    final semesterId = _semesterId;
    final generation = ++_loadGeneration;
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final results = await Future.wait([
        _apiClient.getPendingLeaveRequests(
          token: widget.token,
          academicYearId: academicYearId,
          semesterId: semesterId,
        ),
        _apiClient.getReviewedLeaveRequests(
          token: widget.token,
          academicYearId: academicYearId,
          semesterId: semesterId,
        ),
      ]);
      if (!_isCurrentLoad(generation, academicYearId, semesterId)) return;
      setState(() {
        _pendingRequests = results[0];
        _reviewedRequests = results[1];
        _isLoading = false;
      });
    } catch (e) {
      if (!_isCurrentLoad(generation, academicYearId, semesterId)) return;
      setState(() {
        _errorMessage = e.toString().replaceAll('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  bool _isCurrentLoad(int generation, int? academicYearId, int? semesterId) =>
      mounted &&
      generation == _loadGeneration &&
      academicYearId == _academicYearId &&
      semesterId == _semesterId;

  bool _isCurrentMutation(
    int generation,
    int? academicYearId,
    int? semesterId,
  ) =>
      mounted &&
      generation == _mutationGeneration &&
      academicYearId == _academicYearId &&
      semesterId == _semesterId;

  Future<void> _approve(int id) async {
    final dialogAcademicYearId = _academicYearId;
    final dialogSemesterId = _semesterId;
    final dialogGeneration = _mutationGeneration;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Duyệt đơn xin nghỉ?'),
        content: const Text(
          'Đơn sẽ được chuyển sang trạng thái đã duyệt và phụ huynh, học sinh sẽ nhận thông báo.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Duyệt'),
          ),
        ],
      ),
    );
    if (confirmed != true ||
        !_isCurrentMutation(
          dialogGeneration,
          dialogAcademicYearId,
          dialogSemesterId,
        )) {
      return;
    }
    final academicYearId = _academicYearId;
    final semesterId = _semesterId;
    final generation = ++_mutationGeneration;
    _loadGeneration++;
    setState(() => _isLoading = true);
    try {
      await _apiClient.approveRequest(token: widget.token, id: id);
      if (!_isCurrentMutation(generation, academicYearId, semesterId)) return;
      await _loadData();
      if (!mounted ||
          !_isCurrentMutation(generation, academicYearId, semesterId)) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã phê duyệt đơn xin nghỉ thành công!'),
          behavior: SnackBarBehavior.floating,
        ),
      );
    } catch (e) {
      if (_isCurrentMutation(generation, academicYearId, semesterId)) {
        setState(() {
          _errorMessage = e.toString().replaceAll('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  void _showDetail(Map<String, dynamic> request, {required bool canReview}) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => SafeArea(
        child: Padding(
          padding: EdgeInsets.only(
            left: AppSpacing.lg,
            right: AppSpacing.lg,
            top: AppSpacing.lg,
            bottom:
                MediaQuery.viewInsetsOf(sheetContext).bottom + AppSpacing.lg,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                request['studentName'] as String? ?? 'Học sinh',
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(height: AppSpacing.sm),
              Text(
                'Lớp ${request['className'] ?? ''} • ${request['dateFrom']} → ${request['dateTo']}',
              ),
              const Divider(height: AppSpacing.xl),
              const Text(
                'Lý do',
                style: TextStyle(fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(request['reason'] as String? ?? ''),
              if (!canReview) ...[
                const SizedBox(height: AppSpacing.lg),
                Text(
                  'Kết quả: ${_statusLabel(request['status'] as String?)}',
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
                if ((request['approvedByName'] as String?)?.isNotEmpty == true)
                  Text('Giáo viên xử lý: ${request['approvedByName']}'),
                if ((request['response'] as String?)?.isNotEmpty == true)
                  Text('Phản hồi: ${request['response']}'),
              ],
              if (canReview) ...[
                const SizedBox(height: AppSpacing.xl),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () {
                          Navigator.pop(sheetContext);
                          _reject(request['id'] as int);
                        },
                        child: const Text('Từ chối'),
                      ),
                    ),
                    const SizedBox(width: AppSpacing.md),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () {
                          Navigator.pop(sheetContext);
                          _approve(request['id'] as int);
                        },
                        child: const Text('Phê duyệt'),
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _reject(int id) async {
    final reasonController = TextEditingController();
    final dialogAcademicYearId = _academicYearId;
    final dialogSemesterId = _semesterId;
    final dialogGeneration = _mutationGeneration;
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Từ chối đơn xin nghỉ học'),
        content: TextField(
          controller: reasonController,
          decoration: const InputDecoration(
            hintText: 'Nhập lý do từ chối...',
            border: OutlineInputBorder(),
          ),
          maxLines: 3,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.danger,
              foregroundColor: Colors.white,
            ),
            child: const Text('Từ chối'),
          ),
        ],
      ),
    );

    final reason = reasonController.text.trim();
    reasonController.dispose();
    if (confirm != true ||
        !_isCurrentMutation(
          dialogGeneration,
          dialogAcademicYearId,
          dialogSemesterId,
        )) {
      return;
    }

    final academicYearId = _academicYearId;
    final semesterId = _semesterId;
    final generation = ++_mutationGeneration;
    _loadGeneration++;
    setState(() => _isLoading = true);
    try {
      await _apiClient.rejectRequest(
        token: widget.token,
        id: id,
        response: reason,
      );
      if (!_isCurrentMutation(generation, academicYearId, semesterId)) return;
      await _loadData();
      if (!mounted ||
          !_isCurrentMutation(generation, academicYearId, semesterId)) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã từ chối đơn xin nghỉ học!'),
          behavior: SnackBarBehavior.floating,
        ),
      );
    } catch (e) {
      if (_isCurrentMutation(generation, academicYearId, semesterId)) {
        setState(() {
          _errorMessage = e.toString().replaceAll('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: AppColors.background,
        appBar: const OrangeTopBar(title: 'Duyệt đơn xin nghỉ'),
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
                          onPressed: _loadData,
                          child: const Text('Thử lại'),
                        ),
                      ],
                    ),
                  ),
                )
              : Column(
                  children: [
                    Material(
                      color: Colors.white,
                      child: TabBar(
                        labelColor: AppColors.primary,
                        unselectedLabelColor: AppColors.muted,
                        indicatorColor: AppColors.primary,
                        tabs: [
                          Tab(text: 'Chờ duyệt (${_pendingRequests.length})'),
                          Tab(text: 'Đã duyệt (${_reviewedRequests.length})'),
                        ],
                      ),
                    ),
                    Expanded(
                      child: TabBarView(
                        children: [
                          _buildRequestList(_pendingRequests, canReview: true),
                          _buildRequestList(
                            _reviewedRequests,
                            canReview: false,
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

  Widget _buildRequestList(
    List<Map<String, dynamic>> requests, {
    required bool canReview,
  }) {
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          if (requests.isEmpty)
            AppCard(
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.lg),
                child: Center(
                  child: Text(
                    canReview
                        ? 'Không còn đơn xin nghỉ nào chờ duyệt.'
                        : 'Chưa có đơn nào được xử lý trong năm học đang hoạt động.',
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: AppColors.muted),
                  ),
                ),
              ),
            )
          else
            for (final request in requests) ...[
              _TeacherLeaveTile(
                request: request,
                onTap: () => _showDetail(request, canReview: canReview),
                onApprove: canReview
                    ? () => _approve(request['id'] as int)
                    : null,
                onReject: canReview
                    ? () => _reject(request['id'] as int)
                    : null,
              ),
              const SizedBox(height: AppSpacing.sm),
            ],
        ],
      ),
    );
  }

  String _statusLabel(String? status) =>
      status == 'APPROVED' ? 'Đã phê duyệt' : 'Đã từ chối';
}

class _TeacherLeaveTile extends StatelessWidget {
  const _TeacherLeaveTile({
    required this.request,
    required this.onApprove,
    required this.onReject,
    required this.onTap,
  });

  final Map<String, dynamic> request;
  final VoidCallback? onApprove;
  final VoidCallback? onReject;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final studentName = request['studentName'] as String? ?? 'Học sinh';
    final dateFrom = request['dateFrom'] as String? ?? '';
    final dateTo = request['dateTo'] as String? ?? '';
    final shift = request['shift'] as String? ?? 'FULL_DAY';
    final reason = request['reason'] as String? ?? '';
    final status = request['status'] as String? ?? 'PENDING';

    String periodDisplay = 'Từ $dateFrom đến $dateTo';
    if (dateFrom == dateTo) {
      periodDisplay = dateFrom;
    }
    String shiftLabel = 'Cả ngày';
    if (shift == 'MORNING') shiftLabel = 'Buổi sáng';
    if (shift == 'AFTERNOON') shiftLabel = 'Buổi chiều';

    return GestureDetector(
      onTap: onTap,
      child: AppCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  studentName,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w900,
                    color: AppColors.ink,
                  ),
                ),
                StatusPill(
                  label: status == 'PENDING'
                      ? 'Chờ duyệt'
                      : (status == 'APPROVED' ? 'Đã duyệt' : 'Đã từ chối'),
                  foreground: status == 'PENDING'
                      ? AppColors.warning
                      : (status == 'APPROVED'
                            ? AppColors.success
                            : AppColors.danger),
                  background: status == 'PENDING'
                      ? AppColors.warningSoft
                      : (status == 'APPROVED'
                            ? AppColors.successSoft
                            : AppColors.dangerSoft),
                  compact: true,
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              '$periodDisplay • $shiftLabel',
              style: const TextStyle(
                fontSize: 11,
                color: AppColors.muted,
                fontWeight: FontWeight.bold,
              ),
            ),
            const Divider(height: AppSpacing.lg),
            Text(
              'Lý do: $reason',
              style: const TextStyle(
                fontSize: 13,
                color: AppColors.ink,
                height: 1.3,
              ),
            ),
            if (onApprove != null && onReject != null) ...[
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
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
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
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}
