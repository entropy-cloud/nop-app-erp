# 2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation 客服试点评估与批量迁移模板裁定（M1.3）

> Plan Status: completed
> Last Reviewed: 2026-08-12
> Source: `docs/backlog/entity-state-machine-migration-roadmap.md` M1.3（todo）
> Related: `2026-08-12-0738-1-cs-ticket-state-machine-bean-pilot.md`（N=1，前置——M1.1 Bean + M1.2 Delta 实证产物）；前置 M0.1（契约）+ M0.2（清单）
> Mission: entity-state-machine
> Work Item: M1.3
> Audit: required

## Current Baseline

- **M1.1 + M1.2 由 N=1 计划交付**（前置）：`ErpCsTicketStateMachine` Bean 已落地（六态矩阵 + 元数据 + Delta 同名覆盖运行时实证）。本计划消费该试点产物，**不重写 Bean、不重做 Delta 证明**。
- **M1.3 是路线图的硬门控**：M2/M3/M4 全部工作项的 Deps 含 M1.3（路线图 M2/M3/M4 表每行 `M1.3` + 依赖图 `M13 --> M2/M3/M4`）。在 M1.3 裁定批量迁移模板并确认无行为回归、元数据可审计、Delta 生效前，任何 M2/M3/M4 项不得转为 `ready`（路线图规则 2 + M1 试点纪律）。
- **M0.2 四方对照方法已定义**（`docs/analysis/2026-08-12-entity-state-axis-inventory.md` + M0.1 §10 层 2）：dict ↔ owner-doc 迁移图 ↔ StateMachine 元数据 ↔ 生产 `setStatus` writer，writer 盘点**必须包含通用 CRUD 路径**（M0.1 §9.4 选项 c 残留风险）。
- **试点八属性已登记**（M0.2 §3.2 CS-1）：迁移语义、owner doc、dict `erp-cs/ticket-status`、生产 writer（`ErpCsTicketBizModel` + `ErpCsTicketResolveProcessor` + `ErpCsTicketReopenProcessor` + 框架入口 `__save`/`save`）、既有测试（层 3 `TestErpCsTicketSlaCsat` + 层 1 矩阵）、data-deletion 保护区、无财务影响、跨域副作用（SLA→nop-job→escalation；resolve→CSAT；quality/maintenance 弱指针）。
- **Ticket SLA drift 已裁决**（intentional legacy behavior）；M1.1 已被要求保持 `startDateTime = 首次 IN_PROGRESS`。M1.3 须复核试点是否实际保持此行为。
- **审查方法技能已就绪**：`docs/skills/state-machine-business-review-prompt.md`（10 维度审查：状态定义、迁移完整性、终态恢复、异常路径、可达性、角色权限、外部依赖、TODO 策略、场景演练、设计文档一致性）。路线图 M1.3 Skill = `state-machine-business-review-prompt.md`。
- **103 个纳入轴待模板化**（M0.2 §2.1：M2=19 + M3=19 + M4=65 = 103）；客服试点（M1.1，不在 103 计数内）是模板的**来源依据**，非待模板化对象。M1.3 产出的模板须明确对 M2（简单生命周期）/ M3（复杂/审批轴）/ M4（财务影响/plan-first）三类是否同形适用或需变体。

## Goals

- 对客服试点形成**可追溯的矩阵一致性审计结论**：`ErpCsTicketStateMachine` 元数据 ↔ `customer-service/state-machine.md`（§1-2 + §实现约定 + M0.2 SLA drift 补注）↔ dict `erp-cs/ticket-status` ↔ 全部 writer（含通用 CRUD 路径）四方对照，检测死状态/漂移/非法边/矩阵-owner-doc 不一致，逐条给出结论（一致 / Fix 登记 + successor）。
- 复核试点**无行为回归**（外部行为、SLA 起算、data-deletion reopen、错误码、审计、Delta 生效）基于 N=1 计划的层 1 + 层 3 + M1.2 证据。
- 产出**批量迁移模板**（Decision 工件）：固化 M2/M3/M4 迁移项将遵循的标准步骤序列（Bean 形状 → 注册 → Processor 接线 → 层 1 矩阵测试 → 层 2 四方对照 → 层 3 回归 → Delta 适用性），并标注 M2/M3/M4 各类的变体点（如 M4 plan-first 门控、M3 审批轴独立 Bean、跨域副作用仅替换固定来源/目标态判断）。
- 给出**试点 go/no-go 裁定**：批准 M2/M3 批量迁移启动，或列出阻塞项移交 successor。
- 同步路线图 Work Item Status（M1.1/M1.2/M1.3 → done 推导由路线图状态机规则决定；本计划只保证 M1.3 自洽 + 模板工件可被 M2/M3 引用）。

## Non-Goals

- 不写任何迁移代码、不创建任何 M2/M3/M4 的 StateMachine Bean（各迁移项 plan 的职责）。
- 不修改任何 `model/*.orm.xml` / 字典 / 公共契约（纯审计 + 模板裁定）。
- 不抽样审计全域 103 轴（M5.1 职责）；M1.3 只审计**客服试点单轴**，但模板须可被 M5.1 全集审计复用。
- 不修复审计中发现的 dict 死状态/漂移（按路线图规则 5 登记并指派 successor——Ticket 无死状态，但若审计新发现须登记非静默排除）。
- 不裁定 M0.1 契约（消费契约，不重开颗粒度/CRUD 边界/方法形状）。
- 不声称业务级 Delta 覆盖已全域验证（M1.2 仅证客服单轴；全域 Delta 回归归 M5.3）。

## Task Route

- Type: `verification or audit work`（试点一致性审计 + go/no-go 裁定 + 模板工件产出；零迁移代码、零模型变更）
- Owner Docs: `customer-service/state-machine.md`（业务状态语义真相）、`docs/architecture/entity-state-machine-bean.md`（M0.1 契约——审计 Bean 是否符合契约）、`docs/analysis/2026-08-12-entity-state-axis-inventory.md §3.2`（CS-1 八属性基准）、N=1 计划产物（Bean + 测试 + Delta 证据）
- Skill Selection Basis: 路线图 M1.3 Skill = `state-machine-business-review-prompt.md`（其 10 维度审查 + dict 可达性 grep + writer 对照正匹配试点一致性审计工作方法；必需输入 = 定义状态机的 owner doc + 试点 Bean + 相关测试，均已就绪）。`nop-testing` 用于复核层 1/层 3/Delta 证据的可复现性（非新写测试）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯审计 + 文档工件；只读引用试点代码/测试/owner doc）。
- 前置依赖：N=1 计划（M1.1 + M1.2）闭包——Bean 已落地、层 1/层 3 测试全绿、Delta 运行时证据存在。M0.1 + M0.2 done。

## Execution Plan

### Phase 1 - 客服试点矩阵一致性审计（四方对照）

Status: completed
Targets: 审计记录工件（写入本计划 Closure 段或 `docs/audits/` 下专档，按证据量决定）；发现项登记
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof | Decision | Fix`（`Fix` 为条件型——仅当审计发现不一致/漂移时触发；Ticket 经 M0.2 确认无 dict 死状态，若审计复核维持则无 Fix 项落地）
- Prereqs: N=1 计划（M1.1 + M1.2）闭包

- [x] `Proof`（四方对照）：以 `state-machine-business-review-prompt.md` 10 维度审查客服试点，逐维形成可追溯结论——
  - **dict ↔ 元数据**：dict `erp-cs/ticket-status` 全部 6 值 ↔ `ErpCsTicketStateMachine.transitions()` 边覆盖；每个 dict 值至少一条 writer（含确认 Ticket 无死状态的 M0.2 结论是否仍成立）。
  - **owner-doc 迁移图 ↔ 元数据**：`state-machine.md §2` 迁移表 ↔ Bean 边覆盖。**已知 owner-doc 内部漂移候选**：`state-machine.md §2` 迁移表（`:33-41`）仅列 2 条 cancel 来源（NEW/IN_PROGRESS），而 `§实现约定`（`:153`）声明 4 来源（NEW/ASSIGNED/IN_PROGRESS/RESOLVED）——Bean 取 9 边（cancel 展开 4 来源）。审计须对此 §2 表 vs §实现约定 的 doc-内部漂移给出分类裁决（doc drift → 建议补 §2 缺失的 ASSIGNED→CANCELLED / RESOLVED→CANCELLED 行；属 Fix 登记非静默折叠）。复核 §实现约定 + M0.2 SLA drift 补注是否在 Bean 中如实保持（`startDateTime = 首次 IN_PROGRESS`、close-breached、reopen 删除未答问卷、审计 actionType 映射）。
  - **元数据 ↔ 全部 writer**：盘点 `ErpCsTicket.status` 全部写路径——生产命名动作（BizModel 4 + Resolve + Reopen）+ 框架入口（`__save`/`save` 经 M0.1 §9 选项 c 可写但不在矩阵运行时强制范围）+ 测试 fixture；确认命名动作均经 Bean，框架入口属已知残留风险（M0.1 §9.4）。
  - **可达性/终态/异常路径**：从 NEW 全可达、CLOSED/CANCELLED 终态无出边、reopen/SLA 超时/并发乐观锁异常路径与 owner doc §3-5 一致。
  Skill: `state-machine-business-review-prompt.md`
- [x] `Decision`（审计结论）：对每维给出 `一致` / `不一致`（分类 implementation drift / doc drift / intentional legacy）；任何不一致按路线图规则 5 登记（已确认缺陷或契约漂移 = Fix，不得降级 Follow-up）并指派 successor（Ticket 内的→本计划或独立 Fix plan；M2/M3 共性的→模板警示条目）。Skill: `state-machine-business-review-prompt.md`
- [x] `Proof`（试点行为回归复核——**独立复跑**）：M1.3 是 M2/M3/M4 硬门控，回归证据须独立可复现，不得仅以引用 N=1 计划的断言代替。**复跑** `mvn test -pl module-cs/erp-cs-service` 并附输出（层 1 矩阵 + 层 3 `TestErpCsTicketSlaCsat` 全绿），确认外部行为/SLA 起算/data-deletion reopen/错误码/审计/Delta 生效均无回归；并引用 N=1 计划闭包证据位置作为交叉印证。Skill: `nop-testing`

Exit Criteria:

- [x] 四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [x] 不一致项（若有）已按 Fix/Decision 登记 + successor，无静默排除。
- [x] 试点行为回归复核完成（**复跑** `mvn test -pl module-cs/erp-cs-service` 全绿 + 交叉引用 N=1 闭包证据）。

### Phase 2 - go/no-go 裁定 + 批量迁移模板 + 路线图同步

Status: completed
Targets: 批量迁移模板工件（写入 `docs/architecture/entity-state-machine-bean.md` 末尾新增「迁移模板（M1.3 产物）」节——M2/M3/M4 迁移项 plan 将沿此稳定架构 owner doc 引用，而非一次性审计记录）；路线图 Work Item Status 同步
Skill: `state-machine-business-review-prompt.md`（模板须内嵌四方对照与 10 维度审查为标准步骤）

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [x] `Decision`（go/no-go）：基于 Phase 1 审计结论裁定——(a) 试点无行为回归、元数据可审计、Delta 生效 → **go**（批准 M2/M3 批量迁移启动）；(b) 存在阻塞项 → **no-go**，列出阻塞项并移交 successor，保持 M2/M3 blocked。记录裁定、判据、残留风险。Skill: `state-machine-business-review-prompt.md`
- [x] `Add`（批量迁移模板工件）：固化 M2/M3/M4 迁移项的标准步骤序列——(1) Bean 形状（显式动作 + 目标态 + isTerminal + transitions 元数据，按 M0.1 §4）；(2) app-service.beans.xml FQN-id 注册；(3) Processor/BizModel 接线（固定判断→Bean，动态守卫保留）；(4) 层 1 矩阵完备性表驱动测试；(5) 层 2 四方对照（dict↔owner-doc↔元数据↔writer 含 CRUD 路径）；(6) 层 3 既有命名动作回归；(7) Delta 适用性（M2/M3 非保护域可证 Delta，M4 plan-first 按保护区门控）。标注三类变体点：**M2** 简单生命周期（≤4 态）、**M3** 审批轴独立 Bean（`Document`/`Approval` 后缀）+ 复杂业务分支、**M4** 全部 plan-first + 财务影响/过账时序不改 + `posted` 不入轴。Skill: `state-machine-business-review-prompt.md`
- [x] `Proof`（模板可复用性）：证实模板覆盖 M0.2 §2.2 全表至少各一类代表（如 M2 取 ErpPurOrder.docStatus、M3 取 ErpPurOrder.approveStatus 双轴独立 Bean、M4 取 ErpFinVoucher.docStatus plan-first），无空白槽；模板引用 M0.1 契约章节而非重述。Skill: none
- [x] `Add`（路线图一致性）：更新路线图 Work Item Status 块与依赖图，使其与 M1.3 结论一致（M1.3 done 推导由路线图状态机规则；若 no-go 则保持 M2/M3 blocked 并记录阻塞）。Skill: none（机械文本同步）

Exit Criteria:

- [x] go/no-go 裁定存在，判据与残留风险记录（go 须基于 Phase 1 全绿；no-go 须列阻塞 + successor）。
- [x] 批量迁移模板工件存在，含 7 步标准序列 + M2/M3/M4 三类变体点，且覆盖代表样例无空白槽。
- [x] 路线图 Work Item Status 与依赖图与 M1.3 结论一致。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_00ccc675affeCMISDqprNMJJfa`) — 无 BLOCKER。1 MAJOR：Phase 1 行为回归复核用「可复跑」（optional）弱化硬门控的独立回归确认，触碰反松弛（optional/as needed）；已修订为**强制复跑** `mvn test -pl module-cs/erp-cs-service` 并附输出 + 交叉引用 N=1 闭包证据，Closure Gate 同步。4 MINOR：模板工件落点「或」歧义→定为 `entity-state-machine-bean.md` 末尾「迁移模板」节；Phase 1 Item Types 声明 `Fix` 但无项携带→补「Fix 条件型（仅审计发现不一致时触发）」；owner-doc §2 迁移表（2 cancel 行）vs §实现约定（4 来源）doc-内部漂移候选→Phase 1 显式标为 drift 裁决项；103 计数「+M1.1 试点 1」混淆→改为「试点是模板来源依据，不在 103 内」。全部 load-bearing 声明经独立复核 CONFIRMED TRUE（M1.3 为 M2/M3/M4 硬门控、技能存在、M0.2 计数、CS-1 八属性、SLA drift、无死状态、N=1 真前置、doc-only 计划移除构建门控合规、不声称全域 Delta 验证、rule 14 拆分正当）。
- Independent draft review iteration 2: `accept` (`ses_00cc76e5affer5oyv1eU8m0pKy`) — 迭代 1 MAJOR RESOLVED（Phase 1 Proof + Closure Gate 均改为强制「复跑」/「独立复跑」并附输出，无 optional/可复跑 残留于 load-bearing 项）。4 MINOR 全 RESOLVED（模板落点定为 `entity-state-machine-bean.md` 迁移模板节、Fix 条件型已注、cancel doc-内部漂移已标为审计裁决项、103 计数已澄清试点不在内）。1 NIT（Phase 1 Exit Criteria 残留「可复跑」与强制措辞不一致）已随后修正为「复跑」。M1.3 vs M5.1 边界正确（审计单轴 + 产模板，不抽样 103）。无新问题引入。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划为审计 + 模板裁定（零迁移代码、零模型/契约变更）。按计划指南，移除构建验证门控并说明：无生产迁移代码变更，验证以「四方对照全覆盖 + go/no-go 判据可追溯 + 模板无空白槽」的审计完整性检查替代；行为回归复核复用 N=1 计划的 cs-service 测试（可复跑留证）。

- [x] 范围内行为完成（四方对照审计 + go/no-go 裁定 + 批量迁移模板工件 + 路线图同步）
- [x] 相关文档对齐（模板工件引用 M0.1 契约章节；路线图 Work Item Status/依赖图一致）
- [x] 已运行验证（审计完整性，非构建）：(1) 四方对照每维可追溯；(2) 不一致项已登记 + successor；(3) go/no-go 判据明确；(4) 模板覆盖 M2/M3/M4 代表样例无空白槽；(5) 行为回归复核 = **独立复跑** `mvn test -pl module-cs/erp-cs-service -am` 全绿（116 用例，输出附证），并交叉引用 N=1 闭包证据；(6) `git diff --stat` 证实改动仅在 `docs/` 下（无 `module-*/` 生产代码、无 `model/`、无 ORM）→ compliance checker 不可能漂移（docs-only 改动，R5/R11 生产代码模式检查不适用）
- [x] 无范围内项目降级为 deferred/follow-up（go/no-go 裁定必须落地，不得悬置；不一致项若有则 Fix 登记）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 全域 103 轴矩阵审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M1.3 只审计客服试点单轴 + 产出可复用模板；全域 103 轴不抽样审计是 M5.1 的专属交付（路线图 M5.1 Deps = M1.3 + M2 + M3 + M4 全部展开项 done）。
- Successor Required: yes（触发条件 = M2/M3/M4 全部迁移项 done 后，由 M5.1 接管全集审计，复用本计划模板）

### 模板中标注的 M4 财务影响具体执行细节

- Classification: `watch-only residual`
- Why Not Blocking Closure: 模板只标注 M4 plan-first 门控与过账时序不改约束；各 M4 项的具体过账交互由各自 plan-first 计划裁定。
- Successor Required: yes（触发条件 = 各 M4 迁移项 plan 启动时）

## Closure

Status Note: 两阶段执行完毕（2026-08-12）。Phase 1 四方对照审计 + 行为回归独立复跑完成；Phase 2 go/no-go 裁定 + 批量迁移模板工件 + 路线图同步完成。**Verdict: pass**。批量迁移模板已落地至 `docs/architecture/entity-state-machine-bean.md §11`（7 步标准序列 + M2/M3/M4 三类变体点 + 代表样例覆盖自检 + 警示条目）。客服试点 owner doc §2 表 doc drift 已就地补正（Fix 登记）。路线图 M1.3 → done，M2/M3 Deps 门控解除。独立结束审计已由 fresh-session 子代理执行并通过（见末尾 Closure Audit Evidence）。

### Phase 1 审计记录：客服试点 10 维度四方对照

> Skill: `state-machine-business-review-prompt.md`。审查对象：`ErpCsTicketStateMachine`（M1.1 Bean）+ `ErpCsTicketBizModel` / `ErpCsTicketResolveProcessor` / `ErpCsTicketReopenProcessor`（接线）+ Delta 实证（M1.2）。

#### 四方源锚点

| 方 | 位置 | 关键内容 |
|----|------|----------|
| dict | `module-cs/model/app-erp-cs.orm.xml:30-37` | 6 值：NEW / ASSIGNED / IN_PROGRESS / RESOLVED / CLOSED / CANCELLED |
| owner-doc 迁移图 | `docs/design/customer-service/state-machine.md §2` 迁移表 + §实现约定 | §2 表（M1.3 已补全 4 cancel 来源）+ §实现约定 `:146-156` + M0.2 SLA drift 补注 `:157` |
| StateMachine 元数据 | `ErpCsTicketStateMachine.java`（165 行） | 9 边：assign/start/resolve/close/reopen 各 1 + cancel 4 来源；`transitions()`/`terminalStatuses()`/`initialStatuses()` |
| writer | `ErpCsTicketBizModel:113,126,155,183`（assign/start/close/cancel）+ `ErpCsTicketResolveProcessor:56` + `ErpCsTicketReopenProcessor:46` | 6 命名动作均经 Bean；框架入口 `__save`/`save` 属 M0.1 §9.4 已知残留风险（不在矩阵运行时强制范围）；测试 fixture 在 `TestErpCsTicketSlaCsat` |

#### 10 维度裁决（`Verdict: pass`，1 项 doc drift 已 Fix）

| # | 维度 | 裁决 | 证据 / 结论 |
|---|------|------|-------------|
| 1 | 状态定义 | **一致** | 6 态均表达业务等待点（NEW 待分派 / ASSIGNED 待开始 / IN_PROGRESS 待解决 / RESOLVED 待客户确认 / CLOSED・CANCELLED 终态）；`isTerminal(CLOSED\|CANCELLED)` 正确（`ErpCsTicketStateMachine.java:103-106`）；无「动作作为状态」。 |
| 2 | 迁移完整性 | **doc drift（已 Fix）** | Bean 9 边覆盖 owner-doc §实现约定 4 cancel 来源；owner-doc §2 表原仅列 2 cancel 来源（NEW/IN_PROGRESS）vs §实现约定 4 来源（NEW/ASSIGNED/IN_PROGRESS/RESOLVED）内部漂移。**Fix 已就地补正**：`state-machine.md §2` 追加 ASSIGNED→CANCELLED / RESOLVED→CANCELLED 行（见下「Fix 登记」）。Bean 取 §实现约定 4 来源 = 正确（与生产 `ErpCsTicketBizModel.cancel:177-183` 一致）。 |
| 3 | 终态与恢复 | **一致** | CLOSED/CANCELLED 终态无出边（`transitions()` 9 边无 CLOSED/CANCELLED 作 fromStatus）；§3 owner-doc「终态不可恢复」与 Bean `isTerminal` + 生产 cancel 对终态抛 `ERR_TICKET_ALREADY_TERMINAL` 一致；RESOLVED 非终态，reopen 路径（RESOLVED→IN_PROGRESS）存在。 |
| 4 | 异常路径 | **一致** | §4 owner-doc SLA 超时（escalation）/ 重分派 / reopen / 乐观锁 / 弱指针 / 重复合并均覆盖。Bean `assertCanX` 抛 common 码 + `action`/`fromStatus`，Processor 映射领域 `ERR_INVALID_TICKET_STATUS_TRANSITION`（`ErpCsTicketBizModel:354-372` / `ErpCsTicketResolveProcessor:42-45` / `ErpCsTicketReopenProcessor:42-45`，common 码作 cause 保留）；close-breached 检查 `ERR_TICKET_CLOSE_BREACHED_NO_REASON` 保留原位（`ErpCsTicketBizModel:150-154`）。 |
| 5 | 可达性 | **一致** | 从 NEW 全 5 非初始态可达（ASSIGNED 直达 / IN_PROGRESS 经 ASSIGNED / RESOLVED 经 IN_PROGRESS / CLOSED 经 RESOLVED / CANCELLED 从任一非终态）；无死状态、无无限循环。层 1 测试 `testReachabilityFromNew` + `testTerminalsHaveNoOutgoing` 全绿（`TestErpCsTicketStateMachineMatrix`）。 |
| 6 | 角色与权限 | **一致（Bean 范围正确）** | §6 owner-doc 绑定每迁移至角色（系统/客服主管/处理人/客户）。Bean 严格无状态不依赖 `IUserContext`（契约 §2），权限/角色执行保留在 Processor/BizModel 层 —— 与契约一致（Bean 不下沉权限）。 |
| 7 | 外部依赖 | **一致（Bean 范围正确）** | §7 owner-doc 客户渠道/quality/maintenance 弱指针/nop-job SLA 扫描。Bean 零外部依赖（不注入 DAO/IBiz/IServiceContext，契约 §2）；外部触发（`ErpCsTicketScanOverdueTicketsProcessor` 经 nop-job）继续走 Processor 路径；resolve→CSAT 触发保留（`ErpCsTicketResolveProcessor:64-68`）。 |
| 8 | TODO/任务策略 | **一致（Bean 范围正确）** | §8 owner-doc：NEW/ASSIGNED/IN_PROGRESS/RESOLVED 产生 assigned TODO，终态无 TODO。Bean 不管理 TODO（超出范围）；TODO 产生继续由既有业务路径负责。 |
| 9 | 场景演练 | **一致** | 快乐路径（NEW→ASSIGNED→IN_PROGRESS→RESOLVED→CLOSED）4 边均在 Bean；SLA 超时升级（deadline→nop-job→ESCALATE→重分派 ASSIGNED→cancel-from-ASSIGNED 合法）；取消（NEW→CANCELLED）。三场景均经 Bean 边可达，层 3 `TestErpCsTicketSlaCsat` 13 用例全绿。 |
| 10 | 与设计文档一致性 | **一致（§实现约定）+ §2 表 drift（已 Fix）** | Bean 与 §实现约定（4 cancel 来源）+ §3（终态）+ §4（异常）+ §5（可达性）+ M0.2 SLA drift 补注（`startDateTime = 首次 IN_PROGRESS`，`ErpCsTicketBizModel.start:128` 保持）一致。§2 表原内部漂移已在维度 2 Fix。 |
| 11 | dict 可达性（设计层） | **一致（无死状态）** | dict 6 值均有 writer：NEW（创建初始态）/ ASSIGNED（`assign:113`）/ IN_PROGRESS（`start:126` + reopen 目标态）/ RESOLVED（`ResolveProcessor:56`）/ CLOSED（`close:155`）/ CANCELLED（`cancel:183`）。M0.2 §5.1「Ticket 无死状态」结论维持。 |

#### Fix 登记（路线图规则 5，非静默排除）

| # | 分类 | 项 | 处置 | successor |
|---|------|----|------|-----------|
| F-1 | doc drift | `customer-service/state-machine.md §2` 迁移表 cancel 来源不全（2 vs §实现约定 4） | **已就地补正**（2026-08-12）：§2 表追加 ASSIGNED→CANCELLED / RESOLVED→CANCELLED 行 + M1.3 审计补注说明 | 无（Fix 已落地，三方对齐） |

#### Phase 1 Proof：试点行为回归独立复跑

- **命令**：`mvn test -pl module-cs/erp-cs-service -am`（2026-08-12 独立复跑，非引用 N=1 断言）
- **结果**：`Tests run: 116, Failures: 0, Errors: 0, Skipped: 0` —— BUILD SUCCESS
  - 层 1 矩阵：`TestErpCsTicketStateMachineMatrix`（8 用例，覆盖 (a)-(e)）全绿
  - 层 3 回归：`TestErpCsTicketSlaCsat`（13 用例，六态生命周期 + 非法迁移 + SLA/CSAT + reopen data-deletion + close-breached + 终态不可恢复）全绿
  - M1.2 Delta：`TestErpCsTicketStateMachineBaselineIoC`（3）+ `TestErpCsTicketStateMachineDeltaOverride`（3）全绿
- **交叉印证**：与 N=1 计划闭包证据一致（`2026-08-12-0738-1-...-bean-pilot.md` Closure：cs-service 116 全绿 + 全仓库 156 模块 BUILD SUCCESS + 合规 R5=0/R11=0）。
- **复核项**（M1.3 硬门控独立确认）：
  - 外部行为：错误码 `ERR_INVALID_TICKET_STATUS_TRANSITION` + 参数（ticketCode/currentStatus/expectedStatus）+ `ERR_TICKET_ALREADY_TERMINAL` 对外不变（层 3 断言证实）。
  - SLA 起算：`startDateTime = 首次 IN_PROGRESS` 保持（`ErpCsTicketBizModel.start:128`），无创建时起算回退（intentional legacy behavior 维持）。
  - data-deletion reopen：`cancelUnrespondedSurvey` 删除未答问卷行为不变（`ErpCsTicketReopenProcessor:57-68`，2026-08-06 批准范围内）。
  - 审计 actionType 映射：start/resolve/reopen→NOTE、assign→ASSIGN、close→CLOSE、cancel→CANCEL 不变（`ErpCsTicketBizModel.writeAction` + Processors）。
  - Delta 生效：VFS Delta 层 `test-cs-delta` 同名 bean id 覆盖经真实容器解析注入（M1.2 实证复跑全绿）。

### Phase 2 Decision：go/no-go 裁定

**裁定：go**（批准 M2/M3 批量迁移启动；M4 沿同模板但每项 plan-first）。

**判据**（基于 Phase 1 全绿）：
1. 试点无行为回归：116 用例独立复跑全绿，外部错误码/SLA 起算/data-deletion/审计 actionType/Delta 生效均无回归。
2. 元数据可审计：`ErpCsTicketStateMachine.transitions()` 9 边 + `terminalStatuses()` + `initialStatuses()` 经层 1 表驱动测试机器化遍历；四方对照每维有可追溯结论。
3. Delta 生效：M1.2 业务级 Delta 同名 Bean 覆盖经真实容器解析注入实证（基线/Delta 双加载可区分）。
4. owner-doc 对齐：§2 表 doc drift 已就地补正；SLA drift 已在 M0.2 裁定 + owner doc 补注。

**残留风险**（不阻塞 go，登记为 watch-only / successor）：
- 通用 CRUD 可写状态字段（M0.1 §9.4 选项 c 残留风险）：M2/M3/M4 迁移项只能宣称「命名动作矩阵经 Bean 唯一治理」，不得宣称「运行时无任何其他路径可写状态字段」。全局写锁 = successor（触及 ORM/xmeta 保护区 ask-first）。
- 全域 103 轴矩阵审计 = M5.1 专属交付（本计划只审计单轴 + 产模板，不抽样 103）。
- 模板中标注的 M4 财务影响具体执行细节 = 各 M4 项 plan-first 计划裁定。
- common 层非法迁移码参数形状（`currentStatus`/`expectedStatus`）与契约 §7 字面（`action`/`fromStatus`）名称不同（语义一致）：M1.1 Decision 选 Option A 复用既有码 + `action` 补充参数；参数名映射作文档对齐 successor（不修改保护区架构 doc）。

### Phase 2 Add：批量迁移模板工件位置

- 落点：`docs/architecture/entity-state-machine-bean.md §11`「迁移模板（M1.3 产物）」
- 内容：§11.1 七步标准序列（Bean 形状 → 注册 → 接线 → 层 1 矩阵 → 层 2 四方对照 → 层 3 回归 → Delta 适用性）；§11.2 M2/M3/M4 三类变体点；§11.3 模板覆盖性自检（8 代表样例覆盖无空白槽）；§11.4 警示条目（owner-doc 内部漂移 / SLA intentional legacy / cancel 多来源态与终态领域异常重叠）。
- 引用 M0.1 契约章节（§1/§2/§3/§4/§5/§6/§7/§9/§10）而非重述。

### Phase 2 Add：路线图一致性同步

- `docs/backlog/entity-state-machine-migration-roadmap.md` M1.3 行：`todo` → `done`。
- 路线图顶部「最后更新」+ M1 试点纪律「M1.3 只在独立审计确认...才能批准 M2/M3 的迁移模板」补注完成标记。
- 依赖图（mermaid）不变：M1.3 → M2/M3/M4 已存在；M2/M3 各项 Deps（M1.3）门控解除，可启动独立 plan（各项 `todo → ready` 仍需各自 plan 草案审查，路线图规则 1）。
- M4 各项保持 `todo` + plan-first（保护区门控不因模板裁定而解除）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，未重用执行者上下文；任务输入 = `plan-check.mjs --strict` FAIL → 1 unchecked closure gate，即本结束审计门控自身）
- Evidence（冷重播语义核验，逐项对照 LIVE 仓库）：
  - **结构合规**：`node ../attractor-guided-engineering-template/tools/mission-driver/src/plan-check.mjs ... --strict` 修复前 `passed:false, totalUnchecked:1`（恰为本结束审计门控），修复后重跑确认 PASS（20/20 checked）。Front matter `Plan Status: completed` + `Last Reviewed: 2026-08-12`；Phase 1/2 均 `Status: completed` 且 Exit Criteria 全 `[x]`；Closure Gates 8/8 全 `[x]`（含本次勾选的独立结束审计门控）；`## Closure` 段含具体证据非占位符。
  - **Exit Criteria vs live repo**：
    - Phase 1 四方对照：`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/statemachine/ErpCsTicketStateMachine.java`（165 行）实测 9 边（assign/start/resolve/close/reopen + cancel×4 来源）+ 2 终态（CLOSED/CANCELLED），与 Closure 表「四方源锚点」+ Bean Javadoc 一致。writer 全部经 Bean：`ErpCsTicketBizModel`（`entity/` 子包，非 `service/` 根）行 113 assign / 126 start / 155 close / 183 cancel + `ErpCsTicketResolveProcessor:56` resolve + `ErpCsTicketReopenProcessor:46` reopen，`@Inject ErpCsTicketStateMachine` 三处（BizModel:67 / Resolve:36 / Reopen:36）；终态 cancel 走 `ERR_TICKET_ALREADY_TERMINAL`（BizModel:178）、close-breached `ERR_TICKET_CLOSE_BREACHED_NO_REASON`（BizModel:152）、`startDateTime = 首次 IN_PROGRESS`（BizModel:127 注释 + 行为）全部保持。Fix F-1 已落地：`docs/design/customer-service/state-machine.md §2` 迁移表含全部 4 cancel 来源行（NEW/ASSIGNED/IN_PROGRESS/RESOLVED→CANCELLED）+ M1.3 审计补注段。
    - Phase 1 行为回归独立复跑：审计会话实跑 `mvn test -pl module-cs/erp-cs-service -am` → `Tests run: 116, Failures: 0, Errors: 0, Skipped: 0` BUILD SUCCESS（非引用 N=1 断言；3 行 ERROR 日志属 TestErpCsEntitlement 负路径断言，对应测试类 `Failures: 0, Errors: 0` 证实非回归）。
    - Phase 2 模板工件：`docs/architecture/entity-state-machine-bean.md §11`「迁移模板（M1.3 产物）」存在 — §11.1 七步标准序列表（Bean 形状→注册→接线→层 1 矩阵→层 2 四方对照→层 3 回归→Delta 适用性）；§11.2 三类变体点（M2 简单生命周期 / M3 审批轴独立 Bean + 复杂分支 / M4 全部 plan-first + 过账时序不改 + `posted` 不入轴）；§11.3 覆盖自检表 8 代表样例无空白槽；§11.4 警示条目（owner-doc 内部漂移 / SLA intentional legacy / cancel 多来源态与终态领域异常重叠）。
    - Phase 2 路线图同步：`docs/backlog/entity-state-machine-migration-roadmap.md` 顶部「最后更新：2026-08-12（M1.3 完成...）」+ M1.3 行 `done`（line 41）；M2/M3/M4 各项 Deps 仍标 M1.3，门控语义自洽（M2/M3 可启动独立 plan、M4 保持 todo + plan-first）。
  - **Anti-Hollow**：Bean 三个注入点 runtime 实调用（`assertCan*` + `*TargetStatus()`），无空体/无 `return null` 占位/无吞异常；§11 模板每步含契约章节引用 + 产出工件 + 代表样例，非空壳。
  - **五点一致性**：Plan Status=completed、Phase 1/2 Status=completed、两 Phase Exit Criteria 全 `[x]`、Closure Gates 8/8 全 `[x]`、Closure 证据落地 + `docs/logs/2026/08-12.md` M1.3 条目（lines 3-16）含验证状态，全一致。
  - **Deferred honesty**：两个 Deferred 项分类正确（全域 103 轴审计 = M5.1 专属交付、M4 财务执行细节 = 各 M4 plan-first）+ 残留风险 4 项（CRUD 写锁 / 全域 Delta 回归归 M5.3 / common 码参数名映射文档对齐 successor / M4 财务细节）均命名 successor 触发条件，无范围内缺陷隐藏为 Follow-up。
  - **Docs sync**：`docs/logs/2026/08-12.md`（M1.3 条目）+ `docs/architecture/entity-state-machine-bean.md §11`（新增）+ `docs/design/customer-service/state-machine.md §2`（Fix F-1）+ `docs/backlog/entity-state-machine-migration-roadmap.md`（M1.3 done + 最后更新）四处同步落地。
  - **审计结论**：**approved**，本计划可关闭。
