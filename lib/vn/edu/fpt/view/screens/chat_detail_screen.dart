import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

class ChatThread {
  const ChatThread({
    required this.title,
    required this.subtitle,
    required this.lastMessage,
    required this.time,
    required this.accentColor,
    this.tag,
    this.initialMessages = const [],
  });

  final String title;
  final String subtitle;
  final String lastMessage;
  final String time;
  final Color accentColor;
  final String? tag;
  final List<ChatMessage> initialMessages;
}

class ChatMessage {
  const ChatMessage({
    required this.text,
    required this.time,
    required this.isMine,
  });

  final String text;
  final String time;
  final bool isMine;
}

class ChatDetailScreen extends StatefulWidget {
  const ChatDetailScreen({super.key, required this.thread});

  final ChatThread thread;

  @override
  State<ChatDetailScreen> createState() => _ChatDetailScreenState();
}

class _ChatDetailScreenState extends State<ChatDetailScreen> {
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  late final List<ChatMessage> _messages;

  @override
  void initState() {
    super.initState();
    _messages = widget.thread.initialMessages.isNotEmpty
        ? List<ChatMessage>.of(widget.thread.initialMessages)
        : [
            ChatMessage(
              text: widget.thread.lastMessage,
              time: widget.thread.time,
              isMine: false,
            ),
          ];
  }

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _sendMessage() {
    final text = _controller.text.trim();
    if (text.isEmpty) return;

    setState(() {
      _messages.add(ChatMessage(text: text, time: 'Vừa xong', isMine: true));
    });
    _controller.clear();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent + 80,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      resizeToAvoidBottomInset: true,
      appBar: const OrangeTopBar(title: 'Tin nhắn'),
      body: Column(
        children: [
          _ContactHeader(thread: widget.thread),
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.fromLTRB(
                AppSpacing.lg,
                AppSpacing.sm,
                AppSpacing.lg,
                AppSpacing.lg,
              ),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                return _MessageBubble(
                  message: _messages[index],
                  accentColor: widget.thread.accentColor,
                );
              },
            ),
          ),
          _MessageComposer(controller: _controller, onSend: _sendMessage),
        ],
      ),
    );
  }
}

class _ContactHeader extends StatelessWidget {
  const _ContactHeader({required this.thread});

  final ChatThread thread;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.lg,
        AppSpacing.lg,
        AppSpacing.lg,
        AppSpacing.sm,
      ),
      child: AppCard(
        child: Row(
          children: [
            CircleAvatar(
              radius: 24,
              backgroundColor: thread.accentColor.withValues(alpha: 0.12),
              child: Icon(Icons.person, color: thread.accentColor),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          thread.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w900,
                            color: AppColors.ink,
                          ),
                        ),
                      ),
                      if (thread.tag != null && thread.tag!.isNotEmpty) ...[
                        const SizedBox(width: AppSpacing.sm),
                        StatusPill(
                          label: thread.tag!,
                          foreground: thread.accentColor,
                          background: thread.accentColor.withValues(alpha: 0.12),
                          compact: true,
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    thread.subtitle,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.muted,
                      height: 1.25,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({required this.message, required this.accentColor});

  final ChatMessage message;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final isMine = message.isMine;
    final bubbleColor = isMine ? AppColors.fptOrange : AppColors.surface;
    final textColor = isMine ? Colors.white : AppColors.ink;
    final timeColor = isMine ? Colors.white.withValues(alpha: 0.78) : AppColors.quiet;

    return Align(
      alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: BoxConstraints(maxWidth: MediaQuery.sizeOf(context).width * 0.74),
        margin: const EdgeInsets.only(bottom: AppSpacing.sm),
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.sm),
        decoration: BoxDecoration(
          color: bubbleColor,
          border: isMine ? null : Border.all(color: AppColors.line.withValues(alpha: 0.8)),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(18),
            topRight: const Radius.circular(18),
            bottomLeft: Radius.circular(isMine ? 18 : AppRadius.sm),
            bottomRight: Radius.circular(isMine ? AppRadius.sm : 18),
          ),
          boxShadow: [
            BoxShadow(
              color: (isMine ? AppColors.fptOrange : AppColors.ink).withValues(alpha: 0.06),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              message.text,
              style: TextStyle(fontSize: 13.5, color: textColor, height: 1.35),
            ),
            const SizedBox(height: AppSpacing.xs),
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (!isMine) ...[
                  Icon(Icons.circle, size: 6, color: accentColor.withValues(alpha: 0.65)),
                  const SizedBox(width: AppSpacing.xs),
                ],
                Text(
                  message.time,
                  style: TextStyle(fontSize: 10.5, color: timeColor, fontWeight: FontWeight.w600),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _MessageComposer extends StatelessWidget {
  const _MessageComposer({required this.controller, required this.onSend});

  final TextEditingController controller;
  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(
          AppSpacing.lg,
          AppSpacing.sm,
          AppSpacing.lg,
          AppSpacing.md,
        ),
        decoration: const BoxDecoration(
          color: AppColors.surface,
          border: Border(top: BorderSide(color: AppColors.line, width: 1)),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Expanded(
              child: TextField(
                controller: controller,
                minLines: 1,
                maxLines: 4,
                textInputAction: TextInputAction.newline,
                decoration: InputDecoration(
                  hintText: 'Nhập tin nhắn...',
                  prefixIcon: const Icon(Icons.chat_bubble_outline, size: 20),
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.md,
                    vertical: AppSpacing.sm,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                    borderSide: BorderSide.none,
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                    borderSide: BorderSide.none,
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                    borderSide: const BorderSide(color: AppColors.fptOrange),
                  ),
                  filled: true,
                  fillColor: AppColors.background,
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            SizedBox(
              width: 46,
              height: 46,
              child: IconButton.filled(
                style: IconButton.styleFrom(backgroundColor: AppColors.fptOrange),
                onPressed: onSend,
                icon: const Icon(Icons.send_rounded, color: Colors.white, size: 20),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
