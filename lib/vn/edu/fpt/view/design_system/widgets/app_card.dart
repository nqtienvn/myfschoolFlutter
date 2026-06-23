import 'package:flutter/material.dart';
import '../app_colors.dart';

class AppCard extends StatelessWidget {
  const AppCard({
    super.key,
    required this.child,
    this.padding = 16.0,
    this.borderRadius = 16.0,
    this.gradient,
    this.backgroundColor,
  });

  final Widget child;
  final double padding;
  final double borderRadius;
  final Gradient? gradient;
  final Color? backgroundColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(padding),
      decoration: BoxDecoration(
        color: backgroundColor ?? (gradient == null ? AppColors.surface : null),
        gradient: gradient,
        borderRadius: BorderRadius.circular(borderRadius),
        border: gradient == null && backgroundColor == null
            ? Border.all(color: AppColors.line.withValues(alpha: 0.6), width: 1)
            : null,
        boxShadow: gradient == null
            ? [
                BoxShadow(
                  color: AppColors.ink.withValues(alpha: 0.03),
                  blurRadius: 10,
                  offset: const Offset(0, 4),
                ),
                BoxShadow(
                  color: AppColors.ink.withValues(alpha: 0.02),
                  blurRadius: 20,
                  offset: const Offset(0, 8),
                ),
              ]
            : [
                BoxShadow(
                  color: AppColors.fptOrange.withValues(alpha: 0.15),
                  blurRadius: 16,
                  offset: const Offset(0, 8),
                ),
              ],
      ),
      child: child,
    );
  }
}
