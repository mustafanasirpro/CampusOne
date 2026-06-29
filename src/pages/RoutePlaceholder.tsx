import { Blocks } from "lucide-react";

import { EmptyState, PageHeader } from "@/components/common";

export interface RoutePlaceholderProps {
  title: string;
}

export function RoutePlaceholder({ title }: RoutePlaceholderProps) {
  return (
    <div className="grid gap-6">
      <PageHeader title={title} />
      <EmptyState
        description="This route and its shared foundation are ready. Page-specific content is intentionally reserved for the next phase."
        icon={<Blocks className="size-6" />}
        title={`${title} page scaffold`}
      />
    </div>
  );
}

