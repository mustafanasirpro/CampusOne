// @ts-nocheck
/**
 * sandboxRunner.js
 * ----------------
 * The engine behind CampusOne Learn's "Try it Yourself" editor — written in
 * plain JavaScript. It takes the HTML / CSS / JS a learner types and runs it
 * safely inside a sandboxed <iframe>, streaming any console output back to the
 * page. This is the same idea W3Schools uses for its live code examples.
 *
 * Nothing here is React-specific; it is vanilla DOM + the postMessage API, so
 * it can be read and reused on its own.
 */

/** A random token so we only trust console messages from our own sandbox. */
export const SANDBOX_TOKEN = "campusone-learn-sandbox";

/**
 * Builds a complete HTML document string from the three code panes and injects
 * a tiny bridge that forwards console.log/info/warn/error (and uncaught errors)
 * to the parent window via postMessage.
 *
 * @param {{ html?: string, css?: string, js?: string }} parts
 * @returns {string} a full HTML document usable as an iframe `srcdoc`
 */
export function composeDocument(parts) {
  const html = parts.html || "";
  const css = parts.css || "";
  const js = parts.js || "";

  const bridge = `
    (function () {
      var send = function (level, args) {
        try {
          parent.postMessage({
            source: "${SANDBOX_TOKEN}",
            level: level,
            text: args.map(format).join(" ")
          }, "*");
        } catch (e) {}
      };
      var format = function (value) {
        if (typeof value === "string") return value;
        try { return JSON.stringify(value); } catch (e) { return String(value); }
      };
      ["log", "info", "warn", "error"].forEach(function (level) {
        var original = console[level] ? console[level].bind(console) : function () {};
        console[level] = function () {
          send(level, Array.prototype.slice.call(arguments));
          original.apply(console, arguments);
        };
      });
      window.addEventListener("error", function (event) {
        send("error", [event.message + " (line " + event.lineno + ")"]);
      });
      window.addEventListener("unhandledrejection", function (event) {
        send("error", ["Unhandled promise rejection: " + format(event.reason)]);
      });
    })();
  `;

  return [
    "<!doctype html>",
    '<html lang="en">',
    "<head>",
    '<meta charset="utf-8" />',
    '<meta name="viewport" content="width=device-width, initial-scale=1" />',
    "<style>",
    "body{font-family:system-ui,-apple-system,Segoe UI,sans-serif;margin:16px;color:#222;}",
    css,
    "</style>",
    "</head>",
    "<body>",
    html,
    "<script>" + bridge + "<\/script>",
    "<script>\ntry {\n" + js + "\n} catch (err) {\n  console.error(err && err.message ? err.message : String(err));\n}\n<\/script>",
    "</body>",
    "</html>",
  ].join("\n");
}

/**
 * Renders the composed document into a sandboxed iframe.
 * @param {HTMLIFrameElement | null} iframe
 * @param {{ html?: string, css?: string, js?: string }} parts
 */
export function runInIframe(iframe, parts) {
  if (!iframe) return;
  iframe.setAttribute("sandbox", "allow-scripts allow-modals");
  iframe.srcdoc = composeDocument(parts);
}

/**
 * Subscribes to console output coming back from the sandbox.
 * @param {(entry: { level: string, text: string }) => void} onEntry
 * @returns {() => void} an unsubscribe function
 */
export function listenForConsole(onEntry) {
  const handler = (event) => {
    const data = event && event.data;
    if (!data || data.source !== SANDBOX_TOKEN) return;
    onEntry({ level: data.level, text: data.text });
  };
  window.addEventListener("message", handler);
  return () => window.removeEventListener("message", handler);
}
