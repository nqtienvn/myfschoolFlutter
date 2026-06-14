import 'package:flutter/material.dart';

void main() {
  runApp(const CounterDemo());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(home: Text('Hello Flutter'));
  }
}

class InfoCard extends StatelessWidget {
  final String title;
  final String content;

  const InfoCard({super.key, required this.title, required this.content});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: Center(
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text('$title\n$content'),
            ),
          ),
        ),
      ),
      debugShowCheckedModeBanner: false,
    );
  }
}

