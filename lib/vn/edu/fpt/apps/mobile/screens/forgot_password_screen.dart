import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/screens/role_home_screen.dart';
import 'package:myfschoolse1913/vn/edu/fpt/apps/mobile/theme/fpt_mobile_theme.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  static const routeName = '/forgot-password';

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  static const double _controlRadius = 5;

  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();

  @override
  void dispose() {
    _phoneController.dispose();
    super.dispose();
  }

  void _submit() {
    FocusScope.of(context).unfocus();
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Yêu cầu khôi phục mật khẩu đã được ghi nhận.'),
      ),
    );
  }

  void _goHome() {
    Navigator.of(context).pushNamedAndRemoveUntil(
      RoleHomeScreen.routeName,
      (route) => false,
      arguments: HomeRole.parent,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              padding: const EdgeInsets.fromLTRB(34, 42, 34, 18),
              child: Center(
                child: ConstrainedBox(
                  constraints: BoxConstraints(
                    maxWidth: 360,
                    minHeight: constraints.maxHeight - 60,
                  ),
                  child: IntrinsicHeight(
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const SizedBox(height: 38),
                          Center(
                            child: Image.asset(
                              'assets/images/fpt_schools_logo.jpg',
                              width: 148,
                              height: 76,
                              fit: BoxFit.contain,
                              semanticLabel: 'FPT Schools',
                            ),
                          ),
                          const SizedBox(height: 26),
                          const Text(
                            'Quên mật khẩu',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: Colors.black,
                              fontSize: 20,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _phoneController,
                            keyboardType: TextInputType.phone,
                            textInputAction: TextInputAction.done,
                            onFieldSubmitted: (_) => _submit(),
                            autofillHints: const [
                              AutofillHints.telephoneNumber,
                            ],
                            inputFormatters: [
                              FilteringTextInputFormatter.digitsOnly,
                            ],
                            decoration: _inputDecoration(),
                            validator: (value) {
                              if (value == null || value.trim().isEmpty) {
                                return 'Vui lòng nhập số điện thoại';
                              }
                              if (value.trim().length < 9) {
                                return 'Số điện thoại chưa hợp lệ';
                              }
                              return null;
                            },
                          ),
                          Align(
                            alignment: Alignment.centerRight,
                            child: TextButton(
                              onPressed: _goHome,
                              style: TextButton.styleFrom(
                                foregroundColor: FptMobileColors.orange,
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 0,
                                  vertical: 12,
                                ),
                                textStyle: const TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                              child: const Text('Về Trang Chủ?'),
                            ),
                          ),
                          const Spacer(),
                          const Column(
                            children: [
                              Text(
                                'version 1.0',
                                style: TextStyle(
                                  color: Colors.black,
                                  fontSize: 10,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                              SizedBox(height: 2),
                              Text(
                                '@ 2026 Fschools',
                                style: TextStyle(
                                  color: Colors.black,
                                  fontSize: 10,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
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

  InputDecoration _inputDecoration() {
    final radius = BorderRadius.circular(_controlRadius);
    const normalBorder = BorderSide(color: Color(0xFF525252), width: 1.1);

    return InputDecoration(
      hintText: 'Nhập số điện thoại',
      hintStyle: const TextStyle(
        color: Color(0xFF9B9B9B),
        fontSize: 12,
        fontWeight: FontWeight.w700,
      ),
      isDense: true,
      prefixIcon: const Icon(
        Icons.phone_in_talk_outlined,
        color: Colors.black,
        size: 19,
      ),
      prefixIconConstraints: const BoxConstraints(minWidth: 34),
      enabledBorder: OutlineInputBorder(
        borderRadius: radius,
        borderSide: normalBorder,
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: radius,
        borderSide: const BorderSide(color: FptMobileColors.orange, width: 1.4),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: radius,
        borderSide: const BorderSide(color: FptMobileColors.danger, width: 1.2),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: radius,
        borderSide: const BorderSide(color: FptMobileColors.danger, width: 1.4),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 11),
    );
  }
}
