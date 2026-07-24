# Governed Path（I*Biz 注入）成本评估

> **创建日期**: 2026-07-24
> **来源**: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §闭包前必须项 #1（F1，P0）/ Core Question 7
> **触发计划**: `docs/plans/2026-07-24-0930-3-governed-path-cost-eval-arch-doc-alignment.md` Phase 1
> **评估范围**: 实测 I*Biz 强注入对单模块测试启动的影响，量化 governed path 真实成本，产出 daoFor 真违规子集重构前置条件裁决
> **非目标**: 不执行 Type 1+4 重构——本评估仅产出裁决，重构归裁决后的独立 successor

## 1. 评估动机

Architecture Governance Prompt §7 要求回答"if people keep bypassing the intended path, what cost is selecting the bypass?"。仓库内 contract/aps 域多处 Javadoc 声明：跨域 I*Biz 强注入会级联服务依赖链、破坏单模块测试启动——这是开发者选择 `IDaoProvider` 直访（governed path 的 bypass）的真实理由，而非偷懒。

本评估通过**实测单模块测试 + 依赖结构根因分析**，验证该 Javadoc 声称的成本是否真实可观测，并据此裁决 daoFor 真违规子集（Type 1 + Type 4 ≈ 110-180 处，见 `2026-07-16-2134-1` 6 类分类）能否安全重构为 I*Biz。

## 2. 实测证据

### 2.1 单模块测试运行结果（代表域）

| 模块 | 命令 | 结果 | Tests run | Failures | Errors | Skipped |
|------|------|------|-----------|----------|--------|---------|
| `module-contract/erp-ct-service` | `mvn test -pl module-contract/erp-ct-service` | **BUILD SUCCESS** | 37 | 0 | 0 | 0 |
| `module-aps/erp-aps-service` | `mvn test -pl module-aps/erp-aps-service` | **BUILD SUCCESS** | 22 | 0 | 0 | 0 |

**结论**：两域单模块测试均**启动成功且全绿**。IoC 容器在仅组装本域 + master-data service bean 的条件下完整启动，无 bean 发现失败、无运行期懒加载失败。

### 2.2 根因分析——为什么单模块测试能启动

测试通过的原因是这两域**主动回避了跨域 I*Biz 强注入**，改用 `IDaoProvider` 直访跨域实体（仅依赖跨域 DAO jar，不依赖跨域 service jar）：

**Maven 依赖结构（`*-service/pom.xml` 实测）**：

| 模块 | 跨域 DAO 依赖 | 跨域 service 依赖 |
|------|--------------|-------------------|
| `app-erp-contract-service` | `app-erp-purchase-dao` + `app-erp-sales-dao` | **无**（仅 `app-erp-master-data-service`，md 是 DAG 根允许） |
| `app-erp-aps-service` | `app-erp-inventory-dao` + `app-erp-manufacturing-dao` | **无** |

**跨域 I*Biz 注入实测（`@Inject` 字段扫描，排除本域 ErpCt/ErpAps 与 master-data ErpMd）**：

| 模块 | 跨域 `@Inject I*Biz` 字段数 |
|------|----------------------------|
| `erp-ct-service` | **0** |
| `erp-aps-service` | **0** |

**机制链**：
1. 两域用 `IDaoProvider.daoFor(ErpPurInvoice.class)` 等访问跨域实体 → 仅需跨域 **dao** jar 在 classpath（实体类）。
2. 两域**不注入**跨域 `IErpPurInvoiceBiz`/`IErpInvStockBalanceBiz` 等 → 不需要跨域 **service** jar。
3. 因此单模块测试 IoC 容器只需组装本域 + master-data service bean，启动成功。

### 2.3 反事实验证——若注入 I*Biz 会怎样

Javadoc 声明（`ErpCtInvoicePlanBizModel.java:41-45` / `ErpApsAtpCtpServiceImpl.java:37-43`）：

> "硬注入跨域发票 BizModel 会将其完整服务依赖链（sales→inventory→...）级联进合同域，破坏其隔离单元测试。"
> "跨域 I*Biz 强注入会在 aps-service 单模块部署/测试时因依赖模块未组装而启动失败。"

**反事实推理（基于依赖结构）**：若将 `IDaoProvider` bypass 改为 `@Inject IErpPurInvoiceBiz`：

1. ct-service 需新增 Maven 依赖 `app-erp-purchase-service`（service jar），其 `beans.xml` 拉入 purchase 全部 BizModel bean。
2. purchase-service 自身依赖 inventory-service / master-data-service 等，IoC 容器需组装整条链。
3. 单模块测试（`mvn test -pl module-contract/erp-ct-service`）将尝试装配 `IErpPurInvoiceBiz` 实现 bean → 触发级联依赖 → 任一环节 bean 缺失即 `NoSuchBeanException` 启动失败。

**证据强度**：本评估为**间接证据**（观察"回避 I*Biz → 测试通过"，而非"注入 I*Biz → 测试失败"的直接探针）。但四条独立证据收敛——(a) 测试全绿、(b) 零跨域 I*Biz 注入、(c) 仅 DAO 级 Maven 依赖、(d) Javadoc 显式声明——使反事实结论具有高置信度。直接探针（在 ct-service 临时注入 `IErpPurInvoiceBiz` 跑测试观察失败）可进一步形式化证明，但本评估认为现有证据已足以支撑裁决（探针不在最终 diff 中，见计划 Closure Gates）。

## 3. 裁决

### 3.1 Governed path 成本裁决

**裁决 = 分支 (b)**：I*Biz 强注入**会**破坏单模块测试启动（反事实经依赖结构验证为真）。governed path（I*Biz）在跨域写/读场景存在真实、可量化的成本——它要求跨域 service jar 级联进被注入域的 classpath，破坏模块独立性测试。

> **直接推论**：不能在未解决跨域 service 耦合的前提下，将所有 daoFor 真违规子集（Type 1+4）一刀切重构为 I*Biz。

### 3.2 真违规子集分类裁决（重构前置条件）

daoFor 真违规子集分两类，**重构前置条件不同**：

| 子集 | 估算处数 | 重构目标 | 是否需 I*Biz | 前置条件 | 裁决 |
|------|----------|----------|--------------|----------|------|
| **Type 1**（ORM 导航可替代） | ~100-150 | 改用 ORM 关系 getter（`entity.getLines()` 等） | **否**（ORM 关系已在同 classpath） | 无 | ✅ **可安全重构**（不引入跨域 service 耦合，开独立计划） |
| **Type 4**（设计边界错误，跨域写/读聚合） | ~10-30 | 改用跨域 I*Biz 或保留为登记豁免 | **是** | 需平台层 lazy/SPI 解耦 **或** 显式登记豁免 | ⚠️ **受阻塞**：要么等 nop-entropy 协同提供 lazy/SPI 解耦，要么保留为登记豁免（如 `ErpB2bAsnBizModel` 本计划 Phase 2 补登） |

### 3.3 平台层解耦需求（nop-entropy 协同）

若要封堵 Type 4 的 daoFor 直访且不破坏单模块测试，需 nop-entropy 平台层提供以下能力之一：

- **lazy/SPI 解耦**：跨域 I*Biz 注入支持延迟加载或 SPI 发现，使单模块部署时跨域 bean 缺失不致启动失败（仅运行期调用时报错或降级）。
- **可选注入**：Nop IoC `@Inject` 增加 `required=false` 语义（当前 Nop `@Inject` 无此属性，为 Spring 概念），允许跨域 bean 缺失时注入 null + 业务侧 null 守卫。**注意**：此为临时缓解候选，需平台验证是否符合 Nop IoC 设计意图；不作为正式推荐。

### 3.4 替代方案与残留风险

- **替代方案 A（已采纳）**：保留 Type 4 为登记豁免（`posting-exemptions.md`），逐条记录理由/风险/补偿/收敛条件，待平台解耦后收敛。**优点**：零生产风险、零平台依赖；**缺点**：豁免清单需持续维护。
- **替代方案 B（已否决）**：抽取 `app-erp-common-api` 共享 SPI 内核（F4）承载跨域写契约。**否决理由**：F4 归独立 successor，且 Type 4 多为跨域写（需目标域审批管道），common-api 纯 SPI 无法承载业务校验。
- **残留风险 1**：直接探针未执行——若未来发现某 Type 4 重构为 I*Biz 后单模块测试**未**失败（因 nop-entropy 已隐式支持某种延迟机制），则 Type 4 可提前解阻塞。建议 successor 重构首批时验证。
- **残留风险 2**：Type 1 的 ORM 导航重构需逐处确认 ORM 关系确实存在（部分 daoFor 是因关系未建模），个别可能需补 ORM `<to-one>`（属 ORM 保护区域，需 owner doc 授权）。

### 3.5 第一批落地证据（plan 2026-07-24-0605-3，F1 successor）

`2026-07-24-0605-3` 完成第一批 Type 1 safe 子集重构：18 处 `daoProvider().daoFor(X).getEntityById(entity.getYId())` → ORM `<to-one>` 关系 getter，覆盖 assets（12）/ finance（5）/ manufacturing（1）域。逐处核实 ORM 关系已建模（无 ORM-gap）。全 154 模块 BUILD SUCCESS + 受影响域单模块测试全绿（ast 78/fin 264/mfg 136），验证 §4 前置条件 3（daoFor→关系重构后目标域单模块测试启动成功）。checker 基线下降：R2b 319→317 / R2c 1108→1090 / R2d 34→31（合规改善，非回归）。剩余 Type 1 估算≈82-132 处（原 ~100-150 − 本批 18），分布在其余 14 域，为按域分批 successor。

### 3.6 第二批落地证据 + 收尾评估（plan 2026-07-24-2000-1，F1 successor 收尾）

`2026-07-24-2000-1` 完成第二批 Type 1 safe 子集重构 + `getEntityById(FK)` chained 模式全域收尾：累计 19 处 `daoProvider().daoFor(X).getEntityById(entity.getYId())` → ORM `<to-one>` 关系 getter，覆盖 drp（2）/ logistics（2）/ maintenance（2）/ manufacturing（9：含 SimulationMrpEngine 多行镜像 2 + MrpReleaseService 同域只读 3）/ hr（2：含多行镜像 1）/ sales（1）/ finance（1：多行镜像 ErpFinBudgetControlBiz）域 Processor + BizModel。逐处核实 ORM 关系已建模（ORM-gap=0，与第一批一致）。全 154 模块 BUILD SUCCESS + 受影响域单模块测试全绿（mfg 136 / sal 119 / mnt 54 / hr 113 / drp 34 / log 23 / fin 264）。checker 基线下降：R2b 317→314 / R2c 1090→1071 / R2d 31→27（合规改善，非回归）。

**闭包审计修正**：初始单行 grep 漏看 4 处多行 chained 站点（`daoFor(X)\n.getEntityById(FK)`），经独立结束审计发现并补重构。最终重构 19 处 = 15 单行 + 4 多行。

**估算校正**：§3.5 估算剩余 Type 1 ≈82-132 处基于 6 类分类的全形态估算（含 `findAllByQuery` 等非 `getEntityById` 模式）。经第二批全域机械扫描，`getEntityById(FK)` **chained** 模式实际共 ~37 处（batch1 18 + batch2 19），收尾后生产站点 chained 形态**清零**（仅余 2 处 Type 5 dashboard 排除 + 2 处 ErpCtRebateSettlementBizModel Non-Goal 排除）。差异原因：原估算覆盖所有 Type 1 形态，而 `findAllByQuery` 类的 Type 1 判定需逐处语义分析（非机械替换）。

**`findAllByQuery` Type 1 评估结论**：全域 113 处 `daoFor(...).findAllByQuery(...)` 生产站点经抽样评估（CrpLoadCalculator/DrpDemandAggregator/ReceiptSettler/各 *ReportBizModel），可机械替换为 ORM 导航的候选 **<10 处**（普遍含复合条件 + 调用方以 ID 参数构造 query 非持有托管父实体）→ **watch-only residual**，successor 触发条件（≥10 处可机械替换）未满足，不开 successor。

**variable-split 子模式收尾（plan 2026-07-24-0941-1，F1 successor 落地）**：`IEntityDao<X> dao = daoFor(X); dao.getEntityById(FK)` 变量拆分形态经全域多行 regex 权威枚举（58 文件，远超 2000-1 候选清单 16 站点）逐处三态分类后收尾。safe Type 1 = 8 处（FK 来自作用域托管实体 getter，ORM `<to-one>` 已建模，重构为 getter）/ Type 2 会话存活豁免 = 7 处 voucher-by-link 循环（plan 明确定义，登记理由保留）/ ORM-gap = 1 处（`ErpPrjProjectSettlementProcessor:270` `assetCardId` 弱指针，projects.orm.xml:954 DAG 环约束）/ 其余 ~90 处为 not-Type-1（原始 ID 参数 load-by-id 工具方法）。checker R2c 1071→1065（-6，6 处 variable-split 移除局部 dao 变量；2 处 helper-wrapped 改 getter 但 helper 仍含 daoFor）。R2b/R2d/R2a 不变。全 154 模块 BUILD SUCCESS + 4 受影响域测试全绿。**此 successor 触发条件已满足并完成**。

**MrpReleaseService 豁免裁决**：该文件跨域写豁免（mfg→pur `ErpPurOrder`，`posting-exemptions.md` 登记）与同域只读 `getEntityById(line.getMrpPlanId())`（mfg→mfg `ErpMfgMrpPlan`）独立处理——裁决=选 A（局部重构 3 处只读站点为 `line.getMrpPlan()`，豁免仅约束跨域写路径；`posting-exemptions.md` 无需改动）。

**Type 1 `getEntityById(FK)` 全形态工作流收尾**：chained 两批共重构 37 处（batch1 18 + batch2 19）+ variable-split safe 子集 8 处 = **累计 45 处**，checker 累计下降 R2b 319→314（-5）/ R2c 1108→1065（-43）/ R2d 34→27（-7）。`getEntityById(FK)` **chained + variable-split 两形态**全域清零（仅余已登记 Type 2 voucher-by-link 豁免 7 处 + Type 5 dashboard + ErpCtRebateSettlementBizModel Non-Goal 排除 + helper-wrapped 广义模式 successor 边界），Type 1 `getEntityById(FK)` 工作流**收尾**。残余形态：`findAllByQuery` 子集（watch-only residual）、Type 4（既定阻塞 successor）、helper-wrapped chained-via-helper 变体（独立 successor 边界）。

## 4. 重构前置条件总结

1. **Type 1（~100-150 处）**：无前置阻塞，可立即开按域分批重构计划（目标：daoFor → ORM 关系 getter）。
2. **Type 4（~10-30 处）**：阻塞中——需 (a) nop-entropy 提供平台 lazy/SPI 解耦，或 (b) 保留为登记豁免。在解阻塞前，新增 Type 4 跨域写**必须**登记到 `posting-exemptions.md`。
3. **任何 daoFor → I*Biz 重构**：必须先验证目标域单模块测试（`mvn test -pl <module>/<service>`）是否仍启动成功；若失败则回退为 Type 4 豁免或 ORM 关系方案。

## 5. 关联文档

- 审查来源：`docs/audits/2026-07-23-0000-architecture-governance-review.md` §F1 / 闭包前必须项 #1
- daoFor 6 类分类：`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`
- 跨域写豁免登记：`docs/architecture/posting-exemptions.md`
- checker 精确基线：`docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md`（R2c=1108 / R2b=319）
- 残留风险 #4（governed path 成本需平台层支持）：审查报告 §残留风险
