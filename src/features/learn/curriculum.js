// @ts-nocheck
/**
 * curriculum.js
 * -------------
 * The CampusOne Learn syllabus — plain JavaScript data, easy to extend. Each
 * track has lessons; each lesson has content blocks and a runnable
 * "Try it Yourself" example (html / css / js). Examples are themed around
 * CampusOne so the learning ties back into this project.
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
          { kind: "tip", text: "Press \"Run\" to execute the example live, and edit it to experiment." },
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
          { kind: "text", text: "Headings (<h1>–<h6>), paragraphs (<p>) and emphasis (<strong>, <em>) are the building blocks of a page. A profile card is just headings and text." },
          { kind: "code", lang: "html", code: "<h2>Heading</h2>\n<p>A <strong>bold</strong> and <em>italic</em> paragraph.</p>" },
        ],
        example: {
          html: '<h2>Student Profile</h2>\n<p><strong>Name:</strong> Ayesha Khan</p>\n<p><strong>Program:</strong> BCS, Semester 5</p>',
          css: "",
          js: "",
        },
      },
      {
        id: "html-attributes",
        title: "Attributes",
        blocks: [
          { kind: "text", text: "Attributes give elements extra information, written as name=\"value\" inside the start tag. Links use href, images use src and alt." },
          { kind: "code", lang: "html", code: '<a href="https://campusone.pk">Visit CampusOne</a>' },
          { kind: "tip", text: "alt text describes an image for screen readers and when the image fails to load." },
        ],
        example: {
          html: '<a href="https://example.com" title="Opens the notes library">Open notes</a>\n<p>Hover the link to see its title attribute.</p>',
          css: "a { color: #7d2826; }",
          js: "",
        },
      },
      {
        id: "html-lists",
        title: "Lists",
        blocks: [
          { kind: "text", text: "Use <ul> for an unordered (bulleted) list and <ol> for an ordered (numbered) list. Each item is an <li>." },
          { kind: "code", lang: "html", code: "<ul>\n  <li>Notes</li>\n  <li>Discussions</li>\n</ul>" },
        ],
        example: {
          html: '<h3>Today\'s classes</h3>\n<ol>\n  <li>Operating Systems — 09:55</li>\n  <li>Web Technologies — 11:20</li>\n  <li>Algorithms — 15:05</li>\n</ol>',
          css: "li { margin: 4px 0; }",
          js: "",
        },
      },
      {
        id: "html-tables",
        title: "Tables",
        blocks: [
          { kind: "text", text: "Tables organise data in rows (<tr>) and cells (<td>), with headers (<th>). A timetable is a natural fit for a table." },
          { kind: "code", lang: "html", code: "<table>\n  <tr><th>Time</th><th>Course</th></tr>\n  <tr><td>09:55</td><td>OS</td></tr>\n</table>" },
        ],
        example: {
          html: '<table>\n  <tr><th>Time</th><th>Course</th><th>Room</th></tr>\n  <tr><td>09:55</td><td>Operating Systems</td><td>CLab-5</td></tr>\n  <tr><td>11:20</td><td>Web Technologies</td><td>W4</td></tr>\n</table>',
          css: "table { border-collapse: collapse; width: 100%; font-family: sans-serif; }\nth, td { border: 1px solid #e7e1d1; padding: 8px; text-align: left; }\nth { background: #f3e0de; color: #7d2826; }",
          js: "",
        },
      },
      {
        id: "html-forms",
        title: "Forms & Inputs",
        blocks: [
          { kind: "text", text: "Forms collect input from users. <input>, <label> and <button> power the login, signup and search screens in CampusOne." },
          { kind: "code", lang: "html", code: '<label>Email <input type="email"></label>' },
        ],
        example: {
          html: '<form onsubmit="return false">\n  <label>University email<br><input type="email" placeholder="you@university.edu.pk"></label>\n  <br><br>\n  <button type="submit">Log in</button>\n</form>',
          css: "input { padding: 8px; border: 1px solid #d5cdb8; border-radius: 8px; }\nbutton { padding: 8px 16px; background: #7d2826; color: #fff; border: 0; border-radius: 8px; }",
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
          { kind: "code", lang: "css", code: "h1 {\n  color: #7d2826;   /* CampusOne maroon */\n  font-family: serif;\n}" },
        ],
        example: {
          html: '<div class="card">\n  <h2>FA24-BCS-5-D</h2>\n  <p>Operating Systems · 09:55–11:20 · CLab-5</p>\n</div>',
          css: "body { background: #f2ede0; }\n.card {\n  background: #fff;\n  border: 1px solid #e7e1d1;\n  border-radius: 16px;\n  padding: 20px;\n  box-shadow: 0 10px 30px rgba(52,55,35,.08);\n}\n.card h2 { color: #7d2826; margin: 0 0 6px; }\n.card p { color: #655f4e; margin: 0; }",
          js: "",
        },
      },
      {
        id: "css-colors",
        title: "Colors & Backgrounds",
        blocks: [
          { kind: "text", text: "Colours can be named (red), hex (#7d2826), or rgb(). CampusOne's palette is maroon #7d2826, olive #898861, and cream #efe9d7." },
          { kind: "code", lang: "css", code: "background: #efe9d7;\ncolor: #343723;" },
          { kind: "tip", text: "Change the background values and re-run to preview the palette." },
        ],
        example: {
          html: '<div class="swatch a">Coral</div>\n<div class="swatch b">Leaf</div>\n<div class="swatch c">Juniper</div>',
          css: ".swatch { display:inline-block; width:90px; height:60px; border-radius:12px; color:#fff; font-family:sans-serif; padding:8px; margin:4px; }\n.a { background:#7d2826; }\n.b { background:#898861; }\n.c { background:#343723; }",
          js: "",
        },
      },
      {
        id: "css-box",
        title: "The Box Model",
        blocks: [
          { kind: "text", text: "Every element is a box with content, padding, a border and margin. Understanding the box model is the key to laying anything out." },
          { kind: "tip", text: "Change padding and border-radius in the example and watch the badges respond." },
        ],
        example: {
          html: '<span class="badge">Lab</span>\n<span class="badge alt">2 hr</span>',
          css: ".badge {\n  display: inline-block;\n  padding: 6px 12px;\n  margin: 6px;\n  border-radius: 999px;\n  background: #f3e0de;\n  color: #7d2826;\n  font-family: sans-serif;\n}\n.badge.alt { background: #e8e8d8; color: #41422f; }",
          js: "",
        },
      },
      {
        id: "css-selectors",
        title: "Selectors",
        blocks: [
          { kind: "text", text: "Selectors choose which elements to style: by tag (p), class (.card), or id (#total). Classes are the workhorse of CSS." },
          { kind: "code", lang: "css", code: "p { }          /* all paragraphs */\n.card { }      /* class=\"card\" */\n#total { }     /* id=\"total\" */" },
        ],
        example: {
          html: '<p>Normal paragraph</p>\n<p class="highlight">Highlighted paragraph</p>',
          css: "p { font-family: sans-serif; }\n.highlight { background: #faf2f1; color: #7d2826; padding: 6px; border-radius: 6px; }",
          js: "",
        },
      },
      {
        id: "css-flexbox",
        title: "Flexbox Layout",
        blocks: [
          { kind: "text", text: "Flexbox arranges items in a row or column with easy spacing and alignment. CampusOne's nav bar and card rows use it." },
          { kind: "code", lang: "css", code: "display: flex;\njustify-content: space-between;\ngap: 12px;" },
        ],
        example: {
          html: '<nav class="bar">\n  <span>CampusOne</span>\n  <span>Notes</span>\n  <span>Learn</span>\n</nav>',
          css: ".bar { display:flex; justify-content:space-between; gap:12px; background:#fff; padding:14px; border-radius:12px; border:1px solid #e7e1d1; font-family:sans-serif; }\n.bar span:first-child { color:#7d2826; font-weight:700; }",
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
          html: '<h2>Greeting</h2>\n<p id="out">...</p>',
          css: "#out { color: #7d2826; font-size: 20px; font-family: sans-serif; }",
          js: 'const name = "Ayesha";\ndocument.getElementById("out").textContent = "Welcome back, " + name + "!";\nconsole.log("Rendered greeting for", name);',
        },
      },
      {
        id: "js-variables",
        title: "Variables & Types",
        blocks: [
          { kind: "text", text: "Variables store values. Use const for values that don't change and let for ones that do. Types include string, number and boolean." },
          { kind: "code", lang: "js", code: 'const course = "OS";   // string\nlet credits = 3;        // number\nconst isLab = true;     // boolean' },
        ],
        example: {
          html: '<p id="out"></p>',
          css: "#out { font-family: sans-serif; }",
          js: 'const course = "Operating Systems";\nlet credits = 3;\nconst isLab = true;\ndocument.getElementById("out").textContent = course + " · " + credits + " credits · lab: " + isLab;\nconsole.log(typeof course, typeof credits, typeof isLab);',
        },
      },
      {
        id: "js-functions",
        title: "Functions",
        blocks: [
          { kind: "text", text: "A function is a reusable block of code. It can take inputs (parameters) and return a result." },
          { kind: "code", lang: "js", code: "function gradePoint(marks) {\n  if (marks >= 85) return 4.0;\n  if (marks >= 80) return 3.7;\n  return 3.0;\n}" },
        ],
        example: {
          html: '<p id="out"></p>',
          css: "#out { font-family: sans-serif; font-size: 18px; }",
          js: 'function gradePoint(marks) {\n  if (marks >= 85) return 4.0;\n  if (marks >= 80) return 3.7;\n  if (marks >= 70) return 3.3;\n  return 3.0;\n}\nconst gp = gradePoint(82);\ndocument.getElementById("out").textContent = "Marks 82 → GP " + gp;\nconsole.log("gradePoint(82) =", gp);',
        },
      },
      {
        id: "js-conditions",
        title: "Conditions",
        blocks: [
          { kind: "text", text: "if / else runs different code depending on a condition. Comparisons use === (equal), >, <, and so on." },
          { kind: "code", lang: "js", code: 'if (gpa >= 3.5) {\n  status = "Dean\'s List";\n} else {\n  status = "Keep going";\n}' },
        ],
        example: {
          html: '<p id="out"></p>',
          css: "#out { font-family: sans-serif; font-size: 18px; color:#7d2826; }",
          js: 'const gpa = 3.6;\nlet status;\nif (gpa >= 3.5) status = "Dean\'s List 🎓";\nelse if (gpa >= 2.0) status = "Good standing";\nelse status = "Needs support";\ndocument.getElementById("out").textContent = "GPA " + gpa + ": " + status;\nconsole.log(status);',
        },
      },
      {
        id: "js-arrays",
        title: "Arrays & Loops",
        blocks: [
          { kind: "text", text: "An array holds a list of values. Loop over them with forEach or a for loop to process each item." },
          { kind: "code", lang: "js", code: 'const courses = ["OS", "Web", "DAA"];\ncourses.forEach(c => console.log(c));' },
        ],
        example: {
          html: '<ul id="list"></ul>',
          css: "ul { font-family: sans-serif; }",
          js: 'const courses = ["Operating Systems", "Web Technologies", "Algorithms"];\nconst list = document.getElementById("list");\ncourses.forEach(function (course) {\n  const li = document.createElement("li");\n  li.textContent = course;\n  list.appendChild(li);\n});\nconsole.log("Rendered", courses.length, "courses");',
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
        id: "js-objects",
        title: "Objects",
        blocks: [
          { kind: "text", text: "An object groups related data as key: value pairs — perfect for modelling a student, a course, or a timetable session." },
          { kind: "code", lang: "js", code: 'const student = {\n  name: "Ayesha",\n  semester: 5,\n};' },
        ],
        example: {
          html: '<p id="out"></p>',
          css: "#out { font-family: sans-serif; }",
          js: 'const session = {\n  course: "Operating Systems",\n  time: "09:55–11:20",\n  room: "CLab-5",\n  lab: true,\n};\ndocument.getElementById("out").textContent =\n  session.course + " in " + session.room + " (" + session.time + ")";\nconsole.log(JSON.stringify(session));',
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
