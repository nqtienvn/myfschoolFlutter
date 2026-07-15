import '../../models/homeroom_monitoring.dart';
import 'backend_api_client.dart';

abstract class HomeroomMonitoringApi {
  Future<List<StudentRiskFlag>> getRisks({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
    String? status,
  });
  Future<List<StudentRiskFlag>> recalculateRisks({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
  });
  Future<StudentRiskFlag> updateRiskStatus({
    required String token,
    required int riskId,
    required String action,
  });
  Future<List<HomeroomClassSummary>> getClassSummaries({
    required String token,
    required int academicYearId,
    required int semesterId,
    int? classId,
  });
  Future<List<ParentContactLog>> getContactLogs({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
  });
  Future<ParentContactLog> saveContactLog({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required String contactType,
    required String subject,
    required String summary,
    required String result,
    required DateTime contactedAt,
    int? logId,
  });
  Future<void> deleteContactLog({required String token, required int logId});
  Future<List<ParentMeeting>> getMeetings({
    required String token,
    required int academicYearId,
    required int semesterId,
    int? classId,
  });
  Future<ParentMeeting> saveMeeting({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required String title,
    required DateTime startsAt,
    required String location,
    required String agenda,
    int? studentId,
    int? meetingId,
    String status,
  });
  Future<ParentMeeting> respondMeeting({
    required String token,
    required int meetingId,
    required String response,
  });
  Future<ParentMeeting> markMeetingAttendance({
    required String token,
    required int meetingId,
    required int guardianId,
    required String attendance,
  });
  Future<List<StudentEvent>> getStudentEvents({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    int? classId,
  });
  Future<StudentEvent> saveStudentEvent({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required String eventType,
    required String category,
    required String title,
    required String description,
    required DateTime eventDate,
    int? eventId,
  });
  Future<StudentEvent> publishStudentEvent({
    required String token,
    required int eventId,
  });
}

class HomeroomMonitoringApiClient implements HomeroomMonitoringApi {
  HomeroomMonitoringApiClient({BackendApiClient? backend})
    : _backend = backend ?? BackendApiClient();

  final BackendApiClient _backend;

  Map<String, String?> _scope(int yearId, int semesterId, [int? classId]) => {
    'academicYearId': '$yearId',
    'semesterId': '$semesterId',
    'classId': classId?.toString(),
  };

  @override
  Future<List<StudentRiskFlag>> getRisks({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
    String? status,
  }) async {
    final data = await _backend.getData(
      '/api/homeroom/risks',
      token: token,
      query: {..._scope(academicYearId, semesterId, classId), 'status': status},
    );
    return _list(data).map(StudentRiskFlag.fromJson).toList(growable: false);
  }

  @override
  Future<List<StudentRiskFlag>> recalculateRisks({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
  }) async {
    final data = await _backend.postDataWithQuery(
      '/api/homeroom/risks/recalculate',
      token: token,
      query: _scope(academicYearId, semesterId, classId),
    );
    return _list(data).map(StudentRiskFlag.fromJson).toList(growable: false);
  }

  @override
  Future<StudentRiskFlag> updateRiskStatus({
    required String token,
    required int riskId,
    required String action,
  }) async {
    final data = await _backend.putData(
      '/api/homeroom/risks/$riskId/$action',
      token: token,
    );
    return StudentRiskFlag.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<List<HomeroomClassSummary>> getClassSummaries({
    required String token,
    required int academicYearId,
    required int semesterId,
    int? classId,
  }) async {
    final data = await _backend.getData(
      '/api/homeroom/reports/class-summary',
      token: token,
      query: _scope(academicYearId, semesterId, classId),
    );
    return _list(
      data,
    ).map(HomeroomClassSummary.fromJson).toList(growable: false);
  }

  @override
  Future<List<ParentContactLog>> getContactLogs({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
  }) async {
    final data = await _backend.getData(
      '/api/students/$studentId/contact-logs',
      token: token,
      query: _scope(academicYearId, semesterId, classId),
    );
    return _list(data).map(ParentContactLog.fromJson).toList(growable: false);
  }

  @override
  Future<ParentContactLog> saveContactLog({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required String contactType,
    required String subject,
    required String summary,
    required String result,
    required DateTime contactedAt,
    int? logId,
  }) async {
    final body = {
      'academicYearId': academicYearId,
      'semesterId': semesterId,
      'classId': classId,
      'contactType': contactType,
      'subject': subject,
      'summary': summary,
      'result': result,
      'contactedAt': contactedAt.toIso8601String(),
    };
    final data = logId == null
        ? await _backend.postData(
            '/api/students/$studentId/contact-logs',
            token: token,
            body: body,
          )
        : await _backend.putData(
            '/api/contact-logs/$logId',
            token: token,
            body: body,
          );
    return ParentContactLog.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<void> deleteContactLog({
    required String token,
    required int logId,
  }) async {
    await _backend.deleteData('/api/contact-logs/$logId', token: token);
  }

  @override
  Future<List<ParentMeeting>> getMeetings({
    required String token,
    required int academicYearId,
    required int semesterId,
    int? classId,
  }) async {
    final data = await _backend.getData(
      '/api/parent-meetings',
      token: token,
      query: _scope(academicYearId, semesterId, classId),
    );
    return _list(data).map(ParentMeeting.fromJson).toList(growable: false);
  }

  @override
  Future<ParentMeeting> saveMeeting({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required String title,
    required DateTime startsAt,
    required String location,
    required String agenda,
    int? studentId,
    int? meetingId,
    String status = 'SCHEDULED',
  }) async {
    final body = {
      'title': title,
      'academicYearId': academicYearId,
      'semesterId': semesterId,
      'classId': classId,
      'studentId': studentId,
      'startsAt': startsAt.toIso8601String(),
      'location': location,
      'agenda': agenda,
      'status': status,
    };
    final data = meetingId == null
        ? await _backend.postData(
            '/api/parent-meetings',
            token: token,
            body: body,
          )
        : await _backend.putData(
            '/api/parent-meetings/$meetingId',
            token: token,
            body: body,
          );
    return ParentMeeting.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<ParentMeeting> respondMeeting({
    required String token,
    required int meetingId,
    required String response,
  }) async {
    final data = await _backend.putData(
      '/api/parent-meetings/$meetingId/respond',
      token: token,
      body: {'response': response},
    );
    return ParentMeeting.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<ParentMeeting> markMeetingAttendance({
    required String token,
    required int meetingId,
    required int guardianId,
    required String attendance,
  }) async {
    final data = await _backend.putData(
      '/api/parent-meetings/$meetingId/attendance',
      token: token,
      body: {'guardianId': guardianId, 'attendance': attendance},
    );
    return ParentMeeting.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<List<StudentEvent>> getStudentEvents({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    int? classId,
  }) async {
    final data = await _backend.getData(
      '/api/students/$studentId/events',
      token: token,
      query: _scope(academicYearId, semesterId, classId),
    );
    return _list(data).map(StudentEvent.fromJson).toList(growable: false);
  }

  @override
  Future<StudentEvent> saveStudentEvent({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required String eventType,
    required String category,
    required String title,
    required String description,
    required DateTime eventDate,
    int? eventId,
  }) async {
    final body = {
      'academicYearId': academicYearId,
      'semesterId': semesterId,
      'classId': classId,
      'eventType': eventType,
      'category': category,
      'title': title,
      'description': description,
      'eventDate': _date(eventDate),
    };
    final data = eventId == null
        ? await _backend.postData(
            '/api/students/$studentId/events',
            token: token,
            body: body,
          )
        : await _backend.putData(
            '/api/student-events/$eventId',
            token: token,
            body: body,
          );
    return StudentEvent.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<StudentEvent> publishStudentEvent({
    required String token,
    required int eventId,
  }) async {
    final data = await _backend.postData(
      '/api/student-events/$eventId/publish',
      token: token,
    );
    return StudentEvent.fromJson(data as Map<String, dynamic>);
  }

  Iterable<Map<String, dynamic>> _list(Object? data) =>
      (data as List<dynamic>? ?? const []).whereType<Map<String, dynamic>>();

  String _date(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}';
}
