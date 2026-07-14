import 'dart:async';

import 'package:flutter/material.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/models.dart' as domain;
import 'package:myfschoolse1913/vn/edu/fpt/src/services/services.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_colors.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_radius.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/app_spacing.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/design_system/widgets/app_card.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/school_ui_widgets.dart';

String chatStatusLabel(String status) {
  switch (status) {
    case 'sending':
      return 'Đang gửi';
    case 'delivered':
      return 'Đã nhận';
    case 'read':
      return 'Đã xem';
    case 'failed':
      return 'Gửi lỗi';
    case 'sent':
    default:
      return 'Đã gửi';
  }
}

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
  const ChatDetailScreen({
    super.key,
    this.thread,
    this.conversation,
    this.chatService,
  }) : assert(thread != null || conversation != null);

  final ChatThread? thread;
  final domain.Conversation? conversation;
  final ChatService? chatService;

  @override
  State<ChatDetailScreen> createState() => _ChatDetailScreenState();
}

class _ChatDetailScreenState extends State<ChatDetailScreen> {
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  late final List<ChatMessage> _messages;
  Timer? _typingStopTimer;
  DateTime? _lastTypingStart;
  bool _isLoadingOlder = false;

  @override
  void initState() {
    super.initState();
    final thread = widget.thread;
    _messages = thread == null
        ? <ChatMessage>[]
        : thread.initialMessages.isNotEmpty
        ? List<ChatMessage>.of(thread.initialMessages)
        : [
            ChatMessage(
              text: thread.lastMessage,
              time: thread.time,
              isMine: false,
            ),
          ];
    final conversation = widget.conversation;
    if (conversation != null) {
      widget.chatService?.openConversation(conversation.id);
    }
    _scrollController.addListener(_onScroll);
  }

  void _onScroll() {
    if (_isLoadingOlder) return;
    final conversation = widget.conversation;
    final service = widget.chatService;
    if (conversation == null || service == null) return;
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      _loadOlderMessages(conversation.id, service);
    }
  }

  Future<void> _loadOlderMessages(
    int conversationId,
    ChatService service,
  ) async {
    setState(() => _isLoadingOlder = true);
    try {
      await Future.wait([
        service.loadOlderMessages(conversationId),
        Future<void>.delayed(const Duration(seconds: 1)),
      ]);
    } finally {
      if (mounted) setState(() => _isLoadingOlder = false);
    }
  }

  @override
  void dispose() {
    final conversation = widget.conversation;
    if (conversation != null) {
      widget.chatService?.sendTypingStop(conversation.id);
      widget.chatService?.closeConversation(conversation.id);
    }
    _typingStopTimer?.cancel();
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _handleTyping(String value) {
    final conversation = widget.conversation;
    final service = widget.chatService;
    if (conversation == null || service == null || value.trim().isEmpty) return;
    final now = DateTime.now();
    if (_lastTypingStart == null ||
        now.difference(_lastTypingStart!) > const Duration(seconds: 2)) {
      _lastTypingStart = now;
      service.sendTypingStart(conversation.id);
    }
    _typingStopTimer?.cancel();
    _typingStopTimer = Timer(
      const Duration(seconds: 3),
      () => service.sendTypingStop(conversation.id),
    );
  }

  void _sendMessage() {
    final text = _controller.text.trim();
    if (text.isEmpty) return;

    final conversation = widget.conversation;
    if (conversation != null && widget.chatService != null) {
      widget.chatService!.sendText(conversation.id, text);
    } else {
      setState(() {
        _messages.add(ChatMessage(text: text, time: 'Vừa xong', isMine: true));
      });
    }
    _controller.clear();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        0.0,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final conversation = widget.conversation;
    final service = widget.chatService;
    if (conversation != null && service != null) {
      return ListenableBuilder(
        listenable: service,
        builder: (context, _) {
          final messages = service.messagesFor(conversation.id);
          return Scaffold(
            backgroundColor: AppColors.background,
            resizeToAvoidBottomInset: true,
            appBar: const OrangeTopBar(title: 'Tin nhắn'),
            body: Column(
              children: [
                _DomainContactHeader(conversation: conversation),
                if (service.typingConversationIds.contains(conversation.id))
                  const Padding(
                    padding: EdgeInsets.only(bottom: AppSpacing.xs),
                    child: Text(
                      'Đang nhập...',
                      style: TextStyle(color: AppColors.muted, fontSize: 12),
                    ),
                  ),
                Expanded(
                  child: ListView.builder(
                    controller: _scrollController,
                    reverse: true,
                    padding: const EdgeInsets.fromLTRB(
                      AppSpacing.lg,
                      AppSpacing.sm,
                      AppSpacing.lg,
                      AppSpacing.lg,
                    ),
                    itemCount: messages.length + (_isLoadingOlder ? 1 : 0),
                    itemBuilder: (context, index) {
                      if (_isLoadingOlder && index == messages.length) {
                        return const Padding(
                          padding: EdgeInsets.symmetric(vertical: 16),
                          child: Center(
                            child: SizedBox(
                              width: 24,
                              height: 24,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: AppColors.fptOrange,
                              ),
                            ),
                          ),
                        );
                      }
                      final reversedIndex = messages.length - 1 - index;
                      return _DomainMessageBubble(
                        message: messages[reversedIndex],
                        onRetry: () => service.retryMessage(
                          messages[reversedIndex].clientMessageId,
                        ),
                      );
                    },
                  ),
                ),
                _MessageComposer(
                  controller: _controller,
                  onSend: _sendMessage,
                  onChanged: _handleTyping,
                ),
              ],
            ),
          );
        },
      );
    }

    final thread = widget.thread!;
    return Scaffold(
      backgroundColor: AppColors.background,
      resizeToAvoidBottomInset: true,
      appBar: const OrangeTopBar(title: 'Tin nhắn'),
      body: Column(
        children: [
          _ContactHeader(thread: thread),
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              reverse: true,
              padding: const EdgeInsets.fromLTRB(
                AppSpacing.lg,
                AppSpacing.sm,
                AppSpacing.lg,
                AppSpacing.lg,
              ),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final reversedIndex = _messages.length - 1 - index;
                return _MessageBubble(
                  message: _messages[reversedIndex],
                  accentColor: thread.accentColor,
                );
              },
            ),
          ),
          _MessageComposer(
            controller: _controller,
            onSend: _sendMessage,
            onChanged: (_) {},
          ),
        ],
      ),
    );
  }
}

class _DomainContactHeader extends StatelessWidget {
  const _DomainContactHeader({required this.conversation});

  final domain.Conversation conversation;

  @override
  Widget build(BuildContext context) {
    final participant = conversation.otherParticipant;
    final name = participant?.name ?? 'Hội thoại #${conversation.id}';
    final subtitle = conversation.isOnline ? 'Đang online' : 'Tin nhắn';
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
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w900,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.muted,
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
                          background: thread.accentColor.withValues(
                            alpha: 0.12,
                          ),
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

class _DomainMessageBubble extends StatelessWidget {
  const _DomainMessageBubble({required this.message, required this.onRetry});

  final domain.ChatMessage message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final isMine = message.isMine;
    final bubbleColor = isMine ? AppColors.fptOrange : AppColors.surface;
    final textColor = isMine ? Colors.white : AppColors.ink;
    final timeColor = isMine
        ? Colors.white.withValues(alpha: 0.78)
        : AppColors.quiet;
    final status = chatStatusLabel(message.status.name);

    return Align(
      alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.sizeOf(context).width * 0.74,
        ),
        margin: const EdgeInsets.only(bottom: AppSpacing.sm),
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.sm,
        ),
        decoration: BoxDecoration(
          color: bubbleColor,
          border: isMine
              ? null
              : Border.all(color: AppColors.line.withValues(alpha: 0.8)),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(18),
            topRight: const Radius.circular(18),
            bottomLeft: Radius.circular(isMine ? 18 : AppRadius.sm),
            bottomRight: Radius.circular(isMine ? AppRadius.sm : 18),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              message.content,
              style: TextStyle(fontSize: 13.5, color: textColor, height: 1.35),
            ),
            if (isMine) ...[
              const SizedBox(height: AppSpacing.xs),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    status,
                    style: TextStyle(
                      fontSize: 10.5,
                      color: timeColor,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  if (message.status == domain.ChatMessageStatus.failed) ...[
                    const SizedBox(width: AppSpacing.xs),
                    GestureDetector(
                      onTap: onRetry,
                      child: Icon(Icons.refresh, color: timeColor, size: 14),
                    ),
                  ],
                ],
              ),
            ],
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
    final timeColor = isMine
        ? Colors.white.withValues(alpha: 0.78)
        : AppColors.quiet;

    return Align(
      alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.sizeOf(context).width * 0.74,
        ),
        margin: const EdgeInsets.only(bottom: AppSpacing.sm),
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.sm,
        ),
        decoration: BoxDecoration(
          color: bubbleColor,
          border: isMine
              ? null
              : Border.all(color: AppColors.line.withValues(alpha: 0.8)),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(18),
            topRight: const Radius.circular(18),
            bottomLeft: Radius.circular(isMine ? 18 : AppRadius.sm),
            bottomRight: Radius.circular(isMine ? AppRadius.sm : 18),
          ),
          boxShadow: [
            BoxShadow(
              color: (isMine ? AppColors.fptOrange : AppColors.ink).withValues(
                alpha: 0.06,
              ),
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
                  Icon(
                    Icons.circle,
                    size: 6,
                    color: accentColor.withValues(alpha: 0.65),
                  ),
                  const SizedBox(width: AppSpacing.xs),
                ],
                Text(
                  message.time,
                  style: TextStyle(
                    fontSize: 10.5,
                    color: timeColor,
                    fontWeight: FontWeight.w600,
                  ),
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
  const _MessageComposer({
    required this.controller,
    required this.onSend,
    required this.onChanged,
  });

  final TextEditingController controller;
  final VoidCallback onSend;
  final ValueChanged<String> onChanged;

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
                onChanged: onChanged,
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
                style: IconButton.styleFrom(
                  backgroundColor: AppColors.fptOrange,
                ),
                onPressed: onSend,
                icon: const Icon(
                  Icons.send_rounded,
                  color: Colors.white,
                  size: 20,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
