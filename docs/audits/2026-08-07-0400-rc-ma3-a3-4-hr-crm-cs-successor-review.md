# rc-ma3-a3-4-hr-crm-cs-successor-review hr+crm+cs MA3 successor 追踪完整性与回队复查报告（A3.4）

> Plan Status: completed
> 产出时间：2026-08-07
> 来源 Plan：`docs/plans/2026-08-07-0400-1-rc-ma3-a3-4-hr-crm-cs-successor-review.md`（Work Item A3.4）
> Mission：requirement-compliance（MA3 successor 触发条件复查）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§4 三判据 / §5 Q4 + 保护区域 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）
> 路线图：`docs/backlog/requirement-compliance-roadmap.md`（A3.4 hr+crm+cs 域 successor 复查 + Work Item Details MA3）
> 复查全集：`docs/audits/rc-existing-inventory.md`（§successor 三源对账清单 hr+crm+cs 域分组 — 4 项 + §对账差异登记 #5）
> Skill：`docs/skills/open-ended-audit-prompt.md`
> 审计性质：**只读审计**——读 arm-index / owner doc / backlog README / 实仓代码 / config / ORM 裁决 successor 触发条件，**不修改任何代码/ORM/api.xml/真相源**

---

## §复查口径与 Q4 修复义务边界

本报告复查对象 = M0.3（`rc-existing-inventory.md` §successor 三源对账清单）导出的 hr+crm+cs 域 design-level successor 去重并集 **4 项**。逐项完成方法论 §MA3 四任务：① 触发条件是否已满足（grep 实仓代码/config/ORM 字段验证）；② 是否该回队（已满足→回队 MR1 R1.0；未满足→维持 backlog successor）；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

**Q4 修复义务边界（§5）**：successor 触发条件**已满足**者须回队 MR1（R1.0 展开为 RC-R1.n，Q4 强制实现禁方案 B）；触发条件**未满足**者维持 backlog successor 登记（不强制实现，待触发）。本复查 4 项触发条件**全部未满足**（grep 实仓逐项证实，#3 经 §对账差异 #5 区分「finding 已修复」与「successor 残留」后核心 successor 仍维持 backlog）→ 4 项**全部维持 backlog successor**，无回队 MR1。

**finding 路由 vs successor 触发条件路由（防执行者混淆）**：本 A3.x 只裁决 **successor 触发条件**是否回队，不重审方案 B 关闭裁决本身（属 A2.x），也不重审 finding 是否修复（属 A1.x→MR1）。即：successor 回队与否（A3.x）≠ finding 是否修复（A2.x/A1.x→MR1），两者各自裁决、交叉引用不冲突。本复查的 §对账差异登记 #5 处置（区分「finding 已修复/关闭」与「successor 仍待触发」）正是此原则的体现：

- #1 `P1-MA2-039`（hr 员工离职族）：**finding 经 R1.15 resolved-via-deferral**（owner doc `state-machine.md §场景D/E` Deferred 标注，A2.4 RC 空集认证范围）；其 **successor**（resign/terminate/retire/probationToRegular BizMutation + 跨域联动）触发条件未满足 → 本 A3.4 维持 backlog successor。两者各自裁决不冲突。
- #2 `P1-MA2-045`（hr 银行文件）：**finding 经 R1.15 resolved-via-deferral**（owner doc `payroll.md §七` Deferred 标注，A2.4 RC 空集认证范围）；其 **successor**（UPLOADED/CONFIRMED setStatus writer + 回单自动对账）触发条件未满足 → 本 A3.4 维持 backlog successor。
- #3 `P1-MA2-075`（crm stageId）：**finding 经 R1.24 resolved-via-implementation**（方案 A：`validateStageDirection` 守卫 + config-gated allow-stage-backward，A1.30 RC 复用确认 genuinely resolved[属 A1.x/A2.x 裁决]）；其 **successor**（漏斗统计完善 / stageId 守卫增强）核心已随 R1.24 + FunnelAggregationEngine 落地而满足，**残留**为 P2-RC-036 等值边界 watch-only + `lead-waterfall.md` 增量实时/AMIS 可视化 successor（各自独立触发条件未满足）→ 本 A3.4 维持 backlog successor（仅残留增强层）。
- #4 `P2-MA2-067`（cs 滞留升级）：**finding watch-only 未关闭**（arm-index :744 无 resolution 标记，A2.8 RC 范围 + A1.37/A1.38 RC reuse 交叉引用）；其 **successor**（NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings scheduler）触发条件未满足 → 本 A3.4 维持 backlog successor。

---

## 1. successor 三源对账清单（hr+crm+cs 域，段 1，§6 MA3 适配）

> 三源：S1 = `docs/audits/arm-index.md` 行内 successor/触发条件声明 / S2 = owner doc 内嵌 successor / Deferred 段落 / S3 = `docs/backlog/README.md` 既有追踪行。

| # | successor 项 | 域 | 三源覆盖 | 触发条件摘要 | 复杂度 | A2.x 关闭裁决交叉（two-faces） |
|---|-------------|----|---------|-------------|--------|------------------------------|
| 1 | hr 员工离职/终止/退休/转正状态机 + 跨域联动 | hr | S1+S2 | PM 要求正式离职/退休/试用期转正工作流时（触 nop-auth UserAccount 保护区域） | S | `P1-MA2-039`（A2.4 空集认证：resolved-via-deferral R1.15，owner doc `state-machine.md §场景D/E` Deferred） |
| 2 | hr 银行文件 UPLOADED/CONFIRMED + 回单对账 | hr | S1+S2 | config-gated 银行文件生成/上传确认流接入时（银行回单自动对账 successor） | S | `P1-MA2-045`（A2.4 空集认证：resolved-via-deferral R1.15，owner doc `payroll.md §七` Deferred） |
| 3 | crm stageId 单向递增守卫 + 漏斗统计 | crm | S1+S2 | owner doc §stageId 单向递增契约落地（P1-MA2-075 经 R1.24 实现修复，方案 B 路径未走） | A | `P1-MA2-075`（A2.8：resolved-via-implementation R1.24，A1.30 RC reuse 确认 genuinely resolved） |
| 4 | cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings scheduler | cs | S1+S2 | 通知 successor（0642-1 范式：nop-job 定时自动触发滞留扫描/预警时） | A | `P2-MA2-067`（A2.8：watch-only 未关闭，A1.37/A1.38 RC reuse 交叉引用） |

> **§对账差异登记 #5 覆盖**：#1/#2/#3/#4 四项的「finding 已修复/关闭」与「successor 仍待触发」区分见 §复查口径段（上）。#3 是 §对账差异 #5 的核心核实项——finding（stageId 守卫）经 R1.24 已实现修复（方案 A），须严格区分「finding 已修复」与「successor（漏斗统计完善 / 守卫增强）仍有效」，避免误将已修复 finding 重新纳入 MR1。
>
> **三源覆盖说明**：4 项均为 S1+S2 双源覆盖（arm-index 行内 successor 声明 + owner doc Deferred/Successor 段落）。S3（`docs/backlog/README.md`）经 M0.3 §对账差异登记 #4 核实为 E2E 测试 successor（hr/crm/cs 域如 `hr-recruitment`/`hr-leave-attendance`/`hr-payroll` E2E、`ticket-sla-csat-summary` 报表 E2E、`lead-conversion-funnel` 报表 E2E 均为测试覆盖 successor 非 design successor），不产生独立 design-level successor，仅作覆盖交叉验证。

---

## 2. 逐项四任务核证（段 2，§6 MA3 适配）

> 四任务：① 触发条件是否已满足（grep 实仓代码/config/ORM）；② 是否该回队；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

### 2.1 #1 hr 员工离职/终止/退休/转正状态机 + 跨域联动 — hr

- **① 触发条件状态**：**未满足**。实仓 grep `module-hr/erp-hr-service/src/main` `setEmploymentStatus`：生产代码唯一 writer 为 `ErpHrRecruitmentHireProcessor:63` + `ErpHrRecruitmentBizModel:149`（招聘 hire 创建时设 `EMPLOYMENT_ACTIVE`）——**无任何 `setEmploymentStatus(RESIGNED|TERMINATED|RETIRED)` writer**。`EMPLOYMENT_RESIGNED/TERMINATED/RETIRED` 常量定义于 `ErpHrConstants.java:215-217` + `_ErpHrDaoConstants.java:19,24,29`，仅 `ErpHrEmployeeBizModel.nonTransferableStatuses():321-323` + `ErpHrEmployeeTransferEmployeeProcessor.isTransferable:115-117` 作**只读调动守卫**引用（RESIGNED/TERMINATED/RETIRED 不可调动）。grep `resign|terminate|retire|probationToRegular` × Employee BizMutation 跨 main **零业务命中**。owner doc `state-machine.md §场景D 离职 :160-167 / §场景E 转正 :169-172` + `:126` 显式 **Deferred** 段落声明「下方 §场景 D（离职）/§场景 E（转正）描述的是**目标行为，未接入**」+ **Successor**「PM 要求正式离职/退休/试用期转正工作流时实现上述 mutation（方案A 触及 nop-auth 用户禁用副作用，属保护区域，故 Deferred）」。**跨域联动（nop-auth 保护区域）**：`ErpHrEmployee.userAccountId` 字段存在（`_ErpHrEmployee.java:1900-1908` getter/setter），但 grep `UserAccount|NopAuthUser|disableUser|setStatus.*RESIGNED|userAccount.status|nopAuth` 跨 module-hr main **零 writer**——离职联动禁用 nop-auth UserAccount 的代码**完全未实现**。触发条件 = 「PM 要求正式离职/退休/试用期转正工作流时」，该业务**未上线**。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——PM 离职/退休/转正工作流未上线；修复触及 nop-auth UserAccount 保护区域，属 §5 ask-first 类目，修复归 MR1 须 ask-first + 独立 plan-audit，非本审计实施）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P1-MA2-039` 行含 successor 声明 + owner doc `state-machine.md §场景D/E :126` 显式 Deferred + Successor 触发条件标注最详）。
- **④ README 覆盖复核**：`docs/backlog/README.md` 无独立「hr 员工离职族」design successor 行（README hr 域行如 `:59 hr-recruitment`[招聘漏斗 E2E] / `:76 HR 休假/考勤/招聘/合同到期引擎`[P8 已 done] / `:86/:87 hr 排班/人才发展 E2E` 为 E2E 测试 successor + 已 done 的 P8 引擎实现，非本 design successor；P8 `:76` 已实现合同到期扫描 Job + 续签/到期终止，但⑮不续签→RESIGNED 仍复用 P1-MA2-039 successor Deferred 见 A1.12 RC §6 复用注记）。design successor 经 S1（arm-index `P1-MA2-039` 行）+ S2（owner doc）覆盖，**无「已登记但从未触发」风险**（触发条件「PM 要求正式离职/退休/转正工作流」未触发明确，未误标 done）。
- **结构性约束标注（nop-auth 保护区域 + §对账差异 #5）**：`P1-MA2-039` **finding 已 resolved-via-deferral**（R1.15，owner doc Deferred 标注，非方案 A 实现）。本 A3.4 裁决的是 **successor 触发条件**（未满足→维持 backlog），**不重审** finding 关闭裁决（归 A2.4 RC 空集认证 + A1.12 RC §4 三判据复核——属 A1.x/A2.x 通道，与本 successor 裁决各自独立交叉不冲突）。**与 A1.12 hr-F1 §7 发现的跨域联动同根因同控制点**（合同到期不续签→员工 RESIGNED 未实现，A1.12 复用 P1-MA2-039 successor Deferred）。

### 2.2 #2 hr 银行文件 UPLOADED/CONFIRMED + 回单对账 — hr

- **① 触发条件状态**：**未满足**。实仓 grep `module-hr/erp-hr-service/src/main` `BANK_FILE_STATUS|setBankFileStatus|setStatus.*UPLOADED|setStatus.*CONFIRMED`：`BANK_FILE_STATUS_GENERATED/UPLOADED/CONFIRMED` 常量定义于 `_ErpHrDaoConstants.java:439,444,449`，但 `ErpHrConstants.java:72` **仅定义 `BANK_FILE_STATUS_GENERATED`**（UPLOADED/CONFIRMED 两常量未在主常量类暴露）。生产代码唯一 writer 为 `ErpHrSalaryGenerateBankFileProcessor:53` 设 `BANK_FILE_STATUS_GENERATED`——**无任何 `setStatus(UPLOADED|CONFIRMED)` writer**（UPLOADED/CONFIRMED 为 dict 死状态）。grep `reconcileBankReceipt|bankReconciliation|回单对账` 跨 module-hr main **零命中**——银行回单自动对账**完全未实现**。`ErpHrPayrollBankFileBizModel` 为 CrudBizModel 桩（owner doc `payroll.md §七 :451` 自承「18 行，零状态机 mutation」）。owner doc `payroll.md §七 :449-451` 显式 **Deferred** + **Successor**「`status` 字段 dict 值 `GENERATED/UPLOADED/CONFIRMED` 为**预留死状态**——本期零 `setStatus` writer，无上传/确认 mutation。**Successor**：config-gated 银行文件生成/上传确认流接入时实现 setStatus writer + 状态迁移守卫」。触发条件 = 「config-gated 银行文件生成/上传确认流接入时」，该业务**未上线**。**不破坏主路径**——`generateBankFile` 已批量设 PAID（`ErpHrSalaryBizModel`），资金流在 PAID 时确认而非银行文件确认时确认（owner doc `payroll.md §七` + arm-index `P1-MA2-045` 描述一致）。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——银行文件上传/确认流 + 回单自动对账未上线；修复属代码逻辑类[新增 upload/confirm BizMutation + 回单对账]，预授权可自动执行，但触发条件未满足不强制实现）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P1-MA2-045` 行含 successor 声明 + owner doc `payroll.md §七 :451` 显式 Deferred + Successor 触发条件标注）。
- **④ README 覆盖复核**：无独立 design successor 行（README hr 域行为 E2E 测试 successor + 已 done 的引擎实现，非本 design successor）。S1+S2 覆盖充分，无悬空。
- **结构性约束标注（§对账差异 #5）**：`P1-MA2-045` **finding 已 resolved-via-deferral**（R1.15 方案 B Deferred，owner doc `payroll.md §七` Deferred 标注，A2.4 RC 空集认证范围）。本 A3.4 裁决的是 **successor 触发条件**（未满足→维持 backlog），**不重审** finding 关闭裁决。

### 2.3 #3 crm stageId 单向递增守卫 + 漏斗统计 — crm（§对账差异 #5 核实项）

- **① 触发条件状态**：**核心已满足（R1.24 守卫 + FunnelAggregationEngine 双落地），残留增强层未满足**。本项是 §对账差异登记 #5 的核心核实项，须严格区分「finding 已修复」与「successor 仍有效」：
  - **finding（stageId 单向递增守卫）= 已实现修复（R1.24 方案 A）**：实仓 `ErpCrmLeadProcessor.validateStageDirection:91-107` **已落地**——`if (fromSeq != null && toSeq != null && toSeq < fromSeq)` STRICT 模式（`ErpCrmConfigs.allowStageBackward()` 默认 false）抛 `ErpCrmErrors.ERR_STAGE_BACKWARD_MOVE`（`ErpCrmErrors.java:80-81`），config-gated `erp-crm.allow-stage-backward`=true 放行（LOG.warn + 仍写 convLog 审计）。`ErpCrmLeadMoveStageProcessor:23` 调 `facade.validateStageDirection`。owner doc `state-machine.md §stageId 迁移规则 :40` + `§审查提示 :196` 已与实现一致（「阶段回退经 `validateStageDirection` 守卫拦截」）。A1.30 RC（`2026-08-05-1830-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`）复用 P1-MA2-075 resolved R1.24 确认 **genuinely RESOLVED via implementation 无回退**。
  - **successor（漏斗统计）= 引擎已落地**：`FunnelAggregationEngine.java`（458 行完整聚合引擎，`:200` 按阶段 sequence 排序）从 ConvLog + Lead + Stage 聚合 ErpCrmLeadFunnel 头 + ErpCrmFunnelStageMetrics 明细（进入/流出/剩余/转化率/流失率/停留天数/丢失原因 TOP N），stageName 快照防阶段定义变更。守卫落地后回退被拦截 → 漏斗统计不再被回退污染，**核心 successor 触发条件（stageId 契约落地 → 漏斗统计准确）已满足**。
  - **残留 successor（增强层）= 未满足**：(a) `P2-RC-036`（A1.30 登记）UC-CRM-06 等值边界——L1 `:122` 字面 `<=`（等值拒绝）vs 代码 `validateStageDirection:99` `<`（等值放行），属 stageId 守卫增强 watch-only successor（P2 登记不强制）；(b) owner doc `lead-waterfall.md :237` 增量实时更新 successor（触发=实时漏斗监控业务需求上线）+ `:240` 漏斗 AMIS 可视化前端 successor（触发=CRM 前端可视化套件建立时）——两者为漏斗统计的增强层 successor，**各自独立触发条件未满足**。
- **② 回队决策**：**维持 backlog successor（仅残留增强层）**。核心 successor（stageId 守卫 + 漏斗统计引擎）已随 R1.24 + FunnelAggregationEngine 落地而满足，**不回队 MR1**（finding 已修复，successor 核心已满足，误将已修复 finding 重新纳入 MR1 违反 §对账差异 #5 纪律）。残留增强层（P2-RC-036 等值边界 + lead-waterfall.md 增量实时/AMIS 可视化 successor）维持 backlog，待各自独立触发条件满足。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P1-MA2-075` 行含 successor 声明 + owner doc `state-machine.md §stageId :40,:196` + `lead-waterfall.md :237,:240` 显式 successor 触发条件标注；P2-RC-036 等值边界经 A1.30 已登记为独立 watch-only finding）。
- **④ README 覆盖复核**：README crm 域行如 `:70 销售定价引擎` / `:1045-2 lead-conversion-funnel 报表 E2E` 为 P6 已 done 实现 + 报表 E2E 测试 successor，非本 design successor。S1+S2 覆盖充分。
- **结构性约束标注（§对账差异 #5 — 核心核实项）**：`P1-MA2-075` **finding 经 R1.24 resolved-via-implementation**（方案 A，`validateStageDirection` 守卫落地，非方案 B 降级）。本 A3.4 严格区分「finding 已修复」（stageId 守卫已实现）与「successor 仍有效」（漏斗统计核心已满足 + 残留增强层 P2-RC-036/lead-waterfall.md successor 维持）——**不误将已修复 finding 重新纳入 MR1**（finding 重开归 A1.x/A2.x 通道，successor 裁决归 A3.x，两者各自独立）。这是 §对账差异登记 #5「实现修复项 successor 残留」纪律的体现：finding 修复 ≠ successor 全部关闭，但 successor 核心满足 + 残留为独立 watch-only 增强层时不回队。

### 2.4 #4 cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings scheduler — cs

- **① 触发条件状态**：**未满足**。实仓 grep `module-cs/erp-cs-service/src/main` `findSlaWarnings|staleScan|StaleScan|scanStaleTickets|staleHours|滞留`：(a) `findSlaWarnings` 作为 `@BizQuery` 暴露于 `ErpCsTicketBizModel:224`（pre-breach 早期预警查询，`deadlineDateTime BETWEEN now AND now+beforeMinutes`），但**无 scheduler job 消费**——`ErpCsSlaScanJob.execute():36-49` 仅调 `ticketBiz.scanOverdueTickets`（post-breach deadline-based 升级，`runSlaScan:51-53`），**不调 findSlaWarnings**；(b) NEW>1h / ASSIGNED>2h 滞留升级 grep `staleScan|StaleScan|NEW.*1h|ASSIGNED.*2h|scanStaleTickets` 跨 main **零业务命中**——滞留升级规则**完全未实现**。owner doc `state-machine.md §避免工单滞留 :106` 声明「NEW 停留超过 1 小时→自动升级通知客服主管 / ASSIGNED 停留超过 2 小时→自动提醒处理人 / IN_PROGRESS 超 deadlineDateTime→触发 SLA 超时升级」，**仅第三条（deadline-based）落实**（`scanOverdueTickets`），前两条（NEW>1h / ASSIGNED>2h）未实现。owner doc `sla.md §3.4 预警 :347` 显式「`findSlaWarnings` 供 nop-job 调用；**cron 实际注册归 Non-Goal**（Follow-up：生产部署需定时自动触发时接 nop-job）」+ `sla.md §4.2 :192` 独立 SLA 报表模板归 Deferred successor。触发条件 = 「通知 successor（0642-1 范式：nop-job 定时自动触发滞留扫描/预警时）」，该 nop-job 接线**未落地**。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——NEW>1h/ASSIGNED>2h 滞留升级 + findSlaWarnings scheduler 未实现；主路径 SLA 倒计时 + 到期告警[scanOverdueTickets]已实现，滞留升级属增强层。修复属代码逻辑类[新增 ErpCsTicketStaleScanJob + 注册 erp-cs-sla-warning.job.yaml 调 findSlaWarnings]，预授权可自动执行，但触发条件未满足不强制实现）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P2-MA2-067` 行含 successor 声明 + owner doc `sla.md §3.4 :347` + `state-machine.md §避免工单滞留 :106` 显式 successor 触发条件标注）。
- **④ README 覆盖复核**：无独立 design successor 行（README cs 域行如 `:1045-2 ticket-sla-csat-summary 报表 E2E` 为报表 E2E 测试 successor，非本 design successor）。S1+S2 覆盖充分。
- **结构性约束标注（§对账差异 #5 + reuse 交叉引用）**：`P2-MA2-067` **finding watch-only 未关闭**（arm-index :744 无 resolution 标记）。A1.37 RC（UC-CS-02 ⑤ ASSIGNED>2h 升级）+ A1.38 RC（UC-CS-04 ⑤ 通知目标）已从需求契约视角 reuse P2-MA2-067 交叉引用（同根因同控制点，按 §7 复用既有 finding ID 不新建）。本 A3.4 裁决的是 **successor 触发条件**（未满足→维持 backlog），**不重审** finding 关闭裁决（P2-MA2-067 维持 watch-only MR1 顺手，两维度[状态机+需求契约]互补不重复）。

---

## 3. 既有行为证据（段 3，复用既有 arm 审计，§去重协议）

> 本复查为 successor 触发条件复查（需求契约视角），不重做 doc↔code 文本一致性 / 状态机行为 / 代码质量。实现证据复用既有 arm MA2/MA4 报告 + A1.x RC 复查报告已证实的代码路径，仅列锚点供四任务核证溯源。

| # | successor 项 | 代码锚点（复用 arm MA2/MA4 + A1.x RC 已证实） | 既有证实报告 |
|---|-------------|----------------------------------------------|-------------|
| 1 | hr 员工离职族 | `ErpHrConstants.java:215-217` + `_ErpHrDaoConstants.java:19,24,29`（RESIGNED/TERMINATED/RETIRED 常量定义）+ 零 setEmploymentStatus(三态) writer + 唯一 writer `ErpHrRecruitmentHireProcessor:63`/`ErpHrRecruitmentBizModel:149`（hire→ACTIVE）+ `ErpHrEmployeeBizModel.nonTransferableStatuses():321-323` + `ErpHrEmployeeTransferEmployeeProcessor.isTransferable:115-117`（只读调动守卫）+ `ErpHrEmployee.userAccountId`（`_ErpHrEmployee.java:1900-1908`）+ 零 nop-auth UserAccount disable writer | `2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`（P1-MA2-039 三态死状态已证实）；A1.12 RC §3 + §6（UC-HR-07⑮复用 P1-MA2-039 successor Deferred） |
| 2 | hr 银行文件 | `_ErpHrDaoConstants.java:439,444,449`（GENERATED/UPLOADED/CONFIRMED 常量）+ `ErpHrConstants.java:72`（仅 GENERATED）+ 唯一 writer `ErpHrSalaryGenerateBankFileProcessor:53`（GENERATED）+ 零 setBankFileStatus(UPLOADED/CONFIRMED) + 零回单对账 | `2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（P1-MA2-045 UPLOADED/CONFIRMED 死状态已证实）；A1.14 RC §3 |
| 3 | crm stageId + 漏斗统计 | `ErpCrmLeadProcessor.validateStageDirection:91-107`（R1.24 守卫 `toSeq < fromSeq` + ERR_STAGE_BACKWARD_MOVE）+ `ErpCrmConfigs.allowStageBackward():17`（默认 false）+ `ErpCrmLeadMoveStageProcessor:23` + `ErpCrmErrors.ERR_STAGE_BACKWARD_MOVE:80-81` + `FunnelAggregationEngine.java:200`（按 sequence 排序聚合）+ `ErpCrmLeadConvLog` 四字段完整性 | `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（P1-MA2-075 stageId 守卫未实现[历史]）；A1.30 RC §3（P1-MA2-075 resolved R1.24 genuinely resolved 确认 + P2-RC-036 等值边界 watch-only） |
| 4 | cs 滞留升级 | `ErpCsTicketBizModel.findSlaWarnings:224`（@BizQuery pre-breach，无 scheduler 消费）+ `ErpCsSlaScanJob.execute():36-49`（仅调 scanOverdueTickets post-breach）+ `runSlaScan:51-53` + 零 staleScan/NEW>1h/ASSIGNED>2h 实现 | `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（P2-MA2-067 滞留升级未实现已证实）；A1.37 RC §6.1（reuse P2-MA2-067）+ A1.38 RC §6.1（reuse A2.14:322 残留风险注记） |

---

## 4. 运行时行为证据（段 4，复用既有 arm MA2，§去重协议）

> 本 mission MA3 = successor 触发条件复查（需求契约视角），与 audit-remediation MA2（状态机/链路行为视角）/ MA4（代码质量视角）维度不重叠（methodology §去重协议）。既有 arm 报告 + A1.x RC 报告已证实的运行时行为直接引用：

- **#1 hr 员工离职族**：在职生命周期（ACTIVE/PROBATION）由招聘 hire + 调岗 transferEmployee 完整覆盖；工资核算 `AbstractErpSalaryProcessor:70` + `ErpSalaryBizModel:179` 守卫 `employmentStatus in (ACTIVE,PROBATION)`，不会因终态死状态产生悬挂数据——经 `2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md` + A1.12 RC §4 证实。RESIGNED/TERMINATED/RETIRED 为预留死状态（零 writer + 仅只读调动守卫），无运行时数据破坏。
- **#2 hr 银行文件**：银行文件生成 + 批量 PAID 完整覆盖薪酬发放主路径（`generateBankFile` 设 GENERATED + 批量 PAID）；UPLOADED/CONFIRMED 是 owner doc Deferred 能力（银行回单自动对账 + 实际转账执行归 successor），资金流在 PAID 时确认而非银行文件确认时确认，缺失状态机不产生悬挂数据——经 `2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md` + A1.14 RC §4 证实。
- **#3 crm stageId + 漏斗统计**：stageId 单向递增主路径守卫已落地（R1.24：前移 `>` 允许 + 回退 `<` STRICT 拒绝 + config-gated allow-backward），convLog 四字段（fromStageId/toStageId/changedAt/changedBy）全量留痕；FunnelAggregationEngine 按 sequence 排序聚合，守卫落地后漏斗统计不再被回退污染——经 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md` + A1.30 RC §4 证实。残留等值边界（P2-RC-036）为 watch-only 边界场景，主路径正确。
- **#4 cs 滞留升级**：deadline-based SLA 超时升级主路径完整（`scanOverdueTickets` 扫描 deadlineDateTime<now 触发 ESCALATE 审计 + 通知 + breach close remark 守卫）；NEW>1h / ASSIGNED>2h 滞留升级属增强层（软 UX 升级规则），deadline-based 升级已覆盖 breach 场景，缺失滞留升级不破坏主路径——经 `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md` + A1.37/A1.38 RC §4 证实。

---

## 5. 复查结论（段 5，§6 MA3 适配：触发条件状态 + 回队决策）

> 复查结论三分：`回队 MR1`（触发条件已满足 / Q4 强制）/ `维持 backlog successor`（触发条件未满足）/ `补登记`（owner doc 内嵌但 arm-index 无行）。

### 5.1 逐项复查结论

| # | successor 项 | 触发条件状态 | 证据 | 回队决策 | 与 A2.x 关闭裁决交叉 |
|---|-------------|-------------|------|---------|---------------------|
| 1 | hr 员工离职族状态机 + 跨域联动 | ❌ 未满足（PM 离职/退休/转正工作流未上线） | 零 setEmploymentStatus(三态) writer + 零 nop-auth UserAccount disable writer | **维持 backlog successor** | #1 ↔ `P1-MA2-039`（A2.4 空集认证：resolved-via-deferral R1.15）一致；**nop-auth 保护区域**：修复实施须 ask-first + 独立 plan-audit（§5），本裁决仅判 successor 触发条件；successor 维持 ≠ finding 重开（finding §4 三判据复核归 A1.12 RC→MR1 通道，两者各自裁决交叉不冲突） |
| 2 | hr 银行文件 UPLOADED/CONFIRMED + 回单对账 | ❌ 未满足（银行文件上传/确认流 + 回单自动对账未上线） | 零 setBankFileStatus(UPLOADED/CONFIRMED) writer + 零回单对账 | **维持 backlog successor** | #2 ↔ `P1-MA2-045`（A2.4 空集认证：resolved-via-deferral R1.15）一致；successor 维持 ≠ finding 重开（finding 关闭归 A2.4 RC 空集认证） |
| 3 | crm stageId 单向递增守卫 + 漏斗统计 | ⚠️ 核心已满足（R1.24 守卫 + FunnelAggregationEngine 双落地）/ 残留增强层未满足（P2-RC-036 等值边界 + lead-waterfall.md 增量实时/AMIS 可视化 successor） | `ErpCrmLeadProcessor.validateStageDirection:91-107` 守卫已落地 + `FunnelAggregationEngine.java:200` 引擎已落地 + 残留 P2-RC-036 watch-only + lead-waterfall.md successor | **维持 backlog successor（仅残留增强层）** | #3 ↔ `P1-MA2-075`（A2.8：resolved-via-implementation R1.24，A1.30 RC reuse 确认 genuinely resolved）一致；**§对账差异 #5 核心核实项**：finding 已修复（方案 A）≠ successor 全部关闭，但 successor 核心满足 + 残留为独立 watch-only 增强层 → 不回队 MR1（避免误将已修复 finding 重新纳入 MR1） |
| 4 | cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings scheduler | ❌ 未满足（滞留升级 + findSlaWarnings scheduler 未实现） | `ErpCsSlaScanJob` 仅调 scanOverdueTickets + 零 staleScan + findSlaWarnings 无 scheduler 消费 | **维持 backlog successor** | #4 ↔ `P2-MA2-067`（A2.8：watch-only 未关闭，A1.37/A1.38 RC reuse 交叉引用）一致；successor 维持 ≠ finding 关闭（finding 维持 watch-only MR1 顺手，两维度互补不重复） |

### 5.2 统计

- **回队 MR1**：0 项（4 项触发条件核心均未满足[#1/#2/#4 未满足 + #3 核心满足但残留为 watch-only 增强层不回队]，无 Q4 强制回队）
- **维持 backlog successor**：4 项（#1-#4 全部维持 backlog；#3 仅残留增强层维持）
- **补登记**：0 项（4 项均有 S1 arm-index 覆盖，#1/#2/#3/#4 另有 S2 owner doc 双源覆盖，无 owner doc 内嵌但 arm-index 无行的遗漏项）
- **本审计新发现 P0**：0 项（无 MR0 即时通道触发）

### 5.3 结构性约束（§对账差异 #5 + 保护区域 + reuse 交叉引用）

- **§对账差异 #5（实现修复项 successor 残留）**：#1/#2/#3/#4 四项的 finding 均已有 resolution（#1/#2 resolved-via-deferral R1.15，#3 resolved-via-implementation R1.24，#4 watch-only 未关闭）。本 A3.4 严格区分「finding 已修复/关闭」与「successor 仍待触发」——不误将已修复 finding 重新纳入 MR1（finding 重开/关闭归 A1.x/A2.x 通道，successor 维持归 A3.x，两者各自裁决）。**#3 是此项纪律的核心体现**：finding（stageId 守卫）经 R1.24 已实现修复，successor（漏斗统计）核心已满足，残留为独立 watch-only 增强层（P2-RC-036 + lead-waterfall.md successor）→ 不回队 MR1。
- **#1 nop-auth 保护区域**：hr 员工离职联动触及 nop-auth UserAccount 禁用（`ErpHrEmployee.userAccountId` 存在但零 disable writer），修复实施须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本 A3.4 仅裁决 successor 触发条件（未满足→维持 backlog），nop-auth 变更属 MR1 修复期门控，非本裁决期。
- **#4 reuse 交叉引用**：P2-MA2-067 经 A1.37 RC（UC-CS-02 ⑤）+ A1.38 RC（UC-CS-04 ⑤）从需求契约视角 reuse 交叉引用（同根因同控制点，按 §7 复用既有 finding ID 不新建）。本 A3.4 successor 裁决与 A1.x reuse 注记互补不重复。

---

## 6. 与 arm-index 衔接（段 6，§7「复用 or 新增」裁决）

> §7 规则：successor 项均源自既有 arm finding，本复查原则上**复用既有 finding ID**追加 RC MA3 注记；仅当发现 owner doc 内嵌但 arm-index 无独立行的 successor（如 A3.1 #7/#8）才补登记。本 A3.4 4 项**全部有 S1 arm-index 覆盖**（4 项均有既有 arm finding 行），故**全部复用**，无补登记。

### 6.1 逐项「复用 or 补登记」裁决

| # | successor 项 | arm-index grep 结果 | 裁决 | 操作 |
|---|-------------|---------------------|------|------|
| 1 | hr 员工离职族状态机 + 跨域联动 | 既有 `P1-MA2-039` 行（arm-index :485）含 successor 声明（state-machine.md §场景D/E Deferred） | **复用** | 既有行追加「RC MA3 复查（A3.4）：触发条件未满足[PM 离职/退休/转正工作流未上线]→维持 backlog successor；nop-auth 保护区域[修复实施须 ask-first + 独立 plan-audit]；successor 维持 ≠ finding 重开[finding §4 三判据复核归 A1.12 RC→MR1]」注记 |
| 2 | hr 银行文件 UPLOADED/CONFIRMED + 回单对账 | 既有 `P1-MA2-045` 行（arm-index :491）含 successor 声明（payroll.md §七 Deferred） | **复用** | 既有行追加「RC MA3 复查（A3.4）：触发条件未满足[银行文件上传/确认流 + 回单自动对账未上线]→维持 backlog successor；successor 维持 ≠ finding 重开[finding 关闭归 A2.4 RC 空集认证]」注记 |
| 3 | crm stageId 单向递增守卫 + 漏斗统计 | 既有 `P1-MA2-075` 行（arm-index :518）含 successor 声明（stageId 迁移规则） | **复用** | 既有行追加「RC MA3 复查（A3.4）：§对账差异 #5 核实——finding 经 R1.24 resolved-via-implementation[方案 A 守卫落地] + 漏斗统计核心[FunnelAggregationEngine]已满足，残留增强层[P2-RC-036 等值边界 + lead-waterfall.md 增量实时/AMIS 可视化 successor]维持 backlog；不回队 MR1[避免误将已修复 finding 重新纳入]」注记 |
| 4 | cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings scheduler | 既有 `P2-MA2-067` 行（arm-index :744）含 successor 声明（避免工单滞留） | **复用** | 既有行追加「RC MA3 复查（A3.4）：触发条件未满足[滞留升级 + findSlaWarnings scheduler 未实现]→维持 backlog successor；successor 维持 ≠ finding 关闭[finding 维持 watch-only MR1 顺手，A1.37/A1.38 RC reuse 交叉引用]」注记 |

**裁决依据**：4 项均为既有 arm finding 的同一根因/同一控制点 successor，复用既有 ID 追加 RC MA3 注记。**不新建 `P*-RC-xxx`**（禁止未经比对直接新建）——4 项全部有既有 arm-index 行覆盖，无 owner doc 内嵌但 arm-index 无独立行的遗漏项（与 A3.1 #7/#8 补登记情形不同）。

### 6.2 双向可追溯

- **回队项 ↔ MR1 R1.0 预留展开行**：**0 项**（4 项触发条件核心均未满足，无回队 MR1）。
- **维持 backlog 项 ↔ A3.x successor 登记**：#1-#4 全部维持 backlog，交叉引用本 A3.4 报告 + arm-index successor 注记。
- **finding 重开/关闭项（非本 A3.4 裁决，交叉引用）**：#1（`P1-MA2-039` 经 A1.12 RC §4 三判据复核→MR1）/ #2（`P1-MA2-045` 归 A2.4 RC 空集认证）/ #3（`P1-MA2-075` 经 A1.30 RC reuse 确认 genuinely resolved R1.24，**不重开**）/ #4（`P2-MA2-067` 维持 watch-only MR1 顺手）——这些 finding 裁决属 **A1.x/A2.x→MR1 通道**，与本 A3.4 successor 裁决各自独立（§MA2↔MA3 协作：关闭裁决/finding 修复归 A1.x/A2.x，successor 触发条件归 A3.x，交叉引用不重复）。
- **arm-index 回填**：§6.1 注记已写入 `arm-index.md`（4 既有行追加 RC MA3 注记）。

---

## 7. 静态存疑点清单（段 7，供 MA4 A4.2 展开）

> L5 无法静态定论、需运行时确认的点。本复查为 successor 触发条件复查（读 arm-index/owner doc/实仓代码/config/ORM），以下为复查中静态无法定论、建议 MA4/A4.2 运行时确认的点：

1. **#1 hr 员工离职联动 nop-auth UserAccount 运行时可达性**（离职 mutation 落地后 UserAccount 禁用副作用是否经 nop-auth I*Biz Facade 而非 daoFor 直写）：本复查静态确认 hr `ErpHrEmployee.userAccountId` 字段存在但零 nop-auth UserAccount disable writer，离职 mutation 未实现。未来离职 mutation 落地时，UserAccount 禁用是否经 `IErpAuthUserBiz` 等 nop-auth Facade（而非跨域 daoFor 直写，对齐 P1-MA1-022 跨域只读/写治理）需运行时跨域调用链确认——建议 A4.2 在离职 successor 触发后展开运行时探针。**低优先级**（successor 触发条件未满足，本存疑点仅为前瞻性排除跨域写治理风险）。

> 其余 3 项（#2/#3/#4）的运行时行为已由既有 arm MA2 报告 + A1.x RC §4 充分证实（§4），无新增静态存疑点。特别地：
> - #3 stageId 守卫 + FunnelAggregationEngine 经 A1.30 RC §4 + `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md` 运行时复核确认主路径正确（守卫拦截回退 + 漏斗按 sequence 聚合），残留等值边界（P2-RC-036）为静态已证实的边界场景 watch-only。
> - #4 cs SLA 计时 + deadline-based 升级经 A1.37/A1.38 RC §4 + `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md` 运行时证实主路径完整，滞留升级缺失为静态已证实的增强层缺失。

---

## 8. 过程纪律自检（段 8，§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（actual 见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码不反映 actual vs baseline），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以 checker 脚本退出码作为门控通过依据**。**本审计无生产代码变更（纯审计报告 + arm-index 文档注记），checker 无回归风险**——actual 计数与本审计行为正交（未触及任何生产代码），任何 actual vs baseline 差异均非本审计引入。

  | 规则 | 基线（compliance-baseline.md §BASELINE machine-readable :296-316） | actual（本次实测） | 漂移 | 归因 |
  |------|-------------------------------|-------------------|------|------|
  | R1a | 0 | 0 | 0 | — |
  | R1b | 0 | 0 | 0 | — |
  | R1c | 0 | 0 | 0 | — |
  | R1d | 14 | 14 | 0 | — |
  | R2a | 34 | 34 | 0 | — |
  | R2b | 229 | 229 | 0 | — |
  | R2c | 1382 | 1382（生产代码总计） | 0 | — |
  | R2d | 34 | 34 | 0 | — |

  > 本审计仅产出本报告 + `arm-index.md` 注记（纯文档），未触及 `module-*/` 任何生产代码。actual 全规则 = baseline，零漂移，无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（见来源 plan Closure Gates）。
- [x] **与 arm-index 交叉去重声明**：本报告全部 4 项 successor 已按 §7 规则 grep arm-index 同域同控制点后给出「复用」裁决（§6.1），无未经比对直接新建的 `P*-RC-xxx` finding（4 项全部复用既有 arm finding ID 追加 RC MA3 注记）。

---

## 9. 与既有审计差异增量声明（段 9，§去重协议）

本报告与既有 arm 审计（`docs/audits/2026-07-2*-arm-ma2-*`）+ A1.x RC 复查报告的差异增量：

- **复用既有证据**（不重复验证）：
  - `2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`（#1 P1-MA2-039 员工 RESIGNED/TERMINATED/RETIRED 三态死状态已证实）；
  - `2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（#2 P1-MA2-045 银行文件 UPLOADED/CONFIRMED 死状态已证实）；
  - `2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（#3 P1-MA2-075 stageId 守卫[历史未实现] + #4 P2-MA2-067 滞留升级未实现已证实）；
  - A1.x RC 报告（A1.12 hr 员工与组织 §4 三判据复核 + UC-HR-07⑮复用 / A1.14 hr 薪酬 L1 重开 / A1.30 crm CPQ/漏斗推进 stageId 守卫 genuinely resolved + P2-RC-036 等值边界 / A1.37 cs 工单生命周期 reuse P2-MA2-067 / A1.38 cs SLA 超时与升级 reuse A2.14:322）已证实的代码路径 + §4 三判据复核。

- **本复查只补的差异增量**：**successor 触发条件是否已满足 + 是否该回队**——从 methodology §MA3 四任务（① 触发条件状态 grep 实仓验证 / ② 回队决策 / ③ 补登记 / ④ README 覆盖复核）出发，逐项核证 4 项 hr+crm+cs successor 的触发条件现状。这是既有 arm 审计（状态机行为维度）+ A1.x RC（L1 验收标准视角 + §4 三判据 finding 复核维度）未覆盖的「successor 触发条件完整性 + 回队决策」维度（methodology §去重协议 §MA2↔MA3 协作——关闭裁决/finding 修复归 A1.x/A2.x，successor 触发条件归 A3.x，交叉引用不重复）。特别地，**#3 §对账差异 #5 核实**（区分 crm stageId finding 已修复 vs successor 残留）是本 A3.4 独有的裁决维度。

- **不重复**：不重做 doc↔code 文本一致性（audit-remediation MA3 已收口）、不重做状态机/链路行为（arm MA2 已收口）、不重做代码质量（arm MA4 已收口）、不重审方案 B 关闭裁决本身（A2.x RC 空集认证已收口）、不重审 finding §4 三判据复核（A1.x RC 已收口，本 A3.4 只复查 successor 触发条件，交叉引用）。

---

## 结论

hr+crm+cs MA3 successor 复查（A3.4）完成：4 项 design-level successor 逐项经 §MA3 四任务核证。

- **回队 MR1**：0 项（4 项触发条件核心均未满足，无 Q4 强制回队）。
- **维持 backlog successor**：4 项（#1-#4 全部维持 backlog，待各自触发条件满足）：
  - #1 hr 员工离职族状态机 + 跨域联动（触发=PM 离职/退休/转正工作流上线；nop-auth 保护区域 ask-first）；
  - #2 hr 银行文件 UPLOADED/CONFIRMED + 回单对账（触发=银行文件上传/确认流 + 回单自动对账接入）；
  - #3 crm stageId 单向递增守卫 + 漏斗统计（核心已满足[R1.24 守卫 + FunnelAggregationEngine]，残留增强层维持：P2-RC-036 等值边界 + lead-waterfall.md 增量实时/AMIS 可视化 successor）；
  - #4 cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings scheduler（触发=nop-job 定时自动触发滞留扫描/预警接入）。
- **补登记**：0 项（4 项均有 S1 arm-index 覆盖 + S2 owner doc 双源覆盖，无 owner doc 内嵌但 arm-index 无行的遗漏项）。
- **结构性约束**：§对账差异 #5（#1/#2/#3/#4 finding 已修复/关闭 vs successor 仍待触发 区分；#3 为核心核实项——finding 修复 ≠ successor 全部关闭，但核心满足 + 残留为独立 watch-only 增强层时不回队）；#1 nop-auth 保护区域（修复实施须 ask-first + 独立 plan-audit）；#4 reuse 交叉引用（P2-MA2-067 经 A1.37/A1.38 RC reuse，两维度互补不重复）。
- **arm-index 衔接**：4 项全部复用既有 ID 追加 RC MA3 注记（无新 `P*-RC-xxx`，无补登记）。
- **本审计无生产代码变更**（纯报告 + arm-index 文档注记），§9 真相源冻结条款遵守（未修改 product-scope / owner doc 需求契约段落 / arm-index 已关闭 finding 的关闭事实 / backlog README）。
