# 2026-08-17-2125-1-rc-mr1-r1-65-cs-ticket-create-enrichment RC-R1.65 — cs 工单创建自动富化族（P1-RC-054 B 类预授权：建议匹配 + 自动 deadline + 自动分配 + status 默认 + TK 编号 + 确认通知）

> Plan Status: completed
> Last Reviewed: 2026-08-18（closed）
> Mission: requirement-compliance
> Work Item: RC-R1.65（P1-RC-054，UC-CS-01 ②③④⑤⑥⑦⑧ 创建自动富化 6 项 + ⑧ 无匹配异常路径；A1.40 UC-CS-09 普通保存权益联动 reuse）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.65 行 + `docs/audits/arm-index.md` P1-RC-054 行（:228）+ 2026-08-12 批量裁决 B 类（roadmap 头 :49：「RC-R1.65（gen 规则生成 code + 跨域查询 team 成员）」降级预授权自动执行）
> Related: `docs/plans/2026-08-16-1634-1-rc-mr1-r1-57-crm-team-member-allocation.md`（ErpCrmTeamMember 实体 + ROUND_ROBIN/LOAD_BALANCED TeamMemberResolver 先例）；`docs/plans/2026-08-15-1605-1-rc-mr1-r1-37-38-logistics-job-wiring-family.md`（notify 模板种子 7201 先例）；`docs/audits/2026-08-08-0015-rc-ma4-a4-2-124-142-cs-f1-f2-f3-f4-runtime.md`（A4.2.124/125 死 flag + AMIS 无 matchAndAttachSla 串联运行时确认）
> Audit: required

## Current Baseline

- **finding P1-RC-054（arm-index:228，UC-CS-01 ②③④⑤⑥⑦⑧）**：L1（`docs/design/customer-service/use-cases.md:10-19`）逐字要求 ②「系统读取客户信息，建议匹配的 ticketType、slaPolicy」+ ③「系统自动计算 SLA 截止时间（deadlineDateTime = now + slaPolicy.resolveHours）」+ ④「系统根据 ticketType + team 匹配规则自动分配处理人（轮转/最少未结工单）」+ ⑤「工单状态 → NEW → ASSIGNED」+ ⑥「系统向客户发送工单确认通知（含工单编号 TK{YYYYMM}{SEQ4}）」+ ⑦「工单进入 ASSIGNED 状态，处理人待办列表出现新工单」+ ⑧「自动分派无匹配处理人 → 留 NEW 状态，升级通知客服主管人工分派」。L3 实仓（HEAD 核查）：
  - `ErpCsTicketBizModel.defaultPrepareSave:82-88` 仅 `super.defaultPrepareSave` + businessDate 兜底——**零富化逻辑**；`ErpCsTicket.xbiz` 无 save 时富化；
  - **②建议匹配**：`ErpCsTicketType.defaultSlaPolicyId`（`module-cs/model/app-erp-cs.orm.xml:231` propId 5）+ `defaultPriority`（:230 propId 4）schema 存在但 save 路径零读取；
  - **③自动 deadline**：`ErpCsTicketMatchAndAttachSlaProcessor.matchAndAttachSla:34-67` 经 `SlaPolicyMatcher.match`（静态）+ `SlaDeadlineCalculator.calculate`（静态）算 deadline，但**仅经独立 matchAndAttachSla mutation 可达非 save 自动触发**；同 Processor 内含权益匹配扣减（`:76-95`，单触发点设计）；
  - **④自动分配**：`assign:104-118` 取调用方 assignedToId 入参无分配算法；`erp-cs.auto-assign-on-create` config flag（`ErpCsConstants:55` + `ErpCsConfigs:26-28` 默认 true）**已声明但 main 源码零消费方——死 flag**（A4.2.124 运行时确认）；
  - **⑤status 默认**：`status` 列（orm.xml:165 propId 19 mandatory=true）无默认值，测试/E2E 均显式传 status='NEW'；
  - **⑥TK 编号**：`code` 列（orm.xml:148 propId 2 tagSet="var" 用户自填，UK_CS_TICKET_CODE_ORG(code,orgId)）+ grep `TK\{YYYYMM\}|generateCode|nextTicketCode` 跨 main 零命中（catalog 驱动 `ErpCsServiceCatalogItemCreateFromCatalogProcessor:192` 合成 `TK-<millis>` 毫秒格式属 UC-CS-10 非 UC-CS-01）；
  - **⑥⑧通知**：save 路径无 notify 调用（仅 SLA-overdue/CSAT-reminder/entitlement-expiry 三类事件 `ErpCsConstants:71/77/114`）；通知模板种子机制 = `module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`（现有 ID 7101-7134 + 7201，三方言对齐）；
  - **④候选池载体清单（零 cs 成员子实体）**：`ErpCsTeam`（orm.xml:301-327，仅 teamLeaderId propId 4 stdDomain=userId 无成员子表）；`ErpCsAgentRate`（orm.xml:512-540，agentId propId 3 **BIGINT** + serviceType + isActive + orgId）；crm `ErpCrmTeamMember`（RC-R1.57 已落地：teamId BIGINT→ErpCrmTeam + **userId VARCHAR(36) stdDomain=userId** + `IErpCrmTeamMemberBiz` 可跨域注入）；`ErpCsTicket.assignedToId`（orm.xml:157 propId 11 **VARCHAR(36) stdDomain=userId**）——**id 空间不一致**（cs 侧 agent/escalation 载体 BIGINT vs assignedToId/notify USER_LIST 需 userId 字符串），D3 须显式裁决；
  - **编号生成平台机制**：nop-sys CodeRule + Sequence（`../nop-entropy/docs-for-ai/03-runbooks/generate-business-code.md`）：方式 1 = orm 列 `tagSet="code"`（触 orm.xml 编辑）；方式 2 = **派生 xmeta `biz:codeRule`**（`module-cs/erp-cs-meta/src/main/resources/_vfs/erp/cs/model/ErpCsTicket/ErpCsTicket.xmeta` 派生文件已存在，零 orm.xml 编辑）+ `nop_sys_code_rule`/`nop_sys_sequence` 种子 SQL；**本仓尚无任何 nop_sys_code_rule/sequence 种子（首用，lazyInit `default` 序列兜底存在）**；
  - **算法先例**：R1.57 `TerritoryAssignmentEngine` + `TeamMemberResolver`（ROUND_ROBIN 轮转 / LOAD_BALANCED 最少活跃 + 降级 MANUAL degraded）可镜像。
- **Q4 判据**：§2 P1①（6 项自动富化全缺仅裸 CRUD）+ P1②（⑧ 无匹配异常路径零实现）。三判据复核均不成立（arm-index:228）→ Q4=(a) 强制实现。**2026-08-12 B 类批量裁决**（roadmap 头 :49，用户批准）：RC-R1.65 降级预授权自动执行——修复路径 = gen 规则生成 code + 跨域查询 team 成员，**不需要 ORM 结构变更，无越界项 checkbox**；**边界护栏**：本计划零 `module-cs/model/app-erp-cs.orm.xml` 编辑（编号经方式 2 派生 xmeta）；若执行期发现必须 ORM 结构变更（tagSet 改列/加实体/加列），视为超出 B 类授权，回落双独立子 agent 批准 + 独立 plan-audit。**真相源不一致披露**：roadmap RC-R1.65 **行标签**（:457）仍残留越界项 ask-first 旧措辞（「code 列 tagSet→seq 触 ORM + team 成员载体」），与 2026-08-12 头部 B 类裁决冲突——以头部裁决（后出且经 5 路独立 agent 核实 + 用户批准）为准，Phase 3 文档回填按 R1.63/R1.64 先例**改写行标签消除歧义**。
- **⑦ 待办列表映射**：UC-CS-01 ⑦「处理人待办列表出现新工单」= 分配富化成功后 assignedToId 落库 + status=ASSIGNED（既有按 status/assignedToId 的列表查询天然可见，orm 索引 IDX_CS_TICKET_ASSIGNED_TO_ID 就绪）——本计划经 ④ 分配行为达成，无独立工作项。
- **测试基线**：erp-cs-service **122 @Test 全绿**（RC-R1.28 记录 122 tests，2026-08-17 HEAD 递归实测 @Test 计数 122 一致；主要类：TestErpCsTicketSlaCsat / TestErpCsSlaNotification / TestErpCsServiceCatalog / TestErpCsEntitlement / TestErpCsKnowledgeBaseSearch / TestErpCsCannedResponseBiz / TestErpCsTicketTypeCrudSmoke + dashboard/entity/job/probe/report/statemachine 子包）。
- **compliance 基线**（`docs/audits/compliance-baseline.md` §BASELINE 机器可读块 :427-449）：R2b=235 / R2c=1434 / R2d=35 / R10=9 / R12a=70。

## Goals

- **UC-CS-01 ②-⑧ 创建路径运行时成立**：save 路径自动富化——缺省填充建议匹配（priority ← ticketType 默认；slaPolicy 经后置 matchAndAttachSla 挂载）+ 自动 deadline（复用 SlaPolicyMatcher/SlaDeadlineCalculator）+ 自动分配（轮转/最少未结 + config 死 flag 激活）+ status 缺省 NEW + 自动分配成功 → ASSIGNED + ASSIGN 审计 + TK{YYYYMM}{SEQ4} 编号 + 客户确认通知（notify 占位语义）+ ⑧ 无匹配 → 留 NEW + 客服主管升级通知。
- **UC-CS-09 reuse 闭合**：普通保存自动联动权益校验/扣减（save 时自动触发 matchAndAttachSla 单触发点，幂等守卫防重复扣减）。
- **config 死 flag 激活**：`erp-cs.auto-assign-on-create`（已存在，默认 true）由死 flag 转活跃消费点。
- **测试补强 + 零回归**：新增创建富化测试组（成功/无匹配/config 关闭/显式值不覆盖/catalog 路径共存）+ erp-cs-service 122 基线全绿 + 全量构建 + compliance checker 零漂移（或 baseline-raise 带 per-site 证据）。
- **owner doc 收敛**：README.md §关键业务规则 1-2 + sla.md §实现约定 auto-assign-on-create 注记 + arm-index P1-RC-054 → done (RC-R1.65) + roadmap 行 done + logs 条目。

## Non-Goals

- **不实现外部客户邮件/门户实际投递**（IN_APP notify 占位语义，实际发送属 nop-notification 独立面——对齐 csat.md/ScanOverdueTicketsProcessor:69 既有范式，successor）。
- **不做 AMIS 页面定制/表单向导**（富化字段经既有生成视图；操作按钮编排归产品化 successor）。
- **不实现技能矩阵/忙闲看板等分配增强**（L1 仅要求轮转/最少未结二算法）。
- **不含多级升级链（RC-R1.67）/计时器（RC-R1.66）/质量联动（RC-R1.68）**——同域不同 Work Item。
- **不改真相源契约段落**（use-cases L1 不动；owner doc 仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：BizModel save 路径富化 + 派生 xmeta codeRule + notify 接线 + config 激活；B 类预授权零 ORM 结构变更）
- Owner Docs: `docs/design/customer-service/use-cases.md`（L1 UC-CS-01/09）+ `docs/design/customer-service/README.md`（§关键业务规则 1-2）+ `docs/design/customer-service/sla.md`（§实现约定 auto-assign-on-create）
- Skill Selection Basis: BizModel 富化 + per-mutation 编排（`nop-backend-dev`：defaultPrepareSave/afterSave 钩子 + IBiz 注入 + config 门控）；编号生成（平台 CodeRule runbook `../nop-entropy/docs-for-ai/03-runbooks/generate-business-code.md` 方式 2）；测试（`nop-testing`：JunitBaseTestCase + GraphQL 引擎 + `_cases/` 快照）。

## Infrastructure And Config Prereqs

- **config**：`erp-cs.auto-assign-on-create`（已存在，默认 true，本计划激活消费点）；新增 `erp-cs.assign-method`（ROUND_ROBIN | LEAST_OPEN，默认 ROUND_ROBIN——Phase 1 D4 定稿）。
- **种子 SQL**：`nop_sys_code_rule` + `nop_sys_sequence` 种子行（编号规则 `TK{@year}{@month}{@seq:4}`；三方言 + 种子位置 Phase 1 D2 定稿，倾向 app-erp-all deploy 或 module-cs deploy 先例对齐）+ notify 模板种子行（`cs.ticket-created` / `cs.ticket-assign-no-match`，module-notify 三方言 `_seed_erp-notify.sql` ID 顺延现最大 7201 之后，Phase 1 D5 定稿）。
- **零 orm.xml 编辑**（B 类边界护栏）；**无外部服务/端口/密钥**。

## Execution Plan

### Phase 1 - 富化语义/编号载体/候选池/算法/通知裁决（Decision）

Status: completed
Targets: `docs/design/customer-service/README.md`（§关键业务规则注记草案）；本计划决策记录
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: none

- [x] `Decision` D1 富化语义与触发点：①缺省填充语义 = **fill-when-absent**（save 侧仅填充 **priority（← ticketType.defaultPriority）与 status=NEW**；**slaPolicyId 不在 save 侧填充**——策略挂载归属 matchAndAttachSla Processor 单一咽喉，避免前置填充使后置守卫失效；显式传入永不覆盖——零回归基线）；②②前半「建议匹配的 ticketType」裁决：ticketTypeId 为 mandatory 列（orm.xml:154）后端 fill-when-absent 结构性不可能，建议式交互属前端表单辅助（Non-Goal，successor）；③触发点裁决——选项 A（推荐）：save 成功后置（afterSave/doSave 钩子，RC-R1.8 totalHours afterEntityChange 先例）**仅当 slaPolicyId 与 deadlineDateTime 均为空时**自动调 `matchAndAttachSlaProcessor.matchAndAttachSla`（权益联动 UC-CS-09 reuse + deadline + policy 三合一）；**Processor 本体零改动**（入口不加守卫——既有「priority 变更重算 deadline」手动重匹配语义 `:64` 及其测试保持）；残留风险显式登记：手动重复调用 matchAndAttachSla 重复扣减 PAY_PER_TICKET 权益为**既有语义**（本计划不扩大不改坏，watch-only + successor 触发条件 = 权益扣减幂等化立项时）；**调用点守卫边界**：调用方显式传 slaPolicyId 而 deadline 为空时守卫跳过自动挂载（无自动 deadline/权益联动，手动 matchAndAttachSla mutation 仍可用——正常 L1 流程两者均不传，守卫正确触发）；选项 B：defaultPrepareSave 静态预算 deadline（不触权益——UC-CS-09 gap 残留，否决）；记录选择 + 事务边界（同 @BizMutation 事务内）。
      - Skill: `nop-backend-dev`
- [x] `Decision` D2 TK 编号载体：**方式 2 派生 xmeta `biz:codeRule`**（`ErpCsTicket/ErpCsTicket.xmeta` 增 `<prop name="code" biz:codeRule="cs-ticket-code"/>`——零 orm.xml 编辑对齐 B 类边界）+ `nop_sys_code_rule`（codePattern=`TK{@year}{@month}{@seq:4}`）+ `nop_sys_sequence` 种子 SQL 三方言；**SEQ4 按月重置语义裁决**：平台 Sequence `resetType` 不自动归零（runbook `generate-business-code.md:89` 明示 nextValue 单调递增），采用**按月 seqName 策略**（`cs_ticket_code_seq_{yyyyMM}` + `{@year}{@month}` 模式，runbook 认可的按月重置应用层实现）避免单序列 4 位回绕（>9999 截断回绕碰撞面 UK(code,orgId)）；autoExpr 仅在 code 为空时生成（CrudBizModel doSave 语义）→ **catalog `TK-<millis>` 路径共存**（显式 code 不覆盖）；否决方式 1（tagSet="code" 触 orm.xml 编辑超 B 类边界）；记录种子落点（module-cs deploy vs app-erp-all deploy 先例对齐）与首用风险（lazyInit `default` 序列兜底验证）。
      - Skill: `nop-backend-dev`
- [x] `Decision` D3 自动分配候选池载体（**含 Explore 前置**）：先核实 id 空间（现有测试/种子里 ErpCsAgentRate.agentId 与 ErpCsTimeEntry.agentId 的 BIGINT 指向 vs assignedToId/notify USER_LIST 的 userId VARCHAR(36) 指向），再裁决——选项 A（B 类裁决字面「跨域查询 team 成员」，推荐）：候选池 = 经 `IErpCrmTeamMemberBiz` 跨域查询 crm `ErpCrmTeamMember`（userId VARCHAR(36) 直接可赋 assignedToId；**须新增 cs→crm 依赖**：`module-cs/erp-cs-service/pom.xml` 加 erp-crm-dao compile + erp-crm-service test-scope，镜像既有 master-data/notify 依赖模式），团队关联约定 = SLA 策略 teamId → ErpCsTeam，**按 code 相等约定映射同码 crm 团队取成员**（约定风险显式登记：无同码 crm 团队 → 池空 → ⑧ 异常路径）；选项 B：cs 原生 `ErpCsAgentRate`（isActive=true 去重 agentId 为池——须解决 BIGINT→userId 映射，无映射则不可行）；选项 C：`ErpCsTeam.teamLeaderId` 单人直派（算法退化为单人，L1 轮转/最少未结字面弱满足）。记录选择 + 约定 + 残留风险。
      - Skill: `nop-backend-dev`
- [x] `Decision` D4 分配算法：镜像 R1.57 `TeamMemberResolver` 范式——ROUND_ROBIN（上次分配的下一个，无历史首位）+ LEAST_OPEN（活跃工单计数 docStatus 非终态/状态 IN (ASSIGNED,IN_PROGRESS) 最少者，平手取 id 升序首个）+ `erp-cs.assign-method` config（默认 ROUND_ROBIN）+ config off / 池空 / 解析失败 → 留 NEW 降级（⑧ 异常路径）；分配成功 → ASSIGNED + writeAction(ASSIGN) 审计（镜像 `assign:104-118` 语义）。
      - Skill: `nop-backend-dev`
- [x] `Decision` D5 通知事件与模板：`cs.ticket-created`（确认通知，USER_LIST `${submitterUserId}` 提单人插值——客户为 ErpMdPartner 非系统用户，IN_APP 占位语义对齐 csat.md「实际邮件发送归 nop-notification 独立面」范式 + 显式 successor 登记）+ `cs.ticket-assign-no-match`（⑧ 升级通知，ROLE `["客服主管"]` 对齐 cs.sla-overdue 7101 先例）；种子行 ID 顺延（现最大 7201，三方言一致）；notify 失败静默降级不阻断创建主流程（镜像 `notifySlaOverdue:313-329` try/catch 范式）。
      - Skill: `nop-backend-dev`
- [x] `Decision` D6 守卫与边界：status 缺省 NEW（mandatory 列运行时兜底）；富化仅新建路径（id==null）不触发更新；`erp-cs.auto-assign-on-create=false` → 跳过分配富化（建议匹配/deadline/编号仍执行——config 仅门控分配维度，语义对齐 owner doc flag 命名）；TK 编号 UK(code,orgId) 冲突语义（序列原子性由 nop-sys Sequence 行锁保证）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] D1-D6 各记录选择 + 替代方案 + 残留风险（写入本计划决策记录节或 owner doc 注记草案）
- [x] D2/D5 种子 SQL 草案 well-formed（`xmllint --noout` 或 SQL 方言核对）

### Phase 2 - 创建富化实现（Add）

Status: completed
Targets: `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsTicketBizModel.java`（save 路径富化 + 分配解析器接线）；`module-cs/erp-cs-service/.../entity/TicketAssignResolver.java`（新建，或 service 包合适位置）；`module-cs/erp-cs-meta/src/main/resources/_vfs/erp/cs/model/ErpCsTicket/ErpCsTicket.xmeta`（biz:codeRule）；`ErpCsConstants.java`/`ErpCsConfigs.java`（config 键 + reader）；`module-cs/erp-cs-service/pom.xml`（D3 选项 A 时 cs→crm 依赖：erp-crm-dao compile + erp-crm-service test）；`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`（模板行）；nop_sys_code_rule/sequence 种子
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add` 派生 xmeta `biz:codeRule="cs-ticket-code"` + nop_sys_code_rule/nop_sys_sequence 种子 SQL（D2 定稿）——验证：GraphQL save 不传 code 时落库 `TK{YYYYMM}{SEQ4}` 格式（执行期修正：模块级派生 xmeta 不经 _delta 层时 DefaultMetaPostExtends 不执行，biz:codeRule 不自动展开——显式声明 autoExpr when="save" 标准生成体；nop_sys_sequence 不做静态月行种子由变量懒建，per D2 裁决）
      - Skill: `nop-backend-dev`
- [x] `Add` `TicketAssignResolver`（D3/D4 定稿）：候选池解析 + ROUND_ROBIN/LEAST_OPEN 算法 + 空池/失败降级 null；`ErpCsConfigs` 增 `getAssignMethod()` reader + `ErpCsConstants` 增常量（执行期修正：cs→crm 依赖改道——crm-service test 拉入全量 beans 需 sal/inventory/quality/finance 闭包，改为 erp-crm-dao compile + erp-crm-meta test + app-test-mock-crm.beans.xml 测试 mock）
      - Skill: `nop-backend-dev`
- [x] `Add` save 路径富化接线（D1/D6 定稿）：缺省填充（status=NEW + priority ← ticketType 默认）+ 后置自动 matchAndAttachSla（**调用点守卫：slaPolicyId 与 deadlineDateTime 均为空才触发**，Processor 本体零改动）+ auto-assign（config 门控 → 成功 ASSIGNED + ASSIGN 审计 / 无匹配留 NEW + 主管升级通知）+ 客户确认通知派发（try/catch 降级）；含 cs→crm 依赖接线（D3 定稿时）（执行期修正：doSaveEntity 增 dao().flushSession()——OrmEntityDao.updateEntity 仅接受 MANAGED 态，同事务新建实体未 flush 被拒）
      - Skill: `nop-backend-dev`
- [x] `Add` notify 模板种子行（D5 定稿：cs.ticket-created + cs.ticket-assign-no-match，三方言）
      - Skill: none

Exit Criteria:

- [x] `mvn compile -pl module-cs/erp-cs-service -am` 通过；富化路径本地化类型检查零错误
- [x] 不传 code 的 save 生成 TK 格式编号；显式 code 不被覆盖（TestErpCsTicketCreateEnrichment ③ 断言 TK\d{10} 格式 + 0001→0002 按月递增 + 显式 code 共存）

### Phase 3 - 测试 + 文档回填（Proof）

Status: completed
Targets: `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsTicketCreateEnrichment.java`（新建）；`docs/design/customer-service/README.md` + `sla.md`（实现注记）；`docs/audits/arm-index.md`（P1-RC-054 行）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.65 行）；`docs/logs/2026/08-{17,18}.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] `Proof` `TestErpCsTicketCreateEnrichment` 测试组：①缺省填充（不传 priority/status → ticketType 默认 priority + NEW；slaPolicyId 经后置自动挂载断言）+ 显式值不覆盖；②自动 deadline（policy resolveHours 断言）+ 调用点守卫（slaPolicyId/deadline 已设不再自动触发——不重复扣减权益，UC-CS-09 reuse 断言）+ 手动 matchAndAttachSla 重匹配（priority 变更重算 deadline）既有语义共存；③TK 编号格式 `TK{YYYYMM}{SEQN}` + 按月序列 + 显式 code/catalog TK-millis（:192）共存；④自动分配成功 → ASSIGNED + ASSIGN 审计 + 确认通知落库（recipient=submitter）+ **⑦ 待办可见断言**（按 assignedToId+status=ASSIGNED 查询命中新工单）；⑤无匹配池 → 留 NEW + 升级通知落库（ROLE 客服主管路径或模板缺失静默断言）；⑥config off → 跳过分配富化（建议匹配/deadline/编号仍执行）；⑦分配算法单元断言（ROUND_ROBIN 轮转 + LEAST_OPEN 最少未结，mock 池）；⑧`_cases/` 快照录制（对齐既有范式）
      - Skill: `nop-testing`
- [x] `Proof` 零回归验证：`mvn test -pl module-cs/erp-cs-service` 全绿（130 = 122 基线 + 8 新增零回归）+ 既有 TicketSlaCsat/ServiceCatalog 快照核验（富化不破坏显式传值路径——dao 直插种子带显式 code/status 不触 autoExpr/fill 路径）
      - Skill: `nop-testing`
- [x] `Add` 文档回填：README.md §关键业务规则 1-2 补实现注记（富化语义 + config 激活）+ sla.md §实现约定 auto-assign-on-create 由死 flag 转活跃注记 + D1-D6 裁决引用；arm-index P1-RC-054 → done (RC-R1.65)；**roadmap RC-R1.65 行标签按 R1.63/R1.64 先例改写为 B 类预授权措辞** + 行 done ✅；logs 条目
      - Skill: none

Exit Criteria:

- [x] 测试组 ①-⑧ 全绿（指定成功/失败模式：无匹配留 NEW 断言 + config off 回归断言 + 调用点守卫断言逐项）
- [x] erp-cs-service 模块测试全绿（122 基线零回归，失败模式=任何既有测试翻红——实测 130 全绿零翻红）

 （全仓 `mvn clean install -DskipTests` + compliance checker 属 Closure Gates，不在阶段退出重复——见执行时规则 7。）

## Phase 1 Decision Records（执行时裁决记录，2026-08-18）

### D1 富化语义与触发点 — 选择：选项 A（save 后置，Processor 本体零改动）

- **fill-when-absent 语义**：`defaultPrepareSave` 仅填充 **status=NEW** 与 **priority ← ticketType.defaultPriority**（经 ORM 关系 getter `ticket.getTicketType().getDefaultPriority()`，显式传入永不覆盖——validated map 显式含该 prop 时 copyToEntity 已写入实体，null 判定天然跳过）。**slaPolicyId 不在 save 侧填充**——策略挂载归属 `matchAndAttachSla` Processor 单一咽喉（前置填充会使后置守卫 `:59` 失效）。
- **②「建议匹配的 ticketType」**：ticketTypeId 为 mandatory 列（orm.xml:154），后端 fill-when-absent 结构性不可能；建议式交互属前端表单辅助（Non-Goal，successor）。
- **触发点（选项 A）**：覆写 `doSaveEntity(EntityData, context)` 钩子（RC-R1.8 afterEntityChange 先例族，且可经 `entityData.isRecoverDeleted()` 精确区分新建 vs 逻辑删除恢复），`super.doSaveEntity` 成功后仅对**新建路径**执行三段富化：①**调用点守卫**——`slaPolicyId == null && deadlineDateTime == null` 时自动调 `matchAndAttachSlaProcessor.matchAndAttachSla(ticketId, context)`（policy 挂载 + deadline 计算 + 权益匹配扣减 UC-CS-09 reuse 三合一；Processor 本体零改动，「priority 变更重算 deadline」手动重匹配语义 `:64` 保持）；②自动分配（见 D3/D4）；③客户确认通知（见 D5）。显式传 slaPolicyId 而 deadline 为空 → 守卫跳过（无自动挂载，手动 mutation 仍可用）。
- **事务边界**：同 `@BizMutation` 事务内（save 管道自身事务），无独立传播控制；通知派发 try/catch 静默降级不阻断创建主流程（镜像 `notifySlaOverdue:313-329`）。
- **替代方案（选项 B）否决**：defaultPrepareSave 静态预算 deadline——不触权益，UC-CS-09 gap 残留。
- **残留风险（watch-only + successor）**：手动重复调用 matchAndAttachSla 重复扣减 PAY_PER_TICKET 权益为既有语义（本计划不扩大不改坏；successor = 权益扣减幂等化立项时）。

### D2 TK 编号载体 — 选择：方式 2 派生 xmeta `biz:codeRule` + 自定义 CodeRuleVariable 按月序列

- **载体**：`ErpCsTicket/ErpCsTicket.xmeta` 增 `<prop name="code" biz:codeRule="cs-ticket-code"/>`（零 orm.xml 编辑，对齐 B 类边界；autoExpr `when="save"` 由 meta-gen `GenCodeRuleAutoExpr` 自动生成——`meta-gen.xlib:96-122`）。**autoExpr fill-when-absent 语义**（`OrmEntityCopier:159-163` 显式提交 prop 加入 ignoreAutoExprProps 跳过生成）→ **catalog `TK-<millis>` 路径共存**（`:192` 显式 code 不覆盖）；update 路径 autoExpr 不触发（when="save"）。
- **codePattern**：`TK{@year}{@month}{@csTicketMonthSeq:4}`。
- **按月 seqName 实现**：`SysCodeRuleGenerator.generate` 的 seqName 为规则行静态字段（`:63`），平台不支持动态 seqName → 注册自定义模式变量 bean `nopCodeRuleVariable_csTicketMonthSeq`（runbook「注册自定义模式变量」机制，`DefaultCodeRule.variables` 经 `ioc:collect-beans` 按前缀收集，key=前缀后缀）。变量实现：seqName = `cs_ticket_code_seq_{yyyyMM}`（`{@year}{@month}` 从 params.getNow() 派生）→ **懒建月行**（REQUIRES_NEW 独立事务查插，镜像 `SysSequenceGenerator.runLocal:288-294`——必须先于首次 generateLong 提交，否则 `findSeqItem:262-270` 落 uuid 随机项）→ `sequenceGenerator.generateLong(seqName, false)` → %04d 左补零/右截断回绕（镜像 `generateSeq:71-84`）。
- **种子落点**：`module-cs/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-cs.sql`（新建种子文件，镜像 `module-notify/deploy/sql/.../_seed_erp-notify.sql` 先例；不触 codegen 管理的 `_create_*` 文件）。`nop_sys_code_rule` 行：name=`cs-ticket-code`、codePattern 如上、**seqName=`default`（非消费占位**——pattern 不含 `{@seq}`，SysCodeRuleGenerator 仅非空校验 `:64-66`；`default` 行由平台 lazyInit 真实存在兜底）。
- **nop_sys_sequence 不做静态月行种子**：月行随时间漂移（静态种子次月即失效），由变量首用懒建；stepSize=1、cacheSize=0（连续号语义，DB 行锁原子性保证 UK(code,orgId) 无并发重号）。
- **首用风险验证**：懒建先于取号提交 → findSeqItem 主缓存命中真实行，uuid 随机回退路径结构性不可达（测试 ③ 断言编号格式 + 连续性）。
- **否决方式 1**：tagSet="code" 触 orm.xml 编辑，超 B 类边界。
- **否决单序列 + `{@seq:4}`**：runbook `:89` 明示 resetType 不自动归零，单序列 4 位回绕在单月 >10^4 票时碰撞 UK(code,orgId)（Current Baseline 已披露）。

### D3 自动分配候选池载体 — 选择：选项 A（跨域查询 crm ErpCrmTeamMember）

- **id 空间核实（Explore 前置）**：`ErpCrmTeamMember.userId` VARCHAR(36) stdDomain=userId（`_gen/_ErpCrmTeamMember.java:33-34`）与 `ErpCsTicket.assignedToId` VARCHAR(36) stdDomain=userId（orm.xml:157）**同域同型直接可赋**；cs 侧 BIGINT 载体（`ErpCsAgentRate.agentId` orm.xml:512+ / `ErpCsSlaPolicy.escalationUserId` orm.xml:271）无 BIGINT→userId 映射（选项 B 不可行实证）。
- **依赖接线**：`module-cs/erp-cs-service/pom.xml` 加 `app-erp-crm-dao` compile + `app-erp-crm-service` test（镜像既有 master-data/notify 依赖模式 :43-56/:129-143；cs→crm 单向 R，DAG 无环；app-erp-all 经 crm-web→crm-service 传递提供运行时实现）。
- **团队关联约定**：teamId 解析链 = `ticket.slaPolicy.teamId`（显式挂载/catalog 路径）→ 回退 `ticket.ticketType.defaultSlaPolicy.teamId`（**实际主链**——`SlaPolicyMatcher.match:40` 仅匹配 `teamId IS NULL` 策略，自动挂载路径的策略必无 teamId；全部经 ORM to-one 关系 getter `slaPolicy`/`ticketType`/`defaultSlaPolicy`/`team` 零 daoFor）；得 `ErpCsTeam` 后**按 code 相等约定映射同码 crm 团队**（`IErpCrmTeamBiz.findFirst(eq(code))`）→ `IErpCrmTeamMemberBiz.findList(eq(teamId=crmTeam.id), id asc)` → userId 候选池。
- **约定风险（显式登记）**：无同码 crm 团队 / 无成员行 → 池空 → ⑧ 异常路径（留 NEW + 升级通知）；跨域同码约定属 B 类裁决字面落地，语义注记回填 README.md。
- **选项 B 否决**：BIGINT→userId 无映射载体；**选项 C 否决**：teamLeaderId 单人直派，轮转/最少未结算法退化（L1 字面弱满足）。

### D4 分配算法 — 选择：镜像 R1.57 TeamMemberResolver 范式（ROUND_ROBIN / LEAST_OPEN + config）

- **载体**：`TicketAssignResolver`（bean，cs-service entity 包）——`resolveCandidatePool(policy-chain, context)`（D3 跨域解析）+ `pickAssignee(method, members, lastAssigned, openCounts)`（纯函数算法，mock 池可单测）+ 空池/失败降级 null。工单历史查询（上次分配 / 未结计数）经 BizModel 自有 `findList`（镜像 `findSlaWarnings` 既有模式，零 daoFor 零自注入环）。
- **ROUND_ROBIN**：上次分配（候选池成员内最近一张 assignedToId 非空工单，createTime desc limit 1）的下一个（循环）；无历史首位。
- **LEAST_OPEN**：候选成员活跃工单计数（status IN (ASSIGNED, IN_PROGRESS)）最少者；平手取成员列表序（成员行 id 升序）首个。
- **config**：`erp-cs.assign-method`（ROUND_ROBIN | LEAST_OPEN，默认 ROUND_ROBIN；`ErpCsConfigs.getAssignMethod()`）。
- **降级矩阵**：`erp-cs.auto-assign-on-create=false` → 跳过分配维度（建议匹配/deadline/编号仍执行，D6）；池空 / 解析失败 → 留 NEW + ⑧ 升级通知。**分配成功** → status=ASSIGNED（`stateMachine.assignTargetStatus()`）+ `writeAction(ASSIGN, NEW, ASSIGNED)` 审计（镜像 `assign:104-118` 语义）；守卫：仅 `status==NEW` 且 `assignedToId==null` 时自动分配（显式传入不覆盖）。

### D5 通知事件与模板 — 选择：ID 7202/7203 顺延，三方言对齐

- **`cs.ticket-created`（7202，确认通知）**：USER_LIST `{"userIds":["${submitterUserId}"]}`——提单人 = `ticket.createdBy`（客户为 ErpMdPartner 非系统用户，IN_APP 占位语义对齐 csat.md「实际邮件发送归 nop-notification 独立面」范式 + successor 登记）；上下文：ticketId/ticketCode/customerName/submitterUserId（含 TK 编号，⑥ 达成）。
- **`cs.ticket-assign-no-match`（7203，⑧ 升级通知）**：ROLE `{"roles":["客服主管"]}`（对齐 cs.sla-overdue 7101 先例）；上下文：ticketId/ticketCode/customerName。
- **种子**：`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql` ID 顺延现最大 7201 → 7202/7203（三方言一致，MERGE 窗口 300s/ACTIVE，镜像 7201 行式）。
- **失败语义**：notify try/catch 静默降级不阻断创建主流程（镜像 `notifySlaOverdue:313-329`）；模板缺失 → notify 统一入口 config-gated 静默跳过（既有 dispatcher 行为）。

### D6 守卫与边界

- **status 缺省 NEW**：mandatory 列运行时兜底（fill-when-absent，显式 status 不覆盖）；⑤ NEW→ASSIGNED 仅在自动分配成功时发生（分配失败/关闭留 NEW，⑧ 语义）。
- **富化仅新建**：`doSaveEntity` 钩子 + `!entityData.isRecoverDeleted()`（save 管道语义：带活跃 id 抛 ERR_BIZ_ENTITY_ALREADY_EXISTS、删除恢复走 recover 标志、无 id 为新建——`CrudBizModel:668-688`）；update/copy-for-new 外路径零触发。
- **`erp-cs.auto-assign-on-create=false`**：跳过分配维度（建议匹配/deadline/编号仍执行——config 仅门控分配维度，语义对齐 flag 命名）。
- **TK UK(code,orgId) 冲突语义**：序列原子性由 nop-sys Sequence DB 行锁（REQUIRES_NEW 取号）保证；同月 >10^4 票回绕面经按月 seqName 消除（D2）。

### 种子 SQL 草案（well-formed 核对：mysql/oracle/postgresql 三方言语法与既有种子文件一致）

`module-cs/deploy/sql/{dialect}/_seed_erp-cs.sql`（新建，三方言同构）：

```sql
-- 客服域种子（RC-R1.65，P1-RC-054，UC-CS-01 ⑥）：工单 TK 编号规则。
-- TK{@year}{@month}{@csTicketMonthSeq:4}：csTicketMonthSeq = 自定义 CodeRuleVariable
--（nopCodeRuleVariable_csTicketMonthSeq），按月懒建序列行 cs_ticket_code_seq_{yyyyMM}（stepSize=1/cacheSize=0），
-- 避免 {@seq:4} 单序列 4 位回绕（platform Sequence resetType 不自动归零）。
-- SEQ_NAME='default' 为非消费占位：pattern 不含 {@seq}，SysCodeRuleGenerator 仅作非空校验。
INSERT INTO nop_sys_code_rule
  (SID, NAME, DISPLAY_NAME, CODE_PATTERN, SEQ_NAME, DEL_FLAG, VERSION,
   CREATED_BY, CREATE_TIME, UPDATED_BY, UPDATE_TIME, REMARK)
VALUES
  ('cs-ticket-code-rule', 'cs-ticket-code', '客服工单TK编号规则',
   'TK{@year}{@month}{@csTicketMonthSeq:4}', 'default', 0, 0,
   'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP,
   'RC-R1.65 UC-CS-01 ⑥ TK{YYYYMM}{SEQ4} 按月序列编号');
```

`module-notify/deploy/sql/{dialect}/_seed_erp-notify.sql`（追加 7202/7203，镜像 7201 行式）：

```sql
  -- 业务提醒：工单创建确认通知（RC-R1.65，P1-RC-054，UC-CS-01 ⑥），5 分钟窗口合并。
  -- 接收人=提单人 createdBy（USER_LIST ${submitterUserId} 从 notify context 插值；客户邮件/门户实际投递归 nop-notification successor）。
  (7202, 'cs.ticket-created', '工单创建确认通知', 'IN_APP',
   '工单已创建: ${ticketCode}',
   '您的工单 ${ticketCode} 已创建（ID ${ticketId}），客户 ${customerName}，将按 SLA 及时处理',
   'USER_LIST', '{"userIds":["${submitterUserId}"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '业务提醒样例（工单创建确认，submitterUserId=提单人 createdBy）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
  -- 异常告警：自动分派无匹配处理人升级（RC-R1.65，P1-RC-054，UC-CS-01 ⑧），5 分钟窗口合并。
  (7203, 'cs.ticket-assign-no-match', '工单分派无匹配升级', 'IN_APP',
   '工单分派无匹配: ${ticketCode}',
   '工单 ${ticketCode}（ID ${ticketId}，客户 ${customerName}）自动分派无匹配处理人，请人工分派',
   'ROLE', '{"roles":["客服主管"]}', 300, 'MERGE_BY_USER_TYPE', 'ACTIVE',
   '异常告警样例（自动分派无匹配 → 客服主管人工分派）', 0, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);
```

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_feec138f8ffe9qQxcAbu6h1xz3`) — 2 MAJOR + 6 MINOR。M1 已修订：Current Baseline 增补 roadmap 头 :49 B 类裁决与行 :457 残留越界项标签的**真相源不一致披露**（以头部裁决为准）+ Phase 3 回填项增「行标签按 R1.63/R1.64 先例改写为 B 类措辞」；M2 已修订：D1 重构——save 侧仅填 priority/status（slaPolicyId 归属 Processor 单一咽喉），幂等守卫改为**调用点守卫**（slaPolicyId 与 deadlineDateTime 均为空才自动触发），Processor 本体零改动保住「priority 变更重算」既有手动重匹配语义（`:64`），手动重复扣减权益登记为既有语义 watch-only Deferred；m3 已修订：Phase 2 Targets + D3 选项 A 增 cs→crm pom 依赖（erp-crm-dao compile + erp-crm-service test）；m4 已修订：D2 增 SEQ4 按月重置裁决（按月 seqName `cs_ticket_code_seq_{yyyyMM}` 策略，runbook :89 resetType 不自动归零）；m5 已修订：D1 增 ②「建议匹配 ticketType」裁决（mandatory 列 → 前端辅助 successor Non-Goal）；m6 已修订：baseline 增 ⑦ 待办列表显式映射 + 测试 ④ 增 assignedToId+ASSIGNED 查询断言；m7 已修订：catalog TK-millis 行号 :132→:192（HEAD 核实）；m8 已修订：Phase 3 全仓构建/checker 移出阶段退出（归 Closure Gates，执行时规则 7）。
- Independent draft review iteration 2: `needs revision` (`ses_feba2bc4ffe4Pj6lHYYRji5xe`) — 迭代 1 全部 8 项修复逐项核实 FIXED（M1 披露+改写项 / M2 D1 三角一致 / m3-m8 逐项，含 roadmap :49 vs :457、TK-millis :192、SlaPolicyMatcher 行为、122 @Test、compliance 基线复核），仅余 1 MINOR：Goals :31 残留旧机制措辞「priority/slaPolicyId ← ticketType 默认」与修订后 D1 矛盾（单行修复，审查者声明修复后可 summary 级复核）。起草者已就地修复：Goals 改为「priority ← ticketType 默认；slaPolicy 经后置 matchAndAttachSla 挂载」+ D1 补调用点守卫边界注记。
- Independent draft review iteration 3（summary 级）: `accept`（`ses_feeb6f8ddffeSEhNnPCbbkkKxy`）——Goals 措辞与 D1/Phase 2/测试①② 一致 + 守卫边界注记逻辑自洽 + 无附带损伤 → 草案审查收敛，`Plan Status: draft → active`。

## Closure Gates

- [x] 范围内行为完成（UC-CS-01 ②-⑧ + UC-CS-09 reuse 富化落地）
- [x] 相关文档对齐（README.md/sla.md/arm-index/roadmap/logs）
- [x] 已运行验证（`mvn test -pl module-cs/erp-cs-service` 130/0/0 + 全仓 `mvn clean install -DskipTests` BUILD SUCCESS + 全仓 `mvn test` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` exit 0——R10 9→10 baseline-raise 带 per-site 证据登记 compliance-baseline.md，其余 18 规则零漂移）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred 两项均经裁定分类，非范围内降级）
- [x] 独立草案审查已完成并记录（3 轮迭代见 Draft Review Record）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（独立结束审计复核）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（见 Closure Audit Evidence）

## Deferred But Adjudicated

### 手动重复 matchAndAttachSla 重复扣减 PAY_PER_TICKET 权益（既有语义暴露面）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 手动 mutation 重复调用重复扣减为**既有语义**（Processor :76-95 单触发点设计假设单次调用，本计划不扩大不改坏）；save 自动挂载经调用点守卫（slaPolicyId/deadline 均空才触发）不引入新暴露
- Successor Required: `yes`（权益扣减幂等化立项时）

### 外部客户邮件/门户实际投递

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 客户（ErpMdPartner）非系统用户，notify IN_APP 占位语义为仓内既定范式（csat.md「实际邮件发送归 nop-notification 独立面」）；L1 ⑥ 通知内容与编号已在占位语义下达成
- Successor Required: `yes`（nop-notification 独立面接入时）

### 技能矩阵/忙闲感知分配

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 ④ 仅要求轮转/最少未结二算法
- Successor Required: `no`

## Closure

Status Note: Phase 1-3 全勾选 + 验证全绿（erp-cs-service 130/0/0 零回归 + 全仓 install/test BUILD SUCCESS + checker R10 baseline-raise 带 per-site 证据其余零漂移）+ 独立结束审计 PASS——completed。执行期修正三处已就地记录（Phase 2 条目 + 日志）：① 派生 xmeta 显式 autoExpr（模块级派生文件不经 _delta 层时 GenCodeRuleAutoExpr 不执行）；② cs→crm 测试依赖改道（crm-service 全量 beans 需 sal/inv/quality/finance 闭包，改 crm-dao compile + crm-meta test + CrudBizModel mock）；③ doSaveEntity 增 flushSession（NEW→MANAGED）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，task `ses_fedb38a41ffec6qnPtlbxKMynj`，2026-08-18）
- Evidence: VERDICT **PASS**（0 MAJOR / 0 MINOR / 3 INFO）——①计划一致性（Phase 1-3 + Exit Criteria 全 [x]，[ ] 仅剩 Closure Gates 待审计）②实现 file:line 逐条核验（BizModel 富化链/Resolver 纯函数/月序列变量/派生 xmeta autoExpr/三方言种子×2/beans 注册/configs/pom crm-dao+crm-meta 零 crm-service）③测试 ①-⑧ 组覆盖核验 + 8 空 autotest.yaml 标记（R1.37 断言式先例核实）④独立复跑 `mvn test -pl module-cs/erp-cs-service` 130/0/0 BUILD SUCCESS + checker exit 0（R2b=235/R2c=1434/R2d=35/R10=10/R12a=70 与基线一致）⑤文档回填五处核验（arm-index done 行/roadmap B 类改写 done ✅/README §1-2 注记/sla.md 激活注记/08-18 日志）⑥反模式零命中（@Inject 非 private/IServiceContext 末参/CoreMetrics/NopException/零生成文件编辑）⑦边界护栏（module-cs/model + module-crm/model 零 ORM 编辑，B 类预授权遵守）。3 INFO：08-17 无日志条目（全执行在 08-18，08-17 仅起草）；D3 记录保留原始 crm-service 措辞（改道已在 Phase 2 条目 + 日志显式记录为执行期修正）；docs/plans 存在 R1.66/R1.67 兄弟草稿（Non-Goals 明示范围外）。

Follow-up:

- 无（范围内零遗留预期；Deferred 两项已裁定分类）

Post-closure 补记（2026-08-20）：

- matrix §2.4 cs-service → crm-dao 边登记由 plan `docs/plans/2026-08-20-2052-2-arch-matrix-cs-crm-edge-registration.md` 补齐完成（来源 multi 审计 F2：`docs/audits/2026-08-17-2125-multi-audit-requirement-compliance.md`——原收尾同步清单漏列 matrix 登记项，属契约漂移文档登记缺失，非实现缺陷；roadmap :457 done 行同步补记登记短语）。
