# 2026-07-26-1407-2-budget-rollforward-carryforward-browser-e2e 预算滚动复制 + 结转浏览器层 E2E

> Plan Status: active
> Last Reviewed: 2026-07-26
> Source: `docs/backlog/deepening-roadmap.md` §8.4 A2（落地证据声明 JUnit 单层验证，浏览器层未提及）
> Related: `2026-07-21-1206-2-finance-budget-multi-year-carryforward.md`（A2 后端落地）/ `2026-07-26-0410-2-commitment-accounting-browser-e2e.md`（A2 承付子集浏览器层，本计划同 owner doc 不同结果面）
> Audit: required

## Current Baseline

- A2 预算多年度 / 结转 / 承付后端已落地（plan 2026-07-21-1206-2）：`ErpFinBudgetScenarioBizModel.rollForward(@BizMutation)` + `carryForward(@BizMutation)` 委派 `ErpFinBudgetScenarioProcessor`；config-gated `erp-fin.budget-roll-forward-enabled` + `erp-fin.budget-carry-forward-enabled` 默认 false。
- JUnit 单层验证齐备：`TestErpFinBudgetRollForward` 3 策略全绿（FIXED_PERCENTAGE 100% 复制 1000 / ZERO_BASED 仅结构金额清零 / INCREMENTAL 5% 上调）+ `TestErpFinBudgetCarryForward` 4 规则全绿（REMAINING_FULL=600 / REMAINING_RATIO 50%=300 / USED_FULL=400 / NONE=0）。
- 浏览器层覆盖现状：`fin-budget-scenario.action.spec.ts`（plan 0814-2）仅覆盖预算方案生命周期 submit/approve/reject/cancel + BUDGET 影子凭证行数值断言；`fin-budget-control.action.spec.ts`（1218-2）覆盖预算控制 hook。**零浏览器层覆盖** `rollForward`/`carryForward` 两入口。
- config 现状：`erp-fin.budget-roll-forward-enabled` + `erp-fin.budget-carry-forward-enabled` **均未在** `playwright.config.ts` webServer JVM args 启用（默认 false）。
- `ErpFinBudgetRollforwardLog` + `ErpFinBudgetCarryForwardLog` 两审计实体 JUnit 断言写入（`countRollforwardLogs >= 1`）。
- 剩余差距：两入口 @BizMutation 经 GraphQL 全栈可达但浏览器层无验证；需启用两 config-gate 方能触达。

## Goals

- 为 A2 预算滚动复制 + 结转两入口补全栈浏览器层 E2E 覆盖，收口「JUnit 单层验证但零浏览器层 E2E」缺口。
- 验证 `rollForward` 3 策略（FIXED_PERCENTAGE 100% / ZERO_BASED 金额清零 / INCREMENTAL 上调）+ `carryForward` 4 规则（REMAINING_FULL / REMAINING_RATIO / USED_FULL / NONE）经 GraphQL `/graphql` 端到端可达 + 目标方案 DRAFT 状态 + parentScenarioId 回链 + 新方案行金额确定性派生断言 + RollforwardLog/CarryForwardLog 审计写入。
- owner doc `docs/design/finance/budget.md` §滚动预算自动复制引擎 + §结转规则引擎 增「浏览器层验证」实现注记。

## Non-Goals

- commitment 跨年度结转（0410-2 Deferred「跨年度 commitment 余额处理需求」未触发，归 successor 不变）。
- 预算物化快照表 / 预算冻结解冻多级控制 / 预算编制工作流（A2 Deferred successor，触发条件未满足）。
- 承付会计浏览器层（已由 0410-2 落地，非本计划范围）。
- 生产代码变更（纯测试 + config + 文档）。

## Task Route

- Type: `verification or audit work`（浏览器层 E2E 验证补全，零生产代码变更）
- Owner Docs: `docs/design/finance/budget.md`（§滚动预算自动复制引擎 / §结转规则引擎 / §版本审计链）
- Skill Selection Basis: 匹配 `nop-testing`（Playwright 浏览器层 E2E + 既有 business-actions/_helper 复用 + config-gated 特性 webServer JVM arg 启用范式 + 自包含 setup 经 GraphQL __save 建前置场景），对齐 0410-2 / 0500-2 同型先例。

## Infrastructure And Config Prereqs

- webServer JVM arg 追加 `-Derp-fin.budget-roll-forward-enabled=true` + `-Derp-fin.budget-carry-forward-enabled=true`（启用两入口 config-gate；默认 false）。
- `erp-fin.budget-carry-forward-default-rule` config 可选（若 carryForward 不传 rule 参数走默认；spec 显式传 rule 覆盖，故默认 rule 非必需）。
- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - Explore + 自包含 setup 可达性核实

Status: planned
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinBudgetScenarioBizModel.java`, `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetScenarioProcessor.java`, `tests/e2e/business-actions/_helper.ts`
Skill: `nop-testing`

- Item Types: `Decision | Proof`
- Prereqs: 无（独立计划）

- [ ] Proof: 核实 `rollForward(id, newFiscalYear, strategy)` 入参字段集 + 目标方案字段翻转（fiscalYear/parentScenarioId/docStatus=DRAFT）+ 新方案行金额派生（FIXED_PERCENTAGE/ZERO_BASED/INCREMENTAL 三策略公式），记录文件行号锚点。
  - Skill: `nop-testing`
- [ ] Proof: 核实 `carryForward(id, targetScenarioId, rule)` 入参字段集 + 目标方案行金额派生（REMAINING_FULL/REMAINING_RATIO/USED_FULL/NONE 四规则公式 + ratio config）+ 守卫（源方案 APPROVED / 目标方案存在 / period 匹配），记录行号锚点。
  - Skill: `nop-testing`
- [ ] Decision: 自包含 setup 策略裁决——经 GraphQL `ErpFinBudgetScenario__save` + `ErpFinBudgetLine__save` + `approve` 建源方案（镜像 JUnit `seedApprovedScenario` + 行），记录最小字段集（code/name/orgId/periodId/fiscalYear/docStatus=APPROVED + 行 subjectId/amount）。记录 GraphQL 入参形态裁决。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 setup 可达性核实 + 策略裁决，解除 Phase 2 实施阻塞。

- [ ] 两入口入参/派生/守卫逻辑行号锚点 + setup 策略裁决落盘 plan Execution Decision 段

### Phase 2 - spec 实现 + webServer config 启用

Status: planned
Targets: `tests/e2e/business-actions/fin-budget-rollforward-carryforward.action.spec.ts`, `tests/e2e/business-actions/_helper.ts`, `playwright.config.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: `playwright.config.ts` webServer JVM args 追加 `-Derp-fin.budget-roll-forward-enabled=true -Derp-fin.budget-carry-forward-enabled=true`。
  - Skill: `none`
- [ ] Add: `_helper.ts` 新增 `findBudgetScenarioByCode(page, code)` + `findBudgetLineAmount(page, scenarioId, subjectCode)` + `countBudgetRollforwardLogs(page, scenarioId)` / `countBudgetCarryForwardLogs(page, scenarioId)` 反查原语（对齐既有范式）。
  - Skill: `nop-testing`
- [ ] Add: 新建 `fin-budget-rollforward-carryforward.action.spec.ts`（用例覆盖 rollForward 3 策略 + carryForward 4 规则，按 JUnit 场景镜像）：rollForward FIXED_PERCENTAGE（100% 复制，行金额=源）+ ZERO_BASED（行金额清零=0）+ INCREMENTAL（按 ratio 上调）；carryForward REMAINING_FULL（预算-实际=剩余）+ REMAINING_RATIO（剩余×ratio）+ USED_FULL（实际额）+ NONE（0）。断言目标方案 fiscalYear/parentScenarioId/docStatus=DRAFT + 新方案行金额确定性派生 + RollforwardLog/CarryForwardLog 审计写入。状态/字段翻转均经 `verifyState`/`findBudgetLineAmount` `__get` 独立断言。
  - Skill: `nop-testing`
- [ ] Proof: 运行新 spec 全绿 + 既有 `fin-budget-scenario.action.spec.ts` + `fin-budget-control.action.spec.ts` + `fin-budget-vs-actual.value.spec.ts` 回归 0 新增失败（config 启用对既有预算链路零回归）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 spec 全绿 + config-gate 启用 + 回归零新增失败，解除 Phase 3 owner-doc 对齐阻塞。

- [ ] spec 全绿（指定成功 + 失败模式：3 策略 + 4 规则行金额确定性派生断言 + 守卫）
- [ ] 既有预算 spec 回归 0 新增失败（config-gate 启用对既有链路零回归）

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: planned
Targets: `docs/design/finance/budget.md`, `docs/testing/e2e-runbook.md`, `docs/logs/2026/07-26.md`
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: Phase 2

- [ ] Add: `docs/design/finance/budget.md` §滚动预算自动复制引擎 + §结转规则引擎 增「浏览器层验证」实现注记（自包含 setup 经 __save 建源方案 + 3 策略/4 规则行金额派生断言范式 + config-gate 启用 + RollforwardLog/CarryForwardLog 审计写入断言）。
  - Skill: `none`
- [ ] Add: `docs/testing/e2e-runbook.md` webServer JVM arg 段补两 `budget-*-forward-enabled=true` + 业务动作表新增 finance 预算滚动复制 + 结转行 + spec 计数增量。
  - Skill: `none`
- [ ] Add: `docs/logs/2026/07-26.md` 追加本计划日志条目（任务/Phase 摘要/验证 full-green/Skill）。
  - Skill: `none`

Exit Criteria:

> 本阶段交付 owner-doc 对齐 + 日志。完整仓库验证属 Closure Gates。

- [ ] owner doc + e2e-runbook + 日志三处更新落地

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_062f3d7a6ffeVCz6GIYmBkyJe5) — baseline 全部经实时仓库核实（rollForward/carryForward 方法签名 + Processor config-gate 默认 false + JUnit 3 策略/4 规则精确数值 + 浏览器层零覆盖 grep 确认）；模板/规则合规；无阻塞项。可直接进入实施。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [ ] 范围内行为完成（spec 全绿，覆盖 rollForward 3 策略 + carryForward 4 规则）
- [ ] 相关文档对齐（budget.md + e2e-runbook + 日志）
- [ ] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 新 spec 全绿 + 既有预算 spec 回归 0 新增失败（纯测试 + config + 文档，零生产代码变更）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### commitment 跨年度结转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0410-2 Deferred —— commitment 一并结转语义复杂，归 successor。
- Successor Required: `yes`（触发条件：跨年度 commitment 余额处理需求）

### 预算物化快照表 / 预算编制工作流 / 预算冻结解冻多级控制

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2 Deferred successor —— 触发条件均未满足（物化快照表需性能瓶颈需求 / 编制工作流需业务流程授权 / 冻结解冻需多级控制需求）。
- Successor Required: `yes`（各独立触发条件）

## Closure

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立审计>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- 无非阻塞跟进项（已确认的缺陷不得出现在此处）
