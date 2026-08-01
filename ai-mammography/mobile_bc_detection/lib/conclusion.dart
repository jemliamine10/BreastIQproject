import 'dart:math';

import 'package:flutter/material.dart';

class ConclusionWidget extends StatefulWidget {
  final String messageContent;
  final Function(String)? onConfirm;

  const ConclusionWidget({
    Key? key, 
    required this.messageContent,
    this.onConfirm,
  }) : super(key: key);

  @override
  _ConclusionWidgetState createState() => _ConclusionWidgetState();
}

class _ConclusionWidgetState extends State<ConclusionWidget> {
  late TextEditingController _textController;
  bool _isEditable = false;
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _textController = TextEditingController(text: widget.messageContent);
  }

  @override
  void didUpdateWidget(ConclusionWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.messageContent != oldWidget.messageContent) {
      _textController.text = widget.messageContent;
    }
  }

  @override
  void dispose() {
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _handleEditOrConfirm() {

    setState(() {
      if (_isEditable && widget.onConfirm != null) {
        widget.onConfirm!(_textController.text);
      }
      _isEditable = !_isEditable;
    });
  }

  @override
  Widget build(BuildContext context) {
    final maxWidth = min(700.0,MediaQuery.of(context).size.width);
    return Container(
      margin :  EdgeInsets.only(top:50),

      padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 10.0),
      child: SizedBox(
          width: maxWidth,
          child:Column(

        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                "Conclusion",
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              IconButton(
                icon: Icon(
                  _isEditable ? Icons.check : Icons.edit,
                  color: Colors.white,
                ),
                onPressed: _handleEditOrConfirm,
              ),
            ],
          ),
          const SizedBox(height: 10),
          Container(
            height: 130,
            decoration: BoxDecoration(
              color: const Color.fromARGB(255, 209, 115, 163).withOpacity(0.2),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: const Color.fromARGB(255, 42, 2, 20).withOpacity(0.3)),
            ),
            child: Scrollbar(
              controller: _scrollController,
              thumbVisibility: true,
              child: SingleChildScrollView(
                controller: _scrollController,
                padding: const EdgeInsets.all(12),
                child: ConstrainedBox(
                  constraints: BoxConstraints(
                    minHeight: 150,
                  ),
                  child: IntrinsicHeight(
                    child: TextField(
                      controller: _textController,
                      enabled: _isEditable,
                      maxLines: null,
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        isDense: true,
                        hintText: "Edit the conclusion...",
                        hintStyle: TextStyle(color: Colors.white.withOpacity(0.5)),
                        border: InputBorder.none,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      )),
    );
  }
}