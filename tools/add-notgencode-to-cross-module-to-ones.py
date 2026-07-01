#!/usr/bin/env python3
"""Add notGenCode=true to to-ones referencing cross-module declared entities."""

import re, os

root = '/Users/abc/app/nop-app-erp'

MODULE_BIZ_IDS = {
    'purchase': 'erp/pur', 'sales': 'erp/sal', 'inventory': 'erp/inv',
    'finance': 'erp/fin', 'assets': 'erp/ast', 'projects': 'erp/prj',
    'manufacturing': 'erp/mfg', 'quality': 'erp/qa', 'maintenance': 'erp/mnt',
    'master-data': 'erp/md', 'hr': 'erp/hr', 'drp': 'erp/drp',
    'crm': 'erp/crm', 'cs': 'erp/cs', 'contract': 'erp/ct',
    'aps': 'erp/aps', 'b2b': 'erp/b2b', 'logistics': 'erp/log',
}

# Build entity -> biz:moduleId mapping from all ORM files
entity_module = {}
for mod_dir in sorted(os.listdir(root)):
    if not mod_dir.startswith('module-'): continue
    model_dir = os.path.join(root, mod_dir, 'model')
    if not os.path.isdir(model_dir): continue
    for f in os.listdir(model_dir):
        if not f.endswith('.orm.xml'): continue
        with open(os.path.join(model_dir, f)) as fh:
            content = fh.read()
        for m in re.finditer(r'<entity\s+[^>]*name="([^"]+)"[^>]*biz:moduleId="([^"]+)"', content):
            entity_module[m.group(1)] = m.group(2)

total_added = 0

for mod_dir in sorted(os.listdir(root)):
    if not mod_dir.startswith('module-'): continue
    model_dir = os.path.join(root, mod_dir, 'model')
    if not os.path.isdir(model_dir): continue
    short_mod = mod_dir.replace('module-', '')
    local_biz_id = MODULE_BIZ_IDS.get(short_mod)
    if not local_biz_id:
        continue
    
    for f in os.listdir(model_dir):
        if not f.endswith('.orm.xml'): continue
        fpath = os.path.join(model_dir, f)
        with open(fpath) as fh:
            content = fh.read()
        
        changed = False
        
        for m in re.finditer(r'<to-one\s+([^>]*?)refEntityName="([^"]+)"([^>]*?)/?>', content, re.DOTALL):
            attrs = m.group(1) + m.group(3)
            ref_entity = m.group(2)
            
            if 'notGenCode="true"' in attrs:
                continue
            
            biz_id = entity_module.get(ref_entity)
            if not biz_id or biz_id == local_biz_id:
                continue  # not cross-module or same module
            
            old_str = m.group(0)
            new_str = old_str.replace(
                'refEntityName="' + ref_entity + '"',
                'refEntityName="' + ref_entity + '" notGenCode="true" biz:moduleId="' + biz_id + '"'
            )
            content = content.replace(old_str, new_str, 1)
            changed = True
            total_added += 1
        
        if changed:
            with open(fpath, 'w') as fh:
                fh.write(content)

print(f"Added notGenCode to {total_added} cross-module to-ones")
