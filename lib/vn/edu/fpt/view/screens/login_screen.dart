import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/role_selection_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, this.authService, this.chatService});

  final AuthService? authService;
  final ChatService? chatService;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _phoneController = TextEditingController(text: '0909000002');
  final _passwordController = TextEditingController(text: 'test1234');
  bool _isLoading = false;
  String? _error;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    final authService = widget.authService;
    final chatService = widget.chatService;
    if (authService == null || chatService == null) {
      _openRoleSelection();
      return;
    }

    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final session = await authService.login(
        _phoneController.text.trim(),
        _passwordController.text,
      );
      await chatService.start(session);
      if (!mounted) return;
      _openRoleSelection();
    } catch (_) {
      if (!mounted) return;
      setState(() => _error = 'Không đăng nhập được. Vui lòng kiểm tra tài khoản.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _openRoleSelection() {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => RoleSelectionScreen(
          authService: widget.authService,
          chatService: widget.chatService,
        ),
      ),
    );
  }

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
                            controller: _phoneController,
                            keyboardType: TextInputType.phone,
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
                            controller: _passwordController,
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
                          if (_error != null) ...[
                            const SizedBox(height: AppSpacing.sm),
                            Text(
                              _error!,
                              style: const TextStyle(color: AppColors.danger, fontWeight: FontWeight.w600),
                            ),
                          ],
                          const SizedBox(height: AppSpacing.lg),
                          PrimaryButton(
                            label: 'Đăng Nhập',
                            icon: Icons.login,
                            isLoading: _isLoading,
                            onPressed: _isLoading ? null : _login,
                          ),
                          const Spacer(), //ăn hết khoảng chống
                          const Text('Copyright by Fpt School'),
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
