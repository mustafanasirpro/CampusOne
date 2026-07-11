import { ArrowLeft, CheckCircle2, LockKeyhole } from "lucide-react";
import { useMemo, useState, type ChangeEvent, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { resetPassword } from "@/api/authApi";
import { Button, ErrorMessage } from "@/components/common";
import { PasswordField } from "@/components/forms";
import { AuthPageShell } from "@/components/layout";
import { paths } from "@/routes/paths";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface ResetPasswordForm {
  confirmPassword: string;
  newPassword: string;
}

type ResetPasswordErrors = Partial<Record<keyof ResetPasswordForm, string>>;

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;
const requestTimeoutMs = 15_000;
const successMessage = "Password reset successfully. You can now log in.";

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = useMemo(() => searchParams.get("token")?.trim() ?? "", [
    searchParams,
  ]);
  const [form, setForm] = useState<ResetPasswordForm>({
    confirmPassword: "",
    newPassword: "",
  });
  const [errors, setErrors] = useState<ResetPasswordErrors>({});
  const [requestError, setRequestError] = useState<string | null>(
    token ? null : "This reset link is invalid or expired.",
  );
  const [success, setSuccess] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useDocumentTitle("Reset password · CampusOne");

  const updateField =
    (field: keyof ResetPasswordForm) =>
    (event: ChangeEvent<HTMLInputElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: undefined }));
      setRequestError(null);
      setSuccess(null);
    };

  const validate = () => {
    const nextErrors: ResetPasswordErrors = {};
    if (form.newPassword.length < 8) {
      nextErrors.newPassword = "Password must be at least 8 characters.";
    } else if (form.newPassword.length > 72) {
      nextErrors.newPassword = "Password must be 72 characters or fewer.";
    } else if (!passwordPattern.test(form.newPassword)) {
      nextErrors.newPassword =
        "Use uppercase, lowercase, and at least one digit.";
    }

    if (!form.confirmPassword) {
      nextErrors.confirmPassword = "Confirm your new password.";
    } else if (form.confirmPassword !== form.newPassword) {
      nextErrors.confirmPassword = "Passwords do not match.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (isSubmitting || !validate()) return;
    if (!token) {
      setRequestError("This reset link is invalid or expired.");
      return;
    }
    setIsSubmitting(true);
    setRequestError(null);
    setSuccess(null);
    const controller = new AbortController();
    const timeoutId = window.setTimeout(
      () => controller.abort(),
      requestTimeoutMs,
    );
    try {
      const response = await resetPassword(
        {
          newPassword: form.newPassword,
          token,
        },
        controller.signal,
      );
      setSuccess(response.message || successMessage);
      setForm({ confirmPassword: "", newPassword: "" });
    } catch (error) {
      setRequestError(
        error instanceof DOMException && error.name === "AbortError"
          ? "We could not connect right now. Please try again."
          : error instanceof ApiError
          ? error.message
          : "This reset link is invalid or expired.",
      );
    } finally {
      window.clearTimeout(timeoutId);
      setIsSubmitting(false);
    }
  };

  return (
    <AuthPageShell
      alternateAction="Back to login"
      alternatePrompt="Ready to sign in?"
      alternateTo={paths.login}
      description="Choose a new password for your CampusOne account."
      eyebrow="Account recovery"
      title="Create a new password"
    >
      <form className="grid gap-5" noValidate onSubmit={handleSubmit}>
        {success ? (
          <div
            className="rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-3 text-sm text-emerald-700"
            role="status"
          >
            <span className="inline-flex items-center gap-2">
              <CheckCircle2 className="size-4" />
              {success}
            </span>
          </div>
        ) : null}
        {requestError ? <ErrorMessage message={requestError} /> : null}

        <PasswordField
          autoComplete="new-password"
          error={errors.newPassword}
          icon={<LockKeyhole className="size-4" />}
          label="New password"
          name="newPassword"
          onChange={updateField("newPassword")}
          placeholder="Choose a secure password"
          required
          value={form.newPassword}
        />

        <PasswordField
          autoComplete="new-password"
          error={errors.confirmPassword}
          icon={<LockKeyhole className="size-4" />}
          label="Confirm new password"
          name="confirmPassword"
          onChange={updateField("confirmPassword")}
          placeholder="Re-enter your password"
          required
          value={form.confirmPassword}
        />

        <Button
          className="w-full"
          disabled={!token || Boolean(success)}
          loading={isSubmitting}
          size="lg"
          type="submit"
        >
          {isSubmitting ? "Resetting password" : "Reset password"}
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
