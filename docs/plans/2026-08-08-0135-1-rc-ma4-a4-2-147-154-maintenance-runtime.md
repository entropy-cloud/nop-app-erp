# 2026-08-08-0135-1 rc-ma4-a4-2-147-154-maintenance-runtime 维护域到期访问/设备恢复/备件跨域/联动响应运行时确认

> Plan Status: completed
> Last Reviewed: 2026-08-08
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.147 / A4.2.148 / A4.2.149 / A4.2.150 / A4.2.151 / A4.2.152 / A4.2.153 / A4.2.154
> Related: `docs/audits/2026-08-06-0245-1-rc-ma1-a1-42-maintenance-f1-scheduling-conflict.md`（A1.42 §7 SP-1 + P1-RC-064/065/066 + P2-RC-060）/ `docs/audits/2026-08-06-0245-2-rc-ma1-a1-43-maintenance-f2-visit-sparepart.md`（A1.43 §7 SP-1..SP-3 + P2-RC-061）/ `docs/audits/2026-08-06-0245-3-rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard.md`（A1.44 §7 SP-1..SP-5 + P1-RC-067~071 + reuse P1-MA2-093）
> Audit: required
> Mission: requirement-compliance
> Work Item: A4.2.147-A4.2.154（maintenance 域 MA4 运行时验证）
> Approval: 只读审计 + 测试补充（A4 MA4 类目），受 `docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md` §3 A4 确认自动执行（Q2）覆盖；本计划不实施生产代码修复（P1/P2 修复归 MR1）

## Current Baseline

maintenance 域三个切片（A1.42 调度与冲突 / A1.43 访问与备件 / A1.44 响应/联动/OEE/看板）MA1 报告 §7 共列出 8 项静态存疑点，全部为本计划范围（A4.2.147-A4.2.154）。**不覆盖项**：A1.42 SP-2/SP-3（运行时长采集入口 + recurrenceType RUNTIME——静态已确认缺失，无需 MA4，归 P1-RC-064 修复）；A1.44 SP-5（看板 orgId 行级权限）——已由 A4.2.10 合并闭合（8 域 dashboard orgId P1-MA2-093 reuse R1.29 全覆盖，done ✅），本计划不重复覆盖。存疑点分三类：(1) 主路径行为闭合确认（A4.2.147 generateDueVisits 端到端 / A4.2.149 跨域余额扣减 / A4.2.150 跨域不足 guard）；(2) 低优先级边界确认（A4.2.148 IDLE 恢复分支 P2-RC-061）；(3) 隐式机制确认（A4.2.151-154 A1.44 SP-1..SP-4 零 hook/事件/监听器的运行时证实）。

- **A4.2.147（A1.42 SP-1 UC-MAIN-01 ④ generateDueVisits 自动生成路径→schedule→conflict 端到端）**：静态已澄清**非缺口**——自动生成产 DRAFT（`ScheduleDueGenerator.generateDueVisits`），冲突检测在 DRAFT→SCHEDULED（`ErpMntVisitScheduleProcessor.schedule` 手动排程，`checkScheduleConflict:26`），设备维度冲突检测强测已证实（`TestErpMntVisitRequestStateMachine#testVisitScheduleConflict:93`）。运行时确认端到端 cron→DRAFT→schedule→conflict 全链（低优先级，静态已明确）。
- **A4.2.148（A1.43 SP-1 UC-MAIN-03 ③ complete 时 IDLE 设备是否变 RUNNING）**：静态已确认**是缺口**——`EquipmentStatusLinker.restoreToRunning:38-43` 恒恢复 RUNNING 无 IDLE 分支，javadoc:16-17 自承简化（无前置状态快照列）；测试均 seed RUNNING 未覆盖 IDLE 输入（`TestErpMntVisitRequestStateMachine#testVisitHappyPathWithEquipmentLink:66-88`）。归 **P2-RC-061**。运行时确认 = 补 IDLE 输入测试（seed 设备=IDLE → complete → 断言恢复值）。
- **A4.2.149（A1.43 SP-2 UC-MAIN-04-C generateMove(OUTGOING+relatedBillType) 是否真实触发库存余额扣减）**：静态已澄清**机制存在**——`SparePartIssueService.issue:28` 构造 StockMoveRequest[OUTGOING, RELATED_BILL_TYPE_MNT_SPARE_PART] 调跨域 `IErpInvStockMoveBiz.generateMove` → `ErpInvStockMoveProcessor`（OUTGOING + relatedBillType 非空 → 自动 complete → `bookkeeper.bookCompletion` 更新 `ErpInvStockBalance`）。maintenance 侧测试在 move 层断言 DONE（`TestErpMntSparePartPosting#testSparePartPostingBasic:101`），**未直查余额**。运行时确认：confirm 后直查 `ErpInvStockBalance.totalQuantity` == seed−消耗量。
- **A4.2.150（A1.43 SP-3 UC-MAIN-04-E 备件不足校验失败路径是否抛 inventory ERR）**：静态已澄清**guard 存在**——`ErpInvStockMoveProcessor#validateAvailable:116-136` available<required 抛 `ERR_AVAILABLE_INSUFFICIENT`，经 `isNegativeStockAllowed:285`（config `erp-inv.allow-negative-stock` 默认 FALSE）门控。maintenance 侧未测不足路径。运行时确认：seed 不足库存（5<消耗 10）→ confirm → 断言抛 `ERR_AVAILABLE_INSUFFICIENT`（跨域集成路径）。
- **A4.2.151（A1.44 SP-1 UC-MAIN-05 accept 生成的 visit 是否经隐式机关联回 request）**：静态 grep 零 hook/拦截器（request→visit 关联无自动机制）；A1.44 §5 已裁决 P1-RC-067（关联请求维度 P1，ORM ask-first）。运行时确认 visit 生成时 requestId 实际填充机制，衔接维持既有 finding 不新建。
- **A4.2.152（A1.44 SP-2 UC-MAIN-06 DowntimeEntry record/complete 是否经隐式通道发布跨域事件）**：静态 grep 零事件发布（平台事件总线全局拦截零命中）。运行时确认设备状态联动实际经 `EquipmentStatusLinker` 直接调用达成（`linkToDown:31`/`restoreToRunning:38` config-gated `erp-mnt.equipment-status-link-enabled` 默认 true），非事件订阅模型。
- **A4.2.153（A1.44 SP-3 UC-MAIN-08 资产 SCRAPPED/SOLD 是否反向调 `IErpMntEquipmentBiz.changeStatus(DECOMMISSIONED)`）**：静态 grep maintenance 侧零监听器；assets 域处置 Processor 是否反向调用留待运行时 census 确认（A1.44 §7 SP-3 双向联动确认——maintenance 侧零监听器已证，assets 侧待核）。运行时确认双向联动现状。
- **A4.2.154（A1.44 SP-4 UC-MAIN-07 额外故障是否支持 visit remark/result + 手工创建 request 半自动流程）**：静态无编排方法。运行时确认前端 AMIS 是否有编排补全（visit 表单 remark/result 字段可达性 + request 手工 CRUD 可达性）。

剩余差距：八项均为只读运行时确认 + 两处测试侧补充（A4.2.148 IDLE 输入单测 + A4.2.149 余额直查断言，均限 erp-mnt-service 测试类目，不触生产代码）。缺陷项（A4.2.148 P2-RC-061）维持 P2（登记不强制，修复归 MR1）；A4.2.151-154 按 A1.44 §6 既有 finding（P1-RC-067~071）衔接（复用或维持注记），禁止未经比对新建。本计划仅确认运行时行为以维持/细化裁决，不改变 Q4 强制实现义务，不实施生产代码修复。

## Goals

- 对 A4.2.147-A4.2.154 八项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：A4.2.147/149/150 主路径闭合确认（行为正确 + 跨域集成链证据）；A4.2.148 维持 P2-RC-061（P2 登记不强制，修复归 MR1）+ 补 IDLE 输入测试作为运行时证据；A4.2.151-154 确认隐式机制缺失或存在，按 A1.44 §6 既有 finding 衔接裁决（维持 P1-RC-067~071 分级或登记 watch-only）；若运行时发现活跃数据破坏则触发 MR0（预期不触发——A1.42/43/44 §7 P0 评估均无活跃破坏）。
- 完成后回写 roadmap A4.2.147-A4.2.154 `todo → done`，并按裁决更新 arm-index（维持注记，无未经比对新建）。

## Non-Goals

- 不实现 P1-RC-064/065/066（运行时长触发/任务模板/人员冲突维度）、P2-RC-060/061（warn 模式/IDLE 分支）、P1-RC-067~071（联动/响应缺口）修复——修复义务归 MR1 R1.0 展开器；P1-RC-064 触 ORM 结构变更须 ask-first + 独立 plan-audit。
- 不覆盖 A1.42 SP-2/SP-3（静态已确认缺失，无需 MA4）与 A1.44 SP-5（看板行级权限，已由 A4.2.10 合并闭合）。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落）。
- 不修改 ORM/生产代码/跨域 Facade 接线（本计划只读确认；唯一代码变更 = 测试侧补充：A4.2.148 新增 IDLE 输入单测 + A4.2.149 余额直查断言，均限 erp-mnt-service 测试类目，不触生产代码）。
- 不复跑 MA2/A2.14 状态机审计（A1.42/43/44 已声明复用其证据）；不重审 P1-MA2-086（幂等维度 resolved R1.28）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-06-0245-1-rc-ma1-a1-42-maintenance-f1-scheduling-conflict.md` + `docs/audits/2026-08-06-0245-2-rc-ma1-a1-43-maintenance-f2-visit-sparepart.md` + `docs/audits/2026-08-06-0245-3-rc-ma1-a1-44-maintenance-f3-response-linkage-oee-dashboard.md`（§5/§6/§7）+ `docs/design/maintenance/`（use-cases.md / state-machine.md / equipment-integration.md）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划以只读审计为主 + 一处测试补充，无生产代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 大部分存疑点经静态证明运行时决定性事实得出（A4.2.151-154 零 hook/事件/监听器 ⇒ 运行时无自动联动；A4.2.153 双向调用 census ⇒ 运行时联动现状确定）。A4.2.147/149/150 经既有 L4 测试重跑 + 测试侧断言（A4.2.149 余额直查补断言至 `TestErpMntSparePartPosting` 数据流；A4.2.150 不足路径属 inventory 域 guard，经 `mvn test -pl module-inventory/erp-inv-service` 既有 guard 测试 `TestErpInvStockMoveBookkeeping` 确认）。A4.2.148 新增 1 个 IDLE 输入单测（`TestErpMntVisitRequestStateMachine` 变体），`mvn test -pl module-maintenance/erp-mnt-service` 运行——测试补充均限测试类目，不触生产代码。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.147-A4.2.154）

Status: completed
Targets: `docs/audits/2026-08-08-0135-rc-ma4-a4-2-147-154-maintenance-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Add`
- Prereqs: A4.2 done ✓；A1.42 done ✓；A1.43 done ✓；A1.44 done ✓

- [x] **A4.2.147 generateDueVisits 自动生成路径→schedule→conflict 端到端确认（UC-MAIN-01 ④）**：重跑 `TestErpMntDueVisitJob`（cron 门控）+ `TestErpMntSparePartAndSchedule#testGenerateDueVisitsCreatesPlannedVisitAndAdvancesNextDueDate`（DRAFT 生成）+ `TestErpMntDueVisitIdempotency`（幂等）+ `TestErpMntVisitRequestStateMachine#testVisitScheduleConflict`（DRAFT→SCHEDULED 冲突检测），追踪 `ScheduleDueGenerator.generateDueVisits`→DRAFT→`schedule`→`ERR_VISIT_SCHEDULE_CONFLICT` 全链。裁决：主路径闭合（非缺口确认，静态已明确）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.148 complete 时 IDLE 设备运行时恢复值确认（UC-MAIN-03 ③；P2-RC-061）**：确认 `EquipmentStatusLinker.restoreToRunning:38-43` 恒恢复 RUNNING 无 IDLE 分支（file:line 证据）+ **新增 IDLE 输入单测**（seed 设备=IDLE → complete → 断言设备=RUNNING[证实简化偏差]）并运行 `mvn test -pl module-maintenance/erp-mnt-service -Dtest=TestErpMntVisitRequestStateMachine`。裁决：维持 P2-RC-061（P2 登记不强制，修复归 MR1）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.149 generateMove(OUTGOING+relatedBillType) 库存余额扣减运行时确认（UC-MAIN-04-C 跨域）**：追踪 `SparePartIssueService.issue:28`→跨域 `IErpInvStockMoveBiz.generateMove`→`ErpInvStockMoveProcessor`（OUTGOING+relatedBillType 非空→自动 complete）→`bookkeeper.bookCompletion` 链；重跑 `TestErpMntSparePartPosting#testSparePartPostingBasic:101` 并补余额直查断言（`ErpInvStockBalance.totalQuantity` == seed−消耗量，测试侧补充）。裁决：主路径闭合（跨域集成链证据）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.150 备件不足校验失败路径运行时确认（UC-MAIN-04-E 跨域 guard）**：确认 `ErpInvStockMoveProcessor#validateAvailable:116-136` + `isNegativeStockAllowed:285`（config 默认 FALSE）门控链；seed 不足库存（5<消耗 10）→ confirm → 断言抛 `ERR_AVAILABLE_INSUFFICIENT`（经 `mvn test -pl module-inventory/erp-inv-service` 既有 guard 测试 `TestErpInvStockMoveBookkeeping` 确认跨域 guard 行为）。裁决：主路径闭合（跨域 guard 生效）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.151 accept→visit 隐式关联回 request 确认（UC-MAIN-05）**：grep census 零 hook/拦截器复核 + 代码路径追踪 visit 生成时 requestId 填充机制。裁决：确认无隐式自动联动 → 按 A1.44 §6 衔接维持 P1-RC-067（不新建）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.152 DowntimeEntry 跨域事件发布确认（UC-MAIN-06）**：grep census 零事件发布复核 + 确认设备状态联动实际经 `EquipmentStatusLinker` 直接调用（`linkToDown:31`/`restoreToRunning:38`，config-gated 默认 true）达成行为等价。裁决：确认非事件订阅模型（行为经直接调用达成等价 → 接受/watch-only）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.153 资产 SCRAPPED/SOLD 反向调 changeStatus(DECOMMISSIONED) 确认（UC-MAIN-08 双向联动）**：maintenance 侧零监听器（已证）+ assets 域处置 Processor（`DisposalAcctDocProvider`/`ErpAstDisposalApproveProcessor`）反向调用 census（运行时双向联动确认）。裁决：按 A1.44 §6 既有 finding 衔接，维持分级或登记 watch-only。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.154 额外故障 visit remark/result + 手工 request 半自动流程确认（UC-MAIN-07）**：visit 表单 remark/result 字段（XMeta/AMIS）可达性 + request 手工 CRUD 可达性 + 前端编排 census。裁决：确认半自动流程可达性现状（接受/watch-only）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：八项存疑点各出 §裁决（主路径闭合 / 维持 P2-RC-061 + 新测试证据 / 隐式机制缺失或存在 + 既有 finding 衔接）+ §与既有 finding 衔接（P2-RC-061 + P1-RC-067~071 + reuse P1-MA2-093[A4.2.10 已闭合，排除声明] + P1-MA2-086[不重审]）+ §过程纪律自检（checker 退出码门控——测试补充不触生产代码 actual=baseline；closure-audit 独立性声明）+ §A1.44 SP-5 排除声明（已由 A4.2.10 闭合）+ §跨域探针纪律声明（A4.2.149/150 只读，不改 inventory 生产代码）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段以只读审计为主 + 两处测试侧补充（A4.2.148 IDLE 输入单测 + A4.2.149 余额直查断言，均限 erp-mnt-service 测试类目，不触生产代码）。A4.2.149/150 触及跨域 inventory 行为——只读探针，不改 inventory 生产代码。

- [x] 验证报告落盘，含八项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [x] A4.2.148 新增 IDLE 输入测试已落地并运行通过，作为运行时证据（证伪/证实 IDLE→RUNNING 简化偏差，证据入报告）
- [x] 每项裁决明确：主路径闭合 / 维持分级（P1 Q4 强制实现 / P2 登记 / watch-only）+ 运行时证据记录，或升级触发 MR0

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.147-154 done）、`docs/audits/arm-index.md`（维持注记追加）、`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：P2-RC-061（IDLE 恢复分支缺失）维持 P2（运行时证据 + 测试补充，修复归 MR1，登记不强制）；A1.44 §7 各项按运行时发现衔接既有 finding（P1-RC-067~071）维持或 watch-only，禁止未经比对新建；P1-MA2-093（行级权限）维持 resolved R1.29（A4.2.10 已闭合，不重复覆盖）；P1-MA2-086（幂等维度）不重审。
- [x] `Add` roadmap A4.2.147-A4.2.154 `todo → done`；`docs/logs/2026/08-08.md` 追加完成条目。

Exit Criteria:

- [x] roadmap 八项状态已更新为 done 且与报告裁决一致
- [x] arm-index 维持注记已追加（无未经比对直接新建的 finding）

## Draft Review Record

- Independent draft review iteration 1: needs revision (task ses_0257cc702ffePHTAMdbUjlT5xN) — 2 Major（①A4.2.149 补余额直查断言与 Non-Goals/Exit Criteria/Closure Gates「唯一代码变更 = A4.2.148」矛盾 → 统一为「两处测试侧补充（A4.2.148 + A4.2.149），均限 erp-mnt-service 测试类目，不触生产代码」；②类名 `ErpMntVisitRequestProcessor` 不存在 → 更正为 `ErpMntVisitScheduleProcessor.schedule`）+ 3 Minor（A4.2.148 退出标准「或降级为静态决定性事实」逃逸口删除；A4.2.151 条件裁决与已裁决 P1-RC-067 冲突 → 改衔接维持；A4.2.153「零反向调用」过度断言 → 改运行时 census 开放框架）。全部已修复。
- Independent draft review iteration 2: accept (task ses_02576ecb6ffen3b2L8E37Zi7iE) — 五项 round-1 修复全部实仓复核成立（scope 一致两处测试补充 / 类名更正 / 逃逸口删除 / A4.2.151 衔接维持 / A4.2.153 开放框架）；反松弛词零命中、格式完整、退出标准可测。2 项非阻塞 Minor 已处理：A4.2.150「或 erp-mnt-service 测试侧补充」第三处测试补充矛盾删除；`testSparePartPostingBasic:100` 更正为 `:101`。

## Closure Gates

> 本计划以只读审计为主 + 两处测试侧补充（A4.2.148/149，均限 erp-mnt-service 测试类目）。closure 时确认 checker 未触发 actual > baseline（测试补充不触生产代码，预期无漂移）。

- [x] 范围内行为完成（八项存疑点均有运行时证据 + 明确裁决）
- [x] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [x] 已运行验证（`mvn test -pl module-maintenance/erp-mnt-service` 相关测试类 + checker actual=baseline 确认）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-064/065/066/067~071 + P2-RC-060/061 修复实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认 + 一处测试补充；P1 修复义务归 MR1 R1.0 展开器（P1-RC-064 触 ORM 须 ask-first + 独立 plan-audit），P2 登记不强制。本审计维持分级不撤销。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接）

## Closure

Status Note: 两阶段执行完成（2026-08-08）：Phase 1 八项存疑点运行时证据全部采集——三项主路径闭合（A4.2.147/149/150）+ 一项维持 P2-RC-061（A4.2.148，新增 IDLE 输入单测 `TestErpMntVisitRequestStateMachine#testVisitCompleteFromIdleEquipmentRestoresRunning` 实证）+ 四项维持 P1 既有 finding（A4.2.151-154 → P1-RC-067/068/069/070，无未经比对新建）；验证报告落盘 `docs/audits/2026-08-08-0135-rc-ma4-a4-2-147-154-maintenance-runtime.md`；两处测试侧补充（A4.2.148 + A4.2.149 余额直查断言，均限 erp-mnt-service 测试类目，不触生产代码）。Phase 2 roadmap A4.2.147-154 `todo → done ✅` + arm-index 维持注记 + 日志 08-08.md 追加完成。测试全绿（erp-mnt-service 6 测试类 + erp-inv-service TestErpInvStockMoveBookkeeping 9）+ checker actual==baseline 零漂移。Closure Gates 1-6/8 勾选；Gate 7（独立结束审计）由独立子代理（新会话）在 CLOSURE_VERIFY 执行并勾选。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（CLOSURE_VERIFY 新会话，未重用执行者上下文）
- Evidence: 实仓复核（2026-08-08）：① 验证报告落盘且 9 段齐全（§2 八项逐项 L3 file:line + L4 断言，§5 裁决矩阵，§6 衔接表）；② 新测试实仓验证——`TestErpMntVisitRequestStateMachine#testVisitCompleteFromIdleEquipmentRestoresRunning:93`（seed IDLE→schedule→start→complete→断言 RUNNING）+ `TestErpMntSparePartPosting:126-129` 余额直查断言（totalQuantity==10=seed20−10）均真实落地且断言有效（GraphQL 驱动，非空壳）；③ roadmap A4.2.147-A4.2.154 八行全 `done ✅` 且与报告裁决一致；④ arm-index `:388` 维持注记已追加（P2-RC-061/P1-RC-067/068/069/070 均按既有分级衔接，无未经比对新建）；⑤ `docs/logs/2026/08-08.md` 完成条目已写；⑥ 生产代码 claims 抽查成立（`EquipmentStatusLinker.restoreToRunning:38`/`linkToDown:31`、`SparePartIssueService:55`→`IErpInvStockMoveBiz.generateMove`、`ErpInvStockMoveProcessor#validateAvailable:116`/`isNegativeStockAllowed:285`/`ERR_AVAILABLE_INSUFFICIENT:129`、module-assets 零 `ErpMntEquipment`/`DECOMMISSIONED` 引用）；⑦ 检查清单无剩余 `[ ]`（唯一未勾选项 = 本独立结束审计门，现由本审计勾选）；⑧ Deferred 段仅列 P1/P2 修复实现（归 MR1 successor），无范围内缺陷降级

Follow-up:

- P2-RC-061（IDLE 恢复分支）修复归 MR1 R1.0 展开器，修复形态择期裁决（纯 BizModel 前态恢复逻辑预授权 / 若加 `ErpMntEquipment` preMaintenanceStatus 快照列则 ORM ask-first + 独立 plan-audit）
- P1-RC-067/068/069/070 修复义务归 MR1 R1.0 展开器（按 arm-index :252-255 既有分级与预授权/ask-first 约束执行）
