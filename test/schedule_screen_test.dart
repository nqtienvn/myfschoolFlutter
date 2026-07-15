import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/backend_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/client/schedule_api_client.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/schedule_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/schedule_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

void main() {
  testWidgets('schedule uses sunrise and sunset colors by shift', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(320, 720));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    final backend = _ScheduleBackend();
    final service = ScheduleService(
      apiClient: ScheduleApiClient(backend: backend),
      token: 'token',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: ScheduleScreen(service: service, mode: ScheduleViewMode.student),
      ),
    );
    await tester.pumpAndSettle();

    final brand = tester.widget<RichText>(
      find.byKey(const ValueKey('orange-top-bar-brand')),
    );
    expect(brand.text.toPlainText(), 'FPT Schools');
    expect(find.byKey(const ValueKey('schedule-period-20')), findsOneWidget);
    expect(find.byKey(const ValueKey('schedule-period-21')), findsOneWidget);
    expect(find.text('TIẾT'), findsNWidgets(2));
    expect(find.text('1'), findsOneWidget);
    expect(find.text('5'), findsOneWidget);
    expect(find.text('Tiết 1'), findsNothing);
    expect(find.text('Toán'), findsOneWidget);
    expect(find.text('Nguyễn Văn A'), findsOneWidget);
    expect(find.text('P.203'), findsOneWidget);

    final morningBadge = tester.widget<Container>(
      find.byKey(const ValueKey('schedule-period-20')),
    );
    final afternoonBadge = tester.widget<Container>(
      find.byKey(const ValueKey('schedule-period-21')),
    );
    expect(
      (morningBadge.decoration! as BoxDecoration).color,
      AppColors.sunrise.withValues(alpha: 0.08),
    );
    expect(
      (afternoonBadge.decoration! as BoxDecoration).color,
      AppColors.sunset.withValues(alpha: 0.08),
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('shared header spells FPT Schools', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(home: Scaffold(body: SharedHeader())),
    );

    final brand = tester.widget<RichText>(
      find.byKey(const ValueKey('shared-header-brand')),
    );
    expect(brand.text.toPlainText(), 'FPT Schools');
  });
}

class _ScheduleBackend extends BackendApiClient {
  _ScheduleBackend() : super(baseUrl: 'http://localhost');

  @override
  Future<Object?> getData(
    String path, {
    String? token,
    Map<String, String?> query = const {},
  }) async {
    expect(path, '/api/schedules/me');
    expect(token, 'token');
    return {
      'classId': 12,
      'className': '12A',
      'semesterId': 1,
      'semesterName': 'Học kỳ I',
      'days': [
        {
          'dayOfWeek': 2,
          'dayOfWeekName': 'Thứ 2',
          'morningSlots': [
            {
              'id': 20,
              'classId': 12,
              'className': '12A',
              'subjectName': 'Toán',
              'subjectCode': 'TOAN12',
              'teacherName': 'Nguyễn Văn A',
              'dayOfWeek': 2,
              'period': 1,
              'periodName': 'Tiết 1',
              'room': 'P.203',
              'shift': 'MORNING',
            },
          ],
          'afternoonSlots': [
            {
              'id': 21,
              'classId': 12,
              'className': '12A',
              'subjectName': 'Ngữ văn',
              'subjectCode': 'VAN12',
              'teacherName': 'Trần Thị B',
              'dayOfWeek': 2,
              'period': 5,
              'periodName': 'Tiết 5',
              'room': 'P.204',
              'shift': 'AFTERNOON',
            },
          ],
        },
      ],
    };
  }
}
