# RC MA4 A4.1.7 — 承付 release-on-return config 默认 off 实际启用状态部署普查 验证报告

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 运行时行为验证）
> 工作项：A4.1.7（MA4 运行时行为验证 — A1.2 §7-4：承付 release-on-return（接入点 #4）config 默认 off 的实际启用状态部署普查）
> 验证 plan：`docs/plans/2026-08-07-0944-1-rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4 真相源层级与冲突裁决 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §去重协议）
> 输入存疑点：A1.2 §7 存疑点 4（`docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md:308`）+ §5.2 caveat ① 接受
> 关联 finding：`P1-MA2-082`（release-on-return 保守方向偏移，resolved R1.27；本普查为其补「config 默认值 deployment 启用状态」行为证据）+ `P1-RC-003`（UC-FIN-13 断言④ 报表三列）+ `P1-MA2-084`（控制引擎 actual 含 COMMITMENT，resolved R1.27）+ `P1-MA3-025`（预算余量公式 doc↔javadoc drift，resolved R2.4）
> 关联同型工作项：A4.1.4（`docs/audits/2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`，done — 总开关 `budget-check-enabled` / `budget-commitment-enabled` deployment 契约核对，本普查复用其总开关结论）
> 验证性质：**只读 deployment 普查**（grep config 消费点 + 部署工件 + 读既有 JUnit + 复用 A4.1.4/MA2；不改代码/ORM/api.xml/config 默认值/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 验证日期：2026-08-07
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **§7-4 存疑点裁决** | **维持接受**（A1.2 §5.2 caveat ① 接受确认；release-on-return 子开关归 P1-MA2-082 既有 successor） | 不升级 finding |
| config 消费点全集 | **1 处**（`ErpPurReturnProcessor.runCommitmentReleaseOnReturnHook:283` 单一消费点，默认 `Boolean.FALSE`）+ 1 常量声明 + 1 接线点 | 非死代码（config key 有唯一活跃消费点） |
| 双 config gate 依赖链 | **release-on-return 子开关依赖承付总开关**（`releaseIfPresent` → `isCommitmentEnabled:117-119` 默认 FALSE） | 子开关启用需两 config 同时 true |
| 部署侧 config 显式覆盖 | **零命中**（全 20 生产 application.yaml 无 `commitment-release-on-return` config key） | 生产环境默认值 = 代码默认值 = false |
| 真相源「开箱启用 release-on-return」声明 | **零命中**（product-scope / use-cases / budget.md §配置项 / module-meta.yaml / README / 部署运维文档全集） | 不存在隐含部署契约 |
| 建筑层 optionalFeatures 登记 | `commitment-release-on-return` **未单列**（是其父特性 `budget-commitment`[defaultValue:false] 的子开关，继承 opt-in 分类） | 子开关未在顶层 registry 单列，与父特性分类一致 |
| 测试 profile 显式启用 | `return-commitment-test.yaml:5` `commitment-release-on-return: true`（`TestErpPurReturnCommitmentRelease` 专用） | 佐证「启用后行为正确」非「开箱默认」 |
| 新 finding | **0** | 无新控制点（维持接受，归 P1-MA2-082 既有 successor） |
| MR0 触发 | **无** | — |

**整体裁决**：A1.2 §7 存疑点 4（「承付 release-on-return（接入点 #4）config 默认 off 的实际启用状态」）经 config 消费点全集普查 + deployment 工件全集普查 + 复用 A4.1.4 总开关结论 **CONFIRMED 维持接受**。config key `erp-fin.commitment-release-on-return` 全仓生产代码**唯一消费点** = `ErpPurReturnProcessor.runCommitmentReleaseOnReturnHook:281-297`（读 config :282-283 默认 `Boolean.FALSE` 短路 :284；启用后 `resolvePurchaseOrderCode:286-289` → `budgetCommitmentBiz.releaseIfPresent:291-292` 全额红冲 + NopException 容错 :293-296），经 `ErpPurReturnApproveProcessor.doApprove:60` 接线（退货审核后置）。该消费点**依赖承付总开关**（`releaseIfPresent` → `ErpFinBudgetCommitmentBizModel.isCommitmentEnabled:117-119` 默认 `Boolean.FALSE`）——**双 config gate**：release-on-return 启用需 `commitment-release-on-return=true` **且** `budget-commitment-enabled=true` 同时成立。deployment 工件全集（全 20 生产 application.yaml / seed / README / 部署运维文档）**零覆盖**「开箱启用 release-on-return」声明；owner doc `budget.md §配置项:313` 明确记载默认关闭理由（「release-on-return 总开关（接入点 #4；默认关。启用时退货审核全额红冲原 PO 承付，须同时开启 `budget-commitment-enabled`）」）；建筑层 `module-meta.yaml §optionalFeatures` 未单列该子开关（其父特性 `budget-commitment` 已登记为 `defaultValue:false`，子开关继承 opt-in 分类）。复用 A4.1.4 总开关结论：总开关 `budget-commitment-enabled` deployment 全集零「开箱即用」声明，故总开关默认 false 时 release-on-return 子开关启用与否均不生效。控制机制本身（config-gated `releaseIfPresent` 全额红冲 + 容错）经 A1.2 §2.4 + MA2 A2.16 + `TestErpPurReturnCommitmentRelease` 强断言已证实完整正确（非缺失），config-gate 仅条件启用——属 ERP 通用启用范式（与 A4.1.4 总开关裁决分层一致），**不构成需求分歧**。按 §2 判据，§2 P1①「功能完全缺失」/ P1②「异常路径未实现」**均不成立**（release-on-return 钩子存在 + 容错路径已实现，仅由双 config-gate 条件启用）。**不升级 finding，不触发 MR0。** config-default-false「保守方向偏移」（默认 off 致采购退货不释放承付，剩余未开票数量失去承付保护；全额释放语义致部分退货多释放）归 `P1-MA2-082` 既有 successor（resolved R1.27 钩子已落地；按比例部分释放 successor 触发条件：多组织预算硬约束启用 + 部分开票/部分退货为常态业务路径）。本验证不实施修复（§5 保护区域 + plan Non-Goals）。

---

## 1. 输入存疑点原文 + L1/L3 锚点

### 1.1 输入存疑点原文（A1.2 §7 存疑点 4，逐字引用）

> **承付 release-on-return（接入点 #4）config 默认 off 的实际启用状态**：L3 确认钩子已落地（`ErpPurReturnProcessor:281-297`）但 config 默认 false（`commitment-release-on-return` + 依赖 `budget-commitment-enabled`）。MA2 A2.16 `P1-MA2-082` 已登记（保守方向偏移：默认 off 致采购退货不释放承付，剩余未开票数量失去承付保护）。本切片不重复登记，交 MA4 按需确认部署是否启用——属既有 finding 行为证据，非新发现。
> — `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md:308`

### 1.2 A1.2 §5.2 caveat ① 既有接受结论（输入，本验证复用）

A1.2 §5.2 已对 config 默认关闭给出**接受**裁决（非 P1），三理由（本验证复核 + 补全 release-on-return 子开关 deployment 证据）：

1. 控制机制（release-on-return 钩子 = config-gated `releaseIfPresent` 全额红冲 + 容错）**完整且正确实现**，L1 要求的异常路径（容错 NopException 静默跳过不阻断退货审核流）**已实现非缺失**（§2 P1②「异常路径未实现」不成立）；
2. L1 未强制「默认开启」，config-gate 是 ERP 通用启用范式；
3. §2 P1①「功能完全缺失」不成立——功能存在，启用后行为与 L1 完全一致（A1.2 §3 + `TestErpPurReturnCommitmentRelease` 强断言证实）。

**残留观察**（本验证对象）：config 默认关闭意味着「开箱默认不启用 release-on-return」——若产品存在「开箱即用退货自动释放承付」的**隐含部署契约**，则属默认行为分歧。此点 L1 未显式裁决，A1.2 baseline 仅核 L1 use-cases 文本 + release-on-return 钩子落地，**未全量普查该子开关的 deployment 实际启用状态**——本验证补全该缺口。

### 1.3 L1 需求契约锚点（复核 A1.2 §1，逐字）

UC-FIN-13 预算管理（`docs/design/finance/use-cases.md:238`，承付款段）：
```
// 承付款
采购订单.APPROVED → 生成 COMMITMENT 凭证
订单 CANCELLED 或发票接收 → 红冲 COMMITMENT
```

> **关键观察（复核 A1.2 §5.2）**：L1 逐字未列举「采购退货 → 红冲 COMMITMENT」（接入点 #4 release-on-return 是 owner doc `budget.md §3 接入点 #4` 衍生扩展，非 L1 硬性验收标准）。L2（`budget.md §承付会计 §3 接入点 #4`）描述 release-on-return 作为衍生扩展（config-gated 默认 off + 全额释放语义 + 部分释放归 successor）。**本验证回答：是否存在「开箱启用 release-on-return」的隐含部署契约前提？**

### 1.4 L3 config-gate 消费点 + 依赖链实测锚点（复核 A1.2 §2.4）

| 锚点 | 文件:行（写时实测） | 实现原文 |
|---|---|---|
| config key 常量 | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinConstants.java:426` | `String CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN = "erp-fin.commitment-release-on-return";` |
| 唯一消费点 | `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnProcessor.java` `runCommitmentReleaseOnReturnHook:281-297` | :282-283 `AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN, Boolean.FALSE)` → :284 短路 `return`；启用后 :286-289 `resolvePurchaseOrderCode` → :291-292 `budgetCommitmentBiz.releaseIfPresent(PURCHASE_ORDER, poCode, context)` 全额红冲；:293-296 catch `NopException` 容错（无原承付凭证静默跳过） |
| 接线点（approve 后置） | `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/processor/ErpPurReturnApproveProcessor.java` `doApprove:60` | `processor.runCommitmentReleaseOnReturnHook(returnOrder, context);`（setApproveStatus / 红冲过账之后） |
| 依赖链（承付总开关 gate） | `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/budget/ErpFinBudgetCommitmentBizModel.java` `releaseIfPresent:104-115` + `isCommitmentEnabled:117-119` | :105 `if (!isCommitmentEnabled()) return null;`（总开关 gate）；:117-119 `return Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE));` |

> **双 config gate 依赖链**：release-on-return 启用需 `erp-fin.commitment-release-on-return=true` **且** `erp-fin.budget-commitment-enabled=true` 同时成立（子开关 gate 在 Processor :282-283，总开关 gate 在 BizModel :105/117-119）。

---

## 2. Phase 1 普查三清单（消费点全集 + deployment 工件 + 复用 A4.1.4 总开关结论）

### 2.1 清单 ① — config 消费点全集普查（rg 全仓生产代码，排除 test/target）

> 普查命令：`rg -n "CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN|commitment-release-on-return" --glob '!**/test/**' --glob '!**/target/**'`（生产代码）。

| # | 类型 | 站点 | 文件:行（写时实测） | 默认值 / 用途 | 备注 |
|---|---|---|---|---|---|
| 1 | **常量声明**（非消费） | config key 定义 | `ErpFinConstants.java:426` | `CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN = "erp-fin.commitment-release-on-return"` | 字符串常量定义，非消费 |
| 2 | **唯一消费点** | release-on-return 子开关 gate | `ErpPurReturnProcessor.java#runCommitmentReleaseOnReturnHook:283` | `AppConfig.var(..., Boolean.FALSE)` 默认 false 短路 :284 | **唯一** `AppConfig.var` 站点 |
| 3 | javadoc 引用（非消费） | 钩子方法 javadoc | `ErpPurReturnProcessor.java:273` | javadoc 描述「默认 false + 依赖承付总开关」 | 非消费（注释） |
| 4 | 接线点 | approve 后置调用 | `ErpPurReturnApproveProcessor.java#doApprove:60` | 调 `processor.runCommitmentReleaseOnReturnHook(...)` | 接线（非 config 读） |

**清单 ① 结论**：config key `erp-fin.commitment-release-on-return` 全仓生产代码**唯一活跃消费点 = 1 处**（`ErpPurReturnProcessor:283`）。消费点 < 1 **不是**异常（plan baseline 显式声明）；本普查实测 = 1（非死代码），与 plan baseline「rg 全仓生产代码 commitment-release-on-return config-gate 站点仅 1 处」**逐字吻合**。该消费点经双 config gate 串联（子开关 :282-283 + 总开关 `isCommitmentEnabled:117-119`），与 plan baseline + A1.2 §2.4 一致。**无遗漏消费点**（禁止抽样：全集 rg 仅 1 活跃消费点 + 1 常量声明 + 1 javadoc + 1 接线点）。

### 2.2 清单 ② — deployment 工件普查（生产 application.yaml / seed / README / 部署文档 / 测试 profile）

#### 2.2.1 生产 application.yaml 覆盖普查（全集 20 个 `*-app/src/main/resources/application.yaml`）

```
普查命令：for f in $(find . -path "*/src/main/resources/application*.yaml" | sort -u); do
            grep -q "commitment-release-on-return" "$f" && echo "OVERRIDE: $f"; done
普查结果：（无输出 → 零覆盖）
```

| 生产 application.yaml | commitment-release-on-return 覆盖 |
|---|---|
| 全 20 个 `module-*/erp-*-app/src/main/resources/application.yaml` + `app-erp-all/src/main/resources/application.yaml` | **零覆盖**（生产环境 config key 缺省 → 取代码默认值 `Boolean.FALSE`） |

> 实仓锚点复用 A4.1.4 §2.2.1：`app-erp-all/src/main/resources/application.yaml` 全文无任何 `erp-fin.*` budget config key。本普查独立复核全 20 生产 yaml 对 `commitment-release-on-return` 零命中。

#### 2.2.2 测试 profile 显式启用普查（佐证「启用后行为正确」非「开箱默认」）

| 测试 profile yaml（src/test/resources） | 显式 set 的 release-on-return config | 测试用途 |
|---|---|---|
| `module-purchase/erp-pur-service/src/test/resources/return-commitment-test.yaml:5` | `commitment-release-on-return: true`（+ `budget-commitment-enabled: true` :3 + `budget-commitment-subject-code: "1408"` :4 + `budget-check-enabled: false` :2） | `TestErpPurReturnCommitmentRelease`（`@NopTestConfig testConfigFile = "classpath:return-commitment-test.yaml"`）——release-on-return ON 场景：退货审核红冲原 PO COMMITMENT 凭证（场景1）+ 部分退货全额释放（场景2） |

**解读**：测试套件经独立 yaml profile **显式启用**双 config（子开关 + 总开关）后验证行为正确性（`TestErpPurReturnCommitmentRelease` 强断言：退货审核后 COMMITMENT 凭证红冲 + `reversalOfVoucherId` 指向原凭证 + 部分退货 qty5/qty100 全额释放语义），**佐证「启用后行为正确」**。OFF 默认回归（不调 release）由总开关默认 false + 子开关默认 false **双重门控**，既有 `TestErpPurReturnApproval`（默认 config）已间接覆盖（test javadoc :56-57 明示）。但**测试 profile ≠ 生产 profile**——生产 application.yaml 零覆盖，故生产默认值 = 代码默认值 = false。这正是 ERP config-gate opt-in 范式的标准实现：能力完整实现 + 默认关闭 + 部署方按需启用（与 A4.1.4 总开关 deployment 裁决分层一致）。

#### 2.2.3 seed/demo 数据普查

| seed/demo 载体 | 普查结果 |
|---|---|
| `module-notify/deploy/sql/{postgresql,oracle,mysql}/_seed_erp-notify.sql` 等 seed sql | **零** release-on-return config seed（grep `commitment-release-on-return` 跨 `*.sql/*.csv/*.json/*.yaml` 排除 test/target → 零命中；seed 数据是实体行数据非 config 覆盖载体） |

**解读**：seed 数据不携带 config 覆盖（config 经 application.yaml / 数据库 `nop_sys_config` 表，seed sql 仅实体行数据）。release-on-return 的实际启用状态由代码默认值 + application.yaml 覆盖决定（清单 ② §2.2.1 已证生产零覆盖）。

#### 2.2.4 README / 部署运维文档普查

| 文档 | release-on-return 声明 | 普查结果 |
|---|---|---|
| `README.md` | （无） | `README §财务` 域描述无「退货释放承付 / release-on-return / 开箱启用」声明 |
| `docs/deployment/` | （目录不存在） | 无部署文档 |
| `docs/operations/` | （目录不存在） | 无运维文档 |
| `docs/requirements/product-scope.md` | （无） | grep `commitment-release-on-return\|退货.*释放.*承付\|开箱.*承付\|开箱.*release-on-return` → 零命中 |
| `docs/design/finance/budget.md §配置项:313` | **明确记载默认关闭 + 理由**（owner doc 设计参考） | `erp-fin.commitment-release-on-return` 默认 false — 「release-on-return 总开关（接入点 #4；默认关。启用时退货审核全额红冲原 PO 承付，须同时开启 `budget-commitment-enabled`）」 |
| `docs/design/finance/budget.md §3 接入点 #4:258` | **config-gated 默认 OFF 明示**（owner doc 设计参考） | 「release-on-return \| `ErpPurReturn.approve` 后置（config-gated `erp-fin.commitment-release-on-return` 默认 OFF）」 |
| `docs/architecture/business-module-metadata.md §2.1` | optionalFeatures 设计意图 | 示例列 `budget-commitment` `defaultValue:false`（子开关 release-on-return 未单列，归父特性） |

#### 2.2.5 建筑层 optionalFeatures 登记（建筑层真相源）

| 建筑层载体 | release-on-return 登记 | 证据 |
|---|---|---|
| `module-finance/erp-fin-meta/module-meta.yaml §optionalFeatures` | **未单列 `commitment-release-on-return`** | line 16-55 列 13 个可选特性：`budget-check`（:17-19 defaultValue:false）/ `budget-commitment`（:20-22 defaultValue:false）/ `budget-carry-forward`（:23-25 false）/ `budget-roll-forward`（:26-28 false）/ `expense-budget-check`（:29-31 false）/ `exchange-revaluation`（:32-34 **true**）/ `bank-fx-revaluation`（:35-37 false）/ `notes-fx-gain-loss`（:38-40 false）/ `multi-schema`（:41-43 false）/ `annual-close`（:44-46 **true**）/ `bad-debt-allowance-gate`（:47-49 false）/ `auxiliary-recon-gate`（:50-52 false）/ `posting-exception-notify`（:53-55 **true**）——**无 `commitment-release-on-return` 条目** |

**解读**：`commitment-release-on-return` 是承付子系统（父特性 `budget-commitment`）的**接入点 #4 专用子开关**，未在顶层 `optionalFeatures` registry 单列。这与 A4.1.4 已确认的建筑层分类一致：`budget-commitment` 已登记为 `defaultValue:false` 可选特性（opt-in 组，与默认 true 的 `exchange-revaluation` / `annual-close` / `posting-exception-notify` 形成对照分类），release-on-return 子开关**继承父特性的 opt-in 分类**——即「承付子系统整体 opt-in，其下接入点 #4 子开关随父特性条件启用」。子开关未单列**不构成**「开箱启用 release-on-return」声明，反而是 opt-in 分类的下钻细化。

**清单 ② 结论**：**部署侧工件全集零「开箱启用 release-on-return」声明**。生产 application.yaml 零覆盖（取代码默认 false）；README / 部署 / 运维文档 / product-scope 无 release-on-return 开箱声明；owner doc `budget.md §配置项:313` + `§3 接入点 #4:258` 明确记载默认关闭 + 理由 + 双 config gate 依赖；建筑层 `module-meta.yaml` 父特性 `budget-commitment` 登记 `defaultValue:false`（子开关继承 opt-in 分类）；测试 profile `return-commitment-test.yaml` 显式启用仅佐证「启用后行为正确」非「开箱默认」。

### 2.3 清单 ③ — 复用 A4.1.4 总开关 deployment 结论（双 config gate 分层一致性）

> A4.1.4（done）已证总开关 `budget-check-enabled` + `budget-commitment-enabled` deployment 全集零「开箱即用预算控制」声明（`docs/audits/2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md` §2.1-§2.3 + §3 裁决）。本清单复用其总开关结论，补 release-on-return 子开关的分层依赖关系。

| 层级 | config key | 默认值 | A4.1.4 普查结论（复用） | 本普查（A4.1.7）补全 |
|---|---|---|---|---|
| 总开关（承付） | `erp-fin.budget-commitment-enabled` | false | A4.1.4 §2.3 站点 #2 `ErpFinBudgetCommitmentBizModel#isCommitmentEnabled:118` 默认 FALSE + 无生产覆盖 + 建筑层 `budget-commitment` defaultValue:false | release-on-return 子开关的**依赖前置**（子开关启用需总开关 true） |
| 总开关（预算控制） | `erp-fin.budget-check-enabled` | false | A4.1.4 §2.3 站点 #1 `ErpFinBudgetControlBiz#isBudgetCheckEnabled:226` 默认 FALSE + 无生产覆盖 + 建筑层 `budget-check` defaultValue:false | 正交（预算控制总开关，非 release-on-return 依赖链） |
| **子开关（接入点 #4）** | `erp-fin.commitment-release-on-return` | false | A4.1.4 §2.3 站点 #8 `ErpPurReturnProcessor#runCommitmentReleaseOnReturnHook:283` 默认 FALSE + 无生产覆盖（**A4.1.4 已列该站点但未单列该子开关的 deployment 裁决**） | **本普查补全**：消费点全集 = 1（§2.1）+ deployment 零覆盖（§2.2）+ 双 config gate 依赖链（§1.4） |

**分层一致性裁决**：
- A4.1.4 已证总开关 `budget-commitment-enabled` deployment 全集零「开箱即用」声明（product-scope / use-cases / owner doc / module-meta.yaml / README / 部署运维文档 / seed 数据全集）。
- release-on-return 子开关**依赖**总开关（双 config gate：子开关 :282-283 + 总开关 `isCommitmentEnabled:117-119`）——**逻辑蕴含**：总开关默认 false 时，子开关启用与否**均不生效**（`releaseIfPresent:105` 在总开关 gate 处短路 `return null`）。
- 故 A4.1.4 总开关 deployment 结论（零「开箱即用」声明 + opt-in 范式）**自动覆盖** release-on-return 子开关：不存在「开箱启用 release-on-return」隐含契约（否则与总开关 opt-in 范式自相矛盾）。本普查复核 release-on-return 子开关自身 deployment（§2.2）亦零覆盖，与总开关结论**分层一致**。

---

## 3. §7-4 存疑点裁决（Phase 1 Decision，方法论 §2 判据 + §去重协议）

### 3.1 判据对照

§2 升级 finding 的必要条件 = deployment 普查发现真相源 / 部署工件**显式声明「开箱启用 release-on-return」**且与 config 默认关闭**冲突**。本验证裁决路径：

| 裁决分支 | 触发条件 | 本验证普查结果 | 命中? |
|---|---|---|---|
| **升级 P1（§2 P1①功能实质偏离 / P1④跨组件契约不一致）** | 真相源 / 部署工件显式声明「开箱启用 release-on-return」且 config 默认关闭与之冲突 | **零命中**（§2.1 清单 ① 消费点全集 + §2.2 清单 ② deployment 工件全集：product-scope / use-cases / owner doc / module-meta.yaml / README / 部署运维文档 / seed 数据均无「开箱启用 release-on-return」声明；owner doc 反向明确记载默认关闭 + 双 config gate 依赖） | **❌ 不成立** |
| **维持接受（A1.2 §5.2 caveat ① 接受 + P1-MA2-082 既有 successor）** | 无任何「开箱启用」声明 → config 默认关闭属 ERP 通用启用范式，不构成需求分歧；config-default-false「保守方向偏移」归既有 finding successor | 三清单全集零命中 + 双 config gate 依赖链 + 复用 A4.1.4 总开关结论分层一致 + P1-MA2-082 已登记 successor | **✅ 成立** |

### 3.2 维持接受的依据（复核 + 补全 A1.2 §5.2 caveat ① + P1-MA2-082 successor）

| 依据 | 本验证补全核实 |
|---|---|
| ① release-on-return 钩子机制（config-gated `releaseIfPresent` 全额红冲 + NopException 容错）**完整且正确实现**，L1 要求的异常路径（容错静默跳过不阻断退货审核流）**已实现非缺失** | **CONFIRMED**（复用 A1.2 §2.4 + MA2 A2.16 + `TestErpPurReturnCommitmentRelease` 强断言：场景1 退货审核红冲原 PO COMMITMENT 凭证 + `reversalOfVoucherId` 指向原凭证；场景2 部分退货 qty5/qty100 全额释放语义；OFF 默认回归由双 config gate + `TestErpPurReturnApproval` 间接覆盖）。§2 P1②「异常路径未实现」**不成立**——容错路径存在且强测，仅由双 config-gate 条件启用 |
| ② L1 未强制「默认开启」，config-gate 是 ERP 通用启用范式 | **CONFIRMED + 补全**：L1（product-scope / use-cases UC-FIN-13 承付款段）逐字复核**未列举「采购退货 → 红冲 COMMITMENT」**（接入点 #4 是 owner doc 衍生扩展非 L1 硬性验收标准）；建筑层 `module-meta.yaml §optionalFeatures` 父特性 `budget-commitment` 登记 `defaultValue:false`（子开关继承 opt-in 分类）；owner doc `budget.md §配置项:313` + `§3 接入点 #4:258` 明确记载默认 OFF + 双 config gate 依赖 |
| ③ §2 P1①「功能完全缺失」不成立——功能存在，启用后行为与 L1 完全一致 | **CONFIRMED**：§2.1 清单 ① 唯一消费点（非死代码）+ A1.2 §3 + `TestErpPurReturnCommitmentRelease` 测试 profile 启用后强断言全集证实行为正确 |
| ④ config-default-false「保守方向偏移」归 `P1-MA2-082` 既有 successor | **CONFIRMED**：`P1-MA2-082`（arm-index:530，resolved R1.27 done）已登记 release-on-return 保守方向偏移（默认 off 致采购退货不释放承付 + 全额释放语义致部分退货多释放）；按比例部分释放 successor 触发条件 = 多组织预算硬约束启用 + 部分开票/部分退货为常态业务路径。本普查为该 finding 补「config 默认值 deployment 启用状态」行为证据（§4 交叉注记） |

### 3.3 裁决

**维持 A1.2 §5.2 caveat ① 接受 + `P1-MA2-082` 既有 successor 登记。不升级 finding。不触发 MR0。** config-default-false「保守方向偏移」（默认 off + 全额释放语义）归 `P1-MA2-082` successor（resolved R1.27 钩子已落地；按比例部分释放归 MR1 R1.0→RC-R1.n 修复行展开，触发条件未满足）。本验证不实施 config 默认值变更（plan Non-Goals）。

---

## 4. §去重声明（与 arm-index 交叉比对）

本验证**未产生新 finding**（§7-4 维持接受）。全部相关控制点归以下既有 finding（均**不同控制点 / 不同维度**，不可合并；唯 `P1-MA2-082` 同控制点 → 追加 RC 交叉注记补行为证据）：

| 既有 finding | 控制点 | 与本验证关系 |
|---|---|---|
| `P1-MA2-082`（resolved R1.27） | release-on-return 保守方向偏移（默认 off + 全额释放语义） | **同控制点**——本普查为该 finding 补「config 默认值 deployment 启用状态」行为证据。**追加 RC 交叉注记**（arm-index `P1-MA2-082` 行，§4.1 详述） |
| **A1.2 §5.2 caveat ①** | config 默认关闭 = 接受（非 finding，已登记接受） | **本验证对象**的接受前提（§7-4 是 caveat ① 的 release-on-return 子开关下钻）。普查确认无「开箱启用」部署契约 → 维持接受 |
| `P1-RC-003` | UC-FIN-13 断言④ 报表三列缺失 | **不同控制点**（报表列数 vs config 默认值 deployment 启用状态） |
| `P1-MA2-084`（resolved R1.27） | 控制引擎 actual 含 COMMITMENT 语义混淆 | **不同控制点**（控制引擎聚合语义 vs config 默认值 deployment 启用状态） |
| `P1-MA2-081`（resolved R1.27） | 部分开票全额释放语义（owner doc drift） | **不同控制点**（释放语义 owner doc drift vs config deployment；与 P1-MA2-082 同 successor「按比例部分释放」） |
| `P1-MA2-083`（resolved R1.27，Q4=(a) 下重开经 MR1） | AP/AR 发票冲销后 commitment 未恢复 | **不同控制点**（冲销恢复语义 vs config deployment） |
| `P1-MA3-025`（resolved R2.4） | 预算余量公式 doc↔javadoc drift | **不同维度**（MA3 doc↔javadoc 文本一致性 vs RC config deployment） |
| A4.1.4（done） | 总开关 `budget-check-enabled` / `budget-commitment-enabled` deployment 契约 | **同型工作项不同 config key**（总开关 deployment vs 接入点 #4 子开关 deployment）。本验证复用其总开关结论（§2.3 清单 ③） |

### 4.1 arm-index 交叉注记（同控制点 P1-MA2-082）

按方法论 §7「复用 or 新增」裁决规则：本普查与 `P1-MA2-082`（release-on-return 保守方向偏移）**同控制点**（release-on-return config-default-false 的 deployment 启用状态面），**复用既有 finding ID**，不新建 `P*-RC-xxx`。在 arm-index `P1-MA2-082` 行（`docs/audits/arm-index.md:530`）追加 RC 交叉注记：

> **【RC A4.1.7 普查 2026-08-07】** requirement-compliance MA4 视角：config 默认值 deployment 启用状态普查（`docs/audits/2026-08-07-0944-rc-ma4-a4-1-7-...md`）CONFIRMED release-on-return config `erp-fin.commitment-release-on-return` 全仓生产代码**唯一消费点** = `ErpPurReturnProcessor.runCommitmentReleaseOnReturnHook:283` 默认 `Boolean.FALSE`（双 config gate 依赖承付总开关 `isCommitmentEnabled:117-119`）+ 全 20 生产 application.yaml **零覆盖** + owner doc `budget.md §配置项:313` 明确记载默认 OFF + 建筑层父特性 `budget-commitment` defaultValue:false（子开关继承 opt-in 分类）。**deployment 实际启用状态 = 关闭**（生产默认值 = 代码默认值 = false）——为 P1-MA2-082「保守方向偏移」（默认 off 致采购退货不释放承付）补 deployment 行为证据。维持接受（A1.2 §5.2 caveat ① + P1-MA2-082 既有 successor），不升级 finding。详见 `docs/audits/2026-08-07-0944-rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census.md`。

**无未经比对直接新建的 finding。**（§7 裁决：本验证无新 finding 产出，唯同控制点 P1-MA2-082 追加 RC 交叉注记补行为证据，不新建编号）

---

## 5. §8 过程纪律自检（§8 模板）

### 5.1 checker actual vs baseline（纯 reporter；本计划无生产代码变更故无回归风险）

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码非门控依据；真正门控在 CI workflow `.github/workflows/compliance.yml`）：

| 规则 | actual（实测） | baseline（`compliance-baseline.md §BASELINE`） | 判定 |
|---|---|---|---|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | = |
| R1d | 14 | 14 | = |
| R2a | 34 | 34 | = |
| R2b | 229 | 229 | = |
| R2c | 1382 | 1382 | = |
| R2d | 34 | 34 | = |
| R3+ | （checker R3 段既有早退行为，未打印计数） | 5/0/0/2/0/0/6/0/69/66/40 | 结构上不变（见下） |

**说明**：
1. **本验证零生产代码变更**（只读普查：grep config 消费点 + 部署工件 + 读既有 JUnit + 复用 A4.1.4/MA2/A1.2），故对 R1-R12 计数**结构上零贡献**，无回归可能。
2. **checker R3 段既有早退**与本验证**无关**——是 checker 脚本的既有行为（A4.1.4 §5.1 同一观察：checker 在 R3 段早退不打印后续计数）。本验证不触及任何 `.java` 生产文件，R3-R12c 计数结构上不变。
3. **门控结论**：本验证无回归风险（零生产代码变更），checker 仅作过程记录，不作通过/失败门控。CI workflow 的 actual vs baseline 比对不受本验证影响（结构上不变）。
4. **不以 checker 脚本退出码作为门控通过依据**（§8：checker 是纯 reporter，退出码非门控；真正门控在 CI workflow）。

### 5.2 closure-audit 独立性声明

本验证报告由主代理（执行者）起草。**结束审计将由独立子代理（新会话，不重用执行者上下文）执行**（plan §Closure Gates），执行者未自我审计，未将结束审计留为 `[ ]` 人工门控占位符。

### 5.3 与 arm-index 交叉去重声明

见 §4。§7-4 维持接受**无新 finding 产出**；同控制点 `P1-MA2-082` 追加 RC 交叉注记补 deployment 行为证据（§4.1）；其余相关既有 finding（`P1-RC-003` / `P1-MA2-081` / `P1-MA2-084` / `P1-MA2-083` / `P1-MA3-025`）均**不同控制点 / 不同维度**，无未经比对直接新建的 finding。

---

## 6. 验证范围与非目标（plan §Non-Goals 一致）

- **本验证只读**：grep config 消费点（生产代码 `AppConfig.var(...CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN...)`）+ 部署工件（application.yaml / README / seed / 部署运维文档 / module-meta.yaml）+ 读既有 JUnit（`TestErpPurReturnCommitmentRelease`）+ 复用 A4.1.4/MA2/A1.2。未改任何 `.java` / `.xml` / `.orm.xml` / config 默认值 / 真相源。
- **不重新核实 release-on-return 钩子机制本身**（A1.2 §2.4 + MA2 A2.16 已证钩子落地 + 全额释放语义 + 部分释放 successor；本验证只核实 deployment 实际启用状态）。
- **不重新核实总开关 deployment 契约**（A4.1.4 已闭合；本验证复用其结论，只补子开关 + 双 config gate 依赖链）。
- **不实施修复**（config 默认值变更 / 按比例部分释放实现属代码逻辑修复预授权类目，但本验证裁决为接受故无修复；config-default-false「保守方向偏移」归 `P1-MA2-082` 既有 successor，修复经 MR1 R1.0→RC-R1.n）。
- **不修改真相源**（§9 冻结）。
- **不展开其他 A1.2 §7 存疑点**（§7-1 已由 A4.1.4 闭合；§7-2/3 由 A4.1.5/A4.1.6 闭合；本验证仅 §7-4）。

## 7. MR0 触发登记

**无**。Phase 1 裁决为维持接受（§3.3），不触发 MR0 即时通道（方法论 §10）。本验证不实施修复，config-default-false「保守方向偏移」归 `P1-MA2-082` 既有 successor（按比例部分释放经 MR1，触发条件未满足）。

## 8. 结论

A1.2 §7 存疑点 4（「承付 release-on-return（接入点 #4）config 默认 off 的实际启用状态」）经 config 消费点全集普查 + deployment 工件全集普查 + 复用 A4.1.4 总开关结论 **CONFIRMED 维持接受**。config key `erp-fin.commitment-release-on-return` 全仓生产代码**唯一活跃消费点 = 1 处**（`ErpPurReturnProcessor.runCommitmentReleaseOnReturnHook:283` 默认 `Boolean.FALSE`，双 config gate 依赖承付总开关 `isCommitmentEnabled:117-119`）；deployment 工件全集（全 20 生产 application.yaml / seed / README / 部署运维文档 / product-scope）**零覆盖**「开箱启用 release-on-return」声明；owner doc `budget.md §配置项:313` + `§3 接入点 #4:258` 明确记载默认 OFF + 双 config gate 依赖；建筑层 `module-meta.yaml §optionalFeatures` 父特性 `budget-commitment` 登记 `defaultValue:false`（子开关 release-on-return 继承 opt-in 分类，未单列）；复用 A4.1.4 总开关 deployment 结论分层一致（总开关默认 false 时子开关启用与否均不生效）。**不存在「开箱启用 release-on-return」隐含部署契约，config 默认关闭属 ERP 通用启用范式，不构成需求分歧。** config-default-false「保守方向偏移」（默认 off 致采购退货不释放承付 + 全额释放语义致部分退货多释放）归 `P1-MA2-082` 既有 successor（resolved R1.27 钩子已落地；本普查为其补 deployment 行为证据，§4.1 arm-index 交叉注记）。A1.2 §5.2 caveat ① 接受依据经本验证补全核实全部 CONFIRMED。**维持接受，不升级 finding，不触发 MR0，无新 finding，无新 successor（归既有 `P1-MA2-082` successor）。** 本验证解除 A1.2 §7 存疑点 4 在 MA4（A4.1 展开器）链路的 deployment 实际启用状态证据缺口。
