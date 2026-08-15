# 2026-08-16-0424-1-rc-mr1-r1-50-pur-price-variance-posting RC-R1.50 — purchase 价格差异过账（MR1 2026-08-12 批量裁决 B 类预授权降级）

> Plan Status: completed
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.50（P1-RC-018 purchase 三单匹配价格差异过账：让步接收 PPV 行 + 差异处理策略可达性）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.50 行 + `docs/audits/arm-index.md` P1-RC-018 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 B 类：`createFacts 加 PPV 行`，降级为预授权自动执行，不再须 ask-first checkbox**）
> Related: `docs/design/purchase/use-cases.md`（L1 UC-PUR-05 :130-143）；`docs/design/purchase/three-way-match.md`（owner doc §价格差异/§不匹配的处理策略）；`docs/audits/2026-08-07-2300-rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime.md`（A4.2.34/A4.2.35 运行时证据）；`docs/plans/2026-08-08-1603-1-rc-mr1-r1-11-pur-receive-over-receipt-tolerance.md`（R1.11 同文件 §不匹配的处理策略先例）
> Audit: required

## Current Baseline

- **finding P1-RC-018（arm-index 行，UC-PUR-05）**：L1（`use-cases.md:130-143`）逐字——「差异 = 发票单价 − 订单单价；若 |差异| > 订单单价 × 价格容差: 匹配状态 = 价格差异待处理」+「处理策略(见 §不匹配的处理策略) 策略 ∈ {拒绝, 审批后接收, 接收并过账差异}」+「// 让步接收时 存在过账行: 科目 == 价格差异科目 且 金额 == 差异 * 数量」。A4.2.34/A4.2.35 运行时确认（2026-08-07）：
  - `PurAcctDocProvider.createFacts:74-82` AP_INVOICE 仅 3 行[1403 在途物资 / 2221 进项税 / 2202 应付账款]，**无 PPV 行**；`PurInvoicePostingDispatcher.buildEvent` billData 无 invoice-vs-order 差异数据（差异埋在 1403 在途物资金额中）；GL 仍平衡（管理会计可视性缺口非活跃数据破坏）。
  - `ThreeWayMatcher.match:62-107` 仅 strict 拒绝 / 非 strict warn[**1/3 策略**]；隐藏接线 grep census 零命中[xbiz/审批流 config/Processor 覆盖均无「审批后接收」/「接收并过账差异」]——「审批后接收」+「接收并过账差异」未实现。
- **L3 实仓（HEAD 复核 2026-08-16）**：
  - `ErpPurInvoiceApproveProcessor.approve` 顺序 = validateNotCancelled → validateTransitionForApprove → `validateBusinessRulesForApprove`（`ErpPurInvoiceProcessor:185-188` 调 `threeWayMatcher.match(invoice.getCode(), lines, null)`）→ doPosting → doApprove → runCommitmentReleaseOnInvoiceApproveHook。
  - `ThreeWayMatcher.match` 数量强制（发票≤入库）+ 价格容差（默认 5%）：strict 抛 `ERR_INVOICE_PRICE_MISMATCH`（:93-103 分支）/ 非 strict LOG.warn 放行——**唯一策略分支**，无策略选择、无差异数据输出。
  - `PurInvoicePostingDispatcher.buildEvent:71-89` billData 仅 TOTAL_AMOUNT/TOTAL_TAX_AMOUNT/TOTAL_AMOUNT_WITH_TAX/SUPPLIER_ID 4 键——**无 per-line invoice-vs-order 差异数据**。
  - `PurAcctDocProvider.createFacts:74-82` AP_INVOICE 三行 fact（1403 借 / 2221 借 / 2202 贷），`fact()` helper 已支持 amountSource/amountFunctional 双金额字段（R1.9）；`readDecimal` 缺键回退 ZERO。
  - **科目现状**：seed `app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv`（44 行）含 1403 在途物资 / 2221 应交税费，**无 1404 材料成本差异/采购价格差异科目**——PPV 科目载体待 Decision（新增 seed 行 or 复用既有科目经 ACCOUNT_KEY 映射）。
  - 既有无条件行为基线：发票无 receiveLineId 行跳过匹配；订单价 0 跳过价格比对（`orderPrice.signum() > 0`）；无订单/直接凭发票场景合法。
- **预授权判据（2026-08-12 批量裁决 B 类）**：`RC-R1.50（createFacts 加 PPV 行）`经 ORM 必要性研究核实**不需要 ORM 结构变更**，纯代码逻辑（createFacts/billData/Matcher）即可解决 → **降级为预授权自动执行**（roadmap 文件头裁决段权威；本行 roadmap 表内注记仍为旧「越界项」文案，以文件头 2026-08-12 裁决为准，执行时同步注记）。会计过账逻辑收敛按 Q4（2026-08-07 批量授权）「使实现向 owner doc 契约收敛，不反向改契约段落；不涉删除/迁移；核心路径改动行为仍须独立 plan-audit」执行——**本计划仍须独立草案审查 + 独立结束审计**，但**无 ask-first checkbox**。
- **roadmap RC-R1.50 行** `todo`，Deps（R1.0 done）已满足。

## Goals

- L1 UC-PUR-05「让步接收时存在过账行: 科目 == 价格差异科目 且 金额 == 差异 × 数量」落地：`PurAcctDocProvider.createFacts` AP_INVOICE 增 PPV 行（金额 = Σ超容差差异量值 = Σ[(发票单价−订单单价)×数量]，scale 4 HALF_UP 对齐既有）。
- `PurInvoicePostingDispatcher.buildEvent` 增 invoice-vs-order 差异数据传递（决策：行级聚合到 header 级 billData 键 vs 行级传递，见 Phase 1 Decision）。
- 差异处理策略可达性收敛：至少使「接收并过账差异」（让步接收）在非 strict 模式经 config 门控可达；「审批后接收」策略按 Phase 1 Decision 裁决（实现 or 登记为 L1 三选一策略子集 + 理由）。
- `TestErpPurPriceVariancePosting` 组测试全绿 + `_cases/` 快照录制（对齐 R1.11 快照范式）；erp-pur-service 既有 284 tests 零回归（2026-08-16 实测基线：53 测试文件）+ 全量构建通过 + checker 零漂移。
- owner doc `three-way-match.md` §价格差异/§不匹配的处理策略 补实现注记；arm-index P1-RC-018 → done (RC-R1.50)。

## Non-Goals

- **不做 ORM 结构变更**（B 类裁决零 ORM；差异数据经 billData/已有列承载）。
- **不反向修改 L1/owner doc 需求契约**（Q4 收敛方向：实现向契约收敛；「审批后接收」若裁决不实现须以 L1 三选一策略子集语义论证 + 登记，不得标注 documented simplification）。
- **不覆盖短收差异处理**（P2-RC-014 短收差异处理已归 R1.11 successor watch-only，不在本行范围）。
- **不改 R1.11 已落地的超收容差校验**（`validateOverReceiptTolerance` 既有行为不变，本行仅价格差异面）。
- **不覆盖跨域财务域成本核算**（three-way-match.md:114 财务域价格差异与费用分摊的成本核算属 finance 域，本行只做采购域过账数据面）。

## Task Route

- Type: `implementation-only change`（需求符合性修复——会计过账逻辑收敛，B 类预授权；不触 ORM/删除/契约段）
- Owner Docs: `docs/design/purchase/use-cases.md`（L1 UC-PUR-05）+ `docs/design/purchase/three-way-match.md`（§价格差异 :75-89 + §不匹配的处理策略 :91-114）+ `docs/audits/2026-08-07-2300-rc-ma4-a4-2-33-39-purchase-f2-threeway-match-runtime.md`（A4.2.34/35 证据）
- Skill Selection Basis: `nop-backend-dev`（BizModel/Processor/过账 Provider 修改 + protected step 接线 + 错误码；反模式表自检）；`nop-testing`（JunitAutoTestCase + @var 快照录制回放，镜像 R1.11 `TestErpPurReceiveOverReceiptTolerance` 范式）。不触前端，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新 infra/config 依赖（复用既有 `erp-pur.match-price-tolerance`（5%）/ `erp-pur.match-strict-mode`（false）配置；新增策略门控 config key 在 Phase 2 Decision 裁决，Phase 1 只消费既有配置 + 新 billData 键）。
- PPV 科目载体若需新增 seed 行：`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv`（44 行种子，纯加性数据行，不触 ORM）。
- 验证命令：`mvn test -pl module-purchase/erp-pur-service` + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh`。

## Execution Plan

### Phase 1 - 差异数据传递 + PPV 过账（R1.50 核心）

Status: completed
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurInvoicePostingDispatcher.java`（buildEvent）+ `PurAcctDocProvider.java`（createFacts）+ `ErpPurConstants.java` + seed `app-erp-all/.../_init-data/erp_md_subject.csv`（视 Decision）
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision | Add`
- Prereqs: 无

- [x] `Decision` PPV 过账语义（科目载体 + 1403 拆分）：
      - 科目载体：检查既有科目 seed（1403/1404 族）+ GL 映射键（ACCOUNT_KEY_PURCHASE 先例）——选项 A：新增 `1404 材料成本差异` seed 行（纯加性数据）+ `ACCOUNT_KEY_PRICE_VARIANCE` 映射键 + `SUBJECT_PRICE_VARIANCE="1404"` fallback；选项 B：复用既有 1403 语义分裂（否决——污染在途物资口径）。记录理由与残留风险（科目默认值可经 beans.xml/GL 映射规则覆盖）。
      - **1403 金额拆分（借贷恒等必要条件）**：发票 TOTAL_AMOUNT 已内含差异金额——超容差让步场景必须将差异自 1403 拆出：1403 行金额 = TOTAL_AMOUNT − Σ超容差差异，PPV 行金额 = Σ超容差差异（涨价借 1404 / 降价贷 1404，方向按差异正负），两者合计恒等于原 TOTAL_AMOUNT（Dr Σ = Cr Σ 保持）；差异 0/键缺失场景 1403 保持原金额零变化。否决「增行不减 1403」方案（破坏借贷平衡，被过账引擎 balance assertion 拒绝）。记录理由与残留风险。
      - Skill: `nop-backend-dev`
- [x] `Fix` `PurInvoicePostingDispatcher.buildEvent` 增差异数据：invoice-vs-order 差异金额聚合（差异 = Σ[(发票单价−订单单价)×数量]，仅超容差行；经 ErpPurInvoiceLine.receiveLineId → ErpPurReceiveLine.orderLineId 回链订单价，对齐 ThreeWayMatcher 回链路径）写入 billData（键名 Phase 1 内定，如 `PRICE_VARIANCE_AMOUNT`）；**写入门控**：仅 Phase 2 裁决的策略 = POST_DIFFERENCE 时写入差异值，拒绝族默认写入 0/不写（Phase 2 接线前 Phase 1 测试经显式配置覆盖验证机制）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `PurAcctDocProvider.createFacts` AP_INVOICE 按 1403 拆分语义重写：读差异键，`差异 != 0` 时 1403 行金额 = TOTAL_AMOUNT − 差异 + 增 PPV fact（科目 = 科目载体裁决，**金额 = |差异| 量值**，借/贷方向按差异正负——涨价借 1404/降价贷 1404），amountSource/amountFunctional 双金额字段对齐 `fact()` helper；差异 0 或键缺失走既有三行（行为零变化）。
      - Skill: `nop-backend-dev`
- [x] `Add` `TestErpPurPriceVariancePosting`：组测试覆盖[超容差涨价让步（POST_DIFFERENCE 配置）→PPV 行 借 1404 金额=差异×数量 + 1403 行金额 = TOTAL_AMOUNT−差异 + 借贷恒等（Dr Σ = Cr Σ）/ 超容差降价→贷 1404 + 1403 相应拆分 / 容差内零差异（既有三行零变化）/ 无 receiveLineId 行跳过 / 订单价 0 跳过 / 差异 0 无 PPV 行 1403 不变 / **默认配置（拒绝族）差异键不写→无 PPV 行回归**] + `_cases/` 快照录制。
      - Skill: `nop-testing`

Exit Criteria:

- [x] AP_INVOICE 凭证在差异 != 0 且差异键写入（POST_DIFFERENCE 策略）时含 PPV 行（金额 = |差异| 量值）+ 1403 行金额 = TOTAL_AMOUNT − 差异 + 借贷恒等（Dr Σ = Cr Σ）（测试断言 + 快照实证）；差异 0/键缺失/默认配置（拒绝族）走既有三行零变化（回归断言）。POST_DIFFERENCE 配置下复跑确认由 Phase 2 测试项（策略路径测试）履行。
- [x] 既有 `TestErpPurThreeWayMatch`/`TestErpPurSettleThreeWayMatchRecheck`/R1.11 超收校验零回归（`mvn test -pl module-purchase/erp-pur-service` 局部验证，解除 Phase 2 依赖面）。

### Phase 2 - 差异处理策略可达性收敛

Status: completed
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/entity/ThreeWayMatcher.java` + `ErpPurInvoiceProcessor.java`（validateBusinessRulesForApprove）+ config keys
Skill: `nop-backend-dev`

- Item Types: `Decision | Fix`
- Prereqs: Phase 1（PPV 行已落地，策略「接收并过账差异」依赖 Phase 1 载体）

- [x] `Decision` 策略选择语义：L1 三策略{拒绝, 审批后接收, 接收并过账差异}实现面裁决——选项 A：`erp-pur.price-diff-strategy` config 键（默认沿用 strict/non-strict 既有语义 = 拒绝族；`POST_DIFFERENCE` 值启用「接收并过账差异」= 非 strict 放行 + PPV 行）；「审批后接收」= L1 三选一子集语义（strict 拒绝即该策略的审批前置，记录理由 + 残留风险，不新建审批流）；选项 B：完整三策略工作流（否决——超范围，diff 审批流无 owner doc 契约支撑）。记录理由与替代方案。
      - Skill: `nop-backend-dev`
- [x] `Fix`（随 Decision）策略接线：`ThreeWayMatcher.match` 或 `ErpPurInvoiceProcessor.validateBusinessRulesForApprove` 增 protected step 按策略分支——拒绝族走既有 strict 抛错/非 strict warn 且 `buildEvent` 不写差异键（createFacts 走既有三行，零 PPV）；`POST_DIFFERENCE` 时超容差行 LOG.warn + 写差异键（Phase 1 billData 键，消费 1403 拆分/PPV 行），放行过账。零新 ErrorCode（复用既有 ERR_INVOICE_PRICE_MISMATCH）。
      - Skill: `nop-backend-dev`
- [x] `Add` 策略路径测试：`TestErpPurPriceVariancePosting` 增[POST_DIFFERENCE 放行 + PPV 行（复跑 Phase 1 PPV 场景）/ 默认配置（拒绝族）差异键不写→既有三行零 PPV 回归 / strict 抛错不变]。
      - Skill: `nop-testing`
- [x] `Fix` owner doc 对齐：`three-way-match.md` §不匹配的处理策略 补策略选择 + PPV 过账实现注记（Q4 收敛方向，不反向改契约段）；arm-index P1-RC-018 追加 RC-R1.50 注记。
      - Skill: none

Exit Criteria:

- [x] 「接收并过账差异」策略经 config 门控运行时可达（测试实证：POST_DIFFERENCE 放行 + 差异键写入 + PPV 行；默认拒绝族差异键不写 → 既有三行零 PPV）；拒绝族既有行为（strict/non-strict）零回归。
- [x] owner doc/arm-index 注记落地；「审批后接收」裁决理由落盘计划或引用文档。

## Draft Review Record

- Independent draft review iteration 1: needs revision（`ses_ff8e4d6c4ffearjzby6pJoXkK0`）——Phase 1 测试断言「PPV 行 + 既有 3 行不变 + 借贷恒等」不可共存（1403 金额含差异未拆分）；修订：Phase 1 Decision 增 1403 拆分语义（1403 = TOTAL_AMOUNT − 差异，PPV = |差异|，涨借/降贷），测试/退出标准同步修正 + 测试计数（163→284 实测）+ buildEvent 行号 + infra 交叉引用。
- Independent draft review iteration 2: needs revision（`ses_ff8dd1afaffeKTOrlXJkvXNCuM`）——Phase 1↔Phase 2 边界未裁决（默认配置下 PPV 语义悬空，最终态测试套件无法全过）；修订：差异键写入门控 = POST_DIFFERENCE（默认拒绝族不写键 → 既有三行），Phase 2 增默认配置零 PPV 回归断言，Phase 1 退出标准改「POST_DIFFERENCE 配置下复跑确认」+ 术语统一（|差异| 量值）+ Draft Review Record 落历史。
- Independent draft review iteration 3: acceptable as-is（`ses_ff8d785e4ffe5ugyzECDS835Pv`）——四要件闭包互洽验证通过（门控/复跑/默认零 PPV 回归/终态全测试可过）+ 术语一致 + Draft Review Record 完整；非阻塞建议已吸收：Goals 聚合口径统一、Phase 1 Exit 注明复跑确认由 Phase 2 测试项履行、ThreeWayMatcher 分支行号微调。

> **Plan Status 由 draft 转 active（2026-08-16，独立草案审查已收敛）**

## Execution Record（2026-08-16 执行）

### D1 PPV 过账语义（Phase 1 Decision，选项 A 选定）

- **科目载体**：选项 A 选定——新增 `1404 材料成本差异` seed 行（`erp_md_subject.csv` 44→45，纯加性数据行，不触 ORM）+ `ACCOUNT_KEY_PRICE_VARIANCE="PRICE_VARIANCE"` GL 映射键 + `SUBJECT_PRICE_VARIANCE="1404"` fallback（`PurAcctDocProvider` 常量，规则表无匹配时走 fallback，行为与既有三行键模式一致）。否决选项 B（复用 1403 语义分裂——污染在途物资口径，无法区分价格差异归属）。
- **1403 拆分**：发票 TOTAL_AMOUNT 已内含差异金额，超容差让步场景差异自 1403 拆出——1403 行金额 = TOTAL_AMOUNT − 差异，PPV 行金额 = |差异| 量值（涨价借 1404 / 降价贷 1404，方向按差异正负），合计恒等于原 TOTAL_AMOUNT（Dr Σ = Cr Σ 保持，过账引擎 balance assertion 通过）。否决「增行不减 1403」方案（Dr Σ ≠ Cr Σ 被过账引擎拒绝）。差异 0/键缺失场景 1403 保持原金额零变化（createFacts 走既有三行分支）。
- **残留风险**：1403 = TOTAL_AMOUNT − 差异在差异超 TOTAL_AMOUNT 的极端场景（发票价远低于订单价的降价让步）可能产生负金额借方行——测试面未覆盖极端比例，登记为会计口径边界（既有一致性约束：差异仅限超容差行聚合，业务上发票价趋近 0 属异常场景）；科目默认值可经 beans.xml/GL 映射规则覆盖。

### D2 策略选择语义（Phase 2 Decision，选项 A 选定）

- **策略门控**：选项 A 选定——`erp-pur.price-diff-strategy` config 键（默认空 = 拒绝族：既有 strict 抛错/非 strict warn + buildEvent 不写差异键 = 零 PPV；`POST_DIFFERENCE` = 「接收并过账差异」：价格超容差 LOG.warn 放行[含 strict 模式下价格维度放行，数量维度 strict 语义不变] + 差异键写入 + PPV 行）。否决选项 B（完整三策略工作流——独立 diff 审批流无 owner doc 契约支撑，超范围）。
- **「审批后接收」**：L1 三策略 {…} 为「策略 ∈」枚举语义非「全部必实现」并列断言——strict 拒绝即该策略的审批前置（拒绝后调整单据再提交），L1 三选一子集语义满足；完整审批工作流登记 Deferred But Adjudicated（successor yes，触发条件 = 业务要求价格差异单独审批工作流）。
- **接线位置**：`ThreeWayMatcher.match` 价格超容差分支按策略分流（POST_DIFFERENCE warn 放行 / 拒绝族 strict 抛错或 warn，复用既有 `ERR_INVOICE_PRICE_MISMATCH` 零新 ErrorCode）+ `ErpPurInvoiceProcessor` 新 protected step `resolvePriceVariance`（策略门控，返回 null=拒绝族 / 差异值=POST_DIFFERENCE）→ `doPosting` → `PurInvoicePostingDispatcher.tryPost(invoice, variance)` 重载写键。差异计算 `ThreeWayMatcher.computeOverTolerancePriceVariance` 复用既有 loadReceiveLine/loadOrderLine 加载器（零新增 daoFor 面，R2c=1405 不变）。

## Closure Gates

- [x] 范围内行为完成（Phase 1 + Phase 2 全部执行项与退出标准）
- [x] 相关文档对齐（three-way-match.md 注记 + arm-index P1-RC-018 done + roadmap RC-R1.50 done + 文件头裁决与行内注记同步）
- [x] 已运行验证：`mvn test -pl module-purchase/erp-pur-service`（317 tests 全绿）+ `mvn clean install -DskipTests` 全量 BUILD SUCCESS + 全量 `mvn test`（6886 tests 0 failures 0 errors）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，actual ≤ baseline：R2c=1405/R10=9 精确一致）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（含 `docs/logs/2026/08-16.md` 聚合日志条目）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 「审批后接收」完整审批流（若 Phase 2 裁决选项 A）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 三策略为「策略 ∈ {…}」枚举语义，非「全部三策略必实现」并列断言；「接收并过账差异」落地 + 「拒绝」既有即满足主路径（拒绝前置 = 审批前置语义）；独立 diff 审批流无 owner doc 契约支撑，选项 B 超范围。
- Successor Required: `yes`（触发条件：业务要求价格差异单独审批工作流时，按 owner doc 契约补充 + 独立立项）

### 财务域成本核算消费 PPV（跨域）

- Classification: `watch-only residual`
- Why Not Blocking Closure: three-way-match.md:114 明确「价格差异与费用分摊的成本核算由财务域处理；本域只提供匹配数据」——本行只做采购域过账数据面（PPV 行生成），财务域成本核算消费属 finance 域 successor。
- Successor Required: `yes`（触发条件：财务域成本核算接入 PPV 差异归集时）

## Closure

Status Note: 独立结束审计通过——两 Phase 全部执行项与退出标准经实仓复核（非采信执行者自述）：`ThreeWayMatcher.match` 价格超容差 POST_DIFFERENCE 分支（warn 放行覆盖 strict 价格维度，数量维度不变，零新 ErrorCode 复用 `ERR_INVOICE_PRICE_MISMATCH`）+ 公开方法 `computeOverTolerancePriceVariance`（Σ[(发票单价−订单单价)×qty]，仅超容差行，receiveLineId→orderLineId→unitPrice 回链复用既有加载器）；`ErpPurInvoiceProcessor.resolvePriceVariance` protected step 策略门控（拒绝族返回 null）→ `doPosting` → `tryPost(invoice, variance)` 重载；`PurInvoicePostingDispatcher.buildEvent` 非空非零才写 `PRICE_VARIANCE_AMOUNT` 键（1-arg tryPost 保留）；`PurAcctDocProvider.createFacts` 1403 拆分（1403 = TOTAL_AMOUNT − 差异）+ PPV 行（1404 材料成本差异，|差异| 涨借/降贷，ACCOUNT_KEY_PRICE_VARIANCE），差异 0/键缺失走既有三行零变化；`ErpPurConstants` 策略 config 键；seed 1404 纯加性行（44→45）。验证复跑：`mvn test -pl module-purchase/erp-pur-service -Dtest=TestErpPurPriceVariancePosting` 9/9 全绿 + 快照 9 目录齐备 + `mvn test -pl module-purchase/erp-pur-service` 317 全绿零回归 + `bash docs/audits/nop-compliance-checker.sh` exit 0（R2c=1405 ≤ baseline 1405，R10=9）。文档对齐实证：three-way-match.md §价格差异/§不匹配的处理策略实现注记、arm-index P1-RC-018 → done (RC-R1.50)、roadmap RC-R1.50 → done ✅（B 类字样 + 文件头 2026-08-16 条目）、log 08-16.md 聚合条目。零 ORM 变更（git diff orm.xml 零命中）、零新增 ErrorCode（ErpPurErrors.java 未触碰）、Deferred But Adjudicated 两项分类正确（successor yes）。非阻塞观察：Goals 行「284 tests 基线」为草案期实测数，执行期实测 308 基线（+9=317）以退出标准/日志/roadmap 及本审计复跑为准。

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent (task closure-audit-2026-08-16-0424-1, fresh session, pid 7696)
- Evidence: 实仓代码逐文件核对（5 生产 Java 文件 + seed csv）+ 2 项强制命令复跑（TestErpPurPriceVariancePosting 9/9、checker exit 0 R2c=1405）+ 全模块 317 tests + 文档四件套（three-way-match.md / arm-index :159 / roadmap :442 + header / log 08-16.md :3-13）核对通过；git diff 实证零 ORM、零 ErrorCode 新增、seed 纯加性

Follow-up:

- <仅非阻塞跟进项目> 无阻塞项；Goals 计数漂移（284→308 草案期 vs 执行期基线）为非阻塞文字差异，不产生跟进义务
