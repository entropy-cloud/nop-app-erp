# 2026-07-28-1249-arm-fix-p0-ma2-018-voucher-bill-r-uk P0 fix：过账业财回链表幂等键加 DB 唯一约束

> Plan Status: deferred
> Mission: audit-remediation
> Work Item: P0-MA2-018 fix（A2.17 并发与乐观锁审查发现的 P0）
> Last Reviewed: 2026-07-28
> Source: `docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md §11 P0-MA2-018`
> Related: `docs/plans/2026-07-28-1249-3-audit-remediation-ma2-concurrency-optimistic-lock.md`（来源审计 plan，A2.17 done）；`docs/design/flow-overview.md §八.3 幂等性`（owner doc 契约）；`docs/design/finance/posting.md`（业财回链幂等键）；`docs/architecture/processor-extension-pattern.md`（REQUIRES_NEW Facade 硬规则 1）
> Audit: required
> Blocked: YES — 独立 plan-audit（见 §Closure / Closure Audit Evidence）发现提议的字面 UK `(billCode, businessType)` 与红冲/多套账已实现契约冲突（`testReverse:225` 断言同键 2 行），按字面落地必然回归。
> Disposition: 原始 scope（billR 上加普通多列 UK `(billCode, businessType)`）经独立 plan-audit 裁定为不可实施（见 §Closure Audit Evidence 证据 1/2/3）。依据 AGENTS.md 规则 12「保护区域、未解决的产品风险或真相源冲突需要人工/子代理审查或保持阻塞」+ 计划指南规则 10（范围内的检查清单项目须完成或明确移出范围并写入理由），本 plan 不以原始形式关闭，状态置为 `deferred`。Phase 1 全部 execution items / Exit Criteria / Closure Gates 已移至 §Deferred But Adjudicated 并分类，等候人工裁决修复方向（A 部分唯一索引 / B 反范式化判别列到 voucher+复合 UK / C SELECT FOR UPDATE / D 分布式锁 SPI）后，另起 successor plan 重过独立 plan-audit 方可实施。

## Current Baseline

A2.17 并发与乐观锁审计发现 **P0-MA2-018**：`erp_fin_voucher_bill_r(billCode, businessType)`（或 `(billHeadCode, businessType)`——对齐 `ErpFinPostingProcessor.alreadyPosted:472` query 字段）**无 DB 唯一约束**；`ErpFinPostingProcessor.alreadyPosted` 是 TOCTOU pre-check query；`IErpFinVoucherBiz.post` `@Transactional(REQUIRES_NEW)` 独立事务隔离 → 并发 `post()` / `ErpFinDeferredPostingRetryHelper` 兜底重试 / 人工重试可同时通过 pre-check 双 INSERT 重复凭证 + billR。

**实时仓库证据**（`module-finance/erp-fin-service/src/main/java/`）：

- `ErpFinVoucherBizModel.java:71` `@Transactional(propagation = TransactionPropagation.REQUIRES_NEW)` 显式钉 Facade
- `ErpFinPostingProcessor.java:472-484` `alreadyPosted()` query pre-check + `:837-842` `persistVoucher()` billR INSERT（无 DB 约束兜底）
- `module-finance/model/app-erp-finance.orm.xml:643-647` 仅 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID`（非唯一 on voucherId），**无** `(billCode, businessType)` unique-key
- `ErpFinDeferredPostingRetryHelper.java:74,131` REQUIRES_NEW 单条重试，依赖 `alreadyPosted` pre-check（同 TOCTOU race）

**影响**：GL 重复入账，借贷双计，破坏财务报表正确性 + 业财幂等键不变量（同一单据同一业务类型应仅一张凭证）。触发面广：任何过账失败后被兜底重试 + 人工重试同时介入即触发。

## Goals

- 修复 P0-MA2-018：(1) `module-finance/model/app-erp-finance.orm.xml` 给 `ErpFinVoucherBillR` 加唯一约束 `<key name="UK_FIN_VOUCHER_BILL_R_BILL" unique="true">` on `(billCode, businessType)`（对齐 alreadyPosted query 字段；如 `alreadyPosted` 用 `billHeadCode` 字段则 UK on `(billHeadCode, businessType)`——执行时确认 query 字段）；(2) 数据 cleanup（若现存重复 billR 行，须先归档/合并）；(3) `alreadyPosted` pre-check 保留为友好错误提示；(4) ConstraintViolation 兜底翻译为 `ERR_FIN_VOUCHER_ALREADY_POSTED`（或复用现有错误码）。
- 触及 finance 凭证保护区域 + ORM ask-first（唯一约束变更）→ 须独立 plan-audit + 人工确认。
- 补并发重复 post 负向测试（同 billHeadCode 并发 post 应抛约束违例或业务错误码）。

## Non-Goals

- **不**修复 `CloseVoucherWriter.writeVoucher` 重复 FX/PL 凭证（P1-MA2-087——本 UK 落地后自动受保护，无须独立修复）。
- **不**改 REQUIRES_NEW 事务边界（事务边界钉 Facade 是 `processor-extension-pattern.md` 硬规则 1）。
- **不**改 `ErpFinDeferredPostingRetryHelper` 重试机制（依赖 alreadyPublished pre-check + UK 兜底即可）。

## Task Route

- Type: `Bug investigation` + `implementation change`
- Owner Docs: `docs/design/finance/posting.md`（业财回链幂等键 §）+ `docs/design/flow-overview.md §八.3 幂等性`
- Skill: `nop-backend-dev`（ORM UK 变更 + 错误码翻译）+ ORM ask-first
- Verification: `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-finance/erp-fin-service`（过账相关测试）+ 并发重复 post 负向新测试通过

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。Maven Reactor 走标准构建。
- **保护区域门控**：finance 凭证保护区域 + ORM ask-first（唯一约束变更）→ 须 owner doc + 人工确认 + 独立 plan-audit。
- **数据迁移门控**：UK 落地前须确认 `erp_fin_voucher_bill_r` 现存无重复（如有重复须先归档/合并）。

## Execution Plan

### Phase 1 - 加 ErpFinVoucherBillR 唯一约束 + 数据 cleanup + 错误码翻译

Status: deferred（原始 execution items 已 adjudicate 至 §Deferred But Adjudicated；不可机械勾选 `[x]`，因字面 scope 经独立 plan-audit 裁定为不可实施）

Targets: `module-finance/model/app-erp-finance.orm.xml`（ErpFinVoucherBillR 实体）；`module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java`（alreadyPublished + ConstraintViolation 兜底）；数据 cleanup 脚本（如需）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: A2.17 done（P0-MA2-018 已识别）；nop-entropy 父 POM 已在本地 Maven 仓库；数据 cleanup 评估完成

原 execution items（数据评估 / ORM UK 变更 / codegen 再生 / ConstraintViolation 翻译 / owner doc 同步 / 负向测试 / 全量验证）在原始字面 scope 下均不可实施——独立 plan-audit 已证明字面 UK `(billCode, businessType)` 与红冲「同键 2 行」契约（`testReverse:225`）+ 多套账「同键 N 行」契约（`TestErpFinMultiSchemaPropagatesTwoVouchersWithDistinctSchema:89`）+ 软删除重插生命周期冲突，按字面落地必然回归。依据计划指南规则 10，这些 items 不保持 `[ ]` 待办态，而是 **adjudicate 为 residual-risk-only** 并整体移至 §Deferred But Adjudicated「P0-MA2-018 字面 UK scope」项下，等候人工裁决修复方向后由 successor plan 接管。完整拒绝理由与证据见 §Closure Audit Evidence。

Exit Criteria:

> 此 phase 已 deferred，原始 Exit Criteria 不再适用于现 scope。保留如下仅作 successor plan 起草参考（非本 plan 关闭条件）：

- ErpFinVoucherBillR `(billCode, businessType)` 唯一约束落地 + codegen 再生全绿 — **deferred**（字面 UK 不可实施，须先裁决方向 A/B/C/D）
- ConstraintViolation 兜底翻译为业务错误码 + pre-check 友好提示保留 — **deferred**（依赖 UK 落地）
- 负向测试覆盖并发重复 post 场景 — **deferred**（须先确定「合法多行 vs 并发重复 INSERT」的判别语义）
- owner doc 同步更新 — **deferred**（无代码变更，无须同步）

## Closure Gates

> 本 plan 状态为 `deferred`（非 `completed`），Closure Gates 不适用为关闭门控。保留如下仅作 successor plan 接管时的关闭参考（非本 plan 关闭条件）：

- 范围内行为完成（UK 落地 + 错误码翻译 + 测试通过） — **N/A**（scope deferred，须 successor plan 落地）
- 相关文档对齐（posting.md §业财回链幂等键 + flow-overview.md §八.3） — **N/A**（无代码变更，无须同步）
- 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service` — **N/A**（无代码变更）
- 无范围内项目降级为 deferred/follow-up — **adjudicated**（原始 scope 经独立 plan-audit 裁定为不可实施，已正式移至 §Deferred But Adjudicated 并命名 successor 触发条件，非静默降级）
- 独立 plan-audit 完成 — **done**（独立 plan-audit 子代理已执行实施前审计并出具 REJECT/BLOCK 裁决，证据见 §Closure Audit Evidence；EXECUTE 重跑复核亦确认阻断性发现仍在活仓成立）
- 文本一致性已验证 — **done**（Plan Status=deferred / Phase Status=deferred / Closure Gates=N/A / Closure Status Note=deferred 一致）

## Deferred But Adjudicated

### P0-MA2-018 字面 UK `(billCode, businessType)` scope

- Classification: `watch-only residual`（原始 scope 的字面实现不可实施；底层 P0 并发缺陷仍存在于活仓，但字面 UK 修复方向已裁决为回归性，须换方向）
- Why Not Blocking Closure: 字面 UK `(billCode, businessType)` 经独立 plan-audit 证明与红冲「同键 2 行」契约（`TestErpFinPostingService.testReverse:225` `assertEquals(2, countBillLinks("AP-REV-001", BUSINESS_TYPE_AP_INVOICE))`）+ 多套账「同键 N 行」契约（`TestErpFinMultiSchemaPropagatesTwoVouchersWithDistinctSchema:89`，billR 无 `acctSchemaId` 列无法区分账套）+ 软删除重插生命周期（`erp_fin_voucher_bill_r useLogicalDelete="true"`，orm.xml:625）三重冲突，按字面落地必然回归。区分「并发重复正向 INSERT（应阻止）」与「合法多行：红冲 REVERSAL / 多账套 / 冲销后重过账（应允许）」所需的判别列（`postingType`、`isReversed`、`acctSchemaId`）均位于 `ErpFinVoucher`，不在 `ErpFinVoucherBillR`，故 billR 上的普通多列 UK 无法表达该「部分唯一」语义。本 plan 不以原始形式关闭，置为 `deferred`。
- Successor Required: `yes` — 触发条件：人工裁决下列修复方向之一后，另起 successor plan（重做 UK 设计 + 补红冲/多套账不回归用例）并重过独立 plan-audit：
  - A. 部分唯一索引（`WHERE voucher NORMAL AND NOT reversed`）— NOP 跨 H2/MySQL/PG DDL 不统一支持，需平台层评估
  - B. 反范式化判别列到 voucher（`billHeadCode`+`businessType`+`acctSchemaId`+`postingType`+`isReversed` 复合 UK）— 触及 `ErpFinVoucher` 保护实体 + 软删除 UK 交互，范围显著扩大
  - C. `alreadyPosted` 改 `SELECT FOR UPDATE` 悲观锁 — 审计 §11 已列「方案 B 不推荐（REQUIRES_NEW 下死锁风险）」
  - D. 引入 `IErpSysLockBiz` 分布式锁 — 本仓库不存在该 SPI（审计 §8：全域 grep=0），需先实现
- 活仓证据（2026-07-28 复核确认仍成立）：`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinPostingService.java:225`、`TestErpFinMultiSchemaPosting.java:80,89`、`ErpFinPostingProcessor.java:209,233`、`module-finance/model/app-erp-finance.orm.xml:625-647`

## Closure

Status Note: **DEFERRED — 原始字面 UK scope 经独立 plan-audit 裁定为不可实施（与红冲/多套账/软删除生命周期三重契约冲突，按字面落地必然回归）。本 plan 不以原始形式关闭，状态置为 `deferred`；Phase 1 全部 execution items / Exit Criteria / Closure Gates 已 adjudicate 至 §Deferred But Adjudicated 并命名 successor 触发条件（人工裁决修复方向 A/B/C/D 之一 → 另起 successor plan → 重过独立 plan-audit）。底层 P0 并发缺陷仍存在于活仓，由 successor plan 接管，非静默降级。**

Closure Audit Evidence:

- Auditor / Agent: 独立 plan-audit 子代理（实施前审计门，AGENTS.md 规则 12 + 本 plan Closure Gates「独立 plan-audit 完成」+ Infrastructure And Config Prereqs「须独立 plan-audit + 人工确认」）
- 审计裁决：**REJECT / BLOCK（存在未解决的主要异议）**
- 阻断性发现：**计划提议的 `<key name="UK_FIN_VOUCHER_BILL_R_BILL" unique="true"><column name="billCode"/><column name="businessType"/></key>` 与现行已实现且被测试固化的契约矛盾，按字面落地必然回归。**

  **证据 1（配置无关，必然回归）—— 红冲流程对同一 `(billCode, businessType)` 写入第 2 条 billR：**
  - `ErpFinPostingProcessor.persistVoucher`（`:764-846`）对正向过账与红冲**复用同一路径**写 billR（`:836-842`）。
  - 红冲入口 `reverseProcess`（`:209`、`:232`）以**原** `billHeadCode`+`businessType` 调用 `persistVoucher(..., POSTING_TYPE_REVERSAL, billHeadCode, businessType, ...)`，故红字凭证的 billR 与原凭证 billR 共享同一 `(billCode, businessType)`。
  - 既有测试 `TestErpFinPostingService.testReverse:225` 显式断言：`assertEquals(2, countBillLinks("AP-REV-001", BUSINESS_TYPE_AP_INVOICE))` —— 即同键允许 2 行（原 + 红）。
  - 后续 `markOriginalVoucherReversed`（`:909-923`）仅把原凭证 `isReversed=true`，且「过账→红冲→重过账」会再写第 3 条同键 billR（设计语义，posting.md §业财回链表 + §冲销的并发与审计）。
  - 结论：字面 UK 落地后，红冲 INSERT 立即抛约束违例 → `testReverse` 红 + 红冲功能在生产环境损坏。

  **证据 2（config-gated，启用即回归）—— 多套账传播对同一 `(billCode, businessType)` 写入 N 条 billR：**
  - `process`（`:131`、`:161-188`）对 `schemaPropagator.resolveTargetSchemas` 返回的每个账套各调一次 `persistVoucher(event,...)`，`event!=null` → 每次 `resolvedBillCode=event.getBillHeadCode()`、`resolvedType=event.getBusinessType()` → N 条同键 billR（仅 `voucher.acctSchemaId` 不同，而 billR 无 acctSchemaId 列）。
  - 既有测试 `TestErpFinMultiSchemaPosting.testMultiSchemaPropagatesTwoVouchersWithDistinctSchema:89` 生成 2 张凭证（即 2 条同键 billR）。启用 `erp-fin.multi-schema-enabled=true` 即触发 UK 冲突。

  **证据 3 —— 软删除表 + UK 叠加问题：** `erp_fin_voucher_bill_r` 声明 `useLogicalDelete="true"`（`:625`）；逻辑删除后重插入同键亦与 UK 冲突（与本仓库既有 `UK_*_CODE_ORG` 模式同型，但既有 UK 所在实体不存在「同键多行」生命周期，billR 存在）。

- 根因分析：P0-MA2-018 期望不变量实为「**同一 billCode+businessType（+账套）下至多一张「未冲销的 NORMAL 已过账凭证」**」。区分「并发重复正向 INSERT（应阻止）」与「合法多行：红冲 REVERSAL / 多账套 / 冲销后重过账（应允许）」所需的判别列（`postingType`、`isReversed`、`acctSchemaId`）**均位于 `ErpFinVoucher`，不在 `ErpFinVoucherBillR`**。因此 billR 上的普通多列 UK **无法**表达该「部分唯一」语义；其生命周期（过账→红冲→重过账→…）对同键产生**无界**行数，静态 UK 永远冲突。
- 可行修复方向（均超出本 plan 字面范围，属保护区域 ORM/架构决策，须人工确认后修订 plan）：
  - A. 部分唯一索引（`WHERE voucher NORMAL AND NOT reversed`）—— NOP 跨 H2/MySQL/PG DDL 不统一支持，需平台层评估；
  - B. 反范式化判别列到 billR/voucher（如 voucher 增 `billHeadCode`+`businessType`+`acctSchemaId`+`postingType`+`isReversed` 复合 UK）—— 触及 `ErpFinVoucher` 保护实体 + 软删除 UK 交互，范围显著扩大；
  - C. `alreadyPosted` 改 `SELECT FOR UPDATE` 悲观锁 —— 审计 §11 已列「方案 B 不推荐（REQUIRES_NEW 下死锁风险）」；
  - D. 引入 `IErpSysLockBiz` 分布式锁 —— 本仓库不存在该 SPI（审计 §8：全域 grep=0），需先实现。
- 处置：依据 AGENTS.md 规则 12「保护区域、未解决的产品风险或真相源冲突需要人工/子代理审查或保持阻塞」+ 本 plan prereqs「须独立 plan-audit + 人工确认」，**实施前阻断**。Phase 1 全部条目维持 `[ ]`，`Plan Status` 维持 `planned`。须人工裁决修复方向 → 修订本 plan（重做 §Goals/§Execution Plan 的 UK 设计 + 补红冲/多套账不回归的验证用例）→ 重过独立 plan-audit → 方可实施。
- Evidence: 见上「证据 1/2/3」引用的实仓文件与行号（`ErpFinPostingProcessor.java`、`TestErpFinPostingService.java`、`TestErpFinMultiSchemaPosting.java`、`module-finance/model/app-erp-finance.orm.xml`）。

EXECUTE 重跑复核（2026-07-28，第三次驱动）：依据 MISSION_DRIVER 重新进入 EXECUTE，对三项阻断性发现逐条复核活仓代码，结论不变——计划**仍真正阻断**，不可机械补齐或强关：

- 证据 1 复核确认：`ErpFinPostingProcessor.reverseProcess:209,232-233` 以原 `billHeadCode`+`businessType` 调 `persistVoucher(..., POSTING_TYPE_REVERSAL, billHeadCode, businessType, ...)`；`persistVoucher:836-842` 无条件写 billR；`TestErpFinPostingService.java:225` `assertEquals(2, countBillLinks("AP-REV-001", BUSINESS_TYPE_AP_INVOICE))` 固化同键 2 行（原 + 红）。
- 证据 2 复核确认：`process:131,161-188` 对 `resolveTargetSchemas` 返回的每个账套各调 `persistVoucher`，billR 无 `acctSchemaId` 列（`orm.xml:626-639`）→ N 条同键 billR；`TestErpFinMultiSchemaPosting.java:89` `assertEquals(2, vouchers.size())` 固化 2 张凭证。
- 证据 3 复核确认：`orm.xml:625` `useLogicalDelete="true"` 叠加静态 UK 与「过账→红冲→重过账」生命周期冲突。

字面 UK `(billCode, businessType)` 落地必然回归证据 1/2，故 Phase 1 全部 `[ ]` 维持未勾选、`Plan Status: planned` 维持、`Blocked: YES` 维持。修复方向（A 部分唯一索引 / B 反范式化判别列到 voucher+复合 UK / C SELECT FOR UPDATE / D 分布式锁 SPI）属 finance 凭证保护区域 ORM+架构决策，按 AGENTS.md 规则 12 + 本 plan prereqs「须独立 plan-audit + 人工确认」须由人工裁决后修订 §Goals + §Execution Plan + 补红冲/多套账不回归用例并重过独立 plan-audit 方可实施。本次 EXECUTE 无代码变更，依法返回 `fail`（execution blocked）。
