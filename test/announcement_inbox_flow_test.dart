import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/announcement_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/backend_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/announcement_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/school_announcement.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/announcement_inbox_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/academic_period_scope.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/announcement_inbox_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/announcements_create_screen.dart';

void main() {
  test('announcement dto maps the published notification fields', () {
    final announcement = AnnouncementDto.fromJson({
      'id': 9,
      'title': 'Lịch họp phụ huynh',
      'body': 'Nhà trường kính mời phụ huynh tham dự.',
      'teacherName': 'Cô Mai',
      'classNames': ['12A'],
      'classIds': [3],
      'targetRole': 'PARENT',
      'isRead': true,
      'createdAt': '2026-07-14T08:00:00',
      'academicYearId': 1,
      'deliveryStatus': 'PUBLISHED',
    }).toDomain();

    expect(announcement.classIds, [3]);
    expect(announcement.isRead, isTrue);
    expect(announcement.deliveryStatus, 'PUBLISHED');
  });

  testWidgets(
    'recipient opens and reads a notification without action prompts',
    (tester) async {
      final api = _FakeAnnouncementApi();
      final service = AnnouncementInboxService(
        api: api,
        token: 'token',
        teacher: false,
      );
      await service.start();

      await tester.pumpWidget(
        MaterialApp(home: AnnouncementInboxScreen(service: service)),
      );
      await tester.pumpAndSettle();

      expect(find.text('Chờ xác nhận'), findsNothing);
      expect(find.text('Gửi phản hồi'), findsNothing);

      await tester.tap(find.byKey(const ValueKey('announcement-1')));
      await tester.pumpAndSettle();

      expect(api.markReadCalls, 1);
      expect(service.unreadCount, 0);
      expect(find.text('Nhà trường kính mời phụ huynh tham dự.'), findsWidgets);
      expect(find.text('Xác nhận đã đọc'), findsNothing);
      expect(find.text('Gửi phản hồi'), findsNothing);
    },
  );

  testWidgets('teacher recipient tracking only shows read status', (
    tester,
  ) async {
    final api = _FakeAnnouncementApi()..read = true;
    final service = AnnouncementInboxService(
      api: api,
      token: 'token',
      teacher: true,
    );

    await tester.pumpWidget(
      MaterialApp(
        home: TeacherAnnouncementRecipientsScreen(
          announcement: api.current.toDomain(),
          service: service,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Đã đọc'), findsWidgets);
    expect(find.text('Đã xác nhận'), findsNothing);
    expect(find.text('Đã phản hồi'), findsNothing);
  });

  testWidgets('teacher opens composer from inbox and back returns to inbox', (
    tester,
  ) async {
    final service = AnnouncementInboxService(
      api: _FakeAnnouncementApi(),
      token: 'token',
      teacher: true,
    );
    await service.start();

    await tester.pumpWidget(
      MaterialApp(
        home: AnnouncementInboxScreen(
          service: service,
          teacherComposerBuilder: (_) =>
              Scaffold(appBar: AppBar(title: const Text('Soạn thông báo thử'))),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const ValueKey('send-class-announcement')));
    await tester.pumpAndSettle();
    expect(find.text('Soạn thông báo thử'), findsOneWidget);

    await tester.pageBack();
    await tester.pumpAndSettle();
    expect(
      find.byKey(const ValueKey('send-class-announcement')),
      findsOneWidget,
    );
  });

  testWidgets('deleting a system-rejected announcement requires confirmation', (
    tester,
  ) async {
    final api = _FakeBackendApi();
    final period = AcademicPeriod(
      academicYearId: 1,
      academicYearName: '2026-2027',
      semesterId: 1,
      semesterName: 'Học kỳ 1',
      startDate: DateTime(2026, 8, 1),
      endDate: DateTime(2026, 12, 31),
    );
    final periodController = AcademicPeriodController(token: 'token')
      ..periods = [period]
      ..selected = period
      ..isLoading = false;
    addTearDown(periodController.dispose);

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periodController,
          child: AnnouncementsCreateScreen(token: 'token', api: api),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final deleteButton = find.byKey(const ValueKey('delete-announcement-10'));
    await tester.scrollUntilVisible(
      deleteButton,
      300,
      scrollable: find.byType(Scrollable).last,
    );
    await tester.tap(deleteButton);
    await tester.pumpAndSettle();
    expect(api.deleteCalls, 0);

    await tester.tap(find.byKey(const ValueKey('cancel-delete-announcement')));
    await tester.pumpAndSettle();
    expect(api.deleteCalls, 0);

    await tester.tap(deleteButton);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('confirm-delete-announcement')));
    await tester.pumpAndSettle();
    expect(api.deleteCalls, 1);
  });

  testWidgets('teacher sees policy-check loading then published result', (
    tester,
  ) async {
    final api = _FakeBackendApi()..postCompleter = Completer<Object?>();
    final periodController = _periodController();
    addTearDown(periodController.dispose);

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periodController,
          child: AnnouncementsCreateScreen(token: 'token', api: api),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(
      find.widgetWithText(TextField, 'Tiêu đề'),
      'Thông báo hợp lệ',
    );
    await tester.enterText(
      find.widgetWithText(TextField, 'Nội dung'),
      'Nội dung thông báo',
    );
    await tester.tap(find.byType(FilterChip).first);
    await tester.tap(find.byKey(const ValueKey('send-announcement')));
    await tester.pump();

    expect(find.text('Đang kiểm tra nội dung thông báo…'), findsOneWidget);

    api.postCompleter!.complete({
      'outcome': 'PUBLISHED',
      'message': 'Thông báo đã được gửi thành công.',
      'announcement': {
        'id': 11,
        'title': 'Thông báo hợp lệ',
        'body': 'Nội dung thông báo',
        'targetRole': 'ALL',
        'classNames': ['12A'],
        'classIds': [3],
        'academicYearId': 1,
        'deliveryStatus': 'PUBLISHED',
        'violations': <Object?>[],
      },
      'violations': <Object?>[],
    });
    await tester.pumpAndSettle();

    expect(
      find.byKey(const ValueKey('announcement-published-dialog')),
      findsOneWidget,
    );
    expect(find.text('Gửi thông báo thành công'), findsOneWidget);
  });

  testWidgets('system rejection keeps draft available for editing and retry', (
    tester,
  ) async {
    final api = _FakeBackendApi()..postCompleter = Completer<Object?>();
    final periodController = _periodController();
    addTearDown(periodController.dispose);

    await tester.pumpWidget(
      MaterialApp(
        home: AcademicPeriodScope(
          controller: periodController,
          child: AnnouncementsCreateScreen(token: 'token', api: api),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.enterText(
      find.widgetWithText(TextField, 'Tiêu đề'),
      'Thông báo vi phạm',
    );
    await tester.enterText(
      find.widgetWithText(TextField, 'Nội dung'),
      'Nội dung có câu từ cấm',
    );
    await tester.tap(find.byType(FilterChip).first);
    await tester.tap(find.byKey(const ValueKey('send-announcement')));
    await tester.pump();

    api.postCompleter!.complete({
      'outcome': 'SYSTEM_REJECTED',
      'message':
          'Thông báo này đã vi phạm câu từ trong chính sách của nhà trường.',
      'announcement': {
        'id': 12,
        'title': 'Thông báo vi phạm',
        'body': 'Nội dung có câu từ cấm',
        'targetRole': 'ALL',
        'classNames': ['12A'],
        'classIds': [3],
        'academicYearId': 1,
        'deliveryStatus': 'SYSTEM_REJECTED',
        'systemRejectionMessage':
            'Thông báo này đã vi phạm câu từ trong chính sách của nhà trường.',
        'violations': [
          {'ruleId': 2, 'field': 'BODY', 'phrase': 'câu từ cấm'},
        ],
      },
      'violations': [
        {'ruleId': 2, 'field': 'BODY', 'phrase': 'câu từ cấm'},
      ],
    });
    await tester.pumpAndSettle();

    expect(
      find.byKey(const ValueKey('announcement-rejected-dialog')),
      findsOneWidget,
    );
    expect(find.text('Thông báo bị hệ thống từ chối'), findsOneWidget);
    await tester.tap(find.text('Quay lại chỉnh sửa'));
    await tester.pumpAndSettle();
    expect(find.text('Gửi lại thông báo'), findsOneWidget);
    expect(find.text('Thông báo vi phạm'), findsWidgets);
  });
}

AcademicPeriodController _periodController() {
  final period = AcademicPeriod(
    academicYearId: 1,
    academicYearName: '2026-2027',
    semesterId: 1,
    semesterName: 'Học kỳ 1',
    startDate: DateTime(2026, 8, 1),
    endDate: DateTime(2026, 12, 31),
  );
  return AcademicPeriodController(token: 'token')
    ..periods = [period]
    ..selected = period
    ..isLoading = false;
}

class _FakeAnnouncementApi implements AnnouncementApi {
  bool read = false;
  int markReadCalls = 0;

  AnnouncementDto get current => AnnouncementDto(
    id: 1,
    title: 'Lịch họp phụ huynh',
    body: 'Nhà trường kính mời phụ huynh tham dự.',
    senderName: 'Cô Mai',
    classNames: const ['12A'],
    classIds: const [3],
    targetRole: 'PARENT',
    isRead: read,
    createdAt: DateTime(2026, 7, 14),
    academicYearId: 1,
    deliveryStatus: 'PUBLISHED',
  );

  @override
  Future<AnnouncementDto> getDetail({
    required String token,
    required int id,
  }) async => current;

  @override
  Future<List<AnnouncementDto>> getAnnouncements({
    required String token,
    required bool teacher,
    int? academicYearId,
  }) async => [current];

  @override
  Future<AnnouncementRecipientPage> getRecipients({
    required String token,
    required int announcementId,
    required int academicYearId,
    int? classId,
    String? role,
    String? status,
    String? keyword,
    int page = 0,
    int size = 20,
  }) async => AnnouncementRecipientPage(
    content: [
      AnnouncementRecipient(
        userId: 7,
        userName: 'PH Nguyễn An',
        role: 'PARENT',
        studentNames: const ['Nguyễn An'],
        classNames: const ['12A'],
        status: read ? 'READ' : 'UNREAD',
        readAt: read ? DateTime(2026, 7, 15) : null,
      ),
    ],
    totalElements: 1,
    totalPages: 1,
    page: page,
  );

  @override
  Future<int> getUnreadCount({required String token}) async => read ? 0 : 1;

  @override
  Future<int> getUnreadCountForYear({
    required String token,
    int? academicYearId,
  }) => getUnreadCount(token: token);

  @override
  Future<void> markRead({required String token, required int id}) async {
    markReadCalls++;
    read = true;
  }
}

class _FakeBackendApi extends BackendApiClient {
  int deleteCalls = 0;
  Completer<Object?>? postCompleter;
  List<Map<String, dynamic>> sent = [
    {
      'id': 10,
      'title': 'Thông báo đang chờ',
      'body': 'Nội dung',
      'targetRole': 'STUDENT',
      'classNames': ['12A'],
      'classIds': [3],
      'academicYearId': 1,
      'deliveryStatus': 'SYSTEM_REJECTED',
      'systemRejectionMessage':
          'Thông báo này đã vi phạm câu từ trong chính sách của nhà trường.',
      'violations': [
        {'ruleId': 1, 'field': 'BODY', 'phrase': 'câu từ cấm'},
      ],
    },
  ];

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    if (path.endsWith('/eligible-classes')) {
      return [
        {'id': 3, 'name': '12A', 'isHomeroom': true},
      ];
    }
    if (path.endsWith('/mine')) return sent;
    throw StateError('Unexpected GET $path');
  }

  @override
  Future<Object?> deleteData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
    Object? body,
  }) async {
    deleteCalls++;
    sent = [];
    return null;
  }

  @override
  Future<Object?> postData(String path, {String? token, Object? body}) {
    if (!path.endsWith('/announcements')) {
      throw StateError('Unexpected POST $path');
    }
    return postCompleter?.future ?? Future<Object?>.value(null);
  }
}
