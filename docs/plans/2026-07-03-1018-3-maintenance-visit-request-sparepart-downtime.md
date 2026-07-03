# 2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime 设备维护执行（访问/请求状态机 + 备件消耗出库 + 维护计划到期 + 停机记录）

> Plan Status: completed
> Last Reviewed: 2026-07-03
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.7；`docs/design/maintenance/state-machine.md`；`docs/design/maintenance/README.md`
> Related: `docs/plans/2026-07-01-0811-2-inventory-stockmove-bizmodel.md`（StockMove done，备件消耗经 IErpInvStockMoveBiz 出库）
> Mission: erp
> Work Item: 2.7 维护计划/停机/备件消耗
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **maintenance 实体已完备（非新建）**：`ErpMntEquipment`（status `erp-mnt/equipment-status` RUNNING/IDLE/UNDER_MAINTENANCE/DOWN/DECOMMISSIONED；assetId 关联 assets 域；workcenterId 关联 mfg 域）、`ErpMntSchedule`（scheduleType PREVENTIVE/PREDICTIVE/CALIBRATION；frequency/recurrenceType DAILY/WEEKLY/MONTHLY/YEARLY；nextDueDate/isActive）、`ErpMntVisit`（status `erp-mnt/visit-status` DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED；assignedTo/completedBy/visitType PLANNED/RESPONSIVE/result/totalMinutes/scheduleId/posted）、`ErpMntVisitTask`（status `erp-mnt/visit-task-status` PENDING/IN_PROGRESS/COMPLETED/SKIPPED/FAILED）、`ErpMntRequest`（status `erp-mnt/request-status` OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED；requestedBy/acceptedBy）、`ErpMntDowntimeEntry`（equipmentId/startTime/endTime/totalMinutes/reason/relatedJobOrderId）、`ErpMntSparePartUsage`+`Line`（materialId/uoMId/quantity/unitCost/amount/batchNo；头 posted=「库存已出库」/warehouseId）。
- **BizModel 全为空 CRUD 壳**：`ErpMntVisitBizModel`/`ErpMntRequestBizModel`/`ErpMntScheduleBizModel`/`ErpMntSparePartUsageBizModel`/`ErpMntDowntimeEntryBizModel`/`ErpMntEquipmentBizModel` 均**无 `@BizMutation`/`@BizQuery`**——无访问/请求状态机、无设备状态联动、无备件消耗出库、无维护计划到期生成、无停机记录计算。`ErpMntErrors`/`ErpMntConstants`/`ErpMntConfigs` 已存在为空骨架（须扩展，非新建）。
- **maintenance-service 当前无 inventory-dao 依赖**：pom 仅依赖自身 dao/codegen/meta + nop-sys-dao（master-data-dao 经 maintenance-dao 传递可用）；`IErpInvStockMoveBiz` 位于 `erp-inv-dao`，**备件消耗出库须新增 maintenance-service → erp-inv-dao compile 依赖**（真实新增依赖，infra prereq）。
- **StockMove 出库机制就绪**：`IErpInvStockMoveBiz`（done 1.3）跨域既定模式为 `generateMove(StockMoveRequest)`——`relatedBillType` 非空时自动 DRAFT→CONFIRMED→DONE（写不可变流水 + 更新余额 + 释预留）；`confirm()` 仅 DRAFT→CONFIRMED（增预留不扣余额），`complete()` 才扣余额（见 `docs/design/inventory/cross-domain.md`，purchase/sales 均调 `generateMove`）。备件消耗须沿用 `generateMove`（非 create+confirm，后者停在 CONFIRMED 不扣余额）。
- **既有基线类型异常（继承不修）**：`ErpMntVisit.totalMinutes` 与 `ErpMntDowntimeEntry.totalMinutes` 列 stdSqlType=VARCHAR/stdDataType=string（domain 标注 DECIMAL）；本期「无 ORM 变更」故继承此异常，数值以字符串写入，基线注明避免实施者困惑（修复合规后续）。另 `ErpMntDowntimeEntry.relatedJobOrder` 关系 refEntityName 误指向 `ErpMntDowntimeEntry`（自引用 bug，应指制造工单）；列 `relatedJobOrderId`(BIGINT) 可存值但 EQL 导航坏，本期仅存值不依赖导航。
- **设备状态联动当前无机制**：访问 IN_PROGRESS 时设备应置 UNDER_MAINTENANCE、COMPLETED 时恢复（RUNNING/IDLE），当前无钩子。
- **维护计划到期生成当前无机制**：`ErpMntSchedule.nextDueDate` 存在但无到期生成访问（generateDueVisits）+ nextDueDate 推进逻辑。
- **DAG 依赖方向**：maintenance 引用 master-data（物料/职员/仓库/位置/组织）+ assets（设备.assetId 经 ORM refEntityName R）；**maintenance→inventory**（备件出库 I*Biz 写触发，新依赖）；**maintenance→manufacturing 停机通知**为事件驱动（制造域订阅），本期 Non-Goal（避免 maintenance→manufacturing 反向依赖成环）。
- **剩余差距**：(1) 无访问 5 态状态机 + 设备状态联动；(2) 无请求 6 态状态机（受理生成访问）；(3) 无备件消耗→inventory 出库；(4) 无维护计划到期生成访问 + nextDueDate 推进；(5) 无停机记录 totalMinutes 计算。

## Goals

- **维护访问状态机（5 态 + 设备状态联动）**：`IErpMntVisitBiz` 实现 `schedule`（DRAFT→SCHEDULED，校验 assignedTo+计划时间，排程冲突提示）、`start`（SCHEDULED→IN_PROGRESS + 设备→UNDER_MAINTENANCE）、`complete`（IN_PROGRESS→COMPLETED + 设备恢复 RUNNING/IDLE + completedBy/completedAt/totalMinutes）、`cancel`（任意非终态→CANCELLED + 设备恢复）。非法迁移抛 `ErpMntErrors.ERR_INVALID_VISIT_STATUS_TRANSITION`。
- **维护请求状态机（6 态 + 受理生成访问）**：`IErpMntRequestBiz` 实现 `accept`（OPEN→ACCEPTED + 生成维护访问 DRAFT visitType=RESPONSIVE + acceptedBy）、`startRepair`（ACCEPTED→IN_PROGRESS）、`complete`（IN_PROGRESS→COMPLETED）、`reject`（OPEN/ACCEPTED→REJECTED）、`cancel`（OPEN/ACCEPTED→CANCELLED）。
- **备件消耗→inventory 出库**：`IErpMntSparePartUsageBiz.confirm(usageId)`——推进头 docStatus/approveStatus 后，`SparePartIssueService` 按行调 `IErpInvStockMoveBiz.generateMove(StockMoveRequest{relatedBillType=维护领料语义, relatedBillCode=备件消耗单号, 出库 moveType})`（relatedBillType 非空自动 DRAFT→CONFIRMED→DONE，写流水 + 扣余额 + 释预留）+ posted=true（库存已出库）+ totalAmount 聚合；可用量不足抛 inventory 错误回滚。
- **维护计划到期生成访问**：`IErpMntScheduleBiz.generateDueVisits(asOfDate)`——扫描 active 计划 nextDueDate ≤ asOfDate 生成 DRAFT 访问（visitType=PLANNED）+ 推进 nextDueDate（按 recurrenceType/frequency）；经 `erp-mnt.auto-generate-due-visits`（默认 true）门控；可由 nop-job 周期触发或手动调用。
- **停机记录**：`IErpMntDowntimeEntryBiz.record/complete(downtimeId)`——startTime/endTime/totalMinutes 计算；设备 DOWN 状态联动（record→DOWN，complete→恢复）。
- 行为测试覆盖：访问状态机（含设备联动）+ 排程冲突、请求受理生成访问、备件消耗出库 + 余额扣减 + 可用量不足回滚、计划到期生成 + nextDueDate 推进、停机 totalMinutes 计算 + 设备 DOWN 联动。

## Non-Goals

- **停机通知制造域（排产调整）**：`state-machine.md §7` 事件驱动，制造域订阅；需 maintenance 发布事件 + manufacturing 订阅（避免反向依赖成环）。**触发条件**：APS/排产停机窗口联动需求时（successor）。
- **维修费用过账（备件消耗/工时凭证）**：备件消耗本期仅触发 inventory 出库（`generateMove` 扣余额），不生成 finance 维修费用凭证（Visit.posted 字段为「库存已出库」语义，finance 凭证 MAINTENANCE_ISSUE 类型 Non-Goal）。**明知偏离**：`docs/architecture/data-dependency-matrix.md` 将 maintenance→finance S 写（维修领料过账）列为目标架构一部分，本期显式延后。**触发条件**：维修费用业财一体过账需求时。
- **预测性维护（PREDICTIVE，IoT/传感器数据驱动）**：`scheduleType=PREDICTIVE` 数据来源未就绪。**触发条件**：IoT 集成落地时。
- **校准管理（ErpMntCalibration 全流程）**：校准为独立面（量具校准 + 下次校准日期推进）。**触发条件**：计量管理需求时。
- **设备与资产域价值联动（折旧/资本化）**：assets 域负责资产价值，maintenance 仅实物维护；跨域价值联动 Non-Goal。**触发条件**：资产维护影响价值评估时。
- **多级审批工作流**：访问/请求/备件消耗本期以单级 approve 简化。**触发条件**：多级审批需求时。

## Task Route

- Type: `app-layer design change + implementation`（访问/请求状态机 + 设备状态联动 + 备件消耗出库 + 计划到期生成 + 停机记录；纯服务层 + 既有实体，不新增实体/列/字典，不触及 model/*.orm.xml；新增 maintenance-service→erp-inv-dao compile 依赖）。
- Owner Docs: `docs/design/maintenance/state-machine.md`（访问 5 态 + 请求 6 态 + 迁移完整性 + 异常路径 + 外部依赖 + 场景演练）、`docs/design/maintenance/README.md`（maintenance 与 assets 分工）、`docs/architecture/data-dependency-matrix.md`（maintenance→inventory I*Biz 写触发 + maintenance 不反向依赖 manufacturing）。
- Skill Selection Basis: BizModel + 跨实体（访问/任务/请求/计划/设备/备件消耗/停机）+ 状态机 + 跨域（maintenance→inventory 出库 I*Biz）+ 事务 + 错误码 → 加载 `nop-backend-dev`；测试 `nop-testing`；草案/结束审计 `plan-audit-prompt.md`/`closure-audit-prompt.md`。
- **Decision（设备状态联动范围）**：**选择** 访问 start→设备 UNDER_MAINTENANCE / complete→恢复（恢复到访问前快照状态：RUNNING 或 IDLE，记录前置状态）；停机 record→设备 DOWN / complete→恢复。**替代**：不联动设备状态（与 `state-machine.md §1`「设备状态」列矛盾，rejected）。**残留风险**：并发多访问同设备状态覆盖（乐观锁保护）。
- **Decision（备件消耗出库耦合度）**：**选择** maintenance `SparePartUsageBiz.confirm` 同步调 `IErpInvStockMoveBiz.generateMove(StockMoveRequest)`（relatedBillType 非空→自动 DONE 扣余额，与 purchase/sales 跨域模式一致；maintenance→inventory 合法 I*Biz 写触发，单事务）。**替代**：create+confirm（停在 CONFIRMED 不扣余额，违反「库存已出库」语义，rejected）/ 异步事件出库（最终一致 + 对账复杂度，rejected）。**残留风险**：可用量不足时整笔回滚（符合维护领料强一致预期）。
- **Decision（计划到期生成触发）**：**选择** `generateDueVisits(asOfDate)` 暴露为 BizMutation，可由 nop-job 周期调或手动；本期不强制接线 nop-job（接线属部署配置），但提供可调用入口 + `erp-mnt.auto-generate-due-visits` 门控。**替代**：仅 nop-job 内建（测试不可达，rejected）。
- **Decision（请求受理生成访问的 visitType）**：**选择** 请求 accept 生成访问 visitType=RESPONSIVE（报修响应），scheduleId 留空；计划到期生成访问 visitType=PLANNED，scheduleId 关联。**替代**：不区分（与 `erp-mnt/visit-type` 字典语义不符，rejected）。

## Infrastructure And Config Prereqs

- **新增模块依赖**：`erp-mnt-service` compile 依赖 `app-erp-inventory-dao`（`IErpInvStockMoveBiz`，备件消耗出库）。master-data-dao 经 maintenance-dao 传递可用（物料/职员/仓库）；assets-dao 经 maintenance-dao 可用（设备.assetId）。
- 配置项：`erp-mnt.auto-generate-due-visits`（默认 true，计划到期自动生成门控）、`erp-mnt.equipment-status-link-enabled`（默认 true，设备状态联动门控）。经 `AppConfig.var(..., defaultValue)` 读取，无 .env。
- **无 ORM 变更**（不加实体/列/字典）：equipment/schedule/visit/visitTask/request/downtimeEntry/sparePartUsage(+Line) 表列齐备。**故无 ask-first 保护区域门控**（纯服务层 + 既有表 + 新 compile 依赖）。
- 无数据迁移；无新增端口/密钥/外部服务（nop-job 接线为可选部署配置，非本期硬依赖）。

## Execution Plan

### Phase 1 — 维护访问状态机 + 设备状态联动 + 维护请求状态机（受理生成访问）+ 测试

Status: completed
Targets: `module-maintenance/erp-mnt-service/.../entity/ErpMntVisitBizModel.java`(扩)、`IErpMntVisitBiz.java`(扩)、`ErpMntRequestBizModel.java`(扩)、`IErpMntRequestBiz.java`(扩)、`ErpMntEquipmentBizModel.java`(扩, 状态联动)、`EquipmentStatusLinker.java`(新)、`ErpMntErrors.java`(扩)、`ErpMntConstants.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 既有 maintenance 实体 + master-data（职员/设备）。

- [x] `Add`：`IErpMntVisitBiz` 5 态——`schedule`（DRAFT→SCHEDULED，校验 assignedTo+计划时间，排程冲突提示）、`start`（SCHEDULED→IN_PROGRESS + `EquipmentStatusLinker` 设备→UNDER_MAINTENANCE）、`complete`（IN_PROGRESS→COMPLETED + 设备恢复 + completedBy/completedAt/totalMinutes）、`cancel`（非终态→CANCELLED + 设备恢复）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpMntRequestBiz` 6 态——`accept`（OPEN→ACCEPTED + 生成访问 DRAFT visitType=RESPONSIVE + acceptedBy）、`startRepair`（ACCEPTED→IN_PROGRESS）、`complete`（IN_PROGRESS→COMPLETED）、`reject`（OPEN/ACCEPTED→REJECTED）、`cancel`（OPEN/ACCEPTED→CANCELLED）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`EquipmentStatusLinker`——设备状态联动（start→UNDER_MAINTENANCE / complete→恢复前置快照；config-gated `erp-mnt.equipment-status-link-enabled`）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：设备状态联动范围 + 请求受理生成访问 visitType，见 Task Route Decision。
  - Skill: none
- [x] `Proof`：`TestErpMntVisitRequestStateMachine`（访问 happy path DRAFT→SCHEDULED→IN_PROGRESS(设备UNDER_MAINTENANCE)→COMPLETED(设备恢复)；排程冲突提示；cancel 设备恢复；终态不可恢复；请求 accept 生成访问 RESPONSIVE；请求 reject/cancel；非法迁移抛错）。`mvn test -pl module-maintenance/erp-mnt-service -am -Dtest=TestErpMntVisitRequestStateMachine*`。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 1 交付访问/请求状态机 + 设备状态联动。解除 Phase 2 备件消耗（访问关联）+ 计划到期生成（visitType=PLANNED）基线。

- [x] 访问 5 态 + 请求 6 态（受理生成访问）+ 设备状态联动单测通过

### Phase 2 — 备件消耗→inventory 出库 + 维护计划到期生成访问 + 测试

Status: completed
Targets: `ErpMntSparePartUsageBizModel.java`(扩)、`IErpMntSparePartUsageBiz.java`(扩)、`ErpMntScheduleBizModel.java`(扩)、`IErpMntScheduleBiz.java`(扩)、`SparePartIssueService.java`(新)、`ScheduleDueGenerator.java`(新)、`erp-mnt-service/pom.xml`(新 inv-dao 依赖)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（访问关联）；**新增 erp-mnt-service→erp-inv-dao compile 依赖**（pom，起步落实）；StockMove done（1.3）。

- [x] `Add`：`erp-mnt-service/pom.xml` 新增 `app-erp-inventory-dao` 依赖。
  - Skill: none
- [x] `Add`：`IErpMntSparePartUsageBiz.confirm(usageId)`——推进头 docStatus(DRAFT→ACTIVE)/approveStatus 后，`SparePartIssueService` 按行调 `IErpInvStockMoveBiz.generateMove(StockMoveRequest{relatedBillType, relatedBillCode=备件消耗单号, 出库})`（自动 DONE，写流水 + 扣余额）+ posted=true + totalAmount 聚合；可用量不足回滚抛错。
  - Skill: `nop-backend-dev`
  - **Fix（posted 语义回归）**：原 `usage.setPosted(move.getPosted())` 误取 inventory 的财务过账标记（维修费用过账 MAINTENANCE_ISSUE 为本期 Non-Goal，inventory 侧对 `ERP_MNT_SPARE_PART` 联动跳过 SALES_OUTPUT 派发→`move.posted` 恒 false）。修正为 `posted = (move.docStatus == DONE)`（「库存已出库」语义，对齐 plan Non-Goal）；`ErpMntConstants` 补 `STOCK_MOVE_DOC_STATUS_DONE=30` 调用方副本（main 代码不依赖 inventory-service）；inventory 域 `ErpInvConstants` 补 `RELATED_BILL_TYPE_MNT_SPARE_PART` + `InvPostingDispatcher.resolveBusinessType` 跳过该联动（与 PUR_RETURN/SAL_RETURN 同模式，避免误派 SALES_OUTPUT 凭证）。
- [x] `Add`：`IErpMntScheduleBiz.generateDueVisits(asOfDate)`——`ScheduleDueGenerator` 扫描 active 计划 nextDueDate ≤ asOfDate 生成 DRAFT 访问（visitType=PLANNED + scheduleId 关联）+ 按 recurrenceType/frequency 推进 nextDueDate；`erp-mnt.auto-generate-due-visits` 门控。
  - Skill: `nop-backend-dev`
- [x] `Decision`：备件消耗出库耦合度（同步 I*Biz R）+ 计划到期生成触发（暴露入口 + 门控），见 Task Route Decision。
  - Skill: none
- [x] `Proof`：`TestErpMntSparePartAndSchedule`（备件消耗 confirm→`generateMove` 出库移动单 DONE + 余额扣减 + posted + 头 docStatus/approveStatus 推进；可用量不足回滚；计划到期 generateDueVisits 生成 PLANNED 访问 + nextDueDate 推进；门控关闭不生成）。`mvn test -pl module-maintenance/erp-mnt-service -am -Dtest=TestErpMntSparePartAndSchedule*`。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 备件消耗出库（+余额扣减/+可用量不足回滚）+ 计划到期生成（+nextDueDate 推进）单测通过
- [x] erp-mnt-service→erp-inv-dao 依赖已落实且 inventory 既有套件无回归

### Phase 3 — 停机记录（totalMinutes + 设备 DOWN 联动）+ 端到端 + 文档/日志

Status: completed
Targets: `ErpMntDowntimeEntryBizModel.java`(扩)、`IErpMntDowntimeEntryBiz.java`(扩)、`docs/logs/2026/{执行当日}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/maintenance/state-machine.md`(偏离补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（设备状态联动基线）。

- [x] `Add`：`IErpMntDowntimeEntryBiz.record`（设备→DOWN + startTime）/ `complete`（endTime + totalMinutes 计算（数值写入 VARCHAR 列，见 baseline 类型异常）+ 设备恢复）；关联 relatedJobOrderId（可空字段，记录影响工单；不依赖坏掉的 EQL 自引用导航，仅存值）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：端到端 `TestErpMntDowntimeAndE2E`（停机 record→设备 DOWN + complete→totalMinutes + 设备恢复；维护全场景：计划到期生成访问 PLANNED→schedule→start(设备UNDER_MAINTENANCE)→备件消耗出库→complete(设备恢复)；报修请求→accept 生成访问 RESPONSIVE→执行→complete）。`mvn test -pl module-maintenance/erp-mnt-service -am -Dtest=TestErpMntDowntimeAndE2E*`。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.7 标注 done；`maintenance/state-machine.md` 偏离（停机通知制造 Non-Goal + 维修费用过账 Non-Goal + 预测性/校准 Non-Goal）补注。
  - Skill: none

Exit Criteria:

> Phase 3 交付停机记录 + 维护全场景端到端。完整仓库验证属 Closure Gates。

- [x] 停机记录（totalMinutes + 设备 DOWN 联动）+ 维护全场景端到端单测通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0da3161a8ffefESAzr9TVyIUul`，独立 general 子代理）。1 BLOCKER：(B1) 备件消耗出库契约误述——`IErpInvStockMoveBiz.confirm()` 仅 DRAFT→CONFIRMED（增预留不扣余额），余额扣减在 `complete()`；跨域既定模式为 `generateMove(StockMoveRequest)`（relatedBillType 非空自动 DONE 扣余额）。原计划「create+confirm」无法满足「余额扣减」退出标准。nits：totalMinutes/relatedJobOrder 既有基线类型/自引用异常未注、maintenance→finance matrix 偏离未交叉引用、anti-slack「可选」、confirm 头状态推进未述。**已修订**：备件消耗全程改用 `generateMove(StockMoveRequest)`；baseline 补 totalMinutes VARCHAR 类型异常 + relatedJobOrder 自引用 bug 注明；Non-Goal 维修费用过账交叉引用 data-dependency-matrix 明知偏离；「可选」改「可空字段」；confirm 补 docStatus/approveStatus 推进。
- Independent draft review iteration 2: **accept / consensus**（`ses_0da1bae59ffef61yu1sG2J9LXw`，独立 general 子代理）。iter-1 B1 **确认已解决**：备件消耗全程一致改用 `generateMove(StockMoveRequest)`（relatedBillType 非空自动 DONE 扣余额），经核实 `IErpInvStockMoveBiz.java:28-29` 契约 + `cross-domain.md` purchase/sales 模式；无「create+confirm」残留。所有实体/字典/BizModel 空壳/pom 无 inventory-dao 依赖/totalMinutes VARCHAR 异常/relatedJobOrder 自引用 bug 注明/maintenance→finance matrix 明知偏离交叉引用/docStatus 推进/各 Decision 选择+替代+残留风险/mvn 命令/Closure Gates 均 clean。无新 BLOCKER。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：访问 5 态 + 请求 6 态（受理生成访问）+ 设备状态联动 + 备件消耗出库 + 计划到期生成 + 停机记录，行为测试通过
- [x] 相关文档对齐：`extended-roadmap.md` 2.7 done；当日日志已记；`maintenance/state-machine.md` Non-Goal 偏离补注
- [x] 已运行验证：`mvn test -pl module-maintenance/erp-mnt-service -am`（22 tests/0 failures，inventory/finance 既有套件无回归）；根 `mvn clean install -DskipTests`（146 模块 BUILD SUCCESS）；全仓 `mvn test` BUILD SUCCESS（无下游回归）
- [x] 无范围内项目静默降级（停机通知制造/维修费用过账/预测性/校准/资产价值联动/多级审批 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 停机通知制造域（排产调整）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 事件驱动，制造域订阅；需 maintenance 发布事件 + manufacturing 订阅（避免反向依赖成环）。
- Successor Required: yes（触发条件：APS/排产停机窗口联动需求时）

### 维修费用过账（备件消耗/工时凭证）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 备件消耗本期仅触发 inventory 出库；finance 维修费用凭证（MAINTENANCE_ISSUE）属业财一体面。
- Successor Required: yes（触发条件：维修费用业财一体过账需求时）

### 预测性维护（IoT）/ 校准管理全流程 / 设备-资产价值联动 / 多级审批

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 各为独立深化面（IoT 未就绪 / 校准独立面 / 价值归 assets / 审批本期单级）。
- Successor Required: yes（触发条件：对应需求落地时）

## Closure

- **结束时间**：2026-07-03
- **结束审计**：独立子代理（新会话 `ses_0d852ede0ffe2ro7V62ZZVduWk`）执行，**Verdict: PASS**（无 Blocker）。已据 Concern #1 对齐测试注释（totalMinutes 列 stdSqlType=VARCHAR/domain DECIMAL 基线异常）。
- **验证状态（全绿）**：`mvn test -pl module-maintenance/erp-mnt-service -am` = 22 tests / 0 failures（Phase 3 新增 4：`TestErpMntDowntimeAndE2E`；既有 Phase1 9 + Phase2 4 + smoke 5 无回归）；全仓 `mvn test` BUILD SUCCESS（无下游回归）；根 `mvn clean install -DskipTests` = 146 模块 BUILD SUCCESS。
- **交付物**：`IErpMntDowntimeEntryBiz.record/complete`（设备 DOWN 联动 + totalMinutes 计算）+ `ErpMntErrors` 补 2 错误码 + `TestErpMntDowntimeAndE2E`（4 tests）+ 日志/路线图/state-machine Non-Goal 补注。
- **Deferred（Non-Goal，留后继）**：停机通知制造域（事件驱动）/ 维修费用过账（MAINTENANCE_ISSUE 凭证）/ 预测性维护（IoT）/ 校准管理全流程 / 设备-资产价值联动 / 多级审批（均带触发条件，见 Deferred But Adjudicated）。
