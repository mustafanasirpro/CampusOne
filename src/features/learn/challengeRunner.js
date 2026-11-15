// @ts-nocheck
/**
 * challengeRunner.js
 * ------------------
 * Runs a learner's solution against a challenge's test cases, inside a
 * sandboxed iframe, and reports the per-case pass/fail back to the page. Plain
 * JavaScript + postMessage. The parent correlates a result to the right
 * challenge card by checking event.source against that card's iframe window.
 */

import { SANDBOX_TOKEN } from "./sandboxRunner.js";

/**
 * Builds the test document (learner code + generated assertions) and renders it
 * into the iframe. Results arrive via a "message" event (kind: "challenge").
 *
 * @param {HTMLIFrameElement | null} iframe
 * @param {string} userCode the learner's code
 * @param {{ fn: string, cases: Array<{ args: any[], expected: any }> }} spec
 */
export function runChallenge(iframe, userCode, spec) {
  if (!iframe) return;

  const harness = `
    (function () {
      var cases = ${JSON.stringify(spec.cases)};
      var fn = window[${JSON.stringify(spec.fn)}];
      var hasFn = typeof fn === "function";
      var results = cases.map(function (c) {
        var actual, pass = false, errored = false;
        try {
          actual = hasFn ? fn.apply(null, c.args) : undefined;
          pass = JSON.stringify(actual) === JSON.stringify(c.expected);
        } catch (e) {
          errored = true;
          actual = String((e && e.message) || e);
        }
        return {
          name: ${JSON.stringify(spec.fn)} + "(" +
            c.args.map(function (a) { return JSON.stringify(a); }).join(", ") + ")",
          pass: pass,
          expected: c.expected,
          actual: actual,
          errored: errored
        };
      });
      parent.postMessage({
        source: "${SANDBOX_TOKEN}",
        kind: "challenge",
        hasFn: hasFn,
        passed: hasFn && results.every(function (r) { return r.pass; }),
        results: results
      }, "*");
    })();
  `;

  const doc = [
    "<!doctype html><html><head><meta charset='utf-8'></head><body>",
    "<script>",
    "try {\n" + userCode + "\n} catch (e) {\n" +
      "parent.postMessage({ source: '" + SANDBOX_TOKEN + "', kind: 'challenge', hasFn: false, passed: false, error: String((e && e.message) || e), results: [] }, '*');\n}",
    "<\/script>",
    "<script>" + harness + "<\/script>",
    "</body></html>",
  ].join("");

  iframe.setAttribute("sandbox", "allow-scripts");
  iframe.srcdoc = doc;
}

/**
 * Subscribes to challenge results from a specific iframe.
 * @param {HTMLIFrameElement} iframe the card's iframe
 * @param {(result: { passed: boolean, hasFn: boolean, results: any[], error?: string }) => void} onResult
 * @returns {() => void} unsubscribe
 */
export function listenForChallenge(iframe, onResult) {
  const handler = (event) => {
    const data = event && event.data;
    if (!data || data.source !== SANDBOX_TOKEN || data.kind !== "challenge") return;
    if (iframe && event.source !== iframe.contentWindow) return;
    onResult(data);
  };
  window.addEventListener("message", handler);
  return () => window.removeEventListener("message", handler);
}
