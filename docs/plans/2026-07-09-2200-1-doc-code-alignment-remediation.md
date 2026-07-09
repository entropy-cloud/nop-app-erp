# 2026-07-09-2200-1-doc-code-alignment-remediation 文档-代码一致性修复

> Plan Status: completed
> Last Reviewed: 2026-07-10
> Source: `docs/analysis/2026-07-09-2000-code-quality-and-doc-consistency-analysis.md`
> Related: codebase-map.md, project-context.md 已在前序清理中更新
> Audit: required

## Current Baseline

前序清理已解决（P0 全 + P1 部分 + P3 #15）：

| 已修复项 | 对应报告项 | 文件 |
|----------|-----------|------|
| codebase-map ORM 表清理（移除实体数/日期，补充 notify） | P0 #1 | codebase-map.md |
| Java 文件数声明移除 | P0 #2 | codebase-map.md, project-context.md |
| project-context 阶段描述重写 | P0 #3 | project-context.md |
| AGENTS.md 实体数/文件数声明移除 | P1 #6 | AGENTS.md |
| AGENTS.md `app-erp-web` → `erp-*-web` | P1 #6 | AGENTS.md |
| AGENTS.md contract 重复修复（4→3） | P3 #15 | AGENTS.md |
| domain-module-split-analysis 1721 移除 | P1 #7 | domain-module-split-analysis.md |
| project-context freshness 标签内容同步（随阶段重写自动解决） | P0 #4 | project-context.md |

**仍存在的差距**：

1. **domain-module-split-analysis.md notify 缺失**：§2.0 映射表、§2.1 目录布局、§4.1 DAG、§4.2 跨模块引用白名单均未包含 `module-notify`。第 68 行合计行仍写 "19 行 = 18 业务域 + 1 聚合"。
2. **design/README.md vs codebase-map.md 全局文档计数不一致**：README 列出 6 个稳定 owner docs，codebase-map 声称 "7 份"。
3. **9 域设计描述可能未跟进扩展实体**：assets/finance/hr/manufacturing/quality/inventory/master-data/projects/purchase 域新增实体的业务描述可能在设计文档中遗漏。
4. **空 xbiz `<actions/>` 覆盖文件**：多个域（如 `ErpPurRfq.xbiz`、`ErpPurSupplierScorecard.xbiz`）的 xbiz 文件仅 `<actions/>`，不添加自定义操作时不需此覆盖。
5. **部分 BizModel 内联逻辑未提取 Processor**：`ErpPurSupplierScorecardBizModel`、`ErpPurOrderBizModel` 等有内联业务逻辑。
6. **@SingleSession 使用缺乏选择标准**：部分 BizModel 全部方法加注 `@SingleSession` 但无统一策略。
7. **测试深度度量未标准化**：目前按测试文件数量评估覆盖，未考虑测试深度。

## Goals

- 补齐 domain-module-split-analysis.md 中 notify 域在 4 个小节的缺失
- 统一 design/README.md 与 codebase-map.md 的全局文档计数
- 清理空 xbiz 覆盖文件
- 审计内联业务逻辑，将确认应迁移的部分提取至独立 Processor
- 建立 @SingleSession 使用标准
- 引入测试深度分类度量

## Non-Goals

- 不修改 _gen/ 目录下的任何生成文件
- 不修改 ORM 模型（`model/*.orm.xml`)
- 不新增测试逻辑（仅度量标准化）
- 不处理已在前序清理中完成的所有项

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/domain-module-split-analysis.md`, `docs/design/README.md`
- Skill Selection Basis: 主要涉及文档编辑和代码清理，不涉及 Nop 平台新技能

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - domain-module-split-analysis notify 补齐

Status: completed
Targets: `docs/architecture/domain-module-split-analysis.md`
Skill: none

- Item Types: `Fix`
- Prereqs: none

> **裁决（2026-07-10）**：复核确认 notify 域在 §2.0 映射表、§2.1 目录布局与注释、§4.1 DAG、§4.2 跨模块引用白名单四小节均已完整呈现，合计行已为 "20 行 = 18 业务域 + 1 通知子系统 + 1 聚合启动工程"，与根 pom.xml 的 20 个 reactor 模块一致。二级简称取 `sys`（带脚注说明系统层定位）而非计划草拟的 `notify`，是更准确的裁定。

- [x] §2.0 映射表追加 notify 行（逻辑工程名 `app-erp-notify`，顶层目录 `module-notify/`，子模块前缀 `erp-notify-`，appName `erp-notify`，moduleId `erp/notify`，简称 `notify`，实体前缀 `ErpSys*`，表前缀 `erp_notify_`）
- [x] §2.0 合计行修正："20 行 = 18 业务域 + 1 通知子系统 + 1 聚合启动工程"
- [x] §2.1 目录布局追加 `├── module-notify/` 行以及说明注释
- [x] §2.1 注释更新（"18 域" → 含 notify 的概括描述）
- [x] §4.1 DAG 补充 notify 位置（依赖 master-data，被各业务域引用作为通知派发基础设施）
- [x] §4.2 跨模块引用白名单追加 notify 行（各域引用 notify 的通知模板/实例）

Exit Criteria:
- [x] notify 在 domain-module-split-analysis.md 四小节均有完整表现
- [x] 合计行与 pom.xml 的实际模块数一致

### Phase 2 - 设计文档计数统一

Status: completed
Targets: `docs/design/README.md`, `docs/context/codebase-map.md`
Skill: none

- Item Types: `Fix`
- Prereqs: none

> **裁决（2026-07-10）**：`codebase-map.md` 已将原 "7 份全局 owner doc" 改为概括性 "多份全局 owner doc"（与任意计数不矛盾）。复核 `docs/design/*.md`，确认 `dashboards.md`（经营看板设计规格）是遗漏的稳定 owner doc，已补入 `docs/design/README.md` 全局文档表格（现 7 份）。`erp-design-audit-checklist.md`（核对清单）与 `use-case-authoring-guide.md`（编写指南）属方法/指南文档，不纳入 owner doc 表。

- [x] 检查 `docs/design/README.md` 的全局文档表格，确认 6 个 stable owner docs 定义是否仍准确
- [x] 统一 codebase-map.md 的描述与 design/README.md 保持一致（如已有 "多份" 概括则跳过）
- [x] 如有新增全局 owner doc 应纳入 design/README.md 表格（补入 `dashboards.md`）

Exit Criteria:
- [x] codebase-map.md 与 design/README.md 对全局文档数的描述无矛盾

### Phase 3 - 清理空 xbiz 覆盖文件

Status: **overruled**

> **审计裁决**：空 xbiz 覆盖文件由 codegen 模板 `/nop/templates/orm` 在 `mvn clean compile` 时自动生成，删除后即再生。这是 Nop 平台 codegen 的标准行为，非本项目的文件残留。相位目标不可达。

Reason: codegen regeneration cycle confirmed. `mvn clean compile` recreates all 305 empty override files. To permanently suppress them would require modifying `nop-entropy` ORM templates (out of scope).

Resolution: retain files. They are harmless codegen artifacts with zero runtime impact (inherit `_gen/` layer `<actions/>`).

Exit Criteria:
- [x] 行为已记录在审计发现中

### Phase 4 - Processor 提取与 @SingleSession 标准化

Status: completed
Targets: `ErpPurSupplierScorecardBizModel`, `ErpPurOrderBizModel`, 各域 BizModel
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: Phase 3 (同一域清理工作可以合并)

> **审计裁决（2026-07-10）**：分析报告 §3.1 所称的"内联业务逻辑待提取"基于更早的代码状态；重审当前代码后，两个被点名 BizModel **已完全符合 Facade+Processor 模式**，无需迁移：
> - `ErpPurSupplierScorecardBizModel.finalizeScorecard` 已是薄 Facade：评分计算委托 `ScorecardCalculator`，跨域 AVL 暂停委托 `ScorecardStandingLinker`，本类仅保留状态守卫 + 状态写回 + 条件委托（正确归属 Facade，见 `processor-extension-pattern.md` 两层职责表）。
> - `ErpPurOrderBizModel` 已是薄 Facade：审批动作经 xbiz 一行委托 Processor，`cancel` 委托 `ErpPurOrderProcessor`，`createFromRequisition` 委托 `RequisitionToOrderConverter`，仅 `existsActiveByRequisition`/`updateReceiveStatus` 为薄查询/状态回写。
>
> **@SingleSession 策略**：已在 `docs/architecture/singlesession-strategy.md` 完整记录（总原则、4 类适用场景、3 类不适用场景、逐域审计表、新增方法审查规则）。本次补齐了审计表中缺失的 hr 域（薪酬/排班批量计算多实体）。结论：当前全库 `@SingleSession` 使用均为必要场景，无防御性编程实例，无需清理。Processor 的事务/Session 分层规则（事务钉 Facade、Session 刷新作用域钉编排方法）见 `processor-extension-pattern.md §两层结构 硬规则1`。

- [x] 审计 `ErpPurSupplierScorecardBizModel.finalizeScorecard`、`ErpPurOrderBizModel` 内联操作，评估是否应提取至独立 Processor
- [x] 将确认应迁移的内联逻辑提取至独立 Processor（裁决：无需迁移，已符合模式）
- [x] Decision: 记录 @SingleSession 使用策略 — 哪些场景确实需要长会话，哪些是防御性编程
- [x] 在已提取的 Processor 上补充对应 BizModel 的委托调用（裁决：委托调用已存在，无需补充）

Exit Criteria:
- [x] `ErpPurSupplierScorecardBizModel.finalizeScorecard` 和 `ErpPurOrderBizModel` 内联逻辑已审计，确认需迁移的部分已提取至独立 Processor
- [x] @SingleSession 策略已记录

### Phase 5 - 测试深度标准化（分析+度量，无代码修改）

Status: completed
Targets: `docs/testing/`, 各域 test 目录
Skill: none

- Item Types: `Add | Proof`
- Prereqs: none

> **裁决（2026-07-10）**：`docs/testing/test-depth-classification.md` 已落地深/中/浅三档分类标准（按测试类行数：≥400 深、100-399 中、<100 浅），并刷新逐域汇总表至当前真实数据（255 个测试类：34 深 / 207 中 / 14 浅）。计数口径明确排除 `*CodeGen`/`*TestSupport*`/`TestStub*` 三类非测试产物。19 域全部有测试，无「无测试」域。

- [x] 建立测试深度分类标准：深（状态机/业务规则/端到端）、浅（CRUD 冒烟）、无（无测试）
- [x] 对现有测试文件按深度分类归档
- [x] 将分类结果写入 `docs/testing/test-depth-classification.md`

Exit Criteria:
- [x] 测试深度分类文档落地
- [x] 可按域查询测试覆盖质量

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0b8dc4862ffeknzo7bX0zPEId0) — minor clarity notes addressed: P0 #4 stated, Processor extraction added to Goals, Phase 4 exit criteria tightened, test file count generalized.

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证：`mvn install -DskipTests`（全 154 reactor 模块 BUILD SUCCESS；本计划为纯文档/度量变更，无 Java/ORM 代码改动，构建确认未引入回归）
- [x] 无范围内项目降级为 deferred/follow-up（除非在本计划中有明确裁决）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 9 域设计描述跟进

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 各域 README/use-cases 对扩展实体的描述缺失是低风险 gap（ORM 是真相源），且需逐域人工 review，不适合同一批计划处理。建议在后续各域功能深化时附带补齐。
- Successor Required: `yes` — 由各域后续功能计划自动覆盖

## Closure

Status Note: completed (Phase 3 overruled per audit)

Closure Audit Evidence:

- Auditor / Agent: ses_0b8ca7984ffe3hNyijL0tYJ8mR (independent)
- Evidence: `docs/plans/2026-07-09-2200-1-audit-findings.md`

Follow-up:

- 9 域设计描述对齐：由各域后续功能计划自动覆盖
