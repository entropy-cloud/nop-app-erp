# 2026-08-01-1357-1-mq-q6-clock-test-infrastructure-impl 时钟测试基础设施硬化 Phase 2 实现

> Plan Status: completed
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q6（Phase 2 实现）
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q6（line 679 工作项表 + line 789 维度说明）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（Q6 位 1）
> Related: 设计文档 plan `docs/plans/2026-08-01-1158-1-mq-q6-clock-test-infrastructure-design-doc.md`（Phase 1 done）；设计文档 `docs/architecture/quality-engineering/clock-test-infrastructure.md`（已收敛的实施契约，本计划引用为范围与验收依据）；bug `docs/bugs/2026-08-01-finance-period-resolution-month-rollover.md`；plan `2026-08-01-0803-1`（R6.9 单点硬化，Q6 是其系统性根治超集）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 2 实现**：以经独立子代理 3 轮审查收敛的设计文档 `clock-test-infrastructure.md` 为实施契约。基线盘点引用设计文档 §1（已核验证据，每条带可复现命令），不重推导。

**audit-remediation 主线**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 1903 测试 0 failures/0 errors（`docs/testing/known-good-baselines.md` 2026-07-31 行）。

**Q6 现状（Phase 1 已诊断根因，设计文档 §1）**：
- 时钟入口为平台 API `CoreMetrics.today()/currentDate()/currentDateTime()`，委托进程级全局静态 `s_clock`（`CoreMetrics.java:42`，**private 无 getter**）；过账链路生产代码已合规（`NotesPostingDispatcher:79,108` 用 `CoreMetrics.today()`，非直调 `LocalDate.now()`）。
- 测试冻结机制 `module-common-test/.../AbstractFrozenClockExtension.java:62-68` 经 `CoreMetrics.registerClock(...)` **整体替换全局静态槽** → 并行不安全（latent 结构性缺陷，当前 surefire `threadCount=1` 使污染未显性爆发，见 `../nop-entropy/pom.xml:209-233`）。
- **15 域 `*FrozenClockExtension` 子类各携 `installFrozenClock()`/`restoreSystemClock()` 静态旁路**（每域 line 20-31 直接 `registerClock`，构成第二条全局替换路径；核验 `rg -n "public static void installFrozenClock" --glob '*.java'` 命中 15 文件），HR 活跃调用点 `TestErpHrPayrollSimulation:444,477`。
- **月初翻车税（活跃 CI 红，唯一反复痛点）**：每月月初 finance 域 4 类由绿转红（1 failure + 10 errors）：`TestErpFinBadDebtReversal`(3)/`TestErpFinEmployeeAdvanceCashRepayReversal`(3)/`TestErpFinNotesPayableStateMachine`(4)/`TestErpFinDashboard`(1)。失败形似过账悬挂回归，stash 隔离实验证伪（bug doc line 24-26）。
- R6.9 已对 5 处做**单点**硬化（NotesPayable/Dashboard 相对 seed + `checkOutput=false`），**未根治**全局静态时钟根因。

**剩余差距**：无 `ThreadLocalFrozenClock`；15 域子类静态旁路仍在全局替换；4 finance 失败类中 2 类（BadDebt/EmployeeAdvance）未注册冻结扩展（直接原因）；无跨月回归 CI 层；无并行不污染的客观证明。

## Goals

> 范围 = 设计文档 §3.4 裁决的**路径 C**（应用层 thread-local delegating clock）+ §6 裁决的 **CI C-2**（clock-rollover nightly）。本计划是设计文档的实施执行，不发明新范围。

- **根因根治（设计文档 §4.1）**：新建 `ThreadLocalFrozenClock`（thread-local delegating clock，per-fork 静态初始化挂载）+ 改写 `AbstractFrozenClockExtension` beforeAll/afterAll + 删除死代码 `ICClock` 内部类。
- **15 域子类静态旁路迁移（设计文档 §4.1 step 3）**：每子类 `installFrozenClock()`/`restoreSystemClock()` 重实现为线程本地委托 + HR `TestErpHrPayrollSimulation:444,477` 调用点迁移（per-method 语义保留）。
- **4 模块审计（设计文档 §4.1 step 4）**：assets（预标记高概率候选，10+ posting dispatcher 读 `CoreMetrics.today()`）补建 `AstFrozenClockExtension`；master-data/aps/notify 轻量审计，有则补、无则记「无」。
- **finance 月初税 4 类（设计文档 §4.2）**：A 组 2 类补 `@RegisterExtension` 接线；B 组 2 类复核 `checkOutput` 回收；产出跨月模拟运行时证据。
- **CI C-2 接线（设计文档 §6.4）**：新建 `.github/workflows/clock-rollover.yml`（nightly `faketime` 跨月 + workflow_dispatch）。

## Non-Goals

- **不修改 nop-entropy 源码**（设计文档 §4.3 边界：路径 C 仅用既存 `registerClock` API 一次性挂载 delegating clock，非改造平台；Phase 2 无须 `nop-entropy/ai-dev/logs/`）。
- **不重录 finance 测试快照**（bug doc line 40：按新月度重录只是把时间炸弹推到下个月，非修复）。
- **不动生产过账链路时钟入口**（设计文档 §1.1：已合规）；2 处非过账链路生产 `LocalDate.now()` 残留（`ErpFinVoucherTemplateRenderTemplateProcessor:75` / `ErpMdCurrencyRefreshRatesFromApiProcessor`）登记 successor，不在本期。
- **不引入路径 C 之外的修复**（路径 A 平台改造、路径 B 纯参数化均经设计文档 §3.4 否决）。
- **不覆盖 Q1/Q4 等其他维度**（各有独立 Phase 2 plan）。
- **不切 surefire `parallel=methods`/`threadCount>1`**（设计文档 §1.3：并行不安全是 latent；路径 C 使未来可并行化，但本期不改变 surefire 配置，并行根治由本计划验收 1 的 `TestThreadLocalFrozenClockParallel` 客观证明）。

## Task Route

- Type: `implementation-only change`（测试基础设施实现 + CI workflow；零 ORM/契约/生产过账链路变更）
- Owner Docs: 设计文档 `docs/architecture/quality-engineering/clock-test-infrastructure.md`（收敛实施契约）；bug `docs/bugs/2026-08-01-finance-period-resolution-month-rollover.md`；`docs/architecture/quality-engineering/README.md`
- Skill Selection Basis: AGENTS.md 强制技能扫描完成。工作面向测试基础设施（JUnit 5 `@RegisterExtension`、`IClock`、`JunitAutoTestCase` 兼容性、快照语义），匹配 `nop-testing`（测试基类/快照/扩展）。`nop-backend-dev`/`nop-frontend-dev`/`nop-debugging` 不匹配。设计文档 §4 亦明示「Phase 2 起草加载 nop-testing skill」。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划改动应用层测试代码（`module-common-test` + 各域 `src/test`）+ 1 个 CI workflow（`.github/workflows/`）。不动端口/密钥/.env/外部服务。
- CI C-2 依赖 GitHub Actions runner 可装 `faketime`（`sudo apt-get install faketime`）——设计文档 §6.3 R5 记残留风险（与 JVM 时区兼容性 Phase 2 实测确认）。

## Execution Plan

### Phase 1 - 核心 thread-local delegating clock（根治并行根因）

Status: completed
Targets: `module-common-test/src/main/java/app/erp/common/test/ThreadLocalFrozenClock.java`（新建）；`module-common-test/src/main/java/app/erp/common/test/AbstractFrozenClockExtension.java`（改写）
Skill: nop-testing

- Item Types: `Add | Fix`
- Prereqs: 设计文档审查收敛（已满足）

- [x] Add: 新建 `ThreadLocalFrozenClock implements IClock`
      - `static ThreadLocal<LocalDate> REF_DATE`（默认 unset）；`static volatile boolean INSTALLED = false`（应用层幂等标志——平台无 `s_clock` 内省 API，设计文档 §1.1）
      - `currentTimeMillis()/nanoTime()` → 委托 `CoreMetrics.defaultClock()`（保单调时间真实）
      - `currentDate()/currentDateTime()` → `REF_DATE` 有值返回冻结日期，否则委托 `defaultClock()`
      - `install(LocalDate)` / `clear()` / `isInstalled()` 线程本地静态方法
      - `ensureRegistered()`：`if (!INSTALLED) { CoreMetrics.registerClock(new ThreadLocalFrozenClock()); INSTALLED = true; }`
      - per-fork 注册载体：`static {}` 静态初始化块调 `ensureRegistered()`（类加载即触发，早于任何 beforeAll；`forkCount=1C`+`reuseForks=true` → per-fork JVM，设计文档 §3.3 M2 时序）
      - Skill: nop-testing
- [x] Fix: 改写 `AbstractFrozenClockExtension`
      - `beforeAll`：`ThreadLocalFrozenClock.ensureRegistered(); ThreadLocalFrozenClock.install(referenceDate);`
      - `afterAll`：`ThreadLocalFrozenClock.clear();`（不再 `registerClock(defaultClock())`——全局槽保持 delegating clock）
      - 删除内部死代码 `ICClock` 内部类（被 delegating clock 取代）
      - Skill: nop-testing

Exit Criteria:

> 设计文档 §5 验收 1 的根因层（delegating clock 落盘 + 基类改写），交付可观察的并行隔离能力。全量 build/test 属于 Closure Gates（执行时规则 7）。

- [x] `ThreadLocalFrozenClock` 落盘 + `AbstractFrozenClockExtension` 改写，`module-common-test` 局部编译通过（`mvn compile -pl module-common-test -am -DskipTests`）

### Phase 2 - 15 域子类静态旁路迁移 + HR 调用点 + 逐域冒烟

Status: completed
Targets: 15 个 `*FrozenClockExtension` 子类（Fin/Inv/Pur/Sal/Mfg/Hr/Qa/Mnt/Prj/Crm/Cs/Log/B2b/Ct/Drp）；`module-hr/.../TestErpHrPayrollSimulation.java:444,477`
Skill: nop-testing

- Item Types: `Fix`（Fix-heavy，15 子类 + 1 调用点均为已确认缺陷迁移）
- Prereqs: Phase 1 done

- [x] Fix: 每子类 `installFrozenClock()` / `restoreSystemClock()` 重实现为线程本地委托：
      - `installFrozenClock()` → `ThreadLocalFrozenClock.ensureRegistered(); ThreadLocalFrozenClock.install(REFERENCE_DATE);`
      - `restoreSystemClock()` → `ThreadLocalFrozenClock.clear();`
      - （备选：删除二静态 + 迁移 HR 调用点——Phase 2 裁决二选一，保留 per-method 语义）
      - Skill: nop-testing
- [x] Fix: HR 活跃调用点 `TestErpHrPayrollSimulation:444,477`（per-method try/finally 冻结）迁移——保留二静态（仅改其实现委托 ThreadLocalFrozenClock），调用点不变，per-method 语义在路径 C 下保留（线程本地）
      - Skill: nop-testing
- [x] Proof: 逐域冒烟——HR 模块 `mvn test -pl module-hr/erp-hr-service -am` 125 测试 0 failures/0 errors（含 per-method `TestErpHrPayrollSimulation` 12 测试绿）；其余 14 域子类代码同构 + 由 Closure Gates 全量 `mvn test` 覆盖
      - Skill: nop-testing

Exit Criteria:

- [x] 15 子类二静态迁移完成（grep 闭环：`rg "registerClock" module-common-test/ module-*/erp-*-service/src/test/` 不再含「每子类静态整体替换」模式，仅 `ThreadLocalFrozenClock` 内一次性挂载 + 各子类委托 `ThreadLocalFrozenClock.install/clear`）
- [x] HR 调用点迁移完成，`mvn test -pl module-hr/erp-hr-service -am` 绿（125 测试 0 failures/0 errors，per-method 冻结语义保留）

### Phase 3 - 4 模块审计（assets 预标记高概率）

Status: completed
Targets: `module-assets/`、`module-master-data/`、`module-aps/`、`module-notify/` 的 `src/test`
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 1 done（Phase 2 独立可并行）

- [x] Proof: 逐模块核验日期敏感测试/生产代码存在性（设计文档 §4.1 step 4 命令）：`rg "CoreMetrics\.today|CoreMetrics\.currentDate|resolveOpenPeriod|LocalDate\.now" --glob '*.java' module-<域>/`。**结果**：assets `TestErpAstDashboard` 命中（CURRENT_PERIOD 静态 + seed 读 currentDate，快照 PERIOD=2026-08 即「推弹」证据）；master-data/aps 测试侧 0 命中；notify 仅 `TestErpSysNotificationDispatch:109` `currentDateTime().minusHours(2)`（相对小时窗口，非月敏感）
      - Skill: nop-testing
- [x] Add: **assets**（预标记高概率候选，10+ posting dispatcher + `TestErpAstDashboard` 日期敏感，与 finance 月初税同构）——补建 `AstFrozenClockExtension` 子类（REFERENCE_DATE=2026-07-17）+ `@RegisterExtension` 接线 TestErpAstDashboard + `CURRENT_PERIOD` 改由 REFERENCE_DATE 派生为常量「2026-07」（消除 class-load 读系统时钟）+ 冻结时钟下重录快照（PERIOD 由 2026-08→2026-07 永久稳定）。`mvn test -pl module-assets/erp-ast-service` 104 测试 0 failures/0 errors
      - Skill: nop-testing
- [x] Add: master-data / aps / notify 轻量审计——裁决落盘：**master-data 测试侧 0 日期敏感**；**aps 测试侧 0 日期敏感**；**notify `minusHours(2)` 为相对小时窗口（merge window 3600s），非月敏感，无需冻结**。三模块均记「无」跳过
      - Skill: nop-testing
- [x] Proof: 复核设计文档 §1.1 的 2 处生产 `LocalDate.now()` 残留（`ErpFinVoucherTemplateRenderTemplateProcessor:75` / `ErpMdCurrencyRefreshRatesFromApiProcessor`）覆盖性：**VoucherTemplateRender 被 `TestErpFinVoucherTemplateRender` + `TestErpFinVoucherTemplateExpr` 覆盖**（测试存在）；**CurrencyRefreshRatesFromApiProcessor 无测试覆盖**（登记 successor，设计文档 §7 范围外）。二者均属非过账链路生产残留，本期 out-of-scope
      - Skill: nop-testing

Exit Criteria:

- [x] assets 日期敏感测试得到冻结覆盖（补 `AstFrozenClockExtension` + @RegisterExtension + CURRENT_PERIOD 常量化 + 重录快照）；其余 3 模块裁决落盘（master-data/aps/notify 均记「无」）

### Phase 4 - finance 月初税 4 类 + 跨月模拟证据

Status: completed
Targets: `module-finance/erp-fin-service/src/test` 4 类；跨月模拟测试（方式 a 推荐）
Skill: nop-testing

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 done（冻结机制可用）

- [x] Fix: **A 组 2 类**（内联 `CoreMetrics.today()` 未注册冻结扩展）补 `@RegisterExtension static FinFrozenClockExtension finClock`（冻结到 seed 期间所在月 2026-07-17），使 `voucherDate` 落 seed OPEN 期间内；冻结时钟下重录快照（PERIOD 2026-08→2026-07 永久稳定，回收确定性）
      - `TestErpFinBadDebtReversal`（`:69,133,178,280` 内联 today）
      - `TestErpFinEmployeeAdvanceCashRepayReversal`（`:264` 内联 today）
      - Skill: nop-testing
- [x] Decision: **B 组 2 类裁决均为 (a)**——补冻结扩展 + seed 改 `YearMonth.from(FinFrozenClockExtension.REFERENCE_DATE)`，回收 R6.9 的 `checkOutput=false` 退让（4+1=5 处全部回收为 `true`）。裁决理由：路径 C 冻结时钟下输出确定性，checkOutput 退让无存在理由；seed 改由冻结参考日派生使期间与过账 voucherDate 同源冻结时钟，跨月确定。候选 (b)（维持 YearMonth.now()+checkOutput=false）否决——保留退让非根治
      - `TestErpFinNotesPayableStateMachine`：seedBase() 改 `YearMonth.from(REFERENCE_DATE)`，移除 4 处 `checkOutput=false`
      - `TestErpFinDashboard.testTrendMonthlySeries`：seed 改 `YearMonth.from(REFERENCE_DATE)`，移除 `checkOutput=false`；类级 `@RegisterExtension`（其它方法日期无关不受影响）
      - Skill: nop-testing
- [x] Proof: **跨月模拟运行时证据**（设计文档 §4.2 step 5 + §5 验收 2，方式 a）——新建 `TestClockRolloverFinance extends JunitBaseTestCase`，方法内 `install(2026-07-31)` + `install(2026-08-01)` 两边界日各跑 notesPayableBiz.issue 过账链路（resolveOpenPeriod(voucherDate=today)），断言两边界日均 ISSUED（过账成功）。`mvn test -pl module-finance/erp-fin-service` 307 测试 0 failures/0 errors
      - Skill: nop-testing

Exit Criteria:

- [x] A 组 2 类补冻结扩展后 finance 域绿（307 测试 0 failures/0 errors）；B 组 2 类裁决落盘（均选 (a)，回收 checkOutput）
- [x] 跨月模拟（方式 a）下 4 类全绿，不再 1 failure + 10 errors（TestClockRolloverFinance 证明两边界日过账期间解析成功）

### Phase 5 - CI C-2 clock-rollover nightly 接线 + 基线更新

Status: completed
Targets: `.github/workflows/clock-rollover.yml`（新建）；`docs/testing/known-good-baselines.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1-4 done

- [x] Add: 新建 `.github/workflows/clock-rollover.yml`（设计文档 §6.4）
      - `schedule: cron: '0 3 * * *'`（nightly）+ `workflow_dispatch` ✓
      - matrix `faketime '2026-08-31 23:59:00'` + `faketime '2026-09-01 00:01:00'` 两跨月边界日；先 `mvn install -DskipTests` 构建依赖链（正常时钟），再 `faketime` 仅包裹测试执行跑 `mvn test -pl module-finance/erp-fin-service`（faketime 不作用于构建产物）✓
      - 失败即红 ✓
      - Skill: none
- [x] Proof: `faketime` 与 JVM 时区/`LocalDate.now` 兼容性裁决——**主证明为 TestClockRolloverFinance（JVM 层，每次构建跑，不依赖 faketime）**；faketime nightly 为补充回归层（防路径 C 实施遗漏 + 防未来旁路冻结机制的 LocalDate.now 新测试回潮）。R5 兼容性由 nightly workflow 本身在 GitHub runner 上实测（faketime 在 System.currentTimeMillis 层拦截，LocalDate.now 经其派生应一致）；不兼容则 workflow 可降级（主证明 TestClockRolloverFinance 仍权威）。裁决落盘
      - Skill: none
- [x] Add: `docs/testing/known-good-baselines.md` 追加 2026-08-01 Q6 Phase 2 全绿基线条目（`mvn test` 1920 tests / 0 failures / 0 errors / 1 skipped，含新增 TestThreadLocalFrozenClockParallel 2 + TestClockRolloverFinance 1 计数）
      - Skill: none

Exit Criteria:

- [x] clock-rollover.yml 落盘 + 本地 TestClockRolloverFinance 跨月绿（faketime nightly 在 CI runner 上首次调度时实测，主证明不依赖 faketime）；known-good-baselines 更新

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_04416e85cffeWJiSNDWEMnPinv`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 2 MINOR。全部 Current Baseline 实仓主张独立复核 PASS（`AbstractFrozenClockExtension.java:63,68` registerClock、精确 15 子类 + 15 静态旁路、HR 1 活跃调用点、2 处生产 LocalDate.now 残留、仅 `TestErpFinReportRendering` 注册冻结扩展确认 A 组前提、assets 19 文件读 CoreMetrics.today、ThreadLocalFrozenClock/clock-rollover.yml 不存在[预期]、设计文档 Review Record 3 轮收敛无 BLOCKER/MAJOR）。MQ doc-first（引用收敛设计文档为契约 + 含「实现与设计文档一致」gate）PASS。MINOR-1（§5.6 无双真相源未显式列为独立 gate）已采纳——折叠进「相关文档对齐」gate（下方）。MINOR-2（Phase 1 `ThreadLocalFrozenClock` 接口内联复述）保留——属 rule 6 例外（新建类公共面 = 结构边界契约），不裁剪。无 BLOCKER/MAJOR → converged → 转 active。

## Closure Gates

> 设计文档 §5 验收判据为本计划 closure 契约。全量 `mvn clean install -DskipTests` + `mvn test` 在此一次性运行（执行时规则 7），不重复于各阶段。

- [x] 范围内行为完成（设计文档 §5 验收 1-5）
  - 并行不污染客观证明：`TestThreadLocalFrozenClockParallel`（起 2 线程各 install 不同日期，断言 `CoreMetrics.today()` 各自冻结日互不串扰，20 迭代）落盘且绿 ✓
  - 月初税消除：跨月模拟下 4 类全绿（设计文档 §5 验收 2）—— `TestClockRolloverFinance` 在 install(2026-07-31)+install(2026-08-01) 两边界日过账 resolveOpenPeriod 成功；finance 域 307 测试 0 failures/0 errors ✓
  - `checkOutput` 退让回收：`rg "@EnableSnapshot\(checkOutput\s*=\s*false" module-finance/erp-fin-service/src/test/` 实际注解命中数 = **0**（R6.9 的 5 处全回收；rg 仅剩注释文本命中）✓
  - 15 域子类迁移完成性：`rg -l "extends AbstractFrozenClockExtension" --glob '*.java'` = **16**（15 + assets 补建 AstFrozenClockExtension）；16 文件二静态重实现为线程本地委托（grep 方法体不含裸 `CoreMetrics.registerClock(new`）；HR 调用点迁移（per-method 语义保留，125 测试绿）✓
  - install() 覆盖不变量（快照安全前提，设计文档 §5 验收 7）：4 日期敏感类（BadDebt/EmployeeAdvanceReversal/NotesPayable/Dashboard）均 @RegisterExtension 冻结；**6 个读 CoreMetrics 但未冻结的 finance 测试登记闭门例外**（EmployeeAdvanceCashRepay/BudgetCarryForward/VoucherReversePreview/BudgetEndToEnd/BudgetIsolation/IntercompanyMatching——today() 派生值经 @var 掩码或不在比较输出表，核验 0 字面 2026-08 + Aug 1 全绿，非月敏感；非 bug doc 4 类）✓
- [x] 相关文档对齐：设计文档 `clock-test-infrastructure.md` Review Record 回填 ensureRegistered 实施期发现（NopJunitExtension.afterAll 重置全局时钟）；`docs/logs/2026/08-01.md` 追加日志条目
- [x] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（**1920 tests / 0 failures / 0 errors / 1 skipped**，含新增 TestThreadLocalFrozenClockParallel 2 + TestClockRolloverFinance 1）；零生产代码变更（仅测试基础设施 + CI workflow）
- [x] 无范围内项目降级为 deferred/follow-up（2 处生产 `LocalDate.now()` 残留经设计文档 §7 显式 out-of-scope，非范围内项目）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（known-good-baselines 2026-08-01 行 + 各阶段 mvn 输出）
- [x] **实现与设计文档一致**（一处实施期发现已回填设计文档 Review Record 而非静默偏离：`ensureRegistered()` 由「INSTALLED 标志幂等跳过」改为「每次调用均重新注册」，因平台 NopJunitExtension.afterAll 每个测试类结束后 registerClock(defaultClock()) 重置全局时钟，设计文档 §3.3「per-fork 一次性注册持久」假设不成立——见设计文档 Review Record 回填）

## Deferred But Adjudicated

### 2 处生产 `LocalDate.now()` 残留（非过账链路）

- Classification: `watch-only residual`
- Why Not Blocking Closure: Q6 聚焦测试侧时钟基础设施（设计文档 §1.1 范围裁决）；改生产代码须独立保护区域评估（会计期间/模板有效期/汇率刷新）。设计文档 §7 登记 successor。
- Successor Required: yes —— 触发条件：本期后独立计划评估生产侧日期敏感直调（`ErpFinVoucherTemplateRenderTemplateProcessor:75` / `ErpMdCurrencyRefreshRatesFromApiProcessor`）。

### 路径 A（nop-entropy 平台 thread-local clock 原生根治）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计文档 §3.4 裁决路径 C 应用层闭环优先（AGENTS.md 应用层闭环原则）；路径 A 跨仓库升级耦合阻塞及时交付。路径 C 已根治并行 + 治月初税。
- Successor Required: yes —— 触发条件：上游 nop-entropy 接受路径 A scope API PR 时，delegating clock 可重实现为委托平台 scope（设计文档 §7 R3/R4，无锁定）。

### CI C-3 并发矩阵（threadCount>1 / parallel=methods）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 设计文档 §6.3 裁决路径 C 并发根治由验收 1 本地核验保证；CI 并发矩阵收益不足以抵 nightly 成本。
- Successor Required: yes —— 触发条件：surefire 实际切 `parallel=methods` / `threadCount>1` 时。

## Closure

Status Note: **completed**（独立结束审计 PASS，2026-08-01）

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_043e6c99fffe5PhgRgny7L18pv`（fresh cold context，read-only）— **Verdict: PASS**，10 项检查全绿。逐项证据：(1) ThreadLocalFrozenClock implements IClock + 线程本地 REF_DATE + 委托 defaultClock + ensureRegistered 每次重注册 + static{} 触发；AbstractFrozenClockExtension beforeAll=ensureRegistered+install / afterAll=clear，无 ICClock / 无 registerClock ✓ (2) `rg registerClock` subclasses 仅 ThreadLocalFrozenClock 自身；`extends AbstractFrozenClockExtension` = 16 文件（15+Ast）；子类二静态全委托 ThreadLocalFrozenClock ✓ (3) 4 finance 类 @RegisterExtension FinFrozenClockExtension 齐全；NotesPayable/Dashboard seed 用 YearMonth.from(REFERENCE_DATE)；`@EnableSnapshot(checkOutput=false)` 实际注解 = 0 ✓ (4) AstFrozenClockExtension 存在 + TestErpAstDashboard @RegisterExtension + CURRENT_PERIOD 由 REFERENCE_DATE 派生 ✓ (5) TestThreadLocalFrozenClockParallel（2 线程 20 迭代隔离）+ TestClockRolloverFinance（两边界日过账）落盘且语义正确 ✓ (6) clock-rollover.yml cron+workflow_dispatch+faketime matrix ✓ (7) 设计文档 Phase 2 实施期回填段 + known-good-baselines 2026-08-01 行 + logs/08-01 EXECUTE 条目 + roadmap Q6=done ✓ (8) `mvn test -Dtest=...` 4 类 12 测试 BUILD SUCCESS 0 failures/0 errors ✓ (9) git diff 无 src/main/java 生产代码、无 ../nop-entropy/ 改动（仅测试基础设施 + CI + docs）✓ (10) ensureRegistered 偏离已回填设计文档 Review Record 非静默 ✓。Non-blocking 观察：工作树混有无关 master-data 海关贸易 schema 演进 + Q1/Q4 plan 草稿（跨流污染，非 Q6 scope 违反），建议 Q6 隔离提交。

Follow-up:

- 生产侧 `LocalDate.now()` 残留 successor（见上 Deferred）。
- 其余 MQ 维度（Q1/Q4 同批 Phase 2 plan、Q3/Q2/Q5/Q7 Phase 1 设计文档）各有独立计划。
