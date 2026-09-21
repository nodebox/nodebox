import React from "react";
import AuthWrapper from "../components/auth-wrapper";
import { PasswordField, SubmitField } from "../components/fields";
import InlineMessage from "../components/inline-message";
import { apiRoot } from "../config";

export default function ResetPassword() {
  const [error, setError] = React.useState<string | null>(null);

  const urlParams = new URLSearchParams(window.location.search);
  const userId = urlParams.get("userId");
  const token = urlParams.get("token");

  async function handleResetPassword(e: React.FormEvent<HTMLFormElement>) {
    setError(null);
    e.preventDefault();
    const formElements = (e.target as HTMLFormElement).elements;
    const submitButton = formElements.namedItem("submit") as HTMLButtonElement;
    const password1 = (formElements.namedItem("password1") as HTMLInputElement).value;
    const password2 = (formElements.namedItem("password2") as HTMLInputElement).value;
    if (password1 !== password2) {
      setError("Passwords do not match.");
      (formElements.namedItem("password1") as HTMLInputElement).value = "";
      (formElements.namedItem("password2") as HTMLInputElement).value = "";
      return;
    } else if (password1.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }

    submitButton.disabled = true;
    const res = await fetch(`${apiRoot}/api/auth/reset-password`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ userId, password: password1, token }),
    });
    submitButton.disabled = false;

    const json = await res.json();

    if (json.status !== "ok") {
      setError(json.message);
      return;
    }

    // Redirect to the login page.
    window.location.href = `/auth/login?reason=password-reset`;
  }

  return (
    <AuthWrapper>
      <div className="login-form py-10 px-10 flex flex-col items-stretch">
        <form onSubmit={handleResetPassword}>
          <h2 className="text-zinc-100 mb-4 text-xl font-bold text-center">Reset Password</h2>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <p className="mb-4 text-xs">Please enter your new password:</p>
          <PasswordField label="Password" name="password1" description="Minimum 8 characters" required={true} />
          <PasswordField label="Password (again)" name="password2" required={true} />

          <div>
            <SubmitField name="submit" label="Reset" />
          </div>
        </form>
      </div>
    </AuthWrapper>
  );
}
