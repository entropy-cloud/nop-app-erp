# ck-mfg-subcontract-r3 — manufacturing「委外/批次追溯/差异」mfg-3 五维符合性审计报告（ai-check-r3 M1.7）

> 工作项：M1.7（U08 × 五维 × mfg-3，冻结清单 §4 映射表第 7 行 / §3.3 U08 行；工单/作业卡/领料/预留/完工入库面归 mfg-1（M1.5 已审）；BOM/路由/MRP/仿真/CRP/成本滚算面归 mfg-2（M1.6 同批先落盘）；库存移动消费点（`ErpInvStockMoveReverseProcessor`/`GenerateMoveProcessor` 反向移动消费侧行为）归本格，inv 引擎内部归 U02；posting 引擎内部归 fin-1；common 抽象族归 U20；聚合横切面归 U21，§3.2）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `c63d71bb365668fc3ad25507c2561782297a1410`（短 `c63d71bb3`，姊妹 M1.6 计划落盘提交之后）；脏面 = **2 untracked 计划文件**（本计划 `2026-09-09-0232-2-*` + M1.9 姊妹 `2026-09-09-0232-3-*`，草案产物非生产面）；姊妹在制披露：M1.6（mfg-2）报告已先落盘（`ck-mfg-bom-mrp-r3.md`，T0′ = HEAD `7ad2e426b`），mfg-2 面归并指向该报告；M1.9 为同批未执行计划仅存在于脏面。全部证据引用 T0。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U08 行 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：委外链（`ErpMfgSubcontractOrder` Facade/Processor + 9 per-mutation Processor + `SubcontractPostingDispatcher` + Issue/Receipt/Fee 3 AcctDocProvider + `MfgSubcontractReversalListener` + Document/Approval 双状态机 Bean + 行 BizModel）/批次基因（`BatchGenealogyWriter`/`BatchGenealogyTracer` + `ErpMfgBatchGenealogyBizModel`）/差异链（`ProductionVarianceCalculator` + `ErpMfgCostVarianceBizModel` + `CalculateVariancesProcessor` + `ProductionVarianceDispatcher` + AcctDocProvider）/Forecast 链（`ErpMfgForecast` BizModel 族 + StateMachine）（`module-manufacturing/erp-mfg-{dao,service,web}` src/main）；owner docs `subcontracting.md`（243 行）+ `batch-genealogy.md`（157 行）+ `variance-analysis.md`（123 行）+ `use-cases.md`（269 行）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（mfg-3 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点：②跨实体 I*Biz inv/fin 调用点 ⑧委外双轴状态机 ⑨审批流 + 红冲两段移动反转完整性——F2.7 修复 HEAD 复核、基因写入/追溯口径、差异公式、Forecast 状态机）+ 维度⑮断言抽样 3 doc × 6 断言 | 反模式族 mfg dao+service 全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`/`LocalDateTime.now()`/`new Date()`=0；`@Transactional∩@BizMutation` 7 文件均为 Processor REQUIRES_NEW 已裁决范式，R6=2/R10=14 基线内）；checker 19 规则逐项 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；codegen 产物安全（mfg `_gen/`/`_` 前缀零脏面、`__XGEN_FORCE_OVERRIDE__` 仅 target/ 构建产物）；聚合完整性（E1 勘误路径 `nop/main/auth/app.action-auth.xml` 多行 x:extends 23 模块含 mfg）；**F2.7 五修复 HEAD 复核全部有效**（001 `computeReceiptUnitCost` L427-436 分子=加工费+发料流水材料成本合计（经 `stockMoveBiz`/`stockLedgerBiz` I*Biz）/ 002 `canSafelyReverse` L226-232 MANUFACTURE 分支 + inv `inverseMoveType` MANUFACTURE→OUTGOING（ErpInvStockMoveProcessor L363-376）/ 003 `assertNotCancelled` L294-301 接线 submit/approve/reject 三迁移 / 004 `resolveInputLot` 携带领料单头仓（Writer L157/216/221/246）/ 005 `applySubcontractCostToWorkOrder` config-gated writer（WorkOrderProcessor L492-533））；P2-CK-mfg3-011 F1.3 复核 = 6 个 mfg-3 BizModel 全部 `extends AbstractErpCrudBizModel`；⑧ Document/Approval 双 Bean + Forecast Bean 迁移表声明式、8 态可达 r1 裁决复核成立、cancel 白名单 {DRAFT,SUBMITTED,APPROVED} 一致；⑨ SoD `assertApproverNotCreator` L336 在位；**⑨⑭ FNPT 注册面缺口**（8 个自定义 mutation 零注册）→ 新立 P3-CK-mfg3-020-r3；**② daoFor 跨域直查豁免注释缺失**（3 文件 4 站点 + 切片内范式不对称）→ 新立 P3-CK-mfg3-019-r3；F2.7 修复体 `applySubcontractCostToWorkOrder` 查询无 orgId 过滤 → 归并 P2-CK-mfg3-010 同族新站点；⑮ 3 doc × 6 断言 0 漂移（subcontracting.md L238 红冲两段移动 + L229 科目分解 1408/1401/1405/2202 + L100/L192 成本构成、batch-genealogy.md Decision 2 `FG-{woCode}` + Decision 3 config 默认 true、variance-analysis.md L68 委外差异公式），<2 未触发扩样；涉 inv 引擎/posting 引擎内部标注归属 U02/fin-1 未走查内部 | **finding**（新立 2 P3 + 归并 12 + 复用 6，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + mfg-3 面 = 委外单·行/批次基因/成本差异/预测·预测行 6 实体页族 + production-variance/forecast-variance 2 报表页 | `npm run validate:flux` 双数字对账：`files=855 validated=855`（导出 0 error）+ `errors=325 warnings=18491`——325 条逐条同型（`variant:"primary"`×dropdown-button 枚举漂移）与 M1.4/M1.6 登记值精确一致，mfg 域 32 文件 34 条 = M1.5/M1.6 记录精确一致，mfg-3 页面命中 5 文件各 1 条（BatchGenealogy/CostVariance/Forecast/ForecastLine/SubcontractOrderLine）全部同族既有漂移非本切片 finding（successor 在案）；`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0；6 实体页族源头链 = codegen `_gen/_*.view.xml` + 保留层 `x:extends` bounded-merge（合规定制）+ 空 scaffold lib.xjs（数据访问走 flux 缺省 REST `/r/` 层，页面零 GraphQL 调用，`graphql:labelProp` 为平台 meta 键非 API 调用）；`i18n-en:label/displayName` 承载在位；2 手写报表页 `@query:ErpMfgReport__renderHtml` REST 约定 + `i18nEn` 承载 + `/p/` 下载端点 + 已覆盖 visual 像素回归层；**SubcontractOrder 与 Forecast 列表 grid docStatus 单元格样式分支硬编码 `'ACTIVE'`**（subcontract-status 8 态字典与 forecast-status 字典 DRAFT/APPROVED/CONSUMED/CANCELLED 均无 ACTIVE——高亮分支恒死）→ 归并 P3-CK-pur-017-r3 跨域新站点 2 处；E2E：mfg-subcontract-chain/mfg-variance/mfg-genealogy 3 spec 选择器纪律（data-slot/data-testid/.cxd- adapter 外）=0、`E2E_ENGINE` 无 override 缺省 flux、GraphQL 数据层经 `callMutation` = runbook L229 业务动作套件登记口径（禁令不适用） | **finding**（归并态 0 新立，见 §2.3） |
| **DIM-S seed 数据** | §1.3 全套 + subcontract/cost_variance/forecast seed 自洽 + 双层门控默认关 | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；资产清点 **372 CSV + 1 SQL**（= 冻结口径登记值）；`git status --porcelain _init-data/` 空（零 seed 变更，快照重录义务未触发）；deploy `_seed_*.sql` 全仓仅 cs/notify 两族（= seed-data.md §同步义务登记处两行「✅ 已聚合」，**mfg 无 deploy seed 零同步义务**）；mfg-3 seed 自洽抽查：`erp_mfg_subcontract_order.csv` 3 行（id=2 COMPLETED+posted=true+POSTED_STATUS/AT/BY 齐备、id=3 CANCELLED N-TERM 负例 = seed-data.md L89 登记；docStatus/approveStatus ∈ 8 态字典）、`erp_mfg_cost_variance.csv` 1 行（MATERIAL_USAGE posted=false 无孤儿凭证声明）、`erp_mfg_forecast.csv` 1 行（APPROVED ∈ forecast-status 字典）；引擎重算覆盖防护：`variance-auto-calc-enabled` 默认 false（WorkOrderProcessor L595）+ `subcontract-cost-aggregation-enabled` 默认 false + `subcontract-posting-enabled` 默认 false——seed 静态行不被引擎重算覆盖；SPC 双层门控静态行裁决沿用 M1.12 r3「复核成立不重开」 | **pass** |
| **DIM-T 单元测试** | §1.4 全套（`<SVC>` = `module-manufacturing/erp-mfg-service`）+ 委外发料→收货→加工费→红冲 / 基因写入→追溯 / 差异计算触发链清单行 | `mvn test -pl module-manufacturing/erp-mfg-service` **308/0/0/0 全绿**（= M1.5 执行 + MI.6 批注账模块级参照值逐位一致，零回归；全仓聚合对照面 = known-good-baselines 2026-09-08 MI 终态行 4006/0/0/1）；覆盖对账：公开方法 16 个（SubcontractOrder 5/BatchGenealogy 4/CostVariance 5/Forecast 2/两 Line BizModel 0）× `_cases` 8 目录（Subcontracting/SubcontractReverse/BatchGenealogy/ProductionVariance/VarianceAlert/VarianceRecomputeReversal/ForecastCrudSmoke/ForecastSource）全对齐，测试方法 47 个；关键业务流逐行核：①委外全链红冲 ✓（testFullLifecycleWithPosting + SubcontractReverse 4 测试）——**r1 P1-CK-mfg3-002 盲区已消除**：红冲后余额断言在位（SubcontractReverse L144-148 `findBalance(P)`/`findBalance(M1)`，F2.7「P 余额归 0 + M1 余额恢复 10」）；②基因链 ✓（BatchGenealogy 9 测试含 forwardTrace/backwardTrace/traceChain 深度+环路/recallReport 强断言 + F2.7 testMultiWarehouseInputLotResolved）；③差异链 ✓（ProductionVariance 13 + VarianceRecomputeReversal 4 + VarianceAlert 3 + testSubcontractCostAggregatedOnCompletion）；④Forecast ✓（CrudSmoke 3 + Source 4）；零 P2+ 级覆盖缺口；`SnapshotTest.RECORDING` = 0 残留；mfg-3 `_cases` 抽查 `"*"` 通配/delVersion 手工屏蔽 = 0 命中 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT；170 baseline files，totals CAT1..4=0/0/209/1318 为基线记录值，现树实测 0/0/0/0 与 report mode 一致）；`--self-test` **PASS**；mfg 探针族（CAT-1 31/CAT-2 9）维持清零零回归（F2.7 新近触点 `reverseOneMove`/`aggregateIssueMaterialCost`/`applySubcontractCostToWorkOrder` LOG 全英文实核）；WHITELIST mfg 文件条目总体 = 1 条（ErpMfgDashboardBizModel CAT3）四要素齐备（路径/理由 E3 豁免/owner doc 准绳表 #5/裁决来源 plan 2026-09-07-1715-1 Phase 2），为满足抽 ≥3 加抽跨域 2 条（`MaskHelper` C2② ROLE_* seed 角色名契约 + `ErpCtConfigs` C2② DEFAULT_TERMINATE_APPROVER_ROLE）——3/3 四要素齐备零登记缺陷；`grep -L @Locale` 全量 `*Errors.java` = 空；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ck-mfg-subcontract.md`（18 条：0 P0 / 5 P1 / 7 P2 / 6 P3；跨轮索引 **6 fixed**（001-005 F2.7 + 011 F1.3）**/ 12 open**（006-010、012-018））逐条比对 + r2 只读目录 `2026-08-28-2049-ai-check-r2/` + 同批姊妹 `ck-mfg-bom-mrp-r3.md`（mfg2-024-r3 FNPT 族 / mfg2-026-r3 豁免注释族先例）+ `ai-check-index.md` §Mission 基线快照（checker 各命中均为已裁决偏离）。**本轮新立 2 条**（`P3-CK-mfg3-{019,020}-r3`，自 019 起接续，历史 ID 零覆写）。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 6 条

| 原 ID | 修复在位证据（T0 = HEAD `c63d71bb3`） |
| --- | --- |
| P1-CK-mfg3-001（成品计价仅含加工费） | `computeReceiptUnitCost` L427-436 分子 = 头 processingFee + `aggregateIssueMaterialCost` L443-461（发料 OUTGOING 移动单流水 `|totalCost|` 合计，经 `stockMoveBiz.findByRelatedBill` + `stockLedgerBiz.findList` I*Biz）；javadoc 引用本 ID；回归 `testFullLifecycleWithPosting`（unitCost=(2×5+50)/1=60）在 308 绿内 |
| P1-CK-mfg3-002（收货移动单永不反向） | mfg 侧 `canSafelyReverse` L226-232 补 MANUFACTURE 分支（destWarehouseId 非空）+ inv 侧 `ErpInvStockMoveProcessor.inverseMoveType` L363-376 MANUFACTURE→OUTGOING（注释引用本 ID）；红冲两段移动真实可达；回归 `TestErpMfgSubcontractReverse` 余额断言（P 归 0 + M1 恢复 10）在位 |
| P1-CK-mfg3-003（Pattern B 复活） | `assertNotCancelled` L294-301（ERR_SUBCONTRACT_ILLEGAL_STATUS_TRANSITION + CANCELLED 拒绝）接线 `validateTransitionForSubmit/Approve/Reject` L255/274/284 三处；注释引用本 ID；回归 `testCancelledOrderCannotRevive` 在位 |
| P1-CK-mfg3-004（输入批次按产出仓解析） | `BatchGenealogyWriter` L246 `IssueLineCtx` 携带 `issue.getWarehouseId()` → `resolveInputLot(issueLine, ctx.issueWarehouseId)` L157/216-221；注释引用本 ID；回归 `testMultiWarehouseInputLotResolved`（原料仓 3601/成品仓 3602）在位 |
| P1-CK-mfg3-005（wo.subcontractCost 零 writer） | `ErpMfgWorkOrderProcessor.applySubcontractCostToWorkOrder` L492-533（config-gated `CONFIG_SUBCONTRACT_COST_AGGREGATION_ENABLED`，按产品聚合 COMPLETED 委外单加工费 × 完工量分摊，经 `subcontractOrderBiz.findList` I*Biz）；回归 `testSubcontractCostAggregatedOnCompletion`（fee 100/qty 10/完工 2 → 20）在位 |
| P2-CK-mfg3-011（裸 CrudBizModel 无守卫） | F1.3 统一基类接入自动生效：6 个 mfg-3 BizModel（SubcontractOrder/Line、BatchGenealogy、CostVariance、Forecast/Line）全部 `extends AbstractErpCrudBizModel`（grep 实核），状态锁守卫经基类激活 |

### 2.2 归并（同型 open 追加证据至原 ID）— 12 条（r1 mfg3 open 全量，原 ID 状态不动仍 open）

| 原 ID | r3 复核证据（T0，现症逐条复核仍在位） |
| --- | --- |
| P2-CK-mfg3-006 | `reverseOneMove` L209-217 仍 catch 吞异常仅 LOG（无 dispatchFailureAlert 对照正向通道）；`reverseOneVoucher` 同型；`validateCanReverse` COMPLETED+posted 双前置红冲后入口封口原样 |
| P2-CK-mfg3-007 | `issueMaterials`/`receiveFinished` 仓库参数 `@Optional` 无非空校验（BizModel L56/L63 + `generateIssueMove` L371/`generateReceiptMove` L400 直传）；`receivedQty` 非正静默替换/兜底原样；「收货 ≤ 发料」守卫零落地原样 |
| P2-CK-mfg3-008 | `usedInputLots` L161-163 去重 `continue` 不累加数量原样（同批次多领料行 inputQty 丢失） |
| P2-CK-mfg3-009 | `ProductionVarianceDispatcher.reverseIfExists` L133-142 仍以 posted 行存在为门控 + setLimit(1) + 红冲吞异常 + post 幂等命中 null 复合悬挂链原样 |
| P2-CK-mfg3-010 | `findFirmedRollupLine` L345-366 仍无 orgId 过滤 + 全量 FIRMED 头载入 + businessDate DESC 全局最新；**新站点追加**：F2.7 修复体 `applySubcontractCostToWorkOrder` L500-501 查询仅 productId+docStatus 同样无 orgId 过滤（多组织下跨组织委外费混算，修复体继承 CostRollupService 口径时同族缺口扩散）——证据扩员至原 ID |
| P2-CK-mfg3-012 | `validateTransitionForReverseApprove` L303-310 仍仅审批轴；`doReverseApprove` L351-356 仍清审计字段不动 docStatus——ISSUED/RECEIVED/COMPLETED 可翻 REJECTED 原样 |
| P3-CK-mfg3-013 | `traceChain` L89-92 `currentDepth >= depth` 边界 off-by-one 抛 MAX_DEPTH 并弃全部已收集结果原样 |
| P3-CK-mfg3-014 | Writer L177 `setOutputQty(completedQty)` 每行全量原样 + L182 `lotStatus` 硬编码 RELEASED 原样（recallReport 口径失真面不变） |
| P3-CK-mfg3-015 | 四条静默短路（outputLine null / warehouseId null L130-131 / issueLines empty / inputLot null L163-165 / 去重 L161）仍零 LOG（仅 catch 分支有 LOG.error + notify） |
| P3-CK-mfg3-016 | 死列族原样：`postedStatus`/`amountSource`/`amountFunctional`/`totalAmount`/行级 `unitProcessingFee`/`amount` 运行时零消费（唯一命中 `MrpReleaseService` L220 写常量 ZERO 原样） |
| P3-CK-mfg3-017 | `SUBJECT_FINISHED_GOODS = "1401"` L285（名实相悖）+ `SUBJECT_SUBCONTRACT_FINISHED_GOODS = "1405"` L311-312 双常量并存原样 |
| P3-CK-mfg3-018 | N+1/无界原样：`findFirmedRollupLine` 全量头 + 逐头查行；`findIssueLinesWithBatch` 逐领料单查行（有界）；`recallReport` 无深度上限（对照 traceChain 有 50） |

### 2.3 跨切片归并站点追加 — 2 条

- **P3-CK-mfg-016 族**（currentUserId/config 宽 catch 全域族，r1 已注记 4 个 mfg3 站点）：T0 复核 `readBoolConfig` L529-539 / `currentUserId` L549-559 / `BatchGenealogyWriter.isWriteEnabled` / `BatchGenealogyTracer.defaultMaxDepth` 宽 catch 原样——族注记证据复核在位，不重复立项。
- **P3-CK-pur-017-r3**（M1.13 三单匹配页 docStatus=ACTIVE 死状态样式分支，open）：新站点 ×2——`ErpMfgSubcontractOrder.view.xml` 列表 grid docStatus gen-control 样式分支 `== 'ACTIVE' ? 'primary'`（subcontract-status 8 态字典 DRAFT/SUBMITTED/APPROVED/ISSUED/RECEIVED/COMPLETED/CANCELLED/REJECTED 无 ACTIVE）+ `ErpMfgForecast.view.xml` 同型分支（forecast-status 字典 DRAFT/APPROVED/CONSUMED/CANCELLED 无 ACTIVE）——高亮分支恒死、CANCELLED 删除线分支有效；同型同族证据扩员至原 ID（跨域追加，修复方向同原 ID：按各实体实际字典值改写分支键）。

### 2.4 新立 `-r3` — 2 条

**P3-CK-mfg3-019-r3**（DIM-B 维度② 跨域 daoFor 豁免注释缺失 + 切片内范式不对称；同族 P3-CK-fin3-017-r3 / P3-CK-fin4-022-r3 / P3-CK-ast-028-r3 / P2-CK-qa-027-r3 / P3-CK-mfg2-026-r3，按每切片自立 ID 先例新立）

- **控制点**：mfg-3 范围 3 文件 4 站点跨域 daoFor 直查命中处无豁免注释——`BatchGenealogyWriter.batchDao()` L318-320 `daoFor(ErpInvBatch)`、`ErpMfgBatchGenealogyBizModel.batchDao()` L143-145 `daoFor(ErpInvBatch)`、`SubcontractPostingDispatcher.findMove` L290-291 `daoFor(ErpInvStockMove)` + `loadLedgers` L301-302 `daoFor(ErpInvStockLedger)`。
- **证据**：同切片 F2.7 修复体对同一读路径已改经 I*Biz（Processor `aggregateIssueMaterialCost` L447-455 `stockMoveBiz.findByRelatedBill` + `stockLedgerBiz.findList`，注释显式声明「跨域读经 I*Biz（对齐跨实体访问纪律，避免新增 daoFor 站点）」）——dispatcher 的 `findMove` 与 Processor 的 `findByRelatedBill` 为同一查询语义（relatedBillType+relatedBillCode），`IErpInvStockMoveBiz.findByRelatedBill` 与 `IErpInvStockLedgerBiz.findList` 均在位可满足，构成**切片内范式不对称**（同读两范式并存）。r1 对 `ErpInvBatch` 直写语义豁免的裁决（「IErpInvBatchBiz 仅 CRUD 无所需语义」，r1 验证为正确节）**继续有效不重开**；本轮登记的缺陷形态是「命中处缺本地豁免注释」的文档合规面（r3 家族既定口径），非直写行为本身。
- **问题**：跨域直查站点缺本地豁免理由，后续维护者无法就地判别合规性；dispatcher 与 Processor 同读范式分裂。P3 定级（对齐 fin3/fin4/ast/qa/mfg2 同族全 P3）。
- **建议修复方向**：修复归 M2.x：dispatcher 注入 `IErpInvStockMoveBiz`/`IErpInvStockLedgerBiz` 对齐 F2.7 后范式（首选），或按 fin-2 先例逐站点补豁免注释；与 fin-007/ast-028-r3/qa-027-r3/fin3-017-r3/fin4-022-r3/mfg2-026-r3 同族一并收口，全仓同型扫描归 U20/M1.15。

**P3-CK-mfg3-020-r3**（DIM-B 维度⑨/⑭ FNPT 注册面缺口；同族 P3-CK-mfg2-024-r3 / P3-CK-fin4-023-r3 / P3-CK-drp-018 / P3-CK-b2b-011 / P3-CK-prj-022-r3 / P2-CK-qa-029-r3，按每切片自立 ID 先例新立）

- **控制点**：`module-manufacturing/erp-mfg-web/.../erp/mfg/auth/erp-mfg.action-auth.xml`——mfg-3 面 FNPT 资源仅 `FNPT:ErpMfgSubcontractOrder:approve` + `FNPT:ErpMfgSubcontractOrder:reverseApprove` 2 项；mfg-3 全部 8 个自定义 mutation 零 FNPT/权限树注册且 Java 侧无 `@Auth` 声明：`ErpMfgSubcontractOrder` cancel/issueMaterials/receiveFinished/postProcessingFee/reverseCompletion（BizModel @BizMutation L48-77 实核）+ `ErpMfgCostVariance` calculateVariances（L49）+ `ErpMfgForecast` approve/cancel（L41/56）；xbiz 已声明 auth 的 5 审批动作中 submitForApproval（`:mutation` CRUD 基线）/reject（`:approve` 已注册）/withdrawApproval（`:reverseApprove` 已注册）三面覆盖，不扩入本条。
- **证据**：deny-by-default 下上述 8 动作对非 admin 角色全 deny（页面可见但业务动作不可达的内部不一致，与 fin4-023-r3/mfg2-024-r3 同形）；菜单树 mfg-subcontract/mfg-cost/mfg-trace 分组仅挂实体 main 页面未挂动作节点。M1.6 报告 mfg2-024-r3 证据节独立记载「mfg-3 面 FNPT 仅 approve/reverseApprove」交叉印证。
- **问题**：委外三段动作/差异计算/预测审批无法按角色细粒度授权（采购员/生产计划员/财务员对 issueMaterials→receiveFinished→postProcessingFee→calculateVariances 的职责分离不可配置）。P3 定级（与 mfg2-024-r3/fin4-023-r3 同级一致性）。
- **建议修复方向**：修复归 M2.x：按 approve 范式为 8 动作补 FNPT 注册（issueMaterials/receiveFinished/postProcessingFee→采购员或生产计划员、reverseCompletion→生产主管、cancel→创建人角色组、calculateVariances→成本会计、Forecast approve/cancel→生产计划员）；或按 fin-4 先例登记全局 FNPT 补录 successor；与 mfg2-024-r3 同批跨域对齐。

### 2.5 归属标注（§3.2 共享代码边界）

- 库存移动反向消费点（`canSafelyReverse` 守卫/`reverseInventoryMoves` 编排）= 本格 mfg-3 审计；inv 引擎内部（`inverseMoveType`/bookkeeper/`ErpInvStockMoveReverseProcessor` 装配本体）归 U02（M1.13 已审）——本格仅 F2.7 复用复核消费侧。
- posting 引擎内部（`MfgPostingExecutor` @Transactional、`ErpFinPostingProcessor` 幂等语义、sweep 链）**归属 fin-1**（M1.1 已收官）；本格仅核调用点合规（P2-CK-mfg3-006/009 为消费侧复合缺陷，引擎内部面归并 fin-003 族联动）。
- common 抽象族（`AbstractErpCrudBizModel` 状态锁行为、`AbstractProcessor.illegal*`）调用点已审合规（011 复用即基类接入证据）；基类行为缺陷归 U20（M1.15）。
- 聚合横切面（action-auth 聚合器本体/seed 全量装载/flux 导出门禁本体）归 U21（M1.16）；本格仅核 mfg 注册在位性 + FNPT 域内注册面（020-r3 为域内 action-auth 内容缺陷，非聚合机制缺陷）。
- `CostRollupService.aggregateSubcontractCost` 本体（标准侧归集）归 mfg-2（M1.6 同批已审，r1 C4.2 传导注记已承接）；本格仅审 F2.7 修复体对其口径的镜像面（010 归并证据）。`MrpPlanLineReleaseSubcontractRequestProcessor` 归 mfg-2；Forecast 链（BizModel 族 + StateMachine）归本格（r1 切片先例范围）。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 5（mfg3-001..005） | 0 |
| P2 | 0 | 1（mfg3-011） | 6（mfg3-006..010、012；010 含新站点证据扩员） |
| P3 | 2（mfg3-019/020-r3） | 0 | 6（mfg3-013..018）+ 跨切片家族/站点追加 2（P3-CK-mfg-016 族 + P3-CK-pur-017-r3） |
| **合计** | **2** | **6** | **12 + 2** |

五格 verdict：DIM-B **finding**（2 新立 P3：019/020）/ DIM-F **finding**（归并态 0 新立：pur-017-r3 站点追加 ×2）/ DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 18 条 r1 mfg3 ID 状态零覆写（6 fixed 复核有效 + 12 open 追加证据，与跨轮索引 6 fixed/12 open 完全对账）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：mfg-3 委外链全读（`ErpMfgSubcontractOrderProcessor` 560 行 + 9 per-mutation Processor + `SubcontractPostingDispatcher` + 3 Provider + `MfgSubcontractReversalListener` + 双状态机 Bean + 行 BizModel）+ 基因链（Writer doWrite/ensureOutputLot/resolveInputLot/findIssueLinesWithBatch + Tracer traceChain + BizModel recallReport）+ 差异链（Calculator 关键段 + Dispatcher reverseIfExists/dispatchIfApplicable + CalculateVariancesProcessor）+ Forecast 链（BizModel 2 动作 + StateMachine）+ beans 接线（`app-service.beans.xml` 80 bean 注册含 mfg-3 全族）+ xbiz 审批委托 + `erp-mfg.action-auth.xml` FNPT 面；机械程式全套实跑（checker 19 规则 / 反模式族 / codegen 安全 / 聚合完整性 E1 路径 / validate:flux 双数字 / seed 门禁 4/0/0/0 + 自洽抽查 / mfg 回归 308 全绿 / strict+self-test 双 PASS）；owner docs 3 doc × 6 断言抽样（subcontracting 3 + batch-genealogy 2 + variance-analysis 1）；r1 18 条逐一比对裁决 + r2 目录查重 + 同批姊妹报告交叉印证（mfg2-024-r3 FNPT 面）。
- **未深查（边界归属）**：inv 引擎内部（`inverseMoveType` 装配/bookkeeper 余额记账本体，归 U02 已审，仅 F2.7 消费侧复核）；posting 引擎内部（归 fin-1 已收官）；工单/领料/预留/完工入库面（归 mfg-1 已审，`applySubcontractCostToWorkOrder` 所在文件 `ErpMfgWorkOrderProcessor` 的工单面不在本格）；`CostRollupService` 本体与 BOM/MRP/仿真/CRP 面（归 mfg-2 已审）；`ErpMfgReportBizModel` 是否消费基因链/差异行（报表切片面归 M1.16 全局面）；`BatchGenealogyWriter` 对 `ErpInvBatch` 直写与 inv 域批次生命周期一致性（r1 登记的 inv 侧 successor 裁决面，不重开）；xmeta 前端必填拦截（007 触发面收窄维度）；测试代码仅作覆盖对账消费。
- **残留风险（登记不裁决）**：① 12 条归并 open finding 修复归 M2.x（manufacturing P1 修复批及其后）；其中 P2-CK-mfg3-002 已修后建议 M2.x 复核 P2-CK-mfg3-006（红冲吞异常封口）与已修余额回滚的交互（跳过路径的告警缺口仍在）；② P3-CK-mfg3-020-r3 FNPT 缺口为跨域同族第四站点（drp/b2b/prj/qa/fin4/mfg2/mfg3），建议 U21/M1.16 全局面盘点时升格为横切批次修复；③ P3-CK-mfg3-019-r3 揭示的切片内范式不对称（dispatcher vs Processor 同读两范式）若 M2.x 只补注释不统一范式，双路径维护成本残留；④ F2.7 修复体 orgId 缺口（010 扩员证据）在多组织 + aggregation config 开启时生效，当前默认关触发面收窄但投产启用前须修复；⑤ validate:flux 325 variant 漂移与 compliance 机器基线块差距均为批前在案外部事项（successor：`ai-check-r3-compliance-baseline-raise` / nop-chaos-flux dist 基线裁决），非本切片范围。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.4 open 项（020-r3 首补 FNPT 注册清单、019-r3 首补豁免注释/范式统一）；pur-017-r3 的 mfg 站点随该 ID 修复批统一处置。
