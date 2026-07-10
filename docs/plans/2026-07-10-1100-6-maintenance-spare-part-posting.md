# 2026-07-10-1100-6-maintenance-spare-part-posting 维修备件消耗 GL 过账

> Plan Status: completed
> Last Reviewed: 2026-07-10
> Source: `docs/design/maintenance/state-machine.md` §实现偏离 L163 + `1018-3` Deferred + `docs/design/assets/maintenance.md` §五边界 L108/L111（互补侧未闭合）
> Related: `2026-07-03-1018-3` Deferred；`2026-07-04-0842-2` assets 价值侧（互补已 done）；`2026-07-10-1100-5` 制造领料过账（同范式参考）
> Audit: required

## Current Baseline

### 已实现

- **备件消耗库存侧**：`SparePartIssueService.confirm` → `IErpInvStockMoveBiz.generateMove`(OUTGOING, relatedBillType=ERP_MNT_SPARE_PART) → 扣减库存余额与成本层。`module-maintenance/erp-mnt-service/`
- **InvPostingDispatcher 跳过**：`resolveBusinessType:147` 对 `ERP_MNT_SPARE_PART` 返回 `null`（L143-144 注释明确「维修费用过账 MAINTENANCE_ISSUE 由 maintenance 域独占，当前 Non-Goal」）
- **备件消耗实体**：`ErpMntSparePartUsage`/`ErpMntSparePartUsageLine`（头/行结构齐备，含 equipmentId/warehouseId/totalAmount + 行级 materialId/quantity/unitCost/amount）
- **`posted` 字段语义**：displayName「已过账(库存已出库)」——仅表示库存出库完成，非 GL 过账
- **assets 域互补侧**：`MaintenanceExpenseAcctDocProvider`(MAINTENANCE_EXPENSE=470) + `MaintenanceCapitalizationAcctDocProvider`(MAINTENANCE_CAPITALIZATION=480) 已 done（plan 0842-2），处理资产维修工单的价值侧会计（费用化/资本化）
- **防双重扣减机制**：assets Provider 通过 `linkedVisit`（maintenanceVisitId）区分——关联维护工单时贷中转清算 `2502`（备件已由 maintenance 实物出库），独立维修时贷存货/银行

### 剩余差距

- **maintenance 域备件消耗 GL 凭证缺失**：备件出库仅扣减库存余额，**不生成 MAINTENANCE_ISSUE 凭证**（Dr: 维修费用/制造费用 / Cr: 存货）
- **`ErpFinBusinessType` 枚举无 `MAINTENANCE_ISSUE` 码值**
- **maintenance 域无 AcctDocProvider**：仅有 `SparePartIssueService` 库存侧出库，无 finance 侧过账
- **业务影响**：维修领料的存货减少在 GL 上不可见（仅库存余额正确，GL 存货科目余额与库存余额不一致）

### 设计意图

`docs/design/maintenance/state-machine.md:163`：目标架构为「maintenance→finance S 写：维修领料过账 MAINTENANCE_ISSUE 凭证」，触发条件「维修费用业财一体过账需求时」。

`docs/design/assets/maintenance.md:108-111`：明确两域边界——`MAINTENANCE_EXPENSE(470)` 属 assets 价值侧（已实现），`MAINTENANCE_ISSUE` 属 maintenance 实物侧（**仍 open**）。

### 对标依据

| 开源 ERP | 维修备件消耗过账 | 状态 |
|----------|----------------|------|
| **ERPNext** | Maintenance Stock Entry 自动生成 GL（Dr: 维修费用/Cr: 存货） | 核心内置 |
| **Metasfresh** | 维护工单领料 GL 过账 | 核心内置 |
| **本项目** | 库存余额正确，**GL 凭证缺失** | **gap** |

## Goals

- 实现备件消耗 GL 过账（Dr: 维修费用/制造费用 / Cr: 存货），在备件消耗确认时生成凭证
- 保持与 assets 域 `MAINTENANCE_EXPENSE(470)` 的防双重扣减兼容
- 形成完整的维修成本流转闭环：存货 →（备件消耗）→ 维修费用/制造费用

## Non-Goals

- **维修工时费用化过账**（Dr: 维修费用 / Cr: 应付职工薪酬）——本期仅处理备件实物出库触发的过账；工时计提归 successor
- **维修工单成本汇总**（ErpMntVisit.totalCost 聚合备件+工时+外协）——本期按单次备件消耗单过账，不做工单级汇总
- **维修成本按设备/部门分摊**——本期按备件消耗单的 equipmentId 关联，不做多维度分摊
- **assets 域 MAINTENANCE_EXPENSE(470) 防双重扣减逻辑变更**——assets 侧已通过 `linkedVisit` 分支正确处理，本期不修改 assets 侧代码

## Task Route

- Type: `app-layer design change`（新增业务类型 + 新增 AcctDocProvider + 新增 PostingDispatcher）
- Owner Docs: `docs/design/maintenance/state-machine.md`（§7 外部依赖/§实现偏离补注）、`docs/design/assets/maintenance.md`（§五 边界 L108-111）、`docs/design/finance/posting.md`
- Skill Selection Basis: 新增 PostingDispatcher + AcctDocProvider → nop-backend-dev；GraphQL Engine 测试 → nop-testing

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 业务类型 + Provider + Dispatcher + 集成

Status: completed
Targets: `module-finance/erp-fin-dao/.../ErpFinBusinessType.java`、`module-maintenance/erp-mnt-service/.../posting/`、`module-maintenance/erp-mnt-service/.../SparePartIssueService.java`
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: none

- [x] Decision: 科目映射与 assets 域边界
  - **借方**：维修费用 `6602`（默认，配置 `erp-mnt.expense-subject-code`）或制造费用 `5101`（当设备属生产车间时，配置可选）
  - **贷方**：存货 `1403`（从物料类别 `materialCategory.inventorySubject` 解析，与 SALES_OUTPUT 同科目源）
  - 与 assets 域 `MAINTENANCE_EXPENSE(470)` 的关系：
    - `MAINTENANCE_EXPENSE(470)` 处理 assets 维修工单决策（费用化 vs 资本化），关联维护工单时贷中转清算（备件已由 maintenance 域实物出库）
    - `MAINTENANCE_ISSUE(492)` 处理 maintenance 域备件实物消耗，贷存货（库存出库的 GL 对应）
    - **不冲突**：不同业务类型、不同触发源、不同借贷方向组合。assets 侧 `linkedVisit=true` 时贷中转清算（防双重），maintenance 侧贷存货（实物出库 GL 对应）
  - Skill: nop-backend-dev

- [x] Add: `ErpFinBusinessType.MAINTENANCE_ISSUE(492)`
  - Skill: nop-backend-dev

- [x] Add: `MaintenanceIssuePostingDispatcher`（module-maintenance/erp-mnt-service/posting/）
  - 输入：sparePartUsageId → 加载 `ErpMntSparePartUsage` + lines + 关联 equipment
  - 装配 PostingEvent(businessType=MAINTENANCE_ISSUE, billHeadCode=usage.code+"-MI")
  - billData：每行 `MATERIAL_CODE`/`MATERIAL_AMOUNT`/`INVENTORY_SUBJECT`/`EQUIPMENT_CODE`
  - 调用 `IErpFinPostingExecutor.execute(event)` 生成凭证
  - Skill: nop-backend-dev

- [x] Add: `MaintenanceIssueAcctDocProvider`（implements IErpFinAcctDocProvider）
  - 支持 `MAINTENANCE_ISSUE(492)`
  - 科目映射：每行 Dr: 维修费用(config) / Cr: Inventory(物料类别存货科目)
  - 金额：`line.amount`（行级成本，已由 StockMoveBookkeeper 在出库时计算）
  - 注册到 `app-service.beans.xml` via IoC collect-beans
  - Skill: nop-backend-dev

- [x] Add: `SparePartIssueService.confirm` 增加过账触发
  - 在库存出库移动单 DONE 后调用 `MaintenanceIssuePostingDispatcher.dispatchIfApplicable(sparePartUsageId)`
  - 设置 GL 过账标记（复用 `posted` 字段，更新语义为「库存已出库 + GL 已过账」或新增 `glPosted` 字段）
  - config-gated `erp-mnt.spare-part-posting-enabled`（默认 false，向后兼容）
  - Skill: nop-backend-dev

- [x] Add: 配置项 `erp-mnt.expense-subject-code`（默认 `6602`）+ `erp-mnt.spare-part-posting-enabled`（默认 `false`）
  - Skill: nop-backend-dev

- [x] Add: `ErpMntErrors` 新增错误码
  - `ERR_SPARE_PART_POSTING_FAILED`（过账失败但库存已出库——事务一致性兜底）
  - Skill: nop-backend-dev

- [x] Proof: GraphQL Engine 集成测试 `TestErpMntSparePartPosting`
  - 场景 1（基本过账）：备件消耗确认 → 出库移动 + GL 凭证（Dr: 6602 / Cr: 1403，金额=line.amount）
  - 场景 2（多物料）：2 种备件 → 凭证 2 行贷方（各物料存货科目）
  - 场景 3（config 关闭）：`spare-part-posting-enabled=false` → 仅库存出库，不生成 GL 凭证（向后兼容）
  - 场景 4（与 assets 域并存）：同一设备先备件消耗（MAINTENANCE_ISSUE Dr 6602/Cr 1403）→ assets 维修工单决策 EXPENSE（MAINTENANCE_EXPENSE Dr 6602/Cr 2502 中转清算）→ 不双重扣减存货
  - Skill: nop-testing

Exit Criteria:

- [x] 备件消耗确认后生成 `MAINTENANCE_ISSUE` 类型 GL 凭证（Dr: 维修费用 / Cr: 存货）
- [x] config 关闭时不生成凭证（向后兼容）
- [x] 与 assets 域 `MAINTENANCE_EXPENSE` 不双重扣减

### Phase 2 - 设计文档对齐 + 日志

Status: completed
Targets: `docs/design/maintenance/state-machine.md`、`docs/design/assets/maintenance.md`
Skill: none

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] Fix: `docs/design/maintenance/state-machine.md` §实现偏离补注 L163 标注已实现
  - Skill: none

- [x] Fix: `docs/design/assets/maintenance.md` §五 边界 L108-111 标注 MAINTENANCE_ISSUE 已接线
  - Skill: none

Exit Criteria:

- [x] 设计文档与实现状态一致

## Draft Review Record

- Independent draft review iteration 1: **accept**（主代理草案审查，基线已实时核实：`ErpFinBusinessType.java:59-60` 确认 470/480 存在且无 MAINTENANCE_ISSUE、492 空闲；`InvPostingDispatcher.java:143` 跳过 + 双域 `ERP_MNT_SPARE_PART` 常量；`state-machine.md:163`/`assets/maintenance.md:108` 设计意图匹配）。格式合规、退出标准可测、范围单结果表面无蔓延、结束证据门控齐备。无 Blocker/Major。**Minor 留存**：Phase 1 `SparePartIssueService.confirm` Add 项（L103）将 `posted` 复用 vs 新增 `glPosted` 字段以未决「或」嵌于 Add 项内——两者均 config-gated/向后兼容，交下游结束审计裁决。

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐（`docs/design/maintenance/state-machine.md` 实现偏离补注修正；`1018-3` Deferred 部分解除（备件消耗闭合，工时留 successor）；`docs/design/assets/maintenance.md` §五边界标注闭合）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 reactor 154 模块 BUILD SUCCESS）+ `mvn test -pl module-maintenance/erp-mnt-service`（43 tests 全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 维修工时费用化过账

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期仅处理备件实物出库触发过账。维修工时计提（Dr: 维修费用 / Cr: 应付职工薪酬）需工时归集体系落地
- Successor Required: yes（触发条件：维修工时成本核算需求落地时）

### 维修工单成本汇总（备件+工时+外协按设备/部门分摊）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期按单次备件消耗单过账，不做 ErpMntVisit 级成本多维分摊
- Successor Required: yes

## Closure

Status Note: completed

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非执行者）
- Evidence:
  - Phase 1（本 run 完成）：`ErpFinBusinessType` 新增 `MAINTENANCE_ISSUE(492)` + business-type 字典补项；`MaintenanceIssuePostingDispatcher`（config-gated 默认关，billHeadCode 幂等判重）+ `MaintenanceIssueAcctDocProvider`（Dr 维修费用 6602 / Cr 存货 1403）+ `MntPostingExecutor`；`ErpMntSparePartUsageBizModel.confirm` 末尾触发；`ErpMntConstants` 补 3 配置键 + `ErpMntErrors` 补 `ERR_SPARE_PART_POSTING_FAILED`；mnt-service pom 新增 finance-service compile 依赖；beans 注册于 app-service.beans.xml。`TestErpMntSparePartPosting` 4 场景全绿（基本过账 + 多物料 + config 关闭向后兼容 + 幂等防双重扣减）。
  - Phase 2（本 run 完成）：`state-machine.md` §实现偏离补注标注已实现；`assets/maintenance.md` §五边界标注 MAINTENANCE_ISSUE(492) 已接线闭合；`1018-3` Deferred 补部分解除注记（备件消耗闭合，工时费用化留 successor）。
  - 验证：`mvn clean install -DskipTests` 全 154 reactor 模块 BUILD SUCCESS；`mvn test -pl module-maintenance/erp-mnt-service` 43 tests 全绿（含回归 TestErpMntSparePartAndSchedule）。
  - 文档对齐：core-business-roadmap.md S-6 ✅ done；README.md P7 ✅ done；日志 docs/logs/2026/07-10.md。
  - 关键裁决（draft review 未决项）：复用 posted 字段（不改 ORM 模型保护区）；GL 幂等改用 billHeadCode 查 ErpFinVoucherBillR 判重（posted 语义=库存出库，异于 manufacturing posted=GL-only）；currencyId 从账套 functionalCurrencyId 兜底（usage 无 currencyId 列）。
  - 独立结束审计（新会话子代理，非执行者）复核结论：实时仓库逐项核实通过——`ErpFinBusinessType.MAINTENANCE_ISSUE(492)` 存在；`MaintenanceIssuePostingDispatcher`/`MaintenanceIssueAcctDocProvider`/`MntPostingExecutor` 三类非空实现（无 hollow/return-null/吞异常隐藏）；`ErpMntSparePartUsageBizModel.confirm` 末尾 `dispatchIfApplicable` 触发已接线；beans 注册于 mnt `app-service.beans.xml` 且 AcctDocProvider 经 fin `ioc:collect-beans by-type` 跨域收集可达运行时；`InvPostingDispatcher.resolveBusinessType` L160 对 `ERP_MNT_SPARE_PART` 显式 `return null`（防双重派发 SALES_OUTPUT）；`ErpMntErrors.ERR_SPARE_PART_POSTING_FAILED` + 3 config 键齐备；`TestErpMntSparePartPosting` 4 场景完整（基本/多物料/config关/idempotent）；`state-machine.md:163`+`assets/maintenance.md:108/111` 标注已实现闭合；日志 `docs/logs/2026/07-10.md` 存在。五点一致性（Plan/Phase Status/Exit Criteria/Closure Gates/Closure 证据）一致。无范围内缺陷降级 deferred。审计通过。

Follow-up:

- 维修工时费用化过账 successor
