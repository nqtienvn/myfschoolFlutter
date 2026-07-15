import '../../../view/screens/student_models.dart';
import '../../models/payment_configuration.dart';
import '../dto/payment_configuration_dto.dart';
import '../exception/parse_exception.dart';
import '../dto/teacher_tuition_summary_dto.dart';
import 'backend_api_client.dart';

class TuitionBillApiClient {
  const TuitionBillApiClient({required BackendApiClient backend})
    : _backend = backend;

  final BackendApiClient _backend;

  Future<TeacherTuitionSummaryDto> getTeacherClassSummary({
    required String token,
    required int classId,
    required int semesterId,
  }) async {
    final data = await _backend.getData(
      '/api/tuition/bills/class-summary',
      token: token,
      query: {
        'classId': classId.toString(),
        'semesterId': semesterId.toString(),
      },
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu tổng hợp học phí không hợp lệ.');
    }
    return TeacherTuitionSummaryDto.fromJson(data);
  }

  Future<List<TuitionBill>> getStudentBills({
    required String token,
    required int semesterId,
    int? studentId,
  }) async {
    final data = await _backend.getData(
      '/api/tuition/bills/student',
      token: token,
      query: {
        'studentId': studentId?.toString(),
        'semesterId': semesterId.toString(),
      },
    );
    if (data is! List) {
      throw const ParseException('Dữ liệu học phí phải là dạng mảng.');
    }
    return data
        .map((item) {
          if (item is! Map<String, dynamic>) {
            throw const ParseException('Khoản học phí không đúng định dạng.');
          }
          final id = item['id'];
          final name = item['name'];
          final amount = item['amount'];
          final dueDate = item['dueDate'];
          final status = item['status'];
          if (id is! num ||
              name is! String ||
              name.trim().isEmpty ||
              amount is! num ||
              dueDate is! String ||
              DateTime.tryParse(dueDate) == null ||
              status is! String) {
            throw const ParseException('Khoản học phí thiếu dữ liệu bắt buộc.');
          }
          return TuitionBill(
            id: id.toInt(),
            title: name.trim(),
            amount: amount.round(),
            dueDate: _formatDate(dueDate),
            status: _status(status),
          );
        })
        .toList(growable: false);
  }

  Future<PaymentConfiguration?> getPaymentConfiguration({
    required String token,
    required int semesterId,
  }) async {
    final data = await _backend.getData(
      '/api/payment-configurations/semesters/$semesterId',
      token: token,
    );
    if (data == null) return null;
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Cấu hình thanh toán không hợp lệ.');
    }
    return PaymentConfigurationDto.fromJson(data).toDomain();
  }

  Future<void> requestBankTransfer({
    required String token,
    required int billId,
  }) async {
    await _backend.postData(
      '/api/tuition/bills/$billId/payment-request',
      token: token,
    );
  }

  static String _formatDate(String value) {
    final date = DateTime.parse(value);
    return '${date.day.toString().padLeft(2, '0')}/'
        '${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  static String _status(String value) => switch (value) {
    'PAID' => 'Đã đóng',
    'PROCESSING' => 'Đang xử lý',
    'UNPAID' => 'Chưa đóng',
    _ => throw ParseException('Trạng thái học phí không hợp lệ: $value.'),
  };
}
