#!/usr/bin/env python3
"""Replace NopSysAttachment to-one pattern with stdDomain="file" column pattern."""

import re, os

root = '/Users/abc/app/nop-app-erp'

# Map: (module_rel_path, entity_name_short) -> column_replacement info
REPLACEMENTS = [
    # (file_rel_path, entity_name_pattern, old_col_regex, new_col_xml, old_to_one_text)
    ('module-b2b/model/app-erp-b2b.orm.xml', 'ErpB2bEdiDoc',
     r'<column\s+name="attachmentId"[^>]*/>',
     '<column name="attachmentFileId" displayName="附件（EDI 报文文件）" stdSqlType="VARCHAR" precision="200" stdDataType="string" code="ATTACHMENT_FILE_ID" propId="11" i18n-en:displayName=\'Attachment\'><schema stdDomain="file"/></column>',
     ''),
    ('module-contract/model/app-erp-contract.orm.xml', 'ErpCtContract',
     r'<column\s+name="attachmentId"[^>]*displayName="合同附件"[^>]*/>',
     '<column name="attachmentFileId" displayName="合同附件" stdSqlType="VARCHAR" precision="200" stdDataType="string" code="ATTACHMENT_FILE_ID" propId="17" i18n-en:displayName=\'Attachment\'><schema stdDomain="file"/></column>',
     ''),
    ('module-contract/model/app-erp-contract.orm.xml', 'ErpCtContractVersion',
     r'<column\s+name="attachmentId"[^>]*displayName="版本附件"[^>]*/>',
     '<column name="attachmentFileId" displayName="版本附件" stdSqlType="VARCHAR" precision="200" stdDataType="string" code="ATTACHMENT_FILE_ID" propId="6" i18n-en:displayName=\'Attachment\'><schema stdDomain="file"/></column>',
     ''),
    ('module-contract/model/app-erp-contract.orm.xml', 'ErpCtSignatureRequest',
     r'<column\s+name="attachmentId"[^>]*displayName="已签署文件"[^>]*/>',
     '<column name="attachmentFileId" displayName="已签署文件" stdSqlType="VARCHAR" precision="200" stdDataType="string" code="ATTACHMENT_FILE_ID" propId="12" i18n-en:displayName=\'Attachment\'><schema stdDomain="file"/></column>',
     ''),
    ('module-contract/model/app-erp-contract.orm.xml', 'ErpCtDocument',
     r'<column\s+name="attachmentId"[^>]*displayName="附件"[^>]*/>',
     '<column name="attachmentFileId" displayName="附件" stdSqlType="VARCHAR" precision="200" stdDataType="string" code="ATTACHMENT_FILE_ID" propId="7" i18n-en:displayName=\'Attachment\'><schema stdDomain="file"/></column>',
     ''),
    ('module-cs/model/app-erp-cs.orm.xml', 'ErpCsContract',
     r'<column\s+name="attachmentId"[^>]*displayName="合同附件"[^>]*/>',
     '<column name="attachmentFileId" displayName="合同附件" stdSqlType="VARCHAR" precision="200" stdDataType="string" code="ATTACHMENT_FILE_ID" propId="12" i18n-en:displayName=\'Attachment\'><schema stdDomain="file"/></column>',
     ''),
    ('module-hr/model/app-erp-hr.orm.xml', 'ErpHrEmploymentContract',
     r'<column\s+name="attachmentId"[^>]*displayName="合同文件"[^>]*/>',
     '<column name="attachmentFileId" displayName="合同文件" stdSqlType="VARCHAR" precision="200" stdDataType="string" code="ATTACHMENT_FILE_ID" propId="17" i18n-en:displayName=\'Attachment\'><schema stdDomain="file"/></column>',
     ''),
    ('module-hr/model/app-erp-hr.orm.xml', 'ErpHrRecruitment',
     r'<column\s+name="resumeAttachmentId"[^>]*/>',
     '<column name="resumeAttachmentFileId" displayName="简历附件" stdSqlType="VARCHAR" precision="200" stdDataType="string" code="RESUME_ATTACHMENT_FILE_ID" propId="10" i18n-en:displayName=\'Resume Attachment\'><schema stdDomain="file"/></column>',
     ''),
]

for f_rel, ename, old_col_pat, new_col_xml, old_to_one in REPLACEMENTS:
    fpath = os.path.join(root, f_rel)
    with open(fpath) as fh:
        content = fh.read()
    
    # 1. Replace column
    old_col_match = re.search(old_col_pat, content)
    if not old_col_match:
        print(f"  SKIP {f_rel} {ename}: column not found")
        continue
    content = content.replace(old_col_match.group(0), new_col_xml, 1)
    
    # 2. Remove to-one to NopSysAttachment for this entity
    # Find the entity block and remove the to-one inside it
    entity_start = content.find(f'name="{ename}"')
    if entity_start > 0:
        entity_start = content.rfind('<entity ', 0, entity_start)
        entity_end = content.find('</entity>', entity_start)
        if entity_start > 0 and entity_end > 0:
            eblock = content[entity_start:entity_end]
            # Find to-one to NopSysAttachment
            to_one_match = re.search(r'<to-one\s+[^>]*refEntityName="io\.nop\.sys\.dao\.entity\.NopSysAttachment"[^>]*>.*?</to-one>', eblock, re.DOTALL)
            if not to_one_match:
                # Try self-closing
                to_one_match = re.search(r'<to-one\s+[^>]*refEntityName="io\.nop\.sys\.dao\.entity\.NopSysAttachment"[^>]*/>', eblock)
            if to_one_match:
                content = content[:entity_start + to_one_match.start()] + content[entity_start + to_one_match.end():]
                print(f"  {f_rel} {ename}: column + to-one replaced")
            else:
                print(f"  {f_rel} {ename}: column replaced, to-one NOT FOUND")
    
    # 3. Remove NopSysAttachment entity declaration
    # Only do this once per file (after all replacements)
    
    with open(fpath, 'w') as fh:
        fh.write(content)

# Step 3: Remove NopSysAttachment entity declarations from each file
print("\nRemoving NopSysAttachment entity declarations...")
for f_rel in set(r[0] for r in REPLACEMENTS):
    fpath = os.path.join(root, f_rel)
    with open(fpath) as fh:
        content = fh.read()
    
    # Remove entity declaration block
    new_content = re.sub(
        r'\n\s*<entity displayName="系统附件".*?</entity>',
        '',
        content,
        flags=re.DOTALL
    )
    
    if new_content != content:
        with open(fpath, 'w') as fh:
            fh.write(new_content)
        print(f"  {f_rel}: removed NopSysAttachment entity declaration")
    else:
        print(f"  {f_rel}: no entity declaration found")
PYEOF