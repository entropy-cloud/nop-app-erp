# 2026-07-09-2200-1-doc-code-alignment-remediation 文档-代码一致性修复

> Plan Status: active
> Last Reviewed: 2026-07-09
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

Status: planned
Targets: `docs/architecture/domain-module-split-analysis.md`
Skill: none

- Item Types: `Fix`
- Prereqs: none

- [ ] §2.0 映射表追加 notify 行（逻辑工程名 `app-erp-notify`，顶层目录 `module-notify/`，子模块前缀 `erp-notify-`，appName `erp-notify`，moduleId `erp/notify`，简称 `notify`，实体前缀 `ErpSys*`，表前缀 `erp_notify_`）
- [ ] §2.0 合计行修正："20 行 = 18 业务域 + 1 通知子系统 + 1 聚合启动工程"
- [ ] §2.1 目录布局追加 `├── module-notify/` 行以及说明注释
- [ ] §2.1 注释更新（"18 域" → 含 notify 的概括描述）
- [ ] §4.1 DAG 补充 notify 位置（依赖 master-data，被各业务域引用作为通知派发基础设施）
- [ ] §4.2 跨模块引用白名单追加 notify 行（各域引用 notify 的通知模板/实例）

Exit Criteria:
- [ ] notify 在 domain-module-split-analysis.md 四小节均有完整表现
- [ ] 合计行与 pom.xml 的实际模块数一致

### Phase 2 - 设计文档计数统一

Status: planned
Targets: `docs/design/README.md`, `docs/context/codebase-map.md`
Skill: none

- Item Types: `Fix`
- Prereqs: none

- [ ] 检查 `docs/design/README.md` 的全局文档表格，确认 6 个 stable owner docs 定义是否仍准确
- [ ] 统一 codebase-map.md 的描述与 design/README.md 保持一致（如已有 "多份" 概括则跳过）
- [ ] 如有新增全局 owner doc 应纳入 design/README.md 表格

Exit Criteria:
- [ ] codebase-map.md 与 design/README.md 对全局文档数的描述无矛盾

### Phase 3 - 清理空 xbiz 覆盖文件

Status: **overruled**

> **审计裁决**：空 xbiz 覆盖文件由 codegen 模板 `/nop/templates/orm` 在 `mvn clean compile` 时自动生成，删除后即再生。这是 Nop 平台 codegen 的标准行为，非本项目的文件残留。相位目标不可达。

Reason: codegen regeneration cycle confirmed. `mvn clean compile` recreates all 305 empty override files. To permanently suppress them would require modifying `nop-entropy` ORM templates (out of scope).

Resolution: retain files. They are harmless codegen artifacts with zero runtime impact (inherit `_gen/` layer `<actions/>`).

Exit Criteria:
- [x] 行为已记录在审计发现中

### Phase 4 - Processor 提取与 @SingleSession 标准化

Status: planned
Targets: `ErpPurSupplierScorecardBizModel`, `ErpPurOrderBizModel`, 各域 BizModel
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: Phase 3 (同一域清理工作可以合并)

- [ ] 审计 `ErpPurSupplierScorecardBizModel.finalizeScorecard`、`ErpPurOrderBizModel` 内联操作，评估是否应提取至独立 Processor
- [ ] 将确认应迁移的内联逻辑提取至独立 Processor
- [ ] Decision: 记录 @SingleSession 使用策略 — 哪些场景确实需要长会话，哪些是防御性编程
- [ ] 在已提取的 Processor 上补充对应 BizModel 的委托调用

Exit Criteria:
- [ ] `ErpPurSupplierScorecardBizModel.finalizeScorecard` 和 `ErpPurOrderBizModel` 内联逻辑已审计，确认需迁移的部分已提取至独立 Processor
- [ ] @SingleSession 策略已记录

### Phase 5 - 测试深度标准化（分析+度量，无代码修改）

Status: planned
Targets: `docs/testing/`, 各域 test 目录
Skill: none

- Item Types: `Add | Proof`
- Prereqs: none

- [ ] 建立测试深度分类标准：深（状态机/业务规则/端到端）、浅（CRUD 冒烟）、无（无测试）
- [ ] 对现有测试文件按深度分类归档
- [ ] 将分类结果写入 `docs/testing/test-depth-classification.md`

Exit Criteria:
- [ ] 测试深度分类文档落地
- [ ] 可按域查询测试覆盖质量

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0b8dc4862ffeknzo7bX0zPEId0) — minor clarity notes addressed: P0 #4 stated, Processor extraction added to Goals, Phase 4 exit criteria tightened, test file count generalized.

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证：`mvn compile -DskipTests`（Phase 3/4 删除 xbiz/Processor 后编译通过）
- [ ] 无范围内项目降级为 deferred/follow-up（除非在本计划中有明确裁决）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

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
