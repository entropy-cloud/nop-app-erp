# 2026-08-17-2125-2-rc-mr1-r1-66-cs-timer-session RC-R1.66 — cs 计时器 session（P1-RC-055 A 类 ORM：ErpCsTicketTimerSession 实体 + start/pause/resume/stop + 12h + 单计时器 + 审批 + 聚合）

> Plan Status: active
> Last Reviewed: 2026-08-18
> Mission: requirement-compliance
> Work Item: RC-R1.66（P1-RC-055，UC-CS-11 ①~⑨ 计时器 session 子系统）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.66 行 + `docs/audits/arm-index.md` P1-RC-055 行（:229）+ 2026-08-12 批量裁决 A 类（roadmap 头 :40：「cs: RC-R1.66（新增 ErpCsTicketTimerSession 实体）」ORM 修改授权已批量批准）
> Related: `docs/design/customer-service/time-tracking.md`（§一-七 完整设计）；`docs/plans/2026-08-16-1634-2-rc-mr1-r1-58-qa-critical-item-veto.md`（A 类 ORM 加列 + 双独立子 agent 批准先例）；`docs/plans/2026-08-16-1634-1-rc-mr1-r1-57-crm-team-member-allocation.md`（新实体 + UK 先例）
> Audit: required

## Current Baseline

- **finding P1-RC-055（arm-index:229，UC-CS-11 ①~⑨）**：L1（`docs/design/customer-service/use-cases.md:209-219`）逐字要求 ①前置 `erp-cs.time-tracking-enabled=true` + ②「客服点击'开始计时'→ 系统创建计时器 session」+ ③「客服可暂停/恢复计时（暂停原因可选）」+ ④「客服点击'停止计时'→ 生成 ErpCsTimeEntry（startTime、endTime、duration 自动计算）」+ ⑤「客服补充 description、isBillable 标识 → 提交」+ ⑥「可计费条目自动进入审批（或超阈值触发审批）」+ ⑦「审批通过 → 工单总工时聚合（totalTimeSpent、totalBillableTime）」+ ⑧「单次计时超 12h → 自动停止」+ ⑨「同一客服同一时刻只能启动一个计时器」。L3 实仓（HEAD 核查）：
  - `ErpCsTimeEntryBizModel.java:1-19` = **19 行空 CrudBizModel**（仅构造器 setEntityName）；grep `startTimer|stopTimer|pauseTimer|resumeTimer|TimerSession|activeTimer` 跨 `module-cs/erp-cs-service/src/main` **零命中**——计时器子系统完全不存在；
  - `ErpCsTimeEntry` ORM（`module-cs/model/app-erp-cs.orm.xml:780-831`）字段齐全：startTime/endTime/duration（分钟，propId 5/6/7）/isBillable（propId 8 默认 true）/billingRate/billableAmount/approvalStatus（propId 12，dict `erp-cs/time-entry-approve-status`）/approvedById/approvedAt/source（propId 17，dict `erp-cs/time-entry-source` 含 MANUAL/TIMER_IMPORT）——**条目载体就绪，计时器 session 实体缺失**；
  - config：`time-tracking-enabled|time-entry-require-description|time-entry-approval-threshold|time-entry-auto-approve|default-billing-rate|time-entry-timer-max-hours` 跨 `ErpCsConstants/ErpCsConfigs` **零声明**（owner doc `time-tracking.md §七:246-253` 已定义默认值：true / true / 480 分钟 / false / 0 / 12）；
  - owner doc `time-tracking.md` 设计就绪：§2.2 计时器模式（含「每分钟更新 duration」呈现语义 + **:92「启动新计时器前自动停止当前计时器（提示确认）」多计时器约束句**——与 L1 ⑨ 不变量表述的调和见 Phase 1 D1-②）+ §2.3 暂停/恢复（totalDuration = 运行时长 − 暂停时长）+ §3.1-3.3 审批触发/状态机（DRAFT→PENDING→APPROVED/REJECTED）/审批人（**§3.3「工单.responsibleId」命名漂移：实体无此列，实际 assignedToId**）+ §四 **聚合明确「可选，非实体字段，通过 SQL 聚合」**（totalTimeSpent 含 APPROVED+PENDING / totalBillableTime 仅 APPROVED+isBillable / totalBilledAmount 同形）；
  - **id 空间不一致（D2 裁决输入）**：`ErpCsTimeEntry.agentId`（propId 4）与 `ErpCsAgentRate.agentId`（propId 3）均为 **BIGINT**，而 `ErpCsTicket.assignedToId`（orm.xml:157）为 **VARCHAR(36) stdDomain=userId** 且 notify USER_LIST 需 userId 字符串——新 session 实体的客服载体类型须裁决（BIGINT 镜像条目实体 vs stdDomain=userId 对齐操作者上下文）；
  - **审批零实现**：approvalStatus 列存在但无 submit/approve/reject mutation（grep 跨 main 零命中）；
  - **聚合零实现**：无 totalTimeSpent/totalBillableTime 查询。
- **Q4 判据**：§2 P1①（功能完全缺失）+ P1②（12h/单计时器异常路径零实现）+ P1⑤（零断言）。三判据复核均不成立（arm-index:229）→ Q4=(a) 强制实现。**2026-08-12 A 类批量裁决**（roadmap 头 :40，用户批准）：新增 `ErpCsTicketTimerSession` 实体 ORM 修改授权已批量批准（对齐 Q3 纯加性类自动执行范围：新增实体 + 自有 UK；越界回落双独立子 agent 批准，批准记录落盘计划文件）；**聚合走 SQL 查询非 ticket 加列**（arm-index 修复方向明示「若聚合走 SQL 查询非字段可降低 ORM 触及面」+ owner doc §四「非实体字段」——本计划采纳，ErpCsTicket 零加列）。
- **测试基线**：erp-cs-service **122 @Test 全绿**（2026-08-17 HEAD 递归实测；含 statemachine/entity/job 子包）。
- **compliance 基线**（`docs/audits/compliance-baseline.md` §BASELINE :427-449）：R2b=235 / R2c=1434 / R2d=35 / R10=9 / R12a=70（新实体 dao 预期 R2c baseline-raise，per-site 证据登记，对齐 R1.48/R1.56/R1.57 先例）。

## Goals

- **UC-CS-11 ①-⑨ 运行时成立**：`erp-cs.time-tracking-enabled` config 声明 + `ErpCsTicketTimerSession` 新实体（A 类已授权）+ startTimer/pauseTimer/resumeTimer/stopTimer mutation（per-mutation Processor 范式）+ 停止生成 ErpCsTimeEntry（duration = 运行 − 暂停自动计算，source=TIMER_IMPORT）+ 12h 自动停止守卫 + 单计时器唯一约束 + 审批触发（isBillable/threshold → PENDING + approve/reject mutation）+ 工单聚合 `@BizQuery`（totalTimeSpent/totalBillableTime/totalBilledAmount 三聚合，SQL 聚合零 ticket 加列）。
- **config 六键落地**（owner doc §七默认值对齐）。
- **测试补强 + 零回归**：新增计时器生命周期测试组 + erp-cs-service 122 基线全绿 + 全量构建 + compliance checker（baseline-raise 带 per-site 证据）。
- **owner doc 收敛**：time-tracking.md §实现约定补落地注记 + arm-index P1-RC-055 → done (RC-R1.66) + roadmap 行 done + logs 条目。

## Non-Goals

- **不在 ErpCsTicket 加聚合列**（owner doc §四明确 SQL 聚合；聚合经 @BizQuery 查询侧实现）。
- **不实现计费开票跨域**（§5.2 可计费条目 → sales 订单/发票属独立跨域契约，successor）。
- **不实现 HR 考勤导出**（§5.3 显式可选）。
- **不实现「每分钟更新 duration」的实时呈现**（§2.2 呈现语义经**惰性计算等价**：读取/停止时按 (now − startTime − Σ暂停) 结算——后台每分钟刷新 job 归 Deferred；Phase 1 D3 裁决记录等价性论证）。
- **不做 AMIS 计时器向导/按钮编排**（mutation 经 GraphQL 可达；页面编排归产品化 successor，对齐 R1.65 同批 Non-Goal）。
- **不改真相源契约段落**（use-cases L1 不动；time-tracking.md 契约段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：新增实体[Q3 纯加性，2026-08-12 A 类已授权] + BizModel mutation + config + 聚合查询；Q4=(a) 强制实现）
- Owner Docs: `docs/design/customer-service/time-tracking.md`（§一-七）+ `docs/design/customer-service/use-cases.md`（L1 UC-CS-11）
- Skill Selection Basis: ORM 新实体（增量重生成 `mvn clean install -DskipTests`，勿重跑 nop-cli gen）；BizModel/per-mutation Processor（`nop-backend-dev`）；测试（`nop-testing`：JunitAutoTestCase + GraphQL 引擎 + `_cases/` 快照）。

## Infrastructure And Config Prereqs

- **config 六键**（`ErpCsConstants` + `ErpCsConfigs` reader，默认值对齐 owner doc §七）：`erp-cs.time-tracking-enabled=true` / `erp-cs.time-entry-require-description=true` / `erp-cs.time-entry-approval-threshold=480` / `erp-cs.time-entry-auto-approve=false` / `erp-cs.default-billing-rate=0` / `erp-cs.time-entry-timer-max-hours=12`。
- **ORM 变更**（新增实体）→ `mvn clean install -DskipTests` 增量重生成（**不要重跑 nop-cli gen**），生成产物核对（propId 分配 + DDL 三方言 + dict `erp-cs/timer-session-status` 数据文件）。
- **双独立子 agent 批准 checkbox**（A 类越界回落：两个独立子 agent fresh session 分别检查批准 ORM 新实体，批准记录落盘本计划）。
- 无外部服务/端口/密钥。

## Execution Plan

### Phase 1 - 实体形态/mutation 载体/12h 语义/审批/聚合裁决（Decision）

Status: planned
Targets: `module-cs/model/app-erp-cs.orm.xml`（ErpCsTicketTimerSession 草案）；`docs/design/customer-service/time-tracking.md`（实现注记草案）
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: none

- [ ] `Explore`（D2 前置，独立完成项）：核实现有测试/种子里 `ErpCsTimeEntry.agentId` 的实际取值空间（NopAuthUser.id? 员工 id? 自造 long?）——grep 测试/种子 + 既有 cs 测试 seed 模式，结论证据（文件:行）落盘本计划决策记录，作为 D2 定稿输入。
      - Skill: `nop-backend-dev`
- [ ] `Decision` D1 实体形态与二次启动语义：①`ErpCsTicketTimerSession` 列集（arm-index 修复方向骨架：agentId/ticketId/startTime/pauseStartDateTime/cumulativePauseMinutes/pauseReason/status[RUNNING/PAUSED/STOPPED] + 标准审计列；propId 1..n 顺延，对齐 R1.57 新实体先例）+ **单计时器 UK 裁决**——选项 A（推荐）：`UK(agentId, activeFlag)` + activeFlag 仅 RUNNING 时非空（MySQL/PG NULL 可重复语义天然放行历史行，DB 级单活跃约束）；选项 B：仅应用级 check-then-act（无 DB 兜底，并发窗口）；选项 C：UK(agentId, status) 不可行（历史 STOPPED 多行冲突）；记录选择 + 方言验证（H2 测试库 NULL-UK 行为核对）。②**二次启动行为裁决（owner doc §2.2:92 分歧）**：owner doc 设计「启动新计时器前自动停止当前计时器（提示确认）」，本计划裁决 = **服务端拒绝 + 专属错误码**（选项 A：单计时器不变量为服务端硬约束；「自动停止+确认」为客户端编排 stop→start，前端提示属 Non-Goal successor——拒绝原语使确认流可前端组合实现）；否决选项 B（服务端静默自动停止旧计时器——绕过操作者确认语义 + 隐式生成条目）；记录选择 + 理由；测试 ① 断言以本裁决为准。
      - Skill: `nop-backend-dev`
- [ ] `Decision` D2 客服载体类型（Prereqs: 上方 Explore）：新实体 agent 列类型——选项 A：`stdDomain=userId` VARCHAR(36)（对齐操作者上下文 `context.getUserId()` + 单计时器约束按登录用户，notify/审计直接可用）**但停止时写 ErpCsTimeEntry.agentId（BIGINT）须映射**；选项 B：BIGINT 镜像 ErpCsTimeEntry.agentId（条目生成零映射**但 startTimer 须把 userId 映射 BIGINT**）；按 Explore 结论定稿；记录映射函数或统一裁决 + 残留风险。
      - Skill: `nop-backend-dev`
- [ ] `Decision` D3 12h 自动停止语义：**惰性结算**（start/pause/resume/stop/读取各入口校验 `now − startTime − Σ暂停 > max-hours` 时先按 12h 上限结算并置 STOPPED + 生成条目）vs 后台 job 扫描——惰性为主（零新 job，owner doc「自动停止」行为语义达成：任何下一操作或读取时点观察到的 session 永不超 12h）；「每分钟更新 duration」呈现语义经读取时惰性计算等价实现（Non-Goal 登记实时刷新 job）；记录选择 + 边界（停止操作发生在 >12h 后 → duration 封顶 12h）+ **残留风险**：永不再被操作的 RUNNING 会话不物化封顶条目（聚合查询不触发结算——数据缺失非数据错误，条目在下次任一入口触碰时补物化；兜底扫描 job 归 Deferred successor，触发条件 = 运营要求无人值守结算时）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` D4 审批触发与 mutation：stop → 条目 DRAFT（description/isBillable 补充后）submit → `isBillable=true` 或 `duration ≥ approval-threshold` → PENDING（§3.1）+ `time-entry-auto-approve=true` 直通 APPROVED + approve/reject mutation（审批人 §3.3：工单 assignedToId → 团队 teamLeaderId → 主管兜底经 config `erp-cs.time-entry-approver-id` 指定，空则 WARN 跳过——**owner doc 命名漂移登记**：§3.3「工单.responsibleId」实体不存在，实际列为 `assignedToId`（orm.xml:157），替换属实并回填 owner doc 注记；「Admin 手动指定」经 config 键显式化）；REJECTED 可改后重新 submit（§3.2 状态机）；载体 = `ErpCsTimeEntryBizModel` 增 submit/approve/reject（per-mutation Processor 范式）；require-description config 门控 submit。**§3.1/§3.2 子特性裁决**：「按组织配置要求全部审批」（§3.1 第三触发）与「PENDING→DRAFT 退回」路径（§3.2）超出 UC-CS-11 验收标准 → Deferred But Adjudicated 登记（非 L1 要求）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` D5 聚合载体：`ErpCsTicketBizModel`（或 TimeEntryBizModel）增 `@BizQuery` `totalTimeSpent`/`totalBillableTime`/`totalBilledAmount` **三聚合全量实现**（owner doc §四 同形 SQL 设计，无裁剪）——SQL SUM 聚合（§四口径：totalTimeSpent 含 APPROVED+PENDING；totalBillableTime 仅 isBillable+APPROVED；totalBilledAmount = SUM(billableAmount) isBillable+APPROVED），零 ticket 加列；经 IBiz/dao 查询实现（跨 BizModel 经 IErpCsTimeEntryBiz 注入，R2b 合规）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` D6 config 六键落点 + 门控语义：`time-tracking-enabled=false` → 全部 timer mutation 拒绝（明确错误码，如 ERR_TIME_TRACKING_DISABLED）+ 手工 MANUAL 条目 CRUD 不受影响；`default-billing-rate` → 停止生成条目时 isBillable=true 且 billingRate 缺失时填充 × 费率换算（**§1.2 费率优先级 2-3 级**[ErpCsAgentRate/权益级费率]超出 UC-CS-11 → Deferred 登记，本计划仅全局默认级）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] Explore 项完成（agentId 取值空间证据落盘）+ D1-D6 各记录选择 + 替代方案 + 残留风险
- [ ] orm.xml 新实体草案 well-formed（`xmllint --noout`）+ dict `erp-cs/timer-session-status` 数据文件草案

### Phase 2 - ORM 落地 + 计时器实现（Add）

Status: planned
Targets: `module-cs/model/app-erp-cs.orm.xml`（ErpCsTicketTimerSession）；`module-cs/erp-cs-service/.../entity/ErpCsTicketTimerSessionBizModel.java` + `IErpCsTicketTimerSessionBiz`（dao 模块）+ `processor/ErpCsTicketTimerSession*Processor.java`；`ErpCsTimeEntryBizModel.java`（submit/approve/reject + 聚合查询）；`ErpCsConstants.java`/`ErpCsConfigs.java`；`module-cs/erp-cs-meta`（dict 数据文件）；生成产物
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [ ] `Add` orm.xml 新增 `ErpCsTicketTimerSession`（D1/D2 定稿）+ `mvn clean install -DskipTests` 增量重生成 + 生成产物核对（实体/XMeta/DDL 三方言/IBiz）
      - Skill: `nop-backend-dev`
- [ ] **双独立子 agent 批准 checkbox（ORM 保护区域，A 类越界回落）**：两个独立子 agent（fresh session）分别检查批准新实体（纯加性新表 + 自有 UK，零既有实体改动/零删除/零迁移），批准记录落盘本计划（ses id + 结论）
      - Skill: `nop-backend-dev`
- [ ] `Add` 计时器四 mutation（start/pause/resume/stop）+ 12h 惰性结算 + 单计时器守卫 + 停止生成 ErpCsTimeEntry（duration = 运行 − Σ暂停，分钟；source=TIMER_IMPORT；startTime=会话开始）+ config 六键 reader + 门控
      - Skill: `nop-backend-dev`
- [ ] `Add` 条目审批链（submit/approve/reject per-mutation Processor + 触发条件 + 审批人链 + require-description/auto-approve/threshold 门控）+ 聚合 `@BizQuery`（D4/D5 定稿）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] orm.xml 新实体 + 生成产物一致（实体 getter/setter + DDL 三方言 + dict）；propId 无冲突
- [ ] 双独立子 agent 批准记录落盘（两个 APPROVE 结论 + 检查范围）
- [ ] `mvn compile -pl module-cs/erp-cs-service -am` 通过

### Phase 3 - 测试 + 文档回填（Proof）

Status: planned
Targets: `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsTicketTimerSession.java`（新建）+ `TestErpCsTimeEntryApproval.java`（新建或并入）；`docs/design/customer-service/time-tracking.md`（实现注记）；`docs/audits/arm-index.md`（P1-RC-055 行）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.66 行）；`docs/logs/2026/08-{17,18}.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [ ] `Proof` `TestErpCsTicketTimerSession` 测试组：①start → RUNNING + 单计时器（同客服二次 start **按 D1-② 裁决拒绝** + 专属错误码，UK/应用守卫双路径）；②pause/resume（cumulativePauseMinutes 累计 + pauseReason 可选）；③stop → STOPPED + ErpCsTimeEntry 生成（duration = 运行 − Σ暂停断言 + source=TIMER_IMPORT）；④12h 惰性结算（>12h 后任意操作/读取 → 封顶结算 STOPPED + 条目 720 分钟）；⑤config off → mutation 拒绝 + MANUAL CRUD 不受影响 + **require-description 门控**（true + 空 description → submit 拒绝；false → 放行）；⑥跨工单单计时器（同客服不同工单仍唯一）；⑦审批链（isBillable → PENDING → approve → APPROVED + 聚合变化；reject → REJECTED → 修改重提；threshold 触发；auto-approve 直通）；⑧聚合查询（totalTimeSpent 含 PENDING/APPROVED 口径 + totalBillableTime 仅 APPROVED+isBillable 口径 + totalBilledAmount 三聚合断言）；⑨`_cases/` 快照录制
      - Skill: `nop-testing`
- [ ] `Proof` 零回归验证：`mvn test -pl module-cs/erp-cs-service` 全绿（122 基线 + 新增）
      - Skill: `nop-testing`
- [ ] `Proof` compliance checker 复跑：`bash docs/audits/nop-compliance-checker.sh`——新实体/Processor daoFor 站点若致 actual > baseline 则 baseline-raise 带 per-site 证据登记（对齐 R1.48/R1.56/R1.57 先例；**属 Closure Gates 级验证，此处仅登记证据**）
      - Skill: none
- [ ] `Add` 文档回填：time-tracking.md §实现约定补落地注记（D1-D6 裁决 + 二次启动拒绝语义 + 惰性结算等价性与残留 + 聚合口径 + responsibleId→assignedToId 命名漂移）+ arm-index P1-RC-055 → done (RC-R1.66) + **roadmap RC-R1.66 行标签按 R1.63/R1.64 先例补 A 类授权措辞** + 行 done ✅ + logs 条目
      - Skill: none

Exit Criteria:

- [ ] 测试组 ①-⑨ 全绿（指定成功/失败模式：单计时器拒绝 + 12h 封顶 + config off/require-description 拒绝 + duration 数学断言逐项）
- [ ] erp-cs-service 模块测试全绿（122 基线零回归）

（全仓 `mvn clean install -DskipTests` + compliance checker 最终裁决属 Closure Gates，不在阶段退出重复——见执行时规则 7。）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_feec11eeeffecVQCvISyLILGQ2`) — 2 MAJOR + 5 MINOR，基线核查全 PASS。M1 已修订：D1 增 ②「二次启动行为裁决」——owner doc §2.2:92「自动停止当前计时器（提示确认）」vs 服务端拒绝的分歧显式裁决（选项 A 服务端拒绝 + 专属错误码，确认流为客户端 stop→start 编排 successor；否决服务端静默自动停止），baseline 增 :92 引文披露，测试 ① 断言改引 D1-② 裁决；M2 已修订：D5「totalBilledAmount 可选」反松弛措辞消除——三聚合（totalTimeSpent/totalBillableTime/totalBilledAmount）全量实现 + 测试 ⑧ 补断言；m3 已修订：D4 登记 §3.3 命名漂移（responsibleId 实体不存在 → assignedToId）+ 主管兜底经 config `erp-cs.time-entry-approver-id` 显式化（回填 owner doc 注记）；m4 已修订：D4/D6 登记 §3.1 第三触发（按组织配置）+ §3.2 退回路径（PENDING→DRAFT）+ §1.2 费率优先级 2-3 级 → Deferred But Adjudicated 三条目；m5 已修订：D3 增残留风险（永不再被操作的 RUNNING 会话不物化封顶条目，聚合查询不触发结算）+ Deferred 条目 Why Not Blocking 同步；m6 已修订：Explore 提升为独立 checkbox 项（D2 前置，证据落盘 exit criterion）；m7 已修订：测试 ⑤ 增 require-description 门控双路径断言。另：Phase 3 全仓构建/checker 移出阶段退出（归 Closure Gates，对齐执行时规则 7 与 R1.65 同批一致性）。
- Independent draft review iteration 2: `accept`（`ses_feeba17fcffev5QqojBTwXP0nl`）——迭代 1 全部 7 项修复逐项核实 FIXED（M1 §2.2:92 逐字核对 + 规则 9 合规 / M2 三聚合无松弛 / m3-m7 + Phase 3 归位），0 MAJOR；2 非阻塞 MINOR（Phase 1 Item Types `Decision | Add` 过度声明——无 Add 标签项；Goals 仅列双聚合——与 D5 三聚合对齐更清晰）已由起草者就地修复（Item Types 改 `Decision` + Goals 增 totalBilledAmount）→ 草案审查收敛，`Plan Status: draft → active`。

## Closure Gates

- [ ] 范围内行为完成（UC-CS-11 ①-⑨ 计时器子系统落地）
- [ ] 相关文档对齐（time-tracking.md/arm-index/roadmap/logs）
- [ ] 已运行验证（`mvn test -pl module-cs/erp-cs-service` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 每分钟实时 duration 刷新 + 无人值守 12h 结算扫描（后台 job / 前端轮询）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 惰性计算在任意操作/读取时点等价达成「自动停止 ≤12h」与时长正确性（被触碰的 session 永不超 12h）；**残留**：永不再被操作的 RUNNING 会话不物化封顶条目（聚合查询不触发结算——数据缺失非数据错误，下次触碰补物化）；实时呈现属 UX 增强
- Successor Required: `yes`（运营要求无人值守结算/实时呈现时）

### 计费费率优先级 2-3 级（ErpCsAgentRate / 权益级费率）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §1.2 设计层级超出 UC-CS-11 验收标准（L1 无费率来源断言）；本计划仅全局默认级 `erp-cs.default-billing-rate`
- Successor Required: `no`

### 按组织配置强制全部审批（§3.1 第三触发）+ PENDING→DRAFT 退回路径（§3.2）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 二者超出 UC-CS-11 验收标准（L1 ⑥ 仅「可计费或超阈值触发审批」；REJECTED→修改重提已覆盖返工语义）
- Successor Required: `no`

### 可计费条目 → sales 开票跨域联动

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §5.2 独立跨域契约（按合同/按次/按工单三模式），超出 UC-CS-11 验收标准
- Successor Required: `yes`（计费模式裁决时）

### HR 考勤导出集成

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §5.3 显式「可选」
- Successor Required: `no`

## Closure

Status Note: 待执行完成后填写（Phase 1-3 全勾选 + 验证全绿 + 独立结束审计通过后置 completed）。

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计
- Evidence: 待补

Follow-up:

- 无（范围内零遗留预期；Deferred 三项已裁定分类）
