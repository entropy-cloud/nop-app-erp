# 2026-07-14-0742-1-assets-posting-voucher-line-numeric-e2e assets 过账凭证行精确数值浏览器层 E2E 断言

> Plan Status: completed
> Mission: erp
> Work Item: assets-posting-voucher-line-numeric-e2e
> Last Reviewed: 2026-07-14
> Source: `docs/plans/2026-07-14-0215-1-assets-direct-action-e2e.md` Follow-up「assets 凭证行精确数值断言 successor（触发条件见 Deferred）」（Successor Required: yes，触发条件「assets 过账业务动作 E2E 落地后」——**已满足**：0215-1 已交付 4 spec 全部断言 posted=true + voucherId 非空，凭证行级科目码/金额为已建立范式的下一深层）
> Related: `docs/plans/2026-07-10-0704-1-voucher-line-numeric-assertion-e2e.md`（凭证行数值断言范式确立）、`docs/plans/2026-07-10-1800-1-inventory-move-ncr-scrap-voucher-line-e2e.md`、`docs/plans/2026-07-12-1321-2-finance-voucher-numeric-auto-recon-e2e.md`
> Audit: required

## Current Baseline

### 已实现

0215-1 交付 assets 域 4 个业务动作 spec（`tests/e2e/business-actions/ast-*.action.spec.ts`），全部经 GraphQL `@BizMutation` 驱动过账 happy-path 并断言 **posted=true + voucherId 非空**，但**均未断言凭证行级 subjectCode + debitAmount/creditAmount**：

| spec | 过账业务类型 | 已断言 | 凭证行断言 | AcctDocProvider 科目（默认） | billHeadCode 模式 |
|------|------------|--------|-----------|---------------------------|-------------------|
| `ast-depreciation` | DEPRECIATION | posted=true + voucherId | ❌ 缺 | Dr 6602 折旧费用 / Cr 1602 累计折旧 | `assetCode + "#" + period`（spec 已捕获 schedule.voucherId 可直用） |
| `ast-cip-capitalization` | CAPITALIZATION | posted=true | ❌ 缺 | Dr 1601 固定资产 / Cr 1603 在建工程（CIP 路径） | `cap.getCode()` |
| `ast-inventory-count` | ASSET_INVENTORY_ADJUSTMENT | posted=true | ❌ 缺 | SHORTAGE 路径 Dr 6711 营业外支出 / Cr 1601 固定资产 | `inventory.getCode()` |
| `ast-maintenance` | MAINTENANCE_EXPENSE + MAINTENANCE_CAPITALIZATION | posted=true | ❌ 缺 | EXPENSE Dr 6602 / Cr 1002；CAPITALIZE Dr 1601 / Cr 1002 | `maintenance.getCode()` |

### 凭证行数值断言范式（已确立）

`tests/e2e/orchestration/_helper.ts` 已提供两原语（0704-1 确立，1800-1/1321-2/0606-1/0606-2 复用）：

- `findVoucherIdByBillCode(page, billCode, postingType?: 'NORMAL'|'REVERSAL')` — 经 `ErpFinVoucherBillR.billCode` 反查凭证 id，按 postingType 区分原/红字凭证
- `assertVoucherLines(page, voucherId, expected: VoucherLineExpect[])` — 按 voucherId 查 `ErpFinVoucherLine` 逐行断言 subjectCode + dcDirection + debitAmount/creditAmount

assets 域过账经 `event.setBillHeadCode(...)` 写入 `voucher_bill_r`，故两原语可直接复用。depreciation spec 已捕获 `schedule.voucherId`，可绕过 billCode 反查直接断言。

### 种子 COA 现状（0215-1 补齐 + 既有）

`app-erp-all/.../_init-data/erp_md_subject.csv` 已含全部所需科目码：1601（0215-1 id=27）/1602（id=28）/1603（id=29）/6602（id=31）/6711（既有 id=15）/1002 银行存款（既有）。1002 经 ast-maintenance spec 已验证 posted=true 可达（Provider 读头字段解析科目码），无需追加。

### 剩余差距

assets 是当前唯一「过账 happy-path E2E 已落地（posted=true）但凭证行级数值断言缺失」的核心域。finance/inventory/manufacturing/quality/maintenance/purchase/sales 域凭证行断言均已覆盖（0704-1/1800-1/1321-2/0606-1/0606-2）。

## Goals

- 在 4 个既有 assets 业务动作 spec 内叠加凭证行精确数值断言（subjectCode + dcDirection + debitAmount/creditAmount），覆盖 5 个过账业务类型：DEPRECIATION / CAPITALIZATION / ASSET_INVENTORY_ADJUSTMENT(SHORTAGE) / MAINTENANCE_EXPENSE / MAINTENANCE_CAPITALIZATION
- depreciation reverse 红冲凭证行断言（同向取负，对齐 0704-1 红字范式）
- 复用 `findVoucherIdByBillCode` + `assertVoucherLines` 原语，零生产代码变更

## Non-Goals

- **新增 assets 业务动作 E2E**——0215-1 已交付 4 spec 全生命周期；本计划仅在其内叠加凭证行断言
- **DISPOSAL / VALUE_ADJUSTMENT / SPLIT / MERGE 过账凭证行断言**——0215-1 Non-Goal（useApproval DIRECT 审批轴/低价值），归后续 successor
- **资产卡片净值/折旧累计精确数值断言**——0215-1 已断言 accumulatedDepreciation/netBookValue 回写，本计划聚焦 GL 凭证行
- **生产代码/契约/ORM 模型变更**——纯测试断言叠加

## Task Route

- Type: `verification or audit work`（既有 E2E 叠加凭证行级数值断言，纯测试层）
- Owner Docs: `docs/design/assets/depreciation-and-posting.md`、`docs/design/assets/cip.md`、`docs/design/assets/inventory.md`、`docs/design/assets/maintenance.md`（过账科目对已在 owner doc 声明）
- Skill Selection Basis: 凭证行数值断言属测试层扩展，复用已确立 helper 原语，无新 BizModel/ORM 工作→`Skill: none`（既有 e2e 范式无需 nop-testing 全流程，helper 已就位）；NOP 平台文档无需读取（零平台交互）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（webServer JVM arg 已含 0215-1 assets 科目；本计划无新增 config）

## Execution Plan

### Phase 1 - 凭证行数值断言叠加（4 spec × 5 过账类型）

Status: completed
Targets: `tests/e2e/business-actions/ast-depreciation.action.spec.ts`、`ast-cip-capitalization.action.spec.ts`、`ast-inventory-count.action.spec.ts`、`ast-maintenance.action.spec.ts`
Skill: none

- Item Types: `Proof`
- Prereqs: none（0215-1 spec 已就位且 posted=true 绿）

- [x] `Proof`: `ast-depreciation` 叠加 DEPRECIATION 正向凭证行断言——经既有 `executed.voucherId`（spec 已捕获）调 `assertVoucherLines` 断言 Dr 6602 / Cr 1602，金额 = STRAIGHT_LINE 数学确定性派生（0215-1 已验 (originalValue−salvageValue)/usefulLifeMonths）。reverse 步骤叠加红冲凭证行断言（同向取负，经 `findVoucherIdByBillCode(assetCode+"#"+period, 'REVERSAL')`）
  - Skill: none
- [x] `Proof`: `ast-cip-capitalization` 叠加 CAPITALIZATION 凭证行断言——经 `findVoucherIdByBillCode(capCode, 'NORMAL')` 断言 Dr 1601 / Cr 1603，金额 = 资本化总额（self-contained setup 确定性派生，0215-1 已验资产原值）。reverseTransfer 步骤叠加红冲凭证行断言（REVERSAL Dr 1603 / Cr 1601 同向取负，经 `findVoucherIdByBillCode(capCode, 'REVERSAL')`）
  - Skill: none
- [x] `Proof`: `ast-inventory-count` 叠加 ASSET_INVENTORY_ADJUSTMENT 凭证行断言——SHORTAGE 路径经 `findVoucherIdByBillCode(inventoryCode, 'NORMAL')` 断言 Dr 6711 / Cr 1601，金额 = shortageAmount = bookValue（self-contained 测试资产 netBookValue，确定性 500 per 0215-1 spec 注释）。reverse 红冲同向取负
  - Skill: none
- [x] `Proof`: `ast-maintenance` 叠加双路径凭证行断言——(a) EXPENSE 路径 Dr 6602 / Cr 1002（金额 = totalCost，self-contained 维修费用合计确定性派生）；(b) CAPITALIZE 路径 Dr 1601 / Cr 1002（金额 = capitalizedAmount，0215-1 已验资产原值增量）。reverse 红冲同向取负
  - Skill: none

Exit Criteria:

> 仅证明凭证行断言叠加交付且全绿。完整仓库 build/test 属 Closure Gates。

- [x] 4 spec 凭证行断言全部通过（5 过账类型 + depreciation/cip-capitalization/maintenance/inventory reverse 红冲）
- [x] 零生产代码变更（仅 spec 文件 edit）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0a220ea41ffevN7BR593Dr8ySD) — 全部 load-bearing 主张经实时仓库核实为真（4 spec posted=true 无凭证行断言、helper 两原语签名、5 过账类型科目码、billHeadCode 模式、种子 COA 全在、depreciation voucherId 捕获、0215-1 successor）。**1 MAJOR (M1)**：Task Route owner doc 路径错误（`depreciation.md`/`capitalization.md` 不存在 → `depreciation-and-posting.md`/`cip.md`）。3 MINOR：m1 CIP reverse 凭证行断言遗漏；m2 maintenance 双路径金额混淆；m3 depreciation reverse lookup 不对称（可接受）。**已修订**：M1 owner doc 路径订正；m1 补 CIP reverse 红冲断言（REVERSAL Dr 1603/Cr 1601）+ Exit Criteria 同步；m2 拆分 EXPENSE(totalCost)/CAPITALIZE(capitalizedAmount) 金额。修订后无 Blocker/Major 残留 → 计划 execution-ready。

## Closure Gates

> 完整仓库验证在此处运行一次。

- [x] 范围内行为完成（4 spec × 5 过账类型凭证行断言全绿）
- [x] 相关文档对齐（`docs/testing/e2e-runbook.md` 套件计数 + assets 凭证行断言注记）
- [x] 已运行验证：`PLAYWRIGHT_PORT=8011 npx playwright test tests/e2e/business-actions/ast-*.action.spec.ts --workers=1` 全绿
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

无新增。DISPOSAL / VALUE_ADJUSTMENT / SPLIT / MERGE 凭证行断言为 0215-1 既有 Non-Goal successor（useApproval DIRECT 审批轴/低价值），非本计划范围。

## Closure

Status Note: completed — Phase 1 全部交付。4 个既有 assets 业务动作 spec（ast-depreciation / ast-cip-capitalization / ast-inventory-count / ast-maintenance）内叠加凭证行精确数值断言，覆盖 5 过账业务类型（DEPRECIATION / CAPITALIZATION / ASSET_INVENTORY_ADJUSTMENT-SHORTAGE / MAINTENANCE_EXPENSE / MAINTENANCE_CAPITALIZATION）+ 4 路径红冲同向取负断言。`PLAYWRIGHT_PORT=8011 npx playwright test tests/e2e/business-actions/ast-*.action.spec.ts --workers=1` 8 测试全绿。零生产代码变更（仅 4 spec 文件 edit + e2e-runbook 文档对齐）。assets 自此不再是「过账 happy-path E2E 已落地但凭证行级数值断言缺失」的核心域。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 closure audit（ses_0a2110a2cffeB2LX5fdeg0Ln0Q，新会话，执行者未自我审计）— **AUDIT VERDICT: PASS WITH NOTES**。8 维度全部核实通过（scope conformance 零生产代码、5 AcctDocProvider 断言 vs 源码逐一核对、红冲同向取负非借贷互换、helper 原语复用无重实现、4 billHeadCode vs dispatcher 源码核对、TS 语法有效、plan 一致性、DEPRECIATION 333.3333 派生 + toBe 精确匹配可行性）。零 Blocker；3 非 blocking notes（Closure 段未终态[本步骤已解决]、2 无关 dirty 文件非本计划足迹、cip reverse 注释措辞 nit 无代码影响）。

Follow-up:

- DISPOSAL / VALUE_ADJUSTMENT / SPLIT / MERGE 过账凭证行断言 successor（0215-1 既有 Deferred，触发条件：assets 审批轴/拆合 E2E 深化时）
