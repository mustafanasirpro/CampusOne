import {
  Bell,
  Check,
  Eye,
  Laptop,
  LockKeyhole,
  LogOut,
  Monitor,
  Moon,
  Palette,
  Save,
  ShieldCheck,
  SlidersHorizontal,
  Smartphone,
  Sun,
  UserRound,
} from "lucide-react";
import { useState, type FormEvent, type ReactNode } from "react";

import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
  Modal,
  PageHeader,
  Switch,
  useToast,
} from "@/components/common";
import { FormField, PasswordField, SelectField } from "@/components/forms";
import {
  activeSessions as initialActiveSessions,
  departmentOptions,
  initialAccount,
  initialNotificationPreferences,
  initialPrivacyPreferences,
  initialProfilePreferences,
  languageOptions,
  notificationPreferences,
  privacyPreferences,
  profileVisibilityOptions,
  semesterOptions,
  themeOptions,
  universityOptions,
  type ThemePreference,
} from "@/data/settings";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const settingsNavigation = [
  { id: "account", label: "Account", icon: UserRound },
  { id: "profile", label: "Preferences", icon: SlidersHorizontal },
  { id: "notifications", label: "Notifications", icon: Bell },
  { id: "privacy", label: "Privacy", icon: Eye },
  { id: "appearance", label: "Theme", icon: Palette },
  { id: "security", label: "Security", icon: LockKeyhole },
];

interface SettingsSectionProps {
  children: ReactNode;
  description: string;
  icon: typeof UserRound;
  id: string;
  title: string;
}

function SettingsSection({
  children,
  description,
  icon: Icon,
  id,
  title,
}: SettingsSectionProps) {
  return (
    <Card className="scroll-mt-28 overflow-hidden" id={id}>
      <div className="flex items-start gap-3 border-b border-slate-100 px-5 py-5 sm:px-6">
        <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600">
          <Icon className="size-4.5" />
        </span>
        <div>
          <h2 className="font-semibold text-slate-950">{title}</h2>
          <p className="mt-1 text-sm leading-6 text-slate-500">
            {description}
          </p>
        </div>
      </div>
      <CardContent className="p-5 sm:p-6">{children}</CardContent>
    </Card>
  );
}

export function SettingsPage() {
  const [account, setAccount] = useState(initialAccount);
  const [profilePreferences, setProfilePreferences] = useState(
    initialProfilePreferences,
  );
  const [notificationSettings, setNotificationSettings] = useState(
    initialNotificationPreferences,
  );
  const [privacySettings, setPrivacySettings] = useState(
    initialPrivacyPreferences,
  );
  const [profileVisibility, setProfileVisibility] = useState("community");
  const [theme, setTheme] = useState<ThemePreference>("light");
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(false);
  const [sessions, setSessions] = useState(initialActiveSessions);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [isLogoutOpen, setIsLogoutOpen] = useState(false);
  const { showToast } = useToast();

  useDocumentTitle("Settings · CampusOne");

  const saveSettings = (section: string) => {
    setIsSaving(true);
    window.setTimeout(() => {
      setIsSaving(false);
      showToast({
        title: `${section} saved`,
        message: "Your frontend preferences have been updated.",
        variant: "success",
      });
    }, 450);
  };

  const handleAccountSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    saveSettings("Account settings");
  };

  const handlePasswordSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (newPassword.length < 8) {
      showToast({
        title: "Use a stronger password",
        message: "Your new password must contain at least 8 characters.",
        variant: "error",
      });
      return;
    }
    if (newPassword !== confirmPassword) {
      showToast({
        title: "Passwords do not match",
        message: "Check the confirmation field and try again.",
        variant: "error",
      });
      return;
    }

    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    showToast({
      title: "Password updated",
      message: "This is a secure frontend-only confirmation.",
      variant: "success",
    });
  };

  const removeSession = (sessionId: string) => {
    const session = sessions.find((item) => item.id === sessionId);
    setSessions((current) =>
      current.filter((item) => item.id !== sessionId),
    );
    showToast({
      title: "Session signed out",
      message: session?.device ?? "The selected session was removed.",
      variant: "success",
    });
  };

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        actions={
          <Button
            loading={isSaving}
            onClick={() => saveSettings("All settings")}
          >
            <Save className="size-4" />
            Save all changes
          </Button>
        }
        description="Manage your CampusOne account, experience, privacy, and security preferences."
        eyebrow="Personal workspace"
        title="Settings"
      />

      <div className="grid items-start gap-6 xl:grid-cols-[240px_minmax(0,1fr)]">
        <aside className="xl:sticky xl:top-28">
          <Card className="overflow-hidden">
            <CardContent className="p-3">
              <div className="flex items-center gap-3 border-b border-slate-100 p-3">
                <Avatar name={account.fullName} size="lg" />
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-slate-950">
                    {account.fullName}
                  </p>
                  <p className="truncate text-xs text-slate-500">
                    {account.studentId}
                  </p>
                </div>
              </div>
              <nav
                aria-label="Settings sections"
                className="mt-2 flex gap-1 overflow-x-auto xl:grid"
              >
                {settingsNavigation.map((item) => (
                  <a
                    className="flex h-10 shrink-0 items-center gap-2.5 rounded-xl px-3 text-sm font-medium text-slate-600 transition hover:bg-brand-50 hover:text-brand-700"
                    href={`#${item.id}`}
                    key={item.id}
                  >
                    <item.icon className="size-4 text-slate-400" />
                    {item.label}
                  </a>
                ))}
              </nav>
            </CardContent>
          </Card>
        </aside>

        <div className="grid min-w-0 gap-6">
          <SettingsSection
            description="Keep your contact and university account details current."
            icon={UserRound}
            id="account"
            title="Account settings"
          >
            <form className="grid gap-5" onSubmit={handleAccountSubmit}>
              <div className="grid gap-4 md:grid-cols-2">
                <FormField
                  autoComplete="name"
                  label="Full name"
                  onChange={(event) =>
                    setAccount((current) => ({
                      ...current,
                      fullName: event.target.value,
                    }))
                  }
                  required
                  value={account.fullName}
                />
                <FormField
                  autoComplete="email"
                  label="University email"
                  onChange={(event) =>
                    setAccount((current) => ({
                      ...current,
                      email: event.target.value,
                    }))
                  }
                  required
                  type="email"
                  value={account.email}
                />
                <FormField
                  autoComplete="tel"
                  label="Phone number"
                  onChange={(event) =>
                    setAccount((current) => ({
                      ...current,
                      phone: event.target.value,
                    }))
                  }
                  type="tel"
                  value={account.phone}
                />
                <FormField
                  disabled
                  hint="Contact campus support to update your student ID."
                  label="Student ID"
                  value={account.studentId}
                />
              </div>
              <div>
                <Button loading={isSaving} type="submit">
                  Save account details
                </Button>
              </div>
            </form>
          </SettingsSection>

          <SettingsSection
            description="Tune CampusOne around your degree and study habits."
            icon={SlidersHorizontal}
            id="profile"
            title="Profile preferences"
          >
            <div className="grid gap-5">
              <div className="grid gap-4 md:grid-cols-2">
                <SelectField
                  label="University"
                  onChange={(event) =>
                    setProfilePreferences((current) => ({
                      ...current,
                      university: event.target.value,
                    }))
                  }
                  options={universityOptions}
                  value={profilePreferences.university}
                />
                <SelectField
                  label="Department"
                  onChange={(event) =>
                    setProfilePreferences((current) => ({
                      ...current,
                      department: event.target.value,
                    }))
                  }
                  options={departmentOptions}
                  value={profilePreferences.department}
                />
                <SelectField
                  label="Semester"
                  onChange={(event) =>
                    setProfilePreferences((current) => ({
                      ...current,
                      semester: event.target.value,
                    }))
                  }
                  options={semesterOptions}
                  value={profilePreferences.semester}
                />
                <SelectField
                  label="Preferred content language"
                  onChange={(event) =>
                    setProfilePreferences((current) => ({
                      ...current,
                      language: event.target.value,
                    }))
                  }
                  options={languageOptions}
                  value={profilePreferences.language}
                />
              </div>
              <div className="border-t border-slate-100 pt-3">
                <Switch
                  checked={profilePreferences.compactDashboard}
                  description="Show denser dashboard cards and activity lists."
                  label="Compact dashboard"
                  onCheckedChange={(checked) =>
                    setProfilePreferences((current) => ({
                      ...current,
                      compactDashboard: checked,
                    }))
                  }
                />
              </div>
              <div>
                <Button onClick={() => saveSettings("Profile preferences")}>
                  Save preferences
                </Button>
              </div>
            </div>
          </SettingsSection>

          <SettingsSection
            description="Choose which campus updates deserve your attention."
            icon={Bell}
            id="notifications"
            title="Notification preferences"
          >
            <div className="divide-y divide-slate-100">
              {notificationPreferences.map((preference) => (
                <Switch
                  checked={notificationSettings[preference.key]}
                  className="py-4 first:pt-0 last:pb-0"
                  description={preference.description}
                  key={preference.key}
                  label={preference.label}
                  onCheckedChange={(checked) =>
                    setNotificationSettings((current) => ({
                      ...current,
                      [preference.key]: checked,
                    }))
                  }
                />
              ))}
            </div>
            <Button
              className="mt-5"
              onClick={() => saveSettings("Notification preferences")}
            >
              Save notification settings
            </Button>
          </SettingsSection>

          <SettingsSection
            description="Control who can discover your profile and activity."
            icon={Eye}
            id="privacy"
            title="Privacy settings"
          >
            <div className="grid gap-5">
              <SelectField
                label="Profile visibility"
                onChange={(event) => setProfileVisibility(event.target.value)}
                options={profileVisibilityOptions}
                value={profileVisibility}
              />
              <div className="divide-y divide-slate-100 border-t border-slate-100">
                {privacyPreferences.map((preference) => (
                  <Switch
                    checked={privacySettings[preference.key]}
                    className="py-4 last:pb-0"
                    description={preference.description}
                    key={preference.key}
                    label={preference.label}
                    onCheckedChange={(checked) =>
                      setPrivacySettings((current) => ({
                        ...current,
                        [preference.key]: checked,
                      }))
                    }
                  />
                ))}
              </div>
              <div>
                <Button onClick={() => saveSettings("Privacy settings")}>
                  Save privacy settings
                </Button>
              </div>
            </div>
          </SettingsSection>

          <SettingsSection
            description="Choose a comfortable visual mode for your workspace."
            icon={Palette}
            id="appearance"
            title="Theme settings"
          >
            <div
              aria-label="Theme preference"
              className="grid gap-3 sm:grid-cols-3"
              role="radiogroup"
            >
              {themeOptions.map((option) => {
                const isSelected = theme === option.id;
                const ThemeIcon =
                  option.id === "light"
                    ? Sun
                    : option.id === "dark"
                      ? Moon
                      : Monitor;

                return (
                  <button
                    aria-checked={isSelected}
                    className={cn(
                      "relative rounded-2xl border p-4 text-left transition hover:-translate-y-0.5 hover:shadow-lg",
                      isSelected
                        ? "border-brand-300 bg-brand-50 ring-2 ring-brand-100"
                        : "border-slate-200 bg-white hover:border-slate-300",
                    )}
                    key={option.id}
                    onClick={() => {
                      setTheme(option.id);
                      showToast({
                        title: `${option.title} theme selected`,
                        message: "Your visual preference is reflected in this preview.",
                      });
                    }}
                    role="radio"
                    type="button"
                  >
                    <span
                      className={cn(
                        "grid size-10 place-items-center rounded-xl",
                        option.id === "dark"
                          ? "bg-slate-900 text-white"
                          : option.id === "system"
                            ? "bg-sky-50 text-sky-600"
                            : "bg-amber-50 text-amber-600",
                      )}
                    >
                      <ThemeIcon className="size-4.5" />
                    </span>
                    <span className="mt-3 block text-sm font-semibold text-slate-900">
                      {option.title}
                    </span>
                    <span className="mt-1 block text-xs text-slate-500">
                      {option.description}
                    </span>
                    {isSelected ? (
                      <span className="absolute right-3 top-3 grid size-5 place-items-center rounded-full bg-brand-600 text-white">
                        <Check className="size-3" />
                      </span>
                    ) : null}
                  </button>
                );
              })}
            </div>

            <div
              className={cn(
                "mt-5 rounded-2xl border p-5 transition-colors",
                theme === "dark"
                  ? "border-slate-800 bg-slate-950 text-white"
                  : theme === "system"
                    ? "border-sky-200 bg-gradient-to-r from-white to-sky-50"
                    : "border-slate-200 bg-slate-50",
              )}
            >
              <p className="text-sm font-semibold">Theme preview</p>
              <p
                className={cn(
                  "mt-1 text-xs leading-5",
                  theme === "dark" ? "text-slate-400" : "text-slate-500",
                )}
              >
                CampusOne keeps contrast, typography, and focus states readable
                in your selected interface mode.
              </p>
            </div>
          </SettingsSection>

          <SettingsSection
            description="Protect your account and review signed-in devices."
            icon={LockKeyhole}
            id="security"
            title="Security settings"
          >
            <div className="grid gap-6">
              <div className="rounded-2xl border border-slate-200 p-4">
                <div className="flex items-start gap-3">
                  <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-600">
                    <ShieldCheck className="size-4.5" />
                  </span>
                  <div className="flex-1">
                    <Switch
                      checked={twoFactorEnabled}
                      className="py-0"
                      description="Add a verification step when signing in on a new device."
                      label="Two-factor authentication"
                      onCheckedChange={(checked) => {
                        setTwoFactorEnabled(checked);
                        showToast({
                          title: checked
                            ? "Two-factor authentication enabled"
                            : "Two-factor authentication disabled",
                          message: "This security setting is simulated locally.",
                          variant: checked ? "success" : "info",
                        });
                      }}
                    />
                  </div>
                </div>
              </div>

              <form
                className="grid gap-4 rounded-2xl border border-slate-200 p-4"
                onSubmit={handlePasswordSubmit}
              >
                <div>
                  <h3 className="text-sm font-semibold text-slate-900">
                    Change password
                  </h3>
                  <p className="mt-1 text-xs text-slate-500">
                    Use at least eight characters and avoid reused passwords.
                  </p>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <PasswordField
                    autoComplete="current-password"
                    className="md:col-span-2"
                    label="Current password"
                    onChange={(event) => setCurrentPassword(event.target.value)}
                    required
                    value={currentPassword}
                  />
                  <PasswordField
                    autoComplete="new-password"
                    label="New password"
                    onChange={(event) => setNewPassword(event.target.value)}
                    required
                    value={newPassword}
                  />
                  <PasswordField
                    autoComplete="new-password"
                    label="Confirm new password"
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    required
                    value={confirmPassword}
                  />
                </div>
                <div>
                  <Button type="submit">Update password</Button>
                </div>
              </form>

              <div>
                <h3 className="text-sm font-semibold text-slate-900">
                  Active sessions
                </h3>
                <p className="mt-1 text-xs text-slate-500">
                  Devices currently signed in to your CampusOne account.
                </p>
                <div className="mt-3 divide-y divide-slate-100 rounded-2xl border border-slate-200">
                  {sessions.map((session) => {
                    const DeviceIcon = session.current ? Laptop : Smartphone;

                    return (
                      <div
                        className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center"
                        key={session.id}
                      >
                        <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-600">
                          <DeviceIcon className="size-4.5" />
                        </span>
                        <div className="min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="text-sm font-semibold text-slate-900">
                              {session.device}
                            </p>
                            {session.current ? (
                              <Badge variant="success">Current session</Badge>
                            ) : null}
                          </div>
                          <p className="mt-1 text-xs text-slate-500">
                            {session.location} · {session.lastActive}
                          </p>
                        </div>
                        {!session.current ? (
                          <Button
                            onClick={() => removeSession(session.id)}
                            size="sm"
                            variant="outline"
                          >
                            Sign out
                          </Button>
                        ) : null}
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </SettingsSection>

          <Card className="border-red-200 bg-red-50/40">
            <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
              <div>
                <h2 className="font-semibold text-slate-950">Log out</h2>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  End this CampusOne session on the current device.
                </p>
              </div>
              <Button
                onClick={() => setIsLogoutOpen(true)}
                variant="danger"
              >
                <LogOut className="size-4" />
                Log out
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>

      <Modal
        description="You will return to the login screen when authentication is connected."
        footer={
          <>
            <Button onClick={() => setIsLogoutOpen(false)} variant="outline">
              Stay signed in
            </Button>
            <Button
              onClick={() => {
                setIsLogoutOpen(false);
                showToast({
                  title: "Demo session ended",
                  message: "No real session was changed in this frontend build.",
                  variant: "success",
                });
              }}
              variant="danger"
            >
              <LogOut className="size-4" />
              Log out
            </Button>
          </>
        }
        isOpen={isLogoutOpen}
        onClose={() => setIsLogoutOpen(false)}
        size="sm"
        title="Log out of CampusOne?"
      >
        <div className="flex items-center gap-3 rounded-2xl bg-slate-50 p-4">
          <Avatar name={account.fullName} />
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-slate-900">
              {account.fullName}
            </p>
            <p className="truncate text-xs text-slate-500">{account.email}</p>
          </div>
        </div>
      </Modal>
    </div>
  );
}
