import { ShieldCheck, Zap } from "lucide-react";

import { TUTOR_PROVIDER } from "@/api/tutorClient";
import { TutorConsole } from "@/components/ai/tutor";
import { Badge, PageHeader } from "@/components/common";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function TutorPage() {
  useDocumentTitle("Live Tutor · CampusOne");

  const isGroq = TUTOR_PROVIDER === "groq";

  return (
    <div className="grid gap-4 pb-4">
      <PageHeader
        actions={
          <Badge className="gap-1.5" variant="success">
            {isGroq ? (
              <Zap className="size-3.5" />
            ) : (
              <ShieldCheck className="size-3.5" />
            )}
            {isGroq ? "Fast & free" : "Runs offline on your device"}
          </Badge>
        }
        description="Pick a topic and your tutor teaches it live on the whiteboard. Confused? Raise your hand any time — it stops, answers, and picks up where it left off."
        eyebrow="AI Study Assistant"
        title="Live Whiteboard Tutor"
      />

      <TutorConsole />
    </div>
  );
}
