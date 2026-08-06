# 2026-08-06-1926 rc-ma4-dashboard-orgid-rowlevel-permission-leak 看板 orgId 行级权限跨组织泄漏运行时确认（finance + 8 扩展域，根因 P1-MA2-093）

> 验证类型：verification or audit work（只读评估，零生产代码变更）
> 验证范围：MA4 A4.1.25（finance 看板 `period.orgId` scope）+ A4.2.10（8 扩展域看板 orgId 行级权限）
> 关联 finding：P1-MA2-093（orgId 查询隔离全仓未落地，✅ resolved R1.29）
> 裁决：**维持 P1-MA2-093 resolved R1.29**；**新建 P2-RC-086 watch-only**（看板直访路径 orgId scope 缺失 — R1.29 transformer 非覆盖残留，successor 多组织部署启用时补 orgId scope）

---

## §0 执行摘要

本验证对 **11 个 DashboardBizModel 全集**（finance/mfg/sal/ast/inv/qa/prj/md/mnt/cs/pur）逐域普查聚合查询路径类型 × orgId scope 消费状态，并核验 R1.29 `ErpOrgIsolationQueryTransformer` 覆盖面。

**核心结论**：

1. **R1.29 transformer 不覆盖看板直访路径**——`ErpOrgIsolationQueryTransformer`（bean `nopGlobalQueryTransformer`）由平台 `CrudBizModel` 经 `@Named("nopGlobalQueryTransformer")` 自动注入并应用于 findPage/findList（CrudBizModel 管道内）；看板 BizModel 经 `IDaoProvider.daoFor(...).findAllByQuery(q)` / `IOrmTemplate.findListByQuery(q)` 直访**绕过 CrudBizModel 管道**，故 transformer 不注入。证据：transformer Javadoc 显式 Non-Goal（`ErpOrgIsolationQueryTransformer.java:30-31` 逐字「dashboard/report BizModel 经 IDaoProvider 直访绕过 CrudBizModel 管道，**不被本 transformer 覆盖**」）+ `TestErpOrgIsolation.testReadIsolationFiltersOtherOrg` 仅覆盖 CrudBizModel 路径（`arApItemBiz.findList`）非直访。

2. **10/11 域看板直访路径无 orgId scope**（master-data 例外：ErpMdMaterial/ErpMdPartner 无 orgId 列，组织无关设计，看板 org-agnostic 正确）。finance 部分方法手工补 orgId 但**源是 `period.orgId` 非登录用户组织**（§7 SP-4 原文存疑点），且 `sumBankBalance:214-224` / `findLatestPeriodId:181-188` 无 orgId。

3. **单组织基线（默认 `org-isolation-enabled=false`）无活跃泄漏**——隔离总开关关闭 → transformer/interceptor 全 no-op → 不存在跨组织数据可见；生产/demo seed 单组织（orgId=2）。泄漏**仅在 successor 条件（多组织部署 + 隔离开启）**显现。

4. **裁决**：§2 P2① 命中（次要验收标准未完全满足——主路径[单组织基线/默认关闭]OK，边界[多组织部署直访路径]弱）。**维持 P1-MA2-093 resolved R1.29**（隔离能力已实现，config-gated 默认关闭）；**新建 P2-RC-086 watch-only**（看板直访路径 orgId scope 缺失，R1.29 transformer 非覆盖残留，归 successor 多组织部署启用时修复）。**零 P0/P1**（config-gated 默认关闭 + 单组织种子 → 无活跃数据破坏 + 非会计过账核心路径）。

---

## §1 跨域 census 矩阵（11 域 × 查询方法 × 路径类型 × orgId scope 状态）

> 路径类型：`直访` = `daoProvider.daoFor(...).findAllByQuery/findAllByQuery/countByQuery` 或 `ormTemplate.findListByQuery`（绕过 CrudBizModel 管道，transformer 不注入）；`IBiz` = 经 `I*Biz.findList/findOpenItems`（经 CrudBizModel 管道，transformer 注入）。
> orgId scope 状态：`无` = QueryBean 零 orgId 过滤；`period.orgId` = 手工补 orgId 但源是期间组织非登录用户组织（§7 SP-4 存疑点）；`N/A` = 实体无 orgId 列（组织无关设计）。

| 域 | BizModel 文件 | 查询方法 | 路径类型 | orgId scope | file:line | 多组织泄漏面 |
|---|---|---|---|---|---|---|
| **finance** | ErpFinDashboardBizModel | loadGlBalances | 直访 | period.orgId（非用户） | `:161-179` applyOrgAndSchemaScope | §7 SP-4（用户可传 orgB 期间看 orgB 数据） |
| | | findLatestPeriodId | 直访 | **无** | `:181-188`（仅 orderField+limit） | 是（取最近期间不限组织） |
| | | loadGlBalancesInRange | 直访 | period.orgId（首个期间） | `:190-212 :203` | §7 SP-4 |
| | | sumBankBalance | 直访 | **无** | `:214-224`（仅 accountType=BANK） | **是**（P1-MA2-093 原例：汇总所有组织银行账户） |
| | | sumArApOpen | 直访 | period.orgId | `:226-240 :233` | §7 SP-4 |
| **mfg** | ErpMfgDashboardBizModel | countByDocStatusIn/countByDocStatus | 直访 | **无** | `:253-265` | 是（IServiceContext 收而不用） |
| | | sumCompletedQtyInRange/computeOnTimeRate/loadCompletedInRange | 直访 | **无** | `:267-304` | 是 |
| | | getWorkOrderStatusDistribution | 直访(ormTemplate) | **无** | `:94-115` | 是（GROUP BY docStatus 无 orgId） |
| | | findDelayedWorkOrderAlert | 直访 | **无** | `:148-173` | 是 |
| **sal** | ErpSalDashboardBizModel | loadPostedInvoicesInRange | 直访 | **无** | `:213-220` | 是 |
| | | countActiveOrders | 直访 | **无** | `:222-227` | 是 |
| | | findCustomerTopN | 直访(ormTemplate) | **无** | `:123-164` | 是（GROUP BY customerId 无 orgId） |
| | | sumArApOpen / findArOverdueAlert | **IBiz** | transformer 覆盖 | `:182,229` arApItemBiz.findOpenItems | **否**（CrudBizModel 管道） |
| **ast** | ErpAstDashboardBizModel | loadInServiceAssets | 直访 | **无** | `:176-181`（仅 status=IN_SERVICE） | **是**（A1.24 §5 已证实：IServiceContext 收而不用） |
| | | sumPeriodDepreciation/sumCipBalance/loadExecutedSchedulesInRange/loadAssetIdsWithExecutedDepreciationInPeriod | 直访 | **无** | `:183-223` | 是 |
| **inv** | ErpInvDashboardBizModel | sumBalanceTotalCost | 直访(ormTemplate) | **无** | `:444-456` | **是**（GROUP BY warehouseId 无 orgId，P1-MA2-093 原例） |
| | | findWarehouseDistribution | 直访(ormTemplate) | **无** | `:132-164` | **是**（P1-MA2-093 原例：跨组织仓库总额） |
| | | sumMoveQtyInRange/loadDoneMovesInRange/loadLedgersInRange/findShortageAlert/findSlowMovingAlert/findBatchExpiryAlert | 直访 | **无** | `:166-291,296-437` | 是 |
| **qa** | ErpQaDashboardBizModel | loadInspectionsInRange/countOpenNcrs/findDefectTopN | 直访 | **无** | `:67-160,285-367` | 是（IServiceContext 收而不用，A1.33 证实） |
| | | countOutOfControlCharts/countInadequateCapabilityCharts/countOpenSpcNcrs/findCapaOverdueAlert | 直访 | **无** | `:285-350` | 是 |
| **prj** | ErpPrjDashboardBizModel | loadOpenProjects/sumBudgetForProjects/sumCostForProjects | 直访 | **无** | `:209-264` | 是 |
| | | getProjectStatusDistribution/findCostOverrunAlert/findDelayedProjectAlert/getProjectGrossMargin | 直访 | **无** | `:85-205` | 是 |
| **md** | ErpMdDashboardBizModel | getDashboardKpi/findMaterialWithoutSkuAlert/findSkuWithoutPriceAlert | 直访 | **N/A** | `:47-125` | **否**（ErpMdMaterial/ErpMdPartner/ErpMdMaterialSku 无 orgId 列，组织无关共享主数据，看板 org-agnostic 设计正确） |
| **mnt** | ErpMntDashboardBizModel | countEquipmentNotDecommissioned/countEquipmentByStatus/countRequestsByStatus/countCompletedVisitsInRange | 直访 | **无** | `:168-235` | 是（IServiceContext 收而不用，A1.44 证实） |
| | | getEquipmentStatusDistribution/findEquipmentDowntimeAlert/findMaintenanceOverdueAlert | 直访 | **无** | `:86-164` | 是 |
| **cs** | ErpCsQualityDashboardBizModel | loadClosedTickets/getDashboardKpi/getTeamSlaRanking/getAgentCsatBreakdown | 直访 | **无** | `:66-326` | 是（dashboards.md 显式 cs 产品基线外，看板为后续范围） |
| **pur** | ErpPurDashboardBizModel | loadActiveInvoicesInRange/countActiveOrders/findVendorTopN/computeOnTimeRate | 直访 | **无** | `:245-305` | 是 |
| | | sumArApOpen / findApOverdueAlert | **IBiz** | transformer 覆盖 | `:218,261` arApItemBiz.findOpenItems | **否**（CrudBizModel 管道） |

**census 汇总**：

- **11 域全集普查完成**，无遗漏（glob `**/dashboard/*DashboardBizModel.java` = 11 文件，与本计划 Current Baseline 列出的 11 文件逐一匹配）。
- **直访路径（绕过 transformer）**：10 域（finance/mfg/sal/ast/inv/qa/prj/mnt/cs/pur）；master-data 为组织无关设计。
- **IBiz 路径（transformer 覆盖）**：sal + pur 的 AR/AP 余额/超期预警经 `IErpFinArApItemBiz.findOpenItems`（CrudBizModel 管道，transformer 注入）—— 此部分**安全**。
- **零 orgId scope 直访方法**：mfg/ast/inv/qa/prj/mnt/cs/pur 全集 + sal invoice/order/topN + finance sumBankBalance/findLatestPeriodId = **绝大多数看板聚合方法**。
- **period.orgId 手工补 scope（§7 SP-4 存疑点）**：finance loadGlBalances/loadGlBalancesInRange/sumArApOpen（orgId 源自期间组织非登录用户组织——用户可传 orgB 期间看 orgB GL 余额/AR/AP）。
- **IServiceContext 收而不用模式**：mfg/qa/mnt 全文 `IServiceContext context` 参数传入但零引用（A1.24/A1.33/A1.44 已分别证实）。

---

## §2 R1.29 ErpOrgIsolationQueryTransformer 覆盖面核验

### 2.1 注册/注入点证据

| 证据项 | file:line | 内容 |
|---|---|---|
| IoC bean 注册 | `module-common-service/src/main/resources/_vfs/erp/common/beans/app-service.beans.xml:11-12` | `<bean id="nopGlobalQueryTransformer" class="app.erp.common.org.ErpOrgIsolationQueryTransformer"/>` |
| 覆盖平台默认 | `app-service.beans.xml:9-10` | 「覆盖平台 nop/biz/beans/biz-defaults.beans.xml 的 EmptyQueryTransformer 默认（同 id 非 default 覆盖 default），CrudBizModel 经 @Named("nopGlobalQueryTransformer") 自动注入并应用于全部 findPage/findList」 |
| transform 方法 | `ErpOrgIsolationQueryTransformer.java:45-62` | 签名 `transform(QueryBean filter, String authObjName, String action, IBizObject bizObj, IServiceContext context)` —— `authObjName`/`action`/`bizObj` 是 CrudBizModel 层概念，证实由 CrudBizModel 调用非 DAO 层 |
| config-gate | `ErpOrgContext.java:23-26,39-40` | `isActive(context) = isIsolationEnabled() && currentOrgId != null`；`isIsolationEnabled` = `erp.multi-company.org-isolation-enabled` 默认 **false** |

### 2.2 覆盖范围裁决

**R1.29 transformer 覆盖 = CrudBizModel 管道（findPage/findList/IBiz 接口）**：

- 平台 `CrudBizModel` 经 `@Named("nopGlobalQueryTransformer")` 注入 IQueryTransformer，在 `prepareFindPageQuery`/`appendDataAuthFilter` 中调用 `transformer.transform(...)` 追加 `eq("orgId", currentOrgId)`。
- 经 IBiz 接口调用（如 `arApItemBiz.findOpenItems` / `arApItemBiz.findList`）走 CrudBizModel 管道 → transformer 注入 → 隔离生效。
- **运行时证实**：`TestErpOrgIsolation.testReadIsolationFiltersOtherOrg:59-80` —— `org-isolation-enabled=true` + ctx orgId=2 时，`arApItemBiz.findList(query, null, ctx)` 查 orgId=3 数据断言 `isEmpty()`（隔离生效）；关闭后回归可见。

**R1.29 transformer 不覆盖 = 直访路径（IDaoProvider/IOrmTemplate）**：

- 看板 BizModel 经 `daoProvider.daoFor(X).findAllByQuery(q)` / `ormTemplate.findListByQuery(q)` / `dao.countByQuery(q)` 直访 DAO/OrmTemplate 层，**不经 CrudBizModel** → transformer 不调用 → orgId 不注入。
- **transformer Javadoc 显式 Non-Goal**（`ErpOrgIsolationQueryTransformer.java:30-31` 逐字）：

  > 「Non-Goal：dashboard/report BizModel 经 `IDaoProvider` 直访绕过 CrudBizModel 管道，**不被本 transformer 覆盖**（归 P1-MA1-022 读侧豁免 + 各域查询方法显式补 filter，如 P1-MA2-095）。」

- R1.29 plan Non-Goal（`docs/plans/2026-07-30-0841-3-r1-29-...md:47`）逐字：「不改 dashboard BizModel 的 IDaoProvider 直访模式（归 P1-MA1-022 读侧豁免裁决 + 093 transformer 透明覆盖）」——**注意**：plan 文字假设「093 transformer 透明覆盖」与 transformer 实际 Javadoc「不覆盖直访」存在措辞张力；以**实现层 Javadoc（不覆盖）为准**（实现是真相，plan 措辞为设计期假设）。本验证正是闭合此措辞张力。

**结论**：R1.29 修复声明的覆盖面 = CrudBizModel 管道；看板直访路径在其覆盖范围**外**。R1.29 覆盖面与 DashboardBizModel 查询路径**不对齐**（直访路径占比绝大多数）。

---

## §3 直访路径 orgId scope 消费核验（本存疑点核心）

### 3.1 零 orgId 直访方法（泄漏面）

见 §1 census 矩阵。**绝大多数看板聚合方法零 orgId scope**：

- **mfg/ast/inv/qa/prj/mnt/cs/pur** 全集查询方法零 orgId（仅 status/date/docStatus 过滤）。
- **sal** loadPostedInvoicesInRange/countActiveOrders/findCustomerTopN 零 orgId（AR 余额经 IBiz 安全）。
- **finance** sumBankBalance（`:214-224` 仅 accountType=BANK，P1-MA2-093 原例「汇总所有组织银行账户」）/ findLatestPeriodId（`:181-188` 仅 orderField+limit，取最近期间不限组织）零 orgId。

**泄漏机理**：多组织部署 + `org-isolation-enabled=true` + 用户 ctx orgId=2 → 直访 `findAllByQuery(q)` 不经 transformer → orgId=3 数据混入聚合结果（如 inv findWarehouseDistribution 返回 orgId=3 仓库总额；ast loadInServiceAssets 返回 orgId=3 资产原值；finance sumBankBalance 汇总 orgId=3 银行账户余额）。

### 3.2 period.orgId 手工补 scope（§7 SP-4 存疑点）

finance loadGlBalances（`:161-179 :172,177` applyOrgAndSchemaScope）/ loadGlBalancesInRange（`:190-212 :203-209`）/ sumArApOpen（`:226-240 :233`）手工补 orgId，但**orgId 源自 `period.orgId`（期间所属组织）非登录用户组织**：

- `resolvePeriodOrgId:245-255` 从 `ErpFinAccountingPeriod.getOrgId()` 解析 orgId。
- 用户归属 orgA 但传 orgB 的 periodId → 查询按 orgB 过滤 → 返回 orgB 的 GL 余额/AR/AP（**§7 SP-4 原文存疑点逐字命中**：「scope 按期间所属组织非登录用户组织」）。
- 此为 R1.29 plan P1-MA2-095 修复的 finance 11 处查询方法补 acctSchemaId+orgId 范式（orgId 从期间解析，保护单组织基线零回归）——**该修复使多账套部署不双计，但 orgId 源是数据侧（period.orgId）非用户侧（currentOrgId），故跨组织用户场景仍按期间组织聚合**。

### 3.3 IServiceContext 收而不用模式

mfg/qa/mnt（及全域 dashboard 方法签名）`IServiceContext context` 参数传入但全文零引用（grep `context\.` 在聚合方法体内 = 0）——证实看板方法不消费用户上下文 orgId。A1.24 §5（`loadInServiceAssets`）/ A1.33 / A1.44 已分别证实。

### 3.4 master-data 例外（组织无关设计）

ErpMdMaterial（`app-erp-master-data.orm.xml:188-230`）/ ErpMdPartner（`:422-451`）/ ErpMdMaterialSku 全列普查**无 orgId 列**——主数据（物料/往来单位）为跨组织共享设计（`dashboards.md 主数据看板` 显式「主数据看板偏数据治理，指标少且静态」）。ErpMdDashboardBizModel 直访无 orgId scope **正确**（无可过滤的 orgId 列）。本域不构成泄漏面。

### 3.5 AR/AP 经 IBiz（安全子集）

sal `sumArApOpen`/`findArOverdueAlert`（`:182,229` arApItemBiz.findOpenItems）+ pur `sumArApOpen`/`findApOverdueAlert`（`:218,261`）经 `IErpFinArApItemBiz` IBiz 接口 → CrudBizModel 管道 → transformer 注入 → **隔离生效（安全）**。此子集经 `TestErpOrgIsolation` 证实（`arApItemBiz.findList` 隔离生效）。

---

## §4 seed/部署多组织配置普查

### 4.1 seed orgId 分布

- **生产/demo seed**：单组织基线（orgId=2，P1-MA2-093 finding「种子 176 行业务单据全部 orgId=2」已证实）。
- **R1.29 测试 seed**：`TestErpOrgIsolation`（`module-finance/erp-fin-service/src/test/.../org/TestErpOrgIsolation.java`）在测试内动态 seed orgId=3 数据（`:63 seedArApItem(6001L, 3L, ...)`）作负向隔离测试输入——**仅测试范围**，非生产/demo seed。
- **结论**：生产/demo seed 单组织（orgId=2），**单组织种子掩盖跨组织泄漏**（A2.18 `:32` 已证实的模式，本验证复核确认）。无 orgId=3 生产数据可泄漏给 orgId=2 用户。

### 4.2 部署文档多组织看板行为

- config-gate `erp.multi-company.org-isolation-enabled` 默认 **false**（`ErpOrgIsolationConstants.java:21` + `ErpOrgContext.java:23-26`）。
- 多组织部署启用隔离须人工翻转 config + 设置 `erp.currentOrgId` 上下文（R1.29 plan Deferred But Adjudicated「successor 多组织部署启用」）。
- `dashboards.md §设计原则 4`（`:12`）声明「看板数据受行级权限约束（用户只看自己组织/部门/成本中心的数据）」——**声明-落地差距**：L2 声明行级权限自动注入，但看板直访路径绕过 transformer，声明能力在多组织部署下不生效。

### 4.3 掩盖结论

单组织种子（orgId=2）+ 隔离默认关闭 → **当前基线无活跃跨组织泄漏**。泄漏**仅在 successor 条件（多组织部署 + 隔离开启）显现**——与 P1-MA2-093 successor 触发条件一致。

---

## §5 MA4 ↔ A5.6 边界声明

- **本验证（MA4）**：审「行为是否符合需求」——看板是否跨组织泄漏（orgId 行级权限维度运行时行为）。
- **A5.6**：审「E2E 断言强度」——看板 E2E 是否断言跨组织隔离。
- **边界执行**：本验证**不重做** A5.6 E2E 断言强度审计（仅评估 orgId 行级权限泄漏面，不评估 E2E 断言是否覆盖跨组织场景）。按此边界执行。

---

## §6 运行时裁决（§2 判据 + L1/L2/L3 三源 + P1-MA2-093 分层一致）

### 6.1 §2 判据命中

| §2 判据 | 命中 | 理由 |
|---|---|---|
| §2 P0① 活跃数据破坏 | **否** | config-gated 默认关闭 + 单组织种子 → 无活跃跨组织数据腐败 |
| §2 P0④ 会计过账正确性破坏 | **否** | 看板是读侧聚合，不涉 GL 写入/过账 |
| §2 P1① 主验收标准行为实质偏离 | **否** | 主路径[单组织基线/默认关闭]OK；泄漏仅在 successor 条件[多组织部署+隔离开启]显现，非当前活跃路径 |
| §2 P1⑤ 测试断言完全缺失 | **否** | `TestErpOrgIsolation` 覆盖 CrudBizModel 路径隔离（虽不覆盖直访，但隔离能力有测试） |
| **§2 P2① 次要验收标准未完全满足** | **是** | 主路径[单组织基线/默认关闭]OK，边界[多组织部署直访路径 orgId scope 缺失]弱 |

### 6.2 L1/L2/L3 三源对照

| 源 | 内容 | 与运行时对齐 |
|---|---|---|
| **L1 需求** | 各域 use-cases.md 看板类 UC（finance UC-FIN-17、mfg UC-MFG-11、sal UC-SAL-12 等）要求看板数据反映本组织/授权范围数据 | 看板 UC 验收标准未显式区分单/多组织部署；多组织行级权限是 owner doc 层语义 |
| **L2 设计** | `dashboards.md §设计原则 4:12`「看板数据受行级权限约束（用户只看自己组织/部门/成本中心的数据）」+ `multi-company.md` 多公司隔离声明 | **声明-落地差距**：L2 声明行级权限自动注入，看板直访路径绕过 transformer，多组织部署下不生效 |
| **L3 实现** | 10/11 域直访路径零 orgId scope（master-data 组织无关例外）；R1.29 transformer 不覆盖直访路径 | 与 L2 声明在单组织基线巧合吻合（无组织可隔离），多组织部署下差距显现 |

### 6.3 与 P1-MA2-093[R1.29 resolved] 分层一致

- **P1-MA2-093**（arm-index:550）= 「orgId 查询隔离全仓未落地」broad finding，**已 ✅ resolved R1.29**（ErpOrgContext + ErpOrgIsolationOrmInterceptor + 全局 ErpOrgIsolationQueryTransformer 落地 config-gated 默认关闭）。finding 文字 (d) 项显式列「11 dashboard 直访绕过认证管道」作为 finding 的一部分。
- **R1.29 修复范围** = CrudBizModel 管道（读）+ PrepareSave 拦截器（写），config-gated。R1.29 plan Non-Goal 显式「不改 dashboard BizModel 的 IDaoProvider 直访模式」。
- **本验证裁决**：**维持 P1-MA2-093 resolved R1.29**（隔离能力已实现，config-gated 默认关闭，successor[多组织部署启用]触发条件未满足——与 RC MA3 复查 2026-08-07 `arm-index:550` 裁决一致「finding 机制已实现 ≠ successor 部署侧已验证」）。看板直访路径 orgId scope 缺失是 R1.29 transformer 非覆盖的**已知残留**（transformer Javadoc 显式登记），**非 P1-MA2-093 重开**（重开须 successor 条件满足后泄漏在活跃路径，当前默认关闭非活跃）。
- **新建 P2-RC-086**：看板直访路径 orgId scope 缺失 watch-only（§7 详述），归 successor 多组织部署启用时修复。

### 6.4 决策树分支命中

依 plan Phase 1 Decision 选项：

- 选项①「R1.29 全覆盖或直访路径手工追加 orgId scope → 维持 resolved 无新 finding」——**不成立**（R1.29 不覆盖直访 + 绝大多数直访方法零 orgId scope）。
- 选项①子项「有无 orgId scope 的直访路径但实操单组织不泄漏 → 登记 P2 watch-only」——**命中**（直访路径零 orgId scope + 单组织基线默认关闭不泄漏）。
- 选项②「无 orgId scope 的直访路径且多组织部署致泄漏 → 登记 P1」——**不命中**（多组织部署是 successor 条件未满足，非当前活跃路径；config-gated 默认关闭使泄漏非活跃）。

**裁决 = 选项①子项 = 维持 P1-MA2-093 resolved R1.29 + 新建 P2-RC-086 watch-only**。

---

## §7 finding 衔接

### 7.1 新建 P2-RC-086（看板直访路径 orgId scope 缺失 watch-only）

- **控制点**：DashboardBizModel 经 IDaoProvider/IOrmTemplate 直访绕过 ErpOrgIsolationQueryTransformer，零 orgId scope（10 域，master-data 组织无关例外）+ finance period.orgId 源非用户组织（§7 SP-4）。
- **与 P1-MA2-093 关系**：**互补非重复**——P1-MA2-093 = broad orgId 查询隔离能力（resolved R1.29 覆盖 CrudBizModel 管道）；P2-RC-086 = R1.29 transformer 非覆盖的看板直访路径残留（不同控制点：CrudBizModel 自动注入 vs 直访手工补 filter）。P1-MA2-093 resolved 维持，P2-RC-086 登记残留。
- **去重核验**：grep arm-index 同域同控制点——P1-MA2-093 (d) 项虽列「11 dashboard 直访」但作为 broad finding 的子证据（非独立控制点 finding）；无独立「看板直访 orgId scope」P2 finding。P2-RC-086 是首次将此残留登记为独立 watch-only finding（非重复 P1-MA2-093）。
- **分级**：§2 P2①（次要验收标准未完全满足——主路径[单组织基线/默认关闭]OK，边界[多组织部署直访路径]弱）。
- **修复通道**：successor watch-only（P2 登记不强制）。修复 = 各 DashboardBizModel 直访方法手工补 `eq("orgId", ErpOrgContext.currentOrgId(context))`（消费 IServiceContext，改「收而不用」为「消费」）或 finance 范式 `applyOrgAndSchemaScope` 但 orgId 源改为用户上下文非 period.orgId。**纯 BizModel 代码逻辑（看板读侧过滤，非会计过账核心路径），按 roadmap 预授权类目可自动执行，不触 §5 ask-first**。触发条件 = 多组织部署启用（P1-MA2-093 successor 同条件）。
- **owner doc 对齐**：`dashboards.md §设计原则 4:12` 声明行级权限约束须在多组织部署时落地——P2-RC-086 记录当前看板直访路径未落地此声明，successor 修复时对齐。

### 7.2 P1-MA2-093 arm-index 注记更新

P1-MA2-093（arm-index:550）维持 ✅ resolved R1.29，追加本验证（A4.1.25 + A4.2.10）交叉引用注记：R1.29 transformer 覆盖面 = CrudBizModel 管道，看板直访路径残留登记 P2-RC-086 watch-only。

---

## §8 过程纪律自检

### 8.1 compliance-checker actual vs baseline

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，无生产代码变更故无回归风险）：

| 规则 | 描述 | actual | compliance-baseline.md | 状态 |
|---|---|---|---|---|
| R1a | dao().saveEntity (BizModel) 🔴高 | 0 | 0 | 一致 |
| R1b | dao().updateEntity (BizModel) 🔴高 | 0 | 0 | 一致 |
| R1c | dao().getEntityById (BizModel) 🔴高 | 0 | 0 | 一致 |
| R1d | dao().findAllByQuery (BizModel) 🔴高 | 14 | 14 | 一致（看板直访即此 14 站点的子集，本验证记录非新增） |
| R2a | BizModel daoFor(ErpMd*) 🔴高 | 34 | 34 | 一致 |
| R2b | BizModel daoFor(Erp*) 跨域 🔴高 | 229 | 240（历次重构漂移） | 漂移系既有重构（非本验证引入） |

**结论**：高严重度反模式 R1a-c=0（零命中）；R1d=14 看板直访即此规则记录的既有站点（本验证是只读评估，零生产代码变更，**无回归风险**）。不以 checker 退出码 0 作为门控依据（本计划 Closure Gates 已删除 build/test 门控，因无代码变更）。

### 8.2 closure-audit 独立性声明

本验证报告由执行代理起草；结束审计须由独立子代理（新会话）执行，执行者未自我审计。

### 8.3 与 arm-index 交叉去重声明

- **复用 P1-MA2-093**（resolved R1.29）：本验证不重开 P1-MA2-093（successor 条件未满足 + config-gated 默认关闭非活跃），仅复核 R1.29 覆盖面差异。
- **复用 A1.7 §2.4**（finance 看板 orgId scope + AcctSchemaResolver 消费）：本验证 HEAD 复核证实 finance `applyOrgAndSchemaScope:257-267` 仍按 period.orgId（非用户组织）。
- **复用 A1.24 §5 loadInServiceAssets**（IServiceContext 收而不用）：本验证 HEAD 复核证实 `ErpAstDashboardBizModel.loadInServiceAssets:176-181` 仍仅 status=IN_SERVICE 零 orgId。
- **MA4 ↔ A5.6 边界**：本验证审行为符合性（§5），不重做 A5.6 E2E 断言强度。
- **新建 P2-RC-086**：经 grep arm-index 同域同控制点去重后确认非重复（§7.1），首次将看板直访 orgId scope 残留登记为独立 watch-only finding。

---

## §9 范围内验证完成确认

- [x] 11 域 DashboardBizModel 全集普查完成（census 矩阵 §1，每条有 file:line 证据）
- [x] R1.29 ErpOrgIsolationQueryTransformer 覆盖面核验完成（§2，注册点 + Non-Goal Javadoc + TestErpOrgIsolation 证实）
- [x] 直访路径 orgId scope 消费核验完成（§3，泄漏面 + §7 SP-4 period.orgId + IServiceContext 收而不用 + master-data 例外 + AR/AP IBiz 安全）
- [x] seed/部署多组织配置普查完成（§4，单组织种子掩盖 + config-gate 默认关闭）
- [x] MA4 ↔ A5.6 边界声明（§5）
- [x] 运行时裁决（§6，§2 判据 + L1/L2/L3 + P1-MA2-093 分层 + 决策树分支）
- [x] finding 衔接（§7，新建 P2-RC-086 + P1-MA2-093 注记）
- [x] §8 过程纪律自检（checker actual vs baseline + 独立性 + 交叉去重）
