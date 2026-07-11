import { ArrowRight, LockKeyhole, Mail } from "lucide-react";
import { useState, type ChangeEvent, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { useAuth } from "@/auth/useAuth";
import { Button, ErrorMessage } from "@/components/common";
import { FormField, PasswordField } from "@/components/forms";
import { AuthPageShell } from "@/components/layout";
import { paths } from "@/routes/paths";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface LoginForm {
  email: string;
  password: string;
}

interface LoginLocationState {
  accountCreated?: boolean;
  email?: string;
  from?: string;
}

type LoginErrors = Partial<Record<keyof LoginForm, string>>;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function LoginPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const locationState = location.state as LoginLocationState | null;
  const [form, setForm] = useState<LoginForm>({
    email: locationState?.email ?? "",
    password: "",
  });
  const [errors, setErrors] = useState<LoginErrors>({});
  const { authError, clearAuthError, isLoading, login } = useAuth();

  useDocumentTitle("Log in · CampusOne");

  const updateField =
    (field: keyof LoginForm) =>
    (event: ChangeEvent<HTMLInputElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: undefined }));
      clearAuthError();
    };

  const validate = () => {
    const nextErrors: LoginErrors = {};

    if (!form.email.trim()) {
      nextErrors.email = "Enter your email address.";
    } else if (!emailPattern.test(form.email.trim())) {
      nextErrors.email = "Enter a valid email address.";
    }

    if (!form.password) {
      nextErrors.password = "Enter your password.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;

    try {
      await login({
        email: form.email.trim(),
        password: form.password,
      });
      const destination =
        locationState?.from?.startsWith("/") === true &&
        !locationState.from.startsWith("//")
          ? locationState.from
          : paths.dashboard;
      navigate(destination, { replace: true });
    } catch {
      // AuthContext exposes a safe, user-facing error message.
    }
  };

  return (
    <AuthPageShell
      alternateAction="Create an account"
      alternatePrompt="New to CampusOne?"
      alternateTo={paths.signup}
      description="Welcome back. Enter your details to continue to your campus."
      eyebrow="Welcome back"
      title="Log in to CampusOne"
    >
      <form className="grid gap-5" noValidate onSubmit={handleSubmit}>
        {locationState?.accountCreated ? (
          <div
            className="rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-3 text-sm text-emerald-700"
            role="status"
          >
            Account created successfully. Log in to enter CampusOne.
          </div>
        ) : null}

        {authError ? <ErrorMessage message={authError} /> : null}

        <FormField
          autoComplete="email"
          error={errors.email}
          icon={<Mail className="size-4" />}
          label="Email address"
          name="email"
          onChange={updateField("email")}
          placeholder="you@university.edu.pk"
          required
          type="email"
          value={form.email}
        />

        <PasswordField
          autoComplete="current-password"
          error={errors.password}
          icon={<LockKeyhole className="size-4" />}
          label="Password"
          name="password"
          onChange={updateField("password")}
          placeholder="Enter your password"
          required
          value={form.password}
        />

        <div className="-mt-2 flex justify-end">
          <Link
            className="text-sm font-semibold text-brand-700 hover:text-brand-800"
            to={paths.forgotPassword}
          >
            Forgot password?
          </Link>
        </div>

        <Button className="w-full" loading={isLoading} size="lg" type="submit">
          {isLoading ? "Logging in" : "Log in"}
          {!isLoading ? <ArrowRight className="size-4" /> : null}
        </Button>

        <p className="text-center text-xs leading-5 text-slate-400">
          Your session is protected with secure sign-in.
        </p>
      </form>
    </AuthPageShell>
  );
}
