import { Link } from "wouter";
import AuthWrapper from "../components/auth-wrapper";
import Icon from "../components/icon";

export default function ResetEmailSent() {
  const email = new URLSearchParams(window.location.search).get("email") ?? "";

  return (
    <AuthWrapper>
      <div className="login-form py-10 px-10 flex flex-col items-center">
        <h2 className="text-zinc-100 mb-4 text-xl font-bold text-center">Check Your Email</h2>
        <Icon name="envelope" size={64} className="mb-4" />
        <p className="mb-4 text-center text-sm w-3/4">
          Please check your email address {email} for instructions to reset your password.
        </p>
        <div className="flex justify-center text-xs text-centered mt-4 text-zinc-400">
          <p>
            <Link href="/auth/login" className="underline">
              Back to Log In page
            </Link>
          </p>
        </div>
      </div>
    </AuthWrapper>
  );
}
