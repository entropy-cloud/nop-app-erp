#!/usr/bin/env node
// check-view-cols.mjs
//
// Static lint: for every non-generated *.view.xml grid <col id="X">, check that X
// is a valid property of the view's bound entity (per the generated _<Entity>.xmeta).
// Reports col ids that are NOT declared props (the runtime `nop.err.xui.grid.col-not-prop`
// class) WITHOUT fail-fast — gives the complete list in one pass.
//
// Validity rule for a col id:
//   - "id", "delVersion", "createdBy", ...: system cols present in every entity → valid
//   - no dot  → must be a declared <prop name="X"> in the entity xmeta
//   - has dot → prefix (before first dot) must be a to-one relation prop → path prop valid
//
// Usage:
//   node check-view-cols.mjs                  # scan all web modules
//   node check-view-cols.mjs module-aps        # scope to one module dir
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

const REPO = process.cwd();
const SCOPE = process.argv.slice(2).find((a) => !a.startsWith('-')) || '';

// system/standard cols that exist on every entity; not all are in xmeta explicitly
const SYSTEM_COLS = new Set([
  'id', 'delVersion', 'createdBy', 'createdTime', 'updatedBy', 'updatedTime',
  'createTime', 'updateTime', 'tenantId', 'remark', 'version',
]);

function walk(dir, pred, out = []) {
  let ents;
  try { ents = readdirSync(dir); } catch { return out; }
  for (const e of ents) {
    const p = join(dir, e);
    let s;
    try { s = statSync(p); } catch { continue; }
    if (s.isDirectory()) walk(p, pred, out);
    else if (pred(p)) out.push(p);
  }
  return out;
}

// Build global entityName -> { props:Set, toOne:Set } from all generated xmeta.
function buildMetaMap() {
  const map = new Map();
  const files = walk(REPO, (p) =>
    p.includes('/erp-') && p.includes('-meta/') && /\/_vfs\/.+\/model\/[^/]+\/_[^/]+\.xmeta$/.test(p),
  );
  for (const f of files) {
    const m = f.match(/\/model\/([^/]+)\/_[^/]+\.xmeta$/);
    const entity = m ? m[1] : '';
    if (!entity) continue;
    let txt;
    try { txt = readFileSync(f, 'utf8'); } catch { continue; }
    const props = new Set();
    const toOne = new Set();
    for (const pm of txt.matchAll(/<prop\s+name="([^."]+)"[^>]*>/g)) props.add(pm[1]);
    for (const tm of txt.matchAll(/<prop\s+name="([^."]+)"[^>]*ext:kind="to-one"/g)) toOne.add(tm[1]);
    // also catch to-one declared with tagSet="pub" + ext:kind
    map.set(entity, { props, toOne });
  }
  return map;
}

function entityOfView(viewPath) {
  const m = viewPath.match(/\/pages\/([^/]+)\/[^/]+\.view\.xml$/);
  return m ? m[1] : '';
}

function colsOfView(viewPath) {
  let txt;
  try { txt = readFileSync(viewPath, 'utf8'); } catch { return []; }
  const out = [];
  const lines = txt.split('\n');
  lines.forEach((line, i) => {
    if (line.includes('<col ')) {
      const cm = line.match(/<col [^>]*id="([^"]+)"/);
      if (cm) out.push({ id: cm[1], line: i + 1 });
    }
  });
  return out;
}

function main() {
  const meta = buildMetaMap();
  const views = walk(REPO, (p) =>
    p.includes('/erp-*-web/') === false &&
    /\/_vfs\/.+\/pages\/[^/]+\/[^/]+\.view\.xml$/.test(p) &&
    !p.includes('/_gen/') &&
    (!SCOPE || p.includes('/' + SCOPE + '/')),
  );
  // Only real source web modules (exclude _dump/, _gen/, tests, etc.)
  const allViews = walk(REPO, (p) =>
    /\/module-[^/]+\/erp-[^/]+-web\/.*\/_vfs\/.*\/pages\/[^/]+\/[^/]+\.view\.xml$/.test(p) &&
    !p.includes('/_gen/') &&
    (!SCOPE || p.includes('/' + SCOPE)),
  );

  const findings = [];
  let checkedViews = 0, checkedCols = 0;
  for (const v of allViews) {
    const entity = entityOfView(v);
    const m = meta.get(entity);
    if (!m) continue; // can't validate without meta
    checkedViews++;
    for (const c of colsOfView(v)) {
      checkedCols++;
      const id = c.id;
      if (SYSTEM_COLS.has(id)) continue;
      if (id.includes('.')) {
        const prefix = id.split('.')[0];
        if (m.toOne.has(prefix)) continue;
        findings.push({ view: v, entity, col: id, line: c.line, reason: `path prefix '${prefix}' 不是 to-one 关系` });
        continue;
      }
      if (m.props.has(id)) continue;
      findings.push({ view: v, entity, col: id, line: c.line, reason: `不是实体 ${entity} 的已定义属性` });
    }
  }

  process.stdout.write(`\nChecked ${checkedViews} views, ${checkedCols} cols. ${findings.length} invalid col(s):\n\n`);
  if (!findings.length) return;
  const rel = (p) => p.replace(REPO + '/', '');
  for (const f of findings) {
    process.stdout.write(`${rel(f.view)}:${f.line}  col="${f.col}"  (${f.reason})\n`);
  }
  process.stdout.write('\n');
}

main();
