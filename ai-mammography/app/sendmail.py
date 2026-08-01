import datetime
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
import smtplib
from fastapi import Form, HTTPException  # Add this import at the top
from pydantic import BaseModel, validator, EmailStr
from config import Settings
# Pydantic model to validate the structure of email input data
class EmailData(BaseModel):
    name: str
    email: EmailStr  # Validates that this field is a proper email
    subject: str
    message: str


# Function to send an email to the SafeScan team
def send_email(subject: str, email: str, name: str, message: str, body: str, recipient_email: str, settings: Settings):
    print("innnnnnnnn")
    try:
        # Sender credentials from settings
        sender_email = settings.email
        sender_password = settings.password  # App-specific password for Gmail
        
        # Logic fix: Prepare HTML message outside of f-string to avoid SyntaxError with \n
        msg_html_body = message.replace('\n', '<br>')

        # Compose email with HTML content
        msg = MIMEMultipart('alternative')
        msg['From'] = sender_email
        msg['To'] = recipient_email
        msg['Subject'] = f"SafeScan Contact: {subject}"

        # HTML template for the email body
        html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body {{
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                }}
                .header {{
                    background-color: #e5a0c6;  
                    color: white;
                    padding: 20px;
                    text-align: center;
                    border-radius: 5px 5px 0 0;
                }}
                .content {{
                    padding: 20px;
                    background-color: #f9f9f9;
                    border: 1px solid #ddd;
                    border-top: none;
                }}
                .footer {{
                    margin-top: 20px;
                    font-size: 12px;
                    color: #777;
                    text-align: center;
                }}
                .info-label {{
                    font-weight: bold;
                    color: #e5a0c6;
                }}
                .message-box {{
                    background-color: white;
                    padding: 15px;
                    border-left: 4px solid #e5a0c6;
                    margin-top: 15px;
                }}
            </style>
        </head>
        <body>
            <div class="header">
                <h2>New Message from SafeScan User</h2>
            </div>
            
            <div class="content">
                <p><span class="info-label">Name:</span> {name}</p>
                <p><span class="info-label">Email:</span> {email}</p>
                <p><span class="info-label">Subject:</span> {subject}</p>
                
                <div class="message-box">
                    <p><span class="info-label">Message:</span></p>
                    <p>{msg_html_body}</p>
                </div>
            </div>
            
            <div class="footer">
                <p>This message was sent via the SafeScan Breast Cancer Detection App</p>
                <p>© {datetime.datetime.now().year} SafeScan. All rights reserved.</p>
            </div>
        </body>
        </html>
        """

        # Attach both plain text and HTML versions
        part1 = MIMEText(body, 'plain')
        part2 = MIMEText(html, 'html')
        
        msg.attach(part1)
        msg.attach(part2)

        # Send the email using Gmail's SMTP server over SSL
        with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
            server.login(sender_email, sender_password)
            server.sendmail(sender_email, recipient_email, msg.as_string())

    except Exception as e:
        # Return a 500 Internal Server Error with exception message if sending fails
        raise HTTPException(status_code=500, detail=f"Error sending email: {str(e)}")
    




    
def reply_email(subject: str , email : str ,name: str  , message : str  ,settings:Settings):
    try:
        sender_email = settings.email
        sender_password = settings.password  # Use App Password
        
        msg_html_body = message.replace('\n', '<br>')

       # Create the email message with HTML content
        msg = MIMEMultipart('alternative')
        msg['From'] = sender_email
        msg['To'] = email
        msg['Subject'] = f"SafeScan Contact: {subject}"


        # HTML email template
        html = f"""
<!DOCTYPE html>
<html>
<head>
    <style>
        body {{
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }}
        .header {{
            background-color: #e5a0c6;
            color: white;
            padding: 25px;
            text-align: center;
            border-radius: 5px 5px 0 0;
        }}
        .content {{
            padding: 25px;
            background-color: #f9f9f9;
            border: 1px solid #ddd;
            border-top: none;
        }}
        .footer {{
            margin-top: 20px;
            font-size: 12px;
            color: #777;
            text-align: center;
        }}
        .thank-you {{
            font-size: 18px;
            color: #e5a0c6;
            margin-bottom: 20px;
            font-weight: bold;
        }}
        .team-signature {{
            margin-top: 25px;
            font-style: italic;
            color: #555;
        }}
        .divider {{
            border-top: 1px dashed #e5a0c6;
            margin: 20px 0;
        }}
    </style>
</head>
<body>
    <div class="header">
        <h2 style="margin:0;">Thank You for Contacting SafeScan</h2>
    </div>
    
    <div class="content">
        <div class="thank-you">Dear {name},</div>
        
        <p>We've received your message and truly appreciate you reaching out to us. 
        Our team is reviewing your inquiry and will respond within 24-48 hours.</p>
        
        <div class="divider"></div>
        
        <p><strong>For your reference, here's a copy of your message:</strong></p>
        
        <div style="background: white; padding: 15px; border-radius: 5px; border-left: 3px solid #e27bb1;">
            <p><strong>Subject:</strong> {subject}</p>
            <p>{msg_html_body}</p>
        </div>
        
        <div class="team-signature">
            <p>With care,<br>
            The SafeScan Team</p>
        </div>
    </div>
    
    <div class="footer">
        <p>This email was sent in response to your contact form submission.</p>
        <p>© {datetime.datetime.now().year} SafeScan Breast Cancer Detection App. All rights reserved.</p>
        <p style="font-size:11px; color:#999;">
            <em>Your health matters to us. This message contains confidential information 
            intended only for the recipient.</em>
        </p>
    </div>
</body>
</html>
"""

        # Attach both plain text and HTML versions
        part2 = MIMEText(html, 'html')
        msg.attach(part2)

        # Send email
        with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
            server.login(sender_email, sender_password)
            server.sendmail(sender_email, email, msg.as_string())

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error sending email: {str(e)}")