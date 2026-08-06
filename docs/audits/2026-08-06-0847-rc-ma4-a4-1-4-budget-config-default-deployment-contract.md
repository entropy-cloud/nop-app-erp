# RC MA4 A4.1.4 — config 默认关闭 vs「开箱即用预算硬拦截」部署契约核对 验证报告

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 运行时行为验证）
> 工作项：A4.1.4（MA4 运行时行为验证 — A1.2 §7-1：预算控制/承付 config 默认关闭 vs「开箱即用预算硬拦截」部署契约核对）
> 验证 plan：`docs/plans/2026-08-06-0847-1-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4 真相源层级与冲突裁决 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §去重协议）
> 输入存疑点：A1.2 §7 存疑点 1（`docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md:305`）+ §5.2 caveat ① 接受
> 关联 finding：`P1-RC-003`（UC-FIN-13 断言④ 报表三列）+ `P1-MA2-082`（release-on-return 保守偏移，resolved R1.27）+ `P1-MA2-084`（控制引擎 actual 含 COMMITMENT，resolved R1.27）+ `P1-MA3-037`（expense-budget-check doc→code 默认值 drift，resolved R2.5）+ `P1-MA3-025`（预算余量公式 doc↔javadoc drift，resolved R2.4）
> 验证性质：**只读部署契约核对**（grep 真相源/部署工件/config 默认值 + 读既有 JUnit + 复用 MA2/A1.2；不改代码/ORM/api.xml/config 默认值/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 验证日期：2026-08-06
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **caveat ① 残留观察裁决** | **维持接受**（A1.2 §5.2 caveat ① 接受确认） | 不升级 finding |
| 真相源「开箱即用预算控制」声明 | **零命中**（product-scope / use-cases / 各域 owner doc / 部署文档全集） | 不存在隐含部署契约 |
| 部署侧 config 显式覆盖 | **零命中**（全 20 生产 application.yaml 无 budget config key） | 生产环境默认值 = 代码默认值 = false |
| 建筑层 optionalFeatures 登记 | **明确登记为可选特性 defaultValue:false**（`module-finance/erp-fin-meta/module-meta.yaml` + `business-module-metadata.md §2.1`） | 官方声明：opt-in 范式 |
| 新 finding | **0** | 无新控制点（caveat ① 维持接受，不构成需求分歧） |
| MR0 触发 | **无** | — |

**整体裁决**：A1.2 §5.2 caveat ① 残留观察（「config 默认关闭是否与『开箱即用预算硬拦截』部署契约冲突」）经部署契约真相源 + config 默认值全集核对**CONFIRMED 维持接受**。全仓真相源（`docs/requirements/product-scope.md` / `docs/design/finance/use-cases.md` UC-FIN-11/13 / 各域 owner doc `§配置项` / `§部署`）+ 部署侧工件（全 20 个生产 `application.yaml` / README / seed/demo / 部署运维文档）**零命中**「开箱即用 / 默认开启 / 默认启用预算控制」部署契约声明；建筑层 `module-meta.yaml §optionalFeatures` 反向**显式登记** `budget-check` / `budget-commitment` 为 `defaultValue:false` 的可选特性（与 `exchange-revaluation` / `annual-close` / `posting-exception-notify` 默认 true 的特性形成对照），且 owner doc `budget.md §配置项` 明确记载默认关闭理由（"保护既有 113 purchase 测试不触发承付凭证" / "向后兼容"）。10 个 config-gate 站点（2 finance 总开关 + 8 processor 消费点）逐一复核**全部** `AppConfig.var(..., Boolean.FALSE)` 默认关闭，且**无任何**生产 application.yaml 覆盖。控制机制本身（HARD 拦截 / WARN 日志 / NONE 放行 / 三通道余量 / 承付 commit+release）经 A1.2 §5.2 已证实完整正确（非缺失），config-gate 仅条件启用——属 ERP 通用启用范式（行业参照：`roles-and-permissions.md:172`「已定义≠默认开启」是行业常态），**不构成需求分歧**。按 §2 判据，§2 P1①「功能完全缺失」/ P1②「异常路径未实现」**均不成立**（功能存在 + 异常路径 HARD 抛异常已实现，仅由 config-gate 条件启用），故不升级 finding。**不触发 MR0，无新 finding，维持 A1.2 §5.2 caveat ① 接受。** 本验证不实施修复（config 默认值变更若裁决为分歧则经 MR1，但本验证裁决为接受故无 successor）。

---

## 1. 输入存疑点原文 + L1/L3 锚点

### 1.1 输入存疑点原文（A1.2 §7 存疑点 1，逐字引用）

> **caveat ① config 默认关闭是否与"开箱即用预算硬拦截"部署契约冲突**：L3 静态确认 `isBudgetCheckEnabled`/`isCommitmentEnabled` 默认 false（控制机制完整正确，仅 config-gate 条件启用）。L1 未显式声明"默认开启"。若产品存在"开箱即用预算控制"隐含契约，则属默认行为分歧——交 MA4 A4.1 运行时确认（核对 product-scope / 部署文档是否声明默认预算控制启用；无则维持接受）。当前无 P1 上证据。
> — `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md:305`

### 1.2 A1.2 §5.2 caveat ① 既有接受结论（输入，本验证复核）

A1.2 §5.2 已对 caveat ① 给出**接受**裁决（非 P1），三理由：
1. 控制机制（HARD 拦截异常路径 + WARN 日志 + NONE 放行 + 三通道余量公式 + 承付 commit/release）**完整且正确实现**，L1 要求的异常路径（HARD 抛异常）**已实现非缺失**（§2 P1②「异常路径未实现」不成立）；
2. L1 未强制"默认开启"，config-gate 是 ERP 通用启用范式（行业参照 iDempiere 亦 gate 预算控制）；
3. §2 P1①「功能完全缺失」不成立——功能存在，启用后行为与 L1 完全一致。

**残留观察**（本验证对象）：config 默认关闭意味着"开箱默认不启用预算控制/承付"——若产品存在"开箱即用预算硬拦截"的**隐含部署契约**，则属默认行为分歧。此点 L1 未显式裁决，A1.2 baseline 仅核 L1 use-cases 文本，**未全量普查部署侧文档/配置**——本验证补全该缺口。

### 1.3 L1 需求契约锚点（复核 A1.2 §1，逐字）

UC-FIN-11 预算硬拦截（`docs/design/finance/use-cases.md:204`）：
```
场景:采购订单审核时超预算,被硬拦截。
可验证断言(见 budget.md §业务规则):
  采购订单.审核 → 调用 IErpFinBudgetControlBiz.check(科目, 成本中心, 期间, 金额, 来源单)
  预算余量 = 预算(BUDGET凭证) - 承付(COMMITMENT凭证) - 实际(NORMAL凭证)
  若 余量 < 0 且 控制级别 == HARD: 返回 BLOCKED → 审核抛异常, 订单保持 SUBMITTED
  若 == WARN: 写日志放行
  若 == NONE: 放行
```

UC-FIN-13 预算管理（`docs/design/finance/use-cases.md:238`，预算控制段）：
```
// 预算控制(采购订单审核时,强一致校验)
采购订单.审核 →
  IErpFinBudgetControlBiz.check(科目, 成本中心, 期间, 金额, 来源单)
  预算余量 = BUDGET凭证 - COMMITMENT凭证 - NORMAL凭证(同维度)
  若 余量 < 0 且 控制级别==HARD: 返回 BLOCKED → 审核抛异常
  若 == WARN: 写 BudgetControlLog 放行
```

> **关键观察（复核 A1.2 §5.2）**：L1 描述控制**语义**（HARD/WARN/NONE 三通道 + 余量公式 + 承付 commit/release 触发条件），**未声明"必须默认开启"**。UC 文本以"采购订单.审核 → 调用 check"描述控制机制，控制级别由命中的 `BudgetScenario.controlLevel` 决定。**本验证回答：是否存在「开箱即用预算硬拦截」的隐含部署契约前提？**

### 1.4 L3 config-gate 总开关实测锚点（复核 A1.2 §2.1/§2.3）

| 总开关方法 | 文件:行（写时实测） | 实现原文 |
|---|---|---|
| 预算控制总开关 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetControlBiz.java` `isBudgetCheckEnabled:225-228` | `Boolean enabled = AppConfig.var(ErpFinConstants.CONFIG_BUDGET_CHECK_ENABLED, Boolean.FALSE); return Boolean.TRUE.equals(enabled);` |
| 承付总开关 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetCommitmentBizModel.java` `isCommitmentEnabled:117-119` | `return Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE));` |
| config key 常量 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinConstants.java` | `CONFIG_BUDGET_CHECK_ENABLED = "erp-fin.budget-check-enabled":379` / `CONFIG_BUDGET_COMMITMENT_ENABLED = "erp-fin.budget-commitment-enabled":419` / `CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN = "erp-fin.commitment-release-on-return":426` |

---

## 2. Phase 1 三清单（部署契约真相源 + 部署工件 + config 默认值全集核对）

### 2.1 清单 ① — 真相源声明普查（grep 关键词：「开箱 / 默认开启 / 默认启用 / 默认启用预算 / 开箱即用 / out-of-the-box / budget-check / budget-commitment / 预算控制默认」）

| 真相源（按 §4 层级） | 文件 | 普查结果 | 证据（逐字原文 / 零命中声明） |
|---|---|---|---|
| **L1 顶层范围** | `docs/requirements/product-scope.md` | **零命中**「预算/承付/开箱/默认开启」 | `grep -n "预算\|承付\|budget\|commitment\|开箱\|默认开启\|默认启用" product-scope.md`：仅 line 9/11/19 命中「核心业务域」「财务」域描述（"会计凭证、科目、业财打通、核销、期末结账、成本核算"），**无预算控制/承付/开箱/默认启用任何声明** |
| **L2 功能契约** | `docs/design/finance/use-cases.md` UC-FIN-11/13 | **未声明"必须默认开启"** | UC 文本以"采购订单.审核 → 调用 check"描述控制**机制**，控制级别由命中的 `BudgetScenario.controlLevel` 决定（见 §1.3 逐字引用） |
| **L3 设计参考（owner doc）** | `docs/design/finance/budget.md §配置项` | **明确记载默认关闭 + 理由**（设计参考，非真相源，与 L1 一致未强制默认开启） | line 310: `erp-fin.budget-commitment-enabled` 默认 false — "总开关（默认关，保护既有 113 purchase 测试不触发承付凭证）"；line 313: `erp-fin.commitment-release-on-return` 默认 false — "release-on-return 总开关（接入点 #4；默认关。启用时退货审核全额红冲原 PO 承付，须同时开启 `budget-commitment-enabled`）"；line 183: `erp-fin.budget-roll-forward-enabled` 默认 false — "总开关（默认关，渐进启用）"；line 233: `erp-fin.budget-carry-forward-enabled` 默认 false — "总开关" |
| **建筑层真相源** | `module-finance/erp-fin-meta/module-meta.yaml` `§optionalFeatures` | **明确登记为可选特性 defaultValue:false** | line 16-22: `- feature: budget-check` / `configKey: erp-fin.budget-check-enabled` / `defaultValue: false`；`- feature: budget-commitment` / `configKey: erp-fin.budget-commitment-enabled` / `defaultValue: false`（对照同文件 line 32-34 `exchange-revaluation defaultValue:true` / line 44-46 `annual-close defaultValue:true` / line 53-55 `posting-exception-notify defaultValue:true`——budget 系列在「opt-in」组 vs 这三项在「default-on」组，**显式分类**） |
| **建筑层方法论** | `docs/architecture/business-module-metadata.md §2.1` | **optionalFeatures 设计意图明示** | line 42: 示例 `[{"feature":"budget-commitment","configKey":"erp-fin.budget-commitment-enabled","defaultValue":false}]`；line 47: "config-gated 特性（如 finance 的 `budget-commitment-enabled` 等）散落在各域 `Erp*Configs.java` / `Erp*Constants.java`，无统一清单查询入口；`optionalFeatures` 用极低成本补齐该缺口" |
| **行业参照** | `docs/design/roles-and-permissions.md:172` | **"已定义≠默认开启"是行业常态** | "Axelor 等 ERP 的 portal/模块权限也是『权限定义随模块安装生效、默认非全开』" |
| **L1 顶层范围（部署/运维）** | `README.md` + `docs/deployment/` + `docs/operations/` | **零命中**（`docs/deployment/` / `docs/operations/` 目录不存在；README `§财务` 仅列"过账引擎 / AR/AP 辅助账 / 核销 / 票据 / 费用报销 / 期末结账"，**无预算控制/承付/开箱启用声明**） | README:39 财务域描述无预算控制；README `快速开始` §68-85 仅编译/运行命令，无 config 覆盖示例 |

**清单 ① 结论**：**全集真相源零「开箱即用预算控制」声明**。L1（product-scope / use-cases）未强制默认开启；L2 owner doc 明确记载默认关闭 + 理由（保护既有测试 + 向后兼容 + 渐进启用）；建筑层 `module-meta.yaml` 显式登记为可选特性 `defaultValue:false`，并与默认 true 的特性（exchange-revaluation / annual-close / posting-exception-notify）形成对照分类——**budget 控制系列在建筑层即分类为「opt-in」而非「default-on」**。不存在「开箱即用预算硬拦截」隐含部署契约。

### 2.2 清单 ② — 部署侧工件普查（application.yaml / seed / demo / README / 部署文档 / 测试启用 config）

#### 2.2.1 生产 application.yaml 覆盖普查（全集 20 个 `*-app/src/main/resources/application.yaml`）

```
普查命令：for f in $(find . -path "*/src/main/resources/application*.yaml"); do
            grep -q "budget-check\|budget-commitment\|commitment-release" "$f" && echo "OVERRIDE: $f"; done
普查结果：（无输出 → 零覆盖）
```

| 生产 application.yaml | budget config key 覆盖 |
|---|---|
| 全 20 个 `module-*/erp-*-app/src/main/resources/application.yaml` + `app-erp-all/src/main/resources/application.yaml` | **零覆盖**（生产环境 config key 缺省 → 取代码默认值 `Boolean.FALSE`） |

**实仓锚点**（`app-erp-all/src/main/resources/application.yaml` 全文 52 行）：仅含 `nop.debug / auth.jwt / web.render-mode=flux / orm.init-database-schema / datasource=h2 / graphql / quarkus.http` 配置，**无任何 `erp-fin.budget-*` config key**。

#### 2.2.2 测试启用 config 普查（佐证"启用后行为正确"但不等同"开箱默认"）

| 测试 profile yaml（src/test/resources） | 显式 set 的 budget config | 测试用途 |
|---|---|---|
| `module-finance/erp-fin-service/src/test/resources/budget-test.yaml:7` | `budget-check-enabled: true` | `TestErpFinBudgetEndToEnd`（UC-FIN-11 三级控制强断言） |
| `module-finance/erp-fin-service/src/test/resources/budget-a2-test.yaml:7-8` | `budget-check-enabled: false` / `budget-commitment-enabled: true` | A2 承付 E2E（隔离 budget-check 单独启 commitment） |
| `module-purchase/erp-pur-service/src/test/resources/budget-control-test.yaml:2` | `budget-check-enabled: true` | `TestErpPurBudgetControlIntegration`（跨域预算控制集成） |
| `module-purchase/erp-pur-service/src/test/resources/budget-commitment-test.yaml:2-3` | `budget-check-enabled: false` / `budget-commitment-enabled: true` | `TestErpPurOrderCommitment`（承付 commit 凭证） |
| `module-purchase/erp-pur-service/src/test/resources/return-commitment-test.yaml:2-5` | `budget-check-enabled: false` / `budget-commitment-enabled: true` / `commitment-release-on-return: true` | `TestErpPurReturnCommitmentRelease`（release-on-return ON/OFF 双场景） |
| `module-sales/erp-sal-service/src/test/resources/budget-commitment-sales-test.yaml:2-3` | `budget-check-enabled: false` / `budget-commitment-enabled: true` | `TestErpSalOrderCommitment`（sales 承付） |

**解读**：测试套件经独立 yaml profile **显式启用** config 后验证行为正确性（A1.2 §3 强断言全集：HARD 抛异常 / WARN 日志 / NONE 放行 / 三通道余量 / 承付 commit+release），**佐证"启用后行为正确"**。但**测试 profile ≠ 生产 profile**——生产 application.yaml 零覆盖，故生产默认值 = 代码默认值 = false。这正是 ERP config-gate opt-in 范式的标准实现：能力完整实现 + 默认关闭 + 部署方按需启用。

#### 2.2.3 seed/demo 数据普查

| seed/demo 载体 | BudgetScenario.controlLevel 默认分布 | 普查结果 |
|---|---|---|
| `module-notify/deploy/sql/{postgresql,oracle,mysql}/_seed_erp-notify.sql` | （notify 域无关） | **零 ErpFinBudgetScenario seed 数据**（grep `ErpFinBudgetScenario\|BudgetScenario\|controlLevel` 跨 `*.sql/*.csv/*.json/*.yaml` 排除 test/target → 仅命中 `_dump/` 前端页面元数据，**无 seed 行数据**） |

**解读**：无 seed BudgetScenario 数据 → 新部署的 ERP 实例默认无预算方案 → 即使 config 开启也无 `controlLevel` 命中 → `findMatchingBudgetLine` 返回空（`ErpFinBudgetControlBiz:180-197`）→ check 不拦截。这与 config 默认关闭一致地构成"开箱默认不启用预算控制"双保险，进一步反证不存在"开箱即用预算硬拦截"契约。

#### 2.2.4 README / 部署运维文档普查

| 文档 | 预算控制声明 | 普查结果 |
|---|---|---|
| `README.md` | （无） | `README §18 业务域` 财务域描述（line 39）仅列"过账引擎 / AR/AP / 核销 / 票据 / 报销 / 期末结账"，**无预算控制**；`README §快速开始` 仅编译/运行命令；`README §验证状态` 仅 mvn 命令——**零预算控制部署契约声明** |
| `docs/deployment/` | （目录不存在） | 无部署文档 |
| `docs/operations/` | （目录不存在） | 无运维文档 |
| `docs/architecture/` | （无开箱启用声明） | `business-module-metadata.md §2.1` 反向支撑：budget 系列为 optionalFeatures `defaultValue:false`（§2.1 清单 ① 已列） |

**清单 ② 结论**：**部署侧工件全集零「开箱启用预算控制」声明**。生产 application.yaml 零覆盖（取代码默认 false）；README / 部署 / 运维文档无预算控制开箱声明；seed 数据无 BudgetScenario 默认方案；测试 profile 显式启用仅佐证"启用后行为正确"非"开箱默认"。建筑层 `module-meta.yaml` 反向**显式登记** budget 系列为可选特性 `defaultValue:false`。

### 2.3 清单 ③ — config 默认值逐消费点复核（全集 10 站点 = 2 finance 总开关 + 8 processor 消费点）

> 普查方法：`grep -n "AppConfig.var(.*CONFIG_BUDGET"` 跨 `module-finance` + `module-purchase` + `module-sales` main 路径。每站点记录默认值 + 是否被 application.yaml 覆盖（§2.2.1 已证生产零覆盖）。

| # | 站点 | 文件:行（写时实测） | config key | 默认值 | 生产覆盖 | 备注 |
|---|---|---|---|---|---|---|
| **Finance 总开关（2）** | | | | | | |
| 1 | 预算控制总开关 | `ErpFinBudgetControlBiz.java#isBudgetCheckEnabled:226` | `erp-fin.budget-check-enabled` | `Boolean.FALSE` | 无 | 控制 `check()` 入口短路（:64 `if (!isBudgetCheckEnabled()) return PASS` 等价放行） |
| 2 | 承付总开关 | `ErpFinBudgetCommitmentBizModel.java#isCommitmentEnabled:118` | `erp-fin.budget-commitment-enabled` | `Boolean.FALSE` | 无 | 控制 `commit/release/releaseIfPresent` 三方法入口短路（:57/:82/:105） |
| **Processor 消费点（8）** | | | | | | |
| 3 | PO 预算 check 钩子 | `ErpPurOrderProcessor.java#runBudgetCheckHook:178` | `erp-fin.budget-check-enabled` | `Boolean.FALSE` | 无 | PO approve 后置 |
| 4 | PO 承付 commit 钩子 | `ErpPurOrderProcessor.java#runCommitmentCommitHook:198` | `erp-fin.budget-commitment-enabled` | `Boolean.FALSE` | 无 | PO approve 后置 |
| 5 | PO 承付 release-on-cancel 钩子 | `ErpPurOrderProcessor.java#runCommitmentReleaseHook:223` | `erp-fin.budget-commitment-enabled` | `Boolean.FALSE` | 无 | PO reverseApprove / cancel |
| 6 | AP 发票承付 release-on-invoice-approve 钩子 | `ErpPurInvoiceProcessor.java#runCommitmentReleaseOnInvoiceApproveHook:274` | `erp-fin.budget-commitment-enabled` | `Boolean.FALSE` | 无 | AP 发票过账释放 |
| 7 | 付款预算 check 钩子 | `ErpPurPaymentProcessor.java#runBudgetCheckHook:168` | `erp-fin.budget-check-enabled` | `Boolean.FALSE` | 无 | 付款审核 |
| 8 | 退货承付 release-on-return 钩子 | `ErpPurReturnProcessor.java#runCommitmentReleaseOnReturnHook:283` | `erp-fin.commitment-release-on-return` | `Boolean.FALSE` | 无 | 退货审核（依赖承付总开关，双 gate） |
| 9 | SO 承付 commit 钩子 | `ErpSalOrderProcessor.java#runCommitmentCommitHook:312` | `erp-fin.budget-commitment-enabled` | `Boolean.FALSE` | 无 | SO approve 后置（sales 承付） |
| 10 | SO 承付 release 钩子 | `ErpSalOrderProcessor.java#runCommitmentReleaseHook:333` | `erp-fin.budget-commitment-enabled` | `Boolean.FALSE` | 无 | SO reverseApprove / cancel |
| 11 | AR 发票承付 release 钩子 | `ErpSalInvoiceProcessor.java#runCommitmentReleaseOnInvoiceApproveHook:322` | `erp-fin.budget-commitment-enabled` | `Boolean.FALSE` | 无 | AR 发票过账释放 |

> **站点计数说明**：plan baseline 称"2 finance 总开关 + 8 processor 消费点 = 10 站点"。实仓复核细化 = **2 finance 总开关 + 9 processor 消费点 = 11 站点**（baseline 的 "8" 为起草时口径，实际 processor 消费点 = PO 3 + AP invoice 1 + Payment 1 + Return 1 + SO 2 + AR invoice 1 = 9）。**+1 不影响裁决**（全部默认 false + 全部无生产覆盖的结论不变）。本表按实仓 11 站点全集列出，纠正 plan baseline 计数。

**清单 ③ 结论**：**全 11 config-gate 站点（2 finance 总开关 + 9 processor 消费点）默认 `Boolean.FALSE`，无任何生产 application.yaml 覆盖**。生产环境 config 默认值 = 代码默认值 = false。控制机制本身（HARD/WARN/NONE 三通道 + 三通道余量 + 承付 commit/release）经 A1.2 §5.2 已证实完整正确（非缺失），config-gate 仅条件启用——启用后行为与 L1 完全一致（A1.2 §3 强断言全集证实）。

---

## 3. caveat ① 残留观察裁决（Phase 1 Decision，方法论 §2 判据）

### 3.1 判据对照

§2 升级 finding 的必要条件 = 普查发现真相源 / 部署工件**显式声明"开箱即用预算硬拦截"**且 config 默认关闭与之**冲突**。本验证裁决路径：

| 裁决分支 | 触发条件 | 本验证普查结果 | 命中? |
|---|---|---|---|
| **升级 P1（§2 P1①功能实质偏离 / P1②可用性契约未充分实现）** | 真相源 / 部署工件显式声明"开箱即用预算硬拦截"且 config 默认关闭与之冲突 | **零命中**（§2.1 清单 ① + §2.2 清单 ② 全集普查：product-scope / use-cases / owner doc / module-meta.yaml / README / 部署运维文档 / seed 数据均无"开箱启用预算控制"声明；建筑层反向显式登记为 `defaultValue:false` 可选特性） | **❌ 不成立** |
| **维持接受（A1.2 §5.2 caveat ① 接受）** | 无任何"开箱启用"声明 → config 默认关闭属 ERP 通用启用范式，不构成需求分歧 | 三清单全集零命中 + 建筑层反向登记 + 行业参照支撑 | **✅ 成立** |

### 3.2 维持接受的三理由（复核 + 补全 A1.2 §5.2）

| A1.2 §5.2 理由 | 本验证补全核实 |
|---|---|
| ① 控制机制（HARD 抛异常 + WARN 日志 + NONE 放行 + 三通道余量 + 承付 commit/release）**完整且正确实现**，L1 要求的异常路径（HARD 抛异常）**已实现非缺失** | **CONFIRMED**（复用 A1.2 §2.1/§2.3 + §3 测试强断言全集：`testHardControlBlocked` HARD 抛 `ERR_BUDGET_EXCEEDED` / `testWarnControlLogsAndPasses` WARN 日志 / `testNoneControlPasses` NONE 放行 / `testAvailableDeductsCommitmentSeparately` available=500 三通道分离 / `TestErpFinBudgetCommitment` 6 用例承付 commit/release 强断言）。§2 P1②「异常路径未实现」**不成立**——异常路径存在且强测，仅由 config-gate 条件启用 |
| ② L1 未强制"默认开启"，config-gate 是 ERP 通用启用范式 | **CONFIRMED + 补全**：L1（product-scope / use-cases UC-FIN-11/13）逐字复核**未声明"必须默认开启"**；建筑层 `module-meta.yaml §optionalFeatures` **显式登记** budget-check / budget-commitment 为 `defaultValue:false` 可选特性（与默认 true 的 exchange-revaluation / annual-close / posting-exception-notify 形成对照分类）；行业参照 `roles-and-permissions.md:172`「已定义≠默认开启」是行业常态 |
| ③ §2 P1①「功能完全缺失」不成立——功能存在，启用后行为与 L1 完全一致 | **CONFIRMED**：§2.3 清单 ③ 全 11 站点复核功能存在（非缺失）+ A1.2 §3 测试 profile 启用后强断言全集证实行为正确 |

### 3.3 裁决

**维持 A1.2 §5.2 caveat ① 接受。不升级 finding。不触发 MR0。无 successor（本验证不实施 config 默认值变更；config 默认值变更仅当裁决为分歧时才经 MR1，本验证裁决为接受故无 successor）。**

---

## 4. §去重声明（与 arm-index 交叉比对）

本验证**未产生新 finding**（caveat ① 维持接受）。全部相关控制点归以下既有 finding（均**不同控制点**，不可合并）：

| 既有 finding | 控制点 | 与本验证关系 |
|---|---|---|
| **A1.2 §5.2 caveat ①** | config 默认关闭 = 接受（非 finding，已登记接受） | **本验证对象**。普查确认无"开箱启用"部署契约 → 维持接受 |
| `P1-RC-003` | UC-FIN-13 断言④ 报表三列缺失 | **不同控制点**（报表列数 vs config 默认值部署契约） |
| `P1-MA2-082`（resolved R1.27） | release-on-return 保守偏移 | **不同控制点**（释放语义 vs config 默认值部署契约）。本验证 §2.3 站点 #8 复核 release-on-return 钩子已落地 config 默认 false（双 gate：承付总开关 + release-on-return 总开关） |
| `P1-MA2-084`（resolved R1.27） | 控制引擎 actual 含 COMMITMENT 语义混淆 | **不同控制点**（控制引擎聚合语义 vs config 默认值部署契约） |
| `P1-MA3-037`（resolved R2.5） | expense-budget-check doc→code 默认值 drift（doc true → code false） | **不同维度**（MA3 doc↔code 文本一致性 vs RC 需求契约部署契约）。本验证复核 `expense-budget-check-enabled` 已落 module-meta.yaml `defaultValue:false`（§2.1 清单 ① line 29-31），doc drift 已 resolved |
| `P1-MA3-025`（resolved R2.4） | 预算余量公式 doc 三项式 vs javadoc 二项式 drift | **不同维度**（MA3 doc↔javadoc 文本一致性 vs RC 部署契约） |

**无未经比对直接新建的 finding。**（§7 "复用 or 新增"裁决：本验证无 finding 产出，故无新建 / 复用裁决动作；维持接受本身不产生 arm-index 行）

---

## 5. §8 过程纪律自检（§8 模板）

### 5.1 checker actual vs baseline（纯 reporter；本计划无生产代码变更故无回归风险）

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码非门控依据；真正门控在 CI workflow `.github/workflows/compliance.yml`）：

| 规则 | actual（实测） | baseline（`compliance-baseline.md §BASELINE`） | 判定 |
|---|---|---|---|
| R1a (dao().saveEntity BizModel) | 0 | 0 | = |
| R1b (dao().updateEntity BizModel) | 0 | 0 | = |
| R1c (dao().getEntityById BizModel) | 0 | 0 | = |
| R1d (dao().findAllByQuery BizModel) | 14 | 14 | = |
| R2a (BizModel daoFor ErpMd*) | 34 | 34 | = |
| R2b (BizModel daoFor Erp* 跨域) | 229 | 240 | ≤（改善） |
| R2c (全生产 daoFor 总量) | （checker R3 段退出未打印计数，既有行为，与本验证无关） | 1380 | 结构上不变（见下） |
| R2d (Processor daoFor ErpMd*) | 34 | 32 | +2（见下） |
| R3+ | （checker R3 段既有退出行为） | — | 结构上不变（见下） |

**说明**：
1. **本验证零生产代码变更**（只读核对：grep 真相源/部署工件/config 默认值 + 读既有 JUnit + 复用 MA2/A1.2），故对 R1-R12 计数**结构上零贡献**，无回归可能。
2. **R2d +2 / R2c 未打印**与本验证**无关**——反映的是**本计划执行前既有的仓状态**（由前序已审计的深化计划引入的合法 daoFor 增量，`compliance-baseline.md` 的「下降注记」未同步追平，与 A4.1.2 报告 §5.1 同一观察）。本验证不触及任何 `.java` 生产文件。
3. **门控结论**：本验证无回归风险（零生产代码变更），checker 仅作过程记录，不作通过/失败门控。CI workflow 的 actual vs baseline 比对不受本验证影响（结构上不变）。

### 5.2 closure-audit 独立性声明

本验证报告由主代理（执行者）起草。**结束审计将由独立子代理（新会话，不重用执行者上下文）执行**（plan §Closure Gates），执行者未自我审计，未将结束审计留为 `[ ]` 人工门控占位符。

### 5.3 与 arm-index 交叉去重声明

见 §4。caveat ① 维持接受**无新 finding 产出**；全部相关既有 finding（`P1-RC-003` / `P1-MA2-082` / `P1-MA2-084` / `P1-MA3-037` / `P1-MA3-025`）均**不同控制点 / 不同维度**，无未经比对直接新建的 finding。

---

## 6. 验证范围与非目标（plan §Non-Goals 一致）

- **本验证只读**：grep 真相源（product-scope / use-cases / owner doc / module-meta.yaml / business-module-metadata.md）+ 部署工件（application.yaml / README / seed / 部署运维文档）+ 读既有 JUnit + 复用 MA2/A1.2。未改任何 `.java` / `.xml` / `.orm.xml` / config 默认值 / 真相源。
- **不重新核实控制机制本身**（A1.2 §2.1/§5.2 已证实控制机制完整正确；本验证只核实"默认开启"部署契约前提）。
- **不实施修复**（config 默认值变更属代码逻辑修复预授权类目，但本验证裁决为接受故无修复；若裁决升级为分歧则经 MR1 R1.0→RC-R1.n）。
- **不修改真相源**（§9 冻结）。
- **不重新评级既有 finding**（P1-RC-003 / P1-MA2-082 / P1-MA2-084 / P1-MA3-037 / P1-MA3-025 均为不同控制点，本验证不重审其评级）。

## 7. MR0 触发登记

**无**。Phase 1 裁决为维持接受（§3.3），不触发 MR0 即时通道（方法论 §10）。本验证不实施修复，无 successor。

## 8. 结论

A1.2 §7 存疑点 1（「config 默认关闭是否与『开箱即用预算硬拦截』部署契约冲突」）经部署契约真相源 + 部署工件 + config 默认值全集核对**CONFIRMED 维持接受**。全集真相源（product-scope / use-cases / owner doc / module-meta.yaml / business-module-metadata.md / README / 部署运维文档 / seed 数据）**零命中**「开箱即用预算控制」部署契约声明；建筑层 `module-finance/erp-fin-meta/module-meta.yaml §optionalFeatures` 反向**显式登记** budget-check / budget-commitment 为 `defaultValue:false` 可选特性（与默认 true 的 exchange-revaluation / annual-close / posting-exception-notify 形成对照分类）；全 11 config-gate 站点（2 finance 总开关 + 9 processor 消费点）默认 `Boolean.FALSE` 且无任何生产 application.yaml 覆盖。**不存在「开箱即用预算硬拦截」隐含部署契约，config 默认关闭属 ERP 通用启用范式，不构成需求分歧。** A1.2 §5.2 caveat ① 三接受理由经本验证补全核实全部 CONFIRMED。**维持接受，不升级 finding，不触发 MR0，无 successor，无新 finding。** 本验证解除 A1.2 §7 存疑点 1 在 MA4（A4.1 展开器）链路的部署契约证据缺口。
