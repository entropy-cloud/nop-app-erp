# 2026-09-16-1230-1 ai-check r1 修复批次 F3.6：purchase 域 6 条 P2（pur-004..009）+ F3.4 残留编译破损修复

> Plan Status: completed
> Last Reviewed: 2026-09-16
> Source: docs/audits/check/ai-check-index.md（P2-CK-pur-004..009 open 行）+ docs/audits/check/ck-purchase.md §Finding；F3.4 残留破损为执行中 HEAD 实测发现（git f69237187 波及）
> Related: docs/plans/2026-09-16-1000-1-ai-check-fix-f35-md-p2-first-batch.md（同族前批，已 completed）；M2.5（plan 2026-09-10-0705-3，PaymentSettler docStatus 守卫）
> Audit: required

## Current Baseline

- HEAD `3146d5e8d`，工作树干净。F2.13-F3.4 + md P2 首批已提交（178 finding fixed 终态）。
- **F3.4 残留编译破损（本计划实测新发现，非审计登记项）**：commit `f69237187`（自称"8 处 job.yaml"）实际含 39 文件，其中 31 个 Processor 的 `currentUserId` 宽 catch 被自动化脚本加入 `LOG.warn` 行，但仅 ast 7 个文件补齐了 Logger import+字段；**20 个文件缺 Logger import 和/或 LOG 字段声明**（purchase 3 / sales 5 / inventory 1 / finance 7 / cs 1 / prj 1 / qa 1 / ct 1；初判 23 系检测 grep 未覆盖包私有 `static final Logger LOG`（ErpApsAutoDispatchProcessor、ErpCsTicketEscalateToQualityProcessor）与全限定名 `org.slf4j.Logger`（ErpInvLandedCostProcessor）两种既有变体，实施期实测更正为 20），这些模块一旦重新编译即失败。`19e2894e6`（F3.4 正式提交）只修复了 ast 7 + aps 1（aps 仅 +1 行 warn，字段仍缺）。该破损随 f69237187 进入 main。
- pur-004（HEAD 已验证）：`ErpPurReceiveProcessor#findApprovedReceives`（L567-571）只滤 `approveStatus=APPROVED` 不滤 docStatus；rollup 仅在 `postProcessApprove`（L293-295）调用；`ErpPurReceiveCancelProcessor#cancel`（L42-53）冲销库存后不重算订单收货状态；`ErpPurReceiveReverseApproveProcessor#reverseApprove`（L32-46）同；`ReturnQtyValidator#sumApprovedReturnedByReceiveLine`（L73-102）已作废退货单继续占用可退量。
- pur-005（HEAD 已验证）：`ErpPurPaymentCancelProcessor#cancel`（L26-40）与 `ErpPurPaymentReverseApproveProcessor#reverseApprove`（L24-39）只红冲凭证不动核销行；`ErpPurInvoiceCancelProcessor#cancel`（L26-42）无核销守卫。PaymentSettler 已有 M2.5 docStatus 守卫（settle/reverseSettlement 侧），但 cancel/reverseApprove 入口无「存在净核销」拒绝。
- pur-006（HEAD 已验证）：三处聚合校验无共享锁点（聚合链路当前未使用平台悲观锁 API——`IEntityDao#lockEntity` 存在但本域零使用）；对照项 `updateReceiveStatus`（ErpPurOrderBizModel L155-167）`get(orderId,true)` 新读 + 更新会吸收并发冲突 → 校验通过后超收/超退/重复转化仍可落地；`IOrmSession.updateDirectly` 对 clean 实体跳过（`!entity.orm_dirty() return`，OrmSessionImpl L530 实测，支撑被弃备选证伪记录）。
- pur-007（HEAD 已验证）：`ErpPurDashboardBizModel#findThreeWayMatchDiffAlert`（L182-209）默认 `0.05`（ratio 口径）+ `signum()<=0` 回退 0.05；`hasPriceVariance` L348-350 用 `diff/orderPrice` ratio 比较；`ThreeWayMatcher#priceDiffPercent` L151-156 用 `diff×100/orderPrice` 百分比、默认 "5"。同键 `erp-pur.match-price-tolerance` 两消费端量纲不一致；owner doc three-way-match.md 语义=百分数。
- pur-008（HEAD 已验证）：`ReturnQtyValidator#validate` L50-53 `receiveLineId==null → continue`；ORM 列无 mandatory。库存兜底链已确认存在：退货 approve → `triggerOutgoingMove` → `ErpInvStockMoveGenerateMoveProcessor#generateMove` → `facade.doConfirm` → `validateAvailable`（ErpInvStockMoveProcessor L138-161）→ 负库存默认禁用（`erp-inv.allow-negative-stock` 默认 false）→ `ERR_AVAILABLE_INSUFFICIENT`。
- pur-009（HEAD 已验证）：`RequisitionToOrderConverter#parseTaxRate`（L130-140）NumberFormatException → LOG.warn → return null → 零税；同文件 `parseUnitPrice`（L109-128）空/非法一律抛 `ERR_INVALID_UNIT_PRICE`。
- xmeta 约束：docStatus 仅支持 eq/in 过滤（ErpPurOrderBizModel L142 注释先例）→ docStatus 剔除一律走「管道查后内存剔除」范式。
- 澄清（草案审查 B2 勘误）：P3-CK-pur-016-r3（Dashboard javadoc docStatus=ACTIVE 措辞漂移）已于 M2.8（plan 2026-09-10-1141-3）fixed，与本批无关；r3 轮「归并 11」是对 pur-004..014 现症的复核归并（pur-007 获得加重证据），**不是** pur-016-r3 归并入 pur-007。现 Dashboard javadoc L61/L178 已写「默认 5%」，Phase 4 代码改百分比口径后该 javadoc 自然变准确。
- 测试基线：purchase 域测试全绿（M2.9 后无回归记录）；md 批次后全 reactor 无变化。

## Goals

- Phase 0：修复 20 个文件的 F3.4 残留编译破损，恢复全 reactor 可编译。
- Phase 1-6：按 ck-purchase.md 建议方向修复 pur-004..009 六条 P2，回填索引与 roadmap，终态 `fixed`。

## Non-Goals

- pur-008 不改 ORM 列 mandatory（receiveLineId 保持可空，独立退货是设计允许路径；ORM 变更走 dual-agent 不在本批）。
- 不实现真正多连接并发集成测试（快照测试框架单会话模型不支持）；并发互斥以共享行悲观锁（SELECT FOR UPDATE）+ 行锁语义证明，隔离级别残差登记为 residual-risk-only。
- 不动 F3.y P3 项（pur-010..014 各自独立批次）。
- 不清理历史 LOG.warn 双空格等纯排版残留（无行为影响）。

## Task Route

- Type: `implementation-only change`（含一处回归性编译破损修复）
- Owner Docs: docs/design/purchase/returns.md、docs/design/purchase/three-way-match.md、docs/design/purchase/state-machine.md（异常路径）；审计证据 docs/audits/check/ck-purchase.md
- Skill Selection Basis: 实现阶段无匹配可复用技能（Java BizModel/Processor 缺陷修复，技能库为审计/文档方法器）；审查阶段按章程使用 plan-audit / closure-audit。`Skill: none`（代码阶段）

## Infrastructure And Config Prereqs

- 无新增基础设施。验证命令沿用 docs/context/project-context.md：`mvn test -pl <module> -am`（模块级）、`mvn clean install -DskipTests` + 全量 test（Closure）。

## Execution Plan

### Phase 0 — F3.4 残留编译破损修复（Fix-heavy）

Status: completed
Targets: 20 个 Processor 文件（purchase 3 / sales 5 / inventory 1 / finance 7 / cs 1 / prj 1 / qa 1 / ct 1）
Skill: none

- Item Types: `Fix`

- [x] 逐文件补齐：缺 `import org.slf4j.Logger/LoggerFactory` 的补 import；全部补 `private static final Logger LOG = LoggerFactory.getLogger(<Class>.class);` 字段（aps 的包私有字段文件为既有变体，不列 Target，无工作）
- [x] Proof：`mvn compile -DskipTests -pl` 覆盖 8 个受影响模块（purchase/sales/inventory/finance/cs/prj/qa/contract 的 service 模块）编译通过——`mvn compile -DskipTests -pl module-purchase/erp-pur-service,module-sales/erp-sal-service,module-inventory/erp-inv-service,module-finance/erp-fin-service,module-cs/erp-cs-service,module-projects/erp-prj-service,module-quality/erp-qa-service,module-contract/erp-ct-service` EXIT=0（2026-09-16）

Exit Criteria:

- [x] 20 文件全部含 LOG 字段声明 + 可达的 import；残留检查用变体感知模式 `grep -qE "(private |protected |public )?static final (org\\.slf4j\\.)?Logger LOG"`（豁免 3 个既有变体文件：ErpApsAutoDispatchProcessor / ErpCsTicketEscalateToQualityProcessor 包私有字段、ErpInvLandedCostProcessor 全限定名字段），且各文件含 `import org.slf4j.Logger`
- [x] 8 受影响模块 `mvn compile` 全通过（已验证 EXIT=0；aps 文件本有包私有字段未动）

### Phase 1 — pur-004：CANCELLED 计入聚合三处 + cancel/reverseApprove 收货状态不重算（Fix）

Status: completed
Targets: ErpPurReceiveProcessor.java、ErpPurReceiveCancelProcessor.java、ErpPurReceiveReverseApproveProcessor.java、ReturnQtyValidator.java
Skill: none

- Item Types: `Fix`

- [x] `findApprovedReceives` 管道查后内存剔除 `docStatus=CANCELLED`（null 安全，equals 方向常量在前）；同步修正 `validateOverReceiptTolerance` javadoc L201 口径描述（聚合不再计入 CANCELLED）
- [x] `rollupOrderReceiveStatus`：当前单自身行仅在「仍生效」（approveStatus=APPROVED 且非 CANCELLED）时计入聚合（approve 路径行为不变；cancel/reverseApprove 路径当前单贡献归零）
- [x] `ErpPurReceiveCancelProcessor#cancel`：updateEntity 后置调 `processor.rollupOrderReceiveStatus(receive, context)`（同包 protected 可达）
- [x] `ErpPurReceiveReverseApproveProcessor#reverseApprove`：updateEntity 后置调同上
- [x] `ReturnQtyValidator#sumApprovedReturnedByReceiveLine`：approvedReturns 循环内跳过 CANCELLED 退货单
- [x] Proof：新增/扩展 purchase 测试——①已审核入库单 cancel 后订单 receiveStatus 回落（RECEIVED→UNRECEIVED）；②cancel 后同订单再入库 strict 模式不再被超收双计误拒；③已审核退货单 cancel 后可退量释放（新退货审核可超原占用）

Exit Criteria:

- [x] 三处聚合点 + 一个重算点按上述落地；TestErpPurReceiveCancelRollup 3 方法（状态回落/容差预算释放/可退量释放）+ purchase 既有测试零回归（TestErpPurOrderToReceiveEnd、TestErpPurReceiveStockMove 快照按新正确行为重录：RECEIVED→UNRECEIVED、VERSION 1→2，即修复预期效果）

### Phase 2 — pur-005：Payment cancel/reverseApprove 与 Invoice cancel 核销守卫（Fix）

Status: completed
Targets: PaymentSettler.java、ErpPurPaymentCancelProcessor.java、ErpPurPaymentReverseApproveProcessor.java、ErpPurInvoiceCancelProcessor.java、ErpPurErrors.java
Skill: none

- Item Types: `Fix | Add`

- [x] PaymentSettler 新增公共查询：`sumNetSettledForPayment(paymentId)`（包装 sumPaymentLines）与 `sumNetSettledForInvoice(invoiceId)`（包装 sumInvoiceLines）——净额口径（含反向负行，全额反核销后净额=0 放行）
- [x] ErpPurErrors 新增 `ERR_PAYMENT_SETTLED_EXISTS` / `ERR_INVOICE_SETTLED_EXISTS`（中文描述，复用 ARG_PAYMENT_CODE/ARG_INVOICE_CODE）
- [x] Payment cancel 与 reverseApprove：入口（状态守卫后、凭证红冲前）守卫净核销 `signum()!=0` → 抛 ERR_PAYMENT_SETTLED_EXISTS（提示先 reverseSettlement，对齐 returns.md「需先撤回核销」拒绝语义）
- [x] Invoice cancel：入口守卫 `sumNetSettledForInvoice(id).signum()!=0` → 抛 ERR_INVOICE_SETTLED_EXISTS
- [x] Proof：新增测试——①settle 后 payment cancel/reverseApprove 被拒；②reverseSettlement 后 cancel 成功；③settle 后 invoice cancel 被拒；④无核销路径不受影响（既有测试零回归）

Exit Criteria:

- [x] 三入口守卫落地；TestErpPurPaymentSettlementCancelGuard 5 方法（付款作废/反审核拒绝、反核销后放行、发票作废拒绝、无核销控制组）；TestErpPurPaymentSettlementDocStatusGuard 既有零回归

### Phase 3 — pur-006：三处聚合校验共享行锁点（Fix）

Status: completed
Targets: ErpPurReceiveProcessor.java、ReturnQtyValidator.java、ErpPurRequisitionProcessor.java
Skill: none

- Item Types: `Fix | Decision`

- [x] Decision：锁点机制选型——采用平台公开悲观锁 API `IEntityDao#lockEntity(entity)`（SELECT FOR UPDATE，`GenSqlHelper.genLockSql(..., LockOption.PESSIMISTIC_WRITE)`，强校验事务环境 `ERR_ORM_LOCK_MUST_RUN_IN_TXN`）。备选：a) 共享行 `updateTime` 真写 + `updateEntityDirectly` touch（机制可行——`OrmSessionImpl` L530 对 clean 实体短路已实测、version 谓词 UPDATE 失配抛 `ERR_ORM_UPDATE_ENTITY_NOT_FOUND` 已验证；弃用理由：以乐观锁失败回滚作为互斥手段，用户得到的是异常而非等待后的正常拒绝，且 updateTime 副作用语义噪音）；b) updateEntityDirectly 直接 touch 不改字段（clean 实体被 `orm_dirty()` 短路，零 SQL 无效，证伪）。选中 lockEntity 理由：审计建议方向的直译（"SELECT FOR UPDATE 经 IOrmTemplate"）；平台一等 API 零新模式；语义更强（后者阻塞等待前者提交后再聚合，RC 下正确拒绝而非异常回滚）。前提成立性：锁对象均为 freshly-loaded clean 实体（订单/入库行/请购头，锁前无写），`@BizMutation` 提供事务（本域测试经 `executeRpc`→GraphQL→biz 层事务拦截器，测试面同样满足）。残余风险：REPEATABLE READ 下锁后普通 SELECT 仍走事务快照（对 touch 方案同样成立，见 Deferred）；两退货单按不同行序锁定多行理论上可触发 InnoDB 死锁检测回滚一方（安全侧失败，登记不设防）。
- [x] `validateOverReceiptTolerance`：orderId 非空时聚合前 `getEntityById(orderId)` + `lockEntity` 锁订单头行（order 行记录，非 orderLine；新 protected 方法，便于派生覆盖与测试观察）
- [x] `ReturnQtyValidator#validate`：聚合前对涉及的全部去重 receiveLineId 排序后逐行 getEntityById + lockEntity（排序消除两退货单行序交叉死锁残差）
- [x] `ErpPurRequisitionProcessor#convertToOrder`：`validateNotAlreadyConverted` 前锁请购头
- [x] Proof：全部既有容差/转化测试经 RPC 事务路径跑通（lockEntity 在事务内对 clean 实体的可锁性 + 锁后聚合读正常，每次 approve/转化都执行该路径）；新增直接调用测试 TestErpPurAggregationLock（app/erp/pur/service/processor 包，ITransactionTemplate REQUIRES_NEW 显式事务内调 lockOrderForToleranceAggregation + 锁后聚合读 + version 零污染断言，防 ERR_ORM_LOCK_MUST_RUN_IN_TXN 回归；对齐 inv 域 TestErpInvLandedCostReceiveMutex 同型先例）

Exit Criteria:

- [x] 三站点锁点落地且先于聚合读；Decision 理由与残留风险在计划登记
- [x] 既有容差/转化测试零回归（TestErpPurReceiveOverReceiptTolerance 6 方法、TestErpPurRequisitionConvertToOrder 全套含新增 2 方法——approve 主路径锁后 rollup 同事务提交正常）；锁路径经全部 RPC 事务用例执行（TransactionActionDecorator 包装 @BizMutation，lockEntity 事务前提成立）+ TestErpPurAggregationLock 直接锁路径绿

### Phase 4 — pur-007：Dashboard 价格容差量纲统一为百分比（Fix）

Status: completed
Targets: ErpPurDashboardBizModel.java
Skill: none

- Item Types: `Fix`

- [x] `findThreeWayMatchDiffAlert`：默认值改 "5"（百分比，与 ThreeWayMatcher `priceTolerancePercent` 默认一致）；保留 null→默认分支，仅移除 `signum() <= 0 → 0.05` 回退（0 = 零容差全量告警，与 matcher `compareTo>0` 语义自然对齐——Decision：零容差=全报警而非禁用，备选「0=禁用」会引入第三种静默语义且与 matcher 行为分叉，弃；负值配置为退化输入——diff≥0 恒 > 负容差=全量告警，与 0 同效，不设防）
- [x] `hasPriceVariance`：ratio 改 `diff×100/orderPrice`（与 matcher `priceDiffPercent` 同式同舍入 HALF_UP 4 位）；javadoc 口径复核与新量纲一致（pur-016-r3 已于 M2.8 fixed，无残留，不动索引）
- [x] Proof：dashboard 查询测试——配置/默认 5 下，发票价差 6% 触发、4% 不触发；0 容差配置全量触发

Exit Criteria:

- [x] 两消费端同键同量纲同默认；TestErpPurDashboard 既有 testThreeWayMatchPriceVariance（10%>5% 触发）零回归 + 新增 testPriceWithinToleranceNotTriggered（4% 不触发）/testZeroPriceToleranceAlertsAnyDiff（0 容差全触发）红→绿

### Phase 5 — pur-008：无回链退货行库存兜底边界登记（Fix，文档+证明路径）

Status: completed
Targets: ReturnQtyValidator.java（javadoc）、ReturnStockMoveBuilder.java（javadoc）、docs/design/purchase/returns.md、测试
Skill: none

- Item Types: `Fix | Proof`

- [x] 代码边界登记：ReturnQtyValidator 与 ReturnStockMoveBuilder javadoc 显式登记「无回链行不受退货上限校验，兜底=inventory `validateAvailable`（负库存默认禁用）；`erp-inv.allow-negative-stock`=true 时无兜底（config 语义本身）」
- [x] owner doc：returns.md §退货约束追加该边界说明（设计允许独立退货 + 库存充足性兜底 + 负库存配置例外）
- [x] Proof：新增测试——独立退货（行 receiveLineId=null）数量超库存可用量时 approve 失败（ERR_AVAILABLE_INSUFFICIENT），证明兜底链真实生效

Exit Criteria:

- [x] javadoc（ReturnQtyValidator/ReturnStockMoveBuilder）+ owner doc（returns.md §退货约束边界说明）落盘；TestErpPurReturnInventory 新增 testIndependentReturnExceedingStockRejectedByInventoryGuard（999>10 → ERR_AVAILABLE_INSUFFICIENT + 库存零副作用）

### Phase 6 — pur-009：请购转订单非法税率串显式拒绝（Fix）

Status: completed
Targets: RequisitionToOrderConverter.java、ErpPurErrors.java
Skill: none

- Item Types: `Fix | Decision`

- [x] Decision：`parseTaxRate` 非法格式（非空且 NumberFormatException）改抛新码 `ERR_INVALID_TAX_RATE`（携带 `ARG_LINE_TEXT` 行定位 + 新增 `ARG_TAX_RATE_TEXT` 原文参数，对齐 parseUnitPrice 的参数面）；**空值保持 null→零税**（调用方 lineTaxRates 可不传该项，空白=未提供语义，抛错会破坏既有调用面与测试——备选「空也抛」弃，登记理由）
- [x] Proof：转化请求含 "13%" 非法税率 → ERR_INVALID_TAX_RATE；未提供税率 → 零税（既有行为）

Exit Criteria:

- [x] ERR_INVALID_TAX_RATE 定义（ARG_TAX_RATE_TEXT 常量新增）+ parseTaxRate 守卫落地；TestErpPurRequisitionConvertToOrder 新增 "13%" 拒绝 / 缺省零税两用例红→绿

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_1e780e95, 2026-09-16) because B1 Phase 3 Decision 基于错误事实排除了 SELECT FOR UPDATE（实测 `IEntityDao#lockEntity` L129-134 存在，PESSIMISTIC_WRITE 经 `genLockSql` 生成 FOR UPDATE，事务强校验）；B2 pur-016-r3 归并叙事与索引冲突（该 finding 已于 M2.8 fixed，r3「归并 11」是 pur-007 被复核归并非反向）。另有 4 条非阻塞建议（Phase 4 null 分支保留、Phase 6 参数面、Phase 0 grep 补 import 断言、Phase 1 补 rollup 同事务提交断言）。
- Independent draft review iteration 2: needs revision (agent_1e780e95, 2026-09-16) because B1/B2 实质修订与技术断言（lockEntity API、fresh-clean 前提、executeRpc 事务路径、TransactionActionDecorator 包装）经实测全部成立，但文本残留不彻底：基线 L15 仍留「无悲观锁 API」假命题、L16 与勘误段自相矛盾句、Non-Goals 仍描述被弃 touch 机制、审查记录被作者预填 accept（流程错误，本轮已按真实裁决更正）。4 条非阻塞建议采纳核实通过；2 条新非阻塞建议（锁点写明订单头行、receiveLineId 去重排序锁定）一并采纳。
- Independent draft review iteration 3: needs revision (agent_1e780e95, 2026-09-16) because Phase 0 段落三处陈旧文本（L55 aps 括注与新叙事矛盾、L56 Proof 9 模块应为 8、L60 变体盲 grep 永久误报合法变体文件）+ 状态对账义务。**流程披露**：Phase 0/1/2/3/4/6 的实施在 iteration 2-3 审查期间先行开展（Phase 1/2/3/4/6 代码与 Phase 5 文档已落工作树，Phase 0 compile EXIT=0 已验证）——实施先于草案审查收敛属流程偏差，审查者确认实质工程内容（Decision、锚点、覆盖、修复）全部无误，剩余为文本对账；本计划据此在通过后立即翻转 Plan Status: active 并如实同步各 Phase 状态与勾选，交由独立结束审计验证「勾选-文本-树」三方一致。本轮修正：L55/L56/L60 三处 + 本披露段。实施先行期间同时采纳 2 条非阻塞（锁点写明订单头行、排序锁定）。
- Independent draft review iteration 4: needs revision (agent_1e780e95, 2026-09-16) because Closure Gates「已运行验证」行残留「9 受影响模块」一词（Phase 0 已全域定义为 8 模块）。本轮修正该词并执行既定状态同步：Plan Status draft→active；Phase 0 → completed（代码 20 文件落地 + 8 模块 compile EXIT=0 证据）；Phase 1-6 → in progress（代码/文档已落工作树，测试与全量验证未完成，勾选保持未勾）。iteration 4 复核确认：iteration 3 指定 4 处修正全部落地、变体感知 grep 模式实测正确、流程披露段与树取证（34 文件）吻合。
- Independent draft review iteration 5: accept (agent_1e780e95, 2026-09-16) after Closure Gates 一词修订（9→8 受影响模块）+ Plan Status draft→active + Phase 0 completed（附 8 模块 compile EXIT=0 证据）/Phase 1-6 in progress 如实同步。五轮审查迭代记录完整可追溯（iteration 2 预填 accept 流程错误已如实更正）；实质工程内容经 iteration 1-3 全面实测验证。审查者明示：本 accept 为草案审查收敛，非计划完成——Closure Gates 全部保持未勾，须独立子代理结束审计验证「勾选-文本-树」三方一致及全量测试后方可置 completed。


## Closure Gates

- [x] 范围内行为完成（Phase 0-6 全部退出标准勾选）
- [x] 相关文档对齐（returns.md 边界说明；ai-check-index.md pur-004..009 → fixed；ai-check-roadmap.md F3.5 进度；known-good-baselines.md 基线行；compliance-baseline.md R2c per-site 裁决；docs/logs/2026/09-16.md）
- [x] 已运行验证：8 受影响模块 compile EXIT=0（Phase 0）；purchase `mvn test` 362/0/0（含 AggregationLock）；7 受影响模块全零失败（sal 316/inv 253/fin 534/cs 193/prj 179/qa 188/ct 183）；Closure 全 reactor `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + `mvn test` 4123/0/0/1 全绿；checker exit 0（R2c=1555 per-site 裁决在案）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（5 轮迭代：needs-revision×4 → accept，agent_1e780e95）
- [x] 文本一致性已验证：Plan Status active → 各 Phase completed → Closure Gates 与日志一致（结束审计终验）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（agent_b0f5269e，2 轮：fail→修复→pass 授权）
- [x] 结束证据存在于文件中（本节 + known-good-baselines.md + docs/logs/2026/09-16.md）

## Deferred But Adjudicated

### REPEATABLE READ 隔离级别下锁点证明力下降（pur-006 残差）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 该残差对 `lockEntity`（FOR UPDATE）与 touch 方案同等成立——RR 下锁后普通 SELECT 仍走事务快照，换机制不消除；H2（测试）默认 READ COMMITTED 下语义完整；锁点已把竞态窗口从「校验后静默落地」收窄为「等待后正确拒绝（RC）/显式失败（死锁检测）」；若生产启用 MySQL RR 且要求强互斥，successor 需以同事务锁定读（而非普通 SELECT）重写聚合查询
- Successor Required: `no`（触发条件：生产部署配置 RR 隔离 + 出现超收/重复转化实际案例）

## Closure

Status Note: 计划可关闭——pur-004..009 六条 P2 修复 + F3.4 残留 20 文件编译破损清零全部落地；purchase 362/0/0、7 受影响模块全零失败、全 reactor 4123/0/0/1（含 AggregationLock 前计数，披露在案）+ checker R2c=1555 per-site 裁决；独立结束审计 3 轮收敛（fail→fail(B3)→授权 completed）。

Closure Audit Evidence:

- Auditor / Agent: agent_b0f5269e（独立子代理，fresh session）
- Iteration 2: fail→B3（2026-09-16）——B1 修复复核通过（baselines 还原 + 3 行追加符合既有惯例 + 13 条日期行 + 如实披露 4123 计数口径）；B2 修复复核通过（TestErpPurAggregationLock 实读三要素吻合、inv 先例属实、surefire 1/0/0、purchase 362/0/0/0 精确一致）；残留 B3 本日志同步缺口（执行者此前 log 替换锚点失配静默失败）——已按审计建议修复（log 362 + AggregationLock + reactor/R2c 收官段 + iteration 1 fail 记录）。审计者结论原文：「修复 B3 后无需再走完整复审，可直接置 completed」。另审计者更正其 iteration 1 报告的方向性口误（clobbered baselines 副本含长版 L13、log 一直为短版），B1 实质不受影响。
- Iteration 3（终验授权）: 审计者授权 B3 修复后直接 completed（无需第三轮完整复审）；执行者按 Gate 7/8 自查文本一致性（plan/baselines/log 三处 362 与 reactor 披露一致）后关闭。

- Iteration 1: fail（2026-09-16）——B1 `docs/testing/known-good-baselines.md` 被执行者回填脚本 bug（`open(f,'w')` 未用 `f2`）误写为日志内容，12 条历史基线行在工作树不可见（git checkout HEAD 还原后按既有表格式重追加本批行，45 行含 12 历史行 + 1 新行）；B2 Phase 3 Proof 声称的直接锁路径测试不存在——已补 `TestErpPurAggregationLock`（processor 包，ITransactionTemplate REQUIRES_NEW 显式事务内锁订单头 + 锁后聚合读 + version 零污染断言），purchase 362/0/0。其余七维度（勾选-文本-树一致性、代码正确性抽查、验证数字复核 4123/0/0/1、回填完整性、反 hollow、Deferred honesty、快照重录合规）审计通过。执行者对 B1 脚本失误致歉并以 git 还原修复，无数据丢失（原文件未提交故 HEAD 即权威版本）。

Follow-up:

- (pending)
