# 2026-09-07 i18n 运行时字符串批改的跨模块 `_cases` 快照清扫遗漏

## 分类

- 类别：回归预防 / 批量重构执行协议缺口
- 引入：plan `2026-09-07-0902-1`（MI.6 批1，fin/ast/cs CAT-3 运行时字符串英文化，commits `5ffcfbb64`/`606aeb5fe`/`61901fd6a`）
- 发现：plan `2026-09-07-0902-2`（MI.7）Phase 1 例行 `mvn test` 全 reactor 验证
- 修复：plan `2026-09-07-0902-2` 执行会话（38 `_cases` 快照文件 + 1 测试断言，逐处 code 实锚验证后外科更新）

## 问题

`mvn test` 全 reactor 在 `app-erp-projects-service` 报 2 errors：autotest 快照比对 `erp_ast_asset_action_log.SUMMARY` 期望 `资产创建`、实际 `Asset created`。续跑又暴露 purchase 22 errors（voucher MEMO/budget REASON）、sales 4 errors、app-erp-all 集成测试 7 处（fin MEMO/cs CONTENT/ast SUMMARY/remark + 1 Java 断言 `contains("人工复核")`）。共性：全部是**跨域消费方快照仍录制着被英文化前的中文运行时字面量**。

## 根本原因

MI.6 批1 的快照外科变换协议本身正确（只改消息/名称列、保留通配符），但**作用域按「字面量所属域」划界**（fin/ast/cs 各自模块的 `_cases`），而运行时字符串会跨域持久化：

- fin 过账引擎写的 voucher MEMO 出现在 purchase/sales 的 `_cases`（采购/销售触发承付、预算、汇兑凭证）；
- ast 实体动作日志 SUMMARY 出现在 projects（CIP 转固/工时结算）与 app-erp-all（`TestErpC12`）；
- cs 工单动作 CONTENT/remark 出现在 app-erp-all（`TestErpC04/C16`）；
- app-erp-all 测试代码还有硬编码中文子串断言（`errorMsg.contains("人工复核")`），随运行时消息英文化而恒假。

模块级 `mvn test -am`（仅批域 3 模块聚合）无法暴露这些跨域失败，导致批1 在「1047 tests 全绿」的局部证据下带伤收官。

## 修复

- 逐处失败以 surefire `field-value-not-expected`（field/actual/expected/tableName）为唯一依据，先 grep 生产代码确认新字面量实锚（如 `CommitmentVoucherGenerator.java:170`、`ErpCsTicketBizModel.java:256/284`、`ErpFinBadDebtProcessor.java:208/240`），再外科更新对应快照单元格；
- 严格区分三类中文：运行时字面量（改）、字典 label（`docType_label`/`status_label`，不改）、测试输入回显（subject/reason/remark 等 json5 输入值，不改）；
- 测试断言随语义锚点更新（`人工复核` → `manual review`）。

## 预防

1. **运行时字符串批改的验证门 = 全 reactor `mvn test`**，不得以批域 `-am` 聚合替代；持久化到共享表（voucher_line/ticket_action/action_log/notification）的字面量一律视为跨域契约面。
2. 批改前先枚举字面量的**全部消费面**：`grep -rl` 旧字面量于所有模块 `_cases`（含 app-erp-all）+ `src/test/java` 断言，而非只查本域。
3. 快照期望值更新必须逐一锚定生产代码新字面量（禁止无锚批量替换），并以「输入回显/字典 label 不动」为负面清单。
