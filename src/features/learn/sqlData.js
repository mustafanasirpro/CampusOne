// @ts-nocheck
/**
 * sqlData.js
 * ----------
 * Seed tables for the SQL playground — plain JavaScript. These stand in for a
 * real CampusOne database so learners can run queries against familiar data.
 */

export const tables = {
  students: {
    columns: ["id", "name", "program", "semester", "gpa"],
    rows: [
      [1, "Ayesha Khan", "BCS", 5, 3.8],
      [2, "Bilal Ahmed", "BCS", 5, 3.2],
      [3, "Sana Malik", "BSE", 3, 3.9],
      [4, "Usman Ali", "BCS", 5, 2.7],
      [5, "Hina Raza", "BSE", 3, 3.5],
      [6, "Omar Farooq", "BCS", 7, 3.1],
    ],
  },
  courses: {
    columns: ["code", "title", "credits", "department"],
    rows: [
      ["CS101", "Programming Fundamentals", 4, "CS"],
      ["CS201", "Data Structures", 4, "CS"],
      ["CS301", "Operating Systems", 3, "CS"],
      ["MT101", "Calculus", 3, "MATH"],
      ["SE201", "Web Technologies", 3, "SE"],
    ],
  },
  enrollments: {
    columns: ["student_id", "course_code", "grade"],
    rows: [
      [1, "CS201", "A"],
      [1, "CS301", "A"],
      [2, "CS201", "B"],
      [3, "SE201", "A"],
      [4, "CS301", "C"],
      [5, "SE201", "B"],
      [6, "CS301", "B"],
    ],
  },
};
