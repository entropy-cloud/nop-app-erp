# MA4 manufacturing 代码质量审计 — 工单与报工 / BOM 与工艺路线（A4.2a — 代码实现质量）

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 代码实现质量维度）
> 域/功能模块：manufacturing / 工单与报工 + BOM 与工艺路线（A4.2a S 级拆分 1/2）
> 审计 plan：`docs/plans/2026-07-29-0024-1-audit-remediation-ma4-mfg-work-order-bom-code-quality.md`
> 来源 finding（运行时复核）：P1-MA1-001 / P1-MA1-022 / P1-MA2-035 / P1-MA2-086 / P1-MA3-040 / P1-MA3-041 / P1-MA3-044 / P1-MA3-048 + ManufacturingIssuePostingDispatcher tryPost 吞异常悬挂同型根因
> Skill：`docs/skills/code-quality-audit-prompt.md`（7 重点领域 + 严重性指南 P0-P3）
> 审计日期：2026-07-29
> 审计者：主代理（独立子代理已完成草案审查，见 plan §Draft Review Record；结束审计由独立子代理执行，见 plan §Closure）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏路径） | **0** | 无即时通道修复（无活跃数据破坏路径；齐套校验只读不写预留、并发扣减由 inventory 乐观锁 P0-MA2-020 UK 守护） |
| **P1**（新登记） | **3** | P1-MA4-007（完工编排层差异计算/过账失败吞咽致业财悬挂）/ P1-MA4-008（工单/BOM 链路跨域 daoFor 绕 I\*Biz，同 P1-MA1-022 根因投影）/ P1-MA4-009（工单/领料/BOM 链路测试有效性不足） |
| **P2**（watch-only） | **1** | P2-MA4-004（可维护性热点合并：reload-after-generateMove 脆弱范式 / AcctSchemaResolver vs IErpMdAcctSchemaBiz 内部不一致 / markIssuePosted 依赖脏跟踪 / WorkOrderProcessor 体量） |
| MA1/MA2/MA3 finding 运行时复核 | 9 项 | 全部「如登记」无升级；其中 ManufacturingIssuePostingDispatcher tryPost 吞咽复核时**发现相邻代码路径新缺陷** P1-MA4-007（完工编排层差异吞咽 + ProductionVarianceDispatcher 同型，详见 §6） |

**整体裁决**：**FAIL（有代码实现质量缺陷）**——零 P0（无活跃数据破坏路径；齐套校验为只读状态指示，实际扣减由开工后领料出库移动单 DONE + ErpInvStockBalance 乐观锁/UK 守护）。工单/BOM 链路的核心实现质量在**编排健壮性（ErpMfgWorkOrderProcessor 步骤化 protected 方法 + 审批三轴 + 10 态状态机全守卫）/ BOM 展开正确性（BomExpander DFS 环检测 + 深度上限 + path 回溯 + phantom 展开）/ 算术正确性（报工 laborCost=durationMins/60×hourlyRate / 领料 materialCost=流水 totalCost.abs() 聚合 / 完工 unitCost=total/completed 重算）/ 错误处理规范化（全 NopException+ErrorCode erp.err.mfg.* + 状态迁移上下文齐全）**四面扎实，但**失败恢复闭环**存在 1 项 P1 缺陷（完工触发差异计算/过账失败 catch(Exception)→LOG.error 吞咽致 GL 缺生产差异凭证无告警，与 P1-MA4-004 同型根因）、**架构边界**存在 1 项 P1 缺陷（5 站点跨域 daoFor 绕 I\*Biz，同 P1-MA1-022 根因在 mfg 工单/BOM 代码投影）、**测试有效性**存在 1 项 P1 缺陷（业财一体异常路径零覆盖 + 完工入库 GL voucher 行级多币种断言缺失，致 dispatcher 吞咽悬挂对测试不可见）。

**裁决分布**：P1-MA4-007 → **MR1**（业财悬挂闭环，与 P1-MA2-032/P1-MA4-001/P1-MA4-004 同型根因；config-gated 降低触发面但不消除）/ P1-MA4-008 → **MR1**（同 P1-MA1-022 一并裁决，**不重复计入 MR2**）/ P1-MA4-009 → **MR2**（测试质量，MA4「测试有效性」维度，与 A5.1 互补不重叠）。

---

## 1. 审计范围与方法覆盖矩阵

### 1.1 审计对象（实仓逐项核实，29 源文件中核心组件抽样）

| 组件 | 文件 | 行号 | 审计状态 |
|---|---|---|---|
| 工单编排 Processor | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderProcessor.java` | submitForApproval:72-78 / approve:87-93 / checkAvailability:109-116 / start:118-123 / reportCompletion:173-241 / generateCompletionMove:352-389 / recomputeTotals:413-420 / validateTransitionFor*:245-340 / isInspectionGated:402-411 | ✅ |
| 工单 Facade | `.../service/entity/ErpMfgWorkOrderBizModel.java` | 全文 106 行（@BizMutation 委托 Processor） | ✅ |
| 审批轴 6 Processor | `ErpMfgWorkOrder{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor` + `ErpMfgScheduleToJobCardProcessor` | （extends AbstractApproveProcessor，主方法委托 monolithic Processor，抽象方法 no-op "not reached"） | ✅ |
| 作业卡编排 Processor | `.../processor/ErpMfgJobCardProcessor.java` | startJob:37-43 / recordWork:45-71（laborCost=duration/60×rate）/ applyLaborCostToWorkOrder:158-170 / 状态机守卫 | ✅ |
| 领料单 BizModel | `.../service/entity/ErpMfgMaterialIssueBizModel.java` | confirm:88-128 / reverseConfirm:132-172 / aggregateIssueMaterialCost:199-208 / validateCanReverse:249-256 | ✅ |
| 领料出库移动单构造器 | `.../service/entity/MaterialIssueStockMoveBuilder.java` | build:29-47（OUTGOING + IErpMdAcctSchemaBiz 经 I\*Biz）/ buildLines:57-71 | ✅ |
| 领料过账 Dispatcher | `.../posting/ManufacturingIssuePostingDispatcher.java` | dispatchIfApplicable:79-114（try/catch 吞咽）/ buildEvent:116-152 / markIssuePosted:154-160 / reverse:202-208 | ✅ |
| 领料过账 Provider | `.../posting/ManufacturingIssueAcctDocProvider.java` | createFacts:60-88（Dr WIP / Cr 1401 按物料分列）/ accountKey | ✅ |
| 过账执行器 | `.../posting/MfgPostingExecutor.java` | postEvent:27-33 / reverse:35-41（经 IErpFinVoucherBiz Facade） | ✅ |
| 差异过账 Dispatcher | `.../posting/ProductionVarianceDispatcher.java` | dispatchIfApplicable:70-118（try/catch 吞咽）/ reverseIfExists:133-154 | ✅ |
| 齐套校验器 | `.../workorder/KitAvailabilityChecker.java` | check:62-89（只读，不写预留）/ loadAvailableByMaterial:102-114（daoFor ErpInvStockBalance） | ✅ |
| BOM 展开器 | `.../bom/BomExpander.java` | explode:77-83 / expandLines:85-119（环检测 path.contains + 深度上限 + path 回溯 finally）/ phantom:106-108 | ✅ |
| ErrorCode 集中表 | `.../service/ErpMfgErrors.java`（30+ ErrorCode.define erp.err.mfg.*） | 全文 287 行 | ✅ |
| 常量表 | `.../service/ErpMfgConstants.java`（10 态工单 / 8 态作业卡 / config 项） | 全文 314 行 | ✅ |
| 测试套件 | `TestErpMfgWorkOrderEndToEnd`（3 @Test）/ `TestErpMfgMaterialIssue`（2 @Test）/ `TestErpMfgMaterialIssueReversal` / `TestErpMfgWorkOrderStateMachine` / `TestErpMfgBomExplosion` / `TestErpMfgScheduleToJobCard` | — | ✅ |

### 1.2 Skill 维度覆盖（`code-quality-audit-prompt.md` 7 重点领域）

| # | 维度 | 裁决 | 发现 |
|---|------|------|------|
| 1 | 架构和边界完整性 | ⚠️(P1) | 工单/BOM 链路 5 站点跨域 `daoFor(ErpMd*/ErpInv*)` 绕 I\*Biz（P1-MA4-008，同 P1-MA1-022 根因）；领料出库经 IErpInvStockMoveBiz Facade ✅ / 完工入库过账经 IErpFinVoucherBiz Facade（MfgPostingExecutor）✅ / 生成物零手编 ✅ / 审批轴 6 Processor 孤儿影子契约如 P1-MA3-048 登记 |
| 2 | 核心实现正确性 | ⚠️(P1) | 完工触发差异计算/过账 catch(Exception)→LOG.error 吞咽（P1-MA4-007，与 P1-MA4-004 同型）；齐套校验只读 TOCTOU 为设计选择（实际扣减由库存乐观锁守护）✅ / BomExpander 环检测+深度上限+path 回溯正确 ✅ / 报工 laborCost 算术正确 ✅ / 幂等（领料 DONE 重复 confirm 空操作 + billHeadCode 防重复过账）✅ |
| 3 | 类型和契约质量 | ✅ | cost 回写全 BigDecimal + HALF_UP + nz() null 守卫；审批轴 Processor 参数返回契约一致（String id 跨域对齐 AbstractApproveProcessor）；VoucherFact 单 amount 如 P1-MA2-002/009 登记（mfg 侧 PostingEvent.exchangeRate=ONE 单币种投影） |
| 4 | 错误处理和操作安全 | ✅(P3) | 全链路 NopException+ErrorCode（erp.err.mfg.*）+ 状态迁移上下文齐全（workOrderCode/currentStatus/expectedStatus）；readBoolConfig 守护配置解析异常；仅 ManufacturingIssueAcctDocProvider.readAmount NumberFormatException→ZERO 静默降级为 P3（数据损坏时凭证金额错误，但 billData 由 dispatcher 装配 BigDecimal，触发面窄） |
| 5 | 测试有效性 | ⚠️(P1) | 黄金路径覆盖良好（E2E 断言行级 materialCost/laborCost/totalCost/unitCost 数值 + 余额扣减 + 幂等 + 质检门控负向 + 红冲负向守卫）；但业财异常路径零覆盖（dispatcher 过账失败悬挂 posted=false / 多币种完工入库 GL voucher 行级 amountSource≠amountFunctional 断言缺失），致 P1-MA4-007 吞咽对测试不可见（P1-MA4-009） |
| 6 | 可维护性和未来变更风险 | ⚠️(P2) | ErpMfgWorkOrderProcessor 513 行（步骤化良好但体量增长需关注）；reportCompletion reload-after-generateMove 脆弱范式（REQUIRES_NEW 过账后实体 evict → reload+reapply）/ AcctSchemaResolver(daoFor) vs MaterialIssueStockMoveBuilder(IErpMdAcctSchemaBiz) 同类操作两种模式内部不一致 / markIssuePosted 依赖 ORM 脏跟踪不显式 updateEntity（P2-MA4-004） |
| 7 | 自动化和防护覆盖 | ⚠️(P2) | compliance checker R2d 覆盖 *Processor/*Dispatcher 但未覆盖 *Checker/*Builder/*Expander（KitAvailabilityChecker/BomExpander daoFor 跨域无静态守卫）；业财一体异常路径无测试门控（归 P1-MA4-009） |

---

## 2. 重点领域逐项审查结果

### 2.1 领域「架构和边界完整性」— ⚠️(P1)

**核查项**：领料库存移动是否经 IErpInvStockMoveBiz Facade / 完工入库过账是否经 IErpFinVoucherBiz Facade / 报工 cost 回写是否合规 / BOM 展开读 WorkCenter 是否合规 / P1-MA1-022（aps 读 ErpMfgBom）运行时状态。

**证据**：
- **跨域 daoFor(ErpMd\*/ErpInv\*) 绕 I\*Biz（5 站点 + 1 内部不一致）**：grep `daoFor(Erp(Md|Inv|...)` 于工单/BOM 链路命中 5 处 mfg→master-data/inventory 只读直访：
  - `ErpMfgWorkOrderProcessor.java:364` → `daoProvider.daoFor(ErpMdMaterial.class).getEntityById(productId)`（generateCompletionMove 解析产出物料 UoM 只读）
  - `ManufacturingIssuePostingDispatcher.java:163` → `daoFor(ErpInvStockMove.class)`（findIssueMove 反查领料出库移动单只读）
  - `ManufacturingIssuePostingDispatcher.java:174` → `daoFor(ErpInvStockLedger.class)`（loadLedgers 读出库流水成本只读）
  - `ManufacturingIssuePostingDispatcher.java:188` + `ProductionVarianceDispatcher.java:208` → 经 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)` 内部 `daoFor(ErpMdAcctSchema.class)`（账套解析只读）
  - `KitAvailabilityChecker.java:107` → `daoFor(ErpInvStockBalance.class)`（齐套校验读可用量只读）
- **内部不一致**：`MaterialIssueStockMoveBuilder.java:53` 经 `IErpMdAcctSchemaBiz.findFirstByOrg`（I\*Biz 管道）解析账套，但 `ManufacturingIssuePostingDispatcher.java:188` / `ProductionVarianceDispatcher.java:208` 改用 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider)`（daoFor 直访）——同类账套解析两种模式，可维护性缺陷。
- **领料出库经 Facade**：`ErpMfgMaterialIssueBizModel.confirm:110` → `stockMoveBiz.generateMove`（IErpInvStockMoveBiz Facade）✅；`reverseConfirm:155` → `stockMoveBiz.reverse` Facade ✅。
- **完工入库过账经 Facade**：`MfgPostingExecutor.postEvent:32` / `reverse:40` → `IErpFinVoucherBiz.post/reverse` Facade（跨域失败隔离 REQUIRES_NEW，processor-extension-pattern.md 硬规则 2）✅。
- **生成物零手编**：工单/BOM 链路全部为手写非 `_gen` 文件；未发现 `_` 前缀文件手编 ✅。
- **审批轴 6 Processor 孤儿影子契约**：`ErpMfgWorkOrderApproveProcessor` 等 extends AbstractApproveProcessor 但 `getApproveStatus/setApproveStatus/setApprovedBy/...` 全部 `return null` / no-op + "not reached" 注释，真实逻辑在 monolithic `ErpMfgWorkOrderProcessor`——如 **P1-MA3-048 登记**（孤儿 Processor bean 携带 String 影子契约）。无新代码层缺陷。
- **P1-MA1-022 运行时状态**：aps `ErpApsSchedulingProcessor` 跨域只读 daoFor(ErpMfgBom/ErpMfgBomOperation) 如 MA1 登记（aps→mfg 读侧投影），本审计复核确认 mfg 侧代码对 aps 读访问无感知、无新缺陷。

**裁决**：⚠️(P1) — 5 站点跨域 daoFor 绕 I\*Biz 是 **P1-MA1-022 根因在 mfg 工单/BOM 代码的投影**（P1-MA1-022 原列举 aps→mfg 读 + pur/sal/ast/inv/mnt/prj/qa/drp/aps 9 域，未显式枚举 mfg→md/inv 读侧）。登记 P1-MA4-008 交叉引用 P1-MA1-022，MR1 一并裁决（不重复计入 MR2）。

### 2.2 领域「核心实现正确性」— ⚠️(P1)

**核查项**：WorkOrderProcessor 编排事务边界健壮性 / checkAvailability 齐套校验 TOCTOU / 领料 MaterialIssueStockMoveBuilder 库存移动构造正确性 / ManufacturingIssuePostingDispatcher tryPost 异常吞咽与悬挂 / 报工 recordWork laborCost 算术 / BomExpander 多级展开递归终止与成环检测 / 完工入库 totalCost/unitCost 重算。

**证据**：
- **【新缺陷 P1-MA4-007】完工编排层差异计算/过账失败吞咽致业财悬挂**：`ErpMfgWorkOrderProcessor.reportCompletion:227-239` 在 `willFinish && isVarianceAutoCalcEnabled()` 时包裹 `productionVarianceDispatcher.reverseIfExists` + `productionVarianceCalculator.deleteByWorkOrder` + `calculateVariances` + `productionVarianceDispatcher.dispatchIfApplicable` 四步于单一 `catch (Exception e) { LOG.error("...失败（不阻断完工，可经手动 calculateVariances 重算）", ...); }`——过宽捕获（含 NopException 配置错误如 `ERR_VARIANCE_NO_STANDARD_COST` / `ERR_ROLLUP_BASE_COST_MISSING` / Provider 固定抛错），仅日志不阻断、不进 finance 异常工作台（ErpFinPostingException）、不派发 IErpSysNotificationBiz 告警。失败后紧接工单已 COMPLETED（:220-223），GL 缺 PRODUCTION_VARIANCE 凭证而无人工处置入口。相邻的 `ProductionVarianceDispatcher.dispatchIfApplicable:111-117` 自身亦 `catch (Exception) → LOG.warn/error` 吞咽过账失败保持 posted=false。与 **P1-MA4-004**（期间结账编排 catch(Exception)→LOG.warn 吞咽折旧/成本重算失败）是**同型根因**（编排层跨域/异步步骤异常吞咽致业财悬挂），但 A4.1b 审 finance 期间 Processor 集成层、本审 **mfg 完工触发层** + mfg 侧差异 dispatcher——MA2/A4.1b 均未覆盖此层。**config-gated**（`erp-mfg.variance-auto-calc-enabled` 默认 false）降低触发面，但当 config=true（业务要求完工自动算差异）+ 永久性失败（标准成本未发布/卷算 base cost 缺失）时 GL 缺差异凭证 + 标准成本与实际成本差异不入账，CLOSED 后不可自动补救。非 P0：需 config 开启 + 永久性失败前置 + LOG.error 可见性 + 手动重算入口存在。
- **齐套校验 TOCTOU**（`KitAvailabilityChecker.check:62-89`）：`checkAvailability`（NOT_STARTED→STOCK_RESERVED/STOCK_PARTIAL）仅读 `ErpInvStockBalance.availableQuantity` 置状态，**不写库存预留记录**（state-machine.md 实现偏离补注已记录「齐套校验只读不写预留」）。故两工单并发 checkAvailability 见同一可用量后均 start，TOCTOU 存在但**为设计选择**——实际扣减由开工后领料出库移动单 DONE 完成，并发扣减由 `ErpInvStockBalance` versionProp 乐观锁 + P0-MA2-020 `UK_INV_STOCK_BALANCE_NATURAL` 守护（A2.17 已确认）。无新缺陷。
- **领料 MaterialIssueStockMoveBuilder 构造正确性**（`build:29-47`）：领料出库用 `MOVE_TYPE_OUTGOING_ISSUE("OUTGOING")`（非 MANUFACTURING），对齐库存域 `StockMoveBookkeeper.bookCompletion` 方向语义（OUTGOING 扣减 / MANUFACTURING 入库，state-machine.md 实现偏离补注已记录）；sourceWarehouseId=发料仓、destWarehouseId=null（物料离开仓库）✅；幂等键 `(ERP_MFG_ISSUE, issue.code)` 防重复过账 ✅。
- **ManufacturingIssuePostingDispatcher tryPost 吞咽**（`dispatchIfApplicable:107-113`）：`catch (Exception) → LOG.warn/error` 吞咽过账失败保持 posted=false——如 **MA2 已知 finding 登记**（ManufacturingIssuePostingDispatcher tryPost 吞异常悬挂同型根因，与 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa/projects/maintenance/logistics 同型）。运行时确认 drift 活跃，无升级；本审计**复核时发现相邻代码路径新缺陷** P1-MA4-007（完工编排层差异吞咽 + ProductionVarianceDispatcher 同型，§6）。
- **报工 recordWork laborCost 算术**（`ErpMfgJobCardProcessor.recordWork:56`）：`laborCost = duration.divide(SIXTY, 4, HALF_UP).multiply(rate)`——durationMins/60 × hourlyRate，HALF_UP + scale 4 ✅；`applyLaborCostToWorkOrder:167` 增量累加 `wo.laborCost += laborCostDelta` 后 `recomputeTotals` 重算 total/unit ✅。
- **BomExpander 多级展开**（`expandLines:85-119`）：DFS `path.contains(product)` 环检测 → 抛 `ERR_BOM_CYCLE`；`level > maxDepth`（config `erp-mfg.bom-max-depth` 默认 15）→ 抛 `ERR_BOM_MAX_DEPTH_EXCEEDED`；`finally { path.remove(product); }` 路径回溯正确（兄弟节点不互相阻塞）；phantom（bomType=PHANTOM）展开其子件并入当前层级不产生独立节点 ✅；有效用量 `line.quantity × scale`（scale=requestedQty/bom.qty，divide 守护 b.signum()==0）✅。递归终止 + 成环检测均正确。
- **完工入库 totalCost/unitCost 重算**（`recomputeTotals:413-420`）：`total = material+labor+overhead+subcontract`；`unitCost = completed.signum()!=0 ? total/completed(4,HALF_UP) : ZERO`——completed=0 时 unitCost=0 守护除零 ✅。`generateCompletionMove:383` 用 `wo.getUnitCost()`（累计平均成本）作为入库移动单行 unitCost ✅。
- **reportCompletion reload-after-generateMove 范式**（`:213-217`）：完工入库移动单经 `stockMoveBiz.generateMove`（内部 GL 过账 REQUIRES_NEW），成功过账后当前 session 实体可能被 evict，故 `wo = workOrderDao().getEntityById(workOrderId)` 重新加载并 reapply `completedQuantity`/`recomputeTotals`——范式正确但脆弱（依赖对跨域 Facade 事务边界的隐式假设），P2-MA4-004。

**裁决**：⚠️(P1) — P1-MA4-007（完工编排层差异吞咽致业财悬挂）是新发现的核心实现正确性 + 错误处理闭环缺陷；齐套 TOCTOU / dispatcher tryPost 吞咽如既有 MA2 登记，运行时确认无升级。

### 2.3 领域「类型和契约质量」— ✅

**核查项**：审批轴 6 Processor 参数返回契约一致性 / 领料/报工 cost 回写 BigDecimal 类型安全 / 工单多币种四件套（P1-MA1-001 propId 缺失运行时影响）。

**证据**：
- **cost 回写 BigDecimal 类型安全**：`ErpMfgJobCardProcessor` / `ErpMfgMaterialIssueBizModel` / `ErpMfgWorkOrderProcessor` 全部 `nz()` null→ZERO 守卫 + `BigDecimal.divide(...,HALF_UP)` + `compareTo` 而非 `equals`（避免 1.0 vs 1.00 陷阱）✅。`aggregateIssueMaterialCost:207` `.abs()` 处理出库流水负值 ✅。
- **审批轴 6 Processor 契约一致性**：全部 `String id` 入参（对齐 AbstractApproveProcessor 基类），返回 entity；monolithic `ErpMfgWorkOrderProcessor` 主方法签名一致（submitForApproval/approve/reject/reverseApprove/withdrawApproval 均 `(String id, IServiceContext)`）✅。
- **工单多币种四件套**：`ErpMfgWorkOrder`/`ErpMfgMaterialIssue` 多币种 7 列 propId 缺失如 **P1-MA1-001 登记**（MR1 codegen 增量再生补全）。运行时影响有限：`ManufacturingIssuePostingDispatcher.buildEvent:125` `event.setExchangeRate(BigDecimal.ONE)`（单币种假设），多币种场景PostingEvent未折算——但 mfg 侧 PostingEvent.exchangeRate=ONE 是 P1-MA2-002/009（VoucherFact 单 amount）在 mfg 过账投影，同根因 MR1 一并裁决，无新类型缺陷。

**裁决**：✅ — cost 回写 BigDecimal 类型安全扎实；多币种 propId/exchangeRate 如 MA1/MA2 登记，运行时确认；无新类型/契约缺陷。

### 2.4 领域「错误处理和操作安全」— ✅(P3)

**核查项**：工单/BOM 链路异常是否全扩展 NopException + ErrorCode（erp.err.mfg.*）/ 齐套校验失败/质检门控拦截/报工超量错误传播 / 超产 config 承诺缺失（P1-MA3-041）。

**证据**：
- **全链路 NopException + ErrorCode**：grep `extends RuntimeException` 于工单/BOM 链路 = **0 命中**（R4 合规）。`ErpMfgErrors` 集中定义 30+ `ErrorCode.define("erp.err.mfg.*")`；所有 throw 点使用 NopException + .param() 携带上下文（workOrderCode/currentStatus/expectedStatus/completedQty/plannedQty/bomId/materialId/path/depth）✅。
- **齐套校验失败传播**：`KitAvailabilityChecker.check` 缺料 → `KitAvailabilityResult.partial()`（附 shortage 明细）→ `checkAvailability` 置 STOCK_PARTIAL；强制开工 `validateTransitionForStart:333` config=false 时抛 `ERR_PARTIAL_KIT_START_FORBIDDEN` ✅。
- **质检门控拦截传播**：`reportCompletion:188-200` `willFinish && isInspectionGated` → 抛 `ERR_INSPECTION_REQUIRED`（config-gated `erp-mfg.inspection-gate-enabled` 默认 false）✅。
- **报工超量传播**：`reportCompletion:181-185` `newCompleted > planned` → 抛 `ERR_OVER_REPORT`（附 completedQty/plannedQty）✅。
- **超产 config 承诺缺失**：owner doc `state-machine.md §4` 声明「报工数量超过工单数量 → 拒绝（除非配置允许超产）」，但代码 `reportCompletion:181` **无条件拒绝**（无 `allow-over-report` config）——如 **P1-MA3-041 登记**（可配置超产无 config，MR2 文档类）。运行时确认 drift 活跃，无新代码层缺陷。
- **ManufacturingIssueAcctDocProvider.readAmount:118-130**：`NumberFormatException → BigDecimal.ZERO` 静默降级——billData 由 dispatcher 装配 BigDecimal（`buildEvent:141` `lineCost` 为 `ledger.getTotalCost().abs()`），触发面窄（仅手工篡改 billData 时），但静默降级致凭证金额错误时无告警。P3（归 P2-MA4-004 可维护性）。

**裁决**：✅(P3) — 错误处理规范化扎实（全 NopException + 上下文齐全 + 齐套/质检/超量错误传播完整）；仅 readAmount 静默降级 P3。

### 2.5 领域「测试有效性」— ⚠️(P1)

**核查项**：异常路径覆盖（齐套不足/质检门控拦截/报工超量/领料超量/过账失败悬挂）+ 断言强度（仅断言 status 还是校验 cost 回写数值/库存移动/凭证行）。

**证据**（`TestErpMfgWorkOrderEndToEnd` 3 @Test + `TestErpMfgMaterialIssue` 2 @Test + `TestErpMfgMaterialIssueReversal` + `TestErpMfgWorkOrderStateMachine` + `TestErpMfgBomExplosion` + `TestErpMfgScheduleToJobCard`）：
- **黄金路径断言强度良好**：`TestErpMfgWorkOrderEndToEnd.testEndToEndIssueReportCompletion:118-121` 断言行级数值（materialCost=10 / laborCost=30 / totalCost=40 / unitCost=40），非仅 status ✅；`TestErpMfgMaterialIssue.testConfirmIssuesOutAndAggregatesMaterialCost:99-107` 断言余额扣减（10-2=8）+ WorkOrderLine.actualQuantity 回写 + materialCost 聚合（2×5=10）✅；`testConfirmIdempotent:127-130` 断言幂等（同一移动单 + 仅扣一次）✅。
- **负向路径部分覆盖**：质检门控拦截（`testInspectionGateBlocksCompletionWhenEnabled:167-171` 断言 ERR_INSPECTION_REQUIRED + 工单保持 IN_PROCESS）✅；领料红冲未过账守卫（`TestErpMfgMaterialIssueReversal` 场景 2 断言 ERR_MATERIAL_ISSUE_NOT_POSTED）✅；JobCard/WorkOrder 状态机非法迁移在 `TestErpMfgWorkOrderStateMachine` 覆盖。
- **【新缺陷 P1-MA4-009】业财一体异常路径零覆盖 + 完工入库 GL voucher 行级断言缺失**：
  - (a) **过账失败悬挂零覆盖**：`ManufacturingIssuePostingDispatcher.dispatchIfApplicable` / `ProductionVarianceDispatcher.dispatchIfApplicable` 的 try/catch 吞咽路径（posted=false 悬挂，P1-MA4-007 / MA2-known 同型）无任何测试触发——无 mock 过账失败 + 断言 posted=false + 工单/领料单终态不受影响的测试。
  - (b) **完工入库 GL voucher 行级多币种断言缺失**：`TestErpMfgWorkOrderEndToEnd` 断言产成品余额（P 入库 1 件）+ cost 数值，但**未校验完工入库生成的 MANUFACTURING_RECEIPT 凭证行级** `amountSource/amountFunctional/exchangeRate/debitAmount/creditAmount`（凭证经 stockMoveBiz.generateMove 内部 InvAcctDocProvider 生成）——多币种 bug（P1-MA3-039 amountSource=amountFunctional 同型）对 mfg 完工入库测试不可见。所有测试单币种（CURRENCY_ID 固定）。
  - (c) **报工超量 ERR_OVER_REPORT 负向覆盖薄**：`reportCompletion:181` 超量拒绝路径无直接 assertThrows 测试（grep `ERR_OVER_REPORT` 于测试目录仅 state-machine/dashboard 间接命中，非完工超量场景）。
  - (d) **齐套不足 STOCK_PARTIAL 强制开工负向覆盖薄**：`validateTransitionForStart` config=false 拒绝 + config=true 强制开工路径无 E2E 覆盖。

**裁决**：⚠️(P1) — P1-MA4-009（业财异常路径零覆盖 + 完工入库 GL voucher 行级断言缺失，致 dispatcher 吞咽悬挂 + 多币种 bug 对测试不可见）。黄金路径 + 状态机负向覆盖良好，但业财一体异常路径 + 多币种凭证行级存在系统性空洞。

### 2.6 领域「可维护性和未来变更风险」— ⚠️(P2)

**核查项**：WorkOrderProcessor 编排复杂度 / 审批轴 6 Processor 对称性 / BomExpander 递归可维护性。

**证据**：
- **ErpMfgWorkOrderProcessor 513 行**：单类体量在上限，但已步骤化为 protected 单一职责方法（submitForApproval/approve/checkAvailability/start/reportCompletion + validateTransitionFor* / do* / validateBusinessRulesFor* step）——派生覆盖友好，可维护性可接受，但 reportCompletion 单方法 69 行（:173-241）含 5 个职责（校验/质检门控/移动单/基因链/差异触发）持续增长需关注。
- **【P2-MA4-004】可维护性热点合并**：
  - `reportCompletion:213-217` reload-after-generateMove 脆弱范式：跨域 `stockMoveBiz.generateMove` REQUIRES_NEW 过账后假设实体被 evict 故 reload + reapply 字段——依赖对 Facade 事务边界的隐式假设，若库存域 Facade 事务边界变更则此 reload 可能多余或不足。
  - 账套解析内部不一致：`MaterialIssueStockMoveBuilder:53` 用 `IErpMdAcctSchemaBiz.findFirstByOrg`（I\*Biz）vs `ManufacturingIssuePostingDispatcher:188` / `ProductionVarianceDispatcher:208` 用 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider)`（daoFor）——同类操作两种模式（与 A4.1a ErpFinPostingProcessor:595 vs :688 内部不一致同型）。
  - `ManufacturingIssuePostingDispatcher.markIssuePosted:154-160` 依赖 ORM 脏跟踪（`managed.setPosted(true)` 后无显式 `updateEntity`）——Nop 范式下 session-managed 实体 dirty-check 自动 flush 可工作，但与同类 `confirm` 显式 `updateEntity(issue,...)` 风格不一致（reverseConfirm 注释 :168 明示「跨域 reverse 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化」——已知脏跟踪脆弱性却未统一）。
  - `ManufacturingIssueAcctDocProvider.readAmount:127` NumberFormatException→ZERO 静默降级（§2.4）。
- **审批轴 6 Processor 对称性**：Submit/Approve/Reject/ReverseApprove/Withdraw 5 个 Processor 结构对称（extends AbstractApproveProcessor + 委托 monolithic Processor + 抽象方法 no-op）——但全部抽象方法 no-op "not reached" 是 P1-MA3-048 影子契约模式的投影，可维护性风险已登记。
- **BomExpander 递归可维护性**：`expandLines` 递归 + path Set 回溯 + phantom 展开逻辑清晰，maxDepth config 守护爆炸，可维护性良好 ✅。

**裁决**：⚠️(P2) — P2-MA4-004（4 项可维护性热点合并）watch-only，MR2 顺手收敛。

### 2.7 领域「自动化和防护覆盖」— ⚠️(P2)

**核查项**：工单/BOM 链路是否有 compliance checker 规则守护（R8 Processor 无 xbiz / R2 daoFor 跨域）/ 是否有测试门控防止回归。

**证据**：
- **compliance checker R2d 覆盖缺口**：`nop-compliance-checker.sh` R2d 扫描 `*Processor.java`/`*Dispatcher.java`/`*Engine.java`——**未覆盖 `*Checker` / `*Builder` / `*Expander`**。故 `KitAvailabilityChecker:107`（daoFor ErpInvStockBalance）/ `BomExpander`（daoFor ErpMfgBom 同域但模式）/ `MaterialIssueStockMoveBuilder` 的跨域 daoFor **无静态守卫**（MaterialIssueStockMoveBuilder 已用 I\*Biz 合规，但 checker 不验证）。R2d 命中的 *Dispatcher（ManufacturingIssue/ProductionVariance）有覆盖。R8（Processor 无 xbiz）：审批轴 6 Processor 在 R8=42 基线内（xbiz 委托 monolithic Processor，合规）。
- **测试门控缺口**：业财一体异常路径（dispatcher 过账失败悬挂 P1-MA4-007 / MA2-known）无测试门控（P1-MA4-009）；多币种完工入库 GL voucher 行级无回归保护（P1-MA4-009）。

**裁决**：⚠️(P2) — checker R2d 未覆盖 *Checker/*Builder/*Expander + 业财异常路径测试门控缺口，watch-only，MR2 顺手扩展 R2d 文件名模式或补测试门控。

---

## 3. P1 finding 清单（按严重性 + 目标 MR 排序）

### P1-MA4-007 完工编排层差异计算/过账失败吞咽致业财悬挂闭环缺失

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——业财不一致悬挂，但需 config=true + 永久性失败前置 + 非正常路径） |
| 目标 MR | **MR1**（业务正确性：业财悬挂，与 P1-MA4-004 / P1-MA2-032 同型根因；R4.1 可裁决） |
| 文件 / 行 | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderProcessor.java:227-239`（reportCompletion 完工触发差异计算/过账 catch 吞咽）+ `.../posting/ProductionVarianceDispatcher.java:111-117`（dispatchIfApplicable tryPost 吞咽） |
| 缺陷描述 | `reportCompletion:227-239` 在 `willFinish && isVarianceAutoCalcEnabled()` 时包裹 `reverseIfExists`+`deleteByWorkOrder`+`calculateVariances`+`dispatchIfApplicable` 四步于单一 `catch (Exception e) { LOG.error(...); }`——过宽捕获（含 NopException 配置错误 ERR_VARIANCE_NO_STANDARD_COST / ERR_ROLLUP_BASE_COST_MISSING / Provider 固定抛错），仅日志不阻断、不进 ErpFinPostingException 异常工作台、不派发 IErpSysNotificationBiz 告警。失败后工单已 COMPLETED（:220-223），GL 缺 PRODUCTION_VARIANCE 凭证无人工处置入口。相邻 `ProductionVarianceDispatcher.dispatchIfApplicable:111-117` 自身 catch(Exception)→LOG.warn 吞咽过账失败保持 posted=false。 |
| 影响 | config=true（业务要求完工自动算差异）+ 永久性失败（标准成本未发布/卷算 base cost 缺失/Provider 配置错误）→ GL 缺生产差异凭证（标准成本与实际成本差异不入账）+ 无告警 + 无异常工作台记录；CLOSED 后不可自动补救（仅手动 calculateVariances 重算，但无提示）。期末结账前置检查仅扫 finance 异常工作台 PENDING/RETRYING，**不覆盖** mfg 侧差异 dispatcher 内部失败，间接兜底失效。与 P1-MA4-004（期间编排吞咽折旧/成本重算）同型根因（编排层跨域/异步步骤异常吞咽致业财悬挂），但 A4.1b 审 finance 期间 Processor、本审 mfg 完工触发层 + mfg 差异 dispatcher——MA2/A4.1b 未覆盖。 |
| 修复方向 | MR1 裁决——方案 A（推荐）区分"差异未配置"（无 FIRMED 标准成本→ERR_VARIANCE_NO_STANDARD_COST 容错跳过，非阻断）与"配置错误/真实故障"（其他 NopException→阻断完工或进 finance 异常工作台 status=PENDING + IErpSysNotificationBiz 告警）+ catch 收窄为具体异常类型 + owner doc state-machine.md §实现偏离补注「完工触发差异」段标注错误传播分级；方案 B 差异计算/过账失败统一进异常工作台由期末前置检查兜底阻断。config-gated 降低触发面但业财悬挂性质不变。触及会计保护区域，修复须独立 plan-audit + 人工确认。 |

### P1-MA4-008 工单/BOM 链路跨域 daoFor(ErpMd*/ErpInv*) 绕 I*Biz（同 P1-MA1-022 根因在 mfg 工单/BOM 投影）

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——架构边界违规，read-only 跨域直访绕 I\*Biz 管道） |
| 目标 MR | **MR1**（同 P1-MA1-022 一并裁决，**不重复计入 MR2**——同根因 master-data/inventory I\*Biz 补便捷只读方法后迁移） |
| 文件 / 行 | `processor/ErpMfgWorkOrderProcessor.java:364`（daoFor ErpMdMaterial）/ `posting/ManufacturingIssuePostingDispatcher.java:163,174`（daoFor ErpInvStockMove/ErpInvStockLedger）/ `posting/ManufacturingIssuePostingDispatcher.java:188` + `posting/ProductionVarianceDispatcher.java:208`（经 AcctSchemaResolver daoFor ErpMdAcctSchema）/ `workorder/KitAvailabilityChecker.java:107`（daoFor ErpInvStockBalance）（+ 内部不一致 `entity/MaterialIssueStockMoveBuilder.java:53` 用 IErpMdAcctSchemaBiz I\*Biz vs dispatcher 用 AcctSchemaResolver daoFor） |
| 缺陷描述 | 5 站点 mfg→master-data/inventory 只读 `daoFor(ErpMdMaterial/ErpMdAcctSchema/ErpInvStockMove/ErpInvStockLedger/ErpInvStockBalance)` 直访，违反 AGENTS.md「跨实体访问应通过 I\*Biz 接口」+ data-dependency-matrix.md §5.3。与 P1-MA1-022（pur/sal/ast/inv/mnt/prj/qa/drp/aps 9 域 + aps→mfg 读同型）同根因，本批是其在 mfg 工单/BOM 代码的投影（P1-MA1-022 未显式枚举 mfg→md/inv 读侧）。MaterialIssueStockMoveBuilder 同类内用 IErpMdAcctSchemaBiz（I\*Biz）vs ManufacturingIssuePostingDispatcher/ProductionVarianceDispatcher 用 AcctSchemaResolver（daoFor）不一致加剧可维护性风险。 |
| 影响 | 架构边界侵蚀（read-only，无活跃数据破坏）；master-data/inventory 实体变更时 mfg 工单/BOM 直访点不受 I\*Biz 契约保护。 |
| 修复方向 | MR1——同 P1-MA1-022 方案 A（master-data/inventory I\*Biz 补便捷只读方法后迁移 5 站点）或方案 B（永久接受为 Helper 合法模式，登记 posting-exemptions.md）。ManufacturingIssuePostingDispatcher/ProductionVarianceDispatcher 的 AcctSchemaResolver 改用 MaterialIssueStockMoveBuilder 同型 IErpMdAcctSchemaBiz I\*Biz 管道消除内部不一致。**不重复计入 MR2**（同 P1-MA1-022 一并裁决）。 |

### P1-MA4-009 工单/领料/BOM 链路测试有效性不足（业财异常路径零覆盖 + 完工入库 GL voucher 行级断言缺失）

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——测试空洞致既有业财悬挂/多币种 bug 不可见 + 无回归防护） |
| 目标 MR | **MR2**（测试质量，MA4「测试有效性」维度；与 A5.1 测试覆盖深度统计互补不重叠——本项审异常路径+断言强度，A5.1 审覆盖深度数值） |
| 文件 / 行 | `test/.../TestErpMfgWorkOrderEndToEnd.java`（缺完工入库 GL voucher 行级断言 + 多币种）+ 全文件（缺 dispatcher 过账失败悬挂测试）+ `TestErpMfgMaterialIssue.java`（缺过账失败悬挂） |
| 缺陷描述 | (a) 过账失败悬挂零覆盖：ManufacturingIssuePostingDispatcher/ProductionVarianceDispatcher 的 try/catch 吞咽路径（posted=false 悬挂，P1-MA4-007 / MA2-known）无测试触发。(b) 完工入库 GL voucher 行级多币种断言缺失：TestErpMfgWorkOrderEndToEnd 断言产成品余额 + cost 数值，但未校验 MANUFACTURING_RECEIPT 凭证行级 amountSource/amountFunctional/exchangeRate/debitAmount/creditAmount，多币种 bug（P1-MA3-039 同型）对 mfg 完工入库测试不可见（所有测试单币种 CURRENCY_ID 固定）。(c) 报工超量 ERR_OVER_REPORT + (d) 齐套不足 STOCK_PARTIAL 强制开工负向覆盖薄。 |
| 影响 | P1-MA4-007 完工差异吞咽 + dispatcher tryPost 悬挂（MA2-known）+ 多币种完工入库 bug 三类缺陷均无测试门控；未来结构性变更（如差异 dispatcher 异常工作台接入、PostingEvent 多币种折算）无回归保护。 |
| 修复方向 | MR2——补：(1) dispatcher 过账失败悬挂测试（mock IErpFinVoucherBiz.post 抛异常 → 断言 posted=false + 领料单/工单终态不受影响 + LOG 可观测，闭合 P1-MA4-007 测试可见性）；(2) 完工入库多币种 E2E（exchangeRate≠ONE + 凭证行级 amountSource≠amountFunctional 断言，闭合 P1-MA3-039 mfg 投影）；(3) 报工超量 assertThrows ERR_OVER_REPORT；(4) 齐套不足 STOCK_PARTIAL + config 强制开工 E2E。与 A5.1 互补不重叠。 |

---

## 4. P2 finding 清单（watch-only）

| Finding ID | 描述 | 处置 |
|---|---|---|
| `P2-MA4-004` | 可维护性热点合并（4 项）：(a) `ErpMfgWorkOrderProcessor.reportCompletion:213-217` reload-after-generateMove 脆弱范式（依赖跨域 Facade REQUIRES_NEW 后实体 evict 的隐式假设）；(b) 账套解析内部不一致 `MaterialIssueStockMoveBuilder:53`（IErpMdAcctSchemaBiz I\*Biz）vs `ManufacturingIssuePostingDispatcher:188`/`ProductionVarianceDispatcher:208`（AcctSchemaResolver daoFor）；(c) `ManufacturingIssuePostingDispatcher.markIssuePosted:154-160` 依赖 ORM 脏跟踪不显式 updateEntity（与同类 confirm 显式 updateEntity 风格不一致，reverseConfirm 注释已知脏跟踪脆弱性却未统一）；(d) `ManufacturingIssueAcctDocProvider.readAmount:127` NumberFormatException→ZERO 静默降级。 | watch-only，MR2 顺手收敛（与 P1-MA4-007 / P1-MA4-008 修复时一并） |

---

## 5. 与既有 P1 交叉去重

| 本审计 Finding | 既有 Finding | 关系 | 去重裁决 |
|---|---|---|---|
| P1-MA4-007 | P1-MA4-004（期间编排 catch 吞咽折旧/成本重算） + P1-MA2-032（IGNORED 悬挂） + MA2-known ManufacturingIssuePostingDispatcher tryPost 吞咽 | 相邻代码路径同型根因（编排层跨域/异步步骤异常吞咽致业财悬挂），但 A4.1b 审 finance 期间 Processor、本审 mfg 完工触发层 + mfg 差异 dispatcher——**不重叠** | 独立登记 P1-MA4-007（MR1，与 P1-MA4-004 / P1-MA2-032 协同修复） |
| P1-MA4-008 | P1-MA1-022（9 域跨域 daoFor）+ P1-MA4-003（finance posting 投影）+ P1-MA4-006（A4.1b 投影） | **同根因在 mfg 工单/BOM 投影** | 独立登记但**不重复计入 MR2**（MR1 同 P1-MA1-022 一并裁决） |
| P1-MA4-009 | A5.1（todo，测试覆盖深度）+ P1-MA4-002/005（finance 测试有效性） | 互补不重叠——MA4 审异常路径+断言强度，A5.1 审覆盖深度数值；与 finance P1-MA4-002/005 不同域 | 独立登记 P1-MA4-009（MR2） |
| P2-MA4-004 (b) | P1-MA4-003（ErpFinPostingProcessor:595 vs :687 内部不一致） | 子例（同类账套解析内部不一致） | P2 watch-only，MR2 与 P1-MA4-008 协同 |

---

## 6. MA1/MA2/MA3 已知 finding 运行时复核（9 项）

| Finding ID | 运行时状态 | 裁决 |
|---|---|---|
| `P1-MA1-001` | 如 MA1 登记（ErpMfgWorkOrder/ErpMfgMaterialIssue 多币种四件套 7 列 propId 缺失；PostingEvent.exchangeRate=ONE 单币种投影 P1-MA2-002/009） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA1-022` | 如 MA1 登记 + **本审计补充 mfg 工单/BOM 5 站点投影**（P1-MA4-008，含 aps→mfg 读侧原登记复核确认） | **如 owner doc 声明 + 发现 mfg 工单/BOM 投影新站点**（P1-MA4-008） |
| `P1-MA2-035` | 如 MA2 登记（JobCard PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态——ErpMfgJobCardProcessor 8 方法无一 SET 此两态，运行时确认 NONE SET） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-086` | 如 MA2 登记（erp-mfg.jobcard-auto-generate cron 并发重复副作用 + 透明乐观锁降级——WorkOrder 等 mfg 状态机实体声明 versionProp） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA3-040` | 如 MA3 登记（state-machine.md §质检约束声明引用不存在的 INSPECTING 工单状态——实际 config-gated 钩子 reportCompletion 抛 ERR_INSPECTION_REQUIRED 替代） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA3-041` | 如 MA3 登记（state-machine.md 声明可配置超产但 reportCompletion:181 无条件拒绝无 config——运行时确认 drift 活跃） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA3-044` | 如 MA3 登记（README 列 DowntimeEntry + ProductionPlan 实体但 ORM 不存在） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA3-048` | 如 MA3 登记（审批轴 6 Processor extends AbstractApproveProcessor 但抽象方法 no-op "not reached"，真实逻辑在 monolithic ErpMfgWorkOrderProcessor——孤儿 Processor 影子契约） | **如 owner doc 声明，无新代码层缺陷** |
| ManufacturingIssuePostingDispatcher tryPost 吞咽（MA2-known 同型） | 如 MA2 登记（dispatchIfApplicable:107-113 catch(Exception)→LOG.warn 吞咽保持 posted=false）+ **复核时发现相邻代码路径新缺陷**：完工编排层 reportCompletion:227-239 差异吞咽 + ProductionVarianceDispatcher:111-117 同型（P1-MA4-007） | **如 owner doc 声明 + 发现相邻路径新缺陷**（P1-MA4-007） |

---

## 7. 剩余风险与交接

- **P1-MA4-007 修复前**：config=true 时永久性差异计算/过账失败 3 步搁浅于 LOG.error，需运营手工扫工单 COMPLETED 但无 PRODUCTION_VARIANCE 凭证处置（无自动告警）。MR1 修复后闭环。
- **交接 A4.2b**：A4.2b（MRP/质量集成/成本核算/基因追溯代码质量）执行时复核——(1) `ProductionVarianceCalculator` / `CostRollupService` 实现质量（本审计仅审 ProductionVarianceDispatcher 调用点 + 完工触发编排）；(2) `BatchGenealogyWriter`（本审计仅审 writeBatchGenealogy best-effort 调用点 :211）；(3) `MrpEngine` / `DemandAggregator` / `SimulationMrpEngine` 跨域 daoFor（同 P1-MA1-022/P1-MA4-008 根因，A4.2b 范围更广投影）；(4) P1-MA4-008 同型 daoFor 是否在 A4.2b 范围文件复现。
- **交接 A4.6**：mfg view.xml vs 后端契约 drift（如工单/领料/作业卡列表页字段）归 A4.6；本审计不审前端消费。
- **交接 A5.1**：P1-MA4-009 的测试覆盖深度数值统计（mfg 30 测试 / 74 mutation）归 A5.1 系统化；本审计仅审异常路径 + 断言强度。
- **交接 A6.1/A6.2**：工单/BOM 链路权限注解完整性（reportCompletion/close 等敏感动作）归 A6.1/A6.2；本审计不审权限。

## 8. 裁决

**Verdict: FAIL（有代码实现质量缺陷）**——零 P0（无活跃数据破坏路径；齐套校验只读、并发扣减由库存乐观锁/UK 守护）。工单/BOM 链路在**编排健壮性 / BOM 展开正确性 / 算术正确性 / 错误处理规范化**四面扎实，但**失败恢复闭环**（P1-MA4-007 完工差异吞咽致业财悬挂）、**架构边界**（P1-MA4-008 跨域 daoFor 绕 I\*Biz）、**测试有效性**（P1-MA4-009 业财异常路径零覆盖 + GL voucher 行级断言缺失）三项 P1 缺陷需 MR1/MR2 修复。MA1/MA2/MA3 已知 finding 运行时复核 9 项全部「如登记」无升级，其中 ManufacturingIssuePostingDispatcher tryPost 吞咽复核发现相邻代码路径新缺陷 P1-MA4-007。roadmap A4.2a 推进至 done（待独立 closure audit）——manufacturing 代码质量全片终态在 A4.2b（MRP/质量集成）收口。
