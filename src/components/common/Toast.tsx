import { AlertCircle, CheckCircle2, Info, X } from "lucide-react";
import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { cn } from "@/utils/cn";

type ToastVariant = "success" | "info" | "error";

interface ToastItem {
  id: string;
  message: string;
  title?: string;
  variant: ToastVariant;
}

interface ShowToastOptions {
  duration?: number;
  message: string;
  title?: string;
  variant?: ToastVariant;
}

interface ToastContextValue {
  showToast: (options: ShowToastOptions) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const toastIcons: Record<ToastVariant, ReactNode> = {
  success: <CheckCircle2 className="size-5 text-emerald-600" />,
  info: <Info className="size-5 text-brand-600" />,
  error: <AlertCircle className="size-5 text-red-600" />,
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const showToast = useCallback(
    ({
      duration = 3500,
      message,
      title,
      variant = "info",
    }: ShowToastOptions) => {
      const id = crypto.randomUUID();
      setToasts((current) => [...current, { id, message, title, variant }]);
      window.setTimeout(() => removeToast(id), duration);
    },
    [removeToast],
  );

  const contextValue = useMemo(() => ({ showToast }), [showToast]);

  return (
    <ToastContext.Provider value={contextValue}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-[calc(100%-2rem)] max-w-sm flex-col gap-3"
      >
        {toasts.map((toast) => (
          <div
            className="pointer-events-auto flex gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-xl"
            key={toast.id}
            role="status"
          >
            <span className="mt-0.5">{toastIcons[toast.variant]}</span>
            <div className="min-w-0 flex-1">
              {toast.title ? (
                <p className="text-sm font-semibold text-slate-900">
                  {toast.title}
                </p>
              ) : null}
              <p
                className={cn(
                  "text-sm text-slate-600",
                  toast.title && "mt-0.5",
                )}
              >
                {toast.message}
              </p>
            </div>
            <button
              aria-label="Dismiss notification"
              className="self-start rounded-md p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              onClick={() => removeToast(toast.id)}
              type="button"
            >
              <X className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

// The provider and its companion hook intentionally share this small module.
// eslint-disable-next-line react-refresh/only-export-components
export function useToast() {
  const context = useContext(ToastContext);

  if (!context) {
    throw new Error("useToast must be used within a ToastProvider.");
  }

  return context;
}
