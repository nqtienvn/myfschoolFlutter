import 'package:flutter/material.dart';
import 'package:myfschoolse1913/loginScreen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/theme/fpt_mobile_theme.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: FptMobileTheme.build(),
      home: const LoginScreen(),
    );
  }
}
