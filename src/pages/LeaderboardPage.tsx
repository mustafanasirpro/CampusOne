import {
  BadgeCheck,
  ChevronRight,
  Flame,
  LockKeyhole,
  SearchX,
  Sparkles,
  Star,
  Target,
  UsersRound,
} from "lucide-react";
import { useMemo, useState } from "react";

import { LeaderboardCard, StatCard } from "@/components/cards";
import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
  EmptyState,
  PageHeader,
  SearchBar,
  SectionTitle,
  Tabs,
  useToast,
} from "@/components/common";
import {
  contributionActivity,
  departmentRankings,
  gamificationBadges,
  leaderboardByPeriod,
  leaderboardStats,
  leaderboardTabs,
  myGamificationProgress,
  weeklyChallenges,
  type LeaderboardPeriod,
  type RankedStudent,
} from "@/data/leaderboard";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const podiumStyles = {
  1: {
    wrapper: "bg-gradient-to-br from-amber-300 via-amber-100 to-orange-200",
    label: "bg-amber-100 text-amber-800 ring-amber-600/20",
    title: "Campus champion",
  },
  2: {
    wrapper: "bg-gradient-to-br from-slate-300 via-slate-100 to-slate-200",
    label: "bg-slate-100 text-slate-700 ring-slate-600/20",
    title: "Second place",
  },
  3: {
    wrapper: "bg-gradient-to-br from-orange-300 via-orange-100 to-amber-200",
    label: "bg-orange-100 text-orange-800 ring-orange-600/20",
    title: "Third place",
  },
};

const badgeTones = {
  amber: {
    icon: "bg-amber-100 text-amber-700",
    border: "hover:border-amber-200",
    progress: "bg-amber-500",
  },
  brand: {
    icon: "bg-brand-100 text-brand-700",
    border: "hover:border-brand-200",
    progress: "bg-brand-500",
  },
  emerald: {
    icon: "bg-emerald-100 text-emerald-700",
    border: "hover:border-emerald-200",
    progress: "bg-emerald-500",
  },
  sky: {
    icon: "bg-sky-100 text-sky-700",
    border: "hover:border-sky-200",
    progress: "bg-sky-500",
  },
  rose: {
    icon: "bg-rose-100 text-rose-700",
    border: "hover:border-rose-200",
    progress: "bg-rose-500",
  },
  violet: {
    icon: "bg-violet-100 text-violet-700",
    border: "hover:border-violet-200",
    progress: "bg-violet-500",
  },
};

const activityTones = {
  brand: "bg-brand-50 text-brand-600",
  emerald: "bg-emerald-50 text-emerald-600",
  amber: "bg-amber-50 text-amber-600",
  sky: "bg-sky-50 text-sky-600",
  violet: "bg-violet-50 text-violet-600",
};

interface TopStudentCardProps {
  isFollowing: boolean;
  onFollow: () => void;
  student: RankedStudent;
}

function TopStudentCard({
  isFollowing,
  onFollow,
  student,
}: TopStudentCardProps) {
  const podium = podiumStyles[student.rank as 1 | 2 | 3];

  return (
    <div
      className={cn(
        "rounded-3xl p-1 shadow-lg transition duration-200 hover:-translate-y-1 hover:shadow-xl",
        podium.wrapper,
      )}
    >
      <LeaderboardCard
        entry={{
          rank: student.rank,
          name: student.name,
          university: student.university,
          xp: student.xp,
          badges: student.badges,
        }}
      />
      <div className="m-1 mt-0 rounded-2xl bg-white/90 p-4 backdrop-blur">
        <div className="flex flex-wrap items-center gap-2">
          <Badge className={podium.label}>{podium.title}</Badge>
          <span className="text-xs text-slate-500">{student.department}</span>
        </div>
        <div className="mt-3 flex items-center justify-between gap-3">
          <div className="min-w-0">
            <p className="text-[11px] font-medium uppercase tracking-wide text-slate-400">
              Top contribution
            </p>
            <p className="mt-1 truncate text-sm font-semibold text-slate-700">
              {student.contribution}
            </p>
          </div>
          <Button
            onClick={onFollow}
            size="sm"
            variant={isFollowing ? "secondary" : "outline"}
          >
            {isFollowing ? "Following" : "Follow"}
          </Button>
        </div>
      </div>
    </div>
  );
}

interface RankedStudentRowProps {
  isFollowing: boolean;
  onFollow: () => void;
  student: RankedStudent;
}

function RankedStudentRow({
  isFollowing,
  onFollow,
  student,
}: RankedStudentRowProps) {
  return (
    <Card className="transition duration-200 hover:border-brand-200 hover:shadow-lg">
      <CardContent className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center">
        <div className="flex min-w-0 items-center gap-3 sm:w-72">
          <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-slate-100 text-sm font-bold text-slate-600">
            {student.rank}
          </span>
          <Avatar name={student.name} />
          <div className="min-w-0">
            <h3 className="truncate text-sm font-semibold text-slate-900">
              {student.name}
            </h3>
            <p className="truncate text-xs text-slate-500">
              {student.university}
            </p>
          </div>
        </div>

        <div className="grid flex-1 grid-cols-2 gap-3 sm:grid-cols-3">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
              Department
            </p>
            <p className="mt-1 truncate text-xs font-medium text-slate-700">
              {student.department}
            </p>
          </div>
          <div>
            <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
              Contribution
            </p>
            <p className="mt-1 truncate text-xs font-medium text-slate-700">
              {student.contribution}
            </p>
          </div>
          <div className="col-span-2 sm:col-span-1">
            <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
              Achievements
            </p>
            <p className="mt-1 text-xs font-medium text-slate-700">
              {student.badges} badges
            </p>
          </div>
        </div>

        <div className="flex items-center justify-between gap-4 border-t border-slate-100 pt-3 sm:border-0 sm:pt-0">
          <p className="whitespace-nowrap text-sm font-bold text-brand-700">
            {student.xp.toLocaleString()} XP
          </p>
          <Button
            onClick={onFollow}
            size="sm"
            variant={isFollowing ? "secondary" : "outline"}
          >
            {isFollowing ? "Following" : "Follow"}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export function LeaderboardPage() {
  const [period, setPeriod] = useState<LeaderboardPeriod>("weekly");
  const [searchValue, setSearchValue] = useState("");
  const [followingIds, setFollowingIds] = useState<Set<string>>(
    new Set(["sara-ahmed"]),
  );
  const [claimedChallenges, setClaimedChallenges] = useState<Set<string>>(
    new Set(),
  );
  const { showToast } = useToast();

  useDocumentTitle("Leaderboard · CampusOne");

  const filteredStudents = useMemo(() => {
    const query = searchValue.trim().toLowerCase();

    if (!query) return leaderboardByPeriod[period];

    return leaderboardByPeriod[period].filter((student) =>
      [
        student.name,
        student.university,
        student.department,
        student.contribution,
        ...student.badgeNames,
      ]
        .join(" ")
        .toLowerCase()
        .includes(query),
    );
  }, [period, searchValue]);

  const topStudents = filteredStudents.filter((student) => student.rank <= 3);
  const rankedStudents = filteredStudents.filter(
    (student) => student.rank > 3,
  );

  const toggleFollow = (student: RankedStudent) => {
    const isFollowing = followingIds.has(student.id);
    setFollowingIds((current) => {
      const next = new Set(current);
      if (next.has(student.id)) next.delete(student.id);
      else next.add(student.id);
      return next;
    });
    showToast({
      title: isFollowing ? "Unfollowed student" : "Following student",
      message: student.name,
      variant: isFollowing ? "info" : "success",
    });
  };

  const handleChallengeAction = (
    challenge: (typeof weeklyChallenges)[number],
  ) => {
    const complete = challenge.current >= challenge.target;
    if (!complete) {
      showToast({
        title: "Challenge in progress",
        message: `${challenge.target - challenge.current} more to unlock ${challenge.reward} XP.`,
      });
      return;
    }

    setClaimedChallenges((current) =>
      new Set([...current, challenge.id]),
    );
    showToast({
      title: `${challenge.reward} XP claimed`,
      message: `${challenge.title} is complete. Nice work!`,
      variant: "success",
    });
  };

  const scrollToMyRank = () => {
    document
      .getElementById("my-progress")
      ?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        actions={
          <Button onClick={scrollToMyRank}>
            <Target className="size-4" />
            My rank
          </Button>
        }
        description="Celebrate helpful students, build contribution streaks, and earn recognition for making campus life better."
        eyebrow="Campus gamification"
        title="Leaderboard"
      />

      <SearchBar
        className="max-w-3xl"
        onSearch={setSearchValue}
        onValueChange={setSearchValue}
        placeholder="Search students, departments, badges..."
        value={searchValue}
      />

      <section aria-labelledby="leaderboard-stats">
        <h2 className="sr-only" id="leaderboard-stats">
          Leaderboard statistics
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {leaderboardStats.map((stat) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={stat.label}
            >
              <StatCard
                change={stat.change}
                icon={stat.icon}
                label={stat.label}
                value={stat.value}
              />
            </div>
          ))}
        </div>
      </section>

      <section>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <SectionTitle
            description={`${filteredStudents.length} students in the ${period.replace("-", " ")} ranking.`}
            title="Campus rankings"
          />
          <Tabs
            activeTab={period}
            onChange={setPeriod}
            tabs={leaderboardTabs}
          />
        </div>

        {filteredStudents.length > 0 ? (
          <>
            {topStudents.length > 0 ? (
              <div className="mt-5 grid gap-4 lg:grid-cols-3">
                {topStudents.map((student) => (
                  <TopStudentCard
                    isFollowing={followingIds.has(student.id)}
                    key={`${period}-${student.id}`}
                    onFollow={() => toggleFollow(student)}
                    student={student}
                  />
                ))}
              </div>
            ) : null}

            {rankedStudents.length > 0 ? (
              <div className="mt-4 grid gap-3">
                {rankedStudents.map((student) => (
                  <RankedStudentRow
                    isFollowing={followingIds.has(student.id)}
                    key={`${period}-${student.id}`}
                    onFollow={() => toggleFollow(student)}
                    student={student}
                  />
                ))}
              </div>
            ) : null}
          </>
        ) : (
          <EmptyState
            action={
              <Button
                onClick={() => setSearchValue("")}
                variant="outline"
              >
                Clear search
              </Button>
            }
            className="mt-5"
            description="Try another name, university, department, contribution, or badge."
            icon={<SearchX className="size-6" />}
            title="No students found."
          />
        )}
      </section>

      <section className="scroll-mt-28" id="my-progress">
        <SectionTitle
          description="Your current momentum across the CampusOne community."
          title="My progress"
        />
        <Card className="mt-4 overflow-hidden border-0 bg-slate-950 shadow-xl shadow-slate-950/15">
          <CardContent className="relative p-6 sm:p-8">
            <div className="absolute -left-20 top-0 size-72 rounded-full bg-brand-600/25 blur-3xl" />
            <div className="absolute -bottom-24 right-0 size-80 rounded-full bg-emerald-500/15 blur-3xl" />
            <div className="relative grid gap-8 xl:grid-cols-[1fr_1.2fr] xl:items-center">
              <div>
                <div className="flex items-center gap-4">
                  <Avatar name={myGamificationProgress.name} size="xl" />
                  <div>
                    <p className="text-sm text-slate-400">Current standing</p>
                    <h3 className="mt-1 text-2xl font-bold text-white">
                      Rank #{myGamificationProgress.rank}
                    </h3>
                    <p className="mt-1 text-sm font-semibold text-brand-200">
                      Level {myGamificationProgress.level} ·{" "}
                      {myGamificationProgress.levelName}
                    </p>
                  </div>
                </div>

                <div className="mt-6">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-medium text-slate-300">
                      {myGamificationProgress.xp.toLocaleString()} XP
                    </span>
                    <span className="text-slate-400">
                      {myGamificationProgress.nextLevelXp.toLocaleString()} XP
                      to next level
                    </span>
                  </div>
                  <div className="mt-2 h-2 overflow-hidden rounded-full bg-white/10">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-brand-400 to-emerald-400"
                      style={{ width: `${myGamificationProgress.progress}%` }}
                    />
                  </div>
                </div>

                <div className="mt-6 flex items-center gap-4 rounded-2xl border border-white/10 bg-white/[0.06] p-4">
                  <span className="grid size-11 place-items-center rounded-xl bg-orange-400/15 text-orange-300">
                    <Flame className="size-5 fill-orange-400/30" />
                  </span>
                  <div>
                    <p className="text-lg font-bold text-white">
                      {myGamificationProgress.dailyStreak} day streak
                    </p>
                    <p className="text-xs text-slate-400">
                      Personal best: {myGamificationProgress.bestStreak} days
                    </p>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                {myGamificationProgress.contributions.map((item) => (
                  <div
                    className="rounded-2xl border border-white/10 bg-white/[0.06] p-4 backdrop-blur"
                    key={item.label}
                  >
                    <span className="grid size-9 place-items-center rounded-xl bg-brand-500/15 text-brand-200">
                      <item.icon className="size-4" />
                    </span>
                    <p className="mt-3 text-2xl font-bold text-white">
                      {item.value}
                    </p>
                    <p className="mt-1 text-xs text-slate-400">
                      {item.label}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      </section>

      <section>
        <SectionTitle
          description="Recognition earned through consistent, useful contributions."
          title="Badges & achievements"
        />
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {gamificationBadges.map((achievement) => {
            const tone = badgeTones[achievement.tone];

            return (
              <Card
                className={cn(
                  "group h-full transition duration-200 hover:-translate-y-1 hover:shadow-xl",
                  tone.border,
                  !achievement.earned && "bg-slate-50/70",
                )}
                key={achievement.name}
              >
                <CardContent className="flex h-full flex-col p-5">
                  <div className="flex items-start justify-between gap-3">
                    <span
                      className={cn(
                        "grid size-12 place-items-center rounded-2xl",
                        achievement.earned
                          ? tone.icon
                          : "bg-slate-200 text-slate-500",
                      )}
                    >
                      <achievement.icon className="size-5.5" />
                    </span>
                    <Badge
                      variant={achievement.earned ? "success" : "neutral"}
                    >
                      {achievement.earned ? (
                        <>
                          <BadgeCheck className="mr-1 size-3" />
                          Earned
                        </>
                      ) : (
                        <>
                          <LockKeyhole className="mr-1 size-3" />
                          Locked
                        </>
                      )}
                    </Badge>
                  </div>
                  <h3 className="mt-4 font-semibold text-slate-900">
                    {achievement.name}
                  </h3>
                  <p className="mt-2 flex-1 text-sm leading-6 text-slate-500">
                    {achievement.description}
                  </p>
                  {!achievement.earned ? (
                    <div className="mt-4">
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-slate-500">Progress</span>
                        <span className="font-bold text-slate-700">
                          {achievement.progress}%
                        </span>
                      </div>
                      <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-200">
                        <div
                          className={cn("h-full rounded-full", tone.progress)}
                          style={{ width: `${achievement.progress}%` }}
                        />
                      </div>
                    </div>
                  ) : (
                    <p className="mt-4 flex items-center gap-1.5 text-xs font-semibold text-emerald-700">
                      <Sparkles className="size-3.5" />
                      Added to your profile
                    </p>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      <section>
        <SectionTitle
          description="Complete focused actions before the weekly reset."
          title="Weekly challenges"
        />
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {weeklyChallenges.map((challenge) => {
            const complete = challenge.current >= challenge.target;
            const claimed = claimedChallenges.has(challenge.id);
            const progress = Math.min(
              (challenge.current / challenge.target) * 100,
              100,
            );

            return (
              <Card
                className="group h-full transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-lg"
                key={challenge.id}
              >
                <CardContent className="flex h-full flex-col p-5">
                  <div className="flex items-start gap-3">
                    <span
                      className={cn(
                        "grid size-11 shrink-0 place-items-center rounded-xl",
                        complete
                          ? "bg-emerald-50 text-emerald-600"
                          : "bg-brand-50 text-brand-600",
                      )}
                    >
                      <challenge.icon className="size-5" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <h3 className="font-semibold text-slate-900">
                        {challenge.title}
                      </h3>
                      <p className="mt-1 text-xs leading-5 text-slate-500">
                        {challenge.description}
                      </p>
                    </div>
                    <Badge variant="brand">+{challenge.reward} XP</Badge>
                  </div>

                  <div className="mt-5">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-medium text-slate-500">
                        {challenge.current} of {challenge.target}
                      </span>
                      <span className="text-slate-400">
                        {challenge.deadline}
                      </span>
                    </div>
                    <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className={cn(
                          "h-full rounded-full transition-all",
                          complete ? "bg-emerald-500" : "bg-brand-500",
                        )}
                        style={{ width: `${progress}%` }}
                      />
                    </div>
                  </div>

                  <Button
                    className="mt-5 w-full"
                    disabled={claimed}
                    onClick={() => handleChallengeAction(challenge)}
                    variant={complete ? "primary" : "outline"}
                  >
                    {claimed
                      ? "Reward claimed"
                      : complete
                        ? "Claim reward"
                        : "Keep going"}
                  </Button>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      <div className="grid items-start gap-8 xl:grid-cols-2">
        <section>
          <SectionTitle
            description="Fresh achievements from students across CampusOne."
            title="Contribution activity"
          />
          <Card className="mt-4">
            <CardContent className="p-5">
              <ol className="relative">
                {contributionActivity.map((activity, index) => (
                  <li
                    className={cn(
                      "relative flex gap-3 pb-6",
                      index === contributionActivity.length - 1 && "pb-0",
                    )}
                    key={activity.id}
                  >
                    {index < contributionActivity.length - 1 ? (
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
                      <p className="text-sm font-semibold text-slate-900">
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

        <section>
          <SectionTitle
            description="Collective XP earned by students in each department."
            title="Department rankings"
          />
          <Card className="mt-4">
            <CardContent className="grid gap-4 p-5">
              {departmentRankings.map((department) => {
                const relativeProgress =
                  (department.totalXp / departmentRankings[0].totalXp) * 100;

                return (
                  <div key={department.department}>
                    <div className="flex items-center gap-3">
                      <span
                        className={cn(
                          "grid size-9 shrink-0 place-items-center rounded-xl text-sm font-bold",
                          department.rank === 1
                            ? "bg-amber-100 text-amber-700"
                            : "bg-slate-100 text-slate-600",
                        )}
                      >
                        {department.rank}
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-3">
                          <h3 className="truncate text-sm font-semibold text-slate-900">
                            {department.department}
                          </h3>
                          <span className="shrink-0 text-xs font-bold text-brand-700">
                            {department.totalXp.toLocaleString()} XP
                          </span>
                        </div>
                        <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-brand-500 to-emerald-500"
                            style={{ width: `${relativeProgress}%` }}
                          />
                        </div>
                        <p className="mt-1.5 flex items-center gap-1 text-[11px] text-slate-400">
                          <UsersRound className="size-3" />
                          {department.activeStudents} active students
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}
            </CardContent>
          </Card>
        </section>
      </div>

      <Card className="overflow-hidden border-brand-200 bg-gradient-to-r from-brand-50 via-white to-emerald-50">
        <CardContent className="flex flex-col gap-5 p-6 sm:flex-row sm:items-center">
          <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-brand-600 text-white shadow-lg shadow-brand-600/20">
            <Star className="size-5 fill-white/20" />
          </span>
          <div className="flex-1">
            <h2 className="font-semibold text-slate-950">
              Every useful contribution counts
            </h2>
            <p className="mt-1 text-sm leading-6 text-slate-500">
              Share what you know, help another student, and your CampusOne
              reputation grows naturally.
            </p>
          </div>
          <Button onClick={scrollToMyRank} variant="outline">
            Review my progress
            <ChevronRight className="size-4" />
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
