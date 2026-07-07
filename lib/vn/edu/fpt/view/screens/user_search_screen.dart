import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/api/dto/search_result_dto.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/chat_detail_screen.dart';

class UserSearchScreen extends StatefulWidget {
  const UserSearchScreen({super.key, required this.chatService});

  final ChatService chatService;

  @override
  State<UserSearchScreen> createState() => _UserSearchScreenState();
}

class _UserSearchScreenState extends State<UserSearchScreen> {
  final _controller = TextEditingController();
  final _focusNode = FocusNode();
  List<SearchResultDto> _results = [];
  bool _isLoading = false;
  String? _error;

  @override
  void dispose() {
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  Future<void> _search(String keyword) async {
    final trimmed = keyword.trim();
    if (trimmed.isEmpty) {
      setState(() {
        _results = [];
        _error = null;
      });
      return;
    }
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final results = await widget.chatService.searchUsers(trimmed);
      if (!mounted) return;
      setState(() => _results = results);
    } catch (_) {
      if (!mounted) return;
      setState(() => _error = 'Không tìm được. Thử lại sau.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _onTapUser(SearchResultDto user) async {
    try {
      final conversation = await widget.chatService.createConversation(
        otherUserId: user.id,
      );
      if (!mounted) return;

      // Kiểm tra route hiện tại để tránh race condition khi người dùng nhấn back trong lúc đang tải
      final route = ModalRoute.of(context);
      if (route == null || !route.isCurrent) return;

      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(
          builder: (_) => ChatDetailScreen(
            chatService: widget.chatService,
            conversation: conversation,
          ),
        ),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Không tạo được hội thoại. Thử lại sau.'),
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Tìm kiếm người dùng'),
        backgroundColor: AppColors.fptOrange,
        foregroundColor: Colors.white,
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: TextField(
              controller: _controller,
              focusNode: _focusNode,
              keyboardType: TextInputType.text,
              decoration: InputDecoration(
                hintText: 'Nhập tên hoặc số điện thoại...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _controller.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          _controller.clear();
                          _search('');
                          _focusNode.requestFocus();
                        },
                      )
                    : null,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppRadius.md),
                ),
                filled: true,
                fillColor: AppColors.surface,
              ),
              onChanged: (value) {
                setState(() {});
                _search(value);
              },
              onSubmitted: _search,
            ),
          ),
          if (_isLoading)
            const Expanded(
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_error != null)
            Expanded(
              child: Center(
                child: Text(_error!, style: const TextStyle(color: AppColors.danger)),
              ),
            )
          else if (_results.isEmpty && _controller.text.isNotEmpty)
            const Expanded(
              child: Center(
                child: Text('Không tìm thấy kết quả nào.', style: TextStyle(color: AppColors.muted)),
              ),
            )
          else
            Expanded(
              child: ListView.separated(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
                itemCount: _results.length,
                separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.sm),
                itemBuilder: (context, index) {
                  final user = _results[index];
                  return _UserResultCard(user: user, onTap: () => _onTapUser(user));
                },
              ),
            ),
        ],
      ),
    );
  }
}

class _UserResultCard extends StatelessWidget {
  const _UserResultCard({required this.user, required this.onTap});

  final SearchResultDto user;
  final VoidCallback onTap;

  String _roleLabel(String role) {
    switch (role) {
      case 'PARENT':
        return 'Phụ huynh';
      case 'TEACHER':
        return 'Giáo viên';
      case 'STUDENT':
        return 'Học sinh';
      default:
        return role;
    }
  }

  Color _roleColor(String role) {
    switch (role) {
      case 'PARENT':
        return AppColors.fptOrange;
      case 'TEACHER':
        return AppColors.blue;
      case 'STUDENT':
        return AppColors.green;
      default:
        return AppColors.muted;
    }
  }

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: 0,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: _roleColor(user.role).withValues(alpha: 0.12),
                radius: 22,
                child: Icon(Icons.person, color: _roleColor(user.role), size: 20),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      user.name,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: AppColors.ink,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      user.phone,
                      style: const TextStyle(fontSize: 12, color: AppColors.muted),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: _roleColor(user.role).withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  _roleLabel(user.role),
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: _roleColor(user.role),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              const Icon(Icons.chevron_right, color: AppColors.quiet, size: 18),
            ],
          ),
        ),
      ),
    );
  }
}
