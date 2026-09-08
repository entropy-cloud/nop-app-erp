# ck-inventory-r3 — inventory U02 五维符合性审计报告（ai-check-r3 M1.13）

> 工作项：M1.13（U03 × 五维 + U04 × 五维 + U02 × 五维，冻结清单 §4 映射表第 13 行；本报告 = U02 inventory 格，U03/U04 格分别见 `ck-purchase-r3.md` / `ck-sales-r3.md`）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `e0eda4d231f0f95e48eb1f2623edb26cd25134d5`（2026-09-08；计划基线 `f40b4bbae` 后唯一推进 = 两笔 docs-only 审计产物提交，生产代码零变化）；脏面 = 1 条 untracked 计划文件（本计划 `2026-09-08-1454-3`），tracked 零修改。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2 + §3.3 U02 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：inventory 全域（C 级，无切片细分）——StockMove/StockLedger/StockBalance/Batch/SerialNumber/Reservation/TransferOrder/StockTake/CostAdjust/LandedCost/OwnershipTransfer/PickingOrder/CostLayer 族 + 出库策略族（WA/MovingAvg/FIFO/LIFO/BATCH/SPECIFIC/Standard 7 计价）+ 记账器（StockMoveBookkeeper）+ 成本核算族（costing 11 类）+ 追溯链（trace）+ processor 14 族 + posting dispatcher 族 + dashboard/report/spi 子包（`module-inventory/erp-inv-{dao,service,web}` src/main）；owner docs `docs/design/inventory/`（state-machine/trace-chain/cross-domain/consignment/audit-snapshot-cycle-count/use-cases/README）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting processor 族本体唯一归属 fin-1——inventory 凭证 REQUIRES_NEW 为**消费侧**，消费侧行为归本切片；common 抽象族（`AbstractErpCrudBizModel`/`AbstractErpImmutableCrudBizModel`）行为归 U20（本切片只审调用点，21/21 接入合规）；聚合横切面归 U21；notify 消费点归 U11。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U02 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点②④⑧⑮）+ 维度⑮断言抽样 2 doc × 4 断言 | 反模式族全零（六路径同姊妹切片口径：RuntimeException=0/@Inject private=0/System.currentTimeMillis=0）；checker 19 规则 = M0.3 快照行零漂移；`__XGEN_FORCE_OVERRIDE__` 全为 erp-inv-meta dict.yaml 校验点零手改；聚合器含 `/erp/inv/auth/erp-inv.action-auth.xml`；IDaoProvider/IOrmTemplate 25+ 文件命中处豁免理由注释齐备（costing 策略族/Bookkeeper/StandardCostResolver E3.2 架构不变量 Q4 取值豁免/dashboard/report 范式）。15/15 无跳维：①标准链；②**不可变流水基类修复复核有效**（`ErpInvStockLedgerBizModel extends AbstractErpImmutableCrudBizModel` + 审计快照派生视图，P1-CK-inv-003 复用；21/21 基类接入调用点合规）；③ErpInvErrors 集中；④凭证 REQUIRES_NEW 消费侧 = R10 基线内（P2-CK-inv-012 已证伪+收敛 F1.1，HEAD 复核无回归）；⑤⑥⑦⑨⑩⑪⑬⑭ pass（悲观锁 `IOrmTemplate#lock` 到岸成本串行化注记在位）；⑧**出库策略族**：4 策略 locationId 回退均 `move.getSourceLocationId()`（P1-CK-inv-001 复用）+ **自然键余额查找** `findBalance:452-461` 统一委托 `findBalanceByNaturalKey`（skuId 过滤 + nullable 列 IS NULL，P1-CK-inv-002 复用）+ **批次序列守卫** `validateBatchSerialPresence:204-233`（批次必填 ERR_BATCH_REQUIRED + 在库 ERR_BATCH_NOT_FOUND + 序列必填 ERR_SERIAL_REQUIRED，P1-CK-inv-004 复用）——**但序列「已售拒绝出库」状态校验仍缺**（serial-status 无出库链 writer/守卫，仅 ErpInvSkuReferenceChecker 引用检查消费 ACTIVE 集合）→ **新立 P2-CK-inv-012-r3**；⑮2 doc × 4 断言：state-machine.md L65「批次/序列号缺失拒绝确认」 vs validateBatchSerialPresence → 一致（inv-004 复用）；state-machine.md L67「已售序列号拒绝再次出库」 vs 出库链零序列状态校验 → **漂移（= 新立 012-r3）**；state-machine.md L40「CONFIRMED→DONE 写一条不可变流水」 vs Bookkeeper+Immutable 基类 → 一致；trace-chain.md §实现说明 L42「单 uplink 列 + 反向查询（2026-07-02 登记偏离）」 vs ORM `originMoveId`/`originReturnedMoveId` to-one → 一致（偏离已登记非漂移） | **finding**（1 新立 P2；6 复用 + 15 归并，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 库存移动确认页（复杂手写页清单成员）/批次序列选择 | `npm run validate:flux`：855/855 validated，325 ERR 100% variant=primary 既有外部漂移（inv 域命中 13 条同族成员不立项）；`component="AMIS"` 0、ORM `ext:web-renderer="flux"` 无缺失；`stock-take-flow/main.page.yaml`（手写流程页）`i18nEn`×15 + `@query/@mutation`×4 ✅；`ErpInvStockLedger/ref-move.page.yaml` = M0.4 row 14 登记 codegen wrapper（fixedProps=moveId 关联子表，文案承载在模型源 view.xml `i18n-en:*`，禁改本文件 lesson 06）；批次/序列选择 = Batch/SerialNumber `picker.page.yaml` codegen 族 + 全 21 实体 main+picker wrapper 面（`x:gen-extends`→view.xml）；`graphql:labelProp` 命中均为元数据键读取；E2E inventory-stock-move/inventory-stock-move-reverse spec：非法 selector 0 / 页面级 GraphQL 断言 0（GraphQL 驱动自定义 @BizMutation = runbook sanctioned 范式）/ `E2E_ENGINE` 缺省 flux | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + stock_move/balance/cost_layer（2210-1 批）域内金额自洽（balance↔cost_layer） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；`_init-data` porcelain 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL** = 冻结登记值；inv deploy `_seed_*.sql` 命中 = 0；2210-1 批裁决面（seed-data.md §运营域「域表直 seed」范式）对照抽查：balance#1（org2/mat3/wh2：100×8.50=850.00）↔ cost_layer#1（100/100×8.50=850.00，incomingMoveId=1→MV-2026-001 PURCHASE_RECEIVE→PRCV-2026-001 回链）精确一致；balance#2（80×120=9600.00）↔ cost_layer#2（80/80×120=9600.00，OPENING_BALANCE 面）精确一致；ledger LDG-2026-001（+100@8.50，balance_quantity=100/balance_total_cost=850）↔ balance#1 一致 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P0 库存移动+流水+余额一致性清单行覆盖核对 | `mvn test -pl module-inventory/erp-inv-service` **253/0/0/0 全绿**（基线 inv 248 +5 = `bdb469d22` 提交注记披露增量「inv 253 green」，已知非漂移）；覆盖对账：BizModel 注解动作 42，`_cases` 测试资产 31 项；**P0 行在位**：`TestErpInvStockMoveBookkeeping`（记账↔流水↔余额）+ `TestErpInvLedgerImmutable`（不可变流水）+ `TestErpInvSnapshotAndStockCheck`（快照↔余额一致性）+ 7 计价策略族测试（WA/FIFO/LIFO/BATCH/SPECIFIC/Standard/CostingDispatch）+ `TestErpInvBatchExpiryInterception`（inv-004 修复红→绿）+ `TestErpInvTraceChain`；覆盖密度高于注解面（策略组合多测）；`SnapshotTest.RECORDING` = 0；`TestErpInvResolverRawValueAfterReadPathMasking` 专项屏蔽测试在位 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations，单向收紧）；`--self-test` **PASS**；inv 探针族（CAT-1 26/CAT-2 7）维持清零零回归；WHITELIST inv 条目 3/3 四要素齐备：`IErpInvStockLedgerBiz.java`（@Description 专属 2 行 E3 豁免/判定准绳表 #5/plan 2026-09-07-1715-1 Phase 2）+ `ErpInvDashboardBizModel.java`（1 行同四要素）+ `ErpInvStockLedgerBizModel.java`（2 行同四要素）；`grep -L @Locale` = 空；meta/i18n 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ai-check-index.md` §Finding 追踪（同域报告 `ck-inventory.md` C2.3 全 22 条逐一比对——P1×5 + P2×11 + P3×5）+ r2 只读目录（无 inv 同型新独立登记）+ §Mission 基线快照。**本轮新立 1 条**（`P2-CK-inv-012-r3`）；历史 22 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 6 条

| 原 ID | 修复在位证据（T0 = HEAD `e0eda4d23`） |
| --- | --- |
| P1-CK-inv-001（4 出库策略 locationId 回退误用 warehouseId） | WeightedAverage:74-75 / Batch:85-86 / Lifo:82-83 / Specific:81-82 四处均 `line.getSourceLocationId() ?: move.getSourceLocationId()`；F2.x 修复 HEAD 复核有效 |
| P1-CK-inv-002（findBalance 查询键与 UK 自然键不一致） | `StockMoveBookkeeper#findBalance:452-461` 统一委托 `findBalanceByNaturalKey:350-380`（skuId eq/isNull + locationId/batchNo/ownerId nullable IS NULL，与 UK_INV_STOCK_BALANCE_NATURAL 对齐；:454「P1-CK-inv-002」修复注记在位） |
| P1-CK-inv-003（流水不可变/余额流水驱动零强制） | `ErpInvStockLedgerBizModel extends AbstractErpImmutableCrudBizModel`（16 行裸桩 → 不可变基类 + 审计快照 `@BizQuery` 派生视图，javadoc E3.2 注记）；Balance 侧专用写路径（Bookkeeper/Reservation）不变 |
| P1-CK-inv-004（批次/序列号缺失拒绝确认未实现） | `validateBatchSerialPresence:204-233`：isBatchManaged+blank batchNo → ERR_BATCH_REQUIRED；findBatch null → ERR_BATCH_NOT_FOUND（「批次在库」）；isSerialManaged+blank serialNo → ERR_SERIAL_REQUIRED；置于效期守卫之前（:132-136 注记）；`TestErpInvBatchExpiryInterception` 红→绿在案。**残留**：「已售拒绝」状态校验缺失经裁决新立 P2-CK-inv-012-r3（原 ID fixed 状态不动，修复注记「独立特性 Deferred」无独立 ID 登记由本切片承接） |
| P1-CK-inv-005（通用 CRUD 无状态守卫） | F1.3 统一基类接入在位：21/21 inv BizModel extends 基类族；调用点合规 |
| P2-CK-inv-012（REQUIRES_NEW 凭证先于主事务提交） | r1 证伪+收敛终态复核：StockMove doComplete 子站证伪（dispatchIfApplicable 已是末步）+ landedCost/costAdjust billHeadCode 稳定（F1.1 引擎层传导，inv 零代码变更）；HEAD 无回归 |

### 2.2 归并（同型 open 追加证据至原 ID）— 15 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-inv-006 | **现症复核**：`ErpInvOwnershipTransferProcessor#reclassifyBalance:132` 仍 `divide(qty, BigDecimal.ROUND_HALF_UP)` 废弃重载（非整除 ArithmeticException 面未变） |
| P2-CK-inv-007 | VMI 层式计价零成本转移原样（source.getAvgCost() 对 FIFO/LIFO/BATCH/SPECIFIC 恒 null 路径未变） |
| P2-CK-inv-008 | SpecificCostingStrategy serialNo 参数查询未引用原样（findSpecificLayers:172-195 过滤面未变） |
| P2-CK-inv-009 | reclose 全月加权平均公式与 javadoc 不符 + 顺序依赖原样 |
| P2-CK-inv-010 | 多币种混算无汇率折算原样（PostingEvent 汇率恒 1 族，含 mfg-021/ast2-017/mnt-016 跨域同型族维持 open） |
| P2-CK-inv-011 | Dashboard 周转率分子读 line.totalCost 原样（策略族只刷 unitCost 不回写 totalCost 未变） |
| P2-CK-inv-013 | 预留量写与成本层消耗不走乐观锁重试原样 |
| P2-CK-inv-014 | 到岸成本 GL 金额与成本层调整额口径分叉原样 |
| P2-CK-inv-015 | StandardCostResolver 全表加载 + N+1 + 无 orgId 过滤原样 |
| P2-CK-inv-016 | reclosePeriodCosts 期末全量 N+1 原样 |
| P3-CK-inv-017 | dict 死状态群（batch-status/serial-status/reservation EXPIRED 零 writer）原样——本切片 P2-CK-inv-012-r3 与 serial-status 死字典治理相关联（修复翻转 writer 后字典复活，两 ID 联动） |
| P3-CK-inv-018 | currentUserId 宽 catch 返回 null 无日志（2 处）原样 |
| P3-CK-inv-019 | reclose recomputeOutgoingCogs totalCost 正值符号破坏原样 |
| P3-CK-inv-020 | Dashboard/追溯链无界加载与 N+1 原样 |
| P3-CK-inv-021 | 死配置键 `erp-inv.concurrent-deduct-retry-backoff-ms` 零消费原样 |

### 2.3 新立 `-r3` — 1 条（P2）

**P2-CK-inv-012-r3**（DIM-B 维度⑧/⑮）

- **控制点**：出库链序列号状态校验 + `ErpInvSerialNumberBizModel`（16 行 CRUD 桩，无 IN_STOCK→OUT 状态翻转 writer）+ `ErpInvStockMoveProcessor`（validateBatchSerialPresence 仅做 presence 校验，无序列状态守卫）。
- **owner-doc 断言漂移**：`docs/design/inventory/state-machine.md` §异常路径 L67「序列号已售：出库时校验序列号状态；已售序列号拒绝再次出库」逐字未实现——已售/已出库序列号可再次出库（presence ≠ status gate）；README §关键业务规则 6「序列号未售」同。
- **三态裁决**：新立（原 ID 历史零覆写）。P1-CK-inv-004 已 fixed——其 F2.11 修复注记明示「『未售/在库状态』翻转 writer 缺失为独立特性，**Deferred**」但该 Deferred 残留无独立 finding ID 登记，本切片按 §2 对已登记 Deferred 残留无 ID 可归并的情形新立 `-r3` ID 承接（对齐 code-history-deferred-triangulation §3.1 精神：Deferred 触发条件成立即新立，原 ID 状态不动）。级别 **P2**：属特性级缺口（依赖 serial-status 翻转 writer 先行落地）+ 现网无序列管控 seed 数据单币种单批次场景无症状（同 inv-007/inv-010 门控性降档逻辑）；若序列管控物料投产则升 P1。
- **建议修复方向**：① `ErpInvSerialNumberBizModel` 补出库确认时 IN_STOCK→OUT 状态翻转 writer（经 Bookkeeper 同事务）+ 出库链 status=IN_STOCK 守卫（已售拒绝，新增 ErrorCode `erp.err.inv.serial-not-in-stock`）；② 与 P3-CK-inv-017（serial-status 死字典）联动治理——writer 落地后字典复活，登记同一修复批次；③ 修复阶段先写「已售序列号出库拒绝」失败测试。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting processor 族本体**归属 fin-1**：本切片核对 InvPostingDispatcher/LandedCostPostingDispatcher/CostAdjustmentPostingDispatcher/OwnershipTransferPostingDispatcher/InvReversalListener 消费侧（resolveBusinessType 跳过集、REQUIRES_NEW 边界、红冲回退消费）；引擎内部（重试通道/posted 回写）归 fin-1。
- common 抽象族（`AbstractErpCrudBizModel`/`AbstractErpImmutableCrudBizModel`）调用点合规（21/21）；基类行为归 U20（M1.15）。
- 聚合横切面归 U21（M1.16）；本格仅核 inv 注册在位性。
- `StandardCostResolver` 直读 mfg-dao 成本源 = E3.2/Q4 架构不变量豁免站（r1 已裁决，基线面维持）；md 物料读取经 `materialBiz.get` I*Biz 权限管道合规。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 5（inv-001..005） | 0 |
| P2 | 1（inv-012-r3，DIM-B ⑧⑮） | 1（inv-012） | 10（inv-006..011 + inv-013..016） |
| P3 | 0 | 0 | 5（inv-017..021） |
| **合计** | **1** | **6** | **15** |

五格 verdict：DIM-B **finding**（1 新立 P2）/ DIM-F **pass** / DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 22 条 r1 ID 状态零覆写（6 fixed 复核有效 + 15 open 追加证据 + 1 fixed 之 Deferred 残留由新 ID 承接）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-inv-service 全部 processor（14 族）+ entity BizModel 21（含 Ledger/StockMove/Reservation/Batch/SerialNumber）+ 出库策略族 7 计价全类 + StockMoveBookkeeper 记账器 + costing 11 类（CostAdjustmentService/StandardCostResolver/各 Strategy）+ trace/CostMethodResolver + posting dispatcher 族 + dashboard/report/spi + ErpInvErrors/DaoConstants + 机械程式全套实跑（checker/反模式/codegen/聚合 E1/validate:flux/seed 门禁/inv 回归 253/strict+self-test）；owner docs 2 doc × 4 断言抽样（state-machine.md/trace-chain.md）；r1 22 条逐一比对裁决；r2 目录核对；inv seed 4 CSV 逐行抽查。
- **未深查（边界归属）**：posting 引擎本体（归 fin-1）；`AbstractErpCrudBizModel`/`Immutable` 基类内部（归 U20）；`erp-inv-web` 渲染时行为（静态走查 + 门禁，浏览器回归归看板运行时专项）；cross-domain.md/consignment.md/audit-snapshot-cycle-count.md 全量断言（抽样 ≥2 doc 达标，漂移计数 = 1（L67，即新立 012-r3）< 2 未触发扩样）。
- **残留风险（登记不裁决）**：① 15 条归并 open finding 修复归 M2.x，其中 P2-CK-inv-006/007（VMI 转移除法+零成本族）与 P2-CK-inv-013（乐观锁重试）建议 M2.x 优先；② P2-CK-inv-012-r3 与 P3-CK-inv-017 联动（序列 writer 落地前 serial-status 字典维持死状态面，两 ID 同批收口）；③ multi-currency 族（inv-010 + mfg-021 + ast2-017 + mnt-016 跨域同型族）建议跨域统一裁决批次；④ `validate:flux` 325 条 variant 漂移为批前在案外部事项（successor：nop-chaos-flux dist 基线裁决）。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项；序列管控物料 seed 投产时 P2-CK-inv-012-r3 升 P1 立即修复。
