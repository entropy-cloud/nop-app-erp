#!/usr/bin/env node
// parse-nop-errors.mjs
//
// Parse a Nop platform server log, extract & dedupe structured errors,
// print a summary table. Implements Lesson 05 step 4-5 (backward log search
// + structured-error extraction) as a reusable tool.
//
// Nop error lines look like:
//   ... NopException[seq=3,errorCode=nop.err.xui.grid.col-not-prop,
//       params={gridId=list, colId=slaPolicyId},
//       desc=表格[list]的列[slaPolicyId]不是已定义的实体属性]
//       @_loc=[11:18:0:0]/erp/cs/pages/ErpCsTicketType/ErpCsTicketType.view.xml
//
// Usage:
//   node parse-nop-errors.mjs <logfile>                 # by frequency (most broken first)
//   node parse-nop-errors.mjs <logfile> --recent        # newest-first (what just happened)
//   node parse-nop-errors.mjs <logfile> --grep col-not-prop
//   node parse-nop-errors.mjs <logfile> -n 10           # limit rows
//   tail -n 5000 <logfile> | node parse-nop-errors.mjs  # via stdin
//   node parse-nop-errors.mjs <logfile> --full          # full paths + full desc

import { readFileSync } from 'node:fs';

function parseArgs(argv) {
  const opts = { recent: false, full: false, grep: null, limit: 0, file: null };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--recent' || a === '-r') opts.recent = true;
    else if (a === '--full') opts.full = true;
    else if (a === '--grep') opts.grep = argv[++i];
    else if (a === '-n') opts.limit = parseInt(argv[++i], 10) || 0;
    else if (!a.startsWith('-')) opts.file = a;
  }
  return opts;
}

function readInput(file) {
  if (file) return readFileSync(file, 'utf8');
  try {
    return readFileSync(0, 'utf8'); // stdin
  } catch {
    process.stderr.write('usage: node parse-nop-errors.mjs <logfile> [--recent] [--grep PAT] [-n N] [--full]\n');
    process.exit(2);
  }
}

function extractErrors(text) {
  const events = [];
  const lines = text.split('\n');
  for (const line of lines) {
    if (!line.includes('errorCode=')) continue;
    const ts = (line.match(/^(\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}[,.]\d{3})/) || [])[1] || '';
    const errorCode = (line.match(/errorCode=([\w.-]+)/) || [])[1];
    if (!errorCode || errorCode === 'null') continue;

    const locMatch = line.match(/@_loc=\[(\d+)(:\d+)*[^\]]*\]\s*(\S+)/);
    const locLine = locMatch ? locMatch[1] : '';
    let locPath = locMatch ? locMatch[3].replace(/[:,]\s*$/, '') : '';

    // Prefer the inner desc; fall back to msg= (graphql end-request line).
    let desc =
      (line.match(/desc=([\s\S]*?)\]\s*@_loc/) || [])[1] ||
      (line.match(/\bmsg=([^,}]+)/) || [])[1] ||
      '';
    desc = desc.trim().replace(/\s+/g, ' ');

    const params = (line.match(/params=\{([^}]*)\}/) || [])[1] || '';
    events.push({ ts, errorCode, desc, locLine, locPath, params });
  }
  return events;
}

function summarize(events) {
  const map = new Map();
  for (const e of events) {
    const key = `${e.errorCode}|${e.locPath}|${e.locLine}`;
    const ex = map.get(key);
    if (ex) {
      ex.count++;
      if (e.ts && e.ts > ex.lastTs) ex.lastTs = e.ts;
    } else {
      map.set(key, { ...e, count: 1, lastTs: e.ts });
    }
  }
  return [...map.values()];
}

function shorten(path, full) {
  if (full || !path) return path || '(no @_loc)';
  const segs = path.split('/').filter(Boolean);
  return segs.slice(-2).join('/');
}

function truncate(s, n) {
  if (!s) return '';
  return s.length <= n ? s : s.slice(0, n - 1) + '…';
}

function printTable(rows, opts) {
  if (!rows.length) {
    process.stdout.write('No errors with errorCode= found.\n');
    return;
  }
  const descW = opts.full ? 60 : 40;
  const data = rows.map((r) => ({
    count: String(r.count),
    errorCode: r.errorCode,
    location: r.locLine ? `${shorten(r.locPath, opts.full)}:${r.locLine}` : shorten(r.locPath, opts.full),
    desc: truncate(r.desc, descW),
    params: r.params,
  }));

  const w = {
    count: Math.max(5, ...data.map((d) => d.count.length)),
    errorCode: Math.max(9, ...data.map((d) => d.errorCode.length)),
    location: Math.max(8, ...data.map((d) => d.location.length)),
    desc: descW,
  };

  const header =
    `${'count'.padEnd(w.count)}  ${'errorCode'.padEnd(w.errorCode)}  ${'location'.padEnd(w.location)}  desc`;
  process.stdout.write(header + '\n');
  process.stdout.write('-'.repeat(Math.min(header.length, 160)) + '\n');
  for (const d of data) {
    process.stdout.write(
      `${d.count.padEnd(w.count)}  ${d.errorCode.padEnd(w.errorCode)}  ${d.location.padEnd(w.location)}  ${d.desc}\n`,
    );
    if (opts.full && d.params) process.stdout.write(`${' '.repeat(w.count + w.errorCode + w.location + 4)}params: ${d.params}\n`);
  }
}

function main() {
  const opts = parseArgs(process.argv);
  const text = readInput(opts.file);
  let rows = summarize(extractErrors(text));

  if (opts.grep) {
    const re = new RegExp(opts.grep, 'i');
    rows = rows.filter((r) => re.test(`${r.errorCode} ${r.desc} ${r.locPath} ${r.params}`));
  }

  if (opts.recent) rows.sort((a, b) => (b.lastTs || '').localeCompare(a.lastTs || ''));
  else rows.sort((a, b) => b.count - a.count);

  if (opts.limit > 0) rows = rows.slice(0, opts.limit);

  const source = opts.file || 'stdin';
  process.stdout.write(`\nNop error summary  (source: ${source}, ${rows.length} unique)\n\n`);
  printTable(rows, opts);
  process.stdout.write('\n');
}

main();
