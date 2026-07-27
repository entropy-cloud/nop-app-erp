# 2026-07-27-1227-2-audit-remediation-ma1-platform-conformance-s-tier MA1 Nop 平台合规审计 — finance+mfg+hr（S 级，A1.11）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A1.11 Nop 平台合规审计 — finance+mfg+hr（S 级）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA1（工作项 A1.11）
> Related: `2026-07-27-1015-2-audit-remediation-ma1-orm-model-audit.md`（A1.1–A1.3 S 级 ORM 审计已完成，本计划是其平台合规后续）；`docs/skills/nop-platform-conformance-audit-prompt.md`（审计方法）；`2026-07-01-1900-1-platform-compliance-remediation.md`（前序平台合规偏差修复，已 completed）
> Audit: required

## Current Baseline

Nop 平台合规审计对照 `../nop-entropy/docs-for-ai/`（权威平台文档）核验 nop-app-erp 实现对平台最佳实践的遵循度。这是 MA1 中**全 19 域均为 `❓`（未审计）**的唯一维度（scope matrix §2.1 "Nop 平台合规" 行全 `❓`）。

S 级三域是全域最高复杂度域，平台合规风险密度最高（scope matrix §1.1）：

- **finance**：331 Java 文件 / 137 mutation / 36 Proc / 48 实体（业财一体核心，过账引擎 + 凭证 + 预算 + AR/AP + 成本 + 期间）。
- **manufacturing**：246 Java 文件 / 74 mutation / 21 Proc / 41 实体（工单 + MRP + BOM + 工艺路线）。
- **hr**：127 Java 文件 / 92 mutation / 1 Proc / 42 实体（员工组织 + 考勤 + 工资 + 排班）。

前序平台合规修复（`2026-07-01-1900-1`）已修复一批系统性偏差（D2 BizModel 安全 API / D5 移除多余 IOrmTemplate 等），合规基线 checker 19 规则已落锚（M0.3 实测全 actual ≤ baseline，0 漂移）。但合规基线是**机械化 grep 规则**，不等同于 `nop-platform-conformance-audit-prompt.md` 的 **15 维度语义审计**（含 Model→Delta→Java 决策顺序、跨实体访问规则、异常处理、IoC/事务、平台辅助工具、标准服务模式、状态机/规则引擎、审批流/作业、定制能力顺序、多租户/本地化、codegen 产物安全、聚合完整性、owner-doc→代码漂移抽样）。

剩余差距：S 级三域从未做过 15 维度平台合规审计；潜在 major/blocker（硬编码能模型化的逻辑、绕过 I*Biz 直接 IDaoProvider、@BizMutation 冗余 @Transactional、owner-doc 与代码漂移）待发现。本计划覆盖 A1.11（finance + mfg + hr），A1.12（A 级核心）/ A1.13（B+C 合并）为后续 plan。

## Goals

- 按 `nop-platform-conformance-audit-prompt.md` 15 维度对 finance / manufacturing / hr 三域做系统性平台合规审计，产出审计报告（按域组织）。
- 维度 15（owner-doc→代码关键断言抽样）每域至少 2 个 owner doc × 2 个断言 = 4 核查点；若发现 ≥2 处漂移，扩大抽样。
- scope matrix §2.1 "Nop 平台合规" 行 finance/mfg/hr 三列 `❓` → `✅`/`⚠️(P1)`。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A1.11 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A 级核心域（purchase/sales/assets/inventory）— A1.12，不同域簇、不同 owner doc，留作后续 plan。
- **不**审计 B+C 级域（crm/qa/prj/cs/ct/b2b/mnt/drp/md/aps/log/notify）— A1.13。
- **不**审计 MA1 跨模块依赖（A1.10，不同 skill）/ 架构治理复审（A1.14）。
- **不**审计 MA2–MA7 维度。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**重开 D1（字典 int→string）— 已裁决 Deferred（MA1 ORM plan 登记已知 deferred）。本审计若复现 D1 相关模式，标注为已知 deferred 不重复裁决。
- **不**手改生成物（`_gen/`、`_` 前缀、`_app.orm.xml`、`_service.beans.xml`、`__XGEN_FORCE_OVERRIDE__`）。任何源变更（P0 即时修复）须改保留层文件 + `mvn clean install -DskipTests`。
- **不**重写平台文档（`../nop-entropy/docs-for-ai/`）— 平台文档是审计依据，非审计对象。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `../nop-entropy/docs-for-ai/INDEX.md`（平台文档路由）；`../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`（Model→Delta→Java 决策框架 + 反模式表）；`../nop-entropy/docs-for-ai/02-core-guides/{architecture-principles,domain-logic-and-ddd,cross-module-entity-reference}.md`；`../nop-entropy/docs-for-ai/04-reference/{common-java-helpers,safe-api-reference}.md`；项目 `docs/architecture/{system-baseline,module-boundaries,customization-capabilities}.md`；各域 `docs/design/{finance,manufacturing,human-resource}/`
- Skill Selection Basis: `nop-platform-conformance-audit-prompt.md`（roadmap A1.11 指定此 skill，平台合规专用方法，15 维度，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及源码，则该修复需 `mvn clean install -DskipTests`。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。
- **保护区域门控**：finance 是会计/财务保护区域，hr 涉及员工数据。P0 即时修复若触及保护区域行为（过账/凭证/数据删除/工资计算），须有 owner doc 描述预期行为 + 人工/任务驱动授权 + 该修复子切片独立审计。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - finance + manufacturing + hr 平台合规 15 维度审计（含自动化 grep + 语义抽样）

Status: planned
Targets: `module-finance/`、`module-manufacturing/`、`module-hr/` 下全 `{domain}-service`/`{domain}-dao`/`{domain}-web` Java 文件（scope matrix §1.1 指标 finance 331 / mfg 246 / hr 127；含 `_gen/` 实测 find 计数更高——438/311/182，MV 验证里程碑需重跑 §1.1 数据）；各域 `*.orm.xml`、`*.xbiz.xml`、`docs/design/<domain>/`
Skill: `nop-platform-conformance-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线 + compliance 锚点）；A1.1–A1.3 S 级 ORM 审计 done（ORM 层已 0 blocker，本计划在其上做实现层平台合规审计）

- [ ] 自动化 grep 扫描三域源码，覆盖 skill 列出的机械化规则：`extends RuntimeException`（应 NopException）、`@Inject private`（应非 private）、`@Transactional` 与 `@BizMutation` 共存（冗余）、`System.currentTimeMillis`（应 CoreMetrics）、`IDaoProvider` 直接注入（应 I*Biz）、`_gen/` 是否被手改（git diff）、跨模块 refEntityName 无 notGenCode 声明、`__XGEN_FORCE_OVERRIDE__` 手改。产出三域反模式实例清单。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] finance 15 维度审计：维度 1 决策顺序（Model→Delta→Java，能模型化却硬编码 Java）/ 2 跨实体访问（@Inject I*Biz vs IDaoProvider）/ 3 异常处理（NopException + ErrorCode）/ 4 IoC 与事务（@Inject 非 private、@BizMutation 不重复 @Transactional）/ 5 平台辅助工具 / 6 标准服务模式（CrudBizModel + @BizQuery/@BizMutation + @BizLoader）/ 7 机制 B / 8 状态机与规则引擎（科目映射用 nop-rule）/ 9 审批流与作业（nop-wf/nop-job）/ 10 定制能力顺序（Delta ext）/ 11 多租户与本地化 / 12 测试 / 13 codegen 产物安全 / 14 聚合完整性（app.action-auth.xml x:extends）。重点关注业财过账三件套与多币种四件套的平台规范遵循。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] manufacturing 15 维度审计（同上维度集）。重点关注工单状态机（声明式 vs if-else）、MRP/BOM 计算引擎的平台辅助工具使用、制造-质量跨域事件（nop-message）。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] hr 15 维度审计（同上维度集）。重点关注工资核算（扣税规则用 nop-rule）、排班引擎、员工/组织双表（`erp_hr_employee` vs `erp_md_employee` 已登记 deferred）。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] 维度 15 owner-doc→代码漂移抽样：finance（`docs/design/finance/posting.md` + `state-machine.md` 各抽 2 断言）、mfg（`docs/design/manufacturing/` + `mrp.md` 各抽 2 断言）、hr（`docs/design/human-resource/` 抽 2 owner doc × 2 断言）。对照 orm.xml 字典值 / BizModel 常量 / `*Errors.java` 核验。不一致即报 Major；若单域 ≥2 处漂移扩大抽样。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] 产出审计报告 `docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-s-tier.md`（按 finance/mfg/hr 组织，含：15 维度合规率、反模式实例清单、owner-doc 漂移核查点记录、finding 按 P0/P1/P2 分级、残留风险）。
      - Skill: none

Exit Criteria:

- [ ] finance/mfg/hr 三域 15 维度均有结论，自动化 grep 清单 + 语义抽样均产出
- [ ] 维度 15 每域 ≥4 核查点已记录（或发现 ≥2 漂移已扩大抽样）
- [ ] 报告产出，解除 MA2 业务审计对实现层平台合规的依赖

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: planned
Targets: 三域平台合规审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.1
Skill: none

- Item Types: `Fix | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（手改生成代码 / 跨模块写反向 / 业务异常不扩展 NopException / @Inject private 致 IoC 失败）当即就地修复（改保留层源 + `mvn clean install -DskipTests` + 该修复独立审计，保护区域修复须额外授权）或异步注入 fix plan。P0 永不进入 MR。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA1-NNN`，续 MA1 ORM 批次序号），供 R1.0 展开机制转化为具体修复工作项行。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.1 "Nop 平台合规" 行 finance/mfg/hr 三列 `❓` → `✅`/`⚠️(P1)`。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [ ] arm-index 报告清单 + scope matrix §2.1 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is**（`ses_05e2827dfffeq6wMK0dmT1RmfM`，独立 general 子代理，对照实时仓库逐项复核）。VERDICT = acceptable-as-is，**无 BLOCKER**。核实要点：S 级三模块目录与标准链存在；6 份平台文档 + 各域 owner docs（posting.md/state-machine.md/mrp.md）全部存在；scope matrix §2.1 "Nop 平台合规" 行全 19 列 ❓（未审计）；compliance checker（19 规则机械化 grep）与 15 维度语义审计明确区分；前序 `2026-07-27-1015-2` ORM plan completed、D1 deferred 真实、`2026-07-01-1900-1` 平台合规修复 plan 存在；15 维度全覆盖（含 dim 13 `__XGEN_FORCE_OVERRIDE__`/`_gen/` git diff + dim 14 app.action-auth.xml x:extends + dim 15 ≥4 核查点/域并 ≥2 漂移扩大抽样）；从 A1.12/A1.13 拆分正当（同 skill 同维度但不同域簇/层级，平台合规每域负载重于 ORM 机械审计）；Exit Criteria 本地化；BUILD_VERIFY 审计纪律 + 保护区域门控（finance 会计 + hr 员工数据）齐全。采纳的非阻塞修正：Targets 行 Java 计数补充 `_gen/` 实测差异注记（scope matrix §1.1 数据 331/246/127 vs find 438/311/182，含生成文件；MV 需重跑 §1.1）以防审查者混淆。已完成。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。

- [ ] 范围内行为完成（A1.11 finance/mfg/hr 三域平台合规 15 维度审计报告产出 + arm-index 更新 + scope matrix §2.1 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix 已反映审计结论）
- [ ] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test` 作回归基线确认；若有 P0 即时修复则该修复子切片独立验证
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 不得降级为 MR）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志都一致
- [ ] 独立结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### D1 字典 valueType int→string（已知 deferred，审计中复现不重开）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 已由 `2026-07-02-0900-1` Phase 5 裁决 Deferred（成本/静默回归主导，触发条件=业财一体打通前/跨系统集成启动时）。平台合规审计若复现 D1 相关模式（如字典 int 类型偏离），标注为已知 deferred，引用前序裁决，不重复裁决、不升级为 P1。
- Successor Required: `yes`——前序计划已命名触发条件。

### hr 双员工表（`erp_hr_employee` vs `erp_md_employee`）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 已在 MA1 ORM 审计（A1.3 hr）登记 deferred successor。平台合规审计复核其当前状态，不重复裁决。
- Successor Required: `yes`——hr 域设计深化时统一。

## Closure

Status Note: _（待执行后填充）_

Closure Audit Evidence:

- _（待独立结束审计后填充）_

Follow-up:

- P1 finding 经 R1.0 展开机制进入 MR1
- 若 P0 即时修复注入 fix plan，该 fix plan 独立 closure
