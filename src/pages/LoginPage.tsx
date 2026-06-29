import { ArrowRight, LockKeyhole, Mail } from "lucide-react";
import { useState, type ChangeEvent, type FormEvent } from "react";

import { Button, useToast } from "@/components/common";
import {
  CheckboxField,
  FormField,
  PasswordField,
} from "@/components/forms";
import { AuthPageShell } from "@/components/layout";
import { paths } from "@/routes/paths";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface LoginForm {
  email: string;
  password: string;
  remember: boolean;
}

type LoginErrors = Partial<Record<keyof LoginForm, string>>;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function LoginPage() {
  const [form, setForm] = useState<LoginForm>({
    email: "",
    password: "",
    remember: false,
  });
  const [errors, setErrors] = useState<LoginErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { showToast } = useToast();

  useDocumentTitle("Log in · CampusOne");

  const updateField =
    (field: "email" | "password") =>
    (event: ChangeEvent<HTMLInputElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: undefined }));
    };

  const validate = () => {
    const nextErrors: LoginErrors = {};

    if (!form.email.trim()) {
      nextErrors.email = "Enter your email address.";
    } else if (!emailPattern.test(form.email)) {
      nextErrors.email = "Enter a valid email address.";
    }

    if (!form.password) {
      nextErrors.password = "Enter your password.";
    } else if (form.password.length < 6) {
      nextErrors.password = "Password must contain at least 6 characters.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    window.setTimeout(() => {
      setIsSubmitting(false);
      showToast({
        title: "Welcome back",
        message:
          "Your details look good. Real authentication will be connected later.",
        variant: "success",
      });
    }, 500);
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

        <div className="flex items-start justify-between gap-4">
          <CheckboxField
            checked={form.remember}
            label="Remember me"
            name="remember"
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                remember: event.target.checked,
              }))
            }
          />
          <button
            className="shrink-0 text-sm font-semibold text-brand-700 transition hover:text-brand-800 hover:underline"
            onClick={() =>
              showToast({
                title: "Password reset",
                message:
                  "Password recovery is a demo action until authentication is connected.",
              })
            }
            type="button"
          >
            Forgot password?
          </button>
        </div>

        <Button className="w-full" loading={isSubmitting} size="lg" type="submit">
          Log in
          {!isSubmitting ? <ArrowRight className="size-4" /> : null}
        </Button>

        <div className="flex items-center gap-3">
          <span className="h-px flex-1 bg-slate-200" />
          <span className="text-xs font-medium uppercase tracking-wider text-slate-400">
            or continue with
          </span>
          <span className="h-px flex-1 bg-slate-200" />
        </div>

        <Button
          className="w-full"
          onClick={() =>
            showToast({
              title: "Google sign-in",
              message:
                "Google authentication will be available when the backend is connected.",
            })
          }
          size="lg"
          variant="outline"
        >
          <span
            aria-hidden="true"
            className="grid size-5 place-items-center rounded-full bg-white text-sm font-bold text-brand-600"
          >
            G
          </span>
          Continue with Google
        </Button>
      </form>
    </AuthPageShell>
  );
}

