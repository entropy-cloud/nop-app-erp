# 2026-07-28-2130-1-audit-remediation-ma3-customization-verification MA3 可定制性验证（A3.8）

> Plan Status: completed
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA3（工作项 A3.8）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.5「可定制性验证」行（MA3 v2 新增维度，当前 `可定制性验证`）；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/open-ended-audit-prompt.md`（审计方法）；`docs/architecture/customization-capabilities.md`（roadmap 声明 owner doc——定制能力总览）；`docs/plans/2026-07-28-1953-2-audit-remediation-ma3-api-contract-consistency.md`（A3.6 同批 MA3——API 契约层，本审计聚焦定制能力实际可用性，互补不重叠）
> Audit: required

## Current Baseline

可定制性验证审计（文档-实现一致性层 MA3 最后一项，v2 新增 ERP 特定维度）。roadmap 工作项 A3.8 声明审查"可定制性验证（Delta 定制/扩展字段实际可用性 + 不破坏基线抽样）"，owner doc 标注 `docs/architecture/customization-capabilities.md`，skill `docs/skills/open-ended-audit-prompt.md`。

**关键基线事实（实时仓库核实）**：

- **定制能力总览 owner doc 存在且完整**（`docs/architecture/customization-capabilities.md`，230 行，8 能力 + 决策顺序 Model→Delta→Java + 定制场景决策矩阵 + 升级路径保护 + 与其他文档关系）。该文档是**声明性总览**——描述平台机制如何组合使用，但本项目是否实际**落地并验证**这些能力是本审计的对象。
- **Delta 定制实际落地情况**：实时仓库 `_vfs/_delta/` 仅 `app-erp-all/src/main/resources/_vfs/_delta/default/nop/auth/pages/` 下 2 个平台层 view delta（NopAuthOpLog/NopAuthSession），属于**对平台 nop-auth 模块的 UI 微调**，不是对业务域（finance/inventory/...）的定制。**全域 19 业务域零业务级 Delta 文件**（`find module-*/erp-*-*/src -path "*_delta*"` 0 命中业务域）。
- **扩展字段（EAV NopSysExtField）**：实时仓库未发现业务实体使用 EAV 扩展字段声明（`grep "extField\|extFields\|NopSysExtField" module-*/model/*.orm.xml` = 0 命中 EAV 扩展字段；注意 `ext:dict=` 为字典绑定标注非 EAV 路径）。
- **nop-dyn 动态实体**：实时仓库无 `NopDynEntity` 业务使用（`grep -rl "nop-dyn\|NopDynEntity" module-*/erp-*-service` 0 命中）。nop-dyn 是平台模块，本项目未配置动态实体。
- **BizLoader 计算字段**：实时仓库零 `@BizLoader` 业务使用（`grep -rl "@BizLoader" module-*/erp-*-service --include="*.java"` = 0 命中）。owner doc `customization-capabilities.md:169-184` 声明「能力六：BizLoader 与 GraphQL 扩展」并列举示例（采购"未交量"/物料"当前库存"/凭证"借贷平衡校验结果"），但均未以 `@BizLoader` 落地。
- **非下划线扩展层（保留层定制）**：业务域**广泛使用**保留层——finance 域 40 个非下划线 BizModel + 36 个手写 xbiz + 36 个手写 view.xml。这些是"扩展自己生成保留层"的合法定制，但非"定制别人基线"的 Delta。
- **模块化组装**：`app-erp-all` 聚合工程组装全部 18 域，Maven 依赖方向按 DAG 约束。模块裁剪能力存在但未验证。

**核心审计张力**：owner doc `customization-capabilities.md` 声明 nop-app-erp 是"产品化通用 ERP，充分利用扩展能力，快速适配各领域，**不改基线源码**"，并列举 8 能力 + 场景决策矩阵。但实时仓库中：(a) 业务级 Delta = 0；(b) EAV 扩展字段 = 0；(c) nop-dyn = 0。这意味着**定制能力是"平台提供但本项目未验证落地"的能力**——声明 vs 实证存在 gap。本审计需裁决：(1) 这些能力是否**实际可用**（抽样验证 Delta 合并/扩展字段持久化/保留层继承确实工作）；(2) 是否**不破坏基线**（定制层与基线分离、升级合并不冲突）；(3) owner doc 声明的"产品化可定制"承诺是否需要 owner doc 标注"当前为平台能力声明，落地验证为 successor"。

剩余差距：需要一次系统性可定制性实证审计。发现的 gap 分类为：(a) **能力声明但未实证落地**（major——owner doc 承诺的产品化能力缺实证，登记文档类 P1 目标 MR2）；(b) **保留层定制破坏基线风险**（major——保留层文件改动是否会随基线升级漂移）；(c) **模块裁剪未验证**（major——声明"按需组装"但无裁剪验证证据）；(d) **扩展字段/动态实体完全未使用**（note/major——平台能力未被项目利用，裁决是否为 gap）。本审计属 MA3 维度，全部 finding 目标 MR2（MR2 deps = MA3+MA4 done）；若发现定制层导致基线运行时破坏，升级标注走 P0 即时通道。本审计原则上不产生 P0（定制能力缺失是"能力未实证"非"活跃数据破坏"）。

## Goals

- 按 `open-ended-audit-prompt.md` 对 nop-app-erp 声明的 8 项定制能力做**实际可用性 + 不破坏基线**的抽样验证审计，产出审计报告。
- 验证覆盖核心维度：(1) Delta 定制合并机制是否实际可用（抽样构造一个业务域 Delta 验证合并生效）；(2) 扩展字段 EAV 是否可运行时持久化/读取（抽样验证）；(3) 保留层定制（非下划线 BizModel/xbiz/view）是否与生成基线正确继承且升级合并不冲突；(4) 模块化组装/裁剪是否可行（DAG 边界是否允许裁剪）；(5) BizLoader 计算字段实际可用性抽样。
- **裁决"产品化可定制"承诺状态**：owner doc `customization-capabilities.md` 声明的能力是"已验证落地"、"平台能力声明待落地"还是"声明过当"。
- scope matrix §2.5「可定制性验证」行终态标记（`可定制性验证` → `✅`/`⚠️(P1)`）。
- 发现的 major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总。roadmap A3.8 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做 Delta 定制机制的代码级重构/落地实现 — 若发现能力缺失，登记 P1 目标 MR2，不在本审计内修复。
- **不**做 API 契约层一致性 — 归 A3.6（已 done）。本审计聚焦定制能力的**实证可用性**。
- **不**做索引路由有效性 — 归 A3.7（已 done）。
- **不**做代码质量审计（BizModel/Processor 实现正确性）— 归 MA4（A4.1-A4.9）。本审计只验定制**机制**可用，不审业务逻辑正确性。
- **不**做权限/数据隔离 — 归 MA6（A6.1-A6.4）。定制能力审计不覆盖 orgId 隔离。
- **不**逐个审计全域 ~2890 单元测试 — 归 MA5。本审计的"不破坏基线"验证以**抽样构造定制 + 跑 codegen/build 确认合并生效 + 基线测试不回归**为证据，非全量测试审计。
- **不**手写业务级 Delta 作为生产产物（抽样验证构造的 Delta 在验证后清理，不提交为基线）。
- **不**手改生成物或 ORM 源模型。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/architecture/customization-capabilities.md`（定制能力总览——roadmap A3.8 owner doc）；`../nop-entropy/docs-for-ai/02-core-guides/delta-customization.md`（Delta 机制平台权威）；`../nop-entropy/docs-for-ai/02-core-guides/view-and-page-customization.md`（页面定制权威）；`../nop-entropy/docs-for-ai/03-modules/nop-sys.md`（扩展字段/字典权威）；`../nop-entropy/docs-for-ai/03-modules/nop-dyn.md`（动态实体权威）；`module-*/model/*.orm.xml`（保留层/BizModel/xbiz/view 基线源）
- Skill Selection Basis: `open-ended-audit-prompt.md`（roadmap A3.8 指定此 skill——开放式审计搜索"标准检查清单之外的隐藏问题"，适配可定制性主题时以"声明能力 vs 实证落地"为搜索框架，跨工件跨维度寻找虚假承诺/未验证路径/升级破坏风险。项目定制化层见 `docs/skills/README.md`）。与 A3.6 不同结果表面（定制能力实证 vs API 契约声明），独立计划。
- Verification: 审计不改生产代码/文档；抽样验证可能构造临时 Delta/扩展字段配置（验证后清理），故 Closure Gates 跑 `mvn clean install -DskipTests` 确认基线未被破坏（与同型审计 plan 一致）。能力 gap 修复在 MR2 批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为定制机制实证 + 声明-实证比对审查，不运行应用 UI。
- 抽样验证构造临时 Delta/扩展字段时，使用 `nop.debug=true` 查看合并来源标注（`_dump/`），验证后清理，不提交。
- **审计 plan 的 BUILD_VERIFY**：审计不改生产代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn clean install -DskipTests` 仅作基线未被破坏的回归确认（抽样清理后）。定制能力 gap 修复在 MR2 批量进行。

## Execution Plan

### Phase 1 - 8 项定制能力声明-实证系统性验证（开放式审计）

Status: completed
Targets: `docs/architecture/customization-capabilities.md`（8 能力声明）；实时仓库 Delta/扩展字段/保留层/nop-dyn/BizLoader/模块组装实证；`../nop-entropy/docs-for-ai/` 平台机制权威
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 平台合规审计 done（codegen 纪律已部分覆盖）；A3.1-A3.7 done（设计文档基线 + drift 已建立）。

- [x] 维度「Delta 定制实证」：核实业务域零 Delta 是否意味着"能力未验证"。抽样构造一个代表性业务域 Delta（如 finance 凭证模板科目覆盖 或 master-data 物料加列），用 `x:extends="super"` 验证合并生效（`_dump/` 来源标注 + `nop.debug=true`），验证后清理。标记：Delta 合并是否实际工作 / 是否破坏基线 / 业务域为何零 Delta（设计选择 vs 未落地）。
      - Skill: `open-ended-audit-prompt.md`
- [x] 维度「扩展字段 EAV 实证」：核实全域零 EAV 声明。抽样验证 NopSysExtField 是否可对一个代表性实体运行时持久化/读取（`extFields.fldX.string` 路径），裁决零使用是"能力未利用"还是"能力不可用"。
      - Skill: `open-ended-audit-prompt.md`
- [x] 维度「保留层定制（非下划线扩展层）升级安全性」：finance 40 个非下划线 BizModel + 36 xbiz + 36 view 广泛使用保留层。抽样验证保留层与生成基线的继承关系是否正确（`x:extends` 生成基线），评估基线升级时保留层定制是否会漂移/冲突（codegen 增量生成是否保留保留层）。
      - Skill: `open-ended-audit-prompt.md`
- [x] 维度「模块化组装/裁剪可行性」：owner doc 声明"按需组装/裁剪业务域"。核实 DAG 依赖边是否允许裁剪（如裁剪 manufacturing 后 finance 是否仍可构建），裁决"模块裁剪"是已验证可行还是声明未验证。
      - Skill: `open-ended-audit-prompt.md`
- [x] 维度「BizLoader 计算字段实证」：核实全域零 `@BizLoader` 业务使用。裁决零使用是"能力未利用"（owner doc 示例"未交量"/"当前库存"未以 @BizLoader 落地，改用其他机制实现）还是"能力不可用"，标记 owner doc 声明过当风险。
      - Skill: `open-ended-audit-prompt.md`
- [x] 维度「nop-dyn 动态实体」：核实零 nop-dyn 业务使用。裁决是否为 gap（owner doc 列为能力但项目未配置），或为合理设计选择（项目用 codegen ORM 而非动态实体）。
      - Skill: `open-ended-audit-prompt.md`
- [x] 维度「配置驱动（字典/参数/编码规则）」：核实字典（`ext:dict`）是否实际驱动 UI 下拉 + 编码规则是否可配。这是"改动成本极低"能力，应已广泛落地。
      - Skill: `open-ended-audit-prompt.md`
- [x] 维度「声明过当检测（开放式核心）」：跨工件比对 owner doc `customization-capabilities.md` 的"产品化通用 ERP + 不改基线源码 + 升级友好"承诺 vs 实证。标记：承诺但无实证的能力项 / 升级路径保护的实证缺口 / "不改基线源码"是否被保留层手写违反（保留层是扩展自己基线，不违反；但需裁决边界）。
      - Skill: `open-ended-audit-prompt.md`
- [x] 产出审计报告 `docs/audits/2026-07-28-2130-arm-ma3-customization-verification.md`（含：8 能力声明-实证矩阵 / 每能力裁决（已验证落地/平台声明待落地/声明过当）/ 抽样验证证据（Delta 合并/EAV 持久化/保留层继承）/ 升级路径风险 / blocker/major/minor/note finding 清单 / 裁决通过/失败 / 剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn clean install -DskipTests` 属 Closure Gates（见执行时规则 7）。

- [x] 8 能力逐项声明-实证裁决产出（每能力至少一句裁决，含"已验证落地"或"平台声明待落地"或"声明过当"）
- [x] 抽样验证证据产出（Delta 合并/EAV/保留层/BizLoader 至少各一项实测或裁决依据）
- [x] blocker/major/minor/note finding 清单产出，每个含能力项 / 严重性 / 声明-实证 gap 描述 / 影响 / 目标 MR

### Phase 2 - finding 汇总交接 MR2 + 索引/矩阵更新

Status: completed
Targets: 可定制性 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.5「可定制性验证」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] finding 汇总：全部 gap major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA3-NNN`，next = 057+、报告、能力项、gap 描述、目标 MR2、修复状态 todo）。与 A3.1-A3.7 已登记 P1-MA3-* 去重无冲突。
      - Skill: none
- [x] 分类裁决：全部 A3.8 finding 目标 MR2（MR2 deps = MA3+MA4 done）；定制层导致基线运行时破坏走 P0 即时通道，在报告中明确标注。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.5「可定制性验证」行终态标记（`可定制性验证` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 gap major 已登记 arm-index §P1 汇总（全部目标 MR2），待展开
- [x] 与 A3.1-A3.7 已登记 P1 经交叉去重无重复登记
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_056cc5e54ffetua76Radbzr56Z`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：基线声明全部经直接验证为真（Delta=0 / EAV=0 / nop-dyn=0 / finance 40 BizModel + 36 xbiz + 36 view / max P1-MA3-056 → next 057+ 无碰撞）；scope 匹配 A3.8 owner doc/skill/deps ✓；与 A3.6/A3.7 边界清晰 ✓；anti-slack 零禁词 ✓。**采纳 2 项非阻塞修订**：(1) 基线补 `@BizLoader` 计数（=0，全域零使用，owner doc 声明示例"未交量"/"当前库存"未以 @BizLoader 落地）+ 重述 Phase 1 BizLoader 维度为裁决非抽样；(2) 修正 EAV grep 证据措辞（`extField` 不匹配 `ext:dict=`）。两修订已落地。Plan Status 转 active。

## Closure Gates

> 本计划主体是定制能力声明-实证比对 + 抽样验证（不改生产代码；产出为审计报告 + arm-index/scope-matrix 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。能力 gap 修复在 MR2 批量进行（全部 A3.8 finding 目标 MR2）；定制层导致基线运行时破坏走 P0 即时通道。本审计只识别 gap + 分类。

- [x] 范围内行为完成（A3.8 可定制性验证审计报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [x] 已运行验证：抽样验证清理后 build 仅作基线未被破坏的回归确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### API 契约层一致性（A3.6）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计聚焦定制能力**实证可用性**；API 契约声明 vs 实现一致性归 A3.6（已 done）。若定制审计中发现契约层问题，标注交接 A3.6 复核。
- Successor Required: `no`——A3.6 已 done。

### 代码质量审计（MA4）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计验定制**机制**可用；BizModel/Processor 业务逻辑正确性归 MA4（A4.1-A4.9）。
- Successor Required: `yes`——MA4 执行时复核定制相关代码质量。

### 权限/数据隔离（MA6）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 定制能力审计不覆盖 orgId/角色隔离；Delta 定制是否会绕过权限是 MA6（A6.1-A6.4）范畴。
- Successor Required: `yes`——MA6 执行时复核 Delta 定制与权限交互。

## Closure

Status Note: 计划已执行完成。Phase 1（8 项定制能力声明-实证系统性验证）+ Phase 2（finding 汇总交接 MR2 + 索引/矩阵更新）全部 `[x]`。审计报告 `docs/audits/2026-07-28-2130-arm-ma3-customization-verification.md` 已产出（Verdict: GAPS，零 P0，5 项 P1-MA3-057~061 目标 MR2 文档类）。arm-index.md 已新增报告行 + P1 详细清单 5 行 + A3.8 完成摘要 + 去重说明。scope matrix §2.5「可定制性验证」行终态标记推进至 `⚠️(P1) done`。Closure Gate `mvn clean install -DskipTests` BUILD SUCCESS（exit 0，2026-07-28T23:12:03+08:00，154 reactor 模块全绿，基线未被破坏）。**MA3 文档-实现一致性层全部 8 工作项（A3.1-A3.8）现已全部 done**。结束审计由独立子代理执行（待执行）。

Closure Audit Evidence:

- **Independent Closure Audit (R3.5 Round 3 batch, 2026-07-31)** — Auditor: independent closure audit subagent (fresh session, cold-context; not the implementation agent). Verdict: **PASS**. Five-point consistency: (1) Plan Status `completed` ↔ Phase 1/2 Status 均 `completed` 一致；(2) Phase Status ↔ Exit Criteria 全 `[x]` 且每项均有可观察产物（审计报告 258 行 / arm-index +5 P1 行 / scope matrix 标记 / roadmap A3.8 done）一致；(3) Exit Criteria ↔ Closure Gates 全 `[x]` 一致；(4) Closure Gates ↔ 日志 `docs/logs/2026/07-28.md` line 12-20（plan 2130-1 A3.8 audit-only 条目，Verdict GAPS / 5 P1-MA3-057~061 / 变更清单 5 项 / MA3 全 8 项 done）一致；(5) Plan Status ↔ 日志 收尾声明一致。Anti-hollow: PASS——8 能力逐项裁决 + 5 维度抽样验证证据 + 5 P1 finding 各含能力项/严重性/gap 描述/影响/目标 MR，非空壳。Deferred honesty: PASS——A3.6 已 done（successor no）/ MA4（successor yes）/ MA6（successor yes）三 out-of-scope 项分类与 successor 标注完整，无隐藏降级。Live-repo spot-check: 审计报告 `docs/audits/2026-07-28-2130-arm-ma3-customization-verification.md` 存在（258 行，Verdict GAPS，P1-MA3-057~061 ✓）；`arm-index.md` 报告清单 line 49 done + P1 详细清单 line 274-278（P1-MA3-057~061 todo MR2）+ A3.8 完成摘要 line 103 + 去重说明 line 552 ✓；roadmap `docs/backlog/audit-remediation-roadmap.md` line 85 A3.8 `done`（MA3 全 8 项 done）✓；scope matrix `docs/audits/audit-remediation-scope-and-dimension-matrix.md` line 182「可定制性验证」行 `⚠️(P1) done` + line 176 完成声明段 ✓。基线声明：计划自报 `mvn clean install -DskipTests` BUILD SUCCESS exit 0 / 154 reactor 模块 / 2026-07-28T23:12:03（audit-only 零代码变更，抽样验证已清理，按同型审计 plan 实践未重跑）。注：本审计为 audit-producing plan（零代码变更），无可执行代码 diff 需复核；finding 全部目标 MR2 文档类（待 R2.9 展开），无范围内 deferred 隐藏。(Audit dispatch ref: `docs/plans/2026-07-31-1439-1-r3-5-closure-audit-round3-protected-area.md` Phase 2；R3.5 Round 3 batch backfill.)
- Evidence:
  - 审计报告：`docs/audits/2026-07-28-2130-arm-ma3-customization-verification.md`（230 行，含 8 能力声明-实证裁决矩阵 / 5 维度抽样验证证据 / 升级路径保护 5 项机制实证状态 / 声明过当检测 / 5 项 P1 finding 清单 / 裁决 GAPS + 剩余风险）
  - 索引更新：`docs/audits/arm-index.md`（报告清单 +1 行 line 49 / P1 详细清单 +5 行 P1-MA3-057~061 line 234-238 / A3.8 完成摘要 line 78 / 去重说明 line 405）
  - 矩阵更新：`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.5（A3.8 完成摘要 line 158 + 表格「可定制性验证」行终态标记 `⚠️(P1) done` line 165）
  - 基线回归：`mvn clean install -DskipTests` BUILD SUCCESS（exit 0，154 reactor 模块全绿，2026-07-28T23:12:03+08:00）——抽样验证未构造生产产物（plan Non-Goals），仅做基线未被破坏的回归确认
  - Finding ID 分配：P1-MA3-057~061（接续 A3.7 止于 P1-MA3-056，本批自 057 起，无碰撞）
