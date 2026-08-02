# 2026-08-02-1500-2 mq-q1-mutation-baseline-completeness Q1 变异测试基线完整性收尾（mfg 测试隔离修复，移除 excludedTestClasses 星号）

> Plan Status: active
> Last Reviewed: 2026-08-02
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q1 successor（line 674「mfg 测试隔离修复后移除 excludedTestClasses」）+ `docs/architecture/quality-engineering/mutation-testing.md` §7 successor 表「mfg 测试隔离缺陷（excludedTestClasses workaround）」
> Related: `docs/plans/2026-08-01-1357-2-mq-q1-mutation-testing-impl.md`（Q1 Phase 2 首跑）、`docs/plans/2026-08-01-1158-2-mq-q1-mutation-testing-design-doc.md`（Phase 1 设计文档）
> Audit: required

## Current Baseline

- **Q1 Phase 2 done**：pitest 1.25.8 per-module 接入三域 service pom（fin/mfg/inv 各 `<profile><id>mutation</id>`）；首跑基线落盘 `docs/architecture/quality-engineering/mutation-baseline.md`——finance **61%**（generated 4799，99.4% 覆盖）/ mfg **60%**（generated 2324，≈90% 覆盖）/ inv **59%**（generated 1807，100% 完整）。存活变异体三分类落盘（真实盲区 3152，过账 dispatcher/Processor 清单供 Q4 消费）。
- **mfg 基线带 excludedTestClasses 星号**：`module-manufacturing/erp-mfg-service/pom.xml:211-219` 声明 `<excludedTestClasses><param>*TestErpMfgWorkOrderEndToEnd*</param></excludedTestClasses>`。pom 注记（line 211-215）：`TestErpMfgWorkOrderEndToEnd` 单独运行通过（surefire per-class fork 隔离），但 pitest 单 JVM 合并运行（全 32 mfg test class 共享 JVM/DB）时因**跨类状态污染**失败（非变异导致的失败 → pitest 要求 green suite → 阻断）。mfg 基线 60% 基于 **31/32 test class**（缺 `TestErpMfgWorkOrderEndToEnd` 的杀变异体能力）。
  - **根因待查**：`TestErpMfgWorkOrderEndToEnd extends JunitAutoTestCase`（`@NopTestConfig(localDb = true)`），创建大量实体（material/BOM/work order/material issue/job card 等多处 `dao.saveEntity`）。跨类污染的可能机制：(a) localDb 状态未在类边界完全重置；(b) JunitAutoTestCase 静态/线程局部状态泄漏；(c) 共享种子数据被修改未回滚。fin/inv 测试清理正确未触发此问题（pom 注记明示）。
- **fin/mfg 基线带 partial 星号（本计划不处理，归 watch-only successor）**：mutation-baseline.md §1 注记 ¹²——finance 首跑在 99.4% 进入末段 minion re-fork loop（hang-prone 变异体致 minion 死亡），mfg 同样在 ≈90%。**score 整数位稳定**（fin 61/mfg 60 不受末段 0.6%/10% 缺失影响），nightly 门控以 score 整数位为基准不受 partial 影响。该星号经草案审查裁决为 watch-only successor（见 Deferred But Adjudicated），不在本计划范围——其调优（`mutationUnitSize=1` / `timeoutConstant` / 分批）无可证伪的即时收益（score 不变 + 门控不松），仅在 nightly 出现 variant-count 不稳定噪声时才需触发。
- **CI 门控已激活**：`.github/workflows/mutation.yml` nightly 软门控（单向收紧：actual < baseline → fail），基线 fin 61/mfg 60/inv 59。
- **剩余差距**：mfg 基线缺 1 test class 覆盖（excludedTestClasses 星号）——这是 Q1 Phase 2 显式登记的 deferred successor（mutation-testing.md §7 + Q1 impl plan §Deferred But Adjudicated「mfg 测试隔离缺陷」），**触发条件已满足**（"mfg 测试隔离修复后，移除 excludedTestClasses"）。

## Goals

- **消除 mfg 基线 excludedTestClasses 星号**：调查并修复 `TestErpMfgWorkOrderEndToEnd` 的跨类状态污染根因，使 pitest 单 JVM 合并运行时全 32 mfg test class 通过（green suite），移除 `<excludedTestClasses>` workaround，重跑 mfg 基线含全 32 class。

## Non-Goals

- **不处理 fin/mfg 末段 re-fork loop partial 星号**——经草案审查裁决为 watch-only successor（见 Deferred But Adjudicated）。score 整数位稳定（fin 61/mfg 60）且 nightly 门控以 score 为基准，partial 星号无可证伪的即时收益；仅在 nightly 出现 variant-count 不稳定噪声时触发调优。
- **不扩展至其余 16 域 mutation 基线**——本计划仅收尾 mfg 基线完整性。16 域扩展（含 hr 0.16）是独立 successor（mutation-testing.md §7），触发=三域基线落盘 + 分类工作流沉淀（已满足），但扩展规模大，归独立计划。
- **不修复真实测试盲区（3152 存活变异体）**——Q1 仅产出盲区清单，修复属 Q4 故障注入覆盖（过账路径）+ 后续 MR3-style 测试补强（非过账路径）。本计划不改测试断言强度，仅修测试隔离 + pitest param。
- **不修改 `mutation-baseline.md` BASELINE 块的 score 值**（fin 61/mfg 60/inv 59）——移除星号不改 score 整数位（设计文档 §6.3 R7 + mutation-baseline.md §1 注记明示）。若 mfg 全 32 class 重跑后 score 整数位变化，须独立基线裁决（对齐 compliance-baseline 调高机制）。
- **不修改 nop-entropy**——零平台改动（mfg 测试隔离缺陷是应用层测试代码问题）。

## Task Route

- Type: `bug investigation + implementation-only change`（测试隔离缺陷调查修复 + pitest 配置调优，非契约/模型变更）
- Owner Docs: `docs/architecture/quality-engineering/mutation-testing.md`（§4.3 分类工作流 + §7 successor 表）、`docs/architecture/quality-engineering/mutation-baseline.md`（§1 星号注记 + §2 配置裁决）
- Skill Selection Basis: `nop-testing`（测试隔离修复触及 JunitAutoTestCase/localDb 机制 + pitest 单 JVM 合并运行语义，须遵循测试隔离纪律 + 快照录制回放机制）；`nop-debugging`（跨类状态污染根因调查）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. pitest profile 已接入三域 service pom。mfg 测试隔离缺陷是 pre-existing 应用层测试债务，修复零新依赖。

## Execution Plan

### Phase 1 - mfg 测试隔离根因调查 + 修复（移除 excludedTestClasses 星号）

Status: planned
Targets: `module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgWorkOrderEndToEnd.java`、`module-manufacturing/erp-mfg-service/pom.xml`
Skill: `nop-debugging`

- Item Types: `Fix | Decision | Proof | Explore（pre-Decision 临时项，guide Rule 9）`
- Prereqs: Q1 Phase 2 done（pitest 接入 + 首跑基线已落盘）

- [ ] `Explore`：定位 `TestErpMfgWorkOrderEndToEnd` 跨类状态污染根因——在 pitest 单 JVM 合并运行模式下（`mvn -Pmutation -pl module-manufacturing/erp-mfg-service test-compile org.pitest:pitest-maven:mutationCoverage`，临时移除 excludedTestClasses 复现失败），定位哪条测试 / 哪个共享状态（localDb 种子数据 / JunitAutoTestCase 静态状态 / 未回滚的 `dao.saveEntity`）在跨类边界泄漏。记录根因于 plan。
  - Skill: `nop-debugging`
- [ ] `Decision`：根据根因裁决修复路径——(a) 补 `@AfterEach`/cleanup 清理泄漏状态（若根因是未清理的实体）；(b) 调整 `@NopTestConfig` 隔离边界（若根因是 localDb 类边界重置缺失）；(c) 若根因是 JunitAutoTestCase 框架级限制（平台侧），登记为平台 successor + 保留 excludedTestClasses 但记录精确理由于 pom 注记（降级为 adjudicated residual）。记录选择、替代方案、残留风险。
  - Skill: `nop-testing`
- [ ] `Fix`：按裁决落地修复（若 (a)/(b) 路径），使 pitest 单 JVM 合并运行全 32 mfg test class 通过 green suite
  - Skill: `nop-testing`
- [ ] `Fix`：移除 `erp-mfg-service/pom.xml:217-219` 的 `<excludedTestClasses>` workaround（保留 pom 注记更新为「已修复，excludedTestClasses 移除」）
  - Skill: none
- [ ] `Proof`：重跑 mfg pitest 全 32 class 基线（`mvn -Pmutation -pl module-manufacturing/erp-mfg-service test-compile org.pitest:pitest-maven:mutationCoverage`），核验：(a) green suite（无 excludedTestClasses 排除，全 32 class 运行）；(b) 新 mfg score 落盘——若整数位变化（≠60），触发基线裁决（更新 mutation-baseline.md BASELINE 块须经独立裁决，对齐单向收紧）；若整数位不变（仍 60），仅更新 partial 星号注记
  - Skill: `nop-testing`

Exit Criteria:

- [ ] mfg pitest 单 JVM 合并运行全 32 test class 通过 green suite（excludedTestClasses 已移除），或根因裁决为平台级限制并降级为 adjudicated residual（pom 注记含精确理由）
- [ ] mfg 基线重跑落盘（score 整数位记录 + excluded 星号状态更新）

## Draft Review Record

- Independent draft review iteration 1: `ses_03fd0cb5fffe4epUFHA6T7KRG9`（独立子代理 fresh session cold context）— **needs-revision**，0 BLOCKER / 1 MAJOR / 3 MINOR。全部 Current Baseline 事实经 live-repo 核验 PASS（excludedTestClasses 配置 / mutation-baseline.md 星号注记 / TestErpMfgWorkOrderEndToEnd JunitAutoTestCase + localDb + 多处 saveEntity / fin·inv 无 excludedTestClasses / mutation.yml nightly 门控）。
  - MAJOR-1：Phase 2（fin/mfg re-fork loop）动机薄弱——计划自认 score 整数位不变 + 门控不松 + residual 逃生口预先写定，属「pre-baked residual」反松弛风险。修订：**移除 Phase 2，降级为 Deferred But Adjudicated watch-only successor**（触发条件=nightly 门控出现 variant-count 不稳定噪声），Phase 1（mfg 隔离修复）单独闭合本计划。
  - MINOR-1：Phase 1 Item Types 未列 Explore → 已修订补 Explore（Rule 9 临时项）。
  - MINOR-2：dao.saveEntity 行范围 253-359 不准 → 已改为泛指「多处」。
  - MINOR-3：Closure Gates 测试计数 1903 可能过时（sibling Q-plan 闭包记录 1920-1929）→ 已改为「既有 mvn test 基线（0 failures/0 errors）」。
- Independent draft review iteration 2: `ses_03fcc9e7bffePbp7TQbbFUmPTL`（独立子代理 fresh session cold context，不同于 iter1）— **accept**，0 BLOCKER / 0 MAJOR / 1 optional MINOR。MAJOR-1 修订经核验 fully resolved（Phase 2 完全移除、re-fork loop 降级 Deferred But Adjudicated watch-only successor 含可证伪触发条件、title/Goals/Non-Goals/Closure Gates 单阶段一致性 PASS）。iter1 三项 MINOR 全部 resolved（Explore 补入 Item Types / dao.saveEntity 行范围改泛指 / 测试计数 1903 改泛指）。Rule 1 live-repo spot-check PASS（excludedTestClasses 块 line 211-219 + TestErpMfgWorkOrderEndToEnd JunitAutoTestCase/localDb/saveEntity 核实）。optional MINOR：closure 时可顺带回填 parent Q1 impl plan 的 Deferred「mfg 测试隔离缺陷」登记条目（非阻塞）。**收敛，转入 active。**

## Closure Gates

- [ ] 范围内行为完成（mfg excludedTestClasses 星号消解或根因裁决为平台级 adjudicated residual）
- [ ] 相关文档对齐（mutation-baseline.md excluded 星号注记更新 + mutation-testing.md §7 mfg 隔离 successor 状态回填）
- [ ] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS，pitest profile 默认不激活不影响）+ `mvn test`（0 failures/0 errors，测试隔离修复不破坏既有基线）+ `bash docs/audits/nop-compliance-checker.sh`（无新增命中）+ mfg pitest 重跑基线落盘（全 32 class green suite）
- [ ] 无范围内项目降级为 deferred/follow-up（mfg 隔离缺陷是已确认缺陷须修，不可静默降级——除非根因裁决为平台级限制并记录精确理由，此时降级为 adjudicated residual 是合规的）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### fin/mfg 末段 re-fork loop partial 星号（草案审查 MAJOR-1 降级）

- Classification: `watch-only successor`
- Why Not Blocking Closure: mutation-baseline.md §1 注记 ¹²——fin 首跑 99.4% / mfg ≈90% 因末段 hang-prone 变异体致 minion 死亡受控终止，基线标记 partial。**score 整数位稳定**（fin 61/mfg 60 不受末段缺失影响），nightly 门控（mutation.yml）以 score 整数位为基准，partial 星号不松门控、不改可证伪结论。草案审查（ses_03fd0cb5...）裁决：调优尝试（`mutationUnitSize=1` / `timeoutConstant` / 分批）无可证伪的即时收益（score 不变 + 门控不松），仅在 nightly 出现 variant-count 不稳定噪声（partial 基线每次重跑生成不同变异体子集→门控可能因噪声而非真实退化失败）时才需触发。
- Successor Required: yes — 触发条件：nightly mutation 门控出现 variant-count 不稳定噪声（partial 基线重跑子集漂移致假阳性），或外部审计要求完整（100% 变异体落盘）基线。

### 16 域 mutation 基线扩展（含 hr 0.16）

- Classification: `optimization candidate`
- Why Not Blocking Closure: mutation-testing.md §7 + §1.4——三域首跑聚焦高复杂度域，16 域扩展触发条件已满足（三域基线落盘 + 分类工作流沉淀）但规模大，归独立计划。hr 0.16 低比根因已由 MA5 裁决为「真实异常路径缺口」，其修复路径是补异常路径测试（MA4/MR3 范围）而非先跑 pitest。
- Successor Required: yes — 触发条件：独立计划按域分批扩展（hr 优先，取决于其异常路径测试补强进度）。

### C-3 per-commit 增量变异测试

- Classification: `watch-only successor`
- Why Not Blocking Closure: mutation-testing.md §6.3 + §7——增量模式复杂性 + 首跑基线建立后增量有参照。本计划收尾三域基线完整性，为进一步增量模式提供更可靠参照。
- Successor Required: yes — 触发条件：三域基线稳定 + 团队 PR 节奏需更快反馈时。

## Closure

Status Note: 待执行 + 独立结束审计。

Closure Audit Evidence:

- Auditor / Agent: pending（独立子代理 fresh session）
- Evidence: pending
