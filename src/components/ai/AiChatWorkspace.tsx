import {
  Bot,
  MessageSquarePlus,
  Send,
  Trash2,
} from "lucide-react";
import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type KeyboardEvent,
} from "react";

import { ApiError } from "@/api/apiClient";
import {
  createAiSession,
  deleteAiSession,
  getAiSession,
  listAiSessions,
  sendAiMessage,
} from "@/api/aiApi";
import {
  Button,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  useToast,
} from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import { AiMessageBubble } from "@/components/ai/AiMessageBubble";
import {
  aiModeLabel,
  aiSessionModeOptions,
  formatAiDate,
} from "@/components/ai/aiFormatting";
import type {
  AiSession,
  AiSessionDetail,
  AiSessionMode,
} from "@/types/ai";
import { cn } from "@/utils/cn";

export function AiChatWorkspace() {
  const { showToast } = useToast();
  const [sessions, setSessions] = useState<AiSession[]>([]);
  const [activeSession, setActiveSession] =
    useState<AiSessionDetail | null>(null);
  const [newTitle, setNewTitle] = useState("");
  const [newMode, setNewMode] =
    useState<AiSessionMode>("GENERAL_CHAT");
  const [message, setMessage] = useState("");
  const [provider, setProvider] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoadingSessions, setIsLoadingSessions] = useState(true);
  const [isLoadingChat, setIsLoadingChat] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listAiSessions({ signal: controller.signal, size: 50 })
      .then(async (response) => {
        if (!active) return;
        setSessions(response.content);
        if (response.content[0]) {
          const detail = await getAiSession(
            response.content[0].id,
            controller.signal,
          );
          if (active) setActiveSession(detail);
        }
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "AI chat sessions could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoadingSessions(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "nearest",
    });
  }, [activeSession?.messages.length, isSending]);

  const selectSession = async (sessionId: string) => {
    setIsLoadingChat(true);
    setError(null);
    setProvider(null);
    try {
      setActiveSession(await getAiSession(sessionId));
    } catch (requestError) {
      setError(
        requestError instanceof ApiError && requestError.status === 404
          ? "Your session was not found."
          : requestError instanceof ApiError
            ? requestError.message
            : "The chat session could not be opened.",
      );
    } finally {
      setIsLoadingChat(false);
    }
  };

  const createSession = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const title = newTitle.trim();
    if (title.length < 3) {
      setError("Session titles must contain at least 3 characters.");
      return;
    }
    setIsCreating(true);
    setError(null);
    try {
      const session = await createAiSession({ mode: newMode, title });
      setSessions((current) => [session, ...current]);
      setActiveSession({ ...session, messages: [] });
      setNewTitle("");
      setProvider(null);
      showToast({
        title: "Study session created",
        message: session.title,
        variant: "success",
      });
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "The study session could not be created.",
      );
    } finally {
      setIsCreating(false);
    }
  };

  const removeSession = async (session: AiSession) => {
    if (!window.confirm(`Delete "${session.title}"?`)) return;
    setError(null);
    try {
      await deleteAiSession(session.id);
      const remaining = sessions.filter((item) => item.id !== session.id);
      setSessions(remaining);
      if (activeSession?.id === session.id) {
        setActiveSession(null);
        if (remaining[0]) await selectSession(remaining[0].id);
      }
      showToast({
        title: "Session deleted",
        message: session.title,
        variant: "success",
      });
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "The session could not be deleted.",
      );
    }
  };

  const submitMessage = async () => {
    if (!activeSession || !message.trim() || isSending) return;
    const content = message.trim();
    if (content.length > 5000) {
      setError("Messages cannot exceed 5,000 characters.");
      return;
    }
    setIsSending(true);
    setError(null);
    setMessage("");
    try {
      const response = await sendAiMessage(activeSession.id, content);
      setActiveSession((current) =>
        current
          ? {
              ...current,
              messages: [
                ...current.messages,
                response.userMessage,
                response.assistantMessage,
              ],
              updatedAt: response.assistantMessage.createdAt,
            }
          : current,
      );
      setProvider(response.provider);
      setSessions((current) => {
        const updated = current.map((session) =>
          session.id === activeSession.id
            ? { ...session, updatedAt: response.assistantMessage.createdAt }
            : session,
        );
        return updated.sort((first, second) =>
          second.updatedAt.localeCompare(first.updatedAt),
        );
      });
    } catch (requestError) {
      setMessage(content);
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "Could not generate response. Please try again.",
      );
    } finally {
      setIsSending(false);
    }
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void submitMessage();
    }
  };

  return (
    <div className="grid min-h-[680px] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-card lg:grid-cols-[19rem_minmax(0,1fr)]">
      <aside className="border-b border-slate-200 lg:border-b-0 lg:border-r">
        <form className="grid gap-3 border-b border-slate-100 p-4" onSubmit={createSession}>
          <FormField
            label="New session title"
            maxLength={160}
            onChange={(event) => setNewTitle(event.target.value)}
            placeholder="OOP exam revision"
            value={newTitle}
          />
          <SelectField
            label="Session mode"
            onChange={(event) =>
              setNewMode(event.target.value as AiSessionMode)
            }
            options={aiSessionModeOptions}
            value={newMode}
          />
          <Button loading={isCreating} type="submit">
            <MessageSquarePlus className="size-4" />
            Create session
          </Button>
        </form>
        <div className="max-h-72 overflow-y-auto p-2 lg:max-h-[475px]">
          {isLoadingSessions ? (
            <div className="grid min-h-32 place-items-center">
              <LoadingSpinner label="Loading sessions" />
            </div>
          ) : sessions.length === 0 ? (
            <p className="p-4 text-center text-sm text-slate-500">
              Create your first study session.
            </p>
          ) : (
            sessions.map((session) => (
              <div
                className={cn(
                  "group flex items-start gap-2 rounded-xl p-2",
                  activeSession?.id === session.id
                    ? "bg-brand-50"
                    : "hover:bg-slate-50",
                )}
                key={session.id}
              >
                <button
                  className="min-w-0 flex-1 p-1 text-left"
                  onClick={() => void selectSession(session.id)}
                  type="button"
                >
                  <p className="truncate text-sm font-semibold text-slate-800">
                    {session.title}
                  </p>
                  <p className="mt-1 text-[11px] text-slate-400">
                    {aiModeLabel(session.mode)} · {formatAiDate(session.updatedAt)}
                  </p>
                </button>
                <button
                  aria-label={`Delete ${session.title}`}
                  className="rounded-lg p-2 text-slate-300 hover:bg-red-50 hover:text-red-600"
                  onClick={() => void removeSession(session)}
                  type="button"
                >
                  <Trash2 className="size-3.5" />
                </button>
              </div>
            ))
          )}
        </div>
      </aside>

      <section className="flex min-h-[580px] min-w-0 flex-col bg-slate-50/60">
        {error ? <ErrorMessage className="m-4 mb-0" message={error} /> : null}
        {isLoadingChat ? (
          <div className="grid flex-1 place-items-center">
            <LoadingSpinner label="Opening session" />
          </div>
        ) : activeSession ? (
          <>
            <header className="flex items-center gap-3 border-b border-slate-200 bg-white p-4">
              <span className="grid size-10 place-items-center rounded-xl bg-violet-100 text-violet-700">
                <Bot className="size-5" />
              </span>
              <div className="min-w-0">
                <h2 className="truncate font-semibold text-slate-950">
                  {activeSession.title}
                </h2>
                <p className="text-xs text-slate-400">
                  {aiModeLabel(activeSession.mode)}
                  {provider ? ` · ${provider}` : ""}
                </p>
              </div>
            </header>
            <div className="flex-1 space-y-5 overflow-y-auto p-4 sm:p-6">
              {activeSession.messages.length === 0 ? (
                <EmptyState
                  description="Ask a question to begin this study session."
                  icon={<Bot className="size-6" />}
                  title="Ready when you are"
                />
              ) : (
                activeSession.messages.map((item) => (
                  <AiMessageBubble key={item.id} message={item} />
                ))
              )}
              {isSending ? (
                <div className="mr-auto rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-500">
                  CampusOne AI is thinking…
                </div>
              ) : null}
              <div ref={messagesEndRef} />
            </div>
            <div className="border-t border-slate-200 bg-white p-3 sm:p-4">
              <div className="flex items-end gap-2 rounded-2xl border border-slate-200 bg-slate-50 p-2 focus-within:border-brand-400 focus-within:ring-4 focus-within:ring-brand-100">
                <textarea
                  aria-label="Message CampusOne AI"
                  className="max-h-32 min-h-11 flex-1 resize-none bg-transparent px-2 py-2 text-sm outline-none"
                  maxLength={5000}
                  onChange={(event) => setMessage(event.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Ask about your studies…"
                  value={message}
                />
                <Button
                  aria-label="Send message"
                  disabled={!message.trim()}
                  loading={isSending}
                  onClick={() => void submitMessage()}
                  size="icon"
                >
                  <Send className="size-4" />
                </Button>
              </div>
              <p className="mt-2 text-center text-[10px] text-slate-400">
                Enter sends · Shift+Enter adds a new line
              </p>
            </div>
          </>
        ) : (
          <div className="grid flex-1 place-items-center p-6">
            <EmptyState
              description="Create a session or select one from your history."
              icon={<Bot className="size-6" />}
              title="No active chat"
            />
          </div>
        )}
      </section>
    </div>
  );
}
