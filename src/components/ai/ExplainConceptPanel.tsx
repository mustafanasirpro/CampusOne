import { BrainCircuit, Eraser, WandSparkles } from "lucide-react";
import { useState, type FormEvent } from "react";

import { ApiError } from "@/api/apiClient";
import { explainConcept } from "@/api/aiApi";
import {
  Badge,
  Button,
  Card,
  CardContent,
  ErrorMessage,
} from "@/components/common";
import { FormField } from "@/components/forms";
import type { AiExplanationResponse } from "@/types/ai";

export function ExplainConceptPanel() {
  const [concept, setConcept] = useState("");
  const [context, setContext] = useState("");
  const [result, setResult] = useState<AiExplanationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalized = concept.trim();
    if (normalized.length < 2) {
      setError("Enter a concept with at least 2 characters.");
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      setResult(
        await explainConcept({
          concept: normalized,
          context: context.trim() || null,
        }),
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
    <div className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
      <Card>
        <CardContent>
          <form className="grid gap-5" onSubmit={submit}>
            <FormField
              label="Concept"
              maxLength={200}
              onChange={(event) => setConcept(event.target.value)}
              placeholder="Dependency injection"
              required
              value={concept}
            />
            <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
              Optional course context
              <textarea
                className="min-h-40 rounded-xl border border-slate-200 px-3.5 py-3 text-sm font-normal leading-6 outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
                maxLength={5000}
                onChange={(event) => setContext(event.target.value)}
                value={context}
              />
            </label>
            {error ? <ErrorMessage message={error} /> : null}
            <div className="flex gap-2">
              <Button loading={isLoading} type="submit">
                <WandSparkles className="size-4" />
                Explain concept
              </Button>
              <Button
                onClick={() => {
                  setConcept("");
                  setContext("");
                  setResult(null);
                  setError(null);
                }}
                variant="ghost"
              >
                <Eraser className="size-4" />
                Clear
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
      {result ? (
        <Card className="border-brand-200">
          <CardContent className="p-6">
            <div className="flex flex-wrap items-center gap-2">
              <BrainCircuit className="size-5 text-brand-600" />
              <h2 className="font-semibold text-slate-950">{result.concept}</h2>
              <Badge className="ml-auto">{result.provider}</Badge>
            </div>
            <p className="mt-5 whitespace-pre-wrap text-sm leading-7 text-slate-600">
              {result.explanation}
            </p>
          </CardContent>
        </Card>
      ) : (
        <Card className="border-dashed">
          <CardContent className="grid min-h-72 place-items-center text-center text-sm text-slate-400">
            Your explanation will appear here.
          </CardContent>
        </Card>
      )}
    </div>
  );
}

