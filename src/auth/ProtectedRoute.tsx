import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useAuth } from "@/auth/useAuth";
import { LoadingSpinner } from "@/components/common";
import { paths } from "@/routes/paths";

export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="grid min-h-screen place-items-center bg-slate-50">
        <LoadingSpinner label="Checking your CampusOne session" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <Navigate
        replace
        state={{
          from: `${location.pathname}${location.search}${location.hash}`,
        }}
        to={paths.login}
      />
    );
  }

  return <Outlet />;
}

export function GuestRoute() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? (
    <Navigate replace to={paths.dashboard} />
  ) : (
    <Outlet />
  );
}
