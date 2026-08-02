# RC MA1 A1.7 finance-F7 报表/看板/多账套 需求-实现符合性五级追踪审计报告

> 里程碑：MA1（RC） | 切片：A1.7 | 域：finance
> 工作项：A1.7（UC-FIN-05 多账套并行过账 + UC-FIN-16 财务三大报表 + UC-FIN-17 财务看板，共 3 UC / 12 验收标准）
> 来源：`docs/backlog/requirement-compliance-roadmap.md` Work Item A1.7
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域 / §6 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §10 MR0/MR1 / §去重协议）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）
> 审计类型：**只读审计**（读代码/测试/报告；不改代码/ORM/api.xml/真相源）
> L1 真相源（权威）：`docs/design/finance/use-cases.md`（UC-FIN-05 `:93` / UC-FIN-16 `:318` / UC-FIN-17 `:350`）
> L2 owner doc（设计参考，冲突以 L1 为准）：`docs/design/finance/multiple-accounting-schemas.md` / `docs/design/dashboards.md` / `docs/design/finance/posting.md`
> HEAD：本报告 L3/L4 证据核验时 HEAD = `c1b775491`（与 A1.6 同 HEAD 段，2026-08-02）

---

## 9. 与既有 MA2 报告差异增量声明（前置，§6 段落 9）

> 方法论 §去重协议 + §6 段落 9：报告开头声明与既有 MA2 报告的差异增量。

本切片（报表/看板/多账套）为**查询/读面**，**无专属 MA2 状态机审计报告**（状态机类审计不覆盖纯查询面）。既有证据来源 + 本切片差异增量：

| 既有证据 | 已证实结论（本切片复用） | 本切片差异增量（需求契约视角，仅本切片补） |
|---------|------------------------|------------------------------------------|
| `docs/audits/2026-07-06-use-case-implementation-audit.md:112`（UC-FIN-05 🔶 "无凭证/余额隔离测试"） | UC-FIN-05 多账套传播机制设计存在（multiple-accounting-schemas.md） | **HEAD 复核**：🔶 已闭环（`TestErpFinMultiSchemaPosting` + `TestErpFinMultiSchemaReportIsolation` 已落地，§3/§4 详） |
| `docs/audits/2026-07-06-use-case-implementation-audit.md:123-124`（UC-FIN-16 ✅ / UC-FIN-17 ✅） | 三大报表五张渲染 + 看板 KPI 聚合 + AMIS 页面存在 | **需求契约↔行为差异**：UC-FIN-16 现金流量分类(经营/投资/筹资)+间接法缺失 + CLOSED 期间门控未强制；UC-FIN-17 行级权限未落地（复用 P1-MA2-093） |
| `docs/audits/2026-07-28-1510-arm-ma2-multi-company-isolation.md`（A2.18，P1-MA2-093/095） | 报表/看板读路径原省略 acctSchemaId/orgId；P1-MA2-095 已由 R1.29 fix（`applyOrgAndSchemaScope` 注入） | **HEAD 复核**：P1-MA2-095 读路径双计已修复（period.orgId + 主账套 scope，§2.4/§4 详）；P1-MA2-093 行级权限（用户维度）仍 open，本切片 UC-FIN-17 ⑫ 投影复用 |
| `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md:306`（A1.2 caveat ③） | COMMITMENT 影子凭证 header 借贷不经 assertBalanced；交本切片 UC-FIN-16 复核是否破坏试算平衡/三表恒等式 | **交叉复核结论**：BS/IS 安全（BUDGET/COMMITMENT 不入 GlBalance，orm.xml:1740-1742 注记）；cash flow 读 VoucherLine 不过滤 postingType 但实操不触现金科目（§7 静态存疑点 SP-1） |

本切片不复跑 MA2 状态机/业财链路行为审计，只补"需求契约↔实际行为"差异（§去重协议）。

---

## 1. 需求契约原文（L1 逐字引用，禁止转述）

> 方法论 §1 L1 格式：`<use-cases.md>:<line>` + 验收标准原文块引用。

### UC-FIN-05 多账套并行过账（`docs/design/finance/use-cases.md:93`）

**场景**：同一业务在财务账与管理账(或税务账)各生成一组凭证。

**可验证断言**（逐字引用 `use-cases.md:98-104`）：
```
业务单据.审核 →
  对每个启用的 AcctSchema 各生成一组凭证
  每组凭证.acctSchemaId 不同, 科目映射不同(同业务不同科目)
所有组凭证.posted == true
GlBalance 按 acctSchemaId 隔离(各账套余额独立)
```

**涉及机制**：multiple-accounting-schemas.md、posting.md §多套科目表

### UC-FIN-16 财务三大报表（`docs/design/finance/use-cases.md:318`）

**场景**：基于科目余额生成资产负债表/利润表/现金流量表。

**可验证断言**（逐字引用 `use-cases.md:323-340`）：
```
// 数据来源
三表基于 ErpFinGlBalance(按 subject×period×维度 聚合的余额)

// 资产负债表(恒等式)
资产合计 == 负债 + 所有者权益

// 利润表
收入 - 成本 - 费用 = 净利润
净利润结转至资产负债表"未分配利润"

// 现金流量表
按科目现金流分类(经营/投资/筹资)调整
间接法: 净利润 + 非现金项目 + 营运资金变动

// 期间控制
报表基于已 CLOSED 期间的 GlBalance(未结账期间数据不完整)
```

**涉及机制**：ErpFinGlBalance、period-close.md、多账套(每账套独立三表)

### UC-FIN-17 财务看板（`docs/design/finance/use-cases.md:350`）

**场景**：财务看板的指标展示与异常预警。见 ../dashboards.md §财务看板。

**可验证断言**（逐字引用 `use-cases.md:355-365`）：
```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  收入/支出/净利润, 银行余额, 收支趋势, 预算执行率, 现金流预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

**涉及机制**：../dashboards.md、各域 state-machine.md、roles-and-permissions.md(行级权限)

---

## 2. 实现证据（L3 代码路径，含跨域调用链）

> 方法论 §1 L3 格式：`<file>:<line>`（必须含行号）。HEAD = `c1b775491`。

### 2.1 UC-FIN-05 多账套并行过账 — 写路径传播链

| 验收标准 | L3 代码路径（file:line） | 调用链 |
|---------|------------------------|--------|
| ① 对每个启用的 AcctSchema 各生成一组凭证 | `module-finance/erp-fin-service/.../posting/SchemaPropagator.java:44-81`（`resolveTargetSchemas`） | `ErpFinPostingProcessor.process` → `resolveTargetSchemas(orgId, primarySchemaId)` → 按 `multi-schema-enabled` + `isPropagate` 展开目标账套列表（主账套排首） |
| ② 每组凭证.acctSchemaId 不同 | `ErpFinPostingProcessor.process:131,161-188`（账套循环，每账套一张凭证）+ `persistVoucher:785`（`voucher.setAcctSchemaId(acctSchemaId)`）+ `:820`（`line.setAcctSchemaId(acctSchemaId)`） | 每目标账套 → 独立凭证 + 分录行 stamp `acctSchemaId`（mandatory `app-erp-finance.orm.xml:422/489`） |
| ② GlBalance 按 acctSchemaId 隔离（写） | `ErpFinGlBalance.acctSchemaId` mandatory（`app-erp-finance.orm.xml:910`）+ `ErpFinAccountingPeriodProcessor.populateTrialBalanceForAllSchemas:464-516`（按 acctSchemaId 分组 `:481-492` + `tb.setAcctSchemaId(acctSchemaId)` `:500`） | 过账引擎维护 GlBalance，每账套独立余额行 |
| ③ 科目映射不同 | CoA 映射 `ErpMdAcctSchemaCoa`（按 acctSchemaId FK）+ 无映射时回退源科目（`TestErpFinMultiSchemaPosting#testSecondaryVoucherFallsBackToSourceSubjectWhenNoMapping:132-154` 证实回退 + 借贷平衡保持） | 模板查找 → CoA 映射 → 回退源科目 |
| ④ 所有组凭证.posted == true | `ErpFinPostingProcessor.process` 账套循环内每张凭证走完整 `persistVoucher` + `docStatus=POSTED` | 每账套凭证独立 POSTED |

### 2.2 UC-FIN-16 财务三大报表 — 数据集查询链

| 验收标准 | L3 代码路径（file:line） | 备注 |
|---------|------------------------|------|
| ⑤ 三表数据来源 | `module-finance/erp-fin-service/.../report/ErpFinReportBizModel.java`：资产负债 `buildBalanceSheetDataset:269` / 利润表 `buildIncomeStatementDataset:284` → **均读 `ErpFinGlBalance`**（`loadGlBalances:386-413`）；**现金流量 `buildCashFlowDataset:299` → 读 `ErpFinVoucherLine`**（`loadPostedVoucherLines:424-439`，非 GlBalance） | **偏差**：现金流量表用 VoucherLine（交易级）非 GlBalance（余额级）。L1 字面"三表基于 ErpFinGlBalance"。设计上合理（现金流需交易级现金移动，GlBalance 期间聚合不直接提供），但字面偏离 L1 |
| ⑥ 资产负债恒等式 | `buildBalanceSheetDataset:269-282` 按 subjectClass 分 ASSET/LIABILITY/EQUITY 段 + `balanceAmount:532-538`（借方科目 closingDebit-closingCredit / 贷方科目 closingCredit-closingDebit） | 恒等式由过账引擎 `assertBalanced`（双式记账）结构保证；报表只算分段合计，不断言恒等式（恒等式是平衡账簿的属性） |
| ⑦ 利润表 收入-成本-费用=净利润 | `buildIncomeStatementDataset:284-297` 损益类（INCOME/EXPENSE/COST）本期发生净额 `periodActivity:541-547`；净利润 = 收入-支出 见看板 `ErpFinDashboardBizModel.getDashboardKpi:79` | 报表展示损益类发生额；"结转至未分配利润"是期间结账机制（UC-FIN-06/07，`ProfitLossClosingService`），非报表本身职责 |
| ⑧ 现金流量分类(经营/投资/筹资) + 间接法 | `buildCashFlowDataset:299-323`：现金科目判定 `isCashSubjectCode:549-553`（1001/1002/1012/1031 前缀）→ **`r.put("section", "OPERATING"):314` 硬编码全部为 OPERATING**；无非现金科目分类、无投资/筹资分类、无间接法（净利润+非现金项目+营运资金变动） | **FAIL**：2/3 分类缺失 + 间接法完全缺失；`ErpMdSubject` ORM（`app-erp-master-data.orm.xml:901-932`）**无 cashFlowType/cashFlowClass 字段**，数据模型不支持分类 |
| CLOSED 期间门控 | `loadGlBalances:386-413` 仅按 `periodId` 过滤（`:396/402` `eq("periodId", ...)`），**不校验 `period.status==CLOSED`** | **FAIL (soft)**：OPEN 期间亦可渲染；L1 "报表基于已 CLOSED 期间"未强制 |
| ⑨ BUDGET/COMMITMENT 影子凭证过滤 | GlBalance 查询（BS/IS 数据源）无 postingType 过滤；但 `app-erp-finance.orm.xml:1740-1742` 注记「过账引擎本就不维护 ErpFinGlBalance，故预算不引入 GlBalance 结构变更」+「预算余额/实际余额从 ErpFinVoucherLine 派生不落库」 | BS/IS **安全**（BUDGET/COMMITMENT 不入 GlBalance）；cash flow 读 VoucherLine 不过滤 postingType 但实操不触现金科目 |

### 2.3 UC-FIN-17 财务看板 — KPI 聚合链

| 验收标准 | L3 代码路径（file:line） | 备注 |
|---------|------------------------|------|
| ⑩ KPI 实时聚合非硬编码 | `module-finance/erp-fin-service/.../dashboard/ErpFinDashboardBizModel.java:57-85`（`getDashboardKpi`）：revenue/expense 从 `ErpFinGlBalance` 损益类本期发生额聚合（`:64-75` `periodActivity`）+ bankBalance 从 `ErpFinFundAccount` Σ（`sumBankBalance:214-224`）+ ar/apBalance 从 `ErpFinArApItem` OPEN+PARTIAL Σ（`sumArApOpen:226-240`）+ 趋势 `getDashboardTrend:87-129`（按月聚合） | **PASS**：全部实时聚合自实体，零硬编码数值；`getDashboardKpi`/`getDashboardTrend` 命名对齐 `dashboards.md §实现约定:238` |
| ⑪ 预警阈值配置化非硬编码 | `findCashFlowAlert:136-157`：`threshold = AppConfig.var(CONFIG_DASH_FIN_CASH_FLOW_THRESHOLD, DEFAULT_DASH_FIN_CASH_FLOW_THRESHOLD):137-139` + threshold≤0 关闭（`:140-142`）+ bank<threshold 触发（`:145-156`） | **PASS**：阈值经 `AppConfig.var` 读系统配置（`erp-dash.fin-cash-flow-threshold`），非硬编码；对齐 `dashboards.md §实现约定:242`「阈值放系统配置 NopSysVariable 非硬编码」 |
| ⑫ 行级权限约束（只看自己组织/部门/成本中心） | `getDashboardKpi:57-85` 接收 `IServiceContext context` 但**未用于权限过滤**；`applyOrgAndSchemaScope:257-267` 按 `period.orgId`（**期间所属组织**，非登录用户组织）+ 主账套过滤 | **FAIL**：按期间所属组织过滤，非按登录用户组织/部门/成本中心；无 `IUserContext.getOrgId()`；`IServiceContext` 收而不用。复用 **P1-MA2-093**（A2.18 orgId 查询隔离全仓未落地，显式列 `ErpFinDashboardBizModel`） |

### 2.4 多账套读路径隔离（A2.18 P1-MA2-095 HEAD 复核）

A2.18（`2026-07-28-1510-arm-ma2-multi-company-isolation.md:194-211`）原列 12 处报表/看板查询省略 acctSchemaId/orgId（P1-MA2-095）。**HEAD 复核**：R1.29 fix（plan `2026-07-30-0841-3-r1-29`）已注入 `applyOrgAndSchemaScope`（ReportBizModel `:472-508` / DashboardBizModel `:242-267`）+ `TestErpFinMultiSchemaReportIsolation` 强断言。

| 站点（A2.18 原列） | HEAD 状态 | 证据 |
|------------------|----------|------|
| `ErpFinReportBizModel.loadGlBalances` | ✅ 已 scope（orgId + 主账套） | `applyOrgAndSchemaScope:489-499` + `TestErpFinMultiSchemaReportIsolation#testBalanceSheetScopedToPrimarySchema:47-73`（1000 not 4000） |
| `ErpFinReportBizModel.loadPostedVoucherLines` | ✅ 已 scope | `applyOrgAndSchemaScope:430` |
| `ErpFinReportBizModel.countBillR` | ✅ 已 scope | `applyOrgAndSchemaScope:461` |
| `ErpFinDashboardBizModel.loadGlBalances` / `loadGlBalancesInRange` / `sumArApOpen` | ✅ 已 scope | `applyOrgAndSchemaScope:257-267` + `loadGlBalancesInRange:203-210` |

**HEAD 复核结论**：P1-MA2-095 读路径双计**已修复**（period.orgId + 主账套 scope；多账套部署取主账套 FINANCIAL 不双计）。**注意**：scope 解析失败时（period.orgId 为空）跳过 filter 保护单组织基线零回归（注释 `:474/:243`）；多账套部署下"每账套独立三表"（L1 UC-FIN-16 涉及机制「多账套(每账套独立三表)」）仍只取主账套，非按账套切换渲染——属 §2.1 UC-FIN-05 ② 接受范围（GlBalance 物理隔离已落地，读路径取主账套是合理简化）。

---

## 3. 测试证据（L4 断言强度）

> 方法论 §1 L4 格式：`<TestFile>.java#<method>` + 断言强度。HEAD = `c1b775491`。

### UC-FIN-05 测试（`TestErpFinMultiSchemaPosting`，`module-finance/erp-fin-service/src/test/.../posting/`）

| 测试方法 | 断言强度 | 覆盖验收标准 |
|---------|---------|-------------|
| `#testMultiSchemaPropagatesTwoVouchersWithDistinctSchema:80-98` | **强断言**：`assertEquals(2, vouchers.size())` + `hasPrimary && hasSecondary`（acctSchemaId 不同）+ 每张 `assertEquals(VOUCHER_STATUS_POSTED, v.getDocStatus())` | ①②④（每账套一组凭证 + acctSchemaId 不同 + 全部 posted） |
| `#testNonPropagatePrimaryProducesSingleVoucher:101-113` | **强断言**：`isPropagate=false` → `assertEquals(1, vouchers.size())` + 主账套 | ① 边界（不传播分支） |
| `#testMultiSchemaDisabledProducesSingleVoucher:116-129` | **强断言**：`multi-schema-enabled=false` → 1 张主账套凭证（向后兼容） | ① 边界（开关关闭） |
| `#testSecondaryVoucherFallsBackToSourceSubjectWhenNoMapping:132-154` | **强断言**：无 CoA 映射时次账套回退源科目 + `assertEquals(0, totalDebit.compareTo(totalCredit))` 借贷平衡 + 同额 113 + `assertFalse(primary.getId().equals(secondary.getId()))` | ③（科目映射，回退分支） |

### UC-FIN-05 隔离测试（`TestErpFinMultiSchemaReportIsolation`，`module-finance/erp-fin-service/src/test/.../report/`）

| 测试方法 | 断言强度 | 覆盖验收标准 |
|---------|---------|-------------|
| `#testBalanceSheetScopedToPrimarySchema:47-73` | **强断言**：同期间同组织同科目两账套（FINANCIAL 1000 + MANAGEMENT 3000），报表 `assertEquals(0, totalAsset.compareTo(new BigDecimal("1000")))`「仅主账套 1000，不双计为 4000」 | ②（GlBalance 读路径隔离，P1-MA2-095 fix 证据） |

### UC-FIN-16 测试（`TestErpFinReportRendering`，`module-finance/erp-fin-service/src/test/.../report/`）

| 测试方法 | 断言强度 | 覆盖验收标准 | 缺口 |
|---------|---------|-------------|------|
| `#testFiveReportsRenderHtml:102-113` / `#testFiveReportsDownloadXlsxAndPdf:115-135` | **仅冒烟**：`assertNotNull(html)` + 文件存在 | 五张报表渲染管线 | 不断言数据正确性 |
| `#testBalanceSheetDataset:140-149` | **强断言**：assetTotal=700 / liabilityTotal=300 / equityTotal=400（分段精确） | ⑤⑥（数据源 + 分段；测试数据 700=300+400 恰满足恒等式，但不断言恒等式本身） | 不断言 资产==负债+权益 恒等式 |
| `#testIncomeStatementDataset:152-159` | **强断言**：收入 1000 / 费用 600 | ⑤⑦（损益类发生额） | 不断言净利润结转 |
| `#testCashFlowDataset:162-173` | **强断言**：现金流入 80 | ⑤（现金流量数据源，VoucherLine） | **不断言经营/投资/筹资分类**（实现亦无分类，§2.2 ⑧）；不断言间接法 |
| `#testArApAgingBuckets:176-196` / `#testPeriodCloseDataset:199-220` | **强断言**：账龄桶 + 模块状态 + 损益结转/汇兑重估凭证数 | AR-AP 账龄 + 期末结账报告（非 UC-FIN-16 三表核心，附加报表） | — |
| `#testPathInjectionRejected:225-237` | **强断言**：非法 reportName/renderType 抛 NopException | 路径注入防护（安全） | — |
| **缺口** | — | — | **无 CLOSED 期间门控测试**（实现亦无门控，§2.2）；**无 BUDGET/COMMITMENT 过滤测试**（A1.2 caveat ③ 交叉） |

### UC-FIN-17 测试（`TestErpFinDashboard`，`module-finance/erp-fin-service/src/test/.../dashboard/`）

| 测试方法 | 断言强度 | 覆盖验收标准 | 缺口 |
|---------|---------|-------------|------|
| `#testKpiAggregationArithmetic:69-98` | **强断言**：revenue=1000 / expense=300 / netProfit=700 / bankBalance=5000 / arBalance=800 / apBalance=400（精确算术） | ⑩（KPI 实时聚合） | — |
| `#testKpiEmptyDatasetReturnsZeros:58-66` | **强断言**：空数据集退化为 0 | ⑩ 边界（空集） | — |
| `#testTrendMonthlySeries:101-140`（`@EnableSnapshot`） | **强断言**：近 2 月序列长度 + 分月 revenue 精确 | ⑩（收支趋势） | — |
| `#testCashFlowAlertTriggersWhenBelowThreshold:155-172` / `#testCashFlowAlertDisabledByDefault:143-152` | **强断言**：余额 100<阈值 500 触发 + shortfall=400；阈值=0 默认关闭空列表 | ⑪（预警阈值配置化） | — |
| **缺口** | — | — | **无行级权限测试**（`enableActionAuth=OptionalBoolean.FALSE` 全程关闭认证，§2.3 ⑫）；实现亦无行级权限（复用 P1-MA2-093） |

---

## 4. 运行时行为证据（L5）

> 方法论 §1 L5：复用既有 MA2 报告 + 本切片差异。

| UC | L5 行为证据来源 | 复用/差异 |
|----|----------------|----------|
| UC-FIN-05 | A2.18 `:40`「凭证账套隔离：`ErpFinVoucher.acctSchemaId` mandatory + 多账套并行经 `SchemaPropagator.resolveTargetSchemas` + `ErpFinPostingProcessor.process:131,161-188` 循环按账套生成独立凭证」+ `:185-189` 写路径 stamp PASS | **复用**：写路径 stamp acctSchemaId 已证实；**本切片补**：读路径双计已由 R1.29 fix（`TestErpFinMultiSchemaReportIsolation` 强断言） |
| UC-FIN-16 | 报表/看板为查询面，无专属 MA2 行为报告；2026-07-06 `:123` UC-FIN-16 ✅「五张报表渲染」 | **复用**：渲染管线已证实；**本切片补**：现金流量分类 + 间接法缺失（P1-RC-007）+ CLOSED 门控未强制（P2-RC-008）需求视角差异 |
| UC-FIN-17 | A2.18 `:99-101`「11 dashboard BizModel 全部 `@Inject IDaoProvider` 直访绕过（空）认证管道」+ `:194-211` P1-MA2-095 读路径 12 站点 | **复用**：读路径双计已 fix（P1-MA2-095 R1.29）；**本切片补**：行级权限（用户维度）仍 open（P1-MA2-093，⑫ 投影复用） |

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC P0/P1/P2/接受）

> 方法论 §2 分级判据 + §3 完整枚举（每 UC 一行，验收标准全覆盖）。

### 5.1 五级追踪矩阵

| UC | L1 需求契约 | L2 owner doc（设计参考） | L3 代码 | L4 测试 | L5 运行时 | 结论 |
|----|------------|------------------------|--------|--------|----------|------|
| **UC-FIN-05** | `use-cases.md:93` 多账套并行过账（4 断言：①每 AcctSchema 一组凭证 ②acctSchemaId 不同+GlBalance 隔离 ③科目映射不同 ④全部 posted） | `multiple-accounting-schemas.md §并行核算机制:60` / `§账套级联字段:96` / `§数据隔离:243` + `posting.md §多套科目表`（设计参考，冲突以 L1 为准） | `SchemaPropagator.java:44-81` + `ErpFinPostingProcessor.process:131,161-188` + `persistVoucher:785/:820` + `ErpFinGlBalance.acctSchemaId mandatory orm.xml:910` | `TestErpFinMultiSchemaPosting#testMultiSchemaPropagatesTwoVouchersWithDistinctSchema:80`（强）/ `#testSecondaryVoucherFallsBackToSourceSubjectWhenNoMapping:132`（强）+ `TestErpFinMultiSchemaReportIsolation#testBalanceSheetScopedToPrimarySchema:47`（强） | A2.18 `:40,:185-189` 写路径 stamp PASS（复用）+ R1.29 读路径隔离 fix（本切片 HEAD 复核） | **接受**（历史 🔶 闭环） |
| **UC-FIN-16** | `use-cases.md:318` 财务三大报表（5 断言：⑤三表基于 GlBalance ⑥资产==负债+权益 ⑦收入-成本-费用=净利润+结转未分配利润 ⑧现金流量经营/投资/筹资分类+间接法 + CLOSED 期间门控） | `multiple-accounting-schemas.md §账套查询与报表:149`（每账套独立三表）+ `period-close.md`（CLOSED 期间）+ `dashboards.md`（设计参考） | `ErpFinReportBizModel.java`：`buildBalanceSheetDataset:269` / `buildIncomeStatementDataset:284`（GlBalance）+ `buildCashFlowDataset:299`（**VoucherLine**，`:314` 硬编码 OPERATING）+ `loadGlBalances:386-413`（无 CLOSED 门控） | `TestErpFinReportRendering#testBalanceSheetDataset:140`（强）/ `#testIncomeStatementDataset:152`（强）/ `#testCashFlowDataset:162`（强，但仅断言流入额，**无分类断言**）；**无 CLOSED 门控测试** | 报表/看板查询面无专属 MA2 行为报告；2026-07-06 `:123` ✅ 渲染（复用） | **P1**（⑧ 现金流量分类+间接法缺失 → P1-RC-007）+ P2（CLOSED 门控 → P2-RC-008） |
| **UC-FIN-17** | `use-cases.md:350` 财务看板（3 断言：⑩KPI 实时聚合非硬编码 ⑪预警阈值配置化 ⑫行级权限约束） | `dashboards.md §4 财务看板:105` + `§实现约定:236` + `roles-and-permissions.md`（行级权限，设计参考） | `ErpFinDashboardBizModel.java`：`getDashboardKpi:57-85`（实时聚合）/ `findCashFlowAlert:136-157`（`AppConfig.var` 阈值）/ `applyOrgAndSchemaScope:257-267`（period.orgId 非 user.orgId，`IServiceContext` 收而不用） | `TestErpFinDashboard#testKpiAggregationArithmetic:69`（强）/ `#testCashFlowAlertTriggersWhenBelowThreshold:155`（强）；**无行级权限测试**（`enableActionAuth=FALSE`） | A2.18 `:99-101` 11 dashboard 直访绕过认证（复用 P1-MA2-093）+ R1.29 读路径双计 fix（本切片 HEAD 复核） | **接受 on ⑩⑪**；**⑫ 复用 P1-MA2-093**（行级权限未落地） |

### 5.2 每 UC 符合性结论（§2 判据编号）

#### UC-FIN-05 多账套并行过账 → **接受**

- ①每 AcctSchema 一组凭证：**接受** — `SchemaPropagator.resolveTargetSchemas:44-81` 按 `multi-schema-enabled` + `isPropagate` 展开；`TestErpFinMultiSchemaPosting#testMultiSchemaPropagatesTwoVouchersWithDistinctSchema:80` 强断言 2 张凭证。
- ②acctSchemaId 不同 + GlBalance 隔离：**接受** — 写路径 stamp acctSchemaId（mandatory `orm.xml:422/489/910`）；读路径隔离经 R1.29 fix（`TestErpFinMultiSchemaReportIsolation` 强断言 1000 not 4000）。
- ③科目映射不同：**接受** — CoA 映射（`ErpMdAcctSchemaCoa`）支持；无映射时回退源科目 + 借贷平衡保持（`testSecondaryVoucherFallsBackToSourceSubjectWhenNoMapping:132` 强断言）。
- ④全部 posted：**接受** — `assertEquals(VOUCHER_STATUS_POSTED, ...)` 每张凭证强断言。
- **历史 🔶 HEAD 复核**（2026-07-06 `:112` "无凭证/余额隔离测试"）：**已闭环**。HEAD `c1b775491`：凭证隔离 = `TestErpFinMultiSchemaPosting`（4 测试方法，强断言）+ 余额读路径隔离 = `TestErpFinMultiSchemaReportIsolation`（强断言 1000 not 4000）。🔶 升级为 ✅。

#### UC-FIN-16 财务三大报表 → **P1（P1-RC-007）+ P2（P2-RC-008）**

- ⑤三表基于 GlBalance：**P2 倾向接受** — BS/IS 读 GlBalance（`buildBalanceSheetDataset:269` / `buildIncomeStatementDataset:284`）；现金流量读 VoucherLine（`buildCashFlowDataset:299`）字面偏离 L1 但设计合理（现金流需交易级）。归 P1-RC-007 上下文（现金流量表整体偏离），不单独登记。
- ⑥资产==负债+权益：**接受** — 恒等式由过账引擎 `assertBalanced`（双式记账）结构保证；报表分段合计正确（`testBalanceSheetDataset:140` 测试数据 700=300+400 恒等）。
- ⑦收入-成本-费用=净利润 + 结转未分配利润：**接受** — 损益类发生额正确（`testIncomeStatementDataset:152`）；"结转"是期间结账机制（UC-FIN-06/07 `ProfitLossClosingService`，A1.6 已审），非报表本身职责。
- ⑧现金流量分类(经营/投资/筹资) + 间接法：**P1 → P1-RC-007** — `buildCashFlowDataset:314` 硬编码全部 `OPERATING`；投资/筹资分类完全缺失；间接法（净利润+非现金项目+营运资金变动）完全缺失；`ErpMdSubject` ORM 无 cashFlowType 字段（数据模型不支持）。命中 §2 P1①（功能实质偏离验收标准——分类维度 2/3 缺失 + 间接法缺失，非边界场景）。
- CLOSED 期间门控：**P2 → P2-RC-008** — `loadGlBalances:386-413` 仅按 periodId 过滤，不校验 `period.status==CLOSED`；OPEN 期间可渲染。命中 §2 P2①（次要验收标准未完全满足，主路径[数据存在即渲染]OK，边界[OPEN 期间数据不完整]弱——数据完整性关注非会计正确性破坏）。
- ⑨BUDGET/COMMITMENT 过滤（A1.2 caveat ③ 交叉）：**BS/IS 安全** — `orm.xml:1740-1742` 注记「过账引擎本就不维护 ErpFinGlBalance，故预算不引入 GlBalance 结构变更」，BUDGET/COMMITMENT 影子凭证不入 GlBalance，BS/IS 不过滤亦不受影响；**cash flow 读 VoucherLine 不过滤 postingType** — 理论上可含 BUDGET/COMMITMENT 现金行，但实操 BUDGET/COMMITMENT 不触现金科目（1001/1002/1012/1031）。**caveat ③ 结论**：试算平衡/三表恒等式**不被破坏**（A1.2 caveat ③ 收口；cash flow 低风险登记 §7 静态存疑点 SP-1 供 MA4 运行时确认）。

#### UC-FIN-17 财务看板 → **接受 on ⑩⑪；⑫ 复用 P1-MA2-093**

- ⑩KPI 实时聚合非硬编码：**接受** — `getDashboardKpi:57-85` 全部实时聚合自 GlBalance/FundAccount/ArApItem，零硬编码数值（`testKpiAggregationArithmetic:69` 强断言精确算术）。
- ⑪预警阈值配置化：**接受** — `findCashFlowAlert:137-139` `AppConfig.var(CONFIG_DASH_FIN_CASH_FLOW_THRESHOLD, DEFAULT...)`，非硬编码（`testCashFlowAlertTriggersWhenBelowThreshold:155` + `testCashFlowAlertDisabledByDefault:143` 强断言配置驱动两路径）。
- ⑫行级权限约束：**FAIL → 复用 P1-MA2-093** — `applyOrgAndSchemaScope:257-267` 按 `period.orgId`（期间所属组织）非登录用户组织；`IServiceContext context` 收而不用；无 `IUserContext.getOrgId()`；data-auth.xml 空。同根因同控制点 = A2.18 P1-MA2-093（orgId 查询隔离全仓未落地，A2.18 `:99-101` 显式列 `ErpFinDashboardBizModel` 为 11 dashboard 之一）。按 §7 裁决：**复用 P1-MA2-093**，追加 RC 交叉引用注记，**不新建**。

### 5.3 候选缺口/偏离分级总表（逐条验收标准 ①-⑫）

| # | 验收标准 | 分级 | 命中 §2 判据 | finding |
|---|---------|------|-------------|---------|
| ① | UC-FIN-05 每 AcctSchema 一组凭证 | 接受 | — | — |
| ② | UC-FIN-05 acctSchemaId 隔离 | 接受（写+读均落地） | — | — |
| ③ | UC-FIN-05 科目映射不同 | 接受（CoA 映射 + 回退分支） | — | — |
| ④ | UC-FIN-05 全部 posted | 接受 | — | — |
| ⑤ | UC-FIN-16 三表基于 GlBalance | P2 倾向接受（cash flow 用 VoucherLine，设计合理） | — | 归 P1-RC-007 上下文 |
| ⑥ | UC-FIN-16 资产==负债+权益 | 接受（过账引擎结构保证） | — | — |
| ⑦ | UC-FIN-16 净利润结转未分配利润 | 接受（结转属 UC-FIN-06/07） | — | — |
| ⑧ | UC-FIN-16 现金流量分类+间接法 | **P1** | §2 P1①（功能实质偏离） | **P1-RC-007**（新建） |
| ⑨ | UC-FIN-16 CLOSED 期间门控 | **P2** | §2 P2①（次要验收标准，主路径 OK 边界弱） | **P2-RC-008**（新建） |
| ⑩ | UC-FIN-17 KPI 实时聚合 | 接受 | — | — |
| ⑪ | UC-FIN-17 阈值配置化 | 接受 | — | — |
| ⑫ | UC-FIN-17 行级权限 | **P1（复用）** | §2 P1①（功能未落地） | **复用 P1-MA2-093**（追加 RC 交叉引用，不新建） |
| ⑬ | UC-FIN-16 BUDGET/COMMITMENT 过滤（A1.2 caveat ③ 交叉） | 接受（BS/IS 安全） | — | cash flow 低风险 → §7 SP-1 |

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

> 方法论 §7：产出 finding 前 grep arm-index 同域同控制点后裁决。禁止未经比对直接新建。

### 6.1 裁决表

| 本切片 finding 候选 | arm-index grep 结果 | 裁决 | 依据 |
|-------------------|--------------------|------|------|
| UC-FIN-16 ⑧ 现金流量分类(经营/投资/筹资)+间接法缺失 | grep `报表|看板|现金流量|cash flow|投资|筹资|报表正确性` arm-index → **零命中**现金流量分类控制点（A2.18 P1-MA2-095 是读路径双计非分类；2026-07-06 `:123` UC-FIN-16 ✅ 是渲染非分类） | **新建 P1-RC-007** | 新功能点（现金流量分类维度）+ 新根因（ErpMdSubject 无 cashFlowType 字段 + 硬编码 OPERATING），与既有 finding 不同控制点 |
| UC-FIN-16 CLOSED 期间门控未强制 | grep `CLOSED|期间门控|未结账` arm-index → 零命中报表 CLOSED 门控控制点（P1-MA2-021 CLOSED_FINAL 凭证锁定是过账侧非报表侧） | **新建 P2-RC-008** | 新控制点（报表渲染侧 CLOSED 门控，非过账侧凭证锁定） |
| UC-FIN-17 ⑫ 行级权限未落地 | grep `orgId|行级权限|dashboard|多公司` arm-index → **A2.18 P1-MA2-093 命中**（orgId 查询隔离全仓未落地，`:99-101` 显式列 `ErpFinDashboardBizModel`） | **复用 P1-MA2-093** | 同根因（无 IUserContext.getOrgId + 空 data-auth + dashboard 直访）同控制点（行级权限），§7 复用规则 |
| UC-FIN-05 读路径双计（原 P1-MA2-095） | A2.18 P1-MA2-095 命中 | **复用 + HEAD 复核已 fix** | R1.29 fix 已落地（`TestErpFinMultiSchemaReportIsolation` 强断言），本切片 HEAD 复确认已修复（§2.4） |
| UC-FIN-16 BUDGET/COMMITMENT 过滤（A1.2 caveat ③） | A1.2 caveat ③ 命中（交本切片复核） | **caveat ③ 收口**（BS/IS 安全） | orm.xml:1740-1742 注记证实 BUDGET/COMMITMENT 不入 GlBalance |

### 6.2 双向可追溯

- **新 finding 入 arm-index**：P1-RC-007 + P2-RC-008 将写入 arm-index §RC 发现追踪（§Phase 2 同步更新）。
- **复用 finding 追加 RC 交叉引用**：P1-MA2-093 arm-index 行追加「RC 视角复核：见 rc-ma1-a1-7 UC-FIN-17 ⑫ 投影」注记。
- **修复行引用 finding**：MR1 R1.0 展开时，P1-RC-007/P2-RC-008 修复行须含 finding ID 交叉引用。

---

## 7. 静态存疑点清单（供 MA4 A4.1 运行时展开）

> 方法论 §6 段落 7：L5 无法静态定论、需运行时确认的点。每存疑点一行。

| SP | 存疑点 | 静态状态 | MA4 运行时确认方式 |
|----|--------|---------|-------------------|
| SP-1 | cash flow 读 VoucherLine 不过滤 postingType，BUDGET/COMMITMENT 影子凭证是否含现金科目（1001/1002/1012/1031）行 → 现金流量表是否被影子凭证污染 | 静态推断：BUDGET/COMMITMENT 实操不触现金科目（预算/承付追踪费用/AP 科目），低风险；但代码无显式守卫 | MA4 A4.1 运行时：seed 含 BUDGET postingType + 现金科目行的凭证，跑 `buildCashFlowDataset` 断言是否计入 |
| SP-2 | 多账套部署（`multi-schema-enabled=true`）下"每账套独立三表"运行时渲染 — 当前读路径取主账套 FINANCIAL，非按账套切换渲染 | 静态：GlBalance 物理按 acctSchemaId 隔离已落地（写路径）；读路径取主账套是合理简化（UC-FIN-05 ② 接受）；L1 UC-FIN-16 涉及机制提"每账套独立三表"但未列为验收标准 | MA4 A4.1 运行时：多账套部署下按 acctSchemaId 参数切换报表渲染（当前 `balanceSheetData(periodId)` 无 acctSchemaId 参数） |
| SP-3 | CLOSED 期间门控缺失的运行时数据完整性影响 — OPEN 期间渲染报表是否实际产生误导（部分凭证未过账/未结转） | 静态：`loadGlBalances` 按 periodId 取数，OPEN 期间数据可能不完整（未过账凭证不入 GlBalance） | MA4 A4.1 运行时：OPEN 期间 + 未过账凭证场景跑 BS，对比 CLOSED 后 BS 差异 |
| SP-4 | 看板行级权限运行时过滤 — `period.orgId` scope 在跨组织用户场景下是否泄漏 | 静态：scope 按期间所属组织非登录用户组织；单组织种子（orgId=2）掩盖跨组织泄漏（A2.18 `:32` 已证实） | MA4 A4.1 运行时：多组织部署 + 用户归属 orgA 但查 orgB 期间，断言是否泄漏（复用 P1-MA2-093 运行时确认） |

---

## 8. 过程纪律自检

> 方法论 §8 模板。

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（HEAD `c1b775491`）。actual vs baseline 汇总表见下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0，本次 EXIT=0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本审计为只读审计（零代码变更），checker 无回归风险**——下表 R12c actual=40 == baseline=40（基线经 plan `2026-07-31-1705-2` 裁决上调，含本切片复核的 `ErpFinDashboardBizModel`/`ErpFinReportBizModel` 消费 `AcctSchemaResolver`，§2.4），零裸漂移。

| 规则 | 描述 | Baseline | Actual（HEAD `c1b775491`） | 漂移 | 裁决 |
|------|------|----------|--------------------------|------|------|
| R1a-d | BizModel dao().save/update/get/findAll | 0/0/0/14 | 0/0/0/14 | 0 | ✅ |
| R2a-d | daoFor 跨域/生产总量 | 34/229/1382/34 | 34/229/1382/34 | 0 | ✅ |
| R3 | new Erp*() | 5 | 5 | 0 | ✅ |
| R12a | import ErpFinBusinessType | 69 | 69 | 0 | ✅ |
| R12b | import PostingEvent | 66 | 66 | 0 | ✅ |
| R12c | import AcctSchemaResolver | 40 | 40 | 0 | ✅（基线 `2026-07-31-1705-2` 裁决上调 38→40，含本切片 finance 报表/看板消费） |
| （其余 R4/R5/R6/R7/R8/R10/R11） | — | — | — | 0 | ✅ |

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1 裁决表），无未经比对直接新建的 finding。P1-RC-007/P2-RC-008 经 grep 确认为新控制点；UC-FIN-17 ⑫ 复用 P1-MA2-093。

---

## 报告 9 段完整性自检（§6 段落完整性自检）

| # | 段落 | 状态 |
|---|------|------|
| 1 | 需求契约原文（L1 逐字引用） | ✅ §1（UC-FIN-05/16/17 三 UC 验收标准全逐字） |
| 2 | 实现证据（L3 file:line + 跨域调用链） | ✅ §2（§2.1-§2.4 含行号 + 调用链） |
| 3 | 测试证据（L4 + 断言强度） | ✅ §3（4 测试类逐方法断言强度 + 缺口） |
| 4 | 运行时行为证据（L5 复用 MA2 + 差异） | ✅ §4 |
| 5 | 符合性结论（五级矩阵 + 每 UC P0/P1/P2/接受） | ✅ §5（§5.1 矩阵 + §5.2 每 UC 结论 + §5.3 ①-⑬ 分级总表） |
| 6 | 与 arm-index 衔接（复用 or 新增 裁决） | ✅ §6（§6.1 裁决表 + §6.2 双向可追溯） |
| 7 | 静态存疑点清单（供 MA4 展开） | ✅ §7（SP-1..SP-4） |
| 8 | 过程纪律自检段 | ✅ §8（checker actual vs baseline 表 + 独立性 + 交叉去重） |
| 9 | 与既有 MA2 报告差异增量声明 | ✅ §9（报告开头前置声明） |

**9 段齐全，无缺失。**

---

## 附：finding 摘要（供 arm-index 登记）

- **P1-RC-007**（新建）：UC-FIN-16 ⑧ 现金流量分类(经营/投资/筹资) + 间接法缺失 — `buildCashFlowDataset:314` 硬编码 OPERATING + ErpMdSubject 无 cashFlowType 字段。§2 P1①。目标 MR1。
- **P2-RC-008**（新建）：UC-FIN-16 CLOSED 期间门控未强制 — `loadGlBalances:386-413` 不校验 period.status==CLOSED。§2 P2①。successor watch-only。
- **复用 P1-MA2-093**：UC-FIN-17 ⑫ 行级权限未落地（追加 RC 交叉引用注记，不新建）。
