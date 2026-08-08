# 2026-08-08-1603-1-rc-mr1-r1-11-pur-receive-over-receipt-tolerance RC-R1.11 — purchase 超收容差校验（P1-RC-019，MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.11（MR1 第一批纯预授权：purchase 超收容差校验——`ErpPurReceiveProcessor.validateBusinessRulesForApprove` 增 receive-vs-order qty 容差校验，P1-RC-019）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.11 行 + `docs/audits/arm-index.md` P1-RC-019 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3.1（RC-R1.11 = 纯 BizModel/Processor 容差校验）
> Related: `docs/design/purchase/use-cases.md`（L1 UC-PUR-02 ②）；`docs/design/purchase/three-way-match.md`（L2 §数量差异 + §不匹配的处理策略）；`docs/audits/2026-08-07-2300-rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime.md`（A4.2.36 运行时确认）；`docs/plans/2026-08-08-1154-3-rc-mr1-r1-10-pur-requisition-multi-supplier-split.md`（同批计划范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-019（arm-index 行）**：UC-PUR-02 ② 超收容差校验（receive-vs-order）完全缺失。L1（`use-cases.md:67`）逐字「入库数量之和 <= 订单数量 * (1 + 超收容差)」。L3 实仓：`ThreeWayMatcher.match:62-107` 只做 invoice-vs-receive 数量比较（`:71-85` invoiceQty vs receivedQty 硬比较无容差），**无 receive-vs-order 容差校验**；`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 `requireSupplierActive`，**无 receive-vs-order qty 校验**；`erp-pur.match-qty-tolerance` 配置（`ThreeWayMatcher.java:52-56/122-124`）读取后空守护置零**两侧均未用**（dead config，P2-MA2-004 配置层面 watch-only 不合并——同根因不同控制点）。
- **A4.2.36 运行时确认（`2026-08-07-2300` 报告）**：`ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168` 仅 requireSupplierActive 无 receive-vs-order qty 容差校验 + `ThreeWayMatcher.match` 只做 invoice-vs-receive + 配置两侧均未用。"订单 10 + 入库 20（超收 100%）" approve 无门控通过。**维持 P1 不撤销**（Q4=(a) 强制实现，修复归 MR1 纯 BizModel/Processor 预授权）。
- **实仓（HEAD 核查）**：
  - `ErpPurReceiveApproveProcessor.approve:26-49`：`processor.validateNotCancelled` → `validateTransitionForApprove` → `processor.validateBusinessRulesForApprove(receive, context)`（`:34`，位于 `triggerIncomingMove:37` **之前**——校验点正确，超收拒绝发生在库存移动/过账之前）→ `triggerIncomingMove` → 状态推进 → `postProcessApprove`（`rollupOrderReceiveStatus`）。
  - `ErpPurReceiveProcessor.validateBusinessRulesForApprove:166-168`：仅 `requireSupplierActive`，无任何 qty 校验——**插入点**。
  - **聚合模式现成**：`rollupOrderReceiveStatus:244-284` 已实现 per-order-line 跨入库单聚合（`addLineQuantities(receivedByOrderLine, loadLines(currentReceive))` + `findApprovedReceives(orderId)` 遍历其他 APPROVED 入库单累加 + 按 `orderLineId` merge）——超收校验可复用同型聚合（当前入库单行 + 同订单其他 APPROVED 入库单行）。
  - **配置键现成**：`ErpPurConstants.CONFIG_MATCH_QTY_TOLERANCE = "erp-pur.match-qty-tolerance"`（`ErpPurConstants.java:44`，默认 5%）+ `CONFIG_MATCH_STRICT_MODE = "erp-pur.match-strict-mode"`（`:46`，默认 false）；`ThreeWayMatcher` 已有 `isStrictMode()`（`:117-120`）+ `readDecimalConfig`（`:139-147`）读法范式可参照。
  - **错误码**：`ErpPurErrors` 现有 `ERR_INVOICE_QTY_MISMATCH`（发票超入库侧，`ErpPurErrors.java:138`）；receive 侧无对应超收错误码——**新增 `ERR_RECEIVE_QTY_OVER_TOLERANCE`**（带 ARG_RECEIVE_CODE / ARG_LINE_NO / ARG_RECEIVED_QTY / ARG_ORDER_QTY / ARG_TOLERANCE）。
  - **ORM 载体**：`ErpPurReceiveLine.getOrderLineId()`（`:858`）+ `getQuantity()`（`:953`）；`ErpPurOrderLine.getQuantity()`——**零 ORM 变更**。
- **预授权判据**（第一批纯预授权）：纯 BizModel/Processor 代码逻辑修复（校验 + 错误码 + 测试），**不触 ORM 结构/会计核心/删除**；**无 ask-first checkbox**。roadmap RC-R1.11 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReceiveProcessor.java`；`module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/ErpPurErrors.java`；`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveApproval.java`（或新增 `TestErpPurReceiveOverReceiptTolerance.java`）。

## Goals

- **receive-vs-order 超收容差校验**：`ErpPurReceiveProcessor.validateBusinessRulesForApprove` 增 per-order-line 超收校验——按 `erp-pur.match-qty-tolerance`（默认 5%）判定「当前入库单行 + 同订单其他 APPROVED 入库单行」的 Σ数量 > 订单行数量 × (1 + 容差%)；超容差时 strict 模式（`erp-pur.match-strict-mode`=true）抛 `ERR_RECEIVE_QTY_OVER_TOLERANCE` 拒绝审核，非 strict 模式 LOG.warn 放行（对齐 `ThreeWayMatcher` 双模式范式与 L2 `three-way-match.md:99-100`「非严格模式提示警告允许审核通过 / 严格模式拒绝审核」）。
- **边界语义**：容差内（含恰好 = 边界）放行；`orderLineId == null` 的行（无订单关联独立入库）跳过校验；订单行数量为 0 或 null 时按 0 基处理（Σ>0 即超收）。
- **新增错误码**：`ERR_RECEIVE_QTY_OVER_TOLERANCE` + ARG_* 参数（描述中文，对齐既有错误码风格）。
- **测试矩阵**：strict 拒绝 / 非 strict warn 放行 / 容差内放行 / 多入库单聚合超收 / 无订单行跳过 / config 默认值回归——分域 `mvn test -pl module-purchase/erp-pur-service` 全绿 + `_cases/` 快照。
- owner doc `three-way-match.md` 补实现注记（receive-vs-order 校验落地 + 配置复用说明）；回填 arm-index P1-RC-019 → `done (RC-R1.11)` + roadmap RC-R1.11 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**（零列/零索引/零 UK 变更——orderLineId/quantity 载体已就绪）。
- **不做短收容差差异处理**（UC-PUR-06 ⑮ 短收差异处理 = P2-RC-014 watch-only，不同 UC 不同工作项，不在本行范围；本行只做超收方向）。
- **不改 invoice-vs-receive 侧语义**（`ThreeWayMatcher` 的 invoice 数量硬限制 + 死配置读取维持现状——P2-MA2-004 配置层面 watch-only 归 successor，不顺手清理避免混入范围）。
- **不做「容差内调整订单数量」自动行为**（L2 `three-way-match.md:69`「容差内允许并调整订单数量」——"调整"为人工操作语义，不自动改订单行；校验只做允许/拒绝判定）。
- **不改真相源**（use-cases/three-way-match 需求契约段；仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/purchase/use-cases.md`（L1 UC-PUR-02 ②）+ `docs/design/purchase/three-way-match.md`（L2 §数量差异/§不匹配的处理策略）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）+ `docs/audits/2026-08-07-2300-rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime.md`（A4.2.36 运行时证据）
- Skill Selection Basis: 实现面 = Processor protected step 校验方法 + ErrorCode 新增 + config 读取（`nop-backend-dev`：BizModel/Processor 模式、跨实体访问规则、ErrorCode 复用与新增）；测试（`nop-testing`：JunitAutoTestCase/IGraphQLEngine 断言 + 快照录制）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新增 infra/config（复用既有 `erp-pur.match-qty-tolerance` / `erp-pur.match-strict-mode`，均已有默认值，无需 .env）。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-purchase/erp-pur-service`。

## Execution Plan

### Phase 1 - receive-vs-order 超收容差校验实现

Status: completed
Targets: `ErpPurReceiveProcessor.java`；`ErpPurErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`（2 Add：新错误码 + 新 protected step；1 Fix：接线）
- Prereqs: 无（既有基线）

- [x] `Add` `ErpPurErrors` 新增 `ERR_RECEIVE_QTY_OVER_TOLERANCE`（`erp.err.pur.receive-qty-over-tolerance`，中文描述「入库数量超过订单数量与超收容差之和」）+ `ARG_RECEIVE_CODE / ARG_LINE_NO / ARG_RECEIVED_QTY / ARG_ORDER_QTY / ARG_TOLERANCE` 常量。
      - Skill: `nop-backend-dev`
- [x] `Add` `ErpPurReceiveProcessor` 新增 protected step `validateOverReceiptTolerance(ErpPurReceive receive, IServiceContext context)`：读取 `CONFIG_MATCH_QTY_TOLERANCE`（默认 5，`readDecimalConfig` 同型容错）与 `CONFIG_MATCH_STRICT_MODE`（默认 false）；per-order-line 聚合「当前入库单行（`loadLines`）+ 同订单其他 APPROVED 入库单行（`findApprovedReceives` + `addLineQuantities`）」Σ 数量；对 `orderLineId != null` 的行比较 `Σreceived > orderLine.quantity × (1 + tolerance/100)`：strict 抛 `ERR_RECEIVE_QTY_OVER_TOLERANCE`（带参数），非 strict `LOG.warn` 放行；无 `orderLineId` 行跳过。**边界注记**：聚合继承 rollup 现状——CANCELLED 但仍 APPROVED 的入库单（doCancel 仅置 docStatus）计入 Σ，与 `rollupOrderReceiveStatus` 既有口径一致，非本行新引入缺陷。
      - Skill: `nop-backend-dev`
- [x] `Fix` `validateBusinessRulesForApprove` 接线：在 `requireSupplierActive` 之后追加 `validateOverReceiptTolerance(receive, context)`（保持 protected step 模式，派生可覆盖单步）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `ErpPurReceiveProcessor.validateBusinessRulesForApprove` 含超收容差校验调用链；strict 超容差抛 `ERR_RECEIVE_QTY_OVER_TOLERANCE`、非 strict warn 放行、容差内放行——Phase 2 测试断言证实（本阶段不做全仓构建，本地化编译通过即可解锁 Phase 2）
- [x] 无 ORM 结构变更（`git diff --stat` 仅 erp-pur-service Java + `_cases/` 测试快照）

### Phase 2 - 测试矩阵

Status: completed
Targets: `module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurReceiveOverReceiptTolerance.java`（新增，或扩展 `TestErpPurReceiveApproval.java`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` 测试矩阵：① strict 超收拒绝（订单 10 + 入库 20，strict=true → `ERR_RECEIVE_QTY_OVER_TOLERANCE`，approveStatus 保持 SUBMITTED）；② 非 strict 超收 warn 放行（同场景 strict=false → APPROVED）；③ 容差内放行（订单 10 + 入库 10.5，5% 容差 → APPROVED）；④ 多入库单聚合（订单 10 分批入库 6 + 5 = 11 > 10.5 → strict 拒绝第二张入库单）；⑤ 无订单行跳过（`orderLineId=null` 行不触发校验）；⑥ 边界恰好 = 10.5 放行。
      - Skill: `nop-testing`
- [x] `Proof` GraphQL 冒烟断言（`executeRpc` 调 `ErpPurReceive__approve` strict 超收场景返回错误码）+ `_cases/` 快照录制；config 默认值回归（不设 config 时默认 5% 容差 + 非 strict 生效）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试矩阵全绿 + 既有测试零回归：`mvn test -pl module-purchase/erp-pur-service`（BUILD SUCCESS）
- [x] strict 拒绝/非 strict 放行/容差内放行/聚合/跳过/边界六路径均有断言证据（无「行为落地但零覆盖」缺口）；快照录制完成

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/purchase/three-way-match.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [x] `Add` owner doc 补注：`three-way-match.md` §数量差异/§不匹配的处理策略补「receive-vs-order 超收容差校验已落地」实现注记（配置复用 `erp-pur.match-qty-tolerance`/`erp-pur.match-strict-mode` + strict/非 strict 语义 + 短收差异处理 P2-RC-014 successor 注记）；不修改需求契约段（真相源冻结条款遵守）。
      - Skill: none
- [x] `Add` arm-index P1-RC-019 行「修复状态」→ `done (RC-R1.11)` + 修复落地摘要；roadmap RC-R1.11 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 补注落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_01f91f20affeLPJvu7IY8VeyjU）— 基线证据全部实仓核验准确（validateBusinessRulesForApprove:166-168 / rollupOrderReceiveStatus 聚合模式 / ErpPurReceiveApproveProcessor:34 校验点位于 triggerIncomingMove:37 之前 / 配置键 / 错误码现状 / L1 逐字）；语义审查通过（聚合口径 = 当前行 + 其他 APPROVED 入库单行，无重复计数；「容差内调整订单数量」Non-Goal 合理——调整为人工操作语义；排除短收 P2-RC-014 正确——不同 UC 不同控制点）。3 非阻塞 Minor 已修订：Phase 1 Item Types 统一 `Fix` 改 `Add | Fix`（2 Add + 1 Fix）；Phase 1 Exit Criteria 补本地化编译解锁说明；聚合继承 rollup 的「CANCELLED 但仍 APPROVED 计入 Σ」边界显式注记。
- Independent draft review iteration 2: accept（独立子代理 ses_01f89e7a4ffefOHUTeiqnl1TVS 重扫）— 修订后文件无新问题，Item Types 标注正确，无对抗松弛措辞。**计划可标记 active。**

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-purchase/erp-pur-service` 147 tests 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + 全仓 `mvn test` 4122 tests 零失败 + `bash docs/audits/nop-compliance-checker.sh` exit 0 actual==baseline 零漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——范围内项目全落地后关闭；短收容差差异处理 P2-RC-014 属不同工作项，归 roadmap P2 watch-only，不登记本计划 Deferred。）

## Closure

Status Note: 独立结束审计通过，计划可关闭。审计（新会话冷重播，无执行者上下文）逐项核验：① 实现——`ErpPurErrors.ERR_RECEIVE_QTY_OVER_TOLERANCE`（`erp.err.pur.receive-qty-over-tolerance` + ARG_RECEIVE_CODE/ARG_LINE_NO/ARG_RECEIVED_QTY/ARG_ORDER_QTY/ARG_TOLERANCE，`ErpPurErrors.java:124-126`）；`ErpPurReceiveProcessor.validateBusinessRulesForApprove:172-175` 在 `requireSupplierActive` 后接线 protected step `validateOverReceiptTolerance:185-234`（per-order-line 聚合「当前行 + 同订单其他 APPROVED 行」+ 容差判定 + strict 抛错/非 strict LOG.warn + orderLineId null 跳过 + 边界恰等放行）。② 零 ORM 变更——`git status --short` 仅 erp-pur-service 2 个 Java + 测试文件 + `_cases/` 快照 + docs。③ 测试——`TestErpPurReceiveOverReceiptTolerance` 6 测试方法（strict 拒绝 / 非 strict warn 放行 / 容差内 / 多入库单聚合 / 无订单行跳过 / 边界恰等）+ `_cases/` 6 目录快照（autotest.yaml+input+output）；独立复跑 `mvn test -pl module-purchase/erp-pur-service -o -Dtest=TestErpPurReceiveOverReceiptTolerance` → 6/6 通过；全模块复跑 `mvn test -pl module-purchase/erp-pur-service -o` → 147 tests 零失败（与 Closure Gates 声称一致）；`bash docs/audits/nop-compliance-checker.sh` exit 0（R12c=40 vs 头注基线 38 属历史存量，本计划变更文件 0 处 AcctSchemaResolver import，与本计划无关）。④ 文档回填——`three-way-match.md:73` §数量差异实现注记（RC-R1.11/P1-RC-019 + 配置复用 + strict/非 strict 语义 + P2-RC-014 successor 注记），git diff 证实需求契约段零改动；`arm-index.md:160` P1-RC-019 行含【修复状态：done（RC-R1.11，2026-08-08，plan …）】+ 摘要；`requirement-compliance-roadmap.md:379` RC-R1.11 = done ✅ + 头部（`:3`）最后更新链含 RC-R1.11 done 段；`docs/logs/2026/08-08.md:3` 顶部 RC-R1.11 日志条目。⑤ 计划内部一致性——三阶段全部 completed、执行项/退出标准/门控全 [x]（除本次闭合的两项审计门控）、Draft Review Record 两轮独立审查记录在案。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，冷重播，无执行者上下文；执行者未自我审计）
- Evidence: 本审计会话逐项核验（见 Status Note）：ErpPurErrors.java:124-126 / ErpPurReceiveProcessor.java:172-234 / TestErpPurReceiveOverReceiptTolerance.java 6 @Test / `_cases/` 6 目录 / mvn 独立复跑 6 tests + 147 tests 零失败 / nop-compliance-checker.sh exit 0 / git status 变更面 / three-way-match.md:73 + git diff / arm-index.md:160 / requirement-compliance-roadmap.md:3,379 / docs/logs/2026/08-08.md:3

Follow-up:

- 无阻塞跟进。非阻塞：短收差异处理（UC-PUR-06 ⑮）归 P2-RC-014 roadmap watch-only successor（本计划 Non-Goals 已声明）；R12c AcctSchemaResolver=40 vs 头注基线 38 的历史存量漂移（如确认）建议在下次 compliance 基线集中裁决时归账，非本计划范围。
