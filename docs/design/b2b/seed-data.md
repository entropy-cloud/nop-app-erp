# B2B 域种子数据（Seed Data）

> Owner: 本文件（b2b 域「种子数据」owner doc；plan `2026-09-02-1415-2-m15-b2b-ct-drp-seed-expansion.md` Phase 2 落地）
> 上游规格: `docs/architecture/seed-data.md` 规格表 b2b 节（13 实体逐行规格权威源）+ 对账表（计数权威源）
> 业务语义 owner docs: `docs/design/b2b/`（asn-processing / edi-formats / managed-file-transfer / partner-onboarding / state-machine 等）；本文件只登记种子数据面，不重复业务语义
> 保护区注记: b2b EDI 凭据/外部集成段为 plan-first 保护区——本批落地前经独立 plan-audit 双迭代（iteration 1 NEEDS REVISION → 修订并入 → iteration 2 APPROVE，批准记录落盘 plan Phase 1 执行证据节）

## 1. 范围与文件清单

M1.5 批次（2026-09-02）为 b2b 域补齐 13 个 seed CSV（此前 b2b 为零 seed 域）。文件位于 `app-erp-all/src/main/resources/_vfs/_init-data/`，命名 = `<tableName>.csv`（`DataInitInitializer.loadCsvData` 按表名查找契约）。至此 b2b 域 13 规格实体全量 seed 覆盖（13 / 13）。

| CSV | 行数 | 主子表 | 用例指示 | 说明 |
|---|---|---|---|---|
| erp_b2b_asn.csv | 2 | 头（asn 组头） | P+N-TERM | ASN-SEED-2026-001 RECEIVED_TO_STOCK（真终态正例）+ ASN-SEED-2026-002 CANCELLED（终态；全批 ASN 行全终态承载） |
| erp_b2b_asn_line.csv | 3 | 子表（asn） | P | asn1 两行（MAT-001 ×100 + MAT-002 ×50）+ asn2 取消单行（MAT-003 ×20，SHIPPED_QTY=0） |
| erp_b2b_partner_profile.csv | 2 | 头（partner_profile 组头） | P+N-TERM | 演示集成伙伴A TESTING/SFTP（P）+ 演示集成伙伴B TERMINATED/AS2（终态，ARCHIVED_AT 静态） |
| erp_b2b_certification_checklist.csv | 2 | 独立（FK→partner_profile） | P | profile1 ISO9001 已通过 + DUNS 待补交 |
| erp_b2b_code_mapping.csv | 2 | 独立 | P | MATERIAL 映射（MAT-001↔CUST-MAT-001）+ PARTNER 映射（SUP-001↔GLN-SEED-0001） |
| erp_b2b_edi_format.csv | 2 | 独立 | P+N-DIS | EDIFACT_ORDERS（P，IS_ACTIVE=1）+ X12_850（N-DIS，IS_ACTIVE=0——INTEGER 列分叉，非 boolean）；code 避开 `UBL_*` 族（C03 by-code 碰撞向量） |
| erp_b2b_edi_doc.csv | 2 | 独立（FK→edi_format） | P | EDI-SEED-2026-001 ARCHIVED（出站→asn1）+ EDI-SEED-2026-002 RECEIVED（入站→既有 SHP-2026-001）；relatedBillType 取 ASN/SHIPMENT，避开 SALES_ORDER+IT-C03-SO-001 组合 |
| erp_b2b_edi_log.csv | 2 | 子表（edi_doc） | P | doc1 出站成功日志 + doc2 入站解析日志（payload 无逗号演示 JSON，静态 LOG_TIME） |
| erp_b2b_mft_certificate.csv | 2 | 独立 | P+N-DIS | 演示AS2传输证书（P，IS_ACTIVE=true）+ 停用演示证书（N-DIS，false）；SERIAL_NO UK 两行独立；证书四列（issuer/subject/serial/fingerprint）全 masked |
| erp_b2b_mft_config.csv | 2 | 独立（FK→partner） | P | SFTP 配置（partner 3）+ AS2 配置（partner 4）；TRANSPORT_ENDPOINT 全 masked 伪域；停用列名为 `ACTIVE`（非 IS_ACTIVE，ORM 列名分叉） |
| erp_b2b_mft_log.csv | 2 | 子表（mft_config） | P+N-TERM | config1 SENT 成功传输（P）+ config1 DEAD_LETTER 死信终态（N-TERM，RETRY_COUNT=3） |
| erp_b2b_partner_credential.csv | 2 | 独立（FK→partner_profile） | P+N-DIS | API_KEY 凭据（P）+ USERNAME_PASSWORD 停用凭据（N-DIS，IS_ACTIVE=false）；CREDENTIAL_VALUE 全 masked |
| erp_b2b_test_exchange.csv | 2 | 独立（FK→partner_profile） | P | profile1 出/入站演示测试交换各一（FORMAT_CODE 软指向 EDIFACT_ORDERS，PASSED=true） |

## 2. FK 闭环图

- **〔已 seed〕锚点**：`erp_md_organization`（ORG_ID=2）、`erp_md_partner`（PARTNER_ID=3 = SUP-001 / 4 = SUP-002，asn/mft_config/mft_certificate/code_mapping 的 partner 锚点）。
- **〔本批〕链路**：
  - asn → asn_line（ASN_ID 必填 to-one→asn 1/2，MATERIAL_ID→md_material〔跨域:md·已seed〕）；
  - partner_profile → certification_checklist / partner_credential / test_exchange（PARTNER_PROFILE_ID 必填 to-one→profile 1/2）；
  - edi_doc（FORMAT_ID 可选 to-one→edi_format 1）→ edi_log（EDI_DOC_ID 必填 to-one→edi_doc 1/2）；
  - mft_certificate（独立，partner 锚点）← mft_config（CERT_ID 可选，种子留空）→ mft_log（CONFIG_ID 必填 to-one→mft_config 1）。
- asn.SOURCE_EDI_DOC_ID / mft_config.CERT_ID / dock 类弱指针留空（可选 FK 零悬空最简承载）；asn.RELATED_BILL_TYPE/RELATED_BILL_CODE 与 edi_doc.RELATED_* 为跨域弱指针（ORM 无 to-one，引用完整性门禁天然不校验），取值不命中任何实仓 PO code（`ASN-REF-SEED-00x`）。
- 全部必填 FK 落在〔已 seed〕∪〔本批〕集合内（`TestErpSeedDataIntegrity` 引用完整性断言门禁，白名单零增量；本批 46 边程序化验证全解析）。

## 3. 用例指示编码与 negative 行语义

- **P（最小正例行）**：每表主行，字段值取规整演示值（数量 `.0000` 四位小数、静态日期 2026-05 ~ 2026-08）。
- **N-TERM（终态行）**：asn 2（CANCELLED——audit Minor-4 勘误后 RECEIVED 属 pre-match 态不作终态承载）、partner_profile 2（TERMINATED）、mft_log 2（DEAD_LETTER）——供业务动作非法迁移守卫负路径。
- **N-DIS（禁用/停用行）**：edi_format 2 / partner_credential 2 / mft_certificate 2——partner_credential/mft_certificate 的 IS_ACTIVE 为 BOOLEAN 列取 false，edi_format 的 IS_ACTIVE 为 INTEGER 强制列取 0（audit Minor-3 列型分叉，逐列对齐 ORM）。
- 行级用途以 REMARK 后缀（P）/（N-TERM）/（N-DIS）显式标注（沿 M1.4a/M1.4b 批先例）；列名例外：`erp_b2b_partner_profile` / `erp_b2b_test_exchange` 无 REMARK 列（备注列为 NOTES），二表 N 语义以 STATUS 状态列承载（TESTING→P / TERMINATED→N-TERM）、test_exchange 另以 NOTES 后缀（P）标注，见 §1 表格逐行登记。

## 4. 干扰面零漂移设计（本批核验结论 + 保护段裁决）

- **ASN 三 Processor 事件驱动（预分析 (a) 复证）**：matchPurchaseOrder / retryMatch / createReceiveFromAsn 均为 `@BizMutation` by 显式 asnId（getEntityById 起步），种子行永不被枚举；matchPurchaseOrder 查询面 = `ErpPurOrder.code eq asn.relatedBillCode`（无 relatedBillType 过滤，audit Minor-5 勘误）；CSV 直插不触发 mutation → 零触发面。
- **唯一 b2b job 双门控休眠**：`erp-b2b-onboarding-monitor.job.yaml` enabled 缺省 false + bean cron 空 skip（`ErpB2bOnboardingMonitorJob:106-111`）；其扫描键 = partner_profile status=PRODUCTION + goLiveDate 窗口——种子取 TESTING/TERMINATED + 静态 2026-05/06，双保险不可见。
- **C03 EDI 碰撞向量避让（独立 plan-audit Major-1）**：C03 `createInbound` 以 `formatCode="UBL_ORDER"` by-code 解析 format（无 isActive 过滤），命中将翻转 C03 `erp_b2b_edi_doc` 输出快照 FORMAT_ID 列——种子 edi_format code 避开 `UBL_ORDER` 及整个 `UBL_*` 族（EDIFACT_ORDERS/X12_850）；edi_doc 种子行避用 `relatedBillType=SALES_ORDER` + `relatedBillCode=IT-C03-SO-001` 组合（checkDuplicate UK 面）。C03 对 edi_doc 本体仅消费 createInbound 响应 id（不按 code 查找），output 快照为 `_chgType` 变更行增量，种子行属 before-image 不入变更集。
- **E2E 标记族避让**：dashboard value specs（b2b-asn-flow / b2b-edi-detail）自包含（`E2E-*${Date.now()}` code + relatedBillType `E2E_FLOW`/`E2E_RBT_*`）+ get-by-id + 客户端 id 过滤——种子 relatedBillType/code/编号全批避开 `E2E_` 前缀族与 fixture UK（`IT-C19-ASN-001` / `ASN-WEBHOOK-*` / `IT-C03-SO-001` / partnerCode `probe`）；种子行数（≤3/表）远小于 value specs findPage(limit:5000) 首页截断阈值。
- **masked placeholder 纪律（保护区，audit Major-2 扩展）**：partner_credential.CREDENTIAL_VALUE / partner_profile.TRANSPORT_ENDPOINT+WEBHOOK_SECRET+CERT_FINGERPRINT+CONTACT_* / mft_config.TRANSPORT_ENDPOINT / mft_certificate 证书四列一律取 `MASKED-CREDENTIAL-SEED` 或 `masked-credential-seed.example` 伪域——零真实凭据、零真实端点、零真实个人数据；全仓 grep `MASKED-CREDENTIAL-SEED` 可审计辨识。
- **预存 ORM quirk 登记**：`erp_b2b_edi_doc.BLOCKING_LEVEL` 强制列 default="10" 而字典值为 INFO/WARN/ERROR——种子显式取 `INFO` 归位字典域（quirk 修正归 successor）。
- **集成快照零漂移**：`app-erp-all/_cases` b2b 相关用例仅 C03/C19（grep 实证 6 用例全集中的 b2b 面），均无种子计数断言；纯加性插入不入既有快照（M1.2b 先例）。
- **UK 自洽**：asn/edi_doc/partner_profile UK(code,orgId) 与 edi_doc UK(formatId,relatedBillType,relatedBillCode)、mft_certificate UK(serialNo) 逐行独立。

## 5. 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准，脚本化全等校验 13/13）；省略审计列；日期 ISO（`2026-08-01`）、时间戳空格分隔（`2026-08-03 09:00:00`）；小写布尔（BOOLEAN 列）/0-1（INTEGER 列 IS_ACTIVE/NEEDS_WEB_SERVICE）逐列对齐；ID < 100000（各表独立自 1 起段）；payload/ALLOWED_FORMATS 等文本列取无逗号演示值（零 CSV 引号转义需求）；字典码 ∈ `module-b2b/model/app-erp-b2b.orm.xml` `<dicts>`（erp-b2b/ 命名空间）。
- 拓扑序由 `DataInitInitializer` 按 ORM `getEntityModelsInTopoOrder()` 自动排序，同批主子表（asn_line / edi_log / mft_log / credential / checklist / test_exchange 晚于各自头表）无需手工排序文件名。
