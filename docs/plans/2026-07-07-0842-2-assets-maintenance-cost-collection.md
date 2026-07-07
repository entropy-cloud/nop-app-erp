# 2026-07-07-0842-2-assets-maintenance-cost-collection 资产维修管理与费用归集（UC-AST-10）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.5c（资产维修管理，UC-AST-10，状态 `todo`）
> Related: `2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`（折旧计划生成 + 资本化基线）、`2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime.md`（maintenance 域 ErpMntVisit 范式 + 维修费用过账 Deferred）、`2026-07-07-0842-1-assets-physical-inventory-count.md`（同批另一资产域计划，无相互依赖）
> Audit: required

## Current Baseline

> 实时核实（非采信记忆）。

- **roadmap 状态**：`docs/backlog/extended-roadmap.md:29` 工作项 `2.5c 资产维修管理（UC-AST-10：资产维修工单/费用归集）` 标记 `todo`；与 `2.5b` 同为 M2 资产域仅余 `todo` 项。
- **owner 设计文档缺失**：roadmap 引用的 `docs/design/assets/maintenance.md` **不存在**（glob 确认）。现有设计仅 `docs/design/assets/use-cases.md:164-178` UC-AST-10 给出业务级断言：维修单（关联资产卡片）→ 记录维修费用 → 若延长寿命/提升效能则**资本化**（增加原值 + 重算折旧计划）否则**费用化**（借维修费用 / 贷存货或银行）→ 维修费用可关联维护域 `ErpMntVisit`（若设备资产）。机制引用 `depreciation-and-posting.md` §二（资本化）/§四 + `../maintenance`。
- **ORM 模型现状**：`app-erp-assets.orm.xml` 共 14 实体（见 Plan 0842-1 基线）。**无任何资产维修实体**（`ErpAstMaintenance*`）。
- **跨域 maintenance 基线**：`IErpMntVisitBiz` / `IErpMntVisitTaskBiz` 已存在于 `module-maintenance/erp-mnt-dao`（plan 1018-3 done）。UC-AST-10 的"维修费用可关联维护域 ErpMntVisit"可经此 I*Biz 弱关联（assets→maintenance R 只读，不跨工程 refEntityName）。
- **与 1018-3 Deferred 的边界（互补不闭合）**：1018-3 Deferred 明列「维修费用过账（MAINTENANCE_ISSUE 凭证，备件消耗/工时）」留后继——该后继属 **maintenance 域实物侧**（维护工单 ErpMntVisit 消耗备件经 `ErpMntSparePartUsage.generateMove` 已做库存出库，其费用化凭证 MAINTENANCE_ISSUE 仍未接线）。本计划属 **assets 域价值侧**（资产维修工单的资本化/费用化会计处理），与 1018-3 实物侧**互补而非闭合**：两域不同价值视角、不同业务类型、不同实体。本计划**不闭合** 1018-3 该 Deferred 项，仅在其 owner doc 补交叉引用边界注记。关系裁定见 Phase 1 Decision。
- **资本化重用基线**：`IErpAstAssetCapitalizationBiz` + `ErpAstAssetCapitalizationProcessor`（1000-2 done）已实现"新建 DRAFT 卡片 → IN_SERVICE + 生成折旧计划"。资本化维修需对**既有 IN_SERVICE 资产**增加原值并**重算折旧计划**（区别于新建卡片）——折旧计划重算逻辑需新增，不能直接复用建卡路径。`ErpAstDepreciationScheduleBizModel` 存在。
- **业财过账基线**：assets-service 已注册 6 个 Provider（见 0842-1）。`ErpFinBusinessType` max code=450。维修费用化/资本化无对应业务类型——需新增。
- **缺失差距**：(a) 维修工单 + 费用行实体 + 状态/处置字典；(b) 维修状态机 + 费用归集 + 资本化/费用化裁决；(c) MAINTENANCE_EXPENSE / MAINTENANCE_CAPITALIZATION 业务类型 + 过账 Provider；(d) 资本化维修的原值增量 + 折旧计划重算；(e) owner 设计文档 `assets/maintenance.md`。

## Goals

- 落地 UC-AST-10 资产维修全流程：维修工单（关联资产卡片，可弱关联 maintenance 域 ErpMntVisit）→ 归集维修费用（人工/备件/外协）→ 裁决处置方式（CAPITALIZE 延长寿命/提升效能 vs EXPENSE 日常维修）→ 资本化路径（增加资产原值 + 重算折旧计划 + 资本化凭证）或费用化路径（维修费用凭证 借费用/贷存货或银行）→ 反向红冲纠错。
- 新增维修实体到 ORM 模型（唯一真相源），codegen 重新生成下游模块。
- 补齐 owner 设计文档 `docs/design/assets/maintenance.md`，收口 UC-AST-10 机制 + 资本化/费用化裁决规则 + 与 maintenance 域边界。
- 资本化维修经原值增量驱动折旧计划重算（残值/剩余使用年限重新摊销）。

## Non-Goals

- 维修工单的预测性维护 / IoT 触发（独立能力面，触发条件=IoT 数据源落地时）。
- 维修计划自动生成（定期保养计划归 maintenance 域 1018-3 已 done 的维护计划；本计划只管维修工单价值侧）。
- 多资产联合维修工单（本期一工单一资产；多资产归 successor）。
- 维修质检全流程（归 quality 域）。
- 维修工单 AMIS 页面深度定制（codegen 标准页满足本期）。
- 资产盘点（UC-AST-09，归同批计划 0842-1）。

## Task Route

- Type: `app-layer design change + implementation-only change`（先补 owner 设计，再实现）
- Owner Docs: `docs/design/assets/maintenance.md`（**待创建**）、`docs/design/assets/use-cases.md` §UC-AST-10、`docs/design/assets/depreciation-and-posting.md` §二/§四、`docs/design/assets/state-machine.md`、`docs/design/maintenance/`（跨域边界参照）、`docs/architecture/domain-design-guidelines.md`
- Skill Selection Basis: 实现面为 BizModel 状态机 + 跨实体（注入 `IErpMntVisitBiz` R 只读 + `IErpAstDepreciationScheduleBiz` 重算）+ 业财过账 Provider + 折旧计划重算，匹配 `nop-backend-dev`（强制顺序、Processor/task 决策、跨实体 I*Biz、safe API、反模式自检）。ORM 变更匹配 model-first-development。测试面匹配 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。维修为 assets 域价值管理 + finance 过账，无外部端口/密钥/外部服务。
- 配置点（`ErpAstConstants`/`ErpAstConfigs`）：`erp-ast.maintenance-require-approval`（维修过账强制审批，默认 true）、`erp-ast.maintenance-capitalize-threshold`（资本化金额阈值，低于则强制费用化，默认 0=不设阈值按 treatment 字段判定）、`erp-ast.maintenance-cap-adjust-depreciation-base`（资本化是否重算折旧基数，默认 true，镜像 0540-3 范式）。

## Execution Plan

### Phase 1 - ORM 模型 + owner 设计文档

Status: completed
Targets: `module-assets/model/app-erp-assets.orm.xml`、`docs/design/assets/maintenance.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`：维修实体建模。选项 A：头-行 `ErpAstMaintenance`（维修工单头：资产引用、可选 maintenanceVisitId 弱关联、treatment=CAPITALIZE/EXPENSE、状态）+ `ErpAstMaintenanceCost`（费用行：costType=LABOR/SPARE_PART/SUBCONTRACT、金额、来源存货/银行）。选项 B：单表。**选 A**（费用归集需多行多类型，头-行是仓内范式）。残留风险：无。
  - Skill: `nop-backend-dev`
- [x] `Add`：`app-erp-assets.orm.xml` 加性新增 `ErpAstMaintenance` + `ErpAstMaintenanceCost`（列、`tagSet="pub"` 关系、UK 一工单一资产活跃）。新增字典 `erp-ast/maintenance-status`（DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/POSTED/CANCELLED）+ `erp-ast/maintenance-treatment`（CAPITALIZE/EXPENSE）+ `erp-ast/maintenance-cost-type`（LABOR/SPARE_PART/SUBCONTRACT）。`maintenanceVisitId` 为弱关联字段不跨工程 refEntityName（走 I*Biz）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：维修业务类型。选项 A：复用 `CAPITALIZATION(80)` + 新增费用化类型。选项 B：新增 `MAINTENANCE_EXPENSE(470)` + `MAINTENANCE_CAPITALIZATION(480)` 两个独立类型。**选 B**——资本化维修（既有资产原值增量）与新建资本化（建卡）语义不同，独立类型利于审计与科目映射；费用化维修科目（维修费用/制造费用）独立。同步 `ErpFinBusinessType` 枚举 + `erp-fin/business-type` 字典（code=470/480）。残留风险：与 1018-3 deferred 的 `MAINTENANCE_ISSUE`（maintenance 域实物侧）命名邻近易混—— mitigated by 不同 code 段（470/480 vs 待定）+ owner doc 边界注记。
  - Skill: `nop-backend-dev`
- [x] `Decision`：**与 1018-3 deferred MAINTENANCE_ISSUE 的关系（B1）**。选项 A：本计划**闭合** 1018-3 该 Deferred。选项 B：本计划与 1018-3 该 Deferred **互补共存**（不闭合）。**选 B**——1018-3 的 MAINTENANCE_ISSUE 是 maintenance 域实物侧（维护工单备件出库/工时的费用化凭证，实体 ErpMntVisit/ErpMntSparePartUsage），本计划的 MAINTENANCE_EXPENSE/CAPITALIZATION 是 assets 域价值侧（资产维修工单的会计处理），两域不同实体/不同价值视角/不同结果表面。替代方案 A 风险：混淆域边界、把 maintenance 域实物出库凭证塞进 assets 域违反一域一职责。残留风险：未来若两域统一维修单据模型，命名邻近（MAINTENANCE_EXPENSE vs MAINTENANCE_ISSUE）可能需重整——触发条件：维修单据跨域统一时。Phase 4 仅在 1018-3 owner doc 补**互补边界注记**（非闭合标记）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：**SPARE_PART 费用来源边界（B2，防双重库存扣减）**。当维修工单 `maintenanceVisitId` 关联维护域工单时，备件已由 maintenance 域 `ErpMntSparePartUsage.generateMove` 做物理库存出库（1018-3 done）。选项 A：SPARE_PART 费用行从 `IErpMntSparePartUsageBiz` 自动取数。选项 B：SPARE_PART 费用行为**操作员手工录入的会计确认**（独立于 ErpMntSparePartUsage）。**选 B**——保持 assets 域价值确认的独立性与简单性，不在本期建跨域自动取数。**防双重扣减规则**：当 `maintenanceVisitId` 非空（已关联）时，过账 GL **不贷存货**，改贷**维修中转/清算科目**（备件已由实物侧出库，价值侧只做科目重分类）；当 `maintenanceVisitId` 为空（独立资产维修）时，GL 贷**存货/银行**直接。config-gated `erp-ast.maintenance-linked-credit-clearing`（默认 true=启用中转科目防双重）。替代方案 A 风险：跨域取数耦合 + 一致性校验复杂度。残留风险：操作员须人工对账关联工单的 SPARE_PART 金额与维护工单实际消耗（人工控制，本期不做自动强一致校验—— successor 触发条件：跨域维修单据统一时）。
  - Skill: `nop-backend-dev`
- [x] `Add`：创建 owner 设计文档 `docs/design/assets/maintenance.md`，收口维修状态机、资本化/费用化裁决规则（延长寿命/提升效能→CAPITALIZE，否则 EXPENSE）、资本化原值增量 + 折旧计划重算规则、科目映射、与 maintenance 域边界（资产域=价值，maintenance 域=实物）。对齐既有 owner 文档风格。
  - Skill: none

Exit Criteria:

> 仅此阶段交付的可观察结果 + 解除 Phase 2 阻塞的本地化检查。

- [x] ORM 模型变更 `xmllint --noout` well-formed + codegen dry-run 识别新实体
- [x] `ErpFinBusinessType` 枚举与 `erp-fin/business-type` 字典 code=470/480 一一对应
- [x] 三个新字典 `erp-ast/maintenance-status` / `erp-ast/maintenance-treatment` / `erp-ast/maintenance-cost-type` 经 ORM 声明并可解析（codegen 识别）
- [x] `docs/design/assets/maintenance.md` 存在且含状态机/裁决规则/折旧重算/科目映射/跨域边界五节

### Phase 2 - 维修 BizModel + 状态机 + 费用归集 + 资本化重算

Status: completed
Targets: `module-assets/erp-ast-dao/.../IErpAstMaintenanceBiz.java`、`module-assets/erp-ast-service/.../entity/ErpAstMaintenanceBizModel.java`、`ErpAstErrors.java`、`ErpAstConstants.java`、`module-assets/erp-ast-web/.../erp-ast.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1

- [x] `Add`：`IErpAstMaintenanceBiz`（extends `ICrudBiz`）声明：`createMaintenance`（关联资产，可选 maintenanceVisitId）/`submit`/`startWork`(SUBMITTED→IN_PROGRESS)/`addCost`(费用行归集)/`completeWork`(IN_PROGRESS→COMPLETED)/`decideTreatment`(裁决 CAPITALIZE/EXPENSE + 阈值门控)/`approve`+`post`(过账：按 treatment 分派)/`cancel`/`reverse`。末参 `IServiceContext context`。
  - Skill: `nop-backend-dev`
- [x] `Decision`：编排方式。维修过账为多步（裁决 + 资本化增量/费用化 + 折旧重算 + 过账），拓扑稳定 → **Processor 模式**（镜像 `ErpAstAssetCapitalizationProcessor`），不引入 task.xml。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpAstMaintenanceBizModel` + `ErpAstMaintenanceProcessor`（protected step：`validateTransition` / `validateAsset`（注入 `IErpAstAssetBiz` 校验资产 IN_SERVICE/IDLE 非终态）/ `aggregateCost`（汇总费用行）/ `applyTreatmentCapitalize`（资产原值 += 资本化金额 + 重算折旧计划，注入 `IErpAstDepreciationScheduleBiz`）/ `applyTreatmentExpense` / `post`）。新增 ErrorCode（`ERR_AST_MAINTENANCE_ILLEGAL_STATUS_TRANSITION` / `ERR_AST_MAINTENANCE_ASSET_TERMINAL` / `ERR_AST_MAINTENANCE_NO_COST` / `ERR_AST_MAINTENANCE_TREATMENT_NOT_DECIDED` / `ERR_AST_MAINTENANCE_CAPITALIZE_BELOW_THRESHOLD`）到 `ErpAstErrors`，中文描述。
  - Skill: `nop-backend-dev`
- [x] `Decision`：折旧计划重算落位（S1，触及 done 接口契约）。选项 A：在 `IErpAstDepreciationScheduleBiz`（已 done 接口）**加性新增** `recalculateForCapitalizationMaintenance(assetId, increment)` 方法（additive，不破坏既有契约）。选项 B：在 `ErpAstMaintenanceProcessor` 内经 `IDaoProvider`/`IOrmTemplate` 直接操纵折旧计划实体（按 Nop 平台规则需注释说明 I*Biz 不足的原因）。**选 A**——重算逻辑是稳定领域事实、跨场景可复用、可独立单测；加性方法对 done 接口非破坏性扩展。按剩余使用年限重新摊销（原值+增量 − 已计提累计折旧 − 残值）/ 剩余月数，删除/重建未执行折旧计划条目，残值约束保留。config-gated `maintenance-cap-adjust-depreciation-base`。镜像 0540-3 重估调整折旧基数范式。残留风险：无（加性扩展）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpAstConstants`/`ErpAstConfigs` 配置键 + 状态/处置常量。
  - Skill: `nop-backend-dev`
- [x] `Add`：`erp-ast.action-auth.xml` 为新增 `@BizMutation` 动作补 action-auth 资源条目（镜像仓内既有 assets action-auth 范式），使前端菜单/权限可达。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] IBiz 与 BizModel 公共方法一一对应；末参 `IServiceContext`（P2 自检通过）
- [x] 维修状态机迁移完整，非法迁移抛 ErrorCode；资产终态校验拦截（单元测试覆盖）
- [x] CAPITALIZE 路径：资产原值增量 + 折旧计划重算（残值约束）经集成测试验证；EXPENSE 路径不影响资产原值/折旧

### Phase 3 - 业财过账 Provider + 反向红冲

Status: completed
Targets: `module-assets/erp-ast-service/.../posting/MaintenanceExpenseAcctDocProvider.java`、`module-assets/erp-ast-service/.../posting/MaintenanceCapitalizationAcctDocProvider.java`、`module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`MaintenanceExpenseAcctDocProvider`（`supportedTypes()`=MAINTENANCE_EXPENSE）——费用化路径：借 维修费用/制造费用（按资产类别映射）。贷方科目按 Phase 1 Decision（B2 防双重扣减）分支：`maintenanceVisitId` 非空时贷**维修中转/清算科目**（备件已由 maintenance 域实物出库），为空时贷**存货（备件消耗）/银行存款（外协/人工）**。config-gated `erp-ast.maintenance-linked-credit-clearing`。镜像 `ValueAdjustmentAcctDocProvider` 方向相关分支。
  - Skill: `nop-backend-dev`
- [x] `Add`：`MaintenanceCapitalizationAcctDocProvider`（`supportedTypes()`=MAINTENANCE_CAPITALIZATION）——资本化路径：借 固定资产（原值增量）/ 贷 在建工程或银行存款/存货（费用来源）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`app-service.beans.xml` 经 `ioc:collect-beans by-type` 注册两 Provider（运行时可达）。
  - Skill: `nop-backend-dev`
- [x] `Add`：维修单 `reverse` 红冲——红字凭证 + 回链 + 资产原值/折旧计划回退（资本化路径）或仅凭证回退（费用化路径）；posted 后纠错必经 reverse。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 两 Provider 经 beans.xml 注册为 Bean，过账引擎可路由（anti-hollow 自检通过）
- [x] CAPITALIZE/EXPENSE 两路径凭证方向与 owner doc 一致；资本化 reverse 回退原值 + 折旧计划（集成测试断言）
- [x] reverse 生成红字凭证（两路径均覆盖）

### Phase 4 - 测试 + 收口

Status: completed
Targets: `module-assets/erp-ast-service/src/test/...`、`docs/logs/2026/07-07.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [x] `Proof`：`TestErpAstMaintenance` 覆盖：维修工单创建+资产关联、费用归集（三类成本）、CAPITALIZE 裁决（原值增量+折旧重算+残值约束）、EXPENSE 裁决（费用凭证）、阈值门控、过账凭证方向、reverse 红冲（两路径）、资产终态拦截、非法状态迁移。框架 `JunitAutoTestCase` + 快照 CHECKING。
  - Skill: `nop-testing`
- [x] `Add`：更新 `docs/design/assets/maintenance.md` 实现注记（偏离补注）+ `docs/design/assets/use-cases.md` UC-AST-10 标实现落位。按 Phase 1 Decision（B1 互补不闭合）在 `docs/plans/2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime.md` 的 Deferred 条目与其 owner doc 补**互补边界注记**（标注 assets 域 0842-2 已承接价值侧、maintenance 域 MAINTENANCE_ISSUE 实物侧仍 open），**不**标记 1018-3 该 Deferred 为闭合。
  - Skill: none
- [x] `Add`：更新 `docs/logs/2026/07-07.md` + roadmap 工作项 2.5c 状态 `todo`→`done`。
  - Skill: none

Exit Criteria:

- [x] `module-assets` 测试全绿（0 failures/0 errors），含新增维修测试
- [x] owner 文档与实现一致；roadmap 状态更新；1018-3 Deferred 交叉引用收口

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0c5f61b18ffedJpbamkM6v8yd7`，独立 general 子代理，新会话冷重播无执行者上下文）— 全部 baseline 核实通过（2.5c todo / assets/maintenance.md 缺失 / ErpAstMaintenance* 缺失 / ErpFinBusinessType max=450 470·480 空闲 / IErpMntVisitBiz + IErpAstAssetCapitalizationBiz + IErpAstAssetBiz + IErpAstDepreciationScheduleBiz 存在 / 1018-3 Deferred 列维修费用过账后继 / 资本化重用不足 claim 核实正确）。2 BLOCKER：(B1) 与 1018-3 deferred MAINTENANCE_ISSUE 关系内部矛盾（baseline 称"承接"、Phase 4 称"若承接"）未裁定；(B2) maintenanceVisitId 关联时 SPARE_PART 费用来源边界未定，存在双重库存扣减风险。3 建议（S1 折旧重算落位触及 done 接口需 Decision / S2 Phase 1 退出缺字典物化检查 / S3 action-auth 可见性）。**已修订**：B1→新增 Phase 1 Decision（互补不闭合）+ baseline 改写 + Phase 4 改为互补边界注记非闭合；B2→新增 Phase 1 Decision（手工录入会计确认 + linked 贷中转科目防双重，config-gated）+ Phase 3 expense provider 贷方分支；S1→Phase 2 折旧重算改 Decision（加性 IBiz 方法）；S2→Phase 1 退出加字典物化检查；S3→Phase 2 加 action-auth Add 项 + Targets 补 action-auth 路径。
- Independent draft review iteration 2: **accept**（`ses_0c5ef5536ffeoUR44NbcPOIBQL`，独立 general 子代理，新会话冷重播无执行者上下文）— B1/B2 双 BLOCKER 核实解决：(B1) Phase 1 Decision 选"互补共存不闭合"+ baseline 改写 + Phase 4 互补边界注记非闭合，实时核实 1018-3 Deferred 仍 open；(B2) Phase 1 Decision 选手工录入会计确认 + linked 贷中转科目防双重（config-gated）+ Phase 3 expense provider 贷方分支，实时核实 `IErpInvStockMoveBiz.generateMove` 经 SparePartIssueService:55 调用为真实机制。无 NEW BLOCKER。3 非阻塞术语精度建议（执行期 owner doc 改写 generateMove 调用链 / "承接"改"覆盖" / Phase 4 测试加 SPARE_PART linked 贷方双态断言）。迭代收敛，计划可转 active。

## Closure Gates

> 仅在所有项目 + 各阶段退出标准勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（维修全流程 + CAPITALIZE/EXPENSE 双路径 + 折旧重算 + 业财过账 + reverse）
- [x] 相关文档对齐（`assets/maintenance.md` 创建、use-cases.md 标注、1018-3 Deferred 交叉引用、roadmap 更新）
- [x] 已运行验证：`mvn clean install -DskipTests`（154+ 模块）+ `mvn test -pl module-assets/erp-ast-service -am`（78 tests 0 failures/0 errors）+ ORM `xmllint --noout` + action-auth well-formed
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 预测性维护 / IoT 触发维修工单

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期手工创建维修工单满足 UC-AST-10 价值管理；IoT 触发属实物采集能力面（且主要归 maintenance 域）。
- Successor Required: `yes`（触发条件：IoT 设备数据源落地 + 预测性维护需求时）

### 多资产联合维修工单

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 一工单一资产满足核心价值归集；多资产联合属运营复杂场景。
- Successor Required: `yes`（触发条件：多资产联合维修业务需求时）

### 维修工单 AMIS 深度定制页

- Classification: `optimization candidate`
- Why Not Blocking Closure: codegen 标准页满足本期功能验证。
- Successor Required: `yes`（触发条件：维修 UX 优化需求时）

## Closure

Status Note: 计划 4 阶段全部完成（Phase 1 ORM 模型+字典+业务类型+codegen 回归+owner 设计文档；Phase 2 维修 BizModel+Processor+状态机+费用归集+资本化折旧重算；Phase 3 业财过账 Provider+反向红冲；Phase 4 测试+文档收口）。验证全绿：`mvn test -pl module-assets/erp-ast-service -am` = 78 tests/0 Failures/0 Errors（含新增 TestErpAstMaintenance 12 cases）；根 `mvn clean install -DskipTests` = BUILD SUCCESS（154 模块）；全工作区 `mvn test` = 2212 tests/0 Failures/0 Errors。草案审查 iter2 pass（B1/B2 双 BLOCKER 解决）。roadmap 2.5c 标 ✅ done；日志 `docs/logs/2026/07-07.md`。结束审计由独立子代理（新会话）执行并通过，证据见下 Closure Audit Evidence。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（新会话，冷重播无执行者上下文），通过 `tools/mission-driver` closure-audit 步骤触发
- Evidence:
  - 实时核实 Phase 1：`module-assets/model/app-erp-assets.orm.xml:1296` `ErpAstMaintenance` + `:1371` `ErpAstMaintenanceCost` 头行实体存在；`:158/:167/:172` 三个字典（maintenance-status/treatment/cost-type）物化；`module-finance/erp-fin-dao/.../ErpFinBusinessType.java` 含 `MAINTENANCE_EXPENSE`(470)/`MAINTENANCE_CAPITALIZATION`(480)；`docs/design/assets/maintenance.md` 存在
  - 实时核实 Phase 2：`IErpAstMaintenanceBiz.java`（9 动作）+ `ErpAstMaintenanceBizModel.java` + `ErpAstMaintenanceProcessor.java`（protected step 范式）；`IErpAstDepreciationScheduleBiz.recalculateForCapitalizationMaintenance` 加性扩展落地（DAO 接口 + BizModel + Processor 三处签名一致）；`module-assets/erp-ast-web/.../erp-ast.action-auth.xml:59-66` 含 `ErpAstMaintenance-main`/`ErpAstMaintenanceCost-main` 资源（`app:useCases="UC-AST-10"`，前端菜单可达）
  - 实时核实 Phase 3：`MaintenanceExpenseAcctDocProvider.java:42` `supportedTypes()=EnumSet.of(MAINTENANCE_EXPENSE)` + `MaintenanceCapitalizationAcctDocProvider.java:36` `supportedTypes()=EnumSet.of(MAINTENANCE_CAPITALIZATION)`；`app-service.beans.xml:60-63` 两 Provider 显式注册为 Bean（anti-hollow 自检通过——`supportedTypes` 返回真实枚举，beans.xml 注册后过账引擎可路由）
  - 实时核实 Phase 4：`module-assets/erp-ast-service/src/test/.../TestErpAstMaintenance.java`（24 个 @Test 标注）；`target/surefire-reports/app.erp.ast.service.TestErpAstMaintenance.txt` = `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`；`docs/backlog/extended-roadmap.md:29/:77` 工作项 2.5c 标 ✅ done；`docs/logs/2026/07-07.md` 含 3 处 0842-2/UC-AST-10 引用
  - 反松弛/反hollow 自检：两 Provider 实现非空、`supportedTypes()` 返回真实枚举非空集、beans.xml 注册使其运行时可达；`recalculateForCapitalizationMaintenance` 在 MaintenanceProcessor 两处真实调用（正向 + reverse 回退）
  - 文本一致性：Plan Status=completed / 4 Phase Status 全 completed / 4 Phase Exit Criteria 全 [x] / 7 Closure Gates 全 [x] / 日志 07-07.md 存在 / roadmap 2.5c done — 全部一致

Follow-up:

- 预测性维护 / 多资产联合维修 / 维修工单前端深度定制（均带触发条件，见 Deferred But Adjudicated）
