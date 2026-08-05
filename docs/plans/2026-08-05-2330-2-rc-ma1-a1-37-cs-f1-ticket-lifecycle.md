# 2026-08-05-2330-2 rc-ma1-a1-37-cs-f1-ticket-lifecycle 客服域 cs-F1 工单生命周期需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.37（MA1 需求追踪矩阵审计 — cs-F1 工单生命周期：创建/分派接受/解决确认/计时录入）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.37
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.37 的 0.2 依赖）、`2026-08-05-2330-3-rc-ma1-a1-38-cs-f2-sla-escalation.md`（cs-F2 SLA 与升级同批 N=3，SLA 计时依赖本切片工单状态迁移与 startDateTime 记录，F1 先于 F2）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.37 给出 UC 清单 = `UC-CS-01/02/03/11`（4 UC），含 `use-cases.md:3/23/42/203` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 cs 域首个 RC 切片（cs 域共 4 切片 A1.37/A1.38/A1.39/A1.40，本切片工单生命周期为后续 SLA/知识库/调查权益的基础前置）。

- **L1 需求契约（权威真相源）**：`docs/design/customer-service/use-cases.md`：
  - **UC-CS-01 客户创建工单**（`:3`）：客户提交工单信息 → 系统读取客户信息**建议匹配 ticketType/slaPolicy** → 系统自动计算 **SLA 截止时间(deadlineDateTime = now + slaPolicy.resolveHours)** → 系统根据 ticketType+team 匹配规则**自动分配处理人(轮转/最少未结工单)** → 工单状态 NEW→ASSIGNED → 系统向客户**发送工单确认通知(含工单编号 TK{YYYYMM}{SEQ4})**。
  - **UC-CS-02 工单分派与接受**（`:23`）：主管/系统将 NEW 工单分派给处理人 → 更新 assignedToId，状态→ASSIGNED → 处理人收到通知点击"接受"→ IN_PROGRESS，**startDateTime 记录** → **如处理人拒绝，填写拒绝原因，工单回到 NEW**。异常：处理人长时间(>2h)不响应 → 自动升级通知主管。
  - **UC-CS-03 工单解决与客户确认**（`:42`）：处理人提交解决方案 → 状态→RESOLVED → **SLA 停止计时，计算实际耗时(duration = now - startDateTime)** → 判断是否超时(now > deadlineDateTime → isSlaCompleted=false) → **通知客户确认** → **客户确认→CLOSED(endDateTime=now)；客户驳回→IN_PROGRESS**。后置：超时未确认(>7天)自动 CLOSED。
  - **UC-CS-11 工单计时录入**（`:203`）：客服点击"开始计时"→创建计时器 session → 可**暂停/恢复计时** → "停止计时"→生成 ErpCsTimeEntry(startTime/endTime/duration 自动计算) → 补充 description/isBillable → **可计费条目自动进入审批** → 审批通过→工单总工时聚合(totalTimeSpent/totalBillableTime)。异常：单次计时超 12h→自动停止；**同一客服同一时刻只能启动一个计时器**。
  - **L1 关键不变量**：① 创建时自动建议 ticketType/slaPolicy + 自动计算 deadline + 自动分配 + TK 编号 + 通知；② 分派拒绝路径回 NEW + 2h 不响应升级；③ 解决时 SLA 停止 + 客户确认/驳回门控 + 7 天自动关闭；④ 计时器 session 暂停/恢复 + 12h 自动停 + 单计时器约束 + 工时聚合。

- **L3 代码实现现状（实测）**——**状态机核心完整，但创建自动富化/拒绝路径/客户确认门控/计时器 session 大面积缺失**：
  - **UC-CS-01 客户创建工单（⚠️ 仅裸 CRUD 持久化，6 项自动富化全缺）**：
    - `ErpCsTicketBizModel.java:54-55`（`module-cs/erp-cs-service/.../entity/ErpCsTicketBizModel.java`）+ `defaultPrepareSave:78-85` **仅填 businessDate**，无其他逻辑。`ErpCsTicket.xbiz` 仅审批流 mutation 无 save 时富化。
    - **建议匹配 ticketType/slaPolicy ❌ 缺失**：`ErpCsTicketType.defaultSlaPolicyId`(orm.xml:231) 存在但 save 时从不读取。
    - **自动计算 deadlineDateTime ❌ 缺失**（save 路径）：仅在 `ErpCsTicketMatchAndAttachSlaProcessor.java:62-63`（须单独调 matchAndAttachSla mutation）计算，不自动触发。
    - **自动分配处理人(轮转/最少未结) ❌ 缺失**：`assign():101-117` 从调用方取 assignedToId，无分配算法。config flag `erp-cs.auto-assign-on-create`（`ErpCsConstants.java:55` + `ErpCsConfigs.java:26-28`）**已声明但 main 源码零引用——死 flag**。
    - **NEW→ASSIGNED at create ❌ 缺失**：save 持久化调用方传入的 status；status 列必填(orm.xml:165)无默认。测试/E2E 均显式传 status='NEW'。
    - **TK{YYYYMM}{SEQ4} 编号 ❌ 缺失**：code 列 tagSet="var"(orm.xml:148) 用户自填；grep `TK{YYYYMM}`/`generateCode`/`nextTicketCode` 零命中。仅 catalog 驱动创建 `ErpCsServiceCatalogItemCreateFromCatalogProcessor.java:132` 合成 `TK-<millis>`（非 UC-CS-01 客户创建）。
    - **客户确认通知 ❌ 缺失**：save 路径无 notify 调用。仅 SLA-overdue/CSAT-reminder/entitlement-expiry 通知事件存在（`ErpCsConstants.java:71,77,114`）。
  - **UC-CS-02 工单分派与接受（✅ 分派+接受记录；⚠️ 拒绝路径 + 2h 升级缺失）**：
    - `assign():101-117`（NEW→ASSIGNED + assignedToId + 写 ErpCsTicketAction ASSIGN）✅；`start():119-134`（ASSIGNED→IN_PROGRESS + `setStartDateTime(CoreMetrics.currentTimestamp()):129`）✅（"接受"隐式=调 start，无独立 acceptTicket）。
    - **拒绝路径(回 NEW + 拒绝原因) ❌ 缺失**：grep `rejectTicket`/`acceptTicket` 零文件。ASSIGNED 后唯一前进路径是 start，无"回 NEW"路径。
    - **2h 不响应自动升级 ❌ 缺失**：无 scheduler 检查"已分派未开始>2h"；仅 SLA-overdue 扫描（`ErpCsTicketScanOverdueTicketsProcessor.java:48-77`，基于 deadlineDateTime）。
  - **UC-CS-03 工单解决与客户确认（✅ resolve+SLA 停止+reopen；⚠️ 客户确认门控 + 7 天自动关闭缺失）**：
    - `resolve():136-142` → `ErpCsTicketResolveProcessor.java:35-65`（IN_PROGRESS→RESOLVED + duration 分钟 from startDateTime `:43-46` + isSlaCompleted=(deadline==null||now≤deadline) `:48-50`）✅。
    - `close():144-164`（RESOLVED→CLOSED + endDateTime `:159` + breach 需 remark 守卫 `:152-157`）✅——**但 close 是操作员驱动，无客户确认前置门控**（任何操作员可关闭，无门户侧"客户确认"路径）。
    - `reopen():166-170` → `ErpCsTicketReopenProcessor.java:36-51`（RESOLVED→IN_PROGRESS + 取消未响应 survey `:48-63`）✅——客户驳回路径经 reopen 可达但语义是"操作员 reopen"非"客户门户驳回"。
    - **7 天自动关闭 ❌ 缺失**：grep `autoClose`/`7 days`/`AUTOCLOSE` 零命中，无定时任务。
    - **通知客户确认 ❌ 缺失**：无"ticket resolved, please confirm"通知事件（CSAT survey 触发是满意度调查非解决确认门控）。
  - **UC-CS-11 工单计时录入（⚠️ 仅 ORM 实体 + 裸 CRUD 壳，计时器 session 全缺）**：
    - `ErpCsTimeEntry` 实体存在（orm.xml:779-831，含 startTime/endTime/duration/isBillable/billingRate/billableAmount/approvalStatus[PENDING/APPROVED/REJECTED]/source[MANUAL/TIMER_IMPORT]）+ `ErpCsTimeEntryBizModel.java:11-19`（19 行空 CrudBizModel 无方法）+ 标准 CRUD 页面/菜单（`erp-cs.action-auth.xml:91-97` tagged UC-CS-11）。
    - **startTimer/stopTimer/pause/resume ❌ 全缺**：grep `startTimer|stopTimer|pauseTimer|resumeTimer|TimerSession|activeTimer` 零命中。
    - **自动 duration 计算 ❌ 缺失** + **12h 自动停止 ❌ 缺失** + **单计时器约束 ❌ 缺失** + **totalTimeSpent/totalBillableTime 聚合 ❌ 缺失**。
    - **config `erp-cs.time-tracking-enabled` ❌ 未声明**（`ErpCsConstants.java`/`ErpCsConfigs.java` 均无；grep 仅菜单 i18n）——UC-CS-11 前置条件无法满足。
  - **工单状态机（✅ 核心迁移齐全 + 非法守卫）**：status 字段 + dict `erp-cs/ticket-status`（orm.xml:30-37）6 态 NEW/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED/CANCELLED；常量 `ErpCsConstants.java:18-23`。迁移：assign(NEW→ASSIGNED)/start(ASSIGNED→IN_PROGRESS)/resolve(IN_PROGRESS→RESOLVED)/close(RESOLVED→CLOSED)/reopen(RESOLVED→IN_PROGRESS)/cancel(any non-terminal→CANCELLED)。非法守卫 ErrorCode：`ERR_INVALID_TICKET_STATUS_TRANSITION`/`ERR_TICKET_ALREADY_TERMINAL`/`ERR_TICKET_CLOSE_BREACHED_NO_REASON`/`ERR_TICKET_NOT_FOUND`（`ErpCsErrors.java:51-69`）。每迁移写 ErpCsTicketAction 审计行。
  - **跨域 daoFor**：module-cs 生产代码**零跨域 daoFor**（38 个 main daoFor 调用全 ErpCs* 自域）；跨域访问正确经 I*Biz（IErpMdPartnerBiz/IErpSysNotificationBiz）。P1-MA1-022 不涉及 cs。

- **L4 测试证据现状**（`module-cs/erp-cs-service/src/test/java/`）：
  - `TestErpCsTicketSlaCsat.java`（412 行，12 @Test）**强**：全生命周期 + 非法迁移 ErrorCode 断言 + SLA-match deadline + scanOverdue ESCALATE 审计 + 幂等 + CSAT 生命周期 + reopen 取消 survey + close-breached 拒绝 + findSlaWarnings。
  - E2E `tests/e2e/business-actions/cs-ticket.action.spec.ts`（105 行，2 tests）**强**：6 态状态机经 GraphQL assign/start/resolve/close + status 断言 + 非法迁移拒绝；cancel 直达。
  - **⚠️ 测试缺口**：① UC-CS-01 自动创建语义**零测试**（功能不存在）；② ErpCsTimeEntry 仅 ORM 存在，**零计时器行为测试**（功能不存在）；③ 客户确认门控/7 天自动关闭**零测试**。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14）：cs Ticket 6 态全迁移 + **SLA 计时联动完整**（startDateTime=首次 IN_PROGRESS + isSlaCompleted=(now≤deadline) + **reopen 保留 startDateTime 时长累加重算**——候选 P0「SLA 计时恢复累加缺失」经证据证伪）；**cs 域 zero P1**（候选 P0 证伪）。`P2-MA2-067`（cs NEW>1h/ASSIGNED>2h 滞留升级未实现 + findSlaWarnings 无 scheduler）watch-only 归 owner doc Deferred。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：复用 A2.14 Ticket 6 态状态机 + SLA 计时联动已证实行为，只补需求视角差异（创建自动富化缺失 / 拒绝路径缺失 / 客户确认门控缺失 / 7 天自动关闭缺失 / 计时器 session 全缺）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P2-MA2-067`（cs 滞留升级 watch-only，与本切片 UC-CS-02 "2h 不响应升级"同主题但本切片是 F1 工单生命周期视角）；cs 域**无既有 RC finding**（本切片为 cs 域首批 RC 切片）。projects 域最新 RC 编号 = P1-RC-051 / P2-RC-049（A1.36 同批 N=1 续编 P1-RC-052/053 + P2-RC-050）。**cs 域 RC finding 从本切片起始编**——本切片新 finding 续全仓 RC 序列。本切片须 grep arm-index cs ticket/create/assign/reject/resolve/close/timer/time-entry/auto-assign 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10。本切片候选偏差（自动富化/拒绝路径/客户确认/计时器）属**代码逻辑**类（预授权）。若修复触及 ORM（如计时器 session 实体/单计时器约束）须 ask-first。

- **剩余差距**：A1.37 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.37 报告并登记 finding，解除 cs 域首个切片证据缺口。

## Goals

- 产出 A1.37 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-37-cs-f1-ticket-lifecycle.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-CS-01/02/03/11 逐条核验**每条验收标准**（完整枚举，§3）：创建（建议匹配/自动 deadline/自动分配/TK 编号/通知/NEW→ASSIGNED）、分派接受（assignedToId/startDateTime/拒绝回 NEW/2h 升级）、解决确认（SLA 停止/duration/客户确认 CLOSED/驳回 IN_PROGRESS/7 天自动关闭）、计时（start/pause/resume/stop/duration/12h/单计时器/聚合）全链逐条。
- 对候选缺口给出分级结论：①UC-CS-01 **创建自动富化大面积缺失**（建议匹配 ticketType/slaPolicy + 自动 deadline + 自动分配 + TK 编号 + 确认通知 + NEW→ASSIGNED 6 项全缺，仅裸 CRUD）倾向 **P1**（§4 三判据复核 cs owner doc README/use-cases 是否显式 Deferred + 人工批准——`auto-assign-on-create` 死 flag 说明意图存在但未接线）；②UC-CS-02 **拒绝路径 + 2h 不响应升级缺失**（拒绝回 NEW 不存在 + 2h 升级无 scheduler——与 P2-MA2-067 滞留升级同主题须 §7 裁决复用 or 新增）倾向 **P1/P2**；③UC-CS-03 **客户确认门控 + 7 天自动关闭缺失**（close 操作员驱动无客户门户门控 + 7 天无 scheduler）倾向 **P1/P2**（区分"resolve+SLA 停止+reopen"接受部分 vs "客户确认门控"缺失部分）；④UC-CS-11 **计时器 session 完全未实现**（仅 ORM 实体+裸 CRUD 壳，start/pause/resume/stop/12h/单计时器/聚合全缺 + config flag 未声明）倾向 **P1**——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（续编 P1-RC-054+ / P2-RC-051+）并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/README.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.38 cs-F2 SLA 与升级 / A1.39 cs-F3 知识库 / A1.40 cs-F4 调查权益 独立 plan；A1.37 只覆盖 UC-CS-01/02/03/11）。
- **不复审 SLA 超时扫描/升级/CSAT 调查/知识库/服务目录**（UC-CS-04 属 A1.38 / UC-CS-05/06/07 属 A1.39 / UC-CS-08/09/10/12 属 A1.40；本切片仅核工单生命周期创建-分派-解决-计时）。
- **不重审 P2-MA2-067 cs SLA 计时行为**（§去重协议：watch-only，只补需求视角差异[创建富化/拒绝/客户确认/计时器]；A2.14 已证伪 SLA 计时恢复累加 P0 候选）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.37 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.37 UC 锚点）+ `docs/design/customer-service/use-cases.md`（L1 真相源）+ `docs/design/customer-service/README.md` + `sla.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 A2.14 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-cs/erp-cs-service -Dtest=TestErpCsTicketSlaCsat`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-37-cs-f1-ticket-lifecycle.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-CS-01/02/03/11 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:3/23/42/203` 验收标准原文；L2 引用 cs `README.md` + `sla.md`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpCsTicketBizModel`/`ErpCsTicketResolveProcessor`/`ErpCsTicketReopenProcessor`/`ErpCsTicketMatchAndAttachSlaProcessor`/`ErpCsTimeEntryBizModel`/`ErpCsConfigs`/`ErpCsConstants`（含行号）；L4 引用 `TestErpCsTicketSlaCsat`#method + `tests/e2e/business-actions/cs-ticket.action.spec.ts`（注明断言强度）；L5 复用 A2.14（Ticket 6 态 + SLA 计时联动 PASS + reopen 保留 startDateTime 证伪 P0 候选）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：UC-CS-01 ①建议匹配 ticketType/slaPolicy（defaultPrepareSave:78-85 仅填 businessDate + `ErpCsTicketType.defaultSlaPolicyId` 存在但 save 时从不读 → **❌**）②自动 deadline（仅 matchAndAttachSla mutation 可达非自动 → **❌**）③自动分配（`assign` 取调用方 assignedToId 无算法 + `auto-assign-on-create` 死 flag 零引用 → **❌**）④TK 编号（code tagSet="var" 用户自填 + grep generateCode 零命中 → **❌**）⑤确认通知（save 无 notify → **❌**）⑥NEW→ASSIGNED at create（save 持久化调用方传入 status 无默认 → **❌**）；UC-CS-02 ①assign/start + startDateTime（✅ `:101-134`）②拒绝回 NEW + 拒绝原因（grep rejectTicket 零文件 → **❌**）③2h 不响应升级（无 scheduler → **❌**）；UC-CS-03 ①resolve + SLA 停止 + duration + isSlaCompleted（✅ ResolveProcessor:35-65）②客户确认→CLOSED（close:144-164 操作员驱动**无客户门户门控** → **⚠️ 偏离**）③客户驳回→IN_PROGRESS（reopen:166-170 可达但语义是操作员 reopen → **⚠️**）④7 天自动关闭（grep autoClose 零 → **❌**）⑤通知客户确认（无 resolved-please-confirm 事件 → **❌**）；UC-CS-11 ①ErpCsTimeEntry ORM + CRUD 壳（✅ 存在）②start/pause/resume/stop（grep startTimer 零 → **❌**）③自动 duration（无 → **❌**）④12h 自动停（无 → **❌**）⑤单计时器约束（无 → **❌**）⑥totalTimeSpent 聚合（无 → **❌**）⑦time-tracking-enabled config（未声明 → **❌**）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 4 UC 给出符合性结论（取最高）：UC-CS-01 → **P1**（新 `P1-RC-054`，6 项自动富化全缺仅裸 CRUD——`auto-assign-on-create` 死 flag 证明意图存在但未接线，§4 三判据复核 cs README/use-cases 均未显式 Deferred + product-scope 未裁剪 → Q4 强制实现；非 P0 工单 CRUD 主路径完整可手工全链 + cs 域不产生 GL）；UC-CS-02 → assign+start+startDateTime **接受** + 拒绝路径 **P2**（新 `P2-RC-051`，主路径[start]OK 边界[拒绝]弱 watch-only 声明 Q4 张力）+ 2h 升级 **reuse `P2-MA2-067`**（§7 同根因同控制点[cs ASSIGNED>2h 滞留时间维度升级]，追加 RC A1.37 交叉引用注记不新建）；UC-CS-03 → resolve+SLA 停止+reopen **接受 on 操作员语义**（A2.14 已证实）+ 客户确认门控 + 7 天自动关闭 **P2**（合并入 `P2-RC-051`，主路径[操作员 close/reopen]状态等价 + SLA 计时正确 + breach 守卫齐全，边界[客户门户门控+7 天自动关闭]弱 watch-only 声明 Q4 张力）；UC-CS-11 → **P1**（新 `P1-RC-055`，计时器 session 完全未实现仅 ORM 实体+19 行空 CrudBizModel 壳——UC-CS-11 是独立验收契约非可选项，§4 三判据复核 cs owner doc `time-tracking.md` 与 L1 一致未声明 Deferred + product-scope 未裁剪 → Q4 强制实现；修复触及 ORM[新增计时器 session 实体]须 ask-first；非 P0 工单生命周期主路径完整 + 计时是辅助功能）。每结论列明命中判据编号 + 三源对照 + §4 三判据复核。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-CS-01/02/03/11 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.14 来源
- [x] 4 UC 各有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 ①-④ 有明确分级；创建自动富化/拒绝路径/客户确认/计时器各有 §4 三判据复核路径；状态机核心（assign/start/resolve/close/reopen）接受结论成立

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-37-cs-f1-ticket-lifecycle.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` cs ticket/create/assign/reject/resolve/close/reopen/timer/time-entry/auto-assign/TK 同域同控制点后裁决——cs 域无既有 RC finding，本切片为 cs 域首批；与 `P2-MA2-067`（cs ASSIGNED>2h 滞留升级 watch-only）§7 裁决——**同根因同控制点**（cs ASSIGNED 状态滞留时间维度升级 + 无 scheduler 消费）→ **复用 `P2-MA2-067`**（追加 RC A1.37 交叉引用注记，不新建）；其余控制点（创建自动富化 / 拒绝路径 / 客户门户门控 / 7 天自动关闭 / 计时器 session）均为新根因 → 新建 `P1-RC-054`（UC-CS-01 创建自动富化 6 项全缺）+ `P1-RC-055`（UC-CS-11 计时器 session 完全未实现）+ `P2-RC-051`（UC-CS-02 ③拒绝路径 + UC-CS-03 ④⑤⑥⑦客户确认门控+7 天自动关闭 合并 watch-only）；续 A1.36 P1-RC-053/P2-RC-050 编号无冲突。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（SP-1 auto-assign-on-create 死 flag 在 delta/未来定制消费的运行时行为 / SP-2 matchAndAttachSla 作为创建后置手动步骤的运行时可达性 / SP-3 reopen 作为客户驳回替代路径的语义等价性 / SP-4 close 操作员驱动在无客户确认下是否产生数据完整性问题 / SP-5 ErpCsTimeEntry CRUD 壳在 delta 定制下计时器行为；每存疑点一行）。**P0 即时通道未触发**（本切片无 P0——创建富化/计时器缺失不破坏活跃数据/会计正确性 + 状态机核心完整 + SLA 计时联动正确 + 工单可手工操作全链 + cs 域不产生 GL 凭证）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（R1a-c=0/0/0, R1d=14, R2a=34, R2b=229≤240, R2c=1382 vs baseline 1380 [+2 非本审计引入], R2d=34 vs baseline 32 [+2 非本审计引入], R3=5, R4/5/7/8/11=0；EXIT=0）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**（无生产代码变更，注明"无回归风险"）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14 cs Ticket 6 态 + SLA 计时联动 PASS + reopen 保留 startDateTime 证伪 P0 候选 + close breach remark 守卫 PASS + P2-MA2-067 watch-only），列明只补的需求视角差异（创建自动富化缺失 / 拒绝路径缺失 / 客户确认门控缺失 / 7 天自动关闭缺失 / 计时器 session 全缺）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P1-RC-054` + `P1-RC-055` + `P2-RC-051` 入 RC 发现追踪分区（行 211-213）；audit reports 表新增 A1.37 行（行 105）；P2-MA2-067 行追加 RC A1.37 交叉引用注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在（含段落完整性自检清单）。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding（P1-RC-054 + P1-RC-055 + P2-RC-051）已写入 `arm-index.md`；静态存疑点清单已登记（SP-1~SP-5 供 A4.1/A4.2 展开）；P2-MA2-067 追加 RC 交叉引用注记（reuse 裁决落地）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02fb12991ffeciC4XdbQelY1c3，fresh session，未起草本计划）。17 项 load-bearing 引用经实仓复核 CONFIRMED TRUE：①A1.37 UC 锚点 UC-CS-01/02/03/11 ✅ 一致（inventory:371）；②`defaultPrepareSave:78-85` 仅 setBusinessDate 无富化；③`erp-cs.auto-assign-on-create` 死 flag（ErpCsConstants:55 + ErpCsConfigs:26-28 声明但 grep 全 module-cs 仅 3 自声明命中零调用方）；④ErpCsTimeEntryBizModel 19 行空 CrudBizModel；⑤startTimer/stopTimer/pause/resume/TimerSession grep 零命中；⑥time-tracking-enabled 未声明；⑦rejectTicket/acceptTicket grep 零；⑧autoClose/7 days grep 零；⑨TK{YYYYMM}/generateCode grep 零 + code tagSet="var"；⑩状态机 6 态 + 4 ErrorCode + 全迁移守卫；⑪cs 域无既有 RC finding（仅 P2-MA2-067 watch-only）；⑫A2.14 报告存在；⑬module-cs 生产零跨域 daoFor；⑭RC 编号严格顺序（A1.36→A1.37→A1.38 无冲突）；⑮orm.xml 行号精确。scope（UC-CS-01/02/03/11 only，无 A1.38/A1.39/A1.40 creep）、anti-slack 零禁词、methodology §1-§9 + §4 三判据 + §去重协议 reuse A2.14/P2-MA2-067 全对齐。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.37 报告 9 段齐全 + UC-CS-01/02/03/11 矩阵行（逐验收标准）+ finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.37 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录（R2c=1382/R2d=34，+2 delta 非本审计引入）+ finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（创建自动富化/拒绝路径/客户确认/计时器）属**代码逻辑**类（预授权——BizModel enrich + Processor + notify 接线）；计时器 session 若触及 ORM（session 实体/单计时器约束）须 ask-first。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-CS-02 拒绝/2h 升级与 cs-F2 A1.38 SLA 升级同域可协同修复）

## Closure

Status Note: 执行完成 2026-08-05，结束审计通过 2026-08-05。审计报告 `docs/audits/2026-08-05-2330-2-rc-ma1-a1-37-cs-f1-ticket-lifecycle.md` 9 段齐全落盘；UC-CS-01 → P1（P1-RC-054 创建自动富化 6 项全缺）/ UC-CS-02 → 部分接受 + P2（P2-RC-051 拒绝路径）+ reuse P2-MA2-067（2h 升级同控制点）/ UC-CS-03 → 部分接受 + P2（P2-RC-051 客户确认门控+7 天自动关闭）/ UC-CS-11 → P1（P1-RC-055 计时器 session 完全未实现）；零 P0；2 新 P1 + 1 新 P2 + 1 reuse 已入 arm-index RC 发现追踪分区 + A1.37 audit reports 表行 + P2-MA2-067 RC 交叉引用注记；复用 A2.14 cs Ticket 6 态+SLA 计时联动+候选 P0 证伪已证实行为只补需求视角差异。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER:2026-08-04-224309-mission-driver 闭环节点，fresh session，未参与执行）
- Evidence: 实仓复核全部通过——①报告存在（`docs/audits/2026-08-05-2330-2-rc-ma1-a1-37-cs-f1-ticket-lifecycle.md` 325 行 51KB，9 段齐全 + 段落完整性自检 §1-§9 全 `[x]`）；②arm-index 三 finding 已落（P1-RC-054/P1-RC-055/P2-RC-051 @ arm-index 行 211-213）+ A1.37 audit reports 表行 @ 行 105 + P2-MA2-067 RC A1.37 交叉引用注记 @ 行 639；③roadmap A1.37 状态 = `done`（`docs/backlog/requirement-compliance-roadmap.md:76`）；④日志存在 `docs/logs/2026/08-05.md`；⑤Phase status/items 一致性：两 Phase 均 `Status: completed` + 执行项与退出标准全 `[x]`，无 `[ ]` 残留；⑥反空心：报告逐验收标准给行号级证据 + 三判据复核 + Q4 张力声明，无空壳/占位；⑦Deferred honesty：finding 修复显式路由 MR0/MR1 + successor required，无范围内缺陷降级；⑧只读审计零生产代码变更，checker R2c/R2d +2 delta 已声明非本审计引入。
