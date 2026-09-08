# ck-sales-r3 — sales U04 五维符合性审计报告（ai-check-r3 M1.13）

> 工作项：M1.13（U03 × 五维 + U04 × 五维 + U02 × 五维，冻结清单 §4 映射表第 13 行；本报告 = U04 sales 格，U03/U02 格分别见 `ck-purchase-r3.md` / `ck-inventory-r3.md`）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `e0eda4d231f0f95e48eb1f2623edb26cd25134d5`（2026-09-08；计划基线 `f40b4bbae` 后唯一推进 = 两笔 docs-only 审计产物提交，生产代码零变化）；脏面 = 1 条 untracked 计划文件（本计划 `2026-09-08-1454-3`），tracked 零修改。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2 + §3.3 U04 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：sales 全域（C 级，无切片细分）——Quotation/Contract/Order/Delivery/Invoice/Receipt/Return/PriceList/PricingRule 族 + 取价链（ErpSalCustomerPriceResolver）+ 促销规则引擎（ErpSalPricingRuleEngine）+ 核销（ReceiptSettler）+ 退款编排（ReturnRefundOrchestrator）+ processor 12 族 + posting dispatcher 族 + dashboard/spi 子包（`module-sales/erp-sal-{dao,service,web}` src/main）；owner docs `docs/design/sales/`（state-machine/quotation/contract/returns/use-cases/README）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting processor 族本体唯一归属 fin-1——sales 发票红冲前置为**消费侧**，消费侧行为归本切片，涉引擎内部缺陷标注「归属 fin-1」归并；common 抽象族行为归 U20（本切片只审调用点，16+ 接入合规）；聚合横切面归 U21；notify 消费点归 U11。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U04 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点②④⑧⑮）+ 维度⑮断言抽样 2 doc × 4 断言 | 反模式族全零（六路径同 pur 口径：RuntimeException=0/@Inject private=0/System.currentTimeMillis=0；`@Transactional`×`@BizMutation` 6 Processor 命中均 javadoc 说明引注，真实共存=0）；checker 19 规则 = M0.3 快照行零漂移；`__XGEN_FORCE_OVERRIDE__` 全为 erp-sal-meta dict.yaml 校验点零手改；聚合器含 `/erp/sal/auth/erp-sal.action-auth.xml`；IDaoProvider/IOrmTemplate 15 文件命中处豁免理由注释齐备（ReceiptSettler/ReturnRefundOrchestrator/pricing resolver/dashboard 范式）。15/15 无跳维：①标准链；②**红冲前置消费侧修复复核有效**（ErpSalInvoiceReverseApproveProcessor:40-42 + CancelProcessor:42 `posted \|\| hasActivePosting(code)` 双判，P1-CK-sal-003 sales 侧收口在位；引擎内部归 fin-1 不展开）；③ErpSalErrors 集中；④SoD/REQUIRES_NEW = R10 基线内已裁决偏离；⑤⑥⑦⑨⑩⑪⑬⑭ pass；⑧**促销规则引擎** `lineMatchesRuleTarget:166-171` 仍仅 materialId 匹配（P2-CK-sal-005 现症归并）+ **核销状态守卫** ReceiptSettler 全文零 docStatus 引用（P2-CK-sal-010 现症归并）+ delivery rollup 仅 approveStatus 过滤（P2-CK-sal-011 现症归并）+ receipt reverseApprove/cancel 零 reverseSettlement 编排（P2-CK-sal-012 现症归并）；⑮2 doc × 4 断言：state-machine.md L38「posted=true 后物理锁定」 vs 状态锁基类 16+ BizModel 接入 → 一致（sal-004 复用）；state-machine.md L80「收款核销时发票已作废→拒绝核销」 vs ReceiptSettler 零 docStatus → 漂移（= sal-010 归并）；returns.md L427-428 RC-R1.19 已核销发票守卫链 vs ReturnRefundOrchestrator L77-99 同链路反转 → 一致（sal-001 复用）；returns.md L33 价差语义（EXDIFF- 前缀/Δ<0 兜底/幂等守卫）vs 实现注记 → 一致 | **finding**（归并态：0 新立；4 复用 + 14 归并，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 报价/订单/出库/发票销售链页面 | `npm run validate:flux`：855/855 validated，325 ERR 100% variant=primary 既有外部漂移（sal 域命中 12 条同族成员不立项，`sales-price-list` main.page.json 即族内成员）；`component="AMIS"` 0、ORM `ext:web-renderer="flux"` 无缺失；销售链 Quotation/Order/Delivery/Invoice/Receipt/Return/Contract/PriceList/PricingRule 全 main+picker 页 = codegen wrapper（`x:gen-extends`→view.xml）+ 保留层 view.xml 定制（REST `@query/@mutation` 约定）；`sales-price-list`/`pricing-rule` 手写 wrapper 页零自有 CJK 文案（CAT-4=0 全局面背书）；`graphql:labelProp` 命中均为元数据键读取；E2E sal-return/sal-date-range-validation spec：非法 selector 0 / 页面级 GraphQL 断言 0（GraphQL 驱动 `__save` 写路径 = runbook §业务动作套件 sanctioned 范式）/ `E2E_ENGINE` 缺省 flux | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + O2C 链（SO→Delivery→Invoice→Receipt）posted 一致性 | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；`_init-data` porcelain 空（零 seed 变更）；资产清点 372 CSV + 1 SQL = 冻结登记值；sal deploy `_seed_*.sql` 命中 = 0；O2C 链抽查：SO-2026-001（1130.00 含税，RECEIVED/DELIVERED）→ SDLV-2026-001（orderId=1，1130.00）→ SINV-2026-001（1130.00，RECEIVED，posted=true）→ REC-2026-001（1130.00，WRITTEN_OFF=RECEIVED，posted=true）——金额/状态衔接一致，posted=true ⟺ 财务产物（M1.1 双射裁决面复核一致，不重开） | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P0 销售到收款清单行覆盖核对 | `mvn test -pl module-sales/erp-sal-service` **316/0/0/0 全绿**（= 基线 sal 316 精确一致）；覆盖对账：BizModel 注解动作 18（xbiz/Processor 动作由 Processor 测试类覆盖），`_cases` 测试资产 42 项；**P0 行在位**：`TestErpSalOrderToCashEnd`（SO→Delivery→Invoice→Receipt 全链）+ `TestErpSalOrderToDeliveryEnd` + `TestErpSalPricingEndToEnd` + `TestErpSalReturnRefundEndToEnd`；核心族齐备（Dashboard KPI 口径、Return Exchange/Refund、Contract、Quotation）；`SnapshotTest.RECORDING` = 0 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations，单向收紧）；`--self-test` **PASS**；sal 探针族（CAT-1 22/CAT-2 30）维持清零零回归；WHITELIST sal 条目 1 条（`ErpSalDashboardBizModel.java` cats[3]）四要素齐备（@Description 专属 1 行 E3 豁免/判定准绳表 #5/plan 2026-09-07-1715-1 Phase 3）；`grep -L @Locale` = 空；meta/i18n 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ai-check-index.md` §Finding 追踪（同域报告 `ck-sales.md` C2.2 全 18 条逐一比对）+ r2 只读目录（无 sal 同型新独立登记）+ §Mission 基线快照。**本轮新立 0 条**；历史 18 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 4 条

| 原 ID | 修复在位证据（T0 = HEAD `e0eda4d23`） |
| --- | --- |
| P1-CK-sal-001（ReturnRefundOrchestrator 客户级全量反转核销） | 关联发票限定链路在位（:77-99 退货行 deliveryLineId → ErpSalInvoiceLine.deliveryLineId → invoiceId 集合 → findReceivedInvoicesOfCustomer `in("id", invoiceIds)`）；无关联发票跳过；类 javadoc F2.10 注记在位；HEAD 复核有效 |
| P1-CK-sal-002（Dashboard 订单量 KPI 死状态 ACTIVE 恒 0） | `countActiveOrders:226-229` `and(eq(approveStatus,APPROVED), ne(docStatus,CANCELLED))` + javadoc 同步（:51）；HEAD 复核有效 |
| P1-CK-sal-003（延迟过账重试成功后 posted 标志不回写） | finance F2.1 posted 事件回写（fin-1 收官面）+ sales 侧收口：ReverseApprove:40-42 / Cancel:42 / ErpSalInvoiceProcessor:404-405 `posted \|\| hasActivePosting(code)`（ErpFinVoucherBillR 反查未红冲 AR_INVOICE 凭证）；消费侧复核有效，引擎内部维持归属 C3.1/fin-1 |
| P1-CK-sal-004（通用 CRUD 无状态守卫） | F1.3 统一基类接入在位（16+ BizModel extends `AbstractErpCrudBizModel`，含头/行全族）；调用点合规 |

### 2.2 归并（同型 open 追加证据至原 ID）— 14 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-sal-005 | **现症复核**：`lineMatchesRuleTarget:166-171` 仍仅 `rule.getMaterialId()` 匹配，materialCategoryId 目标维度未参与 |
| P2-CK-sal-006 | GIFT 规则触发物料不在订单仍生成赠品行原样（referenceLine null 容忍路径未变） |
| P2-CK-sal-007 | 非栈式规则 break 全链 vs javadoc「同类型排他」语义相悖原样 |
| P2-CK-sal-008 | 价格清单「最优命中」无确定性排序 + 全量内存过滤原样（matchLine 迭代序首条） |
| P2-CK-sal-009 | 可用量预校验/退货 current 成本 setLimit(1) 单行取值原样 |
| P2-CK-sal-010 | **现症复核**：ReceiptSettler 全文零 docStatus/CANCELLED 引用（settle:55-61 仅 approveStatus + requireInvoiceForSettle 存在/同客户/APPROVED）——owner doc state-machine.md §异常路径 L80「收款核销时发票已作废→拒绝核销」仍漂移；本切片追加维度⑮抽中证据 |
| P2-CK-sal-011 | **现症复核**：`findApprovedDeliveries:381-385` 仅 `eq("approveStatus",APPROVED)` 无 docStatus≠CANCELLED 过滤；cancel 无重 rollup |
| P2-CK-sal-012 | **现症复核**：ErpSalReceiptReverseApproveProcessor/CancelProcessor 零 `ReceiptSettler.reverseSettlement` 编排（grep 零命中） |
| P2-CK-sal-013 | withdrawApproval 无「仅提交人可操作」校验原样（六实体继承无覆盖） |
| P2-CK-sal-014 | Contract INLINE approve 缺 SoD 守卫原样 |
| P2-CK-sal-015 | 报价过期日扫 job 未实现原样（`erp-sal.quotation-expiry-check-cron` 零落地） |
| P2-CK-sal-016 | 一次报价多次转订单阻断原样（疑似需求分歧只登记，维持不裁决） |
| P2-CK-sal-017 | 换货出库单行税额公式与价税分离口径不一致原样 |
| P2-CK-sal-018 | OFFSET_ESTIMATED_RECEIVABLE 硬编码 TRUE 原样（跨域核对归属 C3.1 维持） |

### 2.3 新立 `-r3` — 0 条

本单元走查未发现 r1 18 条之外的新发缺陷：r1 四条 P1 修复全部 HEAD 复核有效；14 条 open 现症逐一复核在位（其中 005/010/011/012 四条高频焦点做站点级代码复核）；机械程式零新增违规。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting processor 族本体**归属 fin-1**：本切片核对 SalInvoicePostingDispatcher/SalReceiptPostingDispatcher/SalReturnPostingDispatcher/SalReversalListener 消费侧（红冲前置双判、P1-RC-024 条件门控、posted 回写消费）；延迟过账 sweep 重试通道内部归 fin-1/C3.1（P2-CK-sal-018 跨域归属维持）。
- common 抽象族调用点合规；基类行为归 U20（M1.15）。
- 聚合横切面归 U21；本格仅核 sal 注册在位性。
- `ReturnCostStrategyResolver` 静态工具形态经调用方传入既有 IDaoProvider（builder/dispatcher 双调用点豁免注释在位）——R2 基线已裁决面。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 4（sal-001..004） | 0 |
| P2 | 0 | 0 | 14（sal-005..018） |
| P3 | 0 | 0 | 0 |
| **合计** | **0** | **4** | **14** |

五格 verdict：DIM-B **finding**（归并态：0 新立，14 open 现症复核在位）/ DIM-F **pass** / DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 18 条 r1 ID 状态零覆写（4 fixed 复核有效 + 14 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-sal-service 全部 processor（12 族）+ entity BizModel 16+（含 ReceiptSettler/ReturnRefundOrchestrator/ReturnCostStrategyResolver/ReturnStockMoveBuilder/CreditLimitChecker/QuotationToOrderConverter/ReturnQtyValidator）+ ErpSalPricingRuleEngine/ErpSalCustomerPriceResolver 取价链 + posting dispatcher 族 + dashboard/spi 子包 + ErpSalErrors/Constants + 机械程式全套实跑（checker/反模式/codegen/聚合 E1/validate:flux/seed 门禁/sal 回归 316/strict+self-test）；owner docs 2 doc × 4 断言抽样（state-machine.md/returns.md）；r1 18 条逐一比对裁决；r2 目录核对；O2C 链 seed 逐行抽查。
- **未深查（边界归属）**：posting 引擎本体与 sweep 重试通道（归 fin-1/C3.1）；`AbstractErpCrudBizModel` 基类内部（归 U20）；`erp-sal-web` 渲染时行为（静态走查 + 门禁，浏览器回归归看板运行时专项）；contract 域外延 owner doc（quotation.md 断言面经 sal-015/016 归并覆盖，未触发扩样条件——漂移计数 = 1（sal-010 同型）< 2）。
- **残留风险（登记不裁决）**：① 14 条归并 open finding 修复归 M2.x，其中 P2-CK-sal-010/011/012（作废/反核销状态族）与 P2-CK-sal-005/006/007/008（促销/取价金额正确性族）建议 M2.x 优先——前者与 pur-015-r3 同族可统一设计「核销/聚合 docStatus 守卫」修复批次；② r1 P1-CK-sal-003 的 finance 侧回写通道（F2.1）跨域一致性由 fin-1 收官面持续背书，sales 侧已双判兜底；③ `validate:flux` 325 条 variant 漂移为批前在案外部事项（successor：nop-chaos-flux dist 基线裁决）。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2 open 项；「核销/聚合 docStatus 守卫」族修复时建议 pur-015-r3/sal-010/sal-011/sal-012/pur-005 统一批次设计。
