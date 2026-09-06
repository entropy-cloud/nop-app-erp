---
status: active
mission: ai-check-r3
work-item: M0.5+M0.6
group: "2026-09-06-1451"
verify: [test]
---

# 2026-09-06-1451-3 M0.5+M0.6 五维符合性审计检查清单冻结与 M0 收官

## Current Baseline

（实仓核验 2026-09-06）

- M0.5 产物 `docs/audits/check/<本轮目录>/m0-5-audit-checklists.md` 不存在（执行目录本身由 plan `2026-09-06-1451-2` Phase 2 创建）。
- 本计划依赖 M0.1~M0.4 产物全部就绪：`docs/architecture/i18n-compliance.md`（plan 1451-1）、`tools/check-hardcoded-cjk.mjs` + `docs/audits/cjk-baseline.md` + 执行目录 + `m0-4-page-yaml-source-map.md`（plan 1451-2）。
- 五维锚点全部实仓在位：DIM-B——`docs/skills/nop-platform-conformance-audit-prompt.md`（15 维度）+ `docs/audits/nop-compliance-checker.sh` R1-R12 数值门控（R2c=1542 现值，`docs/audits/compliance-baseline.md`）；DIM-F——`docs/architecture/view-and-page-strategy.md` + 各 pattern doc + `docs/testing/e2e-runbook.md` §编写规范（E2E 强制 flux 渲染）；DIM-S——`docs/architecture/seed-data.md`（快照重录双面义务 / `_init-data` 同步义务 / `TestErpSeedDataIntegrity` 门禁，2026-09-04 行 363 实体全量 seed 基线）；DIM-T——`docs/architecture/testing-strategy.md`（覆盖要求 / SnapshotTest 纪律 / test-depth-classification）；DIM-I——`docs/architecture/i18n-compliance.md`（M0.1 产物）+ `tools/check-hardcoded-cjk.mjs`（M0.2 产物）。
- 跨轮查重源在位：`docs/audits/check/ai-check-index.md`（r1 已登记 21+ 报告行 + Mission 基线快照）+ r2 历史目录 `2026-08-28-2049-ai-check-r2`（只读）；finding ID 冲突按 `code-history-deferred-triangulation-audit-prompt.md §3.1` 加 `-r3` 后缀，历史 ID 永不覆写。
- 核对单元 = 19 业务域（11 核心域 + 5 第一批扩展域 + 3 第二批扩展域）+ common-service + app-erp-all 横切，共 21 格；S 级域（finance/manufacturing/assets/hr）按功能模块拆分，C 级域合并（与 M1.1~M1.16 切片定义一一对应）。
- MI 先行时序约束：M1.x 审计的 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding——清单须内嵌此口径。
- 与 r1/r2 边界：本轮不接管 r1 行为 finding 修复与 r2 三路交叉审计的工作项（roadmap 横切关注点 5）。

## Goals

- 冻结五维 × 21 核对单元检查清单（每维 owner doc 锚点 + 判定标准 + grep/脚本核查程序式 + 跨轮查重列），落执行目录 `m0-5-audit-checklists.md`，作为 M1.1~M1.16 全部审计切片的强制核对矩阵（禁止抽样、禁止跳维；M1.17 收官以该矩阵完整性校验为准）。
- M0 收官：`git status` 零生产代码改动核证 + 五项 M0 产物齐全核证 + 独立子代理 closure audit 通过。

## Non-Goals

- 不执行任何 M1.x 审计切片（清单只冻结不跑；M1.x 须待 MI.9 完成，roadmap 依赖图）。
- 不产生/修复任何 finding（两阶段时序硬约束：M1.x 只产 finding 不改代码，本计划连 finding 都不产）。
- 不修改生产代码、ORM、配置、页面文件。
- 不回写 roadmap 状态为 done 以外的重排（AI 不自行重排优先级或发明工作项，roadmap 规则 1）。

## Phase 1 — M0.5 五维检查清单冻结

> Skill: `nop-platform-conformance-audit-prompt`（维度来源，roadmap M0.5 指定）
> Item Types: `Add | Decision | Proof`
> Prereqs: plan `2026-09-06-1451-2`（执行目录 + M0.1/M0.2 产物）
> Status: planned
> Targets: `docs/audits/check/<执行目录>/m0-5-audit-checklists.md`

- [ ] Add: 产出执行目录 `m0-5-audit-checklists.md`——五维逐维成节：DIM-B 锚 `nop-platform-conformance-audit-prompt` 15 维度 + compliance checker R1-R12；DIM-F 锚 `view-and-page-strategy.md` + 各 pattern doc + `e2e-runbook.md` §编写规范；DIM-S 锚 `seed-data.md`（快照重录双面义务 / `_init-data` 同步义务 / `TestErpSeedDataIntegrity` 门禁）；DIM-T 锚 `testing-strategy.md`（覆盖要求 / SnapshotTest 纪律 / test-depth-classification）；DIM-I 锚 `i18n-compliance.md` + `check-hardcoded-cjk.mjs`（内嵌「MI.9 后仅验证零回归与白名单合规」口径）。每维列：权威 owner doc 锚点路径 + 判定标准 + grep/脚本核查程序式（可机械执行）。
- [ ] Add: 21 核对单元逐一列格（19 业务域 + common-service + app-erp-all 横切），每格含五维子行与**跨轮查重列**——每候选 finding 必查 `docs/audits/check/ai-check-index.md` 与 r1/r2 既有 finding，三态裁决：同型已 fixed 复用范式 / 同型 open 归并原 ID / 确属新发才立 `-r3` 新 ID。
- [ ] Decision: S 级域切片粒度登记——finance 4 切片（过账与凭证 / AR-AP 核销与坏账 / 预算与成本 / 期间结账与银行对账+跨域凭证链路）、manufacturing 3 切片（工单与报工 / BOM-MRP-CRP / 委外-批次追溯-差异）、assets 2 切片、hr 2 切片，与 M1.1~M1.11 工作项定义对齐；替代方案（S 级域整域单切片）因单会话不可完成被否决（roadmap 规则 2 工作项粒度），残留风险：切片边界跨功能模块的共享代码（如 posting processor 族）归属需在清单中显式指定唯一归属切片，防重复审计。
- [ ] Proof: 清单覆盖五维 × 21 格全矩阵无空格；清单内引用的锚点文件路径逐一实仓可解析；M1.1~M1.16 每个工作项能在清单中找到唯一对应的格集合。

Exit Criteria:

- [ ] 执行目录 `m0-5-audit-checklists.md` 落盘：五维 × 21 格全矩阵无空格，每维含权威 owner doc 锚点路径 + 判定标准 + 可机械执行的 grep/脚本核查程式 + 跨轮查重列（失败模式：任一格空缺、任一锚点路径实仓不可解析、核查程式不可机械执行）。
- [ ] M1.1~M1.16 每个工作项能在清单中找到唯一对应的格集合（失败模式：工作项映射不到格，或映射到多格且无唯一归属裁决）。
- [ ] S 级域 11 切片粒度登记完成（finance 4 / manufacturing 3 / assets 2 / hr 2，与 M1.1~M1.11 工作项定义对齐），跨功能模块共享代码（如 posting processor 族）在清单中显式指定唯一归属切片。

## Phase 2 — M0.6 M0 收官

> Skill: `closure-audit-prompt`（独立子代理审计，roadmap M0.6 指定）
> Item Types: `Proof`
> Prereqs: Phase 1（五项 M0 产物最后一项就绪）
> Status: planned
> Targets: `git status`、`docs/testing/known-good-baselines.md`、roadmap M0.1~M0.6 状态、`mvn test`

- [ ] Proof: `git status` 核证零 `module-*`/`app-erp-all` 生产代码改动（M0 仅 docs + tools 脚本 + 基线登记；任何生产代码改动即收官失败并升级裁决）。
- [ ] Proof: 五项 M0 产物齐全核证——(1) `docs/architecture/i18n-compliance.md`；(2) `tools/check-hardcoded-cjk.mjs` + `docs/audits/cjk-baseline.md`；(3) 本轮唯一执行目录（含基线快照 + `m0-4-page-yaml-source-map.md`）；(4) `m0-5-audit-checklists.md`；(5) `docs/testing/known-good-baselines.md` `ai-check-r3-m0` 行。
- [ ] Proof: 独立子代理（新会话，不重用执行者上下文）按 `closure-audit-prompt` 执行 M0 closure audit 直到通过；执行者不自我审计；通过后按 roadmap 规则 3 由执行流程回写 M0.1~M0.6 状态 `done`（`todo/ready → done` 唯一通道 = 结束审计通过）。
- [ ] Proof: `mvn test` 全 reactor 零新增失败（M0 收官回归核证；对照 known-good-baselines 2026-09-04 行预存失败清单：hr + drp 两项，零新增即通过）。

Exit Criteria:

- [ ] `git status` 核证通过：零 `module-*`/`app-erp-all` 生产代码改动（失败模式：出现任何生产代码改动 = 收官失败并升级裁决）。
- [ ] 五项 M0 产物逐一实仓存在且与登记一致：(1) `docs/architecture/i18n-compliance.md`；(2) `tools/check-hardcoded-cjk.mjs` + `docs/audits/cjk-baseline.md`；(3) 本轮唯一执行目录（含基线快照 + `m0-4-page-yaml-source-map.md`）；(4) `m0-5-audit-checklists.md`；(5) `docs/testing/known-good-baselines.md` `ai-check-r3-m0` 行。
- [ ] `mvn test` 全 reactor 零新增失败（对照 2026-09-04 行 hr + drp 两项预存）。
- [ ] 独立子代理 closure audit 通过且证据落盘（见 Closure Gates；通过后由执行流程按 roadmap 规则 3 回写 M0.1~M0.6 状态 `done`）。

## Closure Gates

> 仅在所有执行项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划零生产代码改动（`git status` 核证项覆盖），无需另行 build/checker 门控；完整仓库验证 = `mvn test` 全 reactor 对照预存失败清单（frontmatter `verify: [test]`）。

- [ ] 范围内行为完成：五维 × 21 格检查清单冻结（含跨轮查重列与 S 级切片粒度登记）+ M0 收官四项核证全部落地
- [ ] 相关文档对齐：执行目录产物、`docs/testing/known-good-baselines.md`、roadmap M0.5/M0.6 状态回写与实仓一致
- [ ] 已运行验证：`mvn test` 全 reactor 零新增失败（对照 2026-09-04 行 hr + drp 两项预存）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中（执行目录产物 + Closure 节）

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-06-1451-3-m0-audit-checklists-closure-1-bba97576 to opencode-reviewer-2026-09-06-151218
- 2026-09-06：iteration 1，共识 accept #review-2026-09-05-123532-mission-driver-2026-09-06-1451-3-m0-audit-checklists-closure-1-bba97576

## Verification

- 验证命令与通过口径：`mvn test` 全 reactor 对照 `docs/testing/known-good-baselines.md` 2026-09-04 行预存失败清单（hr + drp 两项）零新增失败（frontmatter `verify: [test]`）。
- 生产代码零改动由 Phase 2 `git status` 核证项覆盖；本计划产物均为 docs + 执行目录检查清单，无 build/checker 新门控。

## Closure

Status Note: <闭包时填写：为什么计划可以关闭>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理（新会话，无执行者上下文）>
- Evidence: <task id / 执行目录产物 / 日志链接>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
