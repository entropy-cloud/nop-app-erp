#!/usr/bin/env node
/**
 * 主键/外键 string 化迁移顺序冻结脚本（roadmap M0.1）
 *
 * 解析全部 module- 前缀目录各层 pom（codegen/api/dao/meta/service/web/app）+ app-erp-all，
 * 构造模块级依赖 DAG（compile 边与 test 边分开建模），计算域级闭包，
 * 按判据迭代输出可行迁移序列：
 *   「域 D 可行 ⟺ closure(D) ∩ 未迁移域 = 仅含经审计证实的惰性 dao 模块 ∪ 自身」
 * 闭包默认含 test 边（对应 mvn test -pl X -am 的 reactor）；--test-closure=off
 * 可切换为仅 compile 闭包（对应 mvn clean install -pl X -am -DskipTests）。
 *
 * 输出（确定性：不含时间戳，重复运行输出一致）：
 *   默认       人类可读序列（每步含闭包构成）
 *   --format json  机器可读 JSON（stdout）
 *   --edges    打印全部跨域依赖边（供抽样核对 pom）
 *   --lazy <file>  Phase 2 审计结论覆盖惰性 dao 模块集（JSON: { "<domain>": ["<artifactId>",...] }）
 *
 * 用法:
 *   node tools/freeze-id-migration-order.mjs [--test-closure=on|off] [--format json] [--edges] [--lazy file]
 */
import { readFileSync, readdirSync, statSync } from 'fs';
import path from 'path';

const rootDir = process.cwd();
const testClosureOn = !process.argv.includes('--test-closure=off');
const format = process.argv.includes('--format') ? 'json' : 'text';
const showEdges = process.argv.includes('--edges');
const lazyArg = process.argv.find(a => a.startsWith('--lazy='));
const lazyFile = lazyArg ? lazyArg.slice('--lazy='.length) : null;

const LAYERS = ['codegen', 'api', 'dao', 'meta', 'service', 'web', 'app'];
const INFRA_DOMAINS = new Set(['common-service', 'common-test', 'app-erp-all']); // 无 orm 模型的基建域

// ---------- 1. 收集 pom ----------
function collectPoms() {
  const poms = [];
  const dirs = readdirSync(rootDir).filter(d => d.startsWith('module-'));
  if (statSync(path.join(rootDir, 'app-erp-all')).isDirectory()) dirs.push('app-erp-all');
  for (const d of dirs) {
    const stack = [path.join(rootDir, d)];
    while (stack.length) {
      const cur = stack.pop();
      let entries;
      try { entries = readdirSync(cur); } catch { continue; }
      for (const e of entries) {
        const p = path.join(cur, e);
        try { if (!statSync(p).isDirectory()) continue; } catch { continue; }
        if (e === 'target' || e === 'src' || e === 'model' || e === 'deploy' || e.startsWith('.')) continue;
        stack.push(p);
      }
      const direct = path.join(cur, 'pom.xml');
      try { if (statSync(direct).isFile() && !poms.includes(direct)) poms.push(direct); } catch { /* noop */ }
    }
  }
  return poms;
}

function parsePom(text) {
  // 剥离 dependencyManagement / build / profiles 内非实际依赖段落（profiles 已核实无 app-erp 依赖）
  const stripped = text
    .replace(/<dependencyManagement>[\s\S]*?<\/dependencyManagement>/g, '')
    .replace(/<build>[\s\S]*?<\/build>/g, '')
    .replace(/<reporting>[\s\S]*?<\/reporting>/g, '')
    .replace(/<profiles>[\s\S]*?<\/profiles>/g, '');
  const artifacts = [];
  const depRe = /<dependency>([\s\S]*?)<\/dependency>/g;
  let m;
  while ((m = depRe.exec(stripped)) !== null) {
    const block = m[1];
    const g = block.match(/<groupId>\s*([^<]+?)\s*<\/groupId>/);
    const a = block.match(/<artifactId>\s*([^<]+?)\s*<\/artifactId>/);
    const s = block.match(/<scope>\s*([^<]+?)\s*<\/scope>/);
    if (!g || !a) continue;
    const scope = s ? s[1].trim() : 'compile';
    artifacts.push({ groupId: g[1].trim(), artifactId: a[1].trim(), scope });
  }
  return artifacts;
}

// ---------- 2. 模块注册 ----------
const poms = collectPoms();
const modules = new Map(); // artifactId -> { artifactId, domain, layer, pom }
for (const pom of poms) {
  const text = readFileSync(pom, 'utf-8');
  // 自身 artifactId：剥离 <parent> 块后取首个（否则取到的是父聚合器）
  const own = text.replace(/<parent>[\s\S]*?<\/parent>/g, '').match(/<artifactId>\s*([^<]+?)\s*<\/artifactId>/);
  if (!own) continue;
  const artifactId = own[1].trim();
  if (!artifactId.startsWith('app-erp-')) continue;
  const rel = path.relative(rootDir, pom);
  const top = rel.split(path.sep)[0]; // module-<domain> 或 app-erp-all
  const domain = top.startsWith('module-') ? top.slice('module-'.length) : top;
  const isAgg = /<packaging>\s*pom\s*<\/packaging>/.test(text.replace(/<parent>[\s\S]*?<\/parent>/g, ''));
  let layer = null;
  for (const l of LAYERS) if (artifactId.endsWith('-' + l)) layer = l;
  if (!layer) layer = isAgg ? 'agg' : 'jar';
  modules.set(artifactId, { artifactId, domain, layer, pom: rel });
}

// ---------- 3. 依赖边 ----------
const compileEdges = new Map(); // from -> Set(to)
const testEdges = new Map();
const edgeMeta = [];
for (const [artifactId, mod] of modules) {
  if (mod.layer === 'agg') continue;
  const deps = parsePom(readFileSync(path.join(rootDir, mod.pom), 'utf-8'));
  for (const d of deps) {
    if (d.groupId !== 'io.nop.app') continue;
    const target = modules.get(d.artifactId);
    if (!target || target.layer === 'agg') continue;
    if (target.artifactId === artifactId) continue;
    const isTest = d.scope === 'test';
    const map = isTest ? testEdges : compileEdges;
    if (!map.has(artifactId)) map.set(artifactId, new Set());
    map.get(artifactId).add(d.artifactId);
    edgeMeta.push({ from: artifactId, to: d.artifactId, scope: d.scope, fromDomain: mod.domain, toDomain: target.domain });
  }
}

// ---------- 4. 惰性模块集（判据允许出现在闭包中的未迁移模块）----------
// 惰性层依据（M0.1 实测，见审计工件）：
//   codegen: main 零手写 Java（仅 postcompile 脚本 + test 运行器 + orm 模型副本），按本域 orm 自洽重生成
//   meta:    零手写 Java（生成 XMeta/i18n 资源）
//   api:     全部生成件（__XGEN_FORCE_OVERRIDE__ 头，路线图已核零手写）
//   web:     main 为 AMIS 视图数据文件；手写 Java 仅 src/test 本域页面测试，无跨域 id 类型耦合
//   dao:     存在手写代码（如 crm-dao 3 处跨域实体 import）→ 归 Phase 2 逐域审计，
//            默认视为惰性，--lazy 文件可按域收窄（证伪域将阻塞其下游）
//   app:     本域打包聚合（runner jar），无跨域 Java
// 阻塞层：service（手写 BizModel/Processor，下游测试以 String id 集成调用其 bean）
// 基建域（无 orm，不参与迁移）：common-service（orgId 语义由 M1.3 在首个 orm 域之前适配）、
//   common-test（实测 5 个 Java 文件零 id/Long 耦合）、app-erp-all
const LAZY_LAYERS = new Set(['codegen', 'dao', 'meta', 'api', 'web', 'app']);
let lazyDao = new Map();
for (const [, mod] of modules) {
  if (mod.layer !== 'dao' || INFRA_DOMAINS.has(mod.domain)) continue;
  if (!lazyDao.has(mod.domain)) lazyDao.set(mod.domain, []);
  lazyDao.get(mod.domain).push(mod.artifactId);
}
if (lazyFile) {
  const override = JSON.parse(readFileSync(path.join(rootDir, lazyFile), 'utf-8'));
  lazyDao = new Map(Object.entries(override));
}
const lazySet = new Set();
for (const list of lazyDao.values()) for (const a of list) lazySet.add(a);
for (const [, mod] of modules)
  if (LAZY_LAYERS.has(mod.layer) && !INFRA_DOMAINS.has(mod.domain) && mod.layer !== 'dao')
    lazySet.add(mod.artifactId);

// ---------- 5. 域注册（有 orm 模型的域 = 迁移对象）----------
const ormDomains = [];
for (const d of readdirSync(rootDir)) {
  if (!d.startsWith('module-')) continue;
  try {
    const md = path.join(rootDir, d, 'model');
    if (!statSync(md).isDirectory()) continue;
    if (readdirSync(md).some(f => f.endsWith('.orm.xml')))
      ormDomains.push(d.slice('module-'.length));
  } catch { /* noop */ }
}
ormDomains.sort();

const domainModules = new Map(); // domain -> Set(artifactId)
for (const [, mod] of modules) {
  if (mod.layer === 'agg') continue;
  if (!domainModules.has(mod.domain)) domainModules.set(mod.domain, new Set());
  domainModules.get(mod.domain).add(mod.artifactId);
}

// ---------- 6. 闭包 ----------
// 锚点定义（对齐 M1.1 计划实测的域级构建命令）：
//   serviceAnchored: -pl <域>/erp-<short>-service -am —— 域级 verify（build+test）最小单元，
//                    闭包 = 本域 codegen/dao/meta/service + 上游（含 test 边）
//   fullDomain:      -pl <域>/erp-<short>-api,<域>/erp-<short>-app -am —— 全域重生成链（M1.1 式命令），
//                    app 层可能拉入外域 web→service（如 fin-app→ast-web→ast-service），纠缠域需延后 web/app 重生成
function closure(startArtifacts, useTestEdges) {
  const seen = new Set();
  const stack = [...startArtifacts];
  while (stack.length) {
    const a = stack.pop();
    if (seen.has(a)) continue;
    seen.add(a);
    for (const to of (compileEdges.get(a) || [])) stack.push(to);
    if (useTestEdges) for (const to of (testEdges.get(a) || [])) stack.push(to);
  }
  return seen;
}
const domainServiceAnchor = new Map(); // domain -> service artifactId
for (const [, mod] of modules)
  if (mod.layer === 'service' && !INFRA_DOMAINS.has(mod.domain) && ormDomains.includes(mod.domain))
    domainServiceAnchor.set(mod.domain, mod.artifactId);
function domainFullAnchors(domain) {
  return [...(domainModules.get(domain) || [])].filter(a => {
    const l = modules.get(a).layer; return l === 'api' || l === 'app';
  });
}
function serviceClosure(domain) {
  const anchor = domainServiceAnchor.get(domain);
  return anchor ? closure([anchor], testClosureOn) : new Set();
}
function fullClosure(domain) {
  return closure(domainFullAnchors(domain), true);
}

// ---------- 7. 可行性判定 + 迭代 ----------
function feasible(domain, migrated) {
  const mods = serviceClosure(domain);
  const blocked = [];
  for (const a of mods) {
    const dom = modules.get(a).domain;
    if (INFRA_DOMAINS.has(dom)) continue; // 基建域无 orm 迁移，不构成阻塞
    if (dom === domain || migrated.has(dom)) continue;
    if (!lazySet.has(a)) blocked.push(a);
  }
  return { feasible: blocked.length === 0, blocked: blocked.sort() };
}

function closureComposition(domain, migrated) {
  const mods = serviceClosure(domain);
  const upstreamMigrated = new Set(), unmigratedLazy = new Set();
  for (const a of mods) {
    const dom = modules.get(a).domain;
    if (dom === domain || INFRA_DOMAINS.has(dom)) continue;
    if (migrated.has(dom)) upstreamMigrated.add(dom);
    else unmigratedLazy.add(a);
  }
  // 全域构建（api+app 锚点）相对 service 锚点多出的未迁移非惰性模块 = web/app 重生成延后耦合
  const full = fullClosure(domain);
  const fullBlockers = new Set();
  for (const a of full) {
    const dom = modules.get(a).domain;
    if (dom === domain || INFRA_DOMAINS.has(dom) || migrated.has(dom)) continue;
    if (!lazySet.has(a) && !mods.has(a)) fullBlockers.add(a);
  }
  return {
    closureSize: mods.size,
    upstreamMigrated: [...upstreamMigrated].sort(),
    unmigratedLazyModules: [...unmigratedLazy].sort(),
    fullBuildDeferred: [...fullBlockers].sort(),
  };
}

const sequence = [];
const migrated = new Set();
let deadlock = false;
while (sequence.length < ormDomains.length) {
  const candidates = ormDomains.filter(d => !migrated.has(d) && feasible(d, migrated).feasible);
  if (candidates.length === 0) { deadlock = true; break; }
  const pick = candidates[0]; // 确定性 tie-break：字典序
  sequence.push({ domain: pick, ...closureComposition(pick, migrated) });
  migrated.add(pick);
}

// ---------- 8. 输出 ----------
const whyArg = process.argv.find(a => a.startsWith('--why='));
if (whyArg) {
  const [target, whyDomain] = whyArg.slice('--why='.length).split('@');
  const starts = (whyDomain ? [...(domainModules.get(whyDomain) || [])] : ormDomains.flatMap(d => [...(domainModules.get(d) || [])]));
  const prev = new Map();
  const seen = new Set(starts);
  const queue = [...starts];
  while (queue.length) {
    const cur = queue.shift();
    for (const to of [...(compileEdges.get(cur) || []), ...(testEdges.get(cur) || [])]) {
      if (seen.has(to)) continue;
      seen.add(to); prev.set(to, cur); queue.push(to);
    }
  }
  if (!seen.has(target)) { console.log(`${target}: 不可达`); process.exit(0); }
  const chain = [];
  let n = target;
  while (n) { chain.unshift(n); n = prev.get(n); }
  console.log(chain.join('\n  <- '));
  process.exit(0);
}

if (showEdges) {
  const cross = edgeMeta.filter(e => e.fromDomain !== e.toDomain).sort((a, b) =>
    (a.fromDomain + a.to + a.scope).localeCompare(b.fromDomain + b.to + b.scope));
  if (format === 'json') console.log(JSON.stringify(cross, null, 2));
  else for (const e of cross)
    console.log(`${e.from} -> ${e.to} [${e.scope}]  (${e.fromDomain} => ${e.toDomain})`);
  process.exit(0);
}

const report = {
  testClosureMode: testClosureOn ? 'compile+test' : 'compile-only',
  lazyDaoDefault: !lazyFile,
  ormDomains,
  sequence,
  remaining: ormDomains.filter(d => !sequence.find(s => s.domain === d)),
  deadlock,
  domainClosures: Object.fromEntries(ormDomains.map(d => {
    const svc = serviceClosure(d), comp = closure([domainServiceAnchor.get(d)], false), full = fullClosure(d);
    return [d, {
      serviceClosureSize: svc.size,
      compileClosureSize: comp.size,
      testOnlyModules: [...svc].filter(a => !comp.has(a)).map(a => a + ' (' + modules.get(a).domain + ')').sort(),
      closureDomains: [...new Set([...svc].map(a => modules.get(a).domain).filter(x => x !== d))].sort(),
      fullOnlyModules: [...full].filter(a => !svc.has(a)).map(a => a + ' (' + modules.get(a).domain + ')').sort(),
    }];
  })),
};
if (format === 'json') {
  console.log(JSON.stringify(report, null, 2));
} else {
  console.log(`迁移顺序冻结（闭包口径: ${report.testClosureMode}${report.lazyDaoDefault ? '；惰性 dao 集=默认全部 dao 模块' : '；惰性 dao 集=' + lazyFile}）`);
  console.log(`orm 域数: ${ormDomains.length}   判据: closure(D) ∩ 未迁移域 ⊆ 惰性 dao 模块 ∪ {自身}`);
  console.log('');
  sequence.forEach((s, i) => {
    console.log(`${String(i + 1).padStart(2)}. ${s.domain}`);
    console.log(`     service 闭包模块数: ${s.closureSize}  上游已迁移域: [${s.upstreamMigrated.join(', ') || '-'}]`);
    console.log(`     未迁移惰性模块(dao/codegen): [${s.unmigratedLazyModules.join(', ') || '-'}]`);
    if (s.fullBuildDeferred.length) console.log(`     web/app 全域构建延后耦合: [${s.fullBuildDeferred.join(', ')}]`);
  });
  if (deadlock) {
    console.log(`!! 死锁：剩余域不可行 ${report.remaining.join(', ')}`);
    for (const d of report.remaining) {
      const f = feasible(d, migrated);
      console.log(`   ${d}: blocked by [${f.blocked.join(', ')}]`);
    }
    process.exitCode = 1;
  } else {
    console.log(`\n全部 ${sequence.length} 域有序，无死锁。`);
  }
}
