import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/screens/forgot_password_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/screens/role_home_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/theme/fpt_mobile_theme.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  static const routeName = '/login';

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  static const double _controlRadius = 7;

  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();

  bool _obscurePassword = true;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _submit() {
    FocusScope.of(context).unfocus();
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }

    Navigator.of(context).pushReplacementNamed(
      RoleHomeScreen.routeName,
      arguments: HomeRole.parent,
    );
  }

  void _openForgotPassword() {
    Navigator.of(context).pushNamed(ForgotPasswordScreen.routeName);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            final horizontalPadding = constraints.maxWidth < 360 ? 20.0 : 32.0;
            const verticalPadding = 44.0;
            final contentHeight = constraints.maxHeight - verticalPadding;

            return SingleChildScrollView(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              padding: EdgeInsets.fromLTRB(
                horizontalPadding,
                24,
                horizontalPadding,
                20,
              ),
              child: Center(
                child: ConstrainedBox(
                  constraints: BoxConstraints(
                    maxWidth: 420,
                    minHeight: contentHeight,
                  ),
                  child: IntrinsicHeight(
                    child: AutofillGroup(
                      child: Form(
                        key: _formKey,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            const SizedBox(height: 28),
                            Center(
                              child: Image.asset(
                                'assets/images/fpt_schools_logo.jpg',
                                width: 220,
                                height: 112,
                                fit: BoxFit.contain,
                                semanticLabel: 'FPT Schools',
                              ),
                            ),
                            const SizedBox(height: 46),
                            TextFormField(
                              controller: _phoneController,
                              keyboardType: TextInputType.phone,
                              textInputAction: TextInputAction.next,
                              autofillHints: const [
                                AutofillHints.telephoneNumber,
                              ],
                              inputFormatters: [
                                FilteringTextInputFormatter.digitsOnly,
                              ],
                              decoration: _inputDecoration(
                                hint: 'Nhập số điện thoại',
                                prefixIcon: Icons.phone_outlined,
                              ),
                              validator: (value) {
                                if (value == null || value.trim().isEmpty) {
                                  return 'Vui lòng nhập số điện thoại';
                                }
                                return null;
                              },
                            ),
                            const SizedBox(height: 24),
                            TextFormField(
                              controller: _passwordController,
                              obscureText: _obscurePassword,
                              textInputAction: TextInputAction.done,
                              autofillHints: const [AutofillHints.password],
                              onFieldSubmitted: (_) => _submit(),
                              decoration: _inputDecoration(
                                hint: 'Nhập mật khẩu',
                                prefixIcon: Icons.lock_outline,
                                suffixIcon: IconButton(
                                  tooltip: _obscurePassword
                                      ? 'Hiện mật khẩu'
                                      : 'Ẩn mật khẩu',
                                  onPressed: () {
                                    setState(() {
                                      _obscurePassword = !_obscurePassword;
                                    });
                                  },
                                  icon: Icon(
                                    _obscurePassword
                                        ? Icons.visibility_outlined
                                        : Icons.visibility_off_outlined,
                                    color: const Color(0xFFB6BBC1),
                                  ),
                                ),
                              ),
                              validator: (value) {
                                if (value == null || value.isEmpty) {
                                  return 'Vui lòng nhập mật khẩu';
                                }
                                return null;
                              },
                            ),
                            Align(
                              alignment: Alignment.centerRight,
                              child: TextButton(
                                onPressed: _openForgotPassword,
                                style: TextButton.styleFrom(
                                  foregroundColor: FptMobileColors.orange,
                                  padding: const EdgeInsets.symmetric(
                                    vertical: 18,
                                    horizontal: 0,
                                  ),
                                  textStyle: const TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                                child: const Text('Quên mật khẩu?'),
                              ),
                            ),
                            const SizedBox(height: 20),
                            SizedBox(
                              height: 56,
                              child: FilledButton(
                                onPressed: _submit,
                                style: FilledButton.styleFrom(
                                  backgroundColor: FptMobileColors.orange,
                                  foregroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(
                                      _controlRadius,
                                    ),
                                  ),
                                  textStyle: const TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                                child: const Text('Đăng nhập'),
                              ),
                            ),
                            const Spacer(),
                            const SizedBox(height: 26),
                            const Column(
                              children: [
                                Text('Version 1.0'),
                                SizedBox(height: 4),
                                Text('© 2026 MyFSchool'),
                              ],
                            ),
                          ],
                        ),
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

  InputDecoration _inputDecoration({
    required String hint,
    required IconData prefixIcon,
    Widget? suffixIcon,
  }) {
    final borderRadius = BorderRadius.circular(_controlRadius);
    final border = OutlineInputBorder(
      borderRadius: borderRadius,
      borderSide: const BorderSide(color: Color(0xFFE4E6E9), width: 1.4),
    );

    return InputDecoration(
      hintText: hint,
      hintStyle: const TextStyle(color: Color(0xFFB4B8BE), fontSize: 16),
      prefixIcon: Icon(prefixIcon, color: FptMobileColors.orange, size: 22),
      prefixIconConstraints: const BoxConstraints(minWidth: 42),
      suffixIcon: suffixIcon,
      enabledBorder: border,
      focusedBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: FptMobileColors.orange, width: 1.8),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: Colors.redAccent, width: 1.4),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: const BorderSide(color: Colors.redAccent, width: 1.8),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 17),
    );
  }
}
