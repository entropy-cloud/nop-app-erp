#!/usr/bin/env node
/**
 * Generate <to-one> XML snippets for all missing FK columns.
 * Uses categorize-orm-fks output + manual overrides for UNKNOWN items.
 *
 * Output: per-module XML snippets that can be inserted into each orm.xml's entity <relations> block.
 */
import { readFileSync, readdirSync, statSync } from 'fs';
import path from 'path';

const rootDir = process.argv[2] || process.cwd();

// ── Include the categorization logic (reuse from categorize-orm-fks.mjs) ──

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

    entities.push({ className, shortName, name, notGenCode, bizModuleId, columns, toOneRels });
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

function sn(className) { return className ? className.split('.').pop() : ''; }

// Manual overrides for UNKNOWN items (domain knowledge, not derivable from ORM structure)
const MANUAL_OVERRIDES = {
  'aps:ErpApsOperationOrder:assignedToId': { className: 'io.nop.auth.dao.entity.NopAuthUser', relName: 'assignedTo', domain: 'nop-platform' },
  'purchase:ErpPurRequisitionLine:suggestedSupplierId': { className: 'app.erp.md.dao.entity.ErpMdPartner', relName: 'suggestedSupplier', domain: 'md' },
  'quality:ErpQaInspectionLine:parameterId': { className: 'app.erp.qa.dao.entity.ErpQaInspectionParameter', relName: 'parameter', domain: 'quality' },
  'crm:ErpCrmTeam:teamLeaderId': { className: 'app.erp.md.dao.entity.ErpMdEmployee', relName: 'teamLeader', domain: 'md' },
  'hr:ErpHrEmployee:superiorId': { className: 'app.erp.md.dao.entity.ErpMdEmployee', relName: 'superior', domain: 'md' },
  'hr:ErpHrEmployee:userAccountId': { className: 'io.nop.auth.dao.entity.NopAuthUser', relName: 'userAccount', domain: 'nop-platform' },
  'hr:ErpHrSalarySimulation:sourceSalaryId': { className: 'app.erp.hr.dao.entity.ErpHrSalarySimulation', relName: 'sourceSalary', domain: 'hr' },
  'hr:ErpHrSalarySimulation:convertedSalaryId': { className: 'app.erp.hr.dao.entity.ErpHrSalarySimulation', relName: 'convertedSalary', domain: 'hr' },
  'hr:ErpHrShiftAssignment:replacedByAssignmentId': { className: 'app.erp.hr.dao.entity.ErpHrShiftAssignment', relName: 'replacedByAssignment', domain: 'hr' },
  'hr:ErpHrShiftRotationPattern:groupId': { className: 'app.erp.hr.dao.entity.ErpHrShiftRotationGroup', relName: 'group', domain: 'hr' },
  'hr:ErpHrShiftSwapRequest:sourceAssignmentId': { className: 'app.erp.hr.dao.entity.ErpHrShiftAssignment', relName: 'sourceAssignment', domain: 'hr' },
  'hr:ErpHrShiftSwapRequest:targetAssignmentId': { className: 'app.erp.hr.dao.entity.ErpHrShiftAssignment', relName: 'targetAssignment', domain: 'hr' },
  'hr:ErpCtApprovalRecord:approverId': { className: 'app.erp.md.dao.entity.ErpMdEmployee', relName: 'approver', domain: 'md' },
  'contract:ErpCtSignatureRequest:providerRequestId': { className: 'app.erp.ct.dao.entity.ErpCtSignatureRequest', relName: 'providerRequest', domain: 'contract' }, // self-ref
  'cs:ErpCsTicketType:defaultSlaPolicyId': { className: 'app.erp.cs.dao.entity.ErpCsSlaPolicy', relName: 'defaultSlaPolicy', domain: 'cs' },
  'cs:ErpCsServiceCatalogItem:fulfillmentProcessId': { className: 'app.erp.cs.dao.entity.ErpCsFulfillmentProcess', relName: 'fulfillmentProcess', domain: 'cs' },
  'finance:ErpFinReconciliationLine:paymentItemId': { className: 'app.erp.fin.dao.entity.ErpFinArApItem', relName: 'paymentItem', domain: 'finance' },
  'finance:ErpFinReconciliationLine:invoiceItemId': { className: 'app.erp.fin.dao.entity.ErpFinArApItem', relName: 'invoiceItem', domain: 'finance' },
  'manufacturing:ErpMfgWorkOrder:sourceMrpPlanId': { className: 'app.erp.mfg.dao.entity.ErpMfgMrpPlan', relName: 'sourceMrpPlan', domain: 'manufacturing' },
  'manufacturing:ErpMfgMrpPlanLine:parentLineId': { className: 'app.erp.mfg.dao.entity.ErpMfgMrpPlanLine', relName: 'parentLine', domain: 'manufacturing' }, // self-ref
  'manufacturing:ErpMfgBatchGenealogy:inputLotId': { className: 'app.erp.inv.dao.entity.ErpInvBatch', relName: 'inputLot', domain: 'inventory' },
  'manufacturing:ErpMfgBatchGenealogy:outputLotId': { className: 'app.erp.inv.dao.entity.ErpInvBatch', relName: 'outputLot', domain: 'inventory' },
  'maintenance:ErpMntDowntimeEntry:relatedJobOrderId': { className: 'app.erp.mnt.dao.entity.ErpMntDowntimeEntry', relName: 'relatedJobOrder', domain: 'maintenance' }, // self-ref
  'aps:ErpApsOpRouting:operationId': { className: 'app.erp.aps.dao.entity.ErpApsOperation', relName: 'operation', domain: 'aps' },
  'aps:ErpApsOperationOrder:assignedToId': { className: 'io.nop.auth.dao.entity.NopAuthUser', relName: 'assignedTo', domain: 'nop-platform' },
};

// Also fix some wrong cross-domain matches
const CORRECTIONS = {
  // These were matched to the wrong cross-domain entity by the generic script
  'crm:ErpCrmEvent:contactId': { className: 'app.erp.crm.dao.entity.ErpCrmContact', relName: 'contact', domain: 'crm' },
  'cs:ErpCsTimeEntry:taskId': { className: 'app.erp.prj.dao.entity.ErpPrjTask', relName: 'task', domain: 'projects' },
  'master-data:ErpMdSettlementMethod:defaultFundAccountId': { className: 'app.erp.fin.dao.entity.ErpFinFundAccount', relName: 'defaultFundAccount', domain: 'finance' },
};

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
  preferredSourceWarehouseId: 'app.erp.md.dao.entity.ErpMdWarehouse',
  preferredSupplierId: 'app.erp.md.dao.entity.ErpMdPartner',
  stagingLocationId: 'app.erp.md.dao.entity.ErpMdLocation',
  inputUoMId: 'app.erp.md.dao.entity.ErpMdUoM',
  outputUoMId: 'app.erp.md.dao.entity.ErpMdUoM',
  alternativeMaterialId: 'app.erp.md.dao.entity.ErpMdMaterial',
  productId: 'app.erp.md.dao.entity.ErpMdMaterial',
  freightCurrencyId: 'app.erp.md.dao.entity.ErpMdCurrency',
  salaryCurrencyId: 'app.erp.md.dao.entity.ErpMdCurrency',
  partnerBankAccountId: 'app.erp.md.dao.entity.ErpMdBankAccount',
  reservedForPartnerId: 'app.erp.md.dao.entity.ErpMdPartner',
  attachmentId: 'io.nop.sys.dao.entity.NopSysAttachment',
  resumeAttachmentId: 'io.nop.sys.dao.entity.NopSysAttachment',
  voucherId: 'app.erp.fin.dao.entity.ErpFinVoucher',
  reversalOfVoucherId: 'app.erp.fin.dao.entity.ErpFinVoucher',
  incomingMoveId: 'app.erp.inv.dao.entity.ErpInvStockMove',
  inboundMoveId: 'app.erp.inv.dao.entity.ErpInvStockMove',
  outboundMoveId: 'app.erp.inv.dao.entity.ErpInvStockMove',
  completedAssetId: 'app.erp.ast.dao.entity.ErpAstAsset',
  assetId: 'app.erp.ast.dao.entity.ErpAstAsset',
  machineId: 'app.erp.aps.dao.entity.ErpApsMachine',
  workcenterId: 'app.erp.aps.dao.entity.ErpApsWorkcenter',
  gatewayId: 'app.erp.log.dao.entity.ErpLogCarrierGateway',
  defaultFundAccountId: 'app.erp.fin.dao.entity.ErpFinFundAccount',
  suggestedSupplierId: 'app.erp.md.dao.entity.ErpMdPartner',
};

const commonFields = new Set([
  'delVersion', 'version', 'createdBy', 'createTime', 'updatedBy', 'updateTime',
  'code', 'lineNo', 'remark', 'approvedBy', 'approvedAt', 'postedAt', 'postedBy',
]);

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

  const moduleEntities = {}; // moduleName → entities[]
  for (const file of files) {
    const mod = path.basename(file).replace('app-erp-', '').replace('.orm.xml', '');
    moduleEntities[mod] = parseXML(file);
  }

  // Build entity class name lookup: shortName → (className, domain)
  const entityLookup = {};
  for (const [mod, ents] of Object.entries(moduleEntities)) {
    for (const e of ents) {
      if (!e.notGenCode) {
        entityLookup[e.shortName] = { className: e.className, module: mod };
      }
    }
  }

  // Generate per-module XML snippets
  for (const [moduleName, entities] of Object.entries(moduleEntities).sort()) {
    const moduleSnippets = [];

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
        const expectedRelName = baseName.replace(/^./, c => c.toLowerCase());
        if (entity.toOneRels.some(r => r.name === expectedRelName)) continue;

        // Determine target className
        let targetClass = null;
        let targetRelName = expectedRelName;
        let isFixable = false;

        // 1. Manual overrides
        const overrideKey = `${moduleName}:${entity.shortName}:${col.name}`;
        if (MANUAL_OVERRIDES[overrideKey]) {
          const o = MANUAL_OVERRIDES[overrideKey];
          targetClass = o.className;
          targetRelName = o.relName;
          isFixable = true;
        }

        // 2. Corrections
        if (!targetClass && CORRECTIONS[overrideKey]) {
          const o = CORRECTIONS[overrideKey];
          targetClass = o.className;
          targetRelName = o.relName;
          isFixable = true;
        }

        // 3. Master-data patterns
        if (!targetClass && MD_PATTERNS[col.name]) {
          targetClass = MD_PATTERNS[col.name];
          isFixable = true;
        }

        // 4. Same-module entity suffix match
        if (!targetClass) {
          const basePascal = baseName.charAt(0).toUpperCase() + baseName.slice(1);
          const sameModEntities = entities.filter(e =>
            !e.notGenCode && e.shortName !== entity.shortName && e.shortName.endsWith(basePascal)
          );
          if (sameModEntities.length === 1) {
            targetClass = sameModEntities[0].className;
            isFixable = true;
          } else if (sameModEntities.length > 1) {
            // Take the first
            targetClass = sameModEntities[0].className;
            isFixable = true;
          }
        }

        // 5. Cross-module (first match by suffix)
        if (!targetClass) {
          const basePascal = baseName.charAt(0).toUpperCase() + baseName.slice(1);
          for (const [mod, ents] of Object.entries(moduleEntities)) {
            if (mod === moduleName) continue;
            for (const e of ents) {
              if (!e.notGenCode && e.shortName.endsWith(basePascal)) {
                targetClass = e.className;
                isFixable = true;
                break;
              }
            }
            if (targetClass) break;
          }
        }

        if (isFixable && targetClass) {
          const targetShort = targetClass.split('.').pop();
          const snippet = `                <to-one name="${targetRelName}" refEntityName="${targetClass}" tagSet="pub">
                    <join><on leftProp="${col.name}" rightProp="id"/></join>
                </to-one>`;
          moduleSnippets.push({
            entity: entity.shortName,
            column: col.name,
            snippet,
            targetClass,
            targetShort,
          });
        }
      }
    }

    // Output module snippets
    if (moduleSnippets.length > 0) {
      console.log(`\n## ${moduleName} (${moduleSnippets.length} fixes)`);
      const byEntity = {};
      for (const s of moduleSnippets) {
        if (!byEntity[s.entity]) byEntity[s.entity] = [];
        byEntity[s.entity].push(s);
      }
      for (const [entityName, items] of Object.entries(byEntity).sort()) {
        console.log(`\n### ${entityName}`);
        console.log(`在 <entity className="..." name="${entityName}"> 的 <relations> 块中添加：`);
        for (const i of items) {
          console.log(i.snippet);
        }
      }
    }
  }
}

main();
