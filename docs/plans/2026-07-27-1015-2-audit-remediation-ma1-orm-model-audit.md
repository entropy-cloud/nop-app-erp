# 2026-07-27-1015-2-audit-remediation-ma1-orm-model-audit MA1 ORM 模型审计批（A1.1–A1.9）

> Plan Status: active
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA1（工作项 A1.1 / A1.2 / A1.3 / A1.4 / A1.5 / A1.6 / A1.7 / A1.8 / A1.9）
> Related: `2026-07-27-1015-1-audit-remediation-m0-baseline.md`（前置依赖 M0.3）；`docs/skills/orm-model-audit-prompt.md`（审计方法）；`docs/plans/2026-07-02-0900-1-audit-remediation.md`（前一轮 ORM 偏差审计 D1–D5，已修复/裁决）
> Audit: required

## Current Baseline

ORM 模型（`module-<domain>/model/app-erp-<domain>.orm.xml`，19 文件）是 nop-app-erp 的持久化真相源，经 codegen 驱动 dao/meta/service/web/app/api 全链。前序最佳实践合规审计（`2026-07-02-0900-1`）已修复/裁决 D1–D5 类系统性 ORM 偏差：

- D2（BizModel helper 走安全 API）、D5（移除多余 IOrmTemplate）— 代码层已完成。
- D3（业务动作字段 `stdDomain="userId"`）、D4（amount precision=18 scale=2 / boolFlag BOOLEAN）— ORM 已对齐，10 域 58 列补 stdDomain、17 域 amount + 18 域 boolFlag 调整。
- D1（字典 `valueType="int"`→`string`）— 已裁决 Deferred（成本/静默回归主导），前向指导落地（新域字典用 string）。

但前序审计是**偏差修复**视角（针对 D1–D5 已知问题），未做**全量规范与完整性审计**。MA1 ORM 审计（A1.1–A1.9）是路线图首次按 `orm-model-audit-prompt.md` 的 9 维度（类型规范 / 长度精度 / 字典设计 / 标准字段完整性 / 业务字段完整性 / 关系设计 / 跨模块引用一致性 / 命名一致性 / 需求覆盖）对全 19 域 ORM 做系统性审计，覆盖 scope matrix §3.2 残留风险中"daoFor Type 4 设计边界错误（~10-30 处）阻塞 successor"等进入 MA1 的输入项。

复杂度分级（scope matrix §1.2）决定审计批次：ORM 审计为机械维度（字段/类型/关系检查，不需理解业务语义），S 级域可整域审计（单次会话可完成），故按 S / A / B+C 三批组织，全域覆盖。

剩余差距：19 域 ORM 尚未做全量 9 维度审计；潜在 blocker/major finding（关系断裂、标准字段缺失影响业财一体、字典规范违规、跨域命名冲突）待发现并记录。

## Goals

- 按 `orm-model-audit-prompt.md` 9 维度对全 19 域 ORM 模型完成系统性审计，产出审计报告（按 S/A/B+C 批次组织），覆盖 scope matrix §2.1 的 "ORM 模型规范" 行剩余 `❓`→`✅/⚠️`（注：master-data 行已为 `⚠️`，审计复核其当前状态）。
- 发现的 P0 finding 经即时通道就地修复或异步注入 fix plan；P1 finding 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。
- 将 roadmap MA1 工作项 A1.1–A1.9 推进至 `done`（经独立 closure audit 通过）。

## Non-Goals

- **不**审计 MA1 的其他维度（A1.10 跨模块依赖 DAG / A1.11–A1.13 平台合规 / A1.14 架构治理）— 不同 skill、不同结果表面，留作后续 plan。
- **不**审计 MA2–MA7 维度（业务正确性 / 文档一致 / 代码质量 / 测试 / 安全 / 运维）。
- **不**在本计划内批量修复 P1 finding — P1 经 R1.0 展开机制进入 MR1 批量修复（roadmap §R*.0 展开机制）。仅 P0 走即时通道。
- **不**手改生成物（`_gen/`、`_` 前缀、`_app.orm.xml`、`_service.beans.xml`）。任何 ORM 变更（P0 即时修复）须改 `model/*.orm.xml` 源 + `mvn clean install -DskipTests` 增量重生成。
- **不**重开 D1（字典 int→string）— 已裁决 Deferred，触发条件未满足。ORM 审计若再次发现该问题，标注为已知 deferred 不重复裁决。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/domain-design-guidelines.md`（"单据标准字段约定" + "状态机命名与跨域映射规范"）；`docs/design/erp-design-audit-checklist.md`；`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`（平台 ORM 规范）；`../nop-entropy/schema/entity.xdef`（属性权威定义）；各域 `docs/design/<domain>/`
- Skill Selection Basis: `orm-model-audit-prompt.md`（roadmap A1.1–A1.9 全部指定此 skill，机械 ORM 审计，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及 ORM，则该修复需 `mvn clean install -DskipTests`。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。
- **保护区域门控**：ORM 模型（`model/*.orm.xml`）是 ask-first 保护区域。P0 即时修复若触及 ORM，须有 owner doc 描述预期行为 + 人工/任务驱动授权 + 该修复子切片的独立审计。D3/D4 类 stdDomain/精度变更已由 `2026-07-02-0900-1` 授权范式确立。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 mvn test 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - S 级域 ORM 审计（A1.1 finance / A1.2 manufacturing / A1.3 hr）

Status: planned
Targets: `module-finance/model/app-erp-finance.orm.xml`（48 实体）、`module-manufacturing/model/app-erp-manufacturing.orm.xml`（41 实体）、`module-hr/model/app-erp-hr.orm.xml`（42 实体）
Skill: `orm-model-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线 + compliance 锚点）；本计划在 M0（`2026-07-27-1015-1`）完成后启动

- [ ] 按 `orm-model-audit-prompt.md` 9 维度审计 finance ORM（S 级整域，机械维度单次会话可完成），对照 `docs/design/finance/` + `domain-design-guidelines.md` 单据标准字段约定。重点关注：业财过账三件套（posted/postedAt/postedBy）、多币种四件套（currencyId/exchangeRate/amountSource/amountFunctional）、GL 映射与科目表、跨模块引用（机制 B notGenCode 外部实体）。
      - Skill: `orm-model-audit-prompt.md`
- [ ] 按 9 维度审计 manufacturing ORM（S 级整域）。重点关注：工单/BOM/工艺路线/工作中心、质量集成跨域引用、生产成本回写字段。
      - Skill: `orm-model-audit-prompt.md`
- [ ] 按 9 维度审计 hr ORM（S 级整域）。重点关注：员工与组织（双员工表 `erp_hr_employee` vs `erp_md_employee` 已登记 deferred successor）、考勤/工资/排班字段完整性。
      - Skill: `orm-model-audit-prompt.md`
- [ ] 产出 S 级域 ORM 审计报告 `docs/audits/2026-07-27-1015-arm-ma1-s-tier-orm.md`（含裁决、各维度通过率、finding 按 P0/P1/P2 分级、残留风险），并更新 `arm-index.md`（报告清单 + P0/P1 汇总）。
      - Skill: none

Exit Criteria:

- [ ] finance / mfg / hr 三域 ORM 审计报告产出，9 维度均有结论
- [ ] 报告已登记 arm-index（解除 MA2 业务审计对 ORM 完整性的依赖的本地化检查）

### Phase 2 - A 级域 ORM 审计（A1.4 purchase+sales / A1.5 assets+inventory / A1.6 crm+quality+projects）

Status: planned
Targets: purchase（32 实体）、sales（27 实体）、assets（24 实体）、inventory（31 实体）、crm（39 实体）、quality（21 实体）、projects（21 实体）的 `model/*.orm.xml`
Skill: `orm-model-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: Phase 1 完成（S 级范式确立后 A 级按同法执行）

- [ ] 审计 purchase + sales ORM（A 级 2 域合并）。重点关注：双轴状态分离（docStatus + approveStatus）、价税分离字段、退货/退款单据完整性。
      - Skill: `orm-model-audit-prompt.md`
- [ ] 审计 assets + inventory ORM（A 级 2 域合并）。重点关注：assets 折旧/处置/资本化字段、inventory 成本方法（7 种 costMethod）、批次/序列号追踪字段、库存移动类型。
      - Skill: `orm-model-audit-prompt.md`
- [ ] 审计 crm + quality + projects ORM（A 级 3 域合并）。重点关注：crm 漏斗/预测/配额、quality 检验/NCR/CAPA/SPC、projects 工时/结算/成本归集。
      - Skill: `orm-model-audit-prompt.md`
- [ ] 产出 A 级域 ORM 审计报告（按域簇组织），更新 arm-index。
      - Skill: none

Exit Criteria:

- [ ] 7 A 级域 ORM 审计完成，报告产出并登记 arm-index

### Phase 3 - B+C 级域 ORM 审计（A1.7 master-data / A1.8 cs+contract+b2b+maintenance+drp / A1.9 aps+logistics+notify）

Status: planned
Targets: master-data（25 实体，DAG 根域）、cs（18）、contract（19）、b2b（16）、maintenance（20）、drp（16）、aps（7）、logistics（12）、notify（3）的 `model/*.orm.xml`
Skill: `orm-model-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: Phase 2 完成

- [ ] 审计 master-data ORM（B 级，DAG 根域单独）。重点关注：被全域引用的根实体（partner/material/subject/facility/org）字段完整性、`erp-md/*` 字典被全域复用的一致性。
      - Skill: `orm-model-audit-prompt.md`
- [ ] 审计 cs + contract + b2b + maintenance + drp ORM（B 级 5 域合并）。重点关注：cs 工单/SLA/满意度、contract 版本/发票计划/返利、b2b EDI 文档、maintenance 设备/巡检/备件、drp 需求计划/安全库存。
      - Skill: `orm-model-audit-prompt.md`
- [ ] 审计 aps + logistics + notify ORM（C 级 3 域合并）。重点关注：aps 排产/工序单、logistics 发运/承运商、notify 跨域通知派发子系统。
      - Skill: `orm-model-audit-prompt.md`
- [ ] 产出 B+C 级域 ORM 审计报告，更新 arm-index。汇总全域 ORM 维度覆盖率至 scope matrix §2.1（`❓`→`✅/⚠️`）。
      - Skill: none

Exit Criteria:

- [ ] 9 B+C 级域 ORM 审计完成，报告产出并登记 arm-index
- [ ] scope matrix §2.1 "ORM 模型规范" 行全域标记完成

### Phase 4 - P0 即时通道处理 + P1 汇总交接 MR1

Status: planned
Targets: 全域 ORM 审计发现的 P0/P1 finding
Skill: none

- Item Types: `Fix | Follow-up`
- Prereqs: Phase 1–3 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0 当即就地修复（改 `model/*.orm.xml` 源 + `mvn clean install -DskipTests` + 该修复的独立审计）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA1-NNN`、报告、描述、目标 MR1、修复状态 todo），供 R1.0 展开机制转化为具体修复工作项行。
      - Skill: none
- [ ] 确认 scope matrix §3.2 "daoFor Type 4 设计边界错误" 等进入 MA1 的残留风险项已在审计中被覆盖并给出结论（已确认/已修复/归 successor）。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is**（`ses_05ea4289bffeatiwJ3n8zGh7GX`，独立 general 子代理，对照实时仓库逐文件复核）。VERDICT = acceptable-as-is，**无 BLOCKER**。核实要点：19 个 ORM 文件全部存在；finance/mfg/hr 等 19 域实体计数与 scope matrix §1.1 精确一致；D1–D5 状态声称与已完成前序计划 `2026-07-02-0900-1` 一致；A1.1–A1.9 作为单一结果表面（同 skill `orm-model-audit-prompt.md`、同维度、同交付类型）按 S/A/B+C 复杂度分层组织正确；Exit Criteria 已本地化（无重复全仓库验证）；Phase 4 P0 即时通道 + P1→MR1/R1.0 交接忠实实现 roadmap 纪律；audit BUILD_VERIFY 声明、ORM ask-first 保护区域门控、D1 已知 deferred 处理、A1.10–A1.14 正确排除（不同 skill/结果表面）均合规；无 anti-slack 违规；M0.3 依赖正确陈述未虚假声称就绪。采纳的非阻塞修正：(1) Phase 1 Prereqs "本计划 Phase 0 = M0 完成后启动"措辞订正为"本计划在 M0 完成后启动"（计划无 Phase 0）；(2) Goals "全部 ❓→✅/⚠️"放宽为"剩余 ❓→✅/⚠️（master-data 已 ⚠️ 复核当前状态）"。两项均已完成。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间的任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。

- [ ] 范围内行为完成（A1.1–A1.9 全 19 域 ORM 审计报告产出 + arm-index 更新 + scope matrix §2.1 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix 已反映审计结论）
- [ ] 已运行验证：若 P0 即时修复触及 ORM → `mvn clean install -DskipTests`（+ 受影响域 `mvn test`）；若零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test` 作回归基线确认
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 不得降级为 MR）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志都一致
- [ ] 独立结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### D1 字典 valueType int→string（已知 deferred，审计中复现不重开）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 已由 `2026-07-02-0900-1` Phase 5 裁决 Deferred（成本/静默回归主导，触发条件=业财一体打通前/跨系统集成启动时）。ORM 审计若复现，标注为已知 deferred，引用前序裁决，不重复裁决、不升级为 P1。
- Successor Required: `yes`——前序计划已命名触发条件。

## Closure

Status Note: _（待结束审计填充）_

Closure Audit Evidence:

- Auditor / Agent: _（独立子代理，新会话）_
- Evidence: _（task id / 审计报告 / arm-index / closure audit 记录）_

Follow-up:

- P1 finding 经 R1.0 展开机制进入 MR1（roadmap §R*.0 展开机制）
- 若 P0 即时修复注入 fix plan，该 fix plan 独立 closure
