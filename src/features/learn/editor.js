// @ts-nocheck
/**
 * editor.js
 * ---------
 * Small, framework-free helpers that make a plain <textarea> behave like a
 * real code editor: pressing Tab inserts spaces instead of moving focus, and
 * pressing Enter keeps the current indentation. Vanilla JavaScript + the DOM
 * Selection API — no dependencies.
 */

const INDENT = "  "; // two spaces

/**
 * Handles a keydown event on a code textarea. Call it from React's onKeyDown
 * (or addEventListener). Returns the new value when it changed something, or
 * null when the key was left for the browser to handle.
 *
 * @param {{ key: string, preventDefault: () => void, target: any }} event
 * @returns {string | null}
 */
export function handleEditorKeydown(event) {
  const field = event.target;
  const { selectionStart: start, selectionEnd: end, value } = field;

  if (event.key === "Tab") {
    event.preventDefault();
    const next = value.slice(0, start) + INDENT + value.slice(end);
    queueCaret(field, start + INDENT.length);
    return next;
  }

  if (event.key === "Enter") {
    const lineStart = value.lastIndexOf("\n", start - 1) + 1;
    const currentLine = value.slice(lineStart, start);
    const leading = (currentLine.match(/^\s*/) || [""])[0];
    // Add an extra indent level after an opening bracket.
    const extra = /[{([]\s*$/.test(currentLine) ? INDENT : "";
    const insertion = "\n" + leading + extra;
    event.preventDefault();
    const next = value.slice(0, start) + insertion + value.slice(end);
    queueCaret(field, start + insertion.length);
    return next;
  }

  return null;
}

/** Restores the caret position after React re-renders the textarea value. */
function queueCaret(field, position) {
  requestAnimationFrame(() => {
    try {
      field.selectionStart = position;
      field.selectionEnd = position;
    } catch (err) {
      void err;
    }
  });
}

/** Counts lines so the editor can render a gutter. */
export function countLines(value) {
  return value ? value.split("\n").length : 1;
}
