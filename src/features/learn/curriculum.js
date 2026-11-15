// @ts-nocheck
/**
 * curriculum.js
 * -------------
 * The CampusOne Learn syllabus — written as plain JavaScript data so it is easy
 * to extend. Each track has lessons; each lesson has content blocks and a
 * runnable "Try it Yourself" example (html / css / js). Examples are themed
 * around CampusOne so the learning ties back into this project.
 *
 * Block kinds: { kind: "text", text } | { kind: "tip", text } |
 *              { kind: "code", code, lang }
 */

export const tracks = [
  {
    id: "html",
    label: "HTML",
    tagline: "The structure of every CampusOne page",
    lessons: [
      {
        id: "html-intro",
        title: "HTML Introduction",
        blocks: [
          { kind: "text", text: "HTML is the standard markup language for web pages. Every screen in CampusOne — the dashboard, the marketplace, your timetable — is ultimately HTML elements arranged on the page." },
          { kind: "text", text: "An HTML element is written with a start tag, some content, and an end tag:" },
          { kind: "code", lang: "html", code: "<h1>My First Heading</h1>\n<p>My first paragraph.</p>" },
          { kind: "tip", text: "Press \"Try it Yourself\" to edit the example live and see the result instantly." },
        ],
        example: {
          html: '<h1>Welcome to CampusOne</h1>\n<p>Everything your campus needs, in one place.</p>\n<button>Get started</button>',
          css: "",
          js: "",
        },
      },
      {
        id: "html-elements",
        title: "HTML Elements",
        blocks: [
          { kind: "text", text: "Headings, paragraphs, lists and links are the building blocks of a page. A student profile card, for example, is just a heading, some text and a list." },
          { kind: "code", lang: "html", code: "<ul>\n  <li>Notes</li>\n  <li>Discussions</li>\n  <li>Marketplace</li>\n</ul>" },
        ],
        example: {
          html: '<h2>Student Profile</h2>\n<p><strong>Name:</strong> Ayesha Khan</p>\n<p><strong>Program:</strong> BCS, Semester 5</p>\n<h3>Enrolled courses</h3>\n<ol>\n  <li>Operating Systems</li>\n  <li>Web Technologies</li>\n  <li>Design &amp; Analysis of Algorithms</li>\n</ol>',
          css: "",
          js: "",
        },
      },
    ],
  },
  {
    id: "css",
    label: "CSS",
    tagline: "How CampusOne gets its luxury look",
    lessons: [
      {
        id: "css-intro",
        title: "CSS Introduction",
        blocks: [
          { kind: "text", text: "CSS describes how HTML elements should look. CampusOne's warm maroon-and-cream theme is all CSS — colours, spacing, rounded corners and shadows." },
          { kind: "code", lang: "css", code: "h1 {\n  color: #7d2826;        /* CampusOne maroon */\n  font-family: serif;\n}" },
        ],
        example: {
          html: '<div class="card">\n  <h2>FA24-BCS-5-D</h2>\n  <p>Operating Systems · 09:55–11:20 · CLab-5</p>\n</div>',
          css: "body { background: #f2ede0; }\n.card {\n  background: #fff;\n  border: 1px solid #e7e1d1;\n  border-radius: 16px;\n  padding: 20px;\n  box-shadow: 0 10px 30px rgba(52,55,35,.08);\n}\n.card h2 { color: #7d2826; margin: 0 0 6px; }\n.card p { color: #655f4e; margin: 0; }",
          js: "",
        },
      },
      {
        id: "css-box",
        title: "The Box Model",
        blocks: [
          { kind: "text", text: "Every element is a box with content, padding, a border and margin. Understanding the box model is the key to laying anything out." },
          { kind: "tip", text: "Change the padding and border-radius in the example and watch the card respond." },
        ],
        example: {
          html: '<span class="badge">Lab</span>\n<span class="badge alt">2 hr</span>',
          css: ".badge {\n  display: inline-block;\n  padding: 6px 12px;\n  margin: 6px;\n  border-radius: 999px;\n  background: #f3e0de;\n  color: #7d2826;\n  font-family: sans-serif;\n  font-size: 14px;\n}\n.badge.alt { background: #e8e8d8; color: #41422f; }",
          js: "",
        },
      },
    ],
  },
  {
    id: "javascript",
    label: "JavaScript",
    tagline: "The language that makes CampusOne interactive",
    lessons: [
      {
        id: "js-intro",
        title: "JavaScript Introduction",
        blocks: [
          { kind: "text", text: "JavaScript makes pages interactive — reacting to clicks, updating the page, doing calculations. This whole Learn playground is powered by JavaScript." },
          { kind: "code", lang: "js", code: 'console.log("Hello, CampusOne!");' },
          { kind: "tip", text: "Anything you console.log appears in the Console panel under the result." },
        ],
        example: {
          html: "<h2>Greeting</h2>\n<p id=\"out\">...</p>",
          css: "#out { color: #7d2826; font-size: 20px; font-family: sans-serif; }",
          js: 'const name = "Ayesha";\ndocument.getElementById("out").textContent = "Welcome back, " + name + "!";\nconsole.log("Rendered greeting for", name);',
        },
      },
      {
        id: "js-events",
        title: "Events & the DOM",
        blocks: [
          { kind: "text", text: "You can listen for events like clicks and change the page in response. Here a button counts how many times it was pressed." },
          { kind: "code", lang: "js", code: 'button.addEventListener("click", () => {\n  count++;\n});' },
        ],
        example: {
          html: '<button id="btn">Attendance: <span id="n">0</span></button>',
          css: "#btn { padding: 12px 18px; border-radius: 12px; border: 0; background: #7d2826; color: #fff; font-size: 16px; cursor: pointer; }",
          js: 'let count = 0;\nconst n = document.getElementById("n");\ndocument.getElementById("btn").addEventListener("click", () => {\n  count++;\n  n.textContent = count;\n  console.log("Marked present:", count);\n});',
        },
      },
      {
        id: "js-gpa",
        title: "Project: GPA calculator",
        blocks: [
          { kind: "text", text: "Let's build something real — a GPA calculator. It reads grade points, averages them, and shows the result. This is the kind of logic that powers features across CampusOne." },
          { kind: "tip", text: "Edit the grades array and re-run to see the GPA change." },
        ],
        example: {
          html: '<h2>Semester GPA</h2>\n<p id="gpa">—</p>',
          css: "#gpa { font-size: 40px; color: #7d2826; font-family: serif; }",
          js: 'const grades = [4.0, 3.7, 3.3, 4.0, 3.0];\nconst gpa = grades.reduce((sum, g) => sum + g, 0) / grades.length;\ndocument.getElementById("gpa").textContent = gpa.toFixed(2);\nconsole.log("Courses:", grades.length, "GPA:", gpa.toFixed(2));',
        },
      },
    ],
  },
];

/** Finds a track by id. */
export function getTrack(trackId) {
  return tracks.find((t) => t.id === trackId) || tracks[0];
}

/** Finds a lesson within a track. */
export function getLesson(trackId, lessonId) {
  const track = getTrack(trackId);
  return track.lessons.find((l) => l.id === lessonId) || track.lessons[0];
}
