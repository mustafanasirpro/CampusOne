import { X } from "lucide-react";
import { useEffect, useState } from "react";
import { Outlet } from "react-router-dom";

import { Button } from "@/components/common";
import { MobileNavbar } from "@/components/layout/MobileNavbar";
import { Navbar } from "@/components/layout/Navbar";
import { Sidebar } from "@/components/layout/Sidebar";
import { cn } from "@/utils/cn";

export function AppLayout() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    document.body.style.overflow = isMobileMenuOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [isMobileMenuOpen]);

  return (
    <div className="min-h-screen bg-slate-50">
      <a
        className="fixed left-4 top-3 z-[70] -translate-y-20 rounded-xl bg-slate-950 px-4 py-2 text-sm font-semibold text-white shadow-xl transition focus:translate-y-0"
        href="#main-content"
      >
        Skip to main content
      </a>
      <Sidebar className="fixed inset-y-0 left-0 z-40 hidden lg:flex" />

      <div
        aria-hidden={!isMobileMenuOpen}
        className={cn(
          "fixed inset-0 z-50 lg:hidden",
          isMobileMenuOpen ? "pointer-events-auto" : "pointer-events-none",
        )}
      >
        <button
          aria-label="Close navigation"
          className={cn(
            "absolute inset-0 bg-slate-950/40 transition-opacity",
            isMobileMenuOpen ? "opacity-100" : "opacity-0",
          )}
          onClick={() => setIsMobileMenuOpen(false)}
          tabIndex={isMobileMenuOpen ? 0 : -1}
          type="button"
        />
        <div
          className={cn(
            "absolute inset-y-0 left-0 w-72 max-w-[85vw] transform transition-transform duration-200 ease-out",
            isMobileMenuOpen ? "translate-x-0" : "-translate-x-full",
          )}
        >
          <Sidebar onNavigate={() => setIsMobileMenuOpen(false)} />
          <Button
            aria-label="Close navigation"
            className="absolute right-3 top-5"
            onClick={() => setIsMobileMenuOpen(false)}
            size="icon"
            variant="ghost"
          >
            <X className="size-5" />
          </Button>
        </div>
      </div>

      <div className="lg:pl-72">
        <Navbar />
        <MobileNavbar onMenuClick={() => setIsMobileMenuOpen(true)} />
        <main
          className="mx-auto w-full max-w-[1600px] p-4 sm:p-6 lg:p-8"
          id="main-content"
        >
          <Outlet />
        </main>
      </div>
    </div>
  );
}
