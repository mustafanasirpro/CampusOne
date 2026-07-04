import { WandSparkles } from "lucide-react";
import { useState, type FormEvent } from "react";

import { ApiError } from "@/api/apiClient";
import {
  generateFlashcards,
  generateQuiz,
  generateSummary,
} from "@/api/aiApi";
import {
  Badge,
  Button,
  Card,
  CardContent,
  ErrorMessage,
} from "@/components/common";
import { FormField } from "@/components/forms";
import { GeneratedContentRenderer } from "@/components/ai/GeneratedContentRenderer";
import type { AiGeneratedItem } from "@/types/ai";

type GeneratorKind = "flashcards" | "quiz" | "summary";

const configuration = {
  flashcards: {
    button: "Generate flashcards",
    itemType: "FLASHCARDS",
    title: "Flashcard generator",
  },
  quiz: {
    button: "Generate quiz",
    itemType: "QUIZ",
    title: "Practice quiz generator",
  },
  summary: {
    button: "Generate summary",
    itemType: "SUMMARY",
    title: "Study summary generator",
  },
} as const;

export function TextGeneratorPanel({ kind }: { kind: GeneratorKind }) {
  const config = configuration[kind];
  const [title, setTitle] = useState("");
  const [text, setText] = useState("");
  const [count, setCount] = useState("5");
  const [result, setResult] = useState<AiGeneratedItem | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalizedText = text.trim();
    const normalizedTitle = title.trim();
    if (normalizedTitle && normalizedTitle.length < 3) {
      setError("Optional titles must contain at least 3 characters.");
      return;
    }
    if (normalizedText.length < 20) {
      setError("Please enter enough text for the assistant to work with.");
      return;
    }
    const requestedCount = Number(count);
    if (
      kind !== "summary" &&
      (!Number.isInteger(requestedCount) ||
        requestedCount < 1 ||
        requestedCount > 20)
    ) {
      setError("Choose between 1 and 20 items.");
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const request = {
        text: normalizedText,
        title: normalizedTitle || null,
      };
      setResult(
        kind === "summary"
          ? await generateSummary(request)
          : kind === "flashcards"
            ? await generateFlashcards({ ...request, count: requestedCount })
            : await generateQuiz({ ...request, count: requestedCount }),
      );
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "Could not generate response. Please try again.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="grid gap-6">
      <Card>
        <CardContent>
          <form className="grid gap-5" onSubmit={submit}>
            <div>
              <h2 className="text-lg font-semibold text-slate-950">
                {config.title}
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                Generated results are saved automatically to your CampusOne history.
              </p>
            </div>
            <FormField
              label="Optional title"
              maxLength={160}
              onChange={(event) => setTitle(event.target.value)}
              value={title}
            />
            <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
              Source text
              <textarea
                className="min-h-56 rounded-xl border border-slate-200 px-3.5 py-3 text-sm font-normal leading-6 outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
                maxLength={10000}
                onChange={(event) => setText(event.target.value)}
                required
                value={text}
              />
              <span className="text-xs font-normal text-slate-400">
                {text.length}/10000
              </span>
            </label>
            {kind !== "summary" ? (
              <FormField
                label={kind === "quiz" ? "Question count" : "Flashcard count"}
                max={20}
                min={1}
                onChange={(event) => setCount(event.target.value)}
                type="number"
                value={count}
              />
            ) : null}
            {error ? <ErrorMessage message={error} /> : null}
            <Button className="w-fit" loading={isLoading} type="submit">
              <WandSparkles className="size-4" />
              {config.button}
            </Button>
          </form>
        </CardContent>
      </Card>
      {result ? (
        <Card className="border-brand-200">
          <CardContent className="grid gap-5 p-6">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-lg font-semibold text-slate-950">
                {result.title}
              </h2>
              <Badge variant="brand">{result.itemType}</Badge>
            </div>
            <GeneratedContentRenderer
              content={result.generatedContent}
              itemType={result.itemType}
            />
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
