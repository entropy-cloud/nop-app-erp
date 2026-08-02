// Extract genuine user utterances from any opencode-compatible SQLite store
// (opencode.db, mimocode.db, ...) into a JSONL file.
//
// Schema (tables: session / message / part) is shared by opencode and its
// mimocode fork, so the same tool reads both.
//
// What counts as a "genuine user utterance":
//   Only messages typed by a human into a top-level (root) session.
//   Sub-agent sessions (child sessions spawned via the Task tool) are excluded
//   entirely, because their "user" turn is just the prompt the parent agent passed in.
//
// On top of that, the following are filtered out as non-genuine / noise:
//   - mission_driver prompts        -> "[MISSION_DRIVER]" / "[MISSION_DRIVER:..."
//   - auto-continue prompts          -> "Continue if you have next steps..."
//   - commit instructions            -> "提交修改" / "提交"
//   - git bundle export chatter      -> contains "git bundle"
//   - tool-call / XML echoes         -> "Called the ... tool", "<path>", "<system-reminder>", ...
//   - bare file-drop references      -> text that is only a "file:///" link
//
// Usage:
//   # defaults: opencode.db -> _tmp/opencode-user-messages.jsonl
//   node extract-opencode-user-messages.mjs
//
//   # any project + any db + any output (defaults derived from the db name)
//   node extract-opencode-user-messages.mjs --project /path/to/repo
//   node extract-opencode-user-messages.mjs --db ~/.local/share/mimocode/mimocode.db
//   node extract-opencode-user-messages.mjs --db <db> --out <file> --project <path>
//
//   node extract-opencode-user-messages.mjs --stats-only          # just print kept/dropped tallies
//   node extract-opencode-user-messages.mjs --include-children    # also emit child-session prompts (default off)

import { DatabaseSync } from 'node:sqlite';
import { mkdirSync, writeFileSync } from 'node:fs';
import { basename, dirname, extname, join, resolve } from 'node:path';
import { homedir } from 'node:os';

const args = parseArgs(process.argv.slice(2));
const projectPath = resolve(args.project || process.cwd());
const dbPath = resolve(
  args.db || join(homedir(), '.local', 'share', 'opencode', 'opencode.db'),
);
// Default output name is derived from the db path:
//   opencode.db               -> opencode-user-messages.jsonl
//   mimocode.db               -> mimocode-user-messages.jsonl
//   ~/.zcode/cli/db/db.sqlite -> zcode-user-messages.jsonl  (filename "db" is
//   generic, so we walk up the path to the first meaningful dir segment)
const GENERIC_SEGMENTS = new Set([
  'db', 'database', 'sqlite', 'cli', 'lib', 'share', 'local', 'var',
  'app', 'apps', 'data', 'main', 'store', 'storage', 'file', 'files',
  'bin', 'run',
]);
function deriveName(p) {
  const base = basename(p, extname(p));
  if (!GENERIC_SEGMENTS.has(base.toLowerCase())) return base;
  let dir = dirname(p);
  let guard = 0;
  while (dir && dir !== '/' && dir !== '.' && guard++ < 20) {
    const seg = basename(dir).replace(/^\./, '');
    if (seg && !GENERIC_SEGMENTS.has(seg.toLowerCase())) return seg;
    const parent = dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  return base;
}
const dbBase = deriveName(dbPath);
const outPath = resolve(
  args.out || join(projectPath, '_tmp', `${dbBase}-user-messages.jsonl`),
);
const includeChildren = !!args['include-children'];
const statsOnly = !!args['stats-only'];

// --- drop rules ---------------------------------------------------------
// A text is dropped if, after trimming whitespace and surrounding quotes, it:
//   * is empty, or
//   * matches one of the explicit-drop exact strings, or
//   * starts with one of the drop prefixes, or
//   * contains one of the drop substrings.
const DROP_EXACT = new Set([
  '提交修改',
  '提交',
]);
// NOTE: bare "继续" / "补充" / "执行" etc. are KEPT by default.
// Enable MEANINGLESS_EXACT below to also drop context-free continuation fragments.

// Pure continuation / nudge fragments with no standalone meaning once pulled out of
// their session context (e.g. a lone "继续" the user typed just to resume the agent).
// Single ASCII letters are usually stray confirmations / typos.
const MEANINGLESS_EXACT = new Set([
  '继续',
  '继续分析',
  '继续吧',
  '补充',
  '执行',
  '审查',
  '起草',
  '标记',
  '实现',
  '实施',
  '更新',
  '删除',
  '修改',
  '移除',
  '增加',
  '就绪',
  '还差一点',
  'A',
  'a',
]);

const DROP_EXACT_PUNCT = new Set([
  '提交修改。',
  '提交修改.',
  '提交。',
  '提交.',
  '全部提交', // commit variant (no object, means "commit everything")
]);

const DROP_PREFIXES = [
  '[MISSION_DRIVER]',
  '[MISSION_DRIVER:',
  'Continue if you have next steps',
  'Called the ', // "Called the Read tool with the ..."
  // NOTE: only well-known SYSTEM-injected tags are dropped here. We do NOT drop
  // every "<...>" message, because users paste real XML/code tags when discussing
  // tech (e.g. "<source>", "<c:import>", "<suspend>", "<mutation>"). Add new
  // system tags explicitly as new forks surface them.
  '<path>',
  '<path ',
  '<system-reminder>',
  '<system-reminder ',
  '<environment_details>',
  '<environment_details ',
  '<function_calls>',
  '<function_calls ',
  '<antml:',
  '<token',
  '<actor-notification>', // mimocode "background actor" notifications
  '<actor-notification ',
  '<subagent-notification>', // zcode sub-agent notifications
  '<subagent-notification ',
  'file:///', // bare file drop (a "file:///..." link pasted alone)
];

const DROP_SUBSTRINGS = [];

// Each regex carries its own bucket label for the stats breakdown.
const DROP_REGEXES = [
  { re: /git\s*bun[dl]+e/i, reason: 'git_bundle' }, // export git bundle chatter (tolerant of "bunle" typo)
  // "Sync from remote" git operations (e.g. "从远程主分支更新合并", "rebase到远程修改之上",
  // "检查本分支与远程主分支是否完全一样"). Does NOT match design chatter that merely
  // mentions a filename containing "merged" or a rhetorical question like "...和远程对比了吗".
  {
    re: /远程[\s\S]{0,12}(?:更新|合并|拉取|同步|rebase|pull|merge)/i,
    reason: 'remote_sync',
  },
  {
    re: /(?:更新|合并|拉取|同步|rebase|pull|merge)[\s\S]{0,12}远程/i,
    reason: 'remote_sync',
  },
  {
    re: /检查[\s\S]{0,20}远程[\s\S]{0,15}(?:一样|同步|一致)/i,
    reason: 'remote_sync',
  },
  // slide / PPT authoring chatter (incl. the "# PPT Generator" skill template).
  { re: /slide|ppt/i, reason: 'ppt' },
  // Mermaid / diagram tweaking (layout, subgraph, syntax errors).
  {
    re: /mermaid|subgraph|\b子图\b|flowchart|graph\s+(?:TD|LR|TB|RL|BT)|\.mmd\b/i,
    reason: 'diagram',
  },
  // mission driver tooling (improvement, config, execution, log pastes).
  { re: /mission[\s-]*driver/i, reason: 'mission_driver' },
  { re: /║\s*STEP|STEP:\s*\w+[\s\S]{0,10}Model:|<AI_STEP|AI_STEP_RESULT/i, reason: 'md_log' },
  // Slide / article authoring that slips past the "ppt" keyword
  // (PPT title tuning, README one-liner, theory-article references, etc.).
  {
    re: /主标题|副标题|叙事|从\s*Harness 到 Attractor|一句话介绍|@docs\/articles|docs\/articles\/|技术文章|公众号文章|讲稿|写一篇|撰写一篇|章节顺序|这篇文章|网上的文章|本文章|其他文章|文章全文|(?:阅读|修改|核查|补充|优化)[\s\S]{0,6}文章|文章[\s\S]{0,12}(?:修改|阅读|章节|链接|引用|全文|文字|优化)/i,
    reason: 'article',
  },
];

// Sessions whose entire topic is slide/article authoring (not this project's
// design or development) are excluded wholesale. Matched on the session title.
const DROP_SESSION_TITLES = /ppt|文章|讲稿|讲演稿|博客|blog|loop[\s-]*engineering.*merged|项目证据表述|研发历史|mission\s*driver[\s\S]*loop\s*engineering|提取[\s\S]*sqlite|提取[\s\S]*用户对话/i;

function classify(rawText) {
  if (rawText == null) return { keep: false, reason: 'null' };
  // strip surrounding whitespace and matching quote chars
  let t = String(rawText).trim();
  t = stripQuotes(t);
  t = t.trim();
  if (t.length === 0) return { keep: false, reason: 'empty' };

  if (DROP_EXACT.has(t) || DROP_EXACT_PUNCT.has(t)) {
    const reason = t.startsWith('提交') || t === '全部提交' ? 'commit' : 'exact';
    return { keep: false, reason };
  }
  if (MEANINGLESS_EXACT.has(t)) {
    return { keep: false, reason: 'meaningless' };
  }
  for (const p of DROP_PREFIXES) {
    if (t.startsWith(p)) {
      return { keep: false, reason: prefixReason(p) };
    }
  }
  for (const s of DROP_SUBSTRINGS) {
    if (t.includes(s)) return { keep: false, reason: 'git_bundle' };
  }
  for (const rule of DROP_REGEXES) {
    if (rule.re.test(t)) return { keep: false, reason: rule.reason };
  }
  return { keep: true, reason: 'keep', text: t };
}

function stripQuotes(s) {
  // remove up to one layer of matching surrounding quotes (", ', “”, ‘’)
  const pairs = [
    ['"', '"'],
    ["'", "'"],
    ['\u201C', '\u201D'],
    ['\u2018', '\u2019'],
  ];
  for (const [a, b] of pairs) {
    if (s.length >= 2 && s[0] === a && s[s.length - 1] === b) {
      return s.slice(1, -1);
    }
  }
  return s;
}

function prefixReason(p) {
  if (p.startsWith('[MISSION_DRIVER')) return 'mission_driver';
  if (p.startsWith('Continue if')) return 'auto_continue';
  if (p.startsWith('Called the')) return 'tool_echo';
  if (p.startsWith('file:///')) return 'file_drop';
  if (p.startsWith('<')) return 'xml_artifact';
  return 'prefix';
}

// --- DB access ----------------------------------------------------------
const db = new DatabaseSync(dbPath, { readOnly: true });

// Locate the project in this db. opencode/mimocode expose a `project` table
// (worktree -> id); zcode does not, but every fork stores the working
// directory on each `session` row. Match on session.directory directly so the
// tool works uniformly across all three without depending on a project table.
const dirRow = db
  .prepare('SELECT DISTINCT directory AS d FROM session WHERE directory = ?')
  .get(projectPath);
if (!dirRow) {
  console.error(`No session found for directory: ${projectPath}`);
  console.error('Known directories in this db:');
  for (const row of db
    .prepare('SELECT DISTINCT directory FROM session ORDER BY directory')
    .all()) {
    console.error('  ' + (row.directory || '(empty)'));
  }
  process.exit(1);
}

const parentClause = includeChildren ? '' : 'AND s.parent_id IS NULL';

const stmt = db.prepare(`
  SELECT s.id           AS session_id,
         s.title        AS session_title,
         m.time_created AS time_created,
         p.data         AS part_data
  FROM part p
  JOIN message m ON p.message_id = m.id
  JOIN session s ON m.session_id = s.id
  WHERE s.directory = ?
    ${parentClause}
    AND json_extract(m.data, '$.role') = 'user'
    AND json_extract(p.data, '$.type') = 'text'
    -- Keep only the primary agent's turns. Sub-agent (Task tool) turns carry an
    -- agent tag ending in "explore" or "general" (opencode: explore/general,
    -- mimocode: explore/general, zcode: zcode-explore). On opencode/zcode they
    -- live in child sessions (already excluded by parentClause); on mimocode
    -- they leak into root sessions but still carry these tags. Matching on the
    -- suffix makes this work across all three forks.
    AND json_extract(m.data, '$.agent') NOT LIKE '%explore'
    AND json_extract(m.data, '$.agent') NOT LIKE '%general'
  ORDER BY m.time_created ASC, p.id ASC
`);

const rows = stmt.all(projectPath);

// --- filter + collect ---------------------------------------------------
const kept = [];
const reasons = {};
const seenTexts = new Set(); // for optional exact dedupe

for (const row of rows) {
  if (DROP_SESSION_TITLES.test(row.session_title || '')) {
    reasons['session_topic'] = (reasons['session_topic'] || 0) + 1;
    continue;
  }
  let text;
  try {
    const data = JSON.parse(row.part_data);
    text = data.text;
  } catch {
    text = null;
  }
  const verdict = classify(text);
  reasons[verdict.reason] = (reasons[verdict.reason] || 0) + 1;
  if (!verdict.keep) continue;
  // optional exact-dedupe (keeps first occurrence). Comment out the next 3 lines to keep duplicates.
  const key = verdict.text;
  if (seenTexts.has(key)) {
    reasons['dedupe_dup'] = (reasons['dedupe_dup'] || 0) + 1;
    continue;
  }
  seenTexts.add(key);

  kept.push({
    source: dbBase,
    time: iso(row.time_created),
    session_id: row.session_id,
    session_title: redactHome(row.session_title || ''),
    text: redactHome(verdict.text),
  });
}

// --- output -------------------------------------------------------------
const total = rows.length;
const dropped = total - kept.length - (reasons['dedupe_dup'] || 0);

console.error('db:        ' + redactHome(dbPath));
console.error('project:   ' + redactHome(projectPath));
console.error('scope:     ' + (includeChildren ? 'root + child sessions' : 'root sessions only'));
console.error('rows:      ' + total + ' user text parts');
console.error('breakdown:');
for (const [r, c] of Object.entries(reasons).sort((a, b) => b[1] - a[1])) {
  console.error('  ' + r.padEnd(16) + c);
}
console.error('kept:      ' + kept.length + ' (after dedupe)');

if (!statsOnly) {
  mkdirSync(dirname(outPath), { recursive: true });
  const lines = kept.map((o) => JSON.stringify(o)).join('\n') + (kept.length ? '\n' : '');
  writeFileSync(outPath, lines, 'utf8');
  console.error('wrote:     ' + outPath);
}
db.close();

// --- helpers ------------------------------------------------------------
function iso(ms) {
  if (!ms) return null;
  return new Date(Number(ms)).toISOString();
}

// Replace the absolute home directory (e.g. "/Users/abc") with "~" so no
// local username leaks into the exported JSONL.
function redactHome(s) {
  if (!s) return s;
  const home = homedir();
  if (!home) return s;
  return s.split(home).join('~');
}

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const key = a.slice(2);
      if (i + 1 < argv.length && !argv[i + 1].startsWith('--')) {
        out[key] = argv[++i];
      } else {
        out[key] = true;
      }
    }
  }
  return out;
}
