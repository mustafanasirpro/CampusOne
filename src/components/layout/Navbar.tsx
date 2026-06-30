import { useState } from "react";

import { SearchBar, useToast } from "@/components/common";
import { NotificationMenu } from "@/components/layout/NotificationMenu";
import { ProfileMenu } from "@/components/layout/ProfileMenu";

export function Navbar() {
  const [searchValue, setSearchValue] = useState("");
  const { showToast } = useToast();

  const handleSearch = (value: string) => {
    showToast({
      title: "Search ready",
      message: value
        ? `Searching CampusOne for “${value}” in this frontend demo.`
        : "Type something to search across CampusOne.",
    });
  };

  return (
    <header className="sticky top-0 z-30 hidden h-20 items-center border-b border-slate-200/80 bg-white/90 px-8 backdrop-blur lg:flex">
      <SearchBar
        className="max-w-xl"
        onSearch={handleSearch}
        onValueChange={setSearchValue}
        placeholder="Search notes, discussions, events..."
        value={searchValue}
      />
      <div className="ml-auto flex items-center gap-2">
        <NotificationMenu />
        <ProfileMenu />
      </div>
    </header>
  );
}
