# 2026-07-31-1439-2-r3-6-voucher-billr-bill-code-index R3.6 — ErpFinVoucherBillR 缺 (billCode, businessType) 非唯一索引

> Plan Status: completed
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

Status: completed
Targets: `module-finance/erp-fin-service/.../ErpFinPostingProcessor.java`；`module-finance/model/app-erp-finance.orm.xml:643-647`
Skill: `nop-backend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 无

- [x] Proof: grep 确认所有 `(billCode, businessType)` 联合过滤的查询点（`findBillLinks` + `BankReconAdjustmentVoucherBuilder:113-114` + `BadDebtProvisionService:165-166`），证明 (billCode, businessType) 是真实热查询前缀组合（非理论推测）。产出查询点清单 + 行号。
  - Skill: none
  - **查询点清单（实测行号）**：
    1. `ErpFinPostingProcessor.findBillLinks`（`erp-fin-service/.../service/posting/ErpFinPostingProcessor.java:895-901`）：`and(eq("billCode", billHeadCode), eq("businessType", businessType.name()))` → `dao.findAllByQuery(q)`。被 `markOriginalVoucherReversed`（line 920-922，红冲补标）/ `alreadyPosted` 幂等判定 / 红冲路径反复调用——每次业财过账/红冲触发一次该前缀全表扫描。
    2. `BankReconAdjustmentVoucherBuilder.countAdjustmentLinks`（`.../service/bankrecon/BankReconAdjustmentVoucherBuilder.java:109-116`）：`eq("billCode", billHeadCode)` + `eq("businessType", ErpFinBusinessType.BANK_RECON_ADJ.name())`（line 113-114）→ `dao.findAllByQuery(q).size()`。`hasAdjustmentVoucher`（line 105）每次调节表反查触发。
    3. `BadDebtProvisionService.findUnreversedProvisionVouchers`（`.../service/baddebt/BadDebtProvisionService.java:161-168`）：`and(eq("billCode", billCode), eq("businessType", businessType.name()))`（line 164-167）→ `linkDao.findAllByQuery(q)`。坏账准备反审核路径调用。
- [x] Proof: 核实 `ErpFinVoucherBillR.<indexes>`（line 643-647）当前仅 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID`，无任何含 billCode/businessType 的现存索引（避免重复建索引）。
  - Skill: none
  - **确认**：orm.xml:643-647 `<indexes>` 块内仅 `<index name="IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID" unique="false"><column name="voucherId"/></index>`。实体块（623-648）内无任何含 `billCode` 或 `businessType` 的现存索引。无重复建索引风险。
- [x] Decision: 索引列序与唯一性。裁决 `(billCode, businessType)` 列序（billCode 选择性更高/查询先导，放首位）+ `unique="false"`（严格非唯一，避免 P0-MA2-018 三重冲突）。确认不删除/不修改既有 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID`。记录 P0-MA2-018 边界区分（本索引=查询加速无约束 vs deferred UK=唯一约束冲突）。
  - Skill: none
  - **Decision 记录**：列序 `(billCode, businessType)`——billCode（单据编号，高基数）为查询先导放首位，businessType（枚举，~十余值）次位。`unique="false"` 严格非唯一：仅加速查询，不施加唯一约束，故不触发 P0-MA2-018 字面 UK 的三重契约冲突（红冲同 billCode 多凭证 / 多账套 acctSchemaId / 软删除 delVersion）。既有 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID` 保留不变（voucherId 反查路径仍需）。P0-MA2-018 边界区分：本索引=查询加速无约束（allowed now）vs deferred UK=唯一约束冲突（out-of-scope，独立 deferred 决议）。

Exit Criteria:

- [x] 查询点清单产出（行号引用），证明 (billCode, businessType) 是真实热路径
- [x] 现存索引核实（无重复建索引风险）
- [x] 列序 + 非唯一性 Decision 记录（含 P0-MA2-018 边界区分）

### Phase 2 - ORM 索引落地 + codegen 再生 + owner doc

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml:643-647`；`docs/design/finance/posting-log.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 核实 + **人工确认（ORM ask-first）**

- [x] Add: 在 `ErpFinVoucherBillR` 的 `<indexes>` 块（line 643-647）追加：
  ```xml
  <index name="IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE" unique="false">
      <column name="billCode"/>
      <column name="businessType"/>
  </index>
  ```
  命名对齐现有 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID` 范式。**仅此一处 ORM 变更**，不触列/关系/其他实体。
  - Skill: `nop-backend-dev`
  - **落地**：`module-finance/model/app-erp-finance.orm.xml` `<indexes>` 块新增该 index（现位于 line 647-650），紧跟既有 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID`。xmllint `--noout` well-formed（exit 0；既有 ext:/ui: namespace 警告为全文件基线特征，本变更未新增命名空间前缀）。
- [x] Add: 运行 `mvn clean install -DskipTests` 触发 gen-orm.xgen 增量链重新生成。确认生成物（`erp-fin-dao`/`erp-fin-service` 下 `_app.orm.xml` 或等价 DDL 产物）含新索引——不手编生成物。
  - Skill: `nop-backend-dev`
  - **落地**：`mvn clean install -DskipTests` BUILD SUCCESS（154 reactor，1:35 min）。生成物 `module-finance/erp-fin-dao/src/main/resources/_vfs/erp/fin/orm/_app.orm.xml:859` 含新索引 `IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE unique="false"`（billCode+businessType 列），codegen 增量链成功再生，未手编生成物。
- [x] Add: owner doc `docs/design/finance/posting-log.md` 增「ErpFinVoucherBillR 索引」注记：业财回链热查询路径 `(billCode, businessType)` 反查 + 过账/红冲性能含义 + 非唯一索引（与 P0-MA2-018 deferred 字面 UK 的边界区分）。
  - Skill: none
  - **落地**：新增 `## ErpFinVoucherBillR 索引与过账性能` 节（紧随 §关联穿透），记录：热查询路径三点（findBillLinks / countAdjustmentLinks / findUnreversedProvisionVouchers）+ 两非唯一索引裁决（voucherId 与 billCode+bizType）+ 列序裁决 + 性能含义 + 与 P0-MA2-018 deferred 字面 UK 边界区分（unique=false 不触发三重冲突）+ P2-MA7-005 协同注记。

Exit Criteria:

- [x] ORM `<index>` 块落地（xmllint well-formed），`unique="false"`
- [x] codegen 再生成功，生成物含新索引（grep 确认）
- [x] owner doc 注记落地

### Phase 3 - 验证 + arm-index 回填 + 日志

Status: completed
Targets: `docs/audits/arm-index.md` §P1 详细清单；`docs/backlog/audit-remediation-roadmap.md` §MR3 R3.6；`docs/logs/2026/07-31.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] Add: arm-index §P1 详细清单 P1-MA7-001「修复状态」回填 `MR3 done (R3.6)`，附索引名 + 非唯一性 + P0-MA2-018 边界区分 + P2-MA7-005 协同注记。
  - Skill: none
  - **落地**：arm-index:284 P1-MA7-001 行末单元格 `todo (R3.6)`→`done (R3.6)`；新增 R3.6 闭合回填注记块（索引名 + 非唯一性 + P0-MA2-018 边界区分 + P2-MA7-005 协同 + owner doc 指针 + 验证 + successor），对齐 R3.5 backfill 范式。
- [x] Add: 更新 roadmap §MR3 R3.6 Status `todo`→`done`；追加 `docs/logs/2026/07-31.md` 条目（R3.6 billR 非唯一索引落地 + codegen 再生 + owner doc）。
  - Skill: none
  - **落地**：roadmap:204 R3.6 行 Status `todo`→`done` + done 注记（3 Phase 摘要 + 验证 + successor）；`docs/logs/2026/07-31.md` 首部追加 R3.6 日志条目（任务/Phase 1-3/关键决策/验证/Follow-up）。
- [x] Proof: 一致性复核——grep ORM 新索引存在 + 生成物含新索引 + `unique="false"` 确认（不与 P0-MA2-018 冲突）+ arm-index P1-MA7-001 非裸 todo + roadmap R3.6 done。
  - Skill: none
  - **复核结果**（全 pass）：(1) ORM 源 `app-erp-finance.orm.xml:647` 含新索引；(2) 生成物 `_app.orm.xml:859` 含新索引；(3) 生成物 `unique="false"` 确认（billCode+businessType 列，非 P0-MA2-018 唯一约束）；(4) arm-index P1-MA7-001 = `done (R3.6)`（非裸 todo）；(5) roadmap R3.6 = `done`。

Exit Criteria:

- [x] arm-index P1-MA7-001 回填，无裸 todo
- [x] roadmap R3.6 done；日志条目落地

## Draft Review Record

- Independent draft review iteration 1: accept (task `ses_049146102ffeBID3USTskP0z6W`) — ORM/query 行号实测精确（orm.xml:623-648/643-647 + ErpFinPostingProcessor.java:895-901/922）+ P0-MA2-018 非唯一边界论证成立（arm-index:284 自 adjudicate「非唯一索引仅加速查询不强制唯一」）+ 单一结果表面 + ORM ask-first 人工确认/独立 plan-audit 纪律 + codegen `mvn clean install` 非 `nop-cli gen` + 命名合规。已采纳 1 项非阻塞 polish（businessType 非 mandatory，软化「无 NULL 歧义」为可空性不影响非唯一索引正确性）。

## Closure Gates

- [x] 范围内行为完成（ORM 非唯一索引落地 + codegen 再生 + owner doc）
- [x] 相关文档对齐（posting-log.md + arm-index + roadmap + 日志）
- [x] 已运行验证（`mvn clean install -DskipTests` BUILD SUCCESS + xmllint well-formed + compliance checker 无新增命中 + grep 新索引存在于 ORM 与生成物 + grep `unique="false"`）
- [x] 无范围内项目降级为 deferred/follow-up（P0-MA2-018 / P2-MA7-005 为既有独立 Deferred，非本计划范围内项目降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

> **Closure Gate 诚实注记（gate 7）**：本计划由 MISSION_DRIVER EXECUTE 通道执行（执行者未自我审计该 gate，保留 `[ ]` 为职责边界诚实标记）；随后由独立 closure-audit 子代理（新会话、fresh 上下文、未复用执行者上下文）执行结束审计，逐项核验活仓库证据后勾选 gate 7 `[x]`（详见 §Closure Audit Evidence）。

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

Status Note: <Phase 1–3 全 done（2026-07-31）>。3 Phase 全部完成：Phase 1 grep 实测三处 (billCode, businessType) 联合过滤查询点 + 核实既有索引仅 voucherId 无重复风险 + Decision（列序 billCode 先导 + `unique="false"` 严格非唯一，P0-MA2-018 边界区分）；Phase 2 ORM `<indexes>` 落地（xmllint well-formed）+ `mvn clean install -DskipTests` BUILD SUCCESS codegen 增量再生（生成物 `_app.orm.xml:859` 含新索引 `unique="false"` grep 确认）+ owner doc posting-log.md 新增「ErpFinVoucherBillR 索引与过账性能」节；Phase 3 arm-index P1-MA7-001 `done (R3.6)` 回填 + roadmap R3.6 `done` + 日志 + 一致性复核全 pass。验证：xmllint well-formed + `mvn clean install -DskipTests` BUILD SUCCESS（154 reactor）+ finance `mvn test` 306 测试全绿（0 failures / 0 errors）+ compliance checker 无新增命中（纯 ORM 索引元数据零 Java 逻辑变更）。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure-audit 子代理（新会话、fresh 上下文，未复用 MISSION_DRIVER EXECUTE 执行者上下文），任务 `MISSION_DRIVER:2026-07-31-133020-mission-driver`。
- Evidence:
  - **ORM 源**：`module-finance/model/app-erp-finance.orm.xml:647-650` `<index name="IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE" unique="false">`（billCode+businessType），与既有 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID` 并列。xmllint `--noout` exit 0 well-formed。
  - **codegen 生成物**：`module-finance/erp-fin-dao/src/main/resources/_vfs/erp/fin/orm/_app.orm.xml:859-862` 含新索引 `unique="false"`（`mvn clean install -DskipTests` BUILD SUCCESS 154 reactor 触发 gen-orm.xgen 增量再生，未手编生成物、未重跑 `nop-cli gen`）。
  - **owner doc**：`docs/design/finance/posting-log.md` 新增 `## ErpFinVoucherBillR 索引与过账性能` 节（热查询路径三点 + 两非唯一索引裁决 + 列序裁决 + 性能含义 + P0-MA2-018 边界区分 + P2-MA7-005 协同）。
  - **arm-index**：`docs/audits/arm-index.md:284` P1-MA7-001 = `done (R3.6)` + R3.6 闭合回填注记块。
  - **roadmap**：`docs/backlog/audit-remediation-roadmap.md:204` R3.6 = `done` + done 注记。
  - **日志**：`docs/logs/2026/07-31.md` 首部 R3.6 条目。
  - **验证**：finance `mvn test -pl module-finance/erp-fin-service` Tests run: 306, Failures: 0, Errors: 0；compliance checker 无新增命中（纯 ORM 索引元数据零 Java 逻辑变更）。
  - **一致性复核**：ORM 新索引 + 生成物含新索引 + `unique="false"`（非 P0-MA2-018 冲突）+ arm-index 非裸 todo + roadmap done，五项 grep 全 pass。
  - **独立审计复核（本次 closure-audit）**：逐项核验活仓库：(1) ORM 源 line 647-650 索引存在且 `unique="false"` + 列序 (billCode, businessType) 与既有 voucherId 索引并列；(2) codegen 生成物 line 859-862 同索引再生确认（增量链成功，零手编）；(3) owner doc posting-log.md:56-67 节内容完整（热路径三点 + 两非唯一索引 + 列序 + 性能含义 + P0-MA2-018 边界区分 + P2-MA7-005 协同）；(4) arm-index:284 P1-MA7-001 = `done (R3.6)` + 闭合回填块；(5) roadmap:204 R3.6 = `done`；(6) log 07-31.md R3.6 条目首部存在。**Anti-Hollow**：索引为真实 ORM 元数据经 codegen 写入 DDL 产物，运行时由 DB 查询规划器使用（非孤立/占位）。**五点一致性**：Plan Status completed / 3 Phase 全 completed / 全 Exit Criteria `[x]` / Closure Gates 全 `[x]` / 日志一致。**Deferred 诚实**：P0-MA2-018 与 P2-MA7-005 为既有独立 Deferred（含 successor 触发条件），无范围内缺陷降级隐藏。**结论**：approves closure。

Follow-up:

- <非阻塞跟进见 Deferred But Adjudicated（P0-MA2-018 字面 UK / P2-MA7-005 N+1 批量加载）>
