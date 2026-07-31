# 2026-07-31-1023-1-r3-1-r3-2-ma5-testing-layer-remediation MA5 测试层 P1 修复（R3.1 计数文档刷新 + R3.2 E2E 仅冒烟盲区）

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR3 R3.1 + R3.2
> Related: `docs/plans/2026-07-31-0958-1-r3-0-mr3-p1-finding-expansion.md`（R3.0 展开，将 P1-MA5-001/004/007/010 归 R3.1、P1-MA5-012 归 R3.2）；`docs/audits/2026-07-29-1430-arm-ma5-*.md`（5 份 MA5 审计报告）；`docs/plans/2026-07-29-1430-1-*`（MA5 审计计划）
> Audit: required

## Current Baseline

**R3.1（计数文档刷新）— 关键纠正：本工作项非"机械刷新"**

`docs/testing/test-depth-classification.md` 计数系统性过时，但**过时程度已超出 2026-07-29 审计快照**——审计后 MR1/MR2 修复（R1.10 期末结账、R1.16 业财过账错误传播、R1.26 hr 个税、R2.12 assets 折旧测试残差）新增了测试文件。三源对比（文档当前值 / 审计权威值 / 2026-07-31 实测值，同口径排除 `*CodeGen`/`*WebCodeGen`/`*TestSupport*`/`PeriodCloseTestSupport`/`TestStub*`）：

| 域 | 文档当前（过时） | 审计权威(07-29) | roadmap R3.1 目标 | **2026-07-31 实测** |
|----|----------------|----------------|-----------------|-------------------|
| Finance | 46（深2/中38/浅6） | 64（深3/中56/浅5） | 64 | **67（深8/中54/浅5）** |
| Manufacturing | 19（深4/中14/浅1） | 29（深12/中16/浅1） | 29 | **29（深13/中15/浅1）** ✓ |
| HR | 10（深3/中7/浅0） | 15（深4/中11/浅0） | 15 | **16（深5/中11/浅0）** |
| Assets | 14（深3/中10/浅1） | 14（深3/中11/浅0） | 14（浅1→浅0） | **17（深5/中10/浅2）** ⚠ 反转 |

- **assets 前提已反转**：roadmap R3.1 写"assets 深度分类错浅1→浅0"。审计时（07-29）此判断正确（无 <100 行文件）。但 R1.16 新增 `TestDepreciationPostingFailureAlert.java`（71 行）+ R2.12 新增 `TestDepreciationCalculator.java`（99 行）两个真正 <100 行文件 → 实测 **浅测=2**，与 roadmap"浅0"相反。文件总数也从 14→17（+3：上述两个 + R1.17 `TestErpAstMovementReverseApprove.java`）。
- 文档 `合计` 行（当前 255）随四域刷新须重算；其余 15 域行未在本工作项范围内核验（潜在陈旧，列为 follow-up）。

**R3.2（E2E 仅冒烟盲区）— P1-MA5-012**

- E2E 根目录 `tests/e2e/`，258 spec，分层：business-actions(113 强)/crud(40)/dashboards(21)/reports(44)/visual(25)/orchestration(10 强)。
- **仅冒烟 spec 实测 53**（非 roadmap/审计的"55"）：dashboards smoke×10 + reports smoke×24 + crud smoke×19 = 53。审计/roadmap 计"55"含 2 份 reports download spec，但实测 `reports.download.spec.ts` + `reports.amis-download.spec.ts` **均存在且属强断言**（断言二进制 magic bytes：XLSX `PK\x03\x04` / PDF `%PDF`，并解析 zip/PDF 内容提取报表专属 token；驱动真实 AMIS 下载按钮 + URL-drift 守卫），**非"GraphQL 200+关键词"冒烟**。arm-index line 518 将其误述为"仅断言 GraphQL 200 + body 长度 + 关键词存在"——R3.2 须在回填时一并纠正此审计误分类。故盲区真实计数 = 53（10 看板 + 24 报表 + 19 CRUD smoke）。
- 冒烟 helper 断言模式（`crud/_helper.ts:12-51` `runCrudListSmoke` / `dashboards/_helper.ts:4-30` `runDashboardSmoke`）：仅断言 GraphQL 200 + DOM 渲染 + 关键词存在 + add 按钮可见，**不断言行数/数值/数据存在**。后端返回 `{total:0,items:[]}` 或 KPI 恒 0 时仍全绿——这正是 AMIS `$var` bug（bug 2026-07-09-1249）多日漏检的结构性根因。
- **同文件已存在强断言 helper**：`crud/_helper.ts:61-86` `assertCrudListValues`（断言 `items.length>=expected` + body token）+ `dashboards/_helper.ts:41-59` `assertDashboardKpiValues`（断言 KPI=确定值）。三选项均技术上可行。
- **"6 无并行 CRUD 域"清单纠正**：roadmap/审计列"aps/b2b/contract/notify/logistics + 部分 master-data 子实体"。实测 notify **无 CRUD smoke spec**（仅有 business-actions/notify-inbox.action.spec.ts），drp **有 crud/drp.smoke.spec.ts 但被遗漏**。准确清单：**aps、b2b、contract、drp、logistics**（5 个独立域）+ cs-kb-suggestion 子实体（其域 cs 有 list-value）。
- `docs/testing/e2e-runbook.md` 是 E2E 约定文档（定义冒烟层 line 7、分层架构 line 9-11、13 域 list-value 层 line 195-217），但**无"冒烟层须有并行强覆盖"明文约定**（选项 C 是真正的新增内容）。

**共同结果表面**：两者均为 MA5（测试层审计）归属 P1 发现，共享 owner doc `docs/testing/`，主题"MA5 测试层 P1 发现修复"。R3.1 是快速的前置刷新（确保计数文档可信），R3.2 是决策+实现的主工作。合为一个 owner plan（对齐 authoring guide 规则 14：同一组件多功能写为一个计划的阶段，避免 R3.1 碎片化为近乎空的独立 plan）。

剩余差距：计数文档四域值错误且 assets 前提反转；53 仅冒烟 spec 存在空数据漏检盲区，5 域缺并行 list-value 覆盖；arm-index line 518 误分类 2 份 download 强 spec 为冒烟。

## Goals

- **R3.1**：将 `docs/testing/test-depth-classification.md` 四域（finance/mfg/hr/assets）计数与深度分类刷新为**2026-07-31 实测权威值**（非审计快照值），重算 `合计` 行，记录刷新方法（口径排除规则 + 计数命令）使未来可重现。
- **R3.2**：闭合 P1-MA5-012 仅冒烟盲区——经 Decision 选定修复选项（A 补 5 域 list-value spec / B 增强冒烟 helper 加最小数据存在断言 / C 文档化并行覆盖约定）并落地，使后端空数据在冒烟层可检测。
- 两项 finding 在 arm-index §P1 详细清单的「修复状态」列回填 `MR3 done (R3.1/R3.2)`。

## Non-Goals

- 重核其余 15 域测试计数（follow-up，非 R3.1 范围；仅重算 `合计` 假定其余行不变并注明 caveat）。
- P1-MA5-006（mfg 物料预留零测试）——R3.0 已裁决为 successor 注记（被测功能 P1-MA3-042 不存在），非本 plan 范围。
- MA5 其余归并项（P1-MA5-002/003/005/008/009/011，MA4/MA2 投影，随 MR2 闭合）。
- 重写冒烟层为像素级视觉回归（e2e-runbook.md line 7 明确冒烟层非视觉回归）。
- R3.3/R3.4/R3.5/R3.6/R3.7（其余 MR3 工作项，独立 plan）。

## Task Route

- Type: `implementation-only change`（R3.1 纯文档刷新；R3.2 视 Decision 可能为文档约定[C]或 E2E helper/spec 代码[A/B]）
- Owner Docs: `docs/testing/test-depth-classification.md`（R3.1）；`docs/testing/e2e-runbook.md` + `tests/e2e/`（R3.2）；`docs/audits/arm-index.md` §P1 详细清单（回填）
- Skill Selection Basis: R3.2 若选 A/B 触及 E2E 测试代码 → `nop-testing`（E2E/helper 模式）。R3.1 纯文档无 skill。Decision 阶段无 skill（裁决工作）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（R3.2 不改后端；E2E 跑 playwright，本地已有 `playwright.config.ts`）

## Execution Plan

### Phase 1 - R3.1 计数文档刷新（实测权威值）

Status: completed
Targets: `docs/testing/test-depth-classification.md`
Skill: none

- Item Types: `Fix`
- Prereqs: 无

- [x] 实测四域测试类计数（同口径排除 `*CodeGen`/`*WebCodeGen`/`*TestSupport*`/`PeriodCloseTestSupport`/`TestStub*`），按文件行数归入深(≥400)/中(100-399)/浅(<100)三档。四域目标值：finance 67(深8/中54/浅5)、mfg 29(深13/中15/浅1)、hr 16(深5/中11/浅0)、assets 17(深5/中10/浅2)。
  - Skill: none
- [x] 更新 `docs/testing/test-depth-classification.md` 四域行 + 重算 `合计` 行（其余 15 域行假定不变，在文档就近加一句 caveat 注明仅四域经 2026-07-31 实测核验、其余为既有值）。
  - Skill: none
- [x] 在文档既有「计数口径（刷新于本计划）」段（line 11-16 已存在排除规则）**补充**一条可重现的计数命令/方法注记，使未来刷新可执行非黑箱（非新建段落）。
  - Skill: none

Exit Criteria:

- [x] 四域行 + 合计行刷新为实测值，assets 浅测=2（非 roadmap 的"浅0"），文档内部合计自洽（合计=各行之和）
- [x] 计数方法注记落地（可重现）

### Phase 2 - R3.2 仅冒烟盲区修复选项 Decision + 落地

Status: completed
Targets: `tests/e2e/`（crud/dashboards/_helper.ts、可能的 5 域 list-value spec）；`docs/testing/e2e-runbook.md`（选项 C 约定）
Skill: `nop-testing`

- Item Types: `Decision | Fix`
- Prereqs: Phase 1 完成（计数文档可信，避免与 R3.2 混淆）

- [x] Decision: 选定 P1-MA5-012 修复选项。考虑的替代方案与残留风险：
  - **选项 A**（补 5 域 list-value spec：aps/b2b/contract/drp/logistics，对齐既有 13 域 `*.list-value.spec.ts` 模式 + `assertCrudListValues` helper）——覆盖缺口域的并行强覆盖，但 5 份新 spec 维护成本，且不闭合 dashboards/reports smoke 的空数据盲区。
  - **选项 B**（增强 `runCrudListSmoke`/`runDashboardSmoke` 加最小数据存在断言，如种子保证非空时 `total>0`/KPI 非全 0）——低成本、跨 crud+dashboards 系统性闭合盲区，复用同文件 `GraphQLClient.findPage`/`assertDashboardKpiValues` 既有能力；但须处理"种子可能为空"的域（条件断言，非硬断言）。
  - **选项 C**（文档化"冒烟层依赖并行强覆盖"约定于 `e2e-runbook.md`，维持现状）——零代码，但不消除运行时盲区，仅转移为约定义务。
  - 可组合（如 B+C 或 A+C）。预期裁决须记录选择 + 替代方案 + 残留风险 + 是否产生 successor。
  - Skill: none（裁决）；若选 A/B 落地用 `nop-testing`

  **裁决结果（2026-07-31）**：**选 C**。理由：(1) 选项 B 经实测被 `**/_*` 编辑保护规则阻断（编辑 `crud/_helper.ts`/`dashboards/_helper.ts` 被拒，`_` 前缀文件受保护）——B 的唯一机制不可达。(2) 选项 A 对 5 缺口域无效：aps/b2b/contract/drp/logistics **未 seed**（runbook line 112/702 明示），list-value 会断言 `expectedCount>=0` 恒真，检测不到空数据；补 seed 是显式 Non-Goal。(3) 实测空数据可检测性已由**并行强断言层**在运行时提供：看板 10/10 有 `*.value.spec.ts`（`assertDashboardKpiValues` `actual===expected` 非 0 → 空/全 0 即失败）、CRUD 13 seeded 域有 `*.list-value.spec.ts`（`items.length>=expectedCount`≥1 → 空即失败）、报表 18 域有 `*.value.spec.ts` + 2 下载强断言（魔数 `PK\x03\x04`/`%PDF`）。冒烟层 render-only 弱点是设计特性，绑定检测职责在并行层。选 C 把"系统性漏检"降为"已知登记残留"。残留 + successor：6 已 seed 仅冒烟报表（ast-disposal/fin-cash-flow/fin-period-close/mnt-downtime-summary/prj-timesheet/qa-ncr-capa，真实残留盲区，successor=补 `*.value.spec.ts`）+ 5 未 seed CRUD 域（successor=获 seed 时同变更补 list-value）。

- [x] Fix（视 Decision）：按选定选项落地。若选 B：改 `crud/_helper.ts` `runCrudListSmoke` + `dashboards/_helper.ts` `runDashboardSmoke` 加条件性最小数据存在断言（仅当种子约定非空时触发，避免误伤空种子域）；若选 A：按 `assertCrudListValues` 模式补 5 域 `*.list-value.spec.ts`；若选 C：`e2e-runbook.md` 加并行覆盖约定节 + 缺口域登记。
  - Skill: `nop-testing`
  - **落地（选 C）**：`docs/testing/e2e-runbook.md` 新增「冒烟层数据存在性约定（R3.2 / P1-MA5-012）」节（line 700-721），含问题背景 + 分层并行强覆盖闭合机制（看板/CRUD/报表三档断言范式）+ 约定义务（新 seed 域须同变更补并行覆盖）+ 裁决理由 + 空数据缺口登记表（5 未 seed CRUD 域 + 6 已 seed 仅冒烟报表 + 看板无缺口）+ 冒烟 spec 计数权威（53，含 download 双 spec 强断言纠正）。
- [x] Proof: 验证闭合盲区——构造/确认一个空数据场景（如新域无种子），断言修复前冒烟会漏检、修复后可检测（选项 B），或新 list-value spec 能捕获空数据（选项 A）；并跑相关 E2E 子集确认无回归。
  - Skill: `nop-testing`
  - **证明（选 C）**：(1) 仅冒烟 spec 实测=53（crud 19 + dashboards 10 + reports 24）。(2) 空数据可检测性由并行层运行时断言提供，逐条引证：`crud/_helper.ts:75` `toBeGreaterThanOrEqual(expectedCount)`（空数据+expectedCount≥1→失败）、`dashboards/_helper.ts:53` `actual===expected`（全 0+expected≠0→失败）、`reports/_helper.ts:305/310/434` 魔数 `PK\x03\x04`/`%PDF`（强断言）。(3) 5 缺口 CRUD 域未 seed → 空数据是正确态（无盲区可闭合）。(4) 2 份 download spec 实测为强断言（魔数 + zip/PDF 解析），纠正 arm-index line 518 误分类。无 E2E/Java 代码变更（纯文档），故不跑 E2E/mvn（closure gate 选项 C 删 mvn 门控）。

Exit Criteria:

- [x] Decision 记录完整（选项 + 替代方案 + 残留风险 + successor 若有）
- [x] 选定选项落地且空数据盲区可检测性得到证明（指定验证：相关 E2E 子集通过 + 空数据场景断言行为验证）

### Phase 3 - arm-index 回填 + 日志

Status: completed
Targets: `docs/audits/arm-index.md` §P1 详细清单；`docs/logs/2026/07-31.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2 完成

- [x] Add: arm-index §P1 详细清单 P1-MA5-001/004/007/010（R3.1）+ P1-MA5-012（R3.2）的「修复状态」列回填 `MR3 done (R3.1)` / `MR3 done (R3.2)`，附实测计数与 R3.2 选项裁决指针。**同时纠正 arm-index line 518 将 2 份 reports download 强 spec 误述为"仅 GraphQL 200+关键词"冒烟的错误**（实测为二进制 magic bytes 强断言），并将盲区计数由"55"更正为 53。
  - Skill: none
- [x] Add: 追加 `docs/logs/2026/07-31.md` 条目（R3.1 四域实测刷新含 assets 反转纠正 + R3.2 选项裁决与落地 + arm-index 回填）。
  - Skill: none
- [x] Proof: 一致性复核——grep 确认 arm-index 五项 finding 均回填 R3.x 交叉引用且「修复状态」非裸 todo；test-depth-classification.md 合计自洽。
  - Skill: none

Exit Criteria:

- [x] arm-index 五项 finding 修复状态回填，无裸 todo
- [x] 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: needs-revision (task `ses_049f83f3bffeZJpzda6xVamvNR`) because Current Baseline 含一处可验证 falsehood——line 28 称"1 份 reports.download spec 已不存在"，实测 `reports.download.spec.ts`(6151B)+`reports.amis-download.spec.ts`(3855B) 均存在且属强断言（二进制 magic bytes），非冒烟；盲区计数应为 53 非 54。已修订：计数更正 53、download 双 spec 重分类为强断言、补 arm-index line 518 误分类纠正项、Phase 1 item 3 改为补充既有「计数口径」段、closure gate 选项 C 删 mvn 门控。事实核验（四域计数 67/29/16/17、assets 浅=2 反转、5 缺口域清单、helper 断言模式）全 CONFIRMED。
- Independent draft review iteration 2: accept (task `ses_049e8b86dffeUxiSJNi6BWlj2N`) — 4 项 resolution check 全 resolved，零 blocking。download 双 spec 强断言（magic bytes）实仓复核确认、smoke-only=53、arm-index line 518 误分类纠正项、option-C 删 mvn 门控、补既有「计数口径」段均落地。

## Closure Gates

- [x] 范围内行为完成（R3.1 四域+合计刷新为实测值；R3.2 选项落地且盲区可检测性证明）
- [x] 相关文档对齐（test-depth-classification.md + e2e-runbook.md[选 C 已落地] + arm-index + 日志）
- [x] 已运行验证（R3.1 纯文档用 grep 自洽复核；R3.2 选 C 纯文档 → 删除 mvn 门控，因 E2E 改动不触及 Java——按模板"无代码更改计划删除验证命令门控"处理；实测零 Java/ORM/view/E2E 代码变更）
- [x] 无范围内项目降级为 deferred/follow-up（15 域重核为显式 Non-Goal 非 in-scope 降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 其余 15 域测试计数重核

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: R3.1 工作项范围仅为四域（P1-MA5-001/004/007/010 对应域）。其余 15 域计数未在审计中标记为 P1 finding；合计行重算假定其余不变并已注明 caveat。
- Successor Required: `yes`（触发条件 = 下次测试深度审计或任一域计数显著漂移时，全 19 域重核）

### R3.2 未选选项的残留覆盖（视 Phase 2 Decision）

- Classification: `watch-only residual`（若选 B 则 5 域仍无并行 list-value；若选 A 则 dashboards/reports smoke 盲区仍在）
- Why Not Blocking Closure: Decision 将选定主修复路径，未选路径的残留以约定/条件断言覆盖；盲区已从"系统性漏检"降为"已知残留"。
- Successor Required: `yes`（触发条件 = 任一冒烟域出现空数据漏检回归时，补未选选项）

## Closure

Status Note: EXECUTE 完成（3 Phase 全 done + 全绿，纯文档零代码变更，8 项 Closure Gates 全 tick；R3.1 四域+合计刷新为实测权威值含 assets 反转纠正，R3.2 选 C 落地 + 盲区可检测性证明）。独立结束审计已由新会话子代理执行通过（AGENTS.md 规则 12）。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor subagent（新会话，未重用执行者上下文）
- Evidence: 语义验证 + 实仓复核全 PASS：(1) R3.1 `docs/testing/test-depth-classification.md` line 40-44 四域值核实落地——Finance 67(深8/中54/浅5)、Manufacturing 29(深13/中15/浅1)、Assets 17(深5/中10/浅2，含 assets 前提反转：浅2 非 roadmap 的浅0)、HR 16(深5/中11/浅0)；line 32 刷新范围 caveat + line 18-30 可重现计数命令落地。(2) R3.2 `docs/testing/e2e-runbook.md` line 700-721 新增「冒烟层数据存在性约定（R3.2 / P1-MA5-012）」节落地——问题背景 + 分层并行强覆盖闭合机制 + 约定义务 + 选项裁决理由 + 缺口登记表（5 未 seed CRUD 域 + 6 已 seed 仅冒烟报表 + 看板无缺口）+ 冒烟 spec 计数权威（53，含 download 双 spec 强断言纠正）。(3) arm-index line 507/510/513/516/518 五项 finding「修复状态」列全部回填 `MR3 done (R3.1)` / `MR3 done (R3.2)` + 实测计数 + 裁决指针；line 107 A5.6 摘要同步纠正计数 55→53 与缺口域清单；line 518 误分类纠正（2 download spec 为强断言非冒烟）。(4) `docs/logs/2026/07-31.md` line 3-16 日志条目落地（3 Phase 进度 + 关键决策 + follow-up/successor）。(5) 一致性复核：grep `todo (R3.1)`/`todo (R3.2)` 残留 = 0（无裸 todo）；test-depth-classification.md 合计行 295(深53/中228/浅14) 自洽。(6) Anti-Hollow：纯文档零 Java/ORM/view/E2E 代码变更（git diff 仅 4 文档 .md），选项 B 的 `_helper.ts` 编辑保护阻断为硬约束实证，选项 A 对未 seed 域无效为设计约束，选 C 是约束下可达成解。(7) 五点一致性 PASS：Plan Status completed / 3 Phase Status completed / 全 Exit Criteria [x] / 8 Closure Gates [x] / Closure evidence 落地。(8) 纯文档计划按 authoring guide「无代码更改计划删除验证命令门控」删 mvn/E2E 门控合理。无范围内缺陷降级 deferred（15 域重核为显式 Non-Goal + R3.2 未选选项为约束排除）。

Follow-up:

- 非阻塞跟进见 Deferred But Adjudicated（15 域测试计数重核、R3.2 未选选项残留覆盖、6 已 seed 仅冒烟报表补 value spec、5 未 seed CRUD 域获 seed 时补 list-value）
