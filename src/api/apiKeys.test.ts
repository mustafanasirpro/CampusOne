import { beforeEach, describe, expect, it } from "vitest";

import {
  addApiKey,
  authHeader,
  clearApiKey,
  geminiKeyParam,
  getApiKey,
  getApiKeys,
  hasApiKey,
  keyCount,
  orderedKeys,
  providerInfo,
  removeApiKey,
  rotateKey,
} from "./apiKeys";

beforeEach(() => {
  clearApiKey("groq");
  clearApiKey("openrouter");
  clearApiKey("gemini");
});

describe("apiKeys store", () => {
  it("adds, reads, and clears keys", () => {
    expect(hasApiKey("groq")).toBe(false);
    addApiKey("groq", "  gsk_abc123  ");
    expect(getApiKey("groq")).toBe("gsk_abc123"); // trimmed, first key
    expect(hasApiKey("groq")).toBe(true);
    clearApiKey("groq");
    expect(hasApiKey("groq")).toBe(false);
  });

  it("stores MULTIPLE keys and ignores duplicates", () => {
    addApiKey("groq", "gsk_one");
    addApiKey("groq", "gsk_two");
    addApiKey("groq", "gsk_one"); // duplicate, ignored
    expect(getApiKeys("groq")).toEqual(["gsk_one", "gsk_two"]);
    expect(keyCount("groq")).toBe(2);
  });

  it("removes a single key from the list", () => {
    addApiKey("groq", "gsk_one");
    addApiKey("groq", "gsk_two");
    removeApiKey("groq", "gsk_one");
    expect(getApiKeys("groq")).toEqual(["gsk_two"]);
  });

  it("rotates keys for failover", () => {
    addApiKey("groq", "gsk_a");
    addApiKey("groq", "gsk_b");
    addApiKey("groq", "gsk_c");
    expect(orderedKeys("groq")).toEqual(["gsk_a", "gsk_b", "gsk_c"]);
    rotateKey("groq");
    expect(orderedKeys("groq")).toEqual(["gsk_b", "gsk_c", "gsk_a"]);
    rotateKey("groq");
    expect(orderedKeys("groq")).toEqual(["gsk_c", "gsk_a", "gsk_b"]);
  });

  it("builds the Authorization header only when a key exists", () => {
    expect(authHeader("groq")).toEqual({});
    addApiKey("groq", "gsk_xyz");
    expect(authHeader("groq")).toEqual({ Authorization: "Bearer gsk_xyz" });
  });

  it("builds the Gemini key query param only when a key exists", () => {
    expect(geminiKeyParam()).toBe("");
    addApiKey("gemini", "AQ.abc 123");
    expect(geminiKeyParam()).toBe("key=AQ.abc%20123"); // url-encoded
  });

  it("keeps providers independent", () => {
    addApiKey("openrouter", "sk-or-v1-abcdefghijklmnop");
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
