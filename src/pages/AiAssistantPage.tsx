import { Bot, ShieldCheck, Sparkles } from "lucide-react";
import { useState } from "react";

import {
  AiChatWorkspace,
  AiUsagePanel,
  AiWorkspaceTabs,
  ExplainConceptPanel,
  GeneratedItemsPanel,
  StudyPlanPanel,
  TextGeneratorPanel,
  type AiWorkspaceTab,
} from "@/components/ai";
import { Badge, Card, CardContent, PageHeader } from "@/components/common";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function AiAssistantPage() {
  const [activeTab, setActiveTab] = useState<AiWorkspaceTab>("chat");

  useDocumentTitle("AI Study Assistant · CampusOne");

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        actions={
          <Badge className="gap-1.5" variant="success">
            <ShieldCheck className="size-3.5" />
            CampusOne backend
          </Badge>
        }
        description="Chat, explain concepts, generate revision material, and build practical study plans."
        eyebrow="Student productivity"
        title="AI Study Assistant"
      />

      <Card className="overflow-hidden border-0 bg-gradient-to-r from-slate-950 via-brand-950 to-violet-950 text-white shadow-xl">
        <CardContent className="flex flex-col gap-5 p-6 sm:flex-row sm:items-center">
          <span className="grid size-14 shrink-0 place-items-center rounded-2xl bg-white/10 text-brand-200 ring-1 ring-white/10">
            <Bot className="size-7" />
          </span>
          <div className="flex-1">
            <h2 className="flex items-center gap-2 text-lg font-semibold">
              Local study intelligence
              <Sparkles className="size-4 text-amber-300" />
            </h2>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-300">
              Every result is generated through the authenticated CampusOne AI
              backend. No external provider key or direct browser AI call is
              required.
            </p>
          </div>
        </CardContent>
      </Card>

      <AiWorkspaceTabs activeTab={activeTab} onChange={setActiveTab} />

      <section aria-live="polite">
        {activeTab === "chat" ? <AiChatWorkspace /> : null}
        {activeTab === "explain" ? <ExplainConceptPanel /> : null}
        {activeTab === "summary" ? (
          <TextGeneratorPanel kind="summary" />
        ) : null}
        {activeTab === "flashcards" ? (
          <TextGeneratorPanel kind="flashcards" />
        ) : null}
        {activeTab === "quiz" ? <TextGeneratorPanel kind="quiz" /> : null}
        {activeTab === "study-plan" ? <StudyPlanPanel /> : null}
        {activeTab === "generated" ? <GeneratedItemsPanel /> : null}
        {activeTab === "usage" ? <AiUsagePanel /> : null}
      </section>
    </div>
  );
}
