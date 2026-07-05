#!/usr/bin/env node
/**
 * Add high-frequency filter <indexes> to ORM model files + emit derived _create_index.sql.
 *
 * Index strategy (docs/plans/2026-07-05-2352-1-db-index-design.md Phase 1 decisions):
 *   - Naming: IDX_{TABLE_WITHOUT_erp_PREFIX}_{COLS}, uppercase. orgId segment -> ORG, others -> column CODE.
 *   - Entities with orgId: compound (orgId, docStatus) [primary status], (orgId, businessDate).
 *     approveStatus indexed only when docStatus absent (avoid redundant org-prefix compounds).
 *   - Global entities (no orgId): single-column on docStatus/approveStatus/businessDate.
 *   - status column: indexed only when no docStatus/approveStatus and dict is NOT *-active-status (skip binary).
 *   - posted (BOOLEAN): never indexed alone (low selectivity, per Decision 3).
 *   - delVersion / id / code (UK-covered) / text-low-freq: never indexed.
 *   - Every FK column (*Id, excluding id/orgId/delVersion): single-column index (navigation high-freq).
 *
 * SQL generation:
 *   The platform _create_{app}.sql.xgen template (ddl.xlib CreateTables) does NOT emit CREATE INDEX
 *   (confirmed vs nop-app-mall pattern). Runtime ddl.xlib AddIndex applies them at startup from the
 *   model; for manual production deployment parity we emit deploy/sql/{dialect}/_create_index.sql
 *   as a derived artifact (regenerable from this script).
 *
 * Usage:
 *   node scripts/add-orm-indexes.js            # process all 18 modules (ORM + SQL)
 *   node scripts/add-orm-indexes.js module-pur # single module
 *   node scripts/add-orm-indexes.js --sql-only # regenerate SQL only (from existing <indexes>)
 *
 * Idempotent: existing <indexes> blocks are replaced, not duplicated.
 * Reference stubs (entity without className=) are skipped.
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const DIALECTS = ['mysql', 'postgresql', 'oracle'];

// Require className="..." in the opening tag: this (a) skips reference stubs (name= only,
// no className) and (b) avoids matching literal <entity> tokens inside header comments
// (e.g. the naming-convention doc "表名: erp_pur_<entity>").
const ENTITY_RE = /([ \t]*)<entity\b([^>]*?\bclassName="[^"]+"[^>]*?)>([\s\S]*?)<\/entity>/g;
const COL_RE = /<column\b([^>]*?)\/>/g;
const ATTR_RE = (name) => new RegExp(`\\b${name}="([^"]+)"`);

function parseAttrs(str) {
  const out = {};
  for (const key of ['name', 'code', 'stdSqlType', 'ext:dict', 'displayName']) {
    const m = str.match(ATTR_RE(key));
    if (m) out[key] = m[1];
  }
  return out;
}

function isLowCardStatus(col) {
  if (col.stdSqlType === 'BOOLEAN') return true;
  const dict = col['ext:dict'] || '';
  return /active-status$/i.test(dict) || /\/active$/i.test(dict);
}

function segFor(colName, colMap) {
  if (colName === 'orgId') return 'ORG';
  const c = colMap[colName];
  if (!c || !c.code) return colName.replace(/([A-Z])/g, '_$1').replace(/^_/, '').toUpperCase();
  return c.code;
}

function dbCol(colName, colMap) {
  const c = colMap[colName];
  if (!c || !c.code) return colName;
  return c.code.toLowerCase();
}

function tableNoPrefix(tableName) {
  return tableName.replace(/^erp_/, '').toUpperCase();
}

function planIndexes(cols) {
  const colMap = {};
  for (const c of cols) colMap[c.name] = c;
  const has = (n) => !!colMap[n];
  const hasOrg = has('orgId');
  const idx = []; // {cols:[propName,...]}

  const docStatus = has('docStatus');
  const approveStatus = has('approveStatus');

  if (docStatus) {
    idx.push({ cols: hasOrg ? ['orgId', 'docStatus'] : ['docStatus'] });
  } else if (approveStatus) {
    idx.push({ cols: hasOrg ? ['orgId', 'approveStatus'] : ['approveStatus'] });
  } else if (has('status') && !isLowCardStatus(colMap['status'])) {
    idx.push({ cols: hasOrg ? ['orgId', 'status'] : ['status'] });
  }

  if (has('businessDate')) {
    idx.push({ cols: hasOrg ? ['orgId', 'businessDate'] : ['businessDate'] });
  }

  for (const c of cols) {
    const n = c.name;
    if (n === 'id' || n === 'orgId' || n === 'delVersion') continue;
    if (/Id$/.test(n)) idx.push({ cols: [n] });
  }
  return idx;
}

function buildIndexesBlock(indexes, baseIndent) {
  if (!indexes.length) return null;
  let s = baseIndent + '<indexes>';
  for (const ix of indexes) {
    s += '\n' + baseIndent + '    <index name="' + ix.name + '" unique="false">';
    for (const c of ix.cols) {
      s += '\n' + baseIndent + '        <column name="' + c + '"/>';
    }
    s += '\n' + baseIndent + '    </index>';
  }
  s += '\n' + baseIndent + '</indexes>';
  return s;
}

function processOrmFile(ormFile) {
  let content = fs.readFileSync(ormFile, 'utf8');
  const allIndexes = []; // {tableName, idxName, dbCols[]}
  let entityCount = 0;
  let indexCount = 0;

  content = content.replace(ENTITY_RE, (match, indent, attrsRaw, body) => {
    const className = (attrsRaw.match(ATTR_RE('className')) || [])[1];
    if (!className) return match; // reference stub
    const tableName = (attrsRaw.match(ATTR_RE('tableName')) || [])[1];
    if (!tableName) return match;

    const cols = [];
    // Parse columns ONLY from the <columns> section, so <column> entries inside
    // <indexes>/<index> are not mistaken for entity columns (idempotency on re-runs).
    const colsSection = (body.match(/<columns>([\s\S]*?)<\/columns>/) || [])[1] || '';
    let m;
    const re = new RegExp(COL_RE);
    while ((m = re.exec(colsSection)) !== null) {
      cols.push(parseAttrs(m[1]));
    }

    const colMap = {};
    for (const c of cols) colMap[c.name] = c;
    const planned = planIndexes(cols);
    if (!planned.length) return match;

    const named = planned.map((p) => {
      const segs = p.cols.map((c) => segFor(c, colMap));
      const name = 'IDX_' + tableNoPrefix(tableName) + '_' + segs.join('_');
      return { name, cols: p.cols, dbCols: p.cols.map((c) => dbCol(c, colMap)) };
    });

    entityCount++;
    indexCount += named.length;
    for (const n of named) {
      allIndexes.push({ tableName, idxName: n.name, dbCols: n.dbCols });
    }

    const idxBlock = buildIndexesBlock(named, indent + '    ');
    let nb = body.replace(/[ \t]*<indexes>[\s\S]*?<\/indexes>\s*\n?/g, '');
    nb = nb.replace(/\s+$/, '');
    nb += '\n' + idxBlock;
    return indent + '<entity' + attrsRaw + '>' + nb + '\n' + indent + '</entity>';
  });

  fs.writeFileSync(ormFile, content, 'utf8');
  return { entityCount, indexCount, allIndexes };
}

function writeIndexSql(domainDir, appName, allIndexes) {
  const header = (dialect) => [
    `-- ${appName} indexes generated from model/app-erp-${appName.replace(/^app-erp-/, '')}.orm.xml`,
    `-- Dialect: ${dialect}.`,
    `-- This file is maintained separately from _create_${appName.replace(/^app-erp-/, 'erp-')}.sql because the`,
    `-- platform _create.sql.xgen template (ddl.xlib CreateTables) does not emit CREATE INDEX.`,
    `-- Runtime: ddl.xlib AddIndex also applies these at app startup from the ORM model.`,
    `-- Run AFTER _create_${appName.replace(/^app-erp-/, 'erp-')}.sql during manual production deployment.`,
    `-- Source of truth: model <indexes> definitions (${allIndexes.length} indexes).`,
    `-- Regenerate: node scripts/add-orm-indexes.js`,
    '',
  ].join('\n');

  const body = allIndexes
    .map((ix) => `CREATE INDEX ${ix.idxName} ON ${ix.tableName} (${ix.dbCols.join(', ')});`)
    .join('\n');

  for (const dialect of DIALECTS) {
    const dir = path.join(domainDir, 'deploy', 'sql', dialect);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const file = path.join(dir, '_create_index.sql');
    fs.writeFileSync(file, header(dialect) + body + '\n', 'utf8');
  }
}

function discoverModules(filter) {
  const modules = fs
    .readdirSync(ROOT)
    .filter((d) => d.startsWith('module-') && fs.statSync(path.join(ROOT, d)).isDirectory());
  return modules.filter((d) => !filter || d.includes(filter));
}

function main() {
  const args = process.argv.slice(2);
  const sqlOnly = args.includes('--sql-only');
  const filter = args.filter((a) => !a.startsWith('--'))[0];

  const modules = discoverModules(filter);
  let totalIdx = 0;
  let totalEnt = 0;
  const summary = [];

  for (const mod of modules) {
    const ormDir = path.join(ROOT, mod, 'model');
    if (!fs.existsSync(ormDir)) continue;
    const ormFiles = fs.readdirSync(ormDir).filter((f) => f.endsWith('.orm.xml'));
    for (const ormFileName of ormFiles) {
      const ormFile = path.join(ormDir, ormFileName);
      let res;
      if (!sqlOnly) {
        res = processOrmFile(ormFile);
      } else {
        res = collectIndexesOnly(ormFile);
      }
      totalIdx += res.indexCount;
      totalEnt += res.entityCount;
      const appName = ormFileName.replace(/^app-erp-/, '').replace(/\.orm\.xml$/, '');
      const domainDir = path.join(ROOT, mod);
      writeIndexSql(domainDir, 'app-erp-' + appName, res.allIndexes);
      summary.push(`${mod}/${ormFileName}: ${res.entityCount} entities, ${res.indexCount} indexes`);
    }
  }

  console.log(summary.join('\n'));
  console.log(`\nTOTAL: ${totalEnt} entities indexed, ${totalIdx} indexes across ${modules.length} modules.`);
}

function collectIndexesOnly(ormFile) {
  // Re-derive index list from existing <indexes> blocks without modifying the ORM.
  const content = fs.readFileSync(ormFile, 'utf8');
  const allIndexes = [];
  let entityCount = 0;
  let indexCount = 0;
  content.replace(ENTITY_RE, (match, indent, attrsRaw, body) => {
    const className = (attrsRaw.match(ATTR_RE('className')) || [])[1];
    if (!className) return match;
    const tableName = (attrsRaw.match(ATTR_RE('tableName')) || [])[1];
    if (!tableName) return match;

    // need columns for dbCol mapping
    const cols = [];
    let m;
    const re = new RegExp(COL_RE);
    while ((m = re.exec(body)) !== null) cols.push(parseAttrs(m[1]));
    const colMap = {};
    for (const c of cols) colMap[c.name] = c;

    const idxBlockRe = /<indexes>([\s\S]*?)<\/indexes>/g;
    let im;
    while ((im = idxBlockRe.exec(body)) !== null) {
      const block = im[1];
      const indexRe = /<index\b([^>]*?)>([\s\S]*?)<\/index>/g;
      let iim;
      while ((iim = indexRe.exec(block)) !== null) {
        const idxName = (iim[1].match(ATTR_RE('name')) || [])[1];
        const colNames = [];
        let cm;
        const cre = /<column\b([^>]*?)\/>/g;
        while ((cm = cre.exec(iim[2])) !== null) {
          const cn = (cm[1].match(ATTR_RE('name')) || [])[1];
          if (cn) colNames.push(cn);
        }
        if (idxName) {
          entityCount = entityCount; // counted below
          allIndexes.push({
            tableName,
            idxName,
            dbCols: colNames.map((c) => dbCol(c, colMap)),
          });
          indexCount++;
        }
      }
    }
    if (allIndexes.some((x) => x.tableName === tableName)) entityCount++;
    return match;
  });
  return { entityCount, indexCount, allIndexes };
}

main();
