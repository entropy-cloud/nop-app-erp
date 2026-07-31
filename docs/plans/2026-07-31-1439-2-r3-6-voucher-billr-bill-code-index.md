# 2026-07-31-1439-2-r3-6-voucher-billr-bill-code-index R3.6 — ErpFinVoucherBillR 缺 (billCode, businessType) 非唯一索引

> Plan Status: active
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR3 R3.6（P1-MA7-001，**[ORM ask-first]**）
> Related: `docs/plans/2026-07-31-0958-1-r3-0-mr3-p1-finding-expansion.md`（R3.0 展开 R3.6）；`docs/audits/2026-07-29-1708-arm-ma7-error-code-index-nplus1.md`（P1-MA7-001）；`docs/plans/2026-07-28-1249-arm-fix-p0-ma2-018-voucher-billr-uk.md`（P0-MA2-018 deferred 字面 UK——互补不重复）
> Audit: required

## Current Baseline

**P1-MA7-001**：`ErpFinVoucherBillR`（业财回链，`module-finance/model/app-erp-finance.orm.xml:623-648`）是业财过账回链表——每张过账凭证按 `(billCode, businessType)` 反向定位关联单据。审计发现该实体的热查询路径**无对应索引**：

- **唯一索引**：仅 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID`（`unique="false"`，on `voucherId`，line 644）。**无 (billCode, businessType) 索引。**
- **热查询路径全表扫描**（实测 `ErpFinPostingProcessor.java:895-901`）：
  ```java
  protected List<ErpFinVoucherBillR> findBillLinks(String billHeadCode, ErpFinBusinessType businessType, ...) {
      QueryBean q = new QueryBean();
      q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.name())));
      return dao.findAllByQuery(q);
  }
  ```
  `findBillLinks` 被 `alreadyPosted`（line 472-473，过账幂等判定）/ `markOriginalVoucherReversed`（line 920-922，红冲补标）/ 红冲路径（line 861/879/922）反复调用——**每次业财过账/红冲都触发一次 `(billCode, businessType)` 全表扫描**。兄弟服务同样模式：`BankReconAdjustmentVoucherBuilder.java:113-114`（`eq("billCode")+eq("businessType")`）、`BadDebtProvisionService.java:165-166`（`findUnreversedProvisionVouchers`）。
- **性能影响**：随 `erp_fin_voucher_bill_r` 行数累积（每张凭证 N 行回链），过账延迟**线性增长**。MA7 审计评级 P1（非 P0：当前无数据破坏，是性能退化；单组织基线测试不暴露，因数据量小）。
- **列定义**（确认存在，propId 已稳定）：`billCode` VARCHAR(50) mandatory propId=4（line 630）；`businessType` VARCHAR(30) dict=`erp-fin/business-type` propId=6（line 632，**非 mandatory**——schema 可空，但业务上由 `CloseVoucherWriter.setBusinessType` 写入时恒有值）。`billCode` 必填非空；`businessType` 写入期恒有值但 schema 可空——非唯一索引天然容纳 NULL 行（NULL 行不破坏索引加速语义），故可空性不影响索引正确性。

**与 P0-MA2-018（deferred 字面 UK）互补不重复**（arm-index:281 交叉协同注记 + R3.0 确认）：P0-MA2-018 裁决为 deferred 方向——在 billR 上建**字面唯一键 (billCode, businessType, billLineCode)** 会与红冲（同一 billCode 允许多张凭证）/ 多账套（acctSchemaId 维度）/ 软删除（delVersion）三重契约冲突，故 deferred。**本 R3.6 加的是 `unique="false"` 非唯一索引**——仅加速查询，不施加唯一约束，**不触发** P0-MA2-018 的三重冲突。与 P2-MA7-005（红冲有界 N+1 批量加载，watch-only）协同——本索引让 N+1 批量加载的前提（单点查询高效）成立，收益最大。

剩余差距：热查询路径无索引；owner doc 未记录该索引与过账性能关系。

## Goals

- 在 `app-erp-finance.orm.xml` 的 `ErpFinVoucherBillR.<indexes>` 增加非唯一复合索引 `IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE` on `(billCode, businessType)`（`unique="false"`），与现有 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID` 并列。
- 触发 codegen 增量重新生成（`mvn clean install -DskipTests`，**不**重跑 `nop-cli gen`），确认生成物（`_app.orm.xml` / DDL）含新索引。
- owner doc `docs/design/finance/posting-log.md` 记录该索引（业财回链热查询路径 + 过账/红冲性能含义 + 与 P0-MA2-018 deferred UK 的边界区分）。
- 验证：`mvn clean install -DskipTests` BUILD SUCCESS + xmllint ORM well-formed + compliance checker 无新增命中 + grep 新索引存在于生成物 + grep 非唯一（`unique="false"`）确保不与 P0-MA2-018 冲突。
- arm-index §P1 详细清单回填 P1-MA7-001 = `MR3 done (R3.6)`；roadmap R3.6 Status `todo`→`done`。

## Non-Goals

- **P0-MA2-018 deferred 字面唯一键**（三重契约冲突，独立 deferred 决议）——本 plan 严格非唯一，不施加约束。
- P2-MA7-005 红冲有界 N+1 批量加载优化（watch-only，须另行改写 findBillLinks 为批量 in 查询）。
- 其余 S/A 级域的索引缺口（属 MA7 A7.2 索引完整性审计范围，非本 finding）。
- 改写 `findBillLinks` 查询逻辑（仅加索引加速现有查询，不改调用点签名）。
- R3.5 / R3.7（独立 plan）。

## Task Route

- Type: `implementation-only change`（ORM 索引增量 + codegen 再生 + owner doc；不改 API 契约/业务逻辑/实体结构）
- Owner Docs: `module-finance/model/app-erp-finance.orm.xml`（权威 ORM 源）；`docs/design/finance/posting-log.md`（业财回链语义）；`docs/audits/arm-index.md`（P1-MA7-001）
- Skill Selection Basis: 触及 ORM 模型变更（ask-first 保护区域）→ 须 `nop-backend-dev`（ORM 变更 + codegen 增量范式）+ 独立 plan-audit + 人工确认。索引是加性变更（不删列/不改类型/不加唯一约束），风险面最小。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- **回滚策略**：纯加性索引变更，回滚 = git revert 该 ORM `<index>` 块 + `mvn clean install -DskipTests` 再生。无数据迁移、无 schema 破坏性操作。

## Execution Plan

### Phase 1 - 查询模式核实 + 索引无冲突确认

Status: planned
Targets: `module-finance/erp-fin-service/.../ErpFinPostingProcessor.java`；`module-finance/model/app-erp-finance.orm.xml:643-647`
Skill: `nop-backend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 无

- [ ] Proof: grep 确认所有 `(billCode, businessType)` 联合过滤的查询点（`findBillLinks` + `BankReconAdjustmentVoucherBuilder:113-114` + `BadDebtProvisionService:165-166`），证明 (billCode, businessType) 是真实热查询前缀组合（非理论推测）。产出查询点清单 + 行号。
  - Skill: none
- [ ] Proof: 核实 `ErpFinVoucherBillR.<indexes>`（line 643-647）当前仅 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID`，无任何含 billCode/businessType 的现存索引（避免重复建索引）。
  - Skill: none
- [ ] Decision: 索引列序与唯一性。裁决 `(billCode, businessType)` 列序（billCode 选择性更高/查询先导，放首位）+ `unique="false"`（严格非唯一，避免 P0-MA2-018 三重冲突）。确认不删除/不修改既有 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID`。记录 P0-MA2-018 边界区分（本索引=查询加速无约束 vs deferred UK=唯一约束冲突）。
  - Skill: none

Exit Criteria:

- [ ] 查询点清单产出（行号引用），证明 (billCode, businessType) 是真实热路径
- [ ] 现存索引核实（无重复建索引风险）
- [ ] 列序 + 非唯一性 Decision 记录（含 P0-MA2-018 边界区分）

### Phase 2 - ORM 索引落地 + codegen 再生 + owner doc

Status: planned
Targets: `module-finance/model/app-erp-finance.orm.xml:643-647`；`docs/design/finance/posting-log.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 核实 + **人工确认（ORM ask-first）**

- [ ] Add: 在 `ErpFinVoucherBillR` 的 `<indexes>` 块（line 643-647）追加：
  ```xml
  <index name="IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE" unique="false">
      <column name="billCode"/>
      <column name="businessType"/>
  </index>
  ```
  命名对齐现有 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID` 范式。**仅此一处 ORM 变更**，不触列/关系/其他实体。
  - Skill: `nop-backend-dev`
- [ ] Add: 运行 `mvn clean install -DskipTests` 触发 gen-orm.xgen 增量链重新生成。确认生成物（`erp-fin-dao`/`erp-fin-service` 下 `_app.orm.xml` 或等价 DDL 产物）含新索引——不手编生成物。
  - Skill: `nop-backend-dev`
- [ ] Add: owner doc `docs/design/finance/posting-log.md` 增「ErpFinVoucherBillR 索引」注记：业财回链热查询路径 `(billCode, businessType)` 反查 + 过账/红冲性能含义 + 非唯一索引（与 P0-MA2-018 deferred 字面 UK 的边界区分）。
  - Skill: none

Exit Criteria:

- [ ] ORM `<index>` 块落地（xmllint well-formed），`unique="false"`
- [ ] codegen 再生成功，生成物含新索引（grep 确认）
- [ ] owner doc 注记落地

### Phase 3 - 验证 + arm-index 回填 + 日志

Status: planned
Targets: `docs/audits/arm-index.md` §P1 详细清单；`docs/backlog/audit-remediation-roadmap.md` §MR3 R3.6；`docs/logs/2026/07-31.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [ ] Add: arm-index §P1 详细清单 P1-MA7-001「修复状态」回填 `MR3 done (R3.6)`，附索引名 + 非唯一性 + P0-MA2-018 边界区分 + P2-MA7-005 协同注记。
  - Skill: none
- [ ] Add: 更新 roadmap §MR3 R3.6 Status `todo`→`done`；追加 `docs/logs/2026/07-31.md` 条目（R3.6 billR 非唯一索引落地 + codegen 再生 + owner doc）。
  - Skill: none
- [ ] Proof: 一致性复核——grep ORM 新索引存在 + 生成物含新索引 + `unique="false"` 确认（不与 P0-MA2-018 冲突）+ arm-index P1-MA7-001 非裸 todo + roadmap R3.6 done。
  - Skill: none

Exit Criteria:

- [ ] arm-index P1-MA7-001 回填，无裸 todo
- [ ] roadmap R3.6 done；日志条目落地

## Draft Review Record

- Independent draft review iteration 1: accept (task `ses_049146102ffeBID3USTskP0z6W`) — ORM/query 行号实测精确（orm.xml:623-648/643-647 + ErpFinPostingProcessor.java:895-901/922）+ P0-MA2-018 非唯一边界论证成立（arm-index:284 自 adjudicate「非唯一索引仅加速查询不强制唯一」）+ 单一结果表面 + ORM ask-first 人工确认/独立 plan-audit 纪律 + codegen `mvn clean install` 非 `nop-cli gen` + 命名合规。已采纳 1 项非阻塞 polish（businessType 非 mandatory，软化「无 NULL 歧义」为可空性不影响非唯一索引正确性）。

## Closure Gates

- [ ] 范围内行为完成（ORM 非唯一索引落地 + codegen 再生 + owner doc）
- [ ] 相关文档对齐（posting-log.md + arm-index + roadmap + 日志）
- [ ] 已运行验证（`mvn clean install -DskipTests` BUILD SUCCESS + xmllint well-formed + compliance checker 无新增命中 + grep 新索引存在于 ORM 与生成物 + grep `unique="false"`）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P0-MA2-018 字面唯一键 (billCode, businessType, billLineCode)

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 独立 deferred 决议——字面 UK 与红冲（同 billCode 多凭证）/ 多账套（acctSchemaId）/ 软删除（delVersion）三重契约冲突。本 R3.6 非唯一索引严格不施加约束，不触发该冲突。
- Successor Required: `yes`（触发条件 = P0-MA2-018 的方向 A/B/C/D 任一落地后，评估是否需将本非唯一索引升级为含 acctSchemaId 维度的复合唯一约束）

### P2-MA7-005 红冲有界 N+1 批量加载

- Classification: `optimization candidate`
- Why Not Blocking Closure: watch-only——`findBillLinks` 当前逐单调用；批量 `in` 加载优化须改写调用点。本非唯一索引让该优化的前提（单点查询高效）成立，收益最大化留为 successor。
- Successor Required: `yes`（触发条件 = 过账批量场景性能基准显示 N+1 成为主要瓶颈时，改写为批量加载）

## Closure

Status Note: <pending Phase 1–3 完成后填写>

Closure Audit Evidence:

- Auditor / Agent: <pending 独立结束审计子代理（新会话）>
- Evidence: <pending>

Follow-up:

- <非阻塞跟进见 Deferred But Adjudicated（P0-MA2-018 字面 UK / P2-MA7-005 N+1 批量加载）>
