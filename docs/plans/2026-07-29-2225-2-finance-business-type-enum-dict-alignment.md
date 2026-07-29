# 2026-07-29-2225-2 finance ErpFinBusinessType enum↔dict 漂移对齐

> Plan Status: active
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MR1 工作项 R1.6（MA1 结构审计 P1 finding P1-MA1-018）
> Related: `docs/plans/00-plan-authoring-and-execution-guide.md`；`docs/audits/arm-index.md`（P1-MA1-018）；`docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-s-tier.md` §5.2
> Audit: required

## Current Baseline

`ErpFinBusinessType` enum（`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java`）的 4 项 enum 名与 ORM dict `erp-fin/business-type`（`module-finance/model/app-erp-finance.orm.xml`）的 dict value 不一致：

| enum 名（`ErpFinBusinessType.X.name()`） | dict value | 标签 | code |
|---|---|---|---|
| `MANUFACTURING_COST_CLOSE` | `PRODUCTION_COST` | 生产成本结转 | 100 |
| `PROJECT_COST_COLLECTION` | `PROJECT_COST` | 项目成本归集 | 110 |
| `PERIOD_CLOSE` | `PERIOD_CLOSING` | 期末结转 | 120 |
| `EXCHANGE_GAIN_LOSS` | `FX_REVALUATION` | 汇兑损益 | 130 |

**运行时影响**（MA1 审计 §5.2 + MA2 A2.5a 复核确认）：
- 代码以 `ErpFinBusinessType.X.name()` 持久化到 `voucher_bill_r.businessType` 列（实测 `ExchangeRevaluationService.java:152,213` / `ProfitLossClosingService.java:152` / `ErpFinReportBizModel.java:370,372` 等）。
- 内部聚合全用 `enum.name()` 一致 → **非运行时正确性 bug**（同一进程内 enum 名自洽）。
- UI dict 下拉值显示 dict value → 用户按 dict value 筛选时 `WHERE businessType='FX_REVALUATION'` 命中 0 行（实际存储 `EXCHANGE_GAIN_LOSS`）→ UI/审计筛选漏命中。
- owner doc `posting.md §业务类型映射` 声明「常量的 code 与字典的数值逐一一致」→ 文字断言与代码实现不符。

**剩余差距**：4 项漂移无修复，UI/审计筛选不一致持续存在。`mvn clean install -DskipTests` 全绿基线（154 模块）。

## Goals

- 统一 `ErpFinBusinessType` enum 名与 dict value，使 UI 筛选/审计查询能正确命中持久化值。
- owner doc `posting.md §业务类型映射` 断言与代码一致。

## Non-Goals

- 不改 `ErpFinBusinessType` 的 int `code`（100/110/120/130 不变——仅 enum `name` vs dict `value` 字符串漂移需对齐）。
- 不改其他 finance dict / enum（P1-MA1-018 仅涉及 `erp-fin/business-type` 这一个 dict）。
- 不改业务逻辑（过账链路用 `enum.name()` 持久化的机制不变，仅 enum 名值对齐）。

## Task Route

- Type: `implementation-only change`（ORM dict / enum 对齐，不改 API 契约或状态机行为）
- Owner Docs: `docs/design/finance/posting.md §业务类型映射`
- Skill Selection Basis: `nop-backend-dev`（涉及 finance ORM dict 变更 + enum 重命名）；ORM 变更已授权（roadmap 横切关注点）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- 数据迁移影响取决于 Phase 1 Explore 核实结果：若方案 A 被选（enum 重命名 → `enum.name()` 变化），需更新种子数据 delta 中 `voucher_bill_r.businessType` 列的旧名值；若方案 B 被选（dict 改为匹配 enum 名），Phase 1 Explore 将确认是否需迁移列值。本系统为参考应用骨架（单组织种子），数据迁移风险可控。

## Execution Plan

### Phase 1 - 裁决：方案 A vs 方案 B

Status: planned
Targets: `module-finance/erp-fin-dao/.../ErpFinBusinessType.java` + `module-finance/model/app-erp-finance.orm.xml` dict `erp-fin/business-type`
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: MA1 审计 §5.2 已提供两个备选方案

- [ ] Explore: 核实 `ErpFinBusinessType.X.name()` 在代码库中的全部引用站点（grep `ErpFinBusinessType\.` 全域），确认持久化路径确实用 `enum.name()` 而非 `enum.getCode()`，并枚举受影响的 import / switch / 比较 站点数。
      - Skill: `nop-backend-dev`
- [ ] Explore: 核实种子数据中 `voucher_bill_r.businessType` 列的存量值——当前种子是否已写入这 4 个 enum 名（如 `EXCHANGE_GAIN_LOSS`），以及 seed delta 文件位置。
      - Skill: `none`
- [ ] Decision: 方案选择——
  - **方案 A（MA1 审计推荐）**：重命名 enum 4 项为 dict 当前 value（`MANUFACTURING_COST_CLOSE→PRODUCTION_COST` 等）。**关键核实点**：重命名后 `enum.name()` 变化 → 新凭证持久化新名 → 需同步更新存量种子数据中的旧名值。全仓 import 替换 + 测试。优点：UI/查询一致性优先。
  - **方案 B**：把 dict 4 项 value 改为 enum 当前名（`PRODUCTION_COST→MANUFACTURING_COST_CLOSE` 等）。**关键核实点**：若代码用 `enum.name()` 持久化，dict 改为匹配 enum 名则无需迁移列值（DB 已存 enum 名）——MA1 审计描述方案 B 需数据迁移脚本可能不精确，需 Explore 确认。优点：零代码变更（仅 ORM dict value 改）。
  - 记录选择、替代方案、残留风险。框架约束：必须匹配现有 dict 真相源模式（ORM dict 是 UI 下拉值的真相源，enum.name() 是持久化值的真相源——两者必须对齐）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 方案 A vs B 已裁决，含持久化路径核实证据 + 存量种子数据影响评估
- [ ] 若选方案 A：受影响 import / 比较站点已枚举（为 Phase 2 提供范围）

### Phase 2 - 执行对齐 + 验证

Status: planned
Targets: `ErpFinBusinessType.java` + `app-erp-finance.orm.xml` dict + 种子数据 delta + `docs/design/finance/posting.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1 方案已裁决

- [ ] Add | Fix: 按裁决方案执行 enum/dict 对齐——
  - 方案 A：重命名 enum 4 项 + 全仓 import 替换 + 种子数据 delta 更新旧名值
  - 方案 B：改 ORM dict 4 项 value 为 enum 名（若 Explore 确认无需列值迁移）
  - Skill: `nop-backend-dev`
- [ ] Fix: 更新 owner doc `docs/design/finance/posting.md`，修正所有涉及 enum↔dict 对齐的断言——包括 `§业务类型映射` 的 enum 名 ↔ dict value ↔ 标签三列对齐表 + `§契约字段` 中「常量 code 与字典数值逐一一致」的文字断言（该断言本身即本次修复的漂移源）。
      - Skill: `none`
- [ ] Proof: 验证对齐效果——
  - `mvn test -pl module-finance/erp-fin-service` 全绿（finance 单模块测试，确认 enum 重命名/dict 变更后过账/凭证/报表测试无回归）
  - grep 种子 delta 文件确认无旧 enum 名残留（如 `EXCHANGE_GAIN_LOSS` 不再出现在 seed 数据的 businessType 列）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] enum 名与 dict value 4 项全部对齐
- [ ] finance 单模块测试全绿（`mvn test -pl module-finance/erp-fin-service`）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_051b8d927ffebHya6o0VzV2jBy) — Infra prereqs 与 Decision 项在方案 B 是否需数据迁移上自相矛盾 + Non-Goals 括注「code 与 dict value 数值已逐一一致」语义荒谬 + Phase 2 Proof 提供两种验证方法未承诺 + Phase 2 Proof 含全仓 build 应归 Closure Gates + owner doc Fix 仅覆盖 §业务类型映射 遗漏 §契约字段 漂移断言。已全部修订：Infra prereqs 改为条件式并 deferred 到 Phase 1 Explore、Non-Goals 去除伪等价、Proof 固定为 grep 种子 delta + finance 单模块测试（全仓 build 移至 Closure Gates）、owner doc Fix 扩展覆盖全部 posting.md 引用。
- Independent draft review iteration 2: acceptable as-is (ses_051b383e6ffefb4iAKTVOdflGS) — 全部 5 项 iteration 1 问题已修复（Infra prereqs 与 Decision 不再矛盾 / Non-Goals 去除伪等价 / Proof 固定单一方法 / 全仓 build 仅在 Closure Gates / owner doc Fix 覆盖两处 posting.md 引用经 grep 确认）；无新阻塞问题；2 项非阻塞 nit（§契约字段 实际 header 名 / Closure Gates 文档对齐括注仅列一节）不影响可执行性。

## Closure Gates

- [ ] 范围内行为完成（enum ↔ dict 对齐 + owner doc 更新）
- [ ] 相关文档对齐（`posting.md §业务类型映射`）
- [ ] 已运行验证：`mvn clean install -DskipTests` 全绿 + `mvn test -pl module-finance/erp-fin-service` 全绿
- [ ] compliance checker 基线不高于 M0 锚点
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

_（暂无）_

## Closure

Status Note: _（待执行后填写）_

Closure Audit Evidence:

- Auditor / Agent: _（待独立结束审计）_
- Evidence: _（待填写）_

Follow-up:

- 若裁决时发现 `ErpFinBusinessType` 的持久化机制应从 `enum.name()` 改为 `enum.getCode()`（更稳健的持久化策略），此项移出范围并开独立 successor（触及全 finance 过账链路持久化层，超出本 plan 的 dict 对齐范围）。
