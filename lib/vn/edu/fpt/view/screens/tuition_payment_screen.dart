import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/payment_configuration.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

class TuitionPaymentScreen extends StatefulWidget {
  const TuitionPaymentScreen({
    super.key,
    required this.student,
    required this.studentId,
    required this.token,
    this.apiClient,
  });

  final StudentSnapshot student;
  final int studentId;
  final String token;
  final TuitionBillApiClient? apiClient;

  @override
  State<TuitionPaymentScreen> createState() => _TuitionPaymentScreenState();
}

class _TuitionPaymentScreenState extends State<TuitionPaymentScreen> {
  late final TuitionBillApiClient _apiClient;
  List<TuitionBill> _bills = const [];
  PaymentConfiguration? _paymentConfiguration;
  String? _loadedContextKey;
  bool _loading = true;
  bool _submitting = false;
  String? _error;
  int _loadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _apiClient =
        widget.apiClient ?? TuitionBillApiClient(backend: BackendApiClient());
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    final key = period == null
        ? 'none:${widget.student.id}'
        : '${period.semesterId}:${widget.student.id}';
    if (_loadedContextKey == key) return;
    _loadedContextKey = key;
    if (period != null) _loadBills(period);
  }

  Future<void> _loadBills(AcademicPeriod period) async {
    final requestKey = '${period.semesterId}:${widget.student.id}';
    final generation = ++_loadGeneration;
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
        _bills = const [];
        _paymentConfiguration = null;
      });
    }
    try {
      final results = await Future.wait<Object?>([
        _apiClient.getStudentBills(
          token: widget.token,
          semesterId: period.semesterId,
          studentId: widget.studentId,
        ),
        _apiClient.getPaymentConfiguration(
          token: widget.token,
          semesterId: period.semesterId,
        ),
      ]);
      if (_acceptResponse(requestKey, generation)) {
        setState(() {
          _bills = results[0]! as List<TuitionBill>;
          _paymentConfiguration = results[1] as PaymentConfiguration?;
        });
      }
    } catch (error) {
      if (_acceptResponse(requestKey, generation)) {
        setState(() => _error = _message(error));
      }
    } finally {
      if (_acceptResponse(requestKey, generation)) {
        setState(() => _loading = false);
      }
    }
  }

  bool _acceptResponse(String requestKey, int generation) {
    return mounted &&
        _loadedContextKey == requestKey &&
        _loadGeneration == generation;
  }

  List<TuitionBill> get _unpaidBills =>
      _bills.where((bill) => bill.status == 'Chưa đóng').toList();

  int get _unpaidTotal =>
      _unpaidBills.fold(0, (sum, bill) => sum + bill.amount);

  bool get _paymentReady =>
      _paymentConfiguration?.enabled == true &&
      _paymentConfiguration?.method == 'BANK_TRANSFER';

  String _transferContent(AcademicPeriod period) {
    return _paymentConfiguration?.renderTransferContent(
          studentCode: widget.student.studentCode,
          academicYear: period.academicYearName,
          semester: period.semesterName,
        ) ??
        '';
  }

  Future<void> _copyValue(String label, String value) async {
    await Clipboard.setData(ClipboardData(text: value));
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Đã sao chép $label.'),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  Future<void> _confirmBankTransfer(AcademicPeriod period) async {
    final unpaidBills = _unpaidBills;
    final configuration = _paymentConfiguration;
    if (unpaidBills.isEmpty ||
        _submitting ||
        !_paymentReady ||
        configuration == null) {
      return;
    }
    final transferContent = _transferContent(period);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          'Xác nhận đã chuyển khoản',
          style: TextStyle(fontWeight: FontWeight.w900),
        ),
        content: Text(
          'Ứng dụng chỉ gửi xác nhận để nhà trường đối soát, không tự chuyển tiền. '
          'Chỉ tiếp tục khi bạn đã chuyển ${_money(_unpaidTotal)} vào tài khoản '
          '${configuration.accountNumber} tại ${configuration.bankName}, với nội dung “$transferContent”.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Hủy'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.fptOrange,
              foregroundColor: Colors.white,
            ),
            child: const Text('Gửi xác nhận'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    setState(() => _submitting = true);
    try {
      for (final bill in unpaidBills) {
        final billId = bill.id;
        if (billId == null) {
          throw StateError('Khoản học phí thiếu mã định danh.');
        }
        await _apiClient.requestBankTransfer(
          token: widget.token,
          billId: billId,
        );
      }
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Đã ghi nhận trên hệ thống. Nhà trường sẽ đối soát giao dịch.',
            ),
            behavior: SnackBarBehavior.floating,
            backgroundColor: AppColors.blue,
          ),
        );
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(_message(error)),
            behavior: SnackBarBehavior.floating,
            backgroundColor: AppColors.danger,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
      await _loadBills(period);
    }
  }

  static String _message(Object error) =>
      error.toString().replaceFirst('Exception: ', '');

  static String _money(int amount) =>
      '${amount.toString().replaceAllMapped(RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'), (match) => '${match[1]}.')} đ';

  static Color _statusColor(String status) => switch (status) {
    'Đã đóng' => AppColors.success,
    'Đang xử lý' => AppColors.blue,
    _ => AppColors.danger,
  };

  static Color _statusBackground(String status) => switch (status) {
    'Đã đóng' => AppColors.successSoft,
    'Đang xử lý' => AppColors.blueSoft,
    _ => AppColors.dangerSoft,
  };

  static IconData _statusIcon(String status) => switch (status) {
    'Đã đóng' => Icons.check_circle_outline,
    'Đang xử lý' => Icons.hourglass_empty,
    _ => Icons.receipt_long_outlined,
  };

  @override
  Widget build(BuildContext context) {
    final period = AcademicPeriodScope.maybeOf(context)?.selected;
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const OrangeTopBar(title: 'Chi tiết học phí'),
      body: SafeArea(
        child: Column(
          children: [
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
                        child: Column(
                          children: [
                            Text(
                              _error!,
                              style: const TextStyle(color: AppColors.danger),
                            ),
                            const SizedBox(height: 8),
                            TextButton(
                              onPressed: period == null
                                  ? null
                                  : () => _loadBills(period),
                              child: const Text('Thử lại'),
                            ),
                          ],
                        ),
                      )
                    else if (_bills.isEmpty)
                      const AppCard(
                        child: Text(
                          'Không có khoản học phí nào trong học kỳ này.',
                          style: TextStyle(color: AppColors.muted),
                        ),
                      )
                    else
                      for (final bill in _bills) ...[
                        _BillCard(bill: bill),
                        const SizedBox(height: AppSpacing.sm),
                      ],
                    if (!_loading &&
                        _error == null &&
                        period != null &&
                        _unpaidTotal > 0) ...[
                      const SizedBox(height: 18),
                      const SectionHeader(title: 'Thông tin chuyển khoản'),
                      const SizedBox(height: 12),
                      if (_paymentReady && _paymentConfiguration != null)
                        _BankTransferCard(
                          configuration: _paymentConfiguration!,
                          transferContent: _transferContent(period),
                          onCopy: _copyValue,
                        )
                      else
                        const _PaymentUnavailableCard(),
                    ],
                    if (!_loading && _error == null) ...[
                      const SizedBox(height: 12),
                      const AppCard(
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(
                              Icons.info_outline,
                              size: 20,
                              color: AppColors.blue,
                            ),
                            SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                'Trạng thái “Đang xử lý” là xác nhận đã được lưu trên backend và đang chờ nhà trường đối soát.',
                                style: TextStyle(
                                  fontSize: 12,
                                  height: 1.4,
                                  color: AppColors.muted,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
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
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
              child: _TuitionPaymentFooter(
                unpaidTotal: _unpaidTotal,
                submitting: _submitting,
                paymentReady: _paymentReady,
                onConfirm:
                    period != null &&
                        _unpaidTotal > 0 &&
                        !_loading &&
                        !_submitting &&
                        _paymentReady
                    ? () => _confirmBankTransfer(period)
                    : null,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BankTransferCard extends StatelessWidget {
  const _BankTransferCard({
    required this.configuration,
    required this.transferContent,
    required this.onCopy,
  });

  final PaymentConfiguration configuration;
  final String transferContent;
  final Future<void> Function(String label, String value) onCopy;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            decoration: BoxDecoration(
              color: AppColors.blueSoft,
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Row(
              children: [
                Icon(Icons.account_balance_outlined, color: AppColors.blue),
                SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Chuyển khoản thủ công · Chưa sử dụng QR',
                    style: TextStyle(
                      color: AppColors.blue,
                      fontWeight: FontWeight.w800,
                      fontSize: 12,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          _BankDetailRow(label: 'Ngân hàng', value: configuration.bankName),
          if (configuration.branch != null)
            _BankDetailRow(label: 'Chi nhánh', value: configuration.branch!),
          _BankDetailRow(
            label: 'Số tài khoản',
            value: configuration.accountNumber,
            onCopy: () => onCopy('số tài khoản', configuration.accountNumber),
          ),
          _BankDetailRow(
            label: 'Chủ tài khoản',
            value: configuration.accountHolder,
          ),
          _BankDetailRow(
            label: 'Nội dung',
            value: transferContent,
            highlight: true,
            onCopy: () => onCopy('nội dung chuyển khoản', transferContent),
          ),
          const SizedBox(height: 6),
          const Text(
            'Vui lòng nhập đúng nội dung để nhà trường đối soát nhanh hơn.',
            style: TextStyle(fontSize: 11, height: 1.4, color: AppColors.muted),
          ),
        ],
      ),
    );
  }
}

class _BankDetailRow extends StatelessWidget {
  const _BankDetailRow({
    required this.label,
    required this.value,
    this.onCopy,
    this.highlight = false,
  });

  final String label;
  final String value;
  final VoidCallback? onCopy;
  final bool highlight;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 104,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 12,
                color: AppColors.muted,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          Expanded(
            child: SelectableText(
              value,
              style: TextStyle(
                fontSize: 13,
                color: highlight ? AppColors.fptOrange : AppColors.ink,
                fontWeight: highlight ? FontWeight.w900 : FontWeight.w700,
              ),
            ),
          ),
          if (onCopy != null) ...[
            const SizedBox(width: 6),
            InkWell(
              onTap: onCopy,
              borderRadius: BorderRadius.circular(8),
              child: const Padding(
                padding: EdgeInsets.all(6),
                child: Icon(
                  Icons.copy_outlined,
                  size: 18,
                  color: AppColors.blue,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _PaymentUnavailableCard extends StatelessWidget {
  const _PaymentUnavailableCard();

  @override
  Widget build(BuildContext context) {
    return const AppCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.warning_amber_rounded, color: AppColors.warning),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Nhà trường chưa cấu hình hoặc chưa kích hoạt tài khoản chuyển khoản cho năm học này. Vui lòng liên hệ nhà trường trước khi thanh toán.',
              style: TextStyle(
                fontSize: 12,
                height: 1.45,
                color: AppColors.warning,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _TuitionPaymentFooter extends StatelessWidget {
  const _TuitionPaymentFooter({
    required this.unpaidTotal,
    required this.submitting,
    required this.paymentReady,
    required this.onConfirm,
  });

  final int unpaidTotal;
  final bool submitting;
  final bool paymentReady;
  final VoidCallback? onConfirm;

  @override
  Widget build(BuildContext context) {
    final amount = Column(
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
          _TuitionPaymentScreenState._money(unpaidTotal),
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w900,
            color: AppColors.fptOrange,
          ),
        ),
      ],
    );
    final button = ElevatedButton.icon(
      onPressed: onConfirm,
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.fptOrange,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      icon: submitting
          ? const SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: Colors.white,
              ),
            )
          : const Icon(Icons.verified_outlined, size: 18),
      label: Text(
        submitting
            ? 'Đang gửi'
            : unpaidTotal > 0 && !paymentReady
            ? 'Chưa cấu hình'
            : 'Xác nhận đã chuyển',
        style: const TextStyle(fontWeight: FontWeight.bold),
      ),
    );
    return LayoutBuilder(
      builder: (context, constraints) {
        if (constraints.maxWidth < 340) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [amount, const SizedBox(height: 10), button],
          );
        }
        return Row(
          children: [
            Expanded(child: amount),
            const SizedBox(width: 12),
            button,
          ],
        );
      },
    );
  }
}

class _BillCard extends StatelessWidget {
  const _BillCard({required this.bill});

  final TuitionBill bill;

  @override
  Widget build(BuildContext context) {
    final statusColor = _TuitionPaymentScreenState._statusColor(bill.status);
    final icon = Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        color: _TuitionPaymentScreenState._statusBackground(bill.status),
        shape: BoxShape.circle,
      ),
      child: Icon(
        _TuitionPaymentScreenState._statusIcon(bill.status),
        color: statusColor,
        size: 22,
      ),
    );
    final details = Column(
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
          bill.status == 'Đang xử lý'
              ? 'Đang chờ đối soát · Hạn nộp: ${bill.dueDate}'
              : 'Hạn nộp: ${bill.dueDate}',
          style: const TextStyle(
            fontSize: 11,
            color: AppColors.muted,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
    final status = Column(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Text(
          _TuitionPaymentScreenState._money(bill.amount),
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w800,
            color: AppColors.ink,
          ),
        ),
        const SizedBox(height: 4),
        StatusPill(
          label: bill.status,
          foreground: statusColor,
          background: _TuitionPaymentScreenState._statusBackground(bill.status),
          compact: true,
        ),
      ],
    );
    return AppCard(
      child: LayoutBuilder(
        builder: (context, constraints) {
          if (constraints.maxWidth < 300) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    icon,
                    const SizedBox(width: 12),
                    Expanded(child: details),
                  ],
                ),
                const SizedBox(height: 12),
                Align(alignment: Alignment.centerRight, child: status),
              ],
            );
          }
          return Row(
            children: [
              icon,
              const SizedBox(width: 16),
              Expanded(child: details),
              const SizedBox(width: 8),
              status,
            ],
          );
        },
      ),
    );
  }
}
