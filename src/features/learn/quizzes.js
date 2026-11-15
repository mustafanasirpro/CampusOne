// @ts-nocheck
/**
 * quizzes.js
 * ----------
 * Quiz questions for each Learn track — plain JavaScript data. Each question
 * has a prompt, a list of options, the index of the correct answer, and a
 * short explanation shown after grading.
 */

export const quizzes = {
  html: {
    title: "HTML Quiz",
    questions: [
      {
        q: "What does HTML stand for?",
        options: [
          "Hyper Text Markup Language",
          "Home Tool Markup Language",
          "Hyperlinks and Text Markup Language",
          "High Text Machine Language",
        ],
        answer: 0,
        explain: "HTML = Hyper Text Markup Language, the language that structures web pages.",
      },
      {
        q: "Which tag creates the largest heading?",
        options: ["<h6>", "<head>", "<h1>", "<heading>"],
        answer: 2,
        explain: "<h1> is the top-level (largest) heading; <h6> is the smallest.",
      },
      {
        q: "Which attribute sets the destination of a link?",
        options: ["src", "href", "link", "to"],
        answer: 1,
        explain: "The href attribute on an <a> element sets the URL it points to.",
      },
      {
        q: "Which element creates an ordered (numbered) list?",
        options: ["<ul>", "<list>", "<ol>", "<li>"],
        answer: 2,
        explain: "<ol> is an ordered list; <ul> is unordered (bulleted). Items use <li>.",
      },
      {
        q: "Which tag defines a table row?",
        options: ["<td>", "<tr>", "<th>", "<row>"],
        answer: 1,
        explain: "<tr> is a table row; it contains <th> header cells or <td> data cells.",
      },
    ],
  },
  css: {
    title: "CSS Quiz",
    questions: [
      {
        q: "What does CSS stand for?",
        options: [
          "Creative Style Sheets",
          "Cascading Style Sheets",
          "Computer Style Sheets",
          "Colorful Style Sheets",
        ],
        answer: 1,
        explain: "CSS = Cascading Style Sheets — it describes how HTML looks.",
      },
      {
        q: "Which property changes the text colour?",
        options: ["font-color", "text-style", "color", "foreground"],
        answer: 2,
        explain: "The color property sets text colour; background sets the background.",
      },
      {
        q: "In the box model, which is OUTSIDE the border?",
        options: ["padding", "content", "margin", "none of these"],
        answer: 2,
        explain: "Order from inside out: content → padding → border → margin.",
      },
      {
        q: "How do you select an element with class=\"card\"?",
        options: ["#card", ".card", "card", "*card"],
        answer: 1,
        explain: "A leading dot (.card) selects by class; a hash (#id) selects by id.",
      },
      {
        q: "Which display value enables Flexbox?",
        options: ["block", "grid", "flex", "inline"],
        answer: 2,
        explain: "display: flex turns an element into a flex container.",
      },
    ],
  },
  javascript: {
    title: "JavaScript Quiz",
    questions: [
      {
        q: "Which keyword declares a value that never changes?",
        options: ["var", "let", "const", "final"],
        answer: 2,
        explain: "const declares a constant binding; let is for values that change.",
      },
      {
        q: "What does console.log() do?",
        options: [
          "Styles the page",
          "Prints a message to the console",
          "Creates an HTML element",
          "Loads a file",
        ],
        answer: 1,
        explain: "console.log prints to the developer console — you can see it in the Console panel.",
      },
      {
        q: "How do you write a comparison that checks value AND type?",
        options: ["=", "==", "===", "!="],
        answer: 2,
        explain: "=== is strict equality (value and type). = is assignment.",
      },
      {
        q: "Which method runs a function for every array item?",
        options: ["forEach", "map.push", "loopEach", "forAll"],
        answer: 0,
        explain: "array.forEach(callback) calls the callback once per element.",
      },
      {
        q: "How do you read the 'name' property of an object student?",
        options: ["student->name", "student.name", "student::name", "name(student)"],
        answer: 1,
        explain: "Dot notation: student.name. You can also use student[\"name\"].",
      },
    ],
  },
};

/** Returns the quiz for a track, or null if none exists. */
export function getQuiz(trackId) {
  return quizzes[trackId] || null;
}
