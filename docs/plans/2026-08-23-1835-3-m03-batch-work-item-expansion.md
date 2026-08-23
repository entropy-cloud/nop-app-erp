# 2026-08-23-1835-3-m03-batch-work-item-expansion 依设计文档追加 M1-Bn 分批工作项（M0.3）

> Plan Status: active
> Mission: integration-test
> Work Item: M0.3
> Last Reviewed: 2026-08-23
> Source: `docs/backlog/integration-test-roadmap.md`（M0.3 工作项规格，v3）
> Related: `2026-08-23-1835-1-m01-integration-test-case-design`（前置）、`2026-08-23-1835-2-m02-infra-pilot-mechanism-adjudication`（前置）
> Audit: required

## Current Baseline

- **前置依赖**：M0.1 设计文档（`docs/design/integration-testing.md`）定稿 + M0.2 基建试点与机制裁决完成——本计划执行时二者均须 `done`（roadmap Deps：M0.1 + M0.2）。
- M1-Bn 里程碑表当前**零工作项行**（仅注记，对齐「不预注册」纪律）——本计划负责追加 B1-Bn 实体工作项。
- 设计文档将含 15-25 用例全量规格（六要素）+ 覆盖矩阵 + 主导域批次归属（M0.1 产出），本计划据此分批。
- roadmap 规则 3：工作项扩展仅限 M0.3 向 M1-Bn 追加行；追加行初始 `todo`，基于设计文档，**不得自行发明范围**。

## Goals

- 依设计文档向 M1-Bn 里程碑表追加 B1-Bn 分批工作项（按主导域分组 8-12 批，每批 1 工作项 2-3 用例，优先 18-22 用例校准）。
- 每行含：用例编号集、涉及域、依赖、Skill（`nop-testing`）；追加行初始 `todo`。
- 与设计文档用例清单逐项对应（无发明、无遗漏）。

## Non-Goals

- 不实现任何用例（B1-Bn 分批计划由后续 mission 轮次起草）。
- 不修改 M0.1 设计文档已定稿的用例规格。
- 不改 seed、不改生产代码、不接 CI。

## Task Route

- Type: `implementation-only change`（roadmap 行追加，纯文档编辑，零代码/契约/模型变更）
- Owner Docs: `docs/backlog/integration-test-roadmap.md`、`docs/design/integration-testing.md`（M0.1 产物，本计划执行时存在）
- Skill Selection Basis: 纯行级文档编辑 + 分批编排（依赖 M0.1 设计文档的主导域批次归属），无测试代码/模型/页面编写面——roadmap M0.3 行 Skill 列 = `none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯文档计划）。

## Execution Plan

### Phase 1 — 追加 B1-Bn 分批工作项（混合类型：1/2 Add + 1/2 Proof，低于 80% 统一类型阈值，逐项标注）

Status: planned
Targets: `docs/backlog/integration-test-roadmap.md`（M1-Bn 里程碑表）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: M0.1 + M0.2 done（设计文档 + 基建就绪）

- [ ] Add：依设计文档用例清单按主导域分组（8-12 批，每批 1 工作项 2-3 用例，优先 18-22 用例校准），向 M1-Bn 表追加工作项行——每行含用例编号集/涉及域/依赖/Skill（`nop-testing`），初始 `todo`。
  - Skill: none
- [ ] Proof：分批核对——设计文档全部用例均被某批引用且仅一次；批次总数 8-12；每批 2-3 用例；覆盖矩阵每域 ≥2 次口径在分批后仍成立。
  - Skill: none

Exit Criteria:

- [ ] M1-Bn 表含 B1-Bn 行（8-12 批，每批 2-3 用例，初始 `todo`），用例引用与设计文档一一对应
- [ ] 无发明范围（用例编号集 = 设计文档用例全集）；无遗漏（每用例恰属一批）
- [ ] 分批后覆盖矩阵口径仍成立（设计文档覆盖矩阵每域 ≥2 次经分批核对后无回落；失败模式：分批后任域出现次数 < 设计文档覆盖矩阵声明 = 失败）

### Phase 2 — 校验与收尾（Proof-heavy）

Status: planned
Targets: `docs/backlog/integration-test-roadmap.md`（M0.3 → done）、`docs/logs/2026/08-23.md`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: Phase 1

- [ ] Proof：独立子代理审查分批合理性（批次粒度/依赖链/与设计文档一致性）。
  - Skill: none
- [ ] Fix：审查发现修订。
  - Skill: none
- [ ] Proof：roadmap M0.3 状态 `todo` → `done` + `docs/logs/2026/08-23.md` 日志条目（按日志书写指南）。

Exit Criteria:

- [ ] 独立审查收敛记录在案（无未决 BLOCKER/MAJOR）
- [ ] roadmap M0.3 = done + 日志条目存在

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fd1cb0b4fffeirX1P1RtpAPB1W，独立 general 子代理新会话) — 0 BLOCKER / 1 MAJOR / 2 MINOR。事实核验全过（M1-Bn 零工作项行 / 设计文档不存在且依赖声明正确 / Deps = M0.1+M0.2 / Skill=none 与 roadmap 一致 / 规则 3 引用准确）；计划值得性裁定为完整计划（roadmap 规则 2 每工作项需独立草案审查 + 结束审计，且分批含裁决非机械编辑）。MAJOR-1 日志路径 `docs/logs/2026/2026-08-23.md` 错误 → 正确 `docs/logs/2026/08-23.md`（实仓已存在须追加非创建）。MINOR-2 Phase 1 退出标准缺覆盖矩阵谓词（仅在 Proof 项内）；MINOR-3 失败模式未显式命名。修订全部落地。
- Independent draft review iteration 2: accept (ses_fd1c7ac01ffeRzEsM132uYq8uI，独立 general 子代理新会话) — 0 BLOCKER / 0 MAJOR / 1 MINOR。迭代 1 三项发现全部核验 FIXED（日志路径 rg 零命中 + 双侧命令验证 / 覆盖矩阵谓词入退出标准含失败模式 / 量词谓词可推得失败模式）；复扫分批规格与 roadmap M0.3 逐项吻合、模板结构完整、docs-only 门控删除理由充分。唯一 MINOR：Phase 1 头「Add-heavy 2/2」计数不实（实际 1 Add + 1 Proof，50% 低于统一类型阈值）。**共识达成，计划可执行。**（MINOR 已在本轮修订落地）

## Closure Gates

> 本计划为 docs-only（零生产代码/契约/模型/测试代码变更），按计划指南模板规范删除 typecheck/build/lint/test 验证命令门控，以 Phase 1 的分批核对 + Phase 2 审查收敛记录替代。

- [ ] 范围内行为完成（B1-Bn 分批工作项追加完整）
- [ ] 相关文档对齐（roadmap M1-Bn 表 ↔ 设计文档用例清单 ↔ 覆盖矩阵一致）
- [ ] 已运行验证（docs-only：用例编号集双向核对 + 批次计数核对）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

- （执行后按需填写）

## Closure

Status Note: （执行后填写）

Closure Audit Evidence:

- Auditor / Agent: （独立子代理新会话）
- Evidence: （任务 id / 核对记录）

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得出现于此）