# AI Live Whiteboard Tutor ("Aura")

A Jarvis-style teaching agent in CampusOne's AI module. You give Aura a topic and
it teaches it step by step on a **live whiteboard**, narrating as it goes. You can
**raise your hand** at any time to ask a confusion — Aura pauses, answers you, and
resumes the lesson.

It runs **completely free and offline** on a local model (no API keys, no cloud,
no account). Your lessons never leave your machine.

## How it works

```
Browser (React)  ──►  Vite dev proxy "/ollama"  ──►  Ollama (local)  ──►  qwen3:4b
     ▲                                                                         │
     └──────────────── streamed JSON "teaching beats" ◄───────────────────────┘
```

The model streams a lesson as **JSON "beats"**, one per teaching step:

```json
{"say":"Let's find 7 in a sorted list.","board":{"kind":"text","text":"[1, 3, 5, 7, 9]"}}
```

- `say` — what Aura narrates for this beat (shown in the transcript).
- `board` — what it draws: `title`, `text`, `bullet`, `equation`, `code`, `note`,
  `diagram`, or `clear` (null = just talk).

Beats are generated into a buffer, then **played back on a reading-paced timer**, so
it looks like deliberate live teaching rather than a sudden dump.

## Running it

**Prerequisites:** [Ollama](https://ollama.com) installed, and the model pulled:

```bash
ollama pull qwen3:4b
```

**Start it:**

```bash
ollama serve        # if not already running as a service
npm run dev
```

Then open **http://localhost:5173/tutor** — a standalone, login-free page (Phase 1).
Inside the authenticated app the same tutor lives at `/ai/tutor` (needs the backend
running for login).

### Configuration (optional)

Set in the root `.env`:

| Var | Default | Purpose |
|---|---|---|
| `VITE_TUTOR_MODEL` | `qwen3:4b` | Ollama model tag to use |
| `VITE_OLLAMA_PROXY` | `/ollama` | Proxy path to the Ollama server |

The Vite dev proxy (`vite.config.ts`) forwards `/ollama` → `http://127.0.0.1:11434`.

## Features

- **Live whiteboard** with titles, text, bullets, equations, code, notes, diagrams.
- **Raise-hand interruption** → contextual answer (highlighted) → **Resume**.
- **Lesson depth**: Quick (~4–6 beats) / Standard (~6–10) / Deep (~10–15).
- **Follow-ups**: "Go deeper", "Give an example", "Quiz me".
- **Reveal rest** to skip the pacing, **Stop** to pause.
- **Persistence**: your last lesson is saved locally and restored on refresh.
- **Model warm-up** on page load to hide the cold start.

## Performance note

On a CPU-only machine (no GPU), qwen3:4b generates at ~3 tokens/sec, and it is a
*reasoning* model that thinks in prose before answering. So expect roughly **~2 min
to the first beat and ~5 min per full lesson**. This is why the UI shows a clear
"preparing…" state and plays beats back at a readable pace. On a machine with a GPU,
or via a free hosted model (e.g. Groq), this would be near-instant.

### Why these model settings

- `think: false` request parameter disables Qwen3's separate reasoning trace.
  (Do **not** use `/no_think` in the prompt — on this Ollama build it forces
  all-thinking and emits no content.)
- The parser is **brace-matching**, so it tolerates the reasoning prose Qwen3
  still emits, pretty-printed JSON, ```json fences, and arrays.
- A **restart detector** stops the model's occasional "draft-then-restart" habit
  (repeated narration/board line, a second title, or a re-introduction greeting).
- `num_predict` cap + `keep_alive` keep generation bounded and the model warm.

## Code map

| File | Role |
|---|---|
| `src/api/tutorClient.ts` | Ollama streaming, prompt, JSON parser, think-stripper |
| `src/api/tutorClient.test.ts` | Parser + think-stripper unit tests |
| `src/components/ai/tutor/useTutor.ts` | State machine: teach → interrupt → answer → resume, pacing, persistence |
| `src/components/ai/tutor/Whiteboard.tsx` | The whiteboard renderer |
| `src/components/ai/tutor/TutorConsole.tsx` | Controls, transcript, interrupt box, depth |
| `src/pages/TutorPage.tsx` | Page shell (`/tutor` public, `/ai/tutor` in-app) |

## Roadmap

- **Phase 2 — Backend integration.** Move the model call behind a Spring
  `OllamaAiProvider` (implementing the existing `AiProvider`) + an SSE endpoint,
  reusing CampusOne's AI session/message/usage persistence and auth. Then fold the
  standalone `/tutor` route back behind login.
- **Phase 3 — Voice.** Add speech-to-text + text-to-speech (browser Web Speech API,
  free) for spoken teaching and spoken interruptions.
