// Bring-your-own-key (BYOK): every user adds their OWN free API keys, stored only
// in their browser (localStorage). The browser calls Groq / OpenRouter / Gemini
// DIRECTLY with the user's key (those APIs allow CORS) — no server, no proxy — so
// it works on the deployed site, and no one shares anyone else's key.

export type KeyProvider = "groq" | "openrouter" | "gemini";

const PREFIX = "campusone.tutor.key.";

export interface ProviderInfo {
  id: KeyProvider;
  name: string;
  /** Why the user needs it. */
  purpose: string;
  /** Required to use the tutor at all, or optional (unlocks an extra feature)? */
  required: boolean;
  /** Where to create the key. */
  consoleUrl: string;
  /** A cheap sanity check on the pasted value (prefix), to catch obvious mistakes. */
  keyHint: string;
  validate: (value: string) => boolean;
}

export const PROVIDERS: ProviderInfo[] = [
  {
    id: "groq",
    name: "Groq",
    purpose: "Powers the tutor — thinking, teaching, and answering. This is the one you must add.",
    required: true,
    consoleUrl: "https://console.groq.com/keys",
    keyHint: "starts with gsk_",
    validate: (v) => /^gsk_[A-Za-z0-9]{20,}$/.test(v.trim()),
  },
  {
    id: "openrouter",
    name: "OpenRouter",
    purpose: 'Optional — lets the tutor SEE your drawing when you use "Ask about my drawing".',
    required: false,
    consoleUrl: "https://openrouter.ai/keys",
    keyHint: "starts with sk-or-",
    validate: (v) => /^sk-or-\S{10,}$/.test(v.trim()),
  },
  {
    id: "gemini",
    name: "Google Gemini",
    purpose: "Optional — higher-quality generated images (needs Google billing; images also work free without it).",
    required: false,
    consoleUrl: "https://aistudio.google.com/apikey",
    keyHint: "a long key",
    validate: (v) => v.trim().length >= 20,
  },
];

export function providerInfo(id: KeyProvider): ProviderInfo {
  return PROVIDERS.find((p) => p.id === id) ?? PROVIDERS[0];
}

export function getApiKey(provider: KeyProvider): string {
  try {
    return localStorage.getItem(PREFIX + provider)?.trim() ?? "";
  } catch {
    return "";
  }
}

export function hasApiKey(provider: KeyProvider): boolean {
  return getApiKey(provider).length > 0;
}

export function setApiKey(provider: KeyProvider, value: string): void {
  try {
    const clean = value.trim();
    if (clean) localStorage.setItem(PREFIX + provider, clean);
    else localStorage.removeItem(PREFIX + provider);
  } catch {
    // Storage unavailable (private mode / quota) — the key just won't persist.
  }
}

export function clearApiKey(provider: KeyProvider): void {
  try {
    localStorage.removeItem(PREFIX + provider);
  } catch {
    // ignore
  }
}

/**
 * `Authorization: Bearer <key>` for a DIRECT browser call to the provider (Groq,
 * OpenRouter — both allow CORS). Empty when no key is stored. This is how the
 * deployed site uses each user's own key: no server, no proxy, straight to the
 * provider. Gemini takes its key as a query param instead — see `geminiKeyParam`.
 */
export function authHeader(provider: KeyProvider): Record<string, string> {
  const key = getApiKey(provider);
  return key ? { Authorization: `Bearer ${key}` } : {};
}

/** Gemini passes the key as `?key=` — returns that query fragment (or ""). */
export function geminiKeyParam(): string {
  const key = getApiKey("gemini");
  return key ? `key=${encodeURIComponent(key)}` : "";
}
