# purchase 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（测试共享夹具见 `app-erp-test-data`，边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.1c（plan `docs/plans/2026-09-01-0838-3-m11c-purchase-seed-expansion.md`，2026-09-01）。此前 8 张 purchase 表 seed（order/receive/invoice/payment 各头+行，P2P 最小连通链）见 `docs/architecture/seed-data.md` 历史批次段。

## 种子数据范围（M1.1c 批次 12 表）

purchase 域 20 实体中 8 表已由历史批次 seed；本批补齐其余 12 表（**4 组主子表 + 1 组评分主子表链 + 2 独立表**），达成**域内全量 seed 覆盖（20/20）**。

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpPurRequisition（请购单头） | `erp_pur_requisition.csv` | 3 | P+N-TERM | REQUESTER_ID→ErpMdEmployee〔跨域:md·已seed〕 |
| ErpPurRequisitionLine（请购单行） | `erp_pur_requisition_line.csv` | 3（1/头） | P | REQUISITION_ID→ErpPurRequisition〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔跨域:md·已seed〕 |
| ErpPurRfq（询价单头） | `erp_pur_rfq.csv` | 3 | P+N-TERM | —（无必填 FK；REQUISITION_ID 可选指向〔本批〕请购头） |
| ErpPurRfqLine（询价单行） | `erp_pur_rfq_line.csv` | 4（1~2/头） | P | RFQ_ID→ErpPurRfq〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔跨域:md·已seed〕 |
| ErpPurQuotation（供应商报价单头） | `erp_pur_quotation.csv` | 3 | P+N-TERM | SUPPLIER_ID→ErpMdPartner〔跨域:md·已seed〕、CURRENCY_ID→ErpMdCurrency〔已seed〕 |
| ErpPurQuotationLine（报价单行） | `erp_pur_quotation_line.csv` | 3（1/头） | P | QUOTATION_ID→ErpPurQuotation〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔跨域:md·已seed〕 |
| ErpPurSupplierPriceList（供应商价格清单，独立表） | `erp_pur_supplier_price_list.csv` | 2 | P+N-DIS | SUPPLIER_ID→ErpMdPartner〔已seed〕、MATERIAL_ID/UO_M_ID→ErpMd*〔已seed〕、CURRENCY_ID→ErpMdCurrency〔已seed〕 |
| ErpPurSupplierScorecard（供应商评分卡头） | `erp_pur_supplier_scorecard.csv` | 3 | P+N-TERM | PARTNER_ID→ErpMdPartner〔跨域:md·已seed〕 |
| ErpPurSupplierScorecardCriteria（评分维度行） | `erp_pur_supplier_scorecard_criteria.csv` | 9（3/头） | P | SCORECARD_ID→ErpPurSupplierScorecard〔本批〕 |
| ErpPurSupplierScorecardVariable（评分变量，独立表） | `erp_pur_supplier_scorecard_variable.csv` | 3 | P | CRITERIA_ID→ErpPurSupplierScorecardCriteria〔本批〕 |
| ErpPurReturn（采购退货单头） | `erp_pur_return.csv` | 2 | P+N-TERM | SUPPLIER_ID→ErpMdPartner〔已seed〕、WAREHOUSE_ID→ErpMdWarehouse〔已seed〕、CURRENCY_ID→ErpMdCurrency〔已seed〕 |
| ErpPurReturnLine（退货单行） | `erp_pur_return_line.csv` | 2（1/头） | P | RETURN_ID→ErpPurReturn〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔已seed〕；RECEIVE_LINE_ID 可选指向既有 seed `erp_pur_receive_line` id=1 |

## FK 闭环图

```
[跨域:md·已seed] erp_md_partner(3 北方钢铁 / 4 东方化工) ──SUPPLIER_ID/PARTNER_ID──▶ 报价单头 / 价格清单 / 评分卡头 / 退货单头
[跨域:md·已seed] erp_md_employee(1 张三·采购经理)        ──REQUESTER_ID──▶ 请购单头
[跨域:md·已seed] erp_md_material(1..4)                  ──MATERIAL_ID──▶ 请购/询价/报价/退货行表 + 价格清单
[跨域:md·已seed] erp_md_uom(1..4)                       ──UO_M_ID─────▶ 请购/询价/报价/退货行表 + 价格清单
[跨域:md·已seed] erp_md_currency(1 CNY)                 ──CURRENCY_ID──▶ 报价单头 / 价格清单 / 退货单头
[跨域:md·已seed] erp_md_warehouse(2 WH-RAW)             ──WAREHOUSE_ID──▶ 退货单头
[跨域:md·已seed] erp_md_organization(2 ERP-CO)          ──ORG_ID/DEPARTMENT_ID──▶ 各单据头
[已seed·pur] erp_pur_receive(1 PRCV-2026-001)           ──RECEIVE_ID──▶ 退货单头（可选）
[已seed·pur] erp_pur_receive_line(1)                    ──RECEIVE_LINE_ID──▶ 退货行（可选）
[本批] erp_pur_requisition(1..3)                        ──REQUISITION_ID──▶ erp_pur_requisition_line；RFQ 头可选回链
[本批] erp_pur_rfq(1..3)                                ──RFQ_ID─────────▶ erp_pur_rfq_line；报价头可选回链
[本批] erp_pur_quotation(1..3)                          ──QUOTATION_ID───▶ erp_pur_quotation_line
[本批] erp_pur_return(1..2)                             ──RETURN_ID──────▶ erp_pur_return_line
[本批] erp_pur_supplier_scorecard(1..3)                 ──SCORECARD_ID───▶ erp_pur_supplier_scorecard_criteria
[本批] erp_pur_supplier_scorecard_criteria(1..9)        ──CRITERIA_ID────▶ erp_pur_supplier_scorecard_variable
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空（`TestErpSeedDataIntegrity` 引用完整性门禁背书）。

## 与既有 8 表 seed 的衔接（语义一致性约束）

- **维度值复用既有值域**：供应商 id 3（北方钢铁，既有 PO/收货/发票/付款链供应商）/ 4（东方化工，比价陪跑）；物料 id 3（原料 X，PO-2026-001 订购物料）为主、1/2/4 复用 master-data 既有物料；仓库 id 2（WH-RAW，与既有收货同仓）；组织 id 2；币种 id 1（CNY）；UoM id 1(PCS)/2(KG)/4(BOX) 与物料既有主单位一致（物料 3→KG、物料 4→BOX）。
- **P2P 故事线连贯**：REQ-2026-001（原料 X 100 千克，2026-06-20）→ RFQ-2026-001（两家报价）→ PQ-2026-001 北方钢铁中标（单价 8.50）→ 既有 PO-2026-001（同物料同单价 8.50）→ 既有 PRCV-2026-001（收货 100）→ PRET-2026-001（部分锈蚀退货申请 10 千克，引用既有收货行）——请购到退货演示链路自洽，单价/数量/日期互不矛盾。
- **中标价沉淀**：`erp_pur_supplier_price_list` 行 1 即报价单 PQ-2026-001 中标价（物料 3 @ 8.50/CNY，供应商 3），对应 `supplier-evaluation.md`「中标报价沉淀为价格主数据」设计。
- **退货数量不超收货**：退货行合计 10（草稿）+ 20（作废，不计数）≤ 既有收货 100@物料3/仓2，账实不矛盾。
- **业务日期**：全部落在 2026-06 下旬 ~ 2026-07 上旬，与既有 P2P 链（PO 2026-07-01 / 收货 2026-07-03 / 发票 2026-07-05 / 付款 2026-07-10）时序衔接。

## 评分卡数值自洽（ScorecardCalculator 口径）

`erp_pur_supplier_scorecard_criteria` 三头各行权重和 = 100（`validateWeightSum` 硬约束），加权得分 = score × weight / 100，Σ 加权得分 = 头表 `TOTAL_SCORE`，standing 落档口径 = `determineStanding`（≥ warn=80 GREEN / ≥ hold=60 YELLOW / < hold RED）：

| 评分卡 | 供应商 | 周期 | Σ 加权得分 | standing 核验 | STATUS |
|---|---|---|---|---|---|
| 1 | 3 北方钢铁 | 2026-Q1 | 36.00+28.50+24.00 = **88.50** | 88.50 ≥ 80 → GREEN ✓ | FINALIZED |
| 2 | 4 东方化工 | 2026-Q1 | 22.00+18.00+12.00 = **52.00** | 52 < 60 → RED ✓ | FINALIZED |
| 3 | 3 北方钢铁 | 2026-Q2 | 32.00+21.90+17.10 = **71.00** | 60 ≤ 71 < 80 → YELLOW ✓ | DRAFT |

`erp_pur_supplier_scorecard_variable`（3 行）指向评分卡 1 的三个维度行，variable.value 与对应 criteria.score 一致（公式 = 变量名直取，对齐 `TestErpPurScorecardCalc` 先例），公式经 XLang 重算可复现静态得分。

## 用例指示编码与 negative 行语义

编码定义见 `docs/architecture/seed-data.md`「270 个缺 seed 实体最小可用数据集规格表」通用约定 5：

- **P（最小正例行）**：全部 12 表均有。
- **N-TERM（终态行）**：各单据头表末行 `DOC_STATUS=CANCELLED`（`erp/doc-status` 字典终态，非法迁移守卫负路径——`ErpPurRfqBizModel.cancel` 非已作废守卫、退货/请购/报价状态机同族）；评分卡 N-TERM = `STATUS=FINALIZED` 终态行（id=2，重算守卫 `ERR_SCORECARD_ALREADY_FINALIZED` 负路径——FINALIZED 即该实体终态）。
- **N-DIS（禁用行）**：`erp_pur_supplier_price_list` 行 2 `IS_ACTIVE=false`（且 `VALID_TO=2026-06-30` 已过期双重禁用语义），供启用前置守卫负路径消费。

## 状态口径

- 单据头正例默认 `DOC_STATUS=ACTIVE + APPROVE_STATUS=APPROVED`（已生效链路态，对齐既有 PO/RFQ/报价 seed 口径）；**退货单头 P 行取 `DRAFT + UNSUBMITTED + POSTED=false`**——退货审批动作会触发反向出库移动 + 红字凭证（`ErpPurReturnBizModel.approve`，E2E `pur-return.action.spec` 断言 approve 后 posted=true），CSV 直载不触发该动作，APPROVED+posted=false 将与系统行为自相矛盾，草稿态最忠实。
- 评分卡变量/维度仅评分卡 1（FINALIZED GREEN）配全变量行；DRAFT 卡（id=3）得分/加权得分为「上次试算快照」语义，FINALIZED 不可重算守卫确保静态行不被覆盖。
- 供应商准入联动：评分卡 2（供应商 4，FINALIZED RED）使 fresh-DB 演示中东方化工保存报价单时命中 `SupplierEligibilityChecker` PREVENT（`erp-pur.scorecard-prevent-on-red` 默认 true）——即 `supplier-evaluation.md` §业务规则3/5 的现成演示数据；北方钢铁最新定稿卡为 GREEN，准入 ALLOW 不受影响。

## 约定对齐

- 列头 = DB 列名大写下划线（含 `UO_M_ID` 命名分叉，逐表以 ORM `code=` 为准）；省略审计列（delVersion/version/createdBy/createTime/updatedBy/updateTime）与审批/过账留痕列（approved_by/approved_at/posted_by/posted_at）；ISO 日期；小写布尔；ID < 100000（`zz-sequence-advance.sql` 序列推进值域约束）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议）。
