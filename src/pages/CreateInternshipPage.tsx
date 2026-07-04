import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { createInternship } from "@/api/internshipsApi";
import { ErrorMessage, PageHeader, useToast } from "@/components/common";
import { InternshipForm } from "@/components/internships";
import { paths } from "@/routes/paths";
import type { CreateInternshipRequest } from "@/types/internships";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function CreateInternshipPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string[]>>({});
  useDocumentTitle("Post internship · CampusOne");
  const submit = async (request: CreateInternshipRequest) => {
    setIsSubmitting(true); setError(null); setFieldErrors({});
    try {
      const internship = await createInternship(request);
      showToast({ title: "Internship posted", message: internship.title, variant: "success" });
      navigate(paths.internshipDetail(internship.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) { setError(requestError.message); setFieldErrors(requestError.fieldErrors); }
      else setError("The internship could not be posted.");
    } finally { setIsSubmitting(false); }
  };
  return <div className="grid gap-6 pb-8"><Link className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600" to={paths.internships}><ArrowLeft className="size-4" />Back to internships</Link><PageHeader description="Share a clear, verifiable opportunity with students." eyebrow="Internship Hub" title="Post an internship" />{error ? <ErrorMessage message={error} /> : null}<InternshipForm backendFieldErrors={fieldErrors} isSubmitting={isSubmitting} mode="create" onSubmit={submit} submitLabel="Post internship" /></div>;
}

