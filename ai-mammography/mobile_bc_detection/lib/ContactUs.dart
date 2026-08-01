import 'dart:math';
import 'package:flutter/material.dart';
import 'package:mobile_bc_detection/Controller.dart';
import 'package:mobile_bc_detection/header.dart';

class ContactUs extends StatefulWidget {
  const ContactUs({Key? key}) : super(key: key);

  @override
  ContactUsState createState() => ContactUsState();
}

class ContactUsState extends State<ContactUs> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _subjectController = TextEditingController();
  final TextEditingController _messageController = TextEditingController();

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _subjectController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  
void _submitForm() async {
  if (_formKey.currentState!.validate()) {
    // Create the form data to send to the backend
    final Map<String, dynamic> formData = {
      'name': _nameController.text,
      'email': _emailController.text,
      'subject': _subjectController.text,
      'message': _messageController.text,
    };

    final result = await Controller.sendEmail(formData);

    if (result != null && result['status'] == 'success') {
      // Show success dialog
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          backgroundColor: Colors.black,
          title: const Text('Message Sent', style: TextStyle(color: Colors.white)),
          content: const Text(
            'Thank you for contacting us. We will get back to you soon.',
            style: TextStyle(color: Colors.white),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('OK', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      );
      // Clear the form
      _nameController.clear();
      _emailController.clear();
      _subjectController.clear();
      _messageController.clear();
    } else {
      // Show error dialog if request failed
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          backgroundColor: Colors.black,
          title: const Text('Error', style: TextStyle(color: Colors.white)),
          content: Text(result?['message'] ?? 'Unknown error', style: TextStyle(color: Colors.white)),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('OK', style: TextStyle(color: Colors.white)),
            ),
          ],
        ),
      );
    }
  }
}

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final isSmallScreen = screenWidth < 600;

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          image: DecorationImage(
              image: AssetImage('assets/bg2.jpg'), fit: BoxFit.cover),
        ),
        child: Container(
          color: Color.fromARGB(150, 42, 14, 24),
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
                          padding: const EdgeInsets.symmetric(horizontal: 16.0),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const SizedBox(height: 30),
                              Center(
                                child: Text(
                                  'Contact Us',
                                  style: TextStyle(
                                    color: const Color.fromARGB(205, 255, 255, 255),
                                    fontSize: isSmallScreen ? 28 : 34,
                                    fontWeight: FontWeight.bold,
                                    letterSpacing: 1.2,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 10),
                              Center(
                                child: Text(
                                  'Have questions or feedback? We\'d love to hear from you!',
                                  textAlign: TextAlign.center,
                                  style: TextStyle(
                                    color: Colors.white70,
                                    fontSize: isSmallScreen ? 14 : 16,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 30),
                              Card(
                                  color: Colors.transparent, // <-- Make the card itself transparent

                                elevation: 8,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(15),
                                ),
                                margin: EdgeInsets.symmetric(
                                    horizontal: isSmallScreen ? 10 : 50),
                                child: Container(
                                  padding: const EdgeInsets.all(25),
                                  decoration: BoxDecoration(
                                    color: Color.fromARGB(140, 0, 0, 0), // Adjust the alpha value (150) for desired opacity
                                    borderRadius: BorderRadius.circular(15),
                                  ),
                                  child: Form(
                                    key: _formKey,
                                    child: Column(
                                      children: [
                                        _buildTextField(
                                          controller: _nameController,
                                          label: 'Your Name',
                                          icon: Icons.person,
                                          validator: (value) {
                                            if (value == null || value.isEmpty) {
                                              return 'Please enter your name';
                                            }
                                            return null;
                                          },
                                        ),
                                        const SizedBox(height: 20),
                                        _buildTextField(
                                          controller: _emailController,
                                          label: 'Email Address',
                                          icon: Icons.email,
                                          keyboardType:
                                              TextInputType.emailAddress,
                                          validator: (value) {
                                            if (value == null || value.isEmpty) {
                                              return 'Please enter your email';
                                            }
                                            if (!RegExp(
                                                    r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$')
                                                .hasMatch(value)) {
                                              return 'Please enter a valid email';
                                            }
                                            return null;
                                          },
                                        ),
                                        const SizedBox(height: 20),
                                        _buildTextField(
                                          controller: _subjectController,
                                          label: 'Subject',
                                          icon: Icons.subject,
                                          validator: (value) {
                                            if (value == null || value.isEmpty) {
                                              return 'Please enter a subject';
                                            }
                                            return null;
                                          },
                                        ),
                                        const SizedBox(height: 20),
                                        _buildTextField(
                                          controller: _messageController,
                                          label: 'Your Message',
                                          icon: Icons.message,
                                          maxLines: 5,
                                          validator: (value) {
                                            if (value == null || value.isEmpty) {
                                              return 'Please enter your message';
                                            }
                                            if (value.length < 10) {
                                              return 'Message is too short';
                                            }
                                            return null;
                                          },
                                        ),
                                        const SizedBox(height: 30),
                                        SizedBox(
                                          width: double.infinity,
                                          child: ElevatedButton(
                                            onPressed: _submitForm,
                                            style: ElevatedButton.styleFrom(
                                              backgroundColor: Color.fromARGB(
                                                  255, 158, 42, 107),
                                              padding:
                                                  const EdgeInsets.symmetric(
                                                      vertical: 16),
                                              shape: RoundedRectangleBorder(
                                                borderRadius:
                                                    BorderRadius.circular(10),
                                              ),
                                              elevation: 5,
                                            ),
                                            child: const Text(
                                              'SEND MESSAGE',
                                              style: TextStyle(
                                                color: Colors.white,
                                                fontSize: 16,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ),
                              const SizedBox(height: 40),
                              if (!isSmallScreen) ...[
                                _buildContactInfoRow(),
                                const SizedBox(height: 40),
                              ],
                            ],
                          ),
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

  Widget _buildTextField({
  required TextEditingController controller,
  required String label,
  required IconData icon,
  required String? Function(String?) validator,
  TextInputType keyboardType = TextInputType.text,
  int maxLines = 1,
}) {
  return TextFormField(
    controller: controller,
    keyboardType: keyboardType,
    maxLines: maxLines,
    validator: validator,
    style: TextStyle(color: Colors.white), // <--- user-entered text color

    decoration: InputDecoration(
      labelText: label,
      labelStyle: TextStyle(color: Colors.white70), // <--- label color

      prefixIcon: Icon(icon, color: Color.fromARGB(255, 158, 42, 107)),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: BorderSide(color: Colors.grey),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: BorderSide(color: Color.fromARGB(255, 158, 42, 107)),
      ),
    ),
  );
}

  Widget _buildContactInfoRow() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 50),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildContactInfoItem(
            icon: Icons.email,
            title: 'Email Us',
            subtitle: 'safescan.contact@gmail.com',
          ),
          _buildContactInfoItem(
            icon: Icons.phone,
            title: 'Call Us',
            subtitle: '+1 (555) 123-4567',
          ),
          _buildContactInfoItem(
            icon: Icons.location_on,
            title: 'Visit Us',
            subtitle: '123 Medical Drive\nHealth City, HC 12345',
          ),
        ],
      ),
    );
  }

  Widget _buildContactInfoItem({
    required IconData icon,
    required String title,
    required String subtitle,
  }) {
    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(15),
          decoration: BoxDecoration(
            color: Color.fromARGB(255, 158, 42, 107).withOpacity(0.2),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, size: 30, color: Colors.white),
        ),
        const SizedBox(height: 10),
        Text(
          title,
          style: TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 5),
        Text(
          subtitle,
          textAlign: TextAlign.center,
          style: TextStyle(
            color: Colors.white,
            fontSize: 14,
          ),
        ),
      ],
    );
  }
}