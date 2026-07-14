import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/backend_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/leave_request_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/teacher_leave_requests_screen.dart';

class _RecordingLeaveRequestApiClient extends LeaveRequestApiClient {
  _RecordingLeaveRequestApiClient()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final List<String> calls = [];

  @override
  Future<List<Map<String, dynamic>>> getPendingLeaveRequests({
    required String token,
    int? academicYearId,
    int? semesterId,
  }) async {
    calls.add('pending:$academicYearId:$semesterId');
    return [];
  }

  @override
  Future<List<Map<String, dynamic>>> getReviewedLeaveRequests({
    required String token,
    int? academicYearId,
    int? semesterId,
  }) async {
    calls.add('reviewed:$academicYearId:$semesterId');
    return [];
  }
}

class _DelayedLeaveRequestApiClient extends LeaveRequestApiClient {
  _DelayedLeaveRequestApiClient()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  final pendingResponses = <Completer<List<Map<String, dynamic>>>>[];
  final reviewedResponses = <Completer<List<Map<String, dynamic>>>>[];

  @override
  Future<List<Map<String, dynamic>>> getPendingLeaveRequests({
    required String token,
    int? academicYearId,
    int? semesterId,
  }) {
    final response = Completer<List<Map<String, dynamic>>>();
    pendingResponses.add(response);
    return response.future;
  }

  @override
  Future<List<Map<String, dynamic>>> getReviewedLeaveRequests({
    required String token,
    int? academicYearId,
    int? semesterId,
  }) {
    final response = Completer<List<Map<String, dynamic>>>();
    reviewedResponses.add(response);
    return response.future;
  }
}

Map<String, dynamic> _pendingRequest(String studentName) => {
  'id': 1,
  'studentName': studentName,
  'dateFrom': '2026-09-10',
  'dateTo': '2026-09-10',
  'shift': 'FULL_DAY',
  'reason': 'Có việc gia đình',
  'status': 'PENDING',
};

void main() {
  testWidgets('reloads teacher leave data when selected period changes', (
    tester,
  ) async {
    final first = AcademicPeriod(
      academicYearId: 7,
      academicYearName: '2026-2027',
      semesterId: 71,
      semesterName: 'HK I',
      startDate: DateTime(2026, 9),
      endDate: DateTime(2027, 1, 15),
    );
    final second = AcademicPeriod(
      academicYearId: 7,
      academicYearName: '2026-2027',
      semesterId: 72,
      semesterName: 'HK II',
      startDate: DateTime(2027, 1, 16),
      endDate: DateTime(2027, 5, 31),
    );
    final periodController = AcademicPeriodController(token: 'token')
      ..periods = [first, second]
      ..selected = first
      ..isLoading = false;
    addTearDown(periodController.dispose);
    final client = _RecordingLeaveRequestApiClient();

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periodController,
          child: TeacherLeaveRequestsScreen(
            token: 'teacher-token',
            apiClient: client,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(client.calls, ['pending:7:71', 'reviewed:7:71']);

    periodController.select(second);
    await tester.pumpAndSettle();

    expect(client.calls, [
      'pending:7:71',
      'reviewed:7:71',
      'pending:7:72',
      'reviewed:7:72',
    ]);
  });

  testWidgets('period switch A-B-A ignores both stale leave responses', (
    tester,
  ) async {
    final first = AcademicPeriod(
      academicYearId: 7,
      academicYearName: '2026-2027',
      semesterId: 71,
      semesterName: 'HK I',
      startDate: DateTime(2026, 9),
      endDate: DateTime(2027, 1, 15),
    );
    final second = AcademicPeriod(
      academicYearId: 7,
      academicYearName: '2026-2027',
      semesterId: 72,
      semesterName: 'HK II',
      startDate: DateTime(2027, 1, 16),
      endDate: DateTime(2027, 5, 31),
    );
    final periodController = AcademicPeriodController(token: 'token')
      ..periods = [first, second]
      ..selected = first
      ..isLoading = false;
    addTearDown(periodController.dispose);
    final client = _DelayedLeaveRequestApiClient();

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periodController,
          child: TeacherLeaveRequestsScreen(
            token: 'teacher-token',
            apiClient: client,
          ),
        ),
      ),
    );
    await tester.pump();
    periodController.select(second);
    await tester.pump();
    periodController.select(first);
    await tester.pump();
    expect(client.pendingResponses, hasLength(3));

    client.pendingResponses[2].complete([_pendingRequest('Dữ liệu A mới')]);
    client.reviewedResponses[2].complete([]);
    await tester.pump();
    expect(find.text('Dữ liệu A mới'), findsOneWidget);

    client.pendingResponses[1].complete([_pendingRequest('Dữ liệu B cũ')]);
    client.reviewedResponses[1].complete([]);
    client.pendingResponses[0].complete([_pendingRequest('Dữ liệu A cũ')]);
    client.reviewedResponses[0].complete([]);
    await tester.pump();

    expect(find.text('Dữ liệu A mới'), findsOneWidget);
    expect(find.text('Dữ liệu B cũ'), findsNothing);
    expect(find.text('Dữ liệu A cũ'), findsNothing);
  });
}
