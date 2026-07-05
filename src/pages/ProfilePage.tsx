import {
  Award,
  BookOpen,
  CheckCircle2,
  Flame,
  GraduationCap,
  MapPin,
  MessageSquareText,
  Pencil,
  Share2,
  Sparkles,
  Trophy,
} from "lucide-react";
import { useEffect, useMemo, useState, type FormEvent } from "react";

import { ApiError } from "@/api/apiClient";
import { getMyQuestions } from "@/api/discussionApi";
import {
  getMyGamificationProfile,
  getMyXpHistory,
} from "@/api/gamificationApi";
import { getMyNotes } from "@/api/notesApi";
import {
  getCurrentUserProfile,
  replaceCurrentUserSkills,
  updateCurrentUser,
  type CurrentUserIdentity,
} from "@/api/userApi";
import { useAuth } from "@/auth/useAuth";
import { StatCard } from "@/components/cards";
import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  Modal,
  SectionTitle,
  useToast,
} from "@/components/common";
import { QuestionCard } from "@/components/discussion";
import { FormField } from "@/components/forms";
import { NoteCard } from "@/components/notes";
import type {
  DiscussionQuestionPage,
} from "@/types/discussion";
import type {
  GamificationProfile,
  XpHistoryPage,
} from "@/types/gamification";
import type { NotePage } from "@/types/notes";
import { cn } from "@/utils/cn";
import { formatDateTime } from "@/utils/format";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface ProfileDraft {
  bio: string;
  fullName: string;
  location: string;
  skills: string;
}

type ProfileErrors = Partial<Record<keyof ProfileDraft, string>>;

function requestErrorMessage(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : "Your profile could not be loaded. Please try again.";
}

function profileDraft(profile: CurrentUserIdentity): ProfileDraft {
  return {
    bio: profile.bio ?? "",
    fullName: profile.fullName,
    location: profile.location ?? "",
    skills: profile.skills.join(", "),
  };
}

export function ProfilePage() {
  const [profile, setProfile] = useState<CurrentUserIdentity | null>(null);
  const [gamification, setGamification] =
    useState<GamificationProfile | null>(null);
  const [notes, setNotes] = useState<NotePage | null>(null);
  const [questions, setQuestions] =
    useState<DiscussionQuestionPage | null>(null);
  const [history, setHistory] = useState<XpHistoryPage | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [draft, setDraft] = useState<ProfileDraft>({
    bio: "",
    fullName: "",
    location: "",
    skills: "",
  });
  const [errors, setErrors] = useState<ProfileErrors>({});
  const { syncCurrentUser } = useAuth();
  const { showToast } = useToast();

  useDocumentTitle(
    profile ? `${profile.fullName} · CampusOne` : "Profile · CampusOne",
  );

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    void Promise.all([
      getCurrentUserProfile(controller.signal),
      getMyGamificationProfile(controller.signal),
      getMyNotes({ page: 0, signal: controller.signal, size: 3 }),
      getMyQuestions({ page: 0, signal: controller.signal, size: 3 }),
      getMyXpHistory(0, 5, controller.signal),
    ])
      .then(
        ([
          profileResponse,
          gamificationResponse,
          notesResponse,
          questionsResponse,
          historyResponse,
        ]) => {
          if (!active) return;
          setProfile(profileResponse);
          setGamification(gamificationResponse);
          setNotes(notesResponse);
          setQuestions(questionsResponse);
          setHistory(historyResponse);
          setError(null);
        },
      )
      .catch((requestError: unknown) => {
        if (active) setError(requestErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  const stats = useMemo(
    () => [
      {
        description: "Community contribution points",
        icon: Trophy,
        label: "Total XP",
        value: gamification?.totalXp ?? profile?.totalXp ?? 0,
      },
      {
        description: "Calculated from earned XP",
        icon: Sparkles,
        label: "Level",
        value: gamification?.level ?? 1,
      },
      {
        description: "Current activity streak",
        icon: Flame,
        label: "Streak",
        value: gamification?.currentStreak ?? 0,
      },
      {
        description: "Study resources you shared",
        icon: BookOpen,
        label: "Notes",
        value: notes?.totalElements ?? 0,
      },
      {
        description: "Discussion questions you asked",
        icon: MessageSquareText,
        label: "Questions",
        value: questions?.totalElements ?? 0,
      },
      {
        description: "Gamification badges earned",
        icon: Award,
        label: "Badges",
        value: gamification?.badges.length ?? 0,
      },
    ],
    [gamification, notes, profile, questions],
  );

  const openEditProfile = () => {
    if (!profile) return;
    setDraft(profileDraft(profile));
    setErrors({});
    setSaveError(null);
    setIsEditOpen(true);
  };

  const updateDraft = (field: keyof ProfileDraft, value: string) => {
    setDraft((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setSaveError(null);
  };

  const handleSaveProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: ProfileErrors = {};
    const skills = draft.skills
      .split(",")
      .map((skill) => skill.trim())
      .filter(Boolean);

    if (draft.fullName.trim().length < 2) {
      nextErrors.fullName = "Enter at least 2 characters.";
    } else if (draft.fullName.trim().length > 80) {
      nextErrors.fullName = "Full name cannot exceed 80 characters.";
    }
    if (draft.bio.trim().length > 500) {
      nextErrors.bio = "Bio cannot exceed 500 characters.";
    }
    if (draft.location.trim().length > 100) {
      nextErrors.location = "Location cannot exceed 100 characters.";
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
        fullName: draft.fullName.trim(),
        location: draft.location.trim(),
      });
      const updated = await replaceCurrentUserSkills(skills);
      setProfile(updated);
      syncCurrentUser({
        email: updated.email,
        fullName: updated.fullName,
      });
      setIsEditOpen(false);
      showToast({
        title: "Profile updated",
        message: "Your CampusOne profile has been saved.",
        variant: "success",
      });
    } catch (requestError) {
      setSaveError(
        requestError instanceof ApiError
          ? requestError.message
          : "Your profile could not be saved. Please try again.",
      );
    } finally {
      setIsSaving(false);
    }
  };

  const handleShare = async () => {
    if (!profile) return;
    const shareData = {
      title: `${profile.fullName} on CampusOne`,
      text: `View ${profile.fullName}'s student profile on CampusOne.`,
      url: window.location.href,
    };

    if (navigator.share) {
      try {
        await navigator.share(shareData);
        return;
      } catch (shareError) {
        if (
          shareError instanceof DOMException &&
          shareError.name === "AbortError"
        ) {
          return;
        }
      }
    }

    try {
      await navigator.clipboard.writeText(window.location.href);
      showToast({
        title: "Profile link copied",
        message: "The link is ready to share.",
        variant: "success",
      });
    } catch {
      showToast({
        title: "Profile link",
        message: window.location.href,
      });
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Loading your profile" />
      </div>
    );
  }

  if (error || !profile) {
    return (
      <ErrorMessage
        message={error ?? "Your profile could not be loaded."}
      />
    );
  }

  return (
    <div className="grid gap-8 pb-8">
      <Card className="overflow-hidden">
        <div
          className="relative h-36 overflow-hidden bg-slate-950 bg-cover bg-center sm:h-44"
          style={
            profile.coverImageUrl
              ? { backgroundImage: `url("${profile.coverImageUrl}")` }
              : undefined
          }
        >
          {!profile.coverImageUrl ? (
            <>
              <div className="absolute -left-20 -top-32 size-96 rounded-full bg-brand-600/35 blur-3xl" />
              <div className="absolute -bottom-36 right-0 size-96 rounded-full bg-emerald-500/20 blur-3xl" />
            </>
          ) : null}
          <div className="relative flex h-full items-start justify-between p-5 sm:p-7">
            <Badge className="gap-1.5 bg-white/10 text-brand-100 ring-white/10">
              <Sparkles className="size-3.5" />
              CampusOne student profile
            </Badge>
            <Badge className="bg-white/10 text-white ring-white/10">
              {profile.visibility === "PUBLIC"
                ? "Public profile"
                : "Private profile"}
            </Badge>
          </div>
        </div>

        <CardContent className="relative p-5 sm:p-7">
          <div className="-mt-16 flex flex-col gap-5 sm:-mt-20">
            <div className="flex flex-col items-start gap-4 sm:flex-row sm:items-end sm:justify-between">
              <div className="rounded-full bg-white p-1.5 shadow-xl ring-1 ring-slate-200">
                <Avatar
                  name={profile.fullName}
                  size="xl"
                  src={profile.avatarUrl ?? undefined}
                />
              </div>
              <div className="flex w-full flex-wrap gap-2 sm:w-auto sm:justify-end">
                <Button
                  className="flex-1 sm:flex-none"
                  onClick={openEditProfile}
                  variant="outline"
                >
                  <Pencil className="size-4" />
                  Edit profile
                </Button>
                <Button
                  className="flex-1 sm:flex-none"
                  onClick={() => void handleShare()}
                  variant="secondary"
                >
                  <Share2 className="size-4" />
                  Share profile
                </Button>
              </div>
            </div>

            <div className="max-w-3xl">
              <h1 className="text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">
                {profile.fullName}
              </h1>
              <p className="mt-1 text-sm text-slate-500">{profile.email}</p>
              <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-slate-500">
                <span className="flex items-center gap-1.5 font-medium text-slate-700">
                  <GraduationCap className="size-4 text-brand-500" />
                  {profile.university?.name || "University not added yet"}
                </span>
                <span>
                  {profile.department?.name || "Department not added yet"}
                </span>
                <span>Semester {profile.semester}</span>
                <span className="flex items-center gap-1.5">
                  <MapPin className="size-4" />
                  {profile.location || "Location not added yet"}
                </span>
              </div>
              <p className="mt-4 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
                {profile.bio || "Bio not added yet."}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <section aria-labelledby="profile-stats">
        <h2 className="sr-only" id="profile-stats">
          Profile statistics
        </h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          {stats.map((stat) => (
            <StatCard
              description={stat.description}
              icon={stat.icon}
              key={stat.label}
              label={stat.label}
              value={stat.value.toLocaleString()}
            />
          ))}
        </div>
      </section>

      <Card>
        <CardContent className="p-5 sm:p-6">
          <SectionTitle
            action={
              <Button onClick={openEditProfile} size="sm" variant="outline">
                <Pencil className="size-3.5" />
                Edit skills
              </Button>
            }
            description="Topics and tools saved on your CampusOne profile."
            title="Skills & expertise"
          />
          {profile.skills.length > 0 ? (
            <div className="mt-5 flex flex-wrap gap-2.5">
              {profile.skills.map((skill) => (
                <Badge className="px-3 py-1.5" key={skill} variant="brand">
                  {skill}
                </Badge>
              ))}
            </div>
          ) : (
            <p className="mt-5 text-sm text-slate-500">
              No skills added yet.
            </p>
          )}
        </CardContent>
      </Card>

      <div className="grid items-start gap-8 xl:grid-cols-[minmax(0,1.2fr)_minmax(20rem,0.8fr)]">
        <section>
          <SectionTitle
            description="Badges awarded by the CampusOne gamification system."
            title="Earned badges"
          />
          {gamification && gamification.badges.length > 0 ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {gamification.badges.map((userBadge) => (
                <Card key={userBadge.badge.id}>
                  <CardContent className="flex gap-4 p-5">
                    <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-amber-100 text-amber-700">
                      <Award className="size-5" />
                    </span>
                    <div>
                      <h3 className="font-semibold text-slate-950">
                        {userBadge.badge.name}
                      </h3>
                      <p className="mt-1 text-sm leading-6 text-slate-500">
                        {userBadge.badge.description}
                      </p>
                      <p className="mt-2 flex items-center gap-1.5 text-xs font-medium text-emerald-700">
                        <CheckCircle2 className="size-3.5" />
                        Earned {formatDateTime(userBadge.awardedAt)}
                      </p>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : (
            <EmptyState
              className="mt-4 min-h-52"
              description="Contribute around CampusOne to unlock your first badge."
              icon={<Award className="size-6" />}
              title="No badges earned yet"
            />
          )}
        </section>

        <section>
          <SectionTitle
            description="XP activity recorded by the gamification system."
            title="Recent XP activity"
          />
          {history && history.content.length > 0 ? (
            <Card className="mt-4">
              <CardContent className="divide-y divide-slate-100 p-5">
                {history.content.map((transaction) => (
                  <div
                    className="flex items-start gap-3 py-4 first:pt-0 last:pb-0"
                    key={transaction.id}
                  >
                    <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-emerald-50 font-bold text-emerald-700">
                      +{transaction.points}
                    </span>
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-slate-900">
                        {transaction.description ||
                          transaction.actionType
                            .toLowerCase()
                            .replaceAll("_", " ")}
                      </p>
                      <p className="mt-1 text-xs text-slate-400">
                        {formatDateTime(transaction.createdAt)}
                      </p>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          ) : (
            <EmptyState
              className="mt-4 min-h-52"
              description="Your XP transactions will appear here after eligible contributions."
              icon={<Sparkles className="size-6" />}
              title="No XP activity yet"
            />
          )}
        </section>
      </div>

      <section>
        <SectionTitle
          description="Your latest study resources from the Notes module."
          title="My recent notes"
        />
        {notes && notes.content.length > 0 ? (
          <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {notes.content.map((note) => (
              <NoteCard key={note.id} note={note} owned />
            ))}
          </div>
        ) : (
          <EmptyState
            className="mt-4 min-h-52"
            description="Notes you share will appear here."
            icon={<BookOpen className="size-6" />}
            title="No notes uploaded yet"
          />
        )}
      </section>

      <section>
        <SectionTitle
          description="Your latest questions from the Discussion module."
          title="My recent questions"
        />
        {questions && questions.content.length > 0 ? (
          <div className="mt-4 grid gap-4 lg:grid-cols-2">
            {questions.content.map((question) => (
              <QuestionCard key={question.id} owned question={question} />
            ))}
          </div>
        ) : (
          <EmptyState
            className="mt-4 min-h-52"
            description="Questions you ask will appear here."
            icon={<MessageSquareText className="size-6" />}
            title="No discussion questions yet"
          />
        )}
      </section>

      <Modal
        description="These changes are saved to your CampusOne account."
        footer={
          <>
            <Button
              disabled={isSaving}
              onClick={() => setIsEditOpen(false)}
              type="button"
              variant="ghost"
            >
              Cancel
            </Button>
            <Button
              form="edit-profile-form"
              loading={isSaving}
              type="submit"
            >
              Save changes
            </Button>
          </>
        }
        isOpen={isEditOpen}
        onClose={() => {
          if (!isSaving) setIsEditOpen(false);
        }}
        size="lg"
        title="Edit profile"
      >
        <form
          className="grid gap-5"
          id="edit-profile-form"
          noValidate
          onSubmit={(event) => void handleSaveProfile(event)}
        >
          {saveError ? <ErrorMessage message={saveError} /> : null}
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

          <div className="grid gap-1.5">
            <label
              className="text-sm font-semibold text-slate-700"
              htmlFor="edit-profile-bio"
            >
              Bio
            </label>
            <textarea
              aria-describedby={
                errors.bio ? "edit-profile-bio-error" : undefined
              }
              aria-invalid={Boolean(errors.bio)}
              className={cn(
                "min-h-28 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition placeholder:text-slate-400 focus:ring-4",
                errors.bio
                  ? "border-red-300 focus:border-red-400 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              id="edit-profile-bio"
              maxLength={500}
              onChange={(event) => updateDraft("bio", event.target.value)}
              placeholder="Tell the community what you study and enjoy building."
              value={draft.bio}
            />
            {errors.bio ? (
              <p
                className="text-xs font-medium text-red-600"
                id="edit-profile-bio-error"
              >
                {errors.bio}
              </p>
            ) : null}
          </div>

          <FormField
            error={errors.location}
            label="Location"
            maxLength={100}
            onChange={(event) =>
              updateDraft("location", event.target.value)
            }
            placeholder="Islamabad, Pakistan"
            value={draft.location}
          />
          <FormField
            error={errors.skills}
            hint="Separate skills with commas. Leave blank if you have not added any yet."
            label="Skills"
            onChange={(event) =>
              updateDraft("skills", event.target.value)
            }
            placeholder="Java, PostgreSQL, React"
            value={draft.skills}
          />
        </form>
      </Modal>
    </div>
  );
}
