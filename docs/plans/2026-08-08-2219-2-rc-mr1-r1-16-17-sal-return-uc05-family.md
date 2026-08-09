# 2026-08-08-2219-2-rc-mr1-r1-16-17-sal-return-uc05-family RC-R1.16 + RC-R1.17 — sales 退货未开票冲减族（未交货量回填 + 暂估应收条件冲减，MR1 第一批纯预授权）

> Plan Status: active
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.16（P1-RC-023 sales 退货未交货量回填，方案 A 预授权）+ RC-R1.17（P1-RC-024 sales 未开票退货暂估应收条件冲减）— 同 UC（UC-SAL-05）同域同 owner doc（`returns.md`）同结果表面（退货审核后置行为），按计划指南规则 14 合并为一个 owner plan 的两个阶段
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.16/RC-R1.17 行 + `docs/audits/arm-index.md` P1-RC-023/P1-RC-024 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`
> Related: `docs/design/sales/use-cases.md`（L1 UC-SAL-05）；`docs/design/sales/returns.md`（§未交货量更新/§红字发票处理）；`docs/audits/2026-08-03-0630-rc-ma1-a1-20-sales-f3-returns-family.md`（A1.20 静态判定）；`docs/plans/2026-08-08-2219-1-rc-mr1-r1-14-15-sal-pricing-family.md`（同批范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-023（arm-index 行，UC-SAL-05 未交货量更新缺失）**：L1（`use-cases.md:142`）逐字「订单未交货量回填: 未交货量 = 订单数量 - 已出库 + 退货」；L2（`returns.md §未交货量更新:322-338`）设计参考「退货后更新原销售订单的未交货量」。L3 实仓：grep `undeliveredQty\|未交货量` 全生产代码 0 命中；`ErpSalReturnProcessor.doApprove:193-207`（triggerIncomingMove → triggerPosting → orchestrateRefund → 状态推进）不调任何订单量更新；`ErpSalOrderLine` 无 undeliveredQuantity 列（仅 `deliveredQuantity` propId 14 + `invoicedQuantity` propId 15，且 deliveredQuantity 零 writer——A4.2.54 证实，P2-RC-019 同源）。§2 P1①（功能完全缺失）。**非 P0**（不破坏活跃数据——未交货量是订单头 deliveryStatus 派生的辅助口径，`rollupOrderDeliveryStatus` 头级状态仍正确）。
- **finding P1-RC-024（arm-index 行，UC-SAL-05 暂估应收条件冲减缺失）**：L1（`use-cases.md:140`）逐字「冲减暂估应收(若出库时已暂估应收)」+ 条件语义「未开票时退货 → 冲减暂估应收，不生成红字发票」。L3 实仓：`SalReturnPostingDispatcher.buildEvent:84-103` 无条件分支——无论是否已开票/是否暂估应收统一发 SALES_RETURN posting + credit memo（负向 ArApItem），无「若出库时已暂估应收」条件判定。§2 P1①（L1 条件分支未实现）。
- **实仓（HEAD 核查）**：
  - `ErpSalReturnProcessor.doApprove:193-207`——**未交货量回填接线点**（方案 A：doApprove 末尾调 `updateUndeliveredQuantity`）。
  - `ErpSalReturnLine` ORM：`deliveryLineId`（propId 3）→ `ErpSalDeliveryLine.orderLineId`（`app-erp-sales.orm.xml:549` propId 3）→ `ErpSalOrderLine`——回填链载体**已就绪（零 ORM 变更）**。`ErpSalOrderLine.quantity`（propId 7）+ `deliveredQuantity`（propId 14）。
  - `SalReturnPostingDispatcher.buildEvent:84-103`——**暂估应收条件冲减修复点**：billData 当前恒 `KEY_TOTAL_COST`（Σ qty×unitPrice）+ `KEY_TOTAL_AMOUNT_WITH_TAX` + `CUSTOMER_ID`；下游 `SalAcctDocProvider.createFacts`（借 1401/贷 6401 反向 SALES_OUTPUT 口径）+ `ErpFinArApItemGenerator:161-164`（负向 ArApItem credit memo）按 `ErpFinBusinessType.SALES_RETURN` 消费——**条件分支判定载体为跨域只读查询**（原出库单是否已开票/是否已暂估应收，经既有 IBiz 链路）。
  - 暂估应收判定信息源（HEAD 核查）：**实仓不存在「暂估应收」凭证/标记**——`InvAcctDocProvider.createFacts` SALES_OUTPUT = 借 6401 销售成本 / 贷 1401 存货（`SalAcctDocProvider.java:83-87` 反向，仅成本侧，无 AR/暂估应收行）；repo 级 grep `暂估应收|ESTIMATED_RECEIVABLE|estimatedReceivable` 零命中——暂估应收仅是 L1 设计概念，无独立凭证载体。因此条件判定载体 = **运营代理（operational proxy）**：出库单 `ErpSalDelivery.posted=true`（`ErpSalDeliveryProcessor:248` 维护，SALES_OUTPUT 凭证存在）⇒ 视为暂估应收未清，credit memo 实现 L1 冲减——**Decision 项**（见 Phase 1，须显式记录代理语义与残留风险）。关联发票判定：`ErpSalInvoiceLine.deliveryLineId`（orm.xml:689）经 delivery 关联存在即已开票——**Decision 项**。
  - **测试基线（P0-1 关键发现）**：既有 `TestErpSalReturn*` 全部 seed `delivery.setPosted(false)`（`TestErpSalReturnPosting.java:331` / `TestErpSalReturnApproval.java:371,389` / `TestErpSalReturnRefund.java:300` / `TestErpSalReturnRefundEndToEnd.java:529,547` / `TestErpSalReturnInventory.java:321` / `TestErpSalReturnTrace.java:313` / `TestErpSalReturnQty.java:269`）——若按「未暂估（delivery 未过账）→ 不生成冲减」门控，这些种子归类未暂估 → SALES_RETURN 凭证 + 负向 ArApItem 被抑制 → `TestErpSalReturnPosting:97-116`（凭证 1401/6401=20 + ArApItem openAmount=-24 + receivableBalance=-24）等既有断言**必失败**。**零回归不是免费的**：本计划必须拥有种子迁移（既有测试 seed 改 `posted=true` + 补 SALES_OUTPUT 凭证种子使断言值不变），或调整门控使既有种子映射到已暂估——见 Phase 2 执行项。
- **预授权判据**（第一批纯预授权）：纯 BizModel/Processor/PostingDispatcher 代码逻辑修复（buildEvent 为**派发器事件组装**，非 VoucherFact/PostingProcessor 过账核心路径——账务引擎本身不动，仅控制是否/如何构造 SALES_RETURN 事件与 billData），**不触 ORM 结构/删除**；**无 ask-first checkbox**（P1-RC-023 方案 B[触 ORM 加列] 明确排除，按方案 A 执行）。roadmap RC-R1.16/RC-R1.17 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-sales/erp-sal-service/.../processor/ErpSalReturnProcessor.java`；`.../posting/SalReturnPostingDispatcher.java`；`.../entity/ErpSalReturnBizModel.java`（仅当 Decision 选 B 派生查询时变更）；`ErpSalConstants.java`（仅当新增条件标记常量时变更）；测试类 1 个新增 + 既有 `TestErpSalReturn*` 种子迁移 + `_cases/` 快照。

## Goals

- **未交货量回填（P1-RC-023，方案 A 纯预授权）**：`ErpSalReturnProcessor.doApprove` 末尾调 protected step `updateUndeliveredQuantity(returnOrder, context)`：per 退货行按 `deliveryLineId → orderLineId` 链定位原订单行，按 L1 公式 `未交货量 = 订单数量 - 已出库 + 退货` 计算并回填——**回填载体 Decision 项**：选项 A = 以「已出库数量」派生口径维护（补写 `ErpSalOrderLine.deliveredQuantity` 使 未交货量 = `quantity − deliveredQuantity + Σ退货量` 可派生，与 P2-RC-019 同源联动）/ 选项 B = 经派生查询（@BizQuery 读侧计算，零写）——**两选项均零 ORM 变更**；方案 B[ORM 加 undeliveredQuantity 列] 属越界排除。
- **暂估应收条件冲减（P1-RC-024）**：`SalReturnPostingDispatcher.buildEvent` 增条件分支——按**运营代理**判定（`ErpSalDelivery.posted=true` ⇒ 已暂估应收未清；关联发票存在 ⇒ 已开票，`ErpSalInvoiceLine.deliveryLineId` 载体）：未开票且已暂估 → 维持 SALES_RETURN 冲减暂估应收路径（现有 credit memo 语义即 L1「冲减暂估应收」）；已开票 → 现有红字发票替代路径（credit memo 等价，P2-MA2-011 接受）保持；两者差异在 billData 标记/条件传递（**Decision 项**）。**预授权边界钉死（P1-2）**：未暂估 → `tryPost` 跳过事件构造（零下游改动——`SalAcctDocProvider`/`ErpFinArApItemGenerator` 零变更）；若执行中证明必须改下游消费方 → **暂停 + ask-first**，不静默进入 finance 过账路径。
- **零回归（P0-1，须拥有种子迁移）**：既有 `TestErpSalReturn*` 全绿——**在既有 seed `delivery.posted=false` 场景下断言值不变**：门控语义按「未暂估 → 跳过事件」会破坏 `TestErpSalReturnPosting` 等既有断言（见 Current Baseline 测试基线段），因此 Phase 2 必须包含**显式种子迁移项**（既有测试 seed 改 posted=true + 补 SALES_OUTPUT 凭证种子）或门控裁决使既有种子映射已暂估——两选一，零回归以断言值不变为准而非「不加种子」；全仓 `mvn test` 不引入新失败。
- **owner doc 收敛注记**：`returns.md §未交货量更新` 补实现注记（方案 A 载体选择）；`use-cases.md` 需求契约段不动（真相源冻结条款）。
- **测试矩阵**：P1-RC-023（doApprove 后未交货量按公式回填 / 未设 deliveryLineId 行跳过 / 多行聚合 / 幂等重复 / reverseApprove 对称回退）+ P1-RC-024（未开票已暂估 → 冲减 / 已开票 → 红字路径 / 未暂估（posted=false）→ 跳过事件构造 / posted=false 边界变体）——**不含「无 sourceDelivery」路径**（`requireSourceDeliveryApproved` 对 null delivery 直接抛错，审核前不可达）。
- 回填 arm-index P1-RC-023/P1-RC-024 → `done (RC-R1.16/RC-R1.17)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**（零列/零索引——P1-RC-023 方案 B[加 undeliveredQuantity 列] 显式排除；P1-RC-024 零字段需求）。
- **不触会计过账核心路径**（VoucherFact/PostingProcessor/凭证生成引擎零改动——buildEvent 仅做事件组装层条件分支）。
- **不实现独立红字发票实体**（P2-MA2-011 credit-memo 替代已接受，不重开）。
- **不做换货（P1-RC-025）**（独立 finding，ORM ask-first，另行列管）。
- **不改真相源契约段落**（use-cases L1 不动）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/sales/use-cases.md`（L1 UC-SAL-05）+ `docs/design/sales/returns.md`（§未交货量更新/§红字发票处理）+ `docs/design/finance/posting.md`（SALES_RETURN 事件口径）+ `docs/audits/2026-08-03-0630-rc-ma1-a1-20-sales-f3-returns-family.md`（A1.20 静态判定）
- Skill Selection Basis: 实现面 = Processor protected step + PostingDispatcher 条件分支 + 跨域只读 IBiz（`nop-backend-dev`：跨实体访问规则、protected step 模式、过账事件组装边界）；测试（`nop-testing`：JunitAutoTestCase 断言 + 快照录制）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 零新 config（未交货量回填为强制后置行为——L1 无 config 门控语义；暂估应收判定为条件分支，无 config 需求）。若执行中裁决需门控（如回填开关），须在 Decision 中记录理由。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-sales/erp-sal-service`。

## Execution Plan

### Phase 1 - 退货后置行为实现（P1-RC-023 + P1-RC-024）

Status: planned
Targets: `ErpSalReturnProcessor.java`；`SalReturnPostingDispatcher.java`；`ErpSalConstants.java`；`ErpSalReturnBizModel.java`（后两者仅当对应 Phase 1 Decision 分支需要时变更——见下方 Fix 项条件）
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [ ] `Decision` **未交货量回填载体**：选项 A（推荐）= 写路径——doApprove 末尾调 `updateUndeliveredQuantity`：按退货行 `deliveryLineId → ErpSalDeliveryLine.orderLineId` 定位订单行，聚合 Σ退货量，补写 `ErpSalOrderLine.deliveredQuantity`（使 L1 公式可派生成立：未交货量 = quantity − deliveredQuantity + Σ退货量）——与 P2-RC-019（deliveredQuantity 零 writer）同源联动，一处写入口；**写语义钉死（P2-1）**：`deliveredQuantity` 写「毛口径」（Σ APPROVED delivery-line qty by orderLineId，approveStatus 过滤对齐 `ReturnQtyValidator:72-101` 先例）——净口径（扣退货）会与公式中 `+ Σ退货量` 项重复计算，弃；退货回填为增量（幂等按重新聚合重算，对齐矩阵④）。选项 B = 读路径派生——@BizQuery 计算未交货量零写（L1「回填」字面偏写语义，且 deliveredQuantity 仍零 writer，弃）。**两选项均零 ORM 变更**；方案 B[ORM 加列] 显式排除（roadmap 标注）。记录残留风险（deliveredQuantity 既有零 writer 历史数据）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **暂估应收条件判定载体**：选项 A（推荐）= **运营代理**——「已暂估」= `ErpSalDelivery.posted == true`（SALES_OUTPUT 凭证存在；实仓无独立暂估应收凭证，`InvAcctDocProvider` SALES_OUTPUT 仅成本侧 6401/1401，见 Current Baseline）⇒ 视为暂估应收未清，credit memo 实现 L1 冲减；「已开票」= 经 `ErpSalInvoiceLine.deliveryLineId`（orm.xml:689）关联发票存在且已审核——优先判定；选项 B = 事件结构按状态分叉为两种 businessType——审批面更大，超出最小修复面，弃。**预授权边界（P1-2）**：未暂估 → `tryPost` 返回 false/跳过事件构造（`SalAcctDocProvider`/`ErpFinArApItemGenerator` 零变更）；仅当执行中证明必须改下游消费方时暂停 + ask-first，不静默越界。记录代理语义残留风险（delivery.posted 与真实暂估应收状态存在运营近似偏差）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `ErpSalReturnProcessor` 新增 protected step `updateUndeliveredQuantity(ErpSalReturn returnOrder, IServiceContext context)`：doApprove 末尾（`applyPosted` 之前/之后——**Decision 子项**，倾向状态推进后、updateEntity 前）调用；per 行定位订单行 → 按 L1 公式聚合回填（载体按 Phase 1 Decision）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `SalReturnPostingDispatcher.buildEvent`：按 Phase 1 Decision 增条件分支——组装 billData 条件标记（如 `KEY_OFFSET_ESTIMATED_RECEIVABLE` 布尔）与/或判定路径；**不改 VoucherFact/PostingProcessor**（下游消费方 `SalAcctDocProvider`/`ErpFinArApItemGenerator` **零变更**——预授权边界 P1-2：未暂估 → `tryPost` 跳过事件构造；仅当执行证明必须改下游时暂停 + ask-first）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `doApprove` 含未交货量回填调用链且按 L1 公式回填（Phase 2 断言证实）；零 ORM 变更
- [ ] `buildEvent` 含暂估应收条件分支且不触过账核心路径（Phase 2 断言证实）
- [ ] `git diff --stat` 仅 erp-sal-service Java + `_cases/` 快照

### Phase 2 - 测试矩阵

Status: planned
Targets: `module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnCompliance.java`（新增，覆盖 P1-RC-023 + P1-RC-024）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Add` P1-RC-023 矩阵：① doApprove 后订单行未交货量 = quantity − deliveredQuantity + Σ退货量（L1 公式数值断言）；② 退货行无 deliveryLineId → 跳过不报错；③ 多行同订单聚合正确；④ 重复退货二次回填不重复累加（幂等语义——按重新聚合重算，非增量累加）；⑤ 反审核（reverseApprove）后回填行为（**Decision 子项**：是否回退——倾向对称回退，对齐既有 reverse 链）。
      - Skill: `nop-testing`
- [ ] `Add` **种子迁移（P0-1，必须先于 P1-RC-024 断言执行）**：既有 `TestErpSalReturn*` seed `delivery.posted=false` 场景——按门控语义「未暂估 → 跳过事件」会抑制 SALES_RETURN 凭证/ArApItem 致 `TestErpSalReturnPosting` 等既有断言失败；本项将既有种子迁移为 `delivery.posted=true` + 补 SALES_OUTPUT 凭证种子（断言值不变：凭证 1401/6401=20 + ArApItem openAmount=-24 + receivableBalance=-24 等保持），并核对全 `TestErpSalReturn*` 族种子一致性——**这是零回归义务的显式拥有，非静默修补**。
      - Skill: `nop-testing`
- [ ] `Add` P1-RC-024 矩阵：① 未开票 + 已暂估（delivery.posted=true，出库凭证存在）→ 冲减路径（billData 条件标记断言 + 凭证生成断言）；② 已开票 → 红字替代路径（既有行为回归）；③ 未暂估（delivery.posted=false）→ tryPost 跳过事件构造（零凭证/零 ArApItem）；④ 出库已审核但无移动单/无凭证（posted=false 边界变体）→ 同 ③ 跳过语义。
      - Skill: `nop-testing`
- [ ] `Proof` GraphQL 冒烟断言 + `_cases/` 快照录制（对齐 R1.13 快照范式）；既有 `TestErpSalReturn*` 族零回归。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 新增测试矩阵 + 种子迁移全绿 + 既有 sales 测试零回归（断言值不变）：`mvn test -pl module-sales/erp-sal-service`（BUILD SUCCESS）
- [ ] P1-RC-023 五路径 + P1-RC-024 四路径均有断言证据；种子迁移显式登记（无静默修补）；快照录制完成

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: planned
Targets: `docs/design/sales/returns.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [ ] `Add` owner doc 注记：`returns.md §未交货量更新` 补方案 A 载体实现注记 + `§红字发票处理` 补暂估应收条件分支注记；不修改需求契约段。
      - Skill: none
- [ ] `Add` arm-index P1-RC-023 → `done (RC-R1.16)` + P1-RC-024 → `done (RC-R1.17)` + 修复落地摘要；roadmap RC-R1.16/RC-R1.17 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none

Exit Criteria:

- [ ] arm-index/roadmap 状态回填 + owner doc 注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_01e3876f3ffekRWQ6EhrTqiH6U）— 1 P0（P0-1 零回归声明被实测证伪：既有 `TestErpSalReturn*` 全 seed `delivery.posted=false`，门控语义「未暂估 → 跳过事件」将破坏 `TestErpSalReturnPosting:97-116` 等断言——已补显式种子迁移项）+ 2 P1（P1-1 实仓无「暂估应收」凭证载体，`InvAcctDocProvider` SALES_OUTPUT 仅成本侧 6401/1401——判定载体改为运营代理 `delivery.posted=true` + 残留风险；P1-2 预授权边界钉死：未暂估 → tryPost 跳过事件构造，下游零变更，越界则暂停 + ask-first）+ 3 P2（P2-1 deliveredQuantity 写口径钉死毛口径；P2-2 矩阵④「无源出库单」不可达改为 posted=false 边界变体；P2-3「如需」措辞条件化）。行号/ORM/Dispatcher facade 性质全部核验属实。
- Independent draft review iteration 2: accept（独立子代理 ses_01e2f45deffetqMLNam76toOmn 重扫）— P0-1 种子迁移显式拥有（Baseline/Goals/Phase 2 三处落位 + 断言值不变 1401/6401=20、openAmount=-24、receivableBalance=-24）+ P1-1 运营代理载体（delivery.posted=true ⇒ 暂估应收未清 + 残留风险）+ P1-2 预授权边界三处钉死（Goals/Decision/Fix）全部解决；P2-1 毛口径钉死、事实复核全部属实。2 项非阻塞 P2 措辞残留已顺手修订（Goals 测试矩阵删「无 sourceDelivery」不可达路径 / Phase 1 Targets「如需」条件化）。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证（`mvn test -pl module-sales/erp-sal-service` 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-023 方案 B（ORM 加 undeliveredQuantity 列）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 行显式标注「方案 B[触 ORM 加列]须 ask-first」——本计划按方案 A（零 ORM）执行，方案 B 不触发（不越界）；若方案 A 执行中暴露无法零 ORM 达成 L1 断言，计划暂停并升级人工裁决（非自动降级）。
- Successor Required: `no`

### P2-RC-019（deliveredQuantity 零 writer）联动

- Classification: `watch-only residual`
- Why Not Blocking Closure: P1-RC-023 方案 A 写入入口与 P2-RC-019 修复方向同源（写 `ErpSalOrderLine.deliveredQuantity`）；P2-RC-019 为 P2 登记不强制，本行仅覆盖退货回填写入点，出库侧 writer（`rollupOrderDeliveryStatus` 结果写行级）不属本行范围，登记 watch-only。
- Successor Required: `no`

## Closure

Status Note: <待执行>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>

Follow-up:

- <pending — 无范围外 follow-up；MR1 第一批后续 RC-R1.18+ 由 mission driver 继续>
