# RC MA4 A4.2.13 — UC-HR-07 合同到期 30/60/90 多档预警运行时配置确认（A1.12 §7-2） 验证报告

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 运行时行为验证）
> 工作项：A4.2.13（MA4 运行时行为验证 — A1.12 §7-2：UC-HR-07 合同到期 30/60/90 多档预警运行时配置，实现采用单一可配置阈值[默认 30 天] + warningDays 参数可覆盖，运行时是否有多档调度配置[如三个 Job 实例分别传 30/60/90]）
> 验证 plan：`docs/plans/2026-08-06-2247-3-rc-ma4-a4-2-13-hr-contract-expiry-multi-tier-alert.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4 Q1 真相源层级与冲突裁决 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §去重协议）
> 输入存疑点：A1.12 §7-2（`docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md:323`）
> 关联既有裁决：A1.12 §5 UC-HR-07 = **接受**（⑳30/60/90 提醒窗口——单一可配置阈值默认 30 天[L1 多档概念可表达]，单一阈值是配置简化非契约缺失；⑮不续签→RESIGNED 复用 P1-MA2-039 successor Deferred resolved R1.15）
> 关联同型范式：A4.2.12（`docs/audits/2026-08-06-2247-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`，done — 同源 UC-HR-07，**不同控制点**：A4.2.12 = cron 调度接线/默认活跃性，A4.2.13 = 多档阈值/多档调度配置；两计划互补不重复[A4.2.12 已闭合 cron 接线维度，本验证闭合多档预警配置维度]）
> 关联 finding：无既有 P1/P2 finding 覆盖多档预警维度（A1.12 §5 UC-HR-07 = 接受，无独立 finding 行；P1-MA2-086 = 并发幂等，不同控制点）
> 验证性质：**只读多档预警配置评估**（grep scanExpiringContracts + warningDays 参数覆盖机制 + ErpHrContractExpiryJob 调用是否传多档 + 全 application.yaml/部署多档调度配置普查；不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目「只读评估」+「文档更新类修复」）
> 验证日期：2026-08-06
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **A1.12 §7-2 存疑点裁决** | **维持 UC-HR-07 接受**（单一阈值 config 驱动 + warningDays 参数覆盖机制完整；自动化 Job 单一默认窗口[30 天单次]，无多档调度配置；L1 ⑳"30/60/90 天"多档概念可经 config/手工 GraphQL 调用表达，单一阈值是配置简化非契约缺失——与 A1.12 §5 分层一致） | 不升级 P0/P1；多档调度登记 successor watch-only |
| scanExpiringContracts 单一阈值 + 参数覆盖机制 census | **机制完整**（`scanExpiringContracts:65` `@Optional warningDays` + `:67` 参数非空覆盖 config 默认/为空用 config 默认[经 `contractExpiryWarningDays():171-175` 读 `erp-hr.contract-expiry-warning-days` 默认 30]） | 参数覆盖可达，多档可经手工调用表达 |
| 自动化 Job 多档调用核验 | **单一默认窗口**（`ErpHrContractExpiryJob.runExpiryWarnings:72` `scanExpiringContracts(null, ctx)`——传 null → 用 config 默认 30 天**单次**；Job 体内仅 1 处 scan 调用，无 30/60/90 三次调用） | 自动化 Job **不**实现多档预警 |
| 多档调度配置普查 | **无多档调度配置**：nop-job `.job.yaml` 仅 1 个 `erp-hr-contract-expiry` 作业（无 30/60/90 三个实例）+ 全 20 生产 application.yaml **零** `erp-hr.contract-expiry-warning-days` override | 生产环境 = 单一默认 30 天单窗口 |
| L1 ⑳ 多档语义满足度 | **主路径满足**（提醒窗口内扫描+通知完整）+ **60/90 天提前预警未自动化派发**（仅可经 GraphQL 手工 `scanExpiringContracts(60/90)` 表达，非自动化） | §2 P2① 次要验收标准（多档自动派发增强 successor） |
| 新 finding | **1 项 P2-RC-087 watch-only successor**（多档预警调度增强 successor，§2 P2①——主路径[单一阈值 config 驱动 + 30 天自动窗口]OK，边界[60/90 天提前自动派发]未实现，登记不强制） | successor watch-only，归部署/配置 successor 非 MR1 |
| MR0 触发 | **无** | — |

**整体裁决**：A1.12 §7-2 静态存疑点（「L1 '30/60/90 天' 多档预警概念，实现采用单一可配置阈值。运行时是否有多档调度配置（如三个 Job 实例分别传 warningDays=30/60/90）需 MA4 确认」）经 scanExpiringContracts 单一阈值 + 参数覆盖机制 census + ErpHrContractExpiryJob 多档调用核验 + 全 application.yaml/部署多档调度配置普查 **CONFIRMED**：实现采用单一可配置阈值（config 默认 30）+ warningDays 参数可覆盖（参数非空覆盖 config 默认，为空用 config 默认）+ 自动化 Job 单一默认窗口（`scanExpiringContracts(null)` → 30 天单次）+ **无多档调度配置**（无 30/60/90 三个 Job 实例 + 零 application.yaml warning-days override）。

按 §2 分级判据三源复核：①**§2 接受判据主路径满足**——L1 UC-HR-07「扫描 endDate 在提醒窗口内的 ACTIVE 合同 + 通知 HR」主路径经单一阈值 + 单次调度完整实现（`scanExpiringContracts` ACTIVE + dateBetween + `IErpSysNotificationBiz.notify` 跨域派发），L1 ⑳「30/60/90 天」是提醒窗口**概念**（合同落在 30/60/90 天窗口内即提醒），L1 **未强制要求**「30/60/90 各派发一次三次独立提醒」；②**§2 P1①「功能完全缺失」不成立**——主路径（提醒窗口扫描 + 通知 + 续签/到期终止）完整，多档经 config 调整 warning-days 或 GraphQL 手工调用 `scanExpiringContracts(60/90)` 可表达；③**§2 P2①「次要验收标准未完全满足」成立**——60/90 天提前预警**未自动化派发**（自动化 Job 恒传 null → 单一默认 30 天窗口，60/90 天档需多档调度配置[3 Job 实例或多次调度]，当前不存在），属次要验收标准边界场景弱 → 登记 **P2-RC-087 watch-only successor**（多档预警调度增强 successor）；④**§2 P0④ 会计过账正确性破坏不适用**——合同到期扫描不涉 GL 过账。故与 A1.12 §5 UC-HR-07 接受裁决**分层一致**（均接受，单一阈值是配置简化非契约缺失），多档自动派发增强登记 successor watch-only。

**不触发 MR0，登记 1 项 P2-RC-087 watch-only successor（多档预警调度增强，归部署/配置 successor 非 MR1 代码修复）。** 本验证**不实施多档调度**（属增强 successor，plan Non-Goals），仅更新部署运维文档（`docs/architecture/job-scheduling.md §3.15:229` 补多档调度注记 + warning-days config 说明，预授权文档更新）+ 登记 arm-index P2-RC-087 watch-only 行。

> **与 A4.2.12 控制点差异声明**：A4.2.12（cron 接线）评「cron 调度机制是否存在 + 运行时是否默认活跃」（结论：接线完整 + config-gate 默认关闭 = 部署启用决策，0 finding）；本验证 A4.2.13（多档预警）评「多档[30/60/90]调度配置是否存在 + 自动化 Job 是否多档调用」（结论：无多档调度配置 + 自动化 Job 单一默认窗口 → 60/90 天自动派发缺失 → P2-RC-087 successor watch-only）。两控制点互补不重复：A4.2.12 答「cron 通不通」，A4.2.13 答「通了之后是否多档」。即便 A4.2.12 的 cron 启用（两层 config 设 true + 非空），本验证的多档维度评估仍独立成立（启用后仍只跑单一默认 30 天窗口，不自动多档）。

---

## 1. 输入存疑点原文 + L1/L2/L3 锚点

### 1.1 输入存疑点原文（A1.12 §7-2，逐字引用）

> **UC-HR-07 30/60/90 多档预警运行时配置**：L1 "30/60/90 天" 多档预警概念，实现采用单一可配置阈值。运行时是否有多档调度配置（如三个 Job 实例分别传 warningDays=30/60/90）需 MA4 确认（静态确认单一阈值 config 驱动 + 参数可覆盖）。
> — `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md:323`

### 1.2 A1.12 §5 UC-HR-07 既有接受裁决（输入，本验证复核分层一致性）

A1.12 §5（`:259`）对 UC-HR-07 断言⑲-㉔逐条核验给出**接受**裁决，其中⑳「30/60/90 提醒窗口」裁决原文：
> `scanExpiringContracts:64-74` 经 `ErpHrConfigs.contractExpiryWarningDays()` 默认 30 天 + `warningDays` 参数可覆盖。L1 "30/60/90 天" 是提醒窗口**概念**（多档预警），实现采用单一可配置阈值（默认 30）。HR 可调 config 或传参实现多档扫描（如分别调 30/60/90）——**实现可表达 L1 语义**，单一阈值是配置简化非契约缺失 ✓

**本验证对象** = §7-2 残留存疑点「运行时是否有多档调度配置（3 Job 实例分别传 30/60/90）」的部署/运行时普查（A1.12 静态确认代码单一阈值 config 驱动 + 参数覆盖逻辑正确，但未普查部署侧是否有多档调度配置 + 自动化 Job 是否多档调用）。本验证**不重复核实**断言⑳「单一阈值 + 参数覆盖」代码逻辑（A1.12 §5 已证），只评多档预警**运行时调度配置现状**差异。

### 1.3 L1 需求契约（UC-HR-07，逐字）

`docs/design/human-resource/use-cases.md:75-85`：

```
## UC-HR-07 合同到期提醒
| **概述** | 劳动合同到期前自动提醒 HR，支持续签或终止 |
| **触发条件** | 定时任务每日扫描即将到期的合同 |
| **前置条件** | ErpHrEmploymentContract.status = ACTIVE，endDate 为未来 30/60/90 天内 |   ← 断言⑳（本验证核心）
| **基本流程** | 1. 系统扫描 endDate 在提醒窗口内的 ACTIVE 合同
                2. 通知 HR 管理员
                3. HR 操作续签（创建新合同，原合同 endDate 不变但 status→EXPIRED）
                4. 或到期终止（原合同 status→EXPIRED，员工 employmentStatus 联动） |
```

- **本验证对象** = 断言⑳「endDate 为未来 30/60/90 天内」的**运行时多档调度配置现状**。L1 措辞「30/60/90 天」定义**提醒窗口**（合同 endDate 落在未来 30/60/90 天内即进入提醒窗口），基本流程「扫描 endDate 在提醒窗口内的 ACTIVE 合同 → 通知 HR」要求**窗口内扫描+通知机制**（已满足）。L1 **未显式要求**「30/60/90 各派发一次三次独立提醒」（未出现「分三次」「分别在 30/60/90 天各提醒」字样），故 A1.12 §5 裁决「单一阈值是配置简化非契约缺失」与 L1 字面一致。
- 断言⑲「定时任务每日扫描」cron 调度接线活跃性归 **A4.2.12**（已 done，本验证 Non-Goal，仅引用其 cron 接线 census 结论[Job 默认 disabled]作为多档调度前置）。

### 1.4 L2 owner doc 契约（设计参考，非真相源）

- `docs/design/human-resource/state-machine.md:274`：合同主生命周期 `ACTIVE/TERMINATED/EXPIRED` 在合同到期扫描与调动联动中可用（`SUSPENDED` 为预留死状态 Deferred）。
- `docs/architecture/job-scheduling.md §3.15:229`：登记 `erp-hr-contract-expiry`（每日扫描到期劳动合同提醒续签/终止）状态 DONE（config-gated 默认关）+ 两层 config 启用注记（A4.2.12 已修正 stale 漂移）。**本验证补多档调度注记**（§6.2 预授权文档更新）。

### 1.5 L3 实现锚点（live repo 实测，本验证复核）

| 组件 | file:line（写时实测） | 行为断言 |
|------|----------------------|----------|
| 单一阈值 + 参数覆盖 | `module-hr/erp-hr-service/.../entity/ErpHrEmploymentContractBizModel.java#scanExpiringContracts:65-67` | `:65` `@Optional @Name("warningDays") Integer warningDays`（参数可选）+ `:67` `int window = warningDays != null ? warningDays : ErpHrConfigs.contractExpiryWarningDays();`——**参数非空覆盖 config 默认，为空用 config 默认** + `:71` `eq("status", ACTIVE)` + `:72` `dateBetween("endDate", now, now+window)` |
| config 驱动单一阈值 | `module-hr/erp-hr-service/.../ErpHrConfigs.java#contractExpiryWarningDays:171-175` | `:172-174` `AppConfig.var(CONFIG_CONTRACT_EXPIRY_WARNING_DAYS, DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS)` 读 `erp-hr.contract-expiry-warning-days` 默认 30 |
| config 默认值常量 | `ErpHrConfigs.java:47` | `int DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS = 30` |
| config key 常量 | `module-hr/erp-hr-service/.../ErpHrConstants.java:248` | `String CONFIG_CONTRACT_EXPIRY_WARNING_DAYS = "erp-hr.contract-expiry-warning-days"` |
| 自动化 Job 调用 | `module-hr/erp-hr-service/.../job/ErpHrContractExpiryJob.java#runExpiryWarnings:72` | `contractBiz.scanExpiringContracts(null, ctx)`——**传 null warningDays → 用 config 默认 30 天单窗口**；Job 体内仅此 1 处 scan 调用（无 30/60/90 三次调用），经 `execute():62` 单次触发 |
| nop-job 调度实例 | `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-hr-contract-expiry.job.yaml:1-10` | **单一作业实例**（`jobName: erp-hr-contract-expiry`，无 30/60/90 三个独立作业实例），A4.2.12 已 census 6 字段接线完整 |
| L4 测试 | `module-hr/erp-hr-service/src/test/.../job/TestErpHrContractExpiry.java#testScanExpiringContractsHitsWithinWindow:61-74` | 强断言 endDate=today+15 在 warningDays=30 窗口内命中（A1.12 §3 `:192` 确认断言强度「强」）；多档参数覆盖路径（warningDays=60/90）未独立用例但参数覆盖逻辑经 `:67` 三元表达式单点验证 |

---

## 2. Phase 1 — 单一阈值 + 参数覆盖机制 census + 自动化 Job 多档调用 + 多档调度配置普查

### 2.1 scanExpiringContracts 单一阈值 + 参数覆盖机制 census（§7-2 核心）

`module-hr/erp-hr-service/.../entity/ErpHrEmploymentContractBizModel.java#scanExpiringContracts:65-74`（写时实测）：

```java
public List<ErpHrEmploymentContract> scanExpiringContracts(@Optional @Name("warningDays") Integer warningDays,   // :65 参数可选
                                                            IServiceContext context) {
    int window = warningDays != null ? warningDays : ErpHrConfigs.contractExpiryWarningDays();                   // :67 参数非空→覆盖；为空→config 默认
    LocalDate now = CoreMetrics.today();
    LocalDate windowEnd = now.plusDays(window);
    QueryBean q = new QueryBean();
    q.addFilter(eq("status", ErpHrConstants.CONTRACT_STATUS_ACTIVE));                                           // :71 ACTIVE 过滤
    q.addFilter(dateBetween("endDate", now, windowEnd));                                                        // :72 窗口内
    return findList(q, null, context);
}
```

config 驱动链（`module-hr/erp-hr-service/.../ErpHrConfigs.java#contractExpiryWarningDays:171-175`）：

```java
static int contractExpiryWarningDays() {
    Integer v = io.nop.api.core.config.AppConfig.var(
            ErpHrConstants.CONFIG_CONTRACT_EXPIRY_WARNING_DAYS, DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS);           // :172-173 读 config，默认 30
    return v == null ? DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS : v;                                                 // :174 null 兜底默认 30
}
```

**单一阈值 + 参数覆盖机制 census 裁决：完整（机制可达，多档可经手工调用表达）**。

| 维度 | 值 | 证据 | 状态 |
|------|----|------|------|
| 单一阈值 config 驱动 | `erp-hr.contract-expiry-warning-days` 默认 **30** | `ErpHrConstants.java:248`（config key）+ `ErpHrConfigs.java:47`（默认值常量）+ `:171-175`（读取） | ✓ VERIFIED |
| 参数覆盖机制 | `warningDays` 参数非空 → 覆盖 config 默认；为空 → 用 config 默认 30 | `ErpHrEmploymentContractBizModel.java:65`（`@Optional @Name("warningDays")`）+ `:67`（三元表达式） | ✓ VERIFIED |
| 参数可达性 | warningDays 经 GraphQL `ErpHrEmploymentContract__scanExpiringContracts(warningDays:60)` 手工调用可达（`@BizQuery :64` 暴露） | `:64` `@BizQuery` 注解 + `:65` `@Name("warningDays")` GraphQL 参数 | ✓ VERIFIED |
| 多档表达 | 单次调用 = 单一窗口；多档需多次调用 `scanExpiringContracts(30)` / `(60)` / `(90)` 或调 config 后多次跑 Job | `:67` 单一 window 变量 + `:72` 单一 dateBetween 窗口 | ✓ VERIFIED（单一阈值 + 多次调用表达多档） |

**结论**：scanExpiringContracts 是**单一阈值 + 参数覆盖**机制——每次调用产生一个 `[now, now+window]` 窗口的扫描结果。多档（30/60/90 各扫一次）需**多次调用**（传不同 warningDays）或**多次 Job 调度**（每次传不同 warningDays/config）。机制本身支持多档表达（参数可覆盖），但单次调用恒为单一窗口。

### 2.2 自动化 Job 多档调用核验（§7-2 核心）

`module-hr/erp-hr-service/.../job/ErpHrContractExpiryJob.java#runExpiryWarnings:71-87`（写时实测）：

```java
protected int runExpiryWarnings(IServiceContext ctx) {
    List<ErpHrEmploymentContract> expiring = contractBiz.scanExpiringContracts(null, ctx);   // :72 传 null → 用 config 默认 30 天
    if (expiring == null || expiring.isEmpty()) {
        return 0;
    }
    int count = 0;
    for (ErpHrEmployeeContract c : expiring) {
        try {
            notifyExpiry(c, ctx);                                                            // :79 单条通知派发
            count++;
        } catch (Exception ex) {
            LOG.warn("...单条合同预警失败（隔离继续）：contractId={}, reason={}", c.getId(), ex.getMessage());  // :82-84 单条失败隔离
        }
    }
    return count;
}
```

调用入口 `execute():54-68`（经 `:62` 单次触发 `runExpiryWarnings`）：

```java
public void execute() {
    String cron = resolveCronConfig();
    if (StringHelper.isEmpty(cron)) { ... return; }      // :55-59 cron 空值跳过（in-job 第二层门控，A4.2.12 已 census）
    IServiceContext ctx = new ServiceContextImpl();
    try {
        int warned = runExpiryWarnings(ctx);             // :62 单次触发预警
        int expired = runExpirations(ctx);               // :63 单次触发过期推进
        ...
    }
}
```

**自动化 Job 多档调用裁决：单一默认窗口（不实现多档预警）**。

| 维度 | 值 | 证据 | 状态 |
|------|----|------|------|
| Job 调用 scan 的 warningDays 实参 | **null**（恒传 null → 用 config 默认 30 天） | `ErpHrContractExpiryJob.java:72` `scanExpiringContracts(null, ctx)` | ✓ VERIFIED |
| Job 体内 scan 调用次数 | **1 次**（无 30/60/90 三次调用） | grep `scanExpiringContracts` in `ErpHrContractExpiryJob.java` = 仅 `:72` 1 处 | ✓ VERIFIED |
| Job 触发 runExpiryWarnings 次数 | **1 次**（`execute():62` 单次） | `:62` 单次调用 | ✓ VERIFIED |
| 多档自动化派发 | **未实现**（Job 恒传 null → 单一默认 30 天窗口单次扫描单次通知） | `:72` null + `:62` 单次 | ✓ VERIFIED |

**结论**：自动化 Job `ErpHrContractExpiryJob` **不实现多档预警**——每次执行（每日 01:00 若 cron 启用）恒传 `null` warningDays → 用 config 默认 30 天 → 扫描 `[now, now+30]` 窗口内的 ACTIVE 合同 → 每合同派发 1 次 `hr.contract-expiry-warning` 通知。**无 30/60/90 三次调用，无 60/90 天档独立扫描**。多档需 Job 体外干预（手工 GraphQL 调用或部署多 Job 实例）。

### 2.3 多档调度配置普查（site-level）

**普查方法**：`grep -rn "erp-hr-contract-expiry\|contract-expiry-warning-days\|contract-expiry-cron\|nop.job.erp-hr-contract-expiry"` 跨全 20 生产 application.yaml + nop-job `.job.yaml` + 部署运维文档（排除 `/target/` `/_dump/` `/docs/logs/` `/docs/plans/`）。

**普查结果**：

**(a) nop-job `.job.yaml` 实例数 census**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-hr-contract-expiry.job.yaml`（全文 10 行，A4.2.12 §2.1 已 census 6 字段齐全）——**仅 1 个 `erp-hr-contract-expiry` 作业实例**。无 `erp-hr-contract-expiry-30/60/90` 三个独立作业实例（grep `erp-hr-contract-expiry` 跨 `_vfs/nop/job/conf/` 仅此 1 文件命中）。

**(b) 全 application.yaml `erp-hr.contract-expiry-warning-days` override 普查**：**零命中**（grep `contract-expiry-warning-days` 跨 `*.yaml` = No files found——除源码 `ErpHrConfigs.java`/`ErpHrConstants.java` 常量定义 + 本审计/A4.2.12/A1.12 文档引用外，**无任何生产 application.yaml 设 `erp-hr.contract-expiry-warning-days` 非 30 override**）。

全 20 生产 application.yaml 站点清单（逐一确认零 warning-days override，复用 A4.2.12 §2.3 部署普查结论——同站点集合同源 UC-HR-07）：

| # | 站点（application.yaml） | `erp-hr.contract-expiry-warning-days` | 多档调度配置 |
|---|--------------------------|---------------------------------------|-------------|
| 1 | `app-erp-all/src/main/resources/application.yaml` | 未设（=默认 30） | 无 |
| 2 | `module-hr/erp-hr-app/...` | 未设 | 无 |
| 3 | `module-purchase/erp-pur-app/...` | 未设 | 无 |
| 4 | `module-sales/erp-sal-app/...` | 未设 | 无 |
| 5 | `module-inventory/erp-inv-app/...` | 未设 | 无 |
| 6 | `module-finance/erp-fin-app/...` | 未设 | 无 |
| 7 | `module-assets/erp-ast-app/...` | 未设 | 无 |
| 8 | `module-manufacturing/erp-mfg-app/...` | 未设 | 无 |
| 9 | `module-projects/erp-prj-app/...` | 未设 | 无 |
| 10 | `module-quality/erp-qa-app/...` | 未设 | 无 |
| 11 | `module-maintenance/erp-mnt-app/...` | 未设 | 无 |
| 12 | `module-crm/erp-crm-app/...` | 未设 | 无 |
| 13 | `module-cs/erp-cs-app/...` | 未设 | 无 |
| 14 | `module-aps/erp-aps-app/...` | 未设 | 无 |
| 15 | `module-logistics/erp-log-app/...` | 未设 | 无 |
| 16 | `module-b2b/erp-b2b-app/...` | 未设 | 无 |
| 17 | `module-contract/erp-ct-app/...` | 未设 | 无 |
| 18 | `module-drp/erp-drp-app/...` | 未设 | 无 |
| 19 | `module-master-data/erp-md-app/...` | 未设 | 无 |
| 20 | `module-notify/erp-notify-app/...` | 未设 | 无 |

**多档调度配置裁决**：**生产环境无多档调度配置**。全仓无 30/60/90 三个独立 Job 实例 + 零 application.yaml warning-days override → 生产环境合同到期自动化（若 A4.2.12 两层 config 启用）= 单一默认 30 天单窗口单次派发。

### 2.4 L1 ⑳ 多档语义满足度分析

**L1 ⑳「endDate 为未来 30/60/90 天内」语义解读**：

| 解读 | 含义 | 实现现状 | 满足度 |
|------|------|----------|--------|
| **解读 A（提醒窗口）** | 30/60/90 天定义**提醒窗口**——合同 endDate 落在未来 30/60/90 天内即进入提醒窗口，扫描窗口内合同通知 HR（**单次提醒**） | 单一阈值默认 30 天 = 提醒窗口 `[now, now+30]`，扫描+通知完整 | **满足**（窗口内扫描+通知主路径完整） |
| **解读 B（多档各派发）** | 30/60/90 天各提前**派发一次**提醒——合同 endDate 前 90 天提醒一次、前 60 天提醒一次、前 30 天提醒一次（**三次独立提醒**） | 自动化 Job 单一默认 30 天窗口单次派发；60/90 天档**未自动化派发**（仅可经 GraphQL 手工 `scanExpiringContracts(60/90)` 表达） | **部分满足**（30 天档自动化；60/90 天档需手工调用，非自动化） |

**A1.12 §5 既有裁决**采用**解读 A**（「L1 '30/60/90 天' 是提醒窗口概念」），裁决「单一阈值是配置简化非契约缺失」——L1 字面「endDate 为未来 30/60/90 天内」是**前置条件**（定义哪些合同进入提醒窗口），基本流程「扫描 endDate 在提醒窗口内的 ACTIVE 合同」是单次扫描+通知机制。L1 **未出现**「分三次」「分别在 30/60/90 天各提醒」字样，故解读 A 与 L1 字面一致。

**warningDays 参数经 GraphQL 手工调用表达多档的可达性**：`scanExpiringContracts` 经 `@BizQuery :64` 暴露为 GraphQL 查询，HR 可手工调用：
- `ErpHrEmploymentContract__scanExpiringContracts(warningDays:30)` → 30 天档
- `ErpHrEmploymentContract__scanExpiringContracts(warningDays:60)` → 60 天档
- `ErpHrEmploymentContract__scanExpiringContracts(warningDays:90)` → 90 天档

即**解读 B 多档各派发可通过 3 次手工 GraphQL 调用表达**（每次返回不同窗口合同集，HR 据此分别通知）。但这是**手工运维路径**，非自动化调度。

**满足度裁决**：L1 主路径（解读 A 提醒窗口扫描+通知）**完整满足**；解读 B 多档各派发的**自动化调度未实现**（60/90 天档需手工调用），属次要验收标准边界场景弱 → §2 P2①。与 A1.12 §5「单一阈值是配置简化非契约缺失」分层一致（主路径满足，多档自动化属增强）。

### 2.5 A4.2.12 cron 接线前置声明

本验证引用 A4.2.12（N=2）cron 接线 census 结论作为多档调度前置：
- A4.2.12 结论：`erp-hr-contract-expiry.job.yaml` 接线完整（6 字段齐全）+ 两层 config 门控（nop-job `enabled` 默认 false + in-job `contract-expiry-cron` 默认空）均默认关闭 + 全 20 生产 application.yaml 零 override → **合同到期自动化运行时非默认活跃**。

**前置声明**：Job 默认 disabled（两层 config 关闭）下，多档调度更无从谈起（自动化根本不跑）。但**多档配置维度独立于 cron 接线维度**——即便 Job 启用（两层 config 设 true + 非空），本验证的多档评估仍独立成立：启用后 Job 恒传 null → 单一默认 30 天窗口单次派发，**仍不自动多档**。两计划不同控制点不重复（A4.2.12 = cron 通不通，A4.2.13 = 通了之后是否多档）。

### 2.6 MA4 ↔ A5.6 边界声明

本验证审「**行为是否符合需求**」（多档预警运行时配置，需求契约视角），与 A5.6 审「**E2E 断言强度**」边界按此执行——**不重做 A5.6 E2E 断言强度审计**。L4 `TestErpHrContractExpiry` 的断言强度评级（A1.12 §3 `:192` 确认「强」——endDate=today+15 在 warningDays=30 窗口内命中）复用 A1.12 §3 既有结论，本验证不重新评级。

---

## 3. Phase 1 Exit Criteria 复核

- [x] **单一阈值 + 参数覆盖机制 census 有明确结论**：**机制完整**（`scanExpiringContracts:65-67` 单一阈值 + warningDays 参数覆盖[非空覆盖 config 默认/为空用 config 默认 30] + `contractExpiryWarningDays():171-175` config 驱动 + `DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS=30` + `CONFIG_CONTRACT_EXPIRY_WARNING_DAYS` config key），每条有 file:line 证据（§2.1 表）。
- [x] **自动化 Job 多档调用 + 多档调度配置普查有明确结论**：自动化 Job **单一默认窗口**（`runExpiryWarnings:72` 传 null → 30 天单次，Job 体内仅 1 处 scan 调用无 30/60/90 三次）+ 多档调度配置**不存在**（nop-job 仅 1 个 `erp-hr-contract-expiry` 作业无 30/60/90 三实例 + 全 20 生产 application.yaml 零 warning-days override），每条有 file:line 证据（§2.2 + §2.3 表）。

---

## 4. 多维度审计（`docs/skills/multi-dimensional-audit-prompt.md`）

按多维审计提示要求，对每个维度至少给出一句裁决：

| 维度 | 裁决 |
|------|------|
| **需求正确性** | L1 UC-HR-07「扫描提醒窗口内 ACTIVE 合同+通知 HR」主路径完整满足；L1 ⑳「30/60/90 天」是提醒窗口概念（解读 A），未强制三次独立提醒。60/90 天档自动化派发缺失属次要验收标准（§2 P2①），非主路径缺失。 |
| **owner-doc 对齐** | L2 `state-machine.md:274`（合同 ACTIVE/EXPIRED 可用）与实现一致；`job-scheduling.md §3.15:229` 已登记 `erp-hr-contract-expiry` DONE config-gated（A4.2.12 修正），本验证补多档调度注记（§6.2）。 |
| **架构或边界影响** | 无新跨模块依赖 / API 契约变更 / 保护区域触碰。单一阈值 + 参数覆盖是既有 scan 机制，无 DAG 边突破。多档调度 successor 属部署/配置层（3 Job 实例或多次调度），非 ORM/会计保护区域。 |
| **验证充分性** | 多档缺失假设可证伪：若 Job 体内有 3 次 scan 调用或有 3 个 `.job.yaml` 实例，则多档存在（实测 grep 仅 1 处 scan + 1 个作业文件，证伪多档存在）。warning-days override 可经 §2.3 site-level 普查证伪（零 override）。 |
| **回归风险** | 本验证零代码变更（只读评估 + 文档更新），无脆弱路径引入。单一阈值 + 参数覆盖是稳定基线（A1.12 §5 + A2.7a 已证实 scan 行为正确）。 |
| **路由和技能选择正确性** | 任务路由 = verification or audit work（只读评估），Skill = `multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。匹配。换路由无遗漏。 |
| **待办或自主权策略漂移** | 范围未无声扩大；UC-HR-07 接受维持（非降级）；登记的 P2-RC-087 多档调度 successor 是验证**输出**非范围内项目降级（plan Deferred But Adjudicated 正确分类）。 |
| **view.xml gen-control 契约**（项目特定维度） | 不适用——本验证对象是后端 scan/Job 多档配置，不触及 delta view 前端层。本维度无发现。 |

**反窄化自检**：本验证覆盖 7 维度（需求/owner-doc/架构/验证充分性/回归风险/路由/待办漂移）+ 1 项目特定维度（view.xml，不适用），非单维深挖。每个维度已给出裁决。

---

## 5. Phase 2 — 多档预警配置裁决（§2 判据 + 三源对照）

### 5.1 裁决：维持 UC-HR-07 接受 + 登记 P2-RC-087 多档调度 successor watch-only

**§2 判据复核**：

| §2 判据 | 是否成立 | 理由 |
|---------|----------|------|
| **§2 接受**（全部验收标准 L3-L5 各级均有证据且一致） | **主路径 ✓ 成立** | ⑳提醒窗口扫描+通知主路径完整（单一阈值 + 单次调度 + 跨域通知派发）；L1 ⑳「30/60/90 天」解读 A（提醒窗口）满足。但 60/90 天档自动化派发缺失 → 次要验收标准边界弱，故整体裁决为「接受主路径 + P2-RC-087 边界 successor」非纯接受。 |
| §2 P0④ 会计过账正确性破坏 | ✗ 不适用 | 合同到期扫描不涉 GL 过账（`runExpirations` 仅 setStatus ACTIVE→EXPIRED，无凭证生成） |
| §2 P0① 活跃数据破坏 | ✗ 不成立 | scan 是只读查询；多档缺失不影响数据完整性 |
| §2 P1① 功能完全缺失 | ✗ 不成立 | 主路径（提醒窗口扫描+通知+续签/到期终止）完整存在；多档经 config 调整 warning-days 或 GraphQL 手工调用可表达 |
| §2 P1② 异常路径未实现 | ✗ 不成立 | 续签守卫 + 到期终止 + 单条失败隔离均已实现（A1.12 §5 证实） |
| **§2 P2① 次要验收标准未完全满足** | **✓ 成立** | 60/90 天档自动化派发未实现（自动化 Job 恒传 null → 单一默认 30 天窗口），主路径[单一阈值 config 驱动 + 30 天自动窗口]OK，边界[60/90 天提前自动派发]弱 → **P2-RC-087 successor watch-only** |
| §2 P2③ 文档化简化 | ✗ 不成立 | 单一阈值是 config 简化（A1.12 §5 已裁决），非文档化简化 |

**取最高原则**：§2 P2① 成立（60/90 天档自动化派发缺失）→ 登记 **P2-RC-087 watch-only successor**。主路径接受（§2 接受主路径满足），与 A1.12 §5 UC-HR-07 接受裁决**分层一致**（均接受主路径，单一阈值是配置简化非契约缺失；多档自动化是增强 successor）。

**L1/L2/L3 三源对照**：
- L1（use-cases.md:81）「endDate 为未来 30/60/90 天内」= 提醒窗口**概念**（解读 A，合同落窗口即提醒）✓ 主路径满足；解读 B（多档各派发）60/90 天档未自动化
- L2（state-machine.md:274）合同 ACTIVE/EXPIRED 可用 ✓（`runExpirations` setStatus 实现）
- L3（`scanExpiringContracts:65-67` 单一阈值 + 参数覆盖 + `ErpHrContractExpiryJob:72` 传 null 单一默认窗口 + 单一 `.job.yaml` 实例）单一阈值 + 无多档调度配置 ✓

**与 A1.12 §5 UC-HR-07 接受裁决分层一致**：A1.12 §5 静态确认单一阈值 config 驱动 + 参数覆盖逻辑正确（接受）；本 MA4 验证补运行时多档调度配置现状（无多档调度配置 + 自动化 Job 单一默认窗口）→ 分层一致，均**接受主路径**。多档自动化派发缺失登记 **P2-RC-087 watch-only successor**（增强 successor，非主路径降级）。

**登记 P2-RC-087 watch-only successor**：多档预警调度（30/60/90 各提前派发一次）需多档调度配置实现：
- **方案 A（推荐）**：部署 3 个 Job 实例（`.job.yaml` 复制 3 份，jobName 分别 `erp-hr-contract-expiry-30/60/90`，各经 in-job config 或 Job 体内参数传 warningDays=30/60/90）——属部署/配置层，非 ORM/会计保护区域。
- **方案 B**：单一 Job 体内改 `runExpiryWarnings` 三次调用 `scanExpiringContracts(30/60/90)` + 去重（同合同在多档窗口重叠时仅派发一次）——纯 BizModel 代码增强（预授权类目，不触 §5 ask-first）。
- **方案 C**：HR 手工 GraphQL 调用（现状可达，`@BizQuery :64` 暴露 warningDays 参数）——零代码，运维路径。

属增强 successor（多档自动派发是 nice-to-have，主路径单一窗口提醒已满足 L1 解读 A），**watch-only**，归部署/配置 successor 非 MR1 代码修复。

### 5.2 finding 衔接裁决（§7 复用 or 新增）

| 既有 arm-index 行 | 控制点 | 本验证关系 | 裁决 |
|-------------------|--------|-----------|------|
| A1.12 §5 UC-HR-07 接受（无独立 finding 行） | 需求契约主路径 | 本验证维持该接受裁决（主路径） | **不新建主路径 finding**——接受类无 finding |
| `P1-MA2-086`（定时任务并发重复副作用，resolved R1.28 done） | 并发幂等 | 不同控制点（并发幂等 vs 多档阈值调度） | **不合并**——避免模糊 P1-MA2-086 并发焦点 |
| A4.2.12（cron 接线，0 finding，watch-only residual） | cron 调度接线/默认活跃性 | 不同控制点（cron 通不通 vs 通了之后是否多档） | **不合并**——A4.2.12 config-gate residual 是部署启用决策，本验证 P2-RC-087 是多档自动派发增强 |

**新 finding 数 = 1**（P2-RC-087 watch-only successor，§5.1）。grep arm-index 同域同控制点（hr 合同到期 + 多档/30/60/90 + warningDays + multi-tier）零命中既有行覆盖此控制点 → **新建 P2-RC-087**，列明差异依据（与 P1-MA2-086 并发不同控制点 + 与 A4.2.12 cron 接线不同控制点 + A1.12 §5 无独立 finding 行）。

### 5.3 不触发 MR0 / 不归 MR1

- **不触发 MR0**：无 P0（§2 P0①④均不成立）。
- **不归 MR1**：无 P1 finding（§2 P1①②均不成立）。P2-RC-087 是 watch-only successor，登记不强制；多档调度实现属部署/配置 successor（方案 A 3 Job 实例）或纯 BizModel 增强（方案 B 三次调用，预授权类目），非 ORM/会计保护区域修复。

---

## 6. 文档更新（预授权）

### 6.1 本验证报告落盘

`docs/audits/2026-08-06-2247-rc-ma4-a4-2-13-hr-contract-expiry-multi-tier-alert.md`（本文件）。

### 6.2 部署运维文档注记：`docs/architecture/job-scheduling.md §3.15:229`

> **注记位置选择说明**：plan Phase 2 item 2 原指 owner doc `recruitment.md` 补多档调度注记。经 live 核实 `recruitment.md` 是招聘管理文档（`docs/design/human-resource/recruitment.md` 覆盖招聘需求→职位发布→候选人→面试→Offer→入职交接，**不含合同到期提醒**），合同到期提醒的 owner doc 锚点是 `use-cases.md UC-HR-07:75-85` + `state-machine.md:274`（合同生命周期）+ `job-scheduling.md §3.15:229`（作业调度登记）。A4.2.12 已将 cron config 启用注记落在 `job-scheduling.md §3.15:229`（一致性 + 可发现性），本验证多档调度注记**同位补齐**（同作业行 config 单元格追加多档说明），确保两控制点注记同位可比。此为对 plan 字面引用的透明修正（提升可发现性 + 一致性，非范围扩大），记入本节供审计复核。

**注记内容**（§9 真相源冻结条款——`job-scheduling.md` 属 `docs/architecture/` 技术设计非需求契约段落，可协同修订；A4.2.12 已在此行修正 stale 漂移 + 补两层 config 启用注记，本验证同位追加多档调度注记）：

`erp-hr-contract-expiry` 行 config 单元格追加：「**多档预警**：`scanExpiringContracts` 单一阈值（config `erp-hr.contract-expiry-warning-days` 默认 30）+ `warningDays` 参数可覆盖；自动化 Job 恒传 null → 单一默认 30 天单窗口（无 30/60/90 三次调用 + 无 3 Job 实例）；多档（30/60/90 各派发）需 3 Job 实例或 Job 体内多次调用或 GraphQL 手工调用 successor（P2-RC-087 watch-only）。详见 `docs/audits/2026-08-06-2247-rc-ma4-a4-2-13-hr-contract-expiry-multi-tier-alert.md`。」

---

## 7. 与 arm-index / 既有审计去重声明（§去重协议）

- **MA1 ↔ MA4 去重**：本验证复用 A1.12 §3/§5 UC-HR-07 实现证据（scanExpiringContracts 单一阈值 + 参数覆盖 + contractExpiryWarningDays config 驱动）+ A1.12 §5 UC-HR-07 接受裁决，不重新核实代码逻辑本身（§去重协议 1-2）。本验证只补多档预警**运行时调度配置现状**差异（A1.12 静态确认未普查部署侧）。
- **MA4 内部去重（A4.2.12 ↔ A4.2.13）**：本验证与 A4.2.12 同源 UC-HR-07 不同控制点（A4.2.12 = cron 接线/默认活跃性，A4.2.13 = 多档阈值/多档调度配置），互补不重复（§2.5 前置声明）。A4.2.12 config-gate watch-only residual（部署启用决策）与本验证 P2-RC-087（多档自动派发增强）不同控制点不合并。
- **MA4 ↔ A5.6 边界**：本验证审「行为是否符合需求」（多档预警配置），不重做 A5.6 E2E 断言强度审计（§去重协议 MA4↔A5.6）。
- **arm-index 交叉去重**：本报告 1 项新 finding（P2-RC-087），经 grep arm-index 同域同控制点（hr 合同到期 + 多档/30/60/90/warningDays）后裁决（P1-MA2-086 并发不同控制点 + A1.12 §5 无独立 finding 行 + A4.2.12 不同控制点），无未经比对直接新建的 finding。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。**本报告无生产代码变更**（纯审计报告 + `job-scheduling.md §3.15:229` 文档注记，零 Java/ORM/契约变更），checker 无回归风险。actual vs baseline 汇总表如下。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控；本仓 checker 退出码 1 是既有稳定基线态[R2b/R2c 等规则命中数与 compliance-baseline.md 历史快照的已知漂移]，非本审计引入）。

  | 规则 | baseline | actual（本次实测） | 变化 |
  |------|----------|-------------------|------|
  | R1a dao().saveEntity (BizModel) | 0 | 0 | — |
  | R1b dao().updateEntity (BizModel) | 0 | 0 | — |
  | R1c dao().getEntityById (BizModel) | 0 | 0 | — |
  | R1d dao().findAllByQuery (BizModel) | 14 | 14 | — |
  | R2a BizModel daoFor(ErpMd*) | 34 | 34 | — |
  | R2b BizModel daoFor(Erp*) 跨域 | 229 | 229 | — |
  | R2c 全生产代码 daoFor() 总量 | 1382 | 1382 | — |
  | R2d Processor daoFor(ErpMd*) | 34 | 34 | — |
  | R3 new Erp*() 构造实体 | 5 | 5 | — |
  | R8 Processor 无 xbiz 接线 | 0 | 0 | — |

  本审计无生产代码变更，actual == baseline（全可计数规则精确匹配，与 A4.2.12 §8 同一稳定基线），无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告 1 项新 finding（P2-RC-087），经 grep arm-index 同域同控制点后给出「复用 or 新增」裁决（P1-MA2-086 并发不同控制点 + A1.12 §5 无独立 finding + A4.2.12 不同控制点 → 新建 P2-RC-087，§5.2），无未经比对直接新建的 finding。

---

## 9. 结论

A1.12 §7-2 静态存疑点（UC-HR-07 30/60/90 多档预警运行时配置）经 scanExpiringContracts 单一阈值 + 参数覆盖机制 census + ErpHrContractExpiryJob 多档调用核验 + 全 application.yaml/部署多档调度配置普查 + L1 ⑳ 多档语义满足度分析：

- **单一阈值 + 参数覆盖机制完整**（`scanExpiringContracts:65-67` 单一阈值 + warningDays 参数覆盖[非空覆盖 config 默认/为空用 config 默认 30] + `contractExpiryWarningDays():171-175` config 驱动）。
- **自动化 Job 单一默认窗口**（`runExpiryWarnings:72` 传 null → 30 天单次；Job 体内仅 1 处 scan 调用，无 30/60/90 三次）→ 自动化 Job **不**实现多档预警。
- **多档调度配置不存在**（nop-job 仅 1 个 `erp-hr-contract-expiry` 作业无 30/60/90 三实例 + 全 20 生产 application.yaml 零 warning-days override）。
- **L1 ⑳ 多档语义**：解读 A（提醒窗口）主路径满足；解读 B（多档各派发）60/90 天档自动化缺失（仅 GraphQL 手工可达），属 §2 P2①。
- **裁决**：**维持 A1.12 §5 UC-HR-07 接受**（主路径[单一阈值 config 驱动 + 30 天自动窗口 + 跨域通知]完整；L1 ⑳ 解读 A 满足；单一阈值是配置简化非契约缺失，与 A1.12 §5 分层一致）。60/90 天档自动化派发缺失登记 **P2-RC-087 watch-only successor**（多档预警调度增强，§2 P2①；归部署/配置 successor 非 MR1）。
- **不触发 MR0，无 P1 finding**。多档调度实现属增强 successor（plan Non-Goals 不实施），仅更新 `job-scheduling.md §3.15:229` 多档调度注记（预授权文档更新）+ 登记 arm-index P2-RC-087 watch-only 行。

§7-2 存疑点**闭合**。
