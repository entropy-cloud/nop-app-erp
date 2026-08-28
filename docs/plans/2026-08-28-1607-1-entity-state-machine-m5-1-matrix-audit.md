# Plan 2026-08-28-1607-1: entity-state-machine-m5-1-matrix-audit

## Status: draft

## Current Baseline

- Roadmap: `docs/backlog/entity-state-machine-migration-roadmap.md` 最近更新于 2026-08-14（M4.65 done）。
- **前置依赖全部 done**：M0.1 契约 done、M0.2 清单 done、M1.1-1.3 试点 done、M2.1-2.19 非保护域直接生命周期 done、M3.1-3.19 复杂/审批轴 done、M4.1-4.65 财务影响/保护域 done。
- **已完成 Bean 总数**：`find . -name "Erp*StateMachine.java" -not -path "*/target/*"` 共 **106 个** Erp*StateMachine.java（迁移覆盖 19 域）。
- 既有检查工具：`scripts/` 目录现含 8 个 CI 辅助脚本（check-orm-auth-i18n.js 等）；`docs/audits/scripts/` 含跨模块依赖提取 + mutation 分类工具；尚**无状态机矩阵检查工具**。
- 最近同类 plan 范例（参模板风格）：
  - `docs/plans/2026-08-14-2000-1-erpct-rebate-settlement-state-machine-bean.md`（M4.65，最后一批 Bean）
  - `docs/plans/2026-08-14-1931-1-erpast-core-lifecycle-state-machine-beans.md`（assets 域 M4 起步）
- 验证基线（来自 `docs/testing/known-good-baselines.md` 最新行）：全 reactor 3889 tests / 0 failures / 0 errors / 1 skipped / 658 报告文件。

## Goal

按 `entity-state-machine-migration-roadmap.md §M5.1` 要求，**对 106 个已迁移 Erp*StateMachine Bean + 19 域全部纳入轴形成可追溯审计结论**，不修改任何生产代码、不接 CI、不动 ORM/dict/writer。

### Deliverables（D1-D3）

- **D1**：`docs/audits/state-machine-matrix-audit.md`（**报告文档**）
  - 4 维度对账结论：状态可达性 / 终态出边 / 重复冲突边 / dict-writer 对照
  - 覆盖全部 106 Bean（不抽样）
  - 表格化产出：每 Bean 一行（含 ID、domain、entity、transitions 数、initial/terminal/dict 一致性、writer 一致性、finding 引用）
  - 报告末尾聚合：异常清单 + 残余风险 + 给 M5.2 守卫脚本的输入清单
- **D2**：`docs/audits/scripts/state-machine-coverage-check.py`（**Python 检查工具**）
  - 输入：扫描 `module-*/erp-*-service/src/main/java/app/erp/*/service/state-machine/` 全部 Erp*StateMachine.java + ORM dict + Java 常量 + writer 路径
  - 输出：JSON + Markdown 双格式
  - 确定性输出：exit 0（全部通过）/ exit 1（存在异常）+ stderr 日志
  - **不依赖 nop-entropy 平台代码**（纯 stdlib + re + pathlib）；可离线运行
  - 误报裁决：白名单机制（known-false-positives 数组），含 3-5 条初始白名单（来自审计报告确认的 known issue）
- **D3**：`docs/architecture/state-machine-matrix.md`（**新建维护入口文档**）
  - 章节 1：审计方法学（4 维度对账口径）
  - 章节 2：检查工具使用说明（如何跑 + 如何读输出）
  - 章节 3：**新增状态/动作维护义务清单**（开发者加新状态时必须同步检查矩阵 + dict + owner doc + 测试 + 守卫脚本）
  - 章节 4：已知误报白名单（与 D2 工具白名单同步）

## Non-Goals

- 不修任何 `module-*/model/*.orm.xml`（保护区域，需 dual-agent-approval）
- 不修任何 `_gen/`、`_` 前缀文件、`_app.orm.xml`、`_service.beans.xml`
- 不接 CI/CD 平台（M5.2 范围）
- 不写新 Java 代码 / 不写新 Bean
- 不修改 roadmap 状态字段
- 不引入反射/泛型全局 IStateMachine 调度器（与 roadmap §Non-Goals 一致）
- 不覆盖 M5.1 中发现的真问题（任何 finding 留作 deferred successor，由独立 plan 修复）

## Skill

- `docs/skills/state-machine-business-review-prompt.md`（**核心方法学**——审计每条状态轴的 4 维对账）
- `docs/skills/behavioral-failure-mode-scan-prompt.md`（**辅助**——grep B1/B2 异常模式）
- `docs/skills/code-quality-audit-prompt.md`（**辅助**——通用代码质量审视）
- **不**使用 `audit-remediation-roadmap-authoring-prompt`（本 plan 是 mission 收官而非新 audit mission）

## Approach（3 阶段 + 复盘）

### Phase 0 — 前置阅读（10 项必读）

- [ ] Read `AGENTS.md` §任务路由 + §强制技能加载 + §当前项目阶段
- [ ] Read `docs/context/project-context.md`
- [ ] Read `docs/context/ai-autonomy-policy.md`
- [ ] Read `docs/plans/00-plan-authoring-and-execution-guide.md`（**模板与纪律**）
- [ ] Read `docs/audits/00-audit-execution-guide.md`
- [ ] Read `docs/backlog/00-roadmap-authoring-guide.md`
- [ ] Read `docs/skills/README.md` 项目定制化层 + §已知失败模式
- [ ] Read `docs/skills/state-machine-business-review-prompt.md`（**核心方法学**）
- [ ] Read `docs/skills/behavioral-failure-mode-scan-prompt.md`（grep 程式）
- [ ] Read `docs/architecture/processor-extension-pattern.md`（StateMachine Bean 上下文）

### Phase 1 — 编写并跑检查工具（核心产出 D2）

- [ ] 起草 `docs/audits/scripts/state-machine-coverage-check.py`（初版）
  - 扫 19 域共 106 个 Erp*StateMachine.java
  - 解析 Bean 类元数据：transitions / initial / terminal / assertCanXxx / *TargetStatus() 命名
  - 对照 ORM dict（*.dict.yaml 或 orm.xml `<dict>` 段）
  - 对照 Java 常量（*Constants.java 命名 STATUS_X）
  - 对照 writer 路径（grep `setStatus(*_X)` 全 src/main 命中数）
- [ ] 工具必须确定性：跑 3 次输出一致
- [ ] 故意制造失败场景测试（如临时改 1 个 dict 文件，看工具是否能检测 dict-Bean 不一致）
- [ ] 工具产出 JSON + Markdown 双格式
- [ ] 工具含白名单机制（known false positives）

### Phase 2 — 编写审计报告（核心产出 D1）

- [ ] 跑工具产出原始数据
- [ ] 人工补审：跨 19 域的所有 owner doc `state-machine.md` 与工具输出一致性
- [ ] 报告结构：
  - §1 摘要（106 Bean / 4 维度对账结论 / finding 总数）
  - §2 4 维度对账结论（每维度独立小节）
  - §3 异常清单表格（Bean ID / domain / 异常类型 / 控制点 / 建议）
  - §4 已知误报白名单（含理由）
  - §5 残余风险（留待 M5.2 / 独立 Fix plan 处理）
  - §6 给 M5.2 守卫脚本的输入清单
- [ ] 报告**不抽样**：覆盖全部 106 Bean

### Phase 3 — 编写维护入口文档（核心产出 D3）

- [ ] 新建 `docs/architecture/state-machine-matrix.md`
- [ ] 章节 1：审计方法学（与 D1 报告同口径）
- [ ] 章节 2：检查工具使用说明
- [ ] 章节 3：新增状态/动作维护义务清单（开发者加新状态时必须同步）
- [ ] 章节 4：已知误报白名单（与 D2 工具白名单同步）

### Phase 4 — 验证

- [ ] `python3 docs/audits/scripts/state-machine-coverage-check.py` 退出码 0
- [ ] `mvn test -pl module-finance/erp-fin-service -am` 不引入新失败（**仅一个域**，避免 M5.1 跨域全量）
- [ ] 报告覆盖 106 Bean（与 Phase 2 列表一致）

### Phase 5 — 复盘与准备独立 plan-audit

- [ ] 完整检查清单（Phase 0-4 全部 ✓）
- [ ] Plan Status 仍为 draft（待独立 plan-audit）
- [ ] 准备 plan-audit 子代理 prompt（draft review 提示）

## Phases Breakdown

| Phase | 类型 | 关键产出 | Skill |
|---|---|---|---|
| 0 前置阅读 | Proof | 10 项阅读记录 | — |
| 1 检查工具 | Add | `state-machine-coverage-check.py` (300+ 行) | state-machine-business-review |
| 2 审计报告 | Add / Proof | `state-machine-matrix-audit.md` (4 维对账 106 Bean) | state-machine-business-review |
| 3 维护入口 | Add | `state-machine-matrix.md` (4 章节) | state-machine-business-review |
| 4 验证 | Proof | 单域 mvn test + 工具退出码 | nop-testing |
| 5 复盘 | Decision | Plan Status: draft + plan-audit prompt | — |

## Closure Gates

- CG1：`python3 docs/audits/scripts/state-machine-coverage-check.py` 退出码 0 ✅
- CG2：`docs/audits/state-machine-matrix-audit.md` 覆盖 106 Bean（行数 ≥ 106 行主表 + 维度章节齐全）✅
- CG3：报告 4 维度对账每维度有独立结论小节（状态可达性 / 终态出边 / 重复冲突边 / dict-writer 对照）✅
- CG4：`docs/architecture/state-machine-matrix.md` 含「新增状态/动作维护义务清单」章节，明示开发者加新状态时的同步清单 ✅
- CG5：`mvn test -pl module-finance/erp-fin-service -am` 不引入新失败 ✅
- CG6：5 项产物均存在且不为空（plan doc / 工具脚本 / 审计报告 / 维护入口 / known-false-positives 白名单）✅

## Plan Status: draft

## Draft Review Record

（待独立 plan-audit 子代理审查后填写）
