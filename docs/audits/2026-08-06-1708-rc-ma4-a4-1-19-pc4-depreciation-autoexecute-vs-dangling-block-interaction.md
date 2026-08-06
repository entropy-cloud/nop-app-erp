# RC MA4 A4.1.19 — PC-4 资产折旧 auto-execute 与悬挂阻断交互运行时行为验证

> Audit Status: closed
> 里程碑：MA4（运行时行为验证层）
> 工作项：A4.1.19（A1.6 §7 存疑点 2 — UC-FIN-06 PC-4「资产未折旧 → 拒绝」auto-execute rethrow 与悬挂兜底阻断的运行时交互评估）
> 验证 plan：`docs/plans/2026-08-06-1708-1-rc-ma4-a4-1-19-pc4-depreciation-autoexecute-vs-dangling-block-interaction.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4 Q1 真相源层级 / §去重协议 / §8 过程纪律自检 / §MA4↔A5.6 边界）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）
> 验证性质：**只读运行时行为评估**（读 `runDepreciation` G3 双分支 + `findUnresolvedDepreciationSchedules` 悬挂扫描 + `ClosePeriodProcessor` 门控交互 + 既有测试覆盖普查 + 执行时序推理；不改代码/ORM/api.xml/真相源；roadmap 预授权类目）
> 验证日期：2026-08-06
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）
> 验证 HEAD：`c1b775491`（与 A1.6 一致；live repo 实测锚点见 §2）

## 0. 验证结论（TL;DR）

| 维度 | 评估结果 | 处置 |
|---|---|---|
| **PC-4 双路径运行时有效阻断**（存疑点核心） | ✅ **成立** | auto-execute rethrow 在 doClosePeriod 事务内**立即**回滚（早于状态推进）；悬挂兜底扫描覆盖 preCheck 时点的**历史遗留** EXECUTED+posted=false；两路径在执行时序上**互斥**（preCheck 先于 runDepreciation 本次折旧），**无双报、无漏报** |
| **auto-post-on-close 配置轴交互** | ✅ 无漏报 | rethrow **独立**于 auto-post-on-close（doClosePeriod 事务内无条件回滚）；悬挂扫描门控 `!isAutoPostOnClose() && hasIssues()` 仅 auto-post-on-close=false 生效——auto-post-on-close=true 时 PC-4 阻断**仅**依赖 rethrow（悬挂扫描失效但 rethrow 仍阻断 → 无漏报） |
| **悬挂扫描过滤语义正确性** | ✅ 精确覆盖 | `posted=FALSE AND status=EXECUTED AND period=code` 精确覆盖 PC-4「资产未折旧」语义（EXECUTED=已计提 posted=false 悬挂；REVERSED/CANCELLED 合法不阻断）；try/catch 安全跳过 assets entity 未注册场景 |
| **既有测试覆盖边界** | ⚠ 缺口确认（已知，归 MR1） | Branch A impl 未就绪跳过 = `TestErpFinDepreciationIntegration#testDepreciationGateNonBlocking:46-57` 覆盖；**Branch B rethrow NopException 路径 = 零覆盖**；**findUnresolvedDepreciationSchedules 悬挂扫描 = 零覆盖**（A1.6 §3.7 已记，本验证确认缺口对 PC-4 运行时有效阻断的**实际风险面 = 仅验证覆盖不足**，非行为缺陷） |
| **PC-4 符合性裁决** | ✅ **维持接受[运行时确认行为达成]** | §2 接受判据：rethrow 优先阻断 + 悬挂扫描覆盖历史遗留 + auto-post-on-close=true 时 rethrow 独立阻断无漏报；与 A1.6 §5.2 PC-4 接受[行为达成] + §6.2 P1-MA4-004 resolved 分层一致 |

**整体裁决**：A1.6 §7 存疑点 2 闭合。UC-FIN-06 PC-4「资产未折旧 → 拒绝」的 auto-execute（`runDepreciation` G3 Branch B rethrow）+ 悬挂兜底（`findUnresolvedDepreciationSchedules`）双路径在配置错误场景下的运行时交互**有效阻断且无双报无漏报**，PC-4 维持 A1.6 §5.2 已裁决的「接受[行为达成]」。**无新 finding**（PC-4 符合性维持，与 P1-MA4-004 rethrow resolved 的交互面经运行时确认有效；测试覆盖缺口归 MR1 测试补充 follow-up，已在 plan §Deferred But Adjudicated 预声明，不降级 PC-4 符合性）。本验证**不实施修复**（plan Non-Goals + §5 保护区域）。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md`（L1 权威真相源，方法论 §4）。本验证只评 PC-4 一条验收标准。

### UC-FIN-06 期末结账前置门禁（`use-cases.md:110`）

**PC-4 验收标准逐字**（A1.6 §1 已完整枚举 5 条；本验证聚焦 PC-4）：

```
若 资产未折旧 → 拒绝
```

> L2 owner doc `docs/design/finance/period-close.md §期末结账步骤 :60-111`（设计参考）：步骤3「折旧计提」auto-execute（查询本期应折旧资产 → 按折旧方法计算折旧额 → 生成折旧凭证 → 更新资产卡片累计折旧）。L2 为设计参考非真相源（§4 Q1）。

---

## 2. 实现证据（L3，`file:line` 写时实测，HEAD `c1b775491`）

> 全部实现位于 `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/`。本验证 live repo 逐行核实以下断言，零漂移。

### 2.1 runDepreciation G3 双分支（`ErpFinAccountingPeriodProcessor.java`）

**config gate**（`:180-182`）：
```java
protected void runDepreciation(ErpFinAccountingPeriod period, IServiceContext context) {
    if (!isAutoDepreciationOnClose()) {
        return;                                          // config erp-ast.auto-depreciation-on-close=false → 直接跳过
    }
```
`isAutoDepreciationOnClose()`（`:638-641`）：`AppConfig.var(CONFIG_AUTO_DEPRECIATION_ON_CLOSE, Boolean.TRUE)` + `!Boolean.FALSE.equals(flag)` → **默认 true**（config 不设或设非 false 均开启）。

**Branch A — impl 未就绪容错跳过**（`:184-191`）：
```java
IErpAstDepreciationScheduleBiz depreciationBiz;
try {
    depreciationBiz = bizObjectManager
            .getBizObject(ErpAstDepreciationSchedule.class.getSimpleName()).asProxy();
} catch (Exception e) {                                  // 过宽 catch（含 NopException）但语义=impl 未就绪
    LOG.warn("期末结账：期间 {} 折旧集成跳过（impl 未就绪：{}）", period.getCode(), e.getMessage());
    return;                                              // 不阻断结账
}
```
触发条件：assets 域未部署 / `IErpAstDepreciationScheduleBiz` impl 未注册 / `bizObjectManager.getBizObject` 抛任何异常（含解析失败）。

**Branch B — 配置错误/真实故障 rethrow**（`:192-199`）：
```java
try {
    int processed = depreciationBiz.executeBatchDepreciation(period.getCode(), context);
    LOG.info("期末结账：期间 {} 批量折旧完成，成功计提 {} 项资产", period.getCode(), processed);
} catch (NopException e) {                                // 仅 catch NopException（已收窄于 Branch A 的 Exception）
    LOG.error("期末结账：期间 {} 折旧失败（配置错误/真实故障，阻断结账）：{}", period.getCode(), e.getMessage());
    throw e;                                              // rethrow → @BizMutation 事务回滚 → 阻断结账
}
```
触发条件：assets 域部署 + `executeBatchDepreciation` 抛 NopException（如 `ERR_DEPRECIATION_RATE_MISSING` 折旧率缺失 / 真实基础设施故障经 NopException 包装）。

**入口**：`closeAssetModule:151-155`（`runDepreciation` **先于** `advanceModule(AST)`，即折旧失败时模块状态不推进）。

| 分支 | 文件:行 | 触发条件 | 运行时行为 | 阻断结账 |
|---|---|---|---|---|
| **Branch A**（impl 未就绪） | `ErpFinAccountingPeriodProcessor.java:184-191` | assets 未部署 / impl 未注册 / bizObjectManager 解析失败 | catch Exception → LOG.warn → return | ❌ 不阻断（容错） |
| **Branch B**（配置错误/真实故障） | `ErpFinAccountingPeriodProcessor.java:192-199`（rethrow at `:198`） | assets 部署 + executeBatchDepreciation 抛 NopException | catch NopException → LOG.error → **throw e** | ✅ 阻断（事务回滚） |
| config gate | `:180-182` + `:638-641` | `erp-ast.auto-depreciation-on-close=false` | 直接 return | ❌ 不阻断（显式关闭） |

### 2.2 closePeriod 执行时序与 rethrow/悬挂交互（`ErpFinAccountingPeriodClosePeriodProcessor.java`，**本存疑点核心**）

`doClosePeriod:46-87` 时序（live 实测）：

| 步骤 | 行 | 动作 | 与 PC-4 双路径的关系 |
|---|---|---|---|
| 1 | `:48` | `assertPeriodStatus(period, OPEN, "结账")` | 状态守卫 |
| 2 | `:50` | `preCheckProcessor.preCheck(periodId, context)` | **执行悬挂扫描**（含 `findUnresolvedDepreciationSchedules`，扫**历史遗留** EXECUTED+posted=false） |
| 3 | `:52-56` | `if (report.hasAllowanceShortfall()) throw ERR_PRE_CHECK_BLOCKED` | 坏账缺口独立硬阻断（与 PC-4 无关） |
| 4 | `:59-63` | `if (!isAutoPostOnClose() && report.hasIssues()) throw ERR_PRE_CHECK_BLOCKED` | **悬挂门控**：仅 auto-post-on-close=false 时悬挂扫描结果阻断 |
| 5 | `:67` | `advanceModule(status, AR)` | AR 模块关账 |
| 6 | `:68` | `advanceModule(status, AP)` | AP 模块关账 |
| 7 | `:69` | `closeInvModule(period, status, context)` | INV 模块关账（含 `recloseInvCosts`，G3 同型） |
| 8 | `:70` | **`closeAssetModule(period, status, context)`** | **AST 模块关账 → `runDepreciation`（本次折旧，若 Branch B rethrow 则事务回滚）** |
| 9 | `:71` | `closeGlModule(period, status, context)` | GL 模块关账 |
| 10 | `:81-84` | setStatus(CLOSING→CLOSED) + closedAt/closedBy | 状态推进（仅在 `:67-71` 全部成功后） |
| 11 | `:85` | `flushSession()` | 落库 |

**关键时序断言**（PC-4 双路径交互核心）：

1. **悬挂扫描（步骤2）先于 runDepreciation（步骤8）**：`preCheck:50` 在 `closeAssetModule:70` **之前**执行。悬挂扫描读到的是**上一周期遗留**的 EXECUTED+posted=false schedule（本次 runDepreciation 尚未执行，无本次产生的悬挂可扫）。
2. **runDepreciation Branch B rethrow 在事务内立即阻断**：`closeAssetModule:70` → `runDepreciation:198` throw e → 异常传播出 `doClosePeriod` → `@BizMutation` 事务回滚 → 步骤10-11 状态推进**不执行** → 期间保持 OPEN。**preCheck 悬挂扫描结果（步骤2-4）不落地**（事务回滚）。
3. **无双报**：配置错误场景下，Branch B rethrow 在步骤8 阻断，步骤2-4 的悬挂门控已在前序完成（要么已 throw at :59-63，要么放行）。两者**不在同一事务内同时阻断同一结账**——悬挂门控若已在 :59-63 阻断则 runDepreciation 根本不执行；若悬挂门控放行（auto-post-on-close=true 或无历史遗留）则 runDepreciation 是唯一阻断源。无双重 ERR_PRE_CHECK_BLOCKED + NopException 同事务叠加。
4. **无漏报**：auto-post-on-close=true 时悬挂门控失效（`:59-63` 条件不满足），但 runDepreciation Branch B rethrow **独立**于 auto-post-on-close（rethrow 在 doClosePeriod 事务内无条件回滚）→ 配置错误场景仍阻断，无漏报窗口。

### 2.3 悬挂兜底扫描过滤逻辑（`ErpFinAccountingPeriodProcessor.findUnresolvedDepreciationSchedules:506-527`）

```java
protected List<String> findUnresolvedDepreciationSchedules(ErpFinAccountingPeriod period) {
    List<String> keys = new java.util.ArrayList<>();
    try {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("posted", Boolean.FALSE));                    // :511 — posted=false
        q.addFilter(eq("status", "EXECUTED"));                       // :514 — 仅 EXECUTED（排除 REVERSED/CANCELLED）
        if (period.getCode() != null) {
            q.addFilter(eq("period", period.getCode()));             // :516 — 限定本期
        }
        for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
            keys.add("depreciation:" + (s.getAssetId() == null ? s.getId() : s.getAssetId())
                    + "#" + period.getCode());                       // :519-520 — key 形如 depreciation:<assetId>#<periodCode>
        }
    } catch (Exception e) {                                          // :522
        LOG.debug("期末前置检查：assets 折旧 posted=false 扫描跳过（{}）", e.getMessage());  // :524
    }                                                                // :525 — assets entity 未注册时安全跳过
    return keys;
}
```

**过滤语义核验**（PC-4「资产未折旧」精确覆盖）：

| 过滤条件 | 行 | 语义 | 与 PC-4 的对应 |
|---|---|---|---|
| `posted=FALSE` | `:511` | 未过账 | PC-4「未折旧」= 已计提但未生成 GL 凭证（posted=false） |
| `status=EXECUTED` | `:514` | 已计提状态 | 排除 REVERSED（已冲销，posted=false 合法）/ CANCELLED（已取消）—— 避免反结账→重结账循环误判已冲销 schedule |
| `period=<code>` | `:516` | 限定本期 | 仅扫本期悬挂，不误报跨期 |
| try/catch Exception → LOG.debug → 跳过 | `:522-525` | assets entity 未注册时安全跳过 | 单域 finance 测试无 ast-dao impl 时不阻断 |

**经 `findUnresolvedPostingExceptionKeys:471-477`**（三层扫描：finance exceptions + depreciation schedules + landed costs）→ `PeriodPreCheckReport.unresolvedPostingExceptionKeys` → `hasIssues()`（`!unpostedVoucherCodes.isEmpty() || !unresolvedPostingExceptionKeys.isEmpty()`）→ `ClosePeriodProcessor:59-63` hard block（auto-post-on-close=false 时）。

### 2.4 auto-post-on-close 配置对 PC-4 双路径阻断的交互（`ClosePeriodProcessor:59-63`）

```java
// :59-63
if (!facade.isAutoPostOnClose() && report.hasIssues()) {
    throw new NopException(ErpFinErrors.ERR_PRE_CHECK_BLOCKED)
            .param(ErpFinErrors.ARG_PERIOD_CODE, period.getCode())
            .param(ErpFinErrors.ARG_ISSUE_COUNT, report.issueCount());
}
```

`isAutoPostOnClose()`（`ErpFinAccountingPeriodProcessor:633-636`）：`AppConfig.var(CONFIG_AUTO_POST_ON_CLOSE, Boolean.FALSE)` + `Boolean.TRUE.equals(flag)` → **默认 false**（与 A1.6 §2.2 一致）。

**双路径配置轴交互矩阵**：

| auto-post-on-close | 悬挂扫描门控 (`:59-63`) | runDepreciation rethrow (`:70 → :198`) | 配置错误场景 PC-4 阻断源 | 漏报风险 |
|---|---|---|---|---|
| **false**（默认） | ✅ 生效（hasIssues() → throw） | ✅ 生效（rethrow → 事务回滚） | 悬挂扫描（历史遗留）+ rethrow（本次失败）双保险 | 无 |
| **true** | ❌ 失效（条件不满足，放行） | ✅ 生效（rethrow 独立于 auto-post-on-close） | **仅** rethrow | **无**（rethrow 仍阻断） |

**关键断言**：rethrow 在 doClosePeriod 事务内，**不读 auto-post-on-close config**——无论 config 如何，executeBatchDepreciation 抛 NopException 即 throw e → 事务回滚。故 auto-post-on-close=true 时虽悬挂扫描门控失效，PC-4 阻断仍由 rethrow 兜底，**无漏报窗口**。

---

## 3. 测试证据（L4，注明断言强度 + 缺口）

> 测试位于 `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/`。MA5 评级引用。

### 3.1 Branch A impl 未就绪跳过覆盖（`TestErpFinDepreciationIntegration.java`，1 @Test）

| 测试方法 | 文件:行 | 覆盖路径 | 断言强度 |
|---|---|---|---|
| `testDepreciationGateNonBlocking` | `:46-57` | 默认 `auto-depreciation-on-close=true` + finance 单域无 ast-service → bizObjectManager 解析失败 → Branch A catch Exception → LOG.warn → return；closePeriod 不阻断，期间 CLOSED + AST 模块 CLOSED | **中**（状态断言 CLOSED + assetStatus CLOSED；未断言 LOG.warn 触发） |

### 3.2 测试覆盖缺口确认（A1.6 §3.7 已记，本验证实测复核）

> 实测 grep（`rg -n "runDepreciation|recloseInvCosts|executeBatchDepreciation" module-finance/erp-fin-service/src/test/` = **0 命中**；`rg -n "findUnresolvedDepreciationSchedules|depreciation:.*#|unresolvedPostingExceptionKeys" .../test/` = **0 命中**；`rg -n "ERR_DEPRECIATION_RATE_MISSING|mock.*DepreciationSchedule" .../test/` = **0 命中**）：

| 路径 | 覆盖状态 | 缺口性质 |
|---|---|---|
| **Branch B rethrow NopException**（`:192-199`） | ❌ **零覆盖** | 无 mock 注入抛 NopException 的 `IErpAstDepreciationScheduleBiz`，无法验证 rethrow → 事务回滚 → 期间保持 OPEN |
| **`findUnresolvedDepreciationSchedules` 悬挂扫描**（`:506-527`） | ❌ **零覆盖** | 无构造 EXECUTED+posted=false schedule 的测试，无法验证悬挂 key 检出 + hard block |
| **跨域悬挂阻断端到端**（assets 折旧 posted=false → preCheck → ERR_PRE_CHECK_BLOCKED） | ❌ **零覆盖** | 单域 finance 测试无 ast-dao impl，扫描 try/catch 跳过 |
| Branch A impl 未就绪跳过（`:184-191`） | ✅ 覆盖（`:46-57`） | 中断言强度 |

**缺口风险评估**：测试覆盖缺口 = **验证覆盖不足**（无法自动捕获未来回归），**非当前行为缺陷**（本验证 §2 已静态+时序推理确认双路径运行时有效阻断）。归 MR1 测试补充（plan §Deferred But Adjudicated 预声明，方向见 §6）。

---

## 4. 运行时行为证据（L5，执行时序推理 + 既有证据复用）

### 4.1 复用 A1.6 §2.4 + §5.2 PC-4 间接实现静态确认

A1.6 已静态确认 PC-4 经**间接实现**（auto-execute 步骤 `runDepreciation` + 悬挂兜底阻断 `findUnresolvedDepreciationSchedules`）达成，裁决接受[行为达成]。本验证补「双路径运行时交互[执行时序 + 配置错误场景 + 双报可能性]」差异。

### 4.2 复用 A4.1b P1-MA4-004 rethrow resolved（R1.16）

A4.1b 已确认 P1-MA4-004（跨域异常吞噬）resolved R1.16：`runDepreciation:195-200` catch NopException → LOG.error → **rethrow**（G3 配置错误/真实故障阻断结账，impl 未就绪 try/catch 容错跳过）。本验证 §2.1 live 复核该修复在 HEAD `c1b775491` 仍落地（行号微移至 `:192-199`，逻辑一致），**不重新核实 P1-MA4-004 修复本身**（A1.6 §6.2 + A4.1b 已证），只评 rethrow 与悬挂扫描的运行时交互。

### 4.3 复用 A2.3 period-close E2E（行为 PASS）

A2.3 period-close E2E（`docs/audits/2026-07-27-1949-arm-ma2-period-close-e2e.md`）已证 period-close 主链路行为 PASS（preCheck → closePeriod → FX/PL 凭证 → finalizePeriod → reverseClose → re-close）。不含跨域悬挂阻断 / 折旧 rethrow 单测覆盖（A1.6 §3.7 已记缺口）。

### 4.4 本切片运行时交互推理结论（不重审行为，只补交互时序差异）

基于 §2 live 代码 + 执行时序分析，PC-4 双路径在配置错误场景（assets 部署 + `ERR_DEPRECIATION_RATE_MISSING`）的运行时交互：

```
closePeriod(@BizMutation 开启事务)
  └─ doClosePeriod
       ├─ :50 preCheck → findUnresolvedDepreciationSchedules 扫【历史遗留】EXECUTED+posted=false
       │    （本次 runDepreciation 尚未执行，无本次悬挂可扫）
       ├─ :59-63 悬挂门控（auto-post-on-close=false 时）
       │    ├─ 有历史遗留 → throw ERR_PRE_CHECK_BLOCKED → 事务回滚 → 期间保持 OPEN ✅ 阻断
       │    └─ 无历史遗留 → 放行
       ├─ :70 closeAssetModule → runDepreciation
       │    ├─ Branch A（impl 未就绪）→ warn skip → 不阻断（非 PC-4 场景）
       │    └─ Branch B（配置错误）→ executeBatchDepreciation throw NopException
       │         → catch NopException → LOG.error → throw e
       │         → 异常传播出 doClosePeriod → @BizMutation 事务回滚
       │         → :81-84 状态推进不执行 → 期间保持 OPEN ✅ 阻断
       │         → preCheck 悬挂扫描结果不落地（事务回滚）
       └─ （仅全部成功才达）:81-84 状态推进 + :85 flush
```

**交互结论**：
- **① 配置错误场景 rethrow 在事务内立即阻断（事务回滚，悬挂扫描结果不落地）** ✅ 证实
- **② 悬挂扫描只覆盖历史遗留 EXECUTED+posted=false（preCheck 在 runDepreciation 前），不覆盖本次 runDepreciation 产生的悬挂（本次失败经 rethrow 回滚，无悬挂落地）** ✅ 证实
- **③ 无双报（rethrow 优先回滚 vs 悬挂扫描 hasIssues 门控 auto-post-on-close=false）也无漏报（auto-post-on-close=true 时 rethrow 仍阻断，悬挂扫描失效但不致漏——因 rethrow 独立于 auto-post-on-close）** ✅ 证实

---

## 5. 符合性结论（PC-4 运行时确认，方法论 §2 判据 + 三源对照）

### 5.1 PC-4 运行时确认裁决

| 验收标准 | L1 契约 | L2 设计参考 | L3 代码路径（写时实测） | L4 测试 | L5 运行时交互 | 裁决 | 命中判据 |
|---|---|---|---|---|---|---|---|
| **PC-4**（资产未折旧 → 拒绝） | `use-cases.md:110`「若 资产未折旧 → 拒绝」 | `period-close.md §期末结账步骤 :60-111`（步骤3 折旧计提 auto-execute，config-gated） | `closeAssetModule:151-155` → `runDepreciation:179-200`（Branch A impl 未就绪 skip :184-191 / Branch B 配置错误 rethrow :192-199）+ `findUnresolvedDepreciationSchedules:506-527`（悬挂兜底，EXECUTED+posted=false :510-517）+ `ClosePeriodProcessor:50/59-63/70`（时序交互） | Branch A 覆盖（`TestErpFinDepreciationIntegration:46-57`）；Branch B rethrow + 悬挂扫描零覆盖（§3.2 缺口） | §4.4 时序推理：rethrow 优先阻断 + 悬挂扫描覆盖历史遗留 + auto-post-on-close=true 时 rethrow 独立阻断无漏报 | **维持接受[运行时确认行为达成]** | §2 接受（双路径运行时有效阻断 + 无双报无漏报；测试缺口归 MR1 不降级符合性） |

### 5.2 与 A1.6 §5.2 + §6.2 P1-MA4-004 分层一致性

| 既有裁决 | 本验证结论 | 一致性 |
|---|---|---|
| **A1.6 §5.2 PC-4 接受[行为达成]**（间接实现：auto-execute + 悬挂阻断） | PC-4 维持接受[运行时确认行为达成]——双路径运行时交互有效阻断 | ✅ 一致（本验证补运行时交互确认，不升级不降级） |
| **A1.6 §6.2 P1-MA4-004 resolved R1.16**（跨域异常吞噬修复） | rethrow 路径 live 复核仍落地（`:192-199`）；rethrow 与悬挂扫描运行时交互有效（rethrow 优先回滚，悬挂扫描兜底历史遗留） | ✅ 一致（本验证不重新核实 P1-MA4-004 修复本身，只确认交互面） |
| **A2.3 period-close E2E PASS** | 主链路行为复用；本切片补双路径交互差异 | ✅ 一致（去重协议） |

### 5.3 候选缺口评估（本验证产出）

| 候选缺口 | 评估 | 处置 |
|---|---|---|
| **Branch B rethrow NopException 路径零测试覆盖** | 行为正确（§2.1 + §4.4 已静态+时序推理证实），仅验证覆盖不足 | 归 **MR1 测试补充 follow-up**（plan §Deferred But Adjudicated 预声明；与 P1-MA4-005 MR2 测试有效性互补不重叠）—— 不降级 PC-4 符合性 |
| **findUnresolvedDepreciationSchedules 悬挂扫描零测试覆盖** | 同上 | 同上 |
| **双报/漏报窗口** | §4.4 时序分析证实无双报无漏报 | **无 finding**（行为正确） |
| **PC-4 符合性本身** | 维持 A1.6 §5.2 接受 | **无新 finding** |

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

> 方法论 §7：产出 finding 前 grep arm-index 同域同控制点后裁决。本验证**无新 finding**。

| 候选 | grep 比对 | 裁决 | 依据 |
|---|---|---|---|
| Branch B rethrow 路径测试缺口 | 比对 arm-index period-close 分区（P1-MA4-004 rethrow resolved / P1-MA4-005 测试有效性 resolved MR2.10） | **复用 P1-MA4-005 MR2 follow-up**（不新建） | 同控制点：测试有效性。P1-MA4-005 已登记「期间结账 E2E 异常路径（折旧失败→阻断/工作台/告警依赖 P1-MA4-004）」MR2 follow-up（arm-index `:823`），本验证确认该 follow-up 仍未落地（§3.2 实测零覆盖），归 MR1/MR2 测试补充，不新建 |
| 悬挂扫描测试缺口 | 比对 arm-index（P1-MA4-005 测试有效性 + A1.6 §3.7 已记「跨域悬挂阻断无单测」） | **复用 P1-MA4-005 MR2 follow-up + A1.6 §3.7 已记**（不新建） | 同控制点：测试有效性。A1.6 §3.7 已显式登记「跨域悬挂阻断（assets 折旧 / inventory 到岸成本 posted=false 悬挂）：findUnresolvedDepreciationSchedules / findUnresolvedLandedCosts 无单测」，本验证确认仍成立 |
| 双报/漏报风险 | 比对 arm-index（无「PC-4 双路径交互双报/漏报」同控制点 finding） | **无新 finding**（行为正确，§4.4 已证） | §4.4 时序分析证实无双报无漏报，不达 §2 P0/P1/P2 任一判据 |
| PC-4 符合性 | 比对 A1.6 §5.2 PC-4 接受[行为达成] | **维持接受**（arm-index `:375` A1.6 注记不变） | 本验证补运行时确认，不升级不降级 |

**arm-index 更新**：**无变更**（PC-4 符合性维持 A1.6 §5.2 接受[行为达成]；测试缺口归 P1-MA4-005 MR2 follow-up + A1.6 §3.7 已记，不新建 finding；P1-MA4-004 rethrow resolved 状态不变）。grep 依据：`rg "P1-MA4-004|PC-4|资产未折旧|findUnresolvedDepreciation" docs/audits/arm-index.md` 命中行（`:375` A1.6 注记 + `:822` P1-MA4-004 resolved + `:823` P1-MA4-005 MR2）均与本验证结论一致，无需修订。

---

## 7. 静态存疑点清单

> 本验证是 A1.6 §7-2 存疑点的 MA4 运行时展开。本验证**无新存疑点产出**（存疑点 2 已闭合，结论 = 维持接受）。

A1.6 §7 余存疑点（非本验证范围）：
- §7-3 RC-9 反结账审计缺失实际合规影响 → A4.1.20（todo）
- §7-4 年末反结账阻断边界 → A4.1.21（todo）

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。**本验证无生产代码变更**（只读评估），checker 无回归风险。actual vs baseline 实测记录（checker 输出至 R3 后终止，exit 1——checker 是纯 reporter，方法论 §8 不以脚本退出码作为门控通过依据，真正门控在 CI workflow `compliance.yml`）：

  | 规则 | baseline（compliance-baseline.md） | actual（本次实测） | 状态 |
  |---|---|---|---|
  | R1a dao().saveEntity (BizModel) | 0 | 0 | ✓ |
  | R1b dao().updateEntity (BizModel) | 0 | 0 | ✓ |
  | R1c dao().getEntityById (BizModel) | 0 | 0 | ✓ |
  | R1d dao().findAllByQuery (BizModel) | 14 | 14 | ✓ |
  | R2a BizModel daoFor(ErpMd*) | 34 | 34 | ✓ |
  | R2b BizModel daoFor(Erp*) 跨域 | 240 | 229 | ✓（改善） |
  | R2c 全生产代码 daoFor() 总量 | 1380 | 1382 | +2（**预存漂移，非本验证引入**——本验证零生产代码变更） |
  | R2d Processor daoFor(ErpMd*) | 32 | 34 | +2（**预存漂移，非本验证引入**） |
  | R3-R12 | （checker 输出在 R3 处终止，未捕获完整数据） | — | 本验证零生产代码变更，无回归风险 |

  **门控结论**：本验证是只读评估，零生产代码变更，checker actual vs baseline 的任何差异均为**预存状态**（非本验证引入）。R2c/R2d 的 +2 漂移属其他工作项的既有状态，不属本验证范围，不在本报告处置（若需追溯，归独立 compliance-baseline 同步计划）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（见 plan §Closure Gates）。
- [x] **与 arm-index 交叉去重声明**：本报告全部候选缺口已按方法论 §7 规则 grep arm-index 同域同控制点后给出「复用 or 新增」裁决（§6），**无未经比对直接新建的 finding**。本验证产出 = 「维持接受无新 finding」，测试缺口归 P1-MA4-005 MR2 follow-up + A1.6 §3.7 已记。
- [x] **MA4↔A5.6 边界声明**：本验证审「行为是否符合需求」（PC-4 双路径交互是否有效阻断），与 A5.6 审「E2E 断言强度」边界按此执行（方法论 §去重协议 MA4↔A5.6 边界）。本验证**不重做** A5.6 E2E 断言强度审计。
- [x] **真相源冻结声明**：本验证未修改任何真相源（`docs/requirements/product-scope.md` / `docs/design/finance/use-cases.md` / `docs/design/finance/period-close.md` 需求契约段落均只读引用，§9 冻结条款遵守）。
- [x] **保护区域纪律**：本验证是只读评估，不触及 ORM/会计过账逻辑**修改**（plan §保护区域 + Non-Goals）。测试缺口归 MR1 测试补充（roadmap 预授权类目），BizModel 修复预授权类目（若未来需要）。

---

## 9. 与既有审计报告差异增量声明

> 方法论 §6 段落 9 + §去重协议：本验证复用既有审计已证实行为，只补需求视角差异。

本验证相对 A1.6 / A2.3 / A4.1b 的**需求视角增量**：

1. **PC-4 双路径运行时交互时序确认**（A1.6 §2.4 + §5.2 只静态确认间接实现，未做交互时序分析；本验证 §2.2 + §4.4 补「preCheck 先于 runDepreciation + rethrow 事务回滚 + 悬挂扫描覆盖历史遗留」时序推理）—— 闭合 A1.6 §7-2 存疑点
2. **auto-post-on-close 配置轴对 PC-4 双路径阻断的交互评估**（A1.6 未单独评估此配置轴对 PC-4 的影响；本验证 §2.4 补「rethrow 独立于 auto-post-on-close → 无漏报」结论）
3. **Branch B rethrow 路径 + 悬挂扫描测试缺口实测复核**（A1.6 §3.7 已记缺口；本验证 §3.2 实测确认仍成立，并评估对 PC-4 运行时有效阻断的实际风险面 = 仅验证覆盖不足，非行为缺陷）

**不复审项**（去重协议）：
- PC-4 验收标准本身逐条裁决 → A1.6 §5.2 已判接受（本验证只确认运行时交互，不重做裁决）
- P1-MA4-004 rethrow 修复本身 → A1.6 §6.2 + A4.1b 已证 resolved R1.16（本验证只确认 rethrow 与悬挂扫描的交互面，§2.1 live 复核仍落地）
- period-close 主链路 E2E 行为 → A2.3 已证 PASS（本验证复用，不重跑）
- A5.6 E2E 断言强度 → 非本验证范围（MA4↔A5.6 边界）
