// Bring-your-own-key (BYOK): every user adds their OWN free API keys, stored only
// in their browser (localStorage) and sent with each request as the `x-tutor-key`
// header. The dev proxy forwards that header to the provider — so no one shares
// anyone else's key, and keys never live in the shipped app. See vite.config.ts.

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
 * Header to send the user's own key through the dev proxy. Empty when none is
 * stored (the proxy then falls back to the dev's .env, if any). Keeping this in
 * one place means every request path attaches the key the same way.
 */
export function keyHeader(provider: KeyProvider): Record<string, string> {
  const key = getApiKey(provider);
  return key ? { "x-tutor-key": key } : {};
}
