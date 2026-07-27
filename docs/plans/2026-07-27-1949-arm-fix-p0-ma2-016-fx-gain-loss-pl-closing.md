# 2026-07-27-1949-arm-fix-p0-ma2-016-fx-gain-loss-pl-closing P0 修复 — 期末损益结转过度排除汇兑损益分录致费用类科目余额不归零

> Plan Status: active
> Mission: audit-remediation
> Work Item: P0-MA2-016（即时通道修复 — `ProfitLossClosingService` 损益结转聚合排除 `EXCHANGE_GAIN_LOSS` 分录，致汇兑损益费用类科目余额未结转至本年利润）
> Last Reviewed: 2026-07-27
> Source: `docs/audits/arm-index.md` §P0 发现追踪（P0-MA2-016，状态 `fix-plan-injected (protected area gate)`）；`docs/audits/2026-07-27-1949-arm-ma2-period-close-e2e.md` §4 P0 / §3.5
> Related: `2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e.md`（发现该 P0 的审计 plan，closure Follow-up 显式命名本 fix plan）；`docs/design/finance/period-close.md`（§步骤5 损益结转 / §汇兑重估 owner doc）；`docs/design/finance/bad-debt.md`；`docs/design/flow-overview.md §六`（事务边界）
> Audit: required

## Current Baseline

P0-MA2-016 是 A2.3 期末结账端到端审计（plan 2026-07-27-1949-3）在「业务正确性 — 损益结转」维度发现的 P0 实时业务正确性缺陷，已登记 `docs/audits/arm-index.md` §P0 追踪，状态 `fix-plan-injected (protected area gate)`。roadmap 横切关注点 §P0 即时通道纪律明示 P0 不得进入 MR 批量修复；1949-3 closure 因其触及 finance 损益结转保护区域（`project-context.md §AI 阻塞条件`：会计/财务为 ask-first 最高级别保护区域），显式选择异步注入独立 fix plan（本计划），先于 MR1 执行。

缺陷精确定位（实仓核实，`docs/audits/2026-07-27-1949-arm-ma2-period-close-e2e.md` §3.5）：

- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/profitloss/ProfitLossClosingService.java:88-92` 聚合本期分录时排除 `businessType == PERIOD_CLOSE || EXCHANGE_GAIN_LOSS`。
- 排除 `PERIOD_CLOSE` 正确：损益结转凭证自身的收入/费用科目行若再聚合会重复结转。
- 排除 `EXCHANGE_GAIN_LOSS` **过度**：汇兑重估凭证（`ExchangeRevaluationService.revalueArAp:143-145` / `revalueBankDeposits:205-207`）的汇兑损益分录（`CONFIG_FX_GAIN_LOSS_SUBJECT_CODE`，seed `6603` 为 `SUBJECT_CLASS_EXPENSE`，`CloseVoucherWriter:125` 写入 `businessType=EXCHANGE_GAIN_LOSS`）被排除后，**汇兑损益（费用类）余额永不结转至本年利润**。
- 后果：结账后汇兑损益科目残留非零余额；本年利润少计该汇兑净额。违反 owner doc `period-close.md §步骤5`「结转后收入/费用科目余额归零」+ 控制点 3（损益结转余额归零）。

证据链（`TestErpFinPeriodCloseEndToEnd` seed + `period-close-end-to-end-test.yaml`）：AR 外币项 openSource=100/openFunctional=800、periodEndRate=8.5 → revaluedFunctional=850、diff=800−850=−50 → 应收 diff<0=收益 → 借 AR 50 / 贷汇兑损益 50。汇兑损益产生贷方余额 50（收益），被 P&L 结转排除 → 残留。该测试未断言汇兑损益净额归零，故缺陷未被发现。

关键事实（决定修复方案 — 实仓已核实）：

- 汇兑重估凭证的对手方分录（AR/AP/银行存款 = 资产/负债类）本就被 `subjectClass` 过滤（非 INCOME/EXPENSE/COST），排除 `EXCHANGE_GAIN_LOSS` 对它们无影响。
- 唯一受影响的是汇兑损益分录（费用类）。移除 `EXCHANGE_GAIN_LOSS` 排除后，汇兑损益余额将正常结转至本年利润。
- 反结账 + 重新结账幂等性：反结账经 `reverseCloseVoucher` 红冲 FX 凭证（`isReversed=true`），`findPostedVoucherIds` 过滤 `isReversed=false`，故反结账后的重结账不会重复聚合已红冲 FX 凭证。移除排除不破坏幂等性。
- 年度结转 `AnnualCloseService.aggregateYearSubjectActivity:283-307` 仅排除 `PROFIT_TO_RETAINED_EARNINGS`，**不排除** `EXCHANGE_GAIN_LOSS`——故年度级汇兑损益已正确纳入本年利润净额计算；缺陷仅限于月度损益结转层。

剩余差距：从 `ProfitLossClosingService:89-90` 排除条件移除 `EXCHANGE_GAIN_LOSS`（保留 `PERIOD_CLOSE`）+ 补「FX 场景汇兑损益结转后归零」测试证明 + arm-index/scope matrix 状态回填。

## Goals

- 修正 `ProfitLossClosingService` 损益结转聚合，使汇兑重估凭证的汇兑损益（费用类）余额正常结转至本年利润，结账后汇兑损益科目净额归零（对齐 owner doc `period-close.md §步骤5` + 控制点 3）。
- 行为收敛：仅移除 `EXCHANGE_GAIN_LOSS` 排除（保留 `PERIOD_CLOSE` 排除防结转凭证自身重复结转）；不改变汇兑重估凭证生成、反结账红冲、年度结转语义。
- 提供测试证明：FX 场景（外币 AR + periodEndRate 触发重估）下，结账后汇兑损益科目净额归零 + 本年利润含汇兑净额。防回归（防止未来重构悄悄加回排除）。
- 回填 `arm-index.md` P0-MA2-016 状态为 `done` + `audit-remediation-scope-and-dimension-matrix.md §2.2` finance 列 `⚠️(P0→fix-plan + P1)` → `⚠️(P1)`。

## Non-Goals

- **不**修复 P1-MA2-017..022 / P2-MA2-023..025（A2.3 审计其他发现）—— 经 R1.0 展开机制进入 MR1 统一裁决。
- **不**改动 `ExchangeRevaluationService`（汇兑重估凭证生成逻辑正确，每张凭证自平衡）。
- **不**改动 `AnnualCloseService`（年度级汇兑损益聚合已正确）。
- **不**改动 `CloseVoucherWriter`（凭证写入 + 借贷平衡强制正确）。
- **不**实现 FX 重估前期 reversal（P1-MA2-022，MR1 裁决）。
- **不**手改生成物（`_gen/`、`_` 前缀、`_app.orm.xml`）—— 本修复仅触及保留层 Java 源（`ProfitLossClosingService.java` 是非生成文件）+ 测试源。
- **不**改变 `auto-post-on-close` / `exchange-revaluation-enabled` 等 config 默认值（P1-MA2-017 MR1 裁决）。

## Task Route

- Type: `implementation-only change`（修复已确认的实时业务正确性缺陷，无契约/模型变更）
- Owner Docs: `docs/design/finance/period-close.md`（§步骤5 损益结转 — 验证 owner doc 描述「结转后收入/费用科目余额归零」预期行为）；`docs/design/finance/bad-debt.md`（汇兑损益科目归属费用类）；`docs/design/flow-overview.md §六`（事务边界 — 损益结转跟随 Facade @BizMutation 单库 REQUIRED）
- Skill Selection Basis: `nop-backend-dev`（修复涉及损益结转聚合逻辑 + 费用类科目结转方向，roadmap skill 列匹配）。`nop-debugging` 不匹配——这是已定位的已知缺陷修复，非调查。
- Verification: 修复触及 finance 损益结转层；运行 finance 单模块测试（`TestErpFinProfitLossClosing` / `TestErpFinPeriodCloseEndToEnd` / `TestErpFinPeriodPreCheck`）+ 全量 `mvn clean install -DskipTests`（154 模块）+ `mvn test`（回归基线，0 failures）。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。
- **保护区域门控（关键）**：本修复触及 finance 损益结转（`ProfitLossClosingService`，`closePeriod` GL 模块子服务）保护区域。预期行为由 owner doc `docs/design/finance/period-close.md §步骤5`（结转后收入/费用科目余额归零）描述。本修复**使实现与 owner doc 一致**（当前排除 `EXCHANGE_GAIN_LOSS` 是偏离 owner doc 的过度排除），属收敛性修复。仍按保护区域纪律执行：**人工确认**（`project-context.md §AI 阻塞条件`：会计保护区域 P0 即时修复须人工确认）+ 独立 plan-audit + closure-audit（本计划即此流程）。
- 无数据迁移/回滚脚本需求（修复后下次结账即产生正确结转；已结账期间的残留汇兑损益余额由 MR1 裁决是否补结转，不在本 fix plan 范围）。

## Execution Plan

### Phase 1 - 修复损益结转聚合 + 测试证明

Status: planned
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/profitloss/ProfitLossClosingService.java:88-92`；`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/TestErpFinProfitLossClosing.java`（补 FX 场景）或 `TestErpFinPeriodCloseEndToEnd.java`（补汇兑损益归零断言）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: 本 fix plan 经独立 plan-audit 通过 + 人工确认（会计保护区域）

- [ ] 移除 `ProfitLossClosingService:89-90` 排除条件中的 `EXCHANGE_GAIN_LOSS`（保留 `PERIOD_CLOSE` 排除）。更新该处注释说明：仅排除 `PERIOD_CLOSE`（防结转凭证自身分录重复结转）；汇兑重估凭证的汇兑损益（费用类）余额须正常结转至本年利润。
      - Skill: `nop-backend-dev`
- [ ] 补测试证明：在 `TestErpFinProfitLossClosing` 或 `TestErpFinPeriodCloseEndToEnd` 增 FX 场景（外币 AR + periodEndRate 触发重估 + 汇兑损益科目 EXPENSE 类），断言结账后汇兑损益科目净额归零 + 本年利润含汇兑净额（收入−费用−成本−汇兑净额）。
      - Skill: `nop-backend-dev`
- [ ] 运行 finance 单模块测试 + 全量 `mvn clean install -DskipTests` + `mvn test`（0 failures），确认无回归。
      - Skill: none

Exit Criteria:

- [ ] `ProfitLossClosingService` 不再排除 `EXCHANGE_GAIN_LOSS`，仅排除 `PERIOD_CLOSE`
- [ ] FX 场景测试断言汇兑损益结转后归零 + 本年利润含汇兑净额
- [ ] 全量 `mvn clean install -DskipTests` + `mvn test` 绿色（0 failures）

### Phase 2 - 索引/矩阵状态回填

Status: planned
Targets: `docs/audits/arm-index.md`（P0-MA2-016 修复状态）；`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.2`（finance 列）
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成 + closure-audit 通过

- [ ] 回填 `arm-index.md` P0-MA2-016 修复状态为 `done (plan 2026-07-27-1949-arm-fix-p0-ma2-016)`。
      - Skill: none
- [ ] 回填 `audit-remediation-scope-and-dimension-matrix.md §2.2` finance 列 `⚠️(P0→fix-plan + P1)` → `⚠️(P1)`（P1-MA2-017..022 仍待 MR1）。
      - Skill: none

Exit Criteria:

- [ ] arm-index P0-MA2-016 状态 `done`
- [ ] scope matrix §2.2 finance 列反映 P0 已闭包

## Closure Gates

- [ ] 范围内行为完成（损益结转不再排除汇兑损益 + FX 场景测试证明归零）
- [ ] 相关文档对齐（owner doc §步骤5 描述与实现一致；arm-index + scope matrix 状态回填）
- [ ] 已运行验证：全量 `mvn clean install -DskipTests` + `mvn test`（0 failures）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立 plan-audit 已完成（实施前）
- [ ] **人工确认已获得**（会计保护区域 P0 即时修复，`project-context.md §AI 阻塞条件`）
- [ ] 独立 closure-audit 由独立子代理（新会话）执行
- [ ] 文本一致性已验证：状态、阶段、门控、日志都一致
- [ ] 结束证据存在于文件中

## Closure

Status Note: _（待人工确认 + 独立 plan-audit + closure-audit 后填充）_

Closure Audit Evidence:

- _（待独立子代理 closure-audit 填充）_

Follow-up:

- 已结账期间的残留汇兑损益余额是否补结转：MR1 裁决（不在本 fix plan 范围）
- P1-MA2-022（FX 无前期 reversal）MR1 裁决
- P1-MA2-017（auto-post-on-close 阻断分级）MR1 裁决
