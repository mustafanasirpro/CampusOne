import type { IncomingMessage } from "node:http";
import { fileURLToPath, URL } from "node:url";

import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv, type ProxyOptions } from "vite";

export default defineConfig(({ mode }) => {
  // Load all env vars (including non-VITE_ ones like GROQ_API_KEY) for the dev proxy.
  const env = loadEnv(mode, process.cwd(), "");

  // Bring-your-own-key: the browser sends the user's own key as `x-tutor-key`.
  // Prefer that over the dev's .env, so each visitor uses their OWN free key and
  // nobody rides on the owner's. (.env is only a convenience fallback for the dev.)
  const userKey = (req: IncomingMessage): string => {
    const h = req.headers["x-tutor-key"];
    return (Array.isArray(h) ? h[0] : h)?.trim() ?? "";
  };

  // Inject `Authorization: Bearer <key>` from the user's key (or .env fallback).
  const bearerProxy = (target: string, envKey?: string): ProxyOptions => ({
    target,
    changeOrigin: true,
    secure: true,
    configure: (proxy) => {
      proxy.on("proxyReq", (proxyReq, req) => {
        const key = userKey(req) || envKey || "";
        if (key) proxyReq.setHeader("authorization", `Bearer ${key}`);
        proxyReq.removeHeader("x-tutor-key");
      });
    },
  });

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
      },
    },
    server: {
      proxy: {
        // Live tutor (local): proxy to the local Ollama server so the SPA can
        // stream from it same-origin in dev.
        "/ollama": {
          target: "http://127.0.0.1:11434",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/ollama/, ""),
        },
        // Live tutor (Groq free API): the user's own key (x-tutor-key header) is
        // injected as Authorization server-side so it never rides in the URL and
        // CORS is avoided. Falls back to GROQ_API_KEY in .env for the dev only.
        "/groq": {
          ...bearerProxy("https://api.groq.com", env.GROQ_API_KEY),
          rewrite: (path) => path.replace(/^\/groq/, ""),
        },
        // OpenRouter (vision model for drawing/pointing). Same BYOK scheme.
        "/openrouter": {
          ...bearerProxy("https://openrouter.ai", env.OPENROUTER_API_KEY),
          rewrite: (path) => path.replace(/^\/openrouter/, ""),
        },
        // Gemini (image generation). The key is a query param, so inject it onto
        // the proxied path from the user's key (or .env fallback).
        "/gemini": {
          target: "https://generativelanguage.googleapis.com",
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path.replace(/^\/gemini/, ""),
          configure: (proxy) => {
            proxy.on("proxyReq", (proxyReq, req) => {
              const key = userKey(req) || env.GEMINI_API_KEY || "";
              if (key) {
                const sep = proxyReq.path.includes("?") ? "&" : "?";
                proxyReq.path += `${sep}key=${key}`;
              }
              proxyReq.removeHeader("x-tutor-key");
            });
          },
        },
      },
    },
  };
});
