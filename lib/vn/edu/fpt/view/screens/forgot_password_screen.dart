import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/exception/backend_api_exception.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/auth_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/primary_button.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key, required this.authService});

  final AuthService authService;

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();
  bool _submitting = false;
  bool _submitted = false;
  String? _error;

  @override
  void dispose() {
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await widget.authService.requestPasswordReset(_phoneController.text.trim());
      if (mounted) setState(() => _submitted = true);
    } on BackendApiException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'Không thể kết nối máy chủ. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Quên mật khẩu')),
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: AppCard(child: _submitted ? _buildSuccess() : _buildForm()),
          ),
        ),
      ),
    );
  }

  Widget _buildSuccess() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Icon(Icons.mark_email_read_outlined, size: 64, color: AppColors.success),
        const SizedBox(height: AppSpacing.md),
        Text('Đã tiếp nhận yêu cầu', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: AppSpacing.sm),
        const Text(
          'Nếu tài khoản đủ điều kiện, hướng dẫn đặt lại mật khẩu sẽ được gửi tới email đã xác minh. Hãy kiểm tra cả thư mục spam.',
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: AppSpacing.lg),
        PrimaryButton(label: 'Quay lại đăng nhập', onPressed: () => Navigator.pop(context)),
      ],
    );
  }

  Widget _buildForm() {
    return Form(
      key: _formKey,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Khôi phục tài khoản', style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: AppSpacing.sm),
          const Text(
            'Áp dụng cho tài khoản Phụ huynh, Học sinh và Giáo viên có email đã xác minh.',
          ),
          const SizedBox(height: AppSpacing.lg),
          TextFormField(
            controller: _phoneController,
            keyboardType: TextInputType.phone,
            autofillHints: const [AutofillHints.username],
            decoration: const InputDecoration(
              labelText: 'Số điện thoại đăng nhập',
              prefixIcon: Icon(Icons.phone_outlined),
            ),
            validator: (value) => RegExp(r'^0\d{9}$').hasMatch(value?.trim() ?? '')
                ? null
                : 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0',
          ),
          if (_error != null) ...[
            const SizedBox(height: AppSpacing.sm),
            Text(_error!, style: const TextStyle(color: AppColors.danger)),
          ],
          const SizedBox(height: AppSpacing.lg),
          PrimaryButton(
            label: 'Gửi hướng dẫn qua email',
            icon: Icons.mail_outline,
            isLoading: _submitting,
            onPressed: _submitting ? null : _submit,
          ),
        ],
      ),
    );
  }
}
