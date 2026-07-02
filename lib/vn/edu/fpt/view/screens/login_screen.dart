import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/role_selection_screen.dart';

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key, this.authService, this.chatService});

  final AuthService? authService;
  final ChatService? chatService;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: constraints.maxHeight,
                ),
                child: IntrinsicHeight(
                  child: Padding(
                    padding: const EdgeInsets.all(AppSpacing.lg),
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
                              ),
                            ),
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
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => RoleSelectionScreen(
                                    authService: authService,
                                    chatService: chatService,
                                  ),
                                ),
                              );
                            },
                          ),
                          const Spacer(), //ăn hết khoảng chống
                          const Text("Copyright by Fpt School"),
                          const Text('Version 1.0'),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}
