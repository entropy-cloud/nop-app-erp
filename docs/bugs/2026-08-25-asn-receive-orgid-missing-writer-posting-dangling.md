# 2026-08-25 ASN→收货链缺 orgId writer → 过账账集解析 null → posted 永久悬挂（P1 业财过账悬挂）

## 现象

B2B ASN 自动收货链（`ErpB2bAsn__createReceiveFromAsn`）创建的采购收货草稿 `receive.orgId = null`：

- 收货 approve 内建过账时账集解析为 null → PURCHASE_INPUT **零凭证** → `posted` 回写后**永久悬挂 false**（已知失败模式 #11 家族）。
- 级联：`ReceiveStockMoveBuilder.java:33` 透传 null → 入库移动/库存余额/台账 orgId=null。
- C19 集成测试曾以 DAO fixture 补 orgId 后全绿 → 生产悬挂对黄金路径回归套件不可见（测试遮蔽，无 `workaround-for` 标注）。

## 根因

`ErpB2bAsnCreateReceiveFromAsnProcessor.createReceiveFromAsn`（module-b2b/erp-b2b-service）建 `ErpPurReceive` 头时写入 code/orderId/supplierId/warehouseId/currencyId/businessDate/docStatus/approveStatus/receiveStatus/remark——**零 orgId 写入**（全文件 `rg orgId` 零命中），而同链 PO 有 orgId 可透传。属「缺 writer」站点（区别于 R1.16/P1-MA2-074 清扫的 catch 吞异常站点）。

裁决（2026-08-25，plan 0330-3 Phase 1，双取向实证）**平台无 orgId 回填**：

- 生产路径：app-erp-all 真实 GraphQL 链（`ErpPurOrder__save`(orgId=2) → `ErpB2bAsn__save` → `matchPurchaseOrder` → `createReceiveFromAsn`）临时断言 `assertNull(draft.getOrgId())` 通过（1/0/0/0 BUILD SUCCESS）。
- 平台机制：ORM 列 `app-erp-purchase.orm.xml:693` orgId 无 defaultValue（对照 exchangeRate defaultValue=1）；nop-entropy nop-orm/nop-core/nop-service-framework 源码零 orgId 概念；`CrudBizModel.defaultPrepareSave` 仅状态机 initState；全仓 100+ 处派生实体显式 `setOrgId(src.getOrgId())` 透传惯例（如 `ReceiveStockMoveBuilder.java:33`、`ErpInvLandedCostProcessor.java:264`）——orgId 写入义务在应用 writer。
- 影响面：Processor 唯一入口 = GraphQL mutation → 全部触达上下文（交互 UI + webhook 编排汇于此）悬挂，无豁免。

## 证据链

- 源审计：`docs/audits/2026-08-24-2233-open-audit-integration-test.md` P1 发现 OA-03（含裁决义务与遮蔽定性）。
- 实仓锚点：`ErpB2bAsnCreateReceiveFromAsnProcessor.java:68-79`（建头无 orgId）、`module-purchase/model/app-erp-purchase.orm.xml:693`（无 defaultValue）、`ReceiveStockMoveBuilder.java:33`（级联透传）。
- 遮蔽实证：`TestErpC19B2bAsnAutoReceiveLandedCost` 原 :169-173 fixture 补 orgId="2"，注释如实记录生产缺口但无 workaround-for 标注。

## 修复

分支 B 落地（plan `docs/plans/2026-08-25-0330-3-adjudicate-integration-test-defect-routing.md` Phase 2）：Processor 补 orgId 透传 writer（`receive.setOrgId(po.getOrgId())`，对齐 supplierId/warehouseId 既有透传模式）；C19 去 fixture 遮蔽（改断言 `assertEquals("2", draft.getOrgId())`），version 级联快照重录（9: 2→1 / 10: 3→2 / CSV VERSION 3→2，通配纪律 `*` 不变）。module-b2b 测试 PO 种子 orgId 为空 → writer 写 null → 快照零变化。

## 状态

fixed（2026-08-25，plan 2026-08-25-0330-3；全仓 #11 家族「缺 writer」站点扫描归 successor——触发条件：下一轮 arm/审计 mission 或过账悬挂类 bug 再现）
