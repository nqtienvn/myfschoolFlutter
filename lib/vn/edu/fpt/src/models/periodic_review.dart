class ReviewAssignment {
  const ReviewAssignment({
    required this.academicYearId,
    required this.classId,
    required this.className,
    required this.subjectId,
    required this.subjectName,
  });

  final int academicYearId;
  final int classId;
  final String className;
  final int subjectId;
  final String subjectName;

  factory ReviewAssignment.fromJson(Map<String, dynamic> json) =>
      ReviewAssignment(
        academicYearId: json['academicYearId'] as int,
        classId: json['classId'] as int,
        className: json['className'] as String? ?? '',
        subjectId: json['subjectId'] as int,
        subjectName: json['subjectName'] as String? ?? '',
      );
}

class SubjectPeriodicReview {
  const SubjectPeriodicReview({
    required this.studentId,
    required this.studentName,
    required this.studentCode,
    required this.subjectName,
    required this.subjectTeacherName,
    required this.status,
    this.id,
    this.comment,
    this.strengths,
    this.improvements,
    this.returnReason,
  });

  final int? id;
  final int studentId;
  final String studentName;
  final String studentCode;
  final String subjectName;
  final String subjectTeacherName;
  final String status;
  final String? comment;
  final String? strengths;
  final String? improvements;
  final String? returnReason;

  bool get canEdit => status != 'SUBMITTED';

  factory SubjectPeriodicReview.fromJson(Map<String, dynamic> json) =>
      SubjectPeriodicReview(
        id: json['id'] as int?,
        studentId: json['studentId'] as int,
        studentName: json['studentName'] as String? ?? '',
        studentCode: json['studentCode'] as String? ?? '',
        subjectName: json['subjectName'] as String? ?? '',
        subjectTeacherName: json['subjectTeacherName'] as String? ?? '',
        status: json['status'] as String? ?? 'DRAFT',
        comment: json['comment'] as String?,
        strengths: json['strengths'] as String?,
        improvements: json['improvements'] as String?,
        returnReason: json['returnReason'] as String?,
      );
}

class StudentPeriodicReport {
  const StudentPeriodicReport({
    required this.academicYearId,
    required this.semesterId,
    required this.classId,
    required this.className,
    required this.studentId,
    required this.studentName,
    required this.studentCode,
    required this.status,
    required this.submittedSubjects,
    required this.totalSubjects,
    required this.missingSubjects,
    required this.subjectReviews,
    this.id,
    this.homeroomTeacherName,
    this.generalComment,
    this.conduct,
    this.suggestedConduct,
  });

  final int? id;
  final int academicYearId;
  final int semesterId;
  final int classId;
  final String className;
  final int studentId;
  final String studentName;
  final String studentCode;
  final String? homeroomTeacherName;
  final String? generalComment;
  final String? conduct;
  final String? suggestedConduct;
  final String status;
  final int submittedSubjects;
  final int totalSubjects;
  final List<String> missingSubjects;
  final List<SubjectPeriodicReview> subjectReviews;

  bool get isPublished => status == 'PUBLISHED';
  bool get isSubmitted => status == 'SUBMITTED' || isPublished;
  bool get subjectsComplete =>
      totalSubjects > 0 && submittedSubjects == totalSubjects;

  factory StudentPeriodicReport.fromJson(Map<String, dynamic> json) =>
      StudentPeriodicReport(
        id: json['id'] as int?,
        academicYearId: json['academicYearId'] as int,
        semesterId: json['semesterId'] as int,
        classId: json['classId'] as int,
        className: json['className'] as String? ?? '',
        studentId: json['studentId'] as int,
        studentName: json['studentName'] as String? ?? '',
        studentCode: json['studentCode'] as String? ?? '',
        homeroomTeacherName: json['homeroomTeacherName'] as String?,
        generalComment: json['generalComment'] as String?,
        conduct: json['conduct'] as String?,
        suggestedConduct: json['suggestedConduct'] as String?,
        status: json['status'] as String? ?? 'DRAFT',
        submittedSubjects: json['submittedSubjects'] as int? ?? 0,
        totalSubjects: json['totalSubjects'] as int? ?? 0,
        missingSubjects: (json['missingSubjects'] as List<dynamic>? ?? const [])
            .whereType<String>()
            .toList(growable: false),
        subjectReviews: (json['subjectReviews'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(SubjectPeriodicReview.fromJson)
            .toList(growable: false),
      );
}
