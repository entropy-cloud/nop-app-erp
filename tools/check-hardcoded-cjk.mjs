#!/usr/bin/env node
// check-hardcoded-cjk.mjs — ai-check-r3 M0.2 deterministic CJK hardcode detector.
//
// Owner doc : docs/architecture/i18n-compliance.md (M0.1, sole authority for CAT judgments)
// Baseline  : docs/audits/cjk-baseline.md (SNAPSHOT machine-readable block + WHITELIST registry)
// Paradigm  : docs/audits/i18n-coverage-checker.sh (F15) + docs/audits/nop-compliance-checker.sh (pure reporter / external gate)
//
// Scans main-runtime surfaces ONLY:
//   - Java: paths containing /src/main/java/  (excludes /target/ /_gen/ /_dump/ /src/test/ /node_modules/ /.git/ /_tmp/)
//   - Yaml: *.page.yaml / *.flux.yaml under /src/main/resources/
//
// Five categories (per i18n-compliance.md 判定准绳表 + 修复模式对照表):
//   CAT-1  LOG 语句中文 (LOG.trace/debug/info/warn/error statement carrying CJK string literal)
//   CAT-2  异常路径中文参数 (throw statement -> ';' carrying CJK string literal + .param(...) exact caliber)
//   CAT-3  运行时字符串中文 (return "中文" / String constants / setter defaults / concatenation / @Description("中文") ...
//          file-level whitelist is the ONLY exemption channel, registered in cjk-baseline.md WHITELIST block)
//   CAT-4  页面 yaml 中文无 i18nEn (non-comment CJK scalar lines without sibling i18nEn coverage)
//   CAT-5  注释 CJK (exempt — counted for statistics only, never a violation)
//   Compliant (skipped): ErrorCode.define("key", "中文描述", ...) — judgment #1, zh-CN is the source language.
//
// Usage:
//   node tools/check-hardcoded-cjk.mjs                 # report mode (pure reporter, always exit 0)
//   node tools/check-hardcoded-cjk.mjs --baseline      # regenerate SNAPSHOT block in docs/audits/cjk-baseline.md
//   node tools/check-hardcoded-cjk.mjs --baseline --out <file>   # additionally write the same SNAPSHOT block to <file>
//   node tools/check-hardcoded-cjk.mjs --strict        # gate: any per-file per-CAT count above SNAPSHOT => exit 1
//   node tools/check-hardcoded-cjk.mjs --self-test     # in-memory anti-fake-green self-verification (exit 0/1)
//
// Exit codes: 0 = clean/pass, 1 = new violations (strict) or self-test failure.

import { readFile, writeFile } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';
import { execFile } from 'child_process';
import { promisify } from 'util';

const execFileAsync = promisify(execFile);

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');

const BASELINE_FILE = 'docs/audits/cjk-baseline.md';
const SNAPSHOT_HEADING = '## SNAPSHOT (machine-readable)';
const WHITELIST_HEADING = '## WHITELIST (machine-readable)';

const EXCLUDED_SEGMENTS = ['/target/', '/_gen/', '/_dump/', '/node_modules/', '/.git/', '/_tmp/'];

const CJK_RE = /[\u4e00-\u9fff\u3400-\u4dbf]/;
const CJK_GLOBAL_RE = /[\u4e00-\u9fff\u3400-\u4dbf]/g;

const LOG_CALL_RE = /\bLOG\s*\.\s*(trace|debug|info|warn|error)\s*\(/;
const INLINE_LOGGER_RE = /getLogger\s*\([^)]*\)\s*\.\s*(trace|debug|info|warn|error)\s*\(/;
const THROW_RE = /^throw\b/;
const PARAM_CALL_RE = /\.param\s*\(/;
const ERROR_CODE_DEFINE_RE = /\bErrorCode\s*\.\s*define\s*\(/;

const CATS = ['CAT1', 'CAT2', 'CAT3', 'CAT4'];
const CAT_LABELS = {
  CAT1: 'CAT-1 LOG 语句中文',
  CAT2: 'CAT-2 异常路径中文参数',
  CAT3: 'CAT-3 运行时字符串中文',
  CAT4: 'CAT-4 页面 yaml 中文无 i18nEn',
  CAT5: 'CAT-5 注释 CJK（豁免，仅统计）',
};

function isExcluded(relPath) {
  return EXCLUDED_SEGMENTS.some(s => relPath.includes(s));
}

function domainOf(relPath) {
  const m = relPath.match(/^module-([a-z0-9-]+)\//);
  if (m) return m[1];
  if (relPath.startsWith('app-erp-all/')) return 'app-erp-all';
  return 'other';
}

async function listTargetFiles() {
  const { stdout } = await execFileAsync('git', ['ls-files', '-co', '--exclude-standard'], {
    cwd: rootDir,
    maxBuffer: 64 * 1024 * 1024,
  });
  const files = stdout.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
  const java = [];
  const yaml = [];
  for (const f of files) {
    if (isExcluded(f)) continue;
    if (f.includes('/src/main/java/') && f.endsWith('.java')) java.push(f);
    else if (f.includes('/src/main/resources/') && (f.endsWith('.page.yaml') || f.endsWith('.flux.yaml'))) yaml.push(f);
  }
  return { java, yaml };
}

// ---------------------------------------------------------------------------
// Java analysis
// ---------------------------------------------------------------------------

/**
 * Blank out comments while preserving line structure. Keeps string literal
 * contents intact (they are the detection target). Returns:
 *   codeText     — comment-blanked source (same length/line count as input)
 *   commentLines — [{ line, text }] for CAT-5 statistics
 */
function stripComments(content) {
  const n = content.length;
  let out = '';
  const commentLines = [];
  let line = 1;
  let commentBuf = '';
  let inComment = false; // current char consumed as comment
  let i = 0;
  const flushCommentLine = () => {
    if (commentBuf && CJK_RE.test(commentBuf)) commentLines.push({ line, text: commentBuf });
    commentBuf = '';
  };
  while (i < n) {
    const c = content[i];
    const c2 = content.substr(i, 2);
    inComment = false;
    if (c2 === '//') {
      inComment = true;
      while (i < n && content[i] !== '\n') { commentBuf += content[i]; out += ' '; i++; }
    } else if (c2 === '/*') {
      inComment = true;
      commentBuf += c2; out += '  '; i += 2;
      while (i < n && content.substr(i, 2) !== '*/') {
        if (content[i] === '\n') { flushCommentLine(); out += '\n'; line++; }
        else { commentBuf += content[i]; out += ' '; }
        i++;
      }
      if (i < n) { commentBuf += '*/'; out += '  '; i += 2; }
    } else if (c === '"') {
      const tq = content.substr(i, 3) === '"""';
      if (tq) {
        out += '"""'; i += 3;
        while (i < n && content.substr(i, 3) !== '"""') {
          if (content[i] === '\\') { out += content.substr(i, 2); i += 2; continue; }
          if (content[i] === '\n') { out += '\n'; line++; }
          else out += content[i];
          i++;
        }
        if (i < n) { out += '"""'; i += 3; }
      } else {
        out += '"'; i++;
        while (i < n && content[i] !== '"') {
          if (content[i] === '\\') { out += content.substr(i, 2); i += 2; continue; }
          if (content[i] === '\n') break; // unterminated single-line string: stop at EOL
          out += content[i];
          i++;
        }
        if (i < n && content[i] === '"') { out += '"'; i++; }
      }
    } else if (c === "'") {
      out += "'"; i++;
      while (i < n && content[i] !== "'") {
        if (content[i] === '\\') { out += content.substr(i, 2); i += 2; continue; }
        if (content[i] === '\n') break;
        out += content[i];
        i++;
      }
      if (i < n && content[i] === "'") { out += "'"; i++; }
    } else {
      out += c;
      if (c === '\n') { flushCommentLine(); line++; }
      i++;
    }
  }
  if (commentBuf) flushCommentLine();
  return { codeText: out, commentLines };
}

/**
 * Split comment-blanked code into statement segments.
 * Line-based with cumulative paren/bracket depth inside a segment:
 *   - segment closes at a line whose trimmed text ends with ';' / '{' / '}' (depth 0)
 *   - annotation-led segments close when depth returns to 0 on a line ending with ')'
 *   - braces always terminate a segment
 */
function splitStatements(codeText) {
  const lines = codeText.split('\n');
  const segments = [];
  let buf = [];
  let startLine = 1;
  let paren = 0, bracket = 0;
  const count = (s, ch) => {
    let k = 0;
    for (const c of s) if (c === ch) k++;
    return k;
  };
  const stripStringsForDepth = (s) => s.replace(/"(?:[^"\\\n]|\\.)*"/g, '""').replace(/'(?:[^'\\\n]|\\.)*'/g, "''");
  for (let idx = 0; idx < lines.length; idx++) {
    const raw = lines[idx];
    const lineNo = idx + 1;
    const clean = stripStringsForDepth(raw);
    if (buf.length === 0) {
      const t = raw.trim();
      if (t === '' || t === '{' || t === '}' || t.startsWith('}')) {
        // structurally empty / brace-only line: never starts a statement
        if (t !== '') segments.push({ text: t, line: lineNo });
        startLine = lineNo + 1;
        continue;
      }
      startLine = lineNo;
    }
    buf.push(raw);
    paren += count(clean, '(') - count(clean, ')');
    bracket += count(clean, '[') - count(clean, ']');
    const trimmed = raw.trim();
    const depthZero = paren <= 0 && bracket <= 0;
    const isAnnotationLed = buf[0].trim().startsWith('@');
    const closes =
      (depthZero && /(?:;|\{|\})$/.test(trimmed)) ||
      (depthZero && isAnnotationLed && /\)$/.test(trimmed));
    if (closes) {
      segments.push({ text: buf.join('\n'), line: startLine });
      buf = [];
      startLine = lineNo + 1;
      paren = 0; bracket = 0;
    }
  }
  if (buf.length) segments.push({ text: buf.join('\n'), line: startLine });
  return segments;
}

/** Extract string/char literals (with absolute line numbers) from a segment. */
function extractLiterals(segmentText, startLine) {
  const literals = [];
  let i = 0;
  let line = startLine;
  const n = segmentText.length;
  while (i < n) {
    const c = segmentText[i];
    if (c === '"') {
      const tq = segmentText.substr(i, 3) === '"""';
      if (tq) {
        const startLineNo = line;
        let val = '';
        i += 3;
        while (i < n && segmentText.substr(i, 3) !== '"""') {
          if (segmentText[i] === '\\') { val += segmentText.substr(i, 2); i += 2; continue; }
          if (segmentText[i] === '\n') line++;
          val += segmentText[i];
          i++;
        }
        i += 3;
        literals.push({ value: val, line: startLineNo });
        continue;
      }
      const startLineNo = line;
      let val = '';
      i++;
      while (i < n && segmentText[i] !== '"') {
        if (segmentText[i] === '\\') { val += segmentText.substr(i, 2); i += 2; continue; }
        if (segmentText[i] === '\n') break;
        val += segmentText[i];
        i++;
      }
      i++;
      literals.push({ value: val, line: startLineNo });
    } else if (c === "'") {
      const startLineNo = line;
      let val = '';
      i++;
      while (i < n && segmentText[i] !== "'") {
        if (segmentText[i] === '\\') { val += segmentText.substr(i, 2); i += 2; continue; }
        if (segmentText[i] === '\n') break;
        val += segmentText[i];
        i++;
      }
      i++;
      literals.push({ value: val, line: startLineNo });
    } else {
      if (c === '\n') line++;
      i++;
    }
  }
  return literals;
}

function scanJavaFile(relPath, content) {
  const { codeText, commentLines } = stripComments(content);
  const segments = splitStatements(codeText);
  const sites = []; // { cat, line, text }
  for (const seg of segments) {
    const literals = extractLiterals(seg.text, seg.line);
    const cjkLiterals = literals.filter(l => CJK_RE.test(l.value));
    if (!cjkLiterals.length) continue;
    const single = seg.text.replace(/\s+/g, ' ');
    if (LOG_CALL_RE.test(single) || INLINE_LOGGER_RE.test(single)) {
      sites.push({ cat: 'CAT1', line: cjkLiterals[0].line, text: single });
    } else if (THROW_RE.test(seg.text.trim()) || PARAM_CALL_RE.test(single)) {
      sites.push({ cat: 'CAT2', line: cjkLiterals[0].line, text: single });
    } else if (ERROR_CODE_DEFINE_RE.test(single)) {
      // compliant per i18n-compliance.md 判定准绳表 #1 (zh-CN source language)
    } else {
      sites.push({ cat: 'CAT3', line: cjkLiterals[0].line, text: single });
    }
  }
  const cat5 = commentLines.length;
  return { sites, cat5 };
}

// ---------------------------------------------------------------------------
// Yaml analysis (*.page.yaml / *.flux.yaml)
// ---------------------------------------------------------------------------

const YAML_KEY_SCALAR_RE = /^(\s*)(-\s+)?([A-Za-z_][\w.-]*\s*:\s*)(.*)$/;

function scanYamlFile(relPath, content) {
  const lines = content.split('\n');
  const sites = [];
  let cat5 = 0;
  const indentOf = (s) => s.match(/^ */)[0].length;
  for (let i = 0; i < lines.length; i++) {
    const raw = lines[i];
    const trimmed = raw.trim();
    if (trimmed === '') continue;
    if (trimmed.startsWith('#')) {
      if (CJK_RE.test(trimmed)) cat5++;
      continue;
    }
    if (!CJK_RE.test(raw)) continue;
    const m = raw.match(YAML_KEY_SCALAR_RE);
    if (m && m[4] && CJK_RE.test(m[4])) {
      // `key: 中文value` — covered only by an `i18nEn: <non-CJK>` sibling of the SAME
      // mapping node: same indent, no intervening same-indent key (stop at indent < target
      // or at another key at target indent)
      const indent = m[1].length;
      let covered = false;
      for (let j = i + 1; j < Math.min(i + 30, lines.length); j++) {
        const t = lines[j].trim();
        if (t === '' || t.startsWith('#')) continue;
        const jIndent = indentOf(lines[j]);
        if (jIndent < indent) break;
        if (jIndent === indent) {
          if (!t.startsWith('-')) {
            const en = t.match(/^i18nEn\s*:\s*(.*)$/);
            if (en) {
              const v = en[1].replace(/^["']|["']$/g, '').trim();
              if (v && !CJK_RE.test(v)) { covered = true; break; }
            }
            break; // another same-indent key: left this node's entry range
          }
        }
      }
      if (!covered) sites.push({ cat: 'CAT4', line: i + 1, text: trimmed });
    } else if (!m) {
      // plain content line (block scalar body / `- 中文` list item / bare CJK text)
      sites.push({ cat: 'CAT4', line: i + 1, text: trimmed });
    } else {
      // key-only line (`key:`) — CJK must live in the key itself (rare) or nested block below
      const indent = m[1].length;
      let hasBlockContent = false;
      for (let j = i + 1; j < lines.length; j++) {
        const t = lines[j].trim();
        if (t === '') continue;
        if (indentOf(lines[j]) <= indent) break;
        hasBlockContent = true;
        break;
      }
      if (!hasBlockContent && CJK_RE.test(m[3])) {
        sites.push({ cat: 'CAT4', line: i + 1, text: trimmed });
      }
    }
  }
  return { sites, cat5 };
}

// ---------------------------------------------------------------------------
// Whitelist (file-level exemption; only channel per i18n-compliance.md)
// ---------------------------------------------------------------------------

async function loadWhitelist() {
  const full = path.join(rootDir, BASELINE_FILE);
  let content;
  try {
    content = await readFile(full, 'utf8');
  } catch {
    return [];
  }
  const start = content.indexOf(WHITELIST_HEADING);
  if (start === -1) return [];
  const next = content.indexOf('\n## ', start + WHITELIST_HEADING.length);
  const block = next === -1 ? content.slice(start) : content.slice(start, next);
  const entries = [];
  let cur = null;
  for (const rawLine of block.split('\n')) {
    const fm = rawLine.match(/^\s*-\s+file:\s*(.+)$/);
    if (fm) {
      if (cur) entries.push(cur);
      cur = { file: fm[1].trim(), cats: new Set(CATS) };
      continue;
    }
    if (cur) {
      const cm = rawLine.match(/^\s*cats:\s*\[([^\]]*)\]/);
      if (cm) {
        const listed = cm[1].split(',').map(s => s.trim().toUpperCase()).filter(Boolean)
          .map(s => (s.startsWith('CAT') ? s : `CAT${s}`));
        if (listed.length) cur.cats = new Set(listed);
      }
    }
  }
  if (cur) entries.push(cur);
  return entries;
}

function applyWhitelist(scanResult, whitelist) {
  const byFile = new Map(whitelist.map(w => [w.file, w]));
  const filter = (file, sites) => {
    const w = byFile.get(file);
    if (!w) return sites;
    return sites.filter(s => !w.cats.has(s.cat));
  };
  for (const file of Object.keys(scanResult.java)) {
    scanResult.java[file].sites = filter(file, scanResult.java[file].sites);
  }
  for (const file of Object.keys(scanResult.yaml)) {
    scanResult.yaml[file].sites = filter(file, scanResult.yaml[file].sites);
  }
}

// ---------------------------------------------------------------------------
// Full scan
// ---------------------------------------------------------------------------

async function scanAll() {
  const { java, yaml } = await listTargetFiles();
  const result = { java: {}, yaml: {} };
  for (const f of java) {
    const content = await readFile(path.join(rootDir, f), 'utf8');
    result.java[f] = scanJavaFile(f, content);
  }
  for (const f of yaml) {
    const content = await readFile(path.join(rootDir, f), 'utf8');
    result.yaml[f] = scanYamlFile(f, content);
  }
  return { files: { java, yaml }, result };
}

function totalsOf(result) {
  const totals = { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 0, CAT5: 0 };
  const domains = {};
  const files = {};
  const siteLists = { CAT1: [], CAT2: [], CAT3: [], CAT4: [] };
  const addDomain = (d) => (domains[d] ||= { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 0 });
  for (const [file, r] of Object.entries(result.java)) {
    const d = addDomain(domainOf(file));
    for (const s of r.sites) {
      totals[s.cat]++;
      d[s.cat]++;
      siteLists[s.cat].push({ file, line: s.line, text: s.text });
    }
    totals.CAT5 += r.cat5;
    if (r.sites.length) {
      const fc = (files[file] ||= {});
      for (const s of r.sites) fc[s.cat] = (fc[s.cat] || 0) + 1;
    }
  }
  for (const [file, r] of Object.entries(result.yaml)) {
    const d = addDomain(domainOf(file));
    for (const s of r.sites) {
      totals[s.cat]++;
      d[s.cat]++;
      siteLists[s.cat].push({ file, line: s.line, text: s.text });
    }
    totals.CAT5 += r.cat5;
    if (r.sites.length) {
      const fc = (files[file] ||= {});
      for (const s of r.sites) fc[s.cat] = (fc[s.cat] || 0) + 1;
    }
  }
  return { totals, domains, files, siteLists };
}

// ---------------------------------------------------------------------------
// Snapshot serialization / parsing
// ---------------------------------------------------------------------------

function renderSnapshotYaml(totals, domains, files, scanned, generatedAt) {
  const lines = [];
  lines.push(`generated: ${generatedAt}`);
  lines.push(`scanned: { javaFiles: ${scanned.java}, yamlFiles: ${scanned.yaml} }`);
  lines.push(`totals: { CAT1: ${totals.CAT1}, CAT2: ${totals.CAT2}, CAT3: ${totals.CAT3}, CAT4: ${totals.CAT4} }`);
  lines.push('domains:');
  for (const d of Object.keys(domains).sort()) {
    const v = domains[d];
    lines.push(`  ${d}: { CAT1: ${v.CAT1}, CAT2: ${v.CAT2}, CAT3: ${v.CAT3}, CAT4: ${v.CAT4} }`);
  }
  lines.push('files:');
  for (const f of Object.keys(files).sort()) {
    const v = files[f];
    const parts = CATS.filter(c => v[c]).map(c => `${c}: ${v[c]}`);
    lines.push(`  ${f}: { ${parts.join(', ')} }`);
  }
  return lines.join('\n');
}

function parseSnapshotYaml(block) {
  const files = {};
  const totals = { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 0 };
  const scanned = { javaFiles: 0, yamlFiles: 0 };
  let inFiles = false;
  for (const rawLine of block.split('\n')) {
    const line = rawLine.replace(/\t/g, '  ');
    const tm = line.match(/^totals:\s*\{(.*)\}/);
    if (tm) {
      for (const kv of tm[1].split(',')) {
        const [k, v] = kv.split(':').map(s => s.trim());
        if (k in totals) totals[k] = parseInt(v, 10) || 0;
      }
      continue;
    }
    const sm = line.match(/^scanned:\s*\{(.*)\}/);
    if (sm) {
      for (const kv of sm[1].split(',')) {
        const [k, v] = kv.split(':').map(s => s.trim());
        if (k in scanned) scanned[k] = parseInt(v, 10) || 0;
      }
      continue;
    }
    if (/^files:\s*$/.test(line)) { inFiles = true; continue; }
    if (inFiles) {
      const fm = line.match(/^  (\S+):\s*\{(.*)\}\s*$/);
      if (fm && fm[1] !== 'generated') {
        const fc = {};
        for (const kv of fm[2].split(',')) {
          const [k, v] = kv.split(':').map(s => s.trim());
          if (k) fc[k] = parseInt(v, 10) || 0;
        }
        files[fm[1]] = fc;
      }
    }
  }
  return { files, totals, scanned };
}

function extractSnapshotBlock(content) {
  const start = content.indexOf(SNAPSHOT_HEADING);
  if (start === -1) return null;
  const fenceOpen = content.indexOf('```yaml', start);
  if (fenceOpen === -1) return null;
  const bodyStart = content.indexOf('\n', fenceOpen) + 1;
  const fenceClose = content.indexOf('```', bodyStart);
  if (fenceClose === -1) return null;
  return { body: content.slice(bodyStart, fenceClose), fullStart: start, fenceOpen, fenceClose };
}

// ---------------------------------------------------------------------------
// Modes
// ---------------------------------------------------------------------------

function printReport(scan, totals, domains, siteLists, scanned) {
  console.log('='.repeat(72));
  console.log('check-hardcoded-cjk  (ai-check-r3 M0.2 — owner doc: docs/architecture/i18n-compliance.md)');
  console.log('='.repeat(72));
  console.log(`scanned java files : ${scanned.java}  (main only; excluded target/_gen/test/dump)`);
  console.log(`scanned yaml files : ${scanned.yaml}  (*.page.yaml / *.flux.yaml, main resources)`);
  console.log('-'.repeat(72));
  for (const cat of CATS) {
    const filesHit = new Set([
      ...Object.entries(scan.result.java).filter(([, r]) => r.sites.some(s => s.cat === cat)).map(([f]) => f),
      ...Object.entries(scan.result.yaml).filter(([, r]) => r.sites.some(s => s.cat === cat)).map(([f]) => f),
    ]);
    console.log(`${CAT_LABELS[cat].padEnd(28)} : ${String(totals[cat]).padStart(5)} sites in ${filesHit.size} files`);
  }
  console.log(`${CAT_LABELS.CAT5.padEnd(28)} : ${String(totals.CAT5).padStart(5)} lines (exempt, informational)`);
  console.log('-'.repeat(72));
  console.log('per-domain violations (CAT-1..CAT-4):');
  for (const d of Object.keys(domains).sort()) {
    const v = domains[d];
    const sum = v.CAT1 + v.CAT2 + v.CAT3 + v.CAT4;
    if (!sum) continue;
    console.log(`  ${d.padEnd(16)} CAT1=${String(v.CAT1).padStart(4)} CAT2=${String(v.CAT2).padStart(4)} CAT3=${String(v.CAT3).padStart(4)} CAT4=${String(v.CAT4).padStart(4)} total=${sum}`);
  }
  console.log('-'.repeat(72));
  for (const cat of CATS) {
    const list = siteLists[cat];
    if (!list.length) continue;
    console.log(`${CAT_LABELS[cat]} — top sites (showing up to 12 of ${list.length}):`);
    for (const s of list.slice(0, 12)) {
      console.log(`  ${s.file}:${s.line} :: ${s.text.slice(0, 110)}`);
    }
  }
  console.log('='.repeat(72));
}

async function main() {
  const args = process.argv.slice(2);
  const mode = {
    baseline: args.includes('--baseline'),
    strict: args.includes('--strict'),
    selfTest: args.includes('--self-test'),
    report: args.length === 0,
  };
  const outIdx = args.indexOf('--out');
  const outPath = outIdx !== -1 ? args[outIdx + 1] : null;

  if (mode.selfTest) {
    process.exit(runSelfTest());
  }

  const whitelist = await loadWhitelist();
  const scan = await scanAll();
  applyWhitelist(scan.result, whitelist);
  const { totals, domains, files, siteLists } = totalsOf(scan.result);
  const scanned = { java: scan.files.java.length, yaml: scan.files.yaml.length };

  if (mode.baseline) {
    const generatedAt = new Date().toISOString();
    const snapshotBody = renderSnapshotYaml(totals, domains, files, scanned, generatedAt);
    const full = path.join(rootDir, BASELINE_FILE);
    let doc;
    try {
      doc = await readFile(full, 'utf8');
    } catch {
      console.error(`[check-hardcoded-cjk] baseline file missing: ${BASELINE_FILE}`);
      process.exit(1);
    }
    const existing = extractSnapshotBlock(doc);
    const newSection = `${SNAPSHOT_HEADING}\n\n> 本块由 \`node tools/check-hardcoded-cjk.mjs --baseline\` 自动整体重生成（勿手改）。\n> 门控方向：单向收紧（actual 只降不升）；调高须独立计划裁决 + per-site 证据（对齐 docs/audits/compliance-baseline.md 范式）。\n> \`--strict\` 解析本块做 per-file per-CAT 计数比对：任何文件任一 CAT 计数高于快照 = 新增违规 = 非零退出。\n\n\`\`\`yaml\n${snapshotBody}\n\`\`\`\n`;
    let updated;
    if (existing) {
      updated = doc.slice(0, existing.fullStart) + newSection + doc.slice(existing.fenceClose + 3);
    } else {
      updated = doc.replace(/\s*$/, '\n') + '\n' + newSection;
    }
    await writeFile(full, updated, 'utf8');
    console.log(`[check-hardcoded-cjk] snapshot written to ${BASELINE_FILE} (generated ${generatedAt})`);
    if (outPath) {
      const outAbs = path.resolve(rootDir, outPath);
      const outContent = `${SNAPSHOT_HEADING}\n\n> Dual-write copy of docs/audits/cjk-baseline.md SNAPSHOT block (identical body; generated ${generatedAt}).\n\n\`\`\`yaml\n${snapshotBody}\n\`\`\`\n`;
      await writeFile(outAbs, outContent, 'utf8');
      console.log(`[check-hardcoded-cjk] snapshot dual-written to ${outPath}`);
    }
    printReport(scan, totals, domains, siteLists, scanned);
    console.log('RESULT: baseline snapshot written (report mode exit 0)');
    process.exit(0);
  }

  printReport(scan, totals, domains, siteLists, scanned);

  if (mode.strict) {
    const full = path.join(rootDir, BASELINE_FILE);
    let doc;
    try {
      doc = await readFile(full, 'utf8');
    } catch {
      console.error(`[check-hardcoded-cjk] --strict requires baseline file: ${BASELINE_FILE}`);
      process.exit(1);
    }
    const block = extractSnapshotBlock(doc);
    if (!block) {
      console.error('[check-hardcoded-cjk] --strict: SNAPSHOT block not found; run --baseline first');
      process.exit(1);
    }
    const baseline = parseSnapshotYaml(block.body);
    const newViolations = [];
    const improvements = [];
    for (const [file, fc] of Object.entries(files)) {
      const bc = baseline.files[file] || {};
      for (const cat of CATS) {
        const actual = fc[cat] || 0;
        const expected = bc[cat] || 0;
        if (actual > expected) {
          newViolations.push(`${file} :: ${cat} actual=${actual} > baseline=${expected} (+${actual - expected})`);
        } else if (actual < expected) {
          improvements.push(`${file} :: ${cat} actual=${actual} < baseline=${expected} (-${expected - actual})`);
        }
      }
    }
    for (const [file, bc] of Object.entries(baseline.files)) {
      if (!files[file]) {
        const removed = CATS.some(c => bc[c]);
        if (removed) improvements.push(`${file} :: removed from tree (was ${JSON.stringify(bc)})`);
      }
    }
    if (improvements.length) {
      console.log(`IMPROVEMENTS (baseline may be tightened via --baseline): ${improvements.length} file-CAT entries below snapshot`);
      for (const line of improvements.slice(0, 20)) console.log(`  ${line}`);
    }
    if (newViolations.length) {
      console.error(`RESULT: FAIL (--strict: ${newViolations.length} new violation(s) above frozen snapshot)`);
      for (const line of newViolations.slice(0, 50)) console.error(`  NEW ${line}`);
      if (newViolations.length > 50) console.error(`  ... (${newViolations.length - 50} more)`);
      process.exit(1);
    }
    console.log(`RESULT: PASS (--strict: 0 new violations vs frozen snapshot; ${Object.keys(baseline.files).length} baseline files, totals CAT1..4=${baseline.totals.CAT1}/${baseline.totals.CAT2}/${baseline.totals.CAT3}/${baseline.totals.CAT4})`);
    process.exit(0);
  }

  console.log(`RESULT: report mode (pure reporter, exit 0; use --strict to gate against ${BASELINE_FILE})`);
  process.exit(0);
}

// ---------------------------------------------------------------------------
// Anti-fake-green self-test (in-memory injection; must never touch the repo tree)
// ---------------------------------------------------------------------------

export function runSelfTest() {
  const checks = [];
  const expect = (name, cond) => checks.push({ name, ok: !!cond });

  // CAT-1 injection must be caught
  const cat1Src = [
    'package x;',
    'class InjectProbe {',
    '  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(InjectProbe.class);',
    '  void m(int x) {',
    '    // 中文注释应豁免（CAT-5）',
    '    LOG.warn("注入测试：CAT-1 样本 {}", x);',
    '  }',
    '}',
  ].join('\n');
  const cat1 = scanJavaFile('__inject__/InjectProbe.java', cat1Src);
  expect('CAT-1 injected LOG sample is caught', cat1.sites.length === 1 && cat1.sites[0].cat === 'CAT1');
  expect('CAT-5 comment line counted but exempt', cat1.cat5 === 1 && !cat1.sites.some(s => s.cat === 'CAT5'));

  // multi-line LOG statement
  const cat1b = scanJavaFile('__inject__/InjectProbe2.java', [
    'class P2 {',
    '  void m(int x) {',
    '    LOG.warn("多行注入：第一行 {}",',
    '             x);',
    '  }',
    '}',
  ].join('\n'));
  expect('CAT-1 multi-line LOG statement caught once', cat1b.sites.length === 1 && cat1b.sites[0].cat === 'CAT1');

  // CAT-2: throw -> ';' with CJK literal + .param exact caliber
  const cat2 = scanJavaFile('__inject__/InjectProbe3.java', [
    'class P3 {',
    '  void m(String status) {',
    '    throw new NopException(ERR).param(ARG_STATUS, status).param(ARG_EXPECTED, "非已作废");',
    '  }',
    '  void n() {',
    '    NopException e = new NopException(ERR_X);',
    '    e.param(ARG_A, "中文散文参数");',
    '    throw e;',
    '  }',
    '}',
  ].join('\n'));
  expect('CAT-2 throw statement with CJK param caught', cat2.sites.filter(s => s.cat === 'CAT2').length === 2);

  // compliant ErrorCode.define must NOT be CAT-3
  const compliant = scanJavaFile('__inject__/InjectProbe4.java', [
    'interface E4 {',
    '  ErrorCode ERR = ErrorCode.define("erp.err.x", "中文描述 {arg}", ARG);',
    '}',
  ].join('\n'));
  expect('ErrorCode.define zh description is compliant (0 sites)', compliant.sites.length === 0);

  // CAT-3 runtime strings
  const cat3 = scanJavaFile('__inject__/InjectProbe5.java', [
    'class P5 {',
    '  String m() {',
    '    if (x) {',
    '      return "中文返回值";',
    '    }',
    '    return EN_OK;',
    '  }',
    '}',
  ].join('\n'));
  expect('CAT-3 return "中文" caught', cat3.sites.length === 1 && cat3.sites[0].cat === 'CAT3');

  // CAT-4 yaml: bare CJK label caught; i18nEn-covered label exempt; comments exempt
  const yaml = scanYamlFile('__inject__/probe.page.yaml', [
    '# 中文注释豁免',
    'type: page',
    'title: 注入页面标题',
    'body:',
    '- type: button',
    '  label: "注入按钮"',
    '  i18nEn: "Injected Button"',
  ].join('\n'));
  expect('CAT-4 bare zh title caught', yaml.sites.some(s => s.line === 3 && s.cat === 'CAT4'));
  expect('CAT-4 i18nEn-covered label exempt', !yaml.sites.some(s => s.line === 6));
  expect('yaml comment line exempt (CAT-5 only)', yaml.cat5 === 1);

  // --strict delta math: baseline missing the injected file must flag it
  const fakeBaseline = { files: {}, totals: { CAT1: 0, CAT2: 0, CAT3: 0, CAT4: 0 } };
  const actualFiles = { '__inject__/InjectProbe.java': { CAT1: 1 } };
  let flagged = false;
  for (const [file, fc] of Object.entries(actualFiles)) {
    const bc = fakeBaseline.files[file] || {};
    for (const cat of CATS) {
      if ((fc[cat] || 0) > (bc[cat] || 0)) flagged = true;
    }
  }
  expect('strict compare flags injected delta as new violation', flagged);

  let pass = true;
  console.log('[check-hardcoded-cjk] anti-fake-green self-test:');
  for (const c of checks) {
    console.log(`  ${c.ok ? 'PASS' : 'FAIL'}  ${c.name}`);
    if (!c.ok) pass = false;
  }
  console.log(pass ? 'RESULT: PASS (self-test green)' : 'RESULT: FAIL (self-test detected fake-green risk)');
  return pass ? 0 : 1;
}

main().catch((error) => {
  console.error('[check-hardcoded-cjk] Error:', error);
  process.exit(1);
});
