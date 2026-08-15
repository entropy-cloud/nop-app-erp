# 2026-08-16-0424-2-rc-mr1-r1-52-53-54-ast-depreciation-disposal-family RC-R1.52+RC-R1.53+RC-R1.54 — assets 折旧补提/处置凭证/闲置状态机会计收敛族（MR1 2026-08-12 批量裁决 B 类预授权降级）

> Plan Status: completed
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.52（P1-RC-029 方式B 补提）+ RC-R1.53（P1-RC-030 处置凭证 1606 中间科目）+ RC-R1.54（P1-MA2-061 reuse 重开 IDLE 闲置状态机）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.52/R1.53/R1.54 行 + `docs/audits/arm-index.md` P1-RC-029/P1-RC-030/P1-MA2-061 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 B 类：R1.52「catchUpDepreciation mutation + period 推断，isCatchUp 列可选非必需」/ R1.53「createFacts 重构加 1606 中间科目」/ R1.54「suspend/resume mutation，idleSince 列可选非必需」，均降级为预授权自动执行，不再须 ask-first checkbox**）
> Related: `docs/design/assets/use-cases.md`（L1 UC-AST-07 :118-124 方式B + UC-AST-04 :65-81 + UC-AST-05 + UC-AST-03 :50-55）；`docs/design/assets/depreciation-and-posting.md`（§三 处置科目 + §十 期间控制）；`docs/design/assets/state-machine.md`（§1/§2/§8 IDLE）；`docs/audits/2026-08-07-2345-rc-ma4-a4-2-63-73-assets-depreciation-disposal-capitalization-runtime.md`（A4.2.63-68/70 运行时证据）
> Audit: required

## Current Baseline

- **P1-RC-029（arm-index 行，UC-AST-07 方式B + UC-AST-05 出售补提 reuse）**：L1（`use-cases.md:123`）逐字「方式B(当期补提): 当期一次性补提前期漏提额(简化,不追溯)」——多月累计漏提额当期一次性补提。L3 实仓（A4.2.64/67 运行时确认 + HEAD 复核 2026-08-16）：
  - grep `补提|catchUp|backfill|reversePeriod` 全 `module-assets` 生产代码仅 `ErpAstErrors.java:96` 错误消息串（「折旧期间 {period} 已结账，不允许补提折旧」仅拒绝消息无逻辑），catchUp/backfill **零生产匹配**。
  - `ErpAstDepreciationScheduleExecuteDepreciationProcessor.executeDepreciation:67` `elapsed = countExecuted(assetId) - (wasExecuted?1:0)`——**每次仅计算单月折旧**（elapsed 来自 EXECUTED 行计数），无多月补提循环；`validateAssetInService` + `requirePeriodOpen` 前置守卫（:48-51）。
  - 处置路径 `ErpAstDisposalProcessor.executeApprove:62-101` 从不调 executeDepreciation + `buildEvent:110-114` 读陈旧 accumulatedDepreciation——月中出售累计折旧低估 → NBV 高估 → gainLoss 误算（A4.2.67 量化，GL 平衡不破坏仅金额错报）。
  - **2026-08-12 B 类裁决**：`catchUpDepreciation mutation + period 推断，isCatchUp 列可选非必需` → **纯逻辑可实现，ORM 列可选**（零 ORM 变更路径成立：补提标注经 remark/billHeadCode 承载，镜像 A4.2.66 期间可追溯先例）。
- **P1-RC-030（arm-index 行，UC-AST-04 ② + UC-AST-05）**：L1（`use-cases.md:72-74`）逐字「生成处置凭证: 借 累计折旧(结转), **固定资产清理** / 贷 固定资产(原值结转)」+ owner doc `depreciation-and-posting.md` §三:136-137 两步流（报废: 借累计折旧/借固定资产清理/贷固定资产 → 借营业外支出/贷固定资产清理）。L3 实仓（A4.2.68 + HEAD 复核）：
  - `DisposalAcctDocProvider.createFacts:69-85` 仅 4 类科目[1602 累计折旧 借 / 1002 银行存款 借(disposalAmount>0) / 6711|6301 损益 ± / 1601 固定资产 贷]，**无 1606 固定资产清理中间科目腿**；GL 借贷平衡不破坏（合并凭证与两步流对最终余额影响相同，1606 在两步流中网为零），仅凭证结构/审计轨迹偏离 L1 字面。
  - `TestErpAstAcctDocProviderAccountKey#testDisposal:43-54` 断言 4 facts 不断言 1606。
  - **2026-08-12 B 类裁决**：`createFacts 重构加 1606 中间科目` → 预授权纯逻辑重构（VoucherFact 结构变更，不触 ORM）。
- **P1-MA2-061（arm-index 行，UC-AST-03，R1.18 doc-only Deferred → A1.24 §4 复核倾向重开 → RC-R1.54）**：L1（`use-cases.md:50-55`）「IN_SERVICE → IDLE(闲置): 期间不参与折旧计提 / IDLE → IN_SERVICE(恢复): 恢复计提 / 闲置期间不计提(折旧计划跳过)」。L3 实仓：
  - `ASSET_STATUS_IDLE` 常量（`ErpAstConstants:67`）**零 writer**——grep `setStatus(ASSET_STATUS_IDLE)` 全 `module-assets` 零匹配（仅只读守卫引用：`ErpAstValueAdjustmentProcessor:214` / `ErpAstDisposalProcessor:223` / `ErpAstInventoryProcessor:127` / `ErpAstAssetStateMachine.assertCanShortageDispose:96-103`）；`ErpAstAssetBizModel` 17 行 CRUD 桩零状态机 mutation；owner doc `state-machine.md` §2/§5/§384 Deferred 标注（本期 IDLE 不可达）。
  - 折旧引擎仅查询 `IN_SERVICE`（`ErpAstDepreciationScheduleProcessor` + `ErpAstDashboardBizModel:179`）——等价「IDLE 默认停提」语义已满足；`executeDepreciation` 的 `validateAssetInService` 对 IDLE 拒绝（一致性保持）。
  - **处置守卫现状（HEAD 复核 2026-08-16）**：`ErpAstDisposalProcessor.executeApprove:78-119`（read 基线，:89 调 `ErpAstAssetStateMachine.assertCanDispose`，IN_SERVICE-only :71-76，`TestErpAstAssetStateMachineMatrix:107-108` 强制）——**IDLE 资产当前不可经处置路径**（validateAssetDisposable 层允许 IDLE，但状态机层拒绝）——Phase 3 处置交互须显式裁决（见 Phase 3 Decision）。
  - **2026-08-12 B 类裁决**：`suspend/resume mutation，idleSince 列可选非必需` → **纯逻辑 mutation 可实现，ORM 列可选**（零 ORM 变更路径成立）。
- **预授权判据（2026-08-12 批量裁决 B 类）**：三项均经 ORM 必要性研究核实**不需要强制 ORM 结构变更**（isCatchUp/idleSince 列均标注「可选非必需」）→ **降级为预授权自动执行**（roadmap 文件头裁决段权威；行内注记仍为旧「越界项」文案，以文件头为准，执行时同步注记）。会计过账逻辑收敛按 Q4（2026-08-07 批量授权）「使实现向 owner doc 契约收敛，不反向改契约段落；不涉删除/迁移；核心路径改动行为仍须独立 plan-audit」执行——**本计划仍须独立草案审查 + 独立结束审计**，但**无 ask-first checkbox**；若执行中发现必须 ORM 变更（越界），回落暂停等待人工批准。
- **roadmap 三行均 `todo`**，Deps（R1.0 done）已满足。A4.2.63-73 运行时确认维持三 finding P1 分级不撤销（本计划为 MR1 修复）。

## Goals

- **R1.52**：`catchUpDepreciation(assetId, currentPeriod, missedPeriods[])` @BizMutation + per-mutation Processor 多月补提循环（逐漏提期复用折旧计算，汇总凭证或逐期凭证按 Decision）+ 补提标注（remark「补提 {n} 期」或 billHeadCode 后缀，isCatchUp 列不落 ORM）+ **处置路径接线**：`ErpAstDisposalProcessor.executeApprove` 损益计算前调 catchUp（reuse P1-RC-029 出售补提投影，A1.23 修复义务）。
- **R1.53**：`DisposalAcctDocProvider.createFacts` 重构增 1606 固定资产清理中间科目腿两步流（Step1 结转原值+累计折旧至 1606 + Step2 收入/损益从 1606 结转），GL 恒等式保持（Dr Σ = Cr Σ，1606 网为零）；`TestErpAstAcctDocProviderAccountKey#testDisposal` 断言更新 + 新增 1606 结构断言。
- **R1.54**：`suspend`/`resume` @BizMutation（IN_SERVICE↔IDLE + setStatus writer + 守卫链）+ 折旧行为语义保持（IDLE 期间不计提：引擎 IN_SERVICE-only 查询 + validateAssetInService 拒绝 + PENDING 计划处理按 Decision）+ 闲置超期提醒 cron（config-gated，按 Decision 纳入或登记 successor）+ 测试。
- 分域 `mvn test` 全绿（erp-ast-service）+ 全量构建通过 + checker 零漂移；owner doc 注记（depreciation-and-posting.md / state-machine.md）+ arm-index 三行 done；`docs/logs/` 聚合日志。

## Non-Goals

- **不做强制 ORM 结构变更**（isCatchUp/idleSince 列按 B 类裁决「可选非必需」不落 ORM；若执行中发现必须结构变更 → 越界回落：按 2026-08-15 升级裁决经双独立子 agent 分别检查批准 + 独立 plan-audit，否则保持阻塞）。
- **不反向修改 L1/owner doc 需求契约**（Q4 收敛方向：实现向契约收敛；方式A/B 并列语义、处置科目结构、IDLE 状态迁移均向 L1 收敛，不反向裁剪）。
- **不重开已裁决行为维度**（P1-MA2-060 处置 posted=false 不对称等维持 resolved，不随本计划重审）。
- **不覆盖汇总凭证**（P2-RC-026 每资产单张凭证 Deferred 维持 P2，本计划补提凭证形态按 Decision，不承诺汇总凭证）。
- **不做 1606 两步流的凭证级拆分持久化**（VoucherFact 单张凭证内多腿结构，非两张物理凭证——对齐 L1「生成处置凭证」单数语义；若 Decision 裁决两张物理凭证则另行评估）。

## Task Route

- Type: `implementation-only change`（需求符合性修复——会计过账逻辑收敛 + 状态机 mutation，B 类预授权；不触 ORM/删除/契约段）
- Owner Docs: `docs/design/assets/use-cases.md`（L1 UC-AST-07/04/05/03）+ `docs/design/assets/depreciation-and-posting.md`（§三 处置科目 + §十 期间控制 + 方式A/B）+ `docs/design/assets/state-machine.md`（§1/§2/§8 IDLE）+ `docs/audits/2026-08-07-2345-rc-ma4-a4-2-63-73-assets-depreciation-disposal-capitalization-runtime.md`（运行时证据）
- Skill Selection Basis: `nop-backend-dev`（BizModel/Processor/过账 Provider 修改 + per-mutation Processor 范式 + 错误码；反模式表自检——@Inject 非 private、@BizMutation 事务、I*Biz 注入）；`nop-testing`（JunitAutoTestCase + 快照录制，镜像既有 `TestErpAstDepreciation`/`TestErpAstDisposal` 范式）。不触前端，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新 infra/config 依赖（复用既有 `erp-ast` 域 config 机制；闲置超期 cron 若纳入需 job.yaml 注册对齐 R1.4 范式）。
- 验证命令：`mvn test -pl module-assets/erp-ast-service` + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh`。

## Execution Plan

### Phase 1 - 方式B 补提 + 出售补提接线（RC-R1.52）

Status: completed
Targets: `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/processor/ErpAstDepreciationScheduleExecuteDepreciationProcessor.java` + `ErpAstDepreciationScheduleProcessor.java`（protected helper 持有者）+ `ErpAstDisposalProcessor.java` + `ErpAstErrors.java` + `ErpAstConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision | Add`
- Prereqs: 无

- [x] `Decision` 补提凭证形态：多月补提循环聚合为单张凭证（总额 = Σ各期补提额）vs 逐期凭证（每漏提期一张，镜像既有 per-schedule 凭证）——记录理由（凭证粒度 vs 审计可追溯 vs 期间标注）与残留风险。
      - Skill: `nop-backend-dev`
      - **裁决：单张汇总凭证**（`DepreciationPostingDispatcher.tryPostCatchUp`，billHeadCode = `资产编码#currentPeriod#CATCHUP` + 凭证行 memo「补提 {periods}」）。理由：(1) L1 UC-AST-07 方式B 逐字「当期一次性补提前期漏提额（简化,不追溯）」——"一次性"语义即单张；(2) 已结账漏提期无法逐期过账——财务引擎按凭证日期 `resolveOpenPeriod` 落账，逐期凭证的 voucherDate 落在已结账期间必被拒，单张汇总凭证记账于开放 currentPeriod 是唯一可行形态；(3) 审计可追溯经 双载体 满足：billHeadCode 后缀 `#CATCHUP`（期间可追溯镜像 A4.2.66 先例）+ 逐漏提期 EXECUTED 计划行 + 凭证行 memo 标注归属期间。残留风险：(a) 汇总凭证无法按漏提期单期红冲——`reverseDepreciation`/`executeDepreciation` 对已补提期间重执行会因红冲键不匹配（`assetCode#period` vs `assetCode#period#CATCHUP`）抛 `ERR_REVERSE_SOURCE_NOT_FOUND`，须先冲销整张补提凭证，登记 follow-up；(b) 凭证记账期间为 currentPeriod（非各漏提期），期间归属经 memo 与 schedule 行承载。
- [x] `Fix` `catchUpDepreciation(assetId, currentPeriod, missedPeriods[])` @BizMutation Facade + per-mutation Processor（对齐 R6.3 每 mutation 一 Processor 范式）：守卫链[资产存在 + 状态守卫（**IDLE 不允许补提**——闲置期无折旧义务，恢复经 Phase 3 resume 至 IN_SERVICE 后方可补提，理由记录于 Phase 1 Decision）+ requirePeriodOpen(currentPeriod)] + 逐漏提期补提计算（复用 DepreciationCalculator，elapsed 含已执行期 + 漏提期序）+ 折旧计划落行 + 累计折旧/净值回写 + 凭证生成（补提标注经 remark「补提 {n} 期」或 billHeadCode 后缀，isCatchUp 列不落 ORM）。
      - Skill: `nop-backend-dev`
      - 落地：新 per-mutation Processor `ErpAstDepreciationScheduleCatchUpDepreciationProcessor`（beans.xml 注册）+ `IErpAstDepreciationScheduleBiz.catchUpDepreciation` + `ErpAstDepreciationScheduleBizModel` Facade；漏提期去重升序 + 不晚于 currentPeriod 守卫（新错误码 `ERR_DEPRECIATION_CATCHUP_PERIOD_INVALID`）+ 已 EXECUTED 期幂等跳过 + UK 并发兜底复用 `ERR_AST_DEPRECIATION_ALREADY_EXECUTED`；汇总凭证经 `tryPostCatchUp` + Provider memo 标注。
- [x] `Fix` 处置接线（reuse P1-RC-029 投影）：`ErpAstDisposalProcessor.executeApprove` 损益计算（`buildEvent` 读 accumulatedDepreciation 前）调 catchUp 至出售期——先补提后算 gainLoss。
      - Skill: `nop-backend-dev`
      - 落地：`catchUpDepreciationToDisposalPeriod` protected step（validateAssetDisposable 后、gainLoss 计算前）：补提期 = (最近 EXECUTED 期, 出售期间] 逐月含当期（L1 UC-AST-05 ⑤「补提当期折旧至出售日」）；无已执行折旧（无时间基线）或出售期 ≤ 最近已执行期跳过；IDLE 资产跳过（闲置期无折旧义务，与 catchUp IDLE 拒绝守卫一致）。出售期间已结账时 fail-fast 抛 `ERR_DEPRECIATION_PERIOD_CLOSED`（正确性优先，防止关闭期间出售）。
- [x] `Add` `TestErpAstCatchUpDepreciation`：组测试[单漏提期补提金额=漏提月额 / 多漏提期累加 / 补提后累计折旧+净值一致 / 已结账期间拒绝（复用 ERR 消息语义）/ 出售补提接线后 gainLoss 正确 / 凭证标注可追溯] + `_cases/` 快照。
      - Skill: `nop-testing`
      - 7 组全绿（RECORDING→CHECKING 双态）：单漏提期 1000=月额+累计折旧/净值一致+凭证可追溯（#CATCHUP 回链 + memo）/ 多漏提期 Σ2000 单张汇总凭证+共享 voucherId+凭证金额 2000 / 已结账当前期间拒绝（2026-04 CLOSED → ERR_DEPRECIATION_PERIOD_CLOSED）/ IDLE 拒绝补提（ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE）/ 漏提期晚于当前期间拒绝（ERR_DEPRECIATION_CATCHUP_PERIOD_INVALID）/ 幂等重补提零新行零凭证不双计 / 出售补提接线 gainLoss −2000 正确（未补提为 −3000）+ 出售期计划 EXECUTED+posted + 后续计划 CANCELLED。erp-ast-service **306 tests 全绿**（299 基线 + 7 新增零回归）。

Exit Criteria:

- [x] catchUpDepreciation 多月补提循环运行时正确（金额 = Σ漏提期单月额，与人工核算一致）+ 出售路径补提前置接线实证（测试断言）。
- [x] 既有折旧/处置测试零回归（`mvn test -pl module-assets/erp-ast-service` 局部验证，解除后续 Phase 依赖面）。

### Phase 2 - 处置凭证 1606 中间科目腿（RC-R1.53）

Status: completed
Targets: `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/DisposalAcctDocProvider.java` + `TestErpAstAcctDocProviderAccountKey.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 完成（无强依赖，但按文件头执行顺序先行；避免同域并行改动冲突）

- [x] `Fix` `createFacts` 重构：SCRAPPED/SOLD 四组合（±gainLoss）增 1606 中间腿——Step1 借 1602/借 1606（净值）/贷 1601 + Step2 借 1002（处置收入）/借 1606 或贷 1606（± gainLoss 结转）/贷 1606（净值结转）双段，`SUBJECT_DISPOSAL_CLEARING="1606"` 常量 + ACCOUNT_KEY；GL 恒等式 Dr Σ = Cr Σ 保持（1606 网为零）。
      - Skill: `nop-backend-dev`
      - 落地：`DisposalAcctDocProvider.createFacts` 两步流多腿结构（Step1 借 1602 累计折旧 / 借 1606 净值 / 贷 1601 + Step2 借 1002 处置收入 / 贷 1606 + 损益从 1606 结转[收益借 1606/贷 6301，损失借 6711/贷 1606]）；`SUBJECT_DISPOSAL_CLEARING="1606"` + `ACCOUNT_KEY_DISPOSAL_CLEARING`（resolver 空匹配回退 Provider 科目编码，零映射规则新增）；种子科目表 `erp_md_subject.csv` 纯加性补 1606 固定资产清理行（ID 46，R1.50 1404 先例）——1606 缺失会导致过账 ERR_SUBJECT_NOT_FOUND（含既有处置测试 seed 补 1606，4 测试类）。
- [x] `Add` 断言更新 + 新增：`testDisposal` 断言更新为含 1606 腿结构 + 新增 4 组合（SCRAPPED± / SOLD±）行级结构断言 + 余额恒等式（1606 借贷相抵）。
      - Skill: `nop-testing`
      - `TestErpAstAcctDocProviderAccountKey`：`testDisposal` 7 腿断言更新 + 新增 `testDisposalScrapLossWithClearingLegs`（5 腿 + 1606 网零）/ `testDisposalScrapNoDepreciationWithClearingLegs`（4 腿无 1602）/ `testDisposalSaleGainWithClearingLegs`（7 腿 + 6301 + GL 平衡）三组 + `assertDisposalClearingIdentity`/`assertBalanced` 恒等式助手。
- [x] `Fix` owner doc 对齐：`depreciation-and-posting.md` §三 处置科目补 1606 实现注记（两步流结构 + 网为零恒等式）；arm-index P1-RC-030 → done (RC-R1.53)。
      - Skill: none
      - `depreciation-and-posting.md §三` 增「1606 固定资产清理中间科目腿实现注记（RC-R1.53）」块；arm-index P1-RC-030 行状态 → done (RC-R1.53)（修复摘要 + 测试/构建证据落行）。

Exit Criteria:

- [x] 处置凭证含 1606 腿且 GL 恒等（测试断言 + 快照实证）；既有 4 组合零余额回归。
- [x] owner doc/arm-index 注记落地。

### Phase 3 - IDLE 闲置状态机（RC-R1.54）

Status: completed
Targets: `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/ErpAstAssetBizModel.java`（suspend/resume mutation）+ 新 per-mutation Processor + `ErpAstErrors.java` + `ErpAstConstants.java` + 闲置超期 cron（视 Decision）
Skill: `nop-backend-dev`

- Item Types: `Decision | Fix | Add`
- Prereqs: 无（与 Phase 1/2 独立，按文件头顺序最后执行）

- [x] `Decision` PENDING 折旧计划在 suspend 期间的处理：选项 A 保留计划跳过执行（引擎 IN_SERVICE-only + validateAssetInService 拒绝 = 闲置期间不计提自然成立，PENDING 残留对齐 A4.2.73 SCRAPPED orphaned documented 先例）；选项 B suspend 时 cancel PENDING + resume 时重建（干净但改动面大）。记录理由与残留风险。idleSince 列不落 ORM（B 类「可选非必需」），闲置时长经查询派生。
      - Skill: `nop-backend-dev`
      - **裁决：选项 A（保留计划跳过执行）**。理由：(1) 引擎 IN_SERVICE-only 查询 + `validateAssetInService` 拒绝 + catchUp IDLE 拒绝三重守卫使闲置期间不计提天然成立（零额外代码）；(2) PENDING 行是未来折旧标记，resume 后按开放期间正常执行（PENDING→EXECUTED），无需重建逻辑；(3) 选项 B 增加 cancel/rebuild 两面 + 重建时序（resume 后补建哪些期）的复杂面。残留风险：长期闲置期间 PENDING 行滞留（轻量标记行，处置时 `cancelPendingSchedules` 统一取消，无双计风险）。idleSince 列不落 ORM，闲置时长经 remark「闲置自 {date}」+ 查询派生。
- [x] `Decision` 处置状态机扩展：`ErpAstDisposalProcessor.executeApprove:89` 调 `ErpAstAssetStateMachine.assertCanDispose`（当前 IN_SERVICE-only :71-76，`TestErpAstAssetStateMachineMatrix:107-108` 强制该矩阵）——suspend 使 IDLE 可达后，处置路径 IDLE→SCRAPPED/SOLD 是否放行：选项 A（推荐，契约收敛）：扩展 `ErpAstAssetStateMachine` Bean（assertCanDispose 接受 IDLE + transitions() 元数据 + 矩阵测试更新，对齐 owner doc `state-machine.md` §2「IN_SERVICE/IDLE → SCRAPPED/SOLD」契约）；选项 B：resume-first（IDLE 资产须先 resume 再处置，零状态机改动）。记录理由与残留风险。
      - Skill: `nop-backend-dev`
      - **裁决：选项 A（扩展 StateMachine Bean，契约收敛）**。理由：(1) owner doc §2 迁移表已逐字声明「IN_SERVICE/IDLE → SCRAPPED / SOLD」，选项 A 使实现向既有契约收敛（Q4 方向），选项 B 反而维持实现与契约偏离；(2) 闲置资产经处置退出是 owner doc §8「闲置资产待决策（恢复/处置）」的处置分支；(3) `validateAssetDisposable` 层本就允许 IDLE（既有一致性）。实现：`assertCanDispose` 接受 IN_SERVICE/IDLE + transitions() 7→11 边（suspend/resume + dispose 的 IDLE 来源）+ 矩阵测试更新（dispose/suspend/resume 守卫 + 可达性 + IDLE 非死状态登记）。残留风险：IDLE 处置无补提（闲置期无折旧义务），损益按卡片账面计提为准——已实证（`TestErpAstIdleStateMachine#testPendingSchedulesKeptDuringSuspendThenCancelledAtDisposal` gainLoss=−12000 无补提）。
- [x] `Fix` `suspend`/`resume` @BizMutation + per-mutation Processor：suspend（IN_SERVICE→IDLE：状态守卫 + setStatus(IDLE) + 暂停时点经 remark 记录「闲置自 {date}」（强制，非可选——闲置时长派生的时间基准））+ resume（IDLE→IN_SERVICE：状态守卫 + setStatus(IN_SERVICE) + 恢复计提语义）。
      - Skill: `nop-backend-dev`
      - 落地：新 per-mutation Processor `ErpAstAssetSuspendResumeProcessor`（beans.xml 注册，suspend 强制 remark「闲置自 {date}」/ resume）+ `IErpAstAssetBiz.suspend/resume` + `ErpAstAssetBizModel` Facade（17 行 CRUD 桩→4 mutation 面）；新错误码 `ERR_AST_ASSET_ILLEGAL_STATUS_TRANSITION`（契约 §7 common 作 cause）；StateMachine Bean 增 assertCanSuspend/assertCanResume + suspendTargetStatus/resumeTargetStatus。
- [x] `Decision` 闲置超期提醒 cron：纳入本行（config-gated job.yaml 注册 + 扫描 IDLE + 闲置时长超阈值 notify，镜像 R1.4 job 范式）or 登记 successor（owner doc §8 触发条件未满足时）。记录理由 + 残留风险（纳入则新增 job.yaml 注册面 + 门控默认关闭的运维认知）。
      - Skill: `nop-backend-dev`
      - **裁决：登记 successor（不纳入本期）**。理由：(1) L1 UC-AST-03 断言①②③（状态迁移/恢复计提/闲置期间不计提）均已被 suspend/resume + 引擎语义覆盖，cron 为运营便利性非 L1 字面断言；(2) 闲置时长可经 remark「闲置自 {date}」+ 查询派生，无结构化数据缺口；(3) 纳入需新增 job.yaml 注册面 + config 门控默认关闭的运维认知 + 通知模板种子。残留风险：闲置资产无自动提醒，依赖人工定期检查（运营流程兜底）；**Successor Required**：运营要求闲置资产自动提醒决策时，按 R1.4 job 范式立项（已登记于 plan「Deferred But Adjudicated」节 + owner doc §8 补注）。
- [x] `Add` `TestErpAstIdleStateMachine`：组测试[suspend 后批量折旧跳过（IDLE 不入 batch）/ suspend 后单资产 executeDepreciation 拒绝 / resume 后恢复计提 / 非法迁移拒绝（IN_SERVICE→IN_SERVICE 等）/ 处置路径按 Phase 3 处置状态机 Decision 断言（选项 A：IDLE→SCRAPPED/SOLD 放行 + 矩阵更新；选项 B：IDLE 处置拒绝须先 resume）] + `_cases/` 快照。
      - Skill: `nop-testing`
      - 6 组全绿（RECORDING→CHECKING 双态）：批量跳过（processed=1 仅 IN_SERVICE + IDLE 无计划行）/ 单资产拒绝（ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE）/ resume 恢复计提（EXECUTED 1000）/ 非法迁移拒绝 3 场景（DRAFT suspend / IDLE 重复 suspend / DRAFT resume → ERR_AST_ASSET_ILLEGAL_STATUS_TRANSITION + 资产编码参数）/ remark「闲置自 {date}」强制记录 / PENDING 保留（Decision A）+ IDLE 处置放行（选项 A：SCRAPPED + posted + gainLoss=−12000 无补提 + PENDING CANCELLED）；`TestErpAstAssetStateMachineMatrix` 更新（11 边 / suspend/resume/dispose 守卫 / IDLE 可达性 + 非死状态登记）。
- [x] `Fix` owner doc 对齐：`state-machine.md` §1/§2/§5/§8 Deferred 注记更新为已实现（或 cron 部分标注 successor）；arm-index P1-MA2-061 → done (RC-R1.54)。
      - Skill: none
      - `state-machine.md` §1（IDLE 行 ✅ 已实现）/ §2（迁移图 + 迁移表 + Deferred 块 → ✅ 已实现块含 suspend/resume/引擎语义/处置扩展/cron successor）/ §5（可达性修正 → 已实现）/ §8（TODO 表 + cron 补注 → successor）/ §实现模式（IDLE 节 → ✅ 已实现）/ §审查提示更新；arm-index P1-MA2-061 → done (RC-R1.54)（修复摘要 + 测试/构建证据落行）。

Exit Criteria:

- [x] suspend/resume 状态迁移运行时正确 + IDLE 期间不计提（batch 跳过/单资产拒绝断言）+ resume 恢复计提；闲置超期提醒按 Decision 落地或登记。
- [x] owner doc/arm-index 注记落地。

## Draft Review Record

- Independent draft review iteration 1: needs revision（`ses_ff8e4bacfffehyvIUzot9Wun3D`）——Phase 3 处置-IDLE 交互基线错误（`ErpAstDisposalProcessor.executeApprove:89` 调 `assertCanDispose` IN_SERVICE-only :71-76，IDLE 当前不可处置，测试项与「守卫保持」冲突）+ Phase 3「可选暂停时点记录」反松弛违规；修订：增 Phase 3 处置状态机扩展 Decision（选项 A 扩展 Bean 对齐 owner doc §2 / 选项 B resume-first）+ 测试项随 Decision 分支 + 暂停时点 remark 强制 + 越界回落措辞统一（双独立子 agent 批准）+ 只读守卫枚举补全 4 处 + 基线行号校正。
- Independent draft review iteration 2: acceptable as-is（`ses_ff8dd0b93ffem4OBkjKMaVIgrr`）——阻塞问题已修复（处置-IDLE 裁决显式化 + 反松弛清除）；非阻塞建议已吸收：SCARPED→SCRAPPED 拼写、cron Decision 补残留风险、IDLE 补提守卫默认拒绝显式化、Draft Review Record 落历史。

> **Plan Status 由 draft 转 active（2026-08-16，独立草案审查已收敛）**

## Closure Gates

- [x] 范围内行为完成（Phase 1 + Phase 2 + Phase 3 全部执行项与退出标准）
- [x] 相关文档对齐（depreciation-and-posting.md + state-machine.md 注记 + arm-index 三行 done + roadmap RC-R1.52/R1.53/R1.54 done + 文件头裁决与行内注记同步）
- [x] 已运行验证：`mvn test -pl module-assets/erp-ast-service`（317 tests 0/0/0）+ `mvn clean install -DskipTests` 全量（BUILD SUCCESS）+ `bash docs/audits/nop-compliance-checker.sh`（actual ≤ baseline，R2c 1405→1408 baseline-raise 已登记 per-site 证据）
- [x] 无范围内项目降级为 deferred/follow-up（闲置超期 cron + isCatchUp/idleSince 列为 plan 预裁定 Deferred But Adjudicated 登记项；新增非阻塞 follow-up：补提汇总凭证按漏提期单期红冲）
- [x] 独立草案审查已完成并记录（Draft Review Record：iteration 1 needs revision + iteration 2 acceptable）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（含 `docs/logs/2026/08-16.md` 聚合日志条目）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（本 Closure 节 + 审计报告）

## Deferred But Adjudicated

### 闲置超期提醒 cron（若 Phase 3 Decision 不纳入）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §8 触发条件「PM 要求正式资产闲置/恢复工作流」随本行 suspend/resume 落地后部分满足，但 cron 提醒为运营便利性非 L1 UC-AST-03 字面断言；config-gated 默认关闭不改变主路径。
- Successor Required: `yes`（触发条件：运营要求闲置资产自动提醒决策时，按 R1.4 job 范式立项）

### isCatchUp / idleSince 列（B 类裁决「可选非必需」不落 ORM）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 补提标注经 remark/billHeadCode 承载（期间可追溯，A4.2.66 先例）；闲置时长经查询派生——结构列非必需，零 ORM 变更保持 B 类预授权范围。
- Successor Required: `yes`（触发条件：审计/报表需要结构化补提标记或闲置时点列时，按 ORM ask-first 流程立项）

## Closure

Status Note: **closed**（2026-08-16 三 Phase 全部完成 + 独立结束审计 acceptable；验证全绿：erp-ast-service 317 tests 0/0/0 + 全量 `mvn test` 3461/0/0 + 全量构建 BUILD SUCCESS + checker actual ≤ baseline（R2c 1408 基线登记））

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（general agent，新会话，只读审计未改文件）
- Evidence: `ses_ff860e2caffe8yZ6hT91qDaY7G`——审计报告逐 Phase PASS（Phase 1/2/3 代码实装 + GL 恒等式四组合代数验证 + 反模式零命中 + 317 tests + checker 零漂移），1 项文书修正（arm-index P1-RC-029 行未标 done，执行者已补齐）；复审最终裁决 **acceptable**（全部发现已解决，无剩余修正项）

Follow-up:

- 补提汇总凭证按漏提期单期红冲：单张汇总凭证（billHeadCode `资产编码#currentPeriod#CATCHUP`）无法按漏提期单期冲销——`reverseDepreciation`/`executeDepreciation` 对已补提期间重执行会因红冲键不匹配抛 `ERR_REVERSE_SOURCE_NOT_FOUND`。触发条件：需要单期冲销补提时，按 finance reverse 键契约扩展（非阻塞，成功执行无此需求）。
