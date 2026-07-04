# 2026-07-05-0427-2-standard-costing-strategy 标准成本法 + 采购价差

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: deferred 项承接（`2026-07-02-1538-1-inventory-costing-engine.md` Deferred「STANDARD（标准成本）方法」，触发条件「N=2 rollup 落地后」——`2026-07-02-1538-2-manufacturing-bom-routing-rollup.md` 已完成并产出 `ErpMfgCostRollupLine.unitCost`，触发条件已满足）
> Related: `2026-07-02-1538-1-inventory-costing-engine.md`（成本核算引擎，STANDARD Deferred）；`2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`（BOM/工艺成本卷算，标准成本来源）；`docs/design/finance/costing-methods.md`（STANDARD 方法设计）；`docs/design/manufacturing/variance-analysis.md`（差异分析模型 ErpMfgCostVariance）
> Mission: erp
> Work Item: core M1 存货成本核算 STANDARD 方法（承接 1538-1 Deferred，触发条件已满足）
> Audit: required

## Current Baseline

（实时核实于 2026-07-05）

- **成本核算引擎已分派化**：`StockMoveBookkeeper.bookCompletion` 按物料 `costMethod` 经 `CostMethodResolver` 分派到 `CostingStrategy`（`docs/design/finance/costing-methods.md` §实现注记 plan 1538-1）。`CostMethodResolver.isSupported` 当前仅支持 MOVING_AVERAGE 与 FIFO（1538-1 Closure 注：`isSupported` 仅 10/30）；STANDARD 码值在字典 `erp-md/cost-method` 中存在但回退默认（MOVING_AVERAGE），非真实标准成本核算。
- **标准成本来源已就绪**：`ErpMfgCostRollup`/`ErpMfgCostRollupLine`（plan 1538-2）已落地于 `module-manufacturing`，`ErpMfgCostRollupLine.unitCost` 为制造件卷算标准单位成本（_app.orm.xml:1728/1780；model:812/837）。
- **差异分析模型已设计**：`docs/design/manufacturing/variance-analysis.md` 定义 `ErpMfgCostVariance`（varianceType PRICE_USAGE/LABOR_EFFICIENCY/...，standardAmount/actualAmount/varianceAmount）及「工单完工→差异计算→差异入账」流程；本期（1538-1）未实现任何差异捕获。
- **过账通道可扩展**：`IErpFinAcctDocProvider`/业务类型字典范式（1538-1 FIFO 经既有 `InvPostingDispatcher` 读 `ledger.totalCost` 拾取）；新增业务类型（如 PURCHASE_PRICE_VARIANCE）经 `ErpFinBusinessType` 加性扩展（保护区域契约，需同步字典，参照 0831-2 薪酬过账多类型同步范式）。
- **既有测试为回归门控**：1538-1 移动加权平均 + FIFO 端到端套件全绿为本计划回归基线（`TestErpInvStockMoveBookkeeping` 5 + `TestErpInvFifoCosting` 6 + `TestErpInvFifoCostingEndToEnd` 3 + `TestErpInvCostingDispatch` 6）。

剩余差距：STANDARD 方法空转（回退移动加权平均），无标准成本解析、无采购价差捕获、`variance-analysis.md` 差异模型未落地。

## Goals

- 新增 `StandardCostingStrategy implements CostingStrategy`：STANDARD 物料的入库/出库按产品标准成本记账（出库 unitCost=标准成本；入库按标准成本入账，实际与标准差异分离）。
- 新增 `StandardCostResolver`：解析产品当前标准成本（首选最近 approved `ErpMfgCostRollupLine.unitCost`，回退物料主数据标准成本列若存在，均无则抛 `ERR_STANDARD_COST_NOT_AVAILABLE`）；`CostMethodResolver.isSupported` 增 STANDARD。
- 采购价差（PPV）捕获：采购入库 DONE 时，实际入库单位成本与标准成本之差 × 数量 = PPV，经新业务类型 `PURCHASE_PRICE_VARIANCE` 过账（借存货按标准 / 借/贷价差科目 / 贷暂估应付按实际）。

## Non-Goals

- **生产差异**（材料用量差异 / 人工效率差异 / 产量差异等）：归 `variance-analysis.md` 工单完工触发面，本期仅采购价差（PPV）。生产差异留后继。
- **标准成本更新/重估流程**（成本调整单 + 审批 + 重估凭证）：归 1538-1 Deferred「成本调整单」，本期标准成本只读消费 rollup 输出。
- **BATCH / INDIVIDUAL / 全月一次 / LIFO / 到岸成本 Landed Cost**：仍归 1538-1 Deferred（各自独立结果面 / 需 ORM 列）。
- **多账套并行成本 / 存货减值（成本与可变现净值孰低）/ 成本报表**：归 1538-1 Deferred。
- **STANDARD 物料 FIFO/移动平均余额迁移**：本期新启用 STANDARD 的物料从启用点起按标准成本，不回溯重算历史余额。

## Task Route

- Type: `implementation-only change`（承接已裁定 Deferred；新增 CostingStrategy 实现 + 标准 cost 解析 + 业务类型加性扩展，无既有契约破坏）
- Owner Docs: `docs/design/finance/costing-methods.md`（§成本核算方法 STANDARD 行 + §Non-Goal STANDARD 项收口 + 标准成本解析来源补注）；`docs/design/manufacturing/variance-analysis.md`（PPV 落地范围 + 生产差异后继触发条件）
- Skill Selection Basis: 涉及 BizModel/记账器策略 + 业财过账业务类型扩展 + 跨域读制造域 rollup，加载 `nop-backend-dev`（跨实体访问、过账 Provider 范式）。

### Key Decisions

- **Decision: 标准成本来源真相源**
  - 选择：`StandardCostResolver` 解析顺序——(1) 最近一条 `status=APPROVED` 的 `ErpMfgCostRollupLine`（按 materialId 取最新 rollup 的行 unitCost）；(2) 物料主数据标准成本列若存在（fallback，config-gated）；(3) 均无抛 `ERR_STANDARD_COST_NOT_AVAILABLE`。
  - 替代方案：(a) 物料主数据物化 `standardCost` 列由 rollup 回写——拒绝，物化列引入同步一致性问题且 rollup 已是真相源；(b) 仅 rollup——拒绝，采购件（非制造件）无 rollup，需 fallback。
  - 残留风险：采购件若主数据亦无标准成本则该物料不能用 STANDARD（抛错为合理门控）。
- **Decision: PPV 过账科目与业务类型**
  - 选择：新增业务类型 `PURCHASE_PRICE_VARIANCE`（`ErpFinBusinessType` 枚举 + 字典同步，参照 0831-2 多类型同步保护区域范式）；凭证分录——借存货（标准 × qty）/ 贷暂估应付（实际 × qty）/ 差额借或贷价差科目；价差科目经会计科目表配置。
  - 替代方案：差异全额计入差异科目、存货全按实际——拒绝，违背标准成本法（存货必须按标准）。
- **Decision: PPV 捕获时机**
  - 选择：采购入库 DONE 时（既有 `StockMoveBookkeeper.bookCompletion` STANDARD 分派路径内）捕获并过账；移动加权平均/FIFO 路径零改动（行为不变为回归门控）。
  - 替代方案：采购发票三单匹配时——拒绝，那属 0300-1 面且时机晚于入库。

## Infrastructure And Config Prereqs

- 新增 config 项：`erp-inv.standard-cost-fallback-to-material-master`（默认 true，启用物料主数据 fallback）、`erp-inv.standard-cost-ppv-enabled`（默认 true，PPV 捕获总开关）。
- 价差科目映射：依赖既有会计科目表配置（ErpMdAcctSchema）；无新基础设施。
- 无数据迁移；回滚策略：`CostMethodResolver.isSupported` 移除 STANDARD + 删 `StandardCostingStrategy`/`StandardCostResolver` + 移除业务类型，STANDARD 物料回退默认（行为回到 1538-1 基线）。

## Execution Plan

### Phase 1 - 标准成本解析 + CostingStrategy 接入

Status: completed
Targets: `module-inventory/erp-inv-service/.../costing/StandardCostResolver.java`；`StandardCostingStrategy.java`；`CostMethodResolver.java`；`ErpInvErrors`（新 ErrorCode）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`: 在计划记录标准成本来源真相源裁决（见 Task Route Key Decisions）
  - Skill: `nop-backend-dev`
- [x] `Add`: `StandardCostResolver`——读最近 approved `ErpMfgCostRollupLine.unitCost`（跨域经 I*Biz `IErpMfgCostRollupBiz` 查询方法声明于 mfg-dao；遵循 1538-1 finance→inventory R 范式），fallback 物料主数据标准成本列（config-gated），均无抛 `ERR_STANDARD_COST_NOT_AVAILABLE`
  - Skill: `nop-backend-dev`
- [x] `Add`: `StandardCostingStrategy implements CostingStrategy`——入库：按标准成本写 `ledger.unitCost/totalCost`（实际成本经 PPV 通道分离，Phase 2）；出库：unitCost=标准成本，写 `ledger.unitCost/totalCost` 走既有 `InvPostingDispatcher` 拾取（COGS 通道零改动，同 FIFO 范式）
  - Skill: `nop-backend-dev`
- [x] `Add`: `CostMethodResolver.isSupported` 增 STANDARD 码值识别与分派
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅证明 STANDARD 物料出入库按标准成本记账且 COGS 经既有通道流动；PPV 过账在 Phase 2 验证。

- [x] STANDARD 物料出库 `ledger.unitCost` = 标准成本，经 `InvPostingDispatcher` 生成既有 SALES_OUTPUT/PURCHASE_INPUT 凭证（COGS 通道零改动）
- [x] 无标准成本时抛 `ERR_STANDARD_COST_NOT_AVAILABLE`（错误路径）
- [x] 移动加权平均 + FIFO 路径行为不变（既有套件本地化跑通零回归）

### Phase 2 - 采购价差（PPV）捕获 + 过账

Status: completed
Targets: `StandardCostingStrategy`（PPV 分离）；`InvPostingDispatcher` 或新 PPV 过账派发；`ErpFinBusinessType` 枚举 + 字典同步；`module-finance/erp-fin-service` PPV 业务类型注册
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1

- [x] `Decision`: 在计划记录 PPV 科目与业务类型裁决（见 Task Route Key Decisions）
  - Skill: `nop-backend-dev`
- [x] `Add`: 采购入库 DONE 时——实际入库单位成本（既有入库成本来源）与标准成本差额 × qty = PPV；config-gated `erp-inv.standard-cost-ppv-enabled`
  - Skill: `nop-backend-dev`
- [x] `Add`: 新业务类型 `PURCHASE_PRICE_VARIANCE`——`ErpFinBusinessType` 枚举 + 字典同步（保护区域契约，参照 0831-2 同步范式）；PPV 经 `IErpFinVoucherBiz.post` 或既有过账 Provider 生成凭证（借存货标准 / 贷暂估应付实际 / 差额借或贷价差科目）
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] STANDARD 物料采购入库 DONE 生成 PPV 凭证（实际>标准→借价差；实际<标准→贷价差；金额=|实际−标准|×qty）；config 关闭时不生成（标准成本仍记账）

### Phase 3 - 测试 + owner doc 对齐

Status: completed
Targets: 行为测试（inv-service / fin-service）；`docs/design/finance/costing-methods.md`；`docs/design/manufacturing/variance-analysis.md`；`docs/logs/2026/07-05.md`
Skill: `nop-backend-dev`

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] `Proof`: 行为测试——STANDARD 出库按标准成本 COGS / 无标准成本抛错 / PPV 双向（实际>标准、实际<标准）/ config 关闭 PPV 不生成 / 移动加权平均+FIFO 零回归；策略 `JunitAutoTestCase`，`mvn test -pl module-inventory/erp-inv-service -am` + `mvn test -pl module-finance/erp-fin-service -am`
  - Skill: `nop-backend-dev`
- [x] `Add`: owner doc 对齐——`costing-methods.md` §Non-Goal STANDARD 项标记收口 + 标准成本解析来源补注 + PPV 业务类型补注；`variance-analysis.md` 标注本期仅 PPV + 生产差异后继触发条件
  - Skill: none

Exit Criteria:

- [x] 新增 STANDARD 行为测试全绿；1538-1 移动加权平均 + FIFO 套件零回归；finance 过账套件零回归
- [x] owner doc 偏离补注收口

## Draft Review Record

- Independent draft review iteration 1: accept — 格式合规（必需章节齐备、Phase 结构有效、Item Types/Skill/Decision 标注符合 guide 规则 7/8/9）；完整性达标（各阶段 Exit Criteria 可观测可测，Execution Plan 覆盖所有 Closure Gates，反松弛规则无违例，Deferred 项均命名 successor 触发条件）；范围单一结果面（STANDARD 策略 + 标准成本解析 + PPV），Non-Goals 明确；结束证据规则已定义。基线已实时核实：`CostMethodResolver.isSupported` 仅 MOVING_AVERAGE/FIFO、STANDARD 码值在 `cost-method.dict.yaml` 存在但回退默认、4 个回归测试套件文件存在、`ErpMfgCostRollupLine`/`IErpMfgCostRollupBiz`/`ErpFinBusinessType` 均落地、`costing-methods.md` 第 40 行已将 STANDARD 列为 1538-1 Deferred Non-Goal。无 Blocker/Major；Minor：Phase 1 Exit Criteria「出库」行同时列 SALES_OUTPUT/PURCHASE_INPUT 凭证类型（策略双向处理，标签略不精确，留待结束审计/执行时澄清）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（STANDARD 策略 + 标准成本解析 + PPV 捕获过账）
- [x] 相关文档对齐（costing-methods.md / variance-analysis.md / 当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + `mvn test -pl module-inventory/erp-inv-service -am` + `mvn test -pl module-finance/erp-fin-service -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 生产差异（材料用量 / 人工效率 / 费率 / 产量 / 制造费用）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 `variance-analysis.md` 工单完工触发面（ErpMfgCostVariance 模型 + 差异入账）；本期仅采购价差（PPV）。
- Successor Required: yes（触发条件：工单完工差异分析需求落地时）

### 标准成本更新/重估流程（成本调整单 + 审批 + 重估凭证）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 1538-1 Deferred「成本调整单」；本期标准成本只读消费 rollup 输出。
- Successor Required: yes（触发条件：标准成本周期重估/调整单需求落地时）

### BATCH / INDIVIDUAL / 全月一次 / LIFO / 到岸成本 / 多账套 / 存货减值 / 成本报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 仍归 1538-1 Deferred（各自独立结果面 / 需 ORM 列 / 属 nop-report）。
- Successor Required: yes（触发条件：各方法/到岸成本/报表需求落地时）

## Closure

Status Note: 三阶段全部完成。STANDARD 计价方法（`StandardCostingStrategy`）+ 标准成本解析（`StandardCostResolver`）+ 采购价差捕获（`InvPostingDispatcher.dispatchPurchasePriceVariance` + `PURCHASE_PRICE_VARIANCE` 业务类型 + `PurchasePriceVarianceAcctDocProvider`）全部落地。inv-service 55 tests（50 既有零回归 + 5 新增 STANDARD）、fin-service 148 tests（零回归）、mfg-service 56 tests（零回归）全绿；`mvn clean install -DskipTests` 全仓 BUILD SUCCESS。

结束审计修复（re-execution after closure audit）：首轮独立结束审计发现 Phase 2 字典同步只落地枚举侧（`ErpFinBusinessType.PURCHASE_PRICE_VARIANCE(330)`）而字典 YAML 未同步——根因是字典 YAML（`business-type.dict.yaml` 带 `__XGEN_FORCE_OVERRIDE__`）由 codegen 从 ORM 模型 `<dict>` 定义重生成，直接编辑 YAML 会被下次构建覆盖。正确修复已应用：在 ORM 模型真相源 `module-finance/model/app-erp-finance.orm.xml` business-type dict 增 `<option code="PURCHASE_PRICE_VARIANCE" label="采购价差" .../>`，经 `erp-fin-codegen` postcompile 重生成 → YAML 已含 `value: PURCHASE_PRICE_VARIANCE`（source + target/classes 双重核实）。重跑 `mvn clean install -DskipTests`（全仓 BUILD SUCCESS）+ `mvn test -pl module-inventory/erp-inv-service -am`（55 绿）+ `mvn test -pl module-finance/erp-fin-service -am`（148 绿），0831-2 保护区域「枚举 + 字典」同步契约现在双侧落地。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（首轮，opencode build agent，新会话）；缺陷修复 + 复核由执行驱动（本轮）
- Evidence: 首轮独立审计核实产物存在（`StandardCostingStrategy`/`StandardCostResolver`/`CostMethodResolver`/`InvPostingDispatcher`/`PurchasePriceVarianceAcctDocProvider` 均落地）但发现 Phase 2 保护区域契约漂移：`PURCHASE_PRICE_VARIANCE` 枚举已加（`ErpFinBusinessType.java:45`）而字典未同步（`business-type.dict.yaml` 末于 `BANK_RECON_ADJ`，无 PPV，全仓 `*.dict.yaml` 无 PPV 提及），0831-2 范式要求双侧落地。返回 `issues`，closure gate 未关。本轮修复根因（字典 YAML 为 codegen 生成产物，真相源为 ORM 模型 `<dict>`）：在 `module-finance/model/app-erp-finance.orm.xml` business-type dict 增 `PURCHASE_PRICE_VARIANCE` option → `erp-fin-codegen` postcompile 重生成 → YAML source/target 双重含 PPV。复核：`mvn clean install -DskipTests` 全仓 BUILD SUCCESS；`mvn test -pl module-inventory/erp-inv-service -am` 55 绿；`mvn test -pl module-finance/erp-fin-service -am` 148 绿。保护区域双侧同步契约已满足。

Follow-up:

- 生产差异（见上方 Deferred）
- 标准成本重估/成本调整单（见上方 Deferred）
