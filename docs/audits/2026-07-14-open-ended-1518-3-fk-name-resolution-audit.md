> Audit Status: closed
> Audit Type: open-ended
> Mission: erp
> Remediation Plan: plans/2026-07-14-0930-1-1518-3-audit-revisions.md

# 开放式审计：plan 2026-07-13-1518-3 基础域剩余 FK 名称解析（目标显示列存在性核实）

**审计日期**：2026-07-14
**审计类型**：开放式审计（搜索标准检查清单之外的隐藏问题）
**审计对象**：`docs/plans/2026-07-13-1518-3-foundational-remaining-fk-name-resolution.md`（状态 active，尚未实施，已过 3 轮草案审查）
**审计方法**：对抗性核实计划声明 vs 实时 ORM——(1) 全量扫描 6 域 to-one 关系核对 ext:relation 缺口完整性；(2) 核对目标实体显示列（`name`/`code`/其他）的存在性，即机制 D 的"第二跳"
**审计技能**：`docs/skills/open-ended-audit-prompt.md`（已注入项目定制化层：保护区域、已知失败模式）
**仓库快照**：`HEAD = 2927d5a0`

---

## 执行摘要

计划已完成 3 轮草案审查，**全部聚焦于 FK 列名（leftProp / 第一跳）并已修正**大量系统性列名虚构。但**系统性遗漏了第二跳：目标实体显示列的存在性**。本审计通过全量 ORM 核实发现：

- **1 个阻塞项（P0）**：`configId@ErpB2bMftLog` 目标 `ErpB2bMftConfig` 无任何显示列，计划声称可解析且未纳入延后清单。
- **3 个待裁决项（P1）**：`contractLineId`/`contractVersionId`/`drpLineId` 目标仅有 Integer 行号/版本号，计划无裁决。
- **1 个隐藏假设（P2）**：`providerRequestId@ErpCtSignatureRequest`（VARCHAR 自引用 to-one）被静默忽略。
- **大规模叙事错误（P3）**：Goals 第 151 行将 ~15 个目标显示列写成 `.name`，实际只有 `.code` 或其他列。

这些发现全部属于"目标无可用显示列"类别——与前序 HR 批次反复踩中的模式（`目标无 code/name 保留 ID`）完全同构，但本计划的延后清单只覆盖 `ext:relation 缺口`，未覆盖此类。

**裁决：`needs revision`**

---

## P0 — Blocker

### B1. `configId@ErpB2bMftLog → ErpB2bMftConfig` 目标无任何显示列，但计划声称可解析且未列入延后

- 计划 Phase 4 表（行 104）：`ErpB2bMftLog | orgId, configId`
- 计划 Goals（行 151）：`config→configName 读 ErpB2bMftConfig.name`
- **实时 ORM 核实**：`ErpB2bMftConfig` 列集合 = `{id, orgId, partnerId, protocol, transportEndpoint, localAs2Id, remoteAs2Id, sftpUsername, sftpPort, ftpsPort, ...}`，**无 `name`、无 `code`、无 `configCode`、无任何标识性字符串列**（仅 protocol/transportEndpoint/localAs2Id/remoteAs2Id 等技术字段）。
- 后果：实现者写 `getConfig().getName()` 编译失败；机制 D 无法产生有意义显示值。
- 前序先例：HR 批次（`sourceSalaryId 目标 ErpHrSalary 无 code/name 保留 ID`）已建立"保留原始 ID"模式，但**本计划未将此 FK 纳入 Deferred**。
- 修复：将 `configId@ErpB2bMftLog` 加入 `Deferred But Adjudicated`（`out-of-scope improvement`，successor required），并订正 Phase 4 Decision 与 Goals。

---

## P1 — Major（待裁决缺失）

### M1. 3 个 FK 目标仅有 Integer 行号/版本号，无字符串显示列，计划未给裁决

| FK prop | 使用实体 | 目标实体 | 目标仅有候选列 | 计划声称（Goals） |
|---|---|---|---|---|
| `contractLineId` | ErpCtInvoicePlan / ErpCtConsumptionLine / ErpCtVolumeDiscount | ErpCtContractLine | `lineNo`(Int) / `description` | `读 ErpCtContractLine.name` |
| `contractVersionId` | ErpCtSignatureRequest | ErpCtContractVersion | `versionNo`(Int) | `读 ErpCtContractVersion.name` |
| `drpLineId` | ErpInvDrpCrossDock | ErpDrpLine | `lineNo`(Int) | `读 ErpDrpLine.name` |

- 经实时 ORM 核实，这 3 个目标实体**无 `name`/`code`**：
  - `ErpCtContractLine` 列 = `{id, contractId, lineNo, materialId, description, quantity, unitPrice, amount, ...}`
  - `ErpCtContractVersion` 列 = `{id, contractId, versionNo, versionDate, content, ...}`
  - `ErpDrpLine` 列 = `{id, planId, lineNo, materialId, warehouseId, sourceWarehouseId, ...}`
- 需裁决：要么用 `lineNo`/`versionNo`（Integer→String）作 fallback 显示，要么保留原始 ID。计划零 guidance。
- 与 P0 同类，应在 Phase 3/5 Decision 中逐一裁决并视情况加入 Deferred。

---

## P2 — Minor（隐藏假设）

### m1. `providerRequestId@ErpCtSignatureRequest`（VARCHAR 自引用 to-one）被静默忽略

- 实时 ORM：`providerRequestId` 为 VARCHAR(200) 列，且有 to-one `providerRequest → app.erp.contract.dao.entity.ErpCtSignatureRequest`（**自引用**）。
- 计划 Phase 3 表（行 84）只列 `orgId, contractVersionId`，完全未提此列，也未说明排除理由。
- 推断：计划按"VARCHAR 列非数值 FK"规则排除（与 `ownerId`/`approvedById` userId 列一致），但**未显式记录**。
- 建议：Phase 3 Decision 增一行明确"`providerRequestId` 为 VARCHAR 自引用，按规则排除"，避免未来"为何此列显示不透明字符串"的疑问。

---

## P3 — 叙事错误（规模化，非阻塞但削弱审查可信度）

### n1. Goals（行 151）系统性将目标显示列写成 `.name`，实际 ~15 个目标只有 `.code` 或其他列

经实时 ORM 核实，下列目标**无 `name`**，计划"读 .name"叙事错误（实现时 `getName()` 编译失败，`getCode()`/`getXxx()` 才正确）：

| 目标实体 | 实际显示列 | 计划声称 | 所在 ORM |
|---|---|---|---|
| ErpLogCarrier | `code` | `.name` | logistics |
| ErpLogShipment | `code` | `.name` | logistics |
| ErpB2bEdiFormat | `code` | `.name` | b2b |
| ErpB2bEdiDoc | `code` | `.name` | b2b |
| ErpB2bAsn | `code` | `.name` | b2b |
| ErpB2bPartnerProfile | `code` | `.name` | b2b |
| ErpCtContract | `code` | `.name` | contract |
| ErpCtApprovalMatrix | `code` | `.name` | contract |
| ErpCtRebateAgreement | `code` | `.name` | contract |
| ErpDrpPlan | `code` | `.name` | drp |
| ErpApsOperationOrder | `code` | `.name` | aps |
| ErpInvStockMove | `code` | `.name` | inventory（跨域） |
| ErpInvDrpCrossDock | `code` | `.name` | **drp**（非 inventory，ErpInv* 前缀但物理位于 drp orm） |
| ErpLogCarrierConfig | `configCode` | `.name` | logistics |
| ErpB2bMftCertificate | `certName` | `.name` | b2b |

- **为何非阻塞**：HR 批次已有先例（`ErpPrjTask.name → 实为 title，执行时裁决 getTitle()`），实现时编译器会暴露，机制 D 仍可工作（派生 prop 名 `{relation}Name` 与取值 getter 解耦）。
- **为何仍需修订**：(1) 规模远超 HR 的 1 例（本计划 ~15 例），表明 Goals 映射**未对照目标 schema 核实**；(2) 第 3 轮审查记录称"FK 列经 ORM 逐一核实正确"，但只核了第一跳（leftProp），未核第二跳（显示列），此结论需补限定说明，否则误导后续批次信赖。

---

## 已核实为正确（无发现）

- **8 个 ext:relation 缺口全部准确**：脚本全量扫描 6 域 to-one，确认 `defaultFundAccountId@SettlementMethod` + APS 7 个（`workOrderId`/`machineId`×3/`operationId`/`workcenterId`×2）确实无 to-one；且**其余所有声称可解析的 FK 均有 ext:relation**（机制 D `batchLoadProps(rows, singleton("<relation>"))` 前提成立）。
- **FK 列名**：抽样 `ErpMdMaterial`（categoryId/uoMId/defaultWarehouseId/defaultTaxRateId）、`ErpLogShipment`（5 FK）、`ErpDrpLine`（5 FK）等均与 ORM 一致——iteration-2 修正后列名可靠。
- **维度型主数据目标**（ErpMdOrganization/Partner/Material/Currency/Employee/Warehouse/Location/MaterialCategory/UoM/TaxRate/Subject/AcctSchema）均有 `name`，计划 `.name` 正确。
- **人员引用**（`manager`@Warehouse/CostCenter、`shipper`@Shipment）均 → ErpMdEmployee（有 `name`），正确。
- **实体计数算术**：19(MD)+7(logistics)+14(contract)+13(b2b)+7(drp)+6(aps)=66，内部一致。

---

## 修订要求（解除 `needs revision` 的最小集）

1. **P0**：`configId@ErpB2bMftLog` 加入 `Deferred But Adjudicated`（保留原始 ID），Phase 4 Decision + Goals 订正。
2. **P1**：Phase 3/5 Decision 为 `contractLineId`/`contractVersionId`/`drpLineId` 给出明确裁决（fallback 列 或 纳入 Deferred），并相应修订 Goals。
3. **P2**：Phase 3 Decision 增 `providerRequestId` 排除说明。
4. **P3**：Goals 行 151 的"读 .name"按上表批量订正为实际 getter（`getCode`/`getConfigCode`/`getCertName`），或在 Goals 顶部加注"显示 getter 以实现时 ORM 实际列为准，下文 .name 为占位"。
5. 第 3 轮 Draft Review Record 补一句：FK **列名**已核、**目标显示列存在性**未核（本审计补核结果如上）。

---

## 核实方法（可复现）

```
对每个域 orm.xml：
  1. 提取每实体 BIGINT/long 列（FK 候选）+ to-one 关系（含 leftProp）
  2. FK 列无对应 to-one → ext:relation 缺口候选
  3. 对每个 to-one 目标实体，检查是否含 name/code/其他标识列 → 显示列存在性
交叉对比计划 Phase 表 + Goals 映射
```

完整脚本逻辑见本审计执行过程（python 正则提取 entity→columns→to-one）。
