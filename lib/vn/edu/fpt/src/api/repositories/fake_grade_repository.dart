import '../../core/models/grade.dart';
import '../client/fake_api.dart';
import '../dto/grade_dto.dart';

class FakeGradeRepository {
  final FakeApiClient apiClient;

  FakeGradeRepository(this.apiClient);

  Future<List<Grade>> getGrades({
    required int studentId,
  }) async {
    final Map<String, dynamic> response = await apiClient.getGradesResponse(
      studentId: studentId,
    );

    final data = response['data'] as Map<String, dynamic>;
    final items = data['items'] as List<dynamic>;

    return items
        .map((item) => GradeDto.fromJson(
      item as Map<String, dynamic>,
    ).toDomain())
        .toList();
  }
}
