import {
  ArrowLeft,
  ArrowRight,
  Award,
  History,
  Trophy,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  getLeaderboard,
  getMyBadges,
  getMyGamificationProfile,
  getMyXpHistory,
  listBadges,
} from "@/api/gamificationApi";
import {
  Button,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  SectionTitle,
  Tabs,
} from "@/components/common";
import {
  BadgeCard,
  GamificationProfileCard,
  LeaderboardTable,
  XpHistoryList,
} from "@/components/gamification";
import type {
  GamificationBadge,
  GamificationProfile,
  LeaderboardPage,
  LeaderboardPeriod,
  UserBadge,
  XpHistoryPage,
} from "@/types/gamification";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const pageSize = 10;

export function LeaderboardPage() {
  const [profile, setProfile] = useState<GamificationProfile | null>(null);
  const [badges, setBadges] = useState<GamificationBadge[]>([]);
  const [earnedBadges, setEarnedBadges] = useState<UserBadge[]>([]);
  const [leaderboard, setLeaderboard] = useState<LeaderboardPage | null>(null);
  const [history, setHistory] = useState<XpHistoryPage | null>(null);
  const [period, setPeriod] = useState<LeaderboardPeriod>("ALL_TIME");
  const [leaderboardPage, setLeaderboardPage] = useState(0);
  const [historyPage, setHistoryPage] = useState(0);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [leaderboardError, setLeaderboardError] = useState<string | null>(null);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [isProfileLoading, setIsProfileLoading] = useState(true);
  const [isLeaderboardLoading, setIsLeaderboardLoading] = useState(true);
  const [isHistoryLoading, setIsHistoryLoading] = useState(true);

  useDocumentTitle("Leaderboard · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void Promise.all([
      getMyGamificationProfile(controller.signal),
      listBadges(controller.signal),
      getMyBadges(controller.signal),
    ])
      .then(([profileResponse, badgeResponse, earnedResponse]) => {
        if (!active) return;
        setProfile(profileResponse);
        setBadges(badgeResponse);
        setEarnedBadges(earnedResponse);
        setProfileError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setProfileError(
          requestError instanceof ApiError
            ? requestError.message
            : "Your gamification profile could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsProfileLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void getLeaderboard(
      period,
      leaderboardPage,
      pageSize,
      controller.signal,
    )
      .then((response) => {
        if (!active) return;
        setLeaderboard(response);
        setLeaderboardError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setLeaderboardError(
          requestError instanceof ApiError
            ? requestError.message
            : "The leaderboard could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLeaderboardLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [leaderboardPage, period]);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void getMyXpHistory(historyPage, pageSize, controller.signal)
      .then((response) => {
        if (!active) return;
        setHistory(response);
        setHistoryError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setHistoryError(
          requestError instanceof ApiError
            ? requestError.message
            : "XP history could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsHistoryLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [historyPage]);

  const earnedCodes = useMemo(
    () => new Set(earnedBadges.map((item) => item.badge.code)),
    [earnedBadges],
  );

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        description="Track contribution XP, earn badges, and celebrate students helping the CampusOne community."
        eyebrow="Campus gamification"
        title="Leaderboard"
      />

      {profileError ? <ErrorMessage message={profileError} /> : null}
      {isProfileLoading ? (
        <div className="grid min-h-48 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading your progress" />
        </div>
      ) : profile ? (
        <GamificationProfileCard profile={profile} />
      ) : null}

      <section className="grid gap-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <SectionTitle
            description="Rankings reflect XP earned during the selected period."
            title="Campus rankings"
          />
          <Tabs
            activeTab={period}
            onChange={(value) => {
              setPeriod(value);
              setLeaderboardPage(0);
              setIsLeaderboardLoading(true);
            }}
            tabs={[
              { label: "All time", value: "ALL_TIME" },
              { label: "Monthly", value: "MONTHLY" },
              { label: "Weekly", value: "WEEKLY" },
            ]}
          />
        </div>
        {leaderboardError ? <ErrorMessage message={leaderboardError} /> : null}
        {isLeaderboardLoading ? (
          <div className="grid min-h-48 place-items-center rounded-2xl border border-slate-200 bg-white">
            <LoadingSpinner label="Loading rankings" />
          </div>
        ) : leaderboard && leaderboard.content.length > 0 ? (
          <>
            <LeaderboardTable
              currentUserId={profile?.userId ?? null}
              entries={leaderboard.content}
            />
            <Pagination
              first={leaderboard.first}
              label={`${leaderboard.page + 1} / ${Math.max(1, leaderboard.totalPages)}`}
              last={leaderboard.last}
              onNext={() => {
                setLeaderboardPage((value) => value + 1);
                setIsLeaderboardLoading(true);
              }}
              onPrevious={() => {
                setLeaderboardPage((value) => Math.max(0, value - 1));
                setIsLeaderboardLoading(true);
              }}
            />
          </>
        ) : !isLeaderboardLoading && !leaderboardError ? (
          <EmptyState
            description="XP contributions will populate this period's rankings."
            icon={<Trophy className="size-6" />}
            title="No rankings yet"
          />
        ) : null}
      </section>

      <section>
        <SectionTitle
          description="Achievements already unlocked through your CampusOne contributions."
          title="My earned badges"
        />
        {earnedBadges.length > 0 ? (
          <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {earnedBadges.map((item) => (
              <BadgeCard badge={item.badge} earned key={item.badge.id} />
            ))}
          </div>
        ) : !isProfileLoading && !profileError ? (
          <EmptyState
            className="mt-4"
            description="Keep contributing to unlock your first badge."
            icon={<Award className="size-6" />}
            title="No earned badges yet"
          />
        ) : null}
      </section>

      <section>
        <SectionTitle
          description={`${earnedBadges.length} of ${badges.length} active badges earned.`}
          title="Badge catalog"
        />
        {badges.length > 0 ? (
          <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {badges.map((badge) => (
              <BadgeCard
                badge={badge}
                earned={earnedCodes.has(badge.code)}
                key={badge.id}
              />
            ))}
          </div>
        ) : !isProfileLoading && !profileError ? (
          <EmptyState
            className="mt-4"
            description="Active badge definitions will appear here."
            icon={<Award className="size-6" />}
            title="No badges available"
          />
        ) : null}
      </section>

      <section className="grid gap-4">
        <SectionTitle
          description="A chronological record of XP earned through CampusOne contributions."
          title="My XP history"
        />
        {historyError ? <ErrorMessage message={historyError} /> : null}
        {isHistoryLoading ? (
          <div className="grid min-h-40 place-items-center rounded-2xl border border-slate-200 bg-white">
            <LoadingSpinner label="Loading XP history" />
          </div>
        ) : history && history.content.length > 0 ? (
          <>
            <XpHistoryList transactions={history.content} />
            <Pagination
              first={history.first}
              label={`${history.page + 1} / ${Math.max(1, history.totalPages)}`}
              last={history.last}
              onNext={() => {
                setHistoryPage((value) => value + 1);
                setIsHistoryLoading(true);
              }}
              onPrevious={() => {
                setHistoryPage((value) => Math.max(0, value - 1));
                setIsHistoryLoading(true);
              }}
            />
          </>
        ) : !isHistoryLoading && !historyError ? (
          <EmptyState
            description="Your first qualifying contribution will start this history."
            icon={<History className="size-6" />}
            title="No XP activity yet"
          />
        ) : null}
      </section>
    </div>
  );
}

function Pagination({
  first,
  label,
  last,
  onNext,
  onPrevious,
}: {
  first: boolean;
  label: string;
  last: boolean;
  onNext: () => void;
  onPrevious: () => void;
}) {
  return (
    <nav
      aria-label="Pagination"
      className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-3"
    >
      <Button disabled={first} onClick={onPrevious} variant="outline">
        <ArrowLeft className="size-4" />
        Previous
      </Button>
      <span className="text-sm font-semibold">{label}</span>
      <Button disabled={last} onClick={onNext} variant="outline">
        Next
        <ArrowRight className="size-4" />
      </Button>
    </nav>
  );
}
