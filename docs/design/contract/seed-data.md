# Contract 域种子数据（Seed Data）

> Owner: 本文件（contract 域「种子数据」owner doc；plan `2026-09-02-1415-2-m15-b2b-ct-drp-seed-expansion.md` Phase 2 落地）
> 上游规格: `docs/architecture/seed-data.md` 规格表 contract 节（15 实体逐行规格权威源）+ 对账表（计数权威源）
> 业务语义 owner docs: `docs/design/contract/`（approval-workflow / contract-repository / e-signature / volume-discount / state-machine 等）；本文件只登记种子数据面，不重复业务语义

## 1. 范围与文件清单

M1.5 批次（2026-09-02）为 contract 域补齐 15 个 seed CSV（此前 contract 为零 seed 域）。文件位于 `app-erp-all/src/main/resources/_vfs/_init-data/`，命名 = `<tableName>.csv`。至此 contract 域 15 规格实体全量 seed 覆盖（15 / 15）。

| CSV | 行数 | 主子表 | 用例指示 | 说明 |
|---|---|---|---|---|
| erp_ct_contract.csv | 2 | 头（contract 组头） | P+N-TERM | CT-SEED-2026-001 SERVICE/OUTBOUND ACTIVE（生效中，endDate 静态 2026-12-31）+ CT-SEED-2026-002 PURCHASE/INBOUND TERMINATED（终态） |
| erp_ct_contract_line.csv | 3 | 子表（contract） | P | contract1 两行（Σ=120000 = 头 TOTAL_AMOUNT）+ contract2 一行（Σ=80000 = 头） |
| erp_ct_contract_version.csv | 2 | 子表（contract） | P+N-TERM | contract1 V1 FINALIZED/isCurrent（P，待签态正例）+ contract2 V1 SIGNED/isCurrent（终态） |
| erp_ct_signature_request.csv | 2 | 独立（FK→contract_version） | P+N-TERM | version1 PENDING_SIGNATURE/MOCK（P）+ version2 REJECTED（终态，COMPLETED_AT 静态）；PROVIDER_REQUEST_ID 留空（可选自引用 to-one，自由文本将构成门禁悬空） |
| erp_ct_approval_matrix.csv | 2 | 独立 | P+N-DIS | 10 万级 contract-manager 矩阵（P）+ general-manager 档（N-DIS，IS_ACTIVE=false） |
| erp_ct_approval_record.csv | 2 | 独立（FK→contract+matrix） | P | contract1 APPROVED + contract2 REJECTED（均非 PENDING/WAITING——approval-timeout job 扫描键，双保险避让） |
| erp_ct_document.csv | 2 | 独立（FK→contract） | P | contract1 扫描件（OCR_STATUS '10'）+ contract2 已归档文档（IS_ARCHIVED=true，ARCHIVE_DATE 静态）；DOC_TYPE 取字典数字码域 '10'/'20' |
| erp_ct_invoice_plan.csv | 2 | 独立（FK→contract_line） | P | line1 MILESTONE 2027-03-31 + COMPLETION 2027-06-30 各 60000（Σ=120000 与头表自洽）；planDate 静态 2027 不落 triggerDuePlans due 窗口 |
| erp_ct_rebate_agreement.csv | 2 | 头（rebate 组头） | P+N-TERM | SALES/DRAFT partner2（P）+ PURCHASE/SETTLED partner4（终态，TOTAL_ACCUMULATED_AMOUNT=1600 与 accrual Σ 自洽）；窗口静态 2026-01-01~06-30（早于 C18 accrual 窗口 2026-07-07 起） |
| erp_ct_rebate_tier.csv | 2 | 独立（FK→agreement） | P | agr1 两档（0~10 万 2% / 10 万+ 3%） |
| erp_ct_rebate_accrual.csv | 2 | 独立（FK→agreement） | P | agr2 两笔 AP_INVOICE 累计（1000+600=1600 = agr2 TOTAL_ACCUMULATED_AMOUNT，IS_SETTLED=true + SETTLED_DATE 静态） |
| erp_ct_rebate_settlement.csv | 2 | 独立（FK→agreement） | P+N-TERM | agr2 POSTED（POSTED=true，贷项凭证 CT-CM-SEED-0001）+ agr2 CANCELLED（N-TERM，POSTED=false） |
| erp_ct_template.csv | 2 | 独立 | P+N-DIS | SERVICE 模板（P）+ PURCHASE 停用模板（N-DIS）；UK(code) 两行独立 |
| erp_ct_consumption_line.csv | 2 | 独立（FK→contract_line） | P | line1 两笔消耗（30+20 件，来源 SO 弱指针 SEED-REF 码不命中实仓单据） |
| erp_ct_volume_discount.csv | 2 | 独立（FK→contract_line） | P | line1 两档数量折扣（100~500 件 5% / 500+ 件 8%） |

## 2. FK 闭环图

- **〔已 seed〕锚点**：`erp_md_organization`（ORG_ID=2）、`erp_md_partner`（PARTNER_ID=2 = CUST-002 / 4 = SUP-002——**避开 C18 fixture partnerId 1/3**）、`erp_md_material`（MATERIAL_ID=1/2/3）、`erp_md_currency`（CURRENCY_ID=1 CNY）。
- **〔本批〕链路**（3 组主子表 + 2 条 rebate 链）：
  - contract → contract_line（CONTRACT_ID 必填 to-one）→ invoice_plan / consumption_line / volume_discount（CONTRACT_LINE_ID 必填 to-one→line 1）；
  - contract → contract_version（CONTRACT_ID 必填 to-one）→ signature_request（CONTRACT_VERSION_ID 必填 to-one→version 1/2）；
  - rebate_agreement（PARTNER_ID 必填→md_partner；CONTRACT_ID 可选→contract 1/2）→ rebate_tier / rebate_accrual / rebate_settlement（REBATE_AGREEMENT_ID 必填 to-one→agreement 1/2）；
  - approval_record（CONTRACT_ID 必填→contract 1/2 + APPROVAL_MATRIX_ID 可选→matrix 1）；document（CONTRACT_ID 可选→contract 1/2）。
- 全部必填 FK 落在〔已 seed〕∪〔本批〕集合内（`TestErpSeedDataIntegrity` 门禁；本批 ct 段 54 边程序化验证全解析，白名单零增量）。

## 3. 用例指示编码与 negative 行语义

- **P（最小正例行）**：每表主行，金额 `.00` 两位小数、数量 `.0000`、静态日期 2025-12 ~ 2027-06。
- **N-TERM（终态行）**：contract 2（TERMINATED）、contract_version 2（SIGNED——version-status 字典唯一终态）、signature_request 2（REJECTED）、rebate_agreement 2（SETTLED）、rebate_settlement 2（CANCELLED）。
- **N-DIS（禁用/停用行）**：approval_matrix 2 / template 2（IS_ACTIVE=false，BOOLEAN 列）。
- 行级用途以 REMARK 后缀（P）/（N-TERM）/（N-DIS）显式标注（沿 M1.4a/M1.4b 批先例）。

## 4. 干扰面零漂移设计（本批核验结论）

- **C18 零交互（预分析 (b) 复证）**：`ErpCtRebateAgreement__runAccrual` 按显式单一 agreementId 装载（per-mutation Processor），聚合过滤 = posted=true + businessDate ∈ [agreement.startDate, asOfDate] + customerId/supplierId = agreement.partnerId，accrued 去重 per-agreementId——**无任何全表迭代 runAccrual 的调用方**，种子 agreement 行（DRAFT/SETTLED，partner 2/4，窗口 ≤2026-06-30）与 C18（own agreement partner 1 ACTIVE 窗口 2026-07-07~12-31，全断言 by runtime id）恒零交集。C18 fixture AR 发票消费面在 sales 域种子（非本批）。
- **C02 零交互（预分析 (g) 复证）**：C02 自包含 `ErpCtContract__save`（javadoc 自证无 contract seed 前提），output `erp_ct_contract.csv` 为 `_chgType` 变更行增量（恰 1 条 A 行 ID=100000）——种子行属 before-image；**ID < 100000 纪律 load-bearing**（`zz-sequence-advance.sql` 钉 default 序列 NEXT_VALUE=100000，种子显式 id 不消费序列）。
- **ct 批扫描面休眠实证**：`erp-ct-contract-expiry` / `erp-ct-approval-timeout` / `erp-ct-doc-retention` 三 job yaml 均 enabled 缺省 false；belt-and-braces 种子仍避开其扫描键——contract1 endDate 2026-12-31（30/15/7 天到期通知窗外）、approval_record 全 APPROVED/REJECTED（无 PENDING）、signature_request 轮询 cron 零消费方（`erp-ct.signature-status-polling-cron` 无 job yaml）。
- **triggerDuePlans 只读惰性**：`ErpCtInvoicePlanTriggerDuePlansProcessor` 读全部 due plans（planDate≤asOfDate AND isInvoiced=false）但仅 mutate 参数 contractId 的行——种子 planDate 静态 2027 恒不 due，读面亦不可达 e2e 断言（该 spec 按 own contractId 断言 ≥2）。
- **E2E/视觉消费面**：ct 5 个 action spec + contract.smoke 全自包含（unique Date.now() code + own-id/own-contractId 断言 + 自清理），无 totalCount 断言；`ext-domains-child-table.visual.spec.ts`（/ErpCtContract-main 2 sub-grids row-update-button）与 `f12-page-structure.visual.spec.ts`（7 tabs Tier D）此前依赖 no-row skip 分支，种子落地后激活真实行路径——DOM 结构断言不依赖行数（Phase 3 实跑核验）；`status-tag.visual.spec.ts` 对 contract 为 SOFT_PROBE。
- **预存 ORM quirk 登记（已消解）**：`erp-ct/sign-status` / `erp-ct/sign-provider` 字典曾存在 option 数值轨（'10'~'60'/'99'）与运行时词汇（`ErpCtConstants:49-57` 写 `PENDING_SIGNATURE`/`REJECTED`/`MOCK` 码）双轨并存 quirk——已由 P1-CK-ct-025-r3 修复（plan `2026-09-10-1141-2` Phase 1）收敛为 value==code 单轨语义编码，种子按运行时词汇取值现已与字典值域一致，quirk 注记就此关闭。
- **集成快照零漂移**：`app-erp-all/_cases` ct 相关用例仅 C02/C18，均无种子计数断言；纯加性插入不入既有快照。
- **UK/数值自洽**：contract/matrix/agreement/document/template UK(code[,orgId]) 逐行独立；line Σ = 头 TOTAL_AMOUNT（120000/80000）；invoice_plan Σ = 头 TOTAL_AMOUNT；accrual Σ = agreement TOTAL_ACCUMULATED_AMOUNT = settlement POSTED 行金额。

## 5. 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准，脚本化全等校验 15/15）；省略审计列；日期 ISO、时间戳空格分隔；小写布尔逐列对齐；ID < 100000（各表独立自 1 起段）；文本列取无逗号演示值；字典码 ∈ `module-contract/model/app-erp-contract.orm.xml` `<dicts>`（erp-ct/ 命名空间；sign-status/sign-provider 按运行时词汇，见 quirk 登记）。
- 拓扑序由 `DataInitInitializer` 自动排序（line/version 晚于 contract；tier/accrual/settlement 晚于 agreement；signature_request 晚于 version），无需手工排序文件名。
