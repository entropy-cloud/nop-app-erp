# 2026-07-17-1600-1-ar-ap-item-code-length-guard 应收应付辅助账 code 长度守护修复

> Plan Status: completed
> Last Reviewed: 2026-07-17
> Mission: erp
> Work Item: finance 共享过账基础设施 latent defect 修复（ErpFinArApItemGenerator.buildCode 超 voucherCode precision 50）
> Source: `docs/bugs/2026-07-17-1430-ar-ap-item-code-overflows-vouchercode-for-long-notes-codes.md` + `docs/plans/2026-07-17-1430-1-treasury-notes-direct-action-voucher-line-e2e.md` Deferred/Follow-up「`ErpFinArApItemGenerator.buildCode` 长度守护改造」
> Related: `2026-07-17-1430-1`（E2E 发现 latent defect，紧凑码绕过）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-17）：

### 缺陷现状（latent，已复现）

- `ErpFinArApItemGenerator.buildCode`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinArApItemGenerator.java:192-195`）生成辅助账 `code` 时**无总长约束**：

  ```java
  return "ARI-" + sourceBillType + "-" + (sourceBillCode != null ? sourceBillCode : "")
          + "-" + StringHelper.generateUUID().substring(0, 8);
  ```

- 该 `code` 写入 `ErpFinArApItem.code` 列，该列绑定 `domain="voucherCode"`（`module-finance/model/app-erp-finance.orm.xml:285`，**precision=50**，`tagSet="var"`）。
- 拼接结果长度 = `4("ARI-") + len(sourceBillType) + 1("-") + len(sourceBillCode) + 1("-") + 8(uuid8)`。当 `sourceBillType` 为长名（`NOTES_RECEIVABLE`=17 / `OWNERSHIP_TRANSFER`=18 / `NOTES_ENDORSED`=14）且 `sourceBillCode` 较长（>≈19 字符）时，总和超过 50 → H2/生产 DB `sqlState=22001` 字符串右截断 → 过账吞异常（`FinPostingExecutor` 优雅降级）→ `posted=false`。
- **复现证据**（plan 1430-1 执行期捕获）：E2E `ErpFinNotesReceivable__receive` 用长 note.code（23 字符 `E2E-NR-1784263408547-1`）触发 → 服务端日志 `data-integrity-violation / sqlState=22001 / insert into ERP_FIN_AR_AP_ITEM` → `note.posted=false`。**决定性证据**：`"ARI-"(4) + "NOTES_RECEIVABLE"(17) + "-"(1) + 23 + "-"(1) + uuid8(8) = 54 > 50`。

### 受影响业务类型（resolveProfile 全分支覆盖核实）

`resolveProfile` 返回非 null 的业务类型经核实均经 `buildCode`（:85），长 sourceBillType 名的分支为主要风险面：

| sourceBillType 常量 | 长度 | 业务类型 |
| --- | --- | --- |
| SOURCE_BILL_NOTES_RECEIVABLE | 17 | NOTES_RECEIVABLE_RECEIVED |
| SOURCE_BILL_NOTES_ENDORSED | 14 | NOTES_RECEIVABLE_ENDORSED |
| SOURCE_BILL_OWNERSHIP_TRANSFER | 18 | OWNERSHIP_TRANSFER |
| SOURCE_BILL_EMPLOYEE_ADVANCE | 16 | EMPLOYEE_ADVANCE（中名） |
| SOURCE_BILL_PUR_RETURN / SAL_RETURN / EXPENSE_CLAIM / AP_INVOICE / AR_INVOICE / PAYMENT / RECEIPT | ≤14 | 短名，需更长 sourceBillCode 才触发（最坏固定段 OWNERSHIP_TRANSFER=18 仍主导预算计算） |

### 现有测试覆盖（预防差距）

- `TestErpFinArApItemGeneration`（328 行，5 测试方法）全部使用短 sourceBillCode（如 `AP-ARAP-001` ≈11 字符），**从未覆盖长码 + 长 sourceBillType 组合**。
- `TestErpFinNotesReceivablePosting` 同样使用短码，未覆盖长码溢出场景。
- 即缺陷在生产短码（如 `NR-2026-0001` ≈12 字符）下不活跃，但属**共享过账基础设施稳健性缺陷**：任何放宽单据码长度策略的新需求都会令其转活跃，且缺陷表现（`posted=false`）与「过账优雅降级」正常路径不可区分（须读服务端日志才能定位 `22001`）。

### 修复方案空间（待 Decision 裁决）

1. **`buildCode` 应用层长度守护**（本计划首选）：保持 `prefix + sourceBillType + uuid8` 不截断，仅对 `sourceBillCode` 段超限时截断 + 哈希摘要保唯一。改动面 = 1 个 protected 方法 + 1 个常量（precision 上限）。不改 ORM/DDL/契约。
2. 放宽 `voucherCode` domain precision（**否决方向**）：`voucherCode` precision=50 被 **15 个实体的 `code` 列共用**（经核实 `app-erp-finance.orm.xml` 15 处 column 绑定：voucher/template/period/ar_ap_item/fund_account/reconciliation/... 各域单号），放宽须同步 DB DDL 迁移 + 全量核对绑该 domain 的列，侵入面过大且跨保护区域（ORM 模型）。

### 剩余差距

- `buildCode` 无长度约束；零长码回归测试；bug 文档 `docs/bugs/2026-07-17-1430-...` 标注「未改生产代码 / 显式 successor」待关闭。

## Goals

- 修复 `ErpFinArApItemGenerator.buildCode` 使生成 code 确定性 ≤ `voucherCode` precision（50），覆盖 `resolveProfile` 全分支，保证唯一性不退化为冲突。
- 补齐预防差距：新增「长 sourceBillCode + 长 sourceBillType」组合的辅助账插入回归测试，捕获 `22001` 回归。
- 关闭 `docs/bugs/2026-07-17-1430-...` 与 `2026-07-17-1430-1` Follow-up 该 successor。

## Non-Goals

- **不改 ORM 模型 / DDL / `voucherCode` domain precision**（方案 2 否决，侵入面跨保护区域且影响 ~16 实体）。
- **不改 E2E 紧凑码 workaround**：`fin-notes-receivable/payable.action.spec.ts` 当前用紧凑 base36 note.code（≤13 字符，对齐生产票据码实际长度）。紧凑码本身合理，无需为「验证修复」而人为加长——修复正确性由后端 JUnit 长码回归测试保证。E2E spec 保持现状。
- 不触及 `cancelOnReverse` / 红冲路径 / 其他过账生成器（`BudgetVoucherGenerator` / `CloseVoucherWriter` 等使用 `generateUUID` 但不拼接长业务类型名，非本缺陷面）。
- 不引入对 `code` 列的应用层 precision 常量到 ORM（仅在生成器内持有）。

## Task Route

- Type: `bug investigation`（确认根因已完成，转 `implementation-only change` Fix）
- Owner Docs: `docs/design/finance/posting.md`（业财过账三层模型）、`docs/design/finance/ar-ap-reconciliation.md`（辅助账语义）、`docs/bugs/2026-07-17-1430-...`（缺陷记录）
- Skill Selection Basis: 修改 finance service 层过账生成器（手写 Java，非生成代码）→ 加载 `nop-backend-dev`（决策门 / 跨实体访问自检 / 异常处理规范）+ `nop-debugging`（缺陷根因已定位，修复需防回归）。需阅读 `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`StringHelper` 工具）确认截断/哈希辅助。
- Protected Areas: finance 过账引擎为 ERP 保护区域；本计划**仅改 service 层 1 个 protected 方法 + 1 常量 + 测试**，不改 ORM/契约/xbiz。AI 自主权映射为 `plan-first`（对齐 `ai-autonomy-policy.md`：会计/财务过账 → plan-first）；本计划在 `plan-first` 基础上叠加**更严格的人工签收门控**——`active` 后实施前须人工确认生产代码改动未超出「`buildCode` 长度守护」最小范围（policy 允许向更严方向收紧）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。
- 验证：`mvn test -pl module-finance/erp-fin-service -am`（finance service 模块单测）+ `mvn clean install -DskipTests`（154 模块 closure gate）。

## Execution Plan

### Phase 1 - 修复方案 Decision + buildCode 长度守护实现

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinArApItemGenerator.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: none

- [x] `Decision`: 确认采用方案 1（`buildCode` 应用层长度守护），否决方案 2（放宽 `voucherCode` precision）。理由记录于计划：方案 2 侵入面跨 ~16 实体 + DDL 迁移 + ORM 保护区域；方案 1 改动面 = 1 protected 方法 + 1 常量，局部化于生成器，唯一性由既有 `uuid8` + 截断哈希摘要保证。
  - 截断策略裁决（须在实现前定）：当 `prefix("ARI-") + sourceBillType + "-" + sourceBillCode + "-" + uuid8` 超过上限时，**优先保留 `prefix + sourceBillType + uuid8` 段不截断**（sourceBillType 最长 18，prefix 4，分隔符 2，uuid8 8 → 固定段最坏 32 < 50，留 ≥18 给 sourceBillCode）；`sourceBillCode` 段超限时截断为 `remainingBudget`，并追加短哈希摘要（如 `StringHelper.hashCode` 取模 / MD5 前 4 hex）保证两条不同长码不退化为同一 code（避免唯一性冲突）。`remainingBudget = 50 - 固定段长度 - 哈希段长度 - 分隔符`。
  - 上限来源：新增 `protected static final int AR_AP_ITEM_CODE_MAX_LENGTH = 50;`（与 `voucherCode` precision 对齐，Javadoc 注明对齐源 `app-erp-finance.orm.xml` domain `voucherCode`）。不读 ORM domain 元数据（生成器为 DAG 顶，避免反向依赖 ORM 元数据 API）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 实现 `buildCode` 长度守护——短码（≤ 上限）路径行为完全不变（保持现有拼接）；仅超限路径进入截断 + 哈希分支。保持方法签名 `protected String buildCode(String sourceBillType, String sourceBillCode)` 不变（向后兼容，子类/测试可直接调用）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `buildCode` 对所有 `resolveProfile` 分支的 sourceBillType + 任意长度 sourceBillCode 均返回 ≤ 50 字符；短码输入行为与修复前逐字符一致（既有测试基线不破）。

---

### Phase 2 - 预防差距：长码 + 长 sourceBillType 回归测试

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinArApItemGeneration.java`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] `Add`: 在 `TestErpFinArApItemGeneration` 新增回归测试 `testLongSourceBillCodeDoesNotOverflowVoucherCodePrecision`——经 `voucherBiz.post(event, CTX)` 驱动一条长 sourceBillType + 长 sourceBillCode 的过账（如 `NOTES_RECEIVABLE_RECEIVED` + 25 字符 note.code），断言：(a) `voucherId` 非空（过账成功，未吞 `22001`）；(b) `ErpFinArApItem` 落库 1 条 + `status=OPEN`；(c) `item.getCode().length() <= 50`；(d) 幂等路径（同 sourceBillCode 再过账）不产生第二条且不抛异常。
  - setup 复用既有 `seedSubject`/`seedOpenPeriod`/模板范式；长码用确定性字面量（非随机），使断言稳定。
  - Skill: `nop-testing`
- [x] `Proof`: 验证回归测试在**未应用 Phase 1 修复**时确实失败（红）——临时 `git stash` Phase 1 改动跑该测试，确认抛/吞 `22001` 或 `code.length()>50` 断言失败；恢复 Phase 1 后转绿。此为「测试有效性」证明，记录于实施日志（不要求持久化 stash 验证脚本）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 新增长码回归测试在修复后全绿，且经 stash 验证能在未修复时捕获缺陷（红→绿反转证明）。
- [x] 既有 5 个测试方法零回归（短码路径行为不变）。

---

### Phase 3 - bug 文档关闭 + 日志

Status: completed
Targets: `docs/bugs/2026-07-17-1430-ar-ap-item-code-overflows-vouchercode-for-long-notes-codes.md`、`docs/plans/2026-07-17-1430-1-treasury-notes-direct-action-voucher-line-e2e.md`（Follow-up 标注 RELEASED）、`docs/logs/2026/07-17.md`
Skill: none

- Item Types: `Fix | Add`
- Prereqs: Phase 2

- [x] `Fix`: 在 `docs/bugs/2026-07-17-1430-...` `## 修复` 段追加「**已由 plan `2026-07-17-1600-1` 修复**」+ 实施摘要（方案 1 / 覆盖分支 / 回归测试）；`## 预防差距` 段标注已补回归测试。保留 E2E 紧凑码 workaround 说明（仍合理，非缺陷）。
  - Skill: none
- [x] `Fix`: 在 `2026-07-17-1430-1` Follow-up 该 successor 行追加 `**RELEASED by 2026-07-17-1600-1**`。
  - Skill: none
- [x] `Add`: 更新 `docs/logs/2026/07-17.md`（倒序聚合条目）：背景（latent defect 复现）/ Phase 1（方案裁决 + buildCode 守护）/ Phase 2（长码回归测试 + 红绿反转证明）/ Phase 3（bug 关闭）/ 验证状态。
  - Skill: none

Exit Criteria:

- [x] bug 文档与 1430-1 Follow-up 标注本计划释放；日志聚合条目就位。

## Draft Review Record

- Independent draft review iteration 1: **acceptable as-is**（独立 general 子代理 `ses_090ea1163ffequBsg7sgovEyES`，新会话冷重播无起草者上下文，2026-07-17）— 0 Blocker / 0 Major / 3 Minor。全部 load-bearing 事实主张经实时仓库逐项核实**零伪**：`buildCode:192-195` 无长度约束 ✓、`voucherCode` precision=50 @ orm.xml:285 + `ErpFinArApItem.code` 绑该 domain @ :620 ✓、溢出算术 54>50 ✓、`resolveProfile` 非空分支均经 :85 buildCode ✓、sourceBillType 常量长度 @ ErpFinConstants:77-88 ✓、既有 5 测试全用短码（最长 12）预防差距确认 ✓、bug 文档 + 1430-1 Follow-up successor 链确认 ✓、finance 过账保护区域 plan-first 映射 ✓。**Anti-scope-manufacturing 裁决：legitimately warranted**——区别于被取消的 1005-1（其「零覆盖」基线经核实为伪，0941-2 已交付 spec），本计划基线**经核为真**（buildCode 确无守护 + 列确 50 上限 + 算术事实 + 文档化 successor 链 + 零长码测试预防差距）。修复方案健全性已核：短码路径字节不变（既有 5 测试守门）+ uuid8 恒保留保唯一性 + 预算算术最坏固定段 32<50 留 ≥18。规则 R1-R14 + anti-slack 全 PASS。3 Minor 全部修订：M1（sourceBillType 长度表 `≤14`→拆出 EMPLOYEE_ADVANCE=16，最坏段仍 OWNERSHIP_TRANSFER=18 主导）/ M2（`~16 实体`→`15` 经核实 column 绑定计数）/ M3（自主权双重标签澄清为 `plan-first` + 叠加更严人工签收门控）。共识达成 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划触及 finance 保护区域生产代码（1 方法 + 1 常量），Closure Gates 须含完整 `mvn` 验证。

- [x] 范围内行为完成（`buildCode` 长度守护 + 长码回归测试 + bug/1430-1 释放标注）
- [x] 相关文档对齐（bug 文档 + 1430-1 Follow-up + 日志）
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` 全绿（含新增长码回归 + 既有 5 方法零回归）+ `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（草案期为空——本计划范围窄，仅 buildCode 长度守护 + 回归测试。其余 treasury successors 见 1430-1，各自独立触发条件，不在本计划范围。）

## Closure

Status Note: 执行完成（2026-07-17）。3 Phase 全绿——Phase 1（方案裁决：方案 1 `buildCode` 应用层长度守护 vs 方案 2 放宽 voucherCode precision 否决；新增 `AR_AP_ITEM_CODE_MAX_LENGTH=50` 常量 + 短码路径逐字符不变 + 超限路径保留固定段 + sourceBillCode 头部截断 + MD5 前 4 hex 摘要；方法签名不变向后兼容）/ Phase 2（长码回归测试 `testLongSourceBillCodeDoesNotOverflowVoucherCodePrecision` NOTES_RECEIVABLE_RECEIVED + 28 字符 code，经 stash 红绿反转证明：未修复时捕获 sqlState=22001/insert into ERP_FIN_AR_AP_ITEM，修复后全绿；既有 5 方法零回归）/ Phase 3（bug 文档 + 1430-1 Follow-up 标记 RELEASED + 日志聚合条目）。验证：`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinArApItemGeneration` 6 tests 0 errors 0 failures + `TestErpFinNotesReceivablePosting` 5 tests 0 errors + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。范围纪律清洁：生产代码变更严格限定 1 文件 1 protected 方法 + 2 常量，零 ORM/DDL/契约/xbiz/view 变更。

Closure Audit Evidence:

- Independent Closure Audit (2026-07-17-1600-1) — verdict: **PASS**
- Auditor: independent closure auditor 子代理 `ses_090bed33bffe9EmrylJ42rpMpH`（新会话冷重播，无执行者上下文），2026-07-17
- Method: 完整计划重读 + 8 checkpoint 实时仓库核实 + 2 个 maven 命令重跑
  - Checkpoint 1（一致性）：PASS — Phase 1-3 全 `Status: completed`，全部 items/exit criteria `[x]`；剩余 `[ ]` 仅 pre-tick Closure Gates。
  - Checkpoint 2（buildCode 实时代码）：PASS — 常量 `AR_AP_ITEM_CODE_MAX_LENGTH=50`（protected，Javadoc 对齐 voucherCode）@ ErpFinArApItemGenerator.java:49；签名不变 @ :216；短码路径逐字节一致 @ :219-222；超限路径 head 截断 + MD5-4hex 保留固定段 @ :223-229。最坏预算算术 = 恰好 50（max sourceBillType=OWNERSHIP_TRANSFER=18）。零 ORM/api/xbiz/view 变更。
  - Checkpoint 3（回归测试实时代码）：PASS — `testLongSourceBillCodeDoesNotOverflowVoucherCodePrecision` @ TestErpFinArApItemGeneration.java:216 驱动 NOTES_RECEIVABLE_RECEIVED + 28 字符 code；断言 voucherId/1-row+OPEN/code<=50/幂等。git diff 确认既有 5 方法逐字节不变（纯加性）。快照 output 文件就位（3 json5 + tables）。
  - Checkpoint 4（实时构建）：PASS — `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinArApItemGeneration` → `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` + BUILD SUCCESS。`mvn clean install -DskipTests` → 全 reactor BUILD SUCCESS（01:45 min）。
  - Checkpoint 5（红绿证明）：PASS — stash 证明内部一致，记录于 plan Phase 2 / 日志 / bug 文档。未重跑（按审计指令）。
  - Checkpoint 6（文档对齐）：PASS — bug 文档 修复+预防差距 更新；1430-1 Follow-up ×2 RELEASED；日志 07-17.md 顶部条目；backlog README 新 done 行。
  - Checkpoint 7（Deferred 诚实性）：PASS — Deferred 段空/未变；无范围内项目降级；无隐藏缺陷。
  - Checkpoint 8（范围纪律）：PASS — git status 仅 1 生产 Java + 1 测试 Java + 快照 + 4 文档 + 计划；零 ORM/api/xbiz/view。Non-Goals 受尊重。
- Blockers: 0。Major: 0。Minor 观察（无需行动）：超限不变量对所有当前常量恰好（=50）；假想未来 sourceBillType >32 字符会破坏上界，但无此常量存在（超出范围）。
- 结论：修复正确、验证全绿、文档对齐、范围清洁。计划真正完成，Closure Gates 可勾选。

Follow-up:

- 无范围内阻塞跟进。
