# 成本中心维度(Cost Center)

## 目的

设计责任会计的成本中心/利润中心主数据 + 凭证行辅助核算维度 + 科目分摊规则(GL Distribution),实现管理会计的多维度核算与分摊。

## 设计范式

成本中心本质是**凭证行的辅助核算维度**,与现有 `partnerId/departmentId/projectId/warehouseId/materialId` 平级(凭证行已有这五列)。对照 ERPNext `gl_entry.json:55` 的 `cost_center` 维度、iDempiere `AD_Org` 兼作成本中心、Metasfresh 的 `CostElementAccounts`。

**与 GL Distribution 的关系**:成本中心维度提供"挂账粒度",GL Distribution 提供"按成本中心再分摊"的规则——一条挂 A 成本中心的凭证行,经 `IErpFinFactsValidator`(GL Distribution 实现)按比例拆成 A/B/C 多行。

## 实体清单

### ErpMdCostCenter(成本/利润中心主数据,表 `erp_md_cost_center`,归属 master-data)

| 字段 | 含义 |
|---|---|
| id/code/name/orgId | 标准 |
| parentId | 父级(树形,对照 iDempiere AD_Org 树) |
| centerType | dict `erp-md/cost-center-type`:COST(成本中心)/PROFIT(利润中心)/INVESTMENT(投资中心) |
| managerId | 责任人(员工) |
| isProfitAndLoss | 是否损益类(影响结账时是否结转本年利润) |
| validFrom/validTo | 生效区间 |
| status | dict `erp-md/active-status` |
| 标准审计字段 | |

**状态机**:仅 `ACTIVE/INACTIVE`(主数据,无 docStatus 双轴)。停用时不允许新凭证引用,存量凭证保留。

> 该实体放在 `module-master-data/model/app-erp-master-data.orm.xml`,finance 域通过 notGenCode 外部引用机制引用。

### ErpFinGlDistribution(科目分摊规则,表 `erp_fin_gl_distribution`,归属 finance)

实现 posting.md 已占位的 GL Distribution。一条规则把"一条凭证行"按维度拆成多条。

| 字段 | 含义 |
|---|---|
| id/code/name/orgId | 标准 |
| acctSchemaId | 账套 |
| sourceSubjectId | 被分摊的源科目(如"管理费用-总部") |
| sourceCostCenterId/sourceProjectId/sourceDepartmentId | 触发匹配的源维度(任一非空) |
| validFrom/validTo | 生效区间 |
| isActive | 启用 |
| docStatus | dict:DRAFT/ACTIVE/CANCELLED(简化双轴) |
| 标准审计字段 | |

**状态机**:`DRAFT → ACTIVE → CANCELLED`。ACTIVE 后被 IErpFinFactsValidator 实现自动拾取。

### ErpFinGlDistributionLine(分摊规则行,表 `erp_fin_gl_distribution_line`)

| 字段 | 含义 |
|---|---|
| id/distributionId/lineNo | 标准 |
| targetSubjectId | 目标科目 |
| targetCostCenterId/targetDepartmentId/targetProjectId | 目标维度 |
| percent | 分摊比例(DECIMAL(20,8),所有行 Σ=100) |
| amountExpression | 可选:表达式替代百分比(同 VoucherTemplateLine) |
| 标准审计字段 | |

## 业务规则

1. **凭证行新增 `costCenterId` 列**(ErpFinVoucherLine 与 ErpFinGlBalance 同步加),与 projectId/departmentId 同级、同语义、同可空。ErpMdSubject 新增 `isAuxiliaryCostCenter BOOLEAN` 控制该科目是否要求必填成本中心(对照已有的 isAuxiliaryPartner/Department/Project/Warehouse/Product 系列)。

2. **FactsValidator 集成**:新增 `ErpFinGlDistributionValidator implements IErpFinFactsValidator`(接口见 posting.md:127-147),在凭证写库前对每条匹配 ErpFinGlDistribution 的分录行按 percent 拆分,输出多条目标行;getOrder() 设较高值确保在其他 Validator 之后执行。

3. **平衡保持**:分摊只改维度不改总金额,借贷平衡不变;Validator 校验 Σ percent = 100,否则抛 NopException。

4. **树形汇总**:成本中心报表(如部门利润表)通过 ErpMdCostCenter.parentId 递归 CTE 聚合 GlBalance。

5. **利润中心结转**:期末结账(period-close.md)时,对 centerType=PROFIT 的成本中心归集收入/费用,生成结转凭证至"本年利润"。

6. **成本中心不可删**:一旦被凭证引用,status 只能置 INACTIVE(保留审计)。

7. **行级权限**:ErpMdCostCenter 作为行级权限维度(guidelines §6.2),用户只能看自己中心的凭证。

## 与现有实体的关系

- **ErpFinVoucherLine**:新增 `costCenterId BIGINT`(位置紧邻 departmentId)+ to-one `costCenter → ErpMdCostCenter`。
- **ErpFinGlBalance**:新增 costCenterId,加入唯一键组合。
- **ErpMdSubject**:新增 isAuxiliaryCostCenter。
- **IErpFinFactsValidator**:新增 ErpFinGlDistributionValidator Bean,零改动财务核心。
- **预算管理(budget.md)**:budgetLine.costCenterId 依赖本实体。

## 关键决策

> **成本中心用物理列而非 EAV** —— 严格遵循 guidelines §10.6 的"orgId/posted/标准维度物理列"决策。**主数据放 master-data、分摊规则放 finance**,符合 guidelines §1.1 的"主数据归属 master-data,分摊规则是业财逻辑归属 finance"。

## 菜单归属

- master-data 域「核算维度」分组:成本中心(ErpMdCostCenter)。
- finance 域「科目分摊」分组:分摊规则(ErpFinGlDistribution)。

## 参考

- `docs/analysis/erp-survey/2026-06-22-0000-metasfresh.md`(CostElementAccounts 多维科目解析)
- `docs/design/finance/posting.md`(IErpFinFactsValidator 扩展点、GL Distribution 关系)
