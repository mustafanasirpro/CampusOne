import {
  GraduationCap,
  LockKeyhole,
  LogOut,
  Palette,
  Save,
  UserRound,
} from "lucide-react";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  getCurrentUserProfile,
  replaceCurrentUserSkills,
  updateCurrentUser,
  type CurrentUserIdentity,
  type ProfileVisibility,
  type ThemePreference,
} from "@/api/userApi";
import { useAuth } from "@/auth/useAuth";
import {
  Avatar,
  Button,
  Card,
  CardContent,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  Switch,
  useToast,
} from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  campusDepartments,
  campusUniversities,
} from "@/config/campusDirectory";
import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface SettingsDraft {
  bio: string;
  compactMode: boolean;
  departmentId: string;
  fullName: string;
  language: string;
  location: string;
  semester: string;
  skills: string;
  theme: ThemePreference;
  universityId: string;
  visibility: ProfileVisibility;
}

type SettingsErrors = Partial<
  Record<
    | "bio"
    | "departmentId"
    | "fullName"
    | "language"
    | "location"
    | "semester"
    | "skills"
    | "universityId",
    string
  >
>;

function createDraft(profile: CurrentUserIdentity): SettingsDraft {
  return {
    bio: profile.bio ?? "",
    compactMode: profile.preferences.compactMode,
    departmentId: profile.department.id,
    fullName: profile.fullName,
    language: profile.preferences.language,
    location: profile.location ?? "",
    semester: String(profile.semester),
    skills: profile.skills.join(", "),
    theme: profile.preferences.theme,
    universityId: profile.university.id,
    visibility: profile.visibility,
  };
}

function requestError(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : "Your settings could not be loaded. Please try again.";
}

const semesterOptions = [
  { disabled: true, label: "Select semester", value: "" },
  ...Array.from({ length: 8 }, (_, index) => ({
    label: `Semester ${index + 1}`,
    value: String(index + 1),
  })),
];

const visibilityOptions = [
  { label: "Public profile", value: "PUBLIC" },
  { label: "Private profile", value: "PRIVATE" },
];

const themeOptions: Array<{
  description: string;
  label: string;
  value: ThemePreference;
}> = [
  {
    description: "Use your device preference",
    label: "System",
    value: "SYSTEM",
  },
  {
    description: "Use the light interface",
    label: "Light",
    value: "LIGHT",
  },
  {
    description: "Save dark mode as your preference",
    label: "Dark",
    value: "DARK",
  },
];

const languageOptions = [
  { label: "English", value: "en" },
  { label: "Urdu", value: "ur" },
];

export function SettingsPage() {
  const [profile, setProfile] = useState<CurrentUserIdentity | null>(null);
  const [draft, setDraft] = useState<SettingsDraft | null>(null);
  const [errors, setErrors] = useState<SettingsErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const { isLoading: isAuthLoading, logout, syncCurrentUser } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  useDocumentTitle("Settings · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void getCurrentUserProfile(controller.signal)
      .then((response) => {
        if (!active) return;
        setProfile(response);
        setDraft(createDraft(response));
        setError(null);
      })
      .catch((loadError: unknown) => {
        if (active) setError(requestError(loadError));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  const universityOptions = useMemo(() => {
    const options = campusUniversities.map((university) => ({
      label: university.name,
      value: university.id,
    }));
    if (
      profile &&
      !options.some((option) => option.value === profile.university.id)
    ) {
      options.push({
        label: profile.university.name,
        value: profile.university.id,
      });
    }
    return options;
  }, [profile]);

  const departmentOptions = useMemo(() => {
    if (!draft) return [];
    const options = campusDepartments
      .filter(
        (department) => department.universityId === draft.universityId,
      )
      .map((department) => ({
        label: department.name,
        value: department.id,
      }));
    if (
      profile &&
      profile.department.universityId === draft.universityId &&
      !options.some((option) => option.value === profile.department.id)
    ) {
      options.push({
        label: profile.department.name,
        value: profile.department.id,
      });
    }
    return options;
  }, [draft, profile]);

  const updateDraft = <Key extends keyof SettingsDraft>(
    key: Key,
    value: SettingsDraft[Key],
  ) => {
    setDraft((current) => (current ? { ...current, [key]: value } : current));
    setErrors((current) => ({ ...current, [key]: undefined }));
    setSaveError(null);
  };

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!draft) return;

    const nextErrors: SettingsErrors = {};
    const semester = Number(draft.semester);
    const skills = draft.skills
      .split(",")
      .map((skill) => skill.trim())
      .filter(Boolean);

    if (
      draft.fullName.trim().length < 2 ||
      draft.fullName.trim().length > 80
    ) {
      nextErrors.fullName = "Use between 2 and 80 characters.";
    }
    if (draft.bio.trim().length > 500) {
      nextErrors.bio = "Bio cannot exceed 500 characters.";
    }
    if (draft.location.trim().length > 100) {
      nextErrors.location = "Location cannot exceed 100 characters.";
    }
    if (!universityOptions.some(({ value }) => value === draft.universityId)) {
      nextErrors.universityId = "Select your university.";
    }
    if (!departmentOptions.some(({ value }) => value === draft.departmentId)) {
      nextErrors.departmentId =
        "Select a department from your university.";
    }
    if (!Number.isInteger(semester) || semester < 1 || semester > 8) {
      nextErrors.semester = "Select a semester.";
    }
    if (!/^[A-Za-z]{2,3}(-[A-Za-z]{2})?$/.test(draft.language)) {
      nextErrors.language = "Select a supported language.";
    }
    if (
      skills.length > 20 ||
      skills.some((skill) => skill.length < 2 || skill.length > 40)
    ) {
      nextErrors.skills =
        "Use up to 20 comma-separated skills, each 2 to 40 characters.";
    }
    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    setIsSaving(true);
    setSaveError(null);
    try {
      await updateCurrentUser({
        bio: draft.bio.trim(),
        departmentId: draft.departmentId,
        fullName: draft.fullName.trim(),
        location: draft.location.trim(),
        preferences: {
          compactMode: draft.compactMode,
          language: draft.language,
          theme: draft.theme,
        },
        semester,
        universityId: draft.universityId,
        visibility: draft.visibility,
      });
      const updated = await replaceCurrentUserSkills(skills);
      setProfile(updated);
      setDraft(createDraft(updated));
      syncCurrentUser({
        email: updated.email,
        fullName: updated.fullName,
      });
      showToast({
        title: "Settings saved",
        message: "Your account preferences have been updated.",
        variant: "success",
      });
    } catch (saveRequestError) {
      setSaveError(
        saveRequestError instanceof ApiError
          ? saveRequestError.message
          : "Your settings could not be saved. Please try again.",
      );
    } finally {
      setIsSaving(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate(paths.login, { replace: true });
  };

  if (isLoading) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Loading your settings" />
      </div>
    );
  }

  if (error || !profile || !draft) {
    return (
      <ErrorMessage
        message={error ?? "Your settings could not be loaded."}
      />
    );
  }

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        description="Manage the profile and preferences supported by your CampusOne account."
        eyebrow="Personal workspace"
        title="Settings"
      />

      <Card>
        <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:p-6">
          <Avatar
            name={profile.fullName}
            size="lg"
            src={profile.avatarUrl ?? undefined}
          />
          <div className="min-w-0">
            <p className="truncate font-semibold text-slate-950">
              {profile.fullName}
            </p>
            <p className="truncate text-sm text-slate-500">
              {profile.email}
            </p>
          </div>
          <div className="sm:ml-auto">
            <p className="text-sm font-medium text-slate-700">
              {profile.university.name}
            </p>
            <p className="text-xs text-slate-500">
              {profile.department.name} · Semester {profile.semester}
            </p>
          </div>
        </CardContent>
      </Card>

      <form className="grid gap-6" noValidate onSubmit={(event) => void save(event)}>
        {saveError ? <ErrorMessage message={saveError} /> : null}

        <Card>
          <div className="flex items-start gap-3 border-b border-slate-100 px-5 py-5 sm:px-6">
            <span className="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-600">
              <UserRound className="size-4.5" />
            </span>
            <div>
              <h2 className="font-semibold text-slate-950">
                Profile information
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                These details appear on your CampusOne profile.
              </p>
            </div>
          </div>
          <CardContent className="grid gap-5 p-5 sm:p-6">
            <div className="grid gap-5 md:grid-cols-2">
              <FormField
                error={errors.fullName}
                label="Full name"
                maxLength={80}
                onChange={(event) =>
                  updateDraft("fullName", event.target.value)
                }
                required
                value={draft.fullName}
              />
              <FormField
                disabled
                hint="Contact support if you need to change your email."
                label="Email"
                value={profile.email}
              />
            </div>
            <div className="grid gap-1.5">
              <label
                className="text-sm font-semibold text-slate-700"
                htmlFor="settings-bio"
              >
                Bio
              </label>
              <textarea
                aria-describedby={
                  errors.bio ? "settings-bio-error" : undefined
                }
                aria-invalid={Boolean(errors.bio)}
                className={cn(
                  "min-h-28 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition focus:ring-4",
                  errors.bio
                    ? "border-red-300 focus:border-red-400 focus:ring-red-100"
                    : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
                )}
                id="settings-bio"
                maxLength={500}
                onChange={(event) => updateDraft("bio", event.target.value)}
                value={draft.bio}
              />
              {errors.bio ? (
                <p
                  className="text-xs font-medium text-red-600"
                  id="settings-bio-error"
                >
                  {errors.bio}
                </p>
              ) : null}
            </div>
            <div className="grid gap-5 md:grid-cols-2">
              <FormField
                error={errors.location}
                label="Location"
                maxLength={100}
                onChange={(event) =>
                  updateDraft("location", event.target.value)
                }
                placeholder="Not added yet"
                value={draft.location}
              />
              <SelectField
                label="Profile visibility"
                onChange={(event) =>
                  updateDraft(
                    "visibility",
                    event.target.value as ProfileVisibility,
                  )
                }
                options={visibilityOptions}
                value={draft.visibility}
              />
            </div>
            <FormField
              error={errors.skills}
              hint="Separate skills with commas."
              label="Skills"
              onChange={(event) =>
                updateDraft("skills", event.target.value)
              }
              placeholder="No skills added yet"
              value={draft.skills}
            />
          </CardContent>
        </Card>

        <Card>
          <div className="flex items-start gap-3 border-b border-slate-100 px-5 py-5 sm:px-6">
            <span className="grid size-10 place-items-center rounded-xl bg-emerald-50 text-emerald-600">
              <GraduationCap className="size-4.5" />
            </span>
            <div>
              <h2 className="font-semibold text-slate-950">
                Academic details
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                Campus and semester information used by CampusOne.
              </p>
            </div>
          </div>
          <CardContent className="grid gap-5 p-5 sm:grid-cols-2 sm:p-6">
            <SelectField
              error={errors.universityId}
              label="University"
              onChange={(event) => {
                updateDraft("universityId", event.target.value);
                setDraft((current) =>
                  current ? { ...current, departmentId: "" } : current,
                );
              }}
              options={universityOptions}
              required
              value={draft.universityId}
            />
            <SelectField
              error={errors.departmentId}
              label="Department"
              onChange={(event) =>
                updateDraft("departmentId", event.target.value)
              }
              options={[
                {
                  disabled: true,
                  label: "Select department",
                  value: "",
                },
                ...departmentOptions,
              ]}
              required
              value={draft.departmentId}
            />
            <SelectField
              error={errors.semester}
              label="Semester"
              onChange={(event) =>
                updateDraft("semester", event.target.value)
              }
              options={semesterOptions}
              required
              value={draft.semester}
            />
          </CardContent>
        </Card>

        <Card>
          <div className="flex items-start gap-3 border-b border-slate-100 px-5 py-5 sm:px-6">
            <span className="grid size-10 place-items-center rounded-xl bg-violet-50 text-violet-600">
              <Palette className="size-4.5" />
            </span>
            <div>
              <h2 className="font-semibold text-slate-950">Preferences</h2>
              <p className="mt-1 text-sm text-slate-500">
                Shape CampusOne around the way you like to work.
              </p>
            </div>
          </div>
          <CardContent className="grid gap-6 p-5 sm:p-6">
            <div
              aria-label="Theme preference"
              className="grid gap-3 sm:grid-cols-3"
              role="radiogroup"
            >
              {themeOptions.map((option) => (
                <button
                  aria-checked={draft.theme === option.value}
                  className={cn(
                    "rounded-2xl border p-4 text-left transition",
                    draft.theme === option.value
                      ? "border-brand-300 bg-brand-50 ring-2 ring-brand-100"
                      : "border-slate-200 bg-white hover:border-slate-300",
                  )}
                  key={option.value}
                  onClick={() => updateDraft("theme", option.value)}
                  role="radio"
                  type="button"
                >
                  <span className="font-semibold text-slate-900">
                    {option.label}
                  </span>
                  <span className="mt-1 block text-xs text-slate-500">
                    {option.description}
                  </span>
                </button>
              ))}
            </div>
            <SelectField
              error={errors.language}
              label="Preferred language"
              onChange={(event) =>
                updateDraft("language", event.target.value)
              }
              options={languageOptions}
              value={draft.language}
            />
            <Switch
              checked={draft.compactMode}
              description="Save a denser layout preference for supported screens."
              label="Compact mode"
              onCheckedChange={(checked) =>
                updateDraft("compactMode", checked)
              }
            />
          </CardContent>
        </Card>

        <div className="flex justify-end">
          <Button loading={isSaving} size="lg" type="submit">
            <Save className="size-4" />
            Save settings
          </Button>
        </div>
      </form>

      <Card className="border-red-200 bg-red-50/40">
        <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
          <div className="flex items-start gap-3">
            <span className="grid size-10 place-items-center rounded-xl bg-red-100 text-red-600">
              <LockKeyhole className="size-4.5" />
            </span>
            <div>
              <h2 className="font-semibold text-slate-950">
                End this session
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                Log out of CampusOne on this device.
              </p>
            </div>
          </div>
          <Button
            loading={isAuthLoading}
            onClick={() => void handleLogout()}
            variant="danger"
          >
            <LogOut className="size-4" />
            Log out
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
