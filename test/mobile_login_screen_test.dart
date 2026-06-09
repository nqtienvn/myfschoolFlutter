import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/mobile.dart';

void main() {
  testWidgets('login screen shows requested content', (tester) async {
    await tester.pumpWidget(const MyFSchoolMobileApp());

    expect(find.widgetWithText(FilledButton, 'Đăng nhập'), findsOneWidget);
    expect(find.text('Chào mừng quay trở lại'), findsNothing);
    expect(find.byIcon(Icons.phone_outlined), findsOneWidget);
    expect(find.byIcon(Icons.lock_outline), findsOneWidget);
    expect(find.text('© 2026 MyFSchool'), findsOneWidget);
    expect(find.text('Version 1.0'), findsOneWidget);
  });

  testWidgets('forgot password route opens from login screen', (tester) async {
    await tester.pumpWidget(const MyFSchoolMobileApp());

    await tester.tap(find.text('Quên mật khẩu?'));
    await tester.pumpAndSettle();

    expect(find.text('Quên mật khẩu'), findsOneWidget);
    expect(find.text('Nhập số điện thoại'), findsOneWidget);
    expect(find.text('Về Trang Chủ?'), findsOneWidget);
  });

  testWidgets('role home renders parent style content', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: FptMobileTheme.build(),
        home: const RoleHomeScreen(role: HomeRole.parent),
      ),
    );

    expect(find.text('Các chức năng'), findsOneWidget);
    expect(find.text('Lớp 1A - FPT Schools'), findsOneWidget);
    expect(find.text('Gửi thông báo học sinh'), findsOneWidget);
    expect(find.text('Trang chủ'), findsOneWidget);
  });
}
