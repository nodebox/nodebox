import nodemailer from "nodemailer";

const { SMTP_HOST = "localhost", SMTP_PORT = 25, SMTP_SECURE = false, SMTP_USERNAME, SMTP_PASSWORD } = process.env;

// Create a transporter using the specified SMTP server settings
const transporter = nodemailer.createTransport({
  host: SMTP_HOST,
  port: SMTP_PORT,
  secure: SMTP_SECURE === "true",
  auth:
    SMTP_USERNAME && SMTP_PASSWORD
      ? {
          user: SMTP_USERNAME,
          pass: SMTP_PASSWORD,
        }
      : undefined,
});

export async function sendEmail(to, subject, body) {
  const mailOptions = {
    from: '"NodeBox Support" <info@nodebox.live>',
    to,
    subject,
    text: body,
  };

  try {
    const info = await transporter.sendMail(mailOptions);
    console.log("Email sent:", info.messageId);
    return info;
  } catch (error) {
    console.error("Error sending email:", error);
    throw error;
  }
}

const FORGOT_EMAIL_TEMPLATE = `
Hi there,

You have requested a password reset for your NodeBox Live account. Please click the following link to reset your password:

{URL}

If you did not request a password reset, please ignore this email.

Thanks,

The NodeBox Live team
`;

export async function sendForgotPasswordEmail(email, userId, token) {
  const subject = "NodeBox Live password reset";
  const url = `https://new.nodebox.live/auth/reset-password?userId=${userId}&token=${token}`;
  const body = FORGOT_EMAIL_TEMPLATE.replace("{URL}", url);
  await sendEmail(email, subject, body);
}
