import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/announcement_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/announcement_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/school_announcement.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/announcement_inbox_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/announcement_inbox_screen.dart';

void main() {
  test('announcement dto maps acknowledgement and real statistics', () {
    final dto = AnnouncementDto.fromJson({
      'id': 9,
      'title': 'Họp phụ huynh',
      'body': 'Vui lòng xác nhận',
      'teacherName': 'Cô Mai',
      'classNames': ['12A'],
      'classIds': [3],
      'targetRole': 'PARENT',
      'requiresReply': true,
      'isRead': true,
      'acknowledged': true,
      'replyText': 'Tôi sẽ tham gia',
      'repliedAt': '2026-07-15T08:00:00',
      'recipientStatus': 'REPLIED',
      'totalRecipients': 30,
      'readCount': 25,
      'acknowledgedCount': 20,
      'repliedCount': 7,
      'createdAt': '2026-07-14T08:00:00',
      'academicYearId': 1,
      'approvalStatus': 'APPROVED',
    }).toDomain();

    expect(dto.classIds, [3]);
    expect(dto.recipientStatus, 'REPLIED');
    expect(dto.acknowledgedCount, 20);
    expect(dto.repliedCount, 7);
  });

  testWidgets(
    'parent reads, acknowledges and replies with separate pending badge',
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

      expect(
        find.byKey(const ValueKey('pending-action-badge')),
        findsOneWidget,
      );
      expect(find.text('Chờ xác nhận'), findsOneWidget);

      await tester.tap(find.byKey(const ValueKey('announcement-1')));
      await tester.pumpAndSettle();
      expect(api.markReadCalls, 1);
      expect(
        find.byKey(const ValueKey('acknowledge-announcement')),
        findsOneWidget,
      );

      await tester.tap(find.byKey(const ValueKey('acknowledge-announcement')));
      await tester.pumpAndSettle();
      expect(api.acknowledgeCalls, 1);
      expect(find.text('Đã xác nhận đã đọc'), findsOneWidget);

      await tester.enterText(
        find.byKey(const ValueKey('announcement-reply-field')),
        'Gia đình đã nhận thông báo',
      );
      await tester.tap(find.byKey(const ValueKey('send-announcement-reply')));
      await tester.pumpAndSettle();
      expect(api.replyCalls, 1);
      expect(
        find.byKey(const ValueKey('sent-announcement-reply')),
        findsOneWidget,
      );
      expect(find.textContaining('Gia đình đã nhận thông báo'), findsOneWidget);
      expect(service.pendingActionCount, 0);
    },
  );

  testWidgets('teacher receives announcements without recipient tracking', (
    tester,
  ) async {
    final api = _FakeAnnouncementApi();
    final service = AnnouncementInboxService(
      api: api,
      token: 'token',
      teacher: true,
    );
    await service.start();

    await tester.pumpWidget(
      MaterialApp(home: AnnouncementInboxScreen(service: service)),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Họp phụ huynh').first);
    await tester.pumpAndSettle();

    expect(find.text('Theo dõi người nhận'), findsNothing);
    expect(find.text('Nhà trường kính mời phụ huynh tham dự.'), findsWidgets);
  });
}

class _FakeAnnouncementApi implements AnnouncementApi {
  bool read = false;
  bool acknowledged = false;
  String? replyText;
  int markReadCalls = 0;
  int acknowledgeCalls = 0;
  int replyCalls = 0;

  AnnouncementDto get current => AnnouncementDto(
    id: 1,
    title: 'Họp phụ huynh',
    body: 'Nhà trường kính mời phụ huynh tham dự.',
    senderName: 'Cô Mai',
    classNames: const ['12A'],
    classIds: const [3],
    targetRole: 'PARENT',
    requiresReply: true,
    isRead: read,
    acknowledged: acknowledged,
    replyText: replyText,
    repliedAt: replyText == null ? null : DateTime(2026, 7, 15),
    recipientStatus: replyText != null
        ? 'REPLIED'
        : acknowledged
        ? 'ACKNOWLEDGED'
        : read
        ? 'READ'
        : 'UNREAD',
    totalRecipients: 30,
    readCount: read ? 1 : 0,
    acknowledgedCount: acknowledged ? 1 : 0,
    repliedCount: replyText == null ? 0 : 1,
    createdAt: DateTime(2026, 7, 14),
    academicYearId: 1,
    approvalStatus: 'APPROVED',
  );

  @override
  Future<void> acknowledge({required String token, required int id}) async {
    acknowledgeCalls++;
    acknowledged = true;
  }

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
  Future<int> getPendingActionCount({required String token}) async =>
      acknowledged ? 0 : 1;

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
    content: const [
      AnnouncementRecipient(
        userId: 7,
        userName: 'PH Nguyễn An',
        role: 'PARENT',
        studentNames: ['Nguyễn An'],
        classNames: ['12A'],
        status: 'REPLIED',
        replyText: 'Tôi sẽ tham gia',
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

  @override
  Future<void> reply({
    required String token,
    required int id,
    required String text,
  }) async {
    replyCalls++;
    acknowledged = true;
    replyText = text;
  }
}
