#!/usr/bin/env node
// check-plan-gates.mjs — GATE-02 plan gate-state checker (falsification-resistant closure bookkeeping).
//
// Owner doc : docs/plans/00-plan-authoring-and-execution-guide.md rules 10/11/12
// Source    : docs/backlog/perf-ux-debt-consolidation-roadmap.md GATE-02
// Plan      : docs/plans/2026-09-28-1720-1-gate02-plan-gates.md
// Paradigm  : tools/check-hardcoded-cjk.mjs + docs/audits/cjk-baseline.md (lesson 20: snapshot /
//             one-way tightening / self-test / injection replay)
//
// Scans docs/plans/*.md (top level, git ls-files -co incl. untracked; excludes README.md / 00-*).
//
// Checks:
//   C1 completed (alias: done) plans carry zero unchecked "- [ ]" lines (fence-aware)
//   C2 completed plans: the "## Closure" section exists and its "Closure Audit Evidence:" block
//      holds non-empty substantive content (any line shape — three real evidence formats pass:
//      label pair / Auditor line + continuation / Auditor + Iteration lines). Placeholder =
//      CLOSED signature enum only. Known limitation (registered): "self-audit self-acknowledged"
//      evidence is not mechanically detectable — frozen in the SNAPSHOT triage inventory.
//   C3 active (alias: in_progress) plans: closure-audit gate line ticked while evidence is
//      placeholder = pre-tick contradiction (lesson 25). Gate-line matcher is substring-based.
//   C4 any plan: a "### Phase" section marked Status: completed (literal only — done-alias
//      files are reclaimed by C1) containing unchecked lines inside that section.
//   C5 plan-shaped file (has "## Closure Gates" or "## Draft Review Record") with no parseable
//      status header = parse failure. Pure status-less non-plan docs are listed informationally.
//
// Modes:
//   (default)  report all findings; exit 0
//   --strict   actual <= SNAPSHOT per file per class; new violations => exit 1; improvements listed
//   --baseline regenerate docs/audits/plan-gates-baseline.md SNAPSHOT block (never hand-edit;
//              mistaken edits: `git checkout` restore; IMPROVEMENTS never auto-rewrites it)
//   --self-test in-memory fault injection (exit 0/1)
//
// Exit codes: 0 = report/strict-pass/self-test-pass; 1 = strict new violations or self-test fail;
//             2 = operational failure (unknown status token, IO, unparseable baseline).

import { readFile, writeFile } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';
import { execFile } from 'child_process';
import { promisify } from 'util';

const execFileAsync = promisify(execFile);

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');

const BASELINE_FILE = 'docs/audits/plan-gates-baseline.md';
const SNAPSHOT_HEADING = '## SNAPSHOT (machine-readable)';
const CLASSES = ['C1', 'C2', 'C3', 'C4', 'C5'];

// Closed placeholder-signature enum (B-1): a line inside the evidence block is a placeholder
// fragment iff it matches one of these. Anything else non-empty is substantive.
const PLACEHOLDER_LINE_PATTERNS = [
  /^（待[^）]*）$/u,          // （待独立结束审计） / （待审计落盘） / （待结束审计）
  /^<[^<>]*>$/,              // <independent auditor or independent subagent>
  /^[-\s]*<[^<>]*>.*$/,      // "- Evidence: <task id / log link / walkthrough record>"
  /^待[^ ]*$/,                // 待独立结束审计 / 待审计落盘 (bare)
  /^（待回填[^）]*）$/u,
];
const PLACEHOLDER_SUBSTRINGS = [
  '待独立结束审计',
  '待审计落盘',
  '待结束审计',
  '<closure 时填写>',
  '<independent auditor',
  '<task id / log link',
];

function isPlaceholderFragment(line) {
  const t = line.trim();
  if (!t) return true;
  if (PLACEHOLDER_LINE_PATTERNS.some(re => re.test(t))) return true;
  return PLACEHOLDER_SUBSTRINGS.some(s => t.includes(s));
}

// ---------------------------------------------------------------------------
// parsing
// ---------------------------------------------------------------------------

/** Blank fenced code blocks (``` ... ```) preserving line structure. */
function blankFences(content) {
  const lines = content.split('\n');
  let inFence = false;
  return lines.map(line => {
    const trimmed = line.trimStart();
    if (trimmed.startsWith('```')) {
      inFence = !inFence;
      return '';
    }
    return inFence ? '' : line;
  });
}

/** First-wins status parse: "> Plan Status: <token>" with bold-tolerant label, search-style token. */
function parseStatus(lines) {
  for (const line of lines) {
    const m = line.match(/^\s*>\s*\*{0,2}Plan Status\*{0,2}\s*:\s*(.*)$/);
    if (m) {
      const rest = m[1];
      const tok = rest.match(/[a-z_-]+/); // search-style: skips whitespace, '*', CJK, （...）
      if (tok) return { status: tok[0], line: rest };
      return { status: null, unknown: rest.trim().slice(0, 40) };
    }
  }
  return { status: undefined };
}

function aliasOf(token) {
  if (token === 'done') return 'completed';
  if (token === 'in_progress' || token === 'in-progress') return 'active';
  return token;
}

/** Split into top-level "## " sections: [{title, start, end}] over the fence-blanked lines. */
function topLevelSections(lines) {
  const sections = [];
  let cur = null;
  lines.forEach((l, i) => {
    const m = l.match(/^##\s+(.*)$/);
    if (m) {
      if (cur) cur.end = i - 1;
      cur = { title: m[1].trim(), start: i, end: lines.length - 1 };
      sections.push(cur);
    }
  });
  return sections;
}

/** "### " phase sections with their Status: line and unchecked lines. A phase section ends at
 *  ANY heading (## or ###) — Closure Gates items must not be attributed to the last phase. */
function phaseSections(lines) {
  const sections = [];
  let cur = null;
  lines.forEach((l, i) => {
    const m = l.match(/^###\s+(.*)$/);
    if (m) {
      if (cur) cur.end = i - 1;
      cur = { title: m[1].trim(), start: i, end: lines.length - 1, status: null, unchecked: [] };
      sections.push(cur);
    } else if (cur) {
      if (/^##\s+/.test(l)) { cur.end = i - 1; cur = null; return; }
      const sm = l.match(/^\s*Status:\s*(.+?)\s*$/);
      if (sm && cur.status === null) cur.status = sm[1];
      if (/^\s*- \[ \]/.test(l)) cur.unchecked.push({ line: i + 1, text: l.trim().slice(0, 90) });
    }
  });
  return sections;
}

// ---------------------------------------------------------------------------
// per-file scan
// ---------------------------------------------------------------------------

function scanPlan(content) {
  const lines = blankFences(content);
  const parsed = parseStatus(lines);
  const result = { lines: lines.length, violations: [], status: parsed.status, unknown: parsed.unknown, planShaped: false };

  const sections = topLevelSections(lines);
  const has = title => sections.some(s => s.title === title);
  result.planShaped = has('Closure Gates') || has('Draft Review Record');

  // status resolution
  let status;
  if (parsed.status === undefined) {
    status = undefined; // no status header
  } else if (parsed.status === null) {
    result.unknownStatus = parsed.unknown;
    status = null;
  } else {
    status = aliasOf(parsed.status);
    const KNOWN = ['completed', 'active', 'draft', 'superseded', 'replaced', 'deferred', 'cancelled'];
    if (!KNOWN.includes(status)) {
      result.unknownStatus = parsed.status;
      status = null;
    }
  }
  result.resolvedStatus = status;

  const uncheckedAll = [];
  lines.forEach((l, i) => { if (/^\s*- \[ \]/.test(l)) uncheckedAll.push({ line: i + 1, text: l.trim().slice(0, 90) }); });

  const isCompleted = status === 'completed';
  const isActive = status === 'active';

  // C1
  if (isCompleted && uncheckedAll.length) {
    for (const u of uncheckedAll) result.violations.push({ cls: 'C1', line: u.line, text: u.text });
  }

  // C2 (completed) / evidence-block extraction (shared with C3)
  const closureSec = sections.find(s => s.title === 'Closure');
  let evidencePlaceholder = null; // tri-state: null = n/a, true/false
  let evidenceMissing = false;
  if (closureSec) {
    const body = lines.slice(closureSec.start + 1, closureSec.end + 1);
    const evIdx = body.findIndex(l => /^\s*Closure Audit Evidence\s*:\s*$/.test(l.trim()) || /^\s*Closure Audit Evidence\s*:/.test(l.trim()));
    if (evIdx === -1) {
      evidenceMissing = true;
      evidencePlaceholder = true; // no evidence block at all
    } else {
      // evidence block: from the Auditor/first content line after the heading to section end
      const block = body.slice(evIdx + 1);
      const nonEmpty = block.map((l, k) => ({ l, n: evIdx + 1 + k })).filter(x => x.l.trim());
      if (!nonEmpty.length) {
        evidencePlaceholder = true;
      } else {
        const substantive = nonEmpty.filter(x => !isPlaceholderFragment(x.l));
        if (!substantive.length) {
          evidencePlaceholder = true;
        } else {
          // substantive content exists; if EVERY substantive line is a bare "Auditor / Agent:"-style
          // label without any value, still placeholder (empty value form)
          const allLabelOnly = substantive.every(x => /^\s*-\s*(Auditor\s*\/\s*Agent|Evidence)\s*:\s*$/.test(x.l.trim()) || /^\s*(Auditor\s*\/\s*Agent|Evidence)\s*:\s*$/.test(x.l.trim()));
          evidencePlaceholder = allLabelOnly;
        }
      }
    }
  } else {
    evidenceMissing = true;
    evidencePlaceholder = true;
  }

  if (isCompleted && (evidenceMissing || evidencePlaceholder)) {
    result.violations.push({
      cls: 'C2',
      line: closureSec ? closureSec.start + 1 : 1,
      text: evidenceMissing ? 'no ## Closure section / no Closure Audit Evidence block' : 'Closure Audit Evidence block empty or placeholder-only',
    });
  }

  // C3 (active): gate line ticked + placeholder evidence
  if (isActive) {
    const gateTicked = lines.some(l => /^\s*-\s*\[x\]/i.test(l) && l.includes('结束审计由独立子代理'));
    if (gateTicked && evidencePlaceholder) {
      const gi = lines.findIndex(l => /^\s*-\s*\[x\]/i.test(l) && l.includes('结束审计由独立子代理'));
      result.violations.push({ cls: 'C3', line: gi + 1, text: 'closure-audit gate pre-ticked while evidence placeholder (lesson 25)' });
    }
  }

  // C4: phase section marked completed (literal) with unchecked items inside
  for (const ph of phaseSections(lines)) {
    if (ph.status === 'completed' && ph.unchecked.length) {
      for (const u of ph.unchecked) {
        result.violations.push({ cls: 'C4', line: u.line, text: `phase "${ph.title}" Status: completed but unchecked: ${u.text.slice(0, 60)}` });
      }
    }
  }

  // C5: plan-shaped but no parseable status
  if (status === undefined && result.planShaped) {
    result.violations.push({ cls: 'C5', line: 1, text: 'plan-shaped file (## Closure Gates / ## Draft Review Record) without parseable "> Plan Status:" header' });
  }

  return result;
}

// ---------------------------------------------------------------------------
// enumeration
// ---------------------------------------------------------------------------

async function listPlanFiles() {
  const { stdout } = await execFileAsync('git', ['ls-files', '-co', '--exclude-standard'], {
    cwd: rootDir,
    maxBuffer: 64 * 1024 * 1024,
  });
  return stdout.split(/\r?\n/).map(l => l.trim()).filter(Boolean)
    .filter(f => /^docs\/plans\/[^/]+\.md$/.test(f))
    .filter(f => !/^docs\/plans\/(README\.md|00-[^/]*\.md)$/.test(f))
    .sort();
}

// ---------------------------------------------------------------------------
// baseline snapshot
// ---------------------------------------------------------------------------

function extractSnapshotBlock(content) {
  const start = content.indexOf(SNAPSHOT_HEADING);
  if (start === -1) return null;
  const fenceOpen = content.indexOf('```yaml', start);
  if (fenceOpen === -1) return null;
  const bodyStart = content.indexOf('\n', fenceOpen) + 1;
  const fenceClose = content.indexOf('```', bodyStart);
  if (fenceClose === -1) return null;
  return { body: content.slice(bodyStart, fenceClose) };
}

function parseSnapshot(body) {
  // lines: "  <file>: { C1: n, C2: n }"
  const files = {};
  for (const raw of body.split('\n')) {
    const m = raw.match(/^\s{2}(\S+):\s*\{(.*)\}\s*$/);
    if (m) {
      const fc = {};
      for (const kv of m[2].split(',')) {
        const [k, v] = kv.split(':').map(x => x.trim());
        if (CLASSES.includes(k)) fc[k] = parseInt(v, 10) || 0;
      }
      files[m[1]] = fc;
    }
  }
  return files;
}

function renderSnapshotYaml(files, scanned, generatedAt) {
  const out = [];
  out.push(`generated: ${generatedAt}`);
  out.push(`scanned: ${scanned}`);
  for (const f of Object.keys(files).sort()) {
    const fc = files[f];
    const parts = CLASSES.filter(c => fc[c]).map(c => `${c}: ${fc[c]}`);
    out.push(`  ${f}: { ${parts.join(', ')} }`);
  }
  return out.join('\n');
}

// ---------------------------------------------------------------------------
// self-test (in-memory fixtures)
// ---------------------------------------------------------------------------

export function runSelfTest() {
  let pass = true;
  const checks = [];
  const expect = (name, cond) => { checks.push({ name, ok: !!cond }); if (!cond) pass = false; };
  const classesOf = c => scanPlan(c).violations.map(v => v.cls);
  const count = (c, cls) => scanPlan(c).violations.filter(v => v.cls === cls).length;

  const GATE_X = '- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符';
  const GATE_O = '- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符';

  // --- positive: must flag
  const c1 = '> Plan Status: completed\n\n- [x] done item\n- [ ] leftover\n';
  expect('C1 completed with leftover unchecked flagged', count(c1, 'C1') === 1);

  const c2a = '> Plan Status: completed\n\n## Closure\n\nClosure Audit Evidence:\n\n- Auditor / Agent: （待独立结束审计）\n- Evidence: （待审计落盘）\n';
  expect('C2 placeholder evidence (待 signatures) flagged', count(c2a, 'C2') === 1);
  const c2b = '> Plan Status: completed\n\n## Closure\n\nClosure Audit Evidence:\n\n- Auditor / Agent: <independent auditor or independent subagent>\n- Evidence: <task id / log link / walkthrough record>\n';
  expect('C2 template-angle placeholder flagged', count(c2b, 'C2') === 1);
  const c2c = '> Plan Status: completed\n\n## Closure\n\nClosure Audit Evidence:\n';
  expect('C2 empty evidence block flagged', count(c2c, 'C2') === 1);
  const c2d = '> Plan Status: completed\n\n## Goals\n\nno closure section at all\n';
  expect('C2 missing ## Closure section flagged', count(c2d, 'C2') === 1);

  const c3 = `> Plan Status: active\n\n## Closure\n\nClosure Audit Evidence:\n\n- Auditor / Agent: （待独立结束审计）\n\n## Closure Gates\n\n${GATE_X}\n`;
  expect('C3 pre-ticked gate + placeholder evidence flagged', count(c3, 'C3') === 1);

  const c4 = '> Plan Status: completed\n\n### Phase 1 - x\n\nStatus: completed\n\n- [x] a\n- [ ] forgotten\n';
  expect('C4 phase completed with unchecked flagged', count(c4, 'C4') === 1);

  const c1done = '> Plan Status: done（2026-09-21 结束审计 round2 pass）\n\n- [ ] 两修复落地；新测试红→绿\n';
  expect('C1 done-alias with unchecked execution item flagged (f310 form)', count(c1done, 'C1') === 1);

  const c5 = '# some plan\n\n## Closure Gates\n\n- [x] all done\n';
  expect('C5 plan-shaped without status header flagged', count(c5, 'C5') === 1);

  // --- negative: must pass
  const ev1 = '> Plan Status: completed\n\n## Closure\n\nClosure Audit Evidence:\n\n- Auditor / Agent: 独立审计代理（agent_x）\n- Evidence: 三轮复核记录在案\n';
  expect('negative: label-pair evidence format passes', count(ev1, 'C2') === 0);
  const ev2 = '> Plan Status: completed\n\n## Closure\n\nClosure Audit Evidence:\n\n- Auditor / Agent: 独立结束审计两轮收敛（agent_y，全程只读+独立复跑）——\n  - round 1 NEEDS REVISION（B-1 说明）\n  - round 2 RE-REVIEW: RESOLVED\n';
  expect('negative: Auditor+continuation format (no Evidence: line) passes', count(ev2, 'C2') === 0);
  const ev3 = '> Plan Status: completed\n\n## Closure\n\nClosure Audit Evidence:\n\n- Auditor / Agent: agent_z\n- Iteration 1: accept（2026-09-21）\n';
  expect('negative: Auditor+Iteration lines format passes', count(ev3, 'C2') === 0);
  const f39 = `> Plan Status: active\n\n## Closure\n\nClosure Audit Evidence:\n\n- Auditor / Agent: agent_w（两轮收敛 RESOLVED）\n\n## Closure Gates\n\n${GATE_O}\n- [x] 结束证据存在于文件中\n`;
  expect('negative: active gate unticked + evidence filled passes (f39 form)', count(f39, 'C2') === 0 && count(f39, 'C3') === 0);
  const draft = '> Plan Status: draft\n\n- [ ] everything unchecked\n';
  expect('negative: draft passes', count(draft, 'C1') === 0);
  const nonplan = '# notes\n\nsome random document without plan sections\n';
  expect('negative: status-less non-plan doc passes (C5 reverse)', count(nonplan, 'C5') === 0);
  const fenced = '---\n```md\n> Plan Status: draft\n- [ ] template text in fence\n```\n---\n> Plan Status: completed\n\n## Closure\n\nClosure Audit Evidence:\n\n- Auditor / Agent: agent_q（两轮 pass）\n';
  expect('negative: fenced template text does not trigger', count(fenced, 'C1') === 0);

  // --- parser
  expect('parser: done（…） alias resolved', scanPlan(c1done).resolvedStatus === 'completed');
  expect('parser: in_progress alias resolved', scanPlan('> Plan Status: in_progress\n').resolvedStatus === 'active');
  expect('parser: **completed** bold value resolved', scanPlan('> Plan Status: **completed**\n').resolvedStatus === 'completed');
  expect('parser: bold label > **Plan Status**: resolved', scanPlan('> **Plan Status**: done\n').resolvedStatus === 'completed');
  const fw = '> Plan Status: completed\n\n## Plan Status 演进\n\n> Plan Status: active（后置位）\n';
  expect('parser: first-wins (later status line ignored)', scanPlan(fw).resolvedStatus === 'completed');
  const unk = '> Plan Status: finshed\n';
  expect('parser: unknown status token recorded', scanPlan(unk).unknownStatus === 'finshed');
  const gateVariant = `> Plan Status: active\n\n## Closure\n\nClosure Audit Evidence:\n\n- Auditor / Agent: 待独立结束审计\n\n## Closure Gates\n\n- [x] 结束审计由独立子代理执行（PASS，agent_v 两轮）\n`;
  expect('C3 variant gate-line text matched by substring', count(gateVariant, 'C3') === 1);

  // --- strict delta math (replicating compare logic)
  const baseline = { 'a.md': { C1: 2, C2: 0, C3: 0, C4: 0, C5: 0 } };
  const mk = (f, cls, n) => ({ file: f, counts: { [cls]: n } });
  const cmp = (cur) => {
    const newV = [], improved = [];
    for (const { file, counts } of cur) {
      const bc = baseline[file] || {};
      for (const cls of CLASSES) {
        const a = counts[cls] || 0, b = bc[cls] || 0;
        if (a > b) newV.push(`${file} ${cls}`);
        else if (a < b) improved.push(`${file} ${cls}`);
      }
    }
    return { newV, improved };
  };
  expect('strict: count rise flagged', cmp([mk('a.md', 'C1', 3)]).newV.length === 1);
  expect('strict: new file violation flagged', cmp([mk('new.md', 'C5', 1)]).newV.length === 1);
  expect('strict: count drop is improvement only', (() => { const r = cmp([mk('a.md', 'C1', 1)]); return r.newV.length === 0 && r.improved.length === 1; })());

  console.log('[check-plan-gates] anti-fake-green self-test:');
  for (const c of checks) console.log(`  ${c.ok ? 'PASS' : 'FAIL'}  ${c.name}`);
  console.log(pass ? 'RESULT: PASS (self-test green)' : 'RESULT: FAIL (self-test detected fake-green risk)');
  return pass ? 0 : 1;
}

// ---------------------------------------------------------------------------
// main
// ---------------------------------------------------------------------------

async function main() {
  const args = process.argv.slice(2);
  if (args.includes('--self-test')) process.exit(runSelfTest());

  let files;
  try {
    files = await listPlanFiles();
  } catch (error) {
    console.error('[check-plan-gates] enumeration failed:', error.message);
    process.exit(2);
  }

  const results = new Map(); // file -> scan result
  for (const f of files) {
    let content;
    try {
      content = await readFile(path.join(rootDir, f), 'utf8');
    } catch (error) {
      console.error(`[check-plan-gates] read failed: ${f} (${error.message})`);
      process.exit(2);
    }
    const r = scanPlan(content);
    results.set(f, r);
    if (r.unknownStatus) {
      console.error(`[check-plan-gates] unknown plan status token "${r.unknownStatus}" in ${f} — fail-loud (fix the status line)`);
      process.exit(2);
    }
  }

  // collect violations per file per class
  const counts = new Map();
  const noStatusDocs = [];
  for (const [f, r] of results) {
    const fc = {};
    for (const v of r.violations) fc[v.cls] = (fc[v.cls] || 0) + 1;
    if (Object.keys(fc).length) counts.set(f, fc);
    if (r.resolvedStatus === undefined && !r.planShaped) noStatusDocs.push(f);
  }

  // report
  console.log('='.repeat(72));
  console.log('check-plan-gates  (GATE-02 — owner doc: docs/plans/00-plan-authoring-and-execution-guide.md rules 10/11/12)');
  console.log('='.repeat(72));
  console.log(`scanned plan files: ${files.length}`);
  const totalByClass = {};
  for (const cls of CLASSES) {
    let n = 0;
    for (const fc of counts.values()) n += fc[cls] || 0;
    totalByClass[cls] = n;
    console.log(`  ${cls} : ${n} finding(s) in ${[...counts.entries()].filter(([, fc]) => fc[cls]).length} file(s)`);
  }
  if (noStatusDocs.length) {
    console.log(`  (informational: ${noStatusDocs.length} status-less non-plan doc(s) listed, not violations)`);
  }
  if (counts.size) {
    console.log('-'.repeat(72));
    for (const [f, fc] of counts) {
      for (const cls of CLASSES) {
        if (!fc[cls]) continue;
        console.log(`  ${f} :: ${cls} x${fc[cls]}`);
        results.get(f).violations.filter(v => v.cls === cls).slice(0, 5)
          .forEach(v => console.log(`    :${v.line} :: ${v.text}`));
      }
    }
  }

  if (args.includes('--baseline')) {
    const generatedAt = new Date().toISOString();
    const snapshotBody = renderSnapshotYaml(Object.fromEntries(counts), files.length, generatedAt);
    const full = path.join(rootDir, BASELINE_FILE);
    const notes = [
      SNAPSHOT_HEADING,
      '',
      '> 本块由 `node tools/check-plan-gates.mjs --baseline` 自动整体重生成（勿手改；误改可 `git checkout` 还原复验）。',
      '> 门控方向：单向收紧（actual 只降不升）。`--strict` 解析本块做 per-file per-检查类计数比对：新增违规 = 非零退出；',
      '> 计数下降仅在报告列 IMPROVEMENTS 提示收紧，**永不自动改写本快照**。快照的违规登记 ≠ 合法化：',
      '> 存量清单即整改分诊面（C1 f38 类仅需补勾 / f310、01-product-grade 类需重审 / C5 旧约定类可批量裁决后收紧），',
      '> 整改随所属 mission 复访触发（见 GATE-02 计划 Deferred）。',
      '> C2 已知局限：自审自认证据形态机械不可靠定（01-product-grade 为首例），列入人工分诊清单。',
      '',
      '```yaml',
      snapshotBody,
      '```',
      '',
    ].join('\n');
    await writeFile(full, notes, 'utf8');
    console.log(`[check-plan-gates] snapshot written to ${BASELINE_FILE} (generated ${generatedAt})`);
    process.exit(0);
  }

  if (args.includes('--strict')) {
    const full = path.join(rootDir, BASELINE_FILE);
    let doc;
    try {
      doc = await readFile(full, 'utf8');
    } catch {
      console.error(`[check-plan-gates] --strict requires baseline file: ${BASELINE_FILE}`);
      process.exit(2);
    }
    const block = extractSnapshotBlock(doc);
    if (!block) {
      console.error('[check-plan-gates] --strict: SNAPSHOT block not found; run --baseline first');
      process.exit(2);
    }
    const baseline = parseSnapshot(block.body);
    const newViolations = [];
    const improvements = [];
    for (const [f, fc] of counts) {
      const bc = baseline[f] || {};
      for (const cls of CLASSES) {
        const a = fc[cls] || 0, b = bc[cls] || 0;
        if (a > b) newViolations.push(`${f} :: ${cls} actual=${a} > baseline=${b}`);
        else if (a < b) improvements.push(`${f} :: ${cls} actual=${a} < baseline=${b}`);
      }
    }
    for (const f of Object.keys(baseline)) {
      if (!counts.has(f)) {
        const had = CLASSES.some(c => baseline[f][c]);
        if (had) improvements.push(`${f} :: removed from tree (was ${JSON.stringify(baseline[f])})`);
      }
    }
    if (improvements.length) {
      console.log(`IMPROVEMENTS (baseline may be tightened via --baseline; never auto-rewritten): ${improvements.length}`);
      for (const line of improvements.slice(0, 20)) console.log(`  IMPROVED ${line}`);
    }
    if (newViolations.length) {
      console.error(`RESULT: FAIL (--strict: ${newViolations.length} new violation(s) above frozen snapshot)`);
      for (const line of newViolations.slice(0, 50)) console.error(`  NEW ${line}`);
      process.exit(1);
    }
    console.log(`RESULT: PASS (--strict: 0 new violations vs frozen snapshot; ${Object.keys(baseline).length} baseline files)`);
    process.exit(0);
  }

  console.log('RESULT: report mode (use --strict to gate against the frozen snapshot, --baseline to regenerate)');
  process.exit(0);
}

main().catch((error) => {
  console.error('[check-plan-gates] Error:', error);
  process.exit(2);
});
