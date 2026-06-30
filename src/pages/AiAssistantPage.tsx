import {
  Bot,
  CheckCircle2,
  ChevronRight,
  Clock3,
  History,
  Menu,
  Mic,
  Paperclip,
  Plus,
  Send,
  Sparkles,
  Star,
  WandSparkles,
  X,
} from "lucide-react";
import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent,
} from "react";

import { AIResponseCard } from "@/components/common/AIResponseCard";
import { ChatBubble } from "@/components/common/ChatBubble";
import { ConversationList } from "@/components/common/ConversationList";
import { PromptCard } from "@/components/common/PromptCard";
import { TypingIndicator } from "@/components/common/TypingIndicator";
import {
  Button,
  Card,
  CardContent,
  Modal,
  PageHeader,
  SectionTitle,
  useToast,
} from "@/components/common";
import {
  aiFeatures,
  genericAIResponse,
  initialAIConversations,
  promptSuggestions,
  studyHistory,
  studyTools,
} from "@/data/aiAssistant";
import type {
  AIContentBlock,
  AIConversation,
  ChatMessage,
  PromptSuggestion,
  StudyTool,
} from "@/types/ai";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type Reaction = "like" | "dislike";

const featureTones = {
  brand: "bg-brand-50 text-brand-600 group-hover:bg-brand-600",
  emerald: "bg-emerald-50 text-emerald-600 group-hover:bg-emerald-600",
  amber: "bg-amber-50 text-amber-600 group-hover:bg-amber-500",
  sky: "bg-sky-50 text-sky-600 group-hover:bg-sky-600",
};

function createMessageId(prefix: string) {
  return `${prefix}-${crypto.randomUUID()}`;
}

function getCurrentTime() {
  return new Intl.DateTimeFormat("en-PK", {
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date());
}

function blocksToText(blocks: AIContentBlock[] = []) {
  return blocks
    .map((block) => {
      if ("text" in block) return block.text;
      if (block.type === "list") return block.items.join("\n");
      if (block.type === "code") return block.code;
      if (block.type === "table") {
        return [block.headers, ...block.rows]
          .map((row) => row.join(" | "))
          .join("\n");
      }
      return [block.expression, block.explanation].filter(Boolean).join("\n");
    })
    .join("\n\n");
}

export function AiAssistantPage() {
  const [conversations, setConversations] = useState<AIConversation[]>(
    initialAIConversations,
  );
  const [activeChatId, setActiveChatId] = useState<string | null>(
    initialAIConversations[0]?.id ?? null,
  );
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [isChatMenuOpen, setIsChatMenuOpen] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const [reactions, setReactions] = useState<
    Record<string, Reaction | undefined>
  >({});
  const [copiedMessageId, setCopiedMessageId] = useState<string | null>(null);
  const [selectedTool, setSelectedTool] = useState<StudyTool | null>(null);
  const [toolInput, setToolInput] = useState("");
  const [toolResultShown, setToolResultShown] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { showToast } = useToast();

  useDocumentTitle("AI Study Assistant · CampusOne");

  const activeConversation = useMemo(
    () =>
      conversations.find((conversation) => conversation.id === activeChatId) ??
      null,
    [activeChatId, conversations],
  );

  const favoriteConversations = useMemo(
    () => conversations.filter((conversation) => conversation.isFavorite),
    [conversations],
  );

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "nearest",
    });
  }, [activeConversation?.messages.length, isTyping]);

  const startNewChat = () => {
    setActiveChatId(null);
    setInput("");
    setIsTyping(false);
    setIsChatMenuOpen(false);
  };

  const selectConversation = (id: string) => {
    setActiveChatId(id);
    setIsChatMenuOpen(false);
  };

  const toggleFavorite = (conversation: AIConversation) => {
    setConversations((current) =>
      current.map((item) =>
        item.id === conversation.id
          ? { ...item, isFavorite: !item.isFavorite }
          : item,
      ),
    );
    showToast({
      title: conversation.isFavorite
        ? "Removed from favorites"
        : "Chat saved",
      message: conversation.title,
      variant: conversation.isFavorite ? "info" : "success",
    });
  };

  const appendExchange = (
    prompt: string,
    response: AIContentBlock[] = genericAIResponse,
  ) => {
    if (!prompt.trim() || isTyping) return;

    const chatId = activeChatId ?? `chat-${crypto.randomUUID()}`;
    const timestamp = getCurrentTime();
    const userMessage: ChatMessage = {
      id: createMessageId("user"),
      role: "user",
      text: prompt.trim(),
      timestamp,
    };

    setConversations((current) => {
      const existing = current.find((conversation) => conversation.id === chatId);

      if (existing) {
        return current.map((conversation) =>
          conversation.id === chatId
            ? {
                ...conversation,
                messages: [...conversation.messages, userMessage],
                updatedAt: "Just now",
              }
            : conversation,
        );
      }

      const title =
        prompt.trim().length > 34
          ? `${prompt.trim().slice(0, 34)}…`
          : prompt.trim();

      return [
        {
          id: chatId,
          title,
          category: "Study session",
          updatedAt: "Just now",
          isFavorite: false,
          messages: [userMessage],
        },
        ...current,
      ];
    });

    setActiveChatId(chatId);
    setInput("");
    setIsTyping(true);

    window.setTimeout(() => {
      const assistantMessage: ChatMessage = {
        id: createMessageId("assistant"),
        role: "assistant",
        blocks: response,
        timestamp: getCurrentTime(),
      };

      setConversations((current) =>
        current.map((conversation) =>
          conversation.id === chatId
            ? {
                ...conversation,
                messages: [...conversation.messages, assistantMessage],
                updatedAt: "Just now",
              }
            : conversation,
        ),
      );
      setIsTyping(false);
    }, 850);
  };

  const handlePromptSuggestion = (suggestion: PromptSuggestion) => {
    appendExchange(suggestion.prompt, suggestion.response);
  };

  const handleCopy = (message: ChatMessage) => {
    const text = blocksToText(message.blocks);
    if (navigator.clipboard) {
      void navigator.clipboard.writeText(text);
    }
    setCopiedMessageId(message.id);
    showToast({
      title: "Response copied",
      message: "The explanation is ready to paste into your notes.",
      variant: "success",
    });
    window.setTimeout(() => setCopiedMessageId(null), 1800);
  };

  const handleReaction = (messageId: string, reaction: Reaction) => {
    const nextReaction =
      reactions[messageId] === reaction ? undefined : reaction;
    setReactions((current) => ({
      ...current,
      [messageId]: nextReaction,
    }));
    showToast({
      title: nextReaction ? "Feedback saved" : "Feedback cleared",
      message:
        nextReaction === "like"
          ? "Glad this explanation helped."
          : nextReaction === "dislike"
            ? "Thanks — this helps improve future answers."
            : "Your response rating was removed.",
    });
  };

  const handleRegenerate = () => {
    if (!activeChatId || isTyping) return;

    const chatId = activeChatId;
    setIsTyping(true);
    showToast({
      title: "Regenerating response",
      message: "Trying a clearer way to explain the concept.",
    });

    window.setTimeout(() => {
      const assistantMessage: ChatMessage = {
        id: createMessageId("assistant"),
        role: "assistant",
        blocks: [
          {
            type: "heading",
            text: "Here is another way to think about it",
          },
          {
            type: "paragraph",
            text: "Break the topic into its purpose, its main moving parts, and one small example. Once those are clear, the formal definition becomes much easier to remember.",
          },
          {
            type: "list",
            items: [
              "State what problem the concept solves.",
              "Name the pieces involved.",
              "Trace one simple example from start to finish.",
              "Check your understanding with a new example.",
            ],
          },
        ],
        timestamp: getCurrentTime(),
      };
      setConversations((current) =>
        current.map((conversation) =>
          conversation.id === chatId
            ? {
                ...conversation,
                messages: [...conversation.messages, assistantMessage],
                updatedAt: "Just now",
              }
            : conversation,
        ),
      );
      setIsTyping(false);
    }, 800);
  };

  const handleShare = () => {
    showToast({
      title: "Share link created",
      message: "A private dummy conversation link was copied.",
      variant: "success",
    });
  };

  const handleFileChange = (file: File | undefined) => {
    if (!file) return;
    showToast({
      title: "Attachment ready",
      message: `${file.name} is attached for this demo. It will not be uploaded.`,
      variant: "success",
    });
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const toggleVoiceInput = () => {
    setIsListening((current) => !current);
    showToast({
      title: isListening ? "Voice input stopped" : "Listening…",
      message: isListening
        ? "No audio was stored."
        : "Voice mode is a frontend preview in this milestone.",
    });
  };

  const handleInputKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      appendExchange(input);
    }
  };

  const openStudyTool = (tool: StudyTool) => {
    setSelectedTool(tool);
    setToolInput("");
    setToolResultShown(false);
  };

  const closeStudyTool = () => {
    setSelectedTool(null);
    setToolInput("");
    setToolResultShown(false);
  };

  const runStudyTool = () => {
    if (!selectedTool) return;
    if (!toolInput.trim()) {
      showToast({
        title: "Add a little context",
        message: `Enter a topic before you ${selectedTool.actionLabel.toLowerCase()}.`,
        variant: "error",
      });
      return;
    }

    setToolResultShown(true);
    showToast({
      title: `${selectedTool.title} preview ready`,
      message: "A sample result has been generated locally.",
      variant: "success",
    });
  };

  const sidePanel = (
    <div className="flex h-full flex-col bg-white">
      <div className="border-b border-slate-100 p-4">
        <Button className="w-full" onClick={startNewChat}>
          <Plus className="size-4" />
          New chat
        </Button>
      </div>
      <div className="flex-1 space-y-6 overflow-y-auto p-3">
        <ConversationList
          activeId={activeChatId}
          conversations={conversations.slice(0, 6)}
          onSelect={selectConversation}
          onToggleFavorite={toggleFavorite}
          title="Recent chats"
        />
        <ConversationList
          activeId={activeChatId}
          conversations={favoriteConversations}
          emptyMessage="Star a conversation to keep it close."
          onSelect={selectConversation}
          onToggleFavorite={toggleFavorite}
          title="Favorite chats"
        />
        <section>
          <h3 className="px-2 text-[11px] font-bold uppercase tracking-[0.14em] text-slate-400">
            Study history
          </h3>
          <div className="mt-2 grid gap-1">
            {studyHistory.map((item) => (
              <button
                className="flex items-start gap-2.5 rounded-xl px-2.5 py-2.5 text-left transition hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-brand-500"
                key={item.id}
                onClick={() =>
                  showToast({
                    title: item.title,
                    message: `${item.detail}. History is a demo in this milestone.`,
                  })
                }
                type="button"
              >
                <History className="mt-0.5 size-4 shrink-0 text-slate-400" />
                <span>
                  <span className="block text-xs font-semibold text-slate-700">
                    {item.title}
                  </span>
                  <span className="mt-0.5 block text-[10px] text-slate-400">
                    {item.detail}
                  </span>
                </span>
              </button>
            ))}
          </div>
        </section>
      </div>
      <div className="border-t border-slate-100 p-4">
        <div className="rounded-2xl bg-gradient-to-br from-slate-950 to-slate-800 p-4 text-white">
          <div className="flex items-center gap-2">
            <Sparkles className="size-4 text-brand-300" />
            <p className="text-xs font-semibold">Study smarter</p>
          </div>
          <p className="mt-2 text-[11px] leading-5 text-slate-400">
            Ask for an analogy, a quiz, or a step-by-step explanation.
          </p>
        </div>
      </div>
    </div>
  );

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        actions={
          <Button onClick={startNewChat}>
            <Plus className="size-4" />
            New chat
          </Button>
        }
        description="Your personal AI tutor for university learning."
        eyebrow="CampusOne intelligence"
        title="AI Study Assistant"
      />

      <section aria-labelledby="ai-features">
        <h2 className="sr-only" id="ai-features">
          AI study features
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {aiFeatures.map((feature) => {
            const Icon = feature.icon;

            return (
              <Card
                className="group transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl"
                key={feature.title}
              >
                <CardContent className="flex items-start gap-3 p-4 sm:p-5">
                  <span
                    className={cn(
                      "grid size-11 shrink-0 place-items-center rounded-2xl transition-colors group-hover:text-white",
                      featureTones[feature.tone],
                    )}
                  >
                    <Icon className="size-5" />
                  </span>
                  <div>
                    <h3 className="text-sm font-semibold text-slate-900">
                      {feature.title}
                    </h3>
                    <p className="mt-1 text-xs leading-5 text-slate-500">
                      {feature.description}
                    </p>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      <Card className="relative h-[720px] min-h-[620px] overflow-hidden border-slate-200 shadow-xl shadow-slate-900/5">
        <div className="grid h-full lg:grid-cols-[280px_minmax(0,1fr)]">
          <aside className="hidden border-r border-slate-200 lg:block">
            {sidePanel}
          </aside>

          <div className="flex min-w-0 flex-col bg-slate-50/70">
            <div className="flex h-[68px] shrink-0 items-center justify-between gap-3 border-b border-slate-200 bg-white px-3 sm:px-5">
              <div className="flex min-w-0 items-center gap-3">
                <Button
                  aria-label="Open chat history"
                  className="lg:hidden"
                  onClick={() => setIsChatMenuOpen(true)}
                  size="icon"
                  variant="ghost"
                >
                  <Menu className="size-5" />
                </Button>
                <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-brand-600 to-violet-600 text-white shadow-md shadow-brand-600/20">
                  <Bot className="size-4" />
                </span>
                <div className="min-w-0">
                  <h2 className="truncate text-sm font-semibold text-slate-950">
                    {activeConversation?.title ?? "New study session"}
                  </h2>
                  <p className="mt-0.5 flex items-center gap-1.5 text-[11px] text-slate-500">
                    <span className="size-1.5 rounded-full bg-emerald-500" />
                    {activeConversation?.category ?? "Ready when you are"}
                  </p>
                </div>
              </div>
              {activeConversation ? (
                <Button
                  aria-label={
                    activeConversation.isFavorite
                      ? "Remove chat from favorites"
                      : "Add chat to favorites"
                  }
                  onClick={() => toggleFavorite(activeConversation)}
                  size="icon"
                  variant="ghost"
                >
                  <Star
                    className={cn(
                      "size-4.5",
                      activeConversation.isFavorite &&
                        "fill-amber-400 text-amber-500",
                    )}
                  />
                </Button>
              ) : null}
            </div>

            <div className="flex-1 overflow-y-auto overscroll-contain px-3 py-5 sm:px-6">
              {activeConversation ? (
                <div className="mx-auto grid max-w-4xl gap-6">
                  <div className="flex items-center gap-3">
                    <span className="h-px flex-1 bg-slate-200" />
                    <span className="text-[10px] font-bold uppercase tracking-[0.14em] text-slate-400">
                      Study session
                    </span>
                    <span className="h-px flex-1 bg-slate-200" />
                  </div>
                  {activeConversation.messages.map((message) =>
                    message.role === "user" ? (
                      <ChatBubble key={message.id} message={message} />
                    ) : (
                      <AIResponseCard
                        copied={copiedMessageId === message.id}
                        key={message.id}
                        message={message}
                        onCopy={() => handleCopy(message)}
                        onDislike={() =>
                          handleReaction(message.id, "dislike")
                        }
                        onLike={() => handleReaction(message.id, "like")}
                        onRegenerate={handleRegenerate}
                        onShare={handleShare}
                        reaction={reactions[message.id] ?? null}
                      />
                    ),
                  )}
                  {isTyping ? <TypingIndicator /> : null}
                  <div ref={messagesEndRef} />
                </div>
              ) : (
                <div className="mx-auto flex min-h-full max-w-4xl flex-col items-center justify-center py-8 text-center">
                  <div className="relative">
                    <span className="absolute inset-0 animate-pulse rounded-3xl bg-brand-300/40 blur-xl" />
                    <span className="relative grid size-16 place-items-center rounded-3xl bg-gradient-to-br from-brand-600 to-violet-600 text-white shadow-xl shadow-brand-600/25">
                      <WandSparkles className="size-7" />
                    </span>
                  </div>
                  <h2 className="mt-5 text-xl font-bold tracking-tight text-slate-950 sm:text-2xl">
                    What are we learning today?
                  </h2>
                  <p className="mt-2 max-w-lg text-sm leading-6 text-slate-500">
                    Choose a starting point or ask about any course, assignment,
                    exam, or programming problem.
                  </p>
                  <div className="mt-6 grid w-full gap-3 text-left sm:grid-cols-2">
                    {promptSuggestions.map((suggestion) => (
                      <PromptCard
                        key={suggestion.id}
                        onClick={() => handlePromptSuggestion(suggestion)}
                        suggestion={suggestion}
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="shrink-0 border-t border-slate-200 bg-white p-3 sm:p-4">
              {activeConversation ? (
                <div className="mx-auto mb-3 flex max-w-4xl gap-2 overflow-x-auto pb-1">
                  {promptSuggestions.slice(0, 4).map((suggestion) => (
                    <button
                      className="shrink-0 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-[11px] font-semibold text-slate-600 transition hover:border-brand-200 hover:bg-brand-50 hover:text-brand-700"
                      key={suggestion.id}
                      onClick={() => handlePromptSuggestion(suggestion)}
                      type="button"
                    >
                      {suggestion.title}
                    </button>
                  ))}
                </div>
              ) : null}
              <div className="mx-auto flex max-w-4xl items-end gap-2 rounded-2xl border border-slate-200 bg-slate-50 p-2 shadow-sm transition focus-within:border-brand-300 focus-within:bg-white focus-within:ring-4 focus-within:ring-brand-100/60">
                <input
                  accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg"
                  className="hidden"
                  onChange={(event) =>
                    handleFileChange(event.target.files?.[0])
                  }
                  ref={fileInputRef}
                  type="file"
                />
                <Button
                  aria-label="Attach study material"
                  className="mb-0.5"
                  onClick={() => fileInputRef.current?.click()}
                  size="icon"
                  variant="ghost"
                >
                  <Paperclip className="size-4.5" />
                </Button>
                <textarea
                  aria-label="Study question"
                  className="max-h-28 min-h-10 flex-1 resize-none bg-transparent px-1 py-2 text-sm leading-5 text-slate-800 outline-none placeholder:text-slate-400"
                  onChange={(event) => setInput(event.target.value)}
                  onKeyDown={handleInputKeyDown}
                  placeholder="Ask anything about your studies..."
                  rows={1}
                  value={input}
                />
                <Button
                  aria-label={isListening ? "Stop voice input" : "Start voice input"}
                  className={cn(
                    "mb-0.5",
                    isListening && "bg-rose-50 text-rose-600 hover:bg-rose-100",
                  )}
                  onClick={toggleVoiceInput}
                  size="icon"
                  variant="ghost"
                >
                  <Mic className={cn("size-4.5", isListening && "animate-pulse")} />
                </Button>
                <Button
                  aria-label="Send message"
                  className="mb-0.5"
                  disabled={!input.trim() || isTyping}
                  onClick={() => appendExchange(input)}
                  size="icon"
                >
                  <Send className="size-4" />
                </Button>
              </div>
              <p className="mx-auto mt-2 max-w-4xl text-center text-[10px] text-slate-400">
                CampusOne AI is a frontend demo. Always verify important academic
                information.
              </p>
            </div>
          </div>
        </div>

        <div
          aria-hidden={!isChatMenuOpen}
          className={cn(
            "absolute inset-0 z-30 lg:hidden",
            isChatMenuOpen ? "pointer-events-auto" : "pointer-events-none",
          )}
        >
          <button
            aria-label="Close chat history"
            className={cn(
              "absolute inset-0 bg-slate-950/40 transition-opacity",
              isChatMenuOpen ? "opacity-100" : "opacity-0",
            )}
            onClick={() => setIsChatMenuOpen(false)}
            tabIndex={isChatMenuOpen ? 0 : -1}
            type="button"
          />
          <aside
            className={cn(
              "absolute inset-y-0 left-0 w-[290px] max-w-[86vw] border-r border-slate-200 bg-white shadow-2xl transition-transform duration-200",
              isChatMenuOpen ? "translate-x-0" : "-translate-x-full",
            )}
          >
            {sidePanel}
            <Button
              aria-label="Close chat history"
              className="absolute right-2 top-2"
              onClick={() => setIsChatMenuOpen(false)}
              size="icon"
              variant="ghost"
            >
              <X className="size-4.5" />
            </Button>
          </aside>
        </div>
      </Card>

      <section>
        <SectionTitle
          description="Open a focused workspace for a common study task."
          title="Study tools"
        />
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {studyTools.map((tool) => {
            const Icon = tool.icon;

            return (
              <button
                className="group rounded-2xl text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
                key={tool.id}
                onClick={() => openStudyTool(tool)}
                type="button"
              >
                <Card className="h-full transition duration-200 group-hover:-translate-y-1 group-hover:border-brand-200 group-hover:shadow-xl">
                  <CardContent className="flex h-full items-start gap-4 p-5">
                    <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-brand-50 text-brand-600 transition-colors group-hover:bg-brand-600 group-hover:text-white">
                      <Icon className="size-5" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between gap-2">
                        <h3 className="font-semibold text-slate-900">
                          {tool.title}
                        </h3>
                        <ChevronRight className="size-4 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-brand-600" />
                      </div>
                      <p className="mt-1.5 text-sm leading-6 text-slate-500">
                        {tool.description}
                      </p>
                    </div>
                  </CardContent>
                </Card>
              </button>
            );
          })}
        </div>
      </section>

      <Card className="overflow-hidden border-brand-200 bg-gradient-to-r from-brand-50 via-white to-violet-50">
        <CardContent className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center">
          <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-slate-950 text-brand-200 shadow-lg shadow-slate-900/15">
            <Clock3 className="size-5" />
          </span>
          <div className="flex-1">
            <h2 className="font-semibold text-slate-950">
              A study partner, not a shortcut
            </h2>
            <p className="mt-1 text-sm leading-6 text-slate-500">
              Use explanations to strengthen your reasoning, then test yourself
              without looking at the answer.
            </p>
          </div>
          <Button onClick={startNewChat} variant="outline">
            Start a focused session
            <ChevronRight className="size-4" />
          </Button>
        </CardContent>
      </Card>

      <Modal
        description={selectedTool?.description}
        footer={
          selectedTool ? (
            <>
              <Button onClick={closeStudyTool} variant="outline">
                Cancel
              </Button>
              <Button onClick={runStudyTool}>
                <WandSparkles className="size-4" />
                {selectedTool.actionLabel}
              </Button>
            </>
          ) : null
        }
        isOpen={Boolean(selectedTool)}
        onClose={closeStudyTool}
        size="lg"
        title={selectedTool?.title ?? "Study tool"}
      >
        {selectedTool ? (
          <div className="grid gap-5">
            <label className="grid gap-2 text-sm font-semibold text-slate-700">
              {selectedTool.inputLabel}
              <textarea
                className="min-h-32 resize-y rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm font-normal leading-6 text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
                onChange={(event) => {
                  setToolInput(event.target.value);
                  setToolResultShown(false);
                }}
                placeholder={selectedTool.inputPlaceholder}
                value={toolInput}
              />
            </label>

            <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4">
              <div className="flex items-center gap-2">
                <Paperclip className="size-4 text-slate-400" />
                <p className="text-sm font-semibold text-slate-700">
                  Add course material
                </p>
              </div>
              <p className="mt-1 text-xs leading-5 text-slate-500">
                File attachments are represented in the UI only and are never
                uploaded.
              </p>
              <Button
                className="mt-3"
                onClick={() =>
                  showToast({
                    title: "Attachment mode opened",
                    message: "File processing will be available with a future backend.",
                  })
                }
                size="sm"
                variant="outline"
              >
                Choose a file
              </Button>
            </div>

            {toolResultShown ? (
              <div
                aria-live="polite"
                className="rounded-2xl border border-emerald-200 bg-emerald-50/70 p-5"
              >
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="size-5 text-emerald-600" />
                  <h3 className="font-semibold text-emerald-950">
                    Sample result
                  </h3>
                </div>
                <ul className="mt-3 grid gap-2 text-sm leading-6 text-emerald-800">
                  {selectedTool.sampleResult.map((result) => (
                    <li className="flex gap-2" key={result}>
                      <span className="mt-2 size-1.5 shrink-0 rounded-full bg-emerald-500" />
                      {result}
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
          </div>
        ) : null}
      </Modal>
    </div>
  );
}
