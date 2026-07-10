# 2026-07-10-1100-5-manufacturing-receipt-posting 制造完工入库与领料 GL 过账

> Plan Status: active
> Last Reviewed: 2026-07-10
> Source: `docs/design/manufacturing/state-machine.md` §2 L52/§7 L105/§实现偏离补注 L175 + `0704-2` Deferred（posted=false Non-Goal）+ `2237-1` Deferred
> Related: `2026-07-10-0704-2` 制造链 E2E Deferred successor；`2026-07-05-1838-2` 生产差异过账（互补侧已 done）；`2026-07-02-1538-1` 成本引擎（前置已 done）
> Audit: required

## Current Baseline

### 已实现

- **完工入库库存侧**：`reportCompletion` 创建 `ErpInvStockMove`(moveType=MANUFACTURE, relatedBillType=ERP_MFG_WORK_ORDER) → `StockMoveBookkeeper.bookCompletion` 按计价方法（MOVING_AVERAGE/FIFO/STANDARD）更新库存余额与成本层。`module-manufacturing/erp-mfg-service/`
- **领料出库库存侧**：`MaterialIssue.confirm` 创建 `ErpInvStockMove`(moveType=OUTGOING, relatedBillType=ERP_MFG_ISSUE) → 按计价方法扣减库存余额与成本层
- **生产差异过账**：`ProductionVarianceDispatcher` + `ProductionVarianceAcctDocProvider`(PRODUCTION_VARIANCE=400)，config-gated 默认关，生成 WIP↔差异科目凭证（借/贷 1410~1415）。`module-manufacturing/erp-mfg-service/posting/`
- **成本计算**：WorkOrder 累积 `materialCost`（来自领料）+ `laborCost`（来自报工）→ `totalCost` / `unitCost` 在 `reportCompletion` 时计算
- **标准成本解析**：`StandardCostResolver.resolve(materialId)` 从 `ErpMfgCostRollupLine` FIRMED 行取标准成本

### 剩余差距

- **完工入库 GL 过账缺失**：`InvPostingDispatcher.resolveBusinessType:160` 对 moveType=MANUFACTURE 无分支 → 兜底 `return null` → 不构造 PostingEvent → `move.posted` 恒 false。`ErpFinBusinessType` 枚举无 `MANUFACTURING_RECEIPT` 码值
- **领料出库 GL 过账错误/缺失**：moveType=OUTGOING + relatedBillType=ERP_MFG_ISSUE 不在 InvPostingDispatcher 跳过列表中 → 命中 `SALES_OUTPUT` 分支（L158）→ 生成**错误的销售出库凭证**（Dr: COGS / Cr: Inventory），而非正确的生产领料凭证（Dr: WIP / Cr: Inventory）
- **制造业财一体闭环缺口**：生产成本流转链（存货→WIP→产成品）GL 凭证缺失，仅库存余额正确
- **E2E 阻塞**：`0704-2` 制造链 E2E 断言 `posted=false`（因本期 Non-Goal）

### 设计意图

`docs/design/manufacturing/state-machine.md:52`：IN_PROCESS→COMPLETED 结果「全部完工入库，**生成成本结转凭证**」；§7 L105「成本结转凭证 = 财务域监听工单完工」；§实现偏离补注 L175 显式标注为 Non-Goal，等待 successor。

### 对标依据

| 开源 ERP | 制造完工入库过账 | 状态 |
|----------|----------------|------|
| **Odoo** | `mrp.account` 模块：完工入库生成 Dr: Inventory / Cr: WIP | 核心内置 |
| **ERPNext** | Work Order 完工自动生成 Stock Entry + GL Entry | 核心内置 |
| **Metasfresh** | Production 完工 GL 过账（WIP→产成品） | 核心内置 |
| **本项目** | 库存余额正确，**GL 凭证完全缺失** | **gap** |

## Goals

- 实现完工入库 GL 过账（Dr: 产成品存货 / Cr: WIP 或生产成本），支持 MOVING_AVERAGE/FIFO/STANDARD 三种计价方法
- 实现生产领料 GL 过账（Dr: WIP / Cr: 原材料存货），修复当前误派 SALES_OUTPUT 的问题
- 形成完整的制造业财一体成本流转闭环：原材料存货 →（领料）→ WIP →（完工）→ 产成品存货
- 解除 `0704-2` posted=false Deferred，更新制造链 E2E 断言为 posted=true

## Non-Goals

- **直接人工/制造费用 GL 过账**（Dr: WIP / Cr: 应付职工薪酬/制造费用分配）——本期仅处理实物侧库存移动触发的过账（领料+完工）；人工/制费的计提与分配归独立 successor
- **生产差异自动计算触发**——`erp-mfg.variance-auto-calc-enabled` 仍默认关，由 `ProductionVarianceDispatcher` 独立处理
- **多步完工入库**（部分完工的 WIP 余额管理）——本期按单次完工处理
- **副产品和联产品成本分配**——归 successor

## Task Route

- Type: `app-layer design change`（新增业务类型 + 新增 AcctDocProvider + 修改 InvPostingDispatcher）
- Owner Docs: `docs/design/manufacturing/state-machine.md`（§2/§7/§实现偏离补注）、`docs/design/finance/posting.md`（AcctDocProvider 注册体系）、`docs/design/finance/costing-methods.md`（§成本流转）
- Skill Selection Basis: 新增 AcctDocProvider + 修改 InvPostingDispatcher → nop-backend-dev；GraphQL Engine 测试 → nop-testing

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 业务类型枚举 + InvPostingDispatcher 修正

Status: planned
Targets: `module-finance/erp-fin-dao/.../ErpFinBusinessType.java`、`module-inventory/erp-inv-service/.../posting/InvPostingDispatcher.java`
Skill: nop-backend-dev

- Item Types: `Decision | Fix | Add`
- Prereqs: none

- [ ] Decision: 领料过账架构选择
  - **选择 A**：在 InvPostingDispatcher 中为 ERP_MFG_ISSUE 添加跳过，由 manufacturing 域独立 Dispatcher 处理（镜像 `ProductionVarianceDispatcher` 显式调用范式）
  - 替代方案 B：在 InvPostingDispatcher 中映射 ERP_MFG_ISSUE OUTGOING → MANUFACTURING_ISSUE 类型，由 InvAcctDocProvider 处理——rejected，WIP 科目解析依赖 manufacturing 上下文（WorkOrder/BOM），不宜放在 inventory 域
  - 残留风险：InvPostingDispatcher 的跳过列表已有 3 项（PUR_RETURN/SAL_RETURN/MNT_SPARE_PART），新增 ERP_MFG_ISSUE 后为 4 项——跳过列表增长需注释说明各跳过项由哪个域独占
  - Skill: nop-backend-dev

- [ ] Fix: `InvPostingDispatcher.resolveBusinessType` 增加 ERP_MFG_ISSUE 跳过 + MANUFACTURE 分支
  - L145-148 跳过列表新增 `RELATED_BILL_TYPE_MNT_SPARE_PART` 同层添加 `RELATED_BILL_TYPE_MFG_ISSUE` → `return null`（领料由 manufacturing 域独占）
  - L151-160 新增 MANUFACTURE 分支：`if (MOVE_TYPE_MANUFACTURING.equals(moveType)) return MANUFACTURING_RECEIPT;`（完工入库由 inventory 域 InvAcctDocProvider 处理，因存货估值属 inventory 职责）
  - Skill: nop-backend-dev

- [ ] Add: `ErpFinBusinessType` 新增 `MANUFACTURING_RECEIPT(490)` 和 `MANUFACTURING_ISSUE(491)`
  - Skill: nop-backend-dev

- [ ] Add: `ErpInvConstants` 新增 `RELATED_BILL_TYPE_MFG_ISSUE = "ERP_MFG_ISSUE"`
  - inventory 域本地副本（同一字面值已存在于 `ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE`），目的：避免 inventory→manufacturing 上行模块依赖，同 `RELATED_BILL_TYPE_MNT_SPARE_PART` 范式
  - Skill: nop-backend-dev

Exit Criteria:

- [ ] InvPostingDispatcher 编译通过，MANUFACTURE 返回 MANUFACTURING_RECEIPT，ERP_MFG_ISSUE 返回 null
- [ ] 现有 InvPostingDispatcher 测试回归无失败

### Phase 2 - 完工入库 AcctDocProvider

Status: planned
Targets: `module-inventory/erp-inv-service/.../posting/`
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [ ] Decision: 完工入库凭证科目映射
  - **借方**：产成品存货 `1403`（从物料类别 `materialCategory.inventorySubject` 解析，与 PURCHASE_INPUT 同科目源）
  - **贷方**：WIP 在制品 `1411`（系统配置 `erp-mfg.wip-subject-code`，默认 `1411`）或生产成本 `5001`（配置可选）
  - 金额：`moveLine.totalCost`（完工入库成本层的总成本，由 StockMoveBookkeeper 已计算）
  - 替代方案：贷方用生产成本 `5001`（不经过 WIP 中转）——rejected，WIP 中转使得部分完工时在制品余额可观测
  - Skill: nop-backend-dev

- [ ] Add: `InvAcctDocProvider` 扩展支持 `MANUFACTURING_RECEIPT`
  - 现有 `InvAcctDocProvider`（module-inventory）已支持 `PURCHASE_INPUT(10)` / `SALES_OUTPUT(20)`
  - 新增 `MANUFACTURING_RECEIPT(490)` 到 `getSupportedBusinessTypes`
  - 新增 `buildManufacturingReceiptFacts` 方法：Dr: Inventory(物料类别存货科目), Cr: WIP(config)
  - billData 键：`WORKORDER_CODE`（摘要用）、`COMPLETION_QTY`、`UNIT_COST`、`TOTAL_COST`
  - Skill: nop-backend-dev

- [ ] Add: 配置项 `erp-mfg.wip-subject-code`（默认 `1411`，WIP 在制品科目）
  - Skill: nop-backend-dev

- [ ] Proof: GraphQL Engine 测试 `TestErpMfgCompletionPosting`
  - 场景 1（MOVING_AVERAGE）：完工入库 → move.posted=true → 凭证（Dr: 1403 / Cr: 1411，金额=totalCost）
  - 场景 2（STANDARD）：完工入库按标准成本过账 → Dr: 1403(standardCost×qty) / Cr: 1411(同额)
  - 场景 3（FIFO）：完工入库 → 成本层追加 → 凭证同结构
  - Skill: nop-testing

Exit Criteria:

- [ ] 完工入库后 `move.posted=true`，存在 MANUFACTURING_RECEIPT 类型 GL 凭证
- [ ] 三种计价方法均正确过账

### Phase 3 - 领料出库 PostingDispatcher + AcctDocProvider

Status: planned
Targets: `module-manufacturing/erp-mfg-service/.../posting/`
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [ ] Decision: 领料过账触发时机
  - **选择**：在 `MaterialIssue.confirm` 完成后（库存出库移动单 DONE 后），显式调用 `ManufacturingIssuePostingDispatcher.dispatchIfApplicable(materialIssueId)`
  - 镜像 `ProductionVarianceDispatcher` 范式（manufacturing 域显式调用，不经 InvPostingDispatcher）
  - 理由：领料凭证的借方科目（WIP）需要 WorkOrder 上下文，属 manufacturing 域职责
  - Skill: nop-backend-dev

- [ ] Add: `ManufacturingIssuePostingDispatcher`（module-manufacturing/erp-mfg-service/posting/）
  - 输入：materialIssueId → 加载 MaterialIssue + 关联 WorkOrder + 关联出库移动单
  - 装配 PostingEvent(businessType=MANUFACTURING_ISSUE, billHeadCode=issue.code+"-MI")
  - billData：`WORKORDER_CODE`、每行 `MATERIAL_CODE`/`MATERIAL_COST`/`INVENTORY_SUBJECT`
  - Skill: nop-backend-dev

- [ ] Add: `ManufacturingIssueAcctDocProvider`（implements IErpFinAcctDocProvider）
  - 支持 `MANUFACTURING_ISSUE(491)`
  - 科目映射：Dr: WIP `1411`(config) / Cr: Inventory `1403`(物料类别存货科目)
  - 金额：每行 `line.amount`（出库移动单行级成本，已由 StockMoveBookkeeper 计算）
  - Skill: nop-backend-dev

- [ ] Add: `MaterialIssueProcessor` 或 `ErpMfgMaterialIssueBizModel.confirm` 增加过账触发
  - 在库存出库移动单 DONE 后调用 `ManufacturingIssuePostingDispatcher.dispatchIfApplicable`
  - 设置 `materialIssue.posted = true`（GL 过账语义，区别于库存出库 posted）
  - Skill: nop-backend-dev

- [ ] Proof: GraphQL Engine 测试 `TestErpMfgIssuePosting`
  - 场景 1（领料过账）：MaterialIssue.confirm → 出库移动 + GL 凭证（Dr: 1411 / Cr: 1403，金额=materialCost）
  - 场景 2（多物料领料）：2 种物料领料 → 凭证 2 行贷方（各物料存货科目）
  - Skill: nop-testing

Exit Criteria:

- [ ] 领料出库后生成 MANUFACTURING_ISSUE 类型 GL 凭证（Dr: WIP / Cr: Inventory）
- [ ] 不再误派 SALES_OUTPUT 凭证

### Phase 4 - 制造链 E2E 断言更新 + 全链路验证

Status: planned
Targets: `tests/e2e/orchestration/mfg-chain.spec.ts`、`module-manufacturing/erp-mfg-service/src/test/`
Skill: nop-testing

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 + Phase 3

- [ ] Fix: 更新 `0704-2` 制造链 E2E posted 断言
  - 领料出库移动 posted=true（MANUFACTURING_ISSUE 凭证存在）
  - 完工入库移动 posted=true（MANUFACTURING_RECEIPT 凭证存在）
  - 新增凭证行数值断言（Dr/Cr 科目 + 金额）
  - Skill: nop-testing

- [ ] Proof: 全链路端到端测试 `TestErpMfgCostFlowEndToEnd`
  - 完整成本流转验证：领料（Dr WIP/Cr Inventory）→ 报工（WIP 累积）→ 完工（Dr Inventory/Cr WIP）→ 差异（Dr/Cr Variance/Cr/Dr WIP，config-gated）
  - WIP 余额验证：完工后 WIP 余额 = materialCost + laborCost − completionCost（如有差异再加/减差异）
  - Skill: nop-testing

Exit Criteria:

- [ ] 制造链 E2E posted=true 断言通过（解除 `0704-2` Deferred）
- [ ] 全链路成本流转 GL 凭证完整（领料→WIP→完工→产成品）
- [ ] WIP 余额可观测且正确

## Draft Review Record

- Independent draft review iteration 1: accept (2026-07-10) — 格式合规（命名/章节/Phase 结构/Item Types/技能标注齐全），基线经实时仓库核验准确（InvPostingDispatcher L141-161 跳过列表 3 项 + L160 兜底 null、ErpFinBusinessType 止于 480 无 MANUFACTURING_RECEIPT、InvAcctDocProvider 仅支持 PURCHASE_INPUT/SALES_OUTPUT、设计文档 §2 L52/§7 L105/§实现偏离补注 L175 引用一致、mfg-chain.spec.ts 存在）。修复 1 项 Major（Closure Gates Maven `-pl` 双标志→逗号分隔）+ 2 项 Minor（ErpInvConstants 常量补上行依赖说明、副产品 Deferred 补 reopen trigger）。无 Blocker，可开始实施。

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（`docs/design/manufacturing/state-machine.md` §实现偏离补注 标注已实现；`0704-2` Deferred 标注解除；`2237-1` Deferred 解除）
- [ ] 已运行验证：`mvn clean install -DskipTests`（全 reactor）+ `mvn test -pl module-manufacturing/erp-mfg-service,module-inventory/erp-inv-service`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 直接人工/制造费用 GL 过账

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期仅处理实物侧库存移动触发的过账（领料+完工）。直接人工计提（Dr: WIP / Cr: 应付职工薪酬）和制造费用分配（Dr: WIP / Cr: 制造费用归集）属独立成本会计面
- Successor Required: yes（触发条件：人工/制费计提与分配需求落地时）

### 副产品/联产品成本分配

- Classification: `optimization candidate`
- Why Not Blocking Closure: 多产出物（主产品+副产品/联产品）的成本分配需联合/分离成本法，属高级制造会计
- Successor Required: yes
- Reopen Trigger: 当工单需产出多种可估值产品（主产品+副产品/联产品）且需对其分别结转成本时

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 直接人工/制造费用 GL 过账 successor
