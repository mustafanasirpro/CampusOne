import {
  BrainCircuit,
  CalendarDays,
  CircleHelp,
  Clock3,
  Code2,
  Database,
  FileQuestion,
  FileText,
  Layers3,
  ListChecks,
  MessageSquareText,
  NotebookTabs,
  Sparkles,
} from "lucide-react";

import type {
  AIConversation,
  AIFeature,
  PromptSuggestion,
  StudyHistoryItem,
  StudyTool,
} from "@/types/ai";

export const initialAIConversations: AIConversation[] = [
  {
    id: "binary-trees",
    title: "Explain Binary Trees",
    category: "Data Structures",
    updatedAt: "12 min ago",
    isFavorite: true,
    messages: [
      {
        id: "binary-user-1",
        role: "user",
        text: "Explain binary trees simply, then show me a Java traversal example.",
        timestamp: "10:32 AM",
      },
      {
        id: "binary-ai-1",
        role: "assistant",
        timestamp: "10:32 AM",
        blocks: [
          {
            type: "heading",
            text: "Binary trees, without the fog",
          },
          {
            type: "paragraph",
            text: "A binary tree is a hierarchy where each node can have at most two children: a left child and a right child. Think of it as a family tree with only two branches allowed from every person.",
          },
          {
            type: "list",
            items: [
              "The top node is the root.",
              "A node with no children is a leaf.",
              "The depth tells you how far a node is from the root.",
              "In-order traversal visits left, node, then right.",
            ],
          },
          {
            type: "code",
            language: "java",
            code: `void inOrder(Node node) {\n  if (node == null) return;\n\n  inOrder(node.left);\n  System.out.println(node.value);\n  inOrder(node.right);\n}`,
          },
          {
            type: "math",
            expression: "Maximum nodes at level k = 2ᵏ",
            explanation:
              "A perfectly filled binary tree doubles the possible nodes at each new level.",
          },
        ],
      },
    ],
  },
  {
    id: "java-oop",
    title: "Java OOP Help",
    category: "Programming",
    updatedAt: "Yesterday",
    isFavorite: true,
    messages: [
      {
        id: "oop-user-1",
        role: "user",
        text: "When should I use an interface instead of an abstract class in Java?",
        timestamp: "Yesterday, 8:14 PM",
      },
      {
        id: "oop-ai-1",
        role: "assistant",
        timestamp: "Yesterday, 8:14 PM",
        blocks: [
          {
            type: "paragraph",
            text: "Use an interface when unrelated classes need to promise the same capability. Use an abstract class when related classes share identity, state, or partial implementation.",
          },
          {
            type: "table",
            headers: ["Question", "Interface", "Abstract class"],
            rows: [
              ["Shared state?", "No", "Yes"],
              ["Multiple inheritance?", "Multiple allowed", "One class"],
              ["Best for", "Capabilities", "Shared base identity"],
              ["Example", "Payable", "Vehicle"],
            ],
          },
          {
            type: "code",
            language: "java",
            code: `interface Payable {\n  double calculateTotal();\n}\n\nclass Invoice implements Payable {\n  public double calculateTotal() {\n    return 2500.0;\n  }\n}`,
          },
        ],
      },
    ],
  },
  {
    id: "operating-systems",
    title: "Operating Systems Notes",
    category: "Notes summary",
    updatedAt: "Jun 28",
    isFavorite: false,
    messages: [
      {
        id: "os-user-1",
        role: "user",
        text: "Summarize process scheduling for my operating systems exam.",
        timestamp: "Jun 28, 4:20 PM",
      },
      {
        id: "os-ai-1",
        role: "assistant",
        timestamp: "Jun 28, 4:20 PM",
        blocks: [
          {
            type: "heading",
            text: "Process scheduling exam summary",
          },
          {
            type: "list",
            items: [
              "FCFS is simple but can create the convoy effect.",
              "SJF minimizes average waiting time when burst time is known.",
              "Round Robin improves responsiveness through time slices.",
              "Priority scheduling can cause starvation; aging reduces it.",
              "Preemptive scheduling can interrupt a running process.",
            ],
          },
          {
            type: "paragraph",
            text: "For numerical questions, draw a Gantt chart first, then calculate completion, turnaround, and waiting time for every process.",
          },
        ],
      },
    ],
  },
  {
    id: "database-normalization",
    title: "Database Normalization",
    category: "Database Systems",
    updatedAt: "Jun 26",
    isFavorite: false,
    messages: [
      {
        id: "db-user-1",
        role: "user",
        text: "Explain normalization from 1NF to 3NF with a small example.",
        timestamp: "Jun 26, 11:05 AM",
      },
      {
        id: "db-ai-1",
        role: "assistant",
        timestamp: "Jun 26, 11:06 AM",
        blocks: [
          {
            type: "table",
            headers: ["Form", "Main rule", "What it removes"],
            rows: [
              ["1NF", "Atomic values", "Repeating groups"],
              ["2NF", "No partial dependency", "Dependency on part of a key"],
              ["3NF", "No transitive dependency", "Non-key-to-non-key dependency"],
            ],
          },
          {
            type: "code",
            language: "sql",
            code: `CREATE TABLE Student (\n  student_id INT PRIMARY KEY,\n  student_name VARCHAR(100),\n  department_id INT\n);\n\nCREATE TABLE Department (\n  department_id INT PRIMARY KEY,\n  department_name VARCHAR(100)\n);`,
          },
          {
            type: "paragraph",
            text: "The split removes the transitive dependency student_id → department_id → department_name, which is the key idea behind reaching 3NF.",
          },
        ],
      },
    ],
  },
  {
    id: "calculus-revision",
    title: "Calculus Revision",
    category: "Mathematics",
    updatedAt: "Jun 24",
    isFavorite: false,
    messages: [
      {
        id: "calculus-user-1",
        role: "user",
        text: "Help me revise the product rule and chain rule.",
        timestamp: "Jun 24, 7:40 PM",
      },
      {
        id: "calculus-ai-1",
        role: "assistant",
        timestamp: "Jun 24, 7:40 PM",
        blocks: [
          {
            type: "math",
            expression: "(fg)′ = f′g + fg′",
            explanation:
              "Differentiate the first function, keep the second; then keep the first, differentiate the second.",
          },
          {
            type: "math",
            expression: "d/dx f(g(x)) = f′(g(x)) · g′(x)",
            explanation:
              "Differentiate the outer function, leave the inner expression in place, then multiply by the derivative of the inner function.",
          },
          {
            type: "list",
            ordered: true,
            items: [
              "Identify whether functions are multiplied or nested.",
              "Write the relevant rule before substituting.",
              "Differentiate carefully, then simplify.",
              "Check the result with a simple value if possible.",
            ],
          },
        ],
      },
    ],
  },
];

export const promptSuggestions: PromptSuggestion[] = [
  {
    id: "summarize-notes",
    title: "Summarize my notes",
    description: "Turn a long topic into exam-ready points.",
    icon: FileText,
    prompt: "Summarize my notes into concise exam-ready points.",
    response: [
      { type: "heading", text: "Exam-ready summary" },
      {
        type: "list",
        items: [
          "Start with the core definition and purpose.",
          "Separate major concepts into short, testable points.",
          "Highlight formulas, comparisons, and common mistakes.",
          "End with three quick self-check questions.",
        ],
      },
      {
        type: "paragraph",
        text: "Paste or attach your notes in a connected version, and I would organize the actual material using this structure.",
      },
    ],
  },
  {
    id: "explain-simply",
    title: "Explain this topic simply",
    description: "Use analogies and plain language.",
    icon: BrainCircuit,
    prompt: "Explain binary trees in simple language with an analogy.",
    response: [
      {
        type: "paragraph",
        text: "Imagine a university society chart. One president sits at the top, and every member can coordinate at most two smaller groups. That branching structure is a binary tree.",
      },
      {
        type: "list",
        items: [
          "Root: the first node at the top.",
          "Child: a node directly below another node.",
          "Leaf: a node with no children.",
          "Traversal: a planned order for visiting every node.",
        ],
      },
    ],
  },
  {
    id: "generate-mcqs",
    title: "Generate MCQs",
    description: "Practice with answers and explanations.",
    icon: ListChecks,
    prompt: "Generate three OOP MCQs with answers.",
    response: [
      { type: "heading", text: "OOP practice MCQs" },
      {
        type: "list",
        ordered: true,
        items: [
          "Which concept hides internal implementation? A) Inheritance B) Encapsulation C) Overloading D) Aggregation — Answer: B",
          "Which keyword prevents method overriding in Java? A) static B) private C) final D) const — Answer: C",
          "Runtime polymorphism is commonly achieved through: A) Method overriding B) Constructors C) Packages D) Generics — Answer: A",
        ],
      },
    ],
  },
  {
    id: "viva-questions",
    title: "Prepare viva questions",
    description: "Rehearse concise oral answers.",
    icon: MessageSquareText,
    prompt: "Prepare database viva questions for me.",
    response: [
      { type: "heading", text: "Database viva practice" },
      {
        type: "list",
        items: [
          "What problem does normalization solve?",
          "How is a primary key different from a candidate key?",
          "When would an index make a query slower?",
          "Explain ACID using a banking transaction.",
          "What is the difference between DELETE, TRUNCATE, and DROP?",
        ],
      },
    ],
  },
  {
    id: "flashcards",
    title: "Create flashcards",
    description: "Convert concepts into quick recall prompts.",
    icon: Layers3,
    prompt: "Create flashcards for operating systems.",
    response: [
      {
        type: "table",
        headers: ["Front", "Back"],
        rows: [
          ["What is a process?", "A program currently in execution."],
          ["What is context switching?", "Saving one process state and loading another."],
          ["What causes deadlock?", "Mutual exclusion, hold and wait, no preemption, circular wait."],
          ["What is paging?", "Dividing memory into fixed-size pages and frames."],
        ],
      },
    ],
  },
  {
    id: "study-plan",
    title: "Make a study plan",
    description: "Build a realistic revision schedule.",
    icon: CalendarDays,
    prompt: "Make a three-day database exam study plan.",
    response: [
      {
        type: "table",
        headers: ["Day", "Focus", "Outcome"],
        rows: [
          ["Day 1", "ER models, relational algebra, SQL", "Solve 20 query problems"],
          ["Day 2", "Normalization, dependencies, indexing", "Normalize three case studies"],
          ["Day 3", "Transactions, recovery, past paper", "Complete one timed mock"],
        ],
      },
      {
        type: "paragraph",
        text: "Use 50-minute focus blocks with 10-minute breaks, and finish each day by recalling concepts without looking at notes.",
      },
    ],
  },
  {
    id: "programming-problem",
    title: "Solve programming problem",
    description: "Walk through logic before code.",
    icon: Code2,
    prompt: "Show me how to reverse a linked list.",
    response: [
      {
        type: "list",
        ordered: true,
        items: [
          "Keep pointers named previous, current, and next.",
          "Save current.next before changing any link.",
          "Point current.next backward to previous.",
          "Move previous and current one step forward.",
        ],
      },
      {
        type: "code",
        language: "java",
        code: `Node reverse(Node head) {\n  Node previous = null;\n  Node current = head;\n\n  while (current != null) {\n    Node next = current.next;\n    current.next = previous;\n    previous = current;\n    current = next;\n  }\n  return previous;\n}`,
      },
    ],
  },
  {
    id: "explain-java",
    title: "Explain Java code",
    description: "Understand syntax and execution flow.",
    icon: Code2,
    prompt: "Explain a Java stream example line by line.",
    response: [
      {
        type: "code",
        language: "java",
        code: `List<String> passed = students.stream()\n    .filter(student -> student.getMarks() >= 50)\n    .map(Student::getName)\n    .sorted()\n    .toList();`,
      },
      {
        type: "list",
        items: [
          "stream() creates a processing pipeline.",
          "filter keeps only students with marks of 50 or more.",
          "map converts each Student object into a name.",
          "sorted orders those names alphabetically.",
          "toList collects the result into a new list.",
        ],
      },
    ],
  },
  {
    id: "spring-boot",
    title: "Explain Spring Boot",
    description: "Learn controllers, services, and APIs.",
    icon: Sparkles,
    prompt: "Explain Spring Boot layers for a beginner.",
    response: [
      {
        type: "table",
        headers: ["Layer", "Responsibility"],
        rows: [
          ["Controller", "Receives HTTP requests and returns responses"],
          ["Service", "Contains business rules and use-case logic"],
          ["Repository", "Reads and writes persistent data"],
          ["Entity / DTO", "Represents stored data or API payloads"],
        ],
      },
      {
        type: "paragraph",
        text: "Keeping these responsibilities separate makes the application easier to test, change, and reason about.",
      },
    ],
  },
  {
    id: "database-concepts",
    title: "Explain Database concepts",
    description: "Clarify SQL, keys, and normalization.",
    icon: Database,
    prompt: "Explain SQL joins with a compact example.",
    response: [
      {
        type: "table",
        headers: ["Join", "Returns"],
        rows: [
          ["INNER JOIN", "Only matching rows from both tables"],
          ["LEFT JOIN", "Every left row plus matching right rows"],
          ["RIGHT JOIN", "Every right row plus matching left rows"],
          ["FULL JOIN", "All rows, matched where possible"],
        ],
      },
      {
        type: "code",
        language: "sql",
        code: `SELECT s.name, d.department_name\nFROM Student s\nLEFT JOIN Department d\n  ON s.department_id = d.department_id;`,
      },
    ],
  },
];

export const studyTools: StudyTool[] = [
  {
    id: "notes-summarizer",
    title: "Notes Summarizer",
    description: "Reduce long notes into focused revision points.",
    icon: FileText,
    inputLabel: "Notes or topic",
    inputPlaceholder: "Paste a topic or a paragraph from your notes...",
    actionLabel: "Summarize",
    sampleResult: [
      "Core concepts organized into five revision points.",
      "Important definitions separated from examples.",
      "Two likely exam questions identified.",
    ],
  },
  {
    id: "quiz-generator",
    title: "Quiz Generator",
    description: "Create a quick knowledge check for any topic.",
    icon: CircleHelp,
    inputLabel: "Quiz topic",
    inputPlaceholder: "e.g. Database normalization and functional dependencies",
    actionLabel: "Create quiz",
    sampleResult: [
      "5 mixed-difficulty questions generated.",
      "Answer key and short explanations included.",
      "Estimated completion time: 8 minutes.",
    ],
  },
  {
    id: "flashcard-generator",
    title: "Flashcard Generator",
    description: "Turn definitions into active-recall cards.",
    icon: Layers3,
    inputLabel: "Flashcard topic",
    inputPlaceholder: "e.g. Operating systems scheduling",
    actionLabel: "Create flashcards",
    sampleResult: [
      "12 front-and-back cards prepared.",
      "Definitions, comparisons, and formulas included.",
      "Cards ordered from foundational to advanced.",
    ],
  },
  {
    id: "mcq-generator",
    title: "MCQ Generator",
    description: "Practice exam-style multiple-choice questions.",
    icon: ListChecks,
    inputLabel: "MCQ topic",
    inputPlaceholder: "e.g. Java OOP and exception handling",
    actionLabel: "Generate MCQs",
    sampleResult: [
      "10 exam-style MCQs generated.",
      "Correct choices and explanations included.",
      "Three common misconception questions added.",
    ],
  },
  {
    id: "assignment-helper",
    title: "Assignment Helper",
    description: "Break down requirements into manageable steps.",
    icon: NotebookTabs,
    inputLabel: "Assignment brief",
    inputPlaceholder: "Describe the assignment and its requirements...",
    actionLabel: "Build an outline",
    sampleResult: [
      "Requirements grouped into research, implementation, and review.",
      "A five-step completion outline prepared.",
      "Submission checklist and risk points included.",
    ],
  },
  {
    id: "programming-assistant",
    title: "Programming Assistant",
    description: "Reason through code, errors, and algorithms.",
    icon: Code2,
    inputLabel: "Code or programming problem",
    inputPlaceholder: "Paste code or explain the problem you are solving...",
    actionLabel: "Analyze problem",
    sampleResult: [
      "Problem inputs, outputs, and edge cases identified.",
      "Suggested algorithm explained before implementation.",
      "Time and space complexity included.",
    ],
  },
];

export const aiFeatures: AIFeature[] = [
  {
    title: "24/7 Study Help",
    description: "Work through a difficult concept whenever you get stuck.",
    icon: Clock3,
    tone: "brand",
  },
  {
    title: "Instant Summaries",
    description: "Turn dense material into clear, reviewable points.",
    icon: FileText,
    tone: "emerald",
  },
  {
    title: "Quiz Generator",
    description: "Test recall with questions built around your topic.",
    icon: FileQuestion,
    tone: "amber",
  },
  {
    title: "Personalized Learning",
    description: "Choose the format and depth that helps you learn.",
    icon: BrainCircuit,
    tone: "sky",
  },
];

export const studyHistory: StudyHistoryItem[] = [
  {
    id: "history-dsa",
    title: "Data Structures review",
    detail: "42 minutes · 3 topics",
  },
  {
    id: "history-database",
    title: "Database exam prep",
    detail: "1 hour 18 min · 5 topics",
  },
  {
    id: "history-java",
    title: "Java practice session",
    detail: "36 minutes · 8 questions",
  },
];

export const genericAIResponse = [
  {
    type: "heading" as const,
    text: "Let’s work through it step by step",
  },
  {
    type: "paragraph" as const,
    text: "Start by identifying the core concept, what information is given, and what the question is asking you to produce.",
  },
  {
    type: "list" as const,
    items: [
      "Write the definition in your own words.",
      "Connect it to one small example.",
      "Check the common edge cases or exceptions.",
      "Test yourself without looking at the explanation.",
    ],
  },
  {
    type: "paragraph" as const,
    text: "In a connected AI version, I would adapt this explanation to the exact topic or material you provide.",
  },
];
