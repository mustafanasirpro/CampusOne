// @ts-nocheck
/**
 * quiz.js
 * -------
 * Grading logic for Learn quizzes — plain JavaScript, no framework. Keeping the
 * scoring here (rather than in the React component) makes it easy to unit-test
 * and to read on its own.
 */

/**
 * Grades a set of answers against the quiz questions.
 *
 * @param {Array<{ answer: number }>} questions the quiz questions
 * @param {Array<number|null>} answers the option index the learner picked per question
 * @returns {{ score: number, total: number, percent: number, passed: boolean,
 *            results: Array<{ correct: boolean, chosen: number|null, answer: number }> }}
 */
export function gradeQuiz(questions, answers) {
  const results = questions.map((question, index) => {
    const chosen = answers[index] == null ? null : answers[index];
    return {
      correct: chosen === question.answer,
      chosen,
      answer: question.answer,
    };
  });
  const score = results.filter((r) => r.correct).length;
  const total = questions.length;
  const percent = total ? Math.round((score / total) * 100) : 0;
  return { score, total, percent, passed: percent >= 60, results };
}

/** A short, encouraging message based on the percentage scored. */
export function feedbackFor(percent) {
  if (percent === 100) return "Perfect score! You've mastered this track.";
  if (percent >= 80) return "Great work — almost flawless.";
  if (percent >= 60) return "Nice, you passed. Review the ones you missed.";
  return "Keep going — revisit the lessons and try again.";
}
