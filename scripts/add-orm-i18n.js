#!/usr/bin/env node
/**
 * Add missing i18n-en:displayName to ORM model files.
 *
 * Usage:
 *   node scripts/add-orm-i18n.js                    # fix all modules
 *   node scripts/add-orm-i18n.js module-purchase     # fix one module
 *
 * This modifies the ORM XML files in-place. Backups are NOT created.
 * Run `git diff` to review before committing.
 */

const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');

const STD_COL = {
  id: 'ID',
  delVersion: 'Del Flag',
  version: 'Version',
  createdBy: 'Created By',
  createTime: 'Create Time',
  updatedBy: 'Updated By',
  updateTime: 'Update Time',
  remark: 'Remark',
  code: 'Code',
  name: 'Name',
  sortNum: 'Sort Order',
  lineNo: 'Line No',
  quantity: 'Quantity',
  unitPrice: 'Unit Price',
  taxRate: 'Tax Rate',
  taxRateId: 'Tax Rate',
  taxAmount: 'Tax Amount',
  amount: 'Amount',
  amountSource: 'Amt (Source)',
  amountFunctional: 'Amt (Functional)',
  totalAmount: 'Total Amt',
  totalTaxAmount: 'Total Tax',
  totalAmountWithTax: 'Total (Inc. Tax)',
  discountRate: 'Discount %',
  discountAmount: 'Discount Amt',
  exchangeRate: 'Exchange Rate',
  currencyId: 'Currency',
  orgId: 'Organization',
  uoMId: 'UoM',
  materialId: 'Material',
  warehouseId: 'Warehouse',
  locationId: 'Location',
  partnerId: 'Partner',
  customerId: 'Customer',
  supplierId: 'Supplier',
  employeeId: 'Employee',
  departmentId: 'Department',
  categoryId: 'Category',
  parentId: 'Parent',
  status: 'Status',
  docStatus: 'Doc Status',
  approveStatus: 'Approve Status',
  approvedBy: 'Approved By',
  approvedAt: 'Approved At',
  posted: 'Posted',
  postedBy: 'Posted By',
  postedAt: 'Posted At',
  businessDate: 'Business Date',
  projectId: 'Project',
  taskId: 'Task',
  settlementMethodId: 'Settlement Method',
  bankAccountId: 'Bank Account',
  batchNo: 'Batch No',
  invoiceNo: 'Invoice No',
  contractId: 'Contract',
  quotationId: 'Quotation',
  orderId: 'Order',
  deliveryId: 'Delivery',
  templateId: 'Template',
  isActive: 'Active',
  isBase: 'Is Base',
  isRequired: 'Is Required',
  description: 'Description',
  priceListId: 'Price List',
  Schema: 'Schema',
  skuId: 'SKU',
  brandModel: 'Brand / Model',
  invoiceType: 'Invoice Type',
  receiveType: 'Receive Type',
  byproductType: 'Byproduct Type',
  dispositionType: 'Disposition Type',
  visitType: 'Visit Type',
  visitResult: 'Visit Result',
  disposalReason: 'Disposal Reason',
  paidAmount: 'Paid Amount',
  paidStatus: 'Paid Status',
  settlementStatus: 'Settlement Status',
};

function englishColName(colName, chinese) {
  if (STD_COL[colName]) return STD_COL[colName];
  if (colName.endsWith('Id') && colName.length > 3) {
    const base = colName.slice(0, -2);
    if (STD_COL[base]) return STD_COL[base];
    return base.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase()).trim();
  }
  const split = colName.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase()).trim();
  return split || chinese || colName;
}

function englishRelName(relName) {
  return relName.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase()).trim();
}

function getEntityEnName(entityBlock) {
  const m = entityBlock.match(/i18n-en:displayName\s*=\s*["']([^"']+)["']/);
  if (m) return m[1];
  const nameMatch = entityBlock.match(/\bname=["']([^"']+)["']/);
  if (!nameMatch) return '';
  return nameMatch[1].split('.').pop().replace(/^Erp/, '').replace(/([A-Z])/g, ' $1').trim();
}

function esc(val) {
  return val.includes("'") ? `"${val}"` : `'${val}'`;
}

function fixOrmFile(filePath) {
  let xml = fs.readFileSync(filePath, 'utf-8');
  const entityRe = /(<entity\s+[\s\S]*?<\/entity>)/g;

  xml = xml.replace(entityRe, (entityBlock) => {
    const entityEn = getEntityEnName(entityBlock);
    if (!entityEn) return entityBlock;

    // --- Fix columns (self-closing <column ... />) ---
    entityBlock = entityBlock.replace(/(<column[\s\S]*?)\/>/g, (colTag, prefix) => {
      if (colTag.includes('i18n-en:displayName')) return colTag;
      const colName = colTag.match(/\bname=["']([^"']+)["']/);
      if (!colName) return colTag;
      const cnMatch = colTag.match(/\bdisplayName=["']([^"']*)["']/);
      const chinese = cnMatch ? cnMatch[1] : '';
      const en = englishColName(colName[1], chinese);
      return `${prefix} i18n-en:displayName=${esc(en)} />`;
    });

    // --- Fix relations: self-closing <to-one ... /> / <to-many ... /> ---
    entityBlock = entityBlock.replace(
      /(<(to-one|to-many)([^>]*?))\s*\/>/g,
      (match, prefix) => {
        if (match.includes('i18n-en:displayName')) return match;
        const relName = match.match(/\bname=["']([^"']+)["']/);
        if (!relName) return match;
        const cnMatch = match.match(/\bdisplayName=["']([^"']*)["']/);
        const chinese = cnMatch ? cnMatch[1] : '';
        const en = chinese || englishRelName(relName[1]);
        let result = prefix + ` i18n-en:displayName=${esc(en)}`;
        if (!match.includes('ref-i18n-en:displayName')) {
          result += ` ref-i18n-en:displayName=${esc(entityEn)}`;
        }
        return result + ' />';
      }
    );

    // --- Fix relations: with children <to-one ...>...</to-one> ---
    entityBlock = entityBlock.replace(
      /(<(to-one|to-many)([^>]*?))>([\s\S]*?)<\/\2>/g,
      (match, prefix) => {
        if (match.includes('i18n-en:displayName')) return match;
        const relName = match.match(/\bname=["']([^"']+)["']/);
        if (!relName) return match;
        const tagName = match.match(/^<(\S+)/)[1];
        const bodyEnd = match.indexOf('</' + tagName + '>');
        const body = match.slice(match.indexOf('>', prefix.length) + 1, bodyEnd);
        const cnMatch = match.match(/\bdisplayName=["']([^"']*)["']/);
        const chinese = cnMatch ? cnMatch[1] : '';
        const en = chinese || englishRelName(relName[1]);
        let result = prefix + ` i18n-en:displayName=${esc(en)}`;
        if (!match.includes('ref-i18n-en:displayName')) {
          result += ` ref-i18n-en:displayName=${esc(entityEn)}`;
        }
        return result + '>' + body + '</' + tagName + '>';
      }
    );

    return entityBlock;
  });

  fs.writeFileSync(filePath, xml, 'utf-8');
  return true;
}

function main() {
  const filterModule = process.argv[2];
  const entries = fs.readdirSync(ROOT).filter(
    e => e.startsWith('module-') && fs.statSync(path.join(ROOT, e)).isDirectory()
  );

  for (const entry of entries) {
    const modName = entry.replace('module-', '');
    if (filterModule && entry !== filterModule && modName !== filterModule) continue;
    const modelDir = path.join(ROOT, entry, 'model');
    if (!fs.existsSync(modelDir)) continue;

    for (const f of fs.readdirSync(modelDir).filter(f => f.endsWith('.orm.xml'))) {
      const fp = path.join(modelDir, f);
      console.log(`Fixing: ${entry}/${f} ...`);
      fixOrmFile(fp);
    }
  }
  console.log('\nDone. Verify with: node scripts/check-orm-auth-i18n.js');
}

main();
