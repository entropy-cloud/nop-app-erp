# 2026-07-02-0700-1 inventory-trace-chain

> Plan Status: completed
> Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.11（批次追溯链逻辑）；解除计划 0456-1/0456-2 Deferred/Follow-up「批次级退货追溯约束（触发条件：1.11 批次追溯链落地后）」
> Related: `2026-07-02-0456-1-purchase-return-and-refund.md`（PUR_RETURN 移动单未挂 originReturnedMoveId）、`2026-07-02-0456-2-sales-return-and-refund.md`（SAL_RETURN 移动单未挂 originReturnedMoveId）、`2026-07-01-0811-2-inventory-stockmove-bizmodel.md`（generateMove/reverse 契约源头）
> Mission: erp
> Work Item: 库存移动单自追溯链 + 批次/退货追溯查询（1.11）
> Audit: required

## Current Baseline

实时仓库逐行核实的事实：

- **移动单 `ErpInvStockMove`**（`module-inventory/model/app-erp-inventory.orm.xml:112-148`）：列含 code/moveType/orgId/businessDate/sourceWarehouseId/sourceLocationId/destWarehouseId/destLocationId/docStatus/approveStatus/posted/relatedBillType/relatedBillCode + 标准审计。relations 仅 sourceWarehouse/destWarehouse/org/sourceLocation/destLocation/lines(to-many)。**无 `originMoveId`、无 `originReturnedMoveId`**（自关联追溯列不存在）。**无任何指向其它 StockMove 的 relation**。
- **移动单行 `ErpInvStockMoveLine`**(:151-186)：含 batchNo/serialNo（自由文本列，非 FK）。**无 `sourceMoveLineId`**（行级追溯上链不存在）。
- **库存流水 `ErpInvStockLedger`**(:189+)：含 moveId/moveLineId/batchNo/serialNo（不可变流水，按移动单反查）。
- **无批次主数据实体**：全仓 `rg "ErpMdBatch"` 零命中；`batchNo` 为自由文本（master-data 域无 Batch 主表）。故批次追溯 = 按 `batchNo` 字符串跨移动单/行查询，非按批次 FK。
- **跨域库存契约**（0811-2 已落地，逐一核实）：`IErpInvStockMoveBiz.generateMove(StockMoveRequest, IServiceContext)`（幂等键 `(relatedBillType, relatedBillCode)`，DRAFT→DONE 自动推进）、`reverse(Long moveId, IServiceContext)`（DONE 纠错，生成反向冲销移动单）、`findByRelatedBill(relatedBillType, relatedBillCode, ctx)`（只读反查）。
- **`StockMoveRequest`**（`module-inventory/erp-inv-dao/.../biz/StockMoveRequest.java`）：字段 moveType/businessDate/orgId/sourceWarehouseId/sourceLocationId/destWarehouseId/destLocationId/relatedBillType/relatedBillCode/acctSchemaId/currencyId/code/remark + lines(StockMoveLineRequest)。**无 `originMoveId`、无 `originReturnedMoveId`**。`StockMoveLineRequest` 含 materialId/skuId/uoMId/quantity/unitCost/currencyId/batchNo/serialNo（行级无 origin 字段）。
- **退货移动单当前生成路径**（0456-1/0456-2 已落地）：purchase/sales return 审核 → 组装 `StockMoveRequest`(`relatedBillType`=PUR_RETURN/SAL_RETURN) → `generateMove` 生成反向移动单。生成的移动单**未挂 originReturnedMoveId**（StockMoveRequest 无此字段，无法透传源出/入库移动单 id）。退货反查当前仅靠 `relatedBillType/relatedBillCode` 反查退货单，无法跨到「原出/入库移动单」。
- **过账/库存余额**（0811-2）：移动单 DONE → 估值过账 + 流水 + 余额更新。本计划不动过账/余额逻辑。
- **剩余差距**：(1) 移动单无追溯上链列；(2) generateMove 不持久化 origin 链；(3) reverse 不挂 originReturnedMoveId；(4) 无正向/反向/退货递归追溯查询；(5) 无按 batchNo 的批次追溯查询；(6) 退货移动单无法追溯到源出/入库移动单。

## Goals

- `ErpInvStockMove` 新增追溯上链：`originMoveId`（上游移动单，正向链 uplink）、`originReturnedMoveId`（退货移动单指向的原出/入库移动单，退货链 uplink）+ 对应 to-one self relation。下游链（destMoves/returnedMoves）以**反向查询**表达（不存 M2M 中间表）——见 Task Route Decision。
- `StockMoveRequest` 扩展 `originMoveId`/`originReturnedMoveId`（可空）；`generateMove` 持久化上链（建单时回填移动单 `originMoveId`/`originReturnedMoveId`）。
- `reverse(moveId, ctx)` 生成的反向冲销移动单自动挂 `originReturnedMoveId=moveId`（冲销即「退货式」反向链）。
- 追溯查询 BizModel 方法：`forwardTrace(moveId, ctx)`（正向 origin→dest，递归下游，受 `erp-inv.trace-chain-max-depth` 约束）、`backwardTrace(moveId, ctx)`（反向 dest→origin，递归上游）、`returnTrace(moveId, ctx)`（退货链：给定原移动单 → 其所有退货移动单；给定退货移动单 → 其原移动单）、`batchTrace(batchNo, ctx)`（按 batchNo 跨移动单行查全部相关移动单 + 流水）。环检测 + 最大深度兜底。
- **退货移动单挂链**：purchase/sales return 审核触发 `generateMove` 时透传 `originReturnedMoveId`（取退货单的源出/入库移动单 id，经 `findByRelatedBill` 解析），使退货移动单可反向追溯到原出/入库移动单（解除 0456-1/0456-2 Deferred/Follow-up 的「追溯」语义部分）。
- 移动单取消/冲销时清理追溯链一致性（被取消移动单从其上游的下游视图自然消失；双向引用一致性）。
- 行为测试覆盖 ORM 变更回归、generateMove/reverse 挂链、四类追溯查询（含深度/环）、退货透传挂链、批次追溯。

## Non-Goals

- **M2M 自关联中间表**（Odoo `move_orig_ids`/`move_dest_ids` 真正的 M2M）：本计划用「单 uplink 列 + 反向查询」表达，避免 M2M 中间表 + 双向维护复杂度（见 Task Route Decision）。
- **批次主数据实体（`ErpMdBatch`）**：master-data 域无批次主表；本计划批次追溯按 `batchNo` 字符串查询，不引入批次主实体/批次效期/FEFO 分配（属成本核算/库存策略面）。
- **`sourceMoveLineId` 行级追溯**：行级（`ErpInvStockMoveLine`）追溯按需由移动单级追溯 + 行内 materialId/batchNo 关联表达；本计划不加行级上链列（避免行级 M2M）。
- **批次级退货数量约束**（0456-1/0456-2 Deferred 的「约束」部分）：本计划落地**追溯能力**；「退货数量不得超过特定批次可退量」属约束逻辑，依赖批次主数据 + 行级余额，列为 Follow-up（触发条件：批次主数据落地后）。
- **追溯链可视化 UI / 报表**（树形展示、物料/批次/订单追溯报表）：属 web/report 面；本计划只提供后端查询方法 + `@BizQuery` 暴露。
- **跨法人调拨内部交易凭证 / 在途库所有权**：属调拨业务面（非追溯链本身）。
- **成本核算（移动加权/FIFO）**：追溯链只读 `ErpInvStockLedger`/`ErpFinVoucherBillR`，不重算成本。

## Task Route

- Type: `implementation-only change`（业务逻辑 + ORM 模型增量）。**注**：本计划触及 ask-first 保护区域 `module-inventory/model/app-erp-inventory.orm.xml`（给 `ErpInvStockMove` 加列 + relation），以及跨域 `StockMoveRequest`（dao 层 DTO）与 purchase/sales return 服务的透传。ORM 变更为**加性**（新增可空列 + self relation，不改/不删既有列），需重新 codegen。此为本业务逻辑阶段首次触及 ORM 源模型——按规则 1 从实时基线起，Phase 1 含 codegen 回归。
- Owner Docs: `docs/design/inventory/trace-chain.md`（追溯链模型/查询/场景）、`docs/design/inventory/cross-domain.md`（generateMove/reverse 契约）、`docs/design/inventory/README.md`、`docs/design/flow-overview.md §2.4`（库存管理流程-批次/序列号校验确保追溯）。
- Skill Selection Basis: 全为 ORM 模型增量 + BizModel 查询方法 + 跨实体/跨域调用 → 加载 `nop-backend-dev`（覆盖 IBiz 方法、跨实体访问、CodeGen 增量回归自检）。
- **Decision（追溯链存储模型）**：**选择**「单 uplink 列（`originMoveId` + `originReturnedMoveId`）+ 下游反向查询」，不建 M2M 中间表。**替代**：① Odoo 式 M2M `move_orig_ids`/`move_dest_ids`（双向维护复杂、需中间表 + 删除时双向清理，rejected）；② 行级 `sourceMoveLineId`（行级 M2M，过度复杂，rejected）。**残留风险**：一个移动单理论可有多个上游（多源合并移动），单 uplink 只记主上游；本仓当前所有联动（采购入库→出库、调拨出→入、退货→原单）均为单上游，多源合并非现网形态，列为 Follow-up。

## Infrastructure And Config Prereqs

- 追溯链配置项（`trace-chain.md §配置项`）：`erp-inv.trace-chain-enabled`（默认 true，关闭时查询方法返回空/单节点）、`erp-inv.trace-chain-max-depth`（默认 10，递归深度兜底）。经 `AppConfig.var(..., defaultValue)` 读取，缺失走默认，无 .env/外部服务。
- 模块依赖：`erp-inv-service` 已 compile 依赖 master-data-dao；purchase/sales service 已 compile 依赖 `app-erp-inventory-service`（0811-2 接线）。无新增端口/密钥/数据迁移（新增列为可空，存量行 NULL）。

## Execution Plan

### Phase 1 — ORM 模型增量（追溯上链列 + relation）+ 重新 codegen + 回归

Status: completed
Targets: `module-inventory/model/app-erp-inventory.orm.xml`（ErpInvStockMove 加列 + relation）、经 `nop-cli`/codegen 重新生成 `_app.orm.xml` 与 dao entity、`StockMoveRequest.java`（dao，扩字段）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无（本批次首计划）。

- [x] `Add`：`ErpInvStockMove` 加可空列 `originMoveId`(BIGINT, code=ORIGIN_MOVE_ID)、`originReturnedMoveId`(BIGINT, code=ORIGIN_RETURNED_MOVE_ID)，续 propId 序列；加 to-one self relation `originMove`(refEntity=self, on originMoveId=id, tagSet=pub)、`originReturnedMove`(refEntity=self, on originReturnedMoveId=id, tagSet=pub)。
  - Skill: `nop-backend-dev`
- [x] `Decision`：下游链存储模型——**选择**「单 uplink 列 + 反向查询」而非 M2M（理由见 Task Route Decision）；在计划内记录替代方案与残留风险（多上游合并非现网形态）。
  - Skill: none
- [x] `Add`：`StockMoveRequest`（头级 dao DTO）扩可空字段 `originMoveId`/`originReturnedMoveId`（getter/setter）；行级 `StockMoveLineRequest` 不动（行级追溯属 Non-Goal）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：重新 codegen 后，inventory 既有 CRUD 冒烟测试（Milestone 4 ErpInvStockMove/ErpInvStockMoveLine 抽样）+ 0811-2 StockMove 既有套件全绿（验证加性列未破坏既有行为）；本地化 `mvn test -pl module-inventory/erp-inv-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付 ORM 加性增量 + codegen 无回归（既有库存套件绿）。解除 Phase 2 对持久化上链的阻塞。

- [x] `ErpInvStockMove` 含 originMoveId/originReturnedMoveId 列 + relation；codegen 产物更新；既有 inventory 测试无回归

### Phase 2 — generateMove/reverse 挂链 + 追溯查询 BizModel 方法 + 取消清理

Status: completed
Targets: `module-inventory/erp-inv-service/.../entity/ErpInvStockMoveBizModel.java`（扩）、`.../biz/IErpInvStockMoveBiz.java`（扩查询契约）、`.../biz/TraceChainQuery.java`(新增查询助手)、`.../ErpInvConstants.java`(扩深度配置键)、`app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（列已存在）。

- [x] `Add`：`generateMove` 持久化上链——建单时若 `StockMoveRequest.originMoveId`/`originReturnedMoveId` 非空则回填移动单对应字段；保持既有幂等键 `(relatedBillType, relatedBillCode)` 不变。
  - Skill: `nop-backend-dev`
- [x] `Add`：`reverse(moveId, ctx)` 生成的反向冲销移动单自动置 `originReturnedMoveId=moveId`（冲销 = 反向退货式链），无需调用方传参。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpInvStockMoveBiz` 声明 `@BizQuery` 追溯契约 `forwardTrace(moveId, ctx)`/`backwardTrace(moveId, ctx)`/`returnTrace(moveId, ctx)`/`batchTrace(batchNo, ctx)`（只读，返回移动单节点列表 + 链路结构）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`TraceChainQuery` 实现——正向按 `originMoveId=?` 反查下游（destMoves）、反向按 `originMoveId` 上溯（originMove）、退货链按 `originReturnedMoveId` 双向；递归带 `max-depth`（`erp-inv.trace-chain-max-depth`，默认 10）+ 已访问集合**环检测**（重复节点截断并标记），超深截断。`batchTrace` 按 `ErpInvStockMoveLine.batchNo=?`（含流水 ErpInvStockLedger.batchNo）聚合相关移动单。`trace-chain-enabled=false` 时返回单节点/空。
  - Skill: `nop-backend-dev`
- [x] `Decision`：追溯查询的环检测策略——**选择**「访问集合 + 深度上限」双兜底（防数据异常成环 + 防深度爆炸）；记录残留风险（成环数据本身是脏数据，查询截断而非报错）。
  - Skill: none
- [x] `Add`：移动单 cancel/reverse 路径一致性——被取消/冲销移动单逻辑删除（delVersion 自增），追溯查询过滤 `delVersion != 0`（ORM 逻辑删除过滤，已删节点不出现在链中）；上游 to-one 引用保持（审计可追溯被取消的单），下游反查自然排除已删。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpInvTraceChain`——generateMove 带 origin 挂链、reverse 自动挂 originReturnedMoveId、forward/backward/return 递归正确（采购入库→出库→完工多级链）、环检测（人造环截断）、max-depth 截断、batchTrace 按 batchNo 命中、delVersion 过滤、enabled=false 降级。`mvn test -pl module-inventory/erp-inv-service -am -Dtest=TestErpInvTraceChain`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付挂链 + 四类追溯查询（含环/深度兜底）。解除 Phase 3 退货透传对查询能力的阻塞。

- [x] generateMove/reverse 正确回填 origin 链；四类追溯查询返回正确链路，环/深度兜底生效；delVersion 过滤正确

### Phase 3 — 退货移动单透传挂链（purchase/sales return 接入）+ 解除 Deferred

Status: completed
Targets: `module-purchase/erp-pur-service/.../entity/ErpPurReturnBizModel.java`（退货触发段）、`module-sales/erp-sal-service/.../entity/ErpSalReturnBizModel.java`（退货触发段）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2（查询能力 + StockMoveRequest 字段已就绪）。

- [x] `Add`：purchase return 审核触发 `generateMove` 时，解析源入库移动单 id（经 `findByRelatedBill(PUR_RECEIVE 等入库 relatedBillType, 源入库单 code)` 或退货单回链的 receiveLine→move 反查），透传 `StockMoveRequest.originReturnedMoveId`，使 PUR_RETURN 移动单挂退货链上链。
  - Skill: `nop-backend-dev`
- [x] `Add`：sales return 审核触发 `generateMove` 时，解析源出库移动单 id（经 `findByRelatedBill(SAL_DELIVERY, 源出库单 code)`），透传 `StockMoveRequest.originReturnedMoveId`，使 SAL_RETURN 移动单挂退货链上链。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpPurReturnTrace`/`TestErpSalReturnTrace`——退货审核后退货移动单 `originReturnedMoveId` 指向原入库/出库移动单；`returnTrace(原移动单)` 返回该退货移动单；端到端退货→追溯可贯通。`mvn test -pl module-purchase/erp-pur-service -am -Dtest=TestErpPurReturnTrace` 与 `module-sales/erp-sal-service` 对称。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 3 交付退货移动单追溯挂链（解除 0456-1/0456-2 的「追溯」语义 Deferred）。完整仓库验证属 Closure Gates。

- [x] 退货移动单 originReturnedMoveId 正确指向源出/入库移动单；returnTrace 双向贯通

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0e01a33cdffeSQtt64e0qr85p0`，独立 general 子代理，新会话）— 全部 Current Baseline 主张经实时仓库逐行核实属实（ErpInvStockMove 无 origin 列/self-relation、ErpInvStockMoveLine 有 batchNo/serialNo 无 sourceMoveLineId、无 ErpMdBatch、generateMove/reverse/findByRelatedBill 契约、StockMoveRequest 无 origin 字段、0456-1/0456-2 Deferred 1.11 触发条件均在）。1 BLOCKER：Phase 1 item 3 含禁用 hedge「如需」+ 与行级 Non-Goal 矛盾（StockMoveLineRequest 不应获 origin 字段）。S 级 nit：基线字段枚举漏 moveType/businessDate、delVersion 过滤应写 `!=0`。**已修订**：item 3 收窄为 StockMoveRequest 头级 DTO（行级不动，去除「如需」）；基线补 moveType/businessDate；delVersion 过滤改 `!=0`。非阻塞 nit 已吸收。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：追溯上链列 + generateMove/reverse 挂链 + 四类追溯查询 + 退货透传挂链，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 1.11 标注进展；当日日志已记；`trace-chain.md` 存储模型偏离（单 uplink + 反向查询 vs M2M）补注
- [x] 已运行验证：`mvn test -pl module-inventory/erp-inv-service -am`、`module-purchase/erp-pur-service`、`module-sales/erp-sal-service` 全绿；根 `mvn test -fae` 无回归
- [x] 无范围内项目降级为 deferred/follow-up（M2M 中间表/批次主数据/行级追溯/批次级退货约束/可视化报表/成本核算均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 批次级退货数量约束（按批次限制退货可退量）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划落地追溯能力；按批次的可退量约束依赖批次主数据 + 行级批次余额，退货当前按出/入库行聚合（0456-1/0456-2 已实现）。
- Successor Required: yes（触发条件：批次主数据 `ErpMdBatch` 落地后，评估行级批次余额约束）

### 多上游合并移动（一个移动单多个 origin）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 单 uplink 模型只记主上游；现网所有联动均为单上游，多源合并非当前形态。
- Successor Required: yes（触发条件：出现多源合并移动单时，改 M2M 或多 uplink 列）

### 追溯链可视化 UI / 报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 web/report 面；本计划只提供后端 `@BizQuery` 查询方法。
- Successor Required: yes（触发条件：实施库存追溯 web 页面/`nop-report` 报表时）

## Closure

Status Note: 计划关闭条件全部满足——ErpInvStockMove 自追溯上链（originMoveId/originReturnedMoveId）+ generateMove/reverse 挂链 + 四类追溯查询（forward/backward/return/batch，含环/深度兜底、delVersion 过滤、enabled 降级）+ 退货移动单透传挂链（purchase/sales）全部落地，行为测试全绿，根 `mvn test -fae` BUILD SUCCESS 无回归，当日日志已记。独立结束审计通过（独立子代理新会话，0 BLOCKER/MAJOR/MINOR）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0dff965c1ffe2ptj8j5A6WWh9e`（新会话，无执行者上下文），对实时仓库逐项核实，verdict `passes closure audit`。
- 独立审计核实结论（live repo file:line 核对，非采信自报）：
  - **Phase 1（ORM 加性 + codegen）**：`app-erp-inventory.orm.xml:139-140` 加 `originMoveId`(propId 24)/`originReturnedMoveId`(propId 25)；`:148-149` 加 self relation `originMove`/`originReturnedMove`；`_ErpInvStockMove.java:1351-1383` 4 个访问器；`git diff` 证实纯加性（+4 行无删改）。
  - **DTO**：`StockMoveRequest.java:33-34,142-154` 两字段；`StockMoveLineRequest` 无 origin 字段（Non-Goal 遵守）。
  - **Phase 2（挂链）**：`ErpInvStockMoveBizModel.newMove:309-310` 透传上链；`reverse:157` 置 `originReturnedMoveId=原单 id`；幂等键 `(relatedBillType,relatedBillCode)` 不变（`findExisting:355-357`）。
  - **追溯查询**：`IErpInvStockMoveBiz.java:71-91` 4 个 `@BizQuery`；`TraceChainQuery` 正向 BFS（`findActiveMovesByOrigin`）、反向上溯、退货双向、批次（line+ledger batchNo）；环检测 + max-depth + delVersion=0 过滤（`:224`）+ enabled=false 降级均落地；`app-service.beans.xml:25-26` 注册。
  - **Phase 3（退货透传）**：`ErpPurReturnBizModel.triggerOutgoingMove:212`→`resolveSourceReceiveMoveId`(`findByRelatedBill(PUR_RECEIVE)`,`:226-227`)；`ErpSalReturnBizModel.triggerIncomingMove:215`→`findByRelatedBill(SAL_DELIVERY)`,`:229-230`。
  - **测试**：`TestErpInvTraceChain`（9 tests/31 asserts，覆盖 9 项）；`TestErpPurReturnTrace`（2/12）、`TestErpSalReturnTrace`（2/13）；环/max-depth 测试含真实人造数据 + 实质断言。
  - **反空心**：`TraceChainQuery` 为真实递归实现，`return result` 为 null-root/降级早返非占位。
  - **范围保护**：`git diff` 仅 `app-erp-inventory.orm.xml` ORM（+4 加性）；无 `.api.xml` 改；`StockMoveBookkeeper`/`InvPostingDispatcher` 未改。
  - **文档对齐**：`core-business-roadmap.md:24,47` 1.11 `done`；`07-02.md:3-22` 日志；`trace-chain.md:42` 存储模型偏离注。
  - **一致性**：Plan Status `completed`；3 Phase 全 `completed` 且全 `[x]`；唯一 `[ ]` 为独立审计门控（已由本审计闭合）。
  - **反模式自检**：4 个 trace `@BizQuery` 注解齐；`TraceChainQuery` `@Inject` 非 private；plain bean 直用 IDaoProvider（非 BizModel 正确范式）；无 `System.currentTimeMillis`/第三方 JSON。
- 执行者交付证据（执行者自报，经独立审计核实）：
  - Phase 1-3 全部 `[x]` 且各 `Status: completed`（见各 Phase 节）。
  - 范围内行为落地、测试 13 个新行为测试、文档对齐、验证全绿（inventory 30 / purchase 70 / sales 65 / 根 `mvn test -fae` BUILD SUCCESS 0 failures），范围保护（ORM 加性），详见上方独立审计核实。

Follow-up:

- 批次级退货数量约束（见上方 Deferred，批次主数据落地后）
- 多上游合并移动 uplink（见上方 Deferred，多源合并场景出现时）
- 追溯链可视化 UI/报表（见上方 Deferred，web/report 面时）
- 行级 `sourceMoveLineId` 追溯（触发条件：行级追溯需求出现时）
