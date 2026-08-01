import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'app_keys.dart';

Widget _navButton(String title, double fontSize, VoidCallback onPressed) {
  return TextButton(
    onPressed: onPressed,
    style: TextButton.styleFrom(
      foregroundColor: const Color(0xFFF27A9D),
      padding: const EdgeInsets.symmetric(horizontal: 12),
      textStyle: GoogleFonts.inter(
        fontSize: fontSize,
        fontWeight: FontWeight.w500,
      ),
    ),
    child: Text(title),
  );
}

Widget buildHeader(BuildContext context, double screenWidth) {
  final font24 = responsiveFont(24, screenWidth);
  final font14 = responsiveFont(14, screenWidth);

  return ClipRRect(
    borderRadius: BorderRadius.circular(20),
    child: BackdropFilter(
      filter: ImageFilter.blur(sigmaX: 8, sigmaY: 8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 20.0),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.05),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: Colors.white.withOpacity(0.1),
            width: 1,
          ),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Flexible(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.centerLeft,
                child: InkWell(
                  onTap: () {
                    if (ModalRoute.of(context)?.settings.name != '/') {
                      Navigator.popUntil(context, (route) => route.isFirst);
                      Future.delayed(const Duration(milliseconds: 100), () {
                        if (AppKeys.landingPageKey.currentContext != null) {
                          Scrollable.ensureVisible(
                            AppKeys.landingPageKey.currentContext!,
                            duration: const Duration(milliseconds: 500),
                            curve: Curves.easeInOut,
                          );
                        }
                      });
                    } else {
                      if (AppKeys.landingPageKey.currentContext != null) {
                        Scrollable.ensureVisible(
                          AppKeys.landingPageKey.currentContext!,
                          duration: const Duration(milliseconds: 500),
                          curve: Curves.easeInOut,
                        );
                      }
                    }
                  },
                  child: Row(
                    children: [
                      Image.asset('assets/LogoSafeScan.png', height: 32),
                      const SizedBox(width: 12),
                      Text(
                        'SafeScan',
                        style: GoogleFonts.inter(
                          color: const Color(0xFFF27A9D),
                          fontSize: font24,
                          fontWeight: FontWeight.bold,
                          letterSpacing: -0.5,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            Flexible(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                child: Row(
                  children: [
                    _navButton('About Us', font14, () {
                      if (ModalRoute.of(context)?.settings.name != '/') {
                        Navigator.popUntil(context, (route) => route.isFirst);
                        Future.delayed(const Duration(milliseconds: 100), () {
                          if (AppKeys.aboutUsKey.currentContext != null) {
                            Scrollable.ensureVisible(
                              AppKeys.aboutUsKey.currentContext!,
                              duration: const Duration(milliseconds: 500),
                              curve: Curves.easeInOut,
                            );
                          }
                        });
                      } else {
                        if (AppKeys.aboutUsKey.currentContext != null) {
                          Scrollable.ensureVisible(
                            AppKeys.aboutUsKey.currentContext!,
                            duration: const Duration(milliseconds: 500),
                            curve: Curves.easeInOut,
                          );
                        }
                      }
                    }),
                    const SizedBox(width: 12),
                    _navButton('Contact Us', font14, () {
                      Navigator.pushNamed(context, '/contactus');
                    }),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

double responsiveFont(double size, double screenWidth) {
  final scale = screenWidth / 375;
  return (size * scale).clamp(size * 0.8, size * 1.2);
}
