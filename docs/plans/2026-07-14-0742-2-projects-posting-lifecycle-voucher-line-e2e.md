# 2026-07-14-0742-2-projects-posting-lifecycle-voucher-line-e2e projects 工时/结算过账生命周期 + 凭证行数值浏览器层 E2E

> Plan Status: completed
> Mission: erp
> Work Item: projects-posting-lifecycle-voucher-line-e2e
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-14-0215-1-assets-direct-action-e2e.md` 同形 Deferred「全 18 域全业务动作覆盖（DIRECT 域剩余）」（Successor Required: yes，触发条件「按域推进剩余 DIRECT 业务动作浏览器层覆盖时」——**已满足**：projects 域仅 Task DAG 门控 spec，工时过账 + 结算过账为 DIRECT `@BizMutation` 浏览器层可达但零 E2E）+ 0704-1 凭证行数值断言范式；AGENTS.md 当前项目重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-10-0704-1-voucher-line-numeric-assertion-e2e.md`（凭证行数值断言范式）、`docs/plans/2026-07-09-2004-1-business-action-e2e-maintenance-projects-quality.md`（projects Task DAG E2E，不含工时/结算过账）、`docs/design/projects/cost-collection.md`（§4.2 工时归集与过账同事务）
> Audit: required

## Current Baseline

### 已实现

- **projects 域业务动作 E2E**：仅 `projects-task.action.spec.ts`（Task 4 态 + 前驱 DAG 门控，2004-1）。工时单 `ErpPrjTimesheet` 与项目结算 `ErpPrjProjectSettlement` 的过账生命周期**零浏览器层 E2E**。
- **工时过账后端**（completed）：`ErpPrjTimesheetBizModel` DIRECT 状态机 `submit → approve(tryPost→posted=true) → cancel(reverse 若 posted)`；`TimesheetPostingDispatcher` 借项目成本科目 / 贷应付职工薪酬。
  - 借方科目 = 项目类型 `defaultSubjectId`（**必需**，缺失抛 `ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED`）
  - 贷方科目 = config `erp-prj.default-payroll-subject-id`（**必需**，缺失抛 `ERR_PAYROLL_SUBJECT_NOT_CONFIGURED`）
  - 过账业务类型 = `PROJECT_COST_COLLECTION`；同事务 `costAggregator.aggregateFromTimesheet` 回写项目 actualCost
- **结算过账后端**（completed）：`ErpPrjProjectSettlementBizModel` DIRECT `createSettlement(projectId, settlementType) → submit → approve(doPost→tryPost→posted) → reverseSettlement(reverse)`；config `erp-prj.settlement-require-approval` 门控是否需审批。
  - `ProjectSettlementAcctDocProvider` 按 settlementType 产凭证：CLOSE = Dr 1601 固定资产 / Cr 1603 在建工程；FINAL/INTERIM = Dr 5101 项目成本 / Cr 6001 主营业务收入 + 4103 本年利润（损益平衡）
- **凭证行数值断言范式**：`findVoucherIdByBillCode` + `assertVoucherLines` 两原语就位（0704-1 确立）。

### 凭证行断言覆盖现状

finance/inventory/manufacturing/quality/maintenance/purchase/sales 域凭证行断言均已覆盖；assets 由 `2026-07-14-0742-1` 承接。**projects 域过账凭证行断言完全缺失**（工时/结算均无 posted E2E，遑论凭证行）。

### 种子 COA 缺口

`erp_md_subject.csv` 现状（0215-1 后）：含 1001/1002/1131/1401/1403/1601/1602/1603/2221/6001/6301/6602/6711/1410-1417 等。**缺**：
- `5101` 项目成本（工时借方 / 结算 FINAL 借方）— 需新增
- `2211` 应付职工薪酬（工时贷方）— 需新增
- `4103` 本年利润（结算 FINAL/INTERIM 损益平衡）— 需新增

6001（id=7）既有；1601/1603 由 0215-1 补齐。

### 剩余差距

| 功能 | UC | 后端 | CRUD | 业务动作 E2E | 凭证行断言 | 差距 |
|------|----|----|------|------------|-----------|------|
| 工时过账 | 项目成本归集 | ✅ | ✅ | ❌ | ❌ | submit→approve→posted 生命周期 + PROJECT_COST_COLLECTION 凭证行 |
| 结算过账 | 项目结算结转 | ✅ | ✅ | ❌ | ❌ | createSettlement→approve→posted + reverse + 凭证行 |

## Goals

- 新增 projects 工时单过账生命周期浏览器层 E2E：`submit → approve(posted=true) → PROJECT_COST_COLLECTION 凭证行精确数值断言 → cancel(reverse 红字凭证行)`
- 新增 projects 结算过账生命周期浏览器层 E2E：`createSettlement(CLOSE) → approve(posted=true) → 凭证行精确数值断言 → reverseSettlement(红冲)`
- 复用 `findVoucherIdByBillCode` + `assertVoucherLines` 原语 + 0814-2 三原语（createViaSave/callMutation/verifyState）

## Non-Goals

- **结算 FINAL/INTERIM 损益结转凭证行断言**——CLOSE 资本化路径（Dr 1601/Cr 1603）作代表验证；FINAL/INTERIM（5101/4103/6001 三行损益平衡）归 successor（触发条件：项目损益结转浏览器层 E2E 需求落地时）。
- **工时成本归集 actualCost 精确数值断言**——本计划聚焦 GL 凭证行；actualCost 回写属域表断言，归数值断言层 successor
- **项目预算控制 hook E2E**——runBudgetCheckHook 在 submit 时触发，与 finance 预算控制（0606-1）同范式，归 successor
- **生产代码/契约/ORM 模型变更**——纯测试 + 种子 COA 加性追加 + webServer JVM arg

## Task Route

- Type: `verification or audit work`（新增浏览器层 E2E 覆盖既有后端过账，纯测试 + 种子 + config）
- Owner Docs: `docs/design/projects/cost-collection.md`（§4.2 工时归集）、`docs/design/projects/settlement.md`（结算结转科目）、`docs/design/projects/use-cases.md`
- Skill Selection Basis: 新增 business-actions E2E + 凭证行断言，复用既有 helper 原语与 0814-2 范式，无新 BizModel/ORM 工作→`Skill: none`（既有 e2e 范式无需 nop-testing 全流程，helper/三原语已就位）；NOP 平台文档无需读取（零平台交互）

## Infrastructure And Config Prereqs

- webServer JVM arg（`playwright.config.ts`）追加：`-Derp-prj.default-payroll-subject-id=2211`（工时贷方科目，**值为科目编码非种子行 ID**——config 键名含 `-id` 后缀但消费方 `ErpPrjConfigs.defaultPayrollSubjectCode()` 按 code 解析直接填 `BILL_DATA_CREDIT_SUBJECT_CODE`，与借方 `projectType.defaultSubjectId`=数字 ID（经 `resolveSubjectCode` 解析）的非对称性需在 spec setup 注释；缺失抛 ERR_PAYROLL_SUBJECT_NOT_CONFIGURED）；结算审批门控 `erp-prj.settlement-require-approval` 默认 true（`ErpPrjConfigs.DEFAULT_SETTLEMENT_REQUIRE_APPROVAL`），无需显式追加 arg
- 种子 COA 加性追加：`erp_md_subject.csv` +3 行（5101 项目成本 COST DEBIT / 2211 应付职工薪酬 LIABILITY CREDIT / 4103 本年利润 EQUITY CREDIT）
- 无外部服务依赖

## Execution Plan

### Phase 1 - 工时单过账生命周期 + PROJECT_COST_COLLECTION 凭证行断言

Status: completed
Targets: `tests/e2e/business-actions/projects-timesheet-posting.action.spec.ts`（新增）、`app-erp-all/.../_init-data/erp_md_subject.csv`、`playwright.config.ts`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: none

- [x] `Decision | Explore`: 工时 setup 可达性裁定——self-contained 建 `ErpPrjProjectType`(defaultSubjectId → 解析为 5101) + `ErpPrjProject`(projectTypeId) + `ErpPrjTask`(projectId) + `ErpHrEmployee`(optional costRate 源) + `ErpPrjTimesheet`(taskId/projectId/hours)。核实 `CostRateResolver.resolve` 在无 employee rate 时是否回退 config `erp-prj.default-labor-cost-rate`（若回退则加 webServer arg，否则经 employee rate 确定性驱动）。costAmount = hours × costRate 确定性派生。任何 config/字段不可达项降级并记录理由。
  - **裁定（实测）**：costAmount 确定性经 `timesheet.costRate` 单填驱动（CostRateResolver 优先级 1，>=0 即采纳），**无需** employee rate / config `erp-prj.default-labor-cost-rate` 回退。costAmount = hours(8) × costRate(100) = 800。userId 复用种子 employee id=1（HR-EMP-001）。**额外必填发现**：timesheet.orgId 虽 ORM 非 mandatory，但 TimesheetPostingDispatcher 经 `resolveAcctSchemaId(timesheet.getOrgId())` 解析 acctSchema，orgId=null → schemas=0 → voucherId=null → posted=false；故 setup 须显式置 `orgId=2`（种子 acct schema ACCT-FIN-01 org=2 → schemas=1 voucher 生成）。
  - Skill: none
- [x] `Add`: 种子 COA +2 行（5101/2211，对齐 0215-1 加性范式，纯加法不回归）。实际追加 3 行（5101 id=32 / 2211 id=33 / 4103 id=34）——4103 一次性补齐（Phase 2 共享 + Final/INTERIM 损益平衡科目完备化，加性无回归）。
  - Skill: none
- [x] `Add`: webServer JVM arg `-Derp-prj.default-payroll-subject-id=2211`（科目编码，非种子行 ID）
  - Skill: none
- [x] `Add | Proof`: 新增 `projects-timesheet-posting.action.spec.ts`——createViaSave 建链 → `submit`(DRAFT→SUBMITTED) → `approve`(SUBMITTED→APPROVED + posted=true) → `verifyState` 经 `__get` 断言 posted → `findVoucherIdByBillCode` + `assertVoucherLines` 断言 **Dr 5101 / Cr 2211**（amount = costAmount 确定性）→ `cancel`(APPROVED→UNSUBMITTED + reverse 红冲) → 红字凭证行同向取负断言。含 1-2 非法迁移守卫（DRAFT→approve / 已 approve→submit）
  - 实测全绿（1 passed）：submit→SUBMITTED + costRate/costAmount 派生 + approve→APPROVED+posted + NORMAL 凭证 Dr 5101=800/Cr 2211=800 + 2 非法守卫（UNSUBMITTED→approve / APPROVED→submit）+ cancel→UNSUBMITTED+posted=false + REVERSAL 红字 Dr 5101=-800/Cr 2211=-800。后端日志确认 voucherId=100010 schemas=1 + 红冲 voucherId=100016。
  - Skill: none

Exit Criteria:

> 证明工时过账生命周期 E2E 交付且 posted + 凭证行断言全绿，解除后续 Phase 无依赖（两 Phase 独立，但共享种子/config）。

- [x] 工时 submit→approve→posted + PROJECT_COST_COLLECTION 凭证行 Dr 5101/Cr 2211 断言通过
- [x] cancel reverse 红字凭证行同向取负断言通过

### Phase 2 - 结算过账生命周期 + 凭证行断言

Status: completed
Targets: `tests/e2e/business-actions/projects-settlement-posting.action.spec.ts`（新增）、`app-erp-all/.../_init-data/erp_md_subject.csv`、`playwright.config.ts`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1（共享种子/config 加性追加；两 Phase 独立 spec）

- [x] `Decision | Explore`: 结算 CLOSE 路径可达性裁定——(a) **PnL 快照硬前置**：`createSettlement` 调 `pnlBiz.getProjectPnl(projectId)`，缺 `CALCULATED` 快照抛 `ERR_SETTLEMENT_PNL_SNAPSHOT_MISSING`（`ErpPrjProjectSettlementProcessor.java:65-69`）；`getProjectPnl`→`ProjectPnlCalculator.findLatestCalculated` **不惰性计算**（无快照返 null）。故测试流须先调 `IErpPrjProjectPnlBiz.refreshPnl`（@BizMutation）产生快照，再 createSettlement。(b) **CLOSE approve 跨域副作用**：`approve`→`createAndActivateAsset` 经 `IErpAstAssetBiz.save` 跨域**持久化真实资产卡片**（`reverseSettlement` 仅 `rollbackAssetIfNeeded` 回退 status=DRAFT，**不删除卡片**）——评估资产卡片持久化对 inventory/assets dashboard 基线与幂等清理的影响，必要时 cleanup。(c) createSettlement(CLOSE) 需项目具备 finalCost（结算金额来源）；核实 self-contained setup：建项目 + 经 timesheet/费用归集产生成本 → refreshPnl → createSettlement CLOSE 读 finalCost。若 CLOSE 前置链路（refreshPnl + 资产卡片副作用）污染基线风险过高，降级为 FINAL/INTERIM（Dr 5101/Cr 6001 + 4103）并记录 successor。billHeadCode = `settlement.getCode()`（形如 `STL-{projectId}-{millis}`，`ProjectSettlementPostingDispatcher.java:74` 内联 setBillHeadCode，无独立 setter）。
  - **裁定（实测）**：CLOSE 路径全程可达不降级。finalCost 确定性经自包含 `CostCollection(OPEN, docStatus 须 erp-prj/project-status 合法值非 APPROVED) + Line(MATERIAL, amount=1000)` → refreshPnl(projectId,'2026-07-01','2026-07-31') 聚合 totalCost=1000 → createSettlement 读 snapshot.finalCost=1000。资产卡片跨域持久化经 spec cleanup 显式 `deleteById(ErpAstAsset, assetCardId)` + 防御性清理折旧计划清除（reverseSettlement 仅回退 status 不删除卡片，不污染 assets dashboard 基线）。createAndActivateAsset 最小 data map（code/name/orgId/acquisitionDate/originalValue/currentValue/residualValue/status）覆盖 ErpAstAsset 全 mandatory 字段（categoryId 非 mandatory）。
  - Skill: none
- [x] `Add`: 种子 COA +1 行（4103 本年利润，FINAL/INTERIM 损益平衡；若 Phase 2 裁定仅 CLOSE 则 4103 非必需，按 Explore 裁定）。Phase 1 已一次性追加 4103（id=34），Phase 2 共享，无额外变更。
  - Skill: none
- [x] `Add | Proof`: 新增 `projects-settlement-posting.action.spec.ts`——setup 项目 + 成本 → `refreshPnl` → `createSettlement`(CLOSE) → `submit` → `approve`(APPROVED + posted=true) → `verifyState` 断言 posted → `findVoucherIdByBillCode(settlementCode)` + `assertVoucherLines` 断言 **Dr 1601 / Cr 1603**（CLOSE 资本化，amount=finalCost 确定性）→ `reverseSettlement`(红冲) → 红字凭证行断言 + 资产卡片 status 回退断言。含非法迁移守卫。spec cleanup 清理跨域资产卡片（若 Explore 裁定需要）
  - 实测全绿（1 passed）：refreshPnl→CALCULATED+totalCost=1000 + createSettlement(CLOSE)→UNSUBMITTED+transferToAsset+finalCost=1000 + 非法守卫（UNSUBMITTED→approve）+ submit→SUBMITTED + approve→APPROVED+posted+assetCardId(IN_SERVICE,originalValue=1000) + NORMAL 凭证 Dr 1601=1000/Cr 1603=1000 + reverseSettlement→posted=false+asset DRAFT + REVERSAL 红字 Dr 1601=-1000/Cr 1603=-1000。后端日志确认 voucherId=100028 schemas=1 provider=ProjectSettlementAcctDocProvider + 红冲 voucherId=100032。
  - Skill: none

Exit Criteria:

- [x] 结算 createSettlement→approve→posted + CLOSE 凭证行 Dr 1601/Cr 1603 断言通过
- [x] reverseSettlement 红冲凭证行断言通过

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0a220baacffe4m2cTwMgbZHaqT) — 全部 load-bearing 主张经实时仓库核实为真（无 prj 过账 E2E、timesheet/settlement DIRECT @BizMutation 可达、xwf 不涉及浏览器层可达、AcctDocProvider 科目、种子 COA 5101/2211/4103 缺失、helper 原语就位）。**3 MAJOR**：M1 Phase 2 Explore 漏命名 CLOSE 硬前置——`createSettlement` 调 `getProjectPnl` 缺 CALCULATED 快照抛 `ERR_SETTLEMENT_PNL_SNAPSHOT_MISSING`（不惰性计算，须先 refreshPnl）+ CLOSE approve 经 `createAndActivateAsset` 持久化资产卡片（reverseSettlement 仅回退 status 不删除）；M2 webServer arg `default-payroll-subject-id` 值为科目**编码** `2211` 非种子行 ID（消费方按 code 解析，与借方 defaultSubjectId=数字ID 非对称）；M3 Non-Goal FINAL/INTERIM「若 Explore 裁定...可附带覆盖」违反反松弛规则（范围项须唯一状态）。4 MINOR：m1 spec 前缀 `prj-`→`projects-` 对齐既有 `projects-task`；m2 settlement-require-approval arg 冗余（默认 true）；m3 种子 6001 名称「主营业务成本」与 Provider 用法「主营业务收入」不符（FINAL Non-Goal 不阻塞，标记防 successor 误读）；m4 Explore 引不存在方法 `setBillHeadCode`→实为内联 `settlement.getCode()`。**已全部修订**：M1 Explore 增 refreshPnl 硬前置 + 资产卡片副作用 + 测试流插 refreshPnl；M2 arg 改 `=2211`（编码）+ 非对称性注释；M3 删除附带覆盖条款；m1 前缀改 `projects-` + Closure Gates glob 同步；m2/m4 订正。修订后无 Blocker/Major 残留 → 计划 execution-ready。

## Closure Gates

> 完整仓库验证在此处运行一次。

- [x] 范围内行为完成（2 新 spec 工时 + 结算过账生命周期 + 凭证行断言全绿）
- [x] 相关文档对齐（`docs/testing/e2e-runbook.md` 业务动作表 +projects 工时/结算行 + 套件计数 46→48 + webServer JVM arg 段 + 种子 COA 段）
- [x] 已运行验证：`PLAYWRIGHT_PORT=8011 npx playwright test tests/e2e/business-actions/projects-timesheet-posting.action.spec.ts tests/e2e/business-actions/projects-settlement-posting.action.spec.ts --workers=1` 全绿（2 passed）+ `mvn clean install -DskipTests`（种子/零 Java 变更，BUILD SUCCESS 154 模块）+ 回归抽样 projects/assets dashboard value（种子 COA 加性，fresh-DB 后 openProjectCount=1/originalValue 基线无漂移）
- [x] 无范围内项目降级为 deferred/follow-up（FINAL/INTERIM 损益结转为显式 Non-Goal successor，已命名触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 结算 FINAL/INTERIM 损益结转凭证行断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: CLOSE 资本化路径（Dr 1601/Cr 1603）作代表验证结算过账机制；FINAL/INTERIM（Dr 5101/Cr 6001 + 4103 损益平衡三行）为损益结转深化
- Successor Required: `yes`（触发条件：项目损益结转浏览器层 E2E 需求落地时）

### 工时 actualCost 归集精确数值断言

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划聚焦 GL 凭证行；actualCost 回写属域表断言（projects dashboard 数值断言层范畴）
- Successor Required: `no`

## Closure

Status Note: 全 2 Phase 完成（工时 + 结算过账生命周期 + 凭证行数值断言全绿），Closure Gates 全 [x]，独立结束审计 PASS（无阻塞），文本一致性已验证。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0a1ef4c6affeBupdw4hxNMRYsu（general，新会话，无执行者上下文）
- Verdict: PASS（无阻塞项）
- Evidence: 实时仓库核实——两 spec 工件存在且断言真实（timesheet Dr 5101=800/Cr 2211=800 + 红冲 -800；settlement Dr 1601=1000/Cr 1603=1000 + 红冲 -1000）；种子 COA +3 行加性（5101/2211/4103 id=32/33/34）；webServer JVM arg `-Derp-prj.default-payroll-subject-id=2211` 就位；plan 全 [x] 无 [ ] 残留 + 两 Phase Status completed；Deferred But Adjudicated 仅 2 显式项无静默降级；e2e-runbook 业务动作表 +2 projects 行 + JVM arg 段 + 种子 COA 段 + 套件计数 46→48；backlog README ✅ done 行 + 日志条目就位；独立测试运行 `2 passed (15.4s)`；dashboard 非漂移回归 `3 passed (48.2s)`（projects+assets value）；反模式检查全 PASS（setup 置 try 内 + finally null 守卫；git diff 仅测试/种子/config/docs 零 Java/ORM 变更）。

Follow-up:

- 结算 FINAL/INTERIM 损益结转凭证行断言 successor（触发条件见 Deferred）
- 项目预算控制 hook E2E successor（与 finance 预算控制 0606-1 同范式）
