#!/usr/bin/env node
/**
 * Auto-apply safe <to-one> fixes for MISSING_TO_ONE items.
 * Safe = same-module, master-data, or NOP platform entities.
 * Cross-domain items are skipped (need DAG audit).
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs';
import path from 'path';

const rootDir = process.argv[2] || process.cwd();

function parseXMLtext(text) {
  const entities = [];
  const entityRegex = /<entity\s+([^>]*?)(\/>|>([\s\S]*?)<\/entity>)/g;
  let m;
  while ((m = entityRegex.exec(text)) !== null) {
    const attrs = m[1];
    const body = m[3] || '';
    const className = extract(attrs, 'className');
    if (!className) continue;
    const name = extract(attrs, 'name') || className;
    const shortName = className.split('.').pop();
    const notGenCode = extract(attrs, 'notGenCode') === 'true';
    const bizModuleId = extract(attrs, 'biz:moduleId') || '';
    const columns = [];
    const colRegex = /<column\s+([^>]*?)(\/|><\/column>)/g;
    let c;
    while ((c = colRegex.exec(body)) !== null) {
      const ca = c[1];
      const colName = extract(ca, 'name');
      const primary = extract(ca, 'primary') === 'true';
      if (colName) columns.push({ name: colName, primary });
    }
    const toOneRels = [];
    const relRegex = /<to-one\s+[^>]*>/g;
    let r;
    while ((r = relRegex.exec(body)) !== null) {
      const ra = r[0];
      const relName = extract(ra, 'name');
      const refEntity = extract(ra, 'refEntityName');
      toOneRels.push({ name: relName, refEntityName: refEntity });
    }
    entities.push({ className, shortName, name, notGenCode, bizModuleId, columns, toOneRels });
  }
  return entities;
}

function extract(s, name) {
  if (!s) return null;
  const e = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const m = s.match(new RegExp(`${e}\\s*=\\s*"([^"]*)"`, 'i'));
  return m ? m[1] : null;
}

// All master-data classNames (from ErpMd* pattern)
function isMasterData(className) {
  return className && className.includes('.dao.entity.ErpMd');
}

// NOP platform entities
function isNopPlatform(className) {
  return className && (className.startsWith('io.nop.auth.dao.entity.') || className.startsWith('io.nop.sys.dao.entity.'));
}

const commonFields = new Set([
  'delVersion', 'version', 'createdBy', 'createTime', 'updatedBy', 'updateTime',
  'code', 'lineNo', 'remark', 'approvedBy', 'approvedAt', 'postedAt', 'postedBy',
]);

// Master-data patterns (colName → target className)
const MD_PATTERNS = {
  orgId: 'app.erp.md.dao.entity.ErpMdOrganization',
  currencyId: 'app.erp.md.dao.entity.ErpMdCurrency',
  materialId: 'app.erp.md.dao.entity.ErpMdMaterial',
  warehouseId: 'app.erp.md.dao.entity.ErpMdWarehouse',
  locationId: 'app.erp.md.dao.entity.ErpMdLocation',
  partnerId: 'app.erp.md.dao.entity.ErpMdPartner',
  supplierId: 'app.erp.md.dao.entity.ErpMdPartner',
  customerId: 'app.erp.md.dao.entity.ErpMdPartner',
  bankAccountId: 'app.erp.md.dao.entity.ErpMdBankAccount',
  taxRateId: 'app.erp.md.dao.entity.ErpMdTaxRate',
  settlementMethodId: 'app.erp.md.dao.entity.ErpMdSettlementMethod',
  acctSchemaId: 'app.erp.md.dao.entity.ErpMdAcctSchema',
  subjectId: 'app.erp.md.dao.entity.ErpMdSubject',
  employeeId: 'app.erp.md.dao.entity.ErpMdEmployee',
  staffId: 'app.erp.md.dao.entity.ErpMdEmployee',
  requesterId: 'app.erp.md.dao.entity.ErpMdEmployee',
  uomId: 'app.erp.md.dao.entity.ErpMdUoM',
  uoMId: 'app.erp.md.dao.entity.ErpMdUoM',
  costCenterId: 'app.erp.md.dao.entity.ErpMdCostCenter',
  managerId: 'app.erp.md.dao.entity.ErpMdEmployee',
  departmentId: 'app.erp.md.dao.entity.ErpMdOrganization',
  ownerId: 'app.erp.md.dao.entity.ErpMdEmployee',
  pickerId: 'app.erp.md.dao.entity.ErpMdEmployee',
  operatorId: 'app.erp.md.dao.entity.ErpMdEmployee',
  handlerId: 'app.erp.md.dao.entity.ErpMdEmployee',
  interviewerId: 'app.erp.md.dao.entity.ErpMdEmployee',
  reviewerId: 'app.erp.md.dao.entity.ErpMdEmployee',
  approvedById: 'app.erp.md.dao.entity.ErpMdEmployee',
  approverId: 'app.erp.md.dao.entity.ErpMdEmployee',
  teamLeaderId: 'app.erp.md.dao.entity.ErpMdEmployee',
  fromStaffId: 'app.erp.md.dao.entity.ErpMdEmployee',
  toStaffId: 'app.erp.md.dao.entity.ErpMdEmployee',
  fromDepartmentId: 'app.erp.md.dao.entity.ErpMdOrganization',
  toDepartmentId: 'app.erp.md.dao.entity.ErpMdOrganization',
  fromLocationId: 'app.erp.md.dao.entity.ErpMdLocation',
  toLocationId: 'app.erp.md.dao.entity.ErpMdLocation',
  sourceLocationId: 'app.erp.md.dao.entity.ErpMdLocation',
  destLocationId: 'app.erp.md.dao.entity.ErpMdLocation',
  sourceWarehouseId: 'app.erp.md.dao.entity.ErpMdWarehouse',
  destWarehouseId: 'app.erp.md.dao.entity.ErpMdWarehouse',
  stagingLocationId: 'app.erp.md.dao.entity.ErpMdLocation',
  inputUoMId: 'app.erp.md.dao.entity.ErpMdUoM',
  outputUoMId: 'app.erp.md.dao.entity.ErpMdUoM',
  alternativeMaterialId: 'app.erp.md.dao.entity.ErpMdMaterial',
  productId: 'app.erp.md.dao.entity.ErpMdMaterial',
  freightCurrencyId: 'app.erp.md.dao.entity.ErpMdCurrency',
  salaryCurrencyId: 'app.erp.md.dao.entity.ErpMdCurrency',
  partnerBankAccountId: 'app.erp.md.dao.entity.ErpMdBankAccount',
  reservedForPartnerId: 'app.erp.md.dao.entity.ErpMdPartner',
  suggestedSupplierId: 'app.erp.md.dao.entity.ErpMdPartner',
  voucherId: 'app.erp.fin.dao.entity.ErpFinVoucher',
  reversalOfVoucherId: 'app.erp.fin.dao.entity.ErpFinVoucher',
  incomingMoveId: 'app.erp.inv.dao.entity.ErpInvStockMove',
  inboundMoveId: 'app.erp.inv.dao.entity.ErpInvStockMove',
  outboundMoveId: 'app.erp.inv.dao.entity.ErpInvStockMove',
  completedAssetId: 'app.erp.ast.dao.entity.ErpAstAsset',
  assetId: 'app.erp.ast.dao.entity.ErpAstAsset',
  defaultFundAccountId: 'app.erp.fin.dao.entity.ErpFinFundAccount',
  attachmentId: 'io.nop.sys.dao.entity.NopSysAttachment',
  resumeAttachmentId: 'io.nop.sys.dao.entity.NopSysAttachment',
  userAccountId: 'io.nop.auth.dao.entity.NopAuthUser',
  assignedToId: 'io.nop.auth.dao.entity.NopAuthUser',
};

// Manual overrides for UNKNOWN items
const MANUAL_OVERRIDES = {
  'aps:ErpApsOperationOrder:assignedToId': 'io.nop.auth.dao.entity.NopAuthUser',
  'purchase:ErpPurRequisitionLine:suggestedSupplierId': 'app.erp.md.dao.entity.ErpMdPartner',
  // quality:ErpQaInspectionLine:parameterId — no ErpQaInspectionParameter entity
  'crm:ErpCrmTeam:teamLeaderId': 'app.erp.md.dao.entity.ErpMdEmployee',
  'cs:ErpCsTicketType:defaultSlaPolicyId': 'app.erp.cs.dao.entity.ErpCsSlaPolicy',
  'cs:ErpCsServiceCatalogItem:fulfillmentProcessId': 'app.erp.cs.dao.entity.ErpCsFulfillmentProcess',
  'finance:ErpFinReconciliationLine:paymentItemId': 'app.erp.fin.dao.entity.ErpFinArApItem',
  'finance:ErpFinReconciliationLine:invoiceItemId': 'app.erp.fin.dao.entity.ErpFinArApItem',
  'maintenance:ErpMntDowntimeEntry:relatedJobOrderId': 'app.erp.mnt.dao.entity.ErpMntDowntimeEntry',
  'master-data:ErpMdSettlementMethod:defaultFundAccountId': 'app.erp.fin.dao.entity.ErpFinFundAccount',
  'contract:ErpCtApprovalRecord:approvalMatrixId': 'app.erp.ct.dao.entity.ErpCtApprovalMatrix',
  'contract:ErpCtSignatureRequest:providerRequestId': 'app.erp.ct.dao.entity.ErpCtSignatureRequest',
  'b2b:ErpB2bMftConfig:certId': 'app.erp.b2b.dao.entity.ErpB2bMftCertificate',
};

// Items where the FK target entity doesn't exist (design gaps)
const DESIGN_GAPS = new Set([
  'aps:ErpApsConstraint:machineId',      // no ErpApsMachine entity
  'aps:ErpApsDispatchLog:workcenterId',   // no ErpApsWorkcenter
  'aps:ErpApsDispatchRule:workcenterId',  // no ErpApsWorkcenter
  'aps:ErpApsOperationOrder:machineId',   // no ErpApsMachine
  'aps:ErpApsOperationOrder:workOrderId', // no ErpApsWorkOrder
  'aps:ErpApsOpRouting:machineId',        // no ErpApsMachine
  'aps:ErpApsOpRouting:operationId',      // no ErpApsOperation
  'b2b:ErpB2bMftConfig:localAs2Id',      // no ErpB2bAs2 entity
  'b2b:ErpB2bMftConfig:remoteAs2Id',     // no ErpB2bAs2 entity
  'b2b:ErpB2bMftLog:messageId',          // ambiguous — no ErpB2bMessage
  'cs:ErpCsKnowledgeBase:categoryId',    // no matching entity in cs module
  'cs:ErpCsServiceCatalogItem:fulfillmentProcessId', // VARCHAR business key, not entity FK
  'crm:ErpCrmEvent:contactId',          // no ErpCrmContact entity
  'crm:ErpCrmTerritoryAssignmentRule:groupId', // no ErpCrmGroup entity
  'quality:ErpQaInspectionLine:parameterId', // no ErpQaInspectionParameter entity
]);

// These are cross-domain and need DAG audit → skip
const CROSS_DOMAIN = new Set([
  'crm:ErpCrmTerritoryAssignmentRule:groupId',
  'cs:ErpCsTimeEntry:projectId',
  'cs:ErpCsTimeEntry:taskId',
  'drp:ErpInvDrpCrossDock:inboundMoveId',
  'drp:ErpInvDrpCrossDock:outboundMoveId',
  'logistics:ErpLogCarrier:gatewayId',
  'logistics:ErpLogShipmentLog:gatewayId',
  'maintenance:ErpMntEquipment:assetId',
  'maintenance:ErpMntEquipment:workcenterId',
  // maintenance:ErpMntSparePartUsage:warehouseId is master-data, fixable
  'purchase:ErpPurOrderLine:projectId',
  'sales:ErpSalOrderLine:projectId',
  'master-data:ErpMdSettlementMethod:defaultFundAccountId',
  'finance:ErpFinGlBalance:projectId',
]);

function main() {
  const files = [];
  for (const entry of readdirSync(rootDir)) {
    if (!entry.startsWith('module-')) continue;
    const modelDir = path.join(rootDir, entry, 'model');
    try {
      if (!statSync(modelDir).isDirectory()) continue;
      for (const f of readdirSync(modelDir).filter(f => f.endsWith('.orm.xml')))
        files.push({ fullPath: path.join(modelDir, f), dir: entry });
    } catch (e) {}
  }

  // Index all entities (for cross-module lookups)
  const allEntities = {}; // shortName → { className, module }
  for (const { fullPath, dir } of files) {
    const text = readFileSync(fullPath, 'utf-8');
    const ents = parseXMLtext(text);
    const mod = dir.replace('module-', '');
    for (const e of ents) {
      if (!e.notGenCode) {
        allEntities[e.shortName] = { className: e.className, module: mod };
      }
    }
  }

  let totalFixed = 0;

  for (const { fullPath, dir } of files) {
    const text = readFileSync(fullPath, 'utf-8');
    const ents = parseXMLtext(text);
    const mod = dir.replace('module-', '');
    let changed = false;

    let newText = text;

    for (const entity of ents) {
      if (entity.notGenCode) continue;

      const existingLeftProps = new Set(
        entity.toOneRels.map(r => {
          const m = newText.match(new RegExp(`<to-one[^>]*?name="${r.name}"[^>]*?>\\s*<join>\\s*<on\\s+leftProp="([^"]*)"`));
          return m ? m[1] : null;
        }).filter(Boolean)
      );

      for (const col of entity.columns) {
        if (col.name === 'id' || col.primary || commonFields.has(col.name)) continue;
        if (!col.name.endsWith('Id') && !col.name.endsWith('_id')) continue;
        if (existingLeftProps.has(col.name)) continue;

        const baseName = col.name.replace(/Id$|_id$/, '');
        const expectedRelName = baseName.replace(/^./, c => c.toLowerCase());

        // Check if to-one with this name already exists
        if (entity.toOneRels.some(r => r.name === expectedRelName)) continue;

        const overrideKey = `${mod}:${entity.shortName}:${col.name}`;

        // Skip cross-domain items
        if (CROSS_DOMAIN.has(overrideKey)) continue;

        let targetClass = null;

        // 1. Manual overrides
        if (MANUAL_OVERRIDES[overrideKey]) {
          targetClass = MANUAL_OVERRIDES[overrideKey];
        }

        // 2. Master-data patterns
        if (!targetClass && MD_PATTERNS[col.name]) {
          targetClass = MD_PATTERNS[col.name];
        }

        // 3. Same-module entity suffix match
        if (!targetClass) {
          const basePascal = baseName.charAt(0).toUpperCase() + baseName.slice(1);
          const sameMod = ents.filter(e =>
            !e.notGenCode && e.shortName !== entity.shortName && e.shortName.endsWith(basePascal)
          );
          if (sameMod.length >= 1) {
            targetClass = sameMod[0].className;
          }
        }

        // 4. Master-data by entity name prefix
        if (!targetClass) {
          // Check if target entity is ErpMd* (master-data)
          const basePascal = baseName.charAt(0).toUpperCase() + baseName.slice(1);
          const mdEntity = allEntities[`ErpMd${basePascal}`];
          if (mdEntity && isMasterData(mdEntity.className)) {
            targetClass = mdEntity.className;
          }
        }

        // 5. NOP platform by suffix pattern
        if (!targetClass) {
          const basePascal = baseName.charAt(0).toUpperCase() + baseName.slice(1);
          if (basePascal === 'Attachment') {
            targetClass = 'io.nop.sys.dao.entity.NopSysAttachment';
          } else if (basePascal === 'UserAccount' || basePascal === 'AssignedTo') {
            targetClass = 'io.nop.auth.dao.entity.NopAuthUser';
          }
        }

        if (targetClass) {
          // Only apply if target is same-module, master-data, or NOP platform
          const targetModule = allEntities[targetClass.split('.').pop()]?.module;
          const isSafe = targetModule === mod || isMasterData(targetClass) || isNopPlatform(targetClass);

          if (isSafe) {
            const snippet = `                <to-one name="${expectedRelName}" refEntityName="${targetClass}" tagSet="pub">\n                    <join><on leftProp="${col.name}" rightProp="id"/></join>\n                </to-one>`;

            // Find the entity's <relations> block and insert
            const entityPattern = `<entity\\s+[^>]*?(?:className|name)="[^"]*\\b${entity.shortName}\\b[^>]*>`;
            const entityMatch = new RegExp(entityPattern).exec(newText);
            if (entityMatch) {
              const entityStart = entityMatch.index;
              const entityTagEnd = entityMatch[0].match(/\/>/)
                ? entityMatch.index + entityMatch[0].length
                : newText.indexOf('</entity>', entityMatch.index) + '</entity>'.length;
              const entityBody = newText.substring(entityStart, entityTagEnd);

              const relBlockMatch = entityBody.match(/(\s*<relations>)([\s\S]*?)(<\/relations>)/);
              if (relBlockMatch) {
                const beforeRel = relBlockMatch[1];
                const relContent = relBlockMatch[2];
                const afterRel = relBlockMatch[3];
                // Insert before </relations>
                const insertion = relContent.trimEnd() + '\n' + snippet;
                const old = beforeRel + relContent + afterRel;
                const replacement = beforeRel + insertion + '\n            ' + afterRel;
                newText = newText.substring(0, entityStart) +
                  newText.substring(entityStart, entityTagEnd).replace(old, replacement) +
                  newText.substring(entityTagEnd);
                changed = true;
                totalFixed++;
                console.log(`  [FIXED] ${mod}:${entity.shortName}:${col.name} → ${targetClass}`);
              } else {
                // No <relations> block – create one
                const closingTag = '</entity>';
                const closingIdx = newText.indexOf(closingTag, entityStart);
                if (closingIdx > 0) {
                  const relBlock = `\n            <relations>\n${snippet}\n            </relations>\n        `;
                  newText = newText.substring(0, closingIdx) + relBlock + newText.substring(closingIdx);
                  changed = true;
                  totalFixed++;
                  console.log(`  [FIXED+NEW] ${mod}:${entity.shortName}:${col.name} → ${targetClass}`);
                }
              }
            }
          }
        }
      }
    }

    if (changed) {
      writeFileSync(fullPath, newText, 'utf-8');
      console.log(`\n  ✓ Wrote ${fullPath}\n`);
    }
  }

  console.log(`\n=== Total safe fixes applied: ${totalFixed} ===`);
  console.log(`Remaining (skipped cross-domain + unresolved): run check-orm-relations.mjs to see`);
}

main();
