import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { SearchBar } from "@/components/common";
import { NotificationMenu } from "@/components/layout/NotificationMenu";
import { ProfileMenu } from "@/components/layout/ProfileMenu";
import { primaryNavigation, secondaryNavigation } from "@/data/navigation";
import { paths } from "@/routes/paths";

export function Navbar() {
  const [searchValue, setSearchValue] = useState("");
  const location = useLocation();
  const navigate = useNavigate();
  const activeItem = [...primaryNavigation, ...secondaryNavigation].find(
    (item) => location.pathname.startsWith(item.path),
  );

  const handleSearch = (value: string) => {
    const query = value.trim();
    navigate(
      query
        ? `${paths.search}?q=${encodeURIComponent(query)}`
        : paths.search,
    );
  };

  return (
    <header className="sticky top-0 z-30 hidden h-20 items-center border-b border-slate-200/80 bg-white/90 px-8 backdrop-blur lg:flex">
      <div className="mr-8 min-w-36">
        <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-slate-400">
          CampusOne
        </p>
        <p className="mt-0.5 text-sm font-semibold text-slate-900">
          {activeItem?.label ?? "Student portal"}
        </p>
      </div>
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
