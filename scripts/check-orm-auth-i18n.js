#!/usr/bin/env node
/**
 * Check i18n coverage across:
 *   1) ORM model XML (entity/column/relation)
 *   2) action-auth XML (resource)
 *   3) dict YAML → i18n yaml (dict.label / dict.option.label)
 *
 * Usage:
 *   node scripts/check-orm-auth-i18n.js                # check all
 *   node scripts/check-orm-auth-i18n.js module-purchase # one module
 */

const fs = require('fs');
const path = require('path');

const ROOT = (() => {
  // __dirname may be '.' in node -e mode; try process.cwd() first
  const cwd = process.cwd();
  const entries = fs.readdirSync(cwd).filter(e => e.startsWith('module-'));
  if (entries.length > 0) return cwd;
  // fallback
  const parent = path.resolve(__dirname, '..');
  if (fs.existsSync(parent)) return parent;
  return cwd;
})();

// ---------- ORM / action-auth checkers ----------

function extractEntities(xml) {
  const entities = [];
  const entityRe = /<entity\s+([\s\S]*?)<\/entity>/g;
  let match;
  while ((match = entityRe.exec(xml)) !== null) {
    const body = match[1];
    const nameMatch = body.match(/name=["']([^"']+)["']/);
    const displayNameMatch = body.match(/\bdisplayName=["']([^"']+)["']/);
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
    const displayNameMatch = attrs.match(/\bdisplayName=["']([^"']+)["']/);
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
      type, name: nameMatch ? nameMatch[1] : 'unnamed',
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

function checkOrmFile(filePath) {
  const xml = fs.readFileSync(filePath, 'utf-8');
  const entities = extractEntities(xml);
  const errors = [];
  let okCount = 0;

  for (const entity of entities) {
    const shortName = entity.name.split('.').pop();

    if (!entity.i18nEn) {
      errors.push({ element: 'entity', location: shortName, chinese: entity.chinese, missing: 'i18n-en:displayName' });
    } else { okCount++; }

    for (const col of extractColumns(entity.body)) {
      if (!col.i18nEn) {
        errors.push({ element: 'column', location: `${shortName}.${col.name}`, chinese: col.chinese, missing: 'i18n-en:displayName' });
      } else { okCount++; }
    }

    for (const rel of extractRelations(entity.body)) {
      if (!rel.i18nEn) {
        errors.push({ element: rel.type, location: `${shortName}.${rel.name}`, chinese: rel.chinese, missing: 'i18n-en:displayName' });
      } else { okCount++; }
      if (!rel.refI18nEn) {
        errors.push({ element: rel.type, location: `${shortName}.${rel.name}`, chinese: rel.chinese, missing: 'ref-i18n-en:displayName' });
      } else { okCount++; }
    }
  }
  return { errors, okCount, entityCount: entities.length };
}

function checkActionAuthFile(filePath) {
  const xml = fs.readFileSync(filePath, 'utf-8');
  const resources = extractResources(xml);
  const errors = [];
  let okCount = 0;
  for (const res of resources) {
    if (!res.i18nEn) {
      errors.push({ element: 'resource', location: res.id, chinese: res.chinese, missing: 'i18n-en:displayName' });
    } else { okCount++; }
  }
  return { errors, okCount, resourceCount: resources.length };
}

// ---------- dict i18n checker ----------

/** parse a .dict.yaml, return { label, options: [{label,value}] } */
function parseDictYaml(content) {
  const labelMatch = content.match(/^label:\s*(.+)/m);
  const localeMatch = content.match(/^locale:\s*(.+)/m);

  const options = [];
  const optRe = /^\s+-\s+label:\s*(.*?)\s*$/gm;
  let m;
  while ((m = optRe.exec(content)) !== null) {
    const label = m[1].trim();
    // the value line follows right after label, find it
    const rest = content.slice(m.index + m[0].length);
    const valMatch = rest.match(/^\s+value:\s*(.+)$/m);
    if (valMatch) {
      options.push({ label, value: valMatch[1].trim() });
    }
  }
  return { label: labelMatch ? labelMatch[1].trim() : '', locale: localeMatch ? localeMatch[1].trim() : '', options };
}

function checkDictI18n(moduleDir) {
  const errors = [];
  let okCount = 0;

  // scan for dict dir in meta submodules: module-X/erp-XX-meta/src/main/resources/_vfs/dict
  const findMetaDir = (base) => {
    for (const entry of fs.readdirSync(base)) {
      const full = path.join(base, entry);
      if (entry.endsWith('-meta') && fs.statSync(full).isDirectory()) {
        const candidate = path.join(full, 'src/main/resources/_vfs/dict');
        if (fs.existsSync(candidate)) return full;
      }
    }
    return null;
  };
  const metaDir = findMetaDir(moduleDir);
  if (!metaDir) return { errors, okCount };

  const dictDir = path.join(metaDir, 'src/main/resources/_vfs/dict');
  const i18nEnDir = path.join(metaDir, 'src/main/resources/_vfs/i18n/en');
  // collect all i18n keys from en yaml files
  const enKeys = new Set();
  if (fs.existsSync(i18nEnDir)) {
    for (const f of fs.readdirSync(i18nEnDir)) {
      if (f.endsWith('.i18n.yaml')) {
        const content = fs.readFileSync(path.join(i18nEnDir, f), 'utf-8');
        // collect all leaf keys (dict.label.xxx / dict.option.label.xxx.yyy)
        const lines = content.split('\n');
        const stack = []; // [{indent, key}]
        for (const line of lines) {
          const indent = line.search(/\S/);
          const trimmed = line.trim();
          if (!trimmed || trimmed.startsWith('#')) continue;

          if (trimmed.endsWith(':')) {
            let key = trimmed.slice(0, -1);
            if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith('"') && key.endsWith('"')))
              key = key.slice(1, -1);
            // pop stack to correct level
            while (stack.length > 0 && stack[stack.length - 1].indent >= indent) {
              stack.pop();
            }
            stack.push({ indent, key });
          } else if (trimmed.includes(':') && !trimmed.endsWith(':')) {
            const colonIdx = trimmed.indexOf(':');
            if (colonIdx > 0) {
              let key = trimmed.slice(0, colonIdx).trim();
              if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith('"') && key.endsWith('"')))
                key = key.slice(1, -1);
              // pop stack to correct level
              while (stack.length > 0 && stack[stack.length - 1].indent >= indent) {
                stack.pop();
              }
              const fullKey = stack.length > 0
                ? stack.map(s => s.key).join('.') + '.' + key
                : key;
              enKeys.add(fullKey);
            }
          }
        }
      }
    }
  }

  // walk dict dir
  const walkDictDir = (dir, relPrefix) => {
    for (const f of fs.readdirSync(dir)) {
      const full = path.join(dir, f);
      const st = fs.statSync(full);
      if (st.isDirectory()) {
        walkDictDir(full, relPrefix ? `${relPrefix}/${f}` : f);
      } else if (f.endsWith('.dict.yaml')) {
        const dictName = relPrefix ? `${relPrefix}/${f.replace('.dict.yaml', '')}` : f.replace('.dict.yaml', '');
        const content = fs.readFileSync(full, 'utf-8');
        const parsed = parseDictYaml(content);

        // check dict label
        const dictKey = `dict.label.${dictName}`;
        if (!enKeys.has(dictKey)) {
          errors.push({ element: 'dict', location: dictName, chinese: parsed.label, missing: dictKey });
        } else { okCount++; }

        // check option labels
        for (const opt of parsed.options) {
          const optKey = `dict.option.label.${dictName}.${opt.value}`;
          if (!enKeys.has(optKey)) {
            errors.push({ element: 'dict.option', location: dictName, chinese: `${opt.label} (value=${opt.value})`, missing: optKey });
          } else { okCount++; }
        }
      }
    }
  };

  if (fs.existsSync(dictDir)) walkDictDir(dictDir, '');
  return { errors, okCount };
}

// ---------- main ----------

function getModuleName(filePath) {
  const match = filePath.match(/module-([^/]+)/);
  return match ? match[1] : 'unknown';
}

function getModuleDir(moduleName) {
  return path.join(ROOT, `module-${moduleName}`);
}

function main() {
  const filterModule = process.argv[2];
  const modules = fs.readdirSync(ROOT).filter(e => e.startsWith('module-') && fs.statSync(path.join(ROOT, e)).isDirectory());

  let grandOk = 0, grandMissing = 0;

  for (const entry of modules) {
    const modName = entry.replace('module-', '');
    if (filterModule && modName !== filterModule) continue;
    const modDir = path.join(ROOT, entry);

    console.log(`\n=====================================================================`);
    console.log(`  Module: ${modName}`);
    console.log(`=====================================================================`);

    // --- ORM ---
    const modelDir = path.join(modDir, 'model');
    if (fs.existsSync(modelDir)) {
      for (const f of fs.readdirSync(modelDir).filter(f => f.endsWith('.orm.xml'))) {
        const res = checkOrmFile(path.join(modelDir, f));
        grandOk += res.okCount; grandMissing += res.errors.length;
        if (res.errors.length > 0) {
          console.log(`\n  --- ORM: ${f} (${res.entityCount} entities) ---`);
          const byEntity = {};
          for (const e of res.errors) {
            const key = e.location.split('.')[0];
            if (!byEntity[key]) byEntity[key] = [];
            byEntity[key].push(e);
          }
          for (const [entity, items] of Object.entries(byEntity)) {
            console.log(`    ${entity}:`);
            for (const item of items) {
              const field = item.location.includes('.') ? item.location.split('.').slice(1).join('.') : '(entity)';
              const desc = field ? `"${field}"` : '';
              console.log(`      - [${item.missing}] ${item.element} ${desc} (cn: "${item.chinese}")`);
            }
          }
        } else {
          console.log(`  \n  --- ORM: ${f} (${res.entityCount} entities) --- ALL OK`);
        }
      }
    }

    // --- Action-Auth ---
    const findAuth = (dir) => {
      if (!fs.existsSync(dir)) return [];
      const files = [];
      try {
        for (const f of fs.readdirSync(dir)) {
          const full = path.join(dir, f);
          if (fs.statSync(full).isDirectory()) {
            if (f === 'auth') {
              for (const af of fs.readdirSync(full).filter(a => a.endsWith('.action-auth.xml'))) {
                files.push(path.join(full, af));
              }
            } else if (f !== 'target') {
              files.push(...findAuth(full));
            }
          }
        }
      } catch (_) {}
      return files;
    };
    const authFiles = findAuth(modDir);
    let authOk = 0, authMissing = 0;
    for (const af of authFiles) {
      const res = checkActionAuthFile(af);
      authOk += res.okCount; authMissing += res.errors.length;
    }
    grandOk += authOk; grandMissing += authMissing;
    if (authFiles.length > 0) {
      console.log(`\n  --- Action-Auth (${authFiles.length} files) ---`);
      console.log(authMissing === 0 ? `    ALL OK (${authOk} resources)` : `    ${authOk} OK, ${authMissing} missing`);
    }

    // --- Dict i18n ---
    const dictRes = checkDictI18n(modDir);
    grandOk += dictRes.okCount; grandMissing += dictRes.errors.length;
    if (dictRes.errors.length > 0) {
      console.log(`\n  --- Dict i18n (i18n/en/*.i18n.yaml) ---`);
      const byType = {};
      for (const e of dictRes.errors) {
        const type = e.element;
        if (!byType[type]) byType[type] = [];
        byType[type].push(e);
      }
      for (const [type, items] of Object.entries(byType)) {
        console.log(`    ${type === 'dict' ? 'Dict labels' : 'Dict option labels'}:`);
        for (const item of items) {
          console.log(`      - [${item.missing}] "${item.location}" (cn: "${item.chinese}")`);
        }
      }
    } else if (dictRes.okCount > 0 || fs.existsSync(path.join(modDir, 'src/main/resources/_vfs/dict'))) {
      console.log(`\n  --- Dict i18n --- ALL OK (${dictRes.okCount} keys)`);
    }
  }

  // --- Grand total ---
  console.log(`\n=====================================================================`);
  console.log(`  TOTAL: ${grandOk} OK, ${grandMissing} missing`);
  console.log(`=====================================================================`);
}

main();
