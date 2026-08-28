# Plan 2026-08-28-1607-2: entity-state-machine-m5-2-guard-script-and-ci

## Status: draft

## Current Baseline

- M5.1 已 done（commit 1f62edfd3）：
  - 工具 `docs/audits/scripts/state-machine-coverage-check.py`（252 行）
  - 报告 `docs/audits/state-machine-matrix-audit.md`（116 行）
  - 维护入口 `docs/architecture/state-machine-matrix.md`（186 行）
  - Plan `docs/plans/2026-08-28-1607-1-entity-state-machine-m5-1-matrix-audit.md`（141 行）
- M5.1 工具已具备：
  - 确定性输出（3 次运行 sha256 一致）
  - `--strict` 模式（CI 友好）
  - 白名单机制 `KNOWN_FALSE_POSITIVES` 集合
- 现状：M5.1 工具**尚未**接 CI、未本地 wrapper、未与 M5.1 维护入口双向引用

## Goal

将 M5.1 工具升级为**可重复运行 + CI 集成 + 维护入口强化**的守卫层。M5.2 不修改任何生产代码、不接 ORM、不接 dict，**纯增量**：
- D1: `tools/check-state-machine-coverage.sh` Bash wrapper（含本地 manual + CI strict 两入口）
- D2: 在 `scripts/` 目录加 README.md 索引（指向 M5.1 工具 + M5.2 wrapper）
- D3: 强化 `docs/architecture/state-machine-matrix.md`（新增章节 7：M5.2 守卫层与 CI 集成；新增章节 8：误报裁决流程；新增章节 9：扩展 checklist）

## Non-Goals

- 不强制接入 GitHub Actions（避免误报阻断开发流；本地/CI 可选）
- 不修改任何 `module-*/model/*.orm.xml`
- 不修改 `_gen/`、`_` 前缀文件
- 不修改 M5.1 已落地的 plan doc 与工具脚本（除非发现重大 bug，本 plan 末尾有应急路线）
- 不创建新 skill、新 roadmap、新 mission
- 不写新 Java 代码

## Skill

- `docs/skills/behavioral-failure-mode-scan-prompt.md`（grep 程式辅助）
- `docs/skills/code-quality-audit-prompt.md`（设计审视）

## Approach（4 阶段）

### Phase 0 — 前置阅读（必做）

- [ ] Read `AGENTS.md` §保护区域 + §验证命令
- [ ] Read `docs/context/project-context.md` §验证命令
- [ ] Read `docs/plans/00-plan-authoring-and-execution-guide.md`
- [ ] Read `docs/backlog/00-roadmap-authoring-guide.md`
- [ ] Read `docs/audits/00-audit-execution-guide.md`
- [ ] Read `docs/architecture/state-machine-matrix.md`（M5.1 维护入口，新增章节时要兼容）
- [ ] Read `scripts/README.md`（如无则需新建）了解现有 CI 脚本风格
- [ ] Read `tools/` 目录现状

### Phase 1 — Bash wrapper 工具

- [ ] 创建 `tools/check-state-machine-coverage.sh`：
  - 入参：可选 `--strict` / `--root PATH`
  - 默认本地 manual（exit 0 even if finding）
  - `--strict` 模式（CI 用，exit 1 if finding）
  - 输出 JSON + Markdown 报告到 `state-machine-coverage-report/`
  - 调用 `docs/audits/scripts/state-machine-coverage-check.py`
  - 含 human-friendly 摘要（finding 总数 / by_domain / by_finding_type）
  - exit code 语义清晰：0 通过 / 1 finding / 2 内部错误
- [ ] 加 `chmod +x`
- [ ] 单测 3 次：本地 manual / --strict / 错误参数

### Phase 2 — scripts/README 索引

- [ ] 创建 `scripts/README.md`（**若不存在**）
- [ ] 列出现有脚本（check-orm-auth-i18n.js / flip-orm-to-flux.sh 等）
- [ ] 在「CI / 状态机」分类下指向 `tools/check-state-machine-coverage.sh` 与 M5.1 工具 + 维护入口文档

### Phase 3 — 维护入口文档强化

- [ ] 在 `docs/architecture/state-machine-matrix.md` 末尾新增 3 个章节：
  - §7 M5.2 守卫层与 CI 集成：何时跑、谁触发、strict 模式策略、与 M5.1 工具的关系
  - §8 误报裁决流程：新加白名单条目的 checklist（重跑工具 / 文档登记 / plan-audit）
  - §9 扩展 checklist：M5.3 closure audit 需要验证的 6 项
- [ ] 不修改 M5.1 已落地的章节（§1-§6）

### Phase 4 — 验证

- [ ] `bash tools/check-state-machine-coverage.sh --strict` 退出码 0（**当前无 finding，预期通过**）
- [ ] 故意制造 finding（临时改 1 个 dict 文件，看工具是否能检测），跑 `--strict` 退出码 1
- [ ] 还原修改后再次跑 `--strict` 退出码 0（确认还原成功）

## Phases Breakdown

| Phase | 类型 | 关键产出 | Skill |
|---|---|---|---|
| 0 前置阅读 | Proof | 8 项阅读记录 | — |
| 1 Bash wrapper | Add | `tools/check-state-machine-coverage.sh`（50 行） | code-quality-audit |
| 2 scripts README | Add / Proof | `scripts/README.md`（如无） | — |
| 3 维护入口强化 | Add / Proof | `state-machine-matrix.md` §7/§8/§9 | — |
| 4 验证 | Proof | strict mode pass + 故意失败检测 | — |

## Closure Gates

- CG1：`bash tools/check-state-machine-coverage.sh` 退出码 0
- CG2：`bash tools/check-state-machine-coverage.sh --strict` 退出码 0（当前无 finding）
- CG3：故意制造 finding 场景下 `--strict` 退出码 1（已还原）
- CG4：`docs/architecture/state-machine-matrix.md` 新增 §7/§8/§9 三章节齐全
- CG5：`scripts/README.md` 列出 M5.2 wrapper + 链接到 M5.1 工具 + 维护入口
- CG6：所有产物文件存在且非空

## 应急路线

如发现 M5.1 工具脚本（`state-machine-coverage-check.py`）有重大 bug（如扫描逻辑错误、维度对账实现缺失），需在本 plan 内直接修补 M5.1 工具并在新 commit 中标注「fix m5-1 tool」。**不算范围越界**——M5.2 守卫层的前提是 M5.1 工具正确。

## Plan Status: draft
