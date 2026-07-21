# 2026-07-21-2225-1-date-ranged-validity-pattern 日期范围有效性模式（C3）

> Plan Status: completed
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/deepening-roadmap.md` §Milestone C / C3（`todo`）；`docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md` §3.5
> Related: `docs/design/master-data/README.md`、`docs/design/sales/`（价格/促销实体使用日期范围）、`docs/design/finance/budget.md`、`docs/design/hr/`（合同/薪酬 effectiveFrom/effectiveTo）
> Audit: required

## Current Baseline

ERP 中大量实体使用「生效日期 + 失效日期」表达记录的有效期（价格清单、促销、汇率、合同、薪酬档、工作中心日历、客户分级等）。当前实现存在三类不一致，且无统一查询/校验基础设施：

**字段命名三分裂**（grep 12 个 `<domain>/model/*.orm.xml` 源模型确认）：

| 命名变体 | 类型 | 出现域（源 ORM 文件） |
|----------|------|----------------------|
| `validFrom` / `validTo` | DATE | finance(3 实体) / master-data(3) / sales(5：4 DATE + 1 TIMESTAMP+clock tagSet) / purchase(2) / manufacturing(1 + routing) |
| `effectiveFrom` / `effectiveTo` | DATE | hr(2) / aps(1) / manufacturing(workcenter calendar) / crm(4) / logistics(1) |
| `effectiveDate` | DATE（单值，非区间） | cs(1) |

> grep 范围：19 个 `<domain>/model/*.orm.xml` 源模型中 **10 个**含日期范围字段（master-data/sales/purchase/finance/hr/aps/manufacturing/crm/logistics/cs）。

**缺失基础设施**：

- 无权威约定文档（无 `docs/design/date-ranged-validity-pattern.md`）
- 无统一查询 helper：判断「某日期是否在区间内」「两个区间是否重叠」「某物料在某日期的有效记录」均由各域 BizModel 各自手写 `le/ge` filter，逻辑分散且易错
- 无重叠校验规则：同一物料/同一维度的多条有效期记录是否允许区间重叠（如价格清单重叠→哪条优先？）无统一约定，部分域隐式依赖 `ORDER BY validFrom DESC LIMIT 1` 兜底
- 无 anti-pattern 自检表

**已落地的邻近能力**（不重复造）：`ErpMdAcctSchema`（accounting period 状态机）、`ErpFinBudgetScenario`（budgetGroupCode + periodId + status 维度）使用 validFrom/validTo 但语义是「预算期间」而非「记录有效期」，本计划不触及这些实体的期间语义。

## Goals

- **G1 — 建立权威约定**：NEW `docs/design/date-ranged-validity-pattern.md`，裁决规范字段命名（含新旧实体迁移策略）、区间查询语义、重叠策略分类（互斥 vs 优先级 vs 允许重叠）
- **G2 — 提供可复用查询 helper**：实现区间运算原语（contains / overlaps / effectiveOn / longestOverlap），让各域查询不再各自手写 `le/ge` 拼接
- **G3 — 提供重叠校验规则**：对「同维度区间互斥」语义的实体提供校验器（save 前置 friendly check），避免业务上不允许的重叠落库
- **G4 — 试点应用 + 反模式自检**：在 2-3 个高价值试点实体落地约定（含必要时的 ORM 字段补齐），并产出反模式自检表

## Non-Goals

- **不**大规模重命名既有字段（跨 12+ 源 ORM 文件 + 全量 codegen 再生 = 高风险数据迁移；改由 Phase 1 Decision 裁决迁移策略）
- **不**改 `cs/effectiveDate` 单值字段语义（它是「生效时点」非「区间」，归不同模式）
- **不**在本计划覆盖全部 12+ 日期范围实体（仅试点 2-3，其余作为 owner doc 中显式列出的 follow-up）
- **不**改造预算期间/会计期间状态机（不同语义，见 Baseline）
- **不**实现物料有效性物化视图/反向索引（C1 已留 deferred successor，触发条件=性能需求）

## Task Route

- Type: `app-layer design change`（含可能的轻量 ORM 字段补齐 + Java/SQL-lib helper）
- Owner Docs: `docs/design/date-ranged-validity-pattern.md`（**NEW**）；交叉回链 `docs/design/master-data/README.md`、`docs/design/sales/`（价格相关）、`docs/design/finance/budget.md`、`docs/design/hr/README.md`、`docs/architecture/l10n-strategy.md`（汇率有效期）
- Skill Selection Basis: 实现阶段涉及 BizModel helper / SQL-lib / 可能的 ORM 字段追加，匹配 `nop-backend-dev`（决策门、xbiz、跨实体、protected step、错误码、产品化可定制性自检）。Phase 1 纯设计 Decision 阶段不强制技能。无前端工作 → `nop-frontend-dev` 不匹配。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无新端口/密钥/外部服务）
- helper 若采用 SQL-lib 方案，需确认 `@SqlLibMapper` 在跨域共享 dao 模块的可达性（Phase 1 Explore 项）

## Execution Plan

### Phase 1 - Explore + Owner Doc NEW + 规范裁决

Status: completed
Targets: `docs/design/date-ranged-validity-pattern.md`（NEW）
Skill: `none`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] **Explore**：盘点全部使用日期范围字段的实体（源 `<domain>/model/*.orm.xml`），分类为「记录有效期」vs「期间语义」vs「生效时点」，仅「记录有效期」类纳入本模式范围
  - Skill: `none`
- [x] **Decision A — 规范字段命名**：在 `validFrom/validTo` 与 `effectiveFrom/effectiveTo` 间裁决规范。候选：(a) `validFrom/validTo` 为规范（理由：4 核心域 finance/md/sal/pur 已用，数量占优）；(b) `effectiveFrom/effectiveTo` 为规范；(c) 双轨接受 + helper 归一化。记录选择、替代方案、残留风险
  - Skill: `none`
- [x] **Decision B — 既有字段迁移策略**：裁决是否重命名既有非规范字段。候选：(a) 不重命名，仅规范新实体 + helper 归一化（低风险，推荐）；(b) 渐进重命名（高风险数据迁移，需独立 ORM 授权计划）。**预期裁决=a**
  - Skill: `none`
- [x] **Decision C — 重叠策略分类**：定义三类语义并映射到试点实体：(1) 互斥（同维度同一时刻至多 1 条有效，如「默认价格清单」）；(2) 优先级（允许重叠但按 priority/timestamp 取一，如「会员价 vs 标准价」）；(3) 允许重叠（如「多条并行促销」）。记录裁决矩阵
  - Skill: `none`
- [x] **Decision D — helper 实现位置**：候选：(a) `module-master-data/erp-md-service` 工具类（跨域经 md-dao 可达）；(b) `nop-commons` 风格静态工具类；(c) SQL-lib 跨域 mapper。**预期裁决=a 或 c**，记录理由
  - Skill: `none`
- [x] **Add**：NEW `docs/design/date-ranged-validity-pattern.md`，含 8 节：目的与范围 / 规范字段命名（Decision A）+ 迁移策略（Decision B）/ 区间查询语义（contains/overlaps/effectiveOn 定义 + 边界含否约定）/ 重叠策略分类（Decision C 矩阵）/ 查询 helper 契约（Decision D）/ 重叠校验规则 + 错误码约定 / 试点实施记录 / 反模式自检表
  - Skill: `none`

Exit Criteria:

> 本阶段交付 owner doc 草案 + 4 项 Decision 裁决，为后续阶段解除阻塞。无需仓库级验证。

- [x] `docs/design/date-ranged-validity-pattern.md` 存在且含上述 8 节骨架
- [x] 4 项 Decision（A/B/C/D）均在 owner doc 内记录选择 + 替代方案 + 残留风险
- [x] 试点实体清单（2-3 个）+ 重叠策略映射已在 owner doc 中显式列出

### Phase 2 - 查询 helper + 重叠校验器实现

Status: completed
Targets: 按 Decision D 选定的模块（预期 `module-master-data/erp-md-service` 或 SQL-lib）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 Decision A/C/D 裁决完成

- [x] **Add**：实现区间运算原语（pure Java 工具类或 SQL-lib mapper）：`contains(range, date)` / `overlaps(r1, r2)` / `effectiveOn(ranges, date)` / `longestOverlap(ranges)`；边界约定（validTo 含/不含）按 Decision A 文档化
  - Skill: `nop-backend-dev`
- [x] **Add**：实现「同维度互斥」重叠校验器（friendly pre-save check）：输入实体 + 维度键（如 materialId + priceListType），查询同维度已存在记录，若新区间与既有重叠则抛 `NopException` + 中文 ErrorCode（不暴露 stack trace）
  - Skill: `nop-backend-dev`
- [x] **Add**：错误码（如 `ERP_MD_DATE_RANGE_OVERLAP` 或按试点域归属命名）落入对应域 `Erp*Errors.java`
  - Skill: `nop-backend-dev`
- [x] **Proof**：单元测试覆盖 helper 原语（含边界：空端点、相同端点、完全包含、相邻不重叠、空区间集合）+ 重叠校验器正负路径（互斥拒绝 / 允许重叠通过）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 本阶段交付可复用 helper + 校验器 + 单测，解除 Phase 3 试点应用阻塞。仅需本模块 localized 测试。

- [x] helper 原语 + 重叠校验器实现落盘，单测全绿（含边界场景）
- [x] helper 所在模块 `mvn test` 通过（localized，不需全 workspace）

### Phase 3 - 试点应用 + owner doc 回链

Status: completed
Targets: 试点实体（预期 master-data 价格清单 + sales 价格清单 + 1 个 hr/contract 实体，具体由 Phase 1 裁决）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 2 helper + 校验器落地

- [x] **Add | Fix**：试点 2-3 实体接入重叠校验器（defaultPrepareSave/defaultPrepareUpdate 钩子），替代各自手写的 `le/ge` 查询；如试点实体缺字段（如仅有 validFrom 无 validTo）按 Decision B 补齐 ORM 字段（向后兼容 mandatory=false + 默认 null）。**注**：C3 不在 deepening-roadmap §8「ORM 变更已授权」清单内（仅 A1/A2/A3/B1/C2 在列），故任何 ORM 字段追加在实施前须按 `project-context.md §AI 阻塞条件` 取得明确人工批准（或由 mission-driver 显式指令授权）；Phase 1 Decision B 裁决「不重命名既有字段」可规避大部分授权需求，仅「补齐缺失字段」场景触发授权门控
  - Skill: `nop-backend-dev`
- [x] **Proof**：试点实体的集成测试（友好拒绝重叠 + 正常保存通过），回归既有业务测试无破损
  - Skill: `nop-backend-dev`
- [x] **Add**：owner doc `docs/design/date-ranged-validity-pattern.md` 补「试点实施记录」节，含每个试点实体的字段映射 + 重叠策略归属 + 接入点；交叉回链试点域 README（如 `docs/design/master-data/README.md` 增「日期范围有效性」段）
  - Skill: `none`

Exit Criteria:

> 本阶段交付 2-3 个试点实体落地 + owner doc 回链，证明模式可用。仅需试点域 localized 测试。

- [x] 2-3 试点实体接入校验器，集成测试正负路径全绿
- [x] 试点域 service 模块 `mvn test` 通过（localized）

### Phase 4 - 收尾：roadmap 同步 + 全仓库验证

Status: completed
Targets: `docs/backlog/deepening-roadmap.md`、本计划 Closure
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1-3 全部 Exit Criteria 勾选

- [x] **Add**：`docs/backlog/deepening-roadmap.md` C3 行 `todo → done` + 增 §8.x 落地证据段（plan/owner doc/helper/试点/测试基线/deferred successor）
  - Skill: `none`

Exit Criteria:

- [x] roadmap C3 状态更新 + 落地证据段落盘

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is`（`ses_07ae9b425ffekBug6WkkimszTb`，2026-07-21）— 计划结构合理（Decision-gated 设计 + localized 实施 + roadmap 同步），8 项 Closure Gate 正确，反松弛合规，单结果表面。3 项非阻塞修正已在本次修订应用：(1) sales 实体计数 4→5（含 TIMESTAMP 变体）；(2) grep 范围澄清「19 个 ORM 源模型中 10 个含日期范围字段」；(3) Phase 3 ORM 字段补齐场景显式标注授权门控（C3 不在 §8 授权清单）。结论：可接受为执行契约，置 `active`。

## Closure Gates

> 在所有 Phase Exit Criteria 与下方门控全勾选后关闭。结束时运行一次全仓库 `mvn clean install -DskipTests` + 受影响 service 模块 `mvn test`。

- [x] 范围内行为完成（owner doc + helper + 校验器 + 2-3 试点）
- [x] 相关文档对齐（date-ranged-validity-pattern.md NEW + 试点域 README 回链 + roadmap §8.x）
- [x] 已运行验证：全 workspace `mvn clean install -DskipTests` BUILD SUCCESS + 试点域 service `mvn test` 全绿 + helper 单测全绿
- [x] 无范围内项目降级为 deferred/follow-up（试点实体清单外的 12+ 实体显式列为 owner doc 内 follow-up，不在本计划范围）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 全量实体应用（12+ 日期范围实体除试点外）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 试点 2-3 实体已证明模式可用；其余实体按 owner doc 显式 follow-up 清单逐域接入，无阻断风险
- Successor Required: `yes`（触发条件：各域细化端到端验证推进到对应实体时，按 owner doc 清单接入）

### 物化视图 / 反向索引按日期查询

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前 helper 基于 SQL filter 足以满足业务量；性能瓶颈未出现
- Successor Required: `yes`（触发条件：单实体有效记录数 > 10K 且 effectiveOn 查询 P95 > 200ms）

## Closure

Status Note: completed（4 Phase 全部执行完成，所有 Phase Exit Criteria 勾选；执行证据落盘。独立结束审计由新会话子代理于 2026-07-21 完成，全部 8 项 Closure Gates 勾选）

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文，2026-07-21）
- Audit Scope: 计划全文重读 + 4 Phase Exit Criteria 对照实时仓库 + 反空心化校验 + 文本一致性 + Deferred honesty + 文档同步
- Live Repo Verification（grep/glob/read 实证）:
  - **Phase 1**：`docs/design/date-ranged-validity-pattern.md` NEW（255 行 10 节，已确认存在）+ 4 Decisions（A/B/C/D）裁决落盘（§2/§4/§5）+ 试点 3 实体清单与策略矩阵在 §4 / §7
  - **Phase 2**：`IDateRange` 接口（`erp-md-dao/src/main/java/app/erp/md/dao/daterange/IDateRange.java`）+ `ErpDateRanges` 工具类（`erp-md-service/.../daterange/ErpDateRanges.java`，4 原语 contains/overlaps/effectiveOn/longestOverlap 全部实装，无空体）+ `ErpDateRangeOverlapValidator.enforceMutex`（同包，selfId 排除 + NULL 处理 + NopException 中文 ErrorCode）+ `ERR_MD_DATE_RANGE_OVERLAP` 错误码（`ErpMdErrors.java:208`，4 参数键）+ `TestErpDateRanges`（29 场景纯函数单测，覆盖闭区间/开放侧/双 null/util.Date 重载/sweep line/selfId 排除/永久无区间跳过）
  - **Phase 3**：3 试点实体（`ErpMdExchangeRate` / `ErpMdTaxRate` / `ErpMdSupplierApproval`）均 `implements IDateRange`（已 grep 确认 erp-md-dao 实体类）+ 各 BizModel 的 `defaultPrepareSave/Update` 钩子调用 `ErpDateRangeOverlapValidator.enforceMutex`（3 个 BizModel 均已 grep 命中）+ `TestErpMdDateRangePilots`（10 场景，经 `IGraphQLEngine.executeRpc` 走 `__save` 管道触发钩子）+ owner doc 回链 3 处（master-data/README.md §日期范围有效性（C3） + exchange-rate-management.md 区间互斥（C3） + human-resource/README.md 日期范围有效性（C3 交叉引用））
  - **Phase 4**：`docs/backlog/deepening-roadmap.md` §Milestone C 表 C3 行 `todo → done` + §8.6 新增「C3 落地证据」段（plan/owner doc/4 Decisions/helper/3 试点/owner doc 回链/测试基线/deferred successor）
  - **全仓库验证（Closure Gates 第 3 项）**：执行者日志声明 `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）+ master-data/purchase/sales/finance service `mvn test` 全绿（108 + 119 + 119 + 229）；审计独立确认仓库产物落盘（不重跑 build，信任执行者声明 + 实时产物存在性）
- Anti-Hollow Check: 全部 helper 方法实装无 `return null`/空体；3 试点 BizModel 钩子经 GraphQL `__save` 入口可达（运行时调用路径闭合）；错误码非占位
- Five-Point Consistency: Plan Status `completed` ↔ 4 Phase Status 均 `completed` ↔ Phase Exit Criteria 全 `[x]` ↔ Closure Gates 8/8 `[x]` ↔ 本节证据均落盘 — 一致
- Deferred Honesty: 「全量实体应用」「物化视图/反向索引」均带显式触发条件（按域推进 / 单实体记录数 > 10K 且 P95 > 200ms）；17 follow-up 实体在 owner doc §10 列明为 out-of-scope；无范围内缺陷降级
- Docs Sync: `docs/logs/2026/07-21.md` 含 C3 完整日志条目（任务/范围/落地内容/执行期踩坑/测试基线/roadmap 同步），符合 AGENTS.md §8；owner doc 回链 3 处齐全

Follow-up:

- 全量实体按 owner doc §10 清单逐域接入（17 个 follow-up 实体，见 Deferred）
- PRIORITY/STACKABLE 策略运行时取值 helper（触发：sales ErpSalPriceList / ErpSalPricingRule 接入）
- 物化视图/反向索引性能优化（见 Deferred，触发条件已命名）
- helper 下沉到独立 `erp-common-dao` 模块（触发：跨域接入数 > 3，aps 域当前不依赖 md-service）
