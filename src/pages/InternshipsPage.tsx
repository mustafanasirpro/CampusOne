import { ArrowLeft, ArrowRight, BriefcaseBusiness, Plus, RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { getMyInternships, getSavedInternships, listInternships } from "@/api/internshipsApi";
import { Button, EmptyState, ErrorMessage, LoadingSpinner, PageHeader, Tabs } from "@/components/common";
import { InternshipCard, InternshipFilterBar } from "@/components/internships";
import { paths } from "@/routes/paths";
import type { InternshipPage, InternshipSort, InternshipStatus, InternshipType, InternshipWorkMode } from "@/types/internships";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type View = "browse" | "mine" | "saved";
const pageSize = 12;

export function InternshipsPage() {
  const [view, setView] = useState<View>("browse");
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<InternshipSort>("NEWEST");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<InternshipStatus | "">("");
  const [internshipType, setInternshipType] = useState<InternshipType | "">("");
  const [workMode, setWorkMode] = useState<InternshipWorkMode | "">("");
  const [paid, setPaid] = useState<"" | "false" | "true">("");
  const [applied, setApplied] = useState({ search: "", status: "" as InternshipStatus | "", internshipType: "" as InternshipType | "", workMode: "" as InternshipWorkMode | "", paid: "" as "" | "false" | "true" });
  const [result, setResult] = useState<InternshipPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);
  useDocumentTitle("Internship Hub · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    const filters = {
      internshipType: applied.internshipType || undefined,
      page,
      paid: applied.paid === "" ? undefined : applied.paid === "true",
      search: applied.search || undefined,
      signal: controller.signal,
      size: pageSize,
      sort,
      status: applied.status || undefined,
      workMode: applied.workMode || undefined,
    };
    const request = view === "saved"
      ? getSavedInternships({ page, signal: controller.signal, size: pageSize, sort })
      : view === "mine" ? getMyInternships(filters) : listInternships(filters);
    void request.then((response) => {
      if (!active) return; setResult(response); setError(null);
    }).catch((requestError: unknown) => {
      if (!active) return;
      setResult(null);
      setError(requestError instanceof ApiError ? requestError.message : "Internships could not be loaded.");
    }).finally(() => { if (active) setIsLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [applied, page, refreshKey, sort, view]);

  const applyFilters = () => {
    setApplied({ internshipType, paid, search: search.trim(), status, workMode });
    setPage(0); setIsLoading(true); setRefreshKey((value) => value + 1);
  };
  const reset = () => {
    setSearch(""); setStatus(""); setInternshipType(""); setWorkMode(""); setPaid("");
    setApplied({ search: "", status: "", internshipType: "", workMode: "", paid: "" });
    setSort("NEWEST"); setPage(0); setIsLoading(true); setRefreshKey((value) => value + 1);
  };
  return (
    <div className="grid gap-6 pb-8">
      <PageHeader actions={<Link className="inline-flex h-10 items-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white" to={paths.internshipNew}><Plus className="size-4" />Post internship</Link>} description="Find and share student-friendly opportunities across the CampusOne community." eyebrow="Career opportunities" title="Internship Hub" />
      <Tabs activeTab={view} onChange={(next) => { setView(next); setPage(0); setResult(null); setError(null); setIsLoading(true); }} tabs={[{ label: "Browse", value: "browse" }, { label: "My internships", value: "mine" }, { label: "Saved", value: "saved" }]} />
      <InternshipFilterBar disabled={view === "saved"} internshipType={internshipType} onApply={applyFilters} onClear={reset} onInternshipTypeChange={setInternshipType} onPaidChange={setPaid} onSearchChange={setSearch} onSortChange={(value) => { setSort(value); setPage(0); setIsLoading(true); }} onStatusChange={setStatus} onWorkModeChange={setWorkMode} paid={paid} search={search} sort={sort} status={status} workMode={workMode} />
      {error ? <div className="grid gap-3"><ErrorMessage message={error} /><Button className="w-fit" onClick={() => { setError(null); setIsLoading(true); setRefreshKey((value) => value + 1); }} variant="outline"><RefreshCw className="size-4" />Try again</Button></div> : null}
      {isLoading ? <div className="grid min-h-72 place-items-center rounded-2xl border bg-white"><LoadingSpinner label="Loading internships" /></div> : null}
      {!isLoading && !error && result?.content.length === 0 ? <EmptyState action={<Link className="rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white" to={paths.internshipNew}>Post an internship</Link>} description={view === "saved" ? "You have not saved any internships yet." : view === "mine" ? "You have not posted any internships yet." : "No internships match the selected filters."} icon={<BriefcaseBusiness className="size-6" />} title="No internships found" /> : null}
      {!isLoading && !error && result && result.content.length > 0 ? <>
        <p className="text-sm text-slate-500">Showing {result.content.length} of {result.totalElements} internships</p>
        <div className="grid gap-5 md:grid-cols-2 2xl:grid-cols-3">{result.content.map((internship) => <InternshipCard internship={internship} key={internship.id} />)}</div>
        <nav aria-label="Internship pagination" className="flex items-center justify-between rounded-2xl border bg-white p-3"><Button disabled={result.first} onClick={() => { setPage((value) => Math.max(0, value - 1)); setIsLoading(true); }} variant="outline"><ArrowLeft className="size-4" />Previous</Button><span className="text-sm font-semibold">{result.page + 1} / {Math.max(1, result.totalPages)}</span><Button disabled={result.last} onClick={() => { setPage((value) => value + 1); setIsLoading(true); }} variant="outline">Next<ArrowRight className="size-4" /></Button></nav>
      </> : null}
    </div>
  );
}
