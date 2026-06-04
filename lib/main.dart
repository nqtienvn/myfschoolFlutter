import 'dart:async';

import 'package:myfschoolse1913/vn/edu/fpt/packages/core/models/app_notification.dart';
import 'package:myfschoolse1913/vn/edu/fpt/packages/core/service/notification_service.dart';

import 'dart:async';

Future<void> main() async {
  const service = NotificationService();

  final StreamSubscription<AppNotification> subscription =
  service.watchNotifications().listen(
        (notification) {
      print('[${notification.title}] ${notification.message}');
    },
    onError: (Object error, StackTrace stackTrace) {
      print('Stream error: $error');
    },
    onDone: () {
      print('Stream done');
    },
  );

  await Future<void>.delayed(const Duration(seconds: 12));
  await subscription.cancel();
}
