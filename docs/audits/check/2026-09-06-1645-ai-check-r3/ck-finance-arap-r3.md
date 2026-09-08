# ck-finance-arap-r3 — finance「AR/AP 核销与坏账」fin-2 五维符合性审计报告（ai-check-r3 M1.2）

> 工作项：M1.2（U05 × 五维 × fin-2，冻结清单 §4 映射表第 2 行 / §3.2 切片登记；核销/坏账/费用报销抵扣切片）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `dc31a2555399a5de689ab177da3b45b0401a3da1`；脏面 = 仅 3 个未跟踪 plan 文件（`docs/plans/2026-09-08-2238-{1,2,3}-*.md`，mission-driver 同批生成——2238-2/2238-3 为姊妹只读审计计划，无并发写面），零生产路径脏面（脏树实跑 + 披露，MI.9 收官审计先例）。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U05 行 fin-2 列 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：核销/坏账/费用报销抵扣（`module-finance/erp-fin-{dao,service,web}` src/main 中 fin-2 面——`service/reconciliation`、`service/baddebt` 包 + `ErpFinArApItemBizModel`/`ErpFinBadDebtBizModel`/`ErpFinExpenseClaimBizModel(+Line)`/`ErpFinEmployeeAdvanceBizModel`/`ErpFinReconciliationBizModel(+Line)` 及核销 settle/offset、坏账 writeOff/recovery、报销抵扣通道 Processor 族）；owner docs `docs/design/finance/ar-ap-reconciliation.md` + `docs/design/finance/bad-debt.md`。DIM-B 焦点（§3.3）：核销聚合守卫/FX 对称回滚/坏账现态守卫。
> Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物。
> 共享代码边界（冻结清单 §3.2）：posting processor 族本体归 fin-1（已闭合）——本切片只审 fin-2 对凭证引擎的**消费侧调用点**；common 抽象族行为缺陷归 U20（本切片只审调用点）；聚合横切面归 U21；notify 派发子系统本体归 U11。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（fin-2 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点①②③⑧⑨：核销单/坏账单状态机 Bean 与 dict 值域 + 核销/坏账审批流）+ 维度⑮断言抽样 2 doc × 4 断言 | 反模式族全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0，时间取值 CoreMetrics / uuid 用 StringHelper）；checker 19 规则逐项 = M0.3 快照行零漂移；R6=2 两处 REQUIRES_NEW 注释豁免在位（fin-1 归属格，fin-2 零命中）；IDaoProvider/IOrmTemplate fin-2 命中处豁免理由注释齐备（`ReconciliationSettler` 纯算术编排层分工 / `PartnerBalanceUpdater` L22 机制 B plan 裁定 / `DualSideConsistencyChecker` L30-34 跨域只读 R 免 IBiz 管道 / `AbstractErpFinReconciliationProcessor` L124/L136 D2 边界场景 / `ErpFinBadDebtProcessor` slim-facade 范式）；`_gen`/`__XGEN_FORCE_OVERRIDE__` 零手改（module-finance git 脏面 0，82 命中全为 erp-fin-meta dict.yaml codegen 只读校验点）；聚合器含 `/erp/fin/auth/erp-fin.action-auth.xml`（E1 勘误路径）；15/15 维度无跳维（⑫指针 DIM-T）：核销聚合守卫（`validateLine` 方向/伙伴/OPEN/超核销/日期五守卫 + `validateAggregatedNotOver` 多行聚合 = P1-CK-fin2-003 复用）、FX 对称回滚（`reverseSettle(lines, fxPath)` 持久化证据分支 = P1-CK-fin2-002 复用）、坏账现态守卫（`assertItemStatusFor*` 四守卫 + settled 对称校验 + 残额断言 = P1-CK-fin2-004 复用）三焦点全数在位；⑧状态机 Bean 6 枚声明式（Reconciliation/BadDebt/ExpenseClaim/EmployeeAdvance 各 Document/Approval）+ ar-ap-status dict 5 值；⑨审批五动作族 per-mutation Processor + `erp-fin.bad-debt-write-off-require-approval` 门控默认 true；⑮2 doc × 4 断言 4/4 一致零漂移未触发扩样 | **finding**（归并态：0 新立；5 复用 + 12 归并，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + fin-2 面 = 核销单/坏账/费用报销/员工借款/辅助账页族 | `npm run validate:flux` step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；整体 exit 1 余项 = 325 条全部 `variant` dropdown-button 既有 stub 外部漂移族（fin 域 29 条同族，successor 在案，非本切片 finding）；flux-only：`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0；fin-2 面 16 页逐页走查——五族保留层 view.xml 全 `x:extends="_gen/…"`（bounded-merge 定制，main.page.yaml 纯 codegen 面）+ 手写工作台 `expense-claim/main.page.yaml`（`@query:ErpFinExpenseClaim__findPage` REST）+ 报表页 `report/ar-ap-aging.page.yaml`（`@query:ErpFinReport__renderHtml`）零 GraphQL；i18n-en 承载齐备（M0.4 矩阵 L122 MI.8 补 i18nEn 行在位）；E2E：fin-2 相关 spec（fin-bad-debt / fin-bad-debt-provision-reverse）PageObject 模式 + framework 选择器 0/0 干净 + `E2E_ENGINE` 缺省 flux 实证（`tests/e2e/pages/engine.ts` L8-11）+ GraphQL 调用经 runbook 非页面路径 API 断言豁免通道；`fin-period-close-wizard.visual.spec.ts` data-slot 命中属 fin-4 页面面（M1.4 格） | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + ar_ap_item/核销单/坏账/报销面 seed 一致性 | `TestErpSeedDataIntegrity` 4/4 全绿（BUILD SUCCESS，363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning）；`git status --porcelain _init-data/` 空（零 seed 变更）；资产清点 372 CSV + 1 SQL（= 冻结口径）；deploy `_seed_*.sql` 命中 cs/notify 两族均已在 seed-data.md 登记处聚合（fin 无 deploy seed，无第三态）；核销链一致性：`erp_fin_ar_ap_item` 6 行不变式自洽（4 SETTLED 双侧对称 settled=amount/open=0 + 2 OPEN settled=0），`erp_fin_reconciliation` 3 行 POSTED×2 联动金额逐一相等 + REVERSED×1 N-TERM 红冲终态零残留，bad_debt 2 行 DRAFT/REJECTED 无 WRITTEN_OFF 写穿，EA-2026-001 posted=true 联动 item 5 + 凭证 V6，EC-2026-001 posted=false 行内 seed 注记显式登记「GL 过账未 seed」（action 驱动范式一致非未履行重录）；P2P/O2C 8 voucher ↔ 8 billR 双射基线在 T0 仍成立 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + 关键业务流三行（核销+冲销/坏账计提核销收回/报销抵扣借款）+ FX 子路径覆盖 | `mvn test -pl module-finance/erp-fin-service` **533/0/0/0 全绿**（= known-good-baselines fin 533 计数零回归）；覆盖对账：ArApItem 3 方法（aging→TestErpFinAging 3 用例 / findOpenItems 族→HR 契约测试消费）、BadDebt 9 方法（三测试类 11+ 用例全覆盖）、ExpenseClaim（Approval/Posting/ExpenseOffsetAdvance）、EmployeeAdvance（Approval/CashRepay/CashRepayReversal/Posting 四类）、Reconciliation 9 方法（TestErpFinReconciliation 9 用例含 003/005 修复测试 + ReversePreview + _cases/reconciliation 16 用例含 001 修复测试 testByRatioMultiPaymentNoOverAllocation）、两 Line BizModel 裸 CRUD 零自定义；关键业务流三行全覆盖；快照纪律 `SnapshotTest.RECORDING` = 0 残留、delVersion 命中为审计行为断言合规；**缺口 1**：核销 FX 汇兑损益路径（`settleWithFx` + `reverseSettle(fxPath=true)`，config 默认 false）零测试断言（test+_cases grep = 0）→ **新立 P2-CK-fin2-018-r3**（见 §2.3） | **finding**（1 新立 P2-CK-fin2-018-r3） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT；170 baseline files，totals CAT1..4=0/0/209/1318，单向收紧成立）；`--self-test` **PASS**；fin 探针族（CAT-1 54 / CAT-2 20）维持清零零回归；WHITELIST fin 条目（批 1/2 全 5 条）抽样 4（≥3）：4/4 四要素齐备（路径/理由/owner doc 指针=i18n-compliance.md 准绳表 #5/CAT-3 + 裁决来源=plan 2026-09-07-0902-1 C1/C2①）且 HEAD 实仓复核登记准确（@Description×2 各 1 行 / 增值税等 3 字面量 / P_* 常量 12 处均在位）；`grep -L @Locale` *Errors.java = 空；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ai-check-index.md` §报告清单/§Finding 追踪（同域报告 `ck-finance-arap.md` C3.2 fin2 族 17 条逐一比对）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/`（无 fin2 同型独立登记）+ §Mission 基线快照（R2b/R12 等命中均为已裁决偏离）+ fin-1 r3 报告 §2.4 归属标注（P3-CK-fin2-012 跨切片站点已由 fin-1 承接）。**本轮新立 1 条**（`P2-CK-fin2-018-r3`）；历史 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 5 条

| 原 ID | 修复在位证据（T0 = HEAD `dc31a2555`） |
| --- | --- |
| P1-CK-fin2-001（BY_RATIO 分母不刷新 + 尾差守卫反向） | F2.2 在位：`AutoReconciliationEngine` L187-188 分母每笔付款迭代前按剩余 open 重算 + L218-223 尾差守卫 subtract 方向修正；修复测试 `testByRatioMultiPaymentNoOverAllocation` 等在 `_cases/reconciliation/TestErpFinAutoReconciliation/` |
| P1-CK-fin2-002（FX 红冲不对称回滚） | F2.2 在位：`ReconciliationSettler.reverseSettle(lines, fxPath)` L104-129 按 `settledSource × item.exchangeRate` 重演回滚 + `ErpFinReconciliationReverseProcessor` L28-32 以持久化证据（head.fxGainLoss ≠ 0）判路，非 FX 路径逐字节保持原行为 |
| P1-CK-fin2-003（post 不聚合多行共享 item） | F2.2 在位：`ErpFinReconciliationPostProcessor.validateAggregatedNotOver` L41-44/L70-102 按 invoice/paymentItemId 分组聚合 assertNotOver；修复测试 `testMultiLineSharedItemAggregatedRejected` 在案 |
| P1-CK-fin2-004（坏账执行体不校验辅助账现态） | F2.2 在位：`ErpFinBadDebtProcessor` 四现态守卫 `assertItemStatusFor{WriteOff,Recovery,ReverseOfWriteOff,ReverseOfRecovery}` L447-474 + executeWriteOff L182 / executeRecovery L218 / executeReverseApprove L131-135 接线 + executeRecovery settled 对称校验 L220-224 + executeWriteOff 残额后置断言 L185-192 |
| P2-CK-fin2-005（cancelOnReverse 不守卫已核销项） | F2.2 在位：`ErpFinArApItemGenerator.cancelOnReverse` L136-141 settled>0 守卫（拒绝并提示先 reverse 核销单）+ `ReconciliationSettler.applySettlement` L136-140 reverse 对 CANCELLED 拒绝；修复测试 `testReverseSettleOnCancelledItemRejected` 在案 |

### 2.2 归并（同型 open 追加证据至原 ID）— 12 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-fin2-006 | 坏账/报销抵扣链改写辅助账 open 后仍零 partner 余额刷新——`ErpFinBadDebtProcessor`/`AdvanceOffsetOrchestrator` grep `partnerBalanceUpdater` = 0 命中 |
| P2-CK-fin2-007 | `BadDebtProvisionService.findReceivableOpenItems` L289-297 仍仅 direction+status 过滤，无 orgId/acctSchemaId 隔离（多账套计提基础仍全局） |
| P2-CK-fin2-008 | `ErpFinReconciliationRunAutoReconciliationProcessor.isAutoReconcileEnabled` 关闭时仍整批抛 `ERR_AUTO_RECON_DISABLED` + 单 `@BizMutation` 全量原子（L38-41），记录级重试模型未落地 |
| P2-CK-fin2-009 | `ErpFinBadDebtApproveProcessor` 仍无 SoD 守卫（grep SoD/createdBy 对照 = 0），坏账单创建人可自审 |
| P3-CK-fin2-010 | `ar-ap-auto-recon.batch.xml` L21 仍字面量 `'FIFO'` 传参（resolveStrategy config 读取被调用方短路）；设计文档 cron 键「deferred」漂移原样 |
| P3-CK-fin2-011 | `AutoReconciliationEngine.findPartnersWithOpenItems` 仍全量载入实体 + 线性 contains 去重；`DualSideConsistencyChecker` 逐发票 `eq("code",…)` N+1 反查原样 |
| P3-CK-fin2-012 | `ErpFinBadDebtProcessor.currentUserId` L432-439 仍宽 catch 返 null 无日志（本格原站点；fin-1 r3 已追加 RetryProcessor 跨切片站点） |
| P3-CK-fin2-013 | `AdvanceOffsetOrchestrator` L206-218 仍将本位币值直写 `setSettledAmountSource`/`setOpenAmountSource`（外币项源币口径污染原样） |
| P3-CK-fin2-014 | `ErpFinReconciliationReverseProcessor` 仍无原因记录通道（grep reason = 0），设计承诺「核销冲销：财务员 + 原因记录」未落地 |
| P3-CK-fin2-015 | `DualSideConsistencyChecker` 仍无坏账核销单侧变异识别（grep BadDebt/WRITTEN_OFF = 0），合法单侧变异仍报 INCONSISTENT 噪音 |
| P3-CK-fin2-016 | `erp-fin.bad-debt-exclude-disputed` 仍零消费（仅常量定义 `ErpFinConstants:352`，grep 消费点 = 0），计提排除争议项 doc 承诺不生效 |
| P3-CK-fin2-017 | `AbstractErpFinReconciliationProcessor.resolvePeriodId` L239-250 仍无 orgId 过滤/无排序 setLimit(1)；`BadDebtProvisionService.resolveAcctSchemaId` 魔法默认原样 |

### 2.3 新立 `-r3` — 1 条

**P2-CK-fin2-018-r3**（DIM-T 覆盖缺口）

- **控制点**：核销 FX 汇兑损益路径——`ReconciliationSettler.settleWithFx`（L60-82）/`reverseSettle(lines, fxPath=true)`（L112-129）/`AbstractErpFinReconciliationProcessor.generateReconFxVoucher`/`reverseReconFxVoucher`（L174-218）；config `erp-fin.recon-fx-gain-loss-enabled`（默认 false）。
- **问题**：该路径全链零测试断言——`module-finance/erp-fin-service` src/test + `_cases` 全量 grep `settleWithFx`/`fxPath`/`reverseSettle` = 0 命中；`TestErpFinReconciliation` 9 用例全部走非 FX 路径。testing-strategy P1「多币种过账+汇兑损益」行的核销侧子路径无覆盖，FX 对称回滚（P1-CK-fin2-002 修复体）回归将静默穿透。
- **三态裁决**：新立。r1 `ck-finance-arap.md` 交叉验证注记「FX 测试无 reverse 断言」无独立 finding ID（依附 002 上下文），不可归并；r2 无同型；002 本体 fixed 复用（代码修复在位）。按 `ck-inventory-r3.md` P2-CK-inv-012-r3 先例（已登记残留无 ID 可归并 → 新立 `-r3` 承接，对齐 code-history-deferred-triangulation §3.1 精神）。级别 **P2**：测试差距默认起点 + config 门控默认关现网无症状（门控性降档，同 inv-012-r3 逻辑）；多币种核销启用后升 P1。
- **修复建议**：归 M2.x 测试批——config 开启下 settleWithFx→reverse 断言双侧 open 复原 + fxGainLoss 凭证红冲（r1 报告 §最不确定 2 已给出复现程式）。

### 2.4 归属标注（§3.2 共享代码边界）

- 本报告全部控制点属 fin-2 格（核销/坏账/报销抵扣切片本体）。
- posting 引擎消费侧调用点（`voucherBiz.reverse`、`CloseVoucherWriter.writeVoucher`、`FinPostingExecutor.reverse`）仅审调用合规性，引擎内部归 fin-1（已闭合，本切片零涉引擎内部新发现）。
- `ErpFinVoucherBizModel` 两处 REQUIRES_NEW（R6=2）归 fin-1 格基线，本切片零命中。
- `P3-CK-fin2-012` 的 fin-1 跨切片站点（RetryProcessor 宽 catch）已由 fin-1 r3 承接，本格维持原站点证据。

## 3. 统计

| 级别 | 新立 | 复用 | 归并 |
| --- | --- | --- | --- |
| P0 | 0 | — | — |
| P1 | 0 | 4（fin2-001..004） | — |
| P2 | 1（fin2-018-r3） | 1（fin2-005） | 4（fin2-006..009） |
| P3 | 0 | — | 8（fin2-010..017） |

新立合计 1（P2×1）；复用合计 5；归并合计 12；历史 17 ID 零覆写。按维度（新立主维度计）：DIM-T×1。

## 4. 剩余风险（查了什么/没查什么）

- **已查**：核销链五守卫 + 聚合守卫 + FX 对称回滚 + 状态机 Bean 逐行（Post/Reverse/Create 三 Processor + Settler 双向 + 基类共享 helper）；坏账链 writeOff/recover/submit/approve/reject/reverseApprove 全时序（四现态守卫 + settled 对称 + 残额断言 + 审批门控 config）；计提/释放/反向红冲（ProvisionService + Calculator 头部）；报销抵扣借款链（ExpenseClaim/EmployeeAdvance BizModel + AdvanceOffsetOrchestrator 关键行）；双面对账/伙伴余额/自动核销引擎三辅助组件（机械程式 + open finding 现症复核）；15 维度程式全套 + 维度⑮ 4 断言；前端 16 页 + E2E spec 纪律；seed 核销链 6 行不变式 + 双射基线；533 测试全绿 + 覆盖对账 + 快照纪律；DIM-I 双 PASS + 白名单 4 抽查。
- **未深查**：`ErpFinNotesReceivable/PayableProcessor` 全文（票据族归 fin-1 边界注记同 r1）；`BadDebtProvisionCalculator` 分桶算术逐行（r1 已全读，本轮只验 open 现症）；`ErpFinReportBizModel`/`DashboardBizModel` 账龄消费面（只读报表 drift 归 U21/C8.2）；web 层 `_gen` 视图细节（codegen 面，M0.4 矩阵判定）；测试代码仅用于覆盖对账与行为交叉验证未逐文件审。
- **最不确定、建议复核**：① P2-CK-fin2-018-r3 的 P2 定级——若 M2.x 前多币种核销 config 被启用，应即升 P1 先写失败测试；② 归并 12 条的修复优先级建议参考 fin-1 r3 §4 同族排序（P2-CK-fin2-007 多账套族与 P2-CK-fin2-009 SoD 族影响面最大）。
- **残留风险（登记不裁决）**：① 12 条归并 open finding 修复归 M2.x（finance P1 修复批），其中 fin2-006（partner 余额陈旧）与 fin2-008（自动核销失败接力）建议优先；② `validate:flux` 325 条 variant 漂移与 compliance 机器基线块差距均为批前在案外部事项（successor：`ai-check-r3-compliance-baseline-raise` / nop-chaos-flux dist 基线裁决），非本切片范围；③ FX 核销路径在 config 长期默认 false 下存在「实现无测试守护」的隐性漂移窗口（= fin2-018-r3 承接面）。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项；`erp-fin.recon-fx-gain-loss-enabled` 启用投产时 P2-CK-fin2-018-r3 升 P1 立即修复。
