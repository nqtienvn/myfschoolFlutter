package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.PeriodicReportStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.List;

public interface PeriodicReviewService {
    List<ReviewAssignmentDto> getAssignments(Long academicYearId, Long teacherUserId);
    List<SubjectReviewDto> getSubjectReviews(Long academicYearId, Long semesterId, Long classId,
                                             Long subjectId, Long teacherUserId);
    SubjectReviewDto saveSubjectReview(Long studentId, SaveSubjectReviewRequest request, Long teacherUserId);
    List<SubjectReviewDto> submitSubjectReviews(SubmitSubjectReviewsRequest request, Long teacherUserId);
    SubjectReviewDto returnSubjectReview(Long reviewId, ReturnSubjectReviewRequest request, Long teacherUserId);

    List<HomeroomReportDto> getHomeroomReports(Long academicYearId, Long semesterId, Long classId,
                                               Long teacherUserId);
    HomeroomReportDto getHomeroomReport(Long studentId, Long academicYearId, Long semesterId,
                                        Long classId, Long teacherUserId);
    HomeroomReportDto saveHomeroomReport(Long studentId, SavePeriodicReportRequest request, Long teacherUserId);
    HomeroomReportDto publishStudent(Long studentId, ReportScopeRequest request, Long teacherUserId);
    List<HomeroomReportDto> publishClass(ReportScopeRequest request, Long teacherUserId);

    HomeroomReportDto getPublishedReport(Long studentId, Long academicYearId, Long semesterId,
                                         Long requesterId, UserRole requesterRole);
    List<HomeroomReportDto> getAdminReports(Long academicYearId, Long semesterId, Long classId,
                                            PeriodicReportStatus status);
    HomeroomReportDto reopenReport(Long reportId, ReopenPeriodicReportRequest request, Long adminUserId);
}
