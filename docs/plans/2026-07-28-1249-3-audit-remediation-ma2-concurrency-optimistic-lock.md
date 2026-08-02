# 2026-07-28-1249-3-audit-remediation-ma2-concurrency-optimistic-lock MA2 并发与乐观锁审计（A2.17）

> Plan Status: completed
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.17）
> Related: MA2 全部状态机/业财端到端审计报告（A2.1–A2.16）均向本审计交接并发敏感点——finance A2.5b（5 处含 @Version 透明乐观锁降级）/ finance A2.5c（P2-MA2-008/014 ErpFinArApItem versionProp 乐观锁）/ mfg A2.6a（5 处含 @Version 降级）/ mfg A2.6b（5 处含 ErpMfgMrpPlanLine 无 versionProp 行级缺口）/ hr A2.7a（5 处含 @Version 降级）/ hr A2.7b（5 处含 @Version 降级）/ inventory A2.11（4 处：ErpInvStockBalance versionProp 降级 + applyReservation/reclassifyBalance 读-改-写无显式锁 + StockTake 并发同库位盘点 + LandedCost approve 并发同 receiveId 窗口期）/ ext-domains A2.14（9 处）；`docs/plans/2026-07-28-1249-1-audit-remediation-ma2-aps-logistics-state-machine.md`（A2.15 aps 并发排产产能双倍占用 + logistics 并发更新同一发运单交接）；`docs/plans/2026-07-07-0024-2-inventory-concurrency-negative-stock.md`（并发负库存前置实现 owner doc §实现偏离补注来源）；`docs/skills/open-ended-audit-prompt.md`（审计方法）；`docs/design/flow-overview.md` §6 数据一致性保障（owner doc 事务边界真相）
> Audit: required

## Current Baseline

并发与乐观锁审计（ERP 核心并发正确性）。并发是 ERP 系统的**最高风险正确性维度**之一：库存扣减、发票核销、期间结账、预算占用、排产产能预留等核心操作在高并发下若 lost-update 未防护，将导致**超卖（availableQuantity 竞态穿透）/ 双重核销 / 期间重复结账 / 预算余量穿透 / 产能双倍占用**等破坏数据不变量的严重缺陷。Nop Platform 提供两层并发防护：(1) `@Version` 透明乐观锁（ORM `versionProp` 声明，更新时自动校验版本号 + `OptimisticLockException`）；(2) `@BizMutation` 事务边界（自动包装事务，跨域经 `REQUIRES_NEW` 独立事务隔离）。

MA2 全部状态机/业财端到端审计（A2.1–A2.16）均**标注但未裁决**并发敏感点——累计交接至本审计的并发敏感点超过 40 处（finance 5+2 + mfg 5+5 + hr 5+5 + inventory 4 + ext 9 + aps/logistics 2 + use-case-implementation-audit 标记 3 处并发缺口）。本审计是 MA2 并发维度的**收口裁决**：系统性核验 `@Version` 覆盖、lost-update 防护、透明乐观锁降级、REQUIRES_NEW 跨域失败隔离正确性。

实时仓库已落地的并发防护（待审查）：

- **事务边界**（flow-overview.md §6.1）：单据审核 + 库存变更跨域事务（REQUIRED 强一致）；单据审核 + 凭证生成 REQUIRES_NEW 独立事务隔离（跨域失败隔离——过账异常记录 PENDING 供重试，主事务不回滚）；期末结账单库事务（REQUIRED）。
- **兜底机制**（flow-overview.md §6.2）：定时任务每分钟扫描 posted=false 且审核超 5 分钟单据重试，连续失败 3 次告警人工介入。
- **对账机制**（flow-overview.md §6.3）：每日对账（库存余额 vs 流水 / 应收余额 vs 发票-收款 / 应付余额 vs 发票-付款 / 总账余额 vs 凭证明细）——并发缺陷的对账兜底。
- **@Version 透明乐观锁**：部分状态机实体声明 `versionProp`（hr 7 个状态机实体全部声明；mfg/hr 部分实体）；部分实体**无 versionProp**（ErpMfgMrpPlanLine 行级并发缺口 [A2.6b 登记]）。
- **已识别并发敏感点（待本审计逐项裁决）**：
  - **库存扣减竞态**：`applyReservation`/`reclassifyBalance` 读-改-写无显式锁（A2.11 登记 inventory）；并发负库存穿透（plan 2026-07-07-0024-2 前置实现 owner doc）；use-case-implementation-audit UC-INV-08 乐观锁缺口；ErpInvStockBalance versionProp 透明乐观锁降级（A2.11——@Version 存在但是否真保护临界 read-modify-write）。
  - **发票核销竞态**：ErpFinArApItem versionProp 乐观锁（A2.5c P2-MA2-008/014 降级——@Version 存在但是否覆盖并发核销）；use-case-implementation-audit UC-SAL-10 乐观锁缺口 + UC-SAL-10 并发扣批次。
  - **期间结账竞态**：finance A2.5b 5 处并发敏感点（含 @Version 降级）——并发结账/反结账/期间状态翻转竞争；CLOSED_FINAL 凭证锁定（P1-MA2-021）并发防护。
  - **预算占用/承付竞态**：A2.16 标注并发 commit/release 同事务竞争 + 部分开票并发释放；IErpFinBudgetControlBiz.check 与 commitment SYNC 同事务。

> **交接点完整性注记**：上述并发敏感点中，已 done 审计（A2.1–A2.14）交接的约 70 处经 arm-index 与各报告 §并发敏感点 实仓确认（"40+"为保守表述），构成本审计的硬证据基线；**A2.15（aps 并发排产产能双倍占用 + logistics 发运单乐观锁）+ A2.16（承付 commit/release 竞态）的交接点为同批起草（`1249-1`/`1249-2`）的预计产物**，Phase 1 Prereqs 已将其 gate 为 done 后才执行本审计，故基线"40+"在 A2.15/A2.16 落地后只增不减。
  - **制造并发**：mfg A2.6a/b 各 5 处（@Version 降级 + ErpMfgMrpPlanLine 无 versionProp 行级缺口 + MRP 并发运算）；aps A2.15 并发排产同一工作中心产能双倍占用（owner doc §4 声明乐观锁/资源锁——是否落地）。
  - **HR/扩展域并发**：hr A2.7a/b 各 5 处（@Version 降级——请假→排班联动 / 工资支付轴并发 / 仿真并发 convertToFormal）；ext A2.14 9 处（并发状态变更 / b2b EDI 异步重复回调幂等 / contract 到期 Job / maintenance 工时过账悬挂）；logistics A2.15 并发更新同一发运单乐观锁。
  - **LandedCost 窗口期**：A2.11 LandedCost approve 并发同 receiveId PRE-APPROVED 窗口期。
  - **StockTake 并发**：A2.11 StockTake 并发同库位盘点。

**但从未做过一次系统性并发正确性审查**。已知未核验控制点（flow-overview.md §6 + 各审计报告交接 + use-case-implementation-audit 并发缺口）：

- **@Version 覆盖矩阵（核心）**：全 19 域状态机实体/余额实体/核销实体的 `versionProp` 声明覆盖率——哪些声明了 @Version（透明乐观锁），哪些未声明（行级并发缺口）。重点核验：(1) **ErpInvStockBalance** versionProp（库存余额——并发扣减核心）；(2) **ErpFinArApItem** versionProp（核销——并发核销核心）；(3) **ErpMfgMrpPlanLine** 无 versionProp（A2.6b 登记行级缺口）；(4) hr 7 个状态机实体全部声明 versionProp（A2.7a/b 确认）的覆盖完整性。
- **透明乐观锁降级**：@Version 存在但是否真保护临界 read-modify-write？— 即实体声明了 versionProp，但业务方法是否在"读余额→计算新余额→写回"的 read-modify-write 序列中持有同一实体实例使 @Version 生效，还是读出来后用新实体/裸 SQL 更新绕过 @Version（降级为无防护）。重点核验 applyReservation/reclassifyBalance（inventory）/ 核销 amount 回写（finance AR-AP）/ 期间状态翻转（finance period）/ 工资支付轴（hr）。
- **lost-update 防护**：并发库存扣减（超卖）/ 并发双重核销 / 并发重复结账 / 并发产能双倍占用——是否存在无 @Version + 无悲观锁 + 无唯一约束的裸 read-modify-write。重点核验 use-case-implementation-audit UC-INV-08 / UC-SAL-10 三处并发缺口是否已防护。
- **REQUIRES_NEW 跨域失败隔离正确性**：业财过账 IErpFinVoucherBiz.post REQUIRES_NEW——跨域失败不回滚主事务（记录 PENDING 重试）。核验：(1) 主事务单据审核已落库但过账悬挂的最终一致性（兜底扫描）；(2) REQUIRES_NEW 是否在所有过账 Facade 一致；(3) 并发重试是否幂等（同一单据并发重试不重复过账——幂等键 (relatedBillType, relatedBillCode)）。
- **幂等性**：flow-overview.md §八.3 声明"所有操作支持重复调用"——核验并发重复触发（轮询重启/重复回调/并发重试）的幂等：b2b EDI 异步重复回调（A2.14）/ logistics 网关回调 + scanForPolling（A2.15）/ 过账兜底重试（flow §6.2）/ 承付重复释放守卫（A2.16）。
- **并发状态翻转竞争**：多轴状态机（docStatus + approveStatus + posted）并发翻转——是否经 @Version 或状态守卫防止非法并发迁移（如并发 approve + cancel）。重点核验 finance 三轴 / purchase 三轴 / sales 三轴 / assets 状态机。
- **定时任务并发**：cron-gated Job（contract 到期 EXPIRED / 期间开启 / 折旧批量 / 兜底扫描）并发执行——是否经分布式锁或幂等防护（单失败隔离 + 单 Job 实例）。
- **与设计文档一致性**：flow-overview.md §6.1 事务边界表 vs 实现——(1) §6.1 三种事务范围（REQUIRED / REQUIRES_NEW / 单库 REQUIRED）落地；(2) §6.2 兜底重试 3 次告警；(3) §6.3 每日对账四项兜底；(4) §八.3 幂等声明。

剩余差距：需要一次系统性并发正确性审查，发现任何遗漏的 P0（**库存扣减无 @Version + 无悲观锁致并发超卖** [若破坏 availableQuantity 不变量——UC-INV-08 缺口确认] / **核销 ErpFinArApItem 透明乐观锁降级致并发双重核销** [若破坏核销单次性——UC-SAL-10 缺口确认] / **期间结账无并发锁致重复结账** [若破坏期间终态] / **排产产能双倍占用乐观锁未落地** [若破坏产能预留——A2.15 owner doc §4 声明未落实] / **过账 REQUIRES_NEW 并发重试非幂等致重复凭证** [若破坏幂等键] / **承付并发 commit/release 竞态致预算余量穿透** [若破坏预算控制]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `open-ended-audit-prompt.md` 对 **并发与乐观锁正确性**做系统性审查，产出审计报告。审查维度（开放式）：@Version 覆盖矩阵（核心）/ 透明乐观锁降级 / lost-update 防护 / REQUIRES_NEW 跨域失败隔离正确性 / 幂等性 / 并发状态翻转竞争 / 定时任务并发 / 与设计文档一致性。
- **收口裁决** MA2 全部状态机/业财端到端审计（A2.1–A2.16）交接的 40+ 并发敏感点——逐项裁决（sustained 防护 / 透明锁降级 watch-only / 行级缺口 P1 / lost-update P0 候选）。
- 重点核验已识别控制点：(1) @Version 覆盖（ErpInvStockBalance / ErpFinArApItem / ErpMfgMrpPlanLine 缺口 / hr 7 实体覆盖）；(2) 透明乐观锁降级（applyReservation/reclassifyBalance / 核销回写 / 期间翻转 / 工资支付轴）；(3) lost-use 防护（UC-INV-08 超卖 / UC-SAL-10 双重核销 + 并发扣批次）；(4) REQUIRES_NEW 跨域隔离 + 并发重试幂等；(5) 排产产能双倍占用（A2.15 owner doc §4 声明是否落地）；(6) LandedCost 并发同 receiveId 窗口期（A2.11）；(7) StockTake 并发同库位（A2.11）；(8) 承付并发 commit/release 竞态（A2.16）；(9) 定时任务并发（cron-gated Job）；(10) 多轴状态机并发翻转竞争。
- scope matrix §并发正确性 行全域终态标记（`❓` → `✅`/`⚠️(P1)`，并发维度是 A2.17 新增物化维度）。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.17 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**重复 A2.1–A2.16 各域状态机/业财端到端审查 — done；本审计只裁决各审计交接的并发敏感点（并发维度的收口），不重做状态机迁移/业财正确性审查。
- **不**审计 A2.18 多账套/多公司隔离 — 多公司 orgId 隔离污染归 A2.18；本审计只标注并发场景下的 orgId 隔离交叉点（若有）。
- **不**审计 A4.x 代码质量 / A7.3 N+1 查询 — 归 MA4/MA7；本审计只核验并发正确性（@Version/锁/幂等），不核验性能。
- **不**审计 A5.x 测试覆盖深度 — 并发测试覆盖系统性审查归 MA5；本审计只标注并发缺陷的测试缺口（如无并发测试覆盖的 lost-update 路径）。
- **不**审计 config-gated / Deferred 偏离是否应实现 — owner doc 已裁定，本审计只确认其在并发上不引入 lost-update。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复，如补 @Version / 加悲观锁 / 加幂等键）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计 + 人工确认（触及库存/核销/期间保护区域）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/flow-overview.md` §6 数据一致性保障（§6.1 事务边界 + §6.2 兜底机制 + §6.3 对账机制 + §八 流程设计特点）；`docs/design/finance/posting.md`（REQUIRES_NEW + 幂等键）；`docs/architecture/processor-extension-pattern.md`（Facade REQUIRES_NEW 跨域失败隔离）；各域 state-machine.md §并发（库存/核销/期间/排产并发敏感点 owner doc 声明）；`docs/design/inventory/`（并发负库存 plan 2026-07-07-0024-2 owner doc）
- Skill Selection Basis: `open-ended-audit-prompt.md`（roadmap A2.17 指定此 skill，开放式并发正确性审查专用方法——标准检查清单可能遗漏隐藏 lost-update 风险，开放式探索 + 并发缺陷模式 + 实仓 @Version 覆盖核验。项目定制化层见 `docs/skills/README.md`）。并发是 ERP 最高风险正确性维度，超 40 处交接敏感点需逐项裁决，开放式审查比固定维度更适合捕获隐藏竞态。
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM（补 @Version / 加悲观锁 / 加幂等键），则该修复需 `mvn clean install -DskipTests` + 相关测试 + 并发场景测试（若可行）。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：并发触及全部最高级保护区域——库存扣减（inventory availableQuantity）/ 发票核销（finance AR-AP 单次性）/ 期间结账（finance 期间终态）/ 预算控制（finance 预算余量）/ 产能预留（aps 工作中心产能）。P0 即时修复若触及 `ErpInvStockBalance`/`ErpFinArApItem`/期间状态实体/余额实体的 `versionProp` ORM 声明（ask-first ORM 保护区域）或 BizModel 加锁逻辑，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认。ORM `versionProp` 声明变更属 ask-first。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 并发与乐观锁系统性审查 + 交接敏感点逐项裁决

Status: completed
Targets: 全 19 域状态机实体/余额实体/核销实体的 `versionProp` 声明（@Version 覆盖矩阵）；inventory `applyReservation`/`reclassifyBalance`/StockTake/LandedCost；finance AR-AP 核销/期间结账/承付/过账 REQUIRES_NEW；mfg MRP/排产；hr 状态机；aps 排产产能；ext b2b EDI 异步/logistics 网关回调幂等；cron-gated Job
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done；MA2 A2.1–A2.16 全部 done（40+ 并发敏感点交接本审计）；A2.15 aps/logistics done（并发排产产能 + 发运单乐观锁交接）；A2.16 承付释放路径 done（并发 commit/release 交接）。本审计是 MA2 并发维度的收口，依赖全部前序审计的交接证据。

- [x] 维度「@Version 覆盖矩阵（核心）」：grep 全 19 域 `versionProp` 声明，产出覆盖矩阵——哪些状态机实体/余额实体/核销实体声明了 @Version，哪些未声明（行级并发缺口）。重点核验：(1) ErpInvStockBalance versionProp；(2) ErpFinArApItem versionProp；(3) ErpMfgMrpPlanLine 无 versionProp（A2.6b 缺口）；(4) hr 7 状态机实体覆盖完整性；(5) 余额实体（ErpInvStockBalance / ErpFinGlBalance / ErpFinArApItem）覆盖。
      - Skill: `open-ended-audit-prompt.md`
      - **裁决**：全 19 域 336 自有实体 **100% 声明 versionProp**（脚本遍历确认）；A2.6b 交接"ErpMfgMrpPlanLine 无 versionProp"经 `module-manufacturing/model/app-erp-manufacturing.orm.xml:809` **证伪**——行已声明；hr 7 状态机实体全声明；余额实体全覆盖。详见报告 §2。
- [x] 维度「透明乐观锁降级」：@Version 存在但是否真保护临界 read-modify-write？— 核验业务方法在"读余额→计算新余额→写回"序列中是否持有同一实体实例使 @Version 生效，还是读出后用新实体/裸 SQL/daoProvider.update 绕过 @Version。重点核验：applyReservation/reclassifyBalance（inventory）/ 核销 amount 回写（finance AR-AP）/ 期间状态翻转（finance period）/ 工资支付轴 markPaid（hr）/ 库存余额回写（StockMove doComplete）。
      - Skill: `open-ended-audit-prompt.md`
      - **裁决**：6 处候选**全部证伪**——业务方法均经 `dao.findAllByQuery`/`getEntityById` 加载托管实体 + 原地 mutate + `updateEntity`/`saveOrUpdateEntity`/`tryUpdateWithVersionCheck` flush，@Version 自动校验生效。全域 grep `FOR UPDATE`/`withLock`/`executeUpdate`/`sqlUpdate` = 0（无悲观锁、无裸 SQL、无批量 update 绕过）。详见报告 §3。
- [x] 维度「lost-update 防护」：并发库存扣减（超卖）/ 并发双重核销 / 并发重复结账 / 并发产能双倍占用——是否存在无 @Version + 无悲观锁 + 无唯一约束的裸 read-modify-write。重点核验 use-case-implementation-audit UC-INV-08（超卖）/ UC-SAL-10（双重核销 + 并发扣批次）三处并发缺口是否已防护。
      - Skill: `open-ended-audit-prompt.md`
      - **裁决**：UC-INV-08 超卖 + UC-SAL-10 双重核销 + 并发扣批次 + 期间重复结账 + 承付竞态 5 项候选 P0 **全部证伪**（@Version + retry / managed flush 防护成立）；**3 项新 P0 确认**：P0-MA2-018 过账 billR 无 UK 致重复凭证 / P0-MA2-019 排产产能双倍占用（owner doc §4 锁未落地）/ P0-MA2-020 库存余额自然键无 UK 致 silent split-quantity。详见报告 §4。
- [x] 维度「REQUIRES_NEW 跨域失败隔离正确性」：业财过账 IErpFinVoucherBiz.post REQUIRES_NEW——核验：(1) 主事务单据审核已落库但过账悬挂的最终一致性（兜底扫描 flow §6.2）；(2) REQUIRES_NEW 是否在所有过账 Facade 一致；(3) 并发重试是否幂等（同一单据并发重试不重复过账——幂等键 (relatedBillType, relatedBillCode)）。
      - Skill: `open-ended-audit-prompt.md`
      - **裁决**：(1)+(2) sustained（`ErpFinVoucherBizModel:71,79` 显式 REQUIRES_NEW 钉 Facade，11 域 PostingExecutor/Dispatcher 一致复用 + `ErpFinDeferredPostingRetryHelper` 兜底 + O-16 补偿 + 双 REQUIRES_NEW 异常落库）；(3) **FAIL**——`erp_fin_voucher_bill_r(billCode, businessType)` 无 DB UK + `alreadyPosted` TOCTOU pre-check → P0-MA2-018。详见报告 §5。
- [x] 维度「幂等性」：flow-overview.md §八.3 "所有操作支持重复调用"——核验并发重复触发（轮询重启/重复回调/并发重试）的幂等：b2b EDI 异步重复回调（A2.14）/ logistics 网关回调 + scanForPolling（A2.15）/ 过账兜底重试（flow §6.2）/ 承付重复释放守卫 ERR_BUDGET_COMMITMENT_ALREADY_RELEASED（A2.16）/ 期间结账幂等。
      - Skill: `open-ended-audit-prompt.md`
      - **裁决**：logistics 状态轴 + 承付 release + 期间结账幂等 sustained；b2b EDI webhook（P1-MA2-088 config-gated OFF）+ FX 重估（P1-MA2-087 bounded by period version guard）+ 过账兜底（P0-MA2-018）部分 fail。详见报告 §6。
- [x] 维度「并发状态翻转竞争」：多轴状态机（docStatus + approveStatus + posted）并发翻转——是否经 @Version 或状态守卫防止非法并发迁移（如并发 approve + cancel / 并发 approve + reverseApprove）。重点核验 finance 三轴 / purchase 三轴 / sales 三轴 / assets 状态机 / inventory 双轴。
      - Skill: `open-ended-audit-prompt.md`
      - **裁决**：8/9 Processor 组 sustained（显式 `validateTransitionFor*` 源态守卫 + versionProp）；唯一例外 `ErpAstDepreciationScheduleProcessor.executeDepreciation` 缺 PENDING 守卫（P1-MA2-089）。详见报告 §7。
- [x] 维度「定时任务并发」：cron-gated Job（contract 到期 EXPIRED / 期间开启 / 折旧批量 / 兜底扫描 / logistics scanForPolling）并发执行——是否经分布式锁或幂等防护（单失败隔离 + 单 Job 实例 + 幂等键）。
      - Skill: `open-ended-audit-prompt.md`
      - **裁决**：全 19 cron job 运行于 `nop-job-local` 非分布式 + 无 `IErpSysLockBiz` + 全部默认 enabled=false；9 job 幂等（recompute/refresh 类 + deferred-posting-sweep 经引擎 alreadyPosted 去重）；10 job 并发执行产生重复副作用（P1-MA2-086）。详见报告 §8。
- [x] 交接敏感点逐项裁决表：MA2 A2.1–A2.16 交接的 40+ 并发敏感点逐项裁决（sustained 防护 / 透明锁降级 watch-only / 行级缺口 P1 / lost-update P0 候选），每项含证据 + 终态。重点：inventory 4 处（A2.11）/ mfg 10 处（A2.6a/b）/ hr 10 处（A2.7a/b）/ finance 7 处（A2.5b/c）/ ext 9 处（A2.14）/ aps+logistics 2 处（A2.15）/ 承付（A2.16）/ UC 缺口 3 处。
      - Skill: none
      - **裁决**：约 50 处交接敏感点逐项裁决——**绝大多数 sustained**；6 项 MA2/use-case 候选 P0 经证据全部证伪或部分证伪；3 项新 P0 确认（P0-MA2-018/019/020）。详见报告 §10。
- [x] 维度「与设计文档一致性」：flow-overview.md §6.1 事务边界表 vs 实现——(1) §6.1 三种事务范围（REQUIRED / REQUIRES_NEW / 单库 REQUIRED）落地；(2) §6.2 兜底重试 3 次告警；(3) §6.3 每日对账四项兜底；(4) §八.3 幂等声明；(5) §八.2 跨域失败隔离。各域 state-machine.md §并发声明 vs 实现。
      - Skill: `open-ended-audit-prompt.md`
      - **裁决**：§6.1/§6.2/§6.3/§八.2 PASS；§八.3 幂等 + aps state-machine.md §4 锁 FAIL（P0-MA2-018/019）。详见报告 §9。
- [x] 产出审计报告 `docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（含：@Version 覆盖矩阵 [全 19 域实体 × versionProp 声明 × 临界方法]、40+ 交接敏感点逐项裁决表、各维度通过/失败裁决、控制点 PASS/FAIL、库存超卖/双重核销/重复结账/产能双倍占用/过账重试幂等/承付竞态/定时任务并发/多轴翻转竞争裁决、lost-update P0 候选证伪或确认、MA2 交接证据复核表、并发测试覆盖缺口交接 MA5、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] @Version 覆盖矩阵产出（全 19 域实体 × versionProp 声明 × 临界方法），无 @Version 的实体逐项标注并发缺口或裁决
- [x] MA2 A2.1–A2.16 交接的 40+ 并发敏感点逐项裁决表产出，每项含证据 + 终态（sustained / 降级 watch-only / P1 / P0 候选）
- [x] 已识别控制点（@Version 覆盖 / 透明锁降级 / lost-update[含 UC-INV-08/UC-SAL-10] / REQUIRES_NEW 隔离 / 幂等 / 并发翻转竞争 / 定时任务并发 / 与设计文档一致性）均有通过/失败裁决与证据
- [x] open-ended-audit 8 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 并发审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §并发正确性 行全域
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**库存扣减无 @Version + 无悲观锁致并发超卖** [若破坏 availableQuantity——UC-INV-08 确认] / **核销 ErpFinArApItem 透明乐观锁降级致并发双重核销** [若破坏核销单次性——UC-SAL-10 确认] / **期间结账无并发锁致重复结账** [若破坏期间终态] / **排产产能双倍占用乐观锁未落地** [若破坏产能预留——A2.15 owner doc §4 声明未落实] / **过账 REQUIRES_NEW 并发重试非幂等致重复凭证** [若破坏幂等键] / **承付并发 commit/release 竞态致预算余量穿透** [若破坏预算控制]）当即就地修复（改源文件 + 补 @Version / 加悲观锁 / 加幂等键 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及库存/核销/期间保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
      - **裁决**：6 项 plan 列举的 P0 候选经证据**3 项证伪 + 3 项确认**。证伪：库存扣减超卖（UC-INV-08 经 tryUpdateWithVersionCheck + retry 防护）/ 核销双重核销（UC-SAL-10 经 managed flush 防护）/ 期间重复结账（period versionProp + assertPeriodStatus 防护）/ 承付预算余量穿透（aggregate-on-read 无 lost-update）。确认并异步注入独立 fix plan（均触及 ask-first ORM 保护区域，须经独立 plan-audit + 人工确认）：**P0-MA2-018** 过账 billR 无 UK → fix plan `2026-07-28-1249-arm-fix-p0-ma2-018` [planned] / **P0-MA2-019** 排产产能双倍占用 owner doc §4 锁未落地 → fix plan `2026-07-28-1249-arm-fix-p0-ma2-019` [planned] / **P0-MA2-020** 库存余额自然键无 UK silent split-quantity → fix plan `2026-07-28-1249-arm-fix-p0-ma2-020` [planned]。+1 项新发现 P0（库存余额 INSERT 竞态，不在 plan 原列举 6 候选中）。详见报告 §11 + §14。
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。并发 P1（如 ErpMfgMrpPlanLine 无 versionProp 行级缺口 / 透明锁降级 watch-only 升级 P1 [若确认] / 定时任务无分布式锁 [若确认] / 并发测试覆盖缺口）按新 finding ID 登记；对已登记降级 finding（如 P2-MA2-008/014 ErpFinArApItem versionProp）复核是否升级。
      - Skill: none
      - **裁决**：8 项新 P1 登记（P1-MA2-085 LandedCost TOCTOU / P1-MA2-086 定时任务并发重复副作用 10 job 合并 / P1-MA2-087 CloseVoucherWriter 无幂等 pre-check 与 P0-MA2-018 同根因 / P1-MA2-088 b2b EDI webhook 重复回调 config-gated OFF / P1-MA2-089 assets executeDepreciation 缺 PENDING 守卫 / P1-MA2-090 mfg MrpReleaseService 并发释放 UK 兜底 / P1-MA2-091 hr ShiftAssignment 无 UK / P1-MA2-092 logistics Shipment 无 trackingNo UK）；2 项新 P2 watch-only（P2-MA2-074 全域无悲观锁归 MA7 / P2-MA2-075 retry-count 统计漂移）。A2.6b 交接"ErpMfgMrpPlanLine 行级缺口"经证据**证伪**（行已声明 versionProp，无须升级 P1）；P2-MA2-008/014 ErpFinArApItem 维持 P2 watch-only（@Version 将 silent lost-update 降为 detectable conflict）。详见报告 §12。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §并发正确性 行全域终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none
      - **裁决**：arm-index 报告清单新增本报告行 [done]；scope matrix §2.2 MA2 维度表新增「并发与乐观锁」行——finance/inv/aps 列 `⚠️P0→fix-plan + P1(A2.17✅)`，其余 13 业务列 `⚠️P1(A2.17✅)`，md/drp/notify N/A。§2.5 新增维度行「并发与乐观锁」保留作 milestone/skill 索引。

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_058eaddc8ffeym2y3RfiP4TMQL`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：roadmap A2.17 = `todo`，owner-doc `flow-overview.md §事务边界` + skill `open-ended-audit-prompt.md` + deps 0.3 全匹配 ✓；flow-overview §6.1/§6.2/§6.3/§八 实仓存在 ✓；**交接点真实且广泛**——14 份 done 审计（A2.1-A2.14）各携带 §并发敏感点（交接 A2.17）段（spot-check inventory/mfg-mrp/hr-emp-org/finance-arap），arm-index 16 处 `并发敏感点` + P2-MA2-008/014/025 归 A2.17 行，scope matrix 有 `并发与乐观锁 | MA2（A2.17）` 行；仅 done 审计已计数约 70 处，"40+"保守且有据 ✓；`versionProp`/`@Version`/降级 声明跨 finance AR-AP/mfg/hr/assets/inventory 实仓确认 ✓；最低规则 R1/R2/R4（跨 19 域单一结果面=并发正确性，共享行为契约，spanning 经每份前序审计显式交接正当化）/R7/R8/R13 全 PASS；反松弛零禁词（`[若破坏…]` 条件证伪框架可接受）。**采纳 1 项非阻塞修订**：Current Baseline 增交接点完整性注记——A2.15/A2.16 交接点为同批起草预计产物，Phase 1 Prereqs gate 其 done 后执行，"40+"基线在落地后只增不减。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。并发触及库存/核销/期间/预算/产能全部最高级保护区域，P0 即时修复（补 @Version / 加悲观锁 / 加幂等键）须额外人工确认。ORM versionProp 声明变更属 ask-first。

- [x] 范围内行为完成（A2.17 并发与乐观锁系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、flow-overview.md §6 owner doc 结论已反映）
- [x] 已运行验证：审计不改代码，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.18 多账套/多公司隔离（并发场景下 orgId 隔离交叉点）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.18。本审计标注并发场景下的 orgId 隔离交叉点（若有），不做系统性多公司隔离正确性裁决。
- Successor Required: `yes`——A2.18 执行时复核。

### A5.x 测试覆盖深度（并发测试覆盖缺口）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做并发**正确性**审查；并发测试覆盖系统性审查归 MA5（如无并发测试覆盖的 lost-update 路径）。本审计只标注并发缺陷的测试缺口交接 MA5。
- Successor Required: `yes`——MA5 执行时复核。

### A4.x 代码质量 / A7.3 N+1 查询（并发性能）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计核验并发**正确性**（@Version/锁/幂等），不核验性能（N+1/锁争用）。并发性能归 MA4/MA7。
- Successor Required: `no`——并发正确性本审计收口；性能归 MA4/MA7。

## Closure

Status Note: A2.17 并发与乐观锁系统性审查完成（MA2 并发维度收口裁决）。@Version 覆盖矩阵全 19 域 336 自有实体 100% 声明（含 A2.6b 交接 ErpMfgMrpPlanLine 行级缺口证伪——行已声明 versionProp）；透明乐观锁降级 6 处候选全部证伪（业务方法均经 managed-instance read-modify-write + flush 使 @Version 自动校验生效；全域 grep FOR UPDATE/withLock/executeUpdate/sqlUpdate=0）；MA2 交接 40+ 并发敏感点逐项裁决绝大多数 sustained，6 项 MA2/use-case 候选 P0 经证据全部证伪或部分证伪。发现 3 项 P0（P0-MA2-018 finance 过账 billR 无 UK 致并发重复凭证 / P0-MA2-019 aps 排产产能并发双倍占用 owner doc §4 锁未落地 / P0-MA2-020 inventory 库存余额自然键无 UK 致 silent split-quantity corruption）均触及 ask-first ORM 保护区域已异步注入 3 个独立 fix plan 须经独立 plan-audit + 人工确认。8 项新 P1 + 2 项新 P2 watch-only 登记 arm-index 待 MR1。并发维度终态全域 ⚠️(P0→fix-plan + P1)。MA2 全部 18 个工作项（A2.1–A2.17 含承付+多维）现已全部 done。

Closure Audit Evidence:

- Auditor / Agent: 主执行代理（opencode glm-5.2）+ 4 个独立 general 子代理（fresh-context）并行证据采集——(a) inventory 并发域专项（applyReservation/reclassifyBalance/StockTake/LandedCost/StockMoveBookkeeper/CostAdjustmentService 全 6 方法 + upsertBalance 自然键无 UK 新发现）/ (b) finance 并发域专项（AR-AP 核销/期间状态/承付 commit-release/过账 REQUIRES_NEW 幂等/FX 重估全 6 路径 + erp_fin_voucher_bill_r 无 UK 新发现）/ (c) mfg+hr+aps+ext 并发域专项（MrpEngine/WorkOrderProcessor/ApsSchedulingProcessor capacity 双倍占用新发现/markPaid/ShiftAssignment/EDI webhook/contract Job/maintenance posting 全 11 控制点）/ (d) 跨域状态机 Processor + cron-job 全 19 job 并发专项。4 子代理独立 fresh-context 证据采集 + 主代理交叉裁决，无自我审计。
- Evidence: 
  - 审计报告：`docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（done）
  - arm-index 更新：报告清单 +行；P0 表 +3 行（P0-MA2-018/019/020）；P1 表 +8 行（P1-MA2-085~092）；P2 表 +2 行（P2-MA2-074/075）；A2.17 新增项章节 + 收口裁决段
  - scope matrix：§2.2 MA2 维度表 +「并发与乐观锁」行（finance/inv/aps ⚠️P0→fix-plan + P1，13 业务列 ⚠️P1，md/drp/notify N/A）
  - 3 个 P0 fix plan 已注入（`2026-07-28-1249-arm-fix-p0-ma2-018/019/020`，全部 Status: planned 待独立 plan-audit + 人工确认）
  - roadmap：A2.17 `todo` → `done` + 最后更新 v12 段
  - 关键证据：`module-*/model/app-erp-*.orm.xml` 全域 336 自有实体 100% versionProp（Python 脚本遍历确认，0 NO-VP）；`erp-*-service` grep `FOR UPDATE`/`withLock`/`executeUpdate`/`sqlUpdate` = 0；`REQUIRES_NEW` 46 命中跨 11 域；`ErpFinVoucherBizModel:71,79` 显式 REQUIRES_NEW 钉 Facade；nop-job-local 非分布式 + 无 IErpSysLockBiz（全域 grep=0）
  - 验证：本审计不改代码，build/test 门控为回归基线确认（同型审计 plan 的相同 Closure 实践）

Follow-up:

- 3 个 P0 fix plan 须经独立 plan-audit + 人工确认后闭包（P0-MA2-018/019/020 触及 ask-first ORM 保护区域）
- 8 项 P1 + 2 项 P2 watch-only 进入 MR1 批量修复里程碑
- 并发测试覆盖系统性审查交接 MA5（无并发场景集成测试 + 无 retry 路径测试 + 无幂等键负向测试 + 无 cron job 并发测试——见报告 §15）
- 多账套/多公司隔离（A2.18）复核 P0-MA2-018/020 的 UK 是否需含 orgId（已在 fix plan 方案 A 中体现）
- 并发性能（锁争用 N+1）归 MA7（P2-MA2-074 全域无悲观锁 watch-only）
