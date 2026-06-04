import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/packages/apps/mobile/screens/login_screen.dart';

class MyFSchoolMobileApp extends StatelessWidget {
  const MyFSchoolMobileApp({super.key});

  static const Color primaryOrange = Color(0xFFF36F21);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'MyFSchool',
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: Colors.white,
        colorScheme: const ColorScheme.light(
          primary: primaryOrange,
          surface: Colors.white,
          onSurface: Color(0xFF27313A),
        ),
      ),
      home: const LoginScreen(),
    );
  }
}
