#!/usr/bin/env node
/**
 * 前向耦合方向扫描器（M0.2 登记册的数据来源，可重复运行）
 *
 * 三段扫描 + 冻结序方向分类：
 *   1. orm 级跨域图：解析 19 个 module-x/model 目录下 orm.xml 的 to-one/to-many refEntityName，
 *      按冻结序位次分类（前向 = 早域引用晚域 / 后向 = 晚域引用早域），列级 entity.column → 外域实体。
 *   2. service/dao main 代码跨域耦合（与 M0.1 附录 A 同口径：src/main/java，排除 _gen 与 _ 前缀），
 *      逐边标注方向 × 耦合类型（ibiz / entity / daoFor / dto），前向边额外提取调用点
 *      （IBiz 变量.方法( 的 file:line + 晚域 IBiz 接口签名的 Long 参数名）。
 *   3. test 代码跨域引用（src/test/java，探测规则 = 跨域 import app.erp.<pkg> 静态引用信号，
 *      覆盖 @Inject 字段 / 局部变量 / getBizObject 代理等全部形态）+ common-service/common-test 全量。
 *
 * 冻结序权威一致性：脚本内嵌 FROZEN_ORDER 常量，每次运行 spawn
 * `node tools/freeze-id-migration-order.mjs --format json` 交叉核验，不一致即非零退出（fail-closed）。
 *
 * 输出（确定性，无时间戳）：
 *   --orm-graph=<file>     orm 跨域图 JSON（含方向分类与汇总）
 *   --directions=<file>    main/test 耦合方向分类 JSON（含前向边完整清册）
 *   --registry-out=<file>  登记册 JSON5 骨架（orm-column-deferral / service-bridge / backward-pointer）
 *   --reconcile=<file>     与 scan-cross-domain-id-coupling.mjs 输出对账（文件×外域对集合 diff）
 *
 * 用法:
 *   node tools/scan-id-coupling-directions.mjs --orm-graph=<f> --directions=<f> [--registry-out=<f>] [--reconcile=<f>]
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs';
import path from 'path';
import { spawnSync } from 'child_process';

const rootDir = process.cwd();
const arg = n => { const a = process.argv.find(x => x.startsWith(`--${n}=`)); return a ? a.slice(n.length + 3) : null; };

// 冻结总序（M0.1 冻结、M0 裁决 D6 维持；每次运行与 freeze-id-migration-order.mjs 交叉核验）
const FROZEN_ORDER = ['master-data', 'notify', 'aps', 'b2b', 'contract', 'finance', 'assets', 'cs', 'hr',
  'inventory', 'maintenance', 'projects', 'quality', 'manufacturing', 'purchase', 'sales', 'crm', 'drp', 'logistics'];
const WORK_ITEM = {
  'master-data': 'M1.1', notify: 'M1.2', aps: 'M3.9', b2b: 'M3.8', contract: 'M3.6', finance: 'M2.1',
  assets: 'M2.4', cs: 'M3.5', hr: 'M3.3', inventory: 'M2.2', maintenance: 'M3.2', projects: 'M2.7',
  quality: 'M2.3', manufacturing: 'M3.1', purchase: 'M2.5', sales: 'M2.6', crm: 'M3.4', drp: 'M3.7', logistics: 'M3.10',
};
const POS = Object.fromEntries(FROZEN_ORDER.map((d, i) => [d, i + 1]));
const INFRA_DOMAINS = new Set(['common-service', 'common-test']);

const DOMAIN_PKGS = {
  aps: 'aps', assets: 'ast', b2b: 'b2b',
  contract: ['contract', 'ct'], crm: 'crm', cs: 'cs',
  drp: 'drp', finance: 'fin', hr: 'hr',
  inventory: 'inv', logistics: 'log',
  maintenance: 'mnt', manufacturing: 'mfg',
  'master-data': 'md', notify: 'notify', projects: 'prj',
  purchase: 'pur', quality: 'qa', sales: 'sal',
  'common-service': 'common', 'common-test': 'common',
};
const PKG2DOMAIN = {};
for (const [dom, pkgs] of Object.entries(DOMAIN_PKGS))
  for (const p of (Array.isArray(pkgs) ? pkgs : [pkgs])) PKG2DOMAIN[p] = dom;

const pkgsOf = dom => { const v = DOMAIN_PKGS[dom]; return Array.isArray(v) ? v : [v]; };

function verifyFrozenOrder() {
  const res = spawnSync('node', [path.join(rootDir, 'tools/freeze-id-migration-order.mjs'), '--format', 'json'],
    { encoding: 'utf-8' });
  if (res.status !== 0) {
    console.error('[FAIL] freeze-id-migration-order.mjs 运行失败，冻结序无法核验（fail-closed）');
    process.exit(1);
  }
  let seq;
  try { seq = JSON.parse(res.stdout).sequence.map(s => s.domain); } catch { process.exit(1); }
  if (JSON.stringify(seq) !== JSON.stringify(FROZEN_ORDER)) {
    console.error(`[FAIL] 冻结序不一致：脚本产出 ${JSON.stringify(seq)} vs 内嵌权威 ${JSON.stringify(FROZEN_ORDER)}`);
    process.exit(1);
  }
}

function direction(fromDom, toDom) {
  if (INFRA_DOMAINS.has(fromDom) || INFRA_DOMAINS.has(toDom)) return 'infra';
  return POS[fromDom] < POS[toDom] ? 'forward' : 'backward';
}

const lineAt = (text, idx) => text.slice(0, idx).split('\n').length;

// ---------- 1. orm 级跨域图 ----------
function scanOrmGraph() {
  const edges = [];
  const files = [];
  for (const entry of readdirSync(rootDir).sort()) {
    if (!entry.startsWith('module-')) continue;
    const modelDir = path.join(rootDir, entry, 'model');
    try {
      if (!statSync(modelDir).isDirectory()) continue;
      for (const f of readdirSync(modelDir).sort()) if (f.endsWith('.orm.xml')) files.push({ dir: entry, file: f });
    } catch { /* not a dir */ }
  }
  for (const { dir, file } of files) {
    const rel = `${dir}/model/${file}`;
    const text = readFileSync(path.join(rootDir, rel), 'utf-8')
      .replace(/<!--[\s\S]*?-->/g, m => m.replace(/[^\n]/g, ' ')); // 注释打码但保留换行（行号不变）
    const ownDomain = dir.slice('module-'.length);
    const attr = (s, n) => { const m = s.match(new RegExp(`(?:^|\\s)${n}\\s*=\\s*"([^"]*)"`)); return m ? m[1] : null; };
    let em;
    const entityRe = /<entity\s+([^>]*?)(?:\/>|>([\s\S]*?)<\/entity>)/g;
    while ((em = entityRe.exec(text)) !== null) {
      if (em[2] === undefined) continue;
      const entityName = attr(em[1], 'name');
      let rm;
      const relRe = /<(to-one|to-many)\s+([^>]*?)(?:\/>|>([\s\S]*?)<\/\1>)/g;
      while ((rm = relRe.exec(em[2])) !== null) {
        const refEntity = attr(rm[2], 'refEntityName');
        if (!refEntity) continue;
        const pm = refEntity.match(/^app\.erp\.([a-z]+)\./);
        const refDomain = pm ? PKG2DOMAIN[pm[1]] : null;
        if (!refDomain) continue;
        const refShort = refEntity.split('.').pop();
        const relDir = direction(ownDomain, refDomain);
        if (relDir === 'infra' || ownDomain === refDomain) continue;
        let joinLeft = null, joinRight = null;
        let om;
        const onRe = /<on\s+([^>]*?)\/>/g;
        while ((om = onRe.exec(rm[3] || '')) !== null) {
          joinLeft = attr(om[1], 'leftProp'); joinRight = attr(om[1], 'rightProp');
        }
        // to-one：FK 列在本实体（leftProp）；to-many：FK 列在被引用实体（rightProp）
        const column = rm[1] === 'to-one' ? joinLeft : joinRight;
        const bodyStart = em.index + em[0].indexOf(em[2]);
        const absIdx = bodyStart + rm.index;
        edges.push({
          fromDomain: ownDomain, fromEntity: entityName, relType: rm[1], relName: attr(rm[2], 'name'),
          column, refEntity: refShort, refEntityFqn: refEntity, refDomain,
          file: rel, line: lineAt(text, absIdx), direction: relDir,
        });
      }
    }
  }
  edges.sort((a, b) => (a.file + ':' + a.line).localeCompare(b.file + ':' + b.line, undefined, { numeric: true }));
  const forward = edges.filter(e => e.direction === 'forward');
  const byCluster = new Map();
  for (const e of forward) {
    const k = `${e.fromDomain}->${e.refDomain}`;
    if (!byCluster.has(k)) byCluster.set(k, []);
    byCluster.get(k).push(`${e.file}:${e.line}`);
  }
  return {
    freezeOrder: FROZEN_ORDER,
    freezeOrderVerifiedBy: 'spawn node tools/freeze-id-migration-order.mjs --format json（每次运行核验，不一致非零退出）',
    edgeCount: edges.length,
    forwardEdgeCount: forward.length,
    backwardEdgeCount: edges.length - forward.length,
    forwardClusters: Object.fromEntries([...byCluster.entries()].sort()),
    edges,
  };
}

// ---------- IBiz 方法 Long 参数索引 ----------
function stripComments(s) {
  return s.replace(/\/\*[\s\S]*?\*\//g, m => ' '.repeat(m.length)).replace(/\/\/[^\n]*/g, '');
}

function buildIbizIndex() {
  const index = new Map(); // SimpleName -> { domain, methods: Map<name, longParams[]> }
  for (const domDir of readdirSync(rootDir).sort()) {
    if (!domDir.startsWith('module-')) continue;
    const daoRoot = readdirSync(path.join(rootDir, domDir)).filter(d => d.endsWith('-dao'))
      .map(d => path.join(rootDir, domDir, d, 'src/main/java'));
    for (const root of daoRoot) {
      const files = [];
      walkJava(root, files);
      for (const f of files) {
        const simple = path.basename(f, '.java');
        const pm = f.match(/app\/erp\/([a-z]+)\/biz\//);
        if (!pm) continue;
        const domain = PKG2DOMAIN[pm[1]];
        const text = stripComments(readFileSync(f, 'utf-8'));
        const methods = new Map();
        let m;
        const sigRe = /(\w+)\s*\(((?:[^()]|\([^()]*\))*)\)\s*;/g;
        while ((m = sigRe.exec(text)) !== null) {
          if (['if', 'for', 'while', 'switch', 'catch', 'return', 'new'].includes(m[1])) continue;
          const longParams = [...m[2].matchAll(/\bLong\s+([A-Za-z_]\w*)/g)].map(x => x[1]);
          if (!methods.has(m[1])) methods.set(m[1], longParams);
        }
        index.set(simple, { domain, methods, file: path.relative(rootDir, f) });
      }
    }
  }
  return index;
}

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

const ID_LINE = /(Long\s+[a-zA-Z]\w*Id\b|\.getId\(\)|set[A-Z]\w*Id\()/;

// 注释打码（保留换行与长度）：用于区分编译级引用与注释级引用（javadoc {@link} 等）
function maskJavaComments(text) {
  let out = '';
  let i = 0;
  while (i < text.length) {
    const b = text.indexOf('/*', i);
    const l = text.indexOf('//', i);
    const next = Math.min(b < 0 ? Infinity : b, l < 0 ? Infinity : l);
    if (next === Infinity) { out += text.slice(i); break; }
    out += text.slice(i, next);
    const isBlock = next === b;
    const e = isBlock ? text.indexOf('*/', next + 2) : text.indexOf('\n', next);
    const end = e < 0 ? text.length : (isBlock ? e + 2 : e);
    out += text.slice(next, end).replace(/[^\n]/g, ' ');
    i = end;
  }
  return out;
}

// ---------- 2. main 代码跨域耦合（附录 A 同口径：service+dao src/main/java） ----------
function scanMainScope(ibizIndex) {
  const filesOut = [];
  const ormDomains = readdirSync(rootDir).filter(d => d.startsWith('module-') &&
    FROZEN_ORDER.includes(d.slice('module-'.length)));
  const FQN_RE = /app\.erp\.([a-z]+)\.(dao\.entity|dao\.daterange|biz)\.([A-Za-z_][\w.]*)/g;
  for (const domDir of ormDomains.sort()) {
    const ownDomain = domDir.slice('module-'.length);
    const ownPkgs = pkgsOf(ownDomain);
    const layerDirs = readdirSync(path.join(rootDir, domDir)).filter(d => d.endsWith('-service') || d.endsWith('-dao'));
    for (const layerDir of layerDirs) {
      const javaFiles = [];
      walkJava(path.join(rootDir, domDir, layerDir, 'src/main/java'), javaFiles);
      for (const f of javaFiles) {
        const raw = readFileSync(f, 'utf-8');
        const rel = path.relative(rootDir, f);
        const masked = maskJavaComments(raw);
        const refs = [];
        const idLines = [];
        let m;
        FQN_RE.lastIndex = 0;
        while ((m = FQN_RE.exec(raw)) !== null) {
          const pkg = m[1];
          const dom = PKG2DOMAIN[pkg];
          if (!dom || ownPkgs.includes(pkg)) continue;
          const simple = m[3].split('.').pop();
          let kind = 'dto';
          if (m[2] === 'dao.entity') kind = 'entity';
          else if (simple.startsWith('I') && /Biz$/.test(simple)) kind = 'ibiz';
          else if (m[2] === 'dao.daterange') kind = 'other';
          refs.push({
            line: lineAt(raw, m.index), pkg, domain: dom, fqn: m[0], simple, kind,
            commentLevel: masked[m.index] === ' ',
          });
        }
        let lm;
        const lineRe = /[^\n]+/g;
        while ((lm = lineRe.exec(raw)) !== null) {
          if (ID_LINE.test(lm[0])) idLines.push({ line: lineAt(raw, lm.index), text: lm[0].trim().slice(0, 160) });
        }
        if (refs.length === 0) continue;
        filesOut.push({ domain: ownDomain, layer: layerDir.endsWith('-dao') ? 'dao' : 'service', file: rel, refs, idLines });
      }
    }
  }

  // 前向边调用点提取（仅在非注释文本上解析编译级调用点）
  const forwardInventory = [];
  for (const fr of filesOut) {
    const fwdRefs = fr.refs.filter(r => direction(fr.domain, r.domain) === 'forward');
    if (fwdRefs.length === 0) continue;
    const raw = readFileSync(path.join(rootDir, fr.file), 'utf-8');
    const masked = maskJavaComments(raw);
    for (const r of fwdRefs) {
      const base = {
        file: fr.file, earlyDomain: fr.domain, earlyPos: POS[fr.domain],
        lateDomain: r.domain, latePos: POS[r.domain], kind: r.kind, symbol: r.simple,
        refLine: r.line, commentLevel: r.commentLevel,
        retireOwner: WORK_ITEM[r.domain], deferredUntil: WORK_ITEM[r.domain],
      };
      if (r.commentLevel) {
        forwardInventory.push({ ...base, callSite: `${fr.file}:${r.line}`, method: null, longParams: null, note: '注释级引用（javadoc 等，非编译依赖，登记册排除）' });
        continue;
      }
      if (r.kind === 'ibiz' && ibizIndex.has(r.simple)) {
        const meta = ibizIndex.get(r.simple);
        const varNames = new Set();
        for (const vm of masked.matchAll(new RegExp(`\\b${r.simple}\\s+(\\w+)\\s*[;=]`, 'g'))) varNames.add(vm[1]);
        const callSites = [];
        for (const vn of varNames) {
          for (const cm of masked.matchAll(new RegExp(`\\b${vn}\\.(\\w+)\\s*\\(`, 'g'))) {
            const ln = lineAt(masked, cm.index);
            const declared = meta.methods.has(cm[1]);
            callSites.push({
              line: ln, method: cm[1],
              longParams: declared ? meta.methods.get(cm[1]) : null,
              declared,
            });
          }
        }
        if (callSites.length > 0) {
          callSites.sort((a, b) => a.line - b.line || a.method.localeCompare(b.method));
          for (const cs of [...new Map(callSites.map(c => [c.line + ':' + c.method, c])).values()])
            forwardInventory.push({
              ...base, callSite: `${fr.file}:${cs.line}`, method: cs.method, longParams: cs.longParams,
              note: cs.declared ? '' : '方法未声明于接口文件（ICrudBiz 等继承方法）——Long 参数签名以晚域 plan 翻转时为准',
            });
        } else {
          forwardInventory.push({ ...base, callSite: `${fr.file}:${r.line}`, method: null, longParams: null, note: '类型引用（无解析到的方法调用点）' });
        }
      } else {
        // entity/dto：记录符号出现行中含 id 语境的作为证据
        const usage = [];
        let um;
        const symRe = new RegExp(`\\b${r.simple}\\b`, 'g');
        while ((um = symRe.exec(masked)) !== null) {
          const l = masked.slice(um.index, masked.indexOf('\n', um.index));
          if (ID_LINE.test(l)) usage.push(lineAt(masked, um.index));
        }
        forwardInventory.push({
          ...base, callSite: `${fr.file}:${r.line}`, method: null, longParams: null,
          idContextLines: usage.length ? [...new Set(usage)].slice(0, 20) : [],
          note: usage.length ? '符号使用行含 id 语境（见 idContextLines）' : '类型级引用（id 语境经变量流转时由编译器驱动/域 plan grep 定位调用点）',
        });
      }
    }
  }
  forwardInventory.sort((a, b) => a.callSite.localeCompare(b.callSite, undefined, { numeric: true }));
  return { files: filesOut, forwardInventory };
}

// ---------- 3. test 代码跨域引用 + common 模块 ----------
function scanTestScope() {
  const out = [];
  const domDirs = readdirSync(rootDir).filter(d => d.startsWith('module-')).sort();
  for (const domDir of domDirs) {
    const ownDomain = domDir.slice('module-'.length);
    const ownPkgs = pkgsOf(ownDomain);
    const layers = readdirSync(path.join(rootDir, domDir)).filter(d => /^erp-/.test(d));
    for (const layerDir of layers) {
      const scopes = INFRA_DOMAINS.has(ownDomain)
        ? ['src/main/java', 'src/test/java']
        : ['src/test/java'];
      for (const sc of scopes) {
        const javaFiles = [];
        walkJava(path.join(rootDir, domDir, layerDir, sc), javaFiles);
        for (const f of javaFiles) {
          const masked = maskJavaComments(readFileSync(f, 'utf-8'));
          const imports = [];
          let im;
          const impRe = /import\s+(?:static\s+)?app\.erp\.([a-z]+)\.([A-Za-z_][\w.]*)/g;
          while ((im = impRe.exec(masked)) !== null) {
            const pkg = im[1];
            const dom = PKG2DOMAIN[pkg];
            if (!dom || ownPkgs.includes(pkg)) continue;
            imports.push({ line: lineAt(masked, im.index), pkg, domain: dom, symbol: im[2].split('.').pop() });
          }
          if (imports.length === 0) continue;
          out.push({
            domain: ownDomain, layer: layerDir, scope: INFRA_DOMAINS.has(ownDomain) ? (sc.startsWith('src/test') ? 'test' : 'main') : 'test',
            file: path.relative(rootDir, f), imports,
          });
        }
      }
    }
  }
  return out;
}

// ---------- 对账 ----------
function reconcile(mainScan) {
  const res = spawnSync('node', [path.join(rootDir, 'tools/scan-cross-domain-id-coupling.mjs'), '--dao'],
    { encoding: 'utf-8', maxBuffer: 64 * 1024 * 1024 });
  if (res.status !== 0) { console.error('[FAIL] scan-cross-domain-id-coupling.mjs 运行失败'); process.exit(1); }
  const theirs = new Set();
  for (const m of res.stdout.matchAll(/### (\S+)\n- 外域: ([^\n|]+?)\s+\|/g)) {
    for (const d of m[2].split(',').map(s => s.trim()).filter(Boolean))
      theirs.add(`${m[1]}=>${d}`);
  }
  const mine = new Set();
  for (const fr of mainScan.files)
    for (const d of new Set(fr.refs.map(r => r.domain.replace('module-', '')))) mine.add(`${fr.file}=>${d}`);
  const onlyTheirs = [...theirs].filter(x => !mine.has(x)).sort();
  const onlyMine = [...mine].filter(x => !theirs.has(x)).sort();
  return {
    appendixAFilePairCount: theirs.size, thisScanFilePairCount: mine.size,
    onlyInAppendixA: onlyTheirs, onlyInThisScan: onlyMine,
    match: onlyTheirs.length === 0 && onlyMine.length === 0,
  };
}

// ---------- 登记册骨架 ----------
function emitRegistry(ormGraph, mainScan, testScan) {
  const entries = [];
  let n = 0;
  const id = p => `${p}-${String(++n).padStart(3, '0')}`;
  for (const e of ormGraph.edges.filter(x => x.direction === 'forward')) {
    entries.push({
      id: id('orm-deferral'), kind: 'orm-column-deferral',
      domain: e.fromDomain, entity: e.fromEntity, column: e.column,
      refEntity: e.refEntity, refDomain: e.refDomain,
      evidence: `${e.file}:${e.line}`,
      deferredUntil: WORK_ITEM[e.refDomain], retireOwner: WORK_ITEM[e.refDomain], status: 'active',
    });
  }
  for (const fw of mainScan.forwardInventory) {
    if (fw.commentLevel) continue; // 注释级引用非编译依赖，不进登记册
    entries.push({
      id: id('bridge-main'), kind: 'service-bridge', scope: 'main',
      domain: fw.earlyDomain, callSite: fw.callSite,
      target: fw.method ? `${fw.symbol}.${fw.method}` : fw.symbol,
      longParams: fw.longParams || [], refDomain: fw.lateDomain,
      evidence: fw.refLine ? `${fw.file}:${fw.refLine}` : fw.callSite,
      note: fw.note || '',
      deferredUntil: WORK_ITEM[fw.lateDomain], retireOwner: WORK_ITEM[fw.lateDomain], status: 'active',
    });
  }
  // test 前向边：按文件聚合（早域 plan Phase 3 消费）
  for (const tf of testScan) {
    const fwd = [...new Map(tf.imports.filter(im => direction(tf.domain, im.domain) === 'forward')
      .map(im => [im.domain + ':' + im.symbol, im])).values()];
    if (fwd.length === 0) continue;
    entries.push({
      id: id('bridge-test'), kind: 'service-bridge', scope: 'test',
      domain: tf.domain, callSite: tf.file,
      target: fwd.map(im => `${im.symbol}(${im.domain}) @L${im.line}`).join(', '),
      longParams: [], refDomain: [...new Set(fwd.map(im => im.domain))].sort().join(','),
      evidence: fwd.map(im => `${tf.file}:${im.line}`).slice(0, 10),
      deferredUntil: WORK_ITEM[tf.domain], retireOwner: WORK_ITEM[tf.domain],
      note: '早域 test 引用晚域——早域 plan Phase 3 消费（本域测试适配）；晚域翻转时复核',
      status: 'active',
    });
  }
  // 后向 successor 指针：按（本域 → 早域 × scope）聚合
  const backPairs = new Map();
  for (const fr of mainScan.files) {
    for (const d of new Set(fr.refs.map(r => r.domain))) {
      if (direction(fr.domain, d) !== 'backward') continue;
      const k = `main:${fr.domain}=>${d}`;
      if (!backPairs.has(k)) backPairs.set(k, []);
      backPairs.get(k).push(fr.file);
    }
  }
  for (const tf of testScan) {
    for (const d of new Set(tf.imports.map(im => im.domain))) {
      if (direction(tf.domain, d) !== 'backward') continue;
      const k = `test:${tf.domain}=>${d}`;
      if (!backPairs.has(k)) backPairs.set(k, []);
      backPairs.get(k).push(tf.file);
    }
  }
  for (const [k, files] of [...backPairs.entries()].sort()) {
    const [scope, pair] = [k.split(':')[0], k.slice(k.indexOf(':') + 1)];
    const [dom, refDom] = pair.split('=>');
    entries.push({
      id: id('backward'), kind: 'backward-pointer', scope,
      domain: dom, refDomain: refDom, referencesFrom: files.length,
      evidence: files, successor: WORK_ITEM[dom],
      note: scope === 'test' && !INFRA_DOMAINS.has(dom) ? '晚域 test 引用早域——晚域 plan Phase 3 消费' : '晚域引用早域——晚域 plan 编译器驱动修复清单',
      status: 'active',
    });
  }
  // infra 边登记（common-service/common-test 视角）
  const infraPairs = new Map();
  for (const tf of testScan) {
    if (!INFRA_DOMAINS.has(tf.domain)) continue;
    for (const im of tf.imports) {
      const k = `${tf.domain}=>${im.domain}`;
      if (!infraPairs.has(k)) infraPairs.set(k, []);
      infraPairs.get(k).push(`${tf.file}:${im.line}`);
    }
  }
  for (const [k, ev] of [...infraPairs.entries()].sort()) {
    entries.push({
      id: id('infra'), kind: 'backward-pointer', scope: 'infra',
      domain: k.split('=>')[0], refDomain: k.split('=>')[1], referencesFrom: ev.length,
      evidence: ev.slice(0, 30), successor: 'M4.1',
      note: '基建域（无 orm，不参与冻结序）——引用清单登记，M1.3 已 String 适配',
      status: 'active',
    });
  }
  return entries;
}

// ---------- main ----------
verifyFrozenOrder();
const ormGraph = scanOrmGraph();
const ibizIndex = buildIbizIndex();
const mainScan = scanMainScope(ibizIndex);
const testScan = scanTestScope();

const ormFile = arg('orm-graph');
if (ormFile) { writeFileSync(path.join(rootDir, ormFile), JSON.stringify(ormGraph) + '\n'); }
const dirFile = arg('directions');
if (dirFile) {
  const mainPairs = new Map();
  for (const fr of mainScan.files)
    for (const d of new Set(fr.refs.map(r => r.domain)))
      mainPairs.set(`${fr.domain}=>${d}`, (mainPairs.get(`${fr.domain}=>${d}`) || 0) + 1);
  const fwdFiles = new Set(mainScan.forwardInventory.map(f => f.file));
  const testFwd = testScan.filter(tf => tf.imports.some(im => direction(tf.domain, im.domain) === 'forward'));
  const out = {
    freezeOrder: FROZEN_ORDER,
    mainScope: {
      couplingFileCount: mainScan.files.length,
      forwardCouplingFileCount: fwdFiles.size,
      domainPairCount: mainPairs.size,
      domainPairs: Object.fromEntries([...mainPairs.entries()].sort()),
    },
    testScope: {
      couplingFileCount: testScan.length,
      forwardCouplingFileCount: testFwd.length,
      files: testScan.map(tf => ({
        domain: tf.domain, scope: tf.scope, file: tf.file,
        refs: [...new Map(tf.imports.map(im => [im.domain + ':' + im.symbol, im])).values()].map(im => ({
          domain: im.domain, symbol: im.symbol, line: im.line, direction: direction(tf.domain, im.domain),
        })),
      })),
    },
    forwardInventory: mainScan.forwardInventory,
    mainFiles: mainScan.files.map(fr => ({
      domain: fr.domain, layer: fr.layer, file: fr.file,
      foreignDomains: [...new Set(fr.refs.map(r => r.domain))].sort(),
      idLineCount: fr.idLines.length,
      refs: fr.refs.map(r => ({
        line: r.line, domain: r.domain, kind: r.kind, symbol: r.simple,
        direction: direction(fr.domain, r.domain), commentLevel: r.commentLevel,
      })),
    })),
  };
  writeFileSync(path.join(rootDir, dirFile), JSON.stringify(out) + '\n');
}
if (arg('registry-out')) {
  const entries = emitRegistry(ormGraph, mainScan, testScan);
  const counts = {};
  for (const e of entries) counts[e.kind + (e.scope ? ':' + e.scope : '')] = (counts[e.kind + (e.scope ? ':' + e.scope : '')] || 0) + 1;
  const header = `// id-string-migration 前向耦合登记册（M0.2 产出，机器可读）
// 生成命令: node tools/scan-id-coupling-directions.mjs --orm-graph=... --directions=... --registry-out=tools/id-migration-registry.json5
// 消费方: tools/check-bigint-id-types.mjs（仅 orm-column-deferral 条目，active 生效）、tools/verify-id-fix-copy-diff.mjs（同）
// 退役协议: 条目 status: active -> retired 由 retireOwner 工作项执行（M2.7 退役 orm 级 8 条；service-bridge 由晚域 plan 翻转 IBiz 参数时退役）
// 注意: 重新生成会重置 status——退役后本文件为手工维护权威，重跑生成器仅用于对账复核
`;
  writeFileSync(path.join(rootDir, arg('registry-out')),
    header + JSON.stringify({ entries, counts }, null, 2) + '\n');
}
const rec = arg('reconcile');
let recOut = null;
if (rec !== null) {
  recOut = reconcile(mainScan);
  if (arg('reconcile-file')) writeFileSync(path.join(rootDir, arg('reconcile-file')), JSON.stringify(recOut) + '\n');
}

console.log(`orm 跨域边: ${ormGraph.edgeCount}（前向 ${ormGraph.forwardEdgeCount} / 后向 ${ormGraph.backwardEdgeCount}）`);
console.log(`前向簇: ${JSON.stringify(ormGraph.forwardClusters)}`);
console.log(`main 耦合文件: ${mainScan.files.length}  前向清册条目: ${mainScan.forwardInventory.length}`);
console.log(`test 耦合文件: ${testScan.length}（含前向引用: ${testScan.filter(tf => tf.imports.some(im => direction(tf.domain, im.domain) === 'forward')).length}）`);
if (recOut) console.log(`附录 A 对账: ${recOut.appendixAFilePairCount} vs ${recOut.thisScanFilePairCount}  match=${recOut.match}`);
