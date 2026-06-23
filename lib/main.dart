import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_theme.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/login_screen.dart';

void main() {
  runApp(const MyFschoolApp());
}

class MyFschoolApp extends StatelessWidget {
  const MyFschoolApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MyFschool',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      home: const LoginScreen(),
    );
  }
}
