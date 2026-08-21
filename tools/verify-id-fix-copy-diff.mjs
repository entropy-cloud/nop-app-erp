#!/usr/bin/env node
/**
 * 回写新鲜度门控（M0.1 裁定机制 (a) 的门控步骤；M0.2 登记册豁免语义对齐）
 *
 * 逐域校验：实况源 vs dry-run 副本的 diff 仅含 stdDataType 差异行。
 * 各域 plan 回写 orm 源前必须运行并通过本门控（零非 stdDataType 行才允许落源），
 * 防止回滚 dry-run 之后经双批准落地的非 stdDataType 变更（RC 增量）。
 *
 * 登记册（tools/id-migration-registry.json5，M0.2）豁免语义对齐：
 *  - 预期变更行计数排除登记册延后列：延后列在时点副本与实况源同为 long（dry-run
 *    不翻转），本就不产生差异行；M2.7 退役 8 条目后同批翻转 prj + fin 6 列 +
 *    hr 2 列，整文件 cp 落源不引入回滚或误翻转，门控计数与实际落源行集一致；
 *  - 延后列若意外出现在差异行（如被翻转）→ 记非法差异行并 FAIL（禁止落源）；
 *  - 登记册缺失/不可解析 → fail-closed 报错非零退出（与 check-bigint-id-types.mjs
 *    一致；静默回退会放行被翻转的延后列，正是 D4 要预防的破坏）；
 *  - 仅消费 active 的 orm-column-deferral 条目（门控范围 ≠ 登记范围）。
 *
 * 用法:
 *   node tools/verify-id-fix-copy-diff.mjs [module-<domain>]   # 单域；缺省全部 19 域
 */
import { readFileSync, readdirSync, statSync } from 'fs';
import path from 'path';
import { loadRegistry, parseFile } from './check-bigint-id-types.mjs';

const rootDir = process.cwd();
const TMP = '_tmp/bigint-id-string-fix';
const only = process.argv[2] || null;

const norm = s => s.replace(/\s*stdDataType="[^"]*"/g, '');

function listOrm(dir) {
  const out = [];
  for (const e of readdirSync(dir)) {
    if (!e.startsWith('module-')) continue;
    if (only && e !== only) continue;
    const m = path.join(dir, e, 'model');
    try {
      if (!statSync(m).isDirectory()) continue;
      for (const f of readdirSync(m)) if (f.endsWith('.orm.xml')) out.push(path.join(e, 'model', f));
    } catch { /* ignore */ }
  }
  return out.sort();
}

const files = listOrm(rootDir);
if (files.length === 0) {
  console.error(only ? `未找到 ${only}/model/*.orm.xml 或其 dry-run 副本` : '未找到任何 module-*/model/*.orm.xml');
  process.exit(1);
}

const reg = loadRegistry(rootDir); // fail-closed：缺失/不可解析 = 报错 + 非零退出

// active orm-column-deferral 条目 → { relPath → Set('entity.column') }（仅限本次扫描的域）
const scannedModules = new Set(files.map(f => f.split('/')[0]));
const deferredByRel = new Map();
for (const e of reg.entries) {
  if (e.kind !== 'orm-column-deferral' || e.status !== 'active') continue;
  if (!scannedModules.has(`module-${e.domain}`)) continue;
  const rel = files.find(f => f.startsWith(`module-${e.domain}/model/`));
  if (!rel) continue;
  if (!deferredByRel.has(rel)) deferredByRel.set(rel, new Set());
  deferredByRel.get(rel).add(`${e.entity}.${e.column}`);
}

// 延后列 <column> 标签覆盖的行号集合（1-based；标签可能跨行，取起止行闭区间）
const deferredLinesCache = new Map();
function deferredTagLines(rel) {
  if (deferredLinesCache.has(rel)) return deferredLinesCache.get(rel);
  const lines = new Set();
  const set = deferredByRel.get(rel);
  if (set && set.size > 0) {
    const text = readFileSync(path.join(rootDir, rel), 'utf-8');
    const { entities } = parseFile(text);
    for (const entity of entities) {
      for (const col of entity.cols) {
        if (!set.has(`${entity.name}.${col.name}`)) continue;
        const startLine = text.substring(0, col.absStart).split('\n').length;
        const endLine = text.substring(0, col.absEnd).split('\n').length;
        for (let l = startLine; l <= endLine; l++) lines.add(l);
      }
    }
  }
  deferredLinesCache.set(rel, lines);
  return lines;
}

// 实况源中待改列计数（BIGINT PK/FK 且未 string 化，排除登记册延后列）。
// 仅用于 MISSING-COPY 判定：已迁移/无待改列的文件 dry-run 不生成副本，属合法缺席。
// 域级 stdDataType 仅 boolFlag(BOOLEAN)，BIGINT 列无 domain 推断 string 的形态，显式串检查安全。
function pendingChangeCount(rel) {
  const defSet = deferredByRel.get(rel) || new Set();
  const { entities } = parseFile(readFileSync(path.join(rootDir, rel), 'utf-8'));
  let n = 0;
  for (const e of entities) {
    const fkProps = new Set();
    for (const r of e.relations)
      if (r.type === 'to-one') for (const j of r.joins) fkProps.add(j.leftProp);
    for (const c of e.cols) {
      const isPk = c.primary;
      const isFk = ((/Id$/.test(c.name || '') && c.name !== 'id') || /_ID$/.test(c.code || '') || fkProps.has(c.name));
      if ((!isPk && !isFk) || c.stdSqlType !== 'BIGINT') continue;
      if (/stdDataType="string"/.test(c.rawTag)) continue;
      if (defSet.has(`${e.name}.${c.name}`)) continue;
      n++;
    }
  }
  return n;
}

let bad = 0, totalPairs = 0, deferredFlipped = 0;
const rows = [];
for (const rel of files) {
  const defLines = deferredTagLines(rel);
  const deferredCount = deferredByRel.has(rel) ? deferredByRel.get(rel).size : 0;
  let a, b;
  try {
    a = readFileSync(path.join(rootDir, rel), 'utf-8').split('\n');
    b = readFileSync(path.join(rootDir, TMP, rel), 'utf-8').split('\n');
  } catch {
    if (pendingChangeCount(rel) === 0) {
      console.log(`[NO-COPY-NEEDED] ${rel}（本文件无待改列，dry-run 未生成副本，合法缺席）`);
      rows.push({ rel, changedLines: 0, deferredCount, fileDeferredFlipped: 0 });
    } else {
      console.log(`[MISSING-COPY] ${rel}（先运行 node tools/check-bigint-id-types.mjs dry-run）`);
      bad++;
    }
    continue;
  }
  if (a.length !== b.length) { console.log(`[LINE-COUNT-DIFF] ${rel}: ${a.length} vs ${b.length}`); bad++; continue; }
  let pairs = 0, fileDeferredFlipped = 0;
  for (let i = 0; i < a.length; i++) {
    if (a[i] === b[i]) continue;
    if (defLines.has(i + 1)) {
      fileDeferredFlipped++;
      console.log(`[DEFERRED-FLIPPED] ${rel}:${i + 1}（登记册延后列被翻转，禁止落源）`);
      console.log(`  live: ${a[i].trim()}`);
      console.log(`  copy: ${b[i].trim()}`);
      bad++;
      continue;
    }
    pairs++;
    if (norm(a[i]) !== norm(b[i])) {
      console.log(`[NON-STD] ${rel}:${i + 1}`);
      console.log(`  live: ${a[i].trim()}`);
      console.log(`  copy: ${b[i].trim()}`);
      bad++;
    }
  }
  deferredFlipped += fileDeferredFlipped;
  totalPairs += pairs;
  rows.push({ rel, changedLines: pairs, deferredCount, fileDeferredFlipped });
}
console.log('===== 逐域 diff 行数（stdDataType-only 差异行） =====');
for (const r of rows)
  console.log(`${r.rel}  变更行: ${r.changedLines}` +
    (r.deferredCount > 0 ? `（登记册延后列 ${r.deferredCount} 保持 long，不计入变更行${r.fileDeferredFlipped > 0 ? `；延后列差异行 ${r.fileDeferredFlipped} = 非法` : ''}）` : ''));
console.log(`\n合计变更行: ${totalPairs}  非法差异行: ${bad}（含延后列意外差异行 ${deferredFlipped}）`);
console.log(`登记册延后列（active orm-column-deferral，本门控范围）: ${[...deferredByRel.values()].reduce((n, s) => n + s.size, 0)}（保持 long 不翻转）`);
console.log(bad === 0 ? '门控通过：diff 仅含 stdDataType 差异（延后列未被翻转），允许落源。' : '门控失败：存在非 stdDataType 差异或延后列被翻转，禁止落源。');
process.exit(bad === 0 ? 0 : 1);
