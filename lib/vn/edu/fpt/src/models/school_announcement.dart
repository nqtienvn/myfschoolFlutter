class SchoolAnnouncement {
  const SchoolAnnouncement({
    required this.id,
    required this.title,
    required this.body,
    required this.senderName,
    required this.classNames,
    required this.classIds,
    required this.targetRole,
    required this.requiresReply,
    required this.isRead,
    required this.acknowledged,
    required this.totalRecipients,
    required this.readCount,
    required this.acknowledgedCount,
    required this.repliedCount,
    required this.createdAt,
    required this.academicYearId,
    required this.approvalStatus,
    this.replyText,
    this.repliedAt,
    this.recipientStatus,
  });

  final int id;
  final String title;
  final String body;
  final String senderName;
  final List<String> classNames;
  final List<int> classIds;
  final String targetRole;
  final bool requiresReply;
  final bool isRead;
  final bool acknowledged;
  final String? replyText;
  final DateTime? repliedAt;
  final String? recipientStatus;
  final int totalRecipients;
  final int readCount;
  final int acknowledgedCount;
  final int repliedCount;
  final DateTime createdAt;
  final int academicYearId;
  final String approvalStatus;
}

class AnnouncementRecipient {
  const AnnouncementRecipient({
    required this.userId,
    required this.userName,
    required this.role,
    required this.studentNames,
    required this.classNames,
    required this.status,
    this.readAt,
    this.acknowledgedAt,
    this.replyText,
    this.repliedAt,
  });

  final int userId;
  final String userName;
  final String role;
  final List<String> studentNames;
  final List<String> classNames;
  final DateTime? readAt;
  final DateTime? acknowledgedAt;
  final String? replyText;
  final DateTime? repliedAt;
  final String status;
}

class AnnouncementRecipientPage {
  const AnnouncementRecipientPage({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.page,
  });

  final List<AnnouncementRecipient> content;
  final int totalElements;
  final int totalPages;
  final int page;
}
