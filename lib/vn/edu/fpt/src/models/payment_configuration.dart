class PaymentConfiguration {
  const PaymentConfiguration({
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

  String renderTransferContent({
    required String studentCode,
    required String academicYear,
    required String semester,
  }) {
    return transferContentTemplate
        .replaceAll('{studentCode}', studentCode)
        .replaceAll('{academicYear}', academicYear)
        .replaceAll('{semester}', semester)
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }
}
