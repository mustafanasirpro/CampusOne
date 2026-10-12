import {
  ArrowRight,
  Check,
  KeyRound,
  Lock,
  Pencil,
  Sparkles,
  Trash2,
} from "lucide-react";
import { useState } from "react";

import {
  clearApiKey,
  getApiKey,
  hasApiKey,
  PROVIDERS,
  type KeyProvider,
} from "@/api/apiKeys";
import { Button } from "@/components/common";
import { cn } from "@/utils/cn";

import { ApiKeyGuide } from "./ApiKeyGuide";

/** Mask a key for display: gsk_ABCD…WXYZ. */
function mask(key: string): string {
  if (key.length <= 10) return "•".repeat(key.length);
  return `${key.slice(0, 4)}…${key.slice(-4)}`;
}

export function ApiKeySetup({ onDone }: { onDone: () => void }) {
  const [openGuide, setOpenGuide] = useState<KeyProvider | null>(null);
  // Bumped on any key change so the cards re-read localStorage.
  const [, setVersion] = useState(0);
  const refresh = () => setVersion((v) => v + 1);

  const groqReady = hasApiKey("groq");

  if (openGuide) {
    return (
      <ApiKeyGuide
        provider={openGuide}
        onBack={() => setOpenGuide(null)}
        onSaved={() => {
          refresh();
          setOpenGuide(null);
        }}
      />
    );
  }

  return (
    <div className="grid gap-5">
      <div className="rounded-2xl border border-brand-200 bg-gradient-to-br from-brand-50 to-white p-5 shadow-sm">
        <div className="flex items-center gap-2">
          <span className="grid size-9 place-items-center rounded-xl bg-brand-600 text-white">
            <KeyRound className="size-5" />
          </span>
          <h2 className="text-lg font-bold text-slate-900">
            Add your own free API keys to begin
          </h2>
        </div>
        <p className="mt-2 max-w-2xl text-sm text-slate-600">
          This tutor runs on free AI services. To keep it free and fair for
          everyone, <strong>you use your own free keys</strong> — they stay in
          your browser and are never shared. It takes about a minute. Start with
          Groq (required); the others are optional extras.
        </p>
      </div>

      <div className="grid gap-3">
        {PROVIDERS.map((p) => {
          const added = hasApiKey(p.id);
          return (
            <div
              key={p.id}
              className={cn(
                "flex flex-wrap items-center gap-3 rounded-2xl border bg-white p-4 shadow-sm",
                added ? "border-campus-200" : "border-slate-200",
              )}
            >
              <span
                className={cn(
                  "grid size-10 shrink-0 place-items-center rounded-xl",
                  added ? "bg-campus-50 text-campus-600" : "bg-slate-100 text-slate-400",
                )}
              >
                {added ? <Check className="size-5" /> : <KeyRound className="size-5" />}
              </span>

              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-sm font-semibold text-slate-800">
                    {p.name}
                  </h3>
                  <span
                    className={cn(
                      "rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
                      p.required
                        ? "bg-brand-100 text-brand-700"
                        : "bg-slate-100 text-slate-500",
                    )}
                  >
                    {p.required ? "Required" : "Optional"}
                  </span>
                  {added ? (
                    <span className="font-mono text-xs text-campus-600">
                      {mask(getApiKey(p.id))}
                    </span>
                  ) : null}
                </div>
                <p className="mt-0.5 text-xs text-slate-500">{p.purpose}</p>
              </div>

              <div className="flex shrink-0 items-center gap-2">
                {added ? (
                  <>
                    <button
                      type="button"
                      onClick={() => setOpenGuide(p.id)}
                      className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs text-slate-600 transition hover:border-brand-300"
                    >
                      <Pencil className="size-3.5" />
                      Edit
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        clearApiKey(p.id);
                        refresh();
                      }}
                      title="Remove this key"
                      className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs text-slate-400 transition hover:border-red-300 hover:text-red-500"
                    >
                      <Trash2 className="size-3.5" />
                    </button>
                  </>
                ) : (
                  <Button
                    variant={p.required ? "primary" : "outline"}
                    size="sm"
                    onClick={() => setOpenGuide(p.id)}
                  >
                    Add {p.name} key
                    <ArrowRight className="size-4" />
                  </Button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
        <p className="inline-flex items-center gap-1.5 text-xs text-slate-400">
          <Lock className="size-3.5" />
          Keys are saved only in this browser (localStorage). Clear them anytime.
        </p>
        <Button onClick={onDone} disabled={!groqReady}>
          <Sparkles className="size-4" />
          {groqReady ? "Start learning" : "Add your Groq key to start"}
        </Button>
      </div>
    </div>
  );
}
