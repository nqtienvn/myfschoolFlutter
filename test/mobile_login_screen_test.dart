import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/packages/apps/mobile/mobile.dart';

void main() {
  testWidgets('login screen shows requested content', (tester) async {
    await tester.pumpWidget(const MyFSchoolMobileApp());

    expect(find.text('Chào mừng quay trở lại'), findsOneWidget);
    expect(find.byIcon(Icons.phone_outlined), findsOneWidget);
    expect(find.byIcon(Icons.lock_outline), findsOneWidget);
    expect(find.text('© 2026 MyFSchool'), findsOneWidget);
    expect(find.text('Version 1.0'), findsOneWidget);
  });
}
