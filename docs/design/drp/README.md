# DRP（分销需求计划，drp）

## 目的

设计分销需求计划（Distribution Requirements Planning）模块：多级分销网络中的库存补货计划。与 MRP（制造端物料需求）形成互补——MRP 计算"生产什么"，DRP 计算"从哪里调拨/采购什么到哪个仓库"。

## 边界

- 本模块负责：多仓库库存网络建模、分销需求计算（基于安全库存+预测+已分配量→净需求）、补货建议（仓间调拨/向供应商采购）、补货单自动生成。
- 本模块不负责：制造端 MRP 物料需求（见 `../manufacturing/mrp.md`）；实际库存移动（inventory 域）。
- 前置条件：启用了多仓库管理 + 批次追溯（可选），各仓库维护了安全库存/补货策略。
- 持久化字段、字典、状态码以 `module-drp/model/app-erp-drp.orm.xml` 为权威源。
- 跨域协作规则见 `../domain-design-guidelines.md`，全局流程见 `../flow-overview.md`。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-drp` |
| appName | `erp-drp`（两级） |
| 权威模型 | `module-drp/model/app-erp-drp.orm.xml` |
| 实体包 | `app.erp.drp.dao.entity` |
| 表前缀 | `erp_drp_`（主体）；`erp_inv_drp_`（历史命名例外，见下文命名例外登记） |
| 类名前缀 | `ErpDrp*`（主体）；`ErpInvDrp*`（历史命名例外） |
| 字典命名空间 | `erp-drp/*`；`erp-inv/drp-*`（历史命名例外，见 §命名例外登记 F2e） |

## 核心业务对象

| 对象 | 业务含义 |
|------|----------|
| DRP 计划头（ErpDrpPlan） | 一次 DRP 运行的主记录：计划名称、覆盖区间、运行时间/运行人、状态（草稿/已计算/已批准/已执行）、总补货数量（派生） |
| DRP 明细行（ErpDrpLine） | 按物料×目标仓库的净需求计算结果：当前库存、已分配量、在途在单量、预测需求量、安全库存、净需求、建议补货量、批准补货量、补货类型（仓间调拨/采购）、生成的补货单（回写）、行状态（建议/批准/已下单/取消） |
| 仓库补货参数（ErpDrpParameter） | 按仓库×物料配置的补货策略：安全库存、补货提前期、包装/订货倍数、首选调出仓库、首选供应商、补货方式（最小-最大/定期/按需）、MIN_MAX 时的最低/最高库存、PERIODIC 时的审视周期 |
| 安全库存计算/越库/月台预约/提前期记录（ErpInvDrp*） | DRP 运算的辅助实体集：安全库存优化计算（SafetyStockCalc）、越库（CrossDock）、月台预约（DockAppointment）、提前期记录（LeadTimeRecord）。沿用 `ErpInvDrp*` 类名/`erp_inv_drp_` 表前缀为历史命名例外（裁决见下文 §`ErpInvDrp*` 实体命名例外登记 F7） |

字段、类型、精度、字典码以 `module-drp/model/app-erp-drp.orm.xml` 为权威源。

## 状态机

DRP 计划头状态：`DRAFT → COMPUTED → APPROVED → EXECUTED`。明细行状态：`SUGGESTED → APPROVED → ORDERED`（或 `CANCELLED`）。详细规则见 [`state-machine.md`](state-machine.md)。

## 命名例外登记（F2e）

> 治理审查 `docs/audits/2026-07-23-0000-architecture-governance-review.md` 闭包前必须项 #9（F2，P2）裁决产物。

drp 域有 3 个 dict 文件物理归属 `module-drp/erp-drp-meta/.../erp-inv/`，dict key 挂在 `erp-inv/` 命名空间下（文件名均带 `drp-` 前缀）：

| dict key | 物理文件 | ORM 定义 | ORM 消费者 |
|----------|----------|----------|-----------|
| `erp-inv/drp-service-level` | `module-drp/erp-drp-meta/.../erp-inv/drp-service-level.dict.yaml` | `module-drp/model/app-erp-drp.orm.xml` | `ErpDrpParameter.serviceLevel` 等 |
| `erp-inv/drp-ss-method` | `module-drp/erp-drp-meta/.../erp-inv/drp-ss-method.dict.yaml` | `module-drp/model/app-erp-drp.orm.xml` | `ErpDrpParameter.ssMethod` 等 |
| `erp-inv/drp-xdock-status` | `module-drp/erp-drp-meta/.../erp-inv/drp-xdock-status.dict.yaml` | `module-drp/model/app-erp-drp.orm.xml` | 越库状态列 |

**裁决：保留在 `erp-inv/` 命名空间（命名例外，方案 b）。** 理由：

1. 3 个 dict 的 ORM 定义（`module-drp/model/app-erp-drp.orm.xml`）+ 物理文件（`module-drp/erp-drp-meta/`）+ ORM 消费者（3 列）**全部归属 module-drp**，仅在 dict key 命名空间上挂 `erp-inv/`（历史遗留）。
2. 迁移到 `erp-drp/` 命名空间需改 ORM `name=` + 列 `ext:dict=`（3 dict 定义 + 3 列），触 ORM `ext:dict` 引用变更保护区域（ask-first），收益为零风险行为变更。
3. 物理归属已正确（文件在 module-drp 内），文件名 `drp-` 前缀已显式标识归属，登记命名例外即可消除歧义。

> 与 F7（闭包项 #11，`ErpInvDrp*` 实体重命名）属不同 finding：本登记仅覆盖 3 个 dict 文件的命名空间归属。实体级命名例外裁决见下节。

## `ErpInvDrp*` 实体命名例外登记（F7）

> 治理审查 `docs/audits/2026-07-23-0000-architecture-governance-review.md` 闭包项 #11（F7，LOW）裁决产物。
> 与上节 dict 命名空间例外（F2e）不同：本节覆盖 **实体类名/表名前缀** 的命名例外登记。

### 裁决：登记命名例外（方案 b，零 ORM 风险）

4 个 drp 实体保留 `ErpInvDrp*` 类名前缀 + `erp_inv_drp_*` 表前缀（命名例外），**不立即重命名**为 `ErpDrp*` / `erp_drp_*`。

**裁决理由**：

1. **物理归属已正确**：4 实体的 ORM 定义（`module-drp/model/app-erp-drp.orm.xml`）、生成产物（`erp-drp-dao/-meta/-service/-web`）、IBiz/BizModel/Processor/test 全部归属 `module-drp`，`className` 均为 `app.erp.drp.dao.entity.*`。仅类名/表名前缀段沿用 `Inv` 历史段。
2. **重命名触及保护区域，风险高于收益**：重命名需改 ORM `*.orm.xml`（`className` + `tableName` + `name=` + 索引/唯一键名）+ 表名迁移 DDL + 全部 BizModel/Processor/IBiz/test/i18n/view/auth/codegen 产物（66 文件命中 `ErpInvDrp`），属保护区域高风险操作。收益仅为审美一致性（`Inv` 段已由 `Drp` 段 + `module-drp` 物理归属显式标识域归属），无行为变更。
3. **#11 checkpoint 允许登记等效收口**：审查 #11 verification checkpoint 明确允许"`grep ErpInvDrp module-drp` 返回 0（重命名）**或**全部登记在 drp owner doc 的命名例外小节（登记豁免）"两个分支。本裁决采登记分支。
4. **dict 级 F2e 先例**：同型命名例外登记先例（F2e，3 个 dict 文件 `erp-inv/drp-*` 命名空间归属）与本裁决范式一致。

**否决"立即重命名"**：重命名移入 Deferred（见下"收敛触发条件"），待 drp 域进行重大 ORM 变更时顺带处理。

### 实体逐项登记

| 类名 | className | 表名 | 所属域 | 消费 dict（命名空间归属） | 豁免理由 | 收敛触发条件 |
|------|-----------|------|--------|--------------------------|---------|-------------|
| `ErpInvDrpSafetyStockCalc` | `app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc` | `erp_inv_drp_safety_stock_calc` | drp（物理 `module-drp`） | `erp-inv/drp-ss-method`、`erp-inv/drp-service-level`（F2e 已登记） | 类名/表名沿用 `Inv` 历史段，物理归属与 `Drp` 段已标识域归属；重命名触及 ORM 保护区域+表名+66 文件生成产物连锁 | drp 域重大 ORM 变更时顺带重命名为 `ErpDrpSafetyStockCalc` |
| `ErpInvDrpCrossDock` | `app.erp.drp.dao.entity.ErpInvDrpCrossDock` | `erp_inv_drp_cross_dock` | drp（物理 `module-drp`） | `erp-inv/drp-xdock-status`（F2e 已登记） | 同上 | 同上（`ErpDrpCrossDock`） |
| `ErpInvDrpDockAppointment` | `app.erp.drp.dao.entity.ErpInvDrpDockAppointment` | `erp_inv_drp_dock_appointment` | drp（物理 `module-drp`） | （自由字符串 status，无 dict 绑定） | 同上 | 同上（`ErpDrpDockAppointment`） |
| `ErpInvDrpLeadTimeRecord` | `app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord` | `erp_inv_drp_lead_time_record` | drp（物理 `module-drp`） | （无 dict 绑定） | 同上 | 同上（`ErpDrpLeadTimeRecord`） |

> **登记覆盖范围**：`grep ErpInvDrp module-drp` 的全部命中（66 文件：ORM 模型 + 生成产物 `_gen/`、`_` 前缀文件 + 手写 BizModel/Processor/IBiz/test + i18n/view/auth/page）均落入上表 4 实体的登记覆盖范围内（满足 #11 checkpoint "或登记"分支）。

## 跨域协作

| 协作场景 | 对端域 | 协作方式 |
|----------|--------|----------|
| 补货行生成调拨单 | inventory/transfer | DRP 补货行（TRANSFER 类型）APPROVED 后生成调拨单 |
| 补货行生成采购单 | purchase | DRP 补货行（PURCHASE 类型）APPROVED 后生成采购订单 |
| 预测消费 | manufacturing/MRP | DRP 采购到货时间作为 MRP 输入；DRP 消费制造域 APPROVED 预测填充 forecastDemand |
| 仓库网络 | master-data（Warehouse） | 仓库网络（分销中心/区域仓/前置仓层级） |

跨域调用走 `I*Biz` 接口，不做 ORM 层跨工程 `refEntityName`。

## 关键业务规则

1. **净需求计算**：`netRequirement = max(0, safetyStock + forecastDemand - currentStock + allocatedQty - onOrderQty)`。结果为 0 或负时表示库存充足，不产生补货建议。
   - **forecastDemand 来源**：消费制造域 `ErpMfgForecast`（头 status=APPROVED）下的 `ErpMfgForecastLine` 行——按 `materialId + 目标 warehouseId + 区间相交` 聚合 `forecastQty`；warehouseId 为 null 的产品级预测不进入 DRP 仓级消费（由 MRP 消费）。config-gated `erp-drp.forecast-consume-enabled`（默认 true；关闭或无匹配预测时 forecastDemand=0）。
2. **补货类型决策**：若 `warehouse.distributionCenterId` 存在（有上级分销中心），优先走仓间调拨（sourceWarehouseId = distributionCenterId）；否则走采购（supplierId = preferredSupplierId）。
3. **补货量调整**：netRequirement 向上取整到 `orderMultiple` 倍数，生成 `suggestedQty`。
4. **与 MRP 的关系**：DRP 运行在 MRP 之前（DRP 补货采购单的到货时间作为 MRP 的可供量输入之一）；或并行运行（DRP 管分销网络，MRP 管制造端）。
5. **与库存移动的关系**：DRP 不直接写库存。APPROVED 的补货行生成 TransferOrder 或 PurchaseOrder，走 inventory/purchase 域的标准流程。

## 业财过账

DRP 本身不产生会计凭证。DRP 触发的调拨单走跨法人调拨过账（内部交易），采购单走采购过账。

## 配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-inv.drp-run-schedule` | — | DRP 定时运行 cron |
| `erp-inv.drp-default-forecast-horizon-days` | 90 | 默认预测展望期 |
| `erp-inv.drp-auto-generate-order` | false | DRP 批准后是否自动生成补货单 |
| `erp-drp.forecast-consume-enabled` | true | 是否消费制造域 APPROVED 预测填充 forecastDemand；false 时 forecastDemand=0 |

## 菜单归属

manufacturing 域「供应链计划」分组：DRP 计划、DRP 明细、仓库补货参数。

## 反模式警示

- ⛔ **DRP 与 MRP 混为一谈**——DRP 是分销网络补货（仓库 → 仓库，或 供应商 → 仓库），MRP 是制造物料需求（BOM 展开）。运算逻辑不同，实体分开。
- ⛔ **DRP 直接写库存或采购单**——DRP 输出是"建议"，经人工审批后才转成 TransferOrder/PurchaseOrder。
- ⛔ **净需求计算忽略在途库存**——`onOrderQty`（已下单未到货）必须参与计算，否则会重复补货。

## DRP 仿真场景对应物

本域含 DRP 多场景仿真引擎（同构 MRP 侧，详见 [`manufacturing/simulation-engine.md`](../manufacturing/simulation-engine.md) §DRP 对应物）：

- **场景-版本模型**：`ErpDrpScenario` 1:N `ErpDrpScenarioVersion` + `ErpDrpScenarioParam`（参数变体覆盖：SAFETY_STOCK/LEAD_TIME/REPLENISHMENT_QTY）
- **E2 fork 范式**：`SimulationDrpEngine` 复用 DrpEngine 算法但替换 ErpDrpParameter 读取为场景覆盖值，**单次 DRP 路径零触及**（既有 50+ drp 测试零回归）
- **结果对比**：2 维 diff（补货量差/安全库存差），返回 DTO 不持久化
- **转正式计划**：`promoteToFormalPlan(versionId)` 生成新 DRAFT `ErpDrpPlan`，原版本 ARCHIVED，转正后走既有单次释放路径
- **config-gated**：`erp-drp.simulation-enabled` 默认 false

DRP 仿真菜单归属于「DRP仿真」分组（`drp-simulation` orderNo=120，含 ErpDrpScenario/Version/Param 3 菜单）。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、DRP 模型、命名例外登记、跨域协作 |
| `state-machine.md` | DRP 计划/明细行状态机 |
| `safety-stock-optimization.md` | 安全库存优化计算 |
| `lead-time-tracking.md` | 补货提前期记录与追踪 |
| `cross-dock.md` | 越库与月台预约 |
| `use-cases.md` | 用例说明 |
| `ui-patterns.md` | 页面与交互模式 |

## 参考

- DRP 设计参考业界跨域供应链计划与多仓库补货实践（源码分析见 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §供应链计划）
- `docs/design/manufacturing/mrp.md`（MRP 边界）
- `docs/design/manufacturing/simulation-engine.md`（MRP/DRP 仿真引擎）
- `docs/design/inventory/README.md`（库存移动/调拨）
- `docs/design/purchase/README.md`（采购订单）
