# 2026-07-26-0315-1 C3 日期范围有效性 PRIORITY/STACKABLE 策略扩展 + sales 定价实体接入

> Plan Status: completed
> Last Reviewed: 2026-07-26
> Source: `docs/backlog/deepening-roadmap.md` §8.6 C3 落地证据 Deferred successor「PRIORITY 策略运行时取值 helper（pickHighestPriority，触发：sales ErpSalPriceList 接入）」+「STACKABLE 策略叠加计算 helper（触发：sales ErpSalPricingRule 接入）」+「全量实体应用（17 个 follow-up 实体清单见 owner doc §10）」sales 子集
> Related: `docs/plans/2026-07-21-2225-1-date-ranged-validity-pattern.md`（C3 基础设施 + 3 MUTEX 试点实体）、`docs/design/date-ranged-validity-pattern.md`（C3 owner doc §10 follow-up 清单）、`docs/plans/2026-07-10-1100-1-sales-pricing-engine.md`（sales 定价引擎已完成——本计划触发条件）
> Audit: required

## Current Baseline

C3 日期范围有效性模式已落地（plan `2026-07-21-2225-1` completed）：

- **可复用组件**（`module-master-data/erp-md-{dao,service}/.../daterange/`，纯 Java 无 IoC 依赖，跨域经 `app-erp-master-data-service` 依赖可达）：
  - `IDateRange` 接口（dao 层）— 2 getter 归一化 `validFrom/validTo` 与 `effectiveFrom/effectiveTo` 历史变体
  - `ErpDateRanges` 纯函数工具类 — 4 原语：`contains(range, date)` 含 `Date` 重载 / `overlaps(r1, r2)` / `effectiveOn(ranges, date)` / `longestOverlap(ranges)`（sweep line）
  - `ErpDateRangeOverlapValidator` 互斥校验器 — `enforceMutex(candidate, existing, errorCode, selfId)`
- **3 MUTEX 试点实体**（master-data 域）：`ErpMdExchangeRate` / `ErpMdTaxRate` / `ErpMdSupplierApproval`，均在 BizModel `defaultPrepareSave/Update` 钩子中调 `enforceMutex`
- **owner doc** `docs/design/date-ranged-validity-pattern.md` §10 列出 17 个 follow-up 实体，按 PRIORITY/MUTEX/STACKABLE 三策略分类

**缺失（本计划范围）**：C3 当前仅支持 MUTEX 策略。PRIORITY（允许重叠按优先级取首）和 STACKABLE（多条并行叠加）两种策略的 helper 尚未实现——owner doc §10 follow-up 实体中需要这两种策略的实体无法接入。

**触发条件已满足**：owner doc §10 sales 实体接入触发条件为「销售定价引擎细化 + sales service 接入」——sales 定价引擎（plan `2026-07-10-1100-1`）已完成并交付 `ErpSalCustomerPriceResolver` 取价链。sales 定价 3 实体（`ErpSalPriceList` / `ErpSalPriceListLine` / `ErpSalPricingRule`）有完整 validFrom/validTo + priority/stackable 字段但零日期范围校验——用户可创建日期重叠的价格清单/行导致取价歧义。

实时仓库核实（2026-07-26）：

**sales 定价 3 实体字段现状**：

| 实体 | validFrom/validTo | 策略字段 | 维度键 | 当前校验 |
|------|-------------------|----------|--------|----------|
| `ErpSalPriceList` (orm:1005) | DATE propId 7/8 | `priority` INTEGER default 100 (propId 9) | customerGroupCode + partnerId | 无 |
| `ErpSalPriceListLine` (orm:1058) | DATE propId 9/10（覆盖头） | 继承头 priority | priceListId + materialId | 无 |
| `ErpSalPricingRule` (orm:1111) | TIMESTAMP propId 20/21 | `priority` INTEGER (propId 18) + `stackable` BOOLEAN default false (propId 19) | ruleType + targetType + materialId/materialCategoryId + customerGroupCode/partnerId | 无 |

**BizModel 现状**：3 个 BizModel（`ErpSalPriceListBizModel` / `ErpSalPriceListLineBizModel` / `ErpSalPricingRuleBizModel`）均为 codegen 默认 `CrudBizModel` 骨架，无 `defaultPrepareSave/Update` 钩子。

**依赖可达性**：`module-sales/erp-sal-dao/pom.xml` 已依赖 `app-erp-master-data-dao`（`IDateRange` 接口可达）；`module-sales/erp-sal-service` 经传递依赖可达 `ErpDateRanges` + `ErpDateRangeOverlapValidator`。

**TIMESTAMP 变体**：`ErpSalPricingRule` 的 validFrom/validTo 为 TIMESTAMP（含时间分量），`IDateRange.getValidFrom()` 返回 `LocalDate`——`ErpDateRanges` 已有 `contains(IDateRange, Date)` 重载 + `toLocalDate(Date)` 截断适配（C3 试点时已预冻结此变体，见 `ErpDateRanges.java:45` javadoc）。

剩余差距：C3 仅 MUTEX 策略落地，PRIORITY/STACKABLE helper 缺失；sales 定价 3 实体零日期范围校验。

## Goals

- 扩展 C3 helper 支持两种新策略：PRIORITY（允许重叠，运行时取值按优先级取首）+ STACKABLE（允许重叠叠加，含 `stackable` 标志的混合策略校验）
- sales 定价 3 实体接入 C3 日期范围校验：`ErpSalPriceList`（PRIORITY）、`ErpSalPriceListLine`（MUTEX）、`ErpSalPricingRule`（STACKABLE 混合策略）
- EXPAND C3 owner doc 增 PRIORITY/STACKABLE 策略段 + sales 接入记录

## Non-Goals

- **不修改 ORM 模型**——3 实体已有 validFrom/validTo + priority + stackable 字段，无 ORM 变更
- **不改变 sales 定价引擎取价逻辑**——`ErpSalCustomerPriceResolver` 取价链不变；本计划仅增加保存时日期范围校验（防歧义），不改变运行时取价排序
- **不接入 owner doc §10 其余 14 个 follow-up 实体**（sales 2：ErpSalQuotation/ErpSalContract / purchase 2 / hr 2 / crm 4 / aps 1 / mfg 2 / logistics 1）——各域独立触发条件未满足，归 successor
- **不做 PRIORITY 运行时取值 helper 与定价引擎的接线**（即不把 pickHighestPriority 接入 ErpSalCustomerPriceResolver）——定价引擎已有自己的取价排序逻辑，C3 PRIORITY helper 仅为校验层工具；运行时取值接线属不同结果面
- **不做物化视图/反向索引**（C3 Deferred，触发：单实体有效记录数 > 10K 且 effectiveOn 查询 P95 > 200ms）
- **不做 helper 下沉到独立 erp-common-dao 模块**（C3 Deferred，触发：跨域接入数 > 3）

## Task Route

- Type: `implementation-only change`（C3 helper 纯 Java 扩展 + sales BizModel delta 钩子接入；无 ORM/契约/字典变更）
- Owner Docs: `docs/design/date-ranged-validity-pattern.md`（C3 owner doc EXPAND PRIORITY/STACKABLE 段 + sales 接入记录）、`docs/design/sales/pricing-engine.md`（sales 定价 owner doc 交叉引用注记——不改变取价逻辑，仅加保存校验注记）
- Skill Selection Basis: `nop-backend-dev`（BizModel delta `defaultPrepareSave/Update` 钩子 + 跨实体调用规范 + 纯函数 helper 扩展）；需阅读 `docs/design/date-ranged-validity-pattern.md` §3-6 确认 PRIORITY/STACKABLE 语义约定

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 — PRIORITY + STACKABLE helper 扩展

Status: completed
Targets: `module-master-data/erp-md-service/src/main/java/app/erp/md/service/daterange/ErpDateRanges.java`（PRIORITY helper 追加）、`ErpDateRangeOverlapValidator.java`（STACKABLE 混合策略校验器追加）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: C3 基础设施已落地（plan 2026-07-21-2225-1 completed）

- [x] `Decision`：PRIORITY helper 签名裁决。候选：
      - (a) `pickHighestPriority(List<T> effective, Comparator<T> priorityCmp)` — 泛型 Comparator 由调用方提供优先级方向（sales 约定 priority 值越小优先级越高，调用方传 `Comparator.comparingInt(ErpSalPriceList::getPriority)`）
      - (b) `pickHighestPriority(List<T> effective, Function<T,Integer> priorityGetter, boolean higherIsFirst)` — 内置方向标志
      - 推荐裁决倾向 (a)：纯函数不假定优先级方向语义，调用方明确传 Comparator，与 `ErpDateRanges` 既有无业务语义原语风格一致
      - 考虑的替代方案：在 `IDateRange` 接口加 `getPriority()` 默认方法——但并非所有日期范围实体都有 priority 字段（MUTEX 实体无），违反接口最小化
      - 残留风险：调用方传错 Comparator 方向——经测试覆盖缓解
      - **裁决=(a)**：`pickHighestPriority(List<T extends IDateRange>, Comparator<T>)` 已落地，纯函数 + 调用方提供方向。
      - Skill: `nop-backend-dev`
- [x] `Decision`：STACKABLE 混合策略校验语义裁决。`ErpSalPricingRule.stackable` 标志的含义：
      - (a) `stackable=true` 的规则可与任何规则重叠；`stackable=false` 的规则之间互斥（但可与 stackable=true 规则重叠）
      - (b) `stackable=false` 的规则与所有规则互斥（包括 stackable=true 的）
      - 推荐裁决倾向 (a)：stackable=true 表达「我可被叠加」语义，非「我排斥他人」——与促销引擎「满减+折扣+赠品」叠加场景一致
      - 残留风险：若业务语义实为 (b)，需调整——经 owner doc 记录 + sales 域审查缓解
      - **裁决=(a)**：`enforceStackableAware` 仅当两侧均非 stackable 且重叠才抛异常；任一方 stackable=true 允许重叠。
      - Skill: `nop-backend-dev`
- [x] `Add`：`ErpDateRanges.pickHighestPriority(List<T extends IDateRange> effective, Comparator<T> priorityCmp)` — 从 `effectiveOn` 结果中按 Comparator 取首（`stream().min(priorityCmp).orElse(null)`）；空 List 返回 null。纯函数无副作用，不抛异常。
      - Skill: `nop-backend-dev`
- [x] `Add`：`ErpDateRangeOverlapValidator.enforceStackableAware(T candidate, List<T> existing, ErrorCode errorCode, Object selfId, Predicate<T> isStackable)` — 校验逻辑：遍历 existing（排除 selfId + 永久无区间跳过），若 candidate 非 stackable 且 other 非 stackable 且重叠 → 抛异常；若任一方 stackable=true → 允许重叠。`isStackable` Predicate 由调用方提供（如 `rule -> rule.getStackable()`）。
      - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpDateRanges`（既有 29 场景）扩展 PRIORITY 场景——pickHighestPriority 空列表/null 返回 null + 单元素返回该元素 + 多元素按 Comparator 取首 + 相同优先级稳定性；`TestErpDateRangeOverlapValidator`（既有或新建）扩展 STACKABLE 场景——双非 stackable 重叠抛异常 + 一方 stackable 允许重叠 + 双 stackable 允许重叠 + selfId 排除 + 永久无区间跳过
      - Skill: none
      - **新增 13 场景**：pickHighestPriority 5（空/null/单元素/多元素/相同优先级稳定/effectiveOn 管道） + enforceStackableAware 8（双非重叠拒绝/candidate stackable/other stackable/双 stackable/selfId 排除/永久无区间跳过/empty+null existing/相邻日不重叠）。TestErpDateRanges 29→42 场景，全绿。master-data service 全测试 108→122 全绿（0 回归）。

Exit Criteria:

- [x] `pickHighestPriority` + `enforceStackableAware` 两新方法存在且签名与 Decision 一致
- [x] master-data service 单测全绿（既有 108 + 新增 PRIORITY/STACKABLE 场景，0 回归）

### Phase 2 — sales 定价 3 实体接入

Status: completed
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/ErpSalPriceListBizModel.java`、`ErpSalPriceListLineBizModel.java`、`ErpSalPricingRuleBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Item Types Note: Phase 2 is Add-heavy（3 BizModel delta 钩子接入 + 1 IDateRange TIMESTAMP 适配 Decision）
- Prereqs: Phase 1 helper 扩展完成 + 单测全绿

- [x] `Add`：`ErpSalPriceListBizModel` delta 扩展 `defaultPrepareSave` + `defaultPrepareUpdate` 钩子——`ErpSalPriceList implements IDateRange`（直接 implements，字段已为 `validFrom/validTo` 规范命名）；按维度键（customerGroupCode + partnerId，对齐 owner doc `date-ranged-validity-pattern.md:105` + ORM 实测：ErpSalPriceList 仅有 customerGroupCode + partnerId 两维度字段，无 materialId/materialCategoryId）查同维度已存在记录；PRIORITY 策略**不拒绝重叠**（仅记录 log 提示有多份有效清单），不做 enforceMutex——PRIORITY 语义允许重叠由取价引擎择优。**接入价值**：在 hook 中用 `ErpDateRanges.effectiveOn` + `pickHighestPriority` 验证候选保存后仍有确定取价（若多份有效且优先级相同 → warn 提示取价歧义，非阻断）。错误码 `ERR_SAL_PRICE_LIST_PRIORITY_AMBIGUOUS`（warn-only，不抛异常）。
      - Skill: `nop-backend-dev`
- [x] `Add`：`ErpSalPriceListLineBizModel` delta 扩展 `defaultPrepareSave` + `defaultPrepareUpdate` 钩子——`ErpSalPriceListLine implements IDateRange`；按维度键（priceListId + materialId）查同维度已存在记录；调 `enforceMutex(candidate, existing, ERR_SAL_PRICE_LIST_LINE_OVERLAP, selfId)`。错误码经 `ErpSalErrors`（`erp.err.sal.price-list-line.overlap`）。
      - Skill: `nop-backend-dev`
- [x] `Add`：`ErpSalPricingRuleBizModel` delta 扩展 `defaultPrepareSave` + `defaultPrepareUpdate` 钩子——`ErpSalPricingRule implements IDateRange`（TIMESTAMP 变体，`IDateRange.getValidFrom()` 内部截断到 `LocalDate`——因 `ErpSalPricingRule` validFrom 类型为 `Date`（TIMESTAMP），实现 `IDateRange` 时 getter 返回 `toLocalDate(validFromTimestamp)` 或调整 `IDateRange` 接口支持 `Date` 返回变体）；按维度键（ruleType + targetType + materialId/materialCategoryId + customerGroupCode/partnerId）查同维度已存在记录；调 `enforceStackableAware(candidate, existing, ERR_SAL_PRICING_RULE_OVERLAP, selfId, ErpSalPricingRule::getStackable)`。错误码经 `ErpSalErrors`。
      - Skill: `nop-backend-dev`
      - **TIMESTAMP 适配实现偏离**：ORM 生成基类 `_ErpSalPricingRule.getValidFrom()` 为 `public final java.sql.Timestamp`，与 `IDateRange.getValidFrom(): LocalDate` 返回类型不兼容（Java 不允许同签名不同返回类型），无法直接 `implements IDateRange`。按 C3 owner doc §8.1「跨域接入适配器」范式在 BizModel 内构造 `PricingRuleDateRange` 适配器（将 `Timestamp` 截断到 `LocalDate` + 暴露 `id` + `stackable`），传给 `enforceStackableAware`。语义与 Decision (a)「调用方先截断到 LocalDate」等价。
- [x] `Decision`：`IDateRange` 接口 TIMESTAMP 变体适配裁决。`IDateRange.getValidFrom()` 返回 `LocalDate`，但 `ErpSalPricingRule.validFrom` 是 TIMESTAMP（`Date`）。候选：
      - (a) `ErpSalPricingRule implements IDateRange` 时 getter 内部截断 `Date → LocalDate`（`toLocalDate`）——简单，但丢失时间分量精度
      - (b) `IDateRange` 接口增加 `default LocalDate getValidFrom()` + `default LocalDate getValidTo()` 由实体实现，TIMESTAMP 实体自行截断——与 (a) 等价但更显式
      - 推荐裁决倾向 (a)：`ErpSalPricingRule` 在 implements 时提供 getter 转换，`IDateRange` 接口不变（向后兼容既有 3 试点实体）。C3 owner doc §3.1 已约定 TIMESTAMP 变体「调用方应先截断到 LocalDate」。
      - Skill: `nop-backend-dev`
      - **裁决=(a) 语义，实现走 owner doc §8.1 适配器范式**：因 Java 语法限制（ORM 生成 final 方法 + 返回类型不兼容），实体直接 `implements IDateRange` 不可行。适配器实现保留了 (a)「TIMESTAMP 截断到 LocalDate + IDateRange 接口不变」的语义不变性 + 向后兼容。

Exit Criteria:

- [x] 3 sales BizModel 各有 `defaultPrepareSave/Update` 钩子调用 C3 helper
- [x] sales service 单测新增日期范围校验场景全绿（正路径不重叠通过 + 负路径重叠/歧义拒绝）
- [x] 既有 sales service 测试 0 回归（sales 定价引擎取价逻辑不受影响）
   - **验证结果**：sales-service 125→135 测试全绿（+10 新增 TestErpSalDateRange：3 priceListLine MUTEX + 2 priceList PRIORITY + 5 pricingRule STACKABLE），0 回归；全 workspace `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。

### Phase 3 — owner doc EXPAND + roadmap 同步

Status: completed
Targets: `docs/design/date-ranged-validity-pattern.md`、`docs/design/sales/pricing-engine.md`（交叉引用注记）、`docs/backlog/deepening-roadmap.md`（C3 Deferred successor RELEASED 标注）
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 完成

- [x] `Add`：`docs/design/date-ranged-validity-pattern.md` EXPAND——§5 新增 PRIORITY 策略段（pickHighestPriority helper + 调用范式 + 与 MUTEX 语义对比）+ STACKABLE 策略段（enforceStackableAware helper + stackable 标志语义 + Decision (a)/(b) 裁决记录）；§8 试点实施记录增 sales 3 实体接入记录；§10 follow-up 清单 sales 3 实体标 RELEASED
      - Skill: none
      - **落地**：§4 新增 §4.1 PRIORITY 策略段（pickHighestPriority helper + 调用范式 + PRIORITY vs MUTEX 对比表）+ §4.2 STACKABLE 混合策略段（Decision (a) 裁决 + 替代方案否决记录 + 调用范式）；§7 新增「sales 定价 3 实体接入记录」段（含 TIMESTAMP 变体适配 + 关键设计决策记录）；§10 sales 3 实体标 `RELEASED by 2026-07-26-0315-1`。
- [x] `Add`：`docs/design/sales/pricing-engine.md` 增「日期范围有效性校验（C3 交叉引用）」段——3 实体保存校验注记 + PRIORITY/STACKABLE/MUTEX 策略映射 + 不改变取价逻辑声明
      - Skill: none
      - **实现偏离**：`docs/design/sales/pricing-engine.md` 不存在（sales 定价引擎 owner doc 实际分散于 `sales/README.md` 第 70 行 + `sales/use-cases.md`）。按 AGENTS.md「NEVER proactively create documentation files」规则，C3 交叉引用段落地于既有 `docs/design/sales/README.md` 新增「日期范围有效性校验（C3 交叉引用）」段（3 实体策略映射表 + TIMESTAMP 变体说明 + 「不改变取价逻辑」声明）。
- [x] `Add`：`docs/backlog/deepening-roadmap.md` §8.6 C3 Deferred successor 标注 PRIORITY/STACKABLE helper + sales 子集 RELEASED by 本计划
      - Skill: none
      - **落地**：§8.6 Deferred successor 行追注 `sales 子集 3 实体 RELEASED by 2026-07-26-0315-1` + `PRIORITY helper RELEASED` + `STACKABLE helper RELEASED`；helper 下沉触发条件更新为 `当前跨域接入数 = 1：sales`。

Exit Criteria:

- [x] C3 owner doc 含 PRIORITY + STACKABLE 两策略段（含 Decision 记录）
- [x] sales pricing-engine.md 含 C3 交叉引用段
- [x] deepening-roadmap §8.6 Deferred successor 更新

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（`ses_0654a9f9bffevpGsIA9B75xl4I`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-26）— 1 Major / 3 Minor。Load-bearing 事实大部分精确匹配（C3 helper 4 原语 + enforceMutex only ✓ / 3 sales BizModel bare skeleton ✓ / sales-dao→md-dao 依赖 ✓ / 定价引擎 completed ✓ / deferred successor 准确 ✓）。**Major M1**：ErpSalPriceList 维度键 baseline + Phase 2 引用不存在的 materialId/materialCategoryId——ORM 实测仅 customerGroupCode + partnerId（对齐 owner doc :105），Phase 2 不可执行；**已修订**：baseline 表 + Phase 2 item 维度键改为 customerGroupCode + partnerId。**Minor**：(1) ErpSalPriceListLine propId 7/8→9/10 ✓；(2) Phase 2 Item Types `Add | Fix`→`Add | Decision`（零 Fix 项）✓；(3) Non-Goal 物化视图触发条件补齐 latency 联言 ✓。
- Independent draft review iteration 2: `acceptable as-is`（`ses_065482055ffeuFJH3xw52Vynj8`，独立 general 子代理，新会话冷重播，2026-07-26）— 0 Blocker / 0 Major / 1 Minor。M1 + m1/m2/m3 全部修订经实仓核实 CONFIRMED。追加 spot-check（ErpSalPricingRule propIds / md-dao 依赖双向 / TIMESTAMP 重载 / §10 实体计数 / 禁用词 / Decision 三要素 / Skill 记录）全部 ✓。**N1 Minor**：Non-Goal 括号内分项合计 12≠14（漏列 sales 2 实体 ErpSalQuotation/ErpSalContract）；**已修订**：补 `sales 2` 使分项合计一致。计划为可接受的执行契约，可提升为 active。

## Closure Gates

> 本计划无 ORM/契约/字典变更。纯 Java helper 扩展 + BizModel delta 钩子 + 文档。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ master-data/sales service `mvn test`。

- [x] 范围内行为完成（PRIORITY + STACKABLE helper + sales 3 实体接入 + owner doc EXPAND）
- [x] 相关文档对齐（C3 owner doc + sales/README.md C3 交叉引用段 + deepening-roadmap §8.6；pricing-engine.md 不存在，已按 AGENTS.md §「NEVER proactively create documentation files」落地于既有 sales/README.md，偏离记录于 Phase 3 item body）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ master-data service `mvn test`（108→122）+ sales service `mvn test`（125→135，0 回归）—— 见 `docs/logs/2026/07-26.md`
- [x] 无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 两项均为显式 Non-Goal 且触发条件未满足：其余 14 follow-up 实体各域触发条件未满足 / PRIORITY helper 运行时取值接线属不同结果面）
- [x] 独立草案审查已完成并记录（Draft Review Record 2 轮：iteration 1 needs revision 1 Major/3 Minor 已修订；iteration 2 acceptable as-is 0 Blocker/0 Major/1 Minor 已修订）
- [x] 文本一致性已验证：Plan Status completed / 3 Phase 全 completed / 各 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / `docs/logs/2026/07-26.md` 条目存在且与计划一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（见下 Closure Audit Evidence）

## Deferred But Adjudicated

### owner doc §10 其余 14 个 follow-up 实体

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 各域独立触发条件未满足（purchase 定价引擎 / hr 薪酬档调整 / crm 评分引擎 / aps 工艺路线版本 / mfg 工作中心日历 / logistics 交付时间窗）。本计划仅覆盖触发条件已满足的 sales 3 实体。
- Successor Required: `yes`（触发条件：各域业务流程细化 + 该域 service 接入需求）

### PRIORITY helper 与 sales 定价引擎运行时取值接线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划的 PRIORITY helper 仅为保存校验层工具（warn-on-ambiguity）。运行时取价排序由 `ErpSalCustomerPriceResolver` 独有逻辑承担，接线属不同结果面。
- Successor Required: `yes`（触发条件：定价引擎需统一日期范围优先级解析层时）

## Closure

Status Note: 3 Phase 全部完成。Phase 1 helper 扩展（`pickHighestPriority` + `enforceStackableAware` + 13 新单测场景，master-data service 108→122 测试全绿）；Phase 2 sales 定价 3 实体接入（`ErpSalPriceList` PRIORITY warn-only / `ErpSalPriceListLine` MUTEX / `ErpSalPricingRule` STACKABLE 混合策略 + TIMESTAMP 适配器范式偏离记录 + 10 新集成测试，sales-service 125→135 测试全绿 0 回归）；Phase 3 owner doc EXPAND（C3 owner doc §4.1 PRIORITY + §4.2 STACKABLE + §7 sales 接入记录 + §10 RELEASED 标记 + sales/README C3 交叉引用段 + roadmap §8.6 Deferred successor RELEASED 标注）。全 workspace `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。

执行期发现与处置：
1. **依赖可达性 baseline 不准**：plan baseline 称 sales-service 经传递依赖可达 master-data-service helper——实测 master-data-service 原仅 test-scope，已提升为 compile-scope（DAG 无环验证：master-data→sales 无依赖）。
2. **TIMESTAMP 适配实现偏离**：plan Phase 2 Decision (a) 建议 `ErpSalPricingRule implements IDateRange` + getter 截断——实测 ORM 生成基类 getter 为 `final` + 返回类型 `Timestamp` vs `IDateRange.LocalDate` 不兼容（Java 不允许），改走 owner doc §8.1「跨域接入适配器」范式在 BizModel 内构造 `PricingRuleDateRange` 适配器。语义与 Decision (a)「TIMESTAMP 截断到 LocalDate + IDateRange 接口不变」等价，已记录于 plan Phase 2 + owner doc §7。
3. **pricing-engine.md 不存在**：plan Phase 3 称「pricing-engine.md 增 C3 交叉引用段」，实际文件不存在（sales 定价 owner doc 散于 sales/README.md:70 + sales/use-cases.md）。按 AGENTS.md「NEVER proactively create documentation files」规则，C3 交叉引用段落地于既有 sales/README.md 新增段，已在 plan Phase 3 记录偏离。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-auditor，新会话冷重播无执行者上下文，2026-07-26）
- Evidence:
  - **Phase 1 实仓核实**：`module-master-data/erp-md-service/.../daterange/ErpDateRanges.java:176` 存在 `pickHighestPriority(List<T extends IDateRange>, Comparator<T>)` 真实实现（非空体，`stream().min(priorityCmp).orElse(null)`，null/empty 短路返回 null）；`ErpDateRangeOverlapValidator.java:116` 存在 `enforceStackableAware(...)` 真实实现（selfId 排除 + 永久无区间跳过 + 双非 stackable 重叠抛 `NopException`，任一方 stackable=true 允许重叠）。签名与 Phase 1 两 Decision 裁决 (a) 完全一致。
  - **Phase 2 实仓核实**：3 sales BizModel 均有 `defaultPrepareSave` + `defaultPrepareUpdate` 钩子调用 C3 helper——`ErpSalPriceListBizModel.java:47,53`（PRIORITY warn-only，调 `effectiveOn + pickHighestPriority`）；`ErpSalPriceListLineBizModel.java:34,40`（MUTEX，调 `enforceMutex(ERR_SAL_PRICE_LIST_LINE_OVERLAP, selfId)`）；`ErpSalPricingRuleBizModel.java:51,57`（STACKABLE，调 `enforceStackableAware(ERR_SAL_PRICING_RULE_OVERLAP, ..., PricingRuleDateRange::getStackable)`）。TIMESTAMP 适配器 `PricingRuleDateRange implements IDateRange` 存在于 `ErpSalPricingRuleBizModel.java:100`（`Timestamp.toLocalDateTime().toLocalDate()` 截断），与 Phase 2 Decision 裁决 (a) 语义 + owner doc §8.1 适配器范式一致。Anti-hollow 检查：所有钩子方法体非空、无 `return null` 占位、无 swallowed exception。
  - **测试核实**：`TestErpDateRanges.java`（master-data）+ `TestErpSalDateRange.java`（sales，含 3 priceListLine MUTEX + 2 priceList PRIORITY + 5 pricingRule STACKABLE 场景）存在；测试结果证据见 `docs/logs/2026/07-26.md`（master-data service 108→122 全绿，sales-service 125→135 全绿，0 回归）。
  - **Phase 3 文档核实**：`docs/design/date-ranged-validity-pattern.md` 含 §4.1 PRIORITY 段 + §4.2 STACKABLE 段 + §7 sales 接入记录段（含 TIMESTAMP 变体适配）+ §10 sales 3 实体标 `RELEASED by 2026-07-26-0315-1`；`docs/design/sales/README.md:126` 含「日期范围有效性校验（C3 交叉引用）」段（含「不改变定价引擎运行时取价逻辑」声明）；`docs/backlog/deepening-roadmap.md:289` §8.6 Deferred successor 标注 PRIORITY/STACKABLE helper + sales 子集 RELEASED。
  - **Deferred Honesty**：Deferred But Adjudicated 两项均与 Non-Goals 一致（其余 14 follow-up 实体触发条件未满足 / PRIORITY helper 运行时取值接线属不同结果面），无范围内缺陷或契约漂移隐藏为 deferred。
  - **Five-point consistency**：Plan Status completed ↔ 3 Phase 全 completed ↔ 各 Phase Exit Criteria 全 [x] ↔ Closure Gates 全 [x] ↔ `docs/logs/2026/07-26.md` 条目存在——全部一致。
  - **执行期偏离处置**：3 项偏离（依赖可达性 baseline 不准 / TIMESTAMP 适配实现走适配器范式 / pricing-engine.md 不存在改落地 sales/README.md）均已在 Status Note 下「执行期发现与处置」段 + 对应 Phase item body 显式记录，无静默漂移。

Follow-up:

- owner doc §10 其余 14 实体接入（触发条件见上）
- PRIORITY helper 运行时取值接线（触发条件见上）
