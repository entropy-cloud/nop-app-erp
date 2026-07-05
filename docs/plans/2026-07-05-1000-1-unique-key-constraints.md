# 2026-07-05-1000-1-unique-key-constraints 全域唯一键约束补充

> Plan Status: active
> Last Reviewed: 2026-07-05
> Source: 幂等设计审计（docs/analysis/2026-07-01-1900-platform-best-practices-compliance-audit.md O2 发现） + 幂等性设计文档补充（docs/architecture/idempotency-pattern.md）
> Related: docs/plans/2026-07-01-1900-1-platform-compliance-remediation.md（原始 defer 计划，O2 项）
> Audit: required

## Current Baseline

1. **全工程 18 域 ORM XML，仅 2 个 `<unique-key>`**：`uk_config_org`（master-data 的 `configKey,orgId`）和 `UK_EDI_DOC_FORMAT_BILL`（b2b 的 `formatId,relatedBillType,relatedBillCode`）。均非业务单据编号唯一约束。
2. **~150+ `code` 字段（单据编号/编码）无唯一索引**。覆盖所有域：purchase（ErpPurOrder.code 等 8 实体）、sales（8 实体）、inventory（7 实体）、finance（13 实体）、assets（9 实体）、manufacturing（11 实体）、master-data（13 实体）、quality（8 实体）、hr（13 实体）、crm（17 实体）、cs（12 实体）、projects（7 实体）、maintenance（8 实体）、drp、aps、b2b、contract、logistics。
3. **平台审计标记 O2（中等），原始 plan 归类为 priority 5（bootstrap 期一次性统一），已 defer。**
4. **幂等设计专案（docs/architecture/idempotency-pattern.md）明确要求**：异步入口必须 DB 级 UNIQUE 兜底（规则 2），幂等键必须是业务自然键（规则 1）。

## Goals

1. 为所有业务单据和主数据的 `code` 字段（业务自然键）添加 `<unique-key>`
2. 为异步入口幂等所需的组合键添加 `<unique-key>`（webhook eventId、导入去重键等）
3. 建立命名规范与未来新增实体的默认规则

## Non-Goals

- 不添加非唯一性业务索引（如 `(orgId, delFlag)`、`(docStatus, approveStatus)` 等高频过滤路径）— 归独立索引计划
- 不修改现有 entity 类的 Java 代码
- 不涉及数据迁移（现有数据已无重复假定，DDL 变更由 `nop-cli codegen` 生成 DDL 时自动产生）
- 不涉及 `lineNo`、`*Code` 外键引用字段的唯一约束

## Task Route

- Type: `app-layer design change`（模型变更）+ `implementation-only change`（ORM 变更）
- Owner Docs: `docs/architecture/idempotency-pattern.md`、`docs/design/domain-design-guidelines.md §1.3`、18 域各自的 `model/app-erp-<domain>.orm.xml`
- Skill Selection Basis: 需要理解 ORM XML 的 `<unique-keys>` 语法、各域实体业务含义以确认 `code` 是否为自然键；不涉及 Java 代码。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 唯一约束只修改源模型文件（`module-<domain>/model/app-erp-<domain>.orm.xml`），不需环境变量或外部服务。

## Execution Plan

> ⚠ **实体清单为草案指示性，非权威。** 阶段 2–5 的实体表在独立草案审查中被发现与实际 ORM 模型不一致。执行任何域之前，**必须**以该域 `module-<domain>/model/app-erp-<domain>.orm.xml` 为权威，重新枚举所有含 `code`（或单品追踪 `serialNo`）字段的实体，并按 Phase 1 规则分配唯一键。已确认的偏差示例（master-data / purchase 抽查）：
> - **master-data**：`ErpMdCategory` 实为 `ErpMdMaterialCategory`；`ErpMdUnit` 实为 `ErpMdUoM`；`ErpMdConfig` 实为 `ErpSysConfig`（字段为 `configKey`，已由现有 `uk_config_org` 覆盖，**无需新增**）；`ErpMdBook`、`ErpMdCarrier`、`ErpMdCarrierAccount` 在全仓**不存在**。
> - **purchase**：`ErpPurCheck`、`ErpPurDelivery` **不存在**；漏列 `ErpPurQuotation`、`ErpPurRfq`、`ErpPurSupplierPriceList`。
>
> 因此各表计数（"13 个"、"8 个" 等）仅为近似上界，非承诺。每域核对后的真实清单记录于 Closure 证据。未抽查域（sales/inventory/finance/assets/manufacturing/quality 及 Phase 6 十域）同样以实际 ORM 为准，不得直接信任下表。

### Phase 1 — Naming Convention & 统一决策

Status: planned
Targets: 确定唯一键命名、scope 策略、多租户处理
Skill: none

- [ ] Decision: 唯一键命名规范
      - 格式：`UK_{ENTITY_SHORT_NAME}_{COLUMNS}`，全部大写，下划线分隔
      - 示例：`UK_PUR_ORDER_CODE`、`UK_SAL_ORDER_CODE`、`UK_FIN_VOUCHER_CODE_ORG`
      - 理由：与现有 `UK_EDI_DOC_FORMAT_BILL` 保持一致
      - Skill: none
- [ ] Decision: scope 策略 — `(code, orgId)` vs `(code)` 全局唯一
      - **主数据类**（物料/伙伴/科目/仓库/员工/设备/资产/BOM 等）：`(code, orgId)`，同一组织内编码唯一，不同组织可重复
      - **单据类**（采购/销售/库存/财务/生产/质量等单号）：`(code, orgId)`，单号生成通常已包含 org 前缀，但双保险用 `(code, orgId)`
      - **全局编码类**（币种/税率/会计期间/系统配置）：`(code)` 全局唯一
      - 理由：nop-app-erp 支持多组织（`docs/architecture/multi-company.md`），`orgId` 是标准字段
      - Skill: none
- [ ] Decision: 重复处理策略 — 如果现有数据存在违反唯一约束的情况，以日志警告而非阻止启动的方式处理
      - 实现方式：`nop-ddl` 生成的 DDL 不包含 `IF NOT EXISTS` 检查，需 `codegen` 后审查 DDL，必要时手动 SQL 清理
      - 风险：bootstrap 期数据量小，实际重复概率低
      - Skill: none

Exit Criteria:
- [x] 命名规范和 scope 策略已记录（固化在本 plan 中，不另写文档）

### Phase 2 — Master Data（低风险，基础域）

Status: planned
Targets: `module-master-data/model/app-erp-master-data.orm.xml`
Skill: none

| 实体 | 唯一键名 | 列 | Scope |
|------|---------|---|-------|
| ErpMdMaterial | `UK_MD_MATERIAL_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdCategory | `UK_MD_CATEGORY_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdPartner | `UK_MD_PARTNER_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdUnit | `UK_MD_UNIT_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdCurrency | `UK_MD_CURRENCY_CODE` | `code` | 全局 |
| ErpMdTaxRate | `UK_MD_TAX_RATE_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdWarehouse | `UK_MD_WAREHOUSE_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdEmployee | `UK_MD_EMPLOYEE_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdSubject | `UK_MD_SUBJECT_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdBook | `UK_MD_BOOK_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdCarrier | `UK_MD_CARRIER_CODE_ORG` | `code, orgId` | 组织 |
| ErpMdCarrierAccount | `UK_MD_CARRIER_ACCT_CODE_ORG` | `code, orgId` | 组织 |
| ~~ErpMdConfig~~ | — | — | 已由现有 `uk_config_org`（`configKey,orgId`）覆盖；且该实体实为 `ErpSysConfig`，无 `code` 字段，**跳过** |

- [ ] Add: master-data 全部含 `code` 字段的实体（草案指示约 12 个，以实际 ORM 枚举为准）`<unique-key>` 到 `app-erp-master-data.orm.xml`
      - Skill: none

Exit Criteria:
- [ ] master-data ORM 模型所有含 `code`/`serialNo` 字段的实体已添加 `<unique-key>`，格式与命名规范一致；实体清单已与实际 `app-erp-master-data.orm.xml` 核对一致
- [ ] `nop-cli codegen` 可正常生成对应域的 DDL（本地运行验证）

### Phase 3 — 进销存（采购/销售/库存）

Status: planned
Targets: `module-purchase/model/app-erp-purchase.orm.xml`, `module-sales/model/app-erp-sales.orm.xml`, `module-inventory/model/app-erp-inventory.orm.xml`
Skill: none

采购：

| 实体 | 唯一键名 | 列 |
|------|---------|---|
| ErpPurRequisition | `UK_PUR_REQ_CODE_ORG` | `code, orgId` |
| ErpPurOrder | `UK_PUR_ORDER_CODE_ORG` | `code, orgId` |
| ErpPurReceive | `UK_PUR_RECEIVE_CODE_ORG` | `code, orgId` |
| ErpPurReturn | `UK_PUR_RETURN_CODE_ORG` | `code, orgId` |
| ErpPurInvoice | `UK_PUR_INVOICE_CODE_ORG` | `code, orgId` |
| ErpPurPayment | `UK_PUR_PAYMENT_CODE_ORG` | `code, orgId` |
| ErpPurCheck | `UK_PUR_CHECK_CODE_ORG` | `code, orgId` |
| ErpPurDelivery | `UK_PUR_DELIVERY_CODE_ORG` | `code, orgId` |

销售：

| 实体 | 唯一键名 | 列 |
|------|---------|---|
| ErpSalOrder | `UK_SAL_ORDER_CODE_ORG` | `code, orgId` |
| ErpSalContract | `UK_SAL_CONTRACT_CODE_ORG` | `code, orgId` |
| ErpSalDelivery | `UK_SAL_DELIVERY_CODE_ORG` | `code, orgId` |
| ErpSalReturn | `UK_SAL_RETURN_CODE_ORG` | `code, orgId` |
| ErpSalInvoice | `UK_SAL_INVOICE_CODE_ORG` | `code, orgId` |
| ErpSalReceipt | `UK_SAL_RECEIPT_CODE_ORG` | `code, orgId` |
| ErpSalQuotation | `UK_SAL_QUOTATION_CODE_ORG` | `code, orgId` |

库存：

| 实体 | 唯一键名 | 列 |
|------|---------|---|
| ErpInvStockMove | `UK_INV_STOCK_MOVE_CODE_ORG` | `code, orgId` |
| ErpInvInRecord | `UK_INV_IN_RECORD_CODE_ORG` | `code, orgId` |
| ErpInvOutRecord | `UK_INV_OUT_RECORD_CODE_ORG` | `code, orgId` |
| ErpInvTransfer | `UK_INV_TRANSFER_CODE_ORG` | `code, orgId` |
| ErpInvCostAdjust | `UK_INV_COST_ADJ_CODE_ORG` | `code, orgId` |
| ErpInvLot | `UK_INV_LOT_CODE_ORG` | `code, orgId` |

> 注意：`ErpInvSerialNo.serialNo` 应加唯一键 `UK_INV_SERIAL_NO`（`serialNo` 全局唯一，单品追踪）。`ErpInvOnHand`（库存余额）无业务 `code`，不需要。

- [ ] Add: purchase 8 个 `<unique-key>`
- [ ] Add: sales 7 个 `<unique-key>`
- [ ] Add: inventory 6 个 `<unique-key>` + serialNo 唯一键
      - Skill: none

Exit Criteria:
- [ ] 进销存三域的 ORM 模型全部含 `code`/`serialNo` 字段的实体已添加 `<unique-key>`；实体清单已与各域实际 ORM 核对一致（修正草案表偏差，如 purchase 的 `ErpPurCheck`/`ErpPurDelivery` 不存在、漏列 `ErpPurQuotation`/`ErpPurRfq`/`ErpPurSupplierPriceList`）

### Phase 4 — 财务 & 资产

Status: planned
Targets: `module-finance/model/app-erp-finance.orm.xml`, `module-assets/model/app-erp-assets.orm.xml`
Skill: none

财务（含资金）：

| 实体 | 唯一键名 | 列 |
|------|---------|---|
| ErpFinVoucher | `UK_FIN_VOUCHER_CODE_ORG` | `code, orgId` |
| ErpFinVoucherTemplate | `UK_FIN_VOUCHER_TMPL_CODE_ORG` | `code, orgId` |
| ErpFinPeriod | `UK_FIN_PERIOD_CODE` | `code` |
| ErpFinSettlement | `UK_FIN_SETTLEMENT_CODE_ORG` | `code, orgId` |
| ErpFinPayment | `UK_FIN_PAYMENT_CODE_ORG` | `code, orgId` |
| ErpFinAccount | `UK_FIN_ACCOUNT_CODE_ORG` | `code, orgId` |
| ErpFinReceivable | `UK_FIN_RECEIVABLE_CODE_ORG` | `code, orgId` |
| ErpFinPayable | `UK_FIN_PAYABLE_CODE_ORG` | `code, orgId` |
| ErpFoPaymentRequest | `UK_FO_PAY_REQ_CODE_ORG` | `code, orgId` |
| ErpFoPaymentOrder | `UK_FO_PAY_ORDER_CODE_ORG` | `code, orgId` |
| ErpFinNotesReceivable | `UK_FIN_NOTES_RECV_CODE_ORG` | `code, orgId` |
| ErpFinNotesPayable | `UK_FIN_NOTES_PAY_CODE_ORG` | `code, orgId` |
| ErpFinReconciliation | `UK_FIN_RECON_CODE_ORG` | `code, orgId` |

资产：

| 实体 | 唯一键名 | 列 |
|------|---------|---|
| ErpAstAsset | `UK_AST_ASSET_CODE_ORG` | `code, orgId` |
| ErpAstCategory | `UK_AST_CATEGORY_CODE_ORG` | `code, orgId` |
| ErpAstAcquisition | `UK_AST_ACQ_CODE_ORG` | `code, orgId` |
| ErpAstScrap | `UK_AST_SCRAP_CODE_ORG` | `code, orgId` |
| ErpAstInventory | `UK_AST_INVENTORY_CODE_ORG` | `code, orgId` |
| ErpAstTransfer | `UK_AST_TRANSFER_CODE_ORG` | `code, orgId` |
| ErpAstMaintenance | `UK_AST_MAINT_CODE_ORG` | `code, orgId` |
| ErpAstRepair | `UK_AST_REPAIR_CODE_ORG` | `code, orgId` |
| ErpAstProject | `UK_AST_PROJECT_CODE_ORG` | `code, orgId` |

- [ ] Add: finance 13 个 `<unique-key>`
- [ ] Add: assets 9 个 `<unique-key>`
      - Skill: none

Exit Criteria:
- [ ] 财务和资产两域全部含 `code` 字段的实体已添加 `<unique-key>`；实体清单已与各域实际 ORM 核对一致

### Phase 5 — 生产 & 质量

Status: planned
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`, `module-quality/model/app-erp-quality.orm.xml`
Skill: none

生产：

| 实体 | 唯一键名 | 列 |
|------|---------|---|
| ErpMfgBom | `UK_MFG_BOM_CODE_ORG` | `code, orgId` |
| ErpMfgRouting | `UK_MFG_ROUTING_CODE_ORG` | `code, orgId` |
| ErpMfgWorkCenter | `UK_MFG_WK_CENTER_CODE_ORG` | `code, orgId` |
| ErpMfgWorkOrder | `UK_MFG_WORK_ORDER_CODE_ORG` | `code, orgId` |
| ErpMfgVersion | `UK_MFG_VERSION_CODE_ORG` | `code, orgId` |
| ErpMfgPlan | `UK_MFG_PLAN_CODE_ORG` | `code, orgId` |
| ErpMfgIssue | `UK_MFG_ISSUE_CODE_ORG` | `code, orgId` |
| ErpMfgCompletion | `UK_MFG_COMPLETION_CODE_ORG` | `code, orgId` |
| ErpMfgMove | `UK_MFG_MOVE_CODE_ORG` | `code, orgId` |
| ErpMfgQualityCheck | `UK_MFG_QC_CODE_ORG` | `code, orgId` |
| ErpMfgJobCard | `UK_MFG_JOB_CARD_CODE_ORG` | `code, orgId` |

质量：

| 实体 | 唯一键名 | 列 |
|------|---------|---|
| ErpQaInspection | `UK_QA_INSPECTION_CODE_ORG` | `code, orgId` |
| ErpQaCheckList | `UK_QA_CHECKLIST_CODE_ORG` | `code, orgId` |
| ErpQaNcr | `UK_QA_NCR_CODE_ORG` | `code, orgId` |
| ErpQaSample | `UK_QA_SAMPLE_CODE_ORG` | `code, orgId` |
| ErpQaStandard | `UK_QA_STANDARD_CODE_ORG` | `code, orgId` |
| ErpQaInstrument | `UK_QA_INSTRUMENT_CODE_ORG` | `code, orgId` |
| ErpQaRecall | `UK_QA_RECALL_CODE_ORG` | `code, orgId` |
| ErpQaTestReport | `UK_QA_TEST_REPORT_CODE_ORG` | `code, orgId` |

- [ ] Add: manufacturing 11 个 `<unique-key>`
- [ ] Add: quality 8 个 `<unique-key>`
      - Skill: none

Exit Criteria:
- [ ] 生产和质量两域全部含 `code` 字段的实体已添加 `<unique-key>`；实体清单已与各域实际 ORM 核对一致

### Phase 6 — 剩余域（HR / CRM / CS / DRP / APS / B2B / Contract / Logistics / Projects / Maintenance）

Status: planned
Targets: 10 个剩余域
Skill: none

各域实体清单不在此罗列完全，遵循以下策略：
- **有 `code` 字段（编码/单号）的实体** → `(code, orgId)` 唯一键
- **`serialNo` 字段** → `(serialNo)` 全局唯一（单品追踪）
- **已有 unique-key 的实体** → 检查是否需要补充（b2b `UK_EDI_DOC_FORMAT_BILL` 保留，如有 `code` 字段实体需要新增）
- **枚举/字典类实体**（如 `ErpDrpDistCenter.code`）→ `(code, orgId)`

- [ ] Add: 10 个剩余域的全部 `<unique-key>`
      - 预计每个域 3-17 个 `<unique-key>`，共计约 80-100 个
      - Skill: none

Exit Criteria:
- [ ] 全部 18 域的 ORM 模型已审查完毕，含 `code`/`serialNo` 字段的实体无遗漏；各域实际添加清单与该域 `app-erp-<domain>.orm.xml` 完全一致（每域保留核对记录）

### Phase 7 — 验证 & codegen 测试

Status: planned
Targets: 生成 DDL 验证唯一约束正确性
Skill: `nop-testing`（如需）

- [ ] Proof: 运行 `nop-cli codegen` 为已修改 ORM XML 的域生成 DDL，验证 DDL 中包含 `UNIQUE KEY`
      - 至少选取 3 个域（master-data、purchase、finance）做 codegen 验证
      - Skill: none
- [ ] Proof: 检查生成的 DDL 中唯一键名与 ORM XML 中 `name` 属性一致
      - Skill: none
- [ ] Proof: 实体清单核对 — 对每个域，从实际 `module-<domain>/model/app-erp-<domain>.orm.xml` 枚举所有含 `code`/`serialNo` 字段的实体，与该域已添加的 `<unique-key>` 集合双向比对，确认无遗漏、无幽灵实体（如草案中的 `ErpMdBook`/`ErpPurCheck` 等）。核对清单保留为 Closure 证据。
      - Skill: none

Exit Criteria:
- [ ] codegen 生成的 DDL 包含正确的 UNIQUE KEY 定义
- [ ] 至少 3 个域的 DDL 人工审查通过
- [ ] 18 域实体清单双向核对完成，无幽灵实体、无遗漏

## Draft Review Record

- Independent draft review iteration 1: needs revision → fixed → accept (2026-07-05, 独立审查会话)。发现并修复的 Blocker/Major：
  1. **实体表未基于实时基线（违反指南规则 1）。** Phase 2–5 的逐实体表与实际 ORM 不符。抽查证实：master-data 的 `ErpMdCategory`（实为 `ErpMdMaterialCategory`）、`ErpMdUnit`（实为 `ErpMdUoM`）、`ErpMdConfig`（实为 `ErpSysConfig`，无 `code` 字段，已由 `uk_config_org` 覆盖）、`ErpMdBook`/`ErpMdCarrier`/`ErpMdCarrierAccount`（全仓不存在）；purchase 的 `ErpPurCheck`/`ErpPurDelivery`（不存在）、漏列 `ErpPurQuotation`/`ErpPurRfq`/`ErpPurSupplierPriceList`。
  2. **修复**：移除错误的 `ErpMdConfig` 行；在 `## Execution Plan` 顶部加权威核对说明并列举偏差；将 Phase 2/3/5/6 退出标准改为"含 `code`/`serialNo` 字段实体清单已与实际 ORM 双向核对"；Phase 7 增加实体清单双向核对 Proof；Closure Gates 增加"实体清单已与各域实际 ORM 双向核对一致"门控。
  - 未抽查域同样适用核对要求，不得直接信任草案表。
  - Minor（保留，不阻塞）：阶段未显式写 `Item Types:`/`Prereqs:` 行（类型已在每个项目内联标注）；Phase 7 codegen 仅抽 3 域（机械同构变更的可接受采样）；Phase 1 退出标准预置 `[x]`（决策已固化于本计划）。
- 结论：结构合规（必需章节、Phase 结构、Closure Gates、Deferred But Adjudicated 齐全；类型已按规则 7 标注；技能按规则 8 记录；Decision 按规则 9 附理由）。Major 已修复，可转 active。

## Closure Gates

- [ ] 范围内行为完成：18 域 ORM 模型 `<unique-key>` 全部添加
- [ ] 实体清单已与各域实际 `app-erp-<domain>.orm.xml` 双向核对一致（草案表偏差已修正，无幽灵实体、无遗漏）
- [ ] 相关文档对齐：`docs/architecture/idempotency-pattern.md` 已反映唯一键约束规则
- [ ] 已运行验证：codegen DDL 验证通过
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 非 `code` 字段的业务索引

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 高频查询路径索引（orgId、delFlag、docStatus 等）和唯一键补充是正交问题。唯一键补充是"数据正确性"约束，业务索引是"查询性能"优化，分开处理。计划范围不包含索引。
- Successor Required: yes — 归独立"数据库索引设计"计划

### 已存在数据的唯一约束冲突处理

- Classification: `watch-only residual`
- Why Not Blocking Closure: bootstrap 阶段数据量小，实际无重复数据。`nop-ddl` 生成的 DDL 如有冲突会在启动时报错，届时手动 SQL 清理即可。不阻塞 ORM 模型变更。
- Successor Required: no

## Closure

(待结束审计填充)
