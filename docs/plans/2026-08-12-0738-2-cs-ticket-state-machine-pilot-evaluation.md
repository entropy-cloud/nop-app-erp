# 2026-08-12-0738-2-cs-ticket-state-machine-pilot-evaluation 客服试点评估与批量迁移模板裁定（M1.3）

> Plan Status: active
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

Status: planned
Targets: 审计记录工件（写入本计划 Closure 段或 `docs/audits/` 下专档，按证据量决定）；发现项登记
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof | Decision | Fix`（`Fix` 为条件型——仅当审计发现不一致/漂移时触发；Ticket 经 M0.2 确认无 dict 死状态，若审计复核维持则无 Fix 项落地）
- Prereqs: N=1 计划（M1.1 + M1.2）闭包

- [ ] `Proof`（四方对照）：以 `state-machine-business-review-prompt.md` 10 维度审查客服试点，逐维形成可追溯结论——
  - **dict ↔ 元数据**：dict `erp-cs/ticket-status` 全部 6 值 ↔ `ErpCsTicketStateMachine.transitions()` 边覆盖；每个 dict 值至少一条 writer（含确认 Ticket 无死状态的 M0.2 结论是否仍成立）。
  - **owner-doc 迁移图 ↔ 元数据**：`state-machine.md §2` 迁移表 ↔ Bean 边覆盖。**已知 owner-doc 内部漂移候选**：`state-machine.md §2` 迁移表（`:33-41`）仅列 2 条 cancel 来源（NEW/IN_PROGRESS），而 `§实现约定`（`:153`）声明 4 来源（NEW/ASSIGNED/IN_PROGRESS/RESOLVED）——Bean 取 9 边（cancel 展开 4 来源）。审计须对此 §2 表 vs §实现约定 的 doc-内部漂移给出分类裁决（doc drift → 建议补 §2 缺失的 ASSIGNED→CANCELLED / RESOLVED→CANCELLED 行；属 Fix 登记非静默折叠）。复核 §实现约定 + M0.2 SLA drift 补注是否在 Bean 中如实保持（`startDateTime = 首次 IN_PROGRESS`、close-breached、reopen 删除未答问卷、审计 actionType 映射）。
  - **元数据 ↔ 全部 writer**：盘点 `ErpCsTicket.status` 全部写路径——生产命名动作（BizModel 4 + Resolve + Reopen）+ 框架入口（`__save`/`save` 经 M0.1 §9 选项 c 可写但不在矩阵运行时强制范围）+ 测试 fixture；确认命名动作均经 Bean，框架入口属已知残留风险（M0.1 §9.4）。
  - **可达性/终态/异常路径**：从 NEW 全可达、CLOSED/CANCELLED 终态无出边、reopen/SLA 超时/并发乐观锁异常路径与 owner doc §3-5 一致。
  Skill: `state-machine-business-review-prompt.md`
- [ ] `Decision`（审计结论）：对每维给出 `一致` / `不一致`（分类 implementation drift / doc drift / intentional legacy）；任何不一致按路线图规则 5 登记（已确认缺陷或契约漂移 = Fix，不得降级 Follow-up）并指派 successor（Ticket 内的→本计划或独立 Fix plan；M2/M3 共性的→模板警示条目）。Skill: `state-machine-business-review-prompt.md`
- [ ] `Proof`（试点行为回归复核——**独立复跑**）：M1.3 是 M2/M3/M4 硬门控，回归证据须独立可复现，不得仅以引用 N=1 计划的断言代替。**复跑** `mvn test -pl module-cs/erp-cs-service` 并附输出（层 1 矩阵 + 层 3 `TestErpCsTicketSlaCsat` 全绿），确认外部行为/SLA 起算/data-deletion reopen/错误码/审计/Delta 生效均无回归；并引用 N=1 计划闭包证据位置作为交叉印证。Skill: `nop-testing`

Exit Criteria:

- [ ] 四方对照审计记录存在且非空，每维有可追溯结论（引用 Bean 元数据 / owner doc 章节 / dict 位置 / writer 类:行）。
- [ ] 不一致项（若有）已按 Fix/Decision 登记 + successor，无静默排除。
- [ ] 试点行为回归复核完成（**复跑** `mvn test -pl module-cs/erp-cs-service` 全绿 + 交叉引用 N=1 闭包证据）。

### Phase 2 - go/no-go 裁定 + 批量迁移模板 + 路线图同步

Status: planned
Targets: 批量迁移模板工件（写入 `docs/architecture/entity-state-machine-bean.md` 末尾新增「迁移模板（M1.3 产物）」节——M2/M3/M4 迁移项 plan 将沿此稳定架构 owner doc 引用，而非一次性审计记录）；路线图 Work Item Status 同步
Skill: `state-machine-business-review-prompt.md`（模板须内嵌四方对照与 10 维度审查为标准步骤）

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [ ] `Decision`（go/no-go）：基于 Phase 1 审计结论裁定——(a) 试点无行为回归、元数据可审计、Delta 生效 → **go**（批准 M2/M3 批量迁移启动）；(b) 存在阻塞项 → **no-go**，列出阻塞项并移交 successor，保持 M2/M3 blocked。记录裁定、判据、残留风险。Skill: `state-machine-business-review-prompt.md`
- [ ] `Add`（批量迁移模板工件）：固化 M2/M3/M4 迁移项的标准步骤序列——(1) Bean 形状（显式动作 + 目标态 + isTerminal + transitions 元数据，按 M0.1 §4）；(2) app-service.beans.xml FQN-id 注册；(3) Processor/BizModel 接线（固定判断→Bean，动态守卫保留）；(4) 层 1 矩阵完备性表驱动测试；(5) 层 2 四方对照（dict↔owner-doc↔元数据↔writer 含 CRUD 路径）；(6) 层 3 既有命名动作回归；(7) Delta 适用性（M2/M3 非保护域可证 Delta，M4 plan-first 按保护区门控）。标注三类变体点：**M2** 简单生命周期（≤4 态）、**M3** 审批轴独立 Bean（`Document`/`Approval` 后缀）+ 复杂业务分支、**M4** 全部 plan-first + 财务影响/过账时序不改 + `posted` 不入轴。Skill: `state-machine-business-review-prompt.md`
- [ ] `Proof`（模板可复用性）：证实模板覆盖 M0.2 §2.2 全表至少各一类代表（如 M2 取 ErpPurOrder.docStatus、M3 取 ErpPurOrder.approveStatus 双轴独立 Bean、M4 取 ErpFinVoucher.docStatus plan-first），无空白槽；模板引用 M0.1 契约章节而非重述。Skill: none
- [ ] `Add`（路线图一致性）：更新路线图 Work Item Status 块与依赖图，使其与 M1.3 结论一致（M1.3 done 推导由路线图状态机规则；若 no-go 则保持 M2/M3 blocked 并记录阻塞）。Skill: none（机械文本同步）

Exit Criteria:

- [ ] go/no-go 裁定存在，判据与残留风险记录（go 须基于 Phase 1 全绿；no-go 须列阻塞 + successor）。
- [ ] 批量迁移模板工件存在，含 7 步标准序列 + M2/M3/M4 三类变体点，且覆盖代表样例无空白槽。
- [ ] 路线图 Work Item Status 与依赖图与 M1.3 结论一致。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_00ccc675affeCMISDqprNMJJfa`) — 无 BLOCKER。1 MAJOR：Phase 1 行为回归复核用「可复跑」（optional）弱化硬门控的独立回归确认，触碰反松弛（optional/as needed）；已修订为**强制复跑** `mvn test -pl module-cs/erp-cs-service` 并附输出 + 交叉引用 N=1 闭包证据，Closure Gate 同步。4 MINOR：模板工件落点「或」歧义→定为 `entity-state-machine-bean.md` 末尾「迁移模板」节；Phase 1 Item Types 声明 `Fix` 但无项携带→补「Fix 条件型（仅审计发现不一致时触发）」；owner-doc §2 迁移表（2 cancel 行）vs §实现约定（4 来源）doc-内部漂移候选→Phase 1 显式标为 drift 裁决项；103 计数「+M1.1 试点 1」混淆→改为「试点是模板来源依据，不在 103 内」。全部 load-bearing 声明经独立复核 CONFIRMED TRUE（M1.3 为 M2/M3/M4 硬门控、技能存在、M0.2 计数、CS-1 八属性、SLA drift、无死状态、N=1 真前置、doc-only 计划移除构建门控合规、不声称全域 Delta 验证、rule 14 拆分正当）。
- Independent draft review iteration 2: `accept` (`ses_00cc76e5affer5oyv1eU8m0pKy`) — 迭代 1 MAJOR RESOLVED（Phase 1 Proof + Closure Gate 均改为强制「复跑」/「独立复跑」并附输出，无 optional/可复跑 残留于 load-bearing 项）。4 MINOR 全 RESOLVED（模板落点定为 `entity-state-machine-bean.md` 迁移模板节、Fix 条件型已注、cancel doc-内部漂移已标为审计裁决项、103 计数已澄清试点不在内）。1 NIT（Phase 1 Exit Criteria 残留「可复跑」与强制措辞不一致）已随后修正为「复跑」。M1.3 vs M5.1 边界正确（审计单轴 + 产模板，不抽样 103）。无新问题引入。草案审查收敛，Plan Status → active。

## Closure Gates

> 本计划为审计 + 模板裁定（零迁移代码、零模型/契约变更）。按计划指南，移除构建验证门控并说明：无生产迁移代码变更，验证以「四方对照全覆盖 + go/no-go 判据可追溯 + 模板无空白槽」的审计完整性检查替代；行为回归复核复用 N=1 计划的 cs-service 测试（可复跑留证）。

- [ ] 范围内行为完成（四方对照审计 + go/no-go 裁定 + 批量迁移模板工件 + 路线图同步）
- [ ] 相关文档对齐（模板工件引用 M0.1 契约章节；路线图 Work Item Status/依赖图一致）
- [ ] 已运行验证（审计完整性，非构建）：(1) 四方对照每维可追溯；(2) 不一致项已登记 + successor；(3) go/no-go 判据明确；(4) 模板覆盖 M2/M3/M4 代表样例无空白槽；(5) 行为回归复核 = **独立复跑** `mvn test -pl module-cs/erp-cs-service` 全绿（输出附证），并交叉引用 N=1 闭包证据；(6) `git diff --stat` 证实改动仅在 `docs/` 下（无 `module-*/` 生产代码、无 `model/`、无 ORM）→ compliance checker 不可能漂移（仍可运行留证）
- [ ] 无范围内项目降级为 deferred/follow-up（go/no-go 裁定必须落地，不得悬置；不一致项若有则 Fix 登记）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status、Phase Status、Exit Criteria、Closure Gates、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [ ] 结束证据存在于文件中

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

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理（新会话）>
- Evidence: <task id / 审计记录 / 模板工件链接>
