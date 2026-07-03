# 2026-07-03-1707-3-quality-recall-event 批次召回事件聚合（登记/审批/目标定位/客户通知/批量退货）

> Plan Status: active
> Last Reviewed: 2026-07-03
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.11；`docs/design/quality/recall.md`；`docs/design/quality/state-machine.md`
> Related: `docs/plans/2026-07-02-0700-1-inventory-trace-chain.md`（批次追溯链 done 1.11）、`docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md`（NCR/CAPA done 2.4）、`docs/plans/2026-07-02-0456-2-sales-return-and-refund.md`（销售退货 done 1.10）
> Mission: erp
> Work Item: 2.11 批次召回事件
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **召回实体不存在、但 NCR 升级字典值已就位**：quality 实体清单（Action/Calibration/Inspection/InspectionLine/InspectionTemplate/InspectionTemplateLine/NonConformance/QualityGoal/Review/RiskRegister/SamplingPlan）**无 `ErpQaRecall`/`ErpQaRecallTarget`**，二者新建。但 `erp-qa/ncr-status` 字典**已含 `ESCALATED_TO_RECALL`（value 35，`module-quality/model/app-erp-quality.orm.xml:50`）**——裁决 D2「给 NCR 加升级标记状态值」的字典部分已落地，NCR→召回升级无须再加字典值。`ErpQaNonConformance.status`（`:273` ext:dict=`erp-qa/ncr-status`）可承载该值。
- **追溯链定位能力就绪**：`IErpInvStockMoveBiz` 提供 `forwardTrace`/`backwardTrace`/`returnTrace`/`batchTrace` 返回 `TraceChainResult`（`module-inventory/erp-inv-dao/.../biz/IErpInvStockMoveBiz.java:71-91`，done 1.11）。召回目标定位（批次→销售出库→客户）经 `batchTrace(batchNo)` 聚合查询。
- **销售退货触发能力就绪**：`IErpSalReturnBiz` + `ErpSalReturnBizModel`（done 1.10，三轴审批 + 反向入库 + 红字过账）。召回批量退货经此标准退货流程编排（召回只登记不直接改库存）。
- **quality 当前无跨域调用（首次引入 quality→inventory + quality→sales）**：`module-quality/erp-qa-service` 下**无 `IErpInv*Biz`/`IErpSal*Biz` 引用**（已核实零命中）。召回定位 + 批量退货须新增 quality-service→erp-inv-dao + erp-sal-dao compile 依赖（真实新增依赖，infra prereq，类比 maintenance→inventory 既定模式）。
- **`ErpQaNonConformance` 关联广度已就绪**：含 `supplierId`（`:274`→ErpMdPartner 关系 `:297`）。NCR 升级召回可关联来源 NCR。（注：本项目 `ErpQaNonConformance` **仅有 supplierId，无 customerId 列**；受影响客户在新增 `ErpQaRecallTarget.partnerId` 上捕获，不由 NCR 承载——Carbon 源模型的 customerId 关联不适用于本仓实体。）
- **BizModel 现状**：无 Recall BizModel；`ErpQaNonConformanceBizModel`（done 2.4）无「升级为召回」动作。`ErpQaErrors`/`ErpQaConstants`/`ErpQaConfigs` 已存在（扩，非新建）。
- **剩余差距**：(1) 无召回事件头/目标实体 + 状态机（OPEN→APPROVED→IN_PROGRESS→CLOSED/CANCELLED）+ 强制审批；(2) 无目标定位（追溯链反查→ErpQaRecallTarget）；(3) 无客户通知必备动作门控；(4) 无 NCR→ESCALATED_TO_RECALL→建召回；(5) 无 APPROVED→批量退货编排；(6) 无追溯未启用报错。

## Goals

- **召回事件建模**：新增 `ErpQaRecall`（code/recallName；triggerType dict `erp-qa/recall-trigger-type` MANUAL/GAUGE_NCR_UPGRADE/BATCH_NCR_UPGRADE/REGULATORY；sourceNcrId 弱指针→NonConformance；materialId/batchId/serialNo 召回对象；rootCause；severityLevel dict `erp-qa/recall-severity` LOW/MEDIUM/HIGH/CRITICAL；businessDate；notifyCustomer；status dict `erp-qa/recall-status`；approveStatus dict `erp-qa/approve-status`）、`ErpQaRecallTarget`（partnerId→受影响客户；batchId/serialNo；salesDeliveryId 弱指针→ErpSalDelivery；shippedQty；notifiedAt/notifiedBy；returnStatus dict `erp-qa/recall-target-return-status` PENDING/NOTIFIED/RETURNED）至 quality ORM。
- **召回状态机 + 强制审批**：`IErpQaRecallBiz` —— `register`（→OPEN）、`submit`/`approve`（OPEN→APPROVED，强制审批；CRITICAL 需高层）、`reject`（→CANCELLED）、`locateTargets`（APPROVED→经追溯链反查生成 ErpQaRecallTarget + →IN_PROGRESS）、`notifyCustomers`（标记 notified + returnStatus=NOTIFIED）、`close`（IN_PROGRESS→CLOSED，门控：`erp-qua.recall-notify-required-to-close` 默认 true 时所有 target returnStatus≠PENDING + notifyCustomer=true）、`cancel`。
- **目标定位（批次聚合查询）**：`locateTargets` 经 `IErpInvStockMoveBiz.batchTrace(batchNo)`（召回对象批次号→聚合全部相关移动单→筛选销售出库类型→关联客户与发货数量）生成 ErpQaRecallTarget。`batchId`(Long FK)→`batchNo`(String) 须经批次主数据解析（类型桥，见 Decision）。若 `erp-inv.trace-chain-enabled=false` 抛 `ErpQaErrors.ERR_TRACE_CHAIN_DISABLED`。
- **NCR 升级召回**：`IErpQaNonConformanceBiz.upgradeToRecall(ncrId)`——NCR.status→ESCALATED_TO_RECALL（字典值已存在）+ 生成 ErpQaRecall（triggerType=GAUGE/BATCH_NCR_UPGRADE + sourceNcrId 关联）。
- **批量退货编排**：APPROVED + 目标定位后，为每个 ErpQaRecallTarget 生成销售退货单（经 `IErpSalReturnBiz` 标准退货流程，召回只登记不直接改余额），target.returnStatus→RETURNED。
- **配置门控**：`erp-qua.recall-require-approval`（默认 true）、`erp-qua.recall-notify-required-to-close`（默认 true）。
- **行为测试覆盖**：召回状态机（含强制审批）；NCR 升级→建召回；目标定位（追溯链反查→Target）；客户通知门控（未通知全 target 不可 close）；批量退货编排；追溯未启用报错。

## Non-Goals

- **追溯链底层**：`trace-chain.md` 已 done（1.11），召回只消费 `batchTrace(batchNo)`。
- **NCR 单点不合格处理**：`state-machine.md` 已 done（2.4），召回复用 NCI 关联广度。
- **销售退货过账**：sales 域标准退货（done 1.10，红字出库/退款凭证），召回触发的退货走既有流程，召回本身不过账（`recall.md §业务规则1`）。
- **召回直接改库存余额**：反模式警示——召回产生的库存变动经标准销售退货移动单（移动单 DONE 写流水/余额），召回只登记编排（`§业务规则2`）。
- **召回财务过账**：召回事件本身不产生会计凭证（`§业务规则1`）。
- **多级审批工作流**：召回本期以单级强制审批简化（CRITICAL 标记需高层，但无引擎驱动多级）。**触发条件**：多级审批引擎需求时。
- **serialNo 单件追溯目标定位**：`batchTrace(batchNo)` 按批次聚合，不覆盖单件 serialNo 维度召回定位。本期召回对象以批次为主，serialNo 维度 config-gated 降级。**触发条件**：单件追溯查询能力（inventory 域）就绪且单件召回需求时（successor）。

## Task Route

- Type: `app-layer design change + implementation`（新增 2 实体至 quality ORM → codegen → 召回状态机 + 目标定位 + NCR 升级 + 批量退货编排；跨域 quality→inventory（追溯查询 I*Biz 只读 R）+ quality→sales（退货 I*Biz 写触发））。
- Owner Docs: `docs/design/quality/recall.md`（召回实体/状态机/裁决 D2/目标定位算法/业务规则/反模式/工作量声明）、`docs/design/quality/state-machine.md`（NCR 状态机 + ESCALATED_TO_RECALL 已有值）、`docs/design/inventory/trace-chain.md`（batchTrace 批次聚合查询）、`docs/architecture/data-dependency-matrix.md`（quality→inventory 只读 + quality→sales 写触发）。
- Skill Selection Basis: ORM 新增 + BizModel + 状态机 + 强制审批 + 跨域 I*Biz（quality→inventory 只读 + quality→sales 写）+ 事务 + 错误码 → 加载 `nop-backend-dev`；ORM 变更 ask-first；测试 `nop-testing`；草案/结束审计 `plan-audit-prompt.md`/`closure-audit-prompt.md`。
- **Decision（批量退货耦合度）**：**选择** 召回 `close` 前为每个 target 同步调 `IErpSalReturnBiz` 生成销售退货单（标准退货流程，召回只登记不直接改余额），单事务。**替代**：召回直接改 StockBalance（违反反模式警示「召回不绕过移动单」，rejected）/ 异步事件退货（最终一致 + 对账复杂度，rejected）。**残留风险**：大批量召回退货单生成耗时（可分批，本期同步）。
- **Decision（目标定位追溯方向）**：**选择** 经 `IErpInvStockMoveBiz.batchTrace(batchNo)`（按批次号聚合全部相关移动单，筛选销售出库 moveType → 客户+发货数量）定位 target——召回从批次起无单一源移动单，`batchTrace` 以批次号为入口聚合最契合。**替代**：`backwardTrace(moveId)`（沿 originMoveId **上溯**至根，方向相反，无法从批次定位下游销售出库，rejected）/ `forwardTrace(moveId)`（需已知单一源移动单起点，召回从批次起无此起点，作为已知源移动单时的辅助非主入口，rejected as primary）。**残留风险**：(1) `batchTrace` 入参为 `batchNo:String`，而 `ErpQaRecall.batchId` 为 Long FK → 须类型桥（经批次主数据解析 batchNo）；(2) `batchTrace` 按批次聚合，**serialNo 单件追溯本期 batchTrace 不覆盖**——serialNo 维度目标定位本期 config-gated 降级（Non-Goal 见下），待单件追溯查询就绪补齐。**设计文档澄清**：`recall.md §目标定位算法` 原文「反向追溯…正向追溯找到销售出库」自相矛盾（反向 ≠ 正向），实施以 `batchTrace` 为准，并在该文档补注澄清。
- **Decision（NCR 升级字典值）**：**选择** 直接用已有 `erp-qa/ncr-status.ESCALATED_TO_RECALL`（value 35，已存在无须加值）。**替代**：无须分析——框架强制/既成事实选择（字典值已按裁决 D2 落地），作为约束记录。**残留风险**：无。

## Infrastructure And Config Prereqs

- **ORM 模型变更（ask-first 保护区域）**：quality ORM 新增 `ErpQaRecall`/`ErpQaRecallTarget` + 字典（`erp-qa/recall-trigger-type`/`recall-severity`/`recall-status`/`recall-target-return-status`）；复用既有 `erp-qa/approve-status`（`:63` 已存在，无须新增）。**新增不改动既有表**（additive）+ codegen。`erp-qa/ncr-status.ESCALATED_TO_RECALL` 已存在，**无须改字典**。
- **新增模块依赖**：`erp-qa-service` compile 依赖 `app-erp-inventory-dao`（`IErpInvStockMoveBiz` batchTrace 只读）+ `app-erp-sales-dao`（`IErpSalReturnBiz` 批量退货写触发）——quality-service 当前无此二依赖（已核实零跨域引用），真实新增。
- 配置项：`erp-qua.recall-require-approval`（默认 true）、`erp-qua.recall-notify-required-to-close`（默认 true）。经 `AppConfig.var(..., defaultValue)`，无 .env。依赖 `erp-inv.trace-chain-enabled`（inventory 既有配置）。
- 无数据迁移；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — 召回事件建模 + 状态机 + 强制审批（quality）+ codegen

Status: planned
Targets: `module-quality/model/app-erp-quality.orm.xml`(扩)、codegen 产物、`ErpQaRecallBizModel.java`(新)、`IErpQaRecallBiz.java`(新)、`ErpQaErrors.java`(扩)、`ErpQaConstants.java`(扩)、`ErpQaConfigs.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 既有 quality ORM（NonConformance）；ncr-status.ESCALATED_TO_RECALL 已存在。

- [ ] `Add`：quality ORM 新增 `ErpQaRecall` + `ErpQaRecallTarget` + 字典（recall-trigger-type/recall-severity/recall-status/recall-target-return-status/approve-status），codegen 生成 CRUD 骨架。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpQaRecallBiz` 状态机——`register`（→OPEN）、`submit`/`approve`（→APPROVED，强制审批 `erp-qua.recall-require-approval`）、`reject`（→CANCELLED）、`cancel`（非终态→CANCELLED）、`close`（IN_PROGRESS→CLOSED，门控 notify）。非法迁移抛 `ErpQaErrors.ERR_INVALID_RECALL_STATUS_TRANSITION`；未审批不可执行 locateTargets。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`TestErpQaRecallStateMachine`（全状态迁移 + 强制审批 + 非法迁移抛错 + CRITICAL 标记）。`mvn test -pl module-quality/erp-qa-service -am -Dtest=TestErpQaRecallStateMachine*`。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 1 交付召回实体 + 状态机。解除 Phase 2（目标定位 locateTargets + close 门控）+ Phase 3（NCR 升级建召回）基线。

- [ ] 召回头/目标实体 + 状态机（含强制审批）单测通过

### Phase 2 — 目标定位（追溯链反查）+ 客户通知门控 + 批量退货编排 + 测试

Status: planned
Targets: `ErpQaRecallBizModel.java`(扩)、`IErpQaRecallBiz.java`(扩)、`RecallTargetLocator.java`(新)、`erp-qa-service/pom.xml`(新 inv-dao + sal-dao 依赖)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（召回实体）；**新增 erp-qa-service→erp-inv-dao + erp-sal-dao compile 依赖**（pom，起步落实）；trace-chain done（1.11）；sales return done（1.10）。

- [ ] `Add`：`erp-qa-service/pom.xml` 新增 `app-erp-inventory-dao` + `app-erp-sales-dao` 依赖。
  - Skill: none
- [ ] `Add`：`IErpQaRecallBiz.locateTargets(recallId)`——`RecallTargetLocator` 经 `IErpInvStockMoveBiz.batchTrace(batchNo)`（`batchId` Long→经批次主数据解析 `batchNo` String→聚合相关移动单→筛选销售出库 moveType→客户+发货数量）生成 ErpQaRecallTarget（PENDING）+ recall→IN_PROGRESS；`erp-inv.trace-chain-enabled=false` 抛 `ErpQaErrors.ERR_TRACE_CHAIN_DISABLED`。serialNo 维度本期 config-gated 降级（Non-Goal）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpQaRecallBiz.notifyCustomers(recallId, targetIds?)`——标记 target.notifiedAt/notifiedBy + returnStatus=NOTIFIED + recall.notifyCustomer=true（severityLevel≥MEDIUM 必备）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpQaRecallBiz.generateReturns(recallId, targetIds?)`——为每个 target 调 `IErpSalReturnBiz` 生成销售退货单（标准退货流程，召回不直接改余额）+ target.returnStatus=RETURNED。
  - Skill: `nop-backend-dev`
- [ ] `Add`：`IErpQaRecallBiz.close(recallId)` 门控——`erp-qua.recall-notify-required-to-close=true` 时所有 target returnStatus≠PENDING + notifyCustomer=true 方可 CLOSED，否则抛 `ErpQaErrors.ERR_RECALL_NOTIFY_INCOMPLETE`。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：批量退货耦合度（同步 IErpSalReturnBiz）+ 目标定位追溯方向（batchTrace），见 Task Route Decision。
  - Skill: none
- [ ] `Proof`：`TestErpQaRecallLocateNotifyReturn`（locateTargets 经 batchTrace 生成 Target；batchId→batchNo 类型桥；追溯未启用抛错；notifyCustomers 标记；generateReturns 生成退货单 + RETURNED；close 门控未通知全 target 抛错；通知后可 close）。`mvn test -pl module-quality/erp-qa-service -am -Dtest=TestErpQaRecallLocateNotifyReturn*`。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付目标定位 + 客户通知门控 + 批量退货编排。完整仓库验证属 Closure Gates。

- [ ] 目标定位（batchTrace + batchId→batchNo 类型桥 + 追溯未启用报错）+ 客户通知门控 + 批量退货编排单测通过
- [ ] erp-qa-service→erp-inv-dao + erp-sal-dao 依赖已落实且 inventory/sales 既有套件无回归

### Phase 3 — NCR 升级召回 + 端到端 + 文档/日志

Status: planned
Targets: `ErpQaNonConformanceBizModel.java`(扩, upgradeToRecall)、`IErpQaNonConformanceBiz.java`(扩)、`docs/logs/2026/{执行当日 month-day}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/quality/recall.md`(偏离补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（召回实体）+ Phase 2（locateTargets/退货）。

- [ ] `Add`：`IErpQaNonConformanceBiz.upgradeToRecall(ncrId)`——NCR.status→ESCALATED_TO_RECALL（字典值已存在）+ 生成 ErpQaRecall（triggerType=GAUGE/BATCH_NCR_UPGRADE + sourceNcrId 关联 + 召回对象继承 NCR 批次/物料）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：端到端 `TestErpQaRecallE2E`（NCR 升级→建召回 OPEN→submit/approve→locateTargets 经追溯链生成 target→notifyCustomers→generateReturns 退货→close 门控通过；手动 MANUAL 触发全链路；severityLevel=CRITICAL 标记）。`mvn test -pl module-quality/erp-qa-service -am -Dtest=TestErpQaRecallE2E*`。
  - Skill: `nop-testing`
- [ ] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.11 标注 done；`recall.md` 补注（目标定位以 `batchTrace` 为准——原文「反向追溯…正向追溯找到销售出库」自相矛盾，澄清为批次聚合查询；召回不过账/不改余额/多级审批/serialNo 单件追溯 Non-Goal）。
  - Skill: none

Exit Criteria:

> Phase 3 交付 NCR 升级召回 + 召回全场景端到端。完整仓库验证属 Closure Gates。

- [ ] NCR 升级→建召回 + 召回全场景端到端单测通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0d8be2f4fffeu2AdI7WBwObh2c`，独立 general 子代理）。1 BLOCKER：(B1) Decision「目标定位追溯方向」误用 `backwardTrace(moveId)`——该方法沿 originMoveId **上溯**至根（dest→origin，:78），而召回算法需从批次定位**下游**销售出库（方向相反），且 plan 自述「正向找销售出库」与方法名自相矛盾，违反规则 9（Decision 须连贯 + 替代/风险准确）致不可执行。正确方法为 `batchTrace(batchNo)`（:88-91 按批次号聚合全部移动单）或 `forwardTrace`。nits：(N1) 基线误称 `ErpQaNonConformance` 有「customerId 等」关联（实仅 supplierId :274，违反规则 1）；(N2) Decision NCR 升级字典值「替代:无」可补约束说明；(N3) approve-status 已存在(:63) 应去掉「若不存在则新增」对冲。**已修订**：目标定位全程改用 `batchTrace(batchNo)`（Goal/Decision/Phase2 item/Proof/Exit/Closure/Owner Docs/Infra/Non-Goal 全部一致）；补 batchId(Long)→batchNo(String) 类型桥残留风险 + serialNo 单件追溯本期 config-gated Non-Goal + 设计文档 recall.md 自相矛盾澄清补注；基线 customerId 误述删除（改注 partnerId 在新增 Target 上捕获）；NCR 升级 Decision 补「框架强制/既成事实」约束说明；approve-status 对冲去掉（复用既有 :63）。
- Independent draft review iteration 2: **accept / consensus**（`ses_0d8b10548ffeLieyYmvrgF6339`，独立 general 子代理）。iter-1 B1 **确认已解决**：经实时仓库核实 `IErpInvStockMoveBiz.batchTrace(batchNo)`(:90-91) 按批次聚合全部移动单（正确定位下游销售出库），`backwardTrace(moveId)`(:77-78) 确为上溯（dest→origin，方向相反）；plan 在全部 10 处操作位置（基线/Goal/Non-Goal/Owner Docs/Decision/Infra/Phase2 item/Proof/Exit/Closure）一致使用 `batchTrace(batchNo)`，`backwardTrace` 仅作为 Decision rejected 替代 + 基线接口枚举 + 历史审查记录出现。残留风险两项（batchId→batchNo 类型桥 + serialNo 单件追溯降级 Non-Goal）诚实标注。3 项 nit 全部修复并交叉核实（customerId 误述已删，approve-status 复用既有 :63 去对冲，serialNo Non-Goal 已加触发条件）。ESCALATED_TO_RECALL(:50)/ncr-status(:46)/status(:273)/supplierId(:274)/关系(:297) 行号准确。反松弛零违规；阶段退出不重复全仓库 build；3 项 Decision 均含选择+替代+残留风险。无新 BLOCKER/回归。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [ ] 范围内行为完成：召回事件状态机（强制审批）+ 目标定位（追溯链 batchTrace + batchId→batchNo 类型桥 + 追溯未启用报错）+ 客户通知门控 + 批量退货编排（IErpSalReturnBiz）+ NCR 升级召回（ESCALATED_TO_RECALL），行为测试通过
- [ ] 相关文档对齐：`extended-roadmap.md` 2.11 done；当日日志已记；`recall.md` Non-Goal 偏离补注
- [ ] 已运行验证：`mvn test -pl module-quality/erp-qa-service -am`（+ inventory/sales 既有套件无回归）；根 `mvn clean install -DskipTests`
- [ ] 无范围内项目静默降级（追溯链底层/NCR 单点/退货过账/召回改余额/召回过账/多级审批 均为计划内 Non-Goal）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 召回直接库存调整（绕过退货移动单）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 反模式警示——召回库存变动经标准销售退货移动单（DONE 写流水/余额），召回只登记编排。
- Successor Required: no

### 召回事件财务过账

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 召回本身不产生会计凭证；触发的退货走 sales 标准红字过账（`recall.md §业务规则1`）。
- Successor Required: no

### 多级召回审批工作流引擎

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期单级强制审批（CRITICAL 标记需高层）；无多级引擎驱动。
- Successor Required: yes（触发条件：多级审批引擎需求时）

## Closure

（待结束后填写）
