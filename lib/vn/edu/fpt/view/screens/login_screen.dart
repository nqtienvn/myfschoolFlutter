import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/exception/backend_api_exception.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/app_shell.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/forgot_password_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, this.authService, this.chatService});

  final AuthService? authService;
  final ChatService? chatService;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  static const _defaultPhone = String.fromEnvironment(
    'DEMO_LOGIN_PHONE',
    defaultValue: '0902000001',
  );
  static const _defaultPassword = String.fromEnvironment(
    'DEMO_DATA_PASSWORD',
    defaultValue: 'Demo@123',
  );

  final _phoneController = TextEditingController(text: _defaultPhone);
  final _passwordController = TextEditingController(text: _defaultPassword);
  bool _isLoading = false;
  bool _obscurePassword = true;
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
    if (authService == null || chatService == null) return;

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
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute<void>(
          builder: (_) => AppShell(
            actor: session.actor,
            session: session,
            authService: authService,
            chatService: chatService,
          ),
        ),
        (route) => false,
      );
    } on BackendApiException catch (error) {
      if (!mounted) return;
      setState(() => _error = error.message);
    } catch (_) {
      if (!mounted) return;
      setState(() => _error = 'Không thể kết nối máy chủ. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
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
                constraints: BoxConstraints(minHeight: constraints.maxHeight),
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
                                borderRadius: BorderRadius.circular(
                                  AppRadius.md,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: AppSpacing.md),
                          TextField(
                            controller: _passwordController,
                            obscureText: _obscurePassword,
                            decoration: InputDecoration(
                              labelText: 'Mật khẩu',
                              prefixIcon: const Icon(Icons.lock),
                              suffixIcon: IconButton(
                                icon: Icon(
                                  _obscurePassword
                                      ? Icons.visibility_off
                                      : Icons.visibility,
                                ),
                                onPressed: () {
                                  setState(() {
                                    _obscurePassword = !_obscurePassword;
                                  });
                                },
                              ),
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(
                                  AppRadius.md,
                                ),
                              ),
                            ),
                          ),
                          if (_error != null) ...[
                            const SizedBox(height: AppSpacing.sm),
                            Text(
                              _error!,
                              style: const TextStyle(
                                color: AppColors.danger,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                          const SizedBox(height: AppSpacing.lg),
                          PrimaryButton(
                            label: 'Đăng Nhập',
                            icon: Icons.login,
                            isLoading: _isLoading,
                            onPressed: _isLoading ? null : _login,
                          ),
                          TextButton(
                            onPressed: _isLoading || widget.authService == null
                                ? null
                                : () => Navigator.of(context).push(
                                      MaterialPageRoute<void>(
                                        builder: (_) => ForgotPasswordScreen(
                                          authService: widget.authService!,
                                        ),
                                      ),
                                    ),
                            child: const Text('Quên mật khẩu?'),
                          ),
                          const Spacer(), //ăn hết khoảng chống
                          const Text('Copyright by FPT Schools'),
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
