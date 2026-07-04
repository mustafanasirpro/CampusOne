import { Save } from "lucide-react";
import { useState, type FormEvent } from "react";

import { Button, Card, CardContent } from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import { discussionCategoryOptions } from "@/components/discussion/discussionFormatting";
import type {
  CreateDiscussionQuestionRequest,
  DiscussionCategory,
  DiscussionQuestionDetail,
  DiscussionQuestionUpdateStatus,
  UpdateDiscussionQuestionRequest,
} from "@/types/discussion";
import { cn } from "@/utils/cn";

interface QuestionFormState {
  body: string;
  category: DiscussionCategory;
  status: DiscussionQuestionUpdateStatus | "";
  title: string;
}

type QuestionFormErrors = Partial<Record<keyof QuestionFormState, string>>;

interface CommonQuestionFormProps {
  backendFieldErrors?: Record<string, string[]>;
  isSubmitting: boolean;
  submitLabel: string;
}

type QuestionFormProps =
  | (CommonQuestionFormProps & {
      mode: "create";
      onSubmit: (request: CreateDiscussionQuestionRequest) => Promise<void>;
    })
  | (CommonQuestionFormProps & {
      initialQuestion: DiscussionQuestionDetail;
      mode: "edit";
      onSubmit: (request: UpdateDiscussionQuestionRequest) => Promise<void>;
    });

function initialState(
  question?: DiscussionQuestionDetail,
): QuestionFormState {
  return {
    body: question?.body ?? "",
    category: question?.category ?? "GENERAL",
    status:
      question?.status === "OPEN" || question?.status === "CLOSED"
        ? question.status
        : "",
    title: question?.title ?? "",
  };
}

export function QuestionForm(props: QuestionFormProps) {
  const initialQuestion =
    props.mode === "edit" ? props.initialQuestion : undefined;
  const [form, setForm] = useState<QuestionFormState>(() =>
    initialState(initialQuestion),
  );
  const [errors, setErrors] = useState<QuestionFormErrors>({});

  const update = <Key extends keyof QuestionFormState>(
    key: Key,
    value: QuestionFormState[Key],
  ) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  };

  const backendError = (key: string) =>
    props.backendFieldErrors?.[key]?.[0];

  const validate = () => {
    const nextErrors: QuestionFormErrors = {};
    if (form.title.trim().length < 5) {
      nextErrors.title = "Title must contain at least 5 characters.";
    }
    if (form.body.trim().length < 10) {
      nextErrors.body = "Question details must contain at least 10 characters.";
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;

    const request: CreateDiscussionQuestionRequest = {
      body: form.body.trim(),
      category: form.category,
      title: form.title.trim(),
    };

    if (props.mode === "create") {
      await props.onSubmit(request);
      return;
    }

    const updateRequest: UpdateDiscussionQuestionRequest = {};
    if (request.title !== props.initialQuestion.title) {
      updateRequest.title = request.title;
    }
    if (request.body !== props.initialQuestion.body) {
      updateRequest.body = request.body;
    }
    if (request.category !== props.initialQuestion.category) {
      updateRequest.category = request.category;
    }
    if (form.status && form.status !== props.initialQuestion.status) {
      updateRequest.status = form.status;
    }
    await props.onSubmit(updateRequest);
  };

  return (
    <form className="grid gap-6" noValidate onSubmit={handleSubmit}>
      <Card>
        <CardContent className="grid gap-5 p-5 sm:p-6">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">
              Question details
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              A focused title and clear context help the community respond.
            </p>
          </div>

          <FormField
            error={errors.title ?? backendError("title")}
            label="Question title"
            maxLength={180}
            onChange={(event) => update("title", event.target.value)}
            placeholder="How does dependency injection work in Spring?"
            required
            value={form.title}
          />

          <label className="grid gap-1.5">
            <span className="text-sm font-semibold text-slate-700">
              Question details
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </span>
            <textarea
              aria-invalid={Boolean(errors.body ?? backendError("body"))}
              className={cn(
                "min-h-48 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition placeholder:text-slate-400 focus:ring-4",
                errors.body ?? backendError("body")
                  ? "border-red-300 focus:border-red-400 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              maxLength={5000}
              onChange={(event) => update("body", event.target.value)}
              placeholder="Explain what you have tried, where you are stuck, and what outcome you expect."
              required
              value={form.body}
            />
            <span className="flex justify-between gap-3 text-xs">
              <span
                className={
                  errors.body ?? backendError("body")
                    ? "font-medium text-red-600"
                    : "text-slate-500"
                }
              >
                {errors.body ??
                  backendError("body") ??
                  "Use plain text and include enough context to answer well."}
              </span>
              <span className="shrink-0 text-slate-400">
                {form.body.length}/5000
              </span>
            </span>
          </label>

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={errors.category ?? backendError("category")}
              label="Category"
              onChange={(event) =>
                update(
                  "category",
                  event.target.value as DiscussionCategory,
                )
              }
              options={discussionCategoryOptions}
              required
              value={form.category}
            />

            {props.mode === "edit" ? (
              <SelectField
                error={errors.status ?? backendError("status")}
                label="Question status"
                onChange={(event) =>
                  update(
                    "status",
                    event.target.value as
                      | DiscussionQuestionUpdateStatus
                      | "",
                  )
                }
                options={[
                  ...(props.initialQuestion.status === "RESOLVED" ||
                  props.initialQuestion.status === "HIDDEN"
                    ? [
                        {
                          label: `Keep ${props.initialQuestion.status.toLowerCase()}`,
                          value: "",
                        },
                      ]
                    : []),
                  { label: "Open", value: "OPEN" },
                  { label: "Closed", value: "CLOSED" },
                ]}
                value={form.status}
              />
            ) : null}
          </div>
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button
          className="w-full sm:w-auto"
          loading={props.isSubmitting}
          size="lg"
          type="submit"
        >
          <Save className="size-4" />
          {props.isSubmitting ? "Saving question" : props.submitLabel}
        </Button>
      </div>
    </form>
  );
}
