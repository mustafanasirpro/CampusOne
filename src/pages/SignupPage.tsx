import {
  ArrowRight,
  Check,
  GraduationCap,
  LockKeyhole,
  Mail,
  School,
  UserRound,
} from "lucide-react";
import { useMemo, useState, type ChangeEvent, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";

import { useAuth } from "@/auth/useAuth";
import { Button, ErrorMessage } from "@/components/common";
import {
  CheckboxField,
  FormField,
  PasswordField,
  SelectField,
} from "@/components/forms";
import { AuthPageShell } from "@/components/layout";
import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface SignupForm {
  confirmPassword: string;
  departmentId: string;
  email: string;
  fullName: string;
  password: string;
  semester: string;
  terms: boolean;
  universityId: string;
}

type SignupErrors = Partial<Record<keyof SignupForm, string>>;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const semesterOptions = [
  { label: "Select semester", value: "", disabled: true },
  ...Array.from({ length: 8 }, (_, index) => ({
    label: `Semester ${index + 1}`,
    value: String(index + 1),
  })),
];

function getPasswordStrength(password: string) {
  if (!password) return 0;

  return [
    password.length >= 8,
    /[a-z]/.test(password) && /[A-Z]/.test(password),
    /\d/.test(password),
    /[^A-Za-z0-9]/.test(password),
  ].filter(Boolean).length;
}

const strengthLabels = ["Enter a password", "Weak", "Fair", "Good", "Strong"];
const strengthColors = [
  "bg-slate-200",
  "bg-red-500",
  "bg-amber-500",
  "bg-brand-500",
  "bg-emerald-500",
];

export function SignupPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<SignupForm>({
    confirmPassword: "",
    departmentId: "",
    email: "",
    fullName: "",
    password: "",
    semester: "",
    terms: false,
    universityId: "",
  });
  const [errors, setErrors] = useState<SignupErrors>({});
  const { authError, clearAuthError, isLoading, register } = useAuth();
  const passwordStrength = useMemo(
    () => getPasswordStrength(form.password),
    [form.password],
  );

  useDocumentTitle("Create your account · CampusOne");

  const updateInput =
    (
      field:
        | "fullName"
        | "email"
        | "password"
        | "confirmPassword"
        | "universityId"
        | "departmentId",
    ) =>
    (event: ChangeEvent<HTMLInputElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: undefined }));
      clearAuthError();
    };

  const validate = () => {
    const nextErrors: SignupErrors = {};

    if (form.fullName.trim().length < 2) {
      nextErrors.fullName = "Enter your full name.";
    } else if (form.fullName.trim().length > 80) {
      nextErrors.fullName = "Full name cannot exceed 80 characters.";
    }

    if (!form.email.trim()) {
      nextErrors.email = "Enter your email address.";
    } else if (!emailPattern.test(form.email)) {
      nextErrors.email = "Enter a valid email address.";
    }

    if (!form.password) {
      nextErrors.password = "Create a password.";
    } else if (form.password.length < 8 || form.password.length > 72) {
      nextErrors.password = "Use between 8 and 72 characters.";
    } else if (
      !/[a-z]/.test(form.password) ||
      !/[A-Z]/.test(form.password) ||
      !/\d/.test(form.password)
    ) {
      nextErrors.password =
        "Include an uppercase letter, a lowercase letter, and a number.";
    }

    if (!form.confirmPassword) {
      nextErrors.confirmPassword = "Confirm your password.";
    } else if (form.confirmPassword !== form.password) {
      nextErrors.confirmPassword = "Passwords do not match.";
    }

    if (!uuidPattern.test(form.universityId.trim())) {
      nextErrors.universityId = "Enter a valid university UUID.";
    }
    if (!uuidPattern.test(form.departmentId.trim())) {
      nextErrors.departmentId = "Enter a valid department UUID.";
    }
    if (!form.semester) {
      nextErrors.semester = "Select your semester.";
    }
    if (!form.terms) {
      nextErrors.terms = "Accept the terms to create your account.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;

    try {
      await register({
        departmentId: form.departmentId.trim(),
        email: form.email.trim(),
        fullName: form.fullName.trim(),
        password: form.password,
        semester: Number(form.semester),
        universityId: form.universityId.trim(),
      });
      navigate(paths.login, {
        replace: true,
        state: {
          accountCreated: true,
          email: form.email.trim(),
        },
      });
    } catch {
      // AuthContext exposes a safe, user-facing error message.
    }
  };

  return (
    <AuthPageShell
      alternateAction="Log in"
      alternatePrompt="Already have an account?"
      alternateTo={paths.login}
      description="Join your university community and make campus life easier."
      eyebrow="Join CampusOne"
      title="Create your student account"
    >
      <form className="grid gap-5" noValidate onSubmit={handleSubmit}>
        {authError ? <ErrorMessage message={authError} /> : null}

        <div className="grid gap-5 sm:grid-cols-2">
          <FormField
            autoComplete="name"
            error={errors.fullName}
            icon={<UserRound className="size-4" />}
            label="Full name"
            maxLength={80}
            name="fullName"
            onChange={updateInput("fullName")}
            placeholder="Ali Khan"
            required
            value={form.fullName}
          />
          <FormField
            autoComplete="email"
            error={errors.email}
            icon={<Mail className="size-4" />}
            label="Email address"
            maxLength={254}
            name="email"
            onChange={updateInput("email")}
            placeholder="you@university.edu.pk"
            required
            type="email"
            value={form.email}
          />
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          <div>
            <PasswordField
              autoComplete="new-password"
              error={errors.password}
              icon={<LockKeyhole className="size-4" />}
              label="Password"
              maxLength={72}
              name="password"
              onChange={updateInput("password")}
              placeholder="Create a strong password"
              required
              value={form.password}
            />
            <div aria-live="polite" className="mt-2">
              <div className="grid grid-cols-4 gap-1.5">
                {Array.from({ length: 4 }, (_, index) => (
                  <span
                    className={cn(
                      "h-1.5 rounded-full transition-colors",
                      index < passwordStrength
                        ? strengthColors[passwordStrength]
                        : "bg-slate-200",
                    )}
                    key={index}
                  />
                ))}
              </div>
              <div className="mt-1.5 flex items-center justify-between text-[11px]">
                <span
                  className={cn(
                    "font-semibold",
                    passwordStrength === 0 && "text-slate-400",
                    passwordStrength === 1 && "text-red-600",
                    passwordStrength === 2 && "text-amber-600",
                    passwordStrength === 3 && "text-brand-600",
                    passwordStrength === 4 && "text-emerald-600",
                  )}
                >
                  {strengthLabels[passwordStrength]}
                </span>
                <span className="text-slate-400">
                  Uppercase, lowercase, number
                </span>
              </div>
            </div>
          </div>

          <PasswordField
            autoComplete="new-password"
            error={errors.confirmPassword}
            icon={<LockKeyhole className="size-4" />}
            label="Confirm password"
            maxLength={72}
            name="confirmPassword"
            onChange={updateInput("confirmPassword")}
            placeholder="Repeat your password"
            required
            value={form.confirmPassword}
          />
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          <FormField
            autoComplete="off"
            error={errors.universityId}
            hint="Use the UUID from the backend academic directory."
            icon={<School className="size-4" />}
            label="University ID"
            name="universityId"
            onChange={updateInput("universityId")}
            placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            required
            value={form.universityId}
          />
          <FormField
            autoComplete="off"
            error={errors.departmentId}
            hint="The department must belong to the selected university."
            icon={<GraduationCap className="size-4" />}
            label="Department ID"
            name="departmentId"
            onChange={updateInput("departmentId")}
            placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            required
            value={form.departmentId}
          />
        </div>

        <SelectField
          error={errors.semester}
          label="Semester"
          name="semester"
          onChange={(event) => {
            setForm((current) => ({
              ...current,
              semester: event.target.value,
            }));
            setErrors((current) => ({
              ...current,
              semester: undefined,
            }));
            clearAuthError();
          }}
          options={semesterOptions}
          required
          value={form.semester}
        />

        <CheckboxField
          checked={form.terms}
          error={errors.terms}
          label={
            <>
              I agree to the{" "}
              <span className="font-semibold text-slate-800">
                Terms of Service
              </span>{" "}
              and{" "}
              <span className="font-semibold text-slate-800">
                Privacy Policy
              </span>
              .
            </>
          }
          name="terms"
          onChange={(event) => {
            setForm((current) => ({
              ...current,
              terms: event.target.checked,
            }));
            setErrors((current) => ({ ...current, terms: undefined }));
          }}
        />

        <Button className="w-full" loading={isLoading} size="lg" type="submit">
          {isLoading ? "Creating account" : "Create account"}
          {!isLoading ? <ArrowRight className="size-4" /> : null}
        </Button>

        <p className="flex items-center justify-center gap-1.5 text-xs text-slate-400">
          <Check className="size-3.5 text-emerald-500" />
          Registration creates your account; you will log in on the next step.
        </p>
      </form>
    </AuthPageShell>
  );
}
