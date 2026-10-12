// Bring-your-own-key (BYOK): every user adds their OWN free API keys, stored only
// in their browser (localStorage). The browser calls Groq / OpenRouter / Gemini
// DIRECTLY with the user's key (those APIs allow CORS) — no server, no proxy — so
// it works on the deployed site, and no one shares anyone else's key.

export type KeyProvider = "groq" | "openrouter" | "gemini";

// A LIST of keys per provider (JSON array). Users can add several free keys so the
// tutor fails over to the next when one is rate-limited or down. `OLD_PREFIX` held
// a single key in earlier versions — we migrate it into the list on first read.
const PREFIX = "campusone.tutor.keys.";
const OLD_PREFIX = "campusone.tutor.key.";

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

function saveKeys(provider: KeyProvider, keys: string[]): void {
  try {
    const clean = [...new Set(keys.map((k) => k.trim()).filter(Boolean))];
    if (clean.length) localStorage.setItem(PREFIX + provider, JSON.stringify(clean));
    else localStorage.removeItem(PREFIX + provider);
  } catch {
    // Storage unavailable (private mode / quota) — keys just won't persist.
  }
}

/** All stored keys for a provider (most-recently-added last). */
export function getApiKeys(provider: KeyProvider): string[] {
  try {
    const raw = localStorage.getItem(PREFIX + provider);
    if (raw) {
      const arr = JSON.parse(raw) as unknown;
      if (Array.isArray(arr)) {
        return arr
          .filter((k): k is string => typeof k === "string")
          .map((k) => k.trim())
          .filter(Boolean);
      }
    }
    // Migrate a single key saved by an earlier version into the list.
    const old = localStorage.getItem(OLD_PREFIX + provider)?.trim();
    if (old) {
      saveKeys(provider, [old]);
      localStorage.removeItem(OLD_PREFIX + provider);
      return [old];
    }
    return [];
  } catch {
    return [];
  }
}

/** The primary (first) key — for single-key callers like Gemini's query param. */
export function getApiKey(provider: KeyProvider): string {
  return getApiKeys(provider)[0] ?? "";
}

export function keyCount(provider: KeyProvider): number {
  return getApiKeys(provider).length;
}

export function hasApiKey(provider: KeyProvider): boolean {
  return getApiKeys(provider).length > 0;
}

/** Add a key to the provider's list (ignores duplicates and blanks). */
export function addApiKey(provider: KeyProvider, value: string): void {
  const k = value.trim();
  if (!k) return;
  const list = getApiKeys(provider);
  if (!list.includes(k)) saveKeys(provider, [...list, k]);
}

/** Remove one specific key value from the list. */
export function removeApiKey(provider: KeyProvider, value: string): void {
  saveKeys(provider, getApiKeys(provider).filter((k) => k !== value));
}

/** Remove ALL keys for a provider. */
export function clearApiKey(provider: KeyProvider): void {
  try {
    localStorage.removeItem(PREFIX + provider);
    localStorage.removeItem(OLD_PREFIX + provider);
  } catch {
    // ignore
  }
}

// --- Rotation / failover ----------------------------------------------------
// An in-memory cursor per provider: when a key is rate-limited or rejected, we
// advance it so the NEXT request starts from a different key, spreading load and
// skipping an exhausted one. `orderedKeys` returns the keys starting at the cursor.

const cursors: Partial<Record<KeyProvider, number>> = {};

export function orderedKeys(provider: KeyProvider): string[] {
  const keys = getApiKeys(provider);
  if (keys.length <= 1) return keys;
  const start = ((cursors[provider] ?? 0) % keys.length + keys.length) % keys.length;
  return [...keys.slice(start), ...keys.slice(0, start)];
}

/** Advance the rotation cursor (call after a key hits a rate limit / is rejected). */
export function rotateKey(provider: KeyProvider): void {
  const n = getApiKeys(provider).length;
  if (n > 1) cursors[provider] = ((cursors[provider] ?? 0) + 1) % n;
}

/**
 * `Authorization: Bearer <key>` for a direct browser call. Uses the primary key;
 * callers that need failover across keys use `orderedKeys` and build the header per
 * key instead (see streamChat). Empty when no key is stored.
 */
export function authHeader(provider: KeyProvider): Record<string, string> {
  const key = getApiKey(provider);
  return key ? { Authorization: `Bearer ${key}` } : {};
}

/** `Authorization` header for a specific key value. */
export function bearer(key: string): Record<string, string> {
  return key ? { Authorization: `Bearer ${key}` } : {};
}

/** Gemini passes the key as `?key=` — returns that query fragment (or ""). */
export function geminiKeyParam(): string {
  const key = getApiKey("gemini");
  return key ? `key=${encodeURIComponent(key)}` : "";
}
