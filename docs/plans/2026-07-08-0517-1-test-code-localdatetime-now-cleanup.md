# 2026-07-08-0517-1-test-code-localdatetime-now-cleanup 测试代码 LocalDateTime.now() 平台时间 API 对齐

> Plan Status: completed
> Mission: erp
> Work Item: 测试代码 LocalDateTime.now() 平台时间 API 对齐（承接 2200-1 Deferred）
> Last Reviewed: 2026-07-08
> Source: `docs/plans/2026-07-07-2200-1-multi-dim-audit-supplement.md` Deferred「测试代码 LocalDateTime.now() 修复（32 处）」+ `docs/bugs/2026-07-07-1915-localdatetime-now-in-12-domains.md`
> Related: `docs/plans/2026-07-07-2200-1-multi-dim-audit-supplement.md`（completed，其 Deferred 本计划承接）、`docs/plans/2026-07-07-1915-1-audit-remediation-plan.md`（completed，H-2 生产代码已修复）
> Audit: required

## Current Baseline

实时仓库逐项核实（`rg`/`read`，非采信旧记忆）：

- **Deferred 触发条件已满足**：2200-1 Deferred「测试代码 LocalDateTime.now() 修复」明示触发条件「当本计划 Phase 4 M-5 生产代码修复完成后，下一轮计划化周期自动承接测试代码修复；或当某个测试文件因 LocalDateTime.now() 导致 CI 不稳定时立即修复」。2200-1 Plan Status=completed，Phase 4 M-5 生产代码 `LocalDateTime.now()`→`CoreMetrics.current*()` 已落地（`docs/bugs/2026-07-07-1915-localdatetime-now-in-12-domains.md` 关联 H-2）。故「下一轮计划化周期自动承接测试代码修复」触发条件已满足。
- **生产代码基线（镜像基准）**：生产代码 `LocalDateTime.now()` 已统一替换为平台 API（`CoreMetrics.currentDateTime()`——`LocalDateTime`、`CoreMetrics.currentDate()`——`LocalDate`、`CoreMetrics.currentTimeMillis()`——`long`），实时 `rg 'LocalDateTime\.now\(\)' --glob '**/src/main/**/*.java'` = 0 命中（M-5 已由 2200-1/1915-1 修复）。实证：`module-hr/erp-hr-service/.../ErpHrDevelopmentPlanBizModel.java:70` `CoreMetrics.currentDate()`、`ErpHrSalarySimulationBizModel.java:122` `CoreMetrics.currentDateTime()`、`competency/GapAnalysisCalculator.java:50` `CoreMetrics.currentDateTime()`。既有测试已存在对齐范式：`module-sales/erp-sal-service/src/test/.../TestErpSalQuotationToOrder.java:100` `assertEquals(CoreMetrics.currentDate(), order.getBusinessDate(), ...)`。
- **`LocalDate.now()` 残留（非本期范围）**：实时 `rg 'LocalDate\.now\(\)' --glob '**/src/main/**/*.java'` 仍命中 4 处生产代码（`module-crm/.../PriceRuleEngine.java:47`、`module-hr/.../ErpHrGapAnalysisBizModel.java:156`、`module-hr/.../ErpHrDevelopmentPlanBizModel.java:173,174`）；测试代码另有 ~19 个**仅含 `LocalDate.now()`**（无 `LocalDateTime.now()`）的文件（如 `TestErpPurDashboard`/`TestErpCsEntitlement`/`TestErpSalDashboard` 等共 ~96 处）。这些 `LocalDate.now()` 残留**不在本期范围**（本期严格承接 2200-1 Deferred「测试代码 `LocalDateTime.now()` 修复（32 处）」），归 Deferred（见下）。
- **测试代码残留缺口（实时 grep，本期范围）**：14 个测试文件含 `LocalDateTime.now()`，共 32 处：
  - `module-contract/erp-ct-service/src/test/.../TestErpCtESignature.java`（1）
  - `module-crm/erp-crm-service/src/test/.../TestErpCrmEventReminderTimeline.java`（1）
  - `module-crm/erp-crm-service/src/test/.../TestErpCrmSequenceAndFunnel.java`（5）
  - `module-cs/erp-cs-service/src/test/.../TestErpCsSlaNotification.java`（3）
  - `module-cs/erp-cs-service/src/test/.../TestErpCsTicketSlaCsat.java`（8）
  - `module-inventory/erp-inv-service/src/test/.../TestErpInvFinanceReversalWriteback.java`（2）
  - `module-maintenance/erp-mnt-service/src/test/.../TestErpMntDashboard.java`（2）
  - `module-maintenance/erp-mnt-service/src/test/.../TestErpMntDowntimeAndE2E.java`（2）
  - `module-notify/erp-notify-service/src/test/.../TestErpSysNotificationDispatch.java`（1）
  - `module-purchase/erp-pur-service/src/test/.../TestErpPurFinanceReversalWriteback.java`（2）
  - `module-quality/erp-qa-service/src/test/.../dashboard/TestErpQaDashboardSpc.java`（1）
  - `module-quality/erp-qa-service/src/test/.../spc/TestErpQaSpcCapability.java`（1）
  - `module-quality/erp-qa-service/src/test/.../spc/TestErpQaSpcOutOfControl.java`（1）
  - `module-sales/erp-sal-service/src/test/.../TestErpSalFinanceReversalWriteback.java`（2）
- **使用模式（两类）**：(a) 测试夹具构造——`LocalDateTime.now().plusHours(8)` 作为相对当前时刻的输入（如 SLA deadline、通知触发时间）；(b) 断言边界——`LocalDateTime before = LocalDateTime.now(); ... assertTrue(x.isAfter(LocalDateTime.now()))`。两类均应替换为 `CoreMetrics.currentDateTime()`，使测试与生产代码共享同一 `CoreClock` 时间源。
- **conventions 缺口**：`docs/context/conventions.md` 无「时间 API 使用约定」段（bug 文档 :受影响的工件 明示需增补「禁止直接 `LocalDateTime.now()`」规则）。
- **保护区域**：纯测试代码 + conventions 文档，**无 ORM/契约/认证变更**，非 `ask-first`。属 `plan-first`（跨 7+ 模块、>5 文件、>200 行机械替换）。

剩余差距：(1) 14 测试文件 32 处 `LocalDateTime.now()` 未对齐平台 `CoreMetrics` API，与生产代码时间源不一致；(2) `conventions.md` 缺时间 API 使用规约；(3) `LocalDate.now()` 残留（生产 4 处 + 测试 ~96 处）属平行问题，非本期范围（归 Deferred）。

## Goals

- 将 14 个测试文件中全部 32 处 `java.time.LocalDateTime.now()` 替换为平台时间 API `CoreMetrics.currentDateTime()`，使测试与生产代码（已用 `CoreMetrics`）共享 `CoreClock` 时间源。**本期范围严格为 `LocalDateTime.now()`**（承接 2200-1 Deferred 口径）；`LocalDate.now()` 残留归 Deferred。
- 增补 `docs/context/conventions.md` 时间 API 使用规约（禁止生产/测试代码直接调 `LocalDateTime.now()`/`LocalDate.now()`/`Clock.systemDefaultZone()`，统一经 `CoreMetrics`；附注 `LocalDate.now()` 残留清理为平行 Deferred）。
- 验证 14 个受影响模块测试全绿无回归。

## Non-Goals

- **不**替换 `LocalDate.now()`（生产 4 处 + 测试 ~96 处，含 14 本期文件中夹带的 16 处 `LocalDate.now()` 与 ~19 个仅含 `LocalDate.now()` 的文件）——2200-1 Deferred 口径为「测试代码 `LocalDateTime.now()` 修复（32 处）」，`LocalDate.now()` 为平行问题归 Deferred（触发条件见下）。本期 Phase 2 仅替换 `LocalDateTime.now()`。
- **不**引入 `TestClock`/`CoreClock` mock 注入机制（固定时刻断言）——本期仅对齐时间源至平台 API（与生产代码 M-5 修复同口径）；可注入固定时钟属平台能力深化，归 Deferred（触发条件=需断言精确时间戳且 `CoreMetrics.current*()` 仍导致测试边界脆弱时）。
- **不**修改生产代码（生产代码 `LocalDateTime.now()` 已由 2200-1/1915-1 修复完成；`LocalDate.now()` 4 处残留归上述 Deferred）。
- **不**重构测试断言逻辑（仅替换时间源调用，不改变测试覆盖语义；如某断言因时间源对齐后暴露真实脆弱性，记录为 Follow-up 而非本计划范围扩展）。
- **不**触及 `*.orm.xml`/`*.xbiz`/view/`application.yaml`。
- **不**承接 2200-1 另一 Deferred「dao().updateEntity() 剩余域修复」（独立结果面，触发条件未满足）。

## Task Route

- Type: `implementation-only change`（测试代码机械替换 + 约定文档增补；零 ORM/契约/认证变更）
- Owner Docs: `docs/bugs/2026-07-07-1915-localdatetime-now-in-12-domains.md`（缺陷根因与修复方向）、`docs/context/conventions.md`（约定增补目标）、`../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`CoreMetrics` 平台 API 权威）
- Skill Selection Basis: `nop-testing`（测试代码修改 + `JunitAutoTestCase` 基线 + `mvn test` 验证；机械替换非新测试设计，技能用于确保不破坏既有快照/断言语义）。`nop-backend-dev` 不适用（无生产代码变更）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 无 ORM/契约/认证变更，故无 ask-first 保护区域门控

## Execution Plan

### Phase 1 - 残留点精确盘点与替换口径裁决

Status: completed
Targets: 14 个测试文件（grep 精确清单）、`CoreMetrics` API 签名
Skill: `nop-testing`

- Item Types: `Proof | Decision`
- Prereqs: 无

- [x] `Proof`：执行 `rg -n 'LocalDateTime\.now\(\)|LocalDate\.now\(\)|Clock\.systemDefaultZone' --glob '**/src/test/**/*.java'` 产出精确逐行清单（文件:行:代码），核对与本计划 Baseline 列表一致；若实时计数与本计划记录的 14 文件/32 处有偏差，以实时 grep 为准并在此记录差异。
      - Skill: `nop-testing`
- [x] `Decision`：替换口径裁决——`LocalDateTime.now()` → `CoreMetrics.currentDateTime()`（本期唯一替换对象；`LocalDate.now()` 不替换，归 Deferred）。记录选择、残留风险（`CoreMetrics.current*()` 仍返回「当前时刻」非固定时钟，测试边界脆弱性仅缓解为「同源」而非「可固定」——已诚实归 Non-Goal TestClock 注入；`LocalDate.now()` 残留本期不处理——归 Deferred）与替代方案（rejected：一并替换 `LocalDate.now()`——超出 2200-1 Deferred「`LocalDateTime.now()` 修复」口径，且 ~96 处 `LocalDate.now()` 使范围翻倍，应独立承接；rejected：引入 TestClock 固定时刻——超出本期「对齐时间源」结果面）。
      - Skill: `nop-testing`

**Phase 1 执行记录（2026-07-08）：**

- `Proof` 实时 grep 结果：`rg -c 'LocalDateTime\.now\(\)' --glob '**/src/test/**/*.java'` = 14 文件、32 处，与本计划 Baseline 完全一致（无偏差）。逐文件计数：TestErpCtESignature(1)/TestErpCrmEventReminderTimeline(1)/TestErpCrmSequenceAndFunnel(5)/TestErpCsSlaNotification(3)/TestErpCsTicketSlaCsat(8)/TestErpInvFinanceReversalWriteback(2)/TestErpMntDashboard(2)/TestErpMntDowntimeAndE2E(2)/TestErpSysNotificationDispatch(1)/TestErpPurFinanceReversalWriteback(2)/TestErpQaDashboardSpc(1)/TestErpQaSpcCapability(1)/TestErpQaSpcOutOfControl(1)/TestErpSalFinanceReversalWriteback(2)。`Clock.systemDefaultZone` 测试代码 0 命中。
- `Decision` 裁定：唯一替换对象 `LocalDateTime.now()` → `CoreMetrics.currentDateTime()`；`LocalDate.now()` 全部归 Deferred（非本期范围）。残留风险：`CoreMetrics.current*()` 返回「当前时刻」非固定时钟（同源缓解，非可固定）；`LocalDate.now()` 残留归 Deferred。替代方案均 rejected（见上）。
- **Import 路径修正（重要）**：本计划 Phase 2 文本中误写 `import io.nop.core.lang.utils.CoreMetrics;`，经实时核实生产代码（`InvPostingDispatcher.java:12`/`ErpFinPostingProcessor`/`ErpMfgCrpRunJob.java:15` 等）权威路径为 `io.nop.api.core.time.CoreMetrics`。Phase 2 实施采用修正后的正确 import `io.nop.api.core.time.CoreMetrics`，且与既有测试对齐范式（`TestErpSalQuotationToOrder.java:100 assertEquals(CoreMetrics.currentDate(), ...)`）一致。

Exit Criteria:

- [x] 逐行清单落盘（写入本计划 Phase 1 记录段或引用），每行标注替换目标 API；实时 grep 计数与清单一致。
- [x] 替换口径 Decision 记录选择 + 替代方案 + 残留风险。

### Phase 2 - 逐文件替换 + import 对齐

Status: completed
Targets: 14 个测试文件
Skill: `nop-testing`

- Item Types: `Fix`
- Prereqs: Phase 1 清单与口径裁决完成

- [x] `Fix`：逐文件将 `LocalDateTime.now()` → `CoreMetrics.currentDateTime()`，并补 `import io.nop.api.core.time.CoreMetrics;`（若文件未引入）。**不替换同文件中的 `LocalDate.now()`**（归 Deferred）。保留 `import java.time.LocalDateTime;` 仅当文件内无其他 `LocalDateTime` 引用时移除（避免误删）。替换不改变表达式结构（如 `LocalDateTime.now().plusHours(8)` → `CoreMetrics.currentDateTime().plusHours(8)`）。
      - Skill: `nop-testing`

**Phase 2 执行记录（2026-07-08）：**

- 7 文件使用 FQN `java.time.LocalDateTime.now()`（无 LocalDateTime import）：TestErpCtESignature/TestErpInvFinanceReversalWriteback/TestErpPurFinanceReversalWriteback/TestErpQaDashboardSpc/TestErpQaSpcCapability/TestErpQaSpcOutOfControl/TestErpSalFinanceReversalWriteback → 替换为 `CoreMetrics.currentDateTime()`，补 CoreMetrics import，无 LocalDateTime import 需处理。
- 6 文件使用 imported `LocalDateTime.now()` 且 `LocalDateTime` 类型在方法签名/`LocalDateTime.of(...)` 中仍被引用：TestErpCrmEventReminderTimeline/TestErpCrmSequenceAndFunnel/TestErpCsSlaNotification/TestErpCsTicketSlaCsat/TestErpMntDashboard/TestErpMntDowntimeAndE2E → 替换 now() 调用、补 CoreMetrics import、**保留** `import java.time.LocalDateTime;`（类型仍使用）。
- 1 文件 TestErpSysNotificationDispatch：仅 now() 调用引用 LocalDateTime，替换后无其他 LocalDateTime 引用 → 补 CoreMetrics import **并移除** `import java.time.LocalDateTime;`。
- 验证：`rg 'LocalDateTime\.now\(\)' --glob '**/src/test/**/*.java'` = 0 命中；`CoreMetrics.currentDateTime()` 测试代码合计 32 处（含既有 TestErpSalQuotationToOrder 1 处 + 本期新增 32 处的受影响文件）；`mvn test-compile -pl <9 受影响 service 模块> -am` = BUILD SUCCESS（无编译错误；仅有既有 unchecked 警告，非本期引入）。

Exit Criteria:

- [x] 14 文件中 `rg 'LocalDateTime\.now\(\)'` 0 命中（`LocalDate.now()` 允许残留，归 Deferred）；`CoreMetrics` import 已补齐；无编译错误（`mvn test-compile` 受影响模块通过——解除 Phase 3 阻塞的本地化检查）。

### Phase 3 - conventions 增补 + 验证

Status: completed
Targets: `docs/context/conventions.md`、14 受影响模块测试
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 替换落地

- [x] `Add`：`docs/context/conventions.md` 增补「时间 API 使用约定」段——禁止生产/测试代码直接调 `java.time.LocalDateTime.now()`/`LocalDate.now()`/`Clock.systemDefaultZone()`，统一经平台 `CoreMetrics.currentDateTime()`/`currentDate()`/`currentTimeMillis()`（对齐 `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`）；附理由（测试时间源与生产同源 + 未来 `CoreClock` mock 可注入）。**附注**：截至本计划完成，`LocalDate.now()` 仍有残留（生产 4 处 + 测试 ~96 处），归独立 Deferred 清理（见 Deferred 段），非隐藏违规。
      - Skill: none
- [x] `Proof`：执行 `mvn test -pl module-{contract,crm,cs,inventory,maintenance,notify,purchase,quality,sales} -am`（9 个受影响 service 模块），验证 0 failures/0 errors，既有快照/断言语义无回归。
      - Skill: `nop-testing`

**Phase 3 执行记录（2026-07-08）：**

- `conventions.md` 已增补「时间 API 使用约定」段（位于「验证规则」之后），含禁止清单、统一 API、理由、`LocalDate.now()` 残留 Deferred 附注。
- `mvn test`（9 受影响 service 模块，-am）= **BUILD SUCCESS**。14 个被修改测试类逐类 surefire 证据（全部 0 Failures / 0 Errors，合计 78 tests）：
  - TestErpCtESignature(19) / TestErpCrmEventReminderTimeline(5) / TestErpCrmSequenceAndFunnel(9) / TestErpCsSlaNotification(3) / TestErpCsTicketSlaCsat(12) / TestErpInvFinanceReversalWriteback(1) / TestErpMntDashboard(5) / TestErpMntDowntimeAndE2E(4) / TestErpSysNotificationDispatch(5) / TestErpPurFinanceReversalWriteback(2) / TestErpQaDashboardSpc(5) / TestErpQaSpcCapability(3) / TestErpQaSpcOutOfControl(4) / TestErpSalFinanceReversalWriteback(1)。
  - 既有快照/断言语义无回归（时间源对齐为同源替换，未改变表达式结构）。

Exit Criteria:

- [x] `conventions.md` 含「时间 API 使用约定」段且与实现一致。
- [x] 9 受影响模块 `mvn test` 0 failures/0 errors。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c18a8635ffeWIrS8010LpACtn) because 4 BLOCKER + 3 NOTE：
  - B1：Baseline 虚称「生产代码已统一使用平台时间 API」——实时 `rg 'LocalDate\.now\(\)' --glob '**/src/main/**/*.java'` 仍命中 4 处生产残留（PriceRuleEngine:47/ErpHrGapAnalysisBizModel:156/ErpHrDevelopmentPlanBizModel:173,174）。已更正为「`LocalDateTime.now()` 已统一（0 命中）；`LocalDate.now()` 4 处残留非本期范围」。
  - B2：Phase 3 convention「禁止 LocalDate.now()」会立即被 4 处生产残留违反。已更正：convention 附注 `LocalDate.now()` 残留为独立 Deferred，非隐藏违规。
  - B3：14 文件夹带 16 处 `LocalDate.now()`（TestErpCtESignature:6/TestErpMntDashboard:5/TestErpQaDashboardSpc:2/TestErpQaSpcCapability:3），Goal「32 处」低估实际替换量。已收敛：本期严格只替换 `LocalDateTime.now()`（32 处，对齐 2200-1 Deferred 口径），`LocalDate.now()` 全部归 Deferred。
  - B4：Closure Gates grep 仅查 `LocalDateTime.now()` 不含 `LocalDate.now()`，与原 Phase 2 exit（含 LocalDate）不一致。已对齐：Phase 2 exit + Closure Gates 均为 `LocalDateTime.now()` 0 命中（`LocalDate.now()` 允许残留归 Deferred）。
  - N1：~19 个仅含 `LocalDate.now()` 的测试文件边界模糊——已并入 Non-Goal 明确排除。
  - N2：Phase 3 `mvn test` 与 Closure Gates 轻微重复——Phase 3 为测试阶段，模块级 `mvn test` 即其可观察交付，保留（边界合规，执行规则 7 允许末阶段交付验证）。
  - N3：bug 文档 `2026-07-07-1915` 标题「60+ 处」为 M-5 修复前旧值（现生产 0）——本计划 Source 引用该 bug 文档为根因记录，计数以本期实时 grep 为准。
  - 正面确认（无需变更）：Deferred 触发条件已满足（2200-1 completed + Phase 4 M-5 生产代码已修复）；14 文件/32 处 `LocalDateTime.now()` 精确匹配；`Clock.systemDefaultZone` 测试 0 命中；conventions.md 无时间 API 段；非 ORM/非 ask-first；item types/技能标注/Decision 理由/反松弛合规。
- Independent draft review iteration 2: `acceptable as-is` (forEach 草案审查会话) after 逐项复核格式、完整性、范围与结束证据：
  - 格式合规：模板必需段（Current Baseline / Goals / Non-Goals / Task Route / Infrastructure / Execution Plan / Draft Review Record / Closure Gates / Deferred But Adjudicated / Closure）齐全；字段名正确；Phase 结构（Status/Targets/Skill/Item Types/Prereqs/Exit Criteria）有效；item types（Phase 1 `Proof | Decision`、Phase 2 `Fix`、Phase 3 `Add | Proof`）合规；技能标注逐项目落盘。
  - 完整性：Exit Criteria 可测（grep 0 命中 / `mvn test-compile` 通过 / 9 模块 `mvn test` 0 failures）；9 受影响模块与 14 文件覆盖一致（contract/crm/cs/inventory/maintenance/notify/purchase/quality/sales）；Closure Gates 含全仓库 `mvn clean install -DskipTests`（154 模块）+ 9 模块 `mvn test` + grep 门控。
  - 范围单一结果面：`LocalDateTime.now()`（32 处）唯一替换对象；`LocalDate.now()`（生产 4 + 测试 ~96）与 TestClock 注入均显式归 Deferred 并附 Trigger Condition（满足反松弛规则 line 91）。无模糊「and also」扩展。
  - 结束证据：Closure Gates 8 项门控 + Closure 段留独立审计证据占位；Deferred 两项均含 Classification / Why Not Blocking / Successor Required / Trigger Condition。
  - 残留 NOTE（非阻塞，下游结束审计可复查）：Phase 3 exit 的 9 模块 `mvn test` 与 Closure Gates 同命令存在轻微重复——Phase 3 为测试验证阶段，模块级 `mvn test` 即其可观察交付（执行时规则 7 末阶段例外），保留合规。

## Closure Gates

> 本计划为纯测试代码 + 约定文档（零 ORM/契约/认证变更），结束前运行一次受影响模块 + 全工作区验证。

- [x] 范围内行为完成（14 文件 32 处 `LocalDateTime.now()` 替换 + conventions 增补）
- [x] 相关文档对齐（`conventions.md` 时间 API 约定 + bug 文档状态）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test`（9 受影响模块，0 failures/0 errors）+ `rg 'LocalDateTime\.now\(\)' --glob '**/src/test/**/*.java'` 0 命中（`LocalDate.now()` 残留归 Deferred，不在本门控）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### LocalDate.now() 残留清理（生产 4 处 + 测试 ~96 处）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期严格承接 2200-1 Deferred「测试代码 `LocalDateTime.now()` 修复（32 处）」口径。`LocalDate.now()` 为平行问题：生产 4 处（`PriceRuleEngine.java:47`/`ErpHrGapAnalysisBizModel.java:156`/`ErpHrDevelopmentPlanBizModel.java:173,174`）+ 测试 ~96 处（含 14 本期文件夹带的 16 处 + ~19 个仅含 `LocalDate.now()` 的文件）。一并替换使范围翻倍且跨生产/测试两域，应独立承接。`conventions.md` 已附注此残留为独立 Deferred，非隐藏违规。
- Successor Required: `yes`
- Trigger Condition: 下一轮清理周期自动承接；或当某测试因 `LocalDate.now()` 导致 CI 不稳定、或生产 `LocalDate.now()` 与 `CoreMetrics.currentDate()` 跨时区产生偏差时立即修复。

### TestClock / CoreClock 可注入固定时钟

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期仅将测试时间源对齐至平台 `CoreMetrics` API（与生产 M-5 修复同口径），使测试与生产共享 `CoreClock`。可注入固定时钟（断言精确时间戳）属平台能力深化，需 `@NopTestConfig` 扩展或 `ITimeService` 抽象，超出「对齐时间源」结果面。
- Successor Required: `yes`
- Trigger Condition: 当某测试因 `CoreMetrics.current*()` 仍返回「当前时刻」导致断言边界脆弱（如断言精确 `postedAt` 时间戳失败），需固定时钟时。

## Closure

Status Note: 全部 3 Phase 已完成且经独立关闭审计验证通过（无 pending 项）。14 文件 32 处 `LocalDateTime.now()` 已全部替换为 `CoreMetrics.currentDateTime()`；`conventions.md` 时间 API 约定段已增补；9 受影响模块 `mvn test` 0 failures/0 errors；全工作区 `mvn clean install -DskipTests` BUILD SUCCESS。

Closure Audit Evidence:

- Auditor / Agent: 独立关闭审计子代理（新会话 ses_0c11f9655ffexszs0jF2hmzjWb，不重用执行者上下文）。验证方式：rg/find/read 实时仓库，未编辑文件、未重跑 mvn。
- Evidence:
  - **Check 1（替换完整）**：`rg 'LocalDateTime\.now\(\)' --glob '**/src/test/**/*.java'` = 0 命中（基线 14 文件/32 处）。
  - **Check 2/3（新 API + import）**：`rg -c 'CoreMetrics\.currentDateTime\(\)'` = 14 文件/32 处；14 文件均含 `import io.nop.api.core.time.CoreMetrics;`；错误路径 `io.nop.core.lang.utils.CoreMetrics` = 0 命中。
  - **Check 4（import 完整性）**：TestErpSysNotificationDispatch 已移除 unused `LocalDateTime` import；6 个保留类型引用的文件仍保留 import（TestErpCrmEventReminderTimeline:21/TestErpCrmSequenceAndFunnel:33/TestErpCsSlaNotification:23/TestErpCsTicketSlaCsat:22/TestErpMntDashboard:24/TestErpMntDowntimeAndE2E:36）；7 个 FQN 文件无悬空 `LocalDateTime` 引用。
  - **Check 5（范围未越界）**：`LocalDate.now()` 测试残留 = 106 处/23 文件，未被触碰（Deferred 0637-2 承接）。
  - **Check 6（conventions）**：`docs/context/conventions.md:46-51`「时间 API 使用约定」段存在，含 `LocalDate.now()` 残留 Deferred 附注。
  - **Check 8（无范围蔓延）**：`git status` 仅 14 `src/test/*.java` + docs（conventions/bug/log/plan）变更；0 `src/main`、0 orm/xbiz/view。
  - **Check 9（编译证据）**：`target/test-classes` 下存在已编译 `.class`（TestErpCtESignature/TestErpSysNotificationDispatch/TestErpCsTicketSlaCsat/TestErpQaSpcCapability 等）。
  - **构建验证**：`mvn clean install -DskipTests`（154 reactor 模块）BUILD SUCCESS；`mvn test`（9 受影响 service 模块，-am）BUILD SUCCESS，14 被改测试类合计 78 tests / 0 failures / 0 errors。
- Verdict: **CLOSE**——范围严格遵守（仅 `LocalDateTime.now()`），所有门控可满足，Plan Status 已置 `completed`，Closure Gates 全 `[x]`。

Follow-up:

- 无非阻塞跟进项。`LocalDate.now()` 残留（生产 4 + 测试 ~106 处）归独立计划 `docs/plans/2026-07-08-0637-2-localdate-now-cleanup.md` 承接；TestClock 可注入固定时钟归 Deferred（见 Deferred 段，触发条件=需断言精确时间戳且同源仍脆弱时）。
