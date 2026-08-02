# 保护区域纪律审计报告（A6.4 / MA6）

> Milestone: MA6（安全与权限层审计）
> Dimension: 保护区域纪律（过程纪律——证据三件套回溯）
> Domain/Scope: 全 19 域 / `docs/plans/*.md`（396 份）+ `docs/audits/arm-index.md` P0 追踪 + git 历史
> Status: done（报告产出）
> Audit Plan: `docs/plans/2026-07-29-1410-2-ma6-protected-area-discipline-audit.md`
> Owner Doc: `docs/context/ai-autonomy-policy.md §保护区域`（6 区域 + 必需证据规则）+ `docs/context/project-context.md §AI 阻塞条件` + `docs/plans/00-plan-authoring-and-execution-guide.md §计划决策表`
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（7 维度 + 项目定制化层）
> Verdict: ⚠️(P1)（零 P0——无活跃数据破坏；2 项新 P1 + 1 项新 P2 watch-only）

## 1. 审计目的与方法

### 1.1 审计问题

`ai-autonomy-policy.md §保护区域` 定义 6 个保护区域（已落实为真实条目，非占位符）：

| 区域 | 规则 | 必需证据 |
| --- | --- | --- |
| `model/*.orm.xml` 模式 | ask first | design doc + plan audit |
| `model/*.api.xml` 契约 | ask first | design doc + plan audit |
| `data deletion` | ask first | owner doc + tests |
| `accounting/finance postings` | plan-first | owner doc + tests |
| `auth/permissions` | plan-first | owner doc + tests |
| `deployment / external integrations` | plan-first | owner doc + tests |

A6.1-A6.3（plan `2026-07-29-1410-1`）已审计 `auth/permissions` 的**技术正确性**（权限注解完整性、深度抽样、数据权限运行验证）。A6.4 审计**过程纪律**：每个触及 6 个保护区域的工作项是否有「owner doc 描述预期行为 + 测试策略/测试落地 + plan-audit 证据（独立草案审查 + 结束审计记录）」证据三件套。是否系统性遵守从未被独立审计过。

### 1.2 方法（多维审计 + 项目定制化层）

按 `multi-dimensional-audit-prompt.md` 7 维度挑战，并注入项目保护区域（ORM ask-first / 会计财务 / 数据删除）+ 验证命令（`mvn clean install -DskipTests` / `mvn test`）+ 已知失败模式（自审违规、closure 门控假勾选）。

**证据采集**（3 路独立 explore 子代理并行 + 主代理核验关键违规项）：

1. ORM/api.xml 触及计划盘点（agent `ses_053505a45ffe`，36 份计划）
2. accounting/finance postings 触及计划盘点（agent `ses_053503cf0ffe`，56 份计划）
3. data deletion / deployment / auth + 自主权降级检测 + MA6.1-A6.3 交叉去重（agent `ses_053501b44ffe`）

主代理对 6 项 P0 fix plan 全文逐份核实 + 对最严重违规项（1206-2 / 1206-3 / P0-MA2-019 / P0-MA2-020）独立核验 `Plan Status` / `Draft Review Record` / `Closure Audit Evidence` 原文行号。

### 1.3 证据三件套裁决标准

每个触及项裁决为：

- `TRIPLE_COMPLETE` — owner doc + tests + 独立草案审查（含独立子代理 session id）+ 独立结束审计（含独立子代理 session id，非执行者自查）齐全
- `MISSING_OWNER_DOC` — 无 owner doc 描述预期行为
- `MISSING_TESTS` — 无测试落地
- `MISSING_PLAN_AUDIT` — 缺独立草案审查 OR 缺独立结束审计（执行者自查 / mission-driver 单会话 / 留 `pending` 占位但 Plan Status `completed`）
- `MISSING_HUMAN_CONFIRMATION` — ask-first 区域无显式「人工确认/人工批准」记录（依赖「草案审查预授权」属可争议，单列 P2）
- `BLOCKED_PROPERLY` — 独立 plan-audit 阻断实施（纪律生效样本，非违规）

## 2. 保护区域触及清单 + 证据三件套核实矩阵

### 2.1 总览（按区域）

| 保护区域 | 触及计划数 | TRIPLE_COMPLETE | 违规（缺证据） | BLOCKED_PROPERLY | 违规率 |
| --- | --- | --- | --- | --- | --- |
| `accounting/finance postings`（plan-first） | 56 | 55 | 0 | 1（P0-MA2-018） | 0% |
| `model/*.orm.xml` 模式（ask-first） | 36 | 23 | 12（5 MISSING_PLAN_AUDIT + 6 MISSING_HUMAN_CONFIRMATION + 1 audit-cured） | 1（P0-MA2-018 同项） | 33% |
| `model/*.api.xml` 契约（ask-first） | 0 | — | — | — | n/a（无可触及文件） |
| `data deletion`（ask-first） | 2 | 2 | 0 | 0 | 0% |
| `deployment / external integrations`（plan-first） | 9 | 8 | 1（1206-3） | 0 | 11% |
| `auth/permissions`（plan-first） | 4 | 3 | 1（0444-3，低危 menu 结构） | 0 | 25% |
| **合计（去重后唯一计划）** | **~107** | **94** | **14** | **1** | **~13%** |

> 去重说明：多份计划同时触及多个区域（如 P0-MA2-018 同时触及 orm.xml + finance postings；业财端到端计划同时触及 postings + ORM）。~107 为去重后唯一计划数；14 项违规为唯一违规计划（1206-2 同时违规 ORM + postings 仅计 1）。

### 2.2 区域一：`accounting/finance postings`（最高密度，plan-first）— 56 份

**Verdict: 100% 合规（零违规）。** 56 份计划中 55 份 TRIPLE_COMPLETE + 1 份 PROPERLY_BLOCKED（P0-MA2-018）。每份计划均含：引用的 owner doc（`posting.md` / `period-close.md` / `costing-methods.md` / `ar-ap-reconciliation.md` / `bad-debt.md` / `treasury.md` / `variance-analysis.md` 等）+ 行为测试（JUnit JunitAutoTestCase，多数 + Playwright E2E）+ 独立草案审查（含 `ses_*` 子代理 session id，多数多轮收敛）+ 独立结束审计（新会话、非执行者上下文）。

**正面纪律样本（重点抽样）**：

- **6 项 P0 即时通道 fix 全部合规**（P0-MA1-021 / P0-MA2-016 / P0-MA2-017 / P0-MA2-018 [deferred] / P0-MA2-019 / P0-MA2-020）：每项均声明保护区域门控 + 走独立 plan-audit（含 P0-MA2-016 的会计保护区域「人工确认」Closure Gate）+ 补防回归测试。
- **P0-MA2-018 是「合规 deferred 样本」**：独立 plan-audit 阻断了会回归的字面 UK `(billCode, businessType)` 设计（与红冲「同键 2 行」`TestErpFinPostingService.testReverse:225` + 多账套「同键 N 行」`TestErpFinMultiSchemaPropagatesTwoVouchersWithDistinctSchema:89` + 软删除重插三重契约冲突），plan 正确置 `deferred` 等候人工裁决修复方向 A/B/C/D——**这是保护区域纪律生效的最佳证据**，非遗规。
- **业财端到端四链路**（A2.1 P2P / A2.2 O2C / A2.3 期末结账 / A2.4 库存核算）+ **core-business M1 过账三段**（采购订单/销售订单审批-触发-过账）+ **M4 业财一体**（采购到付款/销售到收款/期末结账/成本核算/年度结转/坏账准备）全部 TRIPLE_COMPLETE。
- **owner-doc drift 显式追踪**：多份计划（如 0730-1 FX 捇兑损益 / 1745-3 cashRepay 红冲联动 / 1452-3 平台监控 API 漂移 / 1000-3 CLOSED_FINAL 字典漂移）显式标注 owner-doc drift 并在计划内修复（R13 不可降级）。
- **独立会话卫生**：每份结束审计显式声明「新会话，非执行者上下文」，附 session id；finance postings 域未发现执行者自查。

### 2.3 区域二：`model/*.orm.xml` 模式（ask-first）— 36 份

**Verdict: 23 TRIPLE_COMPLETE + 1 BLOCKED_PROPERLY + 5 MISSING_PLAN_AUDIT + 6 MISSING_HUMAN_CONFIRMATION + 1 audit-cured。** ORM 区域是违规密度最高的区域（33%），且违规集中于近期计划。

**MISSING_PLAN_AUDIT（5 份，最高严重性）**— ORM 变更（ask-first 最高级保护区域）但无独立结束审计（执行者自查 / mission-driver 单会话 / 无 Draft Review Record）：

| # | 计划 | ORM 变更 | 违规证据（行号） |
| --- | --- | --- | --- |
| 1 | `2026-07-21-1206-2-finance-budget-multi-year-carryforward.md` | finance.orm.xml 加 4 字段（propId 26-29）+ 2 新实体（RollforwardLog/CarryForwardLog）+ 3 字典 | `:364` `Auditor / Agent: 执行者自查（mission driver 单会话执行…独立结束审计由后续 OPEN_AUDIT 触发）`；`:287` 草案审查 iter3 `pending`；Plan Status `completed` |
| 2 | `2026-07-22-1000-2-manufacturing-mrp-drp-simulation-engine.md` | mfg.orm.xml + drp.orm.xml 加 6 实体 + 4 字典 | `:274` `主执行代理（GLM 5.2）执行 + 自查…独立结束审计建议由后续 OPEN_AUDIT 执行` |
| 3 | `2026-07-24-2200-1-cross-domain-code-abstraction.md` | 10 份 orm.xml 移除 38 行 `use-approval` tagSet | `:375` `mission-driver（本会话执行）`（10 轮草案审查标 BLOCKER 后仍自查关闭） |
| 4 | `2026-07-28-1249-arm-fix-p0-ma2-019-aps-capacity-lock.md` | aps.orm.xml 新增 `ErpApsCapacityReservation` 实体 + UK | **无 `## Draft Review Record` 章节**；`:94` `Auditor / Agent: 主代理（EXECUTE 模式，MISSION_DRIVER 驱动）` |
| 5 | `2026-07-28-1249-arm-fix-p0-ma2-020-inv-stock-balance-uk.md` | inventory.orm.xml 加 7 列自然键 `UK_INV_STOCK_BALANCE_NATURAL` | **无 `## Draft Review Record` 章节**；`:94` `主代理执行（self-audit…独立结束审计由后续审计轮次 OPEN_AUDIT 复核）` |

> 关键裁决：#4 #5 是 P0 即时通道 fix plan，触及 ask-first 最高级保护区域（新增实体/UK），却**完全跳过独立草案审查**且自查关闭。底层 P0 缺陷修复是真实的、代码测试全绿（无活跃数据破坏），但审计轨迹纪律在 P0 即时通道 + MISSION_DRIVER 时间压力下破例。属过程纪律 P1，非活跃缺陷 P0。

**MISSING_HUMAN_CONFIRMATION（6 份，可争议）**— ORM 变更但无显式「人工确认/人工批准」Closure Gate 记录，依赖「草案审查预授权加性扩展」：`2026-07-02-0700-1`（inventory-trace-chain）/ `2026-07-04-0831-1`（aps-scheduling）/ `2026-07-04-0831-3`（hr-shift）/ `2026-07-04-2200-2`（contract-e-signature）/ `2026-07-05-0427-1`（demand-forecast）/ `2026-07-05-0540-1`（bad-debt-provision）。这 6 份均有独立草案审查 + 独立结束审计（实质纪律遵循），仅缺显式 ask-first 人工确认 checkbox。按 `ai-autonomy-policy.md`「AI 编写或修改的文档不能作为放宽自主权证据除非人工明确批准」，此「草案审查预授权」属可争议——单列 P2 watch-only。

**audit-cured（1 份）**：`2026-07-15-1022-1-orm-tagset-all-domains.md` 初版执行者自查（rule-12 违规），后被 Round 2 独立审计 `ses_0925d2694ffe` PASS_WITH_NOTES 治愈。

**正面样本**：`2026-07-08-0056-1-extended-domains-posted-businessdate-std-fields.md`（7 域 posted/businessDate）是 ask-first 纪律黄金样本——7 轮 mission-driver 迭代显式辩论并单独记录 ORM 门控（path (a) `[MISSION_DRIVER]` 显式 unblock）。

### 2.4 区域三：`model/*.api.xml` 契约（ask-first）— 0 份

**Verdict: 空集，vacuously 合规。** 实仓 `find . -name "*.api.xml"` 返回 0 命中（A3.6 API 契约审计 `2026-07-28-1953-2` 确认：Nop `api-model-and-codegen.md:218-227` 决策表明确 CRUD-centric 模块不应手写 api.xml + 本项目无跨进程 RPC 需求 + 211 生成 `*Api.java` 全部 `ICrudApi` + 352 `_*.xbiz` 全部 ORM 驱动自动派生）。无手写 api.xml 即无可触及文件，保护区域 vacuously 满足。

### 2.5 区域四：`data deletion`（ask-first）— 2 份

**Verdict: 100% 合规（零违规）。** 2 份触及物理删除路径的计划均 TRIPLE_COMPLETE：

- `2026-07-18-2251-1-manufacturing-variance-recompute-reversal.md`：在既有 `deleteByWorkOrder()` 物理删除 `ErpMfgCostVariance` 前加 `reverseIfExists()` 红冲步骤防孤儿凭证——owner doc `variance-analysis.md` + `TestErpMfgVarianceRecomputeReversal` 4 case + 独立草案审查 2 轮 + 独立结束审计 2026-07-19。
- `2026-07-18-1745-2-inventory-manufacturing-posting-reversal.md`：`removeFifoAdjustLayer` 物理删除为既有，计划加 reverse-voucher-first 包装——同族，TRIPLE_COMPLETE。

`2026-07-04-2050-1-use-approval-migration.md` 的 HR 字典删除显式 Non-Goal（deferred 独立 plan）；`2026-07-08-1234-1-demo-seed-data-init.md` 是部署时测试种子 reset（非应用层数据删除特性）。均不计入。

### 2.6 区域五：`deployment / external integrations`（plan-first）— 9 份

**Verdict: 8 TRIPLE_COMPLETE + 1 MISSING_PLAN_AUDIT。**

**MISSING_PLAN_AUDIT（1 份）**：

- `2026-07-21-1206-3-external-api-integration-reference-pattern.md`：新增 `IErpMdExchangeRateApiClient` SPI + Mock + Factory + `refreshRatesFromApi` @BizMutation **真实外部 API 客户端代码**（非纯文档）。`:274` 草案审查 iter3 `pending`；`:357` `Auditor / Agent: pending（独立子代理新会话执行关闭审计；执行者未自我审计）`；但 `:3` `Plan Status: completed` + `:286` Closure Gate `[x] 结束审计由独立子代理（新会话）执行` 假勾选。**部署/外部集成保护区域 plan-first 工作被标记完成但必需的独立草案审查 + 结束审计均 `pending`。** 功能正确性后被兄弟计划 `2026-07-26-1407-3` 的 closure audit 间接确认，但 1206-3 自身的强制审计三件套从未闭合。

**正面样本**：`2026-07-04-1115-3`（logistics carrier SPI + webhook）、`2026-07-04-2200-1`（b2b EDI + MFT）、`2026-07-04-2200-2`（contract e-signature SPI）、`2026-07-05-0306-1`（scheduler.yaml 7 job）、`2026-07-06-0504-1`（notify 子系统）、`2026-07-06-0642-1/2`（通知消费者 + 审批通知）、`2026-07-26-1407-3`（汇率 API client E2E）均 TRIPLE_COMPLETE，且 1115-3 的 closure audit 还捕获并修复了 FREIGHT 字典漂移。

### 2.7 区域六：`auth/permissions`（plan-first）— 4 份

**Verdict: 3 TRIPLE_COMPLETE + 1 低危 MISSING_PLAN_AUDIT。**

**MISSING_PLAN_AUDIT（1 份，低危）**：

- `2026-07-22-0444-3-frontend-f14-menu-action-auth-reconciliation.md`：`:258` `Auditor / Agent: _待独立结束审计_` 但 gates `[x]`。该计划仅编辑 action-auth.xml **菜单结构**（TOPM/SUBM 可达性/orderNo/grouping），显式 Non-Goal 排除「action-auth.xml 除菜单可达性外的角色/资源权限映射」（`:30,246-250`）——是 RBAC 相邻而非核心保护区域修改，严重性低于 1206-3。

**正面样本**：`2026-07-11-1643-3`（roles-permission-mapping，纯文档，正确跳过 build/test Gate）、`2026-07-05-1838-1`（sales-credit-control SPECIAL_APPROVAL 真实权限代码 `context.getActionAuthChecker().isPermitted(...)` + 6 测试含权限授予/拒绝/null-checker 安全默认）、`2026-07-29-1410-1`（MA6 A6.1-A6.3 审计本身）均 TRIPLE_COMPLETE。

> 注：auth/permissions 的**运行时技术正确性**（权限注解完整性 / SoD / 数据权限）已由 A6.1-A6.3 审计，产出 P1-MA3-046 / P1-MA6-001 / P1-MA6-002。A6.4 仅审计**过程纪律**（计划是否有证据三件套），与 A6.1-A6.3 不同审计轴，无 finding 重叠（见 §5 去重）。

## 3. 多维度裁决（`multi-dimensional-audit-prompt.md` 7 维度）

- **维度 1 需求正确性**：审计对象（保护区域纪律）目标不偏离——6 区域定义来自 `ai-autonomy-policy.md`（人工维护，AI 不得放宽），A6.4 仅核实「已定义区域是否被遵守」不裁决「区域定义是否应改」（Non-Goal 合规）。无发现。
- **维度 2 owner-doc 对齐**：保护区域定义 + 必需证据规则在 `ai-autonomy-policy.md:67-74` 明确；`project-context.md §AI 阻塞条件` 重申硬停止；`00-plan-authoring-and-execution-guide.md §计划决策表` 明确契约/数据/模型/API/认证/权限类须完整计划 + 独立审计。维度无矛盾。无发现。
- **维度 3 架构或边界影响**：保护区域触及即跨模块/跨层高风险——ORM 变更驱动 codegen、财务过账影响 GL、外部集成引入外部依赖。§2 已按区域逐一核实证据。无独立新发现。
- **维度 4 验证充分性**：核心发现——14 项违规计划的「关闭门控是否真的可验证」答案是**否**（1206-3 / 1206-2 / P0-MA2-019 / P0-MA2-020 等的 Closure Audit Evidence 为 `pending`/`self-audit`/占位符，但 Plan Status `completed` + Gates `[x]`）。这构成 P1-MA6-003。
- **维度 5 回归风险**：14 项违规中代码均已落地且 `mvn test` 全绿（无活跃数据破坏），但**审计轨迹缺失**意味着未来回归无独立基线可参照。回归风险为「过程风险」非「运行时风险」。无 P0。
- **维度 6 路由和技能选择正确性**：抽样 6 份 P0 fix plan 的 Task Route / Skill Selection 均正确（`implementation-only change` + `nop-backend-dev` + ORM ask-first 声明）。审计计划本身（本 plan）Task Route = `verification or audit work` + Skill = `multi-dimensional-audit-prompt.md` 合规。无发现。
- **维度 7 待办或自主权策略漂移**：**关键发现**——`plan-first`/`ask-first` 工作被当作 `implement` 执行而无审计（P1-MA6-003）；closure 门控假勾选（`[x]` + `Auditor: pending`）是系统性第三波（P1-MA6-004）。无静默 deferred 降级（B.5 核实：所有 `Deferred But Adjudicated` 均含 Classification + Why Not Blocking + Successor Required + 触发条件）。

**项目特定维度补充**：

- **ORM 完整性**：36 份 ORM 变更计划中无 propId 断续 / 表前缀双重拼接 / dict 漂移（这些由 MA1 ORM 审计已捕获并登记 MR1）。
- **代码生成纪律**：无 `_gen/` / `_` 前缀 / `_app.orm.xml` 手编证据（合规）。

## 4. Findings 分类

### 4.1 P0（即时通道）

**零 P0。** 所有违规均为过程纪律缺口（审计轨迹不完整），代码已落地且 `mvn test` 全绿，无活跃数据破坏路径。按 roadmap 规则 6「P0 永不进入 MR 批量修复——即时通道是 P0 唯一合法修复路径」之反面，过程纪律缺口不构成 P0。

### 4.2 P1（目标 MR3）

#### P1-MA6-003：ORM ask-first 保护区域计划缺失独立 plan-audit/closure（5 份）

| 字段 | 值 |
| --- | --- |
| 报告 | ma6-protected-area-discipline |
| 域 | finance / manufacturing / aps / inventory / cross-domain |
| 描述 | 5 份计划触及 `model/*.orm.xml`（ask-first 最高级保护区域，新增实体/UK/字段/tagSet 移除）但关闭时无独立结束审计（执行者自查 / mission-driver 单会话 / 无 Draft Review Record）：①`2026-07-21-1206-2`（finance 4 字段+2 实体+3 字典，`:364` 执行者自查）②`2026-07-22-1000-2`（mfg+drp 6 实体+4 字典，`:274` 主代理自查）③`2026-07-24-2200-1`（10 orm.xml 移除 38 行 use-approval，`:375` mission-driver 本会话）④`2026-07-28-1249-arm-fix-p0-ma2-019`（aps 新实体+UK，无 Draft Review Record，`:94` 主代理 EXECUTE）⑤`2026-07-28-1249-arm-fix-p0-ma2-020`（inventory 7 列自然键 UK，无 Draft Review Record，`:94` self-audit）。违反 AGENTS.md 规则 12（保护区域须人工/子代理审查或保持阻塞）+ `ai-autonomy-policy.md §保护区域`（ask-first 须 design doc + plan audit）+ `00-plan-authoring-and-execution-guide.md` 规则 12（结束审计不得执行者自查）。**P1 非 P0**：代码已落地 + `mvn test` 全绿 + 无活跃数据破坏；缺陷是审计轨迹缺口非运行时缺陷。根因：P0 即时通道 + deepening 阶段计划在 MISSION_DRIVER 时间压力下将 closure-audit 推迟至「后续 OPEN_AUDIT」但未跟踪。 |
| 目标 MR | MR3 |
| 修复状态 | todo |

#### P1-MA6-004：deployment/external-integration + auth 保护区域计划缺失独立 plan-audit/closure（2 份）

| 字段 | 值 |
| --- | --- |
| 报告 | ma6-protected-area-discipline |
| 域 | master-data（external API）/ finance（GL mapping）/ frontend（action-auth menu） |
| 描述 | ①`2026-07-21-1206-3`（deployment/external-integration plan-first）：新增真实外部 API 客户端代码（`IErpMdExchangeRateApiClient` SPI + `refreshRatesFromApi` @BizMutation），`:274` 草案审查 iter3 `pending` + `:357` Closure Auditor `pending`，但 `:3` Plan Status `completed` + `:286` Closure Gate 假勾选 `[x] 结束审计由独立子代理执行`。②`2026-07-24-1351-1-gl-mapping-provider-rollout`（finance GL mapping plan-first）：`:197` `Auditor / Agent: pending independent closure audit` 但 Plan Status `completed`。（低危相邻：`2026-07-22-0444-3` action-auth.xml menu 结构 `:258` closure pending——非核心 RBAC，列为 P1-MA6-004 子例。）违反保护区域 plan-first 必需证据规则。**P1 非 P0**：功能正确性后被兄弟计划间接确认（1206-3 由 1407-3 closure 确认）；缺陷是审计轨迹缺口。 |
| 目标 MR | MR3 |
| 修复状态 | todo |

#### P1-MA6-005：系统性第三波 closure-pending「completed」计划（~16 份超集）

| 字段 | 值 |
| --- | --- |
| 报告 | ma6-protected-area-discipline |
| 域 | 全域（docs/plans/） |
| 描述 | Round 1（`2026-07-14-1449-1`）清理 24 份 + Round 2（`2026-07-17-0900-1`）清理 2 份 deficient plan 后，grep 又浮现 ~16 份 `completed` 计划带 `Auditor: pending`/`self-audit`/`<待…>` 占位——含 P1-MA6-003/004 的保护区域子集 + 非保护区域项（`2026-07-03-2108-1` dict refactor / `2026-07-22-0845-3` / `2026-07-19-2200-2` / `2026-07-20-2059-3` / `2026-07-13-1419-1` / `2026-07-10-1800-1` / `2026-07-14-0215-1` / `2026-07-14-1218-1` / `2026-07-12-1321-2` / `2026-07-29-0749-2` MA4 A4.8 / `2026-07-28-2130-1` MA3 A3.8）。`0900-1:48` 显式声明 OPEN_AUDIT 形式化仍 Deferred。P1-MA6-003/004 是此超集的高优先级保护区域切片。 |
| 目标 MR | MR3 |
| 修复状态 | todo |

### 4.3 P2（watch-only）

#### P2-MA6-001：6 份 ORM 变更计划依赖「草案审查预授权」缺显式 ask-first 人工确认记录

| 字段 | 值 |
| --- | --- |
| 报告 | ma6-protected-area-discipline |
| 域 | inventory / aps / hr / contract / mfg / finance |
| 描述 | 6 份计划（`2026-07-02-0700-1` inventory-trace-chain / `2026-07-04-0831-1` aps-scheduling / `2026-07-04-0831-3` hr-shift / `2026-07-04-2200-2` contract-e-signature / `2026-07-05-0427-1` demand-forecast / `2026-07-05-0540-1` bad-debt-provision）修改 orm.xml 但无显式「人工确认/人工批准」Closure Gate checkbox，依赖「草案审查预授权加性扩展」。按 `ai-autonomy-policy.md`「AI 编写或修改的文档不能作为放宽自主权证据除非人工明确批准」，此预授权属可争议。**实质纪律已遵循**（均有独立草案审查 + 独立结束审计 + session id），仅缺显式 ask-first 人工确认记录。严重性低于 P1-MA6-003。 |
| 目标 MR | MR3（文档卫生，顺手收敛） |
| 修复状态 | todo |

### 4.4 正面合规样本（纪律生效证据，非遗规）

- **P0-MA2-018 字面 UK 被 plan-audit 阻断**：独立 plan-audit 捕获会回归的字面 UK 设计（红冲/多账套/软删除三重契约冲突），plan 正确置 `deferred` 等候人工裁决——**保护区域纪律的最佳证据**。
- **finance postings 56 份 100% 合规**：业财端到端 + core-business M1 + M4 业财一体 + 6 项 P0 fix 全部证据三件套齐全，独立会话卫生严格。
- **`2026-07-08-0056-1`（7 域 posted/businessDate）**：ask-first 纪律黄金样本，7 轮 mission-driver 迭代显式辩论并单独记录 ORM 门控。
- **`2026-07-29-1430-2`（MA5 跨切）**：诚实留 2 项 closure gate `[ ]`（因独立审计未运行），是正确的门控诚实样本（非违规）。
- **Deferred 诚实度强**：所有 `Deferred But Adjudicated` 含 Classification + Why Not Blocking + Successor Required + 触发条件，无静默降级。

## 5. 与 MA1-MA5 + 同批 A6.1-A6.3 已登记发现交叉去重

A6.4 是**过程纪律**审计轴（计划是否有证据三件套），与既有 finding 不同审计轴，无 finding 重叠：

- **P1-MA3-046 / P1-MA6-001 / P1-MA6-002**（A6.1-A6.3）= auth/permissions **运行时技术正确性**（权限注解完整性 / SoD / 数据权限落地）。A6.4 = auth/permissions **过程纪律**（计划是否有证据三件套）。同保护区域，不同审计轴，**无 finding 重叠**——A6.4 不重新登记运行时 gap，仅引用。
- **P1-MA1-001~030**（MA1 ORM/平台合规）= ORM 字段/类型/命名/propId 规范性。A6.4 = ORM 变更计划是否有 ask-first 证据。不同审计轴，无重叠。
- **P1-MA2-***（MA2 业务正确性）= 状态机/业财端到端/并发/多公司业务正确性。A6.4 = 触及这些区域的计划是否有证据三件套。无重叠。
- **P1-MA3-001~061**（MA3 文档/契约/drift）= 设计文档质量/API 契约/索引路由/定制能力。A6.4 = 过程纪律。无重叠。
- **P1-MA4-*** / **P1-MA5-*** = 代码质量/测试覆盖。A6.4 = 过程纪律。无重叠。

**MA6 累计 P1 = 5**（A6.1 0[合并入 P1-MA3-046] + A6.2 1[P1-MA6-001] + A6.3 1[P1-MA6-002] + A6.4 3[003/004/005]），**P2 = 1**（A6.4 1[P2-MA6-001]）。

## 6. 结论

保护区域纪律**总体强健但存在近期退化**：

1. **finance postings（最高密度区域）100% 合规** + **P0-MA2-018 被 plan-audit 正确阻断** = 纪律在高风险区域生效的最佳证据。
2. **ORM ask-first 区域违规密度 33%**（12/36），且违规集中于 2026-07-21 之后的 deepening 阶段 + P0 即时通道 fix plan（P0-MA2-019/020 完全跳过独立草案审查）——MISSION_DRIVER 时间压力下 closure-audit 被推迟至「后续 OPEN_AUDIT」但未跟踪。
3. **系统性第三波 closure-pending「completed」计划**（~16 份超集）是 P1-MA6-003/004 的根因——round-1/round-2 清理未覆盖，OPEN_AUDIT 形式化仍 Deferred。
4. **零活跃数据破坏**——所有违规计划的代码已落地且 `mvn test` 全绿；缺陷是审计轨迹缺口，目标 MR3 批量修复（补独立结束审计或登记为「审计不可追溯」已知简化）。

**A6.4 完成 → MA6 里程碑（A6.1-A6.4）全部 done/ready**。
