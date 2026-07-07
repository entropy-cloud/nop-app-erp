# 2026-07-07-1915-1 综合审计整改计划

> Plan Status: completed
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

Status: completed
Targets: `module-b2b/model/`, `module-cs/model/`, `module-logistics/model/`, `module-contract/model/`, `docs/design/`, `docs/architecture/`
Skill: `nop-debugging | nop-backend-dev`

- Item Types: `Fix | Decision`

#### C-1: 修正跨模块外部实体表名双前缀

- [x] Fix: `module-b2b/model/app-erp-b2b.orm.xml` — 修正 `erp_md_md_partner` → `erp_md_partner`, `erp_md_md_material` → `erp_md_material`
- [x] Fix: `module-cs/model/app-erp-cs.orm.xml` — 修正 `erp_md_md_partner` → `erp_md_partner`
- [x] Fix: `module-logistics/model/app-erp-logistics.orm.xml` — 修正 `erp_md_md_partner`/`erp_md_md_employee`/`erp_md_md_material` 三处
- [x] Fix: `module-contract/model/app-erp-contract.orm.xml` — 修正 `erp_md_md_partner` → `erp_md_partner`
- [x] Proof: `mvn clean install -DskipTests` 全绿通过，验证生成的 `_app.orm.xml` 表名正确
- Skill: `nop-backend-dev`

#### C-2: 统一事务语义

> **审查修正**：独立草案审查指出 REQUIRES_NEW 是故意架构决策（`ErpFinVoucherBizModel.post()` 叠加 `@Transactional(REQUIRES_NEW)`，经 `docs/analysis/2026-07-05-1300-platform-best-practices-extended-audit.md:231` 确认为例外、`docs/design/finance/posting.md:380` 确认为设计）。flow-overview.md 的 REQUIRES_NEW 描述与实现一致。正确方向是修正 data-dependency-matrix.md。

- [x] Decision: 确认过账模式 — 同步调用 + REQUIRES_NEW 独立事务隔离（现存架构），`flow-overview.md` 描述正确
      - 证据：`ErpFinVoucherBizModel.post()` 叠加 `@Transactional(REQUIRES_NEW)`，所有 posting executor JavaDoc 引用独立事务
      - 裁决：修正 `data-dependency-matrix.md §4.1` — 将 SYNC 描述修正为 "同步调用（独立事务传播 REQUIRES_NEW，跨域失败 posted 标志隔离 + 兜底扫描）"
      - 原综合审计报告 §2.1 的"矛盾"判断为误判，需同步更新
- [x] Fix: 修正 `docs/architecture/data-dependency-matrix.md §4.1` 事务策略描述 — 单据审核+凭证生成：同步调用（独立事务 REQUIRES_NEW，跨域失败隔离 via posted 标志 + 兜底扫描）
- [x] Fix: 修正 `docs/architecture/data-dependency-matrix.md §4.4` 中 "默认 SYNC" 描述 → 同步调用（独立事务 REQUIRES_NEW）
- [x] Fix: 修正 `docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md §2.1` 中的误判描述
- [x] Proof: 确认 flow-overview.md §6.1 与 data-dependency-matrix.md §4.1 对 REQUIRES_NEW 描述一致
- Skill: `nop-debugging`

#### C-3: 对齐 DAG 层级

- [x] Fix: 修正 `docs/design/domain-design-guidelines.md §1.2` DAG 顺序为：
      `master-data → inventory → purchase/sales → finance（L3 顶）`
      `assets/projects/manufacturing/quality/maintenance（L2 扩展）`
- [x] Proof: 确认与 `data-dependency-matrix.md §2.1` 和 `module-boundaries.md` 一致
- Skill: none

#### C-4: 解决 docStatus 通用集与实际域值不匹配

- [x] Decision: 方案选择 — 转为概念框架 vs 对齐到某域值
      - 推荐：转为概念框架，去除具体状态值，改为指针到各域 `state-machine.md`
- [x] Fix: 重写 `docs/architecture/document-engine.md §三轴` 的 docStatus 通用值为抽象描述 + 各域链接
- [x] Proof: 确认各域 `state-machine.md` 正确覆盖所有状态值
- Skill: none

Exit Criteria:

- [x] 4 个源 orm.xml 中 7 处表名错误全部修正，构建全绿
- [x] flow-overview.md 事务描述与 data-dependency-matrix.md 一致
- [x] domain-design-guidelines.md DAG 与 data-dependency-matrix.md 一致
- [x] document-engine.md docStatus 表不再包含误导的具体值

### Phase 2 — 修复高优先级问题（H-1 ~ H-6）

Status: completed
Targets: `docs/testing/`, `docs/bugs/`, 7 个扩展域 orm.xml, `docs/architecture/`, `docs/design/`
Skill: none

- Item Types: `Fix | Add`

#### H-1: 补充 known-good-baselines.md

- [x] Add: 基于最近全绿构建提交记录，在 `docs/testing/known-good-baselines.md` 填写基线条目
      - 格式参照指南模板：日期 + 构建命令 + 验证状态 + 备注
- Skill: none

#### H-2: 创建缺陷记录

- [x] Add: `docs/bugs/2026-07-07-1915-sales-credit-control-multi-currency-and-ar-balance-omission.md` — 销售信用控制多币种遗漏+AR 余额遗漏（C-2 财务风险）
- [x] Add: `docs/bugs/2026-07-07-1915-voucher-amount-plaintext-in-graphql-query.md` — 凭证明文金额通过 GraphQL 可查询（C-4 安全风险）
- [x] Add: `docs/bugs/2026-07-07-1915-localdatetime-now-in-12-domains.md` — 12 个域 60+ 处 `LocalDateTime.now()` 导致测试时间不可控（C-5 测试不可控性）
- [x] Add: `docs/bugs/2026-07-07-1915-dao-updateentity-in-bizmodel.md` — BizModel 中 48 处 `dao().updateEntity()` 违规使用
- Skill: none

#### H-3: 创建回顾记录

- [x] Add: `docs/retrospectives/2026-07-07-missing-known-good-baselines.md` — known-good-baselines 长期空白的原因与改进
- [x] Add: `docs/retrospectives/2026-07-07-audit-quality-event-1300.md` — 审计 1300 评分偏差的质量事件分析
- Skill: none

#### H-4: 修正 master-data/sales 文件头注释

- [x] Fix: `module-master-data/model/app-erp-master-data.orm.xml` 文件头 — `dict valueType="string"`, 删除 `option value 10/20/30 递增` 描述
- [x] Fix: `module-sales/model/app-erp-sales.orm.xml` 文件头 — 同上
- Skill: none

#### H-5: 归档或重写 doc-model-design.md

- [x] Decision: 归档到 `docs/archive/architecture/doc-model-design.md`（推荐，因为该文档使用已淘汰的 docType/bizType 双维分类法和过长时的 Java 代码）
- [x] Fix: 移动文件到 `docs/archive/architecture/`，并在原路径留重定向说明文件
- Skill: none

#### H-6: docNo 唯一性对齐

- [x] Fix: 删除或修正 `docs/architecture/doc-model-design.md` 中的全局唯一约束描述（该文件在 H-5 归档后自然解决）
- [x] Proof: 确认 `domain-design-guidelines.md §14.1.1` 的 orgId 内唯一描述已为唯一权威
- Skill: none

Exit Criteria:

- [x] known-good-baselines.md 已填入最近构建基线
- [x] 4 篇缺陷记录完成创建
- [x] 2 篇回顾记录完成创建
- [x] master-data/sales orm.xml 文件头注释修正
- [x] doc-model-design.md 已归档

### Phase 3 — 修复关键中级别问题

Status: completed
Targets: 8 个域 orm.xml, `docs/design/`, `docs/plans/`, `docs/audits/`
Skill: none

- Item Types: `Fix | Proof`

#### M-1: 删除 8 域冗余 approveStatus 字典

- [x] Fix: 从 purchase/sales/manufacturing/quality/maintenance/cs/finance 的 orm.xml 字典定义段中删除 `erp-<domain>/approve-status` 字典定义
      - 注意保留 inventory 中对 `erp-inv/approve-status` 的列引用（其在 `wf/approve-status` 之外额外使用域字典）
      - 实际执行：删 7 个域字典（inventory 字典+列引用全部保留，因其列引用 `erp-inv/approve-status` 而非 `wf/approve-status`）
- [x] Proof: codegen 重新生成的字典目录中不应再包含这些字典 YAML 文件
- Skill: `nop-backend-dev`

#### M-2: 修正 INSPECTING 态

- [x] Fix: 从 `docs/design/domain-design-guidelines.md §16.2` manufacturing 行中删除 `INSPECTING`
- [x] Proof: 确认与 `docs/design/manufacturing/state-machine.md` 和 `flow-overview.md §3.3` 一致
- Skill: none

#### M-3: 解决计划状态冲突

- [x] Decision: `docs/plans/2026-07-03-2108-1-dict-int-to-string-refactor.md` — 检查实际完成状态：内部一致（Plan Status: completed，所有 Phase Status: completed，无 `[ ]` 残留），无需修正
- [x] Fix: `docs/plans/` 重命名 `2026-07-05-1500-1-cross-review-remediation.md` → `2026-07-05-1500-2-cross-review-remediation.md`（含文件标题与 docs/logs、docs/plans 跨文件引用同步）
- [x] Fix: `docs/plans/` 重命名 `2026-07-05-1500-1-nop-platform-compliance-remediation.md` → `2026-07-05-1500-1-nop-platform-compliance-remediation.md`（保留原名，确认无冲突）
- Skill: none

#### M-4: 配置审计合规检查器集成

- [x] Decision: 将 `nop-compliance-checker.sh` 添加到 `project-context.md` 验证命令表
- [x] Fix: 在 `docs/context/project-context.md` 验证命令表中添加：`| 合规性检查 | bash docs/audits/nop-compliance-checker.sh |`
- Skill: none

Exit Criteria:

- [x] 8 域冗余字典删除，构建全绿
- [x] domain-design-guidelines.md 中 INSPECTING 已删除
- [x] 计划状态冲突解决
- [x] project-context.md 包含合规检查器命令

### Phase 4 — 低优先级与后续优化

Status: completed
Targets: `module-sales/model/`, `docs/design/`, `docs/index.md`, `docs/logs/`
Skill: none

- Item Types: `Fix | Documentation`

#### L-1: 补充 tagSet

- [x] Fix: `module-sales/model/app-erp-sales.orm.xml` — `ErpSalOrderLine.project` to-one 添加 `tagSet="pub"`
- Skill: none

#### L-2: 文档编号修正

- [x] Fix: `docs/design/domain-design-guidelines.md` §编号 — 将 §13 的 "12.1" 改为 "13.1/13.2"
- Skill: none

#### L-3: AGENTS.md 同步更新

- [x] Fix: 更新 AGENTS.md 反映当前 codegen 后的阶段描述和完整 18+1 域列表
- Skill: none

#### L-4: 更新每日日志

- [x] Fix: 在 `docs/logs/2026/07-07.md` 追加本计划执行记录
- Skill: none

Exit Criteria:

- [x] sales orm.xml 补充 tagSet
- [x] domain-design-guidelines.md 编号修正
- [x] AGENTS.md 阶段描述更新
- [x] 日志已更新

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

- [x] 范围内行为完成
- [x] 相关文档对齐（DAG / 事务 / docStatus 一致性已验证）
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿（仅对修改 orm.xml 的 Phase 1/3/4 执行后触发）
- [x] known-good-baselines.md 已更新
- [x] docs/bugs/ 至少新增 3 篇缺陷记录
- [x] docs/retrospectives/ 至少新增 2 篇
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

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

Status Note: `completed`（4 Phase 全部完成、全绿增量构建；独立结束审计已由新会话子代理通过）

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor (new session, no executor context reuse) — opencode independent subagent on 2026-07-07
- Audit Scope: 全部 4 Phase 退出标准 + Closure Gates 与实时仓库一致性、反空洞、文本一致性、延迟诚实、文档同步
- Verification Method: 对每个退出标准对应实时仓库文件（源 orm.xml、生成 `_app.orm.xml`、`docs/architecture/`、`docs/design/`、`docs/bugs/`、`docs/retrospectives/`、`docs/archive/`、`docs/context/`、`docs/plans/`、`docs/logs/`、AGENTS.md）执行 grep/glob/read 核验，而非仅信 `[x]`
- Findings:
  - C-1 已落地：`rg "erp_md_md_" --glob '**/*.orm.xml' --glob '!_tmp/**'` = 0 命中（仅 `docs/bugs/`、`docs/audits/`、`docs/logs/` 的描述性文本含历史引用，非活体错误）
  - C-2 已落地：`docs/architecture/data-dependency-matrix.md §4.1/§4.4` 现描述为 "SYNC 同步调用（独立事务传播 REQUIRES_NEW…）"，与 `docs/design/flow-overview.md §6.1` 一致；`docs/audits/2026-07-07-1900-…md §2.1` 已附审查修正裁决段落
  - C-3/C-4 已落地：`domain-design-guidelines.md §1.2` DAG 顺序与 `data-dependency-matrix.md §2.1` 一致；`document-engine.md §三轴` 已改为概念框架 + 各域 state-machine 链接
  - H-1~H-6 已落地：`docs/testing/known-good-baselines.md` 含 commit `957c288e` 基线条目；`docs/bugs/` 下 4 篇缺陷 + `docs/retrospectives/` 下 2 篇回顾均存在；master-data/sales orm.xml 文件头 `valueType="string"`；`docs/architecture/doc-model-design.md` 现为 13 行重定向说明，原文 487 行在 `docs/archive/architecture/doc-model-design.md`
  - M-1~M-4 已落地：purchase/sales/manufacturing/quality/maintenance/cs/finance 7 域无 `erp-<domain>/approve-status` 字典定义残留（inventory/ast 按计划保留）；`domain-design-guidelines.md §16.2` manufacturing 行 10 态无 INSPECTING；`docs/plans/2026-07-05-1500-2-cross-review-remediation.md` 已重命名；`docs/context/project-context.md` 验证表含 `bash docs/audits/nop-compliance-checker.sh`
  - L-1~L-4 已落地：`module-sales/model/app-erp-sales.orm.xml` `ErpSalOrderLine.project` to-one 已含 `tagSet="pub"`；`domain-design-guidelines.md §13` 子节编号为 13.1/13.2；AGENTS.md 阶段描述为 "codegen 已完成、待 BizModel 业务逻辑深化"；`docs/logs/2026/07-07.md` 顶部含本计划聚合日志条目
  - Anti-Hollow：未发现空函数体 / `return null` 占位 / 吞异常；所有修复均与实际仓库内容一一对应
  - Five-point consistency：Plan Status=completed 与各 Phase Status=completed / 各 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / 日志条目一致
  - Deferred Honesty：7 扩展域 posted/businessDate 字段补充 + codegen 重生成明确归 `Deferred But Adjudicated`（带 Trigger Condition，非活体缺陷隐藏）
- Audit Result: **approved** — 所有范围内项目已真实落地，无降级、无 hollow、无契约漂移隐藏
- Evidence:
  - Phase 1（C-1~C-4 严重）：
    - 4 源 orm.xml 修正：`module-{b2b,cs,logistics,contract}/model/app-erp-*.orm.xml` 7 处 `erp_md_md_*` → `erp_md_*`；grep 验证 0 残留
    - `mvn clean install -DskipTests -pl :app-erp-b2b-dao,:app-erp-cs-dao,:app-erp-logistics-dao,:app-erp-contract-dao -am`：BUILD SUCCESS
    - 生成 `_app.orm.xml` grep 验证：`erp_md_md_*` 0 处残留（b2b/cs/logistics/contract 共 4 个 dao 模块全量重生成表名正确）
    - C-2：`docs/architecture/data-dependency-matrix.md §4.1/§4.4` + `docs/audits/2026-07-07-1900-...md §2.1` 修正 REQUIRES_NEW 误判；`docs/design/flow-overview.md §6.1` 与 data-dependency-matrix.md §4.1 已对齐
    - C-3：`docs/design/domain-design-guidelines.md §1.2` DAG 已对齐 `data-dependency-matrix.md §2.1`
    - C-4：`docs/architecture/document-engine.md §三轴` 已重写为概念框架 + 各域 state-machine 链接
  - Phase 2（H-1~H-6 高）：
    - `docs/testing/known-good-baselines.md` 填入 commit `957c288e` 全绿基线 + 本期增量构建基线（2 条）
    - 4 篇缺陷记录：`docs/bugs/2026-07-07-1915-{sales-credit-control,voucher-amount,localdatetime-now,dao-updateentity}-*.md`
    - 2 篇回顾：`docs/retrospectives/2026-07-07-{missing-known-good-baselines,audit-quality-event-1300}.md`
    - master-data/sales orm.xml 文件头 `valueType=int` 已改为 `valueType="string"`
    - `docs/architecture/doc-model-design.md` 已 `git mv` 到 `docs/archive/architecture/`，原路径留重定向说明
    - `domain-design-guidelines.md §14.1.1` `orgId 内唯一` 是 docNo 唯一性唯一权威源（与归档文件原"全局唯一"已无冲突）
  - Phase 3（M-1~M-4 关键中）：
    - 7 个域冗余 `erp-<domain>/approve-status` 字典已删除（purchase/sales/manufacturing/quality/maintenance/cs/finance）；inventory 字典+列引用保留（实测列引用 `erp-inv/approve-status` 而非 `wf/approve-status`）
    - M-1 下游修复：删除 maintenance 字典后引发 `ErpMntDaoConstants.APPROVE_STATUS_*` 生成常量丢失（main 1 处 + test 4 处），范式对齐 purchase/sales，将四态常量显式声明到 `ErpMntConstants.java`（service 层）并替换所有引用
    - `mvn clean install -DskipTests -pl :app-erp-{purchase,sales,manufacturing,quality,maintenance,cs,finance}-dao -am`：BUILD SUCCESS
    - `domain-design-guidelines.md §16.2` manufacturing 工单状态删除 `INSPECTING`，对齐 10 态字典
    - `2026-07-05-1500-1-cross-review-remediation.md` → `2026-07-05-1500-2-cross-review-remediation.md`（含文件标题与 docs/logs、docs/plans 跨文件引用同步）
    - `project-context.md` 验证命令表增补 `bash docs/audits/nop-compliance-checker.sh`
  - Phase 4（L-1~L-4 低）：
    - `module-sales/model/app-erp-sales.orm.xml` `ErpSalOrderLine.project` to-one 补 `tagSet="pub"`
    - `mvn clean install -DskipTests -pl :app-erp-sales-dao -am`：BUILD SUCCESS
    - `domain-design-guidelines.md §13` 子节编号修正（12.1/12.2 → 13.1/13.2）
    - `AGENTS.md` 当前项目阶段更新为 "codegen 已完成、待 BizModel 业务逻辑深化"，附完整 18+1 域列表与 codegen 后阶段规则
    - `docs/logs/2026/07-07.md` 顶部追加本计划执行记录
  - 全工作区验证（M-1 下游修复后）：
    - `mvn clean install -DskipTests`（全工作区 154 reactor 模块）：BUILD SUCCESS
    - `mvn test`（全工作区）：BUILD SUCCESS / 0 failures / 0 errors（含 maintenance 39 tests + 全部其他域既有测试）

Follow-up:

- 独立结束审计已完成（见上方 Closure Audit Evidence，新会话子代理 2026-07-07 通过）
- Deferred 后续工作（已在 Deferred But Adjudicated 段落记录）：7 个扩展域（cs/hr/logistics/b2b/contract/drp/aps）补充 posted/businessDate 标准字段 + 后续 codegen 重新生成
