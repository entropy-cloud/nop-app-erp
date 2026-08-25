# ck-mfg-workorder — manufacturing「工单与报工」切片实现代码检查报告

> 工作项：C4.1。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-manufacturing/erp-mfg-service/src/main/java` 工单与报工切片 49 个手写生产文件——工单链 13 类（Facade `ErpMfgWorkOrderProcessor` + Start/Stop/Resume/Close/ReportCompletion/Approve/Reject/SubmitForApproval/WithdrawApproval/ReverseApprove per-mutation Processor）+ 齐套 `workorder/KitAvailabilityChecker`/`KitAvailabilityResult` + 作业卡链 9 类（`ErpMfgJobCardProcessor` + 7 mutation Processor）+ 领料链 4 类（`AbstractErpMfgMaterialIssueProcessor`/`Confirm`/`ReverseConfirm`/`MaterialIssueStockMoveBuilder`）+ APS 建卡链 3 类（`ErpMfgScheduleToJobCardProcessor` + 2 generate Processor）+ 过账链 4 类（`MfgPostingExecutor`/`ManufacturingIssuePostingDispatcher`+`Provider`/`ProductionVarianceDispatcher`）+ 状态机 Bean 4 类（WorkOrder Document/Approval、JobCard、MaterialIssue）+ entity BizModel 10 类（WorkOrder/JobCard/MaterialIssue/IssueLine/TimeLog/WorkOrderLine/快照族 3/MaterialIssueStockMoveBuilder）+ `dashboard/ErpMfgDashboardBizModel` + 根常量 3 类（`ErpMfgConstants`/`ErpMfgErrors`/`ErpMfgConfigs`）+ `_vfs/nop/batch-task/mfg/jobcard-auto-generate.batch.xml`（D4）。跨域核实：`ErpInvStockMoveGenerateMoveProcessor`（幂等键实证）、`ErpInvStockMoveProcessor#findExisting`、`ErpInvReservationBizModel#consumeReservation`（min 封顶/仓库匹配回退）、`StockMoveRequest#isBusinessLinked`、`module-inventory`/`module-manufacturing` orm.xml（versionProp/orgId/UK）、`app-erp-all/_dump/.../erp-mfg-jobcard-auto-generate.job.yaml`、`erp/mfg/beans/app-service.beans.xml`（接线核对）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。核心链逐文件深读（工单/领料/报工/过账/齐套全读）+ 平台与跨域源码实证（generateMove 幂等短路、consumeReservation 匹配语义、ORM versionProp/orgId）+ arm-index 复用裁决。
> 切片边界：BOM/MRP/CRP/仿真（BomExpander 本体/CostRollupService/MrpEngine/CrpLoadCalculator）归 C4.2；委外/批次追溯/差异分析（Subcontract 族/BatchGenealogy/ProductionVarianceCalculator 本体）归 C4.3。本切片仅涉及其与工单主链的交点（reportCompletion 对 variance 的触发链读了 dispatcher 侧）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P0-CK-mfg-001（D8）完工入库移动单幂等键 (ERP_MFG_WORK_ORDER, wo.code) 使首次部分报工后的所有后续报工静默零入库——WO.completedQuantity 与库存/GL 数量静默分叉

- **控制点**：`app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java#generateCompletionMove`（L397-398 `request.setRelatedBillType(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER); request.setRelatedBillCode(wo.getCode());`——**同一工单的每次报工用完全相同的幂等键**）+ 跨域实证 `app/erp/inv/service/processor/ErpInvStockMoveGenerateMoveProcessor.java#generateMove`（L29-35：`if (request.isBusinessLinked()) { ErpInvStockMove existing = facade.findExisting(...); if (existing != null) return existing; }`）+ `ErpInvStockMoveProcessor#findExisting`（L296-302：仅按 `(relatedBillType, relatedBillCode)` 反查、`findFirstByQuery`，无数量/状态判别）+ `StockMoveRequest` javadoc L18「幂等键：(relatedBillType, relatedBillCode)——同源单重复触发反查已有移动单直接返回」
- **证据**：`reportCompletion` 是设计上的**增量累计**流程——`ErpMfgWorkOrderReportCompletionProcessor` L37-45：`newCompleted = nz(completedQuantity).add(completedQty)`、仅 `newCompleted > planned` 抛 `ERR_OVER_REPORT`、`willFinish = newCompleted >= planned`——报 5/再报 3/再报 2 是标准路径（工单保持 IN_PROCESS 直到达量）。而第二次调用 `generateCompletionMove` 时，inventory 侧 `findExisting` 命中首次的移动单（数量 5）**直接返回已存在单，不为本次 delta（3）建新单**。`generateCompletionMove` 对返回值零检查（L408 `stockMoveBiz.generateMove(request, context);` 返回值丢弃）。测试面：`TestErpMfgCompletionPosting`/`TestErpMfgWorkOrderStateMachine`/`TestErpMfgReservationLifecycle` 每个用例仅调用 `reportCompletion` 一次（单发达量），多次部分报工路径零覆盖。
- **问题**：部分报工 ≥ 2 次的工单：`WO.completedQuantity` 累计正确（如 10），但库存产成品只收到**第一次 delta**（如 5），MANUFACTURING_RECEIPT 凭证与存货估值同样只按第一次 delta 计价；后续每次报工静默无库存写入、无警告、无异常。两套真相源（工单 vs 库存/GL）静默分叉，完工达量后 COMPLETED 状态背书了一个从未入库的数量。这满足 P0 判据「核心业务循环断裂 + 数据不一致风险」：完工入库循环对增量报工流静默失效。
- **建议修复方向**：完工入库幂等键细化——`relatedBillCode` 追加序号/时间戳（对齐 finance `advance cashRepay` 的 `"-{millis}"` 防碰撞范式，如 `wo.code + "-RC-" + seq`），或 mfg 侧为每次报工生成唯一子键；修复时同步核 `ErpMfgMaterialIssueReverseConfirmProcessor.findIssueMove`（`setLimit(1)` 反查）对多移动单工单的语义。领料侧不受影响（一单一移动单，键语义正确）。
- **arm-index 裁决**：新增（grep arm-index「reportCompletion/完工入库 幂等/多次报工」零命中；P2-MA4-004 只登记 reload-after-generateMove 脆弱范式，未覆盖幂等键维度；A2.6a 审状态机未审此跨域契约）。

### P1-CK-mfg-002（D3）reject / reverseApprove 后工单无法重新提交——审批轴与 docStatus 轴双守卫互锁，驳回成为准终态

- **控制点**：`app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java#doReject`（L284-287：仅 `wo.setApproveStatus(rejectTargetStatus())`，**不回写 docStatus**——驳回后 docStatus 停留 SUBMITTED）+ `#doReverseApprove`（L289-294：同样只翻 approveStatus=REJECTED，docStatus 停留 NOT_STARTED）+ `#validateBusinessRulesForSubmit`（L252-254 → `requireStatus(wo, DRAFT)`）+ `ErpMfgWorkOrderDocumentStateMachine#assertCanSubmit`（L46-50：docStatus 侧仅 DRAFT 合法）对照 `ErpMfgWorkOrderApprovalStateMachine#assertCanSubmit`（L44-51：approveStatus 侧 UNSUBMITTED/null/**REJECTED** 合法，javadoc 明示「初始提交或**驳回后重新提交**」）
- **证据**：驳回后重提链：`validateTransitionForSubmit` 检查 approveStatus=REJECTED → 通过（审批轴放行）；随后 `validateBusinessRulesForSubmit` → `documentStateMachine.assertCanSubmit(current=SUBMITTED)` → 抛非法迁移（docStatus 轴拦截）。`withdrawApproval` 也救不回（`assertCanWithdraw` 仅 SUBMITTED approveStatus）。唯一出路是 `cancel`（docStatus=SUBMITTED 仍在 cancel 白名单）→ 作废重建。reverseApprove 后同理（docStatus=NOT_STARTED 非 DRAFT，重提被拦）。
- **问题**：审批轴 Bean 显式声明的「驳回后重新提交」路径在组合层死锁——被驳回的工单只能作废重建（丢失单号/行/快照引用），生产计划员的常规返修循环断裂。审批轴语义（5 态 wf/approve-status 支持 reject→resubmit）与 docStatus 轴固定守卫（submit 仅 DRAFT）在双轴联动写入（`doSubmit` 写双轴、`doReject` 只写单轴）上不对称所致。
- **建议修复方向**：`doReject`/`doReverseApprove` 回写 `docStatus=DRAFT`（对齐 doSubmit 的双轴联动写），或 `validateBusinessRulesForSubmit` 的 docStatus 守卫放宽为 `{DRAFT, SUBMITTED, NOT_STARTED}` 中与 approveStatus=REJECTED 的合法组合。修复时对照 purchase/sales 域同构实现的裁决口径。
- **arm-index 裁决**：新增（A2.6a 工单状态机审查报告（arm-index L684 摘要）列 1 P1 + 3 P2，未登记驳回重提交维度；grep「驳回后重新提交/resubmit」零命中）。

### P1-CK-mfg-003（D8）领料红冲回退闭环断裂——reverseConfirm 只红冲 GL + 反向库存，不回退 WorkOrder.materialCost / WorkOrderLine.actualQuantity / 预留 consumedQuantity

- **控制点**：`app/erp/mfg/service/processor/ErpMfgMaterialIssueReverseConfirmProcessor.java#reverseConfirm`（L20-59：全程只有 ① 红冲凭证 ② 反向移动单 ③ 翻 posted=false/CANCELLED 三步，**零 mfg 侧累计字段回退**）对照正向写入点 `ErpMfgMaterialIssueConfirmProcessor#writebackWorkOrderLineActualQty`（L229 `wol.setActualQuantity(nz(...).add(e.getValue()))`）与 `#applyMaterialCostToWorkOrder`（L256 `wo.setMaterialCost(nz(...).add(materialCostDelta))`）——grep 全 service `setMaterialCost`/`setActualQuantity` 仅此两个增量 writer，无任何 subtract 路径
- **证据**：红冲后：GL WIP 借方被红字凭证冲回、库存余额经 REVERSAL 移动单回滚（两跨域侧闭环正确），但 mfg 侧三组累计字段全部残留：① `WO.materialCost` 虚高 → `recomputeTotals` 使 `totalCost/unitCost` 虚高 → 后续完工入库移动单 `line.setUnitCost(wo.getUnitCost())`（facade L403）以虚高单价入产成品存货 + 差异分析（C4.3）实际材料成本失真；② `WO line.actualQuantity` 残留 → 领料进度虚高；③ `ErpInvReservationLine.consumedQuantity` 不回退 → 预留追踪（reserved−consumed）永久少计，后续完工释放量错。
- **问题**：owner doc `state-machine.md §领料红冲实现注记`（L179）声明的闭环三步（红冲凭证 + 反向移动单 + 翻状态）落了，但「错误确认纠错」的业务语义要求 mfg 侧累计同步回退——GL 净额归零而工单成本字段不为零，业账两面失配（与 finance 侧 P2-CK-fin-015 的「GL 移除但业务侧残留」反向同构，但此处 GL 与库存均正确、mfg 聚合字段单向残留且直接影响后续凭证计价，升 P1）。
- **建议修复方向**：reverseConfirm 增加镜像回退步骤：`wo.materialCost -= aggregateIssueMaterialCost(reversalMove)`（或复用正向聚合对 REVERSAL 移动单流水取负）+ `wol.actualQuantity -=` + 预留消耗回退（inventory 侧需补 un-consume 接口或以负量 consumeReservation 语义裁决）。
- **arm-index 裁决**：新增（A2.6a 摘要声称「领料 reverseConfirm 红冲闭环对称」——其审查范围为 GL/库存两侧，mfg 侧累计字段回退维度未覆盖；grep「materialCost 回退/actualQuantity 回退」零命中）。

### P1-CK-mfg-004（D8/D2）完工入库静默缺失——destWarehouseId/uomId 缺失时 generateCompletionMove 静默 return，工单照常 COMPLETED 但产成品永不入库

- **控制点**：`app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java#generateCompletionMove`（L376-389：`outputLine == null` 或 `destWarehouseId == null` → `return`；`uomId == null`（WO 行与物料主数据均无）→ `return`——**三条静默短路均无 LOG 无异常**）+ 调用方 `ErpMfgWorkOrderReportCompletionProcessor#reportCompletion`（L64 调用后不检查是否真正生成移动单，L76-80 直接翻 COMPLETED）
- **证据**：`ErpMfgWorkOrderLine.destWarehouseId` ORM 非必填（`app-erp-manufacturing.orm.xml` L688 无 mandatory）且 OUTPUT 行本身可为空（工单可无行创建）——DRAFT 提交/审核链（validateBusinessRulesForSubmit/Approve）不校验产出仓/产出行完整性。完工后：`completedQuantity` 记账、状态 COMPLETED、`actualEndDate` 写入，但库存零入库、零 MANUFACTURING_RECEIPT 凭证、预留照常释放——静默断链且不可观测（对照：`snapshotBomOnSubmit` 无 BOM 时有 LOG.warn，此处连 warn 都没有）。
- **问题**：设计 `README.md §工单与库存的关系`「完工入库（move_finished）：产成品入库，生成入库移动单」在产出仓缺失时静默失效。触发条件常见（新建工单漏配入库仓——非必填字段）。用户看到「已完工」，仓库永远等不到货。
- **建议修复方向**：`reportCompletion` 前置校验产出仓存在（缺 `destWarehouseId`/产出行/uom 抛 `NopException` 业务错误码，如 `ERR_COMPLETION_WAREHOUSE_MISSING`）；或最低限度静默短路处补 LOG.error + 通知（对齐 G3 分级）。
- **arm-index 裁决**：新增（grep「destWarehouseId/完工入库 静默」零命中）。

### P1-CK-mfg-005（D2/D8）领料红冲 GL/库存失败吞异常后仍推进终态 CANCELLED+posted=false——重试入口被守卫永久关闭，悬挂不可恢复且无告警

- **控制点**：`app/erp/mfg/service/processor/ErpMfgMaterialIssueReverseConfirmProcessor.java#reverseConfirm`（L25-35 红冲凭证 `catch (Exception e) { LOG.warn/error }`、L40-52 反向移动单同型吞咽——两步失败均继续执行 L56-58 `doReverseConfirm` 翻 `docStatus=CANCELLED + posted=false`）+ `#validateCanReverse`（L70-75：`!Boolean.TRUE.equals(issue.getPosted())` 抛 `ERR_MATERIAL_ISSUE_NOT_POSTED`——**posted=false 后红冲入口永久关闭**）
- **证据**：时序：confirm 成功（GL 凭证 + 库存出库 + posted=true）→ reverseConfirm → GL 红冲失败（如期间锁定 `ERR_PERIOD_CLOSED`）→ 吞异常 → 库存反向成功 → 翻 CANCELLED/posted=false。结果：领料单 CANCELLED 但 GL 借 WIP 凭证**未红冲**（凭证滞留 GL，无源单对应）；库存却已回滚——GL 与库存两面反向失配。反向组合（GL 成功/库存失败）同型。失败通道仅 LOG.warn/error，无 `IErpSysNotificationBiz` 告警（对照 `reportCompletion` 差异失败链有 `dispatchVarianceFailureAlert` G3 通知，L184-201）；正向过账失败有 finance sweep 兜底重试（PENDING → RETRIED），红冲失败**无任何重试通道**且守卫封死了重新进入的可能。
- **问题**：lesson 09（业财过账吞异常悬挂）的红冲方向变体，且比正向更糟：正向悬挂 posted=false 至少可重试，红冲悬挂后单据已终态、入口已封。owner doc `state-machine.md §领料红冲实现注记` 未裁决失败处理，「吞异常保持幂等」注释引用的范式（dispatchIfApplicable）有 sweep 兜底，此处没有。
- **建议修复方向**：任一步失败时中止红冲（抛 NopException 回滚整个 @BizMutation，状态保持 DONE+posted=true 可重试）；或失败仍推进但派发告警通知 + 登记异常工作台（对齐 G3 分级 + finance sweep REVERSAL 重试通道）。
- **arm-index 裁决**：新增（带同族注记）——P1-MA4-007（完工差异吞咽无告警，已修复）同型根因（吞咽无告警致 GL 悬挂），但控制点不同（红冲方向 + 不可重试 + 守卫封口）；grep「reverseConfirm 吞/红冲失败 终态」零命中。

### P2-CK-mfg-006（D5/D3，同型 P1-CK-pur-003 族）通用 CRUD update/delete 无单据状态守卫——工单/作业卡/领料/快照族全实体裸 CrudBizModel

- **控制点**：`app/erp/mfg/service/entity/` 下 `ErpMfgWorkOrderBizModel`/`ErpMfgJobCardBizModel`/`ErpMfgMaterialIssueBizModel`/`ErpMfgMaterialIssueLineBizModel`/`ErpMfgJobCardTimeLogBizModel`/`ErpMfgWorkOrderLineBizModel`/`ErpMfgWorkOrderBomSnapshot(BizModel|LineSnapshot|OperationSnapshot)BizModel` 全部为裸 `CrudBizModel<T>`（15-18 行，零 `defaultPrepareSave/Update/Delete` 覆写）——`save_`/`update_`/`delete_` 通用 mutation 对任何 docStatus 的实体开放
- **证据**：COMPLETED 工单可经 `ErpMfgWorkOrder__update_` 直接改 `completedQuantity/plannedQuantity/materialCost`；DONE 领料单可改 `issuedQuantity`；**快照族三实体可直接手改**——破坏 RC-R1.49 LOCK_AT_CREATION 快照不可变语义（提交时点内容锁定的唯一真相源可被 CRUD 覆写，齐套/差异读侧随之失真）。
- **问题**：同 P1-CK-pur-003/P1-CK-sal-004/P1-CK-inv-005 全域同型——命名动作链的状态守卫可被通用 CRUD 旁路。mfg 站点按约定登记为同型 finding（不复用展开）。
- **建议修复方向**：终态/已过账实体的 update/delete 守卫（`defaultPrepareUpdate` 校验 docStatus 非终态 + posted=false）；快照族建议整体禁用通用 update（只读 + cascade-delete）。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，mfg 站点新增计数）。

### P2-CK-mfg-007（D8）齐套检查与看板聚合无 orgId 过滤——跨组织库存汇总致假齐套，KPI 跨组织混算

- **控制点**：`app/erp/mfg/service/workorder/KitAvailabilityChecker.java#buildBalanceQuery`（L169-173：`q.addFilter(in("materialId", ...))` **仅按物料过滤**，无 `eq("orgId", wo.getOrgId())`）+ `#loadAvailableByMaterial`（L160-166 全仓余额合并）对照 inventory ORM `UK_INV_STOCK_BALANCE_NATURAL (orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)`（`app-erp-inventory.orm.xml` L415，注释 L408「含 orgId 兼容多公司隔离（A2.18）」）；同型站点 `dashboard/ErpMfgDashboardBizModel` 全部查询（`countByDocStatusIn`/`sumCompletedQtyInRange`/`computeOnTimeRate`/`findDelayedWorkOrderAlert`/`getWorkOrderStatusDistribution` 均无 orgId filter，而 UC-MFG-11 断言「看板数据受行级权限约束(只看自己组织)」）
- **证据**：工单带 `orgId`（orm L574），但齐套校验把**所有组织**的同物料余额加总对比需求——A 组织缺料但 B 组织有料时误判 STOCK_RESERVED（假齐套 → 开工后本组织领料失败）；多账套/多组织部署下看板 KPI 跨组织混合。注：预留创建链经 `ReservationCreateRequest.setOrgId(wo.getOrgId())` 走对了组织，与检查口径自相矛盾。
- **问题**：多组织隔离维度（ORM 显式设计）在齐套判定与看板读路径缺失。单组织部署无影响（降 P2 依据）。
- **建议修复方向**：`buildBalanceQuery` 增加 `eq("orgId", wo.getOrgId())`（`check` 已持有 wo）；dashboard 各查询补 orgId 过滤（从 `IUserContext`/context 取当前组织，对齐行级权限声明）。
- **arm-index 裁决**：新增（带同族注记）——与 P2-CK-fin2-007（AR/AP 聚合缺 orgId/acctSchemaId 隔离）同族不同域站点；grep「KitAvailability orgId」零命中。

### P2-CK-mfg-008（D5/D4）ErpMfgBom.consumption（STRICT/WARNING/FLEXIBLE）运行时零消费——README 关键业务规则 3「消耗控制」未落地

- **控制点**：`module-manufacturing/model/app-erp-manufacturing.orm.xml` L201 `consumption` 列 + dict `erp-mfg/consumption`（FLEXIBLE/WARNING/STRICT，L61-64）对照 grep 全 `erp-mfg-service/src/main/java` `getConsumption`/`consumption` **零命中**；领料超耗实际控制点 `ErpMfgMaterialIssueConfirmProcessor#warnIfOverPick`（L156-171）只对**预留未消耗量**比较（全局 config `erp-mfg.over-pick-warning` warn 放行），不对 BOM 需求量；`STOCK_PARTIAL` 强制开工门控用全局 config `erp-mfg.allow-partial-kit-start`（facade L306）非 per-BOM consumption
- **证据**：`README.md §关键业务规则` 3：「消耗控制：consumption（flexible 允许超耗 / warning 超耗警告 / strict 严格按 BOM）」；`UC-MFG-04` 断言 `ErpMfgBom.consumption != STRICT 或 主管权限`。实现中 STRICT BOM 的领料超耗（对 BOM 需求量）不拒绝——超领任意数量仅在有预留时按预留维度 warn。
- **问题**：声明业务规则（声明配置字段）运行时零消费——严格消耗控制的 BOM 约束失效，超耗在 STRICT BOM 下同样放行。属 D4「声明配置/特性未落地」家族 + D5 入参边界（领料无 BOM 需求上限）。
- **建议修复方向**：confirm 路径按 `bom.consumption` 分级：STRICT → 超领（对 WO 行 requiredQuantity 累计）抛错；WARNING → warn；FLEXIBLE → 放行；或 owner doc 显式裁决 Deferred 并从 README 规则表移除。
- **arm-index 裁决**：新增（带注记）——P1-RC-008 行内「baseline 精化」已记载「`ErpMfgBom.consumption` 是 per-BOM 字段运行时零消费，STOCK_PARTIAL 强制开工实际由 allow-partial-kit-start 门控」这一事实，但作为侧注未立案为 finding；RC-R1.48 修复范围（预留写路径）不含本维度。

### P2-CK-mfg-009（D3，疑似需求分歧只登记不裁决）STOCK_RESERVED/STOCK_PARTIAL 状态陷阱——齐套检查后工单不可取消不可关闭，预留无法经 cancel 释放

- **控制点**：`app/erp/mfg/service/statemachine/ErpMfgWorkOrderDocumentStateMachine.java#assertCanCancel`（L134-142：白名单仅 `{DRAFT, SUBMITTED, NOT_STARTED}`）+ `#assertCanClose`（L110-116：仅 `{STOPPED, IN_PROCESS}`）+ `#assertCanCheckAvailability`（L68-72：NOT_STARTED → STOCK_RESERVED/STOCK_PARTIAL 单向，无回边）
- **证据**：approve 时 `createReservations` 已建预留（facade L281）；此后一旦 `checkAvailability` 把工单推到 STOCK_RESERVED/STOCK_PARTIAL，`cancel` 与 `close` 均抛非法迁移——唯一出边是 `start`（开工）。owner doc `material-reservation.md §预留释放场景`「工单取消 → 释放所有预留」在齐套后状态不可达；预留滞留只能靠 start → close（close 不释放，残留边界声明 ① 已裁决 watch-only）绕行。
- **问题**：计划员对已齐套但决定不生产的工单无取消路径（只能开工再结案，流程反直觉且预留全程占用）。代码与 owner doc 迁移树字面一致（树也只画 NOT_STARTED/SUBMITTED → CANCELLED），故登记为设计层缺口，只登记不裁决。
- **建议修复方向**：cancel 白名单补 `STOCK_RESERVED/STOCK_PARTIAL`（未开工语义一致，cancel 已含 releaseReservations 步骤可承接）；需 owner doc `state-machine.md §2` 迁移表同步修订。
- **arm-index 裁决**：新增（A2.6a 审查了迁移矩阵守卫齐全性，未审「齐套后取消不可达」反向可达性；grep「STOCK_RESERVED cancel/齐套 取消」零命中）。

### P2-CK-mfg-010（D3/D5）reverseApprove 无 docStatus 守卫——IN_PROCESS/COMPLETED/CLOSED 工单可被翻 approveStatus=REJECTED 并洗掉审核审计字段

- **控制点**：`app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java#validateTransitionForReverseApprove`（L241-248：仅 `approvalStateMachine.assertCanReverseApprove(approveStatus)`，approveStatus 自 approve 后全程保持 APPROVED——IN_PROCESS/COMPLETED/CLOSED/STOPPED 均通过）+ `#doReverseApprove`（L289-294：翻 REJECTED + `setApprovedBy(null)`/`setApprovedAt(null)`，docStatus 不动）
- **证据**：对生产中/已完工工单调 `ErpMfgWorkOrder__reverseApprove`：守卫全过 → approveStatus=REJECTED + 审核人/时间清空 + docStatus 仍 IN_PROCESS/COMPLETED。产生双轴矛盾态（终态单据 + REJECTED 审批态）；审批链审计字段被洗。终态复活被 `assertCanSubmit`（docStatus 仅 DRAFT）挡住（不升级 P1 的依据），但报工/领料对 approveStatus 无守卫，生产继续在「REJECTED」单上进行。
- **问题**：反审核应在「未开工」内（对照采购/销售域 reverseApprove 均有单据状态前置）。当前实现把审批轴守卫当作了全部守卫。
- **建议修复方向**：`validateTransitionForReverseApprove` 增加 docStatus 白名单（NOT_STARTED 且未建预留/未开工；或至少排除终态与 IN_PROCESS/STOPPED）。
- **arm-index 裁决**：新增（A2.6a 未覆盖双轴组合守卫维度）。

### P2-CK-mfg-011（D7/D2，同型 P2-CK-inv-012/P1-CK-pur-002 族）完工链 REQUIRES_NEW 凭证先于主事务提交——中途失败留下孤儿凭证

- **控制点**：`app/erp/mfg/service/processor/ErpMfgWorkOrderReportCompletionProcessor.java#reportCompletion`（L64 `generateCompletionMove` 中途触发 inventory 侧 DONE + GL 过账（REQUIRES_NEW 独立提交，L70-71 注释自证「其内部 GL 过账用 REQUIRES_NEW 事务」）；其后 L72 `updateEntity`（乐观锁可抛）、L85 `releaseRemainingReservations`（可抛）仍在主事务内）+ 领料链 `ErpMfgMaterialIssueConfirmProcessor#confirm`（L88 `issuePostingDispatcher.dispatchIfApplicable` 为末步，主事务提交失败窗口窄）
- **证据**：主事务回滚时：完工移动单（同事务）回滚，但 MANUFACTURING_RECEIPT GL 凭证（REQUIRES_NEW）已提交 → 凭证无对应移动单；WO 回到 IN_PROCESS，用户重报 → 幂等键（见 P0-CK-mfg-001）命中已回滚前创建的移动单？——否：移动单随主事务回滚消失，重报会重建移动单并再过账一张凭证（前一张孤儿滞留 GL）。触发面：乐观锁冲突（报工并发）/预留释放失败。
- **问题**：同 P2-CK-inv-012（inventory doComplete/approve 同型）——mfg 站点登记。`MfgPostingExecutor` javadoc 明示事务边界钉在 Facade REQUIRES_NEW（processor-extension-pattern 硬规则 1），范式本身是平台裁决，缺陷在「REQUIRES_NEW 之后仍有可失败步骤」的编排顺序。
- **建议修复方向**：把 GL 过账移到主事务末步之后（afterCommit 编排）或接受为已知范式并配对账告警（与 inv-012 联合修复裁决）。
- **arm-index 裁决**：同型登记（P2-CK-inv-012/P1-CK-pur-002 族，mfg 站点新增计数）。

### P3-CK-mfg-012（D5/D6）报工入参无边界——recordWork 负数量直接累计且无上限校验；reportCompletion 负数静默按 0 处理

- **控制点**：`app/erp/mfg/service/processor/ErpMfgJobCardRecordWorkProcessor.java#accumulateQuantities`（L58-63：`nz()` 仅 null 归零，负 `completedQuantity`/`scrappedQuantity` 直接 `add` 累计，无 signum 校验；对卡级/工单级 `plannedQuantity` 均无上限）+ `ErpMfgJobCardProcessor#newLog`（L47-52 同样透传负值）+ `ErpMfgWorkOrderReportCompletionProcessor#reportCompletion`（L34-36：`completedQty == null || signum() < 0` → **静默置 ZERO**，无错误反馈——误输负数得到「成功」的空操作）
- **问题**：输错符号的报工静默生效（卡累计减少）或静默无效（工单侧），无业务错误码提示；超产 config-gate 缺失已由 **P2-MA2-042**（watch-only）登记不复用展开，本条只登记负数/下限维度。
- **建议修复方向**：`recordWork`/`reportCompletion` 前置 `signum() <= 0` 抛业务错误（如 `ERR_INVALID_REPORT_QTY`）。
- **arm-index 裁决**：部分复用注记（超产维度 = P2-MA2-042 已登记；负数/下限维度新增）。

### P3-CK-mfg-013（D4）死配置常量 + cron 键漂移——`erp-mfg.jobcard-auto-generate-cron` 零消费，实际接线键为 nop.job 前缀且默认 cron 非空

- **控制点**：`app/erp/mfg/service/ErpMfgConstants.java` L112 `CONFIG_JOBCARD_AUTO_GENERATE_CRON = "erp-mfg.jobcard-auto-generate-cron"`（grep 全仓 Java 零消费）对照实际接线 `app-erp-all/_dump/nop-app/nop/job/conf/erp-mfg-jobcard-auto-generate.job.yaml`（`enabled: '@cfg:nop.job.erp-mfg-jobcard-auto-generate.enabled|false'` + `cronExpr: '@cfg:nop.job.erp-mfg-jobcard-auto-generate.cron-expr|0 0 1 * * ?'`）+ owner doc `state-machine.md §APS 排程来源建卡`「`erp-mfg.jobcard-auto-generate-cron`（空=不调度）」
- **问题**：文档/常量声明的 cron 键与实际 nop-job 配置键不一致（`erp-mfg.jobcard-auto-generate-cron` vs `nop.job.erp-mfg-jobcard-auto-generate.cron-expr`）；且实现默认 cron 为 `0 0 1 * * ?`（每日 1 点）非空——「空=不调度」语义靠 `enabled=false` 第二层门控兜底成立。运维按 owner doc 键配置将不生效（同型 P3-CK-inv-021/P3-CK-sal-023 家族）。batch.xml 本身接线完整（`mfg.jobcard-auto-generate` → `generatePendingJobCards`，processor 直调验证）。
- **建议修复方向**：删除死常量或对齐键名；owner doc 修正为实际双层门控（enabled + cron-expr）描述。
- **arm-index 裁决**：新增（同型 inv-021/sal-023 家族）。

### P3-CK-mfg-014（D9）Dashboard 无界加载——准时率全表载入 COMPLETED 实体、完工量/趋势全实体内存求和

- **控制点**：`app/erp/mfg/service/dashboard/ErpMfgDashboardBizModel.java#computeOnTimeRate`（L280-295：**无日期窗**全量 `findAllByQuery` COMPLETED 工单载入实体后内存计数）+ `#sumCompletedQtyInRange`（L267-278 全实体载入求和 completedQuantity）+ `#loadCompletedInRange`（L297-303 同型）+ `#findDelayedWorkOrderAlert`（L148-173 全量非终态工单载入后内存过滤 planned<today——过滤条件可下推 QueryBean）
- **问题**：工单量增长后看板 KPI 每次刷新线性成本上升（对照同类 `getWorkOrderStatusDistribution` 已用 DB GROUP BY 正确示范）。计数/求和结果正确，纯性能。同型 P3-CK-pur-011/sal-026/inv-020。
- **建议修复方向**：sum 用 QueryFieldBean 聚合 / count+条件聚合下推；delayed 告警过滤下推（`lt("plannedEndDate", today)` + ne 终态）。
- **arm-index 裁决**：新增（同型家族）。

### P3-CK-mfg-015（D9/D10）findWorkOrderIdsWithJobCards 以 WO 数量作卡行查询 limit——截断致已建卡工单误判 pending，批量任务反复空跑告警噪音

- **控制点**：`app/erp/mfg/service/processor/ErpMfgScheduleToJobCardProcessor.java#findWorkOrderIdsWithJobCards`（L287-290：`q.addFilter(in("workOrderId", ids)); q.setLimit(workOrderIds.size());`——查询对象是 **JobCard 行**（一工单多卡），limit 却取 WO 数）
- **证据**：10 个候选工单各有 3 张卡时，limit=10 只返回前 10 张卡 → 最多覆盖 4 个工单 → 其余 6 个被 `findWorkOrdersPendingJobCards`（L151-165 `alreadyHasJobCards` 判定）误判为待建卡。下游 `generatePendingJobCards` 逐单调 `generateJobCardsFromSchedule`，被其内部幂等守卫（`findJobCardsForWorkOrder` 无 limit 全量 + `ERR_JOB_CARDS_ALREADY_GENERATED`）拦下并 `LOG.warn("erp-mfg-jobcard-auto-gen-failed")`（GeneratePendingJobCardsProcessor L45-47）——**正确性由第二道守卫兜住**（无重复建卡），但每轮批任务对同批工单重复尝试 + warn 噪音 + `@BizQuery` 返回的 pending 列表失真（UI 显示待建卡实际已建）。
- **问题**：limit 语义错配（按工单数截卡行）；批任务成功率统计失真（`success/total` 恒低）。
- **建议修复方向**：去掉 limit（候选集已被 `effectiveLimit` 有界）或改查 distinct workOrderId。
- **arm-index 裁决**：新增（grep「findWorkOrderIdsWithJobCards」arm-index 零命中）。

### P3-CK-mfg-016（D2/D10，同型全域族）currentUserId 宽 catch 返回 null 无日志 + 配置读取宽 catch 静默默认

- **控制点**：`app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java#currentUserId`（L733-743：`catch (Exception e) { return null; }` 无日志——影响 `doApprove` SoD 审计字段 approvedBy）+ `#readBoolConfig`（L505-515 宽 catch 返回默认）+ `ErpMfgMaterialIssueConfirmProcessor#isReservationEnabled/isOverPickWarningEnabled`（L188-204 同型）
- **问题**：同 P3-CK-md-008/pur-010/sal-024/inv-018/fin2-012/fin3-013 全域同型——上下文异常静默吞咽，审计字段（approvedBy）写 null 无可观测信号。
- **建议修复方向**：catch 内补 LOG.warn；同型修复统一裁决。
- **arm-index 裁决**：同型登记（全域族，mfg 站点）。

### P3-CK-mfg-017（D7，同型 P3-CK-pur-013）generatePendingJobCards 单事务逐单吞异常——失败单已保存的卡随外层提交

- **控制点**：`app/erp/mfg/service/processor/ErpMfgScheduleToJobCardGeneratePendingJobCardsProcessor.java#generatePendingJobCards`（L41-48：`for` 内 `catch (Exception e) { LOG.warn }` 继续下一单，但整个批在单个 `@BizMutation` 事务内——失败单此前循环里已 `saveEntity` 的卡 + `updateEntity` 的工单标记留在 session 脏写，随外层一起提交）
- **问题**：同 P3-CK-pur-013 batchApprove 同型——「best-effort 逐单」意图与共享单事务语义不符：失败单的部分写入（如第 3 张卡保存后第 4 张失败）不回滚。
- **建议修复方向**：逐单独立事务（REQUIRES_NEW per WO）或失败时显式 evict/rollback 该单写入。
- **arm-index 裁决**：同型登记（P3-CK-pur-013 族）。

### P3-CK-mfg-018（D10）isInspectionGated 对 bomId 为空的工单恒 false——默认 BOM 的 inspectionRequired 不被门控消费

- **控制点**：`app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java#isInspectionGated`（L422-431：`if (wo.getBomId() == null) return false;`——工单未显式指定 BOM（走默认 BOM 路径，齐套/快照均支持该回退）时直接跳过检验门控，`ErpMfgBom.inspectionRequired=true` 的默认 BOM 失效）
- **问题**：config-gated 完工质检门（`erp-mfg.inspection-gate-enabled`）对默认 BOM 工单不生效。对照 `resolveSnapshotSourceBom`/`resolveBomId` 均有 `findDefaultBomOrNull` 回退，此处缺失。`InspectionTrigger.enforceGate`（qa 侧按产品 FINAL 检验）独立存在可部分兜底，故降 P3。
- **建议修复方向**：bomId 为空时回退 `findDefaultBomOrNull(wo.getProductId())` 再判 inspectionRequired。
- **arm-index 裁决**：新增。

### P3-CK-mfg-019（D8，复用注记 RC-R1.49）AUTO_UPGRADE 读侧 re-resolve 默认 BOM 忽略工单显式 bomId——显式指定非默认 BOM 的工单需求口径被默认 BOM 顶替

- **控制点**：`app/erp/mfg/service/workorder/KitAvailabilityChecker.java#explodeRequirements(ErpMfgWorkOrder, qty)`（L120-137：AUTO_UPGRADE 分支 `findDefaultBomOrNull(wo.getProductId())` 后 `explode(latest.getId(), ...)`——**从不读 `wo.getBomId()`**；LOCK_AT_CREATION 分支读快照（快照来源在提交时对显式 bomId 是正确的））
- **问题**：工单显式绑定 BOM v2（非默认），产品默认 BOM 为 v1 时：默认策略下需求/预留按快照（v2，正确）；切 `erp-mfg.bom-snapshot-strategy=AUTO_UPGRADE` 后齐套与预留创建改按 v1 展开——同一工单两个策略下口径不同，且 AUTO_UPGRADE 违背工单显式 BOM 绑定意图。RC-R1.49 裁决字面为「re-resolve 默认 BOM」（arm-index P1-RC-009 行 + `ErpMfgConfigs` javadoc 同述），显式 bomId 场景未被裁决覆盖。
- **建议修复方向**：AUTO_UPGRADE re-resolve 顺序改为 `wo.getBomId()` 优先（取该 BOM 最新内容），无显式 bomId 才回落默认 BOM；或 owner doc 显式裁决「AUTO_UPGRADE 一律默认 BOM」并登记工单显式绑定失效的残留。
- **arm-index 裁决**：新增（带复用注记——P1-RC-009/RC-R1.49 已裁决 AUTO_UPGRADE 字面语义，本条是其未覆盖的显式 bomId 边界）。

### P3-CK-mfg-020（D5）applyLaborCostToWorkOrder 无工单状态守卫——终态/取消工单仍可被累计人工成本并触发单位成本重算

- **控制点**：`app/erp/mfg/service/processor/ErpMfgJobCardProcessor.java#applyLaborCostToWorkOrder`（L58-70：无 docStatus 守卫——WO 已 COMPLETED/CLOSED/CANCELLED 而作业卡仍 WORK_IN_PROGRESS/SUBMITTED 时，`recordWork` 照常累计 `laborCost` 并 `recomputeTotals` 重算 `unitCost`；`wo == null` 静默 return）
- **问题**：完工后补录工时改写已完工工单的 `unitCost`（该值已被完工入库移动单/凭证消费过）——历史计价漂移；取消工单的卡仍计成本。工单完成与卡完成之间无联动约束（owner doc 已声明 JobCard 级联取消归 successor，但本条是反方向：WO 终态后卡未关）。
- **建议修复方向**：`applyLaborCostToWorkOrder` 前置 `documentStateMachine.isTerminal(wo.getDocStatus())` 拒绝（或 warn + 拒绝）。
- **arm-index 裁决**：新增。

### P3-CK-mfg-021（D6，同型 P2-CK-inv-010 族）mfg 两 PostingEvent 汇率硬编码 ONE + 领料贷方科目 1401 硬编码不可配置（与 WIP 科目可配置不对称）

- **控制点**：`app/erp/mfg/service/posting/ManufacturingIssuePostingDispatcher.java#buildEvent`（L125 `event.setExchangeRate(BigDecimal.ONE)` + L142 `line.put(KEY_INVENTORY_SUBJECT, "1401")` 硬编码进 billData，对照 WIP 借方经 config `erp-mfg.wip-subject-code` 可配置——Provider L90-94）+ `ProductionVarianceDispatcher#buildEvent`（L164 `setExchangeRate(BigDecimal.ONE)`）
- **问题**：外币工单/领料（issue 带 currencyId/exchangeRate 列，orm L1140-1142）过账时源币金额失真（同 P2-CK-inv-010「汇率恒 1」家族 mfg 站点）；贷方存货科目固定 1401 无法按物料类别映射（多科目存货企业全部挤入原材料科目）——GL 映射键 `ACCOUNT_KEY_INVENTORY` 存在但 subjectCode 先行硬编码，映射规则对贷方失效。
- **建议修复方向**：exchangeRate 透传 `issue.getExchangeRate()`；贷方科目改经 GL 映射（accountKey=INVENTORY）解析，billData 不预置 subjectCode。
- **arm-index 裁决**：新增（同族注记 P2-CK-inv-010）。

## 跨域关联影响面注记（不新建 finding）

| 已登记 finding | mfg 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-inv-001`（4 出库策略 locationId 回退误用 warehouseId） | `MaterialIssueStockMoveBuilder#buildLines` L67 `req.setSourceLocationId(line.getLocationId())`——领料行 `locationId` 可空（orm 非必填）时，inventory 侧出库策略回退把仓库 ID 写入库位列，余额维度污染同型传导至 mfg 领料出库移动单 | **同型受影响面确认**：mfg 侧无需新建 finding，修复随 inv-001 落地；mfg 侧可选加固 = 行必填校验 |
| `P1-CK-fin-003`（post() 幂等命中返回 null 与 dispatcher null=失败语义冲突） | `ManufacturingIssuePostingDispatcher#dispatchIfApplicable` L103-105 与 `ProductionVarianceDispatcher#dispatchIfApplicable` L107-109 均为 `if (voucherId != null) markPosted(...)`——幂等命中（凭证已存在）时 posted 永不置 true；领料红冲守卫 `posted=true`（P1-CK-mfg-005 控制点）随之永久封死 | **同型受影响面确认**：全域 8 dispatcher 之外的 2 个 mfg 站点，随 fin-003 三态化修复联动核 |
| `P1-CK-pur-003`（CRUD update 无已审守卫全域同型） | mfg 全实体裸 CrudBizModel（WorkOrder/JobCard/MaterialIssue/行/TimeLog/快照族） | **同型登记为 P2-CK-mfg-006**（快照族额外破坏 LOCK_AT_CREATION 语义，已在 006 内注明） |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：切片 49 文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now/new Date()`=0、`extends RuntimeException/Exception`=0、字典字符串 `==` 比较=0、无手编生成产物（compliance 基线 R4/R5/R7 一致）。
- **乐观锁在位**：mfg 全部实体（含 WorkOrder/MaterialIssue/JobCard/快照族）`versionProp="version"`（orm 逐实体实证）——报工并发（WO completedQuantity 累计）由乐观锁检测冲突（A2.6a 交接 A2.17 的 silent lost-upgrade 前提是版本列在位）。
- **SoD 职责分离在位**：`doApprove` L275 `SoDGuard.assertApproverNotCreator(wo.getCreatedBy(), currentUserId(), ERR_MFG_APPROVER_IS_CREATOR)`——state-machine.md「创建人与审核人不可为同一人」落地。
- **平台 API `QueryBean.addOrderField(name, desc)` 使用正确**：`findReservationLines`/`loadLines` `addOrderField("lineNo", false)`=ASC（ Consumption 顺序无关紧要）、`findCandidateWorkOrders` `addOrderField("id", true)`=DESC（新工单优先，语义选择）——均与第二参 desc 语义相符，非缺陷。
- **P1-MA4-007 修复在位验证**：`reportCompletion` 差异链 catch 分级正确——`isNoStandardCostError`（L175-181）容错 warn、其他失败 `LOG.error + dispatchVarianceFailureAlert`（L184-201，notify 内部再 try/catch 降级）——G3 错误传播分级落地，不重复登记。
- **P1-MA4-008 daoFor 跨域读侧豁免已裁决**：`KitAvailabilityChecker.daoFor(ErpInvStockBalance)`/`ManufacturingIssuePostingDispatcher.daoFor(ErpInvStockMove/Ledger)`/`AcctSchemaResolver` 等站点经 `data-dependency-matrix.md §9` 裁决（inv 子集永久只读豁免 / md 子集可迁移），resolved——不按 D1 反模式登记（orgId 缺失维度除外，见 P2-CK-mfg-007）。
- **预留消耗 min 封顶与仓库匹配回退语义正确**：`ErpInvReservationBizModel#consumeFromLines`（min(请求, 行剩余) 封顶）+ `#matchLines`（material+warehouse 精确优先 → material-only 回退）——mfg `consumeReservations` 用 issue 头仓库（行无仓库列，orm 实证）经回退分支正确匹配；`applyLaborCost` 类 updateBalanceWithRetry 乐观锁在位（RC-R1.48/A4.2.3 验证过）。
- **完工差异重算红冲幂等链在位**：`reverseIfExists`（posted 行存在才红冲 + 吞 `ERR_REVERSE_SOURCE_NOT_FOUND`）→ `deleteByWorkOrder` → `calculateVariances` → `dispatchIfApplicable`（anyUnposted 门 + 全零跳过）——重算不自增生凭证。
- **Dashboard 无死状态消费**（pur-001/sal-002 教训核对）：全部消费状态（IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/COMPLETED/CLOSED/CANCELLED）均有活跃 writer（documentStateMachine 迁移矩阵全覆盖）；`inProcessCount` 含 STOCK_RESERVED 的口径在 javadoc 显式声明。
- **领料红冲入口守卫在位**：`validateCanReverse` posted=true + docStatus=DONE 双守卫（`ERR_MATERIAL_ISSUE_NOT_POSTED`）；confirm 幂等（DONE 短路）+ DRAFT 单源守卫（M4.39 Bean 委托）正确。
- **超产硬拒绝在位**：`reportCompletion` `newCompleted > planned` 抛 `ERR_OVER_REPORT`（P2-MA2-042 已裁决 watch-only 不重复登记）。
- **快照提交复制幂等 + 无 BOM 降级正确**：`snapshotBomOnSubmit` 已存在跳过（reject→resubmit 不重快照）、无 BOM LOG.warn 不阻断、快照经 cascade-delete 随工单清理（orm tagSet 实证）。
- **除零守卫在位**：`recomputeTotals` `completed.signum() != 0` 才除（两处同型实现一致）；`deriveLoadRate` capacity<=0 分支处理。
- **事务边界范式正确**：Processor 族零 `@Transactional`（跟随 Facade `@BizMutation`），对齐 `processor-extension-pattern.md` 硬规则；`MfgPostingExecutor` REQUIRES_NEW 钉在 `IErpFinVoucherBiz` Facade（编排失败隔离的编排顺序问题另见 P2-CK-mfg-011，范式本身合规）。
- **beans 接线完整**：`app-service.beans.xml` 全部 per-mutation Processor + posting 组件注册（抽查 9 个 bean id 全命中）；`_service.beans.xml` 为生成产物未手编。
- **jobcard TRANSFERRED 两死状态已裁决**：`ErpMfgJobCardStateMachine` javadoc + owner doc Deferred 标注（P1-MA2-035 resolved 裁决在位），不重复登记。
- **batch.xml 接线在位**（D4）：`mfg.jobcard-auto-generate.batch.xml` loader 单触发行 → processor `inject('IErpMfgWorkOrderBiz').generatePendingJobCards` → 双层 config 门控（on-schedule + nop-job enabled），无孤立 job（键漂移另见 P3-CK-mfg-013）。
- **领料出库 moveType=OUTGOING 方向语义正确**：`MaterialIssueStockMoveBuilder` L32-35 注释 + state-machine.md 实现约定补注一致（OUTGOING(20) 扣减 / MANUFACTURE(40) 入库），完工入库用 MANUFACTURE 对称正确。
- **`markIssuePosted` 依赖脏跟踪**：P2-MA4-004 watch-only 已登记（不重复）；当前 session 内 managed entity 更随外层提交，行为正确。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `完工入库 幂等键`/`多次报工`/`驳回后重新提交`/`reverseConfirm materialCost`/`destWarehouseId 静默`/`STOCK_RESERVED 取消`/`consumption 零消费`/`findWorkOrderIdsWithJobCards`/`isInspectionGated bomId`/`AUTO_UPGRADE bomId` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记，报告中注记）**：
  - 报工超产 config-gate 缺失 → **P2-MA2-042**（watch-only）——P3-CK-mfg-012 仅登记负数/下限新维度。
  - 作业卡 TRANSFERRED 死状态 → **P1-MA2-035**（resolved，Deferred 裁决在位）——不登记。
  - 完工差异吞咽无告警 → **P1-MA4-007**（resolved，notify 闭环在位验证）——不登记；P1-CK-mfg-005 为红冲方向新控制点（新增带同族注记）。
  - daoFor 跨域读侧 → **P1-MA4-008**（resolved，data-dependency-matrix §9 豁免）——不登记。
  - reload-after-generateMove 脆弱范式 / AcctSchemaResolver 与 IErpMdAcctSchemaBiz 不一致 / markIssuePosted 脏跟踪 / readAmount 静默降级 → **P2-MA4-004**（watch-only）——不登记。
  - 预留写路径 / BOM 快照 → **P1-RC-008 / P1-RC-009**（resolved RC-R1.48/49）——主链在位验证；P2-CK-mfg-008（consumption）与 P3-CK-mfg-019（AUTO_UPGRADE 显式 bomId）为其未覆盖边界的新增（带注记）。
  - CRUD 无守卫 → **P1-CK-pur-003 族**（本 mission）——P2-CK-mfg-006 同型登记。
  - REQUIRES_NEW 凭证先提交 → **P2-CK-inv-012 / P1-CK-pur-002 族**——P2-CK-mfg-011 同型登记。
  - 汇率恒 1 → **P2-CK-inv-010 族**——P3-CK-mfg-021 同族站点。
  - orgId 隔离缺失 → **P2-CK-fin2-007 族**——P2-CK-mfg-007 新域站点（新增）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 1 | P0-CK-mfg-001 |
| P1 | 4 | P1-CK-mfg-002..005 |
| P2 | 6 | P2-CK-mfg-006..011 |
| P3 | 10 | P3-CK-mfg-012..021 |

按主维度：D8×6（001/003/004/007/019 + 005 跨 D2 计 D2）、D3×3（002/009/010）、D5×3（006/008/012 跨 D6/018/020 中 006/008/020 计 D5，012 计 D5）、D2×2（005/016）、D9×2（014/015）、D4×1（013）、D7×2（011/017）、D10×1（015 跨计 D9 后 D10 由 018 承担）、D6×1（021）。（精确主维度归属：001 D8、002 D3、003 D8、004 D8、005 D2、006 D5、007 D8、008 D5、009 D3、010 D3、011 D7、012 D5、013 D4、014 D9、015 D9、016 D2、017 D7、018 D10、019 D8、020 D5、021 D6。）

跨域关联注记 3 项（inv-001 同型受影响面确认 / fin-003 mfg 2 dispatcher 同型受影响面确认 / pur-003 同型登记为 006）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 49 文件中 44 个逐行深读（工单 Facade 751 行全读、ReportCompletion/领料 Confirm+ReverseConfirm/过账 dispatcher 全读、状态机 4 Bean 全读、齐套/建卡/看板全读；快照族/行/TimeLog BizModel 为 15-18 行裸类抽查确认）；平台与跨域源码实证 7 处（`ErpInvStockMoveGenerateMoveProcessor` 幂等短路、`findExisting` 无判别反查、`StockMoveRequest#isBusinessLinked`、`ErpInvReservationBizModel#consumeReservation` min 封顶/匹配回退、`QueryBean.addOrderField` desc 语义沿用 C1.2 校准结论、`app-service.beans.xml` 接线、job yaml 双层门控）；orm 核对（mfg 全实体 versionProp、WorkOrder/Issue/IssueLine 列集、inv StockBalance UK 含 orgId、WorkOrderLine destWarehouseId 非必填）；测试面抽查（5 个 reportCompletion 测试文件确认单发覆盖）。
- **未深查**：`erp-mfg-web` AMIS view.xml 契约 drift（归 C8.2）；`ProductionVarianceAcctDocProvider` 本体与 `ProductionVarianceCalculator`（差异计算归 C4.3，仅读 dispatcher 触发链）；`BomExpander` 展开算法与 `use_multi_level_bom` config 落地（归 C4.2——kit 调用点硬编码 `useMultiLevel=true` 的事实已记录，未定性）；委外链（`MfgSubcontractReversalListener`/SubcontractPostingDispatcher 归 C4.3）；`ErpMfgReportBizModel`（报表切片）；测试代码正确性（52 个测试文件仅用于行为语义交叉验证）；xmeta 层是否有前端侧守卫（如 AMIS 对 destWarehouseId 的必填拦截——P1-CK-mfg-004 若前端强制必填则触发面收窄但后端无守卫的事实不变）。
- **最不确定、建议主 agent 复核**：
  1. **P0-CK-mfg-001**（幂等键吞后续报工）——若产品语义是「一次报满」（UI 只允许单次达量报工），触发面收窄为 P1；但后端 API 契约（增量累计 + willFinish 语义 + 无最小增量守卫）明确支持多次部分报工，且库存侧幂等键设计意图（javadoc「同源单重复触发直接返回」）与工单多次报工语义冲突确凿。建议主 agent 用一次两段报工的集成测试实证库存收货数量。
  2. **P1-CK-mfg-002**（驳回后无法重提交）——若产品预期「驳回即作废重建」（owner doc 迁移表未画驳回→重提边），则降 P2；但审批轴 Bean javadoc 明示「驳回后重新提交」意图，双轴守卫互锁是实现层矛盾。
  3. **P1-CK-mfg-005**（红冲失败吞异常推进终态）——severity 依赖 GL 红冲实际失败频率（期间锁定后红冲是常见运维场景）；且 finance sweep 对 REVERSAL 异常的重试是否最终兜住 GL 侧悬挂（P2-CK-fin-006 已登记重试无源单校验）会影响「不可恢复」的定性——库存侧反向失败则确无兜底。
