# 2026-07-29-1430-1-ma5-s-tier-test-coverage-audit MA5 S 级域测试覆盖深度审计

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MA5（A5.1 / A5.2 / A5.3 / A5.4）
> Related: MA2/MA4 已确认 finding（测试背书交叉验证）；plan `2026-07-27-1015-1`（M0 审计回归起点锚）
> Audit: required

## Current Baseline

- MA1-MA4（结构 / 业务 / 文档 / 代码四层审计）全部 done，`docs/audits/arm-index.md` 已登记 MA1-MA4 全部报告。
- MA5（测试层审计）6 工作项全部 `todo`；本计划覆盖前 4 项（A5.1-A5.4，S 级域测试覆盖深度）。A5.5/A5.6 由 plan `2026-07-29-1430-2` 覆盖。
- 实测 S 级四域测试文件计数（`module-*/src/test/java/**/Test*.java`，排除 `*CodeGen` / `*TestSupport*` / `TestStub*`）：
  - **finance**：64 测试文件（roadmap 记「64 测试 / 137 mutation，比 0.47」）。
  - **manufacturing**：29 测试文件（roadmap 记「30 测试 / 74 mutation，比 0.41」）。
  - **hr**：15 测试文件（roadmap 记「15 测试 / 92 mutation，比 0.16 — 全域最低」）— **本批次最高风险**。
  - **assets**：14 测试文件（roadmap 记「14 测试 / 61 mutation，比 0.23」）。
- `docs/testing/test-depth-classification.md` 按文件行数分档（深≥400 / 中 100-399 / 浅<100）：finance 46[深2/中38/浅6]、mfg 19[深4/中14/浅1]、hr 10[深3/中7/浅0]、assets 14[深3/中10/浅1] — **计数口径与 roadmap 的「测试/mutation」不同，审计须统一口径**。
- 验证基线：`mvn test` 全绿（~2890 单元测试，0 failures）；`tests/e2e/` 258 spec；`docs/testing/known-good-baselines.md` 最新绿基线 2026-07-25（全套件 490 passed / 1 failed [master-data.write.amis 测试基础设施 Non-Goal] / 3 skipped）。
- MA2/MA4 已确认的高风险行为（用作测试背书交叉验证锚点）：finance 多币种凭证折算 [P1-MA2-002/009] / 暂估冲回缺失 [P1-MA2-001] / 期间结账阻断分级 [P1-MA2-017] / mfg 物料预留子系统未实现 [P1-MA3-042] / assets 折旧引擎 48 Processor [A4.3] / hr 薪酬过账 + 模拟引擎 [A4.4]。
- 剩余差距：四域测试覆盖深度从未被系统审计；mutation/test 比是否反映真实业务路径覆盖未知；assertion 强度（状态 / 数值 / 负路径）未评估；与已确认业务 finding 的测试背书关系未建立。

## Goals

- 对 finance / manufacturing / hr / assets 四个 S 级域逐一系统审计**测试覆盖深度**，每域输出一份审计报告。
- 每域评估：①覆盖口径统一 ②关键业务路径（状态机 / 过账 / 端到端）测试缺口 ③assertion 强度（深断言 vs 仅状态/状态码浅断言）④负路径与错误处理覆盖 ⑤与 MA2-MA4 已确认 finding 的测试背书关系（哪些已确认风险零测试覆盖）。
- 注册 P0（即时通道）/ P1（目标 MR3）/ P2（watch-only）发现至 `docs/audits/arm-index.md`，与 MA1-MA4 已登记 P1 交叉去重。
- 推进 roadmap A5.1-A5.4 状态（审计产出后转 `ready`，独立 closure audit 后转 `done`）。

## Non-Goals

- 不修复任何测试缺口或新增测试（修复属 MR3 批量修复，由 R3.0 展开机制生成具体工作项行）。
- 不审计 A5.5（测试隔离性）/ A5.6（E2E 有效性）— 见 plan `2026-07-29-1430-2`。
- 不审计 A/B/C 级域测试覆盖（非 S 级，不在本批次）。
- 不评估 E2E 套件（`tests/e2e/`）业务断言强度（A5.6 范围）；本计划聚焦单元 / 集成层（`src/test`）覆盖深度。
- 不变更任何生产代码 / ORM / 契约（纯审计）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/testing/test-depth-classification.md` + 各域 `docs/design/{finance,manufacturing,human-resource,assets}/` + `docs/design/flow-overview.md`（端到端链路）。
- Skill Selection Basis: 四项工作项 roadmap 明确指定 `docs/skills/open-ended-audit-prompt.md`（测试覆盖深度是开放式、需积极搜索未知覆盖缺口的维度，非结构化检查清单）。加载后须先读 `docs/skills/README.md §项目定制化层`，注入本仓保护区域与验证命令。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- 审计只读仓库，需可运行 `mvn test`（仅作回归基线确认，不改代码）。

## Execution Plan

> 共享审计维度（每域 phase 套用）：(1) 计数口径统一（文件数 / 方法数 / mutation 数，与 roadmap + test-depth-classification 对账）；(2) 关键业务路径覆盖矩阵（状态机全路径 / 过账三段 / 端到端链路是否各有测试）；(3) assertion 强度分档（深=数值/GL/AR-AP 全断言 / 中=状态翻转 / 浅=仅 200 或存在性）；(4) 负路径（非法迁移 / 守卫失败 / 错误码）覆盖；(5) 与 MA2-MA4 已确认 finding 的测试背书关系。

### Phase 1 - finance 测试覆盖深度审计（A5.1）

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/**`；报告 `docs/audits/2026-07-29-1430-arm-ma5-finance-test-coverage.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0 锚点（plan 2026-07-27-1015-1）

- [x] 统一 finance 计数口径（文件 / 方法 / mutation），与 roadmap「64/137/0.47」+ test-depth-classification「46」对账并记录差异原因
      - Skill: `open-ended-audit-prompt.md`
- [x] 建 finance 关键路径覆盖矩阵：过账与凭证链路 / 期间结账 / 预算 / AR-AP 核销 / 成本 / GL 映射，逐链路标注「有深测 / 仅中浅测 / 零测试」
- [x] 评估 finance assertion 强度分档分布；标记仅断言状态码或返回非空而不断言业务数值 / GL 平衡 / AR-AP 余额的「伪覆盖」测试
- [x] 交叉验证 finance MA2/MA4 已确认 finding 的测试背书：多币种折算 [P1-MA2-002/009] / 暂估冲回 [P1-MA2-001] / 期间结账阻断 [P1-MA2-017] / FX 损益结转 [P0-MA2-016 已修] 是否有回归测试守住
- [x] 产出 finance 审计报告，分类 P0/P1/P2，更新 `docs/audits/arm-index.md`（去重 MA1-MA4）
  - Skill: `open-ended-audit-prompt.md`

Exit Criteria:

- [x] finance 覆盖深度报告产出，含计数口径对账表 + 关键路径覆盖矩阵 + assertion 强度分档 + 与 MA2/MA4 finding 背书关系表
- [x] finance P0/P1/P2 已登记 arm-index.md，且与既有 P1 交叉去重无重复

### Phase 2 - manufacturing 测试覆盖深度审计（A5.2）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/test/java/**`；报告 `docs/audits/2026-07-29-1430-arm-ma5-mfg-test-coverage.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: Phase 1（口径方法已建立，复用）

- [x] 统一 manufacturing 计数口径（roadmap「30/74/0.41」实测 29 文件，对账并记录差异）
- [x] 建 mfg 关键路径覆盖矩阵：工单与报工 / BOM 与工艺路线 / MRP-DRP 引擎 / 质量集成 NCR / 委外 / 批次基因追溯 / 质检门控，逐链路标注覆盖档
- [x] 评估 mfg assertion 强度；标记折旧/差异计算/MRP 建议等数值密集逻辑的浅断言风险
- [x] 交叉验证 mfg MA2/MA4 finding 背书：物料预留子系统未实现 [P1-MA3-042] / 差异公式 [P1-MA3-043] 是否有测试触及
- [x] 产出 mfg 审计报告，分类 P0/P1/P2，更新 arm-index.md（去重）

Exit Criteria:

- [x] mfg 覆盖深度报告产出，含口径对账 + 关键路径矩阵 + assertion 分档 + finding 背书表
- [x] mfg P0/P1/P2 已登记 arm-index.md 且去重

### Phase 3 - hr 测试覆盖深度审计（A5.3）— 最高风险（比 0.16 全域最低）

Status: completed
Targets: `module-hr/erp-hr-service/src/test/java/**`；报告 `docs/audits/2026-07-29-1430-arm-ma5-hr-test-coverage.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: Phase 1

- [x] 统一 hr 计数口径（roadmap「15/92/0.16」实测 15 文件一致），重点量化「92 mutation 仅 15 测试」的覆盖缺口规模
- [x] 建 hr 关键路径覆盖矩阵：薪酬过账 + 模拟引擎（A4.4 标全域第二高 mutation 密度）/ 考勤 / 排班 / 工时 / 请假 / 合同 / 招聘 / 考核，逐链路标注覆盖档
- [x] 评估 hr assertion 强度；重点：薪酬过账是否断言 GL 凭证 / 模拟引擎差值计算是否断言数值（而非仅返回非空）
- [x] 交叉验证 hr MA4 finding 背书 [A4.4]
- [x] 产出 hr 审计报告，分类 P0/P1/P2，更新 arm-index.md（去重）；**明确裁决 0.16 比是「测试缺口」还是「mutation 统计口径偏差」**

Exit Criteria:

- [x] hr 覆盖深度报告产出，含 0.16 比根因裁决（缺口 vs 口径）
- [x] hr P0/P1/P2 已登记 arm-index.md 且去重

### Phase 4 - assets 测试覆盖深度审计（A5.4）

Status: completed
Targets: `module-assets/erp-ast-service/src/test/java/**`；报告 `docs/audits/2026-07-29-1430-arm-ma5-assets-test-coverage.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: Phase 1

- [x] 统一 assets 计数口径（roadmap「14/61/0.23」实测 14 文件一致）
- [x] 建 assets 关键路径覆盖矩阵：折旧引擎 48 Processor [A4.3] / 资产卡片 / 7 业务单据（处置/转移/分割/合并/重估/资本化/价值调整）/ CIP / 盘点，逐链路标注覆盖档
- [x] 评估 assets assertion 强度；重点：折旧计算累计值/残值/月折旧额是否数值断言；处置/转移过账是否断言 GL
- [x] 交叉验证 assets MA4 finding 背书 [A4.3 折旧引擎 + A2.10 状态机]
- [x] 产出 assets 审计报告，分类 P0/P1/P2，更新 arm-index.md（去重）

Exit Criteria:

- [x] assets 覆盖深度报告产出，含折旧引擎 48 Processor 覆盖矩阵
- [x] assets P0/P1/P2 已登记 arm-index.md 且去重

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (task `ses_053d6bba1ffelvZUcsSNiOhPi5`) because 全部基线声明经实仓验证准确（finance 64 / mfg 29 / hr 15 / assets 14 测试文件、A5.1-A5.4 todo、MA1-MA4 done、skill 存在、finding ID 全部存在于 arm-index）；A5.1-A5.4 合并为单 plan 符合 rule 14（同里程碑/同 skill/同交付形态）；item typing `Proof|Decision` 正确；exit criteria 未泄漏全仓验证；anti-slack 零命中；计数口径差异（mfg 30→29 / 文件数 vs mutation）诚实暴露而非掩盖。无阻塞问题。

## Closure Gates

> 本 plan 为纯审计，不改代码。`mvn test` 仅作回归基线确认（确认审计只读期间未引入回归），非交付物。

- [x] A5.1-A5.4 四份覆盖深度报告全部产出
- [x] arm-index.md 已登记本批次全部 P0/P1/P2，且与 MA1-MA4 既有 P1 交叉去重无重复
- [x] roadmap A5.1-A5.4 状态推进至 `ready`（独立 closure audit 后转 `done`）
- [x] 已运行 `mvn clean install -DskipTests`（回归基线确认，154 模块绿）+ `mvn test`（回归基线确认，0 failures）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态 / 阶段 / 门控 / 日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A/B/C 级域测试覆盖深度

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: MA5 仅要求 S 级域（A5.1-A5.4）；A/B/C 级域测试覆盖不在本批次，roadmap 无对应工作项。
- Successor Required: `no`（若 MA5 完成后需扩展，由新 plan 触发）

## Closure

Status Note: 四域覆盖深度报告全部产出（finance/mfg/hr/assets）+ arm-index.md 已登记 11 项 P1（5 独立 + 6 归并）+ 4 项 P2 + roadmap A5.1-A5.4 推进至 ready。回归基线确认：`mvn clean install -DskipTests` BUILD SUCCESS（154 模块绿）+ `mvn test` BUILD SUCCESS（0 failures / 0 errors）。0.16 全域最低比根因裁决：缺口为主 + 口径为辅。test-depth-classification.md 计数口径系统性过时（finance 46→64 / mfg 19→29 / hr 10→15 / assets 深度分类错浅1→浅0）登记 P1-MA5-001/004/007/010 目标 MR3 文档刷新。独立结束审计待 Mission Driver CLOSURE_VERIFY 循环执行。

Closure Audit Evidence:

- Auditor / Agent: 执行代理（本会话）产出四域报告 + arm-index 登记 + 回归基线确认；独立结束审计由 Mission Driver CLOSURE_VERIFY 循环指派独立子代理（新会话）执行
- Evidence: 四份报告 `docs/audits/2026-07-29-1430-arm-ma5-{finance,mfg,hr,assets}-test-coverage.md`；arm-index.md 报告清单 4 行 + MA5 汇总段 + P1 详细表 P1-MA5-001~011；roadmap A5.1-A5.4 状态 todo→ready；`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` BUILD SUCCESS（0 failures/0 errors）
- **Independent Closure Audit (R3.5 Round 3 batch, 2026-07-31)** — Auditor: independent closure audit subagent (fresh session, cold-context). Verdict: **PASS**. Five-point consistency: (1) Plan Status `completed` ↔ Phase 1-4 all `completed` — consistent; (2) Phase Status ↔ Exit Criteria — all phase exit criteria [x] match live reports; (3) Exit Criteria ↔ Closure Gates — 9 gates all satisfiable from file evidence; (4) Closure Gates ↔ 日志 — `docs/logs/2026/07-29.md` line 104-111 记录四域报告产出 + arm-index 登记 + roadmap 推进，与门控对账一致；(5) Closure 段 ↔ 全文状态 — 一致。Anti-hollow: PASS — 四份报告均为实质内容（finance 24.7KB / mfg 17.2KB / hr 18.4KB / assets 15.6KB，含计数对账表 + 路径覆盖矩阵 + assertion 分档 + finding 背书表，非空壳）；P1-MA5-001~011 含逐项根因裁决与归并标注，非空登记。Deferred honesty: PASS — line 163 明确记「独立结束审计待 CLOSURE_VERIFY 循环执行」未伪装已审计；Deferred But Adjudicated 段诚实分类 A/B/C 级域为 out-of-scope（successor=no）；roadmap line 320 诚实标注「独立 closure audit 仍 pending，保持 ready 不静默降级」。Live-repo spot-check: 4 reports exist (`ls docs/audits/2026-07-29-1430-arm-ma5-{finance,mfg,hr,assets}-test-coverage.md` ✅)；arm-index.md line 61-64 报告清单 4 行 + line 105 MA5 汇总段 + line 507-517 P1-MA5-001~011 全部注册（5 独立[001/004/006/007/010] + 6 归并[002/003/005/008/009/011] 标注 MA4/MA2/MA3 测试层投影，交叉去重无重复）；roadmap line 107-110 A5.1-A5.4 = `ready` ✅。Baseline-green note: plan 声明 `mvn clean install -DskipTests`（154 模块绿）+ `mvn test`（0 failures）— 本审计为 audit-only 零代码变更计划，基线确认为只读回归保护（非交付物），采纳执行者自报告并与 audit-only 性质一致。**Roadmap impact**: A5.1-A5.4 PASS → eligible `ready`→`done` (Phase 3 of R3.5)。 (Audit dispatch ref: docs/plans/2026-07-31-1439-1-r3-5-closure-audit-round3-protected-area.md Phase 2; appended by R3.5 Round 3 backfill.)

Follow-up:

- 测试缺口修复不在此处；由 R3.0 展开机制将本批次 P1 转化为 MR3 具体修复工作项行。
