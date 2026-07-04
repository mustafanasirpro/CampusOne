import { apiRequest } from "@/api/apiClient";
import type {
  CreateInternshipRequest,
  InternshipDetail,
  InternshipListParameters,
  InternshipPage,
  SavedInternshipState,
  UpdateInternshipRequest,
} from "@/types/internships";

function queryString(
  parameters: Record<string, boolean | string | number | undefined>,
) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

const internshipsPath = "/internships";

function listQuery(parameters: InternshipListParameters) {
  return queryString({
    internshipType: parameters.internshipType,
    page: parameters.page ?? 0,
    paid: parameters.paid,
    search: parameters.search,
    size: parameters.size ?? 12,
    sort: parameters.sort ?? "NEWEST",
    status: parameters.status,
    workMode: parameters.workMode,
  });
}

export function listInternships(
  parameters: InternshipListParameters = {},
) {
  return apiRequest<InternshipPage>(
    `${internshipsPath}${listQuery(parameters)}`,
    { signal: parameters.signal },
  );
}

export function getMyInternships(
  parameters: InternshipListParameters = {},
) {
  return apiRequest<InternshipPage>(
    `${internshipsPath}/my${listQuery(parameters)}`,
    { signal: parameters.signal },
  );
}

export function getSavedInternships({
  page = 0,
  signal,
  size = 12,
  sort = "NEWEST",
}: Pick<
  InternshipListParameters,
  "page" | "signal" | "size" | "sort"
> = {}) {
  return apiRequest<InternshipPage>(
    `${internshipsPath}/saved${queryString({ page, size, sort })}`,
    { signal },
  );
}

export function getInternshipById(
  internshipId: string,
  signal?: AbortSignal,
) {
  return apiRequest<InternshipDetail>(
    `${internshipsPath}/${internshipId}`,
    { signal },
  );
}

export function createInternship(request: CreateInternshipRequest) {
  return apiRequest<InternshipDetail>(internshipsPath, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function updateInternship(
  internshipId: string,
  request: UpdateInternshipRequest,
) {
  return apiRequest<InternshipDetail>(
    `${internshipsPath}/${internshipId}`,
    {
      body: JSON.stringify(request),
      method: "PATCH",
    },
  );
}

export function deleteInternship(internshipId: string) {
  return apiRequest<void>(`${internshipsPath}/${internshipId}`, {
    method: "DELETE",
  });
}

export function saveInternship(internshipId: string) {
  return apiRequest<SavedInternshipState>(
    `${internshipsPath}/${internshipId}/save`,
    { method: "POST" },
  );
}

export function unsaveInternship(internshipId: string) {
  return apiRequest<void>(`${internshipsPath}/${internshipId}/save`, {
    method: "DELETE",
  });
}

export function getSavedState(
  internshipId: string,
  signal?: AbortSignal,
) {
  return apiRequest<SavedInternshipState>(
    `${internshipsPath}/${internshipId}/save/me`,
    { signal },
  );
}

