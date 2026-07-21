import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/api.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/notification_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/grades_screen.dart';

class _TranscriptApi extends GradebookApiClient {
  _TranscriptApi()
    : super(backend: BackendApiClient(baseUrl: 'http://localhost'));

  double score = 8.5;
  int transcriptCalls = 0;

  @override
  Future<Map<String, dynamic>> getTranscript({
    required String token,
    required int academicYearId,
    required int semesterId,
    int? studentId,
  }) async {
    transcriptCalls++;
    return {
      'studentName': 'Nguyễn An',
      'subjects': [
        {
          'subjectName': 'Kỹ năng sống',
          'average': score,
          'complete': true,
          'scores': [
            {
              'code': 'DA_1',
              'name': 'Dự án cá nhân',
              'weight': 1,
              'assessmentType': 'SCORE',
              'score': score,
              'comment': null,
              'isGraded': true,
            },
            {
              'code': 'NL_1',
              'name': 'Năng lực',
              'weight': 1,
              'assessmentType': 'PASS_FAIL',
              'score': null,
              'comment': 'PASS',
              'isGraded': true,
            },
            {
              'code': 'PX_1',
              'name': 'Phản hồi',
              'weight': 1,
              'assessmentType': 'COMMENT',
              'score': null,
              'comment': 'Chủ động và hợp tác tốt',
              'isGraded': true,
            },
          ],
        },
        {
          'subjectName': 'Vật lý',
          'average': null,
          'complete': false,
          'scores': [
            {
              'code': 'DA_1',
              'name': 'Dự án cá nhân',
              'assessmentType': 'SCORE',
              'score': null,
              'comment': null,
              'isGraded': false,
            },
            {
              'code': 'NL_1',
              'name': 'Năng lực',
              'assessmentType': 'PASS_FAIL',
              'score': null,
              'comment': null,
              'isGraded': false,
            },
            {
              'code': 'PX_1',
              'name': 'Phản hồi',
              'assessmentType': 'COMMENT',
              'score': null,
              'comment': null,
              'isGraded': false,
            },
          ],
        },
      ],
    };
  }
}

class _EmptyNotificationBackend extends BackendApiClient {
  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async => <Map<String, dynamic>>[];
}

AcademicPeriodController _periodController() {
  final period = AcademicPeriod(
    academicYearId: 26,
    academicYearName: '2026-2027',
    semesterId: 2,
    semesterName: 'Học kỳ II',
    startDate: DateTime(2027, 1, 1),
    endDate: DateTime(2027, 5, 31),
  );
  return AcademicPeriodController(token: 'token')
    ..periods = [period]
    ..selected = period
    ..isLoading = false;
}

void main() {
  testWidgets('student sees configured columns and subjects without scores', (
    tester,
  ) async {
    final periods = _periodController();
    addTearDown(periods.dispose);
    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periods,
          child: GradesScreen(
            token: 'student-token',
            apiClient: _TranscriptApi(),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Dự án cá nhân'), findsOneWidget);
    expect(find.text('Năng lực'), findsOneWidget);
    expect(find.text('Phản hồi'), findsOneWidget);
    expect(find.text('Đạt'), findsOneWidget);
    expect(find.text('Chủ động và hợp tác tốt'), findsOneWidget);
    expect(find.text('Vật lý'), findsOneWidget);
    expect(find.text('8.5'), findsWidgets);
    expect(find.text('BẢNG TỔNG KẾT HỌC LỰC'), findsOneWidget);
    expect(find.text('BẢNG ĐIỂM CHI TIẾT'), findsOneWidget);
  });

  testWidgets('open grade screen reloads when a realtime grade arrives', (
    tester,
  ) async {
    final periods = _periodController();
    final transcriptApi = _TranscriptApi();
    final socket = StreamController<ChatSocketEventDto>.broadcast();
    final notifications = NotificationService(
      apiClient: NotificationApiClient(backend: _EmptyNotificationBackend()),
      socketEvents: socket.stream,
      token: 'student-token',
    );
    addTearDown(periods.dispose);
    addTearDown(notifications.dispose);
    addTearDown(socket.close);
    await notifications.start();

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periods,
          child: GradesScreen(
            token: 'student-token',
            apiClient: transcriptApi,
            notificationService: notifications,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('8.5'), findsWidgets);

    transcriptApi.score = 9;
    socket.add(
      ChatSocketEventDto(
        type: 'notification.new',
        notification: NotificationDto(
          id: 45,
          title: 'Điểm đã được cập nhật môn Kỹ năng sống',
          body: 'Nguyễn An - Dự án cá nhân: 9',
          tag: 'Bảng điểm',
          isRead: false,
          relatedType: 'GRADE_PUBLISHED',
          createdAt: DateTime(2026, 7, 22, 10),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(transcriptApi.transcriptCalls, 2);
    expect(find.text('9.0'), findsWidgets);
  });
}
