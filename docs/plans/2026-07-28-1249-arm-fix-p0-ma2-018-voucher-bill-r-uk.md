# 2026-07-28-1249-arm-fix-p0-ma2-018-voucher-bill-r-uk P0 fix：过账业财回链表幂等键加 DB 唯一约束

> Plan Status: planned
> Mission: audit-remediation
> Work Item: P0-MA2-018 fix（A2.17 并发与乐观锁审查发现的 P0）
> Last Reviewed: 2026-07-28
> Source: `docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md §11 P0-MA2-018`
> Related: `docs/plans/2026-07-28-1249-3-audit-remediation-ma2-concurrency-optimistic-lock.md`（来源审计 plan，A2.17 done）；`docs/design/flow-overview.md §八.3 幂等性`（owner doc 契约）；`docs/design/finance/posting.md`（业财回链幂等键）；`docs/architecture/processor-extension-pattern.md`（REQUIRES_NEW Facade 硬规则 1）
> Audit: required

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

Status: planned

Targets: `module-finance/model/app-erp-finance.orm.xml`（ErpFinVoucherBillR 实体）；`module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java`（alreadyPublished + ConstraintViolation 兜底）；数据 cleanup 脚本（如需）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: A2.17 done（P0-MA2-018 已识别）；nop-entropy 父 POM 已在本地 Maven 仓库；数据 cleanup 评估完成

- [ ] 数据评估：grep `erp_fin_voucher_bill_r` 现存重复 `(billCode, businessType)` 行（如有，归档/合并策略 + 人工确认）
- [ ] ORM 变更：`app-erp-finance.orm.xml` ErpFinVoucherBillR 加 `<key name="UK_FIN_VOUCHER_BILL_R_BILL" unique="true"><column name="billCode"/><column name="businessType"/></key>`（执行时确认 alreadyPublished query 用 billCode 还是 billHeadCode）
- [ ] `mvn clean install -DskipTests`（codegen 增量再生 + 154 reactor 全绿）
- [ ] `ErpFinPostingProcessor.persistVoucher` 捕获 ConstraintViolation → 翻译为 `ERR_FIN_VOUCHER_ALREADY_POSTED`（或复用现有错误码）+ `alreadyPublished` pre-check 保留为友好错误提示
- [ ] owner doc `docs/design/finance/posting.md §业财回链幂等键` 补「DB 唯一约束 `(billCode, businessType)` 兜底 + alreadyPublished pre-check 友好提示」
- [ ] 补负向测试：`testPostDuplicateBillHeadCodeThrowsAlreadyPosted`（并发/重复 post 同 billHeadCode+businessType 抛约束违例或业务错误码）
- [ ] 运行 `mvn clean install -DskipTests`（154 reactor 全绿）+ `mvn test -pl module-finance/erp-fin-service`（过账测试全绿）

Exit Criteria:

- [ ] ErpFinVoucherBillR `(billCode, businessType)` 唯一约束落地 + codegen 再生全绿
- [ ] ConstraintViolation 兜底翻译为业务错误码 + pre-check 友好提示保留
- [ ] 负向测试覆盖并发重复 post 场景
- [ ] owner doc 同步更新

## Closure Gates

- [ ] 范围内行为完成（UK 落地 + 错误码翻译 + 测试通过）
- [ ] 相关文档对齐（posting.md §业财回链幂等键 + flow-overview.md §八.3）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立 plan-audit 完成
- [ ] 文本一致性已验证

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待执行后填写>
- Evidence: <待执行后填写>
