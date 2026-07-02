import '../exception/parse_exception.dart';

class SearchResultDto {
  const SearchResultDto({
    required this.id,
    required this.name,
    required this.phone,
    this.avatar,
    required this.role,
  });

  final int id;
  final String name;
  final String phone;
  final String? avatar;
  final String role;

  factory SearchResultDto.fromJson(Map<String, dynamic> json) {
    return SearchResultDto(
      id: requireField<int>(json, 'id'),
      name: requireField<String>(json, 'name'),
      phone: requireField<String>(json, 'phone'),
      avatar: json['avatar'] as String?,
      role: requireField<String>(json, 'role'),
    );
  }
}
