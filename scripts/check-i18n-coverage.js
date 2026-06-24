#!/usr/bin/env node
/**
 * Check i18n coverage across ORM XML and action-auth XML files.
 *
 * Usage:
 *   node scripts/check-i18n-coverage.js                # check all modules
 *   node scripts/check-i18n-coverage.js module-purchase # check one module
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const ORM_GLOB = 'module-*/model/app-erp-*.orm.xml';
const AUTH_GLOB = 'module-*/erp-*-web/src/main/resources/_vfs/*/auth/*.action-auth.xml';

// ---------- helpers ----------

function listFiles(pattern) {
  const { globSync } = (() => {
    // minimal glob for common patterns like module-*/model/*.orm.xml
    const prefix = pattern.slice(0, pattern.indexOf('*'));
    const suffix = pattern.slice(pattern.lastIndexOf('*') + 1);
    const midDir = pattern.slice(pattern.indexOf('*'), pattern.lastIndexOf('*') + 1);

    if (!fs.existsSync(prefix)) return [];

    const results = [];
    for (const entry of fs.readdirSync(prefix)) {
      const full = path.join(prefix, entry);
      if (!fs.statSync(full).isDirectory()) continue;
      const sub = midDir.replace('module-*', entry);
      const fullGlob = path.join(prefix, entry, sub, suffix);
      // now do a manual walk for the rest
      walk(fullGlob, results);
    }
    return results;
  })();
  return results;
}

function walk(pattern, out) {
  // pattern e.g. /module-purchase/model/app-erp-purchase.orm.xml
  // or         /module-purchase/erp-pur-web/src/main/resources/_vfs/*/auth/*.action-auth.xml
  const parts = pattern.split('*');
  if (parts.length === 1) {
    if (fs.existsSync(pattern)) out.push(pattern);
    return;
  }
  const prefix = parts[0];
  if (!fs.existsSync(prefix)) return;
  for (const entry of fs.readdirSync(prefix)) {
    const full = path.join(prefix, entry);
    if (parts.length === 2) {
      // single wildcard
      const suffix = parts[1];
      const candidate = full + suffix;
      if (fs.existsSync(candidate)) out.push(candidate);
    } else if (parts.length === 3) {
      // double wildcard
      const mid = entry;
      const suffix = parts[2];
      const candidate = path.join(prefix, mid, suffix);
      if (fs.existsSync(candidate)) out.push(candidate);
      if (fs.statSync(full).isDirectory()) {
        walk(path.join(full, '*', suffix), out);
      }
    }
  }
}

function getModuleName(filePath) {
  const match = filePath.match(/module-([^/]+)/);
  return match ? match[1] : 'unknown';
}

function getEntityName(filePath) {
  const match = filePath.match(/(\w+)\.orm\.xml$/);
  return match ? match[1] : path.basename(filePath);
}

// ---------- XML parsing (regex-based, no deps) ----------

function extractEntities(xml) {
  const entities = [];
  const entityRe = /<entity\s+([\s\S]*?)<\/entity>/g;
  let match;
  while ((match = entityRe.exec(xml)) !== null) {
    const body = match[1];
    const nameMatch = body.match(/name=["']([^"']+)["']/);
    const displayNameMatch = body.match(/displayName=["']([^"']+)["']/);
    const i18nMatch = body.match(/i18n-en:displayName=["']([^"']+)["']/);
    entities.push({
      name: nameMatch ? nameMatch[1] : 'unknown',
      chinese: displayNameMatch ? displayNameMatch[1] : '',
      i18nEn: i18nMatch ? i18nMatch[1] : null,
      body,
    });
  }
  return entities;
}

function extractColumns(entityBody) {
  const columns = [];
  const colRe = /<column\s+([\s\S]*?)\/>/g;
  let match;
  while ((match = colRe.exec(entityBody)) !== null) {
    const attrs = match[1];
    const nameMatch = attrs.match(/name=["']([^"']+)["']/);
    const displayNameMatch = attrs.match(/displayName=["']([^"']+)["']/);
    const i18nMatch = attrs.match(/i18n-en:displayName=["']([^"']+)["']/);
    columns.push({
      name: nameMatch ? nameMatch[1] : 'unnamed',
      chinese: displayNameMatch ? displayNameMatch[1] : '',
      i18nEn: i18nMatch ? i18nMatch[1] : null,
    });
  }
  return columns;
}

function extractRelations(entityBody) {
  const relations = [];
  const relRe = /<(to-one|to-many)\s+([\s\S]*?)\/>/g;
  let match;
  while ((match = relRe.exec(entityBody)) !== null) {
    const type = match[1];
    const attrs = match[2];
    const nameMatch = attrs.match(/name=["']([^"']+)["']/);
    const displayNameMatch = attrs.match(/\bdisplayName=["']([^"']+)["']/);
    const i18nMatch = attrs.match(/i18n-en:displayName=["']([^"']+)["']/);
    const refI18nMatch = attrs.match(/ref-i18n-en:displayName=["']([^"']+)["']/);
    relations.push({
      type,
      name: nameMatch ? nameMatch[1] : 'unnamed',
      chinese: displayNameMatch ? displayNameMatch[1] : '',
      i18nEn: i18nMatch ? i18nMatch[1] : null,
      refI18nEn: refI18nMatch ? refI18nMatch[1] : null,
    });
  }
  return relations;
}

function extractResources(xml) {
  const resources = [];
  const resRe = /<resource\s+([\s\S]*?)\/?>/g;
  let match;
  while ((match = resRe.exec(xml)) !== null) {
    const attrs = match[1];
    const idMatch = attrs.match(/id=["']([^"']+)["']/);
    const displayNameMatch = attrs.match(/\bdisplayName=["']([^"']+)["']/);
    const i18nMatch = attrs.match(/i18n-en:displayName=["']([^"']+)["']/);
    if (displayNameMatch) {
      resources.push({
        id: idMatch ? idMatch[1] : 'unknown',
        chinese: displayNameMatch[1],
        i18nEn: i18nMatch ? i18nMatch[1] : null,
      });
    }
  }
  return resources;
}

// ---------- checkers ----------

function checkOrmFile(filePath) {
  const xml = fs.readFileSync(filePath, 'utf-8');
  const entities = extractEntities(xml);
  const module = getModuleName(filePath);
  const errors = [];
  let okCount = 0;

  for (const entity of entities) {
    const shortName = entity.name.split('.').pop();

    // entity-level i18n
    if (!entity.i18nEn) {
      errors.push({
        module,
        file: filePath,
        element: `entity`,
        location: shortName,
        chinese: entity.chinese,
        missing: 'i18n-en:displayName',
      });
    } else {
      okCount++;
    }

    // column-level i18n
    const columns = extractColumns(entity.body);
    for (const col of columns) {
      if (!col.i18nEn) {
        errors.push({
          module,
          file: filePath,
          element: `column`,
          location: `${shortName}.${col.name}`,
          chinese: col.chinese,
          missing: 'i18n-en:displayName',
        });
      } else {
        okCount++;
      }
    }

    // relation-level i18n
    const relations = extractRelations(entity.body);
    for (const rel of relations) {
      if (!rel.i18nEn) {
        errors.push({
          module,
          file: filePath,
          element: `${rel.type}`,
          location: `${shortName}.${rel.name}`,
          chinese: rel.chinese,
          missing: 'i18n-en:displayName',
        });
      } else {
        okCount++;
      }
      if (!rel.refI18nEn) {
        errors.push({
          module,
          file: filePath,
          element: `${rel.type}`,
          location: `${shortName}.${rel.name}`,
          chinese: rel.chinese,
          missing: 'ref-i18n-en:displayName',
        });
      } else {
        okCount++;
      }
    }
  }

  return { errors, okCount, entityCount: entities.length };
}

function checkActionAuthFile(filePath) {
  const xml = fs.readFileSync(filePath, 'utf-8');
  const resources = extractResources(xml);
  const module = getModuleName(filePath);
  const errors = [];
  let okCount = 0;

  for (const res of resources) {
    if (!res.i18nEn) {
      errors.push({
        module,
        file: filePath,
        element: `resource`,
        location: res.id,
        chinese: res.chinese,
        missing: 'i18n-en:displayName',
      });
    } else {
      okCount++;
    }
  }

  return { errors, okCount, resourceCount: resources.length };
}

// ---------- main ----------

function main() {
  const filterModule = process.argv[2]; // optional module filter
  const ormFiles = [];
  const authFiles = [];

  // walk module directories
  const entries = fs.readdirSync(ROOT);
  for (const entry of entries) {
    if (!entry.startsWith('module-')) continue;
    if (filterModule && entry !== `module-${filterModule}`) continue;
    const modDir = path.join(ROOT, entry);

    // ORM model files
    const modelDir = path.join(modDir, 'model');
    if (fs.existsSync(modelDir)) {
      for (const f of fs.readdirSync(modelDir)) {
        if (f.endsWith('.orm.xml')) {
          ormFiles.push(path.join(modelDir, f));
        }
      }
    }

    // action-auth files: find all auth/ dirs under src/main/resources/_vfs
    const walkForAuth = (dir) => {
      if (!fs.existsSync(dir)) return;
      try {
        for (const f of fs.readdirSync(dir)) {
          const full = path.join(dir, f);
          const st = fs.statSync(full);
          if (st.isDirectory()) {
            if (f === 'auth') {
              for (const af of fs.readdirSync(full)) {
                if (af.endsWith('.action-auth.xml')) {
                  authFiles.push(path.join(full, af));
                }
              }
            } else {
              walkForAuth(full);
            }
          }
        }
      } catch (_) { /* permission errors */ }
    };
    walkForAuth(path.join(modDir));
  }

  // Check ORM
  let ormTotalMissing = 0;
  let ormTotalOk = 0;
  let ormTotalItems = 0;
  const ormResults = [];

  for (const f of ormFiles) {
    const result = checkOrmFile(f);
    ormTotalMissing += result.errors.length;
    ormTotalOk += result.okCount;
    ormTotalItems += result.okCount + result.errors.length;
    ormResults.push({ filePath: f, ...result });
  }

  // Check Action-Auth
  let authTotalMissing = 0;
  let authTotalOk = 0;
  let authTotalItems = 0;
  const authResults = [];

  for (const f of authFiles) {
    const result = checkActionAuthFile(f);
    authTotalMissing += result.errors.length;
    authTotalOk += result.okCount;
    authTotalItems += result.okCount + result.errors.length;
    authResults.push({ filePath: f, ...result });
  }

  // ---------- REPORT ----------

  const pad = (s, n) => String(s).padEnd(n);

  console.log('================================================================================');
  console.log('  i18n Coverage Report');
  console.log('================================================================================\n');

  // --- ORM Report ---
  console.log('--- ORM Model Files (entity / column / relation) ---\n');

  if (ormTotalMissing > 0) {
    for (const r of ormResults) {
      if (r.errors.length === 0) continue;
      const file = path.basename(r.filePath);
      const mod = r.errors[0].module;
      console.log(`  [${mod}] ${file}`);
      // group by entity
      const byEntity = {};
      for (const e of r.errors) {
        const key = e.location.split('.')[0];
        if (!byEntity[key]) byEntity[key] = [];
        byEntity[key].push(e);
      }
      for (const [entity, items] of Object.entries(byEntity)) {
        console.log(`    ${entity}:`);
        for (const item of items) {
          const field = item.location.includes('.') ? item.location.split('.').slice(1).join('.') : '(entity)';
          console.log(`      - [${item.missing}] ${item.element} ${field ? `"${field}"` : ''} (cn: "${item.chinese}")`);
        }
      }
      console.log('');
    }
  }

  console.log(`  ORM Summary: ${ormTotalOk} OK, ${ormTotalMissing} missing / ${ormTotalItems} total\n`);

  // --- Action-Auth Report ---
  console.log('--- Action-Auth Files ---\n');

  if (authTotalMissing > 0) {
    for (const r of authResults) {
      if (r.errors.length === 0) continue;
      const file = path.basename(r.filePath);
      const mod = r.errors[0].module;
      console.log(`  [${mod}] ${file}`);
      for (const item of r.errors) {
        console.log(`      - [${item.missing}] resource "${item.location}" (cn: "${item.chinese}")`);
      }
      console.log('');
    }
  }

  console.log(`  Action-Auth Summary: ${authTotalOk} OK, ${authTotalMissing} missing / ${authTotalItems} total\n`);

  // --- Overall ---
  const grandTotalOk = ormTotalOk + authTotalOk;
  const grandTotalMissing = ormTotalMissing + authTotalMissing;
  const grandTotalItems = ormTotalItems + authTotalItems;

  console.log('================================================================================');
  console.log(`  TOTAL: ${grandTotalOk} OK, ${grandTotalMissing} missing / ${grandTotalItems} total`);
  console.log('================================================================================');
}

main();
