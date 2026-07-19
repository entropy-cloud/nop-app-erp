# 2026-07-19-0849-1-b2b-asn-line-level-receive-fill b2b ASN→采购入库行级回填后端扩展 + 浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-19
> Source: `docs/plans/2026-07-17-1005-1-b2b-aps-inbound-scheduling-orchestration-e2e.md` Deferred But Adjudicated「createReceiveFromAsn 跨域建单深度（行级回填）」(l.171-175，1005-1 cancelled 后该项仍开放) + `docs/plans/2026-07-14-0941-2-b2b-logistics-aps-orchestration-e2e.md` Phase 1 间接覆盖但未深入（0941-2 仅断言 receive 头存在未断言 lines）。
> Related: `2026-07-14-0508-1`（b2b/aps/logistics DIRECT 状态机 E2E，已 completed）、`2026-07-14-0941-2`（b2b/aps/logistics 跨域编排 E2E，已 completed；本计划承接其 ASN→入库深度 successor）、`docs/design/b2b/edi-formats.md`（ASN 业务语义）、`docs/testing/e2e-runbook.md`（业务动作表）
> Audit: required

## Current Baseline

### 已落地（不动）

- **`ErpB2bAsnBizModel.createReceiveFromAsn(@Name("asnId") Long)`**（`module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/entity/ErpB2bAsnBizModel.java:188`）为 DIRECT `@BizMutation`，config-gated `erp-b2b.asn-auto-create-receive` 默认 false。当前实现仅建采购入库**头**：经 `daoProvider().daoFor(ErpPurReceive.class).newEntity()` 创建 `ErpPurReceive`（`:212-223`），设置 `code="RCV-FROM-ASN-"+asn.code` + orderId/supplierId/warehouseId/currencyId/businessDate/docStatus=UNSUBMITTED/approveStatus=UNSUBMITTED/receiveStatus=NOT_RECEIVED/remark。**不创建任何 `ErpPurReceiveLine` 行**。
- ASN 状态机：MATCHED →（createReceiveFromAsn）→ RECEIVED_TO_STOCK（`asn.setStatus(...)` `:226`）。
- **`ErpB2bAsnLine` 实体存在**（`module-b2b/model/app-erp-b2b.orm.xml:265-285`）：含 `asnId`（to-one asn）+ lineNo/materialId/supplierPartNo/quantity/shippedQty/remark + 审计字段。**注意：AsnLine 无 `uoMId` 字段**（待 Phase 1 Decision (f) 裁决 ReceiveLine.mandatory uoMId 来源）。ASN↔AsnLine cascade-delete to-many（`:241`）。
- **`ErpPurReceiveLine` 实体存在**（`module-purchase/model/app-erp-purchase.orm.xml:759`，`receiveId` to-many `:729`）：含 materialId/**uoMId（mandatory=true）**/quantity/unitPrice/amount/warehouseId/lineNo 等字段（具体字段集待 Phase 1 Explore 核实）。
- **既有 spec**：`tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts`（0941-2 落地）已覆盖 matchPurchaseOrder + retryMatch + createReceiveFromAsn 头创建 + 状态翻转。spec 注释明示「仅头创建不断言 lines」（l.18 + l.132）。
- **既有 JUnit 覆盖**：`module-b2b/erp-b2b-service/src/test/java/app/erp/b2b/service/TestErpB2bAsnInventoryIntegration.java`（`testCreateReceiveFromMatchedAsn` l.82 + `testCreateReceiveFromUnmatchedAsnFails` l.111）已覆盖 createReceiveFromAsn 头创建路径。本计划扩展该类新增行级回填用例。
- **既有 cleanup 范式**：0941-2 spec cleanup 删 ErpPurReceive + AsnLine + ASN + ErpPurOrderLine + ErpPurOrder（b2b 跨域建单反向清理）。

### 缺失（本计划对象）

1. **后端 createReceiveFromAsn 不回填行级**——ASN AsnLine 已含 materialId/quantity 等映射所需字段，但 `createReceiveFromAsn` 不读取 AsnLine、不创建 ErpPurReceiveLine。下游 Receive 仅头无行 → 采购入库链断裂（无 materialId 行无法 approve/入库触发）。
2. **行级回填浏览器层零覆盖**——0941-2 仅断言 receive 头存在性；行级回填产物（ErpPurReceiveLine 多行 + materialId/quantity 字段翻转）无独立断言。

### 既有验证范式（本计划复用）

- `tests/e2e/business-actions/_helper.ts`：`createViaSave` / `callMutation` / `verifyState`。
- `tests/e2e/business-actions/b2b-asn-match-receive.action.spec.ts`：ASN/PO/AsnLine setup + cleanup 范式（本计划 spec 在此基础上扩展断言）。
- `findFirst` GraphQL 反查原语（0100-1 范式）。

### 剩余差距

- `ErpB2bAsnBizModel.createReceiveFromAsn` 须扩展：iterate `asn.lines` → 为每条 AsnLine 创建对应 ErpPurReceiveLine（materialId/uoMId/quantity/unitPrice/warehouseId 等字段映射）→ saveEntity 批量持久化。
- 行级字段映射规则须 Phase 1 Decision（unitPrice 来源：PO line unitPrice vs AsnLine 携带 vs null；amount 派生 vs null；lineNo 透传 vs 重生成）。
- 浏览器层 E2E 须断言：createReceiveFromAsn 后 ErpPurReceiveLine 多行存在 + 每行字段（materialId/quantity/warehouseId）映射正确 + 行数 == AsnLine 行数。

## Goals

- 后端扩展 `ErpB2bAsnBizModel.createReceiveFromAsn` 行级回填：ASN AsnLine → ErpPurReceiveLine 字段映射 + 批量持久化。
- 浏览器层 E2E 1 spec（≥2 用例）：覆盖行级回填正路径（多行映射 + 字段精确数值断言）+ 空白 AsnLine 边界（0 行 → Receive 头仍创建 + 0 行 ReceiveLine）+ config-gate 关闭路径（默认 false → 跳过 + null 返回守卫）。
- 行级字段映射规则经 Phase 1 Decision 裁决并写入 plan + owner doc 实现注记。

## Non-Goals

- **不修改 ORM/契约/字典/种子**：ErpB2bAsnLine + ErpPurReceiveLine 字段集已齐备（待 Phase 1 Explore 逐项核实），不动 ORM。如 Explore 发现字段缺失须 ORM 加列，按 R4/R14 拆分为独立 ask-first successor（不入本计划）。
- **不做 ASN 与 PO line 二次匹配**：matchPurchaseOrder 阶段已建立 ASN↔PO 关联（0941-2 已覆盖），createReceiveFromAsn 直接消费 AsnLine 字段，不重复 PO line 匹配逻辑。
- **不做 Receive 后续 approve/入库触发链 E2E**：本计划仅覆盖 createReceiveFromAsn 单动作的行级回填；Receive approve + 入库触发经 1132-1 已覆盖（purchase 域独立 E2E），不并入。
- **不做 Receive 价格继承/差异处理**：unitPrice 来源经 Phase 1 Decision 裁决单一规则；PO vs ASN 价格差异处理属采购价格管理 successor。
- **不做多仓库/多币种 Receive 拆分**：单 ASN → 单 Receive 多行映射；多仓库拆分 Receive 属不同结果面 successor。

## Task Route

- Type: `implementation-only change`（后端方法扩展 + 浏览器层 E2E；消费侧 spec 不动契约）
- Owner Docs:
  - `docs/design/b2b/edi-formats.md`（ASN 业务语义，本计划补 createReceiveFromAsn 行级实现注记）
  - `docs/testing/e2e-runbook.md`（业务动作表 + 套件计数）
- Skill Selection Basis: 后端 `@BizMutation` 方法扩展 + 浏览器层 E2E——加载 `nop-backend-dev`（BizModel 决策门 + 跨实体访问 + protected step）+ `nop-testing`（spec 范式 + JUnit 回归）。

## Infrastructure And Config Prereqs

- 无新基础设施依赖。复用 0941-2 既有 webServer JVM arg `-Derp-b2b.asn-auto-create-receive=true`（已在 `playwright.config.ts` webServer args 中）。
- 独立子代理审计会话用于草案审查 + 结束审计。

## Execution Plan

### Phase 1 - Explore + Decision：行级字段映射规则

Status: completed
Targets: 探索笔记（不落仓库）+ plan Decision 落地
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] `Proof`：核实 `ErpB2bAsnLine`（`module-b2b/model/app-erp-b2b.orm.xml:265-285`）字段集 + `ErpPurReceiveLine`（`module-purchase/model/app-erp-purchase.orm.xml:759+`）字段集，列出映射表（AsnLine.field → ReceiveLine.field）+ 标识字段缺失项（若有）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：核实 `ErpB2bAsnBizModel.createReceiveFromAsn` 当前调用栈（`:188-231`）+ `daoProvider().daoFor(ErpPurReceive.class)` 范式可复用性 + ErpPurReceiveLine DAO 同模块跨域访问路径（b2b-service 模块对 module-purchase 的依赖方向，对齐 0941-2 IErpPurReceiveBiz 跨域调用范式）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：行级字段映射规则（须裁决项）：
  - **(a) unitPrice 来源**：① PO line unitPrice 透传（须经 materialId 反查 PO line，因 AsnLine 无 `poLineId`/`orderLineId` 字段持久化——`matchPurchaseOrder` 在内存中按 materialId 匹配但不持久化）；② AsnLine 自带 unitPrice（待 Explore 核实字段是否存在——预核实：不存在）；③ null 留空（后续 Receive approve 时由 PO 同步覆盖）。**裁决依据**：AsnLine 字段集 + 0941-2 既有断言范式 + 复杂度（materialId 反查 PO line 复杂度 vs null 兜底）。
  - **(b) amount 派生**：① 若 unitPrice 非 null → `amount = unitPrice × quantity`（HALF_UP scale=4 对齐 amount domain）；② unitPrice null → amount null。
  - **(c) lineNo 透传**：① 透传 AsnLine.lineNo；② 重新按数组下标生成（1-based）。**裁决依据**：避免 lineNo 冲突（Receive 已有行时）。
  - **(d) warehouseId 来源**：① 透传 AsnLine.warehouseId（若存在——待 Explore 核实）；② 复用 Receive 头 warehouseId（asn 已映射自 PO）。**裁决依据**：AsnLine 字段集核实。
  - **(e) 空白 AsnLine 边界**：0 行 AsnLine → Receive 头创建 + 0 行 ReceiveLine（不抛错）；或抛守卫拒绝。**裁决依据**：业务语义（无行 ASN 是否合法）+ 0941-2 既有测试预期。
  - **(f) uoMId 来源（关键 Decision，承接 B1/B2）**：AsnLine 无 uoMId 字段 + ReceiveLine.uoMId mandatory=true。候选：① materialId → `ErpMdMaterial.defaultUoMId` 反查（首选，物料主数据应有默认单位）；② PO line uoMId 经 materialId 反查 PO line；③ config-gated 默认 uoMId（兜底）；④ 任一缺失 → 抛 `NopException` 守卫拒创建 ReceiveLine。**裁决依据**：materialId 反查 defaultUoMId 的可行性 + 既有 InventoryMove 等场景 uoMId 解析范式（待 Explore 核实 `ErpMdMaterial` 是否有 defaultUoMId 字段 + 既有反查 helper）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：实现策略（须裁决项）：
  - **(a) 迭代持久化**：① `for (line : asnLines) { dao.newEntity(); setFields; dao.saveEntity(line); }`（简单 + N 次 INSERT）；② `dao.saveDirectly(linesList, true)`（批量）。**裁决依据**：Nop DAO 范式 + 既有 createReceiveFromAsn 单 saveEntity 模式。
  - **(b) 失败回滚**：任一行持久化失败 → 抛 NopException 触发 @BizMutation 事务回滚（强一致，对齐 0949-1 mnt-labor postLabor 范式）；或 best-effort 部分写入（不推荐）。
  - Skill: `nop-backend-dev`

#### Phase 1 Explore 结果（逐行核实，实时仓库）

**Proof (a) — AsnLine 字段集（`module-b2b/model/app-erp-b2b.orm.xml:265-296`）**：

| 字段 | 类型 | mandatory | 备注 |
| --- | --- | --- | --- |
| id | long | Y | 主键 seq-default |
| asnId | long | Y | to-one asn |
| lineNo | int | Y | ASN 内行号 |
| materialId | long | **N** | AsnLine 物料 ID 可空（webhook 解析时未必能映射） |
| supplierPartNo | string(100) | N | 供应商料号 |
| quantity | decimal | N | 订单数量（domain=quantity） |
| shippedQty | decimal | N | 发货数量（domain=quantity） |
| remark | string | N | code-mapping 结果占位（parseToAsn l.320） |
| 审计字段 | - | - | delVersion/version/createdBy/createTime/updatedBy/updateTime |

**关键缺失**：AsnLine 无 `uoMId` / `unitPrice` / `warehouseId` / `orderLineId` / `poLineId` 字段。

**Proof (a) — ReceiveLine 字段集（`module-purchase/model/app-erp-purchase.orm.xml:759-814`）**：

| 字段 | 类型 | mandatory | 来源（按本 Phase Decision） |
| --- | --- | --- | --- |
| id | long | Y | seq-default |
| receiveId | long | Y | receive.getId()（新建 Receive） |
| orderLineId | long | N | **PO line 反查（按 materialId 匹配）→ orderLineId** |
| lineNo | int | Y | **透传 AsnLine.lineNo**（Decision (c)①） |
| materialId | long | **Y** | 透传 AsnLine.materialId（若 null 抛守卫） |
| skuId | long | N | null（ASN 阶段无 SKU 信息） |
| uoMId | long | **Y** | **materialId → ErpMdMaterial.uoMId 反查**（Decision (f)①） |
| quantity | decimal | **Y** | AsnLine.shippedQty（fallback quantity）— **shippedQty 优先（实际发货数量），fallback quantity** |
| rejectedQuantity | decimal | N (default 0) | default 0（不入库阶段不校验拒收） |
| unitPrice | decimal | N | **PO line.unitPrice 反查**（Decision (a)①） |
| taxRate | decimal | N | **PO line.taxRate 反查**（同 unitPrice 路径，附带税率对齐） |
| taxAmount | decimal | N | null（amount 派生不含税；taxAmount 后续 Receive approve 时由 tax engine 计算） |
| amount | decimal | N | **派生：unitPrice != null → unitPrice × quantity（HALF_UP scale=4）；否则 null**（Decision (b)①） |
| warehouseId | long | N | **复用 receive.warehouseId（即 po.warehouseId）**（Decision (d)②） |
| batchNo | string | N | null（ASN 不携带批号） |
| remark | string | N | "由 ASN AsnLine #{lineNo} 自动回填" |

**字段缺失项**：无（AsnLine + ReceiveLine 字段集已覆盖映射所需，无需 ORM 加列）。

**Proof (b) — ErpMdMaterial.uoMId（`module-master-data/model/app-erp-master-data.orm.xml:183`）**：

```
<column name="uoMId" code="UOM_ID" displayName="主计量单位" propId="6"
        stdSqlType="BIGINT" stdDataType="long" mandatory="true" />
```

**结论**：`ErpMdMaterial.uoMId` mandatory=true，反查路径 `materialId → ErpMdMaterial → uoMId` 必然有值。Decision (f) 候选 ① 可行。

**Proof (c) — `createReceiveFromAsn` 当前调用栈（`ErpB2bAsnBizModel.java:188-231`）**：

- `AppConfig.var(...)` config-gate（默认 false）
- `requireAsn(asnId)` → 状态守卫 MATCHED
- `findPurchaseOrder(asn.getRelatedBillCode())` → 已持有 po
- `daoProvider().daoFor(ErpPurReceive.class).newEntity()` + 设置头字段 + `saveEntity(receive)`（l.212-223）
- `asn.setStatus(RECEIVED_TO_STOCK)` + `saveOrUpdateEntity(asn)`（l.226-227）

**已存在 helper（可直接复用，无需新增）**：

- `findPoLines(Long orderId)` → `List<ErpPurOrderLine>`（l.379-383）— 反查 unitPrice/taxRate/orderLineId 来源
- `findMatchingPoLine(List<ErpPurOrderLine>, Long materialId)` → `ErpPurOrderLine`（l.385-395）— 已封装按 materialId 反查 PO line
- `findAsnLines(Long asnId)` → `List<ErpB2bAsnLine>`（l.361-365）— AsnLine 列表读取

**模块依赖方向**：

- `app-erp-b2b-service` pom.xml 显式依赖 `app-erp-purchase-dao`（l.22-27，已注释说明「ASN→采购入库 config-gated 集成」）
- `app-erp-b2b-dao` → `app-erp-master-data-dao`（pom.xml:17-20）→ ErpMdMaterial 类编译期可见
- **结论**：`ErpPurReceiveLine` + `ErpMdMaterial` 类对 b2b-service 编译期可达，`daoProvider().daoFor(ErpPurReceiveLine.class)` + `daoProvider().daoFor(ErpMdMaterial.class)` 范式可复用（无需新加 IBiz 接口）。

#### Phase 1 Decision 落地（逐项裁决）

- **Decision (a) unitPrice 来源** → **裁决 ① PO line.unitPrice 透传**：经 `findMatchingPoLine(poLines, asnLine.materialId)` 反查 PO line → 取 `unitPrice`（+ `taxRate` + `orderLineId` 顺便透传加强关联）。理由：AsnLine 无 unitPrice 字段；PO line 在 matchPurchaseOrder 阶段已建立按 materialId 的内存匹配；createReceiveFromAsn 持有 po 后 findPoLines 可一次性拉取 PO 行列表，O(N) 反查复杂度可接受；价格继承自 PO 符合采购语义（Receive 草稿不重新定价）。
- **Decision (b) amount 派生** → **裁决 ①**：`unitPrice != null && quantity != null → amount = unitPrice.multiply(quantity).setScale(4, RoundingMode.HALF_UP)`；否则 null。
- **Decision (c) lineNo** → **裁决 ① 透传 AsnLine.lineNo**：新 Receive 草稿首次创建无既有行，透传无冲突；保留 ASN 与 Receive 行号映射可追溯。
- **Decision (d) warehouseId** → **裁决 ② 复用 receive.warehouseId**：AsnLine 无 warehouseId 字段；Receive 头 warehouseId 已自 po.warehouseId 映射，行级统一入库到 Receive 头仓库（多仓拆分属 Non-Goal）。
- **Decision (e) 空白 AsnLine 边界** → **裁决 ① 0 行合法**：Receive 头仍创建 + 0 行 ReceiveLine，LOG.warn 提示「AsnLine 为空」。理由：matchPurchaseOrder 已容许部分行未匹配；createReceiveFromAsn 保留头创建能力，行级空可后续手动补录；0941-2 既有 `testCreateReceiveFromMatchedAsn` 覆盖单行正路径，本计划扩展 0 行边界用例。
- **Decision (f) uoMId 来源** → **裁决 ① materialId → ErpMdMaterial.uoMId 反查**（首选，Proof (b) 已验证 mandatory=true 必然有值）；**materialId 为 null 或 ErpMdMaterial 不存在 → 抛 NopException 守卫**（新增 `ERR_B2B_ASN_LINE_MATERIAL_REQUIRED` ErrorCode）。
- **Decision 实现策略 (a) 迭代持久化** → **裁决 ① 简单迭代 saveEntity**：行数典型 ≤30（ext:estRows="30"），N 次 INSERT 性能可接受；对齐既有 createReceiveFromAsn 单 saveEntity 模式 + parseToAsn 中 AsnLine 迭代 saveEntity 模式（l.309-325）；不引入 `saveDirectly` 批量语义偏离。
- **Decision 实现策略 (b) 失败回滚** → **裁决 强一致**：任一行持久化失败或守卫触发 → 抛 NopException，@BizMutation 自动事务回滚，已写入的 Receive 头 + ReceiveLine 全部回滚（强一致，对齐 0949-1 mnt-labor postLabor 范式）。LOG.warn 上下文（asn.code / lineNo / 错误消息）。

Exit Criteria:

- [x] 探索笔记 + 5 字段映射 Decision + 2 实现策略 Decision 在 plan 内记录（Phase 1 章节内嵌或独立 Decision 段），可指导 Phase 2 编码。
- [x] 若 Explore 发现 AsnLine/ReceiveLine 字段缺失须 ORM 加列，开独立 successor 计划（不入本计划范围）。**核实结果**：无字段缺失，无需 successor。

### Phase 2 - 后端实现：createReceiveFromAsn 行级回填扩展

Status: completed
Targets: `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/entity/ErpB2bAsnBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：扩展 `createReceiveFromAsn` —— 在现有 `daoProvider().daoFor(ErpPurReceive.class).saveEntity(receive)`（`:223`）后增 iterate `asn.getLines()`（cascade to-many）→ 为每条 AsnLine 创建 `ErpPurReceiveLine` + 字段映射（按 Phase 1 Decision (a)-(f) 含 uoMId 来源 Decision (f)）+ saveEntity 持久化（按 Phase 1 Decision (a) 迭代策略）。
  - Skill: `nop-backend-dev`
- [x] `Add`：错误处理 —— 任一 ReceiveLine 持久化失败抛 `NopException`（按 Phase 1 Decision (b) 强一致）+ LOG.warn 上下文（asn.code / lineNo / 错误消息）。
  - Skill: `nop-backend-dev`
- [x] `Add`：空白 AsnLine 处理 —— 按 Phase 1 Decision (e) 落地（若 0 行合法则仅头创建；若非法则抛守卫）。
  - Skill: `nop-backend-dev`
- [x] `Add`：扩展 JUnit `module-b2b/erp-b2b-service/src/test/java/app/erp/b2b/service/TestErpB2bAsnInventoryIntegration.java`（已存在，含 `testCreateReceiveFromMatchedAsn` l.82 / `testCreateReceiveFromUnmatchedAsnFails` l.111）—— 新增 ≥3 用例：① 多行 AsnLine（≥2 行）→ Receive 多行映射 + 字段精确数值断言；② 空白 AsnLine 边界；③ config-gated 关闭路径（默认 false → null 返回守卫）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] `mvn test -pl module-b2b/erp-b2b-service -am` JUnit 全绿（既有 + 新增 ≥3 用例）。**核实结果**：31 tests 全绿（含既有 3 + 新增 3 = 6 用例 TestErpB2bAsnInventoryIntegration + 25 既有其他测试）。
- [x] `createReceiveFromAsn` 行级回填逻辑经单元测试覆盖（多行 + 空白 + config-gate 三路径）。

#### Phase 2 实施摘要

- **后端扩展（`ErpB2bAsnBizModel.java`）**：
  - 新增 import `ErpMdMaterial`、`ErpPurReceiveLine`、`RoundingMode`。
  - 在 `createReceiveFromAsn` 现有 `daoProvider().daoFor(ErpPurReceive.class).saveEntity(receive)` 之后插入 `fillReceiveLinesFromAsn(asn, receive, po)` 调用。
  - 新增 protected 风格的 private helper `fillReceiveLinesFromAsn(ErpB2bAsn, ErpPurReceive, ErpPurOrder)`：按 Phase 1 Decision 落地字段映射（materialId/uoMId/quantity/unitPrice/taxRate/orderLineId/amount/warehouseId/lineNo）+ 空白边界 + 守卫。
- **错误码扩展（`ErpB2bErrors.java`）**：新增 `ARG_MATERIAL_ID`、`ARG_LINE_NO`、`ERR_B2B_ASN_LINE_MATERIAL_REQUIRED`（materialId null/物料主数据不存在守卫）。
- **JUnit 扩展（`TestErpB2bAsnInventoryIntegration.java`）**：
  - 更新既有 `testCreateReceiveFromMatchedAsn`：补 `seedMaterial` + `seedPurchaseOrderLine` + `fixAsnLineMaterialId`（webhook 解析 AsnLine materialId=null 已知 gap，后置回填）+ ReceiveLine 字段精确数值断言。
  - 新增 `testCreateReceiveFromAsnMultiLineMapping`（2 Material × 2 PO line × 2 AsnLine → 2 ReceiveLine + 字段精确数值断言：materialId 透传 / uoMId 反查 / lineNo 透传 / quantity=shippedQty / unitPrice 反查 / amount 派生）。
  - 新增 `testCreateReceiveFromAsnEmptyLines`（0 AsnLine 边界 → Receive 头仍创建 + 0 ReceiveLine + ASN RECEIVED_TO_STOCK，Decision (e)①）。
  - 新增 `testCreateReceiveFromAsnConfigGateDisabled`（config-gated 关闭 → 返回 null + ASN 状态保持 + 不建 Receive，Decision config-gate 路径）。
  - 新增 helper：`seedPurchaseOrderLine`、`seedMaterial`、`seedMatchedAsnDirectly`、`seedAsnLine`、`fixAsnLineMaterialId`、`findReceiveLinesByReceiveId`。
- **快照基线**：6 用例经 `forceSaveOutput=true` 一次性录制（含新增 erp_pur_receive_line / erp_md_material / erp_pur_order_line 表），切回 CHECKING 全绿。

### Phase 3 - 浏览器层 E2E + owner-doc + RELEASED

Status: completed
Targets: `tests/e2e/business-actions/b2b-asn-line-level-receive-fill.action.spec.ts`（新 spec）+ `docs/testing/e2e-runbook.md` + `docs/design/b2b/edi-formats.md` + `docs/backlog/README.md` + `docs/logs/2026/07-19.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Add`：新建 `tests/e2e/business-actions/b2b-asn-line-level-receive-fill.action.spec.ts`（≥2 用例）：
  - **(1) 多行映射正路径**：自包含建 PO（≥2 line，materialId 各异）+ ASN（≥2 AsnLine 对应 PO line materialId）→ matchPurchaseOrder（MATCHED）→ createReceiveFromAsn → 经 `findFirst`/GraphQL 反查新建 ErpPurReceive.line 列表，断言行数 == AsnLine 行数 + 每行字段（materialId/quantity 按 Phase 1 Decision (a)(b)(c)(d) 映射规则）精确数值断言 + Receive 头 code/orderId/supplierId 透传断言（对齐 0941-2 范式）+ ASN status=RECEIVED_TO_STOCK。
  - **(2) config-gated 关闭路径守卫**：webServer JVM arg 关闭 erp-b2b.asn-auto-create-receive（如 spec 层可改 config 则改；否则新建 spec 仅覆盖正路径 + 注释说明 config 路径在 JUnit 覆盖）。**裁决待 Phase 1 Explore**：playwright.config.ts webServer args 全局生效无法 per-spec toggle → 本 spec 假设 config 开启，config 关闭路径仅 JUnit 覆盖（Phase 2 用例 ③）。
  - Skill: `nop-testing`
- [x] `Proof`：`PLAYWRIGHT_PORT=8011 npx playwright test tests/e2e/business-actions/b2b-asn-line-level-receive-fill.action.spec.ts --workers=1` 全绿 + business-actions 全套件回归 0 新增失败。
  - Skill: `nop-testing`
- [x] `Add`：`docs/testing/e2e-runbook.md` 业务动作表 +1 b2b ASN 行级回填行 + 套件计数段补本计划增量。
- [x] `Add`：`docs/design/b2b/edi-formats.md` 增「createReceiveFromAsn 行级回填实现注记」段（字段映射规则 + Phase 1 Decisions 落地）。
- [x] `Add`：`docs/backlog/README.md` +1 done 行 + `docs/logs/2026/07-19.md` 聚合日志条目（含范围/裁决/验证状态/范围纪律）。
- [x] `Add`：1005-1 Deferred 段补 `**RELEASED by 2026-07-19-0849-1**` 行 + 实施摘要（多行映射 + 字段精确数值断言）。**注**：0941-2 Deferred 段无 createReceiveFromAsn 行级回填条目（其 Deferred 仅含 logistics scanForPolling/path-2 + b2b handleInboundWebhook），故 0941-2 无须 RELEASED 登记。

Exit Criteria:

- [x] 新 spec ≥2 用例全绿（含多行映射正路径 + 守卫或对照）+ business-actions 回归 0 新增失败。**核实结果**：新 spec 2 用例 passed（多行映射 + 空白 AsnLine 边界对照）+ b2b-asn-match-receive 3 用例 0 新增失败。
- [x] owner-doc + e2e-runbook + backlog + logs + RELEASED 登记 5 处对齐。

#### Phase 3 实施摘要

- **新 spec**（`b2b-asn-line-level-receive-fill.action.spec.ts`，2 用例）：
  - (1) 多行映射正路径——2 AsnLine（MAT-001/MAT-002）→ 2 ReceiveLine + 逐行字段精确数值断言（materialId 透传 / uoMId 反查 / quantity=shippedQty / unitPrice 反查 / amount 派生 5×15=75 / 12×8=96）+ ASN RECEIVED_TO_STOCK + Receive 头字段断言（对齐 0941-2 范式）。
  - (2) 空白 AsnLine 边界对照——0 AsnLine → Receive 头仍创建 + 0 ReceiveLine（Phase 1 Decision (e)①）。
- **helper 增强**：`tests/e2e/business-actions/_helper.ts` 新增 `findItems` 导出（行级回填多行反查原语，对齐 `orchestration/_helper.ts:57-63` 范式）。
- **owner-doc**：`docs/design/b2b/edi-formats.md` 附录新增「createReceiveFromAsn 行级回填实现注记」段（字段映射规则权威表 + 边界与守卫 + config 门控 + 跨域依赖方向）。
- **e2e-runbook**：业务动作表 +1 b2b 行级回填行（区分 0941-2 仅头无行 vs 本计划行级回填）+ 套件计数 87→88（含表头声称修正）+ 套件计数段补本计划增量。
- **backlog**：README +1 done 行（含范围/裁决/验证状态/范围纪律）。
- **logs**：`docs/logs/2026/07-19.md` 顶部追加聚合日志条目。
- **RELEASED**：1005-1 Deferred 段「createReceiveFromAsn 跨域建单深度（行级回填）」补 RELEASED 标记 + 实施摘要。

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_0881f75f2ffeABKKuL7cPXfV4l`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 2 BLOCKERS + 3 MAJORS + 4 MINORS。**B1** AsnLine 实测无 uoMId 字段（plan l.15 误标）；**B2** Phase 1 Decision 缺 uoMId 来源裁决（ReceiveLine.uoMId mandatory=true，AsnLine 无 uoMId 字段）；**M1** spec 引用 l.33-34 错（实际 l.18/l.132）；**M2** `TestErpB2bAsnCreateReceive` 不存在，应为 `TestErpB2bAsnInventoryIntegration`；**M3** 0941-2 Deferred 段无 ASN 行级回填条目（仅 1005-1 有），故 0941-2 无须 RELEASED 登记。
- **本 iter-1 修订**：依据 B1 修正 AsnLine 字段集（移除 uoMId）；依据 B2 新增 Phase 1 Decision (f) uoMId 来源裁决（4 候选 + 裁决依据）；依据 M1 修正 spec 引用为 l.18 + l.132；依据 M2 改 Phase 2 JUnit 目标为 `TestErpB2bAsnInventoryIntegration`（已存在含 testCreateReceiveFromMatchedAsn l.82）+ Current Baseline 增既有 JUnit 覆盖段；依据 M3 显式注记 0941-2 Deferred 段无此条目故仅 1005-1 RELEASED 登记。
- Independent draft review iteration 2: **accept** (`ses_088188950ffeWPVnddkbRcCi3r`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-19) — 0 BLOCKERS / 0 MAJORS / 4 MINORS（iter-1 范围外的 polish 项不再 iter-2 验证）。iter-1 全部 B1/B2/M1/M2/M3 经实时仓库逐行核实 FIXED。计划作为执行契约进入实施。

## Closure Gates

> 本计划含后端 Java 扩展（行级回填）+ 浏览器层 E2E。结束前运行 JUnit + 新 spec + 全套件回归 + 154 模块构建（确认后端扩展未污染其他模块）。

- [x] 范围内行为完成（行级回填后端 + ≥1 spec ≥2 用例 + 字段映射 Decision 落地）
- [x] 相关文档对齐（edi-formats.md 实现注记 + e2e-runbook 业务动作表 + backlog/logs）
- [x] 已运行验证：`mvn test -pl module-b2b/erp-b2b-service -am` JUnit 全绿 + 新 spec 全绿 + business-actions 回归 0 新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS
- [x] 无范围内项目降级为 deferred/follow-up（若 Explore 发现须 ORM 加列开独立 successor 不属降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

> 草案期预登记执行期可能遇到的降级项（取决于 Phase 1 Explore 结果）。执行期确认后分类。

### Receive 后续 approve/入库触发链深度 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅覆盖 createReceiveFromAsn 单动作的行级回填；Receive approve + 入库触发经 1132-1 已覆盖（purchase 域独立 E2E）。深度编排跨多个业务动作属不同结果面。
- Successor Required: `yes`（触发条件：ASN→PO→Receive→approve→入库完整跨域编排浏览器层 E2E 需求落地时）

### 价格差异/继承处理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划 unitPrice 来源经 Phase 1 Decision 单一规则裁决；PO vs ASN 价格差异处理属采购价格管理 successor。
- Successor Required: `yes`（触发条件：采购价格管理 / 多价格表业务需求落地时）

## Closure

Status Note: 执行完成（2026-07-19）。3 Phase 全绿——Phase 1 探索笔记 + 5 字段映射 Decision（含 uoMId 关键 Decision (f) 经 Proof 核实 `ErpMdMaterial.uoMId` mandatory=true 必然有值）+ 2 实现策略 Decision（迭代 saveEntity + 强一致回滚）在 plan 内记录；Phase 2 后端 `ErpB2bAsnBizModel.createReceiveFromAsn` 扩展——新增 private helper `fillReceiveLinesFromAsn`（iterate AsnLine → ErpPurReceiveLine 字段映射）+ 新 ErrorCode `ERR_B2B_ASN_LINE_MATERIAL_REQUIRED` + JUnit `TestErpB2bAsnInventoryIntegration` +3 用例（既有 1 用例更新）；Phase 3 1 新 spec 2 用例（多行映射正路径 + 空白 AsnLine 边界对照）+ `_helper.ts` 新增 `findItems` 导出 + `edi-formats.md` 附录实现注记 + e2e-runbook 业务动作表 +1 行 + 套件计数 87→88 + backlog README done 行 + 当日日志聚合条目 + 1005-1 Deferred RELEASED。验证基线：JUnit 6/6 + b2b-service 31 passed 0 failures + 新 spec 2/2 + b2b-asn-match-receive 回归 3/3 0 新增失败 + 154 模块 BUILD SUCCESS；零生产 ORM/契约/字典/种子/config 变更（纯应用层 Java + 测试 + 文档）。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor（general subagent，新会话冷审计无执行者上下文，2026-07-19）。Verdict: **audit pass** / 0 BLOCKERS。逐项核实：
  - Plan Status=completed + 三 Phase 所有 items `[x]` + Exit Criteria `[x]` + 8 Closure Gates `[x]` 全部勾选。
  - **Phase 2 后端实测**：`ErpB2bAsnBizModel.java:229` 调用 `fillReceiveLinesFromAsn(asn, receive, po)`；`:258-318` 私有 helper 实现字段映射（materialId 透传 / uoMId 经 `materialDao.getEntityById` 反查 / quantity=shippedQty 优先 fallback quantity / unitPrice+taxRate+orderLineId 经 `findMatchingPoLine` 反查 / amount=unitPrice×qty HALF_UP scale=4 派生 / warehouseId 复用 receive.warehouseId / lineNo 透传）+ 空白 AsnLine 边界（0 行 LOG.warn 提示仅建头，Decision (e)①）+ materialId null/material 不存在双守卫抛 `ERR_B2B_ASN_LINE_MATERIAL_REQUIRED` + try/catch LOG.warn 上下文（asn.code/lineNo/错误消息）+ 异常 re-throw 触发 @BizMutation 事务回滚（强一致，Decision 实现策略 (b)）。反 hollow 通过：方法非空、真实运行时路径、被 `createReceiveFromAsn` 主流程调用。
  - **Phase 2 ErrorCode 实测**：`ErpB2bErrors.java:102-107` 定义 `ARG_MATERIAL_ID`/`ARG_LINE_NO`/`ERR_B2B_ASN_LINE_MATERIAL_REQUIRED` 三项。
  - **Phase 2 JUnit 实测**：`TestErpB2bAsnInventoryIntegration.java:134/179/198` 三用例 `testCreateReceiveFromAsnMultiLineMapping`/`testCreateReceiveFromAsnEmptyLines`/`testCreateReceiveFromAsnConfigGateDisabled` 真实落地。
  - **Phase 3 spec 实测**：`tests/e2e/business-actions/b2b-asn-line-level-receive-fill.action.spec.ts` 存在；`_helper.ts:172-179` 新增 `findItems` 导出。
  - **Phase 3 文档对齐实测**：`docs/design/b2b/edi-formats.md:545+` 附录「createReceiveFromAsn 行级回填实现注记」段（字段映射权威表 + 边界与守卫 + config 门控 + 跨域依赖方向）；`docs/testing/e2e-runbook.md:117/292/432` 业务动作表 +1 b2b 行级回填行 + 套件计数 87→88；`docs/backlog/README.md:111` done 行；`docs/logs/2026/07-19.md:5-10` 聚合条目；`docs/plans/2026-07-17-1005-1-b2b-aps-inbound-scheduling-orchestration-e2e.md:176` Deferred 段补 `**RELEASED by 2026-07-19-0849-1**` + 实施摘要。
  - **文本一致性**：顶部 `Plan Status: completed` + Phase 1/2/3 `Status: completed` + 所有 Exit Criteria `[x]` + 8 Closure Gates `[x]` + Closure 段真实证据，全部一致。
  - **范围纪律**：Deferred But Adjudicated 仅含显式 successor（Receive 后续 approve/入库触发链深度 + 价格差异/继承处理），均命名触发条件，无范围内的已确认缺陷/契约漂移被静默降级。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
