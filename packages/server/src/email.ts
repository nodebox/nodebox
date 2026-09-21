const FROM = { name: "NodeBox Support", email: "info@nodebox.live" };

const FORGOT_EMAIL_TEMPLATE = `
Hi there,

You have requested a password reset for your NodeBox Live account. Please click the following link to reset your password:

{URL}

If you did not request a password reset, please ignore this email.

Thanks,

The NodeBox Live team
`;

export async function sendEmail(email: SendEmail | undefined, to: string, subject: string, body: string) {
  if (!email) {
    console.log(`EMAIL binding not available; email to ${to} not sent:\n${body}`);
    return;
  }
  try {
    const result = await email.send({ from: FROM, to, subject, text: body });
    console.log("Email sent:", result.messageId);
  } catch (error) {
    console.error("Error sending email:", error);
    throw error;
  }
}

export async function sendForgotPasswordEmail(email: SendEmail | undefined, to: string, userId: string, token: string) {
  const subject = "NodeBox Live password reset";
  const url = `https://new.nodebox.live/auth/reset-password?userId=${userId}&token=${token}`;
  const body = FORGOT_EMAIL_TEMPLATE.replace("{URL}", url);
  await sendEmail(email, to, subject, body);
}
