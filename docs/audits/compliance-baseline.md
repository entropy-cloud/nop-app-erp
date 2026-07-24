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
| R1d | dao().findAllByQuery (BizModel) | 🔴 高 | 23 |
| R2a | BizModel daoFor(ErpMd*) | 🔴 高 | 37 |
| R2b | BizModel daoFor(Erp*) 跨域 | 🔴 高 | 314 |
| R2c | 全生产代码 daoFor() 总量 | 🔴 高 | 1065 |
| R2d | Processor daoFor(ErpMd*) | 🔴 高 | 27 |
| R3 | new Erp*() 构造实体 | 🟡 中 | 19 |
| R4 | extends RuntimeException | 🟢 低 | 0 |
| R5 | @Inject private | 🟡 中 | 0 |
| R6 | @Transactional in BizModel | 🟢 低 | 7 |
| R7 | System.currentTimeMillis() | 🟢 低 | 2 |
| R8 | Processor 无 xbiz 接线 | 🔴 高 | 42 |
| R10 | REQUIRES_NEW 事务 | 🟡 中 | 51 |
| R11 | Processor 重复状态判断方法 | 🟡 中 | 0 |
| R12a | 共享内核 import ErpFinBusinessType | 🟡 中 | 69 |
| R12b | 共享内核 import PostingEvent | 🟡 中 | 66 |
| R12c | 共享内核 import AcctSchemaResolver | 🟡 中 | 38 |

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

## BASELINE (machine-readable)

> CI gate 解析本块。格式：`RULE=value`，每行一条。仅含可计数规则（R9 除外）。修改本块须经独立计划裁决（见上文"调高基线的唯一途径"）。

```yaml
R1a: 0
R1b: 0
R1c: 0
R1d: 23
R2a: 37
R2b: 314
R2c: 1065
R2d: 27
R3: 19
R4: 0
R5: 0
R6: 7
R7: 2
R8: 42
R10: 51
R11: 0
R12a: 69
R12b: 66
R12c: 38
```

## 关联

- Checker: `docs/audits/nop-compliance-checker.sh`
- CI 门控: `.github/workflows/compliance.yml`
- 源 finding: `docs/audits/2026-07-23-0000-architecture-governance-review.md` F8 + 闭包前必须项 #4
- 计划: `docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md`
- daoFor 分类（为何 R2c 基线合理）: `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`
