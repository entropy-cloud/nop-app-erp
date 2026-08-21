#!/usr/bin/env node
/**
 * dao 层语义性跨域 FK Long 参数/字段扫描（M0.1 审计工件附录 C 生成器）
 * 扫描各域 dao 手写代码中「本域签名声明 Long 但语义指向他域实体」的 @Name("xxxId") Long 参数
 * 与 private Long xxxId 字段（FK 名→归属域映射为启发式，各域 plan 以本域 orm FK 列为准复核）。
 * 用法: node tools/scan-dao-semantic-fk-params.mjs
 */
import { readFileSync, readdirSync, statSync } from 'fs';
import path from 'path';
const root = process.cwd();
// FK 名 → 归属域（据 19 域 orm 实体归属；own 判定后剩余即跨域语义 FK）
const FK_OWNER = {
  materialId:'master-data', skuId:'master-data', uoMId:'master-data', currencyId:'master-data',
  partnerId:'master-data', supplierId:'master-data', customerId:'master-data', employeeId:'hr',
  departmentId:'hr', positionId:'hr', warehouseId:'inventory', locationId:'inventory',
  periodId:'finance', expenseSubjectId:'finance', subjectId:'finance', acctSchemaId:'master-data',
  projectId:'projects', assetId:'assets', workcenterId:'manufacturing', routingId:'manufacturing',
  bomId:'manufacturing', carrierId:'logistics', templateId:'contract', orgId:'master-data',
  materialCategoryId:'master-data', userAccountId:'-', superiorId:'hr', teamId:'crm',
  sourceWarehouseId:'inventory', destWarehouseId:'inventory', sourceLocationId:'inventory', destLocationId:'inventory',
};
const DOM = { 'module-aps':'aps','module-assets':'assets','module-b2b':'b2b','module-contract':'contract','module-crm':'crm','module-cs':'cs','module-drp':'drp','module-finance':'finance','module-hr':'hr','module-inventory':'inventory','module-logistics':'logistics','module-maintenance':'maintenance','module-manufacturing':'manufacturing','module-master-data':'master-data','module-notify':'notify','module-projects':'projects','module-purchase':'purchase','module-quality':'quality','module-sales':'sales' };
function walk(dir, out){ let es; try{es=readdirSync(dir)}catch{return} for(const e of es){const p=path.join(dir,e);let st;try{st=statSync(p)}catch{continue} if(st.isDirectory()){if(e==='_gen'||e==='target')continue;walk(p,out)} else if(e.endsWith('.java')&&!e.startsWith('_')) out.push(p);} }
for(const [mod,dom] of Object.entries(DOM)){
  const daoDir = path.join(root,mod); const layers = readdirSync(daoDir).filter(d=>d.endsWith('-dao'));
  for(const l of layers){ const files=[]; walk(path.join(daoDir,l,'src/main/java'),files);
    for(const f of files){ const lines = readFileSync(f,'utf-8').split('\n');
      lines.forEach((line,i)=>{ if(/interface|class/.test(lines.slice(0,3).join(''))){}
        for(const m of line.matchAll(/@Name\("(\w*[Ii]d)"\)\s+Long\s+\w+/g)){ const name=m[1];
          const owner = FK_OWNER[name] || FK_OWNER[name.replace(/Id$/,'Id')];
          if(owner && owner !== dom && owner !== '-')
            console.log(`${mod}\t${path.relative(root,f)}:${i+1}\t${name}\t-> ${owner}`);
        }
        for(const m of line.matchAll(/private\s+Long\s+(\w*Id)\s*;/g)){ const name=m[1];
          const owner = FK_OWNER[name];
          if(owner && owner !== dom && owner !== '-')
            console.log(`${mod}\t${path.relative(root,f)}:${i+1}\t${name}(field)\t-> ${owner}`);
        }
      });
    }
  }
}
