# 2026-08-07-1932-2-rc-mr1-r1-1-fin-budget-vs-actual-three-column RC-R1.1 — finance 预算对比报表三列化（P1-RC-003，MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: RC-R1.1（MR1 第一批纯预授权：finance 预算对比报表三列化，P1-RC-003）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.1 行 + `docs/audits/arm-index.md` P1-RC-003 行
> Related: `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md`（A1.2 切片，finding 来源）；`docs/audits/2026-08-06-0847-rc-ma4-a4-1-6-budget-vs-actual-e2e-assertion-strength.md`（A4.1.6 E2E 补断言义务 fold-in）；`docs/design/finance/budget.md`（L2）；`docs/plans/2026-08-07-1819-1-rc-mr1-r1-0-finding-expansion.md`（R1.0 展开器）
> Audit: required

## Current Baseline

- **finding P1-RC-003（arm-index 行）**：UC-FIN-13 断言④ 三列对比报表未满足——L1 `docs/design/finance/use-cases.md:261-262` 逐字「按 (acctSchema, subject, period, costCenter, project, postingType) 分组 VoucherLine 得到 Budget/Commitment/Actual **三列**」。实现仅两列 + COMMITMENT 计入 actual。
- **实仓现状**：
  - `ErpFinBudgetLineBizModel.getBudgetVsActual:48-108`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/`）：voucher 过滤 `:64-65` `or(eq(postingType,BUDGET), or(isNull, ne(BUDGET)))` —— COMMITMENT 凭证**计入 actual**；两通道聚合 `:96-102`（isBudget → budgetAmount，否则 actualAmount）；`available = budget − actual`（`:104-106`）。
  - DTO `BudgetVsActualRow`（`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/BudgetVsActualRow.java:16-59`）：仅 budgetAmount/actualAmount/availableAmount，**无 commitmentAmount 字段**。
  - 报表入口 `ErpFinReportBizModel.buildBudgetVsActualDataset:229-232`（`.../service/report/`）委托 `IErpFinBudgetLineBiz#getBudgetVsActual`，无独立聚合逻辑——三通道化只改一处。
  - XPT 模板 `module-finance/erp-fin-service/src/main/resources/_vfs/nop/main/report/fin/budget-vs-actual.xpt.xml`：7 列（科目编码/名称/成本中心/项目/预算/实际/余量），无承诺金额列。
  - 单测 `TestErpFinBudgetEndToEnd#testGetBudgetVsActual:195-219`：仅断言两列且**未 seed COMMITMENT**；同文件 `testAvailableDeductsCommitmentSeparately:222-249` 已有 `seedCommitmentVoucher`（postingType=COMMITMENT）seed 范式可复用。
  - E2E `tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts`：GraphQL selection `:135` 仅 6 字段无 commitmentAmount；setupFull 无承付 seed；断言（`:172-195`）仅两列增量。
- **A4.1.6 fold-in 义务**（`2026-08-06-0847-rc-ma4-a4-1-6` §5.2）：修复后 E2E 必须补 ① GraphQL selection 增 commitmentAmount ② seed COMMITMENT 凭证（PO commit 路径或直接 seed postingType=COMMITMENT voucher line）③ 三列增量断言（commitmentAmount=200 + actual 不含 commitment + available=budget−actual−commitment）。
- **对齐范式**：控制引擎 `ErpFinBudgetControlBiz.applyPostingTypeFilter:162-164` ACTUAL 通道 `notIn(BUDGET, COMMITMENT)`（P1-MA2-084 已 fix）+ `ErpFinBudgetScenarioCarryForwardProcessor:221-222` 排两者——报表须与已落地三通道分离口径一致。
- **预授权判据**（第一批纯预授权）：arm-index P1-RC-003 行「纯 BizModel/DTO/XPT 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first」+ 展开器映射记录 §3.1（「纯 BizModel/DTO/XPT 代码逻辑修复」）。无 ORM/会计核心/删除触及（DTO 非 ORM），**无需 ask-first checkbox**。

## Goals

- `getBudgetVsActual` 三通道化：BUDGET → budgetAmount；COMMITMENT → commitmentAmount；其余（NORMAL/NULL/RESERVATION 等）→ actualAmount——对齐 P1-MA2-084 控制引擎口径（NOT BUDGET AND NOT COMMITMENT = actual）。
- `BudgetVsActualRow` 增 `commitmentAmount` 字段；`available = budget − actual − commitment`。
- XPT 模板增「承诺金额」列（header + 数据列 + 合计）。
- 单测扩展：`testGetBudgetVsActual` 补 COMMITMENT seed + 三列断言（commitmentAmount/actual 不含 commitment/available 三项式）。
- E2E 补断言（A4.1.6 三项义务落地）。
- 回填 arm-index P1-RC-003 修复状态 + roadmap RC-R1.1 标记 done。

## Non-Goals

- **不涉及试算平衡/GL 重分类 5 站点过滤**（P1-RC-091 试算平衡 + 4 GL 重分类——不同控制点，越界项 RC-R1.46 独立处理）。
- **不改控制引擎**（P1-MA2-084 已 fix，仅对齐口径，不重复改动）。
- **不改 ORM / api.xml / 数据字典**（DTO 是跨层契约 bean，非持久化实体；零结构变更）。
- **不修改真相源**（use-cases.md/budget.md 需求契约段——L2 budget.md 已描述三列语义，实现向契约收敛）。
- **不做承付凭证生成侧改动**（COMMITMENT 凭证生成/红冲路径已正确，非本 finding）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/finance/use-cases.md`（L1 UC-FIN-13）+ `docs/design/finance/budget.md`（L2 §业务规则5）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）+ `docs/testing/e2e-runbook.md`（E2E 编写/运行规范，flux 强制）
- Skill Selection Basis: BizModel/DTO 实现 + JUnit（`nop-backend-dev` + `nop-testing`）；XPT 报表模板列（`nop-frontend-dev`——报表模板属前端展示面，无专属 report skill）；E2E spec 修改（`Skill: none`——Playwright 套件以 `docs/testing/e2e-runbook.md` 为权威，无匹配平台技能）。

## Infrastructure And Config Prereqs

- 无新增 config/端口/外部服务。E2E 以 flux 引擎运行（`E2E_ENGINE` 缺省即 flux，runbook §渲染模式）。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-finance/erp-fin-service`。
- E2E 运行前置：flux 链重建（如需，`scripts/rebuild-flux-chain.sh`，runbook §渲染模式三路径）——若本次无 flux 变更则直接跑既有链。

## Execution Plan

### Phase 1 - DTO + BizModel 三通道化

Status: completed
Targets: `module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/BudgetVsActualRow.java`；`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinBudgetLineBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Decision`
- Prereqs: 无

- [x] `Add` `BudgetVsActualRow` 增 `commitmentAmount` 字段（默认 `BigDecimal.ZERO`，setter 空值兜底 ZERO——对齐既有 budgetAmount/actualAmount setter 范式 :47-54）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `getBudgetVsActual` 三通道化：postingType 三分类（BUDGET → budget；COMMITMENT → commitment；其余含 NORMAL/NULL/RESERVATION → actual）；available = budget − actual − commitment（`:104-106` 改三项式）。
      - Skill: `nop-backend-dev`
- [x] `Decision` actual 通道口径：三通道分类在**内存内 per-voucher 分类谓词**处实现——`channelOf` 映射（`:70-74` 同型扩展）：BUDGET → budget 通道；COMMITMENT → commitment 通道；其余（NORMAL/NULL/RESERVATION）→ actual 通道（等价 `actual = NOT(BUDGET OR COMMITMENT)`，对齐控制引擎 `applyPostingTypeFilter:162-164` P1-MA2-084 范式 + `testCommitmentZeroEquivalentAndReservationCountsAsActual` 既有语义——RESERVATION 保持计入 actual）。**voucher 查询过滤（`:64-65`）保持加载全部三通道凭证**（恒真式保留并更新注释，不承担通道分流，三通道分类在内存内 per-voucher 谓词完成）。备选：按三枚举分类且 voucher 过滤改 `in(BUDGET, COMMITMENT) OR isNull(postingType)`（否决：显式排除更符合「NOT BUDGET AND NOT COMMITMENT = actual」跨组件统一口径，且与 P1-RC-091 修复方向一致）。
      - Skill: `nop-backend-dev`
- [x] `Fix` 陈旧注释同步（两通道语义描述随修复失效）：`BudgetVsActualRow.java:5-14` javadoc 增 commitment 列描述；`ErpFinBudgetLineBizModel` 类 javadoc 三通道语义；`TestErpFinBudgetEndToEnd` 相关测试注释（Phase 2 落地）；E2E spec 头注释 `:25-27` 三列语义更新（actual 定义排除 COMMITMENT，Phase 4 落地）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `commitmentAmount` 字段存在；三通道聚合正确（BUDGET/COMMITMENT/其余 各归其道）；available 三项式（`testGetBudgetVsActual` 断言 available=1000−400−200=400 成功）
- [x] 零 ORM/契约面破坏（DTO 纯加字段，反向兼容既有 selection；`mvn clean install -DskipTests` 全量 BUILD SUCCESS）

### Phase 2 - JUnit 单测扩展

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinBudgetEndToEnd.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` 扩展 `testGetBudgetVsActual`：seed COMMITMENT 凭证（复用 `seedCommitmentVoucher:233` 范式）→ 断言三列（budgetAmount=1000 / actualAmount=400 不含 commitment / commitmentAmount=200 / available=1000−400−200=400）；既有 budgetAmount/actualAmount 断言值不变，available 断言由 600 改 400（三项式，属既有断言的契约性更新）。快照经 `@EnableSnapshot(saveOutput=true)` 重录（新增 seed 致 auto-increment ID 偏移）。
      - Skill: `nop-testing`
- [x] `Proof` 边界断言：新增 `testGetBudgetVsActualNoCommitmentDegeneratesToTwoTerm`——无 COMMITMENT 时 commitmentAmount=0 且 available 退化为两项式 600（等价性，对齐 `testCommitmentZeroEquivalentAndReservationCountsAsActual` 控制引擎既有范式）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] `mvn test -pl module-finance/erp-fin-service` 全绿（346 tests 全用例零回归，含 `TestErpFinBudgetEndToEnd` 9 用例）

### Phase 3 - XPT 报表模板增列

Status: completed
Targets: `module-finance/erp-fin-service/src/main/resources/_vfs/nop/main/report/fin/budget-vs-actual.xpt.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 完成

- [x] `Add` XPT 增「承诺金额」列：header（:34-36 区段追加 cell）+ 数据行 cell（`*=commitmentAmount`）+ 合计 cell（`SUM(F3)` 追加）；列宽/样式对齐既有 num 样式（8 列 / 标题 mergeAcross=7，列序 预算→承诺→实际→余量）。
      - Skill: `nop-frontend-dev`
- [x] `Fix` 模板头注释（:2-5）同步三列语义描述（budget.md §业务规则5 契约：预算/承付/实际/余量四项），保持模板自文档一致。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] XPT well-formed（`xmllint --noout` 通过）+ 列结构与 DTO 字段一致（budgetAmount/commitmentAmount/actualAmount/availableAmount 四字段映射）

### Phase 4 - E2E 补断言（A4.1.6 三项义务）

Status: completed
Targets: `tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts`
Skill: none（e2e-runbook 权威）

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1-2 完成（E2E 依赖后端三通道化先落地，E2E 方有 commitment 列可断言）

- [x] `Add` GraphQL selection 增 `commitmentAmount`（`:135` 字段集扩展为 7 字段）。
      - Skill: none
- [x] `Decision` COMMITMENT seed 方式：直接 seed `ErpFinVoucher__save`（postingType=COMMITMENT）+ `ErpFinVoucherLine__save`（对齐 `fin-intercompany-matching-elimination.action.spec.ts` / `fin-budget-rollforward-carryforward.action.spec.ts` 直置凭证范式，且 A4.1.6 明确允许「直接 seed postingType=COMMITMENT voucher line」）；备选：走 PO commit 全链（否决：引入 purchase 域依赖，破坏 spec 自包含隔离）。seed 期间隔离：COMMITMENT 凭证挂现有 period=1 且 subjectId=6602，spec 断言按 subjectId 过滤隔离；cleanup 反依赖链删行+头。
      - Skill: none
- [x] `Add` 三列增量断言（步骤 (d)）：seed COMMITMENT=200 后 commitmentAmount=200 + actualAmount=0 不含 commitment + available=1000−0−200=800（既有 (a)(b)(c) 断言保持通过——无 commitment 时 available 语义不变）；cleanup 增删 COMMITMENT 凭证。
      - Skill: none
- [x] `Proof` E2E 运行验证：`E2E_ENGINE=flux npx playwright test tests/e2e/business-actions/fin-budget-vs-actual.value.spec.ts --workers=1`（runbook §运行命令表单文件范式，repo root 执行）→ 1 passed（9.2s，系统 Chrome channel fallback）。
      - Skill: none

Exit Criteria:

- [x] 三列断言 E2E 绿（含既有断言零回归）；spec 自包含隔离（cleanup 覆盖 COMMITMENT 凭证）

### Phase 5 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/audits/arm-index.md`（P1-RC-003 修复状态）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.1 done）；`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-4 完成

- [x] `Add` arm-index P1-RC-003 行「修复状态」→ `✅ done (RC-R1.1)` + 修复摘要（三通道化 + commitmentAmount + available 三项式 + XPT 列 + 单测/E2E 补断言 + 验证结果）；roadmap RC-R1.1 → `done ✅`；日志条目 `docs/logs/2026/08-07.md` 顶部追加。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 回填 + 日志条目落盘

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 `ses_023fe4a66ffeutoLUqur08Pf1H`，fresh session）——基线/范围/预授权/A4.1.6 义务/typing/技能/退出标准/Closure Gates/反松弛全 PASS，0 BLOCKER 0 MAJOR，3 non-blocking MINOR（Phase 2 措辞矛盾[seed COMMITMENT 后 available 断言 600→400] / Phase 1 actual 通道口径措辞可误读为 voucher 查询过滤 / E2E 头注释+DTP javadoc 陈旧注释未覆盖）——修订：Phase 1 Decision 明确「内存内 per-voucher 分类谓词」+ 陈旧注释 Fix 项（含 `BudgetVsActualRow.java:5-14` javadoc + E2E spec `:25-28` 头注释）；Phase 2 断言措辞改「available 由 600 改 400（三项式）」；XPT 像素 Deferred 条目按实仓核实修正（visual 套件零覆盖本模板）。
- Independent draft review iteration 2: `accept`（独立子代理 `ses_023f353c6ffe9Qs7DWjsbQUq0c`，fresh session）——3 MINOR 全落地核实（`:25-28` off-by-one 观察非阻塞），0 新问题。共识达成，转 active。

## Closure Gates

- [x] 范围内行为完成：三通道聚合 + commitmentAmount + available 三项式 + XPT 列 + 单测/E2E 断言全部落地
- [x] 相关文档对齐：owner doc 无契约段改动（实现向既有契约收敛）；arm-index/roadmap 状态回填 + 日志条目落盘
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service` 全绿（346 tests）+ E2E 单 spec 绿（flux，1 passed）+ `mvn clean install -DskipTests` 全量构建通过 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（全 16 可计数规则 = 基线，零漂移，R12c=40 与 machine-readable 块一致）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-091（试算平衡 + 4 GL 重分类 5 站点 BUDGET-only 过滤）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 与 P1-RC-003 同根因家族但不同控制点（报表列数/口径 vs 试算平衡恒等式 + GL 重分类聚合）；§7 A3 裁决属会计核心路径须独立 plan-audit + ask-first，归越界项 RC-R1.46 独立处理。本计划仅对齐口径（notIn(BUDGET,COMMITMENT)），不触及该 5 站点。
- Successor Required: yes（RC-R1.46 越界项）

### XPT 模板像素级视觉回归

- Classification: `watch-only residual`
- Why Not Blocking Closure: 已核实 `tests/e2e/visual/` 下无任何 snapshot spec 覆盖 budget-vs-actual 模板（grep `budget-vs-actual`/`budgetVsActual` 零命中）——本模板不在像素截图基线内，XPT 增列无像素回归面，无需 visual 套件确认。
- Successor Required: no

## Closure

Status Note: 执行完成（draft → 独立草案审查 ×2 accept → active → 执行 → 待独立结束审计）。第一批纯预授权（无 ask-first）。实现向 L1 三列契约 + 控制引擎已落地三通道口径收敛。`getBudgetVsActual` 三通道化（per-voucher `channelOf` 分类：BUDGET→budget / COMMITMENT→commitment / 其余→actual）+ `BudgetVsActualRow` 增 `commitmentAmount` + `available=budget−actual−commitment` 三项式 + XPT「承诺金额」列 + 单测双用例（三列断言 + 无承付退化等价）+ E2E 三列断言（A4.1.6 三项义务）。验证：分域 `mvn test` 346 全绿 / E2E 单 spec 1 passed（flux）/ 全量 `mvn clean install -DskipTests` BUILD SUCCESS / compliance checker actual == baseline 零漂移。arm-index P1-RC-003 → `done (RC-R1.1)` + roadmap RC-R1.1 → done ✅ + 日志条目落盘。

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计子代理（新会话，不重用执行者上下文）
- Evidence: 待执行后填写（可引用：分域 `mvn test -pl module-finance/erp-fin-service` 346 全绿输出 + E2E `fin-budget-vs-actual.value.spec.ts` 1 passed 输出 + `mvn clean install -DskipTests` BUILD SUCCESS + checker actual==baseline 零漂移表 + arm-index:128 P1-RC-003 行 `done (RC-R1.1)` + roadmap:369 RC-R1.1 行 done ✅ + `docs/logs/2026/08-07.md` 顶部条目）

Follow-up:

- 无范围内 follow-up；RC-R1.46（P1-RC-091）为越界项独立处理
