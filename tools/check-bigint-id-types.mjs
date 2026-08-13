#!/usr/bin/env node
/**
 * BIGINT 主键/外键 stdDataType 检查与批量修改工具
 *
 * 规则（docs-for-ai/02-core-guides/orm-model-design.md「主键设计」节）：
 * BIGINT 主键/外键必须显式声明 stdDataType="string"，避免 JavaScript long 精度问题。
 *
 * 子命令：
 *   scan      （默认）盘点所有 module-* / model 下的 *.orm.xml 的主键/外键，报告 stdSqlType/stdDataType，
 *              并交叉校验关系 join，确认主外键查找完整性。
 *   dry-run   生成批量修改后的副本到 _tmp/bigint-id-string-fix/，用 xmllint 校验 XML 合法性，
 *              重扫副本确认零残留、幂等。不修改任何源文件。
 *   apply     将 dry-run 副本回写源文件（需 --yes，且 dry-run 已通过）。谨慎使用。
 *
 * 用法:
 *   node tools/check-bigint-id-types.mjs [scan|dry-run|apply] [项目根目录] [--yes]
 */
import { readFileSync, writeFileSync, readdirSync, statSync, mkdirSync, existsSync, rmSync } from 'fs';
import path from 'path';
import { spawnSync } from 'child_process';

const OUT_DIR = '_tmp/bigint-id-string-fix';

function extractAttr(s, name) {
  if (!s) return null;
  const e = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const m = s.match(new RegExp(`(?:^|\\s)${e}\\s*=\\s*"([^"]*)"`, 'i'));
  return m ? m[1] : null;
}

function parseFile(text) {
  // 注释按等长空格打码：避免注释中的 <entity>/<column> 文本干扰正则，且保持绝对偏移不变
  text = text.replace(/<!--[\s\S]*?-->/g, m => ' '.repeat(m.length));
  const domains = {};
  let m;
  const domainRe = /<domain\s+([^>]*?)\/>/g;
  while ((m = domainRe.exec(text)) !== null) {
    const name = extractAttr(m[1], 'name');
    if (name)
      domains[name] = {
        stdSqlType: extractAttr(m[1], 'stdSqlType'),
        stdDataType: extractAttr(m[1], 'stdDataType'),
      };
  }

  const entities = [];
  const entityRe = /<entity\s+([^>]*?)(?:\/>|>([\s\S]*?)<\/entity>)/g;
  while ((m = entityRe.exec(text)) !== null) {
    const body = m[2];
    if (body === undefined) continue; // self-closing entity
    const bodyStart = m.index + m[0].indexOf('>') + 1;
    const entityName = extractAttr(m[1], 'name') || extractAttr(m[1], 'className');
    const notGenCode = extractAttr(m[1], 'notGenCode') === 'true';

    const cols = [];
    const colsStart = body.indexOf('<columns>');
    const colsEnd = body.indexOf('</columns>');
    const colsBlock = colsStart >= 0 && colsEnd > colsStart
      ? body.substring(colsStart + '<columns>'.length, colsEnd) : '';
    const colRe = /<column\s+([^>]*?)(\/>|><\/column>)/g;
    let cm;
    while ((cm = colRe.exec(colsBlock)) !== null) {
      const attrs = cm[1];
      cols.push({
        name: extractAttr(attrs, 'name'),
        code: extractAttr(attrs, 'code'),
        stdSqlType: extractAttr(attrs, 'stdSqlType'),
        stdDataType: extractAttr(attrs, 'stdDataType'),
        domain: extractAttr(attrs, 'domain'),
        primary: extractAttr(attrs, 'primary') === 'true',
        absStart: bodyStart + colsStart + '<columns>'.length + cm.index,
        absEnd: bodyStart + colsStart + '<columns>'.length + cm.index + cm[0].length,
        rawTag: cm[0],
      });
    }

    const relations = [];
    const relRe = /<(to-one|to-many)\s+([^>]*?)(?:\/>|>([\s\S]*?)<\/\1>)/g;
    let rm;
    while ((rm = relRe.exec(body)) !== null) {
      const joins = [];
      const onRe = /<on\s+([^>]*?)\/>/g;
      let om;
      while ((om = onRe.exec(rm[3] || '')) !== null) {
        joins.push({ leftProp: extractAttr(om[1], 'leftProp'), rightProp: extractAttr(om[1], 'rightProp') });
      }
      relations.push({
        type: rm[1],
        name: extractAttr(rm[2], 'name'),
        refEntityName: extractAttr(rm[2], 'refEntityName'),
        joins,
      });
    }

    entities.push({ name: entityName, notGenCode, cols, relations });
  }
  return { domains, entities };
}

function collectOrmFiles(rootDir) {
  const files = [];
  for (const entry of readdirSync(rootDir)) {
    if (!entry.startsWith('module-')) continue;
    const modelDir = path.join(rootDir, entry, 'model');
    try {
      if (!statSync(modelDir).isDirectory()) continue;
      for (const f of readdirSync(modelDir)) {
        if (f.endsWith('.orm.xml'))
          files.push({ relPath: path.join(entry, 'model', f), fullPath: path.join(modelDir, f) });
      }
    } catch (e) { /* not a dir */ }
  }
  return files.sort((a, b) => a.relPath.localeCompare(b.relPath));
}

function defaultStdDataType(stdSqlType) {
  const map = {
    BIGINT: 'long', INTEGER: 'int', SMALLINT: 'short', TINYINT: 'byte',
    DECIMAL: 'decimal', NUMERIC: 'decimal', REAL: 'float', FLOAT: 'double', DOUBLE: 'double',
    CHAR: 'string', VARCHAR: 'string', JSON: 'string', CLOB: 'string',
    DATE: 'date', TIME: 'time', DATETIME: 'datetime', TIMESTAMP: 'timestamp',
    BINARY: 'bytes', VARBINARY: 'bytes', BLOB: 'bytes', BOOLEAN: 'boolean',
  };
  return map[stdSqlType] || null;
}

function effectiveStdDataType(col, domains) {
  if (col.stdDataType) return col.stdDataType;
  if (col.domain && domains[col.domain] && domains[col.domain].stdDataType)
    return domains[col.domain].stdDataType;
  return defaultStdDataType(col.stdSqlType);
}

function classify(col, entity) {
  const roles = [];
  if (col.primary) roles.push('PK');
  const nameIsFk = /Id$/.test(col.name || '') && col.name !== 'id';
  const codeIsFk = /_ID$/.test(col.code || '');
  // to-one 的 leftProp 是子表外键；to-many 的 leftProp 是父表主键（已是 PK），不重复计 FK
  const joinRef = entity.relations.some(r => r.type === 'to-one' && r.joins.some(j => j.leftProp === col.name));
  if (nameIsFk || codeIsFk || joinRef) roles.push('FK');
  return roles;
}

function analyzeFile(filePath, relPath, globalIndex, report = true) {
  const text = readFileSync(filePath, 'utf-8');
  const { domains, entities } = parseFile(text);
  const warnings = [];
  const blockers = [];
  const rows = [];
  const unclassified = [];

for (const entity of entities) {
      const colByName = new Map(entity.cols.map(c => [c.name, c]));
      for (const rel of entity.relations) {
        for (const j of rel.joins) {
          const left = colByName.get(j.leftProp);
          if (!left) {
            warnings.push(`${entity.name}: join ${rel.type}.${rel.name} leftProp="${j.leftProp}" 未找到对应列`);
            continue;
          }
          if (rel.type === 'to-one' && classify(left, entity).length === 0)
            warnings.push(`${entity.name}: join leftProp="${j.leftProp}" 未被识别为 FK`);
          if (rel.type === 'to-one') {
            const refEntity = globalIndex.get(rel.refEntityName);
            if (refEntity) {
              const refCol = refEntity.cols.find(c => c.name === j.rightProp);
              if (!refCol) {
                warnings.push(`${entity.name}: join rightProp="${j.rightProp}" 在 ${rel.refEntityName} 中未找到列`);
              } else if (!refCol.primary) {
                warnings.push(`${entity.name}: join rightProp="${j.rightProp}" 在 ${rel.refEntityName} 中不是主键`);
              }
            }
          }
        }
      }

    for (const col of entity.cols) {
      const roles = classify(col, entity);
      if (roles.length === 0) {
        if (col.stdSqlType === 'BIGINT')
          unclassified.push(`${entity.name}.${col.name} (${col.code || ''}) ${col.stdDataType || '(无显式类型)'}`);
        continue;
      }

      const effType = effectiveStdDataType(col, domains);
      const isBigint = col.stdSqlType === 'BIGINT';
      const needsFix = isBigint && effType !== 'string';

      if (needsFix && col.domain && domains[col.domain] && domains[col.domain].stdDataType &&
          domains[col.domain].stdDataType !== 'string') {
        blockers.push(
          `${entity.name}.${col.name}: domain="${col.domain}" 定义 stdDataType="${domains[col.domain].stdDataType}"，` +
          `改列会抛 ERR_ORM_MODEL_COL_DATA_TYPE_NOT_MATCH_DOMAIN_DEFINITION，需改 domain 定义`);
      }

      rows.push({
        entity: entity.name, col: col.name, code: col.code || '',
        role: roles.join('+'), stdSqlType: col.stdSqlType || '(none)',
        stdDataType: col.stdDataType || (effType ? `(default:${effType})` : '(none)'),
        domain: col.domain || '', effType, isBigint, needsFix, notGenCode: entity.notGenCode,
        absStart: col.absStart, absEnd: col.absEnd, rawTag: col.rawTag, filePath, relPath,
      });
    }
  }

  if (report && rows.length > 0) {
    console.log(`\n=== ${relPath} ===`);
    for (const r of rows) {
      const mark = r.needsFix ? '!! NEEDS FIX' : (r.isBigint ? 'ok' : 'n/a');
      console.log(
        `  [${r.role}] ${r.entity}.${r.col} (${r.code}) ${r.stdSqlType}/${r.stdDataType}` +
        (r.domain ? ` domain=${r.domain}` : '') + (r.notGenCode ? ' (notGenCode)' : '') + `  ${mark}`);
    }
  }
  if (report && unclassified.length > 0) {
    console.log(`\n=== ${relPath} — 未分类 BIGINT 列（非 PK/FK，不在改造范围，供排查遗漏） ===`);
    for (const u of unclassified) console.log(`  [BIGINT] ${u}`);
  }
  for (const w of warnings) console.log(`  [WARN] ${w}`);
  for (const b of blockers) console.log(`  [BLOCKER] ${b}`);

  return { rows, warnings, blockers, unclassified, text, entities, domains };
}

function fixTag(attrs) {
  let a = attrs;
  if (!/stdSqlType\s*=\s*"BIGINT"/.test(a)) return a; // 防御：仅处理 BIGINT 列
  if (/stdDataType\s*=\s*"[^"]*"/.test(a)) {
    a = a.replace(/stdDataType\s*=\s*"[^"]*"/, 'stdDataType="string"');
  } else {
    a = a.replace(/stdSqlType\s*=\s*"BIGINT"/, 'stdSqlType="BIGINT" stdDataType="string"');
  }
  return a;
}

function buildFixedText(text, rows) {
  const targets = rows.filter(r => r.needsFix).sort((a, b) => b.absStart - a.absStart);
  let out = text;
  for (const t of targets) {
    const oldTag = t.rawTag;
    const newTag = oldTag.replace(t.rawTag.match(/<column\s+([^>]*?)(\/>|><\/column>)/)[1], fixTag);
    out = out.substring(0, t.absStart) + newTag + out.substring(t.absEnd);
  }
  return out;
}

function validateXml(filePath) {
  let res = spawnSync('xmllint', ['--noout', filePath], { encoding: 'utf-8' });
  if (res.status === 0) return { ok: true, tool: 'xmllint' };
  res = spawnSync('python3', ['-c', 'import sys,xml.etree.ElementTree as E; E.parse(sys.argv[1])', filePath],
    { encoding: 'utf-8' });
  if (res.status === 0) return { ok: true, tool: 'python3' };
  return { ok: false, tool: res.error ? 'none' : 'xmllint+python3', err: res.stderr || res.stdout };
}

function printSummary(results, mode) {
  let pk = 0, fk = 0, pkFk = 0, bigint = 0, needsFix = 0, alreadyString = 0, other = 0, blockers = 0, warns = 0, unclassified = 0;
  let bigPk = 0, bigFk = 0;
  for (const r of results) {
    for (const row of r.rows) {
      const isPk = row.role.includes('PK');
      const isFk = row.role.includes('FK');
      if (isPk && isFk) pkFk++;
      else if (isPk) pk++;
      else if (isFk) fk++;
      if (row.isBigint) {
        bigint++;
        if (row.needsFix) needsFix++;
        else alreadyString++;
        if (isPk) bigPk++;
        if (isFk) bigFk++;
      } else {
        other++;
      }
    }
    blockers += r.blockers.length;
    warns += r.warnings.length;
    unclassified += r.unclassified.length;
  }
  console.log(`\n========== 汇总 (${mode}) ==========`);
  console.log(`主键 id 列: ${pk}  外键 xx_id 列: ${fk}  PK+FK 双角色列: ${pkFk}`);
  console.log(`主外键列合计: ${pk + fk + pkFk}  (= ${pk} + ${fk} + ${pkFk})`);
  console.log(`BIGINT 主键: ${bigPk}  BIGINT 外键: ${bigFk}  BIGINT 主/外键合计: ${bigint}`);
  console.log(`实际修改 stdDataType 的列: ${needsFix}  (= BIGINT 合计, 无重复列)`);
  console.log(`非 BIGINT 主/外键(不改): ${other}  未分类 BIGINT 列(不改): ${unclassified}`);
  console.log(`校验告警: ${warns}  BLOCKER(domain 冲突): ${blockers}`);
  if (blockers > 0) {
    console.log('存在 BLOCKER，禁止 apply；先人工处理 domain 定义。');
    process.exitCode = 2;
  } else if (warns > 0 && mode === 'scan') {
    console.log('存在告警，请核对上方 WARN 行后确认查找完整性。');
  }
}

function main() {
  const positional = process.argv.slice(2).filter(a => !a.startsWith('--'));
  const mode = positional[0] && ['scan', 'dry-run', 'apply'].includes(positional[0]) ? positional[0] : 'scan';
  const rootDir = (positional[0] === mode ? positional[1] : positional[0]) || process.cwd();
  const yes = process.argv.includes('--yes');

  const files = collectOrmFiles(rootDir);
  if (files.length === 0) {
    console.error(`未找到 module-* / model 下的 orm.xml（root=${rootDir}）`);
    process.exit(1);
  }

  // 全局实体索引（跨文件校验 join rightProp 用）
  const globalIndex = new Map();
  for (const f of files) {
    const { entities } = parseFile(readFileSync(f.fullPath, 'utf-8'));
    for (const e of entities) {
      globalIndex.set(e.name, e);
      const short = e.name.split('.').pop();
      if (!globalIndex.has(short)) globalIndex.set(short, e);
    }
  }

  const results = files.map(f => analyzeFile(f.fullPath, f.relPath, globalIndex));
  printSummary(results, mode);

  if (mode === 'dry-run' || mode === 'apply') {
    const outRoot = path.join(rootDir, OUT_DIR);
    if (mode === 'apply') {
      if (!yes) {
        console.error(`apply 需要 --yes 确认。先审核 ${OUT_DIR}/ 下的副本与 git diff。`);
        process.exit(1);
      }
      if (!existsSync(outRoot)) {
        console.error(`未找到 ${OUT_DIR}/，请先运行 dry-run。`);
        process.exit(1);
      }
    } else {
      if (existsSync(outRoot)) rmSync(outRoot, { recursive: true, force: true });
    }

    let changedFiles = 0, changedCols = 0, validated = 0;
    const changedDetails = [];
    for (let i = 0; i < results.length; i++) {
      const f = files[i];
      const fixed = buildFixedText(results[i].text, results[i].rows);
      if (fixed === results[i].text) continue;
      const outPath = path.join(outRoot, f.relPath);
      mkdirSync(path.dirname(outPath), { recursive: true });
      writeFileSync(outPath, fixed);
      changedFiles++;
      changedCols += results[i].rows.filter(r => r.needsFix).length;
      changedDetails.push({ relPath: f.relPath, cols: results[i].rows.filter(r => r.needsFix) });

      if (mode === 'dry-run') {
        const v = validateXml(outPath);
        if (!v.ok) {
          console.error(`[FAIL] XML 校验失败: ${f.relPath} (${v.tool}) ${v.err || ''}`);
          process.exitCode = 1;
        } else {
          validated++;
        }
      }
    }

    if (mode === 'dry-run') {
      // 重扫副本：确认零残留 + 幂等
      let residual = 0, idempotent = true;
      for (const d of changedDetails) {
        const copyPath = path.join(outRoot, d.relPath);
        const re = analyzeFile(copyPath, `${OUT_DIR}/${d.relPath}`, globalIndex, false);
        residual += re.rows.filter(r => r.needsFix).length;
        if (buildFixedText(re.text, re.rows) !== re.text) idempotent = false;
      }
      console.log(`\n========== DRY-RUN 结果 ==========`);
      console.log(`修改文件: ${changedFiles}  修改列: ${changedCols}  XML 校验通过: ${validated}/${changedFiles}`);
      console.log(`副本重扫残留需改: ${residual}  幂等: ${idempotent ? 'yes' : 'NO'}`);
      console.log(`未修改任何源文件。副本位于 ${OUT_DIR}/，审核后执行:`);
      console.log(`  node tools/check-bigint-id-types.mjs apply --yes`);
      if (residual > 0 || !idempotent || changedCols === 0) process.exitCode = 1;
    } else {
      console.log(`\n========== APPLY 结果 ==========`);
      console.log(`已回写文件: ${changedFiles}  修改列: ${changedCols}`);
      console.log(`建议: git diff 审核 + ORM 增量重生成 (mvn clean install -DskipTests) 后跑全量验证。`);
    }
  }
}

main();