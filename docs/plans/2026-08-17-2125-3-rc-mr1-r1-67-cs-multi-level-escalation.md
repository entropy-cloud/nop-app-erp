# 2026-08-17-2125-3-rc-mr1-r1-67-cs-multi-level-escalation RC-R1.67 — cs 多级升级链（P1-RC-056 A 类 ORM：升级计数器 + L2 字段 + 时间窗口 + R1.28 幂等守卫协调）

> Plan Status: completed
> Last Reviewed: 2026-08-18
> Mission: requirement-compliance
> Work Item: RC-R1.67（P1-RC-056，UC-CS-04 ⑩ 重复升级/L2-L3 总监升级链；R1.28 幂等守卫协调）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.67 行 + `docs/audits/arm-index.md` P1-RC-056 行（:231）+ 2026-08-12 批量裁决 A 类（roadmap 头 :40：「cs: RC-R1.67（ErpCsTicket 加 escalation 计数字段 + ErpCsSlaPolicy 加 L2 字段）」ORM 修改授权已批量批准）
> Related: `docs/design/customer-service/sla.md`（§1.1:24-26 + §3.2:163-165 + §实现约定:346 + 配置:286）；`docs/design/customer-service/use-cases.md`（L1 UC-CS-04 :80）；`docs/plans/2026-07-30-0841-2-*`（R1.28 P1-MA2-086 幂等守卫——本 finding 直接成因，不重开并发维度）；`docs/plans/2026-08-16-1634-2-rc-mr1-r1-58-qa-critical-item-veto.md`（A 类 ORM + 双独立子 agent 批准先例）
> Audit: required

## Current Baseline

- **finding P1-RC-056（arm-index:231，UC-CS-04 ⑩）**：L1（`use-cases.md:80`）逐字要求「重复升级（已升级但未处理）→ 每 2h 重复通知 escalationUserId，最多 3 次后向客服总监升级」。L3 实仓（HEAD 核查）：
  - `ErpCsTicketScanOverdueTicketsProcessor.scanOverdueTickets:48-77`：查询过滤（status IN ASSIGNED/IN_PROGRESS + deadlineDateTime < now + isSlaCompleted=false）命中后 `:64-68` `if (hasEscalationAction(ticket.getId())) continue` + `hasEscalationAction:80-86`（QueryBean ticketId + actionType=ESCALATE，limit 1）——**幂等守卫封顶升级次数为 1**（首次 ESCALATE 后永不重复，与 L1 ⑩「每 2h 重复 + 最多 3 次后总监升级」直接冲突）；
  - **无升级级别计数器**：grep `lastEscalationLevel|escalationCount` 跨 module-cs 零命中；`ErpCsTicket` 现有 propId 最大 = 202（businessDate，200-202 已占用 approvedBy/approvedAt/businessDate）→ **新列 propId 203-205 顺延**（禁止中段插入/系统列重编号，对齐 R1.58 先例）；
  - **无 L2 通知目标**：`secondEscalationUserId` ORM 缺失；`ErpCsSlaPolicy` 现有 propId 最大 = 17（updateTime）→ **新列 propId 18/19 顺延**；owner doc `sla.md §1.1:25-26` 已设计 `escalationDelayHours`（一级→二级等待）字段名；
  - **无 escalationDelayHours 定时器**：config `erp-cs.escalation-l1-to-l2-hours`（`sla.md:286` 默认 2h）`ErpCsConfigs.java` 无 reader；
  - **L3 总监通知目标载体缺失**：`slaPolicy.escalationUserId`（orm.xml:271 propId 10 **BIGINT(long) 非 stdDomain=userId**，sla.md:346 caveat）仅 L1 通知目标；
  - **既有单测偏离**：`testScanOverdueTicketsIdempotentNoDuplicateEscalation:162-175` 断言至多一次（`assertEquals(1, countActionsByType(ticketId, ESCALATE))`）——测试与实现同步偏离 L1 ⑩，须改造为「窗口内不重复 + 上限封顶」语义；
  - **通知目标载体漂移（同控制点残留，A2.14:322 / A1.38 reuse 注记）**：`notifySlaOverdue` ctx.put("escalationUserId", ticket.getAssignedToId())（Processor:104 + BizModel:322）非 slaPolicy.escalationUserId；模板 `cs.sla-overdue`（seed 7101）RECIPIENT_CONFIG = ROLE 客服主管（不消费 ctx 目标键）；
  - **调度入口就绪**：`app-erp-all/_vfs/nop/job/conf/erp-cs-sla-scan.job.yaml`（每分钟 cron 默认 `@cfg:nop.job.erp-cs-sla-scan.cron-expr|0 * * * * ?` + `@cfg:nop.job.erp-cs-sla-scan.enabled|false` 双键门控；sla.md:284 语义键 `erp-cs.sla-scan-cron` 为文档命名）→ scanOverdueTickets——多级化在 Processor 内实现，**零新 job**。
- **Q4 判据**：§2 P1①（重复升级/L2-L3 验收标准完全缺失）+ P1②（异常条款结构性不可实现）。三判据复核均不成立（owner doc Non-Goal 标注系 AI 自标无人工批准痕迹，arm-index:231）→ Q4=(a) 强制实现。**2026-08-12 A 类批量裁决**（roadmap 头 :40，用户批准）：`ErpCsTicket` 加 escalation 计数字段 + `ErpCsSlaPolicy` 加 L2 字段 ORM 修改授权已批量批准（对齐 Q3 纯加性类自动执行范围：加列；越界回落双独立子 agent 批准，批准记录落盘计划文件）。**R1.28 协调约束**：`hasEscalationAction` 守卫是 P1-MA2-086 并发去重修复（每分钟 cron 重复 ESCALATE 审计 + 通知噪声最严重案例），本修复以「级别/次数/时间窗口」门控**替代**守卫后必须保持窗口内幂等（防重回并发噪声），非删除去重语义。
- **测试基线**：erp-cs-service **122 @Test 全绿**（2026-08-17 HEAD 递归实测；SLA 相关：TestErpCsTicketSlaCsat / TestErpCsSlaNotification）。
- **compliance 基线**（`docs/audits/compliance-baseline.md` §BASELINE :427-449）：R2b=235 / R2c=1434 / R2d=35 / R10=9 / R12a=70（计数器读取经同实体 getter 预期零新增 daoFor 面；policy 读取经 ticket.slaPolicy to-one）。

## Goals

- **UC-CS-04 ⑩ 多级升级链运行时成立**：deadline 超时 → L1 通知 escalationUserId → 每 escalationDelayHours（默认 2h）重复通知（最多 3 次）→ L2 通知 secondEscalationUserId（未配置则跳级）→ 再经 delayHours → L3 通知客服总监（config 载体）→ 封顶（resolve 前不再升级；工单保持原状态机语义）。
- **ORM 纯加性 5 列**（A 类已授权）：`ErpCsTicket.lastEscalationLevel/escalationCount/lastEscalationAt`（propId 203-205）+ `ErpCsSlaPolicy.secondEscalationUserId/escalationDelayHours`（propId 18/19），无 NOT NULL/默认/索引/UK。
- **R1.28 幂等语义保持**：窗口内（now − lastEscalationAt < delayHours）不重复升级 + 升级后计数器/时间戳同事务更新（替代 hasEscalationAction 反查守卫）——既有并发去重保障不回退。
- **通知目标载体修正（同控制点）**：L1 重复通知 ctx 目标 = slaPolicy.escalationUserId（缺失回退 assignedToId）；L2 = secondEscalationUserId；L3 = config 总监——模板/RECIPIENT_CONFIG 消费路径 Phase 1 D4 定稿。
- **测试补强**：新多级升级测试组 + `testScanOverdueTicketsIdempotentNoDuplicateEscalation` 改造（窗口内不重复 + 次数上限封顶语义）+ 122 基线零回归 + 全量构建 + checker 零漂移（或 baseline-raise per-site 证据）。
- **owner doc 收敛**：sla.md §实现约定:346 Non-Goal 移除 + §1.1/§3.2 实现注记 + README.md:98「仅 L1 升级」更新 + arm-index P1-RC-056 → done (RC-R1.67) + roadmap 行 done + logs 条目。

## Non-Goals

- **不实现 SLA 暂停/仅工作日调整**（owner doc README Non-Goal 列表既有项，与本 finding 无关）。
- **不实现延长 deadline（P2-RC-052 独立 watch-only）**。
- **不实现滞留升级（P2-MA2-067 不同控制点：ASSIGNED>2h 滞留时间维度）**。
- **不拆分 QUALITY_ESCALATE actionType**（RC-R1.68 质量联动越界项范围；本计划 ESCALATE 审计行经 content 承载级别信息并登记 R1.68 协调注记——**R1.68 实现时须改用独立 actionType 或从 fromStatus/toStatus+content 区分，避免与本计划升级计数语义互扰**）。
- **不实现邮件/SMS 实际通道**（IN_APP notify 占位语义，实际发送属 nop-notification 独立面——仓内既定范式）。
- **不改真相源契约段落**（use-cases L1 不动；sla.md 契约段不动，仅移除已失效 Non-Goal 标注 + 补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：ORM 加列[Q3 纯加性，2026-08-12 A 类已授权] + ScanOverdueTicketsProcessor 多级化 + config + notify 目标修正；Q4=(a) 强制实现）
- Owner Docs: `docs/design/customer-service/sla.md`（§1.1/§3.2/§实现约定/§配置）+ `docs/design/customer-service/use-cases.md`（L1 UC-CS-04）+ `docs/design/customer-service/README.md`（:98 L1 升级注记）
- Skill Selection Basis: ORM 加列（增量重生成 `mvn clean install -DskipTests`）；Processor 改造 + config（`nop-backend-dev`：per-mutation Processor + ErpCsConfigs reader 范式）；测试（`nop-testing`：快照 + 时间窗口测试 + CsFrozenClockExtension 既有冻结时钟设施）。

## Infrastructure And Config Prereqs

- **config 三键**（`ErpCsConstants` + `ErpCsConfigs` reader）：`erp-cs.escalation-l1-to-l2-hours`（默认 2，对齐 sla.md:286 + L1「每 2h」）/ `erp-cs.escalation-max-repeat`（默认 3，L1「最多 3 次」）/ `erp-cs.escalation-l3-user-id`（默认空 → 空 = L3 跳过并 LOG.warn，或 ROLE 客服总监模板解析——Phase 1 D4 定稿）。
- **ORM 变更**（双实体加列）→ `mvn clean install -DskipTests` 增量重生成（**不要重跑 nop-cli gen**），生成产物核对（propId 203-205 / 18-19 + DDL 三方言）。
- **双独立子 agent 批准 checkbox**（A 类越界回落：两个独立子 agent fresh session 分别检查批准，批准记录落盘本计划）。
- **调度入口**：复用既有 `erp-cs-sla-scan.job.yaml`（零新 job）；无外部服务/端口/密钥。

## Execution Plan

### Phase 1 - 升级链语义/计数器/守卫/通知目标裁决（Decision）

Status: completed
Targets: `module-cs/model/app-erp-cs.orm.xml`（5 列草案）；`docs/design/customer-service/sla.md`（实现注记草案）
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: none

- [x] `Decision` D1 升级链统一语义（L1 字面 vs sla.md §3.2 表调和）：**主链** = deadline 超时 → L1（escalationUserId，count=1/level=1/lastEscalationAt=now）→ 每经 escalationDelayHours 未解决 → 重复通知 escalationUserId → **「最多 3 次」解释裁决：max-repeat=3 语义 = 重复通知次数上限 3（初始 L1 通知后重复至多 3 次，L1 通知总数至多 4 = count 上限 1+max-repeat）**，count ≥ 1+max-repeat → L2（secondEscalationUserId 非空则通知 + level=2；**null 则跳级**）→ 再经 delayHours → L3（客服总监，config 载体）→ level=3 封顶（不再升级，等 resolve）；否决解释：「最多 3 次」= L1 通知总数 3（重复仅 2 次）——字面主语为「重复通知」，重复计数不含初始更贴字面；sla.md L2/L3 表为此链的 owner-doc 参照层。否决方案：严格 L1 字面（跳过 L2 直达总监——弃用 sla.md L2 设计 + A 类已批 L2 字段）；记录级别/次数/窗口的精确判定式与每分钟 cron 下的观察语义（如 delayHours=2 时窗口粒度=cron 粒度）。
      - Skill: `nop-backend-dev`
- [x] `Decision` D2 计数器载体：`ErpCsTicket` 3 列（lastEscalationLevel INTEGER / escalationCount INTEGER / lastEscalationAt TIMESTAMP，propId 203-205 可空无默认）为**权威判定载体**（替代 hasEscalationAction 反查——反查无法区分级别/次数/时间）；`ErpCsSlaPolicy` 2 列（secondEscalationUserId BIGINT 镜像 escalationUserId 既有类型 + escalationDelayHours INTEGER 可空，propId 18/19；**policy 级 delayHours 优先、null 回退 config 默认 2**）；历史数据兼容（全 null 列 = 未升级，首次扫描视同 level=0 走 L1——既有单次 ESCALATE 审计行存在但计数器 null 的存量工单语义裁决：以计数器为准，审计行仅历史记录，**存量已升级工单重新进入 L1 计数链**——Phase 3 测试 ⑪ 覆盖）。
      - Skill: `nop-backend-dev`
- [x] `Decision` D3 R1.28 幂等守卫重构：查询过滤维持（status/deadline/isSlaCompleted）+ 逐单判定式替换 continue 守卫——`可升级 ⇔ (lastEscalationAt == null) OR (now − lastEscalationAt ≥ delayHours AND 未封顶[级别/次数判据见 D1])`；**并发幂等保障** = 计数器/时间戳与 ESCALATE 审计同事务写入（@BizMutation 单事务 + ticket versionProp 乐观锁），窗口内重复扫描因时间窗判据天然跳过（替代既有 hasEscalationAction 反查，去重语义等价保持）；**hasEscalationAction 处置 = 删除**（避免旧守卫被误用回退；R1.68 质量路径经独立 actionType 区分，不依赖本方法——与 Deferred 条目措辞一致）；**残留风险登记**：批量扫描内单工单乐观锁冲突会中止本轮批量（后续工单本轮跳过）——裁决 = **逐单失败隔离**（per-ticket try/catch WARN，对齐 R1.4/R1.35 批量 job 逐条失败隔离先例），下轮 cron 自然重试。
      - Skill: `nop-backend-dev`
- [x] `Decision` D4 通知目标载体与模板：per-level ctx 键（escalationUserId[L1，policy 值优先/assignedToId 回退——**修正既有 :104/:322 漂移**] / secondEscalationUserId[L2] / l3UserId[L3，config]）+ escalationLevel/repeatCount 信息键；**类型归一化**：policy.escalationUserId 为 BIGINT(long) 而 assignedToId 为 stdDomain=userId VARCHAR(36)——ctx 值统一 stringify（USER_LIST 解析按字符串 userId，Long 直接入 ctx 的模板插值风险显式登记）；**行为变更影响面**：选项 A 将既有 L1 通知从 ROLE 客服主管重路由到策略指定人（UC-CS-04 ③ 合规性修正，但属 live 行为可见变更——README 注记显式声明）；模板裁决——选项 A（推荐）：`cs.sla-overdue` 模板 RECIPIENT_CONFIG 改 USER_LIST `${escalationUserId}`（7101 行更新，三方言）+ L2/L3 复用同事件（ctx 目标键切换）+ 无模板静默降级既有范式；选项 B：新增 `cs.sla-escalation-l2/l3` 独立事件行；记录选择 + 降级语义 + 模板缺失不阻断。
      - Skill: `nop-backend-dev`
- [x] `Decision` D5 ESCALATE 审计行语义：actionType 维持 ESCALATE（dict 不动），content 承载「SLA 超时升级 L{n}（第 {count} 次）通知 {目标}」——为 R1.68 质量路径登记协调注记（R1.68 须用独立 actionType 或 fromStatus/toStatus+content 区分，避免升级计数与质量升级互扰）；fromStatus/toStatus 维持原值（升级不改状态机）。
      - Skill: `nop-backend-dev`
- [x] `Decision` D6 config 语义边界：`erp-cs.escalation-l3-user-id` 空 = L3 跳过 + LOG.warn（对齐 R1.44 上级解析双 null WARN 跳过范式）；`escalation-max-repeat` 下限 1（0 = 直接升级 L2 语义——登记为合法配置边界非错误）；`sla-enabled=false` 既有总门控不变。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] D1-D6 各记录选择 + 替代方案 + 残留风险（写入本计划决策记录节或 sla.md 注记草案）
- [x] orm.xml 5 列草案 well-formed（`xmllint --noout`）+ propId 203-205/18-19 分配核验

#### Phase 1 决策记录（D1-D6 定稿，2026-08-18 执行时落盘）

执行时精化裁决（在计划草案裁决基础上的实现级定稿，均不越出草案授权范围）：

- **D1 定稿判定式**（Processor 逐单执行序）：查询过滤维持（status IN ASSIGNED/IN_PROGRESS ∧ deadlineDateTime<now ∧ isSlaCompleted=false）→ 逐单：`delayHours = policy.escalationDelayHours ?: config(erp-cs.escalation-l1-to-l2-hours, 2)`；`可升级 ⇔ lastEscalationAt==null ∨ now−lastEscalationAt ≥ delayHours`；level=lastEscalationLevel?:0，count=escalationCount?:0；level≥3 → 跳过（封顶）；level=0 → L1（count:=1）；level=1 ∧ count<1+maxRepeat → L1 重复（count++）；level=1 ∧ count≥1+maxRepeat → secondEscalationUserId≠null ? L2（level:=2）: 跳级 L3（level:=3）；level=2 → L3（level:=3）。每分钟 cron 观察语义：实际触发粒度=窗口到期后下一次扫描（≤1 分钟偏差，Deferred 已裁定）。
- **D1 残留**：「最多 3 次」按重复通知计数（总通知 ≤ 4）而非总数 3——语义裁决已记录；若需求方后续按总数解释，仅需下调 max-repeat=2。
- **D2 残留**：存量已升级工单（有 ESCALATE 审计行 + 计数器 null）重新进入 L1 计数链——首轮上线扫描会对这类工单再发一次 L1 通知（一次性噪声，计数器落库后消失；测试 ⑪ 实证）。
- **D3 残留**：per-ticket try/catch WARN 隔离下，若底层异常已标记事务 rollback-only，本轮整批仍可能在提交时回滚——下轮 cron 自然重试（对齐 EntitlementDeactivateExpiredEntitlementsProcessor 同款残留语义）。
- **D4 定稿——单一插值键归一化**：ctx `escalationUserId` 键承载**当前级别有效目标**（L1=policy.escalationUserId→assignedToId 回退；L2=secondEscalationUserId；L3=config 总监），模板 7101 RECIPIENT_CONFIG=`USER_LIST {"userIds":["${escalationUserId}"]}` 单键插值即可按级别路由；同时按级别附 `secondEscalationUserId`/`l3UserId` 原始键 + `escalationLevel`/`repeatCount` 信息键。BizModel findSlaWarnings 预警路径（:322）同步修正为 policy.escalationUserId→assignedToId 回退（同为 UC-CS-04 ③/预警语义修正，README 行为变更声明覆盖）。**行为变更显式声明**：7101 模板 L1/预警接收人从 ROLE 客服主管重路由到策略指定人（policy 缺失回退 assignedToId）。
- **D6 定稿——L3 目标不可解析时的窗口推进**：l3-user-id 空 → LOG.warn 跳过级别推进/通知/审计，**但推进 lastEscalationAt**（不推进 level/count）——约束 WARN 频率至每窗口至多一次（防每分钟 cron 重复 WARN 噪声，R1.28 精神），且保留可恢复性：配置后补后下一窗口真实升级。残留：未配置期间每 delayHours 一次 WARN。**L1 目标双 null 不走此分支**（基线兼容裁决：既有测试断言无 policy/assignee 工单仍产生 ESCALATE 审计与通知派发——L1 升级事件审计 + 计数链独立于通知目标可达性，通知经模板接收人解析静默降级，对齐既有「接收人解析为空 WARN 不阻断」范式）。
- **D6 定稿——max-repeat 边界**：reader 允许 0（=count 上限 1，首次 L1 后下一窗口直接 L2，登记为合法配置边界非错误）；负值钳 0。

### Phase 2 - ORM 落地 + 升级链实现（Add | Fix）

Status: completed
Targets: `module-cs/model/app-erp-cs.orm.xml`（5 列）；`module-cs/erp-cs-service/.../processor/ErpCsTicketScanOverdueTicketsProcessor.java`（多级化）；`ErpCsTicketBizModel.java`（notifySlaOverdue ctx 修正 :313-329）；`ErpCsConstants.java`/`ErpCsConfigs.java`（config 三键）；`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`（7101 行 RECIPIENT_CONFIG 或新行，D4 定稿）；生成产物
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1

- [x] `Add` orm.xml 双实体加 5 列（D2 定稿，propId 203-205/18-19）+ `mvn clean install -DskipTests` 增量重生成 + 生成产物核对（getter/setter + XMeta + DDL 三方言）
      - Skill: `nop-backend-dev`
- [x] **双独立子 agent 批准 checkbox（ORM 保护区域，A 类越界回落）**：两个独立子 agent（fresh session）分别检查批准 5 列纯加性变更（无既有语义变更/无删除/迁移），批准记录落盘本计划（ses id + 结论）
      - Skill: `nop-backend-dev`
- [x] `Fix` `ScanOverdueTicketsProcessor` 多级化（D1/D3）：判定式替换 hasEscalationAction 守卫 + per-level 目标解析 + 计数器/时间戳同事务更新 + ESCALATE 审计 content 级别化（D5）+ notifySlaOverdue ctx per-level 修正（D4，含 :104 目标漂移修正 + BizModel :322 同步）
      - Skill: `nop-backend-dev`
- [x] `Add` config 三键（`ErpCsConstants` 常量 + `ErpCsConfigs` reader，D6 语义）+ 模板种子更新/新增（D4 定稿，三方言一致）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] orm.xml 加列 + 生成产物一致；propId 无冲突（203-205/18-19）
- [x] 双独立子 agent 批准记录落盘（两个 APPROVE 结论 + 检查范围）
- [x] `mvn compile -pl module-cs/erp-cs-service -am` 通过

#### Phase 2 双独立子 agent 批准记录（2026-08-18，A 类越界回落执行）

- **批准 #1**（`ses-orm-approval-1-cs-r167`，task ses_fec8f037dffelmrEl093Rh6jRT）：**APPROVE**。检查范围：git diff 精确 5 插入行 0 删除；5 列与 A 类授权逐字对应；无既有 column/entity/relation/index/UK/dict 修改、无 propId 重编号、新列无 mandatory/defaultValue/index/UK；Ticket propId 1-29+200-205 / Policy 1-19 无碰撞；类型镜像/对齐 sibling（secondEscalationUserId BIGINT 镜像 escalationUserId、lastEscalationAt tagSet=clock）；全可空无默认 → 存量行 NULL 零语义变更；XML 属性风格镜像 sibling。FINDINGS：无阻塞（信息性：Ticket 既有 22-29 排在 200-202 之后的非单调 propId 为预存状态未触碰）。
- **批准 #2**（`ses-orm-approval-2-r167-cs5col`，task ses_fec8eed1fffekHv960qPQb74yy）：**APPROVE**。检查范围：两实体完整 `<columns>` 通读（Ticket 35 列 / Policy 19 列全 propId 编号零重复——「缺失 propId」计数经核为 index 定义内 `<column>` 引用非实体列）；纯加性逐项（索引/UK 块与 HEAD byte-identical）；propId 顺延对齐 approvedBy/approvedAt/businessDate 先例；生成代码 append-only 推演（可空尾加列仅追加 getter/setter/XMeta 可选属性，DDL 为可空 ADD COLUMN）；范围与 2026-08-12 A 类批量裁决逐字段一致无越界。FINDINGS：无阻塞（信息性：ext:/ui: 命名空间 xmllint 警告为全文件预存 DSL 惯例；secondEscalationUserId 保持 BIGINT 镜像为 D2 显式裁决）。
- 生成产物核对（执行者）：`_gen/_ErpCsTicket.java`（getLastEscalationLevel/getEscalationCount/getLastEscalationAt :1821/:1840/:1859）+ `_gen/_ErpCsSlaPolicy.java`（getSecondEscalationUserId/getEscalationDelayHours :1041/:1060）+ `ErpCsTicket.xmeta` + i18n 双语 + api Input/Output bean + `_app.orm.xml` + DDL 三方言（mysql:267-269/oracle:267-269+COMMENT/postgresql）均含 5 新列；`module-cs` 全链 `mvn clean install -DskipTests` exit 0。

### Phase 3 - 测试 + 文档回填（Proof）

Status: completed
Targets: `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsMultiLevelEscalation.java`（新建）+ 既有 SLA 测试改造；`docs/design/customer-service/sla.md` + `README.md`；`docs/audits/arm-index.md`（P1-RC-056 行）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.67 行）；`docs/logs/2026/08-{17,18}.md`
Skill: `nop-testing`

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 2

- [x] `Proof` `TestErpCsMultiLevelEscalation` 测试组（复用 `CsFrozenClockExtension` 冻结时钟推进窗口）：①首次超时 → L1 ESCALATE（count=1/level=1/lastEscalationAt 落库）+ 通知目标 = policy.escalationUserId（漂移修正断言）；②窗口内（<2h）重复扫描 → 零新增审计（幂等保持）；③窗口后 → 重复通知（count=2→3→4，重复 ×3 达 D1 解释上限）；④count=4（1+max-repeat）达上限 → L2（secondEscalationUserId 配置时通知 + level=2）；⑤secondEscalationUserId=null → 跳级直达 L3；⑥L2 窗口后 → L3 总监（config 载体）+ level=3 封顶（后续扫描零动作）；⑦l3-user-id 空 → L3 跳过 WARN；⑧policy.escalationDelayHours 覆盖 config 默认；⑨resolve 后（isSlaCompleted=true）不再命中查询；⑩`_cases/` 快照录制；⑪**存量工单兼容**（seed 含既有 ESCALATE 审计行 + 计数器 null → 首次扫描重新进入 L1 count=1 + 新增审计行——计数器权威判定实证，hasEscalationAction 删除不搁浅存量超时工单）
      - Skill: `nop-testing`
- [x] `Fix` `testScanOverdueTicketsIdempotentNoDuplicateEscalation:162-175` 改造：断言语义从「至多一次」改为「窗口内不重复 + 次数上限封顶」（与实现同步对齐 L1 ⑩，finding 记录的测试偏离修复）
      - Skill: `nop-testing`
- [x] `Proof` 零回归验证：`mvn test -pl module-cs/erp-cs-service` 全绿（122 基线 + 新增，含既有 SLA 快照核验）
      - Skill: `nop-testing`
- [x] `Proof` compliance checker 复跑：`bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（计数器经同实体 getter + policy 经 to-one 预期零新增 daoFor 面；若漂移分类 per-site 证据；**属 Closure Gates 级验证，此处仅登记证据**）
      - Skill: none
- [x] `Add` 文档回填：sla.md §实现约定:346 Non-Goal 移除 + §1.1/§3.2 补实现注记（D1-D6 裁决 + 计数器 + config + 「最多 3 次」解释）+ README.md:98「仅 L1 升级」更新（含 L1 通知目标重路由声明）+ arm-index P1-RC-056 → done (RC-R1.67) + **roadmap RC-R1.67 行标签补 2026-08-12 A 类授权措辞**（对齐 R1.49/51/56/57/58/60 已执行行先例）+ 行 done ✅ + logs 条目 + R1.68 协调注记（ESCALATE actionType 区分义务）
      - Skill: none

Exit Criteria:

- [x] 测试组 ①-⑪ 全绿 + 幂等改造测试通过（指定成功/失败模式：窗口内零新增 + 跳级 + 封顶 + resolve 退出 + 存量兼容逐项）
- [x] erp-cs-service 模块测试全绿（122 基线零回归，失败模式=任何既有测试翻红）

#### Phase 3 执行记录（2026-08-18）

- **测试组 ①-⑪**：`TestErpCsMultiLevelEscalation` 7 @Test 全绿（①+② testFirstOverdueL1PolicyTargetAndInWindowIdempotency / ③④⑥ testRepeatUpToCapThenL2ThenL3Capped / ⑤ testSecondNullSkipsToL3 / ⑦ testL3ConfigEmptyWarnsAndSkips[含窗口推进 + WARN 有界断言] / ⑧ testPolicyDelayHoursOverridesConfigDefault / ⑨ testResolvedTicketNotScanned / ⑩ 空 autotest.yaml 标记 7 方法目录 / ⑪ testLegacyTicketWithExistingEscalateReentersL1）；冻结时钟 CsFrozenClockExtension + advanceDays 按天推进窗口。
- **幂等改造**：`TestErpCsTicketSlaCsat.testScanOverdueTicketsIdempotentNoDuplicateEscalation`（:157-191）——首次扫描 level=1/count=1/lastEscalationAt 落库断言 + 窗口内零新增 + lastEscalationAt 回拨 2h 模拟窗口到期 → 允许重复 count=2。
- **零回归**：`mvn test -pl module-cs/erp-cs-service` **144 tests / 0 failures / 0 errors**（R1.66 后 137 基线 + 7 新增；计划头「122 基线」为起草时点计数，R1.65/66 增量后权威基线 = 137）。
- **执行期发现 + 修复（测试基建）**：模块全量执行时 `TestErpCsTicketSlaCsat` 13 测试 `unknown-operation` 类级失效——根因 = 父 POM（nop-entropy）surefire `forkCount=4 + reuseForks=true + parallel=classes` 类并行共享 JVM 与 nop-autotest 全局静态 `CoreInitialization` 生命周期竞争（调度时序相关：单类/小子集全绿、19 类 DeltaOverride 在场确定性复现；CLI 参数无法覆盖父 POM 硬编码配置）；修复 = `module-cs/erp-cs-service/pom.xml` 覆写 surefire `reuseForks=false` 每类独立 JVM（保留 fork 并行 + jacoco argLine，测试作用域构建配置）；与 R1.66 全仓首跑 TestErpMdSkuServices unknown-operation 同根因形态——bugs 立档 `docs/bugs/2026-08-18-surefire-parallel-classes-autotest-static-lifecycle-race.md`（其他模块同款覆写归 successor）。
- **compliance checker**：全 19 规则 ≤ 权威基线零漂移——R2c 1438≤1439（`hasEscalationAction` 删除 −1 面）/ R10 11≤11 / R2b=235 / R2d=35 / R12a=70 零变化；**本计划零新增 daoFor/REQUIRES_NEW 站点**（计数器经同实体 getter + policy 经 `ticket.getSlaPolicy()` to-one，符合 D2 预期）。计划头引用 R2c=1434/R10=9 为 R1.65/66 baseline-raise 前旧值（现权威基线块 = 1439/11，`compliance-baseline.md` §BASELINE）。
- **文档回填**：sla.md §1.1 实现注记 + §3.2 实现注记（D1 判定式/「最多 3 次」解释/L1 重路由行为变更声明/R1.68 协调义务）+ §实现约定:346 Non-Goal 移除 + §5.3 config 三键 + README.md 多级升级链已实现（含行为变更声明）+ arm-index P1-RC-056 → done (RC-R1.67) + roadmap RC-R1.67 行标签 A 类授权措辞 + done ✅ + logs `docs/logs/2026/08-18.md` 条目 + bugs 立档；use-cases.md L1 不动。

（全仓 `mvn clean install -DskipTests` + compliance checker 最终裁决属 Closure Gates，不在阶段退出重复——见执行时规则 7。）

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_feec1006cffex1kx1LxcnsjMoW`) — 基线抽查全 PASS（Processor/BizModel/orm/use-cases/sla.md/测试/job yaml/种子/122 @Test/compliance 基线逐项核实），7 MINOR 无阻塞项。起草者已就地落实全部 MINOR：m1 已修订：测试 ⑪ 增存量工单兼容组（既有 ESCALATE 审计行 + 计数器 null → 重新进入 L1 计数链）；m2 已修订：D3 hasEscalationAction 处置定稿 = 删除（与 Deferred 措辞一致）；m3 已修订：D3 增批量乐观锁冲突残留 + 裁决 = 逐单失败隔离（per-ticket try/catch WARN，R1.4/R1.35 先例）+ 下轮 cron 重试；m4 已修订：D4 增类型归一化（BIGINT vs VARCHAR userId 统一 stringify）+ 行为变更影响面显式声明（L1 通知 ROLE→策略指定人重路由 = UC-CS-04 ③ 合规性修正）；m5 已修订：baseline job-yaml 键名修正（`nop.job.erp-cs-sla-scan.enabled|false` + `nop.job.erp-cs-sla-scan.cron-expr|0 * * * * ?`，sla.md:284 语义键为文档命名）；m6 已修订：Phase 3 全仓构建/checker 移出阶段退出（归 Closure Gates，执行时规则 7）；m7 已修订：D1 增「最多 3 次」解释裁决（max-repeat=3 = 重复通知次数上限，L1 总数至多 4 = 1+max-repeat；否决解释记录）+ 测试 ③④ 算术同步。
- Independent draft review iteration 2（delta 复核）: `acceptable as-is`（`ses_feeba0ba0ffeyS0BVd03ZB25Qx`）——全部 7 项 MINOR 修复逐项核实落地且内部一致（测试 ⑪ ↔ D2 ↔ D3 删除处置三角一致 / D1 算术链 count=2→3→4→L2 与 D6 max-repeat=0 边界自洽 / 执行时规则 7 引用核实为仓级惯例）；2 项亚 MINOR（N1 cron 键前缀 `nop.job.` 缺失——纯描述性零执行影响；N2 Deferred「弃用」vs D3「删除」措辞差）已由起草者就地修复（键名补前缀 + Deferred 措辞对齐删除）→ 草案审查收敛，`Plan Status: draft → active`。

## Closure Gates

- [x] 范围内行为完成（UC-CS-04 ⑩ 多级升级链 + 计数器 + R1.28 幂等保持 + 通知目标修正）
- [x] 相关文档对齐（sla.md/README.md/arm-index/roadmap/logs）
- [x] 已运行验证（`mvn test -pl module-cs/erp-cs-service` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ESCALATE 与质量升级 actionType 区分（R1.68 协调）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划 ESCALATE 审计 content 承载级别信息且计数器判定不依赖 actionType 反查（hasEscalationAction **删除**）；R1.68 质量联动落地时以独立 actionType/区分约定实现，协调注记已落 owner doc + 本计划
- Successor Required: `yes`（RC-R1.68 执行时）

### 邮件/SMS 实际通道派发

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: IN_APP notify 占位语义为仓内既定范式（sla.md「通知占位，实际发送属 nop-notification 独立面」）
- Successor Required: `yes`（nop-notification 独立面接入时）

### cron 粒度与 delayHours 窗口粒度差

- Classification: `watch-only residual`
- Why Not Blocking Closure: sla-scan 每分钟执行，delayHours=2h 下实际触发粒度为「窗口到期后的下一次扫描」（≤1 分钟偏差）；行为语义（每 2h 重复）不受影响
- Successor Required: `no`

## Closure

Status Note: completed（2026-08-18。Phase 1-3 全勾选 + 验证全绿 + 独立结束审计 PASS。）

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，task `ses_feba7dfdaffe7dAUirsXttENWW`，未参与实现），2026-08-18。
- Verdict: **PASS**（8/8 项 PASS：计划一致性 / 实现 D1-D6 逐条 file:line 核验 / 测试独立复跑 144/0/0 + TestErpCsMultiLevelEscalation 7/0/0 / checker 独立复跑全规则 ≤ 基线（R2c 1438≤1439、R10=11、R2b=235、R2d=35、R12a=70）/ 文档回填六处核验（arm-index done + roadmap done ✅ + logs + bugs + use-cases L1 未动）/ 反模式零命中 / Non-Goals 尊重 / ORM diff 纯加性 5 列零越界）。
- INFO 级非阻塞观察 3 条（审计报告全文存于该 task 会话）：①Closure Gates 回填为本审计后动作（已执行）；②全仓 `mvn clean install -DskipTests` BUILD SUCCESS（03:04，2026-08-18 18:07 执行者运行）在审计范围外，模块级验证均独立复跑通过；③L1 目标双 null 仍写审计+推进计数链为 D6 定稿基线兼容裁决（计划/代码注释/owner doc 三处登记一致）。
- 验证基线（全绿）：`mvn test -pl module-cs/erp-cs-service` 144/0/0（surefire reuseForks=false 每类独立 JVM）+ 全仓 `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 ≤ 权威基线零漂移。

Follow-up:

- 无（范围内零遗留；Deferred 三项已裁定分类——R1.68 actionType 区分 successor=yes、邮件/SMS 通道 successor=yes、cron 粒度差 successor=no；surefire 类并行竞争其他模块同款覆写归 successor，bugs 立档 `docs/bugs/2026-08-18-surefire-parallel-classes-autotest-static-lifecycle-race.md`）。
