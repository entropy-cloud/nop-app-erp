# 中国本地化:金税接口(L10N CN Golden Tax)

## 目的

设计中国大陆增值税发票(专票/普票)、金税接口(导出/导入)、发票认证、税率配置,遵循 OCA 本地化模块化策略。

## 模块结构决策

对照 `docs/analysis/erp-survey/2026-06-22-0000-odoo-l10n-china.md` §5 的 OCA 模式(本地化做成独立可拔模块)。**独立工程 `module-l10n-cn`**(独立 Maven 工程 + 独立 appName `erp-l10n-cn`),不内建在核心 ERP 实体里。金税接口/发票字段/银行对账规则可单独演进,不污染通用进销存模型。本仓已有 18 个业务域工程的同等粒度,新增一个本地化工程符合既有结构。

工程结构沿用本仓约定:
```
module-l10n-cn/
├── model/app-erp-l10n-cn.orm.xml
├── erp-l10n-cn-dao/
├── erp-l10n-cn-service/
├── erp-l10n-cn-web/
└── erp-l10n-cn-app/
```

在 `app-erp-all/pom.xml` 作为**可选依赖**引入(本地化按需启用)。

命名:实体 `ErpL10nCn*`,表名 `erp_l10n_cn_*`,字典 `erp-l10n-cn/*`。

## 实体清单

> 字段约定遵循 `docs/design/domain-design-guidelines.md` §10/§11。表前缀 `erp_l10n_cn_`。

### ErpL10nCnVatInvoice(增值税发票,表 `erp_l10n_cn_vat_invoice`)

| 字段 | 含义 |
|---|---|
| id/code/orgId | 标准 |
| goldenTaxNo | 金税发票号码(税务端分配,8 位流水号 + 10 位代码) |
| invoiceCode | 发票代码(12 位) |
| invoiceType | 发票类型 dict `erp-l10n-cn/invoice-type`:SPECIAL=10(增值税专用发票)/GENERAL=20(普通)/ELECTRONIC_SPECIAL=30/ELECTRONIC_GENERAL=40 |
| businessDate | 开票日期 |
| purchaserName/purchaserTaxNo/purchaserAddr/purchaserBankAccount | 购方四要素(名称/税号/地址电话/开户行账号) |
| salesName/salesTaxNo/salesAddr/salesBankAccount | 销方四要素 |
| amountWithoutTax | 不含税金额(合计) |
| taxAmount | 税额 |
| amountWithTax | 价税合计(amountWithoutTax+taxAmount) |
| amountInWords | 价税合计(中文大写) |
| taxRateId | 适用税率(→ErpL10nCnTaxRate) |
| currencyId/exchangeRate | 多币种(默认 CNY) |
| remarks | 发票备注(清单标志等) |
| payeeName/checkerName/invoiceClerkName | 收款人/复核/开票人 |
| sourceBillType/sourceBillCode | 来源业务单(销售发票/采购发票,凭证指针反查 sales/purchase 域) |
| taxControlStatus | 金税状态 dict `erp-l10n-cn/golden-tax-status`:NOT_ISSUED=10/EXPORTED=20/ISSUED=30/PRINTED=40/CANCELLED=50 |
| goldenTaxFile | 金税导出文件名/路径 |
| verificationStatus | 认证状态 dict `erp-l10n-cn/verification-status`:UNVERIFIED=10/VERIFIED=20/FAILED=30(仅专票进项) |
| verificationDate/verificationBy | 认证日期/人 |
| docStatus/approveStatus | 双轴状态 |
| posted/postedAt/postedBy | 业财过账(发票生成税务凭证) |
| 标准审计字段 | |

### ErpL10nCnVatInvoiceLine(发票明细,表 `erp_l10n_cn_vat_invoice_line`)

| 字段 | 含义 |
|---|---|
| id/invoiceId/lineNo | 主键/父/行号 |
| goodsName | 货物或应税劳务名称 |
| specModel | 规格型号 |
| unit/unitPrice | 单位/单价 |
| quantity | 数量(DECIMAL(20,4)) |
| amountWithoutTax | 金额(不含税) |
| taxRate | 税率%(DECIMAL(6,4),如 13.0000) |
| taxAmount | 税额 |
| amountWithTax | 价税合计 |
| discountAmount | 折扣额 |
| preferentialPolicy | 税收优惠政策编码 |
| materialId | 关联物料(可选,→ErpMdMaterial notGenCode) |
| 标准审计字段 | |

### ErpL10nCnTaxRate(税率配置,表 `erp_l10n_cn_tax_rate`)

| 字段 | 含义 |
|---|---|
| id/code/name | 主键/编码/名称 |
| ratePercent | 税率(13/9/6/3/1/0 等,DECIMAL(6,4)) |
| rateType | dict `erp-l10n-cn/rate-type`:SPECIAL=10(基本税率)/LOW=20(低税率)/ZERO=30(零税率)/EXEMPT=40(免税) |
| outputSubjectId/inputSubjectId | 销项/进项科目(→ErpMdSubject notGenCode) |
| isActive | 启用 |

### ErpL10nCnGoldenTaxLog(金税接口日志,表 `erp_l10n_cn_golden_tax_log`)

| 字段 | 含义 |
|---|---|
| id/logTime | 主键/日志时间 |
| operationType | dict `erp-l10n-cn/golden-tax-op`:EXPORT=10(导出至税控盘)/IMPORT=20(导入回执)/QUERY=30/ISSUE=40(开具)/CANCEL=50(作废) |
| invoiceId | 关联发票 |
| requestPayload/responsePayload | 请求/响应报文(TEXT) |
| resultCode/resultMsg | 结果码/消息 |
| fileName | 导入导出文件名 |
| operatorId | 操作人 |

> 新增字典:`erp-l10n-cn/invoice-type`、`erp-l10n-cn/golden-tax-status`、`erp-l10n-cn/verification-status`、`erp-l10n-cn/rate-type`、`erp-l10n-cn/golden-tax-op`。

## 关键流程

1. **金税导出**:发票审核(ApproveStatus=APPROVED)后,导出到税控系统标准格式(XML/TXT,遵循金税盘/百望/航天信息接口规范)。导出动作写 ErpL10nCnGoldenTaxLog(operationType=EXPORT),发票 taxControlStatus→EXPORTED。

2. **金税导入/开具**:税控系统开具后回传发票号码/代码,导入更新 goldenTaxNo/invoiceCode,taxControlStatus→ISSUED,log operationType=IMPORT/ISSUE。

3. **发票认证**:仅专票(invoiceType=SPECIAL,且为本方取得的进项),经勾选认证平台认证后更新 verificationStatus=VERIFIED,认证结果影响进项税额抵扣凭证(经 finance 域 IErpFinAcctDocProvider 注册 VAT_INPUT_TAX 类型)。

4. **税率配置**:独立维护,支持多税率并存,关联科目用于自动生成税务凭证。

5. **本地化隔离**:本模块**不修改 sales/purchase 域发票实体**;sales/purchase 域发票需开金税票时,在本地化模块内生成 ErpL10nCnVatInvoice 并用 sourceBillType/sourceBillCode 反查(凭证指针模式),保持核心域纯净(OCA §5 借鉴点 1)。

## 与现有实体的关系

- **sales/purchase 域发票**:通过 sourceBillType/sourceBillCode 反查(凭证指针),不修改其实体。
- **ErpMdSubject**:税率配置关联销项/进项科目。
- **ErpMdMaterial**:发票明细可选关联物料。
- **finance IErpFinAcctDocProvider**:注册 VAT_INPUT_TAX/VAT_OUTPUT_TAX 类型生成税务凭证。

## 关键决策

> **独立 module-l10n-cn 工程,凭证指针反查核心域** —— 遵循 OCA 本地化模块化策略,金税字段/接口不污染通用进销存模型。核心域(sales/purchase)保持纯净,本地化按需启用。这与中国本地化的"金税是合规外挂,非核心业务"的定位一致。

## 菜单归属

新增 l10n-cn 域 TOPM「中国本地化」(orderNo=3000,icon=`map-pin`),分组:增值税发票(发票头/明细)、金税接口(导出导入日志)、税率配置。

## 实施

本模块为**设计阶段**,ORM 模型与 Maven 工程在本地化实际启用时再建。当前菜单先以占位形式接入 app-erp-all 的系统配置区域,待 module-l10n-cn 工程建立后切换为实体页面。

## 参考

- `docs/analysis/erp-survey/2026-06-22-0000-odoo-l10n-china.md`(OCA 金税模块化策略、增值税发票规范)
