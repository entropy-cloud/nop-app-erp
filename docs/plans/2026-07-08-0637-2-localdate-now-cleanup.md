# 2026-07-08-0637-2-localdate-now-cleanup LocalDate.now() 平台时间 API 对齐

> Plan Status: completed
> Mission: erp
> Work Item: 生产 + 测试代码 LocalDate.now() 平台时间 API 对齐（承接 0517-1 Deferred，修正其生产计数低估）
> Last Reviewed: 2026-07-08
> Source: `docs/plans/2026-07-08-0517-1-test-code-localdatetime-now-cleanup.md` Deferred「LocalDate.now() 残留清理（生产 4 处 + 测试 ~96 处）」；`docs/bugs/2026-07-07-1915-localdatetime-now-in-12-domains.md`（根因记录）
> Related: `docs/plans/2026-07-08-0517-1-test-code-localdatetime-now-cleanup.md`（同批 N=1 的 LocalDateTime.now() 清理，本计划为其 LocalDate.now() 后继）、`docs/plans/2026-07-07-2200-1-multi-dim-audit-supplement.md`（completed，H-2 生产 LocalDateTime.now() 修复，未覆盖 LocalDate.now()）
> Audit: required

## Current Baseline

实时仓库逐项核实（`rg`，非采信旧记忆）：

- **Deferred 触发条件已满足**：0517-1 Deferred「LocalDate.now() 残留清理」明示触发条件「下一轮清理周期自动承接；或当某测试因 LocalDate.now() 导致 CI 不稳定、或生产 LocalDate.now() 与 CoreMetrics.currentDate() 跨时区产生偏差时立即修复」。当前为「下一轮计划化周期」（mission-driver draft-from-roadmap），触发条件满足。
- **生产代码残留（实时 grep，修正 0517-1 低估）**：`rg 'LocalDate\.now\(\)' --glob '**/src/main/**/*.java'` 命中 **14 文件 / 28 处**（0517-1 记录「生产 4 处」**低估**——0517-1 起草时仅枚举 hr/crm 4 处，遗漏 0935-1/1606-1/1100-3 各看板/报表 BizModel 引入的 LocalDate.now()）：
  - `module-purchase/.../dashboard/ErpPurDashboardBizModel.java`（3：:71/:104/:194）
  - `module-sales/.../dashboard/ErpSalDashboardBizModel.java`（3：:61/:93/:160）
  - `module-quality/.../dashboard/ErpQaDashboardBizModel.java`（3：:69/:100/:166）
  - `module-quality/.../spc/ErpQaSpcCapabilityJob.java`（1：:64）
  - `module-inventory/.../dashboard/ErpInvDashboardBizModel.java`（4：:66/:102/:189/:222）
  - `module-manufacturing/.../dashboard/ErpMfgDashboardBizModel.java`（3：:56/:107/:134）
  - `module-assets/.../dashboard/ErpAstDashboardBizModel.java`（2：:106/:206）
  - `module-maintenance/.../dashboard/ErpMntDashboardBizModel.java`（2：:62/:139）
  - `module-projects/.../dashboard/ErpPrjDashboardBizModel.java`（1：:130）
  - `module-finance/.../dashboard/ErpFinDashboardBizModel.java`（1：:88）
  - `module-finance/.../report/ErpFinReportBizModel.java`（1：:296 `asOfDate != null ? asOfDate : LocalDate.now()`）
  - `module-crm/.../support/PriceRuleEngine.java`（1：:47 `now != null ? now : LocalDate.now()`）
  - `module-hr/.../entity/ErpHrGapAnalysisBizModel.java`（1：:156）
  - `module-hr/.../entity/ErpHrDevelopmentPlanBizModel.java`（2：:173/:174）
- **生产代码 LocalDateTime.now() 已清零**（镜像基准）：`rg 'LocalDateTime\.now\(\)' --glob '**/src/main/**/*.java'` = 0 命中（0517-1/2200-1 H-2 范围，但 0517-1 仍 draft；本计划独立于 0517-1 是否落地——本计划仅处理 LocalDate.now()）。
- **测试代码残留**：`rg 'LocalDate\.now\(\)' --glob '**/src/test/**/*.java'` = **23 文件 / 106 处**（0517-1 记录「~96 处」，略有增长）。其中 4 个与 0517-1（LocalDateTime.now()）重叠文件夹带 16 处 LocalDate.now()（TestErpCtESignature:6/TestErpMntDashboard:5/TestErpQaDashboardSpc:2/TestErpQaSpcCapability:3）+ 19 个仅含 LocalDate.now() 的测试文件（90 处）。
- **替换目标 API**：`CoreMetrics.currentDate()`（返回 `LocalDate`，包 `io.nop.api.core.time.CoreMetrics`，类位于 `../nop-entropy/nop-kernel/nop-api-core/src/main/java/io/nop/api/core/time/CoreMetrics.java`，`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md` 权威；既有生产代码如 `ErpSalReturnProcessor` 等 20+ 文件均 `import io.nop.api.core.time.CoreMetrics;`）。语义等价——`LocalDate.now()` 与 `CoreMetrics.currentDate()` 均返回当前日期 `LocalDate`，经平台 `CoreClock` 统一时间源。
- **两类生产使用模式**：(a) 取「今天」基准——`LocalDate today = LocalDate.now();`（看板/报表默认区间，绝大多数）；(b) 兜底——`asOfDate != null ? asOfDate : LocalDate.now()`（ErpFinReportBizModel:296 / PriceRuleEngine:47，入参为空时回退当前日期）。两类均直接替换 `LocalDate.now()` → `CoreMetrics.currentDate()`，表达式结构不变。
- **conventions 现状**：`docs/context/conventions.md` 实时 `rg` 时间 API 段 = **0 命中**（0517-1 仍 draft，其 Phase 3 拟增补的「时间 API 使用约定」段**尚未落地**）。本计划须自行落地该约定段（与 0517-1 不冲突——0517-1 落地后该段已含 LocalDateTime.now() 禁令，本计划补 LocalDate.now() 禁令；若 0517-1 未落地则本计划一并建段）。
- **保护区域**：纯生产/测试代码机械替换 + conventions 文档，**无 ORM/契约/认证变更**，非 `ask-first`。属 `plan-first`（跨 14 生产模块 + 23 测试文件，>5 文件、>200 行机械替换）。

剩余差距：(1) 生产 14 文件 28 处 + 测试 23 文件 106 处 `LocalDate.now()` 未对齐平台 `CoreMetrics` API，与生产代码 `LocalDateTime.now()` 已清零的时间源不一致（0517-1 修正了 LocalDateTime 一半，LocalDate 另一半残留）；(2) `conventions.md` 缺时间 API 使用规约（含 LocalDate.now() 禁令）；(3) 0517-1 对生产 LocalDate.now() 计数「4 处」低估（实际 28 处）需在本计划纠正。

## Goals

- 将生产代码 14 文件 28 处 `java.time.LocalDate.now()` 替换为平台 `CoreMetrics.currentDate()`，使生产时间源完全统一（与已清零的 `LocalDateTime.now()` 对齐，经 `CoreClock`）。
- 将测试代码 23 文件 106 处 `LocalDate.now()` 替换为 `CoreMetrics.currentDate()`，使测试与生产共享同一时间源。
- 增补 `docs/context/conventions.md`「时间 API 使用约定」段（若 0517-1 已建段则补 LocalDate.now() 禁令 + 移除「残留」附注；若未建则一并建段）——禁止生产/测试代码直接调 `LocalDate.now()`/`LocalDateTime.now()`/`Clock.systemDefaultZone()`，统一经 `CoreMetrics`。
- 纠正 0517-1 对生产 LocalDate.now() 的计数低估（4 处 → 实际 28 处），在本计划记录并（若 0517-1 仍 draft）回写 0517-1 baseline 更正。
- 验证受影响模块测试全绿无回归。

## Non-Goals

- **不**替换 `LocalDateTime.now()`（生产 0 处已清零；测试 14 文件 32 处归 0517-1 范围——本计划不重复；若 0517-1 与本计划同期执行，两者文件集不重叠：0517-1 仅替 LocalDateTime.now()，本计划仅替 LocalDate.now()）。
- **不**引入 TestClock/CoreClock 可注入固定时钟（同 0517-1 Non-Goal——本期仅对齐时间源至平台 API；固定时钟属平台能力深化，归 Deferred）。
- **不**改变业务逻辑/表达式结构（仅替换时间源调用，`LocalDate.now().minusDays(n)` → `CoreMetrics.currentDate().minusDays(n)`；兜底三元 `x != null ? x : LocalDate.now()` → `x != null ? x : CoreMetrics.currentDate()`）。
- **不**触及 `*.orm.xml`/`*.xbiz`/view/`application.yaml`。
- **不**重构看板/报表 BizModel 的查询逻辑（仅替换取「今天」的时间源；看板区间计算语义不变）。
- **不**承接 0517-1 另一 Deferred「dao().updateEntity() 剩余域修复」（独立结果面，归 2200-1 Deferred）。

## Task Route

- Type: `implementation-only change`（生产 + 测试代码机械替换 + 约定文档；零 ORM/契约/认证变更）
- Owner Docs: `docs/bugs/2026-07-07-1915-localdatetime-now-in-12-domains.md`（缺陷根因）、`docs/context/conventions.md`（约定增补目标）、`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`CoreMetrics` 平台 API 权威）
- Skill Selection Basis: `nop-backend-dev`（生产代码修改——14 看板/报表 BizModel + PriceRuleEngine + hr BizModel，替换 `LocalDate.now()` 调用，须确保 `CoreMetrics` import 与 `LocalDate` 变量类型不变；机械替换非新业务逻辑，技能用于确保不破坏既有 BizModel 编译/语义）。`nop-testing` 用于测试代码替换阶段。无 ORM/前端变更，对应技能不适用。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 无 ORM/契约/认证变更，故无 ask-first 保护区域门控

## Execution Plan

### Phase 1 - 残留点精确盘点 + 0517-1 计数纠正

Status: completed
Targets: 14 生产文件 + 23 测试文件（grep 精确清单）、`CoreMetrics.currentDate()` 签名
Skill: `nop-backend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 无

- [x] `Proof`：执行 `rg -n 'LocalDate\.now\(\)' --glob '**/src/**/*.java'` 产出精确逐行清单（文件:行:代码），核对与本计划 Baseline 列表一致；若实时计数与本计划记录的 14 生产文件/28 处 + 23 测试文件/106 处有偏差，以实时 grep 为准并在此记录差异。
      - Skill: `nop-backend-dev`
- [x] `Decision`：替换口径裁决——`LocalDate.now()` → `CoreMetrics.currentDate()`（唯一替换对象）。记录选择、残留风险（`CoreMetrics.currentDate()` 仍返回「当前日期」非固定时钟，与 0517-1 同口径——固定时钟归 Deferred）与替代方案（rejected：仅替生产不替测试——测试与生产时间源不一致违背本计划目的；rejected：引入 TestClock 固定时刻——超出本期「对齐时间源」结果面，与 0517-1 Non-Goal 一致）。
      - Skill: `nop-backend-dev`
- [x] `Add`：若 0517-1 仍 `draft`，回写 0517-1 Baseline「LocalDate.now() 残留」段，将「生产 4 处」纠正为「生产 14 文件/28 处（本计划纠正）」，并指向本计划；若 0517-1 已 `active`/`completed` 则在本计划记录纠正即可。
      - Skill: none

Exit Criteria:

- [x] 逐行清单落盘（写入本计划 Phase 1 记录段），生产 14 文件/测试 23 文件每行标注替换；实时 grep 计数与清单一致。
- [x] 替换口径 Decision 记录选择 + 替代方案 + 残留风险。

#### Phase 1 记录段

**实时 grep 结果（2026-07-08 执行）**：

- 生产代码 `rg -n 'LocalDate\.now\(\)' --glob '**/src/main/**/*.java'` 命中 **14 文件 / 28 处**，与本计划 Baseline 完全一致（无偏差）：
  - `module-purchase/.../dashboard/ErpPurDashboardBizModel.java`（3 处）
  - `module-sales/.../dashboard/ErpSalDashboardBizModel.java`（3 处）
  - `module-quality/.../dashboard/ErpQaDashboardBizModel.java`（3 处）
  - `module-quality/.../spc/ErpQaSpcCapabilityJob.java`（1 处）
  - `module-inventory/.../dashboard/ErpInvDashboardBizModel.java`（4 处）
  - `module-manufacturing/.../dashboard/ErpMfgDashboardBizModel.java`（3 处）
  - `module-assets/.../dashboard/ErpAstDashboardBizModel.java`（2 处）
  - `module-maintenance/.../dashboard/ErpMntDashboardBizModel.java`（2 处）
  - `module-projects/.../dashboard/ErpPrjDashboardBizModel.java`（1 处）
  - `module-finance/.../dashboard/ErpFinDashboardBizModel.java`（1 处）
  - `module-finance/.../report/ErpFinReportBizModel.java`（1 处）
  - `module-crm/.../support/PriceRuleEngine.java`（1 处）
  - `module-hr/.../entity/ErpHrGapAnalysisBizModel.java`（1 处）
  - `module-hr/.../entity/ErpHrDevelopmentPlanBizModel.java`（2 处）
- 测试代码 `rg -n 'LocalDate\.now\(\)' --glob '**/src/test/**/*.java'` 命中 **23 文件 / 106 处**，与本计划 Baseline 完全一致（无偏差）。

**Decision 记录**：

- **选择**：`LocalDate.now()` → `CoreMetrics.currentDate()`（`io.nop.api.core.time.CoreMetrics`，返回 `LocalDate`，类型兼容），表达式结构不变。
- **残留风险**：`CoreMetrics.currentDate()` 仍返回「当前日期」（经 `CoreClock`），非固定时钟——与 0517-1 同口径；固定时钟归 Deferred（见本计划 Deferred 段）。
- **替代方案（rejected）**：(a) 仅替生产不替测试——测试与生产时间源不一致违背本计划目的；(b) 引入 TestClock 固定时刻——超出本期「对齐时间源」结果面，与 0517-1 Non-Goal 一致。

**0517-1 计数纠正**：0517-1 已 `completed`（非 draft），按 Phase 1 Add 指令「若 0517-1 已 active/completed 则在本计划记录纠正即可」，本计划 Baseline（L16）已记录纠正（0517-1「生产 4 处」低估→实际 14 文件/28 处），无需回写 0517-1。

### Phase 2 - 生产代码替换（14 文件 28 处）

Status: completed
Targets: 14 生产文件（看板/报表 BizModel + PriceRuleEngine + hr BizModel）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 清单与口径裁决完成

- [x] `Fix`：逐文件将 `LocalDate.now()` → `CoreMetrics.currentDate()`，并补 `import io.nop.api.core.time.CoreMetrics;`（若文件未引入）。保留 `import java.time.LocalDate;`（变量类型仍为 `LocalDate`，不可删）。替换不改变表达式结构（`today = LocalDate.now()` → `today = CoreMetrics.currentDate()`；兜底三元 `x != null ? x : LocalDate.now()` → `x != null ? x : CoreMetrics.currentDate()`）。逐文件核实 `LocalDate` 变量声明类型不变（`LocalDate today = CoreMetrics.currentDate()` 类型兼容）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 14 生产文件中 `rg 'LocalDate\.now\(\)' --glob '**/src/main/**/*.java'` 0 命中；`CoreMetrics` import 已补齐；受影响模块 `mvn compile -pl <modules> -am` 0 编译错误（解除 Phase 4 阻塞的本地化检查）。

### Phase 3 - 测试代码替换（23 文件 106 处）+ conventions 增补

Status: completed
Targets: 23 测试文件、`docs/context/conventions.md`
Skill: `nop-testing`

- Item Types: `Fix | Add`
- Prereqs: Phase 2 生产替换落地

- [x] `Fix`：逐文件将测试代码 `LocalDate.now()` → `CoreMetrics.currentDate()`，补 `import io.nop.api.core.time.CoreMetrics;`。保留 `import java.time.LocalDate;`（除非文件无其他 `LocalDate` 引用方可移除）。不替换同文件中的 `LocalDateTime.now()`（归 0517-1，若文件重叠）。
      - Skill: `nop-testing`
- [x] `Add`：`docs/context/conventions.md` 增补「时间 API 使用约定」段——禁止生产/测试代码直接调 `java.time.LocalDate.now()`/`LocalDateTime.now()`/`Clock.systemDefaultZone()`，统一经平台 `io.nop.api.core.time.CoreMetrics`（`currentDate()`/`currentDateTime()`/`currentTimeMillis()`，对齐 `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`）。若 0517-1 已建该段（含 LocalDateTime.now() 禁令 +「LocalDate.now() 残留」附注），则补 LocalDate.now() 禁令并**移除残留附注**（因本期已清理）；若未建则一并建段。
      - Skill: none

Exit Criteria:

- [x] 23 测试文件中 `rg 'LocalDate\.now\(\)' --glob '**/src/test/**/*.java'` 0 命中；`CoreMetrics` import 补齐；`mvn test-compile` 受影响模块通过。
- [x] `conventions.md` 含「时间 API 使用约定」段（LocalDate.now() + LocalDateTime.now() 禁令统一）；残留附注已移除（若无残留）。

### Phase 4 - 验证

Status: completed
Targets: 受影响模块测试
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2/3 替换落地

- [x] `Proof`：执行 `mvn test` 受影响域（purchase/sales/quality/inventory/manufacturing/assets/maintenance/projects/finance/crm/hr 11 生产域 + 含 LocalDate.now() 的测试域），验证 0 failures/0 errors，既有断言语义无回归。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 受影响域 `mvn test` 0 failures/0 errors。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c140621affeBlNH7ZqwIZ64b3) because 1 BLOCKER + 1 NOTE：
  - B1：**编译致命错误**——Phase 2/3 import 指令 `io.nop.core.lang.utils.CoreMetrics` 包不存在。实时核实真实类为 `io.nop.api.core.time.CoreMetrics`（`../nop-entropy/nop-kernel/nop-api-core/.../io/nop/api/core/time/CoreMetrics.java`，`currentDate()` 返回 `LocalDate`），既有 20+ 生产文件均用此包。若按原指令执行将致 13 生产文件 + 测试文件编译失败。已更正 Phase 2/3 import + conventions + 替换目标 API 段全部为 `io.nop.api.core.time.CoreMetrics`（注：兄弟计划 0517-1 同样携带此缺陷，其审查应一并修正——本计划回写 0517-1 时一并提示）。
  - N1：Baseline 测试残留分解错误——原「0517-1 本期 14 文件夹带 16 处 + ~9 个仅含」实际为「4 个与 0517-1 重叠文件夹带 16 处 + 19 个仅含 LocalDate.now() 文件（90 处）」。范围总数（23 文件/106 处）正确，Phase 3 指令（不替换 LocalDateTime.now()）正确。已更正分解为 4 重叠/19 仅含。
  - 正面确认（无需变更）：生产基线精确（14 文件/28 处，逐行行号匹配）；测试基线精确（23 文件/106 处）；LocalDateTime.now() 生产 0 命中（镜像基准）；conventions.md 0 时间 API 段；API 正确（`CoreMetrics.currentDate()`→`LocalDate` 类型兼容）；0517-1 低估确认（4→28，0517-1 仍 draft 故回写方案可行）；单结果面（LocalDate.now() 清理）；与 0517-1 无范围重叠（LocalDate vs LocalDateTime，4 重叠文件 Phase 3 处理正确）；反松弛合规；plan-first 分类正确；item types/技能标注正确；Phase 1 自对账 Proof 将重 grep 校准漂移；模板结构完整。
- Independent draft review iteration 2: `accept` (ses_0c13911d1ffeN2FYT9173FSuGr) — B1/N1 全部经实时仓库复核确认已修复，无新增 BLOCKER：B1（CoreMetrics.java 确认在 `io.nop.api.core.time` 包，`currentDate()` :64 返回 `LocalDate` 类型兼容；错误串 `io.nop.core.lang.utils.CoreMetrics` 仅存于 Draft Review Record 历史叙述非指令；4 处指令位置（API 描述 L33/Phase 2 L100/Phase 3 L116/conventions L118）均用正确包；既有 129 生产文件用此包）；N1（分解更正为 4 重叠/19 仅含，4+19=23 文件、16+90=106 处，与 `rg` 实测一致；范围总数不变）。生产基线 14 文件/28 处确认；无新反松弛违规；范围未变。**草案审查已收敛**。

## Closure Gates

> 本计划为生产 + 测试代码机械替换 + 约定文档（零 ORM/契约/认证变更），结束前运行一次受影响模块 + 全工作区验证。

- [x] 范围内行为完成（生产 14 文件 28 处 + 测试 23 文件 106 处 LocalDate.now() 替换 + conventions 增补）
- [x] 相关文档对齐（`conventions.md` 时间 API 约定含 LocalDate.now() 禁令；残留附注已移除；0517-1 计数纠正在本计划 Baseline 记录）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test`（受影响域，0 failures/0 errors）+ `rg 'LocalDate\.now\(\)' --glob '**/src/**/*.java'` 0 命中
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### TestClock / CoreClock 可注入固定时钟

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期仅将生产 + 测试时间源对齐至平台 `CoreMetrics` API（与 0517-1 同口径），使生产/测试共享 `CoreClock`。可注入固定时钟（断言精确日期/时间戳）属平台能力深化，需 `@NopTestConfig` 扩展或 `ITimeService` 抽象，超出「对齐时间源」结果面。与 0517-1 Deferred 同口径。
- Successor Required: `yes`
- Trigger Condition: 当某测试因 `CoreMetrics.current*()` 仍返回「当前时刻/日期」导致断言边界脆弱（如断言精确 `businessDate` 失败），需固定时钟时。

## Closure

Status Note: 全部 4 Phase 已完成。生产 14 文件 28 处 + 测试 23 文件 106 处 `LocalDate.now()` 已全部替换为 `CoreMetrics.currentDate()`（含 1 处全限定 `java.time.LocalDate.now()`）；`conventions.md`「时间 API 使用约定」段 LocalDate.now() 残留附注已移除并改为归零记录；bug 文档 `docs/bugs/2026-07-07-1915-...` 状态已更新为本 bug 闭环。验证全绿：全工作区 `mvn clean install -DskipTests`（154 模块）BUILD SUCCESS + 受影响 13 service 模块 `mvn test` 0 failures/0 errors + `rg 'LocalDate\.now\(\)' --glob '**/src/**/*.java'` 0 命中。

Closure Audit Evidence:

- Auditor / Agent: 由 mission-driver（EXECUTE）指令驱动执行；独立结束审计由独立子代理（新会话）按项目规则另行执行。
- Evidence:
  - 替换落盘：`rg 'LocalDate\.now\(\)' --glob '**/src/main/**/*.java'` = 0 命中；`--glob '**/src/test/**/*.java'` = 0 命中；`rg 'java\.time\.LocalDate\.now\(\)' --glob '**/src/**/*.java'` = 0 命中。
  - 导入补齐：14 生产文件中 13 个新增 `import io.nop.api.core.time.CoreMetrics;`（1 个 ErpHrDevelopmentPlanBizModel 已含），23 测试文件中 18 个新增；无重复导入。
  - 类型兼容：`LocalDate today = CoreMetrics.currentDate()` 变量声明类型不变；兜底三元 `now != null ? now : CoreMetrics.currentDate()` 表达式结构不变（PriceRuleEngine/ErpFinReportBizModel/ErpHrGapAnalysisBizModel）。
  - 编译验证：13 受影响 service 模块 `mvn compile -am` BUILD SUCCESS；`mvn test-compile -am` BUILD SUCCESS。
  - 全量验证：`mvn clean install -DskipTests`（154 模块）BUILD SUCCESS（exit 0）；`mvn test`（purchase/sales/quality/inventory/manufacturing/assets/maintenance/projects/finance/crm/hr/contract/cs 13 模块）BUILD SUCCESS（exit 0），聚合 0 failures / 0 errors。
  - conventions.md：时间 API 约定段含 LocalDate.now() + LocalDateTime.now() 双重禁令；残留附注已替换为归零记录。
  - bug 文档：`docs/bugs/2026-07-07-1915-localdatetime-now-in-12-domains.md` 状态已更新为「✅ 已全部修复，本 bug 闭环」。


- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS_WITH_NOTES**. Plan's own execution verified correct: 28 production replacements in place and type-compatible; conventions.md section added; bug doc closed. Production code fully clean (0 hits). NOTE: 12 test-code regressions introduced by 3 later plans after this plan closed — enforcement gap, not a defect in this plan's work. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2; this evidence block appended by Phase 3 backfill.)
Follow-up:

- Deferred（已裁决，非阻塞）：TestClock / CoreClock 可注入固定时钟——当某测试因 `CoreMetrics.current*()` 仍返回「当前时刻/日期」导致断言边界脆弱时承接（Successor Required: yes）。
