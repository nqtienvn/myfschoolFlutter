import 'package:flutter/material.dart';
import '../../../view/design_system/app_colors.dart';
import '../../../view/screens/student_models.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class LeaveRequestApiClient {
  const LeaveRequestApiClient({required BackendApiClient backend})
    : _backend = backend;

  final BackendApiClient _backend;

  Future<LeaveRequest> createLeaveRequest({
    required String token,
    required int studentId,
    required String dateFrom,
    required String dateTo,
    required String shift,
    required String reason,
  }) async {
    final data = await _backend.postData(
      '/api/leave-requests',
      token: token,
      body: {
        'studentId': studentId,
        'dateFrom': dateFrom,
        'dateTo': dateTo,
        'shift': shift,
        'reason': reason,
      },
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu đơn xin nghỉ không hợp lệ.');
    }
    return _parseLeaveRequest(data);
  }

  Future<List<LeaveRequest>> getMyLeaveRequests({
    required String token,
    int? studentId,
  }) async {
    final data = await _backend.getData(
      '/api/leave-requests/my',
      token: token,
      query: {'studentId': studentId?.toString()},
    );
    if (data is! List) {
      throw const ParseException(
        'Dữ liệu danh sách đơn xin nghỉ phải là dạng mảng.',
      );
    }
    return data.map((item) {
      if (item is! Map<String, dynamic>) {
        throw const ParseException(
          'Thông tin đơn xin nghỉ không đúng định dạng.',
        );
      }
      return _parseLeaveRequest(item);
    }).toList();
  }

  Future<List<Map<String, dynamic>>> getPendingLeaveRequests({
    required String token,
  }) => _getTeacherRequests(token: token, path: '/api/leave-requests/pending');

  Future<List<Map<String, dynamic>>> getReviewedLeaveRequests({
    required String token,
  }) => _getTeacherRequests(token: token, path: '/api/leave-requests/reviewed');

  Future<List<Map<String, dynamic>>> _getTeacherRequests({
    required String token,
    required String path,
  }) async {
    final data = await _backend.getData(path, token: token);
    if (data is! List) {
      throw const ParseException('Dữ liệu đơn xin nghỉ phải là dạng mảng.');
    }
    return data.map((item) {
      if (item is! Map<String, dynamic>) {
        throw const ParseException('Đơn xin nghỉ không hợp lệ.');
      }
      return item;
    }).toList();
  }

  Future<void> approveRequest({required String token, required int id}) async {
    await _backend.putData('/api/leave-requests/$id/approve', token: token);
  }

  Future<int> getPendingCount({required String token}) async {
    final data = await _backend.getData(
      '/api/leave-requests/pending-count',
      token: token,
    );
    if (data is! int) {
      throw const ParseException('Số đơn chờ duyệt không hợp lệ.');
    }
    return data;
  }

  Future<void> cancelRequest({required String token, required int id}) async {
    await _backend.deleteData('/api/leave-requests/$id', token: token);
  }

  Future<void> rejectRequest({
    required String token,
    required int id,
    required String response,
  }) async {
    await _backend.putData(
      '/api/leave-requests/$id/reject',
      token: token,
      body: {'response': response},
    );
  }

  LeaveRequest _parseLeaveRequest(Map<String, dynamic> json) {
    final id = json['id'] as int? ?? 0;
    final dateFrom = json['dateFrom'] as String? ?? '';
    final dateTo = json['dateTo'] as String? ?? '';
    final status = json['status'] as String? ?? 'PENDING';
    final reason = json['reason'] as String? ?? '';
    final response = json['response'] as String? ?? '';
    final studentName = json['studentName'] as String? ?? '';

    final dateDisplay =
        'Nghỉ: $dateFrom${dateFrom != dateTo ? ' đến $dateTo' : ''}';
    final title = 'Đơn nghỉ học của $studentName';

    return LeaveRequest(
      id: id,
      studentId: json['studentId'] as int?,
      title: title,
      date: dateDisplay,
      reason: reason,
      status: status == 'APPROVED'
          ? 'Approved'
          : (status == 'REJECTED' ? 'Rejected' : 'Pending'),
      statusColor: _statusColor(status),
      statusBackground: _statusBg(status),
      note: response.isNotEmpty
          ? response
          : (status == 'PENDING' ? 'Đang chờ cô chủ nhiệm phản hồi.' : ''),
    );
  }

  Color _statusColor(String status) {
    switch (status.toUpperCase()) {
      case 'APPROVED':
        return AppColors.success;
      case 'REJECTED':
        return AppColors.danger;
      case 'PENDING':
      default:
        return AppColors.warning;
    }
  }

  Color _statusBg(String status) {
    switch (status.toUpperCase()) {
      case 'APPROVED':
        return AppColors.successSoft;
      case 'REJECTED':
        return AppColors.dangerSoft;
      case 'PENDING':
      default:
        return AppColors.warningSoft;
    }
  }
}
