// @ts-nocheck
/**
 * sqlChallenges.js
 * ----------------
 * "Write a query to get X" exercises for the SQL track. Plain JavaScript data.
 * Each challenge has a prompt, starter query, and a reference `solution`; the
 * learner's query is graded by running both through the mini SQL engine and
 * comparing the result sets.
 */

export const sqlChallenges = [
  {
    id: "sqlc-high",
    title: "Find the high achievers",
    prompt:
      "Write a query that returns the name and gpa of every student with a GPA of 3.5 or higher.",
    starter: "SELECT name, gpa FROM students\nWHERE ...;",
    solution: "SELECT name, gpa FROM students WHERE gpa >= 3.5",
  },
  {
    id: "sqlc-count",
    title: "Count the BCS students",
    prompt: "Write a query that returns how many students are in the 'BCS' program.",
    starter: "SELECT COUNT(*) FROM students\nWHERE ...;",
    solution: "SELECT COUNT(*) FROM students WHERE program = 'BCS'",
  },
  {
    id: "sqlc-avg",
    title: "Average GPA per program",
    prompt:
      "Write a query that returns each program alongside the average GPA of its students.",
    starter: "SELECT program, AVG(gpa) FROM students\n...;",
    solution: "SELECT program, AVG(gpa) FROM students GROUP BY program",
  },
];

export function getSqlChallenges() {
  return sqlChallenges;
}
