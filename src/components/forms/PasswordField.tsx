import { Eye, EyeOff } from "lucide-react";
import { useState } from "react";

import {
  FormField,
  type FormFieldProps,
} from "@/components/forms/FormField";

export type PasswordFieldProps = Omit<FormFieldProps, "trailing" | "type">;

export function PasswordField(props: PasswordFieldProps) {
  const [isVisible, setIsVisible] = useState(false);

  return (
    <FormField
      {...props}
      trailing={
        <button
          aria-label={isVisible ? "Hide password" : "Show password"}
          className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
          onClick={() => setIsVisible((current) => !current)}
          type="button"
        >
          {isVisible ? (
            <EyeOff className="size-4" />
          ) : (
            <Eye className="size-4" />
          )}
        </button>
      }
      type={isVisible ? "text" : "password"}
    />
  );
}

