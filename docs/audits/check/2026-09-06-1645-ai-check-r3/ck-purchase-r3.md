# ck-purchase-r3 — purchase U03 五维符合性审计报告（ai-check-r3 M1.13）

> 工作项：M1.13（U03 × 五维 + U04 × 五维 + U02 × 五维，冻结清单 §4 映射表第 13 行；本报告 = U03 purchase 格，U04/U02 格分别见 `ck-sales-r3.md` / `ck-inventory-r3.md`）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `e0eda4d231f0f95e48eb1f2623edb26cd25134d5`（2026-09-08；计划基线 `f40b4bbae` 后唯一推进 = M1.10 hr + M1.12 projects/quality 审计产物落盘两笔 docs-only 提交，生产代码零变化）；脏面 = 1 条 untracked 计划文件（本计划 `2026-09-08-1454-3`），tracked 零修改。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U03 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：purchase 全域（C 级，无切片细分）——Requisition/Rfq/Quotation/Order/Receive/Invoice/Payment/Return/SupplierPriceList/SupplierScorecard 族 + 三单匹配（ThreeWayMatcher）+ processor 13 族 + posting dispatcher 族 + dashboard/support/spi 子包（`module-purchase/erp-pur-{dao,service,web}` src/main）；owner docs `docs/design/purchase/`（state-machine/three-way-match/requisition/returns/supplier-evaluation/use-cases/README）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting processor 族本体唯一归属 fin-1（M1.1 已收官，本切片仅审消费侧调用点）；common 抽象族（`AbstractErpCrudBizModel` 状态锁基类）行为归 U20（M1.15），本切片只审调用点（20/20 接入合规）；聚合横切面归 U21；notify 消费点归 U11。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U03 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点②④⑧⑮）+ 维度⑮断言抽样 2 doc × 4 断言 | 反模式族全零（六路径 `extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0）；`@Transactional`×`@BizMutation` 同文件命中 6 Processor 逐文件核验均属 javadoc「本类不带 @Transactional」说明引注（真实共存=0，R6=2 基线站为 finance ErpFinVoucherBizModel 已裁决偏离）；checker 19 规则 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a/b/c=71/66/42）；`__XGEN_FORCE_OVERRIDE__` 命中全为 erp-pur-meta dict.yaml codegen 校验点零手改、`_gen`/`_` 脏面空；聚合器（E1 路径）含 `/erp/pur/auth/erp-pur.action-auth.xml`；IDaoProvider/IOrmTemplate 17 文件命中处豁免理由注释齐备（SPI 解析器/同模块直查/dashboard 聚合范式）。15/15 无跳维：①实体服务标准链无越序 Java；②20/20 BizModel extends 状态锁基类（调用点合规，本体归 U20）+ materialBiz.get 权限管道消费；③ErpPurErrors 集中 `erp.err.pur.*`；④**SoD 前移修复复核有效**（ErpPurInvoiceApproveProcessor:24-27 SoD-first 于 :34 doPosting 之前 + 承付 hook 前移 :31-33，P1-CK-pur-002 复用）；⑤⑥⑦⑨⑩⑪⑬⑭ pass；⑧**三单匹配**数量强制项/strict-mode 门控与 owner doc 一致 + **容差量纲缺陷仍在**（Dashboard `loadActiveInvoicesInRange` 族读 `erp-pur.match-price-tolerance` 默认 `0.05` ratio vs ThreeWayMatcher `priceTolerancePercent():173-175` 同键默认 `"5"` percent，P2-CK-pur-007 现症复核归并）；⑮2 doc × 4 断言 3 一致 1 漂移（state-machine.md §异常路径「付款核销时发票已作废→拒绝核销」 vs PaymentSettler 全文零 docStatus 引用 → **新立 P2-CK-pur-015-r3**；javadoc 残留旧「docStatus=ACTIVE」口径 → **新立 P3-CK-pur-016-r3**） | **finding**（2 新立 DIM-B：1 P2 + 1 P3；3 复用 + 11 归并，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 三单匹配页（复杂手写页清单成员）+ 采购链 codegen+delta 面 | `npm run validate:flux`：step [1/3] 导出正常；报告 `files=855 validated=855 errors=325 warnings=18491`——**机械复核 325 ERR 100% variant=primary 族（non-variant=0）**= 既有 stub 外部漂移 successor 在案（pur 域命中 12 条同族成员不立项）；`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 无缺失；三单匹配页 `dashboard/three-way-match.page.yaml`（M0.4 row 98 登记 flux 原生手写页）：数据访问全 `@query:ErpPurDashboard__findThreeWayMatchDiffAlert`/`ErpPur{Order,Receive,Invoice}__findPage` REST 约定 + `i18nEn` 全字段承载 ✅，页面 remark「默认 5%」与后端 Dashboard 实际默认 0.05 ratio 不一致 = P2-CK-pur-007 UI 可见面（归并证据增强不另立）；**发现 minor**：L129/170/207 三处 `docStatus == "ACTIVE"` 样式分支消费死状态（display-only，live 单据恒 DRAFT/CANCELLED，primary 分支不可达）→ **新立 P3-CK-pur-017-r3**；采购链 12 实体 main/picker 页 codegen wrapper（`x:gen-extends`→view.xml）+ 保留层 view.xml `x:extends` 定制（`@mutation:ErpPurOrder__batchApprove` 等标准动作 API）；`graphql:labelProp` 命中均为 view.xml to-one label 元数据键读取（codegen 约定，非 GraphQL 传输/断言）；E2E pur-return spec：非法 selector 0 / 页面级 GraphQL 断言 0 / `E2E_ENGINE` 缺省 flux | **finding**（minor：1 新立 P3，见 §2.3） |
| **DIM-S seed 数据** | §1.3 全套 + P2P 链（PO→Receive→Invoice→Payment）posted 一致性 + 增量面复核 | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**（BUILD SUCCESS 16.46s）；`git status --porcelain _init-data/` 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL** = 冻结登记值；三域 deploy `_seed_*.sql` 命中 = 0（零同步义务）；P2P 链抽查：PO-2026-001（960.50 含税，PAID/RECEIVED）→ PRCV-2026-001（orderId=1，850，RECEIVED）→ PINV-2026-001（totalAmountWithTax=960.50，APPROVED/PAID，posted=true）→ PAY-2026-001（960.50，WRITTEN_OFF=PAID，posted=true）——金额链 850→960.50（+110.50 税）→960.50 全程衔接；posted=true ⟺ 财务产物（M1.1 双射裁决面复核一致，不重开）；seed 行 docStatus=ACTIVE 表示法 = 既有裁决静态面（Dashboard 查询已按 APPROVED+≠CANCELLED 口径兼容） | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P0 采购到付款清单行覆盖核对 | `mvn test -pl module-purchase/erp-pur-service` **341/0/0/0 全绿**（= 基线 pur 341 精确一致，零增量）；覆盖对账：BizModel 注解动作 30（另有大面积动作经 xbiz 声明 + per-mutation Processor 承载由 Processor 测试类覆盖），`_cases/app/erp/pur/service/` 测试资产 45 项；**P0 行在位**：`TestErpPurProcureToPayEnd`（PO→Receive→Invoice→Payment 全链 + testReverseScenarios 红冲三场景）+ `TestErpPurOrderToReceiveEnd`；核心族齐备（Invoice/Payment/Order Approval、Posting、Settlement、PriceVariancePosting 9 组、MultiCurrencyPosting、CrudStatusLock 五分支、BudgetControl、CommitmentRestore、FinanceReversalWriteback）；末笔生产提交 `bdb469d22`（状态机直抛领域码）附同码测试，零新增未测试公开动作；`SnapshotTest.RECORDING` = 0；`_cases` 抽查 `delVersion` 屏蔽在位、审计字段框架自动屏蔽 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT，170 files 单向收紧改善项）；`--self-test` **PASS**；pur 探针族（CAT-1 22/CAT-2 32）维持清零零回归（report mode CAT1..4=0/0/0/0）；WHITELIST pur 条目 1 条（`ErpPurDashboardBizModel.java` cats[3]）四要素齐备（路径/理由 @Description 专属 1 行 E3 豁免/owner doc 判定准绳表 #5/裁决来源 plan 2026-09-07-1715-1 Phase 3）；`grep -L @Locale` *Errors.java = 空；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ai-check-index.md` §报告清单/§Finding 追踪（同域报告 `ck-purchase.md` C2.1 全 14 条逐一比对）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/`（无 pur 同型新独立登记）+ §Mission 基线快照（checker 各命中均为已裁决偏离）。**本轮新立 3 条**（`P2-CK-pur-015-r3` + `P3-CK-pur-016-r3` + `P3-CK-pur-017-r3`）；历史 14 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 3 条

| 原 ID | 修复在位证据（T0 = HEAD `e0eda4d23`） |
| --- | --- |
| P1-CK-pur-001（Dashboard 全主查询消费 docStatus=ACTIVE 死状态） | `ErpPurDashboardBizModel` 三处查询（loadActiveInvoicesInRange:252 / countActiveOrders:261 / :278）均 `and(eq(approveStatus,APPROVED), ne(docStatus,CANCELLED))`，F2.10 修复 HEAD 复核有效；残留 javadoc 措辞漂移另行新立 P3-CK-pur-016-r3（注释级，不影响查询正确性） |
| P1-CK-pur-002（Invoice/Payment SoD 守卫位于 doPosting 之后） | `ErpPurInvoiceApproveProcessor#approve:26-27` SoDGuard.assertApproverNotCreator 前置于 :34 `processor.doPosting`（REQUIRES_NEW）之前 + :31-33 承付释放 hook 前移（F1.2 注记在位）；Payment 同型；SoD-first 复核有效 |
| P1-CK-pur-003（通用 CRUD update 无「已审核/已过账不可修改」守卫） | F1.3 统一基类接入在位：20/20 pur BizModel extends `AbstractErpCrudBizModel`（posted/APPROVED 拒通用 update/delete；调用点合规，基类行为缺陷归 U20/M1.15） |

### 2.2 归并（同型 open 追加证据至原 ID）— 11 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-pur-004 | CANCELLED 仍计聚合 + cancel/reverseApprove 后进度不重算原样（rollup 面未变） |
| P2-CK-pur-005 | Payment cancel/reverseApprove 不守卫不回滚核销原样（cancel 侧处理器零 PaymentSettler#reverseSettlement 编排） |
| P2-CK-pur-006 | 三处聚合校验无共享行锁竞态原样（超收容差/退货可退量/请购转订单幂等） |
| P2-CK-pur-007 | **现症复核**：Dashboard `AppConfig.var(CONFIG_MATCH_PRICE_TOLERANCE, "0.05")`（ratio）vs ThreeWayMatcher `priceTolerancePercent():173-175` 同键默认 `"5"`（percent）——同键双量纲未收敛；three-way-match.md §配置表 L100 `5 (%)` owner-doc 口径佐证 percent 侧正确；本切片追加 UI 面加重证据（three-way-match 页 remark「默认 5%」与 Dashboard 实际检测阈值 0.05 在未配置时相差 100 倍） |
| P2-CK-pur-008 | 退货行 receiveLineId 可空绕过数量上限原样 |
| P2-CK-pur-009 | 请购转订单非法税率串静默按零税原样 |
| P3-CK-pur-010 | currentUserId 宽 catch 返回 null 无日志（5 处 Processor）原样 |
| P3-CK-pur-011 | Dashboard 无界加载 + N+1 原样（loadActiveInvoicesInRange 全量 + 逐发票 partnerDao.getEntityById） |
| P3-CK-pur-012 | 声明未接线配置/常量原样 |
| P3-CK-pur-013 | batchApprove 逐行吞 NopException 共享单事务原样 |
| P3-CK-pur-014 | ThreeWayMatcher 悬挂回链静默跳过原样（receiveLineId 缺失行跳过匹配语义未变） |

### 2.3 新立 `-r3` — 3 条（1 P2 + 2 P3）

**P2-CK-pur-015-r3**（DIM-B 维度⑧/⑮）

- **控制点**：`PaymentSettler#settle`（L65-130）+ `#reverseSettlement`——settle 仅守卫 `payment.approveStatus=APPROVED`（L66-71）+ `requireInvoiceForSettle` 存在/同客户/APPROVED；**全文零 `docStatus`/`CANCELLED` 引用**。
- **owner-doc 断言漂移**：`docs/design/purchase/state-machine.md` §异常路径 L99「付款核销时发票已作废 → 拒绝核销，提示发票状态异常」逐字未实现——已作废（docStatus=CANCELLED，approveStatus 保持 APPROVED）的发票可被继续核销、已作废付款单可继续核销发票，AR/AP 派生态与作废语义冲突。
- **三态裁决**：新立。与 P2-CK-pur-005（cancel/reverseApprove 侧不守卫已核销态，open）互为镜像**不同控制点**（本条 = settle/reverseSettlement 入口缺 docStatus 维度）；对齐 r1 分立先例（sal-010 settle 侧 / sal-012 反核销侧两条分立）。级别 **P2**（同族 sal-010/pur-005 均 P2；owner doc 显式异常路径承诺未实现）。
- **建议修复方向**：settle/reverseSettlement 双侧补 `docStatus≠CANCELLED` 守卫（payment 与 invoice 两侧；对齐 sal-010 修复方向）；修复阶段先写「作废发票核销拒绝」失败测试。

**P3-CK-pur-016-r3**（DIM-B 维度⑮ code 内文档漂移）

- **控制点**：`ErpPurDashboardBizModel` javadoc L54-55「本期采购额取自 ErpPurInvoice（docStatus=ACTIVE Σ amountFunctional）；本期订单量取自 ErpPurOrder（docStatus=ACTIVE count）」+ L176「检测 ACTIVE 发票行」——类 javadoc 仍描述 pur-001 修复前的旧口径。
- **问题/严重性**：查询实现已改 `approveStatus=APPROVED + docStatus≠CANCELLED`（F2.10），注释未随——措辞级文档漂移误导后续维护（P1-CK-pur-001 同站点残留，注释不影响运行正确性）；P3。
- **建议修复方向**：类/方法 javadoc 口径措辞同步为现查询语义（零行为变更）。

**P3-CK-pur-017-r3**（DIM-F）

- **控制点**：`erp-pur-web/.../dashboard/three-way-match.page.yaml` L129/170/207——三表列 tpl `docStatus == "ACTIVE" ? "primary" : "default"` 消费死状态 ACTIVE 做样式分支。
- **问题/严重性**：生产零 writer（state-machine.md §docStatus 轴注记 L23 intentional legacy dead state），live 单据恒 DRAFT/CANCELLED → primary 分支不可达（display-only 死代码 + 样式语义误导）；P3（与 r1 pur-001 同族死状态消费的页面侧残留站点，控制点不同——纯展示无数据影响）。
- **建议修复方向**：tpl 改按 `approveStatus`（或 `docStatus != "CANCELLED"`）分支；与 P2-CK-pur-007 归并修复时一并清理。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting processor 族本体（凭证引擎/dispatcher 内部/REQUIRES_NEW 语义/sweep）**归属 fin-1**（M1.1 已收官）；本切片仅核对 PurInvoicePostingDispatcher/PurPaymentPostingDispatcher/PurReturnPostingDispatcher/PurReversalListener 消费侧调用序（SoD-first 后 tryPost、回链回写消费正常）。
- `AbstractErpCrudBizModel` 状态锁基类调用点已审合规（20/20）；基类行为缺陷归 U20（M1.15）。
- 聚合横切面（action-auth 聚合器/seed 全量装载/flux 导出门禁本体）归 U21（M1.16）；本格仅核 pur 注册在位性。
- notify 派发消费点（PaymentWorkflowApprovalNotifications 族）调用面正常，子系统本体归 U11。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 3（pur-001..003） | 0 |
| P2 | 1（pur-015-r3，DIM-B ⑧⑮） | 0 | 6（pur-004..009） |
| P3 | 2（pur-016-r3 DIM-B ⑮ / pur-017-r3 DIM-F） | 0 | 5（pur-010..014） |
| **合计** | **3** | **3** | **11** |

五格 verdict：DIM-B **finding**（2 新立：1 P2 + 1 P3）/ DIM-F **finding**（minor：1 新立 P3）/ DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 14 条 r1 ID 状态零覆写（3 fixed 复核有效 + 11 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-pur-service 全部 processor（13 族）+ entity BizModel 20 + ThreeWayMatcher/ScorecardCalculator/ReturnQtyValidator/RequisitionToOrderConverter/PaymentSettler + posting dispatcher 族 + dashboard/support/spi 子包 + ErpPurErrors/Constants + 20 BizModel 基类接入 + 机械程式全套实跑（checker 19 规则/反模式六路径/codegen 校验点/聚合 E1/validate:flux 855 页/seed 门禁 4 用例/pur 回归 341/strict+self-test）；owner docs 2 doc × 4 断言抽样（state-machine.md/three-way-match.md）；r1 14 条逐一比对裁决；r2 目录核对；P2P 链 seed 逐行抽查。
- **未深查（边界归属）**：posting 引擎本体（归 fin-1）；`AbstractErpCrudBizModel` 基类内部（归 U20/M1.15）；`erp-pur-web` 全量渲染时行为（静态走查 + validate:flux 门禁，浏览器回归归看板运行时专项）；requisition/rfq/supplier-evaluation 域外延 owner doc 全量断言（抽样 ≥2 doc 达标，未发现漂移未触发扩样）。
- **残留风险（登记不裁决）**：① 11 条归并 open finding 修复归 M2.x，其中 P2-CK-pur-007（容差量纲）与本轮 P2-CK-pur-015-r3（核销 docStatus 守卫）建议 M2.x 优先（同属金额正确性族）；② P3-CK-pur-016/017-r3 与 pur-001 同站点残留可在 M2.x 修复 pur-001 族时一并收口；③ seed 行 docStatus=ACTIVE 表示法为既有静态裁决面，若未来复活 ACTIVE writer 需重审 Dashboard 兼容口径；④ `validate:flux` 325 条 variant 漂移为批前在案外部事项（successor：nop-chaos-flux dist 基线裁决）。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项；容差量纲统一时须同步收敛 Dashboard/ThreeWayMatcher/页面文案三面（015/007/页面 remark 联动）。
