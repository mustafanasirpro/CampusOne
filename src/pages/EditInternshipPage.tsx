import { ArrowLeft, LockKeyhole } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { getInternshipById, updateInternship } from "@/api/internshipsApi";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader, useToast } from "@/components/common";
import { InternshipForm } from "@/components/internships";
import { paths } from "@/routes/paths";
import type { InternshipDetail, UpdateInternshipRequest } from "@/types/internships";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function EditInternshipPage() {
  const { internshipId } = useParams<{ internshipId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [internship, setInternship] = useState<InternshipDetail | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(internshipId));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(() => internshipId ? null : "The internship ID is missing.");
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string[]>>({});
  useDocumentTitle(internship ? `Edit ${internship.title} · CampusOne` : "Edit internship · CampusOne");
  useEffect(() => {
    if (!internshipId) return;
    const controller = new AbortController(); let active = true;
    void getInternshipById(internshipId, controller.signal).then((response) => { if (active) setInternship(response); }).catch((requestError: unknown) => { if (active) setLoadError(requestError instanceof ApiError ? requestError.message : "The internship could not be loaded."); }).finally(() => { if (active) setIsLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [internshipId]);
  const submit = async (request: UpdateInternshipRequest) => {
    if (!internshipId) return;
    setIsSubmitting(true); setSubmitError(null); setFieldErrors({});
    try {
      const updated = await updateInternship(internshipId, request);
      showToast({ title: "Internship updated", message: updated.title, variant: "success" });
      navigate(paths.internshipDetail(updated.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) { setSubmitError(requestError.message); setFieldErrors(requestError.fieldErrors); }
      else setSubmitError("The internship could not be updated.");
    } finally { setIsSubmitting(false); }
  };
  if (isLoading) return <div className="grid min-h-[60vh] place-items-center"><LoadingSpinner label="Loading internship" /></div>;
  if (!internship || loadError) return <ErrorMessage message={loadError ?? "Internship not found."} />;
  if (!internship.ownedByCurrentUser) return <EmptyState action={<Link className="rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white" to={paths.internshipDetail(internship.id)}>View internship</Link>} description="Only the original poster can edit this internship." icon={<LockKeyhole className="size-6" />} title="Poster access required" />;
  return <div className="grid gap-6 pb-8"><Link className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600" to={paths.internshipDetail(internship.id)}><ArrowLeft className="size-4" />Back to internship</Link><PageHeader description="Update opportunity details, deadline, compensation, or status." eyebrow="Your internships" title="Edit internship" />{submitError ? <ErrorMessage message={submitError} /> : null}<InternshipForm backendFieldErrors={fieldErrors} initialInternship={internship} isSubmitting={isSubmitting} mode="edit" onSubmit={submit} submitLabel="Save changes" /></div>;
}

