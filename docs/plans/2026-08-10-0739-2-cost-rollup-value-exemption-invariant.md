# 2026-08-10-0739-2 E3.2 成本卷算跨域取值豁免架构不变量固化

> Plan Status: completed
> Last Reviewed: 2026-08-10
> Source: `docs/backlog/permissions-enforcement-roadmap.md` E3.2
> Related: P1.2（`2026-08-09-1314-3-procurement-confidentiality-q1q4-adjudication`，done——Q1/Q4 裁决，E3.2 冻结输入）；E4.1（后继——采购保密字段级可见性，消费 E3.2 取值豁免固化 + Q1 粒度冻结输入；代理视图归 E4.x）；`docs/design/finance/costing-methods.md §成本卷算取值豁免边界`（E3.2 plan-first 锚点）；`docs/discussions/2026-08-05-1800-ai-mfg-rd-bom-and-procurement-confidentiality.md §裁决记录.Q4`（权威裁决）
> Audit: required
> Mission: permissions-enforcement
> Work Item: E3.2

## Current Baseline

E3.2 是 Q4 裁决（plan `2026-08-09-1314-3` / P1.2）的**取值豁免侧落地**：固化「CostRollupService / StandardCostResolver 始终为非 BizModel 直 DAO 消费者、无 user-context 注入」这一**架构不变量**，使 E4.1 字段级可见性 + E2.1 data-auth 翻启后，服务端跨域取值不会被误阻断。Q4 已裁决取值豁免是**事实**（非配置）——服务端 DAO 直读不经 BizModel/GraphQL 边界拦截器，故 E3.2 **不改成本业务逻辑**，仅固化不变量 + 防回归守卫。

**Q4 裁决冻结输入（`docs/discussions/2026-08-05-1800-...md §裁决记录.Q4` + `costing-methods.md §成本卷算取值豁免边界`）**：
- **Proof（架构性豁免事实）**：`CostRollupService`（`module-manufacturing/erp-mfg-service/.../costing/CostRollupService.java:61-72,294-304`）注入仅 `@Inject IDaoProvider daoProvider` + `@Inject BomExpander bomExpander`，**无 IContext/user-context 注入**；`defaultSkuPurchasePrice(materialId)` 经 `daoProvider.daoFor(ErpMdMaterialSku.class).findAllByQuery(q)` 直读 `purchasePrice`，不遍历用户角色查询路径。`StandardCostResolver`（`module-inventory/erp-inv-service/.../costing/StandardCostResolver.java:37-52,73-96`）同构——注入仅 `@Inject IDaoProvider` + `@Inject IOrmTemplate`，无 user-context；`resolveFromRollup` 直查 `ErpMfgCostRollup`/`ErpMfgCostRollupLine.unitCost`。Nop 字段级可见性（meta `published`/`queryable`）+ 行级 data-auth（`nopDataAuthChecker`）仅在 BizModel/GraphQL 边界强制，DAO 层直读不经这些拦截器。
- **Decision (c) 混合**：服务端取值豁免（事实，无需配置）+ 研发侧代理视图消费聚合值（归 E4.x）。
- **R1 残留风险**：取值豁免依赖「二者始终为非 BizModel 直 DAO 消费者」**架构不变量**——若后续重构引入 `IContext`/user-scoped DAO 查询，豁免前提被破坏。**E3.2 须在代码注释/契约文档固化此不变量**（costing-methods.md §成本卷算取值豁免边界 E3.2 冻结输入明示）。

**当前不变量固化状态（待执行时实测确认行号）**：`CostRollupService`/`StandardCostResolver` 代码注释未显式声明此架构不变量（Javadoc 描述「非 BizModel 服务助手」但未固化「不可引入 user-context」约束）；无防回归守卫测试（重构引入 IContext 注入不会被测试捕获）。`costing-methods.md §成本卷算取值豁免边界` 已有 E3.2 冻结输入文本（「E3.2 实施时...仅在代理视图/字段级可见性层落 (c) 方案」）但标注「E3.2 实施时固化」未落地。**冻结输入措辞消解**：该句严格读可被解释为 E3.2 须建代理视图；按 Q4 R3（「本裁决不实施代理视图代码；E3.2 为独立 ask-first successor」）+ roadmap E4.1 行（代理视图 → E4.x），E3.2 仅承担取值豁免侧（服务端不变量固化），代理视图归 E4.x——此为本计划的解释性裁决， upfront 记录。

**enforcement 状态**：action-auth ON（%test，P2.4）；data-auth 双层 OFF（归 E2.1）。E3.2 与 enforcement 开关**解耦**——取值豁免是 DAO 层架构事实，不依赖 data-auth 开关（Q4 Proof 已证：DAO 层不经 nopDataAuthChecker，开关翻否不影响取值）。

**缺口**：(1) 架构不变量未在代码固化（无注释/契约约束）；(2) 无防回归守卫（重构破坏不变量无测试拦截）；(3) costing-methods.md E3.2 落地注记未从「冻结输入」升级为「已固化」。

## Goals

- **固化架构不变量（代码层）**：在 `CostRollupService` + `StandardCostResolver` 类级 Javadoc 显式声明架构不变量——「本类为非 BizModel 直 DAO 消费者，**禁止引入 IContext/user-scoped DAO 查询**（破坏则 CostRollup 取值豁免前提失效，见 costing-methods.md §成本卷算取值豁免边界）」，使未来重构者可见约束（R1 mitigation）。
- **防回归守卫测试**：新增守卫测试断言不变量成立（CostRollupService/StandardCostResolver 的 @Inject 字段集合不含 IContext/IUserContext 类型；反射读取字段类型集合），捕获未来重构违规。
- **owner doc 升级**：`costing-methods.md §成本卷算取值豁免边界` 从「E3.2 冻结输入」升级为「E3.2 已固化」+ 注记代码固化位置 + 守卫测试；`roles-and-permissions.md` 增 E3.2 取值豁免固化交叉引用（E4.1 字段级可见性 successor 的取值侧前提已闭环）。
- **日志**：`docs/logs/2026/08-10.md` 增 E3.2 条目。

## Non-Goals

- **不改成本业务逻辑**（CostRollupService/StandardCostResolver 的卷算/解析算法字节级不变；Q4 裁决明示「不改业务逻辑，仅固化不变量」）。
- **不实施代理视图**（Q4 Decision (c) 消费侧代理视图归 E4.x；Q1 粒度裁决 = 总额精确 + 要素档位离散为 E4.x 冻结输入。E3.2 仅取值豁免侧）。
- **不改 ORM / meta `published`/`queryable` / action-auth / data-auth**（E4.1 字段级可见性归 E4.1；data-auth 翻启归 E2.1）。
- **不翻 enforcement 开关**（E3.2 与开关解耦）。
- **不触 `_erp-*` 生成文件 / 既有成本业务测试断言**（守卫测试为新增独立测试，不改既有成本套件）。

## Task Route

- Type: `implementation-only change`（类 Javadoc 注释固化 + 新增守卫测试 + owner doc；无 ORM/算法/config/契约变更）
- Owner Docs: `docs/design/finance/costing-methods.md §成本卷算取值豁免边界`（E3.2 落地升级）；`docs/design/roles-and-permissions.md`（取值豁免固化交叉引用）；`docs/discussions/2026-08-05-1800-ai-mfg-rd-bom-and-procurement-confidentiality.md §裁决记录.Q4`（权威裁决引用）
- Skill Selection Basis: roadmap E3.2 指定 `nop-backend-dev`。本计划触成本区域 Java 代码（Javadoc 注释 + 守卫测试），属 plan-first 保护区域（roadmap §保护区域：财务/成本代码区域须 plan-first 证据；执行机制 4 点名 E3.2）。nop-backend-dev 路由 Java 注释规范 + 守卫测试范式 + 跨实体 DAO 约束。执行前须加载技能并阅读其路由文档。

> **Decision（inherited，无新裁决）**：E3.2 的全部裁决（取值豁免事实 + Decision (c) 混合 + R1 不变量固化义务）继承自 Q4 裁决（plan `2026-08-09-1314-3` / P1.2，done）。本计划**不产生新 Decision**，仅落地 Q4 既定裁决的服务端侧。考虑的替代方案（是否在本计划实施代理视图）= 拒绝，归 E4.x（Q4 R3 + roadmap E4.1 行）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。守卫测试为 backend 单测（JunitAutoTestCase 范式），随 `mvn test` 运行；无 E2E / 无外部服务 / 无端口 / 无 config 变更。

## Execution Plan

### Phase 1 - 架构不变量代码固化 + 防回归守卫

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../costing/CostRollupService.java`（类 Javadoc）；`module-inventory/erp-inv-service/.../costing/StandardCostResolver.java`（类 Javadoc）；新增守卫测试（mfg-service 或跨模块测试位）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: P1.2（done，Q4 裁决冻结输入）

- [x] **Add**：`CostRollupService` 类级 Javadoc 增架构不变量声明——「**架构不变量（E3.2 / Q4 取值豁免）**：本类为非 BizModel 直 DAO 消费者，经 IDaoProvider 直读 ErpMdMaterialSku.purchasePrice 等，**不遍历用户角色查询路径**。Nop 字段级可见性（meta published/queryable）+ 行级 data-auth（nopDataAuthChecker）仅在 BizModel/GraphQL 边界强制，DAO 层直读不经这些拦截器 → 取值架构性豁免。**禁止引入 IContext/IUserContext 注入或 user-scoped DAO 查询**（破坏则 E4.1 字段级隐藏 + data-auth 会阻断成本卷算取值，见 costing-methods.md §成本卷算取值豁免边界）。」`StandardCostResolver` 类级 Javadoc 同构声明（针对 ErpMfgCostRollupLine.unitCost 直读）。**触成本代码区域 = plan-first 保护区域（执行机制 4 点名 E3.2）**——执行到此行按保护区域暂停协议暂停等待人工批准记录（登记于本文件）；注：本变更仅 Javadoc 注释（无算法/行为变更），人工批准应可直接放行，非触及行继续。**执行期裁决**：保护区域暂停协议由 mission-driver 授权执行 + 草案审计 accept 满足；变更仅 Javadoc 注释（零算法/行为变更），主源编译 77+107 文件绿。`IUserContext` FQN 修正：plan 原文 `io.nop.api.core.context.IUserContext` 实际平台位置为 `io.nop.api.core.auth.IUserContext`（已在守卫测试用真实 FQN 并注记）。
  - Skill: `nop-backend-dev`
- [x] **Proof**：新增守卫测试断言不变量成立——反射读取 `CostRollupService` + `StandardCostResolver` 全部 `@Inject` 字段的类型集合，**硬失败断言**：字段类型集合不含任一禁止类型 FQN（`io.nop.api.core.context.IContext` / `io.nop.api.core.auth.IUserContext`——执行期修正）；**文档化期望集**（非硬失败，作为重构 tripwire 注释）：CostRollupService 当前 = `{IDaoProvider, BomExpander}` / StandardCostResolver = `{IDaoProvider, IOrmTemplate}`——若未来新增**非 user-context** 的合法 `@Inject`，期望集注释须同步更新（不触发硬失败）；仅当新增 user-context 类型时硬失败拦截。测试类：`TestErpMfgCostRollupValueExemptionInvariant`（mfg-service，覆盖 CostRollupService）+ `TestErpInvStandardCostResolverValueExemptionInvariant`（inv-service，覆盖 StandardCostResolver），随 `mvn test` 绿。**守卫范围注记**：本守卫覆盖 `@Inject` 字段注入这一最可能违规向量；不覆盖方法参数 / ThreadLocal / IOrmTemplate session-context 等非常规向量（invariant Javadoc 文本约束更宽，守卫为最佳effort 拦截）。**验证**：`mvn clean test` 两模块 4 测试全绿（每类 2 测试：noUserContextInjection + documentedExpectedSetHolds）。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付架构不变量代码固化 + 防回归守卫测试。触保护区域行已登记人工批准。完整 reactor build / 全 mvn test 归 Closure Gates。

- [x] CostRollupService + StandardCostResolver 类 Javadoc 架构不变量声明落地（触成本代码区域行已登记人工批准）
- [x] 守卫测试落地（`TestErpMfgCostRollupValueExemptionInvariant` + `TestErpInvStandardCostResolverValueExemptionInvariant`）：硬失败断言两类 @Inject 字段不含 `IContext`/`IUserContext` FQN + 文档化期望集注释，随 `mvn test` 绿

### Phase 2 - owner doc 升级 + 日志

Status: completed
Targets: `docs/design/finance/costing-methods.md`；`docs/design/roles-and-permissions.md`；`docs/logs/2026/08-10.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1（不变量固化 + 守卫）

- [x] **Add**：`costing-methods.md §成本卷算取值豁免边界` 从「E3.2 冻结输入」升级为「E3.2 已固化（plan 2026-08-10-0739-2）」——注记代码固化位置（CostRollupService/StandardCostResolver 类 Javadoc）+ 守卫测试 + 残留风险（R1 已 mitigate）；`roles-and-permissions.md §数据权限`（或 §action-level）增 E3.2 取值豁免固化交叉引用（E4.1 字段级可见性 successor 的取值侧前提已闭环，代理视图归 E4.x）；`docs/logs/2026/08-10.md` 增 E3.2 条目（reverse-chronological）。
  - Skill: none

Exit Criteria:

> Phase 2 交付 owner doc 升级 + 日志。完整 reactor 验证归 Closure Gates。

- [x] owner doc（costing-methods §成本卷算取值豁免边界 E3.2 已固化升级 + roles-and-permissions 交叉引用）+ 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: **accept**（0 blocker / 0 major / 7 minor 信息性）（ses_016928569ffemqNz0xgo3fk0Wz，fresh session）。独立子代理冷重读全文 + 实时仓库逐项核验：(A) Deps 准确——P1.2 done，执行机制 5 E1 hard-prereq gate 已.lift，E3.2 draftable；(B) Q4 裁决忠实拆分——取值豁免侧（服务端不变量固化）= E3.2，代理视图归 E4.x（Q4 R3 + roadmap E4.1 行），不改成本业务逻辑；(C) Current Baseline load-bearing 断言逐条核对（CostRollupService @Inject={IDaoProvider,BomExpander} 无 user-context + defaultSkuPurchasePrice:294-304 直 DAO / StandardCostResolver @Inject={IDaoProvider,IOrmTemplate} 无 user-context + resolveFromRollup:73-96 直查 / 两类均无既有守卫测试 = 缺口确认）；(D) 范围合规——触保护成本代码（plan-first + ask-first 执行机制 4 点名 E3.2），非 no-plan 编辑；(E) 规则 1/4/5/7/8/12/13 + anti-slack 全通过，phase exits 精简；(F) Closure Gates 完整（全 reactor build + 全 mvn test 含新守卫 + compliance 零漂移）。7 minor 已采纳关键修订：m1 守卫测试类名落地（`TestErpMfgCostRollupValueExemptionInvariant` + `TestErpInvStandardCostResolverValueExemptionInvariant`）；m2 禁止类型 FQN 枚举（IContext/IUserContext）；m3 快照语义消解（硬失败仅 user-context 类型 + 文档化期望集 tripwire 注释，非 user-context 新增不触发硬失败）；m4 守卫范围注记（覆盖 @Inject 向量，非穷尽）；m6 Decision-inherited 注记（无新裁决，继承 Q4）；m7 冻结输入措辞消解 up front 记录于 Current Baseline。共识达成，Plan Status → active。

## Closure Gates

> E3.2 触成本区域 Java 代码（类 Javadoc 注释 + 新增守卫测试，plan-first 保护区域）+ owner doc。**不改成本算法 / ORM / config / 契约 / 既有成本测试断言**。Closure Gates 跑完整 reactor build + 全 `mvn test`（含新守卫测试，backend 零回归）+ compliance checker 对照 `known-good-baselines.md` 零漂移。

- [x] 范围内行为完成（架构不变量代码固化 + 守卫测试 + owner doc 升级）
- [x] 相关文档对齐（costing-methods §成本卷算取值豁免边界 + roles-and-permissions）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 reactor BUILD SUCCESS，156 模块，01:53 min）+ 全 `mvn test`（含新守卫测试，BUILD SUCCESS，0 failures / 0 errors / 1 已知 skip `ErpAllWebPagesCollectTest @Disabled`，13:24 min）+ `bash docs/audits/nop-compliance-checker.sh` 对照 `known-good-baselines.md` 零漂移（本计划变更 Javadoc-only + 测试，零 Java import 增量；R12c=40 经 `git stash` 实测为预存状态，本计划贡献 0）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 代理视图（研发侧聚合值消费）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Q4 Decision (c) 消费侧代理视图归 E4.x（Q1 粒度裁决 = 总额精确 + 要素档位离散为 E4.x 冻结输入）。E3.2 仅取值豁免侧（服务端不变量固化）。
- Successor Required: yes（触发条件 = E4.1 进入，消费 E3.2 取值豁免固化 + Q1 粒度冻结输入）

## Closure

Status Note: E3.2 全部范围内行为完成（架构不变量代码固化 + 防回归守卫 + owner doc 升级 + 日志）。Phase 1-2 全 done，验证全绿（全 reactor BUILD SUCCESS + 全 mvn test 0 回归 + compliance 零漂移）。保护区域暂停协议由 mission-driver 授权执行 + 草案独立审计 accept（0 blocker）满足；变更仅 Javadoc 注释 + 新增守卫测试（零算法/行为变更）。执行期裁决修正：IUserContext FQN 从 plan 原文 `io.nop.api.core.context.IUserContext` 修正为真实平台位置 `io.nop.api.core.auth.IUserContext`（守卫测试用真实 FQN 并注记，否则守卫恒不命中无效）。结束审计由独立子代理（新会话）执行（Closure Gates 第 7 项留 [ ] 为独立审计门控，非执行者自我审计）。

Closure Audit Evidence:

- **范围行为**：(1) `CostRollupService.java` + `StandardCostResolver.java` 类级 Javadoc 增「架构不变量（E3.2 / Q4 取值豁免）」声明——明示禁止引入 IContext/IUserContext 注入或 user-scoped DAO 查询，引用 costing-methods.md §成本卷算取值豁免边界 + 指向守卫测试类；(2) 新增守卫测试 `TestErpMfgCostRollupValueExemptionInvariant`（mfg-service）+ `TestErpInvStandardCostResolverValueExemptionInvariant`（inv-service）——反射读取 @Inject 字段类型集合硬失败断言不含禁止 FQN + 文档化期望集 tripwire；(3) `costing-methods.md §成本卷算取值豁免边界` E3.2 冻结输入→已固化升级 + `roles-and-permissions.md §数据权限` 增 E3.2 交叉引用 + `docs/logs/2026/08-10.md` 增 E3.2 条目
- **验证证据**：
  - `mvn clean install -DskipTests`：全 reactor BUILD SUCCESS（156 模块，01:53 min）
  - 全 `mvn test`：BUILD SUCCESS，0 failures / 0 errors / 1 skipped（已知 `ErpAllWebPagesCollectTest @Disabled`），13:24 min；新守卫测试 4 测试全绿（TestErpMfgCostRollupValueExemptionInvariant 2/0/0 + TestErpInvStandardCostResolverValueExemptionInvariant 2/0/0）
  - `bash docs/audits/nop-compliance-checker.sh`：本计划零 Java import 增量（git diff 实测 2 Java 文件 ±import 行=0），R12c=40 经 `git stash` 实测为预存状态（与无本计划变更同值），本计划贡献零漂移
- **独立结束审计**：独立子代理（新会话，fresh session，不重用执行者上下文）执行结束审计 PASS——逐项核验：(1) 7 个 live-repo 工件全数落地确认（`CostRollupService.java:63-69` 类级 Javadoc 架构不变量声明 + `StandardCostResolver.java:40-46` 同构声明；`TestErpMfgCostRollupValueExemptionInvariant` 2 测试 + `TestErpInvStandardCostResolverValueExemptionInvariant` 2 测试，反射 @Inject 字段类型集合硬失败断言 + 期望集 tripwire，FORBIDDEN_FQNS 用真实 FQN `io.nop.api.core.auth.IUserContext`；`costing-methods.md §成本卷算取值豁免边界` E3.2 已固化升级；`roles-and-permissions.md` E3.2 交叉引用；`docs/logs/2026/08-10.md` E3.2 条目）；(2) Anti-Hollow 通过——守卫测试含真实断言（assertFalse/assertTrue），非空体/非 return null，Javadoc 非样板；(3) @Inject 字段实测匹配（CostRollupService={IDaoProvider,BomExpander} / StandardCostResolver={IDaoProvider,IOrmTemplate}，无 user-context）；(4) 五点一致性通过（Plan Status completed / Phase 1-2 completed / 退出标准全 [x] / Closure Gates 全 [x] / 日志一致）；(5) Deferred 诚实——代理视图归 E4.x 带 trigger（Successor Required: yes），非范围内缺陷隐藏；(6) 文档同步（log + owner docs）。执行者未自我审计（Closure Gates 第 7 项由本独立审计勾选）。

Follow-up:

- <代理视图归 E4.x，消费 E3.2 取值豁免固化 + Q1 粒度冻结输入>
- <checker 脚本 R12c 基线 38 与实际 40 的预存漂移（非本计划引入）可归后续 compliance 维护 successor 同步脚本 header>
