#!/usr/bin/env node
// check-xdsl-invariants.mjs — GATE-01 XDSL structural invariant checker for handwritten view.xml.
//
// Owner doc : docs/architecture/flux-page-export-and-validation.md（XDSL 结构不变量静态门禁节）
// Source    : docs/backlog/perf-ux-debt-consolidation-roadmap.md GATE-01
// Plan      : docs/plans/2026-09-28-1710-1-gate01-xdsl-invariants.md
// Paradigm  : tools/check-hardcoded-cjk.mjs (lesson 20: full scan / self-test / fail-loud / injection replay)
//
// Scans handwritten view.xml ONLY:
//   git ls-files -co --exclude-standard ∩ *.view.xml ∩ /src/main/resources/
//   excluding _gen paths, /target/, and files whose basename starts with "_".
//
// Five invariants (zero-tolerance; any hit = defect — HEAD was verified clean at freeze):
//   I1  <form> element must be a direct child of <forms>            (batch-5 R-1: 16 files form-into-pages corruption)
//   I2  <form id="edit"> / <form id="add"> at most once per file    (whitespace tolerant: <form\s+id="…")
//   I3  no duplicate <cell id> inside the same <form>               (batch-5 F1: 6 files duplicate cell id; cross-form reuse legal)
//   I4  no @[a-zA-Z]+\[\[ expression double-bracket corruption      (CDATA/comment-stripped text)
//   I5  no [=>^]@ group-header mislabel on layout group lines       (line-anchored ^\s*=+ … [=>^]@; avoids <url>@query: false positives)
//
// Usage:
//   node tools/check-xdsl-invariants.mjs                 # gate: 0 violations => exit 0, else exit 1
//   node tools/check-xdsl-invariants.mjs --self-test     # in-memory anti-fake-green self-verification (exit 0/1)
//
// Exit codes: 0 = clean/pass, 1 = violations or self-test failure, 2 = operational failure (enum/IO).

import { readFile } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';
import { execFile } from 'child_process';
import { promisify } from 'util';

const execFileAsync = promisify(execFile);

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');

// ---------------------------------------------------------------------------
// Text preparation (structure- and line-preserving)
// ---------------------------------------------------------------------------

/** Blank out XML comments with spaces, preserving newlines (structure scan input). */
function stripComments(content) {
  let out = '';
  let i = 0;
  const n = content.length;
  while (i < n) {
    if (content.startsWith('<!--', i)) {
      const end = content.indexOf('-->', i + 4);
      const stop = end === -1 ? n : end + 3;
      for (; i < stop; i++) out += content[i] === '\n' ? '\n' : ' ';
    } else {
      out += content[i++];
    }
  }
  return out;
}

/** Blank out CDATA bodies AND comments with spaces, preserving newlines (I4/I5 text-scan input). */
function stripCdataAndComments(content) {
  let out = '';
  let i = 0;
  const n = content.length;
  while (i < n) {
    if (content.startsWith('<![CDATA[', i)) {
      const end = content.indexOf(']]>', i + 9);
      const stop = end === -1 ? n : end + 3;
      for (; i < stop; i++) out += content[i] === '\n' ? '\n' : ' ';
    } else if (content.startsWith('<!--', i)) {
      const end = content.indexOf('-->', i + 4);
      const stop = end === -1 ? n : end + 3;
      for (; i < stop; i++) out += content[i] === '\n' ? '\n' : ' ';
    } else {
      out += content[i++];
    }
  }
  return out;
}

function lineOf(text, index, lineStarts) {
  let lo = 0;
  let hi = lineStarts.length - 1;
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1;
    if (lineStarts[mid] <= index) lo = mid;
    else hi = mid - 1;
  }
  return lo + 1;
}

function computeLineStarts(text) {
  const starts = [0];
  for (let i = 0; i < text.length; i++) {
    if (text[i] === '\n') starts.push(i + 1);
  }
  return starts;
}

// ---------------------------------------------------------------------------
// Invariant scanners
// ---------------------------------------------------------------------------

/**
 * Structural scan (I1/I2/I3) over comment-stripped text.
 * Stack tracks ONLY forms/form/cell boundaries (Decision-4); every other tag
 * (including x:-prefixed, c:script blocks, self-closing non-tracked tags) is
 * transparent.
 */
function scanStructure(content) {
  const sites = [];
  const lineStarts = computeLineStarts(content);
  const tagRe = /<(\/?)(forms|form|cell)\b[^>]*?(\/?)>/g;
  const stack = [];
  let curFormCells = null;
  let curFormLine = 0;
  let m;
  while ((m = tagRe.exec(content))) {
    const closing = m[1] === '/';
    const tag = m[2];
    const selfClosing = m[3] === '/';
    const line = lineOf(content, m.index, lineStarts);

    const idMatch = m[0].match(/\bid\s*=\s*"([^"]*)"/);

    if (closing) {
      if (stack[stack.length - 1] === tag) stack.pop();
      if (tag === 'form') curFormCells = null;
      continue;
    }
    if (tag === 'form') {
      const parent = stack.length ? stack[stack.length - 1] : null;
      if (parent !== 'forms') {
        sites.push({ inv: 'I1', line, excerpt: m[0].slice(0, 100) });
      }
      if (!selfClosing) {
        stack.push(tag);
        curFormCells = new Set();
        curFormLine = line;
      }
      // I2 counts self-closing forms too
      continue;
    }
    if (tag === 'cell' && curFormCells && idMatch) {
      if (curFormCells.has(idMatch[1])) {
        sites.push({ inv: 'I3', line, excerpt: `dup cell id="${idMatch[1]}" (form@${curFormLine})` });
      } else {
        curFormCells.add(idMatch[1]);
      }
      if (!selfClosing) stack.push(tag);
      continue;
    }
    if (!selfClosing) stack.push(tag);
  }

  // I2: form id counting (whitespace tolerant, attribute value may follow any whitespace incl. newline)
  const formIds = { edit: [], add: [] };
  const formReTolerant = /<form\s[^>]*>/g;
  let fm;
  while ((fm = formReTolerant.exec(content))) {
    const tagText = fm[0];
    const idm = tagText.match(/\bid\s*=\s*"([^"]*)"/);
    if (idm && (idm[1] === 'edit' || idm[1] === 'add')) {
      formIds[idm[1]].push(lineOf(content, fm.index, lineStarts));
    }
  }
  for (const id of ['edit', 'add']) {
    if (formIds[id].length > 1) {
      sites.push({ inv: 'I2', line: formIds[id][1], excerpt: `<form id="${id}"> declared ${formIds[id].length} times (lines ${formIds[id].join(',')})` });
    }
  }
  return sites;
}

/** I4/I5 over CDATA+comment-stripped text. */
function scanText(content) {
  const sites = [];
  const lines = content.split('\n');
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const i4 = line.match(/@[a-zA-Z]+\[\[/);
    if (i4) {
      sites.push({ inv: 'I4', line: i + 1, excerpt: line.trim().slice(0, 100) });
      continue;
    }
    if (/^[ \t]*=+[=>^]@/.test(line)) {
      // group-header mislabel: the marker char (>/^/=) immediately followed by @
      // instead of the group id — tighter than a whole-line [=>^]@ search, which
      // would false-positive on same-line <url>@query: link text after the header
      sites.push({ inv: 'I5', line: i + 1, excerpt: line.trim().slice(0, 100) });
    }
  }
  return sites;
}

function scanFile(content) {
  const commentStripped = stripComments(content);
  const sites = [
    ...scanStructure(commentStripped),
    ...scanText(stripCdataAndComments(content)),
  ];
  sites.sort((a, b) => a.line - b.line || a.inv.localeCompare(b.inv));
  return sites;
}

// ---------------------------------------------------------------------------
// Enumeration
// ---------------------------------------------------------------------------

async function listTargetFiles() {
  const { stdout } = await execFileAsync('git', ['ls-files', '-co', '--exclude-standard'], {
    cwd: rootDir,
    maxBuffer: 64 * 1024 * 1024,
  });
  const files = stdout.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
  return files.filter(f =>
    f.endsWith('.view.xml')
    && f.includes('/src/main/resources/')
    && !f.includes('_gen')
    && !f.includes('/target/')
    && !path.basename(f).startsWith('_'));
}

// ---------------------------------------------------------------------------
// Self-test (in-memory injection; never touches the repo tree)
// ---------------------------------------------------------------------------

export function runSelfTest() {
  const checks = [];
  const expect = (name, cond) => checks.push({ name, ok: !!cond });
  const invsOf = (content) => scanFile(content).map(s => `${s.inv}@${s.line}`);

  // --- I1: form outside <forms> must be caught; proper nesting passes
  const i1bad = '<view><pages><form id="edit"><layout>a[甲]</layout></form></pages></view>';
  expect('I1 form under <pages> caught', invsOf(i1bad).some(s => s.startsWith('I1@')));
  const i1ok = '<view><forms><form id="edit"><layout>a[甲]</layout></form></forms></view>';
  expect('I1 proper forms nesting clean', !invsOf(i1ok).some(s => s.startsWith('I1')));

  // --- I2: duplicate edit/add (single and double space) caught; view forms unconstrained
  const i2bad = '<view><forms><form id="add" size="lg"/><form  id="add"/></forms></view>';
  expect('I2 duplicate add (double-space variant) caught', invsOf(i2bad).some(s => s.startsWith('I2@')));
  const i2bad2 = '<view><forms><form id="edit"/><form  id="edit"/></forms></view>';
  expect('I2 duplicate edit (double-space variant) caught', invsOf(i2bad2).some(s => s.startsWith('I2@')));
  const i2ok = '<view><forms><form id="add"/><form id="edit"/><form id="view"/><form  id="view"/></forms></view>';
  expect('I2 add+edit once and repeated view forms clean', !invsOf(i2ok).some(s => s.startsWith('I2')));

  // --- I3: duplicate cell id within one form (incl. self-closing) caught; cross-form reuse legal
  const i3bad = '<view><forms><form id="edit"><cells><cell id="x"/><cell id="x"/></cells></form></forms></view>';
  expect('I3 duplicate cell id in one form caught', invsOf(i3bad).some(s => s.startsWith('I3@')));
  const i3ok = '<view><forms><form id="edit"><cells><cell id="x"/></cells></form><form id="add"><cells><cell id="x"/></cells></form></forms></view>';
  expect('I3 same cell id across forms clean', !invsOf(i3ok).some(s => s.startsWith('I3')));

  // --- I4: @[a-zA-Z]+[[ corruption caught; CDATA script with [[ or arrows exempt
  const i4bad = '<view><forms><form id="view"><cells><cell id="a" label="@code[[x]]"/></cells></form></forms></view>';
  expect('I4 @code[[ corruption caught', invsOf(i4bad).some(s => s.startsWith('I4@')));
  const i4ok = '<view><grids><grid id="list"><gen-control><c:script><![CDATA[const m = list.map(v => ({v})); if (a) { x = arr[[0]]; }]]></c:script></gen-control></grid></grids></view>';
  expect('I4/I5 CDATA script content (arrows, [[) exempt', !invsOf(i4ok).some(s => s.startsWith('I4') || s.startsWith('I5')));

  // --- I5: group-header mislabel caught; legitimate group headers and @query/@i18n link text clean
  const i5bad = '<view><forms><form id="view"><layout>\n=========>@baseInfo[基本信息]======\n code[单号]\n==========^@audit[审计信息]=========\n</layout></form></forms></view>';
  expect('I5 group-header =>@ and ^@ mislabels caught', invsOf(i5bad).filter(s => s.startsWith('I5@')).length === 2);
  const i5ok = '<view><forms><form id="view"><layout>\n=========>baseInfo[基本信息]======\n code[单号]\n==========^audit[审计信息]=========\n</layout>'
    + '<buttons><button><url>@query:ErpX__findList/{@listSelection}?filter_parentId=__null&amp;filter_id__ne=$id</url><confirmText>@i18n:confirm</confirmText></button></buttons></form></forms></view>';
  expect('I5 legit group headers + <url>@query/<confirmText>@i18n clean', !invsOf(i5ok).some(s => s.startsWith('I5')));
  const i5glued = '<view><forms><form id="view"><layout>==========^audit[审计信息]=========</layout><buttons><button><url>@query:X/{@s}</url></button></buttons></form></forms></view>';
  expect('I5 marker-anchored: same-line trailing @query text after header clean', !invsOf(i5glued).some(s => s.startsWith('I5')));

  // --- structural robustness: c:script wrappers + self-closing cells + XML comment containing tags
  const robust = '<view><!-- <forms><form id="edit"/></forms> --><grids><grid id="list"><c:script><![CDATA[x]]></c:script></grid></grids>'
    + '<forms><form id="view"><cells><cell id="a" /><cell id="b"/></cells></form></forms></view>';
  expect('commented-out tags ignored; c:script/self-closing cells transparent', invsOf(robust).length === 0);

  // --- composite corrupted fixture: all five invariants at once
  const all = '<view><pages><form id="add"/><form  id="add"/></pages>';
  const allSites = scanFile(all);
  expect('composite: I1+I2 both detected in one corrupted file',
    allSites.some(s => s.inv === 'I1') && allSites.some(s => s.inv === 'I2'));

  let pass = true;
  console.log('[check-xdsl-invariants] anti-fake-green self-test:');
  for (const c of checks) {
    console.log(`  ${c.ok ? 'PASS' : 'FAIL'}  ${c.name}`);
    if (!c.ok) pass = false;
  }
  console.log(pass ? 'RESULT: PASS (self-test green)' : 'RESULT: FAIL (self-test detected fake-green risk)');
  return pass ? 0 : 1;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  const args = process.argv.slice(2);
  if (args.includes('--self-test')) {
    process.exit(runSelfTest());
  }

  let files;
  try {
    files = await listTargetFiles();
  } catch (error) {
    console.error('[check-xdsl-invariants] enumeration failed:', error.message);
    process.exit(2);
  }

  const violations = [];
  let scanned = 0;
  for (const f of files) {
    let content;
    try {
      content = await readFile(path.join(rootDir, f), 'utf8');
    } catch (error) {
      console.error(`[check-xdsl-invariants] read failed: ${f} (${error.message})`);
      process.exit(2);
    }
    scanned++;
    for (const s of scanFile(content)) {
      violations.push({ file: f, ...s });
    }
  }

  console.log('='.repeat(72));
  console.log('check-xdsl-invariants  (GATE-01 — owner doc: docs/architecture/flux-page-export-and-validation.md)');
  console.log('='.repeat(72));
  console.log(`scanned handwritten view.xml : ${scanned}`);
  for (const inv of ['I1', 'I2', 'I3', 'I4', 'I5']) {
    const n = violations.filter(v => v.inv === inv).length;
    console.log(`  ${inv} : ${n} violation(s)`);
  }
  if (violations.length) {
    console.log('-'.repeat(72));
    for (const v of violations) {
      console.log(`  ${v.file}:${v.line} :: ${v.inv} :: ${v.excerpt}`);
    }
    console.log('='.repeat(72));
    console.log(`RESULT: FAIL (${violations.length} structural violation(s); zero-tolerance gate, no baseline)`);
    process.exit(1);
  }
  console.log('RESULT: PASS (0 structural violations across handwritten view.xml)');
  process.exit(0);
}

main().catch((error) => {
  console.error('[check-xdsl-invariants] Error:', error);
  process.exit(2);
});
