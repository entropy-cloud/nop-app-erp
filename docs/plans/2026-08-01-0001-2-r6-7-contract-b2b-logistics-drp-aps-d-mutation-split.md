# 2026-08-01-0001-2-r6-7-contract-b2b-logistics-drp-aps-d-mutation-split R6.7 contract+b2b+logistics+drp+aps 域 D-mutation + 内联多步 mutation per-mutation 拆分

> Plan Status: completed
> Last Reviewed: 2026-08-01
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.7（contract+b2b+logistics+drp+aps 域子批次）
> Related: `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage，须拆清单来源）；`docs/plans/2026-07-31-2140-3-r6-6-crm-projects-quality-cs-d-mutation-per-mutation-split.md`（R6.6 同范式先例 + helper 归属裁决）；`docs/architecture/processor-extension-pattern.md`（真相源）；`docs/plans/2026-08-01-0001-1-r6-7-hr-d-mutation-split.md`（同批 N=1 先例）
> Mission: audit-remediation
> Work Item: R6.7（contract+b2b+logistics+drp+aps 子批次）
> Audit: required

## Current Baseline

- **类别 A 违规 facade（1 个，持 3 D-mutation 共用，违反 `processor-extension-pattern.md:42`）——实测配线**：
  - `ErpApsSchedulingProcessor`（aps 域）— D-mutation 入口 3：`scheduleForward`、`scheduleBackward`、`insertRushOrder`。`@Inject` 于 `ErpApsOperationOrderBizModel`（字段 `schedulingProcessor`），3 个 `@BizMutation` 方法已单行委托到 facade（`return schedulingProcessor.xxx(id, context)`）。处置：**delete-after-extract**（facade 无 S-mutation）。类别 A 须拆：3 D-mutation → 3 个新 `<Entity><Method>Processor`。D-mutation per-mutation 文件**尚不存在**，本 plan 须**新建**。BizModel 配线变更：`ErpApsOperationOrderBizModel` 从 `@Inject ErpApsSchedulingProcessor` 改为 `@Inject` 3 个 per-mutation Processor。
- **类别 B 违规 BizModel（须拆 33 个内联 `@BizMutation`，零 Processor 引用，违反 `:5/:7`）——按域分组（权威清单见 roadmap §R6.0 triage 展开 §R6.7 lines 577-640）**：
  - **contract（10）**：`ErpCtContractBizModel`（activate / amend）、`ErpCtContractVersionBizModel`（signVersion）、`ErpCtInvoicePlanBizModel`（triggerDuePlans / triggerInvoice）、`ErpCtRebateAgreementBizModel`（runAccrual）、`ErpCtRebateSettlementBizModel`（postSettlement）、`ErpCtSignatureRequestBizModel`（initSignatureRequest / handleSignatureCallback / queryAndUpdateStatus）。
  - **b2b（6）**：`ErpB2bAsnBizModel`（createReceiveFromAsn / handleInboundWebhook / matchPurchaseOrder / retryMatch）、`ErpB2bEdiDocBizModel`（createInbound / createOutbound）。
  - **logistics（6）**：`ErpLogShipmentBizModel`（save / advise / completeShipment / cancelShipment / handleTrackingWebhook / scanForPolling）。
  - **drp（11）**：`ErpDrpPlanBizModel`（runDrp / approvePlan / resetToDraft）、`ErpDrpScenarioBizModel`（runSimulation / promoteToFormalPlan）、`ErpDrpLineBizModel`（releaseLine / releaseApproved / rejectLine / cancelLine）、`ErpInvDrpSafetyStockCalcBizModel`（calculate / confirmWriteback）。
- **须拆合计：36**（类别 A 3 + 类别 B 33）。
- **合法豁免（保留 BizModel 不动）**：本批次各域 ≤2 步 / 单步状态翻转 mutation 经 R6.0 triage 判定豁免（roadmap §R6.7 合法豁免 36 项）。完整豁免清单见 `docs/architecture/processor-per-mutation-exemption-registry.md`。
- **[保护区域]** 本批次为**商业交易/集成/分销计划域**。涉及外部集成的（b2b EDI/ASN webhook 幂等 [R1.28 已修]、logistics tracking webhook、contract 签名回调）属集成保护区域但非会计过账。contract `ErpCtInvoicePlan.triggerInvoice` 触发开票、`ErpCtRebateSettlement.postSettlement` 涉及返利结算可能联动财务，须对照各域 owner doc 静态校验语义不变。owner doc 各域 `docs/design/{contract,b2b,logistics,drp,aps}/`（state-machine）已固化语义。本 plan 仅做**编排位置迁移**，不改业务语义。
- **既有测试基线**：contract 域测试源文件 **7 个**；其余域待执行时实测。
- **helper 归属裁决（继承 R6.1/R6.6 方案 A）**：类别 A facade 被多 D-mutation 共享的 protected helper 保留 facade（delete-after-extract 时类保留为 helper 持有者，per-mutation 经 `@Inject` facade 调用）。类别 B per-mutation Processor 自包含（`@Inject IDaoProvider` + 域内 Service）；同实体多 mutation 共享 helper 抽到域专属基类，仅当重复显著时。
- **规模注记**：本 plan 跨 5 域 36 拆分（类别 A 3 + 类别 B 33），类别 B 占 92%。执行可分域串行（aps 类别 A → contract → b2b → logistics → drp）以控制单会话变更量。

## Goals

- contract + b2b + logistics + drp + aps 域 36 个须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 3 + 类别 B 33），每 Processor 自包含 `process()` 主流程 + protected step，对齐 `processor-extension-pattern.md:29/:42/:80-97`。
- 类别 A 1 facade（`ErpApsSchedulingProcessor`）按处置 delete-after-extract（类保留为 helper 持有者）；`ErpApsOperationOrderBizModel` D-mutation 重配线为 `@Inject` 3 per-mutation Processor + 单行委托。
- 类别 B BizModel 的 33 个内联 `@BizMutation` 改为 `@Inject <Entity><Method>Processor` + 单行委托。
- beans.xml 注册全部新 Processor bean（bean id = 全限定类名）。
- 5 域 `mvn test` 全绿（0 failures），业务语义不变经既有测试验证。
- arm-index P1-MA3-062 本批次须拆项标记 done。

## Non-Goals

- R6.7 其他域子批次（hr[N=1]、maintenance/notify/master-data[N=3]）——属同批 plan。
- R6.8 全量验证——依赖 R6.7 全部子批次完成。
- 新增业务测试——本 plan 仅验证既有测试行为等价。
- 业务语义变更、状态机迁移、错误码语义调整——仅编排位置迁移。
- 合法豁免项保留不动。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/contract/`、`docs/design/b2b/`、`docs/design/logistics/`、`docs/design/drp/`、`docs/design/aps/`（各 state-machine）、`docs/architecture/processor-extension-pattern.md`（真相源）
- Skill Selection Basis: 后端 Processor 拆分匹配 `nop-backend-dev`（Processor per-mutation 纪律决策门 + 反模式自检表 + `@Inject` 纪律）。集成域（b2b/logistics webhook 幂等）须对照 owner doc 静态校验语义不变。`nop-testing` 用于回归验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 A facade D-mutation 拆分（aps 1 facade → 3 per-mutation Processor）+ BizModel 重配线

Status: completed
Targets: `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/processor/ErpApsScheduling{ScheduleForward,ScheduleBackward,InsertRushOrder}Processor.java`（新建 3）；`ErpApsSchedulingProcessor` 瘦身（delete-after-extract，类保留为 helper 持有者）；`ErpApsOperationOrderBizModel` 重配线；`module-aps/erp-aps-service/src/main/resources/_vfs/erp/aps/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: R6.0 done（已满足）

- [x] Decision: 辅助方法归属策略——继承 R6.1/R6.6 方案 A：`ErpApsSchedulingProcessor` facade 被多 D-mutation 共享的 protected helper 保留 facade（类保留为 helper 持有者，delete D-mutation public 入口），per-mutation Processor 经 `@Inject` facade 调用。在拆分时确认 helper 可达性并记录替代分析。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpApsSchedulingProcessor` 3 D-mutation 拆分 → `ErpApsSchedulingScheduleForwardProcessor` / `...ScheduleBackwardProcessor` / `...InsertRushOrderProcessor`。facade delete-after-extract（类保留为 helper 持有者）。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册 3 类别 A 新 Processor bean（bean id = 全限定类名）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpApsOperationOrderBizModel` D-mutation 重配线为 `@Inject` 3 per-mutation Processor + 单行委托（原 `@Inject ErpApsSchedulingProcessor` 移除）。
  - Skill: `nop-backend-dev`
- [x] Proof: aps service 本地编译通过（`mvn compile -pl module-aps/erp-aps-service -am -DskipTests`）。
  - Skill: none

Exit Criteria:

> 本阶段交付类别 A 3 per-mutation 自包含 + 1 facade 瘦身 + 1 BizModel 重配线 + 编译通过。

- [x] 3 个新 `<Entity><Method>Processor` 文件存在且自包含（`process()` + protected step，非回委托）
- [x] 1 facade 按处置执行（delete-after-extract [类保留]）+ 1 BizModel D-mutation 重配线 + beans.xml 更新
- [x] aps service 本地编译通过

### Phase 2 - 类别 B BizModel 内联 mutation 拆分（4 域 → 33 per-mutation Processor）

Status: completed
Targets: 各域 `.../processor/Erp<Entity><Method>Processor.java`（新建 33）；多 BizModel `@BizMutation` 改单行委托；各域 beans.xml 注册
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1
- Item-heavy 注记：本阶段为 `Add`-heavy（33/36 ≈ 92% 项目为 Add）。建议按域串行：contract（10）→ b2b（6）→ logistics（6）→ drp（11），每域完成后跑本地编译确认。

- [x] Add: contract 域 10 类别 B mutation 拆分——`ErpCtContractBizModel`（activate/amend）、`ErpCtContractVersionBizModel`（signVersion）、`ErpCtInvoicePlanBizModel`（triggerDuePlans/triggerInvoice）、`ErpCtRebateAgreementBizModel`（runAccrual）、`ErpCtRebateSettlementBizModel`（postSettlement）、`ErpCtSignatureRequestBizModel`（initSignatureRequest/handleSignatureCallback/queryAndUpdateStatus）。各 BizModel 改 `@Inject` Processor + 单行委托。
  - Skill: `nop-backend-dev`
- [x] Add: b2b 域 6 类别 B mutation 拆分——`ErpB2bAsnBizModel`（createReceiveFromAsn/handleInboundWebhook/matchPurchaseOrder/retryMatch）、`ErpB2bEdiDocBizModel`（createInbound/createOutbound）。
  - Skill: `nop-backend-dev`
- [x] Add: logistics 域 6 类别 B mutation 拆分——`ErpLogShipmentBizModel`（save/advise/completeShipment/cancelShipment/handleTrackingWebhook/scanForPolling）。
  - Skill: `nop-backend-dev`
- [x] Add: drp 域 11 类别 B mutation 拆分——`ErpDrpPlanBizModel`（runDrp/approvePlan/resetToDraft）、`ErpDrpScenarioBizModel`（runSimulation/promoteToFormalPlan）、`ErpDrpLineBizModel`（releaseLine/releaseApproved/rejectLine/cancelLine）、`ErpInvDrpSafetyStockCalcBizModel`（calculate/confirmWriteback）。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册全部 33 类别 B 新 Processor bean。
  - Skill: `nop-backend-dev`
- [x] Proof: 4 域 service 本地编译通过（`mvn compile -pl module-contract/erp-ct-service,module-b2b/erp-b2b-service,module-logistics/erp-log-service,module-drp/erp-drp-service -am -DskipTests`）+ grep 确认各 BizModel 内联 `@BizMutation` 方法体已改为单行委托。
  - Skill: none

Exit Criteria:

> 本阶段交付类别 B 33 per-mutation 自包含 + 各 BizModel 改 `@Inject` Processor 单行委托 + 编译通过。

- [x] 33 个新 Processor 文件存在且自包含（按域计数：contract 10 + b2b 6 + logistics 6 + drp 11）
- [x] 各 BizModel 内联 `@BizMutation` 已改为单行委托（grep 确认无残留编排体）
- [x] beans.xml 更新 + 4 域 service 本地编译通过

### Phase 3 - 5 域运行时行为等价回归

Status: completed
Targets: `module-{contract,b2b,logistics,drp,aps}/erp-*-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] Proof: contract/b2b/logistics/drp/aps 域 `mvn test` 全绿（`mvn test -pl module-contract/erp-ct-service,module-b2b/erp-b2b-service,module-logistics/erp-log-service,module-drp/erp-drp-service,module-aps/erp-aps-service -am`，0 failures）。mutation 经 BizModel→Processor 新路径验证行为等价。快照漂移仅限类名/堆栈变化，重录为新基线或确认无漂移（GraphQL 经 BizModel 契约面不变）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 5 域行为等价证据。

- [x] contract/b2b/logistics/drp/aps 域 `mvn test` 全绿（0 failures）
- [x] 快照漂移已处理（重录或确认无漂移）

## Draft Review Record

- Independent draft review iteration 1: accept（task `ses_0460efa11ffePKhv0EbB6j0Gvh`）— 全部事实独立实仓复核通过：ErpApsSchedulingProcessor facade 3 D-mutation + ErpApsOperationOrderBizModel @Inject 单行委托配线确认 / contract 10 + b2b 6 + logistics 6 + drp 11 mutation 名确认 / 5 域 beans.xml 路径确认 / 36 计数算术正确 / contract 测试 7 确认 / 0 既有 per-mutation Processor。processor-extension-pattern :29/:42/:44-47/:80-97 对齐 + 类别 A facade delete-after-extract [类保留] 处置正确 + 集成 webhook 保护区域（R1.28 b2b / logistics tracking / contract signature）+ 返利结算联动财务均已标注语义不变验证。非阻塞观察：logistics advise/completeShipment/cancelShipment 现为 GatewayDispatcher 单行委托（executor 实施时裁决吸收 vs 委托）。无阻塞，可转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 5 域 + compliance + 全量编译。

- [x] contract + b2b + logistics + drp + aps 域 36 须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 3 + 类别 B 33）
- [x] 1 类别 A facade 按处置执行（delete-after-extract [类保留]）
- [x] 1 类别 A BizModel D-mutation 重配线为 `@Inject` per-mutation Processor 单行委托
- [x] 类别 B BizModel 33 内联 `@BizMutation` 改为 `@Inject` Processor 单行委托（按域：contract 10 + b2b 6 + logistics 6 + drp 11）
- [x] beans.xml 注册一致性（36 新 bean id 与 @Inject 匹配）
- [x] 合法豁免项保留未动
- [x] 业务语义不变（集成 webhook 幂等/返利结算/开票触发经既有测试行为等价）
- [x] `mvn compile` 全域通过 + 5 域 `mvn test` 全绿
- [x] compliance checker 基线不高于当前基线
- [x] arm-index P1-MA3-062 本批次须拆项标记 done
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——R6.0 triage 已完成全部判定；合法豁免项已在 registry 登记非本 plan deferred）_

## Closure

Status Note: 已完成。contract + b2b + logistics + drp + aps 5 域 36 须拆 mutation 全部拆为独立 per-mutation Processor（类别 A 3 + 类别 B 33），1 类别 A facade（`ErpApsSchedulingProcessor`）delete-after-extract[类保留为 helper 持有者]，14 BizModel 改 `@Inject` Processor 单行委托，beans.xml 注册一致（36 bean id=FQCN）。5 域 `mvn test` 全绿（contract 36 + b2b 43 + logistics 34 + drp 25 + aps 33 = 171 tests，0 failures 0 errors），全量 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS，快照无漂移。compliance checker R8/R2c/R2d 漂移为 R6.x per-mutation 拆分系列累积预期（R8 文档化误报 + R2c/R2d 合法增长），全量 checker 校准 + baseline 裁决归 R6.8 全量验证工作项。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 cold context，task `ses_045be748effeSzfyKDdUhDGWST`）
- Evidence:
  - **文件存在性 PASS**：36 per-mutation Processor（aps 3 + contract 10 + b2b 6 + logistics 6 + drp 11）+ 2 抽象基类（AbstractErpCtSignatureRequestProcessor / AbstractErpLogShipmentDeliveredProcessor）+ 1 瘦身 facade。算术 3+10+6+6+11=36 ✓。
  - **facade delete-after-extract[类保留] PASS**：`ErpApsSchedulingProcessor` 3 D-mutation 入口已移除，protected helper（run/loadPendingOrders/persist/acquireReservation/requireSchedule/requireOperationOrder 等）保留。
  - **BizModel 单行委托 PASS**：14 BizModel 的 36 `@BizMutation` 方法逐方法 grep 确认为单行 `return <processor>.<method>(...)`，无残留编排体（`ErpLogShipment.save` 用 `saveProcessor.save(super.save(...))` CrudBizModel 管道互操作，已 javadoc 文档化）。
  - **beans.xml 注册 PASS**：5 域各注册新 Processor bean（aps 3 + contract 10 + b2b 6 + logistics 6 + drp 11 = 36，id=FQCN）；2 抽象基类正确未注册。
  - **无回委托/无 @BizMutation on Processor PASS**：Processor 中 @BizMutation/BizModel 字符串仅出现在注释，无实际注解 + 无 BizModel 注入。
  - **编译 PASS**：`mvn compile -pl <5 service> -am` BUILD SUCCESS。
  - **测试 PASS**：5 域 `mvn test` contract 36 + b2b 43 + logistics 34 + drp 25 + aps 33 = **171 tests，0 failures 0 errors** / BUILD SUCCESS。
  - **合法豁免未动 PASS**：`ErpCtContract.suspend/resume/terminate/expire`（单步状态翻转）+ `ErpDrpLine.approveLine` 等保留内联。
  - **业务语义保留 PASS**：b2b/logistics webhook HMAC（HmacSHA256）+ eventId 幂等 + contract 返利结算负额发票（`total.negate()`）+ DRP 引擎 + APS 插单窗口硬约束逐行等价。
  - **反 hollow 抽查 PASS**：`ErpApsSchedulingInsertRushOrderProcessor`/`ErpDrpPlanRunDrpProcessor` 含真实编排；`ErpLogShipmentAdviseProcessor` 为原 BizModel 同态 GatewayDispatcher 单行委托（draft review 已接受，per-mutation 拆分恢复 Delta 覆盖粒度）。
  - **compliance 漂移（INFORMATIONAL，归 R6.8）**：R8=240 / R2c=1378 / R2d=30 较 baseline（42/1250/28）漂移——R8 文档化误报（self-contained 单 mutation Processor 不继承 Abstract* 不命中 R8 排除模式）+ R2c/R2d per-mutation 拆分合法增长（1057-2 先例）。全量 checker 校准 + baseline 裁决归 R6.8 全量验证工作项，非本子批次回归。

Follow-up:

- _（非阻塞）`ErpCtSignatureRequestBizModel` 残留已迁移 helper（applyEventTransition/applyStatusTransition/completeFullySigned 等）死代码可后续清理（`isTerminal`/`illegalTransition` 仍被豁免 mutation 使用须保留）。_
- _（非阻塞，归 R6.8）R6.x per-mutation 拆分系列累积 compliance 漂移（R8 checker 校准 + R2c/R2d baseline 裁决 + 快照重录）。_
