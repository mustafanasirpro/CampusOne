import { ArrowLeft, ArrowUpRight, Bookmark, Building2, CalendarClock, Edit3, MapPin, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { deleteInternship, getInternshipById, saveInternship, unsaveInternship } from "@/api/internshipsApi";
import { Avatar, Badge, Button, Card, CardContent, ErrorMessage, LoadingSpinner, useToast } from "@/components/common";
import { formatInternshipDeadline, formatInternshipPay, internshipStatusLabel, internshipTypeLabel, internshipWorkModeLabel } from "@/components/internships";
import { paths } from "@/routes/paths";
import type { InternshipDetail } from "@/types/internships";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function InternshipDetailPage() {
  const { internshipId } = useParams<{ internshipId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [internship, setInternship] = useState<InternshipDetail | null>(null);
  const [error, setError] = useState<string | null>(() => internshipId ? null : "The internship ID is missing.");
  const [actionError, setActionError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(internshipId));
  const [busy, setBusy] = useState<string | null>(null);
  useDocumentTitle(internship ? `${internship.title} · CampusOne` : "Internship · CampusOne");
  useEffect(() => {
    if (!internshipId) return;
    const controller = new AbortController(); let active = true;
    void getInternshipById(internshipId, controller.signal).then((detail) => { if (active) setInternship(detail); }).catch((requestError: unknown) => { if (active) setError(requestError instanceof ApiError ? requestError.message : "The internship could not be loaded."); }).finally(() => { if (active) setIsLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [internshipId]);
  const toggleSave = async () => {
    if (!internship) return;
    setBusy("save"); setActionError(null);
    try {
      if (internship.savedByCurrentUser) {
        await unsaveInternship(internship.id); setInternship({ ...internship, savedByCurrentUser: false });
        showToast({ title: "Removed from saved", message: internship.title });
      } else {
        await saveInternship(internship.id); setInternship({ ...internship, savedByCurrentUser: true });
        showToast({ title: "Internship saved", message: internship.title, variant: "success" });
      }
    } catch (requestError) { setActionError(requestError instanceof ApiError ? requestError.message : "Saved state could not be updated."); }
    finally { setBusy(null); }
  };
  const remove = async () => {
    if (!internship || !window.confirm(`Delete "${internship.title}"?`)) return;
    setBusy("delete"); setActionError(null);
    try { await deleteInternship(internship.id); showToast({ title: "Internship deleted", message: internship.title, variant: "success" }); navigate(paths.internships, { replace: true }); }
    catch (requestError) { setActionError(requestError instanceof ApiError ? requestError.message : "The internship could not be deleted."); setBusy(null); }
  };
  if (isLoading) return <div className="grid min-h-[60vh] place-items-center"><LoadingSpinner label="Loading internship details" /></div>;
  if (!internship || error) return <div className="grid gap-4"><ErrorMessage message={error ?? "Internship not found."} /><Link to={paths.internships}>Back to internships</Link></div>;
  return (
    <div className="grid gap-6 pb-8">
      <div className="flex flex-wrap justify-between gap-3"><Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600" to={paths.internships}><ArrowLeft className="size-4" />Back to internships</Link>{internship.ownedByCurrentUser ? <div className="flex gap-2"><Link className="inline-flex h-10 items-center gap-2 rounded-xl border px-4 text-sm font-semibold" to={paths.internshipEdit(internship.id)}><Edit3 className="size-4" />Edit</Link><Button loading={busy === "delete"} onClick={() => void remove()} variant="danger"><Trash2 className="size-4" />Delete</Button></div> : null}</div>
      {actionError ? <ErrorMessage message={actionError} /> : null}
      <section className="rounded-3xl bg-slate-950 p-6 text-white sm:p-8"><div className="flex flex-wrap gap-2"><Badge className="bg-white/10 text-white ring-white/15">{internshipStatusLabel(internship.status)}</Badge><Badge className="bg-white/10 text-white ring-white/15">{internshipTypeLabel(internship.internshipType)}</Badge><Badge className="bg-white/10 text-white ring-white/15">{internshipWorkModeLabel(internship.workMode)}</Badge></div><p className="mt-6 text-sm font-semibold text-brand-300">{internship.companyName}</p><h1 className="mt-2 text-3xl font-bold sm:text-4xl">{internship.title}</h1><div className="mt-5 flex flex-wrap gap-5 text-sm text-slate-300"><span className="flex gap-2"><MapPin className="size-4" />{internship.location}</span><span className="flex gap-2"><CalendarClock className="size-4" />Apply by {formatInternshipDeadline(internship.deadline)}</span></div></section>
      <div className="grid gap-6 xl:grid-cols-[1fr_22rem]"><Card><CardContent className="grid gap-5 p-6"><h2 className="text-lg font-semibold">About the opportunity</h2><p className="whitespace-pre-wrap text-sm leading-7 text-slate-600">{internship.description}</p></CardContent></Card><aside className="grid content-start gap-4"><Card><CardContent className="grid gap-4 p-5"><p className="text-2xl font-bold text-brand-700">{formatInternshipPay(internship.paid, internship.stipendAmount, internship.currency)}</p><a className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700" href={internship.applyUrl} rel="noopener noreferrer" target="_blank">Apply now<ArrowUpRight className="size-4" /></a><Button loading={busy === "save"} onClick={() => void toggleSave()} variant={internship.savedByCurrentUser ? "secondary" : "outline"}><Bookmark className={internship.savedByCurrentUser ? "size-4 fill-current" : "size-4"} />{internship.savedByCurrentUser ? "Unsave internship" : "Save internship"}</Button></CardContent></Card><Card><CardContent className="flex items-center gap-3 p-5"><Avatar name={internship.poster.fullName} src={internship.poster.avatarUrl ?? undefined} /><div><p className="font-semibold">{internship.poster.fullName}</p><p className="text-xs text-slate-400">{internship.poster.university}</p></div><Building2 className="ml-auto size-5 text-slate-300" /></CardContent></Card></aside></div>
    </div>
  );
}
