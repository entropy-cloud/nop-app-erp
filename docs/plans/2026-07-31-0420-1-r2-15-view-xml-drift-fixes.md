# 2026-07-31-0420-1-r2-15-view-xml-drift-fixes R2.15 view.xml 契约 drift 修复（mfg/pur/hr 三项）

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR2 R2.15（P1-MA4-023/024/025）
> Related: `docs/audits/2026-07-29-0430-arm-ma4-finance-mfg-view-xml-drift.md`（A4.6）、`docs/audits/2026-07-29-0749-arm-ma4-pur-sal-inv-view-xml-drift.md`（A4.7）、`docs/audits/2026-07-29-0749-arm-ma4-crm-hr-view-xml-drift.md`（A4.8）
> Audit: required（roadmap 明令 view.xml 代码变更须独立 plan-audit + 独立 closure audit）

## Current Baseline

MA4 view.xml drift 维度（A4.6/A4.7/A4.8 三批共 388 view.xml）全域收口，累计 3 项 P1（零 P0），drift 密度 3/388 ≈ 0.77%。本计划修复这 3 项 P1，全部为保留层（非 `_gen`）delta 自定义 view.xml 中的契约 drift，无活跃数据破坏（最坏为按钮可见性/功能失效/PII 掩码渲染失效）。

逐项实测基线（grep 确认，文件均为保留层源文件）：

- **P1-MA4-023（mfg）**：`module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml:234` `<visibleOn>${docStatus == 'STARTED' || docStatus == 'COMPLETED'}</visibleOn>` 引用 `STARTED`——但 dict `erp-mfg/work-order-status`（10 态 DRAFT/SUBMITTED/NOT_STARTED/IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/COMPLETED/STOPPED/CLOSED/CANCELLED，**无 STARTED**）+ `ErpMfgWorkOrderProcessor.start()`→IN_PROCESS（`:136`）/`stop()`→STOPPED（`:128`）均无 STARTED。grep 全 mfg 代码/dict/constants 零 `STARTED` 定义。后果：IN_PROCESS/STOPPED 工单的「结案」按钮被隐藏（close mutation `ErpMfgWorkOrder__close?workOrderId=$id` 签名匹配 BizModel，仅 visibleOn 状态值错误）。同文件 `:111` 进度 badge 也引用 `STARTED`，属 P2-MA4-014 同型（系统性 ACTIVE/STARTED 死状态 badge），但 P2 不在本 P1 修复范围。
- **P1-MA4-024（pur）**：`module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurRfq/ErpPurRfq.view.xml:92` `<api url="@mutation:ErpPurRfq__cancel?id=$id"/>` 传参数名 `id`，而 `ErpPurRfqBizModel.java:22` 签名为 `cancel(@Name("rfqId") Long rfqId, IServiceContext context)`。零 xbiz cancel adapt → `rfqId` 收到 null → 实体未找到报错。purchase 其余 7 域 cancel 参数名（orderId/receiveId/...）与各自 BizModel `@Name` 全匹配，唯 Rfq 用裸 `id` 漂移。后果：作废按钮功能性失效。
- **P1-MA4-025（hr）**：`module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ErpHrEmployee.view.xml:19,150,157,164` 4 处 gen-control `<c:script>` 内 `tpl` 使用 `${LEFT(field, n)}` / `${RIGHT(field, n)}`——AMIS/Nop `${...}` tpl 表达式按 **JavaScript** 语义求值，`LEFT`/`RIGHT` 是 SQL/Excel 函数非 JS 内置。全仓 grep（排除 `_gen`/`target`）确认仅此一处使用此语法；JS 原生 `String(x).slice()` 是与 AMIS `${}` JS 表达式语义一致的平台通用做法。后果：银行账号/身份证号/手机号 4 处 PII 掩码不按预期渲染。`_gen/_ErpHrEmployee.view.xml` 不含 PII 掩码（delta 自定义 gen-control），无同型风险。

剩余差距：上述 3 处 view.xml 契约 drift 待修复。修复均在保留层 view.xml，不触及 `_gen` 生成物、不触及 ORM 模型、不触及生产 Java 代码（仅 view.xml 文本）。

## Goals

- 修复 3 项 P1 view.xml 契约 drift，使前端 view.xml 与后端 dict/BizModel 签名/JS 运行时契约一致
- P1-MA4-023：mfg WorkOrder「结案」按钮 visibleOn 状态值对齐 dict 实际写入值（IN_PROCESS，按裁决决定是否含 STOPPED）
- P1-MA4-024：pur Rfq「作废」按钮参数名 `id` → `rfqId` 对齐 BizModel `@Name`，作废按钮恢复功能
- P1-MA4-025：hr Employee 4 处 PII 掩码从 `LEFT()`/`RIGHT()` 改为 JS `String(x).slice()`，掩码恢复渲染

## Non-Goals

- 不修复 P2-MA4-014（mfg 进度 badge ACTIVE/STARTED 死状态，系统性 P2 watch-only）——属 P2 非本 P1 范围
- 不修复其他 P2 view.xml drift（P2-MA4-015~020 跨域调色板/拼写，全为 watch-only）
- 不修改后端 BizModel/Processor/xbiz 签名（view 侧对齐风险更低，P1-MA4-024 裁决为 view 侧修复）
- 不修改 ORM 模型或生成代码
- 不补 view.xml drift 的 E2E 测试覆盖（E2E 回归归 MV V.3）；本计划验证以契约一致性 + xmllint + compliance + 全量 build 为准

## Task Route

- Type: `implementation-only change`（view.xml 文本修复，无契约/模型/API 变更，但属代码变更须独立 plan-audit）
- Owner Docs: `docs/design/manufacturing/state-machine.md`（WorkOrder 状态机，结案守卫）、`docs/design/purchase/state-machine.md`（Rfq）、`docs/design/human-resource/`（Employee）。owner-doc 对齐属计划级结束步骤（本计划修改 view.xml 可见性逻辑，须核验 mfg state-machine 结案守卫一致）
- Skill Selection Basis: 工作方法为保留层 view.xml delta 编辑 → `nop-frontend-dev`（AMIS view.xml 三层模型、保留层 vs `_gen`、bounded-merge）。保留层 view.xml 非 Delta 覆盖平台页（是域自有生成 view 的保留层定制），核心是定位保留层源文件并精准编辑 visibleOn/api url/tpl 表达式

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 view.xml 文本编辑，无端口/环境变量/外部服务）

## Execution Plan

### Phase 1 - 三项 view.xml 契约 drift 修复

Status: completed
Targets: `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml`、`module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurRfq/ErpPurRfq.view.xml`、`module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ErpHrEmployee.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Fix | Decision | Proof`
- Prereqs: R2.0 done（已 done）

- [x] Decision: P1-MA4-023 结案按钮 visibleOn 应含的状态值集合
  - 调查 `ErpMfgWorkOrderProcessor.close()` 的状态守卫（实测：`close()` 仅允许 STOPPED + IN_PROCESS，否则抛错「STOPPED 或 IN_PROCESS」），以及 `docs/design/manufacturing/state-machine.md` 结案迁移定义
  - 预期裁决 = 候选 B：`IN_PROCESS` + `STOPPED`（与 close 守卫精确对齐）；当前 visibleOn `STARTED || COMPLETED` 中 STARTED 为死值、COMPLETED 实际被守卫拒绝——修复一并消除该潜在不一致
  - 决策记录选择 + 理由，与 close 守卫对齐（view 可见性不得暴露 Processor 实际拒绝的状态）；执行时复核守卫确认无变更
  - Skill: `nop-frontend-dev`
- [x] Fix: P1-MA4-023 — `ErpMfgWorkOrder.view.xml:234` visibleOn 的 `'STARTED'` 替换为 Decision 裁决的状态值（`IN_PROCESS` 或 `IN_PROCESS`+`STOPPED`），对齐 dict `erp-mfg/work-order-status` 实际值
  - Skill: `nop-frontend-dev`
- [x] Fix: P1-MA4-024 — `ErpPurRfq.view.xml:92` 的 `?id=$id` 改为 `?rfqId=$id`（与其余 7 域命名参数一致，对齐 `ErpPurRfqBizModel.cancel(@Name("rfqId"))`）
  - Skill: `nop-frontend-dev`
- [x] Fix: P1-MA4-025 — `ErpHrEmployee.view.xml:19,150,157,164` 4 处 `${LEFT(field,n)}`/`${RIGHT(field,n)}` 改为 JS：
  - `:19,164` `'****${RIGHT(bankAccountId, 4)}'` → `'****' + String(bankAccountId).slice(-4)`
  - `:150` `'${LEFT(idCardNo, 1)}******${RIGHT(idCardNo, 4)}'` → `String(idCardNo).slice(0,1) + '******' + String(idCardNo).slice(-4)`
  - `:157` `'${LEFT(mobilePhone, 3)}****${RIGHT(mobilePhone, 4)}'` → `String(mobilePhone).slice(0,3) + '****' + String(mobilePhone).slice(-4)`
  - 与全仓其他掩码（`ErpFinVoucher.view.xml` 等 `String(x).slice()`）一致；核验 gen-control `<c:script>` 返回的 tpl 语义（确认是 JS 拼接而非 `${}` 模板求值）
  - Skill: `nop-frontend-dev`
- [x] Proof: xmllint well-formed 校验 3 个修改文件通过
  - `xmllint --noout <3 个 view.xml>`
  - Skill: none
- [x] Proof: compliance checker 零新增命中（纯 view.xml 文本变更，预期零命中）
  - `bash docs/audits/nop-compliance-checker.sh`
  - Skill: none

Exit Criteria:

> view.xml 修复属代码变更但无运行时业务行为变更（仅前端可见性/参数名/掩码渲染契约对齐）。此阶段交付的可观察结果 = 3 处 view.xml 文本与后端契约一致。

- [x] 3 处 view.xml visibleOn/api url/tpl 表达式与后端 dict/BizModel 签名/JS 运行时契约一致（grep 复核：mfg view 无残留 `STARTED` 引用 / pur Rfq cancel 用 `rfqId` / hr Employee 无残留 `LEFT(`/`RIGHT(`）
- [x] xmllint well-formed 校验 3 文件通过

## Draft Review Record

- Independent draft review iteration 1: accept (ses_04b0200abffeRodOhJy4olUer5) — baseline 逐项 grep 实测精确匹配（mfg `:234` STARTED / pur `:92` id / hr `:19,150,157,164` LEFT-RIGHT / close 守卫实测仅允许 STOPPED+IN_PROCESS 印证候选 B / dict 10 态无 STARTED / `_gen` 不含 PII 掩码）；范围精确锁定 R2.15 三项 P1；无阻塞项。已采纳非阻塞改进：修正 ErpFinVoucher 引用（非 PII 掩码范例）、P2-MA4-014 命名 successor 触发、Decision 预裁决候选 B（守卫已证实）。草案审查收敛，可开始实施。

## Closure Gates

> 纯 view.xml 文本变更，无生产 Java 代码/ORM 变更。完整仓库 build 验证在此处一次。

- [x] 范围内行为完成（3 项 P1 view.xml drift 全部修复）
- [x] 相关文档对齐（若 Decision 改变结案可见性集合，核验 `docs/design/manufacturing/state-machine.md` 结案迁移描述一致——仅当 Decision 选择含 STOPPED 且 owner doc 未提及 STOPPED 可结案时更新）
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿（156 模块 BUILD SUCCESS / 0 ERROR）+ `bash docs/audits/nop-compliance-checker.sh` 零新增命中（exit 0；全为 pre-existing Java 基线）+ xmllint 3 文件 well-formed 通过
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P2-MA4-014 mfg 进度 badge STARTED/ACTIVE 死状态（系统性）

- Classification: `watch-only residual`
- Why Not Blocking Closure: P2 级别（非 P1），系统性跨域调色板/死状态 badge 族（P2-MA4-014~020），归 MV/G 知识沉淀而非本 P1 修复批次
- Successor Required: `no`（P2 watch-only；当未来 P2 badge/polish 批次[ P2-MA4-014~020 ]被 scope 时重新打开）

## Closure

Status Note: 执行完成（2026-07-31）。3 项 P1 view.xml drift 全部修复并验证通过：
- P1-MA4-023：`ErpMfgWorkOrder.view.xml:234` 结案按钮 visibleOn `STARTED||COMPLETED` → `IN_PROCESS||STOPPED`。**Decision B 采纳**（与 `ErpMfgWorkOrderProcessor.close()` 守卫精确对齐——守卫仅允许 STOPPED+IN_PROCESS 否则抛「STOPPED 或 IN_PROCESS」；与 `docs/design/manufacturing/state-machine.md` §2 迁移表 STOPPED→CLOSED / IN_PROCESS→CLOSED 一致）。原 STARTED 为 dict 死值、COMPLETED 实被守卫拒绝，修复一并消除该潜在不一致。owner-doc 无需变更（state-machine.md 已文档化 STOPPED/IN_PROCESS 两结案来源）。
- P1-MA4-024：`ErpPurRfq.view.xml:92` cancel `?id=$id` → `?rfqId=$id`，对齐 `ErpPurRfqBizModel.cancel(@Name("rfqId"))`。
- P1-MA4-025：`ErpHrEmployee.view.xml:19,150,157,164` 4 处 `${LEFT/RIGHT(...)}` → `${ String(x).slice(...) }`（保留 `${}` 渲染期求值——gen-control 仅有元数据作用域，按全仓 badge 范式 `${}` 由 AMIS 按行解析；SQL LEFT/RIGHT 在 JS 语义未定义，String.slice 为原生 JS）。
验证：`mvn clean install -DskipTests` BUILD SUCCESS（156 模块 / 0 ERROR / 39s）+ xmllint 3 文件 well-formed + compliance checker exit 0 零新增命中 + grep 复核契约一致（mfg 结案按钮无 STARTED / pur Rfq cancel 用 rfqId / hr Employee 无残留 LEFT( RIGHT(）。
P2-MA4-014（mfg 进度 badge :111 STARTED/ACTIVE 死状态）为本计划 Non-Goal 显式 deferred，已登记 arm-index，不在本批。
**余项**：无（独立结束审计已于 2026-07-31 由独立子代理新会话执行并通过）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文），2026-07-31
- 执行验证证据（executor）：
  - `mvn clean install -DskipTests` → BUILD SUCCESS，156 模块全 SUCCESS，`grep -c '^\[ERROR\]' = 0`，app-erp-mfg-web/app-erp-pur-web/app-erp-hr-web 均 SUCCESS（见本计划执行会话 /tmp/mvn-build.log）。
  - xmllint --noout 3 文件：well-formed 通过（namespace warning 为 Nop schema 前缀 ui:/c:/j:/gql: 既有现象，非 well-formed 错误）。
  - `bash docs/audits/nop-compliance-checker.sh` exit 0；命中全为 pre-existing Java 基线（view.xml 文本变更不触发 Java 规则）→ 零新增命中。
  - grep 复核：mfg `ErpMfgWorkOrder.view.xml:234` 无 STARTED（:111 为 deferred P2-MA4-014，:218 为合法 NOT_STARTED）；pur `ErpPurRfq.view.xml:92` = `ErpPurRfq__cancel?rfqId=$id`；hr `ErpHrEmployee.view.xml` 无 LEFT(/RIGHT( 残留。
- 独立结束审计证据：独立子代理（新会话，无执行者上下文）完成冷重播结束审计，结果 **approved**：
  - **Exit Criteria vs live repo 复核**：实测 mfg `ErpMfgWorkOrder.view.xml:234` = `${docStatus == 'IN_PROCESS' || docStatus == 'STOPPED'}`（grep 确认 :234 无 STARTED；:111 为 deferred P2-MA4-014、:218 为合法 NOT_STARTED）；pur `ErpPurRfq.view.xml:92` = `ErpPurRfq__cancel?rfqId=$id`；hr `ErpHrEmployee.view.xml` grep 零 `LEFT(`/`RIGHT(` 残留（4 处已改 `String(x).slice()`）。
  - **后端契约复核**：`ErpMfgWorkOrderProcessor.close()` 守卫（`ErpMfgWorkOrderProcessor.java:142-147`）仅允许 STOPPED+IN_PROCESS 否则抛「STOPPED 或 IN_PROCESS」→ Decision B 精确对齐；`ErpPurRfqBizModel.cancel(@Name("rfqId") Long rfqId,...)`（`:22`）与 view `rfqId` 精确匹配；`docs/design/manufacturing/state-machine.md` §2 文档化 STOPPED→CLOSED / IN_PROCESS→CLOSED，owner-doc 无需变更（正确）。
  - **Anti-Hollow 复核**：3 处均为实质运行时行为变更（按钮可见性/参数名/PII 掩码渲染），无空体/return null/吞异常/未接线组件。
  - **Deferred honesty**：P2-MA4-014（mfg 进度 badge :111）为系统性 P2 watch-only，Non-Goal 显式 deferred + arm-index 登记，无范围内 P1 缺陷降级。
  - **Docs sync**：`docs/logs/2026/07-31.md` 含完整聚合日志条目（3 项 + Decision B + 验证 + Deferred）。
  - **五点一致性**：Plan Status: completed / Phase 1 Status: completed / Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 证据实存——全部一致。
  - 审计结论：3 项 P1 view.xml drift 修复已落地且与后端 dict/BizModel/JS 运行时契约一致，验证证据真实，可关闭。

Follow-up:

- 无（P2-MA4-014~020 死状态 badge/调色板族为 P2 watch-only，已登记 arm-index，不属本计划范围）
