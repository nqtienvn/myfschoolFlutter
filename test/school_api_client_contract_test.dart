import 'dart:collection';

import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/attendance_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/backend_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/gradebook_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/leave_request_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/exception/parse_exception.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/student_models.dart';

class _BackendCall {
  const _BackendCall({
    required this.method,
    required this.path,
    this.token,
    this.query = const {},
    this.body,
  });

  final String method;
  final String path;
  final String? token;
  final Map<String, String?> query;
  final Object? body;
}

class _RecordingBackendApiClient extends BackendApiClient {
  _RecordingBackendApiClient({List<Object?> responses = const []})
    : _responses = Queue<Object?>.of(responses),
      super(baseUrl: 'http://localhost');

  final Queue<Object?> _responses;
  final List<_BackendCall> calls = [];

  Object? _nextResponse() =>
      _responses.isEmpty ? null : _responses.removeFirst();

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    calls.add(
      _BackendCall(method: 'GET', path: path, token: token, query: query),
    );
    return _nextResponse();
  }

  @override
  Future<Object?> postData(String path, {String? token, Object? body}) async {
    calls.add(
      _BackendCall(method: 'POST', path: path, token: token, body: body),
    );
    return _nextResponse();
  }

  @override
  Future<Object?> putData(String path, {String? token, Object? body}) async {
    calls.add(
      _BackendCall(method: 'PUT', path: path, token: token, body: body),
    );
    return _nextResponse();
  }

  @override
  Future<Object?> deleteData(String path, {String? token, Object? body}) async {
    calls.add(
      _BackendCall(method: 'DELETE', path: path, token: token, body: body),
    );
    return _nextResponse();
  }
}

void main() {
  group('GradebookApiClient', () {
    test('scopes teacher assignments to the selected academic year', () async {
      final backend = _RecordingBackendApiClient(
        responses: [
          [
            <String, dynamic>{'id': 1},
            'invalid item',
            <String, dynamic>{'id': 2},
          ],
        ],
      );
      final client = GradebookApiClient(backend: backend);

      final assignments = await client.getMyAssignments(
        token: 'teacher-token',
        academicYearId: 2026,
      );

      expect(assignments.map((item) => item['id']), [1, 2]);
      expect(backend.calls.single.method, 'GET');
      expect(backend.calls.single.path, '/api/teaching-assignments/mine');
      expect(backend.calls.single.token, 'teacher-token');
      expect(backend.calls.single.query, {'academicYearId': '2026'});
    });

    test(
      'uses the correct transcript resource for self and linked students',
      () async {
        final backend = _RecordingBackendApiClient(
          responses: [
            <String, dynamic>{'studentId': 5},
            <String, dynamic>{'studentId': 9},
          ],
        );
        final client = GradebookApiClient(backend: backend);

        await client.getTranscript(
          token: 'token',
          academicYearId: 2026,
          semesterId: 2,
        );
        await client.getTranscript(
          token: 'token',
          academicYearId: 2026,
          semesterId: 2,
          studentId: 9,
        );

        expect(backend.calls[0].path, '/api/transcripts/me');
        expect(backend.calls[1].path, '/api/transcripts/students/9');
        for (final call in backend.calls) {
          expect(call.query, {'academicYearId': '2026', 'semesterId': '2'});
        }
      },
    );

    test(
      'sends the grade item and audit reason when updating scores',
      () async {
        final backend = _RecordingBackendApiClient();
        final client = GradebookApiClient(backend: backend);
        final entries = [
          <String, dynamic>{'studentId': 4, 'score': 8.5},
        ];

        await client.updateScores(
          token: 'teacher-token',
          gradeItemId: 12,
          entries: entries,
        );

        final call = backend.calls.single;
        expect(call.method, 'PUT');
        expect(call.path, '/api/grade-books/scores');
        expect(call.body, {
          'gradeItemId': 12,
          'reason': isNotEmpty,
          'entries': entries,
        });
      },
    );
  });

  group('AttendanceApiClient', () {
    test(
      'maps attendance events, status colors, and aggregate counts',
      () async {
        final backend = _RecordingBackendApiClient(
          responses: [
            <String, dynamic>{
              'records': [
                <String, dynamic>{
                  'date': '2026-09-03',
                  'shift': 'MORNING',
                  'status': 'PRESENT',
                  'teacherName': 'GV A',
                },
                <String, dynamic>{
                  'date': '2026-09-04',
                  'shift': 'AFTERNOON',
                  'status': 'ABSENT_WITH_LEAVE',
                  'teacherName': 'GV B',
                  'leaveRequestId': 17,
                },
                <String, dynamic>{
                  'date': '2026-09-05',
                  'shift': 'MORNING',
                  'status': 'ABSENT_WITHOUT_LEAVE',
                },
              ],
              'stats': <String, dynamic>{
                'attendanceRate': 75,
                'presentSessions': 1,
                'absentWithLeave': 1,
                'absentWithoutLeave': 1,
                'totalSessions': 3,
                'semesterName': 'Học kỳ 1',
              },
            },
          ],
        );
        final client = AttendanceApiClient(backend: backend);

        final result = await client.getStudentAttendanceLog(
          token: 'student-token',
          studentId: 7,
          semesterId: 2,
        );

        final events = result['events'] as List<AttendanceEvent>;
        expect(events, hasLength(3));
        expect(events[0].date, '03/09');
        expect(events[0].color, AppColors.green);
        expect(events[1].color, AppColors.blue);
        expect(events[1].reason, contains('Vắng phép'));
        expect(events[2].color, AppColors.danger);
        expect(result, containsPair('attendanceRate', 75.0));
        expect(result, containsPair('presentCount', 1));
        expect(result, containsPair('absentCount', 2));
        expect(backend.calls.single.query, {
          'studentId': '7',
          'semesterId': '2',
        });
      },
    );

    test('rejects a malformed student attendance response', () async {
      final backend = _RecordingBackendApiClient(responses: [<dynamic>[]]);
      final client = AttendanceApiClient(backend: backend);

      await expectLater(
        client.getStudentAttendanceLog(token: 'token'),
        throwsA(isA<ParseException>()),
      );
    });

    test('submits the complete attendance session payload', () async {
      final backend = _RecordingBackendApiClient();
      final client = AttendanceApiClient(backend: backend);
      final entries = [
        <String, dynamic>{'studentId': 7, 'status': 'PRESENT'},
      ];

      await client.submitAttendance(
        token: 'teacher-token',
        classId: 3,
        date: '2026-09-03',
        shift: 'MORNING',
        entries: entries,
      );

      final call = backend.calls.single;
      expect(call.method, 'POST');
      expect(call.path, '/api/attendance/submit');
      expect(call.body, {
        'classId': 3,
        'date': '2026-09-03',
        'shift': 'MORNING',
        'entries': entries,
      });
    });

    test('submits a correction reason and maps year-scoped history', () async {
      final history = <String, dynamic>{
        'id': 9,
        'classId': 3,
        'className': '12A1',
        'teacherName': 'Nguyễn Thu Hà',
        'date': '2026-09-03',
        'shift': 'MORNING',
        'status': 'APPROVED',
        'originalPresentCount': 1,
        'originalAbsentWithLeaveCount': 0,
        'originalAbsentWithoutLeaveCount': 0,
        'presentCount': 0,
        'absentWithLeaveCount': 1,
        'absentWithoutLeaveCount': 0,
        'reason': 'Ghi nhầm trạng thái',
        'changes': [
          {
            'studentId': 7,
            'studentName': 'Nguyễn An',
            'studentCode': 'HS007',
            'oldStatus': 'PRESENT',
            'newStatus': 'ABSENT_WITH_LEAVE',
          },
        ],
        'createdAt': '2026-09-03T08:00:00',
        'reviewedByName': 'Admin FPT Schools',
        'reviewedAt': '2026-09-03T09:00:00',
      };
      final backend = _RecordingBackendApiClient(
        responses: [
          null,
          [history],
        ],
      );
      final client = AttendanceApiClient(backend: backend);
      final entries = [
        <String, dynamic>{'studentId': 7, 'status': 'ABSENT_WITH_LEAVE'},
      ];

      await client.requestAttendanceCorrection(
        token: 'teacher-token',
        classId: 3,
        date: '2026-09-03',
        shift: 'MORNING',
        entries: entries,
        reason: 'Ghi nhầm trạng thái',
      );
      final corrections = await client.getCorrectionHistory(
        token: 'teacher-token',
        academicYearId: 26,
      );

      expect(backend.calls.first.body, {
        'classId': 3,
        'date': '2026-09-03',
        'shift': 'MORNING',
        'entries': entries,
        'reason': 'Ghi nhầm trạng thái',
      });
      expect(backend.calls.last.query, {'academicYearId': '26'});
      expect(corrections.single.reason, 'Ghi nhầm trạng thái');
      expect(corrections.single.changes.single.oldStatus, 'PRESENT');
      expect(corrections.single.reviewedByName, 'Admin FPT Schools');
    });
  });

  group('LeaveRequestApiClient', () {
    test('creates and maps an approved multi-day leave request', () async {
      final backend = _RecordingBackendApiClient(
        responses: [
          <String, dynamic>{
            'id': 8,
            'studentId': 7,
            'studentName': 'Nguyễn An',
            'dateFrom': '2026-09-03',
            'dateTo': '2026-09-04',
            'status': 'APPROVED',
            'reason': 'Ốm',
            'response': 'Đã duyệt',
          },
        ],
      );
      final client = LeaveRequestApiClient(backend: backend);

      final request = await client.createLeaveRequest(
        token: 'parent-token',
        studentId: 7,
        dateFrom: '2026-09-03',
        dateTo: '2026-09-04',
        shift: 'FULL_DAY',
        reason: 'Ốm',
      );

      expect(request.id, 8);
      expect(request.studentId, 7);
      expect(request.status, 'Approved');
      expect(request.title, contains('Nguyễn An'));
      expect(request.date, contains('2026-09-04'));
      expect(request.note, 'Đã duyệt');
      expect(backend.calls.single.body, {
        'studentId': 7,
        'dateFrom': '2026-09-03',
        'dateTo': '2026-09-04',
        'shift': 'FULL_DAY',
        'reason': 'Ốm',
      });
    });

    test(
      'maps pending and rejected requests from the selected semester',
      () async {
        final backend = _RecordingBackendApiClient(
          responses: [
            [
              <String, dynamic>{
                'id': 1,
                'studentName': 'An',
                'dateFrom': '2026-09-03',
                'dateTo': '2026-09-03',
                'status': 'PENDING',
                'reason': 'Ốm',
              },
              <String, dynamic>{
                'id': 2,
                'studentName': 'Bình',
                'dateFrom': '2026-09-05',
                'dateTo': '2026-09-05',
                'status': 'REJECTED',
                'reason': 'Bận',
                'response': 'Không hợp lệ',
              },
            ],
          ],
        );
        final client = LeaveRequestApiClient(backend: backend);

        final requests = await client.getMyLeaveRequests(
          token: 'parent-token',
          studentId: 7,
          semesterId: 2,
        );

        expect(requests.map((request) => request.status), [
          'Pending',
          'Rejected',
        ]);
        expect(requests[0].note, isNotEmpty);
        expect(requests[1].note, 'Không hợp lệ');
        expect(backend.calls.single.query, {
          'studentId': '7',
          'semesterId': '2',
        });
      },
    );

    test('rejects malformed teacher request list items', () async {
      final backend = _RecordingBackendApiClient(
        responses: [
          ['not-a-request'],
        ],
      );
      final client = LeaveRequestApiClient(backend: backend);

      await expectLater(
        client.getPendingLeaveRequests(token: 'teacher-token'),
        throwsA(isA<ParseException>()),
      );
    });

    test('scopes teacher leave endpoints to the selected period', () async {
      final backend = _RecordingBackendApiClient(
        responses: [<Map<String, dynamic>>[], <Map<String, dynamic>>[], 4],
      );
      final client = LeaveRequestApiClient(backend: backend);

      await client.getPendingLeaveRequests(
        token: 'teacher-token',
        academicYearId: 7,
        semesterId: 71,
      );
      await client.getReviewedLeaveRequests(
        token: 'teacher-token',
        academicYearId: 7,
        semesterId: 71,
      );
      expect(
        await client.getPendingCount(
          token: 'teacher-token',
          academicYearId: 7,
          semesterId: 71,
        ),
        4,
      );

      expect(backend.calls.map((call) => call.path), [
        '/api/leave-requests/pending',
        '/api/leave-requests/reviewed',
        '/api/leave-requests/pending-count',
      ]);
      for (final call in backend.calls) {
        expect(call.query, {'academicYearId': '7', 'semesterId': '71'});
      }
    });

    test(
      'uses the expected endpoints for teacher review and parent cancellation',
      () async {
        final backend = _RecordingBackendApiClient(
          responses: [4, null, null, null],
        );
        final client = LeaveRequestApiClient(backend: backend);

        expect(await client.getPendingCount(token: 'teacher-token'), 4);
        await client.approveRequest(token: 'teacher-token', id: 10);
        await client.rejectRequest(
          token: 'teacher-token',
          id: 11,
          response: 'Thiếu minh chứng',
        );
        await client.cancelRequest(token: 'parent-token', id: 12);

        expect(backend.calls.map((call) => '${call.method} ${call.path}'), [
          'GET /api/leave-requests/pending-count',
          'PUT /api/leave-requests/10/approve',
          'PUT /api/leave-requests/11/reject',
          'DELETE /api/leave-requests/12',
        ]);
        expect(backend.calls[2].body, {'response': 'Thiếu minh chứng'});
      },
    );
  });
}
