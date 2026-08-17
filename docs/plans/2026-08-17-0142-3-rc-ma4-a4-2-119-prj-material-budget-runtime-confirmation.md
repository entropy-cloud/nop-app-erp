# 2026-08-17-0142-3-rc-ma4-a4-2-119-prj-material-budget-runtime-confirmation A4.2.119 — 采购路径物料归集 budgetChecker.check 接入运行时确认（P1-RC-049 successor 落地后回队探查）

> Plan Status: active
> Last Reviewed: 2026-08-17
> Mission: requirement-compliance
> Work Item: A4.2.119（A1.35 SP-4，P1-RC-049 successor 落地后采购路径 budgetChecker.check 接入运行时行为）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MA4 A4.2.119 行（:306，**ready——2026-08-16 RC-R1.61 落地解锁回队**）+ `docs/audits/2026-08-05-2200-3-rc-ma1-a1-35-projects-f2-budget-dag.md`（A1.35 §7 SP-4 :258）+ `docs/audits/2026-08-07-2359-rc-ma4-a4-2-113-123-projects-f1-f2-f3-runtime.md`（§A4.2.119 MR1-successor 阻塞排除 :26）
> Related: `docs/plans/2026-08-16-2043-1-rc-mr1-r1-61-prj-material-subcontract-aggregation.md`（RC-R1.61 落地：物料归集 + budgetChecker.check 接线 + 测试组）；`docs/design/projects/cost-collection.md`（§3.3 预算检查时机）；`docs/audits/arm-index.md` P1-RC-049/P1-RC-051 行（:220/:223）
> Audit: required

## Current Baseline

- **存疑点（A1.35 §7 SP-4，:258）**：「采购路径物料归集实现后 budgetChecker.check 接入的实际运行时行为（P1-RC-049 successor 落地后，采购入库→项目物料归集同步接 budgetChecker.check 应阻断超预算采购；与 P2-RC-049 承诺项协同）」。SP-4 行的运行时验证设计（同 :258 第三列）：「届时构造采购入库→项目归集（订单行标 projectId）+ 超 budget → 断言 budgetChecker.check 是否阻断 + STRICT 模式是否抛 ERR_BUDGET_EXCEEDED」。
- **MR1 successor 阻塞（原 todo，2026-08-07 MA4 报告 :26）**：P1-RC-049 物料归集未落地致运行时探查结构不可达（与 A4.2.3/A4.2.79 MR1 阻塞先例同型），保留 todo 待 MR1 回队。
- **2026-08-16 RC-R1.61 落地解锁（roadmap :306 注记）**：物料归集经 `ErpPrjCostCollectionAggregateMaterialCostProcessor` 接 `budgetChecker.check`（STRICT 抛 ERR_BUDGET_EXCEEDED / WARNING 放行，`TestErpPrjMaterialAggregation` STRICT/WARNING 两组实证）→ **本行解锁回队，由 mission driver 按运行时探查立项**。
- **RC-R1.61 既有证据（plan 2026-08-16-2043-1 实施记录）**：
  - `TestErpPrjMaterialAggregation` 8 组全绿——④预算 STRICT 超预算抛 `ERR_BUDGET_EXCEEDED`（拒绝不写入）+ WARNING 放行写入（P1-RC-051 采购路径 merge 双态实证，projects 侧 Facade 层）。
  - `TestErpPurReceiveMaterialCostAggregation` 4 组全绿——①订单行标 projectId → 入库审核（`ErpPurReceive__submitForApproval` + `ErpPurReceive__approve` **GraphQL 引擎集成层**）后归集行生成（costCategory=MATERIAL）；②projectId null 行级跳过；③**STRICT 预算超限（预算 30 < 行金额 50）→ approve 返回 ERR_BUDGET_EXCEEDED + 入库单保持 SUBMITTED**（L1 UC-PRJ-04「采购审核拒绝该笔归集」）；④config 关闭（`erp-prj.material-aggregation-enabled=false`）→ 审核正常 + 零归集行。
  - 守卫链（生产代码）：config 门控 → `IErpPrjProjectBiz.requireReferenceable` 单一咽喉 → `budgetChecker.check`（归集行写入前）→ 聚合器幂等写入。
- **测试基线**：erp-prj-service **158 tests 全绿**（R1.60 基线——138 R1.27 + 8 R1.61 + 4 R1.62 + 8 R1.60）；`TestErpPrjMaterialAggregation` 8 组在 **erp-prj-service**（STRICT/WARNING 双态 + requireReferenceable + config 门控），`TestErpPurReceiveMaterialCostAggregation` 4 组在 **erp-pur-service**（GraphQL 引擎集成层 approve 拒绝 + 行级跳过 + config 关闭）；全量 `mvn test` + `mvn clean install -DskipTests` 通过（R1.61 结束审计证据）；compliance 历史（权威源 `docs/audits/compliance-baseline.md` §R2c 增量注记 :455/:471 + §BASELINE 块 :438）R2c **1422→1431**（R1.61 +9，MaterialCostAggregator 9 站点）→**1431**（R1.62 零漂移）→**1433**（R1.60 +2，CostRateResolver 2 站点），R2b=235 / R2d=35。
- **先例**：A4.2.3（mfg 预留写路径运行时确认：既有断言复跑 + 新增跨工单并发探针 + 证据链落盘 + 全量构建通过 + roadmap done）、A4.2.79（inv 效期拦截：既有断言 + 新增既有预占边界探针 + 证据链落盘 + roadmap done）——本行镜像同型流程，**但探针新增按 Phase 1 判定门控**（SP-4 场景已由 `TestErpPurReceiveMaterialCostAggregation#testStrictBudgetRejectsApproval` 预覆盖，与 A4.2.3 当时场景无预覆盖的先例不同，见 Phase 2）。

## Goals

- **A4.2.119 运行时确认闭环（MA4 行 → done）**：按 A4.2.3/A4.2.79 先例对 SP-4 存疑点做运行时行为确认，输出证据链——采购入库→项目归集链上 `budgetChecker.check` 接入的**实际运行时行为**（STRICT 阻断 + WARNING 放行 + config 门控 + 失败事务语义），并评估既有 R1.61 测试证据是否完整覆盖存疑点（断言强度复核 + 缺口补探针）。
- **证据链落盘**：报告 `docs/audits/2026-08-17-*-rc-ma4-a4-2-119-prj-material-budget-runtime.md`——复核记录 + 新增探针（如有）+ 裁决（维持接受/新 finding 登记，零生产代码变更）。
- **回填**：roadmap A4.2.119 → done ✅（回队执行记录）+ arm-index P1-RC-049/P1-RC-051 行追加 A4.2.119 运行时确认注记（如存在既有注记则追加层级）+ 本日志条目。
- **零回归**：涉及测试重跑（erp-prj-service + erp-pur-service）+ 全量 `mvn test` 绿色保持（如有新增探针）。

## Non-Goals

- **不修改生产代码**（纯验证/审计工作：只读 + 测试探针；若探针发现运行时漂移 → 登记新 finding 归 MR1/MR0 通道，本计划不修复）。
- **不重审 P1-RC-049 已闭合行为**（物料归集实现正确性已由 RC-R1.61 结束审计闭环，本行仅补「预算检查接入」存疑点维度）。
- **不覆盖 P2-RC-049 承诺项**（watch-only successor，触发条件未达——projects 域无生产承诺源）。
- **不改真相源契约段落**（use-cases L1 / cost-collection.md 契约段不动）。

## Task Route

- Type: `verification or audit work`（MA4 运行时行为确认，MR1 successor 落地后回队探查）
- Owner Docs: `docs/design/projects/cost-collection.md`（§3.3 预算检查时机）+ `docs/design/projects/use-cases.md`（L1 UC-PRJ-04）+ `docs/plans/2026-08-16-2043-1-rc-mr1-r1-61-prj-material-subcontract-aggregation.md`（实现记录）
- Skill Selection Basis: 运行时探针与断言复核（`nop-testing`：GraphQL 引擎集成层 + JunitBaseTestCase）；审计证据链规范（`docs/skills/multi-dimensional-audit-prompt.md` 定制为单存疑点运行时确认模板，对齐 A4.2.3/A4.2.79 报告范式）。

## Infrastructure And Config Prereqs

- 无新 infra/config/ORM 变更（纯验证）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-projects/erp-prj-service,module-purchase/erp-pur-service`。

## Execution Plan

### Phase 1 - 既有证据复核（Proof）

Status: planned
Targets: `module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjMaterialAggregation.java`（只读复核）、`module-purchase/erp-pur-service/src/test/.../TestErpPurReceiveMaterialCostAggregation.java`（只读复核）、生产代码守卫链（只读）
Skill: `nop-testing`
Item Types: `Proof`
Prereqs: 无

- [ ] Proof：逐组复核 R1.61 既有测试断言强度——①STRICT 组是否断言 errorCode 精确比对 + 零归集行落库 + 入库单状态回滚（SUBMITTED 保持）；②WARNING 组是否断言放行 + 归集行落库 + 审核成功；③config 关闭组是否断言零副作用；④测试是否经生产 Facade/GraphQL 集成层（非 mock）——核对覆盖是否完整满足 SP-4 存疑点（"STRICT 模式是否抛 ERR_BUDGET_EXCEEDED" + "是否阻断超预算采购"）。
      - Skill: `nop-testing`
- [ ] Proof：重跑两组测试确认实仓绿色（`mvn test -pl module-projects/erp-prj-service,module-purchase/erp-pur-service` 相关类），记录实测输出。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 既有证据断言强度逐组核对完成 + 实仓重跑绿色；明确证据缺口清单（如有）
- [ ] 证据缺口（如有）确认归属：补探针（Phase 2）或显式裁决「既有覆盖已充分」直接进 Phase 3

### Phase 2 - 缺口补探针（Phase 1 判定门控）（Add | Proof）

Status: planned
Targets: `module-projects/erp-prj-service/src/test/`（或 `module-purchase/erp-pur-service/src/test/`）
Skill: `nop-testing`
Item Types: `Add | Proof`
Prereqs: Phase 1（**仅在 Phase 1 判定存在证据缺口时执行**；SP-4 运行时设计场景已由 `TestErpPurReceiveMaterialCostAggregation#testStrictBudgetRejectsApproval`（GraphQL 集成层 approve 拒绝 + ERR_BUDGET_EXCEEDED + SUBMITTED 保持 + 零归集行）预覆盖，预期无缺口 → 本段标 N/A 并记录理由——与 A4.2.3 当时场景无预覆盖故无条件新增探针的先例不同，此为显式分歧记录）

- [ ] Proof：按 Phase 1 缺口清单补探针（候选：WARNING 模式采购链路运行时放行 + 归集行落库端到端断言 / STRICT 拒绝后重试审核在预算放宽后的恢复路径 / requireReferenceable 非 OPEN 项目前置拒绝 / 事务回滚后移动单与入库单状态一致性）。
      - Skill: `nop-testing`
- [ ] Proof：新探针全绿 + 既有测试零回归（erp-prj-service + erp-pur-service 分域测试）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 探针（如有）全绿 + 零回归；证据链完整覆盖 SP-4 存疑点

### Phase 3 - 证据链落盘 + 回填（Proof | Add）

Status: planned
Targets: `docs/audits/2026-08-17-*-rc-ma4-a4-2-119-prj-material-budget-runtime.md`（新报告）、`docs/backlog/requirement-compliance-roadmap.md`（A4.2.119 行）、`docs/audits/arm-index.md`（P1-RC-049/P1-RC-051 行注记）、`docs/logs/2026/08-17.md`
Skill: `multi-dimensional-audit-prompt`（定制为单存疑点运行时确认模板，对齐 A4.2.3/A4.2.79 报告范式）
Item Types: `Proof | Add`
Prereqs: Phase 1（+ Phase 2 如执行）

- [ ] Add：证据链报告落盘——存疑点原文 → 既有证据复核结果 → 新探针（如有）实测输出 → 裁决（维持接受 / 新 finding 分级登记归 MR1/MR0）→ 过程纪律自检（checker 门控 + 独立性声明 + 与 arm-index 交叉去重）。
      - Skill: `multi-dimensional-audit-prompt`
- [ ] Add：回填——roadmap A4.2.119 → done ✅（回队执行记录：R1.61 解锁 + 探查结论）；arm-index P1-RC-049/P1-RC-051 行追加 A4.2.119 运行时确认注记；本日志条目。
      - Skill: `none`

Exit Criteria:

- [ ] 证据链报告 + 回填完成（roadmap done / arm-index 注记 / logs 条目一致）

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (ses_ff452042cffesyDaKP2rRQV1zG) because **Major M1**：Closure Gate 全量构建跳过条款过宽（探针属测试代码变更仍可能跳过全量构建），违反指南（docs-only 例外仅适用零代码变更）。已修订：Closure Gates 改双分支裁决（探针落地→全量验证+checker 对齐 A4.2.3；零代码变更→本地化重跑+checker N/A 注记）。6 Minor 已就地折叠：M2 compliance 历史 1429 修正；M3 测试模块归属区分（8 组 prj-service / 4 组 pur-service）+ 158=138+8+4+8 算术；M4 先例引用改显式分歧记录（SP-4 场景已预覆盖）；M5 Phase 2 更名「Phase 1 判定门控」+ N/A-with-reason；M6 checker 复跑或 N/A 注记；M7 Phase 3 Skill 改 `multi-dimensional-audit-prompt`。
- Independent draft review iteration 2: **accept** (ses_ff4463133ffePaZwcQyWMuavgN) because M1 + M2-M7 全部实仓核验折叠落地 + 双分支互斥穷尽（Phase 2 N/A ⇒ 零代码变更）+ checker 仅扫描生产代码判断准确（:50-53/:57-62 实证）+ 探针落点实仓确认（两测试类存在 + testStrictBudgetRejectsApproval）。3 余 Minor 已就地折叠：①compliance 归属改权威源链（见 iter-3 修正）；②SP-4 运行时设计引用 :284→:258（同行第三列）；③Draft Review Record 填充。
- Independent draft review iteration 3: **accept** (ses_ff43d49dfffemjPFgaCGWGupfM) after compliance 归属修正——iter-3 指正：R1.61 的 R2c 增量权威值为 **1422→1431（+9，compliance-baseline.md:455，9 站点）** 非 1429/+7（roadmap 行 :453 与权威基线文档不一致，以 compliance-baseline.md 为准）；R1.62 为**零漂移**（R2c=1431 不变，roadmap :454）非 +2；R1.60 = 1431→1433（+2，compliance-baseline.md:471）。已修正为「1422→1431（R1.61 +9）→1431（R1.62 零漂移）→1433（R1.60 +2）」。
- Independent draft review iteration 4: **accept** (ses_ff42dbe00ffeynni6rCHaufmQK) because compliance 归属修正逐项对照权威源核验通过（compliance-baseline.md:455/:471/:438 + roadmap :454）+ Draft Review Record iter-3 记录与修正一致 + 无残留陈旧值 + 支持性检查全过（roadmap A4.2.119 ready / arm-index 行 / 158=138+8+4+8 / 测试模块归属）+ 无剩余问题。

## Closure Gates

- [ ] 范围内行为完成（存疑点运行时确认 + 证据链 + 回填）
- [ ] 相关文档对齐（报告 + arm-index + roadmap + logs）
- [ ] 已运行验证——按代码变更面裁决：(a) **探针落地（Phase 2 执行）** → 全量 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`（对齐 A4.2.3 先例全量构建通过 + checker 零漂移/登记）；(b) **零代码变更（仅文档，Phase 2 N/A）** → 仅分域 `mvn test -pl module-projects/erp-prj-service,module-purchase/erp-pur-service` 重跑 + checker 注明「N/A：零生产代码变更」（checker 仅扫描生产代码，漂移结构性不可能；探针属测试代码变更故按 (a) 全量）——理由记录于 Closure
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P2-RC-049 承诺项（预算余量公式第三项）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本行存疑点不覆盖承诺项；P2 登记项 + Q4 张力已声明（A1.35 §4/A4.2.119 行注记）；projects 域无生产承诺源致第三项实际为零
- Successor Required: `yes`（触发条件：物料归集后 commitment 源产生时，与 P1-RC-051 采购路径协同补全三项式——既有登记，本行不重开）

### 探针发现的运行时漂移（如有）

- Classification: 按分级裁决（P0 即时通道 MR0 / P1 归 MR1 展开器 / P2 watch-only）
- Why Not Blocking Closure: 本计划为纯验证；发现漂移即登记并显式交接，不本计划修复
- Successor Required: `yes`（触发条件：探针发现漂移且分级 ≥ P1）

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
