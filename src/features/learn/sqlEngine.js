// @ts-nocheck
/**
 * sqlEngine.js
 * ------------
 * A tiny SQL engine written in plain JavaScript — enough to teach the core of
 * querying data without a database server. It supports:
 *
 *   SELECT <*|columns|aggregates> FROM <table>
 *     [WHERE <conditions joined by AND, ops: = != > < >= <= LIKE>]
 *     [GROUP BY <column>]
 *     [ORDER BY <column> [ASC|DESC]]
 *     [LIMIT <n>]
 *
 * Aggregates: COUNT(*), AVG(col), SUM(col), MIN(col), MAX(col).
 * Returns { columns: string[], rows: any[][] } or { error: string }.
 */

export function runSql(query, tables) {
  try {
    const q = String(query || "").trim().replace(/;\s*$/, "");
    if (!q) return { error: "Type a query, e.g. SELECT * FROM students" };

    const re =
      /^select\s+(.+?)\s+from\s+(\w+)(?:\s+where\s+(.+?))?(?:\s+group\s+by\s+(\w+))?(?:\s+order\s+by\s+(\w+)(?:\s+(asc|desc))?)?(?:\s+limit\s+(\d+))?$/is;
    const m = re.exec(q);
    if (!m) {
      return {
        error:
          "Could not parse the query. Supported: SELECT … FROM … [WHERE] [GROUP BY] [ORDER BY] [LIMIT].",
      };
    }
    const [, selectPart, tableName, wherePart, groupBy, orderCol, orderDir, limit] = m;

    const table = tables[tableName.toLowerCase()];
    if (!table) {
      return { error: `Unknown table '${tableName}'. Try: ${Object.keys(tables).join(", ")}.` };
    }

    // Rows as objects for easy access.
    let rows = table.rows.map((r) =>
      Object.fromEntries(table.columns.map((c, i) => [c, r[i]])),
    );

    if (wherePart) rows = rows.filter((row) => evalWhere(row, wherePart, table.columns));

    const items = parseSelect(selectPart, table.columns);
    const hasAgg = items.some((it) => it.type === "agg");

    // ORDER BY on the raw rows (applies to non-grouped projections).
    const sortRows = (list, key) => {
      if (!key) return list;
      const dir = (orderDir || "asc").toLowerCase() === "desc" ? -1 : 1;
      return [...list].sort((a, b) => {
        const av = a[key];
        const bv = b[key];
        if (av === bv) return 0;
        return av > bv ? dir : -dir;
      });
    };

    let outColumns;
    let outRows;

    if (groupBy) {
      // Group rows by the grouping column, then compute each select item per group.
      const groups = new Map();
      for (const row of rows) {
        const key = row[groupBy];
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(row);
      }
      outColumns = items.map((it) => it.label);
      outRows = [...groups.entries()].map(([, groupRows]) =>
        items.map((it) =>
          it.type === "agg" ? aggregate(it, groupRows) : groupRows[0][it.name],
        ),
      );
      if (orderCol) {
        const idx = outColumns.indexOf(orderCol);
        if (idx >= 0) {
          const dir = (orderDir || "asc").toLowerCase() === "desc" ? -1 : 1;
          outRows.sort((a, b) => (a[idx] === b[idx] ? 0 : a[idx] > b[idx] ? dir : -dir));
        }
      }
    } else if (hasAgg) {
      // Aggregate over all matching rows → single summary row.
      outColumns = items.map((it) => it.label);
      outRows = [items.map((it) => (it.type === "agg" ? aggregate(it, rows) : rows[0]?.[it.name]))];
    } else {
      // Plain projection.
      const ordered = sortRows(rows, orderCol);
      if (items.length === 1 && items[0].type === "star") {
        outColumns = table.columns;
        outRows = ordered.map((row) => table.columns.map((c) => row[c]));
      } else {
        outColumns = items.map((it) => it.label);
        outRows = ordered.map((row) => items.map((it) => row[it.name]));
      }
    }

    if (limit) outRows = outRows.slice(0, Number(limit));
    return { columns: outColumns, rows: outRows };
  } catch (err) {
    return { error: String((err && err.message) || err) };
  }
}

function parseSelect(selectPart, columns) {
  return selectPart.split(",").map((raw) => {
    const token = raw.trim();
    if (token === "*") return { type: "star", label: "*" };
    const agg = /^(count|avg|sum|min|max)\s*\(\s*(\*|\w+)\s*\)$/i.exec(token);
    if (agg) {
      const func = agg[1].toUpperCase();
      const arg = agg[2];
      return { type: "agg", func, arg, label: `${func}(${arg})` };
    }
    const col = token.toLowerCase();
    if (!columns.includes(col)) throw new Error(`Unknown column '${token}'.`);
    return { type: "col", name: col, label: col };
  });
}

function aggregate(item, rows) {
  if (item.func === "COUNT") {
    if (item.arg === "*") return rows.length;
    return rows.filter((r) => r[item.arg] != null).length;
  }
  const values = rows.map((r) => Number(r[item.arg])).filter((v) => !Number.isNaN(v));
  if (!values.length) return null;
  if (item.func === "SUM") return round(values.reduce((a, b) => a + b, 0));
  if (item.func === "AVG") return round(values.reduce((a, b) => a + b, 0) / values.length);
  if (item.func === "MIN") return Math.min(...values);
  if (item.func === "MAX") return Math.max(...values);
  return null;
}

function round(n) {
  return Math.round(n * 100) / 100;
}

function evalWhere(row, wherePart, columns) {
  return wherePart
    .split(/\s+and\s+/i)
    .every((clause) => evalCondition(row, clause.trim(), columns));
}

function evalCondition(row, clause, columns) {
  const m = /^(\w+)\s*(>=|<=|!=|=|>|<|like)\s*(.+)$/i.exec(clause);
  if (!m) throw new Error(`Bad condition: '${clause}'.`);
  const [, colRaw, opRaw, valueRaw] = m;
  const col = colRaw.toLowerCase();
  if (!columns.includes(col)) throw new Error(`Unknown column '${colRaw}'.`);
  const op = opRaw.toLowerCase();
  const left = row[col];
  const right = parseValue(valueRaw.trim());

  if (op === "like") {
    const pattern = new RegExp(
      "^" + String(right).replace(/[.+^${}()|[\]\\]/g, "\\$&").replace(/%/g, ".*").replace(/_/g, ".") + "$",
      "i",
    );
    return pattern.test(String(left));
  }
  if (op === "=") return left == right;
  if (op === "!=") return left != right;
  if (op === ">") return left > right;
  if (op === "<") return left < right;
  if (op === ">=") return left >= right;
  if (op === "<=") return left <= right;
  return false;
}

function parseValue(raw) {
  const quoted = /^'(.*)'$/.exec(raw) || /^"(.*)"$/.exec(raw);
  if (quoted) return quoted[1];
  if (/^-?\d+(\.\d+)?$/.test(raw)) return Number(raw);
  return raw;
}
