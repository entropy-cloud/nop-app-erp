#!/usr/bin/env python3
"""Add minimal entity declarations for all cross-module refEntityName references."""

import re, os

root = '/Users/abc/app/nop-app-erp'

# Cached entity declarations (minimal columns) for each known target
ENTITY_STUBS = {
    # master-data
    'ErpMdBankAccount': ('erp_md_bank_account', 'erp/md', ['code', 'bankName']),
    'ErpMdCostCenter': ('erp_md_cost_center', 'erp/md', ['code', 'name']),
    'ErpMdCurrency': ('erp_md_currency', 'erp/md', ['code', 'name']),
    'ErpMdEmployee': ('erp_md_employee', 'erp/md', ['code', 'name']),
    'ErpMdLocation': ('erp_md_location', 'erp/md', ['code', 'name']),
    'ErpMdMaterial': ('erp_md_material', 'erp/md', ['code', 'name']),
    'ErpMdMaterialSku': ('erp_md_material_sku', 'erp/md', ['code', 'name']),
    'ErpMdOrganization': ('erp_md_organization', 'erp/md', ['code', 'name']),
    'ErpMdPartner': ('erp_md_partner', 'erp/md', ['code', 'name']),
    'ErpMdUoM': ('erp_md_uom', 'erp/md', ['code', 'name']),
    'ErpMdWarehouse': ('erp_md_warehouse', 'erp/md', ['code', 'name']),
    'ErpMdAcctSchema': ('erp_md_acct_schema', 'erp/md', ['code', 'name']),
    'ErpMdSubject': ('erp_md_subject', 'erp/md', ['code', 'name']),
    'ErpMdTaxRate': ('erp_md_tax_rate', 'erp/md', ['code', 'name']),
    'ErpMdSettlementMethod': ('erp_md_settlement_method', 'erp/md', ['code', 'name']),
    # finance
    'ErpFinVoucher': ('erp_fin_voucher', 'erp/fin', ['code']),
    'ErpFinFundAccount': ('erp_fin_fund_account', 'erp/fin', ['code', 'name']),
    # inventory
    'ErpInvBatch': ('erp_inv_batch', 'erp/inv', ['code']),
    'ErpInvStockMove': ('erp_inv_stock_move', 'erp/inv', ['code']),
    # projects
    'ErpPrjProject': ('erp_prj_project', 'erp/prj', ['code', 'name']),
    'ErpPrjTask': ('erp_prj_task', 'erp/prj', ['code', 'name']),
    # assets
    'ErpAstAsset': ('erp_ast_asset', 'erp/ast', ['code', 'name']),
    # contract-local (self-refs)
    'ErpCtApprovalMatrix': ('erp_ct_approval_matrix', 'erp/ct', ['code', 'name']),
    'ErpCtSignatureRequest': ('erp_ct_signature_request', 'erp/ct', ['code']),
}

def stub_display_name(en):
    names = {
        'ErpMdBankAccount': '银行账户', 'ErpMdCostCenter': '成本中心',
        'ErpMdCurrency': '币种', 'ErpMdEmployee': '员工',
        'ErpMdLocation': '库位', 'ErpMdMaterial': '物料',
        'ErpMdMaterialSku': '物料SKU', 'ErpMdOrganization': '组织',
        'ErpMdPartner': '往来单位', 'ErpMdUoM': '计量单位',
        'ErpMdWarehouse': '仓库', 'ErpMdAcctSchema': '会计核算表',
        'ErpMdSubject': '会计科目', 'ErpMdTaxRate': '税率',
        'ErpMdSettlementMethod': '结算方式', 'ErpFinVoucher': '凭证',
        'ErpFinFundAccount': '资金账户', 'ErpInvBatch': '批次',
        'ErpInvStockMove': '库存移动', 'ErpPrjProject': '项目',
        'ErpPrjTask': '任务', 'ErpAstAsset': '资产',
        'ErpCtApprovalMatrix': '审批矩阵', 'ErpCtSignatureRequest': '签名请求',
    }
    return names.get(en, en)

def make_stub(entity_classname, table_name, biz_module, extra_cols):
    pkg = 'app.erp.' + biz_module.split('/')[1] if '/' in biz_module else biz_module
    pkg = pkg.replace('erp/', 'app.erp.')
    # Try to determine correct package
    if entity_classname.startswith('ErpMd'):
        pkg_base = 'app.erp.md.dao.entity'
    elif entity_classname.startswith('ErpCt'):
        pkg_base = 'app.erp.ct.dao.entity'
    elif entity_classname.startswith('ErpFin'):
        pkg_base = 'app.erp.fin.dao.entity'
    elif entity_classname.startswith('ErpInv'):
        pkg_base = 'app.erp.inv.dao.entity'
    elif entity_classname.startswith('ErpPrj'):
        pkg_base = 'app.erp.prj.dao.entity'
    elif entity_classname.startswith('ErpAst'):
        pkg_base = 'app.erp.ast.dao.entity'
    else:
        pkg_base = pkg
    
    dn = stub_display_name(entity_classname)
    cols_xml = f'''<column name="id" code="ID" stdSqlType="BIGINT" primary="true" stdDataType="long" i18n-en:displayName='ID' />'''
    for col in extra_cols:
        stype = 'VARCHAR' if col in ('code', 'name', 'bankName') else 'INTEGER'
        prec = '50' if col in ('code', 'bankName') else '200' if col == 'name' else ''
        prec_attr = f' precision="{prec}"' if prec else ''
        cols_xml += f'''
                <column name="{col}" code="{col.upper()}" stdSqlType="{stype}"{prec_attr} stdDataType="string" i18n-en:displayName='{col.title()}' />'''
    
    return f'''        <entity displayName="{dn}" name="{pkg_base}.{entity_classname}"
                notGenCode="true" biz:moduleId="{biz_module}" tableName="{table_name}">
            <columns>
                {cols_xml}
            </columns>
        </entity>'''

for mod_dir in sorted(os.listdir(root)):
    if not mod_dir.startswith('module-'): continue
    model_dir = os.path.join(root, mod_dir, 'model')
    if not os.path.isdir(model_dir): continue
    for f in os.listdir(model_dir):
        if not f.endswith('.orm.xml'): continue
        fpath = os.path.join(model_dir, f)
        with open(fpath) as fh:
            content = fh.read()
        
        # Find all refEntityName references (all modules)
        refs = set()
        for m in re.finditer(r'refEntityName="([^"]+)"', content):
            refs.add(m.group(1))
        
        # Find all declared entity names
        declared = set()
        for m in re.finditer(r'name="([^"]+)"', content):
            declared.add(m.group(1))
        
        missing = refs - declared
        # Filter cross-module ones only
        local_prefixes = [f'app.erp.{mod_dir.replace("module-", "")[:3]}', 'io.nop.']
        cross = {e for e in missing if not any(e.startswith(p) for p in local_prefixes) and not e.startswith('java.')}
        if not cross:
            continue
        
        # Build stubs
        stubs = []
        short_mod = mod_dir.replace('module-', '')
        for ent in sorted(cross):
            classname = ent.split('.')[-1]
            if classname in ENTITY_STUBS:
                table, bizmod, cols = ENTITY_STUBS[classname]
                stubs.append(make_stub(classname, table, bizmod, cols))
            else:
                print(f"  WARNING: No stub for {ent} in {mod_dir}")
        
        if not stubs:
            continue
        
        # Insert before </entities>
        stub_block = '\n\n'.join(stubs)
        new_content = content.replace('    </entities>\n</orm>', f'{stub_block}\n    </entities>\n</orm>')
        if new_content != content:
            with open(fpath, 'w') as fh:
                fh.write(new_content)
            print(f"  {mod_dir}: added {len(stubs)} entity decls ({', '.join(e.split('.')[-1] for e in cross)})")
