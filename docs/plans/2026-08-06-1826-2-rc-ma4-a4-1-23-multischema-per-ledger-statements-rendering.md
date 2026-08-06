# 2026-08-06-1826-2 rc-ma4-a4-1-23-multischema-per-ledger-statements-rendering 多账套部署「每账套独立三表」运行时渲染确认

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.23（MA4 运行时行为验证 — A1.7 §7 存疑点 SP-2：UC-FIN-16 多账套部署[`multi-schema-enabled=true`]下「每账套独立三表」运行时渲染 — 当前报表读路径 `balanceSheetData/incomeStatementData/cashFlowStatementData(periodId)` 无 acctSchemaId 参数，经 `applyOrgAndSchemaScope` 取主账套，非按账套切换渲染）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.23；存疑点来源 `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-2
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done）、`docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`（A1.7 报告 §2.4 多账套读路径隔离 HEAD 复核 + §5.1 UC-FIN-05 接受 + §7 SP-2）、`docs/design/finance/use-cases.md:342`（UC-FIN-16 L1 **涉及机制**「多账套(每账套独立三表)」逐字）、`docs/design/finance/multiple-accounting-schemas.md §账套查询与报表:149-169`（L2 多账套查询：凭证查询按账套过滤[财务账/管理账/全部账套] + 账套对账）、`docs/audits/arm-index.md` P1-MA2-095 行 successor 注记（MA3 A3.5 已裁决「每账套独立三表运行时渲染」触发条件未满足→维持 backlog successor，`2026-08-07-0430-rc-ma3-a3-5-...md` §2.6）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.23 验证报告（落盘 `docs/audits/2026-08-06-1826-rc-ma4-a4-1-23-multischema-per-ledger-statements-rendering.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：读 `balanceSheetData/incomeStatementData/cashFlowStatementData:238-251` 签名 + `applyOrgAndSchemaScope:489-499` 主账套解析 + `AcctSchemaResolver.resolvePrimarySchemaId` + 多账套部署 config + L1/L2 涉及机制措辞核对）。范式对齐 A4.1.21（done — period-close 运行时行为评估同型工作项）。

- **存疑点原文**（A1.7 报告 §7 SP-2，`2026-08-02-2115-...-a1-7-...md` §7）：「多账套部署（`multi-schema-enabled=true`）下『每账套独立三表』运行时渲染 — 当前读路径取主账套 FINANCIAL，非按账套切换渲染」。静态状态：GlBalance 物理按 acctSchemaId 隔离已落地（写路径，UC-FIN-05 ② 接受）；读路径取主账套是合理简化（UC-FIN-05 ② 接受）；L1 UC-FIN-16 涉及机制提「每账套独立三表」但未列为验收标准。MA4 运行时确认方式：多账套部署下按 acctSchemaId 参数切换报表渲染（当前 `balanceSheetData(periodId)` 无 acctSchemaId 参数）。

- **关联既有 finding**：
  - **UC-FIN-05 ② 接受**（A1.7 §5.1/§5.2）：多账套并行过账写路径 stamp acctSchemaId + GlBalance 物理隔离已落地（`SchemaPropagator:44-81` + `ErpFinGlBalance.acctSchemaId` mandatory orm.xml:910 + `TestErpFinMultiSchemaReportIsolation#testBalanceSheetScopedToPrimarySchema:47` 强断言 1000 not 4000）。读路径双计已由 R1.29 fix（`applyOrgAndSchemaScope` 注入）。
  - **P1-MA2-095 resolved R1.29**（A1.7 §2.4 HEAD 复核 + arm-index）：原 A2.18 列 12 处报表/看板查询省略 acctSchemaId/orgId 致双计，R1.29 plan `2026-07-30-0841-3-r1-29` 注入 `applyOrgAndSchemaScope` 修复读路径双计。本验证补「按账套切换渲染[per-ledger 三表]」维度差异（非双计，而是「取主账套 vs 按账套选择」）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:318` UC-FIN-16 财务三大报表。**涉及机制**（`:342` 逐字）：「ErpFinGlBalance、period-close.md、多账套(每账套独立三表)」。**可验证断言**（`:322-340`）：5 条（数据来源 :325 / BS 恒等式 :327-328 / IS 公式 :330-332 / CF 分类+间接法 :334-336 / 期间控制 :339）。**关键裁决点**：L1 将「多账套(每账套独立三表)」列于**涉及机制**（`:342`）而非**可验证断言**（`:322-340` 均未提 per-ledger 切换）。按 Q1 真相源层级（methodology §4），涉及机制是设计语境（非约束力），可验证断言才是符合性判据。L2 `multiple-accounting-schemas.md §账套查询与报表:149-169` 描述的是「多账套查询」= **凭证查询按账套过滤**（财务账/管理账/全部账套 :156-160）+ 账套对账（:171），**非**「每账套独立三表」措辞（该短语仅存在于 L1 :342 涉及机制）。UC-FIN-05 `:93` 多账套并行过账可验证断言②「GlBalance 按 acctSchemaId 隔离(各账套余额独立)」= 写路径物理隔离，已接受。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实，全在 module-finance/erp-fin-service）**：
  - 报表公共入口（`ErpFinReportBizModel.java:238-251`）：`balanceSheetData(@Name("periodId") Long periodId, IServiceContext context):238` / `incomeStatementData(...):244` / `cashFlowStatementData(...):250` 三个 `@BizQuery` 方法签名**仅含 periodId**，无 acctSchemaId 参数 → 调用 `buildBalanceSheetDataset(periodId):239` / `buildIncomeStatementDataset(periodId):245` / `buildCashFlowDataset(periodId):251`。
  - 数据集构建（`:269-323`）：`buildBalanceSheetDataset(periodId):269` → `loadGlBalances(periodId):272`；`buildIncomeStatementDataset(periodId):284` → `loadGlBalances(periodId):287`；`buildCashFlowDataset(periodId):299` → `loadPostedVoucherLines(periodId):302`。均无 acctSchemaId 参数。
  - 主账套 scope 注入（`applyOrgAndSchemaScope:489-499`）：`resolvePeriodOrgId(periodId):490`（查期间 orgId）→ `resolveOrgSchemaId(orgId):495`（`AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId):485`）→ `q.addFilter(eq("acctSchemaId", schemaId)):497`（**仅取主账套**）。注释 `:472-474`「多账套部署下报表按 periodId 聚合会双计；按期间所属组织 + 主账套补 filter 使单账套不双计。scope 不可解析时跳过 filter，保护单组织基线零回归」。
  - **结论**：多账套部署下报表渲染恒取**主账套** FINANCIAL 三表，无按 acctSchemaId 切换渲染次账套（MANAGEMENT 等）三表的入口。次账套三表数据**物理存在**（GlBalance 按 acctSchemaId 隔离已落地），但**读路径无选择入口**。

- **既有证据（复用输入）**：
  - A1.7 §2.4（`:131-142`）：P1-MA2-095 读路径双计 HEAD 复核 resolved R1.29（`applyOrgAndSchemaScope` 注入 ReportBizModel `:472-508` / DashboardBizModel `:242-267`）；`TestErpFinMultiSchemaReportIsolation#testBalanceSheetScopedToPrimarySchema:47-73` 强断言（1000 not 4000）。复核结论（`:142`）：「多账套部署下『每账套独立三表』（L1 UC-FIN-16 涉及机制）仍只取主账套，非按账套切换渲染——属 §2.1 UC-FIN-05 ② 接受范围（GlBalance 物理隔离已落地，读路径取主账套是合理简化）」。
  - A1.7 §5.1 UC-FIN-05 接受（写路径 stamp + GlBalance 隔离）。

- **剩余差距**：「每账套独立三表」运行时渲染的**需求契约符合性**未运行时裁决 —— ①L1 UC-FIN-16 `:342` 将「多账套(每账套独立三表)」列于涉及机制，是否构成可符合性判定的验收义务（Q1 真相源层级：涉及机制 vs 可验证断言 `:322-340` 的判据力）；②当前读路径无 acctSchemaId 参数（恒取主账套）是否满足「每账套独立三表」语义（数据物理隔离已落地[可查] vs 读入口未暴露 per-ledger 切换）；③L2 `multiple-accounting-schemas.md §账套查询与报表:149-169` 实际描述凭证查询按账套过滤+账套对账（**非**「每账套独立三表」短语——该短语仅 L1 :342 涉及机制），其与报表读入口的差距是否构成 doc↔code drift。本验证闭合 UC-FIN-16 per-ledger 三表渲染维度的运行时符合性裁决（既有 MA3 A3.5 successor 裁决「触发条件未满足→维持 backlog」作为输入对齐）。

- **保护区域**：只读评估（读报表方法签名 + applyOrgAndSchemaScope 主账套解析 + AcctSchemaResolver + 多账套 config + L1/L2 措辞核对），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若发现 per-ledger 切换缺失构成 finding，登记归 MR1；修复 = 报表方法加 acctSchemaId 参数 + 切换 scope 属 BizModel 代码逻辑[预授权自动执行]，不触 §5 ask-first — 非会计过账核心路径，仅报表读侧参数化）。

## Goals

- 报表读入口签名核验：给出 `ErpFinReportBizModel.java:238-251`（`balanceSheetData/incomeStatementData/cashFlowStatementData(periodId)` 三个 `@BizQuery` 仅含 periodId 无 acctSchemaId 参数）+ `:269-323`（buildXxxDataset(periodId) 无 acctSchemaId）证据（file:line）。证实读入口无 per-ledger 切换参数。
- 主账套 scope 解析核验：给出 `applyOrgAndSchemaScope:489-499`（`resolvePeriodOrgId` → `resolveOrgSchemaId` → `AcctSchemaResolver.resolvePrimarySchemaId` → `eq("acctSchemaId", schemaId)` 仅取主账套）+ 注释 `:472-474`（多账套防双计 + scope 不可解析跳过 filter 保护单组织基线）证据。证实多账套部署恒取主账套三表。
- 「每账套独立三表」需求契约符合性裁决（本存疑点核心）：核验 L1 UC-FIN-16 `:342` 将「多账套(每账套独立三表)」列于**涉及机制**而非**可验证断言 `:322-340`**（Q1 真相源层级：涉及机制是设计语境[非约束力]，可验证断言才是符合性判据）。裁决：①若 L1 仅要求「数据物理隔离可查」→ 已满足（GlBalance acctSchemaId 隔离 + 次账套数据物理存在）→ 维持 UC-FIN-05 ② 接受 + UC-FIN-16 涉及机制满足；②若 L1/L2 要求「读入口暴露 per-ledger 切换渲染」→ 当前无 acctSchemaId 参数 → 登记 doc↔code drift 或 P2 watch-only（合理简化 successor）。
- doc↔code drift 评估：核验 L2 `multiple-accounting-schemas.md §账套查询与报表:149-169` 实际描述（多账套查询=凭证查询按账套过滤[财务账/管理账/全部账套] + 账套对账，**非**「每账套独立三表」措辞——该短语仅 L1 :342 涉及机制）与报表读入口差距 —— L2 描述的 per-ledger 过滤能力针对**凭证查询**（非报表三表读入口），是否隐含报表侧 per-ledger 切换义务是裁决点；若 L2 仅约束凭证查询过滤（已实现）则与报表读入口无 drift；若解读为泛化 per-ledger 查询能力则报表侧缺切换入口为合理简化 successor。
- 对齐 UC-FIN-05 ② 接受 + P1-MA2-095 resolved R1.29 + §2 判据给出 per-ledger 渲染维度运行时裁决。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 UC-FIN-05 ② 接受[写路径隔离] + P1-MA2-095 resolved[读路径双计]分层一致（per-ledger 渲染是双计修复之外的「切换选择」维度，不撤销双计修复）。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不实施 per-ledger 三表切换渲染**（若发现缺口，登记 finding 归 MR1；修复 = 报表方法加 acctSchemaId 参数 + 切换 scope 属 BizModel 代码逻辑，预授权自动执行，不触 §5 ask-first）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 UC-FIN-05 全部验收标准**（A1.7 §5.1 UC-FIN-05 接受[写路径 stamp + GlBalance 隔离]；本验证只评 UC-FIN-16 涉及机制「每账套独立三表」的读路径渲染维度）。
- **不重审 P1-MA2-095 读路径双计修复**（resolved R1.29，A1.7 §2.4 HEAD 复核已确认；本验证只补「per-ledger 切换选择」维度，非双计；既有 MA3 A3.5 successor 裁决「触发条件未满足→维持 backlog」作为输入对齐，不重新裁决 successor 触发条件）。
- **不展开 A1.7 §7 SP-1/SP-3/SP-4**（cash flow postingType / CLOSED 门控 / 看板行级权限，独立工作项 A4.1.22/A4.1.24/A4.1.25）。
- **不修改 product-scope/use-cases 真相源**（按 methodology §9 真相源冻结条款，审计发现的 doc 分歧记入报告，不直接改真相源；若涉及机制措辞需澄清，登记 successor 等待人工批准）。

## Task Route

- Type: `verification or audit work`（多账套「每账套独立三表」运行时渲染确认 + UC-FIN-16 涉及机制符合性裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级[涉及机制 vs 可验证断言] + §7 衔接 + §8 自检 + §9 真相源冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.23 行）+ `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-2 + §2.4 多账套读路径隔离 + §5.1 UC-FIN-05 接受（输入）+ `docs/design/finance/use-cases.md:318,342`（UC-FIN-16 L1 heading + 涉及机制逐字）+ `docs/design/finance/multiple-accounting-schemas.md §账套查询与报表:149-169`（L2 多账套查询：凭证查询按账套过滤 + 账套对账）+ `docs/audits/arm-index.md` P1-MA2-095 successor 注记（MA3 A3.5 裁决）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。per-ledger 渲染评估需多维度归类（读入口签名 / 主账套 scope 解析 / L1 涉及机制 vs 验收标准判据力[Q1 真相源层级] / L2 doc↔code drift / UC-FIN-05 ② 接受分层 / P1-MA2-095 resolved 分层 / 真相源冻结条款 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读报表方法签名 + applyOrgAndSchemaScope 主账套解析 + AcctSchemaResolver + 多账套 config + L1/L2 措辞核对）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 多账套 per-ledger 三表渲染符合性评估

Status: planned
Targets: `docs/audits/2026-08-06-1826-rc-ma4-a4-1-23-multischema-per-ledger-statements-rendering.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.23 行）；A1.7 done（§7 SP-2 已落盘 + §2.4 多账套读路径隔离 + §5.1 UC-FIN-05 接受）

- [ ] `Proof` 报表读入口签名核验：给出 `ErpFinReportBizModel.java:238-251`（`balanceSheetData/incomeStatementData/cashFlowStatementData(periodId)` 三个 `@BizQuery` 仅含 periodId 无 acctSchemaId 参数）+ `:269-323`（buildXxxDataset(periodId) 无 acctSchemaId）证据（file:line）。证实读入口无 per-ledger 切换参数。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 主账套 scope 解析核验：给出 `applyOrgAndSchemaScope:489-499`（`resolvePeriodOrgId:490` → `resolveOrgSchemaId:495` → `AcctSchemaResolver.resolvePrimarySchemaId:485` → `eq("acctSchemaId", schemaId):497` 仅取主账套）+ 注释 `:472-474`（多账套防双计 + scope 不可解析跳过 filter 保护单组织基线）+ `resolveOrgSchemaId:484-486` 证据。证实多账套部署恒取主账套三表，无按账套切换。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` L1 涉及机制 vs 可验证断言判据力核验（本存疑点核心）：核验 `docs/design/finance/use-cases.md:342`（UC-FIN-16 **涉及机制**「多账套(每账套独立三表)」逐字）vs `:322-340` 可验证断言（5 条：数据来源/BS 恒等式/IS 公式/CF 分类+间接法/期间控制，均未提 per-ledger 切换）。按 methodology §4 Q1 真相源层级裁决「涉及机制」是否构成可符合性判定的验收义务（涉及机制 = 设计语境非约束力；可验证断言 = 符合性判据）。裁决 L1 是否要求「读入口暴露 per-ledger 切换」还是仅要求数据物理隔离可查。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` L2 doc↔code drift 评估：核验 `docs/design/finance/multiple-accounting-schemas.md §账套查询与报表:149-169` 实际描述（多账套查询=凭证查询按账套过滤[财务账/管理账/全部账套 `:156-160`] + 账套对账 `:171`，**非**「每账套独立三表」措辞——该短语仅 L1 :342 涉及机制）与报表读入口差距。L2 描述的 per-ledger 过滤能力针对**凭证查询**（非报表三表读入口）：若 L2 仅约束凭证查询过滤（凭证查询是否已支持按账套过滤须核验）则与报表读入口无 drift；若解读为泛化 per-ledger 查询能力则报表侧缺切换入口为合理简化 successor。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 既有 MA3 successor 裁决交叉引用：核验 arm-index P1-MA2-095 行 successor 注记（MA3 A3.5 `2026-08-07-0430-rc-ma3-a3-5-...md` §2.6 已裁决「每账套独立三表运行时渲染」触发条件未满足→维持 backlog successor）。本验证的 successor 裁决须与既有 MA3 裁决对齐（避免重复裁决/冲突）—— 若维持接受/P2 watch-only 与 MA3「successor 触发条件未满足」一致；若裁决升级须说明触发条件变化。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（多账套 per-ledger 三表渲染是否符合 L1/L2），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` per-ledger 渲染维度运行时裁决（方法论 §2 判据 + §4 Q1 真相源层级 + 三源对照）：①若 L1 仅要求数据物理隔离可查（涉及机制=设计语境非约束力，可验证断言 `:322-340` 未提 per-ledger 切换）+ L2 仅约束凭证查询过滤（非报表读入口）→ 维持 UC-FIN-05 ② 接受 + UC-FIN-16 涉及机制满足，**维持接受无新 finding**（per-ledger 切换 successor，GlBalance 物理隔离已落地，与 MA3 A3.5 successor 裁决一致）；②若 L1/L2 解读为要求读入口暴露 per-ledger 切换而实现仅主账套 → 登记 **P2 watch-only**（合理简化 successor，§2 P2① 次要验收标准边界弱 + doc↔code drift 须 owner doc 标注）。裁决须列明 §2 判据编号 + §4 Q1 层级 + L1/L2/L3 三源 + 与 UC-FIN-05 ② 接受[写路径隔离] + P1-MA2-095 resolved[读路径双计] + MA3 A3.5 successor 裁决分层一致。注意：即便 L2 凭证查询过滤隐含报表侧切换，L1（§4 更高权威）未将其升为可验证断言，不构成 P1。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 读入口签名 + 主账套 scope 解析 + L1 涉及机制判据力 + L2 doc↔code drift 证据落盘（全集，无遗漏），每条有证据（file:line）
- [ ] per-ledger 渲染维度运行时裁决有明确结论（维持接受 或 P2 watch-only），与 UC-FIN-05 ② 接受 + P1-MA2-095 resolved 分层一致

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: planned
Targets: `docs/audits/2026-08-06-1826-rc-ma4-a4-1-23-multischema-per-ledger-statements-rendering.md`（定稿）；`docs/audits/arm-index.md`（新 finding 或注记，若有）；`docs/design/finance/multiple-accounting-schemas.md`（doc drift 标注，若裁决 drift）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 per-ledger 渲染符合性评估 + 运行时裁决完成

- [ ] `Add` finding/注记更新：若 P2 watch-only → 新建 finding（P2-RC-xxx，per-ledger 三表切换读入口缺失 watch-only，与 P1-MA2-095 不同控制点[双计 vs 切换选择]）+ L2 owner doc drift 标注（若裁决 drift，按真相源冻结条款仅登记不直改真相源）；若维持接受无新 finding → 在 arm-index UC-FIN-05 ② / P1-MA2-095 相关行追加 per-ledger 渲染维度注记。禁止未经比对新建重复 finding（grep arm-index 同域同控制点后裁决）。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.7 §2.4/§5.1 UC-FIN-05 / P1-MA2-095 resolved R1.29 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（读入口签名 + 主账套 scope + L1 涉及机制判据力 + L2 drift + 运行时裁决 + finding 衔接 + §8 自检齐全）
- [ ] 新 finding 或注记已登记入 arm-index（若有变更）或有明确「维持接受无变更」记录并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_0295fc45dffeNtqd7IzXykWjFt，新会话不重用执行者上下文）— Q1 层级推理 SOUND（use-cases.md:342 涉及机制 vs :322-340 可验证断言判据力正确）+ 代码锚点准确 + Decision 分支开放 + dedup 可辩护 + 真相源冻结遵守。3 Blockers 已修订：B1（L2 误引——「每账套独立三表」短语**不存在**于 multiple-accounting-schemas.md，L2 §账套查询与报表:149-169 实际描述凭证查询按账套过滤+账套对账；该短语仅 L1 use-cases.md:342 涉及机制）→ 已修正 Current Baseline/Goals/Phase1 Proof L2 引用为实际描述 + 重构 drift 评估；B2（L1 行号 use-cases.md:75→实际 :342，:75 系 A1.7 报告行号误植）→ 已修正全文 L1 引用为 :342；B3（方法名 cashFlowData→实际 cashFlowStatementData:250）→ 已修正。附 R1（既有 MA3 A3.5 successor 裁决交叉引用）→ 已增 Phase1 Proof item + Non-Goal + Related。
- Independent draft review iteration 2: needs revision（独立子代理 ses_02959c8eaffejiH2K8QyvqyW1U，新会话不重用执行者上下文）— iteration-1 3 Blockers（B1/B2/B3）主体已修，但发现 2 处残留：N1（line 33 剩余差距③ 仍含 L2 §:149「每账套独立三表」误引）+ N2（line 33 ① 仍用 :75）+ 1 minor N3（line 13 cashFlowData 简写）。已修订：line 33 ③ 改为 L2 §:149-169 实际描述凭证查询按账套过滤+账套对账（非该短语，短语仅 L1 :342）；line 33 ① 改 :75→:342；line 13 改 cashFlowData→cashFlowStatementData。R1（MA3 A3.5 successor 交叉引用）已确认集成于 Related/Non-Goals/Phase1/Decision。
- Independent draft review iteration 3: accept（独立子代理 ses_02957dd56ffet8JM8iJRH2GF9K，新会话不重用执行者上下文）— N1/N2/N3 全部 resolved（line 33 ①:342 + ③ L2 实际描述 / line 13 cashFlowStatementData）；grep 确认 live body 无残留 :75/cashFlowData（唯一命中在 line 117 Draft Review Record 历史记录块，属可接受的历史记录非 live 声明）；Q1 层级推理 SOUND（涉及机制 :342 设计语境非约束力 vs 可验证断言 :322-340 符合性判据）+ Decision 双分支开放（维持接受 / P2 watch-only）+ Plan Status draft。promote to active。

## Closure Gates

> 本计划为**只读多账套 per-ledger 三表渲染符合性评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 读入口签名 + 主账套 scope + L1 涉及机制判据力 + L2 drift + 运行时裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.1.23 验证报告读入口签名 + 主账套 scope + L1 涉及机制判据力 + L2 drift + 运行时裁决齐全 + finding/注记更新（若有）
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §9 真相源冻结 + §去重协议一致；与 A1.7 §7 SP-2 + §2.4 + §5.1 UC-FIN-05 一致
- [ ] 已运行验证：读入口签名 + 主账套 scope + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### per-ledger 三表切换读入口（若 A4.1.23 登记 finding 后修复归口）

- Classification: `out-of-scope improvement`（本验证是渲染符合性评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是渲染符合性评估，结果表面 = 验证报告 + finding/注记登记。修复（若有）归 MR1（R1.0→RC-R1.n），修复 = 报表方法加 acctSchemaId 参数 + 切换 scope 属 BizModel 代码逻辑[报表读侧参数化]，预授权自动执行，**不触 §5 ask-first**（非会计过账核心路径）。本验证闭环不阻塞于修复落地（finding 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（MR1 R1.0 展开器读取本报告 finding[若有] → RC-R1.n 修复，按报告裁决方向：①维持接受→owner doc 标注涉及机制=数据隔离非读入口切换；②P2 watch-only→报表方法加 acctSchemaId 参数 + applyOrgAndSchemaScope 支持 per-ledger scope 切换）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- MR1 修复 per-ledger 三表切换读入口（若登记 finding）：BizModel 代码逻辑预授权自动执行，不触 ask-first
