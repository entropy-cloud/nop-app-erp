# 多维审计报告（独立子代理）

**日期**: 2026-07-20  
**审计师**: 独立子代理（独立审查）  
**分支**: `main`（脏工作区）  
**HEAD**: `19fda6333` + 9 个未提交修改文件

---

## 裁决: `passes multi-dimensional audit`

无阻塞问题。下面按严重程度排序详细说明风险。

---

## 🔴 高（1 个问题）

### H1. `LocalDate.now()` 在测试代码中回归（6 处，4 个文件）

**违反**: `docs/context/conventions.md` §时间 API 使用约定 —— 生产与测试代码必须使用 `CoreMetrics.today()` / `CoreMetrics.currentDate()` 而非 `LocalDate.now()`。2026-07-08 清理计划声称清零，但当前已提交工作树仍有 6 个遗漏点：

| 文件 | 行号 |
|---|---|
| `module-cs/erp-cs-service/src/test/java/.../TestErpCsQualityDashboard.java` | 66, 84, 107 |
| `module-manufacturing/erp-mfg-service/src/test/java/.../TestErpMfgDashboardCrpChart.java` | 150 |
| `module-finance/erp-fin-service/src/test/java/.../TestErpFinBudgetEndToEnd.java` | 309 |
| `module-finance/erp-fin-service/src/test/java/.../TestErpFinBudgetIsolation.java` | 157 |

这些是 2026-07-08 之后新增的测试文件，遗漏了约定清理。

**影响**: 午夜后快照漂移破坏（与 07-18 在 13 个域中系统性修复的类型完全相同）。这些文件将在日期翻页时导致 `mvn test` 失败（7/20 → 7/21）。

**建议**: 替换 6 处为 `CoreMetrics.today()` / `CoreMetrics.currentDate()`，作为高优先级修复。

---

## 🟡 中（3 个问题）

### M1. 合规性检查器脚本从未实际运行

- `docs/audits/nop-compliance-checker.sh`（312 行，功能完备的启发式检测器）存在且注册于 `project-context.md` 验证命令中，但日志历史中**没有任何运行证据**。
- **风险**: 已知反模式（如不安全 `dao().updateEntity()` 使用、不正确 import）仍潜伏。
- **建议**: 在每次完整构建时运行 `bash docs/audits/nop-compliance-checker.sh` 并记录结果。

### M2. 脏工作区处于 mid-plan 状态

- `git status` 显示 9 个文件修改（view.xml mfg/assets/projects F4 P2 P2 子表编辑器 + 设计文档 + 计划文件）。计划 `2026-07-20-1020-3` 仍 `active` 非 `completed`。
- **风险**: 中断时无回滚点。
- **建议**: 完成当前 F4 P2 P2 计划并通过结束审计后提交。

### M3. 已知失败模式自检

| 模式 | 结果 |
|---|---|
| 跨模块外部实体引用双表前缀 | ✅ 已验证正确 |
| 章节重编号后残留引用 | ✅ 未重编号 |
| `dao().updateEntity()` 越权访问 | ✅ 遵循通过 IBiz 接口路径 |
| `System.currentTimeMillis()` 在生产代码 | ✅ 零命中 |
| 业务异常非 `NopException` | ✅ 零命中 |
| `@Inject private` | ✅ 零命中 |
| 字符串 `==`/`!=` | ⚠️ 未专门扫描（合规检查器可捕获） |
| propId 断续 | ✅ 模型未变更 |

---

## 🟢 低 / 信息性（5 个观察）

### L1. Owner-doc 一致性：优秀
日志显示过账红冲闭环系统性审计后主动修复 owner-doc 漂移（`expense-claim.md`, `bad-debt.md`, `variance-analysis.md`）。状态机文档与 `domain-design-guidelines.md` 三轴状态分离完全一致。

### L2. 验证成熟度：优秀
一致验证链条：`mvn clean install -DskipTests` → `mvn test` → `ErpAllWebPagesCollectTest` → Playwright。日志记录精确时间与测试计数。

### L3. 范围纪律：优秀
近期计划明确声明"纯测试+文档，零生产代码/契约/ORM 模型变更"。当生产代码变更时，范围严格限定并通过 stash 红绿反转证明。

### L4. 已知失败模式管理良好
useWorkflow 裁决（NOT FEASIBLE）基于坚实探索理由。notify inbox saga 展示了真实审计价值。

### L5. Bug 笔记未在 known-good-baselines 中交叉引用
当前基线仅列 `none`，无法区分哪些 bug 已修复/活跃。

---

## 维度摘要

| 维度 | 状态 | H | M | L |
|---|---|---|---|---|
| 需求正确性 | ✅ | 0 | 0 | 0 |
| Owner-doc 一致性 | ✅ | 0 | 0 | 2 |
| 架构/边界影响 | ✅ | 0 | 0 | 0 |
| 验证充分性 | ⚠️ | 0 | 1 | 1 |
| 回归风险 | ⚠️ | 1 | 0 | 0 |
| 路由/技能选择 | ✅ | 0 | 0 | 1 |
| 待办/自主权漂移 | ✅ | 0 | 0 | 1 |
| 项目定制层 | ⚠️ | 0 | 2 | 0 |
