# 2026-07-07-0842-1-assets-physical-inventory-count 资产盘点全流程（UC-AST-09）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.5b（资产盘点，UC-AST-09，状态 `todo`）
> Related: `2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`（资产价值调整基线）、`2026-07-05-0540-3-assets-impairment-revaluation.md`（VALUE_ADJUSTMENT 三轴状态机 + AcctDocProvider 范式）、`2026-07-07-0842-2-assets-maintenance-cost-collection.md`（同批另一资产域后继）
> Audit: required

## Current Baseline

> 实时核实（非采信记忆）。

- **roadmap 状态**：`docs/backlog/extended-roadmap.md:28` 工作项 `2.5b 资产盘点（UC-AST-09：固定资产盘点流程）` 标记 `todo`；M2 资产域仅余 `2.5b`/`2.5c` 两项 `todo`（`2.5d` 拆分合并、`2.14` 减值重估均 `done`）。
- **owner 设计文档缺失**：roadmap 引用的 `docs/design/assets/inventory.md` **不存在**（glob 确认）。现有设计仅 `docs/design/assets/use-cases.md:147-160` UC-AST-09 给出业务级断言（盘点单按部门/类别范围 → 录入实盘 → 差异=实盘−账面 → 盘盈生成卡片增加+价值评估入账 / 盘亏触发处置 UC-AST-04 或调查 → 差异生成调整凭证 借/贷固定资产 差额）。机制引用 `depreciation-and-posting.md` §四（价值调整）+ `state-machine.md`。
- **ORM 模型现状**：`module-assets/model/app-erp-assets.orm.xml` 共 14 实体（ErpAstAsset / ErpAstAssetCategory / ErpAstDepreciationSchedule / ErpAstMovement / ErpAstValueAdjustment / ErpAstDisposal / ErpAstAssetCapitalization / ErpAstCip / ErpAstCipCostItem / ErpAstCipProgressBilling / ErpAstSplit / ErpAstMerge / ErpAstSplitLine / ErpAstMergeLine）。**无任何盘点实体**（`ErpAstInventory*`）。
- **状态机基线**：`ErpAstConstants` 已有 `ASSET_STATUS_*`（DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD/DISPOSED）+ `DOC_STATUS_DRAFT` + 三轴状态机范式（docStatus/approveStatus/posted，见 0540-3 ErpAstValueAdjustmentBizModel 已验证）。`erp-ast/asset-status` 字典已物化。
- **业财过账基线**：assets-service 已注册 6 个 `IErpFinAcctDocProvider`（Depreciation/Disposal/ValueAdjustment/Capitalization/AssetSplit/AssetMerge）。`ErpFinBusinessType` 枚举当前 max code=450（ASSET_MERGE），`erp-fin/business-type` 字典为数值权威源。盘点调整凭证无对应业务类型——需新增。
- **可复用机制**：盘盈"生成资产卡片增加"可复用 `IErpAstAssetCapitalizationBiz` 资本化链（新建 DRAFT 卡片 → IN_SERVICE）；盘亏"触发处置"可复用 `IErpAstDisposalBiz`（SCRAPPED 路径）。两者均已 done（1000-2）。
- **缺失差距**：(a) 盘点头-行实体 + 状态字典；(b) 盘点状态机 + 差异引擎；(c) ASSET_INVENTORY_ADJUSTMENT 业务类型 + 过账 Provider；(d) owner 设计文档 `assets/inventory.md`。

## Goals

- 落地 UC-AST-09 资产盘点全流程：盘点单（范围=部门/类别）→ 录入实盘数量 → 系统计算差异（盘盈/盘亏/一致）→ 差异处置（盘盈建新卡 + 入账；盘亏触发处置流程或登记调查）→ 生成盘点调整凭证（业财过账）→ 反向红冲纠错。
- 新增盘点实体到 ORM 模型（唯一真相源），codegen 重新生成下游模块。
- 补齐 owner 设计文档 `docs/design/assets/inventory.md`，收口 UC-AST-09 机制细节。
- 盘点差异调整经业财过账生成凭证，复用既有 `IErpFinVoucherBiz.post` + 反向 `reverse` 范式。

## Non-Goals

- 条码/RFID/移动端扫码盘点（独立能力面，触发条件=移动盘点需求上线时）。
- 周期性自动盘点（nop-job 定时生成盘点单）；本期仅手工创建盘点单。触发条件=盘点 cron 需求时。
- 盘点结果的多币种重估（盘点只校对实物数量与账面数量，不涉及汇率重估）。
- 盘点工作台 AMIS 页面定制（codegen 标准页可用即满足本期；深度定制面归前端 successor）。
- 盘亏调查审批多级工作流（本期盘亏走"触发处置"或"标记调查"二选一，不引入独立多级审批流）。
- 资产维修管理（UC-AST-10，归同批计划 `2026-07-07-0842-2`）。

## Task Route

- Type: `app-layer design change + implementation-only change`（需先补 owner 设计文档，再实现）
- Owner Docs: `docs/design/assets/inventory.md`（**待创建**）、`docs/design/assets/use-cases.md` §UC-AST-09、`docs/design/assets/depreciation-and-posting.md` §四、`docs/design/assets/state-machine.md`、`docs/architecture/domain-design-guidelines.md` §三轴状态分离
- Skill Selection Basis: 实现面为 BizModel 状态机 + 跨实体调用 + 业财过账 Provider，匹配 `nop-backend-dev`（强制 IBiz→BizModel 顺序、Processor/task 决策、safe API、反模式自检）。ORM 模型变更匹配 model-first-development（`nop-backend-dev` 路由）。测试面匹配 `nop-testing`（IGraphQLEngine 快照）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。盘点为 assets 域内部流程 + finance 过账，无外部端口/密钥/外部服务。
- 配置点（config-gated，归 `ErpAstConstants`/`ErpAstConfigs`）：`erp-ast.inventory-require-approval`（盘点差异过账是否强制审批，默认 true）、`erp-ast.inventory-negative-shortage-blocks`（盘亏是否阻塞过账待调查，默认 false=允许盘亏直接触发处置）。

## Execution Plan

### Phase 1 - ORM 模型 + owner 设计文档

Status: completed
Targets: `module-assets/model/app-erp-assets.orm.xml`、`docs/design/assets/inventory.md`
Skill: `nop-backend-dev`（model-first-development 路由）

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`：盘点实体建模方案。选项 A：头-行两张表 `ErpAstInventory`（盘点单头：范围部门/类别、期间、状态、负责人）+ `ErpAstInventoryLine`（行：资产引用、账面数量、实盘数量、差异量、差异类型、处置标志）。选项 B：单表 + JSON 明细。**选 A**（头-行是仓内既定范式，见 ErpAstSplit/SplitLine、ErpAstCip/CostItem；审计追溯与 cascade-delete 清晰）。替代方案风险：B 牺牲关系完整性与查询能力。残留风险：无。
  - Skill: `nop-backend-dev`
- [x] `Add`：在 `app-erp-assets.orm.xml` 加性新增 `ErpAstInventory` + `ErpAstInventoryLine` 实体（列、`tagSet="pub"` 关系、UK 防重一行一盘点单）。新增字典 `erp-ast/inventory-status`（DRAFT/COUNTING/RECONCILING/POSTED/CANCELLED）+ `erp-ast/variance-type`（SURPLUS/SHORTAGE/MATCHED）。复用 `erp-ast/asset-status` 不新增资产状态。
  - Skill: `nop-backend-dev`
- [x] `Decision`：盘点调整业务类型。选项 A：复用 `VALUE_ADJUSTMENT(390)`。选项 B：新增 `ASSET_INVENTORY_ADJUSTMENT(460)`。**选 B**——盘点调整的科目方向与语义（盘盈贷固定资产/盘亏借固定资产，差额经资本公积或费用）与减值/重估不同，独立业务类型利于审计追溯与科目映射。同步 `ErpFinBusinessType` 枚举 + `erp-fin/business-type` 字典（code=460）。残留风险：无（加性新增，460 空闲已核实）。
  - Skill: `nop-backend-dev`
- [x] `Add`：创建 owner 设计文档 `docs/design/assets/inventory.md`，收口盘点状态机（四态 COUNTING 入口 + RECONCILING 差异冻结 + POSTED 终态）、差异处置裁决（盘盈→建卡+资本化入账；盘亏→触发 IErpAstDisposalBiz SCRAPPED 或标记调查）、科目映射、与 use-cases.md §UC-AST-09 双向引用。对齐既有 owner 文档风格（state-machine.md / depreciation-and-posting.md）。
  - Skill: none

Exit Criteria:

> 仅此阶段交付的可观察结果 + 解除 Phase 2 阻塞的本地化检查。

- [x] ORM 模型变更通过 `xmllint --noout` well-formed + codegen dry-run 识别新实体（无实体名冲突）
- [x] `ErpFinBusinessType` 枚举与 `erp-fin/business-type` 字典 code=460 一一对应（编译 + 字典解析通过）
- [x] `docs/design/assets/inventory.md` 存在且含状态机/差异处置/科目映射三节，与 use-cases.md §UC-AST-09 断言一致

### Phase 2 - 盘点 BizModel + 状态机 + 差异引擎

Status: completed
Targets: `module-assets/erp-ast-dao/.../IErpAstInventoryBiz.java`、`module-assets/erp-ast-service/.../entity/ErpAstInventoryBizModel.java`、`ErpAstErrors.java`、`ErpAstConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1

- [x] `Add`：`IErpAstInventoryBiz`（extends `ICrudBiz`）声明动作：`createInventory`（按部门/类别范围展开资产到行）/`submitForCount`(DRAFT→COUNTING)/`enterActualCount`(录入实盘，行级)/`reconcile`(COUNTING→RECONCILING 冻结差异)/`processVariance`(RECONCILING 差异处置：盘盈建新卡 + 盘亏触发处置或标记)/`approve`+`post`(RECONCILING→POSTED 过账)/`cancel`/`reverse`(红冲)。所有方法 `IServiceContext context` 最后一个参数。
  - Skill: `nop-backend-dev`
- [x] `Decision`：编排方式。选项 A：BizModel 方法内联全部步骤。选项 B：**Processor 模式**（protected step 方法，镜像 `ErpAstAssetCapitalizationProcessor` 范式）。**选 B**——盘点过账为多步（差异处置 + 建卡 + 过账 + 反写），拓扑稳定，Processor 让下游可逐 step 覆盖（产品化）。不引入 task.xml（无外部拓扑可变需求）。残留风险：无（拓扑稳定，Processor 是仓内既定范式）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpAstInventoryBizModel` + `ErpAstInventoryProcessor`（protected step：`validateTransition` / `expandAssetsToLines` / `calculateVariance` / `handleSurplus_createCard`（注入 `IErpAstAssetCapitalizationBiz`）/ `handleShortage_triggerDisposal`（注入 `IErpAstDisposalBiz`）/ `post`）。新增 ErrorCode（`ERR_AST_INVENTORY_ILLEGAL_STATUS_TRANSITION` / `ERR_AST_INVENTORY_LINE_ASSET_DUPLICATE` / `ERR_AST_INVENTORY_RANGE_EMPTY` / `ERR_AST_INVENTORY_NOT_RECONCILED`）到 `ErpAstErrors`，中文描述 + `.param(...)`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpAstConstants`/`ErpAstConfigs` 加配置键（`inventory-require-approval` / `inventory-negative-shortage-blocks`）+ 状态常量。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] IBiz 接口与 BizModel 公共方法一一对应（无 public 方法漏声明）；`IServiceContext` 为末参（P2 自检通过）
- [x] 盘点状态机四态迁移完整，非法迁移抛 `ERR_AST_INVENTORY_ILLEGAL_STATUS_TRANSITION`（单元测试覆盖 happy + 非法路径）
- [x] 差异引擎：盘盈 SURPLUS 行触发 `IErpAstAssetCapitalizationBiz` 建卡；盘亏 SHORTAGE 行触发 `IErpAstDisposalBiz`（config-gated 允许/阻塞）——经集成测试验证

### Phase 3 - 业财过账 Provider + 反向红冲

Status: completed
Targets: `module-assets/erp-ast-service/.../posting/AssetInventoryAcctDocProvider.java`、`module-assets/erp-ast-service/src/main/resources/_vfs/erp/ast/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] `Add`：`AssetInventoryAcctDocProvider implements IErpFinAcctDocProvider`，`supportedTypes()` 返回 `ASSET_INVENTORY_ADJUSTMENT`。按差异类型分支科目分解：盘盈 借 固定资产 / 贷 资本公积-其他资本公积（或营业外收入，按资产类别映射裁决）；盘亏 借 营业外支出（或资产减值损失）/ 贷 固定资产。科目来源资产类别科目映射 + 全局兜底。镜像 `ValueAdjustmentAcctDocProvider` 范式。
  - Skill: `nop-backend-dev`
- [x] `Add`：`app-service.beans.xml` 经 `ioc:collect-beans by-type` 注册 Provider（运行时可达，非空壳）。
  - Skill: `nop-backend-dev`
- [x] `Add`：盘点单 `reverse` 红冲——红字 ASSET_INVENTORY_ADJUSTMENT 凭证 + 回链 + 盘点单 posted 回退门控（posted=true 后纠错必经 reverse，遵守既有不可逆契约范式）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] Provider 经 beans.xml 注册为 Bean，过账引擎可路由到（anti-hollow 自检通过）
- [x] 盘盈/盘亏两条路径均生成方向正确的凭证分录（借/贷科目与 owner doc 一致）；集成测试断言凭证行
- [x] `reverse` 生成红字凭证且回退盘点单 posted 状态（集成测试覆盖）

### Phase 4 - 测试 + 收口

Status: completed
Targets: `module-assets/erp-ast-service/src/test/...`、`docs/logs/2026/07-07.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [x] `Proof`：`TestErpAstInventory` 覆盖：盘点单创建+范围展开、实盘录入、差异计算（盘盈/盘亏/一致三态）、盘盈建卡、盘亏触发处置、过账凭证方向、reverse 红冲、非法状态迁移。框架 `JunitAutoTestCase` + GraphQL + 快照 CHECKING。
  - Skill: `nop-testing`
- [x] `Add`：更新 `docs/design/assets/inventory.md` 实现注记（若有偏离）+ `docs/design/assets/use-cases.md` UC-AST-09 标实现落位。
  - Skill: none
- [x] `Add`：更新 `docs/logs/2026/07-07.md`（按 log-writing-guide）+ roadmap 工作项 2.5b 状态 `todo`→`done`。
  - Skill: none

Exit Criteria:

- [x] `module-assets` 测试全绿（0 failures/0 errors），含新增盘点测试
- [x] owner 文档与实现一致；roadmap 状态更新

## Draft Review Record

- Independent draft review iteration 1: **acceptable as-is**（`ses_0c5f65205ffeWcVoZV8N05CAQf`，独立 general 子代理，新会话冷重播无执行者上下文）— 全部 baseline 核实通过（2.5b todo / assets/inventory.md 缺失 / ErpAstInventory* 缺失 14 实体无盘点 / ErpFinBusinessType max=450 460 空闲 / 6 AcctDocProvider 存在 / IErpAstAssetCapitalizationBiz + IErpAstDisposalBiz 存在）。无 BLOCKER。规则 4（单结果表面）/ 规则 14（与 0842-2 拆分有据：不同 UC/不同 owner doc/不同结果表面）/ 反松弛 / 规则 7/8/13/执行规则 7/命名/保护区域同步义务 全部通过。4 建议（S1 两个 Decision 补残留风险一致性 / S2 盘盈"建新卡"解释提升为 Decision / S3 closure 注明 final re-run / S4 Phase 4 退出标准拆分）。**已修订**：S1→Phase 1 业务类型 Decision + Phase 2 编排 Decision 各补选项 + 残留风险；S2/S3/S4 保留为执行期细化（非阻塞）。迭代收敛，计划可转 active。
- Independent draft review iteration 2: <needs revision | acceptable as-is | accept> (<task/session id>) after <what changed>（无需，iteration 1 已 acceptable as-is）

## Closure Gates

> 仅在所有项目 + 各阶段退出标准勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（盘点全流程 + 业财过账 + reverse）
- [x] 相关文档对齐（`assets/inventory.md` 创建、use-cases.md 标注、roadmap 更新）
- [x] 已运行验证：`mvn clean install -DskipTests`（154+ 模块）+ `mvn test -pl module-assets/erp-ast-service`（0 failures/0 errors，66 tests）+ ORM `xmllint --noout` + action-auth well-formed
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 周期性自动盘点（nop-job 定时生成盘点单）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期手工创建盘点单满足 UC-AST-09 核心流程；定时自动盘点属运营自动化能力面。
- Successor Required: `yes`（触发条件：盘点 cron 需求上线 + nop-job 接线 `erp-ast-inventory` 时）

### 条码/RFID/移动端扫码盘点

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 实物盘点可手工录入实盘数量满足流程；扫码属采集渠道增强。
- Successor Required: `yes`（触发条件：移动盘点需求上线时）

### 盘点工作台 AMIS 深度定制页

- Classification: `optimization candidate`
- Why Not Blocking Closure: codegen 标准列表/编辑页满足本期功能验证；深度定制属前端面。
- Successor Required: `yes`（触发条件：盘点 UX 优化需求时）

## Closure

Status Note: 执行者已完成全部 4 Phase + 7 客观 Closure Gates；第 8 项「结束审计由独立子代理执行」由独立 closure auditor 子代理（新会话，冷重播无执行者上下文）核实仓库语义后勾选并填充下方 Auditor/Agent/Evidence 字段（AGENTS.md 规则 12）。8/8 Closure Gates 全闭合。

Executor Evidence (2026-07-07):
- `mvn clean install -DskipTests`（全工作区 154 模块）：BUILD SUCCESS
- `mvn test -pl module-assets/erp-ast-service`：66 tests，0 failures / 0 errors（含新增 `TestErpAstInventory` 5 cases）
- `xmllint --noout module-assets/model/app-erp-assets.orm.xml`：well-formed
- 新增/修改文件清单：ORM `app-erp-assets.orm.xml`（+2 实体 +3 字典）/ finance `app-erp-finance.orm.xml`（+1 业务类型）/ `ErpFinBusinessType.java`（+460）/ `IErpAstInventoryBiz.java` + `ErpAstInventoryBizModel.java` + `ErpAstInventoryProcessor.java` + `AssetInventoryPostingDispatcher.java` + `AssetInventoryAcctDocProvider.java` + `ErpAstConstants.java` + `ErpAstErrors.java` + `app-service.beans.xml`（+3 Bean）/ owner doc `docs/design/assets/inventory.md`（新建）/ `docs/design/assets/use-cases.md`（UC-AST-09 落位）/ `docs/backlog/extended-roadmap.md`（2.5b done）/ `docs/logs/2026/07-07.md`（新条目）/ `TestErpAstInventory.java`（新建）。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理（新会话，冷重播无执行者上下文，session 与 EXECUTE 不同）
- Evidence: 实时仓库语义核实通过——(1) Phase 1：`module-assets/model/app-erp-assets.orm.xml:1114` `ErpAstInventory` + `:1199` `ErpAstInventoryLine` 实体落地，`:137`/`:145` `erp-ast/inventory-status` + `erp-ast/variance-type` 字典物化；`module-finance/erp-fin-dao/.../ErpFinBusinessType.java:58` `ASSET_INVENTORY_ADJUSTMENT(460)` + `erp-fin/business-type.dict.yaml:185` + finance ORM `:108` 三处对齐；`docs/design/assets/inventory.md` 存在含状态机/差异处置/科目映射节。(2) Phase 2：`IErpAstInventoryBiz` + `ErpAstInventoryBizModel`(84 行) + `ErpAstInventoryProcessor`(489 行) 落地，protected step `expandAssetsToLines:170`/`calculateVariance:229`/`handleSurplusCreateCard:277`/`handleShortageTriggerDisposal:322` 均真实且运行时调用（`:90`/`:269`/`:271`），非空壳。(3) Phase 3：`AssetInventoryAcctDocProvider`(98 行) `supportedTypes()` 返回 `EnumSet.of(ASSET_INVENTORY_ADJUSTMENT)` 非 null；`AssetInventoryPostingDispatcher` + Provider 经 `app-service.beans.xml:50-53` 注册为 Bean（anti-hollow 通过）；`reverse` 经 dispatcher `:59` 走 `ASSET_INVENTORY_ADJUSTMENT` 红字。(4) Phase 4：`TestErpAstInventory`(359 行) 含 5 `@Test`（全流程盘盈盘亏/非法迁移/cancel/reverse 回退/范围空拒）；`docs/design/assets/use-cases.md:162` UC-AST-09 标实现落位；`docs/backlog/extended-roadmap.md:28` 2.5b `done`；`docs/logs/2026/07-07.md` 含本计划条目。(5) 五点一致性：Plan Status completed / 4 Phase 全 completed / 各 Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / 日志一致。(6) Deferred honesty：Deferred But Adjudicated 3 项均为 Non-Goal 增强面（自动盘点/扫码/前端深度定制）带触发条件，非范围内缺陷或契约漂移隐藏；Follow-up「盘盈/盘亏处置链完整复用」已在 owner doc `inventory.md` §八公开偏离说明，非隐藏漂移。(7) 反松弛：执行项目 + 退出标准无 `optional`/`if time permits`/`consider`/`maybe` 等模糊词。无 BLOCKER，计划可关闭。

Follow-up:

- 周期性自动盘点 / 扫码盘点 / 盘点工作台前端深度定制（均带触发条件，见 Deferred But Adjudicated）
- **盘盈/盘亏处置链完整复用**（见 inventory.md §八）：当前默认实现走直接建卡/SCRAPPED 避免双重过账；触发条件=盘盈/盘亏要求生成独立 CAPITALIZATION/DISPOSAL 凭证时，下游可覆盖 `handleSurplusCreateCard`/`handleShortageTriggerDisposal` 经注入的 `IErpAstAssetCapitalizationBiz`/`IErpAstDisposalBiz` 走完整链。
