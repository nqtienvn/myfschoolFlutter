import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/screens/forgot_password_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/screens/login_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/screens/role_home_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/theme/fpt_mobile_theme.dart';

class MyFSchoolMobileApp extends StatelessWidget {
  const MyFSchoolMobileApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'MyFSchool',
      theme: FptMobileTheme.build(),
      home: const LoginScreen(),
      routes: {
        LoginScreen.routeName: (_) => const LoginScreen(),
        ForgotPasswordScreen.routeName: (_) => const ForgotPasswordScreen(),
      },
      onGenerateRoute: (settings) {
        if (settings.name == RoleHomeScreen.routeName) {
          final role = settings.arguments is HomeRole
              ? settings.arguments! as HomeRole
              : HomeRole.parent;

          return MaterialPageRoute<void>(
            builder: (_) => RoleHomeScreen(role: role),
            settings: settings,
          );
        }

        return null;
      },
    );
  }
}
