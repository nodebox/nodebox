import React from "react";
import { Link } from "wouter";
import { useAuth } from "../auth-context";
import AuthWrapper from "../components/auth-wrapper";
import { TextField, SubmitField } from "../components/fields";
import InlineMessage from "../components/inline-message";
import { apiRoot } from "../config";

export default function Login() {
  const [error, setError] = React.useState<string | null>(null);
  const [userId, setUserId] = React.useState("");
  const { login } = useAuth()!;

  const urlParams = new URLSearchParams(window.location.search);
  const passwordReset = urlParams.get("reason") === "password-reset";

  const handleUserIdChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.toLowerCase();
    if (value === "" || /^[a-z][a-z0-9]*$/.test(value)) {
      setUserId(value);
    }
  };

  async function handleLogin(e: React.FormEvent<HTMLFormElement>) {
    setError(null);
    e.preventDefault();
    const formElements = (e.target as HTMLFormElement).elements;
    const password = (formElements.namedItem("password")! as HTMLInputElement).value;

    const res = await fetch(`${apiRoot}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ userId, password }),
    });
    if (res.status === 500) {
      setError("Invalid response from the server. Try again later.");
      return;
    }

    const json = await res.json();
    if (json.status !== "ok") {
      setError(json.message);
      return;
    }

    login(json.token, userId, json.membership);

    // Redirect to home page
    window.location.href = `/${userId}`;
  }

  return (
    <AuthWrapper>
      <div className="login-form py-10 px-10 flex flex-col items-stretch">
        <form onSubmit={handleLogin}>
          <h2 className="text-zinc-100 mb-4 text-xl font-bold text-center">Log In</h2>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          {passwordReset && (
            <p className="text-sm mb-4 text-zinc-100 text-center">Password reset. Please log in again.</p>
          )}
          <TextField
            label="User ID"
            name="userId"
            required={true}
            autoFocus={true}
            value={userId}
            onChange={handleUserIdChange}
          />
          <div className="form-row mb-3">
            <div className="flex justify-between">
              <h3 className="text-zinc-200">Password</h3>
              <Link className="text-zinc-400 text-xs" href="/auth/forgot-password" tabIndex={-1}>
                Forgot password?
              </Link>
            </div>
            <input
              className="bg-zinc-800 border border-zinc-600 rounded p-2 outline-none focus:border-blue-500 text-zinc-200 mb-4 w-full"
              type="password"
              name="password"
              required={true}
            />
          </div>

          <div>
            <SubmitField name="login" label="Log In" />
          </div>

          <div className="flex justify-center text-xs text-centered mt-4 text-zinc-400">
            <p>
              No account yet?{" "}
              <Link href="/auth/signup" className="underline">
                Sign up
              </Link>
            </p>
          </div>
        </form>
      </div>
    </AuthWrapper>
  );
}
