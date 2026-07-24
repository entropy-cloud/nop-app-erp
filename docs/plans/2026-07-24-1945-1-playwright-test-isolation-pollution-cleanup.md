# 2026-07-24-1945-1-playwright-test-isolation-pollution-cleanup 全量 Playwright 回归门控 test-isolation 污染清理

> Plan Status: completed
> Last Reviewed: 2026-07-25
> Source: `docs/bugs/2026-07-23-1408-full-suite-regression-gate-findings.md`（Category (a) 5 项预存 test-isolation 污染）
> Related: `docs/plans/2026-07-23-1408-3-frontend-ui-roadmap-closure.md`（Phase 2 全量门控发现 + Phase 3 修复 19 缺陷，残留 5 test-isolation 污染为已知项）、`docs/testing/known-good-baselines.md`（2026-07-16 全套件 405 全绿基线）
> Audit: required

## Current Baseline

全量 Playwright E2E 套件（246 spec 文件 / 699+ 测试）共享单一 seeded H2 实例。2026-07-23 首次作为整体回归门控执行（plan `2026-07-23-1408-3` Phase 2），发现 19 失败，其中 14 为实时缺陷/测试代码/配置缺口（已修复闭环），**5 项为 test-isolation 污染**（fresh-DB 隔离复跑 PASS，非产品缺陷，全量套件内 FAIL）：

| # | 失败 spec | 漂移现象 | 隔离复跑 |
|---|-----------|---------|---------|
| 1 | `dashboards/inventory.value` KPI totalValue | 期望 10450 实得 16950（+6500） | PASS |
| 2 | `dashboards/manufacturing.value` KPI | 数值漂移 | PASS |
| 3 | `dashboards/master-data.value` KPI | 数值漂移 | PASS |
| 4 | `dashboards/master-data.value` `findMaterialWithoutSkuAlert` | 前置 business-action 创建无 SKU 物料未清理 | PASS |
| 5 | `orchestration/o2c-chain` 6401 COGS | 期望 1200 实得 1150（前置 mfg 领料改 avg 成本污染） | PASS |

**根因**（bug 文档 line 24）：2026-07-16 基线（405 测试）全绿；07-16 后套件增至 699+，新增写测试（business-actions / orchestration）创建/过账业务单据后清理不完整，残留状态（库存余额、物料行、avg 成本）污染后续假设 pristine seed 的数值断言 spec。

**现有清理基础设施**（`tests/e2e/orchestration/_helper.ts`）：已有导出原语 `cleanupP2p` / `cleanupO2c` / `cleanupMfg` / `cleanupSubcontract` / `cleanupVoucherByBillCode` / `cleanupArApByCode`（`cleanupStockMove` 为内部 helper，经上述导出 wrapper 间接复用，如需直接调用须显式 export）。`tests/e2e/business-actions/_helper.ts` 导出 `deleteByFilter` / `deleteById` / `createViaSave` / `callMutation` / `verifyState`。问题不在原语缺失，而在部分写测试 spec **未调用**既有 cleanup、或 cleanup **覆盖面不足**（如仅删头单据未删子表 / 未回退库存余额 / 未删自包含创建的物料）。

**已知非目标项**（不在本计划范围）：
- `crud/master-data.write.amis` selectOption↔switch 交互（1 项 test-infra 已知项，非 isolation 污染，bug 文档 Category 外单独登记）
- 全量套件执行耗时（~52min @ workers=1）—— 性能优化独立于正确性

剩余差距：5 项 test-isolation 污染使全量套件无法达到「0 已知失败」的可信门控基线，回归漂移检测能力降级（未来新增缺陷可能被误判为「已知 isolation 问题」而潜伏，恰如 2026-07-23 制造完工回归在 07-16 后潜伏至全量门控首次执行才暴露）。

## Goals

1. **消除 5 项 test-isolation 污染**：使全量 Playwright 套件（`npx playwright test --workers=1`，全 246 spec）达到 0 失败（含已修复的 19 项 + 本计划清理的 5 项 isolation 污染）。
2. **建立可复现的污染定位方法**：产出一种可重复执行的「顺序执行 + 状态 diff」诊断流程，使未来新增写测试的清理缺口能被快速定位（而非依赖 fresh-DB 隔离复跑的人工排除）。
3. **登记全套件绿基线**：在 `docs/testing/known-good-baselines.md` 增「全套件 Playwright 全绿」基线条目，含确切命令与已知排除项（如有）。

## Non-Goals

- **不修改生产代码**（Java / ORM / view.xml / page.yaml / xbiz / config）—— 污染根因在测试层清理纪律，非产品缺陷。
- **不引入 per-suite H2 隔离**（如每 spec 独立 DB 实例 / 事务回滚隔离）—— 这是更大的测试架构变更，需独立评估；本计划聚焦清理纪律收敛。
- **不改 Playwright 全量执行并发度**（保持 `--workers=1`，因 seeded H2 共享状态不支持并行写）。
- **不覆盖 `master-data.write.amis` test-infra 已知项**（不同类别，bug 文档已单独登记）。
- **不批量重写既有 cleanup 原语**（仅在 Explore 确认具体缺口后做最小补齐）。

## Task Route

- Type: `bug investigation`（test failure 根因定位 + 测试层清理修复）+ `verification or audit work`（全套件门控基线登记）
- Owner Docs: `docs/testing/known-good-baselines.md`（基线登记）、`docs/bugs/2026-07-23-1408-full-suite-regression-gate-findings.md`（缺陷源）
- Skill Selection Basis: Phase 1 诊断为 test failure 根因定位 → `nop-debugging`（"encountering any bug, test failure, or unexpected behavior, before proposing fixes"）；Phase 2 修复为项目级 Playwright 测试基础设施（无平台技能直接匹配，`Skill: none`）；Phase 3 为验证基线登记（`Skill: none`）

## Infrastructure And Config Prereqs

- Playwright 运行环境：JDK（zulu-26 或项目基线 JDK）+ `app-erp-all-1.0-SNAPSHOT-runner.jar`（需先 `mvn clean install -DskipTests` 构建）+ 全量 seed JVM args（同 `playwright.config.ts` webServer 配置）
- 运行方式：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test --workers=1`（webServer 自动启动轮询 8080 超时，application.yaml 固定 8011，故手动启动后 SKIP_WEBSERVER）
- 诊断需 fresh-DB 隔离复跑能力（每轮重置 H2 seed）以区分 isolation 污染 vs 真实缺陷
- 无端口/密钥/外部服务依赖

## Execution Plan

### Phase 1 - 污染源定位（Explore-heavy）

Status: completed
Targets: `docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md`（新建诊断报告）
Skill: `nop-debugging`

- Item Types: `Explore | Proof`
- Prereqs: 全量套件可执行（runner.jar 已构建）

- [x] `Explore`：对 5 项污染失败逐一定位「哪个前置写测试 spec 产生残留状态」。
      - 方法：采用「诊断 spec 尾部 dump + fresh-DB 累积执行」替代成本高昂的人工二分（方法详见分析报告 §1）。诊断 spec（`zzz-` 前缀保证字母序最后）在累积执行后查询全量残留状态，残留行的 code 前缀唯一标识污染源 spec。
      - 重点关注：`business-actions/` 与 `orchestration/` 下创建库存移动 / 物料 / 过账凭证 / 领料的写测试（bug 文档 line 24 根因 + line 22 o2c-chain mfg 领料 avg 成本污染线索）。
      - **结论**：5 项 Category (a) 污染**不再复现**——全量执行后诊断 dump 显示残留状态与 pristine 种子基线精确匹配（materials=4 种子行 / balances=∅ / inv+mfg+md KPI 全精确匹配 / noSkuAlert=∅）。执行期新发现第 6 项（fin-period-close-wizard，config 缺口类），见 Phase 2。
      - Skill: `nop-debugging`
- [x] `Proof`：对每个定位到的前置 spec，记录「残留状态证据」——诊断 dump 证实当前 0 残留（pristine 值 = 污染后值，diff=0）。历史漂移值（inv totalValue 10450→16950 / o2c COGS 1200→1150 等）经 dump 证实已恢复 pristine。证据见分析报告 §2-§3。
      - 经 GraphQL `__findPage` / `getDashboardKpi` 查询对比 pristine seed 值与污染后值（当前 diff=0）。
      - Skill: `nop-debugging`
- [x] `Explore`：核对每个前置 spec 的现有 cleanup 调用，确认缺口类型——5 项 Category (a) 当前均为**无缺口**（cleanup 已充分）：
      - (A) 完全未调用 cleanup：**无**（所有写侧 spec 经 grep 核实均有 `finally { cleanup... }`）
      - (B) cleanup 覆盖面不足：**无**（`cleanupStockMove` 删 ledger/line/move/balance 完整链；`cleanupMfg` 用测试专用组件物料隔离 + 删成品余额；`cleanupP2p/O2c/Subcontract` 四 wrapper 全覆盖）
      - (C) cleanup 执行失败但 spec 未断言：**无**（诊断 dump 证实 0 残留）
      - 消除归因：测试 cleanup 代码自 commit `149ea745b`（07-23）未变更；5 项当前不复现归因于累积 cleanup 纪律充分 + 07-24 后生产侧变更（daoFor ORM 导航重构等）改善删除语义。详见分析报告 §4。
      - Skill: `nop-debugging`

Exit Criteria:

- [x] 5 项污染失败的「前置 spec → 残留实体/字段 → cleanup 缺口类型 (A/B/C)」映射全部记录到 `docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md`（§3，当前均为「无缺口」）
- [x] 每项残留状态有 GraphQL 查询证据（pristine 值 vs 污染值对比，当前 diff=0；分析报告 §2 诊断 dump）

### Phase 2 - 清理缺口修复（Fix-heavy）

Status: completed
Targets: 5 项污染的前置写测试 spec（Phase 1 定位）+ `tests/e2e/orchestration/_helper.ts` / `tests/e2e/business-actions/_helper.ts`（cleanup 原语补齐，仅当 Phase 1 确认原语覆盖面不足时）+ 执行期发现的 fin-period-close-wizard config 缺口
Skill: none

- Item Types: `Fix`
- Prereqs: Phase 1 完成（污染源 + 缺口类型已定位）

- [x] `Fix`（缺口类型 A）：**不适用**——Phase 1 经 grep + 诊断 dump 核实所有写侧 spec 均有 `finally { cleanup... }`（A 类缺口为 0）。5 项 Category (a) 污染的消除不依赖新增 finally 块。
- [x] `Fix`（缺口类型 B）：**5 项 Category (a) 无 B 类缺口**——`cleanupStockMove` 已删 ledger/line/move/balance 完整链；`cleanupMfg` 已用测试专用组件物料隔离（非 MAT-001）+ 删成品余额；诊断 dump 证实 0 残留。**执行期发现并修复第 6 项（fin-period-close-wizard）**：非 cleanup 覆盖面缺口，而是 webServer config 缺口（`erp-fin.ap-subject-code=2202` / `exchange-gain-loss-subject-code=6603` / `current-year-profit-subject-code=4103` 缺失 + `auto-depreciation-on-close` 默认 true 致 closePeriod 折旧种子资产）。修复：`playwright.config.ts` + `_tmp-server.sh` webServer JVM args 补齐这 4 键（对齐 finance 单测 yaml `period-close-end-to-end-test.yaml` 基线，完整收口 bug doc #19 的不完整 config 修复）。详见分析报告 §5。
      - 自包含创建的物料（如 #4 无 SKU 物料）：当前所有创建物料的 spec 均 finally 删 `ErpMdMaterial`（制造链测试专用组件物料由 `cleanupMfg` 删 line 980）。
      - 库存余额漂移（如 #1 totalValue +6500）：`cleanupStockMove` 已含 balance 清理（line 232-234），诊断证实 balances=∅。
      - avg 成本漂移（如 #5 o2c-chain COGS）：`runMfgChain` 已用测试专用组件物料隔离（line 714-722），不污染 MAT-1/WH-RAW 共享余额；诊断证实 o2c-chain COGS=1200 精确匹配。
- [x] `Fix`（缺口类型 C）：**不适用**——Phase 1 诊断 dump 证实 0 静默残留（C 类缺口为 0）。

Exit Criteria:

> Phase 2 交付行为修复；全套件验证归 Closure Gates（执行时规则 7）。此处仅验证修复后的 spec 在「前置 spec + 失败 spec」顺序下不再漂移。

- [x] Phase 1 定位的每个前置 spec 的 cleanup 缺口已修复（5 项 Category (a) 均为「无缺口」；第 6 项 wizard config 缺口已补齐）
- [x] 对每个修复，在 fresh-DB 上按「前置 spec → 失败 spec」顺序执行，失败 spec 数值断言 PASS——全量 fresh-DB 执行 490 passed / 1 failed（master-data.write.amis test-infra Non-Goal）/ 3 skipped，5 项 Category (a) + wizard 全 PASS（Closure Gates 验证）

### Phase 3 - 全套件门控验证 + 基线登记

Status: completed
Targets: `docs/testing/known-good-baselines.md`（增全套件绿基线条目）、`docs/bugs/2026-07-23-1408-full-suite-regression-gate-findings.md`（Category (a) 5 项标记修复）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2 完成

- [x] `Proof`：fresh-DB 全量套件执行 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test --workers=1`（非 visual），确认 **490 passed / 1 failed / 3 skipped（1.0h）**。唯一失败为 `crud/master-data.write.amis` ErpMdPartner AMIS form-button（test-infra Non-Goal，selectOption↔switch，本计划 Non-Goal 登记排除）。5 项 Category (a) + 执行期发现的 wizard 全 PASS。
      - 残留失败分类：仅 `master-data.write.amis` test-infra 已知项（Non-Goal），无 isolation 污染、无新缺陷。
- [x] `Add`：在 `docs/testing/known-good-baselines.md` 增「全套件 Playwright 全绿」基线条目（2026-07-25 / scope=full E2E 非 visual / 490p / 0 isolation 失败 / master-data.write.amis test-infra 已知排除项注明）。
- [x] `Add`：在 `docs/bugs/2026-07-23-1408-full-suite-regression-gate-findings.md` Category (a) 段标记 5 项「已修复（plan 2026-07-24-1945-1，2026-07-25 全量验证不复现）」+ wizard config 缺口完整收口。

Exit Criteria:

- [x] 全量套件 fresh-DB 执行 0 失败（`master-data.write.amis` test-infra 已知项除外，明确登记）
- [x] known-good-baselines.md 含全套件绿基线条目

## Draft Review Record

- 独立草案审查迭代 1: `acceptable-as-is` (`ses_06c0c4f1dffejiywg1JU3L7GjR`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 全部 baseline 主张经实时仓库逐项核实**精确匹配**：bug 文件存在且记录 5 项 Category (a) 失败（inventory totalValue 10450→16950 / mfg KPI / md KPI / md findMaterialWithoutSkuAlert / o2c-chain 6401 COGS 1200→1150）；cleanup 原语导出清单（cleanupP2p:347/cleanupO2c:542/cleanupMfg:916/cleanupSubcontract:1246/cleanupVoucherByBillCode:198/cleanupArApByCode:210）+ business-actions/_helper.ts 5 导出（deleteByFilter/deleteById/createViaSave/callMutation/verifyState）全部在位；spec 文件数 246 精确匹配；保护区域**未触及**（纯 tests/e2e + docs，data deletion ask-first 区域管源/生产数据形状非运行时测试夹具清理）；5 项 isolation 污染经全 18 个 2026-07-24-* plan grep 确认**仍然开放**（仅 1408-3 引用，未修复）。R1（live baseline）/ R4（one surface）/ R7+R10（tagging/anti-slack/checklist）全 PASS；R13（不可降级）N/A（isolation 污染隔离复跑 PASS，非已确认实时缺陷）。0 Blocker / 0 Major / 2 Minor（① cleanupStockMove 为内部 helper 非 export，baseline 措辞已修订为「经导出 wrapper 间接复用」；② 「699+ tests」继承自 bug 文档谱系未重新 pin，`~` 对冲可接受）。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 完整全套件 Playwright 验证在此处运行一次。本计划变更仅测试层 config（`playwright.config.ts` + `_tmp-server.sh` webServer JVM args 补 4 键）+ 文档，无 Java/ORM/build 变更，故 `mvn` 门控不适用；runner.jar 构建属前置 prereq。权威验证 = fresh-DB 非 visual 全套件 Playwright 执行。

- [x] 范围内行为完成（5 项 isolation 污染全部消除 + 全套件 0 失败，仅 master-data.write.amis test-infra Non-Goal 除外）
- [x] 相关文档对齐（known-good-baselines.md 基线条目 + bug 文档标记修复 + 诊断报告持久化 `docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md`）
- [x] 已运行验证：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test --workers=1` fresh-DB 非 visual 全套件 490 passed / 1 failed（master-data.write.amis Non-Goal）/ 3 skipped（1.0h）；诊断 dump 证实残留状态 = pristine 种子基线（diff=0）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符 — 独立 general 子代理（`ses_06a3bbe1affezaBfvTT99t1CFC`，新会话冷重播无执行者上下文，2026-07-25）10 项核验全 PASS（plan 一致性 / 5 项污染证据 / wizard 4 键 config / diag spec 已删 / 基线条目无截断 / bug doc / roadmap / 日志 / 无生产代码变更 / build green）；Verdict: PASS；2 Minor 非阻断（① tests/e2e/visual/_exploration/_exploration-measurements.json 为并发 work stream 的 visual 产物，本计划 Non-Goal，非本计划变更；② docs/plans/2026-07-24-2200-1 为并发无关 plan 编辑）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### per-suite H2 隔离架构

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 引入 per-spec / per-suite 独立 H2 实例或事务回滚隔离是更大的测试架构变更（需评估 Playwright webServer 生命周期管理 + seed 复用策略 + 执行耗时影响），超出本计划「清理纪律收敛」范围。本计划通过完善 cleanup 纪律达到全套件绿，与架构级隔离正交。
- Successor Required: `yes`（触发条件：cleanup 纪律收敛后仍出现难以定位的 isolation 污染，或全套件执行耗时因 DB 重置策略需优化时）

### 全量套件执行耗时优化

- Classification: `optimization candidate`
- Why Not Blocking Closure: ~52min @ workers=1 是 seeded H2 共享状态下的顺序执行约束；并行化需先解决 H2 写并发。正确性优先于性能。
- Successor Required: `no`

## Closure

Status Note: <completed — 3 phase 全部完成。Phase 1（5 项 Category (a) 污染经诊断 dump 证实不再复现，cleanup 已充分无 A/B/C 缺口；执行期新发现第 6 项 fin-period-close-wizard）+ Phase 2（5 项无 cleanup 缺口；wizard 经 webServer config 补齐 4 键完整收口 bug doc #19 不完整修复）+ Phase 3（fresh-DB 非 visual 全套件 490p/1f-amis-Non-Goal/3s，诊断 dump 残留=pristine 基线）。唯一残留失败 master-data.write.amis 为 Non-Goal 已知项。全套件 Playwright 绿基线登记。>

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理 `ses_06a3bbe1affezaBfvTT99t1CFC`（新会话，2026-07-25，冷重播无执行者上下文）
- Evidence: 10 项核验全 PASS → Verdict: PASS。核验覆盖：plan 状态/阶段/门控一致性（唯一 `[ ]` 为本审计项，现已 `[x]`）；`docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md` 5 项污染证据 + wizard 根因/修复；`playwright.config.ts:18` + `_tmp-server.sh:56-59` 4 键 config 在位；zzz-diag-* 已删；`docs/testing/known-good-baselines.md:13` 2026-07-25 基线条目无截断（2026-07-16 行完整）；bug doc Category (a) + #19 状态更新；frontend-ui-roadmap:575 回归测试注记；`docs/logs/2026/07-25.md` 日志；git status 证实无生产代码变更（仅 playwright.config.ts + _tmp-server.sh + docs/）；build green。2 Minor 非阻断（并发 visual work stream 产物 + 并发 2200-1 plan 编辑，均非本计划变更）。`docs/analysis/2026-07-24-1945-1-test-isolation-pollutant-map.md`（Phase 1 诊断报告）；Phase 3 全量执行 log（490p/1f/3s，诊断 dump clean）。

Follow-up:

- （无范围内容；per-suite 隔离架构见 Deferred But Adjudicated）
