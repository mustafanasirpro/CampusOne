import { useState, type FormEvent } from "react";

import {
  Button,
  ErrorMessage,
  Modal,
} from "@/components/common";

export type ResolutionMode = "dismiss" | "resolve";

export function ResolutionModal({
  error,
  isLoading,
  mode,
  onClose,
  onSubmit,
}: {
  error: string | null;
  isLoading: boolean;
  mode: ResolutionMode | null;
  onClose: () => void;
  onSubmit: (resolutionNote: string) => Promise<void>;
}) {
  const [note, setNote] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalized = note.trim();
    if (normalized.length < 3) {
      setValidationError("Resolution note is required.");
      return;
    }
    setValidationError(null);
    void onSubmit(normalized);
  };

  return (
    <Modal
      footer={
        mode ? (
          <>
            <Button onClick={onClose} variant="outline">
              Cancel
            </Button>
            <Button
              form="moderation-resolution-form"
              loading={isLoading}
              type="submit"
              variant={mode === "dismiss" ? "danger" : "primary"}
            >
              {mode === "dismiss" ? "Dismiss report" : "Resolve report"}
            </Button>
          </>
        ) : undefined
      }
      isOpen={Boolean(mode)}
      onClose={onClose}
      title={mode === "dismiss" ? "Dismiss report" : "Resolve report"}
    >
      <form
        className="grid gap-4"
        id="moderation-resolution-form"
        onSubmit={submit}
      >
        <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
          Resolution note
          <textarea
            autoFocus
            className="min-h-36 rounded-xl border border-slate-200 px-3.5 py-3 text-sm font-normal leading-6 outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
            maxLength={1000}
            onChange={(event) => setNote(event.target.value)}
            placeholder="Explain the moderation decision clearly."
            required
            value={note}
          />
          <span className="text-xs font-normal text-slate-400">
            {note.length}/1000
          </span>
        </label>
        {validationError || error ? (
          <ErrorMessage message={validationError ?? error ?? ""} />
        ) : null}
      </form>
    </Modal>
  );
}
