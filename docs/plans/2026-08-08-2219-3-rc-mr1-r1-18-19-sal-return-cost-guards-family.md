# 2026-08-08-2219-3-rc-mr1-r1-18-19-sal-return-cost-guards-family RC-R1.18 + RC-R1.19 — sales 退货成本策略 + 退货 pre-approve 守卫族（MR1 第一批纯预授权）

> Plan Status: active
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.18（P1-RC-026 sales 退货成本策略 config 化）+ RC-R1.19（P1-RC-027 已核销发票 pre-approve 守卫 + P1-RC-028 期间 CLOSED 守卫，同域同控制点协同）— 同域同 owner doc（`returns.md`）同结果表面（退货约束与成本正确性），按计划指南规则 14 合并为一个 owner plan 的三个阶段
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.18/RC-R1.19 行 + `docs/audits/arm-index.md` P1-RC-026/P1-RC-027/P1-RC-028 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`
> Related: `docs/design/sales/use-cases.md`（L1 UC-SAL-07/09）；`docs/design/sales/returns.md`（§退货成本处理/§异常处理）；`docs/audits/2026-08-07-2330-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`（A4.2.57/58 运行时证据）；`docs/audits/2026-08-07-2300-rc-ma4-a4-2-40-46-purchase-f3-returns-runtime.md`（A4.2.43 期间 CLOSED 间接拦截证据）；`docs/plans/2026-08-08-2219-1-rc-mr1-r1-14-15-sal-pricing-family.md`（同批范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-026（arm-index 行，UC-SAL-07 退货成本策略 1/3 + 配置键未声明）**：L1（`use-cases.md:171-174`）逐字「退货入库成本 = 策略(原出库成本 | 当前库存成本 | 退货协议价)，由配置 erp-sal.return-cost-method 决定」。L3 实仓：`ReturnStockMoveBuilder.buildLines:57-69` 恒 `req.setUnitCost(line.getUnitPrice())`（仅「原出库成本」语义）+ `ErpSalConstants` 无 `CONFIG_RETURN_COST_METHOD`（A4.2.57 确认 grep 0 命中）→ 三策略只实现 1/3 + 配置键未声明。§2 P1①（功能实质偏离验收标准）。**非 P0**（GL 平衡不破坏——成本偏差在科目分配非借贷失衡；A4.2.57 量化 FIFO/MA/STANDARD 偏差方向，默认「原出库成本」对齐多数场景）。
- **finding P1-RC-027（arm-index 行，UC-SAL-09 已核销发票 pre-approve 守卫缺失）**：L1（`use-cases.md:207`）逐字「退货关联的发票已核销 → 需先撤回核销再退货」——L1 控制点 = **pre-approve**。L3 实仓：`ErpSalReturnProcessor.validateBusinessRulesForApprove:173-179`（requireCustomerActive + requireSourceDeliveryApproved + requireReasonIfConfigured + returnQtyValidator）**无已核销发票守卫**；`ErpSalErrors` 无 `ERR_RETURN_INVOICE_SETTLED`；post-approve 侧 `ReturnRefundOrchestrator.reverseSettlementsForInvoice:79-99` 静默反向（A4.2.58 运行时确认：依赖 @Version 乐观锁兜底防脏写，无自动重试）。§2 P1②（异常路径未实现）。**非 P0**（@Version 乐观锁兜底 + GL 平衡 + AR 辅助账一致）。
- **finding P1-RC-028（arm-index 行，UC-SAL-09 期间 CLOSED 守卫缺失，会计正确性类 Q4 无例外）**：L1（`use-cases.md:208`）逐字「退货期间已结账 → 拒绝(期间控制)」。L3 实仓：`ErpSalReturnProcessor.validateBusinessRulesForApprove:173-179` **无 `requirePeriodOpen` 守卫** + `ErpSalErrors` 无 `ERR_RETURN_PERIOD_CLOSED`；期间 CLOSED 后退货审核仍可进行（间接经 `SalReturnPostingDispatcher → IErpFinVoucherBiz.post` Facade，finance 引擎 `ErpFinPostingProcessor.resolveOpenPeriod:524-527` 可能在过账环节间接拦截——A4.2.43 证实所有 businessType 全局生效）。§2 P1② + §5 Q4（会计正确性类——期间控制，无例外）。**非 P0**（默认触发面依赖退货审核命中 CLOSED 期间 + 过账侧间接拦截兜底）。
- **实仓（HEAD 核查）**：
  - `ReturnStockMoveBuilder.buildLines:57-69`——**成本策略修复点**：per 行 `unitCost` 按 config `erp-sal.return-cost-method`（默认 original）分支——original=行 unitPrice（现状）/ current=当前库存成本（经库存域 IBiz 查询 `ErpInvStockBalance.avgCost` propId 13 + `costMethod` propId 12，orm.xml:382-383）/ agreement=退货协议价（行 unitPrice 即协议价语义或经行级字段——**Decision 项**）。**同步对齐点（P1-1，arm-index P1-RC-026 修复处方显式要求）**：`SalReturnPostingDispatcher.computeTotalCost:109-117`（billData `KEY_TOTAL_COST` = Σ qty×unitPrice）javadoc 自述与库存移动单 totalCost **「同源」**（`:106-107`）——current/agreement 策略下库存 ledger 按配置成本入账而 GL 凭证 TOTAL_COST 仍按原 unitPrice 将破坏同源不变量；`computeTotalCost` 须同步按策略取值（同一 config 消费点），纳入 Phase 1 范围。
  - `ErpSalReturnProcessor.validateBusinessRulesForApprove:173-179`——**双守卫接入点**（pre-approve，位于 doApprove 之前）。
  - **跨域判定载体**：期间状态——`IErpFinAccountingPeriodBiz`（`ICrudBiz<ErpFinAccountingPeriod>` + `IErpFinPeriodCloseBiz`，四态 OPEN/CLOSING/CLOSED/CLOSED_FINAL + NEVER_OPENED，`IErpFinPeriodCloseBiz.java:48-52`），按 businessDate 查期间 status；**既有先例（非 purchase）**：finance `ErpFinPostingProcessor.resolveOpenPeriod:508-528`（`:519-522` 无期间抛 `ERR_PERIOD_NOT_FOUND`，`:524-527` 非 OPEN 抛 `ERR_PERIOD_CLOSED`）+ assets `ErpAstDepreciationScheduleProcessor.requirePeriodOpen:76-85`（null 期间与非 OPEN 均拒绝）——**A4.2.43 证实 purchase 侧无独立期间校验**（间接守卫），本行对齐 finance/assets 范式；已核销发票——**实仓 reverse 路径按客户级解析**（`ReturnRefundOrchestrator.findReceivedInvoicesOfCustomer:69-77`，`receivedAmount > 0` 即已收核销）——守卫判定载体须对齐该解析面（**Decision 项**：发票级 vs 客户级，见 P2-2 审查项）。
  - `ErpSalErrors`：需新增 `ERR_RETURN_INVOICE_SETTLED` + `ERR_RETURN_PERIOD_CLOSED`（含 ARG_* 参数，中文描述）——零 ORM。
  - **测试基线**：`TestErpSalReturnRefund`（已核销发票反向）+ `TestErpSalReturnQty`（数量守卫）+ `TestErpSalReturnPosting`（过账）——新增守卫后既有测试须零回归（种子场景发票未核销/期间 OPEN）。
- **预授权判据**（第一批纯预授权）：纯 BizModel/Processor + ErrorCode + config key 修复，**不触 ORM 结构/会计过账核心路径（VoucherFact/PostingProcessor 引擎不动——P1-RC-028 守卫为销售域审核侧控制点，P1-RC-026 为移动单请求组装）/删除**；**无 ask-first checkbox**（P1-RC-028 经 A4.2.43 证过账侧已有间接拦截，本行补审核侧显式守卫属收敛性修复）。roadmap RC-R1.18/RC-R1.19 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-sales/erp-sal-service/.../entity/ReturnStockMoveBuilder.java`；`.../posting/SalReturnPostingDispatcher.java`（computeTotalCost 同源对齐）；`.../processor/ErpSalReturnProcessor.java`；`ErpSalConstants.java`；`ErpSalErrors.java`；测试类 1 个新增 + `_cases/` 快照。

## Goals

- **退货成本策略 config 化（P1-RC-026）**：`ErpSalConstants` 增 `CONFIG_RETURN_COST_METHOD = "erp-sal.return-cost-method"`（默认 `original`）+ 三值常量；`ReturnStockMoveBuilder.buildLines` 按 config 分支——original=行 unitPrice（现状默认）/ current=当前库存成本（经库存域 IBiz 跨域查询行物料当前成本，`ErpInvStockBalance.avgCost`）/ agreement=退货协议价（**Decision 项**：协议价载体——倾向行 unitPrice 即协议价语义或经 SKU 默认档）。**`SalReturnPostingDispatcher.computeTotalCost` 同步按策略取值**（同源不变量，arm-index 处方 + javadoc `:106-107` 要求）。零 ORM 变更。
- **已核销发票 pre-approve 守卫（P1-RC-027）**：`ErpSalReturnProcessor.validateBusinessRulesForApprove` 增 protected step（如 `validateInvoiceNotSettled`）：查退货关联发票核销状态，SETTLED 抛新增 `ERR_RETURN_INVOICE_SETTLED` 拒绝审核（提示先撤回核销）；PARTIAL/OPEN 放行（post-approve 既有反向兜底保持）——**Decision 项**记录边界语义（发票级判定载体，若执行中证明必须客户级对齐 reverse 解析面则按审查 P2-2 裁决）。
- **期间 CLOSED 守卫（P1-RC-028）**：`validateBusinessRulesForApprove` 增 protected step（如 `requirePeriodOpen`）：按退货 `businessDate` 查对应会计期间状态，**非 OPEN（含 CLOSING/CLOSED/CLOSED_FINAL/NEVER_OPENED）拒绝**、**无对应期间拒绝**（对齐 finance `resolveOpenPeriod:508-528` 严格语义——无期间抛 `ERR_PERIOD_NOT_FOUND` 同型 + assets `ErpAstDepreciationScheduleProcessor.requirePeriodOpen:76-85` 先例），抛新增 `ERR_RETURN_PERIOD_CLOSED` 拒绝审核（**P1-3 审查裁决：不采用「无期间放行 + LOG.warn」**——该语义与 finance 解析器矛盾且将致 APPROVED + posted=false 悬挂；既有 `TestErpSalReturnPosting` 已 seed OPEN 期间（`seedOpenPeriod("2026-07",...,"OPEN")` `:230`）零回归兼容）。
- **零回归**：既有 `TestErpSalReturn*` 全绿（种子场景发票未核销/期间 OPEN）；全仓 `mvn test` 不引入新失败。
- **owner doc 收敛注记**：`returns.md §退货成本处理` 补 config 化实现注记 + `§异常处理` 补双守卫注记；`use-cases.md` 需求契约段不动（真相源冻结条款）。
- **测试矩阵**：P1-RC-026（默认 original 回归 / current 分支数值断言 + computeTotalCost 同源 / agreement 分支 / config 非法值回退默认）+ P1-RC-027（SETTLED 拒绝 / PARTIAL 放行 / 无发票跳过）+ P1-RC-028（CLOSED/CLOSED_FINAL 拒绝 / OPEN 放行 / CLOSING+NEVER_OPENED 拒绝 / 无对应期间拒绝——对齐 finance 语义，无「无期间跳过」选项）。
- 回填 arm-index P1-RC-026/P1-RC-027/P1-RC-028 → `done (RC-R1.18/RC-R1.19)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**（零列/零索引——P1-RC-026 不新增成本策略列/协议价列；P1-RC-027/028 零字段需求）。
- **不触会计过账核心路径**（VoucherFact/PostingProcessor/凭证生成引擎零改动——期间守卫是审核侧控制点，过账侧 resolveOpenPeriod 既有拦截保持）。
- **不改 post-approve 反向语义**（`ReturnRefundOrchestrator.reverseSettlementsForInvoice` 保持——pre-approve 守卫是其前置拦截，非替换）。
- **不实现换货（P1-RC-025）**（独立 finding，ORM ask-first，另行列管）。
- **不改真相源契约段落**（use-cases L1 不动）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/sales/use-cases.md`（L1 UC-SAL-07/09）+ `docs/design/sales/returns.md`（§退货成本处理/§异常处理）+ `docs/design/finance/period-close.md`（期间状态语义）+ `docs/design/finance/costing-methods.md`（成本策略语义）+ `docs/audits/2026-08-07-2330-rc-ma4-a4-2-56-62-sales-f3-f4-returns-gifts-runtime.md`（A4.2.57/58 运行时证据）
- Skill Selection Basis: 实现面 = BizModel/Processor protected step + config 三级读取 + 跨域 IBiz（`nop-backend-dev`：跨实体访问规则、protected step 模式、config 范式、错误码）；测试（`nop-testing`：JunitAutoTestCase 断言 + 快照录制）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 新增 config key `erp-sal.return-cost-method`（默认 original；current/agreement 部署启用时设置；无需 .env——缺省走默认值）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-sales/erp-sal-service`。

## Execution Plan

### Phase 1 - 退货成本策略 config 化（P1-RC-026）

Status: planned
Targets: `ReturnStockMoveBuilder.java`；`SalReturnPostingDispatcher.java`；`ErpSalConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [ ] `Decision` **current/agreement 策略取值载体**：选项 A（推荐）= current 经库存域既有 IBiz 查询当前库存成本（`IErpInvStockBalance.avgCost` propId 13 + `costMethod` propId 12，orm.xml:382-383——执行时核查库存域暴露的读入口，若 ICrudBiz 空则经 costLayer/valuation 查询组合），agreement = 行 `unitPrice`（退货协议价语义——退货行单价即协议价）；选项 B = 新增跨域 IBiz 方法（契约扩展，审批面更大，超出最小修复面，弃）。记录理由。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `ErpSalConstants` 新增 `CONFIG_RETURN_COST_METHOD = "erp-sal.return-cost-method"` + `RETURN_COST_METHOD_ORIGINAL/CURRENT/AGREEMENT` 常量（默认 original）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `ReturnStockMoveBuilder.buildLines`：per 行读 config（默认 original），按策略分支设置 `unitCost`——original=行 unitPrice / current=库存域查询结果（查询失败回退 unitPrice + LOG.warn，**Decision 子项**记录回退语义）/ agreement=行 unitPrice（协议价）；**同步更新 `ReturnStockMoveBuilder` javadoc**（`:25`「按原出库成本冲减存货估值口径」在 config 分支落地后失实——改为按 `erp-sal.return-cost-method` 策略取值说明）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` **`SalReturnPostingDispatcher.computeTotalCost` 同步对齐（P1-1）**：按同一 config 取值（与 `buildLines` 共用策略解析辅助方法，避免两处漂移）——current/agreement 策略下 `KEY_TOTAL_COST` 与库存移动单 totalCost **同源**（javadoc `:106-107` 自述契约）；original 策略行为不变。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `buildLines` + `computeTotalCost` 按 config 三策略分支且同源一致 + 默认 original 回归不变（Phase 3 断言证实）
- [ ] 零 ORM 变更（`git diff --stat` 仅 erp-sal-service Java + `_cases/` 快照）

### Phase 2 - 退货 pre-approve 守卫族（P1-RC-027 + P1-RC-028）

Status: planned
Targets: `ErpSalReturnProcessor.java`；`ErpSalErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: Phase 1 完成（同域连续，可并行执行但守卫依赖既有基线）

- [ ] `Decision` **已核销发票判定载体与边界（P2-2 对齐 reverse 解析面）**：选项 A（推荐）= 发票级——经 `ErpSalInvoice`（经 delivery 关联发票，`ErpSalInvoiceLine.deliveryLineId` 载体）查核销状态，SETTLED 拒绝 / PARTIAL+OPEN 放行（L1「已核销」字面 = 完全核销）；**须显式裁决与 reverse 路径的解析面差异**：`ReturnRefundOrchestrator.findReceivedInvoicesOfCustomer:69-77` 按**客户级**（receivedAmount > 0）解析——若守卫发票级集合 < reverse 客户级集合，SETTLED 发票可能过守卫仍被 post-approve 静默反向；裁决 = 守卫按发票级（对齐 L1 字面「退货关联的发票」）且登记客户级残余为 watch-only，或守卫扩展到客户级（超 L1 字面）——**执行时二选一并记录理由**；选项 B = 仅发票级 settled 标志聚合——载体选择执行时核查（对齐 A4.2.58 反向兜底语义）。记录理由。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **期间判定载体与无期间兜底（P1-3 裁决：严格对齐 finance）**：选项 A（推荐）= 经 `IErpFinAccountingPeriodBiz` 查 businessDate 对应期间 status——**非 OPEN 拒绝**（含 CLOSING/CLOSED/CLOSED_FINAL/NEVER_OPENED，对齐 finance `resolveOpenPeriod:524-527` 任何非 OPEN 均拒的语义）+ **无对应期间拒绝**（对齐 `resolveOpenPeriod:519-522` 抛 `ERR_PERIOD_NOT_FOUND` 同型——守卫抛 `ERR_RETURN_PERIOD_CLOSED`，消息含期间缺失）；选项 B = 无期间放行 + LOG.warn——与 finance 解析器矛盾（APPROVED 后过账仍失败 → posted=false 悬挂），弃。既有 `TestErpSalReturnPosting` 已 seed OPEN 期间（`seedOpenPeriod("2026-07",2026,7,...,"OPEN")` `:230`），零回归兼容——执行时核对全 `TestErpSalReturn*` 族期间种子。记录理由。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `ErpSalErrors` 新增 `ERR_RETURN_INVOICE_SETTLED`（ARG_INVOICE_CODE/ARG_RETURN_CODE 参数，中文描述「需先撤回核销再退货」）+ `ERR_RETURN_PERIOD_CLOSED`（ARG_RETURN_CODE/ARG_PERIOD 参数，中文描述「退货期间已结账」）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `ErpSalReturnProcessor` 新增 protected step `validateInvoiceNotSettled(returnOrder, context)` + `requirePeriodOpen(returnOrder, context)`，接入 `validateBusinessRulesForApprove`（**守卫顺序确定化（P2-3）**：`requireSourceDeliveryApproved` 之后、`returnQtyValidator` 之前——先跨域守卫后数量守卫，派生可覆盖）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `validateBusinessRulesForApprove` 含双守卫调用链且守卫先于 doApprove（Phase 3 测试断言证实）
- [ ] 新错误码定义 + 零 ORM 变更
### Phase 3 - 测试矩阵

Status: planned
Targets: `module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalReturnCostAndGuards.java`（新增，覆盖 P1-RC-026 + P1-RC-027 + P1-RC-028）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [ ] `Add` P1-RC-026 矩阵：① 默认 original 回归（不设 config，unitCost=行 unitPrice——既有行为）；② current 分支数值断言（assignConfigValue 开 current + seed 库存成本 → unitCost=当前成本 + `computeTotalCost` 同步同源断言）；③ agreement 分支（unitCost=行 unitPrice 协议价语义）；④ config 非法值回退默认。
      - Skill: `nop-testing`
- [ ] `Add` P1-RC-027 矩阵：① SETTLED 拒绝（`ERR_RETURN_INVOICE_SETTLED`，approveStatus 保持 SUBMITTED）；② PARTIAL 放行（审核通过）；③ 无关联发票跳过；④ OPEN 放行。
      - Skill: `nop-testing`
- [ ] `Add` P1-RC-028 矩阵：① CLOSED/CLOSED_FINAL 拒绝（`ERR_RETURN_PERIOD_CLOSED`，approveStatus 保持 SUBMITTED）；② OPEN 放行；③ CLOSING/NEVER_OPENED 拒绝（非 OPEN 语义，P2-1）；④ 无对应期间拒绝（对齐 finance `ERR_PERIOD_NOT_FOUND` 同型，P1-3 裁决）——执行时核对全 `TestErpSalReturn*` 族期间种子兼容（既有 seed OPEN 期间，:230 先例）。
      - Skill: `nop-testing`
- [ ] `Proof` GraphQL 冒烟断言（`ErpSalReturn__approve` 双守卫场景返回错误码）+ `_cases/` 快照录制；既有 `TestErpSalReturnRefund`/`TestErpSalReturnQty`/`TestErpSalReturnPosting` 零回归。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 新增测试矩阵全绿 + 既有 sales 测试零回归：`mvn test -pl module-sales/erp-sal-service`（BUILD SUCCESS）
- [ ] 三 finding 十二路径均有断言证据；快照录制完成

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: planned
Targets: `docs/design/sales/returns.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3 完成

- [ ] `Add` owner doc 注记：`returns.md §退货成本处理` 补 config 化实现注记 + `§异常处理` 补双守卫注记（含期间判定语义）；不修改需求契约段。
      - Skill: none
- [ ] `Add` arm-index P1-RC-026 → `done (RC-R1.18)` + P1-RC-027/P1-RC-028 → `done (RC-R1.19)` + 修复落地摘要；roadmap RC-R1.18/RC-R1.19 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none

Exit Criteria:

- [ ] arm-index/roadmap 状态回填 + owner doc 注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_01e3daddcffeKVD5SK3rfAEcg9）— 3 P1（P1-1 `SalReturnPostingDispatcher.computeTotalCost` 同源对齐缺失，arm-index P1-RC-026 处方显式要求——已纳入 Phase 1；P1-2 虚假先例：purchase 域无 `requirePeriodOpen`（A4.2.43 证实间接守卫）——已改为 finance `resolveOpenPeriod:508-528` + assets `ErpAstDepreciationScheduleProcessor.requirePeriodOpen:76-85` 先例；P1-3 期间判定「无期间放行 + LOG.warn」与 `resolveOpenPeriod:519-522` 抛 `ERR_PERIOD_NOT_FOUND` 矛盾——已裁决严格对齐：非 OPEN 拒绝 + 无期间拒绝，既有 seed OPEN 期间（`TestErpSalReturnPosting:230`）零回归兼容）+ 3 P2（P2-1 CLOSING/NEVER_OPENED 边界已纳入非 OPEN 拒绝语义；P2-2 已核销发票守卫载体与 reverse 客户级解析面差异已显式裁决；P2-3 守卫顺序确定化）。其余证据核验全部属实。
- Independent draft review iteration 2: needs revision（独立子代理 ses_01e2f3248ffebZ6jSo91ufu8my 重扫）— 7/7 迭代 1 finding 核验解决（computeTotalCost 对齐全位落位 / 先例修正 / 期间判定严格化 / 发票载体裁决 / 守卫顺序确定化 / 事实复核属实）；1 项 P1 文本残留：Goals 测试矩阵仍写「无期间跳过或按 finance 语义」——「无期间跳过」恰是 Decision/Goals 已否决的选项 B，与 P1-3 裁决矛盾 → 已改为「无对应期间拒绝（对齐 finance 语义）」。2 项 P2 顺手修订（orm.xml 行号 :383-385→:382-383 / ReturnStockMoveBuilder javadoc 失实补更新项）。
- Independent draft review iteration 3: accept（独立子代理 ses_01e2c31e7ffe7t41EgWKesTD2v 重扫）— P1 残留（Goals 测试矩阵「无期间跳过」矛盾）已修复且与 Phase 2 Decision + Phase 3 矩阵④三方一致；P2-a orm.xml :382-383 行号两处精确；P2-b javadoc 更新项落位（实仓 `ReturnStockMoveBuilder.java:25` 失实文本确认）。禁词扫描零命中、单结果表面贯穿、Decision 项均带选项+理由。**计划可标记 active。**

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

### P1-RC-026 current 策略跨域成本查询契约

- Classification: `watch-only residual`
- Why Not Blocking Closure: current 策略经库存域既有 IBiz 读入口查询（`ErpInvStockBalance.avgCost` propId 13 + `costMethod` propId 12，执行时核查暴露入口）；若库存域暴露入口不足（ICrudBiz 空壳），回退 unitPrice + LOG.warn 的兜底语义显式记录（不静默）。库存成本查询 API 契约扩展（如需要）不属本行最小修复面。
- Successor Required: `no`

### P1-RC-027 部分核销（PARTIAL）边界 + 客户级 reverse 解析面残余

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1「已核销」字面 = 完全核销（SETTLED）；PARTIAL 放行依赖 post-approve `reverseSettlementsForInvoice` 既有反向兜底（@Version 乐观锁保护，A4.2.58 已证）。reverse 路径按客户级解析（`findReceivedInvoicesOfCustomer:69-77`）而守卫按发票级（L1 字面「退货关联的发票」）——守卫集合 ⊂ reverse 集合的残余由 post-approve 反向兜底承接；若执行中裁决守卫扩展到客户级则记录理由并更新本条目。若业务上需要 PARTIAL 也拦截，属策略增强非 L1 字面义务。
- Successor Required: `no`

### P1-RC-028 守卫语义与 posting 侧 ERR_PERIOD_NOT_FOUND 的层级

- Classification: `watch-only residual`
- Why Not Blocking Closure: 守卫在审核侧提前拦截（sales 域错误码，体验层），finance 过账侧 `resolveOpenPeriod:508-528` 独立兜底（同一语义两次拦截非双报——守卫先于过账触发，过账侧为最终防线）。两者错误码不同（`ERR_RETURN_PERIOD_CLOSED` vs `ERR_PERIOD_CLOSED`/`ERR_PERIOD_NOT_FOUND`）属分层设计非缺陷。
- Successor Required: `no`

## Closure

Status Note: <待执行>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>

Follow-up:

- <pending — 无范围外 follow-up；MR1 第一批后续 RC-R1.21+ 由 mission driver 继续>
