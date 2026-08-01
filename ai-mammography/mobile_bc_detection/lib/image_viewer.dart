import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/material.dart';

class ImageViewer extends StatefulWidget {
  final Map<String, dynamic> result;
  final Uint8List originalImageBytes;
  final bool hasDetections;
  final ValueNotifier<bool> isZoomEnabled;
  final AnimationController animationController;
  final bool showOverlay;
  final VoidCallback onToggleOverlay;

  const ImageViewer({
    Key? key,
    required this.result,
    required this.originalImageBytes,
    required this.hasDetections,
    required this.isZoomEnabled,
    required this.animationController,
    required this.showOverlay,
    required this.onToggleOverlay,
  }) : super(key: key);

  @override
  _ImageViewerState createState() => _ImageViewerState();
}

class _ImageViewerState extends State<ImageViewer> {
  int _currentImageIndex = -1;
  Timer? _textTimer;
  bool _showImageText = false;

  // Add these:
  bool _showZoomText = false;
  String _zoomText = "";
  Timer? _zoomTextTimer;

  bool _showFeatures = false;

  void _showTemporaryText() {
    setState(() => _showImageText = true);
    _textTimer?.cancel();
    _textTimer = Timer(const Duration(seconds: 1), () {
      if (mounted) setState(() => _showImageText = false);
    });
  }

  void _showZoomStatus(bool zoomEnabled) {
    setState(() {
      _zoomText = zoomEnabled ? "Zoom enabled" : "Zoom disabled";
      _showZoomText = true;
    });
    _zoomTextTimer?.cancel();
    _zoomTextTimer = Timer(const Duration(seconds: 1), () {
      if (mounted) setState(() => _showZoomText = false);
    });
  }

  void _nextImage() {
    setState(() {
      if (!widget.hasDetections) return;
      if (_currentImageIndex < (widget.result['individual_predictions']?.length ?? 0) - 1) {
        _currentImageIndex++;
      } else {
        _currentImageIndex = -1;
      }
    });
    _showTemporaryText();
  }

  void _toggleFeatures() {
    setState(() {
      _showFeatures = !_showFeatures;
    });
  }

  Map<String, dynamic>? _getCurrentFeatures() {
    if (!widget.hasDetections) return null;
    if (_currentImageIndex == -1) {
      if ((widget.result['individual_predictions']?.length ?? 0) == 1) {
        return widget.result['individual_predictions'][0]['features'];
      }
      return null;
    } else {
      return widget.result['individual_predictions'][_currentImageIndex]['features'];
    }
  }

  Widget _buildImageStack() {
    Uint8List currentImage;
    if (widget.hasDetections) {
      if (_currentImageIndex == -1) {
        currentImage = widget.result["full_image"] ?? widget.originalImageBytes;
      } else {
        final individualPreds = widget.result['individual_predictions'];
        if (individualPreds != null &&
            _currentImageIndex >= 0 &&
            _currentImageIndex < individualPreds.length &&
            individualPreds[_currentImageIndex]['image'] != null) {
          currentImage = individualPreds[_currentImageIndex]['image'];
        } else {
          currentImage = widget.originalImageBytes;
        }
      }
    } else {
      currentImage = widget.originalImageBytes;
    }

    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 500),
      transitionBuilder: (child, animation) => FadeTransition(opacity: animation, child: child),
      child: Stack(
        key: ValueKey<Uint8List>(currentImage),
        children: [
          Image.memory(currentImage, fit: BoxFit.cover),
          AnimatedBuilder(
            animation: widget.animationController,
            builder: (context, _) => Opacity(
              opacity: widget.animationController.value,
              child: Image.memory(widget.originalImageBytes, fit: BoxFit.cover),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildControlButton({required IconData icon, required VoidCallback onPressed}) {
    return Container(
      decoration: BoxDecoration(
        color: const Color.fromARGB(183, 0, 0, 0),
        borderRadius: BorderRadius.circular(20),
      ),
      child: IconButton(
        icon: Icon(icon, color: Colors.white),
        onPressed: onPressed,
      ),
    );
  }

  Widget _buildFeaturesOverlay() {
    final features = _getCurrentFeatures();
    if (features == null) return const SizedBox.shrink();

    return Positioned.fill(
      child: Container(
        color: Colors.black.withOpacity(0.7),
        padding: const EdgeInsets.all(20),
        child: Center(
          child: SingleChildScrollView(
            child: Container(
              decoration: BoxDecoration(
                color: const Color.fromARGB(156, 0, 0, 0),
                borderRadius: BorderRadius.circular(15),
              ),
              padding: const EdgeInsets.all(20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Lesion Features',
                    style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 20),
                  _buildFeatureSection('Morphology', features['morphology']),
                  const SizedBox(height: 15),
                  _buildFeatureSection('Intensity', features['intensity']),
                  const SizedBox(height: 15),
                  _buildFeatureSection('Texture', features['texture']),
                  const SizedBox(height: 20),
                  ElevatedButton(
                    onPressed: _toggleFeatures,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color.fromARGB(159, 173, 23, 96),
                      foregroundColor: Colors.white,
                    ),
                    child: const Text('Close'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildFeatureSection(String title, Map<String, dynamic> features) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(color: Colors.white70, fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        ...features.entries.map((entry) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            children: [
              Text('${entry.key}: ', style: const TextStyle(color: Colors.white54)),
              Text(entry.value.toStringAsFixed(2), style: const TextStyle(color: Colors.white)),
              if (entry.key == 'area_mm2') const Text(' mm²', style: TextStyle(color: Colors.white54)),
              if (entry.key == 'perimeter_mm') const Text(' mm', style: TextStyle(color: Colors.white54)),
            ],
          ),
        )).toList(),
      ],
    );
  }

  bool _shouldShowFeaturesButton() {
    if (!widget.hasDetections) return false;
    return (widget.result['individual_predictions']?.length ?? 0) == 1 || _currentImageIndex != -1;
  }


Widget _buildPredictionClassOverlay() {
  // Don't show if no detections
  if (!widget.hasDetections) return const SizedBox.shrink();

  // Get the current prediction
  Map<String, dynamic>? currentPrediction;
  if (_currentImageIndex == -1) {
    if ((widget.result['individual_predictions']?.length ?? 0) == 1) {
      currentPrediction = widget.result['individual_predictions'][0];
    }
  } else {
    currentPrediction = widget.result['individual_predictions'][_currentImageIndex];
  }

  // Don't show if no prediction to display
  if (currentPrediction == null) return const SizedBox.shrink();

  // Get the class text
  final classText = currentPrediction['classification']?.toString() ?? 'Unknown';
  final opacityText = currentPrediction['label']?.toString() ?? 'single';

  return Positioned(
    bottom: 0,
    left: 0,
    right: 0,
    child: Center(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 4.5, horizontal: 11),
        margin: const EdgeInsets.only(bottom: 0),
        decoration: BoxDecoration(
          color: const Color.fromARGB(255, 43, 43, 43).withOpacity(0.7),
          borderRadius: BorderRadius.circular(4),
        ),
        child: Text(
          'Class of $opacityText opacity: $classText',
          style: const TextStyle(
            color: Colors.white,
            fontSize: 12,
          ),
        ),
      ),
    ),
  );
}


  @override
  void dispose() {
    _textTimer?.cancel();
    _zoomTextTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        ValueListenableBuilder<bool>(
          valueListenable: widget.isZoomEnabled,
          builder: (context, zoomEnabled, _) => zoomEnabled
              ? InteractiveViewer(
            minScale: 0.5,
            maxScale: 4.0,
            child: _buildImageStack(),
          )
              : _buildImageStack(),
        ),

        if (!widget.hasDetections && !widget.showOverlay)
          Positioned.fill(
            child: Container(
              color: Colors.black.withOpacity(0.77),
              alignment: Alignment.center,
              child: const Text(
                'No Abnormality detected',
                style: TextStyle(color: Colors.white, fontSize: 24),
              ),
            ),
          ),

        // Left side controls
        Positioned(
          top: 10,
          left: 10,
          child: Column(
            children: [
              if (_shouldShowFeaturesButton())
                _buildControlButton(
                  icon: Icons.analytics,
                  onPressed: _toggleFeatures,
                ),
            ],
          ),
        ),

        // Right side controls
        Positioned(
          top: 10,
          right: 10,
          child: Row(
            children: [
              _buildControlButton(
                icon: widget.showOverlay ? Icons.layers_clear : Icons.layers,
                onPressed: widget.onToggleOverlay,
              ),
              const SizedBox(width: 10),
              ValueListenableBuilder<bool>(
                valueListenable: widget.isZoomEnabled,
                builder: (context, zoomEnabled, _) => _buildControlButton(
                  icon: zoomEnabled ? Icons.zoom_out_map : Icons.zoom_in_map,
                  onPressed: () {
                    widget.isZoomEnabled.value = !zoomEnabled;
                    _showZoomStatus(widget.isZoomEnabled.value);
                  },
                ),
              ),
              if (widget.hasDetections && (widget.result['individual_predictions']?.length ?? 0) > 1) ...[
                const SizedBox(width: 10),
                _buildControlButton(
                  icon: Icons.navigate_next,
                  onPressed: _nextImage,
                ),
              ],
            ],
          ),
        ),

        // Detection change text
        Positioned(
          bottom: 40,
          left: 0,
          right: 0,
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 300),
            child: _showImageText && widget.hasDetections && (widget.result['individual_predictions']?.length ?? 0) > 1
                ? Container(
              key: ValueKey<int>(_currentImageIndex),
              padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
              decoration: BoxDecoration(
                color: Colors.black54,
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(
                _currentImageIndex == -1
                    ? "Full processed view"
                    : "Detection ${_currentImageIndex + 1}/${widget.result['individual_predictions']?.length ?? 0}",
                style: const TextStyle(color: Colors.white, fontSize: 16),
                textAlign: TextAlign.center,
              ),
            )
                : const SizedBox.shrink(),
          ),
        ),

        // Zoom toggle text with animation
        Positioned(
          bottom: 90,
          left: 0,
          right: 0,
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 300),
            transitionBuilder: (child, animation) {
              return FadeTransition(opacity: animation, child: child);
            },
            child: _showZoomText
                ? Container(
              key: ValueKey<String>(_zoomText),
              padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 14),
              margin: const EdgeInsets.symmetric(horizontal: 80),
              decoration: BoxDecoration(
                color: Colors.black54,
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(
                _zoomText,
                style: const TextStyle(color: Colors.white, fontSize: 16),
                textAlign: TextAlign.center,
              ),
            )
                : const SizedBox.shrink(),
          ),
        ),
          _buildPredictionClassOverlay(),

        if (_showFeatures) _buildFeaturesOverlay(),
      ],
    );
  }
}
