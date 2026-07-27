# 2026-07-27-1430-3-audit-remediation-ma1-architecture-governance-review MA1 架构治理复审 — daoFor Type 4 残留 / 字典真相 / 共享内核守卫 / CI guard（A1.14）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A1.14 架构治理复审（daoFor Type 4 残留 / 字典真相 / 共享内核守卫 / CI guard）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA1（工作项 A1.14）
> Related: `docs/audits/2026-07-23-0000-architecture-governance-review.md`（2026-07-23 首次架构治理审查，9 finding F1–F9 全部已闭包，本次为复审）；`2026-07-27-1227-1-audit-remediation-ma1-cross-module-dag-audit.md`（A1.10 跨模块 DAG 审计 done）；`docs/audits/compliance-baseline.md`（CI guard 基线锚点）；`docs/architecture/{module-boundaries,data-dependency-matrix}.md`
> Audit: required

## Current Baseline

A1.14 是 MA1 架构治理维度的复审。首次架构治理审查（`docs/audits/2026-07-23-0000-architecture-governance-review.md`，2026-07-23）覆盖全仓 19 域 + `app-erp-all` + `docs/`，经 2 路独立子代理冷重播挑战后产出 v2，识别 9 个 finding（F1–F9），**全部已于 2026-07-23/24 闭包**（详见该报告 §闭包前必须项 #1–#12）：

- F1（daoFor 跨域真违规子集）：Type 1 `getEntityById(FK)` chained + variable-split 全域清零；Type 4（~10-30 跨域写/读）阻塞 successor，裁决=分支 (b) 需平台 lazy/SPI 解耦（`docs/analysis/governed-path-cost-evaluation.md`）。
- F2（字典真相碎裂）：D1 全域推广完成（9 域 `Erp*DocStatus` + doc-status 6 域共享 dict 统一）；drp 命名例外登记。
- F3（DAG 边表格）：3 边补登（drp→inv / mfg→inv / mnt→ast）。
- F4（隐性共享内核）：finance/master-data 3 类型显式登记为共享内核 + R12a/b/c 守卫。
- F5（notify owner doc）：`docs/design/notify/README.md` 创建。
- F6（mfg 依赖 qa 生成常量）：`ErpQaInspectionType` 迁移。
- F7（drp 命名前缀）：4 实体命名例外登记。
- F8（compliance checker dead armor）：接入 CI（`.github/workflows/compliance.yml`）+ 基线日志。
- F9（19 web 冒烟测试 @Disabled）：`@Tag("full-app")` + CI `app-erp-all` 阶段强制运行。

scope matrix §2.1 "架构治理（daoFor/字典/共享内核/guard）" 行反映首审终态（实测 `:94`）：**7 列有首审残留标记 `⚠️`**（finance `⚠️F1residual` / mfg `⚠️F6✅` / b2b `⚠️F1half` / inv `⚠️` / md `⚠️F4✅` / drp `⚠️F7✅` / notify `⚠️F5✅`），**12 列仍 `❓`**（hr/assets/pur/sal/qa/crm/prj/cs/ct/mnt/aps/log — 首审 F1–F9 的 owner doc 与 plan 证据虽全域引用，但矩阵单元级别仅有 7 域被显式标记残留）。

**自 2026-07-23 以来的重大变更（复审必要性）**：仓库经历了密集变更（2026-07-24→27 数十个 plan 落地：GL mapping、intercompany、commitment accounting、MRP/DRP 仿真、FX notes、browser E2E、frontend UI 闭合、batch migration、posting reversal 闭合等）。这些变更可能引入新的架构治理漂移（新增跨域写、新 daoFor 站点、共享内核扩张、字典新增）。A1.14 复审核验：(1) F1–F9 残留未回退；(2) 自首审以来的新变更未引入新治理缺口；(3) CI guard 基线相对 M0.3 锚点无上漂。

剩余差距：架构治理维度全域 19 列中 **12 列仍 `❓`**（首审矩阵单元级仅覆盖 7 域）；daoFor Type 4 残留（~10-30 处）仍阻塞 successor 需复核现状；CI guard 基线漂移需对照 M0.3 锚点核验。本计划完成后 MA1 架构治理维度全域有结论（残留确认 + 新漂移识别 + 12 `❓` 列补全），MA1 里程碑（A1.1–A1.14）全部 done。

## Goals

- 复审 F1–F9 残留状态：逐项核验首审闭包结论在实仓是否仍成立（重点 F1 daoFor Type 4 残留计数 / F2 D1 字典真相 / F4 R12 共享内核守卫基线 / F8/F9 CI guard 激活）。
- 新漂移扫描：自 2026-07-23 以来新增的跨域写、新 daoFor 站点、共享内核类型扩张、字典碎裂复发。对照 compliance checker R2/R3/R11/R12 基线 vs M0.3 锚点。
- 补全 scope matrix §2.1 架构治理行剩余 `❓` 列（首审未覆盖域的快速机械核查——daoFor/字典/共享内核/guard 四子维度）。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A1.14 推进至 `done`（经独立 closure audit）。
- 产出复审报告，更新首审报告的"解决状态"注记（追加复审结论 + 日期）。

> **与 A1.13 的关系**：本计划（架构治理维度）与 A1.13（平台合规维度）正交——两者覆盖重叠的域（crm/qa/prj/cs/ct/mnt/aps/log 等），但产出独立的 scope matrix 单元格（§2.1 不同行）+ 独立 finding 通道。A1.14 不依赖 A1.13（Phase 1 Prereqs 仅列 M0.3 + A1.10 + A1.11/A1.12），可独立执行；文档顺序置于 A1.13 之后仅为命名稳定。

## Non-Goals

- **不**重做首审（2026-07-23）已充分覆盖的 Design Review Matrix 8 槽位深度语义审查 — 本计划是**复审**（残留核验 + 新漂移扫描），非首审重跑。
- **不**修复 daoFor Type 4 残留（~10-30 处）— 首审已裁决分支 (b) 需平台 lazy/SPI 解耦（超本项目范围，`docs/analysis/governed-path-cost-evaluation.md`）。本计划仅核验其计数/现状未恶化。
- **不**审计 MA1 ORM / 跨模块 DAG / 平台合规维度（A1.1–A1.13 已覆盖）— 本计划聚焦**架构治理**四子维度（daoFor Type 4 / 字典真相 / 共享内核 / CI guard）。
- **不**审计 MA2–MA7 维度。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**重开 governed path 平台解耦裁决（F1 闭包项 #1）— 已裁决 deferred（超本项目范围）。
- **不**手改生成物。任何源变更（P0 即时修复）须改保留层文件 + `mvn clean install -DskipTests`。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-07-23-0000-architecture-governance-review.md`（首审报告，复审对照基线）；`docs/architecture/{module-boundaries,data-dependency-matrix,system-baseline}.md`；`docs/audits/compliance-baseline.md`（CI guard 锚点）；`docs/analysis/governed-path-cost-evaluation.md`（Type 4 裁决）；`docs/architecture/posting-exemptions.md`（跨域写豁免登记）
- Skill Selection Basis: 参考首审方法（roadmap A1.14 Skill 列 = "参考 arch-gov-review 方法"，非单一 skill 文件）。首审方法 = `architecture_review` + `rot_audit` 双路由 + Design Review Matrix + 9 Rot Indicators（依据 `architecture-governance-prompt.md`）。本复审聚焦残留核验 + 新漂移扫描，复用首审的 grep/核查方法。
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及源码，则该修复需 `mvn clean install -DskipTests`。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。
- **保护区域门控**：本复审主要是核验（不改代码）。若发现 P0 触及 master-data 主数据写 / finance 凭证 / data deletion 等保护区域，须有 owner doc 描述预期行为 + 人工/任务驱动授权 + 该修复子切片独立审计。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认。本复审需运行 `nop-compliance-checker.sh` 得到 R2/R3/R11/R12 当前基线对照 M0.3 锚点。

## Execution Plan

### Phase 1 - F1–F9 残留复审 + 新漂移扫描（自 2026-07-23 以来）

Status: planned
Targets: `docs/audits/2026-07-23-0000-architecture-governance-review.md`（首审，对照）；全 19 `module-*/` + `app-erp-all`；`docs/audits/compliance-baseline.md`；`.github/workflows/compliance.yml`；`docs/architecture/posting-exemptions.md`
Skill: 参考首审 arch-gov-review 方法

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线 + compliance 锚点 HEAD=0e963531d）；A1.10 跨模块 DAG 审计 done（DAG 维度残留已核验）；A1.11/A1.12 平台合规 done（平台合规维度已核验核心域）

- [ ] F1 残留复审：核验 daoFor Type 1 残留（`findAllByQuery` watch-only ~113 处）现状未恶化；daoFor Type 4（~10-30 跨域写/读）计数复核（grep `daoFor(Erp*` 跨域 + 交叉 `posting-exemptions.md` 已登记豁免）。产出 Type 4 当前清单 + 阻塞 successor 状态。
      - Skill: 参考首审 arch-gov-review 方法
- [ ] F2 残留复审：核验 D1 字典真相（9 域 `Erp*DocStatus` 覆盖 + doc-status 6 域共享 dict + cs 特化裁决）现状未回退；扫描自首审以来是否有新 per-domain dict 碎裂复发（`find module-* -name '*status.dict.yaml'`）。
      - Skill: 参考首审 arch-gov-review 方法
- [ ] F4 残留复审：核验共享内核守卫（R12a `ErpFinBusinessType` 跨域 import 基线 69 / R12b 66 / R12c 38）相对 M0.3 锚点是否上漂（新增跨域 import 意味共享内核扩张）。若上漂，定位新增消费方域并登记。
      - Skill: 参考首审 arch-gov-review 方法
- [ ] F3/F5/F6/F7 残留复审：核验 DAG 边表格完整性（3 边仍登记）+ notify owner doc 仍存在 + `ErpQaInspectionType` 未回退（mfg 不再 import `_ErpQaDaoConstants`）+ drp 4 实体命名例外仍登记。快速确认性核查。
      - Skill: 参考首审 arch-gov-review 方法
- [ ] F8/F9 CI guard 复审：运行 `nop-compliance-checker.sh` 得到当前 19 规则基线，对照 `docs/audits/compliance-baseline.md` M0.3 锚点（HEAD=0e963531d）核验无上漂（actual ≤ baseline）。核验 `.github/workflows/compliance.yml` 仍在 PR 检查路径 + `web-pages-validation` job 仍运行。若基线上漂，定位漂移源并登记 P0/P1。
      - Skill: compliance-checker
- [ ] 新漂移扫描（自 2026-07-23 以来）：grep 全仓新增跨域写（`daoFor(Erp*` + `updateEntity`/`saveEntity` 共现，排除已登记豁免）+ 新增 `@Transactional` + 新增 `extends RuntimeException` + 新增 `@Inject private` + 新增 `System.currentTimeMillis`/`LocalDate.now`。交叉 compliance checker 基线确认无系统性回退。重点扫描 2026-07-24→27 落地的密集变更域（finance GL mapping / intercompany / commitment / mfg MRP-DRP 仿真 / FX notes）。
      - Skill: 参考首审 arch-gov-review 方法
- [ ] scope matrix §2.1 架构治理行剩余 `❓` 列快速机械核查（daoFor/字典/共享内核/guard 四子维度）——首审矩阵单元级未覆盖的 **12 个 `❓` 域**（hr/assets/pur/sal/qa/crm/prj/cs/ct/mnt/aps/log）的快速 grep 确认，补全全域结论。注意：7 个已标记 `⚠️` 域（finance/mfg/b2b/inv/md/drp/notify）仅复核残留未回退，不重复首审结论。
      - Skill: 参考首审 arch-gov-review 方法
- [ ] 产出复审报告 `docs/audits/2026-07-27-1430-arm-ma1-architecture-governance-review.md`（含：F1–F9 残留复审表 + 新漂移扫描结论 + CI guard 基线对照表 + scope matrix §2.1 架构治理行全域结论 + finding 按 P0/P1/P2 分级 + 残留风险）。追加更新首审报告 §闭包前必须项的复审注记（日期 + 复审结论）。
      - Skill: none

Exit Criteria:

- [ ] F1–F9 残留状态全部有复审结论（未回退 / 已回退并登记）
- [ ] 新漂移扫描完成，CI guard 基线对照 M0.3 锚点有明确结论
- [ ] scope matrix §2.1 架构治理行全域无 `❓`（12 个 `❓` 列经快速机械核查全部转为 `✅`/`⚠️(residual)`）
- [ ] 报告产出，首审报告复审注记已追加

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: planned
Targets: 复审发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.1
Skill: none

- Item Types: `Fix | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（如 CI guard 基线严重上漂 / 新增跨域写绕过 I\*Biz / 共享内核静默扩张未登记）当即就地修复或异步注入 fix plan。P0 永不进入 MR。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部新 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA1-NNN`，续 MA1 里程碑 P1 序号——A1.13 后接续），供 R1.0 展开机制转化为具体修复工作项行。已知残留（Type 4 / governed path 平台解耦）不重复登记，仅记录复审结论。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本复审报告行）+ scope matrix §2.1 架构治理行全域列更新（`❓` → `✅`/`⚠️(P1)`/`⚠️(residual)`）。MA1 里程碑（A1.1–A1.14）全部 done。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有新 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [ ] arm-index 报告清单 + scope matrix §2.1 已反映复审结论（MA1 全部 done）

## Draft Review Record

- Independent draft review iteration 1: **needs-revision**（`ses_05da406deffe7p5f0bgEwPVCMe`，独立 general 子代理，对照实时仓库逐项复核）。VERDICT = needs-revision，**2 BLOCKER + 4 NON-BLOCKER**。
  - **BLOCKER B1（已修复）**：scope matrix §2.1 架构治理行 `❓` 计数错误——计划称"10 列仍 ❓ / 仅 9 域有首审结论"，实测 `:94` 行为 **12 ❓ + 7 ⚠️**（finance/mfg/b2b/inv/md/drp/notify）。少计 2 域。已校正为"7 列 ⚠️ + 12 列 ❓"，并明确列出 7 个 ⚠️ 域的标记内容（F1residual/F6✅/F1half/⚠️/F4✅/F7✅/F5✅）。
  - **BLOCKER B2（已修复）**：Phase 1 ❓-fill 域清单错误——计划列 b2b/inv（实为 ⚠️ 非 ❓）且漏 hr（实为 ❓）。已校正为精确 12 个 ❓ 域（hr/assets/pur/sal/qa/crm/prj/cs/ct/mnt/aps/log），并补注 7 个已 ⚠️ 域仅复核残留不重复首审结论。Exit Criteria 同步补"12 个 ❓ 列"量化。
  - **NON-BLOCKER N1（已采纳）**：Task Route BUILD_VERIFY 表述——Closure Gates 已含"零 P0 即时修复 → 全量作回归基线确认"对齐 roadmap §其他纪律，无需改动。
  - **NON-BLOCKER N4（已采纳）**：Goals 追加"与 A1.13 的关系"注释——架构治理维度与平台合规维度正交，A1.14 不依赖 A1.13，防止审查者混淆。
- Independent draft review iteration 2: **accept**（主代理对照实时仓库复核修订）。修订后：❓ 计数全域一致（Current Baseline 7 ⚠️+12 ❓ / Goals 补全 ❓ 列 / Phase 1 ❓-fill 精确 12 域 / Exit 12 ❓ 列）；首审 F1–F9 闭包状态（§闭包前必须项 #1–#12 全 ✅）核实准确；Type 4 / governed path 平台解耦正确引用 `docs/analysis/governed-path-cost-evaluation.md:68,79` 分支 (b) 不重复裁决；compliance checker（`nop-compliance-checker.sh` 24735 字节可执行）+ `.github/workflows/compliance.yml`（5031 字节，compliance job + web-pages-validation job）+ `compliance-baseline.md` M0.3 锚点（HEAD=0e963531d）均存在；规则 7（阶段退出本地化 / Closure Gates 全仓库验证）合规；规则 4/13/14（单一结果表面"架构治理复审报告" + 4 子维度同 owner doc 同 surface + Type 4 为前序裁决 deferred 非实时缺陷降级）合规；Deferred successor 触发条件具体（平台 lazy/SPI 解耦落地）；N=3 顺序合理（文档顺序 A1.14 后于 A1.13，但已注明独立可执行）。两 BLOCKER 已闭合，文本一致性恢复。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。

- [ ] 范围内行为完成（A1.14 架构治理复审报告产出 + F1–F9 残留结论 + 新漂移扫描 + CI guard 基线对照 + arm-index 更新 + scope matrix §2.1 架构治理行全域结论）
- [ ] 相关文档对齐（复审报告、首审报告复审注记、arm-index、scope matrix 已反映复审结论）
- [ ] 已运行验证：`nop-compliance-checker.sh` 当前基线 ≤ M0.3 锚点；零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test` 作回归基线确认；若有 P0 即时修复则该修复子切片独立验证
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 不得降级为 MR；已知残留 Type 4/governed path 平台解耦属首审已裁决 deferred，引用前序裁决不重复）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志都一致
- [ ] 独立结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### daoFor Type 4 跨域写/读残留（~10-30 处）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 首审（2026-07-23）F1 闭包项 #1 已裁决分支 (b)：Type 4 需 nop-entropy 平台层 lazy/SPI 解耦（`docs/analysis/governed-path-cost-evaluation.md`），超本项目范围。本复审核验其计数/现状未恶化，不重复裁决、不升级为 P1。
- Successor Required: `yes`——首审已命名触发条件（平台 lazy/SPI 解耦落地时）。

### governed path 平台解耦（F1 闭包项 #1 裁决）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 首审裁决=分支 (b)，I\*Biz 强注入破坏单模块测试（contract/aps 反事实验证），需平台层解耦。超本项目范围，可能需 `nop-entropy` 协同。本复审引用前序裁决不重复。
- Successor Required: `yes`——nop-entropy 平台 lazy/SPI 解耦机制落地。

## Closure

Status Note: _（结束审计通过后填写）_

Closure Audit Evidence:

- _（独立结束审计子代理执行后填写）_

Follow-up:

- 若 P0 即时修复注入 fix plan，该 fix plan 独立 closure
- P1 finding 经 R1.0 展开机制进入 MR1
- daoFor Type 4 / governed path 平台解耦维持首审 deferred successor
