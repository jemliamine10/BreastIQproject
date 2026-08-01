// generatepdf.dart
import 'dart:typed_data';
import 'package:pdf/widgets.dart' as pw;
import 'package:pdf/pdf.dart';
import 'package:intl/intl.dart';
import 'package:printing/printing.dart';

class GeneratePdf {
  static Future<Uint8List> generate({
    required Map<String, dynamic> result,
    required Uint8List originalImageBytes,
    required bool hasDetections,
    required String conclusion,
    required patientName,
  }) async {
    final pdf = pw.Document();
    final image = hasDetections
        ? pw.MemoryImage(result['full_image'])
        : pw.MemoryImage(originalImageBytes);

    final predictions = hasDetections
        ? result['individual_predictions'] as List<dynamic>
        : <dynamic>[];

    pdf.addPage(
      pw.MultiPage(
        pageFormat: PdfPageFormat.a4,
        margin: const pw.EdgeInsets.all(32),
        build: (pw.Context context) {
          final content = <pw.Widget>[];

          // Header
          content.add(_buildReportHeader(patientName));
          content.add(pw.SizedBox(height: 20));

          // Main image
          content.add(
            pw.Center(
              child: pw.Container(
                padding: const pw.EdgeInsets.all(8),
                decoration: pw.BoxDecoration(
                  border: pw.Border.all(color: PdfColors.grey800),
                ),
                child: pw.Image(image, width: 500, height: 300, fit: pw.BoxFit.contain),
              ),
            ),
          );
          content.add(pw.SizedBox(height: 30));

          // Detected opacities
          content.add(_buildDetectedOpacitiesSection(result, hasDetections));
          content.add(pw.SizedBox(height: 20));

          // Detailed analysis and full table
          if (hasDetections) {
            content.add(
              pw.Text('DETAILED ANALYSIS OF OPACITIES',
                  style: pw.TextStyle(
                      fontSize: 14,
                      fontWeight: pw.FontWeight.bold,
                      color: PdfColors.grey600)),
            );
            content.add(pw.Divider(thickness: 0.5));
            content.add(pw.SizedBox(height: 10));
            content.add(_buildPredictionsTable(predictions));
            content.add(pw.SizedBox(height: 20));
          }

          // Conclusion
          if (predictions.length == 2) {
            content.add(pw.SizedBox(height: 60));
          }

          content.add(pw.Container(
            child: _buildConclusionSection(conclusion),
          ));

          return content;
        },
      ),
    );

    return pdf.save();
  }

  static pw.Table _buildPredictionsTable(List<dynamic> predictions) {
    final tableData = predictions.map<List<dynamic>>((prediction) {
      final features = prediction['features'] ?? {};
      final morph = features['morphology'] ?? {};
      final intensity = features['intensity'] ?? {};
      final texture = features['texture'] ?? {};

      final featuresString = [
        if (morph['area_mm2'] != null)
          'Area: ${morph['area_mm2']?.toStringAsFixed(2)}mm²',
        if (morph['circularity'] != null)
          'Circularity: ${morph['circularity']?.toStringAsFixed(2)}',
        if (intensity['mean'] != null)
          'Intensity: ${intensity['mean']?.toStringAsFixed(1)}',
        if (texture['glcm_homogeneity'] != null)
          'Homogeneity: ${texture['glcm_homogeneity']?.toStringAsFixed(2)}',
      ].join(', ');

      final imageBytes = prediction['crop'] as Uint8List?;

      return [
        prediction['label'] ?? 'N/A',
        '${((prediction['score'] ?? 0) * 100).toStringAsFixed(1)}%',
        featuresString,
        prediction['classification'] ?? 'N/A',
        pw.ClipRRect(
          horizontalRadius: 5,
          verticalRadius: 5,
          child: pw.Image(
            pw.MemoryImage(imageBytes ?? Uint8List(0)),
            width: 50,
            height: 50,
          ),
        ),
      ];
    }).toList();

    return pw.TableHelper.fromTextArray(
      border: null,
      cellAlignment: pw.Alignment.centerLeft,
      headerDecoration: const pw.BoxDecoration(color: PdfColors.grey200),
      headerStyle: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 10),
      cellStyle: const pw.TextStyle(fontSize: 10),
      headerAlignments: {
        0: pw.Alignment.centerLeft,
        1: pw.Alignment.center,
        2: pw.Alignment.centerLeft,
        3: pw.Alignment.center,
        4: pw.Alignment.center,
      },
      columnWidths: {
        0: const pw.FlexColumnWidth(2),
        1: const pw.FlexColumnWidth(1),
        2: const pw.FlexColumnWidth(3),
        3: const pw.FlexColumnWidth(1),
        4: const pw.FlexColumnWidth(2),
      },
      headers: ['Opacity', 'Score', 'Features', 'Class', 'Visualization'],
      data: tableData,
    );
  }

  static pw.Widget _buildReportHeader(patientName) {
    return pw.Column(
      crossAxisAlignment: pw.CrossAxisAlignment.start,
      children: [
        pw.Text('MAMMOGRAPHY DIAGNOSTIC REPORT',
            style: pw.TextStyle(
                fontSize: 18,
                fontWeight: pw.FontWeight.bold,
                color: PdfColors.grey600)),
        pw.Text('Supported by SafeScan system',
            style: pw.TextStyle(
                fontSize: 10,
                fontWeight: pw.FontWeight.bold,
                color: PdfColors.grey600)),
        pw.Divider(thickness: 1),
        pw.Row(
          mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
          children: [
            pw.Text('Patient Name: $patientName',
                style: const pw.TextStyle(fontSize: 12)),
            pw.Text('Date: ${DateFormat('yyyy-MM-dd').format(DateTime.now())}',
                style: const pw.TextStyle(fontSize: 12)),
          ],
        ),
      ],
    );
  }

  static pw.Widget _buildDetectedOpacitiesSection(
      Map<String, dynamic> result, bool hasDetections) {
    int masscount = 0;
    int calccount = 0;

    if (hasDetections) {
      for (var prediction in result['individual_predictions']) {
        if (prediction['label'] == 'mass') {
          masscount++;
        } else if (prediction['label'] == 'calc') {
          calccount++;
        }
      }
    }

    return pw.Column(
      crossAxisAlignment: pw.CrossAxisAlignment.start,
      children: [
        pw.Text('DETECTED OPACITIES',
            style: pw.TextStyle(
                fontSize: 14,
                fontWeight: pw.FontWeight.bold,
                color: PdfColors.grey600)),
        pw.Divider(thickness: 0.5),
        pw.SizedBox(height: 10),
        if (!hasDetections)
          pw.Text('No suspicious opacities detected',
              style: const pw.TextStyle(fontSize: 12))
        else
          pw.Column(
            crossAxisAlignment: pw.CrossAxisAlignment.start,
            children: [
              if (masscount > 0)
                pw.Text('- $masscount ${masscount == 1 ? 'mass' : 'masses'}',
                    style: const pw.TextStyle(fontSize: 12)),
              if (calccount > 0)
                pw.Text('- $calccount ${calccount == 1 ? 'calcification' : 'calcifications'}',
                    style: const pw.TextStyle(fontSize: 12)),
            ],
          ),
      ],
    );
  }

  static pw.Widget _buildConclusionSection(String conclusion) {
    return pw.Column(
      crossAxisAlignment: pw.CrossAxisAlignment.start,
      children: [
        pw.Text('CONCLUSION',
            style: pw.TextStyle(
                fontSize: 14,
                fontWeight: pw.FontWeight.bold,
                color: PdfColors.grey600)),
        pw.Divider(thickness: 0.5),
        pw.SizedBox(height: 10),
        pw.Wrap(
          runSpacing: 8,
          children: [pw.Text(conclusion, style: const pw.TextStyle(fontSize: 12))],
        ),
      ],
    );
  }
}