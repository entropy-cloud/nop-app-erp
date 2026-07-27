# 2026-07-27-1227-3-audit-remediation-ma1-platform-conformance-a-tier-core MA1 Nop 平台合规审计 — pur+sal+assets+inv（A 级核心，A1.12）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A1.12 Nop 平台合规审计 — pur+sal+assets+inv（A 级核心）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA1（工作项 A1.12）
> Related: `2026-07-27-1227-2-audit-remediation-ma1-platform-conformance-s-tier.md`（A1.11 S 级平台合规，同 skill 同维度不同域簇，建议先执行以确立范式）；`2026-07-27-1015-2-audit-remediation-ma1-orm-model-audit.md`（A1.4–A1.6 A 级 ORM 审计已完成）；`docs/skills/nop-platform-conformance-audit-prompt.md`（审计方法）；`2026-07-01-1900-1-platform-compliance-remediation.md`（前序平台合规偏差修复）
> Audit: required

## Current Baseline

本计划是 MA1 平台合规维度的第二批，覆盖 A 级核心四域（purchase / sales / assets / inventory）。scope matrix §2.1 "Nop 平台合规" 行此四列均为 `❓`（未审计）。A 级核心域复杂度仅次于 S 级（scope matrix §1.1）：

- **purchase**：187 Java 文件 / 34 mutation / 45 Proc / 32 实体 / 29 状态字段（采购到付款链路起点）。
- **sales**：162 Java 文件 / 30 mutation / 47 Proc / 27 实体 / 25 状态字段（销售到收款链路起点）。
- **assets**：176 Java 文件 / 61 mutation / 48 Proc / 24 实体 / 18 状态字段（折旧引擎 48 Processor 全域最高密度）。
- **inventory**：179 Java 文件 / 36 mutation / 18 Proc / 31 实体 / 19 状态字段（库存核算 + 业财一体写库存核心）。

前序平台合规修复（`2026-07-01-1900-1`）已修复 D2/D5 系统性偏差；合规基线 checker 19 规则已落锚（M0.3 实测全 actual ≤ baseline）。MA1 ORM 审计（A1.4–A1.6）已确认此四域 ORM 层 0 blocker（assets 有 29 列 propId 缺失 P1 待 MR1，crm DECIMAL↔double P1 不在本计划域内）。但 15 维度语义平台合规审计从未在此四域执行。

A 级核心四域是业财一体闭环（采购→入库→凭证 / 销售→发货→凭证）的业务侧支柱，且 assets 折旧引擎与 inventory 库存核算是高风险财务区域。平台合规偏差（硬编码能模型化的状态流转、绕过 I*Biz 直接 IDaoProvider 写库存、@BizMutation 冗余事务）会直接影响业财一体正确性。

剩余差距：A 级核心四域 15 维度平台合规审计待执行；潜在 major/blocker 待发现。本计划覆盖 A1.12，A1.13（B+C 合并）为后续 plan。

## Goals

- 按 `nop-platform-conformance-audit-prompt.md` 15 维度对 purchase / sales / assets / inventory 四域做系统性平台合规审计，产出审计报告（按域组织）。
- 维度 15（owner-doc→代码关键断言抽样）每域至少 2 个 owner doc × 2 个断言 = 4 核查点；若发现 ≥2 处漂移，扩大抽样。
- 重点核验业财一体写路径的平台规范遵循：purchase/sales 过账同事务 S 写 finance + inventory（@BizMutation 单方法原子提交，不依赖显式传播）；inventory 库存扣减走 I*Biz 写方法不绕过；assets 折旧 Processor 链路（48 Processor）的平台规范。
- scope matrix §2.1 "Nop 平台合规" 行 pur/sal/assets/inv 四列 `❓` → `✅`/`⚠️(P1)`。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A1.12 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 S 级域（finance/mfg/hr）— A1.11，先执行以确立范式。
- **不**审计 B+C 级域（crm/qa/prj/cs/ct/b2b/mnt/drp/md/aps/log/notify）— A1.13。
- **不**审计 MA1 跨模块依赖（A1.10）/ 架构治理复审（A1.14）。
- **不**审计 MA2–MA7 维度（业财端到端业务正确性归 MA2 A2.1/A2.2/A2.4；状态机正确性归 MA2 A2.8–A2.11；assets 折旧引擎代码质量归 MA4 A4.3 专属审计）。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**重开 D1（字典 int→string）— 已裁决 Deferred。复现标注为已知 deferred 不重复裁决。
- **不**重开 assets propId 缺失 P1（P1-MA1-008）— 已登记 arm-index 待 MR1。本审计复核其平台合规影响但不重复登记。
- **不**手改生成物（`_gen/`、`_` 前缀、`_app.orm.xml`、`_service.beans.xml`、`__XGEN_FORCE_OVERRIDE__`）。任何源变更（P0 即时修复）须改保留层文件 + `mvn clean install -DskipTests`。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `../nop-entropy/docs-for-ai/INDEX.md`；`../nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`（Model→Delta→Java 决策框架）；`../nop-entropy/docs-for-ai/02-core-guides/{architecture-principles,domain-logic-and-ddd,cross-module-entity-reference,concurrency-and-transactions}.md`（A 级核心涉及并发写库存/发票核销）；`../nop-entropy/docs-for-ai/04-reference/{common-java-helpers,safe-api-reference}.md`；项目 `docs/architecture/{system-baseline,module-boundaries,customization-capabilities,integration-and-transaction-patterns}.md`；各域 `docs/design/{purchase,sales,assets,inventory}/`
- Skill Selection Basis: `nop-platform-conformance-audit-prompt.md`（roadmap A1.12 指定此 skill，与 A1.11 同 skill 同维度，A 级核心域簇）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及源码，则该修复需 `mvn clean install -DskipTests`。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。
- **保护区域门控**：assets 折旧/处置（影响财务报表）、inventory 库存（业财一体写）、purchase/sales 过账（会计凭证）均触及会计/财务保护区域。P0 即时修复若触及保护区域行为，须有 owner doc 描述预期行为 + 人工/任务驱动授权 + 该修复子切片独立审计。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - pur + sal + assets + inv 平台合规 15 维度审计（含自动化 grep + 语义抽样）

Status: planned
Targets: `module-purchase/`、`module-sales/`、`module-assets/`、`module-inventory/` 下全 `{domain}-service`/`{domain}-dao`/`{domain}-web` Java 文件（pur 187 / sal 162 / assets 176 / inv 179）；各域 `*.orm.xml`、`*.xbiz.xml`、`docs/design/<domain>/`
Skill: `nop-platform-conformance-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线 + compliance 锚点）；A1.4–A1.6 A 级 ORM 审计 done（ORM 层已 0 blocker）；建议 A1.11（S 级平台合规）先执行以确立 15 维度范式，本计划复用其报告模板与 grep 脚本

- [ ] 自动化 grep 扫描四域源码，覆盖 skill 列出的机械化规则：`extends RuntimeException`、`@Inject private`、`@Transactional` 与 `@BizMutation` 共存、`System.currentTimeMillis`、`IDaoProvider` 直接注入、`_gen/` 手改、跨模块 refEntityName 无 notGenCode 声明、`__XGEN_FORCE_OVERRIDE__` 手改。另核验维度 14 聚合完整性：`grep 'x:extends' app-erp-all/.../app.action-auth.xml` 确认四域已注册 + 聚合 app POM 含四域依赖（已落地域为确认性核查）。产出四域反模式实例清单。复用 A1.11 的 grep 脚本（若已产出）。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] purchase 15 维度审计。重点关注：采购到付款链路（PO→Receive→Invoice→Pay）的过账同事务 S 写 finance+inventory 平台规范（@BizMutation 单方法原子提交）、双轴状态分离（docStatus+approveStatus）声明式实现、价税分离字段。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] sales 15 维度审计。重点关注：销售到收款链路（SO→Delivery→Invoice→Receipt）的过账平台规范、并发扣批次（UC-SAL-10 已标记并发缺口，归 MA2 A2.17，本审计仅核验其乐观锁/事务边界平台规范）、退货/退款单据。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] assets 15 维度审计。重点关注：折旧引擎 48 Processor 链路的平台规范（Processor 注册/xbiz 声明/错误处理）、处置/资本化/价值调整的过账平台规范、折旧正确性影响财务报表的高风险性。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] inventory 15 维度审计。重点关注：库存移动类型、成本方法（7 种 costMethod）声明式实现、库存扣减走 I*Biz 写方法不绕过 IDaoProvider、批次/序列号追踪字段、业财一体写库存的事务边界。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] 维度 15 owner-doc→代码漂移抽样：pur（`docs/design/purchase/state-machine.md` 抽 2 断言）、sal（`docs/design/sales/state-machine.md` 抽 2 断言）、assets（`docs/design/assets/state-machine.md` + 折旧 owner doc 各抽 2 断言）、inv（`docs/design/inventory/state-machine.md` + costing owner doc 各抽 2 断言）。对照 orm.xml 字典值 / BizModel 常量 / `*Errors.java` 核验。不一致即报 Major；若单域 ≥2 处漂移扩大抽样。
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [ ] 产出审计报告 `docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md`（按 pur/sal/assets/inv 组织，含：15 维度合规率、反模式实例清单、owner-doc 漂移核查点记录、finding 按 P0/P1/P2 分级、残留风险）。
      - Skill: none

Exit Criteria:

- [ ] pur/sal/assets/inv 四域 15 维度均有结论，自动化 grep 清单 + 语义抽样均产出
- [ ] 维度 15 每域 ≥4 核查点已记录（或发现 ≥2 漂移已扩大抽样）
- [ ] 报告产出，业财一体写路径平台规范核验有明确结论

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: planned
Targets: 四域平台合规审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.1
Skill: none

- Item Types: `Fix | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（手改生成代码 / 跨模块写反向 / 业务异常不扩展 NopException / @Inject private 致 IoC 失败 / 业财一体写绕过 I*Biz）当即就地修复（改保留层源 + `mvn clean install -DskipTests` + 该修复独立审计，保护区域修复须额外授权）或异步注入 fix plan。P0 永不进入 MR。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA1-NNN`，续 MA1 里程碑 P1 序号——ORM 审计 A1.1–A1.9 已至 014；A1.10/A1.11 执行后接续，本计划在其后），供 R1.0 展开机制转化为具体修复工作项行。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.1 "Nop 平台合规" 行 pur/sal/assets/inv 四列 `❓` → `✅`/`⚠️(P1)`。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [ ] arm-index 报告清单 + scope matrix §2.1 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is**（`ses_05e27ef37ffeclHlqE13d6bF64`，独立 general 子代理，对照实时仓库逐项复核）。VERDICT = acceptable-as-is，**无 BLOCKER**。核实要点：四模块目录存在；Java 计数 187/162/176/179 与 scope matrix §1.1 精确一致（total − `_gen/` − test 精确还原 source 计数，无陈旧数据）；四域 state-machine.md owner docs 全部存在；P1-MA1-008（assets propId 29 列）已在 arm-index.md 登记，计划正确按已知 deferred/P1 处理不重复登记；前序 ORM plan completed、S 级兄弟 plan（1227-2）为 draft；UC-SAL-10/UC-INV-08 并发缺口正确归 MA2 A2.17 不重复裁决；A4.3 assets Processor MA4 审计正确排除；15 维度全覆盖（dim 13 grep + Non-Goals，dim 15 每域 ≥4 核查点，dim 14 聚合完整性）；Exit Criteria 本地化（无全仓库 build/test，正确归 Closure Gates）；BUILD_VERIFY 审计纪律 + 保护区域门控（assets 折旧/财务报表、inventory 业财一体写、pur/sal 过账凭证）齐全；计划顺序 N=3 正确跟随 A1.10(N=1)/A1.11(N=2) 文档顺序。采纳的非阻塞修正：(1) Phase 1 grep item 显式增加维度 14 聚合完整性核查（app.action-auth.xml x:extends + 聚合 POM 依赖，已落地域为确认性核查）提升可追溯性；(2) Phase 2 P1 编号措辞精确化（ORM 审计已至 014；A1.10/A1.11 执行后接续，本计划在其后）。两项均已完成。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。

- [ ] 范围内行为完成（A1.12 pur/sal/assets/inv 四域平台合规 15 维度审计报告产出 + arm-index 更新 + scope matrix §2.1 标记完成）
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
- Why Not Blocking Closure: 已由 `2026-07-02-0900-1` Phase 5 裁决 Deferred（成本/静默回归主导，触发条件=业财一体打通前/跨系统集成启动时）。平台合规审计若复现 D1 相关模式，标注为已知 deferred，引用前序裁决，不重复裁决、不升级为 P1。
- Successor Required: `yes`——前序计划已命名触发条件。

### assets propId 缺失（P1-MA1-008，已知 P1 待 MR1）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 已在 MA1 ORM 审计（A1.5 assets）登记 P1 待 MR1（codegen 增量再生自动补全）。本审计复核其平台合规影响（如 propId 缺失是否触发维度 1/13 偏差），不重复登记为 P1。
- Successor Required: `yes`——MR1 经 R1.0 展开修复。

### UC-SAL-10 / UC-INV-08 并发缺口（归 MA2 A2.17）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: use-case-implementation-audit 标记的并发缺口（并发扣批次 / 乐观锁）归 MA2 A2.17 并发与乐观锁审计做业务正确性裁决。本审计仅核验其事务边界平台规范（@BizMutation 事务包装、@Version 注解存在性），不裁决并发正确性。
- Successor Required: `yes`——MA2 A2.17 执行时裁决。

## Closure

Status Note: _（待执行后填充）_

Closure Audit Evidence:

- _（待独立结束审计后填充）_

Follow-up:

- P1 finding 经 R1.0 展开机制进入 MR1
- 若 P0 即时修复注入 fix plan，该 fix plan 独立 closure
