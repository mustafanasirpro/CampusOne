import { Check, RefreshCw, Sparkles, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  analyzeAuraResolutionCase,
  applyAuraResolutionCase,
  approveAuraResolutionCase,
  listAuraResolutionCases,
  rejectAuraResolutionCase,
} from "@/api/auraApi";
import { Badge, Button, Card, CardContent, CardHeader, CardTitle, EmptyState, ErrorMessage, LoadingSpinner } from "@/components/common";
import type { AuraResolutionCase } from "@/types/aura";

export function AuraResolutionPanel({ termId }: { termId: string }) {
  const [cases, setCases] = useState<AuraResolutionCase[]>([]);
  const [reason, setReason] = useState("Reviewed against the current published timetable.");
  const [pending, setPending] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (signal?: AbortSignal) => {
    setCases(await listAuraResolutionCases(termId, signal));
  }, [termId]);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listAuraResolutionCases(termId, controller.signal)
      .then((response) => {
        if (active) setCases(response);
      })
      .catch((requestError: unknown) => {
        if (active) setError(apiMessage(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [termId]);

  const act = async (key: string, action: () => Promise<unknown>) => {
    setPending(key);
    setError(null);
    try {
      await action();
      await load();
    } catch (requestError) {
      setError(apiMessage(requestError));
    } finally {
      setPending(null);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <CardTitle>Student clash resolutions</CardTitle>
          <Button onClick={() => void act("refresh", () => load())} variant="outline">
            <RefreshCw className="size-4" /> Refresh
          </Button>
        </div>
      </CardHeader>
      <CardContent className="grid gap-4">
        {error ? <ErrorMessage message={error} /> : null}
        {loading ? <LoadingSpinner label="Loading resolution cases" /> : cases.length ? (
          <>
            <label className="grid gap-1 text-sm font-medium text-slate-700">
              Review note
              <input
                className="rounded-xl border border-slate-300 px-3 py-2"
                maxLength={500}
                onChange={(event) => setReason(event.target.value)}
                value={reason}
              />
            </label>
            <div className="grid gap-3 lg:grid-cols-2">
              {cases.map((resolutionCase) => {
                const safe = resolutionCase.suggestions.find((suggestion) => suggestion.safe);
                return (
                  <article className="grid gap-3 rounded-2xl border border-slate-200 p-4" key={resolutionCase.id}>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="font-semibold text-slate-950">{resolutionCase.studentName}</p>
                      <Badge>{resolutionCase.status.replaceAll("_", " ")}</Badge>
                    </div>
                    <p className="text-sm text-slate-600">{resolutionCase.summary}</p>
                    {resolutionCase.suggestions.map((suggestion) => (
                      <div className={suggestion.safe ? "rounded-xl bg-emerald-50 p-3 text-sm text-emerald-900" : "rounded-xl bg-amber-50 p-3 text-sm text-amber-900"} key={suggestion.id}>
                        <p className="font-semibold">#{suggestion.rankOrder} {suggestion.suggestionType.replaceAll("_", " ")}</p>
                        <p>
                          {suggestion.targetOfferingCode
                            ? `${suggestion.targetOfferingCode} · ${suggestion.targetSectionName ?? "Alternative section"}`
                            : `${suggestion.targetGroupType ?? "Teaching"} group ${suggestion.targetGroupCode ?? "alternative"}`}
                        </p>
                        <p className="mt-1">{suggestion.explanation}</p>
                      </div>
                    ))}
                    <div className="flex flex-wrap gap-2">
                      {resolutionCase.status === "OPEN" ? (
                        <Button loading={pending === `analyze-${resolutionCase.id}`} onClick={() => void act(`analyze-${resolutionCase.id}`, () => analyzeAuraResolutionCase(resolutionCase.id))}>
                          <Sparkles className="size-4" /> Analyze
                        </Button>
                      ) : null}
                      {resolutionCase.status === "SUGGESTED" && safe ? (
                        <Button disabled={!reason.trim()} loading={pending === `approve-${resolutionCase.id}`} onClick={() => void act(`approve-${resolutionCase.id}`, () => approveAuraResolutionCase(resolutionCase.id, safe.id, reason.trim(), resolutionCase.version))}>
                          <Check className="size-4" /> Approve safest
                        </Button>
                      ) : null}
                      {resolutionCase.status === "APPROVED" && safe ? (
                        <Button disabled={!reason.trim()} loading={pending === `apply-${resolutionCase.id}`} onClick={() => void act(`apply-${resolutionCase.id}`, () => applyAuraResolutionCase(resolutionCase.id, safe.id, reason.trim(), resolutionCase.version))}>
                          Apply transfer
                        </Button>
                      ) : null}
                      {["OPEN", "SUGGESTED", "APPROVED"].includes(resolutionCase.status) ? (
                        <Button disabled={!reason.trim()} loading={pending === `reject-${resolutionCase.id}`} onClick={() => void act(`reject-${resolutionCase.id}`, () => rejectAuraResolutionCase(resolutionCase.id, reason.trim(), resolutionCase.version))} variant="outline">
                          <X className="size-4" /> Reject
                        </Button>
                      ) : null}
                    </div>
                  </article>
                );
              })}
            </div>
          </>
        ) : (
          <EmptyState description="Student requests and personal timetable clashes will appear here." icon={<Check className="size-6" />} title="No resolution cases" />
        )}
      </CardContent>
    </Card>
  );
}

function apiMessage(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : "Resolution cases could not be loaded. Please try again.";
}
