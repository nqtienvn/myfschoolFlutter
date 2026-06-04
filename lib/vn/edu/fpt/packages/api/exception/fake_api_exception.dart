class FakeApiException implements Exception {
  final String message;

  const FakeApiException(this.message);

  @override
  String toString() => 'FakeApiException: $message';
}
