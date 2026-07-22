import { Archive, Copy, Download, GitCompare, Lock, MoveRight, Repeat2, Unlock } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  applyAuraSessionMove,
  applyAuraSessionSwap,
  archiveAuraVersion,
  cloneAuraVersion,
  compareAuraVersions,
  downloadAuraVersion,
  listAuraRooms,
  listAuraTimeslots,
  previewAuraSessionMove,
  previewAuraSessionSwap,
  setAuraSessionPinned,
} from "@/api/auraApi";
import { Button, Card, CardContent, CardHeader, CardTitle, ErrorMessage } from "@/components/common";
import type {
  AuraMovePreview,
  AuraRoom,
  AuraSession,
  AuraTimeslot,
  AuraTimetableVersion,
  AuraVersionComparison,
} from "@/types/aura";

interface AuraVersionToolsProps {
  onChanged: () => Promise<void>;
  onVersionSelected: (versionId: string) => void;
  selectedVersionId: string;
  sessions: AuraSession[];
  versions: AuraTimetableVersion[];
}

function message(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

export function AuraVersionTools({
  onChanged,
  onVersionSelected,
  selectedVersionId,
  sessions,
  versions,
}: AuraVersionToolsProps) {
  const [otherVersionId, setOtherVersionId] = useState("");
  const [firstSessionId, setFirstSessionId] = useState("");
  const [secondSessionId, setSecondSessionId] = useState("");
  const [reason, setReason] = useState("");
  const [comparison, setComparison] = useState<AuraVersionComparison | null>(null);
  const [swapPreview, setSwapPreview] = useState<AuraMovePreview | null>(null);
  const [moveSessionId, setMoveSessionId] = useState("");
  const [moveRoomId, setMoveRoomId] = useState("");
  const [moveTimeslotId, setMoveTimeslotId] = useState("");
  const [movePreview, setMovePreview] = useState<AuraMovePreview | null>(null);
  const [moveReason, setMoveReason] = useState("");
  const [rooms, setRooms] = useState<AuraRoom[]>([]);
  const [timeslots, setTimeslots] = useState<AuraTimeslot[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<string | null>(null);

  const selectedVersion = useMemo(
    () => versions.find((version) => version.id === selectedVersionId) ?? null,
    [selectedVersionId, versions],
  );
  const selectedSessions = useMemo(
    () => sessions.filter((session) => session.versionId === selectedVersionId),
    [selectedVersionId, sessions],
  );
  const draft = selectedVersion?.status === "DRAFT";
  const firstSession = selectedSessions.find((session) => session.id === firstSessionId);

  useEffect(() => {
    const controller = new AbortController();
    if (!draft) return () => controller.abort();
    void Promise.all([
      listAuraRooms("", controller.signal),
      listAuraTimeslots("", controller.signal),
    ])
      .then(([roomOptions, timeslotOptions]) => {
        setRooms(roomOptions.filter((room) => room.active));
        setTimeslots(timeslotOptions.filter((timeslot) => timeslot.active));
      })
      .catch((requestError: unknown) => {
        if (!controller.signal.aborted) {
          setError(message(requestError, "Room and timeslot options could not be loaded."));
        }
      });
    return () => controller.abort();
  }, [draft]);

  const run = async (action: string, task: () => Promise<void>) => {
    setPendingAction(action);
    setError(null);
    try {
      await task();
    } catch (requestError) {
      setError(message(requestError, "The timetable action could not be completed."));
    } finally {
      setPendingAction(null);
    }
  };

  if (!selectedVersion) return null;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Version workbench</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-5">
        {error ? <ErrorMessage message={error} /> : null}

        <div className="flex flex-wrap gap-2">
          <Button
            loading={pendingAction === "clone"}
            onClick={() => void run("clone", async () => {
              const clonedVersion = await cloneAuraVersion(
                selectedVersionId,
                `Draft cloned from version ${selectedVersion.versionNumber}`,
              );
              await onChanged();
              onVersionSelected(clonedVersion.id);
            })}
            variant="outline"
          >
            <Copy className="size-4" /> Clone to draft
          </Button>
          <Button
            disabled={!draft}
            loading={pendingAction === "archive"}
            onClick={() => void run("archive", async () => {
              await archiveAuraVersion(selectedVersionId);
              await onChanged();
            })}
            variant="outline"
          >
            <Archive className="size-4" /> Archive draft
          </Button>
          {(["CSV", "XLSX", "JSON", "HTML", "ICS", "PDF"] as const).map((format) => (
            <Button
              key={format}
              loading={pendingAction === `export-${format}`}
              onClick={() => void run(`export-${format}`, () => downloadAuraVersion(selectedVersionId, format))}
              variant="outline"
            >
              <Download className="size-4" /> {format}
            </Button>
          ))}
        </div>

        <div className="grid gap-3 rounded-2xl border border-slate-200 p-4 lg:grid-cols-[1fr_auto]">
          <label className="grid gap-1 text-sm font-medium text-slate-700">
            Compare with
            <select
              aria-label="Compare with"
              className="rounded-xl border border-slate-300 px-3 py-2"
              onChange={(event) => setOtherVersionId(event.target.value)}
              value={otherVersionId}
            >
              <option value="">Choose another version</option>
              {versions.filter((version) => version.id !== selectedVersionId).map((version) => (
                <option key={version.id} value={version.id}>
                  Version {version.versionNumber} · {version.status}
                </option>
              ))}
            </select>
          </label>
          <Button
            className="self-end"
            disabled={!otherVersionId}
            loading={pendingAction === "compare"}
            onClick={() => void run("compare", async () => {
              setComparison(await compareAuraVersions(selectedVersionId, otherVersionId));
            })}
          >
            <GitCompare className="size-4" /> Compare
          </Button>
          {comparison ? (
            <p className="text-sm text-slate-600 lg:col-span-2">
              {comparison.changedOccurrences} of {comparison.totalOccurrences} occurrences changed · {comparison.addedOccurrences} added · {comparison.removedOccurrences} removed
            </p>
          ) : null}
        </div>

        <div className="grid gap-3 rounded-2xl border border-slate-200 p-4 lg:grid-cols-2">
          <p className="font-semibold text-slate-950 lg:col-span-2">Safe session move</p>
          <label className="grid gap-1 text-sm font-medium text-slate-700">
            Session
            <select
              aria-label="Session"
              className="rounded-xl border border-slate-300 px-3 py-2"
              disabled={!draft}
              onChange={(event) => {
                setMoveSessionId(event.target.value);
                setMovePreview(null);
              }}
              value={moveSessionId}
            >
              <option value="">Choose a session</option>
              {selectedSessions.map((session) => (
                <option key={session.id} value={session.id}>
                  {session.courseCode} · {session.sectionName} · {session.roomName}
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1 text-sm font-medium text-slate-700">
            New room
            <select
              aria-label="New room"
              className="rounded-xl border border-slate-300 px-3 py-2"
              disabled={!draft}
              onChange={(event) => {
                setMoveRoomId(event.target.value);
                setMovePreview(null);
              }}
              value={moveRoomId}
            >
              <option value="">Choose a room</option>
              {rooms.map((room) => (
                <option key={room.id} value={room.id}>
                  {room.name} · {room.capacity} seats
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1 text-sm font-medium text-slate-700">
            New timeslot
            <select
              aria-label="New timeslot"
              className="rounded-xl border border-slate-300 px-3 py-2"
              disabled={!draft}
              onChange={(event) => {
                setMoveTimeslotId(event.target.value);
                setMovePreview(null);
              }}
              value={moveTimeslotId}
            >
              <option value="">Choose a timeslot</option>
              {timeslots.map((timeslot) => (
                <option key={timeslot.id} value={timeslot.id}>
                  {timeslot.label}
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1 text-sm font-medium text-slate-700">
            Change reason
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              disabled={!draft}
              maxLength={500}
              onChange={(event) => setMoveReason(event.target.value)}
              placeholder="Explain why this move is needed"
              value={moveReason}
            />
          </label>
          <div className="flex flex-wrap items-end gap-2">
            <Button
              disabled={!draft || !moveSessionId || !moveRoomId || !moveTimeslotId}
              loading={pendingAction === "preview-move"}
              onClick={() => void run("preview-move", async () => {
                setMovePreview(await previewAuraSessionMove(
                  moveSessionId, moveRoomId, moveTimeslotId,
                ));
              })}
              variant="outline"
            >
              <MoveRight className="size-4" /> Preview move
            </Button>
            <Button
              disabled={!draft || !movePreview?.allowed || !moveReason.trim()}
              loading={pendingAction === "apply-move"}
              onClick={() => void run("apply-move", async () => {
                await applyAuraSessionMove(
                  moveSessionId, moveRoomId, moveTimeslotId, moveReason.trim(),
                );
                setMovePreview(null);
                setMoveReason("");
                await onChanged();
              })}
            >
              Apply safe move
            </Button>
          </div>
          {movePreview ? (
            <p className={movePreview.allowed ? "text-sm text-emerald-700 lg:col-span-2" : "text-sm text-red-700 lg:col-span-2"}>
              {movePreview.message}
            </p>
          ) : null}
        </div>

        <div className="grid gap-3 rounded-2xl border border-slate-200 p-4 lg:grid-cols-2">
          <p className="font-semibold text-slate-950 lg:col-span-2">Safe session swap</p>
          {[firstSessionId, secondSessionId].map((value, index) => (
            <label className="grid gap-1 text-sm font-medium text-slate-700" key={index}>
              {index === 0 ? "First session" : "Second session"}
              <select
                aria-label={index === 0 ? "First session" : "Second session"}
                className="rounded-xl border border-slate-300 px-3 py-2"
                disabled={!draft}
                onChange={(event) => {
                  if (index === 0) setFirstSessionId(event.target.value);
                  else setSecondSessionId(event.target.value);
                  setSwapPreview(null);
                }}
                value={value}
              >
                <option value="">Choose a session</option>
                {selectedSessions.map((session) => (
                  <option key={session.id} value={session.id}>
                    {session.courseCode} · {session.sectionName} · {session.roomName}
                  </option>
                ))}
              </select>
            </label>
          ))}
          <label className="grid gap-1 text-sm font-medium text-slate-700 lg:col-span-2">
            Change reason
            <input
              className="rounded-xl border border-slate-300 px-3 py-2"
              disabled={!draft}
              maxLength={500}
              onChange={(event) => setReason(event.target.value)}
              placeholder="Explain why this change is needed"
              value={reason}
            />
          </label>
          <div className="flex flex-wrap gap-2 lg:col-span-2">
            <Button
              disabled={!draft || !firstSessionId || !secondSessionId || firstSessionId === secondSessionId}
              loading={pendingAction === "preview-swap"}
              onClick={() => void run("preview-swap", async () => {
                setSwapPreview(await previewAuraSessionSwap(firstSessionId, secondSessionId));
              })}
              variant="outline"
            >
              <Repeat2 className="size-4" /> Preview swap
            </Button>
            <Button
              disabled={!draft || !swapPreview?.allowed || !reason.trim()}
              loading={pendingAction === "apply-swap"}
              onClick={() => void run("apply-swap", async () => {
                await applyAuraSessionSwap(firstSessionId, secondSessionId, reason.trim());
                setSwapPreview(null);
                setReason("");
                await onChanged();
              })}
            >
              Apply safe swap
            </Button>
            <Button
              disabled={!draft || !firstSessionId}
              loading={pendingAction === "pin"}
              onClick={() => void run("pin", async () => {
                await setAuraSessionPinned(
                  firstSessionId,
                  !firstSession?.locked,
                  firstSession?.locked
                    ? undefined
                    : reason.trim() || "Pinned by timetable administrator",
                );
                await onChanged();
              })}
              variant="outline"
            >
              {firstSession?.locked ? <Unlock className="size-4" /> : <Lock className="size-4" />}
              {firstSession?.locked ? "Unpin first session" : "Pin first session"}
            </Button>
          </div>
          {swapPreview ? (
            <p className={swapPreview.allowed ? "text-sm text-emerald-700 lg:col-span-2" : "text-sm text-red-700 lg:col-span-2"}>
              {swapPreview.message}
            </p>
          ) : null}
        </div>
      </CardContent>
    </Card>
  );
}
