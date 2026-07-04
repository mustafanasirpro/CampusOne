import { ArrowLeft, ArrowRight, Gauge } from "lucide-react";
import { useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import { listAiUsage } from "@/api/aiApi";
import {
  Badge,
  Button,
  Card,
  CardContent,
  Dropdown,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
} from "@/components/common";
import {
  aiFeatureLabel,
  formatAiDate,
} from "@/components/ai/aiFormatting";
import type {
  AiUsageFeature,
  AiUsagePage,
} from "@/types/ai";

const pageSize = 12;

export function AiUsagePanel() {
  const [feature, setFeature] = useState<AiUsageFeature | "">("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<AiUsagePage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listAiUsage({
      feature: feature || undefined,
      page,
      signal: controller.signal,
      size: pageSize,
    })
      .then((response) => {
        if (!active) return;
        setResult(response);
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "AI usage history could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [feature, page]);

  return (
    <div className="grid gap-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-950">AI usage history</h2>
          <p className="mt-1 text-sm text-slate-500">
            Token estimates recorded by CampusOne for each assistant feature.
          </p>
        </div>
        <Dropdown
          className="min-w-48"
          label="Feature"
          onChange={(event) => {
            setFeature(event.target.value as AiUsageFeature | "");
            setPage(0);
            setIsLoading(true);
          }}
          options={[
            { label: "All AI features", value: "" },
            { label: "Chat", value: "CHAT" },
            { label: "Explain concepts", value: "EXPLAIN_CONCEPT" },
            { label: "Summaries", value: "SUMMARIZE" },
            { label: "Flashcards", value: "FLASHCARDS" },
            { label: "Quizzes", value: "QUIZ" },
            { label: "Study plans", value: "STUDY_PLAN" },
          ]}
          value={feature}
        />
      </div>
      {error ? <ErrorMessage message={error} /> : null}
      {isLoading ? (
        <div className="grid min-h-52 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading AI usage" />
        </div>
      ) : result?.content.length === 0 ? (
        <EmptyState
          description="Usage records appear after you use a CampusOne AI feature."
          icon={<Gauge className="size-6" />}
          title="No AI usage yet"
        />
      ) : result ? (
        <>
          <div className="grid gap-3">
            {result.content.map((record) => (
              <Card key={record.id}>
                <CardContent className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
                  <Badge variant="brand">{aiFeatureLabel(record.feature)}</Badge>
                  <div className="grid flex-1 grid-cols-2 gap-3 text-sm sm:grid-cols-3">
                    <Metric label="Input tokens" value={record.inputTokenEstimate} />
                    <Metric label="Output tokens" value={record.outputTokenEstimate} />
                    <Metric
                      label="Total estimate"
                      value={
                        record.inputTokenEstimate +
                        record.outputTokenEstimate
                      }
                    />
                  </div>
                  <div className="text-xs text-slate-400 sm:text-right">
                    <p>{record.provider}</p>
                    <p className="mt-1">{formatAiDate(record.createdAt)}</p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
          <nav
            aria-label="AI usage pagination"
            className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-3"
          >
            <Button
              disabled={result.first}
              onClick={() => {
                setPage((value) => Math.max(0, value - 1));
                setIsLoading(true);
              }}
              variant="outline"
            >
              <ArrowLeft className="size-4" />
              Previous
            </Button>
            <span className="text-sm font-semibold">
              {result.page + 1} / {Math.max(1, result.totalPages)}
            </span>
            <Button
              disabled={result.last}
              onClick={() => {
                setPage((value) => value + 1);
                setIsLoading(true);
              }}
              variant="outline"
            >
              Next
              <ArrowRight className="size-4" />
            </Button>
          </nav>
        </>
      ) : null}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <p className="text-xs text-slate-400">{label}</p>
      <p className="mt-1 font-semibold text-slate-800">
        {value.toLocaleString()}
      </p>
    </div>
  );
}

