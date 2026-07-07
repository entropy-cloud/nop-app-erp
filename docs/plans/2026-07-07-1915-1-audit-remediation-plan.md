# 2026-07-07-1915-1 综合审计整改计划

> Plan Status: active
> Last Reviewed: 2026-07-07
> Source: `docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md`
> Related: `docs/audits/2026-07-05-1400-cross-review-synthesis.md`, `docs/audits/2026-07-05-1300-code-vs-design-vs-best-practices-audit.md`, `docs/audits/2026-07-05-1500-supplementary-audit-and-checker.md`
> Audit: required

## Current Baseline

- 综合审计报告（2026-07-07-1900）识别出 **4 严重 / 6 高 / 9 中 / 6 低** 问题
- 跨模块外部实体表名双前缀错误（b2b/cs/logistics/contract, 7 处 `erp_md_md_partner/material/employee`）已在源 `orm.xml` 和生成的 `_app.orm.xml` 中确认
- 事务语义矛盾（flow-overview.md REQUIRES_NEW vs data-dependency-matrix.md SYNC）未经裁决
- 7 个扩展域（cs/hr/logistics/b2b/contract/drp/aps）缺少 posted/businessDate 标准字段
- known-good-baselines.md 为空模板
- docs/bugs/ 仅 1 篇缺陷记录
- 其他文档/流程/模型问题待修复

## Goals

- 修复所有严重（C-1~C-4）和高（H-1~H-6）级别问题
- 修复关键的中级别问题（M-1~M-4, M-7）
- 建立缺陷记录与基线断言机制，防止同类问题复发

## Non-Goals

- 本计划不涉及扩展域标准字段补充后的 codegen 重新生成（那是后续计划）
- 本计划不涉及新建域的设计或功能增强
- 本计划不涉及技能模板的深度定制化（M-9 为低优先级）

## Task Route

- Type: `bug investigation | verification or audit work | implementation-only change`
- Owner Docs: `docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md`, `docs/bugs/00-bug-fix-note-writing-guide.md`, `docs/testing/known-good-baselines.md`
- Skill Selection Basis: `nop-debugging` 用于跨模块 EQL 阻断调查；`nop-backend-dev` 用于表名修正后的增量重新生成验证；`nop-testing` 用于验证命令和基线断言。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 修正 orm.xml 后需 `mvn clean install -DskipTests` 验证构建

## Execution Plan

### Phase 1 — 修复严重问题（C-1 ~ C-4）

Status: planned
Targets: `module-b2b/model/`, `module-cs/model/`, `module-logistics/model/`, `module-contract/model/`, `docs/design/`, `docs/architecture/`
Skill: `nop-debugging | nop-backend-dev`

- Item Types: `Fix | Decision`

#### C-1: 修正跨模块外部实体表名双前缀

- [ ] Fix: `module-b2b/model/app-erp-b2b.orm.xml` — 修正 `erp_md_md_partner` → `erp_md_partner`, `erp_md_md_material` → `erp_md_material`
- [ ] Fix: `module-cs/model/app-erp-cs.orm.xml` — 修正 `erp_md_md_partner` → `erp_md_partner`
- [ ] Fix: `module-logistics/model/app-erp-logistics.orm.xml` — 修正 `erp_md_md_partner`/`erp_md_md_employee`/`erp_md_md_material` 三处
- [ ] Fix: `module-contract/model/app-erp-contract.orm.xml` — 修正 `erp_md_md_partner` → `erp_md_partner`
- [ ] Proof: `mvn clean install -DskipTests` 全绿通过，验证生成的 `_app.orm.xml` 表名正确
- Skill: `nop-backend-dev`

#### C-2: 统一事务语义

> **审查修正**：独立草案审查指出 REQUIRES_NEW 是故意架构决策（`ErpFinVoucherBizModel.post()` 叠加 `@Transactional(REQUIRES_NEW)`，经 `docs/analysis/2026-07-05-1300-platform-best-practices-extended-audit.md:231` 确认为例外、`docs/design/finance/posting.md:380` 确认为设计）。flow-overview.md 的 REQUIRES_NEW 描述与实现一致。正确方向是修正 data-dependency-matrix.md。

- [ ] Decision: 确认过账模式 — 同步调用 + REQUIRES_NEW 独立事务隔离（现存架构），`flow-overview.md` 描述正确
      - 证据：`ErpFinVoucherBizModel.post()` 叠加 `@Transactional(REQUIRES_NEW)`，所有 posting executor JavaDoc 引用独立事务
      - 裁决：修正 `data-dependency-matrix.md §4.1` — 将 SYNC 描述修正为 "同步调用（独立事务传播 REQUIRES_NEW，跨域失败 posted 标志隔离 + 兜底扫描）"
      - 原综合审计报告 §2.1 的"矛盾"判断为误判，需同步更新
- [ ] Fix: 修正 `docs/architecture/data-dependency-matrix.md §4.1` 事务策略描述 — 单据审核+凭证生成：同步调用（独立事务 REQUIRES_NEW，跨域失败隔离 via posted 标志 + 兜底扫描）
- [ ] Fix: 修正 `docs/architecture/data-dependency-matrix.md §4.4` 中 "默认 SYNC" 描述 → 同步调用（独立事务 REQUIRES_NEW）
- [ ] Fix: 修正 `docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md §2.1` 中的误判描述
- [ ] Proof: 确认 flow-overview.md §6.1 与 data-dependency-matrix.md §4.1 对 REQUIRES_NEW 描述一致
- Skill: `nop-debugging`

#### C-3: 对齐 DAG 层级

- [ ] Fix: 修正 `docs/design/domain-design-guidelines.md §1.2` DAG 顺序为：
      `master-data → inventory → purchase/sales → finance（L3 顶）`
      `assets/projects/manufacturing/quality/maintenance（L2 扩展）`
- [ ] Proof: 确认与 `data-dependency-matrix.md §2.1` 和 `module-boundaries.md` 一致
- Skill: none

#### C-4: 解决 docStatus 通用集与实际域值不匹配

- [ ] Decision: 方案选择 — 转为概念框架 vs 对齐到某域值
      - 推荐：转为概念框架，去除具体状态值，改为指针到各域 `state-machine.md`
- [ ] Fix: 重写 `docs/architecture/document-engine.md §三轴` 的 docStatus 通用值为抽象描述 + 各域链接
- [ ] Proof: 确认各域 `state-machine.md` 正确覆盖所有状态值
- Skill: none

Exit Criteria:

- [ ] 4 个源 orm.xml 中 7 处表名错误全部修正，构建全绿
- [ ] flow-overview.md 事务描述与 data-dependency-matrix.md 一致
- [ ] domain-design-guidelines.md DAG 与 data-dependency-matrix.md 一致
- [ ] document-engine.md docStatus 表不再包含误导的具体值

### Phase 2 — 修复高优先级问题（H-1 ~ H-6）

Status: planned
Targets: `docs/testing/`, `docs/bugs/`, 7 个扩展域 orm.xml, `docs/architecture/`, `docs/design/`
Skill: none

- Item Types: `Fix | Add`

#### H-1: 补充 known-good-baselines.md

- [ ] Add: 基于最近全绿构建提交记录，在 `docs/testing/known-good-baselines.md` 填写基线条目
      - 格式参照指南模板：日期 + 构建命令 + 验证状态 + 备注
- Skill: none

#### H-2: 创建缺陷记录

- [ ] Add: `docs/bugs/2026-07-07-1915-sales-credit-control-multi-currency-and-ar-balance-omission.md` — 销售信用控制多币种遗漏+AR 余额遗漏（C-2 财务风险）
- [ ] Add: `docs/bugs/2026-07-07-1915-voucher-amount-plaintext-in-graphql-query.md` — 凭证明文金额通过 GraphQL 可查询（C-4 安全风险）
- [ ] Add: `docs/bugs/2026-07-07-1915-localdatetime-now-in-12-domains.md` — 12 个域 60+ 处 `LocalDateTime.now()` 导致测试时间不可控（C-5 测试不可控性）
- [ ] Add: `docs/bugs/2026-07-07-1915-dao-updateentity-in-bizmodel.md` — BizModel 中 48 处 `dao().updateEntity()` 违规使用
- Skill: none

#### H-3: 创建回顾记录

- [ ] Add: `docs/retrospectives/2026-07-07-missing-known-good-baselines.md` — known-good-baselines 长期空白的原因与改进
- [ ] Add: `docs/retrospectives/2026-07-07-audit-quality-event-1300.md` — 审计 1300 评分偏差的质量事件分析
- Skill: none

#### H-4: 修正 master-data/sales 文件头注释

- [ ] Fix: `module-master-data/model/app-erp-master-data.orm.xml` 文件头 — `dict valueType="string"`, 删除 `option value 10/20/30 递增` 描述
- [ ] Fix: `module-sales/model/app-erp-sales.orm.xml` 文件头 — 同上
- Skill: none

#### H-5: 归档或重写 doc-model-design.md

- [ ] Decision: 归档到 `docs/archive/architecture/doc-model-design.md`（推荐，因为该文档使用已淘汰的 docType/bizType 双维分类法和过长时的 Java 代码）
- [ ] Fix: 移动文件到 `docs/archive/architecture/`，并在原路径留重定向说明文件
- Skill: none

#### H-6: docNo 唯一性对齐

- [ ] Fix: 删除或修正 `docs/architecture/doc-model-design.md` 中的全局唯一约束描述（该文件在 H-5 归档后自然解决）
- [ ] Proof: 确认 `domain-design-guidelines.md §14.1.1` 的 orgId 内唯一描述已为唯一权威
- Skill: none

Exit Criteria:

- [ ] known-good-baselines.md 已填入最近构建基线
- [ ] 4 篇缺陷记录完成创建
- [ ] 2 篇回顾记录完成创建
- [ ] master-data/sales orm.xml 文件头注释修正
- [ ] doc-model-design.md 已归档

### Phase 3 — 修复关键中级别问题

Status: planned
Targets: 8 个域 orm.xml, `docs/design/`, `docs/plans/`, `docs/audits/`
Skill: none

- Item Types: `Fix | Proof`

#### M-1: 删除 8 域冗余 approveStatus 字典

- [ ] Fix: 从 purchase/sales/manufacturing/quality/maintenance/cs/finance/inventory 的 orm.xml 字典定义段中删除 `erp-<domain>/approve-status` 字典定义
      - 注意保留 inventory 中对 `erp-inv/approve-status` 的列引用（其在 `wf/approve-status` 之外额外使用域字典）
- [ ] Proof: codegen 重新生成的字典目录中不应再包含这些字典 YAML 文件
- Skill: `nop-backend-dev`

#### M-2: 修正 INSPECTING 态

- [ ] Fix: 从 `docs/design/domain-design-guidelines.md §16.2` manufacturing 行中删除 `INSPECTING`
- [ ] Proof: 确认与 `docs/design/manufacturing/state-machine.md` 和 `flow-overview.md §3.3` 一致
- Skill: none

#### M-3: 解决计划状态冲突

- [ ] Decision: `docs/plans/2026-07-03-2108-1-dict-int-to-string-refactor.md` — 检查实际完成状态：如果代码已落地，执行独立结束审计并勾选；如果未完成，修正状态为 active 或 deferred
- [ ] Fix: `docs/plans/` 重命名 `2026-07-05-1500-1-cross-review-remediation.md` → `2026-07-05-1500-2-cross-review-remediation.md`
- [ ] Fix: `docs/plans/` 重命名 `2026-07-05-1500-1-nop-platform-compliance-remediation.md` → `2026-07-05-1500-1-nop-platform-compliance-remediation.md`（保留原名，确认无冲突）
- Skill: none

#### M-4: 配置审计合规检查器集成

- [ ] Decision: 将 `nop-compliance-checker.sh` 添加到 `project-context.md` 验证命令表
- [ ] Fix: 在 `docs/context/project-context.md` 验证命令表中添加：`| 合规性检查 | bash docs/audits/nop-compliance-checker.sh |`
- Skill: none

Exit Criteria:

- [ ] 8 域冗余字典删除，构建全绿
- [ ] domain-design-guidelines.md 中 INSPECTING 已删除
- [ ] 计划状态冲突解决
- [ ] project-context.md 包含合规检查器命令

### Phase 4 — 低优先级与后续优化

Status: planned
Targets: `module-sales/model/`, `docs/design/`, `docs/index.md`, `docs/logs/`
Skill: none

- Item Types: `Fix | Documentation`

#### L-1: 补充 tagSet

- [ ] Fix: `module-sales/model/app-erp-sales.orm.xml` — `ErpSalOrderLine.project` to-one 添加 `tagSet="pub"`
- Skill: none

#### L-2: 文档编号修正

- [ ] Fix: `docs/design/domain-design-guidelines.md` §编号 — 将 §13 的 "12.1" 改为 "13.1/13.2"
- Skill: none

#### L-3: AGENTS.md 同步更新

- [ ] Fix: 更新 AGENTS.md 反映当前 codegen 后的阶段描述和完整 18+1 域列表
- Skill: none

#### L-4: 更新每日日志

- [ ] Fix: 在 `docs/logs/2026/07-07.md` 追加本计划执行记录
- Skill: none

Exit Criteria:

- [ ] sales orm.xml 补充 tagSet
- [ ] domain-design-guidelines.md 编号修正
- [ ] AGENTS.md 阶段描述更新
- [ ] 日志已更新

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c347cde1ffeCPKBsVJbukqDOJ) because:
  - B1: C-2 基于事实错误的代码解读 — REQUIRES_NEW 是故意架构决策，应修正 data-dependency-matrix.md 而非 flow-overview.md
  - B2: C-2 Proof 标准不可能通过 — 项目中确实使用 `@Transactional(REQUIRES_NEW)`
  - B3: Phase 2/4 退出标准含 "构建全绿" 违反指南执行规则 7
  - B4: Phase 2 阶段级技能标注 `nop-backend-dev` 与实际文档工作不匹配
  - N1: M-3 未决决策标为 Fix
  - N2: M-3 "或" 引入了互斥完成路径
- Independent draft review iteration 2: `acceptable as-is` () after:
  - C-2 重写：确认 REQUIRES_NEW 架构，修正 data-dependency-matrix.md 而非 flow-overview.md
  - Phase 2 skill 改为 none
  - Phase 2/4 退出标准移除 "构建全绿"
  - M-3 拆分 ambiguous 项

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（DAG / 事务 / docStatus 一致性已验证）
- [ ] 已运行验证：`mvn clean install -DskipTests` 全绿（仅对修改 orm.xml 的 Phase 1/3 执行后触发）
- [ ] known-good-baselines.md 已更新
- [ ] docs/bugs/ 至少新增 3 篇缺陷记录
- [ ] docs/retrospectives/ 至少新增 2 篇
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 7 个扩展域补充 posted/businessDate 标准字段

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 涉及 7 个域的 ORM 模型修改（保护区域 `ask-first`）和后续 codegen 重新生成，影响面大。单独开计划处理。
- Successor Required: `yes`
- Trigger Condition: 本计划完成关闭后，由 AI 根据 `docs/backlog/README.md` 选择下一工作项时自动触发，或人工在 backlog 中指定优先级时启动

### 扩展域标准字段补充后的 codegen 重新生成

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖上一项的完成
- Successor Required: `yes`
- Trigger Condition: 紧接上一项（扩展域字段补充）完成后自动触发；如果上一项修改 orm.xml，用 `mvn clean install -DskipTests` 增量生成

### 技能模板深度定制化（M-9）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 项目当前技能使用模式是复制后手工调整，功能上可行
- Successor Required: `no`

## Closure

Status Note: `<pending>`

Closure Audit Evidence:

- Auditor / Agent: `<pending>`
- Evidence: `<pending>`

Follow-up:

- `<pending>`
