class SchoolAnnouncement {
  const SchoolAnnouncement({
    required this.id,
    required this.title,
    required this.body,
    required this.senderName,
    required this.classNames,
    required this.classIds,
    required this.targetRole,
    required this.isRead,
    required this.createdAt,
    required this.academicYearId,
    required this.deliveryStatus,
  });

  final int id;
  final String title;
  final String body;
  final String senderName;
  final List<String> classNames;
  final List<int> classIds;
  final String targetRole;
  final bool isRead;
  final DateTime createdAt;
  final int academicYearId;
  final String deliveryStatus;
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
  });

  final int userId;
  final String userName;
  final String role;
  final List<String> studentNames;
  final List<String> classNames;
  final DateTime? readAt;
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
