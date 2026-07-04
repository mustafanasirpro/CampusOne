import { CalendarRange, WandSparkles } from "lucide-react";
import { useState, type FormEvent } from "react";

import { ApiError } from "@/api/apiClient";
import { generateStudyPlan } from "@/api/aiApi";
import {
  Button,
  Card,
  CardContent,
  ErrorMessage,
} from "@/components/common";
import { FormField } from "@/components/forms";
import { GeneratedContentRenderer } from "@/components/ai/GeneratedContentRenderer";
import type { AiGeneratedItem } from "@/types/ai";

export function StudyPlanPanel() {
  const [goal, setGoal] = useState("");
  const [days, setDays] = useState("7");
  const [dailyMinutes, setDailyMinutes] = useState("60");
  const [context, setContext] = useState("");
  const [result, setResult] = useState<AiGeneratedItem | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (goal.trim().length < 5) {
      setError("Describe your study goal using at least 5 characters.");
      return;
    }
    const dayCount = Number(days);
    const minutes = Number(dailyMinutes);
    if (!Number.isInteger(dayCount) || dayCount < 1 || dayCount > 90) {
      setError("Study plans must contain between 1 and 90 days.");
      return;
    }
    if (!Number.isInteger(minutes) || minutes < 10 || minutes > 600) {
      setError("Daily study time must be between 10 and 600 minutes.");
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      setResult(
        await generateStudyPlan({
          context: context.trim() || null,
          dailyMinutes: minutes,
          days: dayCount,
          goal: goal.trim(),
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
    <div className="grid gap-6">
      <Card>
        <CardContent>
          <form className="grid gap-5" onSubmit={submit}>
            <div className="flex items-center gap-3">
              <CalendarRange className="size-6 text-brand-600" />
              <div>
                <h2 className="text-lg font-semibold text-slate-950">
                  Study plan generator
                </h2>
                <p className="text-sm text-slate-500">
                  Build a realistic day-by-day plan around your available time.
                </p>
              </div>
            </div>
            <FormField
              label="Study goal"
              maxLength={500}
              onChange={(event) => setGoal(event.target.value)}
              required
              value={goal}
            />
            <div className="grid gap-5 sm:grid-cols-2">
              <FormField
                label="Days"
                max={90}
                min={1}
                onChange={(event) => setDays(event.target.value)}
                required
                type="number"
                value={days}
              />
              <FormField
                label="Daily minutes"
                max={600}
                min={10}
                onChange={(event) => setDailyMinutes(event.target.value)}
                required
                type="number"
                value={dailyMinutes}
              />
            </div>
            <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
              Optional context
              <textarea
                className="min-h-32 rounded-xl border border-slate-200 px-3.5 py-3 text-sm font-normal leading-6 outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
                maxLength={5000}
                onChange={(event) => setContext(event.target.value)}
                value={context}
              />
            </label>
            {error ? <ErrorMessage message={error} /> : null}
            <Button
              className="w-full sm:w-fit"
              loading={isLoading}
              type="submit"
            >
              <WandSparkles className="size-4" />
              Generate study plan
            </Button>
          </form>
        </CardContent>
      </Card>
      {result ? (
        <GeneratedContentRenderer
          content={result.generatedContent}
          itemType={result.itemType}
        />
      ) : null}
    </div>
  );
}
