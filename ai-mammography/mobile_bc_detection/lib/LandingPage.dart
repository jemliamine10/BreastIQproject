import 'dart:math';
import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'header.dart';

import 'app_keys.dart';

class LandingPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final screenHeight = MediaQuery.of(context).size.height;

    // Responsive font with tighter clamp for larger screens
    double responsiveFont(double size) {
      final scale = screenWidth / 375; // base: mobile
      final scaled = size * scale;
      // clamp between 0.8x and 1.2x
      return scaled.clamp(size * 0.8, size * 1.2);
    }

    // Helper sizes
    double horizontalPadding = min(24.0, screenWidth * 0.05);
    double verticalPadding = min(16.0, screenHeight * 0.02);

    // Determine max image box size
    final maxImageSize = min(screenWidth * 0.8, 300.0);

    return Scaffold(
      body: Stack(
        children: [
          // Background
          Positioned.fill(
            child: Image.asset('assets/bg2.jpg', fit: BoxFit.cover),
          ),
          Positioned.fill(
            child: Container(color: const Color.fromARGB(150, 42, 14, 24)),
          ),

          // Content
          SafeArea(
            child: SingleChildScrollView(
              primary: true,
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: screenHeight),
                child: Padding(
                  padding: EdgeInsets.symmetric(
                    horizontal: horizontalPadding,
                    vertical: verticalPadding,
                  ),
                  child: Column(
                    key: AppKeys.landingPageKey,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Top bar
                      buildHeader(context, screenWidth),

                      SizedBox(height: responsiveFont(16)),

                      // Image container with max size
                      Center(
                        child: TweenAnimationBuilder(
                          tween: Tween<double>(begin: 0, end: 1),
                          duration: const Duration(milliseconds: 1500),
                          curve: Curves.easeInOutSine,
                          builder: (context, double value, child) {
                            return Transform.translate(
                              offset: Offset(
                                  0,
                                  10 *
                                      sin(value * 2 * pi +
                                          DateTime.now()
                                                  .millisecondsSinceEpoch /
                                              1000)),
                              child: child,
                            );
                          },
                          child: StatefulBuilder(
                            builder: (context, setState) {
                              // We use a local timer to drive the animation if needed,
                              // but for simplicity in a stateless widget, we can use a simpler approach.
                              return Container(
                                margin:
                                    EdgeInsets.only(top: responsiveFont(60)),
                                width: maxImageSize,
                                height: maxImageSize,
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(32),
                                  image: const DecorationImage(
                                    image: AssetImage('assets/mammo.png'),
                                    fit: BoxFit.cover,
                                  ),
                                  border: Border.all(
                                    color: const Color(0xFFF27A9D)
                                        .withOpacity(0.5),
                                    width: 2,
                                  ),
                                  boxShadow: [
                                    BoxShadow(
                                      color: const Color(0xFFF27A9D)
                                          .withOpacity(0.2),
                                      blurRadius: 20,
                                      spreadRadius: 5,
                                    ),
                                    BoxShadow(
                                      color: Colors.black.withOpacity(0.4),
                                      blurRadius: 15,
                                      offset: const Offset(0, 10),
                                    ),
                                  ],
                                ),
                                child: Stack(
                                  children: [
                                    Align(
                                      alignment: Alignment.topRight,
                                      child: Container(
                                        margin: EdgeInsets.only(
                                          top: maxImageSize * 0.2,
                                          right: 0,
                                        ),
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 16,
                                          vertical: 8,
                                        ),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFFF27A9D)
                                              .withOpacity(0.8),
                                          borderRadius:
                                              const BorderRadius.horizontal(
                                            left: Radius.circular(12),
                                          ),
                                          boxShadow: [
                                            BoxShadow(
                                              color:
                                                  Colors.black.withOpacity(0.2),
                                              blurRadius: 4,
                                              offset: const Offset(-2, 2),
                                            ),
                                          ],
                                        ),
                                        child: Text(
                                          'AI ANALYSIS',
                                          style: GoogleFonts.inter(
                                            color: Colors.white,
                                            fontWeight: FontWeight.bold,
                                            fontSize: responsiveFont(12),
                                            letterSpacing: 1.5,
                                          ),
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              );
                            },
                          ),
                        ),
                      ),

                      SizedBox(height: responsiveFont(24)),

                      // Title
                      Center(
                        child: Text(
                          'Empowering Early Detection\nwith AI',
                          textAlign: TextAlign.center,
                          style: GoogleFonts.inter(
                            color: const Color(0xFFF27A9D),
                            fontSize: responsiveFont(24),
                            fontWeight: FontWeight.bold,
                            shadows: [
                              Shadow(
                                color: Colors.black.withOpacity(0.5),
                                blurRadius: 4,
                                offset: const Offset(1, 1),
                              ),
                            ],
                          ),
                        ),
                      ),

                      SizedBox(height: responsiveFont(16)),

                      // Subtitle
                      Center(
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16.0),
                          child: Text(
                            'SafeScan helps you detect potential breast cancer signs using AI-powered analysis of mammogram images.',
                            textAlign: TextAlign.center,
                            style: GoogleFonts.inter(
                              color: Colors.white,
                              fontSize: responsiveFont(14),
                              height: 1.5,
                              shadows: [
                                Shadow(
                                  color: Colors.black.withOpacity(0.5),
                                  blurRadius: 4,
                                  offset: const Offset(1, 1),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),

                      SizedBox(height: responsiveFont(32)),

                      // Get Started button

                      Center(
                        child: Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(20),
                            boxShadow: [
                              BoxShadow(
                                color: const Color(0xFFA2314E).withOpacity(0.4),
                                blurRadius: 20,
                                offset: const Offset(0, 10),
                              ),
                            ],
                            gradient: const LinearGradient(
                              colors: [Color(0xFFA2314E), Color(0xFFD16D91)],
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                            ),
                          ),
                          child: ElevatedButton(
                            onPressed: () =>
                                Navigator.pushNamed(context, '/UploadPage'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.transparent,
                              foregroundColor: Colors.white,
                              shadowColor: Colors.transparent,
                              padding: EdgeInsets.symmetric(
                                horizontal: responsiveFont(50),
                                vertical: responsiveFont(18),
                              ),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(20),
                              ),
                            ),
                            child: Text(
                              'Get Started',
                              style: GoogleFonts.inter(
                                color: Colors.white,
                                fontSize: responsiveFont(18),
                                fontWeight: FontWeight.bold,
                                letterSpacing: 0.5,
                              ),
                            ),
                          ),
                        ),
                      ),

                      // About Us Section
                      SizedBox(height: responsiveFont(60)),
                      Center(
                        child: ConstrainedBox(
                          constraints: BoxConstraints(
                            maxWidth: screenWidth > 1100
                                ? 900
                                : screenWidth > 800
                                    ? 750
                                    : screenWidth > 750
                                        ? 700
                                        : double.infinity,
                          ),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(30),
                            child: BackdropFilter(
                              filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                              child: Container(
                                key: AppKeys.aboutUsKey,
                                padding: EdgeInsets.symmetric(
                                  horizontal: horizontalPadding * 1.5,
                                  vertical: responsiveFont(35),
                                ),
                                decoration: BoxDecoration(
                                  color: Colors.white.withOpacity(0.05),
                                  borderRadius: BorderRadius.circular(30),
                                  border: Border.all(
                                    color: Colors.white.withOpacity(0.1),
                                    width: 1.5,
                                  ),
                                  gradient: LinearGradient(
                                    begin: Alignment.topLeft,
                                    end: Alignment.bottomRight,
                                    colors: [
                                      Colors.white.withOpacity(0.1),
                                      Colors.white.withOpacity(0.02),
                                    ],
                                  ),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    // Section Title with indicator
                                    Row(
                                      children: [
                                        Container(
                                          width: 4,
                                          height: 24,
                                          decoration: BoxDecoration(
                                            color: const Color(0xFFF27A9D),
                                            borderRadius:
                                                BorderRadius.circular(2),
                                          ),
                                        ),
                                        const SizedBox(width: 12),
                                        Text(
                                          'About SafeScan',
                                          style: GoogleFonts.inter(
                                            color: const Color(0xFFF27A9D),
                                            fontSize: responsiveFont(22),
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                      ],
                                    ),
                                    SizedBox(height: responsiveFont(24)),
                                    // Content
                                    Text(
                                      'At SafeScan, we are dedicated to revolutionizing breast cancer detection through advanced technology and clinical insight. Our mission is to empower radiologists with intelligent tools that assist in the early detection. We combine cutting-edge AI algorithms with a user-friendly interface to support healthcare professionals in identifying potential malignancies with greater precision and confidence.\n\nTogether, we believe technology and medicine can save lives—one mammogram at a time.',
                                      style: GoogleFonts.inter(
                                        color: Colors.white.withOpacity(0.9),
                                        fontSize: responsiveFont(15),
                                        height: 1.8,
                                        fontWeight: FontWeight.w300,
                                      ),
                                    ),
                                    SizedBox(height: responsiveFont(24)),
                                    // Decorative elements
                                    Row(
                                      mainAxisAlignment: MainAxisAlignment.end,
                                      children: [
                                        _buildPinkDot(),
                                        const SizedBox(width: 8),
                                        _buildPinkDot(),
                                        const SizedBox(width: 8),
                                        _buildPinkDot(),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                      SizedBox(height: responsiveFont(40)),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _navButton(String title, double fontSize, {VoidCallback? onPressed}) {
    return TextButton(
      onPressed: onPressed,
      child: Text(
        title,
        style: GoogleFonts.inter(
          color: const Color(0xFFF27A9D),
          fontSize: fontSize,
          fontWeight: FontWeight.w500,
          shadows: [
            Shadow(
              color: Colors.black.withOpacity(0.5),
              blurRadius: 4,
              offset: const Offset(1, 1),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPinkDot() {
    return Container(
      width: 8,
      height: 8,
      decoration: BoxDecoration(
        color: const Color(0xFFF27A9D),
        shape: BoxShape.circle,
        boxShadow: [
          BoxShadow(
            color: const Color(0xFFF27A9D).withOpacity(0.7),
            blurRadius: 4,
            spreadRadius: 1,
          ),
        ],
      ),
    );
  }
}
