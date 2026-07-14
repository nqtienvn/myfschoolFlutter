import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';

class TuitionPaymentScreen extends StatefulWidget {
  const TuitionPaymentScreen({
    super.key,
    required this.student,
    required this.token,
    this.viewAsStudent = false,
  });

  final StudentSnapshot student;
  final String token;
  final bool viewAsStudent;

  @override
  State<TuitionPaymentScreen> createState() => _TuitionPaymentScreenState();
}

class _TuitionPaymentScreenState extends State<TuitionPaymentScreen> {
  late final TuitionBillApiClient _apiClient;
  late final List<LeaveRequest> _tuitionRequests;
  List<TuitionBill> _bills = const [];
  String? _loadedPeriodKey;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _apiClient = TuitionBillApiClient(backend: BackendApiClient());
    _tuitionRequests = List<LeaveRequest>.of(widget.student.leaveRequests);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    final key = period == null ? 'current' : '${period.semesterId}';
    if (_loadedPeriodKey != key) {
      _loadedPeriodKey = key;
      if (period != null) _loadBills(period);
    }
  }

  Future<void> _loadBills(AcademicPeriod period) async {
    final requestKey = '${period.semesterId}';
    setState(() {
      _loading = true;
      _error = null;
      _bills = const [];
    });
    try {
      final bills = await _apiClient.getStudentBills(
        token: widget.token,
        semesterId: period.semesterId,
        studentId: widget.viewAsStudent ? null : widget.student.id,
      );
      if (mounted && _loadedPeriodKey == requestKey) {
        setState(() => _bills = bills);
      }
    } catch (error) {
      if (mounted && _loadedPeriodKey == requestKey) {
        setState(() => _error = error.toString().replaceAll('Exception: ', ''));
      }
    } finally {
      if (mounted && _loadedPeriodKey == requestKey) {
        setState(() => _loading = false);
      }
    }
  }

  // Compute unpaid total
  int get unpaidTotal {
    return _bills
        .where((bill) => bill.status == 'Chưa đóng')
        .fold(0, (sum, bill) => sum + bill.amount);
  }

  void _createTuitionRequest() {
    final formKey = GlobalKey<FormState>();
    String requestType = 'Đơn xin gia hạn đóng học phí';
    String reason = '';
    String additionalInfo = '';

    showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setStateDialog) {
            return AlertDialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              title: const Text(
                'Gửi yêu cầu học phí',
                style: TextStyle(
                  fontWeight: FontWeight.w900,
                  fontSize: 16,
                  color: AppColors.ink,
                ),
              ),
              content: Form(
                key: formKey,
                child: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Học sinh: ${widget.student.name}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.muted,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 12),

                      // Request Type
                      const Text(
                        'Loại yêu cầu học phí',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: AppColors.ink,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.background,
                          border: Border.all(color: AppColors.line),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: DropdownButton<String>(
                          value: requestType,
                          isExpanded: true,
                          underline: const SizedBox(),
                          items: const [
                            DropdownMenuItem(
                              value: 'Đơn xin gia hạn đóng học phí',
                              child: Text(
                                'Xin gia hạn đóng học phí',
                                style: TextStyle(fontSize: 13),
                              ),
                            ),
                            DropdownMenuItem(
                              value: 'Đơn xin miễn giảm học phí',
                              child: Text(
                                'Xin miễn giảm học phí',
                                style: TextStyle(fontSize: 13),
                              ),
                            ),
                            DropdownMenuItem(
                              value: 'Đơn báo cáo lỗi thanh toán',
                              child: Text(
                                'Báo cáo lỗi thanh toán',
                                style: TextStyle(fontSize: 13),
                              ),
                            ),
                          ],
                          onChanged: (val) {
                            if (val != null) {
                              setStateDialog(() {
                                requestType = val;
                              });
                            }
                          },
                        ),
                      ),
                      const SizedBox(height: 12),

                      // Dynamic Fields based on Request Type
                      if (requestType == 'Đơn xin gia hạn đóng học phí') ...[
                        const Text(
                          'Thời gian xin gia hạn',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: AppColors.ink,
                          ),
                        ),
                        const SizedBox(height: 6),
                        TextFormField(
                          initialValue: '15 ngày',
                          decoration: const InputDecoration(
                            hintText: 'Nhập số ngày hoặc ngày cụ thể...',
                            contentPadding: EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 10,
                            ),
                          ),
                          validator: (value) => value == null || value.isEmpty
                              ? 'Vui lòng điền thông tin'
                              : null,
                          onSaved: (value) => additionalInfo = value ?? '',
                        ),
                      ] else if (requestType ==
                          'Đơn xin miễn giảm học phí') ...[
                        const Text(
                          'Tỷ lệ miễn giảm đề xuất (%)',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: AppColors.ink,
                          ),
                        ),
                        const SizedBox(height: 6),
                        TextFormField(
                          initialValue: '30%',
                          decoration: const InputDecoration(
                            hintText: 'Nhập tỷ lệ phần trăm (ví dụ: 50%)...',
                            contentPadding: EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 10,
                            ),
                          ),
                          validator: (value) => value == null || value.isEmpty
                              ? 'Vui lòng điền thông tin'
                              : null,
                          onSaved: (value) => additionalInfo = value ?? '',
                        ),
                      ] else ...[
                        const Text(
                          'Mã giao dịch / Ngày chuyển',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: AppColors.ink,
                          ),
                        ),
                        const SizedBox(height: 6),
                        TextFormField(
                          decoration: const InputDecoration(
                            hintText: 'Ví dụ: FT260624001, Techcombank...',
                            contentPadding: EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 10,
                            ),
                          ),
                          validator: (value) => value == null || value.isEmpty
                              ? 'Vui lòng điền thông tin'
                              : null,
                          onSaved: (value) => additionalInfo = value ?? '',
                        ),
                      ],
                      const SizedBox(height: 12),

                      // Reason Field
                      const Text(
                        'Lý do / Mô tả chi tiết',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: AppColors.ink,
                        ),
                      ),
                      const SizedBox(height: 6),
                      TextFormField(
                        maxLines: 3,
                        decoration: const InputDecoration(
                          hintText:
                              'Nhập lý do chi tiết hoặc thông tin chuyển khoản...',
                          contentPadding: EdgeInsets.all(12),
                        ),
                        validator: (value) => value == null || value.isEmpty
                            ? 'Vui lòng nhập lý do'
                            : null,
                        onSaved: (value) => reason = value ?? '',
                      ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text(
                    'Hủy',
                    style: TextStyle(color: AppColors.muted),
                  ),
                ),
                ElevatedButton(
                  onPressed: () {
                    if (formKey.currentState?.validate() ?? false) {
                      formKey.currentState?.save();
                      Navigator.pop(context);

                      setState(() {
                        _tuitionRequests.insert(
                          0,
                          LeaveRequest(
                            title: requestType,
                            date: 'Vừa gửi',
                            reason: '$reason ($additionalInfo)',
                            status: 'Pending',
                            statusColor: AppColors.warning,
                            statusBackground: AppColors.warningSoft,
                            note:
                                'Đơn được gửi trực tiếp bởi Phụ huynh. Đang chờ đối soát.',
                          ),
                        );
                      });

                      showDialog<void>(
                        context: context,
                        builder: (context) => AlertDialog(
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(16),
                          ),
                          title: const Text(
                            'Đã gửi yêu cầu',
                            style: TextStyle(fontWeight: FontWeight.bold),
                          ),
                          content: const Text(
                            'Yêu cầu liên quan đến học phí đã được gửi đến Ban giám hiệu & Phòng tài vụ thành công. Vui lòng theo dõi tiến trình phê duyệt bên dưới.',
                          ),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(context),
                              child: const Text('Đóng'),
                            ),
                          ],
                        ),
                      );
                    }
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.fptOrange,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                  ),
                  child: const Text('Gửi yêu cầu'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  void _confirmPayment() {
    setState(() {
      for (final bill in _bills) {
        if (bill.status == 'Chưa đóng') {
          bill.status = 'Đang xử lý';
        }
      }
    });

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Đã ghi nhận yêu cầu chuyển khoản! Đang chờ xác thực.'),
        behavior: SnackBarBehavior.floating,
        backgroundColor: AppColors.blue,
      ),
    );
  }

  void _showQrBottomSheet() {
    if (unpaidTotal == 0) return;

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return Container(
          decoration: const BoxDecoration(
            color: AppColors.background,
            borderRadius: BorderRadius.only(
              topLeft: Radius.circular(24),
              topRight: Radius.circular(24),
            ),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Bottom sheet handle
              Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.line,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(height: 20),
              const Text(
                'Thanh toán chuyển khoản VietQR',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w900,
                  color: AppColors.ink,
                ),
              ),
              const SizedBox(height: 4),
              const Text(
                'Mở ứng dụng ngân hàng quét mã QR để thanh toán nhanh',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 12, color: AppColors.muted),
              ),
              const SizedBox(height: 24),

              // Mock VietQR Ticket
              Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                    color: AppColors.line.withValues(alpha: 0.8),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.05),
                      blurRadius: 10,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                padding: const EdgeInsets.all(20),
                child: Column(
                  children: [
                    // Header logo bank
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'VietQR',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w900,
                            color: Colors.indigo,
                            fontStyle: FontStyle.italic,
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: AppColors.primarySoft,
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: const Text(
                            'NAPAS247',
                            style: TextStyle(
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                              color: AppColors.fptOrange,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const Divider(height: 20),

                    // Mock QR Visual
                    Container(
                      width: 180,
                      height: 180,
                      decoration: BoxDecoration(
                        color: Colors.white,
                        border: Border.all(color: AppColors.line),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      padding: const EdgeInsets.all(12),
                      child: Stack(
                        alignment: Alignment.center,
                        children: [
                          // QR Pattern Mock: a stylized grid or decorative icon
                          Column(
                            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                            children: List.generate(
                              5,
                              (_) => Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceEvenly,
                                children: List.generate(
                                  5,
                                  (_) => Container(
                                    width: 14,
                                    height: 14,
                                    decoration: BoxDecoration(
                                      color: AppColors.ink.withValues(
                                        alpha: 0.8,
                                      ),
                                      borderRadius: BorderRadius.circular(2),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ),
                          // Bank Logo badge in center
                          Container(
                            padding: const EdgeInsets.all(6),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              shape: BoxShape.circle,
                              border: Border.all(color: AppColors.line),
                            ),
                            child: const Icon(
                              Icons.account_balance,
                              color: AppColors.fptOrange,
                              size: 20,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Amount & Info
                    Text(
                      '${unpaidTotal.toString().replaceAllMapped(RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'), (Match m) => '${m[1]}.')} đ',
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w900,
                        color: AppColors.fptOrange,
                      ),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      'Tên TK: CONG TY CO PHAN GIAO DUC FPT\nSố TK: 01913 2026 8888 (FPT Bank)',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 11,
                        color: AppColors.ink,
                        fontWeight: FontWeight.w600,
                        height: 1.4,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              // CTA buttons
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => Navigator.pop(context),
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      child: const Text('Hủy'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () {
                        Navigator.pop(context);
                        _confirmPayment();
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.fptOrange,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      child: const Text('Đã chuyển khoản'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Chi tiết Học phí'),
      body: SafeArea(
        child: Column(
          children: [
            // Bill List
            Expanded(
              child: RefreshIndicator(
                onRefresh: () =>
                    period == null ? Future.value() : _loadBills(period),
                child: ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 24,
                    vertical: 20,
                  ),
                  children: [
                    SectionHeader(
                      title: period == null
                          ? 'Các khoản học phí'
                          : 'Các khoản phí · ${period.label}',
                    ),
                    const SizedBox(height: 12),
                    if (_loading)
                      const Padding(
                        padding: EdgeInsets.all(32),
                        child: Center(child: CircularProgressIndicator()),
                      )
                    else if (_error != null)
                      AppCard(
                        child: Text(
                          _error!,
                          style: const TextStyle(color: AppColors.danger),
                        ),
                      )
                    else if (_bills.isEmpty)
                      const AppCard(
                        child: Text(
                          'Không có khoản học phí nào trong học kỳ này.',
                          style: TextStyle(color: AppColors.muted),
                        ),
                      ),
                    for (final bill in _bills) ...[
                      AppCard(
                        child: Row(
                          children: [
                            Container(
                              width: 44,
                              height: 44,
                              decoration: BoxDecoration(
                                color: bill.status == 'Đã đóng'
                                    ? AppColors.successSoft
                                    : (bill.status == 'Đang xử lý'
                                          ? AppColors.blueSoft
                                          : AppColors.dangerSoft),
                                shape: BoxShape.circle,
                              ),
                              child: Icon(
                                bill.status == 'Đã đóng'
                                    ? Icons.check_circle_outline
                                    : (bill.status == 'Đang xử lý'
                                          ? Icons.hourglass_empty
                                          : Icons.receipt_long_outlined),
                                color: bill.status == 'Đã đóng'
                                    ? AppColors.success
                                    : (bill.status == 'Đang xử lý'
                                          ? AppColors.blue
                                          : AppColors.danger),
                                size: 22,
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    bill.title,
                                    style: const TextStyle(
                                      fontSize: 14,
                                      fontWeight: FontWeight.bold,
                                      color: AppColors.ink,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    'Hạn nộp: ${bill.dueDate}',
                                    style: const TextStyle(
                                      fontSize: 11,
                                      color: AppColors.muted,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text(
                                  '${bill.amount.toString().replaceAllMapped(RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'), (Match m) => '${m[1]}.')} đ',
                                  style: const TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w800,
                                    color: AppColors.ink,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                StatusPill(
                                  label: bill.status,
                                  foreground: bill.status == 'Đã đóng'
                                      ? AppColors.success
                                      : (bill.status == 'Đang xử lý'
                                            ? AppColors.blue
                                            : AppColors.danger),
                                  background: bill.status == 'Đã đóng'
                                      ? AppColors.successSoft
                                      : (bill.status == 'Đang xử lý'
                                            ? AppColors.blueSoft
                                            : AppColors.dangerSoft),
                                  compact: true,
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: AppSpacing.sm),
                    ],

                    // Tuition Requests History Section
                    const SizedBox(height: 24),
                    const SectionHeader(
                      title: 'Đơn từ & Yêu cầu học phí đã gửi',
                    ),
                    const SizedBox(height: 12),
                    if (_tuitionRequests
                        .where(
                          (r) =>
                              r.title.toLowerCase().contains('học phí') ||
                              r.title.toLowerCase().contains('thanh toán'),
                        )
                        .isEmpty)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 16),
                        child: Center(
                          child: Text(
                            'Chưa gửi yêu cầu gia hạn hoặc miễn giảm nào.',
                            style: TextStyle(
                              fontSize: 12,
                              color: AppColors.muted,
                              fontStyle: FontStyle.italic,
                            ),
                          ),
                        ),
                      )
                    else
                      ..._tuitionRequests
                          .where(
                            (r) =>
                                r.title.toLowerCase().contains('học phí') ||
                                r.title.toLowerCase().contains('thanh toán'),
                          )
                          .map(
                            (req) => Padding(
                              padding: const EdgeInsets.only(bottom: 12),
                              child: AppCard(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      mainAxisAlignment:
                                          MainAxisAlignment.spaceBetween,
                                      children: [
                                        Expanded(
                                          child: Text(
                                            req.title,
                                            style: const TextStyle(
                                              fontSize: 13,
                                              fontWeight: FontWeight.bold,
                                              color: AppColors.ink,
                                            ),
                                          ),
                                        ),
                                        StatusPill(
                                          label: req.status == 'Pending'
                                              ? 'Chờ duyệt'
                                              : req.status,
                                          foreground: req.statusColor,
                                          background: req.statusBackground,
                                          compact: true,
                                        ),
                                      ],
                                    ),
                                    const SizedBox(height: 6),
                                    Text(
                                      req.reason,
                                      style: const TextStyle(
                                        fontSize: 12,
                                        color: AppColors.ink,
                                        height: 1.4,
                                      ),
                                    ),
                                    const SizedBox(height: 6),
                                    Row(
                                      mainAxisAlignment:
                                          MainAxisAlignment.spaceBetween,
                                      children: [
                                        Text(
                                          'Trạng thái: ${req.note}',
                                          style: const TextStyle(
                                            fontSize: 11,
                                            color: AppColors.muted,
                                            fontWeight: FontWeight.w500,
                                          ),
                                        ),
                                        Text(
                                          req.date,
                                          style: const TextStyle(
                                            fontSize: 11,
                                            color: AppColors.muted,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                  ],
                ),
              ),
            ),

            // Summary Bottom Panel
            Container(
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(20),
                  topRight: Radius.circular(20),
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.05),
                    blurRadius: 10,
                    offset: const Offset(0, -4),
                  ),
                ],
              ),
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
              child: Row(
                children: [
                  Expanded(
                    flex: 4,
                    child: Semantics(
                      label:
                          'Số tiền chưa thanh toán là ${unpaidTotal.toString().replaceAllMapped(RegExp(r"(\d{1,3})(?=(\d{3})+(?!\d))"), (Match m) => "${m[1]}.")} đồng.',
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Text(
                            'Chưa thanh toán',
                            style: TextStyle(
                              fontSize: 11,
                              color: AppColors.muted,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            '${unpaidTotal.toString().replaceAllMapped(RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'), (Match m) => '${m[1]}.')} đ',
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w900,
                              color: AppColors.fptOrange,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  // Nút Gửi đơn từ miễn giảm / gia hạn cho Phụ huynh
                  if (!widget.viewAsStudent)
                    Semantics(
                      button: true,
                      label:
                          'Gửi yêu cầu gia hạn đóng học phí hoặc xin miễn giảm.',
                      child: OutlinedButton(
                        onPressed: _createTuitionRequest,
                        style: OutlinedButton.styleFrom(
                          foregroundColor: AppColors.fptOrange,
                          side: const BorderSide(
                            color: AppColors.fptOrange,
                            width: 1.5,
                          ),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 14,
                            vertical: 14,
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        child: const Row(
                          children: [
                            Icon(Icons.edit_document, size: 16),
                            SizedBox(width: 4),
                            Text(
                              'Đơn từ',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  if (!widget.viewAsStudent) const SizedBox(width: 8),
                  Expanded(
                    flex: 6,
                    child: Semantics(
                      button: true,
                      enabled: unpaidTotal > 0,
                      label:
                          'Nhấn để thanh toán ngay số tiền chưa đóng qua cổng VietQR.',
                      child: ElevatedButton(
                        onPressed: unpaidTotal > 0 ? _showQrBottomSheet : null,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.fptOrange,
                          disabledBackgroundColor: AppColors.muted.withValues(
                            alpha: 0.2,
                          ),
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        child: const Text(
                          'Thanh toán',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
