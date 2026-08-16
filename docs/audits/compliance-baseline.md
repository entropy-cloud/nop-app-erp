# Nop 合规性检查基线（Compliance Baseline）

> Owner: `docs/audits/nop-compliance-checker.sh`（checker）+ `.github/workflows/compliance.yml`（CI 回归门控）
> 基线落盘日期: 2026-07-24
> 基线来源: 计划 `docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md` Phase 1 实测（`bash docs/audits/nop-compliance-checker.sh` 汇总表）

## 用途

本文件是 F8 治理 finding（`docs/audits/2026-07-23-0000-architecture-governance-review.md`）的**回归门控基线**：CI 每次运行 checker 后将汇总表命中数与本基线比对，**任何规则命中数超过基线即判 CI 失败**。

这激活了既有但此前未接入 CI 的 checker（dead armor → live guard，见治理审查 §Guards 槽与闭包前必须项 #4）。

## 基线表（16 行可计数规则）

| 规则 | 描述 | 严重度 | 基线命中 |
|------|------|--------|----------|
| R1a | dao().saveEntity (BizModel) | 🔴 高 | 0 |
| R1b | dao().updateEntity (BizModel) | 🔴 高 | 0 |
| R1c | dao().getEntityById (BizModel) | 🔴 高 | 0 |
| R1d | dao().findAllByQuery (BizModel) | 🔴 高 | 14 |
| R2a | BizModel daoFor(ErpMd*) | 🔴 高 | 34 |
| R2b | BizModel daoFor(Erp*) 跨域 | 🔴 高 | 230 |
| R2c | 全生产代码 daoFor() 总量 | 🔴 高 | 1399 |
| R2d | Processor daoFor(ErpMd*) | 🔴 高 | 34 |
| R3 | new Erp*() 构造实体 | 🟡 中 | 5 |
| R4 | extends RuntimeException | 🟢 低 | 0 |
| R5 | @Inject private | 🟡 中 | 0 |
| R6 | @Transactional in BizModel | 🟢 低 | 2 |
| R7 | System.currentTimeMillis() | 🟢 低 | 0 |
| R8 | Processor 无 xbiz 接线 | 🔴 高 | 0 |
| R10 | REQUIRES_NEW 事务 | 🟡 中 | 9 |
| R11 | Processor 重复状态判断方法 | 🟡 中 | 0 |
| R12a | 共享内核 import ErpFinBusinessType | 🟡 中 | 69 |
| R12b | 共享内核 import PostingEvent | 🟡 中 | 66 |
| R12c | 共享内核 import AcctSchemaResolver | 🟡 中 | 40 |

> R9（doReverseApprove 一致性）为**定性校验**（输出 ✓/✗ 清单，无数值计数），故不在上表参与数值门控；其输出仍由 checker 打印供人工查阅，CI 不对其做数值断言。

## R2c 增量注记

R2c=1108 较 `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md` 历史快照（965）增长 **+143**，因后续 A2/A3/B1 等深化工作新增生产代码（含跨域写豁免登记的合法新增）。该 delta 已被本次基线吸收，后续以 1108 为回归门控起点。

## R3 同步注记（plan 2026-07-24-0930-2）

`2026-07-24-0930-2`（字典与状态枚举真相统一）为 cs/mnt/mfg/qa 4 域新增 dao 层 `Erp*DocStatus` 接口 + `Erp*Constants extends` 关系，消除 approve-status/doc-status 常量重复声明。该变更**不引入任何 `new Erp*()` 实体构造**（纯接口继承），故 R3 基线 **不变（=19）**。checker 实测全 16 规则均 ≤ 基线（R2c=1108 / R3=19 / R11=0 等），无回归。

## R3/R11 同步注记（plan 2026-07-24-0605-2，F2(d) successor）

`2026-07-24-0605-2`（硬编码状态字面量→`Erp*DocStatus` 常量收敛）将全 9 域服务层 doc/approve 轴裸字面量替换为常量引用，并消除 inv `ErpInvCostAdjustProcessor`/`ErpInvLandedCostProcessor` + mfg `MrpReleaseService` 的本地重复常量定义（统一引用 dao 层 `Erp*DocStatus`）。该变更**不引入任何 `new Erp*()` 实体构造**（纯字面量→常量引用 + import 替换），故 R3 基线 **不变（=19）**；不新增 Processor 重复状态判断方法，故 R11 基线 **不变（=0）**。checker 实测全 16 规则均 ≤ 基线（R3=19 / R11=0 / R2c=1108 等），无回归。替换框/排除集边界见 `docs/audits/hardcoded-status-literal-inventory.md`。

## R12 同步注记（plan 2026-07-24-1400-1）

`2026-07-24-1400-1`（隐性共享内核显式化，F4 闭包项 #5）裁决=分支 (b)：接受 finance/master-data 的 3 个跨域语义类型（`ErpFinBusinessType` enum / `PostingEvent` DTO / `AcctSchemaResolver` dao 耦合工具）为**显式共享内核**——类型不迁移，经 owner doc 登记（`module-boundaries.md §共享内核` + `data-dependency-matrix.md`）+ 本 R12 守卫追踪跨域 import 基线。裁决依据（enum 不可降级为 SPI 接口）见 `docs/analysis/shared-kernel-extraction-decision.md`。

R12 计数口径 = import 语句级，排除 `test/` + 类型所属域（finance / master-data）+ `_gen`/`target`，与裁决文档 §2 基线一致。基线落盘值（69/66/38）经 checker 实测复核。**门控方向**：跨域 import 增长（actual > baseline）→ CI 失败，须开独立计划裁决新增消费方合理性后调高基线；类型演进（如新增 enum 常量）不触发 R12 增长（仅新增消费方文件才增长），故合法的字典扩充不受阻塞。

## 回归门控规则

- **门控方向：单向收紧**。新增命中数（实际 > 基线）→ CI 失败（regression）。命中数下降（实际 < 基线）→ CI 通过，且**鼓励**（不强制）更新本基线以反映改善。
- **调高基线的唯一途径**：开独立计划，在该计划中逐项人工确认新增命中的合理性（合理偏离 / 已登记豁免 / 需重构），并显式更新本文件的基线表与下方机器可读块。**禁止在功能 PR 中直接调高基线**。
- **门控实现方式**：CI workflow（`.github/workflows/compliance.yml`）解析 checker 汇总表，比对下方 `## BASELINE (machine-readable)` 块。checker 脚本本身保持纯报告工具，不侵入其核心逻辑（Phase 1 Decision 裁决方案 b）。

## R2b/R2c/R2d 基线下降注记（plan 2026-07-24-0605-3，F1 successor）

`2026-07-24-0605-3`（daoFor Type 1 ORM 导航可替代重构第一批）将 18 处 `daoProvider().daoFor(X).getEntityById(entity.getYId())` 替换为 ORM `<to-one>` 关系 getter（`getAsset()`/`getCategory()`/`getSourceAsset()`/`getEmployee()`/`getClaimant()`/`getSubject()`/`getVoucher()`/`getBom()`），覆盖 assets（12 处）/ finance（5 处）/ manufacturing（1 处）域 Processor + BizModel。该变更为**合规改善**（非回归），checker 实测基线下降：

- R2b（BizModel 跨域 daoFor）：319 → **317**（-2，ErpFinEmployeeAdvanceBizModel 2 处）
- R2c（全生产代码 daoFor）：1108 → **1090**（-18，全部重构站点）
- R2d（Processor daoFor(ErpMd*)）：34 → **31**（-3，finance 3 处 Processor ErpMd* 站点）

R2a 不变（=37，BizModel ErpMd* 站点未触及）。全 154 模块 `mvn clean install -DskipTests` BUILD SUCCESS + 受影响域单模块测试全绿（ast 78/fin 264/mfg 136）。本批 ORM-gap=0，剩余 Type 1 域 safe 子集为 successor。

## R2b/R2c/R2d 基线下降注记（plan 2026-07-24-2000-1，F1 successor 第二批）

`2026-07-24-2000-1`（daoFor Type 1 ORM 导航可替代重构第二批 + 收尾评估）将剩余 15 处 `daoProvider().daoFor(X).getEntityById(entity.getYId())` 替换为 ORM `<to-one>` 关系 getter（`getBaseDrpPlan()`/`getComputedDrpPlan()`/`getCarrier()`/`getEquipment()`/`getMaterial()`/`getWorkOrder()`/`getMrpPlan()`/`getSourceSalary()`/`getReceipt()`），覆盖 drp（2）/ logistics（2）/ maintenance（2）/ manufacturing（7：含 MrpReleaseService 同域只读 3 处）/ hr（1）/ sales（1）域 Processor + BizModel。该变更为**合规改善**（非回归），checker 实测基线下降：

- R2b（BizModel 跨域 daoFor）：317 → **315**（-2，BizModel 站点 ErpLogShipmentBizModel + ErpHrSalarySimulationBizModel）
- R2c（全生产代码 daoFor）：1090 → **1075**（-15，全部重构站点）
- R2d（Processor daoFor(ErpMd*)）：31 → **27**（-4，mnt/mfg 4 处 Processor ErpMdMaterial 站点）

R2a 不变（=37，BizModel ErpMd* 站点未触及）。本批 ORM-gap=0（15/15 ORM `<to-one>` 关系均已预核存在）。MrpReleaseService 裁决=选 A（局部重构 3 处同域只读 `getMrpPlan()`，豁免仅约束跨域写路径 `ErpPurOrder`，`posting-exemptions.md` 无需改动）。`findAllByQuery` Type 1 评估：全域 113 处站点中可机械替换候选 <10 → watch-only residual，不开 successor。本批收尾后 `getEntityById(FK)` **chained** 模式生产站点全域清零（仅余 2 处 Type 5 dashboard 排除）。全 154 模块 BUILD SUCCESS + 6 受影响域单模块测试全绿（mfg 136/sal 119/mnt 54/hr 113/drp 34/log 23）。

**闭包审计修正（2026-07-24）**：独立结束审计发现初始单行 grep 漏看 4 处**多行 chained** `daoFor(X)\n.getEntityById(FK)` 站点（与已重构站点同型，仅换行）：`SimulationMrpEngine.java:107-108`（→`scenario.getBaseMrpPlan()`）/ `:183-184`（→`version.getComputedMrpPlan()`）/ `ErpHrSalarySimulationBizModel.java:651-652`（→`simulation.getSourceSalary()`，与已重构 :771 同文件）/ `ErpFinBudgetControlBiz.java:151-152`（→`line.getScenario()`）。4 处均已补重构，checker 实测二次下降：R2b 315→**314** / R2c 1075→**1071**（最终 R2c 1090→1071，-19 = 15 单行 + 4 多行）。本计划累计重构 **19 处**。残余 chained 模式仅余 `ErpCtRebateSettlementBizModel` 2 处（Non-Goal 显式排除文件）。**variable-split 子模式**（`dao = daoFor(X); dao.getEntityById(FK)`，~15 处跨 fin/inv/ast/prj）为 successor（需逐处语义分析 Type 1 vs Type 2 会话豁免 vs ORM-gap，非机械替换）。

## R2c 基线下降注记（plan 2026-07-24-0941-1，F1 successor variable-split 收尾）

`2026-07-24-0941-1`（daoFor Type 1 variable-split 子模式 ORM 导航重构）完成 `getEntityById(FK)` variable-split 形态全域分类 + safe 子集重构。variable-split 权威枚举（多行 regex `IEntityDao<...> dao = daoFor(X); ... dao.getEntityById`）实测 58 文件（远超 2000-1 候选清单 16 站点）。三态分类结果：**safe Type 1 = 8 处**（FK 来自作用域托管实体 getter，ORM `<to-one>` 已建模）/ **Type 2 会话存活豁免 = 7 处 voucher-by-link 循环**（plan 明确定义）/ **ORM-gap = 1 处**（`ErpPrjProjectSettlementProcessor:270` `assetCardId` 弱指针，projects.orm.xml:954 DAG 环约束无 ORM 关联）/ 其余 ~90 处为 not-Type-1（原始 ID 参数 load-by-id 工具方法）。

safe 子集 8 处重构为 ORM 关系 getter（`line.getMaterial()`/`facility.getFundAccount()`/`fundAccount.getSubject()`/`timesheet.getActivityType()`/`project.getProjectType()`/`line.getAsset()`/`debt.getSourceArApItem()`/`line.getSourceAsset()`）。checker 实测基线下降：

- R2b（BizModel 跨域 daoFor）：314 → **314**（不变，重构站点全在 Service/Processor/Builder 非 BizModel）
- R2c（全生产代码 daoFor）：1071 → **1065**（-6 = 6 处 variable-split 移除局部 dao 变量声明；2 处 helper-wrapped 站点 `assetDao()`/`arApItemDao()` 改 getter 但 helper 方法仍含 daoFor 供 newEntity/saveEntity，不计入下降）
- R2d（Processor daoFor(ErpMd*)）：27 → **27**（不变，ast/prj Processor 的 ErpMdSubject daoFor 未触及）

R2a 不变（=37）。本批收尾后 `getEntityById(FK)` **chained + variable-split 两形态**生产站点全域清零（仅余已登记 Type 2 voucher-by-link 豁免 7 处 + Type 5 dashboard + Non-Goal 豁免文件）。全 154 模块 `mvn clean install -DskipTests` BUILD SUCCESS + 4 受影响域单模块测试全绿（inv 114/ast 78/prj 67/fin BUILD SUCCESS）。

## R3/R7 收敛注记（plan 2026-07-24-0941-2，R3 构造 + R7 clock helper 收敛）

`2026-07-24-0941-2`（R3 `new Erp*()` 构造 + R7 `System.currentTimeMillis()` 合规收敛）完成两项：

**R7（2 → 0，确定性修复）**：两处 `System.currentTimeMillis()` 直调替换为平台 helper `CoreMetrics.currentTimeMillis()`——`ErpFinGlMappingResolver:259`（GL 映射缓存 TTL 降级时间戳）+ `ErpMdExchangeRateApiClientFactory:69`（汇率 API 客户端 TTL 缓存）。平台 helper 权威来源 `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md` + AGENTS.md 平台 helper 强制规则（R13 不可降级项）。语义等价（同 epoch millis），受影响域既有测试全绿（fin 264 / md 109 含 `TestErpMdExchangeRateApiClient`）。

**R3（19 → 5，测量口径校准 + 瞬态登记）**：

1. **三态分类**（iter-1/iter-2 独立审查 + Phase 1 全量复核定终值，A=14 / B=0 / C=5）：
   - **A false positive（14 处，非 ORM 实体）**：`ErpApsSchedulingEngine`（aps 调度引擎 service 类 ×3，NOT in orm.xml）/ `ErpCrmTerritoryPipeline` + 其 3 个内部 `@DataBean`（crm，DTO/value 类，NOT in orm.xml）/ `ErpCrmPipelineAccumulator`（crm support 累加器）/ `ErpFinPostingMetricsSnapshot` + 其 `.MetricValue` 内部类 ×5（fin 跨层契约 DTO，代码注释明示「非 ORM 实体」）/ `ErpQaActionImpl`（qa 私有内部投影类，实体是 `ErpQaAction` 非 Impl）。
   - **B 合法持久化创建（0 处）**：iter-1 审查逐处核实原草案 B 站点实为 C 或 A，B 类为空。
   - **C 瞬态聚合/虚拟实体（5 处，ORM 实体作内存计算容器）**：`ErpCrmFunnelStageMetrics`（crm 纯函数引擎快照，调用方 saveEntity）/ `ErpCrmQuota`（crm 虚拟聚合行只读返回）/ `ErpHrGapAnalysis`（hr daoProvider==null fallback，已优先 newEntity）/ `ErpMfgMrpDemand`（mfg 仿真内存构造）/ `ErpSalOrderLine`（sal 赠品行评估快照，调用方持久化）。5 处代码注释均已文档化瞬态用途，符合 `nop-backend-dev` skill「ORM 实体构造反转模式」例外（纯函数引擎由调用方 new 实例化、测试直接 new，改 newEntity() 破坏无状态纯净性 + 测试可构造性），**全部保留登记**，无一改 DTO（改 DTO 破坏公共返回类型，触 Non-Goal）。

2. **测量口径校准裁决=option (c) 交叉引用 `*.orm.xml` 实体声明**（精确校准，0 FP / 0 FN）：checker 脚本运行时从源 `model/*.orm.xml` `<entity className>` 动态提取已注册实体短名白名单，R3 仅对 `new <RegisteredEntity>()` 计数。option (a) 后缀排除法否决（iter-1 审查 Major-2 实证仅能排除 ~4/14，余 ~10 非实体类无匹配后缀仍被计数）。校准后 R3 从 19 下降至真实 domain entity 构造计数 **5**（=C 子集，健康合法基线 > 0，符合 Non-Goal「不强求 R3→0」）。未来新增实体自动纳入白名单（checker 运行时动态提取），无需手工维护。

**合法持久化 B 类 baseline rationale**：B 类=0，无对象登记。B 类若未来出现（`new Erp*()` + 同方法块 `saveEntity()`）是 Nop 标准模式，非违规——但应在出现时开独立计划登记为合法基线，不在此预设。

**校准实施位置**：`docs/audits/nop-compliance-checker.sh` R3 段（构建 ENTITY_WHITELIST + 逐行 cls 提取 + `grep -qxF` 白名单比对）。全仓 `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）+ 受影响域 `mvn test` 全绿（fin 264 / md 109）+ checker 复跑 R3=5 / R7=0。

## R3 同步注记（plan 2026-07-26-0300-1，F2(#3) doc-status successor）

`2026-07-26-0300-1`（doc-status 共享字典统一）将 6 域 28 列 ORM `ext:dict="erp-<domain>/doc-status"` 统一为 `erp/doc-status` + 共享 dict 创建于 `module-common-service` + 6 处内联 `<dict>` 移除 + 6 份 per-domain YAML 删除 + logistics stale 清理 + `ErpMntDaoConstants extends ErpMntDocStatus` 兼容链修复。该变更**不引入任何 `new Erp*()` 实体构造**（纯 ORM ext:dict 属性值变更 + 内联 dict 定义删除 + YAML 删除 + 一处 extends 子句扩展），故 R3 基线 **不变（=5）**。checker 实测 R3=5 与基线一致，无回归。其余 15 规则均 ≤ 基线（本计划未触及 BizModel/Processor Java 代码，R1d=28 的 +5 漂移来自前序未同步基线的计划，与本计划无关）。

## R2b/R2c/R2d 基线裁决性上调注记 + R8 checker 校准（plan 2026-07-25-1057-1）

`2026-07-25-1057-1`（合规基线漂移裁决，CI red fix）处理基线门控上线后（`2026-07-24-0930-1` 之后）由已审计深化计划引入的 daoFor 漂移 + R8 抽象基类 false positive。**漂移源全部来自已经独立草案审查 + 结束审计的计划**，其生产代码变更已经审计验证为合法跨域编排。

**R8 checker 校准**（actual 49 → 42，baseline 不变=42）：`module-common-service/` 下 7 个 `Abstract*Processor`（`public abstract class ... extends AbstractProcessor<T>`）由 `2026-07-24-2200-1` Phase 1 创建，为跨域共享抽象基类（非领域 Processor，不经 xbiz 路由，由具体子类继承后经 BizModel `@Inject` 消费）。R8 原始语义「领域 Processor 缺少 xbiz 接线」不覆盖抽象基类 → checker R8 段校准=排除 `module-common-service/` 目录（对齐 0941-2 R3 交叉引用 orm.xml 先例的「排除集」思路，Decision 选方案 A）。**残留风险**：若未来在 `module-common-service/` 新增具体（非 abstract）领域 Processor，本排除会静默豁免——届时升级为动态 `abstract class` 提取（开独立 successor）。校准实施位置：`nop-compliance-checker.sh` R8 段（find 新增 `-type d -name module-common-service -prune`）。

**R2b/R2c/R2d 基线裁决性上调**（逐项合法性分类，源计划均已审计）：

| 规则 | 旧基线 | 新基线 | 漂移源（已审计计划） | 合法性分类 |
|------|--------|--------|---------------------|-----------|
| R2b | 314 | **315** | `2026-07-24-1351-3` `ErpFinBudgetCommitmentBizModel`（承付款 sales→finance 跨域编排） | ✅ 合法跨域编排（承付款过账需读取 master-data subject + sales delivery/order） |
| R2c | 1065 | **1079** | `2026-07-24-1351-2`（intercompany +17 毛）+ `2026-07-24-1351-3`（commitment +16 毛）+ `2026-07-24-1351-1`（GL Mapping +3 毛）− 同提交内重构/移除 | ✅ 合法跨域编排（intercompany 跨公司凭证 / commitment 承付款过账 / GL Mapping 全域接入均为设计既定跨域编排） |
| R2d | 27 | **28** | `2026-07-24-1351-3` `ErpSalOrderProcessor:377`（commitment subject resolution，config-gated `erp-fin.budget-commitment-enabled` 默认 false） | ✅ 合法跨域编排（销售订单审核后置 commitment 释放钩子） |

逐站点 file:line 清单 + 源计划 + 合法性分类 + git 提交时间线核实见 `docs/plans/2026-07-25-1057-1-compliance-baseline-drift-adjudication.md` §Phase 1 Evidence。checker 复跑全 16 规则 actual ≤ baseline（R2b=315≤315 / R2c=1079≤1079 / R2d=28≤28 / R8=42≤42），CI green 恢复。

**纪律强化**（见 `docs/analysis/governed-path-cost-evaluation.md` §基线漂移复发防护）：功能计划的生产代码新增 daoFor 后，closure audit 须核实 checker 基线是否漂移；若漂移则须在 closure 前开独立基线裁决计划（或在 closure gates 中显式记录「基线漂移已知，归 successor 基线裁决计划」）。本计划为先例。

## R8 二次校准（per-mutation 排除）+ R2c 上调注记（plan 2026-07-25-1057-2）

`2026-07-25-1057-2`（per-mutation Processor 文件拆分）将 27 个含至少 1 个标准审批 S-mutation 的 monolithic Processor 拆分为 149 个 per-mutation 文件，每个继承对应 `Abstract*Processor<T>`（`AbstractApproveProcessor` / `AbstractRejectProcessor` / `AbstractSubmitForApprovalProcessor` / `AbstractReverseApproveProcessor` / `AbstractWithdrawApprovalProcessor` / `AbstractCancelProcessor`）。拆分候选范围裁决见 `docs/analysis/per-mutation-processor-split-plan.md`（42 Processor 中 15 个无 S-mutation 故不拆分，27 个有 S-mutation 拆分；实际产出 149 per-mutation 文件，修正原估 ~250）。

**R8 checker 二次校准**（actual 191 → 42，baseline 不变=42）：per-mutation Processor 文件（如 `ErpPurOrderApproveProcessor`）经 BizModel `@BizMutation` → `@Inject` 路由消费（非 Processor xbiz 接线），R8 原始语义「领域 Processor 缺少 xbiz 接线」不覆盖 per-mutation 子类（其路由面是 BizModel `@BizMutation`，而 BizModel 已有自身方法声明经反射自动生成 GraphQL schema）。校准=checker R8 段循环内跳过类体含 `extends Abstract*Processor` 的文件（content-based 排除，对齐 1057-1 排除集思路）。**残留风险**：若未来 per-mutation Processor 不继承抽象基类（如手写 per-mutation 类未 `extends Abstract*`），本 grep 会漏排除——届时升级为 per-mutation 文件命名规则动态匹配（开独立 successor）。校准实施位置：`nop-compliance-checker.sh` R8 段 while 循环内新增 `grep -qE 'extends Abstract[A-Z][a-zA-Z]*Processor'` 早退。

**R2c 基线裁决性上调**（1065 旧基线 → 1057-1 已上调至 1079 → 本计划上调至 **1228**）：

| 规则 | 旧基线 | 新基线 | 漂移源（本计划） | 合法性分类 |
|------|--------|--------|------------------|-----------|
| R2c | 1079 | **1228** | 149 个 per-mutation Processor 每个文件实现抽象基类的 `protected IEntityDao<T> dao()` 抽象方法，方法体 `return daoProvider.daoFor(<EntityClass>.class);` 一行，是 AbstractProcessor<T> 编排骨架（`requireEntity` / `dao().updateEntity(entity)`）的强制契约。149 × 1 daoFor = +149 | ✅ 抽象基类契约强制（每个 per-mutation Processor 是独立 IoC bean，须独立实现 dao() 而非共享静态辅助；`daoProvider.daoFor(<EntityClass>)` 是 Nop 平台读取托管实体 DAO 的标准方式，非业务跨域编排） |

R2a/R2b/R2d 不变（per-mutation Processor 不是 BizModel 故不命中 R2a/R2b；不在跨域 ErpMd* 站点故不命中 R2d）。逐站点证据：每个 per-mutation Processor 文件均含 `protected IEntityDao<T> dao() { return daoProvider.daoFor(<EntityClass>.class); }` 方法体（plan 1057-2 Phase 1-4 拆分产出）。checker 复跑全 16 规则 actual ≤ baseline（R8=42≤42 / R2c=1228≤1228），CI green 保持。

**R2c 后续治理方向（successor）**：149 处 `dao()` 方法的 daoFor 调用是抽象基类契约的机械产物（每个 per-mutation Processor 重复一行），可用以下方式消除（非本计划范围）：
- (a) AbstractProcessor<T> 改用 `Class<T>` 构造参数 + `daoProvider.daoFor(entityClass)` 单次解析缓存（破坏当前无构造函数签名，影响所有子类）
- (b) AbstractProcessor<T> 改用 Nop 泛型反射 (`io.nop.core.type.GenericTypeResolver`) 推断 T 的具体类（运行时反射开销，且 Java 泛型擦除需保留 Class 字段）
- (c) 接受当前 +149 daoFor 为抽象基类代价（基线 1228 永久接受）

本计划裁决=选 (c)（接受基线上调），(a)/(b) 为 successor 候选。

## R1d/R10/R6 checker 注释排除校准 + 基线裁决注记（plan 2026-07-27-0823-1）

`2026-07-27-0823-1`（R1d/R10/R6 合规基线裁决 + checker 注释校准，CI red fix）处理 R1d baseline=23 / actual=28 的 +5 漂移（CI red）+ R10/R6 grandfathered 基线（51/7）的注释虚假命中主导问题。三规则 checker 原始 grep 均不区分代码行与 javadoc/注释行，与 R8 content-based 排除先例（1057-1/1057-2）同型缺陷。

**checker 注释排除校准**（per-rule option b，不动 `rgrep_bizmodel`/`rgrep_prodjava` helper，避免影响已稳定的 R1a/R1b/R1c/R2a/R2b）：R1d/R6/R10 三规则 grep 管道后追加 `grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*|\*/)' | grep -vE '\{@code|\{@link'`——排除 javadoc 续行（`*`）+ 行注释（`//`）+ 块注释开闭（`/*`/`*/`）+ `{@code`/`{@link` 安全网。校准对齐既有 checker 行级启发式风格（不引入 AST 解析，Non-Goal 显式排除）。校准实施位置：`nop-compliance-checker.sh` R1d/R6/R10 段。**残留风险**：块注释 `/* ... */` 跨行命中（无 `*` 续行前缀）漏排除——三规则 Phase 1 实测为 0；若未来出现升级为 AST 解析（开独立 successor）。

**基线裁决**（注释校准后 actual 反映真实代码站点，逐项合法性分类）：

| 规则 | 旧基线 | 新基线 | actual（校准后） | 合法性分类 |
|------|--------|--------|------------------|-----------|
| R1d | 23 | **17** | 17 | ✅ 17 处全部为同域只读内部辅助查询（crm 6 + master-data 8 + sales 3），绕过 CrudBizModel findList 管道的 objMeta 过滤/字段投影为有意设计（C3 日期范围互斥校验 + 价格清单维度查询 + 树形递归 + 唯一性前置友好校验），每处代码注释明示理由。owner doc 背书：`docs/design/date-ranged-validity-pattern.md`（C3）+ 各 BizModel inline 注释。 |
| R10 | 51 | **6** | 6 | ✅ 6 处全部为 `docs/architecture/processor-extension-pattern.md` 硬规则 1 文档化的合法跨域失败隔离事务边界——ErpFinVoucherBizModel post/reverse Facade `@Transactional(REQUIRES_NEW)`（2）+ ErpFinPostingExceptionRecorder/ErpFinDeferredPostingRetryHelper 的 `runInTransaction(...REQUIRES_NEW...)` 独立事务（4）。代码注释 `nop-check: allow @Transactional(REQUIRES_NEW)` 交叉引用 owner doc。 |
| R6 | 7 | **2** | 2 | ✅ 2 处 = R10 BizModel 子集（ErpFinVoucherBizModel post/reverse），同 R10 owner doc 背书。 |

**R1d +5 漂移源裁决**：经 `git diff bd8540037..HEAD -- '*BizModel.java' | grep findAllByQuery` 核实，+5 checker 漂移**全部来自单一已审计计划** `2026-07-26-0315-1`（C3 sales 定价推广）——3 处真实 `dao().findAllByQuery` 调用（ErpSalPricingRule/PriceListLine/PriceList）+ 2 处 javadoc 注释行，全部晚于基线锚点 `bd8540037`（2026-07-24 11:43:20）。master-data BizModels 的 findAllByQuery 调用由 `2026-07-21-2225-1`（C3，2026-07-21）+ `2026-07-21-1206-1`（C2，2026-07-21）落地，均早于基线锚点已计入 baseline 23，post-baseline diff 为 0 不构成漂移。漂移经注释校准（排除 2 javadoc 行）+ 裁决性下调基线（23→17，吸收 3 真实调用并反映真实代码计数）双重吸收。

逐站点 file:line 清单 + 三态分类（A 真实代码 / B javadoc / C 行注释）+ git 提交时间线核实 + Decision 理由见 `docs/plans/2026-07-27-0823-1-r1d-r10-r6-compliance-baseline-adjudication.md` §Phase 1 Evidence。checker 复跑全 16 规则 actual ≤ baseline（R1d=17≤17 / R6=2≤2 / R10=6≤6），R1d CI red 解除，CI green 恢复。

**纪律强化引用**：checker 注释排除校准是测量口径修正（非基线放水），对齐 `docs/analysis/governed-path-cost-evaluation.md §基线漂移复发防护 + checker 校准范式`（R3 orm.xml 白名单 + R8 排除集 + R8 per-mutation 排除 + 本计划 R1d/R6/R10 注释排除四先例构成的 checker 校准范式矩阵）。校准后基线语义清晰反映真实代码站点，未来注释增减不再触发虚假漂移。

## M0 锚点注记（审计-修复回归起点，plan 2026-07-27-1015-1）

> 本段是 `audit-remediation-roadmap.md` M0.3 工作项实测落锚：为 MA1–MA7 审计与 MR 修复提供可对比的回归起点。MV 验证里程碑（V.1/V.2）将对比此锚点。

| 字段 | 值 |
|------|----|
| 锚定日期 | 2026-07-27 |
| HEAD commit | `0e963531d4b07d44b593828a7aab048ea0c9d3db` |
| 工作树状态 | dirty（仅文档变更：scope matrix / arm-index / 本文件 M0 段 / roadmap / 计划；零 Java/ORM/契约变更） |
| compliance checker | `bash docs/audits/nop-compliance-checker.sh` → 全 19 规则 actual ≤ baseline（精确匹配，0 漂移） |
| mvn build | `mvn clean install -DskipTests` → BUILD SUCCESS（156 reactor 模块，0 errors） |
| mvn test | `mvn test` → BUILD SUCCESS（**0 failures / 0 errors / 1 skipped** = 已知 `ErpAllWebPagesCollectTest` `@Disabled`，见 `known-good-baselines.md §Known Failures (Accepted)`） |

**Compliance 规则汇总快照**（与上文 BASELINE machine-readable 块逐行精确一致，0 漂移）：

| 规则 | Baseline | Actual（HEAD=0e963531d） | 状态 |
|------|----------|--------------------------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 17 | 17 | ✅ |
| R2a/R2b/R2c/R2d | 37/315/1228/28 | 37/315/1228/28 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅ |
| R6 | 2 | 2 | ✅ |
| R7 | 0 | 0 | ✅ |
| R8 | 42 | 42 | ✅ |
| R10 | 6 | 6 | ✅ |
| R11 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/38 | 69/66/38 | ✅ |

**测试计数说明**：本次实测 `mvn test` 单元测试方法计数 = **1756**（surefire per-module 汇总，per-module 0 failures / 0 errors / 1 skipped）。roadmap `§框架/平台复用` 与 `§当前基线` 中历史引用的「~2890 测试」与实测不符（1756）—— 该数字应理解为起草期粗估，已被本次实测修正。MV V.1 验证里程碑以本注记的 1756 为对比起点，不依赖 ~2890。该项为文档计数漂移（非 CI red），后续 roadmap / project-context 文案修正归 G.4（已知失败模式与基线文案更新工作项）处理，不阻塞 M0 闭包。

## F15 i18n 基线注记（plan 2026-07-29-0749-3，A4.9 全域 i18n 完整性审计）

> 本段是 F15 i18n regression gate 的回归门控基线，由 MA4 工作项 A4.9（全域 i18n 完整性审计）首次落锚。与本文件主体 F8 compliance checker（`nop-compliance-checker.sh`）不同，F15 由独立的 `docs/audits/i18n-coverage-checker.sh` 承载。MV 验证里程碑（V.1/V.2）对比此锚点。

| 字段 | 值 |
|------|----|
| 锚定日期 | 2026-07-29 |
| 审计工具 | `bash docs/audits/i18n-coverage-checker.sh`（quality + `--strict` 双模式） |
| 扫描范围 | 全域 19 业务域手写 `*.view.xml` + `erp-*.action-auth.xml`（排除 `/target/` `/​_dump/` `/​_gen/`）= **373 文件**（354 view.xml + 19 action-auth.xml） |
| quality 缺陷基线（DEFECTS） | **0** |
| strict 覆盖缺口基线（COVERAGE GAPS） | **0** |
| 域级覆盖率 | 19 域全域 ~100%（无系统性缺口域） |
| 双模式 EXIT | 0（PASS） |

**基线来源**：plan `2026-07-29-0749-3-audit-remediation-ma4-i18n-coverage.md` Phase 1 实测（独立复跑 `i18n-coverage-checker.sh` 双模式确认 frontend-ui-roadmap F15 英文化批次零基线稳定，与起草时实测一致无漂移）。报告详见 `docs/audits/2026-07-29-0749-arm-ma4-i18n-coverage.md`。

**门控方向**：与 F8 同向收紧。quality 缺陷 > 0 或 strict 缺口 > 0 = 回归。**门控状态**：F15 checker **已接入 CI workflow** `.github/workflows/compliance.yml` i18n job（R3.7，plan 2026-07-31-1439-3），live guard——defects>0 或 gaps>0 触发 CI red。接入对齐 F8 `nop-compliance-checker.sh` 的 CI 接入模式（option b「gate 逻辑在 CI / checker 保持纯 reporter」），由 A7.4（CI/guard 激活审计）裁决落地。本基线登记为 A7.4 持续激活验证的起点。

## V.2 compliance 漂移裁决注记（plan 2026-07-31-1705-2，MV V.2）

`2026-07-31-1705-2`（全量验证基线：构建/测试绿 + compliance 基线裁决）是审计-修复回归起点的**首次全量绿基线复确认**。V.1 落定：`mvn clean install -DskipTests` BUILD SUCCESS（**156 reactor 模块**——权威值，消除 154 vs 156 口径分歧，落定=156）+ `mvn test` 0 failures / 0 errors / **1 skipped**（`ErpAllWebPagesCollectTest @Disabled`）+ 单测方法计数 **1902**（M0 锚点 1756 +146，由 post-M0 R3.x/R3.4 等深化计划新增测试）。

V.2 对照 M0 锚点（HEAD=`0e963531d`，全 19 规则 0 漂移）复跑 `nop-compliance-checker.sh` 实测 5 项 post-M0 漂移（与 A7.4 §残留风险「5 项 post-audit compliance 回归」一致，A7.4 已将其 deferred 至 MV V.2）。本计划逐项裁决：每项 `Fix`（驱动回降）或 `adjudicated baseline-raise`（经独立计划裁决上调 BASELINE 块 + per-site 证据）。**操作性门控基线**=本文件 §BASELINE 机器可读块（已含历次裁决性上调）；「M0 基线」是概念锚点。

**裁决汇总表**：

| 规则 | M0 锚点 | 漂移后实测 | 裁决 | 新基线 | 来源 |
|------|---------|-----------|------|--------|------|
| R5（@Inject private） | 0 | 1 | **Fix**（移除 `private` → 包级可见） | **0**（不变） | `module-common-service/.../auth/ErpRoleDataAuthChecker.java:30-31` `@Inject private IDaoProvider` 违反 Nop IoC 硬规则「@Inject 字段不能 private」（AGENTS.md + nop-backend-dev skill 反模式表），Fix 候选正确。R3.4（plan `2026-07-31-1023-3`）新增的 config-gated checker 经 `@Inject` 字段注入 IDaoProvider，setter `setDaoProvider` 并存——移除 `private` 修正为包级可见（对齐全仓 9+ 处 `@Inject IDaoProvider daoProvider;` 范式），setter 保持不变。Fix 后 checker 复跑 R5=0。 |
| R2a（BizModel daoFor(ErpMd*)） | 37 | 38 | **baseline-raise** | **38** | +1 唯一新站点=`module-finance/.../entity/ErpFinReconciliationBizModel.java:460` `daoProvider().daoFor(ErpMdSubject.class)`（finance→master-data 跨域只读：AR/AP 核销读取 master-data 核算科目）。git diff 证伪另两候选文件（ErpB2bAsnBizModel:267 / ErpFinBudgetCommitmentBizModel:122,131）为 pre-existing（锚点已存在，仅行号偏移）。合法跨域只读聚合。 |
| R2b（BizModel daoFor(Erp*)） | 315 | 325 | **baseline-raise** | **325** | 净 +10（+11 新增 −1 移除）。per-site 分类（git diff `*BizModel.java` 锚点..HEAD）：新增站点绝大多数为**同域内部实体访问**（b2b/ErpB2bAsnBizModel×2−1 / cs/ErpCsTicketBizModel ErpCsTicketAction / fin/ErpFinDashboardBizModel+ErpFinReportBizModel ErpFinAccountingPeriod / fin/ErpFinIntercompanyMatchBizModel ErpFinIntercompanyMatch+ErpFinVoucher+ErpFinVoucherLine / hr/ErpHrShiftAssignmentBizModel / log/ErpLogShipmentBizModel）+ 1 跨域只读（fin→md ErpMdSubject，即 R2a +1）。R2b 计数口径含同域 daoFor（checker 不区分同/跨域），均为合法 IDaoProvider 范式，无 B 类「重构为 I*Biz」候选。 |
| R2c（全生产代码 daoFor() 总量） | 1228 | 1250 | **baseline-raise** | **1250** | 净 +22（=R2b BizModel 增量 + 非 BizModel 生产代码增量）。非 BizModel 新站点 per-site（git diff 锚点..HEAD，排除 test/）：aps/ErpApsSchedulingProcessor(ErpApsCapacityReservation 同域) / ast/ErpAstDepreciationScheduleProcessor(ErpAstAsset 同域) / common/ErpOrgIsolationQueryTransformer(generic daoFor(clazz)，R1.29 org 隔离查询转换器 config-gated) / fin/ErpFinBudgetScenarioProcessor(ErpFinAccountingPeriodStatus+ErpFinAccountingPeriod 同域×2) / fin/ErpFinDeferredPostingRetryHelper(ErpFinPostingException 同域) / fin/ErpFinAccountingPeriodProcessor(ErpFinPostingException 同域 + ErpInvLandedCost 跨域只读[期间结账清理] + ErpAstDepreciationSchedule 跨域只读[期间结账折旧清理]) / inv/StandardCostingStrategy(ErpInvStockMoveLine 同域) / inv/StockMoveBookkeeper(ErpInvStockBalance 同域×2) / mnt/ScheduleDueGenerator(ErpMntVisit 同域) / mfg/MrpReleaseService(ErpMfgMrpPlanLine 同域) / pur/PaymentSettler(ErpPurInvoiceLine 同域)。跨域站点（fin→inv/ast 期间结账清理只读）经 owner doc `processor-extension-pattern.md` + `data-dependency-matrix.md` 背书，无 B 类候选。 |
| R12c（共享内核 import AcctSchemaResolver） | 38 | 40 | **baseline-raise** | **40** | +2 新 import（git diff 锚点..HEAD）：`module-finance/.../dashboard/ErpFinDashboardBizModel.java` + `module-finance/.../report/ErpFinReportBizModel.java`（finance 看板/报表消费共享内核 `AcctSchemaResolver` 做账套感知聚合）。R12 已裁决基线 69/66/38 为共享内核代价（`shared-kernel-extraction-decision.md` 背书分支 (b) 显式共享内核），新增消费方为合法跨域引用。 |

**Fix 实施**：`ErpRoleDataAuthChecker.java:30-31` `private IDaoProvider daoProvider` → 包级可见（移除 `private`），`mvn compile -pl module-common-service -am` BUILD SUCCESS，checker 复跑 R5=0。

**BASELINE 块更新**：仅 R2a/R2b/R2c/R12c 裁决性上调（R5 因 Fix 不动=0）。checker 复跑全 19 规则 actual ≤ baseline（零裸漂移）。本裁决纪律对齐 `1057-1`/`1057-2`/`0823-1` 先例（基线漂移须经独立计划裁决 + per-site 证据 + 显式更新 BASELINE 块）。逐站点 file:line + git 提交时间线核实见 `docs/plans/2026-07-31-1705-2-v1-v2-full-build-test-and-compliance-baseline-adjudication.md` §Phase 2 Evidence。

## R6.8 MR6 后 compliance 基线集中裁决注记（plan 2026-08-01-0656-1）

`2026-08-01-0656-1`（R6.8 全量验证 + 完成判据核验 + compliance 基线集中裁决）处理 R6.1–R6.7（256 个 D-mutation per-mutation 拆分）累积的 compliance 漂移。漂移源全部来自已经独立草案审查 + 结束审计的 R6.x 计划，其生产代码变更已经审计验证为合法 per-mutation 架构。

**R8 checker 三次校准**（actual 248 → **0**，baseline 42 → **0**）：MR6 的 256 per-mutation D-mutation Processor 是 self-contained（`process()` + protected step，不 `extends Abstract*Processor`），经 BizModel `@Inject` 路由（非 xbiz），R8 原始语义「领域 Processor 缺少 xbiz 接线」不覆盖任何「被其他生产代码消费」的 Processor——本项目接线路径统一为 BizModel Java 直接调用（`processor-extension-pattern.md §"Processor → BizModel 接线"`），xbiz 非 Processor 契约层。校准=Decision 方案 (a)：R8 循环内排除 (1) `module-common-service`（一次校准 1057-1）+ `extends Abstract*Processor`（二次校准 1057-2）(2) 域级 `abstract` 基类（R6.7 `Abstract*Processor`，content-based `abstract class` 检测）(3) 被任何其他生产 `.java` 文件引用的 Processor（构建「被 ≥2 个不同生产文件引用」白名单 = 自身定义 + ≥1 消费方）。校准后 R8 = 真孤儿 Processor（无任何消费方），2026-08-01 实测全域 **0 孤儿**。**残留风险**：(a) 自引用孤儿（仅在自身文件出现，`uniq -c=1`）仍计——正确；(b) 仅被 test 引用的 Processor 计孤儿——正确（test-only=未真正接线）；(c) 未来若需精确 BizModel 可达性分析（含跨 Processor 传递消费），开独立 successor 升级。校准实施位置：`nop-compliance-checker.sh` R8 段（新增 `consumed_processors` 白名单构建 + abstract 检测 + 白名单 membership 跳过）。**R8 baseline 裁决性下调 42→0**：反映校准后真实语义（孤儿 Processor），非基线放水。

**R2c 基线裁决性上调**（1250 → **1380**，+130）：漂移源 = R6.1–R6.7 新增 256 个 per-mutation Processor 各实现抽象基类/编排骨架的 `dao()` 方法 `return daoProvider.daoFor(<EntityClass>.class)` 一行（对齐 1057-2 +149 先例——每 per-mutation Processor 是独立 IoC bean，须独立实现 dao() 而非共享静态辅助；`daoProvider.daoFor(<EntityClass>)` 是 Nop 平台读取托管实体 DAO 的标准方式，非业务跨域编排）。实仓 293 个生产文件含 `daoProvider.daoFor(`。逐站点证据：每个 MR6 per-mutation Processor 文件均含 `daoProvider.daoFor(<EntityClass>.class)` 调用（R6.1-R6.7 拆分产出）。R2c 后续治理方向（AbstractProcessor 泛型重构消除 dao() 契约产物）维持 1057-2 裁决选 (c) 接受基线上调，(a)/(b) 为 successor 候选（Non-Goal，本 plan 不动）。

**R2d 基线裁决性上调**（28 → **32**，+4）：漂移源 = MR6 per-mutation Processor 读取 master-data 实体（跨域只读聚合）。32 个 Processor/Dispatcher/Engine `daoFor(ErpMd*)` 站点全部为合法跨域只读：ErpMdSubject（GL 过账科目解析：assets 9 PostingDispatcher + finance 5 + purchase 2 + sales 1 + projects 1 + AbstractErpFinReconciliation 1）/ ErpMdMaterial（成本：mfg MrpEngine 2 + SimulationMrpEngine 3 + SubcontractOrder/WorkOrder Processor 2 + b2b AsnCreateReceive 1）/ ErpMdCurrency（汇率：fin NotesReceivable 1 + md RefreshRates 1）/ ErpMdEmployee（报销过账：fin EmployeeAdvance/ExpenseClaim Dispatcher 2）/ ErpMdAcctSchema（账套：mnt 2 PostingDispatcher）。无 B 类「应重构为 I*Biz」候选（跨域只读聚合，经 owner doc `processor-extension-pattern.md` + `data-dependency-matrix.md` 背书）。

**R1d/R2a/R2b 改善回写基线**（17→**14** / 38→**34** / 325→**240**）：R6.x 重构将 BizModel `findAllByQuery`/`daoFor(ErpMd*)`/跨域 `daoFor(Erp*)` 大量下移到 Processor（per-mutation Processor 持有原本在 BizModel 的跨域只读聚合），checker 实测下降。门控方向为单向收紧（下降自动 PASS），回写基线反映真实代码计数（鼓励）。

逐站点 file:line + 漂移分类 + Decision 理由见 `docs/plans/2026-08-01-0656-1-r6-8-mr6-full-verification-completion-criteria.md` §Phase 1 Evidence。checker 复跑全 19 规则 actual ≤ baseline（R8=0≤0 / R2c=1380≤1380 / R2d=32≤32 / R1d=14≤14 / R2a=34≤34 / R2b=240≤240），exit 0，CI green。

## post-R6.8 compliance 基线漂移裁决注记（plan 2026-08-02-0651-1）

`2026-08-02-0651-1`（post-R6.8 compliance 基线漂移裁决，CI red fix）处理 R6.9 per-mutation Processor 补拆（plan `2026-08-01-0803-1`，MR6 closure successor）引入的 compliance 漂移。漂移源全部来自已经独立草案审查 + 结束审计的 R6.9 计划（MR6 已 CLOSED），其生产代码变更已经审计验证为合法 per-mutation 架构。

**漂移源对账**（`git diff 252a6a387..HEAD -- '*.java' ':!*/src/test/*'`，锚定 R6.8 基线 commit `252a6a387`）：

| 文件 | 角色 | ADD | RM | 净 | 源 commit |
|------|------|-----|----|----|-----------|
| `ErpFinBudgetScenarioRollForwardProcessor`（fin，新） | per-mutation Processor | 5 | 0 | +5 | `fb5e7d5c3` |
| `ErpFinBudgetScenarioCarryForwardProcessor`（fin，新） | per-mutation Processor | 9 | 0 | +9 | `fb5e7d5c3` |
| `ErpFinBudgetScenarioProcessor`（fin，facade） | 旧 monolithic Processor | 0 | 14 | −14 | `fb5e7d5c3`（rollForward/carryForward 内联逻辑下移到 2 新 Processor） |
| `ErpInvCostingReclosePeriodCostsProcessor`（inv，新） | per-mutation Processor | 11 | 0 | +11 | `99c42f6da` |
| `ErpInvCostingBizModel`（inv） | BizModel | 0 | 11 | −11 | `99c42f6da`（reclosePeriodCosts D-mutation 下移到 Processor） |
| `ErpMdSupplierApprovalSuspendByPartnerProcessor`（md，新） | per-mutation Processor | 2 | 0 | +2 | `6aaaf6bcb` |
| **合计** | | **27** | **25** | **+2** | 与 checker R2c delta (+1382−1380=+2) 精确吻合 |

**基线裁决**（逐项合法性分类，源计划 `2026-08-01-0803-1` 已审计）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2d | 32 | **34** | 34 | **baseline-raise**（+2） | +2 net-new 站点全部 = `ErpMdSupplierApprovalSuspendByPartnerProcessor:47,74`（2 处 `daoFor(ErpMdSupplierApproval.class)`），**同域** master-data Processor 读取本域实体（非跨域违规——R2d 原始语义关注的是跨域 Processor 读 master-data，本处为 master-data 域内）。类 javadoc 明示 daoProvider 选择理由：BizModel 已 `@Inject` 本 Processor，反向注入 `IErpMdSupplierApprovalBiz` 形成循环依赖；按 partnerId+status 的批量查询非 FK 导航，无 ORM `<to-one>` getter 可替代；Processor 非 CrudBizModel 子类故无 `dao()` 方法。比 R6.8 接受的 32 处跨域 ErpMd* 站点更轻（同域 vs 跨域）。 |
| R2c | 1380 | **1382** | 1382 | **baseline-raise**（+2 净） | R2c +2 净 **全部来自** 上表 R2d 的 2 处同域 daoFor（master-data SupplierApproval）。finance BudgetScenario（+14/−14=0 净）与 inventory InvCosting（+11/−11=0 净）为完美平衡的 per-mutation 拆分迁移，0 净贡献。对齐 R6.8 +130 / 1057-2 +149 先例：per-mutation Processor `daoProvider.daoFor(<EntityClass>)` 是 Nop 平台读取托管实体 DAO 的标准方式，非业务跨域编排。无 B 类「应重构为 I*Biz」候选。 |
| R2b | 240 | **229** | 229 | **改善回写**（−11） | 11 处 `daoFor(ErpInv*)` 从 `ErpInvCostingBizModel` 下移到 `ErpInvCostingReclosePeriodCostsProcessor`（全部 ErpInv* 同域库存实体：StockLedger×3/StockBalance×2/StockMove/StockMoveLine/CostLayer×4）。BizModel→Processor 下移是 per-mutation 架构的合法重构（对齐 R6.8 产生 R2b 325→240 的 −85 改善先例）。门控方向为单向收紧（下降自动 PASS），回写基线反映真实代码计数（鼓励）。 |

**为何 R6.9 closure 未捕获**：各 R6.9 closure 日志记录「compliance exit 0」指的是 checker 脚本退出码——checker 是**纯 reporter**（`compliance-baseline.md §回归门控规则` + `compliance.yml` 注释：option b「gate 逻辑在 CI / checker 保持纯 reporter」），脚本退出码恒为 0，不反映 actual vs baseline。真正的门控判定在 `compliance.yml` `Enforce baseline gate` step（python 解析 actual > baseline => `sys.exit(1)`）。R6.9 closure 误把脚本退出码当作门控通过，致 R2c/R2d +2 漂移累积——即 `project-context.md §已知失败模式（Compliance 基线漂移）` 描述的复发模式。本裁决是闭合回路复跑 checker 后的集中裁决，对齐 3 先例（`1057-1` / `1705-2` / `0656-1`）。

**0 处 Fix 候选**：R2d +2 同域站点无 ORM `<to-one>` getter 可替代（非 FK 导航）+ 无 I*Biz 可注入（循环依赖），全部裁定 baseline-raise。本裁决纯基线对账 + 文档更新，零生产代码变更（Non-Goals 守约）。

逐站点 file:line + 漂移分类 + Decision 理由（含替代方案否决理由 + 残留风险）见 `docs/plans/2026-08-02-0651-1-compliance-baseline-drift-adjudication.md` §Phase 1 Evidence。checker 复跑全 19 规则 actual ≤ updated baseline（R2b=229≤229 / R2c=1382≤1382 / R2d=34≤34，其余=基线），exit 0，CI green 恢复。

## R2c/R10 同步注记（plan 2026-08-07-1932-3，RC-R1.2 银行对账自动红冲调度接线）

`2026-08-07-1932-3`（RC-R1.2，P1-RC-005）新增 batch helper `ErpFinBankReconAutoReverseHelper`（`module-finance/erp-fin-service/.../bankrecon/`），引入 **R2c +1 / R10 +1**，全部为同一既有文档化 pattern 类：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2c | 1382 | **1383** | 1383 | **baseline-raise**（+1） | `ErpFinBankReconAutoReverseHelper.findCandidates:114` 1 处 `daoProvider.daoFor(ErpFinBankReconciliation.class)` ——batch helper 非 BizModel/Processor，无法经 I*Biz 注入扫描查询（IBiz 接口为 force-lazy-property 惰性注入，helper 于容器启动后懒创建时该属性不注入）；直接对齐既有同型 batch helper `ErpFinDeferredPostingRetryHelper`（R2c 基线已含其 4 处 daoFor）。无 ORM `<to-one>` getter 可替代（候选筛选按 docStatus+reconciliationDate 非 FK 导航）。 |
| R10 | 6 | **7** | 7 | **baseline-raise**（+1） | `ErpFinBankReconAutoReverseHelper.reverseOne:93` 1 处 REQUIRES_NEW——单条红冲独立事务 + try/catch WARN 失败隔离（CLOSED 期间候选逐条隔离不中断批次），对齐 R10 校准注记文档化的合法失败隔离事务边界（`ErpFinDeferredPostingRetryHelper` 同型站点已在基线 6 处内）。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=229 / R2c=1383 / R2d=34 / R3=5 / R10=7 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 恢复。独立结束审计按本注记 per-site 证据复核。

## R2c 基线上调注记（plan 2026-08-08-2219-3，RC-R1.18/19 sales 退货成本策略 + 守卫族）

`2026-08-08-2219-3`（RC-R1.18 退货成本策略 config 化 + RC-R1.19 已核销发票/期间 CLOSED pre-approve 守卫族）在 `module-sales/erp-sal-service` 新增 4 处生产代码 `daoFor()`，引入 **R2c +4**。逐站点均已核验「无法经 I*Biz 注入替代」的技术约束（含一次 I*Biz 改造实验证实 objMeta 限制），全部为既有文档化 pattern 的合法同型新增：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2c | 1388 | **1392** | 1392 | **baseline-raise**（+4） | 4 处逐站点见下（均经核验无法以 ICrudBiz.findList 替代）：(1) `ReturnCostStrategyResolver.findAvgCost:78` `daoFor(ErpInvStockBalance)` ——cross-domain（sal→inv）current 成本策略；dispatcher `computeTotalCost` 运行于 Facade `IErpFinVoucherBiz.post` 的 REQUIRES_NEW 隔离事务、`IServiceContext` 不可达（`ICrudBiz.findList` 需 context 入参不可用）；对齐既有跨模块 DAO 直读先例 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider,...)`（`SalInvoice/SalReceiptPostingDispatcher` 已在基线，resolver javadoc 显式登记该先例）。(2) `ErpSalReturnProcessor.findPeriodByDate:349` `daoFor(ErpFinAccountingPeriod)` ——cross-domain（sal→fin）期间 CLOSED 守卫；`ICrudBiz.findList` 经 BizModel 管道强制 objMeta filterable-field 校验，`startDate/endDate/orgId` 非 objMeta-filterable → `nop.err.biz.prop-not-support-filter-op`（I*Biz 改造实验致 `TestErpSalReturnCostAndGuards` 12/12 回归失败已实测验证），须 raw DAO `findAllByQuery` 绕过 objMeta；finance `resolveOpenPeriod:509` 同域 daoFor 先例。(3) `ErpSalReturnProcessor.findInvoiceIdsByDeliveryLines:361` `daoFor(ErpSalInvoiceLine)` ——same-domain（sal）发票行反查；对齐 `ReturnRefundOrchestrator:80`「`daoFor` 处理同模块实体」文档化先例。(4) `ErpSalReturnProcessor.findApprovedInvoices:374` `daoFor(ErpSalInvoice)` ——same-domain（sal）已审核发票查询；对齐 `ReturnRefundOrchestrator:70` 同模块 daoFor 先例。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R2c=1392≤1392，其余 = 基线），CI green 保持。该裁决对齐 `2026-08-07-1932-3`（RC-R1.2）batch helper R2c baseline-raise 先例（同样因技术约束无法经 I*Biz 注入）。

## R10 基线上调注记（plan 2026-08-14-1815-2，RC-R1.23 crm 线索评分 SCHEDULED 触发器接线）

`2026-08-14-1815-2`（RC-R1.23，P1-RC-035）新增 batch helper `ErpCrmLeadScoringRecalcHelper`（`module-crm/erp-crm-service/.../job/`），引入 **R10 +1**，为既有文档化 pattern 类的合法同型新增：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R10 | 7 | **8** | 8 | **baseline-raise**（+1） | `ErpCrmLeadScoringRecalcHelper.recalculateOne` 1 处 REQUIRES_NEW——单条评分独立事务 + try/catch WARN 失败隔离（单线索失败不阻断批次，对齐 L2「单线索失败隔离」声明），镜像 RC-R1.2 `ErpFinBankReconAutoReverseHelper`（R10 基线已含其 1 处 REQUIRES_NEW）同型站点——batch chunk 事务本身不提供 per-item 隔离（`BatchTaskBuilder.buildChunkProcessor` + `InvokerBatchConsumer` 整 chunk 单事务），per-item 隔离须由 REQUIRES_NEW helper 承载。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=229 / R2c=1392 / R2d=34 / R3=5 / R10=8 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## R10 基线上调注记（plan 2026-08-14-2304-3，RC-R1.27 projects 损益汇总调度接线）

`2026-08-14-2304-3`（RC-R1.27，P1-RC-053）新增 batch helper `ErpPrjProjectPnlCalcHelper`（`module-projects/erp-prj-service/.../job/`），引入 **R10 +1**，为既有文档化 pattern 类的合法同型新增：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R10 | 8 | **9** | 9 | **baseline-raise**（+1） | `ErpPrjProjectPnlCalcHelper.recalculateOne` 1 处 REQUIRES_NEW——单条损益汇总独立事务 + try/catch WARN 失败隔离（单项目失败不阻断批次），镜像 RC-R1.23 `ErpCrmLeadScoringRecalcHelper`（R10 基线已含其 1 处 REQUIRES_NEW）同型站点——batch chunk 事务本身不提供 per-item 隔离（`BatchTaskBuilder.buildChunkProcessor` + `InvokerBatchConsumer` 整 chunk 单事务），per-item 隔离须由 REQUIRES_NEW helper 承载。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=230 / R2c=1393 / R2d=34 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## R2b/R2c 基线上调注记（plan 2026-08-14-1815-3，RC-R1.24 crm UTM 归因族）

`2026-08-14-1815-3`（RC-R1.24，P1-RC-037 + P1-RC-038）新增 `ErpCrmReportBizModel.resolveCampaignNames`（`module-crm/erp-crm-service/.../report/`），引入 **R2b +1 / R2c +1**，为既有文档化 pattern 类的合法同型新增（plan Phase 2 显式规定镜像 `resolveStageNames:246-255` 范式）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2b | 229 | **230** | 230 | **baseline-raise**（+1） | `ErpCrmReportBizModel.resolveCampaignNames` 1 处 `daoProvider.daoFor(ErpCrmCampaign.class)`——归因报表数据集 campaignId → campaignName 同域只读解析，镜像同文件既有 `resolveStageNames`（`daoFor(ErpCrmStage.class)`，基线已含）同型站点；报表聚合行非实体，无法经 ORM to-one 导航，合法 IDaoProvider 只读范式，无 B 类「重构为 I*Biz」候选。 |
| R2c | 1392 | **1393** | 1393 | **baseline-raise**（+1） | 同上 per-site 证据（R2b 增量的 R2c 子集）。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=230 / R2c=1393 / R2d=34 / R3=5 / R10=8 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## R2c 基线上调注记（plan 2026-08-15-0320-2，RC-R1.29 master-data supplier 价格解析 SPI 实现）

`2026-08-15-0320-2`（RC-R1.29，P1-RC-063）新增 `ErpPurSupplierPriceResolver`（`module-purchase/erp-pur-service/.../support/`，实现 `IErpMdSupplierPriceResolver` SPI），引入 **R2c +1**，为既有文档化 pattern 类的合法同型新增（镜像 `ErpSalCustomerPriceResolver` 同型先例——基线已含其 daoFor）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2c | 1393 | **1394** | 1394 | **baseline-raise**（+1） | `ErpPurSupplierPriceResolver.findCandidates:75` 1 处 `daoProvider.daoFor(ErpPurSupplierPriceList.class)`——SPI 解析器非 BizModel（由 master-data `ErpMdMaterialSkuBizModel` 经 `@Inject @Nullable` 反向类型注入，无 purchase I*Biz 可注入）；supplier+material 组合条件查询非 FK 导航，ORM to-one getter 不可替代；对齐 `ErpSalCustomerPriceResolver`（sales 域 customer 价格清单解析器，R2c 基线已含其 daoFor）同型站点。无 B 类「重构为 I*Biz」候选。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=230 / R2c=1394 / R2d=34 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## R2c 基线上调注记（plan 2026-08-15-0456-3，RC-R1.33 contract 计费族）

`2026-08-15-0456-3`（RC-R1.33，P1-RC-074 + P1-RC-075）新增 2 个 per-mutation Processor（`ErpCtInvoicePlanGenerateByTermProcessor` + `ErpCtConsumptionPeriodSummarizeProcessor`，`module-contract/erp-ct-service/.../processor/`），引入 **R2c +5**，全部为既有文档化 pattern 类的合法同型新增（对齐 1057-2 +149 / R6.8 +130 / R6.9 +2 / R1.29 +1 先例——per-mutation Processor 的 `daoProvider.daoFor(<EntityClass>)` 是 Nop 平台读取托管实体 DAO 的标准方式，非业务跨域编排）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2c | 1394 | **1399** | 1399 | **baseline-raise**（+5） | (1) `ErpCtInvoicePlanGenerateByTermProcessor:55` `dao().daoFor(ErpCtContract.class)`（合同 ACTIVE 守卫直查）+ `:112` `planDao()=daoFor(ErpCtInvoicePlan)` + `:116` `contractLineDao()=daoFor(ErpCtContractLine)`（行归属校验直查）——同域实体直查，对齐 triggerInvoice/triggerDuePlans Processor 既有 `daoProvider.daoFor` 范式（行归属判定非 FK 导航，ORM to-one getter 不可替代）；(2) `ErpCtConsumptionPeriodSummarizeProcessor:204` `daoFor(ErpCtConsumptionLine)` + `:208` `daoFor(ErpCtContractLine)`——期间汇总直查（对齐 triggerDuePlans 绕过 XMeta 查询算子白名单的既有注记范式）。无 B 类「重构为 I*Biz」候选（per-mutation Processor 编排骨架 + 内部批量查询语义）。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=230 / R2c=1399 / R2d=34 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## R2b/R2c/R2d 基线上调注记（plan 2026-08-15-2119-3，RC-R1.48 mfg 物料预留写路径）

`2026-08-15-2119-3`（RC-R1.48，P1-RC-008 修复：mfg 工单审核触发/释放/领料扣减物料预留写路径）新增跨域预留写路径生产代码，引入 **R2b +3 / R2c +6 / R2d +1**，全部为既有文档化 pattern 类的合法同型新增（对齐 1057-2 / R6.8 / R1.29 / R1.33 先例——预留写路径跨域编排 + 库存余额写经 `StockMoveBookkeeper` 封装的既定范式）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2b | 230 | **233** | 233 | **baseline-raise**（+3） | `ErpInvReservationBizModel` 3 处 `daoProvider().daoFor(...)`（`ErpInvReservation`/`ErpInvReservationLine`/`ErpInvStockBalance`，:422/:426/:430）——预留写接口（createReservation/releaseReservation/consumeReservation）编排头/行/余额三实体持久化，行级 min(需求,可用) 预留量 + 余额增量经 `StockMoveBookkeeper.updateBalanceWithRetry` 乐观锁（对齐 `StockMoveBookkeeper` 自身 daoProvider 直查范式 + `ErpInvStockMoveProcessor` 余额写路径）；CrudBizModel 管道（findList/saveEntity）不承载行级余额编排，ORM to-one getter 不可替代。无 B 类「重构为 I*Biz」候选（本接口即 I*Biz 契约实现）。 |
| R2c | 1399 | **1405** | 1405 | **baseline-raise**（+6） | R2b 增量 3 处（子集）+ `ErpMfgWorkOrderProcessor:604` 1 处 `daoFor(ErpMdMaterial)`（预留行 uom 回退查物料主数据——同文件 :358 既有同型站点先例，getEntityById 主键导航非 FK 弱引用可替代）+ `ErpMfgMaterialIssueConfirmProcessor` 2 处 `daoFor(ErpInvReservation)`/`daoFor(ErpInvReservationLine)`（:332/:342，领料消耗前预留剩余量只读聚合——对齐 `KitAvailabilityChecker` 跨域只读直查先例）。 |
| R2d | 34 | **35** | 35 | **baseline-raise**（+1） | `ErpMfgWorkOrderProcessor:604` `daoFor(ErpMdMaterial.class).getEntityById`（R2c 增量子集，同文件 :358 既有同型站点先例——工单 Processor 物料主数据 uom 回退读）。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=233 / R2c=1405 / R2d=35 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## R2c 基线上调注记（plan 2026-08-16-0424-2，RC-R1.52/53/54 assets 折旧补提/处置凭证/闲置状态机族）

`2026-08-16-0424-2`（RC-R1.52 方式B 补提 + RC-R1.53 处置 1606 腿 + RC-R1.54 IDLE 状态机）新增 2 个 per-mutation Processor（`ErpAstDepreciationScheduleCatchUpDepreciationProcessor` + `ErpAstAssetSuspendResumeProcessor`，`module-assets/erp-ast-service/.../processor/`），引入 **R2c +3**，全部为既有文档化 pattern 类的合法同型新增（对齐 1057-2 +149 / R6.8 +130 / R1.33 +5 / R1.48 +6 先例——per-mutation Processor 的 `daoProvider.daoFor(<EntityClass>)` 是 Nop 平台读取托管实体 DAO 的标准方式，非业务跨域编排）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2c | 1405 | **1408** | 1408 | **baseline-raise**（+3） | (1) `ErpAstDepreciationScheduleCatchUpDepreciationProcessor:81` `daoFor(ErpAstDepreciationSchedule.class)`——补提逐漏提期落行 + 幂等跳过查询的编排骨架（对齐 per-mutation Processor `dao()` 契约先例）；(2) `ErpAstDepreciationScheduleCatchUpDepreciationProcessor:135` `daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset)`——补提累计折旧/净值回写资产卡片，镜像同文件族 `ExecuteDepreciationProcessor:105` 既有同型站点；(3) `ErpAstAssetSuspendResumeProcessor:71` `assetDao()=daoFor(ErpAstAsset.class)`——suspend/resume 状态机 mutation Processor 的实体存取 helper（1 站点承载 require/save 多调用），镜像 `ErpAstDisposalProcessor.disposalDao()` 既有范式。无 B 类「重构为 I*Biz」候选（per-mutation Processor 编排骨架 + 非 FK 导航直查）。R2a/R2b/R2d 不变（新站点全部为 assets 同域实体，无 BizModel 新增站点，无 ErpMd* 站点）。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=233 / R2c=1408 / R2d=35 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## R2c 基线上调注记（plan 2026-08-16-0904-1，RC-R1.49 mfg BOM 快照）

`2026-08-16-0904-1`（RC-R1.49 mfg BOM 快照，P1-RC-009 UC-MFG-10 断言④⑤⑥）新增 BOM 快照写路径（`snapshotBomOnSubmit`）+ 快照源工艺行读取 + 快照工艺费率解析，引入 **R2c +5**，全部为既有文档化 pattern 类的合法同型新增（对齐 1057-2 +149 / R6.8 +130 / R1.33 +5 / R1.48 +6 先例）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2c | 1408 | **1413** | 1413 | **baseline-raise**（+5） | (1) `ErpMfgWorkOrderProcessor.snapshotBomOnSubmit:522` `daoFor(ErpMfgWorkOrderBomSnapshot.class)`——快照头 newEntity/save（提交复制工序唯一写入口，@BizMutation 同事务）；(2) `ErpMfgWorkOrderProcessor.snapshotBomOnSubmit:530` `daoFor(ErpMfgWorkOrderBomLineSnapshot.class)`——快照子件行 newEntity/save（镜像 BOM 行逐行复制）；(3) `ErpMfgWorkOrderProcessor.snapshotBomOnSubmit:545` `daoFor(ErpMfgWorkOrderBomOperationSnapshot.class)`——快照工艺行 newEntity/save（镜像 BOM 工艺行逐行复制）；(4) `BomExpander.loadOperations:220` `daoFor(ErpMfgBomOperation.class)`——快照复制源工艺行读取（public 化对齐既有 `loadLines` 同型只读 helper 范式，`module-manufacturing/erp-mfg-service/.../bom/BomExpander.java`）；(5) `ProductionVarianceCalculator.deriveStandardLaborRate:424` `daoFor(ErpMfgWorkcenter.class).getEntityById(wcId)`——快照工艺行工作中心费率解析（工作中心费率为主数据实时读，与实时路径同口径同工具调用，`costing/ProductionVarianceCalculator.java` 快照分支）。无 B 类「重构为 I*Biz」候选（BOM 快照实体为 mfg 同域新实体无 I*Biz 面需求 + 服务助手直查既有范式 + 非 FK 导航直查）。读侧快照访问（KitAvailabilityChecker.loadBomSnapshot / ProductionVarianceCalculator.snapshotOperationsOrNull / BomExpander.explodeFromSnapshot）全部经 ORM to-many 关系 getter（`wo.getBomSnapshot()`/`snap.getOperations()`/`snap.getLines()`）零新增 daoFor 站点。R2a/R2b/R2d 不变（新站点全部为 mfg 同域实体，无 BizModel 新增站点，无 ErpMd* 站点）。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=233 / R2c=1413 / R2d=35 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## R2c 基线上调注记（plan 2026-08-16-0904-2，RC-R1.51 sales 退货换货）

`2026-08-16-0904-2`（RC-R1.51 sales 退货换货，P1-RC-025 UC-SAL-06 四断言）新增 per-mutation Processor `ErpSalReturnGenerateExchangeDeliveryProcessor`（`module-sales/erp-sal-service/.../processor/`），引入 **R2c +2**，全部为既有文档化 pattern 类的合法同型新增（对齐 R1.18/R1.19 `ErpSalReturnProcessor` same-domain daoFor 先例）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2c | 1413 | **1415** | 1415 | **baseline-raise**（+2） | (1) `ErpSalReturnGenerateExchangeDeliveryProcessor.returnDao():382` `daoFor(ErpSalReturn.class)`——per-mutation Processor 编排骨架的退货单加载/同事务回写（exchangeDeliveryId 双写），对齐 1057-2 per-mutation Processor `dao()` 契约先例 + `ReturnRefundOrchestrator` 同模块 daoFor 范式；(2) `ErpSalReturnGenerateExchangeDeliveryProcessor.loadReturnLines:377` `daoFor(ErpSalReturnLine.class)`——换货行缺省复制退货行的按 returnId 批量查询（非 FK 导航，无 ORM to-one getter 可替代），对齐 `ErpSalReturnProcessor.loadLines`/`ReturnRefundOrchestrator` 同模块先例。两站点均为同域（sal）实体，无 BizModel 新增站点（R2a/R2b 不变）、无 ErpMd* 站点（R2d 不变）。跨实体创建（ErpSalDelivery/ErpSalInvoice）全经 I*Biz 注入（deliveryBiz/invoiceBiz）零新增 daoFor 面。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=233 / R2c=1415 / R2d=35 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## R2c 基线上调注记（plan 2026-08-16-0904-3，RC-R1.56 inventory 盘点完成自动差异移动单）

`2026-08-16-0904-3`（RC-R1.56 / P1-MA2-062，UC-INV-07：completeTake 自动生成盘盈/盘亏差异移动单）新增 per-mutation Processor `ErpInvStockTakeCompleteTakeProcessor`（`module-inventory/erp-inv-service/.../processor/`，经 BizModel `ErpInvStockTakeBizModel` @Inject 路由 + beans.xml 注册），引入 **R2c +5**，全部为既有文档化 pattern 类的合法同型新增（对齐 1057-2 per-mutation Processor `dao()` 契约先例 + R1.48 +6 / R1.51 +2 先例——per-mutation Processor 的 `daoProvider.daoFor(<EntityClass>)` 是 Nop 平台读取托管实体 DAO 的标准方式，非业务跨域编排）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2c | 1415 | **1420** | 1420 | **baseline-raise**（+5） | (1) `ErpInvStockTakeCompleteTakeProcessor.loadLines:107` `daoFor(ErpInvStockTakeLine.class)`——盘点行子表按 takeId 批量查询（非 FK 导航，无 ORM to-one getter 可替代）；(2) `lineDao():129` `daoFor(ErpInvStockTakeLine.class)`——D1 差异回填 updateEntity helper（编排骨架）；(3) `moveDao():218` `daoFor(ErpInvStockMove.class)`——失败行孤立 DRAFT 移动单定位/补偿删除 helper；(4) `moveLineDao():233` `daoFor(ErpInvStockMoveLine.class)`——移动单行加载/删除 helper；(5) `takeDao():248` `daoFor(ErpInvStockTake.class)`——终态 DONE 回写 helper。五站点均为 inv **同域**实体直查（盘点聚合头/行 + 关联移动单头/行），无跨域站点（跨域生成经 `IErpInvStockMoveBiz` Facade 注入，零新增 daoFor 跨域面）。无 BizModel 新增站点（R2a/R2b 不变，`ErpInvStockTakeBizModel` 仅 @Inject 委托无 daoFor）、无 ErpMd* 站点（R2d 不变）、Processor 已被 BizModel 消费（R8 零孤儿）。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=233 / R2c=1420 / R2d=35 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核（plan 0904-3 Closure Audit Evidence ⑦）。

## R2b/R2c 基线上调注记（plan 2026-08-16-1634-1，RC-R1.57 crm 团队分配 ROUND_ROBIN/LOAD_BALANCED）

`2026-08-16-1634-1`（RC-R1.57 / P1-RC-036，UC-CRM-11：ErpCrmTeamMember 新实体 + TerritoryAssignmentEngine 挑人 + config 门控）在 `ErpCrmLeadBizModel` 新增 2 处 daoFor helper（`TeamMemberResolver` 实现，D4 选项 A——BizModel 提供 dao 实现、引擎保持纯函数式），引入 **R2b +2 / R2c +2**，全部为既有文档化 pattern 类的合法同型新增（对齐 `ReturnRefundOrchestrator`「`daoFor` 处理同模块实体」先例 + R2b 口径含同域 BizModel daoFor 的既有注记）：

| 规则 | 旧基线 | 新基线 | actual | 裁决 | 合法性分类 |
|------|--------|--------|--------|------|-----------|
| R2b | 233 | **235** | 235 | **baseline-raise**（+2） | 两站点均为 crm **同域** BizModel daoFor：(1) `ErpCrmLeadBizModel.teamMemberDao():377` `daoFor(ErpCrmTeamMember.class)`——resolver 成员列表查询（新实体无 I*Biz 业务面需求，territory.md Non-Goal「成员维护走标准 CRUD 生成」；teamId 批量查询非 FK 导航，无 ORM to-one getter 可替代；TerritoryAssignmentEngine 纯函数式无 dao 依赖，解析职责由 BizModel 承载）；(2) `ErpCrmLeadBizModel.leadDao():381` `daoFor(ErpCrmLead.class)`——resolver 上次分配（createTime desc limit 1）/活跃线索计数（teamId+ownerId+docStatus 组合条件）查询——本 BizModel 即 ErpCrmLead 自身 BizModel（自引用无法注入 `IErpCrmLeadBiz`），raw DAO 批量查询为实体自身 BizModel 的标准直查范式（对齐同文件既有 `assignmentRuleDao():385` 先例）。两处均含 E3 自检注释说明 daoFor 理由。 |
| R2c | 1420 | **1422** | 1422 | **baseline-raise**（+2） | 同上 2 站点（R2b 增量的 R2c 子集），无跨域站点（ErpCrmTeamMember/ErpCrmLead 均 crm 同域）、无 ErpMd* 站点（R2a/R2d 不变）、引擎 bean 无孤儿注册（R8=0）。 |

checker 复跑全 19 规则 actual ≤ updated baseline（R1d=14 / R2a=34 / R2b=235 / R2c=1422 / R2d=35 / R3=5 / R10=9 / R12a=69 / R12b=66 / R12c=40，其余 = 基线），CI green 保持。独立结束审计按本注记 per-site 证据复核。

## BASELINE (machine-readable)

> CI gate 解析本块。格式：`RULE=value`，每行一条。仅含可计数规则（R9 除外）。修改本块须经独立计划裁决（见上文"调高基线的唯一途径"）。

```yaml
R1a: 0
R1b: 0
R1c: 0
R1d: 14
R2a: 34
R2b: 235
R2c: 1422
R2d: 35
R3: 5
R4: 0
R5: 0
R6: 2
R7: 0
R8: 0
R10: 9
R11: 0
R12a: 69
R12b: 66
R12c: 40
```

## 关联

- Checker: `docs/audits/nop-compliance-checker.sh`
- CI 门控: `.github/workflows/compliance.yml`
- 源 finding: `docs/audits/2026-07-23-0000-architecture-governance-review.md` F8 + 闭包前必须项 #4
- 计划: `docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md`
- daoFor 分类（为何 R2c 基线合理）: `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`
- F15 i18n checker（**已接入 CI**——`.github/workflows/compliance.yml` i18n job，R3.7，基线见上文 §F15 i18n 基线注记）: `docs/audits/i18n-coverage-checker.sh` + 审计 `docs/audits/2026-07-29-0749-arm-ma4-i18n-coverage.md` + 计划 `docs/plans/2026-07-29-0749-3-audit-remediation-ma4-i18n-coverage.md`（CI 接入经 A7.4 裁决，已由 R3.7 plan 2026-07-31-1439-3 落地）
