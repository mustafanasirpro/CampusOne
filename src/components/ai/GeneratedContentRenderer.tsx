import { CheckCircle2, RotateCcw } from "lucide-react";
import { useState } from "react";

import { Badge, Button, Card, CardContent } from "@/components/common";
import type { AiGeneratedItemType } from "@/types/ai";
import { cn } from "@/utils/cn";

interface SummaryContent {
  keyPoints: string[];
  revisionNotes: string;
  shortSummary: string;
}

interface FlashcardContent {
  answer: string;
  question: string;
}

interface QuizContent {
  correctAnswer: string;
  explanation: string;
  options: string[];
  question: string;
}

interface StudyPlanDay {
  day: number;
  estimatedMinutes: number;
  tasks: string[];
  topic: string;
}

interface StudyPlanContent {
  dailyMinutes: number;
  days: number;
  goal: string;
  plan: StudyPlanDay[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isSummary(value: unknown): value is SummaryContent {
  return (
    isRecord(value) &&
    typeof value.shortSummary === "string" &&
    Array.isArray(value.keyPoints) &&
    value.keyPoints.every((item) => typeof item === "string") &&
    typeof value.revisionNotes === "string"
  );
}

function isFlashcards(value: unknown): value is FlashcardContent[] {
  return (
    Array.isArray(value) &&
    value.every(
      (item) =>
        isRecord(item) &&
        typeof item.question === "string" &&
        typeof item.answer === "string",
    )
  );
}

function isQuiz(value: unknown): value is QuizContent[] {
  return (
    Array.isArray(value) &&
    value.every(
      (item) =>
        isRecord(item) &&
        typeof item.question === "string" &&
        Array.isArray(item.options) &&
        item.options.every((option) => typeof option === "string") &&
        typeof item.correctAnswer === "string" &&
        typeof item.explanation === "string",
    )
  );
}

function isStudyPlan(value: unknown): value is StudyPlanContent {
  return (
    isRecord(value) &&
    typeof value.goal === "string" &&
    typeof value.days === "number" &&
    typeof value.dailyMinutes === "number" &&
    Array.isArray(value.plan) &&
    value.plan.every(
      (item) =>
        isRecord(item) &&
        typeof item.day === "number" &&
        typeof item.topic === "string" &&
        Array.isArray(item.tasks) &&
        item.tasks.every((task) => typeof task === "string") &&
        typeof item.estimatedMinutes === "number",
    )
  );
}

export function GeneratedContentRenderer({
  content,
  itemType,
}: {
  content: unknown;
  itemType: AiGeneratedItemType;
}) {
  const [revealedCards, setRevealedCards] = useState<Set<number>>(new Set());
  const [quizSelections, setQuizSelections] = useState<Record<number, string>>({});

  if (itemType === "SUMMARY" && isSummary(content)) {
    return (
      <div className="grid gap-5">
        <div>
          <h3 className="font-semibold text-slate-950">Short summary</h3>
          <p className="mt-2 text-sm leading-7 text-slate-600">
            {content.shortSummary}
          </p>
        </div>
        <div>
          <h3 className="font-semibold text-slate-950">Key points</h3>
          <ul className="mt-2 grid gap-2">
            {content.keyPoints.map((point, index) => (
              <li
                className="flex gap-2 text-sm text-slate-600"
                key={`${point}-${index}`}
              >
                <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-emerald-500" />
                {point}
              </li>
            ))}
          </ul>
        </div>
        <div className="rounded-xl bg-brand-50 p-4 text-sm leading-6 text-brand-800">
          <strong>Revision notes:</strong> {content.revisionNotes}
        </div>
      </div>
    );
  }

  if (itemType === "FLASHCARDS" && isFlashcards(content)) {
    return (
      <div className="grid gap-4 sm:grid-cols-2">
        {content.map((card, index) => {
          const revealed = revealedCards.has(index);
          return (
            <button
              className="min-h-44 rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm transition hover:border-brand-300"
              key={`${card.question}-${index}`}
              onClick={() =>
                setRevealedCards((current) => {
                  const next = new Set(current);
                  if (next.has(index)) next.delete(index);
                  else next.add(index);
                  return next;
                })
              }
              type="button"
            >
              <Badge variant="brand">Card {index + 1}</Badge>
              <p className="mt-4 font-semibold text-slate-950">{card.question}</p>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                {revealed ? card.answer : "Tap to reveal the answer"}
              </p>
            </button>
          );
        })}
      </div>
    );
  }

  if (itemType === "QUIZ" && isQuiz(content)) {
    return (
      <div className="grid gap-5">
        {content.map((question, index) => {
          const selected = quizSelections[index];
          return (
            <Card key={`${question.question}-${index}`}>
              <CardContent className="grid gap-4 p-5">
                <p className="font-semibold text-slate-950">
                  {index + 1}. {question.question}
                </p>
                <div className="grid gap-2">
                  {question.options.map((option, optionIndex) => (
                    <button
                      aria-pressed={selected === option}
                      className={cn(
                        "rounded-xl border px-3 py-2.5 text-left text-sm transition",
                        selected === option
                          ? "border-brand-400 bg-brand-50 text-brand-800"
                          : "border-slate-200 hover:bg-slate-50",
                      )}
                      key={`${option}-${optionIndex}`}
                      onClick={() =>
                        setQuizSelections((current) => ({
                          ...current,
                          [index]: option,
                        }))
                      }
                      type="button"
                    >
                      {option}
                    </button>
                  ))}
                </div>
                {selected ? (
                  <div className="rounded-xl bg-slate-50 p-4 text-sm leading-6">
                    <p className={selected === question.correctAnswer ? "font-semibold text-emerald-700" : "font-semibold text-red-700"}>
                      {selected === question.correctAnswer ? "Correct" : "Not quite"}
                    </p>
                    <p className="mt-1 text-slate-600">
                      Correct answer: {question.correctAnswer}
                    </p>
                    <p className="mt-1 text-slate-500">{question.explanation}</p>
                  </div>
                ) : null}
              </CardContent>
            </Card>
          );
        })}
        {Object.keys(quizSelections).length > 0 ? (
          <Button
            className="w-fit"
            onClick={() => setQuizSelections({})}
            variant="outline"
          >
            <RotateCcw className="size-4" />
            Reset answers
          </Button>
        ) : null}
      </div>
    );
  }

  if (itemType === "STUDY_PLAN" && isStudyPlan(content)) {
    return (
      <div className="grid gap-4">
        <div className="rounded-xl bg-brand-50 p-4">
          <p className="font-semibold text-brand-900">{content.goal}</p>
          <p className="mt-1 text-sm text-brand-700">
            {content.days} days · {content.dailyMinutes} minutes daily
          </p>
        </div>
        <div className="grid gap-3 md:grid-cols-2">
          {content.plan.map((day) => (
            <Card key={day.day}>
              <CardContent className="p-5">
                <Badge variant="brand">Day {day.day}</Badge>
                <h3 className="mt-3 font-semibold text-slate-950">{day.topic}</h3>
                <ul className="mt-3 grid gap-2">
                  {day.tasks.map((task, taskIndex) => (
                    <li
                      className="text-sm leading-6 text-slate-600"
                      key={`${task}-${taskIndex}`}
                    >
                      • {task}
                    </li>
                  ))}
                </ul>
                <p className="mt-3 text-xs font-semibold text-slate-400">
                  {day.estimatedMinutes} minutes
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  return (
    <pre className="max-h-[32rem] overflow-auto rounded-xl bg-slate-950 p-4 text-xs leading-6 text-slate-200">
      {JSON.stringify(content, null, 2)}
    </pre>
  );
}
