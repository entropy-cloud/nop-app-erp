#!/usr/bin/env node
/**
 * 回写新鲜度门控（M0.1 裁定机制 (a) 的门控步骤）
 *
 * 逐域校验：实况源 vs dry-run 副本的 diff 仅含 stdDataType 差异行。
 * 各域 plan 回写 orm 源前必须运行并通过本门控（零非 stdDataType 行才允许落源），
 * 防止回滚 dry-run 之后经双批准落地的非 stdDataType 变更（RC 增量）。
 *
 * 用法:
 *   node tools/verify-id-fix-copy-diff.mjs [module-<domain>]   # 单域；缺省全部 19 域
 */
import { readFileSync, readdirSync, statSync } from 'fs';
import path from 'path';

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

let bad = 0, totalPairs = 0;
const rows = [];
for (const rel of files) {
  let a, b;
  try {
    a = readFileSync(path.join(rootDir, rel), 'utf-8').split('\n');
    b = readFileSync(path.join(rootDir, TMP, rel), 'utf-8').split('\n');
  } catch {
    console.log(`[MISSING-COPY] ${rel}（先运行 node tools/check-bigint-id-types.mjs dry-run）`);
    bad++;
    continue;
  }
  if (a.length !== b.length) { console.log(`[LINE-COUNT-DIFF] ${rel}: ${a.length} vs ${b.length}`); bad++; continue; }
  let pairs = 0;
  for (let i = 0; i < a.length; i++) {
    if (a[i] === b[i]) continue;
    pairs++;
    if (norm(a[i]) !== norm(b[i])) {
      console.log(`[NON-STD] ${rel}:${i + 1}`);
      console.log(`  live: ${a[i].trim()}`);
      console.log(`  copy: ${b[i].trim()}`);
      bad++;
    }
  }
  totalPairs += pairs;
  rows.push({ rel, changedLines: pairs });
}
console.log('===== 逐域 diff 行数（stdDataType-only 差异行） =====');
for (const r of rows) console.log(`${r.rel}  变更行: ${r.changedLines}`);
console.log(`\n合计变更行: ${totalPairs}  非法差异行: ${bad}`);
console.log(bad === 0 ? '门控通过：diff 仅含 stdDataType 差异，允许落源。' : '门控失败：存在非 stdDataType 差异，禁止落源。');
process.exit(bad === 0 ? 0 : 1);
