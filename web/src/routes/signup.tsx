import React from "react";
import { Link } from "wouter";
import AuthWrapper from "../components/auth-wrapper";
import { useAuth } from "../auth-context";
import { TextField, EmailField, PasswordField, SubmitField } from "../components/fields";
import InlineMessage from "../components/inline-message";
import { apiRoot } from "../config";

export default function Signup() {
  const [error, setError] = React.useState<string | null>(null);
  const [userId, setUserId] = React.useState("");
  const { login } = useAuth()!;

  const handleUserIdChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.toLowerCase();
    // Only allow lowercase letters for first char, then letters and numbers
    if (value === "" || (/^[a-z][a-z0-9]*$/.test(value) && value.length <= 20)) {
      setUserId(value);
    }
  };

  async function handleSignup(e: React.FormEvent<HTMLFormElement>) {
    setError(null);
    e.preventDefault();
    const formElements = (e.target as HTMLFormElement).elements;
    const submitButton = formElements.namedItem("submit") as HTMLButtonElement;

    if (!userId.match(/^[a-z][a-z0-9]{2,19}$/)) {
      setError("User ID must start with a letter and be 3-20 characters containing only letters and numbers.");
      return;
    }

    const email = (formElements.namedItem("email") as HTMLInputElement).value;
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
    const res = await fetch(`${apiRoot}/api/auth/signup`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ userId, email, password: password1 }),
    });
    submitButton.disabled = false;

    const json = await res.json();

    if (json.status !== "ok") {
      setError(json.message);
      return;
    }

    login(json.token, userId, json.membership);

    // Redirect to the user's page.
    window.location.href = `/${userId}`;
  }

  return (
    <AuthWrapper>
      <div className="login-form py-10 px-10 flex flex-col items-stretch">
        <form onSubmit={handleSignup}>
          <h2 className="text-zinc-100 mb-4 text-xl font-bold text-center">Sign Up</h2>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <TextField
            label="User ID"
            name="userId"
            required={true}
            autoFocus={true}
            value={userId}
            onChange={handleUserIdChange}
            description="Start with a letter. Only lowercase letters and numbers allowed."
          />
          <EmailField label="Email" name="email" required={true} />
          <PasswordField label="Password" name="password1" required={true} />
          <PasswordField label="Password (again)" name="password2" required={true} />

          <div>
            <SubmitField name="submit" label="Sign Up" />
          </div>

          <div className="flex justify-center text-xs text-centered mt-4 text-zinc-400">
            <p>
              Already signed up?{" "}
              <Link href="/auth/login" className="underline">
                Log In
              </Link>
            </p>
          </div>
        </form>
      </div>
    </AuthWrapper>
  );
}
