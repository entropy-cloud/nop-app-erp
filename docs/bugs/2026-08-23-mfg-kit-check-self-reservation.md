# 2026-08-23 mfg 齐套校验未计回工单自身预留（approve 预留 × checkAvailability 自斥）

## 现象

`erp-mfg.reservation-enabled`（默认 true，UC-MFG-05 审核触发物料预留，落地 2026-08-16 287da3084）开启时，工单审核后 `checkAvailability` 对**完全备料**的工单误报 `STOCK_PARTIAL`：

- 备料 30 / BOM 需求 20 → approve 创建预留（RESERVED_QUANTITY=20，AVAILABLE_QUANTITY=30−20=10）→ checkAvailability 以 AVAILABLE_QUANTITY=10 对比需求 20 → 10 < 20 → **STOCK_PARTIAL**。
- 仅当库存 ≥ 2×需求时（预留后剩余 ≥ 需求）才返回 STOCK_RESERVED——语义错误。

## 根因

`KitAvailabilityChecker.loadAvailableByMaterial`（module-manufacturing/.../workorder/KitAvailabilityChecker.java:155）按 `Σ availableQuantity` 汇总余额，**未加回该工单自身的 ErpInvReservation 预留**。审核预留与本工单齐套校验同源同量——自身预留被计入「他人占用」。

时间线：预留功能 08-16 落地即引入该交互；08-11 E2E 基线在功能落地前（且经 2026-08-23 复核疑为 stale-server 口径，见 plan 2026-08-23-0434-2 裁决），未暴露。

## 影响

- reservation-enabled=true 下所有走 approve→checkAvailability 的工单链路（mfg-chain ×2 / mfg-genealogy / mfg-inspection-gate ×2 / mfg-variance / mfg-variance-recompute-reversal 共 7 E2E 用例）确定性 STOCK_PARTIAL。
- E2E 运行口径已以 `-Derp-mfg.reservation-enabled=false` config-gate 关闭该功能恢复链路语义（playwright.config.ts webServer.command + `_tmp-server.sh` 同步；SoD config-gate 同范式）；**预留功能本身由 JUnit 覆盖（TestErpInvReservation* / TestErpMfgWorkOrder* 全绿）**。

## 修复建议（successor）

`KitAvailabilityChecker` 汇总可用量时加回 `workOrderId = 本工单` 的 OPEN 预留（`ErpInvReservation` 按 (workOrderId, status=OPEN) 聚合 reservedQuantity 合并进 availableByMaterial），随后可移除 E2E 的 config-gate 关闭。

## 复现

fresh-DB + enforcement 栈（不带 `-Derp-mfg.reservation-enabled=false`）：`npx playwright test tests/e2e/orchestration/mfg-chain.spec.ts --workers=1` → `after checkAvailability docStatus=STOCK_RESERVED` 断言收到 STOCK_PARTIAL。

## 状态

open（登记日 2026-08-23，plan 2026-08-23-0434-2 Phase 2/3 裁决落盘；successor = checker 计回自身预留 + 恢复 E2E 默认口径）
