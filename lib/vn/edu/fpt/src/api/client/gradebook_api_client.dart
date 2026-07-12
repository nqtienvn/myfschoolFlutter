import 'backend_api_client.dart';

class GradebookApiClient {
  GradebookApiClient({BackendApiClient? backend})
    : _backend = backend ?? BackendApiClient();
  final BackendApiClient _backend;

  Future<List<Map<String, dynamic>>> getMyAssignments({
    required String token,
    required int academicYearId,
  }) async {
    final data = await _backend.getData(
      '/api/teaching-assignments/mine',
      token: token,
      query: {'academicYearId': '$academicYearId'},
    );
    return (data as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .toList();
  }

  Future<Map<String, dynamic>> getGradeBook({
    required String token,
    required int classId,
    required int subjectId,
    required int semesterId,
  }) async {
    final data = await _backend.getData(
      '/api/grade-books',
      token: token,
      query: {
        'classId': '$classId',
        'subjectId': '$subjectId',
        'semesterId': '$semesterId',
      },
    );
    return data as Map<String, dynamic>;
  }

  Future<List<Map<String, dynamic>>> getStudents({
    required String token,
    required int gradeBookId,
  }) async {
    final data = await _backend.getData(
      '/api/grade-books/$gradeBookId/students',
      token: token,
    );
    return (data as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .toList();
  }

  Future<void> updateScores({
    required String token,
    required int gradeItemId,
    required List<Map<String, dynamic>> entries,
  }) async {
    await _backend.putData(
      '/api/grade-books/scores',
      token: token,
      body: {
        'gradeItemId': gradeItemId,
        'reason': 'Giáo viên nhập điểm trên ứng dụng',
        'entries': entries,
      },
    );
  }

  Future<Map<String, dynamic>> getTranscript({
    required String token,
    required int academicYearId,
    required int semesterId,
    int? studentId,
  }) async {
    final path = studentId == null
        ? '/api/transcripts/me'
        : '/api/transcripts/students/$studentId';
    final data = await _backend.getData(
      path,
      token: token,
      query: {'academicYearId': '$academicYearId', 'semesterId': '$semesterId'},
    );
    return data as Map<String, dynamic>;
  }
}
