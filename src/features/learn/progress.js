// @ts-nocheck
/**
 * progress.js
 * -----------
 * Tracks how far a learner has got through CampusOne Learn — which lessons they
 * finished, whether they passed each quiz, and which code challenges they
 * solved. Stored per-browser in localStorage. Plain JavaScript; every mutator
 * returns a fresh snapshot so React can re-render.
 *
 * Shape: { [trackId]: { lessons: { [id]: true }, quiz: true, challenges: { [id]: true } } }
 */

const KEY = "campusone.learn.progress";

function read() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || {};
  } catch {
    return {};
  }
}

function write(progress) {
  try {
    localStorage.setItem(KEY, JSON.stringify(progress));
  } catch {
    /* storage unavailable — non-fatal */
  }
  return progress;
}

function ensureTrack(progress, trackId) {
  if (!progress[trackId]) {
    progress[trackId] = { lessons: {}, quiz: false, challenges: {} };
  }
  return progress;
}

export function loadProgress() {
  return read();
}

export function markLesson(trackId, lessonId, done = true) {
  const progress = ensureTrack(read(), trackId);
  progress[trackId].lessons[lessonId] = done;
  return write(progress);
}

export function markQuiz(trackId, done = true) {
  const progress = ensureTrack(read(), trackId);
  progress[trackId].quiz = done;
  return write(progress);
}

export function markChallenge(trackId, challengeId, done = true) {
  const progress = ensureTrack(read(), trackId);
  progress[trackId].challenges[challengeId] = done;
  return write(progress);
}

export function isLessonDone(progress, trackId, lessonId) {
  return Boolean(progress[trackId] && progress[trackId].lessons[lessonId]);
}

export function isQuizDone(progress, trackId) {
  return Boolean(progress[trackId] && progress[trackId].quiz);
}

export function isChallengeDone(progress, trackId, challengeId) {
  return Boolean(progress[trackId] && progress[trackId].challenges[challengeId]);
}

/** Aggregate completion for a track: lessons + its quiz + its challenges. */
export function trackStats(progress, trackId, lessonCount, challengeCount) {
  const track = progress[trackId] || { lessons: {}, quiz: false, challenges: {} };
  const lessonsDone = Object.values(track.lessons).filter(Boolean).length;
  const challengesDone = Object.values(track.challenges).filter(Boolean).length;
  const done = lessonsDone + (track.quiz ? 1 : 0) + challengesDone;
  const total = lessonCount + 1 + challengeCount;
  return { done, total, percent: total ? Math.round((done / total) * 100) : 0 };
}
