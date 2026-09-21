import React from "react";
import { Link } from "wouter";
import AuthWrapper from "../components/auth-wrapper";
import { TextField, SubmitField } from "../components/fields";
import InlineMessage from "../components/inline-message";
import { apiRoot } from "../config";

export default function ForgotPassword() {
  const [error, setError] = React.useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    setError(null);
    e.preventDefault();
    const formElements = (e.target as HTMLFormElement).elements;
    const submitButton = formElements.namedItem("submit") as HTMLButtonElement;
    const userIdOrEmail = (formElements.namedItem("userIdOrEmail")! as HTMLInputElement).value;
    let isEmail = false;
    if (userIdOrEmail.match(/^[a-zA-Z0-9]{3,20}$/)) {
      // Pass
    } else if (userIdOrEmail.match(/^(.*)@(.*)$/)) {
      isEmail = true;
    } else {
      setError(
        'Invalid User ID or email. User ID must be 3-20 characters and contain only letters and numbers. Email addresses should have an "@" symbol.',
      );
      return;
    }

    submitButton.disabled = true;
    const res = await fetch(`${apiRoot}/api/auth/forgot-password`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ userIdOrEmail }),
    });
    submitButton.disabled = false;
    if (!res.ok) {
      setError("Something went wrong. Please try again later.");
      return;
    }
    const json = await res.json();

    if (json.status !== "ok") {
      setError(json.message);
      return;
    }

    // Redirect to the "sent an email" page.
    if (isEmail) {
      window.location.href = `/auth/reset-email-sent?email=${userIdOrEmail}`;
    } else {
      window.location.href = `/auth/reset-email-sent`;
    }
  }

  return (
    <AuthWrapper>
      <div className="login-form py-10 px-10 flex flex-col items-stretch">
        <form onSubmit={handleSubmit}>
          <h2 className="text-zinc-100 mb-4 text-xl font-bold text-center">Reset Your Password</h2>
          <p className="mb-4">
            Enter your email address or user ID and we will send you instructions to reset your password.
          </p>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <TextField label="User ID or Email" name="userIdOrEmail" required={true} autoFocus={true} />

          <div>
            <SubmitField name="submit" label="Continue" />
          </div>

          <div className="flex justify-center text-xs text-centered mt-4 text-zinc-400">
            <p>
              <Link href="/auth/login" className="underline">
                Back to Log In page
              </Link>
            </p>
          </div>
        </form>
      </div>
    </AuthWrapper>
  );
}
