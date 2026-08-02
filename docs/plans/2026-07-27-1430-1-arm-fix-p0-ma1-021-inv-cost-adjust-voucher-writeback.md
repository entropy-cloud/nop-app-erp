# 2026-07-27-1430-1-arm-fix-p0-ma1-021-inv-cost-adjust-voucher-writeback P0 修复 — inventory 成本调整红冲绕过 I*Biz 跨模块写 ErpFinVoucher

> Plan Status: completed
> > Mission: audit-remediation
> > Work Item: P0-MA1-021（即时通道修复 — inventory `CostAdjustmentPostingDispatcher` 跨模块写 `ErpFinVoucher` 绕过 `IErpFinVoucherBiz`）
> Last Reviewed: 2026-07-27
> Source: `docs/audits/arm-index.md` §P0 发现追踪（P0-MA1-021，状态 `fix-plan-required (protected area gate)`）；`docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md` §6.1
> Related: `2026-07-27-1227-3-audit-remediation-ma1-platform-conformance-a-tier-core.md`（发现该 P0 的审计 plan，已 completed，closure Follow-up 显式命名本 fix plan）；`2026-07-05-2352-3`（CostAdjustmentPostingDispatcher 引入 plan）；`docs/design/finance/posting.md`（冲销反写 owner doc）；`docs/architecture/integration-and-transaction-patterns.md`
> Audit: required

## Current Baseline

P0-MA1-021 是 A1.12 平台合规审计（plan 2026-07-27-1227-3）在 inventory 维度 2（跨实体访问）发现的 P0 实时缺陷，已登记 `docs/audits/arm-index.md` §P0 追踪，状态 `fix-plan-required (protected area gate)`。roadmap 横切关注点 §P0 即时通道纪律明示 P0 不得进入 MR 批量修复，须即时通道就地修复或异步注入独立 fix plan；1227-3 closure 因其触及 finance 凭证保护区域，显式选择异步注入独立 fix plan（本计划），先于 MR1 执行。

缺陷精确定位（实仓核实）：

- `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/CostAdjustmentPostingDispatcher.java:79` 在 `reverse()` 内调用私有方法 `markOriginalVoucherReversed(adjust.getCode())`。
- 该私有方法 `:121-137` 经 `daoProvider.daoFor(ErpFinVoucher.class)` 与 `daoProvider.daoFor(ErpFinVoucherBillR.class)` 跨模块直接查并 **写** finance 实体 `ErpFinVoucher`：`voucher.setIsReversed(true); voucherDao.updateEntity(voucher);`（`:133-134`）。
- 此举违反 `AGENTS.md "跨实体访问"` + `docs/architecture/data-dependency-matrix.md §5.3`（跨域查询/写必须经 I\*Biz 接口）+ `integration-and-transaction-patterns.md` 业财一体写契约；属 plan P0 类别「业财一体写绕过 I\*Biz」，触及 finance 凭证保护区域。

关键事实（决定修复方案 — 实仓已核实，非推测）：finance 侧 `IErpFinVoucherBiz.reverse(billHeadCode, businessType, context)`（`ErpFinVoucherBizModel:80-84` 委托 `ErpFinPostingProcessor.reverseProcess`）**已经在 I\*Biz 边界内、以平台规范方式做了完全相同的标记**：

- `ErpFinPostingProcessor.reverseProcess:243` 在生成红字冲销凭证后调用 `markOriginalVoucherReversed(billHeadCode, businessType, context)`。
- 该 finance 侧 `markOriginalVoucherReversed`（`:909-923`）逻辑与 inventory 侧私有方法 `:121-137` **逐行等价**：按 `billCode + businessType` 反查 `ErpFinVoucherBillR` 回链 → 对 NORMAL + POSTED + 未冲销的原凭证置 `isReversed=true` + `updateEntity`。

而 `CostAdjustmentPostingDispatcher.reverse():78` 在调用 `:79` 之前**已经**调用了 `voucherBiz.reverse(adjust.getCode(), ErpFinBusinessType.COST_ADJUSTMENT, context)`（`:78`）。因此 `:79` 的 inventory 侧标记是**完全冗余的重复写**——finance 侧 `reverseProcess` 已在同一事务内完成该标记。

剩余差距：删除冗余绕过写 + 清理随之失效的导入 + 测试证明红冲后原凭证 `isReversed=true` 仍由 finance 侧正确设置 + arm-index/scope matrix 状态回填。

## Goals

- 移除 `CostAdjustmentPostingDispatcher` 中绕过 I\*Biz 的跨模块写（删除 `:79` 调用 + `:121-137` 私有方法 + 随之失效的导入），使 inventory 成本调整红冲路径 100% 经 `IErpFinVoucherBiz.reverse()` I\*Biz 契约完成凭证标记。
- 行为保持等价：红冲后原 NORMAL 凭证 `isReversed=true` 仍被设置（由 finance 侧 `reverseProcess:243` 完成，在同一 `@BizMutation` 事务内）。
- 提供测试证明：删除 inventory 侧标记后，`reverseCostAdjust` 仍使原凭证 `isReversed=true`（防回归 — 防止未来 finance 侧重构悄悄丢弃标记）。
- 回填 `arm-index.md` P0-MA1-021 状态为 `done` + `audit-remediation-scope-and-dimension-matrix.md §2.1` inventory Nop 平台合规列 `⚠️(P0)` → `⚠️(P1)`（P1-MA1-022 跨域只读 daoFor 仍待 MR1）。

## Non-Goals

- **不**修复 P1-MA1-022（pur+sal+ast+inv 跨域只读 IDaoProvider 通用模式，~14 文件）— 该 P1 是**只读**访问，与本 P0 的**写**绕过性质不同，经 R1.0 展开机制进入 MR1 统一裁决方案 A/B。
- **不**修复 P2-MA1-025/026（inventory/purchase owner-doc drift）— watch-only，MR1 顺手收敛。
- **不**改动 finance 侧 `ErpFinPostingProcessor.reverseProcess` / `markOriginalVoucherReversed` — finance 侧实现已平台规范且承担唯一标记责任，不在本计划范围。
- **不**改动 `CostAdjustmentPostingDispatcher` 的过账（`tryPost`/`postEvent`/`buildEvent`）与红字冲销调用本身（`:75-88` 的 `voucherBiz.reverse(...)` 保留）— 仅移除其后的冗余重复写。
- **不**重开 D1（字典 int→string）— 已裁决 Deferred。
- **不**手改生成物（`_gen/`、`_` 前缀、`_app.orm.xml`）— 本修复仅触及保留层 Java 源（`CostAdjustmentPostingDispatcher.java` 是非生成文件）。
- **不**为 P1-MA1-016（finance 读 assets 的 IDaoProvider）建立先例 — 那是只读跨域查询，与本 P0 的跨域**写**不同，仍由 MR1 裁决。

## Task Route

- Type: `implementation-only change`（修复已确认的实时缺陷，无契约/模型变更）
- Owner Docs: `docs/design/finance/posting.md`（冲销反写语义 — 验证 owner doc 描述 reverse 标记 isReversed 的预期行为）；`docs/architecture/integration-and-transaction-patterns.md`（业财一体写契约 / I\*Biz 边界）；`docs/architecture/data-dependency-matrix.md §5.3`（跨域访问规则）
- Skill Selection Basis: `nop-backend-dev`（修复涉及 I\*Biz 跨实体访问契约 + BizModel 写边界，roadmap skill 列匹配；技能路由 `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`）。`nop-debugging` 不匹配——这是已定位的已知缺陷修复，非调查。
- Verification: 修复触及 inventory + finance 交互；运行 inventory 单模块测试（`TestErpInvCostAdjust` / `TestErpInvPosting`）+ finance 单模块测试（确认 reverse 链路无回归）+ 全量 `mvn clean install -DskipTests`（154 模块）+ `mvn test`（回归基线，0 failures）。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖（无端口/环境变量/密钥/外部服务）。
- **保护区域门控（关键）**：本修复触及 finance 凭证（`ErpFinVoucher`）保护区域。预期行为由 owner doc `docs/design/finance/posting.md`（冲销反写）描述：`reverse()` 生成红字凭证并标记原凭证 `isReversed=true`。本修复**不改变**该预期行为（finance 侧早已实现标记），仅移除 inventory 侧的冗余重复写——属"使实现与 owner doc + 平台契约一致"的收敛性修复，行为等价。仍按保护区域纪律执行独立 plan-audit + closure-audit（本计划即此流程）。
- 无数据迁移/回滚脚本需求（行为等价，无持久化数据语义变化）。

## Execution Plan

### Phase 1 - 移除 inventory 侧冗余跨模块写 + 导入清理

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/CostAdjustmentPostingDispatcher.java`
Skill: `nop-backend-dev`

- Item Types: `Fix-heavy (3/4 items tagged Fix)`
- Prereqs: P0-MA1-021 已在 arm-index 登记（事实基线已确认）；finance 侧 `reverseProcess:243` 标记行为已实仓核实

- [x] `Decision | Fix`：确认修复方案 = 选项 A（删除冗余 inventory 侧标记，由 finance 侧 `IErpFinVoucherBiz.reverse()` 唯一承担）。理由（记入计划）：实仓核实 `ErpFinPostingProcessor.reverseProcess:243` 已在 I\*Biz 边界内调用 `markOriginalVoucherReversed`（`:909-923`），逻辑与 inventory 侧 `:121-137` 逐行等价；inventory 侧 `reverse():78` 已先调用 `voucherBiz.reverse(...)`，故 `:79` 是完全冗余的重复写。考虑的替代方案：选项 B（在 `IErpFinVoucherBiz` 新增 `markReversed(voucherId)` I\*Biz 写方法供 inventory 显式调用）— 否决，因为会引入多余的 I\*Biz 表面且 finance 侧 `reverse()` 语义已涵盖标记（接口 javadoc `IErpFinVoucherBiz:19,53-57` 明示 reverse 标记原凭证已红冲）。残留风险：若未来 finance 侧重构 `reverseProcess` 拆分标记步骤，须保证标记仍原子完成——由 Phase 2 测试守护。
      - Skill: `nop-backend-dev`
- [x] `Fix`：删除 `CostAdjustmentPostingDispatcher.markOriginalVoucherReversed` 私有方法（`:121-137`）+ 其在 `reverse()` 内的调用（`:79`）。保留 `reverse()` 的 `voucherBiz.reverse(adjust.getCode(), ErpFinBusinessType.COST_ADJUSTMENT, context)` 调用（`:78`）不变。
      - Skill: `nop-backend-dev`
- [x] `Fix`：清理删除后失效的导入。核实并移除仅被已删方法使用的导入（候选：`ErpFinVoucher`、`ErpFinVoucherBillR`、`IEntityDao`、`QueryBean`、`FilterBeans.and`、`FilterBeans.eq`、`java.util.Objects`）。保留仍被他处使用的导入（`IDaoProvider` 仍用于 `:56` 注入 + `:145` `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)`；`List` 仍用于 `tryPost` 签名）。以编译器警告为零标准确认（不留未使用导入）。
      - Skill: `nop-backend-dev`
- [x] `Proof`：核实 owner doc `docs/design/finance/posting.md` 描述 `reverse()` 标记原凭证 `isReversed=true` 的预期行为。若 owner doc 未显式陈述该标记语义，补一句对齐说明（指向 `ErpFinPostingProcessor.markOriginalVoucherReversed`），使保护区域行为有 owner doc 背书。若 owner doc 已充分描述，仅记录核实结论不改动。
      - Skill: none

Exit Criteria:

- [x] `CostAdjustmentPostingDispatcher` 不再含 `daoFor(ErpFinVoucher*)` 跨模块写；`reverse()` 仅经 `voucherBiz.reverse(...)` I\*Biz 契约
- [x] inventory service 单模块类型检查/编译通过（`mvn compile -pl module-inventory/erp-inv-service -am`）——解除后续测试阶段阻塞的本地化检查

### Phase 2 - 测试证明 + 索引/矩阵状态回填

Status: completed
Targets: `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvCostAdjust.java`（既有测试，扩展）；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.1`
Skill: `nop-testing`

- Item Types: `Proof | Follow-up`
- Prereqs: Phase 1 完成（冗余写已移除，编译通过）

- [x] `Proof`：扩展/补强 `TestErpInvCostAdjust`（或既有 reverse 路径测试）：断言 `reverseCostAdjust` 执行后，原成本调整对应的 NORMAL 凭证 `isReversed == true`（证明 finance 侧 `reverseProcess:243` 标记生效，inventory 侧冗余写移除后行为等价）。若既有测试已覆盖该断言，记录覆盖证据不重复添加。测试策略：单模块集成测试（inventory service，finance 在全量 classpath 可达）。
      - Skill: `nop-testing`
- [x] `Proof`：运行 inventory 单模块测试（`mvn test -pl module-inventory/erp-inv-service -am`）+ finance 单模块测试（`mvn test -pl module-finance/erp-fin-service -am`）确认 reverse/红冲链路无回归。0 failures。
      - Skill: `nop-testing`
- [x] `Follow-up`：回填 `docs/audits/arm-index.md` §P0 发现追踪 P0-MA1-021 行的"修复状态"列：`fix-plan-required (protected area gate)` → `done (plan 2026-07-27-1430-1)`，"修复 plan"列填本计划路径。
      - Skill: none
- [x] `Follow-up`：更新 `docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.1` Nop 平台合规行 inventory 列：`⚠️(P0)` → `⚠️(P1)`（P0 已修复；P1-MA1-022 跨域只读 daoFor 仍待 MR1）。同步更新 §2.1 顶部说明文字（inventory P0 已闭包）。
      - Skill: none

Exit Criteria:

- [x] 测试证明红冲后原凭证 `isReversed=true` 仍由 finance 侧正确设置（删除冗余写后行为等价）
- [x] inventory + finance 单模块测试 0 failures

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05da4746affe69cjWJASRZ0f4Q`，独立 general 子代理，对照实时仓库逐行复核）。VERDICT = accept，**无 BLOCKER**。核实要点：(A) P0 缺陷定位精确——`CostAdjustmentPostingDispatcher.java:78/79/121-137/133-134` 行号全部精确、`markOriginalVoucherReversed` 在 module-inventory 内仅 `:79` 一处调用（safe to delete）；(B) **关键冗余性主张经逐行核实为真**——`ErpFinVoucherBizModel.reverse:80-84` → `ErpFinPostingProcessor.reverseProcess:243` → `markOriginalVoucherReversed:909-923` 三段链路确认 finance 侧已在 I\*Biz 边界内、同一 `@BizMutation`/`REQUIRES_NEW` 事务内对 NORMAL+POSTED+未冲销原凭证置 `isReversed=true`，逻辑与 inventory 侧 `:121-137` 逐行等价（同一 link 查询 + 同一过滤 + 同一 `setIsReversed(true)+updateEntity`）；常量解析一致（`ErpFinConstants.VOUCHER_STATUS_POSTED/POSTING_TYPE_NORMAL`）；且因 `reverse` 为 `REQUIRES_NEW`，inventory `:79` 执行时原凭证已 `isReversed=true`，其过滤 `!isReversed` 已评估为 false → inventory 侧 `updateEntity:134` 实为 no-op，删除严格安全；(C) owner doc `posting.md` 存在但未显式陈述 isReversed 标记（契约在 `IErpFinVoucherBiz.java:54-57,63` javadoc + 兄弟 owner doc），Phase 1 Proof 项条件补注分支将触发——处理得当；(D) P1-MA1-022 只读 daoFor 正确排除（与 P0 写绕过性质不同）；(E) Closure Gates 全仓库验证 + 阶段退出本地化（规则 7）合规；(F) 14 条最低规则全满足；(G) N=1 顺序与 P0 即时通道优先级合理。采纳的非阻塞修正：(NB-1) Phase 1 导入候选清单补 `java.util.Objects`（仅被已删方法 `:130,132` 使用）；(NB-2) Phase 2 typo `datrix` → `dimension`（避免陈旧文件路径）；(NB-3) Phase 1 类型标签 `Fix` → `Fix-heavy (3/4)`（4 项中 3 项 Fix-bearing，未达 80% 阈值故按项标注，phase 级标签精化）。三项均已完成。

## Closure Gates

> 本计划是代码修复（触及 finance 凭证保护区域）。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（P0-MA1-021 跨模块写绕过已移除 + 测试证明行为等价 + arm-index/scope matrix 状态回填）
- [x] 相关文档对齐（owner doc `posting.md` 核实结论记录；arm-index P0 状态回填；scope matrix §2.1 inventory 列更新）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，01:38 min）+ `mvn test`（1756 tests，0 failures / 0 errors / 1 skipped = `ErpAllWebPagesCollectTest @Disabled` M0 锚点已知接受项，08:51 min）+ `mvn test -pl module-inventory/erp-inv-service`（120 tests，0 failures）+ `mvn test -pl module-finance/erp-fin-service`（285 tests，0 failures）（reverse 链路无回归）
- [x] compliance checker 基线不高于 M0 锚点（R2c `daoFor()` 总量 1228 → 1226，减少 2 = 移除的两个 `daoFor(ErpFinVoucher*)` 调用；其余规则无增）
- [x] 无范围内项目降级为 deferred/follow-up（P0 不得降级——已即时通道修复；P1-MA1-022 不属本计划范围，按设计进入 MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志都一致
- [x] 独立结束审计由独立子代理（新会话 `ses_05d89627dffeloAbAIB3Ec9YG3`）执行；VERDICT = PASS，0 blocker；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-MA1-022（pur+sal+ast+inv 跨域只读 IDaoProvider 模式）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 该 P1 是**只读**跨域 `daoFor` 查询（~14 文件），与本 P0 的跨域**写**绕过性质不同。已登记 arm-index §P1 详细清单，经 R1.0 展开机制进入 MR1 统一裁决方案 A（master-data/finance I\*Biz 补只读方法后迁移）或方案 B（永久接受为 Helper 合法模式）。本 P0 修复不建立任何先例约束 MR1 对 P1-MA1-022 的裁决。
- Successor Required: `yes`——MR1 经 R1.0 展开。

## Closure

Status Note: 修复完成。inventory `CostAdjustmentPostingDispatcher` 跨模块写绕过已移除（选项 A），红冲凭证标记 100% 由 finance 侧 `IErpFinVoucherBiz.reverse()` I\*Biz 承担，行为等价经 `TestErpInvCostAdjust.testReverseRollsBackBalanceAndVoucher` 防回归断言守护。独立结束审计 PASS（0 blocker）。

Closure Audit Evidence:

- 独立结束审计子代理 `ses_05d89627dffeloAbAIB3Ec9YG3`（general，新会话，对照实时仓库逐项只读复核）。VERDICT = **PASS**，**0 blocker**。核实要点（全部 ✅）：(1) `CostAdjustmentPostingDispatcher` 117 行，`markOriginalVoucherReversed` 方法 + 调用已删，`reverse()` 仅经 `voucherBiz.reverse(...)` I\*Biz 契约（line 67），无跨模块写残留；7 个失效导入 + `POSTING_TYPE_NORMAL` 字段已清，`IDaoProvider` 正确保留（line 14 import + line 115 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)` 使用）；`tryPost/postEvent/buildEvent` 未变。(2) finance 侧 `ErpFinPostingProcessor.reverseProcess:243 → markOriginalVoucherReversed:909-923` 链路完整，标记逻辑未动。(3) 测试证明 `testReverseRollsBackBalanceAndVoucher` 经 `findOriginalNormalVoucherByBillCode` helper（按 `postingType==null||"NORMAL"` 过滤，排除 REVERSAL 凭证）断言红冲后原 NORMAL 凭证 `isReversed=true`。(4) owner doc `posting.md §方向一` 已补引擎侧标记责任说明 + I\*Biz 边界警示。(5) `arm-index.md` P0-MA1-021 行修复状态 = `done (plan 2026-07-27-1430-1)`；`scope-and-dimension-matrix.md §2.1` inventory 列 = `⚠️(P1)`，narrative 显式"P0-MA1-021 已闭包"。(6) 验证数字与代码改动性质一致（纯减法：1 方法 + 1 调用 + 失效导入；R2c 1228→1226 = 移除的 2 个 `daoFor(ErpFinVoucher*)`）。

Follow-up:

- P1-MA1-022 经 R1.0 展开机制进入 MR1（与本 P0 修复独立）
