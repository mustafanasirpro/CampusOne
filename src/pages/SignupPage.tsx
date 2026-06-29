import {
  ArrowRight,
  Check,
  LockKeyhole,
  Mail,
  UserRound,
} from "lucide-react";
import { useMemo, useState, type ChangeEvent, type FormEvent } from "react";

import { Button, useToast } from "@/components/common";
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
  department: string;
  email: string;
  fullName: string;
  password: string;
  semester: string;
  terms: boolean;
  university: string;
}

type SignupErrors = Partial<Record<keyof SignupForm, string>>;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const universityOptions = [
  { label: "Select your university", value: "", disabled: true },
  { label: "COMSATS", value: "COMSATS" },
  { label: "FAST", value: "FAST" },
  { label: "NUST", value: "NUST" },
  { label: "UET", value: "UET" },
  { label: "PU", value: "PU" },
  { label: "LUMS", value: "LUMS" },
];

const departmentOptions = [
  { label: "Select your department", value: "", disabled: true },
  { label: "Computer Science", value: "Computer Science" },
  { label: "Software Engineering", value: "Software Engineering" },
  { label: "AI", value: "AI" },
  { label: "Data Science", value: "Data Science" },
];

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
  const [form, setForm] = useState<SignupForm>({
    confirmPassword: "",
    department: "",
    email: "",
    fullName: "",
    password: "",
    semester: "",
    terms: false,
    university: "",
  });
  const [errors, setErrors] = useState<SignupErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { showToast } = useToast();
  const passwordStrength = useMemo(
    () => getPasswordStrength(form.password),
    [form.password],
  );

  useDocumentTitle("Create your account · CampusOne");

  const updateInput =
    (field: "fullName" | "email" | "password" | "confirmPassword") =>
    (event: ChangeEvent<HTMLInputElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: undefined }));
    };

  const updateSelect =
    (field: "university" | "department" | "semester") =>
    (event: ChangeEvent<HTMLSelectElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: undefined }));
    };

  const validate = () => {
    const nextErrors: SignupErrors = {};

    if (form.fullName.trim().length < 2) {
      nextErrors.fullName = "Enter your full name.";
    }

    if (!form.email.trim()) {
      nextErrors.email = "Enter your email address.";
    } else if (!emailPattern.test(form.email)) {
      nextErrors.email = "Enter a valid email address.";
    }

    if (!form.password) {
      nextErrors.password = "Create a password.";
    } else if (form.password.length < 8) {
      nextErrors.password = "Use at least 8 characters.";
    } else if (passwordStrength < 3) {
      nextErrors.password =
        "Add uppercase, lowercase, a number, or a symbol.";
    }

    if (!form.confirmPassword) {
      nextErrors.confirmPassword = "Confirm your password.";
    } else if (form.confirmPassword !== form.password) {
      nextErrors.confirmPassword = "Passwords do not match.";
    }

    if (!form.university) {
      nextErrors.university = "Select your university.";
    }
    if (!form.department) {
      nextErrors.department = "Select your department.";
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

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    window.setTimeout(() => {
      setIsSubmitting(false);
      showToast({
        title: "Account details accepted",
        message:
          "Your CampusOne profile is ready for backend account creation.",
        variant: "success",
      });
    }, 600);
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
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField
            autoComplete="name"
            error={errors.fullName}
            icon={<UserRound className="size-4" />}
            label="Full name"
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
                <span className="text-slate-400">8+ characters</span>
              </div>
            </div>
          </div>

          <PasswordField
            autoComplete="new-password"
            error={errors.confirmPassword}
            icon={<LockKeyhole className="size-4" />}
            label="Confirm password"
            name="confirmPassword"
            onChange={updateInput("confirmPassword")}
            placeholder="Repeat your password"
            required
            value={form.confirmPassword}
          />
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          <SelectField
            error={errors.university}
            label="University"
            name="university"
            onChange={updateSelect("university")}
            options={universityOptions}
            required
            value={form.university}
          />
          <SelectField
            error={errors.department}
            label="Department"
            name="department"
            onChange={updateSelect("department")}
            options={departmentOptions}
            required
            value={form.department}
          />
        </div>

        <SelectField
          error={errors.semester}
          label="Semester"
          name="semester"
          onChange={updateSelect("semester")}
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

        <Button className="w-full" loading={isSubmitting} size="lg" type="submit">
          {isSubmitting ? "Creating account" : "Create account"}
          {!isSubmitting ? <ArrowRight className="size-4" /> : null}
        </Button>

        <p className="flex items-center justify-center gap-1.5 text-xs text-slate-400">
          <Check className="size-3.5 text-emerald-500" />
          Your student profile remains private until you publish it.
        </p>
      </form>
    </AuthPageShell>
  );
}
