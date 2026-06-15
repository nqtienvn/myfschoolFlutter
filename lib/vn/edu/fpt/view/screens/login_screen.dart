import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import '../design_system/app_radius.dart';
import '../design_system/app_spacing.dart';

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: SafeArea(
          child: AppCard(
            child: Column(
              children: [
                Image.asset(
                  'assets/images/fpt_schools_logo.jpg',
                  width: 200,
                  height: 200,
                ),
                const SizedBox(height: AppSpacing.md),
                TextField(
                  decoration: InputDecoration(
                    labelText: 'Số điện thoại',
                    prefixIcon: const Icon(Icons.phone),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppRadius.md),
                    )
                  )
                ),
                const SizedBox(height: AppSpacing.md),
                TextField(
                  obscureText: true,
                  decoration: InputDecoration(
                    labelText: 'Mật khẩu',
                    prefixIcon: const Icon(Icons.lock),
                      suffixIcon: const Icon(Icons.remove_red_eye),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(AppRadius.md),
                      ),
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),
                PrimaryButton(
                  label: 'Đăng Nhập',
                  icon: Icons.login,
                  onPressed: () {
                    print('login successed');
                  },
                ),
                const Spacer(), //ăn hết khoảng chống
                Text("Copyright by Fpt School"),
                Text('Version 1.0'),
              ],
            ),
          ),
        ),
      ),
      debugShowCheckedModeBanner: false,
    );
  }
}
