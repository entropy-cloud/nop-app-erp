# 2026-08-18 A4.2.119 — 采购路径物料归集 budgetChecker.check 接入运行时确认（P1-RC-049 successor 落地后回队探查）

> Audit Type: MA4 运行时行为确认（单存疑点，模板定制自 `docs/skills/multi-dimensional-audit-prompt.md`，对齐 A4.2.3/A4.2.79 报告范式）
> Work Item: A4.2.119（A1.35 SP-4，P1-RC-049 successor 落地后采购路径 budgetChecker.check 接入运行时行为）
> Plan: `docs/plans/2026-08-17-0142-3-rc-ma4-a4-2-119-prj-material-budget-runtime-confirmation.md`
> Audit Status: closed
> Last Reviewed: 2026-08-18（计划立项 2026-08-17，执行日 08-18——报告/日志按实际执行日落盘，计划 Target 的 `08-17-*` 前缀为立项日漂移）

---

## 0. 结论（TL;DR）

**维持接受（主路径行为正确闭合，零新 finding，零分级变更，零生产代码变更）。** SP-4 存疑点「采购路径物料归集实现后 budgetChecker.check 接入的实际运行时行为」经既有 R1.61 测试证据逐组断言强度复核 + 实仓重跑（8+4 全绿）全数确认：

- **STRICT 阻断**：采购入库审核（GraphQL 引擎集成层）在 STRICT 超预算时被拒——`ERR_BUDGET_EXCEEDED` 精确 errorCode 比对 + 入库单保持 SUBMITTED（事务回滚语义）+ 零归集行落库。
- **WARNING 放行**：projects Facade 层超预算 WARNING 放行 + 归集行落库 + 全额返回。
- **config 门控**：`erp-prj.material-aggregation-enabled=false` 双侧（Facade/采购链路）零副作用。
- **证据缺口裁决**：**无缺口**（Phase 2 按计划门控标 N/A，理由见 §3）。

roadmap A4.2.119 → done ✅；arm-index P1-RC-049/P1-RC-051 行追加本确认注记。

---

## 1. 存疑点原文与回队链

- **A1.35 §7 SP-4（`docs/audits/2026-08-05-2200-3-rc-ma1-a1-35-projects-f2-budget-dag.md` :258）**：「采购路径物料归集实现后 budgetChecker.check 接入的实际运行时行为（P1-RC-049 successor 落地后，采购入库→项目物料归集同步接 budgetChecker.check 应阻断超预算采购；与 P2-RC-049 承诺项协同）」。
- **运行时验证设计（同行第三列）**：「届时构造采购入库→项目归集（订单行标 projectId）+ 超 budget → 断言 budgetChecker.check 是否阻断 + STRICT 模式是否抛 ERR_BUDGET_EXCEEDED」。
- **MR1 successor 阻塞排除（`2026-08-07-2359-rc-ma4-a4-2-113-123-projects-f1-f2-f3-runtime.md` :26）**：P1-RC-049 物料归集未落地致运行时探查结构不可达，保留 todo 待 MR1 回队。
- **解锁（roadmap :306 注记）**：2026-08-16 RC-R1.61 落地（`docs/plans/2026-08-16-2043-1-rc-mr1-r1-61-prj-material-subcontract-aggregation.md`）——物料归集经 `ErpPrjCostCollectionAggregateMaterialCostProcessor` 接 `budgetChecker.check`，`TestErpPrjMaterialAggregation` STRICT/WARNING 两组 + `TestErpPurReceiveMaterialCostAggregation` 4 组实证 → 本行回队。

## 2. 既有证据断言强度逐组复核（Phase 1）

### 2.1 生产守卫链实仓（只读核验，RC-R1.61 落地形态）

```text
GraphQL mutation ErpPurReceive__approve
  └─ ErpPurReceiveApproveProcessor.approve:33-59          [pur-service]
       ├─ SoD + 状态机 + 业务守卫 + 检验门控
       ├─ triggerIncomingMove（入库移动单，:44）
       └─ collectProjectMaterialCost（同事务，:48）
            └─ ErpPurReceiveProcessor.collectProjectMaterialCost:301-318
                 └─ 逐行解析 receiveLine.orderLineId → orderLine.projectId（null 行级跳过）
                      └─ IErpPrjCostCollectionBiz.aggregateMaterialCost(projectId, amount, 入库单号-行号)
                           └─ ErpPrjCostCollectionAggregateMaterialCostProcessor:31-44   [prj-service]
                                ├─ config 门控 materialAggregationEnabled（:33，关闭返回 0）
                                ├─ null/非正金额守卫（:36）
                                ├─ projectBiz.requireReferenceable（:40，P2-RC-048 单一咽喉，非 OPEN 拒绝）
                                ├─ budgetChecker.check(projectId, amount)（:42，归集行写入前）
                                │    └─ BudgetChecker.check:44-69
                                │         ├─ 无预算（null/≤0）静默返回
                                │         ├─ used = Σ 归集行金额（sumUsedAmount）
                                │         ├─ used + addAmount > total：
                                │         │    ├─ STRICT → throw NopException(ERR_BUDGET_EXCEEDED)
                                │         │    │          携 4 param（projectId/budgetTotal/budgetUsed/amount）
                                │         │    └─ WARNING → LOG.warn 放行
                                └─ materialCostAggregator.aggregateMaterial（幂等写入：
                                     按 sourceBillType+sourceBillCode 去重；head totalAmount 累加；
                                     project.actualCost 增量回写）
```

STRICT 异常经 `@BizMutation` 事务边界传播 → approve 回滚 → 入库单保持 SUBMITTED（`ErpPurReceiveApproveProcessor` 注释 :46-47 显式声明此契约，对齐 L1 UC-PRJ-04「采购审核拒绝该笔归集」）。

### 2.2 ①STRICT 组断言强度

**prj Facade 层 `TestErpPrjMaterialAggregation#testBudgetStrictRejectsOverBudget`（:147-174）**：
- `System.setProperty(CONFIG_BUDGET_CONTROL_MODE, "STRICT")` 显式 STRICT + finally `clearProperty`（无跨测试污染）；
- 场景：预算 1000 < 拟归集 1500（已使用 0）；
- `assertThrows(NopException.class, ...)` + `assertEquals(ErpPrjErrors.ERR_BUDGET_EXCEEDED.getErrorCode(), ex.getErrorCode())`（:165-168）——**errorCode 常量精确比对**（引用同一 `ErrorCode` 定义，杜绝字面量漂移）✓；
- `assertNull(findCollectionLine(PURCHASE_RECEIVE, "PR-MAT-S-001-1"))`（:169-170）——**零归集行落库** ✓；
- 「入库单状态回滚」维度不在此层（prj 测试不经入库单）——由 pur 侧覆盖，分层归属正确。

**pur GraphQL 集成层 `TestErpPurReceiveMaterialCostAggregation#testStrictBudgetRejectsApproval`（:136-165）**：
- STRICT 显式设置 + finally 清理；场景：总预算 30 < 入库行金额 50，订单行标 projectId；
- 经真实 `ErpPurReceive__submitForApproval` + `ErpPurReceive__approve` GraphQL 引擎 RPC（`graphQLEngine.executeRpc(newRpcContext(mutation, ...))`，**非 mock**）；
- `assertEquals(ErpPrjErrors.ERR_BUDGET_EXCEEDED.getErrorCode(), resp.getCode())`（:155-156）——**GraphQL 引擎层响应 errorCode 精确比对**（NopException 经引擎包装进 ApiResponse.code）✓；
- `assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, receive.getApproveStatus())`（:157-159）——**入库单保持 SUBMITTED（事务回滚语义/审批状态不迁移）** ✓；
- `assertNull(findCollectionLine(...))`（:160-161）——**零归集行落库** ✓。

### 2.3 ②WARNING 组断言强度

**prj Facade 层 `testBudgetWarningAllowsOverBudget`（:177-202）**：
- WARNING 显式设置；场景：预算 1000 < 拟归集 1500；
- `assertEquals(0, added.compareTo(new BigDecimal("1500")))`（:194-196）——**放行且全额返回** ✓；
- `assertTrue(findCollectionLine(...) != null)`（:197-198）——**归集行落库** ✓；
- 「审核成功」维度（pur 链路层）：见 §3 组合分析——`testApproveTriggersMaterialAggregation` 运行于默认 WARNING 模式（`ErpPrjConfigs.DEFAULT_BUDGET_CONTROL_MODE = BUDGET_MODE_WARNING`，:9-10）下 approve 成功 + 归集行落库端到端隐式覆盖（预算未超）；WARNING 特有差异分支（超预算判定为 true 后 LOG.warn 放行而非抛错）由本 Facade 层测试直接覆盖。

### 2.4 ③config 关闭组断言强度（双侧零副作用）

- prj `testMaterialAggregationDisabledByConfig`（:228-251）：返回 0（:245）+ 零归集行（:246-247）；
- pur `testMaterialAggregationDisabledByConfig`（:168-192）：`assertEquals(0, resp.getStatus())` 审核正常（:186）+ 零归集行（:187-188）——**链路层零副作用**（关闭归集不阻断业务）✓。

### 2.5 ④集成层真实性（非 mock）

- prj 测试：`@Inject IErpPrjCostCollectionBiz collectionBiz`（真实 IoC bean，`JunitAutoTestCase` + `localDb` 容器）→ 调用穿透生产 Processor `ErpPrjCostCollectionAggregateMaterialCostProcessor` 全守卫链（config 门控 → requireReferenceable → budgetChecker.check → 幂等聚合器），**零 mock**；
- pur 测试：`@Inject IGraphQLEngine graphQLEngine` + `executeRpc(mutation, "ErpPurReceive__submitForApproval"/"ErpPurReceive__approve", ...)`——**完整 GraphQL 引擎集成层**（经 BizModel 解析、事务边界、异常包装全链）。

### 2.6 SP-4 存疑点覆盖核对

| SP-4 验证设计要素 | 覆盖证据 | 判定 |
|---|---|---|
| 构造采购入库→项目归集（订单行标 projectId） | pur `testApproveTriggersMaterialAggregation:80-113`（订单行 projectId seed + submit + approve + 归集行断言 costCategory=MATERIAL/amount=50/归属项目）+ `testStrictBudgetRejectsApproval` 同构造 | ✅ |
| 超 budget | STRICT 组预算 30 < 50（pur）/ 1000 < 1500（prj） | ✅ |
| 断言 budgetChecker.check 是否阻断 | approve 返回 `ERR_BUDGET_EXCEEDED` + 入库单 SUBMITTED + 零归集行（pur :155-161） | ✅ |
| STRICT 模式是否抛 ERR_BUDGET_EXCEEDED | GraphQL 层 resp.getCode() 精确比对（pur :155）+ Facade 层 assertThrows NopException + errorCode 精确比对（prj :165-168），携 4 param（生产 `BudgetChecker:60-64`） | ✅ |

**结论：SP-4 验证设计全要素由既有测试预覆盖，且断言强度满足「errorCode 精确 + 状态回滚 + 零落库」三重断言。**

### 2.7 实仓重跑实测（2026-08-18 03:16 +08:00）

```text
mvn test -pl module-projects/erp-prj-service,module-purchase/erp-pur-service \
  -Dtest='TestErpPrjMaterialAggregation,TestErpPurReceiveMaterialCostAggregation' -DfailIfNoTests=false
→ BUILD SUCCESS（双模块 Reactor SUCCESS）

surefire 报告：
  app.erp.prj.service.TestErpPrjMaterialAggregation        Tests run: 8,  Failures: 0, Errors: 0, Skipped: 0（4.123s）
  app.erp.pur.service.TestErpPurReceiveMaterialCostAggregation Tests run: 4, Failures: 0, Errors: 0, Skipped: 0（4.064s）
```

分域全量（Closure Gates 分支 (b)）：`mvn test -pl module-projects/erp-prj-service,module-purchase/erp-pur-service` → **BUILD SUCCESS，prj-service 172 tests / pur-service 324 tests，0 failures 0 errors**（prj 基线 158 → 172 为 R1.63 +10 / R1.64 +4 后续增量，非本行漂移）。

## 3. 证据缺口裁决（Phase 1 → Phase 2 门控）

**缺口清单：空。** 唯一未直接组合的场景——「pur 链路超预算 + WARNING + approve 成功端到端」——经结构分析**不构成缺口**：

1. **purchase 链路对预算控制模式零分支依赖**：`ErpPurReceiveApproveProcessor.approve:33-59` 与 `ErpPurReceiveProcessor.collectProjectMaterialCost:301-318` 全文无 budget mode 引用（grep `budgetControl|BUDGET_MODE|STRICT|WARNING` 零命中于该两文件），行为仅为「Facade 异常 → 传播回滚 / Facade 正常返回 → 继续」。
2. **组合 = 已覆盖分支的并**：「pur approve 成功 + WARNING 模式 + 归集行落库」已由 `testApproveTriggersMaterialAggregation` 隐式覆盖（默认 WARNING 模式，差异仅在 BudgetChecker 内部 `projected > total` 判定）；「Facade 超预算 WARNING 放行 + 落库」由 prj `testBudgetWarningAllowsOverBudget` 直接覆盖。两分支组合不触发任何新生产代码路径。
3. **Phase 2 候选探针逐项归属**：requireReferenceable 非 OPEN 前置拒绝——已有直接覆盖（prj `testRequireReferenceableRejectsNonOpen:205-225`）；事务回滚后入库单状态一致性——已有直接覆盖（pur `testStrictBudgetRejectsApproval:157-159` SUBMITTED 保持）；STRICT 拒绝后预算放宽重试恢复——恢复路径语义 = 「approve 成功路径」（已覆盖）+ 预算值放宽（非行为分支），无独立生产分支。

**与 A4.2.3 先例的显式分歧记录**：A4.2.3（mfg 预留）当时「跨工单并发」场景**无任何预覆盖**（既有并发测试仅单余额行写 API 层），故无条件新增探针；本行 SP-4 场景已由 `testStrictBudgetRejectsApproval` 等**预覆盖**，故 Phase 2 按计划 Prereqs 门控标 N/A——此为计划草案审查（Draft Review iter-1 M4/iter-2）已裁决的显式分歧，非执行期松弛。

## 4. 裁决

- **维持接受**：SP-4 存疑点运行时行为与 L1/L2 契约一致——`cost-collection.md §3.3` 采购审核时机 + §3.2 STRICT/WARNING 双态 + `use-cases.md` L1 UC-PRJ-04「STRICT 模式下超支拦截」经运行时实证成立。
- **零新 finding / 零分级变更**：不重开 P1-RC-049/P1-RC-051 已闭合裁决（RC-R1.61/RC-R1.62 结束审计闭环）；P2-RC-049（预算余量三项式承诺项）维持 watch-only 登记不变（projects 域无生产承诺源，触发条件未达，本行不重开——计划 Deferred But Adjudicated §P2-RC-049）。
- **零生产代码变更**：本确认为纯验证工作（只读复核 + 测试重跑 + 文档落盘），无任何 `.java`/ORM/api.xml/config 变更。

## 5. 过程纪律自检

- [x] **checker 门控核查**：本审计零生产代码变更（docs-only），checker 仅扫描生产代码，漂移结构性不可能。仍实跑 `bash docs/audits/nop-compliance-checker.sh` 留证——**全 19 规则 actual == baseline（`compliance-baseline.md` §BASELINE machine-readable 块 :427-448 逐行一致，0 漂移）**：R1d=14 / R2a=34 / R2b=235 / R2c=1434 / R2d=35 / R3=5 / R6=2 / R10=9 / R12a=70 / R12b=66 / R12c=40 / 其余=0。checker 脚本为纯 reporter（退出码恒 0），真正门控在 CI workflow 解析 actual > baseline => sys.exit(1)；本报告以 actual == baseline 为零漂移依据（对齐 A4.2.3/A1.35 §8 先例）。注：计划 Current Baseline 所记 R2c=1433 为 R1.60 时点值，权威现值为 1434（R1.63 +1 已裁决登记，compliance-baseline.md :478-483）。
- [x] **独立性声明**：本报告为 A4.2.119 运行时确认（verification），执行者与 P1-RC-049 修复（RC-R1.61）实现分属不同计划/会话；复核对象为既有测试断言与实仓重跑输出，非自我审计修复正确性。
- [x] **与 arm-index 交叉去重**：本行不新建 finding；P1-RC-049（:220）/P1-RC-051（:223）追加 A4.2.119 运行时确认注记（既有注记层级追加，不重开分级）；P2-RC-049（:224）watch-only 登记不动。
- [x] **验证证据**：目标类复跑 8+4 全绿（§2.7）+ 分域全量 prj 172 + pur 324 全绿 + checker 19 规则零漂移（本节）。

## 6. 回填清单

| 目标 | 变更 |
|---|---|
| `docs/backlog/requirement-compliance-roadmap.md` :306 | A4.2.119 行 ready → **done ✅**（回队执行记录：R1.61 解锁 + 复核/重跑结论 + N/A 裁决） |
| `docs/audits/arm-index.md` :220（P1-RC-049） | 追加 **【RC MA4 运行时确认（A4.2.119，2026-08-18）】** 注记 |
| `docs/audits/arm-index.md` :223（P1-RC-051） | 追加 **【RC MA4 运行时确认（A4.2.119，2026-08-18）】** 注记 |
| `docs/logs/2026/08-18.md` | 本行聚合日志条目（执行日 08-18） |
| 本计划 | Phase 1/2/3 勾选 + Closure Gates + Plan Status completed |
