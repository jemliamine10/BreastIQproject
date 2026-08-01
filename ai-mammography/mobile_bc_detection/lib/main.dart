import 'dart:ui';
import 'package:flutter/material.dart';
import 'ContactUs.dart';
import 'UploadPage.dart';
import 'LandingPage.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

// main.dart
import 'app_keys.dart';
Future<void> main() async {
  await dotenv.load(fileName: ".env");
  runApp(MyApp());
}
class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      initialRoute: '/',
      routes: {
        '/': (context) => LandingPage(),
        '/UploadPage': (context) => UploadHome(),
        '/contactus':(context)=>ContactUs(),
      },
    );
  }
}