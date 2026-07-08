import { ArrowLeft, Mail, Send } from "lucide-react";
import { useState, type ChangeEvent, type FormEvent } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { forgotPassword } from "@/api/authApi";
import { Button, ErrorMessage } from "@/components/common";
import { FormField } from "@/components/forms";
import { AuthPageShell } from "@/components/layout";
import { paths } from "@/routes/paths";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const successMessage =
  "If an account exists, password reset instructions have been sent.";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [fieldError, setFieldError] = useState<string | undefined>();
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useDocumentTitle("Forgot password · CampusOne");

  const updateEmail = (event: ChangeEvent<HTMLInputElement>) => {
    setEmail(event.target.value);
    setFieldError(undefined);
    setError(null);
    setSuccess(null);
  };

  const validate = () => {
    const value = email.trim();
    if (!value) {
      setFieldError("Enter your email address.");
      return false;
    }
    if (!emailPattern.test(value)) {
      setFieldError("Enter a valid email address.");
      return false;
    }
    return true;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (isSubmitting || !validate()) return;
    setIsSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await forgotPassword({ email: email.trim() });
      setSuccess(response.message || successMessage);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "Password reset instructions could not be requested. Please try again.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthPageShell
      alternateAction="Back to login"
      alternatePrompt="Remembered your password?"
      alternateTo={paths.login}
      description="Enter your account email. If it exists, CampusOne will send password reset instructions."
      eyebrow="Account recovery"
      title="Reset your password"
    >
      <form className="grid gap-5" noValidate onSubmit={handleSubmit}>
        {success ? (
          <div
            className="rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-3 text-sm text-emerald-700"
            role="status"
          >
            {success}
          </div>
        ) : null}
        {error ? <ErrorMessage message={error} /> : null}

        <FormField
          autoComplete="email"
          error={fieldError}
          icon={<Mail className="size-4" />}
          label="Email address"
          name="email"
          onChange={updateEmail}
          placeholder="you@university.edu.pk"
          required
          type="email"
          value={email}
        />

        <Button
          className="w-full"
          loading={isSubmitting}
          size="lg"
          type="submit"
        >
          {isSubmitting ? "Sending instructions" : "Send reset instructions"}
          {!isSubmitting ? <Send className="size-4" /> : null}
        </Button>

        <Link
          className="inline-flex items-center justify-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
          to={paths.login}
        >
          <ArrowLeft className="size-4" />
          Back to login
        </Link>
      </form>
    </AuthPageShell>
  );
}
