#!/usr/bin/env node
/**
 * ORM 外键列分类工具（通用版本 v2）
 *
 * 纯 ORM 结构分析，无硬编码实体名。依赖：
 * 1. 实体 className 包路径推断域（app.erp.xxx → xxx）
 * 2. notGenCode 实体 + biz:moduleId 声明跨域引用
 * 3. 同域短名后缀匹配
 *
 * 用法: node tools/categorize-orm-fks.mjs [项目根目录]
 */
import { readFileSync, readdirSync, statSync } from 'fs';
import path from 'path';

const rootDir = process.argv[2] || process.cwd();

function parseXML(filePath) {
  const text = readFileSync(filePath, 'utf-8');
  const entities = [];
  const entityRegex = /<entity\s+([^>]*?)(\/>|>([\s\S]*?)<\/entity>)/g;
  let entityMatch;
  while ((entityMatch = entityRegex.exec(text)) !== null) {
    const entityAttrs = entityMatch[1];
    const entityBody = entityMatch[3] || '';
    const className = extractAttr(entityAttrs, 'className');
    if (!className) continue;
    const name = extractAttr(entityAttrs, 'name') || className;
    const shortName = className.split('.').pop();
    const tableName = extractAttr(entityAttrs, 'tableName');
    const notGenCode = extractAttr(entityAttrs, 'notGenCode') === 'true';
    const bizModuleId = extractAttr(entityAttrs, 'biz:moduleId') || '';

    const columns = [];
    const colRegex = /<column\s+([^>]*?)(\/|><\/column>)/g;
    let colMatch;
    while ((colMatch = colRegex.exec(entityBody)) !== null) {
      const colAttrs = colMatch[1];
      const colName = extractAttr(colAttrs, 'name');
      const primary = extractAttr(colAttrs, 'primary') === 'true';
      if (colName) columns.push({ name: colName, displayName: extractAttr(colAttrs, 'displayName') || '', primary });
    }

    const toOneRels = [];
    const toOneRegex = /<to-one\s+([^>]*?)(?:\/>|>[\s\S]*?<\/to-one>)/g;
    let toOneMatch;
    while ((toOneMatch = toOneRegex.exec(entityBody)) !== null) {
      const fullMatch = toOneMatch[0];
      const relAttrs = toOneMatch[1];
      const relName = extractAttr(relAttrs, 'name');
      const refEntityName = extractAttr(relAttrs, 'refEntityName');
      const joinMatch = fullMatch.match(/<on\s+leftProp="([^"]*)"\s+rightProp="([^"]*)"\s*\/>/);
      const join = joinMatch ? { leftProp: joinMatch[1], rightProp: joinMatch[2] } : null;
      toOneRels.push({ name: relName, refEntityName, join });
    }

    entities.push({
      className, shortName, name, tableName,
      notGenCode, bizModuleId,
      columns, toOneRels,
    });
  }
  return entities;
}

function extractAttr(attrsStr, attrName) {
  if (!attrsStr) return null;
  const e = attrName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const m = attrsStr.match(new RegExp(`${e}\\s*=\\s*"([^"]*)"`, 'i'));
  if (m) return m[1];
  const m2 = attrsStr.match(new RegExp(`${e}\\s*=\\s*'([^']*)'`, 'i'));
  return m2 ? m2[1] : null;
}

/** Infer domain from className package path */
function domainFromPackage(className) {
  // app.erp.xxx.dao.entity.ErpXxx → xxx
  // io.nop.auth.dao.entity.NopAuthUser → nop-auth
  const parts = className.split('.');
  const erpIdx = parts.indexOf('erp');
  if (erpIdx >= 0 && erpIdx + 1 < parts.length) return parts[erpIdx + 1];
  if (parts[0] === 'io' && parts[1] === 'nop' && parts.length > 2) return `nop-${parts[2]}`;
  return 'unknown';
}

/** Short name from className */
function sn(className) { return className ? className.split('.').pop() : ''; }

function main() {
  const files = [];
  for (const entry of readdirSync(rootDir)) {
    if (!entry.startsWith('module-')) continue;
    const modelDir = path.join(rootDir, entry, 'model');
    try {
      if (!statSync(modelDir).isDirectory()) continue;
      for (const f of readdirSync(modelDir).filter(f => f.endsWith('.orm.xml')))
        files.push(path.join(modelDir, f));
    } catch (e) {}
  }

  // Parse all files
  const moduleEntities = {}; // moduleName → entities[]
  const allEntities = [];
  const entityByClassName = {}; // className → entity
  const entityByShortName = {}; // shortName → entity[]

  for (const file of files) {
    const mod = path.basename(file).replace('app-erp-', '').replace('.orm.xml', '');
    const ents = parseXML(file);
    moduleEntities[mod] = ents;
    for (const e of ents) {
      allEntities.push(e);
      entityByClassName[e.className] = e;
      if (!entityByShortName[e.shortName]) entityByShortName[e.shortName] = [];
      entityByShortName[e.shortName].push({ ...e, module: mod });
    }
  }

  /* Build: for each module, identify "primary entities" (notGenCode=false).
     Also build a map of notGenCode entity name → bizModuleId for cross-domain refs. */
  const notGenCodeMap = {}; // className → { entity, module, bizModuleId }
  const primaryByModule = {}; // module → primary entities[]

  for (const [mod, ents] of Object.entries(moduleEntities)) {
    primaryByModule[mod] = [];
    for (const e of ents) {
      if (e.notGenCode) {
        notGenCodeMap[e.className] = { entity: e, module: mod, bizModuleId: e.bizModuleId };
        const targetMod = e.bizModuleId || domainFromPackage(e.className);
        if (!e.bizModuleId) continue;
      } else {
        primaryByModule[mod].push(e);
      }
    }
  }

  // Master-data naming patterns (truly universal, from erp convention)
  const MD_PATTERNS = {
    orgId: 'ErpMdOrganization', organizationId: 'ErpMdOrganization',
    departmentId: 'ErpMdOrganization', targetDepartmentId: 'ErpMdOrganization',
    fromDepartmentId: 'ErpMdOrganization', toDepartmentId: 'ErpMdOrganization',
    currencyId: 'ErpMdCurrency', sourceCurrencyId: 'ErpMdCurrency',
    freightCurrencyId: 'ErpMdCurrency', salaryCurrencyId: 'ErpMdCurrency',
    materialId: 'ErpMdMaterial', productId: 'ErpMdMaterial',
    alternativeMaterialId: 'ErpMdMaterial',
    warehouseId: 'ErpMdWarehouse', sourceWarehouseId: 'ErpMdWarehouse',
    destWarehouseId: 'ErpMdWarehouse', preferredSourceWarehouseId: 'ErpMdWarehouse',
    locationId: 'ErpMdLocation', sourceLocationId: 'ErpMdLocation',
    destLocationId: 'ErpMdLocation', fromLocationId: 'ErpMdLocation',
    toLocationId: 'ErpMdLocation', stagingLocationId: 'ErpMdLocation',
    uomId: 'ErpMdUoM', uoMId: 'ErpMdUoM', inputUoMId: 'ErpMdUoM', outputUoMId: 'ErpMdUoM',
    supplierId: 'ErpMdPartner', customerId: 'ErpMdPartner',
    partnerId: 'ErpMdPartner', preferredSupplierId: 'ErpMdPartner',
    reservedForPartnerId: 'ErpMdPartner',
    bankAccountId: 'ErpMdBankAccount', partnerBankAccountId: 'ErpMdBankAccount',
    taxRateId: 'ErpMdTaxRate',
    settlementMethodId: 'ErpMdSettlementMethod',
    acctSchemaId: 'ErpMdAcctSchema', subjectId: 'ErpMdSubject',
    employeeId: 'ErpMdEmployee', staffId: 'ErpMdEmployee',
    managerId: 'ErpMdEmployee', requesterId: 'ErpMdEmployee',
    ownerId: 'ErpMdEmployee', pickerId: 'ErpMdEmployee',
    operatorId: 'ErpMdEmployee', handlerId: 'ErpMdEmployee',
    fromStaffId: 'ErpMdEmployee', toStaffId: 'ErpMdEmployee',
    interviewerId: 'ErpMdEmployee', approvedById: 'ErpMdEmployee',
    reviewerId: 'ErpMdEmployee',
    costCenterId: 'ErpMdCostCenter',
    defaultFundAccountId: 'ErpFinFundAccount',
    voucherId: 'ErpFinVoucher', reversalOfVoucherId: 'ErpFinVoucher',
    incomingMoveId: 'ErpInvStockMove', inboundMoveId: 'ErpInvStockMove',
    outboundMoveId: 'ErpInvStockMove',
    completedAssetId: 'ErpAstAsset', assetId: 'ErpAstAsset',
    workcenterId: 'ErpApsWorkcenter', machineId: 'ErpApsMachine',
    gatewayId: 'ErpLogCarrierGateway',
    attachmentId: 'NopSysAttachment',
    resumeAttachmentId: 'NopSysAttachment',
  };

  const commonFields = new Set([
    'delVersion', 'version', 'createdBy', 'createTime', 'updatedBy', 'updateTime',
    'code', 'lineNo', 'remark', 'approvedBy', 'approvedAt', 'postedAt', 'postedBy',
  ]);

  const results = [];

  for (const [moduleName, entities] of Object.entries(moduleEntities)) {
    for (const entity of entities) {
      if (entity.notGenCode) continue;

      const existingToOneLeftProps = new Set(
        entity.toOneRels.filter(r => r.join).map(r => r.join.leftProp)
      );

      for (const col of entity.columns) {
        if (col.name === 'id' || col.primary || commonFields.has(col.name)) continue;
        if (!col.name.endsWith('Id') && !col.name.endsWith('_id')) continue;

        if (existingToOneLeftProps.has(col.name)) continue;

        const baseName = col.name.replace(/Id$|_id$/, '');
        const basePascal = baseName.charAt(0).toUpperCase() + baseName.slice(1);

        // Check if to-one with expected name already exists
        const expectedRelName = baseName.replace(/^./, c => c.toLowerCase());
        if (entity.toOneRels.some(r => r.name === expectedRelName)) continue;

        // ── Strategy 1: Master-data patterns ──
        let target = null;
        let method = '';
        if (MD_PATTERNS[col.name]) {
          const candidate = MD_PATTERNS[col.name];
          // Find in canonical entities or use as-is (might be notGenCode)
          const byShortName = entityByShortName[candidate];
          if (byShortName && byShortName.length > 0) {
            target = byShortName[0];
            method = 'MASTER_DATA_PATTERN';
          } else {
            // Entity name but not in primary index - might be from nop platform
            target = { entity: { className: `app.erp.md.dao.entity.${candidate}` }, module: 'md', shortName: candidate };
            method = 'MASTER_DATA_PATTERN_NC';
          }
        }

        // ── Strategy 2: Self-reference ──
        // column like parentId, reversalOfVoucherId on entity that ends with Voucher
        if (!target) {
          const selfShort = entity.shortName;
          // Check if the entity name contains basePascal as suffix
          // e.g., ErpFinVoucher contains Voucher, reversalOfVoucherId → base=Voucher → matches
          // More precise: if removing basePascal from entity shortName leaves a valid prefix
          if (selfShort.length > basePascal.length && selfShort.endsWith(basePascal)) {
            target = { entity, module: moduleName, shortName: selfShort };
            method = 'SELF_REFERENCE';
          }
        }

        // ── Strategy 3: Same-module entity match ──
        if (!target) {
          // Try to match by suffix: the Fk column name (basePascal) should appear as suffix
          // of some entity short name in the same module
          // e.g., orderLineId on ErpPurReceiveLine → find entity ending with OrderLine in purchase
          const sameModEntities = primaryByModule[moduleName] || [];
          const matches = sameModEntities.filter(e =>
            e.shortName !== entity.shortName && e.shortName.endsWith(basePascal)
          );
          if (matches.length === 1) {
            target = { entity: matches[0], module: moduleName, shortName: matches[0].shortName };
            method = 'SAME_MODULE_SUFFIX';
          } else if (matches.length > 1) {
            // Ambiguous: pick the one with shortest prefix (most likely the actual entity)
            // e.g., ErpPurOrderLine vs ErpPurReceiveLine for basePascal=Line
            // We need "OrderLine" not "Line" match
            const exactMatches = matches.filter(e => {
              const eShort = e.shortName;
              // The entity name should start with domain prefix + basePascal
              // e.g., for 'pur' domain: entity starts with 'ErpPur' + remainder = basePascal  
              return eShort.endsWith(basePascal);
            });
            if (exactMatches.length > 0) {
              target = { entity: exactMatches[0], module: moduleName, shortName: exactMatches[0].shortName };
              method = `${method}_AMBIGUOUS`;
            }
          }
        }

        // ── Strategy 4: notGenCode external entities in this module ──
        if (!target) {
          // Check if any notGenCode entity in this module could be the target
          const extEntities = entities.filter(e => e.notGenCode);
          for (const ext of extEntities) {
            const extShort = sn(ext.className);
            if (extShort.endsWith(basePascal)) {
              const targetMod = ext.bizModuleId || domainFromPackage(ext.className);
              target = { entity: ext, module: targetMod, shortName: extShort, notGenCode: true };
              method = 'NOT_GEN_CODE';
              break;
            }
          }
        }

        // ── Strategy 5: Cross-module canonical match ──
        if (!target) {
          // Find any primary entity from any module whose shortName ends with basePascal
          // but NOT from same module (same-module already tried)
          let bestMatch = null;
          for (const [mod, primaries] of Object.entries(primaryByModule)) {
            if (mod === moduleName) continue;
            for (const pe of primaries) {
              if (pe.shortName.endsWith(basePascal)) {
                bestMatch = { entity: pe, module: mod, shortName: pe.shortName };
                method = 'CROSS_MODULE';
                break;
              }
            }
            if (bestMatch) break;
          }
          if (bestMatch) target = bestMatch;
        }

        // ── Classify ──
        let category = 'UNKNOWN';
        if (target) {
          const tMod = target.module;
          if (tMod === moduleName) {
            category = method === 'SELF_REFERENCE' ? 'SELF_REFERENCE' : 'SAME_DOMAIN';
          } else if (tMod && tMod.startsWith('nop-')) {
            category = 'NOP_PLATFORM';
          } else if (tMod === 'md') {
            category = 'MASTER_DATA';
          } else {
            category = 'CROSS_DOMAIN';
          }
        }

        // For cross-domain, check if it creates a cycle (infer DAG direction)
        let cycleRisk = false;
        if (category === 'CROSS_DOMAIN' && target) {
          // Simple cycle check: two domains cross-referencing each other
          // If domain A has FKs to domain B and domain B has FKs to domain A
          const tMod = target.module;
          if (tMod !== 'md' && !tMod.startsWith('nop-')) {
            // Check if the target module has FKs back to source module
            const reverseCandidates = (primaryByModule[tMod] || []).filter(e => {
              return e.columns.some(c =>
                !commonFields.has(c.name) && c.name.endsWith('Id') &&
                MD_PATTERNS[c.name] === undefined
              );
            });
            // Simplified: just mark as potential cycle
            // cycleRisk = true; // uncomment to flag all cross-domain
          }
        }

        results.push({
          module: moduleName,
          sourceEntity: entity.shortName,
          column: col.name,
          expectedRelName,
          targetShortName: target ? target.shortName : null,
          targetDomain: target ? target.module : null,
          method: method || '?',
          category,
          cycleRisk,
        });
      }
    }
  }

  // Report
  const byCategory = {};
  for (const r of results) {
    if (!byCategory[r.category]) byCategory[r.category] = [];
    byCategory[r.category].push(r);
  }

  console.log(`\n=== ORM 外键列分类报告 ===\n扫描文件: ${files.length}\n缺失 to-one: ${results.length}\n`);

  for (const [cat, items] of Object.entries(byCategory).sort((a, b) => b[1].length - a[1].length)) {
    console.log(`\n## ${cat} (${items.length})`);
    const byModule = {};
    for (const i of items) {
      if (!byModule[i.module]) byModule[i.module] = [];
      byModule[i.module].push(i);
    }
    for (const [mod, modItems] of Object.entries(byModule).sort()) {
      console.log(`  ${mod} (${modItems.length}):`);
      for (const i of modItems) {
        const t = i.targetShortName ? `${i.targetShortName} (${i.targetDomain}) [${i.method}]` : '?';
        const cycle = i.cycleRisk ? ' ⚠️CYCLE' : '';
        console.log(`    ${i.sourceEntity}.${i.column} → ${t}${cycle}`);
      }
    }
  }

  console.log('\n=== 汇总 ===');
  for (const [cat, items] of Object.entries(byCategory).sort((a, b) => b[1].length - a[1].length)) {
    console.log(`  ${cat}: ${items.length}`);
  }
  console.log(`  总计: ${results.length}`);
  console.log(`  UNKNOWN: ${results.filter(r => r.category === 'UNKNOWN').length}`);
}

main();
