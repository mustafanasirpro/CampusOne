import { SlidersHorizontal } from "lucide-react";
import { useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  getAuraConstraintProfile,
  replaceAuraConstraintProfile,
} from "@/api/auraApi";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  ErrorMessage,
  LoadingSpinner,
} from "@/components/common";
import type {
  AuraConstraintProfile,
  AuraConstraintProfileName,
} from "@/types/aura";

const profiles: AuraConstraintProfileName[] = [
  "FAST_FEASIBLE",
  "BALANCED",
  "COMPACT",
  "ROOM_EFFICIENT",
  "INSTRUCTOR_FRIENDLY",
  "QUALITY",
  "REPAIR",
  "WHAT_IF",
];

interface AuraConstraintProfilePanelProps {
  onProfileChange: (profile: AuraConstraintProfileName) => void;
  selectedProfile: AuraConstraintProfileName;
  termId: string;
}

export function AuraConstraintProfilePanel({
  onProfileChange,
  selectedProfile,
  termId,
}: AuraConstraintProfilePanelProps) {
  const [configuration, setConfiguration] =
    useState<AuraConstraintProfile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void Promise.resolve()
      .then(() => {
        if (active) {
          setLoading(true);
          setConfiguration(null);
        }
        return getAuraConstraintProfile(
          termId,
          selectedProfile,
          controller.signal,
        );
      })
      .then((response) => {
        if (!active) return;
        setConfiguration(response);
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (active && !controller.signal.aborted) {
          setError(messageFor(
            requestError,
            "Constraint settings could not be loaded.",
          ));
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [selectedProfile, termId]);

  const save = async () => {
    if (!configuration) return;
    setSaving(true);
    setError(null);
    try {
      setConfiguration(await replaceAuraConstraintProfile(
        termId,
        selectedProfile,
        configuration.weights,
      ));
    } catch (requestError) {
      setError(messageFor(
        requestError,
        "Constraint settings could not be saved.",
      ));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Generation profile</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-4">
        <div className="grid gap-2 sm:grid-cols-[minmax(0,16rem)_1fr] sm:items-end">
          <label className="grid gap-1 text-sm font-medium text-slate-700">
            Profile
            <select
              aria-label="Generation profile"
              className="rounded-xl border border-slate-300 bg-white px-3 py-2"
              onChange={(event) =>
                onProfileChange(event.target.value as AuraConstraintProfileName)
              }
              value={selectedProfile}
            >
              {profiles.map((profile) => (
                <option key={profile} value={profile}>
                  {profile.replaceAll("_", " ")}
                </option>
              ))}
            </select>
          </label>
          <p className="text-sm text-slate-500">
            Hard rules remain authoritative. Medium and soft weights tune the
            balance between speed, compactness, stability, and preferences.
          </p>
        </div>

        {error ? <ErrorMessage message={error} /> : null}
        {loading ? (
          <LoadingSpinner label="Loading constraint settings" />
        ) : configuration ? (
          <div className="grid gap-4">
            {(["HARD", "MEDIUM", "SOFT"] as const).map((level) => (
              <fieldset className="grid gap-2" key={level}>
                <legend className="text-xs font-bold uppercase tracking-wider text-slate-500">
                  {level} constraints
                </legend>
                <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
                  {configuration.weights
                    .filter((weight) => weight.constraintLevel === level)
                    .map((weight) => (
                      <label
                        className="grid grid-cols-[1fr_auto] items-center gap-3 rounded-xl border border-slate-200 p-3 text-sm"
                        key={weight.constraintName}
                      >
                        <span>
                          <span className="block font-medium text-slate-800">
                            {weight.constraintName}
                          </span>
                          <span className="mt-1 flex items-center gap-2 text-xs text-slate-500">
                            <input
                              checked={weight.active}
                              onChange={(event) =>
                                setConfiguration((current) => current ? ({
                                  ...current,
                                  weights: current.weights.map((candidate) =>
                                    candidate.constraintName === weight.constraintName
                                      ? { ...candidate, active: event.target.checked }
                                      : candidate),
                                }) : current)
                              }
                              type="checkbox"
                            />
                            Active
                          </span>
                        </span>
                        <input
                          aria-label={`${weight.constraintName} weight`}
                          className="w-20 rounded-lg border border-slate-300 px-2 py-1 text-right"
                          max={1_000_000}
                          min={0}
                          onChange={(event) =>
                            setConfiguration((current) => current ? ({
                              ...current,
                              weights: current.weights.map((candidate) =>
                                candidate.constraintName === weight.constraintName
                                  ? {
                                      ...candidate,
                                      customized: true,
                                      weight: Math.max(
                                        0,
                                        Math.min(1_000_000, Number(event.target.value)),
                                      ),
                                    }
                                  : candidate),
                            }) : current)
                          }
                          type="number"
                          value={weight.weight}
                        />
                      </label>
                    ))}
                </div>
              </fieldset>
            ))}
            <div>
              <Button loading={saving} onClick={() => void save()}>
                <SlidersHorizontal className="size-4" />
                Save profile settings
              </Button>
            </div>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}

function messageFor(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}
