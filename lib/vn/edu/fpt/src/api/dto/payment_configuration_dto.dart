import '../../models/payment_configuration.dart';
import '../exception/parse_exception.dart';

class PaymentConfigurationDto {
  const PaymentConfigurationDto({
    required this.id,
    required this.academicYearId,
    required this.bankCode,
    required this.bankName,
    required this.accountNumber,
    required this.accountHolder,
    required this.branch,
    required this.transferContentTemplate,
    required this.enabled,
    required this.method,
    required this.displayMode,
    required this.qrAvailable,
  });

  factory PaymentConfigurationDto.fromJson(Map<String, dynamic> json) {
    final id = json['id'];
    final academicYearId = json['academicYearId'];
    final bankCode = json['bankCode'];
    final bankName = json['bankName'];
    final accountNumber = json['accountNumber'];
    final accountHolder = json['accountHolder'];
    final branch = json['branch'];
    final transferContentTemplate = json['transferContentTemplate'];
    final enabled = json['enabled'];
    final method = json['method'];
    final displayMode = json['displayMode'];
    final qrAvailable = json['qrAvailable'];
    if (id is! num ||
        academicYearId is! num ||
        (bankCode != null && bankCode is! String) ||
        bankName is! String ||
        bankName.trim().isEmpty ||
        accountNumber is! String ||
        accountNumber.trim().isEmpty ||
        accountHolder is! String ||
        accountHolder.trim().isEmpty ||
        (branch != null && branch is! String) ||
        transferContentTemplate is! String ||
        transferContentTemplate.trim().isEmpty ||
        enabled is! bool ||
        method is! String ||
        displayMode is! String ||
        qrAvailable is! bool) {
      throw const ParseException('Cấu hình thanh toán thiếu dữ liệu bắt buộc.');
    }
    return PaymentConfigurationDto(
      id: id.toInt(),
      academicYearId: academicYearId.toInt(),
      bankCode: _optional(bankCode as String?),
      bankName: bankName.trim(),
      accountNumber: accountNumber.trim(),
      accountHolder: accountHolder.trim(),
      branch: _optional(branch as String?),
      transferContentTemplate: transferContentTemplate.trim(),
      enabled: enabled,
      method: method,
      displayMode: displayMode,
      qrAvailable: qrAvailable,
    );
  }

  final int id;
  final int academicYearId;
  final String? bankCode;
  final String bankName;
  final String accountNumber;
  final String accountHolder;
  final String? branch;
  final String transferContentTemplate;
  final bool enabled;
  final String method;
  final String displayMode;
  final bool qrAvailable;

  PaymentConfiguration toDomain() => PaymentConfiguration(
    id: id,
    academicYearId: academicYearId,
    bankCode: bankCode,
    bankName: bankName,
    accountNumber: accountNumber,
    accountHolder: accountHolder,
    branch: branch,
    transferContentTemplate: transferContentTemplate,
    enabled: enabled,
    method: method,
    displayMode: displayMode,
    qrAvailable: qrAvailable,
  );

  static String? _optional(String? value) {
    final normalized = value?.trim();
    return normalized == null || normalized.isEmpty ? null : normalized;
  }
}
