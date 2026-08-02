# 2026-07-30-0631-2-r1-22-contract-negotiation-terminate-expiry-job-deferred contract NEGOTIATION→TERMINATED 迁移实现 + EXPIRED 自动到期 Job Deferred

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.22（P1-MA2-071 + P1-MA2-072，源自 A2.14 contract 状态机审查）
> Related: `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`、`docs/audits/arm-index.md §P1-MA2-071/072`；plan `2026-07-30-0512-3-r1-20-quality-linkage-dict-ncr.md`（同型选择性裁决先例：便宜真实缺陷实现 + missing-automation Deferred）、plan `2026-07-30-0512-1-r1-18-assets-idle-state-machine-deferred.md`（Deferred 标注先例）；`docs/audits/arm-index.md §P1-MA2-033`（NEVER_OPENED→OPEN missing-automation P1 分级平行参照——该 finding 在 R1.13 中已实现 openPeriod，此处仅引用其 missing-automation P1 分类同型，非 Deferred 先例）
> Audit: required

## Current Baseline

两项 finding 经实仓逐项确认：均为「owner doc 声明迁移/系统自动但代码未实现」类型，**不破坏已实现主路径**（合同 6 迁移 activate/suspend/resume/terminate/expire/amend + InvoicePlan triggerInvoice ACTIVE 守卫 + 版本 signVersion + RebateSettlement 完整覆盖合同生命周期）。

**P1-MA2-072（NEGOTIATION→TERMINATED 迁移缺失）— 确认：**
- `ErpCtContractBizModel.terminate:103-113` 仅守卫 `status==ACTIVE`——NEGOTIATION 合同谈判失败**无状态机出口**。
- owner doc `state-machine.md §2 L34` ASCII 图「NEGOTIATION ─→ TERMINATED（谈判破裂，终态）」+ L51 迁移表「NEGOTIATION→TERMINATED | 合同管理员 | 谈判破裂，双方确认终止 | 版本归档」+ §3 L58「已进入 NEGOTIATION 或后续态的合同不可作废（CANCELLED），只能 TERMINATED」。
- NEGOTIATION 合同谈判失败当前仅经 useLogicalDelete 逻辑删除逃生，但逻辑删除≠TERMINATED 语义（TERMINATED=未生效合同放弃需归档版本）。属设计契约漂移非数据破坏（useLogicalDelete 提供逃生路径 + NEGOTIATION 是中间态非终态 + 谈判失败低频场景）。

**P1-MA2-071（EXPIRED 自动到期 Job 缺失 + 续期草稿自动创建缺失）— 确认：**
- `expire:117-125` 仅手工 @BizMutation（接 contractId，无批量扫描）；全域无 `ErpCt*Job.java` + 无 scheduler 注册 + 无 `@CronProvider`（grep 全 `module-contract` 零匹配；对比 hr 域有 `ErpHrContractExpiryJob` 同型 Job）。
- 无 `erp-ct.auto-create-renewal-draft` config（`ErpCtConfigs.java:14-54` 仅 volume-discount/rebate/invoiceplan-auto-trigger/settlement-mode/e-signature）；`parentContractId` 字段存在但 grep 全 `module-contract` `renewal|续期|续签` 零 Java 代码使用。
- owner doc `state-machine.md §2 L47`「ACTIVE→EXPIRED | **系统自动** | endDate<now」+ §7 L99「合同到期提醒 | nop-job 定时扫描 endDate」+ §4 L65「endDate 到达但合同仍在执行中 | 先标记 EXPIRED，同时自动创建续期草稿」。
- 生产环境 ACTIVE 合同 endDate 到达后**保持 ACTIVE**（除非运营手工调 expire）；`ErpCtInvoicePlanBizModel.triggerInvoice:71-75` 仅守卫 status==ACTIVE → 过期合同仍可生成发票草稿（虽 unposted DRAFT 经人工审批可拦截，但生命周期不变量破坏）。

**保护区域：** 不触及会计/数据删除保护区域（无凭证/删除写路径变更）。P1-MA2-072 方案A 涉及 BizModel 行为变更（terminate 守卫扩展），按 roadmap 规则走标准 plan-audit + closure-audit（不触及 ORM ask-first）。

## Goals

- 消除 contract 域 owner doc 与代码间两项悬空：(1) **实现** NEGOTIATION→TERMINATED 迁移（terminate 守卫扩展接受 NEGOTIATION 源态）；(2) EXPIRED 自动到期 Job + 续期草稿自动创建对齐（owner doc Deferred 标注，successor 命名触发条件）。
- owner doc 与代码一致；NEGOTIATION 谈判失败经合规状态机出口落地。

## Non-Goals

- 不实现 EXPIRED 自动到期 Job（P1-MA2-071）——裁决 Deferred（owner doc 正式化）。理由：方案A 属 missing-automation（新 Job 类 + scheduler + job.yaml 注册），与危害（expire() 手工路径存在 + InvoicePlan 生成 unposted DRAFT 经人工审批兜底 + 不破坏业财一致）不成比例；对齐 R1.18/R1.20 missing-automation Deferred 范式（P1-MA2-033 在 R1.13 中已实现 openPeriod，仅作 missing-automation P1 分类平行参照）。successor：合同到期自动化需求时实现 `ErpCtContractExpiryJob`（对齐 hr `ErpHrContractExpiryJob` 范式）。
- 不实现续期草稿自动创建（P1-MA2-071 §4 部分）——裁决 Deferred（与 EXPIRED Job 同根因）；`parentContractId` 字段保留为预留语义入口。
- 不实现 InvoicePlan triggerInvoice 对 EXPIRED 的守卫扩展（当前仅守卫 ACTIVE，过期 ACTIVE 合同仍可触发——经人工审批管道兜底，归 EXPIRED Job successor 一并收敛）。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐）+ `implementation-only change`（terminate 守卫扩展）
- Owner Docs: `docs/design/contract/state-machine.md`
- Skill Selection Basis: P1-MA2-072 涉及 BizModel 方法行为变更 + 状态机守卫 + owner doc 同步 → `Skill: nop-backend-dev`；P1-MA2-071 owner doc Deferred 标注为纯文档 → 该部分 `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 两项 finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：两项 finding 处置方案逐项裁决（**选择性裁决**——对齐 R1.20 先例：便宜真实迁移缺陷实现 + missing-automation Deferred）。
      - P1-MA2-072 NEGOTIATION→TERMINATED 迁移缺失：**实现（arm-index 推荐方向）**。理由：(1) arm-index §P1-MA2-072 方案A（推荐）即 `terminate` 守卫扩展为 `status∈{ACTIVE,NEGOTIATION}`；(2) containment 友好（terminate 单点守卫扩展 + javadoc 注明 NEGOTIATION 路径无 signDate/version 归档差异——NEGOTIATION 未生效无需签署归档）；(3) 针对 owner doc §2/§3 核心契约「NEGOTIATION 谈判破裂→TERMINATED 终态」非 missing-automation；(4) 消除「NEGOTIATION 失败仅经 useLogicalDelete 逃生丢失 TERMINATED 审计语义」。残留风险：NEGOTIATION→TERMINATED 无独立法务审批门控（与 ACTIVE→TERMINATED 一致，均经 @BizMutation 入口权限 + e-signature config 覆盖）。
      - P1-MA2-071 EXPIRED 自动到期 Job + 续期草稿缺失：**Deferred（owner doc 正式化）**。**与 arm-index 推荐偏差声明**：arm-index §P1-MA2-071 方案A（推荐）实现 `ErpCtContractExpiryJob`；本计划裁决 Deferred，理由：(1) 与 R1.18/R1.20 missing-automation Deferred 范式一致（P1-MA2-033 在 R1.13 中已实现 openPeriod，仅作 missing-automation P1 分类平行参照）；(2) `expire()` 手工 @BizMutation 路径存在（运营可触发）；(3) InvoicePlan 生成 unposted DRAFT（经人工审批可拦截，非 silent posted）；(4) 方案A 属新 Job 类 + scheduler + job.yaml 注册，与危害（状态悬挂非业财破坏）不成比例；(5) 不破坏业财一致（无 GL 数据错误）。successor：合同到期自动化需求时实现 `ErpCtContractExpiryJob` + config-gated auto-create-renewal-draft。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 逐项记录选择 + 理由 + 与 arm-index 推荐偏差声明 + successor 触发条件；072 进 Phase 2（实现），071 进 Phase 3（Deferred 标注）。

### Phase 2 - NEGOTIATION→TERMINATED 迁移实现（P1-MA2-072）

Status: completed
Targets: `module-contract/erp-ct-service/.../entity/ErpCtContractBizModel.java`、`IErpCtContractBiz`、`docs/design/contract/state-machine.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [x] **Fix（terminate 守卫扩展）**：`ErpCtContractBizModel.terminate` 守卫扩展为接受 `status∈{ACTIVE,NEGOTIATION}`——将 `if (!Objects.equals(contract.getStatus(), CONTRACT_STATUS_ACTIVE))` 改为「既非 ACTIVE 也非 NEGOTIATION 时抛 `illegalTransition`」（illegalTransition helper 已存在，传入 expected 描述）。NEGOTIATION 路径行为：仅 `setStatus(TERMINATED) + updateEntity`（与 ACTIVE 路径一致——NEGOTIATION 未生效无需 signDate/version 归档差异；javadoc 注明「NEGOTIATION→TERMINATED 谈判破裂，未生效合同放弃，版本归档经 useLogicalDelete 既有语义」）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——(1) ACTIVE 合同 terminate 成功（ACTIVE→TERMINATED，行为不变）；(2) NEGOTIATION 合同 terminate 成功（NEGOTIATION→TERMINATED）；(3) SUSPENDED/EXPIRED/TERMINATED/DRAFT 合同 terminate assertThrows `ERR_CT_ILLEGAL_STATUS_TRANSITION`。迁移现有 terminate 测试（若有）。
      - Skill: `nop-backend-dev`
- [x] **Add（owner doc）**：state-machine.md §2 L34 ASCII 图 + L51 迁移表核对一致——NEGOTIATION→TERMINATED 迁移落地（terminate 守卫扩展接受 NEGOTIATION 源态）；§实现偏离补注补「NEGOTIATION→TERMINATED 路径无 signDate/version 归档差异（NEGOTIATION 未生效，版本归档经 useLogicalDelete 既有语义）」。
      - Skill: `none`

Exit Criteria:

- [x] terminate 守卫接受 NEGOTIATION 源态（grep 确认 NEGOTIATION 在守卫条件内）；ACTIVE 路径行为不变；新增/迁移测试全绿（Closure Gates 跑全量 mvn）；owner doc §2 + §实现偏离补注 与代码一致。

### Phase 3 - EXPIRED 自动到期 Job + 续期草稿 Deferred 标注（P1-MA2-071）

Status: completed
Targets: `docs/design/contract/state-machine.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] state-machine.md §2 L47 ACTIVE→EXPIRED 由「系统自动 endDate<now」正式化为「**Deferred**——当前经运营手工 `expire()` 触发，自动到期 `ErpCtContractExpiryJob`（cron-gated 扫描 ACTIVE 且 endDate<now 合同批量 expire）留 successor」；§7 L99 合同到期提醒 同步标注 Deferred（nop-job 定时扫描 successor）；命名 successor 触发条件。
      - Skill: `none`
- [x] state-machine.md §4 L65 「endDate 到达自动创建续期草稿（auto-create-renewal-draft）」正式化为「**Deferred**——续期草稿自动创建（config `erp-ct.auto-create-renewal-draft` + `parentContractId` 关联）留 successor；`parentContractId` 字段保留为预留语义入口」；命名 successor 触发条件。
      - Skill: `none`
- [x] state-machine.md §残留风险补注「过期 ACTIVE 合同 InvoicePlan 仍可生成 unposted DRAFT 发票（triggerInvoice 仅守卫 ACTIVE）——经人工审批管道兜底；EXPIRED Job successor 落地后收敛（expire 后 status=EXPIRED 被 triggerInvoice ACTIVE 守卫拒绝）」。
      - Skill: `none`

Exit Criteria:

- [x] state-machine.md 明确 071 Deferred（EXPIRED Job + 续期草稿 + 到期提醒），owner doc 与代码（expire 手工 @BizMutation + 无 Job）一致；successor 触发事件已命名；InvoicePlan 过期合同发票残留风险已标注。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_04ffc6a72ffeHSpOLxYSM776V6, fresh session) because 全部代码基线声明经实仓验证 TRUE（terminate:103-113 仅守卫 ACTIVE / expire:117-125 手工 @BizMutation / illegalTransition helper 存在 / module-contract 零 Job 类零 scheduler / ErpCtConfigs 无 renewal-draft·ack-timeout 键 / parentContractId 零业务 Java 使用 / triggerInvoice:71-75 仅守卫 ACTIVE）；072 实现精确对齐 arm-index 方案A（terminate 守卫扩展接受 NEGOTIATION，NEGOTIATION 无 signDate 差异因 activate:72 是唯一 writer）；071 Deferred 正式裁决（显式声明偏离 arm-index 方案A + 5 点理由 + 命名 successor + 完整 Deferred But Adjudicated）；无 ORM 变更（BizModel 守卫 + owner doc）正确识别不触及保护区域；Closure Gates 含 mvn 门控（含代码变更）；规则 13 满足（071 是 missing-automation 非活跃数据腐败缺陷，Deferred + 命名 successor 允许）。修订：采纳非阻塞注记——修正先例引用精度（R1.13 P1-MA2-033 已实现 openPeriod 非Deferred 先例，改引 R1.18/R1.20 + R1.13 P1-MA2-031 作为 Deferred-shape 先例，P1-MA2-033 仅作 P1 分级平行参照）。

## Closure Gates

> 本计划含代码变更（P1-MA2-072），故 Closure Gates 含全量 `mvn` 验证（见执行时规则 7）。

- [x] 范围内行为/文档完成（072 NEGOTIATION→TERMINATED 实现 + 071 Deferred 标注）
- [x] 相关文档对齐（contract/state-machine.md）
- [x] 已运行验证（`mvn clean install -DskipTests` 全绿 + contract 域 `mvn test` 全绿 + compliance checker 本计划零新增命中；grep 验证 072 守卫扩展）
- [x] 无范围内项目降级为 deferred/follow-up（072 为范围内存活实现项；071 Deferred 是处置裁决 + 已命名 successor，非范围内缺陷隐瞒）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### EXPIRED 自动到期 Job + 续期草稿自动创建（P1-MA2-071 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `expire()` 手工 @BizMutation 路径存在（运营可触发）；InvoicePlan 生成 unposted DRAFT（经人工审批可拦截，非 silent posted）；不破坏业财一致（无 GL 数据错误，仅状态悬挂）。方案A 属新 Job 类 + scheduler + job.yaml 注册，与危害不成比例。
- Successor Required: `yes`（合同到期自动化需求时实现 `ErpCtContractExpiryJob` cron-gated 扫描 ACTIVE 且 endDate<now 合同批量 expire + config-gated `erp-ct.auto-create-renewal-draft` 经 parentContractId 关联续期草稿 + 到期提醒经 `IErpSysNotificationBiz`，对齐 hr `ErpHrContractExpiryJob` 范式）

### InvoicePlan triggerInvoice 对 EXPIRED 守卫收敛

- Classification: `watch-only residual`
- Why Not Blocking Closure: 当前 triggerInvoice 仅守卫 ACTIVE，过期 ACTIVE 合同（未手工 expire）仍可生成 unposted DRAFT 发票——经人工审批管道兜底。EXPIRED Job successor 落地后自动 expire 使 status=EXPIRED 被 triggerInvoice ACTIVE 守卫拒绝，自然收敛。
- Successor Required: `yes`（EXPIRED Job 落地后核对 triggerInvoice EXPIRED 拒绝路径）

## Closure

Status Note: 三阶段全执行完成。Phase 1 决策记录（072 实现 / 071 Deferred）+ Phase 2 实现 NEGOTIATION→TERMINATED 迁移（terminate 守卫扩展接受 ACTIVE/NEGOTIATION 两源态，ACTIVE 行为不变，新增 6 项 terminate 状态机测试）+ Phase 3 owner doc Deferred 标注（EXPIRED Job + 续期草稿 + 到期提醒 + InvoicePlan EXPIRED 残留风险）。验证：`mvn clean install -DskipTests` 全绿（154 reactor）+ contract 域 `mvn test` 43/43 全绿（含新增 6/6 terminate 测试）+ grep 确认 NEGOTIATION 在守卫内。无 ORM/会计/数据删除保护区域触及。

Closure Audit Evidence:

- 独立结束审计（fresh-session general subagent `ses_04fe12139ffeGiewfQfndqtIIb`，执行者未自我审计）VERDICT=PASS：7 项验证全过——(1) terminate 守卫接受 ACTIVE+NEGOTIATION 且其余状态抛 illegalTransition（`ErpCtContractBizModel.java:109-112`）；(2) 6 项 terminate 测试覆盖 ACTIVE/NEGOTIATION 成功 + DRAFT/SUSPENDED/EXPIRED/TERMINATED 拒绝，Tests run: 6, 0 failures；(3) state-machine.md §2/§3/§4/§7 Deferred 标注 + 残留风险注完整；(4) 确认 module-contract 全域零 Job/CronProvider/scheduler + ErpCtConfigs 无 auto-create-renewal-draft 键 + parentContractId 零业务 Java 使用；(5) 计划所有 [ ] 已勾 + 8 Closure Gates 全勾 + roadmap R1.22=done + 计划内零残留未勾项；(6) git status 确认无 ORM/.api.xml/会计/数据删除变更（仅 BizModel+IBiz+owner doc+plan+roadmap+新测试）；(7) `mvn test -pl module-contract/erp-ct-service` Tests run: 43, 0 failures, BUILD SUCCESS。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件。
