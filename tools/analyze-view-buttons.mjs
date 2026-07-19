#!/usr/bin/env node
// analyze-view-buttons.mjs
//
// Scan all view.xml files under _dump/nop-app/, extract buttons/actions
// per page per entity, and write one markdown report per domain to
// _tmp/view-buttons/.
//
// Usage:
//   node analyze-view-buttons.mjs
//   node analyze-view-buttons.mjs --dump-dir path/to/_dump/nop-app
//   node analyze-view-buttons.mjs --out-dir _tmp/my-report
//
// Output: _tmp/view-buttons/{domain}.md + SUMMARY.md

import { readFileSync, readdirSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, relative } from 'node:path';

const OUT = join(process.cwd(), '_tmp', 'view-buttons');

// ── Baseline patterns ──

const CRUD_TOOLBAR = ['add-button', 'batch-delete-button'];
const CRUD_ROW = ['row-view-button', 'row-update-button', 'row-delete-button'];
const WORKFLOW_ROW = [
  'row-submit-button', 'row-withdraw-approval-button',
  'row-approve-button', 'row-reject-button', 'row-reverse-approve-button',
  'row-cancel-button',
];

function parseArgs(argv) {
  const opts = { dumpDir: null, outDir: null };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--dump-dir') opts.dumpDir = argv[++i];
    else if (a === '--out-dir') opts.outDir = argv[++i];
  }
  return opts;
}

function findViewFiles(dir) {
  const result = [];
  try {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const fp = join(dir, entry.name);
      if (entry.isDirectory()) result.push(...findViewFiles(fp));
      else if (entry.name.endsWith('.view.xml')) result.push(fp);
    }
  } catch (e) {
    if (e.code === 'ENOENT') process.stderr.write(`ERROR: not found: ${dir}\n`);
    else throw e;
  }
  return result;
}

function parseAttrs(str) {
  const attrs = {};
  const re = /([\w-]+)="([\s\S]*?)"/g;
  let m;
  while ((m = re.exec(str)) !== null) attrs[m[1]] = m[2];
  return attrs;
}

function extractSubElements(block) {
  const r = {};
  const apiUrl = block.match(/<api\s+url="([^"]*)"/);
  if (apiUrl) r.apiUrl = apiUrl[1];
  const dialogPage = block.match(/<dialog\s+page="([^"]*)"/);
  if (dialogPage) r.dialogPage = dialogPage[1];
  const confirmText = block.match(/<confirmText>([\s\S]*?)<\/confirmText>/);
  if (confirmText) r.confirmText = confirmText[1].trim();
  const visibleOn = block.match(/<visibleOn>([\s\S]*?)<\/visibleOn>/);
  if (visibleOn) r.visibleOn = visibleOn[1].trim();
  return r;
}

function findTag(xml, tagName, startIdx = 0) {
  const openStart = xml.indexOf(`<${tagName}`, startIdx);
  if (openStart === -1) return null;
  const openEnd = xml.indexOf('>', openStart);
  if (openEnd === -1) return null;
  if (xml[openEnd - 1] === '/') return { content: '', start: openStart, end: openEnd + 1 };
  const closeTag = `</${tagName}>`;
  const contentStart = openEnd + 1;
  const closeIdx = xml.indexOf(closeTag, contentStart);
  if (closeIdx === -1) return null;
  return { content: xml.slice(contentStart, closeIdx), start: openStart, end: closeIdx + closeTag.length };
}

function getTag(xml, tagName) { return findTag(xml, tagName, 0); }

// ── Action parsing ──

function parseActions(xml) {
  const acts = [];
  if (!xml) return acts;
  const re = /<action\s+([\s\S]*?)<\/action>|<action\s+([^>]*?)\/>/g;
  let m;
  while ((m = re.exec(xml)) !== null) {
    const attrStr = m[1] || m[2];
    if (!attrStr) continue;
    const attrs = parseAttrs(attrStr);
    const sub = extractSubElements(m[0]);
    acts.push({ id: attrs.id || '', label: attrs.label || '', level: attrs.level || 'default',
      icon: attrs.icon || '', actionType: attrs.actionType || '', batch: attrs.batch === 'true', ...sub });
  }
  return acts;
}

function extractActionsFromContainer(containerXml) {
  const acts = [];
  if (!containerXml) return acts;
  acts.push(...parseActions(containerXml));
  const grp = getTag(containerXml, 'actionGroup');
  if (grp) acts.push(...parseActions(grp.content));
  return acts;
}

function extractPage(pageXml) {
  const listSec = getTag(pageXml, 'listActions');
  const rowSec = getTag(pageXml, 'rowActions');
  const formSec = getTag(pageXml, 'actions');
  return {
    toolbar: listSec ? extractActionsFromContainer(listSec.content) : [],
    row: rowSec ? extractActionsFromContainer(rowSec.content) : [],
    form: formSec ? parseActions(formSec.content) : [],
  };
}

// ── Page finder ──

function findPages(xml) {
  const pages = {};
  const pagesSec = getTag(xml, 'pages');
  if (!pagesSec) return pages;

  const crudRe = /<crud\s+name="([^"]*)"[\s\S]*?<\/crud>/g;
  let m;
  while ((m = crudRe.exec(pagesSec.content)) !== null) {
    pages[m[1]] = { type: 'crud', ...extractPage(m[0]) };
  }
  const simpleRe = /<simple\s+name="([^"]*)"[\s\S]*?<\/simple>/g;
  while ((m = simpleRe.exec(pagesSec.content)) !== null) {
    pages[m[1]] = { type: 'simple', ...extractPage(m[0]) };
  }
  const pickerRe = /<picker\s+name="([^"]*)"[\s\S]*?<\/picker>/g;
  while ((m = pickerRe.exec(pagesSec.content)) !== null) {
    pages[m[1]] = { type: 'picker', toolbar: [], row: [], form: [] };
  }
  return pages;
}

function parseViewFile(fp) {
  const raw = readFileSync(fp, 'utf-8').replace(/<!--[\s\S]*?-->/g, '');
  const bizObjName = (raw.match(/bizObjName="([^"]+)"/) || [])[1] || '';
  return { bizObjName, pages: findPages(raw) };
}

// ── Helpers ──

function domainFromPath(path) {
  const parts = path.split('/');
  const idx = parts.indexOf('erp');
  if (idx !== -1 && idx + 1 < parts.length) return parts[idx + 1];
  const nopIdx = parts.indexOf('nop');
  if (nopIdx !== -1 && nopIdx + 1 < parts.length) return `nop/${parts[nopIdx + 1]}`;
  return '';
}

function entityName(path) {
  return (path.match(/(\w+)\.view\.xml$/) || [])[1] || '';
}

const PRIORITY = {
  'add-button': 1, 'batch-delete-button': 2,
  'row-view-button': 3, 'row-update-button': 4,
  'row-submit-button': 5, 'row-withdraw-approval-button': 6,
  'row-approve-button': 7, 'row-reject-button': 8,
  'row-reverse-approve-button': 9, 'row-cancel-button': 10,
  'row-delete-button': 11, 'row-more-button': 99,
};

function sortActions(acts) {
  return [...acts].sort((a, b) => (PRIORITY[a.id] || 50) - (PRIORITY[b.id] || 50));
}

function dedupe(acts) {
  const seen = new Set();
  return acts.filter(a => { const k = a.id; if (seen.has(k)) return false; seen.add(k); return true; });
}

function actionSummary(acts) {
  return acts.map(a => {
    let s = `\`${a.id}\``;
    if (a.batch) s += ' 🗂';
    if (a.actionType) s += ` (${a.actionType})`;
    return s;
  }).join('<br>');
}

function actionShort(acts) {
  return acts.map(a => a.id).join(', ');
}

// ── Classification ──

function classifyMain(main) {
  if (!main || main.type !== 'crud') return { kind: 'other' };
  const tb = main.toolbar.map(a => a.id);
  const row = main.row.map(a => a.id);
  const extraToolbar = tb.filter(a => !CRUD_TOOLBAR.includes(a));
  const missingToolbar = CRUD_TOOLBAR.filter(a => !tb.includes(a));
  const extraRow = row.filter(a => !CRUD_ROW.includes(a) && !WORKFLOW_ROW.includes(a));
  const missingRow = CRUD_ROW.filter(a => !row.includes(a));
  const hasWf = row.some(a => WORKFLOW_ROW.includes(a));

  const isCrud = !hasWf && extraToolbar.length === 0 && missingToolbar.length === 0
    && extraRow.length === 0 && missingRow.length === 0;
  if (isCrud) return { kind: 'crud' };

  const isWf = extraToolbar.length === 0 && missingToolbar.length === 0
    && extraRow.length === 0 && missingRow.length === 0 && hasWf;
  if (isWf) return { kind: 'workflow' };

  return { kind: 'custom', extraToolbar, missingToolbar, extraRow, missingRow, hasWf };
}

// ── Render domain page ──

function renderEntity(entityPages, parsed, filePath) {
  const main = parsed.pages['main'];
  const cls = classifyMain(main);
  const lines = [];
  const entLink = `[${parsed.bizObjName || entityName(filePath)}](${filePath})`;
  const pageNames = Object.keys(parsed.pages);

  lines.push(`### ${entLink}`);

  // Classification badge
  if (cls.kind === 'crud') lines.push('> **CRUD** — standard add/batch-delete/view/update/delete');
  else if (cls.kind === 'workflow') lines.push('> **CRUD + workflow** — standard + submit/approve/reject/cancel');
  else {
    const tags = [];
    if (cls.extraToolbar?.length) tags.push(`toolbar+=[${cls.extraToolbar.join(', ')}]`);
    if (cls.missingToolbar?.length) tags.push(`toolbar-=[${cls.missingToolbar.join(', ')}]`);
    if (cls.extraRow?.length) tags.push(`row+=[${cls.extraRow.join(', ')}]`);
    if (cls.missingRow?.length) tags.push(`row-=[${cls.missingRow.join(', ')}]`);
    lines.push(`> **Custom** — ${tags.join('; ')}`);
  }

  lines.push('');
  lines.push('| Page | Type | Actions |');
  lines.push('|------|------|---------|');

  const orderedPages = ['main', 'add', 'view', 'update', ...pageNames.filter(n => !['main', 'add', 'view', 'update', 'picker'].includes(n)), 'picker'];
  for (const name of orderedPages) {
    const pg = parsed.pages[name];
    if (!pg) continue;
    const parts = [];
    if (pg.toolbar.length) parts.push(`toolbar: ${actionShort(sortActions(dedupe(pg.toolbar)))}`);
    if (pg.row.length) parts.push(`row: ${actionShort(sortActions(dedupe(pg.row)))}`);
    if (pg.form.length) parts.push(`form: ${pg.form.map(a => `${a.id}${a.actionType ? ` (${a.actionType})` : ''}`).join(', ')}`);
    if (parts.length === 0) {
      if (pg.type === 'picker') continue; // skip empty picker
      lines.push(`| ${name} | ${pg.type} | _(no actions)_ |`);
    } else {
      lines.push(`| ${name} | ${pg.type} | ${parts.join('; ')} |`);
    }
  }
  lines.push('');
  return lines.join('\n');
}

function renderDomain(domain, entities) {
  const lines = [];
  lines.push(`# ${domain}`);
  lines.push('');
  lines.push(`Total entities: ${entities.length}`);
  lines.push('');

  for (const e of entities) {
    lines.push(renderEntity(e.pages, e.parsed, e.filePath));
  }
  return lines.join('\n');
}

// ── Main ──

function main() {
  const opts = parseArgs(process.argv);
  const rootDir = process.cwd();
  const dumpDir = opts.dumpDir || join(rootDir, '_dump', 'nop-app');
  const outDir = opts.outDir || OUT;

  const viewFiles = findViewFiles(dumpDir);
  if (!viewFiles.length) { process.stderr.write(`No view.xml under ${dumpDir}\n`); process.exit(0); }

  // Parse all files, group by domain
  const byDomain = {};
  for (const fp of viewFiles) {
    const domain = domainFromPath(fp);
    if (!domain) continue;
    if (!byDomain[domain]) byDomain[domain] = [];
    const filePath = relative(dumpDir, fp);
    try {
      const parsed = parseViewFile(fp);
      byDomain[domain].push({ filePath, parsed, pages: parsed.pages });
    } catch (e) {
      process.stderr.write(`WARN: ${fp}: ${e.message}\n`);
    }
  }

  // Ensure output dir
  mkdirSync(outDir, { recursive: true });

  // Write per-domain files
  let totalCrud = 0, totalWf = 0, totalCustom = 0, totalOther = 0;
  for (const [domain, entities] of Object.entries(byDomain).sort()) {
    const content = renderDomain(domain, entities);
    const sanitized = domain.replace('/', '-');
    writeFileSync(join(outDir, `${sanitized}.md`), content, 'utf-8');

    // Count classifications
    for (const e of entities) {
      const cls = classifyMain(e.parsed.pages['main']);
      if (cls.kind === 'crud') totalCrud++;
      else if (cls.kind === 'workflow') totalWf++;
      else if (cls.kind === 'custom') totalCustom++;
      else totalOther++;
    }
  }

  // Write summary
  const summaryLines = [];
  const totalPages = Object.values(byDomain).flat().length;
  summaryLines.push('# View Button/Action Analysis — Summary');
  summaryLines.push('');
  summaryLines.push(`Generated from: \`${dumpDir}\``);
  summaryLines.push(`Total entity pages: ${totalPages}`);
  summaryLines.push(`Output files: \`${outDir}/{domain}.md\``);
  summaryLines.push('');
  summaryLines.push('## Classification');
  summaryLines.push('');
  summaryLines.push('| Category | Count | Description |');
  summaryLines.push('|----------|-------|-------------|');
  summaryLines.push(`| CRUD | ${totalCrud} | Standard add/batch-delete/view/update/delete |`);
  summaryLines.push(`| CRUD + workflow | ${totalWf} | CRUD + submit/approve/reject workflow |`);
  summaryLines.push(`| Custom | ${totalCustom} | Has non-standard actions (see detail) |`);
  summaryLines.push(`| Other | ${totalOther} | No standard CRUD main page |`);
  summaryLines.push(`| **Total** | **${totalPages}** | |`);
  summaryLines.push('');
  summaryLines.push('## Domains');
  summaryLines.push('');
  summaryLines.push('| Domain | Entities | Files |');
  summaryLines.push('|--------|----------|-------|');
  for (const [domain, entities] of Object.entries(byDomain).sort()) {
    const sanitized = domain.replace('/', '-');
    const fileLink = `[\`${sanitized}.md\`](${sanitized}.md)`;
    summaryLines.push(`| ${domain} | ${entities.length} | ${fileLink} |`);
  }
  writeFileSync(join(outDir, 'SUMMARY.md'), summaryLines.join('\n'), 'utf-8');

  process.stdout.write(`Done. ${totalPages} entities across ${Object.keys(byDomain).length} domains.\n`);
  process.stdout.write(`Output: ${outDir}/SUMMARY.md + {domain}.md\n`);
}

main();
