#!/usr/bin/env node
/**
 * Generate dict i18n entries from .dict.yaml files.
 *
 * Usage:
 *   node scripts/add-dict-i18n.js                    # add to all modules
 *   node scripts/add-dict-i18n.js module-assets       # one module
 *
 * Writes to module-{name}/erp-{short}-meta/src/main/resources/_vfs/i18n/en/erp-{short}.i18n.yaml
 * Existing non-dict entries (entity/prop labels) are preserved.
 * Backups are NOT created. Run `git diff` to review.
 */

const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

const SHORT = {
  assets: 'ast', finance: 'fin', inventory: 'inv', maintenance: 'mnt',
  manufacturing: 'mfg', 'master-data': 'md', projects: 'prj',
  purchase: 'pur', quality: 'qa', sales: 'sal',
};

const TRANS = {
  // common dict labels
  '审核状态': 'Approve Status',
  '单据状态': 'Doc Status',
  '资产状态': 'Asset Status',
  '资产类别': 'Asset Category',
  '折旧方法': 'Depreciation Method',
  '价值调整类型': 'Adjustment Type',
  '处置类型': 'Disposal Type',
  '处置原因': 'Disposal Reason',
  '折旧计划状态': 'Depreciation Schedule Status',
  '资本化来源类型': 'Capitalization Source Type',
  '期间状态': 'Period Status',
  '账户类型': 'Account Type',
  '账户状态': 'Account Status',
  '借贷方向': 'Debit/Credit Direction',
  '凭证字': 'Voucher Type',
  '凭证状态': 'Voucher Status',
  'AR/AP 方向': 'AR/AP Direction',
  'AR/AP 状态': 'AR/AP Status',
  '业务类型': 'Business Type',
  '核销单状态': 'Reconciliation Status',
  '模块结账状态': 'Module Close Status',
  '拣货状态': 'Picking Status',
  '盘点类型': 'Take Type',
  '序列号状态': 'Serial Status',
  '批次状态': 'Batch Status',
  '出入库类型': 'Operation Type',
  '预约状态': 'Reservation Status',
  '移库状态': 'Move Status',
  '移库方向': 'Move Direction',
  '设备状态': 'Equipment Status',
  '维修申请状态': 'Request Status',
  '维修类型': 'Visit Type',
  '维修结果': 'Visit Result',
  '维修任务状态': 'Visit Task Status',
  '优先级': 'Priority',
  '排程类型': 'Schedule Type',
  '重复类型': 'Recurrence Type',
  '校准结果': 'Calibration Result',
  '工单状态': 'Work Order Status',
  'BOM 类型': 'BOM Type',
  '副产物类型': 'Byproduct Type',
  '用料明细状态': 'Consumption Status',
  'MRP 状态': 'MRP Status',
  'MRP 需求来源': 'MRP Demand Source',
  'MRP 单据类型': 'MRP Order Type',
  '发料状态': 'Issue Status',
  '委外状态': 'Subcontract Status',
  '成本滚算状态': 'Cost Rollup Status',
  '项目状态': 'Project Status',
  '任务状态': 'Task Status',
  '工时状态': 'Timesheet Status',
  '组织类型': 'Org Type',
  '物料类型': 'Material Type',
  '往来单位类型': 'Partner Type',
  '仓库类型': 'Warehouse Type',
  '结算类型': 'Settlement Type',
  '银行账户类型': 'Bank Account Type',
  '地址类型': 'Address Type',
  '科目方向': 'Subject Direction',
  '科目类别': 'Subject Class',
  '科目体系性质': 'Acct Schema Nature',
  '余额方向': 'Balance Type',
  '成本计价方法': 'Cost Method',
  '价格校验方式': 'Price Validation',
  '税率类型': 'Tax Type',
  '状态': 'Status',
  '活跃状态': 'Active Status',
  '采购发票类型': 'Invoice Type',
  '采购业务类型': 'Biz Type',
  '采购方向': 'Doc Direction',
  '到货状态': 'Receive Status',
  '验收状态': 'Receive Status',
  '付款状态': 'Paid Status',
  '到货类型': 'Receive Type',
  '质检结果': 'Inspection Result',
  '严重程度': 'Severity',
  '处理决定': 'Disposition Type',
  '整改措施类型': 'Action Type',
  '措施状态': 'Action Status',
  '评审类型': 'Review Type',
  '风险状态': 'Risk Status',
  '检验类型': 'Inspection Type',
  'NCR 状态': 'NCR Status',
  '目标状态': 'Goal Status',
  '接收类型': 'Receive Type',
  '发货进度': 'Delivery Status',
  '收款进度': 'Received Status',
  '销售发票类型': 'Invoice Type',
  '销售业务类型': 'Biz Type',
  // option labels
  '草稿': 'Draft',
  '未提交': 'Unsubmitted',
  '已提交': 'Submitted',
  '已审核': 'Approved',
  '已驳回': 'Rejected',
  '已作废': 'Voided',
  '已生效': 'Effective',
  '使用中': 'In Use',
  '闲置': 'Idle',
  '已报废': 'Scrapped',
  '已出售': 'Sold',
  '待执行': 'Pending',
  '已执行': 'Executed',
  '已冲销': 'Reversed',
  '启用': 'Enabled',
  '停用': 'Disabled',
  '未结': 'Unsettled',
  '结账中': 'Closing',
  '已结': 'Settled',
  '未开启': 'Not Open',
  '开启': 'Open',
  '已结账': 'Closed',
  '未核销': 'Unmatched',
  '部分核销': 'Partially Matched',
  '已核销': 'Matched',
  '已过账': 'Posted',
  '已确认': 'Confirmed',
  '已取消': 'Cancelled',
  '是': 'Yes',
  '否': 'No',
  '正常': 'Normal',
  '异常': 'Abnormal',
  '合格': 'Pass',
  '不合格': 'Fail',
  '待检': 'Pending Inspection',
  '检验中': 'Inspecting',
  '高': 'High',
  '中': 'Medium',
  '低': 'Low',
  '报废': 'Scrap',
  '出售': 'Sale',
  '捐赠': 'Donation',
  '被盗': 'Stolen',
  '其他': 'Other',
  '借': 'Debit',
  '贷': 'Credit',
  '不合格': 'Non-conforming',
  '合格': 'Conforming',
  '让步接收': 'Waiver Accept',
  '调整后合格': 'Adjusted Conforming',
  '警告放行': 'Warning Release',
  '降级使用': 'Downgrade Use',
  '来料检验': 'Incoming Inspection',
  '出货检验': 'Outgoing Inspection',
  '制程检验': 'In-Process Inspection',
  '完工检验': 'Final Inspection',
  '待检': 'Pending Inspection',
  '检验中': 'Inspecting',
  '已拒绝': 'Rejected',
  '已解决': 'Resolved',
  '已发布': 'Published',
  '已确认': 'Confirmed',
  '已受理': 'Accepted',
  '已过期': 'Expired',
  '已逾期': 'Overdue',
  '已取消': 'Cancelled',
  '已红冲': 'Red Offset',
  '已关闭': 'Closed',
  '已耗尽': 'Depleted',
  '已全部消耗': 'Fully Consumed',
  '已确认(转工单)': 'Confirmed (To W/O)',
  '已齐套': 'Kitted',
  '已转序': 'Transferred',
  '已计算': 'Calculated',
  '已跳过': 'Skipped',
  '已达成': 'Achieved',
  '已缓解': 'Mitigated',
  '已开工': 'Started',
  '已停工': 'Stopped',
  '已拣货': 'Picked',
  '已排程': 'Scheduled',
  '已出库': 'Delivered',
  '已收清': 'Fully Received',
  '已发清': 'Fully Shipped',
  '已全部收到': 'Fully Received',
  '未发货': 'Unshipped',
  '未收款': 'Unreceived',
  '未收货': 'Unreceived',
  '未付款': 'Unpaid',
  '未开始': 'Not Started',
  '未达成': 'Not Achieved',
  '待处理': 'Pending',
  '待受理': 'Pending Accept',
  '待拣货': 'Pending Pick',
  '待开始': 'Pending Start',
  '运行中': 'Running',
  '生产中': 'In Production',
  '作业中': 'Working',
  '维修中': 'Under Repair',
  '维护中': 'Under Maintenance',
  '执行中': 'Executing',
  '生效中': 'Active',
  '评审中': 'Reviewing',
  '拣货中': 'Picking',
  '运算中': 'Computing',
  '运算完成': 'Computed',
  '进行中': 'In Progress',
  '部分完成': 'Partially Complete',
  '部分发货': 'Partially Shipped',
  '部分收款': 'Partially Received',
  '部分收货': 'Partially Received',
  '部分付款': 'Partially Paid',
  '部分转序': 'Partially Transferred',
  '部分消耗': 'Partially Consumed',
  '部分齐套': 'Partially Kitted',
  '完成': 'Complete',
  '正常': 'Normal',
  '异常': 'Abnormal',
  '启用': 'Enabled',
  '停用': 'Disabled',
  '冻结': 'Frozen',
  '锁定': 'Locked',
  '已停用': 'Disabled',
  '暂停': 'Suspended',
  '在库': 'In Stock',
  '在用': 'In Use',
  '在途仓': 'In Transit',
  '普通仓': 'Normal Warehouse',
  '保税仓': 'Bonded Warehouse',
  '报废仓': 'Scrap Warehouse',
  '受托代销仓': 'Consignment In',
  '委托代销仓': 'Consignment Out',
  '安全库存': 'Safety Stock',
  '质量冻结': 'Quality Frozen',
  '入库': 'Receipt',
  '出库': 'Issue',
  '退货入库': 'Return Receipt',
  '退货': 'Return',
  '采购入库': 'Purchase Receipt',
  '销售出库': 'Sales Issue',
  '内部调拨': 'Internal Transfer',
  '正常入库': 'Normal Receipt',
  '赠品入库': 'Freebie Receipt',
  '报价单': 'Quotation',
  '采购订单': 'Purchase Order',
  '采购发票': 'Purchase Invoice',
  '采购退货': 'Purchase Return',
  '销售订单': 'Sales Order',
  '销售发票': 'Sales Invoice',
  '销售退货': 'Sales Return',
  '销售报价': 'Sales Quotation',
  '销售合同': 'Sales Contract',
  '询价单': 'RFQ',
  '请购单': 'Purchase Requisition',
  '付款单': 'Payment',
  '收款单': 'Receipt',
  '收据': 'Receipt Note',
  '应付发票': 'AP Invoice',
  '应收发票': 'AR Invoice',
  '成本': 'Cost',
  '标准成本': 'Standard Cost',
  '移动加权平均': 'Moving Weighted Avg',
  '全月一次加权平均': 'Monthly Weighted Avg',
  '先进先出': 'FIFO',
  '后进先出': 'LIFO',
  '具体辨认(个别计价)': 'Specific Identification',
  '资产': 'Asset',
  '负债': 'Liability',
  '权益': 'Equity',
  '收入': 'Revenue',
  '费用': 'Expense',
  '借方': 'Debit Side',
  '贷方': 'Credit Side',
  '借余': 'Debit Balance',
  '贷余': 'Credit Balance',
  '借贷双余': 'Debit/Credit Both',
  '固定资产': 'Fixed Asset',
  '折旧': 'Depreciation',
  '资本化': 'Capitalization',
  '处置': 'Disposal',
  '重估': 'Revaluation',
  '减值': 'Impairment',
  '汇兑损益': 'FX Gain/Loss',
  '生产成本结转': 'Prod Cost Transfer',
  '项目成本归集': 'Project Cost Accrual',
  '期末结转': 'Period-End Closing',
  '供应商': 'Supplier',
  '客户': 'Customer',
  '客户且供应商': 'Customer & Supplier',
  '员工(内部往来)': 'Employee (Internal)',
  '部门': 'Department',
  '公司': 'Company',
  '分公司': 'Branch',
  '集团': 'Group',
  '门店': 'Store',
  '车间': 'Workshop',
  '供应商评审': 'Supplier Review',
  '外部评审': 'External Review',
  '内部评审': 'Internal Review',
  '矫正': 'Corrective',
  '纠正预防': 'Corrective & Preventive',
  '预防': 'Preventive',
  '紧急': 'Urgent',
  '高': 'High',
  '低': 'Low',
  '严重': 'Critical',
  '响应性': 'Responsive',
  '定期': 'Periodic',
  '预防性维护': 'Preventive Maintenance',
  '预测性维护': 'Predictive Maintenance',
  '计划性': 'Planned',
  '工单建议': 'Work Order Suggestion',
  '委外建议': 'Outsource Suggestion',
  '请购建议': 'Purchase Req Suggestion',
  '计划订单': 'Planned Order',
  '日常': 'Daily',
  '现金': 'Cash',
  '银行': 'Bank',
  '银行转账': 'Bank Transfer',
  '在线支付': 'Online Payment',
  '支付宝': 'Alipay',
  '微信': 'WeChat',
  '支票': 'Check',
  '票据': 'Note',
  '信用账户': 'Credit Account',
  '活期': 'Current Account',
  '增值税': 'VAT',
  '销售税': 'Sales Tax',
  '增值税专用': 'VAT Special',
  '增值税普通': 'VAT General',
  '关税': 'Customs Duty',
  '预提税': 'Withholding Tax',
  '抵扣': 'Deductible',
  '税务账': 'Tax Book',
  '财务账(法定账)': 'Financial Book',
  '管理账(内部账)': 'Management Book',
  '预算账': 'Budget Book',
  '合并账': 'Consolidated Book',
  '一般': 'General',
  '严格按 BOM': 'Strict BOM',
  '允许超耗': 'Allow Overconsumption',
  '超耗警告': 'Overconsumption Warning',
  '强制拦截': 'Force Block',
  '不校验': 'No Check',
  '虚拟件/Kit': 'Phantom/Kit',
  '联产品': 'Co-product',
  '副产品': 'Byproduct',
  '产成品': 'Finished Good',
  '半成品': 'Semi-Finished',
  '原材料': 'Raw Material',
  '包装物': 'Packaging',
  '消耗品': 'Consumable',
  '商品': 'Merchandise',
  '制造': 'Manufactured',
  '服务': 'Service',
  '批次': 'Batch',
  '收货地址': 'Shipping Address',
  '账单地址': 'Billing Address',
  '发货地址': 'Delivery Address',
  '注册地址': 'Registered Address',
  '故障停机': 'Breakdown',
  '校准': 'Calibration',
  '全面盘点': 'Full Count',
  '循环盘点': 'Cycle Count',
  '抽样盘点': 'Sample Count',
  '收': 'Receive',
  '付': 'Pay',
  '转': 'Transfer',
};

function esc(val) {
  if (val == null) return 'null';
  return val.includes("'") ? `"${val}"` : `'${val}'`;
}

function translate(label) {
  label = label.trim();
  if (TRANS[label]) return TRANS[label];

  // remove parenthetical suffixes like "(值)" or "(value)"
  const parenMatch = label.match(/^(.+?)\s*[（(].+?[）)]$/);
  if (parenMatch && TRANS[parenMatch[1]]) return TRANS[parenMatch[1]];

  // try splitting on common delimiters
  const parts = label.split(/[（(·]/).filter(Boolean);
  const translated = parts.map(p => TRANS[p.trim()] || p.trim()).join(' (');
  const closeParen = parts.length > 1 ? ')' : '';
  return translated + closeParen;
}

function generateDictI18n(modDir) {
  // find meta dir
  const entries = fs.readdirSync(modDir);
  const metaDir = entries.find(e => e.endsWith('-meta') && fs.statSync(path.join(modDir, e)).isDirectory());
  if (!metaDir) return false;

  const modName = path.basename(modDir).replace('module-', '');
  const short = SHORT[modName];
  if (!short) {
    console.error(`  Unknown short name for module: ${modName}`);
    return false;
  }

  const dictDir = path.join(modDir, metaDir, 'src/main/resources/_vfs/dict', `erp-${short}`);
  if (!fs.existsSync(dictDir)) return false;

  const i18nDir = path.join(modDir, metaDir, 'src/main/resources/_vfs/i18n/en');
  if (!fs.existsSync(i18nDir)) {
    fs.mkdirSync(i18nDir, { recursive: true });
  }

  // parse all dict yamls
  const dictEntries = {};
  const optionEntries = {};

  for (const f of fs.readdirSync(dictDir).filter(f => f.endsWith('.dict.yaml'))) {
    const dictName = `erp-${short}/${f.replace('.dict.yaml', '')}`;
    const content = fs.readFileSync(path.join(dictDir, f), 'utf-8');

    const labelMatch = content.match(/^label:\s*(.+)/m);
    if (labelMatch) {
      dictEntries[dictName] = translate(labelMatch[1]);
    }

    const optRe = /^\s+-\s+label:\s*(.*?)\s*$/gm;
    let m;
    while ((m = optRe.exec(content)) !== null) {
      const optLabel = m[1].trim();
      const rest = content.slice(m.index + m[0].length);
      const valMatch = rest.match(/^\s+value:\s*(.+)$/m);
      if (valMatch) {
        const val = valMatch[1].trim();
        if (!optionEntries[dictName]) optionEntries[dictName] = {};
        optionEntries[dictName][val] = translate(optLabel);
      }
    }
  }

  if (Object.keys(dictEntries).length === 0) return false;

  // read existing i18n override file
  const i18nFile = path.join(i18nDir, `erp-${short}.i18n.yaml`);
  let existing = '';
  let hasExtends = false;
  let existingBody = '';
  if (fs.existsSync(i18nFile)) {
    existing = fs.readFileSync(i18nFile, 'utf-8');
    // extract x:extends line and existing body
    const extMatch = existing.match(/^"?x:extends"?:\s*\S+/m);
    if (extMatch) {
      hasExtends = true;
      existingBody = existing.slice(extMatch.index + extMatch[0].length).trim();
    }
  }

  // build new content
  const lines = [];
  if (hasExtends) {
    const extMatch = existing.match(/^"?x:extends"?:\s*\S+/m);
    lines.push(existing.match(/^"?x:extends"?:\s*\S+/m)[0]);
  } else {
    lines.push('"x:extends": _erp-' + short + '.i18n.yaml');
  }
  lines.push('');

  // dict labels
  lines.push('dict:');
  lines.push('  label:');
  const sortedDicts = Object.keys(dictEntries).sort();
  for (const d of sortedDicts) {
    lines.push(`    ${d}: ${esc(dictEntries[d])}`);
  }
  lines.push('');

  // dict option labels (as nested dict.option.label key)
  lines.push('dict.option.label:');
  const sortedOptDicts = Object.keys(optionEntries).sort();
  for (const d of sortedOptDicts) {
    lines.push(`  ${d}:`);
    const sortedOpts = Object.keys(optionEntries[d]).sort((a, b) => parseInt(a) - parseInt(b));
    for (const v of sortedOpts) {
      lines.push(`    ${v}: ${esc(optionEntries[d][v])}`);
    }
  }
  lines.push('');

  // preserve existing non-dict entries (entity/prop labels)
  if (existingBody) {
    // remove any existing dict.* entries from existingBody
    const cleaned = existingBody
      .split('\n')
      .filter(line => !line.trim().startsWith('dict') && !line.trim().match(/^erp-/))
      .join('\n')
      .trim();
    if (cleaned) {
      lines.push(cleaned);
    }
  }

  fs.writeFileSync(i18nFile, lines.join('\n'), 'utf-8');
  console.log(`  Wrote ${Object.keys(dictEntries).length} dict labels + ${Object.values(optionEntries).flatMap(Object.keys).length} option labels to ${modDir.replace(ROOT + '/', '')}/${metaDir}/.../i18n/en/erp-${short}.i18n.yaml`);
  return true;
}

function main() {
  const filterModule = process.argv[2];
  const entries = fs.readdirSync(ROOT).filter(e => e.startsWith('module-') && fs.statSync(path.join(ROOT, e)).isDirectory());

  for (const entry of entries) {
    const modName = entry.replace('module-', '');
    if (filterModule && entry !== filterModule && modName !== filterModule) continue;
    const modDir = path.join(ROOT, entry);
    console.log(`Processing ${entry} ...`);
    generateDictI18n(modDir);
  }
  console.log('\nDone. Verify with: node scripts/check-orm-auth-i18n.js');
}

main();
