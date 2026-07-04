import { apiRequest } from "@/api/apiClient";
import type {
  AdminReportListParameters,
  ContentReportDetail,
  ContentReportPage,
  CreateReportRequest,
  ModerationAction,
  ModerationActionListParameters,
  ModerationActionPage,
  ModeratorStatus,
  MyReportListParameters,
} from "@/types/moderation";

function queryString(
  parameters: Record<string, string | number | undefined>,
) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

const reportPath = "/moderation/reports";
const adminPath = "/admin/moderation";

export function createReport(request: CreateReportRequest) {
  return apiRequest<ContentReportDetail>(reportPath, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function listMyReports({
  page = 0,
  signal,
  size = 12,
  sort = "NEWEST",
  status,
  targetType,
}: MyReportListParameters = {}) {
  return apiRequest<ContentReportPage>(
    `${reportPath}/my${queryString({
      page,
      size,
      sort,
      status,
      targetType,
    })}`,
    { signal },
  );
}

export function getMyReportById(
  reportId: string,
  signal?: AbortSignal,
) {
  return apiRequest<ContentReportDetail>(
    `${reportPath}/my/${reportId}`,
    { signal },
  );
}

export function getModeratorStatus(signal?: AbortSignal) {
  return apiRequest<ModeratorStatus>(`${adminPath}/me`, { signal });
}

export function listAdminReports({
  page = 0,
  reason,
  reporterUserId,
  signal,
  size = 12,
  sort = "NEWEST",
  status,
  targetType,
}: AdminReportListParameters = {}) {
  return apiRequest<ContentReportPage>(
    `${adminPath}/reports${queryString({
      page,
      reason,
      reporterUserId,
      size,
      sort,
      status,
      targetType,
    })}`,
    { signal },
  );
}

export function getAdminReportById(
  reportId: string,
  signal?: AbortSignal,
) {
  return apiRequest<ContentReportDetail>(
    `${adminPath}/reports/${reportId}`,
    { signal },
  );
}

export function markReportUnderReview(reportId: string) {
  return apiRequest<ContentReportDetail>(
    `${adminPath}/reports/${reportId}/review`,
    { method: "PATCH" },
  );
}

export function resolveReport(reportId: string, resolutionNote: string) {
  return apiRequest<ContentReportDetail>(
    `${adminPath}/reports/${reportId}/resolve`,
    {
      body: JSON.stringify({ resolutionNote }),
      method: "PATCH",
    },
  );
}

export function dismissReport(reportId: string, resolutionNote: string) {
  return apiRequest<ContentReportDetail>(
    `${adminPath}/reports/${reportId}/dismiss`,
    {
      body: JSON.stringify({ resolutionNote }),
      method: "PATCH",
    },
  );
}

export function listModerationActions({
  actionType,
  moderatorUserId,
  page = 0,
  reportId,
  signal,
  size = 12,
  sort = "NEWEST",
  targetType,
}: ModerationActionListParameters = {}) {
  return apiRequest<ModerationActionPage>(
    `${adminPath}/actions${queryString({
      actionType,
      moderatorUserId,
      page,
      reportId,
      size,
      sort,
      targetType,
    })}`,
    { signal },
  );
}

export function getModerationActionById(
  actionId: string,
  signal?: AbortSignal,
) {
  return apiRequest<ModerationAction>(
    `${adminPath}/actions/${actionId}`,
    { signal },
  );
}
