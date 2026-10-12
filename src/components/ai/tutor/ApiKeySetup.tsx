import {
  ArrowRight,
  Check,
  KeyRound,
  Lock,
  Plus,
  Sparkles,
  Trash2,
} from "lucide-react";
import { useState } from "react";

import {
  getApiKeys,
  hasApiKey,
  removeApiKey,
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
          const keys = getApiKeys(p.id);
          const added = keys.length > 0;
          return (
            <div
              key={p.id}
              className={cn(
                "grid gap-3 rounded-2xl border bg-white p-4 shadow-sm",
                added ? "border-campus-200" : "border-slate-200",
              )}
            >
              <div className="flex flex-wrap items-center gap-3">
                <span
                  className={cn(
                    "grid size-10 shrink-0 place-items-center rounded-xl",
                    added
                      ? "bg-campus-50 text-campus-600"
                      : "bg-slate-100 text-slate-400",
                  )}
                >
                  {added ? <Check className="size-5" /> : <KeyRound className="size-5" />}
                </span>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-sm font-semibold text-slate-800">{p.name}</h3>
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
                      <span className="rounded-full bg-campus-50 px-2 py-0.5 text-[11px] font-semibold text-campus-600">
                        {keys.length} key{keys.length > 1 ? "s" : ""}
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-0.5 text-xs text-slate-500">{p.purpose}</p>
                </div>

                <Button
                  variant={added ? "outline" : p.required ? "primary" : "outline"}
                  size="sm"
                  onClick={() => setOpenGuide(p.id)}
                >
                  <Plus className="size-4" />
                  {added ? "Add another" : `Add ${p.name} key`}
                  {added ? null : <ArrowRight className="size-4" />}
                </Button>
              </div>

              {/* The list of added keys, each removable. More than one = failover. */}
              {added ? (
                <ul className="grid gap-1.5 pl-1">
                  {keys.map((k, i) => (
                    <li
                      key={k}
                      className="flex items-center gap-2 rounded-lg bg-slate-50 px-2.5 py-1.5"
                    >
                      <span className="grid size-5 shrink-0 place-items-center rounded-full bg-white text-[10px] font-bold text-slate-400 ring-1 ring-slate-200">
                        {i + 1}
                      </span>
                      <span className="flex-1 font-mono text-xs text-slate-600">
                        {mask(k)}
                      </span>
                      <button
                        type="button"
                        onClick={() => {
                          removeApiKey(p.id, k);
                          refresh();
                        }}
                        title="Remove this key"
                        className="text-slate-400 transition hover:text-red-500"
                      >
                        <Trash2 className="size-3.5" />
                      </button>
                    </li>
                  ))}
                  {keys.length > 1 ? (
                    <li className="pl-1 text-[11px] text-campus-600">
                      ✓ Failover on — if one key is busy or rate-limited, the tutor
                      switches to the next automatically.
                    </li>
                  ) : p.id === "groq" ? (
                    <li className="pl-1 text-[11px] text-slate-400">
                      Tip: add a second key so lessons keep working when this one hits
                      its free per-minute limit.
                    </li>
                  ) : null}
                </ul>
              ) : null}
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
