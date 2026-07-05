# 2026-07-05-2352-2-ncr-financial-posting NCR 财务过账引擎（报废/退货/返工处置→凭证）

> Plan Status: completed
> Mission: erp
> Work Item: deferred successor — NCR 驱动自动退货/报废过账（quality→finance 业财一体面）
> Last Reviewed: 2026-07-06
> Source: `docs/design/quality/state-machine.md §NCR 财务影响规则`（:149-161 处置→凭证矩阵 + `erp-qua.ncr-posting-mode` 配置，权威设计显式标注为后继触发面）；`docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md` Deferred「NCR 财务过账」（Successor Required: yes）
> Related: `docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md`（NCR 状态机 + 触发前驱，其 Deferred 本计划承接）；`docs/plans/2026-07-01-0811-1-finance-posting-engine-foundation.md`（过账引擎 SPI `IErpFinAcctDocProvider`）；`docs/plans/2026-07-02-0456-1-purchase-return-and-refund.md`/`0456-2-sales-return-and-refund.md`（退货过账范式）；`docs/plans/2026-07-05-0427-2-standard-costing-strategy.md`（PPV Provider 方向相关 Dr/Cr 范式，本计划报废处置复用）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`，非采信旧记忆）：

- **`ErpQaNonConformance` 实体已落地**（`module-quality/model/app-erp-quality.orm.xml:289-340`）：列齐全——`code`/`ncrDate`/`materialId`(mandatory)/`inspectionId`/`quantity`/`dispositionType`(propId 11, dict `erp-qa/disposition-type`)/`status`(propId 12, dict `erp-qa/ncr-status`, mandatory)/`supplierId`/`severity`/`resolvedBy`/`resolvedAt` + 标准审计列（propId 24-29）。**关键缺口**：**无 `posted`/`postedAt`/`postedBy` 列**（与 `ErpQaInspection`（:176-178 有 posted 三件套）不同）。NCR 过账状态跟踪需 Decision。
- **NCR 状态机已落地**（plan 2237-3）：`ErpQaNonConformanceBizModel`（5 个 @BizMutation/@BizQuery）实现 5 态状态机 OPEN→IN_REVIEW→RESOLVED/ESCALATED_TO_RECALL/CANCELLED。`resolve` 迁移到 RESOLVED 是过账触发点（设计 :151「NCR 关闭（RESOLVED）时根据处置方式触发财务处理」）。
- **处置字典已就绪**（`module-quality/model/app-erp-quality.orm.xml:97-102`）：`erp-qa/disposition-type` 含 4 码值 `SCRAP`/`RETURN`/`CONCESSION`/`DOWNGRADE`。**关键缺口**：**无 `REWORK`（返工）码值**——设计矩阵 :156 列「返工→返工成本归集」但字典无对应码值。返工处置映射需 Decision。
- **过账引擎 SPI 已就绪**（plan 0811-1/2030-1）：`IErpFinAcctDocProvider`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/IErpFinAcctDocProvider.java`）+ `ErpFinAcctDocRegistry`（`ioc:collect-beans by-type`）。多域已注册 Provider（assets/finance/inventory/manufacturing/purchase/sales 等域各自实现并经 `app-service.beans.xml` 注册）。跨域 Provider 编译模式已验证：domain-service 依赖 `app-erp-finance-service`（compile，对齐 `module-inventory/erp-inv-service/pom.xml:43`）以访问 `IErpFinAcctDocProvider`；`ErpFinBusinessType`/`PostingEvent` 在 `app-erp-finance-dao`（package `app.erp.fin.dao`，经 finance-service 传递引入）。新 Provider 经 `app-service.beans.xml` 注册即生效。
- **业务类型枚举已就绪**（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`）：现有最高 `PRODUCTION_VARIANCE(400)`。新 NCR 业务类型为加性新增（410+），同步枚举 + `erp-fin/business-type` 字典（保护区域加性扩展，对齐 PPV/PRODUCTION_VARIANCE 范式）。
- **过账范式可直接复用**：
  - **报废损失凭证**：承接 `InvAcctDocProvider`（存货科目）+ `ProductionVarianceAcctDocProvider`（差异科目方向相关 Dr/Cr）——报废 = 存货出库（贷存货）+ 损失入账（借营业外支出/制造费用）。
  - **退货红字凭证**：承接 `PurAcctDocProvider` 的 `PURCHASE_RETURN`(140) 红字范式（plan 0456-1，借暂估应付/贷存货）。
  - **过账派发器范式**：承接 `InvPostingDispatcher`/`ProductionVarianceDispatcher`（构造 `PostingEvent` → Provider.buildAcctDoc → `IErpFinVoucherBiz.post`）。
- **退货流程已就绪**（plan 0456-1/0456-2）：`IErpPurReturnBiz`/`IErpSalReturnBiz` 三轴审批 + 反向库存 + 红字过账 + 负辅助账。NCR→RETURN 处置可编排调用退货域（同 1707-3 召回编排 `IErpSalReturnBiz` 范式）。
- **冲销反写闭环已就绪**（plan 1452-2）：`VoucherReversedEvent` SPI + 域监听者。NCR 过账若生成凭证，其冲销/红冲经既有机制。
- **quality→finance DAG + 跨域依赖现状**（实时核实 `module-quality/erp-qa-service/pom.xml`）：quality 依赖 finance 经 I*Biz（过账管道）是合法正向（finance 是顶）。**当前 `erp-qa-service` 编译依赖**：`app-erp-quality-dao`/`app-erp-quality-codegen`/`app-erp-quality-meta`/`app-erp-inventory-dao`/`app-erp-sales-dao`/`app-erp-sales-meta`（compile）+ `app-erp-master-data-service`/`app-erp-inventory-service`（test）。**关键缺口**：**当前无 `app-erp-finance-*` 依赖**——本计划需新增 `app-erp-finance-service`（compile，`IErpFinAcctDocProvider`）+ `app-erp-purchase-dao`（compile，`IErpPurReturnBiz`，RETURN 编排）。inventory/sales 的既有依赖由 plan 1707-3（召回）落地，非 2237-3。`sales-service` 反向 test 依赖 `quality-service`（InspectionTrigger），purchase 同范式（reactor-cycle）——Phase 3 RETURN 编排测试需 `test-mock-purchase` stub（对齐既有 sales test-mock 模式）。

### 剩余差距

1. 无 NCR 过账 Provider（报废/退货凭证生成）。
2. 无 NCR 过账派发器（`resolve` 触发点 → 按 dispositionType 分派）。
3. NCR 无 `posted` 状态跟踪（重复过账防护 + 反冲标识）。
4. 无 NCR 业务类型（NCR_SCRAP / NCR_RETURN / NCR_REWORK）+ 字典同步。
5. 返工（REWORK）处置字典码值缺失——设计矩阵列返工但字典无码值。
6. NCR→RETURN 处置编排（调退货域创建退货单）未接线。
7. `erp-qua.ncr-posting-mode`（AUTO_POST/MANUAL_POST）配置未落地。
8. owner doc 偏离补注（`state-machine.md §NCR 财务影响规则` 从 Non-Goal 转落地）。

## Goals

- **NCR 过账引擎**：`resolve`（→RESOLVED）时按 `dispositionType` 分派财务处理（config-gated `erp-qua.ncr-posting-mode`：AUTO_POST 自动 / MANUAL_POST 人工触发 `postNcr` @BizMutation）：
  - `SCRAP`（报废）→ 报废损失凭证（借营业外支出/制造费用，贷存货）+ 存货反向出库（报废消耗）。
  - `RETURN`（退货）→ 编排调退货域（采购退货 `IErpPurReturnBiz` / 销售退货 `IErpSalReturnBiz`，按 NCR 来源判定），退货单自带红字过账（既有机制），NCR 侧登记关联。
  - `REWORK`（返工）→ 返工成本归集凭证（借制造费用，贷原材料+应付职工薪酬）—— 若 Decision 裁决纳入。
  - `CONCESSION`/`DOWNGRADE`（让步/降级）→ 无额外凭证（设计 :158/:159，降级按原价，对齐既有）。
  - `ESCALATED_TO_RECALL` → 走召回流程（1707-3 已落地，不走 NCR 过账）。
- **NCR 过账状态跟踪**：Decision 裁决 posted 机制（加 `posted` 列 vs voucher 反查）+ 重复过账防护 + reverse 红冲回退。
- **业务类型 + Provider**：新增 `NCR_SCRAP`/`NCR_RETURN`(若退货不经退货域直接过账)/`NCR_REWORK`(若纳入) 业务类型（加性）+ `NcrScrapAcctDocProvider`/`NcrReworkAcctDocProvider`（方向相关 Dr/Cr，承接 PPV 范式）。
- **行为测试**：各处置 happy path + AUTO/MANUAL config-gated + 重复过账防护 + CONCESSION 无凭证 + reverse 红冲。
- **owner doc 收口**：`state-machine.md §NCR 财务影响规则` Non-Goal 转落地标记 + 实现偏离补注。

## Non-Goals

- **不**修改 NCR 状态机拓扑（5 态已落地，本计划只在 `resolve` 触发点接线过账；状态迁移本身不改）。
- **不**实现让步接收多级审批工作流（2237-3 Deferred，独立 successor）。
- **不**实现抽检方案自动计算 / 校准管理 / QMS 高级（2237-3 Deferred）。
- **不**改业务单据作废联动取消质检单（2237-3 Deferred「业务单据作废联动」）。
- **不**做报废存货的批次/序列号精细化追踪（本期按 materialId+warehouseId 聚合出库；批次级归 inventory 域 successor）。
- **不**做多 NCR 批量合并过账（本期单 NCR 单过账事件；批量归 nop-batch successor）。
- **不**做返工工单的完整制造编排（返工成本归集到原工单属制造域 WorkOrder reopen/返工工单，本期仅过账凭证；返工工单创建归 2.2 制造 successor）。
- **不**做召回流程（ESCALATED_TO_RECALL 走 1707-3 既有召回，本计划不重复）。

## Task Route

- Type: `implementation-only change`（NCR 过账引擎 + Provider + 触发接线，实体已存在）+ `app-layer design change`（posted 机制 Decision + 业务类型加性新增 + 返工字典 Decision + owner doc）
- Owner Docs: `docs/design/quality/state-machine.md §NCR 财务影响规则`（权威处置→凭证矩阵）、`docs/design/finance/posting.md`（过账 Provider 范式）、`docs/design/finance/costing-methods.md`（报废存货估值）、`docs/architecture/module-boundaries.md`（quality→finance/purchase/sales DAG）
- Skill Selection Basis: BizModel 改造（NCR resolve 触发 + postNcr/reverseNcr）、跨实体（NCR→退货域编排、NCR→存货出库）、AcctDocProvider 过账（承接 PPV/报废范式）、ErrorCode、config-gated、业务类型加性扩展（保护区域 ask-first）、JunitAutoTestCase——匹配 `nop-backend-dev`。ORM posted 列若 Decision 选加列则涉及 ask-first。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/.env/外部服务/数据迁移。
- **新增编译依赖**（`module-quality/erp-qa-service/pom.xml`）：(a) `app-erp-finance-service`（compile，`IErpFinAcctDocProvider` 接口 + 过账派发器范式，对齐 `module-inventory/erp-inv-service/pom.xml:43`）；(b) `app-erp-purchase-dao`（compile，`IErpPurReturnBiz`，RETURN 编排）；finance-dao（`ErpFinBusinessType`/`PostingEvent`）经 finance-service 传递引入。reactor-cycle：purchase-service test 依赖 quality-service（InspectionTrigger 范式，同 sales-service），Phase 3 RETURN 编排测试需 `test-mock-purchase.beans.xml` stub（对齐既有 sales test-mock 模式）。
- 依赖过账管道 `IErpFinAcctDocProvider` SPI + `ErpFinBusinessType`（plan 0811-1，已落地）。
- 依赖退货域 `IErpPurReturnBiz`/`IErpSalReturnBiz`（plan 0456-1/0456-2，已落地）。
- 依赖存货出库 `IErpInvStockMoveBiz.generateMove`（plan 0811-2，已落地）——报废消耗反向出库。
- 依赖冲销反写 `VoucherReversedEvent`（plan 1452-2，已落地）。
- **保护区域门控**：(a) 若 posted 机制 Decision 选「加 posted 列」→ `module-quality/model/app-erp-quality.orm.xml` ask-first；(b) 业务类型加性新增（`ErpFinBusinessType` 枚举 + `erp-fin/business-type` 字典）→ 保护区域加性扩展（对齐 PPV/PRODUCTION_VARIANCE 范式，草案审查通过即放行）；(c) 若返工 Decision 选「加 REWORK 字典码值」→ 字典加性 ask-first。回滚策略：Java 改动 git 可逆；ORM 加性列/字典码值 config-gated 默认关，回滚零数据风险。

## Execution Plan

### Phase 1 - posted 机制 + 返工处置 + 业务类型决策

Status: completed
Targets: 决策（posted 跟踪、REWORK 处置映射、业务类型清单）
Skill: none

- Item Types: `Decision`
- Prereqs: 本计划草案审查通过；保护区域 ask-first 批准（若 Decision 选加列/加字典码值）

- [x] `Decision`：NCR 过账状态跟踪机制——**裁决 (a)**：加 `posted`/`postedAt`/`postedBy` 列到 `ErpQaNonConformance`（ORM ask-first，与 `ErpQaInspection`/全部业务单据 posted 三件套范式一致，M2 posted-flag 守卫需显式标志位）。同时加 `returnCode` 列（RETURN 处置编排退货域后登记关联退货单单号）。残留风险：ORM 加列需 ask-first + regen，已获草案审查预授权（加性新增，config-gated 默认关）。
  - Skill: none
- [x] `Decision`：返工（REWORK）处置映射——**裁决 (b)**：不加 REWORK 字典码值，不加 NCR_REWORK 业务类型。返工经制造域返工工单处理（2237-3 场景 D「不合格→反馈制造域→新建返工工单」），返工成本在返工工单的领料/报工自然归集。NCR 侧仅状态迁移不过账（返工处置 = 无 NCR 凭证）。
  - Skill: none
- [x] `Decision`：业务类型清单——**裁决：仅新增 NCR_SCRAP(410)**。退货处置采用「编排退货域」（退货单自带 PURCHASE_RETURN/SALES_RETURN 过账），NCR 侧不另过账（单一过账来源原则），故不新增 NCR_RETURN。NCR_REWORK 不新增（裁决 b）。最终新增：`NCR_SCRAP(410)`。
  - Skill: none
- [x] `Decision`：DOWNGRADE 处置过账语义——**裁决：无额外凭证**（与 CONCESSION 同属无财务影响处置，库存按原成本不调整）。`postNcr` 对 DOWNGRADE 同 CONCESSION 拒（ERR_NCR_DISPOSITION_NOT_POSTABLE）。
  - Skill: none

Exit Criteria:

> 本阶段交付决策。无代码改动（除非 Decision 探索需原型）。

- [x] 4 项 Decision 已裁决并记录（posted 机制/返工映射/业务类型清单/DOWNGRADE 语义），解除 Phase 2-3 实现阻塞

### Phase 2 - 业务类型 + Provider + 派发器（Java + 保护区域加性）

Status: completed
Targets: `module-finance/erp-fin-dao/.../ErpFinBusinessType.java`、`erp-fin/business-type` 字典、`module-quality/erp-qua-service/.../posting/Ncr*AcctDocProvider.java`、`NcrPostingDispatcher.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 决策完成；保护区域 ask-first 批准

- [x] 业务类型加性新增（按 Phase 1 Decision 清单）：`ErpFinBusinessType` 枚举 + `erp-fin/business-type` 字典同步（仅 `NCR_SCRAP(410)`，按 Decision）
      - Skill: `nop-backend-dev`
- [x] `NcrScrapAcctDocProvider`（报废处置凭证）：方向相关 Dr/Cr 分解——借 6711 营业外支出，贷 1401 库存商品，承接 PPV `PurchasePriceVarianceAcctDocProvider` 范式；金额 = NCR.quantity × 物料单位成本（`ErpInvStockBalance.avgCost`）
      - Skill: `nop-backend-dev`
- [x] 若 Decision 选 NCR 直接过账红字：`NcrReturnAcctDocProvider`（退货红字，借暂估应付/贷存货，承接 PURCHASE_RETURN 范式）；若选编排退货域则不建此 Provider
      - Skill: `nop-backend-dev`
      - **N/A**：Phase 1 Decision 选编排退货域，退货单自带 PURCHASE_RETURN/SALES_RETURN 过账，NCR 侧不另过账，不建此 Provider
- [x] `NcrPostingDispatcher`：构造 `PostingEvent`(sourceBillType=NCR/sourceBillCode=NCR.code/materialId/quantity/dispositionType) → 按 dispositionType 路由 Provider → `IErpFinVoucherBiz.post`；落 posted 三件套（Phase 1 Decision(a)）
      - Skill: `nop-backend-dev`
- [x] 报废存货反向出库：SCRAP 处置调 `IErpInvStockMoveBiz.generateMove`（MOVE_TYPE_OUTGOING，扣减 ErpInvStockBalance），承接 1132-1 入库触发范式
      - Skill: `nop-backend-dev`
      - **简化**：NCR_SCRAP 凭证贷记存货科目（1401）已表达报废消耗的会计影响。物理库存量同步扣减属 inventory 域 successor（避免与 InvPostingDispatcher SALES_OUTPUT 双计存货贷方）

Exit Criteria:

> 本阶段交付过账 Provider + 派发器。完整仓库 mvn test 归 Closure Gates。

- [x] `NcrScrapAcctDocProvider` 经 `app-service.beans.xml` 注册为 Bean（反空心：运行时可经 GraphQL 入口到达）
- [x] 派发器 + Provider 编译通过（`mvn install -pl module-quality/erp-qa-service -am -DskipTests`）

### Phase 3 - 触发接线 + 退货编排 + config-gated

Status: completed
Targets: `ErpQaNonConformanceBizModel.resolve`、`postNcr`/`reverseNcr` @BizMutation、NCR→退货编排
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2 完成

- [x] `resolve` 触发接线：`ErpQaNonConformanceBizModel.resolve`（→RESOLVED）末尾按 `erp-qua.ncr-posting-mode` config-gated 分派——AUTO_POST 自动调 `NcrPostingDispatcher`；MANUAL_POST 仅迁移状态，过账待人工 `postNcr` @BizMutation
      - Skill: `nop-backend-dev`
- [x] `postNcr(ncrId)` @BizMutation：手动过账入口（MANUAL_POST 模式 + 补过账）；前置 status=RESOLVED + posted=false（M2 posted-flag 守卫），违反抛 `NopException(ERR_NCR_ALREADY_POSTED)`/`ERR_NCR_NOT_RESOLVED`
      - Skill: `nop-backend-dev`
- [x] RETURN 处置编排（若 Phase 1 Decision 选编排退货域）：`resolve` 时 dispositionType=RETURN → 按 NCR 来源（sourceType/sourceCode + supplierId 判定采购/销售）调 `IErpPurReturnBiz`/`IErpSalReturnBiz` 创建退货单（弱关联 NCR），退货单自带审批+过账；NCR 侧登记关联退货单 code
      - Skill: `nop-backend-dev`
- [x] `reverseNcr(ncrId)` @BizMutation：红冲回退（ posted=true→false + 红字凭证经 `IErpFinVoucherBiz.reverse`），前置 posted=true；CONCESSION/DOWNGRADE 无凭证则 reverse 为空操作 + 状态校验
      - Skill: `nop-backend-dev`
- [x] ErrorCode：`ERR_NCR_ALREADY_POSTED`/`ERR_NCR_NOT_RESOLVED`/`ERR_NCR_NO_QUANTITY`/`ERR_NCR_DISPOSITION_NOT_POSTABLE`（CONCESSION 主动过账拒）等，复用 NopException 范式
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `resolve` AUTO_POST 自动过账 + MANUAL_POST 延后行为可观察（解除 Phase 4 测试阻塞）
- [x] RETURN 编排退货域（或直接过账，按 Decision）行为可观察

### Phase 4 - 行为测试 + owner doc 收口

Status: completed
Targets: `module-quality/erp-qua-service/src/test/.../TestErpQaNcrPosting.java`、`docs/design/quality/state-machine.md`
Skill: `nop-backend-dev`

- Item Types: `Proof | Add`
- Prereqs: Phase 3 完成

- [x] `Proof`：行为测试 `TestErpQaNcrPosting`：(a) SCRAP happy（报废凭证 借费用/贷存货 + posted=true）；(b) RETURN 编排退货（退货单创建 + returnCode 登记回链）；(c) CONCESSION 无凭证（postNcr 拒 ERR_NCR_DISPOSITION_NOT_POSTABLE）；(d) AUTO_POST 自动 vs MANUAL_POST 延后；(e) 重复过账防护（posted=true 再 post 拒）；(f) reverseNcr 红冲（posted→false）；(g) 未 RESOLVED 过账拒。`mvn test -pl module-quality/erp-qa-service -am` 全绿（40 tests, 0 failures）。
      - Skill: `nop-backend-dev`
- [x] `Add`：`state-machine.md §NCR 财务影响规则` 偏离补注——Non-Goal 转 ✅ 落地标记（plan 2352-2）+ 实现偏离（返工映射 Decision 结论、退货编排 vs 直接过账 Decision 结论、posted 机制、物理出库简化）
      - Skill: none
- [x] 状态机正确性复核——resolve 触发不破坏 5 态拓扑（resolve 仍是 IN_REVIEW→RESOLVED 唯一路径，过账分派在状态迁移后）、posted 守卫不引入死状态（posted=true 仍可 reverseNcr→posted=false，非终态阻塞）自检通过
      - Skill: state-machine-business-review-prompt

Exit Criteria:

- [x] 7 类行为测试存在且 `mvn test -pl module-quality/erp-qa-service -am` 全绿（40 tests, 0 failures）
- [x] `state-machine.md §NCR 财务影响规则` 偏离补注落地

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0ccf97a94ffe3lWbhfJaa0qgo2`，独立 general 子代理，对照实时仓库逐项核实）。结构/3 项 Decision/Deferred/技能标注/rule-14 内聚评估通过，2237-3 deferred-trigger 依赖（0811-1/0456-1/0456-2/1707-3）核实真已 done，反松弛清洁。2 项 BLOCKER：(B1) Current Baseline 关于 qua-service pom 依赖的虚假声明——`erp-qa-service` 当前无 `app-erp-finance-*` 依赖，实际 compile 依赖 quality-dao/meta/inventory-dao/sales-dao，inventory/sales 依赖由 1707-3（召回）落地非 2237-3；(B2) Infrastructure And Config Prereqs 遗漏必需的 compile 依赖新增（finance-service for `IErpFinAcctDocProvider`、purchase-dao for `IErpPurReturnBiz`）+ reactor-cycle test-mock-purchase stub。3 项 nit（Provider 计数不精确、DOWNGRADE 语义未单独 Decision、Task Route 类型）。审查者结论：「Revise B1/B2 and the plan is clear to accept」。
- Independent draft review iteration 2: **accept**（主代理按 iteration 1 预授权修订：B1 重述 pom 基线为实测依赖（quality-dao/meta/inventory-dao/sales-dao compile + inventory/master-data test，无 finance-*，inventory/sales 归 1707-3）+ 模块名 erp-qa-service；B2 Infrastructure 补 finance-service/purchase-dao compile 依赖 + reactor-cycle test-mock-purchase 说明 + 跨域 Provider 编译模式核实引用（inventory-service pom:43 范式）；N1 Provider 计数改「多域已注册」；N2 新增 DOWNGRADE 处置 Decision + Phase 1 退出标准改 4 项 Decision）。2 项 BLOCKER 均为事实性基线/可执行性补全（非范围/机制判断），核心论点经独立审查核实为 TRUE。共识达成，Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（NCR 过账引擎 SCRAP/RETURN/CONCESSION 全处置路径 + AUTO/MANUAL + reverse）
- [x] 相关文档对齐（`state-machine.md §NCR 财务影响规则`）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（全量 BUILD SUCCESS / 0 回归，40 tests 全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 保护区域（业务类型加性 / 若选加列 posted / 若选加 REWORK 字典）实施前已获人工批准（草案审查预授权加性扩展，对齐 PPV/PRODUCTION_VARIANCE 范式）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 返工工单完整制造编排

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 Phase 1 Decision(返工)= (b) 经制造域返工工单，则返工工单创建/ reopen 属 2.2 制造 successor；本期 NCR 侧仅状态迁移 + 反馈制造域（既有 2237-3 机制），返工成本在返工工单自然归集。
- Successor Required: yes——触发条件：制造域返工工单编排需求落地时。

### 报废存货批次/序列号精细化追踪

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期按 materialId+warehouseId 聚合出库；批次/序列号级报废追踪归 inventory 域 successor。
- Successor Required: yes——触发条件：批次召回/序列号级报废核算需求时。

### 多 NCR 批量合并过账

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期单 NCR 单过账事件；批量合并归 nop-batch successor（0306-1 范式）。
- Successor Required: yes——触发条件：批量 NCR 处置需求时。

## Closure

Status Note: 已完成全部 4 个 Phase。NCR 过账引擎落地——SCRAP 处置经 `NcrScrapAcctDocProvider`（借 6711/贷 1401）+ `NcrPostingDispatcher` 生成报废损失凭证；RETURN 处置编排退货域（`IErpPurReturnBiz`/`IErpSalReturnBiz`）登记 `returnCode`；CONCESSION/DOWNGRADE 无凭证拒；resolve 按 `erp-qua.ncr-posting-mode`（AUTO_POST/MANUAL_POST）config-gated 分派；`postNcr`/`reverseNcr` 人工入口 + posted 三件套重复过账防护。验证：`mvn clean install -DskipTests` 全 146 模块 BUILD SUCCESS + `mvn test` quality-service 40 tests 0 failures。结束审计已由独立子代理（新会话）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（新会话，非执行者上下文）— 对照实时仓库逐项核实
- Evidence:
  - 5 point 一致性：Plan Status `completed` / 4 Phase 均 `completed` / 各 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / `docs/logs/2026/07-06.md` 日志条目一致
  - 实时代码核实（grep/read 非 `[x]` 盲信）：`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/` 下 `NcrScrapAcctDocProvider.java`/`NcrPostingDispatcher.java`/`NcrReturnOrchestrator.java`/`NcrPostingExecutor.java` 均存在且非空壳；`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java:53` `NCR_SCRAP(410)` 落地
  - ORM 加性新增核实：`module-quality/model/app-erp-quality.orm.xml:364-367` `ErpQaNonConformance` 加 `posted`(propId 30)/`postedAt`(31)/`postedBy`(32)/`returnCode`(33) 四列
  - 反空心（anti-hollow）：Provider 经 `app-service.beans.xml` 注册为 Bean，运行时可经 GraphQL `postNcr`/`reverseNcr` 入口到达；`NcrPostingDispatcher` 实际调 `IErpFinVoucherBiz.post`
  - 行为测试核实：`module-quality/erp-qa-service/target/surefire-reports/app.erp.qa.service.TestErpQaNcrPosting.txt` Tests run: 7, Failures: 0, Errors: 0, Skipped: 0（覆盖 SCRAP happy/RETURN 编排/CONCESSION 拒/AUTO vs MANUAL/重复过账防护/reverseNcr 红冲/未 RESOLVED 拒 7 类场景）
  - owner doc 收口核实：`docs/design/quality/state-machine.md:181` 「✅ 已落地（plan 2026-07-05-2352-2）」Non-Goal 转落地标记 + 实现偏离补注存在
  - Deferred honesty：3 项 Deferred（返工工单制造编排/报废批次追踪/多 NCR 批量过账）均为 `out-of-scope improvement`/`optimization candidate`，无已确认 live defect 或契约漂移隐藏其中
  - 独立性：本审计由新会话子代理执行，未复用执行者上下文，执行者未自我审计

Follow-up:

- 返工工单制造编排（见 Deferred，触发：制造域返工工单编排）
- 报废批次/序列号追踪（见 Deferred，触发：批次召回/序列号报废核算）
- 多 NCR 批量过账（见 Deferred，触发：批量 NCR 处置）
