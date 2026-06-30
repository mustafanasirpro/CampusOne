import { Menu } from "lucide-react";

import { Button } from "@/components/common";
import { CampusOneLogo } from "@/components/layout/CampusOneLogo";
import { NotificationMenu } from "@/components/layout/NotificationMenu";
import { ProfileMenu } from "@/components/layout/ProfileMenu";
import { paths } from "@/routes/paths";

export interface MobileNavbarProps {
  onMenuClick: () => void;
}

export function MobileNavbar({ onMenuClick }: MobileNavbarProps) {
  return (
    <header className="sticky top-0 z-30 flex h-16 items-center border-b border-slate-200/80 bg-white/95 px-4 backdrop-blur lg:hidden">
      <Button
        aria-label="Open navigation"
        className="-ml-2"
        onClick={onMenuClick}
        size="icon"
        variant="ghost"
      >
        <Menu className="size-5" />
      </Button>
      <CampusOneLogo className="ml-1" compact to={paths.dashboard} />
      <span className="ml-2 text-base font-bold tracking-tight text-slate-950">
        Campus<span className="text-brand-600">One</span>
      </span>
      <div className="ml-auto flex items-center gap-1">
        <NotificationMenu />
        <ProfileMenu compact />
      </div>
    </header>
  );
}
