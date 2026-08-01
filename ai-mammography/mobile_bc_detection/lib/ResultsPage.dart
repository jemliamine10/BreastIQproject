import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:mobile_bc_detection/conclusion.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:pdf/pdf.dart';
import 'package:printing/printing.dart';
import 'package:intl/intl.dart';
import 'package:universal_html/html.dart' as html;
import 'header.dart';
import 'uploadpage.dart';
import 'image_viewer.dart';
import 'package:http/http.dart';
import 'generatepdf.dart';

class ResultsPage extends StatefulWidget {
  final Map<String, dynamic> result;
  final Uint8List originalImageBytes;
  final bool hasDetections;

  const ResultsPage({
    super.key,
    required this.result,
    required this.originalImageBytes,
    required this.hasDetections,
  });

  @override
  _ResultsPageState createState() => _ResultsPageState();
}

class _ResultsPageState extends State<ResultsPage>
    with SingleTickerProviderStateMixin {
  final ValueNotifier<bool> isZoomEnabled = ValueNotifier<bool>(false);
  late final AnimationController _animationController;
  bool _showOverlay = false;
  bool _isGenerating = false;
  String messageContent = ""; // Store API response here
  String req = "";

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1000),
    );
    fetchConclusion();
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  void _toggleOverlay() {
    setState(() {
      _showOverlay = !_showOverlay;
      _showOverlay
          ? _animationController.forward()
          : _animationController.reverse();
    });
  }

  String extractConclusion(String apiResponse) {
    // Trim the input first
    final response = apiResponse.trim();
    if (response.isEmpty) return response;

    // Define possible conclusion markers
    const markers = [
      '**Conclusion:**',
      '**Conclusion**',
      'Conclusion:',
      'Conclusion'
    ];

    // Find the first matching marker
    String? matchedMarker;
    int? markerPosition;

    for (final marker in markers) {
      final pos = response.indexOf(marker);
      if (pos != -1) {
        matchedMarker = marker;
        markerPosition = pos;
        break;
      }
    }

    // If no marker found, return the whole response
    if (matchedMarker == null) return response;

    // Extract text after the marker
    final textAfterMarker =
        response.substring(markerPosition! + matchedMarker.length).trim();

    // Find where the next section starts (either new header or double newline)
    int endOfParagraph = textAfterMarker.length;

    // Look for next header marker
    final nextHeaderMatch =
        RegExp(r'\*\*.+?\*\*:?').firstMatch(textAfterMarker);
    if (nextHeaderMatch != null) {
      endOfParagraph = nextHeaderMatch.start;
    }

    // If no next header, look for first double newline
    final doubleNewlinePos = textAfterMarker.indexOf('\n\n');
    if (doubleNewlinePos != -1 && doubleNewlinePos < endOfParagraph) {
      endOfParagraph = doubleNewlinePos;
    }

    // Extract and return the first paragraph
    return textAfterMarker.substring(0, endOfParagraph).trim();
  }

  Future<void> fetchConclusion() async {
    final backendUrl = dotenv.env['BACKEND_URL'];

    String prompt;

    if (!widget.hasDetections) {
      prompt =
          "Generate a professional and short medical report conclusion stating that no abnormalities were detected in the mammogram. by the way the abormalities can be  mass and/or calcification";
    } else {
      String findings = "";

      for (var item in widget.result['individual_predictions']) {
        final features = item['features'];
        findings +=
            "The ${item['label']} has an opacity score of ${(item['score'] * 100).toStringAsFixed(1)}% "
            "and is classified as ${item['classification']}. Its features include "
            "an area of ${features['morphology']['area_mm2'].toStringAsFixed(2)} mm², "
            "a circularity of ${features['morphology']['circularity'].toStringAsFixed(2)}, "
            "an intensity of ${features['intensity']['mean'].toStringAsFixed(2)}, "
            "and a texture score of ${features['texture']['glcm_homogeneity'].toStringAsFixed(2)}. ";
      }

      prompt =
          "Generate a professional and short medical report conclusion summarizing the following "
          "findings without repeating the characteristics: Mammography report shows: $findings "
          "Focus on the clinical interpretation, not the numbers. maximum 3 sentences.";
    }

    if (backendUrl == null || backendUrl.isEmpty) {
      debugPrint("❌ Error: BACKEND_URL is not defined in .env");
      setState(() {
        messageContent = "Error: Backend URL not configured.";
      });
      return;
    }

    final fullUrl = "$backendUrl/conclusion";
    debugPrint("📡 Calling Backend: $fullUrl");

    try {
      final response = await post(
        Uri.parse(fullUrl),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({"prompt": prompt}),
      );
      if (response.statusCode == 200) {
        final responseData = json.decode(response.body);
        final conclusionText = responseData['conclusion'];
        setState(() {
          this.messageContent = extractConclusion(conclusionText);
        });
      } else {
        setState(() {
          messageContent =
              "Error: Backend failed to provide conclusion (Status ${response.statusCode})";
        });
        print(
            "Failed to fetch data from API. Status Code: ${response.statusCode}");
      }
    } catch (e) {
      setState(() {
        messageContent = "Error: ${e.toString()}";
      });
      print("Exception occurred: $e");
    }
  }

  Future<void> _generateAndDownloadPdf(name) async {
    setState(() => _isGenerating = true);

    try {
      final pdfBytes = await GeneratePdf.generate(
        result: widget.result,
        originalImageBytes: widget.originalImageBytes,
        hasDetections: widget.hasDetections,
        conclusion: messageContent,
        patientName: name,
      );

      if (kIsWeb) {
        final blob = html.Blob([pdfBytes]);
        final url = html.Url.createObjectUrlFromBlob(blob);
        html.AnchorElement(href: url)
          ..setAttribute('download', 'mammography_report.pdf')
          ..click();
        html.Url.revokeObjectUrl(url);
      } else {
        await Printing.sharePdf(
            bytes: pdfBytes, filename: 'mammography_report.pdf');
      }

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('PDF ready!'),
          backgroundColor: Color.fromARGB(255, 0, 0, 0),
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error generating PDF: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      setState(() => _isGenerating = false);
    }
  }

  void _handleConclusionConfirmed(String newText) {
    setState(() {
      messageContent = newText;
    });
  }

  Widget _buildBackButton(double screenWidth) {
    return Container(
      width: 130,
      margin: const EdgeInsets.only(right: 2),
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color.fromARGB(34, 0, 0, 0),
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.only(
              topLeft: Radius.circular(20),
              bottomLeft: Radius.circular(20),
            ),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          foregroundColor: Colors.white,
          minimumSize: const Size(0, 40),
        ),
        onPressed: () => Navigator.pushReplacement(
          context,
          MaterialPageRoute(builder: (_) => UploadHome()),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.refresh, size: 18),
            const SizedBox(width: 6),
            Flexible(
              child: Text(
                'Upload More',
                style: const TextStyle(fontSize: 14),
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }

  final TextEditingController _nameController = TextEditingController();
  void _showPatientNameDialog() {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor:
              const Color.fromARGB(255, 30, 30, 30), // Dark background
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20), // Rounded corners
          ),
          title: const Text(
            'Enter Patient Name',
            style: TextStyle(color: Colors.white),
          ),
          content: TextField(
            controller: _nameController,
            style: const TextStyle(color: Colors.white),
            decoration: const InputDecoration(
              hintText: 'Patient Name',
              hintStyle: TextStyle(color: Colors.white70),
              enabledBorder: UnderlineInputBorder(
                borderSide: BorderSide(color: Colors.white54),
              ),
              focusedBorder: UnderlineInputBorder(
                borderSide: BorderSide(color: Colors.white),
              ),
            ),
          ),
          actions: [
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color.fromARGB(
                    35, 0, 0, 0), // Same as download button
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color.fromARGB(
                    35, 0, 0, 0), // Same as download button
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              onPressed: () {
                final name = _nameController.text.trim();
                if (name.isNotEmpty) {
                  Navigator.of(context).pop();
                  _generateAndDownloadPdf(name);
                }
              },
              child: const Text('Submit'),
            ),
          ],
        );
      },
    );
  }

  Widget _buildDownloadButton(double screenWidth) {
    return Container(
      width: 130,
      margin: const EdgeInsets.only(left: 2),
      child: ElevatedButton.icon(
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color.fromARGB(35, 0, 0, 0),
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.only(
              topRight: Radius.circular(20),
              bottomRight: Radius.circular(20),
            ),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
          foregroundColor: Colors.white,
          minimumSize: const Size(0, 40),
        ),
        onPressed: _showPatientNameDialog,
        icon: const Icon(Icons.download, size: 18),
        label: const Text('Report PDF', style: TextStyle(fontSize: 14)),
      ),
    );
  }

  Widget _buildImageViewer(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final screenHeight = MediaQuery.of(context).size.height;
    final maxW = screenWidth * 0.9;
    final maxH = screenHeight * 0.9;

    return Center(
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxW, maxHeight: maxH),
        child: Container(
          margin: const EdgeInsets.symmetric(vertical: 10, horizontal: 10),
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: const Color(0xFF1A1A1A).withOpacity(0.85),
            borderRadius: BorderRadius.circular(20),
          ),
          child: ImageViewer(
            result: widget.result,
            originalImageBytes: widget.originalImageBytes,
            hasDetections: widget.hasDetections,
            isZoomEnabled: isZoomEnabled,
            animationController: _animationController,
            showOverlay: _showOverlay,
            onToggleOverlay: _toggleOverlay,
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          image: DecorationImage(
            image: AssetImage('assets/bg2.jpg'),
            fit: BoxFit.cover,
          ),
        ),
        child: Container(
          color: const Color.fromARGB(150, 42, 14, 24),
          child: SafeArea(
            child: LayoutBuilder(
              builder: (context, constraints) => SingleChildScrollView(
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight),
                  child: IntrinsicHeight(
                    child: Column(
                      children: [
                        buildHeader(context, screenWidth),
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 10),
                          child: Row(
                            mainAxisAlignment:
                                MainAxisAlignment.center, // Centers the pair
                            mainAxisSize: MainAxisSize
                                .min, // Makes row take minimum space
                            children: [
                              _buildBackButton(screenWidth),
                              _buildDownloadButton(screenWidth),
                            ],
                          ),
                        ),
                        _buildImageViewer(context),
                        ConclusionWidget(
                          messageContent: messageContent,
                          onConfirm: _handleConclusionConfirmed,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
