# 2026-07-04-0549-1-inventory-vmi-ownership-transfer VMI 所有权转移

> Plan Status: completed
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.10（M2 最后一项）；`docs/design/inventory/consignment.md`
> Related: `2026-07-02-0300-1-purchase-invoice-payment-three-way-match.md`（AP 核销链，VMI 生成的应付在此核销）、`2026-07-02-0700-1-inventory-trace-chain.md`（库存流水/余额模型基线）
> Audit: required

## Current Baseline

- `ErpInvStockBalance`（`module-inventory/model/app-erp-inventory.orm.xml:238`）按 物料×SKU×仓库×库位×批次 汇总，**无 ownerId/ownershipType 维度**——`consignment.md` 标记的 P1 缺口。`StockMoveBookkeeper.findBalance(orgId, materialId, skuId, warehouseId, locationId, batchNo)`（`erp-inv-service/.../stock/StockMoveBookkeeper.java:178`）的余额键不含 owner。
- `ErpInvStockLedger`（orm.xml:193）不可变流水，同样无 owner 维度。
- 物理库存移动经 `ErpInvStockMove` 三层模型（移动单/流水/余额，见 `state-machine.md` + `trace-chain.md`），物理移动 `sourceLocId≠destLocId`。
- 业财过账机制就绪：`IErpFinAcctDocProvider`（`module-finance/erp-fin-dao`）+ 各域 Provider/Dispatcher 范式（`InvAcctDocProvider`/`InvPostingDispatcher` inventory 域内已有）。`ErpFinBusinessType`（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`）当前最大 code=250（CREDIT_FACILITY_INTEREST），下一个空闲 code=260。
- `ErpInvStockMove` 移动单本身**不携带 ownerId**——VMI 货物的「入库即供应商寄售」不在本计划范围（见 Non-Goals）。
- CRUD 全 18 域 done；inventory 既有测试套件（costing/trace/stockmove）全绿。

## Goals

- 为 `ErpInvStockBalance`/`ErpInvStockLedger` 增加 `ownerId`（往来单位 → ErpMdPartner）+ `ownershipType`（自有/供应商寄售/寄售出去/客供料）正交维度，默认关（`ownership-tracking-enabled=false` 时行为不变）。
- 新增 `ErpInvOwnershipTransfer`（所有权转移单）+ `ErpInvOwnershipTransferLine`，专用单据（`sourceLocId=destLocId` 物理位置不变），三态状态机 `DRAFT → CONFIRMED → DONE`（+ `CANCELLED`）。
- DONE 时同库位内对 StockBalance 作 ownershipType/ownerId 调账（法权变更，物理位置不变，不复用移动单物理流程）。
- VMI 消耗（`VMI_SUPPLIER → OWNED`）转移 DONE 时自动生成应付（`OWNERSHIP_TRANSFER` 业财过账，借存货(自有)/贷应付-供应商），待供应商开票后经既有 AP 核销链核销。
- 配置门控：`erp-inv.ownership-tracking-enabled`（默认 false）、`erp-inv.vmi-auto-generate-ap`（默认 true）。

## Non-Goals

- **VMI 货物「入库即供应商寄售」**：需在 `ErpInvStockMove`/`StockMoveLine` 透传 ownerId + 采购入库标记 ownershipType，属独立「VMI 收货流」结果面，本计划仅交付所有权转移机制 + 余额维度。本期 VMI_SUPPLIER 余额（含测试用）通过直接余额种子建立；`VMI_CONSUME` 转移单仅负责消耗（`VMI_SUPPLIER → OWNED`），不负责创建 VMI 库存（转移类型字典无创建 VMI 的语义）。
- **寄售出去（CONSIGNMENT_OUT）/客供料（CUSTOMER_PROVIDED）出库按 owner 匹配的消耗规则**：销售出库时 OWNED 优先消耗、VMI 不发给普通订单的匹配逻辑（`consignment.md §业务规则2`）——需改 sales 出库选库存逻辑，独立结果面。
- **StockLedger ownerId 全链路透传**：本计划为流水加列（前向兼容），标准移动单流水仍写 null/OWNED；VMI 收货流落地时再透传（successor）。
- **多级所有权转移审批工作流引擎**：本期单级 CONFIRMED→DONE。
- **跨币种 VMI 结算汇兑**：生成的应付沿用转移单币种，汇兑重估归既有 EXCHANGE_GAIN_LOSS 期末流程。
- **AMIS 所有权管理页面深度定制**：仅依赖 codegen 标准页 + owner 维度筛选。

## Task Route

- Type: `app-layer design change`（含 ORM 模型变更——`module-inventory/model/app-erp-inventory.orm.xml` 为 ask-first 保护区域，本计划变更经模型驱动 codegen 再生）
- Owner Docs: `docs/design/inventory/consignment.md`（权威设计）、`docs/design/inventory/state-machine.md`（三层模型基线）、`docs/design/finance/posting.md`（过账机制）、`docs/design/finance/reconciliation.md`（AP 核销链）
- Skill Selection Basis: ORM 模型设计 + BizModel 自定义动作 + 业财过账 Provider → 加载 `nop-backend-dev`；状态机自检与跨实体调用约束由该技能路由

## Infrastructure And Config Prereqs

- 配置项 `erp-inv.ownership-tracking-enabled`（默认 false，对应 Odoo feature group 开关，非 VMI 用户无感知）。
- 配置项 `erp-inv.vmi-auto-generate-ap`（默认 true，VMI 消耗转移 DONE 时自动生成应付）。
- 无外部服务/端口/密钥依赖；无数据迁移（bootstrap 阶段无生产数据，新增列有默认值，重建优先）。

## Execution Plan

### Phase 1 - owner 维度建模 + codegen 再生

Status: completed
Targets: `module-inventory/model/app-erp-inventory.orm.xml`；codegen 再生的 dao/_gen/meta；`StockMoveBookkeeper.java`；新字典 `erp-inv/ownership-type`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] Add: `ErpInvStockBalance` 加 `ownerId`（BIGINT → ErpMdPartner，可空）+ `ownershipType`（dict `erp-inv/ownership-type`，默认 OWNED）；`ErpInvStockLedger` 加同两列（流水前向兼容，默认 OWNED）。
  - Skill: `nop-backend-dev`
- [x] Add: 新字典 `erp-inv/ownership-type`（OWNED / VMI_SUPPLIER / CONSIGNMENT_OUT / CUSTOMER_PROVIDED）。
  - Skill: `nop-backend-dev`
- [x] Add: 经 nop-cli codegen 从 ORM 模型再生 inventory dao/_gen/meta（不手改生成代码）。
  - Skill: `nop-backend-dev`
- [x] Decision: `StockMoveBookkeeper.findBalance` 的余额键是否纳入 ownerId——记录选择：纳入但 config-gated（`ownership-tracking-enabled=false` 时 ownerId 一律 null，等价既有行为；启用时方入键）。替代方案：不变 findBalance 签名、改为 ownership transfer 独立维护子余额——被否（双余额源易漂移）。
  - Skill: `nop-backend-dev`
- [x] Add: `StockMoveBookkeeper` 余额键按 ownerId 维度（config-gated），`ownership-tracking-enabled=false` 时行为与基线逐字节一致。
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅交付 owner 维度建模 + codegen 再生 + 余额键门控。完整仓库验证归 Closure Gates。

- [x] codegen 再生后 inventory 编译通过；新增 `ownerId`/`ownershipType` 列在 entity/meta 存在且非空壳
- [x] **本地化回归**：既有 costing/trace 测试（`TestErpInvCostingDispatch`、`TestErpInvFifoCosting`、`TestErpInvStockMove*`）在 `ownership-tracking-enabled=false`（默认）下零回归——证明 owner 维度对既有行为无影响（解除 Phase 2 对余额键稳定性的阻塞）

### Phase 2 - 所有权转移单状态机 + 同库位调账

Status: completed
Targets: `module-inventory/model/app-erp-inventory.orm.xml`（新增实体）；codegen 再生；`ErpInvOwnershipTransferBizModel.java`；新字典 `ownership-transfer-type`/`ownership-transfer-status`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1

- [x] Add: 新实体 `ErpInvOwnershipTransfer`（转移单头：transferType/partnerId/sourceLocId/destLocId/fromOwnershipType/toOwnershipType/businessDate/docStatus/posted 三件套）+ `ErpInvOwnershipTransferLine`（materialId/batchId/quantity/sourceBillType/Code）。
  - Skill: `nop-backend-dev`
- [x] Add: 新字典 `erp-inv/ownership-transfer-type`（VMI_CONSUME / CONSIGNMENT_RETURN / OWNERSHIP_TO_CUSTOMER）、`erp-inv/ownership-transfer-status`（DRAFT / CONFIRMED / DONE / CANCELLED）。
  - Skill: `nop-backend-dev`
- [x] Add: codegen 再生 OwnershipTransfer 实体 dao/IBiz/BizModel/meta/web。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpInvOwnershipTransferBizModel` 状态机动作——`confirm`（DRAFT→CONFIRMED）、`done`（CONFIRMED→DONE，触发同库位 StockBalance ownershipType/ownerId 调账 + 过账派发）、`cancel`（CONFIRMED→CANCELLED）。
  - Skill: `nop-backend-dev`
- [x] Decision: DONE 调账策略——记录选择：同库位内对该 (material×warehouse×location×batch) 的余额**重分类**（改 ownershipType/ownerId，数量不移动）；若启用 owner 维度则按 ownerId 拆出独立子余额行。替代方案：写一条零数量物理移动单——被否（违反 `sourceLocId=destLocId` 物理不变约束，且污染移动单语义）。
  - Skill: `nop-backend-dev`
- [x] Add: 不变量校验——`sourceLocId==destLocId`（物理位置不变）；`ownership-tracking-enabled=false` 时 done 抛错（owner 维度未启用不可调账）；`fromOwnershipType/toOwnershipType` 与 transferType 一致性校验。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 状态机三态迁移 + cancel 可达且非法迁移被拒（Proof 见 Phase 3 测试合并）
- [x] DONE 后 StockBalance 的 ownershipType/ownerId 反映转移结果（同库位调账，数量守恒）

### Phase 3 - OWNERSHIP_TRANSFER 业财过账 + AP 联动 + 端到端

Status: completed
Targets: `module-finance/erp-fin-dao/.../ErpFinBusinessType.java`；`erp-fin/business-type` 字典；`InvOwnershipTransferProvider.java`；`OwnershipTransferPostingDispatcher.java`；端到端测试；`consignment.md`；`extended-roadmap.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2

- [x] Add: `ErpFinBusinessType.OWNERSHIP_TRANSFER(260)` 枚举 + 同步 `erp-fin/business-type` 字典（数值权威源）。
  - Skill: `nop-backend-dev`
- [x] Add: `InvOwnershipTransferProvider implements IErpFinAcctDocProvider`（businessType=OWNERSHIP_TRANSFER；典型 VMI 消耗：借 1401 存货(自有, TOTAL_COST) / 贷 2202 应付-供应商）。范式对照 `InvAcctDocProvider`/`SalAcctDocProvider`。
  - Skill: `nop-backend-dev`
- [x] Add: `OwnershipTransferPostingDispatcher`（done 后组装 PostingEvent 经 PostingExecutor；范式对照 `SalReturnPostingDispatcher`/`TimesheetPostingDispatcher`）。
  - Skill: `nop-backend-dev`
- [x] Decision: VMI 消耗生成应付的形态——记录选择：`OWNERSHIP_TRANSFER` 凭证 + `DIRECTION_PAYABLE` 辅助账（与既有 AP 段一致，openAmount 等待供应商采购发票核销，复用 `ErpFinArApItemGenerator` 既有的 AP_INVOICE/PAYMENT 核销路径）。替代方案：直接生成 AP_INVOICE 凭证——被否（无发票来源，应为暂估应付待核销）。落地：`ErpFinArApItemGenerator.resolveProfile` 新增 OWNERSHIP_TRANSFER → DIRECTION_PAYABLE + SOURCE_BILL_OWNERSHIP_TRANSFER，金额取 TOTAL_COST。
  - Skill: `nop-backend-dev`
- [x] Add: config 门控 `vmi-auto-generate-ap`——VMI_CONSUME 转移 DONE 时按开关决定是否生成应付（默认 true）；非 VMI_CONSUME 类型（CONSIGNMENT_RETURN/OWNERSHIP_TO_CUSTOMER）不过账生成应付（物权内部转移或转客户，无供应商结算）。
  - Skill: `nop-backend-dev`
- [x] Proof: 端到端测试 `TestErpInvOwnershipTransfer`（JunitAutoTestCase）——(a) VMI_CONSUME：直接余额种子建 VMI_SUPPLIER 余额（不依赖未实现的收货流）→ 转移 DONE → StockBalance 重分类 OWNED + OWNERSHIP_TRANSFER 凭证回链 posted=true + DIRECTION_PAYABLE 辅助账 openAmount>0；(b) ownership-tracking-enabled=false 时 done 抛 `ERR_OWNERSHIP_TRACKING_DISABLED`；(c) sourceLocId≠destLocId 抛 `ERR_OWNERSHIP_TRANSFER_LOC_MISMATCH`；(d) 非 VMI_CONSUME 不生成应付。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] VMI_CONSUME 端到端：余额重分类 + 凭证回链 + 应付辅助账 openAmount>0 全部落地可验证
- [x] config 门控与不变量校验在异常路径测试中抛出正确 ErrorCode

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0d607a410ffewRlqZey32Ryx2t`) — VERDICT acceptable as-is，无 BLOCKER。独立子代理逐项核实基线（StockBalance/Ledger 无 ownerId/ownershipType、StockMoveBookkeeper.findBalance 余额键无 owner、ErpFinBusinessType 下一个空闲 code=260、Provider/Dispatcher 范式真实、DIRECTION_PAYABLE 既有），符合 consignment.md §边界。应用 advisory 修订：澄清 VMI_SUPPLIER 余额经直接余额种子建立（转移单只负责 VMI_CONSUME 消耗，无创建 VMI 语义）、Phase 3 Proof (a) 测试种子机制补述。无范围内缺陷隐藏于 Deferred。
- Last Reviewed: 2026-07-04

## Closure Gates

> 完整仓库验证在此处一次运行。

- [x] 范围内行为完成（owner 维度 + 所有权转移单状态机 + OWNERSHIP_TRANSFER 过账 + AP 联动）
- [x] 相关文档对齐（`consignment.md` 实现偏离补注、`extended-roadmap.md` 2.10 标 done、`docs/logs/2026/07-04.md`）
- [x] 已运行验证：`mvn clean install -DskipTests`（全模块，146 reactor BUILD SUCCESS）+ `mvn test -pl module-inventory/erp-inv-service`（49 通过：45 既有零回归 + 4 新增端到端）+ finance 既有套件零回归（93 通过）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### VMI 货物「入库即供应商寄售」收货流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需在 `ErpInvStockMove`/`StockMoveLine` 透传 ownerId + 采购入库标记 VMI_SUPPLIER；属独立「VMI 收货流」结果面，本期 VMI 库存经转移单建立。
- Successor Required: `yes`（触发条件：VMI 收货即寄售流需求时）

### 销售出库按 ownershipType 匹配消耗（OWNED 优先、VMI 不发普通订单）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需改 sales 出库选库存逻辑（`consignment.md §业务规则2`），独立结果面。
- Successor Required: `yes`（触发条件：VMI 库存与自有库存并存出库路由需求时）

### StockLedger ownerId 全链路透传 + 多级转移审批工作流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 流水加列前向兼容（标准移动写 null/OWNED）；审批本期单级。
- Successor Required: `yes`（触发条件：VMI 收货流落地 / 多级审批需求时）

## Closure

Status Note: closed (2026-07-04)

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 `ses_0d5f12b45ffeIVXdwnewF9wwOL`），未参与实现
- Verdict: **PASS** — 19/19 验证项全部确认（Phase 1 owner 维度 / Phase 2 状态机+调账 / Phase 3 过账+AP / 测试 4 用例 / 文档对齐）
- 验证基线（全绿）：
  - `mvn clean install -DskipTests`：146 reactor 模块 BUILD SUCCESS
  - `mvn test -pl module-inventory/erp-inv-service`：49 通过（45 既有零回归 + 4 新增 `TestErpInvOwnershipTransfer`）
  - `mvn test -pl module-finance/erp-fin-service`：93 通过（generator + business-type 改动零回归）
- Red flags: 无；`transferType` VARCHAR(30) 容纳 `OWNERSHIP_TO_CUSTOMER`（21 字符）；`_gen/` 正确再生；beans 正确注册

Follow-up:

- VMI 收货即寄售流（见 Deferred）
- 销售出库按 owner 匹配消耗（见 Deferred）
