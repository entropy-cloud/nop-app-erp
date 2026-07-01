#!/usr/bin/env node
/**
 * 为所有 `<to-many>` 添加 `ext:estRows`（基于中型 ERP 业务分析）
 *
 * 思路：
 * - 先按 (entity, relation) 精确匹配定义业务合理估值
 * - 未精确匹配的按关系名/实体名后缀回落
 * - 默认值 30
 *
 * 使用: node tools/add-est-rows.mjs [--force]  加 --force 会覆盖已有值
 */

const FORCE = process.argv.includes('--force');
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs';
import { join } from 'path';

// 按 (entityShortName, relationName) 精确映射
const ENTITY_REL_MAP = {
    // === 主数据 ===
    'ErpMdMaterial':           { skus: 10 },           // 5-20 个SKU
    'ErpMdMaterialCategory':   { children: 10 },        // 分类树5-15子节点
    'ErpMdPartner':            { addresses: 5, contacts: 10, bankAccounts: 5 },  // 地址/银行少，联系人稍多
    'ErpMdWarehouse':          { locations: 100 },      // 中型仓库50-200库位
    'ErpMdSubject':            { children: 10 },         // 科目树子节点
    'ErpMdAcctSchema':         { coaMappings: 300 },    // 科目表200-500映射
    'ErpMdOrganization':       { children: 10 },
    'ErpMdCostCenter':         { children: 10 },

    // === 采购 ===
    'ErpPurRequisition':       { lines: 30 },
    'ErpPurRfq':               { lines: 30, quotations: 10 },  // 询价通常3-10家报价
    'ErpPurQuotation':         { lines: 30 },
    'ErpPurOrder':             { lines: 30 },
    'ErpPurReceive':           { lines: 30 },
    'ErpPurInvoice':           { lines: 30 },
    'ErpPurPayment':           { lines: 10 },            // 一次付款覆盖1-10个发票
    'ErpPurReturn':            { lines: 30 },

    // === 销售 ===
    'ErpSalQuotation':         { lines: 30 },
    'ErpSalOrder':             { lines: 30 },
    'ErpSalDelivery':          { lines: 30 },
    'ErpSalInvoice':           { lines: 30 },
    'ErpSalReceipt':           { lines: 10 },            // 一次收款1-10发票
    'ErpSalReturn':            { lines: 30 },

    // === 库存 ===
    'ErpInvStockMove':         { lines: 30 },
    'ErpInvReservation':       { lines: 30 },
    'ErpInvTransferOrder':     { lines: 30 },
    'ErpInvStockTake':         { lines: 30 },
    'ErpInvPickingOrder':      { lines: 30 },

    // === 财务 ===
    'ErpFinVoucher':           { lines: 10, billLinks: 5 },  // 凭证明细2-20行，单据链接1-5
    'ErpFinVoucherTemplate':   { lines: 10 },               // 模板分录行
    'ErpFinAccountingPeriod':  { statusRecords: 24 },        // 月期间12-24状态记录
    'ErpFinReconciliation':    { lines: 30 },
    'ErpFinBankReconciliation':{ adjustmentLines: 10 },      // 银行调节表调整项通常1-10

    // === 制造 ===
    'ErpMfgBom':               { lines: 50, operations: 20, byproducts: 5 },  // BOM行5-200，工序3-20，副产品0-5
    'ErpMfgRouting':           { operations: 20 },                            // 工艺路线3-20工序
    'ErpMfgWorkOrder':         { lines: 20, jobCards: 5 },                    // 工单行1-20，作业卡1-10
    'ErpMfgMrpPlan':           { lines: 500, demands: 500 },                  // MRP计划行数百到数千
    'ErpMfgMaterialIssue':     { lines: 30 },
    'ErpMfgSubcontractOrder':  { lines: 20 },
    'ErpMfgCostRollup':        { lines: 100 },                                // 成本滚算50-500行

    // === 质量 ===
    'ErpQaInspection':         { lines: 30 },
    'ErpQaInspectionTemplate': { lines: 30 },
    'ErpQaNonConformance':     { actions: 10 },                               // 纠正措施通常1-10

    // === CRM ===
    'ErpCrmLead':              { events: 20, activities: 20, convLogs: 50 },  // 线索活动/沟通记录

    // === 合同 ===
    'ErpCtContract':           { lines: 20, versions: 10 },                   // 合同行1-50，版本1-20
    'ErpCtContractLine':       { invoicePlans: 12, consumptionLines: 36 },    // 按月≤12，消耗行可按36月

    // === 项目 ===
    'ErpPrjProject':           { tasks: 50, timesheets: 200, members: 20, budgets: 10, milestones: 10 },
    'ErpPrjTask':              { childTasks: 10, timesheets: 30 },
    'ErpPrjBudget':            { lines: 30 },
    'ErpPrjCostCollection':    { lines: 30 },
    'ErpPrjBilling':           { lines: 20 },

    // === HR ===
    'ErpHrTimesheet':          { lines: 20 },
    'ErpHrSurvey':             { questions: 20, responses: 50 },
    'ErpHrSurveyResponse':     { answers: 20 },
    'ErpHrCompetency':         { levels: 5 },                                 // 能力等级3-10
    'ErpHrAssessment':         { details: 20 },
    'ErpHrDevelopmentPlan':    { items: 10 },

    // === 维护 ===
    'ErpMntEquipment':         { visits: 50, schedules: 10, requests: 20, sparePartUsages: 20, downtimeEntries: 20 },
    'ErpMntVisit':             { tasks: 5, sparePartUsages: 5 },              // 每次访问1-10任务/备件
    'ErpMntMaintenanceTeam':   { members: 10 },                                // 5-20人
    'ErpMntSparePartUsage':    { lines: 10 },

    // === 物流 ===
    'ErpLogCarrier':           { configs: 5 },                                 // 承运商配置3-10
    'ErpLogShipment':          { lines: 20, parcels: 5, logs: 20 },           // 运单行/包裹/追踪事件

    // === B2B ===
    'ErpB2bAsn':               { lines: 30 },

    // === DRP ===
    'ErpDrpPlan':              { lines: 100 },                                 // 分销计划行50-500

    // === CS ===
    'ErpCsTicket':             { actions: 20 },                                // 工单操作5-30

    // === HR extra ===
    'ErpMfgBomOperation':      {},  // 没有直接的to-many
};

// 通用回落规则
function estimateRows(relName, refEntityShortName, entityShortName) {
    // 精确匹配优先
    if (ENTITY_REL_MAP[entityShortName]?.[relName] !== undefined) {
        return ENTITY_REL_MAP[entityShortName][relName];
    }

    // 关系名模式匹配
    const lower = relName.toLowerCase();
    if (lower === 'lines') return 30;
    if (lower === 'items') return 20;
    if (lower === 'children' || lower === 'childtasks') return 10;
    if (lower === 'jobcards') return 5;
    if (lower === 'timesheets') return 20;
    if (lower === 'responses') return 50;
    if (lower === 'answers') return 20;
    if (lower === 'questions') return 20;
    if (lower === 'budgets') return 10;
    if (lower === 'versions') return 10;

    // 实体名后缀模式
    const refLower = refEntityShortName.toLowerCase();
    if (refLower.includes('ledger')) return 2000;
    if (refLower.includes('costlayer')) return 500;
    if (refLower.includes('log') && !lower.includes('dialog')) return 20;
    if (refLower.includes('history')) return 500;
    if (refLower.includes('approval')) return 20;
    if (refLower.includes('action')) return 20;

    // 关系名启发式
    if (lower.includes('action') || lower.includes('log') || lower.includes('history')) return 20;
    if (lower.includes('version')) return 10;
    if (lower.includes('budget')) return 10;
    if (lower.includes('batch') || lower.includes('serial')) return 2000;
    if (lower.includes('cost')) return 100;
    if (lower.includes('milestone')) return 10;
    if (lower.includes('member')) return 20;
    if (lower.includes('schedule')) return 10;
    if (lower.includes('parcel')) return 5;
    if (lower.includes('config')) return 5;
    if (lower.includes('convlog')) return 50;
    if (lower.includes('quotation') && entityShortName.includes('Rfq')) return 10;
    if (lower.includes('level')) return 5;

    return 30; // 默认
}

function parseShortName(refEntityName) {
    const parts = refEntityName.split('.');
    return parts[parts.length - 1];
}

function collectOrmFiles(rootDir = '.') {
    const files = [];
    const entries = readdirSync(rootDir, { withFileTypes: true });
    for (const entry of entries) {
        if (!entry.isDirectory() || !entry.name.startsWith('module-')) continue;
        const modelDir = join(rootDir, entry.name, 'model');
        try {
            if (!statSync(modelDir).isDirectory()) continue;
            for (const f of readdirSync(modelDir).filter(f => f.endsWith('.orm.xml'))) {
                files.push(join(modelDir, f));
            }
        } catch { /* skip */ }
    }
    return files;
}

const files = collectOrmFiles();
let total = 0;
let updated = 0;

for (const file of files) {
    let content = readFileSync(file, 'utf-8');
    const original = content;

    const regex = /<to-many\s+([^>]*?)\/?\s*>/g;
    let match;
    let currentEntity = '';

    // 预处理：找出每个 to-many 所属的 entity
    const entityRegex = /<entity\s+[^>]*?name\s*=\s*["']([^"']+)["']/g;
    const entityMap = new Map();
    let em;
    while ((em = entityRegex.exec(content)) !== null) {
        entityMap.set(em.index, em[1]);
    }

    // 收集所有匹配及对应 entity
    const matches = [];
    while ((match = regex.exec(content)) !== null) {
        // 找到最近的前置 entity 声明
        let nearestEntity = '';
        let nearestPos = -1;
        for (const [pos, name] of entityMap) {
            if (pos < match.index && pos > nearestPos) {
                nearestPos = pos;
                nearestEntity = name;
            }
        }
        matches.push({
            index: match.index,
            fullTag: match[0],
            attrs: match[1],
            entityShort: nearestEntity ? parseShortName(nearestEntity) : '',
        });
    }

    // 从后往前替换，避免索引偏移
    for (let i = matches.length - 1; i >= 0; i--) {
        const m = matches[i];
        const attrs = m.attrs;
        if (!FORCE && /ext:estRows\s*=/.test(attrs)) continue;

        const nameMatch = attrs.match(/\bname\s*=\s*["']([^"']+)["']/);
        const refMatch = attrs.match(/\brefEntityName\s*=\s*["']([^"']+)["']/);
        if (!nameMatch) continue;

        const relName = nameMatch[1];
        const shortName = refMatch ? parseShortName(refMatch[1]) : '';
        const estRows = estimateRows(relName, shortName, m.entityShort);

        let newTag;
        if (/ext:estRows\s*=/.test(attrs)) {
            newTag = m.fullTag.replace(/ext:estRows\s*=\s*["']\d+["']/, `ext:estRows="${estRows}"`);
        } else {
            newTag = m.fullTag.replace('>', ` ext:estRows="${estRows}">`);
        }
        content = content.slice(0, m.index) + newTag + content.slice(m.index + m.fullTag.length);
        total++;
    }

    if (content !== original) {
        writeFileSync(file, content, 'utf-8');
        updated++;
        console.log(`✓ ${file}: updated`);
    }
}

console.log(`\nDone: ${total} to-many processed across ${updated} files`);
