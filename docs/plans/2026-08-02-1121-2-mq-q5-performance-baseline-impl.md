# 2026-08-02-1121-2-mq-q5-performance-baseline-impl 性能基线与回归门控 Phase 2 实现

> Plan Status: active
> Last Reviewed: 2026-08-02
> Mission: audit-remediation
> Work Item: MQ Q5（Phase 2 实现）
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q5（line 678 工作项表 + line 787 维度说明 + 依赖图 line 824 `Q6 --> Q5`）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（Q5 位 6）
> Related: 设计文档 plan `docs/plans/2026-08-01-1121-3-mq-q5-performance-baseline-design-doc.md`（Phase 1 done）；设计文档 `docs/architecture/quality-engineering/performance-baseline.md`（已收敛的实施契约，本计划引用为范围与验收依据）；Q6 plan `docs/plans/2026-08-01-1357-1-mq-q6-clock-test-infrastructure-impl.md`（Q6 done，硬依赖已满足）；sibling Phase 2 plan `2026-08-02-1121-1`（Q2，cron 协调点）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 2 实现**：以经独立子代理 2 轮审查收敛（0 BLOCKER / 0 残留 MAJOR）的设计文档 `performance-baseline.md` 为实施契约。基线盘点引用设计文档 §1（已核验证据，每条带可复现命令），不重推导。基线复核日期：2026-08-02。

**audit-remediation 主线**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 全绿。MQ Q0/Q1/Q3/Q4/Q6 已 done（**Q6 是 Q5 硬依赖，已满足**）。

**Q5 现状（设计文档 §1 已核验，2026-08-02 复核基线仍成立）**：
- 全仓零性能测试基础设施——2026-08-02 复核 `rg "PerfTest|Benchmark|JMH|Gatling" --glob '*.java' --glob '*.xml'` → **STILL ZERO**（基线未漂移）。无 `*PerfTest*`/`*Benchmark*`/JMH/Gatling。
- **Q6 硬依赖已满足**（设计文档 §1.3 + §7，2026-08-02 复核实仓）：`module-common-test/src/main/java/app/erp/common/test/ThreadLocalFrozenClock.java` 落地 + `AbstractFrozenClockExtension` 改写为委托 + **16 域** `*FrozenClockExtension` 子类（15 + assets 补建 `AstFrozenClockExtension`）+ `TestClockRolloverFinance` 跨月模拟 + `clock-rollover.yml` nightly。`ThreadLocalFrozenClock` 仅冻结日期（`currentDate`/`currentDateTime`），`currentTimeMillis`/`nanoTime` 仍委托真实系统时钟——使被测路径**数据确定性**成立（非计时确定性；计时 GC/JIT/IO 噪声须经 warmup + 多轮收敛，设计文档 §3.4 + §4 统一约定）。
- 4 关键路径入口实仓存在（设计文档 §1.4）：凭证过账 `IErpFinVoucherBiz.post` + 期间结账 `ErpFinAccountingPeriodBizModel` + 库存核算 reclose `IErpInvCostingBiz.reclosePeriodCosts`（R6.9 已拆 `ErpInvCostingReclosePeriodCostsProcessor`）+ 报表渲染 `IReportEngine.getHtmlRenderer/getRenderer`。`ErpFinVoucherBillR` 已加 `(billCode, businessType)` 索引（R3.6 `IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE`）。
- **`mvn test` 隔离先例已证可行**（设计文档 §5.1）：既有 `@Tag("full-app")` + 各 `erp-*-web/pom.xml` `<excludedGroups>full-app</excludedGroups>`（2026-08-02 复核命中 web poms）；perf 测试沿用同模式 `@Tag("perf")` + 各 `erp-*-service/pom.xml` `<excludedGroups>perf</excludedGroups>`。父 `nop-entropy/pom.xml` surefire 无全局 groups，隔离配置在 per-module pom。
- 既有 5 CI workflow（2026-08-02 复核）：nightly 槽位 `clock-rollover.yml` `0 3 * * *` / `mutation.yml` `0 4 * * *`；02:00 空闲但被同批 Q2 `security.yml`（plan `2026-08-02-1121-1`）预定。无性能 job。
- 既有 finance/inventory 测试夹具均为**单记录**（设计文档 §5.2 R2-MAJOR-3），无 1000/1 万/5000 规模批量生成器——4 路径批量 seed 生成器须**从零新建**，是本计划主要工作量。

**剩余差距**：无 `PerfTiming` harness；无 4 perf 测试类；无批量 seed 生成器；无 `perf-baselines/` 基线 JSON；无 `perf-baseline.yml` nightly job。

## Goals

> 范围 = 设计文档 §5（Phase 2 实施契约）+ §3/§4/§6 已裁决的 Decision。本计划是设计文档的实施执行，不发明新范围。

- **PerfTiming harness 落地**（设计文档 §5.1）：复用 `module-common-test` 基类，新增 `PerfTiming` 工具类（K=2 untimed warmup + N=10 timed 测量 + 方差比 = (max−min)/median 计算 + 中位数），复用 `JunitAutoTestCase` + `ThreadLocalFrozenClock` + localDb 测试栈。不新建独立 perf module（设计文档 §5.1 裁决 + successor）。
- **4 perf 测试类 + 批量 seed 生成器**（设计文档 §4 + §5.2）：在被测域 `erp-<short>-service/src/test/` 下新建 `Test*Perf.java`（`TestErpFinVoucherPostingPerf` / `TestErpFinPeriodClosePerf` / `TestErpInvCostingReclosePerf` / `TestErpFinReportRenderPerf`），复用各域 frozen clock extension；从零新建批量 seed 生成器（1000 凭证 / 1 万 GL 行 / 5000 移动单×50 物料 / 8 报表），**计时窗口仅包裹被测业务链路，seed-gen 排除在计时窗口外**（设计文档 §4 统一约定）。
- **首次基线落盘**（设计文档 §5.3）：`docs/architecture/quality-engineering/perf-baselines/baseline-{date}.json`（每路径 `{dataScale, warmupRounds, timedRounds:[t1..tN], median, p95, varianceRatio, clockRef, seedProfile}`，对齐设计文档 §5.3 字段含 p95）+ `LATEST.json` 指针。每路径方差比 < §4 复现性阈值（凭证过账/报表渲染 < 15%；期间结账/reclose < 20%）；超阈值登记 §3 路径 C 升级（JMH）候选。
- **CI 软门控接线**（设计文档 §6 裁决路径 C 首期）：新建 `.github/workflows/perf-baseline.yml`（nightly + `workflow_dispatch`）跑 `mvn test -Dgroups=perf` 4 路径 + 记录 `nightly-{date}.json` + 对比 LATEST 计算退化比，**首期非阻塞**（趋势记录）；积累 ≥ N=14 nightly 后晋升路径 B（退化 > X% 开 issue），最终态阻塞门控须团队同意（successor）。
- **`mvn test` 隔离**（设计文档 §5.1）：`@Tag("perf")` + 各被测域 `erp-*-service/pom.xml` `<excludedGroups>perf</excludedGroups>`，perf 测试不进 per-commit `mvn test`。

## Non-Goals

- **不修改 nop-entropy 源码**（设计文档 §2.2：性能测量在应用层 test scope，零平台改动；Phase 2 无须 `nop-entropy/ai-dev/logs/`）。
- **零 ORM / 零业务契约 / 零生产 Java 变更**（perf 测试类是 test scope；不优化既有代码性能——设计文档 §2.2：基线是回归门控，非性能优化任务）。
- **不追求生产级压测**（设计文档 §2.2：无生产数据规模 / 真实负载 / 并发压力模型；仅 CI 可复现回归基线，测的是 H2 localDb 端到端成本，非生产真实成本；生产级压测是 successor）。
- **首期不引入 JMH**（设计文档 §3 裁决路径 B 首选，路径 C 混合作 successor hook；仅当 Phase 2 首基线显示某路径 `@Test` timing 方差比超 §4 阈值才对该路径升级 JMH）。
- **首期不做 per-commit 阻塞门控**（设计文档 §6.2：路径 C 首期非阻塞，晋升阻塞门控须 ≥30 nightly + runner 同构 + 团队同意）。
- **不覆盖 Q2/Q7 等其他维度**（各有独立计划）。

## Task Route

- Type: `implementation-only change`（test-scope perf 测试类 + harness 工具类 + CI workflow + 基线 JSON；零 ORM / 零业务契约 / 零生产 Java 变更）。
- Owner Docs: 设计文档 `docs/architecture/quality-engineering/performance-baseline.md`（收敛实施契约）；Q6 设计文档 `clock-test-infrastructure.md`（时钟硬化产物真相源，测量确定性依赖）；`docs/design/finance/posting.md`+`posting-log.md`（凭证过账路径）+`period-close.md`（期间结账路径）+`costing-methods.md`（reclose 路径）。
- Skill Selection Basis: AGENTS.md 强制技能扫描完成。本工作面为性能测试类编写——复用 `JunitAutoTestCase` localDb 测试栈 + frozen clock extension + 新建 perf 计时辅助 + 批量 seed 生成器，匹配 `nop-testing`（测试基类 / 快照语义 / `@RegisterExtension` / localDb 夹具）。`nop-backend-dev`（不写 BizModel/Processor 生产代码）、`nop-frontend-dev`、`nop-debugging` 不匹配。设计文档 §5 亦明示 Phase 2 加载测试栈。Skill: **nop-testing**。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划改动应用层测试代码（`module-common-test` + finance/inventory `erp-*-service/src/test`）+ 1 个 CI workflow（`.github/workflows/`）+ 基线 JSON。不动端口/密钥/.env/外部服务。
- CI 假设 GitHub-hosted runner（或自建固定 runner）；runner 代际方差经相对退化比容忍——晋升阻塞门控前须确认 nightly runner 同构（设计文档 §6.4 + §6.5 R）。

## Execution Plan

> 阶段顺序对齐设计文档 §5.4 建议实施顺序（harness → 路径 1 → 路径 4 → 路径 3 → 路径 2 → CI）。每路径共用复现性方法（设计文档 §4 统一约定：K=2 untimed warmup + N=10 timed，方差比 = (max−min)/median）+ 计时窗口纪律（seed-gen 排除在计时窗口外）。

### Phase 1 - PerfTiming harness + surefire/profile 隔离

Status: planned
Targets: `module-common-test/src/main/java/app/erp/common/test/PerfTiming.java`（新建）；各被测域 `erp-*-service/pom.xml`（`<excludedGroups>perf</excludedGroups>`）
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: 设计文档审查收敛（已满足）+ Q6 done（已满足）

- [ ] Add: `PerfTiming` 工具类——`measure(Supplier<Void> timed, int warmupK, int timedN)` 返回 `{timedRounds:[t1..tN], median, p95, varianceRatio}`；K 轮 untimed warmup（不计入测量）+ N 轮 timed（`CoreMetrics.nanoTime()` 包裹）+ 方差比 = (max−min)/median + 中位数 + p95（对齐设计文档 §5.3 baseline 字段）。复用 `module-common-test` 既有基类，无新依赖
      - Skill: nop-testing
- [ ] Add: `@Tag("perf")` 约定 + 各被测域 `erp-*-service/pom.xml` 默认 surefire 加 `<excludedGroups>perf</excludedGroups>`（对齐既有 `@Tag("full-app")` + web pom `<excludedGroups>full-app</excludedGroups>` 先例，2026-08-02 复核命中）；perf 测试经 `-Dgroups=perf`（或 `-DexcludedGroups=` 覆盖）激活
      - Skill: nop-testing
- [ ] Proof: `mvn test -pl module-common-test -am -DskipTests` 编译通过 + finance 域 `mvn test`（perf 测试被 `<excludedGroups>` 排除，不进常规 mvn test，零回归）
      - Skill: nop-testing

Exit Criteria:

- [ ] `PerfTiming` 落盘 + 编译通过；perf 测试经 `<excludedGroups>` 不进 per-commit `mvn test`（finance 域常规 `mvn test` 计数不变，零回归）

### Phase 2 - 路径 1 凭证过账首基线

Status: planned
Targets: `module-finance/erp-fin-service/src/test/.../TestErpFinVoucherPostingPerf.java`（新建）
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 1 done

- [ ] Add: 批量 seed 生成器——在 localDb 构造 1000 张已审核业务单据（如采购入库 `ErpPurReceive` approveStatus=APPROVED；单据类型混合采购入库/销售出库/付款凭证各约 1/3，覆盖不同 Provider 路由，混合比例本计划裁决）。**seed 构造在计时窗口外**。须 Q6 `FinFrozenClockExtension` 冻结日期下构造，确保期间/日期数据确定性
      - Skill: nop-testing
- [ ] Add: `TestErpFinVoucherPostingPerf` —— `@Tag("perf")` + `@RegisterExtension static FinFrozenClockExtension`；计时窗口仅包裹 1000 张过账循环（`IErpFinVoucherBiz.post` per voucher，SYNC 默认），首末 nanoTime 差 / 1000 = 单凭证均摊成本；K=2 warmup + N=10 timed
      - Skill: nop-testing
- [ ] Proof: 首基线测量——记录 `{dataScale:1000, rounds, median, varianceRatio}`；方差比 < 15%（设计文档 §4.1 复现性阈值）；超阈值登记 §3 路径 C 升级候选
      - Skill: nop-testing

Exit Criteria:

- [ ] 路径 1 首基线落盘（`perf-baselines/baseline-{date}.json` 含路径 1 段），方差比 < 15%（或登记路径 C 升级候选 + 理由）；finance 域常规 `mvn test` 零回归（perf 被 excluded）

### Phase 3 - 路径 4 报表渲染首基线

Status: planned
Targets: `module-finance/erp-fin-service/src/test/.../TestErpFinReportRenderPerf.java`（新建）
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 1 done（与路径 1 seed 部分复用；独立可并行但建议顺序）

- [ ] Add: 报表 seed——灌注数据集（部分复用路径 1 过账数据），计时窗口仅包裹单报表 `IReportEngine.getHtmlRenderer/getRenderer` 调用；K=8 份种子报表（覆盖多域，每域 1-2 份代表性），每报表独立计时避免聚合掩盖单报表异常
      - Skill: nop-testing
- [ ] Add: `TestErpFinReportRenderPerf` —— `@Tag("perf")` + frozen clock；K=2 warmup + N=10 timed 每报表
      - Skill: nop-testing
- [ ] Proof: 首基线测量——方差比 < 15%（设计文档 §4.4）
      - Skill: nop-testing

Exit Criteria:

- [ ] 路径 4 首基线落盘（perf-baselines 追加路径 4 段），方差比 < 15%

### Phase 4 - 路径 3 库存核算 reclose 首基线（路径 C 升级候选）

Status: planned
Targets: `module-inventory/erp-inv-service/src/test/.../TestErpInvCostingReclosePerf.java`（新建）
Skill: nop-testing

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 done

- [ ] Add: FIFO 移动单 seed——构造 M=50 物料的 N=5000 条入库/出库移动单（DONE 状态，含正常路径 + 少量成本层缺失/COGS 异常以触发 reclose 补算非零路径）。**seed 构造在计时窗口外**；每轮测量前须重置成本层到 reclose 前状态（重建 seed 或回滚 reclose 写入）。须 `InvFrozenClockExtension` 冻结
      - Skill: nop-testing
- [ ] Add: `TestErpInvCostingReclosePerf` —— `@Tag("perf")` + frozen clock；计时窗口仅包裹 `IErpInvCostingBiz.reclosePeriodCosts(periodId, startDate, endDate)`；K=2 warmup + N=10 timed
      - Skill: nop-testing
- [ ] Proof: 首基线测量——方差比 < 20%（设计文档 §4.3）；**本路径偏计算，是 §3 路径 C 升级（JMH）最可能候选**——若方差比 > 20%，登记路径 C 升级候选 + 理由
      - Skill: nop-testing
- [ ] Decision: 路径 C 升级判定——若路径 3 方差比超 §4 阈值，裁决是否本计划内升级该路径为 JMH（设计文档 §3.4 successor hook）。**首期裁决=登记 successor**（JMH 升级是独立后续，本计划交付 `@Test` timing 基线 + 升级触发判定证据）。理由记录
      - Skill: nop-testing

Exit Criteria:

- [ ] 路径 3 首基线落盘（perf-baselines 追加路径 3 段），方差比 < 20% 或登记路径 C 升级候选 + 触发证据

### Phase 5 - 路径 2 期间结账首基线

Status: planned
Targets: `module-finance/erp-fin-service/src/test/.../TestErpFinPeriodClosePerf.java`（新建）
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 2 done（依赖路径 1 seed 累积到 ≥1 万 GL 行）

- [ ] Add: GL 行 seed——经路径 1 过账 harness 灌注足够凭证累积到 ≥1 万 GL 行（或直接批量插 GL 行 seed）；须确保期间所有单据 posted=true（结账前置检查不阻断）；**每轮测量前须重置期间到可结账状态**（反结账或重建 seed，否则后续轮因已 CLOSED 无法重测）。seed 构造在计时窗口外
      - Skill: nop-testing
- [ ] Add: `TestErpFinPeriodClosePerf` —— `@Tag("perf")` + frozen clock；计时窗口仅包裹 `ErpFinAccountingPeriodBizModel` 结账链路（前置检查 → AR/AP/INV/AST/GL 按序关账 → 损益结转 → 试算平衡）；K=2 warmup + N=10 timed
      - Skill: nop-testing
- [ ] Proof: 首基线测量——方差比 < 20%（设计文档 §4.2）
      - Skill: nop-testing

Exit Criteria:

- [ ] 路径 2 首基线落盘（perf-baselines 追加路径 2 段），方差比 < 20%；4 路径基线齐全 + `LATEST.json` 指针落盘

### Phase 6 - CI perf-baseline.yml nightly 接线 + 软门控

Status: planned
Targets: `.github/workflows/perf-baseline.yml`（新建）；`docs/architecture/quality-engineering/perf-baselines/LATEST.json`
Skill: none

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2-5 首基线落盘

- [ ] Decision: nightly cron 槽位裁决（**跨计划协调点**）——设计文档 §6.3 提议 `0 2 * * *`，但该槽位已被同批 Q2 `security.yml`（plan `2026-08-02-1121-1`，设计文档 §8.4）预定。既有 nightly：clock-rollover 03:00 / mutation 04:00 / security 02:00（Q2）。**本计划裁决 `perf-baseline.yml` cron 为 `0 5 * * *`**（05:00，mutation 04:00 之后，零重叠）。裁决理由：perf 测量长耗时（预估单次 >10 分钟，4 路径 × 多轮），低峰 05:00 避免与既有 nightly 争抢 runner。设计文档 §6.4「02:00 vs clock-rollover 03:00 错开」核验先于 Q2 落地，本裁决为跨计划协调更新
      - Skill: none
- [ ] Add: 新建 `.github/workflows/perf-baseline.yml`（设计文档 §6.3）
      - `on: schedule: cron: '0 5 * * *'`（nightly，本计划裁决非设计文档 02:00）+ `workflow_dispatch`
      - job：checkout → setup JDK 21 → `mvn test -Dgroups=perf`（激活 §5.1 surefire 隔离的 perf 测试，4 路径 × N 轮）→ 生成 `perf-baselines/nightly-{date}.json` → 对比 `LATEST.json` 计算每路径退化比 → **首期非阻塞**（趋势记录 + 摘要，不阻断合并）；退化 > §6.2 X%（建议 20%，相对最近 N=14 nightly 中位数）开 issue
      - 基线更新须显式触发（commit 新 `baseline-{date}.json` + 更新 LATEST 指针），nightly 不自动覆盖基线（防退化悄悄写进基线，设计文档 §6.5）
      - Skill: none
- [ ] Proof: 与既有 5 CI job + Q2 security.yml 零冲突——perf 测试经 `<excludedGroups>` 不进 per-commit `maven.yml`（零冲突）；nightly cron `0 5` 与既有（`0 2` security / `0 3` clock-rollover / `0 4` mutation）零重叠；既有 5 workflow git diff 为空
      - Skill: none

Exit Criteria:

- [ ] `perf-baseline.yml` 落盘（yaml.safe_load 合法 + cron `0 5` + workflow_dispatch + 非阻塞趋势记录）；既有 5 workflow git diff 为空 + cron 零重叠；`LATEST.json` 指针落盘

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_0417a00f0ffeHqaZafe6Lbi6B6`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 3 MINOR。忠实实施收敛设计文档（`performance-baseline.md`）零 scope 漂移——path B @Test timing（非 JMH 首选）+ harness 在 module-common-test（非新 module）+ path C 首期非阻塞 + K=2/N=10 + 方差比=(max−min)/median + 计时窗口纪律（seed-gen 排除在计时窗口外）逐阶段贯穿。**关键跨计划协调点（cron 02:00→05:00，因 sibling Q2 占 02:00）正确记录为 Phase 6 Decision + 理由 + 在「实现与设计文档一致」gate 显式标注**，且 05:00 经核验与全部既有/计划 nightly 槽位（02:00 security / 03:00 clock-rollover / 04:00 mutation）零重叠。全部 Current Baseline 主张实仓复核 PASS（零 perf infra / Q6 done 16 frozen-clock extensions / full-app excludedGroups 先例在 web poms / 4 路径入口存在 / nightly crons 如述）。Phase 顺序精确对齐设计文档 §5.4。successor（JMH 升级 / 生产级压测 / per-commit 阻塞门控 / 独立 perf module）全部正确命名 Deferred 带触发条件。MINOR 采纳：M1 baseline JSON 加 p95（对齐设计文档 §5.3 字段，调和设计文档 §8 内部不一致）；M3 Phase 1 phase-level Item Types 补 `Proof`。M2（K/N 记号重载）保留——继承自收敛设计文档（§4.3/§4.4 同记号），改记号会偏离设计文档契约。0 BLOCKER/MAJOR → converged → 转 active。

## Closure Gates

> 设计文档 §8（6 条验收判据）为本计划 closure 契约。全量 `mvn clean install -DskipTests` + `mvn test` 在此一次性运行（执行时规则 7）。

- [ ] 范围内行为完成（设计文档 §8 验收 1-6）
  - 4 关键路径均有可复现基线（每路径 K=2 warmup + N=10 timed，方差比 < §4 阈值；超阈值登记路径 C 升级候选）✓
  - CI 软门控在退化 > 阈值时告警（nightly 对比 LATEST + 开 issue）✓
  - 基线数据落盘可追溯（`perf-baselines/baseline-{date}.json` 含每路径完整字段，可机器读取）✓
  - Q6 数据确定性支撑可复现（所有 perf 测试在各域 frozen clock extension 下运行）✓
  - 与既有 5 CI job + Q2 security.yml 零冲突（perf 经 `<excludedGroups>` 不进 per-commit + nightly cron 零重叠）✓
  - 路径 C 升级触发判定（路径 3 或任一路径方差超阈值登记）✓
- [ ] 相关文档对齐：`perf-baselines/` 基线 JSON + `LATEST.json` 落盘 + `docs/logs/2026/08-02.md` 追加日志条目；roadmap Q5 工作项状态在 closure 时回填
- [ ] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（0 failures/0 errors，perf 测试被 excluded 不计入常规 mvn test 计数）；`mvn test -Dgroups=perf` 4 路径首基线测量产出
- [ ] 无范围内项目降级为 deferred/follow-up（JMH 升级 / 生产级压测 / per-commit 阻塞门控经设计文档 §9 + 各 Phase Decision 显式 out-of-scope 为 successor，非范围内项目）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中
- [ ] **实现与设计文档一致**（无未经 `performance-baseline.md` 批准的范围偏离；**cron 槽位 `0 5`（非设计文档 02:00）为跨计划协调更新**，已在本计划 Phase 6 Decision 显式记录理由 + 回填依据；任何实施期发现回填设计文档 Review Record 而非静默偏离——尤其各路径首基线方差比实测结果）

## Deferred But Adjudicated

### JMH 升级（路径 C）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计文档 §3 裁决路径 B（`@Test` timing）首选，路径 C（JMH）作 successor hook。仅当 Phase 4 首基线显示某路径（最可能路径 3 reclose）方差比超 §4 阈值（GC/JIT 噪声主导）才升级该路径。
- Successor Required: yes —— 触发条件：本计划 Phase 4 首基线方差比实测超阈值，登记升级候选 + 触发证据后开独立 JMH 升级计划（仅升级超阈路径，非全路径）。

### 生产级压测

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计文档 §2.2 明确非目标（仅 CI 可复现回归基线，测 H2 localDb 非生产真实成本）。
- Successor Required: yes —— 触发条件：首次生产部署 + 真实负载数据可用。

### per-commit 阻塞门控

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计文档 §6.2 裁决路径 C 首期非阻塞；晋升阻塞门控须积累数据 + 团队同意。
- Successor Required: yes —— 触发条件：nightly 累积 ≥30 测量 + runner 同构确认 + 团队明确同意（避免误阻断）。

### 独立 perf module

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计文档 §5.1 裁决复用 `module-common-test` + 各域 test 源码；独立模块 successor 仅当规模膨胀。
- Successor Required: yes —— 触发条件：perf 测试规模膨胀（>10 域 / >20 测试类）。

## Closure

Status Note: （独立结束审计通过后填写）

Closure Audit Evidence:

（独立结束审计子代理证据——fresh session）

Follow-up:

- JMH 升级（路径 C）/ 生产级压测 / per-commit 阻塞门控 / 独立 perf module successor（见上 Deferred）。
- Q2 Phase 2 / Q7 Phase 1 各有独立计划（同批 `2026-08-02-1121-1` / `-3`）。
