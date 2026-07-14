import { ArrowLeft, CheckCircle2, Copy, RefreshCw, XCircle } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
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
  updateLostFoundClaimContactPhone,
} from "@/api/lostFoundApi";
import {
  Badge,
  Button,
  Card,
  CardContent,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  useToast,
} from "@/components/common";
import {
  formatLostFoundDateTime,
  lostFoundClaimStatusLabel,
} from "@/components/lost-found";
import { paths } from "@/routes/paths";
import type {
  LostFoundClaim,
  LostFoundClaimPage,
  LostFoundMatchPage,
} from "@/types/lostFound";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function LostFoundClaimsPage() {
  const [claims, setClaims] = useState<LostFoundClaimPage | null>(null);
  const [matches, setMatches] = useState<LostFoundMatchPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actingId, setActingId] = useState<string | null>(null);
  const [editingContactClaimId, setEditingContactClaimId] =
    useState<string | null>(null);
  const [contactPhoneDraft, setContactPhoneDraft] = useState("");
  const [contactConsentDraft, setContactConsentDraft] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);
  const { showToast } = useToast();

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

  const copyContactPhone = async (phone: string) => {
    try {
      await navigator.clipboard.writeText(phone);
      showToast({
        title: "Contact number copied",
        message: "Use it only to arrange a safe handover.",
        variant: "success",
      });
    } catch {
      setError("The contact number could not be copied.");
    }
  };

  const beginContactEdit = (claim: LostFoundClaim) => {
    setEditingContactClaimId(claim.id);
    setContactPhoneDraft(claim.contactPhone ?? "");
    setContactConsentDraft(false);
  };

  const submitContactUpdate = async (
    event: FormEvent<HTMLFormElement>,
    claimId: string,
  ) => {
    event.preventDefault();
    if (actingId) return;
    if (!contactPhoneDraft.trim()) {
      setError("Add a handover contact number.");
      return;
    }
    if (!contactConsentDraft) {
      setError("Agree to share your number with the finder after approval.");
      return;
    }
    setActingId(`contact-${claimId}`);
    setError(null);
    try {
      await updateLostFoundClaimContactPhone(claimId, {
        contactPhone: contactPhoneDraft,
        contactSharingConsent: contactConsentDraft,
      });
      setEditingContactClaimId(null);
      setContactPhoneDraft("");
      setContactConsentDraft(false);
      setRefreshKey((current) => current + 1);
      showToast({
        title: "Contact number updated",
        message: "Your handover contact is saved privately.",
        variant: "success",
      });
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "The contact number could not be updated.",
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
                    <ClaimContactPanel
                      claim={claim}
                      contactConsentDraft={contactConsentDraft}
                      contactPhoneDraft={contactPhoneDraft}
                      editing={editingContactClaimId === claim.id}
                      isUpdating={actingId === `contact-${claim.id}`}
                      onCancelEdit={() => {
                        setEditingContactClaimId(null);
                        setContactPhoneDraft("");
                        setContactConsentDraft(false);
                      }}
                      onConsentChange={setContactConsentDraft}
                      onCopy={copyContactPhone}
                      onEdit={() => beginContactEdit(claim)}
                      onPhoneChange={setContactPhoneDraft}
                      onSubmit={(event) =>
                        void submitContactUpdate(event, claim.id)
                      }
                    />
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

function ClaimContactPanel({
  claim,
  contactConsentDraft,
  contactPhoneDraft,
  editing,
  isUpdating,
  onCancelEdit,
  onConsentChange,
  onCopy,
  onEdit,
  onPhoneChange,
  onSubmit,
}: {
  claim: LostFoundClaim;
  contactConsentDraft: boolean;
  contactPhoneDraft: string;
  editing: boolean;
  isUpdating: boolean;
  onCancelEdit: () => void;
  onConsentChange: (value: boolean) => void;
  onCopy: (phone: string) => Promise<void>;
  onEdit: () => void;
  onPhoneChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const visiblePhone = claim.contactPhoneVisible ? claim.contactPhone : null;
  const displayPhone = visiblePhone ?? claim.maskedContactPhone;

  if (editing) {
    return (
      <form
        className="grid gap-3 rounded-xl bg-slate-50 p-3"
        onSubmit={onSubmit}
      >
        <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
          Handover contact number
          <input
            autoComplete="tel"
            className="rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm font-normal outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
            onChange={(event) => onPhoneChange(event.target.value)}
            placeholder="+92 300 1234567"
            required
            type="tel"
            value={contactPhoneDraft}
          />
        </label>
        <label className="flex gap-3 text-sm leading-6 text-slate-600">
          <input
            checked={contactConsentDraft}
            className="mt-1 size-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
            onChange={(event) => onConsentChange(event.target.checked)}
            required
            type="checkbox"
          />
          <span>
            I agree that this number may be shared with the finder after my claim is approved so we can arrange the handover.
          </span>
        </label>
        <div className="flex flex-wrap gap-2">
          <Button loading={isUpdating} size="sm" type="submit">
            Save number
          </Button>
          <Button
            disabled={isUpdating}
            onClick={onCancelEdit}
            size="sm"
            type="button"
            variant="ghost"
          >
            Cancel
          </Button>
        </div>
      </form>
    );
  }

  if (!displayPhone && !claim.claimantIsCurrentUser) return null;

  return (
    <div className="grid gap-2 rounded-xl bg-slate-50 p-3 text-sm">
      {displayPhone ? (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              Handover contact
            </p>
            <p className="font-semibold text-slate-800">{displayPhone}</p>
          </div>
          {visiblePhone ? (
            <Button
              onClick={() => void onCopy(visiblePhone)}
              size="sm"
              type="button"
              variant="outline"
            >
              <Copy className="size-4" />
              Copy
            </Button>
          ) : null}
        </div>
      ) : null}
      <p className="text-xs leading-5 text-slate-500">
        Meet in a safe university location and confirm the handover in CampusOne only after the item has physically been returned.
      </p>
      {claim.claimantIsCurrentUser && claim.status === "PENDING" ? (
        <Button onClick={onEdit} size="sm" type="button" variant="outline">
          Update contact number
        </Button>
      ) : null}
    </div>
  );
}
