import { Search } from "lucide-react";

import { ModulePlaceholderPage } from "@/pages/ModulePlaceholderPage";

export function SearchPage() {
  return (
    <ModulePlaceholderPage
      description="Search across notes, discussions, marketplace listings, events, and internships."
      icon={Search}
      title="Global Search"
    />
  );
}
