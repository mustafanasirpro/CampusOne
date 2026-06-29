import { Bell } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";

import {
  Avatar,
  Button,
  SearchBar,
  useToast,
} from "@/components/common";
import { paths } from "@/routes/paths";

export function Navbar() {
  const [searchValue, setSearchValue] = useState("");
  const { showToast } = useToast();

  const handleSearch = (value: string) => {
    showToast({
      title: "Search ready",
      message: value
        ? `Global search will look for “${value}” when page content is connected.`
        : "Type something to search across CampusOne.",
    });
  };

  return (
    <header className="sticky top-0 z-30 hidden h-20 items-center border-b border-slate-200/80 bg-white/90 px-8 backdrop-blur lg:flex">
      <SearchBar
        className="max-w-xl"
        onSearch={handleSearch}
        onValueChange={setSearchValue}
        value={searchValue}
      />
      <div className="ml-auto flex items-center gap-2">
        <Button
          aria-label="Notifications"
          onClick={() =>
            showToast({
              message: "The notifications panel will be connected in the content phase.",
              title: "No new notifications",
            })
          }
          size="icon"
          variant="ghost"
        >
          <Bell className="size-5" />
        </Button>
        <Link
          aria-label="Open profile"
          className="ml-1 rounded-full"
          to={paths.profile}
        >
          <Avatar name="CampusOne Student" />
        </Link>
      </div>
    </header>
  );
}

