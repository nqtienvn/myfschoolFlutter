import '../../../view/screens/student_models.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class TuitionBillApiClient {
  const TuitionBillApiClient({required BackendApiClient backend})
    : _backend = backend;

  final BackendApiClient _backend;

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
          final amount = item['amount'];
          return TuitionBill(
            title: item['name'] as String? ?? 'Khoản học phí',
            amount: amount is num ? amount.round() : 0,
            dueDate: _formatDate(item['dueDate'] as String?),
            status: _status(item['status'] as String?),
          );
        })
        .toList(growable: false);
  }

  static String _formatDate(String? value) {
    final date = DateTime.tryParse(value ?? '');
    if (date == null) return value ?? '';
    return '${date.day.toString().padLeft(2, '0')}/'
        '${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  static String _status(String? value) => switch (value) {
    'PAID' => 'Đã đóng',
    'PROCESSING' => 'Đang xử lý',
    _ => 'Chưa đóng',
  };
}
