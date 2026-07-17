# 2026-07-17-1430 应收/应付票据过账辅助账 code 溢出 voucherCode 精度（latent）

## 问题

- 应收票据 `receive`/`endorse`（含 NOTES_RECEIVABLE_RECEIVED / NOTES_RECEIVABLE_ENDORSED 业务类型）过账时，`ErpFinArApItem` 插入抛 `nop.err.dao.sql.data-integrity-violation`（`sqlState=22001` 字符串右截断），过账吞异常 → `posted=false`。
- 影响范围：仅当票据 `code` 较长（>19 字符）时触发；生产票据码（如 `NR-2026-0001` ≈12 字符）不触发，故后端 JUnit（`TestErpFinNotesReceivablePosting`，短码）全绿未暴露。属**潜在稳健性缺陷**，非活跃生产 Bug。

## 复现

- 环境：webServer fresh-DB + seed，`ErpFinNotesReceivable__receive(notesId)` 经 `NotesPostingDispatcher.tryPostReceivable` → `FinPostingExecutor.postEvent` → `IErpFinVoucherBiz.post` → `ErpFinArApItemGenerator.generate`。
- 触发：建 `ErpFinNotesReceivable`，`code` ≥ 20 字符（如 `E2E-NR-1784263408547-1`，23 字符），partnerId 非空 → 调 `receive`。
- 最小观测：服务端日志 `票据过账失败，单据 ... posted=false：JdbcException[...sqlState=22001... insert into ERP_FIN_AR_AP_ITEM(...) ... vendorCode=22001]`；`note.posted=false`。

## 诊断方法

- 诊断难度：中。症状（`posted=false`）与「过账优雅降级」正常路径不可区分，须读服务端日志定位 `sqlState=22001`。
- 调查路径：首查服务端 `WARN 票据过账失败` 日志 → 抓到 `data-integrity-violation / sqlState=22001 / insert into ERP_FIN_AR_AP_ITEM` → 22001 = 字符串右截断（非 NOT NULL/FK）→ 比对插入列与 `ErpFinArApItem` ORM 列宽 → 锁定 `CODE` 列（domain `voucherCode` precision=50）。
- 被拒假设：①科目码缺失（已补 1121/2203/6603，且 discount/honor 同用 1121 通过 → 排除）；②partnerId FK（partnerId=1 是种子合法 partner，且 22001 非 FK 错误码 23503）。
- 决定性证据：`ErpFinArApItemGenerator.buildCode:192-195` 生成 `code = "ARI-" + sourceBillType + "-" + sourceBillCode + "-" + uuid8`；对 NOTES_RECEIVABLE：`"ARI-"(4) + "NOTES_RECEIVABLE"(17) + "-"(1) + sourceBillCode(23) + "-"(1) + uuid8(8) = 54` > 50 → 截断。

## 根本原因

- `ErpFinArApItemGenerator.buildCode`（`module-finance/erp-fin-service/.../posting/ErpFinArApItemGenerator.java:192-195`）拼接辅助账 `code` 时未约束总长，未参考目标列 `voucherCode` precision=50。
- 长业务类型名（`NOTES_RECEIVABLE`=17 / `NOTES_ENDORSED`=14 / `OWNERSHIP_TRANSFER`=18）+ 长源单据号组合时溢出。短业务类型（`AP_INVOICE`/`AR_INVOICE`=10）+ 短码不触发。
- `ArApItem.code` 列绑 `domain="voucherCode"`（`module-finance/model/app-erp-finance.orm.xml:617`，precision=50），无应用层长度校验。

## 修复

- **本计划（2026-07-17-1430-1）未改生产代码**（过账引擎为共享 Protected Area，生产代码修复须 ask-first）。
- E2E 绕过：`tests/e2e/business-actions/fin-notes-receivable.action.spec.ts` / `fin-notes-payable.action.spec.ts` 使用紧凑 base36 `note.code`（≤13 字符，对齐生产票据码实际长度），使生成 code ≤ 50。
- **已由 plan `2026-07-17-1600-1` 修复**（`docs/plans/2026-07-17-1600-1-ar-ap-item-code-length-guard.md`，Status: completed）——采用方案 1（`buildCode` 应用层长度守护），否决方案 2（放宽 `voucherCode` precision，侵入面跨 15 实体 + DDL 迁移 + ORM 保护区域）：
  - 新增常量 `protected static final int AR_AP_ITEM_CODE_MAX_LENGTH = 50;`（对齐 `voucherCode` precision，生成器为 DAG 顶不反向依赖 ORM 元数据）。
  - `buildCode` 短码路径（拼接结果 ≤ 50）逐字符不变；超限路径优先保留 `ARI- + sourceBillType + uuid8` 固定段（最坏 32 < 50，留 ≥18 给 sourceBillCode 压缩段），对 sourceBillCode 截取头部并追加 MD5 前 4 hex 摘要（保留全长指纹，全局唯一性由 uuid8 兜底）。方法签名 `protected String buildCode(String sourceBillType, String sourceBillCode)` 不变（向后兼容）。
  - 覆盖 `resolveProfile` 全分支（AP/AR_INVOICE/PAYMENT/RECEIPT/PUR_RETURN/SAL_RETURN/EXPENSE_CLAIM/EMPLOYEE_ADVANCE/NOTES_RECEIVABLE/NOTES_ENDORSED/OWNERSHIP_TRANSFER），任意 sourceBillType + 任意长度 sourceBillCode 均返回 ≤ 50。
  - E2E 紧凑码 workaround 说明仍保留（紧凑码本身合理，非缺陷，无需为验证修复而人为加长；修复正确性由后端 JUnit 长码回归测试保证）。

## 测试

- `tests/e2e/business-actions/fin-notes-receivable.action.spec.ts` - 短码绕过后 receive/endorse/writeOff 7 tests 全绿，凭证行精确数值断言覆盖 NOTES_RECEIVABLE_RECEIVED/ENDORSED 正路径 + writeOff REVERSAL 红冲（级别：e2e）。
- 后端 `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinNotesReceivablePosting.java` - 短码正路径已覆盖（级别：unit/integration），但**未覆盖长码溢出场景**（预防差距）。

## 受影响的工件

- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinArApItemGenerator.java:192-195` - `buildCode` 拼接未约束长度（缺陷源，本计划未改）。
- `module-finance/model/app-erp-finance.orm.xml:285,617` - `voucherCode` domain precision=50 / `ErpFinArApItem.code` 列绑该 domain。
- `tests/e2e/business-actions/fin-notes-receivable.action.spec.ts` - 紧凑码绕过。
- `tests/e2e/business-actions/fin-notes-payable.action.spec.ts` - 紧凑码绕过（NP ISSUED/HONORED 不生成辅助账故不触发，但统一紧凑码风格）。

## 未来重构注意事项

- 任何新增生成 `ErpFinArApItem` 的业务类型（长 `sourceBillType` 名）都可能在长单据号下复现 22001——`buildCode` 改造时须覆盖全部 `resolveProfile` 分支。
- 若放宽 `voucherCode` precision，须同步核对所有绑该 domain 的列（凭证号/单据号族）+ DB DDL 迁移。
- 票据/单据 `code` 长度策略若未来放宽（>19 字符），本缺陷将转活跃，须先修 `buildCode`。

## 预防差距

- 缺少「长单据号 + 长 businessType 名」组合的辅助账插入回归测试（现有 JUnit 均用短码）。
- `buildCode` 无应用层长度断言/截断，依赖 DB 列宽兜底但兜底表现为运行时截断异常而非可读校验。
- **已补回归测试**（plan `2026-07-17-1600-1` Phase 2）：`TestErpFinArApItemGeneration#testLongSourceBillCodeDoesNotOverflowVoucherCodePrecision` 驱动 `NOTES_RECEIVABLE_RECEIVED` + 28 字符 note.code 过账，断言过账成功（未吞 `22001`）+ 辅助账 1 条 OPEN + `code.length()<=50` + 幂等。经 stash 红绿反转证明：未应用修复时确实捕获 `sqlState=22001 / data-integrity-violation / insert into ERP_FIN_AR_AP_ITEM`，应用修复后全绿。
