# master-data 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（测试共享夹具见 `app-erp-test-data`，边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.1a（plan `docs/plans/2026-09-01-0838-1-m11a-md-sal-seed-expansion.md`，2026-09-01）。此前 21 张 master-data 表 seed 见 `docs/architecture/seed-data.md` 历史批次段。

## 种子数据范围（M1.1a 批次 4 表）

master-data 域 25 实体中 21 表已由历史批次 seed（`erp_md_*` 21 CSV）；本批补齐其余 4 表，达成**域内全量 seed 覆盖（25/25）**。

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpMdMaterialCustoms（物料报关记录） | `erp_md_material_customs.csv` | 2 | P | MATERIAL_ID→ErpMdMaterial〔已seed〕 |
| ErpMdSubjectMapping（科目映射） | `erp_md_subject_mapping.csv` | 2 | P | SOURCE_SUBJECT_ID/TARGET_SUBJECT_ID→ErpMdSubject〔已seed〕、TARGET_ACCT_SCHEMA_ID→ErpMdAcctSchema〔已seed〕 |
| ErpMdSupplierApproval（供应商准入资格） | `erp_md_supplier_approval.csv` | 3 | P+N-TERM | PARTNER_ID→ErpMdPartner〔已seed〕、MATERIAL_CATEGORY_ID→ErpMdMaterialCategory〔已seed〕 |
| ErpSysConfig（系统配置） | `erp_sys_config.csv` | 3 | P | —（无必填 FK；ORG_ID 可选指向 ErpMdOrganization〔已seed〕） |

## FK 闭环图

```
erp_md_material(1..4 已seed) ──MATERIAL_ID──▶ erp_md_material_customs
erp_md_partner(1..5 已seed) ──PARTNER_ID───▶ erp_md_material_customs（报关行，可选）
                              ──PARTNER_ID──▶ erp_md_supplier_approval
erp_md_material_category(1..3 已seed) ─MATERIAL_CATEGORY_ID─▶ erp_md_supplier_approval
erp_md_subject(1..46 已seed) ─SOURCE/TARGET_SUBJECT_ID─▶ erp_md_subject_mapping
erp_md_acct_schema(1 已seed) ─TARGET_ACCT_SCHEMA_ID───▶ erp_md_subject_mapping
erp_md_organization(1..2 已seed) ─ORG_ID（可选）─▶ erp_md_supplier_approval / erp_sys_config
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空（`TestErpSeedDataIntegrity` 引用完整性门禁背书）。

## 用例指示编码与 negative 行语义

编码定义见 `docs/architecture/seed-data.md`「270 个缺 seed 实体最小可用数据集规格表」通用约定 5：

- **P（最小正例行）**：全部 4 表均有。
- **N-TERM（终态行）**：`erp_md_supplier_approval.csv` 第 3 行 `STATUS=REJECTED`（准入状态机 APPLIED→APPROVED→PROBATION→SUSPENDED→REJECTED 的终态；业务语义 = 已废弃不参与同 partner 区间互斥校验，见 `ErpMdSupplierApprovalBizModel`），供准入状态机非法迁移守卫负路径消费。
- **N-DIS（禁用行）**：本批 master-data 4 表无 enabled/isActive 列，未设。

## 约定对齐

- 列头 = DB 列名大写下划线；省略审计列（delVersion/version/createdBy/createTime/updatedBy/updateTime）；ISO 日期；小写布尔；ID < 100000（`zz-sequence-advance.sql` 序列推进值域约束）。
- `uomDeclared`（UOM_DECLARED）为海关法定单位 VARCHAR（非 FK→ErpMdUoM，海关/内部单位字典解耦，见 ORM 注记），seed 值 `PCS`。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议）。
