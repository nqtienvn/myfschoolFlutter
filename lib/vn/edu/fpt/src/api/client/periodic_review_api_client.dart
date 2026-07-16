import '../../models/periodic_review.dart';
import 'backend_api_client.dart';

abstract class PeriodicReviewApi {
  Future<List<ReviewAssignment>> getAssignments({
    required String token,
    required int academicYearId,
  });
  Future<List<SubjectPeriodicReview>> getSubjectReviews({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required int subjectId,
  });
  Future<SubjectPeriodicReview> saveSubjectReview({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required int subjectId,
    required String comment,
    required String strengths,
    required String improvements,
  });
  Future<void> submitSubjectReview({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required int subjectId,
  });
  Future<List<StudentPeriodicReport>> getHomeroomReports({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
  });
  Future<StudentPeriodicReport> saveHomeroomReport({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required String generalComment,
    required String conduct,
  });
  Future<StudentPeriodicReport> submitHomeroomReport({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
  });
  Future<void> submitHomeroomClass({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
  });
  Future<StudentPeriodicReport> getPublishedReport({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
  });
  Future<int> resolveStudentId({required String token});
}

class PeriodicReviewApiClient implements PeriodicReviewApi {
  PeriodicReviewApiClient({BackendApiClient? backend})
    : _backend = backend ?? BackendApiClient();
  final BackendApiClient _backend;

  @override
  Future<List<ReviewAssignment>> getAssignments({
    required String token,
    required int academicYearId,
  }) async {
    final data = await _backend.getData(
      '/api/subject-reviews/assignments',
      token: token,
      query: {'academicYearId': '$academicYearId'},
    );
    return _list(data).map(ReviewAssignment.fromJson).toList(growable: false);
  }

  @override
  Future<List<SubjectPeriodicReview>> getSubjectReviews({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required int subjectId,
  }) async {
    final data = await _backend.getData(
      '/api/subject-reviews',
      token: token,
      query: {
        'academicYearId': '$academicYearId',
        'semesterId': '$semesterId',
        'classId': '$classId',
        'subjectId': '$subjectId',
      },
    );
    return _list(
      data,
    ).map(SubjectPeriodicReview.fromJson).toList(growable: false);
  }

  @override
  Future<SubjectPeriodicReview> saveSubjectReview({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required int subjectId,
    required String comment,
    required String strengths,
    required String improvements,
  }) async {
    final data = await _backend.putData(
      '/api/subject-reviews/$studentId',
      token: token,
      body: {
        'academicYearId': academicYearId,
        'semesterId': semesterId,
        'classId': classId,
        'subjectId': subjectId,
        'comment': comment,
        'strengths': strengths,
        'improvements': improvements,
      },
    );
    return SubjectPeriodicReview.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<void> submitSubjectReview({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required int subjectId,
  }) async {
    await _backend.postData(
      '/api/subject-reviews/submit',
      token: token,
      body: {
        'academicYearId': academicYearId,
        'semesterId': semesterId,
        'classId': classId,
        'subjectId': subjectId,
        'studentIds': [studentId],
      },
    );
  }

  @override
  Future<List<StudentPeriodicReport>> getHomeroomReports({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
  }) async {
    final data = await _backend.getData(
      '/api/homeroom-reports',
      token: token,
      query: {
        'academicYearId': '$academicYearId',
        'semesterId': '$semesterId',
        'classId': '$classId',
      },
    );
    return _list(
      data,
    ).map(StudentPeriodicReport.fromJson).toList(growable: false);
  }

  @override
  Future<StudentPeriodicReport> saveHomeroomReport({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
    required String generalComment,
    required String conduct,
  }) async {
    final data = await _backend.putData(
      '/api/homeroom-reports/students/$studentId',
      token: token,
      body: {
        'academicYearId': academicYearId,
        'semesterId': semesterId,
        'classId': classId,
        'generalComment': generalComment,
      },
    );
    return StudentPeriodicReport.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<StudentPeriodicReport> submitHomeroomReport({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
    required int classId,
  }) async {
    final data = await _backend.postData(
      '/api/homeroom-reports/students/$studentId/submit',
      token: token,
      body: {
        'academicYearId': academicYearId,
        'semesterId': semesterId,
        'classId': classId,
      },
    );
    return StudentPeriodicReport.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<void> submitHomeroomClass({
    required String token,
    required int academicYearId,
    required int semesterId,
    required int classId,
  }) async {
    await _backend.postData(
      '/api/homeroom-reports/submit-class',
      token: token,
      body: {
        'academicYearId': academicYearId,
        'semesterId': semesterId,
        'classId': classId,
      },
    );
  }

  @override
  Future<StudentPeriodicReport> getPublishedReport({
    required String token,
    required int studentId,
    required int academicYearId,
    required int semesterId,
  }) async {
    final data = await _backend.getData(
      '/api/periodic-reports/students/$studentId',
      token: token,
      query: {'academicYearId': '$academicYearId', 'semesterId': '$semesterId'},
    );
    return StudentPeriodicReport.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<int> resolveStudentId({required String token}) async {
    final data =
        await _backend.getData('/api/user/profile', token: token)
            as Map<String, dynamic>;
    return (data['studentProfile'] as Map<String, dynamic>)['id'] as int;
  }

  Iterable<Map<String, dynamic>> _list(Object? data) =>
      (data as List<dynamic>? ?? const []).whereType<Map<String, dynamic>>();
}
