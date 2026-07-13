import { ArrowLeft, CheckCircle2, RefreshCw, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  cancelLostFoundClaim,
  confirmLostFoundClaimantHandover,
  confirmLostFoundMatch,
  confirmLostFoundReporterHandover,
  listLostFoundClaims,
  listLostFoundMatches,
  rejectLostFoundMatch,
} from "@/api/lostFoundApi";
import {
  Badge,
  Button,
  Card,
  CardContent,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
} from "@/components/common";
import {
  formatLostFoundDateTime,
  lostFoundClaimStatusLabel,
} from "@/components/lost-found";
import { paths } from "@/routes/paths";
import type {
  LostFoundClaimPage,
  LostFoundMatchPage,
} from "@/types/lostFound";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function LostFoundClaimsPage() {
  const [claims, setClaims] = useState<LostFoundClaimPage | null>(null);
  const [matches, setMatches] = useState<LostFoundMatchPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actingId, setActingId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  useDocumentTitle("Lost & Found claims · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void Promise.all([
      listLostFoundClaims({ signal: controller.signal }),
      listLostFoundMatches({ signal: controller.signal }),
    ])
      .then(([claimResponse, matchResponse]) => {
        if (!active) return;
        setClaims(claimResponse);
        setMatches(matchResponse);
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "Lost & Found claims could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [refreshKey]);

  const updateClaim = async (
    claimId: string,
    action: "cancel" | "confirm-claimant" | "confirm-reporter",
  ) => {
    setActingId(`${action}-${claimId}`);
    setError(null);
    try {
      if (action === "cancel") await cancelLostFoundClaim(claimId);
      if (action === "confirm-claimant") {
        await confirmLostFoundClaimantHandover(claimId);
      }
      if (action === "confirm-reporter") {
        await confirmLostFoundReporterHandover(claimId);
      }
      setRefreshKey((current) => current + 1);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "This claim could not be updated.",
      );
    } finally {
      setActingId(null);
    }
  };

  const updateMatch = async (matchId: string, action: "confirm" | "reject") => {
    setActingId(`${action}-${matchId}`);
    setError(null);
    try {
      if (action === "confirm") await confirmLostFoundMatch(matchId);
      if (action === "reject") await rejectLostFoundMatch(matchId);
      setRefreshKey((current) => current + 1);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "This match could not be updated.",
      );
    } finally {
      setActingId(null);
    }
  };

  return (
    <div className="grid gap-6 pb-8">
      <Link
        className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
        to={paths.lostFound}
      >
        <ArrowLeft className="size-4" />
        Back to Lost & Found
      </Link>

      <PageHeader
        description="Track private claims and suggested matches for your Lost & Found items."
        eyebrow="Lost & Found"
        title="Claims and matches"
      />

      {error ? (
        <div className="grid gap-3">
          <ErrorMessage message={error} />
          <Button
            onClick={() => {
              setIsLoading(true);
              setRefreshKey((current) => current + 1);
            }}
          >
            <RefreshCw className="size-4" />
            Try again
          </Button>
        </div>
      ) : null}

      {isLoading ? (
        <div className="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading claims and matches" />
        </div>
      ) : null}

      {!isLoading && !error ? (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardContent className="grid gap-4 p-5 sm:p-6">
              <h2 className="text-xl font-bold text-slate-950">
                Claims
              </h2>
              {claims?.content.length ? (
                claims.content.map((claim) => (
                  <div
                    className="grid gap-2 rounded-xl border border-slate-200 p-4 hover:border-brand-200 hover:bg-brand-50/40"
                    key={claim.id}
                  >
                    <div className="flex flex-wrap gap-2">
                      <Badge>{lostFoundClaimStatusLabel(claim.status)}</Badge>
                      <span className="text-sm text-slate-500">
                        {formatLostFoundDateTime(claim.createdAt)}
                      </span>
                    </div>
                    <Link
                      className="font-semibold text-slate-900 hover:text-brand-700"
                      to={paths.lostFoundDetail(claim.itemId)}
                    >
                      {claim.itemTitle}
                    </Link>
                    <div className="flex flex-wrap gap-2">
                      {claim.status === "APPROVED" ? (
                        <>
                          {claim.claimantIsCurrentUser ? (
                            <Button
                              disabled={
                                claim.claimantHandoverConfirmedAt !== null
                              }
                              loading={
                                actingId === `confirm-claimant-${claim.id}`
                              }
                              onClick={() =>
                                void updateClaim(
                                  claim.id,
                                  "confirm-claimant",
                                )
                              }
                              size="sm"
                              variant="outline"
                            >
                              <CheckCircle2 className="size-4" />
                              {claim.claimantHandoverConfirmedAt
                                ? "Claimant confirmed"
                                : "Confirm handover"}
                            </Button>
                          ) : null}
                          {claim.reporterIsCurrentUser ? (
                            <Button
                              disabled={
                                claim.reporterHandoverConfirmedAt !== null
                              }
                              loading={
                                actingId === `confirm-reporter-${claim.id}`
                              }
                              onClick={() =>
                                void updateClaim(
                                  claim.id,
                                  "confirm-reporter",
                                )
                              }
                              size="sm"
                              variant="outline"
                            >
                              <CheckCircle2 className="size-4" />
                              {claim.reporterHandoverConfirmedAt
                                ? "Reporter confirmed"
                                : "Confirm handover"}
                            </Button>
                          ) : null}
                        </>
                      ) : null}
                      {claim.status === "PENDING" ||
                      claim.status === "APPROVED" ? (
                        <Button
                          disabled={actingId !== null}
                          onClick={() => void updateClaim(claim.id, "cancel")}
                          size="sm"
                          variant="ghost"
                        >
                          Cancel
                        </Button>
                      ) : null}
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-sm text-slate-500">
                  No claims yet.
                </p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardContent className="grid gap-4 p-5 sm:p-6">
              <h2 className="text-xl font-bold text-slate-950">
                Suggested matches
              </h2>
              {matches?.content.length ? (
                matches.content.map((match) => (
                  <div
                    className="grid gap-2 rounded-xl border border-slate-200 p-4"
                    key={match.id}
                  >
                    <Badge variant="brand">{match.score}% match</Badge>
                    <p className="font-semibold text-slate-900">
                      {match.lostItem.title} ↔ {match.foundItem.title}
                    </p>
                    <div className="flex flex-wrap gap-2">
                      <Link
                        className="text-sm font-semibold text-brand-700 hover:text-brand-800"
                        to={paths.lostFoundDetail(match.lostItem.id)}
                      >
                        Lost item
                      </Link>
                      <Link
                        className="text-sm font-semibold text-brand-700 hover:text-brand-800"
                        to={paths.lostFoundDetail(match.foundItem.id)}
                      >
                        Found item
                      </Link>
                    </div>
                    {match.status === "SUGGESTED" ? (
                      <div className="flex flex-wrap gap-2">
                        <Button
                          loading={actingId === `confirm-${match.id}`}
                          onClick={() => void updateMatch(match.id, "confirm")}
                          size="sm"
                        >
                          <CheckCircle2 className="size-4" />
                          Confirm
                        </Button>
                        <Button
                          disabled={actingId !== null}
                          onClick={() => void updateMatch(match.id, "reject")}
                          size="sm"
                          variant="danger"
                        >
                          <XCircle className="size-4" />
                          Reject
                        </Button>
                      </div>
                    ) : null}
                  </div>
                ))
              ) : (
                <p className="text-sm text-slate-500">
                  No suggested matches yet.
                </p>
              )}
            </CardContent>
          </Card>
        </div>
      ) : null}
    </div>
  );
}
