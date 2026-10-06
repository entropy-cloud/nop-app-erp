#!/usr/bin/env node
/**
 * flux 基线门禁（plan 2026-10-06-0355-6）：消费 `npm run validate:flux` 产出的
 * `_tmp/flux-page-validation-report.json` + 全量页面 YAML parse 检查，守护五项基线：
 *
 *   1. totals.errors 钉住（tools/flux-baseline.json errorsPin，漂移即 fail）；
 *   2. 冻结页（7 手写复杂页）unknown-property (prop,renderer) 多重集——禁止新增、允许收缩
 *      （上游 i18nEn 白名单化等合理收缩不误报）；
 *   3. 手写页宇宙（31 个被服务 flux.yaml + 25 个 report/*.page.yaml + 7 手写复杂页）
 *      接线族零容忍——prop ∈ {actionType, api, onEvent}，或 renderer=button 且 prop ∈ {action, args}；
 *   4. 计数器码（unvalidated-component-target / duplicate-schema-id，生成层外部族）禁止增长；
 *   5. 全量 *.page.yaml / *.flux.yaml parse 检查（含死孪生文件——flux 模式不解析它们，
 *      一旦激活即路由崩；plan 0355-3 批次曾因死文件腐化无门禁而隐性带病）。
 *
 * 前置：先跑 `npm run validate:flux` 刷新报告。用法：
 *   node tools/check-flux-baseline.mjs [--self-test]
 * 退出码：0 = 全部基线满足；1 = 存在违例。
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const yaml = require('js-yaml');

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const REPORT = path.join(ROOT, '_tmp/flux-page-validation-report.json');
const BASELINE = path.join(ROOT, 'tools/flux-baseline.json');

const WIRING_PROPS = new Set(['actionType', 'api', 'onEvent']);
const BUTTON_ACTION_PROPS = new Set(['action', 'args']);
const PROP_RENDERER_RE = /Unknown property "([A-Za-z]+)" for renderer type "([A-Za-z]+)"/;

function listPageYaml() {
  const out = [];
  const walk = (dir) => {
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      const p = path.join(dir, e.name);
      if (e.isDirectory()) {
        if (e.name === 'target' || e.name === '_gen' || e.name === '_dump' || e.name === 'node_modules') continue;
        walk(p);
      } else if (e.name.endsWith('.page.yaml') || e.name.endsWith('.flux.yaml')) {
        out.push(p);
      }
    }
  };
  for (const d of fs.readdirSync(ROOT, { withFileTypes: true })) {
    if (d.isDirectory() && d.name.startsWith('module-')) walk(path.join(ROOT, d.name));
  }
  return out.sort();
}

function inUniverse(relFile) {
  // 宇宙=被服务手写页：31 个 flux.yaml（孪生优先）+ report/*.page.yaml + 7 手写复杂页
  // （确定性路径规则，排除 target/_dump/_gen；新增手写页必须入册——glob 判定即册）
  return relFile.endsWith('.flux.yaml') || relFile.includes('/report/') || relFile.includes('/pages/pda-stock-take/')
    || relFile.includes('/pages/stock-take-flow/') || relFile.includes('/pages/disposal-wizard/')
    || relFile.includes('/pages/period-close-wizard/') || relFile.includes('/pages/visit-wizard/')
    || relFile.includes('/pages/lead-conversion/') || relFile.includes('/ErpSysNotification/inbox.page.yaml');
}

function run(baseline, report, yamlFilesTexts) {
  const failures = [];

  // 1. errors pin
  const errors = report.totals?.errors;
  if (errors !== baseline.errorsPin) {
    failures.push(`[errors-pin] totals.errors=${errors} 偏离冻结值 ${baseline.errorsPin}（先对照 known-baseline 族归因，禁止静默改 pin）`);
  }

  // 2/3. per-file checks
  const frozenKeys = Object.keys(baseline.frozenPages);
  for (const f of report.perFile ?? []) {
    const rel = (f.file || '').replace(/^.*flux-pages\//, '');
    const warnings = f.warnings ?? [];

    // 3. wiring-family zero tolerance（手写页宇宙）
    if (inUniverse(rel)) {
      for (const w of warnings) {
        const m = (w.message || '').match(PROP_RENDERER_RE);
        if (!m) continue;
        const [, prop, renderer] = m;
        if (WIRING_PROPS.has(prop) || (renderer === 'button' && BUTTON_ACTION_PROPS.has(prop))) {
          failures.push(`[wiring-zero] ${rel}: ${w.message}`);
        }
      }
    }

    // 2. frozen multiset（禁止新增，允许收缩）
    if (frozenKeys.includes(rel)) {
      const current = warnings
        .map((w) => (w.message || '').match(PROP_RENDERER_RE))
        .filter(Boolean)
        .map((m) => `${m[1]}|${m[2]}`)
        .sort();
      const base = baseline.frozenPages[rel] ?? [];
      const baseCopy = [...base];
      for (const pair of current) {
        const idx = baseCopy.indexOf(pair);
        if (idx >= 0) baseCopy.splice(idx, 1);
        else failures.push(`[frozen-new] ${rel}: 新增 unknown-property "${pair.replace('|', '" for renderer type "')}"（冻结基线无此 (prop,renderer)）`);
      }
      // baseCopy 剩余 = 收缩（允许，仅提示）
      if (baseCopy.length) {
        console.error(`[info] ${rel}: 基线收缩 ${baseCopy.length} 条（允许）：${baseCopy.join(', ')}`);
      }
    }
  }

  // 4. counter codes
  const codes = {};
  for (const f of report.perFile ?? []) {
    for (const w of f.warnings ?? []) {
      const c = w.code || '';
      if (baseline.counterCodes[c] !== undefined) codes[c] = (codes[c] ?? 0) + 1;
    }
  }
  for (const [code, pin] of Object.entries(baseline.counterCodes)) {
    if ((codes[code] ?? 0) > pin) {
      failures.push(`[counter] ${code}=${codes[code]} > 冻结值 ${pin}（生成层计数器禁止增长；增长即新面，须归因裁决）`);
    }
  }

  // 5. parse all page/flux yaml（含死孪生）
  for (const [rel, text] of Object.entries(yamlFilesTexts ?? {})) {
    try {
      yaml.load(text, { json: true });
    } catch (e) {
      failures.push(`[yaml-parse] ${rel}: ${String(e.message).split('\n')[0]}`);
    }
  }

  return failures;
}

function selfTest() {
  const mkWarn = (prop, renderer) => ({ code: 'unknown-property', message: `Unknown property "${prop}" for renderer type "${renderer}".` });
  const base = {
    errorsPin: 0,
    counterCodes: { 'duplicate-schema-id': 1 },
    frozenPages: { 'x/main.page.json': ['i18nEn|button'] },
  };
  const cases = [
    { name: 'errors pin drift', report: { totals: { errors: 1 }, perFile: [] }, expect: 1 },
    { name: 'wiring family on universe page', report: { totals: { errors: 0 }, perFile: [{ file: 'flux-pages/erp/x/pages/report/r.page.json', warnings: [mkWarn('actionType', 'button')] }] }, expect: 1 },
    { name: 'frozen new pair', report: { totals: { errors: 0 }, perFile: [{ file: 'flux-pages/x/main.page.json', warnings: [mkWarn('options', 'button')] }] }, expect: 1 },
    { name: 'counter growth', report: { totals: { errors: 0 }, perFile: [{ file: 'flux-pages/y/main.page.json', warnings: [{ code: 'duplicate-schema-id', message: 'x' }, { code: 'duplicate-schema-id', message: 'y' }] }] }, expect: 1 },
    { name: 'clean pass + shrink allowed', report: { totals: { errors: 0 }, perFile: [{ file: 'flux-pages/x/main.page.json', warnings: [] }] }, expect: 0 },
  ];
  let bad = 0;
  for (const c of cases) {
    const failures = run(base, c.report, []);
    const pass = c.expect === 0 ? failures.length === 0 : failures.length === c.expect;
    console.log(`  self-test [${pass ? 'OK' : 'FAIL'}] ${c.name} → failures=${failures.length}`);
    if (!pass) bad++;
  }
  // yaml parse self-test: invalid yaml must fail
  const badYaml = { 'fake/main.page.yaml': 'a: b\n  c: [unclosed' };
  const failures = run(base, { totals: { errors: 0 }, perFile: [] }, badYaml);
  const parseOk = failures.some((f) => f.startsWith('[yaml-parse]'));
  console.log(`  self-test [${parseOk ? 'OK' : 'FAIL'}] yaml parse detection → failures=${failures.length}`);
  if (!parseOk) bad++;
  console.log(`self-test: ${bad === 0 ? 'PASS' : 'FAIL'} (${bad} broken)`);
  return bad === 0;
}

const isSelfTest = process.argv.includes('--self-test');
if (isSelfTest) {
  const ok = selfTest();
  process.exit(ok ? 0 : 1);
}

const baseline = JSON.parse(fs.readFileSync(BASELINE, 'utf-8'));
const report = JSON.parse(fs.readFileSync(REPORT, 'utf-8'));
const yamlFiles = Object.fromEntries(listPageYaml().map((p) => [path.relative(ROOT, p), fs.readFileSync(p, 'utf-8')]));
console.log(`flux baseline check: report totals=${JSON.stringify(report.totals)} | parse-checked yaml files=${Object.keys(yamlFiles).length}`);
const failures = run(baseline, report, yamlFiles);
if (failures.length) {
  console.error(`\nflux baseline violations (${failures.length}):`);
  for (const f of failures) console.error('  ' + f);
  process.exit(1);
}
console.log('flux baseline: PASS');
