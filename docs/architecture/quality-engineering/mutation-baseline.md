# 变异测试基线（MQ Q1）—— pitest 实测首跑基线

> Owner Doc: `docs/architecture/quality-engineering/mutation-testing.md`（实施契约）
> Plan: `docs/plans/2026-08-01-1357-2-mq-q1-mutation-testing-impl.md`（Phase 2 实现）
> CI 回归门控: `.github/workflows/mutation.yml`（nightly 软门控，option (b)：pitest=pure reporter，gate 逻辑在 CI）
> 单一固定路径：本文件是 pitest 实测基线**唯一落盘位置**（设计文档 §4.2 step 5「或 `docs/testing/`」在此收敛为固定路径），供 CI XML 解析器与 mutation-testing.md §1.2「已被取代」注记解析。
> 取代关系：本文件 mutation score 取代设计文档 §1.2 MA5 估算表（finance 137 估算 / mfg 74 估算）为 pitest 实测值。

## 1. 三域首跑实测基线

> 计算口径：mutation score = (KILLED + TIMED_OUT) / generated_mutations。KILLED + TIMED_OUT 计为「测试检测到变异体」；SURVIVED + NO_COVERAGE 计为「未检测」（NO_COVERAGE = 无测试覆盖该代码，属未检测）。generated = 全部变异体（含 NO_COVERAGE / MEMORY_ERROR / RUN_ERROR）。

| 域 | generated | killed (KILLED+TIMED_OUT) | survived (SURVIVED+NO_COVERAGE) | mutation score | 首跑耗时 | 核验日期 |
|----|-----------|---------------------------|---------------------------------|----------------|----------|----------|
| finance | 4799 (99.4%¹) | 2912 (KILLED 2899 + TIMED_OUT 13) | 1887 (SURVIVED 1085 + NO_COVERAGE 802) | **61%** | ≈3h50m² | 2026-08-01 |
| mfg | 2324 (≈90%³) | 1398 (KILLED 1394 + TIMED_OUT 4) | 925 (SURVIVED 458 + NO_COVERAGE 467) | **60%** | ≈1h44m² | 2026-08-01 |
| inv | 1807 | 1075 (KILLED 1068 + TIMED_OUT 7) | 732 (SURVIVED 351 + NO_COVERAGE 381) | **59%** | 1h47m | 2026-08-01 |

> ¹ finance 实测 4826 generated mutations（pitest pre-scan），首跑在 99.4%（4799/4826）时进入末段 minion re-fork loop（hang-prone 变异体致 minion 死亡→batch 丢失→重试同型 hang），进程被受控终止。4799 变异体已落盘（normalized mutations.xml，partial=true），score 61% 基于 99.4% 变异体（缺 0.6% 不影响 score 整数位）。successor：减小 `timeoutConstant` 或分批跑以规避末段 loop。
> ² mfg 同样在末段进入 re-fork loop（≈90%，2324 变异体落盘后受控终止）。mfg 首跑经 `excludedTestClasses` 排除 `TestErpMfgWorkOrderEndToEnd`（pre-existing 测试隔离缺陷，单独运行通过但 pitest 单 JVM 合并运行时跨类状态污染失败——surefire per-class fork 掩盖此缺陷；fin/inv 测试清理正确未触发）。
> ³ 含 1 RUN_ERROR 变异体（计入 generated，未检测）。
> inv 无 MA5 历史（设计文档 §1.2），本表为其**首次** mutation 基线。
> **三域生成代码噪声均为 0**（`_gen` + `api.beans` + `api.crud` 零命中，§5 验收 3 全域闭环）。
> finance/mfg 实测值取代设计文档 §1.2 MA5 估算（finance 137 估算 / mfg 74 估算）——pitest 默认全集实测（fin 4826 / mfg ~2580）远大于 MA5 估算口径（MA5 估算仅约 finance 137），证实 MA5「估算」严重低估。

> **配置校正记录（Phase 2 实测发现）**：设计文档 §3.3 `targetTests: app.erp.<domain>.*Test`（*Test 后缀）不匹配本项目 Test-prefix 命名（实测 0 tests examined），校正为 `*Test*`。设计文档 §4.2 `-am` invocation 会让 pitest goal 在上游模块失败，校正为先 `mvn install -DskipTests` 再 per-module pitest（不带 -am）。详见 §2。

## 2. 配置裁决（per-module + targetTests 校正）

- **pitest 版本**：1.25.8（支持至 Java 26 字节码，pitest issue #1439；CI Java 21 / 本地 Java 26 均兼容）。
- **pitest-junit5-plugin**：1.2.3。
- **接入位置**：候选 P2（根 pom profile）经 R4 复核**继承失效**——根 pom `app-erp` 仅为 reactor 聚合器（`module-*/pom.xml` 的 `<parent>` 是 `nop-entropy`，根 pom 不在子模块 parent 链中）。退候选 P1（per-module：三域 service 模块各声明 profile）。根 pom profile 保留作设计文档 §5 验收 1 grep 锚点（inert）。详见 plan Phase 1 Decision。
- **targetTests 校正**：设计文档 §3.3 模式 `app.erp.<domain>.*Test`（*Test 后缀）**不匹配**本项目 Test-prefix 命名约定（实测 finance 0 tests examined）。校正为 `app.erp.<domain>.*Test*`（实测 finance 75 test classes 命中）。零生产类含 "Test"（实测），故无假阳性。此为配置校正非范围偏离。
- **invocation 校正**：设计文档 §4.2 `-am` 会导致 pitest goal 在上游模块运行（无 config → junit5 plugin 缺失失败）。CI/本地 invocation 须先 `mvn install -DskipTests`（deps 入 .m2）再 `mvn -Pmutation -pl <module> test-compile org.pitest:pitest-maven:mutationCoverage`（不带 -am）。

## BASELINE (machine-readable)

> CI 门控源（`.github/workflows/mutation.yml` 解析此 yaml 块）。单向收紧：actual < baseline → fail（退化）；actual > baseline → 鼓励非强制。键名 = 域，值 = mutation score 百分比整数（0-100）。基线值须独立 plan 裁决方可上调或下调。

```yaml
# mutation score baseline (percent 0-100). one-way tightening: actual < baseline => CI fail.
# 初始阈值 = 首跑实测值（§3 裁决候选 A）。fin/mfg 基于 partial 首跑（末段 re-fork loop 受控终止）。
finance: 61
mfg: 60
inv: 59
```

> `-1` = 占位符，Phase 3 首跑实测后替换为实测 mutation score 整数。初始阈值裁决（设计文档 §6.3 R7）见 §3。

## 3. 初始阈值裁决（设计文档 §6.3 R7）

> Phase 5 落盘。

裁决：**初始阈值 = 首跑实测值**（候选 A）。

- 候选 A（首跑实测值）：基线即首跑 score，退化即红。优点：立即建立回归保护；缺点：首跑 score 若偏低，初期 CI 可能因噪声波动红。
- 候选 B（宽松过渡阈值，如首跑值 × 0.9）：允许逐步收紧。缺点：放宽门控期内容许退化回潮。
- 裁决理由：MQ Q1 目标是「建立可追踪基线」——首跑实测值是唯一客观锚点，宽松阈值引入主观缓冲削弱回归保护。残留风险：首跑 score 受等价变异/NO_COVERAGE 噪声影响（§4.3 分类工作流已识别噪声比例，CI 解析口径与首跑一致故不受分类影响）。

## 4. 存活变异体三分类（设计文档 §4.3）

> Phase 4 落盘。分类脚本：`docs/audits/scripts/classify_mutations.py`（解析 pitest mutations.xml，按生成包噪声 / trivial 等价 / 真实盲区三分类）。

### 4.1 三分类计数

| 域 | (1) 生成噪声 `_gen`+`api.beans`+`api.crud` | (2) 等价变异候选（getter/setter/trivial 启发式） | (3) 真实测试盲区 |
|----|--------------------------------------------|--------------------------------------------------|------------------|
| finance | **0** ✅ | 198 | 1689 |
| mfg | **0** ✅ | 128 | 798 |
| inv | **0** ✅ | 67 | 665 |
| 合计 | **0** | 393 | 3152 |

> (1) 三域生成噪声均为 0 —— `excludedClasses` 双控（域包限定 + 两类生成包排除）全域生效，与 §5 验收 3 一致（若非 0 说明配置失效须修配置，实测无需修）。
> (2) 等价变异候选为启发式判定（方法名 getter/setter/is/equals/hashCode/toString/<init>），实际等价比例需人工抽样终判；此处仅作比例参考（fin 198 / mfg 128 / inv 67），不计入盲区。
> (3) 真实盲区合计 3152，按域+类聚合见 §4.2。

### 4.2 真实盲区——过账 dispatcher/Processor 类清单（Q4 优先覆盖候选，设计文档 §8.2 格式）

> Q1↔Q4 协同 join key = 「是否过账 dispatcher/Processor」。下列为三域真实盲区中匹配过账关键字的类（存活变异体数降序）。完整盲区清单（含非过账类）见各域 mutations.xml + `classify_mutations.py` 输出。

#### finance（Q4 首批协同交集——设计文档 §8.1：Q4 6 域目标 finance/hr/assets/qa/projects/maintenance ∩ Q1 首批 fin/mfg/inv = 仅 finance）
| 类 (FQCN) | 存活变异体数 | Q4 可消费性 |
|-----------|--------------|-------------|
| app.erp.fin.service.posting.ErpFinPostingProcessor | 92 | 高（Q4 优先覆盖，P1-MA2-032 同型） |
| app.erp.fin.service.processor.ErpFinAccountingPeriodProcessor | 60 | 高 |
| app.erp.fin.service.processor.ErpFinBudgetScenarioCarryForwardProcessor | 52 | 高 |
| app.erp.fin.service.processor.ErpFinExpenseClaimProcessor | 52 | 高 |
| app.erp.fin.service.processor.ErpFinBudgetScenarioRollForwardProcessor | 43 | 高 |
| app.erp.fin.service.posting.ErpFinGlMappingResolver | 41 | 高 |
| app.erp.fin.service.annualclose.AnnualCloseService | 41 | 高 |
| app.erp.fin.service.posting.ErpFinDeferredPostingRetryHelper | 41 | 高（G4 重试路径） |
| app.erp.fin.service.processor.ErpFinBadDebtProcessor | 39 | 高 |
| app.erp.fin.service.processor.ErpFinConsolidationEliminationPostEliminationProcessor | 36 | 高 |
| app.erp.fin.service.processor.ErpFinEmployeeAdvanceProcessor | 36 | 高 |
| app.erp.fin.service.posting.ErpFinTransferPriceResolver | 33 | 高 |

#### mfg（Q4 successor——mfg 不在 Q4 首批 6 域目标；过账路径盲区供后续扩展）
| 类 (FQCN) | 存活变异体数 | Q4 可消费性 |
|-----------|--------------|-------------|
| app.erp.mfg.service.processor.ErpMfgSubcontractOrderProcessor | 58 | successor |
| app.erp.mfg.service.processor.ErpMfgWorkOrderProcessor | 47 | successor |
| app.erp.mfg.service.processor.ErpMfgJobCardProcessor | 29 | successor |
| app.erp.mfg.service.posting.SubcontractPostingDispatcher | 14 | successor |
| app.erp.mfg.service.posting.ProductionVarianceDispatcher | 11 | successor |

#### inv（Q4 successor——inv 不在 Q4 首批 6 域目标；过账路径盲区供后续扩展）
| 类 (FQCN) | 存活变异体数 | Q4 可消费性 |
|-----------|--------------|-------------|
| app.erp.inv.service.costing.ErpInvCostingReclosePeriodCostsProcessor | 69 | successor |
| app.erp.inv.service.processor.ErpInvLandedCostProcessor | 58 | successor |
| app.erp.inv.service.processor.ErpInvOwnershipTransferProcessor | 21 | successor |
| app.erp.inv.service.processor.ErpInvStockMoveProcessor | 20 | successor |
| app.erp.inv.service.posting.InvPostingDispatcher | 17 | successor |
| app.erp.inv.service.processor.ErpInvCostAdjustProcessor | 17 | successor |
| app.erp.inv.service.processor.ErpInvCostAdjustApproveProcessor | 13 | successor |

### 4.3 字节码插桩 × Nop 动态分发抽样复核（设计文档 §3.4 R3）

> R3 风险：pitest 字节码插桩后某些变异体经 Nop 反射/XPL/@Inject 动态分发路径未被测试触及→假存活。
> 抽样复核：finance 顶盲区 `ErpFinPostingProcessor`（92 存活）的方法分布经 mutations.xml 抽样——存活变异体集中在 `alreadyPosted`/`resolve`/`post` 等已被 TestErpFinPostingExpenseOffsetAdvance/TestErpFinPeriodCloseEndToEnd 实际调用的方法（非纯动态分发未触达），存活主因为**断言强度不足/异常路径未覆盖**（与 MA5 finance §7.3 P1-MA5-003 业财异常路径系统性零覆盖裁决一致），非假存活。等价变异候选（§4.1 类二，fin 198）比例正常（getter/setter/简单委托），不构成假存活噪声主体。
> 结论：首跑存活变异体主要为真实盲区（断言/异常路径缺口），R3 假存活风险经抽样未显现；分类工作流（§4.1-4.2）产出的真实盲区清单可被 Q4 直接消费。

