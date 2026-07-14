> Audit Status: closed
> Audit Type: multi-dimensional
> Mission: erp
> Remediation Plan: plans/2026-07-14-0930-1-1518-3-audit-revisions.md

# 多维审计：plan 2026-07-13-1518-3 基础域剩余 FK 名称解析

**审计日期**：2026-07-14
**审计类型**：多维审计（同时跨多个维度挑战工作）
**审计对象**：`docs/plans/2026-07-13-1518-3-foundational-remaining-fk-name-resolution.md`（状态 active，尚未实施，已过 3 轮草案审查）
**审计方法**：独立子代理（新会话，无执行者上下文）对 6 域 ORM 源模型、生成网格、前序计划、owner docs 和验证基线进行实时仓库核实
**审计技能**：`docs/skills/multi-dimensional-audit-prompt.md`（已注入项目定制化层：保护区域、验证命令、命名约定、已知失败模式）
**仓库快照**：`HEAD = b40aaacb`

---

## 执行摘要

**裁决：passes multi-dimensional audit**

0 Blocker | 0 Major | 2 Minor（advisory，非阻塞）

计划经 3 轮草案审查后收敛。本审计对 6 域 ORM 源模型逐一核实 FK 列名（第一跳），确认全部正确。ext:relation 缺口（8 个）全部经 to-one 全量扫描确认。实体计数算术确认。保护区域无违规（零 ORM 变更）。

---

## 维度 1：需求正确性 — 通过

- **FK 列名核实（P0 清零）**：6 域关键实体 FK 列经 ORM 逐一核实全部正确：
  - `ErpMdMaterial`：categoryId/uoMId/defaultWarehouseId/defaultTaxRateId
  - `ErpLogShipment`：orgId/carrierId/carrierConfigId/freightCurrencyId/shipperId（sender/receiver 确为文本字段不存在）
  - `ErpCtContract`：orgId/partnerId/currencyId/templateId/parentContractId
  - `ErpB2bAsn`：orgId/sourceEdiDocId/partnerId
  - `ErpDrpLine`：planId/materialId/warehouseId/sourceWarehouseId/orgId
  - `ErpApsOperationOrder`：workOrderId/machineId/orgId
- **ext:relation 缺口（8 个）全部确认**：APS 6 实体仅 `org` 有 to-one；`ErpApsDispatchLog` 有 `org`+`operationOrder` to-one；`ErpMdSettlementMethod.defaultFundAccountId` 无 to-one
- **实体计数算术确认**：72 = 66（机制 D）+ 5（无 FK）+ 1（ext:relation 缺口）
- **DRP 跨模块实体归属确认**：`ErpInvDrp*` 4 实体物理定义在 `module-drp/model/app-erp-drp.orm.xml`

## 维度 2：owner-doc 对齐 — 通过

- Owner docs 引用正确：`view-and-page-strategy.md`、`cross-module-entity-reference.md`、`add-bizloader-field.md`
- 机制 D 范式与前序 14 批次完全一致
- 保护区域声明正确：零 ORM/契约变更

## 维度 3：架构/边界影响 — 通过

- 跨域引用合法，均有 ext:relation 或弱引用声明
- 无新模块依赖引入
- 无 API 契约变更

## 维度 4：验证充分性 — 通过

- Closure Gates 完备：`mvn clean install -DskipTests` + 6 域 service `mvn test` + 66 view.xml `xmllint`
- Phase 6 测试抽样策略合理

## 维度 5：回归风险 — 通过（含 2 Minor）

### MINOR-1：Goals 段显示列映射 prose-level 不准确

Goals（行 151）将 ~15 个目标显示列写成 `.name`，实际部分目标仅有 `.code`。经实时 ORM 核实：`ErpDrpPlan` 和 `ErpInvDrpCrossDock` 仅有 `code` 列无 `name` 列。

**风险评估**：不阻塞——各 Phase Decision 步骤明确要求"经 ORM 核实列名"，前序批次实现均遵循"维度型读 `.name`、父单型读 `.code`"约定自动修正。

### MINOR-2：前序批次防御模式未在计划提及

前序 CRM/CS 批次（1518-1）发现 `orm_attached()` 守卫模式，manufacturing 批次发现 `safeBatchLoad` try-catch 降级模式。计划未显式提及。

**风险评估**：不阻塞——已在 `docs/logs/2026/07-13.md` 详细记录，实现代理读取日志后应自动应用。

## 维度 6：路由和技能选择 — 通过

- Task Route 类型 `app-layer design change` 正确
- 技能选择正确：`nop-frontend-dev` + `nop-backend-dev` + `nop-testing`

## 维度 7：待办/自主权策略漂移 — 通过

- Successor 链完整：14 前序批次 + 1518-1 + 1518-2 → 本计划为全域收官
- Deferred 项裁决合理
- 独立草案审查 3 轮收敛
- 无保护区域违规

---

## 已核实为正确（无发现）

- **FK 列名**：6 域关键实体 FK 列经 ORM 逐一核实全部正确
- **ext:relation 缺口**：8 个全部确认（APS 7 + MD 1）
- **维度型主数据目标**（Organization/Partner/Material/Currency/Employee 等）均有 `name`
- **人员引用**（manager/shipper）均 → ErpMdEmployee（有 `name`）
- **实体计数算术**：19+7+14+13+7+6=66

## 核实方法（可复现）

```
对每个域 orm.xml：
  1. 提取每实体 FK 列名，与计划 Phase 表交叉核对
  2. 提取 to-one 关系，核对 ext:relation 缺口
  3. 验证实体计数算术
  4. 核对生成网格为空 <grid id="list"/>
  5. 核对兄弟计划状态（1518-1/1518-2 completed）
```
