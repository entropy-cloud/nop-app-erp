# 2026-08-25-0330-1-aggregate-notify-cs-seeds-into-app-init-data 聚合 app 部署种子补齐 notify 模板族与 CS 编号规则 + owner-doc 真相修正

> Plan Status: completed
> Last Reviewed: 2026-08-25
> Source: 开放式审计 `docs/audits/2026-08-24-2233-open-audit-integration-test.md` P1 发现 OA-01
> Related: `docs/plans/2026-08-25-0232-1-v2-closure-alignment-docs-registration.md`（快照重录义务登记，本计划是该义务的首次大范围履行）
> Audit: required

## Current Baseline

- **模块级业务种子只存在于 deploy SQL，从未聚合进聚合 app**：`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql` 承载 `erp_sys_notification_template` 模板族 **27 行**（7101 `cs.sla-overdue` / 7102 `fin.posting-exception` / 7103 `sal.credit-over-limit` / 7104 `crm.event-reminder` / 7105 `cs.csat-reminder` / 7106 `mfg.production-variance` / 7111-7134 `wf.{pur-payment,sal-receipt,ast-disposal,hr-salary}` 审批结果/待办/抄送 12 行 / 7201 `log.draft-escalation` / 7202 `cs.ticket-created` / 7203 `cs.ticket-assign-no-match` 等）；`module-cs/deploy/sql/{三方言}/_seed_erp-cs.sql` 承载 `nop_sys_code_rule` `cs-ticket-code`（TK 编号规则，1 行）。全仓仅此两族 `_seed_*` deploy SQL。
- **聚合 app 唯一自动加载种子源不含它们**：`app-erp-all/src/main/resources/_vfs/_init-data/` = 94 CSV + `zz-sequence-advance.sql`，grep 零 `erp_sys_notification_template` / `nop_sys_code_rule` 命中；`DataInitInitializer` 只装载 `_init-data`（seed-data.md:71 自述）；deploy SQL 无任何自动消费路径（全仓引用检索零命中）。
- **后果（产品级种子契约漂移）**：聚合 app（产品交付物）fresh-DB 模式（E2E/演示/集成测试同源，`-Dnop.orm.init-database-data=true`）下通知子系统整体静默失活（模板驱动通知静默丢弃）+ CS 工单 TK 编号规则缺失回退。
- **owner-doc 根因误判在案**：`docs/design/integration-testing.md:458` 勘误(4) 记 C16 工单链 notify「均模板缺失静默降级不落库……为**未实现动作面**」——但 module-cs 派发实现与模板种子自 2026-08-18 起即存在，早 B8（08-24）六天；真实根因是「种子未聚合进 app 装配」。
- **同步义务未定义**：`docs/architecture/seed-data.md` 的批次构成史与 Non-Goals 全文未提 notify/cs 种子被排除在聚合种子外；「快照重录义务」节把种子资产口径固化为恰「94 CSV + 1 SQL」。
- **引用完整性门禁在位**：`TestErpSeedDataIntegrity`（app-erp-all）对 `_init-data/` 全量种子两层校验（全表可加载 + to-one 非空关联零悬空），新种子追加必须过此门禁（seed-data.md「后续 seed 追加义务」）。

## Goals

- notify 模板族（27 行）+ `cs-ticket-code` 编号规则（1 行）以 `_init-data/` 种子形式聚合落地，聚合 app fresh-DB 启动后通知子系统与 CS TK 编号在无手工导入下即工作。
- `integration-testing.md` §6 C16 勘误(4) 根因表述修正为真实根因（种子未聚合，已修复）。
- `seed-data.md` 增补「模块 deploy 种子 ↔ 聚合 `_init-data` 种子同步义务」强制规则，并同步资产口径（94 → 96 CSV）。
- 履行快照重录义务（seed-data.md §快照重录义务）：面 2（app-erp-all 集成用例）受影响快照全部重录全绿；面 1（各域 `_cases`）影响评估落盘；E2E 数值断言联动评估落盘。

## Non-Goals

- 不实现新的通知动作面/新模板类型（勘误(4) 中「知识库建议」等若确属未实现功能，仅在文档中如实标注为功能缺口，不在本计划实现）。
- 不改造 `DataInitInitializer` 或 deploy SQL 自动消费机制（平台机制不动，纯 app 层 `_init-data` 资产追加——zz-sequence-advance.sql 先例同型）。
- 不补 logistics/b2b/contract/drp/aps 等其他扩展域交易种子（1445-1 Deferred 既定策略不变）。
- 不处理 OA-02（closePeriod FX flush）/ OA-03（ASN orgId）——归本批计划 2/3。

## Task Route

- Type: `implementation-only change`（种子资产追加 + owner-doc 对齐，无 Java 生产代码变更）
- Owner Docs: `docs/architecture/seed-data.md`、`docs/design/integration-testing.md`、`docs/design/notify/notification-strategy.md`（模板族语义）、`docs/design/customer-service/`（TK 编号规则语义）
- Skill Selection Basis: 执行期核心是快照重录与三层全比对回归 → `nop-testing`；CSV 种子编制为数据资产工作，无匹配技能（`Skill: none`）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（H2 fresh-DB 由 `nop.orm.init-database-data=true` 门控触发；运行测试前 `lsof` 确认 8011/8080 无 live server——e2e-runbook 纪律）。

## Execution Plan

### Phase 1 - 种子聚合落盘（erp_sys_notification_template.csv + nop_sys_code_rule.csv）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/erp_sys_notification_template.csv`、`app-erp-all/src/main/resources/_vfs/_init-data/nop_sys_code_rule.csv`
Skill: none

- Item Types: `Add | Decision | Proof`
- Prereqs: 无

- [x] Add: 将 `_seed_erp-notify.sql`（mysql 方言为基准）27 行模板转制为 `erp_sys_notification_template.csv`（列头对齐 `DataInitInitializer.loadCsvData` 契约：表名.csv + ORM 全列；`CURRENT_TIMESTAMP` 固定为确定性字面值，对齐既有 94 CSV 冻结时钟口径 2026-07-17 或 platform 惯例 `system` 时间列——执行期对齐既有 CSV 先例后定格）
      - Skill: none
- [x] Add: 将 `_seed_erp-cs.sql` 的 `cs-ticket-code` 行转制为 `nop_sys_code_rule.csv`（String PK `SID` 列；`nop_auth_role.csv`（ROLE_ID=`采购员`）已证明平台表 String PK CSV 可经 `DataInitInitializer` 装载，主路径可行性有先例；若执行期仍遇障碍，回退为追加一个排序在 CSV 之后的 `.sql` 文件并记录原因）
      - Skill: none
- [x] Decision: 载体选型记录——CSV（与既有 94 CSV 同构、参与 `TestErpSeedDataIntegrity` 引用完整性门禁、拓扑序自动加载）vs 追加 `.sql`（绕过门禁与拓扑序）。选 CSV（先例已证可行，`.sql` 仅作障碍回退）；CSV 列头对齐既有约定（既有 CSV 省略审计列的即省略，不发明新列集）；三方言一致性核对结论（oracle/postgresql 方言 diff 应仅时序函数差异）写入执行日志。
      - Skill: none
- [x] Proof: `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity,TestAuthSeedLoadingProof` 全绿——新表纳入全表可加载枚举、to-one 非空关联零悬空（模板行若有 orgId/维度列则其引用必须合法，否则按白名单机制登记）。
      - Skill: nop-testing

Exit Criteria:

- [x] fresh-DB 启动后 `erp_sys_notification_template` 行数 = 27、`nop_sys_code_rule` 含 `cs-ticket-code` 行（经上述测试或 GraphQL findPage 抽样证明）——TestErpSeedDataIntegrity fresh-DB 留存 H2 实测 27 行 + cs-ticket-code-rule 行（CODE_PATTERN=TK{@year}{@month}{@csTicketMonthSeq:4}）
- [x] `TestErpSeedDataIntegrity` 全绿（本地化验证，后续阶段依赖）——2/2 全绿

### Phase 2 - 受影响面评估与快照重录（面 2 重录 + 面 1/E2E 评估）

Status: completed
Targets: `app-erp-all/_cases/io/nop/app/all/it/`（受影响用例目录）
Skill: nop-testing

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] Fix: 受影响面清单落盘（四档枚举，逐用例映射触发模板）——
  - **确定受影响（快照已重录，重录后 output/tables/erp_sys_notification.csv 实证）**：C01（`wf.pur-payment.task-assigned` ×2 → 用户 2=财务员 ROLE 解析；`result`/`cc` 未触发：end-listener 幂等守卫早退 + ROLE 财务经理无匹配角色；ID 漂移 +2）、C04（7202 `cs.ticket-created`→提单人 autotest-ref + 7204 `cs.knowledge-suggest-create`→处理人 cs-agent-01；ID 漂移 +2）、C16（7202→autotest-ref + 7204→it-c16-agent + 既有自包含 7109；**7101 SLA 预警实证未触发**——resolve 达标路径不进入 overdue 升级分支，符合草期「不得默认在列」预判）、C17（7114 `wf.hr-salary.result`→提单人 autotest-ref + 既有自包含 7109；7124/7134 未触发——ROLE「HR专员/财务主管/部门负责人」无种子匹配（种子角色名「HR 专员」带空格）且无 user_role 映射；ID 漂移 +1）；
  - **待实证归档为零触发（全绿实证）**：C03（receipt xwf 三通知全零落库——7122/7132 ROLE 经理·销售经理无匹配、7112 result 被 end-listener 幂等守卫早退（approve RPC 先置 APPROVED）；快照零影响）、C07（7106 差异 +2 < 阈值默认 100 不派发）、C05/C19（7201 派发方为 cron 门控 Job，用例无 DRAFT 滞留升级路径）、C10（7208/7209 用例零 downtime 触达）；
  - **明确不触发**：`wf.ast-disposal.*`（7113/7123/7133）——it-cases 零 `ErpAstDisposal` 触达（C11 无 xwf 链，grep 实证）。
  - **激活但无 it 用例消费方（逐枚举 grep + 全绿实证归档）**：7101（升级链/scan job 路径，C16 达标路径实证零触发）、7102（过账异常 Recorder 路径，it 过账全成功）、7103（信用超限 Checker 路径，it 额度内）、7104/7205/7207（job/履行链驱动零触达）、7203 `cs.ticket-assign-no-match`（生产派发路径存在，C04/C16 assign 均显式指定处理人/候选池空留 NEW，无 no-match 分支）、7206（履行失败路径零触达）。
  清单 + 每用例触发模板映射写入执行日志（docs/logs/2026/08-25.md）。
       - Skill: nop-testing
- [x] Fix: 受影响用例快照重录（RECORDING→CHECKING 往返），保持既有通配纪律（createTime/updateTime/sentAt 族 `*` 通配、金额/状态/借贷方向全字面值——B4-B10 先例）；通知行新增落 `output/tables` 变更行。——C01/C04/C16/C17 四用例 RECORDING 重录 + CHECKING 往返全绿；录制噪音治理：input/tables 时间戳刷新全部 git checkout 还原（mechanism (c) tableInit=false 下惰性）；C16 duration/startDateTime/endDateTime/sentAt/respondedAt（response + erp_cs_ticket.csv DURATION 列）与 C17 createTime/updateTime/sentAt 墙钟字段手工恢复 `*` 通配（录制器输出为字面量/@var: 别名）。
       - Skill: nop-testing
- [x] Fix: JUnit 层通知断言污染复核——C16/C04 等既有通知断言（如 `TestErpC16CsSlaNotification.java:191-193` `sent.get(0)` 取首条）可能被新激活模板发给同一测试用户的通知污染；逐处复核断言隔离性；修正触发条件 = 复跑发现 sent 列表混入非预期模板通知，届时修正断言选取逻辑（按 notificationType 过滤或锚定自包含模板 code）。——实证零污染：新通知接收人 = 提单人 `autotest-ref`（默认测试上下文用户）/角色用户 2/处理人 cs-agent-01·it-c16-agent，与 C16 `notificationsOf("it-c16-user")`/`countUnread(userId="it-c16-user")` 及 C17 同款自包含断言的接收人完全隔离；全绿复跑 54/0/0/1 无需改断言。
       - Skill: nop-testing
- [x] Proof: 面 1 评估——各域 `module-<domain>/erp-*-service/_cases/` 不消费 app `_init-data`（域级测试用模块自备 test seed），预期零影响；评估证据（装载机制差异）落日志。——grep 实证：`nop.orm.init-database-data=true` 全仓测试代码仅 app-erp-all 声明（域模块零命中）；域级快照走 input/tables tableInit 自备种子（如 module-cs `_cases` 自备 `nop_sys_code_rule.csv`），平台该开关默认 false。零影响确认。
       - Skill: nop-testing
- [x] Proof: E2E 数值断言联动评估（seed-data.md 义务规则 4）——检索 Playwright value spec 是否断言通知相关计数（如 countUnread/通知列表）；有则同步更新，无则落盘「零命中」证据。——唯一触达通知端点的 spec = `tests/e2e/business-actions/notify-inbox.action.spec.ts`：自包含模板（900000000+ 段 id + 独立 eventType + 接收人 nop）+ 全相对断言（`toBeGreaterThanOrEqual`/`toContain`/`beforeN-1`），无绝对计数基线断言；`cs-ticket.action.spec.ts` 显式提供 code（`E2E-TKT-{ts}`）不依赖 TK 编号规则 → 种子聚合对 E2E 零影响，无需更新任何 spec。
       - Skill: nop-testing

Exit Criteria:

- [x] `mvn test -pl app-erp-all` 全绿（54/0/0/1 口径，快照更新后；CS 工单编号若因 `cs-ticket-code` 激活而变化，C16 相关断言同步）——54/0/0/1 BUILD SUCCESS；C16/C04 工单均显式提供 code，TK 编号规则激活未改变用例内编号（规则仅对无 code 创建生效）

### Phase 3 - owner-doc 真相修正与同步义务立法

Status: completed
Targets: `docs/design/integration-testing.md`、`docs/architecture/seed-data.md`、`docs/backlog/integration-test-roadmap.md`、其他经 grep 实证的权威真相源计数处
Skill: none

- Item Types: `Fix | Add`
- Prereqs: Phase 2

- [x] Fix: `integration-testing.md:458` 勘误(4) 根因修正——「未实现动作面」改为「模块 deploy 种子未聚合进 app 装配（2026-08-25 已聚合修复，plan 本计划）」；C16 链通知行为若因聚合而变化（创建确认通知落库），同步修正勘误表述。——勘误(4) 划线改写为根因修正段（含 7202/7204 落库行为 + 7101 仍仅 overdue 路径 + 断言路径保持 C09 先例接收人隔离）；C17 勘误(3) 补注 7114 wf 结果通知已落库而 markPaid 结论不变。
       - Skill: none
- [x] Add: `seed-data.md` 新增「模块 deploy 种子 ↔ 聚合 `_init-data` 种子同步义务」节：任何模块新增/修改 `deploy/sql/_seed_*.sql` 时，必须同步聚合进 `_init-data` 或在本节显式登记 Non-Goal 裁决（含理由）；头部资产口径 94 → 96 CSV 更新；「快照重录义务」节资产口径同步。——新节含登记表（notify/cs 两行已聚合裁决 + 全仓无其他 `_seed_*` grep 实证）+ 联动义务（快照重录/完整性门禁/提交说明）；头部「通用引用完整性校验」段 + 「快照重录义务」节计数均更新 96。
       - Skill: none
- [x] Fix: 种子资产计数全仓权威真相源对齐——「94 CSV/94 csv/全量 94 seed/95 CSV」计数变体的实存位置（草期 grep 实证密集区 = `docs/design/integration-testing.md` 多处 + `docs/architecture/seed-data.md` + `docs/backlog/integration-test-roadmap.md`「框架/平台复用」节 + `docs/testing/e2e-runbook.md` 若有）逐一对齐为「96 CSV + 1 SQL」；注意 `docs/backlog/README.md:126` 的「92→94」是 E2E spec 套件计数，**不得**误改（实证未触碰）；既有 95-vs-94 口径不一致（seed-data.md:24「95 CSV 全覆盖」按文件数计）一并归一（→ 96 CSV 全覆盖）。历史日志/已归档计划中的历史性引用不改。
       - Skill: none
- [x] Add: 提交说明义务履行（seed-data.md 义务规则 3）——变更提交说明中登记重录范围（哪些 seed 文件变更 → 面 2 哪些用例快照重录 + 面 1/E2E 评估结论）。——重录范围全文登记于执行日志 `docs/logs/2026/08-25.md`（含提交说明可引用的变更清单：+2 seed CSV → C01/C04/C16/C17 重录 + 面 1 零影响 + E2E 零影响证据），待提交时引用。
       - Skill: none

Exit Criteria:

- [x] 种子计数变体（`94 CSV`/`94 csv`/`94 seed`/`95 CSV`/`94 张`）在权威真相源（docs/design/、docs/architecture/、docs/testing/、docs/backlog/ roadmap）grep 归零（历史日志/已归档计划除外）——2026-08-25 复跑 grep 零命中；96 计数 25 处就位

## Draft Review Record

- Independent draft review iteration 1: acceptable (task `ses_fca9b2d19ffepEINL5iVLkqTQd`, fresh session) — 全部 Current Baseline 事实断言活仓核验通过（27 模板行/1 code rule 行/95 文件零命中/勘误文本/seed-data.md 口径/TestErpSeedDataIntegrity 在位/C16 断言行号/`nop_auth_role.csv` String PK 先例）；OA-01 三项修复方向全覆盖；计划指南合规（无 slack 词、退出标准可观察、结束审计门控在位）。无阻塞项。采纳非阻塞注记：Phase 2 映射扩为四档（新增「激活但无 it 用例消费方」档，7203 有生产派发路径须实证）、C16 的 7101 触发须以 overdue 路径实证（不得默认在列）、断言污染修正触发条件显式化。已按注记修订，共识达成 → active。

## Closure Gates

- [x] 范围内行为完成（27+1 行种子聚合、fresh-DB 生效证明、owner-doc 修正）——2026-08-25 执行完毕：27 模板行 + 1 编号规则行聚合落 `_init-data`，TestErpSeedDataIntegrity fresh-DB 留存 H2 实测 27 行 + cs-ticket-code-rule 行，勘误(4)/同步义务节/计数对齐全部落地
- [x] 相关文档对齐（seed-data.md / integration-testing.md / runbook / baselines 计数一致）——96 CSV + 1 SQL 口径全仓权威真相源 25 处就位，变体 grep 归零（known-good-baselines 无陈旧计数，零触碰）
- [x] 已运行验证：`mvn test -pl app-erp-all`（54/0/0/1 全绿）+ `TestErpSeedDataIntegrity` + `bash docs/audits/nop-compliance-checker.sh`（对 compliance-baseline 零漂移）+ `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS——2026-08-25 全部实测：54/0/0/1 BUILD SUCCESS（快照重录后两次复跑一致）；checker 18 规则计数与 §BASELINE 机器可读块逐项相等；clean install 01:47 BUILD SUCCESS
- [x] 无范围内项目降级为 deferred/follow-up——Deferred But Adjudicated 节为空（无）
- [x] 独立草案审查已完成并记录——Draft Review Record iteration 1 acceptable（task ses_fca9b2d19ffepEINL5iVLkqTQd，fresh session）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致——三 Phase Status: completed + 全 item [x] + 本日志条目（docs/logs/2026/08-25.md）对齐
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——2026-08-25 独立结束审计 mission（fresh session，非执行者上下文）执行通过，活仓核验全部命中
- [x] 结束证据存在于文件中——Closure Audit Evidence 节已回填独立审计实证（见下）

## Deferred But Adjudicated

（无——本计划范围内无推迟项。）

## Closure

Status Note: closed（2026-08-25 三 Phase 全部完成 + 全量验证绿 + 独立结束审计通过；Plan Status → completed。）

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，mission-driver 2026-08-24-223318 closure audit step，非执行者会话、不重用执行者上下文）
- Evidence: 活仓核验（2026-08-25）——(1) `app-erp-all/src/main/resources/_vfs/_init-data/` 实测 96 CSV + 1 SQL（97 文件）；`erp_sys_notification_template.csv` 27 数据行（7101-7134/7201-7209 全 ID 就位）、`nop_sys_code_rule.csv` 含 `cs-ticket-code` 行。(2) 快照重录实证：C01/C04/C16/C17 `output/tables/erp_sys_notification.csv` 含新通知变更行（C01 `wf.pur-payment` ×2、C04 cs 族 ×2、C16 `cs.ticket-created`/`cs.knowledge-suggest-create` 各 2、C17 `wf.hr-salary.result` ×1）。(3) owner-doc 对齐实证：`docs/architecture/seed-data.md` 96 CSV 口径 + 「模块 deploy 种子 ↔ 聚合 `_init-data` 种子同步义务」节（含 notify/cs 登记表）在位；`docs/design/integration-testing.md:458` 勘误(4) 已改写为根因修正段（含 7202/7204 落库 + 7101 仅 overdue 路径）、C17 勘误(3) 补注 7114；权威真相源 `94 CSV`/`94 csv`/`94 seed`/`95 CSV`/`94 张` 变体 grep 零命中。(4) 日志：`docs/logs/2026/08-25.md` 含本计划条目（重录范围 + 面 1/E2E 零影响结论 + 验证状态）。

Follow-up:

- （无；已确认缺陷不得出现在此处）
