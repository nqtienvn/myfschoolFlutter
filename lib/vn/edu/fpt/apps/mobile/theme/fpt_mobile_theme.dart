import 'package:flutter/material.dart';

class FptMobileColors {
  static const orange = Color(0xFFF36F21);
  static const burntOrange = Color(0xFFA95F16);
  static const background = Color(0xFFFFFAFF);
  static const softPeach = Color(0xFFFFE8D8);
  static const softSurface = Color(0xFFF5F3F8);
  static const text = Color(0xFF1F2933);
  static const mutedText = Color(0xFF747982);
  static const line = Color(0xFFF0E4DD);
  static const danger = Color(0xFFE02424);
}

class FptMobileTheme {
  static ThemeData build() {
    return ThemeData(
      useMaterial3: true,
      scaffoldBackgroundColor: FptMobileColors.background,
      colorScheme: const ColorScheme.light(
        primary: FptMobileColors.orange,
        surface: Colors.white,
        onSurface: FptMobileColors.text,
      ),
      fontFamily: 'Roboto',
      textTheme: const TextTheme(
        titleLarge: TextStyle(
          color: FptMobileColors.text,
          fontSize: 20,
          fontWeight: FontWeight.w800,
        ),
        titleMedium: TextStyle(
          color: FptMobileColors.text,
          fontSize: 16,
          fontWeight: FontWeight.w800,
        ),
        bodyMedium: TextStyle(
          color: FptMobileColors.text,
          fontSize: 14,
          fontWeight: FontWeight.w500,
        ),
        bodySmall: TextStyle(
          color: FptMobileColors.mutedText,
          fontSize: 12,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
