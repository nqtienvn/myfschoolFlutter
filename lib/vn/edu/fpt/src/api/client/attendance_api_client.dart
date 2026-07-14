import 'package:flutter/material.dart';
import '../../../view/design_system/app_colors.dart';
import '../../../view/screens/student_models.dart';
import '../exception/parse_exception.dart';
import 'backend_api_client.dart';

class AttendanceApiClient {
  const AttendanceApiClient({required BackendApiClient backend})
    : _backend = backend;

  final BackendApiClient _backend;

  Future<Map<String, dynamic>> getHomeroomContext({
    required String token,
    String? date,
  }) async {
    final data = await _backend.getData(
      '/api/attendance/homeroom-context',
      token: token,
      query: {'date': date},
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu lớp chủ nhiệm không hợp lệ.');
    }
    return data;
  }

  Future<Map<String, dynamic>> getDailyAttendance({
    required String token,
    required int classId,
    required String date,
    required String shift,
  }) async {
    final data = await _backend.getData(
      '/api/attendance/daily',
      token: token,
      query: {'classId': classId.toString(), 'date': date, 'shift': shift},
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu điểm danh ngày không hợp lệ.');
    }
    return data;
  }

  Future<void> submitAttendance({
    required String token,
    required int classId,
    required String date,
    required String shift,
    required List<Map<String, dynamic>> entries,
  }) async {
    await _backend.postData(
      '/api/attendance/submit',
      token: token,
      body: {
        'classId': classId,
        'date': date,
        'shift': shift,
        'entries': entries,
      },
    );
  }

  Future<void> requestAttendanceCorrection({
    required String token,
    required int classId,
    required String date,
    required String shift,
    required List<Map<String, dynamic>> entries,
  }) async {
    await _backend.postData(
      '/api/attendance/corrections',
      token: token,
      body: {
        'classId': classId,
        'date': date,
        'shift': shift,
        'entries': entries,
      },
    );
  }

  Future<Map<String, dynamic>> getStudentAttendanceLog({
    required String token,
    int? studentId,
    int? semesterId,
  }) async {
    final data = await _backend.getData(
      '/api/attendance/student',
      token: token,
      query: {
        'studentId': studentId?.toString(),
        'semesterId': semesterId?.toString(),
      },
    );
    if (data is! Map<String, dynamic>) {
      throw const ParseException('Dữ liệu chuyên cần học sinh không hợp lệ.');
    }

    final rawItems = data['records'] as List? ?? [];
    final statsJson = data['stats'] as Map<String, dynamic>? ?? {};

    final events = rawItems.map((item) {
      final m = item as Map<String, dynamic>;
      final dateStr = m['date'] as String? ?? '';
      final shiftStr = m['shift'] as String? ?? '';
      final statusStr = m['status'] as String? ?? '';
      final teacherName = m['teacherName'] as String? ?? 'Hệ thống';

      // Format date (e.g. yyyy-MM-dd to MM/dd)
      String dateDisplay = dateStr;
      if (dateStr.length >= 10) {
        final parts = dateStr.split('-');
        if (parts.length >= 3) {
          dateDisplay = '${parts[2]}/${parts[1]}';
        }
      }

      final session = shiftStr == 'MORNING' ? 'Sáng' : 'Chiều';
      final statusDisplay = _mapStatusDisplay(statusStr);
      final color = _statusColor(statusStr);
      final background = _statusBg(statusStr);

      String reason = 'Người điểm danh: $teacherName';
      if (m['leaveRequestId'] != null) {
        reason = 'Vắng phép (Liên kết đơn xin nghỉ học)';
      }

      return AttendanceEvent(
        date: dateDisplay,
        session: session,
        status: statusDisplay,
        reason: reason,
        color: color,
        background: background,
      );
    }).toList();

    final rate = (statsJson['attendanceRate'] as num? ?? 0.0).toDouble();
    final presentCount = statsJson['presentSessions'] as int? ?? 0;
    final absentWithLeave = statsJson['absentWithLeave'] as int? ?? 0;
    final absentWithoutLeave = statsJson['absentWithoutLeave'] as int? ?? 0;
    final absentCount = absentWithLeave + absentWithoutLeave;

    return {
      'events': events,
      'attendanceRate': rate,
      'presentCount': presentCount,
      'absentCount': absentCount,
      'absentWithLeave': absentWithLeave,
      'absentWithoutLeave': absentWithoutLeave,
      'totalSessions': statsJson['totalSessions'] as int? ?? 0,
      'semesterName': statsJson['semesterName'] as String? ?? '',
    };
  }

  String _mapStatusDisplay(String status) {
    switch (status.toUpperCase()) {
      case 'PRESENT':
        return 'Có mặt';
      case 'ABSENT_WITH_LEAVE':
        return 'Vắng có phép';
      case 'ABSENT_WITHOUT_LEAVE':
      default:
        return 'Vắng không phép';
    }
  }

  Color _statusColor(String status) {
    switch (status.toUpperCase()) {
      case 'PRESENT':
        return AppColors.green;
      case 'ABSENT_WITH_LEAVE':
        return AppColors.blue;
      case 'ABSENT_WITHOUT_LEAVE':
      default:
        return AppColors.danger;
    }
  }

  Color _statusBg(String status) {
    switch (status.toUpperCase()) {
      case 'PRESENT':
        return AppColors.greenSoft;
      case 'ABSENT_WITH_LEAVE':
        return AppColors.blueSoft;
      case 'ABSENT_WITHOUT_LEAVE':
      default:
        return AppColors.dangerSoft;
    }
  }
}
