# RC MA4 A4.2.12 — UC-HR-07 合同到期 cron 运行时调度接线确认（A1.12 §7-1） 验证报告

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 运行时行为验证）
> 工作项：A4.2.12（MA4 运行时行为验证 — A1.12 §7-1：MA4 运行时行为验证 — UC-HR-07 合同到期提醒 cron 运行时调度接线，`ErpHrContractExpiryJob.execute()` 经 nop-job `.job.yaml` 反射调用 + 两层 config 门控[nop-job enabled 默认 false + in-job contract-expiry-cron 空值跳过]运行时是否实际活跃）
> 验证 plan：`docs/plans/2026-08-06-2247-2-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4 Q1 真相源层级与冲突裁决 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §去重协议）
> 输入存疑点：A1.12 §7-1（`docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md:322`）
> 关联既有裁决：A1.12 §5 UC-HR-07 = **接受**（cron-gated Job + 单一可配置提醒窗口 + 续签/到期终止完整 + 跨域通知派发；⑮不续签→RESIGNED 复用 P1-MA2-039 successor Deferred resolved R1.15）
> 关联同型范式：A4.1.4（`2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`，done — config 默认关闭 vs 部署契约核对先例，本验证对齐其「config-gate default-off = 部署启用决策非契约缺失」判据框架）+ A4.1.7（`2026-08-07-0944-rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census.md`，done — config-default-off 部署普查同型范式）+ A4.1.14（`2026-08-07-1400-rc-ma4-a4-1-14-bank-recon-auto-reverse-config-orphan-awareness.md`，done — config 消费点全集普查范式，方向相反：A4.1.14 config-on-but-no-consumer vs 本验证 config-off-deployment-census）
> 关联 finding：`P1-MA2-086`（定时任务并发重复副作用合并裁决，resolved R1.28 done — 描述内已登记「`erp-hr-contract-expiry` 全部默认 enabled=false（参考部署 silent）」，本验证为该部署 silent 状态补 site-level 普查证据）
> 验证性质：**只读 cron 接线活跃性评估**（grep nop-job `.job.yaml` 接线 + 两层 config 默认值 + 全 application.yaml 部署 override 普查 + BeanMethodJobInvoker 反射调用链 + 命名对账 + module-meta.yaml configKey 完整性；不改代码/ORM/api.xml/config 默认值/真相源；方法论 §5 保护区域，roadmap 预授权类目「只读评估」+「文档更新类修复」）
> 验证日期：2026-08-06
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **A1.12 §7-1 存疑点裁决** | **维持 UC-HR-07 接受**（cron-gated 机制完整接线 + 运行时可达；两层 config 门控默认关闭是部署启用决策，非契约缺失） | 不升级 finding |
| nop-job `.job.yaml` 接线 census | **接线完整**（`erp-hr-contract-expiry.job.yaml` 6 字段齐全：jobName/enabled/cronExpr/invoker.bean/invoker.method + bean 注册 + 反射目标 no-arg public `execute()`） | 接线断裂证伪 |
| 两层 config 门控默认值 | **均默认关闭**：①nop-job 层 `nop.job.erp-hr-contract-expiry.enabled` 默认 **false**；②in-job 层 `erp-hr.contract-expiry-cron` 默认 **空**（`AppConfig.var(..., "")` 空值跳过） | 合同到期自动化运行时**非默认活跃** |
| 全 application.yaml 部署 override 普查 | **零命中**（全 20 生产 application.yaml + README/seed/部署运维文档 — 无任何站点设 `nop.job.erp-hr-contract-expiry.enabled=true` 或 `erp-hr.contract-expiry-cron` 非空 override） | 生产环境默认值 = 代码默认值 = 双层关闭 |
| config key 命名一致性 | **两层不同约定**（nop-job 层 `nop.job.erp-hr-contract-expiry.*` vs in-job 层 `erp-hr.contract-expiry-cron`），运维须**同时设两层**才能激活——双层门控是 BY DESIGN 安全设计（job 推进员工合同 ACTIVE→EXPIRED + 派发通知，须显式 opt-in 双确认） | 轻微运维认知面，watch-only |
| 新 finding | **0**（对齐 A4.1.4 = 0 新 finding） | 无新控制点（cron-gated 机制存在，config-gate 是部署启用决策） |
| MR0 触发 | **无** | — |

**整体裁决**：A1.12 §7-1 静态存疑点（「`ErpHrContractExpiryJob.execute()` 依赖 `scheduler.yaml` cronExpr 反射调用 + `erp-hr.contract-expiry-cron` config 非空。运行时 scheduler.yaml 是否实际接线 + cron config 是否非空需 MA4 运行时确认」）经 nop-job `.job.yaml` 接线 census + 两层 config 门控默认值 census + 全 application.yaml 部署 override 普查 + BeanMethodJobInvoker 反射调用链核验 **CONFIRMED 接线完整 + 运行时可达，但两层 config 门控叠加默认关闭致合同到期自动化运行时非默认活跃**。

按 §2 分级判据三源复核：①**§2 接受判据满足**——L1 UC-HR-07:80「定时任务每日扫描即将到期的合同」要求 cron 调度**机制存在**（已满足：`.job.yaml` 接线 + bean 注册 + `ErpHrContractExpiryJob.execute()` 两步执行[scan/expire] + `IErpSysNotificationBiz.notify` 跨域通知派发 + `TestErpHrContractExpiry` 7 方法强测全绿），L1 未要求「开箱默认启用」；②**§2 P1①「功能完全缺失」不成立**——功能存在且完整，仅由两层 config-gate 条件启用（nop-job enabled + in-job cron 非空）；③**§2 P1②「异常路径未实现」不成立**——两步执行（`runExpiryWarnings` 单条失败隔离 + `runExpirations` ACTIVE→EXPIRED）+ 续签守卫 + 跨域通知完整，非缺失；④**§2 P0④「会计过账正确性破坏」不适用**——合同到期扫描不涉 GL 过账。故两层 config 门控默认关闭属 **ERP 通用 opt-in 启用范式**（config 默认关闭 = 部署启用决策，非契约缺失），**对齐 A4.1.4 budget config 范式**（A4.1.4 TL;DR：`新 finding | 0 | 无新控制点（caveat ① 维持接受，不构成需求分歧）`）+ A4.1.7 commitment-release-on-return 范式（deployment 实际启用状态 = 关闭，维持接受不升级 finding）。

**不触发 MR0，无新 finding，维持 A1.12 §5 UC-HR-07 接受。** 登记两层 config-gate 默认关闭为 **watch-only residual**（部署启用决策）：部署启用合同到期自动化时须**同时设** `nop.job.erp-hr-contract-expiry.enabled=true` + `erp-hr.contract-expiry-cron` 非空（运维 config 决策，非代码修复）。本验证**不实施 config 默认值变更**（属部署配置决策；plan Non-Goals），仅更新部署运维文档（`docs/architecture/job-scheduling.md §3.15` 修正 stale `erp-hr-contract-expiry-reminder` DESIGN 条目 + 补 config 启用注记，预授权文档更新）。

> **与 A4.1.14 方向差异声明**：A4.1.14（bank-recon auto-reverse）config 默认 **true** 但**零消费**（config-on-but-no-consumer，孤儿化隐性失效，维持既有 P1-RC-005）；本验证 A4.2.12 两层 config 均**默认关闭**且**消费链完整**（config-off-but-fully-wired，opt-in 部署启用决策，无 finding）。两者判据框架互补：A4.1.14 探「运维是否误以为生效」；A4.2.12 探「部署是否隐式启用 + 接线是否断裂」。后者因接线完整 + config-gate 是显式安全 opt-in，误导面低于前者。

---

## 1. 输入存疑点原文 + L1/L2/L3 锚点

### 1.1 输入存疑点原文（A1.12 §7-1，逐字引用）

> **UC-HR-07 cron 运行时调度接线**：`ErpHrContractExpiryJob.execute()` 依赖 `scheduler.yaml` cronExpr 反射调用 + `erp-hr.contract-expiry-cron` config 非空。运行时 scheduler.yaml 是否实际接线 + cron config 是否非空需 MA4 运行时确认（本切片静态确认代码门控逻辑正确 + 单测覆盖 cron 空/非空两路径）。
> — `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md:322`

### 1.2 A1.12 §5 UC-HR-07 既有接受裁决（输入，本验证复核分层一致性）

A1.12 §5（`:257-259`）对 UC-HR-07 给出**接受**裁决，断言⑲逐条核验：
- ⑲cron 调度——`ErpHrContractExpiryJob.execute():54` cron config-gated + `TestErpHrContractExpiry#testJobCronConfiguredTriggersBothSteps` 强断言 ✓
- ⑳30/60/90 提醒窗口——单一可配置阈值默认 30 天（L1 多档概念可表达，归 A4.2.13）✓
- ㉑扫描 ACTIVE 合同 ✓ / ㉒通知 HR（跨域 `IErpSysNotificationBiz.notify`）✓ / ㉓续签 ✓ / ㉔到期终止 ✓

**本验证对象** = §7-1 残留存疑点「运行时 scheduler.yaml 是否实际接线 + cron config 是否非空」的部署/运行时活跃性（A1.12 静态确认代码门控逻辑正确 + 单测覆盖两路径，但未普查部署侧 application.yaml override + nop-job `.job.yaml` 接线 census）。本验证**不重复核实**断言⑲-㉔代码逻辑（A1.12 §5 已证 + A2.7a 状态机复用 pass），只评 cron 接线**运行时活跃性**差异。

### 1.3 L1 需求契约（UC-HR-07，逐字）

`docs/design/human-resource/use-cases.md:75-85`：

```
## UC-HR-07 合同到期提醒
| **概述** | 劳动合同到期前自动提醒 HR，支持续签或终止 |
| **触发条件** | 定时任务每日扫描即将到期的合同 |          ← 断言⑲（本验证核心）
| **前置条件** | ErpHrEmploymentContract.status = ACTIVE，endDate 为未来 30/60/90 天内 |
| **基本流程** | 1. 系统扫描 endDate 在提醒窗口内的 ACTIVE 合同
                2. 通知 HR 管理员
                3. HR 操作续签（创建新合同，原合同 endDate 不变但 status→EXPIRED）
                4. 或到期终止（原合同 status→EXPIRED，员工 employmentStatus 联动） |
```

- **本验证对象** = 断言⑲「定时任务每日扫描」的**运行时调度接线活跃性**。L1 措辞「定时任务每日扫描」要求 cron 调度**机制存在**（已满足），**未要求**「开箱默认启用」（L1 无「默认开启 / 开箱即用」部署契约词）。
- 断言⑳「30/60/90 天」多档预警运行时配置归 **A4.2.13**（独立工作项，本验证 Non-Goal）。

### 1.4 L2 owner doc 契约（设计参考，非真相源）

- `docs/design/human-resource/state-machine.md:274`：合同主生命周期 `ACTIVE/TERMINATED/EXPIRED` 在合同到期扫描与调动联动中可用（`SUSPENDED` 为预留死状态 Deferred）。
- `docs/architecture/job-scheduling.md §3.15:229`：登记 `erp-hr-contract-expiry-reminder`（每日扫描到期劳动合同提醒续签/终止）状态 **DESIGN（待实现）**——**本验证发现该条目为 stale 漂移**：live 实现的 jobName 是 `erp-hr-contract-expiry`（无 `-reminder` 后缀），且状态应为「已实现 + config-gated 默认关闭」非「DESIGN 待实现」。修正见 §6.2（预授权文档更新）。

### 1.5 L3 实现锚点（live repo 实测，本验证复核）

| 组件 | file:line（写时实测） | 行为断言 |
|------|----------------------|----------|
| 作业逻辑类 | `module-hr/erp-hr-service/.../job/ErpHrContractExpiryJob.java:34` | `public void execute():54` 无参 public 方法适配 BeanMethodJobInvoker 反射调用；in-job 第二层门控 `:55-59` cron 空值跳过；两步执行 `runExpiryWarnings:71-87`（scan + notify + 单条失败隔离）+ `runExpirations:90-93`（ACTIVE→EXPIRED）|
| config 常量 | `module-hr/erp-hr-service/.../ErpHrConstants.java:250` | `String CONFIG_CONTRACT_EXPIRY_CRON = "erp-hr.contract-expiry-cron"` |
| cron 解析 | `ErpHrContractExpiryJob.resolveCronConfig():108-110` | `AppConfig.var(ErpHrConstants.CONFIG_CONTRACT_EXPIRY_CRON, "")` 默认**空串** |
| bean 注册 | `module-hr/erp-hr-service/src/main/resources/_vfs/erp/hr/beans/app-service.beans.xml:44` | `<bean id="erpHrContractExpiryJob" class="app.erp.hr.service.job.ErpHrContractExpiryJob"/>` |
| nop-job 调度接线 | `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-hr-contract-expiry.job.yaml:1-10` | 6 字段齐全（见 §2.1） |
| L4 测试 | `module-hr/erp-hr-service/src/test/.../job/TestErpHrContractExpiry.java` | 7 方法（surefire 实测 `tests="7" failures="0" errors="0" skipped="0"`），含 `testJobCronEmptySkipsExecution:133`（空值跳过）+ `testJobCronConfiguredTriggersBothSteps:142`（非空触发两步）+ `testExecuteIsNoArgPublicMethod:152`（反射契约） |

---

## 2. Phase 1 — nop-job 接线 census + 两层 config 门控默认值 + 部署 override 普查

### 2.1 nop-job `.job.yaml` 接线 census（§7-1 核心）

`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-hr-contract-expiry.job.yaml`（写时实测全文 10 行）：

```yaml
jobName: erp-hr-contract-expiry                                                    # :1
enabled: "@cfg:nop.job.erp-hr-contract-expiry.enabled|false"                       # :2  ← nop-job enabled 默认 FALSE
displayName: HR Contract Expiry Scan                                               # :3
description: 每日 01:00 扫描到期前合同派发提醒通知 + 推进已过期合同状态 ACTIVE→EXPIRED；cron 经 erp-hr.contract-expiry-cron 配置门控  # :4
jobGroup: erp-hr                                                                   # :5
trigger:
  cronExpr: "@cfg:nop.job.erp-hr-contract-expiry.cron-expr|0 0 1 * * ?"            # :7  ← 默认每日 01:00
invoker:
  bean: erpHrContractExpiryJob                                                     # :9  ← 反射目标 bean id
  method: execute                                                                   # :10 ← 反射目标 no-arg public 方法
```

**接线完整性裁决：完整（接线断裂证伪）**。

| `.job.yaml` 字段 | 值 | 接线状态 | 证据 |
|------------------|----|----------|------|
| `jobName` | `erp-hr-contract-expiry` | ✓ | `:1` |
| `enabled` | `@cfg:nop.job.erp-hr-contract-expiry.enabled\|false`（默认 **false**） | ✓ | `:2` |
| `trigger.cronExpr` | `@cfg:nop.job.erp-hr-contract-expiry.cron-expr\|0 0 1 * * ?`（默认每日 01:00） | ✓ | `:7` |
| `invoker.bean` | `erpHrContractExpiryJob` | ✓ bean 注册存在 | `:9` → `app-service.beans.xml:44` `<bean id="erpHrContractExpiryJob" .../>` |
| `invoker.method` | `execute` | ✓ no-arg public 方法存在 | `:10` → `ErpHrContractExpiryJob.execute():54`（`public void execute()`，L4 `testExecuteIsNoArgPublicMethod:152` 反射契约强断言） |

**BeanMethodJobInvoker 反射调用链完整可达**：
`.job.yaml invoker.bean=erpHrContractExpiryJob` → `app-service.beans.xml:44` bean 注册（`class=app.erp.hr.service.job.ErpHrContractExpiryJob`）→ `ErpHrContractExpiryJob.execute():54`（无参 public 方法，BeanMethodJobInvoker 反射调用契约满足）→ `resolveCronConfig():108-110`（in-job 第二层门控）→ 两步执行。

> §7-1 字面疑问「scheduler.yaml 是否实际接线」**CONFIRMED 接线存在**（nop-job 调度经 `.job.yaml` 接线，非 scheduler.yaml 文件名直配——本仓 nop-job 调度约定为 `nop/job/conf/*.job.yaml`，`erp-hr-contract-expiry.job.yaml` 存在且 6 字段齐全）。A1.12 §7-1 措辞「scheduler.yaml」是 cron 调度层的泛指（A1.12 §3 `:151` 已使用精确类名 `ErpHrContractExpiryJob`，无命名歧义）。

### 2.2 两层 config 门控默认值 census

| 层 | config key | 默认值 | 证据 | 门控语义 |
|----|-----------|--------|------|----------|
| ① nop-job 层 | `nop.job.erp-hr-contract-expiry.enabled` | **false** | `.job.yaml:2` `@cfg:...\|false` | nop-job 调度器不触发该作业（作业默认不执行） |
| ① nop-job 层 | `nop.job.erp-hr-contract-expiry.cron-expr` | `0 0 1 * * ?`（每日 01:00） | `.job.yaml:7` `@cfg:...\|0 0 1 * * ?` | 触发频率（仅当 enabled=true 时生效） |
| ② in-job 层 | `erp-hr.contract-expiry-cron` | **空串** | `ErpHrConstants.java:250` + `ErpHrContractExpiryJob.resolveCronConfig():108-110` `AppConfig.var(..., "")` + `execute():55-59` 空值跳过 | 即便 nop-job 触发执行，cron config 空值时 `execute()` 直接 `return`（"不调度"语义） |

**两层叠加裁决**：两层均默认关闭 → **合同到期自动化运行时非默认活跃**。即便运维仅设 nop-job `enabled=true` 而遗忘设 in-job `erp-hr.contract-expiry-cron` 非空，`execute():55-59` 第二层门控仍跳过执行（双层安全冗余）。

### 2.3 全 application.yaml 部署 override 普查（site-level）

**普查方法**：`grep -rn "erp-hr-contract-expiry\|contract-expiry-cron\|nop.job.erp-hr-contract-expiry"` 跨全 20 生产 application.yaml + README/seed/部署运维文档（排除 `/target/` `/​_dump/` `/​docs/`）。

**普查结果**：**零命中**（除 `.job.yaml` 自身定义 + 本审计/计划文档引用外，无任何生产 application.yaml 设 `nop.job.erp-hr-contract-expiry.enabled=true` 或 `erp-hr.contract-expiry-cron` 非空 override）。

全 20 生产 application.yaml 站点清单（逐一确认零 override）：

| # | 站点（application.yaml） | `nop.job.erp-hr-contract-expiry.enabled` | `erp-hr.contract-expiry-cron` |
|---|--------------------------|------------------------------------------|-------------------------------|
| 1 | `app-erp-all/src/main/resources/application.yaml` | 未设（=默认 false） | 未设（=默认空） |
| 2 | `module-hr/erp-hr-app/...` | 未设 | 未设 |
| 3 | `module-purchase/erp-pur-app/...` | 未设 | 未设 |
| 4 | `module-sales/erp-sal-app/...` | 未设 | 未设 |
| 5 | `module-inventory/erp-inv-app/...` | 未设 | 未设 |
| 6 | `module-finance/erp-fin-app/...` | 未设 | 未设 |
| 7 | `module-assets/erp-ast-app/...` | 未设 | 未设 |
| 8 | `module-manufacturing/erp-mfg-app/...` | 未设 | 未设 |
| 9 | `module-projects/erp-prj-app/...` | 未设 | 未设 |
| 10 | `module-quality/erp-qa-app/...` | 未设 | 未设 |
| 11 | `module-maintenance/erp-mnt-app/...` | 未设 | 未设 |
| 12 | `module-crm/erp-crm-app/...` | 未设 | 未设 |
| 13 | `module-cs/erp-cs-app/...` | 未设 | 未设 |
| 14 | `module-aps/erp-aps-app/...` | 未设 | 未设 |
| 15 | `module-logistics/erp-log-app/...` | 未设 | 未设 |
| 16 | `module-b2b/erp-b2b-app/...` | 未设 | 未设 |
| 17 | `module-contract/erp-ct-app/...` | 未设 | 未设 |
| 18 | `module-drp/erp-drp-app/...` | 未设 | 未设 |
| 19 | `module-master-data/erp-md-app/...` | 未设 | 未设 |
| 20 | `module-notify/erp-notify-app/...` | 未设 | 未设 |

**部署活跃性裁决**：**生产环境默认值 = 代码默认值 = 双层关闭**。全仓无任何站点启用合同到期自动化 cron。与 A4.1.14 §0 表（hr `erp-hr-contract-expiry.job.yaml` ❌ 无 app override）+ P1-MA2-086 arm-index 描述（「`erp-hr-contract-expiry` 全部默认 enabled=false（参考部署 silent）」）一致——本验证为该部署 silent 状态补 site-level 全集普查证据。

### 2.4 config key 命名一致性 + module-meta.yaml 完整性核验

**两层 config key 命名**：
- nop-job 层：`nop.job.erp-hr-contract-expiry.enabled` / `nop.job.erp-hr-contract-expiry.cron-expr`（nop-job 平台约定，前缀 `nop.job.<jobName>.*`，inline 声明于 `.job.yaml`）
- in-job 层：`erp-hr.contract-expiry-cron`（模块 config 约定，前缀 `erp-hr.*`，常量定义于 `ErpHrConstants.java:250`，经 `AppConfig.var` 读取）

**命名不一致的运维认知面裁决**：两层采用不同前缀约定（`nop.job.*` vs `erp-hr.*`），运维**必须同时设两层**才能激活合同到期自动化（设 `nop.job.erp-hr-contract-expiry.enabled=true` 单层不够——还须 `erp-hr.contract-expiry-cron` 非空，否则 `execute():55-59` 跳过）。这是 **BY DESIGN 双层安全冗余**：该 job 推进员工合同 `ACTIVE→EXPIRED` + 派发跨域通知（不可逆副作用），须显式 opt-in 双确认避免误触发。轻微运维认知面（运维可能只设一层而遗漏另一层），但非缺陷——双层门控镜像 `ErpCsEntitlementExpiryJob` 范式（`ErpHrContractExpiryJob.java:32` 类注释明示），是本仓 cron-gated Job 标准设计。**watch-only**，不构成 finding。

**module-meta.yaml configKey 声明完整性**：`module-hr/erp-hr-meta/module-meta.yaml`（+ precompile 同）`configKey` 仅声明 `erp-hr.shift-cross-day-enabled`（:10），**未声明** `erp-hr.contract-expiry-cron`。这与其他 nop-job-gated job 一致（nop-job keys `nop.job.*` inline 于 `.job.yaml`，模块 config 经 `AppConfig.var` 读取不需 module-meta 声明即可生效）——非缺陷，是平台 config 读取双通道约定（module-meta 声明 = 模块级 config 元数据；`AppConfig.var` = 运行时 config 读取，不依赖 module-meta）。完整性观察：`erp-hr.contract-expiry-cron` 可在 module-meta 登记 configKey 以提升可发现性，但属可选增强非契约缺失。**watch-only**，不构成 finding。

### 2.5 MA4 ↔ A5.6 边界声明

本验证审「**行为是否符合需求**」（cron 接线运行时活跃性，需求契约视角），与 A5.6 审「**E2E 断言强度**」边界按此执行——**不重做 A5.6 E2E 断言强度审计**。L4 `TestErpHrContractExpiry` 7 方法的断言强度评级（强断言）复用 A1.12 §3 + A2.7a 既有结论，本验证不重新评级。

---

## 3. Phase 1 Exit Criteria 复核

- [x] **nop-job 接线 census 有明确结论**：**接线完整**（`.job.yaml` 6 字段齐全 + bean 注册 + 反射目标 no-arg public `execute()` + BeanMethodJobInvoker 反射调用链完整可达 `ErpHrContractExpiryJob.execute()`），每条有 file:line 证据（§2.1 表）。
- [x] **两层 config 门控默认值 + 全 application.yaml 部署 override 普查有明确结论**：两层均默认关闭（nop-job `enabled` 默认 false + in-job `contract-expiry-cron` 默认空）+ 全 20 生产 application.yaml 零 override（§2.2 + §2.3 表），每条有 file:line 证据。

---

## 4. 多维度审计（`docs/skills/multi-dimensional-audit-prompt.md`）

按多维审计提示要求，对每个维度至少给出一句裁决：

| 维度 | 裁决 |
|------|------|
| **需求正确性** | L1 UC-HR-07:80 要求「定时任务每日扫描」= cron 调度机制存在（已满足），未要求开箱默认启用。两层 config-gate 默认关闭不偏离 L1。无「承诺但没有证据」项。 |
| **owner-doc 对齐** | L2 `state-machine.md:274`（合同主生命周期 ACTIVE/EXPIRED 可用）与实现一致；`job-scheduling.md §3.15:229` 登记 stale `erp-hr-contract-expiry-reminder` DESIGN 条目与 live `erp-hr-contract-expiry` 已实现不一致（§6.2 修正）。 |
| **架构或边界影响** | 无新跨模块依赖 / API 契约变更 / 保护区域触碰。`.job.yaml` + bean 注册 + BizModel 是既有 nop-job 调度约定，无 DAG 边突破。 |
| **验证充分性** | 接线断裂假设可证伪：若 `.job.yaml` 不存在或反射链断，`testExecuteIsNoArgPublicMethod:152` + `testJobCronConfiguredTriggersBothSteps:142` 会失败（实测 7/7 绿）。config-gate 默认关闭可经 §2.3 site-level 普查证伪（零 override）。 |
| **回归风险** | 本验证零代码变更（只读评估 + 文档更新），无脆弱路径引入。两层 config-gate 默认关闭是稳定基线（P1-MA2-086 resolved R1.28 已记录）。 |
| **路由和技能选择正确性** | 任务路由 = verification or audit work（只读评估），Skill = `multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。匹配。换路由无遗漏。 |
| **待办或自主权策略漂移** | 范围未无声扩大；UC-HR-07 接受维持（非降级）；登记的 config-gate watch-only residual 是验证**输出**非范围内项目降级（plan Deferred But Adjudicated 正确分类）。 |
| **view.xml gen-control 契约**（项目特定维度） | 不适用——本验证对象是后端 cron 接线，不触及 delta view 前端层。本维度无发现。 |

**反窄化自检**：本验证覆盖 7 维度（需求/owner-doc/架构/验证充分性/回归风险/路由/待办漂移）+ 1 项目特定维度（view.xml，不适用），非单维深挖。每个维度已给出裁决。

---

## 5. Phase 2 — cron 接线活跃性裁决（§2 判据 + 三源对照）

### 5.1 裁决：维持 UC-HR-07 接受 + 登记 config-gate watch-only residual

**§2 判据复核**：

| §2 判据 | 是否成立 | 理由 |
|---------|----------|------|
| **§2 接受**（全部验收标准 L3-L5 各级均有证据且一致） | **✓ 成立** | ⑲cron 调度机制存在（`.job.yaml` 接线 + bean + `execute()` + L4 7 方法强测）；⑳㉑㉒㉓㉔ A1.12 §5 已证完整。L1 未要求「开箱默认启用」。 |
| §2 P0④ 会计过账正确性破坏 | ✗ 不适用 | 合同到期扫描不涉 GL 过账（`runExpirations` 仅 setStatus ACTIVE→EXPIRED，无凭证生成） |
| §2 P0① 活跃数据破坏 | ✗ 不成立 | config-gate 默认关闭 = 不触发，无默认活跃破坏路径；启用后行为经 A2.7a 证实幂等（status filter） |
| §2 P1① 功能完全缺失 | ✗ 不成立 | 功能存在且完整（两步执行 + 跨域通知 + 续签守卫），仅由两层 config-gate 条件启用 |
| §2 P1② 异常路径未实现 | ✗ 不成立 | 单条失败隔离（`:81-84`）+ 续签守卫（TERMINATED 拒绝）+ cron 空值跳过异常路径均已实现 |
| §2 P2① 次要验收标准未完全满足 | ✗ 不成立 | 主路径 + 异常路径均完整，非边界场景弱 |
| §2 P2③ 文档化简化 | ✗ 不成立 | config-gate opt-in 是部署启用决策，非文档化简化（机制完整实现，非简化） |

**取最高原则**：仅 §2 接受成立 → **维持 UC-HR-07 接受**。

**L1/L2/L3 三源对照**：
- L1（use-cases.md:80）「定时任务每日扫描」= cron 调度**机制存在**要求 ✓（已满足）
- L2（state-machine.md:274）合同 ACTIVE/EXPIRED 可用 ✓（`runExpirations` setStatus 实现）
- L3（ErpHrContractExpiryJob.execute() + .job.yaml + bean）接线完整 + 两层 config-gate 默认关闭 ✓

**与 A1.12 §5 UC-HR-07 接受裁决分层一致**：A1.12 §5 静态确认代码门控逻辑正确 + 单测覆盖两路径（接受）；本 MA4 验证补运行时活跃性（接线完整 + 部署 silent）→ 分层一致，均**接受**。两层 config-gate 默认关闭是**部署启用决策**（opt-in 范式），非契约缺失，对齐 A4.1.4 budget config 范式（A4.1.4 TL;DR `新 finding | 0 | 无新控制点（caveat ① 维持接受，不构成需求分歧）`）。

**登记 config-gate watch-only residual**：两层 config 门控默认关闭致合同到期自动化运行时非默认活跃。部署启用时须**同时设**：
- `nop.job.erp-hr-contract-expiry.enabled=true`（nop-job 层）
- `erp-hr.contract-expiry-cron` 非空（in-job 层，如 `0 0 1 * * ?` 每日 01:00）

属运维 config 决策，非代码修复（不触发 §5 ask-first，不归 MR1）。

### 5.2 finding 衔接裁决（§7 复用 or 新增）

| 既有 arm-index 行 | 控制点 | 本验证关系 | 裁决 |
|-------------------|--------|-----------|------|
| `P1-MA2-086`（定时任务并发重复副作用，resolved R1.28 done） | 并发幂等 | 描述内已登记「`erp-hr-contract-expiry` 全部默认 enabled=false（参考部署 silent）」 | **不新建**——config-gate 默认关闭事实已在该行描述记录；本验证为其补 site-level 普查证据（§2.3）。控制点不同（并发幂等 vs 部署激活），不追加 RC 交叉引用注记（避免模糊 P1-MA2-086 并发焦点） |
| A1.12 §5 UC-HR-07 接受（无独立 finding 行） | 需求契约 | 本验证维持该接受裁决 | **不新建**——接受类无 finding |

**新 finding 数 = 0**（对齐 A4.1.4 = 0 新 finding）。config-gate watch-only residual 记录于本报告 §0/§5.1 + plan `Deferred But Adjudicated` 段（非 arm-index finding 行，因 §2 裁决为接受非 P2，且 config-gate-default-off 事实已在 P1-MA2-086 描述记录，§7 去重不重复登记）。

### 5.3 不触发 MR0 / 不归 MR1

- **不触发 MR0**：无 P0（§2 P0①④均不成立）。
- **不归 MR1**：无 P1 finding（§2 P1①②均不成立）。config 默认值变更属部署配置决策（plan Non-Goals 明示不实施），非代码修复。

---

## 6. 文档更新（预授权）

### 6.1 本验证报告落盘

`docs/audits/2026-08-06-2247-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`（本文件）。

### 6.2 部署运维文档修正：`docs/architecture/job-scheduling.md §3.15:229`

**stale 漂移**：原条目登记 `erp-hr-contract-expiry-reminder`（每日扫描到期劳动合同提醒续签/终止）状态 **DESIGN（待实现）**，与 live 实现 `erp-hr-contract-expiry`（jobName 无 `-reminder` 后缀，状态 = 已实现 + config-gated 默认关闭）不一致。

**修正**（预授权文档更新，§9 真相源冻结条款——`job-scheduling.md` 属 `docs/architecture/` 技术设计非需求契约段落，可协同修订）：将 stale `erp-hr-contract-expiry-reminder` DESIGN 条目改为反映 live 实现的 `erp-hr-contract-expiry` 已实现条目 + 补两层 config 启用注记（运维启用决策）。

---

## 7. 与 arm-index / 既有审计去重声明（§去重协议）

- **MA1 ↔ MA2 去重**：本验证复用 A2.7a（`2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`）状态机行为 pass 结论（合同 expire 批量单失败隔离 + cron-gated 设计）+ A1.12 §3/§5 UC-HR-07 实现证据，不重新核实行为本身（§去重协议 1-2）。
- **MA4 ↔ A5.6 边界**：本验证审「行为是否符合需求」（cron 接线活跃性），不重做 A5.6 E2E 断言强度审计（§去重协议 MA4↔A5.6）。
- **arm-index 交叉去重**：本报告 0 新 finding（§5.2），全部经 grep arm-index 同域同控制点后裁决（P1-MA2-086 描述已记录 config-gate-default-off），无未经比对直接新建的 finding。
- **config-gate 范式对比**：本验证对齐 A4.1.4（budget config 默认关闭 → 维持接受 0 finding）+ A4.1.7（commitment release-on-return 默认关闭 → 维持接受 0 finding），方向相反于 A4.1.14（bank-recon auto-reverse config 默认 true 但零消费 → 维持既有 P1-RC-005）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。**本报告无生产代码变更**（纯审计报告 + `job-scheduling.md` 文档修正，零 Java/ORM/契约变更），checker 无回归风险。actual vs baseline 汇总表如下。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。

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

  本审计无生产代码变更，actual == baseline（全 16 可计数规则精确匹配），无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告 0 新 finding（§5.2），全部经 grep arm-index 同域同控制点后给出「复用 or 新增」裁决（P1-MA2-086 描述已记录 config-gate-default-off，§7 去重不重复登记），无未经比对直接新建的 finding。

---

## 9. 结论

A1.12 §7-1 静态存疑点（UC-HR-07 cron 运行时调度接线）经 nop-job `.job.yaml` 接线 census + 两层 config 门控默认值 census + 全 application.yaml 部署 override 普查 + BeanMethodJobInvoker 反射调用链核验 + 命名对账 + module-meta 完整性核验：

- **nop-job 接线完整**（`.job.yaml` 6 字段齐全 + bean 注册 + 反射目标 no-arg public `execute()` + BeanMethodJobInvoker 反射调用链完整可达）。
- **两层 config 门控叠加默认关闭**（nop-job `enabled` 默认 false + in-job `contract-expiry-cron` 默认空）→ 合同到期自动化运行时**非默认活跃**。
- **全 20 生产 application.yaml 零 override** → 生产环境默认值 = 代码默认值 = 双层关闭。
- **裁决**：**维持 A1.12 §5 UC-HR-07 接受**（§2 接受判据满足：cron-gated 机制存在，L1 要求「定时任务每日扫描」机制已满足，未要求开箱默认启用）。两层 config-gate 默认关闭是**部署启用决策**（opt-in 范式，对齐 A4.1.4 budget config），登记为 **watch-only residual**（部署启用时须同时设 `nop.job.erp-hr-contract-expiry.enabled=true` + `erp-hr.contract-expiry-cron` 非空）。
- **不触发 MR0，无新 finding，无 successor**。config 默认值变更属部署配置决策（plan Non-Goals）。

§7-1 存疑点**闭合**。
