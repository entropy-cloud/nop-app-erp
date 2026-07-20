# 2026-07-20-2200-1 独立审计发现整改计划

> Plan Status: active
> Last Reviewed: 2026-07-20
> Source: `audit/2026-07-20-independent-multi-dimensional-audit.md`, `audit/2026-07-20-independent-open-ended-audit.md`, 代码质量审计、平台合规审计、状态机审计（3 独立子代理）
> Related: `docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md`
> Audit: required

## Current Baseline

5 份审计（多维 + 开放式 + 代码质量 + 平台合规 + 状态机）均裁决 `passes`（无阻塞问题），累计识别以下整改项：

### 前序审计（多维 + 开放式）

| ID | 严重度 | 简述 | 来源审计 |
|----|--------|------|----------|
| H-1 | 高 | `LocalDate.now()` 测试代码回归：4 文件 6 处，07-08 清理声称清零但新增测试遗漏替换 | 多维 |
| H-2 | 高 | `PAGE_ERROR_COUNT` 验证环境不稳定：同日内 0↔213 跳变，JDK 26 / antlr 版本不兼容 | 开放式 |
| H-3 | 高 | 代码生成产物编辑重复失败模式：notify inbox + business-type.dict.yaml 两案均需 3 轮审计才捕获根因，需提升为持久教训 | 开放式 |
| M-1 | 中 | 合规检查器脚本 `docs/audits/nop-compliance-checker.sh` 存在但从未实际运行 | 多维 |
| M-2 | 中 | XWF 审批浏览器层可行性裁决（NOT FEASIBLE）后，`roles-and-permissions.md` 等下游文档未更新 | 开放式 |
| M-3 | 中 | Owner-doc 漂移无系统性扫描机制，仅靠审计对抗性抽样发现 | 开放式 |
| L-1 | 低 | Bug 笔记未在 `docs/testing/known-good-baselines.md` 中交叉引用，活跃/已修复不可分辨 | 多维 |
| L-2 | 低 | `docs/errors/` 目录缺失，错误码管理无集中索引或冲突注册表 | 开放式 |
| L-3 | 低 | `docs/plans/` 累积 269 个文件，信号噪声比下降 | 开放式 |
| L-4 | 低 | `docs/input/` 仅含指南文件，外部输入不可追溯 | 开放式 |

### 追加审计（代码质量 + 平台合规 + 状态机）

| ID | 严重度 | 简述 | 来源审计 |
|----|--------|------|----------|
| H-4 | 高 | `RebateEngine.java:74` — `dao().saveEntity()` 绕过 CrudBizModel 生命周期。同行 47 处已修，此 1 处残留 | 代码质量 |
| H-5 | 高 | BizModel 中 290 处 `daoFor(Erp*)` 跨域引用 + 985 处总 `daoFor()` 调用，未全经 I*Biz 接口，可能丢失数据权限管道 | 平台合规 |
| M-4 | 中 | `domain-design-guidelines.md` §16.2 与 quality/logistics/aps/drp 各域 `state-machine.md` 不一致（4 处） | 状态机 |
| M-5 | 中 | 42 个 Processor 无对应 `.xbiz.xml` 桥接文件，Delta 定制方缺 VFS 层切入点 | 平台合规 |
| M-6 | 中 | 13 处 `dao().findAllByQuery()` 在 BizModel 中绕过 CrudBizModel 管道，部分未注释原因 | 平台合规 |
| L-5 | 低 | Contract CANCELLED 态在 `contract/state-machine.md` 定义表中缺失（§1 有 6 态，§2 迁移图用了 CANCELLED） | 状态机 |
| L-6 | 低 | 多域 Processor 中 `isAlreadyApproved`/`isAlreadyRejected` 等状态判断方法重复复制 | 代码质量 |
| L-7 | 低 | 18 处 `new Erp*()` 直接构造实体，应使用 `newEntity()` 工厂方法 | 代码质量 |
| L-8 | 低 | B2B SENT→ERROR 路径未定义；资产直接报废 E2E 缺口 | 状态机 |

## Goals

- 修复 H-1：清空 `LocalDate.now()` / `LocalDateTime.now()` 测试代码回归
- 修复 H-2：消除 PAGE_ERROR_COUNT 验证噪声（要么修复 antlr 兼容性，要么临时 `@Disabled` 并注明）
- 修复 H-3：将代码生成产物编辑模式提升为 `docs/lessons/` 持久经验教训
- 修复 H-4：修复 `RebateEngine.java:74` 残留 DAO 绕过
- 修复 H-5：对 290 处 `daoFor(Erp*)` + 985 处 `daoFor()` 分类审计：哪些合法（Dispatcher/Processor）、哪些需改造为 I*Biz 接口
- 修复 M-1：将合规检查器纳入验证流程并在日志中记录运行结果
- 修复 M-2：更新 `roles-and-permissions.md` 和受影响域 owner docs 的状态机章节，注明 xwf 浏览器层受限
- 修复 M-3：建立周期性 owner-doc → 代码一致性扫描机制
- 修复 M-4：统一 `domain-design-guidelines.md` §16.2 与各域 `state-machine.md`
- 修复 M-5：为无 xbiz 的 Processor 补充桥接文件或记录理由
- 修复 M-6：审计 13 处 `dao().findAllByQuery()`，补充缺注释项的理由
- 修复 L-1~L-8：文档级改进

## Non-Goals

- 不修改 ORM 模型（`model/*.orm.xml`）
- 不涉及新功能设计或架构变更
- 不修改 _gen/ 目录下的生成文件
- 不涉及 xwf 浏览器层可行性的重新探索（2330-1 裁决为权威结论）
- 不处理 07-20 META 审计已修复的 skills 改进（技能系统的 12 缺口已在 07-20 闭环）
- H-5 不要求立即改造全部 985 处 `daoFor()`，仅做分类审计和记录（`Decision` 级别）
- M-6 不要求改造全部 13 处 `findAllByQuery()`，仅审计注释完整性

## Task Route

- Type: `bug investigation | implementation-only change | verification or audit work`
- Owner Docs: `docs/testing/known-good-baselines.md`, `docs/bugs/`, `docs/errors/`, `docs/plans/00-plan-authoring-and-execution-guide.md`, `docs/lessons/README.md`, `docs/design/domain-design-guidelines.md`, `docs/design/contract/state-machine.md`, `docs/design/quality/state-machine.md`, `docs/design/logistics/state-machine.md`, `docs/design/aps/state-machine.md`, `docs/design/drp/state-machine.md`
- Skill Selection Basis: `nop-debugging` 用于 LocalDate.now() 回归清理和 PAGE_ERROR_COUNT 诊断；`nop-backend-dev` 用于 daoFor 审计、Processor xbiz 补齐、RebateEngine 修复

## Infrastructure And Config Prereqs

- H-2 修复需要确认 JDK 版本兼容性或添加 `@Disabled` 条件；可能涉及 `pom.xml` 的 antlr 版本管理或 JDK 约束（ask-first if touching pom）
- No other infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 — 高严重性修复 (H-1 ~ H-5)

Status: planned
Targets: `module-cs/erp-cs-service/src/test/`, `module-manufacturing/erp-mfg-service/src/test/`, `module-finance/erp-fin-service/src/test/`, `module-contract/erp-ct-service/src/main/java/`, `docs/lessons/`, `docs/bugs/`, `docs/testing/known-good-baselines.md`
Skill: `nop-debugging | nop-backend-dev`

- Item Types: `Fix | Add | Decision`
- Prereqs: 确认仓库当前 git status 干净（F4 Phase 2 P2 P2 完成后提交）

- [ ] H-1 Fix: 替换 4 文件 6 处 `LocalDate.now()` → `CoreMetrics.today()` / `CoreMetrics.currentDate()`
  - `TestErpCsQualityDashboard.java:66,84,107`
  - `TestErpMfgDashboardCrpChart.java:150`
  - `TestErpFinBudgetEndToEnd.java:309`
  - `TestErpFinBudgetIsolation.java:157`
  - `@Inject` 字段声明检查（平台约定：不可 private）；用 `mvn test -pl <affected-modules>` 确认清理后测试全绿
  - Skill: `nop-debugging`
- [ ] H-2 Decision: PAGE_ERROR_COUNT 稳定性修复方案选择
  - 方案 A：修复 antlr 版本兼容性（涉及 pom.xml，ask-first）
  - 方案 B：将 `ErpAllWebPagesCollectTest` 临时 `@Disabled` 直到独立环境修复，并注释明确重新启用条件
  - 方案 C：仅记录已知限制到 `known-good-baselines.md` 的 Known Failures 段
  - **默认决策树**：始终执行方案 C（文档记录）；若方案 A 获人工批准则一并执行 A；否则执行方案 B 作为运行时缓解。executor 必须在执行日志中记录实际选择与理由
  - Skill: `nop-debugging`
- [ ] H-3 Fix: 在 `docs/lessons/` 新增教训 06：「代码生成产物编辑：`_` 前缀和 `__XGEN_FORCE_OVERRIDE__` 文件被 `mvn clean install` 覆盖——始终编辑保留层」
  - 引用 notify inbox saga（3 轮审计）和 business-type.dict.yaml（3 轮审计）作为真实案例
  - 在 `docs/lessons/README.md` 注册新教训
  - Skill: none
- [ ] H-3 Proof: 新增后验证 `docs/lessons/README.md` 格式正确、引用文件路径有效
- [ ] H-4 Fix: 修复 `RebateEngine.java:74` 的 `dao().saveEntity(accrual)` → 注入 `IErpCtRebateAgreementBiz` 接口调用 `save()`
  - 验证：`mvn test -pl module-contract/erp-ct-service` 全绿
  - Skill: `nop-backend-dev`
- [ ] H-5 Decision: 对全仓库 `daoFor()` 调用做分类审计
  - 按调用位置分类：BizModel / Dispatcher / Processor / Listener / Service / Helper
  - 量化每类中合法的（Dispatcher/Processor 跨域构造凭证）与可疑的（BizModel 中应经 I*Biz 但不经的）
  - 输出审计结果到 `docs/analysis/` 或 `known-good-baselines.md`
  - **不要求改造**，仅分类记录作为后续计划输入
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 6 处 `LocalDate.now()` 清零确认（grep 验证）
- [ ] PAGE_ERROR_COUNT 方案已记录到 `known-good-baselines.md` 或 `docs/bugs/`
- [ ] 新教训 06 文件存在且在 README 注册
- [ ] `RebateEngine.java:74` 修复且 ct-service 单测全绿
- [ ] `daoFor()` 分类审计报告产出（不要求改造完成）

### Phase 2 — 中严重性修复 (M-1 ~ M-6)

Status: planned
Targets: `docs/audits/nop-compliance-checker.sh`, `docs/design/roles-and-permissions.md`, `docs/design/domain-design-guidelines.md`, 受影响域 state-machine.md，各域 Processor 所在目录
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Decision`
- Prereqs: Phase 1

- [ ] M-1 Fix: 将合规检查器集成到验证流程
  - 在 `project-context.md` 验证命令表中确认 `bash docs/audits/nop-compliance-checker.sh` 命令可执行
  - 检查合规检查器性能（当前全目录 `find` 可能超时 ≥30s），优化 `-prune` 排除规则
  - 在 `known-good-baselines.md` 的 Future Baselines 段新增一行记录首次运行结果
  - Skill: `nop-backend-dev`
- [ ] M-2 Fix: 更新 `docs/design/roles-and-permissions.md` 和受影响域 owner docs
  - 在 roles-and-permissions.md 的「运行基线」节后新增「浏览器层审批路径已知限制」小节，引用 2330-1 裁决
  - 受影响域（finance Payment/Receipt、assets Disposal、hr Salary）的 `state-machine.md` 追加注记
  - Skill: none
- [ ] M-3 Decision: 建立 owner-doc 漂移扫描机制（采用方案 B+C，方案 A 列为 Follow-up）
  - **选择**：方案 C 作为基线（在 `nop-platform-conformance-audit-prompt.md` 与 closure audit 提示模板中新增「owner-doc → 代码关键断言抽样核查」维度）+ 方案 B 作为同步增强（更新审计提示模板）
  - **替代方案**：方案 A（独立扫描脚本）成本高、误报风险大，列为 Follow-up：当 owner docs 总量 > 50 或单次 closure audit 抽样发现 ≥ 2 处漂移时重新评估
  - **残留风险**：抽样核查可能漏掉低频漂移；通过将抽样纳入每次 closure audit 标准步骤降低漏检率
  - 输出：更新后的提示模板文件路径与新维度说明
  - Skill: none
- [ ] M-4 Fix: 对齐 `domain-design-guidelines.md` §16.2 与 4 域 `state-machine.md`
  - quality: §16.2 的 DRAFT/IN_PROGRESS/COMPLETED/CANCELLED → 实际 Inspection 用 PENDING/ACCEPTED/CONDITIONAL/REJECTED，NCR 用 OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED
  - logistics: §16.2 的 5 态（DRAFT/CONFIRMED/IN_TRANSIT/DELIVERED/CANCELLED）→ 实际 6 态（DRAFT/ADVISED/DISPATCHED/IN_TRANSIT/DELIVERED/CANCELLED）
  - aps: §16.2 的 COMPLETED → 实际 FINISHED
  - drp: §16.2 缺 EXECUTED 终态
  - Skill: none
- [ ] M-5 Fix: 对 42 个无 `.xbiz.xml` 的 Processor 逐点审计
  - 分类：哪些由 `@BizMutation` 内 `inject()` 直接调用（合法，不需 xbiz）/ 哪些需要桥接文件
  - 需要桥接的补充 `<action>` 声明文件
  - 不需桥接的在 `docs/architecture/processor-extension-pattern.md` 记录理由
  - Skill: `nop-backend-dev`
- [ ] M-6 Fix: 审计 13 处 `dao().findAllByQuery()` 在 BizModel
  - 已有注释说明原因的（如 `isCodeUnique` 绕 filter-op 限制）→ 确认注释存在
  - 缺注释的 → 补充原因注释
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 合规检查器至少运行一次（含性能优化）并记录结果
- [ ] `roles-and-permissions.md` 新增 xwf 浏览器层受限小节
- [ ] 受影响域 state-machine.md 已更新
- [ ] §16.2 与 4 域 state-machine.md 已对齐
- [ ] 42 Processor xbiz 审计结果已记录（含不需桥接的理由）
- [ ] 13 处 `findAllByQuery()` 注释已补齐
- [ ] 漂移扫描机制已落地（提示模板更新 + closure audit 步骤新增）；方案 A 作为 Follow-up 已记录触发条件

### Phase 3 — 低严重性修复 (L-1 ~ L-8)

Status: planned
Targets: `docs/testing/known-good-baselines.md`, `docs/bugs/`, `docs/errors/`, `docs/plans/`, `docs/input/`, `docs/design/contract/state-machine.md`, 各域 Processor.java
Skill: none

- Item Types: `Fix | Add`
- Prereqs: none（可独立执行）

- [ ] L-1 Fix: 在 `known-good-baselines.md` 表格中新增「活跃错误」列
  - 列出 `docs/bugs/` 中当前活跃的已知问题
  - Skill: none
- [ ] L-2 Add: 新建 `docs/errors/README.md` 作为错误码管理索引
  - 引用各域 `Erp*Errors` 接口位置
  - 引用 `domain-design-guidelines.md §7.1` ErrorCode 命名约定
  - 引用 `docs/lessons/03-process-doc-status-naming.md` 相关经验
  - Skill: none
- [ ] L-3 Add: 制定 `docs/plans/` 保留政策
  - 在 `00-plan-authoring-and-execution-guide.md` 中新增计划生命周期管理小节（何时归档已完成计划）
  - Skill: none
- [ ] L-4 Fix: 在 `docs/input/README.md` 中说明当前无原始输入的原因
  - 或记录项目阶段已过需求澄清期，input 目录为空是设计选择
  - Skill: none
- [ ] L-5 Fix: `contract/state-machine.md` 定义表追加 CANCELLED 态
  - §1 状态定义表追加 CANCELLED行（DRAFT→CANCELLED 草稿废弃路径的终态）
  - 解决文档自身已标注的 `"not in 10 dimensions?"` TODO
  - Skill: none
- [ ] L-6 Fix: 提取 Processor 中重复的 `isAlreadyApproved`/`isAlreadyRejected` 方法
  - 作为实体方法上提（如 `ErpPurOrder.isApproved()`）
  - 或提取为共享工具类
  - Skill: `nop-backend-dev`
- [ ] L-7 Fix: 审计 18 处 `new Erp*()` 直接构造
  - 替换为 `dao().newEntity()` 工厂方法
  - 对非实体 POJO（DTO/Service 对象）保留 `new` 并注释说明
  - Skill: `nop-backend-dev`
- [ ] L-8 Fix: `b2b/state-machine.md` 补充 SENT→ERROR 路径定义
  - 定义超时未收到对方确认时的错误处理路径
  - 资产报废 E2E 缺口记录到 `known-good-baselines.md` 的已知缺口段
  - Skill: none

Exit Criteria:

- [ ] `known-good-baselines.md` 含活跃错误列
- [ ] `docs/errors/README.md` 存在且包含注册表信息
- [ ] `00-plan-authoring-and-execution-guide.md` 含计划生命周期管理
- [ ] `docs/input/README.md` 声明当前状态
- [ ] contract/state-machine.md CANCELLED 态已追加
- [ ] Processor 重复方法已提取（或方案已记录）
- [ ] `new Erp*()` 审计完成
- [ ] b2b/state-machine.md SENT→ERROR 路径已补充

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is after fix-forward (review session 2026-07-20) — 2 Major issues found and fixed in place:
  1. Phase 1 Targets 路径错误 `module-mfg/` → `module-manufacturing/`（物理目录名验证）
  2. M-3 违反反松弛规则（Exit Criteria 允许 Decision "deferred"）— 已改为采用方案 B+C，方案 A 列为带触发条件的 Follow-up
- Minor：H-2 Decision 增加默认决策树，明确执行顺序 C 始终执行、A 优先（需 ask-first）、B 兜底；不阻塞执行。

## Closure Gates

- [ ] 范围内行为完成（3 Phase 全部 `[x]`）
- [ ] 相关文档对齐
- [ ] 已运行验证（Phase 1 的 grep 验证 + Phase 1 H-1 mvn test + 全 reactor build 确认无回归）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 脏工作区清理（多维 M2）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F4 Phase 2 P2 P2 正在执行中，9 个脏文件是正常开发中途状态，计划完成后提交即可自然解决
- Successor Required: `no`

## Closure

Status Note: （待实施后填写）

Closure Audit Evidence:

- Auditor / Agent: （待独立结束审计）
- Evidence: （待填写）

Follow-up:

- （待实施后填写）
