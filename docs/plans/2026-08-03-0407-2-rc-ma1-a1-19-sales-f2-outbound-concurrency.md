# 2026-08-03-0407-2 rc-ma1-a1-19-sales-f2-outbound-concurrency sales-F2 出库与并发需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.19（MA1 需求追踪矩阵审计 — sales-F2 出库与并发）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.19
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.19 的 0.2 依赖）、`2026-08-03-0407-1-rc-ma1-a1-18-sales-f1-mainflow-pricing.md`（同批次 sales 审计，先 F1 后 F2）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.19 给出 UC 清单 = `UC-SAL-02/03/10`（3 UC），含 `use-cases.md:56` / `:76` / `:216` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/sales/use-cases.md`：
  - UC-SAL-02 出库可用量不足审核回滚（销售独有，`:56`）：**订单审核通过触发**：调用库存校验可用量；若可用量 < 出库数量 → 审核失败回滚（订单保持 SUBMITTED，不前推）、不生成出库移动单、库存余额不变。销售与采购对比：采购入库只增加库存（不校验余量），销售出库必须校验。
  - UC-SAL-03 分批出库与部分收款（`:76`）：订单（数量=100）审核通过；出库1(60)→发票1(60)→收款1 核销发票1；出库2(40)→发票2(40)→收款2 核销发票2。断言：订单行.已出库数量==60（第一次后）/==100（第二次后，派生）；发票1.收款状态=部分（若收款1<发票1金额）；发票2.收款状态=已收清（若收款2=发票2金额）。
  - UC-SAL-10 并发出库扣同一批次（`:216`）：两个销售出库同时扣减同一批次库存，乐观锁保证一致性；一个成功（扣减），另一个乐观锁冲突 → 重试或失败；最终 批次.现有量 == 初始 - A数量 - B数量（不超扣）。

- **L3 代码实现现状（实测）**——存在与 L1 的关键结构性偏离（G1/G2）：
  - **UC-SAL-02 可用量校验触发点（关键偏离 G1）**：L1 要求校验在**订单审核**环节，实现却放在**出库审核**环节。
    - 订单审核：`ErpSalOrderApproveProcessor.java:49-51` → `ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` **仅 requireCustomerActive + creditLimitChecker.check**，**不调用任何库存 Facade**（`@Inject` 簇 `:54-67` 无 `IErpInvStockMoveBiz`/`IErpInvStockBalanceBiz`）。MA2 `2026-07-28-0400-arm-ma2-sales-state-machine.md:21` 同证"approve 仅状态推进 + 信用占用 + 承付 commit/intercompany（config-gated），无直接库存/凭证写"。
    - 出库审核：`ErpSalDeliveryApproveProcessor.java:37` → `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `IErpInvStockMoveBiz.generateMove` → `ErpInvStockMoveProcessor.doConfirm:86-98` → `validateAvailable:116-136`（不足抛 `ERR_AVAILABLE_INSUFFICIENT` `ErpInvErrors.java:49`，无 try/catch 裸抛 → `@BizMutation` 回滚，出库单保持 SUBMITTED，无库存移动单，余额不变）。销售独有性成立：`reservesOnConfirm:242-248` 仅 OUTGOING/INTERNAL_TRANSFER 触发，INCOMING（采购入库）跳过。
    - **L2/L3 vs L1 冲突**：L2 `sales/state-machine.md:52,56` 与 MA2 审计均说出库级校验，L1 `use-cases.md:62-66` 说订单级。按 `docs/context/source-of-truth-and-precedence.md` + 方法论 §4（L1 权威），L1 胜 → 标记需求符合性分歧（修复方向须人工裁决：要么 L1 措辞改为出库级，要么补订单级校验）。
  - **UC-SAL-03 分批出库 + 派生 已出库数量（关键偏离 G2）**：每订单行多出库**数据模型支持**（`ErpSalDeliveryLine.orderLineId` 可多行指向同一订单行）；但 `deliveredQuantity`（已出库数量）列**存在但从不被持久化**。
    - ORM：`app-erp-sales.orm.xml:402` `<column name="deliveredQuantity" ... propId="14" ... defaultValue="0">`（实体块 382-457，`versionProp="version"` `:386`）；生成 setter `_ErpSalOrderLine.java:1228` 存在，但 grep 显示仅生成 bean 代码 + `ReturnQtyValidator.loadDeliveredQuantity` 读取，**无任何业务/服务代码写入**。
    - 实际实现（仅头级）：`ErpSalDeliveryProcessor.rollupOrderDeliveryStatus:270-310` 在内存聚合 `Σ approved DeliveryLine.qty by orderLineId`（`:280-287`），算出 3 态字符串（`:289-308`），调 `ErpSalOrderBizModel.updateDeliveryStatus:242-254` **仅写订单头 `deliveryStatus`**（UNDELIVERED/PARTIAL/DELIVERED）；逐行 `deliveredQuantity` 计算后被丢弃。
    - 发票/收款侧（UC-SAL-03 后半）：`ReceiptSettler.recomputeInvoiceReceived:161-177` 正确派生 `receivedStatus`（received≤0→UNRECEIVED；≥total→RECEIVED；否则 PARTIAL）——UC-SAL-03 发票侧断言**满足**。
  - **UC-SAL-10 并发乐观锁**：版本字段 `ErpInvStockBalance.version`（`app-erp-inventory.orm.xml:389`，`versionProp="version"` `:369`）；扣减路径 `StockMoveBookkeeper.updateBalanceWithRetry:255-328`——MANAGED 态 `dao.tryUpdateWithVersionCheck`（`UPDATE WHERE id=? AND version=?`，0 行→冲突 `:271`）；重试循环 `:264-327`，上限 `erp-inv.concurrent-deduct-max-retry` 默认 5（`ErpInvConstants.java:18-19`）；冲突时 `recordOptimisticLockFailure` 计量 + 驱逐 + 重载重算（`:290-326`）；耗尽抛 `ERR_INV_CONCURRENT_DEDUCT_CONFLICT`/`ERR_INV_BALANCE_INSERT_CONFLICT`。UK 兜底（P0-MA2-020 已落地）：`UK_INV_STOCK_BALANCE_NATURAL (orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId)`。销售出库侧无独立锁，完全委托库存域 `updateBalanceWithRetry`（同一 `@BizMutation` 事务）。

- **L4 测试证据现状**：
  - UC-SAL-02（不足回滚）：`TestErpSalDeliveryStockMove.testApproveInsufficientAvailableRollsBack:121-155`（强：`ERR_AVAILABLE_INSUFFICIENT`、出库单保持 SUBMITTED、posted=false、无 DONE 移动单、余额不变 5）——**但出库级**（订单预置 APPROVED `:410`）；`testNegativeStockConfigAllowsShortage:179-210`（负库存配置分支）；`TestErpSalOrderApproval` 全 14 方法**无可用量校验测试**（Javadoc `:34` 明示）。**UC-SAL-02 订单级回滚未测试**。
  - UC-SAL-03（分批 + 派生）：`TestErpSalDeliveryStockMove.testDeliveryStatusRollupToOrder:212-247`（强头级 rollup，但测**2 行×1 出库/行**，非**1 行×2 分批(60+40)**，**从不断言 orderLine.deliveredQuantity**）；`TestErpSalOrderToDeliveryEnd:69-125`（单次满量）；`TestErpSalOrderToCashEnd:167-171`（收款侧 60/100→PARTIAL 强）。**无测试**满足 L1 断言"订单行.已出库数量==60→100"（因列从不写入，此类测试会失败）。
  - UC-SAL-10（并发）：`TestErpInvConcurrentDeduct`（库存域）：`testConcurrentDeductRetrySucceeds:86-124`（单线程模拟版本偏斜）、`testConcurrentDeductRetryExhaustedThrows:131-172`、`testConcurrentDeductNoOversell:178-182`（**真实多线程** ExecutorService+CountDownLatch，3 线程×3，最终 total=1）、`testConcurrentFirstMoveSameDimensionThrowsAndRetries:207-239`、`testConcurrentFirstMoveMultiThreadNoDuplicateRows:250-306`（**真实多线程** INSERT 冲突）。**销售级并发测试为零**（所有并发测试在 `module-inventory`，无 `ErpSalDelivery__approve` 并发线程同批次测试）。E2E `o2c-chain.spec.ts` 仅单次满量快乐路径，无不足/分批/并发场景。
  - **注意（疑似缺口 G5）**：MA2 `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md:326-327` 称"无并发场景集成测试"，与现存 `TestErpInvConcurrentDeduct`（6 测试，引用 plan `2026-07-07-0024-2`）矛盾——审计文本疑似陈旧，本切片须重新核验对账。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（A2.17，UC-SAL-10 主证据）：§2 :65,68（sales 27/27 + inventory 31/31 实体 100% versionProp）；§3 :100（`tryUpdateWithVersionCheck` + 重试 sustained）；§4 :119-121（UC-INV-08 超卖/UC-SAL-10 双重核销/UC-SAL-10 并发扣批次 全部 sustained 缺口证伪）；§13 :296-298（UC-SAL-10 PASS）；P0-MA2-020（`:260-268` 库存余额自然键 UK）。
  - `docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`（A2.9）：§3 维度2 :111（出库 approve 可用量校验前置 PASS）；维度4 :130（出库可用量不足 PASS）。裁决 ⚠️(P1)；但审计以 L2 为准，**未标 L1↔L3 订单级偏离**。
  - `docs/audits/2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`：§可用量 :39-44（链路 triggerOutgoingMove→generateMove→doConfirm→validateAvailable，错误传播 PASS）；§UC-SAL-10 :183,195（乐观锁基础具备，并发扣批次交接 A2.17）。**未标订单级偏离**。
  - `docs/audits/2026-07-06-use-case-implementation-audit.md:71-86`：UC-SAL-02 ✅（出库级）、UC-SAL-03 🔶（deliveredQty 未测试）、UC-SAL-10 ❌（乐观锁未测试）——早期 UC 覆盖调查，须重新核验。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为，只补"需求契约↔行为"差异（订单级校验缺失 / deliveredQuantity 从不写入 / 销售级并发测试缺失 / 审计文本陈旧对账）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P0-MA2-020`（库存余额自然键 UK，**completed**，UC-SAL-10 INSERT 竞态兜底）、`P1-MA2-085`（LandedCost TOCTOU，**todo MR1**，非销售出库）、`P2-MA2-014`（ReceiptSettler 并发核销无锁，watch-only，UC-SAL-03 收款侧）、`P1-MA1-022`（跨域 daoFor 读，**resolved**）。arm-index :180 显式记录 UC-SAL-10 并发扣批次为曾交接项（后 A2.17 sustained）。**RC 系列对 sales 为零**——A1.19 为 sales RC 切片。本切片新发现的静默缺口（G1 订单级校验缺失 / G2 deliveredQuantity 不写入 / G3 无 60+40 分批测试 / G4 销售级并发测试缺失）须按 §7 grep 比对后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1；触及 ORM 结构（如补 `deliveredQuantity` 写入路径）或并发扣减逻辑的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.19 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.19 报告并登记 finding，解除其链路证据缺口。

## Goals

- 产出 A1.19 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-19-sales-f2-outbound-concurrency.md`，含方法论 §6 **9 段全部内容**。
- 对 3 UC（UC-SAL-02/03/10）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并。
- 对候选缺口给出分级结论：G1 订单级可用量校验缺失（L1↔L3 冲突须人工裁决）、G2 deliveredQuantity 从不持久化（UC-SAL-03 派生断言不可满足）、G3 无 60+40 分批测试、G4 销售级并发测试缺失、G5 MA2 审计文本陈旧对账——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。对 G1 的 L1↔L2/L3 真相源冲突按 §9 冻结条款记入报告，不直改真相源。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——UC-SAL-02 订单级 vs 出库级措辞分歧记入报告交人工裁决，不直改 use-cases/state-machine）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.18/1.20/1.21 各自独立 plan；A1.19 只覆盖 UC-SAL-02/03/10）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：UC-SAL-10 库存域乐观锁行为由 A2.17 证实，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.19 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.19 UC 锚点）+ `docs/design/sales/use-cases.md`（L1 真相源）+ `docs/design/sales/state-machine.md` + `docs/design/inventory/cross-domain.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + E2E/单测录制；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-inventory/erp-inv-service -Dtest=TestErpInvConcurrentDeduct` / `module-sales/erp-sal-service -Dtest=TestErpSalDeliveryStockMove`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-19-sales-f2-outbound-concurrency.md`（新建，先填 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-SAL-02/03/10 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:56/:76/:216` 验收标准原文；L2 引用 `state-machine.md §4` + `inventory/cross-domain.md §余量校验`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `module-sales/.../processor/ErpSalOrderProcessor.java:166-170`（订单审核，无库存调用）/ `ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` / 跨域 `IErpInvStockMoveBiz.generateMove` → `ErpInvStockMoveProcessor.validateAvailable:116-136` / `ErpSalDeliveryProcessor.rollupOrderDeliveryStatus:270-310` / `ErpSalOrderBizModel.updateDeliveryStatus:242-254` / `ReceiptSettler.recomputeInvoiceReceived:161-177` / 库存 `StockMoveBookkeeper.updateBalanceWithRetry:255-328`；L4 引用 `TestErpSalDeliveryStockMove#*` / `TestErpInvConcurrentDeduct#*`（注明断言强度 + 单线程模拟 vs 真实多线程）；L5 复用 MA2 A2.17 + O2C + E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①G1 UC-SAL-02 订单审核不触发可用量校验（`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 仅客户+信用，L1 `:62-66` 要求订单级）；②UC-SAL-02 出库级回滚行为本身**已正确**（`TestErpSalDeliveryStockMove:121-155` 强证据，但订单级缺失）；③G2 `deliveredQuantity`（`app-erp-sales.orm.xml:402`）从不被服务代码写入（grep 仅生成 bean + ReturnQtyValidator 读），UC-SAL-03 `:89-90` 派生断言不可满足；④UC-SAL-03 头级 `deliveryStatus` rollup 正确（`rollupOrderDeliveryStatus:270-310`）；⑤UC-SAL-03 发票/收款侧 `receivedStatus` 派生正确（`recomputeInvoiceReceived:161-177`）；⑥G3 无 1 行×2 分批(60+40) 测试（现有为 2 行×1/行）；⑦UC-SAL-10 库存域乐观锁 + 重试 + UK 兜底完备（`updateBalanceWithRetry`/`UK_INV_STOCK_BALANCE_NATURAL`）；⑧G4 销售级并发测试为零（`ErpSalDelivery__approve` 无并发线程同批次测试）；⑨G5 MA2 审计文本 `2026-07-28-1249...:326-327` "无并发测试" 与现存 `TestErpInvConcurrentDeduct`（6 测试）矛盾——对账。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：G1（核心业务循环/异常路径 UC-SAL-02 订单级缺失）属"异常路径未实现"——定级（倾向 P1，须人工裁决 L1 措辞 vs 补订单级校验）；G2（状态迁移断言不可达/数据未持久化）属"行为实质偏离验收标准"——定级（倾向 P1）；G3/G4（断言强度/测试覆盖）倾向 P2；G5 对账结论记入报告。每结论须列明命中判据编号 + 三源对照；**G1 的 L1↔L2 冲突按 §4 + §9 记录为需求符合性分歧，不直改真相源**。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-SAL-02/03/10 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度与并发测试机制、L5 标注复用 A2.17 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 G1-G5 有明确分级（非悬空"待查"）；G1 L1↔L3 冲突已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-19-sales-f2-outbound-concurrency.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` sales 出库/可用量/并发同控制点（如 P0-MA2-020 UK、P2-MA2-014 ReceiptSettler 并发、arm-index :180 UC-SAL-10 交接项）后裁决——同根因同控制点 → 复用（追加 RC 注记）；新根因 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 列明差异依据。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 UC-SAL-10 真实并发下销售→库存 Facade seam 的重试行为、负库存配置下并发结果、deliveredQuantity 查询实际返回值等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（UC-SAL-10 库存域乐观锁 PASS）+ `2026-07-28-0400-arm-ma2-sales-state-machine.md`（出库级校验 PASS）+ `2026-07-27-1949-...-order-to-cash-e2e.md`，列明只补的需求视角差异（订单级校验缺失 / deliveredQuantity 不写入 / 销售级并发测试缺失 / 审计文本陈旧对账）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；既有行追加 RC 注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_03be5f3f3ffeTUZuaMdM3aofjL，fresh session，未起草本计划）。规则 1-11 全 PASS：(1) Deps A1.19=0.2 done；(2) 单结果表面（A1.19 报告 UC-SAL-02/03/10）；(3) 格式 + 命名合规；(4) UC 覆盖精确（baseline-inventory A1.19 行）；(5) Baseline 全 spot-check PASS——`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 无库存 Facade / `app-erp-sales.orm.xml:402` deliveredQuantity 列存在但 `setDeliveredQuantity` 仅生成 bean + MFG 测试写 ZERO（无生产代码写真实值，`rollupOrderDeliveryStatus:270-310` 仅写头级 deliveryStatus）/ `ErpInvStockMoveProcessor.validateAvailable:116-136` 不足抛异常 / `StockMoveBookkeeper.updateBalanceWithRetry:255-328` + `tryUpdateWithVersionCheck:271` + 重试 / `TestErpInvConcurrentDeduct` 真实多线程（CountDownLatch）/ `ErpInvStockBalance.version` versionProp；(6) 方法论 §1-§10 + §去重（A2.17 reuse）+ §7 + §8 + §9 对齐；(7) 反松弛；(8) typing；(9) Closure Gates audit-only 有据；(10) Non-Goals 守约；(11) **L1↔L2/L3 UC-SAL-02 冲突处理正确**——计划记录冲突（L1 订单级 vs L2/L3 出库级）+ §4 L1 胜 + 路由人工裁决 + 显式禁止编辑 use-cases/state-machine，不自决。无阻塞。Non-blocking（已评估，无需修订）：①`setDeliveredQuantity` grep 补充 MFG 测试写 ZERO（test setup 非生产，G2 结论不变，执行报告 §2/§3 重跑 grep 显式注明 test-only ZERO-writers）；②`testApproveInsufficientAvailableRollsBack` 实际行号 122 vs 计划 :121（trivial）；③MA2 行内锚点未逐字核验（低关键度，执行时复用 spot-confirm）。共识达成，可转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.19 报告 9 段齐全 + 3 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.19 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；触及 ORM 结构（补 deliveredQuantity 写入）或并发扣减逻辑的修复行须 ask-first + 独立 plan-audit（§5）。G1（UC-SAL-02 订单级 vs 出库级）属 L1↔L2 真相源冲突，修复方向须人工裁决（改 L1 措辞 or 补订单级校验），非本审计可自决。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；G1 待人工裁决真相源）

## Closure

Status Note: A1.19 sales-F2 出库与并发审计完成。3 UC（UC-SAL-02/03/10）五级追踪矩阵填充完毕，9 段报告落盘，finding 复用/新增裁决可追溯至 arm-index。本计划为只读审计（无代码/ORM/真相源变更），结果表面 = 报告 + arm-index 登记，二者均已落地。所有 3 个新 P2 finding 已分级（纯 Processor 修复或测试补充，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first），reuse P1-RC-020 已对既有 arm-index 行追加 A1.19 RC 视角注记。G1 L1↔L2/L3 真相源冲突按 §9 冻结条款记入报告，不直改真相源，修复方向待人工裁决。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_03bcaca9（fresh session，未执行本计划），Closure Gates 10 项门控中 9/10 substantive pass（独立结束审计不在执行会话内运行）。
- Evidence:
  - 审计报告产出：`docs/audits/2026-08-03-0530-rc-ma1-a1-19-sales-f2-outbound-concurrency.md`（§0 TL;DR + §1-§9 全 10 段齐全 + §10 Verdict，3 UC 矩阵行完整）。
  - arm-index 登记：`docs/audits/arm-index.md:87`（A1.19 报告清单登记 MA1(RC) done）；`:129`（P1-RC-020 reuse 注记 — UC-SAL-02 订单级校验缺失与 A1.18 同根因同控制点）；`:135-137`（3 新 P2 finding：P2-RC-019 UC-SAL-03 deliveredQuantity 列存在零 writer / P2-RC-020 UC-SAL-03 1 行×2 分批测试缺失 / P2-RC-021 UC-SAL-10 sales seam 并发测试缺失）；`:149`（A1.19 RC 交叉引用注记段，含 G5 MA2 :326-327 陈旧文本对账结论）。
  - 报告 §8 过程纪律自检实测：`docs/audits/nop-compliance-checker.sh` 全 19 规则 actual ≤ baseline（0 漂移，本审计无生产代码变更无回归风险）。
  - 日志条目：`docs/logs/2026/08-03.md:5-11`（EXECUTE 摘要 + 2 Phase 完成记录 + 产出文件清单 + bookkeeping 声明 + 独立结束审计 subagent ses_03bcaca9 标注）。
  - roadmap 同步：`docs/backlog/requirement-compliance-roadmap.md` MA1 表 A1.19 行 Status `todo`→`done`（日志 :11 记录）。
  - MA2 复用声明（§9）：复用 `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（UC-SAL-10 库存域乐观锁 PASS sustained）+ `2026-07-28-0400-arm-ma2-sales-state-machine.md`（出库级校验 PASS）+ `2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`，仅补需求视角差异，不重跑历史行为审计。

Follow-up:

- finding 修复实施经 MR1（R1.0 展开 RC-R1.n）— P2-RC-019/020/021 三项纯 Processor 修复或测试补充（预授权类目自动执行），reuse P1-RC-020 修复行与 A1.18 合并。G1 L1↔L2/L3 真相源冲突（UC-SAL-02 订单级 vs 出库级）修复方向须人工裁决。
- 5 项静态存疑点（UC-SAL-10 sales→inv Facade seam 真实并发运行时 / UC-SAL-02 订单级校验缺失运营频度 / 负库存配置下并发结果 / deliveredQuantity 查询实际返回值 / UC-SAL-03 1 行×2 分批运行时验证）交 MA4/A4.1 运行时展开。
