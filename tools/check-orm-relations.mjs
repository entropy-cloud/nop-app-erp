#!/usr/bin/env node
/**
 * ORM 关联完整性审计工具
 *
 * 扫描所有 module-xxx/model 目录下的 .orm.xml，对每个实体：
 * 1. 找到所有以 Id 为后缀的外键列（如 orderId, supplierId）
 * 2. 检查是否存在对应的 <to-one> 关联定义
 * 3. 检查所有 <to-many> 是否有反向 <to-one>
 * 4. 检查 <to-many> 是否标注了 ext:estRows 扩展属性
 * 5. 报告缺失或可疑的关联
 *
 * 用法: node check-orm-relations.mjs [orm-file-glob]
 * 默认: module-xxx/model 下所有 .orm.xml 文件
 *
 * 输出: 按严重性排序的问题列表 + 统计摘要
 */

import { readFileSync, readdirSync, statSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');
const MAX_COLLECTION_ROWS = 3000;

// --- XML 解析（轻量级，不依赖外部包）---

function parseXML(filePath) {
  const text = readFileSync(filePath, 'utf-8');
  const entities = [];
  
  // 匹配 <entity ...>...</entity> 块
  const entityRegex = /<entity\s+([^>]*?)(\/>|>([\s\S]*?)<\/entity>)/g;
  let entityMatch;
  while ((entityMatch = entityRegex.exec(text)) !== null) {
    const entityAttrs = entityMatch[1];
    const entityBody = entityMatch[3] || '';
    
    // 解析 entity 属性
    const name = extractAttr(entityAttrs, 'name') || extractAttr(entityAttrs, 'className');
    const className = extractAttr(entityAttrs, 'className');
    const displayName = extractAttr(entityAttrs, 'displayName');
    const tableName = extractAttr(entityAttrs, 'tableName');
    
    if (!className) continue;
    
    // 短名（ErpPurOrder）
    const shortName = className.split('.').pop();
    
    // 解析 columns
    const columns = [];
    const colRegex = /<column\s+([^>]*?)(\/|><\/column>)/g;
    let colMatch;
    while ((colMatch = colRegex.exec(entityBody)) !== null) {
      const colAttrs = colMatch[1];
      const colName = extractAttr(colAttrs, 'name');
      if (colName) {
        columns.push({
          name: colName,
          displayName: extractAttr(colAttrs, 'displayName'),
          stdDataType: extractAttr(colAttrs, 'stdDataType'),
          primary: extractAttr(colAttrs, 'primary') === 'true',
        });
      }
    }
    
    // 解析 relations
    const toOneRels = [];
    const toManyRels = [];
    
    const toOneRegex = /<to-one\s+([^>]*?)(?:\/>|>[\s\S]*?<\/to-one>)/g;
    let toOneMatch;
    while ((toOneMatch = toOneRegex.exec(entityBody)) !== null) {
      const fullMatch = toOneMatch[0];
      const relAttrs = toOneMatch[1];
      const joinMatch = fullMatch.match(/<on\s+leftProp="([^"]*)"\s+rightProp="([^"]*)"\s*\/>/);
      toOneRels.push({
        name: extractAttr(relAttrs, 'name'),
        refEntityName: extractAttr(relAttrs, 'refEntityName'),
        join: joinMatch ? { leftProp: joinMatch[1], rightProp: joinMatch[2] } : null,
      });
    }
    
    const toManyRegex = /<to-many\s+([^>]*?)(?:\/>|>[\s\S]*?<\/to-many>)/g;
    let toManyMatch;
    while ((toManyMatch = toManyRegex.exec(entityBody)) !== null) {
      const fullMatch = toManyMatch[0];
      const relAttrs = toManyMatch[1];
      const joinMatch = fullMatch.match(/<on\s+leftProp="([^"]*)"\s+rightProp="([^"]*)"\s*\/>/);
      toManyRels.push({
        name: extractAttr(relAttrs, 'name'),
        refEntityName: extractAttr(relAttrs, 'refEntityName'),
        estRows: extractAttr(relAttrs, 'ext:estRows'),
        estRowsReason: extractAttr(relAttrs, 'ext:estRowsReason'),
        tagSet: extractAttr(relAttrs, 'tagSet'),
        join: joinMatch ? { leftProp: joinMatch[1], rightProp: joinMatch[2] } : null,
      });
    }
    
    entities.push({
      className,
      shortName,
      displayName,
      tableName,
      columns,
      toOneRels,
      toManyRels,
    });
  }
  
  return entities;
}

function extractAttr(attrsStr, attrName) {
  // 匹配 attrName="value" 或 attrName='value'
  const regex = new RegExp(`${escapeRegex(attrName)}\\s*=\\s*"([^"]*)"`, 'i');
  const match = attrsStr.match(regex);
  if (match) return match[1];
  const regex2 = new RegExp(`${escapeRegex(attrName)}\\s*=\\s*'([^']*)'`, 'i');
  const match2 = attrsStr.match(regex2);
  return match2 ? match2[1] : null;
}

function escapeRegex(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function extractJoinInfo(entityBody, relStartPos) {
  // join 信息在 to-one/to-many 标签内部的 <join><on .../></join> 中
  // entityBody 是整个 entity body，relStartPos 是 to-one/to-many 开始标签结束后的位置
  const onRegex = /<on\s+leftProp="([^"]*)"\s+rightProp="([^"]*)"\s*\/>/g;
  const match = onRegex.exec(entityBody);
  if (match) return { leftProp: match[1], rightProp: match[2] };
  return null;
}

// --- 审计逻辑 ---

function auditEntities(entities, filePath) {
  const issues = [];
  const moduleName = path.basename(filePath).replace('app-erp-', '').replace('.orm.xml', '');
  
  // 构建 className → entity 的索引
  const entityByClassName = {};
  for (const e of entities) {
    entityByClassName[e.className] = e;
  }
  
  // 构建 shortName → entity 的索引（用于跨文件引用解析）
  const entityByShortName = {};
  for (const e of entities) {
    entityByShortName[e.shortName] = e;
  }
  
  for (const entity of entities) {
    // === 1. 检查 _id 后缀外键列是否有 to-one 关联 ===
    const existingToOneNames = new Set(entity.toOneRels.map(r => r.name));
    
    for (const col of entity.columns) {
      // 跳过主键 id、公共字段（version/createdBy/updatedBy 等不是外键）
      if (col.name === 'id' || col.primary) continue;
      
      // 标准公共字段不是外键引用
      const commonFields = new Set([
        'delVersion', 'version', 'createdBy', 'createTime', 'updatedBy', 'updateTime',
        'code', 'lineNo', 'remark',
      ]);
      if (commonFields.has(col.name)) continue;

      // 用户引用列（userId/roleId 域）不需要 to-one 关联
      // 这些列在ORM中应标注 stdDomain=userId，而非通过 to-one 关联到 NopAuthUser
      const userRefColumns = new Set([
        'ownerId', 'approverId', 'operatorId', 'assignedToId', 'teamLeaderId',
        'handlerId', 'pickerId', 'interviewerId',
      ]);
      if (userRefColumns.has(col.name)) continue;
      
      // 检测 _id 或 Id 后缀
      if (col.name.endsWith('Id') || col.name.endsWith('_id')) {
        // 推导预期的关联名：去掉 Id 后缀，首字母小写
        // orderId → order, supplierId → supplier, orderLineId → orderLine
        const expectedRelName = col.name
          .replace(/Id$|_id$/, '')
          .replace(/^./, c => c.toLowerCase());
        
        if (!existingToOneNames.has(expectedRelName)) {
          // 检查是否已有其他名字的 to-one 指向同一目标
          // 通过 join leftProp 匹配
          const hasJoinMatch = entity.toOneRels.some(rel =>
            rel.join && rel.join.leftProp === col.name
          );
          
          if (!hasJoinMatch) {
            issues.push({
              severity: 'WARN',
              type: 'MISSING_TO_ONE',
              entity: entity.shortName,
              column: col.name,
              expectedRelName,
              message: `外键列 ${col.name}(${col.displayName || ''}) 没有 to-one 关联定义。预期关联名: ${expectedRelName}`,
              module: moduleName,
            });
          }
        }
      }
    }
    
    // === 2. 检查 to-many 是否标注了 ext:estRows ===
    for (const rel of entity.toManyRels) {
      if (rel.estRows == null) {
        issues.push({
          severity: 'WARN',
          type: 'MISSING_EST_ROWS',
          entity: entity.shortName,
          relation: rel.name,
          refEntity: rel.refEntityName?.split('.').pop(),
          message: `to-many ${rel.name} 未标注 ext:estRows（预估行数）`,
          module: moduleName,
        });
      } else {
        const estRows = parseInt(rel.estRows);
        if (estRows > MAX_COLLECTION_ROWS && !rel.estRowsReason) {
          issues.push({
            severity: 'ERROR',
            type: 'LARGE_COLLECTION_NO_REASON',
            entity: entity.shortName,
            relation: rel.name,
            estRows,
            message: `to-many ${rel.name} 预估 ${estRows} 行（>${MAX_COLLECTION_ROWS}），缺少 ext:estRowsReason 理由说明`,
            module: moduleName,
          });
        }
      }
    }
    
    // === 3. 检查 to-many 是否有反向 to-one ===
    for (const rel of entity.toManyRels) {
      const refEntityName = rel.refEntityName;
      let refEntity = entityByClassName[refEntityName];
      if (!refEntity) {
        // 尝试按 shortName 查（可能是跨模块引用）
        const refShortName = refEntityName?.split('.').pop();
        refEntity = entityByShortName[refShortName];
      }
      
      if (refEntity) {
        // 检查被引用实体是否有 to-one 指回当前实体
        const hasReverseToOne = refEntity.toOneRels.some(r => r.refEntityName === entity.className);
        if (!hasReverseToOne) {
          issues.push({
            severity: 'INFO',
            type: 'MISSING_REVERSE_TO_ONE',
            entity: entity.shortName,
            relation: rel.name,
            refEntity: refEntity.shortName,
            message: `to-many ${rel.name} → ${refEntity.shortName} 缺少反向 to-one（${refEntity.shortName} 没有指向 ${entity.shortName} 的关联）`,
            module: moduleName,
          });
        }
      }
    }
  }
  
  return issues;
}

// --- 主流程 ---

function main() {
  const files = [];
  
  // 收集所有 orm.xml 文件
  const modulesDir = path.join(rootDir);
  const entries = readdirSync(modulesDir);
  for (const entry of entries) {
    if (!entry.startsWith('module-')) continue;
    const modelDir = path.join(modulesDir, entry, 'model');
    try {
      const stat = statSync(modelDir);
      if (!stat.isDirectory()) continue;
      const modelFiles = readdirSync(modelDir).filter(f => f.endsWith('.orm.xml'));
      for (const f of modelFiles) {
        files.push(path.join(modelDir, f));
      }
    } catch (e) {
      // 目录不存在，跳过
    }
  }
  
  if (files.length === 0) {
    console.error('No orm.xml files found');
    process.exit(1);
  }
  
  console.log(`扫描 ${files.length} 个 ORM 模型文件\n`);
  
  let allIssues = [];
  const stats = {
    entities: 0,
    toOne: 0,
    toMany: 0,
    fkColumns: 0,
  };
  
  // 先全局解析所有文件（用于跨模块引用检查）
  const allEntities = [];
  const fileEntities = {};
  
  for (const file of files) {
    const entities = parseXML(file);
    fileEntities[file] = entities;
    allEntities.push(...entities);
    stats.entities += entities.length;
    for (const e of entities) {
      stats.toOne += e.toOneRels.length;
      stats.toMany += e.toManyRels.length;
      stats.fkColumns += e.columns.filter(c =>
        (c.name.endsWith('Id') || c.name.endsWith('_id')) &&
        c.name !== 'id' && !c.primary
      ).length;
    }
  }
  
  // 逐文件审计（传入全局实体索引用于跨模块检查）
  for (const file of files) {
    const entities = fileEntities[file];
    const issues = auditEntities(entities, file);
    allIssues.push(...issues);
  }
  
  // 按模块分组
  const byModule = {};
  for (const issue of allIssues) {
    if (!byModule[issue.module]) byModule[issue.module] = [];
    byModule[issue.module].push(issue);
  }
  
  for (const [mod, issues] of Object.entries(byModule).sort()) {
    const missingToOne = issues.filter(i => i.type === 'MISSING_TO_ONE');
    const missingEstRows = issues.filter(i => i.type === 'MISSING_EST_ROWS');
    const largeColl = issues.filter(i => i.type === 'LARGE_COLLECTION_NO_REASON');
    
    if (missingToOne.length === 0 && missingEstRows.length === 0 && largeColl.length === 0) continue;
    
    console.log(`\n=== ${mod} ===`);
    
    if (missingToOne.length > 0) {
      console.log(`  缺失 to-one (${missingToOne.length}):`);
      const byEntity = {};
      for (const i of missingToOne) {
        if (!byEntity[i.entity]) byEntity[i.entity] = [];
        byEntity[i.entity].push(i);
      }
      for (const [entity, ents] of Object.entries(byEntity).sort()) {
        const cols = ents.map(e => `${e.column}→${e.expectedRelName}`).join(', ');
        console.log(`    ${entity}: ${cols}`);
      }
    }
    
    if (missingEstRows.length > 0) {
      console.log(`  to-many 缺 ext:estRows (${missingEstRows.length}):`);
      const byEntity = {};
      for (const i of missingEstRows) {
        if (!byEntity[i.entity]) byEntity[i.entity] = [];
        byEntity[i.entity].push(i);
      }
      for (const [entity, ents] of Object.entries(byEntity).sort()) {
        const rels = ents.map(e => `${e.relation}→${e.refEntity}`).join(', ');
        console.log(`    ${entity}: ${rels}`);
      }
    }
    
    if (largeColl.length > 0) {
      console.log(`  ❌ 大集合无理由 (${largeColl.length}):`);
      for (const i of largeColl) {
        console.log(`    ${i.entity}.${i.relation}: ${i.estRows} 行`);
      }
    }
  }
  
  // 统计摘要
  console.log('\n--- 统计 ---');
  console.log(`实体总数: ${stats.entities}`);
  console.log(`外键列 (_id/_Id): ${stats.fkColumns}`);
  console.log(`to-one 关联: ${stats.toOne}`);
  console.log(`to-many 关联: ${stats.toMany}`);
  console.log(`\n问题统计:`);
  const byType = {};
  for (const issue of allIssues) {
    byType[issue.type] = (byType[issue.type] || 0) + 1;
  }
  for (const [type, count] of Object.entries(byType).sort((a, b) => b[1] - a[1])) {
    console.log(`  ${type}: ${count}`);
  }
  
  const errors = allIssues.filter(i => i.severity === 'ERROR');
  if (errors.length > 0) {
    console.log(`\n退出码 1（${errors.length} 个 ERROR）`);
    process.exit(1);
  }
}

main();
