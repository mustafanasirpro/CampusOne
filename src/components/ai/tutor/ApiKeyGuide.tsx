import {
  ArrowLeft,
  Check,
  ExternalLink,
  Eye,
  EyeOff,
  ShieldCheck,
} from "lucide-react";
import { useState, type ReactNode } from "react";

import {
  getApiKey,
  providerInfo,
  setApiKey,
  type KeyProvider,
} from "@/api/apiKeys";
import { Button } from "@/components/common";
import { cn } from "@/utils/cn";

import {
  CopyKeyScene,
  CreateKeyScene,
  GuideShot,
  NameKeyScene,
  SidebarScene,
  SignInScene,
} from "./guideScenes";

interface Step {
  title: string;
  body: string;
  art: ReactNode;
}

const GUIDES: Record<KeyProvider, (host: string) => Step[]> = {
  groq: (host) => [
    {
      title: "Open Groq and sign in",
      body: "Click the button above to open the Groq console in a new tab, then sign in free with Google, GitHub, or email. No credit card needed.",
      art: (
        <GuideShot url={host}>
          <SignInScene n={1} brand="Groq" />
        </GuideShot>
      ),
    },
    {
      title: 'Open the "API Keys" page',
      body: 'In the left menu, click "API Keys". (The button above already opens this page for you.)',
      art: (
        <GuideShot url="console.groq.com/keys">
          <SidebarScene n={2} item="API Keys" url="Groq" />
        </GuideShot>
      ),
    },
    {
      title: 'Click "Create API Key"',
      body: 'Press the "Create API Key" button in the top-right.',
      art: (
        <GuideShot url="console.groq.com/keys">
          <CreateKeyScene n={3} label="Create API Key" title="API Keys" />
        </GuideShot>
      ),
    },
    {
      title: "Name it and submit",
      body: 'Type any name (e.g. "my-tutor-key") and click Submit.',
      art: (
        <GuideShot url="console.groq.com/keys">
          <NameKeyScene n={4} action="Submit" />
        </GuideShot>
      ),
    },
    {
      title: "Copy your key",
      body: "Click Copy. Groq shows the key only once, so copy it now — then paste it below.",
      art: (
        <GuideShot url="console.groq.com/keys">
          <CopyKeyScene n={5} />
        </GuideShot>
      ),
    },
  ],
  openrouter: (host) => [
    {
      title: "Open OpenRouter and sign in",
      body: "Click the button above to open OpenRouter in a new tab, then sign in free (Google, GitHub, email, or even MetaMask).",
      art: (
        <GuideShot url={host}>
          <SignInScene n={1} brand="OpenRouter" />
        </GuideShot>
      ),
    },
    {
      title: 'Click "Create Key"',
      body: 'On the Keys page, click "Create Key". You can leave the credit limit blank to use free models.',
      art: (
        <GuideShot url="openrouter.ai/keys">
          <CreateKeyScene n={2} label="Create Key" title="API Keys" />
        </GuideShot>
      ),
    },
    {
      title: "Name it and create",
      body: 'Give it a name (e.g. "campus-tutor") and click Create.',
      art: (
        <GuideShot url="openrouter.ai/keys">
          <NameKeyScene n={3} action="Create" />
        </GuideShot>
      ),
    },
    {
      title: "Copy your key",
      body: "Copy the key that appears (it starts with sk-or-), then paste it below.",
      art: (
        <GuideShot url="openrouter.ai/keys">
          <CopyKeyScene n={4} />
        </GuideShot>
      ),
    },
  ],
  gemini: (host) => [
    {
      title: "Open Google AI Studio",
      body: "Click the button above and sign in with your Google account.",
      art: (
        <GuideShot url={host}>
          <SignInScene n={1} brand="Google AI Studio" />
        </GuideShot>
      ),
    },
    {
      title: 'Click "Create API key"',
      body: 'Press "Create API key" and pick a Google Cloud project (a new one is fine).',
      art: (
        <GuideShot url="aistudio.google.com/apikey">
          <CreateKeyScene n={2} label="Create API key" title="API keys" />
        </GuideShot>
      ),
    },
    {
      title: "Copy your key",
      body: "Copy the generated key, then paste it below. (Image generation needs billing enabled on the project.)",
      art: (
        <GuideShot url="aistudio.google.com/apikey">
          <CopyKeyScene n={3} />
        </GuideShot>
      ),
    },
  ],
};

export function ApiKeyGuide({
  provider,
  onBack,
  onSaved,
}: {
  provider: KeyProvider;
  onBack: () => void;
  onSaved: () => void;
}) {
  const info = providerInfo(provider);
  const host = info.consoleUrl.replace(/^https?:\/\//, "");
  const steps = GUIDES[provider](host);

  const [value, setValue] = useState(() => getApiKey(provider));
  const [reveal, setReveal] = useState(false);
  const [touched, setTouched] = useState(false);

  const valid = info.validate(value);
  const showError = touched && value.trim().length > 0 && !valid;

  const handleSave = () => {
    setTouched(true);
    if (!valid) return;
    setApiKey(provider, value);
    onSaved();
  };

  return (
    <div className="grid gap-5">
      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2.5 py-1.5 text-sm text-slate-600 transition hover:border-brand-300"
        >
          <ArrowLeft className="size-4" />
          Back
        </button>
        <h2 className="text-lg font-semibold text-slate-800">
          Get your free {info.name} API key
        </h2>
        <a
          href={info.consoleUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="ml-auto inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-700"
        >
          <ExternalLink className="size-4" />
          Open {info.name}
        </a>
      </div>

      <p className="rounded-xl border border-brand-100 bg-brand-50/60 px-4 py-3 text-sm text-slate-600">
        {info.purpose} Follow the steps below — each picture shows roughly what
        you&apos;ll see and marks where to click.
      </p>

      <ol className="grid gap-4">
        {steps.map((step, i) => (
          <li
            key={step.title}
            className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:grid-cols-[1fr_minmax(0,16rem)] sm:items-center"
          >
            <div className="flex gap-3">
              <span className="grid size-7 shrink-0 place-items-center rounded-full bg-brand-600 text-sm font-bold text-white">
                {i + 1}
              </span>
              <div>
                <h3 className="text-sm font-semibold text-slate-800">
                  {step.title}
                </h3>
                <p className="mt-1 text-sm text-slate-500">{step.body}</p>
              </div>
            </div>
            <div className="sm:order-last">{step.art}</div>
          </li>
        ))}
      </ol>

      {/* Paste + save */}
      <div className="grid gap-2 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <label
          htmlFor={`key-${provider}`}
          className="text-sm font-semibold text-slate-800"
        >
          Paste your {info.name} key here ({info.keyHint})
        </label>
        <div className="flex items-center gap-2">
          <div className="flex flex-1 items-center rounded-xl border border-slate-300 px-3 focus-within:border-brand-500 focus-within:ring-2 focus-within:ring-brand-200">
            <input
              id={`key-${provider}`}
              type={reveal ? "text" : "password"}
              autoComplete="off"
              spellCheck={false}
              className="h-11 flex-1 bg-transparent text-sm outline-none"
              placeholder={info.keyHint}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              onBlur={() => setTouched(true)}
            />
            <button
              type="button"
              onClick={() => setReveal((r) => !r)}
              title={reveal ? "Hide" : "Show"}
              className="text-slate-400 transition hover:text-slate-600"
            >
              {reveal ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
          <Button onClick={handleSave} disabled={!value.trim()}>
            <Check className="size-4" />
            Save key
          </Button>
        </div>
        {showError ? (
          <p className="text-xs text-red-600">
            That doesn&apos;t look like a {info.name} key ({info.keyHint}). Double-check
            you copied the whole thing.
          </p>
        ) : (
          <p
            className={cn(
              "inline-flex items-center gap-1.5 text-xs",
              valid ? "text-campus-600" : "text-slate-400",
            )}
          >
            <ShieldCheck className="size-3.5" />
            Your key is stored only in this browser and sent straight to{" "}
            {info.name} — never to us or anyone else.
          </p>
        )}
      </div>
    </div>
  );
}
