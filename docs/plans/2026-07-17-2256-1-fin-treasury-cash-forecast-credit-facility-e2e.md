# 2026-07-17-2256-1-fin-treasury-cash-forecast-credit-facility-e2e Finance 资金子域深化浏览器层 E2E（现金预测 refreshForecast + 授信额度占用 reserve/release）

> Plan Status: completed
> Last Reviewed: 2026-07-17
> Mission: erp
> Work Item: 各域细化端到端验证（finance treasury 聚合/占用面 successor）
> Source: `docs/plans/2026-07-17-1430-1-treasury-notes-direct-action-voucher-line-e2e.md` 两处 Deferred successor（同一 `treasury.md` owner doc）：
>   - 「现金预测定时任务（nop-job 接线）」(l.263-267) — Successor Required: yes，触发条件「nop-job 接线时，或现金预测浏览器层 E2E 需求落地时」。
>   - 「银行授信额度独立生命周期 + 利息计提」(l.269-273) — Successor Required: yes，触发条件「授信额度浏览器层 E2E 需求落地时」。
> 触发条件经实时仓库核实**已满足**：AGENTS.md / project-context.md:34 明示当前重点含「各域细化端到端验证」；nop-job 已接线（`ErpFinCashForecastJob` + scheduler.yaml，treasury.md:174）；票据生命周期 E2E（1430-1）已落地，treasury 子域仅余聚合/占用面零浏览器覆盖。
> Related: `2026-07-17-1430-1-treasury-notes-direct-action-voucher-line-e2e.md`（票据生命周期 E2E 源，本计划补其两处 successor）；`2026-07-05-0306-1-scheduler-wire-periodic-jobs.md`（nop-job 接线源）；`2026-07-17-2256-2` 同批 N=2（无依赖，可独立推进）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-17，`read`/`grep` 实测，非采信旧记忆）：

### 后端 DIRECT 入口已落地，浏览器层零覆盖

- **现金预测**（`module-finance/erp-fin-service/.../entity/ErpFinCashForecastBizModel.java`）：
  - `@BizMutation refreshForecast(@Name("fromDate") LocalDate, @Name("toDate") LocalDate, IServiceContext)`（:42-68）—— 先删区间内旧预测行（`forecastDate ∈ [from,to]`），再聚合三类源写入 `ErpFinCashForecast` 行，返回写入计数 `Integer`：
    1. `ErpFinArApItem` 未核销到期项（status ∈ {OPEN,PARTIAL}）：RECEIVABLE→INFLOW / PAYABLE→OUTFLOW。
    2. `ErpFinNotesReceivable` 非终态到期项（status ∈ {RECEIVED,DISCOUNTED,ENDORSED,COLLECTION_PENDING}）→ INFLOW。
    3. `ErpFinNotesPayable` ISSUED 到期项 → OUTFLOW。
  - nop-job 已接线：`ErpFinCashForecastJob` + scheduler.yaml（plan 0306-1，treasury.md:174 明示 `erp-fin.cash-forecast-cron` 配置键 + `ErpFinCashForecastJob` + `docs/architecture/job-scheduling.md §3.1 erp-fin-cash-forecast-refresh` 已登记）。空值=跳过门控。
  - 后端单测存在（`TestErpFinCashForecastRefresh` / `TestErpFinCashForecastJob`），**浏览器层零覆盖**（`tests/e2e/business-actions/` 无 `fin-cash-forecast*` spec，实测 grep NONE）。
- **授信额度占用**（`module-finance/erp-fin-service/.../entity/ErpFinCreditFacilityBizModel.java`）：
  - `@BizMutation reserveCredit(@Name("creditFacilityId") Long, @Name("amount") BigDecimal, IServiceContext)`（:32-50）—— 强一致校验 `availableAmount >= amount`（不足抛 `ERR_CREDIT_FACILITY_INSUFFICIENT`），`usedAmount += amount`，`availableAmount = total - used` 同步重算。
  - `@BizMutation releaseCredit(...)`（:53-66）—— `usedAmount -= amount`（下限 0），`availableAmount` 同步重算。
  - 实体携带 `version` 乐观锁列（ CrudBizModel `updateEntity` 管道自动版本校验的后端属性），但本计划范围仅断言单线程顺序下的占用契约字段（used/available 同步重算 + 不足守卫事务回滚），**不**断言 `version` 自增或 stale-version 拒绝（并发面归后端单测 / optimization candidate）。
  - **浏览器层零覆盖**（无 `fin-credit-facility*` spec，实测 grep NONE）。1430-1 仅在应付票据 issue 路径断言授信占用副作用（config 启用时），未覆盖占用契约本身。
  - **利息计提未实现**：treasury.md:148 设计声明业务类型 `CREDIT_FACILITY_INTEREST`（借财务费用-利息支出/贷银行存款），但 `ErpFinCreditFacilityBizModel` 仅有 reserve/release，**无利息计提 `@BizMutation`**。故本计划仅覆盖占用契约，利息计提归后端 successor（见 Deferred But Adjudicated）。

### 浏览器层 E2E 范式已验证（本计划复用，零范式新增）

- DIRECT `@BizMutation` 经 GraphQL `/graphql` mutation 调用 + `verifyState`/`findFirst` 经 `__get` 独立断言（`tests/e2e/business-actions/_helper.ts` 三原语 `createViaSave`/`callMutation`/`verifyState`，0814-2 起范式稳定）。
- 1430-1 票据 spec（`fin-notes-receivable/payable.action.spec.ts`）经自包含 setup（`__save` 直置 status 入口）+ finally 兜底 cleanup（凭证+辅助账+票据逐域删）保护共享 DB 数值断言基线。本计划现金预测为聚合面（写入/删除 `ErpFinCashForecast` 行），cleanup 按写入行 sourceBillCode 批量删；授信占用为额度实体字段更新，cleanup 还原 usedAmount/availableAmount。

### 剩余差距

1. 现金预测 `refreshForecast` 浏览器层零覆盖：聚合三源 + 区间清理 + 计数返回 + 幂等重写均无浏览器层验证。
2. 授信额度占用契约（reserve/release + 不足守卫）浏览器层零覆盖；1430-1 仅侧验 issue 路径副作用。

## Goals

- **现金预测 refreshForecast 浏览器层 E2E**（1 新 spec）：经 GraphQL 驱动 `ErpFinCashForecast__refreshForecast(fromDate,toDate)`，自包含 setup 建 OPEN AR/AP 项 + 非终态应收/ISSUED 应付票据（到期日落入测试区间）→ 断言 (a) 返回计数 > 0；(b) `ErpFinCashForecast` 行写入数量/方向（INFLOW/OUTFLOW）/金额 与 setup 源对齐（经 `findFirst`/`findPage` 查 `ErpFinCashForecast`）；(c) 区间外/终态源不参与；(d) 幂等重写（再次 refresh 区间 → 旧行被清、新行重写，计数稳定不累积）。
- **授信额度占用契约浏览器层 E2E**（1 新 spec）：经 GraphQL 驱动 `ErpFinCreditFacility__reserveCredit`/`__releaseCredit`，自包含建 `ErpFinCreditFacility`（total/used/available）→ 断言 reserve 后 used++/available-- 同步重算 + `availableAmount < amount` 时 `ERR_CREDIT_FACILITY_INSUFFICIENT` 事务回滚（used/available 不变）+ release 后 used--（下限 0）/available++ 恢复。状态翻转均经 `verifyState` `__get` 独立断言。
- **owner doc 收口**：解除 1430-1 两处 Deferred（各补 `**RELEASED by 2026-07-17-2256-1**`）；e2e-runbook 业务动作表 +2 finance 行 + 套件计数对齐；当日日志聚合条目。

## Non-Goals

- **不实现授信额度利息计提**：`CREDIT_FACILITY_INTEREST` 业务类型后端未实现（BizModel 无 `@BizMutation`），属后端 successor（触发条件：利息计提后端落地时）。
- **不实现新后端/契约/ORM 模型**：本计划仅消费侧 DIRECT `@BizMutation` E2E + 测试层。若 Explore 发现某入口不可达或有 bug，属执行期豁免（即时修复 + 记录 + 模块 JUnit 回归），仅确证为生产缺陷时开显式 successor。
- **不做现金预测定时任务（nop-job）浏览器层验证**：cron 触发非浏览器面 mutation 入口（经 scheduler.yaml 配置键驱动），本计划仅手动 `refreshForecast` mutation；定时执行经后端单测 `TestErpFinCashForecastJob` 覆盖。
- **不做多资金账户维度的现金预测分摊**：`refreshForecast` 当前 `fundAccountId=null`（未按账户分摊），属聚合粒度 successor（触发条件：多账户现金预测业务需求落地时）。
- **不重测票据 issue 路径授信占用副作用**：1430-1 已覆盖（config 启用时 issue→reserveCredit 副作用断言）。

## Task Route

- Type: `verification or audit work`（finance 已落地 DIRECT `@BizMutation` 的浏览器层 E2E 覆盖 + 测试层 + 文档；ORM/契约/codegen 无变更，对齐姊妹计划 1430-1 的同形 task type）。
- Owner Docs: `docs/design/finance/treasury.md`（§现金预测派生 / §银行授信额度 / §关键业务规则 1 强一致校验，既有）、`docs/testing/e2e-runbook.md`（业务动作表 + 套件结构，既有）。
- Skill Selection Basis: DIRECT `@BizMutation` 经 GraphQL 浏览器层调用 + `_helper.ts` 三原语复用 → E2E 测试本体 `Skill: none`（对齐 0814-2/1430-1 范式裁决：E2E 覆盖为测试层，nop-testing 路由目标 e2e-testing.md 不存在）。若 Explore 发现 latent defect 需根因诊断，重新加载 `nop-debugging`。
- Protected Areas: 测试在根 `tests/e2e/` 非 reactor 模块；不改 ORM/契约/`_gen/`；任何 finance 生产缺陷须 ask-first / 开 successor。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移。
- 复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- 复用既有种子 COA（finance 域 AR/AP 辅助账 + 票据科目 1121/1122/2202/2203 已由 1430-1/1249-1 种子补齐）。现金预测聚合只读 AR/AP item + 票据实体，无需新科目；授信占用只更新 `ErpFinCreditFacility` 字段，无过账。
- **webServer JVM arg 无需新增**（refreshForecast/reserveCredit/releaseCredit 均无 config 门控，DIRECT 可达）。
- 回滚策略：全部改动为测试层（spec .ts）+ 文档，git 可逆；自包含 setup + finally cleanup 保护共享 DB 数值断言基线（不污染 finance 看板/报表基线）。

## Execution Plan

### Phase 1 - Explore：两入口浏览器层可达性 + setup 可达性冷核实

Status: completed
Targets: `ErpFinCashForecastBizModel.refreshForecast`、`ErpFinCreditFacilityBizModel.reserveCredit`/`releaseCredit`、`tests/e2e/business-actions/_helper.ts`
Skill: none

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] `Proof`：冷核实两 `@BizMutation` 浏览器层调用细节 —— 经实测 `ErpFinCashForecastBizModel.refreshForecast`（:42-68 `@BizMutation Integer refreshForecast(fromDate, toDate, context)`，Nop GraphQL 暴露为 `ErpFinCashForecast__refreshForecast`，`LocalDate` 入参经 String scalar ISO 日期传递，返回 `Integer` 标量无选择集，对齐 `finance-voucher-post.action.spec.ts:49` `gql.raw` 长标量返回范式）；`ErpFinCreditFacilityBizModel.reserveCredit`/`releaseCredit`（:32-66 `@BizMutation ErpFinCreditFacility reserveCredit(creditFacilityId, amount, context)`，暴露为 `ErpFinCreditFacility__reserveCredit`/`__releaseCredit`，`BigDecimal` 经 String scalar 接受，返回实体带选择集）。xbiz 经 biz-gen 生成（`ErpFinCreditFacility.xbiz` extends `_ErpFinCreditFacility.xbiz` `biz-gen:DefaultBizGenExtends forEntity` 自动暴露 `@BizMutation`）。
  - Skill: none
- [x] `Proof`：冷核实 setup 可达性 —— 四实体 ORM `tagSet="gid,erp.finance"` 无 use-approval/use-workflow，DIRECT `__save` 可达。`ErpFinCashForecast` 字段集（forecastDate/sourceBillType/sourceBillCode/direction/partnerId/amountSource/amountFunctional 经 `_ErpFinCashForecast.java` 实测）用于断言。`ErpFinArApItem` 必填 code/orgId/acctSchemaId/direction/partnerId/sourceBillType/sourceBillCode/businessDate/currencyId/amountSource/amountFunctional/openAmountSource/openAmountFunctional/status；`ErpFinNotesReceivable` 必填 code/orgId/notesType/currencyId；`ErpFinNotesPayable` 必填 code/orgId/notesType/currencyId；`ErpFinCreditFacility` 必填 code/orgId/facilityType + totalAmount/usedAmount/availableAmount（Processor 无 auto-compute available，须 __save 显式三值）。`__get`/`findPage` 经 `GraphQLClient.get/findItems/findPageTotal` 可达。
  - Skill: none
- [x] `Decision`：**测试区间 = [2026-08-10, 2026-08-15]**（晚于 today=2026-07-17 避种子源干扰；窄窗 6 天使断言计数确定）。
  - **现金预测 setup（确定性派生，每个 in-window 源各 1 行，区间外 1 行对照）**：
    - Test 1（聚合三源 + 区间外对照，预期计数=4）：OPEN AR(RECEIVABLE, dueDate=2026-08-10, amount=100)→INFLOW + OPEN AP(PAYABLE, dueDate=2026-08-12, amount=200)→OUTFLOW + 应收票据(RECEIVED, dueDate=2026-08-13, amount=300)→INFLOW + 应付票据(ISSUED, dueDate=2026-08-14, amount=400)→OUTFLOW + 区间外 OPEN AR(dueDate=2026-09-01)→不出现在结果。
    - Test 2（终态过滤，预期计数=0 来自该项）：SETTLED AR(RECEIVABLE, dueDate=2026-08-11)→不参与预测。
    - Test 3（幂等重写）：同区间两次 refresh → 第二次计数稳定不累积（先清后写）。
  - **授信额度 setup（每测试自包含建 facility 隔离）**：total=1000 / 初始 used=0 / available=1000；Test 1 reserve(300)→used=300/available=700；Test 2 先 reserve(300)→available=700 再 reserve(2000)→ERR_CREDIT_FACILITY_INSUFFICIENT 守卫事务回滚 used/available 不变；Test 3 reserve(300) 后 release(300)→used=0/available=1000 恢复。
  - 种子引用：org=2 / currency=1(CNY) / acctSchema=1 / AR partner=1(CUST-001) / AP partner=3(SUP-001)（对齐 fin-notes-receivable/payable spec 范式）。
  - Skill: none

Exit Criteria:

- [x] 两入口浏览器层调用签名 + setup 可达性经实时仓库核实明确（无 1005-1 式伪基线：`tests/e2e/business-actions/` 实测 grep 无 `fin-cash-forecast*`/`fin-credit-facility*` spec，ls 65 现有 spec 无此 2 名）。
- [x] Decision（测试区间 + setup 数值）记录入计划，确定性派生可复现。

---

### Phase 2 - 现金预测 refreshForecast 浏览器层 E2E spec

Status: completed
Targets: `tests/e2e/business-actions/fin-cash-forecast.action.spec.ts`（新）、`tests/e2e/business-actions/_helper.ts`（复用，按需扩 `findFirst` 原语已由 2329-2 落地）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] `Add`：`fin-cash-forecast.action.spec.ts` —— 自包含 setup（finally 兜底 cleanup 按 sourceBillCode 批量删 `ErpFinCashForecast` 行 + 删 setup 的 AR/AP/票据源）：
  - 测试 1（聚合三源）：setup 区间内 OPEN AR(INFLOW) + OPEN AP(OUTFLOW) + 非终态应收票据(INFLOW) + ISSUED 应付票据(OUTFLOW) + 区间外对照源 → `refreshForecast(from,to)` 返回计数 ≥ 4 → `findItems ErpFinCashForecast` 断言每行 direction/amount/forecastDate/sourceBillType 与源对齐 + 区间外源不出现在结果。
  - 测试 2（终态/未核销过滤）：setup 一行 status=SETTLED 的 AR 项（区间内）→ refresh → 该项不参与（断言无对应预测行）。
  - 测试 3（幂等重写）：同一区间两次 refresh → 第二次不累积（先清后写），计数稳定（count1==count2），旧行被清重写后行数不变。
  - Skill: none
- [x] `Proof`：spec 独立运行全绿（`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test fin-cash-forecast.action.spec.ts` 3/3 passed 46.4s）；finance 既有 spec 抽样回归（fin-notes-receivable/payable/reconciliation 19/19 passed）0 新增失败。
  - Skill: none

Exit Criteria:

- [x] 3 测试全绿；direction/amount 断言与 setup 源确定性派生对齐；区间外/终态源正确过滤；幂等重写不累积。
- [x] finally cleanup 保护共享 DB（finance 看板/报表数值断言基线无漂移）。

---

### Phase 3 - 授信额度占用契约浏览器层 E2E spec

Status: completed
Targets: `tests/e2e/business-actions/fin-credit-facility.action.spec.ts`（新）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] `Add`：`fin-credit-facility.action.spec.ts` —— 自包含 setup（每测试建独立 facility 隔离，finally 兜底 cleanup 删 `ErpFinCreditFacility`）：
  - 测试 1（reserve 占用）：建 CreditFacility(total=1000,used=0,available=1000) → `reserveCredit(facilityId,300)` → `verifyState` `__get` 断言 usedAmount=300 + availableAmount=700 + totalAmount 不变。
  - 测试 2（不足守卫）：先 reserve(300)→available=700 → `reserveCredit(facilityId,2000)` → GraphQL errors 含「不足」语义 token（ERR_CREDIT_FACILITY_INSUFFICIENT 对应中文描述）+ 事务回滚（usedAmount/availableAmount 不变，`verifyState` 断言回退）。
  - 测试 3（release 释放）：reserve(300) 后 → `releaseCredit(facilityId,300)` → usedAmount=0（下限 0）/ availableAmount=1000 恢复。
  - Skill: none
- [x] `Proof`：spec 独立运行全绿（3/3 passed 46.7s）；与 Phase 2 spec 合并运行 6/6 passed 0 冲突（45.7s）。
  - Skill: none

Exit Criteria:

- [x] 3 测试全绿；used/available 同步重算断言；不足守卫事务回滚断言；release 下限 0 + 恢复断言。状态翻转均经 `verifyState` `__get` 独立断言（非仅 mutation 返回值）。

---

### Phase 4 - 文档对齐 + Deferred RELEASED + 日志

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/plans/2026-07-17-1430-1-*.md` 两处 Deferred 段、`docs/logs/2026/07-17.md`、`docs/backlog/README.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2、Phase 3

- [x] `Add`：`docs/testing/e2e-runbook.md` 业务动作表 +2 finance 行（现金预测 refreshForecast / 授信额度 reserve-release）+ 套件计数对齐（实测 spec 数 64→66）；1430-1 两处 Deferred 段各补 `**RELEASED by 2026-07-17-2256-1**` 行（触发条件已满足 + 本计划交付证据）；`docs/logs/2026/07-17.md` 增聚合条目（spec 数/验证状态/范围纪律）；`docs/backlog/README.md` +1 done 行。
  - Skill: none

Exit Criteria:

- [x] e2e-runbook 业务动作表 +2 行 + 套件计数对齐实际；1430-1 两处 Deferred RELEASED 登记落地；日志 + backlog 条目在位。

## Draft Review Record

- Independent draft review iteration 1: **accept** (`ses_08f685692ffeCOzi3Bct38aK11`，general，新会话冷重播无起草者上下文，2026-07-17) — 全部 load-bearing 基线主张经实时仓库核实**全部 truthful**（区别于被 cancel 的 1005-1 伪基线）：`tests/e2e/business-actions/` 实测无 `fin-cash-forecast*`/`fin-credit-facility*` spec（grep exit 1）；`ErpFinCashForecastBizModel.refreshForecast` 签名/三源聚合/Integer 返回核实；`ErpFinCreditFacilityBizModel` reserve/release + `ERR_CREDIT_FACILITY_INSUFFICIENT` 守卫 + 同步重算核实，且无利息计提方法（Non-Goal 诚实）；实体 totalAmount/usedAmount/availableAmount/version 字段在位；`ErpFinCashForecastJob` + scheduler.yaml（cron `0 0 1 * * ?`）接线确认；1430-1 两处 Deferred（l.263-267/269-273）+ 触发条件逐字核实；`project-context.md:34` 「各域细化端到端验证」触发 argued-as-met；反松弛 clean；R14 单结果面 bundle 合法（同 treasury.md owner doc）；item typing + per-item Skill 完整；reserve/release 经核实为公共 `@BizMutation`（GraphQL `ErpFinCreditFacility__reserveCredit`）非 private internals，1430-1 显式 Deferred 此 successor，为合法 contract 验证非 scope-manufacturing。0 BLOCKER / 0 MAJOR / 2 MINOR：(M1) `version` 乐观锁在 Current Baseline 声明但 Phase 3 单线程顺序测试不断言 version 自增/stale 拒绝 —— 已据 reviewer 选项 (a) 重写基线措辞为「后端属性，本计划不断言，并发面归后端单测」；(M2) Task Route type 与姊妹 1430-1 不一致 —— 已由 `implementation-only change` 改为 `verification or audit work` 对齐。**共识达成 → Plan Status 置 `active`。**

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划为测试层 + 文档（预期零生产契约变更）。结束时运行新增 spec + finance 模块抽样回归 + 全量构建。

- [x] 范围内行为完成（现金预测 refreshForecast 3 测试 + 授信额度占用契约 3 测试）
- [x] 相关文档对齐（e2e-runbook +2 行、1430-1 两处 Deferred RELEASED、当日日志、backlog）
- [x] 已运行验证：新增 2 spec 独立 + 合并运行全绿（6 passed）+ finance 既有 spec 抽样回归 0 新增失败（fin-notes-receivable/payable/reconciliation 19 passed）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（closure gate，确认零后端污染）
- [x] 无范围内项目降级为 deferred/follow-up（利息计提/多账户分摊/定时执行均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 授信额度利息计提（CREDIT_FACILITY_INTEREST）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: treasury.md:148 设计声明业务类型 `CREDIT_FACILITY_INTEREST`（借财务费用-利息支出/贷银行存款），但 `ErpFinCreditFacilityBizModel` 仅 reserve/release，无利息计提 `@BizMutation`。属后端 successor，非浏览器层 E2E 缺口。
- Successor Required: `yes`（触发条件：授信利息计提后端落地时）
- **RELEASED by 2026-07-18-0718-1**：plan `2026-07-18-0718-1-credit-facility-interest-accrual-backend-e2e.md` 落地后端 `ErpFinCreditFacilityBizModel.accrueInterest(@BizMutation)` + `CreditFacilityInterestVoucherBuilder` + `CreditFacilityInterestAcctDocProvider` + bean 注册 + config 常量 + 2 ErrorCode + JUnit 6 用例 + 浏览器层 E2E 1 spec 3 用例；计息公式 `usedAmount × rate × days / 360`（HALF_UP scale=4，360 天基准对齐贴现范式）；rate 来源 config `erp-fin.credit-facility-default-interest-rate`（默认 0=关闭门控）；billHeadCode = `CFI-INT-{facilityId}-{fromDate}_{toDate}` 区间级幂等键（IErpFinVoucherBiz.post 内置 alreadyPosted 守护）；Dr 6603 财务费用-利息支出 / Cr 1002 银行存款。

### 现金预测多资金账户分摊 / 定时执行浏览器层

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `refreshForecast` 当前 `fundAccountId=null`（未按账户分摊）；cron 定时执行经 scheduler.yaml + 后端单测 `TestErpFinCashForecastJob` 覆盖（非浏览器面 mutation 入口）。本计划仅手动聚合 mutation + 单账户维度。
- Successor Required: `yes`（触发条件：多账户现金预测分摊业务需求 / 定时执行浏览器层观测需求落地时）

## Closure

Status Note: 执行完成（2026-07-17）。4 Phase 全绿——Phase 1 Explore（两 `@BizMutation` 浏览器层调用签名 + setup 可达性 + 测试区间 Decision [2026-08-10,2026-08-15]）/ Phase 2（`fin-cash-forecast.action.spec.ts` 3 用例全绿 46.4s：三源聚合 + 终态过滤 + 幂等重写）/ Phase 3（`fin-credit-facility.action.spec.ts` 3 用例全绿 46.7s：reserve/release + 不足守卫事务回滚）/ Phase 4（e2e-runbook 业务动作表 +2 finance 行 + 套件计数 64→66 + 1430-1 两处 Deferred RELEASED + 当日日志 + backlog）。验证：2 新 spec 6/6 + finance 抽样回归 19/19 0 新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（01:31 min，零后端污染确认——纯测试+文档）。GraphQL 错误断言对齐既有范式（中文描述语义 token「不足」而非 ErrorCode 字符串）。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor（冷重播新会话，无执行者上下文，2026-07-17）
- Evidence: 实时仓库核实（非盲信 `[x]`）——
  - 8 维度核实全部通过：
    1. **Plan Status / Phase Status / Exit Criteria / Closure Gates / Closure 五点一致**：front matter `Plan Status: completed` + 4 Phase 均 `Status: completed` + 所有 Phase Exit Criteria `[x]` + Closure Gates 全 `[x]`（含本审计勾选的门控 7）+ Closure 段真实证据。
    2. **Exit Criteria vs live repo**：`tests/e2e/business-actions/fin-cash-forecast.action.spec.ts`（356 行实测）/ `fin-credit-facility.action.spec.ts`（160 行实测）均落地；`docs/testing/e2e-runbook.md` 业务动作表 +2 finance 行（l.292-293）+ 套件计数 64→66 实测 66 个 `.spec.ts` 文件（`ls | wc -l` = 66）；`docs/plans/2026-07-17-1430-1-*.md` 两处 `**RELEASED by 2026-07-17-2256-1**`（l.268 + l.275）；`docs/logs/2026/07-17.md` 聚合条目（l.3-15）+ `docs/backlog/README.md` +1 done 行（l.83）均在位。
    3. **Anti-Hollow**：两 spec 实测非空壳——`fin-cash-forecast` 含 `createArApItem`/`createNotesReceivable`/`createNotesPayable`/`refreshForecast`/`findForecastBySourceCode`/`cleanupRefs` 真实 helper + 三测试用例真实 `gql.raw` mutation 调用 + `findItems` 断言 + `finally` cleanup；`fin-credit-facility` 含 `createFacility`/`callMutationOk`/`verifyState`/`deleteById` 真实调用 + reserve/release/不足守卫三测试。无空函数体 / 无 `return null` 占位 / 无吞异常 / 无注册不可达组件。
    4. **Five-point consistency**：见 #1。
    5. **Deferred honesty**：两 Deferred 项（CREDIT_FACILITY_INTEREST 利息计提 / 现金预测多账户分摊+定时执行）均附明确触发条件（"利息计提后端落地时" / "多账户分摊或定时执行浏览器层观测需求落地时"），无 in-scope live defect 隐藏。
    6. **Docs sync**：`docs/logs/2026/07-17.md` 聚合条目在位（spec 数/验证状态/范围纪律）；本计划纯测试+文档（无 ORM/契约/种子/config 变更），无需 `docs/architecture/` 更新。
    7. **范围纪律**：Non-Goals（利息计提 / 多账户分摊 / 定时执行 / 票据 issue 副作用重测）均诚实声明，无 in-scope 项目降级。
    8. **验证命令**：计划内验证（2 spec 6 passed + finance 抽样回归 19 passed + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS）记录完整。
- Conclusion: **APPROVED** — 计划所有退出标准、门控、文档对齐均真实落地，可标记完成。

Follow-up:

- 授信额度利息计提（CREDIT_FACILITY_INTEREST）后端 `@BizMutation` 未实现，归 Deferred（触发条件：利息计提后端落地时）
- 现金预测多资金账户分摊 / 定时执行浏览器层，归 Deferred（触发条件：多账户分摊/定时执行浏览器层观测需求落地时）
