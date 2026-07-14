import {
  ArrowLeft,
  CheckCircle2,
  Copy,
  Edit,
  Handshake,
  RefreshCw,
  ShieldCheck,
  Trash2,
  XCircle,
} from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  approveLostFoundClaim,
  archiveLostFoundItem,
  closeLostFoundItem,
  confirmLostFoundReporterHandover,
  createLostFoundClaim,
  deleteLostFoundItem,
  getLostFoundItem,
  listLostFoundItemClaims,
  renewLostFoundItem,
  reopenLostFoundItem,
  rejectLostFoundClaim,
  resolveLostFoundItem,
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
  formatLostFoundDate,
  formatLostFoundDateTime,
  lostFoundCategoryLabel,
  lostFoundClaimStatusLabel,
  lostFoundStatusLabel,
  lostFoundTypeLabel,
} from "@/components/lost-found";
import { paths } from "@/routes/paths";
import type {
  LostFoundClaim,
  LostFoundClaimPage,
  LostFoundItemDetail,
} from "@/types/lostFound";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function LostFoundItemDetailPage() {
  const { itemId } = useParams<{ itemId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [item, setItem] = useState<LostFoundItemDetail | null>(null);
  const [claims, setClaims] = useState<LostFoundClaimPage | null>(null);
  const [proofText, setProofText] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [contactSharingConsent, setContactSharingConsent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmittingClaim, setIsSubmittingClaim] = useState(false);
  const [actingClaimId, setActingClaimId] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useDocumentTitle(item ? `${item.title} · Lost & Found` : "Lost & Found item");

  useEffect(() => {
    if (!itemId) return;
    const controller = new AbortController();
    let active = true;
    void getLostFoundItem(itemId, controller.signal)
      .then((response) => {
        if (!active) return;
        setItem(response);
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setItem(null);
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "This Lost & Found item could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [itemId, refreshKey]);

  useEffect(() => {
    if (!itemId || !item?.ownedByCurrentUser) {
      return;
    }
    const controller = new AbortController();
    let active = true;
    void listLostFoundItemClaims(itemId, { signal: controller.signal })
      .then((response) => {
        if (active) setClaims(response);
      })
      .catch(() => {
        if (active) setClaims(null);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [item?.ownedByCurrentUser, itemId, refreshKey]);

  const submitClaim = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!itemId || isSubmittingClaim) return;
    const proof = proofText.trim();
    if (proof.length < 10) {
      setError("Add a few details that help the reporter verify your claim.");
      return;
    }
    if (!contactPhone.trim()) {
      setError("Add a handover contact number.");
      return;
    }
    if (!contactSharingConsent) {
      setError("Agree to share your number with the finder after approval.");
      return;
    }
    setIsSubmittingClaim(true);
    setError(null);
    try {
      await createLostFoundClaim(itemId, {
        contactPhone,
        contactSharingConsent,
        proofText: proof,
      });
      setProofText("");
      setContactPhone("");
      setContactSharingConsent(false);
      showToast({
        title: "Claim submitted",
        message: "The reporter can now review your claim privately.",
        variant: "success",
      });
      setRefreshKey((current) => current + 1);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "Your claim could not be submitted.",
      );
    } finally {
      setIsSubmittingClaim(false);
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

  const removeItem = async () => {
    if (!item || !window.confirm("Delete this Lost & Found item?")) return;
    try {
      await deleteLostFoundItem(item.id);
      showToast({
        title: "Item deleted",
        message: item.title,
        variant: "success",
      });
      navigate(paths.lostFound, { replace: true });
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "This item could not be deleted.",
      );
    }
  };

  const updateItemState = async (
    action:
      | "archive"
      | "close"
      | "renew"
      | "reopen"
      | "resolve",
  ) => {
    if (!item || actingClaimId) return;
    setActingClaimId(`item-${action}`);
    setError(null);
    try {
      const updated =
        action === "archive"
          ? await archiveLostFoundItem(item.id)
          : action === "close"
            ? await closeLostFoundItem(item.id)
            : action === "renew"
              ? await renewLostFoundItem(item.id)
              : action === "reopen"
                ? await reopenLostFoundItem(item.id)
                : await resolveLostFoundItem(item.id);
      setItem(updated);
      showToast({
        title:
          action === "resolve"
            ? "Item marked recovered"
            : action === "renew"
              ? "Submitted for review"
              : "Item updated",
        message: updated.title,
        variant: "success",
      });
      setRefreshKey((current) => current + 1);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "This item could not be updated.",
      );
    } finally {
      setActingClaimId(null);
    }
  };

  const reviewClaim = async (
    claim: LostFoundClaim,
    action: "approve" | "reject" | "complete",
  ) => {
    setActingClaimId(claim.id);
    setError(null);
    try {
      if (action === "approve") await approveLostFoundClaim(claim.id);
      if (action === "reject") await rejectLostFoundClaim(claim.id);
      if (action === "complete") {
        await confirmLostFoundReporterHandover(claim.id);
      }
      showToast({
        title:
          action === "approve"
            ? "Claim approved"
            : action === "reject"
              ? "Claim rejected"
              : "Handover confirmed",
        message: claim.itemTitle,
        variant: "success",
      });
      setRefreshKey((current) => current + 1);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "The claim could not be updated.",
      );
    } finally {
      setActingClaimId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-96 place-items-center">
        <LoadingSpinner label="Loading Lost & Found item" />
      </div>
    );
  }

  if (error && !item) {
    return (
      <div className="grid gap-4">
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
    );
  }

  if (!item) return null;

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
        actions={
          item.ownedByCurrentUser ? (
            <div className="flex flex-wrap gap-2">
              <Link
                className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
                to={paths.lostFoundEdit(item.id)}
              >
                <Edit className="size-4" />
                Edit
              </Link>
              {item.status === "PUBLISHED" && item.type === "LOST" ? (
                <Button
                  loading={actingClaimId === "item-resolve"}
                  onClick={() => void updateItemState("resolve")}
                  variant="outline"
                >
                  <CheckCircle2 className="size-4" />
                  Mark recovered
                </Button>
              ) : null}
              {item.status === "PUBLISHED" ? (
                <>
                  <Button
                    loading={actingClaimId === "item-close"}
                    onClick={() => void updateItemState("close")}
                    variant="outline"
                  >
                    Close
                  </Button>
                  <Button
                    loading={actingClaimId === "item-archive"}
                    onClick={() => void updateItemState("archive")}
                    variant="outline"
                  >
                    Archive
                  </Button>
                </>
              ) : null}
              {item.status === "CLOSED" ? (
                <Button
                  loading={actingClaimId === "item-reopen"}
                  onClick={() => void updateItemState("reopen")}
                  variant="outline"
                >
                  Reopen
                </Button>
              ) : null}
              {item.status === "ARCHIVED" || item.status === "REJECTED" ? (
                <Button
                  loading={actingClaimId === "item-renew"}
                  onClick={() => void updateItemState("renew")}
                  variant="outline"
                >
                  <RefreshCw className="size-4" />
                  Renew
                </Button>
              ) : null}
              <Button onClick={() => void removeItem()} variant="danger">
                <Trash2 className="size-4" />
                Delete
              </Button>
            </div>
          ) : null
        }
        description={`${lostFoundTypeLabel(item.type)} · ${lostFoundCategoryLabel(
          item.category,
        )}`}
        eyebrow="Lost & Found"
        title={item.title}
      />

      {error ? <ErrorMessage message={error} /> : null}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.4fr)_minmax(320px,0.8fr)]">
        <div className="grid gap-4">
          <div className="grid gap-3 sm:grid-cols-2">
            {item.images.length > 0 ? (
              item.images.map((image) => (
                <img
                  alt=""
                  className="aspect-video w-full rounded-2xl border border-slate-200 object-cover"
                  key={image.id}
                  src={image.imageUrl}
                />
              ))
            ) : (
              <div className="grid aspect-video place-items-center rounded-2xl border border-dashed border-slate-300 bg-slate-50 text-sm font-semibold text-slate-400 sm:col-span-2">
                No photo provided
              </div>
            )}
          </div>

          <Card>
            <CardContent className="grid gap-4 p-5 sm:p-6">
              <div className="flex flex-wrap gap-2">
                <Badge variant={item.type === "LOST" ? "danger" : "success"}>
                  {lostFoundTypeLabel(item.type)}
                </Badge>
                <Badge>{lostFoundStatusLabel(item.status)}</Badge>
              </div>
              <p className="leading-7 text-slate-600">{item.description}</p>
              <dl className="grid gap-3 text-sm sm:grid-cols-2">
                <Info label="Location" value={item.locationText} />
                <Info label="Date" value={formatLostFoundDate(item.itemDate)} />
                <Info label="Brand" value={item.brand ?? "Not specified"} />
                <Info label="Color" value={item.color ?? "Not specified"} />
                <Info
                  label="Submitted"
                  value={formatLostFoundDateTime(item.createdAt)}
                />
                <Info
                  label="Expires"
                  value={formatLostFoundDateTime(item.expiresAt)}
                />
              </dl>
            </CardContent>
          </Card>
        </div>

        <aside className="grid content-start gap-4">
          {item.claimable ? (
            <Card>
              <CardContent className="grid gap-4 p-5">
                <div>
                  <h2 className="text-lg font-semibold text-slate-950">
                    Think this is yours?
                  </h2>
                  <p className="mt-1 text-sm text-slate-500">
                    Share private proof and a handover number. Your number is shared with the finder only after approval.
                  </p>
                </div>
                <form className="grid gap-3" onSubmit={submitClaim}>
                  <textarea
                    className="min-h-32 rounded-xl border border-slate-200 px-3.5 py-3 text-sm outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
                    maxLength={2000}
                    onChange={(event) => setProofText(event.target.value)}
                    placeholder="Describe identifying details only the owner would know."
                    value={proofText}
                  />
                  <div className="grid gap-1.5">
                    <label
                      className="text-sm font-semibold text-slate-700"
                      htmlFor="claim-contact-phone"
                    >
                      Handover contact number
                    </label>
                    <input
                      autoComplete="tel"
                      className="rounded-xl border border-slate-200 px-3.5 py-3 text-sm outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
                      id="claim-contact-phone"
                      onChange={(event) => setContactPhone(event.target.value)}
                      placeholder="+92 300 1234567"
                      required
                      type="tel"
                      value={contactPhone}
                    />
                    <p className="text-xs leading-5 text-slate-500">
                      Your number stays private and is shared with the finder only after your claim is approved.
                    </p>
                  </div>
                  <label className="flex gap-3 rounded-xl bg-slate-50 p-3 text-sm leading-6 text-slate-600">
                    <input
                      checked={contactSharingConsent}
                      className="mt-1 size-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                      onChange={(event) =>
                        setContactSharingConsent(event.target.checked)
                      }
                      required
                      type="checkbox"
                    />
                    <span>
                      I agree that this number may be shared with the finder after my claim is approved so we can arrange the handover.
                    </span>
                  </label>
                  <Button loading={isSubmittingClaim} type="submit">
                    <ShieldCheck className="size-4" />
                    Submit claim
                  </Button>
                </form>
              </CardContent>
            </Card>
          ) : null}

          {item.ownedByCurrentUser ? (
            <Card>
              <CardContent className="grid gap-4 p-5">
                <h2 className="text-lg font-semibold text-slate-950">
                  Claims
                </h2>
                {claims?.content.length ? (
                  <div className="grid gap-3">
                    {claims.content.map((claim) => (
                      <div
                        className="grid gap-3 rounded-xl border border-slate-200 p-3"
                        key={claim.id}
                      >
                        <div className="flex flex-wrap gap-2">
                          <Badge>{lostFoundClaimStatusLabel(claim.status)}</Badge>
                          <span className="text-sm font-semibold text-slate-700">
                            {claim.claimant.fullName ?? "Student"}
                          </span>
                        </div>
                        {claim.proofText ? (
                          <p className="text-sm leading-6 text-slate-600">
                            {claim.proofText}
                          </p>
                        ) : null}
                        <ClaimContactCard
                          claim={claim}
                          onCopy={copyContactPhone}
                        />
                        <div className="flex flex-wrap gap-2">
                          {claim.status === "PENDING" ? (
                            <>
                              <Button
                                loading={actingClaimId === claim.id}
                                onClick={() => void reviewClaim(claim, "approve")}
                                size="sm"
                              >
                                <CheckCircle2 className="size-4" />
                                Approve
                              </Button>
                              <Button
                                disabled={actingClaimId === claim.id}
                                onClick={() => void reviewClaim(claim, "reject")}
                                size="sm"
                                variant="danger"
                              >
                                <XCircle className="size-4" />
                                Reject
                              </Button>
                            </>
                          ) : null}
                          {claim.status === "APPROVED" ? (
                            <Button
                              loading={actingClaimId === claim.id}
                              onClick={() => void reviewClaim(claim, "complete")}
                              size="sm"
                              variant="outline"
                            >
                              <Handshake className="size-4" />
                              Complete handover
                            </Button>
                          ) : null}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-slate-500">
                    No claims have been submitted yet.
                  </p>
                )}
              </CardContent>
            </Card>
          ) : null}
        </aside>
      </div>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className={cn("rounded-xl bg-slate-50 p-3")}>
      <dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">
        {label}
      </dt>
      <dd className="mt-1 font-semibold text-slate-800">{value}</dd>
    </div>
  );
}

function ClaimContactCard({
  claim,
  onCopy,
}: {
  claim: LostFoundClaim;
  onCopy: (phone: string) => Promise<void>;
}) {
  const visiblePhone = claim.contactPhoneVisible ? claim.contactPhone : null;
  const displayPhone = visiblePhone ?? claim.maskedContactPhone;
  if (!displayPhone) return null;

  return (
    <div className="grid gap-2 rounded-xl bg-slate-50 p-3 text-sm">
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
      <p className="text-xs leading-5 text-slate-500">
        Meet in a safe university location and confirm the handover in CampusOne only after the item has physically been returned.
      </p>
    </div>
  );
}
