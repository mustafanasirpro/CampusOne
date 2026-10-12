import { beforeEach, describe, expect, it } from "vitest";

import {
  clearApiKey,
  getApiKey,
  hasApiKey,
  keyHeader,
  providerInfo,
  setApiKey,
} from "./apiKeys";

beforeEach(() => {
  clearApiKey("groq");
  clearApiKey("openrouter");
  clearApiKey("gemini");
});

describe("apiKeys store", () => {
  it("saves, reads, and clears a key", () => {
    expect(hasApiKey("groq")).toBe(false);
    setApiKey("groq", "  gsk_abc123  ");
    expect(getApiKey("groq")).toBe("gsk_abc123"); // trimmed
    expect(hasApiKey("groq")).toBe(true);
    clearApiKey("groq");
    expect(hasApiKey("groq")).toBe(false);
  });

  it("builds the x-tutor-key header only when a key exists", () => {
    expect(keyHeader("groq")).toEqual({});
    setApiKey("groq", "gsk_xyz");
    expect(keyHeader("groq")).toEqual({ "x-tutor-key": "gsk_xyz" });
  });

  it("keeps providers independent", () => {
    setApiKey("openrouter", "sk-or-v1-abcdefghijklmnop");
    expect(hasApiKey("openrouter")).toBe(true);
    expect(hasApiKey("groq")).toBe(false);
  });
});

describe("provider key validation", () => {
  it("accepts a well-formed Groq key and rejects junk", () => {
    const groq = providerInfo("groq");
    expect(groq.validate("gsk_" + "a".repeat(40))).toBe(true);
    expect(groq.validate("not-a-key")).toBe(false);
    expect(groq.validate("sk-or-xxxx")).toBe(false);
  });

  it("accepts a well-formed OpenRouter key", () => {
    const or = providerInfo("openrouter");
    expect(or.validate("sk-or-v1-0123456789abcdef")).toBe(true);
    expect(or.validate("gsk_abc")).toBe(false);
  });
});
