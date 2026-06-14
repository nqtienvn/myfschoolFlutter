class ParseException implements Exception {
  final String message;

  const ParseException(this.message);

  @override
  String toString() => 'ParseException: $message';
}

T requireField<T>(Map<String, dynamic> json, String key) {
  final value = json[key];

  if (value is! T) {
    throw ParseException(
      'Field "$key" must is type $T but received ${value.runtimeType}.',
    );
  }
  return value;
}
