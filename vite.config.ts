import { fileURLToPath, URL } from "node:url";

import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// Groq / OpenRouter / Gemini are called DIRECTLY from the browser with each user's
// own key (bring-your-own-key) — those APIs allow CORS, so no proxy is needed and
// it works on the deployed static site, not just `npm run dev`. The only dev proxy
// left is Ollama (fully-local offline mode), which has no CORS headers of its own.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    proxy: {
      "/ollama": {
        target: "http://127.0.0.1:11434",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ollama/, ""),
      },
    },
  },
});
