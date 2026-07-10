# 2026-07-10-1100-3-landed-cost-allocation 到岸成本分摊

> Plan Status: active
> Last Reviewed: 2026-07-10 (iteration 2 — consensus)
> Source: `docs/design/finance/costing-methods.md:302-365` 设计章节 + 5 个 plan Deferred 解除 + logistics path-2 运费过账阻塞解除
> Related: `core-business-roadmap.md:76` Non-Goal 声明；解除 5 个 plan Deferred：`2026-07-02-1538-1`（LANDED_COST_SUPPLEMENT 算法）、`2026-07-04-1115-3`（logistics path-2 运费过账）、`2026-07-05-2352-3`（LANDED_COST_SUPPLEMENT 码值预留 + 成本调整 Deferred）、`2026-07-05-0427-2`（StandardCosting Landed Cost 注记）、`2026-07-02-0300-1`（采购到付款到岸成本段）
> Audit: required

## Current Baseline

### 已实现

- **成本调整单** `ErpInvCostAdjust`/`ErpInvCostAdjustLine`：通用成本调整（adjustType=PURCHASE_PRICE_ADJUST/COST_DIFFERENCE/STANDARD_REVALUATION/**LANDED_COST_SUPPLEMENT**），`module-inventory/model/app-erp-inventory.orm.xml:1201-1294`。`LANDED_COST_SUPPLEMENT` 码值已预留但分摊算法未实现（plan 2352-3 Deferred）
- **成本调整处理链** `ErpInvCostAdjustProcessor` + `CostAdjustmentService`：审核→更新 `ErpInvCostLayer`（用 `incomingMoveId=-行ID` 哨兵追加 delta 层）→ `CostAdjustmentAcctDocProvider`(businessType=COST_ADJUSTMENT 420) 生成凭证
- **FIFO 成本层** `ErpInvCostLayer`：remainingQuantity 消耗 + incomingMoveId 追加层范式成熟
- **WEIGHTED_AVERAGE / FIFO / STANDARD** 三种计价方法已实现（plan 1538-1/0427-2）
- **采购入库** `ErpPurReceive`/`ErpPurReceiveLine`：金额（amount）、物料（materialId）、仓库（warehouseId）、数量（quantity）字段齐备，可作分摊基数
- **AcctDocProvider 注册体系**：28 个实现 + `ErpFinAcctDocRegistry`（IoC collect-beans + 业务类型 fail-fast），新 Provider 可直接注册

### 剩余差距

- **无到岸成本实体**：无独立的到岸成本单（费用录入 + 分摊方法选择 + 分摊结果）
- **无分摊引擎**：按金额/数量/重量比例分摊算法未实现
- **无 LANDED_COST 过账 Provider**：到岸成本凭证（借存货/贷应付）未接入
- **logistics path-2 阻塞**：采购运费过账（`LogisticsFreightProvider` FREIGHT 310）的到岸成本分摊路径未通

### 设计来源

`docs/design/finance/costing-methods.md:302-365` 完整定义了到岸成本分摊的 4 步流程和算例：

```
步骤1：录入到岸成本单（关联采购入库单 + 录入各项费用 + 选择分摊方法）
步骤2：分摊计算（按金额/数量/重量比例）
步骤3：更新入库成本（入库行成本 += 分摊费用 → 更新库存成本层）
步骤4：生成凭证（借：存货 / 贷：应付账款）
```

### 对标依据

| 开源 ERP | 到岸成本 | 状态 |
|----------|---------|------|
| **ERPNext** | Landed Cost Voucher（费用录入 + 按比例分摊 + 更新估值） | 核心内置 |
| **Metasfresh** | Landed Cost（成本要素 + 分摊 + 过账） | 核心内置 |
| **本项目** | 仅预留字典码值 | **gap** |

## Goals

- 实现到岸成本单（头/行），支持关联采购入库单 + 录入多项费用要素（运费/保险/关税/清关/其他）
- 实现分摊引擎，支持按金额/数量/重量三种比例分摊方法
- 审核时更新库存成本层（复用 `ErpInvCostAdjust` + `CostAdjustmentService` 范式）+ 生成 GL 凭证
- 解除 logistics path-2 采购运费过账阻塞
- 解除 5 个 plan 的到岸成本 Deferred

## Non-Goals

- **多段到岸成本**（同一入库单多次分摊的累计管理）——本期仅支持一次性分摊，后续追加归 successor
- **到岸成本预估**（入库前预估到岸成本）——仅支持入库后实际分摊
- **按重量分摊的物料毛重字段物化**——本期 weight 字段从物料主数据读取或手工录入，不新增 ORM 列
- **logistics path-2 完整运费到 AP 流程**——本期仅打通到岸成本分摊引擎；logistics 运费 Provider 的完整 path-2 接线（运费 → 到岸成本单 → 分摊）归 successor

## Task Route

- Type: `app-layer design change` + `implementation-only change`
- Owner Docs: `docs/design/finance/costing-methods.md`（§到岸成本 :302-365）、`docs/design/inventory/cross-domain.md`（§成本层更新）
- Skill Selection Basis: 新增 ORM 实体 + BizModel + AcctDocProvider → nop-backend-dev；GraphQL Engine 测试 → nop-testing

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - ORM 模型：到岸成本实体

Status: planned
Targets: `module-inventory/model/app-erp-inventory.orm.xml`
Skill: nop-backend-dev

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] Decision: 实体结构设计
  - 创建独立实体 `ErpInvLandedCost`（头）+ `ErpInvLandedCostLine`（费用要素行），而非复用 `ErpInvCostAdjust`
  - 理由：到岸成本单有多项费用要素（运费/保险/关税各有不同应付对象）、需选择分摊方法、关联采购入库单——结构比通用成本调整丰富
  - 替代方案 A：复用 `ErpInvCostAdjust`(type=LANDED_COST_SUPPLEMENT)——rejected，费用要素多应付对象和分摊方法选择无法表达
  - 替代方案 B：在采购域建实体——rejected，到岸成本直接操作库存成本层，属库存域职责（与 `ErpInvCostAdjust` 同域）
  - 残留风险：到岸成本审核时创建 `ErpInvCostAdjust`(type=LANDED_COST_SUPPLEMENT) 作为成本层更新的载体——两实体关联但独立
  - Skill: nop-backend-dev

- [ ] Add: `ErpInvLandedCost`（到岸成本单头）
  - 字段：code, orgId, receiveId(→ErpPurReceive, 必填), supplierId(采购供应商, from Receive), currencyId, exchangeRate, totalCostAmount(到岸成本合计), allocationMethod(字典 `erp-inv/landed-cost-alloc-method`: BY_AMOUNT/BY_QUANTITY/BY_WEIGHT), docStatus(字典 `erp-inv/move-status`), approveStatus(字典 `erp-inv/approve-status`), posted(BOOL), postedAt/postedBy, businessDate, remark + 标准审计字段
  - 关系：to-one receive/supplier/currency/org；to-many lines(→ErpInvLandedCostLine)
  - Skill: nop-backend-dev

- [ ] Add: `ErpInvLandedCostLine`（到岸成本费用要素行）
  - 字段：id, landedCostId(必填), lineNo, costElement(字典 `erp-inv/cost-element`: FREIGHT/INSURANCE/DUTY/CUSTOMS_CLEARANCE/OTHER), amount(DECIMAL 20,4, 必填), apPartnerId(应付对象→ErpMdPartner, nullable=与采购供应商相同时为空), remark + 标准审计字段
  - 关系：to-one landedCost/apPartner
  - Skill: nop-backend-dev

- [ ] Add: 字典 `erp-inv/landed-cost-alloc-method`（BY_AMOUNT/BY_QUANTITY/BY_WEIGHT）+ 字典 `erp-inv/cost-element`（FREIGHT/INSURANCE/DUTY/CUSTOMS_CLEARANCE/OTHER）
  - Skill: nop-backend-dev

- [ ] Add: 执行 `mvn clean install -DskipTests`（module-inventory 链）触发增量代码生成
  - Skill: nop-backend-dev

Exit Criteria:

- [ ] ORM 变更后 `mvn clean install -DskipTests`（module-inventory 链）BUILD SUCCESS
- [ ] 生成的 Entity/DAO 类包含 `ErpInvLandedCost`/`ErpInvLandedCostLine`

### Phase 2 - 分摊引擎 + BizModel + Processor

Status: planned
Targets: `module-inventory/erp-inv-service/`
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [ ] Decision: 分摊结果存储方式
  - 分摊结果（每入库行的分摊金额 + 新单位成本）**不独立建表**——在审核时创建 `ErpInvCostAdjust`(type=LANDED_COST_SUPPLEMENT) 行记录分摊结果，作为成本层更新的载体
  - 理由：复用成熟的 `CostAdjustmentService` 成本层更新逻辑（追加 delta 层 + 更新余额），避免重复实现
  - Skill: nop-backend-dev

- [ ] Add: `LandedCostAllocationEngine`（纯函数式引擎，`erp-inv-service/.../support/`）
  - 输入：ErpPurReceiveLine 列表 + ErpInvLandedCostLine 费用要素列表 + allocationMethod
  - 计算逻辑：
    ```
    totalReceiveBase = Σ(receiveLine.amount | quantity | weight)  // 按方法选基数
    for each receiveLine:
        share = receiveLine.base / totalReceiveBase
        allocatedAmount = Σ(costElement.amount) × share
        newUnitCost = receiveLine.unitPrice + allocatedAmount / receiveLine.quantity
    ```
  - 输出：`List<AllocationResult(receiveLineId, materialId, allocatedAmount, newUnitCost)>`
  - 参考：`docs/design/finance/costing-methods.md:341-365` 算例
  - Skill: nop-backend-dev

- [ ] Add: `ErpInvLandedCostBizModel`（CrudBizModel）
  - 标准 CRUD + `defaultPrepareQuery`（按 receiveId/docStatus 过滤）
  - `@BizMutation approve`：审核编排（见下 Processor）
  - Skill: nop-backend-dev

- [ ] Add: `ErpInvLandedCostProcessor`（审核编排）
  - `approve(landedCostId, context)` 步骤：
    1. 加载到岸成本单 + 费用行 + 关联采购入库单 + 入库行（跨域经 `IErpPurReceiveBiz` 只读查询）
    2. 调用 `LandedCostAllocationEngine.allocate(...)` 计算分摊结果
    3. 创建 `ErpInvCostAdjust`(type=LANDED_COST_SUPPLEMENT)，每入库行对应一行 CostAdjustLine（adjustAmount=分摊金额，newUnitCost=新单位成本）
    4. 调用 `CostAdjustmentService.applyCostAdjust(adjust, lines)` **直接更新成本层**（balance/ledger/layer delta 层追加，无过账）——**切勿**经 `ErpInvCostAdjustProcessor.applyCostAdjust` 走完整审核链（其于 :137 独立派发 `COST_ADJUSTMENT(420)` 过账），否则与到岸成本自有的 `LANDED_COST` 过账双重入账
    5. 设置 `ErpInvCostAdjust.posted=true` + `ErpInvLandedCost.posted=true`
    6. 状态迁移 SUBMITTED→APPROVED
  - Skill: nop-backend-dev

- [ ] Add: `IErpInvLandedCostBiz` 接口声明（在 erp-inv-dao）
  - 标准 `ICurdBiz<ErpInvLandedCost>` + `approve` 方法
  - Skill: nop-backend-dev

- [ ] Proof: 单元测试 `TestErpInvLandedCostAllocationEngine`
  - 纯函数引擎测试：BY_AMOUNT/BY_QUANTITY/BY_WEIGHT 三种方法 + 算例验证（对照 costing-methods.md:341-365）
  - Skill: nop-testing

Exit Criteria:

- [ ] 分摊引擎纯单元测试全绿（3 种分摊方法 + 算例验证）
- [ ] `ErpInvLandedCostProcessor.approve` 正确创建 CostAdjust 并更新成本层

### Phase 3 - GL 过账 Provider + 测试

Status: planned
Targets: `module-inventory/erp-inv-service/`（AcctDocProvider）、`module-inventory/erp-inv-service/src/test/`
Skill: nop-backend-dev, nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [ ] Decision: 到岸成本凭证科目映射
  - 借：存货科目（1401，从物料主数据 `materialSubject` 解析，与 PURCHASE_INPUT 同科目）
  - 贷：应付账款（2202，从费用行的 `apPartnerId` 解析应付对象；无 apPartnerId 时用采购供应商）
  - 替代方案：贷方按费用要素分科目（运费→5001 销售费用/关税→6401 税金及附加）——rejected，到岸成本分摊的核心语义是"费用资本化到存货"，贷方是"应付给服务商"，不是费用化
  - 残留风险：实际部署时科目映射可能按客户需求调整——通过 AcctDocProvider 配置化
  - Skill: nop-backend-dev

- [ ] Add: `LandedCostAcctDocProvider`（implements `IErpFinAcctDocProvider`）
  - businessType = `LANDED_COST`(新增码值 **490**，在 `ErpFinBusinessType` 枚举中注册——码值 430 已被 `PROJECT_SETTLEMENT` 占用，420–480 均已占用，下一个空闲值为 490)
  - 凭证行生成：每入库行 → 借方行（存货科目, 金额=分摊金额）；每费用要素 → 贷方行（应付科目, 金额=费用金额）
  - 注册到 `app-service.beans.xml` via IoC collect-beans
  - Skill: nop-backend-dev

- [ ] Add: `ErpInvLandedCostProcessor` 审核时触发过账
  - 在步骤 5 后调用 `IErpFinPostingDispatcher.tryPost(...)`（复用现有过账管道）
  - Skill: nop-backend-dev

- [ ] Add: `ErpInvErrors`（或复用现有）新增错误码
  - `ERR_LANDED_COST_RECEIVE_NOT_APPROVED`（关联入库单未审核）
  - `ERR_LANDED_COST_ALREADY_ALLOCATED`（入库单已有到岸成本单且已审核——防重复分摊）
  - Skill: nop-backend-dev

- [ ] Proof: GraphQL Engine 集成测试 `TestErpInvLandedCostEndToEnd`
  - 场景 1（BY_AMOUNT）：入库单 2 行（物料A 1000/物料B 500）+ 到岸成本 180 → 分摊 A=120/B=60 → 成本层更新 → 凭证（借存货 180 / 贷应付 180）
  - 场景 2（BY_QUANTITY）：同上但按数量分摊
  - 场景 3（多应付对象）：运费 150 应付物流商 + 保险 30 应付保险公司 → 凭证两条贷方行
  - 场景 4（防重复分摊）：同一入库单第二张到岸成本单审核 → 拦截
  - Skill: nop-testing

Exit Criteria:

- [ ] GraphQL Engine 集成测试全绿（≥4 场景）
- [ ] 到岸成本审核 → 成本层更新 + GL 凭证生成 全链路验证

### Phase 4 - 前端页面

Status: planned
Targets: `module-inventory/erp-inv-web/`
Skill: nop-frontend-dev

- Item Types: `Add`
- Prereqs: Phase 3

- [ ] Add: 到岸成本单列表页 + 编辑页（头 + 费用行 grid + 分摊预览）
  - action-auth 菜单注册（inventory 域「成本管理」分组下）
  - 编辑页含分摊预览按钮（调用 GraphQL `allocate` query 预览分摊结果，不落库）
  - Skill: nop-frontend-dev

Exit Criteria:

- [ ] 页面 YAML 通过 AMIS 加载无报错

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0b659dacaffejXxUZs26V6AVHJ) — B1 businessType 码值 430 与 `PROJECT_SETTLEMENT(430)` 硬冲突（`ErpFinBusinessType.java:55`，420–480 均已占用）；B2 内部 `ErpInvCostAdjust` 载体若经 `ErpInvCostAdjustProcessor.apply` 会与 `LANDED_COST` 过账双重入账，且方法名 `execute` 不存在（实为 `applyCostAdjust` :64）；baseline 全部 10 项已核实。
- Independent draft review iteration 2: accept (ses_0b6453b78ffe784mFWlHpomRzC) — B1 码值冲突已解（LANDED_COST=490，430=PROJECT_SETTLEMENT 已占，enum 尾 480 已核实 490 空闲）；B2 双重过账已解（applyCostAdjust :64 无过账，Processor.applyCostAdjust :120/:137 独立派发 420，禁走 Processor 警告成立）；Related 5 个 Deferred 已齐；baseline/设计对齐/GL 映射/两实体/跨域/anti-slack/类型/技能/退出标准回归全 PASS；无新阻塞项。**草案审查收敛，状态 draft→active。**

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（`docs/design/finance/costing-methods.md` §到岸成本 标注已实现；`core-business-roadmap.md:76` Non-Goal 标注修正；5 个 plan Deferred 标注解除）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ `mvn test -pl module-inventory/erp-inv-service`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### logistics path-2 完整运费到 AP 流程

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期实现到岸成本分摊引擎；logistics 运费 Provider → 自动创建到岸成本单的完整编排归 successor
- Successor Required: yes（触发条件：logistics 真实承运商集成需求落地时）

### 多段到岸成本累计管理

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期仅支持一次性分摊；同一入库单多次到岸成本追加归 successor
- Successor Required: yes（触发条件：多段运输到岸成本分次录入需求时）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- logistics path-2 运费 → 到岸成本单自动编排（解除 `2026-07-04-1115-3` path-2 Deferred）
