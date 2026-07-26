# 2026-07-26-0500-3 C3 日期范围有效性校验钩子浏览器层 E2E

> Plan Status: active
> Mission: erp
> Work Item: C3 日期范围有效性 sales 定价 3 实体校验钩子浏览器层端到端验证
> Last Reviewed: 2026-07-26
> Source: 近期深化后端特性浏览器层验证缺口 —— C3 日期范围有效性模式 PRIORITY/STACKABLE helper 扩展 + sales 定价 3 实体接入（plan `2026-07-26-0315-1`）落地 3 BizModel `defaultPrepareSave/Update` 钩子，经 JUnit 覆盖（`TestErpDateRanges` 42 场景 + `TestErpSalDateRange` 10 场景 + master-data service 122 测试 + sales-service 135 测试），但**零浏览器层 E2E**。AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点。
> Related: `docs/plans/2026-07-26-0315-1-c3-priority-stackable-helpers-sales-rollout.md`（C3 helper + sales 接入）、`docs/plans/2026-07-21-2225-1-date-ranged-validity-pattern.md`（C3 基础设施 + 3 MUTEX 试点）、`docs/design/date-ranged-validity-pattern.md`（C3 owner doc）
> Audit: required

## Current Baseline

C3 日期范围有效性模式已落地（plan 2026-07-21-2225-1 基础设施 + plan 2026-07-26-0315-1 PRIORITY/STACKABLE 扩展 + sales 接入）：

**可复用组件**（`module-master-data/erp-md-{dao,service}/.../daterange/`，纯 Java 无 IoC 依赖，跨域经 `app-erp-master-data-service` compile 依赖可达）：
- `IDateRange` 接口（dao 层）— 2 getter 归一化 `validFrom/validTo` 与 `effectiveFrom/effectiveTo` 历史变体
- `ErpDateRanges` 纯函数工具类 — 5 原语：`contains` / `overlaps` / `effectiveOn` / `longestOverlap` / `pickHighestPriority`（PRIORITY，0315-1 新增）
- `ErpDateRangeOverlapValidator` — `enforceMutex`（MUTEX 互斥）+ `enforceStackableAware`（STACKABLE 混合策略，0315-1 新增）

**sales 定价 3 实体接入**（0315-1，`module-sales/erp-sal-service/.../entity/`）：

| 实体 | 策略 | 钩子 | 维度键 | 行为 |
|------|------|------|--------|------|
| `ErpSalPriceList` | PRIORITY | `defaultPrepareSave/Update` | customerGroupCode + partnerId | warn-only（`pickHighestPriority` 检测歧义，LOG.warn 不阻断） |
| `ErpSalPriceListLine` | MUTEX | `defaultPrepareSave/Update` | priceListId + materialId | 拒绝重叠（`enforceMutex` 抛 `ERR_SAL_PRICE_LIST_LINE_OVERLAP`） |
| `ErpSalPricingRule` | STACKABLE | `defaultPrepareSave/Update` | ruleType + targetType + materialId/materialCategoryId + customerGroupCode/partnerId | 混合策略（`enforceStackableAware` 双非 stackable 重叠抛 `ERR_SAL_PRICING_RULE_OVERLAP`） |

**TIMESTAMP 适配**（0315-1 实现偏离）：`ErpSalPricingRule.validFrom/validTo` 为 TIMESTAMP，ORM 生成基类 getter `final + Timestamp 返回类型` 与 `IDateRange.LocalDate` 不兼容。按 owner doc §8.1「跨域接入适配器」范式在 BizModel 内构造 `PricingRuleDateRange implements IDateRange` 适配器（`Timestamp.toLocalDateTime().toLocalDate()` 截断）。

**关键约束（CrudBizModel 钩子触发条件）**：`defaultPrepareSave/Update` 仅在 GraphQL `__save`/`__update` Map 入口走 EntityData 管道时触发（C3 基础设施 plan 2026-07-21-2225-1 关键发现）。浏览器层经 GraphQL `__save` 创建/更新实体可触发钩子，这与既有 `master-data.write.spec.ts`（plan 0628-2）写路径范式一致。

剩余差距：sales 定价 3 实体日期范围校验钩子经 JUnit 单层验证，但**全栈浏览器层路径未验证**——实体经 GraphQL `__save` 创建（含 validFrom/validTo 区间）→ 钩子触发 → MUTEX 拒绝/STACKABLE 混合策略/PRIORITY warn-only 三种行为可观测。

## Goals

- 验证 `ErpSalPriceListLine` MUTEX 策略：重叠区间 `__save` 抛 `ERR_SAL_PRICE_LIST_LINE_OVERLAP`（同 priceListId + materialId 维度），相邻日不重叠通过
- 验证 `ErpSalPricingRule` STACKABLE 混合策略：双非 stackable 重叠抛 `ERR_SAL_PRICING_RULE_OVERLAP`，任一方 stackable=true 允许重叠
- 验证 `ErpSalPriceList` PRIORITY warn-only：多份有效清单不阻断 `__save`（成功保存），warn 行为经日志不可直接断言（Non-Goal 像素级日志断言），仅验证保存成功 + 多份有效记录共存
- 验证 `__update`（更新自身排除）路径：更新既有记录的 validFrom/validTo 不与自身重叠通过

## Non-Goals

- **不接入 owner doc §10 其余 14 follow-up 实体**（purchase/hr/crm/aps/mfg/logistics）—— 各域独立触发条件未满足，归 successor
- **不做 PRIORITY helper 运行时取值接线**（即不把 pickHighestPriority 接入 ErpSalCustomerPriceResolver）—— 0315-1 Deferred（属不同结果面）
- **不做像素级日志断言**（PRIORITY warn-only 的 LOG.warn 不可经浏览器层直接断言）—— 仅验证保存成功
- **不做物化视图/反向索引**（C3 Deferred，触发：单实体有效记录数 > 10K 且 P95 > 200ms）
- **不做 helper 下沉到独立 erp-common-dao 模块**（C3 Deferred，触发：跨域接入数 > 3）
- **不改变 sales 定价引擎取价逻辑**（0315-1 Non-Goal 复述）
- 生产 Java/ORM/契约/codegen/字典/种子变更 —— 纯测试 + 文档

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/date-ranged-validity-pattern.md`（§PRIORITY 策略 + §STACKABLE 混合策略 + §sales 接入记录 + §反模式自检表）、`docs/design/sales/README.md`（§日期范围有效性校验 C3 交叉引用）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层 E2E + GraphQL `__save` 写路径 + 错误响应断言范式，对齐 `master-data.write.spec.ts` 写路径先例）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（C3 钩子为保存时无条件校验，非 config-gated；fresh-DB H2 + 既有 webServer 启动链）。

## Execution Plan

### Phase 1 - Explore（3 实体 __save 最小字段集 + 区间字段 + 错误响应结构核实）

Status: planned
Targets: `module-sales/model/app-erp-sales.orm.xml`（3 实体字段）、`module-sales/erp-sal-service/.../entity/ErpSalPriceListBizModel.java`、`ErpSalPriceListLineBizModel.java`、`ErpSalPricingRuleBizModel.java`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: 无

- [ ] Proof: 核实 `ErpSalPriceList` 经 GraphQL `__save` 创建所需最小必填字段集（code/name/orgId/validFrom/validTo/customerGroupCode/partnerId/priority），确认 `__save` Map 入口触发 `defaultPrepareSave` 钩子
- [ ] Proof: 核实 `ErpSalPriceListLine` 经 GraphQL `__save` 创建所需最小必填字段集（priceListId/materialId/validFrom/validTo），确认 MUTEX 维度键 priceListId + materialId
- [ ] Proof: 核实 `ErpSalPricingRule` 经 GraphQL `__save` 创建所需最小必填字段集（code/name/orgId/ruleType/targetType/validFrom/validTo/priority/stackable），确认 STACKABLE 维度键 + `PricingRuleDateRange` 适配器截断行为
- [ ] Proof: 核实错误响应结构（Nop GraphQL 错误体含 `errors[].message` 中文描述 + ErrorCode token），确定断言匹配范式（对齐 `hr-leave-attendance.action.spec.ts:130-144` 日期重叠拒绝 `__save` + errors 含「重叠」token 断言先例）

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [ ] Explore 笔记记录 3 实体最小字段集 + 维度键 + 错误响应结构（写入 plan Execution Decision 段，不新建独立文档）

### Phase 2 - spec 实现（MUTEX 拒绝 + STACKABLE 混合 + PRIORITY warn-only + 控制对照）

Status: planned
Targets: `tests/e2e/business-actions/sal-date-range-validation.action.spec.ts`（NEW）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: 新建 `sal-date-range-validation.action.spec.ts`，经 GraphQL `__save` 直接驱动 3 实体创建/更新（不经 AMIS 表单层，对齐 `master-data.write.spec.ts` GraphQL 写路径范式）。setup 自包含建 priceList/material 前置（测试专用，避免种子污染）
      - Skill: `nop-testing`
- [ ] Proof: (1) **ErpSalPriceListLine MUTEX 拒绝** —— 同 priceListId + materialId 建第一行 [2026-01-01, 2026-12-31] 成功 → 建第二行重叠区间 [2026-06-01, 2027-06-30] → 抛 `ERR_SAL_PRICE_LIST_LINE_OVERLAP`（GraphQL errors 含「重叠」语义 token，对齐 `hr-leave-attendance.action.spec.ts:130-144` 日期重叠拒绝断言范式）→ 相邻日 [2027-01-01, 2027-12-31] 通过；(2) **ErpSalPricingRule STACKABLE 混合** —— 同维度建 stackable=false 规则 [2026-01-01, 2026-12-31] → 建第二 stackable=false 重叠 → 抛 `ERR_SAL_PRICING_RULE_OVERLAP` → 建 stackable=true 重叠 → 通过（允许叠加）；(3) **ErpSalPriceList PRIORITY warn-only** —— 同维度建两份有效清单（**相同 priority**，触发 `top == next` 歧义检测路径 `ErpSalPriceListBizModel:83-87`）→ 两份均 `__save` 成功（warn 不阻断）→ `__get` 断言两份记录共存（证明歧义代码路径在浏览器层不意外抛异常）；(4) **__update 自身排除** —— 更新第一行的 validTo → 不与自身重叠通过
      - Skill: `nop-testing`
- [ ] Add: cleanup 清理测试专用 priceList/material/pricingRule（经实体 `__delete`）
      - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [ ] `sal-date-range-validation.action.spec.ts` 全绿，断言 MUTEX 拒绝 + STACKABLE 混合 + PRIORITY warn-only + __update 自身排除四组可观察结果

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: planned
Targets: `docs/design/date-ranged-validity-pattern.md`（§浏览器层验证实现注记）、`docs/testing/e2e-runbook.md`（业务动作表 + sales 日期范围校验行）
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 2

- [ ] Add: `date-ranged-validity-pattern.md` 增「浏览器层验证」实现注记（GraphQL `__save` 写路径触发钩子 + MUTEX/STACKABLE/PRIORITY 三策略断言范式 + 错误响应 token 匹配）
- [ ] Add: `e2e-runbook.md` 业务动作表 +sales 日期范围校验行（3 实体 × 3 策略）

Exit Criteria:

- [ ] owner doc + runbook 更新落地（仅此阶段实际更改 owner 行为文档）

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_064300161ffe9iZE2YizGU0jky`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-26）— 0 Blocker / 0 Major / 3 Minor。全部负载事实经实时仓库逐项核实**精确匹配**（3 BizModel hooks 行号 + 3 ErrorCode 行号 + PricingRuleDateRange 适配器 + PRIORITY warn-only `top==next` 路径 + MUTEX enforceMutex 抛 + STACKABLE enforceStackableAware 抛 + 零浏览器 E2E + __save 写路径先例 ✓）。格式合规 + 范围纪律通过（单结果面 / Exit Criteria 阶段本地化 / 无 ORM 保护区域）。PRIORITY warn-only 不可断言为诚实边界声明非 cop-out。**Minor**：(1) 缺 Draft Review Record 段——本次新增 ✓；(2) Phase 2 Proof (3) PRIORITY 测试应用**相同 priority**（触发 `top==next` 歧义检测路径 `:83-87`）非不同 priority（不同 priority 走 trivial clean branch 不触达歧义代码）——已修订 ✓；(3) 更近先例 `hr-leave-attendance.action.spec.ts:130-144` 日期重叠拒绝 `__save` 断言范式比 `master-data.write.spec.ts` 更直接——Phase 1 + Phase 2 引用已更新 ✓；(4) CrudBizModel 钩子触发排他性归因略过强（base plan 记录可达性非排他性）——不阻塞执行（仅需可达性已文档化）保留给结束审计。计划为可接受的执行契约。

## Closure Gates

> 本计划无 ORM/契约/字典/config 变更。纯测试 + 文档。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ sales service `mvn test`（确认既有 135 测试 0 回归）。

- [ ] 范围内行为完成（MUTEX 拒绝 + STACKABLE 混合 + PRIORITY warn-only + __update 自身排除四组断言全绿）
- [ ] 相关文档对齐（date-ranged-validity-pattern.md + e2e-runbook）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/business-actions/sal-date-range-validation.action.spec.ts` 全绿 + sales 既有 spec 回归 0 新增失败）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### owner doc §10 其余 14 follow-up 实体

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0315-1 Deferred —— 各域独立触发条件未满足（purchase/hr/crm/aps/mfg/logistics）。本计划仅覆盖触发条件已满足的 sales 3 实体浏览器层验证。
- Successor Required: `yes`（触发条件：各域业务流程细化 + 该域 service 接入需求 + 浏览器层验证推进时）

### PRIORITY helper 运行时取值接线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0315-1 Deferred —— PRIORITY helper 仅为保存校验层工具；运行时取价排序由 `ErpSalCustomerPriceResolver` 独有逻辑承担，接线属不同结果面
- Successor Required: `yes`（触发条件：定价引擎需统一日期范围优先级解析层时）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理填写>
- Evidence: <待填写>

Follow-up:

- owner doc §10 其余 14 实体接入 + PRIORITY helper 运行时取值接线（触发条件见上 Deferred But Adjudicated 段，非阻塞）
