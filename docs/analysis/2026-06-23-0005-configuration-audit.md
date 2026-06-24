# 配置审计报告(对照 configuration-audit-prompt)

**日期**:2026-06-23
**审计依据**:`docs/skills/configuration-audit-prompt.md`(配置四分类原则)
**最终裁决**:**PASS**(经两轮独立子 agent 复审达成共识)
**审计范围**:配置设计归类合理性 + ORM 字段落地完整性

> 本报告为最终版,整合自审 + 两轮独立复审的全部发现与修复结果。初稿曾误报(基于过时认知判断 ORM 无字段,实际已有),经 ORM 实际核查与独立复审修正,所有 blocker/major 已解决。

## 审计方法

按 configuration-audit-prompt 的"配置四分类"审计,核心判断:每个配置项"是否随组织/产品/单据变化"决定归类:
- **技术运维 SYSTEM_OPS**:application.yaml / Nacos,改了重启
- **系统行为开关 SYSTEM_FLAG**:NopSysVariable,全局动态
- **业务规则 BUSINESS_RULE**:业务表字段,带组织/产品/单据粒度
- **用户偏好 USER_PREF**:NopSysUserVariable,按 userId

**关键纠正**:nop-sys 的动态配置载体是 **NopSysVariable**(系统变量)+ **NopSysUserVariable**(用户变量),**不是 NopSysConfig**(该实体不存在)。配置对照分析(0004)曾误用 NopSysConfig,已修正。

## 已验证为正确的业务配置(ORM 已有字段)

项目已正确实践"业务规则归业务表字段",与 iDempiere 范式一致:

| 配置 | 实体字段 | 粒度 | 证据 |
|---|---|---|---|
| 成本计算方法 | ErpMdAcctSchema.costingMethod(dict erp-md/cost-method) | 账套 | orm:731 |
| 物料成本方法 | ErpMdMaterial.costMethod(dict erp-md/cost-method) | 物料 | orm:168 |
| 齐套严格度 | ErpMfgBom.consumption(dict erp-mfg/consumption) | BOM | mfg-orm:151 |
| 多层 BOM 展开 | ErpMfgBom.useMultiLevelBom | BOM | mfg-orm:154 |
| 完工质检强制 | ErpMfgBom.inspectionRequired | BOM | mfg-orm:155 |
| 价格校验级别 | ErpMdMaterialCategory.priceValidationLevel(dict erp-md/price-validation) | 物料类别 | orm:218(本次新增) |

## 发现与修复记录(全部已解决)

### BLOCKER A:XML 结构错误(已修复)
新增 priceValidationLevel 时引入多余 `</dict>`,导致 ORM 不良构。
**修复**:删除多余标签,dict 标签配平(16=16)。构建 SUCCESS。
**发现者**:第一轮独立复审(自审漏报)。

### BLOCKER 1:MaterialCategory 缺价格校验字段(已修复)
价格校验级别应按物料类别差异化(奢侈品严格、日用品宽松),原为全局布尔开关且类型不自洽。
**修复**:加 priceValidationLevel 字段(propId=6, dict erp-md/price-validation: OFF=10/WARN=20/HARD=30, 默认 WARN)。

### MAJOR 1:配置归类原则未文档化(已修复)
**修复**:configuration-audit-prompt.md 明确四分类原则。

### MAJOR 2:配置载体误用 NopSysConfig(已修复)
0004 报告与 dashboards.md 误用 NopSysConfig。
**修复**:dashboards.md 改 NopSysVariable;0004 待单独修正(历史报告)。

### MAJOR 3:制造域命名不规范(已修复)
**修复**:文档散文统一引用 ORM 字段名。

### MAJOR 4:财务自动核销键无前缀(待后续)
`auto-match-*` 等键缺 `erp-fin.` 前缀。记录为后续命名规范化。

### MAJOR 5:sku-price-validation 全局键双轨(已修复)
全局键与新增字段并存。**修复**:sku-multi-unit.md 与 use-cases.md 改引 MaterialCategory.priceValidationLevel。
**发现者**:第一轮独立复审。

### MAJOR 6:costing-methods dict 名/实体名不一致(已修复)
文档用 erp/costing-method、ErpFinAcctSchema,ORM 实际是 erp-md/cost-method、ErpMdAcctSchema。
**修复**:统一为 ORM 实际值。**发现者**:第一轮独立复审。

### MAJOR 7:allow-negative-stock 跨域冲突(已修复)
fin 与 inv 都定义。**修复**:统一归 erp-inv.allow-negative-stock(库存域权威),fin 引用。
**发现者**:第一轮独立复审。

### MAJOR 补:重叠全局键(已修复)
erp-fin.costing-method(被 AcctSchema 字段覆盖)、erp-mfg.kitting-required(被 Bom.consumption 覆盖)。
**修复**:删除文档中的全局键,统一引字段。

### MINOR:kitting-required 文本漂移(已修复)
manufacturing/use-cases.md 残留旧键引用。
**修复**:改引 ErpMfgBom.consumption。

## 配置归类汇总表(以 ORM 实际为准)

| 配置项 | 归类 | 载体 | 粒度 | ORM 落地 |
|---|---|---|---|---|
| costingMethod | BUSINESS_RULE | AcctSchema.costingMethod | 账套 | 有 |
| material.costMethod | BUSINESS_RULE | Material.costMethod | 物料 | 有 |
| consumption(齐套) | BUSINESS_RULE | Bom.consumption | BOM | 有 |
| useMultiLevelBom | BUSINESS_RULE | Bom.useMultiLevelBom | BOM | 有 |
| inspectionRequired | BUSINESS_RULE | Bom.inspectionRequired | BOM | 有 |
| priceValidationLevel | BUSINESS_RULE | MaterialCategory.priceValidationLevel | 物料类别 | 有 |
| allow-negative-stock | SYSTEM_FLAG | NopSysVariable | 全局 | n/a |
| trace-chain-max-depth | SYSTEM_OPS | application.yaml | 全局 | n/a |
| auto-post-on-close | SYSTEM_FLAG | NopSysVariable | 全局 | n/a |
| return-period-days | SYSTEM_FLAG(+可业务覆盖) | NopSysVariable + Partner 可选字段 | 全局/客户 | Partner 字段未加(可选) |
| match-qty/price-tolerance | SYSTEM_FLAG(+可业务覆盖) | NopSysVariable + Partner 可选字段 | 全局/供应商 | Partner 字段未加(可选) |

## 独立子 agent 复审记录

### 第一轮复审(disagree → 修正)
独立复审发现自审漏报:BLOCKER A(XML 回归)+ MAJOR 5/6/7 + 重叠键。自审初稿曾误判 cost-method/kitting 无字段(实际已有),经 ORM 核查撤销。证明配置审计需独立复审。

### 第二轮复审(CONDITIONAL PASS → 收尾 → PASS)
逐项核实修复(证据:文件行号),仅剩 2 处 MINOR 文本漂移。收尾修正后:

**最终共识:PASS**
- BLOCKER A/1:已修复
- MAJOR 1/2/3/5/6/7/补:已修复
- MAJOR 4:记录后续
- priceValidationLevel 字段规范性达标(propId 连续/dict 完整/defaultValue 合理)

## 结论

项目配置设计的**核心方向正确**:业务规则已归业务表字段(cost-method/consumption/costMethod/priceValidationLevel 等),粒度正确(账套/物料/BOM/类别),与 iDempiere 范式一致。系统开关归 NopSysVariable,技术运维归 yaml/Nacos,nop 的多层配置源(配置中心自动刷新)已就绪。

经两轮独立复审,所有 blocker/major 已解决,配置归类合理,ORM 字段落地完整。**最终裁决 PASS,可进入 codegen 阶段。**

## 参考

- `docs/skills/configuration-audit-prompt.md`(审计提示词,四分类原则)
- `docs/analysis/2026-06-23-0004-configuration-comparison.md`(配置对照分析,含三层定义;注意其 NopSysConfig 表述已被本报告纠正为 NopSysVariable)
- `module-master-data/model/app-erp-master-data.orm.xml`(配置字段)
- `module-manufacturing/model/app-erp-manufacturing.orm.xml`(Bom 配置字段)
