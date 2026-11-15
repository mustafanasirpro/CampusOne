// @ts-nocheck
/**
 * challenges.js
 * -------------
 * Code-challenge exercises — "write a function to pass the tests". Plain
 * JavaScript data. Each challenge names the function the learner must write,
 * gives starter code, and lists test cases (arguments → expected result) that
 * challengeRunner.js checks automatically.
 */

export const challenges = {
  javascript: [
    {
      id: "ch-pass",
      title: "Pass or fail",
      prompt:
        "Write a function isPass(marks) that returns true when marks are 50 or more, otherwise false.",
      fn: "isPass",
      starter: "function isPass(marks) {\n  // your code here\n}\n",
      cases: [
        { args: [50], expected: true },
        { args: [49], expected: false },
        { args: [90], expected: true },
        { args: [0], expected: false },
      ],
    },
    {
      id: "ch-sum",
      title: "Sum an array",
      prompt:
        "Write a function total(numbers) that returns the sum of every number in the array. An empty array should return 0.",
      fn: "total",
      starter: "function total(numbers) {\n  // your code here\n}\n",
      cases: [
        { args: [[1, 2, 3]], expected: 6 },
        { args: [[]], expected: 0 },
        { args: [[10, -5, 5]], expected: 10 },
      ],
    },
    {
      id: "ch-average",
      title: "Average (GPA)",
      prompt:
        "Write a function average(grades) that returns the average of the grades as a number rounded to 2 decimals. Hint: Number(x.toFixed(2)).",
      fn: "average",
      starter: "function average(grades) {\n  // your code here\n}\n",
      cases: [
        { args: [[4, 3, 2]], expected: 3 },
        { args: [[4, 3.7, 3.3, 4, 3]], expected: 3.6 },
      ],
    },
  ],
};

export function getChallenges(trackId) {
  return challenges[trackId] || [];
}
