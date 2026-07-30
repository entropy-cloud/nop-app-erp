# 2026-07-30-0841-2-r1-28-concurrency-uk-idempotency 并发 UK / TOCTOU / cron 幂等缺口修复

> Plan Status: active
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.28（P1-MA2-085/086/087/088/089/090/091/092 = 8 findings），源自 A2.17 并发与乐观锁审计
> Related: `docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`；`docs/design/flow-overview.md §事务边界`；deferred P0-MA2-018（`docs/plans/2026-07-28-1249-arm-fix-p0-ma2-018-voucher-bill-r-uk.md`）
> Audit: required

## Current Baseline

八项 finding 经实仓逐项确认：并发 TOCTOU / cron 重复副作用 / 缺 DB 唯一约束。**多数 config-gated 默认 OFF 或有 version guard 兜底**，无活跃数据破坏（P1 非 P0）。

**并发基础设施现状 — 确认：** 全仓 grep `@CronProvider`/`nop_sys_cluster_leader`/`IErpSysLockBiz`/`*.job.xml` **零匹配**——全 19 cron job 运行于 `nop-job-local` 的 `LocalJobScheduler`（非分布式，无 leader-lock），全部默认 `enabled=false`（P1-MA2-086 根因）。

**P1-MA2-085（inv LandedCost TOCTOU）— 确认：** `erp_inv_landed_cost` 仅 `UK_INV_LANDED_COST_CODE_ORG (code,orgId)`（`app-erp-inventory.orm.xml:1353`），无 `(receiveId)` 维度约束；`ErpInvLandedCostProcessor.validateNotAlreadyAllocated:90` 是 TOCTOU pre-check query → 并发 approve 同 receiveId 两 LandedCost 双 allocation 进 StockBalance。

**P1-MA2-086（10 cron job 并发重复副作用）— 确认：** 10 job 中 2 个产生**重复实体行**（`erp-mnt-due-visit-generation` 无 insert 前 existence check → 重复 `VST-SCH-{schedId}-{date}` 行；`erp-qa-spc-sampling` append sample 重复）+ `erp-cs-sla-scan`（每分钟，最严重噪音）无 escalation dedup（`isSlaCompleted` 不翻转 → 每分钟重复 ESCALATE 审计行 + 通知）+ 通知重复类（crm/hr/cs）。9 job 幂等（recompute/refresh + deferred-posting-sweep 经 alreadyPosted 去重）。

**P1-MA2-087（CloseVoucherWriter 无幂等 pre-check）— 确认：** `CloseVoucherWriter.writeVoucher`（静态工具，:25-140）直接持久化凭证经 `IErpFinVoucherBiz.post` Facade，依赖 `alreadyPosted` TOCTOU pre-check + 无 `(billHeadCode, businessType)` UK。bounded by period version guard（同期间并发 close 经期间 version 守护，一个 OL 回滚）。**与 deferred P0-MA2-018 同根因**（P0-MA2-018 字面 UK 经独立 plan-audit 裁定不可实施——红冲同键 2 行 / 多账套同键 N 行 / 软删除重插三重冲突；deferred plan 方向 A/B/C/D 维持不变）。

**P1-MA2-088（b2b webhook 幂等）— 确认：** `ErpB2bEdiDoc` 仅 `(code,orgId)` UK（`app-erp-b2b.orm.xml:196`），无 `(sourceType, sourceEventId)`；`isDuplicateEvent:439` query `remark="WEBHOOK eventId=..."` TOCTOU + ASN `code="ASN-"+currentTimeMillis()`（每次不同，UK 不兜底）→ 重复 webhook 创建重复 ASN + EdiDoc。config-gated `erp-b2b.b2b-enabled` 默认 OFF。

**P1-MA2-089（assets 折旧 schedule 并发重复）— 确认：** `ErpAstDepreciationScheduleProcessor.executeDepreciation:52` 仅 `requireAsset + validateAssetInService + requirePeriodOpen`，**无 status==PENDING 守卫** → 并发首次折旧两事务都 INSERT 重复 schedule 行 + `setAccumulatedDepreciation` 双计。`erp_ast_depreciation_schedule` 无 `(assetId, period)` UK（`period` 为 VARCHAR(20) 折旧期间字符串，非 periodId FK）。

**P1-MA2-090（mfg MRP release 并发释放同 plan line）— 确认：** `MrpReleaseService.releaseToSubcontractOrder:115-133` `requireReleasable`（isFirmed==false）TOCTOU + 生成 `SUB-MRP-{lineId}`（UK `(code,orgId)` 兜底但抛 `ERR_ORM_DATA_EXCEPTION` 丑陋异常）；同型 releaseToPurchaseOrder/WorkOrder。

**P1-MA2-091（hr shift assignment 并发重复）— 确认：** `ErpHrShiftAssignmentBizModel.assignSingle:60-67` + `assertNoExistingAssignment:153` TOCTOU pre-check + `erp_hr_shift_assignment` 无 `(employeeId, assignmentDate)` UK（`UK_HR_ATTENDANCE_EMP_DATE` 在 attendance 表不在 assignment 表）。

**P1-MA2-092（logistics trackingNo 无 UK）— 确认：** `erp_log_shipment` 仅 `(code,orgId)` UK，无 `(trackingNo, carrierId)`；网关回调 + 手工创建并发可创建重复 trackingNo shipment。状态轴经 versionProp + `advanceTracking:154` DELIVERED 幂等守卫 sustained。

**保护区域：** 多项触及 ORM（`model/*.orm.xml` 加 UK = `[ORM ask-first for UK]`）—— 089/091/092 须人工确认 + 数据 cleanup 评估 + plan-audit。087 受 deferred P0-MA2-018 约束不独立加 UK。无会计/数据删除保护区域触及（并发硬性化为主，非凭证写逻辑变更）。

## Goals

- **085** inv LandedCost TOCTOU 收口（application-layer guard）。
- **086** 10 cron job 中产生重复实体行 + 最严重噪音的 job 补幂等体（existence check / dedup flag）。
- **087** CloseVoucherWriter 幂等约束 documented（bounded by period version guard + successor = P0-MA2-018）。
- **088** b2b webhook 幂等（确定性 ASN code 复用既有 UK）。
- **089** assets 折旧 schedule 加 `(assetId, period)` UK（period 为 VARCHAR 折旧期间）+ status==PENDING 守卫。
- **090** mfg MRP release 并发释放友好错误码（捕获 ConstraintViolation 翻译）。
- **091** hr shift assignment 加 `(employeeId, assignmentDate, shiftId)` UK。
- **092** logistics shipment 加 `(trackingNo, carrierId)` UK。

## Non-Goals

- 不迁移 `nop-job-service` + cluster leader 锁（P1-MA2-086 方案A——平台级迁移，单实例 nop-job-local 部署下不必要；归 successor，触发条件 = 多实例部署）。
- 不实现 `IErpSysLockBiz` 分布式锁 SPI（P1-MA2-086 方案B——本仓库不存在该 SPI，须先实现；归 successor）。
- 不独立为 CloseVoucherWriter 加 UK（P1-MA2-087——与 deferred P0-MA2-018 同根因，P0-MA2-018 字面 UK 已裁定不可实施；归 P0-MA2-018 successor）。
- 不改通知重复类 job 的通知去重键（cs/crm/hr 通知重复归 086 范围内最低优先级，仅 document）。
- 不为 inv LandedCost 加字面 UK（条件唯一——须允许多 DRAFT；走 application-layer guard）。

## Task Route

- Type: `implementation-only change`（并发 UK + 守卫 + cron 幂等）
- Owner Docs: `docs/design/flow-overview.md §事务边界`、各域 `model/*.orm.xml`、`docs/audits/compliance-baseline.md`
- Skill Selection Basis: ORM UK 声明 + Processor 守卫 + cron job 幂等 + 跨实体调用 → `Skill: nop-backend-dev`。UK 加完后 `mvn clean install -DskipTests` 触发增量再生。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. cron job 全部默认 enabled=false；b2b config-gated 默认 OFF。

## Execution Plan

### Phase 1 - 八项 finding 裁决（Decision）

Status: planned
Targets: 本计划（裁决记录）
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: none

- [ ] **Decision（085）**：**方案B（application-layer guard）**。LandedCost 业务允许多 DRAFT（条件唯一，字面 UK 无法表达），故 `validateNotAlreadyAllocated` 改为事务内对 receive 加锁/re-query（`SELECT ... FOR UPDATE` 或 `findApprovedByReceiveId` 后置二次校验）收口 TOCTOU，而非加 UK。方案A（UK）被拒——条件唯一不兼容。
      - Skill: `nop-backend-dev`
- [ ] **Decision（086）**：**方案C（per-job body 幂等）**。优先级：数据腐败类（erp-qa-spc-sampling insert 前 existence check / erp-mnt-due-visit-generation `VST-SCH-{schedId}-{date}` existence check）→ 最严重噪音类（erp-cs-sla-scan `lastEscalationAt`/`isSlaCompleted` dedup flag）→ 通知重复类（cs/crm/hr，仅 document）。方案A/B（平台迁移 / IErpSysLockBiz）归 successor（触发 = 多实例部署）。
      - Skill: `nop-backend-dev`
- [ ] **Decision（087）**：**documented constraint（不独立加 UK）**。与 deferred P0-MA2-018 同根因——P0-MA2-018 字面 `(billCode, businessType)` UK 已裁定不可实施（红冲同键 2 行 / 多账套同键 N 行 / 软删除重插三重冲突）。当前 mitigation = period version guard（同期间并发 close 经期间 version 守护，一个 OL 回滚）+ successor = P0-MA2-018 deferred plan 方向 A/B/C/D 解决。**为何无独立 application-layer 缓解**：(a) `CloseVoucherWriter.writeVoucher` 本身**无任何幂等 pre-check**（非仅 TOCTOU），但凭证 billHeadCode 含期间码（`PERIOD-CLOSE-{code}`/`FX-REVAL-{code}`/`ANNUAL-CLOSE-{code}`），同期间并发 close 已被期间 version guard 序列化；(b) 窄化 UK 仅限 close-voucher billType 仍遭红冲同键冲突（close 凭证可被 reverseClose 红冲产生同 billHeadCode 第二行）；(c) SELECT FOR UPDATE on 期间行等价于已有 version guard。故 application-layer 独立缓解与既有 period version guard 重复或被 P0-MA2-018 同根因阻塞。owner doc flow-overview §事务边界 补注。
      - Skill: `nop-backend-dev`
- [ ] **Decision（088）**：**方案B（确定性 ASN code）**。ASN code 改为 `ASN-WEBHOOK-{eventId}`（确定性派生），使既有 `(code,orgId)` UK 兜底重复 webhook；`isDuplicateEvent` 保留作前置优化。方案A（加 sourceEventId 列 UK = ORM ask-first）被拒——确定性 code 零 ORM 变更即收口。**残留风险**：当前 `handleInboundWebhook:113` 仅 `eventId != null` 时走 dedup，`eventId==null` 路径**零 dedup**（remark 亦不写 :373）；确定性 code 在 eventId==null 时塌缩为 `ASN-WEBHOOK-null` → 第二个 null-eventId webhook 触发 UK 冲突。处理：eventId==null 时 fallback 到 `ASN-WEBHOOK-{currentTimeMillis()}`（保留唯一性）或要求 webhook 边界 eventId 非空；须在 Phase 3 显式覆盖 null-eventId 分支测试。
      - Skill: `nop-backend-dev`
- [ ] **Decision（089）**：**方案A（UK + 守卫）[ORM ask-first]**。`erp_ast_depreciation_schedule` 加 `(assetId, period)` UK（`period` 为 VARCHAR(20) 折旧期间字符串）+ `executeDepreciation` 增 status==PENDING 守卫 + 捕获 ConstraintViolation 翻译为 `ERR_AST_DEPRECIATION_ALREADY_EXECUTED`。须数据 cleanup 评估（历史重复行）。方案B（requireSchedulePending 守卫 + 已存在走 reverse+reexec 自愈扩展到 PENDING）被拒——UK 提供 DB 兜底防御更稳健（对齐 P0-MA2-020 inventory 余额范式）。
      - Skill: `nop-backend-dev`
- [ ] **Decision（090）**：**方案B（友好错误码）**。`MrpReleaseService` release 三方法（Subcontract/Purchase/WorkOrder）捕获既有 `(code,orgId)` UK ConstraintViolation → 翻译为 `ERR_MRP_LINE_ALREADY_RELEASED`（+ isFirmed 守卫前置）。方案A（加 mrpPlanLineId UK = ORM ask-first）被拒——既有 UK 已兜底，仅需友好异常。
      - Skill: `nop-backend-dev`
- [ ] **Decision（091）**：**方案A（UK）[ORM ask-first]**。`erp_hr_shift_assignment` 加 `(employeeId, assignmentDate, shiftId)` UK + 捕获 ConstraintViolation 翻译为 `ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE`。方案B（改 `assertNoExistingAssignment` 为 SELECT FOR UPDATE）被拒——UK 提供 DB 兜底更稳健。**残留风险**：该表 `useLogicalDelete=true`（软删除），字面 UK 与软删除重插存在与 P0-MA2-018 同型交互——须确认排班是否实际软删除；若软删除+重建同键场景存在，UK 须条件唯一回退（delVersion-aware guard）或仅作 application guard。
      - Skill: `nop-backend-dev`
- [ ] **Decision（092）**：**方案A（UK）[ORM ask-first]**。`erp_log_shipment` 加 `(trackingNo, carrierId)` UK（trackingNo 允许 null——未发货单不约束）+ 捕获 ConstraintViolation 翻译为 `ERR_LOG_SHIPMENT_TRACKING_NO_DUPLICATE`。**残留风险**：同 091——`erp_log_shipment` 逻辑删除，须确认软删除重插交互；平台对含 null 列的 UK 语义须验证（null 是否约束）。方案B（SELECT FOR UPDATE）被拒——UK 更稳健。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] Phase 1 八项 Decision 逐项记录；ORM ask-first 项（089/091/092）明确标 `[ORM ask-first]` 待人工确认。

### Phase 2 - ORM UK 声明（089/091/092）[ORM ask-first，须人工确认]

Status: planned
Targets: `module-assets/model/app-erp-assets.orm.xml`、`module-hr/model/app-erp-hr.orm.xml`、`module-logistics/model/app-erp-logistics.orm.xml`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 + 人工确认 ORM 变更

- [ ] **Add（089）**：`ErpAstDepreciationSchedule` 加 `<unique-key name="UK_AST_DEPRECIATION_ASSET_PERIOD" columns="assetId,period"/>`（`period` 为 VARCHAR 折旧期间）+ 历史重复行 cleanup 评估（若存在，须 data fix 脚本）。
      - Skill: `nop-backend-dev`
- [ ] **Add（091）**：`ErpHrShiftAssignment` 加 `<unique-key name="UK_HR_SHIFT_ASSIGNMENT_NATURAL" columns="employeeId,assignmentDate,shiftId"/>`。
      - Skill: `nop-backend-dev`
- [ ] **Add（092）**：`ErpLogShipment` 加 `<unique-key name="UK_LOG_SHIPMENT_TRACKING_CARRIER" columns="trackingNo,carrierId"/>`（确认平台对含 null 列的 UK 语义——若 null 不约束则安全；若约束则须条件唯一回退 application guard）。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：`mvn clean install -DskipTests` 触发增量再生 + 三个域 codegen 成功；DB schema 生成含新 UK。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 三域 ORM UK 声明落地 + codegen 增量再生成功（仅解除 Phase 3 守卫依赖所需的本地化检查）。

### Phase 3 - 守卫 + application-layer 幂等（085/088/089/090/091/092）

Status: planned
Targets: `ErpInvLandedCostProcessor.java`、`ErpB2bAsnBizModel.java`、`ErpAstDepreciationScheduleProcessor.java`、`MrpReleaseService.java`、`ErpHrShiftAssignmentBizModel.java`、logistics 创建路径
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 2

- [ ] **Fix（085）**：`ErpInvLandedCostProcessor.validateNotAlreadyAllocated` 改为事务内对 receive 锁/re-query 收口 TOCTOU（前置 query + 后置二次校验，或 SELECT FOR UPDATE on receive）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（088）**：`ErpB2bAsnBizModel.handleInboundWebhook` ASN code 改确定性派生 `ASN-WEBHOOK-{eventId}`（复用既有 `(code,orgId)` UK 兜底）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（089）**：`executeDepreciation` 增 status==PENDING 守卫 + 捕获 UK ConstraintViolation → `ERR_AST_DEPRECIATION_ALREADY_EXECUTED`。
      - Skill: `nop-backend-dev`
- [ ] **Fix（090）**：`MrpReleaseService` 三 release 方法捕获 ConstraintViolation → `ERR_MRP_LINE_ALREADY_RELEASED` + isFirmed 前置守卫。
      - Skill: `nop-backend-dev`
- [ ] **Fix（091）**：`assignSingle` 捕获 UK ConstraintViolation → `ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE`（TOCTOU pre-check 保留作前置优化）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（092）**：logistics shipment 创建路径捕获 UK ConstraintViolation → `ERR_LOG_SHIPMENT_TRACKING_NO_DUPLICATE`。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：各域并发负向测试——重复 approve/assign/release/webhook 断言抛业务错误码（非 `ERR_ORM_DATA_EXCEPTION`）+ 无重复实体行。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 085/088/090/091/092 并发负向测试抛友好错误码；089 status 守卫 + UK 双重防护可测。

### Phase 4 - cron job 幂等体（086）+ 087 documented 约束 + owner doc

Status: planned
Targets: `erp-qa-spc-sampling` job、`erp-mnt-due-visit-generation` job、`erp-cs-sla-scan` job、`docs/design/flow-overview.md §事务边界`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [ ] **Fix（086，数据腐败类）**：`erp-mnt-due-visit-generation` insert 前 existence check（按 `VST-SCH-{schedId}-{date}` 查存在则 skip）；`erp-qa-spc-sampling` append sample 前 existence/dedup check。
      - Skill: `nop-backend-dev`
- [ ] **Fix（086，最严重噪音类）**：`erp-cs-sla-scan` 增 escalation dedup（`lastEscalationAt` 或 `isSlaCompleted` 翻转，避免每分钟重复 ESCALATE 审计行 + 通知）。
      - Skill: `nop-backend-dev`
- [ ] **Add（086，通知重复类 document）**：cs/crm/hr 通知重复类 job owner doc 标注「nop-job-local 单实例下通知可能重复，去重键 successor」。
      - Skill: `nop-backend-dev`
- [ ] **Add（087，documented）**：`docs/design/flow-overview.md §事务边界` 补注 CloseVoucherWriter 幂等受 period version guard 保护 + P0-MA2-018 successor 约束。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：086 数据腐败类 job 连续两次调用断言无重复实体行；erp-cs-sla-scan 连续两分钟断言不重复 escalation。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 086 数据腐败类 + 最严重噪音类幂等可测；087 documented 约束落地 owner doc。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_04f83d4c1ffe7an7tgJxLhp0zz) because (a) Decision 089 + Phase 2 Add(089) UK 引用不存在的 `periodId` 列（实际为 `period` VARCHAR 折旧期间）→ 字面 UK 会 fail codegen，违反「经实仓逐项确认」诚实性；(b) Decision 088 遗漏 eventId==null 残留风险——`handleInboundWebhook:113` 仅 eventId!=null 走 dedup，确定性 code `ASN-WEBHOOK-null` 在 null-eventId 时塌缩冲突。非阻塞：087 rationale 偏薄 / 091+092 软删除与 UK 交互未记 / 089+091 缺拒绝方案。基线事实全绿（inv LandedCost 无 receiveId UK / 零 cron lock 基础设施 / b2b ASN 非确定性 code + isDuplicateEvent / assets 折旧无 PENDING 守卫无 UK / P0-MA2-018 deferred / hr assignment TOCTOU 无 UK 均确认）。
- Independent draft review iteration 2: accept (ses_04f83d4c1ffe7an7tgJxLhp0zz) after 089 UK 改 `columns="assetId,period"`（period VARCHAR）+ Decision 补拒绝方案B；088 增 eventId==null 残留 + null-fallback 处理 + 测试覆盖；087 强化 rationale（为何无独立 application-layer 缓解——billHeadCode 含期间码 + 窄化 UK 仍遭红冲同键冲突 + SELECT FOR UPDATE 等价 version guard）；091/092 补软删除与 UK 交互残留风险。

## Closure Gates

- [ ] 范围内行为完成（8 项 finding：085 guard + 086 per-job 幂等 + 087 documented + 088 确定性 code + 089/091/092 UK + 090 友好错误码）
- [ ] 相关文档对齐（flow-overview §事务边界 + 各域 owner doc）
- [ ] 已运行验证（`mvn clean install -DskipTests` 全绿 + `mvn test` 全绿 + compliance checker 基线不高于 M0）
- [ ] 无范围内项目降级为 deferred/follow-up（086 平台迁移 / 087 P0-MA2-018 successor 为显式 successor，非范围内降级）
- [ ] ORM ask-first 变更（089/091/092）经人工确认
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### cron job 平台级并发防护（P1-MA2-086 方案A/B）

- Classification: `optimization candidate`
- Why Not Blocking Closure: per-job body 幂等（方案C）已收口数据腐败类 + 最严重噪音类；平台迁移（nop-job-service cluster leader）/ IErpSysLockBiz SPI 在单实例 nop-job-local 部署下不必要。
- Successor Required: `yes`（当部署切换为多实例时，迁移 nop-job-service + cluster leader 锁或实现 IErpSysLockBiz）

### CloseVoucherWriter 幂等 UK（P1-MA2-087）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 与 deferred P0-MA2-018 同根因——字面 UK 已裁定不可实施（红冲同键/多账套/软删除三重冲突）；当前 period version guard 保护 + successor = P0-MA2-018 deferred plan 方向 A/B/C/D。
- Successor Required: `yes`（P0-MA2-018 解决时自动闭包）

### inv LandedCost 字面 UK（P1-MA2-085 方案A）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 业务允许多 DRAFT（条件唯一），字面 UK 不兼容；application-layer guard 已收口 TOCTOU。
- Successor Required: `no`

## Closure

Status Note: <待执行 + 独立结束审计>

Closure Audit Evidence:

- <待独立结束审计>

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件。
