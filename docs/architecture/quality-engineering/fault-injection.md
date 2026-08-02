# 故障注入测试（MQ Q4）—— Phase 1 设计文档

> Owner Doc for Milestone MQ Q4（故障注入测试）
> 创建日期：2026-08-01
> Plan：`docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`
> 单一真相源依赖：本文档是 MQ 文档先行工作流 **Phase 1** 产物（设计/策略文档），**不实现任何代码/ORM/CI 变更**。Phase 2 实现 plan（harness 落地 + 6 域过账悬挂路径故障注入测试）须在本文档审查收敛后方可起草。
> 上游真相源（**只引用**，不重推导，避免双真相源漂移）：
> - `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q4（line 677 工作项表 + line 787 维度说明 + §横切关注点 §文档先行工作流 line 843-862）
> - `docs/architecture/quality-engineering/README.md`（Q0 范围矩阵 + 复杂度分级 + 实施顺序裁决基线，Q4 位 3，Q1↔Q4 协同）
> - `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md` §Current Baseline（Q4 NOT FOUND 实仓证据已核验：零故障注入基础设施）
> - `docs/design/finance/posting-log.md` §错误传播分级策略 G1-G4（**过账错误传播 + 延迟重试模型权威真相源**——Q4 可恢复性断言契约的对齐基准）
> - `docs/audits/arm-index.md`（finding ID 权威索引——§1.3 消解 qa 064-vs-080 漂移的核对源）
> - MR1.16 单点修复证据（P1-MA2-032/048/060/068/074 + P1-MA4 系列——Q4 是其系统性回归保护超集）
> - sibling plan `docs/plans/2026-08-01-1158-2-mq-q1-mutation-testing-design-doc.md`（Q1，Q1↔Q4 协同——Q1 盲区类即 Q4 优先覆盖路径）

## 1. 现状评估

> 本节**引用**（非重推导）上游真相源已核验事实，每条标注可复现核验命令 + 核验日期，便于 Phase 2 plan 与独立审查复核。证据核验日期：2026-08-01（HEAD 含 R6.9 收口）。

### 1.1 无通用化故障注入 harness（关键字零命中）

ERP 全仓无任何通用化故障注入基础设施——无 `FaultInjector` 抽象、无 `@InjectFault` 注解、无跨域可恢复性断言契约。

- 核验命令（2026-08-01 复核零命中）：`rg -il "fault.injection|FaultInjector|@InjectFault" --glob '*.java' --glob '*.xml'`（工作目录 = nop-app-erp）→ **EXIT=1（零命中）**。覆盖范围：nop-app-erp 工作树内全部 Java + XML——无故障注入 harness 抽象、无注解、无配置。
- 引用源：roadmap line 697 + line 787（Q4 维度说明）；Q0 README §范围矩阵 §Q4（核验日期 2026-08-01）；Q0 plan §Current Baseline NOT FOUND 证据第 4 条。
- **重要边界（既有 per-point 先例存在，非"绝对空白"）**：上述关键字零命中证明**无通用化 harness**，但**不代表零故障测试**——本仓已有 **per-point 过账悬挂测试先例**（散落各域，非通用化抽象）。§1.2 枚举这些先例作为路径 A（应用层 stub/override）的可行性与起点证据。

### 1.2 per-point 过账悬挂测试先例（路径 A 的起点证据，非通用化 harness）

本仓已有 **7 个 per-point 过账悬挂/失败告警测试**，散落各域，用确定性桩（Proxy 桩 `IErpFinVoucherBiz.post` 抛异常 / 子类 override `*PostingExecutor.postEvent` 抛异常 / Proxy 桩 `IErpSysNotificationBiz` 录制告警）诱导过账失败，断言悬挂或告警。这些是**路径 A（应用层 test-scope stub/override）已证的可行性起点**——Q4 的任务是把它们**泛化**为统一 harness + 跨域可恢复性断言契约。

| 测试类 | 域 | 桩机制 | 断言 | 引用 finding / 计划 |
|--------|----|--------|------|---------------------|
| `TestErpInvPostingDispatcherFailureHangs` | inventory | Proxy 桩 `IErpFinVoucherBiz.post` 抛 `NopException` + 子类 override `InvPostingExecutor.postEvent` 抛 | tryPost 返回 null（posted=false 悬挂） | plan `2026-07-31-0744-3` P1-MA4-021(b) |
| `TestErpPurPostingDispatcherFailureHangs` | purchase | Proxy 桩 `IErpFinVoucherBiz.post` 抛 | posted=false 悬挂 | R1.16 |
| `TestErpSalPostingDispatcherFailureHangs` | sales | Proxy 桩 `IErpFinVoucherBiz.post` 抛 | posted=false 悬挂 | R1.16 |
| `TestDepreciationPostingFailureAlert` | assets | Proxy 桩 `IErpSysNotificationBiz` 录制告警 + null 桩优雅跳过 | `ast.depreciation-posting-failure` 告警派发 | R1.16 / plan `2026-07-30-0341-2` P1-MA4-013 |
| `TestErpInvLandedCostReverseFailureAlert` | inventory | 子类 override `InvPostingExecutor` | reverse 失败告警 | R1.16 |
| `TestTimesheetPostingFailureAlert` | projects | Proxy 桩（告警录制） | 过账失败告警 | R1.16 |
| `TestErpMfgVarianceRecomputeReversal` | manufacturing | 子类 override `MfgPostingExecutor`（`ThrowingMfgPostingExecutor extends MfgPostingExecutor`） | 红冲失败容错 + 孤儿凭证风险可观测 | plan `2026-07-18-2251-1` |

- 核验命令（2026-08-01 复核命中）：`rg -l "FailureHangs|FailureAlert|ThrowingMfgPostingExecutor" --glob '*Test*.java'` → 命中上述 7 个测试类（另附 2 个 incidental 命中 `TestErpMfgSubcontracting` / `TestErpHrPayrollEngine`，其类名含上述关键词但不属 per-point 过账悬挂先例，不计入）。
- **范式观察（路径 A 设计输入）**：既有先例统一用**两种桩机制**：
  1. **Proxy 桩 Facade 接口**（`IErpFinVoucherBiz` / `IErpSysNotificationBiz`）——经 `java.lang.reflect.Proxy` + `InvocationHandler`，在指定方法名（`post` / `notify`）抛异常或录制调用。无 Mockito 依赖（对齐 R1.16 范式）。
  2. **子类 override 具体类**（`*PostingExecutor`）——匿名子类或命名子类 override `postEvent` 等方法抛异常。
- **既有先例的局限（Q4 要弥补的）**：(1) 无统一 harness 抽象——每测试类各自写 Proxy/子类桩，重复样板；(2) 无跨域可恢复性断言契约——各测试只断言"悬挂"或"告警"，未对齐 `posting-log.md` G1-G4 延迟重试模型的形式化可恢复性定义；(3) 覆盖零散——finance/hr/qa/maintenance 4 域的过账悬挂路径**无** per-point 测试。

### 1.3 同型根因跨 6 域（tryPost 吞异常 → posted=false 静默悬挂）+ finding ID 漂移消解

审计反复发现 **tryPost catch-swallow → posted=false 静默悬挂**同型根因跨域。本节**核对 arm-index 权威值**，消解 roadmap line 787 / Q0 README（qa=064）与 R1.16[roadmap line 154] / Q4 工作项表[line 677]（080）之间的 finding ID 漂移。

**arm-index 权威核对（2026-08-01 复核）**：

- 核验命令：`rg -n "P1-MA2-064|P1-MA2-080" docs/audits/arm-index.md`
- `P1-MA2-064`（`ma2-quality-state-machine` / quality 域）：**「业务单据作废联动取消质检单未落地」**——owner doc §4 声明 Deferred。**不是过账悬挂** finding。
- `P1-MA2-080`（`ma2-aps-logistics-state-machine` / **logistics** 域）：**「网关异常重试耗尽 + DELIVERED 运费过账失败缺告警闭环」**——onDelivered 吞异常 + scanForPolling 不重试 DELIVERED-PENDING。**是过账悬挂 finding，但域是 logistics（第 7 域），非 qa**。
- **qa 域的过账悬挂**：在 A2.12 quality 审计（`docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`）中描述为「**MANUAL_POST NCR 过账悬挂窗口期**」，作为同型根因交叉登记（"同 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 同型根因交接"），**无独立的 P1-MA2-xxx 编号**——它是 A2.12 审计正文内的 inline 交叉引用，而非独立编号 finding。

**漂移消解结论**：

| roadmap / Q0 README 标注 | arm-index 权威值 | 裁决 |
|--------------------------|------------------|------|
| 维度说明 line 787 / Q0 README §Q4：qa = P1-MA2-064 | 064 = qa「作废联动取消质检单」（**非过账悬挂**） | **064 标注错误**——它是 qa 的另一个 finding，不是过账悬挂 |
| 工作项表 line 677：080 | 080 = **logistics**「网关异常 + 运费过账失败」 | **080 是 logistics 域**（第 7 域），非 qa |

**Q4 范围的 6 域同型根因权威清单（消解后）**：

| 域 | finding 标识 | 过账悬挂描述 | MR1.16 修复状态 |
|----|-------------|--------------|-----------------|
| finance | `P1-MA2-032` | 过账 tryPost 吞异常 → posted=false | ✅ resolved (R1.16) |
| hr | `P1-MA2-048` | SalaryPostingDispatcher 吞异常 | ✅ resolved (R1.16) |
| assets | `P1-MA2-060` | DepreciationPostingDispatcher 吞异常 | ✅ resolved (R1.16) |
| qa | **A2.12「MANUAL_POST NCR」（无独立编号）** | NCR 过账 MANUAL_POST 悬挂窗口期 | ✅ resolved (R1.16，AUTO_POST 默认经事务回滚覆盖) |
| projects | `P1-MA2-068` | TimesheetPostingDispatcher 吞异常 | ✅ resolved (R1.16) |
| maintenance | `P1-MA2-074` | Mnt dispatcher 吞异常 | ✅ resolved (R1.16) |

> **logistics（P1-MA2-080）边界声明**：logistics 是同型根因的**第 7 个成员**（roadmap 工作项表 line 677 将其纳入 6 域清单——实际为 7 域族）。本计划 Q4 scope 为 **6 域（finance/hr/assets/qa/projects/maintenance）**，对齐 plan §Goals「覆盖 6 域过账悬挂路径」。logistics 作为 successor 扩展（§7）——其 `ErpLogShipmentBizModel.onDelivered` 吞异常 + `scanForPolling` 不重试 DELIVERED-PENDING 比 peer dispatcher 更严重（无 finance sweep 兜底），但 Q4 首轮 harness 沉淀后扩展更可控。
>
> **[2026-08-02 更新] logistics successor 已闭合**（plan `2026-08-02-1500-1`）：`onDelivered` 失败路径补 `IErpSysNotificationBiz.notify` 告警派发（`log.freight-posting-failure`，对齐 6 peer G4 域范式）+ 新增 `TestLogPostingFaultInjection`（A1+A2+A4-alert）+ CI 门控 baseline 6→7。详见 §5.2 logistics 行 + §8 successor 表。

### 1.4 MR1.16 单点修复的边界（Q4 是其系统性回归保护超集）

MR1.16 已对 6 域同型根因做**单点修复**：catch 收窄 + `IErpSysNotificationBiz` 告警派发（G4 域）+ 不进死状态 + 期末结账前置检查扩展。但**无系统性回归保护**——同型根因在**新增过账路径**时无故障注入测试拦截。

- 引用源：roadmap line 154（R1.16 done 记录）；`docs/design/finance/posting-log.md` §错误传播分级策略 G1-G4（R1.16 对齐的分级模型）。
- **Q4 = MR1.16 的系统性回归保护超集**：MR1.16 修了**已知的 6 个点**；Q4 建 harness 使**未来新增的过账路径**也须通过故障注入 + 可恢复性断言，防止同型根因回潮。

### 1.5 过账错误传播权威真相源（posting-log.md G1-G4 延迟重试模型）

Q4 可恢复性断言契约的对齐基准是 `docs/design/finance/posting-log.md` §错误传播分级策略，**非** `processor-extension-pattern.md`（后者仅 line 66 一处 `@SingleSession` 示例顺带提及过账，无过账失败回退规则）。

**G1-G4 分级 taxonomy**（引用 `posting-log.md` §错误传播分级策略 line 102-116）：

| 分级 | 含义 | 处置规则（延迟重试模型） |
|------|------|--------------------------|
| **G1 瞬时可重试** | 基础设施抖动 / 锁竞争 | 经 finance sweep（`ErpFinPostingException` 工作台）PENDING→retry；retryCount < MAX_RETRY 自动重选 |
| **G2 永久性失败** | 配置缺失 / Provider 固定抛错 | retryCount ≥ MAX_RETRY(3) 升级 **MANUAL** 终态（**非 RETRYING 死状态**）+ `IErpSysNotificationBiz` 告警 |
| **G3 编排层跨域/异步失败** | 期末结账/完工/委外编排层 | catch 收窄：「impl 未就绪」容错跳过告警；「配置错误/真实故障」阻断或进异常工作台 + 告警 |
| **G4 无 sweep 覆盖的域 dispatcher** | assets 折旧/logistics/hr/qa/projects/maintenance dispatcher | `IErpSysNotificationBiz` 告警 + owner doc 自愈路径；**期末结账前置检查兜底仅覆盖 assets/logistics/inv**（posting-log.md §期末结账前置检查覆盖矩阵 line 127-136），hr/qa/projects/maintenance 经告警 + 期末试算平衡人工发现（line 136 明示「不纳入前置检查」） |

> **类名核对（R2-MINOR-2 修正）**：`posting-log.md` 沿用概念名 `DeferredPostingSweepJob`，但实仓**无此类**——经抽取重构为 `ErpFinPostingExceptionRecorder`（异常记录持久化）+ `ErpFinDeferredPostingRetryHelper`（重试辅助）+ `ErpFinPostingExceptionRetryProcessor`（重试 Processor）。Phase 2 核验命令须用实仓类名，非概念名。核验：`rg -l "class DeferredPostingSweepJob"` 零命中；`rg -l "ErpFinPostingExceptionRecorder|ErpFinDeferredPostingRetryHelper" --glob '*.java'` 命中。

> **关键（契约对齐基准）**：Q4 可恢复性断言契约（§4）形式化定义「故障后系统进入可恢复状态而非静默悬挂」时，**对齐 G1-G4 延迟重试模型**：G1→PENDING 可重试；G2→MANUAL 终态 + 告警；G4→告警（+ assets/logistics 有期末前置检查兜底，hr/qa/projects/maintenance 仅告警 + 人工试算平衡）。**非**「显式文档状态回退」——`posting-log.md` 权威：失败必留异常记录/告警，但「可恢复」的形式是「posted 标志与实际过账结果一致 + 异常可观测 + 单据可重试/可冲销或进异常工作台」。

## 2. 目标与非目标

### 2.1 目标（Phase 1 = 本文档；Phase 2 实现见 §5）

1. **裁决故障注入机制的技术选型**（路径 A 应用层 stub/override / B 平台层字节码插桩 / C JUnit 5 Extension），给出候选、考虑的替代、残留风险三要素——满足 plan authoring guide §规则 9（Decision 项记录理由）。
2. **盘点真实过账 SPI 注入点**（§3.1），作为路径 A stub/override 的精确注入点清单——纠正 roadmap line 787 / Q0 README 沿用的误导名 `IPostingDispatcher`（实仓不存在）。
3. **形式化定义可恢复性断言契约**（§4）——「故障后系统进入可恢复状态而非静默悬挂」对齐 `posting-log.md` G1-G4 延迟重试模型。
4. **为 Phase 2 实现 plan 提供实施契约**（§5）：harness 落地位置 + 6 域过账悬挂路径覆盖清单 + 与 Q1 盲区类清单的消费方式。
5. **裁决 CI 门控形态**（§6）：故障注入测试是否纳入 mandatory 回归层 + 与 `.github/workflows/compliance.yml` 集成。
6. **声明与 Q1 的协同接口**（§8）：消费 Q1 输出的盲区类清单，作为 Q4 Phase 2 优先覆盖目标。

### 2.2 非目标

- **不实现任何代码/ORM/CI 变更**——本文档仅产出设计。Phase 2 实现（harness + 6 域测试）是独立后续 plan，须在本文档审查收敛后方可起草（MQ 文档先行工作流硬约束）。
- **不修改 `nop-entropy` 源码**——若 §3 裁决路径 A（应用层 stub/override），其实施零平台改动；路径 B（字节码插桩）若被选中其实施属 Phase 2 + 须遵守 AGENTS.md nop-entropy 日志规则（但 §3 裁决否决路径 B）。
- **不覆盖全 mutation 路径**——Q4 首轮聚焦 6 域过账悬挂同型根因；其余故障模式（并发冲突 / 超时 / 外部集成失败）作为 successor（§7）。
- **不编写 Q1 变异测试设计**——同批独立 sibling plan。
- **不重新推导 NOT FOUND 证据**——§1 引用 Q0 README + roadmap + arm-index + posting-log.md，避免双真相源。
- **不动用 `nop-testing` skill 写测试**——本期纯设计文档；`nop-testing`（JunitAutoTestCase/快照）留待 Phase 2 实现 plan。
- **不覆盖 logistics（P1-MA2-080）**——logistics 是同型根因第 7 域，作为 successor 扩展（§7），不在 Q4 首轮 6 域 scope。

## 3. 过账 SPI 注入点盘点 + 技术选型

### 3.1 过账 SPI 注入点盘点（路径 A 的精确注入点清单）

> roadmap line 787 / Q0 README 沿用 `IPostingDispatcher` 作为过账 SPI 名——**实仓核验：`IPostingDispatcher` 接口不存在**（`rg -l "IPostingDispatcher" --glob '*.java'` 零命中）。真实过账 SPI 是**各域具体类**（无公共接口抽象），分三类。路径 A 须 stub/override 这些**各域具体类**。

**类一：各域具体 `*PostingDispatcher`（过账派发器，per-business-type）**

每域有多个具体 dispatcher（per business-type），均无公共接口，直接 stub/override 具体类。Q4 首轮 6 域目标 dispatcher：

| 域 | 具体 dispatcher 类 | 数量 |
|----|---------------------|------|
| finance | `NotesPostingDispatcher` / `EmployeeAdvancePostingDispatcher` / `ExpenseClaimPostingDispatcher` | 3 |
| hr | `SalaryPostingDispatcher` | 1 |
| assets | `DepreciationPostingDispatcher` / `CapitalizationPostingDispatcher` / `DisposalPostingDispatcher` / `ValueAdjustmentPostingDispatcher` / `AssetSplitPostingDispatcher` / `AssetMergePostingDispatcher` / `AssetInventoryPostingDispatcher` / `MaintenanceCapitalizationPostingDispatcher` / `MaintenanceExpensePostingDispatcher` | 9 |
| qa | `NcrPostingDispatcher` | 1 |
| projects | `TimesheetPostingDispatcher` / `ProjectSettlementPostingDispatcher` | 2 |
| maintenance | `MaintenanceLaborPostingDispatcher` / `MaintenanceIssuePostingDispatcher` | 2 |

- 核验命令（2026-08-01 复核）：`rg -l "class \w+PostingDispatcher" --glob '*.java' --glob '!*Test*'` → 命中上述各域具体类（全域共 **30 个生产 dispatcher**，含 inv/sal/pur/mfg；Q4 首轮聚焦 6 域 18 个。注：不带 `--glob '!*Test*'` 时另匹配 3 个 `Test*FailureHangs` 测试类，非生产 dispatcher）。

**类二：各域具体 `*PostingExecutor`（过账执行器，per-domain 共享）**

每域 1 个 executor，被该域多个 dispatcher 共享，承载 `postEvent(PostingEvent)` 等方法。子类 override 抛异常是既有先例（§1.2 `ThrowingMfgPostingExecutor`）：

| 域 | 具体 executor 类 |
|----|-------------------|
| finance | `FinPostingExecutor` |
| hr | `SalaryPostingExecutor` |
| assets | `AssetPostingExecutor` |
| qa | `NcrPostingExecutor` |
| projects | `ProjectPostingExecutor` |
| maintenance | `MntPostingExecutor` |

- 核验命令（2026-08-01 复核）：`rg -l "class.*PostingExecutor" --glob '*.java'` → 命中上述各域具体类。

**类三：finance facade `IErpFinVoucherBiz`（凭证聚合根 Facade，跨域过账入口）**

所有域 dispatcher 最终经 `IErpFinVoucherBiz.post(...)` Facade 调用 finance 凭证引擎（对齐 `processor-extension-pattern.md` 硬规则 2「跨域注入 IErpXxxBiz」）。Proxy 桩此接口的 `post` 方法抛异常是既有先例（§1.2 inventory/purchase/sales 三个 FailureHangs 测试）。

- 核验命令（2026-08-01 复核）：`rg -l "IErpFinVoucherBiz" --glob '*.java'` → 命中全域 dispatcher + executor 引用此 Facade。
- **注入点裁决**：`IErpFinVoucherBiz.post` 抛异常 = 模拟「finance 凭证引擎宕机」（G1/G2 故障）；具体 `*PostingExecutor.postEvent` 抛异常 = 模拟「域编排层故障」（G3 故障）；`IErpSysNotificationBiz.notify` 录制 = 验证告警闭环（G4 断言）。三类注入点互补，覆盖 G1-G4 分级。

### 3.2 路径 A —— 应用层 test-scope stub/override 各域具体类 + Facade

**机制**：复用 §1.2 既有先例的两种桩机制，泛化为统一 harness：
1. **Proxy 桩 Facade 接口**（`IErpFinVoucherBiz` / `IErpSysNotificationBiz`）——经 `java.lang.reflect.Proxy` + `InvocationHandler`，在指定方法名抛异常 / 录制调用 / 返回受控值。无 Mockito（对齐 R1.16 范式）。
2. **子类 override 具体类**（`*PostingDispatcher` / `*PostingExecutor`）——匿名子类 override `tryPost` / `postEvent` / `dispatchIfApplicable` 抛受控异常。

**优点**：
- **既有 per-point 先例已证可行**（§1.2 枚举 7 个测试，inventory/purchase/sales/assets/projects/mfg 6 域均已落地此范式）——Q4 是泛化而非探索。
- **应用层内闭环**：全部在 `module-*/erp-*-service/src/test` 测试代码，零 nop-entropy 改动，无跨仓库 / 升级耦合，Phase 2 交付不被平台 PR 阻塞。
- **保真度高**：stub 的是真实 SPI 边界（`IErpFinVoucherBiz.post` 是生产代码真实跨域调用点），故障经真实 dispatcher catch 路径传播，验证真实可恢复性行为（而非模拟框架的合成行为）。
- **与 Nop 测试栈兼容**：纯 JUnit 5 + 反射 Proxy，不与 `JunitAutoTestCase` 快照语义冲突（故障注入测试断言状态/告警，不依赖快照录制）。

**缺点 / 风险**：
- **per-dispatcher stub 工作量**：6 域 18 个 dispatcher 须逐一可注入故障。但既有先例证明每 dispatcher 的 stub 是机械的（Proxy handler 复用 + helper），harness 抽象后样板收敛。
- **field 注入可见性**：既有先例直接 `dispatcher.voucherBiz = throwingStub`（package-private field）。harness 须确认各 dispatcher 的 `voucherBiz` / `notificationBiz` / `executor` field 可见性（部分可能须反射 set 或同包测试）。Phase 2 须逐 dispatcher 核验。
- **真实链路保真度的边界**：Proxy 桩 Facade 在调用边界抛异常，与真实 GL 引擎内部失败的传播路径可能不完全一致（如真实失败可能经 `ErpFinPostingException` 持久化 + sweep 重试，而 Proxy 桩在 Facade 边界即抛）。§4 可恢复性断言契约须区分「Facade 边界故障」（路径 A 可注入）与「引擎内部故障」（须端到端集成测试，超出 Q4 unit harness 范围）。

### 3.3 路径 B —— 平台层字节码插桩

**机制**：在 nop-entropy 平台层或应用层 build 时用字节码插桩（ByteBuddy / ASM agent）在过账链路方法入口注入受控异常。

**优点**：
- 可注入任意内部方法（不限于 SPI 边界），故障点更细粒度。

**缺点 / 风险**：
- **跨仓库改动 / 升级耦合**：若触及 nop-entropy，须平台 PR + 独立 CI / 发布节奏对齐，Phase 2 交付被阻塞（与 Q6 路径 A 同源的否决理由）。
- **回归面大**：字节码 agent 改变全域类加载行为，回归测试成本高、与 Nop 反射/XPL 动态分发潜在交互（同 Q1 pitest R3 风险）。
- **过度工程**：Q4 目标是验证过账悬挂可恢复性（SPI 边界故障已足够触发 dispatcher catch-swallow 路径），无需引擎内部细粒度故障注入。

### 3.4 路径 C —— JUnit 5 `Extension` + 受控异常/超时/事务回滚注入点

**机制**：自定义 JUnit 5 `Extension`（`InvocationInterceptor` 或 `BeforeTestExecutionCallback`），在测试方法执行前后注入受控状态（设置故障标志 / 替换 bean / 触发超时）。

**优点**：
- 轻量，JUnit 5 原生，与现有测试基类（`JunitAutoTestCase` / `@RegisterExtension`）集成自然。
- 可声明式标注（`@InjectFault(domain="finance", method="post", type=EXCEPTION)`）。

**缺点 / 风险**：
- **注入点覆盖范围受限**：JUnit Extension 在测试生命周期层操作，仍须委托到路径 A 的 Proxy/子类 stub 才能实际注入故障到 dispatcher——Extension 是**编排层**（管理故障何时触发），路径 A 的 stub 是**注入层**（故障如何生效）。二者互补非互斥，路径 C 不能独立替代路径 A。
- **事务回滚注入**：`@Transactional` 回滚由 nop-entropy IoC + `@BizMutation` 管理（自动包装事务），JUnit Extension 难以在事务边界注入受控回滚——须经路径 A stub 制造会触发回滚的异常，而非直接注入回滚。

### 3.5 裁决（Decision）

> 决策输入：§1 现状（无通用化 harness 但 7 个 per-point 先例已证路径 A 可行 + 6 域同型根因 + MR1.16 单点修复无系统性保护）+ §3.1 SPI 注入点盘点（真实 SPI = 各域具体类，`IPostingDispatcher` 不存在）+ §3.2-3.4 三路径优缺点 + Q0 README §复杂度分级（Q4 平台依赖中-高）+ AGENTS.md「应用层闭环优先，不动平台」。

**裁决：选路径 A（应用层 test-scope stub/override 各域具体 `*PostingDispatcher`/`*PostingExecutor` + finance facade `IErpFinVoucherBiz` / `IErpSysNotificationBiz`）作为主路径。路径 B（字节码插桩）否决。路径 C（JUnit 5 Extension）作为路径 A 的编排层补充（可选，非必需）。**

**裁决理由**：

1. **既有先例已证可行 + 应用层闭环（AGENTS.md 决策顺序）**：§1.2 枚举的 7 个 per-point 测试已用路径 A 的两种桩机制（Proxy 桩 Facade + 子类 override executor）覆盖 6 域。Q4 是**泛化既有范式**为统一 harness + 跨域可恢复性断言契约，而非探索新技术。路径 A 全部在应用层测试代码，零 nop-entropy 改动，无跨仓库 / 升级耦合（与 Q6 路径 C 同源的应用层闭环优先原则）。
2. **路径 B 跨仓库耦合 + 过度工程**：Q4 目标是验证过账悬挂可恢复性，SPI 边界故障（Facade `post` 抛异常）已足够触发真实 dispatcher catch-swallow 路径。引擎内部细粒度故障注入（路径 B）超出 Q4 unit harness 范围，且引入平台 PR 阻塞 + 字节码 agent 回归面。否决。
3. **路径 C 是编排层补充非独立替代**：JUnit 5 Extension 可提供声明式故障触发（`@InjectFault`）+ 与 `@RegisterExtension` 集成，但实际注入仍委托路径 A 的 Proxy/子类 stub。Phase 2 可选采用 Extension 作 harness 的触发 API（降低样板），但 harness 的核心注入机制是路径 A。
4. **保真度对齐 G1-G4**：路径 A stub 的是真实 SPI 边界，故障经真实 dispatcher catch 传播，验证真实可恢复性行为。Facade 边界故障（`IErpFinVoucherBiz.post` 抛）对应 G1/G2（引擎宕机）；executor 边界故障（`postEvent` 抛）对应 G3（编排层）；告警录制（`IErpSysNotificationBiz.notify`）验证 G4 闭环。三类注入点覆盖 G1-G4 分级。

**考虑的替代（记录为何否决）**：

- **路径 B（平台层字节码插桩）**：否决——跨仓库 / 升级耦合 + 过度工程 + 回归面大。保留为 successor 候选（触发：Q4 首轮证明 SPI 边界故障不足以覆盖某类可恢复性场景，需引擎内部细粒度故障注入时）。
- **路径 C 作主路径**：否决作为独立主路径——Extension 仍须委托路径 A stub，不能独立替代。保留为路径 A 的可选编排层补充。
- **Mockito `mock(...)` / `@MockBean`**：评估后否决。(1) 既有先例（§1.2）统一用反射 Proxy 无 Mockito（R1.16 范式），引入 Mockito 会与既有测试风格不一致；(2) `@MockBean` 替换 IoC bean 经 Spring 上下文重启，与 Nop IoC（非 Spring Boot）语义可能冲突；(3) 反射 Proxy 更轻量且已在 7 个先例中验证。否决，保持 Proxy/子类范式。
- **维持现状（仅 MR1.16 单点 + 散落 per-point 测试）**：否决——新增过账路径时无故障注入拦截，同型根因回潮无预警（§1.4 边界）。

**残留风险**：

- **R1（per-dispatcher field 注入可见性）**：既有先例 `dispatcher.voucherBiz = stub` 依赖 package-private field。harness 须确认 6 域 18 个 dispatcher 的注入 field 可见性，部分可能须反射 set 或同包测试。Phase 2 逐 dispatcher 核验。
- **R2（Facade 边界故障 vs 引擎内部故障保真度）**：路径 A 在 SPI 边界注入故障，与真实 GL 引擎内部失败（经 `ErpFinPostingException` 持久化 + sweep 重试）传播路径可能不完全一致。Q4 unit harness 覆盖「dispatcher catch-swallow 可恢复性」，引擎内部端到端故障属集成测试范畴（successor）。
- **R3（finance sweep 完整链路覆盖 = A3-integration successor）**：`ErpFinPostingException` 记录持久化（`ErpFinPostingExceptionRecorder`）+ `ErpFinDeferredPostingRetryHelper` 重试 + retry→MANUAL 升级由引擎**内部**承载，Facade 边界 Proxy 桩绕过 Recorder。Q4 unit harness 仅覆盖 A3-unit（dispatcher catch 可恢复性）；完整 sweep 链路（A3-integration）属 successor（§4.3 / §7），须更深注入点（throwing `IErpFinAcctDocProvider`）或端到端/定时任务测试。
- **R4（路径 C Extension 可选引入的复杂度）**：若 Phase 2 采用路径 C 作编排层，须评估 `@InjectFault` 注解 + Extension 的样板减少收益是否抵其复杂度。

## 4. 可恢复性断言契约

> 本节形式化定义「故障后系统进入可恢复状态而非静默悬挂」——Q4 的核心产出。对齐真相源 `docs/design/finance/posting-log.md` §错误传播分级策略 G1-G4 延迟重试模型（§1.5 引用），**非**「显式文档状态回退」。

### 4.1 反模式定义（Q4 要拦截的）

「静默悬挂」反模式（同型根因 P1-MA2-032/048/060/068/074 + A2.12 MANUAL_POST）：

```
dispatcher.tryPost() 内 try { voucherBiz.post(...) } catch (Exception e) { /* 吞掉，仅 log 或完全静默 */ }
→ 返回 null（posted 不置 true）
→ 单据状态进终态（DONE/COMPLETED/...）但 posted=false
→ 无告警、无异常记录、无重试入口
→ 期末结账才发现（或永远不被发现）
```

### 4.2 可恢复性断言契约（形式化定义，对齐 G1-G4）

故障注入后，系统须满足以下断言之一（按 dispatcher 所属 G 分级）：

**通用断言（所有分级，故障注入后必成立）**：

- **A1（posted 一致性）**：`posted` 标志与实际过账结果一致——过账成功则 `posted=true` + 凭证存在；过账失败则 `posted=false` + **无孤儿凭证**（不出现「凭证已生成但 posted 未置 true」或反之）。
- **A2（异常可观测）**：过账失败必留可观测痕迹——经 `IErpSysNotificationBiz.notify` 告警派发（G4 域）**或** `ErpFinPostingException` 异常记录（G1/G2 finance sweep 域）**或** dispatcher LOG.warn/error（最低基线）。**不允许完全静默**（对齐 `posting-log.md` §失败不静默丢弃）。

**分级专属断言**：

| G 分级 | 适用域 | 断言 |
|--------|--------|------|
| **G1/G2**（finance sweep 覆盖域） | finance | **A3-unit**（unit harness 可验证）：Facade 边界故障（Proxy 桩 `IErpFinVoucherBiz.post` 抛）经 dispatcher catch-swallow 路径传播 → **断言**：posted=false（与失败一致，A1）+ dispatcher catch 非完全静默（A2：LOG/alert 留痕）。**注**：finance sweep 的完整链路（`ErpFinPostingException` 记录持久化 + `ErpFinDeferredPostingRetryHelper` 重试 + retryCount≥MAX→MANUAL 升级）由引擎内部 `ErpFinPostingExceptionRecorder` 承载——它在 `IErpFinVoucherBiz.post()` **内部**执行，Facade 边界 Proxy 桩会**绕过** Recorder。故 unit harness 不覆盖 `ErpFinPostingException` 记录持久化与 sweep 重试时序（属集成测试范畴，§4.3 边界 + A3-integration successor） |
| **G4**（无 sweep 覆盖域） | assets（折旧）/ hr / qa / projects / maintenance | **A4-alert**：失败派发 `IErpSysNotificationBiz.notify`（event type = `<domain>.posting-failure`）+ 单据保留可重试/可冲销入口（posted=false 但不进不可恢复死状态）。**断言**：Proxy 桩 `IErpSysNotificationBiz` 录制 captured event type 匹配 + posted=false 但单据可被手工/冲销恢复。<br>**恢复路径分级（对齐 posting-log.md §期末结账前置检查覆盖矩阵 line 127-136）**：assets 折旧经**期末前置检查兜底**（`ErpAstDepreciationSchedule` posted=false 扫描阻断结账）；hr/qa/projects/maintenance **不纳入前置检查**（line 136 明示），经告警 + 期末试算平衡人工发现。<br>**注**：assets Cap/Disposal **非 G4**——posting-log.md line 116 明示 assets Cap/Disposal **有** sweep 兜底（G1/G2），仅 assets 折旧无 sweep（G4）。§5.2 选 `DepreciationPostingDispatcher` 作 assets 代表（G4），Cap/Disposal 作 successor |

> **契约可验证性（每条断言的可执行核验机制，Phase 2 closure gate）**：
> - **A1**（posted 一致性）：故障注入后查 `posted` 字段 + 反查 `ErpFinVoucherBillR` 凭证存在性，断言一致。Facade 边界桩下，过账失败 → posted=false + 无凭证生成（一致）。
> - **A2**（异常可观测）：Proxy 桩 `IErpSysNotificationBiz` 录制 `notify` 调用（event type captured）；或查 `ErpFinPostingException` 记录存在（**仅当引擎真实运行**，非 Facade 桩）；或捕获 dispatcher LOG.warn/error（最低基线）。
> - **A3-unit**（finance dispatcher catch 可恢复性）：Facade 桩 `IErpFinVoucherBiz.post` 抛 → 断言 posted=false + dispatcher catch 非完全静默（LOG/alert）。
> - **A4-alert**（G4 域告警闭环）：Proxy 桩 `IErpSysNotificationBiz.notify` 录制 event type 断言匹配 `<domain>.posting-failure`。

### 4.3 契约边界（不要求 unit harness 覆盖的）

- **finance sweep 完整链路（A3-integration successor）**：`ErpFinPostingException` 记录持久化（`ErpFinPostingExceptionRecorder`）+ `ErpFinDeferredPostingRetryHelper` 重试 + retryCount≥MAX→MANUAL 升级时序，均由引擎**内部**承载。Facade 边界 Proxy 桩绕过 Recorder（R2-MAJOR-1 修正）。覆盖此链路须 (a) 更深注入点（throwing `IErpFinAcctDocProvider` 使引擎运行后失败并记录）+ 端到端集成测试，或 (b) 定时任务测试覆盖 sweep 重试时序。二者均超 Q4 unit harness scope，列为 successor（§7）。
- **引擎内部端到端故障**（真实 GL 写入失败 / 期间关闭 / 科目缺失）：属集成测试范畴，Q4 unit harness 在 SPI 边界注入故障（模拟引擎宕机），不重现引擎内部完整失败链路。successor（§7）。
- **并发冲突 / 超时故障**：Q4 首轮聚焦 tryPost 吞异常同型根因，并发/超时作为 successor（§7）。
- **并发冲突 / 超时故障**：Q4 首轮聚焦 tryPost 吞异常同型根因，并发/超时作为 successor（§7）。

## 5. 实施步骤（Phase 2 实现 plan 的范围契约）

> 本节为 Phase 2 实现 plan 提供步骤骨架与边界声明。Phase 2 plan 起草时（加载 `nop-testing` skill）以本节为实施契约，可细化但不得偏离已裁决的路径 A + 6 域过账悬挂路径范围。

### 5.1 harness 落地（路径 A 通用化抽象）

1. **新建 harness 工具类**（位置待 Phase 2 裁决：`module-common-test` 共享测试工具 vs 各域 `src/test` 内聚）：
   - `FaultInjectionStubs`（或同等命名）——封装 §3.1 两类桩机制的通用 helper：
     - `throwingVoucherBiz(String methodName, ErrorCode/RuntimeException)` —— Proxy 桩 `IErpFinVoucherBiz`，指定方法抛异常。
     - `recordingNotificationBiz(String[] capturedEventType)` —— Proxy 桩 `IErpSysNotificationBiz`，录制 `notify` 调用。
     - `throwingExecutor(Class<? extends *PostingExecutor>, String methodName)` —— 子类 override 模板的工厂。
   - 复用 §1.2 既有先例的 `InvocationHandler` + `defaultReturn` helper（已在 `TestErpInvPostingDispatcherFailureHangs` / `TestDepreciationPostingFailureAlert` 验证）。
2. **field 注入可见性审计（R1）**：逐 dispatcher 核验 `voucherBiz` / `notificationBiz` / `executor` field 可见性。若 package-private 则同包测试；若 `private` 则反射 set 或经测试构造器注入。Phase 2 须给出每域注入方式清单。
3. **（可选）路径 C Extension 作触发 API**：若 Phase 2 裁决采用 `@InjectFault(domain, method, type)` 声明式触发，新建 JUnit 5 `Extension` 委托 harness stub（R4 评估）。

### 5.2 6 域过账悬挂路径覆盖清单（Phase 2 实施契约）

每域至少 1 个故障注入测试，覆盖该域代表性 dispatcher 的 catch-swallow 路径 + §4 可恢复性断言契约：

| 域 | 代表 dispatcher | 注入点 | 断言分级 | 备注 |
|----|-----------------|--------|----------|------|
| finance | `NotesPostingDispatcher`（或 `ErpFinPostingProcessor`） | `IErpFinVoucherBiz.post` 抛 | G1/G2（**A3-unit**） | finance 有 sweep 兜底，但 Facade 桩绕过 Recorder，unit harness 覆盖 dispatcher catch 可恢复性；完整 sweep 链路（A3-integration）successor |
| hr | `SalaryPostingDispatcher` | `IErpFinVoucherBiz.post` 抛 / `SalaryPostingExecutor.postEvent` 抛 | G4（A4-alert） | 既有先例少，harness 首次覆盖；恢复经告警 + 试算平衡（无前置检查） |
| assets | `DepreciationPostingDispatcher` | `IErpFinVoucherBiz.post` 抛 + `IErpSysNotificationBiz` 录制 | G4（A4-alert） | 既有先例 `TestDepreciationPostingFailureAlert`；恢复经告警 + **期末前置检查兜底**（assets 折旧 posted=false 扫描）。注：assets Cap/Disposal 是 G1/G2（有 sweep），非本行 G4 代表 |
| qa | `NcrPostingDispatcher` | `IErpFinVoucherBiz.post` 抛 | G4（A4-alert） | A2.12 MANUAL_POST 路径，harness 首次覆盖；恢复经告警 + 试算平衡（无前置检查） |
| projects | `TimesheetPostingDispatcher` | `IErpFinVoucherBiz.post` 抛 / `ProjectPostingExecutor.postEvent` 抛 | G4（A4-alert） | 既有先例 `TestTimesheetPostingFailureAlert`；恢复经告警 + 试算平衡（无前置检查） |
| maintenance | `MaintenanceLaborPostingDispatcher` / `MaintenanceIssuePostingDispatcher` | `IErpFinVoucherBiz.post` 抛 / `MntPostingExecutor.postEvent` 抛 | G4（A4-alert） | harness 首次覆盖；恢复经告警 + 试算平衡（无前置检查） |
| logistics | `AbstractErpLogShipmentDeliveredProcessor`（`ErpLogShipmentScanForPollingProcessor` / `ErpLogShipmentHandleTrackingWebhookProcessor` 继承） | `IErpFinVoucherBiz.post` 抛 + `IErpSysNotificationBiz` 录制 | G4（A4-alert） | **Q4 successor 已闭合**（plan `2026-08-02-1500-1`，P1-MA2-080 运费过账悬挂告警闭环）。原缺陷：`onDelivered` catch 仅 LOG 无告警派发；修复补 `dispatchFreightFailureAlert`（`log.freight-posting-failure`）+ `TestLogPostingFaultInjection`。logistics 无 sweep 兜底，恢复经告警 + **期末前置检查兜底**（posting-log.md 覆盖矩阵 line 127-136） |

- **与 Q1 盲区类清单的消费方式**：Q4 Phase 2 起草时，若 Q1 Phase 2 已产出盲区类清单（§8），优先覆盖清单中属于上述 6 域过账 dispatcher/Processor 的盲区类。若 Q1 尚未产出（Q4 不阻塞于 Q1 全域完成），按本表代表性 dispatcher 覆盖。

### 5.3 跨 nop-entropy 改造边界声明

| 改动面 | 位置 | Q4 Phase 2 是否触碰 | 说明 |
|--------|------|----------------------|------|
| harness 工具类 | `module-common-test` 或各域 `src/test`（应用层） | **是** | 新建测试工具，路径 A |
| 6 域故障注入测试 | 各域 `erp-*-service/src/test`（应用层） | **是** | 新建测试类 |
| 各域 `*PostingDispatcher` / `*PostingExecutor` 生产代码 | 各域 `src/main` | **否** | 仅经 field 注入 stub，不改生产代码（除非 field 可见性须调整——R1，Phase 2 裁决） |
| nop-entropy 平台源码 | `../nop-entropy/` | **否** | 路径 A 零平台改动 |
| CI workflow | `.github/workflows/` | **是**（§6 裁决若纳入回归层） | 新建 fault-injection.yml 或加 job 到 compliance.yml |
| ORM / model | `<domain>/model/*.orm.xml` | **否** | 零 ORM 变更 |

> 边界裁决：Q4 Phase 2 **零 nop-entropy 改动 + 零 ORM 变更**，全部在应用层测试代码 + 可选 CI workflow。Phase 2 无须在 `nop-entropy/ai-dev/logs/` 记日志。

### 5.4 Phase 2 执行顺序建议

1. 5.1 step 1-2（harness 工具类 + field 注入可见性审计）——基础设施，先行
2. 5.2 6 域过账悬挂测试（finance G1/G2 → 其余 5 域 G4，按既有先例多的域先做降低风险）
3. §4 可恢复性断言契约落地（A1/A2 通用 + A3-unit/A4-alert 分级）
4. §5.2 验收 + §8 Q1 协同产物消费（若 Q1 已产出）
5. §6 CI 门控接线（若 §6 裁决纳入）

## 6. 验收判据（Phase 2 closure gate 契约）

> 每条须在 Phase 2 closure audit 时由独立子代理在 live repo 核验。每条给出具体可执行机制。

1. **通用化 harness 落地**：harness 工具类（`FaultInjectionStubs` 或同等）落盘，封装 Proxy 桩 Facade + 子类 override executor 两类机制。**可执行核验**：`rg "FaultInjectionStubs|throwingVoucherBiz|recordingNotificationBiz" module-*/erp-*-service/src/test/`（或 `module-common-test/`）命中。
2. **6 域过账悬挂路径均有故障注入测试**：finance/hr/assets/qa/projects/maintenance 各 ≥1 测试。**可执行核验**：6 域各有故障注入测试类存在，每测试注入故障 + 断言可恢复性。
3. **可恢复性断言契约成立（§4）**：
   - A1（posted 一致性）：故障注入后 `posted` 与凭证存在性一致。
   - A2（异常可观测）：失败有告警/异常记录/LOG，无完全静默。
   - A3-unit（finance G1/G2 dispatcher catch 可恢复性）：Facade 桩 `IErpFinVoucherBiz.post` 抛 → posted=false + dispatcher catch 非完全静默（LOG/alert）。**注**：finance sweep 完整链路（`ErpFinPostingException` 记录 + 重试 + MANUAL 升级）由引擎内部 Recorder 承载，Facade 桩绕过之 → 属 A3-integration successor（§4.3），**不在 unit harness closure gate**。
   - A4-alert（G4 域）：`IErpSysNotificationBiz.notify` captured event type 匹配 `<domain>.posting-failure`。
   - **可执行核验**：逐测试类核验断言覆盖 A1+A2 + 分级专属（A3-unit 或 A4-alert）。
4. **不污染并行测试（与 Q6 时钟硬化协同）**：故障注入测试的 Proxy stub / 子类 override 须是测试内局部实例（不全局替换 IoC bean / 不改全局静态状态），确保与 Q6 thread-local clock 并行隔离无冲突。**可执行核验**：故障注入测试不经 `CoreMetrics.registerClock` 或全局 bean 替换；stub 作用于测试内局部 dispatcher 实例。
5. **全量回归绿**：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；`mvn test` 0 failures / 0 errors（故障注入测试本身绿 + 不破坏既有 1903 测试基线）。测试计数基线以 `docs/testing/known-good-baselines.md` 最近全量绿基线为准。
6. **无双真相源**：本文档 §1 引用上游真相源（Q0 README + arm-index + posting-log.md），Phase 2 plan 引用本文档，不重推导证据。

## 7. CI 门控设计

> 裁决故障注入测试是否纳入 mandatory 回归层。记录候选、考虑的替代、残留风险。

### 7.1 现状

- 现有 CI（`.github/workflows/`）：`maven.yml`（`mvn -B package` 全量构建/测试）+ `compliance.yml`（F8 反模式基线单向收紧 + F15 i18n + web 页面校验）+ `e2e.yml`。
- 故障注入测试（Phase 2 落地后）是标准 JUnit 测试，会被 `maven.yml` 的 `mvn test` 自动包含——**无需单独 CI job 即纳入 per-commit 回归**（区别于 Q1 pitest 须 nightly + Q6 clock-rollover 须 nightly）。

### 7.2 候选

- **C-1：依赖 `maven.yml` 自动包含（无单独 job）**：故障注入测试是普通 JUnit 测试，`mvn test` 自动跑。残留：无显式「故障注入覆盖率」门控（如 6 域是否全覆盖无 CI 检查）。
- **C-2：新建 `.github/workflows/fault-injection.yml` 显式 job**：独立 job 跑故障注入测试 + 报告覆盖率（6 域覆盖矩阵）。残留：与 `maven.yml` 重复跑同样的测试，CI 资源浪费。
- **C-3：加 job 到 `compliance.yml`**：在 compliance workflow 增 fault-injection 覆盖率检查（grep 6 域测试存在性），对齐 compliance-baseline 单向收紧模式。

### 7.3 裁决（Decision）

> 决策输入：故障注入测试是标准 JUnit（`maven.yml` 自动包含）+ Q4 首轮仅 6 域（规模小，无独立 job 必要）+ compliance-baseline 单向收紧先例（F8/F15）。

**裁决：Phase 2 采用 C-1（依赖 `maven.yml` 自动包含）作为主路径 + C-3（`compliance.yml` 加覆盖率 grep 检查）作为可选增强。C-2（独立 job）否决（资源浪费）。**

**裁决理由**：

1. **标准 JUnit 自动纳入 per-commit 回归**：故障注入测试无需特殊运行时（无 JVM agent / 无 faketime / 无 nightly 调度），`maven.yml` 的 `mvn test` 自动跑。这是 Q4 相比 Q1（pitest 须 nightly）/ Q6（clock-rollover 须 nightly）的 CI 集成优势。
2. **首轮规模小（6 域）无独立 job 必要**：6 域各 1 测试 = 6 测试类，远小于 Q1 三域 pitest 的资源开销。独立 job（C-2）重复跑 `maven.yml` 已跑的测试，浪费 CI 资源。
3. **C-3 可选增强防覆盖回潮**：若需显式门控「6 域均有故障注入测试」（防新增过账路径时遗漏），可在 `compliance.yml` 加 grep 检查（对齐 F8 单向收紧：6 域测试存在性 grep 命中数 ≥ 基线）。Phase 2 视团队需求裁决是否引入。

**考虑的替代**：

- **C-2（独立 workflow）**：否决——重复跑 `maven.yml` 已含测试，资源浪费。保留为 successor（触发：故障注入测试规模显著增长或需独立报告时）。
- **per-commit 全量 + nightly 覆盖率矩阵**：否决——Q4 首轮 6 域规模无需 nightly 矩阵。

**残留风险**：

- **R5（无显式覆盖率门控则覆盖回潮无预警）**：C-1 依赖 `maven.yml` 跑测试，但不检查「6 域是否全覆盖」。若新增过账路径时遗漏故障注入测试，无 CI 显式拦截（除非 C-3 grep 门控）。接受（首轮 6 域 + Phase 2 plan 明确覆盖清单）；C-3 作可选增强。
- **R6（故障注入测试与并行测试隔离）**：见 §6 验收 4——须确保 stub 不污染并行测试（与 Q6 协同）。

### 7.4 与现有 CI 的集成方式（Phase 2 落地）

- **主路径（C-1）**：零 CI 改动——故障注入测试落盘到各域 `src/test`，`maven.yml` 的 `mvn -B package` 自动包含。
- **可选增强（C-3）**：若引入，在 `compliance.yml` 加 step：grep 6 域故障注入测试存在性（如 `rg -l "FailureHangs|FailureAlert|FaultInjection" module-{finance,hr,assets,quality,projects,maintenance}/erp-*-service/src/test/`），命中域数 ≥ 6（单向收紧）。对齐 F8 架构（checker=pure reporter + gate 逻辑在 CI），Phase 2 须新写 grep 检查逻辑。

## 8. 残留风险汇总与 successor

> 汇总 §3.5 + §7.3 残留风险，登记 successor 触发条件（plan authoring guide §反松弛规则：Follow-up 须命名触发条件）。

| 风险 ID | 描述 | 分类 | successor 触发条件 |
|---------|------|------|--------------------|
| R1 | per-dispatcher field 注入可见性 | Phase 2 实施约束 | Phase 2 逐 dispatcher 核验；不可见则反射 set / 同包测试 / 调整 field 可见性 |
| R2 | Facade 边界故障 vs 引擎内部故障保真度 | 契约边界 | Q4 unit harness 覆盖 SPI 边界；引擎内部端到端故障属集成测试 successor |
| R3 | finance sweep 完整链路（A3-integration）覆盖 | 契约边界 | unit harness 仅覆盖 A3-unit（dispatcher catch 可恢复性）；完整 sweep 链路（`ErpFinPostingException` 记录 + 重试 + MANUAL 升级）属 A3-integration successor——须更深注入点（throwing `IErpFinAcctDocProvider`）或端到端/定时任务测试（§4.3） |
| R4 | 路径 C Extension 可选复杂度 | Phase 2 实施约束 | Phase 2 评估 `@InjectFault` 注解样板减少收益 vs 复杂度 |
| R5 | 无显式覆盖率门控则覆盖回潮 | CI 门控 | C-3 grep 门控作可选增强；或 successor（覆盖规模增长时） |
| R6 | 故障注入测试与并行测试隔离 | Phase 2 实施约束 | §6 验收 4 核验 stub 局部性；与 Q6 thread-local clock 协同 |
| —（successor） | Q4 Phase 2 实现 plan（harness + 6 域测试） | out-of-scope（本文档 Phase 1） | 本文档经 ≥2 轮独立审查收敛（§Review Record）+ 路径 A 裁决落定（§3.5）→ DRAFT_PLANS 起草 |
| —（successor） | logistics（P1-MA2-080）过账悬挂覆盖 | out-of-scope improvement | **已闭合（plan `2026-08-02-1500-1`）**：onDelivered 补 `log.freight-posting-failure` 告警派发 + `TestLogPostingFaultInjection`（A1+A2+A4-alert）+ CI 门控扩展 6→7 域。successor 触发条件（Q4 首轮 6 域 harness 沉淀）已满足并落地 |
| —（successor） | 其余故障模式（并发冲突 / 超时 / 外部集成失败） | optimization candidate | 过账悬挂路径 harness 沉淀后扩展 |
| —（successor） | 引擎内部端到端故障注入（路径 B 字节码插桩） | watch-only successor | Q4 首轮证明 SPI 边界故障不足以覆盖某类可恢复性场景时 |

## 9. 与 Q1 协同接口

> Q1↔Q4 协同（roadmap line 786，Q0 README §实施顺序裁决 line 152）：Q1 发现的测试盲区类正是 Q4 应优先覆盖的可恢复性路径（tryPost 吞异常同型根因跨 6 域）。本节声明 Q4 消费 Q1 输出的盲区类清单的方式。

### 9.1 协同契约

- **Q1 产出**：`mutation-testing.md` §8.2 定义的「真实测试盲区类清单」（剔除全部生成代码噪声 `_gen`+`api.beans`+`api.crud` + 等价变异后的真实盲区），格式为 FQCN + 存活变异体数 + 是否过账 dispatcher/Processor。
- **Q4 消费**：Q4 Phase 2（6 域过账悬挂故障注入测试）以 Q1 盲区类清单作为**优先覆盖目标**——清单中属于 finance/hr/assets/qa/projects/maintenance 6 域过账 dispatcher/Processor 的盲区类，正是 tryPost 吞异常同型根因的可恢复性路径。
- **首批协同覆盖不对称（重要边界，引用 Q1 §8.1）**：Q1 首批只跑 finance/mfg/inv 三域，Q4 的 6 域目标是 finance/hr/assets/qa/projects/maintenance——**首批交集仅 finance**。故 Q1 首批盲区清单对 Q4 的即时可消费域仅 finance；hr/assets/qa/projects/maintenance 的盲区须待 Q1 successor 扩展跑这些域后方能供 Q4 消费。Q4 排期不阻塞于 Q1 全域完成（Q0 README §残留风险：协同假设若盲区与过账路径不重合，Q4 排期可独立前移）。
- **协同时序**：Q4 Phase 2 plan 起草建议在 Q1 Phase 2 产出盲区类清单后启动（本计划 §Deferred 已记录）。Q1/Q4 的 Phase 1 设计文档（本文档 + Q1 doc）同批起草，Phase 2 可独立或协同推进。

### 9.2 Q4 消费格式（Q1 输出 → Q4 优先覆盖候选）

Q4 Phase 2 起草时，从 Q1 盲区类清单（`mutation-testing.md` §8.2 格式）提取「Q4 优先覆盖候选」：

```
# Q4 优先覆盖候选（交集：Q1 盲区类 ∩ 6 域过账悬挂路径）
# 来源：Q1 Phase 2 盲区类清单 ∩ §5.2 6 域 dispatcher 列表
# 若 Q1 尚未产出本域盲区，Q4 按 §5.2 代表性 dispatcher 覆盖（不阻塞）

## finance（首批交集域）
| 盲区类（FQCN） | 存活变异体数 | 对应 §5.2 dispatcher | Q4 覆盖优先级 |
|----------------|--------------|----------------------|---------------|
| app.erp.fin.service.posting.NotesPostingDispatcher | N | NotesPostingDispatcher | 高 |
| ... | ... | ... | ... |

## hr/assets/qa/projects/maintenance（待 Q1 successor 扩展）
（Q1 首批未跑，Q4 按 §5.2 代表性 dispatcher 覆盖）
```

### 9.3 协同边界

- Q1 **不负责**修复盲区（仅产出清单）；过账路径盲区修复属 Q4 故障注入覆盖 + 后续测试补强。
- Q4 **不负责**重跑变异测试（仅消费清单）；Q1 的 mutation score 基线是 Q4 补测试后回归验证的参照。
- 若 Q1 盲区类与 Q4 过账悬挂路径不重合，Q4 排期可独立前移，协同清单为空集亦有效（Q0 README §残留风险）。

## Review Record

> 审查记录：MQ 文档先行工作流要求 ≥2 轮独立子代理审查（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），由不同子代理会话执行（不同 task id），审查者不可与作者为同一会话。每轮输出 BLOCKER/MAJOR/MINOR 分级意见，作者修订后重审直至收敛（无残留 BLOCKER/MAJOR）。本文档经 2 轮审查收敛（R1 合规 + R2 覆盖/可执行性）。

- **Round 1（规范合规审查）**: `ses_04434b8d6ffe1exrtBzGZg6t3Z`（独立子代理 fresh session cold context）— **accept**，0 BLOCKER / 0 MAJOR / 3 MINOR。全部 live-repo 核验 PASS（`IPostingDispatcher` 零命中确认 / `IPostingDispatcher` 接口实仓不存在 / 064=qa-cancel-linkage 非 posting-hang / 080=logistics / posting-log.md G1-G4 内容逐条核实 / 7 个 per-point 先例存在性抽查 PASS / 6 域 18 个 dispatcher + 6 executor 全部存在 / facade `IErpFinVoucherBiz` 存在）。
  - R1-MINOR-1：§3.1「全域 33 个 dispatcher」含 3 个测试类，实际生产 dispatcher = 30。
  - R1-MINOR-2：§1.2 grep 实际命中 9（含 2 incidental 命中 `TestErpMfgSubcontracting`/`TestErpHrPayrollEngine`），非 7。
  - R1-MINOR-3：§4.2 把 assets 整体归 G4，但 posting-log.md line 116 明示 assets Cap/Disposal 有 sweep（G1/G2），仅折旧无 sweep（G4）。
  - 修改摘要：§3.1 全域计数改 30 + 标注 `--glob '!*Test*'`；§1.2 标注另附 2 incidental 命中；§4.2 A4-alert 行 + §5.2 assets 行补「Cap/Disposal 是 G1/G2，仅折旧 G4」边界（与 R2-MAJOR-2 一并修订）。

- **Round 2（覆盖面与可执行性审查）**: `ses_04434960bffeDKppsywjoC4FOP`（**另一个**独立子代理，不同 task id，新会话）— **needs-revision**，0 BLOCKER / 2 MAJOR / 2 MINOR。R1 三项 MINOR 经 R2 复核均 **resolved**（实仓复核：dispatcher 计数 30 精确、grep 9 命中、assets taxonomy 已修正）。覆盖面维度 PASS（三路径替代充分评估 / 6 域 SPI 全部存在 / 应用层边界裁决无跨仓库依赖 / Q6 并行隔离无冲突 / Q1 协同接口可消费）。
  - R2-MAJOR-1（可执行性）：finance A3-sweep closure criterion 不可满足——Proxy 桩 `IErpFinVoucherBiz.post` 抛**绕过**引擎 `ErpFinPostingExceptionRecorder`（它在 post() 内部执行），故无 `ErpFinPostingException` 记录持久化，但 §6 acceptance-3 仍把「记录存在 + status 正确」列为 closure gate。实仓核验：`NotesPostingDispatcher.java:61` catch 无 record 引用；`ErpFinPostingExceptionRecorder.java:89-97` 经 `daoProvider.daoFor(ErpFinPostingException.class)` 持久化（引擎内部）。
  - R2-MAJOR-2（契约精度）：A4-alert/G4 over-generalize「期末结账前置检查兜底」到 hr/qa/projects/maintenance——posting-log.md line 136 明示「mfg/hr/projects/maintenance dispatcher 失败经 IErpSysNotificationBiz 告警闭环，**不纳入前置检查**（经期末试算平衡人工发现）」；前置检查覆盖矩阵（line 127-136）仅 finance/assets/inventory/logistics。
  - R2-MINOR-1：同 R1-MINOR-1（33→30，已修）。
  - R2-MINOR-2：`DeferredPostingSweepJob` 实仓无此类——经重构为 `ErpFinPostingExceptionRecorder` + `ErpFinDeferredPostingRetryHelper` + `ErpFinPostingExceptionRetryProcessor`（概念名继承自 posting-log.md）。
  - 修改摘要：
    - **R2-MAJOR-1 修正**：§4.2 A3-sweep 重命名为 **A3-unit**（Facade 桩覆盖 dispatcher catch 可恢复性：posted=false + catch 非静默）；完整 sweep 链路（记录持久化 + 重试 + MANUAL 升级）= **A3-integration successor**（§4.3 边界，须更深注入点 throwing `IErpFinAcctDocProvider` 或端到端/定时任务测试）。§5.2 finance 行 + §6 acceptance-3 同步改为 A3-unit；§3.5 R3 + §8 R3 successor 同步。
    - **R2-MAJOR-2 修正**：§1.5 G4 行 + §4.2 A4-alert 行 + §5.2 备注列，区分 assets（前置检查兜底，line 127-136 覆盖）vs hr/qa/projects/maintenance（仅告警 + 试算平衡，line 136 不纳入前置检查）；删除 G4 通用「期末结账前置检查兜底」措辞。
    - **R2-MINOR-2 修正**：§1.5 增「类名核对」注（`DeferredPostingSweepJob` 概念名 vs 实仓 `ErpFinPostingExceptionRecorder`/`ErpFinDeferredPostingRetryHelper`/`ErpFinPostingExceptionRetryProcessor`）；§4.3 + R3 同步用实仓类名。

**收敛结论**：2 轮审查后无残留 BLOCKER / 无残留 MAJOR（R1 的 3 MINOR + R2 的 2 MAJOR + 2 MINOR 全部修订；R2 实仓复核确认 SPI 类存在性 + G1-G4 内容 + finding-ID 漂移消解 + Recorder 内部持久化事实）。文档可作为 Phase 2 实现 plan 的实施契约。MINOR 不阻塞收敛。

<!-- 审查者多样性已满足：R1（ses_04434b8d...）/ R2（ses_04434960b...）两会话 task id 不同，均独立 fresh cold context，未复用作者上下文。 -->
