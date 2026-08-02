# 2026-07-26-0500-3 C3 日期范围有效性校验钩子浏览器层 E2E

> Plan Status: completed
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

Status: completed
Targets: `module-sales/model/app-erp-sales.orm.xml`（3 实体字段）、`module-sales/erp-sal-service/.../entity/ErpSalPriceListBizModel.java`、`ErpSalPriceListLineBizModel.java`、`ErpSalPricingRuleBizModel.java`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: 无

- [x] Proof: 核实 `ErpSalPriceList` 经 GraphQL `__save` 创建所需最小必填字段集（code/name/orgId/validFrom/validTo/customerGroupCode/partnerId/priority），确认 `__save` Map 入口触发 `defaultPrepareSave` 钩子
- [x] Proof: 核实 `ErpSalPriceListLine` 经 GraphQL `__save` 创建所需最小必填字段集（priceListId/materialId/validFrom/validTo），确认 MUTEX 维度键 priceListId + materialId
- [x] Proof: 核实 `ErpSalPricingRule` 经 GraphQL `__save` 创建所需最小必填字段集（code/name/orgId/ruleType/targetType/validFrom/validTo/priority/stackable），确认 STACKABLE 维度键 + `PricingRuleDateRange` 适配器截断行为
- [x] Proof: 核实错误响应结构（Nop GraphQL 错误体含 `errors[].message` 中文描述 + ErrorCode token），确定断言匹配范式（对齐 `hr-leave-attendance.action.spec.ts:130-144` 日期重叠拒绝 `__save` + errors 含「重叠」token 断言先例）

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] Explore 笔记记录 3 实体最小字段集 + 维度键 + 错误响应结构（写入 plan Execution Decision 段，不新建独立文档）

#### Execution Decisions（Phase 1 Explore 笔记）

经实时仓库核实（`module-sales/model/app-erp-sales.orm.xml:1005-1164` + 3 BizModel.java + `ErpSalErrors.java:241-258` + `ErpDateRangeOverlapValidator.java` + dict yaml）：

1. **字段名修正（plan 基线笔误，执行期纠正）**：
   - `ErpSalPriceList` 最小必填字段 = `code`(mandatory) + `name`(mandatory)；可选 `customerGroupCode`/`partnerId`/`validFrom`/`validTo`/`priority`(default 100)。**无 `orgId` 列**（plan 基线误列 orgId）。
   - `ErpSalPriceListLine` 最小必填 = `priceListId`(mandatory) + `unitPrice`(mandatory)；可选 `materialId`/`validFrom`/`validTo`。**无 orgId**。
   - `ErpSalPricingRule` 最小必填 = `ruleCode`(mandatory, 即 plan 所述 code) + `ruleName`(mandatory, 即 plan 所述 name) + `ruleType`(mandatory, dict `erp-sal/pricing-rule-type`: PERCENT_DISCOUNT/AMOUNT_OFF/GIFT/PRICE_OVERRIDE) + `targetType`(mandatory, dict `erp-sal/pricing-target`: LINE/ORDER)；可选 `materialId`/`materialCategoryId`/`customerGroupCode`/`partnerId`/`priority`(default 100)/`stackable`(default false)/`validFrom`/`validTo`(**TIMESTAMP** 非 DATE)。**无 orgId / 无 code / 无 name 列**（用 ruleCode/ruleName）。

2. **区间字段类型**：
   - `ErpSalPriceList.validFrom/validTo` = `stdSqlType=DATE`（GraphQL 传 ISO 日期 `"2026-01-01"`）
   - `ErpSalPriceListLine.validFrom/validTo` = `DATE`（同上）
   - `ErpSalPricingRule.validFrom/validTo` = `TIMESTAMP`（`tagSet=clock`，GraphQL 传 ISO 时间戳 `"2026-01-01T00:00:00"`；`PricingRuleDateRange` 适配器 `Timestamp.toLocalDateTime().toLocalDate()` 截断到 LocalDate）

3. **维度键**（来自 3 BizModel 的 QueryBean filter + owner doc §10）：
   - PriceListLine MUTEX：`priceListId + materialId`（`ErpSalPriceListLineBizModel:57-58`）
   - PricingRule STACKABLE：`ruleType + targetType + materialId + materialCategoryId + customerGroupCode + partnerId`（`ErpSalPricingRuleBizModel:73-78`）
   - PriceList PRIORITY：`customerGroupCode + partnerId`（`ErpSalPriceListBizModel:70-71`）

4. **钩子触发确认**：3 BizModel 均 override `defaultPrepareSave/Update`，CrudBizModel `__save`/`__update` Map 入口经 EntityData 管道触发钩子（对齐基线 §30 既有发现，非排他性归因）。

5. **错误响应结构 + 断言 token 决策**：Nop GraphQL 业务异常封装为 `{data:null, errors:[{message}]}`。Phase 1 运行期实测（Phase 2 落地后）：**GraphQL error envelope 仅 surface `message` 字段**，`extensions.nopError.errorCode` 路径未在浏览器层 `/graphql` 响应中暴露（`JSON.stringify(errors)` 仅含 `[{message:"..."}]`）。两个 ErrorCode 中文描述实测：
   - `ERR_SAL_PRICE_LIST_LINE_OVERLAP`（`erp.err.sal.price-list-line.overlap`）message = "价格清单行 ErpSalPriceListLine 在区间 [...] 内与既有记录 id=... **冲突**（同 priceListId+materialId 维度 MUTEX 策略）"
   - `ERR_SAL_PRICING_RULE_OVERLAP`（`erp.err.sal.pricing-rule.overlap`）message = "促销规则 PricingRuleDateRange 在区间 [...] 内与既有不可叠加规则 id=... **冲突**（STACKABLE 混合策略：双方均 stackable=false 时互斥）"
   - **Decision（执行期纠正）**：message 实测含「**冲突**」非「重叠」（plan 基线 + hr-leave 先例用「重叠」token 不适用于 sales ErrorCode 描述——hr-leave 用独立 ErrorCode `ERR_LEAVE_DATE_OVERLAP` 描述含「重叠」，sales 两码描述含「冲突」）。断言匹配采用中文「**冲突**」token（实测唯一稳定可达 token，对齐 hr-leave 先例「errors 含语义中文 token」范式，仅 token 字面从「重叠」适配为 sales 实测的「冲突」）。

6. **PRIORITY warn 路径触达性分析（执行期发现，影响 Phase 2 Proof (3) 用例数）**：`ErpSalPriceListBizModel.warnIfPriorityAmbiguous:73` 早返回条件 `sameDimension.size() < 2`，而 `dao().findAllByQuery` 在 `defaultPrepareSave`（**持久化前**）执行，候选记录尚未落库 → 不在结果集。故顺序 `__save` 第 1 份查到 `[]`（早返回）、第 2 份查到 `[#1]`（effective size=1 早返回 `:77`），均**未触达** `:83-87 top==next` 歧义分支。要真正触达歧义代码路径需 **3 份**同维度同优先级有效清单（第 3 份 save 时 `[#1,#2]` 已落库 → effective size=2 → sort → top==next → LOG.warn）。Non-Goal 已声明「不做像素级日志断言」，核心可观察断言仍为「保存成功 + 记录共存」；Phase 2 Proof (3) 扩展为 3 份以最大化歧义代码路径覆盖（不偏离 plan 意图，仅从 2→3 强化路径触达）。

### Phase 2 - spec 实现（MUTEX 拒绝 + STACKABLE 混合 + PRIORITY warn-only + 控制对照）

Status: completed
Targets: `tests/e2e/business-actions/sal-date-range-validation.action.spec.ts`（NEW）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 新建 `sal-date-range-validation.action.spec.ts`，经 GraphQL `__save` 直接驱动 3 实体创建/更新（不经 AMIS 表单层，对齐 `master-data.write.spec.ts` GraphQL 写路径范式）。setup 自包含建 priceList/material 前置（测试专用，避免种子污染）
      - Skill: `nop-testing`
- [x] Proof: (1) **ErpSalPriceListLine MUTEX 拒绝** —— 同 priceListId + materialId 建第一行 [2026-01-01, 2026-12-31] 成功 → 建第二行重叠区间 [2026-06-01, 2027-06-30] → 抛 `ERR_SAL_PRICE_LIST_LINE_OVERLAP`（GraphQL errors 含「重叠」语义 token，对齐 `hr-leave-attendance.action.spec.ts:130-144` 日期重叠拒绝断言范式）→ 相邻日 [2027-01-01, 2027-12-31] 通过；(2) **ErpSalPricingRule STACKABLE 混合** —— 同维度建 stackable=false 规则 [2026-01-01, 2026-12-31] → 建第二 stackable=false 重叠 → 抛 `ERR_SAL_PRICING_RULE_OVERLAP` → 建 stackable=true 重叠 → 通过（允许叠加）；(3) **ErpSalPriceList PRIORITY warn-only** —— 同维度建两份有效清单（**相同 priority**，触发 `top == next` 歧义检测路径 `ErpSalPriceListBizModel:83-87`）→ 两份均 `__save` 成功（warn 不阻断）→ `__get` 断言两份记录共存（证明歧义代码路径在浏览器层不意外抛异常）；(4) **__update 自身排除** —— 更新第一行的 validTo → 不与自身重叠通过
      - Skill: `nop-testing`
- [x] Add: cleanup 清理测试专用 priceList/material/pricingRule（经实体 `__delete`）
      - Skill: `nop-testing`

#### Execution Decisions（Phase 2 落地注记）

- **实测 4 用例全绿**（`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test sal-date-range-validation.action.spec.ts`，4 passed 28.7s）。
- **partnerId FK 校验**：3 实体 `partnerId` 均有 FK 到 `ErpMdPartner`，`__save` 校验存在性——plan 基线假设 `partnerId: 0` 不可达（partner id=0 不存在抛「往来单位 id=0 记录不存在」），改用种子 CUSTOMER id=1（`SEED.CUSTOMER`）满足 FK + 维度键一致性。
- **拒绝断言 token 适配**：GraphQL error envelope 仅 surface `message`（无 errorCode 路径），断言改用 message 实测中文 token「冲突」（详见 Phase 1 Execution Decisions §5 修订）。
- **PRIORITY 用例 2→3 份**：详见 Phase 1 Execution Decisions §6（第 3 份 save 时 [#1,#2] 已落库 → 真正触达 `:73-87` effective size≥2 + top==next 歧义分支）。Non-Goal 边界不变（不做像素级日志断言，仅断言 3 份保存成功 + `__get` 共存）。
- **materialId 复用种子** `MAT_1=1`（MAT-001），不新建测试物料（plan Proof (1) 维度键 priceListId+materialId 命中即可，material FK 已满足）。

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] `sal-date-range-validation.action.spec.ts` 全绿，断言 MUTEX 拒绝 + STACKABLE 混合 + PRIORITY warn-only + __update 自身排除四组可观察结果

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: completed
Targets: `docs/design/date-ranged-validity-pattern.md`（§浏览器层验证实现注记）、`docs/testing/e2e-runbook.md`（业务动作表 + sales 日期范围校验行）
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] Add: `date-ranged-validity-pattern.md` 增「浏览器层验证」实现注记（GraphQL `__save` 写路径触发钩子 + MUTEX/STACKABLE/PRIORITY 三策略断言范式 + 错误响应 token 匹配）
- [x] Add: `e2e-runbook.md` 业务动作表 +sales 日期范围校验行（3 实体 × 3 策略）

Exit Criteria:

- [x] owner doc + runbook 更新落地（仅此阶段实际更改 owner 行为文档）

#### Execution Decisions（Phase 3 落地注记）

- `date-ranged-validity-pattern.md` 新增 §7 子节「浏览器层验证实现注记」（位于 sales 接入记录之后、§8 反模式自检表之前），覆盖写路径触发 + 三策略断言范式 + 错误响应 token 匹配（「冲突」非「重叠」）+ PRIORITY warn 路径触达性（≥3 份）。
- `e2e-runbook.md` 业务动作表追加 sales C3 日期范围校验行（位于 drp-simulation 行之后、调用范式段之前）。
- runbook header 业务动作 spec 聚合计数（94/63）与实际（106）的漂移为前序计划遗留，超出本计划范围（Phase 3 Exit Criteria「仅此阶段实际更改 owner 行为文档」），不改。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_064300161ffe9iZE2YizGU0jky`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-26）— 0 Blocker / 0 Major / 3 Minor。全部负载事实经实时仓库逐项核实**精确匹配**（3 BizModel hooks 行号 + 3 ErrorCode 行号 + PricingRuleDateRange 适配器 + PRIORITY warn-only `top==next` 路径 + MUTEX enforceMutex 抛 + STACKABLE enforceStackableAware 抛 + 零浏览器 E2E + __save 写路径先例 ✓）。格式合规 + 范围纪律通过（单结果面 / Exit Criteria 阶段本地化 / 无 ORM 保护区域）。PRIORITY warn-only 不可断言为诚实边界声明非 cop-out。**Minor**：(1) 缺 Draft Review Record 段——本次新增 ✓；(2) Phase 2 Proof (3) PRIORITY 测试应用**相同 priority**（触发 `top==next` 歧义检测路径 `:83-87`）非不同 priority（不同 priority 走 trivial clean branch 不触达歧义代码）——已修订 ✓；(3) 更近先例 `hr-leave-attendance.action.spec.ts:130-144` 日期重叠拒绝 `__save` 断言范式比 `master-data.write.spec.ts` 更直接——Phase 1 + Phase 2 引用已更新 ✓；(4) CrudBizModel 钩子触发排他性归因略过强（base plan 记录可达性非排他性）——不阻塞执行（仅需可达性已文档化）保留给结束审计。计划为可接受的执行契约。

## Closure Gates

> 本计划无 ORM/契约/字典/config 变更。纯测试 + 文档。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ sales service `mvn test`（确认既有 135 测试 0 回归）。

- [x] 范围内行为完成（MUTEX 拒绝 + STACKABLE 混合 + PRIORITY warn-only + __update 自身排除四组断言全绿）
- [x] 相关文档对齐（date-ranged-validity-pattern.md + e2e-runbook）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/business-actions/sal-date-range-validation.action.spec.ts` 全绿 + sales 既有 spec 回归 0 新增失败）—— 独立审计复核：spec `--list` 4 tests 可发现 + 断言模式健全（saveRaw 拒绝路径 + createViaSave 成功路径 + verifyState/__get 独立断言），执行者自报 mvn 154 模块 + 4 passed 28.7s 与计划记录一致
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

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

Status Note: 三阶段全绿交付——Phase 1 Explore 笔记内嵌 plan；Phase 2 新建 `sal-date-range-validation.action.spec.ts`（4 用例：MUTEX 拒绝+相邻日通过 / STACKABLE 双非拒绝+stackable=true 通过 / PRIORITY warn-only 3 份共存 / __update 自身排除）；Phase 3 owner doc 增「浏览器层验证实现注记」+ e2e-runbook +sales 行 + deepening-roadmap §8.6 浏览器层验证条目。

Closure Audit Evidence:

- Auditor / Agent: independent general closure-audit subagent (fresh session, ses_independent-closure-audit-2026-07-26)
- Evidence: 冷重播独立审计，逐项核实实时仓库（不信计划自报）—— **Phase 2 spec**：`tests/e2e/business-actions/sal-date-range-validation.action.spec.ts` 287 行 4 用例真实存在，`npx playwright test --list` 4 tests 可发现编译通过（spec:107/142/208/259）；4 用例与 3 BizModel 源码精确对齐——(1) MUTEX 维度 `priceListId+materialId` 匹配 `ErpSalPriceListLineBizModel.java:57-58`，`enforceMutex(...,entity.getId())` 自身排除路径 `:59-63`；spec 用 `saveRaw` 直取 `{data,errors}` 绕过 `createViaSave` 内置 `errors==null` 断言 + 「冲突」语义 token 匹配（执行期纠正「重叠」为「冲突」对齐 sales ErrorCode 描述）+ 相邻日通过对照；(2) STACKABLE 维度 6 键匹配 `ErpSalPricingRuleBizModel.java:73-78`，`enforceStackableAware(...,entity.getId(),PricingRuleDateRange::getStackable)` `:88-93`，TIMESTAMP 区间经 ISO 时间戳 + `PricingRuleDateRange` 适配器（`:100-130` `Timestamp.toLocalDateTime().toLocalDate()` 截断）；(3) PRIORITY warn-only 维度 `customerGroupCode+partnerId` 匹配 `ErpSalPriceListBizModel.java:70-71`，`warnIfPriorityAmbiguous:73` `sameDimension.size()<2` 早返回 + `:83-87 top==next` LOG.warn-only 不抛异常；spec 扩展 3 份（非 plan 原 2 份）真正触达 effective.size()≥2 歧义分支（执行期发现记录于 Phase 1 §6）+ `__get` 断言记录共存；(4) __update 经 `gql.raw` 直驱 `__update` mutation，缩窄 validTo 触发 `defaultPrepareUpdate` → `enforceMutex(selfId=entity.getId())` 排除自身通过。**Phase 3 docs** 全部落地：`docs/design/date-ranged-validity-pattern.md:288` §7「浏览器层验证实现注记（plan 2026-07-26-0500-3）」+ `docs/testing/e2e-runbook.md:330` sales C3 日期范围行 + `docs/backlog/deepening-roadmap.md:290` §8.6 浏览器层验证 bullet（含 ✅ done 标记）。**范围纪律**（`git status --short` + `git diff --stat`）：仅 5 modified docs（plan 自身 + date-ranged-validity-pattern.md + e2e-runbook.md + deepening-roadmap.md + logs/2026/07-26.md）+ 1 新增 untracked spec 文件；**零生产 Java/ORM/契约/字典/config 变更**——纯测试+文档，与 plan Non-Goals §49 声明一致。**断言模式健全性**：拒绝路径 `expect(errors).toBeTruthy()` + `JSON.stringify(errors)` 包含语义中文 token；成功路径 `createViaSave` + `expect(id).toBeTruthy()`；状态确认 `verifyState`/`__get` 独立断言；cleanup `try/finally` 经 `deleteById` 逐域清理保护共享 DB 基线。**计划内部一致性**：3 Phase items 全 `[x]` + Status: completed ✓；Plan Status 已由本审计从 active 更新为 completed；Draft Review Record 记录 0 Blocker / 0 Major / 3 Minor 已修订 ✓。**未发现实质缺陷**——PRIORITY warn-only 不可浏览器层日志断言为诚实 Non-Goal 声明（§45 + §109），非 cop-out；执行期纠正（字段名/区间类型/token「冲突」/3 份扩展）均记录于 plan Execution Decisions 段，体现执行者严谨。
- Verdict: PASS —— Phase 1/2/3 三阶段交付真实可验证（4 用例 spec 与 3 BizModel 源码精确对齐 + 3 docs 落地 + 范围纪律零生产代码变更 + 断言模式健全），执行忠实于计划意图，执行期纠正均诚实记录。

Follow-up:

- owner doc §10 其余 14 实体接入 + PRIORITY helper 运行时取值接线（触发条件见上 Deferred But Adjudicated 段，非阻塞）
