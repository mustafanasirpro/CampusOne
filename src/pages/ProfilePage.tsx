import {
  ArrowRight,
  Bookmark,
  CheckCircle2,
  Download,
  Eye,
  FileText,
  GraduationCap,
  MapPin,
  Pencil,
  Plus,
  Share2,
  Sparkles,
  Star,
} from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";

import { DiscussionCard, StatCard } from "@/components/cards";
import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
  Modal,
  SectionTitle,
  useToast,
} from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  initialStudentProfile,
  profileAchievements,
  profileActivity,
  profileDepartmentOptions,
  profileDiscussions,
  profileNotes,
  profileSemesterOptions,
  profileStats,
  profileUniversityOptions,
  type StudentProfile,
} from "@/data/profile";
import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const achievementTones = {
  amber: {
    icon: "bg-amber-100 text-amber-700",
    border: "group-hover:border-amber-200",
    progress: "bg-amber-500",
  },
  brand: {
    icon: "bg-brand-100 text-brand-700",
    border: "group-hover:border-brand-200",
    progress: "bg-brand-500",
  },
  emerald: {
    icon: "bg-emerald-100 text-emerald-700",
    border: "group-hover:border-emerald-200",
    progress: "bg-emerald-500",
  },
  sky: {
    icon: "bg-sky-100 text-sky-700",
    border: "group-hover:border-sky-200",
    progress: "bg-sky-500",
  },
  rose: {
    icon: "bg-rose-100 text-rose-700",
    border: "group-hover:border-rose-200",
    progress: "bg-rose-500",
  },
  violet: {
    icon: "bg-violet-100 text-violet-700",
    border: "group-hover:border-violet-200",
    progress: "bg-violet-500",
  },
};

const activityTones = {
  brand: "bg-brand-50 text-brand-600 ring-brand-100",
  emerald: "bg-emerald-50 text-emerald-600 ring-emerald-100",
  amber: "bg-amber-50 text-amber-600 ring-amber-100",
  sky: "bg-sky-50 text-sky-600 ring-sky-100",
  rose: "bg-rose-50 text-rose-600 ring-rose-100",
};

interface ProfileDraft {
  bio: string;
  department: string;
  fullName: string;
  semester: string;
  skills: string;
  university: string;
}

type ProfileErrors = Partial<Record<keyof ProfileDraft, string>>;

function createDraft(profile: StudentProfile): ProfileDraft {
  return {
    fullName: profile.fullName,
    bio: profile.bio,
    university: profile.university,
    department: profile.department,
    semester: profile.semester,
    skills: profile.skills.join(", "),
  };
}

export function ProfilePage() {
  const [profile, setProfile] = useState(initialStudentProfile);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [draft, setDraft] = useState<ProfileDraft>(() =>
    createDraft(initialStudentProfile),
  );
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [bookmarkedNotes, setBookmarkedNotes] = useState<Set<string>>(
    new Set(["database-cheatsheet"]),
  );
  const [upvotedDiscussions, setUpvotedDiscussions] = useState<Set<string>>(
    new Set(),
  );
  const { showToast } = useToast();
  const navigate = useNavigate();

  useDocumentTitle(`${profile.fullName} · CampusOne`);

  const openEditProfile = () => {
    setDraft(createDraft(profile));
    setErrors({});
    setIsEditOpen(true);
  };

  const updateDraft = (field: keyof ProfileDraft, value: string) => {
    setDraft((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  };

  const handleSaveProfile = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: ProfileErrors = {};
    const skills = draft.skills
      .split(",")
      .map((skill) => skill.trim())
      .filter(Boolean);

    if (draft.fullName.trim().length < 2) {
      nextErrors.fullName = "Enter your full name.";
    }
    if (draft.bio.trim().length < 20) {
      nextErrors.bio = "Write at least 20 characters about yourself.";
    }
    if (!draft.university) nextErrors.university = "Select a university.";
    if (!draft.department) nextErrors.department = "Select a department.";
    if (!draft.semester) nextErrors.semester = "Select a semester.";
    if (skills.length === 0) {
      nextErrors.skills = "Add at least one skill.";
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    setProfile((current) => ({
      ...current,
      fullName: draft.fullName.trim(),
      bio: draft.bio.trim(),
      university: draft.university,
      department: draft.department,
      semester: draft.semester,
      skills,
    }));
    setIsEditOpen(false);
    showToast({
      title: "Profile updated",
      message: "Your changes are visible in this frontend demo.",
      variant: "success",
    });
  };

  const handleShare = async () => {
    const shareData = {
      title: `${profile.fullName} on CampusOne`,
      text: `View ${profile.fullName}’s student profile on CampusOne.`,
      url: window.location.href,
    };

    if (navigator.share) {
      try {
        await navigator.share(shareData);
        return;
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") return;
      }
    }

    try {
      await navigator.clipboard.writeText(window.location.href);
      showToast({
        title: "Profile link copied",
        message: "Share it with classmates or recruiters.",
        variant: "success",
      });
    } catch {
      showToast({
        title: "Profile ready to share",
        message: window.location.href,
      });
    }
  };

  const toggleNoteBookmark = (id: string, title: string) => {
    const isBookmarked = bookmarkedNotes.has(id);
    setBookmarkedNotes((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
    showToast({
      title: isBookmarked ? "Bookmark removed" : "Note bookmarked",
      message: title,
      variant: isBookmarked ? "info" : "success",
    });
  };

  const toggleDiscussionUpvote = (title: string) => {
    const isUpvoted = upvotedDiscussions.has(title);
    setUpvotedDiscussions((current) => {
      const next = new Set(current);
      if (next.has(title)) next.delete(title);
      else next.add(title);
      return next;
    });
    showToast({
      title: isUpvoted ? "Upvote removed" : "Discussion upvoted",
      message: title,
      variant: isUpvoted ? "info" : "success",
    });
  };

  return (
    <div className="grid gap-8 pb-8">
      <Card className="overflow-hidden">
        <div className="relative h-36 overflow-hidden bg-slate-950 sm:h-44">
          <div className="absolute -left-20 -top-32 size-96 rounded-full bg-brand-600/35 blur-3xl" />
          <div className="absolute -bottom-36 right-0 size-96 rounded-full bg-emerald-500/20 blur-3xl" />
          <div
            className="absolute inset-0 opacity-[0.08]"
            style={{
              backgroundImage:
                "linear-gradient(rgba(255,255,255,.4) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.4) 1px, transparent 1px)",
              backgroundSize: "32px 32px",
            }}
          />
          <div className="relative flex h-full items-start justify-between p-5 sm:p-7">
            <Badge className="gap-1.5 bg-white/10 text-brand-100 ring-white/10">
              <Sparkles className="size-3.5" />
              CampusOne student profile
            </Badge>
            <div className="hidden rounded-xl border border-white/10 bg-white/[0.07] px-3 py-2 text-right backdrop-blur sm:block">
              <p className="text-xs text-slate-400">Community rank</p>
              <p className="mt-0.5 text-sm font-bold text-white">Top 8%</p>
            </div>
          </div>
        </div>

        <CardContent className="relative p-5 sm:p-7">
          <div className="-mt-16 flex flex-col gap-5 sm:-mt-20">
            <div className="flex flex-col items-start gap-4 sm:flex-row sm:items-end sm:justify-between">
              <div className="rounded-full bg-white p-1.5 shadow-xl ring-1 ring-slate-200">
                <Avatar name={profile.fullName} size="xl" />
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
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">
                  {profile.fullName}
                </h1>
                <span
                  aria-label="Verified CampusOne student"
                  className="grid size-5 place-items-center rounded-full bg-brand-600 text-white"
                  title="Verified student"
                >
                  <CheckCircle2 className="size-3.5" />
                </span>
              </div>
              <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-slate-500">
                <span className="flex items-center gap-1.5 font-medium text-slate-700">
                  <GraduationCap className="size-4 text-brand-500" />
                  {profile.university}
                </span>
                <span>{profile.department}</span>
                <span>{profile.semester}</span>
                <span className="flex items-center gap-1.5">
                  <MapPin className="size-4" />
                  {profile.location}
                </span>
              </div>
              <p className="mt-4 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
                {profile.bio}
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
          {profileStats.map((stat) => (
            <div
              className="min-w-0 transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={stat.label}
            >
              <StatCard
                description={stat.description}
                icon={stat.icon}
                label={stat.label}
                value={stat.value.toLocaleString()}
              />
            </div>
          ))}
        </div>
      </section>

      <Card>
        <CardContent className="p-5 sm:p-6">
          <SectionTitle
            action={
              <Button onClick={openEditProfile} size="sm" variant="outline">
                <Plus className="size-3.5" />
                Add skill
              </Button>
            }
            description={`Topics and tools ${profile.fullName.split(" ")[0]} is comfortable working with.`}
            title="Skills & expertise"
          />
          <div className="mt-5 flex flex-wrap gap-2.5">
            {profile.skills.map((skill) => (
              <Badge
                className="border border-brand-100 bg-brand-50 px-3 py-1.5 text-brand-700 transition hover:-translate-y-0.5 hover:bg-brand-100"
                key={skill}
                variant="brand"
              >
                {skill}
              </Badge>
            ))}
          </div>
        </CardContent>
      </Card>

      <div className="grid items-start gap-8 xl:grid-cols-[minmax(0,1.55fr)_minmax(20rem,0.65fr)]">
        <section>
          <SectionTitle
            description="Milestones earned through helpful campus contributions."
            title="Badges & achievements"
          />
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            {profileAchievements.map((achievement) => {
              const tone = achievementTones[achievement.tone];

              return (
                <Card
                  className={cn(
                    "group h-full transition duration-200 hover:-translate-y-1 hover:shadow-xl",
                    tone.border,
                  )}
                  key={achievement.name}
                >
                  <CardContent className="flex h-full flex-col p-5">
                    <div className="flex items-start gap-4">
                      <span
                        className={cn(
                          "grid size-12 shrink-0 place-items-center rounded-2xl",
                          tone.icon,
                        )}
                      >
                        <achievement.icon className="size-5.5" />
                      </span>
                      <div>
                        <h3 className="font-semibold text-slate-950">
                          {achievement.name}
                        </h3>
                        <p className="mt-1 text-sm leading-6 text-slate-500">
                          {achievement.description}
                        </p>
                      </div>
                    </div>
                    {achievement.earned ? (
                      <div className="mt-5 flex items-center gap-2 border-t border-slate-100 pt-4 text-xs font-semibold text-emerald-700">
                        <CheckCircle2 className="size-4" />
                        {achievement.earned}
                      </div>
                    ) : (
                      <div className="mt-5 border-t border-slate-100 pt-4">
                        <div className="flex items-center justify-between text-xs">
                          <span className="font-medium text-slate-500">
                            Badge progress
                          </span>
                          <span className="font-bold text-slate-700">
                            {achievement.progress}%
                          </span>
                        </div>
                        <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100">
                          <div
                            className={cn("h-full rounded-full", tone.progress)}
                            style={{ width: `${achievement.progress}%` }}
                          />
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </section>

        <section className="xl:sticky xl:top-28">
          <SectionTitle
            description="Recent contributions across CampusOne."
            title="Contribution activity"
          />
          <Card className="mt-4">
            <CardContent className="p-5">
              <ol className="relative">
                {profileActivity.map((activity, index) => (
                  <li
                    className={cn(
                      "relative flex gap-3 pb-6",
                      index === profileActivity.length - 1 && "pb-0",
                    )}
                    key={activity.id}
                  >
                    {index < profileActivity.length - 1 ? (
                      <span className="absolute bottom-0 left-5 top-10 w-px bg-slate-200" />
                    ) : null}
                    <span
                      className={cn(
                        "z-10 grid size-10 shrink-0 place-items-center rounded-xl ring-4 ring-white",
                        activityTones[activity.tone],
                      )}
                    >
                      <activity.icon className="size-4" />
                    </span>
                    <div className="min-w-0 pt-0.5">
                      <p className="text-sm font-semibold leading-5 text-slate-900">
                        {activity.title}
                      </p>
                      <p className="mt-1 text-xs leading-5 text-slate-500">
                        {activity.description}
                      </p>
                      <p className="mt-1 text-[11px] font-medium text-slate-400">
                        {activity.time}
                      </p>
                    </div>
                  </li>
                ))}
              </ol>
            </CardContent>
          </Card>
        </section>
      </div>

      <section>
        <SectionTitle
          action={
            <Link
              className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 transition hover:text-brand-800"
              to={paths.notes}
            >
              View all notes
              <ArrowRight className="size-3.5" />
            </Link>
          }
          description="Study resources shared with the CampusOne community."
          title="Uploaded notes"
        />
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {profileNotes.map((note) => {
            const isBookmarked = bookmarkedNotes.has(note.id);

            return (
              <Card
                className="group transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl"
                key={note.id}
              >
                <CardContent className="p-5">
                  <div className="flex items-start gap-3">
                    <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600">
                      <FileText className="size-5" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <Badge variant="brand">{note.course}</Badge>
                      <h3 className="mt-2 font-semibold leading-6 text-slate-950">
                        {note.title}
                      </h3>
                    </div>
                    <Button
                      aria-label={
                        isBookmarked ? "Remove bookmark" : "Bookmark note"
                      }
                      onClick={() =>
                        toggleNoteBookmark(note.id, note.title)
                      }
                      size="icon"
                      variant="ghost"
                    >
                      <Bookmark
                        className={cn(
                          "size-4.5",
                          isBookmarked &&
                            "fill-brand-600 text-brand-600",
                        )}
                      />
                    </Button>
                  </div>
                  <div className="mt-5 flex items-center gap-4 border-t border-slate-100 pt-4 text-xs text-slate-500">
                    <span className="flex items-center gap-1">
                      <Download className="size-3.5" />
                      {note.downloads} downloads
                    </span>
                    <span className="flex items-center gap-1">
                      <Star className="size-3.5 fill-amber-400 text-amber-400" />
                      {note.rating}
                    </span>
                    <Button
                      className="ml-auto"
                      onClick={() => navigate(paths.notes)}
                      size="sm"
                      variant="ghost"
                    >
                      <Eye className="size-3.5" />
                      View
                    </Button>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      <section>
        <SectionTitle
          action={
            <Link
              className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 transition hover:text-brand-800"
              to={paths.discussions}
            >
              View all discussions
              <ArrowRight className="size-3.5" />
            </Link>
          }
          description={`Questions, answers, and ideas shared by ${profile.fullName.split(" ")[0]}.`}
          title="Recent discussions"
        />
        <div className="mt-4 grid gap-4 lg:grid-cols-2 2xl:grid-cols-3">
          {profileDiscussions.map((discussion) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={discussion.title}
            >
              <DiscussionCard
                discussion={{
                  ...discussion,
                  upvotes:
                    discussion.upvotes +
                    (upvotedDiscussions.has(discussion.title) ? 1 : 0),
                }}
                onOpen={() => navigate(paths.discussions)}
                onUpvote={() => toggleDiscussionUpvote(discussion.title)}
              />
            </div>
          ))}
        </div>
      </section>

      <Modal
        description="Update the student information shown on your CampusOne profile."
        footer={
          <>
            <Button
              onClick={() => setIsEditOpen(false)}
              type="button"
              variant="ghost"
            >
              Cancel
            </Button>
            <Button form="edit-profile-form" type="submit">
              Save changes
            </Button>
          </>
        }
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        size="lg"
        title="Edit profile"
      >
        <form
          className="grid gap-5"
          id="edit-profile-form"
          noValidate
          onSubmit={handleSaveProfile}
        >
          <FormField
            error={errors.fullName}
            label="Full name"
            onChange={(event) => updateDraft("fullName", event.target.value)}
            placeholder="Your full name"
            required
            value={draft.fullName}
          />

          <div className="grid gap-1.5">
            <label
              className="text-sm font-semibold text-slate-700"
              htmlFor="edit-profile-bio"
            >
              Bio
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </label>
            <textarea
              aria-describedby={
                errors.bio ? "edit-profile-bio-error" : undefined
              }
              aria-invalid={Boolean(errors.bio)}
              className={cn(
                "min-h-28 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:ring-4",
                errors.bio
                  ? "border-red-300 focus:border-red-400 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              id="edit-profile-bio"
              maxLength={280}
              onChange={(event) => updateDraft("bio", event.target.value)}
              placeholder="Tell the community what you study and enjoy building."
              value={draft.bio}
            />
            <div className="flex items-center justify-between gap-3">
              {errors.bio ? (
                <p
                  className="text-xs font-medium text-red-600"
                  id="edit-profile-bio-error"
                >
                  {errors.bio}
                </p>
              ) : (
                <span />
              )}
              <span className="text-xs text-slate-400">
                {draft.bio.length}/280
              </span>
            </div>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={errors.university}
              label="University"
              onChange={(event) =>
                updateDraft("university", event.target.value)
              }
              options={profileUniversityOptions}
              required
              value={draft.university}
            />
            <SelectField
              error={errors.department}
              label="Department"
              onChange={(event) =>
                updateDraft("department", event.target.value)
              }
              options={profileDepartmentOptions}
              required
              value={draft.department}
            />
          </div>

          <SelectField
            error={errors.semester}
            label="Semester"
            onChange={(event) => updateDraft("semester", event.target.value)}
            options={profileSemesterOptions}
            required
            value={draft.semester}
          />

          <FormField
            error={errors.skills}
            hint="Separate skills with commas."
            label="Skills"
            onChange={(event) => updateDraft("skills", event.target.value)}
            placeholder="React, Java, UI/UX"
            required
            value={draft.skills}
          />
        </form>
      </Modal>
    </div>
  );
}
