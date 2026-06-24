#!/usr/bin/env node
/**
 * 用例-菜单对照与完备性检查工具
 *
 * 作用(详见 docs/design/use-case-authoring-guide.md §4):
 *   1. 菜单→用例对照:解析 action-auth.xml 的 app:useCases,列举每个菜单关联的用例
 *   2. 完备性检查:识别"无用例关联的功能菜单"(遗漏场景设计)与"无菜单引用的孤儿用例"
 *   3. 用例统计/概览:按域统计用例数、菜单覆盖率
 *
 * 用法:
 *   node tools/use-case-map.js              # 完整报告(对照 + 完备性 + 概览)
 *   node tools/use-case-map.js --menu       # 仅菜单→用例对照
 *   node tools/use-case-map.js --coverage   # 仅完备性检查
 *   node tools/use-case-map.js --overview   # 仅域级概览
 *
 * 无外部依赖,纯 Node.js(用正则解析,不引第三方库)。
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

// ============ 数据收集 ============

/** 收集所有业务域 action-auth.xml 的菜单与 app:useCases 关联 */
function collectMenuCases() {
  const results = []; // { domain, moduleId, resourceId, displayName, url, userCases:[] }
  const domains = ['md','pur','sal','inv','fin','ast','prj','mfg','mnt','qa'];
  for (const short of domains) {
    const pattern = `${ROOT}/module-*/erp-${short}-web/src/main/resources/_vfs/erp/${short}/auth/erp-${short}.action-auth.xml`;
    // glob 简易实现
    const dir = findAuthDir(short);
    if (!dir) continue;
    const content = fs.readFileSync(dir, 'utf-8');
    // 匹配所有 <resource ...>(跨行),提取 id/displayName/url/app:useCases
    const re = /<resource\b([^>]*?)>/gs;
    let m;
    while ((m = re.exec(content)) !== null) {
      const attrs = m[1];
      const idMatch = attrs.match(/id="([^"]+)"/);
      if (!idMatch) continue;
      const rid = idMatch[1];
      // 只看叶子菜单(有 url 或 app:useCases 的),跳过纯分组(无 url 无 userCase)
      const urlMatch = attrs.match(/\surl="([^"]+)"/);
      const ucMatch = attrs.match(/app:useCases="([^"]+)"/);
      const dnMatch = attrs.match(/displayName="([^"]+)"/);
      if (!urlMatch && !ucMatch) continue; // 纯分组菜单,跳过
      const userCases = ucMatch ? ucMatch[1].split(',').map(s => s.trim()).filter(Boolean) : [];
      results.push({
        domain: short,
        resourceId: rid,
        displayName: dnMatch ? dnMatch[1] : rid,
        url: urlMatch ? urlMatch[1] : '',
        userCases,
      });
    }
  }
  return results;
}

/** 找到域的 action-auth 文件路径 */
function findAuthDir(short) {
  const moduleDir = `${ROOT}/module-${domainName(short)}`;
  if (!fs.existsSync(moduleDir)) return null;
  const webDir = `${moduleDir}/erp-${short}-web`;
  if (!fs.existsSync(webDir)) return null;
  const authFile = `${webDir}/src/main/resources/_vfs/erp/${short}/auth/erp-${short}.action-auth.xml`;
  return fs.existsSync(authFile) ? authFile : null;
}

function domainName(short) {
  return {md:'master-data',pur:'purchase',sal:'sales',inv:'inventory',fin:'finance',
    ast:'assets',prj:'projects',mfg:'manufacturing',mnt:'maintenance',qa:'quality'}[short];
}

/** 收集所有 use-cases.md 的用例编号与标题 */
function collectUseCases() {
  const results = []; // { domain, code, title }
  const domains = ['master-data','purchase','sales','inventory','finance','assets','projects','manufacturing','maintenance','quality'];
  for (const dom of domains) {
    const f = `${ROOT}/docs/design/${dom}/use-cases.md`;
    if (!fs.existsSync(f)) continue;
    const content = fs.readFileSync(f, 'utf-8');
    const short = {masterdata:'md'}.masterdata || dom.slice(0,3); // 简化,实际用编号里的域简称
    // 匹配 ## UC-XXX-NN 标题
    const re = /^## (UC-[A-Z]+-\d+)\s+(.+)$/gm;
    let m;
    while ((m = re.exec(content)) !== null) {
      results.push({ code: m[1], title: m[2].trim(), domain: dom });
    }
  }
  return results;
}

// ============ 报告生成 ============

function reportMenuCases(menus, useCases) {
  console.log('\n== 菜单→用例对照 ==');
  // 建立用例编号→标题映射,用于把编号解析为「编号 + 标题」
  const codeToTitle = {};
  for (const uc of useCases) {
    codeToTitle[uc.code] = uc.title;
  }
  const byDomain = {};
  for (const m of menus) {
    (byDomain[m.domain] = byDomain[m.domain] || []).push(m);
  }
  for (const dom of Object.keys(byDomain).sort()) {
    console.log(`\n[${dom}]`);
    for (const m of byDomain[dom]) {
      if (m.userCases.length === 0) {
        console.log(`  ${m.displayName.padEnd(20)} (${m.resourceId}) → (无关联)`);
      } else {
        // 每个用例编号解析为「编号 + 标题」,找不到标题的只显示编号
        const caseLines = m.userCases.map(c => {
          const title = codeToTitle[c];
          return title ? `    • ${c} ${title}` : `    • ${c} (标题未找到)`;
        });
        console.log(`  ${m.displayName.padEnd(20)} (${m.resourceId}):`);
        for (const line of caseLines) console.log(line);
      }
    }
  }
}

function reportCoverage(menus, useCases) {
  console.log('\n== 完备性检查 ==');
  let hasFinding = false;

  // 1. 无用例关联的功能菜单(有 url 但无 app:useCases)
  const noCaseMenus = menus.filter(m => m.url && m.userCases.length === 0);
  if (noCaseMenus.length) {
    hasFinding = true;
    console.log(`\n⚠ 无用例关联的功能菜单(可能遗漏场景设计) ${noCaseMenus.length} 个:`);
    for (const m of noCaseMenus) {
      console.log(`  [${m.domain}] ${m.displayName} (${m.resourceId})`);
    }
  }

  // 2. 无菜单引用的孤儿用例
  const referencedCases = new Set();
  for (const m of menus) {
    for (const c of m.userCases) referencedCases.add(c);
  }
  const orphanCases = useCases.filter(uc => !referencedCases.has(uc.code));
  if (orphanCases.length) {
    hasFinding = true;
    console.log(`\n⚠ 无菜单引用的孤儿用例 ${orphanCases.length} 个:`);
    for (const uc of orphanCases) {
      console.log(`  ${uc.code} ${uc.title}`);
    }
  }

  // 3. 引用了不存在用例的菜单(笔误)
  const validCodes = new Set(useCases.map(uc => uc.code));
  const dangling = [];
  for (const m of menus) {
    for (const c of m.userCases) {
      if (!validCodes.has(c)) dangling.push({ menu: m, code: c });
    }
  }
  if (dangling.length) {
    hasFinding = true;
    console.log(`\n⚠ 引用了不存在用例编号的菜单 ${dangling.length} 处(可能笔误):`);
    for (const d of dangling) {
      console.log(`  [${d.menu.domain}] ${d.menu.resourceId} → ${d.code} (用例不存在)`);
    }
  }

  if (!hasFinding) {
    console.log('\n✓ 完备性检查通过:无遗漏菜单、无孤儿用例、无悬空引用。');
  }
}

function reportOverview(menus, useCases) {
  console.log('\n== 域级概览 ==');
  // 用例编号简称 → 工程简称 别名(统一为工程目录简称,小写)
  const normalizeDomain = d => d.toLowerCase() === 'main' ? 'mnt' : d.toLowerCase();
  const byDomain = {};
  // 用例按域简称分组(统一小写做 key,避免大小写分裂)
  for (const uc of useCases) {
    const domShort = normalizeDomain(uc.code.split('-')[1]);
    (byDomain[domShort] = byDomain[domShort] || { cases: 0, menus: 0, covered: 0 });
    byDomain[domShort].cases++;
  }
  // 菜单按域 + 是否有用例
  const referencedCases = new Set();
  for (const m of menus) {
    const domShort = normalizeDomain(m.domain);
    (byDomain[domShort] = byDomain[domShort] || { cases: 0, menus: 0, covered: 0 });
    if (m.url) byDomain[domShort].menus++;
    for (const c of m.userCases) {
      referencedCases.add(c);
    }
  }
  // 被菜单引用的用例数(按域)
  for (const uc of useCases) {
    if (referencedCases.has(uc.code)) {
      const domShort = normalizeDomain(uc.code.split('-')[1]);
      if (byDomain[domShort]) byDomain[domShort].covered++;
    }
  }

  console.log('域            用例数  功能菜单数  被引用用例  覆盖率');
  let totalCases = 0, totalCovered = 0;
  for (const dom of Object.keys(byDomain).sort()) {
    const d = byDomain[dom];
    const rate = d.cases ? Math.round(d.covered / d.cases * 100) : 0;
    console.log(`${dom.padEnd(12)}  ${String(d.cases).padStart(4)}    ${String(d.menus).padStart(6)}      ${String(d.covered).padStart(6)}      ${rate}%`);
    totalCases += d.cases;
    totalCovered += d.covered;
  }
  console.log(`${'合计'.padEnd(12)}  ${String(totalCases).padStart(4)}    ${''.padStart(6)}      ${String(totalCovered).padStart(6)}      ${totalCases ? Math.round(totalCovered/totalCases*100) : 0}%`);
}

// ============ 主入口 ============

function main() {
  const arg = process.argv[2] || '';
  const menus = collectMenuCases();
  const useCases = collectUseCases();

  if (!arg || arg === '--menu') reportMenuCases(menus, useCases);
  if (!arg || arg === '--coverage') reportCoverage(menus, useCases);
  if (!arg || arg === '--overview') reportOverview(menus, useCases);

  if (!arg) {
    console.log(`\n(共 ${menus.length} 个功能菜单, ${useCases.length} 个用例)`);
  }
}

main();
