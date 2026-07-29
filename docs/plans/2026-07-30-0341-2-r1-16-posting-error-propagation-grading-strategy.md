# 2026-07-30-0341-2-r1-16-posting-error-propagation-grading-strategy 业财过账错误传播分级策略整体裁决

> Plan Status: active
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.16（P1-MA2-032/048/060/068/074/080 [6 MA2] + P1-MA4-001/004/007/010/013/020 [6 MA4] = 12 findings），源自 A2.5a/A2.16/A4.1a/A4.1b/A4.2a/A4.2b/A4.3/A4.5 状态机与代码质量审查
> Related: `docs/design/finance/posting-log.md`、各域 `depreciation-and-posting.md`/`payroll.md`；plan R1.26（P1-MA4-017 hr 计提/公司承担过账接线，与本计划 P1-MA2-048 catch 侧协同）；plan `2026-07-28-1249-arm-fix-p0-ma2-018-voucher-bill-r-uk.md`（P0-MA2-018 deferred，与本计划 P1-MA2-087 CloseVoucherWriter 互补但本计划不覆盖 R1.28 范围）
> Audit: required

## Current Baseline

12 项 finding 经实仓逐项确认：**同型根因**——过账 dispatcher / 编排层 `catch(Exception)` 过宽吞咽失败 → `posted=false` 悬挂 / GL 缺凭证 / 无告警闭环 / 无自动重试入口 / 部分编排吞咽致期间带病关闭。**不破坏主路径**（AUTO_POST 默认经 @BizMutation 事务回滚覆盖；悬挂需永久性失败前置 + 非正常路径；LOG.warn/error 提供运维可见性；可经期末试算平衡人工发现）。

**统一现状（12 站点 catch-swallow 模式）：**
- finance 引擎：`ErpFinDeferredPostingRetryHelper.incrementRetryAndRethrow:133-136` retryCount≥MAX_RETRY(3) 设 status=**RETRYING**（死状态——sweep loader filter `status=PENDING AND retryCount<3`，RETRYING 永不被重选，无升级 MANUAL、无二次告警）。`ErpFinPostingException` IGNORED 显式放弃态悬挂（P1-MA2-032）。
- finance 期间编排：`ErpFinAccountingPeriodProcessor` 三处跨域集成 `runDepreciation:373` / `recloseInvCosts:395` / `reverseDepreciation` 均 `catch(Exception){ LOG.warn("...跳过") }`——期间正常 CLOSED 但 GL 缺折旧/成本重算凭证。
- mfg 编排：`ErpMfgWorkOrderProcessor.reportCompletion:227-239` 差异计算/过账包裹于单一 `catch(Exception){ LOG.error }`；`SubcontractPostingDispatcher` issue/receipt 段 catch-swallow 且**无 posted 追踪**（仅 fee 段有 posted）。
- assets：`DepreciationPostingDispatcher.tryPost:47-55` `catch(Exception){ LOG.warn/error; return null }` → posted=false，**折旧路径无 DeferredPostingSweepJob 扫描**（sweep 仅扫 finance ErpFinPostingException），无自动重试/告警。Cap/Disposal tryPost 吞咽 + reverseApprove 仅 posted=true 时回滚资产状态（P1-MA2-060）。
- hr：`SalaryPostingDispatcher.tryPostPayment:66/tryPostAccrual:46` catch-swallow → posted=false 悬挂（tryPostAccrual 死代码零调用方，接线归 R1.26）。
- projects：`TimesheetPostingDispatcher` tryPost catch-swallow。
- maintenance：`MaintenanceIssuePostingDispatcher`/`MaintenanceLaborPostingDispatcher` tryPost catch-swallow。
- logistics：`GatewayDispatcher` 网关异常重试耗尽 + DELIVERED 运费过账 catch-swallow，**无 DeferredPostingSweepJob 兜底**（比 peer dispatcher 更严重）。
- inventory：到岸成本 reverse 方向过账悬挂——DeferredPostingSweepJob 不覆盖 reverse 方向（P1-MA4-020）。

**告警通道（已可用）：** `IErpSysNotificationBiz`（`module-notify/erp-notify-dao/.../IErpSysNotificationBiz.java`，已在 cs 域 `ErpCsEntitlementBizModel:57` 注入并 `notify` 派发）。

**保护区域：** 12 项修复均触及**会计保护区域**（业财过账失败恢复 + 期末结账前置检查 + posted 终态语义）。每项修复须独立 plan-audit + closure-audit；期末前置检查扩展 / MANUAL 终态 / catch 收窄须在 owner doc posting-log.md 文档化错误传播分级。

## Goals

- 建立全域统一的**业财过账错误传播分级策略**（owner doc posting-log.md 权威），消除「catch-swallow → 静默悬挂」反模式。
- 12 站点按分级策略统一修复：catch 收窄到具体异常类型 + 永久性失败升级 MANUAL/告警（finance 引擎）+ 编排层区分「impl 未就绪」容错跳过 vs「配置错误/真实故障」阻断/进异常工作台 + 无 sweep 覆盖的域 dispatcher 补 IErpSysNotificationBiz 告警 + 期末结账前置检查扩展覆盖各域 posted=false。
- 每个修复站点可测试（mock post 抛异常 → 断言 posted=false + 告警派发 + 终态行为）。

## Non-Goals

- 不接线 hr 计提/公司承担过账链路（tryPostAccrual 死代码调用方接线归 R1.26）；本计划仅修 tryPostPayment/tryPostAccrual 的 catch-swallow + 告警。
- 不覆盖并发 UK/TOCTOU/cron 幂等缺口（P1-MA2-085~092 归 R1.28）；不覆盖 P0-MA2-018 voucher-bill-r UK（deferred plan）。
- 不重写 DeferredPostingSweepJob 为全域通用（仅在分级策略中裁决哪些域 dispatcher 进 finance 异常工作台、哪些补独立告警）。
- 不补多币种凭证行级断言测试（归 MR2 R2.10~R2.14）；本计划测试聚焦异常路径悬挂 + 告警 + 终态。

## Task Route

- Type: `architecture change`（统一错误传播分级策略）+ `implementation-only change`（12 站点 catch 收窄/告警/终态）
- Owner Docs: `docs/design/finance/posting-log.md`、各域 `depreciation-and-posting.md`/`payroll.md`/`state-machine.md`
- Skill Selection Basis: BizModel/Processor/Dispatcher catch 修改 + IErpSysNotificationBiz 跨域注入 + 期末前置检查扩展 → `Skill: nop-backend-dev`（跨实体调用/事务边界/错误码 + 产品化可定制性自检）；错误传播分级为架构裁决 → `Skill: nop-backend-dev`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.（IErpSysNotificationBiz 通道已就绪；DeferredPostingSweepJob / ErpFinPostingException 异常工作台已存在。）

## Execution Plan

### Phase 1 - 错误传播分级策略裁决 + posting-log.md 框架（Decision）

Status: planned
Targets: `docs/design/finance/posting-log.md`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] **Decision**：定义业财过账失败分级 taxonomy 与处置规则（owner doc posting-log.md 权威记录）。
      - **G1 瞬时可重试**（基础设施抖动/锁竞争）：经 DeferredPostingSweepJob PENDING→retry，保留现有行为。
      - **G2 永久性失败**（科目/模板配置缺失、Provider 固定抛错）：retryCount≥MAX_RETRY 升级 **MANUAL** 终态（非 RETRYING 死状态）+ 派发 IErpSysNotificationBiz 告警（P1-MA4-001）；IGNORED 显式放弃态同样补告警（P1-MA2-032）。
      - **G3 编排层跨域/异步步骤失败**（期间结账/完工触发差异/委外）：catch 收窄——「impl 未就绪」（bizObjectManager 解析失败）容错跳过；「配置错误/真实故障」（NopException ErrorCode）阻断或进 ErpFinPostingException 异常工作台 + 告警（P1-MA4-004/007/010）。
      - **G4 无 sweep 覆盖的域 dispatcher**（assets 折旧[无 sweep] / logistics 网关[无 sweep] / inventory 到岸成本 reverse[sweep 不覆盖 reverse]）：补 IErpSysNotificationBiz 告警 + owner doc 错误处理段标注自愈路径；裁决哪些进 finance 异常工作台由期末前置检查兜底（P1-MA4-013/020 + P1-MA2-060/080）。注：Cap/Disposal（P1-MA2-060）*有* DeferredPostingSweepJob 兜底重试，仅 reverseApprove 不对称需对齐——分级归 G2/G4 之间，Phase 1 裁决时按「有 sweep 但 reverseApprove 不对称」单独处置。
      - **期末结账前置检查扩展**：`findUnresolvedPostingExceptionKeys` 扩展覆盖各域 posted=false（不仅扫 finance 异常工作台）。
      - 替代方案：维持现状（每域独立吞咽）——被拒绝（同型根因反复出现 12 站点证明需统一策略）。残留风险：G4 告警通道可能产生噪声 → 经 config-gated `erp-fin.posting-alert.min-severity` 控制。
      - Skill: `nop-backend-dev`
- [ ] **Add**：posting-log.md 新增「§错误传播分级策略」段（G1-G4 + 处置规则 + 告警通道 + 期末前置检查覆盖矩阵），作为后续 Phase 权威。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] posting-log.md 含可执行的 G1-G4 分级规则；Phase 2-5 严格遵循分级裁决。

### Phase 2 - finance 过账引擎核心（P1-MA4-001 MAX_RETRY + P1-MA2-032 IGNORED + P1-MA4-004 期间编排）

Status: planned
Targets: `ErpFinDeferredPostingRetryHelper.java`、`ErpFinAccountingPeriodProcessor.java`、`docs/design/finance/posting-log.md`、`docs/design/finance/period-close.md`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] **Fix（P1-MA4-001，G2）**：retryCount≥MAX_RETRY 设 status=MANUAL（或新增 FAILED 终态）+ 派发 IErpSysNotificationBiz 告警；移除 RETRYING 死状态语义（或将其重新定义为 MANUAL 过渡）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（P1-MA2-032，G2）**：IGNORED 显式放弃态补 IErpSysNotificationBiz 告警（首次记录已有 dispatchNotify，本项补放弃态告警）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（P1-MA4-004，G3）**：`ErpFinAccountingPeriodProcessor` 三处 catch 收窄——区分「impl 未就绪」（容错跳过）vs「配置错误/真实故障」（NopException→阻断结账或进异常工作台 + 告警）；period-close.md §步骤2/3 标注错误传播分级。
      - Skill: `nop-backend-dev`
- [ ] **Fix（期末前置检查扩展，跨域收口）**：`findUnresolvedPostingExceptionKeys` 扩展覆盖各域 posted=false（assets 折旧 / logistics / inventory reverse / mfg / hr / projects / maintenance），使期末结账前置检查能阻断「域 dispatcher 静默悬挂」的带病关闭。此为 G3/G4 域 dispatcher 修复的统一兜底入口。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：单元测试——mock runDepreciation 抛 NopException(配置错误)→断言期间不 CLOSED 或进工作台 + 告警派发；MAX_RETRY 耗尽→断言 MANUAL + 告警。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] finance 引擎 RETRYING 死状态消除（升级 MANUAL + 告警）；期间编排三处 catch 收窄可测；本阶段为后续 Phase 建立可复用告警 + 工作台 pattern。

### Phase 3 - 无 sweep 覆盖的域 dispatcher 告警（assets 折旧 + inventory 到岸成本 reverse + logistics 网关）

Status: planned
Targets: `DepreciationPostingDispatcher.java`、`ErpInvLandedCostProcessor.java`、`GatewayDispatcher.java`、各域 `depreciation-and-posting.md`/owner doc
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1（+ Phase 2 告警 pattern 已建立）

- [ ] **Fix（P1-MA4-013，G4）**：assets 折旧 posted=failure 派发 IErpSysNotificationBiz 告警 + owner doc depreciation-and-posting.md §错误处理 标注自愈路径（手动重跑 executeDepreciation / reverseDepreciation）；裁决折旧 posted=false 是否进 finance 异常工作台（按 Phase 1 G4 裁决）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（P1-MA2-060，G4）**：Cap/Disposal tryPost 吞咽补告警 + reverseApprove 对称（posted=false 窗口也回滚资产状态或文档化 deliberate 不对称）；assets state-machine.md 标注。
      - Skill: `nop-backend-dev`
- [ ] **Fix（P1-MA4-020，G4）**：inventory 到岸成本 reverse 方向过账悬挂——补告警 + 裁决 reverse 方向是否纳入 sweep/工作台覆盖；owner doc 标注。
      - Skill: `nop-backend-dev`
- [ ] **Fix（P1-MA2-080，G4）**：logistics 网关重试耗尽 + DELIVERED 运费过账吞咽——deadLetter 派发告警 + onDelivered catch 收窄 + scanForPolling 不重试 DELIVERED-PENDING 补告警；owner doc 标注。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：单元测试——mock post/reverse 抛异常→断言 posted=false + 告警派发 + 终态不受影响（或按 G4 裁决阻断）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] assets/inventory/logistics 无 sweep 覆盖的 dispatcher 失败不再静默（告警派发可测）；owner doc 错误处理段与代码一致。

### Phase 4 - 编排层 catch 收窄（mfg 完工差异/委外 + hr salary + projects timesheet + maintenance）

Status: planned
Targets: `ErpMfgWorkOrderProcessor.java`、`SubcontractPostingDispatcher.java`、`SalaryPostingDispatcher.java`、`TimesheetPostingDispatcher.java`、`MaintenanceIssuePostingDispatcher.java`/`MaintenanceLaborPostingDispatcher.java`、各域 state-machine.md/payroll.md
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] **Fix（P1-MA4-007，G3）**：mfg reportCompletion 差异计算/过账 catch 收窄——「无 FIRMED 标准成本」容错跳过 vs 其他 NopException 阻断完工或进工作台 + 告警；state-machine.md §实现偏离补注错误传播分级。
      - Skill: `nop-backend-dev`
- [ ] **Fix（P1-MA4-010，G3）**：mfg 委外 issue/receipt 段引入段级 posted 追踪（或复用 postedStatus 多段）+ 失败进异常工作台/告警。
      - Skill: `nop-backend-dev`
- [ ] **Fix（P1-MA2-048，G3，与 R1.26 协同）**：hr SalaryPostingDispatcher tryPostPayment/tryPostAccrual catch 收窄 + 告警（tryPostAccrual 死代码接线归 R1.26；本项仅修 catch-swallow + 告警，不新增调用方）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（P1-MA2-068，G3）**：projects TimesheetPostingDispatcher tryPost catch 收窄 + 告警。
      - Skill: `nop-backend-dev`
- [ ] **Fix（P1-MA2-074，G3）**：maintenance MaintenanceIssue/Labor PostingDispatcher tryPost catch 收窄 + 告警。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：单元测试——各 dispatcher mock post 抛异常→断言 posted=false + 告警派发 + 终态不受影响。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] mfg/hr/projects/maintenance 编排/dispatcher catch-swallow 消除（catch 收窄 + 告警可测）；tryPostAccrual 调用方接线明确归属 R1.26 不在本计划。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_05094d22dffeFJGXKgxCEz9Lrb) because「期末结账前置检查扩展（findUnresolvedPostingExceptionKeys 覆盖各域 posted=false）」作为 Goal/Decision 子条目存在但无任何执行阶段的 typed Fix 项目 + 复选框——属未归属交付物（规则 10 检查清单完整性风险）；另 P1-MA2-060 G4 分级不精确（Cap/Disposal 有 sweep 兜底，仅折旧无 sweep）、hr/projects/maintenance dispatcher 标 G3 实为功能 G4。基线锚点全绿、12/12 finding 覆盖、规则 14 单一结果表面合理、mvn 在 Closure Gates、会计保护区域纪律 intact。
- Independent draft review iteration 2: accept (ses_050910f52ffehy6w8lcsj5G4uv) after Phase 2 新增 typed Fix 项目「期末前置检查扩展，跨域收口」+ Skill: nop-backend-dev（已归属不可静默丢弃）；Phase 1 G4 注记 Cap/Disposal 有 sweep 兜底仅 reverseApprove 不对称 + 显式枚举无 sweep 域（assets 折旧 / logistics 网关 / inventory reverse）消除 hr/projects/maintenance 歧义。12/12 finding 覆盖、mvn 在 Closure Gates、tryPostAccrual 接线正确归属 R1.26、无禁用词、无降级。

## Closure Gates

- [ ] 范围内行为完成（12 站点 catch-swallow 全部按 G1-G4 分级修复或明确处置）
- [ ] 相关文档对齐（posting-log.md 错误传播分级 + period-close.md + 各域 state-machine/depreciation/payroll）
- [ ] 已运行验证（`mvn clean install -DskipTests` 全绿 + `mvn test` 全绿 + compliance checker 基线不高于 M0；各站点异常路径单元测试通过）
- [ ] 无范围内项目降级为 deferred/follow-up（G4 告警/工作台裁决是处置裁决非降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 会计保护区域变更经独立 closure audit 验证（期末前置检查扩展 / MANUAL 终态 / catch 收窄语义）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### hr tryPostAccrual 死代码调用方接线

- Classification: `moved to explicit successor ownership`
- Why Not Blocking Closure: 本计划修复 tryPostAccrual 的 catch-swallow + 告警（P1-MA2-048 catch 侧）；实际调用方接线（approve→APPROVED 联动 + 公司承担 event 组装）归属 R1.26（P1-MA4-017）。
- Successor Required: `yes`（R1.26 完成 hr 计提/公司承担过账接线）

### 告警通道噪声治理

- Classification: `optimization candidate`
- Why Not Blocking Closure: G4 域 dispatcher 告警可能在批量折旧/物流高并发场景产生噪声；config-gated `erp-fin.posting-alert.min-severity` 为 successor 调节项。
- Successor Required: `yes`（告警量超出运维阈值时启用 severity 过滤）

## Closure

Status Note: _（结束审计后填充）_

Closure Audit Evidence:

- _（独立结束审计后填充）_

Follow-up:

- _（非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件）_
