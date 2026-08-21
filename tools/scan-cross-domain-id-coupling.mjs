#!/usr/bin/env node
/**
 * 跨域 id 类型耦合扫描器（M0.1 审计 ②，各域 plan Phase 2/4 grep 门控可复用）
 *
 * 扫描各域 service（可选 dao）模块手写 main 代码（排除 _gen/_ 前缀），输出：
 *   A. 跨域耦合点：引用外域实体类型 / 外域 I*Biz 接口 / daoFor(外域实体) 的 file:line
 *   B. 耦合文件内的 id-as-Long 证据行（Long xxxId 声明 / .getId() / setXxxId(）file:line
 *   C. 域对汇总（D 引用 F：文件数 / 证据行数）——「未迁移下游域引用已迁移域」修复预告
 *
 * 用法:
 *   node tools/scan-cross-domain-id-coupling.mjs [--dao] [--domain module-xxx] [--out file.md]
 */
import { readFileSync, readdirSync, statSync, writeFileSync } from 'fs';
import path from 'path';

const rootDir = process.cwd();
const includeDao = process.argv.includes('--dao');
const domArg = process.argv.find(a => a.startsWith('--domain='));
const onlyDomain = domArg ? domArg.slice('--domain='.length) : null;
const outArg = process.argv.find(a => a.startsWith('--out='));
const outFile = outArg ? outArg.slice('--out='.length) : null;

// 域目录 -> 包前缀（module-contract 双包 contract/ct）
const DOMAIN_PKGS = {
  'module-aps': ['aps'], 'module-assets': ['ast'], 'module-b2b': ['b2b'],
  'module-contract': ['contract', 'ct'], 'module-crm': ['crm'], 'module-cs': ['cs'],
  'module-drp': ['drp'], 'module-finance': ['fin'], 'module-hr': ['hr'],
  'module-inventory': ['inv'], 'module-logistics': ['log'],
  'module-maintenance': ['mnt'], 'module-manufacturing': ['mfg'],
  'module-master-data': ['md'], 'module-notify': ['notify'], 'module-projects': ['prj'],
  'module-purchase': ['pur'], 'module-quality': ['qa'], 'module-sales': ['sal'],
  'module-common-service': ['common'], 'module-common-test': ['common'],
};
const PKG2DOMAIN = {};
for (const [dom, pkgs] of Object.entries(DOMAIN_PKGS))
  for (const p of pkgs) PKG2DOMAIN[p] = dom;

const ID_LINE = /(Long\s+[a-zA-Z]\w*Id\b|\.getId\(\)|set[A-Z]\w*Id\()/;

function walkJava(dir, out) {
  let entries;
  try { entries = readdirSync(dir); } catch { return; }
  for (const e of entries) {
    const p = path.join(dir, e);
    let st; try { st = statSync(p); } catch { continue; }
    if (st.isDirectory()) {
      if (e === '_gen' || e === 'target') continue;
      walkJava(p, out);
    } else if (e.endsWith('.java') && !e.startsWith('_')) {
      out.push(p);
    }
  }
}

const domains = Object.keys(DOMAIN_PKGS).filter(d => d.startsWith('module-') &&
  (!onlyDomain || d === onlyDomain));
const report = [];
for (const dom of domains) {
  const ownPkgs = DOMAIN_PKGS[dom];
  const layers = includeDao ? ['dao', 'service'] : ['service'];
  const layerDirs = readdirSync(path.join(rootDir, dom)).filter(d => layers.some(l => d.endsWith('-' + l)));
  for (const layerDir of layerDirs) {
    const files = [];
    walkJava(path.join(rootDir, dom, layerDir, 'src/main/java'), files);
    for (const f of files) {
      const lines = readFileSync(f, 'utf-8').split('\n');
      const foreignRefs = [];
      const idLines = [];
      lines.forEach((line, i) => {
        const no = i + 1;
        for (const m of line.matchAll(/app\.erp\.([a-z]+)\.(?:dao\.entity|dao\.daterange|biz)\.([A-Za-z_][\w.]*)/g)) {
          const pkg = m[1];
          if (PKG2DOMAIN[pkg] && !ownPkgs.includes(pkg)) foreignRefs.push({ line: no, pkg, what: m[2] });
        }
        for (const m of line.matchAll(/daoFor\(\s*(Erp[A-Z]\w*)\.class\s*\)/g)) {
          // daoFor(ErpXxx.class) —— 经 import 判定归属（无 import 视为本域）
          const imp = lines.find(l => l.includes(`import app.erp.`) && l.includes(`.${m[1]};`));
          if (imp) {
            const pm = imp.match(/import app\.erp\.([a-z]+)\./);
            if (pm && PKG2DOMAIN[pm[1]] && !ownPkgs.includes(pm[1]))
              foreignRefs.push({ line: no, pkg: pm[1], what: m[1] + '.class (daoFor)' });
          }
        }
        for (const m of line.matchAll(/@Inject[^I]*I(Erp[A-Z]\w*)Biz\s+(\w+)/g)) {
          const imp = lines.find(l => l.includes(`import app.erp.`) && l.includes(`.I${m[1]};`));
          if (imp) {
            const pm = imp.match(/import app\.erp\.([a-z]+)\.biz\./);
            if (pm && PKG2DOMAIN[pm[1]] && !ownPkgs.includes(pm[1]))
              foreignRefs.push({ line: no, pkg: pm[1], what: `I${m[1]} ${m[2]}` });
          }
        }
        if (ID_LINE.test(line)) idLines.push({ line: no, text: line.trim().slice(0, 160) });
      });
      if (foreignRefs.length > 0) {
        const foreignDomains = [...new Set(foreignRefs.map(r => PKG2DOMAIN[r.pkg]))].sort();
        report.push({ dom, layerDir, file: path.relative(rootDir, f), foreignDomains, foreignRefs, idLineCount: idLines.length, idLines });
      }
    }
  }
}

// 输出 markdown
let md = `# 跨域 id 类型耦合清单（机器生成，勿手改）\n\n命令: \`node tools/scan-cross-domain-id-coupling.mjs${includeDao ? ' --dao' : ''}\`\n\n`;
const byDom = new Map();
for (const r of report) {
  if (!byDom.has(r.dom)) byDom.set(r.dom, []);
  byDom.get(r.dom).push(r);
}
const pairCount = new Map();
for (const [dom, rows] of [...byDom.entries()].sort()) {
  md += `## ${dom} （引用外域的耦合文件 ${rows.length} 个）\n\n`;
  for (const r of rows) {
    md += `### ${r.file}\n- 外域: ${r.foreignDomains.map(d => d.replace('module-', '')).join(', ')}  |  本文件 id-as-Long 证据行: ${r.idLineCount}\n`;
    for (const fr of r.foreignRefs) md += `- 耦合点 L${fr.line}: ${fr.what} (${PKG2DOMAIN[fr.pkg].replace('module-', '')})\n`;
    for (const il of r.idLines) md += `  - id 行 L${il.line}: \`${il.text}\`\n`;
    md += '\n';
    for (const fd of r.foreignDomains) {
      const k = `${dom}=>${fd}`;
      pairCount.set(k, (pairCount.get(k) || 0) + 1 + r.idLineCount);
    }
  }
}
md += `## 域对汇总（权重 = 耦合点行 + id 证据行）\n\n| 引用方 | 被引用方 | 权重 |\n| --- | --- | --- |\n`;
for (const [k, v] of [...pairCount.entries()].sort((a, b) => b[1] - a[1]))
  md += `| ${k.split('=>')[0].replace('module-', '')} | ${k.split('=>')[1].replace('module-', '')} | ${v} |\n`;

if (outFile) { writeFileSync(path.join(rootDir, outFile), md); console.error(`written: ${outFile}`); }
else console.log(md);
