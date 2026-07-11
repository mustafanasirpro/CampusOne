import {
  ArrowLeft,
  ArrowRight,
  Eye,
  FileStack,
  Trash2,
} from "lucide-react";
import { useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  deleteGeneratedItem,
  getGeneratedItem,
  listGeneratedItems,
} from "@/api/aiApi";
import {
  Badge,
  Button,
  Card,
  CardContent,
  Dropdown,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  useToast,
} from "@/components/common";
import { GeneratedContentRenderer } from "@/components/ai/GeneratedContentRenderer";
import {
  aiItemTypeLabel,
  formatAiDate,
} from "@/components/ai/aiFormatting";
import type {
  AiGeneratedItem,
  AiGeneratedItemPage,
  AiGeneratedItemType,
} from "@/types/ai";

const pageSize = 12;

export function GeneratedItemsPanel() {
  const { showToast } = useToast();
  const [itemType, setItemType] = useState<AiGeneratedItemType | "">("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<AiGeneratedItemPage | null>(null);
  const [selected, setSelected] = useState<AiGeneratedItem | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isOpening, setIsOpening] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listGeneratedItems({
      itemType: itemType || undefined,
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
            : "Generated items could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [itemType, page, refreshKey]);

  const openItem = async (itemId: string) => {
    setIsOpening(true);
    setError(null);
    try {
      setSelected(await getGeneratedItem(itemId));
    } catch (requestError) {
      setError(
        requestError instanceof ApiError && requestError.status === 404
          ? "This generated item was not found."
          : requestError instanceof ApiError
            ? requestError.message
            : "The generated item could not be opened.",
      );
    } finally {
      setIsOpening(false);
    }
  };

  const remove = async (item: AiGeneratedItem) => {
    if (!window.confirm(`Delete "${item.title}"?`)) return;
    setError(null);
    try {
      await deleteGeneratedItem(item.id);
      if (selected?.id === item.id) setSelected(null);
      if (result?.content.length === 1 && page > 0) {
        setPage((value) => Math.max(0, value - 1));
      }
      setIsLoading(true);
      setRefreshKey((value) => value + 1);
      showToast({
        title: "Generated item deleted",
        message: item.title,
        variant: "success",
      });
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "The generated item could not be deleted.",
      );
    }
  };

  return (
    <div className="grid gap-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-950">Generated items</h2>
          <p className="mt-1 text-sm text-slate-500">
            Reopen your saved summaries, flashcards, quizzes, and study plans.
          </p>
        </div>
        <Dropdown
          className="min-w-48"
          label="Item type"
          onChange={(event) => {
            setItemType(event.target.value as AiGeneratedItemType | "");
            setPage(0);
            setIsLoading(true);
            setSelected(null);
          }}
          options={[
            { label: "All generated items", value: "" },
            { label: "Summaries", value: "SUMMARY" },
            { label: "Flashcards", value: "FLASHCARDS" },
            { label: "Quizzes", value: "QUIZ" },
            { label: "Study plans", value: "STUDY_PLAN" },
          ]}
          value={itemType}
        />
      </div>
      {error ? <ErrorMessage message={error} /> : null}
      {isLoading ? (
        <div className="grid min-h-52 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading generated items" />
        </div>
      ) : result?.content.length === 0 ? (
        <EmptyState
          description="Use one of the AI generators to create your first saved study item."
          icon={<FileStack className="size-6" />}
          title="No generated items"
        />
      ) : result ? (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {result.content.map((item) => (
              <Card key={item.id}>
                <CardContent className="flex h-full flex-col p-5">
                  <Badge variant="brand">
                    {aiItemTypeLabel(item.itemType)}
                  </Badge>
                  <h3 className="mt-4 line-clamp-2 font-semibold text-slate-950">
                    {item.title}
                  </h3>
                  <p className="mt-2 text-xs text-slate-400">
                    {formatAiDate(item.createdAt)}
                  </p>
                  <div className="mt-auto flex gap-2 pt-5">
                    <Button
                      loading={isOpening}
                      onClick={() => void openItem(item.id)}
                      size="sm"
                    >
                      <Eye className="size-3.5" />
                      Open
                    </Button>
                    <Button
                      onClick={() => void remove(item)}
                      size="sm"
                      variant="ghost"
                    >
                      <Trash2 className="size-3.5 text-red-600" />
                      Delete
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
          <nav
            aria-label="Generated items pagination"
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

      {selected ? (
        <Card className="border-brand-200">
          <CardContent className="grid gap-5 p-6">
            <div className="flex flex-wrap items-center gap-3">
              <div>
                <Badge variant="brand">
                  {aiItemTypeLabel(selected.itemType)}
                </Badge>
                <h2 className="mt-3 text-xl font-bold text-slate-950">
                  {selected.title}
                </h2>
              </div>
              <Button
                className="ml-auto"
                onClick={() => setSelected(null)}
                variant="ghost"
              >
                Close
              </Button>
            </div>
            <GeneratedContentRenderer
              content={selected.generatedContent}
              itemType={selected.itemType}
            />
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
